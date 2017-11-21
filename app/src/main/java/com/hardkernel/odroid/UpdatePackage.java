package com.hardkernel.odroid;

import java.io.File;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

class UpdatePackage {
    private static final String TAG = "UpdatePackage";

    private static final String HEADER = "updatepackage";
    private static final String MODEL = "odroidn1";
    private static final String VARIANT = "eng";
    private static final String BRANCH = "rk3399_7.1.2_master";

    public static final long PACKAGE_MAXSIZE = 500 * 1024 * 1024;   /* 500MB */

    public final static String OFFICAL_SERVER_URL =
        "https://dn.odroid.com/RK3399/Android/ODROID-N1/";
    public final static String MIRROR_SERVER_URL =
        "https://www.odroid.in/mirror/dn.odroid.com/RK3399/Android/ODROID-N1/";

    private static String mRemoteUrl = MIRROR_SERVER_URL;
    private int m_buildNumber = -1;
    private long m_downloadId = -1;

    UpdatePackage(String packageName) {
        String[] s = packageName.split("-");
        if (s.length <= 4)
            return;

        if (!s[0].equals(HEADER) || !s[1].equals(MODEL) ||
                !s[2].equals(VARIANT) || !s[3].equals(BRANCH))
            return;

        setBuildNumber(Integer.parseInt(s[4].split("\\.")[0]));
    }

    UpdatePackage(int buildNumber) {
        setBuildNumber(buildNumber);
    }

    void setBuildNumber(int buildNumber) {
        Log.d(TAG, "Build Number is set as " + buildNumber);
        m_buildNumber = buildNumber;
    }

    public int buildNumber() {
        return m_buildNumber;
    }

    public String packageName() {
        if (m_buildNumber == -1)
            return null;

        return HEADER + "-" + MODEL + "-" + VARIANT + "-" + BRANCH + "-"
            + Integer.toString(m_buildNumber) + ".zip";
    }

    static File getDownloadDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    }

    public Uri localUri(Context context) {
        return Uri.parse("file://" + getDownloadDir(context) + "/update.zip");
    }

    static public String remoteUrl() {
        return mRemoteUrl;
    }

    static public void setRemoteUrl(String url) {
        mRemoteUrl = url;
    }

    public long downloadId() {
        return m_downloadId;
    }

    /*
     * Request to download update package if necessary
     */
    public long requestDownload(Context context, DownloadManager dm) {
        String name = packageName();
        if (name == null)
            return 0;

        Uri uri = Uri.parse(remoteUrl() + name);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Downloading new update package");
        request.setDescription(uri.getPath());
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationToSystemCache();
        request.setDestinationUri(localUri(context));

        Log.d(TAG, "Requesting to download " + uri.getPath() + " to " + localUri(context));

        /* Remove if the same file is exist */
        File file = new File(localUri(context).getPath());
        if (file.exists())
            file.delete();

        m_downloadId = dm.enqueue(request);

        return m_downloadId;
    }
};
