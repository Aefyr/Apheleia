package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.WeekDay;

import java.io.Serializable;
import java.util.ArrayList;


public class Schedule implements Serializable{
	ArrayList<WeekDay> days;
	
	public ArrayList<WeekDay> getDays(){
		return days;
	}
}
