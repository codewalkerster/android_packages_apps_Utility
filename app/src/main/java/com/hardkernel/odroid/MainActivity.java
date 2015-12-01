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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;
import android.widget.ToggleButton;

import static java.lang.System.*;

public class MainActivity extends Activity {

    private final static String TAG = "ODROIDUtility";
    public final static String GOVERNOR_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

    private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";

    private Spinner mSpinnerGovernor;
    private String mGovernorString;

    private RadioButton mRadio_left;
    private RadioButton mRadio_right;

    private Spinner mSpinner_Resolution;
    private String mResolution = "720p60hz";

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

    private RadioGroup mRG_resolution;
    private RadioGroup mRG_phy;
    private RadioGroup mRG_degree;

    private String mOrientation;
    private int mDegree;

    private ToggleButton mBtnFanMode;
    private EditText mEditTextFanSpeed1;
    private EditText mEditTextFanSpeed2;
    private EditText mEditTextFanSpeed3;
    private EditText mEditTextFanSpeed4;
    private EditText mEditTextSpeeds[] = new EditText[4];
    private Button mBtnFanSpeedsApply;
    private Button mBtnGetPWMDuty;
    private EditText mEditTextPWMDuty;
    private Button mBtnPWMDutyApply;
    private ToggleButton mBtnPWMEnable;
    private EditText mEditTextTempLevels[] = new EditText[3];
    private EditText mEditTextTempLevel1;
    private EditText mEditTextTempLevel2;
    private EditText mEditTextTempLevel3;
    private Button mBtnTempLevelsApply;

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
        TabSpec tab5 = tabHost.newTabSpec("Fan");

        tab1.setIndicator("CPU");
        tab1.setContent(R.id.tab1);
        tab2.setIndicator("Mouse");
        tab2.setContent(R.id.tab2);
        tab3.setIndicator("Screen");
        tab3.setContent(R.id.tab3);
        tab4.setIndicator("Rotation");
        tab4.setContent(R.id.tab4);
        tab5.setIndicator("Fan");
        tab5.setContent(R.id.tab5);

        //tabHost.addTab(tab1);
        //tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);
        tabHost.addTab(tab5);

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

