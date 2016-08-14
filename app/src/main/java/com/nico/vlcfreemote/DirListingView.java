package com.nico.vlcfreemote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nico.vlcfreemote.model.VlcPath;
import com.nico.vlcfreemote.model.Server;
import com.nico.vlcfreemote.vlc_connector.Cmd_DirList;
import com.nico.vlcfreemote.vlc_connector.RemoteVlc;
import com.nico.vlcfreemote.vlc_connector.VlcCommand;

import java.util.ArrayList;
import java.util.List;


public class DirListingView extends VlcFragment
                            implements View.OnClickListener,
                                       VlcPath.UICallback {

    public interface DirListingCallback {
        void onAddToPlaylistRequested(final String uri);
    }

    private DirListEntry_ViewAdapter dirViewAdapter;
    private DirListingCallback callback = null;
    private VlcPath vlcPath = null;
    private VlcCommand.GeneralCallback systemFailCallback = null;

    /************************************************************/
    /* Mostly Android boilerplate                               */
    /************************************************************/
    public DirListingView() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dir_listing_view, container, false);
        dirViewAdapter = new DirListEntry_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wDirListing_List)).setAdapter(dirViewAdapter);
        v.findViewById(R.id.wDirListing_Bookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_JumpToBookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_ManageBookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_PlayRandom).setOnClickListener(this);
        return v;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            this.callback = (DirListingCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DirListingCallback");
        }

        try {
            this.vlcPath = new VlcPath((RemoteVlc.ConnectionProvider) context, context, this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement RemoteVlc.ConnectionProvider");
        }

        try {
            this.systemFailCallback = (VlcCommand.GeneralCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement VlcCommand.GeneralCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.callback = null;
        this.vlcPath = null;
        this.systemFailCallback = null;
    }

    /************************************************************/
    /* Display & event handling                                 */
    /************************************************************/
    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);

        if (visible && isResumed())
        {
            triggerCurrentPathListUpdate();
        }
    }

    @Override
    public void onClick(View v) {
        Cmd_DirList.DirListEntry item = (Cmd_DirList.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                if (item == null) throw new RuntimeException(DirListingView.class.getName() + " received a menu item with no tag");
                onAddToPlaylistRequested(item);
                break;

            case R.id.wDirListElement_Name:
                if (item == null) throw new RuntimeException(DirListingView.class.getName() + " received a menu item with no tag");
                if (item.isDirectory) {
                    vlcPath.cd(item.path, item.human_friendly_path);
                    triggerCurrentPathListUpdate();
                } else {
                    onAddToPlaylistRequested(item);
                }

                break;

            case R.id.wDirListing_Bookmark:
                saveCurrentPathAsBookmark();
                break;

            case R.id.wDirListing_JumpToBookmark:
                jumpToBookmark();
                break;

            case R.id.wDirListing_ManageBookmark:
                deleteBookmark();
                break;

            case R.id.wDirListing_PlayRandom:
                // TODO playRandomSubDir(currentPath);
                break;

            default:
                throw new RuntimeException(DirListingView.class.getName() + " received a click event it can't handle.");
        }
    }

    /************************************************************/
    /* UI stuff                                                 */
    /************************************************************/
    private void triggerCurrentPathListUpdate() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        vlcPath.updateDirContents();

        dirViewAdapter.clear();
        ((TextView) activity.findViewById(R.id.wDirListing_CurrentPath)).setText(vlcPath.getPrettyCWD());
        activity.findViewById(R.id.wDirListing_List).setEnabled(false);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.VISIBLE);

    }

    @Override
    public void onNewDirListAvailable(List<Cmd_DirList.DirListEntry> results) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wDirListing_List).setEnabled(true);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.GONE);

        dirViewAdapter.clear();
        dirViewAdapter.addAll(results);
    }

    private void onAddToPlaylistRequested(final Cmd_DirList.DirListEntry path) {
        callback.onAddToPlaylistRequested(path.path);


        if (! path.isDirectory) {
            vlcPath.onAddToPlaylistRequested(path.path);
        }
    }

    @Override
    public void onDirListFatalFailure(VlcPath.VlcPath_ApplicationError ex) {
        systemFailCallback.onSystemError(ex);
    }

    public void onServerChanged(final Server srv) {
        if (vlcPath != null) vlcPath.onServerChanged(srv);
    }

    private void saveCurrentPathAsBookmark() {
        vlcPath.bookmarkCurrentDirectory();

        final String msg = String.format(getResources().getString(R.string.dir_listing_saved_bookmark), vlcPath.getPrettyCWD());
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    // TODO: Move to a Fragment?
    private interface BookmarkCallback {
        void onBookmarkSelected(final String uri, final String prettyName);
    }

    private void jumpToBookmark() {
        displayBookmarkPicker(R.string.R_string_dir_listing_goto_bookmark_title, new BookmarkCallback() {
            @Override
            public void onBookmarkSelected(String uri, String prettyName) {
                vlcPath.cd(uri, prettyName);
                triggerCurrentPathListUpdate();
            }
        });
    }

    private void deleteBookmark() {
        displayBookmarkPicker(R.string.R_string_dir_listing_delete_bookmark_title, new BookmarkCallback() {
            @Override
            public void onBookmarkSelected(String uri, String prettyName) {
                vlcPath.deleteBookmark(uri);
            }
        });
    }

    private void displayBookmarkPicker(int titleStringId, final BookmarkCallback cb) {
        List<String> bookmarks = vlcPath.getBookmarks();

        final List<String> pathDisplayNames = new ArrayList<>();
        final List<String> pathUris = new ArrayList<>();
        for (String bookmark : bookmarks) {
            pathUris.add(bookmark);
            pathDisplayNames.add(bookmark);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(titleStringId));
        builder.setItems(pathDisplayNames.toArray(new String[pathDisplayNames.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String uri = pathUris.get(which);
                final String prettyName = pathDisplayNames.get(which);
                cb.onBookmarkSelected(uri, prettyName);
            }
        });

        builder.show();
    }


    /************************************************************/
    /* List view stuff                                          */
    /************************************************************/
    private static class DirListEntry_ViewAdapter extends ArrayAdapter<Cmd_DirList.DirListEntry> {
        private static final int layoutResourceId = R.layout.fragment_dir_listing_list_element;

        final private LayoutInflater inflater;
        final private View.OnClickListener onClickCallback;

        public DirListEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<Cmd_DirList.DirListEntry>());
            this.inflater = ((Activity) context).getLayoutInflater();
            this.onClickCallback = onClickCallback;
        }

        public static class Row {
            Cmd_DirList.DirListEntry values;
            ImageView dirOrFile;
            TextView fName;
            ImageView alreadySeen;
            ImageButton actionButton;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View row;
            if (convertView == null) {
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.values = this.getItem(position);

            holder.dirOrFile = (ImageView)row.findViewById(R.id.wDirListElement_DirOrFile);
            if (!holder.values.isDirectory) holder.dirOrFile.setVisibility(View.INVISIBLE);

            holder.fName = (TextView)row.findViewById(R.id.wDirListElement_Name);
            holder.fName.setText(holder.values.name);
            holder.fName.setTag(holder.values);
            holder.fName.setOnClickListener(onClickCallback);

            holder.alreadySeen = (ImageView)row.findViewById(R.id.wDirListElement_AlreadySeen);
            holder.alreadySeen.setOnClickListener(onClickCallback);
            if (holder.values.wasPlayedBefore) {
                holder.alreadySeen.setVisibility(View.VISIBLE);
            } else {
                holder.alreadySeen.setVisibility(View.INVISIBLE);
            }

            holder.actionButton = (ImageButton)row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);
            holder.actionButton.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
