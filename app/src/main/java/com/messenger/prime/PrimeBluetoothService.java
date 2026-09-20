package com.messenger.prime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PrimeBluetoothService extends Service {

    public static final String CHANNEL_ID = "prime_bt_channel";
    public static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "PrimeBluetoothService";

    @Override
    public void onCreate() {
        super.onCreate();
        promoteToForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        return START_STICKY;
    }

    private synchronized void promoteToForeground() {
        createNotificationChannel();
        try {
            Intent notificationIntent = new Intent(this, ChatListActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Prime Messenger")
                    .setContentText("Служба Bluetooth активна")
                    .setSmallIcon(R.drawable.ic_person)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    );
                } catch (Throwable t) {
                    Log.e(TAG, "Failed typed startForeground, falling back to untyped", t);
                    startForeground(NOTIFICATION_ID, notification);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Fatal failure in promoteToForeground", e);
            try {
                Notification emptyNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_person)
                        .setContentTitle("Prime")
                        .build();
                startForeground(NOTIFICATION_ID, emptyNotification);
            } catch (Throwable ignored) {}
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Prime Bluetooth Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(serviceChannel);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create notification channel", e);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        if (context == null) return;
        try {
            Intent serviceIntent = new Intent(context, PrimeBluetoothService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    public static void stopService(Context context) {
        if (context == null) return;
        try {
            Intent serviceIntent = new Intent(context, PrimeBluetoothService.class);
            context.stopService(serviceIntent);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop service", e);
        }
    }
}
