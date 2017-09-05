package com.aefyr.apheleia;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;

import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Tutorial;
import com.aefyr.apheleia.watcher.WatcherHelper;


/**
 * Created by Aefyr on 26.08.2017.
 */

public class PreferencesFragment extends PreferenceFragment {

    SharedPreferences prefs;

    //Watcher
    private Preference watcherEnabled;
    private Preference watcherViaCell;

    //Tutorial
    private Preference showTutorial;

    int i = 0;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        initializeWatcherPrefs();
        initializeTutorialPrefs();
        initializeQuickPickerPrefs();
    }

    private void initializeQuickPickerPrefs(){
        findPreference("quick_day_picker_enabled").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Chief.makeAToast(getActivity(), getString(R.string.quick_picker_warn));
                return false;
            }
        });
    }

    private void initializeWatcherPrefs() {
        watcherEnabled = findPreference("watcher_enabled");
        watcherViaCell = findPreference("allow_watcher_with_cell");

        watcherViaCell.setEnabled(prefs.getBoolean("watcher_enabled", false));

        watcherEnabled.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean value = ((SwitchPreference) preference).isChecked();
                WatcherHelper.setWatcherEnabled(getActivity(), value);
                watcherViaCell.setEnabled(value);

                return false;
            }
        });

        watcherViaCell.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (((SwitchPreference) preference).isChecked())
                    WatcherHelper.showNetworkWarning(getActivity());

                return false;
            }
        });
    }

    private void initializeTutorialPrefs() {
        showTutorial = findPreference("show_tut");
        showTutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Tutorial.showTutorial(getActivity());
                return true;
            }
        });
    }
}
