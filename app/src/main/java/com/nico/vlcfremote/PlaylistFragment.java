package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class PlaylistFragment extends Fragment implements View.OnClickListener {
    public PlaylistFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePlaylist();
    }


    private static class PlaylistEntry_ViewAdapter extends ArrayAdapter<VlcConnector.PlaylistEntry> {
        private List<VlcConnector.PlaylistEntry> items;
        private static final int layoutResourceId = R.layout.fragment_playlist_list_element;
        private Context context;
        private final View.OnClickListener onClickCallback;

        public static class Row {
            VlcConnector.PlaylistEntry values;
            ImageView wPlaylistElement_FileStatus;
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

            holder.wPlaylistElement_Duration = (TextView)row.findViewById(R.id.wPlaylistElement_Duration);
            holder.wPlaylistElement_Duration.setText(String.valueOf(holder.values.duration));
            holder.wPlaylistElement_Duration.setTag(holder.values);

            holder.wPlaylistElement_Remove = (ImageButton)row.findViewById(R.id.wPlaylistElement_Remove);
            holder.wPlaylistElement_Remove.setTag(String.valueOf(holder.values));
            holder.wPlaylistElement_Remove.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }

    void updatePlaylist() {
        final FragmentActivity activity = getActivity();
        final PlaylistFragment self = this;

        VlcConnector vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
        vlc.getPlaylist(new VlcConnector.PlaylistCallback() {
            @Override
            public void fetchPlaylist_Response(List<VlcConnector.PlaylistEntry> contents) {

                final PlaylistEntry_ViewAdapter adapt = new PlaylistEntry_ViewAdapter(self, activity, contents);
                final ListView lst = (ListView) activity.findViewById(R.id.wPlaylist_List);
                lst.setAdapter(adapt);
                ((ArrayAdapter) lst.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void fetchPlaylist_ConnectionFailure() {

            }

            @Override
            public void fetchPlaylist_InvalidResponseReceived(Throwable ex) {

            }

            @Override
            public void fetchPlaylist_InternalError(Throwable ex) {

            }
        });
    }

    @Override
    public void onClick(View v) {

    }
}
