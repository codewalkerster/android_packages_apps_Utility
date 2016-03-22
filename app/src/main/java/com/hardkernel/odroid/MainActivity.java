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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RecoverySystem;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import com.droidlogic.app.OutputModeManager;
import com.droidlogic.app.PlayBackManager;
import com.droidlogic.app.HdmiCecManager;

public class MainActivity extends Activity {

    private final static String TAG = "ODROIDUtility";
    public final static String GOVERNOR_NODE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public final static String SCALING_AVAILABLE_GOVERNORS = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";

    public final static String SCALING_AVAILABLE_FREQUESIES = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    public final static String SCALING_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";

    public final static String WINDOW_AXIS = "/sys/class/graphics/fb0/window_axis";
    public final static String FREE_SCALE_AXIS = "/sys/class/graphics/fb0/free_scale_axis";
    public final static String FREE_SCALE = "/sys/class/graphics/fb0/free_scale";
    public final static String FREE_SCALE_VALUE = "0x10001";

    public final static String DISP_CAP = "/sys/devices/virtual/amhdmitx/amhdmitx0/disp_cap";

    //private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";
    private final static String BOOT_INI = "/storage/internal/boot.ini";
    private Spinner mSpinnerGovernor;
    private String mGovernorString;

    private Spinner mSpinnerFreq;
    private String mScalingMaxFreq;

    private CheckBox mCBKodi;

    private RadioButton mRadio_left;
    private RadioButton mRadio_right;

    private Spinner mSpinner_Resolution;
    private String mResolution = "720p60hz";
    private String mPreviousResolution = "720p60hz";
    private static String mSystemResolution = "720p60hz";

    private Timer mTimer = null;

    private CheckBox mShowAllResolution;
    List<String> mAvableDispList = new ArrayList<String>();
    ArrayAdapter<CharSequence> mAdapterResolution = null;

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

    private CheckBox mCBSelfAdaption;
    private CheckBox mCBCECSwitch;
    private CheckBox mCBOneKeyPlay;
    private CheckBox mCBAutoPowerOn;
    private CheckBox mCBAutoChangeLanguage;

    private LinearLayout mLLOneKeyPlay;
    private LinearLayout mLLAutoChangeLanguage;

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

    private RadioGroup mRG_degree;

    private String mOrientation;
    private int mDegree;

    private Process mSu;

    private static Context context;

    private static final String LATEST_VERSION = "latestupdate";
    private static final int FILE_SELECT_CODE = 0;

    private DownloadManager downloadManager;
    private long enqueue;

    private UpdatePackage m_updatePackage = null;

    private OutputModeManager mOutputModeManager;
    private PlayBackManager mPlayBackManager;
    private HdmiCecManager mHdmiCecManager;

    private SharedPreferences mSharepreference = null;

    //For sharedPreferences
    private static final String PREFERENCE_BOX_SETTING = "preference_box_settings";
    private static final String SWITCH_ON = "true";
    private static final String SWITCH_OFF = "false";
    private static final String SWITCH_CEC = "switch_cec";
    private static final String SWITCH_ONE_KEY_PLAY = "switch_one_key_play";
    //private static final String SWITCH_ONE_KEY_POWER_OFF = "switch_one_key_power_off";
    private static final String SWITCH_AUTO_POWER_ON = "switch_auto_power_on";
    private static final String SWITCH_AUTO_CHANGE_LANGUAGE = "switch_auto_change_languace";

