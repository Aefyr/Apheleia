package com.aefyr.apheleia.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.Utility;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class Destroyer {
    private Context c;
    private OnDestructionListener listener;
    private ProgressDialog progressDialog;

    public Destroyer(Context c) {
        this.c = c;
        progressDialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(c.getString(R.string.logging_out));
    }

    public void destroy(OnDestructionListener listener) {
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

            Utility.deleteRecursive(c.getFilesDir());
            PreferenceManager.getDefaultSharedPreferences(c).edit().clear().commit();
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
