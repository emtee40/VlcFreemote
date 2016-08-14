package com.nico.vlcfreemote.model;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.nico.vlcfreemote.local_settings.Bookmarks;
import com.nico.vlcfreemote.local_settings.PlayedFiles;
import com.nico.vlcfreemote.local_settings.RememberedServers;
import com.nico.vlcfreemote.vlc_connector.Cmd_DirList;
import com.nico.vlcfreemote.vlc_connector.RemoteVlc;

import java.util.List;

public class VlcPath {

    public interface UICallback {
        void onNewDirListAvailable(List<Cmd_DirList.DirListEntry> results);
        void onDirListFatalFailure(VlcPath_ApplicationError vlcPath_applicationError);
    }

    public class VlcPath_ApplicationError extends Exception {
        public VlcPath_ApplicationError() {
            super("Fatal error: the default VlcPath (" + VLC_DEFAULT_START_PATH + ") is not valid "+
                  "on your setup. Please submit a bug report." );
        }
    }

    // Is this Windows compatible? Who knows...
    private static final String VLC_DEFAULT_START_PATH = "~";

    private final RemoteVlc.ConnectionProvider vlcProvider;
    private final Context dbContext;
    private final UICallback uiCallback;

    private String currentPath;
    private String prettyPath;

    // Usually a person will cd to several directories before settling for one: there's no point
    // in saving them all, so there will be a timeout after which we can consider the user has
    // decided; only then will the directory be saved.
    private static final int LAST_PATH_SAVE_DELAY_MS = 5000;
    private final Handler bgRunner = new Handler();
    private Runnable saveLastPathTask = null;

    public VlcPath(RemoteVlc.ConnectionProvider vlcProvider, Context dbContext, UICallback uiCallback) {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();

        if (srv.getLastPath() == null) {
            this.currentPath = VLC_DEFAULT_START_PATH;
            this.prettyPath = VLC_DEFAULT_START_PATH;
        } else {
            this.currentPath = srv.getLastPath();
            this.prettyPath = srv.getLastPath();
        }

        this.vlcProvider = vlcProvider;
        this.dbContext = dbContext;
        this.uiCallback = uiCallback;
    }

    public void onServerChanged(final Server srv) {
        if (srv.getLastPath() != null) {
            this.currentPath = srv.getLastPath();
            this.prettyPath = srv.getLastPath();
        }
    }

    /**
     * Changes directory to $path and saves $path as the last known directory for the current server
     * @param path CD to $path
     * @param prettyPath Display name for $path
     */
    public void cd(final String path, final String prettyPath) {
        this.currentPath = path;
        this.prettyPath = prettyPath;

        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        srv.setLastPath(path);
        saveCurrentPath(srv);
    }

    private void saveCurrentPath(final Server srv) {

        if (saveLastPathTask != null) {
            bgRunner.removeCallbacks(saveLastPathTask);
        }

        saveLastPathTask = new Runnable() {
            @Override
            public void run() {
                new RememberedServers(dbContext).saveLastPathForServer(srv);
            }
        };

        bgRunner.postDelayed(saveLastPathTask, LAST_PATH_SAVE_DELAY_MS);
    }

    public String getPrettyCWD() {
        return prettyPath;
    }

    public void updateDirContents() {
        updateDirContents_impl(true);
    }

    private void updateDirContents_impl(final boolean mayRecurse) {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        List<String> filesPlayedInDir = (new PlayedFiles(dbContext)).getListOfPlayedFiles(srv, currentPath);

        vlcProvider.getActiveVlcConnection().exec(new Cmd_DirList(currentPath, filesPlayedInDir,
                                                        new Cmd_DirList.Callback() {
            @Override
            public void onContentAvailable(List<Cmd_DirList.DirListEntry> results) {
                uiCallback.onNewDirListAvailable(results);
            }

            @Override
            public void onContentError() {
                Log.e(getClass().getSimpleName(), "Couldn't `cd " + currentPath + "`. Will `cd "+
                                                    VLC_DEFAULT_START_PATH +"` instead.");

                currentPath = VLC_DEFAULT_START_PATH;
                prettyPath = VLC_DEFAULT_START_PATH;
                if (mayRecurse) {
                    updateDirContents_impl(false);
                } else {
                    // If this fails for a second time, it means the default path is broken
                    // and there is nothing we can do... there is a bug in the application
                    uiCallback.onDirListFatalFailure(new VlcPath_ApplicationError());
                }
            }
        }));
    }

    public void bookmarkCurrentDirectory() {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        (new Bookmarks(dbContext)).addBookmark(srv, currentPath);
    }

    public List<String> getBookmarks() {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        return (new Bookmarks(dbContext)).getBookmarks(srv);
    }

    public void deleteBookmark(final String path) {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        (new Bookmarks(dbContext)).deleteBookmark(srv, path);
    }

    public void onAddToPlaylistRequested(String path) {
        final Server srv = vlcProvider.getActiveVlcConnection().getServer();
        (new PlayedFiles(dbContext)).addPlayedFile(srv, path);
    }
}