    //For start service
    private static final String CEC_SERVICE = "com.android.tv.settings.system.CecService";
    private static final String CEC_ACTION = "CEC_LANGUAGE_AUTO_SWITCH";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            if (id != enqueue) {
                Log.v(TAG, "Ingnoring unrelated download " + id);
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor cursor = downloadManager.query(query);

            if (!cursor.moveToFirst()) {
                Log.e(TAG, "Not able to move the cursor for downloaded content.");
                return;
            }

            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (DownloadManager.ERROR_INSUFFICIENT_SPACE == status) {
                Log.e(TAG, "Download is failed due to insufficient space");
                return;
            }
            if (DownloadManager.STATUS_SUCCESSFUL != status) {
                Log.e(TAG, "Download Failed");
                return;
            }

            /* Get URI of downloaded file */
            Uri uri = Uri.parse(cursor.getString(
                        cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));

            cursor.close();

            File file = new File(uri.getPath());
            if (!file.exists()) {
                Log.e(TAG, "Not able to find downloaded file: " + uri.getPath());
                return;
            }

            if (file.getName().equals(LATEST_VERSION)) {
                try {
                    StringBuilder text = new StringBuilder();

                    BufferedReader br = new BufferedReader(new FileReader(file));
                    text.append(br.readLine());
                    br.close();

                    m_updatePackage = new UpdatePackage(text.toString());

                    int currentVersion = 0;
                    String[] version = Build.VERSION.INCREMENTAL.split("-");
                    if (version.length < 4) {
                        Toast.makeText(context,
                                "Not able to detect the version number installed. "
                                + "Remote package will be installed anyway!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        currentVersion = Integer.parseInt(version[3]);
                    }

                    if (currentVersion < m_updatePackage.buildNumber()) {
                        updatePckageFromOnline();
                    } else if (currentVersion > m_updatePackage.buildNumber()) {
                        Toast.makeText(context,
                                "The current installed build number might be wrong",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context,
                                "Already latest Android image is installed.",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                }
            } else if (id == m_updatePackage.downloadId()) {
                /* Update package download is done, time to install */
                installPackage(new File(m_updatePackage.localUri(context).getPath()));
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        String url = ServerInfo.read();
        if (url == null)
            ServerInfo.write(UpdatePackage.remoteUrl());

        downloadManager = (DownloadManager)context.getSystemService(
                Context.DOWNLOAD_SERVICE);

        registerReceiver(mReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

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

        mOutputModeManager = new OutputModeManager(this);
        mPlayBackManager = new PlayBackManager(this);

        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabSpec tab1 = tabHost.newTabSpec("CPU");
        TabSpec tab2 = tabHost.newTabSpec("Mouse");
        TabSpec tab3 = tabHost.newTabSpec("Screen");
        TabSpec tab4 = tabHost.newTabSpec("Rotation");
        TabSpec tab5 = tabHost.newTabSpec("HDMI-CEC");

        tab1.setIndicator("CPU");
        tab1.setContent(R.id.tab1);
        tab2.setIndicator("Mouse");
        tab2.setContent(R.id.tab2);
        tab3.setIndicator("Screen");
        tab3.setContent(R.id.tab3);
        tab4.setIndicator("Rotation");
        tab4.setContent(R.id.tab4);
        tab5.setIndicator("HDMI-CEC");
        tab5.setContent(R.id.tab5);

        tabHost.addTab(tab1);
        //tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);
        tabHost.addTab(tab5);

        mSpinnerGovernor = (Spinner) findViewById(R.id.spinner_governors);
        String available_governors = getScaclingAvailableGovernor();
        String[] governor_array = available_governors.split(" ");
        ArrayAdapter<String> governorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, governor_array);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerGovernor.setAdapter(governorAdapter);

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

        if (mGovernorString != null)
            mSpinnerGovernor.setSelection(governorAdapter.getPosition(mGovernorString));

        mSpinnerFreq = (Spinner) findViewById(R.id.spinner_freq);

        String available_freq = getScaclingAvailableFrequensies();
        String[] freq_array = available_freq.split(" ");
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, freq_array);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFreq.setAdapter(freqAdapter);

        mSpinnerFreq.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                // TODO Auto-generated method stub
                String freq = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "freq");
                setScalingMaxFreq(freq);

                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("freq", freq);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        mScalingMaxFreq = getScaclingCurFreq();

        if (mScalingMaxFreq != null)
            mSpinnerFreq.setSelection(freqAdapter.getPosition(mScalingMaxFreq));

        mCBKodi = (CheckBox)findViewById(R.id.cb_kodi);
        mCBKodi.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("kodi", isChecked);
                editor.commit();
            }
        });

