package org.docheinstein.edgetimer.singleplus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private static final String ACTION_STOP_TIMER = "org.docheinstein.edgetimer.ACTION_STOP_TIMER";

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
            Log.w(TAG, "Null action?");
            return;
        }

        Log.d(TAG, "CocktailSinglePlusProvider.onReceive(): " + action);

        Bundle extras = intent.getExtras();

        switch (action) {
            case ACTION_START_TIMER:
                Log.d(TAG, "ACTION_START_TIMER");

                if (extras != null && extras.containsKey(EXTRA_COCKTAIL_ID)) {
                    final int cocktailId = extras.getInt(EXTRA_COCKTAIL_ID);

                    Log.d(TAG, "notifyCocktailViewDataChanged of " + cocktailId);
                    mStopwatch = new Stopwatch();
                    mStopwatch.start();

                    mStopwatchTask = new TimerTask() {
                        @Override
                        public void run() {
                            String displayTime = TimeUtils.millisToDisplayTime(mStopwatch.elapsed());
                            Log.d(TAG, displayTime);
                            mView.setTextViewText(R.id.timerText, displayTime);
                                SlookCocktailManager.getInstance(context).updateCocktail(
                                    cocktailId, mView, mHelperView);
                            }
                    };

                    new Timer().scheduleAtFixedRate(mStopwatchTask, 0, 100);
                }
                break;
            case ACTION_STOP_TIMER:
                Log.d(TAG, "ACTION_STOP_TIMER");

                if (mStopwatch != null) {
                    mStopwatch.stop();
                    mStopwatch = null;
                }

                if (mStopwatchTask != null) {
                    mStopwatchTask.cancel();
                    mStopwatchTask = null;
                }

                break;
            case ACTION_COCKTAIL_UPDATE_V2:
                Log.d(TAG, "ACTION_COCKTAIL_UPDATE_V2");

                if (extras != null && extras.containsKey(EXTRA_COCKTAIL_IDS)) {
                    onCocktailUpdate(
                            context,
                            extras.getIntArray(EXTRA_COCKTAIL_IDS)
                    );
                }
                break;
            default:
                Log.w(TAG, "Unknown action received");
                break;
        }
    }

    public void onCocktailUpdate(Context context, int[] cocktailIds) {
        if (cocktailIds == null) {
            Log.e(TAG, "Unexpected cocktailIds array");
            return;
        }

        int cocktailId = cocktailIds[0];

        if (mView == null) {
            Log.d(TAG, "Creating single_plus_layout");
            mView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout);

            setViewAction(context, cocktailId, R.id.startButton, ACTION_START_TIMER);
            setViewAction(context, cocktailId, R.id.resetButton, ACTION_STOP_TIMER);
        }

        if (mHelperView == null) {
            Log.d(TAG, "Creating single_plus_layout_helper");
            mHelperView = new RemoteViews(context.getPackageName(), R.layout.single_plus_layout_helper);
        }

        SlookCocktailManager.getInstance(context).updateCocktail(cocktailId, mView, mHelperView);
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