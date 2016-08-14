package com.nico.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public abstract class LocalSettings extends SQLiteOpenHelper implements BaseColumns {

    /**
     * An error condition in the sql layer which can't be recovered.
     * The only real purpose of this class is to force explicit handling in upper layers.
     */
    public static class LocalSettingsError extends Exception {
        @Override
        public String getMessage() { return "Unexpected SQLite error. Try clearing application data."; }
    }

    public LocalSettings(Context context, final String DbName, int DbVersion) {
        super(context, DbName, null, DbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableSQL());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // This DB doesn't save anything important, just delete everything
        db.execSQL(getDeleteTableSQL());
        db.execSQL(getCreateTableSQL());
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int i, int i1) {
        onUpgrade(db, i, i1);
    }

    public void insert(final String table, final ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            db.insertOrThrow(table, null, values);
        } finally {
            db.close();
        }
    }

    protected <Args> void run(final String query, Args[] args) {
        SQLiteDatabase db = this.getWritableDatabase();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            db.execSQL(query, args);
        } finally {
            db.close();
        }
    }

    abstract protected String getDeleteTableSQL();
    abstract protected String getCreateTableSQL();
}
