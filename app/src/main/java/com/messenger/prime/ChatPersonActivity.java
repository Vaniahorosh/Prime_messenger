package com.messenger.prime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.OutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.widget.Button;
import android.widget.LinearLayout;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import kotlin.Unit;

public class ChatPersonActivity extends AppCompatActivity {

    private static final String TAG = "ChatPersonActivity";
    private static final UUID UUID_CHAT = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private static final int MESSAGE_READ = 1;
    private static final int MESSAGE_WRITE = 2;
    private static final int MESSAGE_TOAST = 3;
    private static final int HANDSHAKE_SUCCESS = 4;
    private static final int MESSAGE_READ_PHOTO = 5;
    private static final int MESSAGE_TYPING = 6;
    private static final int MESSAGE_EDIT_RECEIVED = 7;
    private static final int MESSAGE_READ_AVATAR = 8;
    private static final int MESSAGE_CHAT_DELETED = 9;
    private static final int MESSAGE_READ_RECEIPT = 10;
    private static final int MESSAGE_DISCONNECTED = 11;
    private static final int MESSAGE_DELETE_SINGLE = 12;
    private static final int MESSAGE_PRESENCE_UPDATED = 13;
    private static final int MESSAGE_REACTION_RECEIVED = 14;

    private static final byte TYPE_TEXT = 0x01;
    private static final byte TYPE_PHOTO = 0x02;
    private static final byte TYPE_PING = 0x03;
    private static final byte TYPE_TYPING = 0x04;
    private static final byte TYPE_EDIT = 0x05;
    private static final byte TYPE_ACK = 0x06;
    private static final byte TYPE_AVATAR = 0x07;
    private static final byte TYPE_CHAT_DELETED = 0x08;
    private static final byte TYPE_DISCONNECT = 0x09;
    private static final byte TYPE_READ_RECEIPT = 0x0A;
    private static final byte TYPE_DELETE_MSG = 0x0B;
    private static final byte TYPE_PRESENCE = 0x0C;
    private static final byte TYPE_REACTION = 0x0D;

    private static class PendingMessage {
        byte type;
        byte[] payload;

        PendingMessage(byte type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    private final Queue<PendingMessage> pendingMessageQueue = new ConcurrentLinkedQueue<>();

    private String targetUsername;
    private String deviceAddress;
    private boolean isServer;
    private boolean isOnlineExpected;
    private String localUsername;

    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private TextView tvChatStatus;
    private TextView tvChatName;
    private TextView tvFloatingDate;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ChatAdapter chatAdapter;
    private Handler handler;

    private View layoutConnectAction;
    private Button btnPrimeConnect;
    private View layoutInput;
    private ImageButton btnAttach;
    private ImageButton btnCancelEdit;
    private ImageButton btnCloseEditBar;
    private View layoutEditBar;

    private boolean isEditMode = false;
    private String editingMessageId = null;

    private boolean isHandshakeDone = false;
    private boolean isChatDeleted = false;
    private boolean isRemoteUserOnline = false;
    private boolean isOpeningSubActivity = false;
    private String currentActivityState = "IDLE";
    private String myLocalActivityState = "IDLE";
    private int remoteVersionCode = -1;

    public void setOpeningSubActivity(boolean openingSubActivity) {
        this.isOpeningSubActivity = openingSubActivity;
    }
    private Boolean pendingRoleAsServer = null;
    private String remoteAvatarUri = null;

    private long lastTypingSentTime = 0;
    private final Handler typingResetHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetTypingRunnable = () -> {
        currentActivityState = "IDLE";
        if (isRemoteUserOnline) {
            if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(ChatPersonActivity.this)) {
                setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
            } else {
                setStatusWithAnimation("В сети", R.color.prime_success);
            }
        } else {
            updateOfflineLastSeenStatus();
        }
    };

