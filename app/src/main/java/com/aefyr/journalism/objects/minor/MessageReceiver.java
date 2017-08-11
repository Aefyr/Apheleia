package com.aefyr.journalism.objects.minor;

public class MessageReceiver extends MessagePerson{
	String info;
	
	public String getInfo(){
		return info;
	}
	
	public boolean hasInfo(){
		return info!=null;
	}
}
