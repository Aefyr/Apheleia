package com.aefyr.apheleia.helpers;

import android.content.Context;

import com.aefyr.journalism.objects.major.MarksGrid;

/**
 * Created by Aefyr on 14.08.2017.
 */

public class MarksHelper extends SerializerHelperWithTimeAndStudentKeysBase<MarksGrid> {
    private static MarksHelper instance;


    private MarksHelper(Context c){
        super(c);
        instance = this;
    }

    @Override
    protected String getFolderName() {
        return "marks";
    }

    @Override
    protected String getExtension() {
        return ".amg";
    }


    public static MarksHelper getInstance(Context c){
        return instance==null?new MarksHelper(c): instance;
    }

    public boolean isGridSaved(String weeks){
        return isObjectSaved(weeks);
    }


    public boolean saveGrid(MarksGrid grid, String weeks){
        return saveObject(grid, weeks);
    }

    public MarksGrid loadSavedGrid(String weeks) throws Exception {
        return loadSavedObject(weeks);
    }

    public void saveGridAsync(MarksGrid grid, String weeks, ObjectSaveListener listener){
        saveObjectAsync(grid, weeks, listener);
    }

}
