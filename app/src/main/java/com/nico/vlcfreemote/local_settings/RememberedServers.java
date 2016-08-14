package com.nico.vlcfreemote.local_settings;

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
     * Remember a server and its settings. Will automatically set the last used flag.
     * @param srv Last used server
     */
    public void rememberServer(final Server srv) {
        final String query = " REPLACE INTO " + TABLE_NAME + "( " +
                                    COLUMN_IP + "," +
                                    COLUMN_VLCPORT + "," +
                                    COLUMN_PASS + "," +
                                    COLUMN_LAST_PATH + ", " +
                                    COLUMN_LAST_USED +
                             " ) VALUES ( ?, ?, ?, ?, 1)";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort),
                                           srv.getPassword(), srv.getLastPath()};

        run(query, args);
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
    public Server getLastUsedServer() throws LocalSettingsError {
        final String query = "SELECT * "+
                             "  FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_LAST_USED+ " =?";
        final String[] args = new String[]{"1"};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if (res.getCount() > 1) {
            Log.w(getClass().getSimpleName(), "Multiple last used servers. Will reset flag.");
            resetLastUsedServer();
        }

        return readServerFrom(res);
    }

    /**
     * Returns the remembered server for an ip:port set (in a Server object). This is useful
     * to retrieve the saved settings (ie last path and password)
     * @param srv ip:port that needs a password
     * @return Known last password, or null if not known
     */
    public Server getRememberedServer(final Server srv) throws LocalSettingsError {
        final String query = "SELECT * " +
                             "  FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_IP + " =? " +
                             "   AND " + COLUMN_VLCPORT + " =? ";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};

        Cursor res = getReadableDatabase().rawQuery(query, args);

        if ((res.getCount() > 1) || (res.getColumnIndex(COLUMN_PASS) == -1)) {
            // End of world exception: either unique constrain failed in sqlite or the pass column
            // is unknown. In any case, something is horribly wrong and we can't recover.
            res.close();
            throw new LocalSettingsError();
        }

        return readServerFrom(res);
    }

    private Server readServerFrom(final Cursor c) throws LocalSettingsError {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }

        final String ip;
        final Integer port;
        final String pass;
        final String lastPath;

        c.moveToFirst();
        try {
            ip = c.getString(c.getColumnIndexOrThrow(COLUMN_IP));
            port = c.getInt(c.getColumnIndexOrThrow(COLUMN_VLCPORT));
            pass = c.getString(c.getColumnIndexOrThrow(COLUMN_PASS));
            lastPath = c.getString(c.getColumnIndexOrThrow(COLUMN_LAST_PATH));
        } catch (Exception e) {
            throw new LocalSettingsError();
        } finally {
            c.close();
        }

        Server srv = new Server(ip, port, null);
        srv.setPassword(pass);
        srv.setLastPath(lastPath);
        return srv;
    }

    /**
     * Updates the last known path for a server
     * @param srv Current server
     */
    public void saveLastPathForServer(final Server srv) {
        final String query = "UPDATE " + TABLE_NAME +
                             "   SET " + COLUMN_LAST_PATH + "=? " +
                             " WHERE " + COLUMN_IP + "=? " +
                             "   AND " + COLUMN_VLCPORT + "=? ";
        final String[] args = new String[]{srv.getLastPath(), srv.ip, String.valueOf(srv.vlcPort)};
        run(query, args);
    }
}
