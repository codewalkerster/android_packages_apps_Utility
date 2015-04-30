package com.hardkernel.odroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

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
		}
	}

}
