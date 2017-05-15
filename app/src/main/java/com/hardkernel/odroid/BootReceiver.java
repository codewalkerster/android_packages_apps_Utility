package com.hardkernel.odroid;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Debug;
import android.util.Log;

import android.view.KeyEvent;
import android.view.WindowManager;

public class BootReceiver extends BroadcastReceiver {

    private final static String TAG = "ODROIDUtility";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            MainActivity.setValueToNode(pref.getString("governor", "ondemand"), MainActivity.GOVERNOR_NODE);
            MainActivity.setValueToNode(pref.getString("DRAM governor", "simple_exynos"), MainActivity.DRAM_GOVERNOR_NODE);
            MainActivity.setValueToNode(pref.getString("DRAM freq", "825000"), MainActivity.DRAM_FREQUENCY_NODE);
            MainActivity.setMouse(pref.getString("mouse", "right"));
        }

        String pkg[] =  new String[4];
        for(int i=0; i<4;  i++)
            pkg[i]  = pref.getString("shortcut_f" + (i+7), null);

        List<Intent> appList = MainActivity.getAvailableAppList(context);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        for (int i=0;  i<4;  i++) {
            for (Intent app : appList) {
                if (app.getPackage().equals(pkg[i])) {
                    wm.setApplicationShortcut(KeyEvent.KEYCODE_F7 + i, app);
                }
            }
        }
    }
}
