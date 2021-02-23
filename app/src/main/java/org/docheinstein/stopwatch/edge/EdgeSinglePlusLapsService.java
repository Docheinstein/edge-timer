package org.docheinstein.stopwatch.edge;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.docheinstein.stopwatch.BuildConfig;
import org.docheinstein.stopwatch.R;
import org.docheinstein.stopwatch.logging.Logger;
import org.docheinstein.stopwatch.utils.PreferencesUtils;
import org.docheinstein.stopwatch.utils.StringUtils;
import org.docheinstein.stopwatch.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class EdgeSinglePlusLapsService extends RemoteViewsService {
    private static final String TAG = EdgeSinglePlusLapsService.class.getSimpleName();

    public static final String PREF_LAP_COUNT = "pref_lap_count";
    public static final String PREF_LAP_NTH_ = "pref_lap_";

    private static class Laps {

        private final List<String> mLaps;

        public Laps(Context context) {
            // Load laps from preferences
            // (necessary only because app can get killed by low memory killer)
            int timesCount = PreferencesUtils.getInt(context, PREF_LAP_COUNT);
            Logger.i(context, TAG, "Will load " + timesCount + " laps from memory");
            mLaps = new ArrayList<>(timesCount);
            for (int i = 0; i < timesCount; i++) {
                String displayTime = PreferencesUtils.getString(context, PREF_LAP_NTH_ + i);
                if (StringUtils.isValid(displayTime))
                    mLaps.add(displayTime);
                else
                    Logger.w(context, TAG, "Invalid display time at position " + i + "/" + timesCount);
            }
        }

        public void add(Context context, long time) {
            String displayTime = (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds());
            int idx = mLaps.size();
            Logger.i(null, TAG, "Adding lap '" + displayTime + "' at position " + idx);
            mLaps.add(displayTime);
            PreferencesUtils.getWriter(context)
                    .putString(PREF_LAP_NTH_ + idx, displayTime)
                    .putInt(PREF_LAP_COUNT, mLaps.size())
                    .apply();
        }

        public void clear(Context context) {
            Logger.i(null, TAG, "Clearing laps");
            mLaps.clear();
            PreferencesUtils.setInt(context, PREF_LAP_COUNT, mLaps.size());
        }

        public int count() {
            return mLaps.size();
        }

        public String get(int position) {
            return mLaps.get(position);
        }
    }

    private static Laps sLaps;

    public static void addLap(Context context, long time) {
        getLaps(context).add(context, time);
    }

    public static void clearLaps(Context context) {
        getLaps(context).clear(context);
    }

    public static int getLapsCount(Context context) {
        return getLaps(context).count();
    }

    public static String getLap(Context context, int position) {
        return getLaps(context).get(position);
    }

    private static Laps getLaps(Context context) {
        if (sLaps == null)
            sLaps = new Laps(context);
        return sLaps;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusLapsViewFactory(getBaseContext());
    }

    public static class CocktailSinglePlusLapsViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private static final String TAG = CocktailSinglePlusLapsViewFactory.class.getSimpleName();

        private final Context mContext;

        public CocktailSinglePlusLapsViewFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {}

        @Override
        public void onDataSetChanged() {
            Logger.d(null, TAG, "onDataSetChanged");
        }

        @Override
        public void onDestroy() {}

        @Override
        public int getCount() {
            return getLapsCount(mContext);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Logger.v(null, TAG, "getViewAt: " + position);

            if (position >= getLapsCount(mContext)) {
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_lap_item_layout);

            itemView.setTextViewText(R.id.lapItemText, getLap(mContext, position));
            itemView.setTextViewText(R.id.lapItemPosition,
                    StringUtils.format( "%d.", position + 1));

            return itemView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_lap_item_layout);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
