package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;


public class PlaylistView extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);
        updatePlaylist();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void updatePlaylist() {
        final PlaylistView self = this;
        VlcConnector vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
        vlc.getPlaylist(new VlcConnector.PlaylistCallback() {
            @Override
            public void fetchPlaylist_Response(List<VlcConnector.PlaylistEntry> contents) {

                final PlaylistEntry_ViewAdapter adapt = new PlaylistEntry_ViewAdapter(self, R.layout.playlist_element, contents);
                final ListView lst = (ListView) findViewById(R.id.wPlaylistElements);
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

    private static class PlaylistEntry_ViewAdapter extends ArrayAdapter<VlcConnector.PlaylistEntry> {
        private List<VlcConnector.PlaylistEntry> items;
        private int layoutResourceId;
        private Context context;

        public static class Row {
            VlcConnector.PlaylistEntry values;
            ImageView wPlaylistElement_FileStatus;
            TextView wPlaylistElement_Name;
            TextView wPlaylistElement_Duration;
            ImageButton wPlaylistElement_Remove;
        }

        public PlaylistEntry_ViewAdapter(Context context, int layoutResourceId, List<VlcConnector.PlaylistEntry> items) {
            super(context, layoutResourceId, items);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View row = inflater.inflate(layoutResourceId, parent, false);

            Row holder = new Row();
            holder.values = items.get(position);

            holder.wPlaylistElement_FileStatus = (ImageView)row.findViewById(R.id.wPlaylistElement_FileStatus);
            // holder.dirOrFile.setImageResource();

            holder.wPlaylistElement_Name = (TextView)row.findViewById(R.id.wPlaylistElement_Name);
            holder.wPlaylistElement_Name.setText(holder.values.name);
            holder.wPlaylistElement_Name.setTag(holder.values);

            holder.wPlaylistElement_Duration = (TextView)row.findViewById(R.id.wPlaylistElement_Duration);
            holder.wPlaylistElement_Duration.setText(String.valueOf(holder.values.duration));
            holder.wPlaylistElement_Duration.setTag(holder.values);

            holder.wPlaylistElement_Remove = (ImageButton)row.findViewById(R.id.wPlaylistElement_Remove);
            holder.wPlaylistElement_Remove.setTag(String.valueOf(holder.values));

            row.setTag(holder);

            return row;
        }
    }
}
