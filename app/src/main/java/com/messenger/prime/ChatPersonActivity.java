package com.messenger.prime;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import java.lang.reflect.Method;
import java.util.Set;

import android.util.Log;
import android.util.LruCache;
import android.util.Size;
import android.media.ThumbnailUtils;
import android.view.GestureDetector;

import android.view.HapticFeedbackConstants;
import android.view.animation.AccelerateInterpolator;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.RemoteInput;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.util.Pair;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.concurrent.Executors;
import android.view.LayoutInflater;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
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
import androidx.recyclerview.widget.SimpleItemAnimator;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;
import kotlin.Unit;

public class ChatPersonActivity extends AppCompatActivity {

    private static final String TAG = "ChatPersonActivity";
    private static final UUID UUID_CHAT = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private static final int MESSAGE_READ = 1;
    private static final int MESSAGE_WRITE = 2;
    private static final int MESSAGE_TOAST = 3;
    private static final int HANDSHAKE_SUCCESS = 4;
    private static final int MESSAGE_READ_PHOTO = 5;
    private static final int MESSAGE_READ_FILE = 6;
    private static final int MESSAGE_TYPING = 7;
    private static final int MESSAGE_EDIT_RECEIVED = 8;
    private static final int MESSAGE_CHAT_DELETED = 9;
    private static final int MESSAGE_READ_RECEIPT = 10;
    private static final int MESSAGE_DISCONNECTED = 11;
    private static final int MESSAGE_DELETE_SINGLE = 12;
    private static final int MESSAGE_PRESENCE_UPDATED = 13;
    private static final int MESSAGE_REACTION_RECEIVED = 14;
    private static final int MESSAGE_SEND_PROGRESS = 16;
    private static final int MESSAGE_SEND_CANCELLED = 17;

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
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

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
    private String replyingToText = null;

    // Attachment Panel & Pending Attachment Views
    private FrameLayout layoutAttachmentPanel;
    private LinearLayout layoutPendingAttachment;
    private RecyclerView rvPendingCards;
    private PendingCardsAdapter pendingCardsAdapter;
    private LinearLayout layoutSendingProgress;
    private ProgressBar pbSendingProgress;
    private TextView tvSendingProgressPercent;
    private ImageButton btnCancelSendingProgress;
    private FloatingActionButton fabScrollToBottom;
    private View btnCancelPending;
    private String currentSendingMessageId = null;

    // Attachment Panel Modes UI
    private LinearLayout layoutModeCamera, layoutModePhoto, layoutModeFiles;
    private FrameLayout vModeCameraBg, vModePhotoBg, vModeFilesBg;
    private ImageView ivModeCameraIcon, ivModePhotoIcon, ivModeFilesIcon;
    private TextView tvModeCameraLabel, tvModePhotoLabel, tvModeFilesLabel;

