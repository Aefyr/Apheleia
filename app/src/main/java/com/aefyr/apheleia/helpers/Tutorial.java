package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import com.aefyr.apheleia.R;

/**
 * Created by Aefyr on 29.08.2017.
 */

public class Tutorial {
    public static void showTutorial(Context c){
        AlertDialog tutorialDialog = new AlertDialog.Builder(c).setTitle(c.getString(R.string.tut)).setMessage(c.getString(R.string.tut_short)).setPositiveButton(c.getString(R.string.got_it), null).create();
        tutorialDialog.setCanceledOnTouchOutside(false);
        tutorialDialog.show();
    }
}
