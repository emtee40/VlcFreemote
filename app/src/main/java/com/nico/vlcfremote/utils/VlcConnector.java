package com.nico.vlcfremote.utils;

import android.util.Base64;
import android.util.Log;

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
        void playlistContentsAvailable(final List<PlaylistEntry> contents);
    }

    public void getPlaylist(final PlaylistCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_GET_PLAYLIST);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void responseReceived(final String msg) {
                List<PlaylistEntry> lst = HttpUtils.parseXmlList(msg, "leaf", new HttpUtils.XmlMogrifierCallback<PlaylistEntry>() {
                    private PlaylistEntry object;

                    @Override
                    public void reset() {
                        this.object = new PlaylistEntry();
                    }

                    @Override
                    public void parseValue(String name, String value) {
                        switch (name) {
                            case "uri": object.uri = value; break;
                            case "name": object.name = value; break;
                            case "id": object.id = Integer.parseInt(value); break;
                            case "duration": object.duration = Integer.parseInt(value); break;
                        }
                    }

                    @Override
                    public PlaylistEntry getParsedObject() {
                        return object;
                    }
                });

                callback.playlistContentsAvailable(lst);
            }
        }).execute();
    }

    public static class DirListEntry {
        public boolean isDirectory;
        public String name;
        public String path;
    }

    public static interface DirListCallback {
        void dirContents(final String requestedPath, final List<DirListEntry> contents);
    }

    public void getDirList(final String path, final DirListCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + ACTION_DIR_LIST + path);
        getOp.addHeader("Authorization", authStr);

        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void responseReceived(final String msg) {
                List<DirListEntry> dirList = HttpUtils.parseXmlList(msg, "element", new HttpUtils.XmlMogrifierCallback<DirListEntry>() {
                    private DirListEntry object;

                    @Override
                    public void reset() {
                        this.object = new DirListEntry();
                    }

                    @Override
                    public void parseValue(String name, String value) {
                        switch (name) {
                            case "path": object.path = value; break;
                            case "name": object.name = value; break;
                            case "type": object.isDirectory = (value.equals("dir")); break;
                        }
                    }

                    @Override
                    public DirListEntry getParsedObject() {
                        return object;
                    }
                });

                callback.dirContents(path, dirList);
            }
        }).execute();
    }
}