        File boot_ini = new File(BOOT_INI);
        if (boot_ini.exists()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(BOOT_INI));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.startsWith("setenv bootargs"))
                        break;

                    if (line.startsWith("setenv hdmimode")) {
                        Log.e(TAG, line);
                        mResolution = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                        mSystemResolution = mResolution;
                        Log.e(TAG, mResolution);
                    }

                    if (line.startsWith("setenv overscan_top")) {
                        mTopDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "top : " + mTopDelta);
                    }

                    if (line.startsWith("setenv overscan_left")) {
                        mLeftDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "left : " + mLeftDelta);
                    }

                    if (line.startsWith("setenv overscan_right")) {
                        mRightDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "right : " + mRightDelta);
                    }

                    if (line.startsWith("setenv overscan_bottom")) {
                        mBottomDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "bottom : " + mBottomDelta);
                    }
                }
                bufferedReader.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else {
            //default value
            Log.e(TAG, "Not found " + BOOT_INI);
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
        mShowAllResolution = (CheckBox)findViewById(R.id.cb_show_all);
        mShowAllResolution.setChecked(true);

        mAdapterResolution = fillResolutionTable(true, mAvableDispList, mSpinner_Resolution);

        mSpinner_Resolution.setAdapter(mAdapterResolution);

        mSpinner_Resolution.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                String resolution = arg0.getItemAtPosition(arg2).toString();
                Log.e(TAG, "Selected resolution = " + resolution);
                if (mResolution.equals(resolution))
                    return;
                else {
                    mPreviousResolution = mResolution;
                    mResolution = resolution;
                }

                enableOverScanButtons(mSystemResolution.equals(mResolution) && mOrientation.equals("landscape"));
                mOutputModeManager.setBestMode(mResolution);

                mTimer = new Timer();

                final AlertDialog.Builder dialog =
                    new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("Does the display look OK?")
                    .setMessage("The display will be reset to its previous configuration in few seconds.")
                    .setPositiveButton("Keep this configuration", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mTimer.cancel();
                            modifyBootIni();
                        }
                    });

                final AlertDialog alert = dialog.create();
                alert.show();

                mTimer.schedule(new TimerTask() {
                    public void run() {
                        mTimer.cancel();
                        mOutputModeManager.setBestMode(mPreviousResolution);
                        mResolution = mPreviousResolution;
                        Log.e(TAG, "Time over, set to = " + mResolution);
                        alert.dismiss();
                    }
                }, 10000);

                alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });

        mShowAllResolution.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAdapterResolution = fillResolutionTable(isChecked, mAvableDispList, mSpinner_Resolution);
                if (mAdapterResolution.getPosition(mResolution) >= 0)
                    mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));
                else {
                    if (mAdapterResolution.getPosition("720p60hz") >= 0) {
                        mResolution = "720p60hz";
                        mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));
                        mOutputModeManager.setBestMode(mResolution);
                    }
                }
            }
        });

        mCBSelfAdaption = (CheckBox)findViewById(R.id.cb_selfadaption);
        mCBSelfAdaption.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                mPlayBackManager.setHdmiSelfadaption(isChecked);
                updateHDMISelfAdaption();
            }
        });

        mLLOneKeyPlay = (LinearLayout)findViewById(R.id.layout_one_key_play);

        mLLAutoChangeLanguage = (LinearLayout)findViewById(R.id.layout_auto_change_language);

        mCBCECSwitch = (CheckBox)findViewById(R.id.cb_cecswitch);
        mCBCECSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                switchCec(isChecked);
            }
        });

        mCBOneKeyPlay = (CheckBox)findViewById(R.id.cb_one_key_play);
        mCBOneKeyPlay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                switchOneKeyPlay(isChecked);
            }
        });

        mCBAutoPowerOn = (CheckBox)findViewById(R.id.cb_auto_power_on);
        mCBAutoPowerOn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                switchAutoPowerOn(isChecked);
            }
        });

        mCBAutoChangeLanguage = (CheckBox)findViewById(R.id.cb_auto_change_language);
        mCBAutoChangeLanguage.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                switchAutoChangeLanguage(isChecked);
            }
        });

        mRadio_left = (RadioButton)findViewById(R.id.radio_left);
        mRadio_right = (RadioButton)findViewById(R.id.radio_right);

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
                modifyBootIni();
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
                    stdin.writeBytes("mount -o rw,remount /\n");

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
            }

        });

        initCecFun();
    }

    private boolean isCecServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CEC_SERVICE.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void initCecFun(){
        Log.e(TAG, "initCecFun()");
        mHdmiCecManager = new HdmiCecManager(this);
        mSharepreference = getSharedPreferences(PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE);

        Editor editor = this.getSharedPreferences(PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE).edit();
        String str = mHdmiCecManager.getCurConfig();
        Log.e(TAG, "cec config = " + str);
        if (!mHdmiCecManager.remoteSupportCec()) {
            switchCec(false);
            mCBCECSwitch.setChecked(false);
            mCBCECSwitch.setEnabled(false);
            mLLOneKeyPlay.setVisibility(View.GONE);
            mLLAutoChangeLanguage.setVisibility(View.GONE);
            return;
        }

        // get rid of '0x' prefix
        int cec_config = Integer.valueOf(str.substring(2, str.length()), 16);
        Log.d(TAG, "cec config str:" + str + ", value:" + cec_config);
        if ((cec_config & HdmiCecManager.MASK_FUN_CEC) != 0) {
            if ((cec_config & HdmiCecManager.MASK_ONE_KEY_PLAY) != 0) {
                editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_ON);
                mCBOneKeyPlay.setChecked(true);
            } else {
                editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_OFF);
                mCBOneKeyPlay.setChecked(false);
            }
            /*
            if ((cec_config & HdmiCecManager.MASK_ONE_KEY_STANDBY) != 0) {
                editor.putString(SWITCH_ONE_KEY_POWER_OFF, SWITCH_ON);
            } else {
                editor.putString(SWITCH_ONE_KEY_POWER_OFF, SWITCH_OFF);
            }
            */
            if ((cec_config & HdmiCecManager.MASK_AUTO_POWER_ON) != 0) {
                editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_ON);
                mCBAutoPowerOn.setChecked(true);
            } else {
                editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_OFF);
                mCBAutoPowerOn.setChecked(false);
            }
            if ((cec_config & HdmiCecManager.MASK_AUTO_CHANGE_LANGUAGE) != 0) {
                editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_ON);
                mCBAutoChangeLanguage.setChecked(true);
            } else {
                editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
                mCBAutoChangeLanguage.setChecked(false);
            }
            editor.putString(SWITCH_CEC, SWITCH_ON);
            mCBCECSwitch.setChecked(true);
            mLLOneKeyPlay.setVisibility(View.VISIBLE);
            mLLAutoChangeLanguage.setVisibility(View.VISIBLE);
        } else {
            editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_OFF);
            //editor.putString(SWITCH_ONE_KEY_POWER_OFF, SWITCH_OFF);
            editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_OFF);
            editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
            editor.putString(SWITCH_CEC, SWITCH_OFF);
            mCBCECSwitch.setChecked(false);
            mLLOneKeyPlay.setVisibility(View.GONE);
            mLLAutoChangeLanguage.setVisibility(View.GONE);
        }
        editor.commit();
        mHdmiCecManager.setCecEnv(cec_config);
    }

    private void switchCec(boolean on) {
        String isOpen = mSharepreference.getString(SWITCH_CEC, SWITCH_OFF);
        Log.d(TAG, "switch CEC, on:" + on + ", isOpen:" + isOpen);
        Editor editor = this.getSharedPreferences(PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE).edit();
        if (isOpen.equals(SWITCH_ON) && !on) {
            editor.putString(SWITCH_CEC, SWITCH_OFF);
            editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_OFF);
            //editor.putString(SWITCH_ONE_KEY_POWER_OFF, SWITCH_OFF);
            editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_OFF);
            editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_CEC, HdmiCecManager.FUN_CLOSE);
        } else if (isOpen.equals(SWITCH_OFF) && on) {
            editor.putString(SWITCH_CEC, SWITCH_ON);
            editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_ON);
            //editor.putString(SWITCH_ONE_KEY_POWER_OFF, SWITCH_ON);
            editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_ON);
            editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_ON);
            editor.commit();
            if (!isCecServiceRunning()) {
                Intent serviceIntent = new Intent();
                serviceIntent.setAction(CEC_ACTION);
                this.startService(serviceIntent);
            }
            mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_CEC, HdmiCecManager.FUN_OPEN);
            updateCecLanguage();
        }
        if (on) {
            mCBCECSwitch.setText(R.string.on);
            mLLOneKeyPlay.setVisibility(View.VISIBLE);
            mLLAutoChangeLanguage.setVisibility(View.VISIBLE);
        } else {
            mCBCECSwitch.setText(R.string.off);
            mLLOneKeyPlay.setVisibility(View.GONE);
            mLLAutoChangeLanguage.setVisibility(View.GONE);
        }
    }

    private void switchOneKeyPlay(boolean on) {
        String isOpen = mSharepreference.getString(SWITCH_ONE_KEY_PLAY, SWITCH_OFF);
        Editor editor = this.getSharedPreferences(PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE).edit();
        if (isOpen.equals(SWITCH_ON) && !on) {
            editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_OFF);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_ONE_KEY_PLAY, HdmiCecManager.FUN_CLOSE);
        } else if (isOpen.equals(SWITCH_OFF) && on) {
            editor.putString(SWITCH_ONE_KEY_PLAY, SWITCH_ON);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_ONE_KEY_PLAY, HdmiCecManager.FUN_OPEN);
        }
        if (on)
            mCBOneKeyPlay.setText(R.string.on);
        else
            mCBOneKeyPlay.setText(R.string.off);
    }

    private void switchAutoPowerOn(boolean on) {
        String isOpen = mSharepreference.getString(SWITCH_AUTO_POWER_ON, SWITCH_OFF);
        Editor editor = this.getSharedPreferences(
                PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE).edit();
        if (isOpen.equals(SWITCH_ON) && !on) {
            editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_OFF);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(
                    HdmiCecManager.FUN_AUTO_POWER_ON, HdmiCecManager.FUN_CLOSE);
        } else if (isOpen.equals(SWITCH_OFF) && on) {
            editor.putString(SWITCH_AUTO_POWER_ON, SWITCH_ON);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(
                    HdmiCecManager.FUN_AUTO_POWER_ON, HdmiCecManager.FUN_OPEN);
        }
        if (on)
            mCBAutoPowerOn.setText(R.string.on);
        else
            mCBAutoPowerOn.setText(R.string.off);
    }

    private void switchAutoChangeLanguage(boolean on) {
        String isOpen = mSharepreference.getString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
        Editor editor = this.getSharedPreferences(
                PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE).edit();
        if (isOpen.equals(SWITCH_ON) && !on) {
            editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
            editor.commit();
            mHdmiCecManager.setCecSysfsValue(
                    HdmiCecManager.FUN_AUTO_CHANGE_LANGUAGE, HdmiCecManager.FUN_CLOSE);
        } else if (isOpen.equals(SWITCH_OFF) && on) {
            editor.putString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_ON);
            editor.commit();
            if (!isCecServiceRunning()) {
                Intent serviceIntent = new Intent();
                serviceIntent.setAction(CEC_ACTION);
                this.startService(serviceIntent);
            }
            mHdmiCecManager.setCecSysfsValue(
                    HdmiCecManager.FUN_AUTO_CHANGE_LANGUAGE, HdmiCecManager.FUN_OPEN);
            updateCecLanguage();
        }
        if (on)
            mCBAutoChangeLanguage.setText(R.string.on);
        else
            mCBAutoChangeLanguage.setText(R.string.off);
    }

    private void updateCecLanguage(){
        String curLanguage = mHdmiCecManager.getCurLanguage();
        Log.d(TAG,"update curLanguage:" + curLanguage);
        if (curLanguage == null)
            return;

        String[] cec_language_list = getResources().getStringArray(R.array.cec_language);
        String[] language_list = getResources().getStringArray(R.array.language);
        String[] country_list = getResources().getStringArray(R.array.country);
        mHdmiCecManager.setLanguageList(cec_language_list, language_list, country_list);
        mHdmiCecManager.doUpdateCECLanguage(curLanguage);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private ArrayAdapter<CharSequence> fillResolutionTable(boolean all, List<String> list, Spinner spinner) {
        list.clear();
        ArrayAdapter<CharSequence> adapter = null;
        if (all) {
            adapter = ArrayAdapter.createFromResource(this,
            R.array.resolution_array, android.R.layout.simple_spinner_dropdown_item);
        } else {
            File disp_cap = new File(DISP_CAP);
            try {
                FileReader fr = new FileReader(disp_cap);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.equals("null edid")) {
                        Log.e(TAG, line);
                        if (line.indexOf('*') != -1)
                            line = line.substring(0, line.indexOf('*'));
                        list.add(line);
                    }
                }
                fr.close();
                br.close();
            } catch (Exception e) {
            }

            adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                    list);
        }

        spinner.setAdapter(adapter);
        return adapter;
    }

    public void modifyBootIni() {
        String resolution = "setenv hdmimode \"" + mResolution + "\"";
        String top, left, bottom, right;

        if (!mSystemResolution.equals(mResolution)) {
            top = "setenv overscan_top \"0\"";
            left = "setenv overscan_left \"0\"";
            bottom = "setenv overscan_bottom \"0\"";
            right = "setenv overscan_right \"0\"";
        } else {
            top = "setenv overscan_top \"" + mTopDelta + "\"";
            left = "setenv overscan_left \"" + mLeftDelta + "\"";
            bottom = "setenv overscan_bottom \"" + mBottomDelta + "\"";
            right = "setenv overscan_right \"" + mRightDelta + "\"";
        }

        List<String> lines = new ArrayList<String>();
        String line = null;

        try {
            File f1 = new File(BOOT_INI);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                if (line.startsWith("setenv hdmimode")) {
                    line = resolution;
                    Log.e(TAG, line);
                }

                if (line.startsWith("setenv overscan_top")) {
                    line = top;
                    Log.e(TAG, line);
                }

                if (line.startsWith("setenv overscan_left")) {
                    line = left;
                    Log.e(TAG, line);
                }

                if (line.startsWith("setenv overscan_bottom")) {
                    line = bottom;
                    Log.e(TAG, line);
                }

                if (line.startsWith("setenv overscan_right")) {
                    line = right;
                    Log.e(TAG, line);
                }

                lines.add(line + "\n");
            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);
            for(String s : lines)
                out.write(s);
            out.flush();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Log.e(TAG, "Update boot.ini");
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

    public static void checkBootINI() {
        File boot_ini = new File(BOOT_INI);
        if (!boot_ini.exists()) {
            try {
                OutputStream stream;
                Process p = Runtime.getRuntime().exec("su");
                stream = p.getOutputStream();
                String cmd =  "cp /system/etc/boot.ini.template " + BOOT_INI;
                stream.write(cmd.getBytes());
                stream.flush();
                stream.close();
                Log.e(TAG, cmd);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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

    public static void setScalingMaxFreq(String freq) {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(SCALING_MAX_FREQ));
            out.write(freq);
            out.newLine();
            out.close();
            Log.e(TAG, "set freq : " + freq);
        } catch (IOException e) {
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
        SharedPreferences pref = getSharedPreferences("utility", Context.MODE_PRIVATE);
        mCBKodi.setChecked(pref.getBoolean("kodi", false));

        if (pref.getString("mouse", "right").equals("right"))
            mRadio_right.setChecked(true);
        else
            mRadio_left.setChecked(true);

        if (mRadio_portrait.isChecked())
            mRG_degree.setVisibility(View.VISIBLE);

        updateHDMISelfAdaption();
    }

    private void updateHDMISelfAdaption() {
        mCBSelfAdaption.setChecked(mPlayBackManager.isHdmiSelfadaptionOn());
        if (mCBSelfAdaption.isChecked())
            mCBSelfAdaption.setText(R.string.on);
        else
            mCBSelfAdaption.setText(R.string.off);
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

    protected String getScaclingCurFreq() {
        String freq = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(SCALING_MAX_FREQ));
            freq = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, freq);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return freq;
    }

    protected String getScaclingAvailableGovernor() {
        String available_governors = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(SCALING_AVAILABLE_GOVERNORS));
            available_governors = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, available_governors);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return available_governors;
    }


    protected String getScaclingAvailableFrequensies() {
        String available_freq = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(SCALING_AVAILABLE_FREQUESIES));
            available_freq = bufferedReader.readLine();
            bufferedReader.close();
            Log.e(TAG, available_freq);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return available_freq;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.server_setting:
                editServerUrl();
                return true;

            case R.id.check_online_update:
                checkLatestVersion();
                return true;

            case R.id.update_from_file:
                updatePackageFromStorage();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static class ServerInfo {
        private static File file;
        private static final String FILENAME = "server.cfg";

        static {
            file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    FILENAME);

            if (!file.exists()) {
                try {
                    file.createNewFile();
                    write(UpdatePackage.remoteUrl());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        static public String read() {
            String text = null;

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                text = br.readLine();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return text;
        }

        static void write(String url) {
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(url);
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void editServerUrl() {
            // get prompts.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View promptView = layoutInflater.inflate(R.layout.url_dialog, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText editText = (EditText)promptView.findViewById(R.id.edittext);
            editText.setText(UpdatePackage.remoteUrl(), TextView.BufferType.EDITABLE);

            final RadioButton rbOfficalServer =
                (RadioButton)promptView.findViewById(R.id.rb_offical_server);

            rbOfficalServer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    editText.setText(UpdatePackage.OFFICAL_SERVER_URL,
                        TextView.BufferType.EDITABLE);
                }
            });

            rbOfficalServer.setChecked(true);

            final RadioButton rbMirrorServer =
                (RadioButton)promptView.findViewById(R.id.rb_mirror_server);

            rbMirrorServer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    editText.setText(UpdatePackage.MIRROR_SERVER_URL,
                        TextView.BufferType.EDITABLE);
                }
            });

            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ServerInfo.write(editText.getText().toString());
                            UpdatePackage.setRemoteUrl(editText.getText().toString());
                        }
                    })
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
    }

    /*
     * Request to retrive the latest update package version
     */
    public void checkLatestVersion() {
        String remote = UpdatePackage.remoteUrl();

        /* Remove if the same file is exist */
        new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                LATEST_VERSION).delete();

        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(remote + LATEST_VERSION));
        request.setVisibleInDownloadsUi(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setDestinationInExternalFilesDir(context,
                Environment.DIRECTORY_DOWNLOADS,
                LATEST_VERSION);

        enqueue = downloadManager.enqueue(request);
    }

    public void updatePckageFromOnline() {
        new AlertDialog.Builder(this)
            .setTitle("New update package is found!")
            .setMessage("Do you want to download new update package?\n"
                    + "It would take a few minutes or hours depends on your network speed.")
            .setPositiveButton("Download",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                            int whichButton) {
                            if (sufficientSpace()) {
                                enqueue = m_updatePackage.requestDownload(context,
                                    downloadManager);
                            }
                        }
                    })
            .setCancelable(true)
            .create().show();
    }

    public void updatePackageFromStorage() {
        // TODO Auto-generated method stub
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);

        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Update"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(context,
                    "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void installPackage(final File packageFile) {
        try {
            RecoverySystem.verifyPackage(packageFile, null, null);

            new AlertDialog.Builder(this)
                .setTitle("Selected package file is verified")
                .setMessage("Your Android can be updated, do you want to proceed?")
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            RecoverySystem.installPackage(context,
                                packageFile);
                        } catch (Exception e) {
                            Toast.makeText(context,
                                "Error while install OTA package: " + e,
                                Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setCancelable(true)
                .create().show();
        } catch (Exception e) {
            Toast.makeText(context,
                    "The package file seems to be corrupted!!\n" +
                    "Please select another package file...",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean sufficientSpace() {
        StatFs stat = new StatFs(UpdatePackage.getDownloadDir(context).getPath());

        double available = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();

        if (available < UpdatePackage.PACKAGE_MAXSIZE) {
            new AlertDialog.Builder(this)
                .setTitle("Check free space")
                .setMessage("Insufficient free space!\nAbout " +
                        UpdatePackage.PACKAGE_MAXSIZE / 1024 / 1024 +
                        " MBytes free space is required.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String path = getRealPathFromURI(uri);
                    if (path == null)
                        return;
                    installPackage(new File(path));
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = "";
        filePath = uri.getPath();
        Log.e(TAG, "uri.getPath() = " + filePath);
        if (filePath.startsWith("/storage"))
            return filePath;

        String wholeID = DocumentsContract.getDocumentId(uri);

        if (!wholeID.contains(":"))
            return filePath;

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        Log.e(TAG, "id = " + id);

        String[] column = { MediaStore.Files.FileColumns.DATA };

        // where id is equal to
        String sel = MediaStore.Files.FileColumns.DATA + " LIKE '%" + id + "%'";

        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"),
                column, sel, null, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

}
