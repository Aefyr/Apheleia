package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.JournalismException;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class WeekDay implements Serializable {
    boolean vacation;
    String name;
    long date;
    ArrayList<Lesson> lessons;
    ArrayList<Lesson> overtimeLessons;

    void parseDate(String rawDate) throws JournalismException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        try {
            date = simpleDateFormat.parse(rawDate).getTime();
        } catch (ParseException e) {
            throw new JournalismException("Failed to parse day date\n" + e.getMessage());
        }
    }

    public boolean isVacation() {
        return vacation;
    }

    public String getCanonicalName() {
        return name;
    }

    public long getDate() {
        return date;
    }

    public ArrayList<Lesson> getLessons() {
        return lessons;
    }

    public ArrayList<Lesson> getOvertimeLessons() {
        return overtimeLessons;
    }

    void addOvertimeLessons(ArrayList<Lesson> overtimeLessons) {
        this.overtimeLessons = overtimeLessons;
    }

    public boolean hasOvertimeLessons() {
        return overtimeLessons != null;
    }


}
