package com.nico.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

abstract class LocalSettings extends SQLiteOpenHelper implements BaseColumns {
    public static final String DB_NAME = "VlcFreemote.db";
    public static final int DB_VERSION = 1;

    public LocalSettings(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
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
        try {
            db.insert(table, null, values);
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
