package com.messenger.prime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.ComponentActivity;
import androidx.core.content.FileProvider;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import com.google.android.material.imageview.ShapeableImageView;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }

    public interface OnMessageActionListener {
        void onEditMessage(ChatMessage message, int position);
        void onDeleteMessage(ChatMessage message, int position);
        void onQuickReaction(ChatMessage message, String reaction, int position);
        void onReplyMessage(ChatMessage message, int position);
        void onReplyToSelectedText(ChatMessage message, String selectedText, int position);
        void onForwardMessage(ChatMessage message, int position);
        void onJumpToMessage(String messageId);
        void onJumpToMessage(String messageId, String quotedText);
        void onCancelSending(ChatMessage message, int position);
    }

    private List<ChatMessage> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;
    private OnMessageActionListener actionListener;
    private String localUsername = "";
    private boolean isConnectionActive = false;

    public void setConnectionActive(boolean active) {
        this.isConnectionActive = active;
    }

    public boolean isConnectionActive() {
        return isConnectionActive;
    }

    public void setLocalUsername(String username) {
        this.localUsername = username;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateMessageSendingProgress(String messageId, int progress) {
        if (messageId == null) return;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (messageId.equals(msg.getMessageId())) {
                msg.setSendingProgress(progress);
                if (progress >= 100) {
                    msg.setMessageStatus(MessageStatus.SENT);
                }
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages = new ArrayList<>(newMessages != null ? newMessages : new ArrayList<>());
        notifyDataSetChanged();
    }

    public ChatMessage getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    public void deleteMessageAnimated(View itemView, int position, Runnable onComplete) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
        if (itemView != null) {
            itemView.animate()
                    .alpha(0f)
                    .scaleX(0.1f)
                    .scaleY(0.1f)
                    .translationY(-40f)
                    .setDuration(220)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        itemView.setAlpha(1f);
                        itemView.setScaleX(1f);
                        itemView.setScaleY(1f);
                        itemView.setTranslationY(0f);
                        if (onComplete != null) onComplete.run();
                    })
                    .start();
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public void deleteMessageByIdAnimated(RecyclerView recyclerView, String messageId, Runnable onComplete) {
        if (messageId == null || messages == null || messages.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        int foundPos = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                foundPos = i;
                break;
            }
        }

        if (foundPos == -1) {
            String tsPart = messageId.contains("_") ? messageId.split("_")[1] : null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);
                if (tsPart != null && m.getMessageId() != null && m.getMessageId().contains(tsPart)) {
                    foundPos = i;
                    break;
                }
            }
        }

        if (foundPos != -1 && foundPos < messages.size()) {
            final int pos = foundPos;
            RecyclerView.ViewHolder holder = recyclerView != null ? recyclerView.findViewHolderForAdapterPosition(pos) : null;
            View viewToAnimate = holder != null ? holder.itemView : null;
            deleteMessageAnimated(viewToAnimate, pos, onComplete);
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public void updateMessageById(String messageId, String newText) {
        updateMessageTextById(messageId, newText);
    }

    public void updateMessageStatusById(String messageId, MessageStatus status) {
        if (messages.isEmpty()) return;
        boolean found = false;
        if (messageId != null && !messageId.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);
                if (messageId.equals(m.getMessageId())) {
                    m.setMessageStatus(status);
                    notifyItemChanged(i);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);
                if (m.isOutgoing()) {
                    m.setMessageStatus(status);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    public void updateMessageTextById(String messageId, String newText) {
        if (messageId == null || messageId.isEmpty()) return;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                messages.get(i).setEdited(true);
                messages.get(i).setText(newText);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateMessageReactionById(String messageId, String reaction) {
        updateMessageReactionById(messageId, reaction, null);
    }

    public void updateMessageReactionById(String messageId, String reaction, String authorLogin) {
        if (messageId == null || messages.isEmpty()) return;
        String author = (authorLogin != null && !authorLogin.trim().isEmpty()) ? authorLogin : localUsername;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                messages.get(i).setReactionForUser(author, reaction);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateMessageByTimestamp(long timestamp, String newText) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getTimestamp() == timestamp) {
                messages.get(i).setEdited(true);
                messages.get(i).setText(newText);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).isOutgoing()) {
            return VIEW_TYPE_OUTGOING;
        } else {
            return VIEW_TYPE_INCOMING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OUTGOING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_outgoing, parent, false);
            return new OutgoingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_incoming, parent, false);
            return new IncomingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean showDateHeader = false;
        if (position == 0) {
            showDateHeader = true;
        } else if (isDifferentDay(messages.get(position - 1).getTimestamp(), message.getTimestamp())) {
            showDateHeader = true;
        }

        if (holder.getItemViewType() == VIEW_TYPE_OUTGOING) {
            ((OutgoingViewHolder) holder).bind(message, showDateHeader, longClickListener, position);
        } else {
            ((IncomingViewHolder) holder).bind(message, showDateHeader, longClickListener, position);
        }
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        holder.itemView.setTranslationX(0f);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("HIGHLIGHT")) {
            View view = holder.itemView;
            view.animate().cancel();
            ObjectAnimator flicker = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.15f, 1f, 0.15f, 1f, 0.35f, 1f);
            flicker.setDuration(1200);
            flicker.setInterpolator(new AccelerateDecelerateInterpolator());
            flicker.start();
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public int findPositionByMessageId(String messageId) {
        if (messageId == null || messages == null || messages.isEmpty()) return -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                return i;
            }
        }
        return -1;
    }

    public void highlightMessageAtPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, "HIGHLIGHT");
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        holder.itemView.setTranslationX(0f);
        if (holder instanceof IncomingViewHolder) {
            if (((IncomingViewHolder) holder).videoMessagePreview != null) {
                ((IncomingViewHolder) holder).videoMessagePreview.stopPlayback();
            }
        } else if (holder instanceof OutgoingViewHolder) {
            if (((OutgoingViewHolder) holder).videoMessagePreview != null) {
                ((OutgoingViewHolder) holder).videoMessagePreview.stopPlayback();
            }
        }
    }

    public ChatMessage getMessageAt(int position) {
        if (position >= 0 && position < messages.size()) {
            return messages.get(position);
        }
        return null;
    }

    public long getMessageTimestamp(int position) {
        if (position >= 0 && position < messages.size()) {
            return messages.get(position).getTimestamp();
        }
        return 0;
    }

    public int findDateSectionStartPosition(int currentVisiblePos) {
        if (messages == null || messages.isEmpty()) return -1;
        if (currentVisiblePos < 0) currentVisiblePos = 0;
        if (currentVisiblePos >= messages.size()) currentVisiblePos = messages.size() - 1;

        long currentTs = messages.get(currentVisiblePos).getTimestamp();
        if (currentTs <= 0) return currentVisiblePos;

        int startPos = currentVisiblePos;
        for (int i = currentVisiblePos; i >= 0; i--) {
            long ts = messages.get(i).getTimestamp();
            if (ts > 0 && !isDifferentDay(ts, currentTs)) {
                startPos = i;
            } else if (ts > 0 && isDifferentDay(ts, currentTs)) {
                break;
            }
        }

        if (startPos == currentVisiblePos && startPos > 0) {
            long prevTs = messages.get(startPos - 1).getTimestamp();
            if (prevTs > 0) {
                for (int i = startPos - 1; i >= 0; i--) {
                    long ts = messages.get(i).getTimestamp();
                    if (ts > 0 && !isDifferentDay(ts, prevTs)) {
                        startPos = i;
                    } else if (ts > 0 && isDifferentDay(ts, prevTs)) {
                        break;
                    }
                }
            }
        }

        return startPos;
    }

    private static boolean isDifferentDay(long t1, long t2) {
        if (t1 <= 0 || t2 <= 0) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(t1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(t2);
        return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
               cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String getDateHeaderString(long timestamp) {
        if (timestamp <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
            return "Сегодня";
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
            return "Вчера";
        }

        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(timestamp));
    }

    private void setAnimation(View viewToAnimate) {
        viewToAnimate.setAlpha(0f);
        viewToAnimate.setTranslationY(20f);
        viewToAnimate.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }



    private static String formatTime(long timestamp, String fallbackTime, boolean isEdited) {
        String baseTime;
        if (timestamp <= 0) {
            baseTime = fallbackTime != null ? fallbackTime : "";
        } else {
            baseTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        return isEdited ? "ред. " + baseTime : baseTime;
    }

    public static void showFullScreenMedia(Context context, View view, ChatMessage clickedMessage, List<ChatMessage> allMessages) {
        showFullScreenMedia(context, view, clickedMessage, allMessages, 0);
    }

    public static void showFullScreenMedia(Context context, View view, ChatMessage clickedMessage, List<ChatMessage> allMessages, int selectedMediaIndex) {
        if (context == null) return;
        if (context instanceof ChatPersonActivity) {
            ((ChatPersonActivity) context).setOpeningSubActivity(true);
        }

        List<ChatMessage> mediaMessages = new ArrayList<>();
        int startIndex = 0;
        
        if (allMessages != null) {
            for (ChatMessage msg : allMessages) {
                if (msg.isMultiMedia() || (msg.getImagePath() != null && msg.getImagePath().startsWith("MULTI:"))) {
                    List<ChatMessage.MediaItem> items = msg.getMediaItems();
                    int baseIndex = mediaMessages.size();
                    for (int i = 0; i < items.size(); i++) {
                        ChatMessage.MediaItem item = items.get(i);
                        ChatMessage subMsg = new ChatMessage(msg.getText(), msg.getTime(), msg.getSenderLogin(), msg.isOutgoing(), null, msg.getTimestamp(), item.path, msg.getMessageId() + "_item_" + i);
                        subMsg.setMessageType(item.isVideo ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE);
                        subMsg.setVideoDuration(item.durationStr);
                        mediaMessages.add(subMsg);
                    }
                    if (clickedMessage != null && msg.getMessageId() != null && msg.getMessageId().equals(clickedMessage.getMessageId())) {
                        int offset = (selectedMediaIndex >= 0 && selectedMediaIndex < items.size()) ? selectedMediaIndex : 0;
                        startIndex = baseIndex + offset;
                    }
                } else if (msg.getMessageType() == ChatMessage.MessageType.IMAGE || 
                           msg.getMessageType() == ChatMessage.MessageType.VIDEO || 
                           msg.isVideo() || 
                           msg.getImageBitmap() != null || 
                           (msg.getImagePath() != null && !msg.getImagePath().isEmpty())) {
                    
                    if (msg.getMessageType() == ChatMessage.MessageType.TEXT || msg.getMessageType() == ChatMessage.MessageType.FILE) {
                        msg.setMessageType(msg.isVideo() ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE);
                    }
                    mediaMessages.add(msg);
                    if (clickedMessage != null && msg.getMessageId() != null && msg.getMessageId().equals(clickedMessage.getMessageId())) {
                        startIndex = mediaMessages.size() - 1;
                    }
                }
            }
        }
        
        // If clickedMessage is not in the list (e.g. standalone call), just add it
        if (mediaMessages.isEmpty() && clickedMessage != null) {
            if (clickedMessage.isMultiMedia() || (clickedMessage.getImagePath() != null && clickedMessage.getImagePath().startsWith("MULTI:"))) {
                List<ChatMessage.MediaItem> items = clickedMessage.getMediaItems();
                for (int i = 0; i < items.size(); i++) {
                    ChatMessage.MediaItem item = items.get(i);
                    ChatMessage subMsg = new ChatMessage(clickedMessage.getText(), clickedMessage.getTime(), clickedMessage.getSenderLogin(), clickedMessage.isOutgoing(), null, clickedMessage.getTimestamp(), item.path, clickedMessage.getMessageId() + "_item_" + i);
                    subMsg.setMessageType(item.isVideo ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE);
                    subMsg.setVideoDuration(item.durationStr);
                    mediaMessages.add(subMsg);
                }
                int offset = (selectedMediaIndex >= 0 && selectedMediaIndex < items.size()) ? selectedMediaIndex : 0;
                startIndex = offset;
            } else {
                if (clickedMessage.getMessageType() == ChatMessage.MessageType.TEXT || clickedMessage.getMessageType() == ChatMessage.MessageType.FILE) {
                    clickedMessage.setMessageType(clickedMessage.isVideo() ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE);
                }
                mediaMessages.add(clickedMessage);
                startIndex = 0;
            }
        }

        MediaPlayerActivity.setSharedMediaList(mediaMessages, startIndex);

        Intent intent = new Intent(context, MediaPlayerActivity.class);
        if (view != null) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
            intent.putExtra("EXTRA_RECT", rect);
        }
        context.startActivity(intent);
    }

    public static void showFullScreenPhoto(Context context, View view, Bitmap bitmap, String path) {
        ChatMessage dummy = new ChatMessage("", "", "", false);
        dummy.setImageBitmap(bitmap);
        dummy.setImagePath(path);
        dummy.setMessageType(ChatMessage.MessageType.IMAGE);
        showFullScreenMedia(context, view, dummy, null);
    }

    public static void showFullScreenPhoto(Context context, Bitmap bitmap, String path) {
        ChatMessage dummy = new ChatMessage("", "", "", false);
        dummy.setImageBitmap(bitmap);
        dummy.setImagePath(path);
        dummy.setMessageType(ChatMessage.MessageType.IMAGE);
        showFullScreenMedia(context, null, dummy, null);
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void bindLinkPreview(View itemView, String messageText) {
        View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
        TextView tvLinkPreviewTitle = itemView.findViewById(R.id.tvLinkPreviewTitle);
        TextView tvLinkPreviewUrl = itemView.findViewById(R.id.tvLinkPreviewUrl);

        if (layoutLinkPreview != null) {
            String url = extractUrl(messageText);
            if (url != null && !url.isEmpty()) {
                layoutLinkPreview.setVisibility(View.VISIBLE);
                if (tvLinkPreviewTitle != null) {
                    try {
                        Uri uri = Uri.parse(url);
                        String host = uri.getHost();
                        tvLinkPreviewTitle.setText(host != null ? host : url);
                    } catch (Exception e) {
                        tvLinkPreviewTitle.setText(url);
                    }
                }
                if (tvLinkPreviewUrl != null) {
                    tvLinkPreviewUrl.setText(url);
                }
                layoutLinkPreview.setOnClickListener(v -> {
                    try {
                        String targetUrl = url;
                        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                            targetUrl = "https://" + targetUrl;
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e("ChatAdapter", "Failed to open link", e);
                    }
                });
            } else {
                layoutLinkPreview.setVisibility(View.GONE);
            }
        }
    }

    public static String extractUrl(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = Patterns.WEB_URL.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void bindMessageAttachments(
            ChatMessage message,
            int position,
            View layoutMediaContainer,
            ImageView ivMessageImage,
            VideoView videoMessagePreview,
            ImageView ivVideoPlayBadge,
            TextView tvVideoDuration,
            View layoutFileContainer,
            ImageView ivFileIcon,
            TextView tvFileName,
            TextView tvFileSize,
            ImageButton btnFileDownload,
            View layoutDownloadProgress,
            ProgressBar pbDownloadProgress,
            TextView tvDownloadPercent,
            ImageButton btnCancelDownload
    ) {
        if (message.isFile()) {
            if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
            if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
            if (layoutFileContainer != null) {
                layoutFileContainer.setVisibility(View.VISIBLE);
                if (tvFileName != null) {
                    tvFileName.setText(message.getFileName() != null && !message.getFileName().isEmpty() ? message.getFileName() : "Документ");
                }
                if (tvFileSize != null) {
                    tvFileSize.setText(formatFileSize(message.getFileSize()));
                }

                if (message.isDownloading()) {
                    if (btnFileDownload != null) btnFileDownload.setVisibility(View.GONE);
                    if (layoutDownloadProgress != null) layoutDownloadProgress.setVisibility(View.VISIBLE);
                    if (btnCancelDownload != null) btnCancelDownload.setVisibility(View.VISIBLE);
                    if (pbDownloadProgress != null) pbDownloadProgress.setProgress(message.getDownloadProgress());
                    if (tvDownloadPercent != null) tvDownloadPercent.setText(message.getDownloadProgress() + "%");
                } else {
                    boolean isDownloaded = message.getImagePath() != null && new File(message.getImagePath()).exists();
                    if (isDownloaded) {
                        if (btnFileDownload != null) btnFileDownload.setVisibility(View.GONE);
                        if (layoutDownloadProgress != null) layoutDownloadProgress.setVisibility(View.GONE);
                        if (btnCancelDownload != null) btnCancelDownload.setVisibility(View.GONE);
                    } else {
                        if (btnFileDownload != null) btnFileDownload.setVisibility(View.VISIBLE);
                        if (layoutDownloadProgress != null) layoutDownloadProgress.setVisibility(View.GONE);
                        if (btnCancelDownload != null) btnCancelDownload.setVisibility(View.GONE);
                    }
                }

                if (btnFileDownload != null) {
                    btnFileDownload.setOnClickListener(v -> startDownloadSimulation(message, position));
                }
                if (btnCancelDownload != null) {
                    btnCancelDownload.setOnClickListener(v -> cancelDownloadSimulation(message, position));
                }

                layoutFileContainer.setOnClickListener(v -> {
                    String path = message.getImagePath();
                    if (path != null && new File(path).exists()) {
                        openFile(v.getContext(), path);
                    } else if (!message.isDownloading()) {
                        startDownloadSimulation(message, position);
                    }
                });
            }
        } else {
            if (layoutFileContainer != null) layoutFileContainer.setVisibility(View.GONE);

            List<ChatMessage.MediaItem> mediaItems = message.getMediaItems();
            if (mediaItems != null && !mediaItems.isEmpty()) {
                if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.VISIBLE);

                View layoutSingleMedia = layoutMediaContainer != null ? layoutMediaContainer.findViewById(R.id.layoutSingleMedia) : null;
                View layoutGridMedia = layoutMediaContainer != null ? layoutMediaContainer.findViewById(R.id.layoutGridMedia) : null;
                View layoutSliderMedia = layoutMediaContainer != null ? layoutMediaContainer.findViewById(R.id.layoutSliderMedia) : null;

                int count = mediaItems.size();

                if (count == 1) {
                    if (layoutSingleMedia != null) layoutSingleMedia.setVisibility(View.VISIBLE);
                    if (layoutGridMedia != null) layoutGridMedia.setVisibility(View.GONE);
                    if (layoutSliderMedia != null) layoutSliderMedia.setVisibility(View.GONE);

                    ChatMessage.MediaItem single = mediaItems.get(0);
                    bindSingleMediaItem(single, layoutSingleMedia != null ? layoutSingleMedia : layoutMediaContainer, message, ivMessageImage, videoMessagePreview, ivVideoPlayBadge, tvVideoDuration);
                } else if (count == 2) {
                    if (layoutSingleMedia != null) layoutSingleMedia.setVisibility(View.GONE);
                    if (layoutGridMedia != null) layoutGridMedia.setVisibility(View.VISIBLE);
                    if (layoutSliderMedia != null) layoutSliderMedia.setVisibility(View.GONE);

                    bindGridMediaItems(mediaItems, layoutGridMedia, message);
                } else { // 3, 4, 5 items
                    if (layoutSingleMedia != null) layoutSingleMedia.setVisibility(View.GONE);
                    if (layoutGridMedia != null) layoutGridMedia.setVisibility(View.GONE);
                    if (layoutSliderMedia != null) layoutSliderMedia.setVisibility(View.VISIBLE);

                    bindSliderMediaItems(mediaItems, layoutSliderMedia, message);
                }
            } else {
                if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
            }
        }
    }

    private void bindSingleMediaItem(ChatMessage.MediaItem item, View container, ChatMessage message, ImageView ivImage, VideoView vvPreview, ImageView ivPlay, TextView tvDur) {
        if (container == null) return;
        ImageView img = ivImage != null ? ivImage : container.findViewById(R.id.ivMessageImage);
        VideoView vv = vvPreview != null ? vvPreview : container.findViewById(R.id.videoMessagePreview);
        ImageView play = ivPlay != null ? ivPlay : container.findViewById(R.id.ivVideoPlayBadge);
        TextView dur = tvDur != null ? tvDur : container.findViewById(R.id.tvVideoDuration);

        if (item.isVideo) {
            if (img != null) img.setVisibility(View.GONE);
            if (vv != null && item.path != null && new File(item.path).exists()) {
                vv.setVisibility(View.VISIBLE);
                vv.setVideoURI(Uri.fromFile(new File(item.path)));
                vv.setOnPreparedListener(mp -> {
                    mp.setVolume(0f, 0f);
                    mp.setLooping(true);
                    vv.start();
                });
                vv.setOnErrorListener((mp, what, extra) -> true);
            } else if (img != null) {
                img.setVisibility(View.VISIBLE);
                Bitmap thumb = getVideoThumbnail(item.path);
                if (thumb != null) img.setImageBitmap(thumb);
                else img.setImageResource(R.drawable.ic_video);
            }
            if (play != null) play.setVisibility(View.GONE);
            if (dur != null) {
                dur.setVisibility(View.VISIBLE);
                dur.setText(item.durationStr != null && !item.durationStr.isEmpty() ? item.durationStr : "00:00");
            }
        } else {
            if (vv != null) vv.setVisibility(View.GONE);
            if (img != null) {
                img.setVisibility(View.VISIBLE);
                if (item.path != null && new File(item.path).exists()) {
                    img.setImageBitmap(decodeSampledBitmapFromFile(item.path, 512, 512));
                } else {
                    img.setImageResource(R.drawable.ic_photo);
                }
            }
            if (play != null) play.setVisibility(View.GONE);
            if (dur != null) dur.setVisibility(View.GONE);
        }

        View.OnClickListener clickListener = v -> showFullScreenMedia(v.getContext(), v, message, messages);
        if (img != null) img.setOnClickListener(clickListener);
        if (vv != null) vv.setOnClickListener(clickListener);
        if (container != null) container.setOnClickListener(clickListener);
    }

    private void bindGridMediaItems(List<ChatMessage.MediaItem> items, View container, ChatMessage message) {
        if (container == null || items.size() < 2) return;
        ImageView iv1 = container.findViewById(R.id.ivGridTile1);
        ImageView play1 = container.findViewById(R.id.ivGridPlay1);
        ImageView iv2 = container.findViewById(R.id.ivGridTile2);
        ImageView play2 = container.findViewById(R.id.ivGridPlay2);
        View tile1 = container.findViewById(R.id.layoutGridTile1);
        View tile2 = container.findViewById(R.id.layoutGridTile2);

        bindTile(items.get(0), iv1, play1, tile1, message, 0);
        bindTile(items.get(1), iv2, play2, tile2, message, 1);
    }

    private void bindTile(ChatMessage.MediaItem item, ImageView iv, ImageView play, View tile, ChatMessage message, int itemIndex) {
        if (item.isVideo) {
            if (play != null) play.setVisibility(View.VISIBLE);
            Bitmap thumb = getVideoThumbnail(item.path);
            if (thumb != null && iv != null) iv.setImageBitmap(thumb);
            else if (iv != null) iv.setImageResource(R.drawable.ic_video);
        } else {
            if (play != null) play.setVisibility(View.GONE);
            if (iv != null) {
                if (item.path != null && new File(item.path).exists()) {
                    iv.setImageBitmap(decodeSampledBitmapFromFile(item.path, 512, 512));
                } else {
                    iv.setImageResource(R.drawable.ic_photo);
                }
            }
        }
        View.OnClickListener clickListener = v -> showFullScreenMedia(v.getContext(), v, message, messages, itemIndex);
        if (tile != null) tile.setOnClickListener(clickListener);
        if (iv != null) iv.setOnClickListener(clickListener);
    }

    private void bindSliderMediaItems(List<ChatMessage.MediaItem> items, View container, ChatMessage message) {
        if (container == null) return;
        ViewPager2 vp = container.findViewById(R.id.vpMediaSlider);
        TextView tvIndicator = container.findViewById(R.id.tvSliderIndicator);

        if (vp != null) {
            MediaSliderAdapter sliderAdapter = new MediaSliderAdapter(items, (item, pos) -> showFullScreenMedia(container.getContext(), vp, message, messages, pos));
            vp.setAdapter(sliderAdapter);

            if (tvIndicator != null) {
                tvIndicator.setText("1 из " + items.size());
            }

            vp.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            });

            try {
                View child = vp.getChildAt(0);
                if (child != null) {
                    child.setOnTouchListener((v, event) -> {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_MOVE:
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                        }
                        return false;
                    });
                }
            } catch (Exception ignored) {}

            vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (tvIndicator != null) {
                        tvIndicator.setText((position + 1) + " из " + items.size());
                    }
                }
            });
        }
    }

    private void showMediaItemFull(Context context, ChatMessage.MediaItem item, ChatMessage message) {
        if (context == null || item == null) return;
        String path = item.path != null ? item.path : (message != null ? message.getImagePath() : null);
        boolean isVid = item.isVideo;

        ChatMessage dummy = new ChatMessage("", "", message != null ? message.getSenderLogin() : "", false);
        if (path != null) dummy.setImagePath(path);
        dummy.setMessageType(isVid ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE);
        if (message != null) {
            dummy.setText(message.getText());
            dummy.setTimestamp(message.getTimestamp());
        }

        List<ChatMessage> list = new ArrayList<>();
        list.add(dummy);
        MediaPlayerActivity.setSharedMediaList(list, 0);

        Intent intent = new Intent(context, MediaPlayerActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static class MediaSliderAdapter extends RecyclerView.Adapter<MediaSliderAdapter.SliderViewHolder> {
        private final List<ChatMessage.MediaItem> items;
        private final OnMediaItemClickListener clickListener;

        public interface OnMediaItemClickListener {
            void onItemClick(ChatMessage.MediaItem item, int position);
        }

        public MediaSliderAdapter(List<ChatMessage.MediaItem> items, OnMediaItemClickListener clickListener) {
            this.items = items != null ? items : new ArrayList<>();
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slider_media_page, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            ChatMessage.MediaItem item = items.get(position);
            if (item.isVideo) {
                holder.ivSliderPlayBadge.setVisibility(View.VISIBLE);
                holder.tvSliderVideoDuration.setVisibility(View.VISIBLE);
                holder.tvSliderVideoDuration.setText(item.durationStr != null && !item.durationStr.isEmpty() ? item.durationStr : "00:00");
                Bitmap thumb = getVideoThumbnail(item.path);
                if (thumb != null) {
                    holder.ivSliderImage.setImageBitmap(thumb);
                } else {
                    holder.ivSliderImage.setImageResource(R.drawable.ic_video);
                }
            } else {
                holder.ivSliderPlayBadge.setVisibility(View.GONE);
                holder.tvSliderVideoDuration.setVisibility(View.GONE);
                if (item.path != null && new File(item.path).exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(item.path);
                    holder.ivSliderImage.setImageBitmap(bmp);
                } else {
                    holder.ivSliderImage.setImageResource(R.drawable.ic_photo);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView ivSliderImage;
            ImageView ivSliderPlayBadge;
            TextView tvSliderVideoDuration;

            SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                ivSliderImage = itemView.findViewById(R.id.ivSliderImage);
                ivSliderPlayBadge = itemView.findViewById(R.id.ivSliderPlayBadge);
                tvSliderVideoDuration = itemView.findViewById(R.id.tvSliderVideoDuration);
            }
        }
    }

    public static Bitmap getVideoThumbnail(String pathOrUri) {
        if (pathOrUri == null || pathOrUri.isEmpty()) return null;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            if (pathOrUri.startsWith("content://")) {
                return null;
            } else {
                retriever.setDataSource(pathOrUri);
            }
            return retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        if (path == null || path.isEmpty() || !new File(path).exists()) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void startDownloadSimulation(ChatMessage message, int position) {
        if (message.isDownloading()) return;
        message.setDownloading(true);
        message.setDownloadProgress(0);
        int idx = messages.indexOf(message);
        if (idx != -1) {
            notifyItemChanged(idx);
        } else if (position >= 0 && position < messages.size()) {
            notifyItemChanged(position);
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!message.isDownloading()) return;
                int cur = message.getDownloadProgress() + 15;
                int currentIdx = messages.indexOf(message);
                if (cur >= 100) {
                    message.setDownloadProgress(100);
                    message.setDownloading(false);
                    if (currentIdx != -1) {
                        notifyItemChanged(currentIdx);
                    }
                } else {
                    message.setDownloadProgress(cur);
                    if (currentIdx != -1) {
                        notifyItemChanged(currentIdx);
                    }
                    handler.postDelayed(this, 150);
                }
            }
        };
        handler.postDelayed(runnable, 150);
    }

    private void cancelDownloadSimulation(ChatMessage message, int position) {
        message.setDownloading(false);
        message.setDownloadProgress(0);
        int idx = messages.indexOf(message);
        if (idx != -1) {
            notifyItemChanged(idx);
        } else if (position >= 0 && position < messages.size()) {
            notifyItemChanged(position);
        }
    }

    public static void openFile(Context context, String filePath) {
        if (context == null || filePath == null || filePath.isEmpty()) return;
        try {
            Uri uri;
            if (filePath.startsWith("content://")) {
                uri = Uri.parse(filePath);
            } else {
                File file = new File(filePath);
                if (!file.exists()) {
                    if (context instanceof Activity) {
                        PrimeNotification.INSTANCE.show((Activity) context, "Файл не найден на устройстве", null);
                    }
                    return;
                }
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mime = getMimeType(filePath);
            intent.setDataAndType(uri, mime != null ? mime : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("ChatAdapter", "Failed to open file", e);
            if (context instanceof Activity) {
                PrimeNotification.INSTANCE.show((Activity) context, "Не удалось открыть файл", null);
            }
        }
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type != null ? type : "*/*";
    }

    class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        TextView tvMessageText;
        TextView tvMessageTime;
        ImageView ivMessageImage;
        TextView tvMessageReaction;
        View layoutMessageReaction;
        ImageView ivReactionAvatar;
        View layoutIncomingBubble;
        View layoutMessageActions;
        TextView btnReactHeart;
        TextView btnReactFire;
        TextView btnReactLike;
        ImageButton btnReplyMsgAction;
        View layoutQuotedReply;
        TextView tvQuotedSender;
        TextView tvQuotedText;

        View layoutMediaContainer;
        ImageView ivVideoPlayBadge;
        TextView tvVideoDuration;
        VideoView videoMessagePreview;
        View layoutFileContainer;
        ImageView ivFileIcon;
        TextView tvFileName;
        TextView tvFileSize;
        ImageButton btnFileDownload;
        View layoutDownloadProgress;
        ProgressBar pbDownloadProgress;
        TextView tvDownloadPercent;
        ImageButton btnCancelDownload;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            layoutMessageReaction = itemView.findViewById(R.id.layoutMessageReaction);
            ivReactionAvatar = itemView.findViewById(R.id.ivReactionAvatar);
            tvMessageReaction = itemView.findViewById(R.id.tvMessageReaction);
            layoutIncomingBubble = itemView.findViewById(R.id.layoutIncomingBubble);
            layoutMessageActions = itemView.findViewById(R.id.layoutMessageActions);
            btnReactHeart = itemView.findViewById(R.id.btnReactHeart);
            btnReactFire = itemView.findViewById(R.id.btnReactFire);
            btnReactLike = itemView.findViewById(R.id.btnReactLike);
            btnReplyMsgAction = itemView.findViewById(R.id.btnReplyMsgAction);
            layoutQuotedReply = itemView.findViewById(R.id.layoutQuotedReply);
            tvQuotedSender = itemView.findViewById(R.id.tvQuotedSender);
            tvQuotedText = itemView.findViewById(R.id.tvQuotedText);

            layoutMediaContainer = itemView.findViewById(R.id.layoutMediaContainer);
            ivVideoPlayBadge = itemView.findViewById(R.id.ivVideoPlayBadge);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            videoMessagePreview = itemView.findViewById(R.id.videoMessagePreview);
            layoutFileContainer = itemView.findViewById(R.id.layoutFileContainer);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnFileDownload = itemView.findViewById(R.id.btnFileDownload);
            layoutDownloadProgress = itemView.findViewById(R.id.layoutDownloadProgress);
            pbDownloadProgress = itemView.findViewById(R.id.pbDownloadProgress);
            tvDownloadPercent = itemView.findViewById(R.id.tvDownloadPercent);
            btnCancelDownload = itemView.findViewById(R.id.btnCancelDownload);
        }

        void bind(ChatMessage message, boolean showDateHeader, OnMessageLongClickListener listener, int position) {
            if (showDateHeader && message.getTimestamp() > 0) {
                tvDateHeader.setVisibility(View.VISIBLE);
                tvDateHeader.setText(getDateHeaderString(message.getTimestamp()));
            } else {
                tvDateHeader.setVisibility(View.GONE);
            }

            if (layoutQuotedReply != null) {
                if (message.isReply()) {
                    layoutQuotedReply.setVisibility(View.VISIBLE);
                    if (tvQuotedSender != null) tvQuotedSender.setText(message.getReplyToSender());
                    if (tvQuotedText != null) {
                        String qText = message.getReplyToText();
                        tvQuotedText.setText(qText != null && !qText.isEmpty() ? qText : "Вложение");
                    }
                    layoutQuotedReply.setOnClickListener(v -> {
                        if (actionListener != null && message.getReplyToMessageId() != null) {
                            actionListener.onJumpToMessage(message.getReplyToMessageId(), message.getReplyToText());
                        }
                    });
                } else {
                    layoutQuotedReply.setVisibility(View.GONE);
                }
            }

            bindMessageAttachments(
                message, position,
                layoutMediaContainer, ivMessageImage, videoMessagePreview, ivVideoPlayBadge, tvVideoDuration,
                layoutFileContainer, ivFileIcon, tvFileName, tvFileSize,
                btnFileDownload, layoutDownloadProgress, pbDownloadProgress, tvDownloadPercent, btnCancelDownload
            );

            String text = message.getText();
            if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text) && !"Файл".equals(text)) {
                tvMessageText.setVisibility(View.VISIBLE);
                tvMessageText.setText(text);
                tvMessageText.setMovementMethod(LinkMovementMethod.getInstance());
                tvMessageText.setTextIsSelectable(true);
                setupCustomTextSelectionActionMode(itemView, tvMessageText, message, position);
                bindLinkPreview(itemView, text);
            } else {
                tvMessageText.setVisibility(View.GONE);
                View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
                if (layoutLinkPreview != null) layoutLinkPreview.setVisibility(View.GONE);
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            if (layoutMessageReaction instanceof LinearLayout) {
                bindReactions((LinearLayout) layoutMessageReaction, message, position);
            }

            if (layoutMessageActions != null) {
                layoutMessageActions.setVisibility(View.GONE);

                View.OnClickListener rxClick = v -> {
                    layoutMessageActions.setVisibility(View.GONE);
                    if (actionListener != null) {
                        if (v == btnReactHeart) actionListener.onQuickReaction(message, "❤️", position);
                        else if (v == btnReactFire) actionListener.onQuickReaction(message, "🔥", position);
                        else if (v == btnReactLike) actionListener.onQuickReaction(message, "👍", position);
                    }
                };

                if (btnReactHeart != null) btnReactHeart.setOnClickListener(rxClick);
                if (btnReactFire != null) btnReactFire.setOnClickListener(rxClick);
                if (btnReactLike != null) btnReactLike.setOnClickListener(rxClick);

                if (btnReplyMsgAction != null) {
                    btnReplyMsgAction.setOnClickListener(v -> {
                        layoutMessageActions.setVisibility(View.GONE);
                        if (actionListener != null) {
                            actionListener.onReplyMessage(message, position);
                        }
                    });
                }

                ImageButton btnForwardMsgAction = itemView.findViewById(R.id.btnForwardMsgAction);
                if (btnForwardMsgAction != null) {
                    btnForwardMsgAction.setOnClickListener(v -> {
                        layoutMessageActions.setVisibility(View.GONE);
                        if (actionListener != null) {
                            actionListener.onForwardMessage(message, position);
                        }
                    });
                }
            }

            itemView.setOnLongClickListener(v -> {
                if (!isConnectionActive) {
                    if (v.getContext() instanceof Activity) {
                        PrimeNotification.INSTANCE.show((Activity) v.getContext(), "Действие недоступно без подключения", null);
                    }
                    return true;
                }

                View shakeView = layoutIncomingBubble != null ? layoutIncomingBubble : itemView;
                ObjectAnimator animator = ObjectAnimator.ofFloat(shakeView, "translationX", 0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f);
                animator.setDuration(350);
                animator.start();

                if (layoutMessageActions != null) {
                    View layoutDefaultActions = itemView.findViewById(R.id.layoutDefaultActions);
                    View layoutSelectionActions = itemView.findViewById(R.id.layoutSelectionActions);
                    if (layoutDefaultActions != null) layoutDefaultActions.setVisibility(View.VISIBLE);
                    if (layoutSelectionActions != null) layoutSelectionActions.setVisibility(View.GONE);

                    if (layoutMessageActions.getVisibility() == View.VISIBLE) {
                        layoutMessageActions.setVisibility(View.GONE);
                    } else {
                        layoutMessageActions.setVisibility(View.VISIBLE);
                        layoutMessageActions.setAlpha(0f);
                        layoutMessageActions.setScaleX(0.7f);
                        layoutMessageActions.setScaleY(0.7f);
                        layoutMessageActions.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(220)
                                .setInterpolator(new OvershootInterpolator())
                                .start();
                    }
                }

                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                }
                return true;
            });
        }
    }

    class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        TextView tvMessageText;
        TextView tvMessageTime;
        ImageView ivMessageImage;
        ImageView ivMessageStatus;
        TextView tvMessageReaction;
        View layoutMessageReaction;
        ImageView ivReactionAvatar;
        View layoutOutgoingBubble;
        View layoutMessageActions;
        TextView btnReactHeart;
        TextView btnReactFire;
        TextView btnReactLike;
        ImageButton btnReplyMsgAction;
        ImageButton btnEditMsgAction;
        ImageButton btnDeleteMsgAction;
        View layoutQuotedReply;
        TextView tvQuotedSender;
        TextView tvQuotedText;

        View layoutMediaContainer;
        ImageView ivVideoPlayBadge;
        TextView tvVideoDuration;
        VideoView videoMessagePreview;
        View layoutFileContainer;
        ImageView ivFileIcon;
        TextView tvFileName;
        TextView tvFileSize;
        ImageButton btnFileDownload;
        View layoutDownloadProgress;
        ProgressBar pbDownloadProgress;
        TextView tvDownloadPercent;
        ImageButton btnCancelDownload;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
            tvMessageReaction = itemView.findViewById(R.id.tvMessageReaction);
            layoutMessageReaction = itemView.findViewById(R.id.layoutMessageReaction);
            ivReactionAvatar = itemView.findViewById(R.id.ivReactionAvatar);
            layoutOutgoingBubble = itemView.findViewById(R.id.layoutOutgoingBubble);
            layoutMessageActions = itemView.findViewById(R.id.layoutMessageActions);
            btnReactHeart = itemView.findViewById(R.id.btnReactHeart);
            btnReactFire = itemView.findViewById(R.id.btnReactFire);
            btnReactLike = itemView.findViewById(R.id.btnReactLike);
            btnReplyMsgAction = itemView.findViewById(R.id.btnReplyMsgAction);
            btnEditMsgAction = itemView.findViewById(R.id.btnEditMsgAction);
            btnDeleteMsgAction = itemView.findViewById(R.id.btnDeleteMsgAction);
            layoutQuotedReply = itemView.findViewById(R.id.layoutQuotedReply);
            tvQuotedSender = itemView.findViewById(R.id.tvQuotedSender);
            tvQuotedText = itemView.findViewById(R.id.tvQuotedText);

            layoutMediaContainer = itemView.findViewById(R.id.layoutMediaContainer);
            ivVideoPlayBadge = itemView.findViewById(R.id.ivVideoPlayBadge);
            tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            videoMessagePreview = itemView.findViewById(R.id.videoMessagePreview);
            layoutFileContainer = itemView.findViewById(R.id.layoutFileContainer);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnFileDownload = itemView.findViewById(R.id.btnFileDownload);
            layoutDownloadProgress = itemView.findViewById(R.id.layoutDownloadProgress);
            pbDownloadProgress = itemView.findViewById(R.id.pbDownloadProgress);
            tvDownloadPercent = itemView.findViewById(R.id.tvDownloadPercent);
            btnCancelDownload = itemView.findViewById(R.id.btnCancelDownload);
        }

        void bind(ChatMessage message, boolean showDateHeader, OnMessageLongClickListener listener, int position) {
            if (showDateHeader && message.getTimestamp() > 0) {
                tvDateHeader.setVisibility(View.VISIBLE);
                tvDateHeader.setText(getDateHeaderString(message.getTimestamp()));
            } else {
                tvDateHeader.setVisibility(View.GONE);
            }

            if (layoutQuotedReply != null) {
                if (message.isReply()) {
                    layoutQuotedReply.setVisibility(View.VISIBLE);
                    if (tvQuotedSender != null) tvQuotedSender.setText(message.getReplyToSender());
                    if (tvQuotedText != null) {
                        String qText = message.getReplyToText();
                        tvQuotedText.setText(qText != null && !qText.isEmpty() ? qText : "Вложение");
                    }
                    layoutQuotedReply.setOnClickListener(v -> {
                        if (actionListener != null && message.getReplyToMessageId() != null) {
                            actionListener.onJumpToMessage(message.getReplyToMessageId(), message.getReplyToText());
                        }
                    });
                } else {
                    layoutQuotedReply.setVisibility(View.GONE);
                }
            }

            bindMessageAttachments(
                message, position,
                layoutMediaContainer, ivMessageImage, videoMessagePreview, ivVideoPlayBadge, tvVideoDuration,
                layoutFileContainer, ivFileIcon, tvFileName, tvFileSize,
                btnFileDownload, layoutDownloadProgress, pbDownloadProgress, tvDownloadPercent, btnCancelDownload
            );

            String text = message.getText();
            if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text) && !"Файл".equals(text)) {
                tvMessageText.setVisibility(View.VISIBLE);
                tvMessageText.setText(text);
                tvMessageText.setMovementMethod(LinkMovementMethod.getInstance());
                tvMessageText.setTextIsSelectable(true);
                setupCustomTextSelectionActionMode(itemView, tvMessageText, message, position);
                bindLinkPreview(itemView, text);
            } else {
                tvMessageText.setVisibility(View.GONE);
                View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
                if (layoutLinkPreview != null) layoutLinkPreview.setVisibility(View.GONE);
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            if (layoutMessageReaction instanceof LinearLayout) {
                bindReactions((LinearLayout) layoutMessageReaction, message, position);
            }

            if (ivMessageStatus != null) {
                if (message.getMessageStatus() == MessageStatus.SENDING) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_clock);
                } else if (message.getMessageStatus() == MessageStatus.READ) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_done_all);
                } else if (message.getMessageStatus() == MessageStatus.SENT) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_done);
                } else if (message.getMessageStatus() == MessageStatus.ERROR) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_error);
                } else {
                    ivMessageStatus.setVisibility(View.GONE);
                }
            }

            View layoutMessageProgress = itemView.findViewById(R.id.layoutMessageProgress);
            ProgressBar pbMessageProgress = itemView.findViewById(R.id.pbMessageProgress);
            TextView tvMessageProgressPercent = itemView.findViewById(R.id.tvMessageProgressPercent);
            ImageButton btnCancelMessageSending = itemView.findViewById(R.id.btnCancelMessageSending);

            if (layoutMessageProgress != null) {
                if (message.getMessageStatus() == MessageStatus.SENDING || (message.getSendingProgress() > 0 && message.getSendingProgress() < 100)) {
                    layoutMessageProgress.setVisibility(View.VISIBLE);
                    if (pbMessageProgress != null) pbMessageProgress.setProgress(message.getSendingProgress());
                    if (tvMessageProgressPercent != null) tvMessageProgressPercent.setText(message.getSendingProgress() + "%");
                    if (btnCancelMessageSending != null) {
                        btnCancelMessageSending.setOnClickListener(v -> {
                            if (actionListener != null) {
                                actionListener.onCancelSending(message, position);
                            }
                        });
                    }
                } else {
                    layoutMessageProgress.setVisibility(View.GONE);
                }
            }

            if (layoutMessageActions != null) {
                layoutMessageActions.setVisibility(View.GONE);

                View.OnClickListener rxClick = v -> {
                    layoutMessageActions.setVisibility(View.GONE);
                    if (actionListener != null) {
                        if (v == btnReactHeart) actionListener.onQuickReaction(message, "❤️", position);
                        else if (v == btnReactFire) actionListener.onQuickReaction(message, "🔥", position);
                        else if (v == btnReactLike) actionListener.onQuickReaction(message, "👍", position);
                    }
                };

                if (btnReactHeart != null) btnReactHeart.setOnClickListener(rxClick);
                if (btnReactFire != null) btnReactFire.setOnClickListener(rxClick);
                if (btnReactLike != null) btnReactLike.setOnClickListener(rxClick);

                if (btnReplyMsgAction != null) {
                    btnReplyMsgAction.setOnClickListener(v -> {
                        layoutMessageActions.setVisibility(View.GONE);
                        if (actionListener != null) {
                            actionListener.onReplyMessage(message, position);
                        }
                    });
                }

                if (btnEditMsgAction != null) {
                    btnEditMsgAction.setOnClickListener(v -> {
                        layoutMessageActions.setVisibility(View.GONE);
                        if (actionListener != null) {
                            actionListener.onEditMessage(message, position);
                        }
                    });
                }

                if (btnDeleteMsgAction != null) {
                    btnDeleteMsgAction.setOnClickListener(v -> {
                        layoutMessageActions.setVisibility(View.GONE);
                        if (actionListener != null) {
                            actionListener.onDeleteMessage(message, position);
                        }
                    });
                }
            }

            itemView.setOnLongClickListener(v -> {
                if (!isConnectionActive) {
                    if (v.getContext() instanceof Activity) {
                        PrimeNotification.INSTANCE.show((Activity) v.getContext(), "Действие недоступно без подключения", null);
                    }
                    return true;
                }

                View shakeView = layoutOutgoingBubble != null ? layoutOutgoingBubble : itemView;
                ObjectAnimator animator = ObjectAnimator.ofFloat(shakeView, "translationX", 0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f);
                animator.setDuration(350);
                animator.start();

                if (layoutMessageActions != null) {
                    View layoutDefaultActions = itemView.findViewById(R.id.layoutDefaultActions);
                    View layoutSelectionActions = itemView.findViewById(R.id.layoutSelectionActions);
                    if (layoutDefaultActions != null) layoutDefaultActions.setVisibility(View.VISIBLE);
                    if (layoutSelectionActions != null) layoutSelectionActions.setVisibility(View.GONE);

                    if (layoutMessageActions.getVisibility() == View.VISIBLE) {
                        layoutMessageActions.setVisibility(View.GONE);
                    } else {
                        layoutMessageActions.setVisibility(View.VISIBLE);
                        layoutMessageActions.setAlpha(0f);
                        layoutMessageActions.setScaleX(0.7f);
                        layoutMessageActions.setScaleY(0.7f);
                        layoutMessageActions.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(220)
                                .setInterpolator(new OvershootInterpolator())
                                .start();
                    }
                }

                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                }
                return true;
            });
        }
    }

    private void setupCustomTextSelectionActionMode(View itemView, TextView textView, ChatMessage message, int position) {
        if (textView == null || itemView == null) return;

        View layoutMessageActions = itemView.findViewById(R.id.layoutMessageActions);
        View layoutDefaultActions = itemView.findViewById(R.id.layoutDefaultActions);
        View layoutSelectionActions = itemView.findViewById(R.id.layoutSelectionActions);
        View btnReplySelectionAction = itemView.findViewById(R.id.btnReplySelectionAction);
        View btnCopySelectionAction = itemView.findViewById(R.id.btnCopySelectionAction);

        textView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            private ActionMode activeMode = null;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                activeMode = mode;
                menu.clear();

                if (layoutMessageActions != null) {
                    if (layoutDefaultActions != null) layoutDefaultActions.setVisibility(View.GONE);
                    if (layoutSelectionActions != null) layoutSelectionActions.setVisibility(View.VISIBLE);

                    if (layoutMessageActions.getVisibility() != View.VISIBLE) {
                        layoutMessageActions.setVisibility(View.VISIBLE);
                        layoutMessageActions.setAlpha(0f);
                        layoutMessageActions.setScaleX(0.7f);
                        layoutMessageActions.setScaleY(0.7f);
                        layoutMessageActions.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(220)
                                .setInterpolator(new OvershootInterpolator())
                                .start();
                    }
                }

                if (btnReplySelectionAction != null) {
                    btnReplySelectionAction.setOnClickListener(v -> {
                        String selectedText = getSelectedText(textView);
                        if (!selectedText.isEmpty() && actionListener != null) {
                            actionListener.onReplyToSelectedText(message, selectedText, position);
                        }
                        if (activeMode != null) {
                            try { activeMode.finish(); } catch (Exception ignored) {}
                        }
                    });
                }

                if (btnCopySelectionAction != null) {
                    btnCopySelectionAction.setOnClickListener(v -> {
                        String selectedText = getSelectedText(textView);
                        if (!selectedText.isEmpty()) {
                            ClipboardManager clipboard = (ClipboardManager) textView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            if (clipboard != null) {
                                ClipData clip = ClipData.newPlainText("selected_text", selectedText);
                                clipboard.setPrimaryClip(clip);
                                if (textView.getContext() instanceof Activity) {
                                    PrimeNotification.INSTANCE.show((Activity) textView.getContext(), "Текст скопирован", null);
                                }
                            }
                        }
                        if (activeMode != null) {
                            try { activeMode.finish(); } catch (Exception ignored) {}
                        }
                    });
                }

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                activeMode = null;
                if (layoutMessageActions != null) {
                    layoutMessageActions.setVisibility(View.GONE);
                    if (layoutSelectionActions != null) layoutSelectionActions.setVisibility(View.GONE);
                    if (layoutDefaultActions != null) layoutDefaultActions.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private String getSelectedText(TextView textView) {
        if (textView == null) return "";
        int start = textView.getSelectionStart();
        int end = textView.getSelectionEnd();
        if (start < 0) start = 0;
        if (end < 0) end = 0;
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        CharSequence fullText = textView.getText();
        if (fullText != null && start < fullText.length() && end <= fullText.length() && start < end) {
            return fullText.subSequence(start, end).toString();
        }
        return "";
    }

    public void triggerHighlightAnimation(RecyclerView recyclerView, String messageId, String quotedText) {
        if (recyclerView == null || messageId == null) return;

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            int pos = holder.getAdapterPosition();
            if (pos >= 0 && pos < messages.size()) {
                ChatMessage msg = messages.get(pos);
                if (messageId.equals(msg.getMessageId())) {
                    TextView tvMessageText = child.findViewById(R.id.tvMessageText);
                    View bubble = child.findViewById(R.id.layoutIncomingBubble);
                    if (bubble == null) bubble = child.findViewById(R.id.layoutOutgoingBubble);

                    if (tvMessageText != null && tvMessageText.getVisibility() == View.VISIBLE && quotedText != null && !quotedText.isEmpty()) {
                        String fullText = tvMessageText.getText().toString();
                        int start = fullText.indexOf(quotedText);
                        if (start == -1) {
                            start = fullText.toLowerCase().indexOf(quotedText.toLowerCase());
                        }
                        if (start >= 0) {
                            int end = start + quotedText.length();
                            animateTextHighlight(tvMessageText, fullText, start, end);
                        } else {
                            animateBubbleHighlight(bubble != null ? bubble : child);
                        }
                    } else {
                        animateBubbleHighlight(bubble != null ? bubble : child);
                    }
                    break;
                }
            }
        }
    }

    private void animateTextHighlight(TextView textView, String fullText, int start, int end) {
        if (textView == null || fullText == null || start < 0 || end > fullText.length() || start >= end) return;

        SpannableString spannable = new SpannableString(fullText);
        int highlightColor = Color.parseColor("#803EB489");
        BackgroundColorSpan colorSpan = new BackgroundColorSpan(highlightColor);

        ValueAnimator animator = ValueAnimator.ofInt(0, 1, 0, 1, 0, 1, 0);
        animator.setDuration(2000);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            if (val == 1) {
                spannable.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannable.removeSpan(colorSpan);
            }
            textView.setText(spannable);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                textView.setText(fullText);
            }
        });
        animator.start();
    }

    private void animateBubbleHighlight(View bubbleView) {
        if (bubbleView == null) return;
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0.3f, 1f, 0.3f, 1f);
        animator.setDuration(2000);
        animator.addUpdateListener(anim -> {
            float alpha = (float) anim.getAnimatedValue();
            bubbleView.setAlpha(alpha);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bubbleView.setAlpha(1f);
            }
        });
        animator.start();
    }

    private void bindReactions(LinearLayout layoutMessageReaction, ChatMessage message, int position) {
        if (layoutMessageReaction == null) return;

        Map<String, String> reactions = message.getReactionsMap();
        if (reactions == null || reactions.isEmpty()) {
            layoutMessageReaction.setVisibility(View.GONE);
            layoutMessageReaction.removeAllViews();
            return;
        }

        layoutMessageReaction.setVisibility(View.VISIBLE);
        layoutMessageReaction.removeAllViews();

        Context context = layoutMessageReaction.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int chipPaddingH = (int) (6 * density);
        int chipPaddingV = (int) (3 * density);
        int marginEndPx = (int) (4 * density);
        int avatarSize = (int) (16 * density);

        for (Map.Entry<String, String> entry : reactions.entrySet()) {
            String author = entry.getKey();
            String emoji = entry.getValue();

            if (emoji == null || emoji.trim().isEmpty()) continue;

            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(chipPaddingH, chipPaddingV, chipPaddingH, chipPaddingV);
            chip.setBackgroundResource(R.drawable.bg_floating_island);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(marginEndPx);
            chip.setLayoutParams(lp);

            ShapeableImageView ivAvatar = new ShapeableImageView(context);
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
            avatarLp.setMarginEnd((int) (4 * density));
            ivAvatar.setLayoutParams(avatarLp);
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivAvatar.setShapeAppearanceModel(
                ivAvatar.getShapeAppearanceModel().toBuilder()
                    .setAllCornerSizes(avatarSize / 2f)
                    .build()
            );

            Bitmap rxBmp = getAvatarBitmapForUser(context, author);
            if (rxBmp != null) {
                ivAvatar.setImageBitmap(rxBmp);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            TextView tvEmoji = new TextView(context);
            tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvEmoji.setText(emoji);

            chip.addView(ivAvatar);
            chip.addView(tvEmoji);

            SharedPreferences sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sp.getString("current_user", "");
            boolean isLocalAuthor = author == null || author.isEmpty() ||
                    author.equalsIgnoreCase("my") || author.equalsIgnoreCase("me") ||
                    (localUsername != null && author.equalsIgnoreCase(localUsername)) ||
                    (!currentUser.isEmpty() && author.equalsIgnoreCase(currentUser));

            chip.setOnClickListener(v -> {
                if (isLocalAuthor) {
                    if (actionListener != null) {
                        actionListener.onQuickReaction(message, null, position);
                    }
                } else {
                    if (actionListener != null) {
                        String myCurrentRx = message.getReactionForUser(localUsername);
                        if (emoji.equalsIgnoreCase(myCurrentRx)) {
                            actionListener.onQuickReaction(message, null, position);
                        } else {
                            actionListener.onQuickReaction(message, emoji, position);
                        }
                    }
                }
            });

            layoutMessageReaction.addView(chip);
        }
    }

    private Bitmap getAvatarBitmapForUser(Context context, String author) {
        if (context == null) return null;

        SharedPreferences sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String currentUser = sp.getString("current_user", "");

        boolean isLocal = author == null || author.isEmpty() ||
                author.equalsIgnoreCase("my") || author.equalsIgnoreCase("me") ||
                (localUsername != null && author.equalsIgnoreCase(localUsername)) ||
                (!currentUser.isEmpty() && author.equalsIgnoreCase(currentUser));

        if (isLocal) {
            if (!currentUser.isEmpty()) {
                String localAvatarUri = sp.getString(currentUser + "_avatar", null);
                if (localAvatarUri != null && !localAvatarUri.isEmpty()) {
                    Bitmap bmp = loadBitmapFromUriString(context, localAvatarUri);
                    if (bmp != null) return bmp;
                }
                File fCurrent = new File(context.getFilesDir(), "avatar_" + currentUser + ".jpg");
                if (fCurrent.exists()) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeFile(fCurrent.getAbsolutePath());
                        if (bmp != null) return bmp;
                    } catch (Exception ignored) {}
                }
            }
            if (localUsername != null && !localUsername.isEmpty()) {
                File fLocal = new File(context.getFilesDir(), "avatar_" + localUsername + ".jpg");
                if (fLocal.exists()) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeFile(fLocal.getAbsolutePath());
                        if (bmp != null) return bmp;
                    } catch (Exception ignored) {}
                }
            }
        } else {
            if (author != null && !author.isEmpty()) {
                File fAuthor = new File(context.getFilesDir(), "avatar_" + author + ".jpg");
                if (fAuthor.exists()) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeFile(fAuthor.getAbsolutePath());
                        if (bmp != null) return bmp;
                    } catch (Exception ignored) {}
                }
            }
            File[] files = context.getFilesDir().listFiles((dir, name) -> name.startsWith("avatar_") && name.endsWith(".jpg"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (!currentUser.isEmpty() && name.contains(currentUser)) continue;
                    if (localUsername != null && !localUsername.isEmpty() && name.contains(localUsername)) continue;
                    try {
                        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                        if (bmp != null) return bmp;
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }

    private Bitmap loadBitmapFromUriString(Context context, String uriStr) {
        try {
            Uri uri = Uri.parse(uriStr);
            if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                File f = new File(uri.getPath());
                if (f.exists()) {
                    return BitmapFactory.decodeFile(f.getAbsolutePath());
                }
            } else {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    return bmp;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
