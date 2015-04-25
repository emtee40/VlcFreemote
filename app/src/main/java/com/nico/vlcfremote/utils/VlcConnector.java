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


    public static interface DirListCallback {
        // contents == {Path, Name, Type}
        void dirContents(final String requestedPath, final List<String[]> contents);
    }

    public void getDirList(final String path, final DirListCallback callback) {
        final HttpGet getOp = new HttpGet(urlBase + DIR_LIST_ACTION + path);
        getOp.addHeader("Authorization", authStr);
        new HttpUtils.AsyncRequester(httpClient, getOp, new HttpUtils.HttpResponseCallback() {
            @Override
            public void responseReceived(final String msg) {
                String[] interestingAttrs = {"path", "name", "type"};
                List<String[]> dirList = HttpUtils.parseXmlList(msg, "element", interestingAttrs);
                callback.dirContents(path, dirList);
            }
        }).execute();
    }
}

