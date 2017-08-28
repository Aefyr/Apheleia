package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class ConnectionHelper {
    private static ConnectionHelper instance;
    private ConnectivityManager connectivityManager;

    private ConnectionHelper(Context c) {
        instance = this;
        connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static ConnectionHelper getInstance(Context c) {
        return instance == null ? new ConnectionHelper(c) : instance;
    }

    public boolean hasNetworkConnection() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean connectedViaWifi(){
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo !=null && networkInfo.isConnected() && networkInfo.getType()==ConnectivityManager.TYPE_WIFI;
    }
}
