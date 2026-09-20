package com.messenger.prime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {

    private static final String PREF_NAME = "PrimeChatHistory";

    public static void deleteHistory(Context context, String targetUsername) {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        List<ChatMessage> history = loadMessages(context, targetUsername);
        for (ChatMessage msg : history) {
            if (msg.getImagePath() != null && !msg.getImagePath().isEmpty()) {
                try {
                    File file = new File(msg.getImagePath());
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove("history_" + targetUsername).apply();
    }

    public static void saveMessage(Context context, String targetUsername, ChatMessage message) {
        if (targetUsername == null || targetUsername.isEmpty()) return;

        List<ChatMessage> history = loadMessages(context, targetUsername);
        
        // Handle photo saving if bitmap exists but imagePath doesn't
        if (message.getImageBitmap() != null && (message.getImagePath() == null || message.getImagePath().isEmpty())) {
            String path = saveBitmapToFile(context, message.getImageBitmap(), message.getTimestamp());
            message.setImagePath(path);
        }

        // Check if message already exists (e.g. edit update)
        boolean updated = false;
        for (int i = 0; i < history.size(); i++) {
            if (message.getMessageId() != null && message.getMessageId().equals(history.get(i).getMessageId())) {
                history.set(i, message);
                updated = true;
                break;
            }
        }
        if (!updated) {
            history.add(message);
        }

        saveHistoryList(context, targetUsername, history);
    }

    public static List<ChatMessage> loadMessages(Context context, String targetUsername) {
        List<ChatMessage> list = new ArrayList<>();
        if (targetUsername == null || targetUsername.isEmpty()) return list;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString("history_" + targetUsername, null);

        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String text = obj.optString("text", "");
                    String time = obj.optString("time", "");
                    String senderLogin = obj.optString("senderLogin", "");
                    boolean isOutgoing = obj.optBoolean("isOutgoing", false);
                    long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
                    String imagePath = obj.optString("imagePath", null);
                    String messageId = obj.optString("messageId", null);
                    boolean isEdited = obj.optBoolean("isEdited", false);

                    Bitmap bmp = null;
                    if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
                        bmp = BitmapFactory.decodeFile(imagePath);
                    }

                    ChatMessage msg = new ChatMessage(text, time, senderLogin, isOutgoing, bmp, timestamp, imagePath, messageId);
                    msg.setEdited(isEdited);
                    list.add(msg);
                }
            } catch (Exception e) {
                Log.e("ChatHistoryManager", "Failed to load history", e);
            }
        }
        return list;
    }

    private static void saveHistoryList(Context context, String targetUsername, List<ChatMessage> history) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        try {
            for (ChatMessage msg : history) {
                JSONObject obj = new JSONObject();
                obj.put("messageId", msg.getMessageId() != null ? msg.getMessageId() : "");
                obj.put("text", msg.getText() != null ? msg.getText() : "");
                obj.put("time", msg.getTime() != null ? msg.getTime() : "");
                obj.put("senderLogin", msg.getSenderLogin() != null ? msg.getSenderLogin() : "");
                obj.put("isOutgoing", msg.isOutgoing());
                obj.put("timestamp", msg.getTimestamp());
                obj.put("imagePath", msg.getImagePath() != null ? msg.getImagePath() : "");
                obj.put("isEdited", msg.isEdited());
                array.put(obj);
            }
            prefs.edit().putString("history_" + targetUsername, array.toString()).apply();
        } catch (Exception e) {
            Log.e("ChatHistoryManager", "Failed to save history", e);
        }
    }

    public static String saveBitmapToFile(Context context, Bitmap bitmap, long timestamp) {
        try {
            File file = new File(context.getFilesDir(), "cache_img_" + timestamp + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ChatHistoryManager", "Failed to save bitmap", e);
            return null;
        }
    }
}
