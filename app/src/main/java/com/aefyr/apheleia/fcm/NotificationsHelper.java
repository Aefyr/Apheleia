package com.aefyr.apheleia.fcm;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;

import com.aefyr.apheleia.R;

/**
 * Created by Aefyr on 13.12.2017.
 */

public class NotificationsHelper {

    public static void showDevAlert(Context c, String message) {
        final AlertDialog dialog = new AlertDialog.Builder(c).setTitle(R.string.system_notification).setMessage(message).setPositiveButton(R.string.sys_notification_delay, null).create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.got_it);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
        }, 5000);
    }
}
