package com.aefyr.journalism.exceptions;

public class JournalismException extends Exception {
    public JournalismException() {

    }

    public JournalismException(String message) {
        super(message);
    }

    public JournalismException(Exception e) {
        super(e);
    }
}
