package org.docheinstein.stopwatch.edge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.docheinstein.stopwatch.BuildConfig;
import org.docheinstein.stopwatch.R;
import org.docheinstein.stopwatch.Stopwatch;
import org.docheinstein.stopwatch.logging.Logger;
import org.docheinstein.stopwatch.utils.TimeUtils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EdgeSinglePlusReceiver extends SlookCocktailProvider {

    private static final String TAG = EdgeSinglePlusReceiver.class.getSimpleName();

    private static final String ACTION_START_STOPWATCH = "org.docheinstein.stopwatch.ACTION_START_STOPWATCH";
    private static final String ACTION_LAP_STOPWATCH = "org.docheinstein.stopwatch.ACTION_LAP_STOPWATCH";
    private static final String ACTION_PAUSE_STOPWATCH = "org.docheinstein.stopwatch.ACTION_PAUSE_STOPWATCH";
    private static final String ACTION_RESUME_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESUME_STOPWATCH";
    private static final String ACTION_RESET_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESET_STOPWATCH";
    private static final String ACTION_CLEAR_LAPS = "org.docheinstein.stopwatch.ACTION_CLEAR_LAPS";

    private static final String EXTRA_COCKTAIL_ID = "cocktailId";

    private static Stopwatch sStopwatch;
    private static ScheduledThreadPoolExecutor sStopwatchScheduler;

    private static RemoteViews sPanelView;
    private static RemoteViews sHelperView;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            wtrace(context, "Invalid action onReceive");
            return;
        }

        i(context, "[" + hashCode() + "] onReceive: " + action);

        int cocktailId = -1;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(EXTRA_COCKTAIL_ID)) {
            cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
        }

        switch (action) {
            case ACTION_START_STOPWATCH:
                onStartStopwatch(context, cocktailId);
                break;
            case ACTION_LAP_STOPWATCH:
                onLapStopwatch(context, cocktailId);
                break;
            case ACTION_PAUSE_STOPWATCH:
                onPauseStopwatch(context, cocktailId);
                break;
            case ACTION_RESUME_STOPWATCH:
                onStartStopwatch(context, cocktailId);
                break;
            case ACTION_RESET_STOPWATCH:
                onResetStopwatch(context, cocktailId);
                break;
            case ACTION_CLEAR_LAPS:
                onClearLaps(context, cocktailId);
                break;
            default:
                super.onReceive(context, intent);
                break;
        }
    }

    @Override
    public void onEnabled(Context context) {
        i(context, "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        i(context, "onDisabled");
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        boolean visible = visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW;
        i(context, "onVisibilityChanged, visible = " + visible);
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        if (cocktailIds == null || cocktailIds.length != 1) {
            w(context, "Unexpected cocktails array");
            return;
        }

        int cocktailId = cocktailIds[0];

        i(context, "onUpdate: [" + cocktailId + "]");

        renderCocktail(context, cocktailId);
    }

    private RemoteViews createPanelView(Context context, int cocktailId) {
        d(context, "Creating panel view");

        RemoteViews panelView = new RemoteViews(
                BuildConfig.APPLICATION_ID, R.layout.single_plus_panel_layout);

        setViewAction(context, panelView, cocktailId, R.id.startButton,
                EdgeSinglePlusReceiver.ACTION_START_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.lapButton,
                EdgeSinglePlusReceiver.ACTION_LAP_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.resumeButton,
                EdgeSinglePlusReceiver.ACTION_RESUME_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.stopButton,
                EdgeSinglePlusReceiver.ACTION_PAUSE_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.resetButton,
                EdgeSinglePlusReceiver.ACTION_RESET_STOPWATCH);

        return panelView;
    }

    private RemoteViews createHelperView(Context context, int cocktailId) {
        d(context, "Creating helper view");

        RemoteViews helperView = new RemoteViews(
                BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_layout);

        setViewAction(context, helperView, cocktailId, R.id.lapsClearButton,
                EdgeSinglePlusReceiver.ACTION_CLEAR_LAPS);

        Intent intent = new Intent(context, EdgeSinglePlusLapsService.class);
        helperView.setRemoteAdapter(R.id.lapsList, intent);

        return helperView;
    }

    private void setViewAction(Context context, RemoteViews remoteView, int cocktailId, int viewId, String action) {
        Intent clickIntent = new Intent(context, EdgeSinglePlusReceiver.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EdgeSinglePlusReceiver.EXTRA_COCKTAIL_ID, cocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteView.setOnClickPendingIntent(viewId, clickPendingIntent);
    }


    public void onStartStopwatch(final Context context, final int cocktailId) {
        if (sStopwatch == null) {
            d(context, "Creating sStopwatch instance");
            sStopwatch = new Stopwatch();
        }

        if (sStopwatchScheduler == null) {
            d(context, "Creating sStopwatchScheduler instance");
            sStopwatchScheduler = new ScheduledThreadPoolExecutor(1);
        }

        sStopwatch.start();
        sStopwatchScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                renderCocktail(context, cocktailId);
            }
        }, 0, 8, TimeUnit.MILLISECONDS);

