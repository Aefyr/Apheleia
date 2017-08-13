package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.minor.Student;

import java.util.ArrayList;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class TheInitializer {
    Context c;

    public interface OnInitializationListener{
        void OnSuccess();
        void OnError(String m);
    }

    private OnInitializationListener listener;

    public TheInitializer(Context c, OnInitializationListener listener){
        this.c = c;
        this.listener = listener;
    }

    public void initialize(){
        new InitializationTask().execute();
    }

    private class InitializationTask extends AsyncTask<Void, Integer, String>{

        private ProgressDialog dialog;
        private String error;
        private int i = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
            dialog.setTitle(c.getString(R.string.initializing));
            dialog.setMessage(c.getString(R.string.loading_profile));
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            final Helper helper = Helper.getInstance(c);

            loadProfile(helper.getPersona(), new EljurApiClient.JournalismListener<PersonaInfo>() {
                @Override
                public void onSuccess(final PersonaInfo result) {
                    final ProfileHelper profileHelper = ProfileHelper.getInstance(c);
                    profileHelper.savePersonaInfo(result);


                    for(final Student s: result.getStudents()){
                        loadPeriods(helper.getPersona(), s.id(), new EljurApiClient.JournalismListener<PeriodsInfo>() {
                            @Override
                            public void onSuccess(PeriodsInfo periods) {
                                profileHelper.setCurrentStudent(s.id());
                                PeriodsHelper periodsHelper = PeriodsHelper.getInstance(c);
                                periodsHelper.savePeriodsInfo(periods);

                                if(i++==result.getStudents().size()-1)
                                    listener.OnSuccess();
                            }

                            @Override
                            public void onNetworkError() {
                                listener.OnError(c.getString(R.string.network_error_tip));
                            }

                            @Override
                            public void onApiError(String message) {
                                listener.OnError(message);
                            }
                        });
                    }

                }

                @Override
                public void onNetworkError() {
                    listener.OnError(c.getString(R.string.network_error_tip));
                }

                @Override
                public void onApiError(String message) {
                    listener.OnError(message);
                }
            });

            return error;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]){
                case InitializationStep.LOADING_PROFILE:
                    dialog.setMessage(c.getString(R.string.loading_profile));
                    break;
                case InitializationStep.LOADING_PERIODS:
                    dialog.setMessage(c.getString(R.string.loading_periods));
                    break;

            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String aBool) {
            super.onPostExecute(aBool);
        }
    }

    private class InitializationStep{
        private static final int LOADING_PROFILE = 0;
        private static final int LOADING_PERIODS = 1;
    }

    private void loadProfile(EljurPersona persona, EljurApiClient.JournalismListener<PersonaInfo> listener){
        EljurApiClient.getInstance(c).getRules(persona, listener);
    }

    private void loadPeriods(EljurPersona persona, String studentId, EljurApiClient.JournalismListener<PeriodsInfo> listener){
        EljurApiClient.getInstance(c).getPeriods(persona, studentId, listener);
    }
}
