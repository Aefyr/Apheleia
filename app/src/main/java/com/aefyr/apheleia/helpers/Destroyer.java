package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.apheleia.watcher.WatcherHelper;

import java.io.File;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class Destroyer {
    private Context c;
    private OnDestructionListener listener;
    private ProgressDialog progressDialog;
    private boolean keepCache;

    public Destroyer(Context c) {
        this.c = c;
        progressDialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(c.getString(R.string.logging_out));
    }

    public void destroy(boolean keepCache, OnDestructionListener listener) {
        this.keepCache = keepCache;
        this.listener = listener;
        progressDialog.show();
        new DestructionTask().execute();
    }

    public interface OnDestructionListener {
        void onDestroyed();
    }

    private class DestructionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            DiaryHelper.destroy();
            FinalsHelper.destroy();
            MarksHelper.destroy();
            MessagesHelper.destroy();
            PeriodsHelper.destroy();
            ProfileHelper.destroy();
            ScheduleHelper.destroy();
            Helper.destroy();
            WatcherHelper.setWatcherEnabled(c, false);

            PreferenceManager.getDefaultSharedPreferences(c).edit().clear().commit();

            if (keepCache)
                return null;

            Utility.deleteRecursive(new File(c.getFilesDir(), "diary"));
            Utility.deleteRecursive(new File(c.getFilesDir(), "marks"));
            Utility.deleteRecursive(new File(c.getFilesDir(), "messages"));
            Utility.deleteRecursive(new File(c.getFilesDir(), "schedule"));
            Utility.deleteRecursive(new File(c.getFilesDir(), "finals"));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if (listener != null)
                listener.onDestroyed();
        }
    }
}
