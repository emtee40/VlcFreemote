package com.nico.vlcfremote;

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
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class MainActivity extends ActionBarActivity
                            implements SeekBar.OnSeekBarChangeListener,
                                       VlcConnector.VlcConnectionCallback,
                                       VlcConnector.VlcConnectionHandler {

    private VlcConnector vlc;
    private DirListingFragment dirlistView;
    private PlaylistFragment playlistView;
    private ServerSelectView serverSelectView;

    private class MainMenuNavigation extends FragmentPagerAdapter {

        private final PlaylistFragment playlistView;
        private final DirListingFragment dirlistView;
        private final ServerSelectView serverSelectView;

        public MainMenuNavigation(FragmentManager fm, PlaylistFragment playlistView, DirListingFragment dirlistView, ServerSelectView serverSelectView) {
            super(fm);
            this.playlistView = playlistView;
            this.dirlistView = dirlistView;
            this.serverSelectView = serverSelectView;
        }

        @Override
        public void finishUpdate(ViewGroup vw) {
            super.finishUpdate(vw);
            Log.i("HOLA", "Update playlist NOW");
            // TODO // playlistView.updatePlaylist();
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return playlistView;
                case 1: return dirlistView;
                case 2: return serverSelectView;
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.main_menu_title_dir_listing);
                case 1: return getString(R.string.main_menu_title_playlist);
                case 2: return getString(R.string.main_menu_title_servers);
            }
            return "ASDADS";
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        this.vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
        this.playlistView = new PlaylistFragment();
        this.dirlistView = new DirListingFragment();
        this.serverSelectView = new ServerSelectView();

        MainMenuNavigation tabCtrl = new MainMenuNavigation(getSupportFragmentManager(), playlistView, dirlistView, serverSelectView);
        ((ViewPager) super.findViewById(R.id.wMainMenu)).setAdapter(tabCtrl);
        ((SeekBar) findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);
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
        getVlcConnector().playPosition_JumpRelative(-0.5);
    }

    public void onPlayPosition_JumpForward(View view) {
        getVlcConnector().playPosition_JumpRelative(0.5);
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        getVlcConnector().setVolume(progress);
    }


    // Handle VLC callbacks
    @Override public void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId) { this.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnPlaylistFetched(List<VlcConnector.PlaylistEntry> contents) { this.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnDirListingFetched(String requestedPath, List<VlcConnector.DirListEntry> contents) { this.Vlc_OnProgrammingError(); }

    @Override
    public void Vlc_OnLoginIncorrect() {
        CharSequence msg = getResources().getString(R.string.status_vlc_wrong_password);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void Vlc_OnConnectionFail() {
        CharSequence msg = getResources().getString(R.string.status_vlc_cant_connect);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
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

    @Override
    public void Vlc_OnProgrammingError() {
        CharSequence msg = getResources().getString(R.string.status_assertion_failed);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }
}
