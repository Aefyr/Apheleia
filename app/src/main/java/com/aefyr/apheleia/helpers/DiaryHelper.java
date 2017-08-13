package com.aefyr.apheleia.helpers;

import android.content.Context;

import com.aefyr.journalism.objects.major.DiaryEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class DiaryHelper {
    private static DiaryHelper instance;
    private File savedEntriesPath;
    private ProfileHelper profileHelper;
    public static final String DIARY_ENTRY_EXTENSION = ".ade";

    private DiaryHelper(Context c){
        instance = this;
        profileHelper = ProfileHelper.getInstance(c);

        savedEntriesPath = new File(c.getFilesDir()+"/diary");
        if(!savedEntriesPath.exists())
            savedEntriesPath.mkdirs();
    }

    public static DiaryHelper getInstance(Context c){
        return instance==null?new DiaryHelper(c):instance;
    }

    public boolean isEntrySaved(String weeks){
        return new File(savedEntriesPath, weeks+"_"+profileHelper.getCurrentStudentId()+DIARY_ENTRY_EXTENSION).exists();
    }


    public boolean saveEntry(DiaryEntry entry, String weeks){
        try{
            FileOutputStream fos = new FileOutputStream(savedEntriesPath+"/"+weeks+"_"+profileHelper.getCurrentStudentId()+DIARY_ENTRY_EXTENSION, false);
            ObjectOutputStream stream = new ObjectOutputStream(fos);
            stream.writeObject(entry);
            fos.close();
            stream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public DiaryEntry loadSavedEntry(String weeks) throws Exception {
            FileInputStream stream = new FileInputStream(savedEntriesPath+"/"+weeks+"_"+profileHelper.getCurrentStudentId()+DIARY_ENTRY_EXTENSION);
            ObjectInputStream objectInputStream = new ObjectInputStream(stream);
            return  (DiaryEntry) objectInputStream.readObject();
    }


}
