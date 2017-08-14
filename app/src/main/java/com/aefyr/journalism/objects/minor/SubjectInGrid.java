package com.aefyr.journalism.objects.minor;

import java.io.Serializable;
import java.util.ArrayList;

public class SubjectInGrid implements Serializable{
	String name;
	String averageMark;
	ArrayList<GridMark> marks;
	
	public String getName(){
		return name;
	}
	
	public String getAverageMark(){
		return averageMark;
	}
	
	public boolean hasMarks(){
		return marks!=null;
	}
	
	public ArrayList<GridMark> getMarks(){
		return marks;
	}
}
