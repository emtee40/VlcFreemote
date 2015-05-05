package com.nico.vlcfremote.utils;

import android.app.Activity;
import android.support.v4.app.Fragment;

import java.util.List;

public abstract class VlcActionFragment extends Fragment implements VlcConnector.VlcConnectionCallback {

    protected VlcConnector.VlcConnectionCallback vlcDefaultCallback;
    protected VlcConnector.VlcConnectionHandler vlcConnection;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            vlcDefaultCallback = (VlcConnector.VlcConnectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement VlcConnectionCallback");
        }

        try {
            vlcConnection = (VlcConnector.VlcConnectionHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement VlcConnectionHandler");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        vlcDefaultCallback = null;
        vlcConnection = null;
    }

    // Forward default VLC actions
    @Override public void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId) { vlcDefaultCallback.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnDirListingFetched(String requestedPath, List<VlcConnector.DirListEntry> contents) { vlcDefaultCallback.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnSelectDirIsInvalid(String requestedPath) { vlcDefaultCallback.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnPlaylistFetched(List<VlcConnector.PlaylistEntry> contents) { vlcDefaultCallback.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnStatusUpdated(VlcConnector.VlcStatus stat){ vlcDefaultCallback.Vlc_OnProgrammingError(); }

    @Override public void Vlc_OnLoginIncorrect() { vlcDefaultCallback.Vlc_OnLoginIncorrect(); }
    @Override public void Vlc_OnConnectionFail() { vlcDefaultCallback.Vlc_OnConnectionFail(); }
    @Override public void Vlc_OnInternalError(Throwable ex) { vlcDefaultCallback.Vlc_OnInternalError(ex); }
    @Override public void Vlc_OnInvalidResponseReceived(Throwable ex) { vlcDefaultCallback.Vlc_OnInvalidResponseReceived(ex); }
    @Override public void Vlc_OnProgrammingError() { vlcDefaultCallback.Vlc_OnProgrammingError(); }

}
