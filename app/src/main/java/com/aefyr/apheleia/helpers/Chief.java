package com.aefyr.apheleia.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    public static void makeReportApiErrorDialog(final Context c, final String containsWhat, final String message, final String json, boolean addAreYouSurePrompt){
        new AlertDialog.Builder(c).setTitle(c.getString(R.string.api_error)).setMessage(String.format(c.getString(R.string.api_error_report_prompt), containsWhat)).setPositiveButton(c.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Send message and json to firebase
                makeAnAlert(c, c.getString(R.string.api_error_reported));
                System.out.println("Reported");
            }
        }).setNegativeButton(c.getString(R.string.no), addAreYouSurePrompt?new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                makeAreYouSurePrompt(c, message, json);
            }
        }:null).create().show();
    }

    private static void makeAreYouSurePrompt(final Context c, final String message, final String json){
        new AlertDialog.Builder(c).setTitle(c.getString(R.string.api_error)).setMessage(c.getString(R.string.api_error_prompt_declined)).setPositiveButton(c.getString(R.string.report), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                makeAnAlert(c, c.getString(R.string.api_error_reported));
                System.out.println("Reported");
            }
        }).setNegativeButton(c.getString(R.string.dont_report), null).create().show();
    }
}
