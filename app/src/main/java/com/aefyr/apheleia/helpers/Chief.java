package com.aefyr.apheleia.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.aefyr.apheleia.R;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class Chief {
    public static void makeASnack(View view, String message){
        final Snackbar snackbar = Snackbar.make(view, message, 3000);
        snackbar.setAction(view.getContext().getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    public static void makeAnAlert(Context c, String message){
        new AlertDialog.Builder(c).setMessage(message).setPositiveButton(c.getString(R.string.ok), null).create().show();
    }
}
