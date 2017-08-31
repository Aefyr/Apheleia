package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.os.Bundle;

import com.aefyr.apheleia.utility.FirebaseConstants;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by Aefyr on 31.08.2017.
 */

public class AnalyticsHelper {
    public static void viewSection(String section, FirebaseAnalytics analytics){
        Bundle b = new Bundle();
        b.putString(FirebaseConstants.APP_SECTION, section);
        analytics.logEvent(FirebaseConstants.VIEW_SECTION, b);
    }

    public static void viewedMessage(FirebaseAnalytics analytics){
        analytics.logEvent(FirebaseConstants.MESSAGE_VIEWED, null);
    }

    public static void sentMessage(FirebaseAnalytics analytics){
        analytics.logEvent(FirebaseConstants.MESSAGE_SENT, null);
    }

    public static void caughtParseError(Context c){
        FirebaseAnalytics.getInstance(c).logEvent(FirebaseConstants.CAUGHT_API_EXCEPTION, null);
    }
}
