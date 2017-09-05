package com.aefyr.journalism.parsing;

import android.os.AsyncTask;

import com.aefyr.journalism.EljurApiClient;

/**
 * Created by Aefyr on 16.08.2017.
 */

abstract class AsyncParserBase<T> extends AsyncTask<AsyncParserParams<T>, Void, AsyncParserTaskResult<T>> {

    private EljurApiClient.JournalismListener<T> listener;

    protected void bindJournalismListener(EljurApiClient.JournalismListener<T> listener) {
        this.listener = listener;
    }

    @Override
    protected void onPostExecute(AsyncParserTaskResult asyncParserTaskResult) {
        if (asyncParserTaskResult.failed)
            listener.onApiError(asyncParserTaskResult.error);
        else
            listener.onSuccess((T) asyncParserTaskResult.journalismMajorObject);
    }
}
