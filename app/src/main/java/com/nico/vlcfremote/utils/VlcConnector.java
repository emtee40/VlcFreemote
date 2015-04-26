package com.nico.vlcfremote.utils;

import android.util.Base64;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

public class VlcConnector {
    final static String ACTION_DIR_LIST = "requests/browse.xml?dir=";
    final static String ACTION_GET_PLAYLIST = "requests/playlist.xml";

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
                    final List<PlaylistEntry> lst = HttpUtils.parseXmlList(msg, "leaf", new HttpUtils.XmlMogrifier<PlaylistEntry>(PlaylistEntry.class) {
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

                    callback.fetchPlaylist_Response(lst);
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
                try {
                    final List<DirListEntry> lst = HttpUtils.parseXmlList(msg, "element", new HttpUtils.XmlMogrifier<DirListEntry>(DirListEntry.class) {
                        @Override
                        void parseValue(DirListEntry object, String key, String value) {
                            switch (key) {
                                case "path": object.path = value; break;
                                case "name": object.name = value; break;
                                case "type": object.isDirectory = (value.equals("dir")); break;
                            }
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
}

