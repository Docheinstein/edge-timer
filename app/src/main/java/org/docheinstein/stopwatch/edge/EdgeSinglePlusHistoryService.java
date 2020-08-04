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

public class EdgeSinglePlusHistoryService extends RemoteViewsService {
    private static final String TAG = EdgeSinglePlusHistoryService.class.getSimpleName();

    private static class History {
        private static final String PREF_HISTORY_COUNT_KEY = "pref_history_count_key";
        private static final String PREF_HISTORY_NTH_ = "pref_history_";

        private List<String> mHistory;

        public History(Context context) {
            // Load times from preferences
            int timesCount = PreferencesUtils.getInt(context, PREF_HISTORY_COUNT_KEY);
            Logger.d(context, TAG, "Will load " + timesCount + " times from history");
            mHistory = new ArrayList<>(timesCount);
            for (int i = 0; i < timesCount; i++) {
                String displayTime = PreferencesUtils.getString(context, PREF_HISTORY_NTH_ + i);
                if (StringUtils.isValid(displayTime))
                    mHistory.add(displayTime);
                else
                    Logger.w(context, TAG, "Invalid display time at position " + i + "/" + timesCount);
            }
        }

        public void add(Context context, long time) {
            String displayTime = (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds());
            int idx = mHistory.size();
            Logger.d(null, TAG, "Adding time '" + displayTime + "' at position " + idx);
            mHistory.add(displayTime);
            PreferencesUtils.getWriter(context)
                    .putString(PREF_HISTORY_NTH_ + idx, displayTime)
                    .putInt(PREF_HISTORY_COUNT_KEY, mHistory.size())
                    .apply();
        }

        public void clear(Context context) {
            Logger.d(null, TAG, "Clearing times");
            mHistory.clear();
            PreferencesUtils.setInt(context, PREF_HISTORY_COUNT_KEY, mHistory.size());
        }

        public int count() {
            return mHistory.size();
        }

        public String get(int position) {
            return mHistory.get(position);
        }
    }

    private static History sHistory;

    public static void addHistoryTime(Context context, long time) {
        getHistory(context).add(context, time);
    }

    public static void clearHistory(Context context) {
        getHistory(context).clear(context);
    }

    public static int getHistoryCount(Context context) {
        return getHistory(context).count();
    }

    public static String getHistoryTime(Context context, int position) {
        return getHistory(context).get(position);
    }

    private static History getHistory(Context context) {
        if (sHistory == null)
            sHistory = new History(context);
        return sHistory;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusHistoryViewFactory(getBaseContext());
    }

    public static class CocktailSinglePlusHistoryViewFactory implements RemoteViewsFactory {

        private final String TAG = CocktailSinglePlusHistoryViewFactory.class.getSimpleName();

        private final Context mContext;

        public CocktailSinglePlusHistoryViewFactory(Context context) {
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
            return getHistoryCount(mContext);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Logger.d(null, TAG, "getViewAt: " + position);

            if (position >= getHistoryCount(mContext)) {
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_time_item_layout);

            itemView.setTextViewText(R.id.timeItemText, getHistoryTime(mContext, position));
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
}
