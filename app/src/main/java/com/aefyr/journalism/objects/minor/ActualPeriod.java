package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.EljurApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ActualPeriod {
    String name;
    String fullName;
    ArrayList<Week> weeks;
    long start;
    long end;

    public ActualPeriod() {
        weeks = new ArrayList<Week>();
    }

    void parseDate(String rawEnd, String rawStart) throws EljurApiException {
        SimpleDateFormat periodDateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            start = periodDateFormat.parse(rawStart).getTime();
            end = periodDateFormat.parse(rawEnd).getTime();
        } catch (ParseException e) {
            throw new EljurApiException("Failed to parse period dates\n" + e.getMessage());
        }
    }

    void addWeek(Week week) {
        weeks.add(week);
    }

    public ArrayList<Week> getWeeks() {
        return weeks;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() {
        return end;
    }
}
