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
import android.graphics.Canvas;
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
import java.util.Set;

import android.util.Log;

import android.view.HapticFeedbackConstants;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentUris;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.concurrent.Executors;
import android.view.LayoutInflater;

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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
    private static final int MESSAGE_READ_FILE = 15;
    private static final int MESSAGE_SEND_PROGRESS = 16;

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
    private static final byte TYPE_FILE = 0x0E;
    private static final byte TYPE_PROFILE_UPDATE = 0x0F;

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

    private View layoutReplyBar;
    private TextView tvReplyBarTitle;
    private TextView tvReplyBarText;
    private ImageButton btnCloseReplyBar;
    private ChatMessage replyingToMessage = null;

    // Attachment Panel & Pending Attachment Views
    private LinearLayout layoutAttachmentPanel;
    private LinearLayout layoutPendingAttachment;
    private LinearLayout layoutSendingProgress;
    private ProgressBar pbSendingProgress;
    private TextView tvSendingProgressPercent;
    private ShapeableImageView ivPendingThumbnail;
    private ImageView ivPendingVideoBadge;
    private TextView tvPendingName;
    private TextView tvPendingSize;
    private ImageButton btnCancelPending;

    // Attachment Panel Modes UI
    private LinearLayout layoutModeCamera, layoutModePhoto, layoutModeFiles;
    private FrameLayout vModeCameraBg, vModePhotoBg, vModeFilesBg;
    private ImageView ivModeCameraIcon, ivModePhotoIcon, ivModeFilesIcon;
    private TextView tvModeCameraLabel, tvModePhotoLabel, tvModeFilesLabel;

    // Attachment Panel Content Sections UI
    private View layoutSectionCamera, layoutSectionPhoto, layoutSectionFiles;
    private MaterialCardView btnCameraPhoto, btnCameraVideo;
    private RecyclerView rvGalleryGrid, rvFilesGrid;
    private TextView tvGalleryEmpty, tvFilesEmpty;

    // Attachment State
    private boolean isAttachmentPanelOpen = false;
    private int currentAttachmentMode = 0; // 0: Camera, 1: Photo, 2: Files
    private PendingAttachment currentPendingAttachment = null;
    private GalleryGridAdapter galleryAdapter;
    private FileGridAdapter filesAdapter;

    private Uri cameraPhotoUri = null;
    private Uri cameraVideoUri = null;

    private final ActivityResultLauncher<Intent> cameraPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraPhotoUri != null) {
                        setPendingAttachmentFromUri(cameraPhotoUri, false, "Фото с камеры");
                    } else if (result.getData() != null && result.getData().getData() != null) {
                        setPendingAttachmentFromUri(result.getData().getData(), false, "Фото с камеры");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraVideoUri != null) {
                        setPendingAttachmentFromUri(cameraVideoUri, true, "Видео с камеры");
                    } else if (result.getData() != null && result.getData().getData() != null) {
                        setPendingAttachmentFromUri(result.getData().getData(), true, "Видео с камеры");
                    }
                }
            });

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

    private final Handler senderTypingHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopSenderTypingRunnable = () -> {
        if ("TYPING".equalsIgnoreCase(myLocalActivityState)) {
            sendActivityState("IDLE");
        }
    };

    private long lastTypingSentTime = 0;
    private final Handler typingResetHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetTypingRunnable = () -> {
        currentActivityState = "IDLE";
        saveActivityStateToChatList(targetUsername, "IDLE");
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

    private int connectionRetryCount = 0;
    private static final int MAX_AUTO_RETRIES = 3;
    private final Handler autoRetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed() && (connectedThread == null || !connectedThread.isAlive())) {
                Log.d(TAG, "Auto-retrying connection attempt " + (connectionRetryCount + 1) + "...");
                startPrimeConnection();
            }
        }
    };

    private final Handler connectTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectedThread == null || !connectedThread.isAlive()) {
                if (btnPrimeConnect != null) {
                    btnPrimeConnect.setEnabled(true);
                    btnPrimeConnect.setText("⚡ Соединиться");
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

    private static class ParsedMessagePayload {
        String msgId = null;
        String realText = "";
        String replyToId = null;
        String replyToSender = null;
        String replyToText = null;

        static ParsedMessagePayload parse(String data) {
            ParsedMessagePayload p = new ParsedMessagePayload();
            if (data == null || data.isEmpty()) return p;

            String body = data;
            int sep = data.indexOf(":::");
            if (sep != -1) {
                p.msgId = data.substring(0, sep);
                body = data.substring(sep + 3);
            }

            int replySep = body.indexOf(":::REPLY:::");
            if (replySep != -1) {
                String replyPayload = body.substring(replySep + 11);
                p.realText = body.substring(0, replySep);
                String[] rParts = replyPayload.split(":::");
                if (rParts.length >= 1) p.replyToId = rParts[0];
                if (rParts.length >= 2) p.replyToSender = rParts[1];
                if (rParts.length >= 3) p.replyToText = rParts[2];
            } else {
                p.realText = body;
            }

            return p;
        }
    }

    @SuppressLint("InlinedApi")
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean connectGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false));
                if (connectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    if (pendingRoleAsServer != null) {
                        isServer = pendingRoleAsServer;
                        pendingRoleAsServer = null;
                        startPrimeConnection();
                    }
                }
                loadGalleryMediaAsync();
                loadFilesAsync();
            });

    private boolean checkAndRequestAllAppPermissions() {
        List<String> neededPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        if (!neededPermissions.isEmpty()) {
            requestPermissionLauncher.launch(neededPermissions.toArray(new String[0]));
            return false;
        }
        return true;
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View focusView = getCurrentFocus();
            if (focusView != null) {
                imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            } else if (etMessage != null) {
                imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
                etMessage.clearFocus();
            }
        }
    }

    private final ActivityResultLauncher<String> pickSystemGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    setPendingAttachmentFromUri(uri, isVideoMimeOrPath(uri, getPathFromUri(uri)), "Медиа из галереи");
                }
            });

    private final ActivityResultLauncher<String> pickSystemFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    setPendingAttachmentFromUri(uri, isVideoMimeOrPath(uri, getPathFromUri(uri)), "Файл из менеджера");
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

    private final SharedPreferences.OnSharedPreferenceChangeListener profileChangeListener = (sharedPreferences, key) -> {
        try {
            String currentUser = sharedPreferences.getString("current_user", "");
            if (key != null && (key.equals(currentUser + "_name") || key.equals(currentUser + "_avatar"))) {
                reloadLocalProfileFromSettings();
                if (connectedThread != null && connectedThread.isAlive()) {
                    String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + localUsername + ";version=" + getAppVersionCode(getApplicationContext());
                    connectedThread.sendPacket(TYPE_TEXT, handshake.getBytes(StandardCharsets.UTF_8));
                    sendLocalAvatar();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

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
                if (isAttachmentPanelOpen) {
                    closeAttachmentPanel();
                } else if (isEditMode) {
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
                if (ime.bottom > 0 && isAttachmentPanelOpen) {
                    closeAttachmentPanel();
                }
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
        layoutReplyBar = findViewById(R.id.layoutReplyBar);
        tvReplyBarTitle = findViewById(R.id.tvReplyBarTitle);
        tvReplyBarText = findViewById(R.id.tvReplyBarText);
        btnCloseReplyBar = findViewById(R.id.btnCloseReplyBar);

        layoutSendingProgress = findViewById(R.id.layoutSendingProgress);
        pbSendingProgress = findViewById(R.id.pbSendingProgress);
        tvSendingProgressPercent = findViewById(R.id.tvSendingProgressPercent);

        initAttachmentPanel();

        getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(profileChangeListener);

        if (btnCancelEdit != null) {
            btnCancelEdit.setOnClickListener(v -> exitEditMode());
        }
        if (btnCloseEditBar != null) {
            btnCloseEditBar.setOnClickListener(v -> exitEditMode());
        }
        if (btnCloseReplyBar != null) {
            btnCloseReplyBar.setOnClickListener(v -> cancelReplyMode());
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

        ItemTouchHelper.SimpleCallback swipeToReplyCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private boolean hapticTriggered = false;

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (chatAdapter != null) {
                    chatAdapter.notifyItemChanged(viewHolder.getBindingAdapterPosition());
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.99f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 10f;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    return;
                }

                float density = recyclerView.getContext().getResources().getDisplayMetrics().density;
                float threshold = -65f * density;
                float maxDrag = -120f * density;

                float clampedDx = Math.max(maxDrag, Math.min(0f, dX));

                View itemView = viewHolder.itemView;
                View bubble = itemView.findViewById(R.id.layoutIncomingBubble);
                if (bubble == null) bubble = itemView.findViewById(R.id.layoutOutgoingBubble);
                if (bubble == null) bubble = itemView;

                bubble.setTranslationX(clampedDx);

                if (clampedDx <= threshold && !hapticTriggered) {
                    hapticTriggered = true;
                    recyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } else if (clampedDx > threshold) {
                    hapticTriggered = false;
                }

                if (!isCurrentlyActive && clampedDx <= threshold) {
                    int pos = viewHolder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && chatAdapter != null) {
                        ChatMessage msg = chatAdapter.getMessageAt(pos);
                        if (msg != null) {
                            enterReplyMode(msg);
                        }
                    }
                }

                if (!isCurrentlyActive) {
                    bubble.animate().translationX(0f).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
                }
            }
        };

        new ItemTouchHelper(swipeToReplyCallback).attachToRecyclerView(rvMessages);

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

        if (tvFloatingDate != null) {
            tvFloatingDate.setOnClickListener(v -> {
                if (rvMessages != null && chatAdapter != null) {
                    LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
                    if (lm != null) {
                        int firstPos = lm.findFirstVisibleItemPosition();
                        if (firstPos == RecyclerView.NO_POSITION) {
                            firstPos = 0;
                        }
                        int targetPos = chatAdapter.findDateSectionStartPosition(firstPos);
                        if (targetPos >= 0 && targetPos < chatAdapter.getItemCount()) {
                            lm.scrollToPositionWithOffset(targetPos, 0);

                            long ts = chatAdapter.getMessageTimestamp(targetPos);
                            if (ts > 0) {
                                tvFloatingDate.setText(ChatAdapter.getDateHeaderString(ts));
                                tvFloatingDate.setAlpha(1f);
                                dateHideHandler.removeCallbacks(hideDateRunnable);
                                dateHideHandler.postDelayed(hideDateRunnable, 2200);
                            }
                        }
                    }
                }
            });
        }

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

            @Override
            public void onReplyMessage(ChatMessage message, int position) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    return;
                }
                enterReplyMode(message);
            }

            @Override
            public void onJumpToMessage(String messageId) {
                if (messageId == null || chatAdapter == null) return;
                int pos = chatAdapter.findPositionByMessageId(messageId);
                if (pos != -1) {
                    LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
                    if (lm != null) {
                        lm.scrollToPositionWithOffset(pos, (int) (80 * getResources().getDisplayMetrics().density));
                        rvMessages.postDelayed(() -> chatAdapter.highlightMessageAtPosition(pos), 200);
                    }
                } else {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Исходное сообщение не найдено", null);
                }
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
                        ParsedMessagePayload parsed = ParsedMessagePayload.parse(data);

                        long timestamp = System.currentTimeMillis();
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                        ChatMessage incomingMessage = new ChatMessage(parsed.realText, time, targetUsername, false, null, timestamp, null, parsed.msgId);
                        if (parsed.replyToId != null) {
                            incomingMessage.setReplyToMessageId(parsed.replyToId);
                            incomingMessage.setReplyToSender(parsed.replyToSender);
                            incomingMessage.setReplyToText(parsed.replyToText);
                        }
                        addMessageToUI(incomingMessage);
                        typingResetHandler.removeCallbacks(resetTypingRunnable);
                        resetTypingRunnable.run();
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
                            typingResetHandler.removeCallbacks(resetTypingRunnable);
                            resetTypingRunnable.run();
                        }
                        break;
                    case MESSAGE_READ_FILE:
                        byte[] fileBuf = (byte[]) msg.obj;
                        if (fileBuf != null && msg.arg1 > 0) {
                            ChatMessage fileMsg = parseFileMessageBytes(fileBuf, targetUsername);
                            if (fileMsg != null) {
                                addMessageToUI(fileMsg);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                resetTypingRunnable.run();
                            }
                        }
                        break;
                    case MESSAGE_SEND_PROGRESS:
                        int sendProg = msg.arg1;
                        if (sendProg >= 0 && sendProg < 100) {
                            if (layoutSendingProgress != null) layoutSendingProgress.setVisibility(View.VISIBLE);
                            if (pbSendingProgress != null) pbSendingProgress.setProgress(sendProg);
                            if (tvSendingProgressPercent != null) {
                                tvSendingProgressPercent.setText("Передача получателю... " + sendProg + "%");
                            }
                        } else if (sendProg >= 100) {
                            if (layoutSendingProgress != null) layoutSendingProgress.setVisibility(View.VISIBLE);
                            if (pbSendingProgress != null) pbSendingProgress.setProgress(100);
                            if (tvSendingProgressPercent != null) {
                                tvSendingProgressPercent.setText("Доставлено получателю ✓");
                            }
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (layoutSendingProgress != null) layoutSendingProgress.setVisibility(View.GONE);
                            }, 1800);
                        } else {
                            if (layoutSendingProgress != null) layoutSendingProgress.setVisibility(View.GONE);
                            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Ошибка передачи получателю", null);
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
                        setOnlineStatusIndicator(true);
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
                        updateConnectionStateInAdapter();
                        break;
                    case MESSAGE_TYPING:
                        String stateStr = (msg.obj instanceof String) ? (String) msg.obj : null;
                        if (stateStr != null && stateStr.startsWith("STATE:")) {
                            String action = stateStr.substring(6);
                            currentActivityState = action;

                            if ("VIEWING_PHOTO".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит фото", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "VIEWING_PHOTO");
                            } else if ("SENDING_PHOTO".equalsIgnoreCase(action) || "SENDING_VIDEO".equalsIgnoreCase(action) || "SENDING_MEDIA".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Отправка медиа", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "SENDING_MEDIA");
                            } else if ("VIEWING_VIDEO".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит видео", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "VIEWING_VIDEO");
                            } else if ("VIEWING_FILE".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит файл", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "VIEWING_FILE");
                            } else if ("SENDING_FILE".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Отправка файла", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "SENDING_FILE");
                            } else if ("IDLE".equalsIgnoreCase(action)) {
                                currentActivityState = "IDLE";
                                stopAnimatingStatus();
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "IDLE");
                                resetTypingRunnable.run();
                            } else if ("TYPING".equalsIgnoreCase(action)) {
                                if (!"VIEWING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_VIDEO".equalsIgnoreCase(currentActivityState) && !"SENDING_FILE".equalsIgnoreCase(currentActivityState) && !"SENDING_MEDIA".equalsIgnoreCase(currentActivityState)) {
                                    startAnimatingStatus("Печатает", R.color.prime_success);
                                    typingResetHandler.removeCallbacks(resetTypingRunnable);
                                    typingResetHandler.postDelayed(resetTypingRunnable, 3000);
                                    saveActivityStateToChatList(targetUsername, "TYPING");
                                }
                            }
                        } else {
                            if (!"VIEWING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_PHOTO".equalsIgnoreCase(currentActivityState) && !"SENDING_VIDEO".equalsIgnoreCase(currentActivityState) && !"SENDING_FILE".equalsIgnoreCase(currentActivityState) && !"SENDING_MEDIA".equalsIgnoreCase(currentActivityState)) {
                                startAnimatingStatus("Печатает", R.color.prime_success);
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
                        BluetoothSocketHolder.clearSocket();
                        PrimeBluetoothService.stopService(ChatPersonActivity.this);
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, deletedByName, deviceAddress);
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, targetUsername, deviceAddress);
                        deleteChatFromChatListEx(deletedByName);
                        deleteChatFromChatListEx(targetUsername);
                        new Handler(Looper.getMainLooper()).postDelayed(ChatPersonActivity.this::finish, 500L);
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
                        setStatusWithAnimation("Отключено", R.color.prime_danger);
                        setOnlineStatusIndicator(false);
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, targetUsername + " отключил(а) соединение", null);
                        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
                        if (layoutInput != null) layoutInput.setVisibility(View.GONE);
                        if (btnPrimeConnect != null) {
                            btnPrimeConnect.setEnabled(true);
                            btnPrimeConnect.setText("⚡ Соединиться");
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!isFinishing()) {
                                finish();
                            }
                        }, 1200L);
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
                                        m.setReactionForUser(authorLogin, finalReaction);
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

            if (currentPendingAttachment != null) {
                PendingAttachment pending = currentPendingAttachment;
                clearPendingAttachment();
                etMessage.setText("");

                long timestamp = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));

                if (pending.isVideo || pending.isFile) {
                    if (pending.size > 2 * 1024 * 1024 * 1024L) {
                        PrimeNotification.INSTANCE.show(this, "Превышен лимит размера файла (до 2 ГБ)", null);
                        return;
                    }
                    sendVideoOrFile(pending, text);
                } else { // Photo
                    sendPhoto(pending.uri, text);
                }
                closeAttachmentPanel();
                return;
            }

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
                senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
                boolean hasText = s != null && s.toString().trim().length() > 0;
                if (!hasText) {
                    if ("TYPING".equalsIgnoreCase(myLocalActivityState)) {
                        sendActivityState("IDLE");
                    }
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastTypingSentTime > 1500 && connectedThread != null && connectedThread.isAlive()) {
                        lastTypingSentTime = now;
                        sendActivityState("TYPING");
                    }
                    senderTypingHandler.postDelayed(stopSenderTypingRunnable, 2500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
            Object threadObj = BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername);
            if (threadObj instanceof ConnectedThread) {
                ConnectedThread existingThread = (ConnectedThread) threadObj;
                if (existingThread.isAlive()) {
                    this.connectedThread = existingThread;
                    this.connectedThread.setUiHandler(handler);
                    
                    BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress);
                    BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

                    if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                    if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                    setOnlineStatusIndicator(true);
                    if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                        setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
                    } else {
                        setStatusWithAnimation("В сети", R.color.prime_success);
                    }
                    sendLocalAvatar();
                    flushPendingMessages();
                }
            }
        } else if (useExistingSocket && BluetoothSocketHolder.getSocketFor(deviceAddress, targetUsername) != null && BluetoothSocketHolder.getSocketFor(deviceAddress, targetUsername).isConnected()) {
            BluetoothSocket existingSocket = BluetoothSocketHolder.getSocketFor(deviceAddress, targetUsername);
            if (BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername) instanceof ConnectedThread) {
                ConnectedThread existingThread = (ConnectedThread) BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername);
                if (existingThread.isAlive()) {
                    this.connectedThread = existingThread;
                    this.connectedThread.setUiHandler(handler);
                    
                    BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress);
                    BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

                    if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                    if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                    setOnlineStatusIndicator(true);
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
        } else if (this.connectedThread != null && this.connectedThread.isAlive()) {
            this.connectedThread.setUiHandler(handler);
        }

        if (connectedThread == null || !connectedThread.isAlive()) {
            connectionRetryCount = 0;
            autoRetryHandler.removeCallbacks(autoRetryRunnable);
            autoRetryHandler.postDelayed(this::startPrimeConnection, 500L);
        }

        reloadLocalProfileFromSettings();

        if (connectedThread != null && connectedThread.isAlive()) {
            sendLocalAvatar();
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
        updateConnectionStateInAdapter();

        if (targetUsername != null && chatAdapter != null) {
            int oldSize = chatAdapter.getItemCount();
            List<ChatMessage> history = ChatHistoryManager.loadMessages(this, targetUsername);
            if (!history.isEmpty() || oldSize == 0) {
                chatAdapter.setMessages(history);
                if (history.size() > oldSize && history.size() > 0) {
                    rvMessages.scrollToPosition(history.size() - 1);
                }
            }
        }
    }

    private void reloadLocalProfileFromSettings() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPreferences.getString("current_user", "");
            String myDisplayName = sharedPreferences.getString(currentUser + "_name", currentUser);
            localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";
            if (chatAdapter != null) {
                chatAdapter.setLocalUsername(localUsername);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reload local profile from settings", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {}

        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        if ("TYPING".equalsIgnoreCase(myLocalActivityState)) {
            sendActivityState("IDLE");
        }

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

        String msgId = message.getMessageId();

        // If local user already placed this exact reaction, toggle it off
        String existingReaction = message.getReactionForUser(localUsername);
        if (reaction != null && reaction.equalsIgnoreCase(existingReaction)) {
            reaction = null;
        }

        message.setReactionForUser(localUsername, reaction);
        chatAdapter.updateMessageReactionById(msgId, reaction, localUsername);

        if (connectedThread != null && connectedThread.isAlive()) {
            String payloadStr = msgId + ":::" + (reaction != null ? reaction : "REMOVE") + ":::" + localUsername;
            connectedThread.sendPacket(TYPE_REACTION, payloadStr.getBytes(StandardCharsets.UTF_8));
        }

        ChatHistoryManager.saveMessage(this, targetUsername, message);
    }

    private void updateConnectionStateInAdapter() {
        if (chatAdapter != null) {
            boolean active = isRemoteUserOnline && connectedThread != null && connectedThread.isAlive();
            chatAdapter.setConnectionActive(active);
        }
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
        setStatusWithAnimation("Отключено", R.color.prime_danger);
        setOnlineStatusIndicator(false);
        updateConnectionStateInAdapter();

        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
        if (layoutInput != null) layoutInput.setVisibility(View.GONE);
        if (btnPrimeConnect != null) {
            btnPrimeConnect.setEnabled(true);
            btnPrimeConnect.setText("⚡ Соединиться");
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

    private void enterReplyMode(ChatMessage message) {
        if (message == null) return;
        replyingToMessage = message;

        if (isEditMode) {
            exitEditMode();
        }

        if (layoutReplyBar != null) {
            layoutReplyBar.setVisibility(View.VISIBLE);
            layoutReplyBar.setTranslationY(-30f);
            layoutReplyBar.setAlpha(0f);
            layoutReplyBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(this::updateMessageListPadding)
                    .start();
        }

        if (tvReplyBarTitle != null) {
            String sender = message.getSenderLogin();
            tvReplyBarTitle.setText("Ответ: " + (sender != null && !sender.isEmpty() ? sender : "Пользователю"));
        }

        if (tvReplyBarText != null) {
            String text = message.getText();
            tvReplyBarText.setText(text != null && !text.isEmpty() ? text : "Фотография");
        }

        if (etMessage != null) {
            etMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        }
        updateMessageListPadding();
    }

    private void cancelReplyMode() {
        replyingToMessage = null;
        if (layoutReplyBar != null && layoutReplyBar.getVisibility() == View.VISIBLE) {
            layoutReplyBar.animate()
                    .translationY(-30f)
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        layoutReplyBar.setVisibility(View.GONE);
                        layoutReplyBar.setTranslationY(0f);
                        layoutReplyBar.setAlpha(1f);
                        updateMessageListPadding();
                    })
                    .start();
        } else {
            updateMessageListPadding();
        }
    }

    private void updateMessageListPadding() {
        View chatRoot = findViewById(R.id.chatRoot);
        View rvMessages = findViewById(R.id.rvMessages);
        View bottomContainer = findViewById(R.id.bottomContainer);
        if (rvMessages == null || chatRoot == null) return;

        float density = getResources().getDisplayMetrics().density;

        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(chatRoot);
        int topInset = (int) (24 * density);
        int bottomInset = (int) (16 * density);
        if (rootInsets != null) {
            Insets sb = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = rootInsets.getInsets(WindowInsetsCompat.Type.ime());
            topInset = sb.top;
            bottomInset = Math.max(sb.bottom, ime.bottom);
        }

        int topPadding = topInset + (int) (76 * density);
        int bottomPadding = bottomInset + (int) (72 * density);
        if (bottomContainer != null && bottomContainer.getHeight() > 0) {
            bottomPadding = bottomContainer.getHeight() + bottomInset + (int) (16 * density);
        }

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

    private void setOnlineStatusIndicator(boolean connected) {
        View viewStatus = findViewById(R.id.viewOnlineStatus);
        if (viewStatus == null) return;
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.OVAL);
        
        if (connected) {
            viewStatus.setVisibility(View.VISIBLE);
            badge.setColor(ContextCompat.getColor(this, R.color.prime_success));
        } else {
            viewStatus.setVisibility(View.VISIBLE);
            badge.setColor(ContextCompat.getColor(this, R.color.prime_danger));
        }
        viewStatus.setBackground(badge);
    }

    private final Handler statusAnimHandler = new Handler(Looper.getMainLooper());
    private int statusDotCount = 0;
    private String currentBaseStatus = "";
    private int currentStatusColor = Color.parseColor("#4CAF50");

    private final Runnable statusAnimRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentBaseStatus == null || currentBaseStatus.isEmpty()) return;
            statusDotCount = (statusDotCount + 1) % 4; // 0: "", 1: ".", 2: "..", 3: "..."
            StringBuilder sb = new StringBuilder(currentBaseStatus);
            for (int i = 0; i < statusDotCount; i++) {
                sb.append(".");
            }
            setStatusTextDirect(sb.toString(), currentStatusColor);
            statusAnimHandler.postDelayed(this, 500);
        }
    };

    private void setStatusTextDirect(String newText, int colorResOrValue) {
        if (tvChatStatus == null) return;
        int finalColor;
        try {
            finalColor = ContextCompat.getColor(this, colorResOrValue);
        } catch (Exception e) {
            finalColor = colorResOrValue;
        }
        tvChatStatus.setText(newText);
        tvChatStatus.setTextColor(finalColor);
    }

    private void startAnimatingStatus(String baseStatus, int colorResOrValue) {
        statusAnimHandler.removeCallbacks(statusAnimRunnable);
        currentBaseStatus = baseStatus;
        try {
            currentStatusColor = ContextCompat.getColor(this, colorResOrValue);
        } catch (Exception e) {
            currentStatusColor = colorResOrValue;
        }
        statusDotCount = 0;
        setStatusWithAnimation(baseStatus, currentStatusColor);
        statusAnimHandler.postDelayed(statusAnimRunnable, 500);
    }

    private void stopAnimatingStatus() {
        statusAnimHandler.removeCallbacks(statusAnimRunnable);
        currentBaseStatus = "";
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
        File localAvatarFileId = deviceAddress != null ? new File(getFilesDir(), "avatar_" + deviceAddress + ".jpg") : null;
        File targetAvatarFile = localAvatarFile.exists() ? localAvatarFile : (localAvatarFileId != null && localAvatarFileId.exists() ? localAvatarFileId : null);

        if (targetAvatarFile != null && targetAvatarFile.exists()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(targetAvatarFile.getAbsolutePath());
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
                    isChatDeleted = true;
                    if (connectedThread != null && connectedThread.isAlive()) {
                        String deletionPayload = "DELETE_CHAT:login=" + localUsername + ";name=" + localUsername;
                        connectedThread.sendPacket(TYPE_CHAT_DELETED, deletionPayload.getBytes(StandardCharsets.UTF_8));
                        try { Thread.sleep(100); } catch (Exception ignored) {}
                    }
                    BluetoothSocketHolder.clearSocket();
                    PrimeBluetoothService.stopService(this);
                    ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
                    if (chatAdapter != null) chatAdapter.setMessages(new ArrayList<>());
                    disconnectCurrentChat();
                    Toast.makeText(this, "Переписка удалена", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteChatFromChatList(String targetName) {
        deleteChatFromChatListEx(targetName);
    }

    private void deleteChatFromChatListEx(String nameOrId) {
        isChatDeleted = true;
        BluetoothSocketHolder.clearSocket();
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
            if (remoteAvatarUri == null || remoteAvatarUri.isEmpty()) {
                File fName = new File(getFilesDir(), "avatar_" + targetUsername + ".jpg");
                File fAddr = deviceAddress != null ? new File(getFilesDir(), "avatar_" + deviceAddress + ".jpg") : null;
                if (fName.exists()) {
                    remoteAvatarUri = Uri.fromFile(fName).toString();
                } else if (fAddr != null && fAddr.exists()) {
                    remoteAvatarUri = Uri.fromFile(fAddr).toString();
                }
            }

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
        if (isChatDeleted || targetName == null || targetName.isEmpty()) return;
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
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");
        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        ChatMessage message = new ChatMessage(text, time, localUsername, true, null, timestamp, null, null);
        
        if (replyingToMessage != null) {
            message.setReplyToMessageId(replyingToMessage.getMessageId());
            message.setReplyToSender(replyingToMessage.getSenderLogin());
            String qText = replyingToMessage.getText();
            message.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Фотография");
            cancelReplyMode();
        }

        String packetContent = message.getMessageId() + ":::" + text;
        if (message.isReply()) {
            packetContent += ":::REPLY:::" + message.getReplyToMessageId() + ":::" + message.getReplyToSender() + ":::" + message.getReplyToText();
        }

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
        sendPhoto(uri, null);
    }

    private void sendPhoto(Uri uri, String captionText) {
        if (uri == null) return;
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");
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
                
                ChatMessage photoMsg = new ChatMessage(captionText, time, localUsername, true, scaledBitmap, timestamp, null, messageId);
                photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
                if (replyingToMessage != null) {
                    photoMsg.setReplyToMessageId(replyingToMessage.getMessageId());
                    photoMsg.setReplyToSender(replyingToMessage.getSenderLogin());
                    String qText = replyingToMessage.getText();
                    photoMsg.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Фотография");
                    cancelReplyMode();
                }
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
        if (connectedThread == null || !connectedThread.isAlive()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPrefs.getString("current_user", "");
            String localAvatarUri = sharedPrefs.getString(currentUser + "_avatar", "");

            if (localAvatarUri == null || localAvatarUri.isEmpty()) {
                File f = new File(getFilesDir(), "avatar_" + currentUser + ".jpg");
                if (f.exists()) {
                    localAvatarUri = Uri.fromFile(f).toString();
                }
            }

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

    private boolean isValidMacAddress(String address) {
        if (address == null || address.isEmpty() || "null".equalsIgnoreCase(address)) return false;
        try {
            return BluetoothAdapter.checkBluetoothAddress(address.toUpperCase(Locale.US));
        } catch (Exception e) {
            return false;
        }
    }

    private String findMacForTargetUsername(String targetName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return null;
        try {
            @SuppressLint("MissingPermission")
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded != null) {
                for (BluetoothDevice dev : bonded) {
                    @SuppressLint("MissingPermission")
                    String devName = dev.getName();
                    if (devName != null && (devName.equalsIgnoreCase(targetName) || devName.contains("Prime"))) {
                        return dev.getAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to find MAC for target username", e);
        }
        return null;
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
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, 30000L);

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
        String addressToConnect = deviceAddress;
        if (!isValidMacAddress(addressToConnect)) {
            addressToConnect = findMacForTargetUsername(targetUsername);
        }

        if (isValidMacAddress(addressToConnect)) {
            final String targetMac = addressToConnect;
            String myAddress = "";
            try {
                myAddress = bluetoothAdapter.getAddress();
            } catch (SecurityException ignored) {}

            boolean isPrimaryInitiator = myAddress != null && myAddress.compareToIgnoreCase(targetMac) > 0;
            long delayMs = isPrimaryInitiator ? 200 : 1000;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if ((connectedThread == null || !connectedThread.isAlive()) && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    try {
                        bluetoothAdapter.cancelDiscovery();
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(targetMac);
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
        connectionRetryCount = 0;
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
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
            connectedThread.setUiHandler(handler);
            connectedThread.start();
        }

        String resolvedAddress = deviceAddress != null ? deviceAddress : (device != null ? device.getAddress() : null);
        BluetoothSocketHolder.registerConnection(resolvedAddress, targetUsername, socket, connectedThread);
        BluetoothSocketHolder.setActiveDeviceAddress(resolvedAddress);
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
            setOnlineStatusIndicator(true);
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
            if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
        });
    }

    private void connectionFailed() {
        runOnUiThread(() -> {
            if (btnPrimeConnect != null) {
                btnPrimeConnect.setEnabled(true);
                btnPrimeConnect.setText("⚡ Соединиться");
            }
            updateOfflineLastSeenStatus();
            setOnlineStatusIndicator(false);
        });

        if (connectionRetryCount < MAX_AUTO_RETRIES) {
            connectionRetryCount++;
            autoRetryHandler.removeCallbacks(autoRetryRunnable);
            autoRetryHandler.postDelayed(autoRetryRunnable, 3000L);
        } else {
            PrimeNotification.INSTANCE.show(this, "Не удалось подключиться", null);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    finish();
                }
            }, 1000L);
        }
    }

    private void connectionLost() {
        BluetoothSocketHolder.clearSocket();
        isHandshakeDone = false;
        isRemoteUserOnline = false;

        runOnUiThread(() -> {
            saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
            setStatusWithAnimation("Отключено", R.color.prime_danger);
            setOnlineStatusIndicator(false);
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
            if (layoutInput != null) layoutInput.setVisibility(View.GONE);
            if (btnPrimeConnect != null) {
                btnPrimeConnect.setEnabled(true);
                btnPrimeConnect.setText("⚡ Соединиться");
            }
        });

        if (!isOpeningSubActivity && !isChatDeleted) {
            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Соединение разорвано", null);
        }
        
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }

        if (connectionRetryCount < MAX_AUTO_RETRIES) {
            connectionRetryCount++;
            autoRetryHandler.removeCallbacks(autoRetryRunnable);
            autoRetryHandler.postDelayed(autoRetryRunnable, 3000L);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    finish();
                }
            }, 1200L);
        }
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
        try {
            getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(profileChangeListener);
        } catch (Exception ignored) {}

        if (senderTypingHandler != null) senderTypingHandler.removeCallbacksAndMessages(null);
        if (lastSeenHandler != null) lastSeenHandler.removeCallbacksAndMessages(null);
        if (typingResetHandler != null) typingResetHandler.removeCallbacksAndMessages(null);
        if (dateHideHandler != null) dateHideHandler.removeCallbacksAndMessages(null);
        if (connectTimeoutHandler != null) connectTimeoutHandler.removeCallbacksAndMessages(null);
        if (deletionHandler != null) deletionHandler.removeCallbacksAndMessages(null);
        if (statusAnimHandler != null) statusAnimHandler.removeCallbacksAndMessages(null);

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
            if (h == null && handler != null) {
                h = handler;
            }
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
                    if (mmSocket == null || !mmSocket.isConnected()) {
                        keepAliveHandler.removeCallbacks(this);
                        return;
                    }
                    sendPacket(TYPE_PING, new byte[0]);
                    keepAliveHandler.postDelayed(this, 10000); // 10s ping is less aggressive
                }
            };
            keepAliveHandler.postDelayed(keepAliveRunnable, 10000);

            while (true) {
                try {
                    byte type = mmInStream.readByte();
                    int length = mmInStream.readInt();
                    
                    if (length < 0 || length > 100 * 1024 * 1024) {
                        throw new IOException("Invalid packet length: " + length);
                    }

                    byte[] payload = null;
                    if (length > 0) {
                        payload = new byte[length];
                        mmInStream.readFully(payload);
                    }

                    if (isChatDeleted && type != TYPE_CHAT_DELETED) {
                        continue;
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
                            
                            if (!remoteName.isEmpty() && !remoteName.equals(targetUsername)) {
                                String oldTarget = targetUsername;
                                targetUsername = remoteName;
                                try {
                                    getIntent().putExtra("EXTRA_CHAT_NAME", remoteName);
                                } catch (Exception ignored) {}
                                
                                // Update BluetoothSocketHolder
                                BluetoothSocketHolder.setActiveTargetUsername(remoteName);
                                BluetoothSocketHolder.registerConnection(deviceAddress, remoteName, mmSocket, this);
                                
                                // Migrate history
                                migrateHistoryIfNeeded(oldTarget, remoteName);
                                if (deviceAddress != null) {
                                    migrateHistoryIfNeeded(deviceAddress, remoteName);
                                }
                                
                                // Update persisted_chats OLD name to NEW name
                                try {
                                    SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                                    String jsonChats = sharedPrefs.getString("persisted_chats", "[]");
                                    JSONArray chatArray = new JSONArray(jsonChats);
                                    for (int i = 0; i < chatArray.length(); i++) {
                                        JSONObject obj = chatArray.getJSONObject(i);
                                        if (oldTarget.equalsIgnoreCase(obj.optString("name")) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(obj.optString("id")))) {
                                            obj.put("name", remoteName);
                                            break;
                                        }
                                    }
                                    sharedPrefs.edit().putString("persisted_chats", chatArray.toString()).commit();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to rename chat in persisted_chats", e);
                                }

                                final String finalName = remoteName;

                                saveLastMessageToChatList(null, null, false, "ONLINE");

                                runOnUiThread(() -> {
                                    if (tvChatName != null) tvChatName.setText(finalName);
                                    updateAvatarUi(remoteAvatarUri, finalName);
                                    List<ChatMessage> updatedHistory = ChatHistoryManager.loadMessages(ChatPersonActivity.this, finalName);
                                    if (chatAdapter != null && !updatedHistory.isEmpty()) {
                                        chatAdapter.setMessages(updatedHistory);
                                        if (chatAdapter.getItemCount() > 0) {
                                            rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                                        }
                                    }
                                });

                                postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
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
                    } else if (type == TYPE_FILE && payload != null) {
                        sendPacket(TYPE_ACK, new byte[0]);
                        String fileMetaStr = new String(payload, 0, Math.min(payload.length, 500), StandardCharsets.UTF_8);
                        String fileMsgId = extractMsgId(fileMetaStr);
                        if (fileMsgId != null && !fileMsgId.isEmpty()) {
                            sendPacket(TYPE_READ_RECEIPT, fileMsgId.getBytes(StandardCharsets.UTF_8));
                        }

                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_READ_FILE, payload.length, -1, payload);
                            String lowerData = fileMetaStr.toLowerCase();
                            boolean isVideo = lowerData.contains(".mp4") || lowerData.contains(".mkv") || lowerData.contains(".3gp") || lowerData.contains(".webm") || lowerData.contains(":::duration:::");
                            saveLastMessageToChatList(isVideo ? "Видео" : "Файл", MessageStatus.READ, false, "ONLINE");
                            saveActivityStateToChatList(targetUsername, isVideo ? "VIEWING_VIDEO" : "VIEWING_FILE");
                        } else {
                            processBackgroundFileMessage(payload);
                        }
                    } else if (type == TYPE_PROFILE_UPDATE && payload != null) {
                        String receivedData = new String(payload, StandardCharsets.UTF_8);
                        if (receivedData.startsWith("HANDSHAKE:")) {
                            String data = receivedData.substring(10).trim();
                            String remoteName = targetUsername;
                            if (data.contains("login=") || data.contains("name=")) {
                                String[] parts = data.split(";");
                                for (String p : parts) {
                                    if (p.startsWith("name=")) remoteName = p.substring(5);
                                }
                            } else {
                                remoteName = data;
                            }
                            
                            if (!remoteName.isEmpty() && !remoteName.equals(targetUsername)) {
                                String oldTarget = targetUsername;
                                targetUsername = remoteName;
                                try {
                                    getIntent().putExtra("EXTRA_CHAT_NAME", remoteName);
                                } catch (Exception ignored) {}
                                
                                BluetoothSocketHolder.setActiveTargetUsername(remoteName);
                                BluetoothSocketHolder.registerConnection(deviceAddress, remoteName, mmSocket, this);
                                
                                migrateHistoryIfNeeded(oldTarget, remoteName);
                                if (deviceAddress != null) {
                                    migrateHistoryIfNeeded(deviceAddress, remoteName);
                                }
                                
                                try {
                                    SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                                    String jsonChats = sharedPrefs.getString("persisted_chats", "[]");
                                    JSONArray chatArray = new JSONArray(jsonChats);
                                    for (int i = 0; i < chatArray.length(); i++) {
                                        JSONObject obj = chatArray.getJSONObject(i);
                                        if (oldTarget.equalsIgnoreCase(obj.optString("name")) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(obj.optString("id")))) {
                                            obj.put("name", remoteName);
                                            break;
                                        }
                                    }
                                    sharedPrefs.edit().putString("persisted_chats", chatArray.toString()).commit();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to rename chat in persisted_chats", e);
                                }

                                final String finalName = remoteName;
                                saveLastMessageToChatList(null);

                                if (activeUiHandler != null) {
                                    activeUiHandler.post(() -> {
                                        if (tvChatName != null) tvChatName.setText(finalName);
                                        updateAvatarUi(remoteAvatarUri, finalName);
                                        List<ChatMessage> updatedHistory = ChatHistoryManager.loadMessages(ChatPersonActivity.this, finalName);
                                        if (chatAdapter != null && !updatedHistory.isEmpty()) {
                                            chatAdapter.setMessages(updatedHistory);
                                            if (chatAdapter.getItemCount() > 0) {
                                                rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    } else if (type == TYPE_AVATAR) {
                        try {
                            SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                            String currUser = sp.getString("current_user", "");
                            if (targetUsername != null && !targetUsername.isEmpty() && !targetUsername.equalsIgnoreCase(currUser)) {
                                if (payload == null || payload.length == 0) {
                                    File avatarFile = new File(getFilesDir(), "avatar_" + targetUsername + ".jpg");
                                    if (avatarFile.exists()) {
                                        boolean ignoredDel = avatarFile.delete();
                                    }
                                    remoteAvatarUri = null;
                                    saveLastMessageToChatList(null);
                                    
                                    if (activeUiHandler != null) {
                                        activeUiHandler.post(() -> {
                                            updateAvatarUi(null, targetUsername);
                                        });
                                    }
                                } else {
                                    Bitmap avatarBmp = BitmapFactory.decodeByteArray(payload, 0, payload.length);
                                    if (avatarBmp != null) {
                                        File avatarFile = new File(getFilesDir(), "avatar_" + targetUsername + ".jpg");
                                        FileOutputStream fos = new FileOutputStream(avatarFile);
                                        avatarBmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                                        fos.flush();
                                        fos.close();
                                        
                                        remoteAvatarUri = Uri.fromFile(avatarFile).toString();
                                        saveLastMessageToChatList(null);
                                        
                                        if (activeUiHandler != null) {
                                            activeUiHandler.post(() -> {
                                                ImageView ivAvatar = findViewById(R.id.ivChatAvatar);
                                                if (ivAvatar != null) {
                                                    ivAvatar.setImageBitmap(avatarBmp);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to process background avatar", e);
                        }
                    } else if (type == TYPE_TYPING) {
                        String stateData = (payload != null && payload.length > 0) ? new String(payload, StandardCharsets.UTF_8) : "STATE:TYPING";
                        postToUi(MESSAGE_TYPING, -1, -1, stateData);
                        if (stateData.startsWith("STATE:")) {
                            String action = stateData.substring(6);
                            saveActivityStateToChatList(targetUsername, action);
                        } else if (stateData.contains("TYPING")) {
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
                        BluetoothSocketHolder.clearSocket();
                        PrimeBluetoothService.stopService(getApplicationContext());
                        ChatHistoryManager.deleteHistoryCompletely(getApplicationContext(), targetUsername, deviceAddress);
                        deleteChatFromChatList(targetUsername);
                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_CHAT_DELETED, -1, -1, null);
                        } else {
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
            return ParsedMessagePayload.parse(data).realText;
        }

        private void processBackgroundTextMessage(String receivedData) {
            if (receivedData == null || receivedData.startsWith("HANDSHAKE:")) return;
            ParsedMessagePayload parsed = ParsedMessagePayload.parse(receivedData);
            if (parsed.realText.startsWith("HANDSHAKE:")) return;

            long timestamp = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            ChatMessage incomingMessage = new ChatMessage(parsed.realText, time, targetUsername, false, null, timestamp, null, parsed.msgId);
            if (parsed.replyToId != null) {
                incomingMessage.setReplyToMessageId(parsed.replyToId);
                incomingMessage.setReplyToSender(parsed.replyToSender);
                incomingMessage.setReplyToText(parsed.replyToText);
            }
            
            ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, incomingMessage);
            saveLastMessageToChatList(parsed.realText, MessageStatus.NONE, true, "ONLINE");

            Handler uiH = activeUiHandler;
            if (uiH != null) {
                uiH.post(() -> addMessageToUI(incomingMessage));
            } else {
                showBackgroundNotification(targetUsername, parsed.realText);
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

        private void processBackgroundFileMessage(byte[] fullPayload) {
            if (fullPayload == null) return;
            ChatMessage fileMsg = parseFileMessageBytes(fullPayload, targetUsername);
            if (fileMsg == null) return;
            ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, fileMsg);
            
            String desc = (fileMsg.isVideo() ? "Видео: " : "Файл: ") + (fileMsg.getFileName() != null ? fileMsg.getFileName() : "Файл");
            saveLastMessageToChatList(desc, MessageStatus.NONE, true, "ONLINE");

            Handler uiH = activeUiHandler;
            if (uiH != null) {
                uiH.post(() -> addMessageToUI(fileMsg));
            } else {
                showBackgroundNotification(targetUsername, desc);
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
                            if ((type == TYPE_PHOTO || type == TYPE_FILE) && length > 1024) {
                                int progress = (int) ((offset * 100L) / length);
                                postToUi(MESSAGE_SEND_PROGRESS, progress, -1, null);
                            }
                        }
                    }
                    mmOutStream.flush();
                    if (type == TYPE_PHOTO || type == TYPE_FILE) {
                        postToUi(MESSAGE_SEND_PROGRESS, 100, -1, null);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                    if (type == TYPE_PHOTO || type == TYPE_FILE) {
                        postToUi(MESSAGE_SEND_PROGRESS, -1, -1, null);
                    }
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

    private byte[] getBytesFromPending(PendingAttachment pending) {
        if (pending == null) return new byte[0];
        try {
            if (pending.path != null && new File(pending.path).exists()) {
                File f = new File(pending.path);
                FileInputStream fis = new FileInputStream(f);
                byte[] data = new byte[(int) Math.min(f.length(), 100 * 1024 * 1024L)];
                int read = fis.read(data);
                fis.close();
                if (read > 0) {
                    if (read < data.length) {
                        byte[] trimmed = new byte[read];
                        System.arraycopy(data, 0, trimmed, 0, read);
                        return trimmed;
                    }
                    return data;
                }
            } else if (pending.uri != null) {
                InputStream is = getContentResolver().openInputStream(pending.uri);
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    long totalRead = 0;
                    while ((len = is.read(buf)) != -1 && totalRead < 100 * 1024 * 1024L) {
                        baos.write(buf, 0, len);
                        totalRead += len;
                    }
                    is.close();
                    return baos.toByteArray();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read pending file bytes", e);
        }
        return new byte[0];
    }

    private ChatMessage parseFileMessageBytes(byte[] fullPayload, String sender) {
        if (fullPayload == null || fullPayload.length == 0) return null;

        int headerEndIdx = -1;
        String headerEndTag = ":::HEADER_END:::";
        byte[] tagBytes = headerEndTag.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i <= fullPayload.length - tagBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < tagBytes.length; j++) {
                if (fullPayload[i + j] != tagBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                headerEndIdx = i;
                break;
            }
        }

        String headerStr = "";
        byte[] fileDataBytes = new byte[0];

        if (headerEndIdx != -1) {
            headerStr = new String(fullPayload, 0, headerEndIdx, StandardCharsets.UTF_8);
            int bodyStart = headerEndIdx + tagBytes.length;
            int bodyLen = fullPayload.length - bodyStart;
            if (bodyLen > 0) {
                fileDataBytes = new byte[bodyLen];
                System.arraycopy(fullPayload, bodyStart, fileDataBytes, 0, bodyLen);
            }
        } else {
            headerStr = new String(fullPayload, StandardCharsets.UTF_8);
        }

        String msgId = null;
        String fileName = "Файл";
        long fileSize = 0L;
        String text = "";
        String videoDuration = null;
        String replyToId = null, replyToSender = null, replyToText = null;

        int replySep = headerStr.indexOf(":::REPLY:::");
        if (replySep != -1) {
            String replyPart = headerStr.substring(replySep + 11);
            headerStr = headerStr.substring(0, replySep);
            String[] rParts = replyPart.split(":::");
            if (rParts.length >= 1) replyToId = rParts[0];
            if (rParts.length >= 2) replyToSender = rParts[1];
            if (rParts.length >= 3) replyToText = rParts[2];
        }

        int durSep = headerStr.indexOf(":::DURATION:::");
        if (durSep != -1) {
            videoDuration = headerStr.substring(durSep + 14);
            headerStr = headerStr.substring(0, durSep);
        }

        String[] parts = headerStr.split(":::");
        if (parts.length >= 1) msgId = parts[0];
        if (parts.length >= 2) fileName = parts[1];
        if (parts.length >= 3) {
            try { fileSize = Long.parseLong(parts[2]); } catch (Exception ignored) {}
        }
        if (parts.length >= 4) text = parts[3];

        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));

        String localSavedPath = null;
        if (fileDataBytes.length > 0) {
            try {
                File localFile = new File(getFilesDir(), "rec_file_" + (msgId != null ? msgId : timestamp) + "_" + fileName);
                FileOutputStream fos = new FileOutputStream(localFile);
                fos.write(fileDataBytes);
                fos.flush();
                fos.close();
                localSavedPath = localFile.getAbsolutePath();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save received file to disk", e);
            }
        }

        ChatMessage msg = new ChatMessage(text, time, sender, false, null, timestamp, localSavedPath, msgId);

        String lowerName = fileName.toLowerCase();
        boolean isVideo = (videoDuration != null && !videoDuration.equals("00:00")) || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") || lowerName.endsWith(".webm");

        if (isVideo) {
            msg.setMessageType(ChatMessage.MessageType.VIDEO);
            msg.setVideoDuration(videoDuration != null ? videoDuration : "00:00");
            if (localSavedPath != null) {
                Bitmap thumb = getVideoThumbnail(localSavedPath);
                if (thumb != null) msg.setImageBitmap(thumb);
            }
        } else {
            msg.setMessageType(ChatMessage.MessageType.FILE);
        }

        msg.setFileName(fileName);
        msg.setFileSize(fileSize > 0 ? fileSize : fileDataBytes.length);

        if (replyToId != null) {
            msg.setReplyToMessageId(replyToId);
            msg.setReplyToSender(replyToSender);
            msg.setReplyToText(replyToText);
        }

        return msg;
    }

    private void sendVideoOrFile(PendingAttachment pending, String text) {
        if (pending == null) return;
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");

        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();

        String fileName = pending.name != null ? pending.name : (pending.isVideo ? "video.mp4" : "file.bin");
        long fileSize = pending.size;
        String durStr = pending.durationStr != null ? pending.durationStr : "00:00";

        byte[] fileBytes = getBytesFromPending(pending);

        ChatMessage.MessageType type = pending.isVideo ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.FILE;

        ChatMessage msg = new ChatMessage(text, time, localUsername, true, pending.thumbnail, timestamp, pending.path, messageId);
        msg.setMessageType(type);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize > 0 ? fileSize : fileBytes.length);
        if (pending.isVideo) msg.setVideoDuration(durStr);

        if (replyingToMessage != null) {
            msg.setReplyToMessageId(replyingToMessage.getMessageId());
            msg.setReplyToSender(replyingToMessage.getSenderLogin());
            String qText = replyingToMessage.getText();
            msg.setReplyToText(qText != null && !qText.isEmpty() ? qText : (pending.isVideo ? "Видео" : "Файл"));
            cancelReplyMode();
        }

        String header = messageId + ":::" + fileName + ":::" + msg.getFileSize() + ":::" + (text != null ? text : "") + ":::DURATION:::" + durStr;
        if (msg.isReply()) {
            header += ":::REPLY:::" + msg.getReplyToMessageId() + ":::" + msg.getReplyToSender() + ":::" + msg.getReplyToText();
        }
        header += ":::HEADER_END:::";

        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] fullPayload = new byte[headerBytes.length + fileBytes.length];
        System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
        if (fileBytes.length > 0) {
            System.arraycopy(fileBytes, 0, fullPayload, headerBytes.length, fileBytes.length);
        }

        if (connectedThread != null && connectedThread.isAlive()) {
            connectedThread.sendPacket(TYPE_FILE, fullPayload);
        } else {
            pendingMessageQueue.add(new PendingMessage(TYPE_FILE, fullPayload));
            startPrimeConnection();
        }

        addMessageToUI(msg);
        ChatHistoryManager.saveMessage(this, targetUsername, msg);
        boolean isVid = pending.isVideo;
        saveLastMessageToChatList((isVid ? "Видео: " : "Файл: ") + fileName, MessageStatus.SENT, false, "ONLINE");
        saveActivityStateToChatList(targetUsername, isVid ? "SENDING_VIDEO" : "SENDING_FILE");
        sendActivityState("STATE:" + (isVid ? "SENDING_VIDEO" : "SENDING_FILE"));
        clearPendingAttachment();
    }

    // =========================================================================
    // Attachment Panel & Media/File System
    // =========================================================================

    private static class PendingAttachment {
        Uri uri;
        String path;
        String name;
        long size;
        boolean isVideo;
        boolean isFile;
        Bitmap thumbnail;
        String durationStr;
    }

    private static class MediaItem {
        Uri uri;
        String path;
        boolean isVideo;
        String durationStr;
        long date;
        long size;
        String name;

        MediaItem(Uri uri, String path, boolean isVideo, String durationStr, long date, long size, String name) {
            this.uri = uri;
            this.path = path;
            this.isVideo = isVideo;
            this.durationStr = durationStr;
            this.date = date;
            this.size = size;
            this.name = name;
        }
    }

    private static class FileItem {
        Uri uri;
        String path;
        String name;
        long size;
        long date;

        FileItem(Uri uri, String path, String name, long size, long date) {
            this.uri = uri;
            this.path = path;
            this.name = name;
            this.size = size;
            this.date = date;
        }
    }

    private void initAttachmentPanel() {
        layoutAttachmentPanel = findViewById(R.id.layoutAttachmentPanel);
        layoutPendingAttachment = findViewById(R.id.layoutPendingAttachment);
        ivPendingThumbnail = findViewById(R.id.ivPendingThumbnail);
        ivPendingVideoBadge = findViewById(R.id.ivPendingVideoBadge);
        tvPendingName = findViewById(R.id.tvPendingName);
        tvPendingSize = findViewById(R.id.tvPendingSize);
        btnCancelPending = findViewById(R.id.btnCancelPending);

        layoutModeCamera = findViewById(R.id.layoutModeCamera);
        layoutModePhoto = findViewById(R.id.layoutModePhoto);
        layoutModeFiles = findViewById(R.id.layoutModeFiles);

        vModeCameraBg = findViewById(R.id.vModeCameraBg);
        vModePhotoBg = findViewById(R.id.vModePhotoBg);
        vModeFilesBg = findViewById(R.id.vModeFilesBg);

        ivModeCameraIcon = findViewById(R.id.ivModeCameraIcon);
        ivModePhotoIcon = findViewById(R.id.ivModePhotoIcon);
        ivModeFilesIcon = findViewById(R.id.ivModeFilesIcon);

        tvModeCameraLabel = findViewById(R.id.tvModeCameraLabel);
        tvModePhotoLabel = findViewById(R.id.tvModePhotoLabel);
        tvModeFilesLabel = findViewById(R.id.tvModeFilesLabel);

        layoutSectionCamera = findViewById(R.id.layoutSectionCamera);
        layoutSectionPhoto = findViewById(R.id.layoutSectionPhoto);
        layoutSectionFiles = findViewById(R.id.layoutSectionFiles);

        btnCameraPhoto = findViewById(R.id.btnCameraPhoto);
        btnCameraVideo = findViewById(R.id.btnCameraVideo);

        rvGalleryGrid = findViewById(R.id.rvGalleryGrid);
        rvFilesGrid = findViewById(R.id.rvFilesGrid);

        tvGalleryEmpty = findViewById(R.id.tvGalleryEmpty);
        tvFilesEmpty = findViewById(R.id.tvFilesEmpty);

        if (rvGalleryGrid != null) {
            rvGalleryGrid.setLayoutManager(new GridLayoutManager(this, 3));
            galleryAdapter = new GalleryGridAdapter();
            rvGalleryGrid.setAdapter(galleryAdapter);
        }

        if (rvFilesGrid != null) {
            rvFilesGrid.setLayoutManager(new GridLayoutManager(this, 3));
            filesAdapter = new FileGridAdapter();
            rvFilesGrid.setAdapter(filesAdapter);
        }

        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> {
                hideSoftKeyboard();
                if (checkAndRequestAllAppPermissions()) {
                    toggleAttachmentPanel();
                }
            });
        }

        View btnOpenSystemGallery = findViewById(R.id.btnOpenSystemGallery);
        if (btnOpenSystemGallery != null) {
            btnOpenSystemGallery.setOnClickListener(v -> {
                hideSoftKeyboard();
                if (checkAndRequestAllAppPermissions()) {
                    pickSystemGalleryLauncher.launch("image/*,video/*");
                }
            });
        }

        View btnOpenSystemFileManager = findViewById(R.id.btnOpenSystemFileManager);
        if (btnOpenSystemFileManager != null) {
            btnOpenSystemFileManager.setOnClickListener(v -> {
                hideSoftKeyboard();
                if (checkAndRequestAllAppPermissions()) {
                    pickSystemFileLauncher.launch("*/*");
                }
            });
        }

        if (layoutModeCamera != null) layoutModeCamera.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(0);
        });
        if (layoutModePhoto != null) layoutModePhoto.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(1);
        });
        if (layoutModeFiles != null) layoutModeFiles.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(2);
        });

        if (btnCameraPhoto != null) btnCameraPhoto.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) launchCameraPhoto();
        });
        if (btnCameraVideo != null) btnCameraVideo.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) launchCameraVideo();
        });

        if (btnCancelPending != null) btnCancelPending.setOnClickListener(v -> clearPendingAttachment());
    }

    private void toggleAttachmentPanel() {
        if (isAttachmentPanelOpen) {
            closeAttachmentPanel();
        } else {
            openAttachmentPanel();
        }
    }

    private void openAttachmentPanel() {
        if (isAttachmentPanelOpen || layoutAttachmentPanel == null) return;
        isAttachmentPanelOpen = true;

        hideSoftKeyboard();

        if (btnAttach != null) {
            btnAttach.setImageResource(R.drawable.ic_arrow_up);
        }

        layoutAttachmentPanel.setVisibility(View.VISIBLE);
        layoutAttachmentPanel.setTranslationY(300f);
        layoutAttachmentPanel.setAlpha(0f);
        layoutAttachmentPanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        switchAttachmentMode(currentAttachmentMode);
    }

    private void closeAttachmentPanel() {
        if (!isAttachmentPanelOpen || layoutAttachmentPanel == null) return;
        isAttachmentPanelOpen = false;

        if (btnAttach != null) {
            btnAttach.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        layoutAttachmentPanel.animate()
                .translationY(300f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    layoutAttachmentPanel.setVisibility(View.GONE);
                    layoutAttachmentPanel.setTranslationY(0f);
                })
                .start();
    }

    private void switchAttachmentMode(int mode) {
        currentAttachmentMode = mode;

        int activeBrand = ContextCompat.getColor(this, R.color.prime_brand);
        int idleSecondary = ContextCompat.getColor(this, R.color.prime_text_secondary);

        if (vModeCameraBg != null) vModeCameraBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);
        if (vModePhotoBg != null) vModePhotoBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);
        if (vModeFilesBg != null) vModeFilesBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);

        if (ivModeCameraIcon != null) ImageViewCompat.setImageTintList(ivModeCameraIcon, ColorStateList.valueOf(idleSecondary));
        if (ivModePhotoIcon != null) ImageViewCompat.setImageTintList(ivModePhotoIcon, ColorStateList.valueOf(idleSecondary));
        if (ivModeFilesIcon != null) ImageViewCompat.setImageTintList(ivModeFilesIcon, ColorStateList.valueOf(idleSecondary));

        if (tvModeCameraLabel != null) tvModeCameraLabel.setTextColor(idleSecondary);
        if (tvModePhotoLabel != null) tvModePhotoLabel.setTextColor(idleSecondary);
        if (tvModeFilesLabel != null) tvModeFilesLabel.setTextColor(idleSecondary);

        if (layoutSectionCamera != null) layoutSectionCamera.setVisibility(View.GONE);
        if (layoutSectionPhoto != null) layoutSectionPhoto.setVisibility(View.GONE);
        if (layoutSectionFiles != null) layoutSectionFiles.setVisibility(View.GONE);

        if (mode == 0) { // Camera
            if (vModeCameraBg != null) vModeCameraBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeCameraIcon != null) ImageViewCompat.setImageTintList(ivModeCameraIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeCameraLabel != null) tvModeCameraLabel.setTextColor(activeBrand);
            if (layoutSectionCamera != null) layoutSectionCamera.setVisibility(View.VISIBLE);
        } else if (mode == 1) { // Photo & Video Gallery
            if (vModePhotoBg != null) vModePhotoBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModePhotoIcon != null) ImageViewCompat.setImageTintList(ivModePhotoIcon, ColorStateList.valueOf(activeBrand));
            if (tvModePhotoLabel != null) tvModePhotoLabel.setTextColor(activeBrand);
            if (layoutSectionPhoto != null) layoutSectionPhoto.setVisibility(View.VISIBLE);
            loadGalleryMediaAsync();
        } else if (mode == 2) { // Files
            if (vModeFilesBg != null) vModeFilesBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeFilesIcon != null) ImageViewCompat.setImageTintList(ivModeFilesIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeFilesLabel != null) tvModeFilesLabel.setTextColor(activeBrand);
            if (layoutSectionFiles != null) layoutSectionFiles.setVisibility(View.VISIBLE);
            loadFilesAsync();
        }
    }

    private void launchCameraPhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = new File(getCacheDir(), "cam_photo_" + System.currentTimeMillis() + ".jpg");
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            cameraPhotoUri = photoUri;
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraPhotoLauncher.launch(intent);
        } catch (Exception e) {
            cameraPhotoLauncher.launch(intent);
        }
    }

    private void launchCameraVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            File videoFile = new File(getCacheDir(), "cam_video_" + System.currentTimeMillis() + ".mp4");
            Uri videoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", videoFile);
            cameraVideoUri = videoUri;
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            cameraVideoLauncher.launch(intent);
        } catch (Exception e) {
            cameraVideoLauncher.launch(intent);
        }
    }

    private void setPendingAttachmentFromUri(Uri uri, boolean isVideo, String fallbackName) {
        if (uri == null) return;
        String path = getPathFromUri(uri);
        String name = getFileNameFromUri(uri);
        if (name == null || name.isEmpty() || "Вложение".equals(name) || "Файл".equals(name)) {
            if (fallbackName != null && !fallbackName.isEmpty()) name = fallbackName;
        }
        long size = getFileSizeFromUri(uri);

        if (size > 2 * 1024 * 1024 * 1024L) {
            PrimeNotification.INSTANCE.show(this, "Превышен лимит размера файла (до 2 ГБ)", null);
            return;
        }

        Bitmap thumbnail = null;
        String durationStr = null;

        if (isVideo) {
            thumbnail = getVideoThumbnail(path != null ? path : uri.toString());
            durationStr = getVideoDurationFromUri(uri, path);
        } else {
            thumbnail = getPhotoThumbnail(uri, path);
        }

        PendingAttachment pending = new PendingAttachment();
        pending.uri = uri;
        pending.path = path != null ? path : uri.toString();
        pending.name = name;
        pending.size = size;
        pending.isVideo = isVideo;
        pending.isFile = !isVideo && !isPhotoMimeOrPath(uri, path);
        pending.thumbnail = thumbnail;
        pending.durationStr = durationStr;

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        closeAttachmentPanel();
    }

    private void setPendingAttachmentFromMediaItem(MediaItem item) {
        PendingAttachment pending = new PendingAttachment();
        pending.uri = item.uri;
        pending.path = item.path != null ? item.path : item.uri.toString();
        pending.name = item.name != null ? item.name : (item.isVideo ? "Видео" : "Фотография");
        pending.size = item.size;
        pending.isVideo = item.isVideo;
        pending.isFile = false;
        pending.durationStr = item.durationStr;

        if (item.isVideo) {
            pending.thumbnail = getVideoThumbnail(pending.path);
        } else if (item.path != null && new File(item.path).exists()) {
            pending.thumbnail = BitmapFactory.decodeFile(item.path);
        }

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        closeAttachmentPanel();
    }

    private void setPendingAttachmentFromFileItem(FileItem item) {
        PendingAttachment pending = new PendingAttachment();
        pending.uri = item.uri;
        pending.path = item.path != null ? item.path : item.uri.toString();
        pending.name = item.name != null ? item.name : "Файл";
        pending.size = item.size;
        pending.isVideo = false;
        pending.isFile = true;

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        closeAttachmentPanel();
    }

    private void showPendingAttachmentBar(PendingAttachment pending) {
        if (layoutPendingAttachment == null) return;
        layoutPendingAttachment.setVisibility(View.VISIBLE);

        if (tvPendingName != null) tvPendingName.setText(pending.name);
        if (tvPendingSize != null) tvPendingSize.setText(ChatAdapter.formatFileSize(pending.size));

        if (pending.isVideo) {
            if (ivPendingVideoBadge != null) ivPendingVideoBadge.setVisibility(View.VISIBLE);
            if (ivPendingThumbnail != null) {
                if (pending.thumbnail != null) ivPendingThumbnail.setImageBitmap(pending.thumbnail);
                else ivPendingThumbnail.setImageResource(R.drawable.ic_video);
            }
        } else if (pending.isFile) {
            if (ivPendingVideoBadge != null) ivPendingVideoBadge.setVisibility(View.GONE);
            if (ivPendingThumbnail != null) ivPendingThumbnail.setImageResource(R.drawable.ic_file);
        } else { // Photo
            if (ivPendingVideoBadge != null) ivPendingVideoBadge.setVisibility(View.GONE);
            if (ivPendingThumbnail != null) {
                if (pending.thumbnail != null) ivPendingThumbnail.setImageBitmap(pending.thumbnail);
                else if (pending.uri != null) ivPendingThumbnail.setImageURI(pending.uri);
                else ivPendingThumbnail.setImageResource(R.drawable.ic_photo);
            }
        }
    }

    private void clearPendingAttachment() {
        currentPendingAttachment = null;
        if (layoutPendingAttachment != null) {
            layoutPendingAttachment.setVisibility(View.GONE);
        }
    }

    private boolean isPhotoMimeOrPath(Uri uri, String path) {
        if (path != null) {
            String lower = path.toLowerCase(Locale.US);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                return true;
            }
        }
        if (uri != null) {
            String mime = getContentResolver().getType(uri);
            return mime != null && mime.startsWith("image/");
        }
        return false;
    }

    private boolean isVideoMimeOrPath(Uri uri, String path) {
        if (path != null) {
            String lower = path.toLowerCase(Locale.US);
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".avi")) {
                return true;
            }
        }
        if (uri != null) {
            String mime = getContentResolver().getType(uri);
            return mime != null && mime.startsWith("video/");
        }
        return false;
    }

    private Bitmap getPhotoThumbnail(Uri uri, String path) {
        try {
            if (path != null && new File(path).exists()) {
                return BitmapFactory.decodeFile(path);
            }
            if (uri != null) {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    return bmp;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Bitmap getVideoThumbnail(String pathOrUri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            if (pathOrUri.startsWith("content://")) {
                retriever.setDataSource(this, Uri.parse(pathOrUri));
            } else {
                retriever.setDataSource(pathOrUri);
            }
            Bitmap bmp = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private String getVideoDurationFromUri(Uri uri, String path) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            if (uri != null && uri.toString().startsWith("content://")) {
                retriever.setDataSource(this, uri);
            } else if (path != null) {
                retriever.setDataSource(path);
            }
            String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            if (durStr != null) {
                long ms = Long.parseLong(durStr);
                return formatMsToDuration(ms);
            }
        } catch (Exception ignored) {}
        return "00:00";
    }

    private static String formatMsToDuration(long ms) {
        long sec = (ms / 1000) % 60;
        long min = (ms / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) return uri.getPath();
        String[] proj = {MediaStore.MediaColumns.DATA};
        try (Cursor cursor = getContentResolver().query(uri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (col != -1) return cursor.getString(col);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return "Вложение";
        if ("file".equalsIgnoreCase(uri.getScheme())) return new File(uri.getPath()).getName();
        String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME};
        try (Cursor cursor = getContentResolver().query(uri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (col != -1) return cursor.getString(col);
            }
        } catch (Exception ignored) {}
        return "Файл";
    }

    private long getFileSizeFromUri(Uri uri) {
        if (uri == null) return 0L;
        if ("file".equalsIgnoreCase(uri.getScheme())) return new File(uri.getPath()).length();
        String[] proj = {MediaStore.MediaColumns.SIZE};
        try (Cursor cursor = getContentResolver().query(uri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (col != -1) return cursor.getLong(col);
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private void loadGalleryMediaAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MediaItem> list = new ArrayList<>();
            Uri queryUri = MediaStore.Files.getContentUri("external");
            String[] projection = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            };

            String selection = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                               " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ")";
            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, null, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    int typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                    int nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);

                    int count = 0;
                    while (cursor.moveToNext() && count < 60) {
                        long id = cursor.getLong(idCol);
                        int mediaType = cursor.getInt(typeCol);
                        long size = cursor.getLong(sizeCol);
                        long date = cursor.getLong(dateCol);
                        String name = nameCol != -1 ? cursor.getString(nameCol) : "Media";
                        String path = dataCol != -1 ? cursor.getString(dataCol) : null;

                        boolean isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                        long durMs = (durCol != -1 && isVideo) ? cursor.getLong(durCol) : 0L;
                        String durationStr = isVideo ? formatMsToDuration(durMs) : null;

                        Uri contentUri = isVideo ?
                            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id) :
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                        list.add(new MediaItem(contentUri, path, isVideo, durationStr, date, size, name));
                        count++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (galleryAdapter != null) {
                    galleryAdapter.setItems(list);
                    if (tvGalleryEmpty != null) {
                        tvGalleryEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        });
    }

    private void loadFilesAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FileItem> list = new ArrayList<>();
            Uri queryUri = MediaStore.Files.getContentUri("external");
            String[] projection = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "!=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                               " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + "!=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, null, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                    int nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);

                    int count = 0;
                    while (cursor.moveToNext() && count < 60) {
                        long id = cursor.getLong(idCol);
                        long size = cursor.getLong(sizeCol);
                        long date = cursor.getLong(dateCol);
                        String name = nameCol != -1 ? cursor.getString(nameCol) : "Файл";
                        String path = dataCol != -1 ? cursor.getString(dataCol) : null;

                        if (size <= 2 * 1024 * 1024 * 1024L) {
                            Uri contentUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                            list.add(new FileItem(contentUri, path, name, size, date));
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (filesAdapter != null) {
                    filesAdapter.setItems(list);
                    if (tvFilesEmpty != null) {
                        tvFilesEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        });
    }

    private class GalleryGridAdapter extends RecyclerView.Adapter<GalleryGridAdapter.ViewHolder> {
        private List<MediaItem> items = new ArrayList<>();

        void setItems(List<MediaItem> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_media, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            if (item.isVideo) {
                holder.layoutVideoBadge.setVisibility(View.VISIBLE);
                holder.tvVideoDuration.setText(item.durationStr != null ? item.durationStr : "00:00");
            } else {
                holder.layoutVideoBadge.setVisibility(View.GONE);
            }

            if (item.uri != null) {
                holder.ivGalleryThumbnail.setImageURI(item.uri);
            }

            holder.itemView.setOnClickListener(v -> setPendingAttachmentFromMediaItem(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivGalleryThumbnail;
            View layoutVideoBadge;
            TextView tvVideoDuration;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivGalleryThumbnail = itemView.findViewById(R.id.ivGalleryThumbnail);
                layoutVideoBadge = itemView.findViewById(R.id.layoutVideoBadge);
                tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
            }
        }
    }

    private class FileGridAdapter extends RecyclerView.Adapter<FileGridAdapter.ViewHolder> {
        private List<FileItem> items = new ArrayList<>();

        void setItems(List<FileItem> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_attachment, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FileItem item = items.get(position);
            holder.tvFileName.setText(item.name);
            holder.tvFileSize.setText(ChatAdapter.formatFileSize(item.size));

            holder.itemView.setOnClickListener(v -> {
                if (item.size > 2 * 1024 * 1024 * 1024L) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Превышен лимит размера файла (до 2 ГБ)", null);
                    return;
                }
                setPendingAttachmentFromFileItem(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivFileIcon;
            TextView tvFileName;
            TextView tvFileSize;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
            }
        }
    }
}
