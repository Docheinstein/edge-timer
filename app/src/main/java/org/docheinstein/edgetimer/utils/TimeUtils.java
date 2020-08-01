package org.docheinstein.edgetimer.utils;

public class TimeUtils {
    public static class Timesnap {
        public long minutes;
        public long seconds;
        public long millis;

        public Timesnap(long time) {
            minutes = time / 60000;
            seconds = (time % 60000) / 1000;
            millis = (time % 1000);
        }

        public String toMinutesSecondsCentiseconds(boolean hideMinutesIfZero) {
            if (hideMinutesIfZero && minutes == 0)
                return StringUtils.format("%02d.%02d", seconds, millis / 10);
            return StringUtils.format("%02d:%02d.%02d", minutes, seconds, millis / 10);
        }
    }
}
