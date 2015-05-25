package com.nico.vlcfremote.utils;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// TODO
// $ vim /usr/share/doc/vlc/lua/http/requests/README.txt.gz
public class VlcConnector {

    private static final String URL_ENCODED_PERCENT = "%25";

    private enum VLC_Actions {
        // Replaceable actions
        ACTION_DIR_LIST,
        ACTION_GET_PLAYLIST,
        ACTION_SET_VOLUME,
        ACTION_PLAY_POSITION_JUMP,

        // Ordered and priority actions
        ACTION_ADD_TO_PLAYLIST,
        ACTION_TOGGLE_PLAY,
        ACTION_START_PLAYING,
        ACTION_PLAY_NEXT,
        ACTION_PLAY_PREVIOUS,
        ACTION_DELETE_FROM_PLAYLIST,
        ACTION_CLEAR_PLAYLIST,
        ACTION_TOGGLE_FULLSCREEN,
        ACTION_CYCLE_SUBTITLE,
        ACTION_CYCLE_AUDIO,

        // Lowest priority actions (only if nothing other than ACTION_DIR_LIST or ACTION_GET_PLAYLIST are pending)
        ACTION_GET_STATUS
    }

    private static String getUrlForAction(final VLC_Actions action) {
        switch (action) {
            case ACTION_DIR_LIST: return "requests/browse.xml?dir=";
            case ACTION_GET_PLAYLIST: return "requests/playlist.xml";
            case ACTION_ADD_TO_PLAYLIST: return "requests/status.xml?command=in_enqueue&input=";
            case ACTION_TOGGLE_PLAY: return "requests/status.xml?command=pl_pause";
            case ACTION_START_PLAYING: return "requests/status.xml?command=pl_play&id=";
            case ACTION_PLAY_NEXT: return "requests/status.xml?command=pl_next";
            case ACTION_PLAY_PREVIOUS: return "requests/status.xml?command=pl_previous";
            case ACTION_SET_VOLUME: return "requests/status.xml?command=volume&val=";
            case ACTION_DELETE_FROM_PLAYLIST: return "requests/status.xml?command=pl_delete&id=";
            case ACTION_CLEAR_PLAYLIST: return "requests/status.xml?command=pl_empty";
            case ACTION_GET_STATUS: return "requests/status.xml";
            case ACTION_PLAY_POSITION_JUMP: return "requests/status.xml?command=seek&val=";
            case ACTION_TOGGLE_FULLSCREEN: return "requests/status.xml?command=fullscreen";
            case ACTION_CYCLE_SUBTITLE: return "requests/status.xml?command=key&val=subtitle-track";
            case ACTION_CYCLE_AUDIO: return "requests/status.xml?command=key&val=audio-track";
            default: throw new RuntimeException("Requested non existent VLC action");
        }
    }

    final String urlBase;
    final String authStr;
    final HttpClient httpClient = new DefaultHttpClient();
    private final VlcConnectionCallback callback;
    private VlcStatus lastKnownStatus;
    private boolean requestInProgress = false;

    public VlcStatus getLastKnownStatus() {
        return lastKnownStatus;
    }

    public VlcConnector(final VlcConnectionCallback callback, final String ip, final String port, final String pass) {
        this(callback, "http://" + ip + ":" + port + "/", pass);
    }

    private VlcConnector(final VlcConnectionCallback callback,final String url, final String pass) {
        this.lastKnownStatus = new VlcStatus();
        this.callback = callback;
        urlBase = url;
        authStr = "Basic " + Base64.encodeToString((":" + pass).getBytes(), Base64.DEFAULT);
    }

    public final String getServerUrl() {
        return urlBase;
    }

    public static class VlcStatus {
        public int length;
        public float position; // Progress % of the current file
        public int volume;
        public int time;
        public float rate;
        public float audiodelay;
        public float subtitledelay;
        public boolean repeat;
        public boolean loop;
        public boolean random;
        public boolean fullscreen;
        public String state;
        public String currentMedia_filename;
        public String currentMedia_album;
        public String currentMedia_title;
        public String currentMedia_artist;
        public int currentMedia_trackNumber;
        public int currentMedia_tracksTotal;

