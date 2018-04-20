package com.aefyr.apheleia.viewmodels.messages;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.MessagesHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MessagesList;

/**
 * Created by Aefyr on 20.04.2018.
 */
public class MessagesLiveData extends LiveData<MessagesListState> {
    private EljurApiClient client;
    private Helper helper;
    private MessagesHelper messagesHelper;

    private boolean cachedLoadRequested = true;

    public MessagesLiveData(Context c){
        client = EljurApiClient.getInstance(c);
        helper = Helper.getInstance(c);
        messagesHelper = MessagesHelper.getInstance(c);
        setValue(new MessagesListState());
    }

    void loadMessages(){
        MessagesListState currentState = getValue();
        currentState.setUpdating();
        setValue(currentState);

        if(cachedLoadRequested){
            messagesHelper.loadMessages(getValue().folder() == MessagesList.Folder.INBOX, new MessagesHelper.LoadMessagesTaskResultListener() {
                @Override
                public void onSuccess(MessagesList list) {
                    cachedLoadRequested = false;

                    MessagesListState currentState = getValue();
                    currentState.setDataFromCache(list);
                    setValue(currentState);
                }

                @Override
                public void onFail() {

                }
            });
        }

        client.getMessages(helper.getPersona(), getValue().folder(), false, new EljurApiClient.JournalismListener<MessagesList>() {
            @Override
            public void onSuccess(MessagesList result) {
                MessagesListState currentState = getValue();
                currentState.setData(result);
                setValue(currentState);

                messagesHelper.saveMessages(result, currentState.folder() == MessagesList.Folder.INBOX, new MessagesHelper.SaveMessagesTaskResultListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        Log.d("Messages", "Serialized messages");
                    }
                });
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                MessagesListState currentState = getValue();
                if(tokenIsWrong)
                    currentState.setTokenDead();
                else
                    currentState.setNetError();
                setValue(currentState);
            }

            @Override
            public void onApiError(JournalismException e) {
                MessagesListState currentState = getValue();
                currentState.setApiError(e);
                setValue(currentState);
            }
        });
    }

    void setFolder(MessagesList.Folder folder){
        getValue().setFolder(folder);
        cachedLoadRequested = true;
        loadMessages();
    }

    void saveMessages(){
        messagesHelper.saveMessages(getValue().getData(), getValue().folder() == MessagesList.Folder.INBOX, null);
    }

    @Override
    protected void onActive() {
        if(getValue().getState()==MessagesListState.NOT_READY)
            loadMessages();
    }
}
