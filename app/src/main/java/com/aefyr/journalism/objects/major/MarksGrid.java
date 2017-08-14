package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.SubjectInGrid;

import java.io.Serializable;
import java.util.ArrayList;

public class MarksGrid implements Serializable{
	ArrayList<SubjectInGrid> subjects;
	
	public ArrayList<SubjectInGrid> getLessons(){
		return subjects;
	}
}
