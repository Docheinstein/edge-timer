package org.docheinstein.stopwatch.edge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.docheinstein.stopwatch.BuildConfig;
import org.docheinstein.stopwatch.R;
import org.docheinstein.stopwatch.Stopwatch;
import org.docheinstein.stopwatch.logging.Logger;
import org.docheinstein.stopwatch.utils.PreferencesUtils;
import org.docheinstein.stopwatch.utils.ResourcesUtils;
import org.docheinstein.stopwatch.utils.StringUtils;
import org.docheinstein.stopwatch.utils.TimeUtils;

import java.util.Timer;
import java.util.TimerTask;

public class EdgeSinglePlusReceiver extends SlookCocktailProvider implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = EdgeSinglePlusReceiver.class.getSimpleName();

    private static final int UPDATE_DELAY = 80;

    private static final String ACTION_START_STOPWATCH = "org.docheinstein.stopwatch.ACTION_START_STOPWATCH";
    private static final String ACTION_LAP_STOPWATCH = "org.docheinstein.stopwatch.ACTION_LAP_STOPWATCH";
    private static final String ACTION_PAUSE_STOPWATCH = "org.docheinstein.stopwatch.ACTION_PAUSE_STOPWATCH";
    private static final String ACTION_RESUME_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESUME_STOPWATCH";
    private static final String ACTION_RESET_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESET_STOPWATCH";
    private static final String ACTION_TAB_LAPS = "org.docheinstein.stopwatch.ACTION_TAB_LAPS";
    private static final String ACTION_TAB_TIMES = "org.docheinstein.stopwatch.ACTION_TAB_TIMES";
    private static final String ACTION_CLEAR_LAPS = "org.docheinstein.stopwatch.ACTION_CLEAR_LAPS";
    private static final String ACTION_CLEAR_TIMES = "org.docheinstein.stopwatch.ACTION_CLEAR_TIMES";

    private static final String EXTRA_COCKTAIL_ID = "cocktailId";

    private static Stopwatch sStopwatch;
    private static Timer sStopwatchScheduler;

//    private static RemoteViews sPanelView;
//    private static RemoteViews sHelperView;

    private static Prefs sPreferences;
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;
    private static boolean sPreferencesChanged = false;

    private static Tab sTab = Tab.Times;

    private enum  Tab {
        Times,
        Laps
    }

    private static class Prefs {
        public enum Theme {
            Light("light"),
            Dark("dark")
            ;

            Theme(String value) {
                this.value = value;
            }

            public static Theme fromValue(String value) {
                if (value.equals("dark"))
                    return Dark;
                if (value.equals("light"))
                    return Light;
                return null;
            }

            public String value;
        }

        public boolean largeDisplay;
        public boolean history;
        public boolean laps;
        public Theme theme;
    };


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
            case ACTION_RESUME_STOPWATCH:
                onStartStopwatch(context, cocktailId);
                break;
            case ACTION_LAP_STOPWATCH:
                onLapStopwatch(context, cocktailId);
                break;
            case ACTION_PAUSE_STOPWATCH:
                onPauseStopwatch(context, cocktailId);
                break;
            case ACTION_RESET_STOPWATCH:
                onResetStopwatch(context, cocktailId);
                break;
            case ACTION_CLEAR_LAPS:
                onClearLaps(context, cocktailId);
                break;
            case ACTION_CLEAR_TIMES:
                onClearTimes(context, cocktailId);
                break;
            case ACTION_TAB_LAPS:
                onTabLaps(context, cocktailId);
                break;
            case ACTION_TAB_TIMES:
                onTabTimes(context, cocktailId);
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
        if (visible)
            renderCocktail(context, cocktailId);
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        if (cocktailIds == null || cocktailIds.length != 1) {
            w(context, "Unexpected cocktails array");
            return;
        }

        int cocktailId = cocktailIds[0];

        i(context, "onUpdate {" + cocktailId + "}");

        sPreferencesListener = this;

        PreferenceManager
            .getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(sPreferencesListener);

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

        setViewAction(context, helperView, cocktailId, R.id.helperTabLapsHeader,
                EdgeSinglePlusReceiver.ACTION_TAB_LAPS);

        setViewAction(context, helperView, cocktailId, R.id.helperTabTimesHeader,
                EdgeSinglePlusReceiver.ACTION_TAB_TIMES);

        helperView.setRemoteAdapter(R.id.lapsList, new Intent(context, EdgeSinglePlusLapsService.class));
        helperView.setRemoteAdapter(R.id.timesList, new Intent(context, EdgeSinglePlusTimesService.class));

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
//            sStopwatchScheduler = new ScheduledThreadPoolExecutor(1);
            sStopwatchScheduler = new Timer();
        }

