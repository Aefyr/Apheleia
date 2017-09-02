package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.JournalismException;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class GridMark implements Serializable {

    String value;
    String comment;
    long date;


    void parseDate(String rawDate) throws JournalismException {
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = sdFormat.parse(rawDate).getTime();
        } catch (ParseException e) {
            throw new JournalismException("Failed to parse mark date\n" + e.getMessage());
        }
    }

    public String getValue() {
        return value;
    }

    public boolean hasComment() {
        return comment != null;
    }

    public String getComment() {
        return comment;
    }

    public long getDate() {
        return date;
    }


}
