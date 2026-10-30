package com.messenger.prime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;
import androidx.recyclerview.widget.DiffUtil;
import com.bumptech.glide.Glide;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import android.content.res.Configuration;
import eightbitlab.com.blurview.BlurView;

import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.util.Size;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.graphics.drawable.ColorDrawable;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
        void onSelectMessage(ChatMessage message, int position);
        void onSelectionChanged(int selectedCount);
        void onExpandReactions(ChatMessage message, View anchorView, int position);
        void onJumpToMessage(String messageId);
        void onJumpToMessage(String messageId, String quotedText);
        void onCancelSending(ChatMessage message, int position);
        void onRetryMessage(ChatMessage message, int position);
    }

    private List<ChatMessage> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;
    private OnMessageActionListener actionListener;
    private String localUsername = "";
    private boolean isConnectionActive = false;

    private final Set<String> selectedMessageIds = new java.util.LinkedHashSet<>();
    private boolean isMultiSelectionMode = false;

    public boolean isMultiSelectionMode() {
        return isMultiSelectionMode;
    }

    public void setMultiSelectionMode(boolean enabled) {
        if (this.isMultiSelectionMode != enabled) {
            this.isMultiSelectionMode = enabled;
            if (!enabled) {
                selectedMessageIds.clear();
            }
            notifyDataSetChanged();
        }
    }

    public Set<String> getSelectedMessageIds() {
        return new java.util.LinkedHashSet<>(selectedMessageIds);
    }

    public List<ChatMessage> getSelectedMessages() {
        List<ChatMessage> list = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (m != null && m.getMessageId() != null && selectedMessageIds.contains(m.getMessageId())) {
                list.add(m);
            }
        }
        return list;
    }

    public void toggleMessageSelection(ChatMessage message) {
        if (message == null || message.getMessageId() == null) return;
        String id = message.getMessageId();
        int pos = findPositionByMessageId(id);
        if (selectedMessageIds.contains(id)) {
            selectedMessageIds.remove(id);
        } else {
            selectedMessageIds.add(id);
        }
        if (selectedMessageIds.isEmpty()) {
            setMultiSelectionMode(false);
            if (actionListener != null) {
                actionListener.onSelectionChanged(0);
            }
        } else {
            if (pos != -1) {
                notifyItemChanged(pos, "SELECTION_CHANGE");
            } else {
                notifyDataSetChanged();
            }
            if (actionListener != null) {
                actionListener.onSelectionChanged(selectedMessageIds.size());
            }
        }
    }

    private final Set<String> dragInitialSelectedIds = new java.util.HashSet<>();

    public void startDragSelection(int startPos) {
        dragInitialSelectedIds.clear();
        dragInitialSelectedIds.addAll(selectedMessageIds);
    }

    public boolean updateDragSelectionRange(int startPos, int currentPos, boolean isSelecting) {
        if (messages == null || messages.isEmpty()) return false;
        int min = Math.max(0, Math.min(startPos, currentPos));
        int max = Math.min(messages.size() - 1, Math.max(startPos, currentPos));

        boolean changedAny = false;
        if (!isMultiSelectionMode) {
            isMultiSelectionMode = true;
        }

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg == null || msg.getMessageId() == null) continue;
            String id = msg.getMessageId();

            boolean wasInitiallySelected = dragInitialSelectedIds.contains(id);
            boolean shouldBeSelected;

            if (i >= min && i <= max) {
                shouldBeSelected = isSelecting;
            } else {
                shouldBeSelected = wasInitiallySelected;
            }

            boolean isCurrentlySelected = selectedMessageIds.contains(id);
            if (shouldBeSelected != isCurrentlySelected) {
                if (shouldBeSelected) {
                    selectedMessageIds.add(id);
                } else {
                    selectedMessageIds.remove(id);
                }
                notifyItemChanged(i, "SELECTION_CHANGE");
                changedAny = true;
            }
        }

        if (changedAny && actionListener != null) {
            actionListener.onSelectionChanged(selectedMessageIds.size());
        }
        return changedAny;
    }

    public void clearSelection() {
        selectedMessageIds.clear();
        isMultiSelectionMode = false;
        notifyDataSetChanged();
        if (actionListener != null) {
            actionListener.onSelectionChanged(0);
        }
    }
    private boolean isScrolling = false;

    public void setScrolling(boolean scrolling) {
        this.isScrolling = scrolling;
    }

    public boolean isScrolling() {
        return isScrolling;
    }

    public static Activity getActivityFromContext(Context context) {
        if (context == null) return null;
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

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

    private String newlyAnimatedMessageId = null;

    public void addMessage(ChatMessage message) {
        if (message == null) return;
        String msgId = message.getMessageId();
        if (msgId != null && !msgId.isEmpty()) {
            int pos = findPositionByMessageId(msgId);
            if (pos != -1) {
                messages.set(pos, message);
                notifyItemChanged(pos);
                return;
            }
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessageAnimated(ChatMessage message) {
        if (message == null) return;
        String msgId = message.getMessageId();
        if (msgId != null && !msgId.isEmpty()) {
            int pos = findPositionByMessageId(msgId);
            if (pos != -1) {
                messages.set(pos, message);
                notifyItemChanged(pos);
                return;
            }
        }
        messages.add(message);
        newlyAnimatedMessageId = msgId;
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
                } else {
                    msg.setMessageStatus(MessageStatus.SENDING);
                }
                notifyItemChanged(i, "PROGRESS");
                break;
            }
        }
    }

    private String lastVoicePlayingMessageId = null;

    public void updateVoicePlaybackState(String msgId, PrimeMediaPlaybackService.PlaybackState state) {
        if (!Objects.equals(lastVoicePlayingMessageId, msgId)) {
            if (lastVoicePlayingMessageId != null) {
                int oldPos = findPositionByMessageId(lastVoicePlayingMessageId);
                if (oldPos != -1) notifyItemChanged(oldPos, "VOICE_PROGRESS");
            }
            lastVoicePlayingMessageId = msgId;
        }
        if (msgId != null && !msgId.isEmpty()) {
            int pos = findPositionByMessageId(msgId);
            if (pos != -1) {
                notifyItemChanged(pos, "VOICE_PROGRESS");
            }
        }
    }

    public void setMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> incomingMessages = newMessages != null ? newMessages : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return messages.size();
            }

            @Override
            public int getNewListSize() {
                return incomingMessages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(messages.get(oldItemPosition).getMessageId(), incomingMessages.get(newItemPosition).getMessageId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldItem = messages.get(oldItemPosition);
                ChatMessage newItem = incomingMessages.get(newItemPosition);
                return Objects.equals(oldItem.getText(), newItem.getText()) &&
                       oldItem.getMessageStatus() == newItem.getMessageStatus() &&
                       Objects.equals(oldItem.getReaction(), newItem.getReaction()) &&
                       oldItem.isEdited() == newItem.isEdited() &&
                       Objects.equals(oldItem.getImagePath(), newItem.getImagePath());
            }
        });
        
        this.messages.clear();
        this.messages.addAll(incomingMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    public ChatMessage getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    public void deleteMessageAnimated(View itemView, int position, Runnable onComplete) {
        if (position < 0 || position >= messages.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (currentlyPlayingVideoNote != null) {
            ChatMessage msgAtPos = messages.get(position);
            if (msgAtPos != null && msgAtPos.getMessageId() != null &&
                msgAtPos.getMessageId().equals(currentlyPlayingVideoNote.getMessageId())) {
                stopCurrentlyPlayingVideoNote();
            }
        }

        if (itemView != null) {
            itemView.animate().cancel();
            itemView.animate()
                    .alpha(0f)
                    .translationX(-itemView.getWidth() * 0.4f)
                    .setDuration(200)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            itemView.setAlpha(1f);
                            itemView.setTranslationX(0f);
                            if (position >= 0 && position < messages.size()) {
                                messages.remove(position);
                                notifyItemRemoved(position);
                            }
                            if (onComplete != null) onComplete.run();
                        }
                    }).start();
        } else {
            messages.remove(position);
            notifyItemRemoved(position);
            if (onComplete != null) onComplete.run();
        }
    }

    public void deleteMessageByIdAnimated(RecyclerView recyclerView, String messageId, Runnable onComplete) {
        if (messageId == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        deleteMessagesByIds(recyclerView, java.util.Collections.singletonList(messageId), onComplete);
    }

    public void deleteMessagesByIds(RecyclerView recyclerView, List<String> messageIds, Runnable onComplete) {
        if (messageIds == null || messageIds.isEmpty() || messages == null || messages.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (currentlyPlayingVideoNote != null && currentlyPlayingVideoNote.getMessageId() != null) {
            if (messageIds.contains(currentlyPlayingVideoNote.getMessageId())) {
                stopCurrentlyPlayingVideoNote();
            }
        }

        Set<String> idSet = new java.util.HashSet<>(messageIds);
        Set<String> tsParts = new java.util.HashSet<>();
        for (String id : messageIds) {
            if (id != null && id.contains("_")) {
                String[] parts = id.split("_");
                if (parts.length > 1) {
                    tsParts.add(parts[1]);
                }
            }
        }

        List<Integer> positionsToDelete = new ArrayList<>();
        List<ChatMessage> itemsToDelete = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m == null) continue;
            String mId = m.getMessageId();
            if (mId != null) {
                boolean match = idSet.contains(mId);
                if (!match && mId.contains("_")) {
                    String[] parts = mId.split("_");
                    if (parts.length > 1 && tsParts.contains(parts[1])) {
                        match = true;
                    }
                }
                if (match) {
                    positionsToDelete.add(i);
                    itemsToDelete.add(m);
                }
            }
        }

        if (itemsToDelete.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        List<View> viewsToAnimate = new ArrayList<>();
        if (recyclerView != null) {
            for (int pos : positionsToDelete) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(pos);
                if (holder != null && holder.itemView != null) {
                    viewsToAnimate.add(holder.itemView);
                }
            }
        }

        Runnable finishAction = () -> {
            messages.removeAll(itemsToDelete);
            for (ChatMessage m : itemsToDelete) {
                if (m.getMessageId() != null) {
                    selectedMessageIds.remove(m.getMessageId());
                }
            }
            if (selectedMessageIds.isEmpty()) {
                isMultiSelectionMode = false;
                if (actionListener != null) {
                    actionListener.onSelectionChanged(0);
                }
            } else if (actionListener != null) {
                actionListener.onSelectionChanged(selectedMessageIds.size());
            }
            notifyDataSetChanged();
            if (onComplete != null) onComplete.run();
        };

        if (!viewsToAnimate.isEmpty()) {
            int[] pendingAnimations = {viewsToAnimate.size()};
            for (View view : viewsToAnimate) {
                view.animate().cancel();
                view.animate()
                        .alpha(0f)
                        .translationX(-view.getWidth() * 0.4f)
                        .setDuration(200)
                        .setInterpolator(new AccelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                view.setAlpha(1f);
                                view.setTranslationX(0f);
                                pendingAnimations[0]--;
                                if (pendingAnimations[0] <= 0) {
                                    finishAction.run();
                                }
                            }
                        }).start();
            }
        } else {
            finishAction.run();
        }
    }

    public void updateMessageById(String messageId, String newText) {
        updateMessageTextById(messageId, newText);
    }

    public void updateMessageStatusById(String messageId, MessageStatus status) {
        if (messages == null || messages.isEmpty()) return;
        if (status == MessageStatus.READ) {
            markOutgoingMessagesAsReadUpTo(messageId);
            return;
        }
        if (messageId != null && !messageId.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);
                if (messageId.equals(m.getMessageId())) {
                    m.setMessageStatus(status);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    public void markOutgoingMessagesAsReadUpTo(String readMsgId) {
        if (messages == null || messages.isEmpty()) return;

        String cleanId = readMsgId;
        boolean markAll = (readMsgId == null || readMsgId.isEmpty() || "READ_ALL".equalsIgnoreCase(readMsgId));
        if (readMsgId != null && readMsgId.startsWith("READ_ALL:::")) {
            cleanId = readMsgId.substring(11);
        }

        int targetIndex = messages.size() - 1;
        if (!markAll && cleanId != null && !cleanId.isEmpty()) {
            int foundIdx = findPositionByMessageId(cleanId);
            if (foundIdx != -1) {
                targetIndex = foundIdx;
            }
        }

        for (int i = 0; i <= targetIndex && i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m != null && m.isOutgoing() && m.getMessageStatus() != MessageStatus.READ) {
                m.setMessageStatus(MessageStatus.READ);
                notifyItemChanged(i, "STATUS_UPDATE");
            }
        }
    }

    public void markIncomingMessagesAsReadUpTo(String readMsgId) {
        if (messages == null || messages.isEmpty()) return;

        String cleanId = readMsgId;
        boolean markAll = (readMsgId == null || readMsgId.isEmpty() || "READ_ALL".equalsIgnoreCase(readMsgId));
        if (readMsgId != null && readMsgId.startsWith("READ_ALL:::")) {
            cleanId = readMsgId.substring(11);
        }

        int targetIndex = messages.size() - 1;
        if (!markAll && cleanId != null && !cleanId.isEmpty()) {
            int foundIdx = findPositionByMessageId(cleanId);
            if (foundIdx != -1) {
                targetIndex = foundIdx;
            }
        }

        for (int i = 0; i <= targetIndex && i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m != null && !m.isOutgoing() && m.getMessageStatus() != MessageStatus.READ) {
                m.setMessageStatus(MessageStatus.READ);
                notifyItemChanged(i, "STATUS_UPDATE");
            }
        }
    }

    public boolean updateMessageTextById(String messageId, String newText) {
        if (messageId == null || messageId.isEmpty() || messages == null || messages.isEmpty()) return false;
        String cleanId = messageId.trim();

        // 1. Exact ID match
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (cleanId.equals(m.getMessageId()) || (m.getMessageId() != null && m.getMessageId().trim().equals(cleanId))) {
                m.setEdited(true);
                m.setText(newText);
                notifyItemChanged(i);
                return true;
            }
        }

        // 2. Partial ID match
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m.getMessageId() != null && (cleanId.contains(m.getMessageId()) || m.getMessageId().contains(cleanId))) {
                m.setEdited(true);
                m.setText(newText);
                notifyItemChanged(i);
                return true;
            }
        }

        // 3. Fallback: last incoming (non-outgoing) message
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (!m.isOutgoing()) {
                m.setEdited(true);
                m.setText(newText);
                notifyItemChanged(i);
                return true;
            }
        }

        return false;
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

        View v = holder.itemView;
        v.animate().cancel();

        if (holder.getItemViewType() == VIEW_TYPE_OUTGOING) {
            ((OutgoingViewHolder) holder).bind(message, showDateHeader, longClickListener, position);
        } else {
            ((IncomingViewHolder) holder).bind(message, showDateHeader, longClickListener, position);
        }

        if (message.getMessageId() != null && message.getMessageId().equals(newlyAnimatedMessageId)) {
            newlyAnimatedMessageId = null;
            
            View bubble = null;
            if (holder instanceof OutgoingViewHolder) {
                bubble = holder.itemView.findViewById(R.id.layoutOutgoingBubble);
            } else if (holder instanceof IncomingViewHolder) {
                bubble = holder.itemView.findViewById(R.id.layoutIncomingBubble);
            }

            if (bubble != null) {
                float density = v.getResources().getDisplayMetrics().density;
                bubble.setTranslationY(24f * density);
                bubble.setAlpha(0f);
                bubble.animate()
                        .translationY(0f)
                        .alpha(1.0f)
                        .setDuration(260)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                v.setTranslationY(24f * v.getResources().getDisplayMetrics().density);
                v.setAlpha(0f);
                v.animate()
                        .translationY(0f)
                        .alpha(1.0f)
                        .setDuration(260)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        } else {
            v.setAlpha(1f);
            v.setTranslationY(0f);
            v.setTranslationX(0f);
            
            View bubble = null;
            if (holder instanceof OutgoingViewHolder) {
                bubble = holder.itemView.findViewById(R.id.layoutOutgoingBubble);
            } else if (holder instanceof IncomingViewHolder) {
                bubble = holder.itemView.findViewById(R.id.layoutIncomingBubble);
            }
            if (bubble != null) {
                bubble.animate().cancel();
                bubble.setAlpha(1f);
                bubble.setTranslationY(0f);
                bubble.setScaleX(1f);
                bubble.setScaleY(1f);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            if (payloads.contains("SELECTION_CHANGE")) {
                if (position >= 0 && position < messages.size()) {
                    ChatMessage message = messages.get(position);
                    boolean isSelected = isSelectedInMultiMode(message);

                    holder.itemView.setAlpha(isSelected ? 0.85f : 1.0f);

                    View layoutSelectionCircle = holder.itemView.findViewById(R.id.layoutSelectionCircle);
                    ImageView ivSelectionCheck = holder.itemView.findViewById(R.id.ivSelectionCheck);

                    if (layoutSelectionCircle != null) {
                        if (isMultiSelectionMode) {
                            layoutSelectionCircle.setVisibility(View.VISIBLE);
                            if (ivSelectionCheck != null) {
                                if (isSelected) {
                                    ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_active);
                                    ivSelectionCheck.setImageResource(R.drawable.ic_done);
                                    ivSelectionCheck.setAlpha(1.0f);
                                    ivSelectionCheck.setScaleX(0.3f);
                                    ivSelectionCheck.setScaleY(0.3f);
                                    ivSelectionCheck.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                                } else {
                                    ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_idle);
                                    ivSelectionCheck.setImageDrawable(null);
                                    ivSelectionCheck.setAlpha(0.6f);
                                    ivSelectionCheck.setScaleX(1.0f);
                                    ivSelectionCheck.setScaleY(1.0f);
                                }
                            }
                        } else {
                            layoutSelectionCircle.setVisibility(View.GONE);
                        }
                    }
                }
                return;
            }

            if (payloads.contains("VOICE_PROGRESS")) {
                View layoutVoiceContainer = holder.itemView.findViewById(R.id.layoutVoiceContainer);
                if (layoutVoiceContainer != null && position >= 0 && position < messages.size()) {
                    ChatMessage msg = messages.get(position);
                    ImageButton btnPlay = layoutVoiceContainer.findViewById(R.id.btnVoicePlayPause);
                    VoiceMessageItemView waveformView = layoutVoiceContainer.findViewById(R.id.voiceWaveform);
                    TextView tvDuration = layoutVoiceContainer.findViewById(R.id.tvVoiceDuration);

                    boolean isCurrentPlaying = Objects.equals(msg.getMessageId(), PrimeMediaPlaybackService.Companion.getCurrentVoiceMessageId());
                    PrimeMediaPlaybackService.PlaybackState state = PrimeMediaPlaybackService.Companion.getPlaybackState().getValue();

                    if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Playing) {
                        PrimeMediaPlaybackService.PlaybackState.Playing playing = (PrimeMediaPlaybackService.PlaybackState.Playing) state;
                        if (btnPlay != null) btnPlay.setImageResource(R.drawable.ic_media_pause);
                        if (waveformView != null) waveformView.setProgress(playing.getProgress());
                        if (tvDuration != null) {
                            long curSec = (long)(playing.getDurationMs() * playing.getProgress() / 1000);
                            long totalSec = playing.getDurationMs() / 1000;
                            tvDuration.setText(String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60));
                        }
                    } else if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Paused) {
                        PrimeMediaPlaybackService.PlaybackState.Paused paused = (PrimeMediaPlaybackService.PlaybackState.Paused) state;
                        if (btnPlay != null) btnPlay.setImageResource(R.drawable.ic_media_play);
                        if (waveformView != null) waveformView.setProgress(paused.getProgress());
                        if (tvDuration != null) {
                            long curSec = (long)(paused.getDurationMs() * paused.getProgress() / 1000);
                            long totalSec = paused.getDurationMs() / 1000;
                            tvDuration.setText(String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60));
                        }
                    } else {
                        if (btnPlay != null) btnPlay.setImageResource(R.drawable.ic_media_play);
                        if (waveformView != null) waveformView.setProgress(0f);
                        if (tvDuration != null) tvDuration.setText(msg.getAudioDuration() != null ? msg.getAudioDuration() : "00:00");
                    }
                }
                return;
            } else if (payloads.contains("HIGHLIGHT")) {
                View view = holder.itemView;
                view.animate().cancel();
                ObjectAnimator flicker = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.15f, 1f, 0.15f, 1f, 0.35f, 1f);
                flicker.setDuration(1200);
                flicker.setInterpolator(new AccelerateDecelerateInterpolator());
                flicker.start();
            } else if (payloads.contains("PROGRESS") || payloads.contains("STATUS_UPDATE")) {
                if (position >= 0 && position < messages.size()) {
                    ChatMessage message = messages.get(position);
                    View layoutMessageProgress = holder.itemView.findViewById(R.id.layoutMessageProgress);
                    com.messenger.prime.PrimeWaterLoadingView pbMessageProgress = holder.itemView.findViewById(R.id.pbMessageProgress);
                    TextView tvMessageProgressPercent = holder.itemView.findViewById(R.id.tvMessageProgressPercent);

                    if (layoutMessageProgress != null) {
                        if (message.getMessageStatus() == MessageStatus.SENDING && message.getSendingProgress() > 0 && message.getSendingProgress() < 100) {
                            layoutMessageProgress.setVisibility(View.VISIBLE);
                            if (pbMessageProgress != null) pbMessageProgress.setProgress(message.getSendingProgress());
                            if (tvMessageProgressPercent != null) tvMessageProgressPercent.setText(message.getSendingProgress() + "%");
                        } else {
                            layoutMessageProgress.setVisibility(View.GONE);
                        }
                    }

                    ImageView ivMessageStatus = holder.itemView.findViewById(R.id.ivMessageStatus);
                    if (ivMessageStatus != null) {
                        if (message.getMessageStatus() == MessageStatus.SENDING) {
                            ivMessageStatus.setVisibility(View.VISIBLE);
                            ivMessageStatus.setImageResource(R.drawable.ic_clock);
                        } else if (message.getMessageStatus() == MessageStatus.READ) {
                            ivMessageStatus.setVisibility(View.VISIBLE);
                            ivMessageStatus.setImageResource(R.drawable.ic_done_all);
                            ivMessageStatus.setColorFilter(Color.parseColor("#00E676")); // Зеленый для прочитанных
                        } else if (message.getMessageStatus() == MessageStatus.SENT) {
                            ivMessageStatus.setVisibility(View.VISIBLE);
                            ivMessageStatus.setImageResource(R.drawable.ic_done);
                            ivMessageStatus.clearColorFilter();
                        } else if (message.getMessageStatus() == MessageStatus.ERROR) {
                            ivMessageStatus.setVisibility(View.VISIBLE);
                            ivMessageStatus.setImageResource(R.drawable.ic_error);
                            ivMessageStatus.clearColorFilter();
                        } else {
                            ivMessageStatus.setVisibility(View.GONE);
                        }
                    }
                }
                return;
            }
        }
        super.onBindViewHolder(holder, position, payloads);
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

    public ChatMessage findMessageById(String messageId) {
        int pos = findPositionByMessageId(messageId);
        return pos != -1 ? messages.get(pos) : null;
    }

    public int findPositionByQuery(String query, int startFromIndex) {
        if (query == null || query.trim().isEmpty() || messages == null || messages.isEmpty()) return -1;
        String lowerQuery = query.trim().toLowerCase(Locale.getDefault());
        int size = messages.size();
        for (int i = Math.max(0, startFromIndex); i < size; i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getText() != null && msg.getText().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                return i;
            }
            if (msg.getFileName() != null && msg.getFileName().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
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
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        holder.itemView.setTranslationX(0f);

        View bubble = null;
        if (holder instanceof OutgoingViewHolder) {
            bubble = holder.itemView.findViewById(R.id.layoutOutgoingBubble);
        } else if (holder instanceof IncomingViewHolder) {
            bubble = holder.itemView.findViewById(R.id.layoutIncomingBubble);
        }

        if (bubble != null) {
            bubble.animate().cancel();
            bubble.setAlpha(1f);
            bubble.setTranslationY(0f);
            bubble.setTranslationX(0f);
            bubble.setScaleX(1f);
            bubble.setScaleY(1f);
        }

        stopVideoPreviewInViewHolder(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        stopVideoPreviewInViewHolder(holder);
    }

    private void stopVideoPreviewInViewHolder(RecyclerView.ViewHolder holder) {
        if (holder == null) return;

        com.messenger.prime.TextureVideoView vv = null;
        if (holder instanceof IncomingViewHolder) {
            vv = ((IncomingViewHolder) holder).videoMessagePreview;
        } else if (holder instanceof OutgoingViewHolder) {
            vv = ((OutgoingViewHolder) holder).videoMessagePreview;
        }
        if (vv != null) {
            vv.removeCallbacks(null);
            try {
                vv.stopPlayback();
                vv.setVisibility(View.GONE);
            } catch (Exception ignored) {}
        }

        com.messenger.prime.TextureVideoView vvNote = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
        if (vvNote != null) {
            try {
                if (vvNote.isPlaying()) {
                    vvNote.pause();
                    vvNote.stopPlayback();
                }
                vvNote.setVisibility(View.GONE);
            } catch (Exception ignored) {}
        }

        try {
            ImageView iv = holder.itemView.findViewById(R.id.ivMessageImage);
            if (iv != null) {
                Glide.with(holder.itemView.getContext()).clear(iv);
            }
        } catch (Exception ignored) {}
    }

    public void cleanup() {
        if (mRecyclerView != null) {
            for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                View child = mRecyclerView.getChildAt(i);
                RecyclerView.ViewHolder vh = mRecyclerView.getChildViewHolder(child);
                stopVideoPreviewInViewHolder(vh);
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
        if (PrimeTransitions.areAnimationsDisabled(viewToAnimate.getContext())) {
            viewToAnimate.setAlpha(1f);
            viewToAnimate.setTranslationY(0f);
            return;
        }
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
        Activity act = getActivityFromContext(context);
        if (act instanceof ChatPersonActivity) {
            ((ChatPersonActivity) act).setOpeningSubActivity(true);
        }

        List<ChatMessage> mediaMessages = new ArrayList<>();
        int startIndex = 0;
        
        if (allMessages != null) {
            for (ChatMessage msg : allMessages) {
                if (msg == null) continue;
                if (msg.isVideoNote() || (msg.getText() != null && msg.getText().startsWith("VIDEO_NOTE:"))) {
                    continue; // BARRIER: Video notes are strictly excluded from MediaPlayerActivity!
                }

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
            if (clickedMessage.isVideoNote() || (clickedMessage.getText() != null && clickedMessage.getText().startsWith("VIDEO_NOTE:"))) {
                return; // BARRIER: Video notes are strictly excluded from MediaPlayerActivity!
            }
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
        if (context instanceof android.app.Activity) {
            PrimeTransitions.startActivityWithTransition((android.app.Activity) context, intent);
        } else {
            context.startActivity(intent);
        }
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

    private void bindLinkPreview(View itemView, String messageText, ChatMessage message) {
        View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
        ImageView ivLinkPreviewImage = itemView.findViewById(R.id.ivLinkPreviewImage);
        TextView tvLinkPreviewTitle = itemView.findViewById(R.id.tvLinkPreviewTitle);
        TextView tvLinkPreviewDescription = itemView.findViewById(R.id.tvLinkPreviewDescription);
        TextView tvLinkPreviewUrl = itemView.findViewById(R.id.tvLinkPreviewUrl);

        if (layoutLinkPreview != null) {
            String url = extractUrl(messageText);
            if (url != null && !url.trim().isEmpty()) {
                layoutLinkPreview.setVisibility(View.VISIBLE);

                String domain = url;
                try {
                    Uri uri = Uri.parse(url.startsWith("http") ? url : "https://" + url);
                    if (uri.getHost() != null) domain = uri.getHost();
                } catch (Exception ignored) {}

                if (tvLinkPreviewTitle != null) tvLinkPreviewTitle.setText(domain);
                if (tvLinkPreviewUrl != null) tvLinkPreviewUrl.setText(url);
                if (tvLinkPreviewDescription != null) tvLinkPreviewDescription.setVisibility(View.GONE);

                LinkPreviewManager.getInstance().fetchPreview(url, data -> {
                    if (data != null && itemView.isAttachedToWindow()) {
                        if (tvLinkPreviewTitle != null && data.title != null && !data.title.isEmpty()) {
                            tvLinkPreviewTitle.setText(data.title);
                        }
                        if (tvLinkPreviewDescription != null && data.description != null && !data.description.isEmpty()) {
                            tvLinkPreviewDescription.setText(data.description);
                            tvLinkPreviewDescription.setVisibility(View.VISIBLE);
                        }
                        if (tvLinkPreviewUrl != null && data.domain != null && !data.domain.isEmpty()) {
                            tvLinkPreviewUrl.setText(data.domain);
                        }
                        if (ivLinkPreviewImage != null && data.imageUrl != null && !data.imageUrl.isEmpty()) {
                            Glide.with(itemView.getContext())
                                    .load(data.imageUrl)
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_search)
                                    .error(R.drawable.ic_search)
                                    .into(ivLinkPreviewImage);
                        }
                    }
                });

                layoutLinkPreview.setOnClickListener(v -> {
                    if (isMultiSelectionMode) {
                        toggleMessageSelection(message);
                        return;
                    }
                    try {
                        com.messenger.prime.browser.AppBrowserManager.openUrl(v.getContext(), url);
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
            com.messenger.prime.TextureVideoView videoMessagePreview,
            ImageView ivVideoPlayBadge,
            TextView tvVideoDuration,
            View layoutFileContainer,
            ImageView ivFileIcon,
            TextView tvFileName,
            TextView tvFileSize,
            ImageButton btnFileDownload,
            View layoutDownloadProgress,
            com.messenger.prime.PrimeWaterLoadingView pbDownloadProgress,
            TextView tvDownloadPercent,
            ImageButton btnCancelDownload
    ) {
        View layoutMusicContainer = layoutFileContainer != null && layoutFileContainer.getParent() != null ?
                ((View) layoutFileContainer.getParent()).findViewById(R.id.layoutMusicContainer) : null;
        View layoutVoiceContainer = layoutFileContainer != null && layoutFileContainer.getParent() != null ?
                ((View) layoutFileContainer.getParent()).findViewById(R.id.layoutVoiceContainer) : null;

        if (message.getMessageType() == ChatMessage.MessageType.VOICE) {
            if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
            if (layoutFileContainer != null) layoutFileContainer.setVisibility(View.GONE);
            if (layoutMusicContainer != null) layoutMusicContainer.setVisibility(View.GONE);
            if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
            if (layoutVoiceContainer != null) {
                layoutVoiceContainer.setVisibility(View.VISIBLE);
                
                ImageButton btnPlay = layoutVoiceContainer.findViewById(R.id.btnVoicePlayPause);
                VoiceMessageItemView waveformView = layoutVoiceContainer.findViewById(R.id.voiceWaveform);
                TextView tvDuration = layoutVoiceContainer.findViewById(R.id.tvVoiceDuration);

                if (message.getWaveformChunks() != null) {
                    waveformView.setWaveform(message.getWaveformChunks());
                }

                boolean isCurrentPlaying = Objects.equals(message.getMessageId(), PrimeMediaPlaybackService.Companion.getCurrentVoiceMessageId());
                PrimeMediaPlaybackService.PlaybackState state = PrimeMediaPlaybackService.Companion.getPlaybackState().getValue();
                
                if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Playing) {
                    PrimeMediaPlaybackService.PlaybackState.Playing playing = (PrimeMediaPlaybackService.PlaybackState.Playing) state;
                    btnPlay.setImageResource(R.drawable.ic_media_pause);
                    waveformView.setProgress(playing.getProgress());
                    long curSec = (long)(playing.getDurationMs() * playing.getProgress() / 1000);
                    long totalSec = playing.getDurationMs() / 1000;
                    tvDuration.setText(String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60));
                } else if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Paused) {
                    PrimeMediaPlaybackService.PlaybackState.Paused paused = (PrimeMediaPlaybackService.PlaybackState.Paused) state;
                    btnPlay.setImageResource(R.drawable.ic_media_play);
                    waveformView.setProgress(paused.getProgress());
                    long curSec = (long)(paused.getDurationMs() * paused.getProgress() / 1000);
                    long totalSec = paused.getDurationMs() / 1000;
                    tvDuration.setText(String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60));
                } else {
                    btnPlay.setImageResource(R.drawable.ic_media_play);
                    waveformView.setProgress(0f);
                    tvDuration.setText(message.getAudioDuration() != null ? message.getAudioDuration() : "00:00");
                }

                btnPlay.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), PrimeMediaPlaybackService.class);
                    if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Playing) {
                        intent.setAction("PAUSE_VOICE");
                        v.getContext().startService(intent);
                    } else if (isCurrentPlaying && state instanceof PrimeMediaPlaybackService.PlaybackState.Paused) {
                        intent.setAction("RESUME_VOICE");
                        v.getContext().startService(intent);
                    } else {
                        intent.setAction("PLAY_VOICE");
                        intent.putExtra("EXTRA_PATH", message.getImagePath());
                        intent.putExtra("EXTRA_AUTHOR", message.getSenderLogin());
                        intent.putExtra("EXTRA_MSG_ID", message.getMessageId());
                        v.getContext().startService(intent);
                    }
                });

                waveformView.setOnSeekListener(prog -> {
                    Context ctx = layoutVoiceContainer.getContext();
                    Intent intent = new Intent(ctx, PrimeMediaPlaybackService.class);
                    if (isCurrentPlaying) {
                        intent.setAction("SEEK_VOICE");
                        intent.putExtra("EXTRA_PROGRESS", prog);
                        ctx.startService(intent);
                    } else {
                        intent.setAction("PLAY_VOICE");
                        intent.putExtra("EXTRA_PATH", message.getImagePath());
                        intent.putExtra("EXTRA_AUTHOR", message.getSenderLogin());
                        intent.putExtra("EXTRA_MSG_ID", message.getMessageId());
                        intent.putExtra("EXTRA_SEEK_PROGRESS", prog);
                        ctx.startService(intent);
                    }
                    return kotlin.Unit.INSTANCE;
                });
            }
            return;
        } else {
            if (layoutVoiceContainer != null) layoutVoiceContainer.setVisibility(View.GONE);
        }

        if (message.isVideoNote() || (message.getText() != null && message.getText().startsWith("VIDEO_NOTE:"))) {
            if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
            if (layoutFileContainer != null) layoutFileContainer.setVisibility(View.GONE);
            if (layoutMusicContainer != null) layoutMusicContainer.setVisibility(View.GONE);
            if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
            return;
        }

        if (message.isMusic()) {
            if (layoutMediaContainer != null) layoutMediaContainer.setVisibility(View.GONE);
            if (videoMessagePreview != null) videoMessagePreview.setVisibility(View.GONE);
            if (layoutFileContainer != null) layoutFileContainer.setVisibility(View.GONE);

            if (layoutMusicContainer != null) {
                layoutMusicContainer.setVisibility(View.VISIBLE);

                ImageView ivMusicCover = layoutMusicContainer.findViewById(R.id.ivMusicCover);
                ImageView btnMusicPlayPause = layoutMusicContainer.findViewById(R.id.btnMusicPlayPause);
                TextView tvMusicTitle = layoutMusicContainer.findViewById(R.id.tvMusicTitle);
                TextView tvMusicArtist = layoutMusicContainer.findViewById(R.id.tvMusicArtist);
                TextView tvMusicDuration = layoutMusicContainer.findViewById(R.id.tvMusicDuration);
                ImageView btnMusicDownload = layoutMusicContainer.findViewById(R.id.btnMusicDownload);
                ProgressBar pbMusicProgress = layoutMusicContainer.findViewById(R.id.pbMusicProgress);

                String path = message.getImagePath() != null ? message.getImagePath() : "";
                TrackItem trackItem = PrimeMusicManager.INSTANCE.extractMetadata(layoutMusicContainer.getContext(), path);

                String title = message.getTrackTitle() != null && !message.getTrackTitle().isEmpty() ? message.getTrackTitle() : trackItem.getTitle();
                String artist = message.getArtistName() != null && !message.getArtistName().isEmpty() ? message.getArtistName() : trackItem.getArtist();
                String duration = message.getAudioDuration() != null && !message.getAudioDuration().isEmpty() ? message.getAudioDuration() : trackItem.getDurationStr();

                if (tvMusicTitle != null) {
                    tvMusicTitle.setText(title);
                    if (!tvMusicTitle.isSelected()) {
                        tvMusicTitle.setSelected(true);
                    }
                }
                if (tvMusicArtist != null) tvMusicArtist.setText(artist);
                if (tvMusicDuration != null) tvMusicDuration.setText(duration);

                if (ivMusicCover != null) {
                    if (trackItem.getCoverPath() != null) {
                        Glide.with(layoutMusicContainer.getContext())
                                .load(trackItem.getCoverPath())
                                .placeholder(R.drawable.ic_music)
                                .error(R.drawable.ic_music)
                                .into(ivMusicCover);
                    } else {
                        ivMusicCover.setImageResource(R.drawable.ic_music);
                    }
                }

                boolean isPlayingThis = PrimeMusicManager.INSTANCE.isPlaying() &&
                        PrimeMusicManager.INSTANCE.getCurrentTrack() != null &&
                        path.equals(PrimeMusicManager.INSTANCE.getCurrentTrack().getPath());

                if (pbMusicProgress != null) {
                    if (isPlayingThis && trackItem.getDurationMs() > 0) {
                        pbMusicProgress.setVisibility(View.VISIBLE);
                        long currentPos = PrimeMusicManager.INSTANCE.getCurrentPosition();
                        int progress = (int) ((currentPos * 100) / trackItem.getDurationMs());
                        pbMusicProgress.setProgress(Math.max(0, Math.min(100, progress)));
                    } else {
                        pbMusicProgress.setProgress(0);
                    }
                }

                if (btnMusicPlayPause != null) {
                    btnMusicPlayPause.setImageResource(isPlayingThis ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
                    btnMusicPlayPause.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        if (isPlayingThis) {
                            PrimeMusicManager.INSTANCE.togglePlayPause(v.getContext());
                        } else {
                            PrimeMusicManager.INSTANCE.playTrack(v.getContext(), trackItem, java.util.Collections.singletonList(trackItem));
                        }
                        notifyItemChanged(position);
                    });
                }

                if (btnMusicDownload != null) {
                    boolean isSaved = PrimeMusicManager.INSTANCE.isTrackInCollection(layoutMusicContainer.getContext(), path);
                    btnMusicDownload.setImageResource(isSaved ? R.drawable.ic_done : R.drawable.ic_download);
                    btnMusicDownload.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        TrackItem saved = PrimeMusicManager.INSTANCE.saveTrackToCollection(v.getContext(), path, title, artist);
                        if (saved != null) {
                            Toast.makeText(v.getContext(), "Сохранено в коллекцию", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(v.getContext(), "Трек уже в коллекции", Toast.LENGTH_SHORT).show();
                        }
                        btnMusicDownload.setImageResource(R.drawable.ic_done);
                    });
                }

                layoutMusicContainer.setOnClickListener(v -> {
                    if (isMultiSelectionMode) {
                        toggleMessageSelection(message);
                        return;
                    }
                    PrimeMusicPlayerDialog playerDialog = new PrimeMusicPlayerDialog(v.getContext());
                    if (isPlayingThis) {
                        playerDialog.show(null, java.util.Collections.singletonList(trackItem));
                    } else {
                        playerDialog.show(trackItem, java.util.Collections.singletonList(trackItem));
                    }
                });
            }
            return;
        } else {
            if (layoutMusicContainer != null) layoutMusicContainer.setVisibility(View.GONE);
        }

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
                    boolean isDownloaded = message.isOutgoing() || (message.getImagePath() != null && !message.getImagePath().isEmpty() && (message.getImagePath().startsWith("content://") || new File(message.getImagePath()).exists()));
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
                    btnFileDownload.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        startDownloadSimulation(message, position);
                    });
                }
                if (btnCancelDownload != null) {
                    btnCancelDownload.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        cancelDownloadSimulation(message, position);
                    });
                }

                layoutFileContainer.setOnClickListener(v -> {
                    if (isMultiSelectionMode) {
                        toggleMessageSelection(message);
                        return;
                    }
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
            boolean hasValidPath = message.getImagePath() != null &&
                    !message.getImagePath().trim().isEmpty() &&
                    !"null".equalsIgnoreCase(message.getImagePath().trim()) &&
                    !message.getImagePath().startsWith("MULTI:");
            boolean hasBitmap = message.getImageBitmap() != null;
            boolean isImageOrVideoType = message.getMessageType() == ChatMessage.MessageType.IMAGE ||
                    message.getMessageType() == ChatMessage.MessageType.VIDEO ||
                    message.isVideo();

            if ((mediaItems == null || mediaItems.isEmpty()) && (hasValidPath || hasBitmap || isImageOrVideoType)) {
                mediaItems = new ArrayList<>();
                ChatMessage.MediaItem singleItem = new ChatMessage.MediaItem(
                    message.getImagePath(),
                    message.isVideo() || message.getMessageType() == ChatMessage.MessageType.VIDEO,
                    message.getVideoDuration()
                );
                mediaItems.add(singleItem);
                message.setMediaItems(mediaItems);
            }

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
                } else { // 2, 3, 4 ... up to 20 items: swipable slider with counter!
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

    private void setupSingleTapMediaOpener(View targetView, ChatMessage message, List<ChatMessage> messagesList) {
        if (targetView == null) return;
        targetView.setOnClickListener(v -> {
            if (isMultiSelectionMode) {
                toggleMessageSelection(message);
                return;
            }
            showFullScreenMedia(v.getContext(), v, message, messagesList);
        });
    }

    private void setupSingleTapMediaOpenerIndexed(View targetView, ChatMessage message, List<ChatMessage> messagesList, int itemIndex) {
        if (targetView == null) return;
        targetView.setOnClickListener(v -> {
            if (isMultiSelectionMode) {
                toggleMessageSelection(message);
                return;
            }
            showFullScreenMedia(v.getContext(), v, message, messagesList, itemIndex);
        });
    }

    public interface OnVideoNotePlaybackListener {
        void onVideoNoteStarted(ChatMessage message, android.media.MediaPlayer mp, String shape, int position);
        void onVideoNoteStopped(ChatMessage message);
        void onPlayNextVideoNote(int nextPosition);
    }

    private OnVideoNotePlaybackListener videoNotePlaybackListener;

    public void setOnVideoNotePlaybackListener(OnVideoNotePlaybackListener listener) {
        this.videoNotePlaybackListener = listener;
    }

    private ChatMessage currentlyPlayingVideoNote = null;
    private int currentlyPlayingPosition = -1;

    public ChatMessage getCurrentlyPlayingVideoNote() {
        return currentlyPlayingVideoNote;
    }

    public int getCurrentlyPlayingPosition() {
        return currentlyPlayingPosition;
    }

    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.mRecyclerView = null;
    }

    private void requestAudioFocusForVideoNote(Context context) {
        android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    private void abandonAudioFocusForVideoNote(Context context) {
        android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    public void stopCurrentlyPlayingVideoNote() {
        stopVideoProgressAnimation();
        if (currentlyPlayingPosition >= 0 && mRecyclerView != null) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(currentlyPlayingPosition);
            if (holder != null && holder.itemView != null) {
                abandonAudioFocusForVideoNote(holder.itemView.getContext());
                com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                ImageView ivPlayIcon = holder.itemView.findViewById(R.id.ivVideoNotePlayIcon);
                ImageView ivImg = holder.itemView.findViewById(R.id.ivVideoNoteImage);
                if (vv != null) {
                    vv.pause();
                    vv.stopPlayback();
                    vv.setVisibility(View.GONE);
                }
                if (ivImg != null) ivImg.setVisibility(View.VISIBLE);
                if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
                
                com.messenger.prime.PrimeWaterLoadingView pb = holder.itemView.findViewById(R.id.pbVideoNoteFrameProgress);
                if (pb != null) pb.setProgress(0);
            }
        }
        currentlyPlayingVideoNote = null;
        currentlyPlayingPosition = -1;
        notifyDataSetChanged();
    }

    public void syncVideoNoteTimestamp(RecyclerView recyclerView, int position, int msec) {
        if (recyclerView != null && position >= 0) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null && holder.itemView != null) {
                com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                ImageView ivImg = holder.itemView.findViewById(R.id.ivVideoNoteImage);
                ImageView ivPlayIcon = holder.itemView.findViewById(R.id.ivVideoNotePlayIcon);
                if (vv != null) {
                    if (ivImg != null) ivImg.setVisibility(View.GONE);
                    vv.setVisibility(View.VISIBLE);
                    if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.GONE);
                    vv.seekTo(msec);
                    vv.start();
                }
            }
        }
    }

    public int findNextVideoNotePosition(int startPos) {
        if (messages == null) return -1;
        for (int i = startPos + 1; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg != null && msg.getText() != null && msg.getText().startsWith("VIDEO_NOTE:")) {
                return i;
            }
        }
        return -1;
    }

    private void applyBubbleCornerRadius(View layoutBubble, boolean isOutgoing) {
        if (layoutBubble == null) return;
        SharedPreferences sharedPrefs = layoutBubble.getContext().getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        int percent = sharedPrefs.getInt("settings_bubble_radius_percent", 50);

        float density = layoutBubble.getContext().getResources().getDisplayMetrics().density;
        float maxRadiusPx = 22f * density;
        float radiusPx = maxRadiusPx * (percent / 100f);

        int bgColor;
        if (isOutgoing) {
            bgColor = ColorAccentManager.getCurrentAccentColor(layoutBubble.getContext());
        } else {
            boolean isDark = (layoutBubble.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            bgColor = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#E2E8F0");
        }

        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(radiusPx);
        drawable.setColor(bgColor);

        layoutBubble.setBackground(drawable);
    }

    private void resetBubbleForNormalMessage(View itemView, boolean isOutgoing) {
        View layoutBubble = itemView.findViewById(isOutgoing ? R.id.layoutOutgoingBubble : R.id.layoutIncomingBubble);
        if (layoutBubble != null) {
            ViewGroup.LayoutParams lp = layoutBubble.getLayoutParams();
            if (lp != null && lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutBubble.setLayoutParams(lp);
            }
            applyBubbleCornerRadius(layoutBubble, isOutgoing);
            int paddingPx = (int) (12 * itemView.getResources().getDisplayMetrics().density);
            layoutBubble.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        }

        View layoutBlock = itemView.findViewById(isOutgoing ? R.id.layoutOutgoingMessageBlock : R.id.layoutIncomingMessageBlock);
        if (layoutBlock != null) {
            ViewGroup.LayoutParams lpBlock = layoutBlock.getLayoutParams();
            if (lpBlock != null && lpBlock.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lpBlock.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutBlock.setLayoutParams(lpBlock);
            }
        }

        View vnContainer = itemView.findViewById(R.id.layoutVideoNoteContainer);
        if (vnContainer != null) {
            vnContainer.setVisibility(View.GONE);
        }

        View badgeIn = itemView.findViewById(R.id.ivRichBadgeIn);
        if (badgeIn != null) badgeIn.setVisibility(View.GONE);

        View badgeOut = itemView.findViewById(R.id.ivRichBadgeOut);
        if (badgeOut != null) badgeOut.setVisibility(View.GONE);

        View layoutMediaContainer = itemView.findViewById(R.id.layoutMediaContainer);
        if (layoutMediaContainer != null) {
            ViewGroup.LayoutParams lp = layoutMediaContainer.getLayoutParams();
            if (lp != null) {
                lp.width = (int) (240 * itemView.getResources().getDisplayMetrics().density);
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutMediaContainer.setLayoutParams(lp);
            }
            layoutMediaContainer.setOutlineProvider(null);
            layoutMediaContainer.setClipToOutline(false);
        }

        View layoutSingleMedia = itemView.findViewById(R.id.layoutSingleMedia);
        if (layoutSingleMedia != null) {
            ViewGroup.LayoutParams lp = layoutSingleMedia.getLayoutParams();
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutSingleMedia.setLayoutParams(lp);
            }
            layoutSingleMedia.setOutlineProvider(null);
            layoutSingleMedia.setClipToOutline(false);
        }

        ImageView img = itemView.findViewById(R.id.ivMessageImage);
        if (img != null) {
            img.setOutlineProvider(null);
            img.setClipToOutline(false);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        com.messenger.prime.TextureVideoView vv = itemView.findViewById(R.id.videoMessagePreview);
        if (vv != null) {
            vv.setOutlineProvider(null);
            vv.setClipToOutline(false);
            vv.setScaleX(1.0f);
            vv.setScaleY(1.0f);
        }
    }

    private void applyCenterCropToVideoView(VideoView vv, android.media.MediaPlayer mp) {
        if (vv == null || mp == null) return;
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        int viewWidth = vv.getWidth();
        int viewHeight = vv.getHeight();

        if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            float videoAspect = (float) videoWidth / (float) videoHeight;
            float viewAspect = (float) viewWidth / (float) viewHeight;

            if (videoAspect > viewAspect) {
                vv.setScaleX(videoAspect / viewAspect);
                vv.setScaleY(1.0f);
            } else {
                vv.setScaleX(1.0f);
                vv.setScaleY(viewAspect / videoAspect);
            }
        }
    }

    private final android.os.Handler videoProgressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable activeVideoProgressRunnable = null;

    private void startVideoProgressAnimation(com.messenger.prime.TextureVideoView vv, com.messenger.prime.PrimeWaterLoadingView pbFrame) {
        stopVideoProgressAnimation();

        activeVideoProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (vv != null && vv.isPlaying() && pbFrame != null) {
                    int cur = vv.getCurrentPosition();
                    int dur = vv.getDuration();
                    if (dur > 0) {
                        pbFrame.setProgress((cur * 100) / dur);
                    }
                    videoProgressHandler.postDelayed(this, 50);
                }
            }
        };
        videoProgressHandler.post(activeVideoProgressRunnable);
    }

    private void stopVideoProgressAnimation() {
        if (activeVideoProgressRunnable != null) {
            videoProgressHandler.removeCallbacks(activeVideoProgressRunnable);
            activeVideoProgressRunnable = null;
        }
    }

    private void bindVideoNoteMessage(View itemView, ChatMessage message, boolean isOutgoing, int position) {
        FrameLayout vnContainer = itemView.findViewById(R.id.layoutVideoNoteContainer);
        if (vnContainer == null) return;

        ImageView ivImg = itemView.findViewById(R.id.ivVideoNoteImage);
        com.messenger.prime.TextureVideoView vvPlayer = itemView.findViewById(R.id.vvVideoNotePlayer);
        com.messenger.prime.PrimeWaterLoadingView pbFrame = itemView.findViewById(R.id.pbVideoNoteFrameProgress);
        ImageView ivPlayIcon = itemView.findViewById(R.id.ivVideoNotePlayIcon);
        TextView tvTime = itemView.findViewById(R.id.tvVideoNoteTimeBadge);

        View layoutBubble = itemView.findViewById(isOutgoing ? R.id.layoutOutgoingBubble : R.id.layoutIncomingBubble);
        if (layoutBubble != null) {
            layoutBubble.setBackground(null);
            layoutBubble.setPadding(0, 0, 0, 0);
        }

        View layoutMediaContainer = itemView.findViewById(R.id.layoutMediaContainer);
        if (layoutMediaContainer != null) {
            layoutMediaContainer.setVisibility(View.GONE);
        }

        vnContainer.setVisibility(View.VISIBLE);

        String text = message.getText();
        String shape = "circle";
        if (text != null && text.startsWith("VIDEO_NOTE:")) {
            shape = text.substring("VIDEO_NOTE:".length()).trim();
        } else if (message.getFileName() != null && message.getFileName().startsWith("vn_")) {
            String[] parts = message.getFileName().replace(".mp4", "").split("_");
            if (parts.length >= 3) {
                shape = parts[2].trim();
            }
        }

        if (pbFrame != null) {
            pbFrame.setBorderProgressMode(true);
            pbFrame.setStaticShapeType(shape);
            int accentColor = ColorAccentManager.getCurrentAccentColor(itemView.getContext());
            pbFrame.setIndicatorColor(accentColor);
        }

        applyShapeToMediaViews(vnContainer, ivImg, vvPlayer, shape);

        if (tvTime != null) {
            tvTime.setText(formatTime(message.getTimestamp(), message.getTime(), false));
        }

        boolean isThisMessagePlaying = currentlyPlayingVideoNote != null &&
            message != null &&
            message.getMessageId() != null &&
            message.getMessageId().equals(currentlyPlayingVideoNote.getMessageId());

        if (!isThisMessagePlaying) {
            if (vvPlayer != null) {
                vvPlayer.stopPlayback();
                vvPlayer.setVisibility(View.GONE);
            }
            if (ivImg != null) ivImg.setVisibility(View.VISIBLE);
            if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
            if (pbFrame != null) pbFrame.setProgress(0);
        }

        String videoPath = message.getImagePath();
        if (videoPath != null && new File(videoPath).exists()) {
            if (ivImg != null) {
                ivImg.setVisibility(View.VISIBLE);
                loadMediaImageIntoView(itemView.getContext(), videoPath, ivImg);
            }

            final String finalShape = shape;
            View.OnClickListener toggleListener = v -> {
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                    return;
                }

                if (vvPlayer != null) {
                    if (vvPlayer.isPlaying()) {
                        vvPlayer.pause();
                        vvPlayer.stopPlayback();
                        vvPlayer.setVisibility(View.GONE);
                        if (ivImg != null) ivImg.setVisibility(View.VISIBLE);
                        stopVideoProgressAnimation();
                        if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
                        if (pbFrame != null) pbFrame.setProgress(0);
                        if (videoNotePlaybackListener != null) {
                            videoNotePlaybackListener.onVideoNoteStopped(message);
                        }
                        currentlyPlayingVideoNote = null;
                        currentlyPlayingPosition = -1;
                    } else {
                        if (ivImg != null) ivImg.setVisibility(View.GONE);
                        vvPlayer.setVisibility(View.VISIBLE);
                        if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.GONE);

                        vvPlayer.setVideoPath(videoPath);
                        vvPlayer.setOnPreparedListener(mp -> {
                            mp.setLooping(false);
                            requestAudioFocusForVideoNote(itemView.getContext());
                            mp.start();
                            startVideoProgressAnimation(vvPlayer, pbFrame);
                            currentlyPlayingVideoNote = message;
                            currentlyPlayingPosition = position;
                            if (videoNotePlaybackListener != null) {
                                videoNotePlaybackListener.onVideoNoteStarted(message, mp, finalShape, position);
                            }
                        });
                        vvPlayer.setOnCompletionListener(mp -> {
                            abandonAudioFocusForVideoNote(itemView.getContext());
                            stopVideoProgressAnimation();
                            if (vvPlayer != null) {
                                vvPlayer.setVisibility(View.GONE);
                            }
                            if (ivImg != null) ivImg.setVisibility(View.VISIBLE);
                            if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
                            if (pbFrame != null) pbFrame.setProgress(0);
                            
                            int currentPos = position;
                            currentlyPlayingVideoNote = null;
                            currentlyPlayingPosition = -1;

                            int nextPos = findNextVideoNotePosition(currentPos);
                            if (nextPos != -1 && videoNotePlaybackListener != null) {
                                videoNotePlaybackListener.onPlayNextVideoNote(nextPos);
                            } else if (videoNotePlaybackListener != null) {
                                videoNotePlaybackListener.onVideoNoteStopped(message);
                            }
                        });
                    }
                }
            };

            vnContainer.setOnClickListener(toggleListener);
            if (vvPlayer != null) vvPlayer.setOnClickListener(toggleListener);
            if (pbFrame != null) pbFrame.setOnClickListener(toggleListener);
            if (ivPlayIcon != null) ivPlayIcon.setOnClickListener(toggleListener);
            if (ivImg != null) ivImg.setOnClickListener(toggleListener);
        }

        View.OnLongClickListener longClickListener = v -> {
            if (isMultiSelectionMode) {
                toggleMessageSelection(message);
            } else {
                showMessageContextMenu(v, message, position, 0f, 0f);
            }
            return true;
        };

        vnContainer.setOnLongClickListener(longClickListener);
        if (vvPlayer != null) vvPlayer.setOnLongClickListener(longClickListener);
        if (pbFrame != null) pbFrame.setOnLongClickListener(longClickListener);
        if (ivPlayIcon != null) ivPlayIcon.setOnLongClickListener(longClickListener);
        if (ivImg != null) ivImg.setOnLongClickListener(longClickListener);
        itemView.setOnLongClickListener(longClickListener);

        View layoutSelectionCircle = itemView.findViewById(R.id.layoutSelectionCircle);
        ImageView ivSelectionCheck = itemView.findViewById(R.id.ivSelectionCheck);
        boolean isSelected = isSelectedInMultiMode(message);

        if (layoutSelectionCircle != null) {
            if (isMultiSelectionMode) {
                layoutSelectionCircle.setVisibility(View.VISIBLE);
                if (ivSelectionCheck != null) {
                    if (isSelected) {
                        ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_active);
                        ivSelectionCheck.setImageResource(R.drawable.ic_done);
                        ivSelectionCheck.setAlpha(1.0f);
                    } else {
                        ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_idle);
                        ivSelectionCheck.setImageDrawable(null);
                        ivSelectionCheck.setAlpha(0.6f);
                    }
                }
            } else {
                layoutSelectionCircle.setVisibility(View.GONE);
            }
        }

        ImageView ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
        if (ivMessageStatus != null) {
            if (message.getMessageStatus() == MessageStatus.SENDING) {
                ivMessageStatus.setVisibility(View.VISIBLE);
                ivMessageStatus.setImageResource(R.drawable.ic_clock);
                ivMessageStatus.setOnClickListener(null);
            } else if (message.getMessageStatus() == MessageStatus.READ) {
                ivMessageStatus.setVisibility(View.VISIBLE);
                ivMessageStatus.setImageResource(R.drawable.ic_done_all);
                ivMessageStatus.setOnClickListener(null);
            } else if (message.getMessageStatus() == MessageStatus.SENT) {
                ivMessageStatus.setVisibility(View.VISIBLE);
                ivMessageStatus.setImageResource(R.drawable.ic_done);
                ivMessageStatus.setOnClickListener(null);
            } else if (message.getMessageStatus() == MessageStatus.ERROR) {
                ivMessageStatus.setVisibility(View.VISIBLE);
                ivMessageStatus.setImageResource(R.drawable.ic_error);
                ivMessageStatus.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onRetryMessage(message, position);
                    }
                });
            } else {
                ivMessageStatus.setVisibility(View.GONE);
                ivMessageStatus.setOnClickListener(null);
            }
        }

        View layoutMessageProgress = itemView.findViewById(R.id.layoutMessageProgress);
        com.messenger.prime.PrimeWaterLoadingView pbMessageProgress = itemView.findViewById(R.id.pbMessageProgress);
        TextView tvMessageProgressPercent = itemView.findViewById(R.id.tvMessageProgressPercent);
        ImageButton btnCancelMessageSending = itemView.findViewById(R.id.btnCancelMessageSending);

        if (layoutMessageProgress != null) {
            if (message.getMessageStatus() == MessageStatus.SENDING && message.getSendingProgress() > 0 && message.getSendingProgress() < 100) {
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
    }

    private void applyShapeToMediaViews(View container, ImageView img, View vv, String shape) {
        if (container == null) return;
        SharedPreferences sharedPrefs = container.getContext().getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        int sizeDp = sharedPrefs.getInt("video_message_size_dp", 180);

        int radiusPercent = sharedPrefs.getInt("video_message_corner_radius", 50);
        if (shape != null && !shape.isEmpty()) {
            try {
                radiusPercent = Integer.parseInt(shape.trim());
            } catch (NumberFormatException e) {
                if ("square".equalsIgnoreCase(shape)) {
                    radiusPercent = 0;
                } else if ("star".equalsIgnoreCase(shape)) {
                    radiusPercent = 35;
                } else if ("prime".equalsIgnoreCase(shape)) {
                    radiusPercent = 25;
                }
            }
        }

        float density = container.getContext().getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        ViewGroup.LayoutParams lp = container.getLayoutParams();
        if (lp != null) {
            lp.width = sizePx;
            lp.height = sizePx;
            container.setLayoutParams(lp);
        }

        final float cornerRadiusPx = sizePx * (radiusPercent / 100f);

        android.view.ViewOutlineProvider provider = new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
            }
        };

        container.setOutlineProvider(provider);
        container.setClipToOutline(true);
        container.invalidateOutline();

        if (img != null) {
            img.setOutlineProvider(provider);
            img.setClipToOutline(true);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.invalidateOutline();
        }
        if (vv != null) {
            vv.setOutlineProvider(provider);
            vv.setClipToOutline(true);
            vv.invalidateOutline();
        }
    }

    private static final android.util.LruCache<String, int[]> mediaSizeCache = new android.util.LruCache<>(200);

    public static void cacheVideoDimensions(String path, int width, int height) {
        if (path != null && width > 0 && height > 0) {
            mediaSizeCache.put(path, new int[]{width, height});
        }
    }

    public static int[] getMediaDimensions(Context context, String pathOrUri, boolean isVideo) {
        if (pathOrUri == null || pathOrUri.isEmpty()) return new int[]{16, 9};
        int[] cached = mediaSizeCache.get(pathOrUri);
        if (cached != null) return cached;

        if (isVideo) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") || pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
                    retriever.setDataSource(context, Uri.parse(pathOrUri));
                } else {
                    retriever.setDataSource(pathOrUri);
                }
                String wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                String rStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (wStr != null && hStr != null) {
                    int w = Integer.parseInt(wStr);
                    int h = Integer.parseInt(hStr);
                    int rot = rStr != null ? Integer.parseInt(rStr) : 0;
                    int[] dims = (rot == 90 || rot == 270) ? new int[]{h, w} : new int[]{w, h};
                    if (dims[0] > 0 && dims[1] > 0) {
                        mediaSizeCache.put(pathOrUri, dims);
                        return dims;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        } else {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                if (pathOrUri.startsWith("content://")) {
                    try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(pathOrUri))) {
                        if (is != null) BitmapFactory.decodeStream(is, null, options);
                    }
                } else if (new File(pathOrUri).exists()) {
                    BitmapFactory.decodeFile(pathOrUri, options);
                }
                if (options.outWidth > 0 && options.outHeight > 0) {
                    int[] dims = new int[]{options.outWidth, options.outHeight};
                    mediaSizeCache.put(pathOrUri, dims);
                    return dims;
                }
            } catch (Exception ignored) {}
        }
        return new int[]{16, 9};
    }

    private void bindSingleMediaItem(ChatMessage.MediaItem item, View container, ChatMessage message, ImageView ivImage, com.messenger.prime.TextureVideoView vvPreview, ImageView ivPlay, TextView tvDur) {
        if (container == null) return;

        ImageView img = ivImage != null ? ivImage : container.findViewById(R.id.ivMessageImage);
        com.messenger.prime.TextureVideoView vv = vvPreview != null ? vvPreview : container.findViewById(R.id.videoMessagePreview);

        float density = container.getContext().getResources().getDisplayMetrics().density;

        // Dynamic aspect ratio calculation so bubble adapts to vertical or horizontal format
        int[] dims = getMediaDimensions(container.getContext(), item.path, item.isVideo);
        int mw = dims[0];
        int mh = dims[1];
        float aspect = (mw > 0 && mh > 0) ? ((float) mw / (float) mh) : (16f / 9f);
        int targetWidthDp;
        int targetHeightDp;

        if (aspect >= 1.2f) {
            // Horizontal / Landscape (e.g. 16:9, 4:3)
            targetWidthDp = 270;
            targetHeightDp = Math.round(targetWidthDp / aspect);
            if (targetHeightDp < 140) targetHeightDp = 140;
            if (targetHeightDp > 210) targetHeightDp = 210;
        } else if (aspect <= 0.85f) {
            // Vertical / Portrait (e.g. 9:16, 3:4)
            targetHeightDp = 280;
            targetWidthDp = Math.round(targetHeightDp * aspect);
            if (targetWidthDp < 160) targetWidthDp = 160;
            if (targetWidthDp > 215) targetWidthDp = 215;
        } else {
            // Square / Near square
            targetWidthDp = 230;
            targetHeightDp = Math.round(targetWidthDp / aspect);
            if (targetHeightDp < 180) targetHeightDp = 180;
            if (targetHeightDp > 240) targetHeightDp = 240;
        }

        int targetW = (int) (targetWidthDp * density);
        int targetH = (int) (targetHeightDp * density);

        View layoutMediaContainer = container.getParent() instanceof View ? (View) container.getParent() : null;
        if (layoutMediaContainer != null && layoutMediaContainer.getId() == R.id.layoutMediaContainer) {
            ViewGroup.LayoutParams lpMedia = layoutMediaContainer.getLayoutParams();
            if (lpMedia != null) {
                lpMedia.width = targetW;
                lpMedia.height = targetH;
                layoutMediaContainer.setLayoutParams(lpMedia);
            }
        }

        ViewGroup.LayoutParams lpSingle = container.getLayoutParams();
        if (lpSingle != null) {
            lpSingle.width = targetW;
            lpSingle.height = targetH;
            container.setLayoutParams(lpSingle);
        }

        if (img != null) {
            ViewGroup.LayoutParams lpImg = img.getLayoutParams();
            if (lpImg != null) {
                lpImg.width = targetW;
                lpImg.height = targetH;
                img.setLayoutParams(lpImg);
            }
        }

        if (vv != null) {
            ViewGroup.LayoutParams lpVv = vv.getLayoutParams();
            if (lpVv != null) {
                lpVv.width = targetW;
                lpVv.height = targetH;
                vv.setLayoutParams(lpVv);
            }
        }

        // Apply very narrow margins between bubble and media (Telegram style)
        View bubble = null;
        View walk = container;
        while (walk != null && walk.getParent() instanceof View) {
            walk = (View) walk.getParent();
            if (walk.getId() == R.id.layoutIncomingBubble || walk.getId() == R.id.layoutOutgoingBubble) {
                bubble = walk;
                break;
            }
        }
        if (bubble != null) {
            int narrowPad = (int) (3.5f * density);
            bubble.setPadding(narrowPad, narrowPad, narrowPad, narrowPad);
        }

        if (layoutMediaContainer != null) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutMediaContainer.getLayoutParams();
            if (mlp != null) {
                boolean hasCaption = message != null && message.getText() != null && !message.getText().trim().isEmpty()
                        && !"Фото".equals(message.getText()) && !"Видео".equals(message.getText()) && !"Видеозапись".equals(message.getText())
                        && !"Фото отправлено".equals(message.getText()) && !"Видео отправлено".equals(message.getText())
                        && !"Файл".equals(message.getText());
                mlp.bottomMargin = (int) ((hasCaption ? 4 : 2) * density);
                layoutMediaContainer.setLayoutParams(mlp);
            }
        }

        float cornerRadiusPx = 14 * density;
        android.view.ViewOutlineProvider clipProvider = new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                int w = view.getWidth() > 0 ? view.getWidth() : targetW;
                int h = view.getHeight() > 0 ? view.getHeight() : targetH;
                outline.setRoundRect(0, 0, w, h, cornerRadiusPx);
            }
        };
        container.setOutlineProvider(clipProvider);
        container.setClipToOutline(true);
        container.invalidateOutline();

        if (message != null && message.getText() != null && message.getText().startsWith("VIDEO_NOTE:")) {
            String shape = message.getText().substring("VIDEO_NOTE:".length()).trim();
            applyShapeToMediaViews(container, img, vv, shape);
        }
        ImageView play = ivPlay != null ? ivPlay : container.findViewById(R.id.ivVideoPlayBadge);
        TextView dur = tvDur != null ? tvDur : container.findViewById(R.id.tvVideoDuration);

        if (item.isVideo) {
            if (play != null) play.setVisibility(View.GONE);
            if (dur != null) {
                dur.setVisibility(View.VISIBLE);
                dur.setText("0:00");
            }
            loadVideoThumbnailIntoView(container.getContext(), item.path, img, null, isScrolling);

            if (vv != null) {
                String p = item.path;
                boolean isPlayable = p != null && !p.isEmpty() && (
                    p.startsWith("content://") || 
                    p.startsWith("http://") || 
                    p.startsWith("https://") || 
                    (p.startsWith("file://") ? new File(p.substring(7)).exists() : new File(p).exists())
                );

                if (!isScrolling && isPlayable) {
                    vv.setOutlineProvider(clipProvider);
                    vv.setClipToOutline(true);
                    vv.invalidateOutline();

                    vv.isMuted = true;
                    vv.loopDurationMs = -1;
                    vv.autoPlayOnPrepared = true;
                    vv.setOnFirstFrameRenderedListener(() -> {
                        if (img != null) {
                            img.setVisibility(View.INVISIBLE);
                        }
                    });
                    vv.setOnProgressUpdateListener((curMs, totalMs) -> {
                        if (dur != null) {
                            int curSec = curMs / 1000;
                            dur.setText(String.format(Locale.getDefault(), "%d:%02d", curSec / 60, curSec % 60));
                        }
                    });
                    vv.setVisibility(View.VISIBLE);
                    vv.bringToFront();
                    if (play != null) play.bringToFront();
                    if (dur != null) dur.bringToFront();
                    vv.setVideoPath(p);
                } else {
                    vv.removeCallbacks(null);
                    try { vv.stopPlayback(); } catch (Exception ignored) {}
                    vv.setVisibility(View.GONE);
                    if (img != null) {
                        img.setVisibility(View.VISIBLE);
                    }
                    if (dur != null) {
                        dur.setText(item.durationStr != null && !item.durationStr.isEmpty() ? item.durationStr : "0:00");
                    }
                }
            }
        } else {
            if (vv != null) {
                vv.removeCallbacks(null);
                try { vv.stopPlayback(); } catch (Exception ignored) {}
                vv.setVisibility(View.GONE);
            }
            if (play != null) play.setVisibility(View.GONE);
            if (dur != null) dur.setVisibility(View.GONE);
            if (img != null) {
                img.setVisibility(View.VISIBLE);
                loadMediaImageIntoView(container.getContext(), item.path, img);
            }
        }
        if (vv != null) {
            vv.setOnTouchListener(new View.OnTouchListener() {
                private float startX, startY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            float diffX = Math.abs(event.getX() - startX);
                            float diffY = Math.abs(event.getY() - startY);
                            if (diffX < 20 && diffY < 20) {
                                v.performClick();
                                showFullScreenMedia(v.getContext(), v, message, messages);
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        if (img != null) setupSingleTapMediaOpener(img, message, messages);
        if (container != null) setupSingleTapMediaOpener(container, message, messages);
        if (dur != null) setupSingleTapMediaOpener(dur, message, messages);
        if (play != null) setupSingleTapMediaOpener(play, message, messages);
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
            if (iv != null) {
                loadMediaImageIntoView(iv.getContext(), item.path, iv);
            }
        } else {
            if (play != null) play.setVisibility(View.GONE);
            if (iv != null) {
                loadMediaImageIntoView(iv.getContext(), item.path, iv);
            }
        }
        if (tile != null) setupSingleTapMediaOpenerIndexed(tile, message, messages, itemIndex);
        if (iv != null) setupSingleTapMediaOpenerIndexed(iv, message, messages, itemIndex);
        if (play != null) setupSingleTapMediaOpenerIndexed(play, message, messages, itemIndex);
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
                loadVideoThumbnailIntoView(holder.itemView.getContext(), item.path, holder.ivSliderImage, null);
            } else {
                holder.ivSliderPlayBadge.setVisibility(View.GONE);
                holder.tvSliderVideoDuration.setVisibility(View.GONE);
                loadMediaImageIntoView(holder.itemView.getContext(), item.path, holder.ivSliderImage);
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

    public static void loadMediaImageIntoView(Context context, String pathOrUri, ImageView imageView) {
        if (imageView == null || context == null) return;
        if (pathOrUri == null || pathOrUri.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_photo);
            return;
        }
        try {
            File f = new File(pathOrUri);
            if (!f.exists() && !pathOrUri.contains("://")) {
                f = new File(context.getFilesDir(), pathOrUri);
            }
            Uri uri = pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") || pathOrUri.startsWith("http")
                    ? Uri.parse(pathOrUri)
                    : Uri.fromFile(f);

            Glide.with(context)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo)
                    .error(R.drawable.ic_photo)
                    .into(imageView);
        } catch (Throwable e) {
            imageView.setImageResource(R.drawable.ic_photo);
        }
    }

    public static void loadVideoThumbnailIntoView(Context context, String pathOrUri, ImageView imageView, View videoView, boolean isScrolling) {
        if (context == null || pathOrUri == null || pathOrUri.isEmpty()) return;

        if (videoView != null) {
            videoView.removeCallbacks(null);
            if (videoView instanceof com.messenger.prime.TextureVideoView) {
                try { ((com.messenger.prime.TextureVideoView) videoView).stopPlayback(); } catch (Exception ignored) {}
            } else if (videoView instanceof VideoView) {
                try { ((VideoView) videoView).stopPlayback(); } catch (Exception ignored) {}
            }
            videoView.setVisibility(View.GONE);
        }

        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
            Uri uri = pathOrUri.startsWith("content://")
                    ? Uri.parse(pathOrUri)
                    : (new File(pathOrUri).exists() ? Uri.fromFile(new File(pathOrUri)) : null);

            Object loadUri = uri != null ? uri : pathOrUri;
            Glide.with(context)
                    .load(loadUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(imageView);
        }
    }

    public static void loadVideoThumbnailIntoView(Context context, String pathOrUri, ImageView imageView, View videoView) {
        loadVideoThumbnailIntoView(context, pathOrUri, imageView, videoView, false);
    }

    public static Bitmap getVideoThumbnail(Context context, String pathOrUri) {
        if (pathOrUri == null || pathOrUri.isEmpty() || context == null) return null;
        try {
            if (pathOrUri.startsWith("content://")) {
                Uri uri = Uri.parse(pathOrUri);
                if (Build.VERSION.SDK_INT >= 29) {
                    try {
                        return context.getContentResolver().loadThumbnail(uri, new Size(512, 512), null);
                    } catch (Exception ignored) {}
                }
                try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                    retriever.setDataSource(context, uri);
                    return retriever.getFrameAtTime(1000000);
                }
            } else if (new File(pathOrUri).exists()) {
                try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                    retriever.setDataSource(pathOrUri);
                    Bitmap frame = retriever.getFrameAtTime(1000000);
                    if (frame != null) return frame;
                } catch (Exception ignored) {}
                return ThumbnailUtils.createVideoThumbnail(pathOrUri, MediaStore.Images.Thumbnails.MINI_KIND);
            }
        } catch (Exception e) {
            Log.e("ChatAdapter", "Failed to load video thumbnail: " + pathOrUri, e);
        }
        return null;
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
            Activity act = getActivityFromContext(context);
            if (act instanceof ChatPersonActivity) {
                ((ChatPersonActivity) act).setOpeningSubActivity(true);
            }

            Uri uri;
            if (filePath.startsWith("content://")) {
                uri = Uri.parse(filePath);
            } else {
                File file = new File(filePath);
                if (!file.exists()) {
                    if (act != null) {
                        PrimeNotification.INSTANCE.show(act, "Файл не найден на устройстве", null);
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
            Activity act = getActivityFromContext(context);
            if (act != null) {
                PrimeNotification.INSTANCE.show(act, "Не удалось открыть файл", null);
            }
        }
    }

    private static String getMimeType(String filePath) {
        if (filePath == null) return "*/*";
        String ext = "";
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot != -1 && lastDot < filePath.length() - 1) {
            ext = filePath.substring(lastDot + 1).toLowerCase(Locale.US);
        }
        if (!ext.isEmpty()) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (type != null && !type.isEmpty()) {
                return type;
            }
        }
        return "*/*";
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
        com.messenger.prime.TextureVideoView videoMessagePreview;
        View layoutFileContainer;
        ImageView ivFileIcon;
        TextView tvFileName;
        TextView tvFileSize;
        ImageButton btnFileDownload;
        View layoutDownloadProgress;
        com.messenger.prime.PrimeWaterLoadingView pbDownloadProgress;
        TextView tvDownloadPercent;
        ImageButton btnCancelDownload;
        View layoutSelectionCircle;
        ImageView ivSelectionCheck;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSelectionCircle = itemView.findViewById(R.id.layoutSelectionCircle);
            ivSelectionCheck = itemView.findViewById(R.id.ivSelectionCheck);
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
            float[] lastTouchRaw = new float[2];
            GestureDetector doubleTapDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    if (isConnectionActive && actionListener != null) {
                        actionListener.onQuickReaction(message, "❤️", position);
                        itemView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        return true;
                    }
                    return false;
                }
            });

            View.OnTouchListener touchTracker = (v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastTouchRaw[0] = event.getRawX();
                    lastTouchRaw[1] = event.getRawY();
                }
                doubleTapDetector.onTouchEvent(event);
                return false;
            };

            itemView.setOnTouchListener(touchTracker);
            if (layoutIncomingBubble != null) {
                layoutIncomingBubble.setOnTouchListener(touchTracker);
            }

            if (layoutIncomingBubble != null) {
                layoutIncomingBubble.setBackground(ColorAccentManager.createIncomingBubbleDrawable(itemView.getContext()));
            }

            View layoutUnreadHeader = itemView.findViewById(R.id.layoutUnreadHeader);
            if (layoutUnreadHeader != null) {
                layoutUnreadHeader.setVisibility(message.isFirstUnread() ? View.VISIBLE : View.GONE);
            }

            bindDateHeader(itemView, tvDateHeader, showDateHeader, message.getTimestamp());

            if (layoutQuotedReply != null) {
                if (message.isReply()) {
                    layoutQuotedReply.setVisibility(View.VISIBLE);
                    if (tvQuotedSender != null) tvQuotedSender.setText(message.getReplyToSender());
                    if (tvQuotedText != null) {
                        String qText = message.getReplyToText();
                        if (qText != null && (qText.startsWith("VIDEO_NOTE:") || qText.startsWith("vn_"))) {
                            qText = "📹 Видеосообщение";
                        }
                        tvQuotedText.setText(qText != null && !qText.isEmpty() ? qText : "Вложение");
                    }
                    layoutQuotedReply.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        if (actionListener != null && message.getReplyToMessageId() != null) {
                            actionListener.onJumpToMessage(message.getReplyToMessageId(), message.getReplyToText());
                        }
                    });
                } else {
                    layoutQuotedReply.setVisibility(View.GONE);
                }
            }

            String text = message.getText();
            boolean isVideoNote = message != null && message.isVideoNote();
            if (isVideoNote) {
                bindVideoNoteMessage(itemView, message, false, position);
            } else {
                resetBubbleForNormalMessage(itemView, false);
            }

            if (message != null && message.isRichFormatted()) {
                if (layoutIncomingBubble != null) {
                    ViewGroup.LayoutParams lp = layoutIncomingBubble.getLayoutParams();
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        layoutIncomingBubble.setLayoutParams(lp);
                    }
                }
                View block = itemView.findViewById(R.id.layoutIncomingMessageBlock);
                if (block != null) {
                    ViewGroup.LayoutParams lpBlock = block.getLayoutParams();
                    if (lpBlock != null) {
                        lpBlock.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        block.setLayoutParams(lpBlock);
                    }
                }
            }

            bindMessageAttachments(
                message, position,
                layoutMediaContainer, ivMessageImage, videoMessagePreview, ivVideoPlayBadge, tvVideoDuration,
                layoutFileContainer, ivFileIcon, tvFileName, tvFileSize,
                btnFileDownload, layoutDownloadProgress, pbDownloadProgress, tvDownloadPercent, btnCancelDownload
            );

            if (!isVideoNote && message.getMessageType() != ChatMessage.MessageType.VOICE && text != null && !text.isEmpty() && !"Голосовое сообщение".equals(text) && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text) && !"Файл".equals(text) && !"Видео".equals(text) && !"Видеозапись".equals(text) && !"Видео отправлено".equals(text)) {
                View ivRichBadgeIn = itemView.findViewById(R.id.ivRichBadgeIn);
                if (ivRichBadgeIn != null) {
                    ivRichBadgeIn.setVisibility(message.isRichFormatted() ? View.VISIBLE : View.GONE);
                }

                if (message.isRichFormatted()) {
                    tvMessageText.setVisibility(View.GONE);
                    WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                    if (wvRichMessage != null) {
                        wvRichMessage.setVisibility(View.VISIBLE);
                        wvRichMessage.getSettings().setJavaScriptEnabled(false);
                        wvRichMessage.setOnTouchListener((v, event) -> {
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                            }
                            if (isMultiSelectionMode) {
                                if (event.getAction() == MotionEvent.ACTION_UP) {
                                    toggleMessageSelection(message);
                                }
                                return true;
                            }
                            return false;
                        });
                        wvRichMessage.setOnLongClickListener(v -> {
                            if (!isMultiSelectionMode) {
                                showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                            } else {
                                toggleMessageSelection(message);
                            }
                            return true;
                        });

                        String htmlContent = message.getCleanText();
                        boolean isDark = (itemView.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                        String textColor = isDark ? "#E0E0E0" : "#121212";
                        String linkColor = isDark ? "#90CAF9" : "#1976D2";
                        String tableBg = isDark ? "#262626" : "#EEEEEE";
                        String thBg = isDark ? "#333333" : "#DDDDDD";
                        String borderColor = isDark ? "#555555" : "#CCCCCC";

                        String css = "<style>body { margin: 0; padding: 0; font-family: sans-serif; font-size: 16px; color: " + textColor + "; background-color: transparent; line-height: 1.4; word-wrap: break-word; }" +
                            "img { max-width: 100%; height: auto; border-radius: 8px; margin: 4px 0; } " +
                            "table { width: 100%; border-collapse: collapse; margin: 8px 0; background-color: " + tableBg + "; border-radius: 6px; overflow: hidden; } " +
                            "th, td { border: 1px solid " + borderColor + "; padding: 6px; text-align: left; } th { background-color: " + thBg + "; font-weight: bold; } " +
                            "a { color: " + linkColor + "; text-decoration: underline; }</style>";
                        String fullHtml = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" + css + "</head><body>" + htmlContent + "</body></html>";
                        wvRichMessage.loadDataWithBaseURL("file:///android_asset/", fullHtml, "text/html", "UTF-8", null);
                        wvRichMessage.setBackgroundColor(0x00000000);
                    }
                } else {
                    WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                    if (wvRichMessage != null) {
                        wvRichMessage.setVisibility(View.GONE);
                        wvRichMessage.loadUrl("about:blank");
                    }
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(message.getCleanText());
                    float density = itemView.getResources().getDisplayMetrics().density;
                    tvMessageText.setPadding((int) (6 * density), (int) (4 * density), (int) (6 * density), (int) (2 * density));
                    tvMessageText.setMovementMethod(com.messenger.prime.browser.AppBrowserMovementMethod.getInstance());
                    tvMessageText.setTextIsSelectable(true);
                    setupCustomTextSelectionActionMode(itemView, tvMessageText, message, position);
                    bindLinkPreview(itemView, text, message);
                    tvMessageText.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                        } else if (!tvMessageText.hasSelection()) {
                            showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                        }
                    });
                }
            } else {
                tvMessageText.setVisibility(View.GONE);
                WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                if (wvRichMessage != null) {
                    wvRichMessage.setVisibility(View.GONE);
                    wvRichMessage.loadUrl("about:blank");
                }
                View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
                if (layoutLinkPreview != null) layoutLinkPreview.setVisibility(View.GONE);
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));
            View layoutMessageTime = itemView.findViewById(R.id.layoutMessageTimeContainer);
            if (layoutMessageTime != null) {
                layoutMessageTime.setVisibility(isVideoNote ? View.GONE : View.VISIBLE);
            } else {
                tvMessageTime.setVisibility(isVideoNote ? View.GONE : View.VISIBLE);
            }

            if (layoutMessageReaction instanceof LinearLayout) {
                bindReactions((LinearLayout) layoutMessageReaction, message, position);
            }

            if (layoutMessageActions != null) {
                layoutMessageActions.setVisibility(View.GONE);
            }

            boolean isSelected = isSelectedInMultiMode(message);

            if (layoutSelectionCircle != null) {
                if (isMultiSelectionMode) {
                    layoutSelectionCircle.setVisibility(View.VISIBLE);
                    if (ivSelectionCheck != null) {
                        if (isSelected) {
                            ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_active);
                            ivSelectionCheck.setImageResource(R.drawable.ic_done);
                            ivSelectionCheck.setAlpha(1.0f);
                            ivSelectionCheck.setScaleX(1.0f);
                            ivSelectionCheck.setScaleY(1.0f);
                        } else {
                            ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_idle);
                            ivSelectionCheck.setImageDrawable(null);
                            ivSelectionCheck.setAlpha(0.6f);
                            ivSelectionCheck.setScaleX(1.0f);
                            ivSelectionCheck.setScaleY(1.0f);
                        }
                    }
                } else {
                    layoutSelectionCircle.setVisibility(View.GONE);
                }
            }

            itemView.setAlpha(isSelected ? 0.85f : 1.0f);

            View.OnClickListener bubbleClickListener = v -> {
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                } else {
                    showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                }
            };

            if (layoutIncomingBubble != null) {
                layoutIncomingBubble.setOnClickListener(bubbleClickListener);
            }

            itemView.setOnClickListener(bubbleClickListener);
            itemView.setOnLongClickListener(v -> {
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                } else {
                    showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
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
        com.messenger.prime.TextureVideoView videoMessagePreview;
        View layoutFileContainer;
        ImageView ivFileIcon;
        TextView tvFileName;
        TextView tvFileSize;
        ImageButton btnFileDownload;
        View layoutDownloadProgress;
        com.messenger.prime.PrimeWaterLoadingView pbDownloadProgress;
        TextView tvDownloadPercent;
        ImageButton btnCancelDownload;
        View layoutSelectionCircle;
        ImageView ivSelectionCheck;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSelectionCircle = itemView.findViewById(R.id.layoutSelectionCircle);
            ivSelectionCheck = itemView.findViewById(R.id.ivSelectionCheck);
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
            float[] lastTouchRaw = new float[2];
            GestureDetector doubleTapDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    if (isConnectionActive && actionListener != null) {
                        actionListener.onQuickReaction(message, "❤️", position);
                        itemView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        return true;
                    }
                    return false;
                }
            });

            View.OnTouchListener touchTracker = (v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastTouchRaw[0] = event.getRawX();
                    lastTouchRaw[1] = event.getRawY();
                }
                doubleTapDetector.onTouchEvent(event);
                return false;
            };

            itemView.setOnTouchListener(touchTracker);
            if (layoutOutgoingBubble != null) {
                layoutOutgoingBubble.setOnTouchListener(touchTracker);
            }

            if (layoutOutgoingBubble != null) {
                layoutOutgoingBubble.setBackground(ColorAccentManager.createOutgoingBubbleDrawable(itemView.getContext()));
            }

            bindDateHeader(itemView, tvDateHeader, showDateHeader, message.getTimestamp());

            if (layoutQuotedReply != null) {
                if (message.isReply()) {
                    layoutQuotedReply.setVisibility(View.VISIBLE);
                    if (tvQuotedSender != null) tvQuotedSender.setText(message.getReplyToSender());
                    if (tvQuotedText != null) {
                        String qText = message.getReplyToText();
                        if (qText != null && (qText.startsWith("VIDEO_NOTE:") || qText.startsWith("vn_"))) {
                            qText = "📹 Видеосообщение";
                        }
                        tvQuotedText.setText(qText != null && !qText.isEmpty() ? qText : "Вложение");
                    }
                    layoutQuotedReply.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                            return;
                        }
                        if (actionListener != null && message.getReplyToMessageId() != null) {
                            actionListener.onJumpToMessage(message.getReplyToMessageId(), message.getReplyToText());
                        }
                    });
                } else {
                    layoutQuotedReply.setVisibility(View.GONE);
                }
            }

            String text = message.getText();
            boolean isVideoNoteOutgoing = message != null && message.isVideoNote();
            if (isVideoNoteOutgoing) {
                bindVideoNoteMessage(itemView, message, true, position);
            } else {
                resetBubbleForNormalMessage(itemView, true);
            }

            if (message != null && message.isRichFormatted()) {
                if (layoutOutgoingBubble != null) {
                    ViewGroup.LayoutParams lp = layoutOutgoingBubble.getLayoutParams();
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        layoutOutgoingBubble.setLayoutParams(lp);
                    }
                }
                View block = itemView.findViewById(R.id.layoutOutgoingMessageBlock);
                if (block != null) {
                    ViewGroup.LayoutParams lpBlock = block.getLayoutParams();
                    if (lpBlock != null) {
                        lpBlock.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        block.setLayoutParams(lpBlock);
                    }
                }
            }

            bindMessageAttachments(
                message, position,
                layoutMediaContainer, ivMessageImage, videoMessagePreview, ivVideoPlayBadge, tvVideoDuration,
                layoutFileContainer, ivFileIcon, tvFileName, tvFileSize,
                btnFileDownload, layoutDownloadProgress, pbDownloadProgress, tvDownloadPercent, btnCancelDownload
            );

            if (!isVideoNoteOutgoing && message.getMessageType() != ChatMessage.MessageType.VOICE && text != null && !text.isEmpty() && !"Голосовое сообщение".equals(text) && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text) && !"Файл".equals(text) && !"Видео".equals(text) && !"Видеозапись".equals(text) && !"Видео отправлено".equals(text)) {
                View ivRichBadgeOut = itemView.findViewById(R.id.ivRichBadgeOut);
                if (ivRichBadgeOut != null) {
                    ivRichBadgeOut.setVisibility(message.isRichFormatted() ? View.VISIBLE : View.GONE);
                }

                if (message.isRichFormatted()) {
                    tvMessageText.setVisibility(View.GONE);
                    WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                    if (wvRichMessage != null) {
                        wvRichMessage.setVisibility(View.VISIBLE);
                        wvRichMessage.getSettings().setJavaScriptEnabled(false);
                        wvRichMessage.setOnTouchListener((v, event) -> {
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                            }
                            if (isMultiSelectionMode) {
                                if (event.getAction() == MotionEvent.ACTION_UP) {
                                    toggleMessageSelection(message);
                                }
                                return true;
                            }
                            return false;
                        });
                        wvRichMessage.setOnLongClickListener(v -> {
                            if (!isMultiSelectionMode) {
                                showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                            } else {
                                toggleMessageSelection(message);
                            }
                            return true;
                        });

                        String htmlContent = message.getCleanText();
                        boolean isDark = (itemView.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                        String textColor = "#FFFFFF";
                        String linkColor = "#FFFFFF";
                        String tableBg = isDark ? "rgba(0,0,0,0.2)" : "rgba(255,255,255,0.2)";
                        String thBg = isDark ? "rgba(0,0,0,0.3)" : "rgba(255,255,255,0.3)";
                        String borderColor = isDark ? "rgba(255,255,255,0.3)" : "rgba(255,255,255,0.5)";

                        String css = "<style>body { margin: 0; padding: 0; font-family: sans-serif; font-size: 16px; color: " + textColor + "; background-color: transparent; line-height: 1.4; word-wrap: break-word; }" +
                            "img { max-width: 100%; height: auto; border-radius: 8px; margin: 4px 0; } " +
                            "table { width: 100%; border-collapse: collapse; margin: 8px 0; background-color: " + tableBg + "; border-radius: 6px; overflow: hidden; } " +
                            "th, td { border: 1px solid " + borderColor + "; padding: 6px; text-align: left; } th { background-color: " + thBg + "; font-weight: bold; } " +
                            "a { color: " + linkColor + "; text-decoration: underline; }</style>";
                        String fullHtml = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" + css + "</head><body>" + htmlContent + "</body></html>";
                        wvRichMessage.loadDataWithBaseURL("file:///android_asset/", fullHtml, "text/html", "UTF-8", null);
                        wvRichMessage.setBackgroundColor(0x00000000);
                    }
                } else {
                    WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                    if (wvRichMessage != null) {
                        wvRichMessage.setVisibility(View.GONE);
                        wvRichMessage.loadUrl("about:blank");
                    }
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(message.getCleanText());
                    float density = itemView.getResources().getDisplayMetrics().density;
                    tvMessageText.setPadding((int) (6 * density), (int) (4 * density), (int) (6 * density), (int) (2 * density));
                    tvMessageText.setMovementMethod(com.messenger.prime.browser.AppBrowserMovementMethod.getInstance());
                    tvMessageText.setTextIsSelectable(true);
                    setupCustomTextSelectionActionMode(itemView, tvMessageText, message, position);
                    bindLinkPreview(itemView, text, message);
                    tvMessageText.setOnClickListener(v -> {
                        if (isMultiSelectionMode) {
                            toggleMessageSelection(message);
                        } else if (!tvMessageText.hasSelection()) {
                            showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                        }
                    });
                }
            } else {
                tvMessageText.setVisibility(View.GONE);
                WebView wvRichMessage = itemView.findViewById(R.id.wvRichMessage);
                if (wvRichMessage != null) {
                    wvRichMessage.setVisibility(View.GONE);
                    wvRichMessage.loadUrl("about:blank");
                }
                View layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
                if (layoutLinkPreview != null) layoutLinkPreview.setVisibility(View.GONE);
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));
            View layoutMessageTime = itemView.findViewById(R.id.layoutMessageTimeContainer);
            if (layoutMessageTime != null) {
                layoutMessageTime.setVisibility(isVideoNoteOutgoing ? View.GONE : View.VISIBLE);
            } else {
                tvMessageTime.setVisibility(isVideoNoteOutgoing ? View.GONE : View.VISIBLE);
            }

            if (layoutMessageReaction instanceof LinearLayout) {
                bindReactions((LinearLayout) layoutMessageReaction, message, position);
            }

            if (ivMessageStatus != null) {
                if (message.getMessageStatus() == MessageStatus.SENDING) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_clock);
                    ivMessageStatus.setOnClickListener(null);
                } else if (message.getMessageStatus() == MessageStatus.READ) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_done_all);
                    ivMessageStatus.setOnClickListener(null);
                } else if (message.getMessageStatus() == MessageStatus.SENT) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_done);
                    ivMessageStatus.setOnClickListener(null);
                } else if (message.getMessageStatus() == MessageStatus.ERROR) {
                    ivMessageStatus.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_error);
                    ivMessageStatus.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onRetryMessage(message, position);
                        }
                    });
                } else {
                    ivMessageStatus.setVisibility(View.GONE);
                    ivMessageStatus.setOnClickListener(null);
                }
            }

            View layoutMessageProgress = itemView.findViewById(R.id.layoutMessageProgress);
            com.messenger.prime.PrimeWaterLoadingView pbMessageProgress = itemView.findViewById(R.id.pbMessageProgress);
            TextView tvMessageProgressPercent = itemView.findViewById(R.id.tvMessageProgressPercent);
            ImageButton btnCancelMessageSending = itemView.findViewById(R.id.btnCancelMessageSending);

            if (layoutMessageProgress != null) {
                if (message.getMessageStatus() == MessageStatus.SENDING && message.getSendingProgress() > 0 && message.getSendingProgress() < 100) {
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
            }

            boolean isSelected = isSelectedInMultiMode(message);

            if (layoutSelectionCircle != null) {
                if (isMultiSelectionMode) {
                    layoutSelectionCircle.setVisibility(View.VISIBLE);
                    if (ivSelectionCheck != null) {
                        if (isSelected) {
                            ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_active);
                            ivSelectionCheck.setImageResource(R.drawable.ic_done);
                            ivSelectionCheck.setAlpha(1.0f);
                            ivSelectionCheck.setScaleX(1.0f);
                            ivSelectionCheck.setScaleY(1.0f);
                        } else {
                            ivSelectionCheck.setBackgroundResource(R.drawable.bg_circular_mode_idle);
                            ivSelectionCheck.setImageDrawable(null);
                            ivSelectionCheck.setAlpha(0.6f);
                            ivSelectionCheck.setScaleX(1.0f);
                            ivSelectionCheck.setScaleY(1.0f);
                        }
                    }
                } else {
                    layoutSelectionCircle.setVisibility(View.GONE);
                }
            }

            itemView.setAlpha(isSelected ? 0.85f : 1.0f);

            View.OnClickListener bubbleClickListener = v -> {
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                } else {
                    showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                }
            };

            if (layoutOutgoingBubble != null) {
                layoutOutgoingBubble.setOnClickListener(bubbleClickListener);
            }

            itemView.setOnClickListener(bubbleClickListener);
            itemView.setOnLongClickListener(v -> {
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                } else {
                    showMessageContextMenu(v, message, position, lastTouchRaw[0], lastTouchRaw[1]);
                }
                return true;
            });
        }
    }

    public boolean isSelectedInMultiMode(ChatMessage message) {
        return isMultiSelectionMode && message != null && message.getMessageId() != null && selectedMessageIds.contains(message.getMessageId());
    }

    private void showMessageContextMenu(View anchorView, ChatMessage message, int position, float touchRawX, float touchRawY) {
        if (anchorView == null || message == null) return;

        Context context = anchorView.getContext();
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_message_context_menu, null);

        eightbitlab.com.blurview.BlurView blurContextMenu = menuView.findViewById(R.id.blurContextMenu);
        if (blurContextMenu != null) {
            Activity act = getActivityFromContext(context);
            if (act != null) {
                ViewGroup rootView = act.findViewById(android.R.id.content);
                if (rootView == null) rootView = (ViewGroup) act.getWindow().getDecorView();
                boolean isDark = (act.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int overlayColor = isDark ? Color.parseColor("#450F172A") : Color.parseColor("#45154B87");
                com.messenger.prime.BlurViewKt.setupBlur(blurContextMenu, rootView, 18f, overlayColor, act.getWindow().getDecorView().getBackground());
            }
        }

        PopupWindow popupWindow = new PopupWindow(
                menuView,
                (int) (250 * context.getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setElevation(20f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View layoutReactionsRow = menuView.findViewById(R.id.layoutReactionsRow);
        View btnMenuReply = menuView.findViewById(R.id.btnMenuReply);
        View btnMenuCopy = menuView.findViewById(R.id.btnMenuCopy);
        View btnMenuForward = menuView.findViewById(R.id.btnMenuForward);
        View btnMenuEdit = menuView.findViewById(R.id.btnMenuEdit);
        View btnMenuDelete = menuView.findViewById(R.id.btnMenuDelete);
        View btnMenuSelect = menuView.findViewById(R.id.btnMenuSelect);

        // Check connection status: when offline/disconnected, only Copy is available
        if (!isConnectionActive) {
            if (layoutReactionsRow != null) layoutReactionsRow.setVisibility(View.GONE);
            if (btnMenuReply != null) btnMenuReply.setVisibility(View.GONE);
            if (btnMenuForward != null) btnMenuForward.setVisibility(View.GONE);
            if (btnMenuEdit != null) btnMenuEdit.setVisibility(View.GONE);
            if (btnMenuDelete != null) btnMenuDelete.setVisibility(View.GONE);
            if (btnMenuSelect != null) btnMenuSelect.setVisibility(View.GONE);
            if (btnMenuCopy != null) btnMenuCopy.setVisibility(View.VISIBLE);
        } else {
            // Online/Connected state
            if (layoutReactionsRow != null) layoutReactionsRow.setVisibility(View.VISIBLE);
            if (btnMenuReply != null) btnMenuReply.setVisibility(View.VISIBLE);
            if (btnMenuCopy != null) btnMenuCopy.setVisibility(View.VISIBLE);
            if (btnMenuForward != null) btnMenuForward.setVisibility(View.VISIBLE);
            if (btnMenuSelect != null) btnMenuSelect.setVisibility(View.VISIBLE);

            boolean isMyMessage = message.isOutgoing();
            boolean isTextMessage = message.getText() != null && !message.getText().isEmpty()
                    && !"Фото".equals(message.getText()) && !"Файл".equals(message.getText())
                    && !message.getText().startsWith("VIDEO_NOTE:");

            if (btnMenuEdit != null) {
                btnMenuEdit.setVisibility((isMyMessage && isTextMessage) ? View.VISIBLE : View.GONE);
            }
            if (btnMenuDelete != null) {
                btnMenuDelete.setVisibility(isMyMessage ? View.VISIBLE : View.GONE);
            }
        }

        // Setup Reactions listeners
        View.OnClickListener reactionClick = v -> {
            popupWindow.dismiss();
            if (actionListener != null) {
                String emoji = "👍";
                int id = v.getId();
                if (id == R.id.btnReactionThumbUp) emoji = "👍";
                else if (id == R.id.btnReactionHeart) emoji = "❤️";
                else if (id == R.id.btnReactionJoy) emoji = "😂";
                else if (id == R.id.btnReactionWow) emoji = "😮";
                else if (id == R.id.btnReactionSad) emoji = "😢";
                else if (id == R.id.btnReactionPray) emoji = "🙏";

                actionListener.onQuickReaction(message, emoji, position);
            }
        };

        View btnThumbUp = menuView.findViewById(R.id.btnReactionThumbUp);
        View btnHeart = menuView.findViewById(R.id.btnReactionHeart);
        View btnJoy = menuView.findViewById(R.id.btnReactionJoy);
        View btnWow = menuView.findViewById(R.id.btnReactionWow);
        View btnSad = menuView.findViewById(R.id.btnReactionSad);
        View btnPray = menuView.findViewById(R.id.btnReactionPray);
        View btnExpand = menuView.findViewById(R.id.btnExpandReactions);

        if (btnThumbUp != null) btnThumbUp.setOnClickListener(reactionClick);
        if (btnHeart != null) btnHeart.setOnClickListener(reactionClick);
        if (btnJoy != null) btnJoy.setOnClickListener(reactionClick);
        if (btnWow != null) btnWow.setOnClickListener(reactionClick);
        if (btnSad != null) btnSad.setOnClickListener(reactionClick);
        if (btnPray != null) btnPray.setOnClickListener(reactionClick);

        View layoutExpandedReactionsContainer = menuView.findViewById(R.id.layoutExpandedReactionsContainer);
        GridLayout gridExpandedReactions = menuView.findViewById(R.id.gridExpandedReactions);

        if (btnExpand != null) {
            btnExpand.setRotation(0f); // При открытии меню иконка всегда "+" (0°)

            btnExpand.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

                boolean isEmojisOpen = layoutExpandedReactionsContainer != null
                        && layoutExpandedReactionsContainer.getVisibility() == View.VISIBLE;

                if (isEmojisOpen) {
                    // --- Возврат от эмоций к действиям (Крестик 'x' -> Плюсик '+') ---

                    // 1. Поворот кнопки 'x' (135°) назад к плюсику '+' (0°)
                    btnExpand.animate()
                            .rotation(0f)
                            .scaleX(1.25f)
                            .scaleY(1.25f)
                            .setDuration(240L)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .withEndAction(() -> btnExpand.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100L).start())
                            .start();

                    // 2. Анимация затухания сетки эмодзи
                    gridExpandedReactions.animate()
                            .alpha(0f)
                            .setDuration(140L)
                            .start();

                    // 3. Плавная анимация уменьшения высоты меню
                    android.transition.TransitionManager.beginDelayedTransition(
                            blurContextMenu != null ? blurContextMenu : (ViewGroup) menuView,
                            new android.transition.AutoTransition()
                                    .setDuration(280L)
                                    .setInterpolator(new DecelerateInterpolator())
                    );

                    layoutExpandedReactionsContainer.setVisibility(View.GONE);

                    // 4. Возвращаем отображение пунктов действий меню
                    View[] allButtons = new View[]{
                            btnMenuReply, btnMenuCopy, btnMenuForward,
                            btnMenuEdit, btnMenuDelete, btnMenuSelect
                    };

                    for (View btn : allButtons) {
                        if (btn == null) continue;
                        boolean shouldShow = false;
                        if (!isConnectionActive) {
                            shouldShow = (btn == btnMenuCopy);
                        } else {
                            if (btn == btnMenuReply || btn == btnMenuCopy || btn == btnMenuForward || btn == btnMenuSelect) {
                                shouldShow = true;
                            } else if (btn == btnMenuEdit) {
                                boolean isMyMessage = message.isOutgoing();
                                boolean isTextMessage = message.getText() != null && !message.getText().isEmpty()
                                        && !"Фото".equals(message.getText()) && !"Файл".equals(message.getText())
                                        && !message.getText().startsWith("VIDEO_NOTE:");
                                shouldShow = isMyMessage && isTextMessage;
                            } else if (btn == btnMenuDelete) {
                                shouldShow = message.isOutgoing();
                            }
                        }
                        if (shouldShow) {
                            btn.setVisibility(View.VISIBLE);
                            btn.setAlpha(0f);
                            btn.animate().alpha(1.0f).setDuration(200L).setStartDelay(40L).start();
                        } else {
                            btn.setVisibility(View.GONE);
                        }
                    }

                } else {
                    // --- Раскрытие реакций из действий (Плюсик '+' -> Крестик 'x') ---

                    // 1. Поворот кнопки плюсик '+' (0°) в крестик 'x' (135°)
                    btnExpand.animate()
                            .rotation(135f)
                            .scaleX(1.25f)
                            .scaleY(1.25f)
                            .setDuration(240L)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .withEndAction(() -> btnExpand.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100L).start())
                            .start();

                    if (layoutExpandedReactionsContainer != null && gridExpandedReactions != null) {
                        // 2. Скрываем пункты действий меню
                        View[] menuButtons = new View[]{
                                btnMenuReply, btnMenuCopy, btnMenuForward,
                                btnMenuEdit, btnMenuDelete, btnMenuSelect
                        };

                        for (View btn : menuButtons) {
                            if (btn != null && btn.getVisibility() == View.VISIBLE) {
                                btn.animate()
                                        .alpha(0f)
                                        .setDuration(140L)
                                        .withEndAction(() -> btn.setVisibility(View.GONE))
                                        .start();
                            }
                        }

                        // 3. Наполняем расширенную сетку эмодзи
                        gridExpandedReactions.setAlpha(1.0f);
                        gridExpandedReactions.removeAllViews();
                        float density = context.getResources().getDisplayMetrics().density;
                        int sizePx = (int) (36 * density);

                        String[] emojiList = new String[]{
                                "❤️", "🔥", "👍", "👎", "😂", "😮", "😢", "🙏", "👏", "🎉",
                                "💯", "🥰", "🤩", "🥳", "🤡", "💩", "😡", "🤯", "😱", "🥺",
                                "⚡", "🌟", "😴", "🤮", "🤝", "🤖", "🎃", "👑", "🎯", "🚀",
                                "💔", "👀", "🙈", "😜", "😎", "🤔", "🤫", "🙄", "🫠", "💀"
                        };

                        for (int i = 0; i < emojiList.length; i++) {
                            String emoji = emojiList[i];
                            TextView tv = new TextView(context);
                            tv.setText(emoji);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                            tv.setGravity(Gravity.CENTER);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                            tv.setLayoutParams(lp);
                            tv.setBackgroundResource(R.drawable.bg_emoji_chip);
                            tv.setFocusable(true);
                            tv.setClickable(true);

                            tv.setAlpha(0f);
                            tv.setScaleX(0.2f);
                            tv.setScaleY(0.2f);

                            tv.setOnClickListener(emojiView -> {
                                emojiView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                emojiView.animate()
                                        .scaleX(1.4f)
                                        .scaleY(1.4f)
                                        .setDuration(120L)
                                        .start();

                                menuView.animate()
                                        .scaleX(0.88f)
                                        .scaleY(0.88f)
                                        .alpha(0f)
                                        .setDuration(160L)
                                        .withEndAction(popupWindow::dismiss)
                                        .start();

                                if (actionListener != null) {
                                    actionListener.onQuickReaction(message, emoji, position);
                                }
                            });

                            gridExpandedReactions.addView(tv);

                            long delay = 60L + (i / 6) * 30L + (i % 6) * 12L;
                            tv.animate()
                                    .alpha(1.0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setStartDelay(delay)
                                    .setDuration(220L)
                                    .setInterpolator(new OvershootInterpolator(1.3f))
                                    .start();
                        }

                        // 4. Плавное расширение высоты меню
                        android.transition.TransitionManager.beginDelayedTransition(
                                blurContextMenu != null ? blurContextMenu : (ViewGroup) menuView,
                                new android.transition.AutoTransition()
                                        .setDuration(280L)
                                        .setInterpolator(new DecelerateInterpolator())
                        );

                        layoutExpandedReactionsContainer.setVisibility(View.VISIBLE);
                    } else {
                        popupWindow.dismiss();
                        if (actionListener != null) {
                            actionListener.onExpandReactions(message, anchorView, position);
                        }
                    }
                }
            });
        }

        // Setup Item Listeners
        if (btnMenuReply != null) {
            btnMenuReply.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (actionListener != null) {
                    actionListener.onReplyMessage(message, position);
                }
            });
        }

        if (btnMenuCopy != null) {
            btnMenuCopy.setOnClickListener(v -> {
                popupWindow.dismiss();
                String copyText = message.getText();
                if (copyText == null || copyText.isEmpty()) {
                    copyText = message.getImagePath() != null ? message.getImagePath() : "";
                }
                if (!copyText.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("chat_message", copyText);
                        clipboard.setPrimaryClip(clip);
                        Activity act = getActivityFromContext(context);
                        if (act != null) {
                            PrimeNotification.INSTANCE.show(act, "Сообщение скопировано", null);
                        }
                    }
                }
            });
        }

        if (btnMenuForward != null) {
            btnMenuForward.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (actionListener != null) {
                    actionListener.onForwardMessage(message, position);
                }
            });
        }

        if (btnMenuEdit != null) {
            btnMenuEdit.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (actionListener != null) {
                    actionListener.onEditMessage(message, position);
                }
            });
        }

        if (btnMenuDelete != null) {
            btnMenuDelete.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (actionListener != null) {
                    actionListener.onDeleteMessage(message, position);
                }
            });
        }

        if (btnMenuSelect != null) {
            btnMenuSelect.setOnClickListener(v -> {
                popupWindow.dismiss();
                setMultiSelectionMode(true);
                toggleMessageSelection(message);
                if (actionListener != null) {
                    actionListener.onSelectMessage(message, position);
                }
            });
        }

        // Anchor & Position calculation from touch point
        menuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int menuWidth = menuView.getMeasuredWidth();
        int menuHeight = menuView.getMeasuredHeight();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        float touchX = touchRawX > 0 ? touchRawX : (location[0] + anchorView.getWidth() / 2f);
        float touchY = touchRawY > 0 ? touchRawY : (location[1] + anchorView.getHeight() / 2f);

        int x = (int) (touchX - menuWidth / 2f);
        x = Math.max(16, Math.min(x, screenWidth - menuWidth - 16));

        boolean showAbove = touchY > (screenHeight * 0.55f);

        int y;
        if (showAbove) {
            y = (int) (touchY - menuHeight - (8 * context.getResources().getDisplayMetrics().density));
            if (y < (60 * context.getResources().getDisplayMetrics().density)) {
                y = (int) (touchY + (8 * context.getResources().getDisplayMetrics().density));
            }
        } else {
            y = (int) (touchY + (8 * context.getResources().getDisplayMetrics().density));
            if (y + menuHeight > screenHeight - (60 * context.getResources().getDisplayMetrics().density)) {
                y = (int) (touchY - menuHeight - (8 * context.getResources().getDisplayMetrics().density));
            }
        }

        // Set expansion pivot to exact touch position relative to menu
        float pivotX = Math.max(0f, Math.min(touchX - x, (float) menuWidth));
        float pivotY = (y < touchY) ? (float) menuHeight : 0f;

        menuView.setPivotX(pivotX);
        menuView.setPivotY(pivotY);

        menuView.setScaleX(0.15f);
        menuView.setScaleY(0.15f);
        menuView.setAlpha(0f);

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, Math.max(80, y));

        menuView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void setupCustomTextSelectionActionMode(View itemView, TextView textView, ChatMessage message, int position) {
        if (textView == null || itemView == null) return;

        textView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            private ActionMode activeMode = null;
            private PopupWindow activePopupWindow = null;
            private boolean isInternalDismiss = false;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                activeMode = mode;
                menu.clear();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                if (activePopupWindow != null && activePopupWindow.isShowing()) {
                    isInternalDismiss = true;
                    try { activePopupWindow.dismiss(); } catch (Exception ignored) {}
                    activePopupWindow = null;
                    isInternalDismiss = false;
                }

                Context context = textView.getContext();
                View menuView = LayoutInflater.from(context).inflate(R.layout.layout_text_selection_menu, null);

                eightbitlab.com.blurview.BlurView blurTextSelectionMenu = menuView.findViewById(R.id.blurTextSelectionMenu);
                if (blurTextSelectionMenu != null) {
                    Activity act = getActivityFromContext(context);
                    if (act != null) {
                        ViewGroup rootView = act.findViewById(android.R.id.content);
                        if (rootView == null) rootView = (ViewGroup) act.getWindow().getDecorView();
                        boolean isDark = (act.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                        int overlayColor = isDark ? Color.parseColor("#450F172A") : Color.parseColor("#45154B87");
                        com.messenger.prime.BlurViewKt.setupBlur(blurTextSelectionMenu, rootView, 18f, overlayColor, act.getWindow().getDecorView().getBackground());
                    }
                }

                int menuWidth = (int) (230 * context.getResources().getDisplayMetrics().density);
                int menuHeight = (int) (90 * context.getResources().getDisplayMetrics().density);

                PopupWindow popupWindow = new PopupWindow(
                        menuView,
                        menuWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        false
                );

                popupWindow.setElevation(16f);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setTouchable(true);
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                activePopupWindow = popupWindow;

                popupWindow.setOnDismissListener(() -> {
                    if (!isInternalDismiss) {
                        activePopupWindow = null;
                        if (activeMode != null) {
                            try { activeMode.finish(); } catch (Exception ignored) {}
                        }
                    }
                });

                View btnReply = menuView.findViewById(R.id.btnSelectionReply);
                View btnCopy = menuView.findViewById(R.id.btnSelectionCopy);

                if (btnReply != null) {
                    btnReply.setVisibility(View.VISIBLE);
                    btnReply.setOnClickListener(v -> {
                        String selectedText = getSelectedText(textView);
                        isInternalDismiss = true;
                        if (activePopupWindow != null) {
                            try { activePopupWindow.dismiss(); } catch (Exception ignored) {}
                            activePopupWindow = null;
                        }
                        if (activeMode != null) {
                            try { activeMode.finish(); } catch (Exception ignored) {}
                        }
                        isInternalDismiss = false;
                        if (!selectedText.isEmpty() && actionListener != null) {
                            actionListener.onReplyToSelectedText(message, selectedText, position);
                        }
                    });
                }

                if (btnCopy != null) {
                    btnCopy.setOnClickListener(v -> {
                        String selectedText = getSelectedText(textView);
                        isInternalDismiss = true;
                        if (activePopupWindow != null) {
                            try { activePopupWindow.dismiss(); } catch (Exception ignored) {}
                            activePopupWindow = null;
                        }
                        if (activeMode != null) {
                            try { activeMode.finish(); } catch (Exception ignored) {}
                        }
                        isInternalDismiss = false;
                        if (!selectedText.isEmpty()) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (clipboard != null) {
                                ClipData clip = ClipData.newPlainText("selected_text", selectedText);
                                clipboard.setPrimaryClip(clip);
                                Activity act = getActivityFromContext(context);
                                if (act != null) {
                                    PrimeNotification.INSTANCE.show(act, "Текст скопирован", null);
                                }
                            }
                        }
                    });
                }

                int[] location = new int[2];
                textView.getLocationOnScreen(location);

                int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

                float selectX = location[0] + textView.getWidth() / 2f;
                float selectY = location[1];

                android.text.Layout tvLayout = textView.getLayout();
                if (tvLayout != null) {
                    int selStart = textView.getSelectionStart();
                    if (selStart >= 0 && selStart <= textView.getText().length()) {
                        int line = tvLayout.getLineForOffset(Math.min(selStart, textView.getText().length()));
                        float lineTop = tvLayout.getLineTop(line);
                        float horizontal = tvLayout.getPrimaryHorizontal(Math.min(selStart, textView.getText().length()));

                        selectX = location[0] + textView.getCompoundPaddingLeft() + horizontal;
                        selectY = location[1] + textView.getCompoundPaddingTop() + lineTop;
                    }
                }

                int x = (int) (selectX - menuWidth / 2f);
                x = Math.max(16, Math.min(x, screenWidth - menuWidth - 16));

                boolean showAbove = selectY > (screenHeight * 0.45f);
                int y;
                if (showAbove) {
                    y = (int) (selectY - menuHeight - (10 * context.getResources().getDisplayMetrics().density));
                    if (y < (60 * context.getResources().getDisplayMetrics().density)) {
                        y = (int) (selectY + (32 * context.getResources().getDisplayMetrics().density));
                    }
                } else {
                    y = (int) (selectY + (32 * context.getResources().getDisplayMetrics().density));
                }

                if (textView.isAttachedToWindow()) {
                    try {
                        popupWindow.showAtLocation(textView, Gravity.NO_GRAVITY, x, Math.max(80, y));
                    } catch (Exception ignored) {}
                }

                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                activeMode = null;
                if (activePopupWindow != null && activePopupWindow.isShowing()) {
                    isInternalDismiss = true;
                    try { activePopupWindow.dismiss(); } catch (Exception ignored) {}
                    activePopupWindow = null;
                    isInternalDismiss = false;
                }
                try {
                    CharSequence cs = textView.getText();
                    if (cs instanceof android.text.Spannable) {
                        android.text.Selection.removeSelection((android.text.Spannable) cs);
                    }
                } catch (Exception ignored) {}
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
                            start = fullText.toLowerCase(Locale.getDefault()).indexOf(quotedText.toLowerCase(Locale.getDefault()));
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

    private void bindDateHeader(View itemView, TextView tvDateHeader, boolean showDateHeader, long timestamp) {
        View layoutDateHeader = itemView.findViewById(R.id.layoutDateHeader);
        View vDateHeaderLine = itemView.findViewById(R.id.vDateHeaderLine);

        if (showDateHeader && timestamp > 0) {
            if (layoutDateHeader != null) layoutDateHeader.setVisibility(View.VISIBLE);
            else if (tvDateHeader != null) tvDateHeader.setVisibility(View.VISIBLE);

            String dateStr = getDateHeaderString(timestamp);
            if (tvDateHeader != null) {
                tvDateHeader.setText(dateStr);
                int accentColor = ColorAccentManager.getCurrentAccentColor(itemView.getContext());
                tvDateHeader.setTextColor(accentColor);
                if (vDateHeaderLine != null) {
                    vDateHeaderLine.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                }
            }
        } else {
            if (layoutDateHeader != null) layoutDateHeader.setVisibility(View.GONE);
            else if (tvDateHeader != null) tvDateHeader.setVisibility(View.GONE);
        }
    }

    public void showExpandedReactionsPopup(View anchorView, ChatMessage message, int position) {
        if (anchorView == null || anchorView.getContext() == null) return;
        Context context = anchorView.getContext();

        View popupView = LayoutInflater.from(context).inflate(R.layout.layout_reactions_popup, null);

        BlurView blurReactionsPopup = popupView.findViewById(R.id.blurReactionsPopup);
        if (blurReactionsPopup != null) {
            Activity act = getActivityFromContext(context);
            if (act != null) {
                ViewGroup rootView = act.findViewById(android.R.id.content);
                if (rootView == null) rootView = (ViewGroup) act.getWindow().getDecorView();
                boolean isDark = (act.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int overlayColor = isDark ? Color.parseColor("#450F172A") : Color.parseColor("#45154B87");
                com.messenger.prime.BlurViewKt.setupBlur(blurReactionsPopup, rootView, 18f, overlayColor, act.getWindow().getDecorView().getBackground());
            }
        }

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setElevation(16f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        GridLayout flexGrid = popupView.findViewById(R.id.gridReactionsEmoji);
        String[] emojiList = new String[]{
                "❤️", "🔥", "👍", "👎", "😂", "😮", "😢", "🙏", "👏", "🎉",
                "💯", "🥰", "🤩", "🥳", "🤡", "💩", "😡", "🤯", "😱", "🥺",
                "⚡", "🌟", "😴", "🤮", "🤝", "🤖", "🎃", "👑", "🎯", "🚀",
                "💔", "👀", "🙈", "😜", "😎", "🤔", "🤫", "🙄", "🫠", "💀"
        };

        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (38 * density);

        if (flexGrid != null) {
            flexGrid.removeAllViews();
        }

        for (int i = 0; i < emojiList.length; i++) {
            String emoji = emojiList[i];
            TextView tv = new TextView(context);
            tv.setText(emoji);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            tv.setLayoutParams(lp);
            tv.setBackgroundResource(R.drawable.bg_emoji_chip);
            tv.setFocusable(true);
            tv.setClickable(true);

            tv.setAlpha(0f);
            tv.setScaleX(0.2f);
            tv.setScaleY(0.2f);

            tv.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                v.animate().scaleX(1.4f).scaleY(1.4f).setDuration(120L).start();
                popupView.animate()
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .alpha(0f)
                        .setDuration(160L)
                        .withEndAction(popupWindow::dismiss)
                        .start();

                if (actionListener != null) {
                    actionListener.onQuickReaction(message, emoji, position);
                }
            });

            if (flexGrid != null) {
                flexGrid.addView(tv);
            }

            long delay = 40L + (i / 6) * 30L + (i % 6) * 12L;
            tv.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setStartDelay(delay)
                    .setDuration(220L)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int x = location[0] + (anchorView.getWidth() / 2) - (popupWidth / 2);
        int y = location[1] - popupHeight - (int) (8 * density);

        if (y < 100) {
            y = location[1] + anchorView.getHeight() + (int) (8 * density);
        }

        popupView.setPivotX(popupWidth / 2f);
        popupView.setPivotY(y < location[1] ? (float) popupHeight : 0f);
        popupView.setAlpha(0f);
        popupView.setScaleX(0.35f);
        popupView.setScaleY(0.35f);

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);

        popupView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(280L)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
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
                if (isMultiSelectionMode) {
                    toggleMessageSelection(message);
                    return;
                }
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

        boolean isLocal = author == null || author.isEmpty() ||
                AvatarManager.isLocalUserIdentity(context, author);

        if (isLocal) {
            String myAvatar = AvatarManager.getMyAvatarUri(context);
            if (myAvatar != null && !myAvatar.isEmpty()) {
                Bitmap bmp = loadBitmapFromUriString(context, myAvatar);
                if (bmp != null) return bmp;
            }
        } else {
            String contactAvatar = AvatarManager.getContactAvatarUriOrFile(context, author, author, null);
            if (contactAvatar != null && !contactAvatar.isEmpty()) {
                Bitmap bmp = loadBitmapFromUriString(context, contactAvatar);
                if (bmp != null) return bmp;
            }
        }

        return null;
    }

    private Bitmap loadBitmapFromUriString(Context context, String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) return null;
        try {
            Uri uri;
            if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("http")) {
                uri = Uri.parse(uriStr);
            } else {
                uri = Uri.fromFile(new File(uriStr));
            }

            if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                File f = new File(uri.getPath());
                if (f.exists()) {
                    return BitmapFactory.decodeFile(f.getAbsolutePath());
                }
            } else {
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        return BitmapFactory.decodeStream(is);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
