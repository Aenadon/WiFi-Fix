package com.aenadon.wififix;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean disclaimerOk = prefs.getBoolean("disclaimerOk", false);
        if (!disclaimerOk) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.disclaimerTitle))
                    .setMessage(getString(R.string.disclaimer))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor pe = prefs.edit();
                            pe.putBoolean("disclaimerOk", true);
                            pe.apply();
                        }
                    }).setCancelable(false).show();
        }
    }

    public void rootMethod(View view) {
        final RepairMethod method;
        String title, message;

        switch (view.getId()) {
            case R.id.mDhcp:
                method = RepairMethod.DHCP;
                title = getString(R.string.mDhcp_title);
                message = getString(R.string.mDhcp_expl);
                break;
            case R.id.mWpaconf:
                method = RepairMethod.WPACONF;
                title = getString(R.string.mWpaconf_title);
                message = getString(R.string.mWpaconf_expl);
                break;
            default:
                return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new RootRepair(MainActivity.this).execute(method);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }
}