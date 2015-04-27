package com.nico.vlcfremote;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class MainActivity extends ActionBarActivity
                            implements SeekBar.OnSeekBarChangeListener,
                                       VlcConnector.VlcConnectionCallback,
                                       VlcConnector.VlcConnectionHandler {

    private VlcConnector vlc;

    private class MainMenuNavigation extends FragmentPagerAdapter {

        public MainMenuNavigation(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new DirListingFragment();
                case 1:
                    return new PlaylistFragment();
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.main_menu_title_dir_listing);
                case 1:
                    return getString(R.string.main_menu_title_playlist);
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
        ((ViewPager) super.findViewById(R.id.wMainMenu)).setAdapter(new MainMenuNavigation(getSupportFragmentManager()));
        ((SeekBar) findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);
        this.vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
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

    }

    @Override
    public void Vlc_OnConnectionFail() {

    }

    @Override
    public void Vlc_OnInternalError(Throwable ex) {

    }

    @Override
    public void Vlc_OnInvalidResponseReceived(Throwable ex) {

    }

    @Override
    public void Vlc_OnProgrammingError() {

    }
}
