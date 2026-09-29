package com.messenger.prime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrimeBluetoothService extends Service implements BluetoothConnectionManager.ConnectionCallback {

    public static final String CHANNEL_ID = "prime_bt_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_STOP_SERVICE = "com.messenger.prime.action.STOP_SERVICE";
    private static final String TAG = "PrimeBluetoothService";

    @Override
    public void onCreate() {
        super.onCreate();
        promoteToForeground();
        BluetoothConnectionManager.getInstance().registerCallback(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothConnectionManager.getInstance().unregisterCallback(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            Log.d(TAG, "Stop service requested from notification shade");
            shutdownAllBluetoothOperations();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "App task removed from Recents, shutting down Bluetooth operations...");
        shutdownAllBluetoothOperations();
        stopSelf();
    }

    @SuppressWarnings("MissingPermission")
    private void shutdownAllBluetoothOperations() {
        try {
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
            if (adapter != null && adapter.isEnabled()) {
                adapter.cancelDiscovery();
            }
        } catch (Throwable ignored) {}

        BluetoothSocketHolder.clearSocket();

        try {
            Intent stopSearchIntent = new Intent("com.messenger.prime.STOP_ALL_SEARCH").setPackage(getPackageName());
            sendBroadcast(stopSearchIntent);
        } catch (Throwable ignored) {}

        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Throwable ignored) {}
    }

    private synchronized void promoteToForeground() {
        createNotificationChannel();
        try {
            Intent notificationIntent = new Intent(this, ChatListActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent stopIntent = new Intent(this, PrimeBluetoothService.class);
            stopIntent.setAction(ACTION_STOP_SERVICE);
            PendingIntent stopPendingIntent = PendingIntent.getService(
                    this, 1, stopIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Prime Messenger")
                    .setContentText("Служба Bluetooth активна")
                    .setSmallIcon(R.drawable.ic_prime_statusbar)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(R.drawable.ic_cancel, "Отключить фоновую работу", stopPendingIntent)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    );
                } catch (Throwable t) {
                    Log.e(TAG, "Failed typed startForeground, trying standard startForeground", t);
                    try {
                        startForeground(NOTIFICATION_ID, notification);
                    } catch (Throwable t2) {
                        Log.e(TAG, "Standard startForeground also failed, calling stopSelf() to prevent crash", t2);
                        stopSelf();
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    );
                } catch (Throwable t) {
                    try {
                        startForeground(NOTIFICATION_ID, notification);
                    } catch (Throwable t2) {
                        stopSelf();
                    }
                }
            } else {
                try {
                    startForeground(NOTIFICATION_ID, notification);
                } catch (Throwable t) {
                    stopSelf();
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Fatal failure in promoteToForeground, stopping service to prevent crash", e);
            try {
                Notification emptyNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_prime_statusbar)
                        .setContentTitle("Prime")
                        .build();
                startForeground(NOTIFICATION_ID, emptyNotification);
            } catch (Throwable t) {
                stopSelf();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    NotificationChannel serviceChannel = new NotificationChannel(
                            CHANNEL_ID,
                            "Prime Bluetooth Service",
                            NotificationManager.IMPORTANCE_LOW
                    );
                    manager.createNotificationChannel(serviceChannel);

                    NotificationChannel msgChannel = new NotificationChannel(
                            MSG_CHANNEL_ID,
                            "Входящие сообщения",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    msgChannel.enableVibration(true);
                    msgChannel.setShowBadge(true);
                    manager.createNotificationChannel(msgChannel);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create notification channel", e);
            }
        }
    }

    public static final String MSG_CHANNEL_ID = "prime_messages_channel";

    public static void showMessageNotification(Context context, String senderName, String messageText, String deviceAddress) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel msgChannel = new NotificationChannel(
                        MSG_CHANNEL_ID,
                        "Входящие сообщения",
                        NotificationManager.IMPORTANCE_HIGH
                );
                msgChannel.enableVibration(true);
                msgChannel.setShowBadge(true);
                nm.createNotificationChannel(msgChannel);
            }

            Intent chatIntent = new Intent(context, ChatPersonActivity.class);
            chatIntent.putExtra("EXTRA_CHAT_NAME", senderName);
            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                chatIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
            }
            chatIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int requestCode = (senderName != null ? senderName.hashCode() : 100);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, requestCode, chatIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new NotificationCompat.Builder(context, MSG_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_prime_statusbar)
                    .setContentTitle(senderName != null && !senderName.isEmpty() ? senderName : "Prime Messenger")
                    .setContentText(messageText != null ? messageText : "Новое сообщение")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();

            int notificationId = 2000 + (senderName != null ? Math.abs(senderName.hashCode() % 5000) : 1);
            nm.notify(notificationId, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to post message notification", e);
        }
    }

    @Override
    public void onStateChanged(BluetoothConnectionManager.ConnectionState state, String deviceName, String deviceAddress) {
        if (state == BluetoothConnectionManager.ConnectionState.CONNECTED) {
            updateStatus(this, "Подключено: " + (deviceName != null ? deviceName : "Prime User"));
        } else if (state == BluetoothConnectionManager.ConnectionState.CONNECTING) {
            updateStatus(this, "Установка связи...");
        } else {
            updateStatus(this, "Служба Bluetooth активна");
        }
    }

    @Override
    public void onSendProgress(int progress) {
        // Фоновая служба прогресс отправки не обрабатывает
    }

    @Override
    public void onPacketReceived(byte type, byte[] payload) {
        if (payload == null || payload.length == 0) return;
        String activeAddress = BluetoothConnectionManager.getInstance().getActiveDeviceAddress();
        String rawActiveName = BluetoothConnectionManager.getInstance().getActiveDeviceName();
        
        String displayName = ChatHistoryManager.getDisplayNameForAddress(this, activeAddress);
        if (displayName == null || displayName.isEmpty() || displayName.equalsIgnoreCase(activeAddress)) {
            displayName = ChatHistoryManager.getDisplayNameForAddress(this, rawActiveName);
        }
        if (displayName == null || displayName.isEmpty() || displayName.equalsIgnoreCase(activeAddress)) {
            displayName = (rawActiveName != null && !rawActiveName.isEmpty()) ? rawActiveName : "Собеседник";
        }

        try {
            if (type == 0x01) { // TYPE_TEXT
                String textData = new String(payload, StandardCharsets.UTF_8);
                if (!textData.startsWith("HANDSHAKE:") && !textData.startsWith("HANDSHAKE_ACK:")) {
                    ChatPersonActivity.ParsedMessagePayload parsed = ChatPersonActivity.ParsedMessagePayload.parse(textData);
                    long ts = System.currentTimeMillis();
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
                    ChatMessage msg = new ChatMessage(parsed.realText, time, displayName, false, null, ts, null, parsed.msgId);
                    
                    ChatHistoryManager.saveMessage(this, displayName, msg);
                    saveBackgroundLastMessageToChatList(displayName, activeAddress, parsed.realText);
                    
                    if (!ChatPersonActivity.isActivityForeground()) {
                        showMessageNotification(this, displayName, parsed.realText, activeAddress);
                    }
                }
            } else if (type == 0x02) { // TYPE_PHOTO
                if (!ChatPersonActivity.isActivityForeground()) {
                    saveBackgroundLastMessageToChatList(displayName, activeAddress, "📷 Фотография");
                    showMessageNotification(this, displayName, "📷 Фотография", activeAddress);
                }
            } else if (type == 0x0E) { // TYPE_FILE
                if (!ChatPersonActivity.isActivityForeground()) {
                    saveBackgroundLastMessageToChatList(displayName, activeAddress, "📎 Файл");
                    showMessageNotification(this, displayName, "📎 Файл", activeAddress);
                }
            } else if (type == 0x08) { // TYPE_CHAT_DELETED
                ChatHistoryManager.deleteHistoryCompletely(this, displayName, activeAddress);
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error handling background packet in PrimeBluetoothService", e);
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.w(TAG, "Service Connection Error: " + errorMessage);
    }

    private void saveBackgroundLastMessageToChatList(String targetName, String deviceAddr, String lastMsg) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            JSONObject updatedObj = null;

            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                if (user.equalsIgnoreCase(targetName) || (deviceAddr != null && deviceAddr.equalsIgnoreCase(id))) {
                    obj.put("lastMessage", lastMsg);
                    obj.put("time", timeStr);
                    obj.put("onlineStatus", "ONLINE");
                    
                    int currentUnread = obj.optInt("unreadCount", 0);
                    obj.put("unreadCount", currentUnread + 1);
                    
                    updatedObj = obj;
                } else {
                    newArray.put(obj);
                }
            }

            if (updatedObj == null) {
                updatedObj = new JSONObject();
                updatedObj.put("id", deviceAddr != null ? deviceAddr : targetName);
                updatedObj.put("name", targetName);
                updatedObj.put("lastMessage", lastMsg);
                updatedObj.put("time", timeStr);
                updatedObj.put("onlineStatus", "ONLINE");
                updatedObj.put("messageStatus", "NONE");
                updatedObj.put("unreadCount", 1);
            }
            newArray.put(updatedObj);

            sharedPrefs.edit().putString("persisted_chats", newArray.toString()).apply();
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Throwable e) {
            Log.e(TAG, "Failed to save background last message", e);
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
                try {
                    context.startForegroundService(serviceIntent);
                } catch (Throwable bgError) {
                    Log.w(TAG, "Foreground service start disallowed, falling back to standard startService", bgError);
                    try { context.startService(serviceIntent); } catch (Throwable ignored) {}
                }
            } else {
                context.startService(serviceIntent);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    public static void updateStatus(Context context, String contentText) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent notificationIntent = new Intent(context, ChatListActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent stopIntent = new Intent(context, PrimeBluetoothService.class);
            stopIntent.setAction(ACTION_STOP_SERVICE);
            PendingIntent stopPendingIntent = PendingIntent.getService(
                    context, 1, stopIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Prime Messenger")
                    .setContentText(contentText != null ? contentText : "Служба Bluetooth активна")
                    .setSmallIcon(R.drawable.ic_prime_statusbar)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(R.drawable.ic_cancel, "Отключить фоновую работу", stopPendingIntent)
                    .build();

            nm.notify(NOTIFICATION_ID, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to update notification status", e);
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
