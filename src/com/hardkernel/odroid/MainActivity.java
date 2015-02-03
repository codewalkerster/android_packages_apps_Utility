package com.hardkernel.odroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

    public final static String WINDOW_AXIS = "/sys/class/graphics/fb0/window_axis";
    public final static String FREE_SCALE_AXIS = "/sys/class/graphics/fb0/free_scale_axis";
    public final static String FREE_SCALE = "/sys/class/graphics/fb0/free_scale";
    public final static String FREE_SCALE_VALUE = "0x10001";

    private Spinner mSpinnerGovernor;
    private String mGovernorString;

    private RadioButton mRadio_left;
    private RadioButton mRadio_right;

    private Spinner mSpinner_Resolution;
    private String mResolution = "720p";
    private static String mSystemResolution;

    private int mTopValue;
    private int mTopDelta = 0;
    private TextView mTextViewTopValue;
    private Button mBtnTopDecrease;
    private Button mBtnTopIncrease;

    private int mLeftValue;
    private int mLeftDelta = 0;
    private TextView mTextViewLeftValue;
    private Button mBtnLeftDecrease;
    private Button mBtnLeftIncrease;

    private int mRightValue;
    private int mRightDelta = 0;
    private TextView mTextViewRightValue;
    private Button mBtnRightIncrease;
    private Button mBtnRightDecrease;

    private int mBottomValue;
    private int mBottomDelta = 0;
    private TextView mTextViewBottomValue;
    private Button mBtnBottomIncrease;
    private Button mBtnBottomDecrease;

    private ArrayList<Button> mBtnOverScanList;

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

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

        SharedPreferences pref = getSharedPreferences("utility", Context.MODE_PRIVATE);

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
                if (line.contains("[hdmimode]")) {
                    mResolution = line.substring(line.lastIndexOf("[") + 1, line.length() - 1);
                    mSystemResolution = mResolution;
                    Log.e(TAG, mResolution);
                }
                if (line.contains("[overscan.top]")) {
                    mTopDelta = Integer.parseInt(line.substring(line.lastIndexOf("[") + 1, line.length() - 1));
                    Log.e(TAG, "top : " + mTopDelta);
                }
                if (line.contains("[overscan.left]")) {
                    mLeftDelta = Integer.parseInt(line.substring(line.lastIndexOf("[") + 1, line.length() - 1));
                    Log.e(TAG, "left : " + mLeftDelta);
                }
                if (line.contains("[overscan.right]")) {
                    mRightDelta = Integer.parseInt(line.substring(line.lastIndexOf("[") + 1, line.length() - 1));
                    Log.e(TAG, "right : " + mRightDelta);
                }
                if (line.contains("[overscan.bottom]")) {
                    mBottomDelta = Integer.parseInt(line.substring(line.lastIndexOf("[") + 1, line.length() - 1));
                    Log.e(TAG, "bottom : " + mBottomDelta);
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

        tabHost.addTab(tab1);
        //tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);

        mSpinnerGovernor = (Spinner) findViewById(R.id.spinner_governors);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> mAdapterGovenor = ArrayAdapter.createFromResource(this,
                R.array.governor_s805_array, android.R.layout.simple_spinner_item);
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

        mTextViewTopValue = (TextView)findViewById(R.id.tv_top);
        mTextViewTopValue.setText(Integer.toString(mTopDelta));
        mBtnTopDecrease = (Button)findViewById(R.id.btn_top_decrease);
        mBtnTopDecrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mTopDelta == 0)
                    return;
                mTopDelta--;
                if (!setOverScan())
                    mTopDelta++;
                mTextViewTopValue.setText(Integer.toString(mTopDelta));
            }
        });
        mBtnTopIncrease = (Button)findViewById(R.id.btn_top_increase);
        mBtnTopIncrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mTopDelta++;
                if (!setOverScan())
                    mTopDelta--;
                mTextViewTopValue.setText(Integer.toString(mTopDelta));
            }
        });

        mTextViewLeftValue = (TextView)findViewById(R.id.tv_left);
        mTextViewLeftValue.setText(Integer.toString(mLeftDelta));
        mBtnLeftDecrease = (Button)findViewById(R.id.btn_left_decrease);
        mBtnLeftDecrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mLeftDelta == 0)
                    return;
                mLeftDelta--;
                if (!setOverScan())
                    mLeftDelta++;
                mTextViewLeftValue.setText(Integer.toString(mLeftDelta));
            }
        });
        mBtnLeftIncrease = (Button)findViewById(R.id.btn_left_increase);
        mBtnLeftIncrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mLeftValue == mRightValue)
                    return;
                mLeftDelta++;
                if (!setOverScan())
                    mLeftDelta--;
                mTextViewLeftValue.setText(Integer.toString(mLeftDelta));
            }
        });

        mTextViewRightValue = (TextView)findViewById(R.id.tv_right);
        mTextViewRightValue.setText(Integer.toString(mRightDelta));
        mBtnRightIncrease = (Button)findViewById(R.id.btn_right_increase);
        mBtnRightIncrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mRightValue == mLeftValue)
                    return;
                mRightDelta++;
                if (!setOverScan())
                    mRightDelta--;
                mTextViewRightValue.setText(Integer.toString(mRightDelta));
            }
        });
        mBtnRightDecrease = (Button)findViewById(R.id.btn_right_decrease);
        mBtnRightDecrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mRightDelta == 0)
                    return;
                mRightDelta--;
                if (!setOverScan())
                    mRightDelta++;
                mTextViewRightValue.setText(Integer.toString(mRightDelta));
            }
        });

        mTextViewBottomValue = (TextView)findViewById(R.id.tv_bottom);
        mTextViewBottomValue.setText(Integer.toString(mBottomDelta));
        mBtnBottomIncrease = (Button)findViewById(R.id.btn_bottom_increase);
        mBtnBottomIncrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mBottomValue == mTopValue)
                    return;
                mBottomDelta++;
                if (!setOverScan())
                    mBottomDelta--;
                mTextViewBottomValue.setText(Integer.toString(mBottomDelta));
            }
        });
        mBtnBottomDecrease = (Button)findViewById(R.id.btn_bottom_decrease);
        mBtnBottomDecrease.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mBottomDelta == 0)
                    return;
                mBottomDelta--;
                if (!setOverScan())
                    mBottomDelta++;
                mTextViewBottomValue.setText(Integer.toString(mBottomDelta));
            }
        });

        mBtnOverScanList = new ArrayList<Button>();
        mBtnOverScanList.add(mBtnTopDecrease);
        mBtnOverScanList.add(mBtnTopIncrease);
        mBtnOverScanList.add(mBtnLeftDecrease);
        mBtnOverScanList.add(mBtnLeftIncrease);
        mBtnOverScanList.add(mBtnRightIncrease);
        mBtnOverScanList.add(mBtnRightDecrease);
        mBtnOverScanList.add(mBtnBottomIncrease);
        mBtnOverScanList.add(mBtnBottomDecrease);

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

                enableOverScanButtons(mSystemResolution.equals(mResolution) && mOrientation.equals("landscape"));

                Log.e(TAG, "Selected resolution = " + resolution);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        mRadio_left = (RadioButton)findViewById(R.id.radio_left);
        mRadio_right = (RadioButton)findViewById(R.id.radio_right);

        if (pref.getString("mouse", "right").equals("right"))
            mRadio_right.setChecked(true);
        else
            mRadio_left.setChecked(true);

        mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));

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

        btn = (Button)findViewById(R.id.button_apply_reboot);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.e(TAG, "mSystemResolution = " + mSystemResolution + ", mResolution = " + mResolution);
                if (!mSystemResolution.equals(mResolution)) {
                    setResolution(mResolution);
                    saveDelt2ubootenv(0, 0, 0, 0);
                } else
                    saveDelt2ubootenv(mLeftDelta, mTopDelta, mRightDelta, mBottomDelta);

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
                    if (mDegree == 0)
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                    else
                        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    android.provider.Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 1);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        saveDelt2ubootenv(mLeftDelta, mTopDelta, mRightDelta, mBottomDelta);
    }

    private void saveDelt2ubootenv(int left, int top, int right, int bottom) {
        try {
            Log.e(TAG, "saveDelt2ubootenv : " + top + " " + left + " " + right + " " + bottom);
            OutputStream stream;
            Process p = Runtime.getRuntime().exec("su");
            stream = p.getOutputStream();
            String cmd = "fw_setenv overscan_top " + top + "\n";
            stream.write(cmd.getBytes());
            stream.flush();
            cmd = "fw_setenv overscan_left " + left + "\n";
            stream.write(cmd.getBytes());
            stream.flush();
            cmd = "fw_setenv overscan_right " + right + "\n";
            stream.write(cmd.getBytes());
            stream.flush();
            cmd = "fw_setenv overscan_bottom " + bottom;
            stream.write(cmd.getBytes());
            stream.flush();
            stream.close();
        } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
    }

    private void enableOverScanButtons(boolean enable) {
        for (Button btn : mBtnOverScanList)
            btn.setEnabled(enable);
    }

    private void setOverScanRange() {
        String line = "";
        try {
            BufferedReader r = new BufferedReader(
                    new FileReader(FREE_SCALE_AXIS));
            line = r.readLine();
            r.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String[] axis = line.split(" ");
        mLeftValue = new Integer(axis[0]);
        mTopValue = new Integer(axis[1]);
        mRightValue = new Integer(axis[2]);
        mBottomValue = new Integer(axis[3]);
        Log.e(TAG, "left : " + mLeftValue
                + ", top : " + mTopValue
                + ", right : " + mRightValue
                + ", bottom : " + mBottomValue);
    }

    public boolean setOverScan() {
        try {
            String value = (mLeftValue + mLeftDelta) + " "
                                + (mTopValue + mTopDelta) + " "
                                + (mRightValue - mRightDelta) + " "
                                + (mBottomValue - mBottomDelta);

            Log.e(TAG, "echo " + value + " > " + WINDOW_AXIS);
            BufferedWriter window_axis_out = new BufferedWriter(new FileWriter(WINDOW_AXIS));
            window_axis_out.write(value);
            window_axis_out.close();
            BufferedWriter free_scale_out = new BufferedWriter(new FileWriter(FREE_SCALE));
            free_scale_out.write(FREE_SCALE_VALUE);
            free_scale_out.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Fail OverScan");
            enableOverScanButtons(false);
            return false;
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

    public static void setResolution(String resolution) {
        try {
            OutputStream stream;
            Process p = Runtime.getRuntime().exec("su");
            stream = p.getOutputStream();
            String cmd = "fw_setenv hdmimode " + resolution;
            stream.write(cmd.getBytes());
            stream.flush();
            stream.close();

            Log.e(TAG, "fw_setenv hdmimode " + resolution);
        } catch (Exception e) {
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

        setOverScanRange();

        enableOverScanButtons(mOrientation.equals("landscape"));
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
