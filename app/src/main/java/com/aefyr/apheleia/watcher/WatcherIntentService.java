package com.aefyr.apheleia.watcher;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.MessageViewActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.fragments.MessagesFragment;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.ShortMessage;

import java.util.ArrayList;

/**
 * Created by Aefyr on 26.08.2017.
 */

public class WatcherIntentService extends IntentService {
    public WatcherIntentService(String name) {
        super(name);
    }


    public WatcherIntentService(){
        super("WatcherIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        System.out.println("Checking for new messages >-<");
        if(!ConnectionHelper.getInstance(this).hasNetworkConnection()){
            System.out.println("Whoops, no connection, dying!");
            die();
        }
        checkMessages();
    }

    private void checkMessages(){
        EljurApiClient.getInstance(this).getMessages(Helper.getInstance(this).getPersona(), MessagesList.Folder.INBOX, true, new EljurApiClient.JournalismListener<MessagesList>() {
            @Override
            public void onSuccess(MessagesList result) {
                checkMessages(result);
                Chief.makeAToast(WatcherIntentService.this, "And context is still alive!");
            }

            @Override
            public void onNetworkError() {
                //who cares
            }

            @Override
            public void onApiError(String message, String json) {
                // who cares
            }
        });
    }

    private void checkMessages(MessagesList messages){
        //Should we use watcher's own shared prefs here?
        if(messages.getCount()==0) {
            System.out.println("No new messages");
            die();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long lastMessageTimeWas = prefs.getLong("last_message_time", 0);
        ArrayList<ShortMessage> newMessages = new ArrayList<>();
        for(ShortMessage m: messages.getMessages()){
            if(m.getDate()>lastMessageTimeWas)
                newMessages.add(m);
            else
                break;
        }

        if(newMessages.size()==0) {
            System.out.println("No new messages");
            die();
        }

        prefs.edit().putLong("last_message_time", newMessages.get(0).getDate()).apply();
        if(newMessages.size()==1)
            notifyAboutNewMessage(newMessages.get(0));
        else
            notifyAboutMultipleNewMessages(newMessages.size());

        die();
    }

    private void notifyAboutMultipleNewMessages(int count){
        Intent messagesFragmentIntent = new Intent(this, MainActivity.class);
        messagesFragmentIntent.putExtra("requested_fragment", "messages");
        PendingIntent messageFragmentPendingIntent = PendingIntent.getActivity(this, 0, messagesFragmentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        postNotification(createNotification(getString(R.string.app_name), String.format(getString(R.string.multiple_new_messages), count)).setContentIntent(messageFragmentPendingIntent));
    }

    private void notifyAboutNewMessage(ShortMessage message){
        Intent messageViewIntent = new Intent(this, MessageViewActivity.class);
        messageViewIntent.putExtra("messageId", message.getId());
        messageViewIntent.putExtra("inbox", true);
        PendingIntent messageViewPendingIntent = PendingIntent.getActivity(this, 0, messageViewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        postNotification(createNotification(message.getSender().getCompositeName(true, false, true), message.getSubject()).setContentIntent(messageViewPendingIntent));
    }

    private Notification.Builder createNotification(String title, String message){
        return new Notification.Builder(WatcherIntentService.this).setContentTitle(title).setContentText(message).setSmallIcon(android.R.drawable.sym_def_app_icon).setVibrate(new long[]{500, 500, 500}).setLights(getResources().getColor(R.color.colorPrimary), 200, 3000);
    }

    private void postNotification(Notification.Builder notificationBuilder){
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(0, notificationBuilder.build());
    }

    private void die(){
        stopSelf();
        System.exit(0);
    }
}