    private final Handler connectTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectedThread == null || !connectedThread.isAlive()) {
                if (btnPrimeConnect != null) {
                    btnPrimeConnect.setEnabled(true);
                    btnPrimeConnect.setText("⚡ Праймериться!");
                }
                updateOfflineLastSeenStatus();
            }
        }
    };

    private final Handler dateHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideDateRunnable = () -> {
        if (tvFloatingDate != null) {
            tvFloatingDate.animate().alpha(0f).setDuration(250).start();
        }
    };

    @SuppressLint("InlinedApi")
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean connectGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false));
                boolean scanGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false));
                
                if (connectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    if (pendingRoleAsServer != null) {
                        isServer = pendingRoleAsServer;
                        pendingRoleAsServer = null;
                        startPrimeConnection();
                    }
                } else {
                    Toast.makeText(this, "Требуется разрешение Bluetooth для подключения", Toast.LENGTH_SHORT).show();
                }
            });

    public void sendActivityState(String stateStr) {
        if (stateStr != null) myLocalActivityState = stateStr;
        if (connectedThread != null && connectedThread.isAlive()) {
            connectedThread.sendPacket(TYPE_TYPING, ("STATE:" + stateStr).getBytes(StandardCharsets.UTF_8));
        }
    }

    private final ActivityResultLauncher<Intent> editPhotoLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                isOpeningSubActivity = false;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String editedUriStr = result.getData().getStringExtra("EDITED_IMAGE_URI");
                    if (editedUriStr != null && !editedUriStr.isEmpty()) {
                        sendPhoto(Uri.parse(editedUriStr));
                    }
                }
            });

    private final ActivityResultLauncher<String> pickPhotoLauncher = 
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    isOpeningSubActivity = true;
                    Intent intent = new Intent(ChatPersonActivity.this, PhotoEditorActivity.class);
                    intent.putExtra("EXTRA_IMAGE_URI", uri.toString());
                    editPhotoLauncher.launch(intent);
                } else {
                    isOpeningSubActivity = false;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        boolean isDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDarkTheme);
            controller.setAppearanceLightNavigationBars(!isDarkTheme);
        }

        setContentView(R.layout.activity_chat_person_content);

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            );
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            );
        }

        SlidrConfig slidrConfig = new SlidrConfig.Builder()
                .position(SlidrPosition.LEFT)
                .build();
        Slidr.attach(this, slidrConfig);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    exitEditMode();
                } else {
                    finish();
                }
            }
        });

        View chatRoot = findViewById(R.id.chatRoot);
        if (chatRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                float density = getResources().getDisplayMetrics().density;
                int baseMargin = (int) (12 * density);

                View vTopGradient = findViewById(R.id.vTopGradient);
                if (vTopGradient != null) {
                    ViewGroup.LayoutParams lp = vTopGradient.getLayoutParams();
                    if (lp != null) {
                        lp.height = systemBars.top + (int) (36 * density);
                        vTopGradient.setLayoutParams(lp);
                    }
                    vTopGradient.setBackgroundResource(isDarkTheme ? R.drawable.bg_top_fog_dark : R.drawable.bg_top_fog_light);
                }

                View vBottomGradient = findViewById(R.id.vBottomGradient);
                if (vBottomGradient != null) {
                    ViewGroup.LayoutParams lp = vBottomGradient.getLayoutParams();
                    if (lp != null) {
                        lp.height = systemBars.bottom + (int) (36 * density);
                        vBottomGradient.setLayoutParams(lp);
                    }
                    vBottomGradient.setBackgroundResource(isDarkTheme ? R.drawable.bg_bottom_fog_dark : R.drawable.bg_bottom_fog_light);
                }

                View layoutHeader = findViewById(R.id.layoutHeader);
                if (layoutHeader != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutHeader.getLayoutParams();
                    if (lp != null) {
                        lp.topMargin = systemBars.top + baseMargin;
                        layoutHeader.setLayoutParams(lp);
                    }
                }

                View bottomContainer = findViewById(R.id.bottomContainer);
                if (bottomContainer != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomContainer.getLayoutParams();
                    if (lp != null) {
                        lp.bottomMargin = Math.max(systemBars.bottom, ime.bottom) + baseMargin;
                        bottomContainer.setLayoutParams(lp);
                    }
                }

                View tvFloatingDate = findViewById(R.id.tvFloatingDate);
                if (tvFloatingDate != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tvFloatingDate.getLayoutParams();
                    if (lp != null) {
                        lp.topMargin = systemBars.top + (int) (80 * density);
                        tvFloatingDate.setLayoutParams(lp);
                    }
                }

                updateMessageListPadding();

                return insets;
            });
        }

        tvChatName = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        tvFloatingDate = findViewById(R.id.tvFloatingDate);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView ivChatAvatar = findViewById(R.id.ivChatAvatar);
        
        layoutConnectAction = findViewById(R.id.layoutConnectAction);
        btnPrimeConnect = findViewById(R.id.btnPrimeConnect);
        layoutInput = findViewById(R.id.layoutInput);
        btnAttach = findViewById(R.id.btnAttach);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnCloseEditBar = findViewById(R.id.btnCloseEditBar);
        layoutEditBar = findViewById(R.id.layoutEditBar);

        if (btnCancelEdit != null) {
            btnCancelEdit.setOnClickListener(v -> exitEditMode());
        }
        if (btnCloseEditBar != null) {
            btnCloseEditBar.setOnClickListener(v -> exitEditMode());
        }

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (ivChatAvatar != null) {
            ivChatAvatar.setOnClickListener(v -> showFullScreenAvatar());
            ivChatAvatar.setOnLongClickListener(v -> {
                showAvatarActionTray();
                return true;
            });
        }

        // Получение данных из Intent
        Intent intent = getIntent();
        targetUsername = intent.getStringExtra("EXTRA_CHAT_NAME");
        deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");
        isServer = intent.getBooleanExtra("EXTRA_IS_SERVER", false);
        isOnlineExpected = intent.getBooleanExtra("EXTRA_IS_ONLINE", false);
        String avatarUri = intent.getStringExtra("EXTRA_AVATAR_URI");
        if (avatarUri != null && !avatarUri.isEmpty()) {
            remoteAvatarUri = avatarUri;
        }
        if (targetUsername == null || targetUsername.isEmpty()) {
            targetUsername = "Собеседник";
        }
        tvChatName.setText(targetUsername);

        try {
            SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPreferences.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (targetUsername != null && (targetUsername.equalsIgnoreCase(obj.optString("name")) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(obj.optString("id"))))) {
                    String st = obj.optString("onlineStatus", "OFFLINE");
                    isRemoteUserOnline = "ONLINE".equalsIgnoreCase(st);
                    break;
                }
            }
        } catch (Exception ignored) {}

        if (isRemoteUserOnline) {
            if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
            } else {
                setStatusWithAnimation("В сети", R.color.prime_success);
            }
        } else {
            updateOfflineLastSeenStatus();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String currentUser = sharedPreferences.getString("current_user", "");
        String myDisplayName = sharedPreferences.getString(currentUser + "_name", currentUser);
        localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";

        updateAvatarUi(avatarUri, targetUsername);

        // Настройка списка
        chatAdapter = new ChatAdapter();
        chatAdapter.setLocalUsername(localUsername);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && tvFloatingDate != null) {
                    int firstPos = lm.findFirstVisibleItemPosition();
                    if (firstPos != RecyclerView.NO_POSITION && firstPos < chatAdapter.getItemCount()) {
                        long ts = chatAdapter.getMessageTimestamp(firstPos);
                        if (ts > 0) {
                            String dateStr = ChatAdapter.getDateHeaderString(ts);
                            tvFloatingDate.setText(dateStr);
                            if (tvFloatingDate.getAlpha() < 1f) {
                                tvFloatingDate.animate().alpha(1f).setDuration(150).start();
                            }
                            dateHideHandler.removeCallbacks(hideDateRunnable);
                            dateHideHandler.postDelayed(hideDateRunnable, 1800);
                        }
                    }
                }
            }
        });

        // Загружаем сохраненную историю
        List<ChatMessage> history = ChatHistoryManager.loadMessages(this, targetUsername);
        if (!history.isEmpty()) {
            chatAdapter.setMessages(history);
            rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
        }

        chatAdapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onEditMessage(ChatMessage message, int position) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Действие недоступно без подключения", null);
                    return;
                }
                showEditMessageDialog(message, position);
            }

            @Override
            public void onDeleteMessage(ChatMessage message, int position) {
                if (message == null) return;
                
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Действие недоступно без подключения", null);
                    return;
                }
                
                if (connectedThread != null && connectedThread.isAlive()) {
                    connectedThread.sendPacket(TYPE_DELETE_MSG, message.getMessageId().getBytes(StandardCharsets.UTF_8));
                }

                RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(position);
                View viewToAnimate = holder != null ? holder.itemView : null;
                
                chatAdapter.deleteMessageAnimated(viewToAnimate, position, () -> {
                    ChatHistoryManager.deleteSingleMessage(ChatPersonActivity.this, targetUsername, message.getMessageId());
                    ChatMessage newLast = chatAdapter.getLastMessage();
                    if (newLast != null) {
                        String text = newLast.getText();
                        if (text == null || text.isEmpty()) {
                            text = (newLast.getImagePath() != null && !newLast.getImagePath().isEmpty()) ? "Фотография" : "";
                        }
                        saveLastMessageToChatList(text);
                    } else {
                        saveLastMessageToChatList(""); // Clear last message
                    }
                });
            }

            @Override
            public void onQuickReaction(ChatMessage message, String reaction, int position) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Действие недоступно без подключения", null);
                    return;
                }
                applyAndSendReaction(message, reaction);
            }
        });

        // Handler для связи потоков с UI
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        String data = new String(readBuf, 0, msg.arg1, StandardCharsets.UTF_8);
                        String msgId = null;
                        String realText = data;
                        int sep = data.indexOf(":::");
                        if (sep != -1) {
                            msgId = data.substring(0, sep);
                            realText = data.substring(sep + 3);
                        }
                        long timestamp = System.currentTimeMillis();
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                        ChatMessage incomingMessage = new ChatMessage(realText, time, targetUsername, false, null, timestamp, null, msgId);
                        addMessageToUI(incomingMessage);
                        break;
                    case MESSAGE_READ_PHOTO:
                        byte[] fullPayload = (byte[]) msg.obj;
                        if (fullPayload != null && fullPayload.length > 0) {
                            String photoMsgId = null;
                            byte[] photoBytes = fullPayload;
                            
                            int sepIdx = -1;
                            for (int i = 0; i < Math.min(fullPayload.length, 120); i++) {
                                if (fullPayload[i] == ':' && i + 2 < fullPayload.length && fullPayload[i+1] == ':' && fullPayload[i+2] == ':') {
                                    sepIdx = i;
                                    break;
                                }
                            }
                            if (sepIdx != -1) {
                                photoMsgId = new String(fullPayload, 0, sepIdx, StandardCharsets.UTF_8);
                                photoBytes = new byte[fullPayload.length - (sepIdx + 3)];
                                System.arraycopy(fullPayload, sepIdx + 3, photoBytes, 0, photoBytes.length);
                            }

                            Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                            long photoTs = System.currentTimeMillis();
                            String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                            ChatMessage photoMsg = new ChatMessage(null, photoTime, targetUsername, false, photoBitmap, photoTs, null, photoMsgId);
                            addMessageToUI(photoMsg);
                        }
                        break;
                    case MESSAGE_READ_AVATAR:
                        byte[] avatarBuf = (byte[]) msg.obj;
                        try {
                            Bitmap avatarBmp = BitmapFactory.decodeByteArray(avatarBuf, 0, avatarBuf.length);
                            if (avatarBmp != null) {
                                File avatarFile = new File(getFilesDir(), "avatar_" + targetUsername + ".jpg");
                                FileOutputStream fos = new FileOutputStream(avatarFile);
                                avatarBmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                                fos.flush();
                                fos.close();
                                
                                remoteAvatarUri = Uri.fromFile(avatarFile).toString();
                                
                                ImageView ivAvatar = findViewById(R.id.ivChatAvatar);
                                if (ivAvatar != null) {
                                    ivAvatar.setImageBitmap(avatarBmp);
                                }
                                saveLastMessageToChatList(null);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to receive avatar", e);
                        }
                        break;
                    case MESSAGE_WRITE:
                        break;
                    case MESSAGE_TOAST:
                        String toastStr = msg.getData().getString("toast");
                        if (toastStr != null) {
                            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, toastStr, null);
                        }
                        break;
                    case HANDSHAKE_SUCCESS:
                        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
                        isRemoteUserOnline = true;
                        saveLastSeenTimestamp();
                        CharSequence curStatus = tvChatStatus != null ? tvChatStatus.getText() : "";
                        if (curStatus == null || (!curStatus.toString().contains("Печатает") && !curStatus.toString().contains("фото"))) {
                            if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(ChatPersonActivity.this)) {
                                setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
                            } else {
                                setStatusWithAnimation("В сети", R.color.prime_success);
                            }
                        }
                        saveLastMessageToChatList(null, null, false, "ONLINE");
                        if (!"IDLE".equals(myLocalActivityState)) {
                            sendActivityState(myLocalActivityState);
                        }
                        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                        if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                        sendLocalAvatar();
                        flushPendingMessages();
                        break;
                    case MESSAGE_TYPING:
                        String stateStr = (msg.obj instanceof String) ? (String) msg.obj : null;
                        if (stateStr != null && stateStr.startsWith("STATE:")) {
                            String action = stateStr.substring(6);
                            currentActivityState = action;

                            if ("VIEWING_PHOTO".equalsIgnoreCase(action)) {
                                setStatusWithAnimation("Смотрит фото", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "VIEWING_PHOTO");
                            } else if ("SENDING_PHOTO".equalsIgnoreCase(action)) {
                                setStatusWithAnimation("Отправка фото...", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "SENDING_PHOTO");
                            } else if ("IDLE".equalsIgnoreCase(action)) {
                                currentActivityState = "IDLE";
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "IDLE");
                                resetTypingRunnable.run();
                            } else if ("TYPING".equalsIgnoreCase(action)) {
                                if (!"VIEWING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_PHOTO".equalsIgnoreCase(currentActivityState)) {
                                    setStatusWithAnimation("Печатает...", R.color.prime_success);
                                    typingResetHandler.removeCallbacks(resetTypingRunnable);
                                    typingResetHandler.postDelayed(resetTypingRunnable, 3000);
                                    saveActivityStateToChatList(targetUsername, "TYPING");
                                }
                            }
                        } else {
                            if (!"VIEWING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_PHOTO".equalsIgnoreCase(currentActivityState)) {
                                setStatusWithAnimation("Печатает...", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 3000);
                                saveActivityStateToChatList(targetUsername, "TYPING");
                            }
                        }
                        break;
                    case MESSAGE_EDIT_RECEIVED:
                        String editData = (String) msg.obj;
                        int editSep = editData.indexOf(":::");
                        if (editSep != -1) {
                            try {
                                String editMsgId = editData.substring(0, editSep);
                                String updatedText = editData.substring(editSep + 3);
                                chatAdapter.updateMessageById(editMsgId, updatedText);
                                ChatMessage newLast = chatAdapter.getLastMessage();
                                if (newLast != null && editMsgId.equals(newLast.getMessageId())) {
                                    saveLastMessageToChatList(updatedText);
                                }
                                
                                List<ChatMessage> history = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
                                for (ChatMessage m : history) {
                                    if (editMsgId.equals(m.getMessageId())) {
                                        m.setEdited(true);
                                        m.setText(updatedText);
                                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, m);
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse edit message", e);
                            }
                        }
                        break;
                    case MESSAGE_CHAT_DELETED:
                        isChatDeleted = true;
                        String deletedByName = (msg.obj instanceof String) ? (String) msg.obj : targetUsername;
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, deletedByName + " полностью удалил(а) переписку!", null);
                        ChatHistoryManager.deleteHistory(ChatPersonActivity.this, deletedByName);
                        ChatHistoryManager.deleteHistory(ChatPersonActivity.this, targetUsername);
                        deleteChatFromChatListEx(deletedByName);
                        deleteChatFromChatListEx(targetUsername);
                        new Handler(Looper.getMainLooper()).postDelayed(ChatPersonActivity.this::finish, 1200L);
                        break;
                    case MESSAGE_READ_RECEIPT:
                        byte[] receiptBuf = (byte[]) msg.obj;
                        if (receiptBuf != null && msg.arg1 > 0) {
                            String receiptData = new String(receiptBuf, 0, msg.arg1, StandardCharsets.UTF_8);
                            String readMsgId = receiptData;
                            int sepIdx = receiptData.indexOf(":::");
                            if (sepIdx != -1) {
                                readMsgId = receiptData.substring(0, sepIdx);
                            }
                            chatAdapter.updateMessageStatusById(readMsgId, MessageStatus.READ);
                            
                            List<ChatMessage> history = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
                            for (ChatMessage m : history) {
                                if (readMsgId.equals(m.getMessageId())) {
                                    m.setMessageStatus(MessageStatus.READ);
                                    ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, m);
                                    break;
                                }
                            }
                        }
                        saveLastMessageToChatList(null, MessageStatus.READ, false, "ONLINE");
                        break;
                    case MESSAGE_DISCONNECTED:
                        isRemoteUserOnline = false;
                        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
                        updateOfflineLastSeenStatus();
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, targetUsername + " отключил(а) соединение", null);
                        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
                        if (layoutInput != null) layoutInput.setVisibility(View.GONE);
                        if (btnPrimeConnect != null) {
                            btnPrimeConnect.setEnabled(true);
                            btnPrimeConnect.setText("⚡ Праймериться!");
                        }
                        break;
                    case MESSAGE_PRESENCE_UPDATED:
                        String pData = (String) msg.obj;
                        if (pData != null) {
                            if (pData.startsWith("ONLINE")) {
                                isRemoteUserOnline = true;
                                saveLastSeenTimestamp();
                                if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(ChatPersonActivity.this)) {
                                    setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
                                } else {
                                    setStatusWithAnimation("В сети", R.color.prime_success);
                                }
                                saveLastMessageToChatList(null, null, false, "ONLINE");
                            } else if (pData.startsWith("OFFLINE:")) {
                                isRemoteUserOnline = false;
                                try {
                                    long ts = Long.parseLong(pData.substring(8));
                                    saveCustomLastSeenTimestamp(ts);
                                    updateOfflineLastSeenStatus(ts);
                                } catch (Exception e) {
                                    updateOfflineLastSeenStatus();
                                }
                                saveLastMessageToChatList(null, null, false, "OFFLINE");
                            }
                        }
                        break;
                    case MESSAGE_DELETE_SINGLE:
                        String deletedMsgId = (String) msg.obj;
                        if (deletedMsgId != null && chatAdapter != null) {
                            chatAdapter.deleteMessageByIdAnimated(rvMessages, deletedMsgId, () -> {
                                ChatMessage newLast = chatAdapter.getLastMessage();
                                if (newLast != null) {
                                    String text = newLast.getText();
                                    if (text == null || text.isEmpty()) {
                                        text = (newLast.getImagePath() != null && !newLast.getImagePath().isEmpty()) ? "Фотография" : "";
                                    }
                                    saveLastMessageToChatList(text);
                                } else {
                                    saveLastMessageToChatList("");
                                }
                            });
                        }
                        break;
                    case MESSAGE_REACTION_RECEIVED:
                        String rxData = (String) msg.obj;
                        if (rxData != null && rxData.contains(":::")) {
                            String[] rxParts = rxData.split(":::");
                            if (rxParts.length >= 2) {
                                String rxMsgId = rxParts[0];
                                String reactionStr = rxParts[1];
                                String authorLogin = rxParts.length >= 3 ? rxParts[2] : targetUsername;
                                String finalReaction = "REMOVE".equalsIgnoreCase(reactionStr) ? null : reactionStr;
                                
                                chatAdapter.updateMessageReactionById(rxMsgId, finalReaction, authorLogin);
                                
                                List<ChatMessage> history = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
                                for (ChatMessage m : history) {
                                    if (rxMsgId.equals(m.getMessageId())) {
                                        m.setReaction(finalReaction);
                                        m.setReactionSenderLogin(authorLogin);
                                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, m);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        };

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            if (isEditMode && editingMessageId != null) {
                if (connectedThread != null && connectedThread.isAlive()) {
                    String payloadStr = editingMessageId + ":::" + text;
                    connectedThread.sendPacket(TYPE_EDIT, payloadStr.getBytes(StandardCharsets.UTF_8));
                }
                chatAdapter.updateMessageTextById(editingMessageId, text);
                
                ChatMessage newLast = chatAdapter.getLastMessage();
                if (newLast != null && editingMessageId.equals(newLast.getMessageId())) {
                    saveLastMessageToChatList(text);
                }

                List<ChatMessage> editHistory = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
                for (ChatMessage m : editHistory) {
                    if (editingMessageId.equals(m.getMessageId())) {
                        m.setEdited(true);
                        m.setText(text);
                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, m);
                        break;
                    }
                }
                saveLastMessageToChatList(text);
                exitEditMode();
            } else {
                sendText(text);
                etMessage.setText("");
            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                long now = System.currentTimeMillis();
                if (now - lastTypingSentTime > 1500 && connectedThread != null) {
                    lastTypingSentTime = now;
                    sendActivityState("TYPING");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAttach.setOnClickListener(v -> {
            pickPhotoLauncher.launch("image/*");
        });

        if (btnPrimeConnect != null) {
            btnPrimeConnect.setOnClickListener(v -> {
                checkPermissionsAndStartRole(false);
            });
        }

        // Настройка Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается (Эмулятор)", Toast.LENGTH_SHORT).show();
            setupMockMode();
            return;
        }

        boolean useExistingSocket = intent.getBooleanExtra("EXTRA_USE_EXISTING_SOCKET", false);

        if (BluetoothSocketHolder.isConnectedWith(deviceAddress, targetUsername)) {
            Object threadObj = BluetoothSocketHolder.getConnectedThreadInstance();
            if (threadObj instanceof ConnectedThread) {
                ConnectedThread existingThread = (ConnectedThread) threadObj;
                if (existingThread.isAlive()) {
                    this.connectedThread = existingThread;
                    this.connectedThread.setUiHandler(handler);
                    
                    BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress);
                    BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

                    if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                    if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                    if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                        setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
                    } else {
                        setStatusWithAnimation("В сети", R.color.prime_success);
                    }
                    sendLocalAvatar();
                    flushPendingMessages();
                }
            }
        } else if (useExistingSocket && BluetoothSocketHolder.getSocket() != null && BluetoothSocketHolder.getSocket().isConnected()) {
            BluetoothSocket existingSocket = BluetoothSocketHolder.getSocket();
            if (BluetoothSocketHolder.getConnectedThreadInstance() instanceof ConnectedThread) {
                ConnectedThread existingThread = (ConnectedThread) BluetoothSocketHolder.getConnectedThreadInstance();
                if (existingThread.isAlive()) {
                    this.connectedThread = existingThread;
                    this.connectedThread.setUiHandler(handler);
                    
                    BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress);
                    BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

                    if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                    if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                    if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                        setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
                    } else {
                        setStatusWithAnimation("В сети", R.color.prime_success);
                    }
                    sendLocalAvatar();
                    flushPendingMessages();
                }
            } else {
                BluetoothDevice device = null;
                try {
                    device = existingSocket.getRemoteDevice();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get remote device", e);
                }
                connected(existingSocket, device);
            }
        } else {
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
            if (layoutInput != null) layoutInput.setVisibility(View.GONE);
            checkPermissionsAndStartRole(false);
        }

        saveLastMessageToChatList(null);
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                sendPresenceUpdate(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()) || Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                sendPresenceUpdate(true);
            }
        }
    };

    private void sendPresenceUpdate(boolean isOnline) {
        if (connectedThread != null && connectedThread.isAlive()) {
            long now = System.currentTimeMillis();
            String payloadStr = isOnline ? "ONLINE" : ("OFFLINE:" + now);
            connectedThread.sendPacket(TYPE_PRESENCE, payloadStr.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            String newTarget = intent.getStringExtra("EXTRA_CHAT_NAME");
            String newAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");
            if (newTarget != null && !newTarget.isEmpty()) {
                this.targetUsername = newTarget;
                if (newAddress != null) this.deviceAddress = newAddress;
                if (tvChatName != null) tvChatName.setText(targetUsername);
                updateAvatarUi(remoteAvatarUri, targetUsername);
                List<ChatMessage> history = ChatHistoryManager.loadMessages(this, targetUsername);
                if (chatAdapter != null) {
                    chatAdapter.setMessages(history);
                    if (chatAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean wasSubActivity = isOpeningSubActivity;
        isOpeningSubActivity = false;

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(screenReceiver, filter);
        } catch (Exception ignored) {}

        if (wasSubActivity) {
            sendActivityState("IDLE");
        } else {
            sendPresenceUpdate(true);
        }

        Object threadObj = BluetoothSocketHolder.getConnectedThreadInstance();
        if (threadObj instanceof ConnectedThread && ((ConnectedThread) threadObj).isAlive()) {
            this.connectedThread = (ConnectedThread) threadObj;
            this.connectedThread.setUiHandler(handler);
        }

        if (isRemoteUserOnline && connectedThread != null && connectedThread.isAlive()) {
            saveLastSeenTimestamp();
            if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
            } else {
                setStatusWithAnimation("В сети", R.color.prime_success);
            }
        } else {
            updateOfflineLastSeenStatus();
        }

        if (targetUsername != null && chatAdapter != null) {
            int oldSize = chatAdapter.getItemCount();
            List<ChatMessage> history = ChatHistoryManager.loadMessages(this, targetUsername);
            chatAdapter.setMessages(history);
            if (history.size() > oldSize && history.size() > 0) {
                rvMessages.scrollToPosition(history.size() - 1);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {}

        if (!isOpeningSubActivity) {
            sendPresenceUpdate(false);
        }

        if (!isChatDeleted) {
            saveLastMessageToChatList(null);
        }
        if (connectedThread != null) {
            connectedThread.setUiHandler(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connectedThread != null) {
            connectedThread.setUiHandler(null);
        }
        lastSeenHandler.removeCallbacks(lastSeenRunnable);
    }

    private final Handler lastSeenHandler = new Handler(Looper.getMainLooper());
    private final Runnable lastSeenRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectedThread == null || !connectedThread.isAlive()) {
                updateOfflineLastSeenStatus();
                lastSeenHandler.postDelayed(this, 30000);
            }
        }
    };

    private void saveLastSeenTimestamp() {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        sp.edit().putLong("last_seen_" + targetUsername, System.currentTimeMillis()).apply();
    }

    private void saveCustomLastSeenTimestamp(long ts) {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        sp.edit().putLong("last_seen_" + targetUsername, ts).apply();
    }

    private void updateOfflineLastSeenStatus() {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        long lastSeenTs = sp.getLong("last_seen_" + targetUsername, 0L);
        updateOfflineLastSeenStatus(lastSeenTs);
    }

    private void updateOfflineLastSeenStatus(long ts) {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        String lastSeenStr = formatLastSeen(ts);
        setStatusWithAnimation(lastSeenStr, R.color.prime_text_secondary);
    }

    public static String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Не в сети";
        long now = System.currentTimeMillis();
        long diffMs = now - timestamp;
        if (diffMs < 0) diffMs = 0;
        
        long diffMin = diffMs / (1000 * 60);
        long diffHours = diffMin / 60;

        if (diffMin < 1) {
            return "Был(а) в сети только что";
        } else if (diffMin < 60) {
            return "Был(а) в сети " + diffMin + " мин. назад";
        } else if (diffHours < 24) {
            return "Был(а) в сети " + diffHours + " ч. назад";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM в HH:mm", Locale.getDefault());
            return "Был(а) в сети " + sdf.format(new Date(timestamp));
        }
    }

    private void applyAndSendReaction(ChatMessage message, String reaction) {
        if (message == null || message.getMessageId() == null) return;

        // Protection: if reaction exists and belonged to someone else, prevent removing/modifying it
        if (reaction == null && message.getReaction() != null) {
            String author = message.getReactionSenderLogin();
            if (author != null && !author.isEmpty() && !author.equalsIgnoreCase(localUsername) && !author.equalsIgnoreCase("my") && !author.equalsIgnoreCase("me")) {
                PrimeNotification.INSTANCE.show(this, "Нельзя удалить чужую реакцию (" + author + ")", null);
                return;
            }
        }

        String msgId = message.getMessageId();
        message.setReaction(reaction);
        message.setReactionSenderLogin(localUsername);
        chatAdapter.updateMessageReactionById(msgId, reaction, localUsername);

        if (connectedThread != null && connectedThread.isAlive()) {
            String payloadStr = msgId + ":::" + (reaction != null ? reaction : "REMOVE") + ":::" + localUsername;
            connectedThread.sendPacket(TYPE_REACTION, payloadStr.getBytes(StandardCharsets.UTF_8));
        }

        ChatHistoryManager.saveMessage(this, targetUsername, message);
    }

    private void showAvatarActionTray() {
        String[] options = new String[]{
                "⚡ Отключиться",
                "📷 Просмотр фотографии",
                "🗑️ Удалить чат"
        };

        new MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
                .setTitle(targetUsername)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        disconnectCurrentChat();
                    } else if (which == 1) {
                        showFullScreenAvatar();
                    } else if (which == 2) {
                        initiateChatDeletionWithUndo();
                    }
                })
                .show();
    }

    private void disconnectCurrentChat() {
        if (connectedThread != null && connectedThread.isAlive()) {
            try {
                connectedThread.sendPacket(TYPE_DISCONNECT, "DISCONNECT".getBytes(StandardCharsets.UTF_8));
                Thread.sleep(100);
            } catch (Exception ignored) {}
        }
        isRemoteUserOnline = false;
        BluetoothSocketHolder.clearSocket();
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Exception ignored) {}

        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
        updateOfflineLastSeenStatus();

        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
        if (layoutInput != null) layoutInput.setVisibility(View.GONE);
        if (btnPrimeConnect != null) {
            btnPrimeConnect.setEnabled(true);
            btnPrimeConnect.setText("⚡ Праймериться!");
        }
        PrimeNotification.INSTANCE.show(this, "Соединение отключено", null);
    }

    private void enterEditMode(ChatMessage message, int position) {
        if (message == null || message.getMessageId() == null) return;
        isEditMode = true;
        editingMessageId = message.getMessageId();

        if (etMessage != null) {
            etMessage.setText(message.getText());
            etMessage.setSelection(etMessage.getText().length());
            etMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        }

        if (btnAttach != null) btnAttach.setVisibility(View.GONE);
        if (btnCancelEdit != null) btnCancelEdit.setVisibility(View.VISIBLE);

        if (layoutEditBar != null) {
            layoutEditBar.setVisibility(View.VISIBLE);
            layoutEditBar.setTranslationY(-30f);
            layoutEditBar.setAlpha(0f);
            layoutEditBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(this::updateMessageListPadding)
                    .start();
        }
        updateMessageListPadding();
    }

    private void exitEditMode() {
        isEditMode = false;
        editingMessageId = null;

        if (layoutEditBar != null && layoutEditBar.getVisibility() == View.VISIBLE) {
            layoutEditBar.animate()
                    .translationY(-30f)
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        layoutEditBar.setVisibility(View.GONE);
                        layoutEditBar.setTranslationY(0f);
                        layoutEditBar.setAlpha(1f);
                        updateMessageListPadding();
                    })
                    .start();
        }

        if (btnCancelEdit != null) btnCancelEdit.setVisibility(View.GONE);
        if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);

        if (etMessage != null) {
            etMessage.setText("");
        }
        updateMessageListPadding();
    }

    private void updateMessageListPadding() {
        View chatRoot = findViewById(R.id.chatRoot);
        View rvMessages = findViewById(R.id.rvMessages);
        if (rvMessages == null || chatRoot == null) return;

        float density = getResources().getDisplayMetrics().density;

        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(chatRoot);
        int topInset = (int) (24 * density);
        if (rootInsets != null) {
            Insets sb = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            topInset = sb.top;
        }

        int topPadding = topInset + (int) (76 * density);
        int bottomPadding = (int) (16 * density);

        rvMessages.setPadding(
                rvMessages.getPaddingLeft(),
                topPadding,
                rvMessages.getPaddingRight(),
                bottomPadding
        );
    }

    private void showEditMessageDialog(ChatMessage message, int position) {
        enterEditMode(message, position);
    }

    private void showFullScreenAvatar() {
        ImageView ivAvatar = findViewById(R.id.ivChatAvatar);
        String avatarPath = remoteAvatarUri;
        File localAvatarFile = new File(getFilesDir(), "avatar_" + targetUsername + ".jpg");
        if (localAvatarFile.exists()) {
            avatarPath = localAvatarFile.getAbsolutePath();
        }

        Bitmap bmp = null;
        if (ivAvatar != null && ivAvatar.getDrawable() instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable) ivAvatar.getDrawable()).getBitmap();
        }

        if (avatarPath != null || bmp != null) {
            ChatAdapter.showFullScreenPhoto(this, ivAvatar, bmp, avatarPath);
        } else {
            PrimeNotification.INSTANCE.show(this, "Аватар отсутствует", null);
        }
    }

    private boolean isDeletionPending = false;
    private final Handler deletionHandler = new Handler(Looper.getMainLooper());
    private final Runnable performDeletionRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDeletionPending) {
                isChatDeleted = true;
                if (connectedThread != null && connectedThread.isAlive()) {
                    String deletionPayload = "DELETE_CHAT:login=" + localUsername + ";name=" + localUsername;
                    connectedThread.sendPacket(TYPE_CHAT_DELETED, deletionPayload.getBytes(StandardCharsets.UTF_8));
                }
                ChatHistoryManager.deleteHistory(ChatPersonActivity.this, targetUsername);
                deleteChatFromChatListEx(targetUsername);
                disconnectCurrentChat();
                Toast.makeText(ChatPersonActivity.this, "Переписка полностью удалена", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    private void initiateChatDeletionWithUndo() {
        isDeletionPending = true;
        
        PrimeNotification.INSTANCE.show(this, "Удаление чата с " + targetUsername + "...", () -> {
            isDeletionPending = false;
            deletionHandler.removeCallbacks(performDeletionRunnable);
            Toast.makeText(ChatPersonActivity.this, "Удаление отменено", Toast.LENGTH_SHORT).show();
            return Unit.INSTANCE;
        });

        deletionHandler.postDelayed(performDeletionRunnable, 3000L);
    }

    private void checkPermissionsAndStartRole(boolean asServer) {
        Log.d(TAG, "Role clicked: " + asServer);
        pendingRoleAsServer = asServer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                });
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        pendingRoleAsServer = null;
        startPrimeConnection();
    }

    private void setupMockMode() {
        setStatusWithAnimation(isOnlineExpected ? "В сети (Локальный режим)" : "Не в сети (Локальный режим)", R.color.prime_text_secondary);
    }

    public static int getAppVersionCode(Context context) {
        if (context == null) return 1;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
            } else {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            }
        } catch (Exception e) {
            return 1;
        }
    }

    private void setStatusWithAnimation(String newText, int colorResOrValue) {
        if (tvChatStatus == null) return;
        CharSequence currentText = tvChatStatus.getText();
        if (currentText != null && currentText.toString().equals(newText)) return;

        int finalColor;
        try {
            finalColor = ContextCompat.getColor(this, colorResOrValue);
        } catch (Exception e) {
            finalColor = colorResOrValue;
        }

        int textColor = finalColor;
        tvChatStatus.animate()
                .translationY(25f)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    tvChatStatus.setText(newText);
                    tvChatStatus.setTextColor(textColor);
                    tvChatStatus.setTranslationY(-25f);
                    tvChatStatus.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(160)
                            .start();
                }).start();
    }

    private Bitmap createLetterAvatar(String name, int sizePx) {
        if (name == null || name.trim().isEmpty()) name = "P";
        String letter = name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
        int color = getAvatarColor(name);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sizePx * 0.45f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        float y = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(letter, sizePx / 2f, y, textPaint);

        return bitmap;
    }

    private void updateAvatarUi(String avatarUri, String name) {
        ImageView ivChatAvatar = findViewById(R.id.ivChatAvatar);
        if (ivChatAvatar == null) return;
        boolean loaded = false;
        
        File localAvatarFile = new File(getFilesDir(), "avatar_" + name + ".jpg");
        if (localAvatarFile.exists()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(localAvatarFile.getAbsolutePath());
                if (bmp != null) {
                    ivChatAvatar.setImageBitmap(bmp);
                    loaded = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load cached avatar", e);
            }
        }
        
        if (!loaded && avatarUri != null && !avatarUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(avatarUri);
                boolean exists = false;
                if ("file".equals(uri.getScheme())) {
                    String path = uri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        exists = file.exists();
                    }
                } else if ("content".equals(uri.getScheme())) {
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        exists = (is != null);
                    } catch (Exception ignored) {
                        exists = false;
                    }
                } else {
                    exists = true;
                }

                if (exists) {
                    ivChatAvatar.setImageURI(uri);
                    if (ivChatAvatar.getDrawable() != null) {
                        loaded = true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load avatar URI", e);
            }
        }
        if (!loaded) {
            ivChatAvatar.setImageBitmap(createLetterAvatar(name, 120));
        }
    }

    private int getAvatarColor(String name) {
        if (name == null) name = "Prime";
        String[] colors = new String[]{"#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"};
        int hash = name.hashCode();
        int index = (hash == Integer.MIN_VALUE ? 0 : Math.abs(hash)) % colors.length;
        return Color.parseColor(colors[index]);
    }

    private void addMessageToUI(ChatMessage message) {
        if (message == null) return;
        if (message.getText() != null && message.getText().startsWith("HANDSHAKE:")) return;
        
        if (message.getImageBitmap() != null && (message.getImagePath() == null || message.getImagePath().isEmpty())) {
            String path = ChatHistoryManager.saveBitmapToFile(this, message.getImageBitmap(), message.getTimestamp());
            message.setImagePath(path);
        }
        
        chatAdapter.addMessage(message);
        rvMessages.post(() -> {
            if (chatAdapter.getItemCount() > 0) {
                rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
        
        // Save to persistence
        ChatHistoryManager.saveMessage(this, targetUsername, message);
        String lastMsg = message.getText();
        saveLastMessageToChatList(
            lastMsg != null && !lastMsg.isEmpty() ? lastMsg : "Фотография",
            message.isOutgoing() ? message.getMessageStatus() : MessageStatus.NONE,
            false,
            "ONLINE"
        );
    }

    private void showDeleteChatDialog() {
        new MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
                .setTitle("Удалить переписку")
                .setMessage("Вы действительно хотите полностью безвозвратно удалить всю историю сообщений с " + targetUsername + "?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    ChatHistoryManager.deleteHistory(this, targetUsername);
                    chatAdapter.setMessages(new ArrayList<>());
                    deleteChatFromChatList(targetUsername);
                    Toast.makeText(this, "Переписка удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteChatFromChatList(String targetName) {
        deleteChatFromChatListEx(targetName);
    }

    private void deleteChatFromChatListEx(String nameOrId) {
        isChatDeleted = true;
        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
        if (nameOrId != null && !nameOrId.equalsIgnoreCase(targetUsername)) {
            ChatHistoryManager.deleteHistoryCompletely(this, nameOrId, deviceAddress);
        }
    }

    private void migrateHistoryIfNeeded(String oldKey, String newKey) {
        if (oldKey == null || newKey == null || oldKey.equalsIgnoreCase(newKey)) return;
        List<ChatMessage> oldHistory = ChatHistoryManager.loadMessages(this, oldKey);
        if (!oldHistory.isEmpty()) {
            List<ChatMessage> newHistory = ChatHistoryManager.loadMessages(this, newKey);
            newHistory.addAll(oldHistory);
            ChatHistoryManager.saveHistoryList(this, newKey, newHistory);
            ChatHistoryManager.deleteHistory(this, oldKey);
        }
    }

    private void saveLastMessageToChatList(String lastMsg, MessageStatus messageStatus, boolean incrementUnread, String onlineStatusStr) {
        if (isChatDeleted || targetUsername == null || targetUsername.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            JSONObject updatedObj = null;
            JSONArray newArray = new JSONArray();
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (targetUsername.equalsIgnoreCase(obj.optString("name")) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(obj.optString("id")))) {
                    if (lastMsg != null) {
                        obj.put("lastMessage", lastMsg);
                        obj.put("time", timeStr);
                    }
                    if (messageStatus != null) {
                        obj.put("messageStatus", messageStatus.name());
                    }
                    if (incrementUnread) {
                        int currentUnread = obj.optInt("unreadCount", 0);
                        obj.put("unreadCount", currentUnread + 1);
                    } else if (connectedThread != null && connectedThread.getUiHandler() != null) {
                        obj.put("unreadCount", 0);
                    }
                    if (onlineStatusStr != null) {
                        obj.put("onlineStatus", onlineStatusStr);
                    }
                    if (remoteAvatarUri != null && !remoteAvatarUri.isEmpty()) {
                        obj.put("avatarUri", remoteAvatarUri);
                    }
                    if (targetUsername != null && !targetUsername.isEmpty()) {
                        obj.put("name", targetUsername);
                    }
                    updatedObj = obj;
                } else {
                    newArray.put(obj);
                }
            }

            if (updatedObj == null) {
                updatedObj = new JSONObject();
                updatedObj.put("id", deviceAddress != null ? deviceAddress : System.currentTimeMillis() + "");
                updatedObj.put("name", targetUsername);
                updatedObj.put("lastMessage", lastMsg != null ? lastMsg : "");
                updatedObj.put("time", timeStr);
                updatedObj.put("avatarUri", remoteAvatarUri != null ? remoteAvatarUri : "");
                updatedObj.put("onlineStatus", onlineStatusStr != null ? onlineStatusStr : "ONLINE");
                updatedObj.put("messageStatus", messageStatus != null ? messageStatus.name() : "NONE");
                updatedObj.put("unreadCount", incrementUnread ? 1 : 0);
                updatedObj.put("isMuted", false);
            }

            // Put active updated chat at top (index 0)
            JSONArray finalArray = new JSONArray();
            finalArray.put(updatedObj);
            for (int i = 0; i < newArray.length(); i++) {
                finalArray.put(newArray.get(i));
            }

            sharedPrefs.edit().putString("persisted_chats", finalArray.toString()).commit();
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats", e);
        }
    }

    private final Runnable clearChatListTypingRunnable = () -> {
        saveActivityStateToChatList(targetUsername, "IDLE");
    };

    private void saveActivityStateToChatList(String targetName, String state) {
        if (targetName == null || targetName.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (targetName.equalsIgnoreCase(obj.optString("name"))) {
                    obj.put("activityState", state != null ? state : "IDLE");
                    if ("TYPING".equalsIgnoreCase(state)) {
                        obj.put("typingUntil", System.currentTimeMillis() + 3500L);
                    } else {
                        obj.put("typingUntil", 0L);
                    }
                    break;
                }
            }
            sharedPrefs.edit().putString("persisted_chats", array.toString()).commit();
            ChatListNotifier.INSTANCE.notifyChanged();

            if ("TYPING".equalsIgnoreCase(state)) {
                typingResetHandler.removeCallbacks(clearChatListTypingRunnable);
                typingResetHandler.postDelayed(clearChatListTypingRunnable, 3600L);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save activity state to chat list", e);
        }
    }

    private void saveTypingStateToChatList(String targetName, boolean isTyping) {
        saveActivityStateToChatList(targetName, isTyping ? "TYPING" : "IDLE");
    }

    private void saveLastMessageToChatList(String lastMsg) {
        saveLastMessageToChatList(lastMsg, null, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) return null;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap;
        }
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        }
        resizedWidth = Math.max(1, resizedWidth);
        resizedHeight = Math.max(1, resizedHeight);
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void flushPendingMessages() {
        if (connectedThread == null || !connectedThread.isAlive()) return;
        while (!pendingMessageQueue.isEmpty()) {
            PendingMessage pm = pendingMessageQueue.poll();
            if (pm != null && connectedThread != null) {
                connectedThread.sendPacket(pm.type, pm.payload);
            }
        }
    }

    private void sendText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        ChatMessage message = new ChatMessage(text, time, localUsername, true, null, timestamp, null, null);
        
        String packetContent = message.getMessageId() + ":::" + text;
        byte[] payload = packetContent.getBytes(StandardCharsets.UTF_8);
        if (connectedThread != null && connectedThread.isAlive()) {
            connectedThread.sendPacket(TYPE_TEXT, payload);
        } else {
            pendingMessageQueue.add(new PendingMessage(TYPE_TEXT, payload));
            startPrimeConnection();
        }
        addMessageToUI(message);
    }

    private void sendPhoto(Uri uri) {
        if (uri == null) return;
        try {
            Bitmap bitmap = null;
            if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                File file = new File(uri.getPath());
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            }
            if (bitmap == null) {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is != null) {
                    bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                }
            }
            
            if (bitmap != null) {
                Bitmap scaledBitmap = scaleBitmapDown(bitmap, 1024);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] photoBytes = baos.toByteArray();
                
                long timestamp = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();
                
                byte[] headerBytes = (messageId + ":::").getBytes(StandardCharsets.UTF_8);
                byte[] fullPayload = new byte[headerBytes.length + photoBytes.length];
                System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
                System.arraycopy(photoBytes, 0, fullPayload, headerBytes.length, photoBytes.length);

                if (connectedThread != null && connectedThread.isAlive()) {
                    connectedThread.sendPacket(TYPE_PHOTO, fullPayload);
                } else {
                    pendingMessageQueue.add(new PendingMessage(TYPE_PHOTO, fullPayload));
                    startPrimeConnection();
                }
                
                ChatMessage photoMsg = new ChatMessage(null, time, localUsername, true, scaledBitmap, timestamp, null, messageId);
                addMessageToUI(photoMsg);
            } else {
                Log.e(TAG, "Failed to decode photo from URI: " + uri);
                PrimeNotification.INSTANCE.show(this, "Не удалось загрузить фото", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load photo", e);
            PrimeNotification.INSTANCE.show(this, "Не удалось загрузить фото", null);
        }
    }

    private void sendLocalAvatar() {
        if (connectedThread == null) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPrefs.getString("current_user", "");
            String localAvatarUri = sharedPrefs.getString(currentUser + "_avatar", "");
            
            if (localAvatarUri != null && !localAvatarUri.isEmpty()) {
                InputStream is = getContentResolver().openInputStream(Uri.parse(localAvatarUri));
                if (is != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bitmap != null) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] avatarBytes = baos.toByteArray();
                        connectedThread.sendPacket(TYPE_AVATAR, avatarBytes);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send local avatar", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void startPrimeConnection() {
        Log.d(TAG, "Starting Fast Direct Connection...");
        
        if (btnPrimeConnect != null) {
            btnPrimeConnect.setEnabled(false);
            btnPrimeConnect.setText("Подключение...");
        }
        setStatusWithAnimation("Установка связи...", R.color.prime_accent);

        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, 3000L);

        // 1. Немедленно отменяем любое фоновое сканирование ОС (критично для скорости RFCOMM)
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }

        // 2. Очищаем старые потоки
        if (acceptThread != null) { acceptThread.cancel(); acceptThread = null; }
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }

        // 3. Всегда поднимаем серверный сокет для приема входящих подключений
        acceptThread = new AcceptThread();
        acceptThread.start();

        // 4. Мгновенный сокетный коннект по известному MAC-адресу
        if (deviceAddress != null && !deviceAddress.isEmpty() && !"null".equals(deviceAddress)) {
            String myAddress = "";
            try {
                myAddress = bluetoothAdapter.getAddress();
            } catch (SecurityException ignored) {}

            boolean isPrimaryInitiator = myAddress != null && myAddress.compareToIgnoreCase(deviceAddress) > 0;
            long delayMs = isPrimaryInitiator ? 200 : 1000;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (connectedThread == null && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    try {
                        bluetoothAdapter.cancelDiscovery();
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                        connectThread = new ConnectThread(device);
                        connectThread.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start ConnectThread", e);
                    }
                }
            }, delayMs);
        }
    }

    @SuppressLint("MissingPermission")
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }

        if (isServer) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
            acceptThread = null;
        } else {
            if (acceptThread != null) {
                acceptThread.cancel();
                acceptThread = null;
            }
            connectThread = null;
        }

        if (connectedThread != null && connectedThread.isAlive()) {
            // Re-use active connected thread if already active
            connectedThread.setUiHandler(handler);
        } else {
            boolean handshakeReceived = getIntent().getBooleanExtra("EXTRA_HANDSHAKE_RECEIVED", false);
            connectedThread = new ConnectedThread(socket, handshakeReceived);
            connectedThread.start();
        }

        BluetoothSocketHolder.setSocket(socket);
        BluetoothSocketHolder.setConnectedThreadInstance(connectedThread);
        BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress != null ? deviceAddress : (device != null ? device.getAddress() : null));
        BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

        isRemoteUserOnline = true;
        saveLastMessageToChatList(null, null, false, "ONLINE");

        try {
            PrimeBluetoothService.startService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start bluetooth service", e);
        }

        runOnUiThread(() -> {
            setStatusWithAnimation("В сети", R.color.prime_success);
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
            if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
        });
    }

    private void connectionFailed() {
        PrimeNotification.INSTANCE.show(this, "Не удалось подключиться", null);
        runOnUiThread(() -> {
            if (btnPrimeConnect != null) {
                btnPrimeConnect.setEnabled(true);
                btnPrimeConnect.setText("⚡ Праймериться!");
            }
            updateOfflineLastSeenStatus();
        });
    }

    private void connectionLost() {
        BluetoothSocketHolder.clearSocket();
        if (!isHandshakeDone && connectedThread == null) {
            return;
        }
        isHandshakeDone = false;

        if (!isOpeningSubActivity && !isChatDeleted && connectedThread != null && connectedThread.getUiHandler() != null) {
            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Соединение разорвано", null);
        } else if (!isOpeningSubActivity && !isChatDeleted) {
            showBackgroundNotification(targetUsername, "Соединение разорвано");
        }
        
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }

        saveLastSeenTimestamp();
        runOnUiThread(() -> {
            updateOfflineLastSeenStatus();
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
            if (layoutInput != null) layoutInput.setVisibility(View.GONE);
            if (btnPrimeConnect != null) {
                btnPrimeConnect.setEnabled(true);
                btnPrimeConnect.setText("⚡ Праймериться!");
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT < 34) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void showBackgroundNotification(String title, String message) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            "prime_messages",
                            "Сообщения Prime",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    nm.createNotificationChannel(channel);
                }
                
                Intent intent = new Intent(this, ChatPersonActivity.class);
                intent.putExtra("EXTRA_CHAT_NAME", targetUsername);
                intent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
                intent.putExtra("EXTRA_USE_EXISTING_SOCKET", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pi = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                Bitmap largeAvatarBmp = getAvatarBitmapForNotification(targetUsername, remoteAvatarUri);

                Person.Builder personBuilder = new Person.Builder().setName(title);
                if (largeAvatarBmp != null) {
                    personBuilder.setIcon(IconCompat.createWithBitmap(largeAvatarBmp));
                }
                Person sender = personBuilder.build();
                Person me = new Person.Builder().setName("Вы").build();

                NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(me)
                        .addMessage(message, System.currentTimeMillis(), sender);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "prime_messages")
                        .setSmallIcon(R.drawable.ic_prime_statusbar)
                        .setStyle(style)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi);

                nm.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show background notification", e);
        }
    }

    private Bitmap getAvatarBitmapForNotification(String name, String avatarUri) {
        try {
            File localAvatarFile = new File(getFilesDir(), "avatar_" + name + ".jpg");
            if (localAvatarFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(localAvatarFile.getAbsolutePath());
                if (bmp != null) return bmp;
            }
            if (avatarUri != null && !avatarUri.isEmpty()) {
                Uri uri = Uri.parse(avatarUri);
                if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                    File file = new File(uri.getPath());
                    if (file.exists()) {
                        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bmp != null) return bmp;
                    }
                }
            }
        } catch (Exception ignored) {}
        return createLetterAvatar(name, 120);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lastSeenHandler != null) lastSeenHandler.removeCallbacksAndMessages(null);
        if (typingResetHandler != null) typingResetHandler.removeCallbacksAndMessages(null);
        if (dateHideHandler != null) dateHideHandler.removeCallbacksAndMessages(null);
        if (connectTimeoutHandler != null) connectTimeoutHandler.removeCallbacksAndMessages(null);
        if (deletionHandler != null) deletionHandler.removeCallbacksAndMessages(null);

        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }
        // Detach UI handler so background loop handles incoming messages, BUT DO NOT CLOSE SOCKET OR STOP SERVICE
        if (connectedThread != null) {
            connectedThread.setUiHandler(null);
        }
    }

    // --- Потоки Bluetooth ---

    @SuppressLint("MissingPermission")
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("PrimeChat", UUID_CHAT);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (mmServerSocket == null) return;
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                if (socket != null) {
                    connected(socket, socket.getRemoteDevice());
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            if (mmServerSocket == null) return;
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        public void run() {
            if (bluetoothAdapter != null) {
                try {
                    bluetoothAdapter.cancelDiscovery();
                } catch (Exception ignored) {}
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            // 3-Stage Robust RFCOMM Connection Strategy
            // Stage 1: Insecure RFCOMM
            try {
                mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(UUID_CHAT);
                mmSocket.connect();
                connected(mmSocket, mmDevice);
                return;
            } catch (IOException e1) {
                Log.w(TAG, "Stage 1 (Insecure RFCOMM) failed, trying Stage 2...", e1);
                if (mmSocket != null) {
                    try { mmSocket.close(); } catch (IOException ignored) {}
                }
            }

            // Stage 2: Secure RFCOMM
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID_CHAT);
                mmSocket.connect();
                connected(mmSocket, mmDevice);
                return;
            } catch (IOException e2) {
                Log.w(TAG, "Stage 2 (Secure RFCOMM) failed, trying Stage 3...", e2);
                if (mmSocket != null) {
                    try { mmSocket.close(); } catch (IOException ignored) {}
                }
            }

            // Stage 3: Reflection Channel 1
            try {
                mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", int.class).invoke(mmDevice, 1);
                if (mmSocket != null) {
                    mmSocket.connect();
                    connected(mmSocket, mmDevice);
                    return;
                }
            } catch (Exception e3) {
                Log.e(TAG, "Stage 3 (Reflection RFCOMM) failed", e3);
                if (mmSocket != null) {
                    try { mmSocket.close(); } catch (IOException ignored) {}
                }
            }

            connectionFailed();
        }

        public void cancel() {
            if (mmSocket == null) return;
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;
        private boolean isHandshakeDone = false;
        private Handler keepAliveHandler;
        private Runnable keepAliveRunnable;
        private volatile Handler activeUiHandler;
        private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

        public ConnectedThread(BluetoothSocket socket, boolean handshakeDone) {
            mmSocket = socket;
            this.activeUiHandler = handler;
            DataInputStream tmpIn = null;
            DataOutputStream tmpOut = null;
            try {
                tmpIn = new DataInputStream(socket.getInputStream());
                tmpOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            isHandshakeDone = handshakeDone;
            if (isHandshakeDone) {
                postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
            }
        }

        public void setUiHandler(Handler uiHandler) {
            this.activeUiHandler = uiHandler;
        }

        public Handler getUiHandler() {
            return activeUiHandler;
        }

        private void postToUi(int what, int arg1, int arg2, Object obj) {
            Handler h = activeUiHandler;
            if (h != null) {
                h.obtainMessage(what, arg1, arg2, obj).sendToTarget();
            }
        }

        public void run() {
            if (mmInStream == null || mmOutStream == null) return;
            // Отправляем рукопожатие при старте
            try {
                SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                String currentUser = sharedPrefs.getString("current_user", "");
                String myDisplayName = sharedPrefs.getString(currentUser + "_name", currentUser);
                String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + (myDisplayName != null ? myDisplayName : localUsername) + ";version=" + getAppVersionCode(getApplicationContext());
                sendPacket(TYPE_TEXT, handshake.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                Log.e(TAG, "Exception during handshake write", e);
            }

            keepAliveHandler = new Handler(Looper.getMainLooper());
            keepAliveRunnable = new Runnable() {
                @Override
                public void run() {
                    sendPacket(TYPE_PING, new byte[0]);
                    keepAliveHandler.postDelayed(this, 5000);
                }
            };
            keepAliveHandler.postDelayed(keepAliveRunnable, 5000);

            while (true) {
                try {
                    byte type = mmInStream.readByte();
                    int length = mmInStream.readInt();
                    
                    if (length < 0 || length > 15 * 1024 * 1024) {
                        throw new IOException("Invalid packet length: " + length);
                    }

                    byte[] payload = null;
                    if (length > 0) {
                        payload = new byte[length];
                        mmInStream.readFully(payload);
                    }

                    if (type == TYPE_PING) {
                        postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                        continue;
                    }

                    if (type == TYPE_ACK) {
                        continue;
                    }

                    if (type == TYPE_READ_RECEIPT && payload != null) {
                        postToUi(MESSAGE_READ_RECEIPT, payload.length, -1, payload);
                        saveLastMessageToChatList(null, MessageStatus.READ, false, "ONLINE");
                    } else if (type == TYPE_DISCONNECT) {
                        postToUi(MESSAGE_DISCONNECTED, -1, -1, null);
                        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
                    } else if (type == TYPE_TEXT && payload != null) {
                        sendPacket(TYPE_ACK, new byte[0]);
                        String receivedData = new String(payload, StandardCharsets.UTF_8);
                        
                        if (receivedData.startsWith("HANDSHAKE:")) {
                            isHandshakeDone = true;
                            isRemoteUserOnline = true;
                            String data = receivedData.substring(10).trim();
                            String remoteLogin = targetUsername;
                            String remoteName = targetUsername;
                            if (data.contains("login=") || data.contains("name=")) {
                                String[] parts = data.split(";");
                                for (String p : parts) {
                                    if (p.startsWith("login=")) remoteLogin = p.substring(6);
                                    else if (p.startsWith("name=")) remoteName = p.substring(5);
                                    else if (p.startsWith("version=")) {
                                        try {
                                            remoteVersionCode = Integer.parseInt(p.substring(8));
                                        } catch (Exception ignored) {}
                                    }
                                }
                            } else {
                                remoteName = data;
                            }
                            
                            if (!remoteName.isEmpty()) {
                                String oldTarget = targetUsername;
                                targetUsername = remoteName;
                                migrateHistoryIfNeeded(oldTarget, remoteName);
                                if (deviceAddress != null) {
                                    migrateHistoryIfNeeded(deviceAddress, remoteName);
                                }
                                
                                // Do not trust the remote avatar URI string (e.g. content://) 
                                // from another device. We will rely on TYPE_AVATAR packets.

                                final String finalName = remoteName;

                                saveLastMessageToChatList(null, null, false, "ONLINE");

                                if (activeUiHandler != null) {
                                    runOnUiThread(() -> {
                                        if (tvChatName != null) tvChatName.setText(finalName);
                                        updateAvatarUi(remoteAvatarUri, finalName);
                                    });
                                    postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                                } else {
                                    try {
                                        Intent openChatIntent = new Intent(getApplicationContext(), ChatPersonActivity.class);
                                        openChatIntent.putExtra("EXTRA_CHAT_NAME", finalName);
                                        openChatIntent.putExtra("EXTRA_DEVICE_ADDRESS", mmSocket.getRemoteDevice().getAddress());
                                        openChatIntent.putExtra("EXTRA_USE_EXISTING_SOCKET", true);
                                        openChatIntent.putExtra("EXTRA_HANDSHAKE_RECEIVED", true);
                                        openChatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        getApplicationContext().startActivity(openChatIntent);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to auto-open ChatPersonActivity on incoming handshake", e);
                                    }
                                }
                            } else {
                                postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                            }
                            continue;
                        }

                        if (activeUiHandler != null) {
                            String textMsgId = extractMsgId(receivedData);
                            if (textMsgId != null && !textMsgId.isEmpty()) {
                                sendPacket(TYPE_READ_RECEIPT, textMsgId.getBytes(StandardCharsets.UTF_8));
                            }
                            postToUi(MESSAGE_READ, payload.length, -1, payload);
                            saveLastMessageToChatList(extractRealText(receivedData), MessageStatus.READ, false, "ONLINE");
                        } else {
                            processBackgroundTextMessage(receivedData);
                        }
                    } else if (type == TYPE_PHOTO && payload != null) {
                        sendPacket(TYPE_ACK, new byte[0]);
                        
                        String photoMsgId = null;
                        int sepIdx = -1;
                        for (int i = 0; i < Math.min(payload.length, 120); i++) {
                            if (payload[i] == ':' && i + 2 < payload.length && payload[i+1] == ':' && payload[i+2] == ':') {
                                sepIdx = i;
                                break;
                            }
                        }
                        if (sepIdx != -1) {
                            photoMsgId = new String(payload, 0, sepIdx, StandardCharsets.UTF_8);
                        }

                        if (photoMsgId != null && !photoMsgId.isEmpty()) {
                            sendPacket(TYPE_READ_RECEIPT, photoMsgId.getBytes(StandardCharsets.UTF_8));
                        }

                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_READ_PHOTO, payload.length, -1, payload);
                            saveLastMessageToChatList("Фотография", MessageStatus.READ, false, "ONLINE");
                        } else {
                            processBackgroundPhotoMessage(payload);
                        }
                    } else if (type == TYPE_AVATAR && payload != null) {
                        postToUi(MESSAGE_READ_AVATAR, payload.length, -1, payload);
                    } else if (type == TYPE_TYPING) {
                        String stateData = (payload != null && payload.length > 0) ? new String(payload, StandardCharsets.UTF_8) : "STATE:TYPING";
                        postToUi(MESSAGE_TYPING, -1, -1, stateData);
                        if (stateData.contains("TYPING")) {
                            saveTypingStateToChatList(targetUsername, true);
                        }
                    } else if (type == TYPE_EDIT && payload != null) {
                        String data = new String(payload, StandardCharsets.UTF_8);
                        postToUi(MESSAGE_EDIT_RECEIVED, -1, -1, data);
                    } else if (type == TYPE_DELETE_MSG && payload != null) {
                        String delMsgId = new String(payload, StandardCharsets.UTF_8);
                        postToUi(MESSAGE_DELETE_SINGLE, -1, -1, delMsgId);
                        ChatHistoryManager.deleteSingleMessage(getApplicationContext(), targetUsername, delMsgId);
                    } else if (type == TYPE_CHAT_DELETED) {
                        ChatHistoryManager.deleteHistory(getApplicationContext(), targetUsername);
                        deleteChatFromChatList(targetUsername);
                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_CHAT_DELETED, -1, -1, null);
                        } else {
                            BluetoothSocketHolder.clearSocket();
                            PrimeBluetoothService.stopService(getApplicationContext());
                            showBackgroundNotification(targetUsername, "Собеседник полностью удалил переписку!");
                        }
                    } else if (type == TYPE_PRESENCE && payload != null) {
                        String presenceData = new String(payload, StandardCharsets.UTF_8);
                        postToUi(MESSAGE_PRESENCE_UPDATED, -1, -1, presenceData);
                        if (presenceData.startsWith("OFFLINE:")) {
                            try {
                                long ts = Long.parseLong(presenceData.substring(8));
                                saveCustomLastSeenTimestamp(ts);
                                saveLastMessageToChatList(null, null, false, "OFFLINE");
                            } catch (Exception ignored) {}
                        } else if (presenceData.startsWith("ONLINE")) {
                            saveLastSeenTimestamp();
                            saveLastMessageToChatList(null, null, false, "ONLINE");
                        }
                    } else if (type == TYPE_REACTION && payload != null) {
                        String rxData = new String(payload, StandardCharsets.UTF_8);
                        postToUi(MESSAGE_REACTION_RECEIVED, -1, -1, rxData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
            
            if (keepAliveHandler != null && keepAliveRunnable != null) {
                keepAliveHandler.removeCallbacks(keepAliveRunnable);
            }
            writeExecutor.shutdownNow();
        }

        private String extractMsgId(String data) {
            if (data == null) return null;
            int sep = data.indexOf(":::");
            return sep != -1 ? data.substring(0, sep) : null;
        }

        private String extractRealText(String data) {
            if (data == null) return "";
            int sep = data.indexOf(":::");
            return sep != -1 ? data.substring(sep + 3) : data;
        }

        private void processBackgroundTextMessage(String receivedData) {
            if (receivedData == null || receivedData.startsWith("HANDSHAKE:")) return;
            String msgId = null;
            String realText = receivedData;
            int sep = receivedData.indexOf(":::");
            if (sep != -1) {
                msgId = receivedData.substring(0, sep);
                realText = receivedData.substring(sep + 3);
            }
            if (realText.startsWith("HANDSHAKE:")) return;
            long timestamp = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            ChatMessage incomingMessage = new ChatMessage(realText, time, targetUsername, false, null, timestamp, null, msgId);
            
            ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, incomingMessage);
            saveLastMessageToChatList(realText, MessageStatus.NONE, true, "ONLINE");

            Handler uiH = activeUiHandler;
            if (uiH != null) {
                uiH.post(() -> addMessageToUI(incomingMessage));
            } else {
                showBackgroundNotification(targetUsername, realText);
            }
        }

        private void processBackgroundPhotoMessage(byte[] fullPayload) {
            try {
                String photoMsgId = null;
                byte[] photoBytes = fullPayload;
                int sepIdx = -1;
                for (int i = 0; i < Math.min(fullPayload.length, 120); i++) {
                    if (fullPayload[i] == ':' && i + 2 < fullPayload.length && fullPayload[i+1] == ':' && fullPayload[i+2] == ':') {
                        sepIdx = i;
                        break;
                    }
                }
                if (sepIdx != -1) {
                    photoMsgId = new String(fullPayload, 0, sepIdx, StandardCharsets.UTF_8);
                    photoBytes = new byte[fullPayload.length - (sepIdx + 3)];
                    System.arraycopy(fullPayload, sepIdx + 3, photoBytes, 0, photoBytes.length);
                }

                Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                long photoTs = System.currentTimeMillis();
                String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                ChatMessage photoMsg = new ChatMessage(null, photoTime, targetUsername, false, photoBitmap, photoTs, null, photoMsgId);
                
                ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, photoMsg);
                saveLastMessageToChatList("Фотография", MessageStatus.NONE, true, "ONLINE");

                Handler uiH = activeUiHandler;
                if (uiH != null) {
                    uiH.post(() -> addMessageToUI(photoMsg));
                } else {
                    showBackgroundNotification(targetUsername, "Фотография");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process background photo", e);
            }
        }

        public void sendPacket(byte type, byte[] payload) {
            if (mmOutStream == null) return;
            writeExecutor.execute(() -> {
                try {
                    mmOutStream.writeByte(type);
                    int length = payload != null ? payload.length : 0;
                    mmOutStream.writeInt(length);
                    if (length > 0 && payload != null) {
                        int offset = 0;
                        int chunkSize = 8192;
                        while (offset < length) {
                            int bytesToWrite = Math.min(chunkSize, length - offset);
                            mmOutStream.write(payload, offset, bytesToWrite);
                            offset += bytesToWrite;
                        }
                    }
                    mmOutStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                }
            });
        }

        public void cancel() {
            try {
                if (keepAliveHandler != null && keepAliveRunnable != null) {
                    keepAliveHandler.removeCallbacks(keepAliveRunnable);
                }
                writeExecutor.shutdownNow();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
