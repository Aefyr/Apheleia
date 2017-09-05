package com.aefyr.apheleia.helpers;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class TimeLord {
    private static TimeLord instance;

    private SimpleDateFormat dayTitleSDF;

    private SimpleDateFormat lessonTimesSDF;

    private SimpleDateFormat weeksAndPeriodsSDF;

    private SimpleDateFormat gridMarkSDF;

    private SimpleDateFormat messageSDF;

    private SimpleDateFormat fullMessageSDF;

    private SimpleDateFormat quickPickerSDF;

    private TimeLord() {
        instance = this;

        dayTitleSDF = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());

        lessonTimesSDF = new SimpleDateFormat("HH:mm", Locale.getDefault());

        weeksAndPeriodsSDF = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        gridMarkSDF = new SimpleDateFormat("dd.MM", Locale.getDefault());

        messageSDF = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());

        fullMessageSDF = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        quickPickerSDF = new SimpleDateFormat("EE", Locale.getDefault());
    }

    public static TimeLord getInstance() {
        return instance == null ? new TimeLord() : instance;
    }

    public String getDayTitle(long date) {
        String d = dayTitleSDF.format(date);
        return d.replaceFirst(String.valueOf(d.charAt(0)), String.valueOf(d.charAt(0)).toUpperCase());
    }

    public String getLessonTime(long time) {
        return lessonTimesSDF.format(time);
    }

    public String getWeekOrPeriodDate(long date) {
        return weeksAndPeriodsSDF.format(date);
    }

    public String getGridMarkDate(long date) {
        return gridMarkSDF.format(date);
    }

    public String getMessageDate(long date) {
        return messageSDF.format(date);
    }

    public String getFullMessageDate(long date) {
        return fullMessageSDF.format(date);
    }

    public String getQuickPickerDate(long date) {
        return quickPickerSDF.format(date);
    }
}
