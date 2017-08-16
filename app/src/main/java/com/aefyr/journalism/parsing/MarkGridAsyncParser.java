package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.EljurApiException;
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

    private MarkGridAsyncParser(){
        instance = this;
    }

    public static MarkGridAsyncParser getInstance(){
        return instance==null?new MarkGridAsyncParser():instance;
    }

    public void parseGrid(String rawResponse, String studentId, EljurApiClient.JournalismListener<MarksGrid> listener){
        new MarksParseTask().execute(new AsyncParserParams<>(rawResponse, studentId, listener));
    }

    private class MarksParseTask extends AsyncParserBase<MarksGrid>{

        @Override
        protected AsyncParserTaskResult<MarksGrid> doInBackground(AsyncParserParams<MarksGrid>... asyncParserParams) {
            bindJournalismListener(asyncParserParams[0].listener);
            String studentId = asyncParserParams[0].journalismParam;
            String rawResponse = asyncParserParams[0].rawResponse;

            JsonObject response = Utility.getJsonFromResponse(rawResponse);

            if(response.size()==0||response.get("students")==null){
                return new AsyncParserTaskResult<MarksGrid>(MajorObjectsFactory.createMarksGrid(new ArrayList<SubjectInGrid>(0)));
            }

            JsonArray lessons = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("lessons");

            if(lessons == null||lessons.size()==0){
                return new AsyncParserTaskResult<MarksGrid>(MajorObjectsFactory.createMarksGrid(new ArrayList<SubjectInGrid>(0)));
            }

            ArrayList<SubjectInGrid> subjects = new ArrayList<>();

            for(JsonElement lessonEl: lessons){
                JsonObject lesson = lessonEl.getAsJsonObject();

                ArrayList<GridMark> marks = new ArrayList<>();

                if(lesson.get("marks")!=null&&lesson.getAsJsonArray("marks").size()>0){
                    for(JsonElement markEl: lesson.getAsJsonArray("marks")){
                        JsonObject mark = markEl.getAsJsonObject();

                        //Phantom marks filter, why do they even appear tho?
                        if(mark.get("value").getAsString().equals(""))
                            continue;

                        GridMark gridMark;

                        try {
                            if (!mark.get("lesson_comment").isJsonNull()) {
                                gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), mark.get("date").getAsString(), mark.get("lesson_comment").getAsString());
                            } else if (mark.get("comment").getAsString().length() > 0) {
                                gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), mark.get("date").getAsString(), mark.get("comment").getAsString());
                            } else {
                                gridMark = MinorObjectsFactory.createGridMark(mark.get("value").getAsString(), mark.get("date").getAsString());
                            }
                        }catch (EljurApiException e){
                            return new AsyncParserTaskResult<MarksGrid>(e.getMessage(), rawResponse);
                        }

                        marks.add(gridMark);
                    }
                    subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), marks));
                }else {
                    subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), null));
                }

            }
            return new AsyncParserTaskResult<MarksGrid>(MajorObjectsFactory.createMarksGrid(subjects));
        }
    }
}
