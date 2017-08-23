package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.os.AsyncTask;

import com.aefyr.journalism.objects.major.MessagesList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Aefyr on 17.08.2017.
 */

public class MessagesHelper {
    private static MessagesHelper instance;

    private static final String MESSAGES_PATH = "/messages";
    private static final String MESSAGES_EXTENSION = ".am";
    private File path;

    private MessagesHelper(Context c) {
        instance = this;
        path = new File(c.getFilesDir() + MESSAGES_PATH);
        if (!path.exists())
            path.mkdirs();
    }

    public static MessagesHelper getInstance(Context c) {
        return instance == null ? new MessagesHelper(c) : instance;
    }


    public LoadMessagesTask loadMessages(boolean inbox, LoadMessagesTaskResultListener listener) {
        LoadMessagesTask task = new LoadMessagesTask();
        task.execute(new LoadMessagesTaskParams(inbox, listener));
        return task;
    }

    private class LoadMessagesTaskParams {
        private boolean inbox;
        private LoadMessagesTaskResultListener listener;

        private LoadMessagesTaskParams(boolean inbox, LoadMessagesTaskResultListener listener) {
            this.inbox = inbox;
            this.listener = listener;
        }
    }

    public interface LoadMessagesTaskResultListener {
        void onSuccess(MessagesList list);

        void onFail();
    }

    private class LoadMessagesTask extends AsyncTask<LoadMessagesTaskParams, Void, MessagesList> {
        private LoadMessagesTaskResultListener listener;

        @Override
        protected MessagesList doInBackground(LoadMessagesTaskParams... loadMessagesTaskParamses) {
            this.listener = loadMessagesTaskParamses[0].listener;
            try {
                FileInputStream stream = new FileInputStream(path + "/" + (loadMessagesTaskParamses[0].inbox ? "inbox" : "sent") + MESSAGES_EXTENSION);
                ObjectInputStream objectInputStream = new ObjectInputStream(stream);
                return (MessagesList) objectInputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(MessagesList list) {
            super.onPostExecute(list);
            if (listener == null)
                return;

            if (list != null)
                listener.onSuccess(list);
            else
                listener.onFail();

            listener = null;
        }
    }

    public SaveMessagesTask saveMessages(MessagesList list, boolean inbox, SaveMessagesTaskResultListener listener) {
        SaveMessagesTask task = new SaveMessagesTask();
        task.execute(new SaveMessagesTaskParams(list, inbox, listener));
        return task;
    }

    private class SaveMessagesTaskParams {
        private boolean inbox;
        private MessagesList messagesList;
        private SaveMessagesTaskResultListener listener;

        private SaveMessagesTaskParams(MessagesList list, boolean inbox, SaveMessagesTaskResultListener listener) {
            this.messagesList = list;
            this.inbox = inbox;
            this.listener = listener;
        }
    }

    public interface SaveMessagesTaskResultListener {
        void onSaveCompleted(boolean successful);
    }

    private class SaveMessagesTask extends AsyncTask<SaveMessagesTaskParams, Void, Boolean> {

        private SaveMessagesTaskResultListener listener;

        @Override
        protected Boolean doInBackground(SaveMessagesTaskParams... saveMessagesTaskParamses) {
            this.listener = saveMessagesTaskParamses[0].listener;
            try {
                FileOutputStream fos = new FileOutputStream(path + "/" + (saveMessagesTaskParamses[0].inbox ? "inbox" : "sent") + MESSAGES_EXTENSION, false);
                ObjectOutputStream stream = new ObjectOutputStream(fos);
                stream.writeObject(saveMessagesTaskParamses[0].messagesList);
                fos.close();
                stream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (listener == null)
                return;

            listener.onSaveCompleted(aBoolean);
        }
    }

    static void destroy() {
        instance = null;
    }
}
