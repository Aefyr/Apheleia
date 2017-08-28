package com.aefyr.apheleia;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;

import com.aefyr.apheleia.watcher.WatcherHelper;


/**
 * Created by Aefyr on 26.08.2017.
 */

public class PreferencesFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        (findPreference("watcher_enabled")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                WatcherHelper.setWatcherEnabled(getActivity(), ((SwitchPreference)preference).isChecked());
                return false;
            }
        });

        findPreference("allow_watcher_with_cell").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(((SwitchPreference)preference).isChecked())
                    WatcherHelper.showNetworkWarning(getActivity());

                return false;
            }
        });
    }
}
