package com.aefyr.apheleia.viewmodels.messages;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.aefyr.journalism.objects.major.MessagesList;

/**
 * Created by Aefyr on 20.04.2018.
 */
public class MessagesViewModel extends AndroidViewModel{
    private MessagesLiveData messagesLiveData;

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        messagesLiveData = new MessagesLiveData(application.getApplicationContext());
    }

    public MessagesLiveData getMessagesLiveData() {
        return messagesLiveData;
    }

    public void updateMessages(){
        messagesLiveData.loadMessages();
    }

    public void setFolder(MessagesList.Folder folder){
        messagesLiveData.setFolder(folder);
    }

    public void saveMessages(){
        messagesLiveData.saveMessages();
    }
}
