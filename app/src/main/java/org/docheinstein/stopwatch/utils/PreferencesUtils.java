package org.docheinstein.stopwatch.utils;

import android.content.Context;

import androidx.annotation.BoolRes;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

public class PreferencesUtils {

    public static String getString(Context context, @StringRes int keyId, @StringRes int defaultValueId) {
        return getString(context, keyId, ResourcesUtils.getString(context, defaultValueId));
    }

    public static String getString(Context context, @StringRes int keyId) {
        return getString(context, keyId, "");
    }

    public static String getString(Context context, @StringRes int keyId, String defaultValue) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        ResourcesUtils.getString(context, keyId),
                        defaultValue
                );
    }

    public static boolean getBool(Context context, @StringRes int keyId, @BoolRes int defaultValueId) {
        return getBool(context, keyId, ResourcesUtils.getBool(context, defaultValueId));
    }

    public static boolean getBool(Context context, @StringRes int keyId) {
        return getBool(context, keyId, false);
    }

    public static boolean getBool(Context context, @StringRes int keyId, boolean defaultValue) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(
                        ResourcesUtils.getString(context, keyId),
                        defaultValue
                );
    }
}
