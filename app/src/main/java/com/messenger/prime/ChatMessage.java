package com.messenger.prime;

import android.graphics.Bitmap;
import java.util.UUID;

public class ChatMessage {
    private String messageId;
    private String text;
    private String time;
    private String senderLogin;
    private boolean isOutgoing;
    private Bitmap imageBitmap;
    private long timestamp;
    private String imagePath;
    private boolean isEdited;

    public ChatMessage(String text, String time, String senderLogin, boolean isOutgoing) {
        this(text, time, senderLogin, isOutgoing, null, System.currentTimeMillis(), null, null);
    }

    public ChatMessage(String text, String time, String senderLogin, boolean isOutgoing, Bitmap imageBitmap) {
        this(text, time, senderLogin, isOutgoing, imageBitmap, System.currentTimeMillis(), null, null);
    }

    public ChatMessage(String text, String time, String senderLogin, boolean isOutgoing, Bitmap imageBitmap, long timestamp, String imagePath) {
        this(text, time, senderLogin, isOutgoing, imageBitmap, timestamp, imagePath, null);
    }

    public ChatMessage(String text, String time, String senderLogin, boolean isOutgoing, Bitmap imageBitmap, long timestamp, String imagePath, String messageId) {
        this.text = text;
        this.time = time;
        this.senderLogin = senderLogin;
        this.isOutgoing = isOutgoing;
        this.imageBitmap = imageBitmap;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
        this.isEdited = false;
        this.messageId = messageId != null && !messageId.isEmpty() ? messageId : (senderLogin + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 4));
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTime() {
        return time;
    }

    public String getSenderLogin() {
        return senderLogin;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }
}
