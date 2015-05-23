package com.nico.vlcfremote.utils;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO
// $ vim /usr/share/doc/vlc/lua/http/requests/README.txt.gz
public class VlcConnector {
    private static final String ACTION_DIR_LIST = "requests/browse.xml?dir=";
    private static final String ACTION_GET_PLAYLIST = "requests/playlist.xml";
    private static final String ACTION_ADD_TO_PLAYLIST = "requests/status.xml?command=in_enqueue&input=";
    private static final String ACTION_TOGGLE_PLAY = "requests/status.xml?command=pl_pause";
    private static final String ACTION_START_PLAYING = "requests/status.xml?command=pl_play&id=";
    private static final String ACTION_PLAY_NEXT = "requests/status.xml?command=pl_next";
    private static final String ACTION_PLAY_PREVIOUS = "requests/status.xml?command=pl_previous";
    private static final String ACTION_SET_VOLUME = "requests/status.xml?command=volume&val=";
    private static final String ACTION_DELETE_FROM_PLAYLIST = "requests/status.xml?command=pl_delete&id=";
    private static final String ACTION_CLEAR_PLAYLIST = "requests/status.xml?command=pl_empty";
    private static final String ACTION_GET_STATUS = "requests/status.xml";
    private static final String ACTION_PLAY_POSITION_JUMP = "requests/status.xml?command=seek&val=";
    private static final String ACTION_TOGGLE_FULLSCREEN = "requests/status.xml?command=fullscreen";
    private static final String URL_ENCODED_PERCENT = "%25";

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

    public VlcConnector(final VlcConnectionCallback callback,final String url, final String pass) {
        this.lastKnownStatus = new VlcStatus();
        this.callback = callback;
        urlBase = url;
        authStr = "Basic " + Base64.encodeToString((":" + pass).getBytes(), Base64.DEFAULT);
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
        doSimpleCommand(ACTION_TOGGLE_PLAY);
    }

    public void playNext() {
        doSimpleCommand(ACTION_PLAY_NEXT);
    }

    public void playPrevious() {
        doSimpleCommand(ACTION_PLAY_PREVIOUS);
    }

    public void toggleFullscreen() {
        doSimpleCommand(ACTION_TOGGLE_FULLSCREEN);
    }

    public void startPlaying(Integer id) {
        doSimpleCommand(ACTION_START_PLAYING + String.valueOf(id));
    }

    public void removeFromPlaylist(Integer id) {
        doSimpleCommand(ACTION_DELETE_FROM_PLAYLIST + String.valueOf(id));
        updatePlaylist();
    }

    public void clearPlaylist() {
        doSimpleCommand(ACTION_CLEAR_PLAYLIST);
        updatePlaylist();
    }

    /**
     * VLC time jump
     * @param jumpPercent Percent to jump, including - or + (5 is not valid, +5 is)
     */
    public void playPosition_JumpRelative(final String jumpPercent) {
        doSimpleCommand(ACTION_PLAY_POSITION_JUMP + jumpPercent + URL_ENCODED_PERCENT);
    }

    public synchronized void playPosition_JumpToPercent(int position) {
        doSimpleCommand(ACTION_PLAY_POSITION_JUMP + String.valueOf(position) + URL_ENCODED_PERCENT);
    }

    public synchronized void setVolume(final int progress) {
        doSimpleCommand(ACTION_SET_VOLUME + progress);
    }

    public void updateStatus() {
        doSimpleCommand(ACTION_GET_STATUS);
    }

    public void addToPlayList(final String uri) {
        doSimpleCommand(ACTION_ADD_TO_PLAYLIST + uri);
        updatePlaylist();
    }

    private void doSimpleCommand(final String action) {
        if (requestInProgress) {
            // Usually, this will only be triggered when the user changes the volume or some other
            // seek bar or element that generates a lot of events
            return;
        }

        requestInProgress = true;
        final HttpGet getOp = new HttpGet(urlBase + action);
        getOp.addHeader("Authorization", authStr);
        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { requestInProgress = false; }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                requestInProgress = false;
                processVlcStatus(httpStatusCode, msg);
            }
        }).execute();
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

    public void updatePlaylist() {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_GET_PLAYLIST);
        getOp.addHeader("Authorization", authStr);

        Log.d("VLCFREEMOTE","Sending playlist update RQ");
        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                Log.d("VLCFREEMOTE","Received playlist update response");
                if (!isHttpCodeValid(httpStatusCode)) return;

                try {
                    Log.d("VLCFREEMOTE","Received playlist update response - XML parse start");
                    final List<PlaylistEntry> parsed = parsePlaylistXml(msg);
                    Log.d("VLCFREEMOTE","Received playlist update response - XML parse end, callback start");
                    callback.Vlc_OnPlaylistFetched(parsed);
                    Log.d("VLCFREEMOTE","Received playlist update response - callback end");
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
        Log.d("VLCFREEMOTE","Sent playlist update RQ");
    }

    public void getDirList(final String path) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_DIR_LIST + path);
        getOp.addHeader("Authorization", authStr);

        Log.d("VLCFREEMOTE","Get dir list RQ send start");
        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode)) return;


                Log.d("VLCFREEMOTE","Get dir list RQ response ready, XML parse start");

                try {
                    final List<DirListEntry> lst = HttpUtils.parseXmlList(msg, "element", new HttpUtils.XmlMogrifier<DirListEntry>(DirListEntry.class) {
                        @Override
                        void parseValue(DirListEntry object, String key, String value) {
                            switch (key) {
                                // Using the uri as path saves the work of url encoding stuff: the uri (sans
                                // it's protocol) is enough to be passed to vlc again as a param
                                case "uri": object.path = value.substring("file://".length()); break;
                                case "path": object.human_friendly_path = value; break;
                                case "name": object.name = value; break;
                                case "type": object.isDirectory = (value.equals("dir")); break;
                            }
                        }
                    });

                    Log.d("VLCFREEMOTE","Get dir list RQ parse done, sort start");

                    Collections.sort(lst, new Comparator<DirListEntry>() {
                        @Override
                        public int compare(DirListEntry lhs, DirListEntry rhs) {
                            if (lhs.isDirectory == rhs.isDirectory) {
                                return lhs.name.compareTo(rhs.name);
                            }

                            return lhs.isDirectory? -1 : 1;
                        }
                    });


                    Log.d("VLCFREEMOTE","Get dir list RQ sort done, start CB");
                    // If browsing to the directory fails we get an html response with a 200 status
                    // instead of a nice http error or a parsable xml message
                    if ((lst.size() == 0) && msg.contains("cannot open directory")) {
                        callback.Vlc_OnSelectDirIsInvalid(path);
                    } else {
                        callback.Vlc_OnDirListingFetched(path, lst);
                    }
                    Log.d("VLCFREEMOTE","Get dir list RQ CB done");
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
        Log.d("VLCFREEMOTE","Get dir list RQ sent");
    }

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
}

