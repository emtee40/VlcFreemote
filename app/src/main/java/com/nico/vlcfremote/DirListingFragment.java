package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcActionFragment;
import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class DirListingFragment extends VlcActionFragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dir_listing, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDirectoryList();
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
            if (!holder.values.isDirectory) holder.dirOrFile.setVisibility(View.INVISIBLE);

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

    String currentPath = "~";
    String currentPath_display = "Home directory";

    public void updateDirectoryList() {
        ((TextView) getActivity().findViewById(R.id.wDirListing_CurrentPath)).setText(currentPath_display);
        vlcConnection.getVlcConnector().getDirList(currentPath);
    }

    public void Vlc_OnDirListingFetched(String requestedPath, List<VlcConnector.DirListEntry> contents) {
        final DirListEntry_ViewAdapter adapt = new DirListEntry_ViewAdapter(this, getActivity(), contents);
        // TODO: Clean & set adapt instead of new?
        ((ListView) getActivity().findViewById(R.id.wDirListing_List)).setAdapter(adapt);
        adapt.notifyDataSetChanged();
    }

    public void Vlc_OnSelectDirIsInvalid(String requestedPath) {
        currentPath = "~";
        currentPath_display = "Home directory";
    }

    public void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId) {
        // TODO?
    }

    @Override
    public void onClick(View v) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                vlcConnection.getVlcConnector().addToPlayList(item.path);
                break;

            case R.id.wDirListElement_Name:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                if (item.isDirectory) {
                    currentPath = item.path;
                    currentPath_display = item.human_friendly_path;
                    updateDirectoryList();
                }
                break;

            default:
                throw new RuntimeException(DirListingFragment.class.getName() + " received a click event it can't handle.");
        }
    }
}
