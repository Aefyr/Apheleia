package com.aefyr.journalism.objects.minor;

import java.util.ArrayList;

public class FinalSubject {
	String name;
	ArrayList<FinalPeriod> periods;
	
	public String getName(){
		return name;
	}
	
	public boolean hasPeriods(){
		return periods!=null;
	}
	
	public ArrayList<FinalPeriod> getPeriods(){
		return periods;
	}
}
