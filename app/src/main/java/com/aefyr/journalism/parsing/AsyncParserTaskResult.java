package com.aefyr.journalism.parsing;

import com.aefyr.journalism.exceptions.JournalismException;

/**
 * Created by Aefyr on 16.08.2017.
 */

class AsyncParserTaskResult<T> {
    JournalismException error;
    T journalismMajorObject;
    boolean failed;

    AsyncParserTaskResult(JournalismException e) {
        error = e;
        failed = true;
    }

    AsyncParserTaskResult(T journalismMajorObject) {
        this.journalismMajorObject = journalismMajorObject;
        failed = false;
    }
}
