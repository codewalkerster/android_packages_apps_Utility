package com.hardkernel.odroid;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Governor {
    /* big Cluster */
    private final static String BIG_GOVERNOR_NODE = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor";
    private final static String BIG_SCALING_AVAILABLE_GOVERNORS = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors";
    /* little Cluster */
    private final static String LITTLE_GOVERNOR_NODE = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor";
    private final static String LITTLE_SCALING_AVAILABLE_GOVERNORS = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors";

    private static String TAG;
    private CPU.Cluster cluster;

    public Governor(String tag, CPU.Cluster cluster) {
        TAG = tag;
        this.cluster = cluster;
    }

    public String[] getGovernors() {
        String available_governors = getScaclingAvailable();
        return available_governors.split(" ");
    }

    public String getCurrent() {
        String governor = null;
        try {
            FileReader fileReader;

            switch (cluster) {
                case Big:
                    fileReader = new FileReader(BIG_GOVERNOR_NODE);
                    break;
                case Little:
                    fileReader = new FileReader(LITTLE_GOVERNOR_NODE);
                    break;
                default:
                    fileReader = new FileReader(BIG_GOVERNOR_NODE);
                    break;
            }

            BufferedReader bufferedReader = new BufferedReader(fileReader);
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

    public void set(String governor) {
        BufferedWriter out;
        FileWriter fileWriter;

        try {
            switch (cluster) {
                case Big:
                    fileWriter = new FileWriter(BIG_GOVERNOR_NODE);
                    break;
                case Little:
                    fileWriter = new FileWriter(LITTLE_GOVERNOR_NODE);
                    break;
                default:
                    fileWriter = new FileWriter(BIG_GOVERNOR_NODE);
                    break;
            }

            out = new BufferedWriter(fileWriter);
            out.write(governor);
            out.newLine();
            out.close();
            Log.e(TAG, "set governor : " + governor);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getScaclingAvailable() {
        String available_governors = null;
        try {
            FileReader fileReader;

            switch (cluster) {
                case Big:
                    fileReader = new FileReader(BIG_SCALING_AVAILABLE_GOVERNORS);
                    break;
                case Little:
                    fileReader = new FileReader(LITTLE_SCALING_AVAILABLE_GOVERNORS);
                    break;
                default:
                    fileReader = new FileReader(BIG_SCALING_AVAILABLE_GOVERNORS);
                    break;
            }

            BufferedReader bufferedReader= new BufferedReader(fileReader);
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
}
