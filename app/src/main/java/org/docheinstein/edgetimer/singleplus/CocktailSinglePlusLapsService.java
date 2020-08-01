package org.docheinstein.edgetimer.singleplus;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.docheinstein.edgetimer.BuildConfig;
import org.docheinstein.edgetimer.R;
import org.docheinstein.edgetimer.utils.StringUtils;
import org.docheinstein.edgetimer.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class CocktailSinglePlusLapsService extends RemoteViewsService {
    private static final String TAG = CocktailSinglePlusLapsService.class.getSimpleName();

    public static List<String> sLaps = new ArrayList<>();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CocktailSinglePlusLapsViewFactory(intent);
    }

    public static void addLap(long time) {
        String displayTime = (new TimeUtils.Timesnap(time).toMinutesSecondsCentiseconds(true));
        Log.d(TAG, "Adding lap: " + displayTime);
        sLaps.add(displayTime);
    }

    public static void clearLaps() {
        sLaps.clear();
    }

    public static int getCount() {
        return sLaps != null ? sLaps.size() : 0;
    }

    public static class CocktailSinglePlusLapsViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private static final String TAG = CocktailSinglePlusLapsViewFactory.class.getSimpleName();

        public CocktailSinglePlusLapsViewFactory(Intent intent) {
            Log.d(TAG,"CocktailSinglePlusLapsViewFactory()");
            sLaps = new ArrayList<>();
        }
        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            Log.d(TAG, "onDataSetChanged");
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return (sLaps != null) ? sLaps.size() : 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Log.d(TAG, "getViewAt " + position);

            if (position >= sLaps.size()) {
                Log.w(TAG, "Can't provide view for out of bounds position (laps length = " + sLaps.size() + ")");
                return null;
            }

            RemoteViews itemView = new RemoteViews(
                    BuildConfig.APPLICATION_ID,
                    R.layout.single_plus_helper_item_layout);

            itemView.setTextViewText(R.id.lapItemText, sLaps.get(position));
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
}
