package com.hardkernel.odroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private final static String TAG = "ODROIDUtility";
    private final static String autoStart = "org.xbmc.kodi";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
            MainActivity.setGovernor(pref.getString("governor", "performance"));
            MainActivity.setScalingMaxFreq(pref.getString("freq", "1488000"));
            MainActivity.setMouse(pref.getString("mouse", "right"));

            /* Auto start application on boot */
            if (pref.getBoolean("kodi", false))
                context.startActivity(context.getPackageManager()
                                        .getLaunchIntentForPackage(autoStart));

            MainActivity.checkBootINI();
        }
    }

}
