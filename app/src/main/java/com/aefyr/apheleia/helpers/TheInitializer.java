package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.MarksGrid;
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

    private class InitializationTask extends AsyncTask<Void, Void, Void>{

        private ProgressDialog dialog;
        private int i = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
            dialog.setMessage(c.getString(R.string.loading_data));
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
                                final PeriodsHelper periodsHelper = PeriodsHelper.getInstance(c);
                                periodsHelper.savePeriodsInfo(periods);

                                if(i++==result.getStudents().size()-1){
                                    i = 0;
                                    for(final Student s: result.getStudents()){
                                        profileHelper.setCurrentStudent(s.id());
                                        loadDiary(helper.getPersona(), s.id(), periodsHelper.getCurrentWeek(), new EljurApiClient.JournalismListener<DiaryEntry>() {
                                            @Override
                                            public void onSuccess(DiaryEntry entry) {
                                                profileHelper.setCurrentStudent(s.id());
                                                if(!DiaryHelper.getInstance(c).saveEntry(entry, periodsHelper.getCurrentWeek()))
                                                    listener.OnError("Критическая ошибка. Не удалось сериализовать дневник");


                                                if(i++==result.getStudents().size()*2-1) {
                                                    dialog.dismiss();
                                                    listener.OnSuccess();
                                                }
                                            }

                                            @Override
                                            public void onNetworkError() {
                                                listener.OnError(c.getString(R.string.network_error_tip));
                                            }

                                            @Override
                                            public void onApiError(String message, String json) {
                                                listener.OnError(message);
                                            }
                                        });

                                        loadMarks(helper.getPersona(), s.id(), periodsHelper.getCurrentPeriod(), new EljurApiClient.JournalismListener<MarksGrid>() {
                                            @Override
                                            public void onSuccess(MarksGrid grid) {
                                                profileHelper.setCurrentStudent(s.id());
                                                if(!MarksHelper.getInstance(c).saveGrid(grid, periodsHelper.getCurrentPeriod()))
                                                    listener.OnError("Критическая ошибка. Не удалось сериализовать оценки");

                                                if(i++==result.getStudents().size()*2-1) {
                                                    dialog.dismiss();
                                                    listener.OnSuccess();
                                                }
                                            }

                                            @Override
                                            public void onNetworkError() {
                                                listener.OnError(c.getString(R.string.network_error_tip));
                                            }

                                            @Override
                                            public void onApiError(String message, String json) {
                                                listener.OnError(message);
                                            }
                                        });


                                    }
                                }
                            }

                            @Override
                            public void onNetworkError() {
                                listener.OnError(c.getString(R.string.network_error_tip));
                            }

                            @Override
                            public void onApiError(String message, String json) {
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
                public void onApiError(String message, String json) {
                    listener.OnError(message);
                }
            });

            return null;
        }
    }

    private void loadProfile(EljurPersona persona, EljurApiClient.JournalismListener<PersonaInfo> listener){
        EljurApiClient.getInstance(c).getRules(persona, listener);
    }

    private void loadPeriods(EljurPersona persona, String studentId, EljurApiClient.JournalismListener<PeriodsInfo> listener){
        EljurApiClient.getInstance(c).getPeriods(persona, studentId, listener);
    }

    private void loadDiary(EljurPersona persona, String studentId, String days,  EljurApiClient.JournalismListener<DiaryEntry> listener){
        EljurApiClient.getInstance(c).getDiary(persona, studentId, days, true, listener);
    }

    private void loadMarks(EljurPersona persona, String studentId, String days,  EljurApiClient.JournalismListener<MarksGrid> listener){
        EljurApiClient.getInstance(c).getMarks(persona, studentId, days, listener);
    }
}
