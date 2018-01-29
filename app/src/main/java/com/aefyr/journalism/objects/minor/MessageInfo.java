package com.aefyr.journalism.objects.minor;


import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MessagesList;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MessageInfo implements Serializable {
    MessagesList.Folder folder;
    MessagePerson sender;
    int receiversCount;
    ArrayList<MessagePerson> parsedReceivers;
    ArrayList<Attachment> attachments;

    String id;
    String subject;
    String text;

    long date;

    public MessagesList.Folder getFolder() {
        return folder;
    }

    public MessagePerson getSender() {
        return sender;
    }

    public ArrayList<MessagePerson> getParsedReceivers() {
        return parsedReceivers;
    }

    public int receiversCount(){
        return receiversCount;
    }

    public String getText() {
        return text;
    }

    public String getSubject() {
        return subject;
    }

    public boolean hasAttachments() {
        return attachments != null;
    }

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }

    public long getDate() {
        return date;
    }

    void addAttachments(ArrayList<Attachment> attachments) {
        this.attachments = attachments;
    }

    void parseDate(String rawDate) throws JournalismException {
        SimpleDateFormat messageDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = messageDateFormat.parse(rawDate).getTime();
        } catch (ParseException e) {
            throw new JournalismException("Failed to parse ShortMessage date\n" + e.getMessage());
        }
    }
}
