package com.nico.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.nico.vlcfreemote.net_utils.Server;

public class RememberedServers extends SQLiteOpenHelper {

    private static abstract class RememberedServer implements BaseColumns {
        public static final String DB_NAME = "remembered_servers.db";
        public static final int DB_VERSION = 1;

        public static final String TABLE_NAME = "remembered_server";
        public static final String COLUMN_IP = "ip";
        public static final String COLUMN_VLCPORT = "port";
        // TODO: Is it worth encrypting this?
        public static final String COLUMN_PASS = "pass";
        public static final String COLUMN_LAST_USED = "last_used";
        public static final String COLUMN_LAST_PATH = "last_path";


        private static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_IP + " TEXT, " +
                        COLUMN_VLCPORT + " INTEGER, " +
                        COLUMN_PASS + " TEXT, " +
                        COLUMN_LAST_USED + " INTEGER, " +
                        COLUMN_LAST_PATH + " TEXT, " +
                        "PRIMARY KEY ("+ COLUMN_IP + "," + COLUMN_VLCPORT + ") " +
                        " )";

        private static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    private final Context context;

    public RememberedServers(Context context) {
        super(context, RememberedServer.DB_NAME, null, RememberedServer.DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RememberedServer.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // This DB doesn't save anything important, just delete everything
        db.execSQL(RememberedServer.SQL_DELETE_TABLE);
        db.execSQL(RememberedServer.SQL_CREATE_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int i, int i1) {
        onUpgrade(db, i, i1);
    }


    private <Args> void run(final String query, Args[] args) {
        SQLiteDatabase db = this.getWritableDatabase();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            db.execSQL(query, args);
        } finally {
            db.close();
        }
    }

    private void forgetServers(String ip, int vlc_port) {
        final String query =
                "DELETE FROM " + RememberedServer.TABLE_NAME + " WHERE " +
                        RememberedServer.COLUMN_IP + "=? AND " +
                        RememberedServer.COLUMN_VLCPORT + "=?";
        final String[] args = new String[]{ip, String.valueOf(vlc_port)};
        run(query, args);

        (new Bookmarks(context)).forgetAllBookmarks(new Server(ip, vlc_port, null));
    }

    private void resetLastUsedServer() {
        final String query =
                "UPDATE " + RememberedServer.TABLE_NAME +
                        " SET " + RememberedServer.COLUMN_LAST_USED + "=?";
        final String[] args = new String[]{"0"};
        run(query, args);
    }

    public void rememberServer(Server srv) {
        if (srv.vlcPort != null) {
            forgetServers(srv.ip, srv.vlcPort);
            resetLastUsedServer();
        }

        ContentValues values = new ContentValues();
        values.put(RememberedServer.COLUMN_IP, srv.ip);
        values.put(RememberedServer.COLUMN_VLCPORT, srv.vlcPort);
        values.put(RememberedServer.COLUMN_LAST_USED, 1);
        values.put(RememberedServer.COLUMN_PASS, srv.getPassword());

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.insert(
                    RememberedServer.TABLE_NAME,
                    null,
                    values);
        } finally {
            db.close();
        }
    }

    private Server getRememberedServer(final String ip, final int vlc_port) {
        final String query =
                "SELECT " + RememberedServer.COLUMN_PASS + " FROM " + RememberedServer.TABLE_NAME +
                " WHERE " + RememberedServer.COLUMN_IP + " = ? AND " +
                            RememberedServer.COLUMN_VLCPORT + " = ? ";

        final String[] args = new String[]{ip, String.valueOf(vlc_port)};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() == 0) {
            return null;
        }

        if (res.getCount() > 1) {
            Log.w(getClass().getSimpleName(), "Multiple remembered servers for " + ip + ":" +
                                               vlc_port + ". Will truncate table and return random.");
            forgetServers(ip, vlc_port);
        }

        if (res.getColumnIndex(RememberedServer.COLUMN_PASS) == -1) {
            res.close();
            // TODO throw new IOException("ASD");
        }

        res.moveToFirst();
        final String pass = res.getString(res.getColumnIndex(RememberedServer.COLUMN_PASS));
        res.close();

        Server srv = new Server(ip, vlc_port, null);
        srv.setPassword(pass);
        return srv;
    }

    public String getRememberedPassword(final String ip, final int vlc_port) {
        Server srv = this.getRememberedServer(ip, vlc_port);
        if (srv != null) {
            return srv.password;
        } else {
            return "";
        }
    }

    public Server getLastUsedServer() {
        final String query =
                "SELECT * FROM " + RememberedServer.TABLE_NAME +
                        " WHERE " + RememberedServer.COLUMN_LAST_USED+ " = ?";
        final String[] args = new String[]{"1"};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() == 0) {
            return null;
        }

        if (res.getCount() > 1) {
            Log.w(getClass().getSimpleName(),
                    "Multiple last used servers. Will truncate table and return null.");
            resetLastUsedServer();
            return null;
        }

        final String ip;
        final Integer port;
        final String pass;
        res.moveToFirst();
        try {
            ip = res.getString(res.getColumnIndexOrThrow(RememberedServer.COLUMN_IP));
            port = res.getInt(res.getColumnIndexOrThrow(RememberedServer.COLUMN_VLCPORT));
            pass = res.getString(res.getColumnIndexOrThrow(RememberedServer.COLUMN_PASS));
        } catch (Exception e) {
            // TODO throw new IOException("ASD");
            throw e;
        } finally {
            res.close();
        }

        Server srv = new Server(ip, port, null);
        srv.setPassword(pass);
        return srv;
    }

    public void saveLastPathForServer(String path, final Server srv) {
        Log.e("XXXXXXXX", "SAVED " + path);
        final String query =
                "UPDATE " + RememberedServer.TABLE_NAME +
                " SET " + RememberedServer.COLUMN_LAST_PATH + "=? " +
                " WHERE " + RememberedServer.COLUMN_IP + "=? " +
                "   AND " + RememberedServer.COLUMN_VLCPORT + "=? ";
        final String[] args = new String[]{path, srv.ip, String.valueOf(srv.vlcPort)};
        run(query, args);
    }

    public String getLastPathForServer(final Server srv) {
        final String query =
                "SELECT " + RememberedServer.COLUMN_LAST_PATH  +
                " FROM " + RememberedServer.TABLE_NAME +
                " WHERE " + RememberedServer.COLUMN_IP + " = ? " +
                " AND " + RememberedServer.COLUMN_VLCPORT + " = ? ";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() == 0) {
            return null;
        }

        if (res.getCount() > 1) {
            Log.w(getClass().getSimpleName(), "Multiple remembered servers for " + srv.ip + ":" +
                    srv.vlcPort + ". Will truncate table and return random.");
            // TODO: WTF? Set table unique
        }

        if (res.getColumnIndex(RememberedServer.COLUMN_LAST_PATH) == -1) {
            res.close();
            // TODO throw new IOException("ASD");
        }

        res.moveToFirst();
        final String lastPath = res.getString(res.getColumnIndex(RememberedServer.COLUMN_LAST_PATH));
        res.close();

        return lastPath;
    }
}
