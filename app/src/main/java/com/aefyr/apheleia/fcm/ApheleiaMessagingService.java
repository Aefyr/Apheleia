package com.aefyr.apheleia.fcm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aefyr.apheleia.utility.FirebaseConstants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Aefyr on 05.12.2017.
 */

public class ApheleiaMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        p.edit().putInt("mdb", (p.getInt("mdb", 0)+1)).apply();
        super.onMessageReceived(remoteMessage);

        //TODO add message action data param
        Intent messageIntent = new Intent(FirebaseConstants.INTENT_ACTION_GOT_FCM_MESSAGE);
        messageIntent.putExtra("message", remoteMessage.getData().get("message_dup"));
        sendBroadcast(messageIntent);
    }

}