    // Attachment Panel Content Sections UI
    private FrameLayout layoutSectionsContainer;
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
    private boolean isManuallyDisconnected = false;
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
                updateOfflineLastSeenStatus();
                long jitter = (long) (Math.random() * 1200);
                autoRetryHandler.removeCallbacks(autoRetryRunnable);
                autoRetryHandler.postDelayed(autoRetryRunnable, 2500L + jitter);
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
                boolean connectGranted = Objects.equals(result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false), Boolean.TRUE);
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

    private final ActivityResultLauncher<PickVisualMediaRequest> pickSystemGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(20), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    setPendingAttachmentFromUris(uris);
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
                    sendLocalAvatar(true);
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
        // Deprecated below 30
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        boolean isDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDarkTheme);
            controller.setAppearanceLightNavigationBars(!isDarkTheme);
        }

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

        setContentView(R.layout.activity_chat_person_content);

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
        
        UIExtensionsKt.setupEdgeToEdge(this, !isDarkTheme);
        setupChatBlurViews();

        View chatRoot = findViewById(R.id.chatRoot);
        if (chatRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                if (ime.bottom > 0 && isAttachmentPanelOpen) {
                    closeAttachmentPanel();
                }
                float density = getResources().getDisplayMetrics().density;
                int statusBarTop = systemBars.top > 0 ? systemBars.top : getStatusBarHeight();

                View vTopGradient = findViewById(R.id.vTopGradient);
                if (vTopGradient != null) {
                    ViewGroup.LayoutParams lp = vTopGradient.getLayoutParams();
                    if (lp != null) {
                        lp.height = statusBarTop + (int) (56 * density);
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
                        lp.topMargin = statusBarTop + (int) (12 * density);
                        layoutHeader.setLayoutParams(lp);
                    }
                }

                View bottomContainer = findViewById(R.id.bottomContainer);
                if (bottomContainer != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomContainer.getLayoutParams();
                    if (lp != null) {
                        lp.bottomMargin = Math.max(systemBars.bottom, ime.bottom);
                        bottomContainer.setLayoutParams(lp);
                    }
                }

                View tvFloatingDate = findViewById(R.id.tvFloatingDate);
                if (tvFloatingDate != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tvFloatingDate.getLayoutParams();
                    if (lp != null) {
                        lp.topMargin = statusBarTop + (int) (80 * density);
                        tvFloatingDate.setLayoutParams(lp);
                    }
                }

                updateMessageListPadding();

                return insets;
            });
            ViewCompat.requestApplyInsets(chatRoot);
        }

        tvChatName = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        tvFloatingDate = findViewById(R.id.tvFloatingDate);
        rvMessages = findViewById(R.id.rvMessages);
        
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && chatAdapter != null && chatAdapter.getItemCount() > 0) {
                rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1), 100);
            }
        });

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (chatAdapter == null) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int lastVisible = lm.findLastVisibleItemPosition();
                for (int i = 0; i <= lastVisible; i++) {
                    if (i >= 0 && i < chatAdapter.getItemCount()) {
                        ChatMessage msg = chatAdapter.getMessageAt(i);
                        if (msg != null && msg.isFirstUnread()) {
                            msg.setFirstUnread(false);
                            chatAdapter.notifyItemChanged(i);

                            if (connectedThread != null && connectedThread.isAlive() && msg.getMessageId() != null) {
                                connectedThread.sendPacket(TYPE_READ_RECEIPT, msg.getMessageId().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
        });

        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView ivChatAvatar = findViewById(R.id.ivChatAvatar);
        View layoutHeader = findViewById(R.id.layoutHeader);
        
        if (ivChatAvatar != null) {
            ViewCompat.setTransitionName(ivChatAvatar, "transition_avatar");
            ivChatAvatar.setOnClickListener(v -> openPersonInformationActivity());
            ivChatAvatar.setOnLongClickListener(v -> {
                showSearchDialog();
                return true;
            });
        }
        if (tvChatName != null) {
            ViewCompat.setTransitionName(tvChatName, "transition_name");
            tvChatName.setOnClickListener(v -> openPersonInformationActivity());
            tvChatName.setOnLongClickListener(v -> {
                showSearchDialog();
                return true;
            });
        }
        if (tvChatStatus != null) {
            tvChatStatus.setOnClickListener(v -> openPersonInformationActivity());
            tvChatStatus.setOnLongClickListener(v -> {
                showSearchDialog();
                return true;
            });
        }
        if (layoutHeader != null) {
            layoutHeader.setOnClickListener(v -> openPersonInformationActivity());
            layoutHeader.setOnLongClickListener(v -> {
                showSearchDialog();
                return true;
            });
        }
        
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
        btnCancelSendingProgress = findViewById(R.id.btnCancelSendingProgress);
        if (btnCancelSendingProgress != null) {
            btnCancelSendingProgress.setOnClickListener(v -> cancelCurrentFileSending());
        }
        fabScrollToBottom = findViewById(R.id.fabScrollToBottom);

        if (fabScrollToBottom != null) {
            fabScrollToBottom.setOnClickListener(v -> {
                if (rvMessages != null && chatAdapter != null && chatAdapter.getItemCount() > 0) {
                    rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }
            });
        }

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
            ViewCompat.setTransitionName(ivChatAvatar, "transition_avatar");
            ivChatAvatar.setOnClickListener(v -> openPersonInformationActivity());
            ivChatAvatar.setOnLongClickListener(v -> {
                showSearchDialog();
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
        
        RecyclerView.ItemAnimator animator = rvMessages.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        if (animator != null) {
            animator.setAddDuration(150);
            animator.setRemoveDuration(150);
        }

        rvMessages.setItemViewCacheSize(20);
        ItemTouchHelper.SimpleCallback swipeToReplyCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private boolean hapticTriggered = false;

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    return 0;
                }
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && chatAdapter != null) {
                    ChatMessage msg = chatAdapter.getMessageAt(pos);
                    if (msg != null && (msg.isMultiMedia() || (msg.getMediaItems() != null && msg.getMediaItems().size() > 1))) {
                        return 0;
                    }
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
                if (lm != null) {
                    int totalCount = chatAdapter != null ? chatAdapter.getItemCount() : 0;
                    int lastVisible = lm.findLastVisibleItemPosition();
                    boolean isNearBottom = (totalCount - 1 - lastVisible) <= 3;

                    if (fabScrollToBottom != null) {
                        if (!isNearBottom && totalCount > 5) {
                            if (fabScrollToBottom.getVisibility() != View.VISIBLE) {
                                fabScrollToBottom.setVisibility(View.VISIBLE);
                                fabScrollToBottom.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();
                            }
                        } else {
                            if (fabScrollToBottom.getVisibility() == View.VISIBLE) {
                                fabScrollToBottom.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200)
                                        .withEndAction(() -> fabScrollToBottom.setVisibility(View.GONE)).start();
                            }
                        }
                    }

                    if (tvFloatingDate != null && chatAdapter != null) {
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
            boolean foundUnread = false;
            for (ChatMessage m : history) {
                if (!m.isOutgoing() && (m.getMessageStatus() == MessageStatus.NONE || m.getMessageStatus() == MessageStatus.SENT)) {
                    if (!foundUnread) {
                        m.setFirstUnread(true);
                        foundUnread = true;
                    } else {
                        m.setFirstUnread(false);
                    }
                } else {
                    m.setFirstUnread(false);
                }
            }
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
                
                Intent deleteMsgIntent = new Intent("com.messenger.prime.MSG_DELETED").setPackage(getPackageName());
                deleteMsgIntent.putExtra("messageId", message.getMessageId());
                sendBroadcast(deleteMsgIntent);

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
            public void onReplyToSelectedText(ChatMessage message, String selectedText, int position) {
                if (!isRemoteUserOnline || connectedThread == null || !connectedThread.isAlive()) {
                    return;
                }
                enterReplyModeForSelectedText(message, selectedText);
            }

            @Override
            public void onForwardMessage(ChatMessage message, int position) {
                showForwardDialog(message);
            }

            @Override
            public void onCancelSending(ChatMessage message, int position) {
                cancelCurrentFileSending();
            }

            @Override
            public void onJumpToMessage(String messageId) {
                onJumpToMessage(messageId, null);
            }

            @Override
            public void onJumpToMessage(String messageId, String quotedText) {
                if (messageId == null || chatAdapter == null) return;
                int pos = chatAdapter.findPositionByMessageId(messageId);
                if (pos != -1) {
                    LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
                    if (lm != null) {
                        lm.scrollToPositionWithOffset(pos, (int) (80 * getResources().getDisplayMetrics().density));
                        rvMessages.postDelayed(() -> {
                            chatAdapter.highlightMessageAtPosition(pos);
                            chatAdapter.triggerHighlightAnimation(rvMessages, messageId, quotedText);
                        }, 200);
                    }
                } else {
                    PrimeNotification.show(ChatPersonActivity.this, "Исходное сообщение не найдено", null);
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
                            String metaCheck = new String(fullPayload, 0, Math.min(fullPayload.length, 300), StandardCharsets.UTF_8);
                            if (metaCheck.contains(":::MULTI:")) {
                                int headerEndIdx = -1;
                                String headerEndTag = ":::HEADER_END:::";
                                byte[] tagBytes = headerEndTag.getBytes(StandardCharsets.UTF_8);
                                for (int i = 0; i <= fullPayload.length - tagBytes.length; i++) {
                                    boolean match = true;
                                    for (int j = 0; j < tagBytes.length; j++) {
                                        if (fullPayload[i + j] != tagBytes[j]) { match = false; break; }
                                    }
                                    if (match) { headerEndIdx = i; break; }
                                }

                                if (headerEndIdx != -1) {
                                    String headerStr = new String(fullPayload, 0, headerEndIdx, StandardCharsets.UTF_8);
                                    int bodyStart = headerEndIdx + tagBytes.length;

                                    String photoMsgId = ChatPersonActivity.this.extractMsgId(headerStr);
                                    String sizesPart = "";
                                    int sizesIdx = headerStr.indexOf(":::SIZES:");
                                    if (sizesIdx != -1) {
                                        sizesPart = headerStr.substring(sizesIdx + 9);
                                        int endSizes = sizesPart.indexOf(":::");
                                        if (endSizes != -1) sizesPart = sizesPart.substring(0, endSizes);
                                    }

                                    String captionText = "";
                                    int multiIdx = headerStr.indexOf(":::MULTI:");
                                    if (multiIdx != -1 && sizesIdx != -1 && sizesIdx > multiIdx) {
                                        String multiSub = headerStr.substring(multiIdx + 9, sizesIdx);
                                        int colonIdx = multiSub.indexOf(":::");
                                        if (colonIdx != -1) {
                                            captionText = multiSub.substring(colonIdx + 3);
                                        }
                                    }

                                    String[] itemMetas = sizesPart.split(",");
                                    List<ChatMessage.MediaItem> recMediaItems = new ArrayList<>();
                                    int currentOffset = bodyStart;

                                    long photoTs = System.currentTimeMillis();
                                    String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));

                                    for (int idx = 0; idx < itemMetas.length; idx++) {
                                        try {
                                            String metaStr = itemMetas[idx].trim();
                                            String[] parts = metaStr.split("\\|");
                                            boolean isVideo = parts.length >= 1 && "1".equals(parts[0]);
                                            int pSize = parts.length >= 2 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
                                            String durStr = parts.length >= 3 ? parts[2] : "00:00";
                                            String ext = parts.length >= 4 ? parts[3] : (isVideo ? "mp4" : "jpg");

                                            if (currentOffset + pSize <= fullPayload.length) {
                                                byte[] itemBytes = new byte[pSize];
                                                System.arraycopy(fullPayload, currentOffset, itemBytes, 0, pSize);
                                                currentOffset += pSize;

                                                File mediaFile = new File(getFilesDir(), "rec_media_" + (photoMsgId != null ? photoMsgId : photoTs) + "_" + idx + "." + ext);
                                                FileOutputStream fos = new FileOutputStream(mediaFile);
                                                fos.write(itemBytes);
                                                fos.flush();
                                                fos.close();

                                                recMediaItems.add(new ChatMessage.MediaItem(mediaFile.getAbsolutePath(), isVideo, durStr));
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to parse multi-media item " + idx, e);
                                        }
                                    }

                                    if (!recMediaItems.isEmpty()) {
                                        ChatMessage multiMsg = new ChatMessage(captionText, photoTime, targetUsername, false, null, photoTs, null, photoMsgId);
                                        multiMsg.setMediaItems(recMediaItems);
                                        addMessageToUI(multiMsg);
                                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, multiMsg);
                                        typingResetHandler.removeCallbacks(resetTypingRunnable);
                                        resetTypingRunnable.run();
                                        break;
                                    }
                                }
                            }

                            String photoMsgId = null;
                            String captionText = null;
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

                            if (photoBytes.length > 9) {
                                byte[] magic = "|PRM|".getBytes(StandardCharsets.UTF_8);
                                boolean hasMagic = true;
                                for (int i = 0; i < 5; i++) {
                                    if (photoBytes[photoBytes.length - 5 + i] != magic[i]) {
                                        hasMagic = false;
                                        break;
                                    }
                                }
                                if (hasMagic) {
                                    int capLen = ((photoBytes[photoBytes.length - 9] & 0xFF) << 24) |
                                                 ((photoBytes[photoBytes.length - 8] & 0xFF) << 16) |
                                                 ((photoBytes[photoBytes.length - 7] & 0xFF) << 8) |
                                                 (photoBytes[photoBytes.length - 6] & 0xFF);
                                    if (capLen > 0 && capLen < photoBytes.length - 9) {
                                        captionText = new String(photoBytes, photoBytes.length - 9 - capLen, capLen, StandardCharsets.UTF_8);
                                        byte[] cleanPhoto = new byte[photoBytes.length - 9 - capLen];
                                        System.arraycopy(photoBytes, 0, cleanPhoto, 0, cleanPhoto.length);
                                        photoBytes = cleanPhoto;
                                    }
                                }
                            }

                            String savedPhotoPath = null;
                            if (photoBytes.length > 0) {
                                try {
                                    File photoFile = new File(getFilesDir(), "rec_photo_" + (photoMsgId != null ? photoMsgId : System.currentTimeMillis()) + ".jpg");
                                    FileOutputStream fos = new FileOutputStream(photoFile);
                                    fos.write(photoBytes);
                                    fos.flush();
                                    fos.close();
                                    savedPhotoPath = photoFile.getAbsolutePath();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to save received photo to disk", e);
                                }
                            }

                            Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                            long photoTs = System.currentTimeMillis();
                            String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                            ChatMessage photoMsg = new ChatMessage(captionText, photoTime, targetUsername, false, photoBitmap, photoTs, savedPhotoPath, photoMsgId);
                            photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
                            
                            addMessageToUI(photoMsg);
                            ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, photoMsg);
                            typingResetHandler.removeCallbacks(resetTypingRunnable);
                            resetTypingRunnable.run();
                        }
                        break;
                    case MESSAGE_READ_FILE:
                        byte[] fileBuf = (byte[]) msg.obj;
                        if (fileBuf != null && fileBuf.length > 0) {
                            ChatMessage fileMsg = parseFileMessageBytes(fileBuf, targetUsername);
                            if (fileMsg != null) {
                                addMessageToUI(fileMsg);
                                ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, fileMsg);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                resetTypingRunnable.run();
                            }
                        }
                        break;
                    case MESSAGE_SEND_PROGRESS:
                        int sendProg = msg.arg1;
                        if (chatAdapter != null && currentSendingMessageId != null) {
                            chatAdapter.updateMessageSendingProgress(currentSendingMessageId, sendProg);
                        }
                        break;
                    case MESSAGE_SEND_CANCELLED:
                        cancelCurrentFileSending();
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
                        sendReadReceiptsForUnreadMessages();
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
                                if (Objects.equals(editMsgId, newLast.getMessageId())) {
                                    saveLastMessageToChatList(updatedText);
                                }
                                
                                List<ChatMessage> history = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
                                for (ChatMessage m : history) {
                                    if (Objects.equals(editMsgId, m.getMessageId())) {
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
                        
                        Intent chatDeletedIntent = new Intent("com.messenger.prime.CHAT_DELETED").setPackage(getPackageName());
                        sendBroadcast(chatDeletedIntent);

                        try {
                            if (connectedThread != null) {
                                connectedThread.cancel();
                            }
                        } catch (Exception ignored) {}

                        BluetoothSocketHolder.clearSocket();
                        PrimeBluetoothService.stopService(ChatPersonActivity.this);
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, deletedByName, deviceAddress);
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, targetUsername, deviceAddress);
                        deleteChatFromChatListEx(deletedByName);
                        deleteChatFromChatListEx(targetUsername);

                        try {
                            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                            if (bluetoothManager != null) {
                                BluetoothAdapter bAdapter = bluetoothManager.getAdapter();
                                if (bAdapter != null && bAdapter.isEnabled() && deviceAddress != null) {
                                    BluetoothDevice device = bAdapter.getRemoteDevice(deviceAddress);
                                    if (device != null) {
                                        Method removeBondMethod = device.getClass().getMethod("removeBond");
                                        removeBondMethod.invoke(device);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to remove bond", e);
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(ChatPersonActivity.this::finish, 500L);
                        break;
                    case MESSAGE_READ_RECEIPT:
                        byte[] receiptBuf = (byte[]) msg.obj;
                        if (receiptBuf != null && msg.arg1 > 0) {
                            String receiptData = new String(receiptBuf, 0, msg.arg1, StandardCharsets.UTF_8);
                            chatAdapter.markOutgoingMessagesAsReadUpTo(receiptData);
                            ChatHistoryManager.markOutgoingMessagesAsRead(ChatPersonActivity.this, targetUsername, receiptData);
                        }
                        saveLastMessageToChatList(null, MessageStatus.READ, false, "ONLINE");
                        break;
                    case MESSAGE_DISCONNECTED:
                        isRemoteUserOnline = false;
                        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
                        handleDisconnectionUiAndAutoReconnect();
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
                            Intent deleteMsgIntent = new Intent("com.messenger.prime.MSG_DELETED").setPackage(getPackageName());
                            deleteMsgIntent.putExtra("messageId", deletedMsgId);
                            sendBroadcast(deleteMsgIntent);

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

            if (currentPendingAttachment != null && currentPendingAttachment.items != null && !currentPendingAttachment.items.isEmpty()) {
                List<PendingAttachmentItem> sendItems = new ArrayList<>(currentPendingAttachment.items);
                clearPendingAttachment();
                etMessage.setText("");

                boolean allMedia = true;
                for (PendingAttachmentItem item : sendItems) {
                    if (item.isFile) { allMedia = false; break; }
                }

                if (sendItems.size() > 1 && allMedia) {
                    sendMultiMedia(sendItems, text);
                } else {
                    for (PendingAttachmentItem item : sendItems) {
                        if (item.isVideo || item.isFile) {
                            if (item.size > 2 * 1024 * 1024 * 1024L) {
                                PrimeNotification.INSTANCE.show(this, "Превышен лимит размера файла (до 2 ГБ)", null);
                                continue;
                            }
                            sendVideoOrFile(item, text);
                        } else { // Photo
                            sendPhoto(item.uri != null ? item.uri : Uri.parse(item.path), text);
                        }
                    }
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
                    if (Objects.equals(editingMessageId, m.getMessageId())) {
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
                boolean hasText = s != null && !s.toString().trim().isEmpty();
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
                isManuallyDisconnected = false;
                checkPermissionsAndStartRole(false);
            });
        }

        // Настройка Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
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
            } else if ("com.messenger.prime.DISCONNECT_REQUESTED".equals(intent.getAction())) {
                isManuallyDisconnected = true;
                disconnectCurrentChat();
            } else if ("com.messenger.prime.CHAT_DELETED".equals(intent.getAction())) {
                isChatDeleted = true;
                finish();
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
                isChatDeleted = false;
                isManuallyDisconnected = false;
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
                autoRetryHandler.removeCallbacks(autoRetryRunnable);
                startPrimeConnection();
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
            filter.addAction("com.messenger.prime.DISCONNECT_REQUESTED");
            filter.addAction("com.messenger.prime.CHAT_DELETED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
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

        if ((connectedThread == null || !connectedThread.isAlive()) && (acceptThread == null || !acceptThread.isAlive()) && (connectThread == null || !connectThread.isAlive())) {
            connectionRetryCount = 0;
            autoRetryHandler.removeCallbacks(autoRetryRunnable);
            autoRetryHandler.postDelayed(this::startPrimeConnection, 500L);
        }

        reloadLocalProfileFromSettings();

        MaterialShapesWallpaperView vWallpaper = findViewById(R.id.vWallpaperBackground);
        if (vWallpaper != null) {
            vWallpaper.updateThemeColors();
            if (!wasSubActivity) {
                vWallpaper.startEntranceAnimation();
            }
        }

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
                if (history.size() > oldSize && !history.isEmpty()) {
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
        isManuallyDisconnected = true;
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
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
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Exception ignored) {}

        hideSoftKeyboard();
        closeAttachmentPanel();
        clearPendingAttachment();

        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
        stopAnimatingStatus();
        updateOfflineLastSeenStatus();
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

    private void beginLayoutTransition(ViewGroup parent) {
        if (parent == null) return;
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new Fade());
        transition.addTransition(new ChangeBounds());
        transition.setDuration(220);
        transition.setInterpolator(new DecelerateInterpolator());
        TransitionManager.beginDelayedTransition(parent, transition);
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
            beginLayoutTransition((ViewGroup) layoutEditBar.getParent());
            layoutEditBar.setVisibility(View.VISIBLE);
        }
        updateMessageListPadding();
    }

    private void exitEditMode() {
        isEditMode = false;
        editingMessageId = null;

        if (layoutEditBar != null) {
            beginLayoutTransition((ViewGroup) layoutEditBar.getParent());
            layoutEditBar.setVisibility(View.GONE);
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
        replyingToText = null;

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

    private void enterReplyModeForSelectedText(ChatMessage message, String selectedText) {
        if (message == null || selectedText == null || selectedText.trim().isEmpty()) return;
        replyingToMessage = message;
        replyingToText = selectedText.trim();

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
            tvReplyBarText.setText("\"" + replyingToText + "\"");
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
        replyingToText = null;
        if (layoutReplyBar != null) {
            beginLayoutTransition((ViewGroup) layoutReplyBar.getParent());
            layoutReplyBar.setVisibility(View.GONE);
        }
        updateMessageListPadding();
    }

    private void showForwardDialog(ChatMessage messageToForward) {
        if (messageToForward == null) return;

        SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
        String jsonChats = sharedPrefs.getString("persisted_chats", "[]");
        List<String> chatNames = new ArrayList<>();

        try {
            JSONArray chatArray = new JSONArray(jsonChats);
            for (int i = 0; i < chatArray.length(); i++) {
                JSONObject obj = chatArray.getJSONObject(i);
                String name = obj.optString("name", "");
                if (!name.isEmpty() && !chatNames.contains(name)) {
                    chatNames.add(name);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading persisted_chats for forward", e);
        }

        if (targetUsername != null && !targetUsername.isEmpty() && !chatNames.contains(targetUsername)) {
            chatNames.add(0, targetUsername);
        }

        if (chatNames.isEmpty()) {
            PrimeNotification.INSTANCE.show(this, "Нет доступных чатов для пересылки", null);
            return;
        }

        String[] chatArray = chatNames.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Переслать сообщение")
                .setItems(chatArray, (dialog, which) -> {
                    String selectedTarget = chatArray[which];
                    forwardMessageToTarget(messageToForward, selectedTarget);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void forwardMessageToTarget(ChatMessage messageToForward, String targetName) {
        if (messageToForward == null || targetName == null || targetName.isEmpty()) return;

        long timestamp = System.currentTimeMillis();
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
        String myLogin = sharedPrefs.getString("current_user", "");

        boolean isCurrentChat = targetName.equalsIgnoreCase(targetUsername);

        String forwardedText = messageToForward.getText();
        if (forwardedText == null) forwardedText = "";

        ChatMessage fwdMsg = new ChatMessage(
                forwardedText,
                timeStr,
                myLogin,
                isCurrentChat,
                null,
                timestamp,
                messageToForward.getImagePath()
        );
        fwdMsg.setFileName(messageToForward.getFileName());
        fwdMsg.setFileSize(messageToForward.getFileSize());
        fwdMsg.setVideoDuration(messageToForward.getVideoDuration());
        fwdMsg.setMessageType(messageToForward.getMessageType());
        if (messageToForward.getMediaItems() != null && !messageToForward.getMediaItems().isEmpty()) {
            fwdMsg.setMediaItems(messageToForward.getMediaItems());
        }

        fwdMsg.setReplyToSender(messageToForward.getSenderLogin() != null ? messageToForward.getSenderLogin() : "Сообщение");
        fwdMsg.setReplyToText(forwardedText.isEmpty() ? "Вложение" : forwardedText);
        fwdMsg.setReplyToMessageId(messageToForward.getMessageId());

        if (isCurrentChat) {
            if (chatAdapter != null) {
                chatAdapter.addMessage(fwdMsg);
                if (chatAdapter.getItemCount() > 0) {
                    rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }
            ChatHistoryManager.saveMessage(this, targetUsername, fwdMsg);
            saveLastMessageToChatList(fwdMsg.getText());

            if (connectedThread != null && connectedThread.isAlive()) {
                String packetContent = fwdMsg.getMessageId() + ":::" + fwdMsg.getText();
                packetContent += ":::REPLY:::" + fwdMsg.getReplyToMessageId() + ":::" + fwdMsg.getReplyToSender() + ":::" + fwdMsg.getReplyToText();
                connectedThread.sendPacket(TYPE_TEXT, packetContent.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            ChatHistoryManager.saveMessage(this, targetName, fwdMsg);
        }

        PrimeNotification.INSTANCE.show(this, "Переслано: " + targetName, null);
    }

    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getWindow().getDecorView());
        if (insets != null) {
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
        }
        return statusBarHeight > 0 ? statusBarHeight : (int) (36 * getResources().getDisplayMetrics().density);
    }

    private void updateMessageListPadding() {
        View chatRoot = findViewById(R.id.chatRoot);
        View rvMessages = findViewById(R.id.rvMessages);
        View bottomContainer = findViewById(R.id.bottomContainer);
        if (rvMessages == null || chatRoot == null) return;

        float density = getResources().getDisplayMetrics().density;

        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(chatRoot);
        int topInset = getStatusBarHeight();
        int bottomInset = (int) (16 * density);
        if (rootInsets != null) {
            Insets sb = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = rootInsets.getInsets(WindowInsetsCompat.Type.ime());
            if (sb.top > 0) topInset = sb.top;
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

    private void setupChatBlurViews() {
        ViewGroup rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) rootView = (ViewGroup) getWindow().getDecorView();

        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Drawable windowBg = getWindow().getDecorView().getBackground();
        int overlayColor = isDark
                ? Color.parseColor("#400F172A")
                : Color.parseColor("#40154B87");

        BlurView blurHeader = findViewById(R.id.layoutHeader);
        BlurView blurConnectAction = findViewById(R.id.layoutConnectAction);
        BlurView blurInput = findViewById(R.id.layoutInput);
        BlurView blurAttachmentPanel = findViewById(R.id.layoutAttachmentPanel);

        BlurView[] blurViews = new BlurView[]{
                blurHeader, blurConnectAction, blurInput, blurAttachmentPanel
        };

        for (BlurView bv : blurViews) {
            if (bv != null) {
                BlurViewKt.setupBlur(bv, rootView, 16f, overlayColor, windowBg);
            }
        }
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
                return (int) PackageInfoCompat.getLongVersionCode(context.getPackageManager().getPackageInfo(context.getPackageName(), 0));
            }
        } catch (Exception e) {
            return 1;
        }
    }

    private int resolveColor(int colorResOrValue) {
        if ((colorResOrValue >>> 24) == 0x7f || (colorResOrValue >>> 24) == 0x01) {
            try {
                return ContextCompat.getColor(this, colorResOrValue);
            } catch (Exception ignored) {}
        }
        return colorResOrValue;
    }

    private void setStatusWithAnimation(String newText, int colorResOrValue) {
        setStatusWithAnimation(newText, colorResOrValue, true);
    }

    private void setStatusWithAnimation(String newText, int colorResOrValue, boolean stopAnimation) {
        if (stopAnimation) {
            stopAnimatingStatus();
        }
        try {
            sendBroadcast(new Intent("com.messenger.prime.STATUS_UPDATED").setPackage(getPackageName()));
        } catch (Exception ignored) {}
        if (tvChatStatus == null) return;
        CharSequence currentText = tvChatStatus.getText();
        if (currentText != null && Objects.equals(currentText.toString(), newText)) return;

        int textColor = resolveColor(colorResOrValue);
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
        if (viewStatus != null) {
            viewStatus.setVisibility(View.GONE);
        }
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
            statusAnimHandler.postDelayed(this, 300);
        }
    };

    private void setStatusTextDirect(String newText, int colorResOrValue) {
        if (tvChatStatus == null) return;
        int finalColor = resolveColor(colorResOrValue);
        tvChatStatus.setText(newText);
        tvChatStatus.setTextColor(finalColor);
    }

    private void startAnimatingStatus(String baseStatus, int colorResOrValue) {
        statusAnimHandler.removeCallbacks(statusAnimRunnable);
        currentBaseStatus = baseStatus;
        currentStatusColor = resolveColor(colorResOrValue);
        statusDotCount = 0;
        setStatusWithAnimation(baseStatus, currentStatusColor, false);
        statusAnimHandler.postDelayed(statusAnimRunnable, 300);
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

    @SuppressWarnings("unchecked")
    private void openPersonInformationActivity() {
        Intent intent = new Intent(this, PersonInformationActivity.class);
        intent.putExtra("EXTRA_CHAT_NAME", targetUsername);
        intent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
        intent.putExtra("EXTRA_AVATAR_URI", remoteAvatarUri);
        intent.putExtra("EXTRA_IS_ONLINE", isRemoteUserOnline);
        setOpeningSubActivity(true);

        View btnBackView = findViewById(R.id.btnBack);
        ImageView ivAvatar = findViewById(R.id.ivChatAvatar);
        TextView tvName = findViewById(R.id.tvChatName);

        List<Pair<View, String>> pairs = new ArrayList<>();
        if (btnBackView != null) {
            pairs.add(Pair.create(btnBackView, "transition_back_btn"));
        }
        if (ivAvatar != null) {
            pairs.add(Pair.create(ivAvatar, "transition_avatar"));
        }
        if (tvName != null) {
            pairs.add(Pair.create(tvName, "transition_name"));
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                pairs.toArray(new Pair[0])
        );
        startActivity(intent, options.toBundle());
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Введите текст для поиска...");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
                .setTitle("Поиск сообщений")
                .setView(input)
                .setPositiveButton("Найти", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty() && chatAdapter != null) {
                        int foundPos = chatAdapter.findPositionByQuery(query, 0);
                        if (foundPos != -1) {
                            rvMessages.scrollToPosition(foundPos);
                            chatAdapter.highlightMessageAtPosition(foundPos);
                            Toast.makeText(this, "Сообщение найдено", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Сообщение не найдено", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }



    private void showDeleteChatDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_blur_delete_chat, null);
        BlurView blurCard = dialogView.findViewById(R.id.blurDialogCard);
        TextView tvTitle = dialogView.findViewById(R.id.tvBlurDialogTitle);
        View btnNo = dialogView.findViewById(R.id.btnBlurDialogNo);
        View btnYes = dialogView.findViewById(R.id.btnBlurDialogYes);

        if (tvTitle != null) {
            tvTitle.setText("Удалить чат с " + (targetUsername != null ? targetUsername : "") + "?");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.setOnShowListener(d -> {
            ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            if (decorView != null && blurCard != null) {
                BlurAlgorithm algorithm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? new RenderEffectBlur()
                        : new RenderScriptBlur(this);
                blurCard.setupWith(decorView, algorithm)
                        .setBlurRadius(20f)
                        .setOverlayColor(Color.parseColor("#80154B87"))
                        .setBlurAutoUpdate(true);
            }
        });

        if (btnNo != null) btnNo.setOnClickListener(v -> dialog.dismiss());
        if (btnYes != null) btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            performCompleteChatDeletion();
        });

        dialog.show();
    }

    private void performCompleteChatDeletion() {
        isChatDeleted = true;
        if (connectedThread != null && connectedThread.isAlive()) {
            String deletionPayload = "DELETE_CHAT:login=" + localUsername + ";name=" + localUsername;
            connectedThread.sendPacket(TYPE_CHAT_DELETED, deletionPayload.getBytes(StandardCharsets.UTF_8));
            try { Thread.sleep(100); } catch (Exception ignored) {}
        }
        BluetoothSocketHolder.clearSocket();
        PrimeBluetoothService.stopService(this);

        try {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bAdapter = bluetoothManager.getAdapter();
                if (bAdapter != null && bAdapter.isEnabled() && deviceAddress != null) {
                    BluetoothDevice device = bAdapter.getRemoteDevice(deviceAddress);
                    if (device != null) {
                        Method removeBondMethod = device.getClass().getMethod("removeBond");
                        removeBondMethod.invoke(device);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove bond", e);
        }

        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
        deleteChatFromChatListEx(targetUsername);
        if (deviceAddress != null) deleteChatFromChatListEx(deviceAddress);

        Intent chatDeletedIntent = new Intent("com.messenger.prime.CHAT_DELETED").setPackage(getPackageName());
        sendBroadcast(chatDeletedIntent);

        if (chatAdapter != null) chatAdapter.setMessages(new ArrayList<>());
        disconnectCurrentChat();
        Toast.makeText(this, "Переписка и устройство удалены", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, ChatListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void deleteChatFromChatList(String targetName) {
        deleteChatFromChatListEx(targetName);
    }

    private void deleteChatFromChatListEx(String nameOrId) {
        isChatDeleted = true;
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        BluetoothSocketHolder.clearSocket();
        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
        if (nameOrId != null && !nameOrId.equalsIgnoreCase(targetUsername)) {
            ChatHistoryManager.deleteHistoryCompletely(this, nameOrId, deviceAddress);
        }

        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name");
                String id = obj.optString("id");
                
                boolean matches = false;
                if (nameOrId != null && !nameOrId.isEmpty()) {
                    if (nameOrId.equalsIgnoreCase(name) || nameOrId.equalsIgnoreCase(id)) {
                        matches = true;
                    }
                }
                if (targetUsername != null && !targetUsername.isEmpty()) {
                    if (targetUsername.equalsIgnoreCase(name) || targetUsername.equalsIgnoreCase(id)) {
                        matches = true;
                    }
                }
                if (deviceAddress != null && !deviceAddress.isEmpty()) {
                    if (deviceAddress.equalsIgnoreCase(id) || deviceAddress.equalsIgnoreCase(name)) {
                        matches = true;
                    }
                }

                if (!matches) {
                    newArray.put(obj);
                }
            }
            sharedPrefs.edit().putString("persisted_chats", newArray.toString()).apply();
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove deleted chat from persisted_chats", e);
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
                
                boolean isMatch = false;
                if (updatedObj == null) {
                    if (deviceAddress != null && !deviceAddress.isEmpty() && deviceAddress.equalsIgnoreCase(obj.optString("id"))) {
                        isMatch = true;
                    } else if (targetUsername.equalsIgnoreCase(obj.optString("name"))) {
                        isMatch = true;
                    }
                }
                
                if (isMatch) {
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
                    if (deviceAddress != null && !deviceAddress.isEmpty()) {
                        obj.put("id", deviceAddress);
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

            sharedPrefs.edit().putString("persisted_chats", finalArray.toString()).apply();
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats", e);
        }
    }

    public static String extractMsgId(String data) {
        if (data == null) return null;
        int sep = data.indexOf(":::");
        return sep != -1 ? data.substring(0, sep) : null;
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
            sharedPrefs.edit().putString("persisted_chats", array.toString()).apply();
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

    private void sendReadReceiptsForUnreadMessages() {
        if (connectedThread == null || !connectedThread.isAlive() || chatAdapter == null) return;
        
        String lastUnreadMsgId = null;
        for (int i = 0; i < chatAdapter.getItemCount(); i++) {
            ChatMessage m = chatAdapter.getMessageAt(i);
            if (m != null && !m.isOutgoing() && (m.getMessageStatus() == MessageStatus.NONE || m.getMessageStatus() == MessageStatus.SENT)) {
                m.setMessageStatus(MessageStatus.READ);
                lastUnreadMsgId = m.getMessageId();
            }
        }

        if (lastUnreadMsgId != null) {
            String receiptPayload = "READ_ALL:::" + lastUnreadMsgId;
            connectedThread.sendPacket(TYPE_READ_RECEIPT, receiptPayload.getBytes(StandardCharsets.UTF_8));
            ChatHistoryManager.markOutgoingMessagesAsRead(this, targetUsername, lastUnreadMsgId);
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
            String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
            message.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Фотография");
            cancelReplyMode();
        }

        if (connectedThread != null && connectedThread.isAlive()) {
            message.setMessageStatus(MessageStatus.SENT);
        } else {
            message.setMessageStatus(MessageStatus.SENDING);
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

    private void sendMultiMedia(List<PendingAttachmentItem> items, String captionText) {
        if (items == null || items.isEmpty()) return;
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");

        showSendingProgressUi(0);

        Executors.newSingleThreadExecutor().execute(() -> {
            long timestamp = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();

            List<ChatMessage.MediaItem> chatMediaList = new ArrayList<>();
            List<byte[]> photoByteList = new ArrayList<>();
            StringBuilder sizesSb = new StringBuilder();

            for (int i = 0; i < items.size(); i++) {
                PendingAttachmentItem item = items.get(i);
                byte[] b = getBytesFromPending(item);
                photoByteList.add(b);
                if (i > 0) sizesSb.append(",");
                
                String isVidStr = item.isVideo ? "1" : "0";
                String durStr = item.durationStr != null ? item.durationStr : "00:00";
                String extStr = item.isVideo ? "mp4" : "jpg";
                sizesSb.append(isVidStr).append("|").append(b.length).append("|").append(durStr).append("|").append(extStr);

                String p = item.path != null ? item.path : (item.uri != null ? item.uri.toString() : "");
                chatMediaList.add(new ChatMessage.MediaItem(p, item.isVideo, item.durationStr));

                int progress = (int) (((i + 1) / (float) items.size()) * 70);
                runOnUiThread(() -> showSendingProgressUi(progress));
            }

            ChatMessage multiMsg = new ChatMessage(captionText, time, localUsername, true, null, timestamp, null, messageId);
            multiMsg.setMediaItems(chatMediaList);
            multiMsg.setMessageStatus(MessageStatus.SENT);

            if (replyingToMessage != null) {
                multiMsg.setReplyToMessageId(replyingToMessage.getMessageId());
                multiMsg.setReplyToSender(replyingToMessage.getSenderLogin());
                String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
                multiMsg.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Медиафайлы (" + items.size() + ")");
                runOnUiThread(this::cancelReplyMode);
            }

            String header = messageId + ":::MULTI:" + items.size() + ":::" + (captionText != null ? captionText : "") + ":::SIZES:" + sizesSb.toString() + ":::HEADER_END:::";
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

            int totalLen = headerBytes.length;
            for (byte[] b : photoByteList) totalLen += b.length;

            byte[] fullPayload = new byte[totalLen];
            System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
            int pos = headerBytes.length;
            for (byte[] b : photoByteList) {
                System.arraycopy(b, 0, fullPayload, pos, b.length);
                pos += b.length;
            }

            currentSendingMessageId = messageId;
            if (connectedThread != null && connectedThread.isAlive()) {
                connectedThread.sendPacket(TYPE_PHOTO, fullPayload);
            } else {
                pendingMessageQueue.add(new PendingMessage(TYPE_PHOTO, fullPayload));
                startPrimeConnection();
            }

            runOnUiThread(() -> {
                showSendingProgressUi(100);
                addMessageToUI(multiMsg);
                ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, multiMsg);
                saveLastMessageToChatList("Медиафайлы (" + items.size() + ")", MessageStatus.SENT, false, "ONLINE");
            });
        });
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
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                }
            }
            
            if (bitmap != null) {
                Bitmap scaledBitmap = scaleBitmapDown(bitmap, 1024);
                if (scaledBitmap != bitmap && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] photoBytes = baos.toByteArray();
                
                long timestamp = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();
                
                String savedPhotoPath = null;
                try {
                    File photoFile = new File(getFilesDir(), "sent_photo_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 4) + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                        fos.write(photoBytes);
                        fos.flush();
                    }
                    savedPhotoPath = photoFile.getAbsolutePath();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save sent photo to disk", e);
                }

                byte[] captionBytes = null;
                if (captionText != null && !captionText.trim().isEmpty()) {
                    captionBytes = captionText.trim().getBytes(StandardCharsets.UTF_8);
                }
                
                byte[] finalPhotoBytes;
                if (captionBytes != null && captionBytes.length > 0) {
                    byte[] magic = "|PRM|".getBytes(StandardCharsets.UTF_8);
                    int capLen = captionBytes.length;
                    finalPhotoBytes = new byte[photoBytes.length + capLen + 4 + magic.length];
                    System.arraycopy(photoBytes, 0, finalPhotoBytes, 0, photoBytes.length);
                    System.arraycopy(captionBytes, 0, finalPhotoBytes, photoBytes.length, capLen);
                    
                    finalPhotoBytes[finalPhotoBytes.length - 9] = (byte) (capLen >> 24);
                    finalPhotoBytes[finalPhotoBytes.length - 8] = (byte) (capLen >> 16);
                    finalPhotoBytes[finalPhotoBytes.length - 7] = (byte) (capLen >> 8);
                    finalPhotoBytes[finalPhotoBytes.length - 6] = (byte) capLen;
                    
                    System.arraycopy(magic, 0, finalPhotoBytes, finalPhotoBytes.length - 5, magic.length);
                } else {
                    finalPhotoBytes = photoBytes;
                }
                
                byte[] headerBytes = (messageId + ":::").getBytes(StandardCharsets.UTF_8);
                byte[] fullPayload = new byte[headerBytes.length + finalPhotoBytes.length];
                System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
                System.arraycopy(finalPhotoBytes, 0, fullPayload, headerBytes.length, finalPhotoBytes.length);

                currentSendingMessageId = messageId;
                if (connectedThread != null && connectedThread.isAlive()) {
                    connectedThread.sendPacket(TYPE_PHOTO, fullPayload);
                } else {
                    pendingMessageQueue.add(new PendingMessage(TYPE_PHOTO, fullPayload));
                    startPrimeConnection();
                }
                
                ChatMessage photoMsg = new ChatMessage(captionText, time, localUsername, true, scaledBitmap, timestamp, savedPhotoPath, messageId);
                photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
                if (connectedThread != null && connectedThread.isAlive()) {
                    photoMsg.setMessageStatus(MessageStatus.SENT);
                } else {
                    photoMsg.setMessageStatus(MessageStatus.SENDING);
                }
                if (replyingToMessage != null) {
                    photoMsg.setReplyToMessageId(replyingToMessage.getMessageId());
                    photoMsg.setReplyToSender(replyingToMessage.getSenderLogin());
                    String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
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

    private String lastSentAvatarChecksum = "";

    private void sendLocalAvatar() {
        sendLocalAvatar(false);
    }

    private void sendLocalAvatar(boolean force) {
        if (connectedThread == null || !connectedThread.isAlive()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPrefs.getString("current_user", "");
            String localAvatarUri = sharedPrefs.getString(currentUser + "_avatar", "");
            if (localAvatarUri == null || localAvatarUri.isEmpty()) {
                localAvatarUri = sharedPrefs.getString(currentUser + "_avatarUri", "");
            }

            if (localAvatarUri == null || localAvatarUri.isEmpty()) {
                File f = new File(getFilesDir(), "avatar_" + currentUser + ".jpg");
                if (f.exists()) {
                    localAvatarUri = Uri.fromFile(f).toString();
                }
            }

            if (!localAvatarUri.isEmpty()) {
                InputStream is = getContentResolver().openInputStream(Uri.parse(localAvatarUri));
                if (is != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bitmap != null) {
                        // 256x256 пикселей более чем достаточно для аватарок (44dp) и весит всего ~12 КБ (быстрый мгновенный старт)
                        int targetSize = 256;
                        float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
                        int width = targetSize;
                        int height = targetSize;
                        if (aspectRatio > 1) {
                            height = Math.round(targetSize / aspectRatio);
                        } else {
                            width = Math.round(targetSize * aspectRatio);
                        }
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos);
                        byte[] avatarBytes = baos.toByteArray();

                        String currentPeerKey = (deviceAddress != null ? deviceAddress : "") + "_" + (targetUsername != null ? targetUsername : "");
                        String checksum = localAvatarUri + "_" + avatarBytes.length + "_" + currentPeerKey;

                        if (!force && checksum.equals(lastSentAvatarChecksum)) {
                            Log.d(TAG, "Avatar unchanged and already sent to peer, skipping duplicate send.");
                            return;
                        }

                        lastSentAvatarChecksum = checksum;
                        connectedThread.sendPacket(TYPE_AVATAR, avatarBytes);
                    }
                }
            } else {
                if (force || !"EMPTY_AVATAR".equals(lastSentAvatarChecksum)) {
                    lastSentAvatarChecksum = "EMPTY_AVATAR";
                    connectedThread.sendPacket(TYPE_AVATAR, new byte[0]);
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
        if (targetName == null || targetName.isEmpty()) return null;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String jsonChats = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray chatArray = new JSONArray(jsonChats);
            for (int i = 0; i < chatArray.length(); i++) {
                JSONObject obj = chatArray.getJSONObject(i);
                String name = obj.optString("name");
                String id = obj.optString("id");
                if (targetName.equalsIgnoreCase(name) && isValidMacAddress(id)) {
                    return id;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read MAC from persisted_chats", e);
        }

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
        if (isChatDeleted || isManuallyDisconnected) return;
        if (connectedThread != null && connectedThread.isAlive()) return;
        if (connectThread != null && connectThread.isAlive()) {
            Log.d(TAG, "ConnectThread is already running, waiting for result...");
            return;
        }

        Log.d(TAG, "Starting Fast Direct Connection...");
        
        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
        if (layoutInput != null) layoutInput.setVisibility(View.GONE);
        if (btnPrimeConnect != null) {
            btnPrimeConnect.setEnabled(false);
            btnPrimeConnect.setText("Подключение...");
        }
        startAnimatingStatus("Установка связи", R.color.prime_accent);

        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, 30000L);

        // 1. Немедленно отменяем любое фоновое сканирование ОС
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }

        // 2. Серверный сокет запускается/поддерживается постоянно без пересоздания
        if (acceptThread == null || !acceptThread.isAlive()) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        // 3. Мгновенный сокетный коннект по известному MAC-адресу
        String addressToConnect = deviceAddress;
        if (!isValidMacAddress(addressToConnect)) {
            addressToConnect = findMacForTargetUsername(targetUsername);
        }

        if (isValidMacAddress(addressToConnect)) {
            final String targetMac = addressToConnect;

            String currentUser = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).getString("current_user", "");
            if (currentUser.isEmpty()) currentUser = "UserA";

            // Асимметричное определение роли по уникальному ID/MAC для полного исключения коллизий (Race condition)
            String myMacOrId = BluetoothSocketHolder.getLocalDeviceId(this);
            if (!myMacOrId.isEmpty()) {
                myMacOrId = currentUser.toLowerCase(Locale.ROOT);
            }
            
            String targetId = targetMac.toUpperCase(Locale.ROOT);
            boolean isPrimaryInitiator = myMacOrId.toUpperCase(Locale.ROOT).compareTo(targetId) > 0;
            long jitter = (long) (Math.random() * 800);
            long delayMs = isPrimaryInitiator ? 100L : (2200L + jitter);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if ((connectedThread == null || !connectedThread.isAlive()) && (connectThread == null || !connectThread.isAlive()) && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
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
            stopAnimatingStatus();
            if (remoteVersionCode != -1 && remoteVersionCode != getAppVersionCode(this)) {
                setStatusWithAnimation("В сети (Другая версия)", Color.parseColor("#FFC107"));
            } else {
                setStatusWithAnimation("В сети", R.color.prime_success);
            }
            setOnlineStatusIndicator(true);
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
            if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
        });
    }

    private void handleDisconnectionUiAndAutoReconnect() {
        runOnUiThread(() -> {
            hideSoftKeyboard();
            closeAttachmentPanel();
            clearPendingAttachment();

            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
            if (layoutInput != null) layoutInput.setVisibility(View.GONE);

            stopAnimatingStatus();
            updateOfflineLastSeenStatus();
            setOnlineStatusIndicator(false);
            updateConnectionStateInAdapter();

            if (!isChatDeleted && !isFinishing() && !isDestroyed() && !isManuallyDisconnected) {
                autoRetryHandler.removeCallbacks(autoRetryRunnable);
                autoRetryHandler.postDelayed(autoRetryRunnable, 200L);
            }
        });
    }

    private void connectionFailed() {
        if (isChatDeleted) return;
        connectThread = null;
        handleDisconnectionUiAndAutoReconnect();
    }

    private void connectionLost() {
        BluetoothSocketHolder.clearSocket();
        isHandshakeDone = false;
        isRemoteUserOnline = false;
        connectThread = null;

        handleDisconnectionUiAndAutoReconnect();

        if (!isOpeningSubActivity && !isChatDeleted) {
            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Соединение разорвано", null);
        }
        
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void showBackgroundNotification(String title, String message) {
        Activity currentActivity = PrimeApplication.getCurrentActivity();
        if (currentActivity instanceof ChatPersonActivity) {
            ChatPersonActivity cpa = (ChatPersonActivity) currentActivity;
            if (Objects.equals(cpa.targetUsername, title)) {
                return; // Уже находимся в этом чате, ничего не показываем
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted");
                    return;
                }
            }

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            PrimeApplication.CHANNEL_MESSAGES_ID,
                            "Сообщения Prime",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                    channel.enableLights(true);
                    nm.createNotificationChannel(channel);
                }
                
                Intent intent = new Intent(this, ChatPersonActivity.class);
                intent.putExtra("EXTRA_CHAT_NAME", targetUsername);
                intent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
                intent.putExtra("EXTRA_USE_EXISTING_SOCKET", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                
                int requestCode = targetUsername != null ? Math.abs(targetUsername.hashCode()) : 0;
                PendingIntent pi = PendingIntent.getActivity(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                Bitmap largeAvatarBmp = getAvatarBitmapForNotification(targetUsername, remoteAvatarUri, deviceAddress);

                Person.Builder personBuilder = new Person.Builder().setName(title);
                if (largeAvatarBmp != null) {
                    personBuilder.setIcon(IconCompat.createWithBitmap(largeAvatarBmp));
                }
                Person sender = personBuilder.build();
                Person me = new Person.Builder().setName("Вы").build();

                NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(me)
                        .addMessage(message, System.currentTimeMillis(), sender);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PrimeApplication.CHANNEL_MESSAGES_ID)
                        .setSmallIcon(R.drawable.ic_prime_statusbar)
                        .setStyle(style)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi);

                if (largeAvatarBmp != null) {
                    builder.setLargeIcon(largeAvatarBmp);
                }

                int notificationId = targetUsername != null ? Math.abs(targetUsername.hashCode()) : (int) System.currentTimeMillis();

                // 1. Direct Reply Action
                RemoteInput remoteInput = new RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
                        .setLabel("Ответить...")
                        .build();

                Intent replyIntent = new Intent(this, NotificationActionReceiver.class);
                replyIntent.setAction(NotificationActionReceiver.ACTION_REPLY);
                replyIntent.putExtra("EXTRA_CHAT_NAME", targetUsername);
                replyIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
                replyIntent.putExtra("EXTRA_NOTIFICATION_ID", notificationId);

                PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                        this,
                        notificationId + 1,
                        replyIntent,
                        PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                        R.drawable.ic_done,
                        "Ответить",
                        replyPendingIntent
                ).addRemoteInput(remoteInput).build();

                // 2. Mark as Read Action
                Intent markReadIntent = new Intent(this, NotificationActionReceiver.class);
                markReadIntent.setAction(NotificationActionReceiver.ACTION_MARK_READ);
                markReadIntent.putExtra("EXTRA_CHAT_NAME", targetUsername);
                markReadIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
                markReadIntent.putExtra("EXTRA_NOTIFICATION_ID", notificationId);

                PendingIntent markReadPendingIntent = PendingIntent.getBroadcast(
                        this,
                        notificationId + 2,
                        markReadIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                NotificationCompat.Action markReadAction = new NotificationCompat.Action.Builder(
                        R.drawable.ic_done_all,
                        "Отметить как прочитанное",
                        markReadPendingIntent
                ).build();

                builder.addAction(replyAction);
                builder.addAction(markReadAction);

                nm.notify(notificationId, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show background notification", e);
        }
    }

    private Bitmap getAvatarBitmapForNotification(String name, String avatarUri, String devAddress) {
        try {
            Bitmap bmp = null;
            File localAvatarFile = new File(getFilesDir(), "avatar_" + name + ".jpg");
            if (localAvatarFile.exists()) {
                bmp = BitmapFactory.decodeFile(localAvatarFile.getAbsolutePath());
            } else if (devAddress != null && !devAddress.isEmpty()) {
                File devAvatarFile = new File(getFilesDir(), "avatar_" + devAddress + ".jpg");
                if (devAvatarFile.exists()) {
                    bmp = BitmapFactory.decodeFile(devAvatarFile.getAbsolutePath());
                }
            } else if (avatarUri != null && !avatarUri.isEmpty()) {
                Uri uri = Uri.parse(avatarUri);
                if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                    File file = new File(uri.getPath());
                    if (file.exists()) {
                        bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    }
                }
            }
            
            if (bmp != null) {
                // Обязательно сжимаем аватарку для уведомления, чтобы избежать TransactionTooLargeException (лимит 1MB)
                if (bmp.getWidth() > 256 || bmp.getHeight() > 256) {
                    float ratio = Math.min(256f / bmp.getWidth(), 256f / bmp.getHeight());
                    bmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * ratio), Math.round(bmp.getHeight() * ratio), true);
                }
                return bmp;
            }
        } catch (Exception ignored) {}
        return createLetterAvatar(name, 120);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
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
        if (autoRetryHandler != null) autoRetryHandler.removeCallbacksAndMessages(null);

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
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                for (int i = 0; i < 3; i++) {
                    try {
                        tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("PrimeChat", UUID_CHAT);
                        if (tmp != null) break;
                    } catch (IOException e) {
                        Log.w(TAG, "Socket listen() attempt " + (i + 1) + " failed, retrying...", e);
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    }
                }
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
                Thread.sleep(50);
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
        private final Object writeLock = new Object();
        private final ExecutorService controlWriteExecutor = Executors.newSingleThreadExecutor();
        private final ExecutorService mediaWriteExecutor = Executors.newSingleThreadExecutor();
        private volatile boolean isMediaSendingCancelled = false;

        public void cancelCurrentMediaSend() {
            isMediaSendingCancelled = true;
        }

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
            // Отправляем рукопожатие и аватар сразу при старте
            try {
                SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                String currentUser = sharedPrefs.getString("current_user", "");
                String myDisplayName = sharedPrefs.getString(currentUser + "_name", currentUser);
                String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + (myDisplayName != null ? myDisplayName : localUsername) + ";version=" + getAppVersionCode(getApplicationContext());
                sendPacket(TYPE_TEXT, handshake.getBytes(StandardCharsets.UTF_8));
                sendLocalAvatar();
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
                    keepAliveHandler.postDelayed(this, 8000); // 8s ping keeps Bluetooth ACL link active
                }
            };
            keepAliveHandler.postDelayed(keepAliveRunnable, 8000);

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
                        if (isRemoteUserOnline) {
                            saveLastMessageToChatList(null, null, false, "ONLINE");
                            postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                        }
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
                            
                            if (!remoteName.isEmpty() && !Objects.equals(remoteName, targetUsername)) {
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
                                    sharedPrefs.edit().putString("persisted_chats", chatArray.toString()).apply();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to rename chat in persisted_chats", e);
                                }

                                final String finalName = remoteName;

                                saveLastMessageToChatList(null, null, false, "ONLINE");

                                runOnUiThread(() -> {
                                    if (tvChatName != null) tvChatName.setText(finalName);
                                    updateAvatarUi(remoteAvatarUri, finalName);
                                    if (chatAdapter != null) {
                                        List<ChatMessage> updatedHistory = ChatHistoryManager.loadMessages(ChatPersonActivity.this, finalName);
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
                            
                            if (!remoteName.isEmpty() && !Objects.equals(remoteName, targetUsername)) {
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
                                    sharedPrefs.edit().putString("persisted_chats", chatArray.toString()).apply();
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
                                        avatarFile.delete();
                                    }
                                    if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                        File devFile = new File(getFilesDir(), "avatar_" + deviceAddress + ".jpg");
                                        if (devFile.exists()) devFile.delete();
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
                                        avatarBmp.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                                        fos.flush();
                                        fos.close();
                                        
                                        if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                            try {
                                                File devFile = new File(getFilesDir(), "avatar_" + deviceAddress + ".jpg");
                                                FileOutputStream devFos = new FileOutputStream(devFile);
                                                avatarBmp.compress(Bitmap.CompressFormat.JPEG, 95, devFos);
                                                devFos.flush();
                                                devFos.close();
                                            } catch (Exception ignored) {}
                                        }

                                        remoteAvatarUri = Uri.fromFile(avatarFile).toString();
                                        saveLastMessageToChatList(null);
                                        
                                        if (activeUiHandler != null) {
                                            activeUiHandler.post(() -> {
                                                updateAvatarUi(remoteAvatarUri, targetUsername);
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
                        if (payload != null) {
                            String data = new String(payload, StandardCharsets.UTF_8);
                            String deletedByName = targetUsername;
                            if (data.startsWith("DELETE_CHAT:")) {
                                String[] parts = data.substring(12).split(";");
                                for (String p : parts) {
                                    if (p.startsWith("name=")) {
                                        deletedByName = p.substring(5);
                                    }
                                }
                            }
                            ChatHistoryManager.deleteHistoryCompletely(getApplicationContext(), deletedByName, deviceAddress);
                            deleteChatFromChatList(deletedByName);
                        }
                        ChatHistoryManager.deleteHistoryCompletely(getApplicationContext(), targetUsername, deviceAddress);
                        deleteChatFromChatList(targetUsername);
                        
                        BluetoothSocketHolder.clearSocket();
                        PrimeBluetoothService.stopService(getApplicationContext());
                        
                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_CHAT_DELETED, -1, -1, payload);
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
            controlWriteExecutor.shutdownNow();
            mediaWriteExecutor.shutdownNow();
        }

        private String extractMsgId(String data) {
            return ChatPersonActivity.extractMsgId(data);
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
            
            Handler uiH = activeUiHandler;
            if (uiH != null) {
                uiH.post(() -> addMessageToUI(incomingMessage));
            } else {
                ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, incomingMessage);
                saveLastMessageToChatList(parsed.realText, MessageStatus.NONE, true, "ONLINE");
                showBackgroundNotification(targetUsername, parsed.realText);
            }
        }

        private void processBackgroundPhotoMessage(byte[] fullPayload) {
            try {
                if (fullPayload == null || fullPayload.length == 0) return;

                String metaCheck = new String(fullPayload, 0, Math.min(fullPayload.length, 300), StandardCharsets.UTF_8);
                if (metaCheck.contains(":::MULTI:")) {
                    int headerEndIdx = -1;
                    String headerEndTag = ":::HEADER_END:::";
                    byte[] tagBytes = headerEndTag.getBytes(StandardCharsets.UTF_8);
                    for (int i = 0; i <= fullPayload.length - tagBytes.length; i++) {
                        boolean match = true;
                        for (int j = 0; j < tagBytes.length; j++) {
                            if (fullPayload[i + j] != tagBytes[j]) { match = false; break; }
                        }
                        if (match) { headerEndIdx = i; break; }
                    }

                    if (headerEndIdx != -1) {
                        String headerStr = new String(fullPayload, 0, headerEndIdx, StandardCharsets.UTF_8);
                        int bodyStart = headerEndIdx + tagBytes.length;

                        String photoMsgId = ChatPersonActivity.extractMsgId(headerStr);
                        String sizesPart = "";
                        int sizesIdx = headerStr.indexOf(":::SIZES:");
                        if (sizesIdx != -1) {
                            sizesPart = headerStr.substring(sizesIdx + 9);
                            int endSizes = sizesPart.indexOf(":::");
                            if (endSizes != -1) sizesPart = sizesPart.substring(0, endSizes);
                        }

                        String[] itemMetas = sizesPart.split(",");
                        List<ChatMessage.MediaItem> recMediaItems = new ArrayList<>();
                        int currentOffset = bodyStart;

                        long photoTs = System.currentTimeMillis();
                        String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));

                        for (int idx = 0; idx < itemMetas.length; idx++) {
                            try {
                                String metaStr = itemMetas[idx].trim();
                                String[] parts = metaStr.split("\\|");
                                boolean isVideo = parts.length >= 1 && "1".equals(parts[0]);
                                int pSize = parts.length >= 2 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
                                String durStr = parts.length >= 3 ? parts[2] : "00:00";
                                String ext = parts.length >= 4 ? parts[3] : (isVideo ? "mp4" : "jpg");

                                if (currentOffset + pSize <= fullPayload.length) {
                                    byte[] itemBytes = new byte[pSize];
                                    System.arraycopy(fullPayload, currentOffset, itemBytes, 0, pSize);
                                    currentOffset += pSize;

                                    File mediaFile = new File(getFilesDir(), "rec_media_" + (photoMsgId != null ? photoMsgId : photoTs) + "_" + idx + "." + ext);
                                    FileOutputStream fos = new FileOutputStream(mediaFile);
                                    fos.write(itemBytes);
                                    fos.flush();
                                    fos.close();

                                    recMediaItems.add(new ChatMessage.MediaItem(mediaFile.getAbsolutePath(), isVideo, durStr));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse multi-media item " + idx, e);
                            }
                        }

                        if (!recMediaItems.isEmpty()) {
                            ChatMessage multiMsg = new ChatMessage("", photoTime, targetUsername, false, null, photoTs, null, photoMsgId);
                            multiMsg.setMediaItems(recMediaItems);
                            
                            ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, multiMsg);
                            saveLastMessageToChatList("Медиафайлы (" + recMediaItems.size() + ")", MessageStatus.NONE, true, "ONLINE");

                            Handler uiH = activeUiHandler;
                            if (uiH != null) {
                                uiH.post(() -> addMessageToUI(multiMsg));
                            } else {
                                showBackgroundNotification(targetUsername, "Медиафайлы (" + recMediaItems.size() + ")");
                            }
                            return;
                        }
                    }
                }

                String photoMsgId = null;
                String captionText = null;
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

                if (photoBytes.length > 9) {
                    byte[] magic = "|PRM|".getBytes(StandardCharsets.UTF_8);
                    boolean hasMagic = true;
                    for (int i = 0; i < 5; i++) {
                        if (photoBytes[photoBytes.length - 5 + i] != magic[i]) {
                            hasMagic = false;
                            break;
                        }
                    }
                    if (hasMagic) {
                        int capLen = ((photoBytes[photoBytes.length - 9] & 0xFF) << 24) |
                                     ((photoBytes[photoBytes.length - 8] & 0xFF) << 16) |
                                     ((photoBytes[photoBytes.length - 7] & 0xFF) << 8) |
                                     (photoBytes[photoBytes.length - 6] & 0xFF);
                        if (capLen > 0 && capLen < photoBytes.length - 9) {
                            captionText = new String(photoBytes, photoBytes.length - 9 - capLen, capLen, StandardCharsets.UTF_8);
                            byte[] cleanPhoto = new byte[photoBytes.length - 9 - capLen];
                            System.arraycopy(photoBytes, 0, cleanPhoto, 0, cleanPhoto.length);
                            photoBytes = cleanPhoto;
                        }
                    }
                }

                String savedPhotoPath = null;
                if (photoBytes.length > 0) {
                    try {
                        File photoFile = new File(getFilesDir(), "rec_photo_" + (photoMsgId != null ? photoMsgId : System.currentTimeMillis()) + ".jpg");
                        FileOutputStream fos = new FileOutputStream(photoFile);
                        fos.write(photoBytes);
                        fos.flush();
                        fos.close();
                        savedPhotoPath = photoFile.getAbsolutePath();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to save background photo to disk", e);
                    }
                }

                Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                long photoTs = System.currentTimeMillis();
                String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                ChatMessage photoMsg = new ChatMessage(captionText, photoTime, targetUsername, false, photoBitmap, photoTs, savedPhotoPath, photoMsgId);
                photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
                
                Handler uiH = activeUiHandler;
                if (uiH != null) {
                    uiH.post(() -> addMessageToUI(photoMsg));
                } else {
                    ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, photoMsg);
                    saveLastMessageToChatList(captionText != null ? captionText : "Фотография", MessageStatus.NONE, true, "ONLINE");
                    showBackgroundNotification(targetUsername, captionText != null ? captionText : "Фотография");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process background photo", e);
            }
        }

        private void processBackgroundFileMessage(byte[] fullPayload) {
            if (fullPayload == null) return;
            ChatMessage fileMsg = parseFileMessageBytes(fullPayload, targetUsername);
            if (fileMsg == null) return;
            Handler uiH = activeUiHandler;
            if (uiH != null) {
                uiH.post(() -> addMessageToUI(fileMsg));
            } else {
                ChatHistoryManager.saveMessage(getApplicationContext(), targetUsername, fileMsg);
                String desc = (fileMsg.isVideo() ? "Видео: " : "Файл: ") + (fileMsg.getFileName() != null ? fileMsg.getFileName() : "Файл");
                saveLastMessageToChatList(desc, MessageStatus.NONE, true, "ONLINE");
                showBackgroundNotification(targetUsername, desc);
            }
        }

        public void sendPacket(byte type, byte[] payload) {
            if (mmOutStream == null) return;
            boolean isChunkedMedia = (type == TYPE_PHOTO || type == TYPE_FILE || type == TYPE_AVATAR);
            boolean showProgressUi = (type == TYPE_PHOTO || type == TYPE_FILE);
            ExecutorService executor = isChunkedMedia ? mediaWriteExecutor : controlWriteExecutor;

            if (isChunkedMedia) {
                isMediaSendingCancelled = false;
            }

            executor.execute(() -> {
                synchronized (writeLock) {
                    try {
                        mmOutStream.writeByte(type);
                        int length = payload != null ? payload.length : 0;
                        mmOutStream.writeInt(length);
                        if (length > 0 && payload != null) {
                            int offset = 0;
                            int chunkSize = isChunkedMedia ? 16384 : length;
                            while (offset < length) {
                                if (isMediaSendingCancelled) {
                                    Log.d(TAG, "Media send cancelled by user");
                                    isMediaSendingCancelled = false;
                                    postToUi(MESSAGE_SEND_CANCELLED, -1, -1, null);
                                    return;
                                }
                                int bytesToWrite = Math.min(chunkSize, length - offset);
                                mmOutStream.write(payload, offset, bytesToWrite);
                                offset += bytesToWrite;
                                mmOutStream.flush();

                                if (isChunkedMedia) {
                                    if (showProgressUi && length > 1024) {
                                        int progress = (int) ((offset * 100L) / length);
                                        postToUi(MESSAGE_SEND_PROGRESS, progress, -1, null);
                                    }
                                    if (type == TYPE_PHOTO || type == TYPE_FILE) {
                                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                                    }
                                }
                            }
                        } else {
                            mmOutStream.flush();
                        }
                        if (showProgressUi && !isMediaSendingCancelled) {
                            postToUi(MESSAGE_SEND_PROGRESS, 100, -1, null);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception during write", e);
                        if (showProgressUi) {
                            postToUi(MESSAGE_SEND_PROGRESS, -1, -1, null);
                        }
                    }
                }
            });
        }

        public void cancel() {
            try {
                if (keepAliveHandler != null && keepAliveRunnable != null) {
                    keepAliveHandler.removeCallbacks(keepAliveRunnable);
                }
                controlWriteExecutor.shutdownNow();
                mediaWriteExecutor.shutdownNow();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private byte[] getBytesFromPending(PendingAttachmentItem pending) {
        if (pending == null) return new byte[0];
        try {
            if (!pending.isVideo && !pending.isFile) {
                Bitmap bmp = null;
                if (pending.path != null && new File(pending.path).exists()) {
                    bmp = BitmapFactory.decodeFile(pending.path);
                } else if (pending.uri != null) {
                    try (InputStream is = getContentResolver().openInputStream(pending.uri)) {
                        if (is != null) bmp = BitmapFactory.decodeStream(is);
                    }
                }
                if (bmp != null) {
                    Bitmap scaled = scaleBitmapDown(bmp, 1280);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] compressed = baos.toByteArray();
                    if (!bmp.isRecycled() && bmp != scaled) bmp.recycle();
                    return compressed;
                }
            }

            InputStream is = null;
            if (pending.path != null && new File(pending.path).exists()) {
                is = Files.newInputStream(Paths.get(pending.path));
            } else if (pending.uri != null) {
                is = getContentResolver().openInputStream(pending.uri);
            }
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int len;
                long totalRead = 0;
                while ((len = is.read(buf)) != -1 && totalRead < 100 * 1024 * 1024L) {
                    baos.write(buf, 0, len);
                    totalRead += len;
                }
                is.close();
                return baos.toByteArray();
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
        boolean isVideo = (videoDuration != null && !Objects.equals(videoDuration, "00:00")) || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") || lowerName.endsWith(".webm");

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

    private void sendVideoOrFile(PendingAttachmentItem pending, String text) {
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

        String localSavedPath = pending.path;
        if (pending.isVideo && pending.uri != null) {
            if (localSavedPath == null || localSavedPath.startsWith("content://") || !new File(localSavedPath).exists()) {
                try {
                    File videoFile = new File(getFilesDir(), "sent_video_" + timestamp + "_" + fileName);
                    InputStream is = getContentResolver().openInputStream(pending.uri);
                    if (is != null) {
                        FileOutputStream fos = new FileOutputStream(videoFile);
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        fos.close();
                        is.close();
                        localSavedPath = videoFile.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save sent video locally", e);
                }
            }
        }

        ChatMessage msg = new ChatMessage(text, time, localUsername, true, pending.thumbnail, timestamp, localSavedPath, messageId);
        msg.setMessageType(type);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize > 0 ? fileSize : fileBytes.length);
        if (pending.isVideo) msg.setVideoDuration(durStr);

        if (replyingToMessage != null) {
            msg.setReplyToMessageId(replyingToMessage.getMessageId());
            msg.setReplyToSender(replyingToMessage.getSenderLogin());
            String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
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

        currentSendingMessageId = messageId;
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

    private static class PendingAttachmentItem {
        Uri uri;
        String path;
        String name;
        long size;
        boolean isVideo;
        boolean isFile;
        Bitmap thumbnail;
        String durationStr;

        PendingAttachmentItem(Uri uri, String path, String name, long size, boolean isVideo, boolean isFile, Bitmap thumbnail, String durationStr) {
            this.uri = uri;
            this.path = path;
            this.name = name;
            this.size = size;
            this.isVideo = isVideo;
            this.isFile = isFile;
            this.thumbnail = thumbnail;
            this.durationStr = durationStr;
        }

        String getFormatLabel() {
            if (isVideo) return "MP4";
            if (!isFile) return "IMG";
            if (name != null && name.contains(".")) {
                String ext = name.substring(name.lastIndexOf(".") + 1).toUpperCase(Locale.ROOT);
                if (ext.length() <= 5) return ext;
            }
            return "FILE";
        }
    }

    private static class PendingAttachment {
        List<PendingAttachmentItem> items = new ArrayList<>();

        long getTotalSize() {
            long sum = 0;
            for (PendingAttachmentItem item : items) sum += item.size;
            return sum;
        }
    }

    private class PendingCardsAdapter extends RecyclerView.Adapter<PendingCardsAdapter.ViewHolder> {
        private List<PendingAttachmentItem> items = new ArrayList<>();

        void setItems(List<PendingAttachmentItem> newItems) {
            this.items = new ArrayList<>(newItems != null ? newItems : new ArrayList<>());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PendingAttachmentItem item = items.get(position);
            holder.tvFormat.setText(item.getFormatLabel());
            holder.tvSize.setText(ChatAdapter.formatFileSize(item.size));

            if (item.isVideo) {
                holder.ivVideoBadge.setVisibility(View.VISIBLE);
                if (item.thumbnail != null) {
                    holder.ivThumbnail.setImageBitmap(item.thumbnail);
                } else {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_video);
                }
            } else if (item.isFile) {
                holder.ivVideoBadge.setVisibility(View.GONE);
                holder.ivThumbnail.setImageResource(R.drawable.ic_file);
            } else { // Photo
                holder.ivVideoBadge.setVisibility(View.GONE);
                if (item.thumbnail != null) {
                    holder.ivThumbnail.setImageBitmap(item.thumbnail);
                } else if (item.uri != null) {
                    holder.ivThumbnail.setImageURI(item.uri);
                } else {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_photo);
                }
            }

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < items.size()) {
                    PendingAttachmentItem removedItem = items.remove(pos);
                    if (currentPendingAttachment != null && currentPendingAttachment.items != null) {
                        currentPendingAttachment.items.remove(removedItem);
                    }
                    if (galleryAdapter != null) {
                        galleryAdapter.removeItemByUri(removedItem.uri, removedItem.path);
                    }
                    if (filesAdapter != null) {
                        filesAdapter.removeItemByUri(removedItem.uri, removedItem.path);
                    }
                    if (items.isEmpty()) {
                        clearPendingAttachment();
                    } else {
                        notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            ImageView ivVideoBadge;
            TextView tvFormat;
            TextView tvSize;
            View btnRemove;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.ivCardThumbnail);
                ivVideoBadge = itemView.findViewById(R.id.ivCardVideoBadge);
                tvFormat = itemView.findViewById(R.id.tvCardFormat);
                tvSize = itemView.findViewById(R.id.tvCardSize);
                btnRemove = itemView.findViewById(R.id.btnRemoveCard);
            }
        }
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
        rvPendingCards = findViewById(R.id.rvPendingCards);
        btnCancelPending = findViewById(R.id.btnCancelPending);

        if (rvPendingCards != null) {
            rvPendingCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            pendingCardsAdapter = new PendingCardsAdapter();
            rvPendingCards.setAdapter(pendingCardsAdapter);
        }

        if (btnCancelPending != null) {
            btnCancelPending.setOnClickListener(v -> clearPendingAttachment());
        }

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

        layoutSectionsContainer = findViewById(R.id.layoutSectionsContainer);
        layoutSectionCamera = findViewById(R.id.layoutSectionCamera);
        layoutSectionPhoto = findViewById(R.id.layoutSectionPhoto);
        layoutSectionFiles = findViewById(R.id.layoutSectionFiles);

        if (layoutSectionsContainer != null) {
            GestureDetector swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 80 && Math.abs(velocityX) > 150) {
                        if (diffX < 0) {
                            if (currentAttachmentMode < 2) {
                                switchAttachmentMode(currentAttachmentMode + 1);
                                return true;
                            }
                        } else {
                            if (currentAttachmentMode > 0) {
                                switchAttachmentMode(currentAttachmentMode - 1);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            layoutSectionsContainer.setOnTouchListener((v, event) -> {
                swipeDetector.onTouchEvent(event);
                return false;
            });
        }

        btnCameraPhoto = findViewById(R.id.btnCameraPhoto);
        btnCameraVideo = findViewById(R.id.btnCameraVideo);

        rvGalleryGrid = findViewById(R.id.rvGalleryGrid);
        rvFilesGrid = findViewById(R.id.rvFilesGrid);

        tvGalleryEmpty = findViewById(R.id.tvGalleryEmpty);
        tvFilesEmpty = findViewById(R.id.tvFilesEmpty);

        if (rvGalleryGrid != null) {
            rvGalleryGrid.setLayoutManager(new GridLayoutManager(this, 3));
            rvGalleryGrid.setHasFixedSize(true);
            rvGalleryGrid.setItemViewCacheSize(20);
            galleryAdapter = new GalleryGridAdapter();
            rvGalleryGrid.setAdapter(galleryAdapter);
        }

        if (rvFilesGrid != null) {
            rvFilesGrid.setLayoutManager(new GridLayoutManager(this, 3));
            rvFilesGrid.setHasFixedSize(true);
            rvFilesGrid.setItemViewCacheSize(20);
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
                    pickSystemGalleryLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                            .build()
                    );
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

    private View getSectionViewForMode(int mode) {
        if (mode == 0) return layoutSectionCamera;
        if (mode == 1) return layoutSectionPhoto;
        if (mode == 2) return layoutSectionFiles;
        return null;
    }

    private void switchAttachmentMode(int newMode) {
        int oldMode = currentAttachmentMode;
        View oldView = getSectionViewForMode(oldMode);
        View newView = getSectionViewForMode(newMode);

        currentAttachmentMode = newMode;

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

        if (newMode == 0) { // Camera
            if (vModeCameraBg != null) vModeCameraBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeCameraIcon != null) ImageViewCompat.setImageTintList(ivModeCameraIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeCameraLabel != null) tvModeCameraLabel.setTextColor(activeBrand);
        } else if (newMode == 1) { // Photo & Video Gallery
            if (vModePhotoBg != null) vModePhotoBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModePhotoIcon != null) ImageViewCompat.setImageTintList(ivModePhotoIcon, ColorStateList.valueOf(activeBrand));
            if (tvModePhotoLabel != null) tvModePhotoLabel.setTextColor(activeBrand);
            loadGalleryMediaAsync();
        } else if (newMode == 2) { // Files
            if (vModeFilesBg != null) vModeFilesBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeFilesIcon != null) ImageViewCompat.setImageTintList(ivModeFilesIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeFilesLabel != null) tvModeFilesLabel.setTextColor(activeBrand);
            loadFilesAsync();
        }

        if (oldView == null || newView == null || oldView == newView) {
            if (layoutSectionCamera != null) layoutSectionCamera.setVisibility(newMode == 0 ? View.VISIBLE : View.GONE);
            if (layoutSectionPhoto != null) layoutSectionPhoto.setVisibility(newMode == 1 ? View.VISIBLE : View.GONE);
            if (layoutSectionFiles != null) layoutSectionFiles.setVisibility(newMode == 2 ? View.VISIBLE : View.GONE);
            return;
        }

        float containerWidth = (layoutSectionsContainer != null && layoutSectionsContainer.getWidth() > 0)
                ? layoutSectionsContainer.getWidth()
                : getResources().getDisplayMetrics().widthPixels;

        boolean movingRight = newMode > oldMode;
        float oldTargetX = movingRight ? -containerWidth : containerWidth;
        float newStartX = movingRight ? containerWidth : -containerWidth;

        oldView.animate().cancel();
        newView.animate().cancel();

        newView.setTranslationX(newStartX);
        newView.setAlpha(0.2f);
        newView.setVisibility(View.VISIBLE);

        final View animOldView = oldView;
        oldView.animate()
                .translationX(oldTargetX)
                .alpha(0.2f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    animOldView.setVisibility(View.GONE);
                    animOldView.setTranslationX(0f);
                    animOldView.setAlpha(1f);
                })
                .start();

        newView.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator())
                .start();
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

        boolean isFile = !isVideo && !isPhotoMimeOrPath(uri, path);
        PendingAttachmentItem item = new PendingAttachmentItem(uri, path != null ? path : uri.toString(), name, size, isVideo, isFile, thumbnail, durationStr);

        PendingAttachment pending = new PendingAttachment();
        pending.items.add(item);

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        closeAttachmentPanel();
    }

    private void setPendingAttachmentFromUris(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Uri uri : uris) {
            String path = getPathFromUri(uri);
            boolean isVid = isVideoMimeOrPath(uri, path);
            String duration = isVid ? getVideoDurationFromUri(uri, path) : "00:00";
            MediaItem item = new MediaItem(uri, path != null ? path : uri.toString(), isVid, duration, System.currentTimeMillis(), getFileSizeFromUri(uri), getFileNameFromUri(uri));
            mediaItems.add(item);
        }
        setPendingAttachmentFromMediaItems(mediaItems, true);
    }

    private void setPendingAttachmentFromMediaItems(List<MediaItem> items, boolean closePanel) {
        if (items == null || items.isEmpty()) return;
        PendingAttachment pending = new PendingAttachment();
        for (MediaItem mi : items) {
            Bitmap thumb = null;
            if (mi.isVideo) {
                thumb = getVideoThumbnail(mi.path);
            } else if (mi.path != null && new File(mi.path).exists()) {
                thumb = BitmapFactory.decodeFile(mi.path);
            }
            PendingAttachmentItem item = new PendingAttachmentItem(
                mi.uri,
                mi.path != null ? mi.path : (mi.uri != null ? mi.uri.toString() : ""),
                mi.name != null ? mi.name : (mi.isVideo ? "Видео" : "Фотография"),
                mi.size,
                mi.isVideo,
                false,
                thumb,
                mi.durationStr
            );
            pending.items.add(item);
        }

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        if (closePanel) closeAttachmentPanel();
    }

    private void setPendingAttachmentFromFileItems(List<FileItem> items, boolean closePanel) {
        if (items == null || items.isEmpty()) return;
        PendingAttachment pending = new PendingAttachment();
        for (FileItem fi : items) {
            PendingAttachmentItem item = new PendingAttachmentItem(
                fi.uri,
                fi.path != null ? fi.path : (fi.uri != null ? fi.uri.toString() : ""),
                fi.name != null ? fi.name : "Файл",
                fi.size,
                false,
                true,
                null,
                "00:00"
            );
            pending.items.add(item);
        }

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        if (closePanel) closeAttachmentPanel();
    }

    private void showPendingAttachmentBar(PendingAttachment pending) {
        if (layoutPendingAttachment == null || pending == null || pending.items == null || pending.items.isEmpty()) return;

        if (pendingCardsAdapter != null) {
            pendingCardsAdapter.setItems(pending.items);
        }

        if (layoutPendingAttachment.getVisibility() != View.VISIBLE) {
            layoutPendingAttachment.setVisibility(View.VISIBLE);
            layoutPendingAttachment.setAlpha(0f);
            layoutPendingAttachment.setTranslationY(120f * getResources().getDisplayMetrics().density);
            layoutPendingAttachment.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .setListener(null)
                .start();
        }
    }

    private void clearPendingAttachment() {
        if (currentPendingAttachment == null && (layoutPendingAttachment == null || layoutPendingAttachment.getVisibility() != View.VISIBLE)) return;
        currentPendingAttachment = null;
        if (galleryAdapter != null) galleryAdapter.clearSelection();
        if (filesAdapter != null) filesAdapter.clearSelection();

        if (layoutPendingAttachment != null && layoutPendingAttachment.getVisibility() == View.VISIBLE) {
            layoutPendingAttachment.animate()
                .translationY(120f * getResources().getDisplayMetrics().density)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator(1.5f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutPendingAttachment.setVisibility(View.GONE);
                        layoutPendingAttachment.setTranslationY(0f);
                        layoutPendingAttachment.setAlpha(1f);
                    }
                })
                .start();
        }
    }

    private void showSendingProgressUi(int progress) {
        if (layoutSendingProgress == null) return;

        if (progress >= 0 && progress < 100) {
            if (pbSendingProgress != null) pbSendingProgress.setProgress(progress);
            if (tvSendingProgressPercent != null) {
                tvSendingProgressPercent.setText("Передача... " + progress + "%");
            }
            if (layoutSendingProgress.getVisibility() != View.VISIBLE) {
                layoutSendingProgress.setVisibility(View.VISIBLE);
                layoutSendingProgress.setAlpha(0f);
                layoutSendingProgress.setTranslationY(120f * getResources().getDisplayMetrics().density);
                layoutSendingProgress.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setListener(null)
                    .start();
            }
        } else {
            hideSendingProgressUi();
        }
    }

    private void hideSendingProgressUi() {
        if (layoutSendingProgress != null && layoutSendingProgress.getVisibility() == View.VISIBLE) {
            layoutSendingProgress.animate()
                .translationY(120f * getResources().getDisplayMetrics().density)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator(1.5f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutSendingProgress.setVisibility(View.GONE);
                        layoutSendingProgress.setTranslationY(0f);
                        layoutSendingProgress.setAlpha(1f);
                    }
                })
                .start();
        }
    }

    private void cancelCurrentFileSending() {
        if (connectedThread != null) {
            connectedThread.cancelCurrentMediaSend();
        }
        if (currentSendingMessageId != null) {
            final String msgIdToDelete = currentSendingMessageId;
            currentSendingMessageId = null;
            int pos = chatAdapter != null ? chatAdapter.findPositionByMessageId(msgIdToDelete) : -1;
            if (pos != -1) {
                Intent deleteMsgIntent = new Intent("com.messenger.prime.MSG_DELETED").setPackage(getPackageName());
                deleteMsgIntent.putExtra("messageId", msgIdToDelete);
                sendBroadcast(deleteMsgIntent);

                chatAdapter.deleteMessageAnimated(null, pos, () -> {
                    ChatHistoryManager.deleteSingleMessage(ChatPersonActivity.this, targetUsername, msgIdToDelete);
                    ChatMessage newLast = chatAdapter.getLastMessage();
                    saveLastMessageToChatList(newLast != null ? newLast.getText() : "");
                });
            } else {
                ChatHistoryManager.deleteSingleMessage(ChatPersonActivity.this, targetUsername, msgIdToDelete);
            }
        }
        if (layoutSendingProgress != null) {
            layoutSendingProgress.setVisibility(View.GONE);
        }
        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Отправка файла отменена", null);
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
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        return BitmapFactory.decodeStream(is);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Bitmap getVideoThumbnail(String pathOrUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (pathOrUri.startsWith("content://")) {
                retriever.setDataSource(this, Uri.parse(pathOrUri));
            } else {
                retriever.setDataSource(pathOrUri);
            }
            return retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
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
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String p = uri.getPath();
            if (p != null) return new File(p).getName();
        }
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
        ioExecutor.execute(() -> {
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
        ioExecutor.execute(() -> {
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

    private static final int MEM_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
    private final LruCache<String, Bitmap> galleryThumbnailCache = new LruCache<String, Bitmap>(MEM_CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private final ExecutorService galleryThumbnailExecutor = Executors.newFixedThreadPool(4);

    @SuppressWarnings("deprecation")
    private void loadGalleryThumbnailAsync(ImageView imageView, MediaItem item) {
        if (item == null || item.uri == null) return;
        String cacheKey = item.uri.toString();
        Bitmap cached = galleryThumbnailCache.get(cacheKey);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        imageView.setImageResource(R.drawable.ic_photo);
        imageView.setTag(cacheKey);

        galleryThumbnailExecutor.execute(() -> {
            Bitmap thumb = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    thumb = getContentResolver().loadThumbnail(item.uri, new Size(220, 220), null);
                } else {
                    if (item.isVideo && item.path != null) {
                        thumb = ThumbnailUtils.createVideoThumbnail(item.path, MediaStore.Images.Thumbnails.MINI_KIND);
                    } else if (item.path != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(item.path, options);
                        options.inSampleSize = calculateInSampleSize(options, 220, 220);
                        options.inJustDecodeBounds = false;
                        thumb = BitmapFactory.decodeFile(item.path, options);
                    }
                }
                if (thumb == null && item.uri != null) {
                    InputStream is = getContentResolver().openInputStream(item.uri);
                    if (is != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(is, null, options);
                        is.close();
                        options.inSampleSize = calculateInSampleSize(options, 220, 220);
                        options.inJustDecodeBounds = false;
                        InputStream is2 = getContentResolver().openInputStream(item.uri);
                        if (is2 != null) {
                            thumb = BitmapFactory.decodeStream(is2, null, options);
                            is2.close();
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (thumb != null) {
                galleryThumbnailCache.put(cacheKey, thumb);
                final Bitmap finalThumb = thumb;
                runOnUiThread(() -> {
                    if (Objects.equals(cacheKey, imageView.getTag())) {
                        imageView.setImageBitmap(finalThumb);
                    }
                });
            }
        });
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

    private class GalleryGridAdapter extends RecyclerView.Adapter<GalleryGridAdapter.ViewHolder> {
        private List<MediaItem> items = new ArrayList<>();
        private final List<MediaItem> selectedItems = new ArrayList<>();

        void setItems(List<MediaItem> newItems) {
            this.items = new ArrayList<>(newItems);
            selectedItems.clear();
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

            loadGalleryThumbnailAsync(holder.ivGalleryThumbnail, item);

            boolean isSel = selectedItems.contains(item);
            if (holder.layoutSelectionBadge != null) {
                if (isSel) {
                    holder.layoutSelectionBadge.setVisibility(View.VISIBLE);
                    if (holder.tvSelectionIndex != null) {
                        holder.tvSelectionIndex.setText(String.valueOf(selectedItems.indexOf(item) + 1));
                    }
                } else {
                    holder.layoutSelectionBadge.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                    notifyDataSetChanged();
                } else {
                    if (selectedItems.size() < 20) {
                        selectedItems.add(item);
                        notifyDataSetChanged();
                    } else {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Максимум 20 медиафайлов", null);
                    }
                }

                if (!selectedItems.isEmpty()) {
                    setPendingAttachmentFromMediaItems(selectedItems, false);
                } else {
                    currentPendingAttachment = null;
                    if (layoutPendingAttachment != null) layoutPendingAttachment.setVisibility(View.GONE);
                }
            });
        }

        void removeItemByUri(Uri uri, String path) {
            selectedItems.removeIf(mi -> (uri != null && uri.equals(mi.uri)) || (path != null && !path.isEmpty() && path.equals(mi.path)));
            notifyDataSetChanged();
        }

        void clearSelection() {
            selectedItems.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivGalleryThumbnail;
            View layoutVideoBadge;
            TextView tvVideoDuration;
            View layoutSelectionBadge;
            TextView tvSelectionIndex;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivGalleryThumbnail = itemView.findViewById(R.id.ivGalleryThumbnail);
                layoutVideoBadge = itemView.findViewById(R.id.layoutVideoBadge);
                tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
                layoutSelectionBadge = itemView.findViewById(R.id.layoutSelectionBadge);
                tvSelectionIndex = itemView.findViewById(R.id.tvSelectionIndex);
            }
        }
    }

    private class FileGridAdapter extends RecyclerView.Adapter<FileGridAdapter.ViewHolder> {
        private List<FileItem> items = new ArrayList<>();
        private final List<FileItem> selectedFileItems = new ArrayList<>();

        void setItems(List<FileItem> newItems) {
            this.items = new ArrayList<>(newItems);
            selectedFileItems.clear();
            notifyDataSetChanged();
        }

        void clearSelection() {
            selectedFileItems.clear();
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

            boolean isSel = selectedFileItems.contains(item);
            if (holder.layoutSelectionBadge != null) {
                if (isSel) {
                    holder.layoutSelectionBadge.setVisibility(View.VISIBLE);
                    if (holder.tvSelectionIndex != null) {
                        holder.tvSelectionIndex.setText(String.valueOf(selectedFileItems.indexOf(item) + 1));
                    }
                } else {
                    holder.layoutSelectionBadge.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.size > 2 * 1024 * 1024 * 1024L) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Превышен лимит размера файла (до 2 ГБ)", null);
                    return;
                }

                if (selectedFileItems.contains(item)) {
                    selectedFileItems.remove(item);
                    notifyDataSetChanged();
                } else {
                    if (selectedFileItems.size() < 20) {
                        selectedFileItems.add(item);
                        notifyDataSetChanged();
                    } else {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Максимум 20 файлов", null);
                    }
                }

                if (!selectedFileItems.isEmpty()) {
                    setPendingAttachmentFromFileItems(selectedFileItems, false);
                } else {
                    currentPendingAttachment = null;
                    if (layoutPendingAttachment != null) layoutPendingAttachment.setVisibility(View.GONE);
                }
            });
        }

        void removeItemByUri(Uri uri, String path) {
            selectedFileItems.removeIf(fi -> (uri != null && uri.equals(fi.uri)) || (path != null && !path.isEmpty() && path.equals(fi.path)));
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivFileIcon;
            TextView tvFileName;
            TextView tvFileSize;
            View layoutSelectionBadge;
            TextView tvSelectionIndex;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                layoutSelectionBadge = itemView.findViewById(R.id.layoutSelectionBadge);
                tvSelectionIndex = itemView.findViewById(R.id.tvSelectionIndex);
            }
        }
    }
}
