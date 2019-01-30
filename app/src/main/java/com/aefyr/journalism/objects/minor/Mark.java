package com.aefyr.journalism.objects.minor;

import com.google.gson.JsonObject;

import java.io.Serializable;

public class Mark implements Serializable {
    String value;
    String comment;
    String weight;
    String lessonComment;

    public Mark(JsonObject jsonMark){
         value = jsonMark.get("value").getAsString();

         if(jsonMark.has("weight"))
             weight = jsonMark.get("weight").getAsString();

         if(jsonMark.has("comment"))
             comment = jsonMark.get("comment").getAsString();

         if(jsonMark.has("lesson_comment"))
             lessonComment = jsonMark.get("lesson_comment").getAsString();
    }

    public String getValue() {
        return value;
    }

    public boolean hasLessonComment(){
        return lessonComment != null && lessonComment.length() > 0;
    }

    public String getLessonComment(){
        return lessonComment;
    }

    public boolean hasComment() {
        return comment != null && comment.length() > 0;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasWeight() {
        return weight != null;
    }

    public String getWeight() {
        return weight;
    }
}
