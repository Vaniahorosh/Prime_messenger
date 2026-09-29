package com.messenger.prime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "BootReceiver triggered with action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {

            try {
                PrimeBluetoothService.startService(context);
                Log.d(TAG, "Successfully started PrimeBluetoothService on boot");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start PrimeBluetoothService on boot", e);
            }
        }
    }
}
