package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;

/**
 * Created by Aefyr on 16.08.2017.
 */

class AsyncParserParams<T> {
    String rawResponse;
    String journalismParam;
    EljurApiClient.JournalismListener<T> listener;

    AsyncParserParams(){

    }

    AsyncParserParams(String rawResponse, String journalismParam, EljurApiClient.JournalismListener<T> listener){
        this.rawResponse = rawResponse;
        this.journalismParam = journalismParam;
        this.listener = listener;
    }

}
