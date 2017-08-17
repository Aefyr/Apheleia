package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class MessagePerson implements Serializable {
	String id;
	String firstName;
	String middleName;
	String lastName;
	
	public String getId(){
		return id;
	}
	
	public String getCompositeName(boolean f, boolean m, boolean l){
		String compositeName = "";
		if(f)
			compositeName+=firstName;
		if(m)
			compositeName+=" "+middleName;
		if(l)
			compositeName+=" "+lastName;
		return compositeName;
	}
}
