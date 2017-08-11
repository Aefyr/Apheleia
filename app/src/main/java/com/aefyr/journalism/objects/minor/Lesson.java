package com.aefyr.journalism.objects.minor;

import java.util.ArrayList;

public class Lesson {
	String name;
	String room;
	String teacher;
	String num;
	Homework homework;
	ArrayList<Mark> marks;
	
	public String getNumber(){
		return num;
	}
	
	public String getName(){
		return name;
	}
	
	public String getTeacherName(){
		return teacher;
	}
	
	public String getRoom(){
		return room;
	}
	
	void addHomework(Homework homework){
		this.homework = homework;
	}
	
	public boolean hasHomework(){
		return homework != null;
	}
	
	public Homework getHomework(){
		return homework;
	}
	
	void addMarks(ArrayList<Mark> marks){
		this.marks = marks;
	}
	
	public boolean hasMarks(){
		return marks!=null;
	}
	
	public ArrayList<Mark> getMarks(){
		return marks;
	}
	
}
