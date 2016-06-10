package com.ayla.marvell;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import android.os.Handler;

import com.ayla.marvell.provisioning.FinishPairingTask;
import com.ayla.marvell.provisioning.PairingTask;

public class MainActivity extends Activity {

    private EditText wifiNetworkName;
    private EditText wifiPassword;
    private EditText devicePin;
    private int wifiSecurity;
    private RadioButton noSecurity;
    private RadioButton wepSecurity;
    private RadioButton wpaSecurity;
    private RadioButton wapMixedSecurity;
    public static MainActivity instance;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiNetworkName = (EditText) findViewById(R.id.networkName);
        wifiPassword = (EditText) findViewById(R.id.networkPassword);
        devicePin = (EditText) findViewById(R.id.accessoryPin);

        noSecurity = (RadioButton) findViewById(R.id.radioButton);
        wepSecurity = (RadioButton) findViewById(R.id.radioButton2);
        wpaSecurity = (RadioButton) findViewById(R.id.radioButton3);
        wapMixedSecurity = (RadioButton) findViewById(R.id.radioButton4);

        Button button = (Button) findViewById(R.id.provisionButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean complete = true;
                if(wifiNetworkName.getText() == null || wifiNetworkName.getText().length() < 1) {
                    complete = false;
                }
                if(wifiPassword.getText() == null || wifiPassword.getText().length() < 1) {
                    complete = false;
                }
                if(devicePin.getText() == null || devicePin.getText().length() < 1) {
                    complete = false;
                }
                if(!(noSecurity.isChecked() || wepSecurity.isChecked() || wpaSecurity.isChecked() || wapMixedSecurity.isChecked())) {
                    complete = false;
                }
                if(complete) {
                    System.out.println("Marvell button complete");
                    showProgressDialog();
                    int security = 0;
                    if(noSecurity.isChecked())
                        security = 0;
                    if(wepSecurity.isChecked())
                        security = 1;
                    if(wpaSecurity.isChecked())
                        security = 4;
                    if(wapMixedSecurity.isChecked())
                        security = 5;
                    provisionAccessory(wifiNetworkName.getText().toString(), wifiPassword.getText().toString(), security, devicePin.getText().toString());
                } else {
                    provisionAccessory("Ayla Guest", "@yla607$", 5, "hkAIQImA");
                    //Toast.makeText(instance.getApplicationContext(), "Incomplete fields. Please fill and select all fields", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void provisionAccessory(String wifiName, String wifiPassword, int wifiSecurity, String devicePin) {
        final PairingTask task = new PairingTask();
        task.setNetworkName(wifiName);
        task.setNetworkPassword(wifiPassword);
        task.setNetworkType(wifiSecurity);
        task.setDevicePin(devicePin);
        task.handler = new PairingTask.ProvisioningHandler() {
            @Override
            public void error(final String message) {
                instance.cancelProgressDialog();
                instance.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(instance.getBaseContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
                //Toast.makeText(instance.getBaseContext(), message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void success(String message) {
                instance.cancelProgressDialog();
                instance.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(instance.getApplicationContext(), "Accessory provisioned part 1!", Toast.LENGTH_LONG).show();
                        final FinishPairingTask task2 = task.getFinishPairingTask();
                        task2.handler = new PairingTask.ProvisioningHandler() {
                            @Override
                            public void error(String message) {

                            }

                            @Override
                            public void success(String message) {

                            }
                        };
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                task2.execute("");
                            }
                        }, 20000);

                    }
                });
            }
        };
        task.execute("");

    }

    private void errorToast() {

    }

    private void showProgressDialog() {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Provisioning...");
        mDialog.setCancelable(false);
        mDialog.show();
    }

    private void cancelProgressDialog() {
        if(mDialog != null)
            mDialog.dismiss();
    }
}
