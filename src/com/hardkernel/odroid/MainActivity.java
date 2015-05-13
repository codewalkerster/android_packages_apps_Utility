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

public class MainActivity extends Activity {

    private final static String TAG = "ODROIDUtility";
    public final static String GOVERNOR_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

    private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";

    private Spinner mSpinnerGovernor;
    private String mGovernorString;

    private RadioButton mRadio_left;
    private RadioButton mRadio_right;

    private RadioButton mRadio_hdmi;
    private RadioButton mRadio_lcd;
    private RadioButton mRadio_r1920;
    private RadioButton mRadio_r1280_720;
    private RadioButton mRadio_r1280_800;
    private RadioButton mRadio_p1080p60;
    private RadioButton mRadio_p1080p50;
    private RadioButton mRadio_p1080p30;
    private RadioButton mRadio_p1080i60;
    private RadioButton mRadio_p1080i50;
    private RadioButton mRadio_p800p59;
    private RadioButton mRadio_p720p60;
    private RadioButton mRadio_p720p50;

    private RadioButton mRadio_saved_resolution;
    private RadioButton mRadio_saved_phy;

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

    private RadioGroup mRG_resolution;
    private RadioGroup mRG_phy;
    private RadioGroup mRG_degree;

    private String mOrientation;
    private int mDegree;

    private Process mSu;

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

        tab1.setIndicator("CPU");
        tab1.setContent(R.id.tab1);
        tab2.setIndicator("Mouse");
        tab2.setContent(R.id.tab2);
        tab3.setIndicator("Screen");
        tab3.setContent(R.id.tab3);
        tab4.setIndicator("Rotation");
        tab4.setContent(R.id.tab4);

        //tabHost.addTab(tab1);
        //tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);

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
        mRadio_lcd = (RadioButton)findViewById(R.id.radio_lcd);
        mRadio_lcd.setVisibility(View.GONE);
        mRadio_r1920 = (RadioButton)findViewById(R.id.radio_r1920);
        mRadio_r1280_720 = (RadioButton)findViewById(R.id.radio_r1280);
        mRadio_r1280_800 = (RadioButton)findViewById(R.id.radio_r1280_800);
        mRadio_p1080p60 = (RadioButton)findViewById(R.id.radio_p1080p60);
        mRadio_p1080p50 = (RadioButton)findViewById(R.id.radio_p1080p50);
        mRadio_p1080p30 = (RadioButton)findViewById(R.id.radio_p1080p30);
        mRadio_p1080i60 = (RadioButton)findViewById(R.id.radio_p1080i60);
        mRadio_p1080i50 = (RadioButton)findViewById(R.id.radio_p1080i50);
        mRadio_p720p60 = (RadioButton)findViewById(R.id.radio_p720p60);
        mRadio_p720p50 = (RadioButton)findViewById(R.id.radio_p720p50);
        mRadio_p800p59 = (RadioButton)findViewById(R.id.radio_p800p59);

        mRG_resolution = (RadioGroup)findViewById(R.id.radioGroup_resolution);
        mRG_phy = (RadioGroup)findViewById(R.id.radioGroup_phy);

