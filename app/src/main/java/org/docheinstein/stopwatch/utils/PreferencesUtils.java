package org.docheinstein.stopwatch.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.BoolRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

@SuppressWarnings("unused")
public class PreferencesUtils {

    public static void setString(Context context, @StringRes int keyId, String value) {
        setString(context, ResourcesUtils.getString(context, keyId), value);
    }

    public static void setString(Context context,String key, String value) {
        getWriter(context).putString(key, value).apply();
    }

    public static String getString(Context context, @StringRes int keyId, @StringRes int defaultValueId) {
        return getString(context, keyId, ResourcesUtils.getString(context, defaultValueId));
    }

    public static String getString(Context context, @StringRes int keyId) {
        return getString(context, keyId, "");
    }

    public static String getString(Context context, @StringRes int keyId, String defaultValue) {
        return getString(context, ResourcesUtils.getString(context, keyId), defaultValue);
    }

    public static String getString(Context context, String key) {
        return getString(context, key, "");
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getReader(context).getString(key, defaultValue);
    }

    public static void setBool(Context context, @StringRes int keyId, boolean value) {
        setBool(context, ResourcesUtils.getString(context, keyId), value);
    }

    public static void setBool(Context context, String key, boolean value) {
        getWriter(context).putBoolean(key, value).apply();
    }

    public static boolean getBool(Context context, @StringRes int keyId, @BoolRes int defaultValueId) {
        return getBool(context, keyId, ResourcesUtils.getBool(context, defaultValueId));
    }

    public static boolean getBool(Context context, @StringRes int keyId) {
        return getBool(context, keyId, false);
    }

    public static boolean getBool(Context context, @StringRes int keyId, boolean defaultValue) {
        return getBool(context, ResourcesUtils.getString(context, keyId), defaultValue);
    }

    public static boolean getBool(Context context, String key) {
        return getBool(context, key, false);
    }

    public static boolean getBool(Context context, String key, boolean defaultValue) {
        return getReader(context).getBoolean(key, defaultValue);
    }

    public static void setInt(Context context, @StringRes int keyId, int value) {
        setInt(context, ResourcesUtils.getString(context, keyId), value);
    }

    public static void setInt(Context context, String key, int value) {
        getWriter(context).putInt(key, value).apply();
    }

    public static int getInt(Context context, @StringRes int keyId, @IntegerRes int defaultValueId) {
        return getInt_(context, keyId, ResourcesUtils.getInt(context, defaultValueId));
    }

    public static int getInt(Context context, @StringRes int keyId) {
        return getInt_(context, keyId, 0);
    }

    public static int getInt_(Context context, @StringRes int keyId, int defaultValue) {
        return getInt(context, ResourcesUtils.getString(context, keyId), defaultValue);
    }

    public static int getInt(Context context, String key) {
        return getInt(context, key , 0);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getReader(context).getInt(key, defaultValue);
    }

    public static SharedPreferences getReader(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SharedPreferences.Editor getWriter(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
    }
}
