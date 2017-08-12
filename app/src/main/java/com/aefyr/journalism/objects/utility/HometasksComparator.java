package com.aefyr.journalism.objects.utility;

import com.aefyr.journalism.objects.minor.Hometask;

import java.util.Comparator;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class HometasksComparator implements Comparator<Hometask> {
    @Override
    public int compare(Hometask hometask1, Hometask hometask2) {
        return hometask1.getTask().compareTo(hometask2.getTask());
    }
}
