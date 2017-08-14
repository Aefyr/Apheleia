package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.os.AsyncTask;

import com.aefyr.journalism.objects.major.MarksGrid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Aefyr on 14.08.2017.
 */

public class MarksHelper {
    private static MarksHelper instance;
    private File savedEntriesPath;
    private ProfileHelper profileHelper;
    public static final String MARKS_GRID_EXTENSION = ".amg";

    private MarksHelper(Context c){
        instance = this;
        profileHelper = ProfileHelper.getInstance(c);

        savedEntriesPath = new File(c.getFilesDir()+"/marks");
        if(!savedEntriesPath.exists())
            savedEntriesPath.mkdirs();
    }

    public static MarksHelper getInstance(Context c){
        return instance==null?new MarksHelper(c):instance;
    }

    public boolean isGridSaved(String weeks){
        return new File(savedEntriesPath, weeks+"_"+profileHelper.getCurrentStudentId()+ MARKS_GRID_EXTENSION).exists();
    }


    public boolean saveGrid(MarksGrid grid, String weeks){
        try{
            FileOutputStream fos = new FileOutputStream(savedEntriesPath+"/"+weeks+"_"+profileHelper.getCurrentStudentId()+ MARKS_GRID_EXTENSION, false);
            ObjectOutputStream stream = new ObjectOutputStream(fos);
            stream.writeObject(grid);
            fos.close();
            stream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public MarksGrid loadSavedGrid(String weeks) throws Exception {
        FileInputStream stream = new FileInputStream(savedEntriesPath+"/"+weeks+"_"+profileHelper.getCurrentStudentId()+ MARKS_GRID_EXTENSION);
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return  (MarksGrid) objectInputStream.readObject();
    }

    public interface GridSaveListener {
        void onSaveCompleted(boolean successful);
    }

    public void saveGridAsync(MarksGrid grid, String weeks, GridSaveListener listener){
        GridSaveTask gridSaveTask = new GridSaveTask();
        gridSaveTask.execute(new GridSaveTaskParams(listener, grid, weeks));
    }

    private class GridSaveTaskParams {
        MarksGrid grid;
        String days;
        GridSaveListener listener;

        private GridSaveTaskParams(GridSaveListener listener, MarksGrid grid, String days){
            this.listener = listener;
            this.grid = grid;
            this.days = days;
        }
    }

    private class GridSaveTask extends AsyncTask<GridSaveTaskParams, Void, Boolean>{
        private GridSaveListener listener;
        @Override
        protected Boolean doInBackground(GridSaveTaskParams... params) {
            listener = params[0].listener;
            return saveGrid(params[0].grid, params[0].days);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            listener.onSaveCompleted(success);
        }
    }

}
