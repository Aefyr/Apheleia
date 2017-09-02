package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.JournalismException;

import java.text.ParseException;
import java.text.SimpleDateFormat;


public class Week {
    long start;
    long end;
    String canonicalName;


    void parseDate(String rawEnd, String rawStart) throws JournalismException {
        SimpleDateFormat weekDateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            start = weekDateFormat.parse(rawStart).getTime();
            end = weekDateFormat.parse(rawEnd).getTime();
        } catch (ParseException e) {
            throw new JournalismException("Failed to parse week dates\n" + e.getMessage());
        }
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() {
        return end;
    }

    public String getCanonicalName() {
        return canonicalName;
    }
}
