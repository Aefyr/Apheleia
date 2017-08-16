package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.minor.Student;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class TheInitializer {
    Context c;

    public interface OnInitializationListener{
        void OnSuccess();
        void OnError(String m, String json, String failedWhat);
    }

    private OnInitializationListener listener;


    public TheInitializer(Context c, OnInitializationListener listener){
        this.c = c;
        this.listener = listener;
    }

    public void initialize(){
        new InitializationTask().execute();
    }

    private boolean finished;
    private ProgressDialog dialog;

    private class InitializationTask extends AsyncTask<Void, Integer, Void>{


        private int i = 0;
        private int actions = 3;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
            dialog.setMessage(c.getString(R.string.loading_profile));
            dialog.setMax(0);
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

                    publishProgress(result.getStudents().size());


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
                                                    fail("Критическая ошибка. Не удалось сериализовать дневник", null, null);


                                                if(i++==result.getStudents().size()*actions-1) {
                                                    done();
                                                }

                                                publishProgress(i);
                                            }

                                            @Override
                                            public void onNetworkError() {
                                                fail(c.getString(R.string.network_error_tip), null, null);
                                            }

                                            @Override
                                            public void onApiError(String message, String json) {
                                                fail(message, json, c.getString(R.string.diary));
                                            }
                                        });

                                        loadMarks(helper.getPersona(), s.id(), periodsHelper.getCurrentPeriod(), new EljurApiClient.JournalismListener<MarksGrid>() {
                                            @Override
                                            public void onSuccess(MarksGrid grid) {
                                                profileHelper.setCurrentStudent(s.id());
                                                if(!MarksHelper.getInstance(c).saveGrid(grid, periodsHelper.getCurrentPeriod()))
                                                    fail("Критическая ошибка. Не удалось сериализовать оценки", null, null);

                                                if(i++==result.getStudents().size()*actions-1) {
                                                    done();
                                                }

                                                publishProgress(i);
                                            }

                                            @Override
                                            public void onNetworkError() {
                                                fail(c.getString(R.string.network_error_tip), null, null);
                                            }

                                            @Override
                                            public void onApiError(String message, String json) {
                                                fail(message, json, c.getString(R.string.marks));
                                            }
                                        });

                                        loadSchedule(helper.getPersona(), s.id(), periodsHelper.getCurrentScheduleWeek(), new EljurApiClient.JournalismListener<Schedule>() {
                                            @Override
                                            public void onSuccess(Schedule schedule) {
                                                profileHelper.setCurrentStudent(s.id());
                                                if(!ScheduleHelper.getInstance(c).saveSchedule(schedule, periodsHelper.getCurrentScheduleWeek()))
                                                    fail("Критическая ошибка. Не удалось серилизовать расписание", null, null);

                                                if(i++==result.getStudents().size()*actions-1) {
                                                    done();
                                                }

                                                publishProgress(i);
                                            }

                                            @Override
                                            public void onNetworkError() {
                                                fail(c.getString(R.string.network_error_tip), null, null);
                                            }

                                            @Override
                                            public void onApiError(String message, String json) {
                                                fail(message, json, c.getString(R.string.marks));
                                            }
                                        });


                                    }

                                }
                            }

                            @Override
                            public void onNetworkError() {
                                fail(c.getString(R.string.network_error_tip), null, null);
                            }

                            @Override
                            public void onApiError(String message, String json) {
                                fail(message, json, c.getString(R.string.periods));
                            }
                        });


                    }

                }

                @Override
                public void onNetworkError() {
                    fail(c.getString(R.string.network_error_tip), null, null);
                }

                @Override
                public void onApiError(String message, String json) {
                    fail(message, json, c.getString(R.string.profile));
                }
            });

            return null;
        }


        private boolean styleUpdated;
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(!styleUpdated){
                dialog.dismiss();

                dialog = new ProgressDialog(c);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMessage(c.getString(R.string.loading_data));
                dialog.setMax(values[0]*actions);
                dialog.setProgress(0);
                dialog.show();
                styleUpdated = true;
            }else
                dialog.setProgress(values[0]);

        }
    }



    private void fail(String m, String json, String failedWhat){
        if(finished)
            return;
        finished = true;
        dialog.dismiss();
        listener.OnError(m, json, failedWhat);
    }

    private void done(){
        if(finished)
            return;
        finished = true;
        dialog.dismiss();
        listener.OnSuccess();
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

    private void loadSchedule(EljurPersona persona, String studentId, String days,  EljurApiClient.JournalismListener<Schedule> listener){
        EljurApiClient.getInstance(c).getSchedule(persona, studentId, days, true, listener);
    }
}
