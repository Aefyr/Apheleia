package com.aefyr.apheleia.helpers;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class TimeLord {
    private static TimeLord instance;

    private SimpleDateFormat dayTitleSDF;
    private DateFormatSymbols russianDFS;

    private SimpleDateFormat lessonTimesSDF;

    private SimpleDateFormat weeksAndPeriodsSDF;

    private SimpleDateFormat gridMarkSDF;

    private SimpleDateFormat messageSDF;

    private SimpleDateFormat fullMessageSDF;

    private TimeLord(){
        instance = this;

        russianDFS = new DateFormatSymbols(){
            private final String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
            @Override
            public String[] getMonths() {
                return months;
            }
        };
        dayTitleSDF = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());

        lessonTimesSDF = new SimpleDateFormat("HH:mm", Locale.getDefault());

        weeksAndPeriodsSDF = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        gridMarkSDF = new SimpleDateFormat("dd.MM", Locale.getDefault());

        messageSDF = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());

        fullMessageSDF = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }

    public static TimeLord getInstance(){
       return instance==null?new TimeLord():instance;
    }

    public String getDayTitle(long date){
        return dayTitleSDF.format(date);
    }

    public String getLessonTime(long time){
        return  lessonTimesSDF.format(time);
    }

    public String getWeekOrPeriodDate(long date){
        return  weeksAndPeriodsSDF.format(date);
    }

    public String getGridMarkDate(long date){
        return  gridMarkSDF.format(date);
    }

    public String getMessageDate(long date){
        return messageSDF.format(date);
    }

    public String getFullMessageDate(long date){
        return  fullMessageSDF.format(date);
    }
}
