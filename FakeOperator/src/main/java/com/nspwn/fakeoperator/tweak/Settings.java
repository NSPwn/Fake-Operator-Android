package com.nspwn.fakeoperator.tweak;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;

public class Settings {
    private static Settings ourInstance = new Settings();
    private static final String TAG = "com.nspwn.fakeoperator.tweak.Settings";
    public static final String DATA_PATH = "/data/data/com.nspwn.fakeoperator/shared_prefs/";
    public static final String DATA_FILE = "com.nspwn.fakeoperator_preferences.xml";
    private SharedPreferences sharedPreferences;

    public static Settings getInstance() {
        return ourInstance;
    }

    private Settings() {
        this.reloadPreferences();
        this.watchPreferences();
    }

    public void watchPreferences() {
        FileObserver fileObserver = new FileObserver(DATA_PATH) {
            @Override
            public void onEvent(int event, String path) {
                if ((FileObserver.CREATE & event) != 0 || (FileObserver.MODIFY & event) != 0) {
                    Settings.getInstance().reloadPreferences();
                }
            }
        };

        fileObserver.startWatching();
    }

    public void reloadPreferences() {
        try {
            Log.i(TAG, "reloading preferences...");
            Class clazz = Class.forName("android.app.SharedPreferencesImpl");
            Class[] params = new Class[] { File.class, int.class };
            Constructor constructor = clazz.getDeclaredConstructor(params);
            constructor.setAccessible(true);

            String file = DATA_PATH + DATA_FILE;
            Object prefs = constructor.newInstance(new File(file), Context.MODE_WORLD_READABLE);

            this.sharedPreferences = (SharedPreferences)prefs;

            for (Map.Entry<String, ?> kvp : this.sharedPreferences.getAll().entrySet()) {
                Log.i(TAG, String.format("key=%s;value=%s", kvp.getKey(), kvp.getValue().toString()));
            }
        } catch (Exception e) {
            Log.e(TAG, "error creating preferences...", e);
        }
    }

    public SharedPreferences getPreferences() {
        reloadPreferences();
        return this.sharedPreferences;
    }
}
