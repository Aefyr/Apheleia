package com.aefyr.journalism.objects.minor;

import java.util.ArrayList;

public class Homework {
	ArrayList<Hometask> hometasks;
	ArrayList<Attachment> attachments;
	
	void addTasks(ArrayList<Hometask> hometasks){
		this.hometasks = hometasks;
	}
	
	void addAttachments(ArrayList<Attachment> attachments){
		this.attachments = attachments;
	}
	
	public ArrayList<Hometask> getTasks(){
		return hometasks;
	}
	
	public boolean hasAttachments(){
		return attachments!=null;
	}
	
	public ArrayList<Attachment> getAttachments(){
		return attachments;
	}
}
