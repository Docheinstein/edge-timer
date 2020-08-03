package org.docheinstein.stopwatch.utils;

import android.content.Context;

import androidx.preference.PreferenceManager;

public class PreferencesUtils {

    public static String getString(Context context, int key) {
        return getString(context, key, "");
    }

    public static String getString(Context context, int key, String defaultValue) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        ResourcesUtils.getString(context, key),
                        defaultValue
                );
    }

    public static boolean getBool(Context context, int key) {
        return getBool(context, key, false);
    }

    public static boolean getBool(Context context, int key, boolean defaultValue) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(
                        ResourcesUtils.getString(context, key),
                        defaultValue
                );
    }
}
