package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.FinalSubject;
import com.aefyr.journalism.objects.minor.MessageReceiversGroup;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.aefyr.journalism.objects.minor.SubjectInGrid;
import com.aefyr.journalism.objects.minor.WeekDay;

import java.util.ArrayList;


public class MajorObjectsFactory {

	public static DiaryEntry createDiaryEntry(ArrayList<WeekDay> weekDays){
		DiaryEntry diaryEntry = new DiaryEntry();
		
		diaryEntry.weekDays = weekDays;
		
		return diaryEntry;
	}
	
	public static MarksGrid createMarksGrid(ArrayList<SubjectInGrid> subjects){
		MarksGrid marksGrid = new MarksGrid();
		
		marksGrid.subjects = subjects;
		
		return marksGrid;
	}

	public static PeriodsInfo createPeriodsInfo(){
		return new PeriodsInfo();
	}

	public static Schedule createSchedule(ArrayList<WeekDay> days){
		Schedule schedule = new Schedule();
		
		schedule.days = days;
		
		return schedule;
	}
	
	

	public static MessagesList createMessagesList(int total, int count, ArrayList<ShortMessage> messages, MessagesList.Folder folder){
		MessagesList list = new MessagesList();

		list.folder = folder;
		list.total = total;
		list.count = count;
		list.messages = messages;
		
		return list;
	}
	
	public static MessageReceiversInfo createMessageReceiversInfo(ArrayList<MessageReceiversGroup> groups){
		MessageReceiversInfo info = new MessageReceiversInfo();
		
		info.groups = groups;
		
		return info;
	}
	
	public static SentMessageResponse createSentMessageResponse(boolean wasSent){
		SentMessageResponse response = new SentMessageResponse();
		
		response.wasSent = wasSent;
		
		return response;
	}
	
	public static Finals createFinals(ArrayList<FinalSubject> subjects){
		Finals finals = new Finals();
		
		finals.subjects = subjects;
		
		return finals;
	}
}
