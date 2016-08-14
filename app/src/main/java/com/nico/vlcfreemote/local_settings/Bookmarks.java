package com.nico.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.nico.vlcfreemote.net_utils.Server;

import java.util.ArrayList;
import java.util.List;


public class Bookmarks extends LocalSettings {

    public static final String TABLE_NAME = "bookmarks";
    public static final String COLUMN_IP = "ip";
    public static final String COLUMN_VLCPORT = "port";
    public static final String COLUMN_PATH = "path";

    public Bookmarks(Context context) {
        super(context);
    }

    @Override
    protected String getDeleteTableSQL() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    @Override
    protected String getCreateTableSQL() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_IP + " TEXT, " +
                COLUMN_VLCPORT + " INTEGER, " +
                COLUMN_PATH + " TEXT, " +
                "PRIMARY KEY ("+ COLUMN_IP + "," + COLUMN_VLCPORT + ", " + COLUMN_PATH + ") " +
                " )";
    }

    public void addBookmark(final Server srv, final String path) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_IP, srv.ip);
        values.put(COLUMN_VLCPORT, srv.vlcPort);
        values.put(COLUMN_PATH, path);

        try {
            insert(TABLE_NAME, values);
        } catch (SQLiteConstraintException ignored) {
            // If a unique constraint fails, it means the bookmark was already there
        }
    }

    public List<String> getBookmarks(final Server srv) {
        final String query =
            "SELECT " + COLUMN_PATH + " FROM " + TABLE_NAME +
            " WHERE " + COLUMN_IP + " = ? " +
            " AND " + COLUMN_VLCPORT + " = ? ";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getColumnIndex(COLUMN_PATH) == -1) {
            res.close();
            // TODO throw new IOException("ASD");
        }

        final List<String> results = new ArrayList<>();
        while (res.moveToNext()) {
            results.add(res.getString(res.getColumnIndex(COLUMN_PATH)));
        }

        res.close();

        return results;
    }

    public void deleteBookmark(final Server srv, final String path) {
        final String query = "DELETE FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_IP + "=? " +
                             " AND " + COLUMN_VLCPORT + "=?" +
                             " AND " + COLUMN_PATH + "=?";
        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort), path};
        run(query, args);
    }

    public void forgetAllBookmarks(final Server srv) {
        final String query = "DELETE FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_IP + "=? " +
                             " AND " + COLUMN_VLCPORT + "=?";
        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};
        run(query, args);
    }
}
