package com.aefyr.journalism.parsing;

/**
 * Created by Aefyr on 16.08.2017.
 */

class AsyncParserTaskResult<T> {
    String errorMessage;
    String rawResponse;
    T journalismMajorObject;
    boolean failed;

    AsyncParserTaskResult(String errorMessage, String rawResponse) {
        this.errorMessage = errorMessage;
        this.rawResponse = rawResponse;
        failed = true;
    }

    AsyncParserTaskResult(T journalismMajorObject) {
        this.journalismMajorObject = journalismMajorObject;
        failed = false;
    }
}
