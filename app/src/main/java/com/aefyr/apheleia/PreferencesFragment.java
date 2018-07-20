package com.aefyr.apheleia;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.aefyr.apheleia.custom.ApheleiaRequest;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.Tutorial;
import com.aefyr.apheleia.watcher.WatcherHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurApiRequest;
import com.aefyr.journalism.exceptions.JournalismException;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.JsonParser;

import java.io.IOException;


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
        initializeDebugPrefs();
    }

    private void initializeQuickPickerPrefs() {
        findPreference("quick_day_picker_enabled").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Chief.makeAToast(getActivity(), getString(R.string.quick_picker_warn));
                return true;
            }
        });
    }

    private void initializeWatcherPrefs() {
        watcherEnabled = findPreference("watcher_enabled");
        watcherViaCell = findPreference("allow_watcher_with_cell");

        watcherViaCell.setEnabled(prefs.getBoolean("watcher_enabled", false));

        watcherEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = (boolean) newValue;

                WatcherHelper.setWatcherEnabled(getActivity(), checked);
                watcherViaCell.setEnabled(checked);

                return true;
            }
        });

        watcherViaCell.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = (boolean) newValue;
                if (checked)
                    WatcherHelper.showNetworkWarning(getActivity());

                return true;
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

    private void initializeDebugPrefs() {
        if (!PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false) && !Debug.debugMode) {
            getPreferenceScreen().removePreference(findPreference("debug"));
            return;
        }

        Preference week = findPreference("debug_override_week");
        Preference period = findPreference("debug_override_period");
        Preference fcmToken = findPreference("debug_fcm_token");
        Preference dumpDiary = findPreference("debug_dump_diary");

        Preference.OnPreferenceClickListener debugListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                switch (preference.getKey()) {
                    case "debug_override_week":
                    case "debug_override_period":
                        final boolean week = preference.getKey().equals("debug_override_week");
                        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(week ? "Enter week" : "Enter period").setView(R.layout.edit_text_dialog_view).setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null).create();
                        dialog.show();
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String enteredText = ((EditText) dialog.findViewById(R.id.dialogEditText)).getText().toString();
                                PeriodsHelper periodsHelper = PeriodsHelper.getInstance(getActivity());

                                if (week)
                                    periodsHelper.setCurrentWeek(enteredText);
                                else
                                    periodsHelper.setCurrentPeriod(enteredText);

                                //Why on next launch? Well this is just debug functionality, so I don't wanna rewrite whole workflow just to allow week swaps like this
                                Chief.makeAToast(getActivity(), "Current " + (week ? "week" : "period") + " for student with ID " + ProfileHelper.getInstance(getActivity()).getCurrentStudentId() + " will be forced to " + enteredText + " on next app launch");
                                dialog.dismiss();
                            }
                        });
                        return false;
                    case "debug_fcm_token":
                        final String fcmTokenV = FirebaseInstanceId.getInstance().getToken();
                        int mc = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("mdb", 0);
                        new AlertDialog.Builder(getActivity()).setTitle("FCM Token").setMessage("MC: " + mc + "\n" + fcmTokenV).setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", fcmTokenV));
                                Chief.makeAToast(getActivity(), "Copied FCM token to clipboard");
                            }
                        }).setNeutralButton("Hijack FCM", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new FCMF(new L() {
                                    @Override
                                    public void onA(String t) {
                                        if (t != null) {
                                            Chief.makeAToast(getActivity(), "Got token for eljur: " + t);
                                            EljurApiClient.getInstance(getActivity()).hijackFCM(Helper.getInstance(getActivity()).getPersona(), t, new EljurApiClient.JournalismListener<Boolean>() {
                                                @Override
                                                public void onSuccess(Boolean result) {
                                                    Chief.makeAToast(getActivity(), "Hijacked FCM: " + result);
                                                }

                                                @Override
                                                public void onNetworkError(boolean tokenIsWrong) {
                                                    Chief.makeAToast(getActivity(), "Net error while hijacking FCM");
                                                }

                                                @Override
                                                public void onApiError(JournalismException e) {

                                                }
                                            });
                                        } else
                                            Chief.makeAToast(getActivity(), "Couldn't get token for eljur");
                                    }
                                }).execute();

                            }
                        }).create().show();
                        return false;
                    case "debug_dump_diary":
                        EljurApiRequest apiRequest = new EljurApiRequest(Helper.getInstance(getActivity()).getPersona(), EljurApiRequest.GET_DIARY).addParameter("days", PeriodsHelper.getInstance(getActivity()).getCurrentWeek()).addParameter("student", ProfileHelper.getInstance(getActivity()).getCurrentStudentId()).addParameter("rings", "__yes");
                        Request diaryRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
                            @Override
                            public void onResponse(final String rawResponse) {

                                final String betterResponse = new JsonParser().parse(rawResponse).toString().replaceAll("[А-Я][а-я]+ ([А-Я][а-я ]+)*[А-Я][а-я]+", "Имя Вырезано");
                                new AlertDialog.Builder(getActivity()).setTitle("Дамп Дневника").setMessage(betterResponse).setPositiveButton("Скопировать", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", betterResponse));
                                        Chief.makeAToast(getActivity(), "Дамп Дневника скопирован в буфер обмена");
                                    }
                                }).setNegativeButton("Закрыть", null).create().show();

                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Chief.makeAnAlert(getActivity(), "Ошибка сети");
                            }
                        });
                        Volley.newRequestQueue(getActivity()).add(diaryRequest);
                        return false;
                }

                return false;
            }
        };

        week.setOnPreferenceClickListener(debugListener);
        period.setOnPreferenceClickListener(debugListener);
        fcmToken.setOnPreferenceClickListener(debugListener);
        dumpDiary.setOnPreferenceClickListener(debugListener);
    }

    private interface L {
        void onA(String t);
    }

    private class FCMF extends AsyncTask<Void, Void, String> {
        private L l;

        FCMF(L l) {
            super();
            this.l = l;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String eToken;
            try {
                eToken = FirebaseInstanceId.getInstance().getToken("581165343215", "FCM");
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }
            return eToken;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            l.onA(s);
        }
    }
}
