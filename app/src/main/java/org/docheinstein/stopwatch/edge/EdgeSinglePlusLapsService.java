package org.docheinstein.stopwatch.edge;

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
            Logger.d(null, TAG, "Adding lap: " + displayTime);
            mLaps.add(displayTime);
        }

        public void clear() {
            Logger.d(null, TAG, "Clearing laps");
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
        getLaps().add(time);
    }

    public static void clearLaps() {
        getLaps().clear();
    }

    public static int getLapsCount() {
        return getLaps().count();
    }

    public static String getLap(int position) {
        return getLaps().get(position);
    }

    private static Laps getLaps() {
        if (sLaps == null)
            sLaps = new Laps();
        return sLaps;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusLapsViewFactory();
    }

    public static class CocktailSinglePlusLapsViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private final String TAG = CocktailSinglePlusLapsViewFactory.class.getSimpleName();

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
            Logger.d(null, TAG, "getCount -> " + getLapsCount());
            return getLapsCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Logger.d(null, TAG, "getViewAt: " + position);

            if (position >= getLapsCount()) {
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_lap_item_layout);

            itemView.setTextViewText(R.id.lapItemText, getLap(position));
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
