package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.minor.ActualPeriod;
import com.aefyr.journalism.objects.minor.Week;
import com.google.firebase.crash.FirebaseCrash;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class PeriodsHelper {
    private static PeriodsHelper instance;
    private ProfileHelper profileHelper;
    private SharedPreferences preferences;
    private Context c;

    public interface OnPeriodsChangeDetectedListener {
        void OnFoundMoreWeeks();

        void OnFoundMorePeriods();

        void OnFoundLessWeeks();

        void OnFoundLessPeriods();

        void onNothingChanged();

        void onNetworkError();
    }

    private PeriodsHelper(Context c) {
        instance = this;
        this.c = c;
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
        profileHelper = ProfileHelper.getInstance(c);
    }

    public static PeriodsHelper getInstance(Context c) {
        return instance == null ? new PeriodsHelper(c) : instance;
    }

    public void checkPeriods(final OnPeriodsChangeDetectedListener listener) {
        EljurApiClient.getInstance(c).getPeriods(Helper.getInstance(c).getPersona(), profileHelper.getCurrentStudentId(), new EljurApiClient.JournalismListener<PeriodsInfo>() {
            @Override
            public void onSuccess(PeriodsInfo result) {
                if(result == null){
                    listener.onNothingChanged();
                    return;
                }
                int prevPeriodsCount = getPeriodsCount();
                int prevWeeksCount = getWeeksCount();

                int newPeriodsCount = result.getPeriods().size();
                int newWeeksCount = 0;
                for (ActualPeriod p : result.getPeriods()) {
                    newWeeksCount += p.getWeeks().size();
                }

                if (prevWeeksCount < newWeeksCount) {
                    savePeriodsInfo(result);
                    listener.OnFoundMoreWeeks();
                    return;
                } else if (prevWeeksCount > newWeeksCount) {
                    listener.OnFoundLessWeeks();
                    return;
                }

                if (prevPeriodsCount < newPeriodsCount) {
                    savePeriodsInfo(result);
                    listener.OnFoundMorePeriods();
                    return;
                } else if (prevPeriodsCount > newPeriodsCount) {
                    listener.OnFoundLessPeriods();
                    return;
                }

                listener.onNothingChanged();
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                listener.onNetworkError();
            }

            @Override
            public void onApiError(JournalismException e) {
                Chief.makeAToast(c, c.getString(R.string.check_periods_failed));
                FirebaseCrash.report(e);
            }
        });
    }

    private void savePeriods(Set<String> periods) {
        preferences.edit().putStringSet("periods_" + profileHelper.getCurrentStudentId(), periods).apply();
    }

    public Set<String> getPeriods() {
        return preferences.getStringSet("periods_" + profileHelper.getCurrentStudentId(), null);
    }

    public String getCurrentPeriod() {
        return preferences.getString("current_period_" + profileHelper.getCurrentStudentId(), null);
    }

    public void setCurrentPeriod(String period) {
        preferences.edit().putString("current_period_" + profileHelper.getCurrentStudentId(), period).apply();
    }

    public String getPeriodName(String period) {
        return preferences.getString("period_name_" + profileHelper.getCurrentStudentId() + "_" + period, "nani");
    }

    public int getPeriodsCount() {
        return preferences.getInt("periods_count_" + profileHelper.getCurrentStudentId(), 0);
    }

    private void setPeriodsCount(int count) {
        preferences.edit().putInt("periods_count_" + profileHelper.getCurrentStudentId(), count).apply();
    }

    private void setPeriodName(String period, String name) {
        preferences.edit().putString("period_name_" + profileHelper.getCurrentStudentId() + "_" + period, name).apply();
    }

    private void saveWeeks(Set<String> weeks) {
        preferences.edit().putStringSet("weeks_" + profileHelper.getCurrentStudentId(), weeks).apply();
    }

    public Set<String> getWeeks() {
        return preferences.getStringSet("weeks_" + profileHelper.getCurrentStudentId(), null);
    }

    public String getCurrentWeek() {
        return preferences.getString("current_week_" + profileHelper.getCurrentStudentId(), null);
    }

    public void setCurrentWeek(String weeks) {
        preferences.edit().putString("current_week_" + profileHelper.getCurrentStudentId(), weeks).apply();
    }

    public int getWeeksCount() {
        return preferences.getInt("weeks_count_" + profileHelper.getCurrentStudentId(), 0);
    }

    private void setWeeksCount(int count) {
        preferences.edit().putInt("weeks_count_" + profileHelper.getCurrentStudentId(), count).apply();
    }

    public void setCurrentScheduleWeek(String weeks) {
        preferences.edit().putString("current_schedule_week_" + profileHelper.getCurrentStudentId(), weeks).apply();
    }

    public String getCurrentScheduleWeek() {
        return preferences.getString("current_schedule_week_" + profileHelper.getCurrentStudentId(), null);
    }

    public void savePeriodsInfo(PeriodsInfo periodsInfo) {
        HashSet<String> periods = new HashSet<>();
        String lastPeriod = null;

        TimeLord timeLord = TimeLord.getInstance();

        HashSet<String> weeks = new HashSet<>();
        String lastWeek = null;

        for (ActualPeriod period : periodsInfo.getPeriods()) {
            lastPeriod = timeLord.getWeekOrPeriodDate(period.getStartTime()) + "-" + timeLord.getWeekOrPeriodDate(period.getEndTime());
            periods.add(lastPeriod);
            setPeriodName(lastPeriod, period.getFullName());

            for (Week week : period.getWeeks()) {
                lastWeek = timeLord.getWeekOrPeriodDate(week.getStartTime()) + "-" + timeLord.getWeekOrPeriodDate(week.getEndTime());
                weeks.add(lastWeek);
            }

        }

        savePeriods(periods);
        saveWeeks(weeks);
        setWeeksCount(weeks.size());
        setPeriodsCount(periods.size());
        setCurrentPeriod(lastPeriod);
        setCurrentWeek(lastWeek);
        setCurrentScheduleWeek(lastWeek);
    }

    static void destroy() {
        instance = null;
    }
}
