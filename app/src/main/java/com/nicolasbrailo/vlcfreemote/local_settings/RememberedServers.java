package com.nicolasbrailo.vlcfreemote.local_settings;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.nicolasbrailo.vlcfreemote.model.Server;

import java.util.ArrayList;
import java.util.List;

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
    public Server getLastUsedServer() {
        final String query = "SELECT * " +
                             "   FROM "  + TABLE_NAME +
                             "  WHERE "  + COLUMN_LAST_USED+ " =?";
        final String[] args = new String[]{"1"};

        final Server[] dbSrv = new Server[1];
        dbSrv[0] = null;

        readQuery(query, args, new String[]{COLUMN_IP, COLUMN_VLCPORT, COLUMN_PASS, COLUMN_LAST_PATH},
                new QueryReadCallback() {
            @Override
            public void onCursorReady(Cursor res) {
                if (res.getCount() == 1) {
                    res.moveToFirst();
                    dbSrv[0] = readServerFrom(res);
                }
            }
        });

        if (dbSrv[0] == null) {
            Log.w(getClass().getSimpleName(), "Multiple last used servers. Will reset flag.");
            resetLastUsedServer();
        }

        return dbSrv[0];
    }

    /**
     * Returns the remembered server for an ip:port set (in a Server object). This is useful
     * to retrieve the saved settings (ie last path and password)
     * @param srv ip:port that needs a password
     * @return Known last password, or null if not known
     */
    public Server getRememberedServer(final Server srv)  {
        final String query = "SELECT * " +
                             "  FROM " + TABLE_NAME +
                             " WHERE " + COLUMN_IP + " =? " +
                             "   AND " + COLUMN_VLCPORT + " =? ";

        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};

        final Server[] dbSrv = new Server[1];
        dbSrv[0] = null;

        readQuery(query, args, new String[]{COLUMN_IP, COLUMN_VLCPORT, COLUMN_PASS, COLUMN_LAST_PATH},
                new QueryReadCallback() {
            @Override
            public void onCursorReady(Cursor res) {
                if (res.getCount() == 1) {
                    res.moveToFirst();
                    dbSrv[0] = readServerFrom(res);
                }
            }
        });

        return dbSrv[0];
    }

    /**
     * Returns the last N used server, sorted by IP
     * @param count Expected number of servers
     * @return List of servers
     */
    public List<Server> getLastUsedServers(final int count) {
        final String query = "SELECT * FROM " + TABLE_NAME +
                " ORDER BY " + COLUMN_IP + " LIMIT ? ";

        final String[] args = new String[]{String.valueOf(count)};

        final List<Server> results = new ArrayList<>();
        readQuery(query, args, new String[]{COLUMN_IP, COLUMN_VLCPORT, COLUMN_PASS, COLUMN_LAST_PATH}, new QueryReadCallback() {
            @Override
            public void onCursorReady(Cursor res) {
                while (res.moveToNext()) {
                    results.add(readServerFrom(res));
                }
            }
        });

        return results;
    }

    private Server readServerFrom(final Cursor c)  {
        final String ip;
        final Integer port;
        final String pass;
        final String lastPath;

        try {
            ip = c.getString(c.getColumnIndexOrThrow(COLUMN_IP));
            port = c.getInt(c.getColumnIndexOrThrow(COLUMN_VLCPORT));
            pass = c.getString(c.getColumnIndexOrThrow(COLUMN_PASS));
            lastPath = c.getString(c.getColumnIndexOrThrow(COLUMN_LAST_PATH));
        } catch (Exception e) {
            return null;
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

    public void forget(Server srv) {
        final String query = " DELETE FROM " + TABLE_NAME + " WHERE " +
                                COLUMN_IP + " = ? AND " +
                                COLUMN_VLCPORT + " = ?";
        final String[] args = new String[]{srv.ip, String.valueOf(srv.vlcPort)};
        run(query, args);
    }
}