//        final AtomicInteger c = new AtomicInteger(0);
        sStopwatch.start();
        sStopwatchScheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
//                int x = c.getAndIncrement();
//                d(context, "renderCocktail() [" + x + "]");
                renderCocktail(context, cocktailId);
//                d(context, "renderCocktail() DONE [" + x + "]");
            }
        }, 0, UPDATE_DELAY);

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
            sStopwatchScheduler.cancel();
            sStopwatchScheduler.purge();
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

    public void onClearTimes(Context context, int cocktailId) {
//        EdgeSinglePlusLapsService.clearLaps();
        // ...

        invalidateHelperView(context, cocktailId);
        renderCocktail(context, cocktailId);
    }

    public void onTabLaps(Context context, int cocktailId) {
        sTab = Tab.Laps;
        renderCocktail(context, cocktailId);
    }

    public void onTabTimes(Context context, int cocktailId) {
        sTab = Tab.Times;
        renderCocktail(context, cocktailId);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        sPreferencesChanged = true;
        i(null, "Detected preferences change");
    }

    private void renderCocktail(Context context, int cocktailId) {
        // Reload preference if those are changed
        if (sPreferences == null || sPreferencesChanged) {
            sPreferencesChanged = false;
            reloadPreferences(context);
        }

        // Detect current state
        Stopwatch.State state;
        TimeUtils.Timesnap timesnap;

        if (sStopwatch != null) {
            state = sStopwatch.getState();
            timesnap = new TimeUtils.Timesnap(sStopwatch.elapsed());
        } else  {
            state = Stopwatch.State.None;
            timesnap = new TimeUtils.Timesnap(0);
        }

        // Create views, if needed
        /*
        if (sPanelView == null) {
            sPanelView = createPanelView(context, cocktailId);
        }

        if (sHelperView == null) {
            sHelperView = createHelperView(context, cocktailId);
        }
        */
        RemoteViews sPanelView = createPanelView(context, cocktailId);
        RemoteViews sHelperView = createHelperView(context, cocktailId);

        // Update UI: display
        if (!sPreferences.largeDisplay) {
            // inline
            sPanelView.setViewVisibility(R.id.displayInlineText, View.VISIBLE);
            sPanelView.setViewVisibility(R.id.displayMultilineContainer, View.GONE);

            sPanelView.setTextViewText(R.id.displayInlineText,
                    timesnap.toMinutesSecondsCentiseconds());
        }
        else {
            // multiline
            sPanelView.setViewVisibility(R.id.displayInlineText, View.GONE);
            sPanelView.setViewVisibility(R.id.displayMultilineContainer, View.VISIBLE);

            if (timesnap.minutes > 0) {
                sPanelView.setViewVisibility(R.id.displayLargeMinutesLineText, View.VISIBLE);
                sPanelView.setTextViewText(R.id.displayLargeMinutesLineText,
                    StringUtils.format("%02d", timesnap.minutes));
            }
            else {
                sPanelView.setViewVisibility(R.id.displayLargeMinutesLineText, View.GONE);
            }

            sPanelView.setTextViewText(R.id.displayLargeSecondsLineText,
                    StringUtils.format("%02d", timesnap.seconds));
            sPanelView.setTextViewText(R.id.displayLargeCentisLineText,
                    StringUtils.format("%02d", timesnap.millis / 10));
        }


        // Update UI: buttons container
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

        // Update UI: buttons
        sPanelView.setViewVisibility(R.id.lapButton, sPreferences.laps ? View.VISIBLE : View.GONE);

        // Update UI: theme
        int textColor;
        int panelUpperColorRes;
        int panelLowerColorRes;

        if (sPreferences.theme == Prefs.Theme.Light) {
            textColor = ResourcesUtils.getColor(context, R.color.colorTextDark);
            panelUpperColorRes = R.color.colorLightBackground;
            panelLowerColorRes = R.color.colorLightBackgroundLighter;
        } else {
            textColor = ResourcesUtils.getColor(context, R.color.colorTextLight);
            panelUpperColorRes = R.color.colorDarkBackground;
            panelLowerColorRes = R.color.colorDarkBackgroundLighter;
        }

        sPanelView.setInt(R.id.panelUpperContainer, "setBackgroundResource", panelUpperColorRes);
        sPanelView.setInt(R.id.panelLowerContainer, "setBackgroundResource", panelLowerColorRes);
        sPanelView.setTextColor(R.id.displayInlineText, textColor);
        sPanelView.setTextColor(R.id.displayLargeSecondsLineText, textColor);
        sPanelView.setTextColor(R.id.displayLargeMinutesLineText, textColor);
        sPanelView.setTextColor(R.id.displayLargeCentisLineText, textColor);

        // Update UI: laps
        sHelperView.setViewVisibility(
            R.id.helperContainer,
                (sPreferences.laps && EdgeSinglePlusLapsService.getLapsCount() > 0) ?
                        View.VISIBLE : View.GONE
        );

        // Update UI: tab
        if (sTab == Tab.Times) {
            sHelperView.setViewVisibility(R.id.timesContainer, View.VISIBLE);
            sHelperView.setViewVisibility(R.id.lapsContainer, View.GONE);
            sHelperView.setTextColor(R.id.helperTabTimesHeader,
                    ResourcesUtils.getColor(context, R.color.colorAccent));
            sHelperView.setTextColor(R.id.helperTabLapsHeader,
                    ResourcesUtils.getColor(context, R.color.colorTextDark));
        } else if (sTab == Tab.Laps) {
            sHelperView.setViewVisibility(R.id.timesContainer, View.GONE);
            sHelperView.setViewVisibility(R.id.lapsContainer, View.VISIBLE);
            sHelperView.setTextColor(R.id.helperTabTimesHeader,
                    ResourcesUtils.getColor(context, R.color.colorTextDark));
            sHelperView.setTextColor(R.id.helperTabLapsHeader,
                    ResourcesUtils.getColor(context, R.color.colorAccent));
        } else {
            w(context, "Unknown tab mode: " + sTab);
        }


        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
    }


    private void invalidateHelperView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.lapsList);
    }

    private void reloadPreferences(Context context) {
        d(context, "Reloading and caching preferences");

        if (sPreferences == null)
            sPreferences = new Prefs();

        sPreferences.largeDisplay = PreferencesUtils.getBool(context,
                R.string.pref_large_display_key, R.bool.pref_large_display_default_value);
        sPreferences.history = PreferencesUtils.getBool(context,
                R.string.pref_history_key, R.bool.pref_history_default_value);
        sPreferences.laps = PreferencesUtils.getBool(context,
                R.string.pref_laps_key, R.bool.pref_large_display_default_value);
        sPreferences.theme = Prefs.Theme.fromValue(PreferencesUtils.getString(context,
                R.string.pref_theme_key, R.string.pref_theme_default_value));

        i(context, "Large display: " + sPreferences.largeDisplay);
        i(context, "History: " + sPreferences.history);
        i(context, "Laps: " + sPreferences.laps);
        i(context, "Theme: " + sPreferences.theme);
    }


    private static void etrace(Context ctx, String s) { Logger.getInstance(ctx).etrace(TAG, s); }
    private static void wtrace(Context ctx, String s) { Logger.getInstance(ctx).wtrace(TAG, s); }
    private static void e(Context ctx, String s) { Logger.getInstance(ctx).e(TAG, s); }
    private static void w(Context ctx, String s) { Logger.getInstance(ctx).w(TAG, s); }
    private static void i(Context ctx, String s) { Logger.getInstance(ctx).i(TAG, s); }
    private static void d(Context ctx, String s) { Logger.getInstance(ctx).d(TAG, s); }
}
