package com.aefyr.apheleia.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.aefyr.apheleia.R;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class Chief {
    public static void makeASnack(View view, String message) {
        final Snackbar snackbar = Snackbar.make(view, message, 2500);
        snackbar.setAction(view.getContext().getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    public static void makeAnAlert(Context c, String message) {
        new AlertDialog.Builder(c).setMessage(message).setPositiveButton(c.getString(R.string.ok), null).create().show();
    }

    public static AlertDialog makeWarning(Context c, CharSequence warningMessage) {
        AlertDialog d = new AlertDialog.Builder(c).setTitle(c.getString(R.string.warning)).setMessage(warningMessage).setPositiveButton(c.getString(R.string.got_it), null).create();
        d.setCanceledOnTouchOutside(false);
        d.show();
        return d;
    }

    public static void makeAToast(Context c, String message) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
    }

    public static void makeApiErrorAlert(final Context c, boolean closeActivityOnDialogClosed) {
        AlertDialog.Builder b = new AlertDialog.Builder(c).setTitle(c.getString(R.string.error)).setMessage(c.getString(R.string.error_api)).setPositiveButton(c.getString(R.string.ok), null);
        if (closeActivityOnDialogClosed) {
            b.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ((Activity) c).finish();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    ((Activity) c).finish();
                }
            });
        }
        b.create().show();
    }
}
