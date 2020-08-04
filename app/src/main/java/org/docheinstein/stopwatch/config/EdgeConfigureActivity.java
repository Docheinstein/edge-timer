package org.docheinstein.stopwatch.config;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import org.docheinstein.stopwatch.R;

public class EdgeConfigureActivity extends AppCompatActivity {
    public static class EdgeConfigurePreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);
        }
    }

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
