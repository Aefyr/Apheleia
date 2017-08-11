package com.aefyr.apheleia.gcm;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Aefyr on 10.08.2017.
 */

public class CloudMessagesListener extends GcmListenerService {

    @Override
    public void onMessageReceived(String s, Bundle bundle) {
        System.out.println("Got some message");
        for(String key: bundle.keySet()){
            System.out.println("Something: "+bundle.get(key)+"with key: "+key);
        }
        super.onMessageReceived(s, bundle);
    }
}
