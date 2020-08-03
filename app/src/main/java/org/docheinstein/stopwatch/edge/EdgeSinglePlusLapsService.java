package org.docheinstein.stopwatch.edge;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.docheinstein.stopwatch.BuildConfig;
import org.docheinstein.stopwatch.R;
import org.docheinstein.stopwatch.logging.Logger;
import org.docheinstein.stopwatch.utils.StringUtils;
import org.docheinstein.stopwatch.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class EdgeSinglePlusLapsService extends RemoteViewsService {
    private static final String TAG = EdgeSinglePlusLapsService.class.getSimpleName();

    private static class Laps {
        private List<String> mLaps = new ArrayList<>();

        public void add(long time) {
            String displayTime = (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds());
            d(null, "Adding lap: " + displayTime);
            mLaps.add(displayTime);
        }

        public void clear() {
            d(null, "Clearing laps");
            mLaps.clear();
        }

        public int count() {
            return mLaps.size();
        }

        public String get(int position) {
            return mLaps.get(position);
        }
    }

    private static Laps sLaps;

    public static void addLap(long time) {
        if (sLaps == null)
            sLaps = new Laps();
        sLaps.add(time);
    }

    public static void clearLaps() {
        if (sLaps == null)
            sLaps = new Laps();
        sLaps.clear();
    }

    public static int getLapsCount() {
        if (sLaps == null)
            sLaps = new Laps();
        return sLaps.count();
    }

    public String getLap(int position) {
        if (sLaps == null)
            sLaps = new Laps();
        return sLaps.get(position);
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusLapsViewFactory(intent);
    }

    public class CocktailSinglePlusLapsViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private final String TAG = CocktailSinglePlusLapsViewFactory.class.getSimpleName();

        public CocktailSinglePlusLapsViewFactory(Intent intent) {}

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
            d(null, "getCount -> " + getLapsCount());
            return getLapsCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            d(null, "getViewAt: " + position);

            if (position >= getLapsCount()) {
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_item_layout);

            itemView.setTextViewText(R.id.lapItemText, getLap(position));
            itemView.setTextViewText(R.id.lapItemPosition,
                    StringUtils.format( "%d.", position + 1));

            return itemView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.single_plus_helper_item_layout);
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
