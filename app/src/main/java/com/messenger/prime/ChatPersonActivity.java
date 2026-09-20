package com.messenger.prime;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private static final byte TYPE_TEXT = 0x01;
    private static final byte TYPE_PHOTO = 0x02;
    private static final byte TYPE_PING = 0x03;
    private static final byte TYPE_TYPING = 0x04;
    private static final byte TYPE_EDIT = 0x05;
    private static final byte TYPE_AVATAR = 0x07;

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

    private boolean isHandshakeDone = false;
    private Boolean pendingRoleAsServer = null;
    private String remoteAvatarUri = null;

    private long lastTypingSentTime = 0;
    private final Handler typingResetHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetTypingRunnable = () -> {
        setStatusWithAnimation("В сети", R.color.prime_success);
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

    private final ActivityResultLauncher<String> pickPhotoLauncher = 
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    sendPhoto(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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

        View chatRoot = findViewById(R.id.chatRoot);
        if (chatRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                float density = getResources().getDisplayMetrics().density;
                int baseMargin = (int) (12 * density);

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

                View rvMessages = findViewById(R.id.rvMessages);
                if (rvMessages != null) {
                    int topPadding = systemBars.top + (int) (76 * density);
                    int bottomPadding = Math.max(systemBars.bottom, ime.bottom) + (int) (76 * density);
                    rvMessages.setPadding(
                            rvMessages.getPaddingLeft(),
                            topPadding,
                            rvMessages.getPaddingRight(),
                            bottomPadding
                    );
                }

                return insets;
            });
        }

        boolean isHandshakeDone = false;
        boolean isHandshakeDoneRef = false;
        tvChatName = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView ivChatAvatar = findViewById(R.id.ivChatAvatar);
        
        layoutConnectAction = findViewById(R.id.layoutConnectAction);
        btnPrimeConnect = findViewById(R.id.btnPrimeConnect);
        layoutInput = findViewById(R.id.layoutInput);
        btnAttach = findViewById(R.id.btnAttach);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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

        if (isOnlineExpected) {
            setStatusWithAnimation("В сети", R.color.prime_success);
        } else {
            setStatusWithAnimation("Не в сети", R.color.prime_text_secondary);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String currentUser = sharedPreferences.getString("current_user", "");
        String myDisplayName = sharedPreferences.getString(currentUser + "_name", currentUser);
        localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";

        updateAvatarUi(avatarUri, targetUsername);

        // Настройка списка
        chatAdapter = new ChatAdapter();
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

        chatAdapter.setOnMessageLongClickListener((message, position) -> {
            if (message.isOutgoing() && message.getImageBitmap() == null && (message.getImagePath() == null || message.getImagePath().isEmpty())) {
                showEditMessageDialog(message, position);
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
                        byte[] photoBuf = (byte[]) msg.obj;
                        Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoBuf, 0, photoBuf.length);
                        long photoTs = System.currentTimeMillis();
                        String photoTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(photoTs));
                        ChatMessage photoMsg = new ChatMessage(null, photoTime, targetUsername, false, photoBitmap, photoTs, null, null);
                        addMessageToUI(photoMsg);
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
                        Toast.makeText(ChatPersonActivity.this, msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                        break;
                    case HANDSHAKE_SUCCESS:
                        setStatusWithAnimation("Подключено по Bluetooth", R.color.prime_success);
                        if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
                        if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
                        sendLocalAvatar();
                        break;
                    case MESSAGE_TYPING:
                        setStatusWithAnimation("Печатает...", R.color.prime_success);
                        typingResetHandler.removeCallbacks(resetTypingRunnable);
                        typingResetHandler.postDelayed(resetTypingRunnable, 2500);
                        break;
                    case MESSAGE_EDIT_RECEIVED:
                        String editData = (String) msg.obj;
                        int editSep = editData.indexOf(":::");
                        if (editSep != -1) {
                            try {
                                String editMsgId = editData.substring(0, editSep);
                                String updatedText = editData.substring(editSep + 3);
                                chatAdapter.updateMessageById(editMsgId, updatedText);
                                saveLastMessageToChatList(updatedText);
                                
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
                }
            }
        };

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
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
                    connectedThread.sendPacket(TYPE_TYPING, new byte[]{1});
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

        if (useExistingSocket) {
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.GONE);
            if (layoutInput != null) layoutInput.setVisibility(View.VISIBLE);
            try {
                BluetoothSocket existingSocket = BluetoothSocketHolder.getSocket();
                if (existingSocket != null && existingSocket.isConnected()) {
                    BluetoothDevice device = null;
                    try {
                        device = existingSocket.getRemoteDevice();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get remote device", e);
                    }
                    connected(existingSocket, device);
                } else {
                    Toast.makeText(this, "Ошибка соединения: сокет потерян или закрыт", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fatal error resolving existing socket", e);
                Toast.makeText(this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            if (layoutConnectAction != null) layoutConnectAction.setVisibility(View.VISIBLE);
            if (layoutInput != null) layoutInput.setVisibility(View.GONE);
        }
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

    private void showEditMessageDialog(ChatMessage message, int position) {
        final EditText etInput = new EditText(this);
        etInput.setText(message.getText());
        etInput.setSelection(etInput.getText().length());
        etInput.setPadding(32, 16, 32, 16);

        new MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
                .setTitle("Редактировать сообщение")
                .setView(etInput)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newText = etInput.getText().toString().trim();
                    if (!newText.isEmpty() && !newText.equals(message.getText())) {
                        editMessage(message, newText, position);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void editMessage(ChatMessage message, String newText, int position) {
        message.setEdited(true);
        message.setText(newText);
        String payloadStr = message.getMessageId() + ":::" + newText;
        if (connectedThread != null) {
            connectedThread.sendPacket(TYPE_EDIT, payloadStr.getBytes(StandardCharsets.UTF_8));
        }
        chatAdapter.notifyItemChanged(position);
        ChatHistoryManager.saveMessage(this, targetUsername, message);
        saveLastMessageToChatList(newText);
    }

    private void setStatusWithAnimation(String newText, int colorResId) {
        if (tvChatStatus == null) return;
        CharSequence currentText = tvChatStatus.getText();
        if (currentText != null && currentText.toString().equals(newText)) return;

        tvChatStatus.animate()
                .translationY(25f)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    tvChatStatus.setText(newText);
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, colorResId));
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
        
        if (message.getImageBitmap() != null && (message.getImagePath() == null || message.getImagePath().isEmpty())) {
            String path = ChatHistoryManager.saveBitmapToFile(this, message.getImageBitmap(), message.getTimestamp());
            message.setImagePath(path);
        }
        
        chatAdapter.addMessage(message);
        rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        
        // Save to persistence
        ChatHistoryManager.saveMessage(this, targetUsername, message);
        String lastMsg = message.getText();
        saveLastMessageToChatList(lastMsg != null && !lastMsg.isEmpty() ? lastMsg : "Фотография");
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
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!targetName.equals(obj.optString("name")) && !(deviceAddress != null && deviceAddress.equals(obj.optString("id")))) {
                    newArray.put(obj);
                }
            }
            sharedPrefs.edit().putString("persisted_chats", newArray.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete chat from persisted_chats", e);
        }
    }

    private void saveLastMessageToChatList(String lastMsg) {
        if (targetUsername == null || targetUsername.isEmpty()) return;
        try {
            SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String json = sharedPrefs.getString("persisted_chats", "[]");
            JSONArray array = new JSONArray(json);
            boolean found = false;
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (targetUsername.equals(obj.optString("name")) || (deviceAddress != null && deviceAddress.equals(obj.optString("id")))) {
                    obj.put("lastMessage", lastMsg);
                    obj.put("time", timeStr);
                    if (remoteAvatarUri != null && !remoteAvatarUri.isEmpty()) {
                        obj.put("avatarUri", remoteAvatarUri);
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                JSONObject newChat = new JSONObject();
                newChat.put("id", deviceAddress != null ? deviceAddress : System.currentTimeMillis() + "");
                newChat.put("name", targetUsername);
                newChat.put("lastMessage", lastMsg);
                newChat.put("time", timeStr);
                newChat.put("avatarUri", remoteAvatarUri != null ? remoteAvatarUri : "");
                newChat.put("onlineStatus", "ONLINE");
                newChat.put("messageStatus", "NONE");
                newChat.put("unreadCount", 0);
                newChat.put("isMuted", false);
                array.put(newChat);
            }

            sharedPrefs.edit().putString("persisted_chats", array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update persisted_chats", e);
        }
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
        } else {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void sendText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        ChatMessage message = new ChatMessage(text, time, localUsername, true, null, timestamp, null, null);
        
        if (connectedThread != null) {
            String packetContent = message.getMessageId() + ":::" + text;
            connectedThread.sendPacket(TYPE_TEXT, packetContent.getBytes(StandardCharsets.UTF_8));
        }
        addMessageToUI(message);
    }

    private void sendPhoto(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                Bitmap scaledBitmap = scaleBitmapDown(bitmap, 1024);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] payload = baos.toByteArray();
                if (connectedThread != null) {
                    connectedThread.sendPacket(TYPE_PHOTO, payload);
                }
                long timestamp = System.currentTimeMillis();
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                ChatMessage photoMsg = new ChatMessage(null, time, localUsername, true, scaledBitmap, timestamp, null, null);
                addMessageToUI(photoMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load photo", e);
            Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show();
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

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        boolean handshakeReceived = getIntent().getBooleanExtra("EXTRA_HANDSHAKE_RECEIVED", false);
        connectedThread = new ConnectedThread(socket, handshakeReceived);
        connectedThread.start();

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
        Message msg = handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Не удалось подключиться");
        msg.setData(bundle);
        handler.sendMessage(msg);
        runOnUiThread(() -> {
            if (btnPrimeConnect != null) {
                btnPrimeConnect.setEnabled(true);
                btnPrimeConnect.setText("⚡ Праймериться!");
            }
            if (isOnlineExpected) {
                setStatusWithAnimation("В сети (ошибка BT)", R.color.prime_success);
            } else {
                setStatusWithAnimation("Не в сети", R.color.prime_text_secondary);
            }
        });
    }

    private void connectionLost() {
        if (!isHandshakeDone && connectedThread == null) {
            // Игнорируем фоновые ошибки во время подбора сокета
            return;
        }
        isHandshakeDone = false;
        Message msg = handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Соединение разорвано");
        msg.setData(bundle);
        handler.sendMessage(msg);
        
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }

        runOnUiThread(() -> {
            setStatusWithAnimation("Отключено", R.color.prime_danger);
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

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (Exception ignored) {}
        }
        try {
            PrimeBluetoothService.stopService(this);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to stop bluetooth service", e);
        }
        if (acceptThread != null) acceptThread.cancel();
        if (connectThread != null) connectThread.cancel();
        if (connectedThread != null) connectedThread.cancel();
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
            bluetoothAdapter.cancelDiscovery();
            
            int maxRetries = 5;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(UUID_CHAT);
                    mmSocket.connect();
                    connected(mmSocket, mmDevice);
                    return; // Успешно подключились, выходим
                } catch (IOException e) {
                    Log.e(TAG, "Connect try " + (i + 1) + " failed", e);
                    if (mmSocket != null) {
                        try {
                            mmSocket.close();
                        } catch (IOException ignored) {}
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;
        private boolean isHandshakeDone = false;
        private Handler keepAliveHandler;
        private Runnable keepAliveRunnable;

        public ConnectedThread(BluetoothSocket socket, boolean handshakeDone) {
            mmSocket = socket;
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
                handler.obtainMessage(HANDSHAKE_SUCCESS).sendToTarget();
            }
        }

        public void run() {
            if (mmInStream == null || mmOutStream == null) return;
            // Отправляем рукопожатие при старте
            try {
                SharedPreferences sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
                String currentUser = sharedPrefs.getString("current_user", "");
                String myDisplayName = sharedPrefs.getString(currentUser + "_name", currentUser);
                String localAvatar = sharedPrefs.getString(currentUser + "_avatar", "");
                String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + (myDisplayName != null ? myDisplayName : localUsername) + ";avatar=" + (localAvatar != null ? localAvatar : "");
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
                    
                    byte[] payload = null;
                    if (length > 0) {
                        payload = new byte[length];
                        mmInStream.readFully(payload);
                    }

                    if (type == TYPE_PING) {
                        handler.obtainMessage(HANDSHAKE_SUCCESS).sendToTarget();
                        continue;
                    }

                    if (type == TYPE_TEXT && payload != null) {
                        String receivedData = new String(payload, StandardCharsets.UTF_8);
                        
                        if (!isHandshakeDone) {
                            if (receivedData.startsWith("HANDSHAKE:")) {
                                isHandshakeDone = true;
                                String data = receivedData.substring(10).trim();
                                String remoteLogin = targetUsername;
                                String remoteName = targetUsername;
                                String remoteAvatar = null;
                                
                                if (data.contains("login=") || data.contains("name=")) {
                                    String[] parts = data.split(";");
                                    for (String p : parts) {
                                        if (p.startsWith("login=")) remoteLogin = p.substring(6);
                                        else if (p.startsWith("name=")) remoteName = p.substring(5);
                                        else if (p.startsWith("avatar=")) remoteAvatar = p.substring(7);
                                    }
                                } else {
                                    remoteName = data;
                                }
                                
                                if (!remoteName.isEmpty()) {
                                    targetUsername = remoteName;
                                    if (remoteAvatar != null && !remoteAvatar.isEmpty()) {
                                        try {
                                            Uri parsed = Uri.parse(remoteAvatar);
                                            if (!"file".equals(parsed.getScheme()) || (parsed.getPath() != null && new File(parsed.getPath()).exists())) {
                                                remoteAvatarUri = remoteAvatar;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                    final String finalName = remoteName;
                                    final String finalAvatar = remoteAvatar;
                                    runOnUiThread(() -> {
                                        tvChatName.setText(finalName);
                                        updateAvatarUi(finalAvatar, finalName);
                                    });
                                }
                                handler.obtainMessage(HANDSHAKE_SUCCESS).sendToTarget();
                            } else {
                                // Если первое сообщение пришло без префикса HANDSHAKE
                                handler.obtainMessage(MESSAGE_READ, payload.length, -1, payload).sendToTarget();
                            }
                        } else {
                            // Обычное текстовое сообщение
                            handler.obtainMessage(MESSAGE_READ, payload.length, -1, payload).sendToTarget();
                        }
                    } else if (type == TYPE_PHOTO && payload != null) {
                        handler.obtainMessage(MESSAGE_READ_PHOTO, payload.length, -1, payload).sendToTarget();
                    } else if (type == TYPE_AVATAR && payload != null) {
                        handler.obtainMessage(MESSAGE_READ_AVATAR, payload.length, -1, payload).sendToTarget();
                    } else if (type == TYPE_TYPING) {
                        handler.obtainMessage(MESSAGE_TYPING).sendToTarget();
                    } else if (type == TYPE_EDIT && payload != null) {
                        String data = new String(payload, StandardCharsets.UTF_8);
                        handler.obtainMessage(MESSAGE_EDIT_RECEIVED, -1, -1, data).sendToTarget();
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
        }

        public void sendPacket(byte type, byte[] payload) {
            if (mmOutStream == null) return;
            try {
                mmOutStream.writeByte(type);
                int length = payload != null ? payload.length : 0;
                mmOutStream.writeInt(length);
                if (length > 0 && payload != null) {
                    int offset = 0;
                    int chunkSize = 4096;
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
        }

        public void cancel() {
            try {
                if (keepAliveHandler != null && keepAliveRunnable != null) {
                    keepAliveHandler.removeCallbacks(keepAliveRunnable);
                }
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
