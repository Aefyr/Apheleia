package com.aefyr.journalism.objects.minor;

public class Mark {
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
