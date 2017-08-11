package com.aefyr.apheleia.gcm;

/**
 * Created by Aefyr on 10.08.2017.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"topics"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("581165343215",
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);


            Log.i(TAG, "GCM Registration Token: " + token);

            sendRegistrationToServer(token);


            sharedPreferences.edit().putBoolean("token_is_fine", true).apply();
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean("token_is_fine", false).apply();
        }
    }

    private void sendRegistrationToServer(final String token) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://1399.eljur.ru/apiv3/setpushtoken?token="+token+"&type=google&activate=1&auth_token=db5c882198114a36aa740954de0f66a35fb8a1d8119a659b57968___85&vendor=1399&devkey=6ec0e964a29c22fe5542f748b5143c4e&out_format=json";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response+"\nDid we do it?");
                        try {
                            subscribeTopics(token);
                            System.out.println("Subscribed to topics");
                        } catch (IOException e) {
                            System.out.println("Failed to subscribe!");
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                System.out.println("Fail :c");
            }
        });

        queue.add(stringRequest);

    }

    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

}
