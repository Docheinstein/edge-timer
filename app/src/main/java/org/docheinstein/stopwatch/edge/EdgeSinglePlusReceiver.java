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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.docheinstein.stopwatch.utils.TimeUtils.Timesnap;

public class EdgeSinglePlusReceiver extends SlookCocktailProvider {

    private static final String TAG = EdgeSinglePlusReceiver.class.getSimpleName();

    // Actually should be 8 for being able to see every update,
    // but we should avoid to stress the UI too much
    private static final int UPDATE_DELAY_CENTI_SECONDS_PRECISION = 80;

    private static final int UPDATE_DELAY_SECONDS_PRECISION = 200;

    private static final String ACTION_START_STOPWATCH = "org.docheinstein.stopwatch.ACTION_START_STOPWATCH";
    private static final String ACTION_PAUSE_STOPWATCH = "org.docheinstein.stopwatch.ACTION_PAUSE_STOPWATCH";
    private static final String ACTION_LAP_STOPWATCH = "org.docheinstein.stopwatch.ACTION_LAP_STOPWATCH";
    private static final String ACTION_STOP_STOPWATCH = "org.docheinstein.stopwatch.ACTION_STOP_STOPWATCH";
    private static final String ACTION_RESUME_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESUME_STOPWATCH";
    private static final String ACTION_RESET_STOPWATCH = "org.docheinstein.stopwatch.ACTION_RESET_STOPWATCH";
    private static final String ACTION_TAB_LAPS = "org.docheinstein.stopwatch.ACTION_TAB_LAPS";
    private static final String ACTION_TAB_HISTORY = "org.docheinstein.stopwatch.ACTION_TAB_HISTORY";
    private static final String ACTION_CLEAR_LAPS = "org.docheinstein.stopwatch.ACTION_CLEAR_LAPS";
    private static final String ACTION_CLEAR_HISTORY = "org.docheinstein.stopwatch.ACTION_CLEAR_HISTORY";

    private static final String EXTRA_COCKTAIL_ID = "cocktailId";

    private static final String PREF_STATE = "pref_state";
    private static final int PREF_STATE_VALUE_NONE = 0;
    private static final int PREF_STATE_VALUE_RUNNING = 1;
    private static final int PREF_STATE_VALUE_PAUSED = 2;

    private static final String PREF_STOPWATCH_START_TIME = "pref_start_time";
    private static final String PREF_STOPWATCH_SAVED_TIME_AMOUNT = "pref_stopwatch_time_amount";

    private static final String PREF_TAB = "pref_tab";
    private static final int PREF_TAB_VALUE_NONE = 0;
    private static final int PREF_TAB_VALUE_HISTORY = 1;
    private static final int PREF_TAB_VALUE_LAP = 2;

    private static final Object sLock = new Object();

    private static Stopwatch sStopwatch;
    private static Timer sStopwatchScheduler;

    private static RemoteViews sPanelView;
    private static RemoteViews sHelperView;

    private static Tab sTab = Tab.History;

    private static Prefs sPreferences = null;

    // Real preferences (prefs.xml)
    // Every other pref_ should be ignored when changes
    private static final Set<String> PREFERENCES_KEYS = new HashSet<>(
        Arrays.asList(
            "pref_large_display",
            "pref_theme",
            "pref_history",
            "pref_laps",
            "pref_centiseconds",
            "pref_startstop_only"
        )
    );

    private static final SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Logger.d(null ,TAG, "Changed: " + key);
            if (!PREFERENCES_KEYS.contains(key))
                return; // ignored

