package org.docheinstein.edgetimer.singleplus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;

import org.docheinstein.edgetimer.BuildConfig;
import org.docheinstein.edgetimer.R;
import org.docheinstein.edgetimer.Stopwatch;
import org.docheinstein.edgetimer.utils.PreferencesUtils;
import org.docheinstein.edgetimer.utils.ResourcesUtils;
import org.docheinstein.edgetimer.utils.StringUtils;
import org.docheinstein.edgetimer.utils.TimeUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class CocktailSinglePlusReceiver extends BroadcastReceiver implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SinglePlusProvider";

    private static final String DEFAULT_DISPLAY_TIME = "00.00";

    private static final String ACTION_START_TIMER = "org.docheinstein.edgetimer.ACTION_START_TIMER";
    private static final String ACTION_LAP_TIMER = "org.docheinstein.edgetimer.ACTION_LAP_TIMER";
    private static final String ACTION_PAUSE_TIMER = "org.docheinstein.edgetimer.ACTION_PAUSE_TIMER";
    private static final String ACTION_RESUME_TIMER = "org.docheinstein.edgetimer.ACTION_RESUME_TIMER";
    private static final String ACTION_RESET_TIMER = "org.docheinstein.edgetimer.ACTION_RESET_TIMER";
    private static final String ACTION_CLEAR_LAPS = "org.docheinstein.edgetimer.ACTION_CLEAR_LAPS";

    private static final String ACTION_COCKTAIL_ENABLED = "com.samsung.android.cocktail.action.COCKTAIL_ENABLED";
    private static final String ACTION_COCKTAIL_UPDATE = "com.samsung.android.cocktail.action.COCKTAIL_UPDATE";
    private static final String ACTION_COCKTAIL_UPDATE_V2 = "com.samsung.android.cocktail.v2.action.COCKTAIL_UPDATE";
    private static final String ACTION_COCKTAIL_DISABLED = "com.samsung.android.cocktail.action.COCKTAIL_DISABLED";
    private static final String ACTION_COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";
    private static final String EXTRA_COCKTAIL_IDS = "cocktailIds";
    private static final String EXTRA_COCKTAIL_ID = "cocktailId";
    private static final String EXTRA_COCKTAIL_VISIBILITY = "cocktailVisibility";

    private static int sCocktailId;
    private static RemoteViews sPanelView;
    private static RemoteViews sHelperView;
    private static Stopwatch sStopwatch;
    private static Timer sStopwatchTimer;
    private static final Object sStopwatchSync = new Object();
    private static boolean sWasRunning = false;
    private static AtomicBoolean sPreferencesChanged = new AtomicBoolean(false);
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;

    private static class Settings {
        public boolean largeDisplay;
        public String theme;
    };

    private static Settings sSettings = new Settings();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            error(context, "Null action?");
            return;
        }

        debug(context, action);

        debug(context, "Acting on stopwatch [" + (sStopwatch != null ? sStopwatch.hashCode() : "<null>") + "]");

        switch (action) {
            case ACTION_START_TIMER:
                onStartTimer(context);
                break;
            case ACTION_LAP_TIMER:
                onLapTimer(context);
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
            case ACTION_CLEAR_LAPS:
                onClearLaps(context);
                break;
            case ACTION_COCKTAIL_ENABLED:
                onCocktailEnabled(context);
                break;
            case ACTION_COCKTAIL_UPDATE_V2: {
                Bundle extras = intent.getExtras();

                if (extras != null &&
                        extras.containsKey(EXTRA_COCKTAIL_IDS)) {
                    int[] cocktailIds = extras.getIntArray(EXTRA_COCKTAIL_IDS);

                    if (cocktailIds == null || cocktailIds.length < 1) {
                        error(context, "Unexpected cocktailIds extra");
                        return;
                    }

                    int cocktailId = cocktailIds[0];

                    onCocktailUpdate(context, cocktailId);
                } else {
                    warn(context, "No extras?");
                }

                break;
            }
            case ACTION_COCKTAIL_VISIBILITY_CHANGED: {
                Bundle extras = intent.getExtras();
                if (extras != null &&
                        extras.containsKey(EXTRA_COCKTAIL_ID) &&
                        extras.containsKey(EXTRA_COCKTAIL_VISIBILITY)) {
                    int cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);
                    int visibility = extras.getInt(EXTRA_COCKTAIL_VISIBILITY);
                    onCocktailVisibilityChanged(context, cocktailId, visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW);
                } else {
                    warn(context, "No extras?");
                }

                break;
            }
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
        initTimer();
        sStopwatch.start();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updateDisplayTimeUI(context);
                invalidatePanel(context);
            }
        };

        sStopwatchTimer.scheduleAtFixedRate(task, 0, 7);
    }

    private void onLapTimer(final Context context) {
        if (sStopwatch == null)
            return;

        long elapsed = sStopwatch.elapsed();

        debug(context, "LAP: " + elapsed + "ms");

        CocktailSinglePlusLapsService.addLap(elapsed);

        updateLapsUI(context);
        invalidatePanelSwissKnife(context, R.id.lapsList);
    }

    private void onPauseTimer(final Context context) {
        if (sStopwatch != null) {
            sStopwatch.pause();
        }

        if (sStopwatchTimer != null) {
            sStopwatchTimer.cancel();
            sStopwatchTimer.purge();
            sStopwatchTimer = null;
        }

        sWasRunning = false;

        updateButtonsUI(context);
        invalidatePanel(context);
    }

    private void onResetTimer(final Context context) {
        if (sStopwatch != null) {
            sStopwatch.reset();
        }

        if (sStopwatchTimer != null && sStopwatch != null && !sStopwatch.isRunning()) {
            sStopwatchTimer.cancel();
            sStopwatchTimer = null;
        }

        CocktailSinglePlusLapsService.clearLaps();

        updateLapsUI(context);
        updateButtonsUI(context);
        updateDisplayTimeUI(context);
        invalidatePanel(context);
    }

    private void onClearLaps(Context context) {
        CocktailSinglePlusLapsService.clearLaps();
        updateLapsUI(context);
        invalidatePanelSwissKnife(context, R.id.lapsList);
        invalidatePanel(context);
    }

    private void updateButtonsUI(Context context) {
        if (sPanelView == null) {
            error(context, "updateButtonsUI: Invalid sPanelView");
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

        debug(context, "Stopwatch state is: " + state.name());

        switch (state) {
            case None:
                sPanelView.setViewVisibility(R.id.notRunningButtons, View.VISIBLE);
                sPanelView.setViewVisibility(R.id.runningButtons, View.GONE);
                sPanelView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Running:
                sPanelView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                sPanelView.setViewVisibility(R.id.runningButtons, View.VISIBLE);
                sPanelView.setViewVisibility(R.id.pausedButtons, View.GONE);
                break;
            case Paused:
                sPanelView.setViewVisibility(R.id.notRunningButtons, View.GONE);
                sPanelView.setViewVisibility(R.id.runningButtons, View.GONE);
                sPanelView.setViewVisibility(R.id.pausedButtons, View.VISIBLE);
                break;
        }
    }

    private void updateLapsUI(Context context) {
        sHelperView.setViewVisibility(
                R.id.helperContainer,
                CocktailSinglePlusLapsService.getCount() > 0 ? View.VISIBLE : View.GONE
        );
    }

    private void updateDisplayTimeUI(Context context) {
        if (sPanelView == null) {
            error(context, "updateDisplayTimeUI: Invalid sPanelView");
            return;
        }

        TimeUtils.Timesnap snap = new TimeUtils.Timesnap(sStopwatch.elapsed());

        if (!sSettings.largeDisplay) {
            // inline display
            sPanelView.setViewVisibility(R.id.timerDisplayInlineText, View.VISIBLE);
            sPanelView.setViewVisibility(R.id.timerDisplayMultilineContainer, View.GONE);

            String displayTime = sStopwatch != null ?
                    snap.toMinutesSecondsCentiseconds(true) :
                    DEFAULT_DISPLAY_TIME;
            sPanelView.setTextViewText(R.id.timerDisplayInlineText, displayTime);
        }
        else {
            // multiline display
            sPanelView.setViewVisibility(R.id.timerDisplayInlineText, View.GONE);
            sPanelView.setViewVisibility(R.id.timerDisplayMultilineContainer, View.VISIBLE);

            if (snap.minutes > 0) {
                sPanelView.setViewVisibility(R.id.timerDisplayLargeMinutesLineText, View.VISIBLE);
                sPanelView.setTextViewText(R.id.timerDisplayLargeMinutesLineText,
                    StringUtils.format("%02d", snap.minutes));
            }
            else {
                sPanelView.setViewVisibility(R.id.timerDisplayLargeMinutesLineText, View.GONE);
            }

            sPanelView.setTextViewText(R.id.timerDisplayLargeSecondsLineText,
                    StringUtils.format("%02d", snap.seconds));
            sPanelView.setTextViewText(R.id.timerDisplayLargeCentisLineText,
                    StringUtils.format("%02d", snap.millis / 10));
        }
    }

    private void updateThemeUI(Context context) {
        if (sPanelView == null) {
            error(context, "updateThemeUI: Invalid sPanelView");
            return;
        }

        debug(context, "Theme: " + sSettings.theme);

        int textColor;
        int panelUpperColorRes;
        int panelLowerColorRes;

        if (sSettings.theme.equals(ResourcesUtils.getString(context, R.string.pref_theme_value_light))) {
            textColor = ResourcesUtils.getColor(context, R.color.colorTextDark);
            panelUpperColorRes = R.color.colorLightBackgroundLighter;
            panelLowerColorRes = R.color.colorLightBackground;
        } else {
            textColor = ResourcesUtils.getColor(context, R.color.colorTextLight);
            panelUpperColorRes = R.color.colorDarkBackground;
            panelLowerColorRes = R.color.colorDarkBackgroundLighter;
        }

        sPanelView.setInt(R.id.panelUpperContainer, "setBackgroundResource", panelUpperColorRes);
        sPanelView.setInt(R.id.panelLowerContainer, "setBackgroundResource", panelLowerColorRes);
        sPanelView.setTextColor(R.id.timerDisplayInlineText, textColor);
        sPanelView.setTextColor(R.id.timerDisplayLargeSecondsLineText, textColor);
        sPanelView.setTextColor(R.id.timerDisplayLargeMinutesLineText, textColor);
        sPanelView.setTextColor(R.id.timerDisplayLargeCentisLineText, textColor);
    }

    private void loadSettings(Context context) {
        sSettings.largeDisplay = PreferencesUtils.getBool(context, R.string.pref_large_display_key);
        sSettings.theme = PreferencesUtils.getString(context, R.string.pref_theme_key);

        debug(context, "Display: " + sSettings.largeDisplay);
        debug(context, "Theme: " + sSettings.theme);
    }

    private void updateUI(Context context) {
        updateDisplayTimeUI(context);
        updateButtonsUI(context);
        updateLapsUI(context);
        updateThemeUI(context);
    }

    private void invalidatePanel(Context context) {
        if (sPanelView == null) {
            error(context, "invalidatePanel: Invalid sView");
            return;
        }

        int[] cocktailIds = SlookCocktailManager.getInstance(context).getCocktailIds(
                new ComponentName(context, CocktailSinglePlusReceiver.class));

        for (int cocktailId : cocktailIds) {
//            debug(context, "Panel has cocktail [" + cocktailId + "], updating it");
//            if (cocktailId != sCocktailId) {
//                warn(context, "Unexpected cocktail id, expected " + sCocktailId + ", found " + cocktailId);
//            }
            SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, sPanelView, sHelperView);
        }
    }


    private void invalidatePanelSwissKnife(Context context, int viewId) {
        if (sPanelView == null) {
            error(context, "invalidatePanel: Invalid sView");
            return;
        }

        SlookCocktailManager.getInstance(context).notifyCocktailViewDataChanged(sCocktailId, viewId);
    }


    private void onCocktailEnabled(Context context) {
        debug(context, "COCKTAIL ENABLED");
        Slook slook = new Slook();

        try {
            slook.initialize(context);
        } catch (SsdkUnsupportedException e) {
            error(context, "Slook not supported, not on a Samsung Edge?");
        }
    }

    private void onCocktailUpdate(Context context, int cocktailId) {
        debug(context, "[" + cocktailId + "] COCKTAIL UPDATE");
        sCocktailId = cocktailId;

        initTimer();

        sPanelView = createPanelView(context);
        sHelperView = createHelperView(context);

        loadSettings(context);
        updateUI(context);
        invalidatePanel(context);

        sPreferencesListener = this;
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(sPreferencesListener);
    }

    private void onCocktailVisibilityChanged(Context context, int cocktailId, boolean visible) {
        debug(context, "[" + cocktailId + "] COCKTAIL VISIBILITY CHANGED to " + visible);

        if (visible) {
//            synchronized (sStopwatchSync) {
//                if (sWasRunning)
//                    doStartTimer(context);
//            }
            if (sPreferencesChanged.compareAndSet(true, false)) {
                debug(context, "Detected preference changes, reloading panel");
                loadSettings(context);
                updateUI(context);
                invalidatePanel(context);
            }
        }
//        } else {
//            synchronized (sStopwatchSync) {
//                if (sStopwatchTimer != null) {
//                    sWasRunning = true;
//                    sStopwatchTimer.cancel();
//                    sStopwatchTimer.purge();
//                    sStopwatchTimer = null;
//                } else {
//                    sWasRunning = false;
//                }
//            }
//        }
    }

    private RemoteViews createPanelView(Context context) {
        debug(context, "Creating single_plus_layout");

        RemoteViews panelView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.single_plus_layout);

        setViewAction(context, panelView, R.id.startButton, ACTION_START_TIMER);
        setViewAction(context, panelView, R.id.lapButton, ACTION_LAP_TIMER);
        setViewAction(context, panelView, R.id.resumeButton, ACTION_RESUME_TIMER);
        setViewAction(context, panelView, R.id.stopButton, ACTION_PAUSE_TIMER);
        setViewAction(context, panelView, R.id.resetButton, ACTION_RESET_TIMER);

        return panelView;
    }

    private RemoteViews createHelperView(Context context) {
        debug(context, "Creating single_plus_helper_layout");

        RemoteViews helperView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_layout);

        setViewAction(context, helperView, R.id.lapsClearButton, ACTION_CLEAR_LAPS);

        Intent intent = new Intent(context, CocktailSinglePlusLapsService.class);
        helperView.setRemoteAdapter(R.id.lapsList, intent);

        return helperView;
    }

    private void initTimer() {
        if (sStopwatch == null) {
            sStopwatch = new Stopwatch();
        }

        if (sStopwatchTimer != null) {
            sStopwatchTimer.cancel();
            sStopwatchTimer.purge();
        }

         sStopwatchTimer = new Timer();
    }

    private void setViewAction(Context context, RemoteViews remoteView, int viewId, String action) {
        Intent clickIntent = new Intent(context, CocktailSinglePlusReceiver.class);
        clickIntent.setAction(action);
        clickIntent.putExtra(EXTRA_COCKTAIL_ID, sCocktailId);

        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteView.setOnClickPendingIntent(viewId, clickPendingIntent);
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
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stes) {
            Log.e(TAG, "[E]@ " + ste);
        }
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Detected preferences changes, marking sPreferencesChanged = true");
        sPreferencesChanged.set(true);
    }
}