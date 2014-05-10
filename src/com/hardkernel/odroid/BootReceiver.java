package com.hardkernel.odroid;

import android.app.ActionBar.LayoutParams;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class BootReceiver extends BroadcastReceiver {

	private final static String TAG = "ODROIDUtility";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
	        MainActivity.setGovernor(pref.getString("governor", "ondemand"));
	        MainActivity.setClock(pref.getString("max_clock", "1704000"), MainActivity.MAX_FREQ_NODE);
	        MainActivity.setClock(pref.getString("min_clock", "200000"), MainActivity.MIN_FREQ_NODE);
	        MainActivity.setMouse(pref.getString("mouse", "right"));
	        
	        String value = pref.getString("hdmi_rotation", "0");
	        Log.e(TAG, "hdmi_rotation = " + value);
	        if (value.equals("1")) {
	            LinearLayout orientationChanger = new LinearLayout(context);
	            WindowManager.LayoutParams orientationLayout = new WindowManager.LayoutParams(
	                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 
	                    0, PixelFormat.RGBA_8888);
	            orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

	            WindowManager wm = (WindowManager) context.getSystemService(Service.WINDOW_SERVICE);
	            wm.addView(orientationChanger, orientationLayout);
	            orientationChanger.setVisibility(View.VISIBLE);
	        }
		}
	}

}
