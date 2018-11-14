package com.aefyr.journalism.objects.minor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

public class SubjectInGrid implements Serializable {
    String name;
    String averageMark;
    ArrayList<GridMark> marks;

    public String getName() {
        return name;
    }

    public String getAverageMark() {
        return averageMark;
    }

    public String getCalculatedAverageMark(){
        try {
            float a = 0;
            float marksCount = 0;
            for(GridMark mark: marks){
                float markValue;
                try {
                    markValue = Float.parseFloat(mark.getValue().replaceAll("(-|\\+)", ""));
                }catch (Exception e){
                    continue;
                }

                if(mark.hasWeight())
                    markValue *= Float.parseFloat(mark.getWeight());

                a += markValue;
                marksCount++;
            }

            return String.format(Locale.getDefault(),"%.2f", a/marksCount);
        }catch (Exception e){
            return getAverageMark();
        }
    }

    public boolean hasMarks() {
        return marks != null;
    }

    public ArrayList<GridMark> getMarks() {
        return marks;
    }
}
