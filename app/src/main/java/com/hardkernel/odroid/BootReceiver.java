package com.hardkernel.odroid;

import java.io.OutputStream;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Debug;
import android.util.Log;

import android.os.SystemProperties;
import android.view.KeyEvent;
import android.view.WindowManager;

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
            setMouse(pref.getString("mouse", "right"));

            /* Auto start application on boot */
            if (pref.getBoolean("kodi", false))
                context.startActivity(context.getPackageManager()
                                        .getLaunchIntentForPackage(autoStart));

            MainActivity.checkBootINI();
        }

        SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);

        String pkg[] =  new String[4];
        for(int i=0; i<4;  i++)
            pkg[i]  = pref.getString("shortcut_f" + (i+7), null);

        List<Intent> appList = MainActivity.getAvailableAppList(context);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        for (int i=0;  i<4;  i++) {
            for (Intent app : appList) {
                if (app.getPackage().equals(pkg[i])) {
                    //wm.setApplicationShortcut(KeyEvent.KEYCODE_F7 + i, app);
                }
            }
        }
    }

    private void setMouse(String handed) {
        SystemProperties.set("mouse.firstbutton", handed);
        Log.e(TAG, "set prop mouse.firstbutton " + handed);
    }
}
