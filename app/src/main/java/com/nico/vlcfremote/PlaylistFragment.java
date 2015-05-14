package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcActionFragment;
import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class PlaylistFragment extends VlcActionFragment
                              implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.wPlaylist_Clear).setOnClickListener(this);
        getActivity().findViewById(R.id.wPlaylist_Refresh).setOnClickListener(this);
        updatePlaylist();
    }

    public void updatePlaylist() {
        vlcConnection.getVlcConnector().updatePlaylist();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wPlaylist_Clear:
                vlcConnection.getVlcConnector().clearPlaylist();
                return;
            case R.id.wPlaylist_Refresh:
                vlcConnection.getVlcConnector().updatePlaylist();
                return;
        }

        VlcConnector.PlaylistEntry item = (VlcConnector.PlaylistEntry) v.getTag();
        if (item == null)
            throw new RuntimeException(PlaylistFragment.class.getName() + " received a click event for a view with no playlist item.");

        switch (v.getId()) {
            case R.id.wPlaylistElement_Duration:
            case R.id.wPlaylistElement_Name:
                vlcConnection.getVlcConnector().startPlaying(item.id);
                break;
            case R.id.wPlaylistElement_Remove:
                vlcConnection.getVlcConnector().removeFromPlaylist(item.id);
                break;
            default:
                throw new RuntimeException(PlaylistFragment.class.getName() + " received a click event for a view which doesn't exist.");
        }
    }

    public void Vlc_OnPlaylistFetched(final List<VlcConnector.PlaylistEntry> contents) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        final PlaylistEntry_ViewAdapter adapt = new PlaylistEntry_ViewAdapter(this, activity, contents);
        final ListView lst = (ListView) activity.findViewById(R.id.wPlaylist_List);
        // lst might be null if the user changes tabs at this point
        if (lst != null) {
            lst.setAdapter(adapt);
            ((ArrayAdapter) lst.getAdapter()).notifyDataSetChanged();
        }
    }

    private static class PlaylistEntry_ViewAdapter extends ArrayAdapter<VlcConnector.PlaylistEntry> {
        private List<VlcConnector.PlaylistEntry> items;
        private static final int layoutResourceId = R.layout.fragment_playlist_list_element;
        private Context context;
        private final View.OnClickListener onClickCallback;

        public static class Row {
            VlcConnector.PlaylistEntry values;
            TextView wPlaylistElement_Name;
            TextView wPlaylistElement_Duration;
            ImageButton wPlaylistElement_Remove;
        }

        public PlaylistEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context, List<VlcConnector.PlaylistEntry> items) {
            super(context, layoutResourceId, items);
            this.context = context;
            this.items = items;
            this.onClickCallback = onClickCallback;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View row = inflater.inflate(layoutResourceId, parent, false);

            Row holder = new Row();
            holder.values = items.get(position);

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
