package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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

public class DirListingFragment extends Fragment implements View.OnClickListener {
    public DirListingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vw = inflater.inflate(R.layout.fragment_dir_listing, container, false);
        requestDirectoryList();
        return vw;
    }

    private static class DirListEntry_ViewAdapter extends ArrayAdapter<VlcConnector.DirListEntry> {
        private static final int layoutResourceId = R.layout.fragment_dir_listing_list_element;

        final private List<VlcConnector.DirListEntry> items;
        final private Context context;
        final private View.OnClickListener onClickCallback;

        public DirListEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context, List<VlcConnector.DirListEntry> items) {
            super(context, layoutResourceId, items);
            this.context = context;
            this.items = items;
            this.onClickCallback = onClickCallback;
        }

        public static class Row {
            VlcConnector.DirListEntry values;
            ImageView dirOrFile;
            TextView fName;
            ImageButton actionButton;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View row = inflater.inflate(layoutResourceId, parent, false);

            Row holder = new Row();
            holder.values = items.get(position);

            holder.dirOrFile = (ImageView)row.findViewById(R.id.wDirListElement_DirOrFile);
            // holder.dirOrFile.setImageResource();

            holder.fName = (TextView)row.findViewById(R.id.wDirListElement_Name);
            holder.fName.setText(holder.values.name);
            holder.fName.setTag(holder.values);
            holder.fName.setOnClickListener(onClickCallback);

            holder.actionButton = (ImageButton)row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);
            holder.actionButton.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }

    String currentPath = "/home/laptus";

    private void requestDirectoryList() {
        final FragmentActivity activity = getActivity();
        final DirListingFragment self = this;

        VlcConnector vlc = new VlcConnector("http://192.168.1.5:8080/", "qwepoi");
        vlc.getDirList(currentPath, new VlcConnector.DirListCallback() {
            @Override
            public void fetchDirList_Response(String requestedPath, List<VlcConnector.DirListEntry> contents) {
                final DirListEntry_ViewAdapter adapt = new DirListEntry_ViewAdapter(self, activity, contents);
                // TODO: Clean & set adapt instead of new?
                ((ListView) activity.findViewById(R.id.wDirListing_List)).setAdapter(adapt);
                adapt.notifyDataSetChanged();
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

    @Override
    public void onClick(View v) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                // Assert(item != null) TODO
                Log.i("HOLA", "Adding " + item.path);
                break;

            case R.id.wDirListElement_Name:
                // Assert(item != null) TODO
                if (item.isDirectory) {
                    currentPath = item.path;
                    requestDirectoryList();
                }
                break;

            default:
                // Assert(WTF) // TODO
        }
    }
}
