package com.aefyr.journalism.objects.utility;

import com.aefyr.journalism.objects.minor.WeekDay;

import java.util.Comparator;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class WeekDaysComparator implements Comparator<WeekDay> {

    @Override
    public int compare(WeekDay day1, WeekDay day2) {
        if(day1.getDate()>day2.getDate())
            return 1;
        if(day1.getDate()<day2.getDate())
            return -1;
        return 0;
    }
}
