package com.aefyr.journalism.objects.minor;

import com.aefyr.journalism.exceptions.JournalismException;

import java.util.ArrayList;

public class MinorObjectsHelper {

    public static void addWeekToActualPeriod(ActualPeriod period, Week week) {
        period.addWeek(week);
    }

    public static void addHometasksToHomework(Homework homework, ArrayList<Hometask> hometasks) {
        homework.addTasks(hometasks);
    }

    public static void addAttachmentsToHomework(Homework homework, ArrayList<Attachment> attachments) {
        homework.addAttachments(attachments);
    }

    public static void addMarksToLesson(Lesson lesson, ArrayList<Mark> marks) {
        lesson.addMarks(marks);
    }

    public static void addHomeworkToLesson(Lesson lesson, Homework homework) {
        lesson.addHomework(homework);
    }

    public static void addTimesToLesson(Lesson lesson, String rawStart, String rawEnd) throws JournalismException {
        lesson.parseTimes(rawStart, rawEnd);
    }

    public static void addOvertimeLessonsToWeekDat(WeekDay weekDay, ArrayList<Lesson> overtimeLessons) {
        weekDay.addOvertimeLessons(overtimeLessons);
    }

    public static void addAttacmentsToMessageInfo(MessageInfo messageInfo, ArrayList<Attachment> attachments) {
        messageInfo.addAttachments(attachments);
    }

}
