package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.FinalSubject;

import java.io.Serializable;
import java.util.ArrayList;


public class Finals implements Serializable {
    ArrayList<FinalSubject> subjects;

    public ArrayList<FinalSubject> getSubjects() {
        return subjects;
    }
}
