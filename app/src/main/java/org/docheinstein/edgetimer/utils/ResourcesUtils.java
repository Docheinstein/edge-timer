package org.docheinstein.edgetimer.utils;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

public class ResourcesUtils {
    public static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }

    public static int getColor(Context context, @ColorRes int id) {
        return context.getResources().getColor(id, null);
    }
}
