package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.Finals;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.minor.Student;
import com.android.volley.toolbox.StringRequest;

import java.util.HashSet;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class TheInitializer {
    Context c;
    private HashSet<StringRequest> requests;

    public interface OnInitializationListener {
        void OnSuccess();

        void OnError(String message);
    }

    private OnInitializationListener listener;


    public TheInitializer(Context c, OnInitializationListener listener) {
        this.c = c;
        this.listener = listener;
        requests = new HashSet<>();
    }

    public void initialize() {
        new InitializationTask().execute();
    }

    private boolean finished;
    private ProgressDialog dialog;

    private class InitializationTask extends AsyncTask<Void, Integer, Void> {


        private int i = 0;
        private int actionsPerStudent = 5;
        private int actionsPerAccount = 1;
        private int actionsGoal;


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

                    actionsGoal = result.getStudents().size() * actionsPerStudent + actionsPerAccount;
                    publishProgress(0);


                    for (final Student s : result.getStudents()) {
                        loadPeriods(helper.getPersona(), s.id(), new EljurApiClient.JournalismListener<PeriodsInfo>() {
                            @Override
                            public void onSuccess(PeriodsInfo periods) {
                                profileHelper.setCurrentStudent(s.id());
                                System.out.println("Set student " + s.id());
                                final PeriodsHelper periodsHelper = PeriodsHelper.getInstance(c);
                                periodsHelper.savePeriodsInfo(periods);
                                System.out.println("Saved periods for " + s.id());
                                System.out.println("period " + s.id() + " " + periodsHelper.getCurrentWeek());

                                publishProgress(++i);

                                profileHelper.setCurrentStudent(s.id());
                                loadDiary(helper.getPersona(), s.id(), periodsHelper.getCurrentWeek(), new EljurApiClient.JournalismListener<DiaryEntry>() {
                                    @Override
                                    public void onSuccess(DiaryEntry entry) {
                                        profileHelper.setCurrentStudent(s.id());
                                        if (!DiaryHelper.getInstance(c).saveEntry(entry, periodsHelper.getCurrentWeek()))
                                            fail("Критическая ошибка. Не удалось сериализовать дневник");


                                        if (++i == actionsGoal) {
                                            done();
                                        }

                                        publishProgress(i);
                                    }

                                    @Override
                                    public void onNetworkError(boolean tokenIsWrong) {
                                        fail(c.getString(R.string.network_error_tip));
                                    }

                                    @Override
                                    public void onApiError(JournalismException e) {
                                        fail(null);
                                    }
                                });

                                loadMarks(helper.getPersona(), s.id(), periodsHelper.getCurrentPeriod(), new EljurApiClient.JournalismListener<MarksGrid>() {
                                    @Override
                                    public void onSuccess(MarksGrid grid) {
                                        profileHelper.setCurrentStudent(s.id());
                                        if (!MarksHelper.getInstance(c).saveGrid(grid, periodsHelper.getCurrentPeriod()))
                                            fail("Критическая ошибка. Не удалось сериализовать оценки");

                                        if (++i == actionsGoal) {
                                            done();
                                        }

                                        publishProgress(i);
                                    }

                                    @Override
                                    public void onNetworkError(boolean tokenIsWrong) {
                                        fail(c.getString(R.string.network_error_tip));
                                    }

                                    @Override
                                    public void onApiError(JournalismException e) {
                                        fail(null);
                                    }
                                });

                                loadSchedule(helper.getPersona(), s.id(), periodsHelper.getCurrentScheduleWeek(), new EljurApiClient.JournalismListener<Schedule>() {
                                    @Override
                                    public void onSuccess(Schedule schedule) {
                                        profileHelper.setCurrentStudent(s.id());
                                        if (!ScheduleHelper.getInstance(c).saveSchedule(schedule, periodsHelper.getCurrentScheduleWeek()))
                                            fail("Критическая ошибка. Не удалось сериализовать расписание");

                                        if (++i == actionsGoal) {
                                            done();
                                        }

                                        publishProgress(i);
                                    }

                                    @Override
                                    public void onNetworkError(boolean tokenIsWrong) {
                                        fail(c.getString(R.string.network_error_tip));
                                    }

                                    @Override
                                    public void onApiError(JournalismException e) {
                                        fail(null);
                                    }
                                });

                            }

                            @Override
                            public void onNetworkError(boolean tokenIsWrong) {
                                fail(c.getString(R.string.network_error_tip));
                            }

                            @Override
                            public void onApiError(JournalismException e) {
                                fail(null);
                            }
                        });

                        loadFinals(helper.getPersona(), s.id(), new EljurApiClient.JournalismListener<Finals>() {
                            @Override
                            public void onSuccess(Finals finals) {
                                profileHelper.setCurrentStudent(s.id());
                                if (!FinalsHelper.getInstance(c).saveFinals(finals))
                                    fail("Критическая ошибка. Не удалось сериализовать расписание");

                                if (++i == actionsGoal) {
                                    done();
                                }

                                publishProgress(i);
                            }

                            @Override
                            public void onNetworkError(boolean tokenIsWrong) {
                                fail(c.getString(R.string.network_error_tip));
                            }

                            @Override
                            public void onApiError(JournalismException e) {
                                fail(null);
                            }
                        });


                    }

                    loadMessages(helper.getPersona(), new EljurApiClient.JournalismListener<MessagesList>() {
                        @Override
                        public void onSuccess(MessagesList messages) {
                            MessagesHelper messagesHelper = MessagesHelper.getInstance(c);
                            messagesHelper.saveMessages(messages, messages.getFolder() == MessagesList.Folder.INBOX, new MessagesHelper.SaveMessagesTaskResultListener() {
                                @Override
                                public void onSaveCompleted(boolean successful) {
                                    if (successful) {
                                        if (++i == actionsGoal) {
                                            done();
                                        }

                                        publishProgress(i);
                                    } else {
                                        fail("Критическая ошибка. Не удалось сериализовать сообщения");
                                    }
                                }
                            });
                        }

                        @Override
                        public void onNetworkError(boolean tokenIsWrong) {
                            fail(c.getString(R.string.network_error_tip));
                        }

                        @Override
                        public void onApiError(JournalismException e) {
                            fail(null);
                        }
                    });

                }

                @Override
                public void onNetworkError(boolean tokenIsWrong) {
                    fail(c.getString(R.string.network_error_tip));
                }

                @Override
                public void onApiError(JournalismException e) {
                    switch (e.getMessage()){
                        case "unsupported role":
                            fail(c.getString(R.string.unupported_role));
                            break;
                        default:
                            break;
                    }
                }
            });

            return null;
        }


        private boolean styleUpdated;

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (!styleUpdated) {
                dialog.dismiss();

                dialog = new ProgressDialog(c);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setTitle(c.getString(R.string.loading_data));
                dialog.setMax(actionsGoal);
                dialog.setProgress(0);
                dialog.show();
                styleUpdated = true;
            } else
                dialog.setProgress(values[0]);

        }
    }


    private void fail(String m) {
        if (finished)
            return;
        finished = true;

        for (StringRequest request : requests) {
            if (!request.hasHadResponseDelivered())
                request.cancel();
        }

        dialog.dismiss();
        listener.OnError(m==null?c.getString(R.string.error_api):m);
    }

    private void done() {
        if (finished)
            return;
        finished = true;
        dialog.dismiss();
        listener.OnSuccess();
    }

    private void loadProfile(EljurPersona persona, EljurApiClient.JournalismListener<PersonaInfo> listener) {
        requests.add(EljurApiClient.getInstance(c).getRules(persona, listener));
    }

    private void loadPeriods(EljurPersona persona, String studentId, EljurApiClient.JournalismListener<PeriodsInfo> listener) {
        requests.add(EljurApiClient.getInstance(c).getPeriods(persona, studentId, listener));
    }

    private void loadDiary(EljurPersona persona, String studentId, String days, EljurApiClient.JournalismListener<DiaryEntry> listener) {
        requests.add(EljurApiClient.getInstance(c).getDiary(persona, studentId, days, true, listener));
    }

    private void loadMarks(EljurPersona persona, String studentId, String days, EljurApiClient.JournalismListener<MarksGrid> listener) {
        requests.add(EljurApiClient.getInstance(c).getMarks(persona, studentId, days, listener));
    }

    private void loadSchedule(EljurPersona persona, String studentId, String days, EljurApiClient.JournalismListener<Schedule> listener) {
        requests.add(EljurApiClient.getInstance(c).getSchedule(persona, studentId, days, true, listener));
    }

    private void loadMessages(EljurPersona persona, EljurApiClient.JournalismListener<MessagesList> listener) {
        requests.add(EljurApiClient.getInstance(c).getMessages(persona, MessagesList.Folder.INBOX, false, listener));
        requests.add(EljurApiClient.getInstance(c).getMessages(persona, MessagesList.Folder.SENT, false, listener));
    }

    private void loadFinals(EljurPersona persona, String studentId, EljurApiClient.JournalismListener<Finals> listener) {
        requests.add(EljurApiClient.getInstance(c).getFinals(persona, studentId, listener));
    }

}
