package com.aenadon.wififix;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Context ctx; // Context used in "onPostExecute"
    WifiManager wifiManager; // used for disabling WiFi before the process
    private final int DHCP_METHOD = 1;
    private final int WPACONF_METHOD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;
        wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        // Acknowledge Disclaimer. If already acknowledged, don't show again
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean disclaimerOk = p.getBoolean("disclaimerOk", false);
        if (!disclaimerOk) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.disclaimerTitle))
                    .setMessage(getString(R.string.disclaimer))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor pe = p.edit();
                            pe.putBoolean("disclaimerOk", true);
                            pe.apply();
                        }
                    }).setCancelable(false).show();
        }
    }

    public void rootMethod(View view) {
        final int method;
        String title, message;

        switch (view.getId()) {
            case R.id.mDhcp:
                method = DHCP_METHOD;
                title = getString(R.string.mDhcp_title);
                message = getString(R.string.mDhcp_expl);
                break;
            case R.id.mWpaconf:
                method = WPACONF_METHOD;
                title = getString(R.string.mWpaconf_title);
                message = getString(R.string.mWpaconf_expl);
                break;

            default:
                return; // no other buttons!
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new RootRepair().execute(method); // if OK, execute the chosen method
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }

    private class RootRepair extends AsyncTask<Integer, String, Integer> {
        // some magic numbers
        private final int NOROOT = 0;
        private final int SUCCESS = 1;
        private final int OTHER = 2;
        private ProgressDialog dialog; // tells user to wait

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setTitle("");
            dialog.setMessage(getString(R.string.working));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            wifiManager.setWifiEnabled(false); // Disable WiFi if enabled
            try {
                int method = params[0]; // given parameter
                int res;
                switch (method) { // check for method
                    case DHCP_METHOD:
                        res = Runtime.getRuntime().exec(new String[]{"su", "-c", "rm -f /data/misc/dhcp/dhcpcd-wlan0.lease",
                                        "rm -f /data/misc/dhcp/dhcpcd-wlan0.pid"}).waitFor(); // delete the DHCP files
                        break;
                    case WPACONF_METHOD:
                        res = Runtime.getRuntime().exec(new String[]{"su", "-c", "rm -f /data/misc/wifi/wpa_supplicant.conf"}).waitFor(); // delete wpa_supplicant.conf
                        break;
                    default:
                        return OTHER;
                }
                if (res != 0) return NOROOT; // No-Root-Error!

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return OTHER; // some other error (? just in case)
            }
            return SUCCESS; // success!
        }

        @Override
        protected void onPostExecute(Integer result) {
            String title, message;
            dialog.dismiss();
            switch (result) {
                case NOROOT:
                    title = ctx.getString(R.string.fail_title);
                    message = ctx.getString(R.string.fail_message);
                    break;
                case SUCCESS:
                    title = ctx.getString(R.string.success_title);
                    message = ctx.getString(R.string.success_message);
                    break;
                case OTHER:
                    title = ctx.getString(R.string.othererror_title);
                    message = ctx.getString(R.string.othererror_message);
                    break;
                default:
                    return; // there are no other return values than 0,1,2
            }

            new AlertDialog.Builder(ctx)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

            wifiManager.setWifiEnabled(true); // Enable WiFi Again

        }
    }

}