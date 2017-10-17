package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.MajorObjectsFactory;
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.Hometask;
import com.aefyr.journalism.objects.minor.Homework;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.Mark;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.MinorObjectsHelper;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.aefyr.journalism.objects.utility.HometasksComparator;
import com.aefyr.journalism.objects.utility.WeekDaysComparator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Aefyr on 16.08.2017.
 */

public class DiaryAsyncParser {
    private static DiaryAsyncParser instance;

    private DiaryAsyncParser() {
        instance = this;
    }

    public static DiaryAsyncParser getInstance() {
        return instance == null ? new DiaryAsyncParser() : instance;
    }

    public void parseDiary(String rawResponse, String studentId, EljurApiClient.JournalismListener<DiaryEntry> listener) {
        new DiaryParseTask().execute(new AsyncParserParams<>(rawResponse, studentId, listener));
    }

    private class DiaryParseTask extends AsyncParserBase<DiaryEntry> {

        @Override
        protected AsyncParserTaskResult<DiaryEntry> doInBackground(AsyncParserParams<DiaryEntry>... asyncParserParams) {
            bindJournalismListener(asyncParserParams[0].listener);
            String rawResponse = asyncParserParams[0].rawResponse;
            String studentId = asyncParserParams[0].journalismParam;

            JsonObject response = Utility.getJsonFromResponse(rawResponse);

            if (response == null || response.size() == 0 || response.get("students") == null) {
                return new AsyncParserTaskResult<>(MajorObjectsFactory.createDiaryEntry(new ArrayList<WeekDay>(0)));
            }

            JsonObject weekDaysObj = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonObject("days");

            ArrayList<WeekDay> weekDays = new ArrayList<>(weekDaysObj.size());

            for (Map.Entry<String, JsonElement> entry : weekDaysObj.entrySet()) {
                JsonObject weekDay = entry.getValue().getAsJsonObject();
                if (weekDay.get("alert") != null && weekDay.get("alert").getAsString().equals("vacation")) {
                    try {
                        weekDays.add(MinorObjectsFactory.createVacationWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString()));
                    } catch (JournalismException e) {
                        return new AsyncParserTaskResult<>(e);
                    }
                    continue;
                }

                ArrayList<Lesson> lessons;
                LESSONS:
                {
                    if(weekDay.get("items")==null){
                        lessons = new ArrayList<>(0);
                        break LESSONS;
                    }

                    JsonObject jLessons = weekDay.get("items").getAsJsonObject();
                    lessons = new ArrayList<>(jLessons.size());

                    for (Map.Entry<String, JsonElement> jLessonKey : jLessons.entrySet()) {
                        JsonObject lessonObj = jLessonKey.getValue().getAsJsonObject();
                        Lesson lesson = MinorObjectsFactory.createLesson(lessonObj.get("num").getAsString(), lessonObj.get("name").getAsString(), lessonObj.get("room").getAsString(), lessonObj.get("teacher").getAsString());

                        if (lessonObj.get("starttime") != null && lessonObj.get("endtime") != null) {
                            try {
                                MinorObjectsHelper.addTimesToLesson(lesson, lessonObj.get("starttime").getAsString(), lessonObj.get("endtime").getAsString());
                            } catch (JournalismException e) {
                                return new AsyncParserTaskResult<>(e);
                            }
                        }

                        Homework homework = null;
                        if (lessonObj.get("homework") != null) {
                            homework = MinorObjectsFactory.createHomework();

                            JsonObject jHomework = lessonObj.getAsJsonObject("homework");
                            ArrayList<Hometask> hometasks = new ArrayList<>(jHomework.size());

                            for (Map.Entry<String, JsonElement> jHomeworkKey : jHomework.entrySet()) {
                                JsonObject hometask = jHomeworkKey.getValue().getAsJsonObject();
                                hometasks.add(MinorObjectsFactory.createHometask(hometask.get("value").getAsString(), hometask.get("individual").getAsBoolean()));
                            }

                            Collections.sort(hometasks, new HometasksComparator());
                            MinorObjectsHelper.addHometasksToHomework(homework, hometasks);
                        }

                        if (lessonObj.get("files") != null) {
                            if (homework == null)
                                homework = MinorObjectsFactory.createHomework();

                            JsonArray jAttachments = lessonObj.getAsJsonArray("files");
                            ArrayList<Attachment> attachments = new ArrayList<>(jAttachments.size());

                            for (JsonElement attachmentEl : jAttachments) {
                                JsonObject attachment = attachmentEl.getAsJsonObject();
                                attachments.add(MinorObjectsFactory.createAttacment(attachment.get("filename").getAsString(), attachment.get("link").getAsString()));
                            }
                            MinorObjectsHelper.addAttachmentsToHomework(homework, attachments);
                            ;
                        }

                        if (homework != null) {
                            MinorObjectsHelper.addHomeworkToLesson(lesson, homework);
                        }

                        if (lessonObj.get("assessments") != null) {

                            JsonArray jMarks = lessonObj.getAsJsonArray("assessments");
                            ArrayList<Mark> marks = new ArrayList<>(jMarks.size());

                            for (JsonElement markEl : jMarks) {
                                JsonObject mark = markEl.getAsJsonObject();

                                //Phantom marks filter, why do they even appear tho?
                                if (mark.get("value").getAsString().equals(""))
                                    continue;

                                if (mark.get("comment") != null && mark.get("comment").getAsString().length() > 0)
                                    marks.add(MinorObjectsFactory.createMarkWithComment(mark.get("value").getAsString(), mark.get("comment").getAsString()));
                                else if (mark.get("lesson_comment") != null && mark.get("lesson_comment").getAsString().length() > 0)
                                    marks.add(MinorObjectsFactory.createMarkWithComment(mark.get("value").getAsString(), mark.get("lesson_comment").getAsString()));
                                else
                                    marks.add(MinorObjectsFactory.createMark(mark.get("value").getAsString()));
                            }
                            MinorObjectsHelper.addMarksToLesson(lesson, marks);
                        }
                        lessons.add(lesson);
                    }
                }

                ArrayList<Lesson> overtimeLessons = null;
                if (weekDay.get("items_extday") != null) {

                    JsonArray jOvertimeLessons = weekDay.getAsJsonArray("items_extday");
                    overtimeLessons = new ArrayList<>(jOvertimeLessons.size());

                    for (JsonElement otLessonEl : jOvertimeLessons) {
                        JsonObject otLessonObj = otLessonEl.getAsJsonObject();
                        Lesson otLesson = MinorObjectsFactory.createLesson("OT", otLessonObj.get("name").getAsString(), "OT", otLessonObj.get("teacher").getAsString());

                        if (otLessonObj.get("starttime") != null && otLessonObj.get("endtime") != null) {
                            try {
                                MinorObjectsHelper.addTimesToLesson(otLesson, otLessonObj.get("starttime").getAsString(), otLessonObj.get("endtime").getAsString());
                            } catch (JournalismException e) {
                                return new AsyncParserTaskResult<>(e);
                            }
                        }

                        Homework homework = null;
                        if (otLessonObj.get("homework") != null) {
                            homework = MinorObjectsFactory.createHomework();

                            JsonArray jHomework = otLessonObj.getAsJsonArray("homework");
                            ArrayList<Hometask> hometasks = new ArrayList<>(jHomework.size());

                            for (JsonElement homeworkEl : jHomework) {
                                JsonObject hometask = homeworkEl.getAsJsonObject();
                                hometasks.add(MinorObjectsFactory.createHometask(hometask.get("value").getAsString(), hometask.get("individual").getAsBoolean()));
                            }

                            Collections.sort(hometasks, new HometasksComparator());
                            MinorObjectsHelper.addHometasksToHomework(homework, hometasks);
                        }

                        if (otLessonObj.get("files") != null) {
                            if (homework == null)
                                homework = MinorObjectsFactory.createHomework();

                            JsonArray jAttachments = otLessonObj.getAsJsonArray("files");
                            ArrayList<Attachment> attachments = new ArrayList<>(jAttachments.size());

                            for (JsonElement attachmentEl : jAttachments) {
                                JsonObject attachment = attachmentEl.getAsJsonObject();
                                attachments.add(MinorObjectsFactory.createAttacment(attachment.get("filename").getAsString(), attachment.get("link").getAsString()));
                            }
                            MinorObjectsHelper.addAttachmentsToHomework(homework, attachments);
                        }

                        if (homework != null)
                            MinorObjectsHelper.addHomeworkToLesson(otLesson, homework);

                        if (otLessonObj.get("assessments") != null) {

                            JsonArray jMarks = otLessonObj.getAsJsonArray("assessments");
                            ArrayList<Mark> marks = new ArrayList<>(jMarks.size());

                            for (JsonElement markEl : jMarks) {
                                JsonObject mark = markEl.getAsJsonObject();

                                //Phantom marks filter, why do they even appear tho?
                                if (mark.get("value").getAsString().equals(""))
                                    continue;

                                if (mark.get("comment") != null && mark.get("comment").getAsString().length() > 0)
                                    marks.add(MinorObjectsFactory.createMarkWithComment(mark.get("value").getAsString(), mark.get("comment").getAsString()));
                                else if (mark.get("lesson_comment") != null && mark.get("lesson_comment").getAsString().length() > 0)
                                    marks.add(MinorObjectsFactory.createMarkWithComment(mark.get("value").getAsString(), mark.get("lesson_comment").getAsString()));
                                else
                                    marks.add(MinorObjectsFactory.createMark(mark.get("value").getAsString()));
                            }
                            MinorObjectsHelper.addMarksToLesson(otLesson, marks);
                        }
                        overtimeLessons.add(otLesson);
                    }
                }
                try {
                    WeekDay day = MinorObjectsFactory.createWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString(), lessons);

                    if (overtimeLessons != null)
                        MinorObjectsHelper.addOvertimeLessonsToWeekDat(day, overtimeLessons);

                    weekDays.add(day);
                } catch (JournalismException e) {
                    return new AsyncParserTaskResult<>(e);
                }


            }
            Collections.sort(weekDays, new WeekDaysComparator());
            return new AsyncParserTaskResult<>(MajorObjectsFactory.createDiaryEntry(weekDays));
        }
    }

}
