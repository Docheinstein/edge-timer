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

    private static final String DEFAULT_DISPLAY_TIME = "00.00";

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

    private static int sCocktailId;
    private static RemoteViews sView;
    private static RemoteViews sHelperView;
    private static Stopwatch sStopwatch;
    private static TimerTask sStopwatchTask;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            error(context, "Null action?");
            return;
        }

        debug(context, action);


        Bundle extras = intent.getExtras();

        if (extras == null) {
            warn(context, "Null extras? Not handling action");
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
                error(context, "Unexpected cocktailIds extra");
                return;
            }

            cocktailId = cocktailIds[0];
        }
        else {
            error(context, "No cocktailId");
            return;
        }

        debug(context, "Acting on stopwatch [" + (sStopwatch != null ? sStopwatch.hashCode() : "<null>") + "]");

        switch (action) {
            case ACTION_START_TIMER:
                onStartTimer(context);
                break;
            case ACTION_PAUSE_TIMER:
                onPauseTimer(context);
                break;
            case ACTION_RESUME_TIMER:
                onResumeTimer(context);
                break;
            case ACTION_RESET_TIMER:
                onResetTimer(context);
                break;
            case ACTION_COCKTAIL_UPDATE_V2:
                onCocktailUpdate(context, cocktailId);
                break;
            default:
                warn(context, "Unknown action received");
                break;
        }
    }

    private void onStartTimer(final Context context) {
        doStartTimer(context);
        updateButtonsUI(context);
        invalidatePanel(context);
    }

    private void onResumeTimer(final Context context) {
        doStartTimer(context);
        updateButtonsUI(context);
        invalidatePanel(context);
    }

    private void doStartTimer(final Context context) {
        if (sStopwatch == null) {
            sStopwatch = new Stopwatch();
        }

        sStopwatch.start();

        if (sStopwatchTask == null) {
            sStopwatchTask = new TimerTask() {
                @Override
                public void run() {
                    updateDisplayTimeUI(context);
                    invalidatePanel(context);
                }
            };

            new Timer().scheduleAtFixedRate(sStopwatchTask, 0, 7);
        }
    }

    private void onPauseTimer(final Context context) {
        if (sStopwatch != null) {
            sStopwatch.pause();
        }

        if (sStopwatchTask != null) {
            sStopwatchTask.cancel();
            sStopwatchTask = null;
        }

        updateButtonsUI(context);
        invalidatePanel(context);
    }

    private void onResetTimer(final Context context) {
        if (sStopwatch != null) {
            sStopwatch.reset();
        }

        if (sStopwatchTask != null && sStopwatch != null && !sStopwatch.isRunning()) {
            sStopwatchTask.cancel();
            sStopwatchTask = null;
        }

        updateButtonsUI(context);
        updateDisplayTimeUI(context);
        invalidatePanel(context);
    }

    private void updateButtonsUI(Context context) {
        if (sView == null) {
            error(context, "updateButtonsUI: Invalid sView");
            return;
        }

        Stopwatch.State state;

        if (sStopwatch != null) {
            state = sStopwatch.getState();
        }
        else {
            warn(context, "Null stopwatch?");
            state = Stopwatch.State.None;
        }

        switch (state) {
            case None:
                sView.setViewVisibility(R.id.notRunningButtons, View.VISIBLE);
                sView.setViewVisibility(R.id.runningButtons, View.GONE);
                sView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Running:
                sView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                sView.setViewVisibility(R.id.runningButtons, View.VISIBLE);
                sView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Paused:
                sView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                sView.setViewVisibility(R.id.runningButtons, View.GONE);
                sView.setViewVisibility(R.id.pausedButtons, View.VISIBLE);
                break;
        }
    }

    private void updateDisplayTimeUI(Context context) {
        if (sView == null) {
            error(context, "updateDisplayTimeUI: Invalid sView");
            return;
        }

        String displayTime =
                sStopwatch != null ?
                        TimeUtils.millisToDisplayTime(sStopwatch.elapsed()) :
                        DEFAULT_DISPLAY_TIME;
        sView.setTextViewText(R.id.timerText, displayTime);
    }

    private void invalidatePanel(Context context) {
        if (sView == null) {
            error(context, "invalidatePanel: Invalid sView");
            return;
        }

        SlookCocktailManager.getInstance(context).updateCocktail(sCocktailId, sView, sHelperView);
    }

    private void onCocktailUpdate(Context context, int cocktailId) {
        debug(context, "[" + cocktailId + "] COCKTAIL UPDATE");

        if (cocktailId != sCocktailId) {
            if (sStopwatch != null)
                sStopwatch = null;

            if (sStopwatchTask != null) {
                sStopwatchTask.cancel();
                sStopwatchTask = null;
            }

            debug(context, "Creating single_plus_layout");
            sView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout);

            setViewAction(context, R.id.startButton, ACTION_START_TIMER);
            setViewAction(context, R.id.pauseButton, ACTION_PAUSE_TIMER);
            setViewAction(context, R.id.resumeButton, ACTION_RESUME_TIMER);
            setViewAction(context, R.id.resetButton, ACTION_RESET_TIMER);
            setViewAction(context, R.id.resetButton2, ACTION_RESET_TIMER);

            debug(context, "Creating single_plus_layout_helper");
            sHelperView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout_helper);

            sCocktailId = cocktailId;
        }

        invalidatePanel(context);
    }

    private void setViewAction(Context context, int viewId, String action) {
        Intent clickIntent = new Intent(context, CocktailSinglePlusProvider.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EXTRA_COCKTAIL_ID, sCocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        sView.setOnClickPendingIntent(viewId, clickPendingIntent);
    }
    
//    private static String sLogs;
    
    private void debug(Context context, String s) {
        Log.d(TAG, "[D] " + s);
//        if (sHelperView != null) {
//            sLogs += "[D] " + s + "\n";
//            sHelperView.setTextViewText(R.id.console, sLogs);
//            invalidatePanel(context);
//        }
    }
    
    private void error(Context context, String s) {
        Log.e(TAG, "[E] " + s);
//        if (sHelperView != null) {
//            sLogs += "[E] " + s + "\n";
//            sHelperView.setTextViewText(R.id.console, sLogs);
//            invalidatePanel(context);
//        }
    }
    
    private void warn(Context context, String s) {
        Log.w(TAG, "[W] " + s);
//        if (sHelperView != null) {
//            sLogs += "[W] " + s + "\n";
//            sHelperView.setTextViewText(R.id.console, sLogs);
//            invalidatePanel(context);
//        }
    }
}