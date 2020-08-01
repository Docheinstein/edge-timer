package org.docheinstein.edgetimer;

public class Stopwatch {

    public enum State {
        None,
        Running,
        Paused
    }

    private long mStart;
    private long mSavedAmount;
    private State mState;

    public Stopwatch() {
        mSavedAmount = 0;
        mState = State.None;
    }

    public void start() {
        if (!isRunning()) {
            mStart = System.currentTimeMillis();
            mState = State.Running;
        }
    }

    public void pause() {
        if (isRunning()) {
            mSavedAmount = elapsed();
            mState = State.Paused;
        }
    }

    public void reset() {
        mSavedAmount = 0;
        mStart = System.currentTimeMillis();
        /*
        *   [None => None]
        *   Running => Running
        *   Paused => None
        **/
        if (mState == State.Paused)
            mState = State.None;
    }

    public long elapsed() {
        long amount = mSavedAmount;
        if (isRunning())
            amount += System.currentTimeMillis() - mStart;
        return amount;
    }

    public boolean isRunning() {
        return mState == State.Running;
    }

    public State getState() {
        return mState;
    }
}
