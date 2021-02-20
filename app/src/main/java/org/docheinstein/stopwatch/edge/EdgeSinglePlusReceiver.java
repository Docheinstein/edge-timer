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

    private static final Object sLock = new Object();

    private static Stopwatch sStopwatch;
    private static Timer sStopwatchScheduler;

//    private static RemoteViews sPanelView;
//    private static RemoteViews sHelperView;

    private static Prefs sPreferences;
    private static boolean sPreferencesChanged = false;

    // Keep a reference because Timer.scheduleAtFixedRate requires so
    @SuppressWarnings("FieldCanBeLocal")
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;

    private static Tab sTab = Tab.History;

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
            Logger.wtrace(context, TAG, "Invalid action onReceive");
            return;
        }

        Logger.i(context, TAG, "[" + hashCode() + "] onReceive: " + action);

        int cocktailId = -1;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(EXTRA_COCKTAIL_ID)) {
            cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
        }

        switch (action) {
            case ACTION_START_STOPWATCH:
                onStartStopwatch(context, cocktailId, true);
                break;
            case ACTION_PAUSE_STOPWATCH:
                onPauseStopwatch(context, cocktailId);
                break;
            case ACTION_RESUME_STOPWATCH:
                onStartStopwatch(context, cocktailId, false);
                break;
            case ACTION_LAP_STOPWATCH:
                onLapStopwatch(context, cocktailId);
                break;
            case ACTION_STOP_STOPWATCH:
                onStopStopwatch(context, cocktailId);
                break;
            case ACTION_RESET_STOPWATCH:
                onResetStopwatch(context, cocktailId);
                break;
            case ACTION_CLEAR_LAPS:
                onClearLaps(context, cocktailId);
                break;
            case ACTION_CLEAR_HISTORY:
                onClearHistory(context, cocktailId);
                break;
            case ACTION_TAB_LAPS:
                onTabLaps(context, cocktailId);
                break;
            case ACTION_TAB_HISTORY:
                onTabHistory(context, cocktailId);
                break;
            default:
                super.onReceive(context, intent);
                break;
        }
    }

    @Override
    public void onEnabled(Context context) {
        Logger.i(context, TAG, "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        Logger.i(context, TAG, "onDisabled");
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        boolean visible = visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW;
        Logger.i(context, TAG, "onVisibilityChanged, visible = " + visible);
        if (visible)
            synchronized (sLock) {
                renderCocktail(context, cocktailId);
            }
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        if (cocktailIds == null || cocktailIds.length != 1) {
            Logger.w(context, TAG, "Unexpected cocktails array");
            return;
        }

        int cocktailId = cocktailIds[0];

        Logger.i(context, TAG, "onUpdate {" + cocktailId + "}");

        synchronized (sLock) {
            sPreferencesListener = this;

            PreferenceManager
                .getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(sPreferencesListener);

            renderCocktail(context, cocktailId);
        }
    }

    private RemoteViews createPanelView(Context context, int cocktailId) {
        Logger.v(context, TAG, "Creating panel view");

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

    private RemoteViews createHelperView(Context context, int cocktailId) {
        Logger.v(context, TAG, "Creating helper view");

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

    private void setViewAction(Context context, RemoteViews remoteView, int cocktailId, int viewId, String action) {
        Intent clickIntent = new Intent(context, EdgeSinglePlusReceiver.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EdgeSinglePlusReceiver.EXTRA_COCKTAIL_ID, cocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteView.setOnClickPendingIntent(viewId, clickPendingIntent);
    }


    public void onStartStopwatch(final Context context, final int cocktailId, boolean reset) {
        synchronized (sLock) {
            if (sStopwatch == null) {
                Logger.d(context, TAG, "Creating sStopwatch instance");
                sStopwatch = new Stopwatch();
            }

            if (sStopwatchScheduler == null) {
                Logger.d(context, TAG, "Creating sStopwatchScheduler instance");
                sStopwatchScheduler = new Timer();
            }

            if (reset) {
                sStopwatch.reset();
                EdgeSinglePlusLapsService.clearLaps();
                invalidateLapsView(context, cocktailId);
            }

            sStopwatch.start();

            long updateDelay = getPreferences(context).centiseconds ?
                    UPDATE_DELAY_CENTI_SECONDS_PRECISION : UPDATE_DELAY_SECONDS_PRECISION;

            sStopwatchScheduler.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    synchronized (sLock) {
                        renderCocktail(context, cocktailId);
                    }
                }
            }, 0, updateDelay);

            renderCocktail(context, cocktailId);
        }
    }

    public void onLapStopwatch(Context context, int cocktailId) {
        synchronized (sLock) {
            if (sStopwatch == null)
            return;

            long elapsed = sStopwatch.elapsed();
            Logger.d(context, TAG, "Lap: " + elapsed + "ms");


            if (addLap(context, elapsed)) {
                sTab = Tab.Laps; // auto switch to LAPS tab
                invalidateLapsView(context, cocktailId);
            }

            renderCocktail(context, cocktailId);
        }
    }

    public void onPauseStopwatch(Context context, int cocktailId) {
        synchronized (sLock) {
            if (sStopwatch != null) {
                if (!getPreferences(context).startStopOnly) {
                    sStopwatch.pause();
                }
                else {
                    Logger.w(context, TAG, "onPauseStopwatch called while startStopOnly is true");
                }
            } else {
                Logger.wtrace(context, TAG, "Invalid stopwatch");
            }

            if (sStopwatchScheduler != null) {
                sStopwatchScheduler.cancel();
                sStopwatchScheduler.purge();
                sStopwatchScheduler = null;
            } else {
                Logger.wtrace(context, TAG, "Invalid stopwatch scheduler");
            }

            renderCocktail(context, cocktailId);
        }
    }

    public void onStopStopwatch(Context context, int cocktailId) {
        synchronized (sLock) {
            if (sStopwatch != null) {
                if (getPreferences(context).startStopOnly) {
                    sStopwatch.stop();

                    if (addTime(context, sStopwatch.elapsed())) {
                        sTab = Tab.History; // auto switch to HISTORY tab
                        invalidateHistoryView(context, cocktailId);
                    }
                } else {
                    Logger.w(context, TAG, "onStopStopwatch called while startStopOnly is false");
                }
            } else {
                Logger.wtrace(context, TAG, "Invalid stopwatch");
            }

            if (sStopwatchScheduler != null) {
                sStopwatchScheduler.cancel();
                sStopwatchScheduler.purge();
                sStopwatchScheduler = null;
            } else {
                Logger.wtrace(context, TAG, "Invalid stopwatch scheduler");
            }

            renderCocktail(context, cocktailId);
        }
    }

    public void onResetStopwatch(Context context, int cocktailId) {
        synchronized (sLock) {
            if (sStopwatch != null) {
                long elapsed = sStopwatch.elapsed();
                sStopwatch.reset();

                if (addTime(context, elapsed)) {
                    sTab = Tab.History; // auto switch to HISTORY tab
                    invalidateHistoryView(context, cocktailId);
                }
            } else {
                Logger.wtrace(context, TAG, "Invalid stopwatch");
            }

            renderCocktail(context, cocktailId);
        }
    }

    public void onClearLaps(Context context, int cocktailId) {
        synchronized (sLock) {
            EdgeSinglePlusLapsService.clearLaps();
            invalidateLapsView(context, cocktailId);
            renderCocktail(context, cocktailId);
        }
    }

    public void onClearHistory(Context context, int cocktailId) {
        synchronized (sLock) {
            EdgeSinglePlusHistoryService.clearHistory(context);

            invalidateHistoryView(context, cocktailId);
            renderCocktail(context, cocktailId);
        }
    }

    public void onTabLaps(Context context, int cocktailId) {
        synchronized (sLock) {
            sTab = Tab.Laps;
            renderCocktail(context, cocktailId);
        }
    }

    public void onTabHistory(Context context, int cocktailId) {
        synchronized (sLock) {
            sTab = Tab.History;
            renderCocktail(context, cocktailId);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        synchronized (sLock) {
            Logger.i(null, TAG, "Detected preferences change");
            sPreferencesChanged = true;
            // defer preference reloading to onVisibilityChange
        }
    }

    private boolean addTime(Context context, long time) {
        if (getPreferences(context).history) {
            EdgeSinglePlusHistoryService.addHistoryTime(context, time);
            return true;
        }
        return false;
    }

    private boolean addLap(Context context, long time) {
        if (getPreferences(context).laps) {
            EdgeSinglePlusLapsService.addLap(time);
            return true;
        }
        return false;
    }

    private static Prefs getPreferences(Context context) {
        synchronized (sLock) {
            if (sPreferences == null || sPreferencesChanged) {
                Logger.d(context, TAG, "Reloading and caching preferences");
                sPreferencesChanged = false;
                sPreferences = reloadPreferences(context);
            }

            return sPreferences;
        }
    }

    private static Prefs reloadPreferences(Context context) {
        Prefs prefs = new Prefs();

        prefs.largeDisplay = PreferencesUtils.getBool(context,
                R.string.pref_large_display_key, R.bool.pref_large_display_default_value);
        prefs.history = PreferencesUtils.getBool(context,
                R.string.pref_history_key, R.bool.pref_history_default_value);
        prefs.laps = PreferencesUtils.getBool(context,
                R.string.pref_laps_key, R.bool.pref_large_display_default_value);
        prefs.startStopOnly = PreferencesUtils.getBool(context,
                R.string.pref_startstop_only_key, R.bool.pref_startstop_only_default_value);
        prefs.centiseconds = PreferencesUtils.getBool(context,
                R.string.pref_centiseconds_key, R.bool.pref_centiseconds_default_value);
        prefs.theme = Prefs.Theme.fromValue(PreferencesUtils.getString(context,
                R.string.pref_theme_key, R.string.pref_theme_default_value));

        Logger.i(context, TAG, "Large display: " + prefs.largeDisplay);
        Logger.i(context, TAG, "History: " + prefs.history);
        Logger.i(context, TAG, "Laps: " + prefs.laps);
        Logger.i(context, TAG, "Start/Stop only: " + prefs.startStopOnly);
        Logger.i(context, TAG, "Theme: " + prefs.theme);

        return prefs;
    }

    private void renderCocktail(Context context, int cocktailId) {
        // Reload preference if those are changed
        Prefs prefs = getPreferences(context);

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
        // ^^ This would be ideal, but for some reason something freezes
        // if we try to avoid to recreate the views each time
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
        if (!prefs.largeDisplay) {
            // inline
            sPanelView.setViewVisibility(R.id.displayInlineText, View.VISIBLE);
            sPanelView.setViewVisibility(R.id.displayMultilineContainer, View.GONE);

            String displayTime = prefs.centiseconds ?
                    timesnap.toMinutesSecondsCentiseconds() :
                    timesnap.toMinutesSeconds();

            sPanelView.setTextViewText(R.id.displayInlineText, displayTime);
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

            if (prefs.centiseconds) {
                sPanelView.setViewVisibility(R.id.displayLargeCentisLineText, View.VISIBLE);
                sPanelView.setTextViewText(R.id.displayLargeCentisLineText,
                    StringUtils.format("%02d", timesnap.millis / 10));
            } else {
                sPanelView.setViewVisibility(R.id.displayLargeCentisLineText, View.GONE);
            }

        }


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
        boolean lapsIsFilled = prefs.laps && EdgeSinglePlusLapsService.getLapsCount() > 0;
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
        // Ensure the tab is appropriate
        if (sTab == Tab.History && !prefs.history)
            sTab = Tab.Laps;
        if (sTab == Tab.Laps && !prefs.laps)
            sTab = Tab.History;

        if (sTab == Tab.History) {
            sHelperView.setViewVisibility(R.id.historyContainer, View.VISIBLE);
            sHelperView.setViewVisibility(R.id.lapsContainer, View.GONE);
            sHelperView.setTextColor(R.id.helperTabHistoryHeader,
                    ResourcesUtils.getColor(context, R.color.colorAccent));
            sHelperView.setTextColor(R.id.helperTabLapsHeader,
                    ResourcesUtils.getColor(context, R.color.colorTextMid));
        } else if (sTab == Tab.Laps) {
            sHelperView.setViewVisibility(R.id.historyContainer, View.GONE);
            sHelperView.setViewVisibility(R.id.lapsContainer, View.VISIBLE);
            sHelperView.setTextColor(R.id.helperTabHistoryHeader,
                    ResourcesUtils.getColor(context, R.color.colorTextMid));
            sHelperView.setTextColor(R.id.helperTabLapsHeader,
                    ResourcesUtils.getColor(context, R.color.colorAccent));
        } else {
            Logger.w(context, TAG, "Unknown tab mode: " + sTab);
        }

        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
    }

    private void invalidateLapsView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.lapsList);
    }

    private void invalidateHistoryView(Context context, int cocktailId) {
        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(cocktailId, R.id.historyList);
    }
}
