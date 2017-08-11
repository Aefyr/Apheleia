package com.aefyr.apheleia.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Aefyr on 10.08.2017.
 */

public class ApheleiaInstanceIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        System.out.println("Token refresh!!");
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
