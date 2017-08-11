package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.ShortMessage;

import java.util.ArrayList;


public class MessagesList {
	
	int total;
	int count;
	ArrayList<ShortMessage> messages;
	
	public enum Folder{
		SENT, INBOX
	}
	
	public int getTotal(){
		return total;
	}
	
	public int getCount(){
		return count;
	}
	
	public ArrayList<ShortMessage> getMessages(){
		return messages;
	}
}
