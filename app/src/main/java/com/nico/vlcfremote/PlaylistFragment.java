package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcActionFragment;
import com.nico.vlcfremote.utils.VlcConnector;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends VlcActionFragment
                              implements View.OnClickListener {

    private PlaylistEntry_ViewAdapter playlistViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playlist, container, false);

        playlistViewAdapter = new PlaylistEntry_ViewAdapter(this, getActivity());
        v.findViewById(R.id.wPlaylist_Clear).setOnClickListener(this);
        v.findViewById(R.id.wPlaylist_Refresh).setOnClickListener(this);
        ((ListView) v.findViewById(R.id.wPlaylist_List)).setAdapter(playlistViewAdapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getUserVisibleHint() && isAdded()) updatePlaylist();
    }

    public void updatePlaylist() {
        playlistViewAdapter.clear();
        vlcConnection.getVlcConnector().updatePlaylist();
    }

    public void updateCurrentlyPlayingMedia(String currentPlayingMediaTitle) {
        playlistViewAdapter.setCurrentPlayingMedia(currentPlayingMediaTitle);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wPlaylist_Clear:
                vlcConnection.getVlcConnector().clearPlaylist();
                vlcConnection.getVlcConnector().updatePlaylist();
                return;
            case R.id.wPlaylist_Refresh:
                vlcConnection.getVlcConnector().updatePlaylist();
                return;
        }

        VlcConnector.PlaylistEntry item = (VlcConnector.PlaylistEntry) v.getTag();
        if (item == null)
            throw new RuntimeException(PlaylistFragment.class.getName() + " received a click event for a view with no playlist item.");

        switch (v.getId()) {
            case R.id.wPlaylistElement_CurrentStatus:
            case R.id.wPlaylistElement_Duration:
            case R.id.wPlaylistElement_Name:
                vlcConnection.getVlcConnector().startPlaying(item.id);
                break;
            case R.id.wPlaylistElement_Remove:
                vlcConnection.getVlcConnector().removeFromPlaylist(item.id);
                vlcConnection.getVlcConnector().updatePlaylist();
                break;
            default:
                throw new RuntimeException(PlaylistFragment.class.getName() + " received a click event for a view which doesn't exist.");
        }
    }

    public void Vlc_OnPlaylistFetched(final List<VlcConnector.PlaylistEntry> contents) {
        playlistViewAdapter.addAll(contents);
    }

    private static class PlaylistEntry_ViewAdapter extends ArrayAdapter<VlcConnector.PlaylistEntry> {
        private static final int layoutResourceId = R.layout.fragment_playlist_list_element;
        private Context context;
        private final View.OnClickListener onClickCallback;
        private String currentPlayingMediaTitle;

        public static class Row {
            VlcConnector.PlaylistEntry values;
            ImageButton wPlaylistElement_CurrentStatus;
            TextView wPlaylistElement_Name;
            TextView wPlaylistElement_Duration;
            ImageButton wPlaylistElement_Remove;
        }

        public PlaylistEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<VlcConnector.PlaylistEntry>());
            this.context = context;
            this.onClickCallback = onClickCallback;
            this.currentPlayingMediaTitle = null;
        }

        public void setCurrentPlayingMedia(String currentPlayingMediaTitle) {
            if (this.currentPlayingMediaTitle != null && currentPlayingMediaTitle.equals(this.currentPlayingMediaTitle)) {
                // No need to update anything
                return;
            }

            this.currentPlayingMediaTitle = currentPlayingMediaTitle;
            this.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final View row;
            if (convertView == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.values = this.getItem(position);

            holder.wPlaylistElement_CurrentStatus = (ImageButton)row.findViewById(R.id.wPlaylistElement_CurrentStatus);
            holder.wPlaylistElement_CurrentStatus.setTag(holder.values);
            holder.wPlaylistElement_CurrentStatus.setOnClickListener(onClickCallback);


            Log.i("UPDATE STAT", String.valueOf(currentPlayingMediaTitle) + " == " + holder.values.id);

            if (currentPlayingMediaTitle != null && currentPlayingMediaTitle.equals(holder.values.name)) {
                holder.wPlaylistElement_CurrentStatus.setVisibility(View.VISIBLE);
            } else {
                holder.wPlaylistElement_CurrentStatus.setVisibility(View.INVISIBLE);
            }

            holder.wPlaylistElement_Name = (TextView)row.findViewById(R.id.wPlaylistElement_Name);
            holder.wPlaylistElement_Name.setText(holder.values.name);
            holder.wPlaylistElement_Name.setTag(holder.values);
            holder.wPlaylistElement_Name.setOnClickListener(onClickCallback);

            holder.wPlaylistElement_Duration = (TextView)row.findViewById(R.id.wPlaylistElement_Duration);
            if (holder.values.duration >= 0) {
                holder.wPlaylistElement_Duration.
                        setText(String.format("%d:%02d", holder.values.duration / 60, holder.values.duration % 60));
            } else {
                holder.wPlaylistElement_Duration.setText(context.getString(R.string.playlist_item_duration_unknown));
            }

            holder.wPlaylistElement_Duration.setTag(holder.values);
            holder.wPlaylistElement_Duration.setOnClickListener(onClickCallback);

            holder.wPlaylistElement_Remove = (ImageButton)row.findViewById(R.id.wPlaylistElement_Remove);
            holder.wPlaylistElement_Remove.setTag(holder.values);
            holder.wPlaylistElement_Remove.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
