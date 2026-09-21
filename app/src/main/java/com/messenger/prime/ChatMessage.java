package com.messenger.prime;

import android.graphics.Bitmap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ChatMessage {

    public enum MessageType {
        TEXT, IMAGE, VIDEO, FILE
    }

    private String messageId;
    private String text;
    private String time;
    private String senderLogin;
    private boolean isOutgoing;
    private Bitmap imageBitmap;
    private long timestamp;
    private String imagePath;
    private boolean isEdited;
    private MessageStatus messageStatus = MessageStatus.SENT;
    private String reaction = null;
    private String reactionSenderLogin = null;
    private final Map<String, String> reactionsMap = new LinkedHashMap<>();

    private MessageType messageType = MessageType.TEXT;
    private String fileName;
    private long fileSize;
    private String videoDuration;
    private int downloadProgress = 0;
    private boolean isDownloading = false;
    private int sendingProgress = 0;

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
        
        if (imagePath != null && !imagePath.isEmpty()) {
            String lower = imagePath.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".webm") || lower.contains("video")) {
                this.messageType = MessageType.VIDEO;
            } else {
                this.messageType = MessageType.IMAGE;
            }
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(String videoDuration) {
        this.videoDuration = videoDuration;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public int getSendingProgress() {
        return sendingProgress;
    }

    public void setSendingProgress(int sendingProgress) {
        this.sendingProgress = sendingProgress;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public boolean isVideo() {
        if (messageType == MessageType.VIDEO) return true;
        if (imagePath != null) {
            String lower = imagePath.toLowerCase();
            return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".webm");
        }
        return false;
    }

    public boolean isFile() {
        if (messageType == MessageType.FILE) return true;
        return fileName != null && !fileName.isEmpty() && !isVideo();
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
        if (imagePath != null && !imagePath.isEmpty()) {
            String lower = imagePath.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".webm") || lower.contains("video")) {
                this.messageType = MessageType.VIDEO;
            }
        }
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public Map<String, String> getReactionsMap() {
        return new LinkedHashMap<>(reactionsMap);
    }

    public String getReactionForUser(String authorLogin) {
        if (authorLogin == null) return null;
        return reactionsMap.get(authorLogin);
    }

    public void setReactionForUser(String authorLogin, String reaction) {
        if (authorLogin == null || authorLogin.trim().isEmpty()) {
            authorLogin = senderLogin != null ? senderLogin : "unknown";
        }
        if (reaction == null || reaction.trim().isEmpty() || "REMOVE".equalsIgnoreCase(reaction)) {
            reactionsMap.remove(authorLogin);
        } else {
            reactionsMap.put(authorLogin, reaction);
        }
        updateLegacyReactionFields();
    }

    private void updateLegacyReactionFields() {
        if (reactionsMap.isEmpty()) {
            this.reaction = null;
            this.reactionSenderLogin = null;
        } else {
            Map.Entry<String, String> first = reactionsMap.entrySet().iterator().next();
            this.reactionSenderLogin = first.getKey();
            this.reaction = first.getValue();
        }
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        if (reactionSenderLogin != null && !reactionSenderLogin.isEmpty()) {
            setReactionForUser(reactionSenderLogin, reaction);
        } else {
            this.reaction = reaction;
            if (reaction != null && !reaction.isEmpty()) {
                setReactionForUser(senderLogin != null ? senderLogin : "me", reaction);
            } else {
                reactionsMap.clear();
            }
        }
    }

    public String getReactionSenderLogin() {
        return reactionSenderLogin;
    }

    public void setReactionSenderLogin(String reactionSenderLogin) {
        this.reactionSenderLogin = reactionSenderLogin;
        if (this.reaction != null && !this.reaction.isEmpty() && reactionSenderLogin != null) {
            reactionsMap.put(reactionSenderLogin, this.reaction);
        }
    }

    private String replyToMessageId = null;
    private String replyToSender = null;
    private String replyToText = null;

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToSender() {
        return replyToSender;
    }

    public void setReplyToSender(String replyToSender) {
        this.replyToSender = replyToSender;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public boolean isReply() {
        return replyToMessageId != null && !replyToMessageId.trim().isEmpty();
    }
}
