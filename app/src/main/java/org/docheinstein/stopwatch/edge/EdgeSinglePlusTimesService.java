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

public class EdgeSinglePlusTimesService extends RemoteViewsService {
    private static final String TAG = EdgeSinglePlusTimesService.class.getSimpleName();

    private static class Times {
        private static final String PREF_TIMES_COUNT_KEY = "pref_times_count_key";
        private static final String PREF_TIMES_NTH_ = "pref_time_";

        private final Context mContext;

        private List<String> mTimes;

        public Times(Context context) {
            mContext = context;
            // Load times from preferences
            int timesCount = PreferencesUtils.getInt(mContext, PREF_TIMES_COUNT_KEY);
            d(context, "Will load " + timesCount + " times");
            mTimes = new ArrayList<>(timesCount);
            for (int i = 0; i < timesCount; i++) {
                String displayTime = PreferencesUtils.getString(context, PREF_TIMES_NTH_ + i);
                if (StringUtils.isValid(displayTime))
                    mTimes.add(displayTime);
                else
                    w(context, "Invalid display time at position " + i + "/" + timesCount);
            }
        }

        public void add(long time) {
            String displayTime = (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds());
            int idx = mTimes.size();
            d(null, "Adding time '" + displayTime + "' at position " + idx);
            mTimes.add(displayTime);
            PreferencesUtils.getWriter(mContext)
                    .putString(PREF_TIMES_NTH_ + idx, displayTime)
                    .putInt(PREF_TIMES_COUNT_KEY, mTimes.size())
                    .apply();
        }

        public void clear() {
            d(null, "Clearing times");
            mTimes.clear();
            PreferencesUtils.setInt(mContext, PREF_TIMES_COUNT_KEY, mTimes.size());
        }

        public int count() {
            return mTimes.size();
        }

        public String get(int position) {
            return mTimes.get(position);
        }
    }

    private static Times sTimes;

    public static void addTime(Context context, long time) {
        getTimes(context).add(time);
    }

    public static void clearTimes(Context context) {
        getTimes(context).clear();
    }

    public static int getTimesCount(Context context) {
        return getTimes(context).count();
    }

    public static String getTime(Context context, int position) {
        return getTimes(context).get(position);
    }

    private static Times getTimes(Context context) {
        if (sTimes == null)
            sTimes = new Times(context);
        return sTimes;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusTimesViewFactory(intent);
    }

    public class CocktailSinglePlusTimesViewFactory implements RemoteViewsFactory {

        private final String TAG = CocktailSinglePlusTimesViewFactory.class.getSimpleName();

        public CocktailSinglePlusTimesViewFactory(Intent intent) { }

        @Override
        public void onCreate() {}

        @Override
        public void onDataSetChanged() {
            d(null, "onDataSetChanged");
        }

        @Override
        public void onDestroy() {}

        @Override
        public int getCount() {
            return getTimesCount(getBaseContext());
        }

        @Override
        public RemoteViews getViewAt(int position) {
            d(null, "getViewAt: " + position);

            if (position >= getTimesCount(getBaseContext())) {
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_time_item_layout);

            itemView.setTextViewText(R.id.timeItemText, getTime(getBaseContext(), position));
            itemView.setTextViewText(R.id.timeItemPosition,
                    StringUtils.format( "%d.", position + 1));

            return itemView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_time_item_layout);
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


    private static void etrace(Context ctx, String s) { Logger.getInstance(ctx).etrace(TAG, s); }
    private static void wtrace(Context ctx, String s) { Logger.getInstance(ctx).wtrace(TAG, s); }
    private static void e(Context ctx, String s) { Logger.getInstance(ctx).e(TAG, s); }
    private static void w(Context ctx, String s) { Logger.getInstance(ctx).w(TAG, s); }
    private static void i(Context ctx, String s) { Logger.getInstance(ctx).i(TAG, s); }
    private static void d(Context ctx, String s) { Logger.getInstance(ctx).d(TAG, s); }
}
