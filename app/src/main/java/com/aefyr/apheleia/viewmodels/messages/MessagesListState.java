package com.aefyr.apheleia.viewmodels.messages;

import com.aefyr.apheleia.viewmodels.ApheleiaDataState;
import com.aefyr.journalism.objects.major.MessagesList;

/**
 * Created by Aefyr on 20.04.2018.
 */
public class MessagesListState extends ApheleiaDataState<MessagesList>{
    private MessagesList.Folder folder = MessagesList.Folder.INBOX;

    public void setFolder(MessagesList.Folder f){
        this.folder = f;
    }

    public MessagesList.Folder folder(){
        return folder;
    }
}
