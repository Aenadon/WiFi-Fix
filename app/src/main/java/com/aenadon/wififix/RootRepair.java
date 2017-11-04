package com.aenadon.wififix;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RootRepair extends AsyncTask<RepairMethod, String, TaskResult> {

    private static final String LOG_TAG = RootRepair.class.getSimpleName();

    private ProgressDialog dialog;
    private WifiManager wifiManager;

    private WeakReference<Context> weakCtx;

    RootRepair(Context ctx) {
        this.weakCtx = new WeakReference<>(ctx);

        this.wifiManager = (WifiManager)ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        this.dialog = new ProgressDialog(ctx);
        this.dialog.setMessage(ctx.getString(R.string.working));
        this.dialog.setIndeterminate(true);
        this.dialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected TaskResult doInBackground(RepairMethod... params) {
        wifiManager.setWifiEnabled(false);
        RepairMethod method = params[0];
        try {
            int result;
            switch (method) {
                case DHCP:
                    result = Runtime.getRuntime().exec(new String[]{
                            "su", "-c",
                            "rm -f /data/misc/dhcp/dhcpcd-wlan0.lease",
                            "rm -f /data/misc/dhcp/dhcpcd-wlan0.pid"
                    }).waitFor();
                    break;
                case WPACONF:
                    result = Runtime.getRuntime().exec(new String[]{
                            "su", "-c",
                            "rm -f /data/misc/wifi/wpa_supplicant.conf"
                    }).waitFor();
                    break;
                default:
                    return new TaskResult(TaskStatus.EXCEPTION);
            }
            if (result != 0) return new TaskResult(TaskStatus.NO_ROOT);

        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Exception on Root task", e);
            return new TaskResult(TaskStatus.EXCEPTION, e, method);
        } catch (IOException e) {
            Log.e(LOG_TAG, "No root!", e);
            return new TaskResult(TaskStatus.NO_ROOT);
        }
        return new TaskResult(TaskStatus.SUCCESS);
    }

    @Override
    protected void onPostExecute(TaskResult result) {
        dialog.dismiss();
        Context ctx = weakCtx.get();
        if (ctx != null) {
            TaskStatus status = result.getTaskStatus();
            AlertDialog dialogToDisplay;
            if (status == TaskStatus.SUCCESS) {
                String title = ctx.getString(R.string.success_title);
                String message = ctx.getString(R.string.success_message);

                dialogToDisplay = getSimpleDialog(ctx, title, message);
            } else if (status == TaskStatus.NO_ROOT) {
                String title = ctx.getString(R.string.fail_title);
                String message = ctx.getString(R.string.fail_message);

                dialogToDisplay = getSimpleDialog(ctx, title, message);
            } else {
                dialogToDisplay = getReportExceptionDialog(ctx, result);
            }
            dialogToDisplay.show();
        }

        wifiManager.setWifiEnabled(true);
    }

    private AlertDialog getSimpleDialog(Context ctx, String title, String message) {
        return new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private AlertDialog getReportExceptionDialog(final Context ctx, TaskResult result) {
        String title = ctx.getString(R.string.exception_title);
        String message = ctx.getString(R.string.exception_message);

        StringWriter sw = new StringWriter();
        result.getException().printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        @SuppressLint("SimpleDateFormat")
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String timestamp = isoFormat.format(new Date());
        final String body =
                ctx.getString(R.string.crash_email_body_head) +
                "Timestamp: " + timestamp + "\n\n" +
                "Used method: " + result.getRepairMethod().name() + "\n\n" +
                "Stacktrace:\n" + exceptionAsString;

        return new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent emailIntent =
                                new Intent(
                                        Intent.ACTION_SENDTO,
                                        Uri.fromParts(
                                                "mailto",
                                                BuildConfig.CRASH_REPORT_EMAIL_ADDRESS,
                                                null
                                        )
                                );
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.crash_email_subject));
                        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                        ctx.startActivity(Intent.createChooser(emailIntent, ctx.getString(R.string.send_crash_report)));
                    }
                })
                .setNegativeButton(R.string.dismiss, null)
                .create();
    }
}
