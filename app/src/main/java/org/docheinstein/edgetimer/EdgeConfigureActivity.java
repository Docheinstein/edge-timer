package org.docheinstein.edgetimer;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class EdgeConfigureActivity extends Activity {
    private static final String TAG = "EdgeConfigureActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_layout);
    }
}
