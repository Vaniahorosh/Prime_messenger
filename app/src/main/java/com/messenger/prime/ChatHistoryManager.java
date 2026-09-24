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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatHistoryManager {

    private static final String PREF_NAME = "PrimeChatHistory";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void deleteHistory(Context context, String targetUsername) {
        deleteHistoryCompletely(context, targetUsername, null);
    }

    public static void deleteHistoryCompletely(Context context, String targetUsername, String deviceAddress) {
        if (context == null) return;
        executor.execute(() -> {
            Context appContext = context.getApplicationContext();
            
            // 1. Удаление всей истории и прикрепленных файлов (фото/видео/документы)
            SharedPreferences historyPrefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor historyEdit = historyPrefs.edit();

            List<String> keysToDelete = new ArrayList<>();
            if (targetUsername != null && !targetUsername.isEmpty()) keysToDelete.add(targetUsername);
            if (deviceAddress != null && !deviceAddress.isEmpty() && !deviceAddress.equalsIgnoreCase(targetUsername)) keysToDelete.add(deviceAddress);

            for (String key : keysToDelete) {
                List<ChatMessage> history = loadMessages(appContext, key);
                for (ChatMessage msg : history) {
                    if (msg.getImagePath() != null && !msg.getImagePath().isEmpty()) {
                        try {
                            File file = new File(msg.getImagePath());
                            if (file.exists()) file.delete();
                        } catch (Exception ignored) {}
                    }
                }
                historyEdit.remove("history_" + key);
            }

            Map<String, ?> allHistoryKeys = historyPrefs.getAll();
            for (String key : allHistoryKeys.keySet()) {
                for (String tKey : keysToDelete) {
                    if (key.equalsIgnoreCase("history_" + tKey) || key.toLowerCase().endsWith(tKey.toLowerCase())) {
                        historyEdit.remove(key);
                    }
                }
            }
            historyEdit.apply();

            // 2. Полное физическое удаление всех файлов аватарок, медиа и кэша, связанных с контактом
            try {
                File[] files = appContext.getFilesDir().listFiles();
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName().toLowerCase();
                        for (String tKey : keysToDelete) {
                            String lowerKey = tKey.toLowerCase();
                            if (name.contains(lowerKey) || name.equals("avatar_" + lowerKey + ".jpg")) {
                                f.delete();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                File[] cacheFiles = appContext.getCacheDir().listFiles();
                if (cacheFiles != null) {
                    for (File f : cacheFiles) {
                        String name = f.getName().toLowerCase();
                        for (String tKey : keysToDelete) {
                            if (name.contains(tKey.toLowerCase())) {
                                f.delete();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 3. Полное удаление метрик последнего визита, статусов набора текста и записи из списка чатов
            SharedPreferences localDb = appContext.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            SharedPreferences.Editor dbEdit = localDb.edit();
            for (String tKey : keysToDelete) {
                dbEdit.remove("last_seen_" + tKey);
                dbEdit.remove("typing_until_" + tKey);
                dbEdit.remove("activity_state_" + tKey);
            }

            try {
                String json = localDb.getString("persisted_chats", "[]");
                JSONArray array = new JSONArray(json);
                JSONArray newArray = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String objName = obj.optString("name");
                    String objId = obj.optString("id");
                    boolean matchName = targetUsername != null && targetUsername.equalsIgnoreCase(objName);
                    boolean matchId = deviceAddress != null && deviceAddress.equalsIgnoreCase(objId);
                    boolean matchNameAsId = targetUsername != null && targetUsername.equalsIgnoreCase(objId);
                    boolean matchIdAsName = deviceAddress != null && deviceAddress.equalsIgnoreCase(objName);

                    if (!matchName && !matchId && !matchNameAsId && !matchIdAsName) {
                        newArray.put(obj);
                    }
                }
                dbEdit.putString("persisted_chats", newArray.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            dbEdit.apply();
            ChatListNotifier.INSTANCE.notifyChanged();
        });
    }

    public static void deleteSingleMessage(Context context, String targetUsername, String messageId) {
        if (targetUsername == null || targetUsername.isEmpty() || messageId == null || messageId.isEmpty()) return;
        executor.execute(() -> {
            Context appContext = context.getApplicationContext();
            List<ChatMessage> history = loadMessages(appContext, targetUsername);
            boolean removed = false;
            for (int i = 0; i < history.size(); i++) {
                if (messageId.equals(history.get(i).getMessageId())) {
                    ChatMessage msg = history.remove(i);
                    if (msg.getImagePath() != null && !msg.getImagePath().isEmpty()) {
                        try {
                            File file = new File(msg.getImagePath());
                            if (file.exists()) file.delete();
                        } catch (Exception ignored) {}
                    }
                    removed = true;
                    break;
                }
            }
            if (removed) {
                saveHistoryList(appContext, targetUsername, history);
            }
        });
    }

    public static void saveMessage(Context context, String targetUsername, ChatMessage message) {
        if (targetUsername == null || targetUsername.isEmpty()) return;

        executor.execute(() -> {
            Context appContext = context.getApplicationContext();
            List<ChatMessage> history = loadMessages(appContext, targetUsername);
            
            // Handle photo saving if bitmap exists but imagePath doesn't
            if (message.getImageBitmap() != null && (message.getImagePath() == null || message.getImagePath().isEmpty())) {
                String path = saveBitmapToFile(appContext, message.getImageBitmap(), message.getTimestamp());
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

            saveHistoryList(appContext, targetUsername, history);
        });
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
                    if (text.startsWith("HANDSHAKE:")) continue;
                    String time = obj.optString("time", "");
                    String senderLogin = obj.optString("senderLogin", "");
                    boolean isOutgoing = obj.optBoolean("isOutgoing", false);
                    long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
                    String imagePath = obj.optString("imagePath", null);
                    String messageId = obj.optString("messageId", null);
                    boolean isEdited = obj.optBoolean("isEdited", false);
                    String msgStatusStr = obj.optString("messageStatus", MessageStatus.SENT.name());
                    String msgTypeStr = obj.optString("messageType", ChatMessage.MessageType.TEXT.name());
                    String fileName = obj.optString("fileName", null);
                    long fileSize = obj.optLong("fileSize", 0L);
                    String videoDuration = obj.optString("videoDuration", null);

                    Bitmap bmp = null;
                    if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
                        bmp = BitmapFactory.decodeFile(imagePath);
                    }

                    ChatMessage msg = new ChatMessage(text, time, senderLogin, isOutgoing, bmp, timestamp, imagePath, messageId);
                    msg.setEdited(isEdited);
                    msg.setFileName(fileName);
                    msg.setFileSize(fileSize);
                    msg.setVideoDuration(videoDuration);
                    try {
                        msg.setMessageType(ChatMessage.MessageType.valueOf(msgTypeStr));
                    } catch (Exception ignored) {}

                    JSONObject reactionsObj = obj.optJSONObject("reactions");
                    if (reactionsObj != null) {
                        Iterator<String> keys = reactionsObj.keys();
                        while (keys.hasNext()) {
                            String rxAuthor = keys.next();
                            String emoji = reactionsObj.optString(rxAuthor, "");
                            if (!emoji.isEmpty()) {
                                msg.setReactionForUser(rxAuthor, emoji);
                            }
                        }
                    } else {
                        String reactionStr = obj.optString("reaction", "");
                        String reactionAuthor = obj.optString("reactionSenderLogin", "");
                        if (!reactionStr.isEmpty()) {
                            msg.setReactionForUser(!reactionAuthor.isEmpty() ? reactionAuthor : senderLogin, reactionStr);
                        }
                    }

                    String replyToMessageId = obj.optString("replyToMessageId", null);
                    String replyToSender = obj.optString("replyToSender", null);
                    String replyToText = obj.optString("replyToText", null);
                    if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
                        msg.setReplyToMessageId(replyToMessageId);
                        msg.setReplyToSender(replyToSender);
                        msg.setReplyToText(replyToText);
                    }

                    try {
                        msg.setMessageStatus(MessageStatus.valueOf(msgStatusStr));
                    } catch (Exception ignored) {}
                    list.add(msg);
                }
            } catch (Exception e) {
                Log.e("ChatHistoryManager", "Failed to load history", e);
            }
        }
        return list;
    }

    public static void saveHistoryList(Context context, String targetUsername, List<ChatMessage> history) {
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
                obj.put("messageStatus", msg.getMessageStatus() != null ? msg.getMessageStatus().name() : MessageStatus.SENT.name());
                obj.put("messageType", msg.getMessageType() != null ? msg.getMessageType().name() : ChatMessage.MessageType.TEXT.name());
                obj.put("fileName", msg.getFileName() != null ? msg.getFileName() : "");
                obj.put("fileSize", msg.getFileSize());
                obj.put("videoDuration", msg.getVideoDuration() != null ? msg.getVideoDuration() : "");
                obj.put("replyToMessageId", msg.getReplyToMessageId() != null ? msg.getReplyToMessageId() : "");
                obj.put("replyToSender", msg.getReplyToSender() != null ? msg.getReplyToSender() : "");
                obj.put("replyToText", msg.getReplyToText() != null ? msg.getReplyToText() : "");
                
                JSONObject reactionsObj = new JSONObject();
                for (Map.Entry<String, String> entry : msg.getReactionsMap().entrySet()) {
                    reactionsObj.put(entry.getKey(), entry.getValue());
                }
                obj.put("reactions", reactionsObj);
                obj.put("reaction", msg.getReaction() != null ? msg.getReaction() : "");
                obj.put("reactionSenderLogin", msg.getReactionSenderLogin() != null ? msg.getReactionSenderLogin() : "");
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
