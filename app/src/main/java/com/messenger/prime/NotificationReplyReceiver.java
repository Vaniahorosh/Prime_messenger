package com.messenger.prime;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.RemoteInput;

import com.messenger.prime.events.ChatEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class NotificationReplyReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReplyRx";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        String senderName = intent.getStringExtra("EXTRA_CHAT_NAME");
        String deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");
        int notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0);

        String targetAddr = (deviceAddress != null && !deviceAddress.isEmpty()) ? deviceAddress : senderName;

        if ("ACTION_NOTIFICATION_REPLY".equals(action)) {
            Bundle remoteInputResults = RemoteInput.getResultsFromIntent(intent);
            if (remoteInputResults != null) {
                CharSequence replyText = remoteInputResults.getCharSequence("KEY_TEXT_REPLY");
                if (replyText != null && replyText.length() > 0) {
                    String text = replyText.toString().trim();
                    long timestamp = System.currentTimeMillis();
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));

                    SharedPreferences sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                    String currentUser = sharedPrefs.getString("current_user", "");
                    String myDisplayName = sharedPrefs.getString(currentUser + "_name", currentUser);
                    String localUser = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";

                    String messageId = localUser + "_" + timestamp + "_" + UUID.randomUUID().toString();
                    ChatMessage message = new ChatMessage(text, time, localUser, true, null, timestamp, null, messageId);
                    message.setMessageStatus(MessageStatus.SENT);

                    ChatHistoryManager.saveMessage(context, senderName, message);

                    String payloadStr = messageId + ":::" + text;
                    byte[] payload = payloadStr.getBytes(StandardCharsets.UTF_8);
                    BluetoothConnectionManager.getInstance().sendPacket(targetAddr, (byte) 0x01, payload);

                    resetUnreadCountAndUpdateLastMessage(context, senderName, targetAddr, text);
                }
            }
        } else if ("ACTION_NOTIFICATION_MARK_READ".equals(action)) {
            String readReceiptPayload = "READ_ALL:::MARK_READ";
            BluetoothConnectionManager.getInstance().sendPacket(targetAddr, (byte) 0x0A, readReceiptPayload.getBytes(StandardCharsets.UTF_8));

            resetUnreadCountAndUpdateLastMessage(context, senderName, targetAddr, null);
        } else if ("com.messenger.prime.action.CANCEL_UPLOAD".equals(action)) {
            if (targetAddr != null && !targetAddr.isEmpty()) {
                BluetoothConnectionManager.getInstance().cancelCurrentMediaSend(targetAddr);
            }
        }

        if (notificationId > 0) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(notificationId);
            }
        }
    }

    private void resetUnreadCountAndUpdateLastMessage(Context context, String senderName, String deviceAddr, String lastMsg) {
        try {
            SharedPreferences sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();

            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String user = obj.optString("name", "");
                String id = obj.optString("id", "");

                if (user.equalsIgnoreCase(senderName) || (deviceAddr != null && deviceAddr.equalsIgnoreCase(id))) {
                    if (lastMsg != null) {
                        obj.put("lastMessage", lastMsg);
                        obj.put("time", timeStr);
                        obj.put("messageStatus", "SENT");
                    }
                    obj.put("unreadCount", 0);
                }
                newArray.put(obj);
            }

            sharedPrefs.edit().putString("persisted_chats", newArray.toString()).apply();
            ChatListNotifier.emitEvent(ChatEvent.GeneralUpdate.INSTANCE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update chat list from NotificationReplyReceiver", e);
        }
    }
}
