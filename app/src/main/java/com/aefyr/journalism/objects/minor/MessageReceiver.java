package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class MessageReceiver extends MessagePerson implements Serializable {
	String info;
	
	public String getInfo(){
		return info;
	}
	
	public boolean hasInfo(){
		return info!=null;
	}
}