            Logger.i(null, TAG, "Preference changed: " + key);
            sPreferences = null; // invalidate, defer preference reloading to getPreferences()
        }
    };
    private static final AtomicBoolean sInitialized = new AtomicBoolean(false);

    public EdgeSinglePlusReceiver() {
        Logger.d(null, TAG, "EdgeSinglePlusReceiver()");
    }

    private enum  Tab {
        History,
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
        public boolean startStopOnly;
        public boolean centiseconds;
        public Theme theme;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Logger.wtrace(context, TAG, "Invalid onReceive action");
            return;
        }

        Logger.i(context, TAG, "Handling action: " + action);

        int cocktailId = -1;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(EXTRA_COCKTAIL_ID)) {
            cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
        }

        if (cocktailId >= 0)
            initializeIfNeeded(context, cocktailId);

        synchronized (sLock) {
            switch (action) {
                case ACTION_START_STOPWATCH:
                    onStartStopwatchAction(context, cocktailId, true);
                    break;
                case ACTION_PAUSE_STOPWATCH:
                    onPauseStopwatchAction(context, cocktailId);
                    break;
                case ACTION_RESUME_STOPWATCH:
                    onStartStopwatchAction(context, cocktailId, false);
                    break;
                case ACTION_LAP_STOPWATCH:
                    onLapStopwatchAction(context, cocktailId);
                    break;
                case ACTION_STOP_STOPWATCH:
                    onStopStopwatchAction(context, cocktailId);
                    break;
                case ACTION_RESET_STOPWATCH:
                    onResetStopwatchAction(context, cocktailId);
                    break;
                case ACTION_CLEAR_LAPS:
                    onClearLapsAction(context, cocktailId);
                    break;
                case ACTION_CLEAR_HISTORY:
                    onClearHistoryAction(context, cocktailId);
                    break;
                case ACTION_TAB_LAPS:
                    onTabLapsAction(context, cocktailId);
                    break;
                case ACTION_TAB_HISTORY:
                    onTabHistoryAction(context, cocktailId);
                    break;
                default:
                    super.onReceive(context, intent);
                    break;
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        Logger.i(context, TAG, "EdgeStopwatch enabled");
    }

    @Override
    public void onDisabled(Context context) {
        Logger.i(context, TAG, "EdgeStopwatch disabled");
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        boolean visible = visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW;
        Logger.i(context, TAG, "EdgeStopwatch visibility changed, visible = " + visible);

        synchronized (sLock) {
            if (visible) {
                restoreStopwatchState(context); // do always even if already initialized, for handle corner cases
                                                // (e.g. was paused and preference changed to start/stop only)
                if (sStopwatch.isRunning())
                    scheduleStopwatchUpdate(context, cocktailId);
                int tab = PreferencesUtils.getInt(context, PREF_TAB, PREF_TAB_VALUE_NONE);
                switchTab(context, cocktailId, tabFromInt(tab), false);
                updateUI(context, cocktailId);
            } else {
                unscheduleStopwatchUpdate(context);
            }
        }
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        if (cocktailIds == null || cocktailIds.length != 1) {
            Logger.w(context, TAG, "Unexpected cocktails array");
            return;
        }

        int cocktailId = cocktailIds[0];
        Logger.d(context, TAG, "onUpdate cocktail {" + cocktailId + "}");

        synchronized (sLock) {
            initializeIfNeeded(context, cocktailId);
            updateUI(context, cocktailId);
        }
    }

    private static void initializeIfNeeded(Context context, int cocktailId) {
        if (sInitialized.compareAndSet(false, true)) {
            Logger.i(context, TAG, "EdgeStopwatch initialization");

            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .registerOnSharedPreferenceChangeListener(sPreferencesListener);

            restoreStopwatchState(context);

            sPanelView = createPanelView(context, cocktailId);
            sHelperView = createHelperView(context, cocktailId);
        }
    }

    private static RemoteViews createPanelView(Context context, int cocktailId) {
        Logger.d(context, TAG, "Creating panel view");

        RemoteViews panelView = new RemoteViews(
                BuildConfig.APPLICATION_ID, R.layout.single_plus_panel_layout);

        setViewAction(context, panelView, cocktailId, R.id.startButton,
                EdgeSinglePlusReceiver.ACTION_START_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.pauseButton,
                EdgeSinglePlusReceiver.ACTION_PAUSE_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.lapButton,
                EdgeSinglePlusReceiver.ACTION_LAP_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.resumeButton,
                EdgeSinglePlusReceiver.ACTION_RESUME_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.stopButton,
                EdgeSinglePlusReceiver.ACTION_STOP_STOPWATCH);
        setViewAction(context, panelView, cocktailId, R.id.resetButton,
                EdgeSinglePlusReceiver.ACTION_RESET_STOPWATCH);

        return panelView;
    }

    private static RemoteViews createHelperView(Context context, int cocktailId) {
        Logger.d(context, TAG, "Creating helper view");

        RemoteViews helperView = new RemoteViews(
                BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_layout);

        setViewAction(context, helperView, cocktailId, R.id.lapsClearButton,
                EdgeSinglePlusReceiver.ACTION_CLEAR_LAPS);

        setViewAction(context, helperView, cocktailId, R.id.historyClearButton,
                EdgeSinglePlusReceiver.ACTION_CLEAR_HISTORY);

        setViewAction(context, helperView, cocktailId, R.id.helperTabLapsHeader,
                EdgeSinglePlusReceiver.ACTION_TAB_LAPS);

        setViewAction(context, helperView, cocktailId, R.id.helperTabHistoryHeader,
                EdgeSinglePlusReceiver.ACTION_TAB_HISTORY);

        helperView.setRemoteAdapter(R.id.lapsList, new Intent(context, EdgeSinglePlusLapsService.class));
        helperView.setRemoteAdapter(R.id.historyList, new Intent(context, EdgeSinglePlusHistoryService.class));

        return helperView;
    }

    private static void setViewAction(Context context, RemoteViews remoteView,
                                      int cocktailId, int viewId, String action) {
        Intent clickIntent = new Intent(context, EdgeSinglePlusReceiver.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EdgeSinglePlusReceiver.EXTRA_COCKTAIL_ID, cocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteView.setOnClickPendingIntent(viewId, clickPendingIntent);
    }


    private static void onStartStopwatchAction(final Context context, final int cocktailId, boolean reset) {
        Logger.i(context, TAG, "START");

        if (reset) {
            resetStopwatch(context);
            EdgeSinglePlusLapsService.clearLaps(context);
            invalidateLapsView(context, cocktailId);
        }

        startStopwatch(context);
        scheduleStopwatchUpdate(context, cocktailId);

        updateUI(context, cocktailId);
    }

    public void onLapStopwatchAction(Context context, int cocktailId) {
        long elapsed = sStopwatch.elapsed();

        Logger.i(context, TAG, "LAP: " + elapsed + "ms");

        if (addLap(context, elapsed))
            switchTab(context, cocktailId, Tab.Laps);

        updateUI(context, cocktailId);
}

    public void onPauseStopwatchAction(Context context, int cocktailId) {
        if (getPreferences(context).startStopOnly) {
            Logger.e(context, TAG, "onPauseStopwatch called while startStopOnly is true");
            return;
        }

        Logger.i(context, TAG, "PAUSE");

        pauseStopwatch(context);
        unscheduleStopwatchUpdate(context);
        updateUI(context, cocktailId);
    }

    public void onStopStopwatchAction(Context context, int cocktailId) {
        if (!getPreferences(context).startStopOnly) {
            Logger.w(context, TAG, "onStopStopwatch called while startStopOnly is false");
            return;
        }

        Logger.i(context, TAG, "STOP");

        stopStopwatch(context);
        unscheduleStopwatchUpdate(context);

        if (addTime(context, sStopwatch.elapsed()))
            switchTab(context, cocktailId, Tab.History);

        updateUI(context, cocktailId);
    }

    public void onResetStopwatchAction(Context context, int cocktailId) {
        Logger.i(context, TAG, "RESET");

        long elapsed = sStopwatch.elapsed();
        resetStopwatch(context);

        if (addTime(context, elapsed))
            switchTab(context, cocktailId, Tab.History);

        updateUI(context, cocktailId);
    }

    public void onClearHistoryAction(Context context, int cocktailId) {
        EdgeSinglePlusHistoryService.clearHistory(context);
        invalidateHistoryView(context, cocktailId);
        updateUI(context, cocktailId);
    }

    public void onClearLapsAction(Context context, int cocktailId) {
        EdgeSinglePlusLapsService.clearLaps(context);
        invalidateLapsView(context, cocktailId);
        updateUI(context, cocktailId);
    }

    public void onTabHistoryAction(Context context, int cocktailId) {
        switchTab(context, cocktailId, Tab.History);
        updateUI(context, cocktailId);
    }

    public void onTabLapsAction(Context context, int cocktailId) {
        switchTab(context, cocktailId, Tab.Laps);
        updateUI(context, cocktailId);
    }

    private boolean addTime(Context context, long time) {
        if (!getPreferences(context).history)
            return false;

        EdgeSinglePlusHistoryService.addHistoryTime(context, time);
        return true;
    }

    private boolean addLap(Context context, long time) {
        if (!getPreferences(context).laps)
            return false;

        EdgeSinglePlusLapsService.addLap(context, time);
        return true;
    }

    private static Prefs getPreferences(Context context) {
        if (sPreferences == null) {
            Logger.d(context, TAG, "Reloading preferences");

            sPreferences = new Prefs();

            sPreferences.largeDisplay = PreferencesUtils.getBool(context,
                    R.string.pref_large_display_key, R.bool.pref_large_display_default_value);
            sPreferences.history = PreferencesUtils.getBool(context,
                    R.string.pref_history_key, R.bool.pref_history_default_value);
            sPreferences.laps = PreferencesUtils.getBool(context,
                    R.string.pref_laps_key, R.bool.pref_large_display_default_value);
            sPreferences.startStopOnly = PreferencesUtils.getBool(context,
                    R.string.pref_startstop_only_key, R.bool.pref_startstop_only_default_value);
            sPreferences.centiseconds = PreferencesUtils.getBool(context,
                    R.string.pref_centiseconds_key, R.bool.pref_centiseconds_default_value);
            sPreferences.theme = Prefs.Theme.fromValue(PreferencesUtils.getString(context,
                    R.string.pref_theme_key, R.string.pref_theme_default_value));

            Logger.i(context, TAG, "Preference 'Large display' = " + sPreferences.largeDisplay);
            Logger.i(context, TAG, "Preference 'History' = " + sPreferences.history);
            Logger.i(context, TAG, "Preference 'Laps' = " + sPreferences.laps);
            Logger.i(context, TAG, "Preference 'Start/Stop only' = " + sPreferences.startStopOnly);
            Logger.i(context, TAG, "Preference 'Theme' = " + sPreferences.theme);
        }

        return sPreferences;
    }

    private static void updateUI(Context context, int cocktailId) {
        updateUI(context, cocktailId, false);
    }

    private static void updateUI(Context context, int cocktailId, boolean timeOnly) {
        Prefs prefs = getPreferences(context);

        // Detect current state
        Stopwatch.State state = sStopwatch.state;
        Timesnap timesnap = new Timesnap(sStopwatch.elapsed());

        String displayTime = prefs.centiseconds ?
                timesnap.toMinutesSecondsCentiseconds() :
                timesnap.toMinutesSeconds();
        Logger.v(context, TAG, "updateUI: " + state + ": " + displayTime);

        // Update UI: display
        if (!prefs.largeDisplay) {
            // inline
            sPanelView.setTextViewText(R.id.displayInlineText, displayTime);
        }
        else {
            // multiline
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

            if (prefs.centiseconds) {
                sPanelView.setViewVisibility(R.id.displayLargeCentisLineText, View.VISIBLE);
                sPanelView.setTextViewText(R.id.displayLargeCentisLineText,
                    StringUtils.format("%02d", timesnap.millis / 10));
            } else {
                sPanelView.setViewVisibility(R.id.displayLargeCentisLineText, View.GONE);
            }
        }

        // Do all the the necessary control and visibility changes only for major changes.
        // (skipped for the periodically scheduled updateUI task)
        if (!timeOnly) {
            // Update UI: display visibility
            sPanelView.setViewVisibility(R.id.displayInlineText,
                    prefs.largeDisplay ? View.GONE : View.VISIBLE);
            sPanelView.setViewVisibility(R.id.displayMultilineContainer,
                    prefs.largeDisplay ? View.VISIBLE : View.GONE);

            // Update UI: buttons container
            sPanelView.setViewVisibility(R.id.notRunningButtons,
                    state == Stopwatch.State.None ? View.VISIBLE : View.GONE);
            sPanelView.setViewVisibility(R.id.runningButtons,
                    state == Stopwatch.State.Running ? View.VISIBLE : View.GONE);
            sPanelView.setViewVisibility(R.id.pausedButtons,
                    state == Stopwatch.State.Paused ? View.VISIBLE : View.GONE);

            // Update UI: start/stop only?
            if (prefs.startStopOnly) {
                sPanelView.setViewVisibility(R.id.resumeButton, View.GONE);
                sPanelView.setViewVisibility(R.id.resetButton, View.GONE);
                sPanelView.setViewVisibility(R.id.pauseButton, View.GONE);
                sPanelView.setViewVisibility(R.id.stopButton, View.VISIBLE);
            } else {
                sPanelView.setViewVisibility(R.id.resumeButton, View.VISIBLE);
                sPanelView.setViewVisibility(R.id.resetButton, View.VISIBLE);
                sPanelView.setViewVisibility(R.id.pauseButton, View.VISIBLE);
                sPanelView.setViewVisibility(R.id.stopButton, View.GONE);
            }

            // Update UI: theme
            int textColor;
            int panelUpperColorRes;
            int panelLowerColorRes;

            if (prefs.theme == Prefs.Theme.Light) {
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

            // Update UI: helper/tabs
            boolean lapsIsFilled = prefs.laps && EdgeSinglePlusLapsService.getLapsCount(context) > 0;
            boolean historyIsFilled = prefs.history && EdgeSinglePlusHistoryService.getHistoryCount(context) > 0;
            int tabsEnabledCount = (prefs.laps ? 1 : 0) + (prefs.history ? 1 : 0);
            int tabsFilledCount = (lapsIsFilled ? 1 : 0) + (historyIsFilled ? 1 : 0);

            sHelperView.setViewVisibility(R.id.helperContainer, tabsFilledCount > 0 ? View.VISIBLE : View.GONE);
            sHelperView.setViewVisibility(R.id.tabsDivider, tabsEnabledCount > 1 ? View.VISIBLE : View.GONE);

            // Update UI: laps
            if (prefs.laps) {
                sPanelView.setViewVisibility(R.id.lapButton, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.lapsList, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.lapsContainer, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.helperTabLapsHeader, View.VISIBLE);
            } else {
                sPanelView.setViewVisibility(R.id.lapButton, View.GONE);
                sHelperView.setViewVisibility(R.id.lapsList, View.GONE);
                sHelperView.setViewVisibility(R.id.lapsContainer, View.GONE);
                sHelperView.setViewVisibility(R.id.helperTabLapsHeader, View.GONE);
            }

            // Update UI: history
            if (prefs.history) {
                sHelperView.setViewVisibility(R.id.historyList, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.historyContainer, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.helperTabHistoryHeader, View.VISIBLE);
            } else {
                sHelperView.setViewVisibility(R.id.historyList, View.GONE);
                sHelperView.setViewVisibility(R.id.historyContainer, View.GONE);
                sHelperView.setViewVisibility(R.id.helperTabHistoryHeader, View.GONE);
            }

            // Update UI: tab
            // Ensure the tab is feasible
            if (sTab == Tab.History && !prefs.history)
                sTab = Tab.Laps;
            if (sTab == Tab.Laps && !prefs.laps)
                sTab = Tab.History;

            if (sTab == Tab.History && prefs.history) {
                sHelperView.setViewVisibility(R.id.historyContainer, View.VISIBLE);
                sHelperView.setViewVisibility(R.id.lapsContainer, View.GONE);
                sHelperView.setTextColor(R.id.helperTabHistoryHeader,
                        ResourcesUtils.getColor(context, R.color.colorAccent));
                sHelperView.setTextColor(R.id.helperTabLapsHeader,
                        ResourcesUtils.getColor(context, R.color.colorTextMid));
            } else if (sTab == Tab.Laps && prefs.laps) {
                sHelperView.setViewVisibility(R.id.historyContainer, View.GONE);
                sHelperView.setViewVisibility(R.id.lapsContainer, View.VISIBLE);
                sHelperView.setTextColor(R.id.helperTabHistoryHeader,
                        ResourcesUtils.getColor(context, R.color.colorTextMid));
                sHelperView.setTextColor(R.id.helperTabLapsHeader,
                        ResourcesUtils.getColor(context, R.color.colorAccent));
            }
        }

        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
    }

    private static void startStopwatch(final Context context) {
        sStopwatch.start();
        saveStopwatchState(context);
    }

    private static void pauseStopwatch(final Context context) {
        sStopwatch.pause();
        saveStopwatchState(context);
    }

    private static void stopStopwatch(final Context context) {
        sStopwatch.stop();
        saveStopwatchState(context);
    }

    private static void resetStopwatch(final Context context) {
        sStopwatch.reset();
        saveStopwatchState(context);
    }

    private static int stopwatchStateToInt(Stopwatch.State state) {
        switch (state) {
                case None:
                    return PREF_STATE_VALUE_NONE;
                case Running:
                    return PREF_STATE_VALUE_RUNNING;
                case Paused:
                    return PREF_STATE_VALUE_PAUSED;
            }
        return PREF_STATE_VALUE_NONE;
    }

    private static Stopwatch.State stopwatchStateFromInt(int state) {
        switch (state) {
                case PREF_STATE_VALUE_NONE:
                    return Stopwatch.State.None;
                case PREF_STATE_VALUE_RUNNING:
                    return Stopwatch.State.Running;
                case PREF_STATE_VALUE_PAUSED:
                    return Stopwatch.State.Paused;
            }
        return Stopwatch.State.None;
    }

    private static Tab tabFromInt(int tab) {
        switch (tab) {
            case PREF_TAB_VALUE_HISTORY:
                return Tab.History;
            case PREF_TAB_VALUE_LAP:
                return Tab.Laps;
        }
        return Tab.History;
    }

    private static int tabToInt(Tab tab) {
        switch (tab) {
            case History:
                return PREF_TAB_VALUE_HISTORY;
            case Laps:
                return PREF_TAB_VALUE_LAP;
        }
        return PREF_TAB_VALUE_HISTORY;
    }

    private static void saveStopwatchState(final Context context) {
        // Since low memory killer can kill the app when in background,
        // store the stopwatch state and restore it later if we get killed.
        // Futhermore the state is saved even when the app is no more visible,
        // in order to suspend to timer while in background (avoiding to get killed)
        int state = PREF_STATE_VALUE_NONE;
        long startTime = 0;
        long savedAmount = 0;

        if (sStopwatch != null) {
            state = stopwatchStateToInt(sStopwatch.state);
            savedAmount = sStopwatch.savedAmount;
            startTime = sStopwatch.startTime;
        }

        PreferencesUtils.getWriter(context)
                .putInt(PREF_STATE, state)
                .putLong(PREF_STOPWATCH_START_TIME, startTime)
                .putLong(PREF_STOPWATCH_SAVED_TIME_AMOUNT, savedAmount)
                .apply();
    }

    private static void restoreStopwatchState(final Context context) {
        Stopwatch.State state = stopwatchStateFromInt(PreferencesUtils.getInt(context, PREF_STATE));

        if (state != Stopwatch.State.None) {
            // Restore start time and save amount from storage
            long startTime = PreferencesUtils.getLong(context, PREF_STOPWATCH_START_TIME);
            long savedAmount = PreferencesUtils.getLong(context, PREF_STOPWATCH_SAVED_TIME_AMOUNT);

            Logger.i(context, TAG, "Restoring stopwatch state (" +
                state + ", startTime = " + startTime + ", savedAmount = " + savedAmount + ")");

            sStopwatch = new Stopwatch(state, startTime, savedAmount);
        }

        if (state == Stopwatch.State.None ||
            state == Stopwatch.State.Paused && getPreferences(context).startStopOnly) {
            Logger.i(context, TAG, "Initializing default stopwatch (nothing to restore)");
            sStopwatch = new Stopwatch();
        }
    }

    private static void scheduleStopwatchUpdate(final Context context, final int cocktailId) {
        Logger.i(context, TAG, "Starting stopwatch UI updater");
        long updateDelay = getPreferences(context).centiseconds ?
                UPDATE_DELAY_CENTI_SECONDS_PRECISION : UPDATE_DELAY_SECONDS_PRECISION;

        unscheduleStopwatchUpdate(context);
        sStopwatchScheduler = new Timer();
        sStopwatchScheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (sLock) {
                    updateUI(context, cocktailId, true);
                }
            }
        }, 0, updateDelay);
    }

    private static void unscheduleStopwatchUpdate(final Context context) {
        Logger.i(context, TAG, "Stopping stopwatch UI updater");
        if (sStopwatchScheduler != null) {
            sStopwatchScheduler.cancel();
            sStopwatchScheduler.purge();
            sStopwatchScheduler = null;
        }
    }

    private static void switchTab(Context context, int cocktailId, Tab tab) {
        switchTab(context, cocktailId, tab, true);
    }

    private static void switchTab(Context context, int cocktailId, Tab tab, boolean invalidate) {
        Prefs prefs = getPreferences(context);

        if (tab == Tab.History && !prefs.history)
            tab = Tab.Laps; // attempt
        if (tab == Tab.Laps && !prefs.laps)
            tab = Tab.History; // attempt
        if (tab == Tab.History && !prefs.history) {
            return;
        }

        PreferencesUtils.setInt(context, PREF_TAB, tabToInt(tab));
        sTab = tab;
        if (invalidate) {
            if (tab == Tab.History)
                invalidateHistoryView(context, cocktailId);
            else if (tab == Tab.Laps)
                invalidateLapsView(context, cocktailId);
        }
    }

    private static void invalidateHistoryView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.historyList);
    }

    private static void invalidateLapsView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.lapsList);
    }


}