        File boot_init = new File(BOOT_INI);
        if (boot_init.exists()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(BOOT_INI));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("bootargs"))
                        break;
                    if (line.contains("fb_y_res")) {
                        mRadio_saved_resolution = null;
                        if (line.contains("1080")) {
                            mRadio_r1920.setChecked(true);
                            Log.e(TAG, "1920x1080");
                            mRadio_saved_resolution = mRadio_r1920;
                        } else if (line.contains("800")) {
                            mRadio_r1280_800.setChecked(true);
                            Log.e(TAG, "1280x800");
                            mRadio_saved_resolution = mRadio_r1280_800;
                        } else if (line.contains("720")) {
                            mRadio_r1280_800.setChecked(true);
                            Log.e(TAG, "1280x720");
                            mRadio_saved_resolution = mRadio_r1280_720;
                        }
                    }

                    if (line.contains("vout")) {
                        if (line.contains("hdmi")) {
                            Log.e(TAG, "HDMI");
                            setHDMIMode();
                        } else if (line.contains("lcd")) {
                            Log.e(TAG, "LCD");
                            setLCDMode();
                        }
                    }

                    if (line.contains("hdmi_phy_res")) {
                        mRadio_saved_phy = null;
                        if (line.contains("1080p60hz")) {
                            mRadio_p1080p60.setChecked(true);
                            Log.e(TAG, "1080P 60Hz");
                            mRadio_saved_phy = mRadio_p1080p60;
                        } else if (line.contains("1080p50hz")) {
                            mRadio_p1080p50.setChecked(true);
                            Log.e(TAG, "1080P 50Hz");
                            mRadio_saved_phy = mRadio_p1080p50;
                        } else if (line.contains("1080p30hz")) {
                            mRadio_p1080p30.setChecked(true);
                            Log.e(TAG, "1080P 30Hz");
                            mRadio_saved_phy = mRadio_p1080p30;
                        } else if (line.contains("1080i60hz")) {
                            mRadio_p1080p60.setChecked(true);
                            Log.e(TAG, "1080i 60Hz");
                            mRadio_saved_phy = mRadio_p1080i60;
                        } else if (line.contains("1080i50hz")) {
                            mRadio_p1080i50.setChecked(true);
                            Log.e(TAG, "1080i 60Hz");
                            mRadio_saved_phy = mRadio_p1080i50;
                        } else if (line.contains("720p60hz")) {
                            mRadio_p720p60.setChecked(true);
                            Log.e(TAG, "720P 60Hz");
                            mRadio_saved_phy = mRadio_p720p60;
                        } else if (line.contains("720p50hz")) {
                            mRadio_p720p50.setChecked(true);
                            Log.e(TAG, "720P 50Hz");
                            mRadio_saved_phy = mRadio_p720p50;
                        } else if (line.contains("800p59hz")) {
                            mRadio_p800p59.setChecked(true);
                            Log.e(TAG, "800P 59Hz");
                            mRadio_saved_phy = mRadio_p800p59;
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
            mRadio_r1280_720.setChecked(true);
            mRadio_p720p60.setChecked(true);
        }

        mRadio_hdmi.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setHDMIMode();
            }

        });

        mRadio_lcd.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                setLCDMode();
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

            writer.println("ODROIDXU-UBOOT-CONFIG\n");

            String value;
            if (mRadio_r1920.isChecked())
                value = "1920";
            else
                value = "1280";
            writer.println("setenv fb_x_res \"" + value +"\"");

            if (mRadio_r1920.isChecked())
                value = "1080";
            else if (mRadio_r1280_800.isChecked())
                value = "800";
            else
                value = "720";
            writer.println("setenv fb_y_res \"" + value +"\"\n");

            if (mRadio_hdmi.isChecked())
                value = "hdmi";
            else
                value = "lcd";
            writer.println("setenv vout \"" + value +"\"\n");

            writer.println(
            "setenv left     \"56\"\n" +
            "setenv right    \"24\"\n" +
            "setenv upper    \"3\"\n" +
            "setenv lower    \"3\"\n" +
            "setenv hsync    \"14\"\n" +
            "setenv vsync    \"3\"\n\n");

            if (mRadio_p800p59.isChecked())
                value = "800p59hz";
            else if (mRadio_p720p60.isChecked())
                value = "720p60hz";
            else if (mRadio_p720p50.isChecked())
                value = "720p50hz";
            else if (mRadio_p1080p60.isChecked())
                value = "1080p60hz";
            else if (mRadio_p1080i60.isChecked())
                value = "1080i60hz";
            else if (mRadio_p1080i50.isChecked())
                value = "1080i50hz";
            else if (mRadio_p1080p50.isChecked())
                value = "1080p50hz";
            else if (mRadio_p1080p30.isChecked())
                value = "1080p30hz";

            writer.println("setenv hdmi_phy_res \"" + value +"\"\n");
            writer.println("setenv edid \"0\"\n");
            writer.println("setenv led_blink        \"1\"\n");
            writer.println("setenv bootcmd      \"movi read kernel 0 40008000;bootz 40008000\"\n");
            writer.println("setenv bootargs     \"fb_x_res=${fb_x_res} fb_y_res=${fb_y_res} vout=${vout} hdmi_phy_res=${hdmi_phy_res} edid=${edid} left=${left} right=${right} upper=${upper} lower=${lower} vsync=${vsync} hsync=${hsync} led_blink=${led_blink}\"");

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

    private void reboot() {
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

    public void setLCDMode() {
        mRadio_lcd.setChecked(true);
        for (int i = 0; i < mRG_resolution.getChildCount(); i++) {
            mRG_resolution.getChildAt(i).setEnabled(false);
        }

        for (int i = 0; i < mRG_phy.getChildCount(); i++) {
            mRG_phy.getChildAt(i).setEnabled(false);
        }

        mRadio_r1280_800.setChecked(true);
        mRadio_p1080p60.setChecked(true);
    }

    public void setHDMIMode() {
        mRadio_hdmi.setChecked(true);
        for (int i = 0; i < mRG_resolution.getChildCount(); i++) {
            mRG_resolution.getChildAt(i).setEnabled(true);
        }
        for (int i = 0; i < mRG_phy.getChildCount(); i++) {
            mRG_phy.getChildAt(i).setEnabled(true);
        }

        //mRadio_r1280_800.setEnabled(false);

        if (mRadio_saved_resolution == mRadio_r1920)
            mRadio_r1920.setChecked(true);
        else if (mRadio_saved_resolution == mRadio_r1280_800)
            mRadio_r1280_800.setChecked(true);
        else
            mRadio_r1280_720.setChecked(true);

        if (mRadio_saved_phy == mRadio_p800p59)
            mRadio_p800p59.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p720p50)
            mRadio_p720p60.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p720p50)
            mRadio_p720p50.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p1080p60)
            mRadio_p1080p60.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p1080i60)
            mRadio_p1080i60.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p1080i50)
            mRadio_p1080i50.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p1080p50)
            mRadio_p1080p50.setChecked(true);
        else if (mRadio_saved_phy == mRadio_p1080p30)
            mRadio_p1080p30.setChecked(true);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
