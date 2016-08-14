package com.nico.vlcfreemote.vlc_connector;

import android.util.Log;

import com.nico.vlcfreemote.vlc_connector.http_utils.Wget;
import com.nico.vlcfreemote.vlc_connector.http_utils.XmlListReader;
import com.nico.vlcfreemote.vlc_connector.http_utils.XmlMogrifier;

import java.util.List;

public class Cmd_DirList implements VlcCommand {

    public static class DirListEntry {
        public boolean isDirectory;
        public String name;
        public String path;
        public String human_friendly_path;
    }

    public interface Callback {
        void onContentAvailable(List<DirListEntry> results);
        void onContentError();
    }


    private final String path;
    private final Callback cb;

    public Cmd_DirList(final String path, Callback cb) {
        this.path = path;
        this.cb = cb;
    }

    @Override
    public String getCommandPath() {
        return "requests/browse.xml?dir=" + path;
    }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }

    @Override
    public Wget.Callback getWgetCallback(final VlcCommand.GeneralCallback generalCallback) {
        return new Wget.Callback() {
            @Override
            public void onConnectionError(Exception request_exception) { generalCallback.onConnectionError(); }

            @Override
            public void onAuthFailure() {
                generalCallback.onAuthError();
            }

            @Override
            public void onHttpFail(int httpRetCode, String result) {
                cb.onContentError();
            }

            @Override
            public void onResponse(final String result) {

                // If the request fails (ie dir doesn't exist) Vlc still returns http 200
                if (result.indexOf("<title>Error loading /requests/browse.xml</title>") != -1) {
                    onHttpFail(-1, result);
                    return;
                }

                XmlMogrifier.XmlKeyValReader<DirListEntry> keyValReader;
                keyValReader = new XmlMogrifier.XmlKeyValReader<DirListEntry>(DirListEntry.class) {
                    @Override
                    protected void parseValue(DirListEntry object, String key, String value) {
                        switch (key) {
                            // Using the uri as path saves the work of url encoding stuff: the uri (sans
                            // its protocol) is enough to be passed to vlc again as a param
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
                };

                new XmlListReader<>(result, "element", keyValReader, new XmlMogrifier.Callback<DirListEntry>() {
                    @Override
                    public void onXmlSystemError(Exception e) {
                        generalCallback.onSystemError(e);
                    }

                    @Override
                    public void onXmlDecodingError(Exception e) {
                        cb.onContentError();
                    }

                    @Override
                    public void onResult(List<DirListEntry> results) {
                        cb.onContentAvailable(results);
                    }
                });
            }
        };
    }
}
