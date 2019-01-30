package com.aefyr.journalism.parsing;

import android.util.Log;

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
import com.crashlytics.android.Crashlytics;
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
        new DiaryParseTask(rawResponse, studentId, listener).execute();
    }

    private class DiaryParseTask extends AsyncParserBase<DiaryEntry> {
        private String rawResponse;
        private String studentId;

        DiaryParseTask(String rawResponse, String studentId, EljurApiClient.JournalismListener<DiaryEntry> listener) {
            bindJournalismListener(listener);
            this.rawResponse = rawResponse;
            this.studentId = studentId;
        }

        @Override
        protected AsyncParserTaskResult<DiaryEntry> doInBackground(Void... voids) {

            try {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null || response.get("students").isJsonNull())
                    return new AsyncParserTaskResult<>(MajorObjectsFactory.createDiaryEntry(new ArrayList<WeekDay>(0)));


                JsonObject jStudent;

                if (response.get("students").isJsonArray())
                    jStudent = response.getAsJsonArray("students").get(0).getAsJsonObject();
                else
                    jStudent = response.getAsJsonObject("students").getAsJsonObject(studentId);

                if (jStudent.get("days") == null)
                    return new AsyncParserTaskResult<>(MajorObjectsFactory.createDiaryEntry(new ArrayList<WeekDay>(0)));


                JsonObject weekDaysObj = jStudent.getAsJsonObject("days");

                ArrayList<WeekDay> weekDays = new ArrayList<>(weekDaysObj.size());

                for (Map.Entry<String, JsonElement> entry : weekDaysObj.entrySet()) {
                    JsonObject weekDay = entry.getValue().getAsJsonObject();
                    if (weekDay.get("alert") != null && weekDay.get("alert").getAsString().equals("vacation")) {
                        try {
                            weekDays.add(MinorObjectsFactory.createVacationWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString()));
                        } catch (JournalismException e) {
                            Log.w("Apheleia", e);
                            Crashlytics.log("Unable to parse " + rawResponse);
                            Crashlytics.logException(e);
                            return new AsyncParserTaskResult<>(e);
                        }
                        continue;
                    }

                    ArrayList<Lesson> lessons;
                    LESSONS:
                    {
                        if (weekDay.get("items") == null) {
                            lessons = new ArrayList<>(0);
                            break LESSONS;
                        }

                        JsonArray jLessons;
                        if (weekDay.get("items").isJsonArray()) {
                            jLessons = weekDay.get("items").getAsJsonArray();
                        } else {
                            jLessons = new JsonArray();
                            JsonObject jLessonsObj = weekDay.get("items").getAsJsonObject();

                            for (Map.Entry<String, JsonElement> jLessonKey : jLessonsObj.entrySet()) {
                                jLessons.add(jLessonKey.getValue());
                            }
                        }


                        lessons = new ArrayList<>(jLessons.size());

                        for (JsonElement jLessonEl : jLessons) {
                            JsonObject lessonObj = jLessonEl.getAsJsonObject();
                            Lesson lesson = MinorObjectsFactory.createLesson(Utility.getStringFromJsonSafe(lessonObj, "num", "0"), Utility.getStringFromJsonSafe(lessonObj, "name", "Неизвестно"), Utility.getStringFromJsonSafe(lessonObj, "room", "Неизвестно"), Utility.getStringFromJsonSafe(lessonObj, "teacher", "Неизвестно"));

                            if (lessonObj.get("starttime") != null && lessonObj.get("endtime") != null) {
                                try {
                                    MinorObjectsHelper.addTimesToLesson(lesson, lessonObj.get("starttime").getAsString(), lessonObj.get("endtime").getAsString());
                                } catch (JournalismException e) {
                                    Log.w("Apheleia", e);
                                    Crashlytics.log("Unable to parse " + rawResponse);
                                    Crashlytics.logException(e);
                                }
                            }

                            Homework homework = null;
                            if (lessonObj.has("homework")) {
                                JsonObject jHomework = lessonObj.getAsJsonObject("homework");
                                if(jHomework.size() > 0){
                                    homework = MinorObjectsFactory.createHomework();
                                    ArrayList<Hometask> hometasks = new ArrayList<>(jHomework.size());

                                    for (Map.Entry<String, JsonElement> jHomeworkKey : jHomework.entrySet()) {
                                        JsonObject hometask = jHomeworkKey.getValue().getAsJsonObject();
                                        hometasks.add(MinorObjectsFactory.createHometask(hometask.get("value").getAsString(), hometask.get("individual").getAsBoolean()));
                                    }

                                    Collections.sort(hometasks, new HometasksComparator());
                                    MinorObjectsHelper.addHometasksToHomework(homework, hometasks);
                                }
                            }

                            if (lessonObj.has("files")) {
                                JsonArray jAttachments = lessonObj.getAsJsonArray("files");
                                if(jAttachments.size() > 0){
                                    if (homework == null)
                                        homework = MinorObjectsFactory.createHomework();

                                    ArrayList<Attachment> attachments = new ArrayList<>(jAttachments.size());

                                    for (JsonElement attachmentEl : jAttachments) {
                                        JsonObject attachment = attachmentEl.getAsJsonObject();
                                        attachments.add(MinorObjectsFactory.createAttacment(Utility.getStringFromJsonSafe(attachment, "filename", "Без имени"), Utility.getStringFromJsonSafe(attachment, "link", "https://eljur.ru/404")));
                                    }
                                    MinorObjectsHelper.addAttachmentsToHomework(homework, attachments);;
                                }
                            }

                            if (homework != null) {
                                MinorObjectsHelper.addHomeworkToLesson(lesson, homework);
                            }

                            if (lessonObj.get("assessments") != null) {

                                JsonArray jMarks = lessonObj.getAsJsonArray("assessments");
                                ArrayList<Mark> marks = new ArrayList<>(jMarks.size());

                                for (JsonElement markEl : jMarks) {
                                    JsonObject jMark = markEl.getAsJsonObject();

                                    //Phantom marks filter, why do they even appear tho?
                                    if (jMark.get("value").getAsString().equals(""))
                                        continue;

                                    marks.add(new Mark(jMark));
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
                            Lesson otLesson = MinorObjectsFactory.createLesson("OT", Utility.getStringFromJsonSafe(otLessonObj, "name", "Неизвестно"), "OT", Utility.getStringFromJsonSafe(otLessonObj, "teacher", "Неизвестно"));

                            if (otLessonObj.get("starttime") != null && otLessonObj.get("endtime") != null) {
                                try {
                                    MinorObjectsHelper.addTimesToLesson(otLesson, otLessonObj.get("starttime").getAsString(), otLessonObj.get("endtime").getAsString());
                                } catch (JournalismException e) {
                                    Log.w("Apheleia", e);
                                    Crashlytics.log("Unable to parse " + rawResponse);
                                    Crashlytics.logException(e);
                                }
                            }

                            Homework homework = null;
                            if (otLessonObj.has("homework")) {
                                JsonArray jHomework = otLessonObj.getAsJsonArray("homework");
                                if(jHomework.size() > 0){
                                    homework = MinorObjectsFactory.createHomework();
                                    ArrayList<Hometask> hometasks = new ArrayList<>(jHomework.size());

                                    for (JsonElement homeworkEl : jHomework) {
                                        JsonObject hometask = homeworkEl.getAsJsonObject();
                                        hometasks.add(MinorObjectsFactory.createHometask(hometask.get("value").getAsString(), hometask.get("individual").getAsBoolean()));
                                    }

                                    Collections.sort(hometasks, new HometasksComparator());
                                    MinorObjectsHelper.addHometasksToHomework(homework, hometasks);
                                }
                            }

                            if (otLessonObj.has("files")) {
                                JsonArray jAttachments = otLessonObj.getAsJsonArray("files");
                                if(jAttachments.size() > 0){
                                    if (homework == null)
                                        homework = MinorObjectsFactory.createHomework();

                                    ArrayList<Attachment> attachments = new ArrayList<>(jAttachments.size());

                                    for (JsonElement attachmentEl : jAttachments) {
                                        JsonObject attachment = attachmentEl.getAsJsonObject();
                                        attachments.add(MinorObjectsFactory.createAttacment(Utility.getStringFromJsonSafe(attachment, "filename", "Без имени"), Utility.getStringFromJsonSafe(attachment, "link", "https://eljur.ru/404")));
                                    }
                                    MinorObjectsHelper.addAttachmentsToHomework(homework, attachments);;
                                }
                            }

                            if (homework != null)
                                MinorObjectsHelper.addHomeworkToLesson(otLesson, homework);

                            if (otLessonObj.get("assessments") != null) {

                                JsonArray jMarks = otLessonObj.getAsJsonArray("assessments");
                                ArrayList<Mark> marks = new ArrayList<>(jMarks.size());

                                for (JsonElement markEl : jMarks) {
                                    JsonObject jMark = markEl.getAsJsonObject();

                                    //Phantom marks filter, why do they even appear tho?
                                    if (jMark.get("value").getAsString().equals(""))
                                        continue;

                                    marks.add(new Mark(jMark));
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
                        Log.w("Apheleia", e);
                        Crashlytics.log("Unable to parse " + rawResponse);
                        Crashlytics.logException(e);
                    }


                }
                Collections.sort(weekDays, new WeekDaysComparator());
                return new AsyncParserTaskResult<>(MajorObjectsFactory.createDiaryEntry(weekDays));
            } catch (Exception e) {
                Log.w("Apheleia", e);
                Crashlytics.log("Unable to parse " + rawResponse);
                Crashlytics.logException(e);
                return new AsyncParserTaskResult<>(new JournalismException(e));
            }
        }
    }

}
