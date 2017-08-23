package com.aefyr.journalism.objects.minor;

import java.io.Serializable;
import java.util.ArrayList;

public class Homework implements Serializable {
    ArrayList<Hometask> hometasks;
    ArrayList<Attachment> attachments;

    void addTasks(ArrayList<Hometask> hometasks) {
        this.hometasks = hometasks;
    }

    void addAttachments(ArrayList<Attachment> attachments) {
        this.attachments = attachments;
    }

    public boolean hasTasks() {
        return hometasks != null;
    }

    public ArrayList<Hometask> getTasks() {
        return hometasks;
    }

    public boolean hasAttachments() {
        return attachments != null;
    }

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }
}
