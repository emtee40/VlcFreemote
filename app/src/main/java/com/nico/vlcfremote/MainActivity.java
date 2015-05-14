package com.nico.vlcfremote;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class MainActivity extends ActionBarActivity
                            implements SeekBar.OnSeekBarChangeListener,
                                       VlcConnector.VlcConnectionCallback,
                                       VlcConnector.VlcConnectionHandler,
                                       ServerSelectView.OnServerSelectedCallback {

    private VlcConnector vlc;
    private DirListingFragment dirlistView;
    private PlaylistFragment playlistView;
    private ServerSelectView serverSelectView;
    private MainMenuNavigation mainMenu;

    private class MainMenuNavigation extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        private final PlaylistFragment playlistView;
        private final DirListingFragment dirlistView;
        private final ServerSelectView serverSelectView;
        private final ViewPager parentView;

        public MainMenuNavigation(ViewPager view, FragmentManager fm, PlaylistFragment playlistView,
                                   DirListingFragment dirlistView, ServerSelectView serverSelectView)
        {
            super(fm);
            this.parentView = view;
            this.playlistView = playlistView;
            this.dirlistView = dirlistView;
            this.serverSelectView = serverSelectView;
            
            parentView.setAdapter(this);
            parentView.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return playlistView;
                case 1: return dirlistView;
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
                case 0: playlistView.updatePlaylist(); return;
                case 1: dirlistView.updateDirectoryList(); return;
                case 2: serverSelectView.scanServers(); return;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " selected a page which doesn't exist.");
            }
        }

        public void jumpToServersPage() {
            parentView.setCurrentItem(2, true);
        }

        public void jumpToPlaylistPage() {
            parentView.setCurrentItem(0, true);
        }
    }

    @Override
    public void onNewServerSelected(String ip, String port, String password) {
        SharedPreferences.Editor cfg = getPreferences(0).edit();
        cfg.putString("VLC_Last_Server_IP", ip);
        cfg.putString("VLC_Last_Server_Port", port);
        cfg.putString("VLC_Last_Server_Pass", password);
        cfg.apply();

        this.vlc = new VlcConnector(this, ip, port, password);
        this.playlistView.updatePlaylist();
        this.dirlistView.updateDirectoryList();
        this.mainMenu.jumpToPlaylistPage();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, PlaylistFragment.class.getName(), playlistView);
        getSupportFragmentManager().putFragment(outState, DirListingFragment.class.getName(), dirlistView);
        getSupportFragmentManager().putFragment(outState, ServerSelectView.class.getName(), serverSelectView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Will connect to a dummy server first time and jump directly to the servers page
        final String lastServer_IP = getPreferences(0).getString("VLC_Last_Server_IP", "localhost");
        final String lastServer_Port = getPreferences(0).getString("VLC_Last_Server_Port", "8080");
        final String lastServer_Pass = getPreferences(0).getString("VLC_Last_Server_Pass", "dummy_pass");
        this.vlc = new VlcConnector(this, lastServer_IP, lastServer_Port, lastServer_Pass);

        // Create or restore all views
        if (savedInstanceState != null) {
            playlistView = (PlaylistFragment) getSupportFragmentManager().getFragment(savedInstanceState, PlaylistFragment.class.getName());
            dirlistView = (DirListingFragment) getSupportFragmentManager().getFragment(savedInstanceState, DirListingFragment.class.getName());
            serverSelectView = (ServerSelectView) getSupportFragmentManager().getFragment(savedInstanceState, ServerSelectView.class.getName());
        }

        if (this.playlistView == null) this.playlistView = new PlaylistFragment();
        if (this.dirlistView == null) this.dirlistView = new DirListingFragment();
        if (this.serverSelectView == null) this.serverSelectView = new ServerSelectView();


        this.mainMenu = new MainMenuNavigation(((ViewPager) super.findViewById(R.id.wMainMenu)),
                                                getSupportFragmentManager(), playlistView, dirlistView, serverSelectView);

        ((SeekBar) findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.wPlayer_PlayPosition)).setOnSeekBarChangeListener(this);

        // Periodically update VLC
        vlcStatusUpdateTimerHandler.postDelayed(vlcStatusUpdateTimer, 0);
    }

    android.os.Handler vlcStatusUpdateTimerHandler = new android.os.Handler();
    Runnable vlcStatusUpdateTimer = new Runnable() {
        @Override
        public void run() {
            vlc.updateStatus();
            vlcStatusUpdateTimerHandler.postDelayed(this, 2000);
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        vlcStatusUpdateTimerHandler.removeCallbacks(vlcStatusUpdateTimer    );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void wPlayer_ToggleMoreOptions(View view) {
        if (findViewById(R.id.wPlayer_ExtraOptions).getVisibility() == View.GONE) {
            // TODO: Set image
            findViewById(R.id.wPlayer_ExtraOptions).setVisibility(View.VISIBLE);
        } else {
            // TODO: Set image
            findViewById(R.id.wPlayer_ExtraOptions).setVisibility(View.GONE);
        }
    }

    public void onPlayer_ToggleFullscreen(View view) {
        vlc.toggleFullscreen();
    }

    public void onPlayer_CycleAudioTrack(View view) {
    }

    public void onPlayer_CycleSubtitleTrack(View view) {
    }


    @Override
    public VlcConnector getVlcConnector() {
        return vlc;
    }

    public void onPlayPauseClick(View view) {
        getVlcConnector().togglePlay();
    }

    public void onPlayPreviousClick(View view) {
        getVlcConnector().playPrevious();
    }

    public void onPlayNextClick(View view) {
        getVlcConnector().playNext();
    }

    public void onPlayPosition_JumpBack(View view) {
        getVlcConnector().playPosition_JumpRelative("-0.5");
    }

    public void onPlayPosition_JumpForward(View view) {
        getVlcConnector().playPosition_JumpRelative("+0.5");
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;

        switch (seekBar.getId()) {
            case R.id.wPlayer_PlayPosition:
                getVlcConnector().playPosition_JumpToPercent(progress);
                break;
            case R.id.wPlayer_Volume:
                getVlcConnector().setVolume(progress);
                break;
            default:
                throw new RuntimeException(MainMenuNavigation.class.getName() + " received a progress event for a SeekBar which doesn't exist.");
        }
    }

    @Override
    public void Vlc_OnStatusUpdated(final VlcConnector.VlcStatus stat) {
        ((TextView) findViewById(R.id.wPlayer_CurrentlyPlaying)).setText("Current status: " + stat.state);

        ((SeekBar) findViewById(R.id.wPlayer_Volume)).setProgress(stat.volume);

        ((SeekBar) findViewById(R.id.wPlayer_PlayPosition)).setProgress((int) (100 * stat.position));
        ((TextView) findViewById(R.id.wPlayer_PlayPosition_CurrentPositionText))
                    .setText(String.format("%d:%02d", stat.time / 60, stat.time % 60));
        ((TextView) findViewById(R.id.wPlayer_PlayPosition_Length))
                .setText(String.format("%d:%02d", stat.length / 60, stat.length % 60));

    }

    @Override
    public void Vlc_OnPlaylistFetched(final List<VlcConnector.PlaylistEntry> contents) {
        this.playlistView.Vlc_OnPlaylistFetched(contents);
    }

    @Override
    public void Vlc_OnDirListingFetched(final String requestedPath, final List<VlcConnector.DirListEntry> contents) {
        this.dirlistView.Vlc_OnDirListingFetched(requestedPath, contents);
    }

    @Override
    public void Vlc_OnSelectDirIsInvalid(final String requestedPath) {
        this.dirlistView.Vlc_OnSelectDirIsInvalid(requestedPath);
    }


    @Override
    public void Vlc_OnLoginIncorrect() {
        CharSequence msg = getResources().getString(R.string.status_vlc_wrong_password);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();

        mainMenu.jumpToServersPage();
    }

    @Override
    public void Vlc_OnConnectionFail() {
        CharSequence msg = getResources().getString(R.string.status_vlc_cant_connect);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();

        mainMenu.jumpToServersPage();
    }

    @Override
    public void Vlc_OnInternalError(Throwable ex) {
        CharSequence msg = getResources().getString(R.string.stats_vlc_parser_internal_error);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void Vlc_OnInvalidResponseReceived(Throwable ex) {
        CharSequence msg = getResources().getString(R.string.status_invalid_vlc_response);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }
}
