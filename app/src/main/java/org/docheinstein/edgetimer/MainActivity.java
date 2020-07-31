package org.docheinstein.edgetimer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Slook slook = new Slook();

        try {
            slook.initialize(this);
            Log.d(TAG, "Slook v. " + slook.getVersionName());
        } catch (SsdkUnsupportedException e) {
            Log.e(TAG, "SsdkUnsupportedException: " + e.getType());
            return;
        }

        Log.d(TAG, "COCKTAIL_PANEL feature supported " +
                "(Single-Mode, Single-Plus-Mode, Edge-Feeds-Mode): " +
                slook.isFeatureEnabled(Slook.COCKTAIL_PANEL));
        Log.d(TAG, "COCKTAIL_BAR feature supported " +
                "(Edge-Immersive_mode): " +
                slook.isFeatureEnabled(Slook.COCKTAIL_BAR));

        setContentView(R.layout.single_plus_layout);
    }

}
