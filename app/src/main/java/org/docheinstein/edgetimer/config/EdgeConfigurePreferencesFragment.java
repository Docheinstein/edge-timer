package org.docheinstein.edgetimer.config;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.docheinstein.edgetimer.R;

public class EdgeConfigurePreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.edge_timer_prefs, rootKey);
    }
}
