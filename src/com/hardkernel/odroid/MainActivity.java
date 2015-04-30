package com.hardkernel.odroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final static String TAG = "ODROIDUtility";
    public final static String GOVERNOR_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public final static String MAX_FREQ_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public final static String MIN_FREQ_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";

    private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";
    public int mCurrentMaxFreq;
    public int mCurrentMinFreq;

    private Spinner mSpinnerGovernor;
    private String mGovernorString;

    private Spinner mSpinnerMaxFreq;
    private Spinner mSpinnerMinFreq;

    private RadioButton mRadio_left;
    private RadioButton mRadio_right;

    private HashMap<String, String> mClockMap;
    private List<String> clock_list;
    private ArrayAdapter<CharSequence> mAdapterClock;

    private RadioButton mRadio_hdmi;
    private RadioButton mRadio_dvi;
    private RadioButton mRadio_r1920;
    private RadioButton mRadio_r1280;
    private RadioButton mRadio_r1360;
    private RadioButton mRadio_p1080p60;
    private RadioButton mRadio_p1080i60;
    private RadioButton mRadio_p1080p50;
    private RadioButton mRadio_p720p60;
    private RadioButton mRadio_p720p50;
    private RadioButton mRadio_lcd_false;
    private RadioButton mRadio_lcd_true;
    private RadioButton mRadio_mipi;

    private RadioButton mRadio_saved_resolution;
    private RadioButton mRadio_saved_phy;

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

    private RadioButton mRadio_AnalogMIC;
    private RadioButton mRadio_DigitalMIC;

    private RadioGroup mRG_resolution;
    private RadioGroup mRG_phy;
    private RadioGroup mRG_degree;

    private String mProduct;
    private String mOrientation;
    private int mDegree;

    private Process mSu;

    private final static int CPU_TAB = 0;
    private final static int MOUSE_TAB = 1;
    private final static int SCREEN_TAB = 2;
    private final static int ROTATION_TAB = 3;
    private final static int CAMERA_TAB = 4;
    private final static int MIC_TAB = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mSu = Runtime.getRuntime().exec("su");
        } catch (Exception e) {
        }


        mRadio_left = (RadioButton)findViewById(R.id.radio_left);
        mRadio_right = (RadioButton)findViewById(R.id.radio_right);

        mRadio_AnalogMIC = (RadioButton)findViewById(R.id.radio_analog_mic);
        mRadio_DigitalMIC = (RadioButton)findViewById(R.id.radio_digital_mic);

        InputStream inputstream = null;
        try {
            inputstream = Runtime.getRuntime().exec("getprop")
                    .getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader bufferedReader = new BufferedReader(
                  new InputStreamReader(inputstream));

        mOrientation = "landscape";
        mDegree = 0;

        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("mouse.firstbutton")) {
                    if (line.contains("right")) {
                        mRadio_right.toggle();
                        Log.e(TAG, "right");
                    } else if (line.contains("left")) {
                        mRadio_left.toggle();
                        Log.e(TAG, "left");
                    }
                }
                if (line.contains("ro.build.product")) {
                    Log.e(TAG, line);
                    mProduct = line.substring(21, line.length() -1);
                    Log.e(TAG, mProduct);
                }
                if (line.contains("persist.demo.hdmirotation")) {
                    if (line.contains("portrait")) {
                        Log.e(TAG, line);
                        mOrientation = "portrait";
                    }
                }
                if (line.contains("ro.sf.hwrotation")) {
                    if (line.contains("90"))
                        mDegree = 90;
                    else if (line.contains("270"))
                        mDegree = 270;
                    else if (line.contains("0"))
                        mDegree = 0;
                }
            }
            bufferedReader.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabSpec tab1 = tabHost.newTabSpec("CPU");
        TabSpec tab2 = tabHost.newTabSpec("Mouse");
        TabSpec tab3 = tabHost.newTabSpec("Screen");
        TabSpec tab4 = tabHost.newTabSpec("Rotation");
        TabSpec tab5 = tabHost.newTabSpec("Camera");
        TabSpec tab6 = tabHost.newTabSpec("MIC");

        tab1.setIndicator("CPU");
        tab1.setContent(R.id.tab1);
        tab2.setIndicator("Mouse");
        tab2.setContent(R.id.tab2);
        tab3.setIndicator("Screen");
        tab3.setContent(R.id.tab3);
        tab4.setIndicator("Rotation");
        tab4.setContent(R.id.tab4);
        tab5.setIndicator("Camera");
        tab5.setContent(R.id.tab5);
        tab6.setIndicator("MIC");
        tab6.setContent(R.id.tab6);

        tabHost.addTab(tab1);
        tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);
        tabHost.addTab(tab5);
        tabHost.addTab(tab6);

        clock_list = new ArrayList<String>();
        if (!mProduct.equals("odroidx")) {
            clock_list.add("1704000");
            clock_list.add("1600000");
            clock_list.add("1500000");
        }
        clock_list.add("1400000");
        clock_list.add("1300000");
        clock_list.add("1200000");
        clock_list.add("1100000");
        clock_list.add("1000000");
        clock_list.add("900000");
        clock_list.add("800000");
        clock_list.add("700000");
        clock_list.add("600000");
        clock_list.add("500000");
        clock_list.add("400000");
        clock_list.add("300000");
        clock_list.add("200000");

        if (mProduct.equals("odroidx")) {
            mAdapterClock = ArrayAdapter.createFromResource(this,
                    R.array.clock_array, android.R.layout.simple_spinner_item);
        } else {
            mAdapterClock = ArrayAdapter.createFromResource(this,
                    R.array.clock_prime_array, android.R.layout.simple_spinner_item);
        }
        mAdapterClock.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mClockMap = new HashMap<String, String>();
        for (int i = 0; i < clock_list.size(); i++) {
            //Log.e(TAG, clock_list.get(i) + " : " + mAdapterClock.getItem(i).toString());
            mClockMap.put(clock_list.get(i), mAdapterClock.getItem(i).toString());
        }

        /*
        for (int x = 0; x < mClockMap.size(); x++) {
            Log.e(TAG, mClockMap.get(clock_list.get(x)));
        }
        */

        mSpinnerMaxFreq = (Spinner) findViewById(R.id.spinner_max_freq);
        mSpinnerMaxFreq.setAdapter(mAdapterClock);
        String max_freq = getMaxFreq();
        mCurrentMaxFreq = Integer.parseInt(max_freq);
        if (max_freq != null) {
            if (mClockMap.get(max_freq) != null) {
                Log.e(TAG, mClockMap.get(max_freq));
                mSpinnerMaxFreq.setSelection(mAdapterClock.getPosition(mClockMap.get(max_freq)));
            }
        }
        mSpinnerMaxFreq.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                Log.e(TAG, "Max Freq = " + arg0.getItemAtPosition(arg2).toString());
                String clock = clock_list.get(arg2);
                String min_clock = getMinFreq();
                if (Integer.parseInt(min_clock) > Integer.parseInt(clock)) {
                    mSpinnerMaxFreq.setSelection(mAdapterClock.getPosition(mClockMap.get(min_clock)));
                    return;
                }

                setClock(clock, MAX_FREQ_NODE);

                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("max_clock", clock);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        mSpinnerMinFreq = (Spinner) findViewById(R.id.spinner_min_freq);
        mSpinnerMinFreq.setAdapter(mAdapterClock);
        String min_freq = getMinFreq();
        mCurrentMinFreq = Integer.parseInt(min_freq);
        if (min_freq != null) {
            if (mClockMap.get(min_freq) != null) {
                Log.e(TAG, mClockMap.get(min_freq));
                mSpinnerMinFreq.setSelection(mAdapterClock.getPosition(mClockMap.get(min_freq)));
            }
        }
        mSpinnerMinFreq.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                Log.e(TAG, "Min Freq = " + arg0.getItemAtPosition(arg2).toString());
                String clock = clock_list.get(arg2);
                String max_clock = getMaxFreq();
                if (Integer.parseInt(max_clock) < Integer.parseInt(clock)) {
                    mSpinnerMinFreq.setSelection(mAdapterClock.getPosition(mClockMap.get(max_clock)));
                    return;
                }

                setClock(clock, MIN_FREQ_NODE);

                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("min_clock", clock);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        mSpinnerGovernor = (Spinner) findViewById(R.id.spinner_governors);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> mAdapterGovenor = ArrayAdapter.createFromResource(this,
                R.array.governor_array, android.R.layout.simple_spinner_item);
        mAdapterGovenor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerGovernor.setAdapter(mAdapterGovenor);
        
        mSpinnerGovernor.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                String governor = arg0.getItemAtPosition(arg2).toString();
                Log.e(TAG, "governor = " + governor);
                setGovernor(governor);
                if (governor.equals("performance"))
                    mSpinnerMinFreq.setEnabled(false);
                else
                    mSpinnerMinFreq.setEnabled(true);

                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("governor", governor);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        mGovernorString = getCurrentGovernor();

        if (mGovernorString != null) {
            mSpinnerGovernor.setSelection(mAdapterGovenor.getPosition(mGovernorString));
        }

        mRadio_left = (RadioButton)findViewById(R.id.radio_left);
        mRadio_right = (RadioButton)findViewById(R.id.radio_right);

        SharedPreferences pref = getSharedPreferences("utility", Context.MODE_PRIVATE);
        if (pref.getString("mouse", "right").equals("right"))
            mRadio_right.setChecked(true);
        else
            mRadio_left.setChecked(true);

        tabHost.getTabWidget().getChildAt(MOUSE_TAB).setVisibility(View.GONE);
        tabHost.getTabWidget().getChildAt(CAMERA_TAB).setVisibility(View.GONE);

        if (mProduct.equals("odroidx") || mProduct.equals("odroidx2"))
            tabHost.getTabWidget().getChildAt(MIC_TAB).setVisibility(View.GONE);

        //tabHost.getTabWidget().getChildAt(3).setVisibility(View.GONE);

        Button btn = (Button)findViewById(R.id.button_mouse_apply);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                if (mRadio_left.isChecked()) {
                    editor.putString("mouse", "left");
                    setMouse("left");
                } else if (mRadio_right.isChecked()) {
                    editor.putString("mouse", "right");
                    setMouse("right");
                }
                editor.commit();
            }

        });

        mRadio_hdmi = (RadioButton)findViewById(R.id.radio_hdmi);
        mRadio_dvi = (RadioButton)findViewById(R.id.radio_dvi);
        mRadio_r1920 = (RadioButton)findViewById(R.id.radio_r1920);
        mRadio_r1280 = (RadioButton)findViewById(R.id.radio_r1280);
        mRadio_r1360 = (RadioButton)findViewById(R.id.radio_r1360);
        mRadio_p1080p60 = (RadioButton)findViewById(R.id.radio_p1080p60);
        mRadio_p1080i60 = (RadioButton)findViewById(R.id.radio_p1080i60);
        mRadio_p1080p50 = (RadioButton)findViewById(R.id.radio_p1080p50);
        mRadio_p720p60 = (RadioButton)findViewById(R.id.radio_p720p60);
        mRadio_p720p50 = (RadioButton)findViewById(R.id.radio_p720p50);
        mRadio_lcd_false = (RadioButton)findViewById(R.id.radio_lcd_false);
        mRadio_lcd_true = (RadioButton)findViewById(R.id.radio_lcd_true);

        mRG_resolution = (RadioGroup)findViewById(R.id.radioGroup_resolution);
        mRG_phy = (RadioGroup)findViewById(R.id.radioGroup_phy);

        if (!(mProduct.equals("odroidx") || mProduct.equals("odroidx2"))) {
            RadioGroup rg = (RadioGroup)findViewById(R.id.radioGroup_lcd);
            rg.setVisibility(View.GONE);
            TextView tv = (TextView)findViewById(R.id.textView_lcd);
            tv.setVisibility(View.GONE);
        }

        File boot_ini = new File(BOOT_INI);
        if (boot_ini.exists()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(BOOT_INI));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("bootargs")) {
                        if (line.contains("audio_in=dmic"))
                            mRadio_DigitalMIC.setChecked(true);
                        break;
                    }

                    if (line.contains("v_out")) {
                        if (line.contains("dvi")) {
                            Log.e(TAG, "DVI");
                            setDVIMode();
                        } else if (line.contains("hdmi")) {
                            Log.e(TAG, "HDMI");
                            setHDMIMode();
                            if (line.contains("fake_fb")) {
                                if (line.contains("false")) {
                                    Log.e(TAG, "fake fb = false");
                                    mRadio_lcd_true.setChecked(true);
                                    if (!mProduct.equals("odroidu"))
                                        setLCDMode(true);
                                } else if (line.contains("true")) {
                                    Log.e(TAG, "fake fb = true");
                                    mRadio_lcd_false.setChecked(true);
                                    setLCDMode(false);
                                }
                            }
                        }
                    }

                    if (line.contains("fb_x_res")) {
                        mRadio_saved_resolution = null;
                        if (line.contains("1920")) {
                            mRadio_r1920.setChecked(true);
                            Log.e(TAG, "1920x1080");
                            mRadio_saved_resolution = mRadio_r1920;
                        } else if (line.contains("1280")) {
                            mRadio_r1280.setChecked(true);
                            Log.e(TAG, "1280x720");
                            mRadio_saved_resolution = mRadio_r1280;
                        } else if (line.contains("1360")) {
                            mRadio_r1360.setChecked(true);
                            Log.e(TAG, "1360x768");
                            mRadio_saved_resolution = mRadio_r1360;
                        }
                    }

                    if (line.contains("hdmi_phy_res")) {
                        mRadio_saved_phy = null;
                        if (line.contains("1080i")) {
                            mRadio_p1080i60.setChecked(true);
                            Log.e(TAG, "1080I60");
                        } else if (line.contains("1080")) {
                            mRadio_p1080p60.setChecked(true);
                            Log.e(TAG, "1080P60");
                            mRadio_saved_phy = mRadio_p1080p60;
                        } else if (line.contains("1080p50")) {
                            mRadio_p1080p50.setChecked(true);
                            Log.e(TAG, "1080P50");
                            mRadio_saved_phy = mRadio_p1080p50;
                        } else if (line.contains("720")) {
                            mRadio_p720p60.setChecked(true);
                            Log.e(TAG, "720P");
                            mRadio_saved_phy = mRadio_p720p60;
                        } else if (line.contains("720p50")) {
                            mRadio_p720p50.setChecked(true);
                            Log.e(TAG, "720P50");
                            mRadio_saved_phy = mRadio_p720p50;
                        }
                    }
                }
                bufferedReader.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else {
            //default value
            mRadio_hdmi.setChecked(true);
            mRadio_lcd_false.setChecked(true);
            setLCDMode(false);
            mRadio_r1280.setChecked(true);
            mRadio_p720p60.setChecked(true);
        }

        mRadio_hdmi.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setHDMIMode();
            }

        });

        mRadio_dvi.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setDVIMode();
            }

        });

        mRadio_lcd_false.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setLCDMode(false);
            }

        });

        mRadio_lcd_true.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setLCDMode(true);
            }

        });

        btn = (Button)findViewById(R.id.button_screen_apply);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                saveBootIni();
            }

        });

        mRadio_mipi = (RadioButton)findViewById(R.id.radio_mipi);

        btn = (Button)findViewById(R.id.button_camera_apply);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                OutputStream stream;
                Process p;
                try {
                    stream = mSu.getOutputStream();
                    String cmd =  "mount -o rw,remount /system \n";
                    stream.write(cmd.getBytes());
                    stream.flush();
                    if (mRadio_mipi.isChecked())
                        cmd = "cp /system/lib/hw/camera.odroid_mipi.so /system/lib/hw/camera." + mProduct + ".so\n";
                    else
                        cmd = "cp /system/lib/hw/camera.odroid_uvc.so /system/lib/hw/camera." + mProduct + ".so\n";
                    stream.write(cmd.getBytes());
                    Log.e(TAG, cmd);
                    stream.flush();
                    cmd = "chmod 644 /system/lib/hw/camera." + mProduct + ".so\n";
                    stream.write(cmd.getBytes());
                    Log.e(TAG, cmd);
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });

        btn = (Button)findViewById(R.id.button_apply_reboot);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                reboot();
            }

        });

        mRadio_portrait = (RadioButton)findViewById(R.id.radio_portrait);
        mRadio_landscape = (RadioButton)findViewById(R.id.radio_landscape);
        mRadio_90 = (RadioButton)findViewById(R.id.radio_90);
        mRadio_270 = (RadioButton)findViewById(R.id.radio_270);
        mRG_degree = (RadioGroup)findViewById(R.id.radioGroup_degree);
        if (mOrientation.equals("landscape")) {
           mRadio_landscape.setChecked(true);
           mRG_degree.setVisibility(View.GONE);
           mDegree = 0;
        } else {
           mRadio_portrait.setChecked(true);
           mRG_degree.setVisibility(View.VISIBLE);
        }

        if (mDegree == 90) {
            mRadio_90.setChecked(true);
            mRadio_270.setChecked(false);
        } else {
            mRadio_90.setChecked(false);
            mRadio_270.setChecked(true);
        }

        mRadio_portrait.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mRG_degree.setVisibility(View.VISIBLE);
                mDegree = 270;
                mRadio_90.setChecked(false);
                mRadio_270.setChecked(true);
            }

        });

        mRadio_landscape.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mRG_degree.setVisibility(View.GONE);
                mDegree = 0;
            }

        });

        mRadio_90.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mDegree = 90;
            }

        });

        mRadio_270.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mDegree = 270;
            }

        });


        btn = (Button)findViewById(R.id.button_rotation_apply);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                try {
                    DataOutputStream stdin = new DataOutputStream(mSu.getOutputStream());
                    stdin.writeBytes("mount -o rw,remount /system\n");

                    if (mRadio_portrait.isChecked()) {
                        stdin.writeBytes("sed -i s/persist.demo.hdmirotation=landscape/persist.demo.hdmirotation=portrait/g /system/build.prop\n");
                        if (mDegree == 90) {
                            stdin.writeBytes("sed -i s/ro.sf.hwrotation=0/ro.sf.hwrotation=90/g /system/build.prop\n");
                            stdin.writeBytes("sed -i s/ro.sf.hwrotation=270/ro.sf.hwrotation=90/g /system/build.prop\n");
                        } else {
                            stdin.writeBytes("sed -i s/ro.sf.hwrotation=0/ro.sf.hwrotation=270/g /system/build.prop\n");
                            stdin.writeBytes("sed -i s/ro.sf.hwrotation=90/ro.sf.hwrotation=270/g /system/build.prop\n");
                        }
                    } else if (mRadio_landscape.isChecked()) {
                        stdin.writeBytes("sed -i s/persist.demo.hdmirotation=portrait/persist.demo.hdmirotation=landscape/g /system/build.prop\n");
                        stdin.writeBytes("sed -i s/ro.sf.hwrotation=90/ro.sf.hwrotation=0/g /system/build.prop\n");
                        stdin.writeBytes("sed -i s/ro.sf.hwrotation=270/ro.sf.hwrotation=0/g /system/build.prop\n");
                    }

                    stdin.writeBytes("mount -o ro,remount /system\n");
                    if (mDegree == 0) {
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 0);
                    } else if (mDegree == 90) {
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 1);
                    } else if (mDegree == 270) {
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 3);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                saveBootIni();
            }

        });
    }

    public void saveBootIni() {
        File boot_ini = new File(BOOT_INI);
        if (boot_ini.exists()) {
            boot_ini.delete();
        }

        PrintWriter writer;
        try {
            writer = new PrintWriter(BOOT_INI, "UTF-8");

            writer.println("ODROID4412-UBOOT-CONFIG");
            String value;
            if (mRadio_hdmi.isChecked())
                value = "hdmi";
            else
                value = "dvi";
            writer.println("setenv v_out \"" + value +"\"");

            if (mRadio_lcd_true.isChecked())
                value = "false";
            else
                value = "true";
            writer.println("setenv fake_fb \"" + value +"\"");

            if (mRadio_r1360.isChecked())
                value = "1360";
            else if (mRadio_r1280.isChecked())
                value = "1280";
            else
                value = "1920";
            writer.println("setenv fb_x_res \"" + value +"\"");

            if (mRadio_r1360.isChecked())
            value = "768";
            else if (mRadio_r1280.isChecked())
                value = "720";
            else
                value = "1080";
            writer.println("setenv fb_y_res \"" + value +"\"");

            if (mRadio_p720p60.isChecked())
                value = "720";
            else if (mRadio_p720p50.isChecked())
                value = "720p50";
            else if (mRadio_p1080i60.isChecked())
                value = "1080i";
            else if (mRadio_p1080p60.isChecked())
                value = "1080";
            else if (mRadio_p1080p50.isChecked())
                value = "1080p50";
            writer.println("setenv hdmi_phy_res \"" + value +"\"");

            writer.println("setenv led_blink        \"1\"");
            writer.println("setenv bootcmd      \"movi read kernel 0 40008000;movi read rootfs 0 41000000 100000;bootm 40008000 41000000\"");
            if (mRadio_DigitalMIC.isChecked())
                writer.println("setenv bootargs     \"console=/dev/ttySAC1,115200n8 androidboot.console=ttySAC1 v_out=${v_out} fake_fb=${fake_fb} fb_x_res=${fb_x_res} fb_y_res=${fb_y_res} hdmi_phy_res=${hdmi_phy_res} led_blink=${led_blink} audio_in=dmic\"");
            else
                writer.println("setenv bootargs     \"console=/dev/ttySAC1,115200n8 androidboot.console=ttySAC1 v_out=${v_out} fake_fb=${fake_fb} fb_x_res=${fb_x_res} fb_y_res=${fb_y_res} hdmi_phy_res=${hdmi_phy_res} led_blink=${led_blink}\"");

            writer.println("boot");
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void reboot() {
        OutputStream stream;
        try {
            stream = mSu.getOutputStream();
            String cmd =  "reboot";
            stream.write(cmd.getBytes());
            stream.flush();
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void setLCDMode(boolean lcd) {
        for (int i = 0; i < mRG_resolution.getChildCount(); i++) {
            mRG_resolution.getChildAt(i).setEnabled(!lcd);
        }

        for (int i = 0; i < mRG_phy.getChildCount(); i++) {
            mRG_phy.getChildAt(i).setEnabled(true);
        }

        if (lcd)
            mRadio_r1360.setChecked(true);
        else {
            mRadio_r1360.setEnabled(false);

            if (mRadio_saved_resolution == mRadio_r1360)
                mRadio_r1360.setChecked(true);
            else if (mRadio_saved_resolution == mRadio_r1280)
                mRadio_r1280.setChecked(true);
            else
                mRadio_r1920.setChecked(true);

            if (mRadio_saved_phy == mRadio_p720p60)
                mRadio_p720p60.setChecked(true);
            else if (mRadio_saved_phy == mRadio_p720p50)
                mRadio_p720p50.setChecked(true);
            else if (mRadio_saved_phy == mRadio_p1080i60)
                mRadio_p1080i60.setChecked(true);
            else if (mRadio_saved_phy == mRadio_p1080p60)
                mRadio_p1080p60.setChecked(true);
            else if (mRadio_saved_phy == mRadio_p1080p50)
                mRadio_p1080p50.setChecked(true);
        }
    }

    public void setDVIMode() {
        mRadio_dvi.setChecked(true);

        /*
        for (int i = 0; i < mRG_resolution.getChildCount(); i++) {
            mRG_resolution.getChildAt(i).setEnabled(false);
        }
        for (int i = 0; i < mRG_phy.getChildCount(); i++) {
            mRG_phy.getChildAt(i).setEnabled(false);
        }
        */
        mRadio_r1360.setEnabled(false);
        mRadio_r1280.setChecked(true);
        mRadio_p720p60.setChecked(true);
    }

    public void setHDMIMode() {
        mRadio_hdmi.setChecked(true);
        for (int i = 0; i < mRG_resolution.getChildCount(); i++) {
            mRG_resolution.getChildAt(i).setEnabled(true);
        }
        for (int i = 0; i < mRG_phy.getChildCount(); i++) {
            mRG_phy.getChildAt(i).setEnabled(true);
        }
    }

    public static void setMouse(String handed) {
        try {
            OutputStream stream;
            Process p = Runtime.getRuntime().exec("su");
            stream = p.getOutputStream();
            String cmd =  "setprop mouse.firstbutton " + handed;
            stream.write(cmd.getBytes());
            stream.flush();
            stream.close();

            Log.e(TAG, "setprop mouse.firstbutton " + handed);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void setGovernor(String governor) {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(GOVERNOR_NODE));
            out.write(governor);
            out.newLine();
            out.close();
            Log.e(TAG, "set governor : " + governor);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void setClock(String clock, String node) {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(node));
            out.write(clock);
            out.newLine();
            out.close();
            Log.e(TAG, "set clock : " + clock + " , " + node);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (mRadio_portrait.isChecked()) {
            mRG_degree.setVisibility(View.VISIBLE);
        }
    }

    protected String getCurrentGovernor() {
        String governor = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(GOVERNOR_NODE));
            governor = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, governor);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return governor;
    }

    protected String getMaxFreq() {
        String freq = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(MAX_FREQ_NODE));
            freq = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, "scaling_max_freq = " + freq);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return freq;
    }

    protected String getMinFreq() {
        String freq = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(MIN_FREQ_NODE));
            freq = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, "scaling_min_freq = " + freq);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return freq;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
