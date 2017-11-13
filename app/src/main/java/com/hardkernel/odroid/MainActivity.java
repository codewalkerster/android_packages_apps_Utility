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
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.view.Display;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private final static String TAG = "ODROIDUtility";

    public final static String WINDOW_AXIS = "/sys/class/graphics/fb0/window_axis";
    public final static String FREE_SCALE_AXIS = "/sys/class/graphics/fb0/free_scale_axis";
    public final static String FREE_SCALE = "/sys/class/graphics/fb0/free_scale";
    public final static String FREE_SCALE_VALUE = "0x10001";

    public final static String DISP_CAP = "/sys/devices/virtual/amhdmitx/amhdmitx0/disp_cap";

    //private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";
    private final static String BOOT_INI = "/storage/internal/boot.ini";

    private CheckBox mCBKodi;

    private String blueLed = "on";
    private CheckBox mCBBlueLed;

    private CheckBox mCBSelfAdaption;
    private CheckBox mCBCECSwitch;
    private CheckBox mCBOneKeyPlay;
    private CheckBox mCBAutoPowerOn;
    private CheckBox mCBAutoChangeLanguage;
    private CheckBox mCBOneKeyShutdown;

    private LinearLayout mLLOneKeyPlay;
    private LinearLayout mLLAutoChangeLanguage;
    private LinearLayout mLLAutoPowerOn;
    private LinearLayout mLLOneKeyShutdown;

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

    private static Context context;

    private static final String LATEST_VERSION = "latestupdate_marshmallow";
    private static final int FILE_SELECT_CODE = 0;

    private DownloadManager downloadManager;
    private long enqueue;

    private UpdatePackage m_updatePackage = null;

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
    private static final String SWITCH_ONE_KEY_SHUTDOWN = "switch_one_key_shutdown";

    //For start service
    private static final String CEC_ACTION = "CEC_LANGUAGE_AUTO_SWITCH";

    private static CpuActivity cpuActivity;

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

        Display display = getWindowManager().getDefaultDisplay();
        mDegree = display.getRotation() * 90;
        mOrientation = mDegree == 0 ? "landscape" : "portrait";

        cpuActivity = new CpuActivity(this, TAG);
        cpuActivity.onCreate();

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
                String line;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(BOOT_INI));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.startsWith("setenv bootargs"))
                        break;

                    if (line.startsWith("setenv hdmimode")) {
                        Log.e(TAG, line);
                    }

                    if (line.startsWith("setenv vout_mode")) {
                        Log.e(TAG, line);
                        String vout_mode = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                    }

                    if (line.startsWith("setenv led_onoff")) {
                        blueLed = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));

                        Log.e(TAG, "blue led : " + blueLed);
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

        mCBBlueLed = (CheckBox)findViewById(R.id.blue_led);
        mCBBlueLed.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                blueLed = isChecked? "on": "off";
                mCBBlueLed.setText(isChecked? R.string.on: R.string.off);
                modifyBootIni();
            }
        });

        mCBBlueLed.setChecked(blueLed.equals("on"));
        mCBBlueLed.setText(blueLed.equals("on")? R.string.on: R.string.off);

        mCBSelfAdaption = (CheckBox)findViewById(R.id.cb_selfadaption);
        mCBSelfAdaption.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                updateHDMISelfAdaption();
            }
        });

        mLLOneKeyPlay = (LinearLayout)findViewById(R.id.layout_one_key_play);
        mLLAutoChangeLanguage = (LinearLayout)findViewById(R.id.layout_auto_change_language);
        mLLAutoPowerOn = (LinearLayout)findViewById(R.id.layout_auto_power_on);
        mLLOneKeyShutdown= (LinearLayout)findViewById(R.id.layout_one_key_shutdown);

        mCBCECSwitch = (CheckBox)findViewById(R.id.cb_cecswitch);
        mCBCECSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBOneKeyPlay = (CheckBox)findViewById(R.id.cb_one_key_play);
        mCBOneKeyPlay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBAutoPowerOn = (CheckBox)findViewById(R.id.cb_auto_power_on);
        mCBAutoPowerOn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBAutoChangeLanguage = (CheckBox)findViewById(R.id.cb_auto_change_language);
        mCBAutoChangeLanguage.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBOneKeyShutdown = (CheckBox) findViewById(R.id.cb_one_key_shutdown);
        mCBOneKeyShutdown.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });

        Button btn;

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
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void modifyBootIni() {
        String vout_mode = "setenv vout_mode \"hdmi\"";
        String backlight_pwm = "setenv backlight_pwm \"no\"";

        String Blueled = "setenv led_onoff \"" + blueLed +"\"";

        List<String> lines = new ArrayList<String>();
        String line = null;

        try {
            File f1 = new File(BOOT_INI);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                if (line.startsWith("setenv vout_mode")) {
                    line = vout_mode;
                }

                if (line.startsWith("setenv led_onoff")) {
                    line = Blueled;
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

        Log.e(TAG, "Update boot.ini");
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

    public static void checkBootINI() {
        File boot_ini = new File(BOOT_INI);
        if (!boot_ini.exists()) {
            //SystemProperties.set("ctl.start", "makebootini");
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        cpuActivity.onResume();
        SharedPreferences pref = getSharedPreferences("utility", Context.MODE_PRIVATE);
        mCBKodi.setChecked(pref.getBoolean("kodi", false));

        if (mRadio_portrait.isChecked())
            mRG_degree.setVisibility(View.VISIBLE);
        else
            mRG_degree.setVisibility(View.GONE);

        updateHDMISelfAdaption();
    }

    private void updateHDMISelfAdaption() {
        if (mCBSelfAdaption.isChecked())
            mCBSelfAdaption.setText(R.string.on);
        else
            mCBSelfAdaption.setText(R.string.off);
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
    static boolean checkCustomServer = false;
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
                        TextView.BufferType.NORMAL);
                    checkCustomServer = false;
                    editText.setEnabled(false);
                }
            });

            final RadioButton rbMirrorServer =
                (RadioButton)promptView.findViewById(R.id.rb_mirror_server);

            rbMirrorServer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    editText.setText(UpdatePackage.MIRROR_SERVER_URL,
                        TextView.BufferType.NORMAL);
                    checkCustomServer = false;
                    editText.setEnabled(false);
                }
            });

            final RadioButton rbCustomServer =
                    (RadioButton) promptView.findViewById(R.id.rb_custom_server);

            rbCustomServer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                    editText.setText(pref.getString("custom_server",
                            UpdatePackage.MIRROR_SERVER_URL),
                            TextView.BufferType.EDITABLE);

                    checkCustomServer = true;
                    editText.setEnabled(true);
                }
            });

            if (checkCustomServer) {
                rbCustomServer.setChecked(true);
            } else {
                rbMirrorServer.setChecked(true);
                editText.setEnabled(false);
            }

            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String url = editText.getText().toString();
                            ServerInfo.write(url);
                            UpdatePackage.setRemoteUrl(url);

                            if (checkCustomServer) {
                                SharedPreferences pref = getSharedPreferences("utility", MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("custom_server", url);
                                editor.commit();
                            }
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
                Editor edit = pref.edit();
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
