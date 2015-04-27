package com.nico.vlcfremote.utils;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO
// /usr/share$ vim doc/vlc/lua/http/requests/README.txt.gz
public class VlcConnector {
    private static final String ACTION_DIR_LIST = "requests/browse.xml?dir=";
    private static final String ACTION_GET_PLAYLIST = "requests/playlist.xml";
    private static final String ACTION_ADD_TO_PLAYLIST = "requests/playlist.xml?command=in_enqueue&input=";
    private static final String ACTION_TOGGLE_PLAY = "requests/playlist.xml?command=pl_pause";
    private static final String ACTION_PLAY_NEXT = "requests/playlist.xml?command=pl_next";
    private static final String ACTION_PLAY_PREVIOUS = "requests/playlist.xml?command=pl_previous";
    private static final String ACTION_SET_VOLUME = "requests/playlist.xml?command=volume&val=";
    private static final String ACTION_GET_STATUS = "requests/status.xml";
    private static final String ACTION_PLAY_POSITION_JUMP = "requests/playlist.xml?command=seek&val=";

    final String urlBase;
    final String authStr;
    final HttpClient httpClient = new DefaultHttpClient();

    public VlcConnector(final String url, final String pass) {
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

    public static class InvalidMediaAddResponse extends Throwable {
        final String msg;
        public InvalidMediaAddResponse() { msg = "Expected to find added media in playlist, none found."; }
        @Override public String getMessage() { return msg; }
    }

    public static interface VlcConnectionCallback {
        void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId);
        void Vlc_OnPlaylistFetched(final List<PlaylistEntry> contents);
        void Vlc_OnDirListingFetched(final String requestedPath, final List<DirListEntry> contents);

        void Vlc_OnLoginIncorrect();
        void Vlc_OnConnectionFail();
        void Vlc_OnInternalError(final Throwable ex);
        void Vlc_OnInvalidResponseReceived(final Throwable ex);
        void Vlc_OnProgrammingError();
    }

    public static interface VlcConnectionHandler {
        VlcConnector getVlcConnector();
    }


    public void togglePlay() {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_TOGGLE_PLAY);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {            }
        }).execute();
    }

    public void playNext() {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_PLAY_NEXT);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {            }
        }).execute();
    }


    public void playPrevious() {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_PLAY_PREVIOUS);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {            }
        }).execute();
    }

    public void setVolume(int progress) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_SET_VOLUME + progress);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {            }
        }).execute();
    }



    public void getStatus() {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_GET_STATUS);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                Log.i("ASD", msg);
            }
        }).execute();
    }


    public void playPosition_JumpRelative(double jumpTime) {
        HttpGet getOp = null;
        try {
            getOp = new HttpGet(urlBase + ACTION_PLAY_POSITION_JUMP + jumpTime + URLEncoder.encode("%", "UTF-8"));
            Log.i("ASD", URLEncoder.encode("%", "UTF-8"));
            Log.i("ASD", URLEncoder.encode("%", "UTF-8"));
            Log.i("ASD", URLEncoder.encode("%", "UTF-8"));
            Log.i("ASD", URLEncoder.encode("%", "UTF-8"));
            Log.i("ASD", URLEncoder.encode("%", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { }
            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) { }
        }).execute();
    }






    public void addToPlayList(final String uri, final VlcConnectionCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_ADD_TO_PLAYLIST + uri);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode, callback)) return;

                try {
                    final List<PlaylistEntry> lst = parsePlaylistXml(msg);

                    for (PlaylistEntry media : lst) {
                        if (media.uri.equals(uri)) {
                            callback.Vlc_OnAddedToPlaylistCallback(media.id);
                            return;
                        }
                    }

                    // If no media with eq uri is found, the add command must have somehow failed
                    callback.Vlc_OnInvalidResponseReceived(new InvalidMediaAddResponse());

                } catch (HttpUtils.CantParseXmlResponse ex) {
                    callback.Vlc_OnInvalidResponseReceived(ex);
                } catch (HttpUtils.CantCreateXmlParser ex) {
                    callback.Vlc_OnInternalError(ex);
                }
            }
        }).execute();
    }

    public void getPlaylist(final VlcConnectionCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_GET_PLAYLIST);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                Log.i("ASD", "HTTP" + httpStatusCode + " Playlist recv " + msg);
                if (!isHttpCodeValid(httpStatusCode, callback)) return;
                Log.i("ASD", "ASDASD");

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

    public void getDirList(final String path, final VlcConnectionCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_DIR_LIST + path);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void onHttpConnectionFailure() { callback.Vlc_OnConnectionFail(); }

            @Override
            public void onHttpResponseReceived(int httpStatusCode, String msg) {
                if (!isHttpCodeValid(httpStatusCode, callback)) return;

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

                    callback.Vlc_OnDirListingFetched(path, lst);
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.Vlc_OnInternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.Vlc_OnInvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
    }

    private boolean isHttpCodeValid(int httpStatusCode, final VlcConnectionCallback callback){
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

