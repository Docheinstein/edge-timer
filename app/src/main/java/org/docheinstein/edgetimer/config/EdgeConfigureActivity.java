package org.docheinstein.edgetimer.config;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.docheinstein.edgetimer.R;

public class EdgeConfigureActivity extends AppCompatActivity {
    private static final String TAG = "EdgeConfigureActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_layout);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferenceFragment, new EdgeConfigurePreferencesFragment())
                .commit();

    }
}
