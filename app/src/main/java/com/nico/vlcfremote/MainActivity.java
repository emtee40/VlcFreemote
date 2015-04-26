package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestDirectoryList();
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

    private static class DirListEntry_ViewAdapter extends ArrayAdapter<VlcConnector.DirListEntry> {
        private List<VlcConnector.DirListEntry> items;
        private int layoutResourceId;
        private Context context;

        public static class IsThisARow {
            VlcConnector.DirListEntry values;
            ImageView dirOrFile;
            TextView fName;
            ImageButton actionButton;
        }

        public DirListEntry_ViewAdapter(Context context, int layoutResourceId, List<VlcConnector.DirListEntry> items) {
            super(context, layoutResourceId, items);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View row = inflater.inflate(layoutResourceId, parent, false);

            IsThisARow holder = new IsThisARow();
            holder.values = items.get(position);

            holder.dirOrFile = (ImageView)row.findViewById(R.id.wDirListElement_DirOrFile);
            // holder.dirOrFile.setImageResource();

            holder.fName = (TextView)row.findViewById(R.id.wDirListElement_Name);
            holder.fName.setText(holder.values.name);
            holder.fName.setTag(holder.values);

            holder.actionButton = (ImageButton)row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);

            row.setTag(holder);

            return row;
        }
    }

    String currentPath = "/home/laptus";

    private void requestDirectoryList() {
        final MainActivity self = this;
        VlcConnector vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
        vlc.getDirList(currentPath, new VlcConnector.DirListCallback() {
            @Override
            public void fetchDirList_Response(String requestedPath, List<VlcConnector.DirListEntry> contents) {
                Log.i("ASD", "CALL CONTENT CB");
                final DirListEntry_ViewAdapter adapt = new DirListEntry_ViewAdapter(self, R.layout.dir_listing_element, contents);
                final ListView dirList = (ListView) findViewById(R.id.wDirList);
                dirList.setAdapter(adapt);
                ((ArrayAdapter) dirList.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void fetchDirList_ConnectionFailure() {
                Log.i("ASD", "CALL CONN FAIL CB");
            }

            @Override
            public void fetchDirList_InvalidResponseReceived(Throwable ex) {
                Log.i("ASD", "CALL INV RESPONSE CB");

            }

            @Override
            public void fetchDirList_InternalError(Throwable ex) {
                Log.i("ASD", "CALL ERR CB");
            }
        });
    }

    public void gotoPlaylist(View view) {
        Intent intent = new Intent(this, PlaylistView.class);
        startActivity(intent);
    }

    public void dirListElement_OnClickCallback(View view) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry)view.getTag();
        if (item.isDirectory) {
            currentPath = item.path;
            requestDirectoryList();
        }
    }

    public void dirListElement_ActionCallback(View view) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry)view.getTag();
        Log.i("HOLA", "Adding " + item.path);
    }
}
