package com.nico.vlcfremote.utils;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VlcConnector {
    final static String ACTION_DIR_LIST = "requests/browse.xml?dir=";
    final static String ACTION_GET_PLAYLIST = "requests/playlist.xml";
    final static String ACTION_ADD_TO_PLAYLIST = "requests/playlist.xml?command=in_enqueue&input=";

    final String urlBase;
    final String authStr;
    final HttpClient httpClient = new DefaultHttpClient();

    public VlcConnector(final String url, final String pass) {
        urlBase = url;
        authStr = "Basic " + Base64.encodeToString((":" + pass).getBytes(), Base64.DEFAULT);
    }



    public static interface AddToPlayListCallback {
        void AddToPlayListCallback_Response(Integer addedMediaId);
        void AddToPlayListCallback_ConnectionFailure();
        void AddToPlayListCallback_InternalError(final Throwable ex);
        void AddToPlayListCallback_InvalidResponseReceived(final Throwable ex);
    }

    public void addToPlayList(final String uri, final AddToPlayListCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_ADD_TO_PLAYLIST + uri);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void connectionFailure() { callback.AddToPlayListCallback_ConnectionFailure(); }

            @Override
            public void responseReceived(int httpStatusCode, String msg) {
                try {
                    final List<PlaylistEntry> lst = parsePlaylistXml(msg);

                    for (PlaylistEntry media : lst) {
                        if (media.uri.equals(uri)) {
                            callback.AddToPlayListCallback_Response(media.id);
                            return;
                        }
                    }

                    // If no media with eq uri is found, the add command must have somehow failed
                    callback.AddToPlayListCallback_InvalidResponseReceived(null);

                } catch (HttpUtils.CantParseXmlResponse ex) {
                    callback.AddToPlayListCallback_InvalidResponseReceived(ex);
                } catch (HttpUtils.CantCreateXmlParser ex) {
                    callback.AddToPlayListCallback_InternalError(ex);
                }
            }
        }).execute();
    }



    public static class PlaylistEntry {
        public String name;
        public String uri;
        public Integer id;
        public Integer duration;
    }

    public static interface PlaylistCallback {
        void fetchPlaylist_Response(final List<PlaylistEntry> contents);
        void fetchPlaylist_ConnectionFailure();
        void fetchPlaylist_InvalidResponseReceived(Throwable ex);
        void fetchPlaylist_InternalError(Throwable ex);
    }

    public void getPlaylist(final PlaylistCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_GET_PLAYLIST);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void connectionFailure() { callback.fetchPlaylist_ConnectionFailure(); }

            @Override
            public void responseReceived(int httpStatusCode, String msg) {
                try {
                    callback.fetchPlaylist_Response(parsePlaylistXml(msg));
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.fetchPlaylist_InternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.fetchPlaylist_InvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
    }



    public static class DirListEntry {
        public boolean isDirectory;
        public String name;
        public String path;
        public String human_friendly_path;
    }

    public static interface DirListCallback {
        void fetchDirList_Response(final String requestedPath, final List<DirListEntry> contents);
        void fetchDirList_ConnectionFailure();
        void fetchDirList_InvalidResponseReceived(Throwable ex);
        void fetchDirList_InternalError(Throwable ex);
    }

    public void getDirList(final String path, final DirListCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_DIR_LIST + path);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void connectionFailure() { callback.fetchDirList_ConnectionFailure(); }

            @Override
            public void responseReceived(int httpStatusCode, String msg) {
                Log.i("ASD", msg);
                // TODO: if code != 200 fail
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

                    callback.fetchDirList_Response(path, lst);
                } catch (HttpUtils.CantCreateXmlParser cantCreateXmlParser) {
                    callback.fetchDirList_InternalError(cantCreateXmlParser);
                } catch (HttpUtils.CantParseXmlResponse cantParseXmlResponse) {
                    callback.fetchDirList_InvalidResponseReceived(cantParseXmlResponse);
                }
            }
        }).execute();
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

