package com.nico.vlcfremote.utils;

import android.app.Activity;
import android.support.v4.app.Fragment;

public abstract class VlcActionFragment extends Fragment {

    protected VlcConnector.VlcConnectionHandler vlcConnection;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            vlcConnection = (VlcConnector.VlcConnectionHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement VlcConnectionHandler");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        vlcConnection = null;
    }
}
