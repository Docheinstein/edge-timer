package org.docheinstein.edgetimer.utils;

import java.util.Locale;

public class TimeUtils {
    public static String millisToDisplayTime(long millis) {
        long m = millis / 60000;
        long s = (millis % 60000) / 1000;
        long ms = (millis % 1000) / 10;
        if (m > 0)
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", m, s, ms);
        return String.format(Locale.getDefault(), "%02d:%02d", s, ms);
    }
}
