package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.WeekDay;

import java.io.Serializable;
import java.util.ArrayList;

public class DiaryEntry implements Serializable {
    ArrayList<WeekDay> weekDays;

    public ArrayList<WeekDay> getDays() {
        return weekDays;
    }
}
