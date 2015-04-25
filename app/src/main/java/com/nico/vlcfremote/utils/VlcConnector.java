package com.nico.vlcfremote.utils;

import android.util.Base64;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

public class VlcConnector {
    final static String DIR_LIST_ACTION = "requests/browse.xml?dir=";

    final String urlBase;
    final String authStr;
    final HttpClient httpClient = new DefaultHttpClient();

    public VlcConnector(final String url, final String pass) {
        urlBase = url;
        authStr = "Basic " + Base64.encodeToString((":" + pass).getBytes(), Base64.DEFAULT);
    }

    public static class DirListEntry {
        public boolean isDirectory;
        public String name;
        public String path;
    }

    public static interface DirListCallback {
        // contents == {Path, Name, Type}
        void dirContents(final String requestedPath, final List<DirListEntry> contents);
    }

    public void getDirList(final String path, final DirListCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + DIR_LIST_ACTION + path);
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

