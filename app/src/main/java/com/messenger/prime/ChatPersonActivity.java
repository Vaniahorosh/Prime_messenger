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
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.WindowManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.net.Uri;
import android.os.Environment;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import android.util.Log;
import android.util.LruCache;
import android.util.Size;
import android.media.ThumbnailUtils;
import android.view.GestureDetector;
import android.view.Gravity;

import android.view.HapticFeedbackConstants;
import android.view.animation.AccelerateInterpolator;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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

public class ChatPersonActivity extends AppCompatActivity implements BluetoothConnectionManager.ConnectionCallback, PrimeMusicManager.PlayerListener {

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
    private kotlin.jvm.functions.Function2<Byte, byte[], kotlin.Unit> companionPacketListener;
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
    private volatile String pendingReadReceiptMsgId = null;
    private static final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    public static volatile String activeSendingMessageId = null;
    public static volatile String activeSendingTargetUsername = null;

    private void runOnIoThread(Runnable runnable) {
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            try {
                ioExecutor.execute(runnable);
            } catch (Exception ignored) {}
        }
    }

    private String targetUsername;
    private String deviceAddress;
    private boolean isOnlineExpected;
    private String localUsername;

    private BluetoothAdapter bluetoothAdapter;

    private static final Set<String> processedIncomingMsgIds = Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>(200, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Entry<String, Boolean> eldest) {
                    return size() > 300;
                }
            }
    );

    private boolean isDuplicatePacket(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) return false;
        if (!processedIncomingMsgIds.add(msgId.trim())) {
            Log.w(TAG, "DDoS Guard: Duplicate packet dropped for msgId: " + msgId);
            return true;
        }
        return false;
    }

    private TextView tvChatStatus;
    private eightbitlab.com.blurview.BlurView layoutMusicIsland;
    private View layoutVersionInfo;
    private TextView tvVersionInfoText;
    private ImageButton btnVersionInfoClose;
    private View layoutMusicSection;
    private boolean isVersionInfoDismissed = false;
    private TextView tvMusicIslandTitle, tvMusicIslandTime;
    private ProgressBar pbMusicIslandProgress;
    private ImageView btnMusicIslandPlayPause;
    private ImageButton btnMusicIslandClose;
    private TextView tvChatName;
    private TextView tvFloatingDate;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ChatAdapter chatAdapter;
    private Handler handler;

    private View layoutHeaderNormal;
    private View layoutHeaderSelection;
    private ImageButton btnCancelSelection;
    private TextView tvSelectionCounter;
    private ImageButton btnMultiForward;
    private ImageButton btnMultiCopy;
    private ImageButton btnMultiDelete;
    private boolean isMultiSelectionActive = false;

    private View layoutInput;
    private ImageButton btnAttach;
    private ImageButton btnSend;
    private ImageButton btnVoiceVideoNote;
    private boolean isVideoNoteMode = false;
    private ImageButton btnCancelEdit;
    private ImageButton btnCloseEditBar;
    private View layoutEditBar;
    private View layoutActionIsland;

    private View layoutPullIndicator;
    private ProgressBar pbPullProgress;
    private TextView tvPullStatus;
    private ImageView ivPullCheck;
    private boolean isPullToOpenActive = false;
    private boolean stage1HapticTriggered = false;
    private float pullStartY = 0f;
    private float pullStartX = 0f;

    private View layoutReplyBar;
    private TextView tvReplyBarTitle;
    private TextView tvReplyBarText;
    private ImageButton btnCloseReplyBar;
    private ChatMessage replyingToMessage = null;
    private String replyingToText = null;

    // Attachment Panel & Pending Attachment Views
    private BottomSheetDialog attachmentDialog;
    private View btnConfirmAttachment;
    private View layoutConfirmContainer;
    private TextView tvConfirmBadgeCounter;
    private int lastConfirmCount = -1;
    private View layoutAttachmentPanel;
    private LinearLayout layoutPendingAttachment;
    private RecyclerView rvPendingCards;
    private PendingCardsAdapter pendingCardsAdapter;
    private LinearLayout layoutSendingProgress;
    private com.messenger.prime.PrimeWaterLoadingView pbSendingProgress;
    private TextView tvSendingProgressPercent;
    private ImageButton btnCancelSendingProgress;
    private FloatingActionButton fabScrollToBottom;
    private View btnCancelPending;
    private String currentSendingMessageId = null;

    // Attachment Panel Modes UI
    private LinearLayout layoutModeCamera, layoutModePhoto, layoutModeFiles, layoutModeMusic;
    private FrameLayout vModeCameraBg, vModePhotoBg, vModeFilesBg, vModeMusicBg;
    private ImageView ivModeCameraIcon, ivModePhotoIcon, ivModeFilesIcon, ivModeMusicIcon;
    private TextView tvModeCameraLabel, tvModePhotoLabel, tvModeFilesLabel, tvModeMusicLabel;

    // Attachment Panel Content Sections UI
    private FrameLayout layoutSectionsContainer;
    private View layoutSectionCamera, layoutSectionPhoto, layoutSectionFiles, layoutSectionMusic;
    
    private RecyclerView rvMusicGrid;
    private TextView tvMusicEmpty;
    private View btnMusicTabDevice, btnMusicTabSaved;
    private MusicTrackAdapter musicAdapter;
    private List<com.messenger.prime.TrackItem> currentMusicList = new ArrayList<>();
    private boolean showingSavedMusic = false;
    private RecyclerView rvGalleryGrid, rvFilesGrid;
    private TextView tvGalleryEmpty, tvFilesEmpty;

    // Attachment State
    private boolean isAttachmentPanelOpen = false;
    private PrimeVideoMessageRecorder videoMessageRecorder;
    // Slidr removed, using Predictive Back
    private com.messenger.prime.VideoNoteIslandManager videoNoteIslandManager;
    private com.messenger.prime.FloatingPipManager floatingPipManager;
    private com.messenger.prime.VideoNoteMediaSessionManager videoNoteMediaSessionManager;
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

    private int dragStartPos = -1;
    private int lastDragPos = -1;

    private boolean isChatDeleted = false;
    private boolean isManuallyDisconnected = false;
    private boolean isRemoteUserOnline = false;
    private boolean isOpeningSubActivity = false;
    private String currentActivityState = "IDLE";
    private String myLocalActivityState = "IDLE";
    private int remoteVersionCode = -1;
    private boolean remoteHasVideoNotesSupport = true;

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
            setStatusWithAnimation("В сети", R.color.prime_success);
            checkAndUpdateVersionInfoIsland();
        } else {
            updateOfflineLastSeenStatus();
        }
    };

    private int connectionRetryCount = 0;
    private final Handler autoRetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed() && !isManuallyDisconnected && !isConnectionActive()) {
                connectionRetryCount++;
                Log.d(TAG, "Auto-retrying connection attempt " + connectionRetryCount + "...");
                startPrimeConnection();
            }
        }
    };

    private final Handler connectTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnectionActive()) return;
            if (!isConnectionActive()) {
                stopAnimatingStatus();
                updateOfflineLastSeenStatus();
                setOnlineStatusIndicator(false);
                updateInputVisibility(false);
                handleDisconnectionUiAndAutoReconnect();
            }
        }
    };

    private final Handler dateHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideDateRunnable = () -> {
        if (tvFloatingDate != null) {
            tvFloatingDate.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> tvFloatingDate.setVisibility(View.GONE))
                    .start();
        }
    };

    public static class ParsedMessagePayload {
        public String msgId = null;
        public String realText = "";
        public String replyToId = null;
        public String replyToSender = null;
        public String replyToText = null;

        public static ParsedMessagePayload parse(String data) {
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
                    for (Uri u : uris) {
                        try {
                            getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Throwable ignored) {}
                    }
                    setPendingAttachmentFromUris(uris);
                }
            });

    private final ActivityResultLauncher<String> pickSystemFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Throwable ignored) {}
                    setPendingAttachmentFromUri(uri, isVideoMimeOrPath(uri, getPathFromUri(uri)), "Файл из менеджера");
                }
            });

    public void sendActivityState(String stateStr) {
        if (stateStr != null) myLocalActivityState = stateStr;
        sendPacket(TYPE_TYPING, ("STATE:" + stateStr).getBytes(StandardCharsets.UTF_8));
    }

    private final ActivityResultLauncher<Intent> editPhotoLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                isOpeningSubActivity = false;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String editedUriStr = result.getData().getStringExtra("EDITED_IMAGE_URI");
                    if (editedUriStr == null || editedUriStr.isEmpty()) {
                        editedUriStr = result.getData().getStringExtra("EXTRA_IMAGE_URI");
                    }
                    if (editedUriStr != null && !editedUriStr.isEmpty()) {
                        Uri editedUri = Uri.parse(editedUriStr);
                        if (currentPendingAttachment != null && currentPendingAttachment.items != null && !currentPendingAttachment.items.isEmpty()) {
                            for (PendingAttachmentItem item : currentPendingAttachment.items) {
                                if (item.messageType == ChatMessage.MessageType.IMAGE) {
                                    item.uri = editedUri;
                                    item.path = getPathFromUri(editedUri);
                                    if (item.path == null || item.path.isEmpty()) item.path = editedUriStr;
                                    item.thumbnail = getPhotoThumbnail(editedUri, item.path);
                                    break;
                                }
                            }
                            if (pendingCardsAdapter != null) {
                                pendingCardsAdapter.setItems(currentPendingAttachment.items);
                            }
                        } else {
                            sendPhoto(editedUri);
                        }
                    }
                }
            });

    private final SharedPreferences.OnSharedPreferenceChangeListener profileChangeListener = (sharedPreferences, key) -> {
        try {
            String currentUser = sharedPreferences.getString("current_user", "");
            if (key != null && (key.equals("my_name") || key.equals("my_avatar") || key.equals("my_local_name") || key.equals("my_local_avatar") || key.equals("current_user_name") || key.equals(currentUser + "_name") || key.equals(currentUser + "_avatar"))) {
                reloadLocalProfileFromSettings();
                if (isConnectionActive()) {
                    long now = System.currentTimeMillis();
                    String myPresence = isActivityForeground ? "ONLINE" : ("OFFLINE:" + now);
                    String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + localUsername + ";version=" + getAppVersionCode(getApplicationContext()) + ";features=vn;presence=" + myPresence;
                    sendPacket(TYPE_TEXT, handshake.getBytes(StandardCharsets.UTF_8));
                    sendLocalAvatar(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            }

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


            boolean isTabletDevice = getResources().getConfiguration().smallestScreenWidthDp >= 600;
            if (isTabletDevice) {
                boolean isEmbedded = false;
                try {
                    isEmbedded = androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this);
                } catch (Exception ignored) {}

                if (!isEmbedded) {
                    Intent chatListIntent = new Intent(this, ChatListActivity.class);
                    chatListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(chatListIntent);

                    Intent selfIntent = new Intent(getIntent());
                    startActivity(selfIntent);
                    finish();
                    return;
                }
            }

            setContentView(R.layout.activity_chat_person_content);

        try {
        } catch (Exception ignored) {}

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (videoMessageRecorder != null && videoMessageRecorder.isRecordingOrPreviewing()) {
                    videoMessageRecorder.cancelRecording();
                } else if (isMultiSelectionActive) {
                    exitMultiSelectionMode();
                } else if (isAttachmentPanelOpen) {
                    closeAttachmentPanel();
                } else if (isEditMode) {
                    exitEditMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        
        UIExtensionsKt.setupEdgeToEdge(this, !isDarkTheme);
        setupChatBlurViews();

        View chatRoot = findViewById(R.id.chatRoot);
        if (chatRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                float density = getResources().getDisplayMetrics().density;
                int statusBarTop = Math.max(systemBars.top > 0 ? systemBars.top : getStatusBarHeight(), cutout.top);

                v.setPadding(cutout.left, 0, cutout.right, 0);

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
                        lp.topMargin = (int) (6 * density);
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

        layoutHeaderNormal = findViewById(R.id.layoutHeaderNormal);
        layoutHeaderSelection = findViewById(R.id.layoutHeaderSelection);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        tvSelectionCounter = findViewById(R.id.tvSelectionCounter);
        btnMultiForward = findViewById(R.id.btnMultiForward);
        btnMultiCopy = findViewById(R.id.btnMultiCopy);
        btnMultiDelete = findViewById(R.id.btnMultiDelete);

        if (btnCancelSelection != null) {
            btnCancelSelection.setOnClickListener(v -> exitMultiSelectionMode());
        }
        if (btnMultiCopy != null) {
            btnMultiCopy.setOnClickListener(v -> performMultiCopy());
        }
        if (btnMultiForward != null) {
            btnMultiForward.setOnClickListener(v -> performMultiForward());
        }
        if (btnMultiDelete != null) {
            btnMultiDelete.setOnClickListener(v -> performMultiDelete());
        }
        
        layoutMusicIsland = findViewById(R.id.layoutMusicIsland);
        layoutVersionInfo = findViewById(R.id.layoutVersionInfo);
        tvVersionInfoText = findViewById(R.id.tvVersionInfoText);
        btnVersionInfoClose = findViewById(R.id.btnVersionInfoClose);
        layoutMusicSection = findViewById(R.id.layoutMusicSection);

        tvMusicIslandTitle = findViewById(R.id.tvMusicIslandTitle);
        tvMusicIslandTime = findViewById(R.id.tvMusicIslandTime);
        pbMusicIslandProgress = findViewById(R.id.pbMusicIslandProgress);
        btnMusicIslandPlayPause = findViewById(R.id.btnMusicIslandPlayPause);
        btnMusicIslandClose = findViewById(R.id.btnMusicIslandClose);

        if (layoutMusicIsland != null) {
            layoutMusicIsland.setOnClickListener(v -> {
                if (layoutMusicSection != null && layoutMusicSection.getVisibility() == View.VISIBLE) {
                    com.messenger.prime.TrackItem currentTrack = PrimeMusicManager.INSTANCE.getCurrentTrack();
                    java.util.List<com.messenger.prime.TrackItem> playlist = PrimeMusicManager.INSTANCE.getCurrentPlaylist();
                    if (currentTrack != null) {
                        PrimeMusicPlayerDialog dialog = new PrimeMusicPlayerDialog(this);
                        dialog.show(null, playlist);
                    }
                }
            });
        }

        if (btnVersionInfoClose != null) {
            btnVersionInfoClose.setOnClickListener(v -> {
                isVersionInfoDismissed = true;
                if (layoutVersionInfo != null) layoutVersionInfo.setVisibility(View.GONE);
                updateIslandVisibility();
            });
        }

        if (btnMusicIslandClose != null) {
            btnMusicIslandClose.setOnClickListener(v -> {
                PrimeMusicManager.INSTANCE.stopAndReleasePlayer(this);
                if (layoutMusicSection != null) layoutMusicSection.setVisibility(View.GONE);
                updateIslandVisibility();
            });
        }

        if (btnMusicIslandPlayPause != null) {
            btnMusicIslandPlayPause.setOnClickListener(v -> {
                PrimeMusicManager.INSTANCE.togglePlayPause(this);
            });
        }
        
        PrimeMusicManager.INSTANCE.addListener(this);
        updateMusicIslandUI(PrimeMusicManager.INSTANCE.getCurrentTrack());
        
        tvFloatingDate = findViewById(R.id.tvFloatingDate);
        if (tvFloatingDate != null) {
            tvFloatingDate.bringToFront();
            int accentColor = ColorAccentManager.getCurrentAccentColor(this);
            tvFloatingDate.setTextColor(accentColor);
        }
        rvMessages = findViewById(R.id.rvMessages);
        
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && chatAdapter != null && chatAdapter.getItemCount() > 0) {
                final int lastIdx = chatAdapter.getItemCount() - 1;
                if (lastIdx >= 0 && rvMessages != null) {
                    rvMessages.postDelayed(() -> {
                        if (chatAdapter != null && lastIdx < chatAdapter.getItemCount()) {
                            rvMessages.smoothScrollToPosition(lastIdx);
                        }
                    }, 100);
                }
            }
        });

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (chatAdapter == null) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int firstVisible = lm.findFirstVisibleItemPosition();
                int lastVisible = lm.findLastVisibleItemPosition();
                int startIdx = Math.max(0, firstVisible);
                for (int i = startIdx; i <= lastVisible && i < chatAdapter.getItemCount(); i++) {
                    ChatMessage msg = chatAdapter.getMessageAt(i);
                    if (msg != null && msg.isFirstUnread()) {
                        msg.setFirstUnread(false);
                        final int itemIdx = i;
                        recyclerView.post(() -> {
                            if (chatAdapter != null && itemIdx >= 0 && itemIdx < chatAdapter.getItemCount()) {
                                chatAdapter.notifyItemChanged(itemIdx);
                            }
                        });

                        if (msg.getMessageId() != null) {
                            sendPacket(TYPE_READ_RECEIPT, msg.getMessageId().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        });

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoiceVideoNote = findViewById(R.id.btnVoiceVideoNote);

        SharedPreferences spMediaRecord = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        isVideoNoteMode = spMediaRecord.getBoolean("chat_media_record_mode_is_video", false);

        View layoutVoiceRecordBar = findViewById(R.id.layoutVoiceRecordBar);
        TextView tvVoiceRecordTimer = findViewById(R.id.tvVoiceRecordTimer);
        VoiceMessageItemView voiceRecordWaveform = findViewById(R.id.voiceRecordWaveform);
        ImageButton btnVoiceRecordCancel = findViewById(R.id.btnVoiceRecordCancel);
        ImageButton btnVoiceRecordSend = findViewById(R.id.btnVoiceRecordSend);
        View btnAttach = findViewById(R.id.btnAttach);

        if (btnVoiceVideoNote != null && layoutVoiceRecordBar != null) {
            btnVoiceVideoNote.setImageResource(isVideoNoteMode ? R.drawable.ic_camera : R.drawable.ic_mic);

            final PrimeVoiceRecorderController voiceController = new PrimeVoiceRecorderController(
                this,
                this,
                layoutVoiceRecordBar,
                etMessage,
                tvVoiceRecordTimer,
                voiceRecordWaveform,
                btnVoiceRecordCancel,
                btnVoiceRecordSend,
                btnAttach,
                new PrimeVoiceRecorderController.VoiceRecordListener() {
                    @Override
                    public void onVoiceRecorded(@NonNull File file, @NonNull List<Integer> amplitudes) {
                        sendVoiceMessage(file.getAbsolutePath(), amplitudes);
                    }

                    @Override
                    public void onRecordingStateChanged(boolean isRecording) {
                        if (isRecording) {
                            sendActivityState("RECORDING_VOICE");
                        } else {
                            sendActivityState("IDLE");
                        }
                    }
                }
            );

            PrimeMediaPlaybackService.observePlaybackState(this, (msgId, state) -> {
                runOnUiThread(() -> {
                    if (chatAdapter != null) {
                        chatAdapter.updateVoicePlaybackState(msgId, state);
                    }
                });
            });

            final Handler recordHandler = new Handler(Looper.getMainLooper());
            final float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            final float[] startX = new float[1];
            final float[] startY = new float[1];
            final long[] downTime = new long[1];
            final boolean[] isLongPressTriggered = new boolean[1];
            final boolean[] isVoiceLocked = new boolean[1];

            final Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPressTriggered[0] = true;
                    btnVoiceVideoNote.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Log.d("VoiceDebug", "Long press triggered. isVideoNoteMode: " + isVideoNoteMode);

                    if (isVideoNoteMode) {
                        if (videoMessageRecorder != null) {
                            videoMessageRecorder.startVideoRecordingIfPermitted();
                        }
                    } else {
                        voiceController.startRecordingProcess();
                    }
                }
            };

            btnVoiceVideoNote.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("VoiceDebug", "ACTION_DOWN on btnVoiceVideoNote");
                        startX[0] = event.getRawX();
                        startY[0] = event.getRawY();
                        downTime[0] = System.currentTimeMillis();
                        isLongPressTriggered[0] = false;
                        isVoiceLocked[0] = false;

                        if (!isConnectionActive()) {
                            Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        PrimePredictiveBack.setGestureLocked(true);
                        recordHandler.postDelayed(longPressRunnable, 180);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX[0];
                        float dy = event.getRawY() - startY[0];
                        float dist = (float) Math.hypot(dx, dy);

                        if (!isLongPressTriggered[0]) {
                            if (dist > touchSlop) {
                                recordHandler.removeCallbacks(longPressRunnable);
                            }
                        } else {
                            if (isVideoNoteMode) {
                                if (videoMessageRecorder != null) {
                                    videoMessageRecorder.handleVideoMove(startX[0], startY[0], event.getRawX(), event.getRawY());
                                }
                            } else {
                                if (dy < -180f) {
                                    isVoiceLocked[0] = true;
                                    Log.d("VoiceDebug", "Voice recording locked (Hands-free)");
                                }
                                if (dx < -200f) {
                                    voiceController.cancelRecording();
                                }
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        Log.d("VoiceDebug", "ACTION_UP on btnVoiceVideoNote");
                        recordHandler.removeCallbacks(longPressRunnable);

                        long duration = System.currentTimeMillis() - downTime[0];
                        float releaseDist = (float) Math.hypot(event.getRawX() - startX[0], event.getRawY() - startY[0]);

                        if (!isLongPressTriggered[0]) {
                            if (duration < 250 && releaseDist <= touchSlop) {
                                Log.d("VoiceDebug", "Short tap detected -> toggling mode");
                                btnVoiceVideoNote.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                
                                btnVoiceVideoNote.setScaleX(0.8f);
                                btnVoiceVideoNote.setScaleY(0.8f);
                                btnVoiceVideoNote.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();

                                toggleVoiceVideoNoteMode();
                            }
                            PrimePredictiveBack.setGestureLocked(false);
                        } else {
                            if (isVideoNoteMode) {
                                if (videoMessageRecorder != null) {
                                    videoMessageRecorder.handleVideoUp();
                                }
                                PrimePredictiveBack.setGestureLocked(false);
                            } else {
                                if (!isVoiceLocked[0]) {
                                    voiceController.stopAndSendRecording();
                                    PrimePredictiveBack.setGestureLocked(false);
                                } else {
                                    Log.d("VoiceDebug", "ACTION_UP ignored: hands-free mode active");
                                }
                            }
                        }
                        isLongPressTriggered[0] = false;
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        Log.d("VoiceDebug", "ACTION_CANCEL on btnVoiceVideoNote");
                        recordHandler.removeCallbacks(longPressRunnable);
                        PrimePredictiveBack.setGestureLocked(false);
                        if (isLongPressTriggered[0]) {
                            if (isVideoNoteMode) {
                                if (videoMessageRecorder != null) {
                                    videoMessageRecorder.cancelRecording();
                                }
                            } else {
                                voiceController.cancelRecording();
                            }
                        }
                        isLongPressTriggered[0] = false;
                        return true;

                    default:
                        return false;
                }
            });
        }
        try {
            initVideoMessageRecorder();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize video message recorder", t);
        }
        updateActionButtonsVisibility(false);
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
        
        layoutInput = findViewById(R.id.layoutInput);
        btnAttach = findViewById(R.id.btnAttach);
        if (btnAttach != null) {
            btnAttach.setEnabled(true);
            btnAttach.setClickable(true);
            boolean activeOnStart = isConnectionActive();
            btnAttach.setAlpha(activeOnStart ? 1.0f : 0.4f);
            btnAttach.setOnClickListener(v -> {
                if (!isConnectionActive()) {
                    Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                    v.setAlpha(0.4f);
                    closeAttachmentPanel();
                    return;
                }
                hideSoftKeyboard();
                toggleAttachmentPanel();
            });
        }
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnCloseEditBar = findViewById(R.id.btnCloseEditBar);
        layoutEditBar = findViewById(R.id.layoutEditBar);
        layoutActionIsland = findViewById(R.id.layoutActionIsland);
        layoutReplyBar = findViewById(R.id.layoutReplyBar);
        tvReplyBarTitle = findViewById(R.id.tvReplyBarTitle);
        tvReplyBarText = findViewById(R.id.tvReplyBarText);
        btnCloseReplyBar = findViewById(R.id.btnCloseReplyBar);

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
        if (layoutReplyBar != null) {
            layoutReplyBar.setOnClickListener(v -> {
                if (replyingToMessage != null && replyingToMessage.getMessageId() != null) {
                    jumpToTargetMessage(replyingToMessage.getMessageId(), replyingToText);
                }
            });
        }
        if (layoutEditBar != null) {
            layoutEditBar.setOnClickListener(v -> {
                if (editingMessageId != null) {
                    jumpToTargetMessage(editingMessageId, null);
                }
            });
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
        isOnlineExpected = intent.getBooleanExtra("EXTRA_IS_ONLINE", false);
        String avatarUri = intent.getStringExtra("EXTRA_AVATAR_URI");
        if (avatarUri != null && !avatarUri.isEmpty()) {
            remoteAvatarUri = avatarUri;
        }
        SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String myName = sharedPreferences.getString("my_name", null);
        if (myName == null || myName.isEmpty()) myName = sharedPreferences.getString("my_local_name", null);
        if (myName == null || myName.isEmpty()) myName = sharedPreferences.getString("current_user_name", null);

        String resolvedName = AvatarManager.getContactDisplayName(this, deviceAddress, targetUsername, targetUsername);
        if (resolvedName != null && !resolvedName.isEmpty() && !isValidMacAddress(resolvedName) && !"Собеседник".equals(resolvedName)) {
            targetUsername = resolvedName;
        } else if (targetUsername == null || targetUsername.isEmpty() || targetUsername.equals("Собеседник")) {
            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                targetUsername = deviceAddress;
            }
        }
        tvChatName.setText(formatDisplayName(targetUsername));

        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (targetUsername != null && (targetUsername.equalsIgnoreCase(obj.optString("name")) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(obj.optString("id"))))) {
                    break;
                }
            }
        } catch (Exception ignored) {}

        boolean isCurrentlyConnected = isConnectionActive();
        if (isCurrentlyConnected) {
            isRemoteUserOnline = true;
            updateInputVisibility(true);
            stopAnimatingStatus();
            setStatusWithAnimation("В сети", R.color.prime_success);
            checkAndUpdateVersionInfoIsland();
        } else {
            isRemoteUserOnline = false;
            startAnimatingStatus("Определение подключения...", R.color.prime_accent);
            updateInputVisibility(false);
        }

        saveLastMessageToChatList(null, null, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");

        String currentUser = sharedPreferences.getString("current_user", "");
        String myDisplayName = sharedPreferences.getString("my_name", null);
        if (myDisplayName == null || myDisplayName.isEmpty()) {
            myDisplayName = sharedPreferences.getString("my_local_name", null);
        }
        if (myDisplayName == null || myDisplayName.isEmpty()) {
            myDisplayName = sharedPreferences.getString("current_user_name", null);
        }
        if (myDisplayName == null || myDisplayName.isEmpty()) {
            myDisplayName = sharedPreferences.getString(currentUser + "_name", currentUser);
        }
        localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";

        String contactAvatar = AvatarManager.getContactAvatarUriOrFile(this, targetUsername, targetUsername, deviceAddress);
        if (contactAvatar == null || contactAvatar.isEmpty()) {
            contactAvatar = avatarUri;
        }
        updateAvatarUi(contactAvatar, targetUsername);

        // Настройка списка
        chatAdapter = new ChatAdapter();
        chatAdapter.setLocalUsername(localUsername);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);

        try {
            initVideoNotePlaybackManagers();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize video note playback managers", t);
        }
        
        List<ChatMessage> initialHistory = ChatHistoryManager.loadMessages(this, targetUsername);
        if (!initialHistory.isEmpty()) {
            chatAdapter.setMessages(initialHistory);
            rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
        
        RecyclerView.ItemAnimator animator = rvMessages.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        if (animator != null) {
            animator.setAddDuration(150);
            animator.setRemoveDuration(150);
        }

        rvMessages.setItemViewCacheSize(6);
        ItemTouchHelper.SimpleCallback swipeToReplyCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private boolean hapticTriggered = false;

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (!isConnectionActive()) {
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

                if (!isConnectionActive()) {
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
                    bubble.animate()
                            .translationX(0f)
                            .setDuration(280)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start();
                }
            }
        };

        new ItemTouchHelper(swipeToReplyCallback).attachToRecyclerView(rvMessages);
        setupPullToOpenPersonInfo();

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (chatAdapter != null) {
                    chatAdapter.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE);
                }
            }

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
                            fabScrollToBottom.animate().cancel();
                            if (fabScrollToBottom.getVisibility() != View.VISIBLE || fabScrollToBottom.getAlpha() < 0.9f) {
                                fabScrollToBottom.setVisibility(View.VISIBLE);
                                fabScrollToBottom.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();
                            }
                        } else {
                            fabScrollToBottom.animate().cancel();
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

                                tvFloatingDate.animate().cancel();
                                if (tvFloatingDate.getVisibility() != View.VISIBLE) {
                                    tvFloatingDate.setVisibility(View.VISIBLE);
                                    tvFloatingDate.setAlpha(0f);
                                }
                                tvFloatingDate.bringToFront();
                                if (tvFloatingDate.getAlpha() < 1f) {
                                    tvFloatingDate.animate().alpha(1f).setDuration(150).start();
                                }
                                dateHideHandler.removeCallbacks(hideDateRunnable);
                                dateHideHandler.postDelayed(hideDateRunnable, 2000);
                            }
                        }
                    }
                }
            }
        });

        if (tvFloatingDate != null) {
            tvFloatingDate.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (rvMessages != null && chatAdapter != null) {
                    LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
                    if (lm != null) {
                        int firstPos = lm.findFirstVisibleItemPosition();
                        if (firstPos == RecyclerView.NO_POSITION) {
                            firstPos = 0;
                        }
                        int targetPos = chatAdapter.findDateSectionStartPosition(firstPos);
                        if (targetPos >= 0 && targetPos < chatAdapter.getItemCount()) {
                            lm.scrollToPositionWithOffset(targetPos, (int) (8 * getResources().getDisplayMetrics().density));

                            long ts = chatAdapter.getMessageTimestamp(targetPos);
                            if (ts > 0) {
                                tvFloatingDate.setText(ChatAdapter.getDateHeaderString(ts));
                                if (tvFloatingDate.getVisibility() != View.VISIBLE) {
                                    tvFloatingDate.setVisibility(View.VISIBLE);
                                    tvFloatingDate.setAlpha(0f);
                                }
                                tvFloatingDate.bringToFront();
                                tvFloatingDate.animate().alpha(1f).setDuration(150).start();
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
                if (message == null) return;
                showEditMessageDialog(message, position);
            }

            @Override
            public void onDeleteMessage(ChatMessage message, int position) {
                if (message == null) return;
                
                if (!isConnectionActive()) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Действие недоступно без подключения", null);
                    return;
                }
                
                sendPacket(TYPE_DELETE_MSG, message.getMessageId().getBytes(StandardCharsets.UTF_8));
                
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
                if (!isConnectionActive()) {
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Действие недоступно без подключения", null);
                    return;
                }
                applyAndSendReaction(message, reaction);
            }

            @Override
            public void onReplyMessage(ChatMessage message, int position) {
                if (!isConnectionActive()) {
                    return;
                }
                enterReplyMode(message);
            }

            @Override
            public void onReplyToSelectedText(ChatMessage message, String selectedText, int position) {
                if (!isConnectionActive()) {
                    return;
                }
                enterReplyModeForSelectedText(message, selectedText);
            }

            @Override
            public void onForwardMessage(ChatMessage message, int position) {
                showForwardDialog(message);
            }

            @Override
            public void onSelectMessage(ChatMessage message, int position) {
                enterMultiSelectionMode();
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                if (selectedCount == 0) {
                    exitMultiSelectionMode();
                } else {
                    if (!isMultiSelectionActive) {
                        enterMultiSelectionMode();
                    }
                    animateSelectionCounterUpdate(selectedCount);
                    checkAndUpdateMultiDeleteButton();
                }
            }

            @Override
            public void onExpandReactions(ChatMessage message, View anchorView, int position) {
                if (chatAdapter != null) {
                    chatAdapter.showExpandedReactionsPopup(anchorView, message, position);
                }
            }

            @Override
            public void onCancelSending(ChatMessage message, int position) {
                cancelCurrentFileSending();
            }

            @Override
            public void onRetryMessage(ChatMessage message, int position) {
                if (message == null || message.getMessageId() == null) return;
                message.setMessageStatus(MessageStatus.SENDING);
                chatAdapter.updateMessageStatusById(message.getMessageId(), MessageStatus.SENDING);
                ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, message.getMessageId(), MessageStatus.SENDING);
                flushPendingMessages();
                if (!isConnectionActive()) {
                    startPrimeConnection();
                }
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
                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, incomingMessage);
                        if (deviceAddress != null && !deviceAddress.isEmpty() && !deviceAddress.equalsIgnoreCase(targetUsername)) {
                            ChatHistoryManager.saveMessage(ChatPersonActivity.this, deviceAddress, incomingMessage);
                        }
                        saveLastMessageToChatList(parsed.realText, MessageStatus.READ, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                        typingResetHandler.removeCallbacks(resetTypingRunnable);
                        resetTypingRunnable.run();
                        break;
                    case MESSAGE_READ_PHOTO:
                        byte[] fullPayload = (byte[]) msg.obj;
                        if (fullPayload != null && fullPayload.length > 0) {
                            runOnIoThread(() -> {
                                try {
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
                                                        try (FileOutputStream fos = new FileOutputStream(mediaFile)) {
                                                            fos.write(itemBytes);
                                                            fos.flush();
                                                        }

                                                        recMediaItems.add(new ChatMessage.MediaItem(mediaFile.getAbsolutePath(), isVideo, durStr));
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Failed to parse multi-media item " + idx, e);
                                                }
                                            }

                                            if (!recMediaItems.isEmpty()) {
                                                ChatMessage multiMsg = new ChatMessage(captionText, photoTime, targetUsername, false, null, photoTs, null, photoMsgId);
                                                multiMsg.setMediaItems(recMediaItems);
                                                runOnUiThread(() -> {
                                                    if (isFinishing() || isDestroyed()) return;
                                                    addMessageToUI(multiMsg);
                                                    ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, multiMsg);
                                                    typingResetHandler.removeCallbacks(resetTypingRunnable);
                                                    resetTypingRunnable.run();
                                                });
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

                                    boolean isReceivedGif = photoBytes.length > 3 && photoBytes[0] == (byte) 'G' && photoBytes[1] == (byte) 'I' && photoBytes[2] == (byte) 'F';
                                    String ext = isReceivedGif ? ".gif" : ".jpg";

                                    String savedPhotoPath = null;
                                    if (photoBytes.length > 0) {
                                        File photoFile = ChatHistoryManager.saveBytesToAtomicFile(ChatPersonActivity.this, "rec_photo_" + (photoMsgId != null ? photoMsgId : System.currentTimeMillis()) + ext, photoBytes);
                                        if (photoFile != null) {
                                            savedPhotoPath = photoFile.getAbsolutePath();
                                        }
                                    }

                                    Bitmap photoBitmap = isReceivedGif ? null : decodeSampledBitmapFromByteArray(photoBytes, 512, 512);
                                    long photoTs = System.currentTimeMillis();
                                    String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                                    ChatMessage photoMsg = new ChatMessage(captionText, photoTime, targetUsername, false, photoBitmap, photoTs, savedPhotoPath, photoMsgId);
                                    photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);

                                    runOnUiThread(() -> {
                                        if (isFinishing() || isDestroyed()) return;
                                        addMessageToUI(photoMsg);
                                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, photoMsg);
                                        typingResetHandler.removeCallbacks(resetTypingRunnable);
                                        resetTypingRunnable.run();
                                    });
                                } catch (Throwable e) {
                                    Log.e(TAG, "Error processing received photo message", e);
                                }
                            });
                        }
                        break;
                    case MESSAGE_READ_FILE:
                        byte[] fileBuf = (byte[]) msg.obj;
                        if (fileBuf != null && fileBuf.length > 0) {
                            runOnIoThread(() -> {
                                try {
                                    ChatMessage fileMsg = parseFileMessageBytes(fileBuf, targetUsername);
                                    if (fileMsg != null) {
                                        runOnUiThread(() -> {
                                            if (isFinishing() || isDestroyed()) return;
                                            addMessageToUI(fileMsg);
                                            ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, fileMsg);
                                            typingResetHandler.removeCallbacks(resetTypingRunnable);
                                            resetTypingRunnable.run();
                                        });
                                    }
                                } catch (Throwable e) {
                                    Log.e(TAG, "Error processing received file message", e);
                                }
                            });
                        }
                        break;
                    case MESSAGE_SEND_PROGRESS:
                        int sendProg = msg.arg1;
                        String targetMsgId = (msg.obj instanceof String) ? (String) msg.obj : currentSendingMessageId;
                        if (chatAdapter != null && targetMsgId != null) {
                            chatAdapter.updateMessageSendingProgress(targetMsgId, sendProg);
                            showSendingProgressUi(sendProg);
                            if (sendProg >= 100) {
                                String msgIdFinished = targetMsgId;
                                chatAdapter.updateMessageStatusById(msgIdFinished, MessageStatus.SENT);
                                ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, msgIdFinished, MessageStatus.SENT);
                                if (Objects.equals(currentSendingMessageId, msgIdFinished)) {
                                    currentSendingMessageId = null;
                                }
                                if (layoutSendingProgress != null) {
                                    layoutSendingProgress.setVisibility(View.GONE);
                                }
                                saveActivityStateToChatList(targetUsername, "IDLE");
                                sendActivityState("IDLE");
                                ChatMessage sentMsg = chatAdapter.findMessageById(msgIdFinished);
                                String summaryStr = "Медиафайл";
                                if (sentMsg != null) {
                                    if (sentMsg.isVideoNote() || (sentMsg.getText() != null && sentMsg.getText().startsWith("VIDEO_NOTE:"))) {
                                        summaryStr = "📹 Видеосообщение";
                                    } else if (sentMsg.getMessageType() == ChatMessage.MessageType.IMAGE) summaryStr = "Фотография";
                                    else if (sentMsg.getMessageType() == ChatMessage.MessageType.VIDEO) summaryStr = "Видео: " + (sentMsg.getFileName() != null ? sentMsg.getFileName() : "");
                                    else if (sentMsg.getMessageType() == ChatMessage.MessageType.FILE) summaryStr = "Файл: " + (sentMsg.getFileName() != null ? sentMsg.getFileName() : "");
                                }
                                saveLastMessageToChatList(summaryStr, MessageStatus.SENT, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                            }
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
                        setOnlineStatusIndicator(true);
                        if (isRemoteUserOnline) {
                            setStatusWithAnimation("В сети", R.color.prime_success);
                            saveLastSeenTimestamp();
                        } else {
                            updateOfflineLastSeenStatus();
                        }
                        checkAndUpdateVersionInfoIsland();
                        if (!"IDLE".equals(myLocalActivityState)) {
                            sendActivityState(myLocalActivityState);
                        }
                        updateInputVisibility(true);
                        sendLocalAvatar();
                        flushPendingMessages();
                        sendReadReceiptsForUnreadMessages();
                        break;
                    case MESSAGE_TYPING:
                        String stateStr = (msg.obj instanceof String) ? (String) msg.obj : null;
                        if (!isConnectionActive()) {
                            stopAnimatingStatus();
                            updateOfflineLastSeenStatus();
                            setOnlineStatusIndicator(false);
                            break;
                        }
                        if (stateStr != null && stateStr.startsWith("STATE:")) {
                            String action = stateStr.substring(6);
                            currentActivityState = action;

                            if ("SELECTING_MEDIA".equalsIgnoreCase(action) || "SELECTING_FILE".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Выбирает файл или медиа", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, action);
                            } else if ("SENDING_PHOTO".equalsIgnoreCase(action) || "SENDING_VIDEO".equalsIgnoreCase(action) || "SENDING_MEDIA".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Отправка медиа", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 10000);
                                saveActivityStateToChatList(targetUsername, "SENDING_MEDIA");
                            } else if ("SENDING_FILE".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Отправка файла", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 10000);
                                saveActivityStateToChatList(targetUsername, "SENDING_FILE");
                            } else if ("RECORDING_VIDEO_NOTE".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Записывает видеокружок...", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 10000);
                                saveActivityStateToChatList(targetUsername, "RECORDING_VIDEO_NOTE");
                            } else if ("SENDING_VIDEO_NOTE".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Отправка видеокружка...", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 10000);
                                saveActivityStateToChatList(targetUsername, "SENDING_VIDEO_NOTE");
                            } else if ("RECORDING_VOICE".equalsIgnoreCase(action) || "RECORDING_AUDIO".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Записывает голосовое...", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 10000);
                                saveActivityStateToChatList(targetUsername, "RECORDING_VOICE");
                            } else if ("VIEWING_PHOTO".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит фото", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, action);
                            } else if ("VIEWING_VIDEO".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит видео", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, action);
                            } else if ("VIEWING_FILE".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Смотрит файл", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, action);
                            } else if ("READING_MODE".equalsIgnoreCase(action)) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("В режиме чтения", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, action);
                            } else if ("LISTENING_MUSIC".equalsIgnoreCase(action) || "LISTENING_AUDIO".equalsIgnoreCase(action) || action.toUpperCase(java.util.Locale.US).startsWith("LISTENING_TRACK")) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("Слушает аудио...", R.color.prime_brand);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 6000);
                                saveActivityStateToChatList(targetUsername, "LISTENING_AUDIO");
                            } else if ("IDLE".equalsIgnoreCase(action)) {
                                currentActivityState = "IDLE";
                                stopAnimatingStatus();
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                saveActivityStateToChatList(targetUsername, "IDLE");
                                resetTypingRunnable.run();
                            } else if ("TYPING".equalsIgnoreCase(action)) {
                                startAnimatingStatus("Печатает", R.color.prime_success);
                                typingResetHandler.removeCallbacks(resetTypingRunnable);
                                typingResetHandler.postDelayed(resetTypingRunnable, 4500);
                                saveActivityStateToChatList(targetUsername, "TYPING");
                            }
                        } else {
                            startAnimatingStatus("Печатает", R.color.prime_success);
                            typingResetHandler.removeCallbacks(resetTypingRunnable);
                            typingResetHandler.postDelayed(resetTypingRunnable, 4500);
                            saveActivityStateToChatList(targetUsername, "TYPING");
                        }
                        break;
                    case MESSAGE_EDIT_RECEIVED:
                        String editData = (String) msg.obj;
                        if (editData != null) {
                            int editSep = editData.indexOf(":::");
                            if (editSep != -1) {
                                try {
                                    String editMsgId = editData.substring(0, editSep).trim();
                                    String updatedText = editData.substring(editSep + 3);

                                    if (chatAdapter != null) {
                                        chatAdapter.updateMessageTextById(editMsgId, updatedText);
                                        ChatMessage newLast = chatAdapter.getLastMessage();
                                        if (newLast != null) {
                                            saveLastMessageToChatList(newLast.getText());
                                        }
                                    }

                                    ChatHistoryManager.updateMessageText(ChatPersonActivity.this, targetUsername, editMsgId, updatedText);
                                    ChatListNotifier.INSTANCE.notifyChanged();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to parse edit message", e);
                                }
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
                            BluetoothConnectionManager.getInstance().disconnect(resolveTargetAddress());
                        } catch (Exception ignored) {}

                        BluetoothSocketHolder.removeConnection(deviceAddress, targetUsername);
                        if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
                            PrimeBluetoothService.stopService(ChatPersonActivity.this);
                        }
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, deletedByName, deviceAddress);
                        ChatHistoryManager.deleteHistoryCompletely(ChatPersonActivity.this, targetUsername, deviceAddress);
                        deleteChatFromChatListEx(deletedByName);
                        deleteChatFromChatListEx(targetUsername);

                        if (deviceAddress != null && !deviceAddress.isEmpty()) {
                            BluetoothConnectionManager.getInstance().blockMacAddress(deviceAddress, 0, "Chat deleted");
                            BluetoothConnectionManager.getInstance().disconnect(deviceAddress);
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
                        saveLastMessageToChatList(null, MessageStatus.READ, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
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
                                setStatusWithAnimation("В сети", R.color.prime_success);
                                checkAndUpdateVersionInfoIsland();
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

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                if (!isConnectionActive()) {
                    Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                    return;
                }
                String text = etMessage.getText().toString().trim();

                if (currentPendingAttachment != null && currentPendingAttachment.items != null && !currentPendingAttachment.items.isEmpty()) {
                    List<PendingAttachmentItem> sendItems = new ArrayList<>(currentPendingAttachment.items);
                    clearPendingAttachment();
                    etMessage.setText("");

                    boolean allMedia = true;
                    for (PendingAttachmentItem item : sendItems) {
                        if (item.messageType != ChatMessage.MessageType.IMAGE && item.messageType != ChatMessage.MessageType.VIDEO) {
                            allMedia = false;
                            break;
                        }
                    }

                    if (sendItems.size() > 1 && allMedia) {
                        sendMultiMedia(sendItems, text);
                    } else {
                        for (PendingAttachmentItem item : sendItems) {
                            if (item.messageType == ChatMessage.MessageType.IMAGE) {
                                sendPhoto(item.uri != null ? item.uri : Uri.parse(item.path), text);
                            } else {
                                if (item.size > 2 * 1024 * 1024 * 1024L) {
                                    PrimeNotification.INSTANCE.show(this, "Превышен лимит размера файла (до 2 ГБ)", null);
                                    continue;
                                }
                                sendVideoOrFile(item, text);
                            }
                        }
                    }
                    closeAttachmentPanel();
                    updateActionButtonsVisibility(true);
                    return;
                }

                if (isEditMode && editingMessageId != null) {
                    if (text.isEmpty()) {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Текст не может быть пустым", null);
                        return;
                    }

                    String payloadStr = editingMessageId + ":::" + text;
                    byte[] payloadBytes = payloadStr.getBytes(StandardCharsets.UTF_8);
                    if (isConnectionActive()) {
                        sendPacket(TYPE_EDIT, payloadBytes);
                    } else {
                        pendingMessageQueue.add(new PendingMessage(TYPE_EDIT, payloadBytes));
                    }

                    chatAdapter.updateMessageTextById(editingMessageId, text);
                    ChatHistoryManager.updateMessageText(ChatPersonActivity.this, targetUsername, editingMessageId, text);

                    ChatMessage newLast = chatAdapter.getLastMessage();
                    if (newLast != null && Objects.equals(editingMessageId, newLast.getMessageId())) {
                        saveLastMessageToChatList(text);
                    }

                    etMessage.setText("");
                    exitEditMode();
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Сообщение изменено", null);
                } else {
                    if (!text.isEmpty()) {
                        sendText(text);
                        etMessage.setText("");
                    }
                }
                updateActionButtonsVisibility(true);
            });
        }

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateActionButtonsVisibility(true);
                senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
                boolean hasText = s != null && !s.toString().trim().isEmpty();
                if (!hasText) {
                    if ("TYPING".equalsIgnoreCase(myLocalActivityState)) {
                        sendActivityState("IDLE");
                    }
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastTypingSentTime > 1500 && isConnectionActive()) {
                        lastTypingSentTime = now;
                        sendActivityState("TYPING");
                    }
                    senderTypingHandler.postDelayed(stopSenderTypingRunnable, 2500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Настройка Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается (Эмулятор)", Toast.LENGTH_SHORT).show();
            setupMockMode();
            return;
        }

        boolean useExistingSocket = intent.getBooleanExtra("EXTRA_USE_EXISTING_SOCKET", false);

        if (isConnectionActive()) {
            BluetoothSocketHolder.setActiveDeviceAddress(deviceAddress);
            BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

            setOnlineStatusIndicator(true);
            updateInputVisibility(true);
            sendLocalAvatar();
            flushPendingMessages();
        } else {
            updateInputVisibility(false);
            checkPermissionsAndStartRole(false);
        }

        applyChatUiCornerRadius();
        saveLastMessageToChatList(null);
        } catch (Throwable e) {
            Log.e("LockScreenLaunch", "Crash in onCreate of ChatPersonActivity", e);
            finish();
        }
    }

    private void applyChatUiCornerRadius() {
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        int topBarRadiusDp = sp.getInt("settings_top_bar_radius_dp", 24);
        int bottomBarRadiusDp = sp.getInt("settings_bottom_bar_radius_dp", 24);

        float density = getResources().getDisplayMetrics().density;
        final float topRadiusPx = topBarRadiusDp * density;
        final float bottomRadiusPx = bottomBarRadiusDp * density;

        eightbitlab.com.blurview.BlurView layoutHeader = findViewById(R.id.layoutHeader);
        if (layoutHeader != null) {
            layoutHeader.setClipToOutline(true);
            layoutHeader.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), topRadiusPx);
                }
            });
            layoutHeader.invalidateOutline();
            updateViewBackgroundCorners(layoutHeader, topRadiusPx);
        }

        eightbitlab.com.blurview.BlurView[] blurViews = new eightbitlab.com.blurview.BlurView[] {
            findViewById(R.id.layoutInput),
            findViewById(R.id.layoutActionIsland)
        };

        for (eightbitlab.com.blurview.BlurView bv : blurViews) {
            if (bv != null) {
                bv.setClipToOutline(true);
                bv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), bottomRadiusPx);
                    }
                });
                bv.invalidateOutline();
                updateViewBackgroundCorners(bv, bottomRadiusPx);
            }
        }
    }

    private void updateViewBackgroundCorners(View view, float radiusPx) {
        if (view == null) return;
        android.graphics.drawable.Drawable bg = view.getBackground();
        if (bg == null) return;
        if (bg instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable) bg).setCornerRadius(radiusPx);
        } else if (bg instanceof android.graphics.drawable.RippleDrawable) {
            android.graphics.drawable.RippleDrawable ripple = (android.graphics.drawable.RippleDrawable) bg;
            for (int i = 0; i < ripple.getNumberOfLayers(); i++) {
                android.graphics.drawable.Drawable layer = ripple.getDrawable(i);
                if (layer instanceof android.graphics.drawable.GradientDrawable) {
                    ((android.graphics.drawable.GradientDrawable) layer).setCornerRadius(radiusPx);
                }
            }
        }
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                sendPresenceUpdate(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                sendPresenceUpdate(true);
            } else if ("com.messenger.prime.DISCONNECT_REQUESTED".equals(action)) {
                isManuallyDisconnected = true;
                disconnectCurrentChat();
            } else if ("com.messenger.prime.CHAT_DELETED".equals(action)) {
                isChatDeleted = true;
                finish();
            } else if ("com.messenger.prime.AVATAR_CHANGED".equals(action) || "com.messenger.prime.NAME_CHANGED".equals(action) || "com.messenger.prime.ACCENT_CHANGED".equals(action) || "com.messenger.prime.THEME_CHANGED".equals(action) || "com.messenger.prime.BUBBLE_RADIUS_CHANGED".equals(action) || "com.messenger.prime.AVATAR_RADIUS_CHANGED".equals(action) || "com.messenger.prime.WALLPAPER_CHANGED".equals(action) || "com.messenger.prime.CHAT_UI_RADIUS_CHANGED".equals(action) || "com.messenger.prime.NAV_STYLE_CHANGED".equals(action)) {
                applyChatUiCornerRadius();
                reloadLocalProfileFromSettings();
                String updatedName = AvatarManager.getContactDisplayName(ChatPersonActivity.this, targetUsername, deviceAddress, targetUsername);
                if (!updatedName.isEmpty()) {
                    targetUsername = updatedName;
                    if (tvChatName != null) tvChatName.setText(formatDisplayName(targetUsername));
                }
                String contactAvatar = AvatarManager.getContactAvatarUriOrFile(ChatPersonActivity.this, targetUsername, targetUsername, deviceAddress);
                if (contactAvatar == null) contactAvatar = remoteAvatarUri;
                updateAvatarUi(contactAvatar, targetUsername);
                MaterialShapesWallpaperView wallpaperView = findViewById(R.id.vWallpaperBackground);
                if (wallpaperView != null) {
                    wallpaperView.setContactKey(targetUsername);
                    wallpaperView.updateThemeColors();
                }
                applyAccentToChatUi();
                if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action) || BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice dev = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    @SuppressWarnings("deprecation")
                    BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    dev = d;
                }
                if (dev != null && !isConnectionActive()) {
                    String devAddr = dev.getAddress();
                    String devName = dev.getName();
                    if (isValidMacAddress(devAddr) && devName != null && (devName.equalsIgnoreCase(targetUsername) || devName.toLowerCase(Locale.ROOT).contains(targetUsername.toLowerCase(Locale.ROOT)) || devName.contains("Prime"))) {
                        Log.d(TAG, "Discovery found target device: " + devName + " [" + devAddr + "], initiating auto-connection...");
                        getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                                .edit()
                                .putString(targetUsername + "_mac", devAddr)
                                .putString(devAddr + "_name", targetUsername)
                                .apply();
                        ChatPersonActivity.this.deviceAddress = devAddr;
                        startPrimeConnection(true);
                    }
                }
            }
        }
    };

    private void sendPresenceUpdate(boolean isOnline) {
        if (isConnectionActive()) {
            long now = System.currentTimeMillis();
            String payloadStr = isOnline ? "ONLINE" : ("OFFLINE:" + now);
            sendPacket(TYPE_PRESENCE, payloadStr.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void reloadMessagesFromHistory() {
        String keyToLoad = ChatHistoryManager.getStorageKey(this, (targetUsername != null && !targetUsername.isEmpty()) ? targetUsername : deviceAddress);
        List<ChatMessage> updatedHistory = ChatHistoryManager.loadMessages(this, keyToLoad);
        if (chatAdapter != null && updatedHistory != null) {
            int oldSize = chatAdapter.getItemCount();
            android.os.Parcelable scrollState = (rvMessages != null && rvMessages.getLayoutManager() != null)
                    ? rvMessages.getLayoutManager().onSaveInstanceState()
                    : null;

            chatAdapter.setMessages(updatedHistory);

            if (rvMessages != null && !updatedHistory.isEmpty()) {
                if (oldSize == 0) {
                    rvMessages.post(() -> {
                        if (chatAdapter != null && chatAdapter.getItemCount() > 0 && rvMessages != null) {
                            rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                        }
                    });
                } else if (scrollState != null && rvMessages.getLayoutManager() != null) {
                    rvMessages.getLayoutManager().onRestoreInstanceState(scrollState);
                }
            }
            sendReadReceiptsForUnreadMessages();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            super.onNewIntent(intent);
            setIntent(intent);
            if (intent != null) {
                String newTarget = intent.getStringExtra("EXTRA_CHAT_NAME");
                String newAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");

                if (newTarget != null && !newTarget.isEmpty()) {
                    this.targetUsername = newTarget;
                }
                if (newAddress != null && !newAddress.isEmpty()) {
                    this.deviceAddress = newAddress;
                }

                activeChatPersonAddress = this.deviceAddress;
                activeChatPersonName = this.targetUsername;

                BluetoothSocketHolder.setActiveDeviceAddress(this.deviceAddress);
                BluetoothSocketHolder.setActiveTargetUsername(this.targetUsername);

                SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                String resolvedName = (this.deviceAddress != null && !this.deviceAddress.isEmpty()) ?
                    sharedPrefs.getString("contact_name_" + this.deviceAddress, this.targetUsername) : this.targetUsername;
                if (resolvedName != null && !resolvedName.isEmpty()) {
                    this.targetUsername = resolvedName;
                }

                if (tvChatName != null) {
                    tvChatName.setText(formatDisplayName(this.targetUsername));
                }

                String contactAvatar = AvatarManager.getContactAvatarUriOrFile(this, this.targetUsername, this.targetUsername, this.deviceAddress);
                if (contactAvatar == null) contactAvatar = remoteAvatarUri;
                updateAvatarUi(contactAvatar, this.targetUsername);

                reloadMessagesFromHistory();
                sendReadReceiptsForUnreadMessages();

                SharedPreferences spMediaRecord = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                isVideoNoteMode = spMediaRecord.getBoolean("chat_media_record_mode_is_video", false);
                if (btnVoiceVideoNote != null) {
                    btnVoiceVideoNote.setImageResource(isVideoNoteMode ? R.drawable.ic_camera : R.drawable.ic_mic);
                }
                updateActionButtonsVisibility(false);

                if (isConnectionActive()) {
                    isRemoteUserOnline = true;
                    stopAnimatingStatus();
                    setStatusWithAnimation("В сети", R.color.prime_success);
                    updateInputVisibility(true);
                } else {
                    updateInputVisibility(false);
                    updateOfflineLastSeenStatus();
                }

                try {
                    String json = ChatHistoryManager.getPersistedChatsJson(this);
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String name = obj.optString("name", "");
                        String id = obj.optString("id", "");
                        if ((targetUsername != null && targetUsername.equalsIgnoreCase(name)) ||
                            (deviceAddress != null && deviceAddress.equalsIgnoreCase(id))) {
                            obj.put("unreadCount", 0);
                        }
                    }
                    sharedPrefs.edit().putString(ChatHistoryManager.getPersistedChatsKey(this), array.toString()).apply();
                    ChatListNotifier.INSTANCE.notifyChanged();
                } catch (Exception ignored) {}
            }
        } catch (Throwable e) {
            Log.e("LockScreenLaunch", "Crash in onNewIntent of ChatPersonActivity", e);
        }
    }

    private void applyAccentToChatUi() {
        int accentColor = ColorAccentManager.getCurrentAccentColor(this);
        ColorStateList colorStateList = ColorStateList.valueOf(accentColor);

        if (btnSend != null) btnSend.setImageTintList(colorStateList);
        if (btnVoiceVideoNote != null) btnVoiceVideoNote.setImageTintList(colorStateList);
        if (btnAttach != null) btnAttach.setImageTintList(colorStateList);
        if (tvFloatingDate != null) tvFloatingDate.setTextColor(accentColor);

        ProgressBar pbIsland = findViewById(R.id.pbVideoIslandProgress);
        if (pbIsland != null) pbIsland.setProgressTintList(colorStateList);

        TextView tvIslandSpeed = findViewById(R.id.btnVideoIslandSpeed);
        if (tvIslandSpeed != null) tvIslandSpeed.setTextColor(accentColor);

        ImageButton btnIslandSend = findViewById(R.id.btnVideoRecordSend);
        if (btnIslandSend != null) btnIslandSend.setImageTintList(colorStateList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAccentToChatUi();
        applyChatUiCornerRadius();
        PrimeMusicManager.INSTANCE.addListener(this);
        updateMusicIslandUI(PrimeMusicManager.INSTANCE.getCurrentTrack());
        isActivityForeground = true;
        activeChatPersonAddress = deviceAddress;
        activeChatPersonName = targetUsername;
        PrimeApplication.currentActiveChatAddress = deviceAddress;
        PrimeApplication.currentActiveChatName = targetUsername;
        boolean wasSubActivity = isOpeningSubActivity;
        isOpeningSubActivity = false;

        if (companionPacketListener == null) {
            companionPacketListener = (type, payload) -> {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ensureOnlineStateAndInputVisible();
                        processReceivedPacket(type, payload);
                    }
                });
                return kotlin.Unit.INSTANCE;
            };
        }
        PrimeCompanionServer.addPacketListener(companionPacketListener);

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction("com.messenger.prime.DISCONNECT_REQUESTED");
            filter.addAction("com.messenger.prime.CHAT_DELETED");
            filter.addAction("com.messenger.prime.AVATAR_CHANGED");
            filter.addAction("com.messenger.prime.NAME_CHANGED");
            filter.addAction("com.messenger.prime.ACCENT_CHANGED");
            filter.addAction("com.messenger.prime.THEME_CHANGED");
            filter.addAction("com.messenger.prime.BUBBLE_RADIUS_CHANGED");
            filter.addAction("com.messenger.prime.AVATAR_RADIUS_CHANGED");
            filter.addAction("com.messenger.prime.WALLPAPER_CHANGED");
            ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception ignored) {}

        if (wasSubActivity) {
            sendActivityState("IDLE");
        } else {
            sendPresenceUpdate(true);
        }

        BluetoothConnectionManager.getInstance().setCallback(this);

        connectionCheckHandler.removeCallbacks(connectionCheckRunnable);
        connectionCheckHandler.post(connectionCheckRunnable);

        if (isConnectionActive()) {
            updateInputVisibility(true);
            sendLocalAvatar(false);
        } else {
            updateInputVisibility(false);
            if (!isManuallyDisconnected) {
                autoRetryHandler.removeCallbacks(autoRetryRunnable);
                startPrimeConnection(true);
            }
        }

        reloadLocalProfileFromSettings();

        MaterialShapesWallpaperView vWallpaper = findViewById(R.id.vWallpaperBackground);
        if (vWallpaper != null) {
            vWallpaper.updateThemeColors();
            if (!wasSubActivity) {
                vWallpaper.startEntranceAnimation();
            }
        }

        if (isConnectionActive()) {
            sendLocalAvatar();
        }

        if (isConnectionActive()) {
            saveLastSeenTimestamp();
            updateInputVisibility(true);
        } else {
            updateOfflineLastSeenStatus();
            updateInputVisibility(false);
        }

        reloadMessagesFromHistory();
        ChatHistoryManager.clearUnreadCountInPersistedChats(this, targetUsername);
        if (deviceAddress != null) {
            ChatHistoryManager.clearUnreadCountInPersistedChats(this, deviceAddress);
        }
        PrimeNotification.cancelNotificationForUser(this, targetUsername, deviceAddress);
        PrimeNotification.INSTANCE.cancelSendingProgressNotification(this);
        setOnlineStatusIndicator(isConnectionActive());
        ChatListNotifier.INSTANCE.notifyChanged();
    }

    private void reloadLocalProfileFromSettings() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPreferences.getString("current_user", "");
            String myDisplayName = sharedPreferences.getString("my_local_name", null);
            if (myDisplayName == null || myDisplayName.isEmpty()) {
                myDisplayName = sharedPreferences.getString("current_user_name", null);
            }
            if (myDisplayName == null || myDisplayName.isEmpty()) {
                myDisplayName = sharedPreferences.getString(currentUser + "_name", currentUser);
            }
            localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";
            if (chatAdapter != null) {
                chatAdapter.setLocalUsername(localUsername);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reload local profile from settings", e);
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onPause() {
        super.onPause();
        if (videoMessageRecorder != null) {
            videoMessageRecorder.cancelRecording();
        }
        isActivityForeground = false;
        activeChatPersonAddress = null;
        activeChatPersonName = null;
        if (Objects.equals(PrimeApplication.currentActiveChatAddress, deviceAddress) ||
            Objects.equals(PrimeApplication.currentActiveChatName, targetUsername)) {
            PrimeApplication.currentActiveChatAddress = null;
            PrimeApplication.currentActiveChatName = null;
        }
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {}

        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
        connectionCheckHandler.removeCallbacks(connectionCheckRunnable);
        if ("TYPING".equalsIgnoreCase(myLocalActivityState)) {
            sendActivityState("IDLE");
        }

        if (!isOpeningSubActivity) {
            sendPresenceUpdate(false);
        }

        if (!isChatDeleted) {
            saveLastMessageToChatList(null);
        }
        BluetoothConnectionManager.getInstance().removeCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BluetoothConnectionManager.getInstance().removeCallback(this);
        lastSeenHandler.removeCallbacks(lastSeenRunnable);
    }

    private final Handler lastSeenHandler = new Handler(Looper.getMainLooper());
    private final Runnable lastSeenRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isConnectionActive()) {
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

        if (isConnectionActive()) {
            String payloadStr = msgId + ":::" + (reaction != null ? reaction : "REMOVE") + ":::" + localUsername;
            sendPacket(TYPE_REACTION, payloadStr.getBytes(StandardCharsets.UTF_8));
        }

        ChatHistoryManager.saveMessage(this, targetUsername, message);
    }

    private void updateConnectionStateInAdapter() {
        if (chatAdapter != null) {
            boolean active = isConnectionActive();
            chatAdapter.setConnectionActive(active);
        }
    }

    private void disconnectCurrentChat() {
        isManuallyDisconnected = true;
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);

        try {
            sendPacket(TYPE_DISCONNECT, "DISCONNECT".getBytes(StandardCharsets.UTF_8));
            Thread.sleep(80);
        } catch (Exception ignored) {}

        BluetoothConnectionManager.getInstance().disconnect(resolveTargetAddress());

        try {
            PrimeBluetoothService.stopService(this);
        } catch (Exception ignored) {}

        hideSoftKeyboard();
        closeAttachmentPanel();
        clearPendingAttachment();

        saveLastMessageToChatList(null, MessageStatus.NONE, false, "OFFLINE");
        updateInputVisibility(false);
        stopAnimatingStatus();
        updateOfflineLastSeenStatus();
        setOnlineStatusIndicator(false);
        updateConnectionStateInAdapter();

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
        sendActivityState("TYPING");

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
            layoutEditBar.animate().cancel();
            layoutEditBar.animate().setListener(null);
            layoutEditBar.setVisibility(View.VISIBLE);
            layoutEditBar.setAlpha(1f);
            layoutEditBar.setTranslationY(0f);
            TextView tvEditBarTitle = findViewById(R.id.tvEditBarTitle);
            if (tvEditBarTitle != null) {
                String originalText = message.getText();
                tvEditBarTitle.setText(originalText != null && !originalText.isEmpty() ? originalText : "Редактирование сообщения");
            }
        }
        updateActionIslandVisibility();
    }

    private void exitEditMode() {
        isEditMode = false;
        editingMessageId = null;
        sendActivityState("IDLE");

        if (layoutEditBar != null) {
            layoutEditBar.animate().cancel();
            layoutEditBar.animate().setListener(null);
            layoutEditBar.setVisibility(View.GONE);
            layoutEditBar.setAlpha(1f);
            layoutEditBar.setTranslationY(0f);
        }

        if (btnCancelEdit != null) btnCancelEdit.setVisibility(View.GONE);
        if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);

        if (etMessage != null) {
            etMessage.setText("");
        }
        updateActionIslandVisibility();
    }

    private void enterReplyMode(ChatMessage message) {
        if (message == null) return;
        replyingToMessage = message;
        replyingToText = null;

        if (isEditMode) {
            exitEditMode();
        }

        if (layoutReplyBar != null) {
            layoutReplyBar.animate().cancel();
            layoutReplyBar.animate().setListener(null);
            layoutReplyBar.setVisibility(View.VISIBLE);
            layoutReplyBar.setAlpha(1f);
            layoutReplyBar.setTranslationY(0f);
        }

        if (tvReplyBarTitle != null) {
            String sender = message.getSenderLogin();
            tvReplyBarTitle.setText("Ответ: " + (sender != null && !sender.isEmpty() ? sender : "Пользователю"));
        }

        if (tvReplyBarText != null) {
            String text = message.getText();
            if (message.isVideoNote() || (text != null && (text.startsWith("VIDEO_NOTE:") || text.startsWith("vn_")))) {
                text = "📹 Видеосообщение";
            }
            tvReplyBarText.setText(text != null && !text.isEmpty() ? text : "Сообщение");
        }

        if (etMessage != null) {
            etMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        }
        updateActionIslandVisibility();
    }

    private void enterReplyModeForSelectedText(ChatMessage message, String selectedText) {
        if (message == null || selectedText == null || selectedText.trim().isEmpty()) return;
        replyingToMessage = message;
        replyingToText = selectedText.trim();

        if (isEditMode) {
            exitEditMode();
        }

        if (layoutReplyBar != null) {
            layoutReplyBar.animate().cancel();
            layoutReplyBar.animate().setListener(null);
            layoutReplyBar.setVisibility(View.VISIBLE);
            layoutReplyBar.setAlpha(1f);
            layoutReplyBar.setTranslationY(0f);
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
        updateActionIslandVisibility();
    }

    private void cancelReplyMode() {
        replyingToMessage = null;
        replyingToText = null;
        if (layoutReplyBar != null) {
            layoutReplyBar.animate().cancel();
            layoutReplyBar.animate().setListener(null);
            layoutReplyBar.setVisibility(View.GONE);
            layoutReplyBar.setAlpha(1f);
            layoutReplyBar.setTranslationY(0f);
        }
        updateActionIslandVisibility();
    }

    private void checkAndUpdateMultiDeleteButton() {
        if (chatAdapter == null || btnMultiDelete == null) return;
        List<ChatMessage> selected = chatAdapter.getSelectedMessages();

        boolean hasIncoming = false;
        for (ChatMessage m : selected) {
            if (m != null && !m.isOutgoing()) {
                hasIncoming = true;
                break;
            }
        }

        btnMultiDelete.setVisibility(hasIncoming ? View.GONE : View.VISIBLE);
    }

    private void animateInputVisibility(boolean show) {
        if (layoutInput == null) return;
        layoutInput.animate().cancel();
        if (show) {
            layoutInput.setVisibility(View.VISIBLE);
            layoutInput.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            hideSoftKeyboard();
            layoutInput.animate()
                    .translationY(layoutInput.getHeight() * 1.5f)
                    .alpha(0f)
                    .setDuration(200)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> layoutInput.setVisibility(View.GONE))
                    .start();
        }
    }

    private void enterMultiSelectionMode() {
        if (isMultiSelectionActive) return;
        isMultiSelectionActive = true;
        animateHeaderFlip(true);
        checkAndUpdateMultiDeleteButton();
        animateInputVisibility(false);
    }

    private void exitMultiSelectionMode() {
        if (!isMultiSelectionActive) return;
        isMultiSelectionActive = false;
        if (chatAdapter != null) {
            chatAdapter.clearSelection();
        }
        animateHeaderFlip(false);
        animateInputVisibility(true);
    }

    private void animateHeaderFlip(boolean enteringSelection) {
        View normalView = findViewById(R.id.layoutHeaderNormal);
        View selectionView = findViewById(R.id.layoutHeaderSelection);
        if (normalView == null || selectionView == null) return;

        View hideView = enteringSelection ? normalView : selectionView;
        View showView = enteringSelection ? selectionView : normalView;

        hideView.animate().cancel();
        showView.animate().cancel();

        float distance = 10000 * getResources().getDisplayMetrics().density;
        hideView.setCameraDistance(distance);
        showView.setCameraDistance(distance);

        float hideTargetRotationX = enteringSelection ? -90f : 90f;
        float showStartRotationX = enteringSelection ? 90f : -90f;

        hideView.animate()
                .rotationX(hideTargetRotationX)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    hideView.setVisibility(View.GONE);
                    hideView.setRotationX(0f);

                    showView.setVisibility(View.VISIBLE);
                    showView.setRotationX(showStartRotationX);
                    showView.animate()
                            .rotationX(0f)
                            .setDuration(180)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void animateSelectionCounterUpdate(int newCount) {
        TextView tvCounter = findViewById(R.id.tvSelectionCounter);
        if (tvCounter == null) return;

        String text = String.valueOf(newCount);
        if (text.equals(tvCounter.getText().toString())) return;

        tvCounter.animate().cancel();
        tvCounter.animate()
                .translationY(24f)
                .alpha(0f)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    tvCounter.setText(text);
                    tvCounter.setTranslationY(-24f);
                    tvCounter.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(140)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void performMultiCopy() {
        if (chatAdapter == null) return;
        List<ChatMessage> selected = chatAdapter.getSelectedMessages();
        if (selected.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : selected) {
            String text = m.getText();
            if (text == null || text.isEmpty()) {
                text = m.getImagePath() != null ? m.getImagePath() : "";
            }
            if (!text.isEmpty()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(text);
            }
        }

        if (sb.length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("selected_messages", sb.toString());
                clipboard.setPrimaryClip(clip);
                PrimeNotification.INSTANCE.show(this, "Скопировано сообщений: " + selected.size(), null);
            }
        }
        exitMultiSelectionMode();
    }

    private void performMultiForward() {
        if (chatAdapter == null) return;
        List<ChatMessage> selected = chatAdapter.getSelectedMessages();
        if (selected.isEmpty()) return;

        showForwardDialogForMultiple(selected);
    }

    private void showForwardDialogForMultiple(List<ChatMessage> messagesToForward) {
        if (messagesToForward == null || messagesToForward.isEmpty()) return;

        String jsonChats = ChatHistoryManager.getPersistedChatsJson(this);
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

        PrimeBlurDialog.showList(
                this,
                "Переслать сообщения (" + messagesToForward.size() + ")",
                chatArray,
                null,
                (index, selectedTarget) -> {
                    for (ChatMessage m : messagesToForward) {
                        forwardMessageToTarget(m, selectedTarget);
                    }
                    PrimeNotification.INSTANCE.show(this, "Сообщения пересланы", null);
                    exitMultiSelectionMode();
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private void performMultiDelete() {
        if (chatAdapter == null) return;
        List<ChatMessage> selected = chatAdapter.getSelectedMessages();
        if (selected.isEmpty()) return;

        int count = selected.size();
        PrimeBlurDialog.show(
                this,
                "Удалить выбранные сообщения?",
                "Вы уверены, что хотите удалить " + count + " " + (count == 1 ? "сообщение" : "сообщений") + "?",
                "Удалить",
                "Отмена",
                null,
                true,
                null,
                () -> {
                    List<String> idsToDelete = new ArrayList<>();
                    for (ChatMessage m : selected) {
                        if (m != null && m.getMessageId() != null) {
                            String msgId = m.getMessageId();
                            idsToDelete.add(msgId);
                            sendPacket(TYPE_DELETE_MSG, msgId.getBytes(StandardCharsets.UTF_8));
                            ChatHistoryManager.deleteSingleMessage(ChatPersonActivity.this, targetUsername, msgId);

                            Intent deleteMsgIntent = new Intent("com.messenger.prime.MSG_DELETED").setPackage(getPackageName());
                            deleteMsgIntent.putExtra("messageId", msgId);
                            sendBroadcast(deleteMsgIntent);
                        }
                    }

                    chatAdapter.deleteMessagesByIds(rvMessages, idsToDelete, () -> {
                        ChatMessage newLast = chatAdapter.getLastMessage();
                        saveLastMessageToChatList(newLast != null ? (newLast.getText() != null ? newLast.getText() : "") : "");
                        exitMultiSelectionMode();
                    });
                    return kotlin.Unit.INSTANCE;
                },
                null
        );
    }

    private void showForwardDialog(ChatMessage messageToForward) {
        if (messageToForward == null) return;

        SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
        String jsonChats = ChatHistoryManager.getPersistedChatsJson(this);
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

        PrimeBlurDialog.showList(
                this,
                "Переслать сообщение",
                chatArray,
                null,
                (index, selectedTarget) -> {
                    forwardMessageToTarget(messageToForward, selectedTarget);
                    return kotlin.Unit.INSTANCE;
                }
        );
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

            if (isConnectionActive()) {
                String packetContent = fwdMsg.getMessageId() + ":::" + fwdMsg.getText();
                packetContent += ":::REPLY:::" + fwdMsg.getReplyToMessageId() + ":::" + fwdMsg.getReplyToSender() + ":::" + fwdMsg.getReplyToText();
                sendPacket(TYPE_TEXT, packetContent.getBytes(StandardCharsets.UTF_8));
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

    private void jumpToTargetMessage(String messageId, String quotedText) {
        if (messageId == null || chatAdapter == null || rvMessages == null) return;
        int pos = chatAdapter.findPositionByMessageId(messageId);
        if (pos != -1) {
            LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
            if (lm != null) {
                lm.scrollToPositionWithOffset(pos, (int) (80 * getResources().getDisplayMetrics().density));
                rvMessages.postDelayed(() -> {
                    chatAdapter.highlightMessageAtPosition(pos);
                    chatAdapter.triggerHighlightAnimation(rvMessages, messageId, quotedText);
                }, 150);
            }
        } else {
            PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Исходное сообщение не найдено", null);
        }
    }

    private void updateActionIslandVisibility() {
        if (layoutActionIsland == null) return;
        boolean showEdit = layoutEditBar != null && layoutEditBar.getVisibility() == View.VISIBLE;
        boolean showReply = layoutReplyBar != null && layoutReplyBar.getVisibility() == View.VISIBLE;
        boolean showPending = layoutPendingAttachment != null && layoutPendingAttachment.getVisibility() == View.VISIBLE;
        boolean showProgress = layoutSendingProgress != null && layoutSendingProgress.getVisibility() == View.VISIBLE;

        if (showEdit || showReply || showPending || showProgress) {
            if (layoutActionIsland.getVisibility() != View.VISIBLE) {
                layoutActionIsland.setVisibility(View.VISIBLE);
                layoutActionIsland.setAlpha(1f);
            }
        } else {
            if (layoutActionIsland.getVisibility() != View.GONE) {
                layoutActionIsland.setVisibility(View.GONE);
            }
        }
        updateMessageListPadding();
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
        if (layoutActionIsland != null && layoutActionIsland.getVisibility() == View.VISIBLE && layoutActionIsland.getHeight() > 0) {
            bottomPadding += layoutActionIsland.getHeight() + (int) (8 * density);
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
        BlurView blurInput = findViewById(R.id.layoutInput);
        BlurView blurAttachmentPanel = findViewById(R.id.layoutAttachmentPanel);
        BlurView blurMusicIsland = findViewById(R.id.layoutMusicIsland);
        BlurView blurActionIsland = findViewById(R.id.layoutActionIsland);

        BlurView[] blurViews = new BlurView[]{
                blurHeader, blurInput, blurAttachmentPanel, blurMusicIsland, blurActionIsland
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
                if (isConnectionActive()) {
                    String deletionPayload = "DELETE_CHAT:login=" + localUsername + ";name=" + localUsername;
                    sendPacket(TYPE_CHAT_DELETED, deletionPayload.getBytes(StandardCharsets.UTF_8));
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
        if (tvChatStatus == null) return;

        int textColor = resolveColor(colorResOrValue);
        CharSequence currentText = tvChatStatus.getText();
        if (currentText != null && Objects.equals(currentText.toString(), newText) && tvChatStatus.getAlpha() >= 0.95f && tvChatStatus.getTranslationY() == 0f) {
            tvChatStatus.setTextColor(textColor);
            return;
        }

        try {
            sendBroadcast(new Intent("com.messenger.prime.STATUS_UPDATED").setPackage(getPackageName()));
        } catch (Exception ignored) {}

        tvChatStatus.animate().cancel();
        tvChatStatus.animate()
                .translationY(25f)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    if (tvChatStatus == null) return;
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
            viewStatus.setVisibility(connected ? View.VISIBLE : View.GONE);
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
            statusAnimHandler.postDelayed(this, 500);
        }
    };

    private void setStatusTextDirect(String newText, int colorResOrValue) {
        if (tvChatStatus == null) return;
        int finalColor = resolveColor(colorResOrValue);
        tvChatStatus.setText(newText);
        tvChatStatus.setTextColor(finalColor);
    }

    private void startAnimatingStatus(String baseStatus, int colorResOrValue) {
        int color = resolveColor(colorResOrValue);
        if (Objects.equals(currentBaseStatus, baseStatus) && currentStatusColor == color) {
            return;
        }
        statusAnimHandler.removeCallbacks(statusAnimRunnable);
        currentBaseStatus = baseStatus;
        currentStatusColor = color;
        statusDotCount = 0;

        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE);
        boolean disableAnim = sp.getBoolean("disable_animations", false);
        if (disableAnim) {
            setStatusTextDirect(baseStatus, currentStatusColor);
        } else {
            setStatusWithAnimation(baseStatus, currentStatusColor, false);
        }
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

    private boolean isGenericName(String name) {
        if (name == null) return true;
        String trimmed = name.trim().toLowerCase(Locale.US);
        return trimmed.isEmpty() ||
                trimmed.equals("собеседник") ||
                trimmed.equals("prime собеседник") ||
                trimmed.equals("пользователь") ||
                trimmed.equals("prime user") ||
                trimmed.equals("контакт") ||
                trimmed.equals("1") ||
                trimmed.equals("null");
    }

    private boolean isLocalUserKey(String key) {
        if (key == null || key.trim().isEmpty()) return true;
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String currentUser = sp.getString("current_user", "");
        String myDisplayName = sp.getString(currentUser + "_name", currentUser);

        return key.equalsIgnoreCase(currentUser) ||
                key.equalsIgnoreCase(myDisplayName) ||
                (localUsername != null && !localUsername.isEmpty() && key.equalsIgnoreCase(localUsername)) ||
                isGenericName(key);
    }

    private String lastLoadedAvatarTag = null;

    private void updateAvatarUi(String avatarUri, String name) {
        ImageView ivChatAvatar = findViewById(R.id.ivChatAvatar);
        if (ivChatAvatar == null) return;

        String avatarToLoad = AvatarManager.getContactAvatarUriOrFile(this, targetUsername, name, deviceAddress);
        if (avatarToLoad == null) avatarToLoad = avatarUri;

        String currentTag = (avatarToLoad != null ? avatarToLoad : "") + "::" + (name != null ? name : "");
        if (Objects.equals(lastLoadedAvatarTag, currentTag) && ivChatAvatar.getDrawable() != null) {
            return;
        }
        lastLoadedAvatarTag = currentTag;

        AvatarManager.invalidateAvatarFileCache();
        if (avatarToLoad != null && !avatarToLoad.isEmpty()) {
            AvatarUtilsKt.loadAvatarIntoView(this, avatarToLoad, ivChatAvatar, name != null ? name : "");
        } else {
            Bitmap letterBmp = createLetterAvatar(name != null ? name : "P", 120);
            ivChatAvatar.setImageBitmap(letterBmp);
        }
    }

    private boolean isRecipientVersionBelow64() {
        if (remoteVersionCode == -1) return true;
        return remoteVersionCode < 64;
    }

    private void sendUpdateTextToRecipient() {
        try {
            long timestamp = System.currentTimeMillis();
            String msgId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();
            String updateText = "Это сообщение доступно в более свежей версии приложения Prime. Пожалуйста, обновите приложение до последней версии в настройках или по ссылке https://github.com/Vaniahorosh/Prime_messenger/releases";
            String packetContent = msgId + ":::" + updateText;
            sendPacket(TYPE_TEXT, packetContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Error sending update text packet", e);
        }
    }

    private void sendVoiceMessage(String path, java.util.List<Integer> amps) {
        if (path == null) return;
        File voiceFile = new File(path);
        if (!voiceFile.exists()) return;

        if (isRecipientVersionBelow64()) {
            sendUpdateTextToRecipient();
            return;
        }

        String durStr = "00:00";
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(voiceFile.getAbsolutePath());
            String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) {
                long ms = Long.parseLong(d);
                long sec = (ms / 1000) % 60;
                long min = (ms / 1000) / 60;
                durStr = String.format(Locale.US, "%02d:%02d", min, sec);
            }
        } catch (Exception ignored) {
        } finally {
            if (mmr != null) {
                try { mmr.release(); } catch (Exception ignored) {}
            }
        }

        PendingAttachmentItem pendingItem = new PendingAttachmentItem(
            Uri.fromFile(voiceFile),
            voiceFile.getAbsolutePath(),
            voiceFile.getName(),
            voiceFile.length(),
            false,
            true,
            ChatMessage.MessageType.VOICE,
            null,
            durStr
        );
        pendingItem.waveformChunks = amps;
        sendVideoOrFile(pendingItem, "Голосовое сообщение");
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

        if (!message.isOutgoing() && isActivityForeground) {
            message.setMessageStatus(MessageStatus.READ);
        }
        
        chatAdapter.addMessageAnimated(message);
        if (rvMessages != null) {
            rvMessages.post(() -> {
                if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
                    rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }
            });
        }
        
        // Save to persistence
        ChatHistoryManager.saveMessage(this, targetUsername, message);
        String lastMsg = ChatMessage.getSummaryDescription(message);
        saveLastMessageToChatList(
            lastMsg,
            message.isOutgoing() ? message.getMessageStatus() : MessageStatus.NONE,
            false,
            "ONLINE"
        );
    }

    @SuppressWarnings("unchecked")
    private void openPersonInformationActivity() {
        if (isMultiSelectionActive || (chatAdapter != null && chatAdapter.isMultiSelectionMode())) {
            return;
        }
        Intent intent = new Intent(this, PersonInformationActivity.class);
        intent.putExtra("EXTRA_CHAT_NAME", targetUsername);
        intent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);

        String avatarToPass = remoteAvatarUri;
        if (avatarToPass == null || avatarToPass.isEmpty()) {
            avatarToPass = AvatarManager.getContactAvatarUriOrFile(this, targetUsername, targetUsername, deviceAddress);
        }

        intent.putExtra("EXTRA_AVATAR_URI", avatarToPass);
        intent.putExtra("EXTRA_IS_ONLINE", isRemoteUserOnline);
        setOpeningSubActivity(true);

        PrimeTransitions.startActivityWithTransition(this, intent);
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Введите текст для поиска...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#80FFFFFF"));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        PrimeBlurDialog.show(
                this,
                "Поиск сообщений",
                null,
                "Найти",
                "Отмена",
                null,
                false,
                input,
                () -> {
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
                    return kotlin.Unit.INSTANCE;
                },
                null
        );
    }



    private void performCompleteChatDeletion() {
        isChatDeleted = true;
        if (isConnectionActive()) {
            String deletionPayload = "DELETE_CHAT:login=" + localUsername + ";name=" + localUsername;
            sendPacket(TYPE_CHAT_DELETED, deletionPayload.getBytes(StandardCharsets.UTF_8));
            new Handler(Looper.getMainLooper()).postDelayed(this::finalizeChatDeletion, 200);
        } else {
            finalizeChatDeletion();
        }
    }

    private void finalizeChatDeletion() {
        String targetAddr = (deviceAddress != null && !deviceAddress.isEmpty()) ? deviceAddress : targetUsername;
        
        BluetoothSocketHolder.removeConnection(deviceAddress, targetUsername);
        if (targetAddr != null && !targetAddr.isEmpty()) {
            BluetoothConnectionManager.getInstance().disconnect(targetAddr);
        }

        DeviceBindingManager.unbindDeviceSecurely(this, targetUsername, deviceAddress);

        if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
            PrimeBluetoothService.stopService(this);
        }

        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
        deleteChatFromChatListEx(targetUsername);
        if (deviceAddress != null) deleteChatFromChatListEx(deviceAddress);

        PrimeNotification.cancelNotificationForUser(this, targetUsername, deviceAddress);

        Intent chatDeletedIntent = new Intent("com.messenger.prime.CHAT_DELETED");
        chatDeletedIntent.setPackage(getPackageName());
        sendBroadcast(chatDeletedIntent);

        if (chatAdapter != null) chatAdapter.setMessages(new ArrayList<>());
        disconnectCurrentChat();
        Toast.makeText(this, "Переписка и устройство полностью удалены", Toast.LENGTH_SHORT).show();

        Intent mainIntent = new Intent(this, ChatListActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void deleteChatFromChatList(String targetName) {
        deleteChatFromChatListEx(targetName);
    }

    private void deleteChatFromChatListEx(String nameOrId) {
        isChatDeleted = true;
        autoRetryHandler.removeCallbacks(autoRetryRunnable);
        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        BluetoothSocketHolder.removeConnection(deviceAddress, targetUsername);
        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress);
        if (nameOrId != null && !nameOrId.equalsIgnoreCase(targetUsername)) {
            ChatHistoryManager.deleteHistoryCompletely(this, nameOrId, deviceAddress);
        }

        if (chatAdapter != null) {
            chatAdapter.setMessages(new ArrayList<>());
        }

        try {
            String json = ChatHistoryManager.getPersistedChatsJson(this);
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
            ChatHistoryManager.savePersistedChatsJson(this, newArray.toString());
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
            
            // Удаляем только старый ключ из SharedPreferences, не трогая физические медиа-файлы!
            SharedPreferences prefs = getSharedPreferences("PrimeChatHistory", Context.MODE_PRIVATE);
            prefs.edit().remove("history_" + oldKey).apply();
        }
    }

    private void saveLastMessageToChatList(String lastMsg, MessageStatus messageStatus, boolean incrementUnread, String onlineStatusStr) {
        saveLastMessageToChatListForUser(targetUsername, lastMsg, messageStatus, incrementUnread, onlineStatusStr);
    }

    private void saveLastMessageToChatListForUser(String user, String lastMsg, MessageStatus messageStatus, boolean incrementUnread, String onlineStatusStr) {
        if (isChatDeleted || user == null || user.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String myName = sharedPrefs.getString("my_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("my_local_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("current_user_name", null);
            if (isLocalUserKey(user)) {
                return;
            }
            String avatarUriToUse = sharedPrefs.getString(user + "_avatar", null);
            if (avatarUriToUse == null) avatarUriToUse = sharedPrefs.getString(user + "_avatarUri", null);
            if (avatarUriToUse == null && deviceAddress != null) {
                avatarUriToUse = sharedPrefs.getString(deviceAddress + "_avatar", null);
                if (avatarUriToUse == null) avatarUriToUse = sharedPrefs.getString(deviceAddress + "_avatarUri", null);
            }
            if (avatarUriToUse == null && user.equalsIgnoreCase(targetUsername) && remoteAvatarUri != null && !remoteAvatarUri.isEmpty()) {
                avatarUriToUse = remoteAvatarUri;
            }

            if (avatarUriToUse == null) {
                File[] possible = new File[] {
                    new File(getFilesDir(), "rec_avatar_" + user + ".gif"),
                    new File(getFilesDir(), "rec_avatar_" + user + ".jpg"),
                    deviceAddress != null ? new File(getFilesDir(), "rec_avatar_" + deviceAddress + ".gif") : null,
                    deviceAddress != null ? new File(getFilesDir(), "rec_avatar_" + deviceAddress + ".jpg") : null
                };
                for (File f : possible) {
                    if (f != null && f.exists() && f.length() > 0) {
                        avatarUriToUse = Uri.fromFile(f).toString();
                        break;
                    }
                }
            }
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            JSONObject updatedObj = null;
            JSONArray newArray = new JSONArray();
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                
                boolean isGenericName = "Собеседник".equalsIgnoreCase(user) || "Prime Собеседник".equalsIgnoreCase(user) || "Контакт".equalsIgnoreCase(user);
                boolean isMatch = false;
                if (updatedObj == null) {
                    String objId = obj.optString("id", "");
                    String objName = obj.optString("name", "");
                    if (deviceAddress != null && !deviceAddress.isEmpty() && deviceAddress.equalsIgnoreCase(objId)) {
                        isMatch = true;
                    } else if (user != null && !user.isEmpty() && user.equalsIgnoreCase(objId)) {
                        isMatch = true;
                    } else if (!isGenericName && user != null && !user.isEmpty() && user.equalsIgnoreCase(objName) && !isValidMacAddress(user)) {
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
                    } else if (user.equalsIgnoreCase(targetUsername)) {
                        obj.put("unreadCount", 0);
                    }
                    if (onlineStatusStr != null) {
                        obj.put("onlineStatus", onlineStatusStr);
                    }
                    if (avatarUriToUse != null && !avatarUriToUse.isEmpty()) {
                        obj.put("avatarUri", avatarUriToUse);
                    }
                    String nameToSave = user;
                    if (deviceAddress != null && !deviceAddress.isEmpty()) {
                        String mappedName = sharedPrefs.getString("contact_name_" + deviceAddress, null);
                        if (mappedName != null && !mappedName.isEmpty() && !mappedName.equals("1")) {
                            nameToSave = mappedName;
                        }
                    }
                    if (nameToSave != null && !nameToSave.isEmpty()) {
                        obj.put("name", nameToSave);
                    }
                    updatedObj = obj;
                } else {
                    newArray.put(obj);
                }
            }

            if (updatedObj == null) {
                String actualLastMsg = lastMsg;
                if (actualLastMsg == null) {
                    List<ChatMessage> history = ChatHistoryManager.loadMessages(this, user);
                    if (history != null && !history.isEmpty()) {
                        ChatMessage lastHistoryMsg = history.get(history.size() - 1);
                        actualLastMsg = lastHistoryMsg.getText();
                        if (actualLastMsg == null || actualLastMsg.isEmpty()) {
                            actualLastMsg = ChatMessage.getSummaryDescription(lastHistoryMsg);
                        }
                    }
                }

                updatedObj = new JSONObject();
                updatedObj.put("id", (user.equalsIgnoreCase(targetUsername) && deviceAddress != null && !deviceAddress.isEmpty()) ? deviceAddress : user);
                updatedObj.put("name", user);
                updatedObj.put("lastMessage", actualLastMsg != null ? actualLastMsg : "");
                updatedObj.put("time", timeStr);
                updatedObj.put("avatarUri", avatarUriToUse != null ? avatarUriToUse : "");
                updatedObj.put("onlineStatus", onlineStatusStr != null ? onlineStatusStr : (user.equalsIgnoreCase(targetUsername) && isRemoteUserOnline ? "ONLINE" : "OFFLINE"));
                updatedObj.put("messageStatus", messageStatus != null ? messageStatus.name() : "NONE");
                updatedObj.put("unreadCount", incrementUnread ? 1 : 0);
                updatedObj.put("isMuted", false);
            }

            String mediaPathToSave = ChatHistoryManager.getLastMediaPreviewPath(this, user, deviceAddress);
            if (mediaPathToSave != null && !mediaPathToSave.isEmpty()) {
                updatedObj.put("mediaPreviewPath", mediaPathToSave);
            } else {
                updatedObj.remove("mediaPreviewPath");
            }

            // Put active updated chat at top (index 0)
            JSONArray finalArray = new JSONArray();
            finalArray.put(updatedObj);
            for (int i = 0; i < newArray.length(); i++) {
                finalArray.put(newArray.get(i));
            }

            String newJsonStr = finalArray.toString();
            if (!newJsonStr.equals(json)) {
                sharedPrefs.edit().putString(ChatHistoryManager.getPersistedChatsKey(this), newJsonStr).apply();
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats", e);
        }
    }

    public static String extractMsgId(String data) {
        if (data == null) return null;
        int sep = data.indexOf(":::");
        return sep != -1 ? data.substring(0, sep) : null;
    }

    private void updatePersistedChatUser(String devAddress, String oldName, String newName, String login) {
        if (newName == null || newName.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String myName = sharedPrefs.getString("my_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("my_local_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("current_user_name", null);
            if (myName != null && !myName.isEmpty() && newName.equalsIgnoreCase(myName)) {
                return;
            }

            String jsonChats = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray chatArray = new JSONArray(jsonChats);
            boolean updated = false;

            for (int i = 0; i < chatArray.length(); i++) {
                JSONObject obj = chatArray.getJSONObject(i);
                String objId = obj.optString("id", "");
                String objName = obj.optString("name", "");

                boolean isGenericOldName = "Собеседник".equalsIgnoreCase(oldName) || "Prime Собеседник".equalsIgnoreCase(oldName) || "Контакт".equalsIgnoreCase(oldName);
                boolean isGenericObjName = "Собеседник".equalsIgnoreCase(objName) || "Prime Собеседник".equalsIgnoreCase(objName) || "Контакт".equalsIgnoreCase(objName);

                boolean matches = false;
                if (devAddress != null && !devAddress.isEmpty() && (devAddress.equalsIgnoreCase(objId) || devAddress.equalsIgnoreCase(objName))) {
                    matches = true;
                } else if (!isGenericOldName && oldName != null && !oldName.isEmpty() && (oldName.equalsIgnoreCase(objName) || oldName.equalsIgnoreCase(objId))) {
                    matches = true;
                } else if (!isGenericObjName && newName != null && !newName.isEmpty() && (newName.equalsIgnoreCase(objName) || newName.equalsIgnoreCase(objId))) {
                    matches = true;
                }

                if (matches) {
                    obj.put("name", newName);
                    if (devAddress != null && !devAddress.isEmpty()) {
                        obj.put("id", devAddress);
                    }
                    if (login != null && !login.isEmpty()) {
                        obj.put("login", login);
                    }

                    String avatarUri = AvatarManager.getContactAvatarUriOrFile(this, newName, newName, devAddress);
                    if (avatarUri != null && !avatarUri.isEmpty()) {
                        obj.put("avatarUri", avatarUri);
                    }
                    updated = true;
                }
            }

            if (!updated) {
                JSONObject newChat = new JSONObject();
                newChat.put("id", devAddress != null ? devAddress : newName);
                newChat.put("name", newName);
                if (login != null) newChat.put("login", login);
                newChat.put("lastMessage", "");
                newChat.put("time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                newChat.put("unreadCount", 0);
                newChat.put("onlineStatus", "ONLINE");
                newChat.put("messageStatus", "NONE");

                String avatarUri = AvatarManager.getContactAvatarUriOrFile(this, newName, newName, devAddress);
                if (avatarUri != null && !avatarUri.isEmpty()) {
                    newChat.put("avatarUri", avatarUri);
                }
                chatArray.put(newChat);
            }

            sharedPrefs.edit().putString(ChatHistoryManager.getPersistedChatsKey(this), chatArray.toString()).apply();
            ChatListNotifier.INSTANCE.notifyChanged();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats", e);
        }
    }

    private void updatePersistedChatAvatar(String username, String avatarUri) {
        if (username == null || username.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            boolean updated = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String objName = obj.optString("name", "");
                String objId = obj.optString("id", "");

                if (username.equalsIgnoreCase(objName) || (deviceAddress != null && deviceAddress.equalsIgnoreCase(objId))) {
                    if (avatarUri != null) {
                        obj.put("avatarUri", avatarUri);
                    } else {
                        obj.remove("avatarUri");
                    }
                    updated = true;
                }
            }

            if (updated) {
                sharedPrefs.edit().putString(ChatHistoryManager.getPersistedChatsKey(this), array.toString()).apply();
                ChatListNotifier.INSTANCE.notifyChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats avatar", e);
        }
    }

    private final Runnable clearChatListTypingRunnable = () -> {
        saveActivityStateToChatList(targetUsername, "IDLE");
    };

    private void saveActivityStateToChatList(String targetName, String state) {
        if (isChatDeleted || targetName == null || targetName.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray array = new JSONArray(json);
            boolean updated = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                boolean isGenericName = "Собеседник".equalsIgnoreCase(targetName) || "Prime Собеседник".equalsIgnoreCase(targetName) || "Контакт".equalsIgnoreCase(targetName);
                boolean isMatch = false;
                if (deviceAddress != null && !deviceAddress.isEmpty() && deviceAddress.equalsIgnoreCase(obj.optString("id"))) {
                    isMatch = true;
                } else if (!isGenericName && targetName.equalsIgnoreCase(obj.optString("name")) && !isValidMacAddress(targetName)) {
                    isMatch = true;
                } else if (targetName.equalsIgnoreCase(obj.optString("id"))) {
                    isMatch = true;
                }

                if (isMatch) {
                    boolean isNonIdle = state != null && !"IDLE".equalsIgnoreCase(state);
                    obj.put("activityState", isNonIdle ? state : "IDLE");
                    if (isNonIdle) {
                        long duration = (state.toUpperCase(Locale.US).startsWith("SENDING_") || state.toUpperCase(Locale.US).startsWith("SELECTING_")) ? 10000L : 5000L;
                        obj.put("typingUntil", System.currentTimeMillis() + duration);
                        obj.put("isTyping", true);
                    } else {
                        obj.put("typingUntil", 0L);
                        obj.put("isTyping", false);
                    }
                    updated = true;
                    break;
                }
            }
            if (updated) {
                sharedPrefs.edit().putString(ChatHistoryManager.getPersistedChatsKey(this), array.toString()).apply();
                ChatListNotifier.INSTANCE.notifyChanged();
            }

            if (state != null && !"IDLE".equalsIgnoreCase(state)) {
                long duration = (state.toUpperCase(Locale.US).startsWith("SENDING_") || state.toUpperCase(Locale.US).startsWith("SELECTING_")) ? 10200L : 5200L;
                typingResetHandler.removeCallbacks(clearChatListTypingRunnable);
                typingResetHandler.postDelayed(clearChatListTypingRunnable, duration);
            } else {
                typingResetHandler.removeCallbacks(clearChatListTypingRunnable);
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
        if (!isConnectionActive()) return;

        if (pendingReadReceiptMsgId != null) {
            String receiptPayload = "READ_ALL:::" + pendingReadReceiptMsgId;
            sendPacket(TYPE_READ_RECEIPT, receiptPayload.getBytes(StandardCharsets.UTF_8));
            pendingReadReceiptMsgId = null;
        }

        while (!pendingMessageQueue.isEmpty()) {
            PendingMessage pm = pendingMessageQueue.poll();
            if (pm != null) {
                sendPacket(pm.type, pm.payload);
            }
        }

        runOnIoThread(() -> {
            List<ChatMessage> history = ChatHistoryManager.loadMessages(ChatPersonActivity.this, targetUsername);
            for (ChatMessage m : history) {
                if (m.isOutgoing() && (m.getMessageStatus() == MessageStatus.SENDING || m.getMessageStatus() == MessageStatus.NONE || m.getMessageStatus() == MessageStatus.ERROR)) {
                    String msgId = m.getMessageId();
                    String text = m.getText();
                    String imgPath = m.getImagePath();

                    if (imgPath != null && new File(imgPath).exists()) {
                        m.setMessageStatus(MessageStatus.SENT);
                        ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, msgId, MessageStatus.SENT);
                        runOnUiThread(() -> {
                            if (chatAdapter != null && msgId != null) {
                                chatAdapter.updateMessageStatusById(msgId, MessageStatus.SENT);
                            }
                        });
                    } else if (text != null && !text.isEmpty()) {
                        String packetContent = (msgId != null ? msgId : (localUsername + "_" + m.getTimestamp())) + ":::" + text;
                        if (m.isReply()) {
                            packetContent += ":::REPLY:::" + m.getReplyToMessageId() + ":::" + m.getReplyToSender() + ":::" + m.getReplyToText();
                        }
                        sendPacket(TYPE_TEXT, packetContent.getBytes(StandardCharsets.UTF_8));
                        m.setMessageStatus(MessageStatus.SENT);
                        ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, msgId, MessageStatus.SENT);
                        runOnUiThread(() -> {
                            if (chatAdapter != null && msgId != null) {
                                chatAdapter.updateMessageStatusById(msgId, MessageStatus.SENT);
                            }
                        });
                    }
                }
            }
        });
    }

    private void sendReadReceiptsForUnreadMessages() {
        if (chatAdapter == null) return;
        
        String lastUnreadMsgId = pendingReadReceiptMsgId;
        boolean hasNewUnread = false;
        for (int i = 0; i < chatAdapter.getItemCount(); i++) {
            ChatMessage m = chatAdapter.getMessageAt(i);
            if (m != null && !m.isOutgoing() && m.getMessageStatus() != MessageStatus.READ) {
                m.setMessageStatus(MessageStatus.READ);
                lastUnreadMsgId = m.getMessageId();
                hasNewUnread = true;
            }
        }

        if (lastUnreadMsgId != null || hasNewUnread) {
            if (hasNewUnread) {
                ChatHistoryManager.markIncomingMessagesAsRead(this, targetUsername, lastUnreadMsgId);
                runOnUiThread(() -> {
                    if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
                });
            }
            if (isConnectionActive() && lastUnreadMsgId != null) {
                String receiptPayload = "READ_ALL:::" + lastUnreadMsgId;
                sendPacket(TYPE_READ_RECEIPT, receiptPayload.getBytes(StandardCharsets.UTF_8));
                pendingReadReceiptMsgId = null;
            } else if (lastUnreadMsgId != null) {
                pendingReadReceiptMsgId = lastUnreadMsgId;
            }
        }
        ChatHistoryManager.clearUnreadCountInPersistedChats(this, targetUsername);
        if (deviceAddress != null) {
            ChatHistoryManager.clearUnreadCountInPersistedChats(this, deviceAddress);
        }
        PrimeNotification.cancelNotificationForUser(this, targetUsername, deviceAddress);
    }

    private String resolveTargetAddress() {
        if (deviceAddress != null && !deviceAddress.isEmpty() && isValidMacAddress(deviceAddress)) {
            return deviceAddress;
        }
        if (targetUsername != null && isValidMacAddress(targetUsername)) {
            return targetUsername;
        }
        String mac = findMacForTargetUsername(targetUsername);
        if (mac != null && !mac.isEmpty() && isValidMacAddress(mac)) {
            return mac;
        }
        return deviceAddress != null && !deviceAddress.isEmpty() ? deviceAddress : targetUsername;
    }

    public boolean isConnectionActive() {
        if (isRemoteUserOnline) return true;
        if (PrimeCompanionServer.INSTANCE.hasConnectedClients()) return true;
        if (deviceAddress != null && !deviceAddress.isEmpty() && BluetoothConnectionManager.getInstance().isConnected(deviceAddress)) return true;
        if (targetUsername != null && !targetUsername.isEmpty() && BluetoothConnectionManager.getInstance().isConnected(targetUsername)) return true;
        if (BluetoothSocketHolder.isConnectedWith(deviceAddress, targetUsername)) return true;
        String targetAddr = resolveTargetAddress();
        if (targetAddr != null && !targetAddr.isEmpty() && BluetoothConnectionManager.getInstance().isConnected(targetAddr)) return true;
        if (targetAddr != null && !targetAddr.isEmpty() && BluetoothSocketHolder.isConnectedWith(targetAddr, targetUsername)) return true;
        if (BluetoothConnectionManager.getInstance().getConnectedDeviceCount() > 0) return true;
        if (BluetoothSocketHolder.hasAnyActiveConnection()) return true;
        return false;
    }

    private void sendPacket(byte type, byte[] payload) {
        String targetAddr = resolveTargetAddress();

        if (type == TYPE_TEXT || type == TYPE_PHOTO || type == TYPE_FILE || type == TYPE_EDIT || type == TYPE_REACTION) {
            if (!isConnectionActive() && !isRemoteUserOnline) {
                return;
            }
        }

        boolean sentViaSocketHolder = BluetoothSocketHolder.sendPacket(targetAddr, targetUsername, type, payload);
        if (!sentViaSocketHolder) {
            BluetoothConnectionManager.getInstance().sendPacket(targetAddr, type, payload);
        }
        PrimeCompanionServer.INSTANCE.broadcastPacket(type, payload);
    }

    private void sendText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!isConnectionActive()) {
            PrimeNotification.show(this, "Сообщение не отправлено: нет подключения к собеседнику");
            updateInputVisibility(false);
            return;
        }
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");
        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);
        ChatMessage message = new ChatMessage(text, time, localUsername, true, null, timestamp, null, messageId);
        
        if (replyingToMessage != null) {
            message.setReplyToMessageId(replyingToMessage.getMessageId());
            message.setReplyToSender(replyingToMessage.getSenderLogin());
            String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
            if (replyingToMessage.isVideoNote() || (qText != null && (qText.startsWith("VIDEO_NOTE:") || qText.startsWith("vn_")))) {
                qText = "📹 Видеосообщение";
            }
            message.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Сообщение");
            cancelReplyMode();
        }

        boolean isConnected = isConnectionActive();
        message.setMessageStatus(isConnected ? MessageStatus.SENT : MessageStatus.SENDING);

        String packetContent = message.getMessageId() + ":::" + text;
        if (message.isReply()) {
            packetContent += ":::REPLY:::" + message.getReplyToMessageId() + ":::" + message.getReplyToSender() + ":::" + message.getReplyToText();
        }

        byte[] payload = packetContent.getBytes(StandardCharsets.UTF_8);
        if (isConnected) {
            sendPacket(TYPE_TEXT, payload);
        } else {
            pendingMessageQueue.add(new PendingMessage(TYPE_TEXT, payload));
            runOnUiThread(this::startPrimeConnection);
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

        runOnIoThread(() -> {
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
            multiMsg.setMessageStatus(MessageStatus.SENDING);
            multiMsg.setSendingProgress(0);

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
                activeSendingMessageId = messageId;
                activeSendingTargetUsername = targetUsername;
            if (isConnectionActive()) {
                sendPacket(TYPE_PHOTO, fullPayload);
            } else {
                pendingMessageQueue.add(new PendingMessage(TYPE_PHOTO, fullPayload));
                runOnUiThread(this::startPrimeConnection);
            }

            runOnUiThread(() -> {
                showSendingProgressUi(100);
                addMessageToUI(multiMsg);
                ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, multiMsg);
                saveLastMessageToChatList("Медиафайлы (" + items.size() + ")", MessageStatus.SENT, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
            });
        });
    }

    private void sendPhoto(Uri uri, String captionText) {
        if (uri == null) return;
        senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
        sendActivityState("IDLE");

        runOnIoThread(() -> {
            try {
                boolean isGif = false;
                if (uri != null) {
                    String uriStr = uri.toString().toLowerCase(Locale.US);
                    String mime = getContentResolver().getType(uri);
                    if (uriStr.endsWith(".gif") || "image/gif".equalsIgnoreCase(mime)) {
                        isGif = true;
                    }
                }

                byte[] photoBytes = null;

                if (isGif) {
                    try (InputStream is = getContentResolver().openInputStream(uri);
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        if (is != null) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = is.read(buf)) != -1) {
                                baos.write(buf, 0, len);
                            }
                            photoBytes = baos.toByteArray();
                        }
                    }
                } else {
                    Bitmap bitmap = decodeSampledBitmapFromUri(uri, 1024, 1024);
                    if (bitmap != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                        photoBytes = baos.toByteArray();
                    }
                }

                if (photoBytes != null && photoBytes.length > 0) {
                    long timestamp = System.currentTimeMillis();
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                    String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();

                    String ext = isGif ? ".gif" : ".jpg";
                    String savedPhotoPath = null;
                    File photoFile = ChatHistoryManager.saveBytesToAtomicFile(this, "sent_photo_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 4) + ext, photoBytes);
                    if (photoFile != null) {
                        savedPhotoPath = photoFile.getAbsolutePath();
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

                    final String photoPath = savedPhotoPath;
                    ChatMessage photoMsg = new ChatMessage(captionText, time, localUsername, true, null, timestamp, photoPath, messageId);
                    photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);
                    photoMsg.setMessageStatus(MessageStatus.SENDING);
                    photoMsg.setSendingProgress(0);
                    if (replyingToMessage != null) {
                        photoMsg.setReplyToMessageId(replyingToMessage.getMessageId());
                        photoMsg.setReplyToSender(replyingToMessage.getSenderLogin());
                        String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
                        photoMsg.setReplyToText(qText != null && !qText.isEmpty() ? qText : "Фотография");
                    }

                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    runOnUiThread(() -> {
                        cancelReplyMode();
                        showSendingProgressUi(0);
                        saveActivityStateToChatList(targetUsername, "SENDING_MEDIA");
                        sendActivityState("SENDING_MEDIA");
                        addMessageToUI(photoMsg);
                        ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, photoMsg);
                        saveLastMessageToChatList("Фотография", MessageStatus.SENDING, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                        latch.countDown();
                    });

                    try {
                        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}

                    currentSendingMessageId = messageId;
                activeSendingMessageId = messageId;
                activeSendingTargetUsername = targetUsername;

                    if (isConnectionActive()) {
                        sendPacket(TYPE_PHOTO, fullPayload);
                    } else {
                        pendingMessageQueue.add(new PendingMessage(TYPE_PHOTO, fullPayload));
                        runOnUiThread(this::startPrimeConnection);
                    }
                } else {
                    Log.e(TAG, "Failed to decode photo from URI: " + uri);
                    runOnUiThread(() -> PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Не удалось загрузить фото", null));
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to load photo", e);
                runOnUiThread(() -> PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Не удалось загрузить фото", null));
            }
        });
    }

    private String lastSentAvatarChecksum = "";

    private static Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to decode sampled bitmap from byte array", e);
        }
        return null;
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        if (uri == null) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    BitmapFactory.decodeStream(is, null, options);
                }
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    return BitmapFactory.decodeStream(is, null, options);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to decode sampled bitmap from URI: " + uri, e);
        }
        return null;
    }

    private void sendLocalAvatar() {
        sendLocalAvatar(false);
    }

    private void sendLocalAvatar(boolean force) {
        if (!isConnectionActive()) return;
        runOnIoThread(() -> {
            try {
                String localAvatarUri = AvatarManager.getMyAvatarUri(this);
                if (localAvatarUri != null && !localAvatarUri.isEmpty()) {
                    boolean isGif = localAvatarUri.toLowerCase(Locale.US).endsWith(".gif") || localAvatarUri.toLowerCase(Locale.US).contains("gif");
                    byte[] avatarBytes = null;

                    Uri uri;
                    if (localAvatarUri.startsWith("content://") || localAvatarUri.startsWith("file://") || localAvatarUri.startsWith("android.resource://") || localAvatarUri.startsWith("http")) {
                        uri = Uri.parse(localAvatarUri);
                    } else {
                        uri = Uri.fromFile(new File(localAvatarUri));
                    }

                    if (isGif) {
                        try (InputStream is = getContentResolver().openInputStream(uri);
                             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            if (is != null) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) != -1) {
                                    baos.write(buf, 0, len);
                                }
                                avatarBytes = baos.toByteArray();
                            }
                        }
                    } else {
                        Bitmap bitmap = decodeSampledBitmapFromUri(uri, 256, 256);
                        if (bitmap != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos);
                            avatarBytes = baos.toByteArray();
                            bitmap.recycle();
                        }
                    }

                    if (avatarBytes != null && avatarBytes.length > 0) {
                        String currentPeerKey = (deviceAddress != null ? deviceAddress : "") + "_" + (targetUsername != null ? targetUsername : "");
                        String checksum = localAvatarUri + "_" + avatarBytes.length + "_" + currentPeerKey;

                        if (!force && checksum.equals(lastSentAvatarChecksum)) {
                            Log.d(TAG, "Avatar unchanged and already sent to peer, skipping duplicate send.");
                            return;
                        }

                        lastSentAvatarChecksum = checksum;
                        sendPacket(TYPE_AVATAR, avatarBytes);
                    } else {
                        if (force || !"EMPTY_AVATAR".equals(lastSentAvatarChecksum)) {
                            lastSentAvatarChecksum = "EMPTY_AVATAR";
                            sendPacket(TYPE_AVATAR, new byte[0]);
                        }
                    }
                } else {
                    if (force || !"EMPTY_AVATAR".equals(lastSentAvatarChecksum)) {
                        lastSentAvatarChecksum = "EMPTY_AVATAR";
                        sendPacket(TYPE_AVATAR, new byte[0]);
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to send local avatar", e);
            }
        });
    }

    private boolean isValidMacAddress(String address) {
        if (address == null || address.isEmpty() || "null".equalsIgnoreCase(address)) return false;
        try {
            return BluetoothAdapter.checkBluetoothAddress(address.toUpperCase(Locale.US));
        } catch (Exception e) {
            return false;
        }
    }

    private String formatDisplayName(String name) {
        if (name == null || name.isEmpty() || isValidMacAddress(name)) {
            name = "Собеседник";
        }
        boolean isDesktop = false;
        try {
            isDesktop = "true".equalsIgnoreCase(AvatarManager.getContactPreferenceSafely(this, name, "is_desktop"))
                     || (deviceAddress != null && "true".equalsIgnoreCase(AvatarManager.getContactPreferenceSafely(this, deviceAddress, "is_desktop")));
        } catch (Exception ignored) {}
        if (isDesktop && !name.contains("💻")) {
            return name + " 💻";
        }
        return name;
    }

    private String findMacForTargetUsername(String targetName) {
        if (targetName == null || targetName.isEmpty()) return null;
        if (isValidMacAddress(targetName)) return targetName;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String jsonChats = ChatHistoryManager.getPersistedChatsJson(this);
            JSONArray chatArray = new JSONArray(jsonChats);
            for (int i = 0; i < chatArray.length(); i++) {
                JSONObject obj = chatArray.getJSONObject(i);
                String name = obj.optString("name");
                String login = obj.optString("login");
                String id = obj.optString("id");
                if ((targetName.equalsIgnoreCase(name) || targetName.equalsIgnoreCase(login)) && isValidMacAddress(id)) {
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
            if (bonded != null && !bonded.isEmpty()) {
                for (BluetoothDevice dev : bonded) {
                    @SuppressLint("MissingPermission")
                    String devName = dev.getName();
                    if (devName != null && (devName.equalsIgnoreCase(targetName) || devName.contains("Prime"))) {
                        return dev.getAddress();
                    }
                }
                return bonded.iterator().next().getAddress();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to find MAC for target username", e);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private void startPrimeConnection() {
        startPrimeConnection(false);
    }

    @SuppressLint("MissingPermission")
    private void startPrimeConnection(boolean forceClient) {
        if (isChatDeleted || isManuallyDisconnected) return;
        if (isConnectionActive()) {
            updateInputVisibility(true);
            return;
        }

        Log.d(TAG, "Starting Fast Direct Connection via BluetoothConnectionManager (forceClient=" + forceClient + ")...");
        startAnimatingStatus("Установка связи", R.color.prime_accent);
        updateInputVisibility(false);

        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, 25000L);

        try {
            PrimeBluetoothService.startService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start bluetooth service", e);
        }

        String localId = (localUsername != null && !localUsername.isEmpty()) ? localUsername : BluetoothSocketHolder.getLocalDeviceId(this);
        String remoteId = (targetUsername != null && !targetUsername.isEmpty()) ? targetUsername : deviceAddress;
        
        String addressToConnect = deviceAddress;
        if (!isValidMacAddress(addressToConnect)) {
            addressToConnect = getIntent().getStringExtra("EXTRA_DEVICE_ADDRESS");
        }
        if (!isValidMacAddress(addressToConnect)) {
            addressToConnect = findMacForTargetUsername(targetUsername);
        }

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (isValidMacAddress(addressToConnect)) {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addressToConnect);
                BluetoothConnectionManager.getInstance().connectToDevice(
                        bluetoothAdapter,
                        device,
                        UUID_CHAT,
                        localId,
                        remoteId,
                        forceClient
                );
            } else {
                BluetoothConnectionManager.getInstance().startServer(bluetoothAdapter, UUID_CHAT);
            }
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

        String socketAddr = device != null ? device.getAddress() : null;
        if (socketAddr != null && !socketAddr.isEmpty()) {
            this.deviceAddress = socketAddr;
        }

        BluetoothConnectionManager.getInstance().onSocketConnected(socket, device);

        String resolvedAddress = deviceAddress != null ? deviceAddress : (socketAddr != null ? socketAddr : null);
        BluetoothSocketHolder.setActiveDeviceAddress(resolvedAddress);
        BluetoothSocketHolder.setActiveTargetUsername(targetUsername);

        saveLastMessageToChatList(null, null, false, "ONLINE");

        try {
            PrimeBluetoothService.startService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start bluetooth service", e);
        }
    }

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    private ConnectionState currentConnectionState = ConnectionState.DISCONNECTED;
    private static boolean isActivityForeground = false;
    private static String activeChatPersonAddress = null;
    private static String activeChatPersonName = null;

    public static boolean isActivityForeground() {
        return isActivityForeground;
    }

    public static boolean isForegroundWithAddress(String address) {
        if (!isActivityForeground) return false;
        if (address == null || address.isEmpty()) return true;
        if (activeChatPersonAddress != null && activeChatPersonAddress.equalsIgnoreCase(address)) return true;
        if (activeChatPersonName != null && activeChatPersonName.equalsIgnoreCase(address)) return true;
        
        String remoteName = BluetoothConnectionManager.getInstance().getThreadRemoteUsername(address);
        if (remoteName != null && activeChatPersonName != null && remoteName.equalsIgnoreCase(activeChatPersonName)) {
            return true;
        }

        return false;
    }

    private boolean isMatchingDevice(String addrOrName) {
        if (addrOrName == null || addrOrName.isEmpty()) return false;
        if (this.deviceAddress != null && this.deviceAddress.equalsIgnoreCase(addrOrName)) return true;
        if (this.targetUsername != null && this.targetUsername.equalsIgnoreCase(addrOrName)) return true;

        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String savedMac = sp.getString(targetUsername + "_mac", null);
        if (savedMac != null && savedMac.equalsIgnoreCase(addrOrName)) {
            if (this.deviceAddress == null || !isValidMacAddress(this.deviceAddress)) {
                this.deviceAddress = savedMac;
            }
            return true;
        }

        boolean isGeneric = "Собеседник".equalsIgnoreCase(targetUsername) ||
                            "Prime Собеседник".equalsIgnoreCase(targetUsername) ||
                            "Устройство".equalsIgnoreCase(targetUsername) ||
                            targetUsername == null || targetUsername.trim().isEmpty() ||
                            this.deviceAddress == null || !isValidMacAddress(this.deviceAddress);

        if (isGeneric) {
            if (isValidMacAddress(addrOrName)) {
                this.deviceAddress = addrOrName;
                if (targetUsername != null && !targetUsername.trim().isEmpty()) {
                    sp.edit().putString(targetUsername + "_mac", addrOrName).apply();
                }
            }
            return true;
        }

        // Check if the connection belongs to our target username
        if (BluetoothConnectionManager.getInstance().isConnected(addrOrName)) {
            String remoteName = BluetoothConnectionManager.getInstance().getThreadRemoteUsername(addrOrName);
            if (remoteName != null && remoteName.equalsIgnoreCase(targetUsername)) {
                if (this.deviceAddress == null || !isValidMacAddress(this.deviceAddress)) {
                    if (isValidMacAddress(addrOrName)) {
                        this.deviceAddress = addrOrName;
                        sp.edit().putString(targetUsername + "_mac", addrOrName).apply();
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSendProgress(String deviceAddress, int progress) {
        if (!isMatchingDevice(deviceAddress)) return;
        runOnUiThread(() -> {
            if (chatAdapter != null && currentSendingMessageId != null) {
                if (progress >= 0 && progress < 100) {
                    chatAdapter.updateMessageSendingProgress(currentSendingMessageId, progress);
                    showSendingProgressUi(progress);
                } else if (progress >= 100) {
                    chatAdapter.updateMessageSendingProgress(currentSendingMessageId, 100);
                    String msgIdFinished = currentSendingMessageId;
                    chatAdapter.updateMessageStatusById(msgIdFinished, MessageStatus.SENT);
                    ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, msgIdFinished, MessageStatus.SENT);
                    currentSendingMessageId = null;
                    activeSendingMessageId = null;
                    activeSendingTargetUsername = null;
                    hideSendingProgressUi();
                    saveActivityStateToChatList(targetUsername, "IDLE");
                    sendActivityState("IDLE");
                    ChatMessage sentMsg = chatAdapter.findMessageById(msgIdFinished);
                    String summaryStr = "Медиафайл";
                    if (sentMsg != null) {
                        if (sentMsg.getMessageType() == ChatMessage.MessageType.IMAGE) summaryStr = "Фотография";
                        else if (sentMsg.getMessageType() == ChatMessage.MessageType.VIDEO) summaryStr = "Видео: " + (sentMsg.getFileName() != null ? sentMsg.getFileName() : "");
                        else if (sentMsg.getMessageType() == ChatMessage.MessageType.FILE) summaryStr = "Файл: " + (sentMsg.getFileName() != null ? sentMsg.getFileName() : "");
                    }
                    saveLastMessageToChatList(summaryStr, MessageStatus.SENT, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                } else {
                    currentSendingMessageId = null;
                    hideSendingProgressUi();
                    saveActivityStateToChatList(targetUsername, "IDLE");
                    sendActivityState("IDLE");
                }
            }
        });
    }

    private void ensureOnlineStateAndInputVisible() {
        runOnUiThread(() -> {
            isRemoteUserOnline = true;
            connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
            setOnlineStatusIndicator(true);
            updateInputVisibility(true);
            updateConnectionStateInAdapter();
            stopAnimatingStatus();
            setStatusWithAnimation("В сети", R.color.prime_success);
            checkAndUpdateVersionInfoIsland();
        });
    }

    @Override
    public void onStateChanged(String deviceAddress, BluetoothConnectionManager.ConnectionState state, String deviceName) {
        if (!isMatchingDevice(deviceAddress)) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            switch (state) {
                case CONNECTED:
                    isRemoteUserOnline = true;
                    ensureOnlineStateAndInputVisible();
                    sendHandshake();
                    sendLocalAvatar(true);
                    flushPendingMessages();
                    sendReadReceiptsForUnreadMessages();
                    saveLastMessageToChatList(null, null, false, "ONLINE");
                    break;

                case CONNECTING:
                case LISTENING:
                    isRemoteUserOnline = false;
                    setOnlineStatusIndicator(false);
                    startAnimatingStatus("Установка связи", R.color.prime_accent);
                    updateInputVisibility(false);
                    saveLastMessageToChatList(null, null, false, "OFFLINE");
                    break;

                case DISCONNECTED:
                default:
                    isRemoteUserOnline = false;
                    setOnlineStatusIndicator(false);
                    stopAnimatingStatus();
                    updateOfflineLastSeenStatus();
                    updateInputVisibility(false);
                    saveLastMessageToChatList(null, null, false, "OFFLINE");
                    handleDisconnectionUiAndAutoReconnect();
                    break;
            }
            updateConnectionStateInAdapter();
        });
    }

    @Override
    public void onError(String deviceAddress, String errorMessage) {
        if (!isMatchingDevice(deviceAddress)) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            Log.w(TAG, "Bluetooth Connection Error: " + errorMessage);
            
            if (currentSendingMessageId != null) {
                if (chatAdapter != null) chatAdapter.updateMessageStatusById(currentSendingMessageId, MessageStatus.NONE);
                ChatHistoryManager.updateMessageStatus(ChatPersonActivity.this, targetUsername, currentSendingMessageId, MessageStatus.NONE);
                currentSendingMessageId = null;
            }
            if (layoutSendingProgress != null) layoutSendingProgress.setVisibility(View.GONE);
            
            String addr = this.deviceAddress != null ? this.deviceAddress : this.targetUsername;
            if (!BluetoothConnectionManager.getInstance().isConnected(addr)) {
                isRemoteUserOnline = false;
                setOnlineStatusIndicator(false);
                stopAnimatingStatus();
                updateOfflineLastSeenStatus();
                updateInputVisibility(false);
            }
        });
    }

    @Override
    public void onPacketReceived(String fromAddress, byte type, byte[] payload) {
        if (!isMatchingDevice(fromAddress)) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            ensureOnlineStateAndInputVisible();
            processReceivedPacket(type, payload);
            PrimeCompanionServer.INSTANCE.broadcastPacket(type, payload);
        });
    }

    private void sendHandshake() {
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPrefs.getString("current_user", "");
            String myName = sharedPrefs.getString("my_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("my_local_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("current_user_name", null);
            if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString(currentUser + "_name", currentUser);
            if (myName == null || myName.isEmpty()) myName = localUsername;
            long now = System.currentTimeMillis();
            String myPresence = isActivityForeground ? "ONLINE" : ("OFFLINE:" + now);
            String handshake = "HANDSHAKE:name=" + myName + ";login=" + currentUser + ";version=" + getAppVersionCode(getApplicationContext()) + ";features=vn;presence=" + myPresence;
            sendPacket(TYPE_TEXT, handshake.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Failed to send handshake", e);
        }
    }

    private void processReceivedPacket(byte type, byte[] payload) {
        if (payload == null) payload = new byte[0];
        try {
            switch (type) {
                case TYPE_TEXT:
                    String textData = new String(payload, StandardCharsets.UTF_8);
                    if (textData.startsWith("HANDSHAKE:")) {
                        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
                        updateInputVisibility(true);

                        String data = textData.substring(10).trim();
                        String remoteName = targetUsername;
                        String remoteLogin = "";
                        String remotePresence = null;

                        if (data.contains("login=") || data.contains("name=") || data.contains("presence=") || data.contains("platform=") || data.contains("device=")) {
                            String[] parts = data.split(";");
                            for (String p : parts) {
                                if (p.startsWith("login=")) remoteLogin = p.substring(6);
                                else if (p.startsWith("name=")) remoteName = p.substring(5);
                                else if (p.startsWith("version=")) {
                                    try { remoteVersionCode = Integer.parseInt(p.substring(8)); } catch (Exception ignored) {}
                                } else if (p.startsWith("features=")) {
                                    remoteHasVideoNotesSupport = p.contains("vn");
                                } else if (p.startsWith("presence=")) {
                                    remotePresence = p.substring(9);
                                } else if (p.equalsIgnoreCase("platform=desktop") || p.equalsIgnoreCase("device=pc")) {
                                    AvatarManager.saveContactPreferenceSafely(ChatPersonActivity.this, targetUsername, "is_desktop", "true");
                                    if (deviceAddress != null) AvatarManager.saveContactPreferenceSafely(ChatPersonActivity.this, deviceAddress, "is_desktop", "true");
                                }
                            }
                        } else {
                            remoteName = data;
                        }

                        if ("ONLINE".equalsIgnoreCase(remotePresence)) {
                            isRemoteUserOnline = true;
                            stopAnimatingStatus();
                            setStatusWithAnimation("В сети", R.color.prime_success);
                            saveLastMessageToChatList(null, null, false, "ONLINE");
                        } else if (remotePresence != null && remotePresence.startsWith("OFFLINE:")) {
                            isRemoteUserOnline = false;
                            stopAnimatingStatus();
                            try {
                                long ts = Long.parseLong(remotePresence.substring(8));
                                saveCustomLastSeenTimestamp(ts);
                                updateOfflineLastSeenStatus(ts);
                            } catch (Exception e) {
                                updateOfflineLastSeenStatus();
                            }
                            saveLastMessageToChatList(null, null, false, "OFFLINE");
                        } else {
                            if (isRemoteUserOnline) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("В сети", R.color.prime_success);
                            } else {
                                stopAnimatingStatus();
                                updateOfflineLastSeenStatus();
                            }
                        }

                        if (remoteName != null && !remoteName.trim().isEmpty() && !remoteName.equals("1")) {
                            String oldName = this.targetUsername;
                            this.targetUsername = remoteName;
                            BluetoothConnectionManager.getInstance().setRemoteUsername(deviceAddress, remoteName);
                            if (tvChatName != null) tvChatName.setText(formatDisplayName(remoteName));

                            SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sp.edit();
                            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                AvatarManager.saveContactPreferenceSafely(this, deviceAddress, "contact_name", remoteName);
                            }
                            if (oldName != null && !oldName.isEmpty() && !isLocalUserKey(oldName)) {
                                AvatarManager.saveContactPreferenceSafely(this, oldName, "contact_name", remoteName);
                            }
                            if (remoteLogin != null && !remoteLogin.isEmpty() && !isLocalUserKey(remoteLogin)) {
                                AvatarManager.saveContactPreferenceSafely(this, remoteLogin, "contact_name", remoteName);
                            }
                            ed.apply();

                            if (oldName != null && !oldName.equalsIgnoreCase(remoteName) && !isValidMacAddress(oldName)) {
                                migrateHistoryIfNeeded(oldName, remoteName);
                            }

                            updatePersistedChatUser(deviceAddress, oldName, remoteName, remoteLogin);
                            ChatListNotifier.INSTANCE.notifyChanged();
                            sendBroadcast(new Intent("com.messenger.prime.NAME_CHANGED").setPackage(getPackageName()));
                        }
                        handler.sendEmptyMessage(HANDSHAKE_SUCCESS);

                        // Двусторонний ответ на Handshake
                        SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                        String currentUser = sharedPrefs.getString("current_user", "");
                        String myName = sharedPrefs.getString("my_name", null);
                        if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("my_local_name", null);
                        if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("current_user_name", null);
                        if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString(currentUser + "_name", currentUser);
                        if (myName == null || myName.isEmpty()) myName = localUsername;
                        long now = System.currentTimeMillis();
                        String myPresence = isActivityForeground ? "ONLINE" : ("OFFLINE:" + now);
                        sendPacket(TYPE_TEXT, ("HANDSHAKE_ACK:name=" + myName + ";presence=" + myPresence).getBytes(StandardCharsets.UTF_8));

                        sendLocalAvatar(true);
                    } else if (textData.startsWith("HANDSHAKE_ACK:")) {
                        connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
                        updateInputVisibility(true);

                        String data = textData.substring(14).trim();
                        String remoteName = targetUsername;
                        String remotePresence = null;

                        if (data.contains("name=") || data.contains("presence=")) {
                            for (String p : data.split(";")) {
                                if (p.startsWith("name=")) remoteName = p.substring(5);
                                else if (p.startsWith("presence=")) remotePresence = p.substring(9);
                            }
                        } else if (!data.isEmpty()) {
                            remoteName = data;
                        }

                        if ("ONLINE".equalsIgnoreCase(remotePresence)) {
                            isRemoteUserOnline = true;
                            stopAnimatingStatus();
                            setStatusWithAnimation("В сети", R.color.prime_success);
                            saveLastMessageToChatList(null, null, false, "ONLINE");
                        } else if (remotePresence != null && remotePresence.startsWith("OFFLINE:")) {
                            isRemoteUserOnline = false;
                            stopAnimatingStatus();
                            try {
                                long ts = Long.parseLong(remotePresence.substring(8));
                                saveCustomLastSeenTimestamp(ts);
                                updateOfflineLastSeenStatus(ts);
                            } catch (Exception e) {
                                updateOfflineLastSeenStatus();
                            }
                            saveLastMessageToChatList(null, null, false, "OFFLINE");
                        } else {
                            if (isRemoteUserOnline) {
                                stopAnimatingStatus();
                                setStatusWithAnimation("В сети", R.color.prime_success);
                            } else {
                                stopAnimatingStatus();
                                updateOfflineLastSeenStatus();
                            }
                        }

                        if (remoteName != null && !remoteName.trim().isEmpty() && !remoteName.equals("1")) {
                            String oldName = this.targetUsername;
                            this.targetUsername = remoteName;
                            BluetoothConnectionManager.getInstance().setRemoteUsername(deviceAddress, remoteName);
                            if (tvChatName != null) tvChatName.setText(formatDisplayName(remoteName));

                            SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sp.edit();
                            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                ed.putString("contact_name_" + deviceAddress, remoteName);
                                ed.putString(deviceAddress + "_name", remoteName);
                            }
                            if (oldName != null && !oldName.isEmpty() && !isLocalUserKey(oldName)) {
                                ed.putString("contact_name_" + oldName, remoteName);
                                ed.putString(oldName + "_name", remoteName);
                            }
                            ed.apply();

                            if (oldName != null && !oldName.equalsIgnoreCase(remoteName) && !isValidMacAddress(oldName)) {
                                migrateHistoryIfNeeded(oldName, remoteName);
                            }

                            updatePersistedChatUser(deviceAddress, oldName, remoteName, null);
                            ChatListNotifier.INSTANCE.notifyChanged();
                            sendBroadcast(new Intent("com.messenger.prime.NAME_CHANGED").setPackage(getPackageName()));
                        }
                        handler.sendEmptyMessage(HANDSHAKE_SUCCESS);
                        sendLocalAvatar(false);
                    } else {
                        String textMsgId = extractMsgId(textData);
                        if (textMsgId != null && !textMsgId.isEmpty()) {
                        if (isActivityForeground) {
                            sendPacket(TYPE_READ_RECEIPT, textMsgId.getBytes(StandardCharsets.UTF_8));
                        } else {
                            pendingReadReceiptMsgId = textMsgId;
                            sendPacket(TYPE_ACK, new byte[0]);
                        }
                        }
                        if (isDuplicatePacket(textMsgId)) break;
                        handler.obtainMessage(MESSAGE_READ, payload.length, -1, payload).sendToTarget();

                        if (!isActivityForeground) {
                            ParsedMessagePayload parsed = ParsedMessagePayload.parse(textData);
                            PrimeBluetoothService.showMessageNotification(getApplicationContext(), targetUsername, parsed.realText, deviceAddress);
                        }
                    }
                    break;

                case TYPE_PHOTO:
                    if (payload.length > 0) {
                        String photoMsgId = extractMsgId(new String(payload, 0, Math.min(payload.length, 300), StandardCharsets.UTF_8));
                        if (photoMsgId != null && !photoMsgId.isEmpty()) {
                        if (isActivityForeground) {
                            sendPacket(TYPE_READ_RECEIPT, photoMsgId.getBytes(StandardCharsets.UTF_8));
                        } else {
                            pendingReadReceiptMsgId = photoMsgId;
                            sendPacket(TYPE_ACK, new byte[0]);
                        }
                        }
                        if (isDuplicatePacket(photoMsgId)) break;
                        handler.obtainMessage(MESSAGE_READ_PHOTO, payload.length, -1, payload).sendToTarget();

                        if (!isActivityForeground) {
                            PrimeBluetoothService.showMessageNotification(getApplicationContext(), targetUsername, "Фотография", deviceAddress);
                        }
                    }
                    break;

                case TYPE_FILE:
                    if (payload.length > 0) {
                        String fileMetaStr = new String(payload, 0, Math.min(payload.length, 500), StandardCharsets.UTF_8);
                        String fileMsgId = extractMsgId(fileMetaStr);
                        if (fileMsgId != null && !fileMsgId.isEmpty()) {
                        if (isActivityForeground) {
                            sendPacket(TYPE_READ_RECEIPT, fileMsgId.getBytes(StandardCharsets.UTF_8));
                        } else {
                            pendingReadReceiptMsgId = fileMsgId;
                            sendPacket(TYPE_ACK, new byte[0]);
                        }
                        }
                        if (isDuplicatePacket(fileMsgId)) break;
                        handler.obtainMessage(MESSAGE_READ_FILE, payload.length, -1, payload).sendToTarget();

                        if (!isActivityForeground) {
                            PrimeBluetoothService.showMessageNotification(getApplicationContext(), targetUsername, "Файл", deviceAddress);
                        }
                    }
                    break;

                case TYPE_TYPING:
                    String stateData = (payload.length > 0) ? new String(payload, StandardCharsets.UTF_8) : "STATE:TYPING";
                    handler.obtainMessage(MESSAGE_TYPING, -1, -1, stateData).sendToTarget();
                    break;

                case TYPE_EDIT:
                    String editData = new String(payload, StandardCharsets.UTF_8);
                    handler.obtainMessage(MESSAGE_EDIT_RECEIVED, -1, -1, editData).sendToTarget();
                    break;

                case TYPE_AVATAR:
                    final byte[] avatarPayload = payload;
                    runOnIoThread(() -> {
                        try {
                            if (avatarPayload == null || avatarPayload.length == 0) {
                                AvatarManager.clearContactAvatar(this, targetUsername, targetUsername, deviceAddress);
                                remoteAvatarUri = null;
                                updatePersistedChatAvatar(targetUsername, null);
                                sendBroadcast(new Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(getPackageName()));
                                runOnUiThread(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        updateAvatarUi(null, targetUsername);
                                        saveLastMessageToChatList(null, null, false, "ONLINE");
                                    }
                                    ChatListNotifier.INSTANCE.notifyChanged();
                                });
                            } else {
                                boolean isGif = avatarPayload.length > 3 && avatarPayload[0] == (byte)'G' && avatarPayload[1] == (byte)'I' && avatarPayload[2] == (byte)'F';
                                String ext = isGif ? ".gif" : ".jpg";
                                File avatarFile = ChatHistoryManager.saveBytesToAtomicFile(this, "rec_avatar_" + targetUsername + ext, avatarPayload);
                                if (avatarFile == null) return;
                                String avatarUri = Uri.fromFile(avatarFile).toString();
                                if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                    AvatarManager.saveContactPreferenceSafely(this, deviceAddress, "contact_avatar", avatarUri);
                                    ChatHistoryManager.saveBytesToAtomicFile(this, "rec_avatar_" + deviceAddress + ext, avatarPayload);
                                    AvatarHistoryManager.INSTANCE.addContactAvatar(this, deviceAddress, avatarUri);
                                }
                                if (targetUsername != null && !targetUsername.isEmpty() && !isLocalUserKey(targetUsername)) {
                                    AvatarManager.saveContactPreferenceSafely(this, targetUsername, "contact_avatar", avatarUri);
                                    AvatarHistoryManager.INSTANCE.addContactAvatar(this, targetUsername, avatarUri);
                                }
                                remoteAvatarUri = avatarUri;
                                updatePersistedChatAvatar(targetUsername, avatarUri);
                                sendBroadcast(new Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(getPackageName()));

                                runOnUiThread(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        updateAvatarUi(avatarUri, targetUsername);
                                        saveLastMessageToChatList(null, null, false, "ONLINE");
                                    }
                                    ChatListNotifier.INSTANCE.notifyChanged();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save received avatar", e);
                        }
                    });
                    break;

                case TYPE_CHAT_DELETED:
                    String deletedByName = (payload.length > 0) ? new String(payload, StandardCharsets.UTF_8) : targetUsername;
                    isChatDeleted = true;
                    ChatHistoryManager.deleteHistoryCompletely(getApplicationContext(), targetUsername, deviceAddress);
                    try { BluetoothConnectionManager.getInstance().disconnect(resolveTargetAddress()); } catch (Exception ignored) {}
                    runOnUiThread(() -> {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Собеседник удалил этот чат", null);
                        finish();
                    });
                    break;

                case TYPE_DISCONNECT:
                    handler.sendEmptyMessage(MESSAGE_DISCONNECTED);
                    break;

                case TYPE_READ_RECEIPT:
                    handler.obtainMessage(MESSAGE_READ_RECEIPT, payload.length, -1, payload).sendToTarget();
                    break;

                case TYPE_DELETE_MSG:
                    String deletedMsgId = new String(payload, StandardCharsets.UTF_8);
                    handler.obtainMessage(MESSAGE_DELETE_SINGLE, -1, -1, deletedMsgId).sendToTarget();
                    break;

                case TYPE_PRESENCE:
                    String presenceData = new String(payload, StandardCharsets.UTF_8);
                    handler.obtainMessage(MESSAGE_PRESENCE_UPDATED, -1, -1, presenceData).sendToTarget();
                    break;

                case TYPE_REACTION:
                    String rxData = new String(payload, StandardCharsets.UTF_8);
                    handler.obtainMessage(MESSAGE_REACTION_RECEIVED, -1, -1, rxData).sendToTarget();
                    break;

                case TYPE_PROFILE_UPDATE:
                    String profileData = new String(payload, StandardCharsets.UTF_8);
                    if (profileData.startsWith("HANDSHAKE:")) {
                        String data = profileData.substring(10).trim();
                        String remoteName = targetUsername;
                        String remoteLogin = "";
                        if (data.contains("login=") || data.contains("name=")) {
                            for (String p : data.split(";")) {
                                if (p.startsWith("name=")) remoteName = p.substring(5);
                                else if (p.startsWith("login=")) remoteLogin = p.substring(6);
                            }
                        } else {
                            remoteName = data;
                        }
                        if (remoteName != null && !remoteName.trim().isEmpty() && !remoteName.equals("1")) {
                            String oldName = targetUsername;
                            targetUsername = remoteName;
                            if (tvChatName != null) tvChatName.setText(formatDisplayName(remoteName));

                            SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sp.edit();
                            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                ed.putString("contact_name_" + deviceAddress, remoteName);
                                ed.putString(deviceAddress + "_name", remoteName);
                            }
                            if (oldName != null && !oldName.isEmpty()) {
                                ed.putString("contact_name_" + oldName, remoteName);
                                ed.putString(oldName + "_name", remoteName);
                            }
                            if (remoteLogin != null && !remoteLogin.isEmpty()) {
                                ed.putString("contact_name_" + remoteLogin, remoteName);
                                ed.putString(remoteLogin + "_name", remoteName);
                            }
                            ed.apply();

                            if (oldName != null && !oldName.equalsIgnoreCase(remoteName) && !isValidMacAddress(oldName)) {
                                migrateHistoryIfNeeded(oldName, remoteName);
                            }

                            updatePersistedChatUser(deviceAddress, oldName, remoteName, remoteLogin);
                            saveLastMessageToChatList(null, null, false, "ONLINE");
                            ChatListNotifier.INSTANCE.notifyChanged();
                            sendBroadcast(new Intent("com.messenger.prime.NAME_CHANGED").setPackage(getPackageName()));
                        }
                    }
                    break;

                case TYPE_PING:
                    sendPacket(TYPE_ACK, new byte[0]);
                    if (isRemoteUserOnline) {
                        saveLastMessageToChatList(null, null, false, "ONLINE");
                    }
                    break;

                case TYPE_ACK:
                    // Delivery receipt acknowledged
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process received packet type: " + type, e);
        }
    }

    private void updateInputVisibility(boolean connected) {
        if (connected) {
            isRemoteUserOnline = true;
        }
        boolean active = connected || isConnectionActive();
        runOnUiThread(() -> {
            if (layoutInput != null) {
                layoutInput.setVisibility(View.VISIBLE);
                layoutInput.setAlpha(1.0f);
                layoutInput.bringToFront();
                
                View bottomContainer = findViewById(R.id.bottomContainer);
                if (bottomContainer != null) {
                    bottomContainer.setVisibility(View.VISIBLE);
                    bottomContainer.setAlpha(1.0f);
                    bottomContainer.bringToFront();
                }

                if (etMessage != null) {
                    etMessage.setEnabled(active);
                    etMessage.setFocusable(active);
                    etMessage.setFocusableInTouchMode(active);
                    if (active) {
                        etMessage.setHint("Сообщение...");
                        etMessage.setAlpha(1.0f);
                    } else {
                        etMessage.setHint("Не в сети");
                        etMessage.setAlpha(0.5f);
                        etMessage.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
                        }
                    }
                }

                if (btnSend != null) {
                    btnSend.setEnabled(active);
                    btnSend.setAlpha(active ? 1.0f : 0.4f);
                }

                if (btnVoiceVideoNote != null) {
                    btnVoiceVideoNote.setEnabled(active);
                    btnVoiceVideoNote.setAlpha(active ? 1.0f : 0.4f);
                }

                if (btnAttach != null) {
                    btnAttach.setEnabled(true);
                    btnAttach.setClickable(true);
                    btnAttach.setAlpha(active ? 1.0f : 0.4f);
                }

                if (!active) {
                    closeAttachmentPanel();
                }

                updateActionButtonsVisibility(false);
            }
        });
    }

    private final Handler connectionCheckHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed() && isActivityForeground) {
                boolean active = isConnectionActive();
                if (btnAttach != null) {
                    btnAttach.setEnabled(true);
                    btnAttach.setClickable(true);
                    btnAttach.setAlpha(active ? 1.0f : 0.4f);
                    if (!active) closeAttachmentPanel();
                }
                if (btnSend != null) {
                    btnSend.setEnabled(active);
                    btnSend.setAlpha(active ? 1.0f : 0.4f);
                }
                if (btnVoiceVideoNote != null) {
                    btnVoiceVideoNote.setEnabled(active);
                    btnVoiceVideoNote.setAlpha(active ? 1.0f : 0.4f);
                }
                if (etMessage != null) {
                    etMessage.setEnabled(active);
                    etMessage.setFocusable(active);
                    etMessage.setFocusableInTouchMode(active);
                    if (active) {
                        if ("Не в сети".equals(String.valueOf(etMessage.getHint()))) {
                            etMessage.setHint("Сообщение...");
                        }
                        etMessage.setAlpha(1.0f);
                    } else {
                        etMessage.setHint("Не в сети");
                        etMessage.setAlpha(0.5f);
                    }
                }
                updateConnectionStateInAdapter();
                connectionCheckHandler.postDelayed(this, 300);
            }
        }
    };

    private void handleDisconnectionUiAndAutoReconnect() {
        if (!isChatDeleted && !isFinishing() && !isDestroyed() && !isManuallyDisconnected && isActivityForeground) {
            autoRetryHandler.removeCallbacks(autoRetryRunnable);
            if (connectionRetryCount >= 4) {
                Log.d(TAG, "Max auto-reconnect attempts reached (4). Stopping idle auto-retry loop.");
                stopAnimatingStatus();
                updateOfflineLastSeenStatus();
                setOnlineStatusIndicator(false);
                updateInputVisibility(false);
                return;
            }
            long backoff = connectionRetryCount <= 1 ? 3000L : (connectionRetryCount == 2 ? 8000L : (connectionRetryCount == 3 ? 15000L : 25000L));
            long jitter = (long) (Math.random() * 800);
            autoRetryHandler.postDelayed(autoRetryRunnable, backoff + jitter);
        }
    }

    private void connectionFailed() {
        if (isChatDeleted) return;
        handleDisconnectionUiAndAutoReconnect();
    }

    private void connectionLost() {
        BluetoothSocketHolder.removeConnection(deviceAddress, targetUsername);
        isRemoteUserOnline = false;

        handleDisconnectionUiAndAutoReconnect();

        try {
            if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
                PrimeBluetoothService.stopService(this);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }
    }

    @Override
    public void finish() {
        super.finish();
        PrimeTransitions.applyCloseTransition(this);
    }

    private void showBackgroundNotification(String title, String message) {
        String addr = (title != null && title.equalsIgnoreCase(targetUsername)) ? deviceAddress : null;
        PrimeNotification.postMessageNotification(this, title, message, addr);
    }

    private Bitmap getAvatarBitmapForNotification(String name, String avatarUri, String devAddress) {
        try {
            Bitmap bmp = null;
            SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String contactAvatar = AvatarManager.getContactAvatarUriOrFile(this, name, name, devAddress);
            if (contactAvatar == null) contactAvatar = avatarUri;

            File[] candidates = new File[] {
                devAddress != null ? new File(getFilesDir(), "rec_avatar_" + devAddress + ".jpg") : null,
                devAddress != null ? new File(getFilesDir(), "rec_avatar_" + devAddress + ".gif") : null,
                new File(getFilesDir(), "rec_avatar_" + name + ".jpg"),
                new File(getFilesDir(), "rec_avatar_" + name + ".gif")
            };
            for (File f : candidates) {
                if (f != null && f.exists() && f.length() > 0) {
                    bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                    if (bmp != null) break;
                }
            }

            if (bmp == null && contactAvatar != null && !contactAvatar.isEmpty()) {
                try {
                    Uri uri = Uri.parse(contactAvatar);
                    if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                        File file = new File(uri.getPath());
                        if (file.exists()) {
                            bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                        }
                    }
                } catch (Exception ignored) {}
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
        if (currentSendingMessageId == null && activeSendingMessageId == null) {
            PrimeNotification.INSTANCE.cancelSendingProgressNotification(this);
        }
        stopAllVideoNotePlayback();
        PrimeMusicManager.INSTANCE.removeListener(this);
        if (companionPacketListener != null) {
            PrimeCompanionServer.removePacketListener(companionPacketListener);
        }
        super.onDestroy();
        // ioExecutor is shared static; let ongoing transfers finish in background
        try {
            getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(profileChangeListener);
        } catch (Exception ignored) {}

        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (connectionCheckHandler != null) connectionCheckHandler.removeCallbacksAndMessages(null);
        if (islandProgressHandler != null) islandProgressHandler.removeCallbacksAndMessages(null);
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

        Object threadObj = BluetoothSocketHolder.getConnectedThreadInstance();
        if (threadObj instanceof ConnectedThread) {
            ((ConnectedThread) threadObj).setUiHandler(null);
        }
        threadObj = BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername);
        if (threadObj instanceof ConnectedThread) {
            ((ConnectedThread) threadObj).setUiHandler(null);
        }

        isActivityForeground = false;
        activeChatPersonAddress = null;
        activeChatPersonName = null;
        BluetoothConnectionManager.getInstance().removeCallback(this);
    }

    // --- Потоки Bluetooth ---

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;
        private boolean isHandshakeDone = false;
        private Handler keepAliveHandler;
        private Runnable keepAliveRunnable;
        private volatile Handler activeUiHandler;
        private String threadRemoteUsername;
        private String threadRemoteAddress;
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
            if (socket != null && socket.getRemoteDevice() != null) {
                try {
                    this.threadRemoteAddress = socket.getRemoteDevice().getAddress();
                    this.threadRemoteUsername = socket.getRemoteDevice().getName();
                } catch (SecurityException ignored) {}
            }
            if (this.threadRemoteUsername == null || this.threadRemoteUsername.isEmpty()) {
                this.threadRemoteUsername = targetUsername;
            }
            if (this.threadRemoteAddress == null || this.threadRemoteAddress.isEmpty()) {
                this.threadRemoteAddress = deviceAddress;
            }
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

        public String getRemoteUsername() {
            return (threadRemoteUsername != null && !threadRemoteUsername.isEmpty()) ? threadRemoteUsername : targetUsername;
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
                String myName = sharedPrefs.getString("my_name", null);
                if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("my_local_name", null);
                if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString("current_user_name", null);
                if (myName == null || myName.isEmpty()) myName = sharedPrefs.getString(currentUser + "_name", currentUser);
                if (myName == null || myName.isEmpty()) myName = localUsername;
                long now = System.currentTimeMillis();
                String myPresence = isActivityForeground ? "ONLINE" : ("OFFLINE:" + now);
                String handshake = "HANDSHAKE:name=" + myName + ";login=" + currentUser + ";version=" + getAppVersionCode(getApplicationContext()) + ";features=vn;presence=" + myPresence;
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
                        try {
                            payload = new byte[length];
                            mmInStream.readFully(payload);
                        } catch (OutOfMemoryError e) {
                            Log.e(TAG, "OOM while allocating payload of size " + length, e);
                            // Пропускаем байты, чтобы не сломать синхронизацию потока
                            int bytesToSkip = length;
                            byte[] skipBuffer = new byte[8192];
                            while (bytesToSkip > 0) {
                                int read = mmInStream.read(skipBuffer, 0, Math.min(bytesToSkip, skipBuffer.length));
                                if (read < 0) throw new IOException("Stream closed during OOM skip");
                                bytesToSkip -= read;
                            }
                            postToUi(MESSAGE_SEND_CANCELLED, -1, -1, null);
                            continue;
                        }
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
                        saveLastMessageToChatList(null, MessageStatus.READ, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
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
                                    } else if (p.startsWith("features=")) {
                                        remoteHasVideoNotesSupport = p.contains("vn");
                                    }
                                }
                            } else {
                                remoteName = data;
                            }
                            
                            if (!remoteName.isEmpty()) {
                                String oldName = getRemoteUsername();
                                this.threadRemoteUsername = remoteName;
                                
                                BluetoothSocketHolder.setActiveTargetUsername(remoteName);
                                BluetoothSocketHolder.registerConnection(this.threadRemoteAddress, remoteName, mmSocket, this);
                                
                                if (oldName != null && !oldName.equalsIgnoreCase(remoteName) && !isValidMacAddress(oldName)) {
                                    migrateHistoryIfNeeded(oldName, remoteName);
                                }
                                
                                SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                                if (this.threadRemoteAddress != null && !this.threadRemoteAddress.isEmpty()) {
                                    sp.edit().putString("contact_name_" + this.threadRemoteAddress, remoteName).apply();
                                }

                                updatePersistedChatUser(this.threadRemoteAddress, oldName, remoteName, remoteLogin);

                                final String finalName = remoteName;

                                if (Objects.equals(oldName, ChatPersonActivity.this.targetUsername) || Objects.equals(this.threadRemoteAddress, ChatPersonActivity.this.deviceAddress) || Objects.equals(remoteName, ChatPersonActivity.this.targetUsername)) {
                                    ChatPersonActivity.this.targetUsername = remoteName;
                                    try {
                                        getIntent().putExtra("EXTRA_CHAT_NAME", remoteName);
                                    } catch (Exception ignored) {}

                                    runOnUiThread(() -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            if (tvChatName != null) tvChatName.setText(formatDisplayName(finalName));
                                            updateAvatarUi(remoteAvatarUri, finalName);
                                            if (chatAdapter != null && rvMessages != null) {
                                                List<ChatMessage> updatedHistory = ChatHistoryManager.loadMessages(ChatPersonActivity.this, finalName);
                                                chatAdapter.setMessages(updatedHistory);
                                                if (chatAdapter.getItemCount() > 0) {
                                                    rvMessages.post(() -> {
                                                        if (chatAdapter != null && chatAdapter.getItemCount() > 0 && rvMessages != null) {
                                                            rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                }

                                postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                            } else {
                                postToUi(HANDSHAKE_SUCCESS, -1, -1, null);
                            }
                            continue;
                        }

                        if (activeUiHandler != null) {
                            String textMsgId = extractMsgId(receivedData);
                            if (textMsgId != null && !textMsgId.isEmpty()) {
                if (isActivityForeground) {
                    sendPacket(TYPE_READ_RECEIPT, textMsgId.getBytes(StandardCharsets.UTF_8));
                } else {
                    pendingReadReceiptMsgId = textMsgId;
                    sendPacket(TYPE_ACK, new byte[0]);
                }
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
                        if (isActivityForeground) {
                            sendPacket(TYPE_READ_RECEIPT, photoMsgId.getBytes(StandardCharsets.UTF_8));
                        } else {
                            pendingReadReceiptMsgId = photoMsgId;
                            sendPacket(TYPE_ACK, new byte[0]);
                        }
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
                        if (isActivityForeground) {
                            sendPacket(TYPE_READ_RECEIPT, fileMsgId.getBytes(StandardCharsets.UTF_8));
                        } else {
                            pendingReadReceiptMsgId = fileMsgId;
                            sendPacket(TYPE_ACK, new byte[0]);
                        }
                        }

                        if (activeUiHandler != null) {
                            postToUi(MESSAGE_READ_FILE, payload.length, -1, payload);
                            String lowerData = fileMetaStr.toLowerCase(Locale.ROOT);
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
                            String remoteLogin = "";
                            if (data.contains("login=") || data.contains("name=")) {
                                String[] parts = data.split(";");
                                for (String p : parts) {
                                    if (p.startsWith("name=")) remoteName = p.substring(5);
                                    else if (p.startsWith("login=")) remoteLogin = p.substring(6);
                                }
                            } else {
                                remoteName = data;
                            }
                            
                            if (remoteName != null && !remoteName.trim().isEmpty() && !remoteName.equals("1") && !Objects.equals(remoteName, targetUsername)) {
                                String oldTarget = targetUsername;
                                targetUsername = remoteName;
                                try {
                                    getIntent().putExtra("EXTRA_CHAT_NAME", remoteName);
                                } catch (Exception ignored) {}
                                
                                BluetoothSocketHolder.setActiveTargetUsername(remoteName);
                                BluetoothSocketHolder.registerConnection(this.threadRemoteAddress, remoteName, mmSocket, this);

                                if (this.threadRemoteAddress != null && !this.threadRemoteAddress.isEmpty()) {
                                    AvatarManager.saveContactPreferenceSafely(getApplicationContext(), this.threadRemoteAddress, "contact_name", remoteName);
                                }
                                if (oldTarget != null && !oldTarget.isEmpty() && !isLocalUserKey(oldTarget)) {
                                    AvatarManager.saveContactPreferenceSafely(getApplicationContext(), oldTarget, "contact_name", remoteName);
                                }
                                if (remoteLogin != null && !remoteLogin.isEmpty() && !isLocalUserKey(remoteLogin)) {
                                    AvatarManager.saveContactPreferenceSafely(getApplicationContext(), remoteLogin, "contact_name", remoteName);
                                }
                                
                                if (oldTarget != null && !oldTarget.equalsIgnoreCase(remoteName) && !isValidMacAddress(oldTarget)) {
                                    migrateHistoryIfNeeded(oldTarget, remoteName);
                                }
                                if (deviceAddress != null) {
                                    migrateHistoryIfNeeded(deviceAddress, remoteName);
                                }
                                
                                updatePersistedChatUser(deviceAddress, oldTarget, remoteName, remoteLogin);

                                final String finalName = remoteName;
                                saveLastMessageToChatList(null, null, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                                ChatListNotifier.INSTANCE.notifyChanged();
                                sendBroadcast(new Intent("com.messenger.prime.NAME_CHANGED").setPackage(getPackageName()));

                                if (activeUiHandler != null) {
                                    activeUiHandler.post(() -> {
                                        if (tvChatName != null) tvChatName.setText(formatDisplayName(finalName));
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
                            String sender = getRemoteUsername();
                            if (payload == null || payload.length == 0) {
                                AvatarManager.clearContactAvatar(getApplicationContext(), sender, sender, deviceAddress);
                                updatePersistedChatAvatar(sender, null);
                                sendBroadcast(new Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(getPackageName()));
                                if (sender != null && sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername)) {
                                    remoteAvatarUri = null;
                                    if (activeUiHandler != null) {
                                        activeUiHandler.post(() -> updateAvatarUi(null, sender));
                                    }
                                }
                            } else {
                                boolean isGif = payload.length > 3 && payload[0] == (byte)'G' && payload[1] == (byte)'I' && payload[2] == (byte)'F';
                                String ext = isGif ? ".gif" : ".jpg";
                                File avatarFile = ChatHistoryManager.saveBytesToAtomicFile(getApplicationContext(), "rec_avatar_" + sender + ext, payload);
                                if (avatarFile == null) return;

                                String newAvatarUri = Uri.fromFile(avatarFile).toString();
                                if (deviceAddress != null && !deviceAddress.isEmpty()) {
                                    AvatarManager.saveContactPreferenceSafely(getApplicationContext(), deviceAddress, "contact_avatar", newAvatarUri);
                                    ChatHistoryManager.saveBytesToAtomicFile(getApplicationContext(), "rec_avatar_" + deviceAddress + ext, payload);
                                    AvatarHistoryManager.INSTANCE.addContactAvatar(getApplicationContext(), deviceAddress, newAvatarUri);
                                }
                                if (sender != null && !sender.isEmpty() && !isLocalUserKey(sender)) {
                                    AvatarManager.saveContactPreferenceSafely(getApplicationContext(), sender, "contact_avatar", newAvatarUri);
                                    AvatarHistoryManager.INSTANCE.addContactAvatar(getApplicationContext(), sender, newAvatarUri);
                                }

                                updatePersistedChatAvatar(sender, newAvatarUri);
                                sendBroadcast(new Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(getPackageName()));

                                if (sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername)) {
                                    remoteAvatarUri = newAvatarUri;
                                    if (activeUiHandler != null) {
                                        activeUiHandler.post(() -> {
                                            updateAvatarUi(newAvatarUri, sender);
                                            saveLastMessageToChatList(null, null, false, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to process background TYPE_AVATAR", e);
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
                        
                        BluetoothSocketHolder.removeConnection(deviceAddress, targetUsername);
                        if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
                            PrimeBluetoothService.stopService(getApplicationContext());
                        }
                        
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

            String sender = getRemoteUsername();

            long timestamp = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            ChatMessage incomingMessage = new ChatMessage(parsed.realText, time, sender, false, null, timestamp, null, parsed.msgId);
            if (parsed.replyToId != null) {
                incomingMessage.setReplyToMessageId(parsed.replyToId);
                incomingMessage.setReplyToSender(parsed.replyToSender);
                incomingMessage.setReplyToText(parsed.replyToText);
            }

            boolean isCurrentChatActive = activeUiHandler != null && sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername);

            if (isCurrentChatActive && activeUiHandler != null) {
                activeUiHandler.post(() -> addMessageToUI(incomingMessage));
            } else {
                ChatHistoryManager.saveMessage(getApplicationContext(), sender, incomingMessage);
                saveLastMessageToChatListForUser(sender, parsed.realText, MessageStatus.NONE, true, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                showBackgroundNotification(sender, parsed.realText);
            }
        }

        private void processBackgroundPhotoMessage(byte[] fullPayload) {
            try {
                if (fullPayload == null || fullPayload.length == 0) return;
                String sender = getRemoteUsername();

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

                                    File mediaFile = ChatHistoryManager.saveBytesToAtomicFile(ChatPersonActivity.this, "rec_media_" + (photoMsgId != null ? photoMsgId : photoTs) + "_" + idx + "." + ext, itemBytes);
                                    if (mediaFile != null) {
                                        recMediaItems.add(new ChatMessage.MediaItem(mediaFile.getAbsolutePath(), isVideo, durStr));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse multi-media item " + idx, e);
                            }
                        }

                        if (!recMediaItems.isEmpty()) {
                            ChatMessage multiMsg = new ChatMessage("", photoTime, sender, false, null, photoTs, null, photoMsgId);
                            multiMsg.setMediaItems(recMediaItems);

                            boolean isCurrentChatActive = activeUiHandler != null && sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername);
                            if (isCurrentChatActive && activeUiHandler != null) {
                                activeUiHandler.post(() -> addMessageToUI(multiMsg));
                            } else {
                                ChatHistoryManager.saveMessage(getApplicationContext(), sender, multiMsg);
                                saveLastMessageToChatListForUser(sender, "Медиафайлы (" + recMediaItems.size() + ")", MessageStatus.NONE, true, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                                showBackgroundNotification(sender, "Медиафайлы (" + recMediaItems.size() + ")");
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
                    File photoFile = ChatHistoryManager.saveBytesToAtomicFile(ChatPersonActivity.this, "rec_photo_" + (photoMsgId != null ? photoMsgId : System.currentTimeMillis()) + ".jpg", photoBytes);
                    if (photoFile != null) {
                        savedPhotoPath = photoFile.getAbsolutePath();
                    }
                }

                Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                long photoTs = System.currentTimeMillis();
                String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                ChatMessage photoMsg = new ChatMessage(captionText, photoTime, sender, false, photoBitmap, photoTs, savedPhotoPath, photoMsgId);
                photoMsg.setMessageType(ChatMessage.MessageType.IMAGE);

                boolean isCurrentChatActive = activeUiHandler != null && sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername);
                if (isCurrentChatActive && activeUiHandler != null) {
                    activeUiHandler.post(() -> addMessageToUI(photoMsg));
                } else {
                    ChatHistoryManager.saveMessage(getApplicationContext(), sender, photoMsg);
                    saveLastMessageToChatListForUser(sender, captionText != null ? captionText : "Фотография", MessageStatus.NONE, true, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                    showBackgroundNotification(sender, captionText != null ? captionText : "Фотография");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process background photo", e);
            }
        }

        private void processBackgroundFileMessage(byte[] fullPayload) {
            if (fullPayload == null) return;
            String sender = getRemoteUsername();
            ChatMessage fileMsg = parseFileMessageBytes(fullPayload, sender);
            if (fileMsg == null) return;

            boolean isCurrentChatActive = activeUiHandler != null && sender.equalsIgnoreCase(ChatPersonActivity.this.targetUsername);
            if (isCurrentChatActive && activeUiHandler != null) {
                activeUiHandler.post(() -> addMessageToUI(fileMsg));
            } else {
                ChatHistoryManager.saveMessage(getApplicationContext(), sender, fileMsg);
                String desc = (fileMsg.isVideo() ? "Видео: " : "Файл: ") + (fileMsg.getFileName() != null ? fileMsg.getFileName() : "Файл");
                saveLastMessageToChatListForUser(sender, desc, MessageStatus.NONE, true, isRemoteUserOnline ? "ONLINE" : "OFFLINE");
                showBackgroundNotification(sender, desc);
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

            String msgId = null;
            if (showProgressUi && payload != null && payload.length > 0) {
                String metaStr = new String(payload, 0, Math.min(payload.length, 300), StandardCharsets.UTF_8);
                msgId = ChatPersonActivity.extractMsgId(metaStr);
            }
            final String finalMsgId = msgId;

            executor.execute(() -> {
                synchronized (writeLock) {
                    try {
                        mmOutStream.writeByte(type);
                        int length = payload != null ? payload.length : 0;
                        mmOutStream.writeInt(length);
                        if (length > 0 && payload != null) {
                            int offset = 0;
                            int chunkSize = isChunkedMedia ? 16384 : 4096;
                            while (offset < length) {
                                if (isMediaSendingCancelled) {
                                    Log.d(TAG, "Media send cancelled by user");
                                    isMediaSendingCancelled = false;
                                    postToUi(MESSAGE_SEND_CANCELLED, -1, -1, finalMsgId);
                                    return;
                                }
                                int bytesToWrite = Math.min(chunkSize, length - offset);
                                mmOutStream.write(payload, offset, bytesToWrite);
                                offset += bytesToWrite;
                                mmOutStream.flush();

                                if (isChunkedMedia) {
                                    if (showProgressUi && length > 1024) {
                                        int progress = (int) ((offset * 100L) / length);
                                        postToUi(MESSAGE_SEND_PROGRESS, progress, -1, finalMsgId);
                                    }
                                }
                            }
                        } else {
                            mmOutStream.flush();
                        }
                        if (showProgressUi && !isMediaSendingCancelled) {
                            postToUi(MESSAGE_SEND_PROGRESS, 100, -1, finalMsgId);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception during write", e);
                        if (showProgressUi) {
                            postToUi(MESSAGE_SEND_PROGRESS, -1, -1, finalMsgId);
                        }
                    }
                }
            });
        }

        public void streamFilePacket(byte type, byte[] header, java.io.File file) {
            if (mmOutStream == null) return;
            boolean isChunkedMedia = (type == TYPE_PHOTO || type == TYPE_FILE || type == TYPE_AVATAR);
            boolean showProgressUi = (type == TYPE_PHOTO || type == TYPE_FILE);
            ExecutorService executor = isChunkedMedia ? mediaWriteExecutor : controlWriteExecutor;

            if (isChunkedMedia) {
                isMediaSendingCancelled = false;
            }

            String msgId = null;
            if (showProgressUi && header != null && header.length > 0) {
                String metaStr = new String(header, 0, Math.min(header.length, 300), StandardCharsets.UTF_8);
                msgId = ChatPersonActivity.extractMsgId(metaStr);
            }
            final String finalMsgId = msgId;

            executor.execute(() -> {
                synchronized (writeLock) {
                    try {
                        mmOutStream.writeByte(type);
                        long fileLength = file != null ? file.length() : 0;
                        int headerLen = header != null ? header.length : 0;
                        long totalLen = fileLength + headerLen;

                        if (totalLen > Integer.MAX_VALUE) {
                             throw new IOException("File too large");
                        }
                        mmOutStream.writeInt((int)totalLen);
                        
                        if (headerLen > 0) {
                            mmOutStream.write(header);
                        }

                        if (fileLength > 0 && file != null) {
                            try (InputStream fis = new java.io.FileInputStream(file)) {
                                int offset = 0;
                                int chunkSize = isChunkedMedia ? 16384 : 4096;
                                byte[] buf = new byte[chunkSize];
                                int read;
                                while ((read = fis.read(buf)) != -1) {
                                    if (isMediaSendingCancelled) {
                                        Log.d(TAG, "Media send cancelled by user");
                                        isMediaSendingCancelled = false;
                                        postToUi(MESSAGE_SEND_CANCELLED, -1, -1, finalMsgId);
                                        return;
                                    }
                                    mmOutStream.write(buf, 0, read);
                                    offset += read;
                                    mmOutStream.flush();

                                    if (isChunkedMedia) {
                                        if (showProgressUi && fileLength > 1024) {
                                            int progress = (int) ((offset * 100L) / fileLength);
                                            postToUi(MESSAGE_SEND_PROGRESS, progress, -1, finalMsgId);
                                        }
                                    }
                                }
                            }
                        } else {
                            mmOutStream.flush();
                        }
                        if (showProgressUi && !isMediaSendingCancelled) {
                            postToUi(MESSAGE_SEND_PROGRESS, 100, -1, finalMsgId);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception during stream write", e);
                        if (showProgressUi) {
                            postToUi(MESSAGE_SEND_PROGRESS, -1, -1, finalMsgId);
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
                try (InputStream stream = is) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384];
                    int len;
                    long totalRead = 0;
                    while ((len = stream.read(buf)) != -1 && totalRead < 100 * 1024 * 1024L) {
                        baos.write(buf, 0, len);
                        totalRead += len;
                    }
                    return baos.toByteArray();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read pending file bytes", e);
        }
        return new byte[0];
    }

    private String extractHeaderTag(String header, String tag) {
        int idx = header.indexOf(tag);
        if (idx == -1) return null;
        int start = idx + tag.length();
        int end = header.indexOf(":::", start);
        if (end == -1) end = header.length();
        return header.substring(start, end).trim();
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

        String replyPart = extractHeaderTag(headerStr, ":::REPLY:::");
        if (replyPart != null) {
            String[] rParts = replyPart.split(":::");
            if (rParts.length >= 1) replyToId = rParts[0];
            if (rParts.length >= 2) replyToSender = rParts[1];
            if (rParts.length >= 3) replyToText = rParts[2];
        }

        videoDuration = extractHeaderTag(headerStr, ":::DURATION:::");
        String isVideoStr = extractHeaderTag(headerStr, ":::IS_VIDEO:::");
        boolean isVideoFlag = "1".equals(isVideoStr);

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
            File localFile = ChatHistoryManager.saveBytesToAtomicFile(this, "rec_file_" + (msgId != null ? msgId : timestamp) + "_" + fileName, fileDataBytes);
            if (localFile != null) {
                localSavedPath = localFile.getAbsolutePath();
            }
        }

        ChatMessage msg = new ChatMessage(text, time, sender, false, null, timestamp, localSavedPath, msgId);

        String msgTypeStr = extractHeaderTag(headerStr, ":::MSG_TYPE:::");
        ChatMessage.MessageType parsedType = null;
        if (msgTypeStr != null) {
            try {
                parsedType = ChatMessage.MessageType.valueOf(msgTypeStr);
            } catch (Exception ignored) {}
        }

        if (parsedType == null) {
            String lowerName = fileName.toLowerCase(Locale.US);
            boolean isVoice = lowerName.contains("voice_") || lowerName.contains("/voices/") || "Голосовое сообщение".equals(text);
            boolean isAudio = !isVoice && (lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") || lowerName.endsWith(".flac") || lowerName.endsWith(".opus") || lowerName.endsWith(".wma"));
            boolean isVid = !isAudio && !isVoice && (isVideoFlag || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi"));
            boolean isImg = !isAudio && !isVid && !isVoice && (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif"));

            if (isVoice) parsedType = ChatMessage.MessageType.VOICE;
            else if (isAudio) parsedType = ChatMessage.MessageType.MUSIC;
            else if (isVid) parsedType = ChatMessage.MessageType.VIDEO;
            else if (isImg) parsedType = ChatMessage.MessageType.IMAGE;
            else parsedType = ChatMessage.MessageType.FILE;
        }

        msg.setMessageType(parsedType);

        String ampsStr = extractHeaderTag(headerStr, ":::AMPS:::");
        if (ampsStr != null && !ampsStr.isEmpty()) {
            String[] ampParts = ampsStr.split(",");
            java.util.List<Integer> parsedAmps = new java.util.ArrayList<>();
            for (String p : ampParts) {
                try {
                    parsedAmps.add(Integer.parseInt(p.trim()));
                } catch (Exception ignored) {}
            }
            msg.setWaveformChunks(parsedAmps);
        }
        if (parsedType == ChatMessage.MessageType.VIDEO) {
            msg.setVideoDuration(videoDuration != null ? videoDuration : "00:00");
            if (localSavedPath != null) {
                Bitmap thumb = getVideoThumbnail(localSavedPath);
                if (thumb != null) msg.setImageBitmap(thumb);
            }
        } else if (parsedType == ChatMessage.MessageType.MUSIC) {
            msg.setAudioDuration(videoDuration != null ? videoDuration : "00:00");
            if (fileName.contains(" - ")) {
                String[] mParts = fileName.split(" - ", 2);
                msg.setArtistName(mParts[0].trim());
                msg.setTrackTitle(mParts[1].trim());
            } else {
                msg.setTrackTitle(fileName);
            }
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

        boolean isVideoNoteOrVoice = (pending.messageType == ChatMessage.MessageType.VOICE) || (text != null && text.startsWith("VIDEO_NOTE:"));
        if (isVideoNoteOrVoice && isRecipientVersionBelow64()) {
            sendUpdateTextToRecipient();
            return;
        }

        runOnUiThread(() -> {
            senderTypingHandler.removeCallbacks(stopSenderTypingRunnable);
            sendActivityState("IDLE");
            showSendingProgressUi(0);
        });

        runOnIoThread(() -> {
            try {
                long timestamp = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                String messageId = localUsername + "_" + timestamp + "_" + UUID.randomUUID().toString();

                String mimeType = null;
                try {
                    if (pending.uri != null) {
                        mimeType = getContentResolver().getType(pending.uri);
                    }
                } catch (Exception ignored) {}

                ChatMessage.MessageType type;
                if (pending.messageType != null) {
                    type = pending.messageType;
                } else {
                    boolean isAudio = isAudioMimeOrPath(pending.uri, pending.path);
                    boolean isVideo = pending.isVideo || (!isAudio && ((mimeType != null && mimeType.startsWith("video/")) || isVideoMimeOrPath(pending.uri, pending.path)));
                    type = isAudio ? ChatMessage.MessageType.MUSIC : (isVideo ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.FILE);
                }

                boolean isVideo = (type == ChatMessage.MessageType.VIDEO);
                boolean isMusic = (type == ChatMessage.MessageType.MUSIC);
                boolean isVidNote = (text != null && text.startsWith("VIDEO_NOTE:"));

                String fileName = pending.name != null ? pending.name : (isMusic ? "audio.mp3" : (isVideo ? (isVidNote ? "video_note.mp4" : "video.mp4") : "file.bin"));
                if (isVideo && !fileName.toLowerCase(Locale.US).matches(".*\\.(mp4|mkv|3gp|webm|mov|avi)$")) {
                    fileName = fileName + ".mp4";
                }
                long fileSize = pending.size;
                String durStr = pending.durationStr != null ? pending.durationStr : "00:00";

                String localSavedPath = pending.path;
                if ((isVideo || isMusic) && pending.uri != null) {
                    if (localSavedPath == null || localSavedPath.startsWith("content://") || !new File(localSavedPath).exists()) {
                        try {
                            String prefix = isMusic ? "sent_music_" : "sent_video_";
                            File mediaFile = new File(getFilesDir(), prefix + timestamp + "_" + fileName);
                            try (InputStream is = getContentResolver().openInputStream(pending.uri)) {
                                if (is != null) {
                                    try (FileOutputStream fos = new FileOutputStream(mediaFile)) {
                                        byte[] buf = new byte[8192];
                                        int len;
                                        while ((len = is.read(buf)) > 0) {
                                            fos.write(buf, 0, len);
                                        }
                                        fos.flush();
                                    }
                                    localSavedPath = mediaFile.getAbsolutePath();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save sent media locally", e);
                        }
                    }
                }
                
                long fileSizeBytes = fileSize > 0 ? fileSize : 0;
                if (localSavedPath != null && new File(localSavedPath).exists()) {
                     fileSizeBytes = new File(localSavedPath).length();
                }

                ChatMessage msg = new ChatMessage(text, time, localUsername, true, pending.thumbnail, timestamp, localSavedPath, messageId);
                msg.setMessageType(type);
                msg.setFileName(fileName);
                msg.setFileSize(fileSizeBytes);
                if (isVideo) msg.setVideoDuration(durStr);
                if (isMusic || type == ChatMessage.MessageType.VOICE) {
                    msg.setAudioDuration(durStr);
                    if (fileName.contains(" - ")) {
                        String[] mParts = fileName.split(" - ", 2);
                        msg.setArtistName(mParts[0].trim());
                        msg.setTrackTitle(mParts[1].trim());
                    } else {
                        msg.setTrackTitle(fileName);
                    }
                }
                msg.setMessageStatus(MessageStatus.SENDING);
                msg.setSendingProgress(0);

                boolean hasReply = (replyingToMessage != null);
                if (hasReply) {
                    msg.setReplyToMessageId(replyingToMessage.getMessageId());
                    msg.setReplyToSender(replyingToMessage.getSenderLogin());
                    String qText = replyingToText != null && !replyingToText.isEmpty() ? replyingToText : replyingToMessage.getText();
                    if (replyingToMessage.isVideoNote() || (qText != null && (qText.startsWith("VIDEO_NOTE:") || qText.startsWith("vn_")))) {
                        qText = "📹 Видеосообщение";
                    }
                    msg.setReplyToText(qText != null && !qText.isEmpty() ? qText : (isMusic ? "Аудиозапись" : (isVideo ? "Видео" : "Файл")));
                }

                if (type == ChatMessage.MessageType.VOICE && pending.waveformChunks != null) {
                    msg.setWaveformChunks(pending.waveformChunks);
                }

                String header = messageId + ":::" + fileName + ":::" + msg.getFileSize() + ":::" + (text != null ? text : "") + ":::DURATION:::" + durStr + ":::IS_VIDEO:::" + (isVideo ? "1" : "0") + ":::MSG_TYPE:::" + type.name();
                if (msg.getWaveformChunks() != null && !msg.getWaveformChunks().isEmpty()) {
                    StringBuilder sbAmps = new StringBuilder();
                    for (int i = 0; i < msg.getWaveformChunks().size(); i++) {
                        if (i > 0) sbAmps.append(",");
                        sbAmps.append(msg.getWaveformChunks().get(i));
                    }
                    header += ":::AMPS:::" + sbAmps.toString();
                }
                if (msg.isReply()) {
                    header += ":::REPLY:::" + msg.getReplyToMessageId() + ":::" + msg.getReplyToSender() + ":::" + msg.getReplyToText();
                }
                header += ":::HEADER_END:::";

                byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                runOnUiThread(() -> {
                    if (hasReply) cancelReplyMode();
                    showSendingProgressUi(0);
                    addMessageToUI(msg);
                    ChatHistoryManager.saveMessage(ChatPersonActivity.this, targetUsername, msg);
                    String summaryStr = ChatMessage.getSummaryDescription(msg);
                    saveLastMessageToChatList(summaryStr, MessageStatus.SENDING, false, "ONLINE");
                    String pendingState = isVideo ? "SENDING_MEDIA" : "SENDING_FILE";
                    saveActivityStateToChatList(targetUsername, pendingState);
                    sendActivityState(pendingState);
                    clearPendingAttachment();
                    latch.countDown();
                });

                try {
                    latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                currentSendingMessageId = messageId;
                activeSendingMessageId = messageId;
                activeSendingTargetUsername = targetUsername;

                if (isConnectionActive()) {
                    if (localSavedPath != null && new File(localSavedPath).exists()) {
                         String targetAddr = resolveTargetAddress();
                         boolean sentViaSocketHolder = BluetoothSocketHolder.streamFilePacket(targetAddr, targetUsername, TYPE_FILE, headerBytes, new File(localSavedPath));
                         if (!sentViaSocketHolder) {
                             BluetoothConnectionManager.getInstance().streamFilePacket(targetUsername, TYPE_FILE, headerBytes, new File(localSavedPath));
                         }
                    } else {
                         // Fallback if not saved locally
                         byte[] fileBytes = getBytesFromPending(pending);
                         byte[] fullPayload = new byte[headerBytes.length + fileBytes.length];
                         System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
                         if (fileBytes.length > 0) {
                             System.arraycopy(fileBytes, 0, fullPayload, headerBytes.length, fileBytes.length);
                         }
                         sendPacket(TYPE_FILE, fullPayload);
                    }
                } else {
                    if (localSavedPath != null && new File(localSavedPath).exists()) {
                         // We can't queue stream easily in pendingMessageQueue, so we fallback to full bytes
                         byte[] fileBytes = getBytesFromPending(pending);
                         byte[] fullPayload = new byte[headerBytes.length + fileBytes.length];
                         System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
                         if (fileBytes.length > 0) {
                             System.arraycopy(fileBytes, 0, fullPayload, headerBytes.length, fileBytes.length);
                         }
                         pendingMessageQueue.add(new PendingMessage(TYPE_FILE, fullPayload));
                    } else {
                         byte[] fileBytes = getBytesFromPending(pending);
                         byte[] fullPayload = new byte[headerBytes.length + fileBytes.length];
                         System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
                         if (fileBytes.length > 0) {
                             System.arraycopy(fileBytes, 0, fullPayload, headerBytes.length, fileBytes.length);
                         }
                         pendingMessageQueue.add(new PendingMessage(TYPE_FILE, fullPayload));
                    }
                    runOnUiThread(this::startPrimeConnection);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Error in sendVideoOrFile", e);
                runOnUiThread(() -> {
                    hideSendingProgressUi();
                    PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Ошибка отправки файла", null);
                });
            }
        });
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
        ChatMessage.MessageType messageType;
        Bitmap thumbnail;
        String durationStr;
        java.util.List<Integer> waveformChunks;

        PendingAttachmentItem(Uri uri, String path, String name, long size, boolean isVideo, boolean isFile, ChatMessage.MessageType messageType, Bitmap thumbnail, String durationStr) {
            this.uri = uri;
            this.path = path;
            this.name = name;
            this.size = size;
            this.messageType = messageType != null ? messageType : (isVideo ? ChatMessage.MessageType.VIDEO : (isFile ? ChatMessage.MessageType.FILE : ChatMessage.MessageType.IMAGE));
            this.isVideo = (this.messageType == ChatMessage.MessageType.VIDEO);
            this.isFile = (this.messageType == ChatMessage.MessageType.FILE || this.messageType == ChatMessage.MessageType.MUSIC || this.messageType == ChatMessage.MessageType.VOICE);
            this.thumbnail = thumbnail;
            this.durationStr = durationStr;
        }

        String getFormatLabel() {
            if (messageType == ChatMessage.MessageType.MUSIC) return "MP3";
            if (messageType == ChatMessage.MessageType.VOICE) return "VOICE";
            if (messageType == ChatMessage.MessageType.VIDEO) return "MP4";
            if (messageType == ChatMessage.MessageType.IMAGE) return "IMG";
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

            if (item.messageType == ChatMessage.MessageType.MUSIC) {
                holder.ivVideoBadge.setVisibility(View.GONE);
                holder.ivThumbnail.setImageResource(R.drawable.ic_music);
            } else if (item.messageType == ChatMessage.MessageType.VIDEO) {
                holder.ivVideoBadge.setVisibility(View.VISIBLE);
                if (item.thumbnail != null) {
                    holder.ivThumbnail.setImageBitmap(item.thumbnail);
                } else {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_video);
                }
            } else if (item.messageType == ChatMessage.MessageType.FILE) {
                holder.ivVideoBadge.setVisibility(View.GONE);
                holder.ivThumbnail.setImageResource(R.drawable.ic_file);
            } else { // Photo
                holder.ivVideoBadge.setVisibility(View.GONE);
                if (item.thumbnail != null) {
                    holder.ivThumbnail.setImageBitmap(item.thumbnail);
                } else if (item.uri != null) {
                    Glide.with(holder.itemView.getContext()).load(item.uri).into(holder.ivThumbnail);
                } else {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_photo);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < items.size()) {
                    PendingAttachmentItem clickedItem = items.get(pos);
                    if (clickedItem.messageType == ChatMessage.MessageType.IMAGE) {
                        Intent intent = new Intent(ChatPersonActivity.this, PhotoEditorActivity.class);
                        String uriStr = clickedItem.uri != null ? clickedItem.uri.toString() : clickedItem.path;
                        Uri itemUri = Uri.parse(uriStr);
                        intent.setDataAndType(itemUri, "image/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.putExtra("EXTRA_IMAGE_URI", uriStr);
                        isOpeningSubActivity = true;
                        editPhotoLauncher.launch(intent);
                    }
                }
            });

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

    private View btnPickGifFromGallery;
    private EditText etGifSearch;
    private ProgressBar pbGifLoading;
    private TextView tvGifEmpty;
    private RecyclerView rvTenorGifs;
    private TenorGifAdapter tenorAdapter;

    public static class TenorGifItem {
        public String id;
        public String previewUrl;
        public String fullUrl;
    }

    private class TenorGifAdapter extends RecyclerView.Adapter<TenorGifAdapter.GifViewHolder> {
        private List<TenorGifItem> items = new ArrayList<>();

        public void setItems(List<TenorGifItem> newItems) {
            this.items = newItems != null ? newItems : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public GifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenor_gif, parent, false);
            return new GifViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GifViewHolder holder, int position) {
            if (position < 0 || position >= items.size()) return;
            TenorGifItem item = items.get(position);

            Glide.with(holder.itemView.getContext())
                    .load(item.previewUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_prime_gif)
                    .error(R.drawable.ic_prime_gif)
                    .into(holder.ivThumbnail);

            holder.itemView.setOnClickListener(v -> sendGifFromUrl(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class GifViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;

            GifViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.ivGifThumbnail);
            }
        }
    }

    private void fetchTenorGifs(String query) {
        if (pbGifLoading != null) pbGifLoading.setVisibility(View.VISIBLE);
        if (tvGifEmpty != null) tvGifEmpty.setVisibility(View.GONE);
        runOnIoThread(() -> {
            List<TenorGifItem> gifList = new ArrayList<>();
            try {
                // 1. Google Tenor v2 Official API
                String tenorKey = "LIVDSRZULELA";
                String urlStr;
                if (query == null || query.trim().isEmpty()) {
                    urlStr = "https://tenor.googleapis.com/v2/featured?key=" + tenorKey + "&limit=30&media_filter=minimal";
                } else {
                    String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
                    urlStr = "https://tenor.googleapis.com/v2/search?key=" + tenorKey + "&q=" + encodedQuery + "&limit=30&media_filter=minimal";
                }

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    is.close();

                    String jsonStr = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    JSONObject root = new JSONObject(jsonStr);
                    JSONArray results = root.optJSONArray("results");
                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject resObj = results.getJSONObject(i);
                            JSONObject mediaFormats = resObj.optJSONObject("media_formats");
                            if (mediaFormats != null) {
                                String previewUrl = null;
                                String fullUrl = null;

                                JSONObject tinyObj = mediaFormats.optJSONObject("tinygif");
                                if (tinyObj == null) tinyObj = mediaFormats.optJSONObject("nanogif");
                                if (tinyObj != null) previewUrl = tinyObj.optString("url");

                                JSONObject gifObj = mediaFormats.optJSONObject("gif");
                                if (gifObj == null) gifObj = mediaFormats.optJSONObject("mediumgif");
                                if (gifObj != null) fullUrl = gifObj.optString("url");

                                if (fullUrl == null || fullUrl.isEmpty()) fullUrl = previewUrl;
                                if (previewUrl == null || previewUrl.isEmpty()) previewUrl = fullUrl;

                                if (previewUrl != null && !previewUrl.isEmpty()) {
                                    TenorGifItem item = new TenorGifItem();
                                    item.id = resObj.optString("id");
                                    item.previewUrl = previewUrl;
                                    item.fullUrl = fullUrl;
                                    gifList.add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Tenor v2 GIF search failed, trying Giphy fallback...", e);
            }

            // Fallback 1: Giphy API
            if (gifList.isEmpty()) {
                try {
                    String giphyKey = "pL1R3p6mThB4O3a4pS05o13xWw1aG6u1";
                    String urlStr = (query == null || query.trim().isEmpty())
                            ? "https://api.giphy.com/v1/gifs/trending?api_key=" + giphyKey + "&limit=25"
                            : "https://api.giphy.com/v1/gifs/search?api_key=" + giphyKey + "&q=" + URLEncoder.encode(query.trim(), "UTF-8") + "&limit=25";

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    if (conn.getResponseCode() == 200) {
                        InputStream is = conn.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, len);
                        }
                        is.close();

                        JSONObject root = new JSONObject(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                        JSONArray results = root.optJSONArray("data");
                        if (results != null) {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject resObj = results.getJSONObject(i);
                                JSONObject imagesObj = resObj.optJSONObject("images");
                                if (imagesObj != null) {
                                    JSONObject fixedObj = imagesObj.optJSONObject("fixed_height");
                                    JSONObject origObj = imagesObj.optJSONObject("original");
                                    String previewUrl = fixedObj != null ? fixedObj.optString("url") : null;
                                    String fullUrl = origObj != null ? origObj.optString("url") : previewUrl;

                                    if (previewUrl != null && !previewUrl.isEmpty()) {
                                        TenorGifItem item = new TenorGifItem();
                                        item.id = resObj.optString("id");
                                        item.previewUrl = previewUrl;
                                        item.fullUrl = fullUrl != null ? fullUrl : previewUrl;
                                        gifList.add(item);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "Giphy fallback failed", e2);
                }
            }

            // Fallback 2: Scan device for local GIFs
            if (gifList.isEmpty()) {
                gifList.addAll(scanDeviceForGifs());
            }

            runOnUiThread(() -> {
                if (pbGifLoading != null) pbGifLoading.setVisibility(View.GONE);
                if (tvGifEmpty != null) tvGifEmpty.setVisibility(gifList.isEmpty() ? View.VISIBLE : View.GONE);
                if (tenorAdapter != null) {
                    tenorAdapter.setItems(gifList);
                }
            });
        });
    }

    private List<TenorGifItem> scanDeviceForGifs() {
        List<TenorGifItem> list = new ArrayList<>();
        try {
            Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME
            };
            String selection = MediaStore.Images.Media.MIME_TYPE + "=? OR " + MediaStore.Images.Media.DATA + " LIKE '%.gif'";
            String[] selectionArgs = new String[] { "image/gif" };
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                    int count = 0;
                    while (cursor.moveToNext() && count < 50) {
                        long id = cursor.getLong(idCol);
                        String path = dataCol != -1 ? cursor.getString(dataCol) : null;
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        String gifUriStr = (path != null && new File(path).exists()) ? Uri.fromFile(new File(path)).toString() : contentUri.toString();

                        TenorGifItem item = new TenorGifItem();
                        item.id = String.valueOf(id);
                        item.previewUrl = gifUriStr;
                        item.fullUrl = gifUriStr;
                        list.add(item);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to scan device for GIFs", e);
        }
        return list;
    }

    private void sendGifFromUrl(TenorGifItem item) {
        if (item == null || item.fullUrl == null) return;
        if (item.fullUrl.startsWith("file://") || item.fullUrl.startsWith("content://")) {
            sendPhoto(Uri.parse(item.fullUrl), null);
            closeAttachmentPanel();
            return;
        }
        PrimeNotification.INSTANCE.show(this, "Загрузка GIF...", null);
        runOnIoThread(() -> {
            try {
                URL url = new URL(item.fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    byte[] gifBytes;
                    try (InputStream is = conn.getInputStream();
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, len);
                        }
                        gifBytes = baos.toByteArray();
                    }

                    File gifFile = ChatHistoryManager.saveBytesToAtomicFile(ChatPersonActivity.this, "sent_gif_" + System.currentTimeMillis() + ".gif", gifBytes);
                    if (gifFile != null) {
                        Uri gifUri = Uri.fromFile(gifFile);
                        runOnUiThread(() -> {
                            sendPhoto(gifUri, null);
                            closeAttachmentPanel();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to download and send GIF", e);
                runOnUiThread(() -> PrimeNotification.show(ChatPersonActivity.this, "Ошибка загрузки GIF", null));
            }
        });
    }

    private void initAttachmentPanel() {
        if (attachmentDialog != null) return;

        attachmentDialog = new BottomSheetDialog(this, R.style.Theme_Prime_BottomSheetDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.layout_attachment_panel, null);
        attachmentDialog.setContentView(dialogView);

        if (attachmentDialog.getWindow() != null) {
            attachmentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            attachmentDialog.getWindow().setDimAmount(0.5f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    attachmentDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    WindowManager.LayoutParams params = attachmentDialog.getWindow().getAttributes();
                    params.setBlurBehindRadius(60);
                    attachmentDialog.getWindow().setAttributes(params);
                } catch (Throwable ignored) {}
            }
        }

        attachmentDialog.setOnShowListener(dialog -> {
            View bottomSheetView = attachmentDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetView != null) {
                bottomSheetView.setBackgroundColor(Color.TRANSPARENT);
                ViewGroup.LayoutParams lp = bottomSheetView.getLayoutParams();
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheetView.setLayoutParams(lp);
                }
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetView);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setFitToContents(false);
                behavior.setExpandedOffset(0);
            }
        });

        attachmentDialog.setOnDismissListener(dialog -> {
            isAttachmentPanelOpen = false;
            if (btnAttach != null) {
                btnAttach.setImageResource(R.drawable.ic_photo);
            }
            if (galleryAdapter != null) galleryAdapter.clearSelection();
            if (filesAdapter != null) filesAdapter.clearSelection();
            if (musicAdapter != null) musicAdapter.clearSelection();
            updateConfirmAttachmentButton();
        });

        layoutAttachmentPanel = dialogView.findViewById(R.id.layoutAttachmentPanel);
        if (layoutAttachmentPanel != null) {
            layoutAttachmentPanel.setVisibility(View.VISIBLE);
            if (layoutAttachmentPanel instanceof eightbitlab.com.blurview.BlurView && attachmentDialog.getWindow() != null) {
                eightbitlab.com.blurview.BlurView blurView = (eightbitlab.com.blurview.BlurView) layoutAttachmentPanel;
                ViewGroup decorView = (ViewGroup) attachmentDialog.getWindow().getDecorView();
                Drawable bg = decorView.getBackground();
                BlurViewKt.setupBlur(blurView, decorView, 16f, Color.parseColor("#400F172A"), bg);
            }
        }
        btnPickGifFromGallery = dialogView.findViewById(R.id.btnPickGifFromGallery);
        if (btnPickGifFromGallery != null) {
            btnPickGifFromGallery.setOnClickListener(v -> {
                hideSoftKeyboard();
                if (checkAndRequestAllAppPermissions()) {
                    pickSystemFileLauncher.launch("image/gif");
                }
            });
        }

        etGifSearch = dialogView.findViewById(R.id.etGifSearch);
        pbGifLoading = dialogView.findViewById(R.id.pbGifLoading);
        tvGifEmpty = dialogView.findViewById(R.id.tvGifEmpty);
        rvTenorGifs = dialogView.findViewById(R.id.rvTenorGifs);

        if (rvTenorGifs != null) {
            rvTenorGifs.setLayoutManager(new GridLayoutManager(this, 2));
            tenorAdapter = new TenorGifAdapter();
            rvTenorGifs.setAdapter(tenorAdapter);
        }

        if (etGifSearch != null) {
            etGifSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    hideSoftKeyboard();
                    fetchTenorGifs(etGifSearch.getText().toString());
                    return true;
                }
                return false;
            });

            etGifSearch.addTextChangedListener(new TextWatcher() {
                private final Handler searchHandler = new Handler(Looper.getMainLooper());
                private Runnable searchRunnable;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> fetchTenorGifs(s.toString());
                    searchHandler.postDelayed(searchRunnable, 500);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        layoutModeCamera = dialogView.findViewById(R.id.layoutModeCamera);
        layoutModePhoto = dialogView.findViewById(R.id.layoutModePhoto);
        layoutModeFiles = dialogView.findViewById(R.id.layoutModeFiles);
        layoutModeMusic = dialogView.findViewById(R.id.layoutModeMusic);

        vModeCameraBg = dialogView.findViewById(R.id.vModeCameraBg);
        vModePhotoBg = dialogView.findViewById(R.id.vModePhotoBg);
        vModeFilesBg = dialogView.findViewById(R.id.vModeFilesBg);
        vModeMusicBg = dialogView.findViewById(R.id.vModeMusicBg);

        ivModeCameraIcon = dialogView.findViewById(R.id.ivModeCameraIcon);
        ivModePhotoIcon = dialogView.findViewById(R.id.ivModePhotoIcon);
        ivModeFilesIcon = dialogView.findViewById(R.id.ivModeFilesIcon);
        ivModeMusicIcon = dialogView.findViewById(R.id.ivModeMusicIcon);

        tvModeCameraLabel = dialogView.findViewById(R.id.tvModeCameraLabel);
        tvModePhotoLabel = dialogView.findViewById(R.id.tvModePhotoLabel);
        tvModeFilesLabel = dialogView.findViewById(R.id.tvModeFilesLabel);
        tvModeMusicLabel = dialogView.findViewById(R.id.tvModeMusicLabel);

        layoutSectionsContainer = dialogView.findViewById(R.id.layoutSectionsContainer);
        layoutSectionCamera = dialogView.findViewById(R.id.layoutSectionCamera);
        layoutSectionPhoto = dialogView.findViewById(R.id.layoutSectionPhoto);
        layoutSectionFiles = dialogView.findViewById(R.id.layoutSectionFiles);
        layoutSectionMusic = dialogView.findViewById(R.id.layoutSectionMusic);

        rvMusicGrid = dialogView.findViewById(R.id.rvMusicGrid);
        tvMusicEmpty = dialogView.findViewById(R.id.tvMusicEmpty);
        btnMusicTabDevice = dialogView.findViewById(R.id.btnMusicTabDevice);
        btnMusicTabSaved = dialogView.findViewById(R.id.btnMusicTabSaved);

        if (layoutSectionsContainer != null) {
            GestureDetector swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 80 && Math.abs(velocityX) > 150) {
                        if (diffX < 0) {
                            if (currentAttachmentMode < 3) {
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

        rvGalleryGrid = dialogView.findViewById(R.id.rvGalleryGrid);
        rvFilesGrid = dialogView.findViewById(R.id.rvFilesGrid);

        tvGalleryEmpty = dialogView.findViewById(R.id.tvGalleryEmpty);
        tvFilesEmpty = dialogView.findViewById(R.id.tvFilesEmpty);

        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;

        int photoSpanCount = isTablet ? 5 : 3;
        int gifSpanCount = isTablet ? 3 : 2;

        if (rvTenorGifs != null) {
            rvTenorGifs.setLayoutManager(new GridLayoutManager(this, gifSpanCount));
            tenorAdapter = new TenorGifAdapter();
            rvTenorGifs.setAdapter(tenorAdapter);
        }

        if (rvGalleryGrid != null) {
            rvGalleryGrid.setLayoutManager(new GridLayoutManager(this, photoSpanCount));
            rvGalleryGrid.setHasFixedSize(true);
            rvGalleryGrid.setItemViewCacheSize(20);
            galleryAdapter = new GalleryGridAdapter();
            rvGalleryGrid.setAdapter(galleryAdapter);
        }

        if (rvFilesGrid != null) {
            rvFilesGrid.setLayoutManager(new GridLayoutManager(this, photoSpanCount));
            rvFilesGrid.setHasFixedSize(true);
            rvFilesGrid.setItemViewCacheSize(20);
            filesAdapter = new FileGridAdapter();
            rvFilesGrid.setAdapter(filesAdapter);
        }

        layoutConfirmContainer = dialogView.findViewById(R.id.layoutConfirmContainer);
        tvConfirmBadgeCounter = dialogView.findViewById(R.id.tvConfirmBadgeCounter);
        btnConfirmAttachment = dialogView.findViewById(R.id.btnConfirmAttachment);
        if (btnConfirmAttachment != null) {
            btnConfirmAttachment.setOnClickListener(v -> {
                if (!isConnectionActive()) {
                    Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                    closeAttachmentPanel();
                    return;
                }
                if (currentAttachmentMode == 0 && galleryAdapter != null && galleryAdapter.getSelectedCount() > 0) {
                    setPendingAttachmentFromMediaItems(galleryAdapter.getSelectedItems(), true);
                } else if (currentAttachmentMode == 1 && filesAdapter != null && filesAdapter.getSelectedCount() > 0) {
                    setPendingAttachmentFromFileItems(filesAdapter.getSelectedItems(), true);
                } else if (currentAttachmentMode == 3 && musicAdapter != null && musicAdapter.getSelectedItems().size() > 0) {
                    setPendingAttachmentFromMusic(musicAdapter.getSelectedItems(), true);
                } else {
                    closeAttachmentPanel();
                }
            });
        }

        View btnOpenSystemGallery = dialogView.findViewById(R.id.btnOpenSystemGallery);
        if (btnOpenSystemGallery != null) {
            btnOpenSystemGallery.setOnClickListener(v -> {
                if (!isConnectionActive()) {
                    Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                    closeAttachmentPanel();
                    return;
                }
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

        View btnOpenSystemFileManager = dialogView.findViewById(R.id.btnOpenSystemFileManager);
        if (btnOpenSystemFileManager != null) {
            btnOpenSystemFileManager.setOnClickListener(v -> {
                if (!isConnectionActive()) {
                    Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                    closeAttachmentPanel();
                    return;
                }
                hideSoftKeyboard();
                if (checkAndRequestAllAppPermissions()) {
                    pickSystemFileLauncher.launch("*/*");
                }
            });
        }

        if (layoutModePhoto != null) layoutModePhoto.setOnClickListener(v -> {
            if (!isConnectionActive()) {
                Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                closeAttachmentPanel();
                return;
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(0);
        });
        if (layoutModeFiles != null) layoutModeFiles.setOnClickListener(v -> {
            if (!isConnectionActive()) {
                Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                closeAttachmentPanel();
                return;
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(1);
        });
        if (layoutModeCamera != null) layoutModeCamera.setOnClickListener(v -> {
            if (!isConnectionActive()) {
                Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                closeAttachmentPanel();
                return;
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(2);
        });
        if (layoutModeMusic != null) layoutModeMusic.setOnClickListener(v -> {
            if (!isConnectionActive()) {
                Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
                closeAttachmentPanel();
                return;
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            hideSoftKeyboard();
            if (checkAndRequestAllAppPermissions()) switchAttachmentMode(3);
        });
    }

    @Override
    public void onTrackChanged(@androidx.annotation.Nullable com.messenger.prime.TrackItem track) {
        if (track != null && PrimeMusicManager.INSTANCE.isPlaying()) {
            stopAllVideoNotePlayback();
        }
        runOnUiThread(() -> updateMusicIslandUI(track));
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (isPlaying) {
            stopAllVideoNotePlayback();
        }
        runOnUiThread(() -> {
            if (btnMusicIslandPlayPause != null) {
                btnMusicIslandPlayPause.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
            }
            if (musicAdapter != null) {
                com.messenger.prime.TrackItem current = PrimeMusicManager.INSTANCE.getCurrentTrack();
                musicAdapter.setActiveTrack(current != null ? current.getId() : null, isPlaying);
            }
        });
    }

    @Override
    public void onProgressUpdated(long progress, long duration) {
        runOnUiThread(() -> {
            if (tvMusicIslandTime != null) {
                tvMusicIslandTime.setText(PrimeMusicManager.INSTANCE.formatDuration(progress) + " / " + PrimeMusicManager.INSTANCE.formatDuration(duration));
            }
            if (pbMusicIslandProgress != null && duration > 0) {
                int percent = (int) ((progress * 100) / duration);
                pbMusicIslandProgress.setProgress(Math.max(0, Math.min(100, percent)));
            }
        });
    }

    private boolean isMusicIslandShowing = false;

    private void checkAndUpdateVersionInfoIsland() {
        if (isVersionInfoDismissed) {
            if (layoutVersionInfo != null) layoutVersionInfo.setVisibility(View.GONE);
            updateIslandVisibility();
            return;
        }

        int myVersion = getAppVersionCode(this);
        if (remoteVersionCode != -1 && remoteVersionCode != myVersion) {
            if (layoutVersionInfo != null && tvVersionInfoText != null) {
                if (remoteVersionCode > myVersion) {
                    tvVersionInfoText.setText("Пользователь использует более новую версию клиента");
                } else {
                    tvVersionInfoText.setText("Пользователь использует предыдущую версию клиента");
                }
                layoutVersionInfo.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutVersionInfo != null) layoutVersionInfo.setVisibility(View.GONE);
        }
        updateIslandVisibility();
    }

    private void updateIslandVisibility() {
        if (layoutMusicIsland == null) return;

        boolean hasVersionInfo = (layoutVersionInfo != null && layoutVersionInfo.getVisibility() == View.VISIBLE);
        boolean hasMusicInfo = (layoutMusicSection != null && layoutMusicSection.getVisibility() == View.VISIBLE);
        boolean isVideoNoteActive = chatAdapter != null && chatAdapter.getCurrentlyPlayingPosition() != -1;

        if ((hasVersionInfo || hasMusicInfo) && !isVideoNoteActive) {
            showMusicIslandWithAnimation();
        } else {
            hideMusicIslandWithAnimation();
        }
    }

    private void showMusicIslandWithAnimation() {
        if (layoutMusicIsland == null) return;

        boolean hasVersionInfo = (layoutVersionInfo != null && layoutVersionInfo.getVisibility() == View.VISIBLE);
        boolean hasMusicInfo = (layoutMusicSection != null && layoutMusicSection.getVisibility() == View.VISIBLE);
        if (!hasVersionInfo && !hasMusicInfo) {
            layoutMusicIsland.animate().cancel();
            layoutMusicIsland.setVisibility(View.GONE);
            layoutMusicIsland.setTranslationY(0f);
            layoutMusicIsland.setAlpha(1f);
            isMusicIslandShowing = false;
            return;
        }

        ViewGroup rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) rootView = (ViewGroup) getWindow().getDecorView();

        float density = getResources().getDisplayMetrics().density;
        float cornerRadiusPx = 26f * density;
        layoutMusicIsland.setClipToOutline(true);
        layoutMusicIsland.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
            }
        });

        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        android.graphics.drawable.Drawable windowBg = getWindow().getDecorView().getBackground();
        int overlayColor = isDark ? Color.parseColor("#400F172A") : Color.parseColor("#40154B87");
        BlurViewKt.setupBlur(layoutMusicIsland, rootView, 16f, overlayColor, windowBg);
        layoutMusicIsland.invalidateOutline();

        if (isMusicIslandShowing && layoutMusicIsland.getVisibility() == View.VISIBLE) return;

        isMusicIslandShowing = true;
        layoutMusicIsland.setVisibility(View.VISIBLE);
        layoutMusicIsland.animate().cancel();

        layoutMusicIsland.setAlpha(0f);
        layoutMusicIsland.setTranslationY(-30f * density);

        layoutMusicIsland.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(260)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();
    }

    private void hideMusicIslandWithAnimation() {
        if (layoutMusicIsland == null) return;
        isMusicIslandShowing = false;
        layoutMusicIsland.animate().cancel();

        float density = getResources().getDisplayMetrics().density;
        layoutMusicIsland.animate()
                .translationY(-30f * density)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    layoutMusicIsland.setVisibility(View.GONE);
                    layoutMusicIsland.setTranslationY(0f);
                    layoutMusicIsland.setAlpha(1f);
                })
                .start();
    }

    private void updateMusicIslandUI(com.messenger.prime.TrackItem track) {
        if (layoutMusicIsland == null) return;
        if (track == null) {
            if (layoutMusicSection != null) layoutMusicSection.setVisibility(View.GONE);
            updateIslandVisibility();
            return;
        }

        if (layoutMusicSection != null) {
            layoutMusicSection.setVisibility(View.VISIBLE);
        }

        if (tvMusicIslandTitle != null) {
            String newTitle = track.getTitle() + " - " + track.getArtist();
            if (!newTitle.equals(tvMusicIslandTitle.getText().toString())) {
                tvMusicIslandTitle.setText(newTitle);
            }
            if (!tvMusicIslandTitle.isSelected()) {
                tvMusicIslandTitle.setSelected(true);
            }
        }

        updateIslandVisibility();
        onPlaybackStateChanged(PrimeMusicManager.INSTANCE.isPlaying());
    }

    private void initMusicSection() {
        if (rvMusicGrid != null && musicAdapter == null) {
            rvMusicGrid.setLayoutManager(new LinearLayoutManager(this));
            musicAdapter = new MusicTrackAdapter(new ArrayList<>(), track -> {
                PrimeMusicManager.INSTANCE.playTrack(this, track, currentMusicList);
                return kotlin.Unit.INSTANCE;
            }, selectionCount -> {
                updateConfirmAttachmentButton();
                return kotlin.Unit.INSTANCE;
            });
            rvMusicGrid.setAdapter(musicAdapter);

            if (btnMusicTabDevice != null) {
                btnMusicTabDevice.setOnClickListener(v -> {
                    showingSavedMusic = false;
                    loadMusicTracksAsync();
                });
            }
            if (btnMusicTabSaved != null) {
                btnMusicTabSaved.setOnClickListener(v -> {
                    showingSavedMusic = true;
                    loadMusicTracksAsync();
                });
            }
        }
    }

    private void loadMusicTracksAsync() {
        if (btnMusicTabDevice != null) btnMusicTabDevice.setAlpha(showingSavedMusic ? 0.5f : 1.0f);
        if (btnMusicTabSaved != null) btnMusicTabSaved.setAlpha(showingSavedMusic ? 1.0f : 0.5f);

        new Thread(() -> {
            List<com.messenger.prime.TrackItem> tracks = showingSavedMusic
                    ? new ArrayList<>(PrimeMusicManager.INSTANCE.getSavedTracks(this))
                    : new ArrayList<>(PrimeMusicManager.INSTANCE.getDeviceTracks(this));

            runOnUiThread(() -> {
                currentMusicList.clear();
                currentMusicList.addAll(tracks);
                if (musicAdapter != null) {
                    musicAdapter.updateTracks(currentMusicList);
                }
                if (tvMusicEmpty != null) {
                    tvMusicEmpty.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (tvModeMusicLabel != null) {
                    tvModeMusicLabel.setText("Музыка" + (tracks.isEmpty() ? "" : " (" + tracks.size() + ")"));
                }
            });
        }).start();
    }

    private void toggleAttachmentPanel() {
        if (attachmentDialog != null && attachmentDialog.isShowing()) {
            closeAttachmentPanel();
        } else {
            openAttachmentPanel();
        }
    }

    private void openAttachmentPanel() {
        if (!isConnectionActive()) {
            Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
            return;
        }
        hideSoftKeyboard();
        
        // Delay opening to allow keyboard to hide and window to resize
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isDestroyed() || isFinishing()) return;
            if (attachmentDialog == null) {
                initAttachmentPanel();
            }
            if (attachmentDialog != null && !attachmentDialog.isShowing()) {
                isAttachmentPanelOpen = true;
                sendActivityState("SELECTING_MEDIA");
                if (btnAttach != null) {
                    btnAttach.setImageResource(R.drawable.ic_arrow_up);
                }
                if (layoutAttachmentPanel != null) {
                    layoutAttachmentPanel.setVisibility(View.VISIBLE);
                }
                if (galleryAdapter != null) galleryAdapter.clearSelection();
                if (filesAdapter != null) filesAdapter.clearSelection();
                if (musicAdapter != null) musicAdapter.clearSelection();
                updateConfirmAttachmentButton();

                switchAttachmentMode(currentAttachmentMode);
                try {
                    attachmentDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    private void closeAttachmentPanel() {
        isAttachmentPanelOpen = false;
        sendActivityState("IDLE");
        if (btnAttach != null) {
            btnAttach.setImageResource(R.drawable.ic_photo);
        }
        if (galleryAdapter != null) galleryAdapter.clearSelection();
        if (filesAdapter != null) filesAdapter.clearSelection();
        if (musicAdapter != null) musicAdapter.clearSelection();
        updateConfirmAttachmentButton();

        if (attachmentDialog != null && attachmentDialog.isShowing()) {
            try {
                attachmentDialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    private void updateConfirmAttachmentButton() {
        if (layoutConfirmContainer == null || tvConfirmBadgeCounter == null) return;

        int galleryCount = (galleryAdapter != null) ? galleryAdapter.getSelectedCount() : 0;
        int filesCount = (filesAdapter != null) ? filesAdapter.getSelectedCount() : 0;
        int musicCount = (musicAdapter != null) ? musicAdapter.getSelectedItems().size() : 0;
        
        int totalCount = 0;
        if (currentAttachmentMode == 0) totalCount = galleryCount;
        else if (currentAttachmentMode == 1) totalCount = filesCount;
        else if (currentAttachmentMode == 3) totalCount = musicCount;

        if (totalCount > 0) {
            String newCountStr = String.valueOf(totalCount);

            if (totalCount != lastConfirmCount) {
                if (lastConfirmCount != -1 && tvConfirmBadgeCounter.getVisibility() == View.VISIBLE) {
                    tvConfirmBadgeCounter.animate().cancel();
                    tvConfirmBadgeCounter.animate()
                            .translationY(25f)
                            .alpha(0f)
                            .setDuration(100)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(() -> {
                                tvConfirmBadgeCounter.setText(newCountStr);
                                tvConfirmBadgeCounter.setTranslationY(-25f);
                                tvConfirmBadgeCounter.animate()
                                        .translationY(0f)
                                        .alpha(1f)
                                        .setDuration(140)
                                        .setInterpolator(new OvershootInterpolator(1.8f))
                                        .start();
                            })
                            .start();
                } else {
                    tvConfirmBadgeCounter.setText(newCountStr);
                    tvConfirmBadgeCounter.setTranslationY(0f);
                    tvConfirmBadgeCounter.setAlpha(1f);
                }
                lastConfirmCount = totalCount;
            }

            if (layoutConfirmContainer.getVisibility() != View.VISIBLE) {
                layoutConfirmContainer.setVisibility(View.VISIBLE);
                layoutConfirmContainer.setAlpha(0f);
                layoutConfirmContainer.setScaleX(0.7f);
                layoutConfirmContainer.setScaleY(0.7f);
                layoutConfirmContainer.animate().cancel();
                layoutConfirmContainer.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(new OvershootInterpolator(1.4f))
                        .start();
            }
        } else {
            lastConfirmCount = -1;
            if (layoutConfirmContainer.getVisibility() == View.VISIBLE) {
                layoutConfirmContainer.animate().cancel();
                layoutConfirmContainer.animate()
                        .alpha(0f)
                        .scaleX(0.7f)
                        .scaleY(0.7f)
                        .setDuration(180)
                        .withEndAction(() -> layoutConfirmContainer.setVisibility(View.GONE))
                        .start();
            } else {
                layoutConfirmContainer.setVisibility(View.GONE);
            }
        }
    }

    private View getSectionViewForMode(int mode) {
        if (mode == 0) return layoutSectionPhoto;
        if (mode == 1) return layoutSectionFiles;
        if (mode == 2) return layoutSectionCamera;
        if (mode == 3) return layoutSectionMusic;
        return null;
    }

    private void switchAttachmentMode(int newMode) {
        int oldMode = currentAttachmentMode;
        View oldView = getSectionViewForMode(oldMode);
        View newView = getSectionViewForMode(newMode);

        currentAttachmentMode = newMode;

        int activeBrand = ContextCompat.getColor(this, R.color.prime_brand);
        int idleSecondary = ContextCompat.getColor(this, R.color.prime_text_secondary);

        if (vModePhotoBg != null) vModePhotoBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);
        if (vModeFilesBg != null) vModeFilesBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);
        if (vModeCameraBg != null) vModeCameraBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);
        if (vModeMusicBg != null) vModeMusicBg.setBackgroundResource(R.drawable.bg_circular_mode_idle);

        if (ivModePhotoIcon != null) ImageViewCompat.setImageTintList(ivModePhotoIcon, ColorStateList.valueOf(idleSecondary));
        if (ivModeFilesIcon != null) ImageViewCompat.setImageTintList(ivModeFilesIcon, ColorStateList.valueOf(idleSecondary));
        if (ivModeCameraIcon != null) ImageViewCompat.setImageTintList(ivModeCameraIcon, ColorStateList.valueOf(idleSecondary));
        if (ivModeMusicIcon != null) ImageViewCompat.setImageTintList(ivModeMusicIcon, ColorStateList.valueOf(idleSecondary));

        if (tvModePhotoLabel != null) tvModePhotoLabel.setTextColor(idleSecondary);
        if (tvModeFilesLabel != null) tvModeFilesLabel.setTextColor(idleSecondary);
        if (tvModeCameraLabel != null) tvModeCameraLabel.setTextColor(idleSecondary);
        if (tvModeMusicLabel != null) tvModeMusicLabel.setTextColor(idleSecondary);

        if (newMode == 0) { // Фото и видео
            if (vModePhotoBg != null) vModePhotoBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModePhotoIcon != null) ImageViewCompat.setImageTintList(ivModePhotoIcon, ColorStateList.valueOf(activeBrand));
            if (tvModePhotoLabel != null) tvModePhotoLabel.setTextColor(activeBrand);
            loadGalleryMediaAsync();
        } else if (newMode == 1) { // Файлы
            if (vModeFilesBg != null) vModeFilesBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeFilesIcon != null) ImageViewCompat.setImageTintList(ivModeFilesIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeFilesLabel != null) tvModeFilesLabel.setTextColor(activeBrand);
            loadFilesAsync();
        } else if (newMode == 2) { // GIF
            if (vModeCameraBg != null) vModeCameraBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeCameraIcon != null) ImageViewCompat.setImageTintList(ivModeCameraIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeCameraLabel != null) tvModeCameraLabel.setTextColor(activeBrand);
            fetchTenorGifs("");
        } else if (newMode == 3) { // Музыка
            if (vModeMusicBg != null) vModeMusicBg.setBackgroundResource(R.drawable.bg_circular_mode_active);
            if (ivModeMusicIcon != null) ImageViewCompat.setImageTintList(ivModeMusicIcon, ColorStateList.valueOf(activeBrand));
            if (tvModeMusicLabel != null) tvModeMusicLabel.setTextColor(activeBrand);
            initMusicSection();
            loadMusicTracksAsync();
        }

        if (oldView == null || newView == null || oldView == newView) {
            if (layoutSectionPhoto != null) layoutSectionPhoto.setVisibility(newMode == 0 ? View.VISIBLE : View.GONE);
            if (layoutSectionFiles != null) layoutSectionFiles.setVisibility(newMode == 1 ? View.VISIBLE : View.GONE);
            if (layoutSectionCamera != null) layoutSectionCamera.setVisibility(newMode == 2 ? View.VISIBLE : View.GONE);
            if (layoutSectionMusic != null) layoutSectionMusic.setVisibility(newMode == 3 ? View.VISIBLE : View.GONE);
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

        boolean isAudio = isAudioMimeOrPath(uri, path);
        boolean isVid = !isAudio && isVideo;
        boolean isImg = !isAudio && !isVid && isPhotoMimeOrPath(uri, path);
        ChatMessage.MessageType msgType = isAudio ? ChatMessage.MessageType.MUSIC : (isVid ? ChatMessage.MessageType.VIDEO : (isImg ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.FILE));

        if (isVid) {
            thumbnail = getVideoThumbnail(path != null ? path : uri.toString());
            durationStr = getVideoDurationFromUri(uri, path);
        } else if (isImg) {
            thumbnail = getPhotoThumbnail(uri, path);
        }

        PendingAttachmentItem item = new PendingAttachmentItem(uri, path != null ? path : uri.toString(), name, size, isVid, !isVid && !isImg && !isAudio, msgType, thumbnail, durationStr);

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
            ChatMessage.MessageType msgType = mi.isVideo ? ChatMessage.MessageType.VIDEO : ChatMessage.MessageType.IMAGE;
            if (mi.isVideo) {
                thumb = getVideoThumbnail(mi.path != null ? mi.path : (mi.uri != null ? mi.uri.toString() : null));
            } else {
                thumb = getPhotoThumbnail(mi.uri, mi.path);
            }
            PendingAttachmentItem item = new PendingAttachmentItem(
                mi.uri,
                mi.path != null ? mi.path : (mi.uri != null ? mi.uri.toString() : ""),
                mi.name != null ? mi.name : (mi.isVideo ? "Видео" : "Фотография"),
                mi.size,
                mi.isVideo,
                false,
                msgType,
                thumb,
                mi.durationStr
            );
            pending.items.add(item);
        }

        this.currentPendingAttachment = pending;
        showPendingAttachmentBar(pending);
        if (closePanel) closeAttachmentPanel();
    }

    private void setPendingAttachmentFromMusic(List<com.messenger.prime.TrackItem> tracks, boolean closePanel) {
        if (tracks == null || tracks.isEmpty()) return;
        PendingAttachment pending = new PendingAttachment();
        for (com.messenger.prime.TrackItem track : tracks) {
            String path = track.getPath();
            String name = track.getTitle() + " - " + track.getArtist();
            PendingAttachmentItem item = new PendingAttachmentItem(
                Uri.parse(path),
                path,
                name,
                track.getFileSize(),
                false,
                false,
                ChatMessage.MessageType.MUSIC,
                null,
                track.getDurationStr()
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
            boolean isAud = isAudioMimeOrPath(fi.uri, fi.path);
            boolean isVid = !isAud && isVideoMimeOrPath(fi.uri, fi.path);
            boolean isImg = !isAud && !isVid && isPhotoMimeOrPath(fi.uri, fi.path);
            ChatMessage.MessageType msgType = isAud ? ChatMessage.MessageType.MUSIC : (isVid ? ChatMessage.MessageType.VIDEO : (isImg ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.FILE));

            Bitmap thumb = isVid ? getVideoThumbnail(fi.path != null ? fi.path : (fi.uri != null ? fi.uri.toString() : null)) :
                    (isImg ? getPhotoThumbnail(fi.uri, fi.path) : null);
            String dur = isVid ? getVideoDurationFromUri(fi.uri, fi.path) : (isAud ? "00:00" : null);
            PendingAttachmentItem item = new PendingAttachmentItem(
                fi.uri,
                fi.path != null ? fi.path : (fi.uri != null ? fi.uri.toString() : ""),
                fi.name != null ? fi.name : (isAud ? "Аудиозапись" : (isVid ? "Видео" : (isImg ? "Фотография" : "Файл"))),
                fi.size,
                isVid,
                !isVid && !isImg && !isAud,
                msgType,
                thumb,
                dur
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

        layoutPendingAttachment.animate().cancel();
        layoutPendingAttachment.animate().setListener(null);
        layoutPendingAttachment.setVisibility(View.VISIBLE);
        layoutPendingAttachment.setAlpha(1f);
        layoutPendingAttachment.setTranslationY(0f);
        updateActionIslandVisibility();
        updateActionButtonsVisibility(true);
    }

    private void clearPendingAttachment() {
        currentPendingAttachment = null;
        if (galleryAdapter != null) galleryAdapter.clearSelection();
        if (filesAdapter != null) filesAdapter.clearSelection();
        if (musicAdapter != null) musicAdapter.clearSelection();

        if (layoutPendingAttachment != null) {
            layoutPendingAttachment.animate().cancel();
            layoutPendingAttachment.animate().setListener(null);
            layoutPendingAttachment.setVisibility(View.GONE);
            layoutPendingAttachment.setAlpha(1f);
            layoutPendingAttachment.setTranslationY(0f);
        }
        updateActionIslandVisibility();
        updateActionButtonsVisibility(true);
    }

    private void showSendingProgressUi(int progress) {
        if (layoutSendingProgress != null) {
            if (progress >= 0 && progress < 100) {
                if (pbSendingProgress != null) pbSendingProgress.setProgress(progress);
                if (tvSendingProgressPercent != null) {
                    tvSendingProgressPercent.setText("Передача... " + progress + "%");
                }
                layoutSendingProgress.animate().cancel();
                layoutSendingProgress.animate().setListener(null);
                layoutSendingProgress.setVisibility(View.VISIBLE);
                layoutSendingProgress.setAlpha(1f);
                layoutSendingProgress.setTranslationY(0f);
                updateActionIslandVisibility();
            } else {
                hideSendingProgressUi();
            }
        }

        if (!isActivityForeground && progress >= 0 && progress < 100) {
            PrimeNotification.INSTANCE.showSendingProgressNotification(
                this,
                targetUsername,
                deviceAddress,
                progress,
                "файл"
            );
        } else {
            PrimeNotification.INSTANCE.cancelSendingProgressNotification(this);
        }
    }

    private void hideSendingProgressUi() {
        if (layoutSendingProgress != null) {
            layoutSendingProgress.animate().cancel();
            layoutSendingProgress.animate().setListener(null);
            layoutSendingProgress.setVisibility(View.GONE);
            layoutSendingProgress.setAlpha(1f);
            layoutSendingProgress.setTranslationY(0f);
        }
        PrimeNotification.INSTANCE.cancelSendingProgressNotification(this);
        updateActionIslandVisibility();
    }

    private void cancelCurrentFileSending() {
        pendingMessageQueue.removeIf(pm -> pm.type == TYPE_PHOTO || pm.type == TYPE_FILE);

        String targetAddr = (deviceAddress != null && !deviceAddress.isEmpty()) ? deviceAddress : targetUsername;
        if (targetAddr == null || targetAddr.isEmpty() || !isValidMacAddress(targetAddr)) {
            targetAddr = findMacForTargetUsername(targetUsername);
        }
        if (targetAddr != null && !targetAddr.isEmpty()) {
            BluetoothConnectionManager.getInstance().cancelCurrentMediaSend(targetAddr);
        }

        activeSendingMessageId = null;
        activeSendingTargetUsername = null;
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
        hideSendingProgressUi();
        saveActivityStateToChatList(targetUsername, "IDLE");
        sendActivityState("IDLE");
        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Отправка файла отменена", null);
    }

    private boolean isAudioMimeOrPath(Uri uri, String path) {
        if (path != null) {
            String lower = path.toLowerCase(Locale.US);
            if (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".opus") || lower.endsWith(".wma")) {
                return true;
            }
        }
        if (uri != null) {
            String mime = getContentResolver().getType(uri);
            return mime != null && mime.startsWith("audio/");
        }
        return false;
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
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                options.inSampleSize = calculateInSampleSize(options, 512, 512);
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeFile(path, options);
            }
            if (uri != null) {
                return decodeSampledBitmapFromUri(uri, 512, 512);
            }
        } catch (Throwable ignored) {}
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
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            if (uri != null && uri.toString().startsWith("content://")) {
                retriever.setDataSource(this, uri);
            } else if (path != null) {
                retriever.setDataSource(path);
            }
            String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durStr != null) {
                long ms = Long.parseLong(durStr);
                return formatMsToDuration(ms);
            }
        } catch (Exception ignored) {
        } finally {
            if (retriever != null) {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        }
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
        runOnIoThread(() -> {
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
                if (tvModePhotoLabel != null) {
                    tvModePhotoLabel.setText("Фото и видео" + (list.isEmpty() ? "" : " (" + list.size() + ")"));
                }
            });
        });
    }

    private void loadFilesAsync() {
        runOnIoThread(() -> {
            List<FileItem> list = new ArrayList<>();

            Uri queryUri = MediaStore.Files.getContentUri("external");
            String[] projection = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            };

            String selection = MediaStore.Files.FileColumns.SIZE + " > 0";
            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, null, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                    int nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);

                    int count = 0;
                    while (cursor.moveToNext() && count < 100) {
                        long id = cursor.getLong(idCol);
                        long size = cursor.getLong(sizeCol);
                        long date = cursor.getLong(dateCol);
                        String name = nameCol != -1 ? cursor.getString(nameCol) : null;
                        String path = dataCol != -1 ? cursor.getString(dataCol) : null;

                        if (name == null || name.trim().isEmpty()) {
                            if (path != null) name = new File(path).getName();
                        }
                        if (name == null || name.trim().isEmpty() || name.startsWith(".")) continue;

                        String lowerName = name.toLowerCase(Locale.US);
                        if (lowerName.endsWith(".tmp") || lowerName.endsWith(".dat") || lowerName.endsWith(".db") || 
                            lowerName.endsWith(".nomedia") || lowerName.endsWith(".shm") || lowerName.endsWith(".wal")) {
                            continue;
                        }

                        if (size > 0 && size <= 2 * 1024 * 1024 * 1024L) {
                            Uri contentUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                            list.add(new FileItem(contentUri, path, name, size, date));
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback scan of Downloads, Documents, and Music directories
            String[] targetDirs = new String[] {
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_MUSIC
            };

            for (String dirName : targetDirs) {
                if (list.size() >= 100) break;
                try {
                    File dir = Environment.getExternalStoragePublicDirectory(dirName);
                    if (dir != null && dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File f : files) {
                                if (f.isFile() && f.length() > 0 && !f.getName().startsWith(".")) {
                                    String fname = f.getName();
                                    boolean exists = false;
                                    for (FileItem fi : list) {
                                        if (fname.equalsIgnoreCase(fi.name)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        Uri contentUri = Uri.fromFile(f);
                                        list.add(new FileItem(contentUri, f.getAbsolutePath(), fname, f.length(), f.lastModified() / 1000));
                                        if (list.size() >= 100) break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            runOnUiThread(() -> {
                if (filesAdapter != null) {
                    filesAdapter.setItems(list);
                    if (tvFilesEmpty != null) {
                        tvFilesEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
                if (tvModeFilesLabel != null) {
                    tvModeFilesLabel.setText("Файлы" + (list.isEmpty() ? "" : " (" + list.size() + ")"));
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
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    try (InputStream is = getContentResolver().openInputStream(item.uri)) {
                        if (is != null) {
                            BitmapFactory.decodeStream(is, null, options);
                        }
                    }
                    options.inSampleSize = calculateInSampleSize(options, 220, 220);
                    options.inJustDecodeBounds = false;
                    try (InputStream is2 = getContentResolver().openInputStream(item.uri)) {
                        if (is2 != null) {
                            thumb = BitmapFactory.decodeStream(is2, null, options);
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
                } else {
                    if (selectedItems.size() < 20) {
                        selectedItems.add(item);
                    } else {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Максимум 20 медиафайлов", null);
                    }
                }
                notifyItemChanged(position);
                updateConfirmAttachmentButton();
            });
        }

        int getSelectedCount() {
            return selectedItems.size();
        }

        List<MediaItem> getSelectedItems() {
            return new ArrayList<>(selectedItems);
        }

        void removeItemByUri(Uri uri, String path) {
            selectedItems.removeIf(mi -> (uri != null && uri.equals(mi.uri)) || (path != null && !path.isEmpty() && path.equals(mi.path)));
            notifyDataSetChanged();
            updateConfirmAttachmentButton();
        }

        void clearSelection() {
            selectedItems.clear();
            notifyDataSetChanged();
            updateConfirmAttachmentButton();
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

        int getSelectedCount() {
            return selectedFileItems.size();
        }

        List<FileItem> getSelectedItems() {
            return new ArrayList<>(selectedFileItems);
        }

        void clearSelection() {
            selectedFileItems.clear();
            notifyDataSetChanged();
            updateConfirmAttachmentButton();
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
                } else {
                    if (selectedFileItems.size() < 20) {
                        selectedFileItems.add(item);
                    } else {
                        PrimeNotification.INSTANCE.show(ChatPersonActivity.this, "Максимум 20 файлов", null);
                    }
                }
                notifyItemChanged(position);
                updateConfirmAttachmentButton();
            });
        }

        void removeItemByUri(Uri uri, String path) {
            selectedFileItems.removeIf(fi -> (uri != null && uri.equals(fi.uri)) || (path != null && !path.isEmpty() && path.equals(fi.path)));
            notifyDataSetChanged();
            updateConfirmAttachmentButton();
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupPullToOpenPersonInfo() {
        layoutPullIndicator = findViewById(R.id.layoutPullIndicator);
        pbPullProgress = findViewById(R.id.pbPullProgress);
        tvPullStatus = findViewById(R.id.tvPullStatus);
        ivPullCheck = findViewById(R.id.ivPullCheck);

        if (rvMessages == null) return;

        rvMessages.setOverScrollMode(View.OVER_SCROLL_NEVER);

        float density = getResources().getDisplayMetrics().density;
        final float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float pullThresholdPx = 130f * density;
        final float maxTranslationPx = 110f * density;

        rvMessages.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int action = e.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        pullStartY = e.getRawY();
                        pullStartX = e.getRawX();
                        isPullToOpenActive = false;
                        stage1HapticTriggered = false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dy = e.getRawY() - pullStartY;
                        float dx = e.getRawX() - pullStartX;

                        if (!rv.canScrollVertically(-1) && dy > touchSlop && dy > Math.abs(dx) * 1.2f) {
                            isPullToOpenActive = true;
                            MotionEvent cancelEvent = MotionEvent.obtain(e);
                            cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                            rv.onTouchEvent(cancelEvent);
                            cancelEvent.recycle();
                            return true;
                        }
                        break;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (!isPullToOpenActive) return;

                int action = e.getActionMasked();
                float dy = e.getRawY() - pullStartY;
                if (dy < 0) dy = 0;

                switch (action) {
                    case MotionEvent.ACTION_MOVE: {
                        float progress = Math.min(1.0f, dy / pullThresholdPx);
                        if (progress >= 1.0f) progress = 1.0f;
                        float dampenedDistance = Math.min(maxTranslationPx, dy * 0.45f);

                        updatePullUi(progress, dampenedDistance);

                        if (progress >= 0.5f && !stage1HapticTriggered) {
                            stage1HapticTriggered = true;
                            triggerHapticVibration();
                        } else if (progress < 0.5f && stage1HapticTriggered) {
                            stage1HapticTriggered = false;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        float finalDy = e.getRawY() - pullStartY;
                        float finalProgress = Math.min(1.0f, Math.max(0f, finalDy) / pullThresholdPx);
                        finishPullGesture(finalProgress);
                        break;
                    }
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
        final int[] autoScrollVelocity = {0};
        final boolean[] isDragSelectingMode = {true};

        final Runnable autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (chatAdapter != null && chatAdapter.isMultiSelectionMode() && autoScrollVelocity[0] != 0 && rvMessages != null) {
                    rvMessages.scrollBy(0, autoScrollVelocity[0]);
                    int checkY = autoScrollVelocity[0] < 0 ? (int) (100 * density) : (rvMessages.getHeight() - (int) (100 * density));
                    View child = rvMessages.findChildViewUnder(rvMessages.getWidth() / 2f, checkY);
                    if (child != null) {
                        int pos = rvMessages.getChildAdapterPosition(child);
                        if (pos != RecyclerView.NO_POSITION && pos != lastDragPos) {
                            int startPos = dragStartPos != -1 ? dragStartPos : pos;
                            if (chatAdapter.updateDragSelectionRange(startPos, pos, isDragSelectingMode[0])) {
                                rvMessages.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            }
                            lastDragPos = pos;
                        }
                    }
                    autoScrollHandler.postDelayed(this, 25);
                }
            }
        };

        rvMessages.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (chatAdapter == null || !chatAdapter.isMultiSelectionMode()) {
                    return false;
                }
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    dragStartPos = -1;
                    lastDragPos = -1;
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null) {
                        int pos = rv.getChildAdapterPosition(child);
                        if (pos != RecyclerView.NO_POSITION) {
                            dragStartPos = pos;
                            lastDragPos = pos;
                            ChatMessage msg = chatAdapter.getMessageAt(pos);
                            isDragSelectingMode[0] = msg == null || !chatAdapter.getSelectedMessageIds().contains(msg.getMessageId());
                            chatAdapter.startDragSelection(pos);
                        }
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (dragStartPos == -1) {
                        View child = rv.findChildViewUnder(e.getX(), e.getY());
                        if (child != null) {
                            int pos = rv.getChildAdapterPosition(child);
                            if (pos != RecyclerView.NO_POSITION) {
                                dragStartPos = pos;
                                lastDragPos = pos;
                                ChatMessage msg = chatAdapter.getMessageAt(pos);
                                isDragSelectingMode[0] = msg == null || !chatAdapter.getSelectedMessageIds().contains(msg.getMessageId());
                                chatAdapter.startDragSelection(pos);
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (chatAdapter == null || !chatAdapter.isMultiSelectionMode()) {
                    autoScrollVelocity[0] = 0;
                    autoScrollHandler.removeCallbacks(autoScrollRunnable);
                    return;
                }
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_MOVE) {
                    float y = e.getY();
                    int rvHeight = rv.getHeight();
                    int topEdge = (int) (110 * density);
                    int bottomEdge = rvHeight - (int) (110 * density);

                    if (y < topEdge) {
                        autoScrollVelocity[0] = -Math.min(30, Math.max(8, (int) ((topEdge - y) / 3f)));
                        autoScrollHandler.removeCallbacks(autoScrollRunnable);
                        autoScrollHandler.post(autoScrollRunnable);
                    } else if (y > bottomEdge) {
                        autoScrollVelocity[0] = Math.min(30, Math.max(8, (int) ((y - bottomEdge) / 3f)));
                        autoScrollHandler.removeCallbacks(autoScrollRunnable);
                        autoScrollHandler.post(autoScrollRunnable);
                    } else {
                        autoScrollVelocity[0] = 0;
                        autoScrollHandler.removeCallbacks(autoScrollRunnable);
                    }

                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null) {
                        int pos = rv.getChildAdapterPosition(child);
                        if (pos != RecyclerView.NO_POSITION && pos != lastDragPos) {
                            int startPos = dragStartPos != -1 ? dragStartPos : pos;
                            if (chatAdapter.updateDragSelectionRange(startPos, pos, isDragSelectingMode[0])) {
                                rv.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            }
                            lastDragPos = pos;
                        }
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (action == MotionEvent.ACTION_UP && dragStartPos != -1) {
                        chatAdapter.updateDragSelectionRange(dragStartPos, lastDragPos != -1 ? lastDragPos : dragStartPos, isDragSelectingMode[0]);
                    }
                    autoScrollVelocity[0] = 0;
                    autoScrollHandler.removeCallbacks(autoScrollRunnable);
                    dragStartPos = -1;
                    lastDragPos = -1;
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        View layoutHeader = findViewById(R.id.layoutHeader);
        if (layoutHeader != null) {
            layoutHeader.setOnTouchListener(new View.OnTouchListener() {
                private float hStartY = 0f;
                private float hStartX = 0f;
                private boolean hDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    int action = e.getActionMasked();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            hStartY = e.getRawY();
                            hStartX = e.getRawX();
                            hDragging = false;
                            stage1HapticTriggered = false;
                            return false;

                        case MotionEvent.ACTION_MOVE: {
                            float dy = e.getRawY() - hStartY;
                            float dx = e.getRawX() - hStartX;

                            if (!hDragging && dy > touchSlop && dy > Math.abs(dx) * 1.2f) {
                                hDragging = true;
                                isPullToOpenActive = true;
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                            }

                            if (hDragging) {
                                float progress = Math.min(1.0f, Math.max(0f, dy) / pullThresholdPx);
                                if (progress >= 1.0f) progress = 1.0f;
                                float dampenedDistance = Math.min(maxTranslationPx, Math.max(0f, dy) * 0.45f);

                                updatePullUi(progress, dampenedDistance);

                                if (progress >= 0.5f && !stage1HapticTriggered) {
                                    stage1HapticTriggered = true;
                                    triggerHapticVibration();
                                } else if (progress < 0.5f && stage1HapticTriggered) {
                                    stage1HapticTriggered = false;
                                }

                                return true;
                            }
                            break;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            if (hDragging) {
                                float dyUp = e.getRawY() - hStartY;
                                float finalProgress = Math.min(1.0f, Math.max(0f, dyUp) / pullThresholdPx);
                                finishPullGesture(finalProgress);
                                hDragging = false;
                                return true;
                            }
                            break;
                        }
                    }
                    return false;
                }
            });
        }
    }

    private void updatePullUi(float progress, float dampenedDistance) {
        View layoutHeader = findViewById(R.id.layoutHeader);
        View tvFloatingDate = findViewById(R.id.tvFloatingDate);

        if (layoutHeader != null) {
            layoutHeader.setTranslationY(dampenedDistance);
        }
        if (rvMessages != null) {
            rvMessages.setTranslationY(dampenedDistance);
        }
        if (tvFloatingDate != null && tvFloatingDate.getAlpha() > 0) {
            tvFloatingDate.setTranslationY(dampenedDistance);
        }

        if (layoutPullIndicator != null) {
            if (layoutPullIndicator.getVisibility() != View.VISIBLE) {
                layoutPullIndicator.setVisibility(View.VISIBLE);
            }
            layoutPullIndicator.setAlpha(Math.min(1.0f, progress * 1.5f));
            layoutPullIndicator.setTranslationY(dampenedDistance * 0.35f);
        }

        if (pbPullProgress != null) pbPullProgress.setProgress((int) (progress * 100));

        if (tvPullStatus != null) {
            tvPullStatus.setText("Информация о собеседнике");
        }

        if (ivPullCheck != null) {
            ivPullCheck.setVisibility(progress >= 0.85f ? View.VISIBLE : View.GONE);
        }
    }

    private void finishPullGesture(float finalProgress) {
        isPullToOpenActive = false;

        View layoutHeader = findViewById(R.id.layoutHeader);
        View tvFloatingDate = findViewById(R.id.tvFloatingDate);

        long duration = 220L;
        DecelerateInterpolator interpolator = new DecelerateInterpolator();

        if (layoutHeader != null) {
            layoutHeader.animate().translationY(0).setDuration(duration).setInterpolator(interpolator).start();
        }
        if (rvMessages != null) {
            rvMessages.animate().translationY(0).setDuration(duration).setInterpolator(interpolator).start();
        }
        if (tvFloatingDate != null) {
            tvFloatingDate.animate().translationY(0).setDuration(duration).setInterpolator(interpolator).start();
        }

        if (layoutPullIndicator != null) {
            layoutPullIndicator.animate()
                    .translationY(0)
                    .alpha(0f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction(() -> {
                        layoutPullIndicator.setVisibility(View.INVISIBLE);
                        if (pbPullProgress != null) pbPullProgress.setProgress(0);
                        if (ivPullCheck != null) ivPullCheck.setVisibility(View.GONE);
                    })
                    .start();
        }

        if (finalProgress >= 0.75f) {
            openPersonInformationActivity();
        }
    }

    private void triggerHapticVibration() {
        View chatRoot = findViewById(R.id.chatRoot);
        if (chatRoot != null) {
            chatRoot.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        }
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    @SuppressWarnings("deprecation")
                    Vibrator vibrator = v;
                    vibrator.vibrate(45);
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateActionButtonsVisibility(boolean animate) {
        if (btnSend == null || btnVoiceVideoNote == null || etMessage == null) return;

        boolean hasText = etMessage.getText() != null && etMessage.getText().toString().trim().length() > 0;
        boolean hasAttachment = currentPendingAttachment != null && currentPendingAttachment.items != null && !currentPendingAttachment.items.isEmpty();
        boolean shouldShowSend = hasText || hasAttachment;

        boolean active = isConnectionActive();
        btnSend.setEnabled(active);
        btnVoiceVideoNote.setEnabled(active);
        if (btnAttach != null) {
            btnAttach.setEnabled(true);
            btnAttach.setClickable(true);
            btnAttach.setAlpha(active ? 1.0f : 0.4f);
        }

        if (shouldShowSend) {
            if (btnSend.getVisibility() != View.VISIBLE) {
                if (animate) {
                    btnVoiceVideoNote.animate().cancel();
                    btnVoiceVideoNote.animate()
                            .scaleX(0.0f)
                            .scaleY(0.0f)
                            .alpha(0.0f)
                            .setDuration(180)
                            .withEndAction(() -> btnVoiceVideoNote.setVisibility(View.GONE))
                            .start();

                    btnSend.setVisibility(View.VISIBLE);
                    btnSend.setScaleX(0.2f);
                    btnSend.setScaleY(0.2f);
                    btnSend.setRotation(-90f);
                    btnSend.setAlpha(0.0f);
                    btnSend.animate().cancel();
                    btnSend.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .rotation(0f)
                            .alpha(active ? 1.0f : 0.4f)
                            .setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start();
                } else {
                    btnVoiceVideoNote.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                    btnSend.setScaleX(1.0f);
                    btnSend.setScaleY(1.0f);
                    btnSend.setRotation(0f);
                    btnSend.setAlpha(active ? 1.0f : 0.4f);
                }
            } else {
                btnSend.setAlpha(active ? 1.0f : 0.4f);
            }
        } else {
            if (btnVoiceVideoNote.getVisibility() != View.VISIBLE) {
                if (animate) {
                    btnSend.animate().cancel();
                    btnSend.animate()
                            .scaleX(0.0f)
                            .scaleY(0.0f)
                            .rotation(90f)
                            .alpha(0.0f)
                            .setDuration(180)
                            .withEndAction(() -> btnSend.setVisibility(View.GONE))
                            .start();

                    btnVoiceVideoNote.setVisibility(View.VISIBLE);
                    btnVoiceVideoNote.setScaleX(0.2f);
                    btnVoiceVideoNote.setScaleY(0.2f);
                    btnVoiceVideoNote.setAlpha(0.0f);
                    btnVoiceVideoNote.animate().cancel();
                    btnVoiceVideoNote.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(active ? 1.0f : 0.4f)
                            .setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start();
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnVoiceVideoNote.setVisibility(View.VISIBLE);
                    btnVoiceVideoNote.setScaleX(1.0f);
                    btnVoiceVideoNote.setScaleY(1.0f);
                    btnVoiceVideoNote.setAlpha(active ? 1.0f : 0.4f);
                }
            } else {
                btnVoiceVideoNote.setAlpha(active ? 1.0f : 0.4f);
            }
        }
    }

    private void toggleVoiceVideoNoteMode() {
        if (btnVoiceVideoNote == null) return;
        if (!isConnectionActive()) {
            Toast.makeText(ChatPersonActivity.this, "Действие недоступно: нет подключения к собеседнику", Toast.LENGTH_SHORT).show();
            return;
        }
        btnVoiceVideoNote.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

        isVideoNoteMode = !isVideoNoteMode;
        SharedPreferences sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        sp.edit().putBoolean("chat_media_record_mode_is_video", isVideoNoteMode).apply();

        btnVoiceVideoNote.animate().cancel();
        btnVoiceVideoNote.animate()
                .rotationBy(180f)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .alpha(0.0f)
                .setDuration(160)
                .withEndAction(() -> {
                    btnVoiceVideoNote.setImageResource(isVideoNoteMode ? R.drawable.ic_camera : R.drawable.ic_mic);
                    btnVoiceVideoNote.animate()
                            .rotationBy(180f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(1.3f))
                            .start();
                })
                .start();
    }

    private void initVideoMessageRecorder() {
        FrameLayout layoutVideoRecordOverlay = findViewById(R.id.layoutVideoRecordOverlay);
        FrameLayout viewCameraPreview = findViewById(R.id.viewCameraPreview);
        TextView tvVideoRecordTimer = findViewById(R.id.tvVideoRecordTimer);
        com.messenger.prime.PrimeWaterLoadingView pbVideoRecordTime = findViewById(R.id.pbVideoRecordTime);
        View layoutVideoPreviewControls = findViewById(R.id.layoutVideoPreviewControls);
        ImageButton btnVideoRecordCancel = findViewById(R.id.btnVideoRecordCancel);
        ImageButton btnVideoRecordRetry = findViewById(R.id.btnVideoRecordRetry);
        ImageButton btnVideoRecordSend = findViewById(R.id.btnVideoRecordSend);
        ImageButton btnVideoRecordSettings = findViewById(R.id.btnVideoRecordSettings);
        ImageButton btnVideoRecordStopLocked = findViewById(R.id.btnVideoRecordStopLocked);

        if (layoutVideoRecordOverlay != null && viewCameraPreview != null && btnVoiceVideoNote != null) {
            videoMessageRecorder = new PrimeVideoMessageRecorder(
                this,
                this,
                layoutVideoRecordOverlay,
                viewCameraPreview,
                btnVoiceVideoNote,
                tvVideoRecordTimer,
                pbVideoRecordTime,
                layoutVideoPreviewControls,
                btnVideoRecordCancel,
                btnVideoRecordRetry,
                btnVideoRecordSend,
                btnVideoRecordSettings,
                btnVideoRecordStopLocked,
                new PrimeVideoMessageRecorder.VideoRecordListener() {
                    @Override
                    public void onVideoRecorded(@NonNull File file) {
                        sendVideoNoteFile(file);
                    }

                    @Override
                    public void onVoiceRecorded(@NonNull File file, @NonNull java.util.List<Integer> amplitudes) {
                        sendVoiceMessage(file.getAbsolutePath(), amplitudes);
                    }

                    @Override
                    public void onOpenSettings() {
                        Intent intent = new Intent(ChatPersonActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public boolean isVideoMode() {
                        return isVideoNoteMode;
                    }

                    @Override
                    public boolean isConnectionActive() {
                        return ChatPersonActivity.this.isConnectionActive();
                    }

                    @Override
                    public void onRecordingStateChanged(boolean isRecording) {
                        if (isRecording) {
                            stopAllVideoNotePlayback();
                            sendActivityState("RECORDING_VIDEO_NOTE");
                        } else {
                            sendActivityState("IDLE");
                        }
                    }
                }
            );
        }
    }

    private void sendVideoNoteFile(File file) {
        if (file == null || !file.exists()) return;

        if (isRecipientVersionBelow64()) {
            sendUpdateTextToRecipient();
            return;
        }

        runOnUiThread(this::stopAllVideoNotePlayback);

        SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        int radiusPercent = sharedPrefs.getInt("video_message_corner_radius", 50);
        final String finalShapeTag = String.valueOf(radiusPercent);

        File storageDir = new File(getFilesDir(), "video_notes");
        if (!storageDir.exists()) storageDir.mkdirs();

        String fileName = "vn_" + System.currentTimeMillis() + "_" + finalShapeTag + ".mp4";
        File permFile = new File(storageDir, fileName);

        try {
            java.nio.file.Files.copy(file.toPath(), permFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Log.e(TAG, "Error copying video note file", e);
            return;
        }

        runOnIoThread(() -> {
            try {
                String durationStr = "00:00";
                android.media.MediaMetadataRetriever retriever = null;
                try {
                    retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(permFile.getAbsolutePath());
                    String timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (timeStr != null) {
                        long durationMs = Long.parseLong(timeStr);
                        long sec = (durationMs / 1000) % 60;
                        long min = (durationMs / (1000 * 60)) % 60;
                        durationStr = String.format(Locale.US, "%02d:%02d", min, sec);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (retriever != null) {
                        try { retriever.release(); } catch (Exception ignored) {}
                    }
                }

                Uri fileUri = Uri.fromFile(permFile);
                PendingAttachmentItem pendingItem = new PendingAttachmentItem(
                    fileUri,
                    permFile.getAbsolutePath(),
                    permFile.getName(),
                    permFile.length(),
                    true,
                    false,
                    ChatMessage.MessageType.VIDEO,
                    null,
                    durationStr
                );
                sendVideoOrFile(pendingItem, "VIDEO_NOTE:" + finalShapeTag);
            } catch (Exception e) {
                Log.e(TAG, "Error sending video note", e);
            }
        });
    }

    private void initVideoNotePlaybackManagers() {
        eightbitlab.com.blurview.BlurView layoutVideoNoteIsland = findViewById(R.id.layoutVideoNoteIsland);
        if (layoutVideoNoteIsland != null) {
            videoNoteIslandManager = new com.messenger.prime.VideoNoteIslandManager(
                layoutVideoNoteIsland,
                findViewById(R.id.btnVideoIslandPlayPause),
                findViewById(R.id.tvVideoIslandSenderName),
                findViewById(R.id.pbVideoIslandProgress),
                findViewById(R.id.tvVideoIslandDuration),
                findViewById(R.id.btnVideoIslandSpeed),
                findViewById(R.id.btnVideoIslandClose)
            );
        }

        FrameLayout layoutVideoNoteFloatingPip = findViewById(R.id.layoutVideoNoteFloatingPip);
        com.messenger.prime.TextureVideoView vvFloatingPip = findViewById(R.id.vvFloatingPip);
        if (layoutVideoNoteFloatingPip != null && vvFloatingPip != null) {
            floatingPipManager = new com.messenger.prime.FloatingPipManager(
                layoutVideoNoteFloatingPip,
                vvFloatingPip,
                () -> {
                    if (chatAdapter != null) {
                        chatAdapter.stopCurrentlyPlayingVideoNote();
                    }
                    if (videoNoteIslandManager != null) {
                        videoNoteIslandManager.hideIsland();
                    }
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    if (chatAdapter != null && rvMessages != null) {
                        int pos = chatAdapter.getCurrentlyPlayingPosition();
                        if (pos != -1) {
                            rvMessages.smoothScrollToPosition(pos);
                        }
                    }
                    return kotlin.Unit.INSTANCE;
                }
            );
        }

        videoNoteMediaSessionManager = new com.messenger.prime.VideoNoteMediaSessionManager(
            getApplicationContext(),
            new com.messenger.prime.VideoNoteMediaSessionManager.OnMediaSessionActionListener() {
                @Override
                public void onMediaPlay() {
                    handleVideoNotePlayPauseToggle();
                }

                @Override
                public void onMediaPause() {
                    handleVideoNotePlayPauseToggle();
                }

                @Override
                public void onMediaStop() {
                    stopAllVideoNotePlayback();
                }
            }
        );

        if (videoNoteIslandManager != null) {
            videoNoteIslandManager.setOnPlayPauseClickListener(() -> {
                handleVideoNotePlayPauseToggle();
                return kotlin.Unit.INSTANCE;
            });

            videoNoteIslandManager.setOnSpeedClickListener(speed -> {
                if (floatingPipManager != null && floatingPipManager.isShowing()) {
                    floatingPipManager.setSpeed(speed);
                } else if (chatAdapter != null && rvMessages != null) {
                    int activePos = chatAdapter.getCurrentlyPlayingPosition();
                    if (activePos != -1) {
                        RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(activePos);
                        if (holder != null && holder.itemView != null) {
                            com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                            if (vv != null) {
                                vv.setSpeed(speed);
                            }
                        }
                    }
                }
                return kotlin.Unit.INSTANCE;
            });

            videoNoteIslandManager.setOnCloseClickListener(() -> {
                stopAllVideoNotePlayback();
                return kotlin.Unit.INSTANCE;
            });
        }

        if (chatAdapter != null) {
            chatAdapter.setOnVideoNotePlaybackListener(new ChatAdapter.OnVideoNotePlaybackListener() {
                @Override
                public void onVideoNoteStarted(ChatMessage message, android.media.MediaPlayer mp, String shape, int position) {
                    try {
                        PrimeMusicManager.INSTANCE.stopAndReleasePlayer(ChatPersonActivity.this);
                    } catch (Exception ignored) {}
                    if (layoutMusicSection != null) layoutMusicSection.setVisibility(View.GONE);
                    hideMusicIslandWithAnimation();

                    if (videoNoteIslandManager != null) {
                        videoNoteIslandManager.showIsland(formatDisplayName(message.getSenderLogin()), mp != null ? mp.getDuration() : 0);
                    }
                    if (videoNoteMediaSessionManager != null) {
                        videoNoteMediaSessionManager.startMediaSession(formatDisplayName(message.getSenderLogin()), message.getImagePath(), mp != null ? mp.getDuration() : 0, true);
                    }
                    startIslandProgressLoop();
                }

                @Override
                public void onVideoNoteStopped(ChatMessage message) {
                    stopAllVideoNotePlayback();
                }

                @Override
                public void onPlayNextVideoNote(int nextPosition) {
                    runOnUiThread(() -> {
                        if (rvMessages != null && chatAdapter != null) {
                            rvMessages.smoothScrollToPosition(nextPosition);
                            rvMessages.postDelayed(() -> {
                                RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(nextPosition);
                                if (holder != null && holder.itemView != null) {
                                    View vnContainer = holder.itemView.findViewById(R.id.layoutVideoNoteContainer);
                                    if (vnContainer != null) {
                                        vnContainer.performClick();
                                    }
                                }
                            }, 250);
                        }
                    });
                }
            });
        }

        if (rvMessages != null && rvMessages.getLayoutManager() instanceof LinearLayoutManager) {
            rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (chatAdapter == null) return;

                    ChatMessage activeMsg = chatAdapter.getCurrentlyPlayingVideoNote();
                    int activePos = chatAdapter.getCurrentlyPlayingPosition();
                    if (activeMsg != null && activePos != -1) {
                        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                        int firstVisible = lm.findFirstVisibleItemPosition();
                        int lastVisible = lm.findLastVisibleItemPosition();

                        String text = activeMsg.getText();
                        String shape = "circle";
                        if (text != null && text.startsWith("VIDEO_NOTE:")) {
                            shape = text.substring("VIDEO_NOTE:".length()).trim();
                        }

                        if (activePos < firstVisible || activePos > lastVisible) {
                            if (floatingPipManager != null && activeMsg.getImagePath() != null && !floatingPipManager.isShowing()) {
                                int currentMs = 0;
                                RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(activePos);
                                if (holder != null && holder.itemView != null) {
                                    com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                                    if (vv != null) currentMs = vv.getCurrentPosition();
                                }
                                floatingPipManager.showPip(activeMsg.getImagePath(), shape, currentMs);
                            }
                        } else {
                            if (floatingPipManager != null && floatingPipManager.isShowing()) {
                                int pipMs = floatingPipManager.hidePipForReturn();
                                if (chatAdapter != null) {
                                    chatAdapter.syncVideoNoteTimestamp(rvMessages, activePos, pipMs);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void handleVideoNotePlayPauseToggle() {
        if (floatingPipManager != null && floatingPipManager.isShowing()) {
            floatingPipManager.pause();
            if (videoNoteIslandManager != null) videoNoteIslandManager.updatePlayPauseIcon(false);
            if (videoNoteMediaSessionManager != null) videoNoteMediaSessionManager.updatePlaybackState(false, floatingPipManager.getCurrentPosition(), 0f);
        } else if (chatAdapter != null) {
            int activePos = chatAdapter.getCurrentlyPlayingPosition();
            if (activePos != -1 && rvMessages != null) {
                RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(activePos);
                if (holder != null && holder.itemView != null) {
                    com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                    if (vv != null) {
                        if (vv.isPlaying()) {
                            vv.pause();
                            if (videoNoteIslandManager != null) videoNoteIslandManager.updatePlayPauseIcon(false);
                            if (videoNoteMediaSessionManager != null) videoNoteMediaSessionManager.updatePlaybackState(false, vv.getCurrentPosition(), 0f);
                        } else {
                            vv.start();
                            if (videoNoteIslandManager != null) videoNoteIslandManager.updatePlayPauseIcon(true);
                            if (videoNoteMediaSessionManager != null) videoNoteMediaSessionManager.updatePlaybackState(true, vv.getCurrentPosition(), 1f);
                        }
                    }
                }
            }
        }
    }

    private final Handler islandProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable islandProgressRunnable = null;

    private void startIslandProgressLoop() {
        stopIslandProgressLoop();
        islandProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (chatAdapter != null) {
                    int activePos = chatAdapter.getCurrentlyPlayingPosition();
                    if (activePos != -1) {
                        int curMs = 0;
                        int durMs = 0;

                        if (floatingPipManager != null && floatingPipManager.isShowing()) {
                            curMs = floatingPipManager.getCurrentPosition();
                        } else if (rvMessages != null) {
                            RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(activePos);
                            if (holder != null && holder.itemView != null) {
                                com.messenger.prime.TextureVideoView vv = holder.itemView.findViewById(R.id.vvVideoNotePlayer);
                                if (vv != null) {
                                    curMs = vv.getCurrentPosition();
                                    durMs = vv.getDuration();
                                }
                            }
                        }

                        if (videoNoteIslandManager != null) {
                            videoNoteIslandManager.updateProgressUI(curMs, durMs);
                        }
                        islandProgressHandler.postDelayed(this, 100);
                        return;
                    }
                }
                stopIslandProgressLoop();
            }
        };
        islandProgressHandler.post(islandProgressRunnable);
    }

    private void stopIslandProgressLoop() {
        if (islandProgressRunnable != null) {
            islandProgressHandler.removeCallbacks(islandProgressRunnable);
            islandProgressRunnable = null;
        }
    }

    private void stopAllVideoNotePlayback() {
        stopIslandProgressLoop();
        if (chatAdapter != null) {
            chatAdapter.stopCurrentlyPlayingVideoNote();
        }
        if (videoNoteIslandManager != null) {
            videoNoteIslandManager.hideIsland();
        }
        if (floatingPipManager != null) {
            floatingPipManager.dismissPip();
        }
        if (videoNoteMediaSessionManager != null) {
            videoNoteMediaSessionManager.stopMediaSession();
        }
    }
}


