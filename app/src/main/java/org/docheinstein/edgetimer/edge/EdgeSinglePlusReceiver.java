package org.docheinstein.edgetimer.edge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.docheinstein.edgetimer.BuildConfig;
import org.docheinstein.edgetimer.R;
import org.docheinstein.edgetimer.Stopwatch;
import org.docheinstein.edgetimer.logging.Logger;
import org.docheinstein.edgetimer.utils.TimeUtils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EdgeSinglePlusReceiver extends SlookCocktailProvider {

    private static final String TAG = EdgeSinglePlusReceiver.class.getSimpleName();

    private static final String ACTION_START_TIMER = "org.docheinstein.edgetimer.ACTION_START_TIMER";
    private static final String ACTION_LAP_TIMER = "org.docheinstein.edgetimer.ACTION_LAP_TIMER";
    private static final String ACTION_PAUSE_TIMER = "org.docheinstein.edgetimer.ACTION_PAUSE_TIMER";
    private static final String ACTION_RESUME_TIMER = "org.docheinstein.edgetimer.ACTION_RESUME_TIMER";
    private static final String ACTION_RESET_TIMER = "org.docheinstein.edgetimer.ACTION_RESET_TIMER";
    private static final String ACTION_CLEAR_LAPS = "org.docheinstein.edgetimer.ACTION_CLEAR_LAPS";

    private static final String EXTRA_COCKTAIL_ID = "cocktailId";

    private static Stopwatch sStopwatch;
    private static ScheduledThreadPoolExecutor sStopwatchScheduler;

    private static RemoteViews sPanelView;
    private static RemoteViews sHelperView;

    private static class EdgeSinglePlusModel {
        public long time;

        public EdgeSinglePlusModel(long time) {
            this.time = time;

        }
        public static EdgeSinglePlusModel createDefault() {
            return new EdgeSinglePlusModel(0);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        i(context, "[" + hashCode() + "] onReceive: " + action);

        int cocktailId = -1;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(EXTRA_COCKTAIL_ID)) {
            cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
        }

        switch (action) {
            case ACTION_START_TIMER:
                onStartTimer(context, cocktailId);
                break;
            case ACTION_LAP_TIMER:
                break;
            case ACTION_PAUSE_TIMER:
                onPauseTimer(context, cocktailId);
                break;
            case ACTION_RESUME_TIMER:
                break;
            case ACTION_RESET_TIMER:
                onResetTimer(context, cocktailId);
                break;
            case ACTION_CLEAR_LAPS:
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
                EdgeSinglePlusReceiver.ACTION_START_TIMER);
        setViewAction(context, panelView, cocktailId, R.id.lapButton,
                EdgeSinglePlusReceiver.ACTION_LAP_TIMER);
        setViewAction(context, panelView, cocktailId, R.id.resumeButton,
                EdgeSinglePlusReceiver.ACTION_RESUME_TIMER);
        setViewAction(context, panelView, cocktailId, R.id.stopButton,
                EdgeSinglePlusReceiver.ACTION_PAUSE_TIMER);
        setViewAction(context, panelView, cocktailId, R.id.resetButton,
                EdgeSinglePlusReceiver.ACTION_RESET_TIMER);

        return panelView;
    }

    private RemoteViews createHelperView(Context context, int cocktailId) {
        d(context, "Creating helper view");

        RemoteViews helperView = new RemoteViews(
                BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_layout);

        setViewAction(context, helperView, cocktailId, R.id.lapsClearButton,
                EdgeSinglePlusReceiver.ACTION_CLEAR_LAPS);

//        Intent intent = new Intent(context, EdgeSinglePlusLapsService.class);
//        helperView.setRemoteAdapter(R.id.lapsList, intent);

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

    private void renderCocktail(Context context, int cocktailId) {
        long t = sStopwatch != null ? sStopwatch.elapsed() : 0;
        EdgeSinglePlusModel model = new EdgeSinglePlusModel(t);
        renderCocktail(context, cocktailId, model);
    }

    private void renderCocktail(Context context, int cocktailId, EdgeSinglePlusModel model) {
        if (sPanelView == null) {
            sPanelView = createPanelView(context, cocktailId);;
        }

        if (sHelperView == null) {
            sHelperView = createHelperView(context, cocktailId);
        }

        sPanelView.setTextViewText(R.id.timerDisplayInlineText,
                (new TimeUtils.Timesnap(model.time).toMinutesSecondsCentiseconds()));

        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
    }

    public void onStartTimer(final Context context, final int cocktailId) {
        if (sStopwatch == null) {
            d(context, "Creating sStopwatch instance");
            sStopwatch = new Stopwatch();
        }

        if (sStopwatchScheduler == null) {
            d(context, "Creating sStopwatchScheduler instance");
            sStopwatchScheduler = new ScheduledThreadPoolExecutor(1);
        }

        sStopwatch.start();
        sStopwatchScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                renderCocktail(context, cocktailId);
            }
        }, 0, 8, TimeUnit.MILLISECONDS);

        renderCocktail(context, cocktailId);
    }

    public void onPauseTimer(Context context, int cocktailId) {
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

    public void onResetTimer(Context context, int cocktailId) {
        if (sStopwatch != null) {
            sStopwatch.reset();
        } else {
            wtrace(context, "Invalid stopwatch");
        }

        renderCocktail(context, cocktailId);
    }

    private static void etrace(Context ctx, String s) { Logger.getInstance(ctx).etrace(TAG, s); }
    private static void wtrace(Context ctx, String s) { Logger.getInstance(ctx).wtrace(TAG, s); }
    private static void e(Context ctx, String s) { Logger.getInstance(ctx).e(TAG, s); }
    private static void w(Context ctx, String s) { Logger.getInstance(ctx).w(TAG, s); }
    private static void i(Context ctx, String s) { Logger.getInstance(ctx).i(TAG, s); }
    private static void d(Context ctx, String s) { Logger.getInstance(ctx).d(TAG, s); }
}
