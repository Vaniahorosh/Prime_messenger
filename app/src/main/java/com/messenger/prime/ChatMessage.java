package com.messenger.prime;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatMessage {

    public enum MessageType {
        TEXT, IMAGE, VIDEO, FILE
    }

    public static class MediaItem {
        public String path;
        public boolean isVideo;
        public String durationStr;

        public MediaItem(String path, boolean isVideo, String durationStr) {
            this.path = path;
            this.isVideo = isVideo;
            this.durationStr = durationStr != null ? durationStr : "00:00";
        }
    }

    private List<MediaItem> mediaItems = null;

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
        } else if (imageBitmap != null) {
            this.messageType = MessageType.IMAGE;
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

    public List<MediaItem> getMediaItems() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            return mediaItems;
        }
        List<MediaItem> list = new ArrayList<>();
        if (imagePath != null && !imagePath.isEmpty()) {
            if (imagePath.startsWith("MULTI:")) {
                list.addAll(parseMultiMediaString(imagePath));
            } else {
                list.add(new MediaItem(imagePath, isVideo(), videoDuration));
            }
        }
        this.mediaItems = list;
        return list;
    }

    public void setMediaItems(List<MediaItem> items) {
        this.mediaItems = items != null ? items : new ArrayList<>();
        if (this.mediaItems.size() == 1) {
            MediaItem first = this.mediaItems.get(0);
            this.imagePath = first.path;
            this.videoDuration = first.durationStr;
            this.messageType = first.isVideo ? MessageType.VIDEO : MessageType.IMAGE;
        } else if (this.mediaItems.size() > 1) {
            this.imagePath = buildMultiMediaString(this.mediaItems);
            this.messageType = MessageType.IMAGE;
        }
    }

    public boolean isMultiMedia() {
        return getMediaItems().size() > 1;
    }

    public int getMediaCount() {
        return getMediaItems().size();
    }

    public static String buildMultiMediaString(List<MediaItem> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("MULTI:");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(";");
            MediaItem item = items.get(i);
            sb.append(item.path != null ? item.path : "")
              .append("|")
              .append(item.isVideo ? "1" : "0")
              .append("|")
              .append(item.durationStr != null ? item.durationStr : "");
        }
        return sb.toString();
    }

    public static List<MediaItem> parseMultiMediaString(String multiStr) {
        List<MediaItem> list = new ArrayList<>();
        if (multiStr == null || !multiStr.startsWith("MULTI:")) return list;
        String data = multiStr.substring(6);
        String[] parts = data.split(";");
        for (String part : parts) {
            String[] fields = part.split("\\|", -1);
            if (fields.length >= 2) {
                String path = fields[0];
                boolean isVideo = "1".equals(fields[1]);
                String duration = fields.length >= 3 ? fields[2] : "";
                if (!path.isEmpty()) {
                    list.add(new MediaItem(path, isVideo, duration));
                }
            }
        }
        return list;
    }

    public boolean isVideo() {
        if (messageType == MessageType.VIDEO) return true;
        if (imagePath != null && !imagePath.startsWith("MULTI:")) {
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
        if (imageBitmap != null && (this.messageType == MessageType.TEXT || this.messageType == MessageType.FILE)) {
            this.messageType = MessageType.IMAGE;
        }
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
            } else if (this.messageType == MessageType.TEXT || this.messageType == MessageType.FILE) {
                this.messageType = MessageType.IMAGE;
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
