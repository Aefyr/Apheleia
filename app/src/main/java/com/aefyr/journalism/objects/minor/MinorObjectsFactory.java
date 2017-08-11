package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.EljurApiException;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.major.PersonaInfo;

import java.util.ArrayList;


public class MinorObjectsFactory {
	
	public static ActualPeriod createActualPeriod(String name, String fullName, String rawStart, String rawEnd) throws EljurApiException {
		ActualPeriod period = new ActualPeriod();
		
		period.parseDate(rawEnd, rawStart);
		period.name = name;
		period.fullName = fullName;
		
		return period;
	}
	
	public static AmbigiousPeriod createAmbigiousPeriod(String name, String fullName){
		AmbigiousPeriod period = new AmbigiousPeriod();
		
		period.name = name;
		period.fullName = fullName;
		
		return period;
	}
	
	public static Attachment createAttacment(String name, String uri){
		Attachment attachment = new Attachment();
		
		attachment.name = name;
		attachment.uri = uri;
		
		return attachment;
	}

	public static GridMark createGridMark(String value, String rawDate) throws EljurApiException{
		GridMark mark = new GridMark();
		
		mark.value = value;
		mark.parseDate(rawDate);
		
		return mark;
	}
	
	public static GridMark createGridMarkWithComment(String value, String rawDate, String comment) throws EljurApiException{
		GridMark mark = new GridMark();
		
		mark.value = value;
		mark.comment = comment;
		mark.parseDate(rawDate);
		
		return mark;
	}

	public static Hometask createHometask(String task, boolean personal){
		Hometask hometask = new Hometask();
		
		hometask.task = task;
		hometask.personal = personal;
		
		return hometask;
	}
	
	public static Homework createHomework(){
		return new Homework();
	}

	public static Lesson createLesson(String num, String name, String room, String teacher){
		Lesson lesson = new Lesson();
		
		lesson.num = num;
		lesson.name = name;
		lesson.room = room;
		lesson.teacher = teacher;
		
		return lesson;
	}
	
	public static Mark createMark(String value){
		Mark mark = new Mark();
		
		mark.value = value;
		
		return mark;
	}
	
	public static Mark createMarkWithComment(String value, String comment){
		Mark mark = new Mark();
		
		mark.value = value;
		mark.comment = comment;
		
		return mark;
	}

	public static Student createStudent(String name, String id, PersonaInfo.Gender gender, String className){
		Student student = new Student();
		
		student.name = name;
		student.id = id;
		student.gender = gender;
		student.className = className;
		
		return student;
	}

	public static SubjectInGrid createSubjectInGrid(String name, String averageMark, ArrayList<GridMark> marks){
		SubjectInGrid subjectInGrid = new SubjectInGrid();
		
		subjectInGrid.name = name;
		subjectInGrid.averageMark = averageMark;
		subjectInGrid.marks = marks;
		
		return subjectInGrid;
	}

	public static WeekDay createVacationWeekDay(String name, String rawDate) throws EljurApiException{
		WeekDay weekDay = new WeekDay();
	
		weekDay.parseDate(rawDate);
		weekDay.vacation = true;
		weekDay.name = name;
		
		return weekDay;
	}
	
	public static WeekDay createWeekDay(String name, String rawDate, ArrayList<Lesson> lessons) throws EljurApiException{
		WeekDay weekDay = new WeekDay();
		
		weekDay.parseDate(rawDate);
		weekDay.name = name;
		weekDay.lessons = lessons;
		
		return weekDay;
	}
	
 	public static Week createWeek(String rawStart, String rawEnd, String name) throws EljurApiException{
		Week week = new Week();
		
		week.parseDate(rawEnd, rawStart);
		week.canonicalName = name;
		return week;
	}
	
	public static ShortMessage createInboxShortMessage(String id, String subject, String previewText, String rawDate, MessagePerson sender, boolean unread, boolean hasFiles, boolean hasResources) throws EljurApiException{
		ShortMessage message = new ShortMessage();
		
		message.parseDate(rawDate);
		message.id = id;
		message.subject = subject;
		message.setText(previewText);
		message.sender = sender;
		message.unread = unread;
		message.hasFiles = hasFiles;
		message.hasResources = hasResources;
		message.folder = MessagesList.Folder.INBOX;
		
		return message;
	}
	
	public static ShortMessage createSentShortMessage(String id, String subject, String previewText, String rawDate, ArrayList<MessagePerson> receivers, boolean unread, boolean hasFiles, boolean hasResources) throws EljurApiException{
		ShortMessage message = new ShortMessage();
		
		message.parseDate(rawDate);
		message.id = id;
		message.subject = subject;
		message.setText(previewText);
		message.receivers = receivers;
		message.unread = unread;
		message.hasFiles = hasFiles;
		message.hasResources = hasResources;
		message.folder = MessagesList.Folder.SENT;
		
		return message;
	}
	
	public static MessagePerson createMessagePerson(String id, String firstName, String middleName, String lastName){
		MessagePerson sender = new MessagePerson();
		
		sender.id = id;
		sender.firstName = firstName;
		sender.middleName = middleName;
		sender.lastName = lastName;
		
		return sender;
	}
	
	public static MessageInfo createMessageInfo(String id, String subject, String text, String rawDate, MessagesList.Folder folder, MessagePerson sender, ArrayList<MessagePerson> receivers) throws EljurApiException{
		MessageInfo message = new MessageInfo();
		
		message.parseDate(rawDate);
		message.id = id;
		message.subject = subject;
		message.text = text;
		message.sender = sender;
		message.receivers = receivers;
		message.folder = folder;
		
		return message;
	}
	
	public static MessageReceiver createMessageReceiver(String id, String firstName, String middleName, String lastName, String info){
		MessageReceiver receiver = new MessageReceiver();
		
		receiver.id = id;
		receiver.firstName = firstName;
		receiver.middleName = middleName;
		receiver.lastName = lastName;
		receiver.info = info;
		
		return receiver;
	}
	
	public static MessageReceiversGroup createMessageReceiversGroup(String key, String name, ArrayList<MessageReceiver> receivers){
		MessageReceiversGroup group = new MessageReceiversGroup();
		
		group.key = key;
		group.name = name;
		group.addPeople(receivers);
		
		return group;
	}
	
	public static FinalPeriod createFinalPeriod(String name, String mark, String comment){
		FinalPeriod period = new FinalPeriod();
		
		period.name = name;
		period.value = mark;
		period.comment = comment;
		
		return period;
	}
	
	public static FinalSubject createFinalSubject(String name, ArrayList<FinalPeriod> periods){
		FinalSubject subject = new FinalSubject();
		
		subject.name = name;
		subject.periods = periods;
		
		return subject;
	}

	
}

