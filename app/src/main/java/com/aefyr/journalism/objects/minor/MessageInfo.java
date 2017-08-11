package com.aefyr.journalism.objects.minor;


import com.aefyr.journalism.exceptions.EljurApiException;
import com.aefyr.journalism.objects.major.MessagesList;

import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MessageInfo {
	MessagesList.Folder folder;
	MessagePerson sender;
	ArrayList<MessagePerson> receivers;
	ArrayList<Attachment> attachments;
	
	String id;
	String subject;
	String text;
	
	long date;
	
	public MessagesList.Folder getFolder(){
		return folder;
	}
	
	public MessagePerson getSender(){
		return sender;
	}
	
	public ArrayList<MessagePerson> getReceivers(){
		return receivers;
	}
	
	public String getText(){
		return text;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public boolean hasAttachments(){
		return attachments!=null;
	}
	
	public ArrayList<Attachment> getAttachments(){
		return attachments;
	}
	
	public long getDate(){
		return date;
	}
	
	void addAttachments(ArrayList<Attachment> attachments){
		this.attachments = attachments;
	}
	
	void parseDate(String rawDate) throws EljurApiException {
		SimpleDateFormat messageDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			date = messageDateFormat.parse(rawDate).getTime();
		} catch (ParseException e) {
			throw new EljurApiException("Failed to parse ShortMessage date\n"+e.getMessage());
		}
	}
}
