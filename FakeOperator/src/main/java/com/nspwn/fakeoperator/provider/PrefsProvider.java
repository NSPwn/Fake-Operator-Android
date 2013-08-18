package com.nspwn.fakeoperator.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;

public class PrefsProvider extends ContentProvider {
    private static final String AUTHORITY = "com.nspwn.fakeoperator.provider.PrefsProvider";
    private static final String PREFS_CMD = "prefs";
    public static final Uri CONTENT_URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, PREFS_CMD));

    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PREFS_CMD, 1);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        MatrixCursor cursor = new MatrixCursor(new String[] { "enabled", "fake_operator" }, 1);
        cursor.addRow(new Object[] { preferences.getBoolean("tweak_enabled", false), preferences.getString("fake_operator", "NSPwn") });

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.com.nspwn.fakeoperator.provider.PrefsProvider";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
