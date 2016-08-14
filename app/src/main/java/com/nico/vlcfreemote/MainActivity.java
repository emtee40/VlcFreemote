package com.nico.vlcfreemote;

import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.nico.vlcfreemote.model.Server;
import com.nico.vlcfreemote.vlc_connector.Cmd_AddToPlaylist;
import com.nico.vlcfreemote.vlc_connector.Cmd_TogglePlay;
import com.nico.vlcfreemote.vlc_connector.Cmd_UpdateStatus;
import com.nico.vlcfreemote.vlc_connector.RemoteVlc;
import com.nico.vlcfreemote.vlc_connector.VlcCommand;
import com.nico.vlcfreemote.model.VlcStatus;

public class MainActivity extends FragmentActivity
                          implements RemoteVlc.ConnectionProvider,
                                     VlcCommand.GeneralCallback,
                                     VlcStatus.Observer,
                                     ServerSelectView.ServerSelectionCallback,
                                     DirListingView.DirListingCallback {

    private RemoteVlc vlcConnection;
    private PlayerControllerView playerControllerView;
    private PlaylistView playlistView;
    private DirListingView dirListView;
    private ServerSelectView serverSelectView;
    private MainMenuNavigation mainMenu;

    private class MainMenuNavigation extends FragmentPagerAdapter
                                     implements ViewPager.OnPageChangeListener {

        private final PlaylistView playlistView;
        private final DirListingView dirListView;
        private final ServerSelectView serverSelectView;
        private final ViewPager parentView;

        public MainMenuNavigation(ViewPager view, FragmentManager fm, PlaylistView playlistView,
                                  DirListingView dirListView, ServerSelectView serverSelectView)
        {
            super(fm);
            this.parentView = view;
            this.playlistView = playlistView;
            this.dirListView = dirListView;
            this.serverSelectView = serverSelectView;

            parentView.setAdapter(this);
            parentView.addOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return playlistView;
                case 1: return dirListView;
                case 2: return serverSelectView;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to select a page item which doesn't exist.");
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.main_menu_title_playlist);
                case 1: return getString(R.string.main_menu_title_dir_listing);
                case 2: return getString(R.string.main_menu_title_servers);
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to get a title for a page which doesn't exist.");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override public void onPageScrolled(int i, float v, int i2) {}
        @Override public void onPageScrollStateChanged(int i) {}

        @Override
        public void onPageSelected(int i) {
            switch (i) {
                case 0: /*playlistView.triggerPlaylistUpdate();*/ return;
                case 1: /*dirListView.triggerCurrentPathListUpdate();*/ return;
                case 2: /*serverSelectView.scanServers();*/ return;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " selected a page which doesn't exist.");
            }
        }

        public void jumpToServerSelection() { parentView.setCurrentItem(2, true); }
        public void jumpToPlaylist() { parentView.setCurrentItem(0, true); }
    }

    private void safePutFragment(final Bundle outState, final String name, Fragment obj) {
        try {
            getSupportFragmentManager().putFragment(outState, name, obj);
        } catch (IllegalStateException e) {
            // Some fragments might not be in the fragment manager: if this is the case, just save a null
            // object to give the activity a chance of recreating the fragment when resuming
        }
    }

    @Override
    // This should handle things like device rotation: if state is not saved then the fragment may
    // be recreated and all sort of funny crashes will happen.
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        safePutFragment(outState, PlayerControllerView.class.getName(), playerControllerView);
        safePutFragment(outState, PlaylistView.class.getName(), playlistView);
        safePutFragment(outState, DirListingView.class.getName(), dirListView);
        safePutFragment(outState, ServerSelectView.class.getName(), serverSelectView);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create or restore all views
        if (savedInstanceState != null) {
            playerControllerView = (PlayerControllerView) getSupportFragmentManager().getFragment(savedInstanceState, PlayerControllerView.class.getName());
            playlistView = (PlaylistView) getSupportFragmentManager().getFragment(savedInstanceState, PlaylistView.class.getName());
            dirListView = (DirListingView) getSupportFragmentManager().getFragment(savedInstanceState, DirListingView.class.getName());
            serverSelectView = (ServerSelectView) getSupportFragmentManager().getFragment(savedInstanceState, ServerSelectView.class.getName());
        }

        if (this.playerControllerView == null) this.playerControllerView = new PlayerControllerView();
        if (this.playlistView == null) this.playlistView = new PlaylistView();
        if (this.dirListView == null) this.dirListView = new DirListingView();
        if (this.serverSelectView == null) this.serverSelectView = new ServerSelectView();

        this.mainMenu = new MainMenuNavigation(((ViewPager) super.findViewById(R.id.wMainMenu)),
                getSupportFragmentManager(), playlistView, dirListView, serverSelectView);

        onNewServerSelected(serverSelectView.getLastUsedServer(this));
        // TODO updateVlcStatus();
    }

    @Override
    public void onNewServerSelected(final Server srv) {
        if (srv!=null) {
            Log.i(getClass().getSimpleName(), "Connecting to server " + srv.ip + ":" + srv.vlcPort);
            this.vlcConnection = new RemoteVlc(srv, this);
        } else {
            // Connect to dummy server: the first command will fail and prompt a new server select
            this.vlcConnection = new RemoteVlc(new Server("", 0, null), this);
        }

        playlistView.triggerPlaylistUpdate();
        dirListView.onServerChanged(srv);
        mainMenu.jumpToPlaylist();
    }

    private void updateVlcStatus() {
        // TODO
        if (! vlcConnection.getLatestStats().isStopped()) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("XXXXXXX", "DELAYED");
                    vlcConnection.exec(new Cmd_UpdateStatus(vlcConnection));
                    updateVlcStatus();
                }
            }, 5000);
        }
    }

    @Override
    public void onAddToPlaylistRequested(final String uri) {
        Log.i(getClass().getSimpleName(), "Add to playlist: " + uri);
        vlcConnection.exec(new Cmd_AddToPlaylist(uri, vlcConnection));

        if (vlcConnection.getLatestStats().isStopped()) {
            vlcConnection.exec(new Cmd_TogglePlay(vlcConnection));
        }

        this.playlistView.triggerPlaylistUpdate();
    }

    @Override
    public void onVlcStatusUpdate(VlcStatus results) {
        this.playerControllerView.onStatusUpdated(this, results);
    }

    @Override
    public RemoteVlc getActiveVlcConnection() {
        return vlcConnection;
    }

    @Override
    public void onAuthError() {
        mainMenu.jumpToServerSelection();

        CharSequence msg = getString(R.string.status_vlc_wrong_password);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onConnectionError() {
        mainMenu.jumpToServerSelection();

        final String server = vlcConnection.getServer().ip + ':' + vlcConnection.getServer().vlcPort;
        final String msg = getString(R.string.status_vlc_cant_connect);
        final String fmtMsg = String.format(msg, server);
        Toast toast = Toast.makeText(getApplicationContext(), fmtMsg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onVlcStatusFetchError() {
        // Getting here means there was an error that couldn't be handled. The only handled. The
        // only reasonable thing to do is to disconnect and try again.
        onConnectionError();
    }

    @Override
    public void onSystemError(Exception e) {
        Log.e(getClass().getSimpleName(), "Programmer error! " + e.toString());

        final String msg = getString(R.string.status_fatal_app_error);
        final String server = vlcConnection.getServer().ip + ':' + vlcConnection.getServer().vlcPort;
        final String fmtMsg = String.format(msg, server);

        final String fmtMsgExtra;
        final String msgExtra = getString(R.string.status_fatal_app_error_extra_info);
        fmtMsgExtra = String.format(msgExtra, e.getMessage());

        Toast toast = Toast.makeText(getApplicationContext(), fmtMsg + fmtMsgExtra, Toast.LENGTH_LONG);
        toast.show();

        mainMenu.jumpToServerSelection();
    }
}
