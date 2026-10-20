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
        Log.d(TAG, "BootReceiver triggered with action: " + action + " (Service auto-start disabled on boot to ensure connections run only when user opens Prime)");
    }
}
