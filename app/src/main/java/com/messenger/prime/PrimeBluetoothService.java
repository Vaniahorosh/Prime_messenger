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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Objects;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class PrimeBluetoothService extends Service implements BluetoothConnectionManager.ConnectionCallback {

    public static final String CHANNEL_ID = "prime_bt_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_STOP_SERVICE = "com.messenger.prime.action.STOP_SERVICE";
    private static final String TAG = "PrimeBluetoothService";

    private PowerManager.WakeLock wakeLock;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<String, Integer> reconnectAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Runnable> pendingReconnects = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        initWakeLock();
        promoteToForeground();
        BluetoothConnectionManager.getInstance().registerCallback(this);
    }

    @Override
    public void onDestroy() {
        BluetoothConnectionManager.getInstance().unregisterCallback(this);
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }

    private void initWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Prime:BluetoothServiceWakeLock");
                wakeLock.setReferenceCounted(false);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to initialize WakeLock", e);
        }
    }

    private synchronized void manageWakeLock() {
        int activeCount = BluetoothConnectionManager.getInstance().getConnectedDeviceCount();
        if (activeCount > 0) {
            if (wakeLock != null) {
                // Всегда используем таймаут для безопасности батареи
                wakeLock.acquire(15 * 60 * 1000L); // 15 минут максимум
                Log.d(TAG, "Partial WakeLock acquired (active connections: " + activeCount + ")");
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Partial WakeLock released (no active connections)");
            }
        }
    }

    private void resetReconnectAttempts(String address) {
        if (address != null) {
            reconnectAttempts.remove(address);
            Runnable old = pendingReconnects.remove(address);
            if (old != null) {
                reconnectHandler.removeCallbacks(old);
            }
        }
    }

    public static void cancelReconnect(Context context, String address) {
        if (context == null || address == null || address.isEmpty()) return;
        try {
            Intent intent = new Intent(context, PrimeBluetoothService.class);
            intent.setAction("CANCEL_RECONNECT");
            intent.putExtra("EXTRA_ADDRESS", address);
            context.startService(intent);
        } catch (Exception ignored) {}
    }

    private void scheduleReconnect(String address) {
        if (address == null || address.isEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) return;

        int attempts = reconnectAttempts.getOrDefault(address, 0) + 1;
        if (attempts > 3) {
            Log.d(TAG, "Max reconnect attempts (3) reached for " + address + ", stopping auto-reconnect.");
            return;
        }

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
        if (adapter != null) {
            try {
                BluetoothDevice dev = adapter.getRemoteDevice(address);
                int bondState = BluetoothDevice.BOND_NONE;
                try {
                    bondState = dev.getBondState();
                } catch (SecurityException ignored) {}

                if (dev == null || bondState != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Device " + address + " is not bonded, skipping background auto-reconnect.");
                    return;
                }
            } catch (Exception ignored) {}
        }

        reconnectAttempts.put(address, attempts);

        Runnable existing = pendingReconnects.remove(address);
        if (existing != null) {
            reconnectHandler.removeCallbacks(existing);
        }

        long delayMs = attempts == 1 ? 3000L : (attempts == 2 ? 8000L : 20000L);

        Log.d(TAG, "Scheduling exponential reconnect for " + address + " (Attempt #" + attempts + " in " + (delayMs/1000) + "s)");

        Runnable reconnectTask = () -> {
            pendingReconnects.remove(address);
            if (!BluetoothConnectionManager.getInstance().isConnected(address)) {
                if (adapter != null && adapter.isEnabled()) {
                    try {
                        BluetoothDevice device = adapter.getRemoteDevice(address);
                        String displayName = ChatHistoryManager.getDisplayNameForAddress(this, address);
                        BluetoothConnectionManager.getInstance().connectToDevice(
                                adapter, device, UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"),
                                "PrimeUser", displayName, false
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Failed reconnect to " + address, e);
                    }
                }
            }
        };

        pendingReconnects.put(address, reconnectTask);
        reconnectHandler.postDelayed(reconnectTask, delayMs);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        if (intent != null) {
            String action = intent.getAction();
            if ("CANCEL_RECONNECT".equals(action)) {
                String addr = intent.getStringExtra("EXTRA_ADDRESS");
                if (addr != null && !addr.isEmpty()) {
                    resetReconnectAttempts(addr);
                }
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "Stop service requested from notification shade");
                shutdownAllBluetoothOperations();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
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

        BluetoothConnectionManager.getInstance().disconnect();
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
                        Log.e(TAG, "Standard startForeground also failed", t2);
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
                        Log.e(TAG, "Standard startForeground also failed", t2);
                    }
                }
            } else {
                try {
                    startForeground(NOTIFICATION_ID, notification);
                } catch (Throwable t) {
                    Log.e(TAG, "startForeground failed", t);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Fatal failure in promoteToForeground", e);
            try {
                Notification emptyNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_prime_statusbar)
                        .setContentTitle("Prime")
                        .build();
                startForeground(NOTIFICATION_ID, emptyNotification);
            } catch (Throwable t) {
                Log.e(TAG, "Failed fallback startForeground", t);
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

    private static File getAvatarFileFor(Context context, String senderName, String deviceAddress) {
        if (context == null) return null;
        File[] candidates = new File[]{
                deviceAddress != null ? new File(context.getFilesDir(), "rec_avatar_" + deviceAddress + ".gif") : null,
                deviceAddress != null ? new File(context.getFilesDir(), "rec_avatar_" + deviceAddress + ".jpg") : null,
                deviceAddress != null ? new File(context.getFilesDir(), "avatar_" + deviceAddress + ".gif") : null,
                deviceAddress != null ? new File(context.getFilesDir(), "avatar_" + deviceAddress + ".jpg") : null,
                senderName != null ? new File(context.getFilesDir(), "rec_avatar_" + senderName + ".gif") : null,
                senderName != null ? new File(context.getFilesDir(), "rec_avatar_" + senderName + ".jpg") : null,
                senderName != null ? new File(context.getFilesDir(), "avatar_" + senderName + ".gif") : null,
                senderName != null ? new File(context.getFilesDir(), "avatar_" + senderName + ".jpg") : null
        };
        for (File f : candidates) {
            if (f != null && f.exists() && f.length() > 0) return f;
        }
        return null;
    }

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

            boolean isFoldableOrTablet = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
            Class<?> targetActivity = isFoldableOrTablet ? ChatListActivity.class : ChatPersonActivity.class;
            Intent chatIntent = new Intent(context, targetActivity);
            chatIntent.putExtra("EXTRA_CHAT_NAME", senderName);
            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                chatIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
            }
            chatIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int requestCode = (senderName != null ? senderName.hashCode() : 100);
            int notificationId = 2000 + (senderName != null ? Math.abs(senderName.hashCode() % 5000) : 1);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, requestCode, chatIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            String displaySenderName = senderName;
            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                SharedPreferences sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                String savedName = sp.getString("contact_name_" + deviceAddress, null);
                if (savedName == null || savedName.isEmpty()) {
                    savedName = sp.getString(deviceAddress + "_name", null);
                }
                if (savedName != null && !savedName.isEmpty() && !BluetoothAdapter.checkBluetoothAddress(savedName)) {
                    displaySenderName = savedName;
                }
            }
            if (displaySenderName == null || displaySenderName.isEmpty() || BluetoothAdapter.checkBluetoothAddress(displaySenderName)) {
                displaySenderName = "Prime Собеседник";
            }

            // Build Person & MessagingStyle
            Person.Builder personBuilder = new Person.Builder()
                    .setName(displaySenderName)
                    .setKey(senderName != null ? senderName : "unknown");

            File avatarFile = getAvatarFileFor(context, senderName, deviceAddress);
            if (avatarFile != null && avatarFile.exists()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                    if (bmp != null) {
                        personBuilder.setIcon(IconCompat.createWithBitmap(bmp));
                    }
                } catch (Throwable ignored) {}
            }
            Person senderPerson = personBuilder.build();

            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(senderPerson)
                    .setConversationTitle(displaySenderName)
                    .addMessage(messageText != null ? messageText : "Новое сообщение", System.currentTimeMillis(), senderPerson);

            // 1. RemoteInput Action: "Ответить"
            RemoteInput remoteInput = new RemoteInput.Builder("KEY_TEXT_REPLY")
                    .setLabel("Ответить...")
                    .build();

            Intent replyIntent = new Intent(context, NotificationReplyReceiver.class);
            replyIntent.setAction("ACTION_NOTIFICATION_REPLY");
            replyIntent.putExtra("EXTRA_CHAT_NAME", senderName);
            replyIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
            replyIntent.putExtra("EXTRA_NOTIFICATION_ID", notificationId);

            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode + 10,
                    replyIntent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_undo,
                    "Ответить",
                    replyPendingIntent
            ).addRemoteInput(remoteInput).build();

            // 2. Mark as Read Action: "Прочитано"
            Intent markReadIntent = new Intent(context, NotificationReplyReceiver.class);
            markReadIntent.setAction("ACTION_NOTIFICATION_MARK_READ");
            markReadIntent.putExtra("EXTRA_CHAT_NAME", senderName);
            markReadIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
            markReadIntent.putExtra("EXTRA_NOTIFICATION_ID", notificationId);

            PendingIntent markReadPendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode + 20,
                    markReadIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Action markReadAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_done,
                    "Прочитано",
                    markReadPendingIntent
            ).build();

            Notification notification = new NotificationCompat.Builder(context, MSG_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_prime_statusbar)
                    .setStyle(messagingStyle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .addAction(replyAction)
                    .addAction(markReadAction)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();

            nm.notify(notificationId, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to post message notification", e);
        }
    }

    @Override
    public void onStateChanged(String deviceAddress, BluetoothConnectionManager.ConnectionState state, String deviceName) {
        manageWakeLock();
        promoteToForeground();

        boolean isConnected = (state == BluetoothConnectionManager.ConnectionState.CONNECTED);
        if (isConnected) {
            resetReconnectAttempts(deviceAddress);
        } else if (state == BluetoothConnectionManager.ConnectionState.DISCONNECTED) {
            scheduleReconnect(deviceAddress);
        }

        saveBackgroundPresenceToChatList(deviceName, deviceAddress, isConnected);
    }

    private void saveBackgroundTypingStateToChatList(String targetName, String deviceAddr, String textData) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            boolean updated = false;

            boolean isTyping = textData != null && (textData.contains("TYPING") || textData.contains("SENDING_PHOTO") || textData.contains("RECORDING"));
            long typingUntil = isTyping ? (System.currentTimeMillis() + 6000L) : 0L;
            String actState = textData != null && textData.startsWith("STATE:") ? textData.substring(6) : (isTyping ? "TYPING" : "IDLE");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                boolean isMatch = (deviceAddr != null && !deviceAddr.isEmpty() && deviceAddr.equalsIgnoreCase(id))
                        || (user.equalsIgnoreCase(targetName) && !BluetoothAdapter.checkBluetoothAddress(user));

                if (isMatch) {
                    obj.put("isTyping", isTyping);
                    obj.put("typingUntil", typingUntil);
                    obj.put("activityState", actState);
                    updated = true;
                }
                newArray.put(obj);
            }

            if (updated) {
                ChatHistoryManager.savePersistedChatsJson(this, newArray.toString());
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to save background typing state", e);
        }
    }

    private void saveBackgroundReadReceiptToChatList(String targetName, String deviceAddr, String readMsgId) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            boolean updated = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                boolean isMatch = (deviceAddr != null && !deviceAddr.isEmpty() && deviceAddr.equalsIgnoreCase(id))
                        || (user.equalsIgnoreCase(targetName) && !BluetoothAdapter.checkBluetoothAddress(user));

                if (isMatch) {
                    obj.put("messageStatus", "READ");
                    obj.put("unreadCount", 0);
                    updated = true;
                }
                newArray.put(obj);
            }

            if (updated) {
                ChatHistoryManager.savePersistedChatsJson(this, newArray.toString());
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to save background read receipt", e);
        }
    }

    private void saveBackgroundPresenceToChatList(String targetName, String deviceAddr, boolean isOnline) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            boolean updated = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                boolean isMatch = (deviceAddr != null && !deviceAddr.isEmpty() && deviceAddr.equalsIgnoreCase(id))
                        || (user.equalsIgnoreCase(targetName) && !BluetoothAdapter.checkBluetoothAddress(user));

                if (isMatch) {
                    obj.put("onlineStatus", isOnline ? "ONLINE" : "OFFLINE");
                    updated = true;
                }
                newArray.put(obj);
            }

            if (updated) {
                ChatHistoryManager.savePersistedChatsJson(this, newArray.toString());
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to save background presence", e);
        }
    }

    public static final int PROGRESS_NOTIFICATION_ID = 1002;

    @Override
    public void onSendProgress(String deviceAddress, int progress) {
        updateSendProgressNotification(deviceAddress, progress);
    }

    private void updateSendProgressNotification(String deviceAddress, int progress) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (progress >= 100 || progress < 0) {
                nm.cancel(PROGRESS_NOTIFICATION_ID);
                return;
            }

            Intent cancelIntent = new Intent(this, NotificationReplyReceiver.class);
            cancelIntent.setAction("com.messenger.prime.action.CANCEL_UPLOAD");
            cancelIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
            cancelIntent.putExtra("EXTRA_NOTIFICATION_ID", PROGRESS_NOTIFICATION_ID);

            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                    this, 1002, cancelIntent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            String peerName = ChatHistoryManager.getDisplayNameForAddress(this, deviceAddress);
            if (peerName == null || peerName.isEmpty() || BluetoothAdapter.checkBluetoothAddress(peerName)) peerName = "Собеседнику";

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentTitle("Отправка файла...")
                    .setContentText(progress + "% (" + peerName + ")")
                    .setProgress(100, progress, false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(R.drawable.ic_cancel, "Отменить", cancelPendingIntent)
                    .build();

            nm.notify(PROGRESS_NOTIFICATION_ID, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to update send progress notification", e);
        }
    }

    @Override
    public void onPacketReceived(String fromAddress, byte type, byte[] payload) {
        if (payload == null || payload.length == 0) return;
        
        resetReconnectAttempts(fromAddress);
        manageWakeLock();

        if (type == 0x01) { // TYPE_TEXT
            String textData = new String(payload, StandardCharsets.UTF_8);
            if (textData.startsWith("HANDSHAKE:") || textData.startsWith("HANDSHAKE_ACK:")) {
                processBackgroundHandshake(fromAddress, textData);
                return;
            }
        }
        
        String displayName = resolveBackgroundDisplayName(fromAddress);

        try {
            if (ChatPersonActivity.isForegroundWithAddress(fromAddress)) {
                // Если чат открыт на экране, он сам обработает и сохранит все пакеты,
                // поэтому фоновому сервису не нужно дублировать работу и ломать файлы.
                if (type == 0x08) {
                    ChatListNotifier.INSTANCE.notifyChanged();
                }
                return;
            }

            if (type == 0x01) { // TYPE_TEXT
                String textData = new String(payload, StandardCharsets.UTF_8);
                ChatPersonActivity.ParsedMessagePayload parsed = ChatPersonActivity.ParsedMessagePayload.parse(textData);
                long ts = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
                ChatMessage msg = new ChatMessage(parsed.realText, time, displayName, false, null, ts, null, parsed.msgId);
                
                ChatHistoryManager.saveMessage(this, displayName, msg);
                if (fromAddress != null && !fromAddress.isEmpty() && !fromAddress.equalsIgnoreCase(displayName)) {
                    ChatHistoryManager.saveMessage(this, fromAddress, msg);
                }
                saveBackgroundLastMessageToChatList(displayName, fromAddress, parsed.realText);
                showMessageNotification(this, displayName, parsed.realText, fromAddress);
            } else if (type == 0x02) { // TYPE_PHOTO
                long ts = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
                ChatMessage msg = parseBackgroundPhotoMessage(this, payload, displayName, ts, time);
                ChatHistoryManager.saveMessage(this, displayName, msg);
                if (fromAddress != null && !fromAddress.isEmpty() && !fromAddress.equalsIgnoreCase(displayName)) {
                    ChatHistoryManager.saveMessage(this, fromAddress, msg);
                }

                String summaryStr = ChatMessage.getSummaryDescription(msg);
                saveBackgroundLastMessageToChatList(displayName, fromAddress, summaryStr);
                showMessageNotification(this, displayName, summaryStr, fromAddress);
            } else if (type == 0x0E) { // TYPE_FILE
                long ts = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
                ChatMessage msg = parseBackgroundFileMessage(this, payload, displayName, ts, time);
                ChatHistoryManager.saveMessage(this, displayName, msg);
                if (fromAddress != null && !fromAddress.isEmpty() && !fromAddress.equalsIgnoreCase(displayName)) {
                    ChatHistoryManager.saveMessage(this, fromAddress, msg);
                }

                String summaryStr = ChatMessage.getSummaryDescription(msg);
                saveBackgroundLastMessageToChatList(displayName, fromAddress, summaryStr);
                showMessageNotification(this, displayName, summaryStr, fromAddress);
            } else if (type == 0x04) { // TYPE_TYPING
                String textData = new String(payload, StandardCharsets.UTF_8);
                saveBackgroundTypingStateToChatList(displayName, fromAddress, textData);
            } else if (type == 0x05) { // TYPE_EDIT
                String editData = new String(payload, StandardCharsets.UTF_8);
                int editSep = editData.indexOf(":::");
                if (editSep != -1) {
                    String editMsgId = editData.substring(0, editSep);
                    String updatedText = editData.substring(editSep + 3);
                    List<ChatMessage> history = ChatHistoryManager.loadMessages(this, displayName);
                    for (ChatMessage m : history) {
                        if (Objects.equals(editMsgId, m.getMessageId())) {
                            m.setEdited(true);
                            m.setText(updatedText);
                            ChatHistoryManager.saveMessage(this, displayName, m);
                            break;
                        }
                    }
                    saveBackgroundLastMessageToChatList(displayName, fromAddress, updatedText);
                }
            } else if (type == 0x08) { // TYPE_CHAT_DELETED
                ChatHistoryManager.deleteHistoryCompletely(this, displayName, fromAddress);
                ChatListNotifier.INSTANCE.notifyChanged();
            } else if (type == 0x0A) { // TYPE_READ_RECEIPT
                String readMsgId = new String(payload, StandardCharsets.UTF_8);
                ChatHistoryManager.markOutgoingMessagesAsRead(this, displayName, readMsgId);
                saveBackgroundReadReceiptToChatList(displayName, fromAddress, readMsgId);
            } else if (type == 0x0B) { // TYPE_DELETE_MSG
                String deletedMsgId = new String(payload, StandardCharsets.UTF_8);
                ChatHistoryManager.deleteSingleMessage(this, displayName, deletedMsgId);
            } else if (type == 0x0C) { // TYPE_PRESENCE
                String pData = new String(payload, StandardCharsets.UTF_8);
                saveBackgroundPresenceToChatList(displayName, fromAddress, pData.startsWith("ONLINE"));
            } else if (type == 0x0D) { // TYPE_REACTION
                String rxData = new String(payload, StandardCharsets.UTF_8);
                String[] rxParts = rxData.split(":::");
                if (rxParts.length >= 2) {
                    String rxMsgId = rxParts[0];
                    String reactionStr = rxParts[1];
                    String authorLogin = rxParts.length >= 3 ? rxParts[2] : displayName;
                    String finalReaction = "REMOVE".equalsIgnoreCase(reactionStr) ? null : reactionStr;
                    List<ChatMessage> history = ChatHistoryManager.loadMessages(this, displayName);
                    for (ChatMessage m : history) {
                        if (Objects.equals(rxMsgId, m.getMessageId())) {
                            m.setReactionForUser(authorLogin, finalReaction);
                            ChatHistoryManager.saveMessage(this, displayName, m);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error handling background packet in PrimeBluetoothService", e);
        }
    }

    @Override
    public void onError(String deviceAddress, String errorMessage) {
        Log.w(TAG, "Service Connection Error (" + deviceAddress + "): " + errorMessage);
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(PROGRESS_NOTIFICATION_ID);
        } catch (Throwable ignored) {}
    }

    private String resolveBackgroundDisplayName(String fromAddress) {
        if (fromAddress == null || fromAddress.isEmpty()) return "Собеседник";
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
        String name = sp.getString("contact_name_" + fromAddress, null);
        if (name == null || name.isEmpty()) name = sp.getString(fromAddress + "_name", null);
        if (name != null && !name.isEmpty() && !BluetoothAdapter.checkBluetoothAddress(name)) {
            return name;
        }
        String hName = ChatHistoryManager.getDisplayNameForAddress(this, fromAddress);
        if (hName != null && !hName.isEmpty() && !BluetoothAdapter.checkBluetoothAddress(hName)) {
            return hName;
        }
        return "Собеседник";
    }

    private void processBackgroundHandshake(String fromAddress, String textData) {
        try {
            boolean isAck = textData.startsWith("HANDSHAKE_ACK:");
            String data = textData.substring(isAck ? 14 : 10).trim();
            String remoteName = null;
            String remoteLogin = null;

            if (data.contains("login=") || data.contains("name=")) {
                String[] parts = data.split(";");
                for (String p : parts) {
                    if (p.startsWith("login=")) remoteLogin = p.substring(6);
                    else if (p.startsWith("name=")) remoteName = p.substring(5);
                }
            } else {
                remoteName = data;
            }

            if (remoteName != null && !remoteName.trim().isEmpty() && !remoteName.equals("1")) {
                if (fromAddress != null && !fromAddress.isEmpty()) {
                    AvatarManager.saveContactPreferenceSafely(this, fromAddress, "contact_name", remoteName);
                    AvatarManager.saveContactPreferenceSafely(this, remoteName, "mac", fromAddress);
                }
                if (remoteLogin != null && !remoteLogin.isEmpty()) {
                    AvatarManager.saveContactPreferenceSafely(this, remoteLogin, "contact_name", remoteName);
                }

                BluetoothConnectionManager.getInstance().setRemoteUsername(fromAddress, remoteName);

                if (!isAck) {
                    SharedPreferences sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
                    String currentUser = sp.getString("current_user", "");
                    String myName = sp.getString("my_name", null);
                    if (myName == null || myName.isEmpty()) myName = sp.getString("my_local_name", null);
                    if (myName == null || myName.isEmpty()) myName = sp.getString("current_user_name", null);
                    if (myName == null || myName.isEmpty()) myName = sp.getString(currentUser + "_name", currentUser);
                    if (myName == null || myName.isEmpty()) myName = "Prime User";

                    String ackStr = "HANDSHAKE_ACK:name=" + myName;
                    BluetoothConnectionManager.getInstance().sendPacket(fromAddress, (byte) 0x01, ackStr.getBytes(StandardCharsets.UTF_8));
                }

                saveBackgroundPresenceToChatList(remoteName, fromAddress, true);
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing background handshake", e);
        }
    }

    private void saveBackgroundLastMessageToChatList(String targetName, String deviceAddr, String lastMsg) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            JSONObject updatedObj = null;
            boolean foundMatch = false;

            long nowTs = System.currentTimeMillis();
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(nowTs));

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                boolean isGenericName = "Собеседник".equalsIgnoreCase(targetName) || "Prime Собеседник".equalsIgnoreCase(targetName) || "Контакт".equalsIgnoreCase(targetName);
                boolean isMatch = !foundMatch && ((deviceAddr != null && !deviceAddr.isEmpty() && deviceAddr.equalsIgnoreCase(id))
                        || (targetName != null && !targetName.isEmpty() && targetName.equalsIgnoreCase(id))
                        || (!isGenericName && user.equalsIgnoreCase(targetName) && !BluetoothAdapter.checkBluetoothAddress(user)));

                if (isMatch) {
                    foundMatch = true;
                    obj.put("lastMessage", lastMsg);
                    obj.put("time", timeStr);
                    obj.put("timestamp", nowTs);
                    obj.put("onlineStatus", "ONLINE");
                    
                    int currentUnread = obj.optInt("unreadCount", 0);
                    obj.put("unreadCount", currentUnread + 1);
                    
                    if (deviceAddr != null && !deviceAddr.isEmpty() && BluetoothAdapter.checkBluetoothAddress(deviceAddr)) {
                        obj.put("id", deviceAddr);
                    }
                    if (targetName != null && !targetName.isEmpty() && !BluetoothAdapter.checkBluetoothAddress(targetName) && !isGenericName) {
                        obj.put("name", targetName);
                    }
                    updatedObj = obj;
                } else {
                    newArray.put(obj);
                }
            }

            if (updatedObj == null) {
                updatedObj = new JSONObject();
                updatedObj.put("id", deviceAddr != null ? deviceAddr : targetName);
                String saveName = (targetName != null && !BluetoothAdapter.checkBluetoothAddress(targetName)) ? targetName : "Собеседник";
                updatedObj.put("name", saveName);
                updatedObj.put("lastMessage", lastMsg);
                updatedObj.put("time", timeStr);
                updatedObj.put("timestamp", nowTs);
                updatedObj.put("onlineStatus", "ONLINE");
                updatedObj.put("messageStatus", "NONE");
                updatedObj.put("unreadCount", 1);
            }
            newArray.put(updatedObj);

            ChatHistoryManager.savePersistedChatsJson(this, newArray.toString());
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Throwable e) {
            Log.e(TAG, "Failed to save background last message", e);
        }
    }

    private static ChatMessage parseBackgroundPhotoMessage(Context context, byte[] fullPayload, String sender, long photoTs, String photoTime) {
        if (fullPayload == null || fullPayload.length == 0) {
            return new ChatMessage("Фотография", photoTime, sender, false, null, photoTs, null, null);
        }
        try {
            String metaCheck = new String(fullPayload, 0, Math.min(fullPayload.length, 300), StandardCharsets.UTF_8);
            if (metaCheck.contains(":::MULTI:")) {
                int headerEndIdx = -1;
                String headerEndTag = ":::HEADER_END:::";
                byte[] tagBytes = headerEndTag.getBytes(StandardCharsets.UTF_8);
                for (int i = 0; i <= fullPayload.length - tagBytes.length; i++) {
                    boolean match = true;
                    for (int j = 0; j < tagBytes.length; j++) {
                        if (fullPayload[i + j] != tagBytes[j]) { match = false; break; }
                    }
                    if (match) { headerEndIdx = i; break; }
                }

                if (headerEndIdx != -1) {
                    String headerStr = new String(fullPayload, 0, headerEndIdx, StandardCharsets.UTF_8);
                    int bodyStart = headerEndIdx + tagBytes.length;

                    String photoMsgId = ChatPersonActivity.extractMsgId(headerStr);
                    String sizesPart = "";
                    int sizesIdx = headerStr.indexOf(":::SIZES:");
                    if (sizesIdx != -1) {
                        sizesPart = headerStr.substring(sizesIdx + 9);
                        int endSizes = sizesPart.indexOf(":::");
                        if (endSizes != -1) sizesPart = sizesPart.substring(0, endSizes);
                    }

                    String captionText = "";
                    int multiIdx = headerStr.indexOf(":::MULTI:");
                    if (multiIdx != -1 && sizesIdx != -1 && sizesIdx > multiIdx) {
                        String multiSub = headerStr.substring(multiIdx + 9, sizesIdx);
                        int colonIdx = multiSub.indexOf(":::");
                        if (colonIdx != -1) {
                            captionText = multiSub.substring(colonIdx + 3);
                        }
                    }

                    String[] itemMetas = sizesPart.split(",");
                    List<ChatMessage.MediaItem> recMediaItems = new ArrayList<>();
                    int currentOffset = bodyStart;

                    for (int idx = 0; idx < itemMetas.length; idx++) {
                        try {
                            String metaStr = itemMetas[idx].trim();
                            String[] parts = metaStr.split("\\|");
                            boolean isVideo = parts.length >= 1 && "1".equals(parts[0]);
                            int pSize = parts.length >= 2 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
                            String durStr = parts.length >= 3 ? parts[2] : "00:00";
                            String ext = parts.length >= 4 ? parts[3] : (isVideo ? "mp4" : "jpg");

                            if (currentOffset + pSize <= fullPayload.length) {
                                byte[] itemBytes = new byte[pSize];
                                System.arraycopy(fullPayload, currentOffset, itemBytes, 0, pSize);
                                currentOffset += pSize;

                                File mediaFile = ChatHistoryManager.saveBytesToAtomicFile(context, "rec_media_" + (photoMsgId != null ? photoMsgId : photoTs) + "_" + idx + "." + ext, itemBytes);
                                if (mediaFile != null) {
                                    recMediaItems.add(new ChatMessage.MediaItem(mediaFile.getAbsolutePath(), isVideo, durStr));
                                }
                            }
                        } catch (Exception e) {
                            Log.e("PrimeBluetoothService", "Failed to parse multi-media item " + idx, e);
                        }
                    }

                    if (!recMediaItems.isEmpty()) {
                        ChatMessage multiMsg = new ChatMessage(captionText, photoTime, sender, false, null, photoTs, null, photoMsgId);
                        multiMsg.setMediaItems(recMediaItems);
                        return multiMsg;
                    }
                }
            }

            String photoMsgId = null;
            String captionText = null;
            byte[] photoBytes = fullPayload;
            int sepIdx = -1;
            for (int i = 0; i < Math.min(fullPayload.length, 120); i++) {
                if (fullPayload[i] == ':' && i + 2 < fullPayload.length && fullPayload[i+1] == ':' && fullPayload[i+2] == ':') {
                    sepIdx = i;
                    break;
                }
            }
            if (sepIdx != -1) {
                photoMsgId = new String(fullPayload, 0, sepIdx, StandardCharsets.UTF_8);
                photoBytes = new byte[fullPayload.length - (sepIdx + 3)];
                System.arraycopy(fullPayload, sepIdx + 3, photoBytes, 0, photoBytes.length);
            }

            if (photoBytes.length > 9) {
                byte[] magic = "|PRM|".getBytes(StandardCharsets.UTF_8);
                boolean hasMagic = true;
                for (int i = 0; i < 5; i++) {
                    if (photoBytes[photoBytes.length - 5 + i] != magic[i]) {
                        hasMagic = false;
                        break;
                    }
                }
                if (hasMagic) {
                    int capLen = ((photoBytes[photoBytes.length - 9] & 0xFF) << 24) |
                                 ((photoBytes[photoBytes.length - 8] & 0xFF) << 16) |
                                 ((photoBytes[photoBytes.length - 7] & 0xFF) << 8) |
                                 (photoBytes[photoBytes.length - 6] & 0xFF);
                    if (capLen > 0 && capLen < photoBytes.length - 9) {
                        captionText = new String(photoBytes, photoBytes.length - 9 - capLen, capLen, StandardCharsets.UTF_8);
                        byte[] cleanPhoto = new byte[photoBytes.length - 9 - capLen];
                        System.arraycopy(photoBytes, 0, cleanPhoto, 0, cleanPhoto.length);
                        photoBytes = cleanPhoto;
                    }
                }
            }

            String savedPhotoPath = null;
            if (photoBytes.length > 0) {
                try {
                    boolean isGif = photoBytes.length > 3 && photoBytes[0] == (byte) 'G' && photoBytes[1] == (byte) 'I' && photoBytes[2] == (byte) 'F';
                    String ext = isGif ? ".gif" : ".jpg";
                    File photoFile = ChatHistoryManager.saveBytesToAtomicFile(context, "rec_photo_" + (photoMsgId != null ? photoMsgId : photoTs) + ext, photoBytes);
                    if (photoFile != null) {
                        savedPhotoPath = photoFile.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e("PrimeBluetoothService", "Failed to save received photo in background", e);
                }
            }

            ChatMessage photoMsg = new ChatMessage(captionText, photoTime, sender, false, null, photoTs, savedPhotoPath, photoMsgId);
            photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
            return photoMsg;
        } catch (Throwable e) {
            Log.e("PrimeBluetoothService", "Failed to parse background photo message", e);
            return new ChatMessage("Фотография", photoTime, sender, false, null, photoTs, null, null);
        }
    }

    private static ChatMessage parseBackgroundFileMessage(Context context, byte[] fullPayload, String sender, long timestamp, String time) {
        if (fullPayload == null || fullPayload.length == 0) {
            return new ChatMessage("Файл", time, sender, false, null, timestamp, null, null);
        }
        try {
            int headerEndIdx = -1;
            String headerEndTag = ":::HEADER_END:::";
            byte[] tagBytes = headerEndTag.getBytes(StandardCharsets.UTF_8);

            for (int i = 0; i <= fullPayload.length - tagBytes.length; i++) {
                boolean match = true;
                for (int j = 0; j < tagBytes.length; j++) {
                    if (fullPayload[i + j] != tagBytes[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    headerEndIdx = i;
                    break;
                }
            }

            String headerStr = "";
            byte[] fileDataBytes = new byte[0];

            if (headerEndIdx != -1) {
                headerStr = new String(fullPayload, 0, headerEndIdx, StandardCharsets.UTF_8);
                int bodyStart = headerEndIdx + tagBytes.length;
                int bodyLen = fullPayload.length - bodyStart;
                if (bodyLen > 0) {
                    fileDataBytes = new byte[bodyLen];
                    System.arraycopy(fullPayload, bodyStart, fileDataBytes, 0, bodyLen);
                }
            } else {
                headerStr = new String(fullPayload, StandardCharsets.UTF_8);
            }

            String msgId = null;
            String fileName = "Файл";
            long fileSize = 0L;
            String text = "";
            String videoDuration = null;

            BiFunction<String, String, String> extractTag = (header, tag) -> {
                int idx = header.indexOf(tag);
                if (idx == -1) return null;
                int start = idx + tag.length();
                int end = header.indexOf(":::", start);
                if (end == -1) end = header.length();
                return header.substring(start, end).trim();
            };

            videoDuration = extractTag.apply(headerStr, ":::DURATION:::");
            String isVideoStr = extractTag.apply(headerStr, ":::IS_VIDEO:::");
            boolean isVideoFlag = "1".equals(isVideoStr);

            String[] parts = headerStr.split(":::");
            if (parts.length >= 1) msgId = parts[0];
            if (parts.length >= 2) fileName = parts[1];
            if (parts.length >= 3) {
                try { fileSize = Long.parseLong(parts[2]); } catch (Exception ignored) {}
            }
            if (parts.length >= 4) text = parts[3];

            String localSavedPath = null;
            if (fileDataBytes.length > 0) {
                try {
                    File localFile = ChatHistoryManager.saveBytesToAtomicFile(context, "rec_file_" + (msgId != null ? msgId : timestamp) + "_" + fileName, fileDataBytes);
                    if (localFile != null) {
                        localSavedPath = localFile.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e("PrimeBluetoothService", "Failed to save received file in background", e);
                }
            }

            ChatMessage msg = new ChatMessage(text, time, sender, false, null, timestamp, localSavedPath, msgId);

            String msgTypeStr = extractTag.apply(headerStr, ":::MSG_TYPE:::");
            ChatMessage.MessageType parsedType = null;
            if (msgTypeStr != null) {
                try {
                    parsedType = ChatMessage.MessageType.valueOf(msgTypeStr);
                } catch (Exception ignored) {}
            }

            if (parsedType == null) {
                String lowerName = fileName.toLowerCase(Locale.US);
                boolean isAudio = lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") || lowerName.endsWith(".flac") || lowerName.endsWith(".opus") || lowerName.endsWith(".wma");
                boolean isVid = !isAudio && (isVideoFlag || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi"));
                boolean isImg = !isAudio && !isVid && (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif"));

                if (isAudio) parsedType = ChatMessage.MessageType.MUSIC;
                else if (isVid) parsedType = ChatMessage.MessageType.VIDEO;
                else if (isImg) parsedType = ChatMessage.MessageType.IMAGE;
                else parsedType = ChatMessage.MessageType.FILE;
            }

            msg.setMessageType(parsedType);
            if (parsedType == ChatMessage.MessageType.VIDEO) {
                msg.setVideoDuration(videoDuration != null ? videoDuration : "00:00");
            } else if (parsedType == ChatMessage.MessageType.MUSIC) {
                msg.setAudioDuration(videoDuration != null ? videoDuration : "00:00");
                if (fileName.contains(" - ")) {
                    String[] tParts = fileName.split(" - ", 2);
                    msg.setArtistName(tParts[0].trim());
                    msg.setTrackTitle(tParts[1].trim());
                } else {
                    msg.setTrackTitle(fileName);
                }
            }

            msg.setFileName(fileName);
            msg.setFileSize(fileSize > 0 ? fileSize : fileDataBytes.length);

            return msg;
        } catch (Throwable e) {
            Log.e("PrimeBluetoothService", "Failed to parse background file message", e);
            return new ChatMessage("Файл", time, sender, false, null, timestamp, null, null);
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