        VlcStatus() { state = ""; }

        public boolean isCurrentlyPlayingSomething() { return state.equalsIgnoreCase("playing"); }
    }

    public static class PlaylistEntry {
        public String name;
        public String uri;
        public Integer id;
        public Integer duration;
    }

    public static class DirListEntry {
        public boolean isDirectory;
        public String name;
        public String path;
        public String human_friendly_path;
    }

    public static class InvalidHttpResponseCode extends Throwable {
        final String msg;
        public InvalidHttpResponseCode(int httpStatusCode) { msg = "Received invalid HTTP code " + httpStatusCode; }
        @Override public String getMessage() { return msg; }
    }

    public static interface VlcConnectionCallback {
        void Vlc_OnPlaylistFetched(final List<PlaylistEntry> contents);
        void Vlc_OnDirListingFetched(final String requestedPath, final List<DirListEntry> contents);
        void Vlc_OnSelectDirIsInvalid(String path);
        void Vlc_OnStatusUpdated(VlcStatus stat);

        void Vlc_OnLoginIncorrect();
        void Vlc_OnConnectionFail();
        void Vlc_OnInternalError(final Throwable ex);
        void Vlc_OnInvalidResponseReceived(final Throwable ex);
    }

    public static interface VlcConnectionHandler {
        VlcConnector getVlcConnector();
    }

    public void togglePlay() {
        doSimpleCommand(VLC_Actions.ACTION_TOGGLE_PLAY);
    }

    public void playNext() {
        doSimpleCommand(VLC_Actions.ACTION_PLAY_NEXT);
    }

    public void playPrevious() {
        doSimpleCommand(VLC_Actions.ACTION_PLAY_PREVIOUS);
    }

    public void toggleFullscreen() {
        doSimpleCommand(VLC_Actions.ACTION_TOGGLE_FULLSCREEN);
    }

    public void startPlaying(Integer id) {
        doSimpleCommand(VLC_Actions.ACTION_START_PLAYING, String.valueOf(id));
    }

    public void removeFromPlaylist(Integer id) {
        doSimpleCommand(VLC_Actions.ACTION_DELETE_FROM_PLAYLIST, String.valueOf(id));
        updatePlaylist();
    }

    public void clearPlaylist() {
        doSimpleCommand(VLC_Actions.ACTION_CLEAR_PLAYLIST);
        updatePlaylist();
    }

    /**
     * VLC time jump
     * @param jumpPercent Percent to jump, including - or + (5 is not valid, +5 is)
     */
    public void playPosition_JumpRelative(final String jumpPercent) {
        doSimpleCommand(VLC_Actions.ACTION_PLAY_POSITION_JUMP, jumpPercent + URL_ENCODED_PERCENT);
    }

    public synchronized void playPosition_JumpToPercent(int position) {
        doSimpleCommand(VLC_Actions.ACTION_PLAY_POSITION_JUMP, String.valueOf(position) + URL_ENCODED_PERCENT);
    }

    public synchronized void setVolume(final int progress) {
        doSimpleCommand(VLC_Actions.ACTION_SET_VOLUME, String.valueOf(progress));
    }

    public void updateStatus() {
        doSimpleCommand(VLC_Actions.ACTION_GET_STATUS);
    }

    public void addToPlayList(final String uri) {
        doSimpleCommand(VLC_Actions.ACTION_ADD_TO_PLAYLIST, uri);
        updatePlaylist();
    }

    public void cycleAudioTrack() {
        doSimpleCommand(VLC_Actions.ACTION_CYCLE_AUDIO);
    }

    public void cycleSubtitleTrack() {
        doSimpleCommand(VLC_Actions.ACTION_CYCLE_SUBTITLE);
    }

    /**
     * Forwards an action with no param
     * @param action VLC action to perform
     */
    private void doSimpleCommand(final VLC_Actions action) { doSimpleCommand(action, null); }

