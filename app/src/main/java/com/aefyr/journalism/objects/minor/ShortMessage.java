package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.EljurApiException;
import com.aefyr.journalism.objects.major.MessagesList;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ShortMessage implements Serializable {
	MessagesList.Folder folder;
	
	String id;
	String subject;
	String previewText;
	
	MessagePerson sender;
	ArrayList<MessagePerson> receivers;
	
	boolean unread;
	boolean hasFiles;
	boolean hasResources;
	
	long date;
	
	void parseDate(String rawDate) throws EljurApiException {
		SimpleDateFormat messageDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			date = messageDateFormat.parse(rawDate).getTime();
		} catch (ParseException e) {
			throw new EljurApiException("Failed to parse ShortMessage date\n"+e.getMessage());
		}
	}
	
	void setText(String text){
		previewText = text.replaceAll("<br />", " ").replaceAll("\n", "");
	}
	
	public String getId(){
		return id;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public String getText(){
		return previewText;
	}
	
	public long getDate(){
		return date;
	}
	
	public MessagePerson getSender(){
		return sender;
	}
	
	public ArrayList<MessagePerson> getReceivers(){
		return receivers;
	}
	
	public boolean isUnread(){
		return unread;
	}

	public void read(){
		unread = false;
	}
	
	public boolean hasFiles(){
		return hasFiles;
	}
	
	public boolean hasResources(){
		return hasResources;
	}
	
	public MessagesList.Folder getFolder(){
		return folder;
	}
	
	
}
