package com.nico.vlcfreemote;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.nico.vlcfreemote.vlc_connector.RemoteVlc;

public abstract class VlcFragment extends Fragment {
    private RemoteVlc.ConnectionProvider vlcProvider;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            vlcProvider = (RemoteVlc.ConnectionProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement RemoteVlc.ConnectionProvider");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        vlcProvider = null;
    }

    RemoteVlc getVlc() {
        return vlcProvider.getActiveVlcConnection();
    }
}
