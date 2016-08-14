package com.nico.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.nico.vlcfreemote.net_utils.Server;

public class RememberedServers extends LocalSettings {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "servers.db";
    private static final String TABLE_NAME = "remembered_server";
    private static final String COLUMN_IP = "ip";
    private static final String COLUMN_VLCPORT = "port";
    private static final String COLUMN_PASS = "pass";   // TODO: Is it worth encrypting this?
    private static final String COLUMN_LAST_USED = "last_used";
    private static final String COLUMN_LAST_PATH = "last_path";

    public RememberedServers(Context context) {
        super(context, DB_NAME, DB_VERSION);
    }

    @Override
    protected String getCreateTableSQL() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_IP + " TEXT, " +
                        COLUMN_VLCPORT + " INTEGER, " +
                        COLUMN_PASS + " TEXT, " +
                        COLUMN_LAST_USED + " INTEGER, " +
                        COLUMN_LAST_PATH + " TEXT, " +
                        "PRIMARY KEY ("+ COLUMN_IP + "," + COLUMN_VLCPORT + ") " +
               " )";
    }

    @Override
    protected String getDeleteTableSQL() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    /**
     * Remember a server and its password. Will automatically set the last used flag.
     * @param srv Last used server
     */
    public void rememberServer(final Server srv) {
        if (srv.vlcPort != null) {
            resetLastUsedServer();
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_IP, srv.ip);
        values.put(COLUMN_VLCPORT, srv.vlcPort);
        values.put(COLUMN_LAST_USED, 1);
        values.put(COLUMN_PASS, srv.getPassword());

        insert(TABLE_NAME, values);
    }

    /**
     * Forget last used server
     */
    private void resetLastUsedServer() {
        final String query = "UPDATE " + TABLE_NAME +
                             "   SET " + COLUMN_LAST_USED + "=?";
        final String[] args = new String[]{"0"};
        run(query, args);
    }

    /**
     * Returns the server with LAST_USED=1. Will return a random one if multiple servers have
     * this column or null if there are no recent servers.
     *
     * @return Last used server
     */
    public Server getLastUsedServer() {
        final String query = "SELECT * "+
                             "  FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_LAST_USED+ " =?";
        final String[] args = new String[]{"1"};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() == 0) {
            return null;
        }

        if (res.getCount() > 1) {
            Log.w(getClass().getSimpleName(), "Multiple last used servers. Will reset flag.");
            resetLastUsedServer();
        }

        final String ip;
        final Integer port;
        final String pass;
        final String lastPath;

        res.moveToFirst();
        try {
            ip = res.getString(res.getColumnIndexOrThrow(COLUMN_IP));
            port = res.getInt(res.getColumnIndexOrThrow(COLUMN_VLCPORT));
            pass = res.getString(res.getColumnIndexOrThrow(COLUMN_PASS));
            lastPath = res.getString(res.getColumnIndexOrThrow(COLUMN_LAST_PATH));
        } catch (Exception e) {
            // TODO throw new IOException("ASD");
            throw e;
        } finally {
            res.close();
        }

        Server srv = new Server(ip, port, null);
        srv.setPassword(pass);
        srv.setLastPath(lastPath);
        return srv;
    }

    /**
     * Returns the password for a remembered server
     * @param srv ip:port that needs a password
     * @return Known last password, or null if not known
     */
    public String getRememberedPassword(final Server srv) {
        final String query = "SELECT " + COLUMN_PASS +
                             "  FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_IP + " =? " +
                             "   AND " + COLUMN_VLCPORT + " =? ";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() == 0) {
            return null;
        }

        if ((res.getCount() > 1) || (res.getColumnIndex(COLUMN_PASS) == -1)) {
            // End of world exception: either unique constrain failed in sqlite or the pass column
            // is unknown. In any case, something is horribly wrong and we can't recover.
            res.close();
            // TODO throw new IOException("ASD");
        }

        res.moveToFirst();
        final String pass = res.getString(res.getColumnIndex(COLUMN_PASS));
        res.close();
        return pass;
    }

    /**
     * Updates the last known path for a server
     * @param srv Current server
     */
    public void saveLastPathForServer(final Server srv) {
        Log.e("XXXXXXXX", "SAVED " + srv.getLastPath());
        final String query = "UPDATE " + TABLE_NAME +
                             "   SET " + COLUMN_LAST_PATH + "=? " +
                             " WHERE " + COLUMN_IP + "=? " +
                             "   AND " + COLUMN_VLCPORT + "=? ";
        final String[] args = new String[]{srv.getLastPath(), srv.ip, String.valueOf(srv.vlcPort)};
        run(query, args);
    }
}