        File boot_ini = new File(BOOT_INI);
        if (boot_ini.exists()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(BOOT_INI));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("bootargs"))
                        break;

                    if (line.contains("hdmi_phy_res") && line.indexOf("#") < 0) {
                        Log.e(TAG, line);
                        mResolution = line.substring(line.indexOf("\"") + 1, line.length() - 1);
                        Log.e(TAG, mResolution);
                    }
                }
                bufferedReader.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        mSpinner_Resolution = (Spinner)findViewById(R.id.spinner_resolution);
        ArrayAdapter<CharSequence> mAdapterResolution = ArrayAdapter.createFromResource(this,
                R.array.resolution_array, android.R.layout.simple_spinner_item);
        mAdapterResolution.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_Resolution.setAdapter(mAdapterResolution);

        mSpinner_Resolution.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                String resolution = arg0.getItemAtPosition(arg2).toString();
                if (mResolution.equals(resolution))
                    return;
                else
                    mResolution = resolution;

                Log.e(TAG, "Selected resolution = " + resolution);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });
        for(int i = 0; i < mAdapterResolution.getCount(); i++) {
            String item = (String)mAdapterResolution.getItem(i);
            Log.e(TAG, item);
        }

        mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));

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

        mBtnFanMode = (ToggleButton) findViewById(R.id.btn_fan_mode);

        mBtnFanMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int auto_manual = 0;
                if (isChecked) {
                    auto_manual = 1;
                }
                setFanMode(auto_manual);
                getFanValues();
            }
        });

        mEditTextFanSpeed1 = (EditText) findViewById(R.id.et_speed1);
        mEditTextFanSpeed2 = (EditText) findViewById(R.id.et_speed2);
        mEditTextFanSpeed3 = (EditText) findViewById(R.id.et_speed3);
        mEditTextFanSpeed4 = (EditText) findViewById(R.id.et_speed4);

        mEditTextSpeeds[0]= mEditTextFanSpeed1;
        mEditTextSpeeds[1]= mEditTextFanSpeed2;
        mEditTextSpeeds[2]= mEditTextFanSpeed3;
        mEditTextSpeeds[3]= mEditTextFanSpeed4;

        mBtnFanSpeedsApply = (Button) findViewById(R.id.btn_speed);
        mBtnFanSpeedsApply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int fan_speed[] = new int[4];
                int i = 0;
                for(EditText ed : mEditTextSpeeds) {
                    fan_speed[i++] = Integer.parseInt(ed.getText().toString());
                }
                String value;
                if ((fan_speed[0] > 0) && (fan_speed[0] < fan_speed[1]) &&
                        (fan_speed[1] < fan_speed[2]) && (fan_speed[2] < fan_speed[3]) && fan_speed[3] <= 100) {
                    value = mEditTextFanSpeed1.getText() + " " + mEditTextFanSpeed2.getText() + " " +
                            mEditTextFanSpeed3.getText() + " " + mEditTextFanSpeed4.getText();
                    Log.e(TAG, value);
                    setFanSpeeds(value);
                    getFanValues();
                } else {
                    Toast.makeText(
                            getBaseContext(), "Wrong format",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnGetPWMDuty = (Button) findViewById(R.id.btn_get_pwm_duty);
        mBtnGetPWMDuty.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = readPWMDuty();
                Log.e(TAG, value);
                value = value.trim();
                mEditTextPWMDuty.setText(value);
            }
        });

        mEditTextPWMDuty = (EditText) findViewById(R.id.et_pwm_duty);
        mBtnPWMDutyApply = (Button) findViewById(R.id.btn_pwm_duty);
        mBtnPWMDutyApply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int duty = Integer.parseInt(mEditTextPWMDuty.getText().toString());
                if (duty >= 0 && duty < 256) {
                    String value = mEditTextPWMDuty.getText().toString().trim();
                    setPWMDuty(value);
                    getFanValues();
                } else {
                    Toast.makeText(
                            getBaseContext(), "Wrong format",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnPWMEnable = (ToggleButton) findViewById(R.id.btn_pwm_enable);
        mBtnPWMEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int enable = 0;
                if (isChecked) {
                    enable = 1;
                }
                setPWMEnable(enable);
                getFanValues();
            }
        });

        mEditTextTempLevel1 = (EditText) findViewById(R.id.et_temp1);
        mEditTextTempLevel2 = (EditText) findViewById(R.id.et_temp2);
        mEditTextTempLevel3 = (EditText) findViewById(R.id.et_temp3);

        mEditTextTempLevels[0] = mEditTextTempLevel1;
        mEditTextTempLevels[1] = mEditTextTempLevel2;
        mEditTextTempLevels[2] = mEditTextTempLevel3;

        mBtnTempLevelsApply = (Button) findViewById(R.id.btn_temp_levels);
        mBtnTempLevelsApply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp_levels[] = new int[3];
                int i = 0;
                for(EditText ed : mEditTextTempLevels) {
                    temp_levels[i++] = Integer.parseInt(ed.getText().toString());
                }
                String value;
                if ((temp_levels[0] > 0) && (temp_levels[0] < temp_levels[1]) &&
                        (temp_levels[1] < temp_levels[2]) && temp_levels[2] <= 100) {
                    value = mEditTextTempLevels[0].getText() + " " + mEditTextTempLevels[1].getText() + " " +
                            mEditTextTempLevels[2].getText();
                    Log.e(TAG, value);
                    setTempLevels(value);
                    getFanValues();
                } else {
                    Toast.makeText(
                            getBaseContext(), "Wrong format",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        getFanValues();
    }

    private void getFanValues() {
        String value = readFanMode();
        Log.e(TAG, value);

        if (value.contains("auto")) {
            mBtnFanMode.setChecked(true);
        } else {
            mBtnFanMode.setChecked(false);
        }

        value = readFanSpeeds();
        Log.e(TAG, value);
        int i = 0;
        for (String token : value.split(" ")) {
            token = token.trim();
            mEditTextSpeeds[i++].setText(token);
        }

        value = readPWMDuty();
        Log.e(TAG, value);
        value = value.trim();
        mEditTextPWMDuty.setText(value);

        value = readPWMEnable();
        Log.e(TAG, value);

        if (value.contains("on")) {
            mBtnPWMEnable.setChecked(true);
        } else {
            mBtnPWMEnable.setChecked(false);
        }

        value = readTempLevels();
        Log.e(TAG, value);
        i = 0;
        for (String token : value.split(" ")) {
            token = token.trim();
            mEditTextTempLevels[i++].setText(token);
        }
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

            String x_res = "1280";
            String y_res = "720";
            if ("480p60hz".equals(mResolution)) {
                x_res = "640";
                y_res = "480";
           } else if ("480p59.94hz".equals(mResolution)) {
                x_res = "720";
                y_res = "480";
            } else if ("576p50hz".equals(mResolution)) {
                x_res = "720";
                y_res = "576";
            } else if ("800x480p60hz".equals(mResolution)) {
                x_res = "800";
                y_res = "480";
            } else if ("600p60hz".equals(mResolution)) {
                x_res = "800";
                y_res = "600";
            } else if ("768p60hz".equals(mResolution)) {
                x_res = "1024";
                y_res = "768";
            } else if (mResolution.contains("720p")) {
                x_res = "1280";
                y_res = "720";
            } else if ("800p59hz".equals(mResolution)) {
                x_res = "1280";
                y_res = "800";
            } else if ("960p60hz".equals(mResolution)) {
                x_res = "1280";
                y_res = "960";
            } else if ("900p60hz".equals(mResolution)) {
                x_res = "1440";
                y_res = "900";
            } else if ("1024p60hz".equals(mResolution)) {
                x_res = "1280";
                y_res = "1024";
            } else if (mResolution.contains("1080")) {
                x_res = "1920";
                y_res = "1080";
            }

            writer.println("# setenv fb_x_res \"640\"");
            writer.println("# setenv fb_y_res \"480\"");
            writer.println("# setenv hdmi_phy_res \"480p60hz\"\n");

            writer.println("# setenv fb_x_res \"720\"");
            writer.println("# setenv fb_y_res \"489\"");
            writer.println("# setenv hdmi_phy_res \"480p59.94\"\n");

            writer.println("# setenv fb_x_res \"720\"");
            writer.println("# setenv fb_y_res \"576\"");
            writer.println("# setenv hdmi_phy_res \"576p50hz\"\n");

            writer.println("# setenv fb_x_res \"800\"");
            writer.println("# setenv fb_y_res \"480\"");
            writer.println("# setenv hdmi_phy_res \"800x480p60hz\"\n");

            writer.println("# setenv fb_x_res \"800\"");
            writer.println("# setenv fb_y_res \"600\"");
            writer.println("# setenv hdmi_phy_res \"600p60hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"720\"");
            writer.println("# setenv hdmi_phy_res \"720p50hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"720\"");
            writer.println("# setenv hdmi_phy_res \"720p60hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"768\"");
            writer.println("# setenv hdmi_phy_res \"768p60hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"800\"");
            writer.println("# setenv hdmi_phy_res \"800p59hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"960\"");
            writer.println("# setenv hdmi_phy_res \"960p60hz\"\n");

            writer.println("# setenv fb_x_res \"1440\"");
            writer.println("# setenv fb_y_res \"900\"");
            writer.println("# setenv hdmi_phy_res \"900p60hz\"\n");

            writer.println("# setenv fb_x_res \"1440\"");
            writer.println("# setenv fb_y_res \"900\"");
            writer.println("# setenv hdmi_phy_res \"900p60hz\"\n");

            writer.println("# setenv fb_x_res \"1280\"");
            writer.println("# setenv fb_y_res \"1024\"");
            writer.println("# setenv hdmi_phy_res \"1024p60hz\"\n");

            writer.println("# setenv fb_x_res \"1920\"");
            writer.println("# setenv fb_y_res \"1080\"");
            writer.println("# setenv hdmi_phy_res \"1080i50hz\"\n");

            writer.println("# setenv fb_x_res \"1920\"");
            writer.println("# setenv fb_y_res \"1080\"");
            writer.println("# setenv hdmi_phy_res \"1080i60hz\"\n");

            writer.println("# setenv fb_x_res \"1920\"");
            writer.println("# setenv fb_y_res \"1080\"");
            writer.println("# setenv hdmi_phy_res \"1080p30hz\"\n");

            writer.println("# setenv fb_x_res \"1920\"");
            writer.println("# setenv fb_y_res \"1080\"");
            writer.println("# setenv hdmi_phy_res \"1080p50hz\"\n");

            writer.println("# setenv fb_x_res \"1920\"");
            writer.println("# setenv fb_y_res \"1080\"");
            writer.println("# setenv hdmi_phy_res \"1080p60hz\"\n");

            writer.println("setenv fb_x_res \"" + x_res +"\"");
            writer.println("setenv fb_y_res \"" + y_res +"\"");
            writer.println("setenv hdmi_phy_res \"" + mResolution +"\"\n");

            writer.println("setenv edid \"0\"\n");
            writer.println("setenv hpd \"1\"\n");
            writer.println("setenv led_blink        \"1\"\n");
            writer.println("setenv bootcmd      \"movi read kernel 0 40008000;bootz 40008000\"\n");
            writer.println("setenv bootargs     \"fb_x_res=${fb_x_res} fb_y_res=${fb_y_res} hdmi_phy_res=${hdmi_phy_res} edid=${edid} hpd=${hpd} led_blink=${led_blink}\"");

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

    public native static String readFanMode();
    public native static void setFanMode(int auto_manual);
    public native static String readFanSpeeds();
    public native static void setFanSpeeds(String speeds);
    public native static String readPWMDuty();
    public native static void setPWMDuty(String duty);
    public native static String readPWMEnable();
    public native static void setPWMEnable(int enable);
    public native static String readTempLevels();
    public native static void setTempLevels(String temps);

    static {
        System.loadLibrary("fancontrol");
    }
}