    private void doSimpleCommand(final VLC_Actions action, final String param) {
        if (requestInProgress) {
            // Usually, this will only be triggered when the user changes the volume or some other
            // seek bar or element that generates a lot of events
            return;
        }

        requestInProgress = true;
        String url = urlBase + getUrlForAction(action);
        if (param != null) url += param;
        final HttpGet getOp = new HttpGet(url);
        getOp.addHeader("Authorization", authStr);
        
        HttpUtils.AsyncRequester task = new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() {
                requestInProgress = false;
            }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                requestInProgress = false;
                processVlcStatus(httpStatusCode, msg);
            }
        });

        queueTask(task, action);
    }

    public void updatePlaylist() {
        final HttpGet getOp = new HttpGet(urlBase + getUrlForAction(VLC_Actions.ACTION_GET_PLAYLIST));
        getOp.addHeader("Authorization", authStr);

        HttpUtils.AsyncRequester task = new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() {
                callback.Vlc_OnConnectionFail();
            }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode)) return;

                try {
                    final List<PlaylistEntry> parsed = parsePlaylistXml(msg);
                    callback.Vlc_OnPlaylistFetched(parsed);
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        });

        queueTask(task, VLC_Actions.ACTION_GET_PLAYLIST);
    }

    public void getDirList(final String path) {
        final HttpGet getOp = new HttpGet(urlBase + getUrlForAction(VLC_Actions.ACTION_DIR_LIST) + path);
        getOp.addHeader("Authorization", authStr);

        HttpUtils.AsyncRequester task = new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() {
                callback.Vlc_OnConnectionFail();
            }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode)) return;

                try {
                    final List<DirListEntry> lst = HttpUtils.parseXmlList(msg, "element", new HttpUtils.XmlMogrifier<DirListEntry>(DirListEntry.class) {
                        @Override
                        void parseValue(DirListEntry object, String key, String value) {
                            switch (key) {
                                // Using the uri as path saves the work of url encoding stuff: the uri (sans
                                // it's protocol) is enough to be passed to vlc again as a param
                                case "uri":
                                    object.path = value.substring("file://".length());
                                    break;
                                case "path":
                                    object.human_friendly_path = value;
                                    break;
                                case "name":
                                    object.name = value;
                                    break;
                                case "type":
                                    object.isDirectory = (value.equals("dir"));
                                    break;
                            }
                        }
                    });

                    Collections.sort(lst, new Comparator<DirListEntry>() {
                        @Override
                        public int compare(DirListEntry lhs, DirListEntry rhs) {
                            if (lhs.isDirectory == rhs.isDirectory) {
                                return lhs.name.compareTo(rhs.name);
                            }

                            return lhs.isDirectory ? -1 : 1;
                        }
                    });


                    // If browsing to the directory fails we get an html response with a 200 status
                    // instead of a nice http error or a parsable xml message
                    if ((lst.size() == 0) && msg.contains("cannot open directory")) {
                        callback.Vlc_OnSelectDirIsInvalid(path);
                    } else {
                        callback.Vlc_OnDirListingFetched(path, lst);
                    }
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        });

        queueTask(task, VLC_Actions.ACTION_DIR_LIST);
    }

    private static class PendingTask {
        final public HttpUtils.AsyncRequester task;
        final public VLC_Actions action;
        PendingTask(final HttpUtils.AsyncRequester task, final VLC_Actions action) {
            this.task = task;
            this.action = action;
        }
    }

    private void queueTask(final HttpUtils.AsyncRequester task, final VLC_Actions action) {
        task.execute();
        /*
        Log.i("PENDING QUEUE", "Enqueue " + getUrlForAction(action));

        List<PendingTask> pendingTasks = new ArrayList<>();
        // These actions always take priority and must be executed in order
        switch (action) {
            case ACTION_ADD_TO_PLAYLIST:
            case ACTION_TOGGLE_PLAY:
            case ACTION_START_PLAYING:
            case ACTION_PLAY_NEXT:
            case ACTION_PLAY_PREVIOUS:
            case ACTION_DELETE_FROM_PLAYLIST:
            case ACTION_CLEAR_PLAYLIST:
            case ACTION_TOGGLE_FULLSCREEN:
            case ACTION_CYCLE_SUBTITLE:
            case ACTION_CYCLE_AUDIO:
                pendingTasks.add(new PendingTask(task, action));
                return;
        }

        // It makes no sense to queue more than one of these actions
        switch (action) {
            case ACTION_DIR_LIST:
            case ACTION_GET_PLAYLIST:
            case ACTION_SET_VOLUME:
            case ACTION_PLAY_POSITION_JUMP:
                for (int i=0; i<pendingTasks.size(); ++i) {
                    if (pendingTasks.get(i).action == action) {
                        pendingTasks.remove(i);
                        break;
                    }
                }
                pendingTasks.add(new PendingTask(task, action));
                return;
        }

        // Low priority actions, only exec if there are no other pending tasks
        switch (action) {
            case ACTION_GET_STATUS:
                if (pendingTasks.size() == 0) {
                    pendingTasks.add(new PendingTask(task, action));
                }
                //noinspection UnnecessaryReturnStatement
                return;
        }
        */
    }


    /**
     * Helpers to parse stuff
     */
    private boolean isHttpCodeValid(int httpStatusCode){
        if (httpStatusCode == 401) {
            callback.Vlc_OnLoginIncorrect();
            return false;
        } else if (httpStatusCode != 200) {
            callback.Vlc_OnInvalidResponseReceived(new InvalidHttpResponseCode(httpStatusCode));
            return false;
        }
        return true;
    }

    private static List<PlaylistEntry> parsePlaylistXml(final String msg) throws HttpUtils.CantParseXmlResponse, HttpUtils.CantCreateXmlParser {
        return HttpUtils.parseXmlList(msg, "leaf", new HttpUtils.XmlMogrifier<PlaylistEntry>(PlaylistEntry.class) {
            @Override
            void parseValue(PlaylistEntry object, String key, String value) {
                switch (key) {
                    case "uri": object.uri = value; break;
                    case "name": object.name = value; break;
                    case "id": object.id = Integer.parseInt(value); break;
                    case "duration": object.duration = Integer.parseInt(value); break;
                }
            }
        });
    }

    private void processVlcStatus(int httpStatusCode, String msg) {
        if (!isHttpCodeValid(httpStatusCode)) return;

        try {
            VlcStatus stat = HttpUtils.parseXmlObject(msg, new HttpUtils.XmlMogrifier<VlcStatus>(VlcStatus.class) {
                @Override
                void parseValue(VlcStatus obj, String key, String val) {
                    switch (key) {
                        case "length":
                            obj.length = Integer.parseInt(val);
                            break;
                        case "position":
                            obj.position = Float.parseFloat(val);
                            break;
                        case "volume":
                            obj.volume = Integer.parseInt(val);
                            break;
                        case "time":
                            obj.time = Integer.parseInt(val);
                            break;
                        case "rate":
                            obj.rate = Float.parseFloat(val);
                            break;
                        case "audiodelay":
                            obj.audiodelay = Float.parseFloat(val);
                            break;
                        case "subtitledelay":
                            obj.subtitledelay = Float.parseFloat(val);
                            break;
                        case "repeat":
                            obj.repeat = Boolean.parseBoolean(val);
                            break;
                        case "loop":
                            obj.loop = Boolean.parseBoolean(val);
                            break;
                        case "random":
                            obj.random = Boolean.parseBoolean(val);
                            break;
                        case "fullscreen":
                            obj.fullscreen = Boolean.parseBoolean(val);
                            break;
                        case "state":
                            obj.state = val;
                            break;
                        case "filename":
                            obj.currentMedia_filename = val;
                            break;
                        case "album":
                            obj.currentMedia_album = val;
                            break;
                        case "title":
                            obj.currentMedia_title = val;
                            break;
                        case "artist":
                            obj.currentMedia_artist = val;
                            break;
                        case "track_number":
                            obj.currentMedia_trackNumber = Integer.parseInt(val);
                            break;
                        case "track_total":
                            obj.currentMedia_tracksTotal = Integer.parseInt(val);
                            break;
                        default:
                            /* Do nothing, we don't care about this tag */
                    }
                }
            });

            this.lastKnownStatus = stat;
            callback.Vlc_OnStatusUpdated(stat);
        } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
            callback.Vlc_OnInternalError(cantCreateXmlParser);
        } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
            callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
        }
    }
}

