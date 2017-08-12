package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class Hometask implements Serializable{
	String task;
	boolean personal;
	
	public String getTask(){
		return task;
	}
	
	public boolean isPersonal(){
		return personal;
	}
}
