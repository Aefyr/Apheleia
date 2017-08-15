package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Aefyr on 15.08.2017.
 */

public abstract class SerializerHelperWithTimeAndStudentKeysBase<T> {

    private File savedObjectsPath;
    private ProfileHelper profileHelper;

    protected abstract String getFolderName();

    protected abstract String getExtension();

    protected SerializerHelperWithTimeAndStudentKeysBase(Context c){
        profileHelper = ProfileHelper.getInstance(c);

        savedObjectsPath = new File(c.getFilesDir()+"/"+ getFolderName());
        if(!savedObjectsPath.exists())
            savedObjectsPath.mkdirs();
    }


    protected boolean isObjectSaved(String timeKey){
        return new File(savedObjectsPath, timeKey+"_"+profileHelper.getCurrentStudentId()+getExtension()).exists();
    }


    protected boolean saveObject(T object, String timeKey){
        try{
            FileOutputStream fos = new FileOutputStream(savedObjectsPath +"/"+timeKey+"_"+profileHelper.getCurrentStudentId()+getExtension(), false);
            ObjectOutputStream stream = new ObjectOutputStream(fos);
            stream.writeObject(object);
            fos.close();
            stream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected T loadSavedObject(String timeKey) throws Exception {
        FileInputStream stream = new FileInputStream(savedObjectsPath +"/"+timeKey+"_"+profileHelper.getCurrentStudentId()+getExtension());
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return  (T) objectInputStream.readObject();
    }

    public interface ObjectSaveListener {
        void onSaveCompleted(boolean successful);
    }

    protected void saveObjectAsync(T object, String timeKey, ObjectSaveListener listener){
        ObjectSaveTask objectSaveTask = new ObjectSaveTask();
        objectSaveTask.execute(new ObjectSaveTaskParams(listener, object, timeKey));
    }

    protected class ObjectSaveTaskParams {
        T entry;
        String timeKey;
        ObjectSaveListener listener;

        protected ObjectSaveTaskParams(ObjectSaveListener listener, T entry, String timeKey){
            this.listener = listener;
            this.entry = entry;
            this.timeKey = timeKey;
        }
    }

    protected class ObjectSaveTask extends AsyncTask<ObjectSaveTaskParams, Void, Boolean> {
        private ObjectSaveListener listener;
        @Override
        protected Boolean doInBackground(ObjectSaveTaskParams... params) {
            listener = params[0].listener;
            return saveObject(params[0].entry, params[0].timeKey);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            listener.onSaveCompleted(success);
        }
    }

}
