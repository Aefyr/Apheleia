package com.aefyr.apheleia.helpers;

import android.content.Context;

import com.aefyr.journalism.objects.major.DiaryEntry;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class DiaryHelper extends SerializerHelperWithTimeAndStudentKeysBase<DiaryEntry> {
    private static DiaryHelper instance;

    private DiaryHelper(Context c){
        super(c);
        instance = this;
    }

    @Override
    protected String getFolderName() {
        return "diary";
    }

    @Override
    protected String getExtension() {
        return ".ade";
    }

    public static DiaryHelper getInstance(Context c){
        return instance==null?new DiaryHelper(c):instance;
    }

    public boolean saveEntry(DiaryEntry entry, String weeks){
        return saveObject(entry, weeks);
    }

    public boolean isEntrySaved(String weeks){
        return isObjectSaved(weeks);
    }

    public DiaryEntry loadSavedEntry(String weeks) throws Exception {
            return loadSavedObject(weeks);
    }

    public void saveEntryAsync(DiaryEntry grid, String weeks, ObjectSaveListener listener){
        saveObjectAsync(grid, weeks, listener);
    }

}
