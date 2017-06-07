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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RecoverySystem;
import android.os.StatFs;
import android.os.ServiceManager;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
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
    private String mResolution = "720p";
    private static String mSystemResolution = "720p";

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

    private RadioButton mRadio_portrait;
    private RadioButton mRadio_landscape;

    private RadioButton mRadio_90;
    private RadioButton mRadio_270;

    private RadioGroup mRG_degree;

    private Spinner shortcut_f7;
    private Spinner shortcut_f8;
    private Spinner shortcut_f9;
    private Spinner shortcut_f10;

    private String mOrientation;
    private int mDegree;

    private CheckBox mCBEnableIR;
    private Button mBtnIRSave;
    private int mEnableIR;

    private static Context context;

    private static final String LATEST_VERSION = "latestupdate_kitkat";
    private static final int FILE_SELECT_CODE = 0;

    private DownloadManager downloadManager;
    private long enqueue;

    private UpdatePackage m_updatePackage = null;

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

        mRadio_left = (RadioButton)findViewById(R.id.radio_left);
        mRadio_right = (RadioButton)findViewById(R.id.radio_right);
        mCBEnableIR = (CheckBox)findViewById(R.id.cb_ir);

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
        TabSpec tab5 = tabHost.newTabSpec("IR Remote Control");
        TabSpec tab6 = tabHost.newTabSpec("Shortcut");

        tab1.setIndicator("CPU");
        tab1.setContent(R.id.tab1);
        tab2.setIndicator("Mouse");
        tab2.setContent(R.id.tab2);
        tab3.setIndicator("Screen");
        tab3.setContent(R.id.tab3);
        tab4.setIndicator("Rotation");
        tab4.setContent(R.id.tab4);
        tab5.setIndicator("IR remote control");
        tab5.setContent(R.id.tab5);
        tab6.setIndicator("Shortcut");
        tab6.setContent(R.id.tab6);

        tabHost.addTab(tab1);
        //tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);
        tabHost.addTab(tab5);
        tabHost.addTab(tab6);

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
                mScalingMaxFreq = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "scaling_max_freq = " + mScalingMaxFreq);
                setScalingMaxFreq(mScalingMaxFreq);
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

                    if (line.startsWith("setenv vout_mode")) {
                        Log.e(TAG, line);
                        String vout_mode = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                        if (vout_mode.equals("dvi")) {
                            if (mResolution.equals("800x480p60hz"))
                                mResolution = "ODROID-VU5/7";
                            else if (mResolution.equals("1024x600p60hz"))
                                mResolution = "ODROID-VU7 Plus";
                            else if (mResolution.equals("1024x768p60hz"))
                                mResolution = "ODROID-VU8";

                        }
                        Log.e(TAG, mResolution);
                    }

                    if (line.startsWith("setenv top")) {
                        mTopDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "top : " + mTopDelta);
                    }

                    if (line.startsWith("setenv left")) {
                        mLeftDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "left : " + mLeftDelta);
                    }

                    if (line.startsWith("setenv right")) {
                        mRightDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "right : " + mRightDelta);
                    }

                    if (line.startsWith("setenv bottom")) {
                        mBottomDelta = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "bottom : " + mBottomDelta);
                    }

                        Log.e(TAG, line);
                    if (line.startsWith("setenv ir_remote")) {
                        mEnableIR = Integer.parseInt(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                        Log.e(TAG, "mEnableIR " + mEnableIR);
                        if (mEnableIR == 1)
                            mCBEnableIR.setChecked(true);
                        else
                            mCBEnableIR.setChecked(false);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Not found boot.ini")
                .setMessage("Check and Format Internal FAT storage?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
                    }
                })
                .setNegativeButton("No", null).show();

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
                Log.e(TAG, "mResolution = " + mResolution);
                if (mResolution.equals(resolution))
                    return;
                else
                    mResolution = resolution;

                enableOverScanButtons(mSystemResolution.equals(mResolution) && mOrientation.equals("landscape"));

                Log.e(TAG, "Selected resolution = " + mResolution);
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
                    if (mAdapterResolution.getPosition("720p") >= 0) {
                        mResolution = "720p";
                        mSpinner_Resolution.setSelection(mAdapterResolution.getPosition(mResolution));
                    }
                }
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
                Log.e(TAG, "mSystemResolution = " + mSystemResolution + ", mResolution = " + mResolution);
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
                if (mRadio_portrait.isChecked()) {
                    if (mDegree == 90) {
                        SystemProperties.set("ctl.start", "rotation:portrait 90");
                    } else {
                        SystemProperties.set("ctl.start", "rotation:portrait 270");
                    }
                } else if (mRadio_landscape.isChecked()) {
                    SystemProperties.set("ctl.start", "rotation:landscape");
                }

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
            }
        });
        shortcutActivity();

        mBtnIRSave = (Button)findViewById(R.id.btn_ir_save);
        mBtnIRSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setIREnable(mCBEnableIR.isChecked());
            }
        });
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        modifyBootIni();
    }

    public void setIREnable(boolean enable) {
        List<String> lines = new ArrayList<String>();
        String line = null;

        try {
            File f1 = new File(BOOT_INI);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                if (line.startsWith("setenv ir_remote")) {
                    Log.e(TAG, line);
                    line = "setenv ir_remote \"" + (enable ? "1" : "0") + "\"";
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
        String vout_mode = "setenv vout_mode \"hdmi\"";
        String resolution = "setenv hdmimode \"720p\"           # 720p 1280x720";
        String backlight_pwm = "setenv backlight_pwm \"no\"";
        if (mResolution.equals("vga"))
            resolution = "setenv hdmimode \"vga\"            # 640x480";
        else if (mResolution.equals("480i"))
            resolution = "setenv hdmimode \"480i\"           # 720x480";
        else if (mResolution.equals("480p"))
            resolution = "setenv hdmimode \"480p\"           # 720x480";
        else if (mResolution.equals("576p"))
            resolution = "setenv hdmimode \"576p\"           # 720x576";
        else if (mResolution.equals("576i"))
            resolution = "setenv hdmimode \"576i\"           # 720x576";
        else if (mResolution.equals("800x480p60hz"))
            resolution = "setenv hdmimode \"800x480p60hz\"   # 800x480";
        else if (mResolution.equals("480x800p60hz")) {
            resolution = "setenv hdmimode \"480x800p60hz\"   # 480x800";
            vout_mode = "setenv vout_mode \"dvi\"";
        } else if (mResolution.equals("800x600p60hz"))
            resolution = "setenv hdmimode \"800x600p60hz\"   # 800x600";
        else if (mResolution.equals("1024x600p60hz"))
            resolution = "setenv hdmimode \"1024x600p60hz\"  # 1024x600";
        else if (mResolution.equals("1024x768p60hz"))
            resolution = "setenv hdmimode \"1024x768p60hz\"  # 1024x768";
        else if (mResolution.equals("1360x768p60hz"))
            resolution = "setenv hdmimode \"1360x768p60hz\"  # 1360x768";
        else if (mResolution.equals("1366x768p60hz"))
            resolution = "setenv hdmimode \"1366x768p60hz\"  # 1366x768";
        else if (mResolution.equals("1440x900p60hz"))
            resolution = "setenv hdmimode \"1440x900p60hz\"  # 1440x900";
        else if (mResolution.equals("1600x900p60hz"))
            resolution = "setenv hdmimode \"1600x900p60hz\"  # 1600x900";
        else if (mResolution.equals("1680x1050p60hz"))
            resolution = "setenv hdmimode \"1680x1050p60hz\" # 1680x1050";
        else if (mResolution.equals("720p"))
            resolution = "setenv hdmimode \"720p\"           # 720p 1280x720";
        else if (mResolution.equals("720p50hz"))
            resolution = "setenv hdmimode \"720p50hz\"           # 720p 1280x720";
        else if (mResolution.equals("800p"))
            resolution = "setenv hdmimode \"800p\"           # 1280x800";
        else if (mResolution.equals("sxga"))
            resolution = "setenv hdmimode \"sxga\"           # 1280x1024";
        else if (mResolution.equals("1080i50hz"))
            resolution = "setenv hdmimode \"1080i50hz\"      # 1080I@50Hz";
        else if (mResolution.equals("1080p24hz"))
            resolution = "setenv hdmimode \"1080p24hz\"      # 1080P@24Hz";
        else if (mResolution.equals("1080p50hz"))
            resolution = "setenv hdmimode \"1080p50hz\"      # 1080P@50Hz";
        else if (mResolution.equals("1080p"))
            resolution = "setenv hdmimode \"1080p\"          # 1080P@60Hz";
        else if (mResolution.equals("1080i"))
            resolution = "setenv hdmimode \"1080i\"          # 1080I@60Hz";
        else if (mResolution.equals("1920x1200"))
            resolution = "setenv hdmimode \"1920x1200\"      # 1920x1200";
        else if (mResolution.equals("ODROID-VU5/7")) {
            resolution = "setenv hdmimode \"800x480p60hz\"   # 800x480";
            vout_mode = "setenv vout_mode \"dvi\"";
            backlight_pwm = "setenv backlight_pwm \"yes\"";
        } else if (mResolution.equals("ODROID-VU7 Plus")) {
            resolution = "setenv hdmimode \"1024x600p60hz\"  # 1024x600";
            vout_mode = "setenv vout_mode \"dvi\"";
            backlight_pwm = "setenv backlight_pwm \"yes\"";
        } else if (mResolution.equals("ODROID-VU8")) {
            resolution = "setenv hdmimode \"1024x768p60hz\"  # 1024x768";
            vout_mode = "setenv vout_mode \"dvi\"";
            backlight_pwm = "setenv backlight_pwm \"invert\"";
        }

        String top, left, bottom, right;
        if (!mSystemResolution.equals(mResolution)) {
            top = "setenv top \"0\"";
            left = "setenv left \"0\"";
            bottom = "setenv bottom \"0\"";
            right = "setenv right \"0\"";
        } else {
            top = "setenv top \"" + mTopDelta + "\"";
            left = "setenv left \"" + mLeftDelta + "\"";
            bottom = "setenv bottom \"" + mBottomDelta + "\"";
            right = "setenv right \"" + mRightDelta + "\"";
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
                }

                if (line.startsWith("setenv max_freq")) {
                    int freq = Integer.parseInt(mScalingMaxFreq) / 1000;
                    line = "setenv max_freq \"" + freq + "\"";
                }

                if (line.startsWith("setenv vout_mode")) {
                    line = vout_mode;
                }

                if (line.startsWith("setenv top")) {
                    line = top;
                }

                if (line.startsWith("setenv left")) {
                    line = left;
                }

                if (line.startsWith("setenv bottom")) {
                    line = bottom;
                }

                if (line.startsWith("setenv right")) {
                    line = right;
                }

                if (line.startsWith("setenv backlight_pwm")) {
                    line = backlight_pwm;
                }

                Log.e(TAG, line);

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
        try {
            IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager
                    .getService(Context.POWER_SERVICE));
            pm.reboot(false, null, false);
        } catch (RemoteException e) {
            Log.e(TAG, "PowerManager service died!", e);
            return;
        }
    }

    public static void setMouse(String handed) {
        SystemProperties.set("mouse.firstbutton", handed);
        Log.e(TAG, "setprop mouse.firstbutton " + handed);
    }

    public static void checkBootINI() {
        File boot_ini = new File(BOOT_INI);
        if (!boot_ini.exists()) {
            SystemProperties.set("ctl.start", "makebootini");
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

            rbMirrorServer.setChecked(true);

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
        Log.e(TAG, "installPackage = " + packageFile.getPath());
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

        // Split at colon, use second item in the array
        String id = filePath.substring("/document/".length(), filePath.length());

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

    private static List<ApplicationInfo> appList = null;
    public static List<Intent> getAvailableAppList(Context context) {
        final PackageManager pm = context.getPackageManager();
        appList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<Intent> launchApps = new ArrayList<Intent>();
        for (ApplicationInfo appInfo: appList) {
            Intent launchApp = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchApp != null)
                launchApps.add(launchApp);
        }

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setPackage("home");
        launchApps.add(home);

        return launchApps;
    }

    private void shortcutActivity () {
        final SharedPreferences pref = getSharedPreferences("utility", Context.MODE_PRIVATE);
        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        String pkg_f7 = pref.getString("shortcut_f7", null);
        String pkg_f8 = pref.getString("shortcut_f8", null);
        String pkg_f9 = pref.getString("shortcut_f9", null);
        String pkg_f10 = pref.getString("shortcut_f10", null);

        shortcut_f7 = (Spinner) findViewById(R.id.shortcut_f7);
        shortcut_f8 = (Spinner) findViewById(R.id.shortcut_f8);
        shortcut_f9 = (Spinner) findViewById(R.id.shortcut_f9);
        shortcut_f10 = (Spinner) findViewById(R.id.shortcut_f10);

        final List<Intent> appIntentList = getAvailableAppList(context);
        final ArrayList<String> appTitles = new ArrayList<String>();

        appTitles.add("No shortcut");
        for(Intent intent: appIntentList) {
            appTitles.add(intent.getPackage());
        }

        ApplicationAdapter adapter = new ApplicationAdapter(this, R.layout.applist_dropdown_item_1line, appTitles, appList);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        shortcut_f7.setAdapter(adapter);
        shortcut_f8.setAdapter(adapter);
        shortcut_f9.setAdapter(adapter);
        shortcut_f10.setAdapter(adapter);

        shortcut_f7.setSelection(appTitles.indexOf(pkg_f7));
        shortcut_f8.setSelection(appTitles.indexOf(pkg_f8));
        shortcut_f9.setSelection(appTitles.indexOf(pkg_f9));
        shortcut_f10.setSelection(appTitles.indexOf(pkg_f10));

        OnItemSelectedListener listner = new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long arg3) {
                SharedPreferences.Editor edit = pref.edit();
                int keycode = 0;

                switch (spinner.getId()) {
                    case R.id.shortcut_f7:
                        keycode = KeyEvent.KEYCODE_F7;
                        break;
                    case R.id.shortcut_f8:
                        keycode = KeyEvent.KEYCODE_F8;
                        break;
                    case R.id.shortcut_f9:
                        keycode = KeyEvent.KEYCODE_F9;
                        break;
                    case R.id.shortcut_f10:
                        keycode = KeyEvent.KEYCODE_F10;
                        break;
                }

                String shortcut_pref =
                        "shortcut_f" + ((keycode - KeyEvent.KEYCODE_F1)  + 1);

                if (position == 0) {
                    wm.setApplicationShortcut(keycode, null);
                    edit.putString(shortcut_pref, "No shortcut");
                }
                else{
                    wm.setApplicationShortcut(keycode, appIntentList.get(position - 1));
                    edit.putString(shortcut_pref, appIntentList.get(position - 1).getPackage());
                }
                edit.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };

        shortcut_f7.setOnItemSelectedListener(listner);
        shortcut_f8.setOnItemSelectedListener(listner);
        shortcut_f9.setOnItemSelectedListener(listner);
        shortcut_f10.setOnItemSelectedListener(listner);

    }
}