//        renderCocktail(context, cocktailId);
    }

    public void onLapStopwatch(Context context, int cocktailId) {
        if (sStopwatch == null)
            return;

        long elapsed = sStopwatch.elapsed();
        d(context, "Lap: " + elapsed + "ms");

        EdgeSinglePlusLapsService.addLap(elapsed);

        invalidateHelperView(context, cocktailId);
        renderCocktail(context, cocktailId);
    }

    public void onPauseStopwatch(Context context, int cocktailId) {
        if (sStopwatch != null) {
            sStopwatch.pause();
        } else {
            wtrace(context, "Invalid stopwatch");
        }

        if (sStopwatchScheduler != null) {
            sStopwatchScheduler.shutdown();
            sStopwatchScheduler = null;
        } else {
            wtrace(context, "Invalid stopwatch scheduler");
        }

        renderCocktail(context, cocktailId);
    }

    public void onResetStopwatch(Context context, int cocktailId) {
        if (sStopwatch != null) {
            sStopwatch.reset();
        } else {
            wtrace(context, "Invalid stopwatch");
        }

        onClearLaps(context, cocktailId);
    }

    public void onClearLaps(Context context, int cocktailId) {
        EdgeSinglePlusLapsService.clearLaps();

        invalidateHelperView(context, cocktailId);
        renderCocktail(context, cocktailId);
    }

    private void renderCocktail(Context context, int cocktailId) {
        // Detect current state
        Stopwatch.State state;
        long time;

        if (sStopwatch != null) {
            state = sStopwatch.getState();
            time = sStopwatch.elapsed();
        } else  {
            state = Stopwatch.State.None;
            time = 0;
        }

        // Create views, if needed
        if (sPanelView == null) {
            sPanelView = createPanelView(context, cocktailId);;
        }

        if (sHelperView == null) {
            sHelperView = createHelperView(context, cocktailId);
        }

        // Update UI: display
        sPanelView.setTextViewText(R.id.displayInlineText,
                (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds()));

        // Update UI: buttons
        int notRunningButtonsVisibility = View.GONE;
        int runningButtonsVisibility = View.GONE;
        int pausedButtonsVisibility = View.GONE;

        switch (state) {
            case None:
                notRunningButtonsVisibility = View.VISIBLE;
                break;
            case Running:
                runningButtonsVisibility = View.VISIBLE;
                break;
            case Paused:
                pausedButtonsVisibility = View.VISIBLE;
                break;
        }

        sPanelView.setViewVisibility(R.id.notRunningButtons, notRunningButtonsVisibility);
        sPanelView.setViewVisibility(R.id.runningButtons, runningButtonsVisibility);
        sPanelView.setViewVisibility(R.id.pausedButtons, pausedButtonsVisibility);

        // Update UI: laps
        sHelperView.setViewVisibility(
            R.id.helperContainer,
            EdgeSinglePlusLapsService.getLapsCount() > 0 ? View.VISIBLE : View.GONE
        );

        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
    }


    private void invalidateHelperView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.lapsList);
    }


    private static void etrace(Context ctx, String s) { Logger.getInstance(ctx).etrace(TAG, s); }
    private static void wtrace(Context ctx, String s) { Logger.getInstance(ctx).wtrace(TAG, s); }
    private static void e(Context ctx, String s) { Logger.getInstance(ctx).e(TAG, s); }
    private static void w(Context ctx, String s) { Logger.getInstance(ctx).w(TAG, s); }
    private static void i(Context ctx, String s) { Logger.getInstance(ctx).i(TAG, s); }
    private static void d(Context ctx, String s) { Logger.getInstance(ctx).d(TAG, s); }
}
