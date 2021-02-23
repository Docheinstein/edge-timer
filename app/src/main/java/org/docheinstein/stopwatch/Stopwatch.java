package org.docheinstein.stopwatch;

public class Stopwatch {

    public enum State {
        None,
        Running,
        Paused
    }

    public State state;
    public long startTime;
    public long savedAmount;

    public Stopwatch(State state, long startTime, long savedAmount) {
        this.state = state;
        this.startTime = startTime;
        this.savedAmount = savedAmount;
    }

    public Stopwatch() {
        this(State.None, 0, 0);
    }

    public void start() {
        if (!isRunning()) {
            startTime = System.currentTimeMillis();
            state = State.Running;
        }
    }

    public void pause() {
        if (isRunning()) {
            savedAmount = elapsed();
            state = State.Paused;
        }
    }

    public void stop() {
        if (isRunning()) {
            savedAmount = elapsed();
            state = State.None;
        }
    }

    public void reset() {
        savedAmount = 0;
        startTime = System.currentTimeMillis();
        /*
        *   [None => None]
        *   Running => Running (continue to run, resetting saved amount)
        *   Paused => None
        **/
        if (state == State.Paused)
            state = State.None;
    }

    public long elapsed() {
        long amount = savedAmount;
        if (isRunning())
            amount += System.currentTimeMillis() - startTime;
        return amount;
    }

    public boolean isRunning() {
        return state == State.Running;
    }
}
