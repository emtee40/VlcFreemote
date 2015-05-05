package com.nico.vlcfremote.utils;

import android.util.Base64;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO
// /usr/share$ vim doc/vlc/lua/http/requests/README.txt.gz
public class VlcConnector {
    private static final String ACTION_DIR_LIST = "requests/browse.xml?dir=";
    private static final String ACTION_GET_PLAYLIST = "requests/playlist.xml";
    private static final String ACTION_ADD_TO_PLAYLIST = "requests/status.xml?command=in_enqueue&input=";
    private static final String ACTION_TOGGLE_PLAY = "requests/status.xml?command=pl_pause";
    private static final String ACTION_PLAY_NEXT = "requests/status.xml?command=pl_next";
    private static final String ACTION_PLAY_PREVIOUS = "requests/status.xml?command=pl_previous";
    private static final String ACTION_SET_VOLUME = "requests/status.xml?command=volume&val=";
    private static final String ACTION_GET_STATUS = "requests/status.xml";
    private static final String ACTION_PLAY_POSITION_JUMP = "requests/status.xml?command=seek&val=";

    final String urlBase;
    final String authStr;
    final HttpClient httpClient = new DefaultHttpClient();
    private final VlcConnectionCallback callback;

    public VlcConnector(final VlcConnectionCallback callback, final String ip, final String port, final String pass) {
        this(callback, "http://" + ip + ":" + port + "/", pass);
    }

    public VlcConnector(final VlcConnectionCallback callback,final String url, final String pass) {
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

    /**
     * VLC time jump
     * @param jumpPercent Percent to jump, including - or + (5 is not valid, +5 is)
     */
    public void playPosition_JumpRelative(final String jumpPercent) {
        doSimpleCommand(ACTION_PLAY_POSITION_JUMP + jumpPercent + "%25"); // %25 == UrlEncode('%');
    }

    public void updateStatus() {
        doSimpleCommand(ACTION_GET_STATUS);
    }

    public void addToPlayList(final String uri) {
        doSimpleCommand(ACTION_ADD_TO_PLAYLIST + uri);
        /* TODO: Is there any point in having this callback? Maybe just update playlist + state
        void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId);
        */
    }

    private void doSimpleCommand(final String action) {
        final HttpGet getOp = new HttpGet(urlBase + action);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) { processVlcStatus(httpStatusCode, msg); }
        }).execute();
    }

    private boolean setVolume_InProgress = false;
    public synchronized void setVolume(final int progress) {
        if (setVolume_InProgress) return;
        setVolume_InProgress = true;

        final HttpGet getOp = new HttpGet(urlBase + ACTION_SET_VOLUME + progress);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { setVolume_InProgress = false; }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                setVolume_InProgress = false;
                processVlcStatus(httpStatusCode, msg);
            }
        }).execute();
    }

    public static class VlcStatus {
        public int currentplid;
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
    }

    private void processVlcStatus(int httpStatusCode, String msg) {
        if (!isHttpCodeValid(httpStatusCode)) return;

        try {
            VlcStatus stat = HttpUtils.parseXmlObjec(msg, new HttpUtils.XmlMogrifier<VlcStatus>(VlcStatus.class) {
                @Override
                void parseValue(VlcStatus obj, String key, String val) {
                    switch (key) {
                        case "currentplid":
                            obj.currentplid = Integer.parseInt(val);
                            break;
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
                        default:
                                    /* Do nothing, we don't care about this tag */
                    }
                }
            });

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

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode)) return;

                try {
                    callback.Vlc_OnPlaylistFetched(parsePlaylistXml(msg));
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
    }

    public void getDirList(final String path) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_DIR_LIST + path);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

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
                                case "uri": object.path = value.substring("file://".length()); break;
                                case "path": object.human_friendly_path = value; break;
                                case "name": object.name = value; break;
                                case "type": object.isDirectory = (value.equals("dir")); break;
                            }
                        }
                    });

                    Collections.sort(lst, new Comparator<DirListEntry>() {
                        @Override
                        public int compare(DirListEntry lhs, DirListEntry rhs) {
                            if (lhs.isDirectory == rhs.isDirectory) {
                                return lhs.name.compareTo(rhs.name);
                            }

                            return lhs.isDirectory? -1 : 1;
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
        }).execute();
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

