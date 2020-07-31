package org.docheinstein.edgetimer.singleplus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;

import org.docheinstein.edgetimer.R;
import org.docheinstein.edgetimer.Stopwatch;
import org.docheinstein.edgetimer.utils.TimeUtils;

import java.util.Timer;
import java.util.TimerTask;

public class CocktailSinglePlusProvider extends BroadcastReceiver {

    private static final String TAG = "SinglePlusProvider";

    private static final String ACTION_START_TIMER = "org.docheinstein.edgetimer.ACTION_START_TIMER";
    private static final String ACTION_PAUSE_TIMER = "org.docheinstein.edgetimer.ACTION_PAUSE_TIMER";
    private static final String ACTION_RESUME_TIMER = "org.docheinstein.edgetimer.ACTION_RESUME_TIMER";
    private static final String ACTION_RESET_TIMER = "org.docheinstein.edgetimer.ACTION_RESET_TIMER";

    private static final String ACTION_COCKTAIL_ENABLED = "com.samsung.android.cocktail.action.COCKTAIL_ENABLED";
    private static final String ACTION_COCKTAIL_UPDATE = "com.samsung.android.cocktail.action.COCKTAIL_UPDATE";
    private static final String ACTION_COCKTAIL_UPDATE_V2 = "com.samsung.android.cocktail.v2.action.COCKTAIL_UPDATE";
    private static final String ACTION_COCKTAIL_DISABLED = "com.samsung.android.cocktail.action.COCKTAIL_DISABLED";
    private static final String ACTION_COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";
    private static final String EXTRA_COCKTAIL_IDS = "cocktailIds";
    private static final String EXTRA_COCKTAIL_ID = "cocktailId";
    private static final String EXTRA_COCKTAIL_VISIBILITY = "cocktailVisibility";

    private static RemoteViews mView;
    private static RemoteViews mHelperView;
    private static Stopwatch mStopwatch;
    private static TimerTask mStopwatchTask;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Log.e(TAG, "Null action?");
            return;
        }

        Log.i(TAG, action);


        Bundle extras = intent.getExtras();

        if (extras == null) {
            Log.e(TAG, "Null extras?");
            return;
        }


        int cocktailId;

        if (extras.containsKey(EXTRA_COCKTAIL_ID)) {
            // org.docheinstein.edgetimer.ACTION_*
            cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
        }
        else if (extras.containsKey(EXTRA_COCKTAIL_IDS)) {
            // com.samsung.android.cocktail.v2.action.COCKTAIL_UPDATE
            int[] cocktailIds = extras.getIntArray(EXTRA_COCKTAIL_IDS);

            if (cocktailIds == null || cocktailIds.length < 1) {
                Log.e(TAG, "Unexpected cocktailIds extra");
                return;
            }

            cocktailId = cocktailIds[0];
        }
        else {
            Log.e(TAG, "No cocktailId");
            return;
        }

        Log.d(TAG, "Acting on stopwatch [" + (mStopwatch != null ? mStopwatch.hashCode() : "<null>") + "]");

        switch (action) {
            case ACTION_START_TIMER:
                onStartTimer(context, cocktailId);
                break;
            case ACTION_PAUSE_TIMER:
                onPauseTimer(context, cocktailId);
                break;
            case ACTION_RESUME_TIMER:
                onResumeTimer(context, cocktailId);
                break;
            case ACTION_RESET_TIMER:
                onResetTimer(context, cocktailId);
                break;
            case ACTION_COCKTAIL_UPDATE_V2:
                onCocktailUpdate(context, cocktailId);
                break;
            default:
                Log.w(TAG, "Unknown action received");
                break;
        }
    }

    private void onStartTimer(final Context context, final int cocktailId) {
        doStartTimer(context, cocktailId);
        updateButtonsUI();
        invalidatePanel(context, cocktailId);
    }

    private void onResumeTimer(final Context context, final int cocktailId) {
        doStartTimer(context, cocktailId);
        updateButtonsUI();
        invalidatePanel(context, cocktailId);
    }

    private void doStartTimer(final Context context, final int cocktailId) {
        if (mStopwatch == null) {
            mStopwatch = new Stopwatch();
        }

        mStopwatch.start();

        if (mStopwatchTask == null) {
            mStopwatchTask = new TimerTask() {
                @Override
                public void run() {
                    updateDisplayTimeUI();
                    invalidatePanel(context, cocktailId);
                }
            };

            new Timer().scheduleAtFixedRate(mStopwatchTask, 0, 8);
        }
    }

    private void onPauseTimer(final Context context, final int cocktailId) {
        if (mStopwatch != null) {
            mStopwatch.pause();
        }

        if (mStopwatchTask != null) {
            mStopwatchTask.cancel();
            mStopwatchTask = null;
        }

        updateButtonsUI();
        invalidatePanel(context, cocktailId);
    }

    private void onResetTimer(final Context context, final int cocktailId) {
        if (mStopwatch != null) {
            mStopwatch.reset();
        }

        if (mStopwatchTask != null && mStopwatch != null && !mStopwatch.isRunning()) {
            mStopwatchTask.cancel();
            mStopwatchTask = null;
        }

        updateButtonsUI();
        updateDisplayTimeUI();
        invalidatePanel(context, cocktailId);
    }

    private void updateButtonsUI() {
        if (mStopwatch == null) {
            Log.w(TAG, "Null stopwatch?");
            return;
        }

        switch (mStopwatch.getState()) {
            case None:
                mView.setViewVisibility(R.id.notRunningButtons, View.VISIBLE);
                mView.setViewVisibility(R.id.runningButtons, View.GONE);
                mView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Running:
                mView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                mView.setViewVisibility(R.id.runningButtons, View.VISIBLE);
                mView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Paused:
                mView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                mView.setViewVisibility(R.id.runningButtons, View.GONE);
                mView.setViewVisibility(R.id.pausedButtons, View.VISIBLE);
                break;
        }
    }

    private void updateDisplayTimeUI() {
        String displayTime = TimeUtils.millisToDisplayTime(mStopwatch.elapsed());
        mView.setTextViewText(R.id.timerText, displayTime);
    }

    private void invalidatePanel(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, mView, mHelperView);
    }

    private void onCocktailUpdate(Context context, int cocktailId) {
        if (mView == null) {
            Log.d(TAG, "Creating single_plus_layout");
            mView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout);

            setViewAction(context, cocktailId, R.id.startButton, ACTION_START_TIMER);
            setViewAction(context, cocktailId, R.id.pauseButton, ACTION_PAUSE_TIMER);
            setViewAction(context, cocktailId, R.id.resumeButton, ACTION_RESUME_TIMER);
            setViewAction(context, cocktailId, R.id.resetButton, ACTION_RESET_TIMER);
            setViewAction(context, cocktailId, R.id.resetButton2, ACTION_RESET_TIMER);
        }

        if (mHelperView == null) {
            Log.d(TAG, "Creating single_plus_layout_helper");
            mHelperView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout_helper);
        }

        invalidatePanel(context, cocktailId);
    }

    private void setViewAction(Context context, int cocktailId, int viewId, String action) {
        Intent clickIntent = new Intent(context, CocktailSinglePlusProvider.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EXTRA_COCKTAIL_ID, cocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mView.setOnClickPendingIntent(viewId, clickPendingIntent);
    }
}