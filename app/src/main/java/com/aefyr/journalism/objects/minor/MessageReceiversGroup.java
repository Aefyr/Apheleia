package com.aefyr.journalism.objects.minor;

import java.util.ArrayList;

public class MessageReceiversGroup {
	String name;
	String key;
	ArrayList<MessageReceiver> people;
	
	public String getName(){
		return name;
	}
	
	public ArrayList<MessageReceiver> getPeople(){
		return people;
	}
	
	public String getKey(){
		return key;
	}
	
	void addPeople(ArrayList<MessageReceiver> people){
		this.people = people;
	}
}
