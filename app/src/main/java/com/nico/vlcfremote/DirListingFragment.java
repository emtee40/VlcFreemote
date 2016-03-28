package com.nico.vlcfremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import android.widget.Toast;

import com.nico.vlcfremote.utils.VlcActionFragment;
import com.nico.vlcfremote.utils.VlcConnector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DirListingFragment extends VlcActionFragment implements View.OnClickListener {

    // Is this Windows compatible? Who knows...
    private static final String VLC_DEFAULT_START_PATH = "~";

    // A char that can never appear in a path, used to separate two paths in a setting
    private static final char BOOKMARK_PATHS_SEPARATOR = '|';

    String currentPath;
    String currentPath_display;
    DirListEntry_ViewAdapter dirViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dir_listing, container, false);
        dirViewAdapter = new DirListEntry_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wDirListing_List)).setAdapter(dirViewAdapter);
        v.findViewById(R.id.wDirListing_Bookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_JumpToBookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_ManageBookmark).setOnClickListener(this);
        v.findViewById(R.id.wDirListing_PlayRandom).setOnClickListener(this);
        currentPath = VLC_DEFAULT_START_PATH;
        currentPath_display = getResources().getString(R.string.dir_listing_default_folder_label);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If we're visible and belong to an activity (?) do an update
        if (getUserVisibleHint() && isAdded()) updateDirectoryList();
    }

    public void updateDirectoryList() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        vlcConnection.getVlcConnector().getDirList(currentPath);
        ((TextView) activity.findViewById(R.id.wDirListing_CurrentPath)).setText(currentPath_display);

        dirViewAdapter.clear();
        activity.findViewById(R.id.wDirListing_List).setEnabled(false);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.VISIBLE);
    }

    public void Vlc_OnDirListingFetched(List<VlcConnector.DirListEntry> contents) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wDirListing_List).setEnabled(true);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.GONE);

        dirViewAdapter.addAll(contents);
    }

    public void Vlc_OnSelectDirIsInvalid() {
        currentPath = VLC_DEFAULT_START_PATH;
        currentPath_display = getResources().getString(R.string.dir_listing_default_folder_label);
        updateDirectoryList();
    }

    private void addPathToPlaylist(final String path) {
        vlcConnection.getVlcConnector().addToPlayList(path);
        if (!vlcConnection.getVlcConnector().getLastKnownStatus().isCurrentlyPlayingSomething()) {
            vlcConnection.getVlcConnector().playNext();
        }
        vlcConnection.getVlcConnector().updatePlaylist();
    }

    @Override
    public void onClick(View v) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                addPathToPlaylist(item.path);
                break;

            case R.id.wDirListElement_Name:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                if (item.isDirectory) {
                    currentPath = item.path;
                    currentPath_display = item.human_friendly_path;
                    updateDirectoryList();
                } else {
                    addPathToPlaylist(item.path);
                }

                break;

            case R.id.wDirListing_Bookmark:
                saveBookmark(this.currentPath_display, this.currentPath);
                break;

            case R.id.wDirListing_JumpToBookmark:
                gotoBookmark();
                break;

            case R.id.wDirListing_ManageBookmark:
                deleteBookmark();
                break;

            case R.id.wDirListing_PlayRandom:
                playRandomSubDir(currentPath);
                break;

            default:
                throw new RuntimeException(DirListingFragment.class.getName() + " received a click event it can't handle.");
        }
    }

    private void playRandomSubDir(final String startPath) {
        Log.i(this.getClass().getName(), "Picking random directory from start point " + startPath);

        final DirListingFragment self = this;
        vlcConnection.getVlcConnector().getDirList(startPath, new VlcConnector.Callback_OnDirListingFetched() {
            @Override
            public void Vlc_OnDirListingFetched(String requestedPath, List<VlcConnector.DirListEntry> contents) {
                List<String> subDirs = new ArrayList<>();
                for (VlcConnector.DirListEntry i : contents) {
                    if (i.isDirectory && (!i.name.equals(".."))) subDirs.add(i.path);
                }

                if (subDirs.isEmpty()) {
                    self.addPathToPlaylist(requestedPath);
                } else {
                    final Random rand = new Random();
                    final int i = rand.nextInt(subDirs.size());
                    // TODO: Refactor rec call out
                    self.playRandomSubDir(subDirs.get(i));
                }
            }

            @Override
            public void Vlc_OnSelectDirIsInvalid(String path) {
                Log.e("DirListing", "Error fetching dir list: invalid dir");
            }

            @Override
            public void Vlc_OnLoginIncorrect() {
                Log.e("DirListing", "Error fetching dir list: invalid login");
            }

            @Override
            public void Vlc_OnConnectionFail() {
                Log.e("DirListing", "Error fetching dir list: connection failed");
            }

            @Override
            public void Vlc_OnInternalError(Throwable ex) {
                Log.e("DirListing", "Error fetching dir list: internal error");
            }

            @Override
            public void Vlc_OnInvalidResponseReceived(Throwable ex) {
                Log.e("DirListing", "Error fetching dir list: invalid server response");
            }
        });
    }

    private void saveBookmark(final String pathDisplayName, final String pathUri) {
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        final HashSet<String> bookmarks = getSavedBookmarks(activity);
        bookmarks.add(pathUri + BOOKMARK_PATHS_SEPARATOR + pathDisplayName);
        saveBookmarksToPrefs(activity, bookmarks);

        final String msg = String.format(getResources().getString(R.string.dir_listing_saved_bookmark), pathDisplayName);
        Toast toast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    private interface BookmarkSelected {
        void callback(final String pathUri, final String pathName);
    }

    private void gotoBookmark() {
        displayBookmarkPicker(R.string.R_string_dir_listing_goto_bookmark_title, new BookmarkSelected() {
            @Override
            public void callback(String pathUri, String pathName) {
                currentPath = pathUri;
                currentPath_display = pathName;
                updateDirectoryList();
            }
        });
    }

    private void deleteBookmark() {
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        displayBookmarkPicker(R.string.R_string_dir_listing_delete_bookmark_title, new BookmarkSelected() {
            @Override
            public void callback(String pathUri, String pathName) {
                final String bookmarkToDelete = pathUri + BOOKMARK_PATHS_SEPARATOR + pathName;
                HashSet<String> newBookmarks = new HashSet<>();
                for (String bookmark : getSavedBookmarks(activity)) {
                    if (!bookmarkToDelete.equals(bookmark)) {
                        newBookmarks.add(bookmark);
                    }
                }

                saveBookmarksToPrefs(activity, newBookmarks);
            }
        });
    }

    private void displayBookmarkPicker(int titleStringId, final BookmarkSelected cb) {
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        final List<String> pathDisplayNames = new ArrayList<>();
        final List<String> pathUris = new ArrayList<>();
        for (String bookmark : getSavedBookmarks(activity)) {
            final String pathUri = bookmark.substring(0, bookmark.indexOf(BOOKMARK_PATHS_SEPARATOR));
            final String pathDisplayName = bookmark.substring(bookmark.indexOf(BOOKMARK_PATHS_SEPARATOR)+1);
            pathUris.add(pathUri);
            pathDisplayNames.add(pathDisplayName);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(titleStringId));
        builder.setItems(pathDisplayNames.toArray(new String[pathDisplayNames.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cb.callback(pathUris.get(which), pathDisplayNames.get(which));
            }
        });
        builder.show();
    }

    private HashSet<String> getSavedBookmarks(final FragmentActivity activity) {
        // Get the known bookmarks for the current server
        Set<String> oldBookmarks = activity.getPreferences(Context.MODE_PRIVATE).getStringSet(getBookmarksPrefKey(), null);
        HashSet<String> newBookmarks = new HashSet<>();

        // I have no idea why I can't reuse the same oldBookmarks object, but if I do so stuff
        // doesn't get saved properly
        if (oldBookmarks != null) {
            for (String s : oldBookmarks) {
                newBookmarks.add(s);
            }
        }

        return newBookmarks;
    }

    private void saveBookmarksToPrefs(final Activity activity, final HashSet<String> bookmarks) {
        SharedPreferences.Editor cfg = activity.getPreferences(Context.MODE_PRIVATE).edit();
        cfg.putStringSet(getBookmarksPrefKey(), bookmarks);
        cfg.apply();
    }

    private String getBookmarksPrefKey() {
        // Bookmarks are kept per server
        return vlcConnection.getVlcConnector().getServerUrl() + "_dirBookmarks";
    }

    private static class DirListEntry_ViewAdapter extends ArrayAdapter<VlcConnector.DirListEntry> {
        private static final int layoutResourceId = R.layout.fragment_dir_listing_list_element;

        final private LayoutInflater inflater;
        final private View.OnClickListener onClickCallback;

        public DirListEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<VlcConnector.DirListEntry>());
            this.inflater = ((Activity) context).getLayoutInflater();
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

            holder.actionButton = (ImageButton)row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);
            holder.actionButton.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
