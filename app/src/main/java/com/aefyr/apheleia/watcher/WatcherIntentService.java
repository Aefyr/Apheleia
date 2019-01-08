package com.aefyr.apheleia.watcher;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.MessageViewActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

/**
 * Created by Aefyr on 26.08.2017.
 */

public class WatcherIntentService extends IntentService {
    public WatcherIntentService(String name) {
        super(name);
    }


    public WatcherIntentService() {
        super("WatcherIntentService");
    }

    SharedPreferences prefs;

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        System.out.println("Checking for new messages >-<");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!ConnectionHelper.getInstance(this).hasNetworkConnection() || (!ConnectionHelper.getInstance(this).connectedViaWifi()) && !prefs.getBoolean("allow_watcher_with_cell", false)) {
            die();
            return;
        }
        checkMessages();
    }

    private void checkMessages() {
        EljurApiClient.getInstance(this).getMessages(Helper.getInstance(this).getPersona(), MessagesList.Folder.INBOX, true, new EljurApiClient.JournalismListener<MessagesList>() {
            @Override
            public void onSuccess(MessagesList result) {
                checkMessages(result);
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                //who cares
            }

            @Override
            public void onApiError(JournalismException e) {
                Crashlytics.logException(e);
            }
        });
    }

    private void checkMessages(MessagesList messages) {
        //Should we use watcher's own shared prefs here?
        if (messages.getCount() == 0)
            die();


        long lastMessageTimeWas = prefs.getLong("last_message_time", 0);
        ArrayList<ShortMessage> newMessages = new ArrayList<>();
        for (ShortMessage m : messages.getMessages()) {
            if (m.getDate() > lastMessageTimeWas)
                newMessages.add(m);
            else
                break;
        }

        if (newMessages.size() == 0)
            die();


        prefs.edit().putLong("last_message_time", newMessages.get(0).getDate()).apply();
        if (newMessages.size() == 1)
            notifyAboutNewMessage(newMessages.get(0));
        else
            notifyAboutMultipleNewMessages(newMessages.size());

        prefs.edit().putLong("last_watcher_update_time", System.currentTimeMillis()).apply();
        die();
    }

    private void notifyAboutMultipleNewMessages(int count) {
        Intent messagesFragmentIntent = new Intent(this, MainActivity.class);
        messagesFragmentIntent.putExtra("requested_fragment", "messages");
        PendingIntent messageFragmentPendingIntent = PendingIntent.getActivity(this, 0, messagesFragmentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        postNotification(createNotification(getString(R.string.app_name), String.format(getString(R.string.multiple_new_messages), count)).setContentIntent(messageFragmentPendingIntent));
    }

    private void notifyAboutNewMessage(ShortMessage message) {
        Intent messageViewIntent = new Intent(this, MessageViewActivity.class);
        messageViewIntent.putExtra("messageId", message.getId());
        messageViewIntent.putExtra("inbox", true);
        PendingIntent messageViewPendingIntent = PendingIntent.getActivity(this, 0, messageViewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        postNotification(createNotification(message.getSender().getCompositeName(true, false, true), message.getSubject()).setContentIntent(messageViewPendingIntent));
    }

    public Notification.Builder createNotification(String title, String message) {
        Notification.Builder builder = new Notification.Builder(this).setContentTitle(title).setContentText(message).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.icon)).setSmallIcon(R.drawable.ic_email_white_24dp).setVibrate(new long[]{0, 500, 500, 500}).setLights(getResources().getColor(R.color.colorPrimary), 200, 3000).setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 21)
            builder.setColor(getResources().getColor(R.color.colorAccent));
        return builder;
    }

    private void postNotification(Notification.Builder notificationBuilder) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(0, notificationBuilder.build());
    }

    private void die() {
        stopSelf();
        System.exit(0);
    }
}
