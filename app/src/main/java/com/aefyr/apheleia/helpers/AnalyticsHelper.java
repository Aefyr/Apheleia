package com.aefyr.apheleia.helpers;

import android.os.Bundle;

import com.aefyr.apheleia.utility.FirebaseConstants;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by Aefyr on 22.08.2017.
 */

public class AnalyticsHelper {

    public static void logAppSectionViewEvent(FirebaseAnalytics mFirebaseAnalytics, String section) {
        Bundle params = new Bundle();
        params.putString(FirebaseConstants.APP_SECTION, section);
        mFirebaseAnalytics.logEvent(FirebaseConstants.VIEW_SECTION, params);
    }

    public static void logMessageViewEvent(FirebaseAnalytics mFirebaseAnalytics) {
        mFirebaseAnalytics.logEvent(FirebaseConstants.MESSAGE_VIEWED, null);
    }

    public static void logMessageSentEvent(FirebaseAnalytics mFirebaseAnalytics) {
        mFirebaseAnalytics.logEvent(FirebaseConstants.MESSAGE_SENT, null);
    }

}
