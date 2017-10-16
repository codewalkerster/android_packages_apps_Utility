package com.hardkernel.odroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class CpuActivity implements AdapterView.OnItemSelectedListener{
    private Context context;
    private String TAG;

    /* Big Cluster */
    private Spinner SpinnerBigGovernor;
    private String BigGovernor;

    private Spinner SpinnerBigFreq;
    private String BigScalingMaxFreq;

    /* Little Cluster */
    private Spinner SpinnerLittleGovernor;
    private String LittleGovernor;

    private Spinner SpinnerLittleFreq;
    private String LittleScalingMaxFreq;

    private CPU cpu;

    public CpuActivity (Context context, String tag) {
        this.context = context;
        TAG = tag;
    }

    public void onCreate() {
        String[] governor_array;
        ArrayAdapter<String> governorAdapter;

        String[] frequency_array;
        ArrayAdapter<String> freqAdapter;

        /* Big Cluster */
		/* Governor */
        cpu = CPU.getCPU(TAG, CPU.Cluster.Big);

        SpinnerBigGovernor = (Spinner) ((Activity)context).findViewById(R.id.spinner_big_governors);
        governor_array = cpu.governor.getGovernors();

        governorAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, governor_array);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerBigGovernor.setAdapter(governorAdapter);

        SpinnerBigGovernor.setOnItemSelectedListener(this);

        BigGovernor = cpu.governor.getCurrent();

        if (BigGovernor != null)
            SpinnerBigGovernor.setSelection(governorAdapter.getPosition(BigGovernor));

		/* Frequency */
        SpinnerBigFreq = (Spinner) ((Activity)context).findViewById(R.id.spinner_big_freq);

        frequency_array = cpu.frequency.getFrequencies();
        freqAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, frequency_array);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerBigFreq.setAdapter(freqAdapter);

        SpinnerBigFreq.setOnItemSelectedListener(this);

        BigScalingMaxFreq = cpu.frequency.getScalingCurrent();

        if (BigScalingMaxFreq != null)
            SpinnerBigFreq.setSelection(freqAdapter.getPosition(BigScalingMaxFreq));

        /* Little Cluster */
        cpu = CPU.getCPU(TAG, CPU.Cluster.Little);

        SpinnerLittleGovernor = (Spinner) ((Activity)context).findViewById(R.id.spinner_little_governors);
        governor_array = cpu.governor.getGovernors();

        governorAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, governor_array);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerLittleGovernor.setAdapter(governorAdapter);

        SpinnerLittleGovernor.setOnItemSelectedListener(this);

        LittleGovernor = cpu.governor.getCurrent();

        if (LittleGovernor != null)
            SpinnerLittleGovernor.setSelection(governorAdapter.getPosition(LittleGovernor));

		/* Frequency */
        SpinnerLittleFreq = (Spinner) ((Activity)context).findViewById(R.id.spinner_little_freq);

        frequency_array = cpu.frequency.getFrequencies();
        freqAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, frequency_array);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerLittleFreq.setAdapter(freqAdapter);

        SpinnerLittleFreq.setOnItemSelectedListener(this);

        LittleScalingMaxFreq = cpu.frequency.getScalingCurrent();

        if (LittleScalingMaxFreq != null)
            SpinnerLittleFreq.setSelection(freqAdapter.getPosition(LittleScalingMaxFreq));
    }

    public void onResume() {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String governor;
        String freq;

        SharedPreferences pref = context.getSharedPreferences("utility", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        switch (parent.getId()) {
            case R.id.spinner_big_governors:
                governor = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "governor = " + governor);

                cpu = CPU.getCPU(TAG, CPU.Cluster.Big);
                cpu.governor.set(governor);
                editor.putString("governor", governor);
                break;
            case R.id.spinner_little_governors:
                governor = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "governor = " + governor);

                cpu = CPU.getCPU(TAG, CPU.Cluster.Little);
                cpu.governor.set(governor);
                editor.putString("governor", governor);
                break;
            case R.id.spinner_big_freq:
                freq = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "freq");

                cpu = CPU.getCPU(TAG, CPU.Cluster.Big);
                cpu.frequency.setScalingMax(freq);
                editor.putString("freq", freq);
                break;
            case R.id.spinner_little_freq:
                freq = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "freq");

                cpu = CPU.getCPU(TAG, CPU.Cluster.Little);
                cpu.frequency.setScalingMax(freq);
                editor.putString("freq", freq);
                break;
            default:
                break;
        }
        editor.commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
