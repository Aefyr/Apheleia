package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class Mark implements Serializable{
	String value;
	String comment;
	
	public String getValue(){
		return value;
	}
	
	public boolean hasComment(){
		return comment !=null;
	}
	
	public String getComment(){
		return comment;
	}
}
