package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MajorObjectsFactory;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.minor.GridMark;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.SubjectInGrid;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

/**
 * Created by Aefyr on 16.08.2017.
 */

public class MarkGridAsyncParser {
    private static MarkGridAsyncParser instance;

    private MarkGridAsyncParser() {
        instance = this;
    }

    public static MarkGridAsyncParser getInstance() {
        return instance == null ? new MarkGridAsyncParser() : instance;
    }

    public void parseGrid(String rawResponse, String studentId, EljurApiClient.JournalismListener<MarksGrid> listener) {
        new MarksParseTask(rawResponse, studentId, listener).execute();
    }

    private class MarksParseTask extends AsyncParserBase<MarksGrid> {
        private String rawResponse;
        private String studentId;

        MarksParseTask(String rawResponse, String studentId, EljurApiClient.JournalismListener<MarksGrid> listener){
            bindJournalismListener(listener);
            this.rawResponse = rawResponse;
            this.studentId = studentId;
        }

        @Override
        protected AsyncParserTaskResult<MarksGrid> doInBackground(Void... voids) {
            JsonObject response = Utility.getJsonFromResponse(rawResponse);

            if (response == null || response.size() == 0 || response.get("students") == null) {
                return new AsyncParserTaskResult<>(MajorObjectsFactory.createMarksGrid(new ArrayList<SubjectInGrid>(0)));
            }

            JsonArray lessons = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("lessons");

            ArrayList<SubjectInGrid> subjects = new ArrayList<>(lessons.size());

            for (JsonElement lessonEl : lessons) {
                JsonObject lesson = lessonEl.getAsJsonObject();

                ArrayList<GridMark> marks;

                if (lesson.get("marks") != null) {

                    JsonArray jMarks = lesson.getAsJsonArray("marks");

                    if(jMarks.size()==0)
                        continue;

                    marks = new ArrayList<>(jMarks.size());

                    for (JsonElement markEl : jMarks) {
                        JsonObject mark = markEl.getAsJsonObject();

                        //Phantom marks filter, why do they even appear tho?
                        if (mark.get("value").getAsString().equals(""))
                            continue;

                        GridMark gridMark;

                        String markWeight = null;
                        if(mark.get("weight")!=null)
                            markWeight = mark.get("weight").getAsString();

                        try {
                            if (!mark.get("lesson_comment").isJsonNull()) {
                                gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), markWeight, mark.get("date").getAsString(), mark.get("lesson_comment").getAsString());
                            } else if (mark.get("comment").getAsString().length() > 0) {
                                gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), markWeight, mark.get("date").getAsString(), mark.get("comment").getAsString());
                            } else {
                                gridMark = MinorObjectsFactory.createGridMark(mark.get("value").getAsString(), markWeight, mark.get("date").getAsString());
                            }
                        } catch (JournalismException e) {
                            return new AsyncParserTaskResult<>(e);
                        }

                        marks.add(gridMark);
                    }
                    subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), marks));
                } else {
                    subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), null));
                }

            }
            return new AsyncParserTaskResult<>(MajorObjectsFactory.createMarksGrid(subjects));
        }
    }
}
