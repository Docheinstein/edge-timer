package org.docheinstein.edgetimer;

public class Stopwatch {

    private long mStart;

    public Stopwatch() {

    }

    public void start() {
        mStart = System.currentTimeMillis();
    }

    public void stop() {}

    public long elapsed() {
        return System.currentTimeMillis() - mStart;
    }
}
