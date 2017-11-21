package com.hardkernel.odroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class UpdateActivity implements View.OnClickListener {
    private Context context;
    private String TAG;

    private EditText editText;
    private RadioButton rbOfficalServer;
    private RadioButton rbMirrorServer;
    private RadioButton rbCustomServer;

    private static boolean checkCustomServer = false;

    public UpdateActivity (Context context, String tag) {
        this.context = context;
        TAG = tag;
    }

    public void onCreate() {
        editText = (EditText) ((Activity)context).findViewById(R.id.edittext);

        rbOfficalServer = (RadioButton) ((Activity)context).findViewById(R.id.rb_offical_server);
        rbMirrorServer = (RadioButton) ((Activity)context).findViewById(R.id.rb_mirror_server);
        rbCustomServer = (RadioButton) ((Activity)context).findViewById(R.id.rb_custom_server);

        rbOfficalServer.setOnClickListener(this);
        rbMirrorServer.setOnClickListener(this);
        rbCustomServer.setOnClickListener(this);

        Button btn = (Button) ((Activity)context).findViewById(R.id.button_update_url);
        btn.setOnClickListener(this);

        SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
        checkCustomServer = pref.getBoolean("custom_server_rb", false);

        if (checkCustomServer) {
            rbCustomServer.setChecked(true);
            editText.setText(pref.getString("custom_server", UpdatePackage.remoteUrl()),
                    TextView.BufferType.EDITABLE);

        } else  {
            rbMirrorServer.setChecked(true);
            editText.setText(UpdatePackage.remoteUrl(), TextView.BufferType.EDITABLE);
            editText.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rb_offical_server: {
                checkCustomServer = false;

                editText.setText(UpdatePackage.OFFICAL_SERVER_URL,
                        TextView.BufferType.NORMAL);
                editText.setEnabled(false);
            }
                break;
            case R.id.rb_mirror_server: {
                checkCustomServer = false;

                editText.setText(UpdatePackage.MIRROR_SERVER_URL,
                        TextView.BufferType.NORMAL);
                editText.setEnabled(false);
            }
                break;
            case R.id.rb_custom_server: {
                checkCustomServer = true;

                SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
                editText.setText(pref.getString("custom_server",
                        UpdatePackage.MIRROR_SERVER_URL),
                        TextView.BufferType.EDITABLE);
                editText.setEnabled(true);
            }
                break;
            case R.id.button_update_url: {
                String url = editText.getText().toString();
                MainActivity.ServerInfo.write(url);
                UpdatePackage.setRemoteUrl(url);

                SharedPreferences pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();

                if (checkCustomServer) {
                    editor.putString("custom_server", url);
                    editor.putBoolean("custom_server_rb", true);
                } else {
                    editor.putBoolean("custom_server_rb", false);
                }
                editor.commit();
            }
                break;
            default:
                break;
        }
    }
}
