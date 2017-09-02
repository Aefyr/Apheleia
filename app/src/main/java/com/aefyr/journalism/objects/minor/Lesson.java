package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.JournalismException;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Lesson implements Serializable {
    String name;
    String room;
    String teacher;
    String num;
    long starts = 0;
    long ends;
    Homework homework;
    ArrayList<Mark> marks;

    void parseTimes(String rawStarts, String rawEnds) throws JournalismException {
        SimpleDateFormat lessonTimesSDF = new SimpleDateFormat("HH:mm:ss");
        try {
            starts = lessonTimesSDF.parse(rawStarts).getTime();
            ends = lessonTimesSDF.parse(rawEnds).getTime();
        } catch (ParseException e) {
            throw new JournalismException("Unable to parse lesson times\n" + e.getMessage());
        }

    }

    public boolean hasTimes() {
        return starts != 0;
    }

    public long getStartTime() {
        return starts;
    }

    public long getEndTime() {
        return ends;
    }

    public String getNumber() {
        return num;
    }

    public String getName() {
        return name;
    }

    public String getTeacherName() {
        return teacher;
    }

    public String getRoom() {
        return room;
    }

    void addHomework(Homework homework) {
        this.homework = homework;
    }

    public boolean hasHomework() {
        return homework != null;
    }

    public Homework getHomework() {
        return homework;
    }

    void addMarks(ArrayList<Mark> marks) {
        this.marks = marks;
    }

    public boolean hasMarks() {
        return marks != null;
    }

    public ArrayList<Mark> getMarks() {
        return marks;
    }

}
