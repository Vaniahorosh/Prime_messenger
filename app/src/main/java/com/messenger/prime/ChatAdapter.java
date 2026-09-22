package com.messenger.prime;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
        void onJumpToMessage(String messageId);
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

    public static void showFullScreenMedia(Context context, View view, Bitmap bitmap, String path, String senderName, String timeStr, String caption) {
        if (context == null) return;
        if (context instanceof ChatPersonActivity) {
            ((ChatPersonActivity) context).setOpeningSubActivity(true);
        }

        Uri imageUri = null;
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                imageUri = Uri.fromFile(file);
            } else {
                imageUri = Uri.parse(path);
            }
        } else if (bitmap != null) {
            String savedPath = ChatHistoryManager.saveBitmapToFile(context, bitmap, System.currentTimeMillis());
            if (savedPath != null) {
                imageUri = Uri.fromFile(new File(savedPath));
            }
        }

        if (imageUri != null) {
            Intent intent = new Intent(context, MediaPlayerActivity.class);
            intent.putExtra("EXTRA_URI", imageUri.toString());
            intent.putExtra("EXTRA_SENDER_NAME", senderName != null ? senderName : "Отправитель");
            intent.putExtra("EXTRA_TIMESTAMP", timeStr != null ? timeStr : "сейчас");
            if (caption != null && !caption.isEmpty() && !"Фото".equals(caption) && !"Фотография".equals(caption)) {
                intent.putExtra("EXTRA_CAPTION", caption);
            }
            if (view != null) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
                intent.putExtra("EXTRA_RECT", rect);
            }
            context.startActivity(intent);
        }
    }

    public static void showFullScreenPhoto(Context context, View view, Bitmap bitmap, String path) {
        showFullScreenMedia(context, view, bitmap, path, "Отправитель", "сейчас", null);
    }

    public static void showFullScreenPhoto(Context context, Bitmap bitmap, String path) {
        showFullScreenMedia(context, null, bitmap, path, "Отправитель", "сейчас", null);
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

            Bitmap bmp = message.getImageBitmap();
            if (bmp == null && message.getImagePath() != null && !message.getImagePath().isEmpty()) {
                try {
                    if (message.isVideo()) {
                        bmp = getVideoThumbnail(message.getImagePath());
                        if (bmp != null) {
                            message.setImageBitmap(bmp);
                        }
                    } else {
                        bmp = BitmapFactory.decodeFile(message.getImagePath());
                        message.setImageBitmap(bmp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null || message.isVideo() || (message.getImagePath() != null && !message.getImagePath().isEmpty())) {
                if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.VISIBLE);

                if (message.isVideo()) {
                    if (ivMessageImage != null) ivMessageImage.setVisibility(View.GONE);
                    if (videoMessagePreview != null && message.getImagePath() != null && new File(message.getImagePath()).exists()) {
                        videoMessagePreview.setVisibility(View.VISIBLE);
                        String vPath = message.getImagePath();
                        if (vPath.startsWith("content://") || vPath.startsWith("file://")) {
                            videoMessagePreview.setVideoURI(Uri.parse(vPath));
                        } else {
                            videoMessagePreview.setVideoURI(Uri.fromFile(new File(vPath)));
                        }
                        videoMessagePreview.setOnPreparedListener(mp -> {
                            mp.setVolume(0f, 0f);
                            mp.setLooping(true);
                            videoMessagePreview.start();

                            int vWidth = mp.getVideoWidth();
                            int vHeight = mp.getVideoHeight();
                            if (vWidth > 0 && vHeight > 0) {
                                Context ctx = videoMessagePreview.getContext();
                                float density = ctx.getResources().getDisplayMetrics().density;
                                float aspect = (float) vWidth / vHeight;
                                int maxWidth = (int) (220 * density);
                                int maxHeight = (int) (200 * density);
                                int minHeight = (int) (120 * density);

                                int w = maxWidth;
                                int h = (int) (w / aspect);
                                if (h > maxHeight) {
                                    h = maxHeight;
                                    w = (int) (h * aspect);
                                } else if (h < minHeight) {
                                    h = minHeight;
                                    w = (int) (h * aspect);
                                }

                                ViewGroup.LayoutParams lp = videoMessagePreview.getLayoutParams();
                                lp.width = w;
                                lp.height = h;
                                videoMessagePreview.setLayoutParams(lp);

                                if (layoutMediaContainer != null) {
                                    ViewGroup.LayoutParams cp = layoutMediaContainer.getLayoutParams();
                                    cp.width = w;
                                    cp.height = h;
                                    layoutMediaContainer.setLayoutParams(cp);
                                }
                            }
                        });
                        videoMessagePreview.setOnErrorListener((mp, what, extra) -> true);
                        videoMessagePreview.setClickable(false);
                        videoMessagePreview.setFocusable(false);
                    } else if (ivMessageImage != null) {
                        ivMessageImage.setVisibility(View.VISIBLE);
                        if (bmp != null) ivMessageImage.setImageBitmap(bmp);
                        else ivMessageImage.setImageResource(R.drawable.ic_video);
                    }

                    if (ivVideoPlayBadge != null) ivVideoPlayBadge.setVisibility(View.GONE);
                    if (tvVideoDuration != null) {
                        tvVideoDuration.setVisibility(View.VISIBLE);
                        tvVideoDuration.setText(message.getVideoDuration() != null && !message.getVideoDuration().isEmpty() ? message.getVideoDuration() : "00:00");
                    }
                } else {
                    if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
                    if (ivMessageImage != null) {
                        ivMessageImage.setVisibility(View.VISIBLE);
                        if (bmp != null) {
                            ivMessageImage.setImageBitmap(bmp);
                        } else {
                            ivMessageImage.setImageResource(R.drawable.ic_photo);
                        }
                    }
                    if (ivVideoPlayBadge != null) ivVideoPlayBadge.setVisibility(View.GONE);
                    if (tvVideoDuration != null) tvVideoDuration.setVisibility(View.GONE);
                }

                final Bitmap finalBmp = bmp != null ? bmp : message.getImageBitmap();
                View.OnClickListener openMedia = v -> showFullScreenMedia(
                        v.getContext(),
                        v,
                        finalBmp,
                        message.getImagePath(),
                        message.getSenderLogin(),
                        formatTime(message.getTimestamp(), message.getTime(), false),
                        message.getText()
                );

                if (layoutMediaContainer != null) layoutMediaContainer.setOnClickListener(openMedia);
                if (ivMessageImage != null) ivMessageImage.setOnClickListener(openMedia);
                if (videoMessagePreview != null) videoMessagePreview.setOnClickListener(openMedia);
            } else {
                if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
                if (ivMessageImage != null) ivMessageImage.setVisibility(View.GONE);
                if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
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
        if (context == null || filePath == null) return;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                if (context instanceof Activity) {
                    PrimeNotification.INSTANCE.show((Activity) context, "Файл не найден на устройстве", null);
                }
                return;
            }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(filePath));
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
                            actionListener.onJumpToMessage(message.getReplyToMessageId());
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
                            actionListener.onJumpToMessage(message.getReplyToMessageId());
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
