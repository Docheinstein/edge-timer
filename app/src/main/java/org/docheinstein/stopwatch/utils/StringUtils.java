package org.docheinstein.stopwatch.utils;

import java.util.Locale;

public class StringUtils {
    public static String format(String fmt, Object... args) {
        return String.format(Locale.getDefault(), fmt, args);
    }
}
