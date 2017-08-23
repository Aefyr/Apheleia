package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.ShortMessage;

import java.io.Serializable;
import java.util.ArrayList;


public class MessagesList implements Serializable {

    Folder folder;
    int total;
    int count;
    ArrayList<ShortMessage> messages;

    public enum Folder {
        SENT, INBOX
    }

    public Folder getFolder() {
        return folder;
    }

    public int getTotal() {
        return total;
    }

    public int getCount() {
        return count;
    }

    public ArrayList<ShortMessage> getMessages() {
        return messages;
    }
}
