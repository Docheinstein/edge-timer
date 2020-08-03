package org.docheinstein.stopwatch.utils;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.StringRes;

public class ResourcesUtils {
    public static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }

    public static int getColor(Context context, @ColorRes int id) {
        return context.getResources().getColor(id, null);
    }

    public static float getDimen(Context context, @DimenRes int id) {
        return context.getResources().getDimension(id);
    }
}
