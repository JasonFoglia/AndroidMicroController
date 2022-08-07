package com.jasonfoglia.universalrobotcontroller;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.jasonfoglia.universalrobotcontroller.licensing.AESObfuscator;
import com.jasonfoglia.universalrobotcontroller.licensing.LicenseChecker;
import com.jasonfoglia.universalrobotcontroller.licensing.LicenseCheckerCallback;
import com.jasonfoglia.universalrobotcontroller.licensing.ServerManagedPolicy;


/**
 * Created by jasonfoglia on 11/13/16.
 */
public class LicenseCheckActivity extends AppCompatActivity {
    static boolean licensed = false;
    static boolean didCheck = false;
    static boolean checkingLicense = false;
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnmEH/pwNPUMO1zQrlmcRrPrOSji8ayOjQnC6BBX7p1Fup+siwURu+StV1DXfvmrHguJ7q6EKDlNWmPGoIyyCjlX0n6gvw6JpyL5orsKhrqXbRS890cFq++80JwztA5sSvzdotB/koBbgEKNFlbUGIQwTB9kULS5oswodGi5BQi//YwnygQe5tkRMfvkbaHTVkbvXY/Dqq0OL9FjLp/8kUVAiM2HM6ORveSeGX7ijAf1eGgLTDqdQNaeVRW0IhPAPMhliKP0hrxUhy5SeUJP2y64k0evOPSz7Q1XxCIbXUk5bOUMQqz0TFAqPMd0sBt1rLvlCBBEEcIO/kKILxhLxqwIDAQAB";

    LicenseCheckerCallback mLicenseCheckerCallback;
    LicenseChecker mChecker;
    Handler mHandler;
    private String tag = "BoeBot_license";

    // REPLACE WITH YOUR OWN SALT , THIS IS FROM EXAMPLE
    private static final byte[] SALT = new byte[] { -46, 65, 30, -128, -103,
            -57, 74, -64, 51, 88, -95, -45, 77, -117, -36, -113, -11, 32, -64,
            89 };

    private void displayResult(String results) {
        mHandler.post(new Runnable() {
            public void run() {
                //
            }
        });
    }

    protected void doCheck() {

        didCheck = false;
        checkingLicense = true;
        //

        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    protected void checkLicense() {

        Log.i(tag, "checkLicense");
        mHandler = new Handler();

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);

        doCheck();
    }

    protected class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        public void allow() {
            Log.i(tag, "allow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            displayResult("");
            licensed = true;
            checkingLicense = false;
            didCheck = true;

        }

        public void dontAllow() {
            Log.i(tag, "dontAllow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult("");
            licensed = false;
            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            checkingLicense = false;
            didCheck = true;

            //showDialog(0);
        }

        public void applicationError(ApplicationErrorCode errorCode) {
            Log.i(tag, "error: " + errorCode);
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            licensed = false;
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            String result = String.format(getString(R.string.application_error), errorCode);
            checkingLicense = false;
            didCheck = true;

            displayResult(result);
            //showDialog(0);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // We have only one dialog.
        return new AlertDialog.Builder(this)
                .setTitle(R.string.unlicensed_dialog_title)
                .setMessage(R.string.unlicensed_dialog_body)
                .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent marketIntent = new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("http://market.android.com/details?id="
                                                + getPackageName()));
                                startActivity(marketIntent);
                                finish();
                            }
                        })
                .setNegativeButton(R.string.quit_button,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {

                    public boolean onKey(DialogInterface dialogInterface,
                                         int i, KeyEvent keyEvent) {
                        Log.i(tag, "Key Listener");
                        finish();
                        return true;
                    }
                }).create();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChecker != null) {
            Log.i(tag, "Destroy Checker");
            mChecker.onDestroy();
        }
    }
}
