package org.docheinstein.stopwatch.utils;

import java.util.Locale;

public class StringUtils {
    public static boolean isValid(String s) {
        return s != null && ! s.isEmpty();
    }
    public static String format(String fmt, Object... args) {
        return String.format(Locale.getDefault(), fmt, args);
    }
}
