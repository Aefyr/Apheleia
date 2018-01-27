package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.MessageReceiversGroup;

import java.io.Serializable;
import java.util.ArrayList;


public class MessageReceiversInfo implements Serializable {
    ArrayList<MessageReceiversGroup> groups;

    public ArrayList<MessageReceiversGroup> getGroups() {
        return groups;
    }

}
