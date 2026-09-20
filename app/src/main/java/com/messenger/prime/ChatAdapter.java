package com.messenger.prime;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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

    private List<ChatMessage> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages = new ArrayList<>(newMessages);
        notifyDataSetChanged();
    }

    public void updateMessageById(String messageId, String newText) {
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
        setAnimation(holder.itemView);
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

    private static void showFullScreenPhoto(Context context, Bitmap bitmap, String path) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else if (path != null && !path.isEmpty()) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        }
        
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(imageView);
        dialog.show();
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        TextView tvMessageText;
        TextView tvMessageTime;
        ImageView ivMessageImage;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
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
                    showFullScreenPhoto(v.getContext(), finalBmp, message.getImagePath())
                );
                
                String text = message.getText();
                if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text)) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            } else {
                ivMessageImage.setVisibility(View.GONE);
                tvMessageText.setVisibility(View.VISIBLE);
                tvMessageText.setText(message.getText());
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                    return true;
                }
                return false;
            });
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        TextView tvMessageText;
        TextView tvMessageTime;
        ImageView ivMessageImage;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
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
                    showFullScreenPhoto(v.getContext(), finalBmp, message.getImagePath())
                );
                
                String text = message.getText();
                if (text != null && !text.isEmpty() && !"Фото".equals(text) && !"Фото отправлено".equals(text)) {
                    tvMessageText.setVisibility(View.VISIBLE);
                    tvMessageText.setText(text);
                } else {
                    tvMessageText.setVisibility(View.GONE);
                }
            } else {
                ivMessageImage.setVisibility(View.GONE);
                tvMessageText.setVisibility(View.VISIBLE);
                tvMessageText.setText(message.getText());
            }

            tvMessageTime.setText(formatTime(message.getTimestamp(), message.getTime(), message.isEdited()));

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                    return true;
                }
                return false;
            });
        }
    }
}
