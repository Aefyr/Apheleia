package com.aefyr.apheleia.watcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.Chief;


/**
 * Created by Aefyr on 26.08.2017.
 */

public class WatcherHelper extends BroadcastReceiver{

    public static void setWatcherEnabled(Context c, boolean enabled){
        setWatcherOnBootBCReceiverEnabled(c, enabled);
        if(enabled)
            startWatcher(c);
        else
            killWatcher(c);
    }

    public static void showPrompt(final Context c){
        new AlertDialog.Builder(c).setTitle(c.getString(R.string.pref_enable_watcher)).setMessage(c.getString(R.string.watcher_desc)).setPositiveButton(c.getString(R.string.enable), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setWatcherEnabled(c, true);
                PreferenceManager.getDefaultSharedPreferences(c).edit().putBoolean("watcher_enabled", true).apply();
                showFirstCheckWarning(c);
            }
        }).setNegativeButton(c.getString(R.string.dont_enable), null).create().show();
    }

    public static void showFirstCheckWarning(Context c){
        Chief.makeWarning(c, c.getString(R.string.watcher_first_check_warning));
    }

    public static void showNetworkWarning(Context c){
        Chief.makeWarning(c, c.getString(R.string.watcher_traffic_warning));
    }

    private static void startWatcher(Context c){
        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+10000, AlarmManager.INTERVAL_HALF_HOUR, getWatcherPendingIntent(c));
        Chief.makeAToast(c, "The watcher is now watching.");
    }

    private static void killWatcher(Context c){
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(getWatcherPendingIntent(c));
        Chief.makeAToast(c, "The Watcher... has fallen...");
    }

    private static void setWatcherOnBootBCReceiverEnabled(Context c, boolean enabled){
        c.getPackageManager().setComponentEnabledSetting(new ComponentName(c, WatcherIntentService.class), enabled?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private static PendingIntent getWatcherPendingIntent(Context c){
        return PendingIntent.getService(c, 1337322, new Intent(c, WatcherIntentService.class), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("watcher_enabled", false))
            startWatcher(context);
    }
}
