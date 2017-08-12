package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.EljurApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;


public class WeekDay {
	boolean vacation;
	String name;
	long date;
	ArrayList<Lesson> lessons;
	ArrayList<Lesson> overtimeLessons;
	
	void parseDate(String rawDate)throws EljurApiException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		
		try {
			date = simpleDateFormat.parse(rawDate).getTime();
		} catch (ParseException e) {
			throw new EljurApiException("Failed to parse day date\n"+e.getMessage());
		}
	}
	
	public boolean isVacation(){
		return vacation;
	}
	
	public String getCanonicalName(){
		return name;
	}
	
	public long getDate(){
		return date;
	}
	
	public ArrayList<Lesson> getLessons(){
		return lessons;
	}
	
	void addOvertimeLessons(ArrayList<Lesson> overtimeLessons){
		this.overtimeLessons = overtimeLessons;
	}
	
	public boolean hasOvertimeLessons(){
		return overtimeLessons != null;
	}


}
