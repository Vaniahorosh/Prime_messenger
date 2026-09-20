package com.messenger.prime;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    }

    private List<ChatMessage> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;
    private OnMessageActionListener actionListener;
    private String localUsername = "";
    
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
        if (itemView == null) {
            if (position >= 0 && position < messages.size()) {
                messages.remove(position);
                notifyItemRemoved(position);
            }
            if (onComplete != null) onComplete.run();
            return;
        }

        itemView.animate()
                .alpha(0f)
                .scaleX(0.1f)
                .scaleY(0.1f)
                .translationY(-40f)
                .setDuration(260)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    itemView.setAlpha(1f);
                    itemView.setScaleX(1f);
                    itemView.setScaleY(1f);
                    itemView.setTranslationY(0f);
                    if (position >= 0 && position < messages.size()) {
                        messages.remove(position);
                        notifyItemRemoved(position);
                    }
                    if (onComplete != null) onComplete.run();
                })
                .start();
    }

    public void deleteMessageByIdAnimated(RecyclerView recyclerView, String messageId, Runnable onComplete) {
        if (messageId == null || messages.isEmpty()) {
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
                } else if (m.getImagePath() != null || m.getImageBitmap() != null) {
                    foundPos = i;
                    break;
                }
            }
        }

        if (foundPos != -1) {
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
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                messages.get(i).setReaction(reaction);
                if (authorLogin != null) {
                    messages.get(i).setReactionSenderLogin(authorLogin);
                }
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
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
    }

    public long getMessageTimestamp(int position) {
        if (position >= 0 && position < messages.size()) {
            return messages.get(position).getTimestamp();
        }
        return 0;
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

    public static void showFullScreenPhoto(Context context, View view, Bitmap bitmap, String path) {
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
            Intent intent = new Intent(context, PhotoViewActivity.class);
            intent.putExtra("EXTRA_URI", imageUri.toString());
            if (view != null) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
                intent.putExtra("EXTRA_RECT", rect);
            }
            context.startActivity(intent);
        }
    }

    public static void showFullScreenPhoto(Context context, Bitmap bitmap, String path) {
        showFullScreenPhoto(context, null, bitmap, path);
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
        }

        void bind(ChatMessage message, boolean showDateHeader, OnMessageLongClickListener listener, int position) {
            if (showDateHeader && message.getTimestamp() > 0) {
                tvDateHeader.setVisibility(View.VISIBLE);
                tvDateHeader.setText(getDateHeaderString(message.getTimestamp()));
            } else {
                tvDateHeader.setVisibility(View.GONE);
            }

            Bitmap bmp = message.getImageBitmap();
            if (bmp == null && message.getImagePath() != null && !message.getImagePath().isEmpty()) {
                try {
                    bmp = BitmapFactory.decodeFile(message.getImagePath());
                    message.setImageBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                ivMessageImage.setVisibility(View.VISIBLE);
                ivMessageImage.setImageBitmap(bmp);
                Bitmap finalBmp = bmp;
                ivMessageImage.setOnClickListener(v -> 
                    showFullScreenPhoto(v.getContext(), v, finalBmp, message.getImagePath())
                );
                ivMessageImage.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageLongClick(message, position);
                        return true;
                    }
                    return false;
                });
                
                String text = message.getText();
                if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text)) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            } else {
                ivMessageImage.setVisibility(View.GONE);
                String text = message.getText();
                if (text == null || text.isEmpty()) {
                    text = (message.getImagePath() != null && !message.getImagePath().isEmpty()) ? "Фотография" : "";
                }
                if (!text.isEmpty()) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            if (layoutMessageReaction != null && tvMessageReaction != null) {
                String reaction = message.getReaction();
                if (reaction != null && !reaction.trim().isEmpty()) {
                    layoutMessageReaction.setVisibility(View.VISIBLE);
                    tvMessageReaction.setText(reaction);
                    
                    if (ivReactionAvatar != null) {
                        String rxAuthor = message.getReactionSenderLogin();
                        String resolvedName = (rxAuthor != null && !rxAuthor.isEmpty() && !rxAuthor.equalsIgnoreCase("my") && !rxAuthor.equalsIgnoreCase("me")) ? rxAuthor : localUsername;
                        
                        try {
                            File localAvatarFile = new File(itemView.getContext().getFilesDir(), "avatar_" + resolvedName + ".jpg");
                            if (localAvatarFile.exists()) {
                                Bitmap rxBmp = BitmapFactory.decodeFile(localAvatarFile.getAbsolutePath());
                                ivReactionAvatar.setImageBitmap(rxBmp);
                            } else {
                                ivReactionAvatar.setImageResource(R.drawable.ic_person); // Fallback
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    layoutMessageReaction.setOnClickListener(v -> {
                        String author = message.getReactionSenderLogin();
                        if (author != null && (author.equalsIgnoreCase("my") || author.equalsIgnoreCase("me") || author.equalsIgnoreCase(localUsername))) {
                            if (actionListener != null) {
                                actionListener.onQuickReaction(message, null, position);
                            }
                        } else {
                            if (listener != null) listener.onMessageLongClick(message, position);
                        }
                    });
                } else {
                    layoutMessageReaction.setVisibility(View.GONE);
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
            }

            itemView.setOnLongClickListener(v -> {
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
        ImageButton btnEditMsgAction;
        ImageButton btnDeleteMsgAction;

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
            btnEditMsgAction = itemView.findViewById(R.id.btnEditMsgAction);
            btnDeleteMsgAction = itemView.findViewById(R.id.btnDeleteMsgAction);
        }

        void bind(ChatMessage message, boolean showDateHeader, OnMessageLongClickListener listener, int position) {
            if (showDateHeader && message.getTimestamp() > 0) {
                tvDateHeader.setVisibility(View.VISIBLE);
                tvDateHeader.setText(getDateHeaderString(message.getTimestamp()));
            } else {
                tvDateHeader.setVisibility(View.GONE);
            }

            Bitmap bmp = message.getImageBitmap();
            if (bmp == null && message.getImagePath() != null && !message.getImagePath().isEmpty()) {
                try {
                    bmp = BitmapFactory.decodeFile(message.getImagePath());
                    message.setImageBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                ivMessageImage.setVisibility(View.VISIBLE);
                ivMessageImage.setImageBitmap(bmp);
                Bitmap finalBmp = bmp;
                ivMessageImage.setOnClickListener(v -> 
                    showFullScreenPhoto(v.getContext(), v, finalBmp, message.getImagePath())
                );
                ivMessageImage.setOnLongClickListener(v -> {
                    itemView.performLongClick();
                    return true;
                });
                
                String text = message.getText();
                if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text) && !"Фотография".equals(text)) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            } else {
                ivMessageImage.setVisibility(View.GONE);
                String text = message.getText();
                if (text == null || text.isEmpty()) {
                    text = (message.getImagePath() != null && !message.getImagePath().isEmpty()) ? "Фотография" : "";
                }
                if (!text.isEmpty()) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            if (layoutMessageReaction != null && tvMessageReaction != null) {
                String reaction = message.getReaction();
                if (reaction != null && !reaction.trim().isEmpty()) {
                    layoutMessageReaction.setVisibility(View.VISIBLE);
                    tvMessageReaction.setText(reaction);
                    
                    if (ivReactionAvatar != null) {
                        String rxAuthor = message.getReactionSenderLogin();
                        String resolvedName = (rxAuthor != null && !rxAuthor.isEmpty() && !rxAuthor.equalsIgnoreCase("my") && !rxAuthor.equalsIgnoreCase("me")) ? rxAuthor : localUsername;
                        
                        try {
                            File localAvatarFile = new File(itemView.getContext().getFilesDir(), "avatar_" + resolvedName + ".jpg");
                            if (localAvatarFile.exists()) {
                                Bitmap rxBmp = BitmapFactory.decodeFile(localAvatarFile.getAbsolutePath());
                                ivReactionAvatar.setImageBitmap(rxBmp);
                            } else {
                                ivReactionAvatar.setImageResource(R.drawable.ic_person);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    layoutMessageReaction.setOnClickListener(v -> {
                        String author = message.getReactionSenderLogin();
                        if (author != null && (author.equalsIgnoreCase("my") || author.equalsIgnoreCase("me") || author.equalsIgnoreCase(localUsername))) {
                            if (actionListener != null) {
                                actionListener.onQuickReaction(message, null, position);
                            }
                        } else {
                            if (listener != null) listener.onMessageLongClick(message, position);
                        }
                    });
                } else {
                    layoutMessageReaction.setVisibility(View.GONE);
                }
            }

            if (ivMessageStatus != null) {
                if (message.getMessageStatus() == MessageStatus.READ) {
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
}
