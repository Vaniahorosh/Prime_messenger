package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.messenger.prime.databinding.ActivityChatListContentBinding
import com.messenger.prime.databinding.LayoutIslandBinding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import org.json.JSONArray
import org.json.JSONObject

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListContentBinding
    private lateinit var islandBinding: LayoutIslandBinding
    private lateinit var adapter: ChatAdapter
    private var allChats: List<ChatModel> = ArrayList()

    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkConnected = true
    private var isLegacyListenersSetup = false
    private val isContentBindingReady = mutableStateOf(false)
    private val isIslandBindingReady = mutableStateOf(false)

    private val isIslandVisibleState = mutableStateOf(true)
    private val isContactDialogVisible = mutableStateOf(false)
    private val chatListState = mutableStateListOf<ChatModel>()
    private val hazeState = HazeState()

    private var startY = 0f
    private var isPulling = false
    private var isThresholdCrossed = false
    private val PULL_THRESHOLD = 350f
    private var isTransitioning = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread {
                isNetworkConnected = true
                adapter.updateNetworkHint("Прайм")
                refreshUserUi()
            }
        }

        override fun onLost(network: Network) {
            runOnUiThread {
                isNetworkConnected = false
                adapter.updateNetworkHint("ОЖИДАНИЕ СЕТИ")
            }
        }
    }

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        if (key == "current_user" || key?.endsWith("_name") == true || key?.endsWith("_avatar") == true) {
            runOnUiThread { refreshUserUi() }
        }
    }

    private fun animateSearchHint(hint: String) {
        if (!::islandBinding.isInitialized) return
        animateViewHint(islandBinding.inputLayoutSearch, hint)
    }

    private fun animateViewHint(inputLayout: com.google.android.material.textfield.TextInputLayout, newHint: String) {
        val editText = inputLayout.editText ?: return
        editText.animate().alpha(0f).setDuration(150).withEndAction {
            inputLayout.hint = newHint
            editText.animate().alpha(1f).setDuration(150).start()
        }.start()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isTransitioning) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                isPulling = false
                isThresholdCrossed = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - startY
                val layoutManager = binding.recyclerViewChats.layoutManager as LinearLayoutManager
                val isAtTop = layoutManager.findFirstCompletelyVisibleItemPosition() <= 0

                if (isAtTop && dy > 50 && !adapter.isSearchActive) {
                    isPulling = true
                    val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)
                    
                    if (progress >= 1.0f && !isThresholdCrossed) {
                        isThresholdCrossed = true
                        binding.recyclerViewChats.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    } else if (progress < 1.0f) {
                        isThresholdCrossed = false
                    }
                    
                    binding.recyclerViewChats.translationY = dy * 0.4f
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPulling) {
                    if (isThresholdCrossed && !isTransitioning) {
                        isTransitioning = true
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        
                        binding.recyclerViewChats.postDelayed({
                            resetPullUiInstant()
                            isTransitioning = false
                        }, 500)
                    } else {
                        cancelPullToProfile()
                    }
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun cancelPullToProfile() {
        binding.recyclerViewChats.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                isPulling = false
                isThresholdCrossed = false
            }
            .start()
    }

    private fun resetPullUiInstant() {
        binding.recyclerViewChats.translationY = 0f
        isPulling = false
        isThresholdCrossed = false
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (adapter.isSearchActive) {
                if (islandBinding.etSearch.hasFocus()) {
                    hideKeyboardAndClearFocus()
                } else {
                    islandBinding.etSearch.text?.clear()
                    hideKeyboardAndClearFocus()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
        
        setContentView(R.layout.activity_chat_list)

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        try {
            loadContacts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        allChats = ArrayList(chatListState)

        adapter = ChatAdapter(
            allChats, savedAvatarUri, savedName,
            onStartChatClick = { isContactDialogVisible.value = true },
            onAvatarClick = {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            },
            onAvatarLongClick = {
                showLogoutDialog()
            },
            onHeaderSearchClick = { activateIslandSearch() },
            onNameClick = { showNameEditDialog() },
            onChatClick = { chat ->
                if (chat.id == "block_test_contact") {
                    if (::islandBinding.isInitialized) {
                        val parts = islandBinding.etSearch.text.toString().split(" ")
                        if (parts.size >= 4) {
                            val reason = parts.subList(1, parts.size - 2).joinToString(" ")
                            val value = parts[parts.size - 2].toLongOrNull() ?: 0
                            val unit = parts[parts.size - 1]

                            val expiry = if (value <= 0) -1L else System.currentTimeMillis() + calculateMillis(value, unit)
                            sharedPrefs.edit().apply {
                                putString("ban_reason", reason)
                                putLong("ban_value", value)
                                putString("ban_unit", unit)
                                putLong("ban_expiry", expiry)
                                apply()
                            }

                            val intent = Intent(this, BanActivity::class.java).apply {
                                putExtra("EXTRA_USER_NAME", savedName)
                                putExtra("EXTRA_REASON", reason)
                                putExtra("EXTRA_VALUE", value)
                                putExtra("EXTRA_UNIT", unit)
                            }
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                } else {
                    val intent = Intent(this, ChatPersonActivity::class.java)
                    intent.putExtra("EXTRA_CHAT_ID", chat.id)
                    intent.putExtra("EXTRA_CHAT_NAME", chat.name)
                    intent.putExtra("EXTRA_CHAT_AVATAR", chat.avatarUri)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            },
            onDeleteClick = { chat, _ ->
                deleteContact(chat)
            },
            onEditClick = { chat, _ ->
                showEditContactDialog(chat)
            }
        )

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val isIslandVisible by isIslandVisibleState
            val isDialogVisible by isContactDialogVisible
            val isEditVisible by isEditDialogVisible
            
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        val view = layoutInflater.inflate(R.layout.activity_chat_list_content, null)
                        val contentBinding = ActivityChatListContentBinding.bind(view)
                        binding = contentBinding
                        
                        binding.recyclerViewChats.layoutManager = LinearLayoutManager(context)
                        binding.recyclerViewChats.adapter = adapter
                        
                        binding.btnStartChatEmpty.setOnClickListener {
                            isContactDialogVisible.value = true
                        }
                        
                        setupSwipeToDelete()
                        isContentBindingReady.value = true
                        view
                    },
                    update = {
                        updateEmptyState()
                        if (isContentBindingReady.value && isIslandBindingReady.value) {
                            setupLegacyListeners()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = isDialogVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = true, onClick = { 
                                isContactDialogVisible.value = false 
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        var contactName by remember { mutableStateOf("") }
                        var isOnline by remember { mutableStateOf(true) }
                        var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
                        
                        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                            selectedAvatarUri = uri
                        }

                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 24.dp,
                                        noiseFactor = 0.05f,
                                        tint = dev.chrisbanes.haze.HazeTint(Color(0xFF154B87).copy(alpha = 0.6f))
                                    )
                                )
                                .clickable(enabled = true) { 
                                    focusManager.clearFocus() 
                                }
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(
                                text = "Тестовое окно",
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarUri != null) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply {
                                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                            }
                                        },
                                        update = { it.setImageURI(selectedAvatarUri) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    androidx.compose.material3.Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.fillMaxSize().padding(12.dp)
                                    )
                                }
                            }
                            androidx.compose.material3.Text(
                                text = if (selectedAvatarUri == null) "Выбрать фото" else "Изменить фото",
                                color = Color.White.copy(alpha = 0.7f),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            androidx.compose.material3.OutlinedTextField(
                                value = contactName,
                                onValueChange = { if (it.length <= 16) contactName = it },
                                label = { androidx.compose.material3.Text("Как зовут контакта", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Text(
                                        text = if (isOnline) "В сети" else "Не в сети",
                                        color = Color.White,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = isOnline,
                                    onCheckedChange = { isOnline = it },
                                    thumbContent = null,
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF154B87),
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                        checkedIconColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            androidx.compose.material3.Button(
                                onClick = {
                                    if (contactName.isNotBlank() && isDialogVisible) {
                                        isContactDialogVisible.value = false
                                        val newChat = ChatModel(
                                            id = System.currentTimeMillis().toString(),
                                            name = contactName,
                                            lastMessage = "Новый контакт создан",
                                            time = "сейчас",
                                            avatarUri = selectedAvatarUri?.toString(),
                                            onlineStatus = if (isOnline) OnlineStatus.ONLINE else OnlineStatus.OFFLINE
                                        )
                                        chatListState.add(0, newChat)
                                        allChats = ArrayList(chatListState)
                                        adapter.updateList(allChats)
                                        saveContacts()
                                        updateEmptyState()
                                        val intent = Intent(this@ChatListActivity, ChatPersonActivity::class.java).apply {
                                            putExtra("EXTRA_CHAT_ID", newChat.id)
                                            putExtra("EXTRA_CHAT_NAME", newChat.name)
                                            putExtra("EXTRA_CHAT_AVATAR", newChat.avatarUri)
                                        }
                                        startActivity(intent)
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF154B87)
                                )
                            ) {
                                androidx.compose.material3.Text("Готово", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }

                // Диалоговое окно РЕДАКТИРОВАНИЯ контакта
                AnimatedVisibility(
                    visible = isEditVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = true, onClick = { 
                                isEditDialogVisible.value = false 
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentContact = contactToEdit ?: return@Box
                        
                        var contactName by remember(currentContact.id) { mutableStateOf(currentContact.name) }
                        var selectedAvatarUri by remember(currentContact.id) { 
                            mutableStateOf(currentContact.avatarUri?.let { Uri.parse(it) }) 
                        }
                        
                        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                            selectedAvatarUri = uri
                        }

                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 24.dp,
                                        noiseFactor = 0.05f,
                                        tint = dev.chrisbanes.haze.HazeTint(Color(0xFF154B87).copy(alpha = 0.6f))
                                    )
                                )
                                .clickable(enabled = true) { 
                                    focusManager.clearFocus() 
                                }
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(
                                text = "Редактировать",
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarUri != null) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply {
                                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                            }
                                        },
                                        update = { it.setImageURI(selectedAvatarUri) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    androidx.compose.material3.Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.fillMaxSize().padding(12.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            androidx.compose.material3.OutlinedTextField(
                                value = contactName,
                                onValueChange = { if (it.length <= 16) contactName = it },
                                label = { androidx.compose.material3.Text("Имя", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            androidx.compose.material3.Button(
                                onClick = {
                                    if (contactName.isNotBlank()) {
                                        isEditDialogVisible.value = false
                                        
                                        // Находим контакт в списке и обновляем его
                                        val index = chatListState.indexOfFirst { it.id == currentContact.id }
                                        if (index != -1) {
                                            val updated = chatListState[index].copy(
                                                name = contactName,
                                                avatarUri = selectedAvatarUri?.toString()
                                            )
                                            chatListState[index] = updated
                                            allChats = ArrayList(chatListState)
                                            
                                            if (adapter.isSearchActive) {
                                                val query = islandBinding.etSearch.text.toString().trim().lowercase()
                                                val filtered = allChats.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
                                                adapter.updateList(filtered)
                                            } else {
                                                adapter.updateList(allChats)
                                            }
                                            saveContacts()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF154B87)
                                )
                            ) {
                                androidx.compose.material3.Text("Сохранить", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 20.dp,
                                noiseFactor = 0f,
                                tint = dev.chrisbanes.haze.HazeTint(Color(0x0DFFFFFF))
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    AnimatedVisibility(
                        visible = isIslandVisible && !isDialogVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 24.dp,
                                        noiseFactor = 0.05f,
                                        tint = dev.chrisbanes.haze.HazeTint(Color(0xFF154B87).copy(alpha = 0.6f))
                                    )
                                )
                        ) {
                        AndroidView(
                            factory = { context ->
                                val view = layoutInflater.inflate(R.layout.layout_island, null)
                                val islandBind = LayoutIslandBinding.bind(view)
                                islandBinding = islandBind
                                updateToolbarInitialUi(savedAvatarUri, savedName)
                                isIslandBindingReady.value = true
                                view
                            },
                            update = {
                                if (isContentBindingReady.value && isIslandBindingReady.value) {
                                    setupLegacyListeners()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        }
                    }
                }
            }
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkConnected = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback)
        showScrollTopHintOnce(sharedPrefs)
        onBackPressedDispatcher.addCallback(this, backCallback)
        backCallback.isEnabled = false
    }

    private fun setupLegacyListeners() {
        if (!::islandBinding.isInitialized || !::binding.isInitialized) return
        
        islandBinding.etSearch.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !islandBinding.etSearch.text.isNullOrEmpty()) {
                animateShowSearchClear()
            } else {
                animateHideSearchClear()
            }
        }

        islandBinding.btnSearchClear.setOnClickListener {
            animateHideSearchClear()
            islandBinding.etSearch.text?.clear()
            hideKeyboardAndClearFocus()
        }

        binding.recyclerViewChats.clearOnScrollListeners()
        binding.recyclerViewChats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                if (adapter.isSearchActive) { showIsland(); return }
                if (firstVisiblePos == 0) hideIsland() else showIsland()
            }
        })

        islandBinding.ivToolbarAvatar.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        islandBinding.tvToolbarInitials.setOnClickListener {
            islandBinding.ivToolbarAvatar.performClick()
        }

        islandBinding.ivToolbarAvatar.setOnLongClickListener {
            binding.recyclerViewChats.smoothScrollToPosition(0)
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }
        islandBinding.tvToolbarInitials.setOnLongClickListener {
            islandBinding.ivToolbarAvatar.performLongClick()
            true
        }

        islandBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.toString() == " ") { islandBinding.etSearch.text?.clear(); hideKeyboardAndClearFocus() }
                if (!s.isNullOrEmpty()) {
                    animateShowSearchClear()
                } else if (!islandBinding.etSearch.hasFocus()) {
                    animateHideSearchClear()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                if (query == "/blocktestme") {
                    adapter.setSearchActive(true)
                    backCallback.isEnabled = true
                    showIsland()
                    val testContact = ChatModel("block_test_contact", "Тестирование активити блока", "Нажмите, чтобы протестировать", "сейчас", null, OnlineStatus.ONLINE)
                    adapter.updateList(listOf(testContact))
                    return
                }

                if (query.isEmpty()) {
                    adapter.setSearchActive(false)
                    backCallback.isEnabled = false
                    val mainList = ArrayList(chatListState)
                    allChats = mainList
                    adapter.updateList(mainList)
                    val layoutManager = binding.recyclerViewChats.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) hideIsland()
                } else {
                    adapter.setSearchActive(true)
                    backCallback.isEnabled = true
                    showIsland()
                    val filtered = chatListState.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
                    adapter.updateList(filtered)
                }
            }
        })
    }

    private fun updateToolbarInitialUi(avatarUri: String?, name: String) {
        if (!::islandBinding.isInitialized) return
        if (avatarUri != null) {
            islandBinding.ivToolbarAvatar.setImageURI(Uri.parse(avatarUri))
            islandBinding.tvToolbarInitials.visibility = View.GONE
            islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
        } else {
            val initial = name.take(1).uppercase()
            islandBinding.tvToolbarInitials.text = initial
            islandBinding.tvToolbarInitials.visibility = View.VISIBLE
            islandBinding.ivToolbarAvatar.visibility = View.INVISIBLE
            val color = getAvatarColor(name)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 15 * resources.displayMetrics.density
                setColor(color)
            }
            islandBinding.tvToolbarInitials.setBackground(bg)
        }
    }

    private fun calculateMillis(value: Long, unit: String): Long {
        val seconds = when (unit.uppercase()) {
            "S", "SECOND" -> value
            "M", "MIN", "MINUTE" -> value * 60
            "H", "HOUR" -> value * 3600
            "D", "DAYS" -> value * 86400
            "MO", "MOUNTH" -> value * 2592000
            "Y", "YEAR" -> value * 31536000
            else -> value
        }
        return seconds * 1000
    }

    private fun showScrollTopHintOnce(sharedPrefs: android.content.SharedPreferences) {
        if (!sharedPrefs.getBoolean("hint_scroll_top_shown", false)) {
            PrimeNotification.show(this, "Зажмите, для подтягивание к верху экрана")
            sharedPrefs.edit().putBoolean("hint_scroll_top_shown", true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserUi()
        isPulling = false; isThresholdCrossed = false
    }

    private fun refreshUserUi() {
        if (!::binding.isInitialized || !::islandBinding.isInitialized) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val avatar = sharedPrefs.getString("${currentUser}_avatar", null)
        val name = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        if (avatar != null) {
            islandBinding.ivToolbarAvatar.setImageURI(null)
            islandBinding.ivToolbarAvatar.setImageURI(Uri.parse(avatar))
            islandBinding.tvToolbarInitials.visibility = View.GONE
            islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
            adapter.updateAvatar(avatar)
        } else {
            val initial = name.take(1).uppercase()
            islandBinding.tvToolbarInitials.text = initial
            islandBinding.tvToolbarInitials.visibility = View.VISIBLE
            islandBinding.ivToolbarAvatar.visibility = View.INVISIBLE
            val color = getAvatarColor(name)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 15 * resources.displayMetrics.density
                setColor(color)
            }
            islandBinding.tvToolbarInitials.setBackground(bg)
            adapter.updateAvatar(null)
        }
        adapter.updateUserName(name)
        binding.recyclerViewChats.post {
            val layoutManager = binding.recyclerViewChats.layoutManager as? LinearLayoutManager
            if (layoutManager != null) {
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first == 0 && !adapter.isSearchActive) hideIsland() else showIsland()
            }
        }
    }

    private fun showLogoutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setTitle("Выход")
            .setMessage("Сделать выход из аккаунта?")
            .setPositiveButton("Да") { _, _ ->
                getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).edit()
                    .putBoolean("is_logged_in", false)
                    .apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun deleteContact(contact: ChatModel) {
        val index = chatListState.indexOfFirst { it.id == contact.id }
        if (index == -1) return
        chatListState.removeAt(index)
        allChats = ArrayList(chatListState)
        val adapterPos = if (adapter.isSearchActive) index else index + 1
        if (adapter.isSearchActive) {
            val query = islandBinding.etSearch.text.toString().trim().lowercase()
            val filtered = allChats.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
            adapter.updateList(filtered, notify = true)
        } else {
            adapter.updateList(allChats, notify = false)
            adapter.notifyItemRemoved(adapterPos)
            if (allChats.isEmpty()) adapter.notifyItemChanged(1)
        }
        saveContacts()
        updateEmptyState()
        binding.recyclerViewChats.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private val isEditDialogVisible = mutableStateOf(false)
    private var contactToEdit: ChatModel? = null

    private fun showEditContactDialog(chat: ChatModel) {
        contactToEdit = chat
        isEditDialogVisible.value = true
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.itemViewType != 0) return 0 
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder is ChatAdapter.ChatViewHolder) {
                    if (viewHolder.isRevealed) viewHolder.resetReveal()
                }
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val actualIndex = if (!adapter.isSearchActive) position - 1 else position
                val currentList = adapter.getChatList()
                if (actualIndex >= 0 && actualIndex < currentList.size) {
                    deleteContact(currentList[actualIndex])
                } else {
                    adapter.notifyItemChanged(position)
                }
            }
            override fun onChildDraw(c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                
                if (dX < 0f) {
                    val paint = android.graphics.Paint()
                    val cornerRadius = 24.dpToPx()
                    
                    // При свайпе показываем только красный фон (удаление)
                    paint.color = androidx.core.content.ContextCompat.getColor(this@ChatListActivity, R.color.prime_danger)
                    val background = android.graphics.RectF(itemView.right.toFloat() + dX, itemView.top.toFloat() + 6.dpToPx(), itemView.right.toFloat(), itemView.bottom.toFloat() - 6.dpToPx())
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint)

                    // Иконка корзины (всегда справа)
                    val icon = androidx.core.content.ContextCompat.getDrawable(this@ChatListActivity, R.drawable.ic_cancel)
                    icon?.let {
                        val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconRight = itemView.right - iconMargin.toInt()
                        val iconLeft = iconRight - it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(android.graphics.Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewChats)
    }

    private fun Int.dpToPx(): Float = (this * resources.displayMetrics.density)
    private fun updateEmptyState() {
        if (!::binding.isInitialized) return
        runOnUiThread { binding.layoutEmptyState.visibility = if (chatListState.isEmpty()) View.VISIBLE else View.GONE }
    }
    private fun saveContacts() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val array = JSONArray()
        chatListState.forEach { chat ->
            val obj = JSONObject().apply {
                put("id", chat.id)
                put("name", chat.name)
                put("lastMessage", chat.lastMessage)
                put("time", chat.time)
                put("avatarUri", chat.avatarUri)
                put("onlineStatus", chat.onlineStatus.name)
                put("messageStatus", chat.messageStatus.name)
                put("unreadCount", chat.unreadCount)
                put("isMuted", chat.isMuted)
            }
            array.put(obj)
        }
        sharedPrefs.edit().putString("persisted_chats", array.toString()).apply()
    }
    private fun loadContacts() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("persisted_chats", null)
        if (json != null && json.isNotEmpty() && json != "[]") {
            try {
                val array = JSONArray(json)
                val tempList = ArrayList<ChatModel>()
                for (i in 0 until array.length()) {
                    try {
                        val item = array.opt(i)
                        if (item is JSONObject) {
                            val obj = item
                            val chat = ChatModel(
                                obj.optString("id", System.currentTimeMillis().toString() + i),
                                obj.optString("name", "Контакт"),
                                obj.optString("lastMessage", ""),
                                obj.optString("time", "сейчас"),
                                if (obj.isNull("avatarUri")) null else obj.optString("avatarUri"),
                                try { OnlineStatus.valueOf(obj.optString("onlineStatus", "OFFLINE")) } catch(e: Exception) { OnlineStatus.OFFLINE },
                                try { MessageStatus.valueOf(obj.optString("messageStatus", "NONE")) } catch(e: Exception) { MessageStatus.NONE },
                                obj.optInt("unreadCount", 0),
                                obj.optBoolean("isMuted", false)
                            )
                            tempList.add(chat)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                chatListState.clear(); chatListState.addAll(tempList)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    private fun showIsland() { isIslandVisibleState.value = true }
    private fun hideIsland() { isIslandVisibleState.value = false }
    private fun activateIslandSearch() {
        if (!::islandBinding.isInitialized) return
        showIsland()
        islandBinding.etSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(islandBinding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun showNameEditDialog() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        
        // Используем ViewBinding для инфлейта диалога
        val dialogBinding = com.messenger.prime.databinding.DialogEditNameBinding.inflate(layoutInflater)
        
        // Находим контейнер в activity_chat_list_content.xml
        val container = binding.root.findViewById<android.widget.FrameLayout>(R.id.dialogContainer)
        if (container == null) {
            // Если контейнер не найден в binding, попробуем найти его напрямую в activity
            findViewById<android.widget.FrameLayout>(R.id.dialogContainer)?.let {
                setupDialogInContainer(it, dialogBinding, sharedPrefs, currentUser, currentName)
            }
            return
        }
        
        setupDialogInContainer(container, dialogBinding, sharedPrefs, currentUser, currentName)
    }

    private fun setupDialogInContainer(
        container: android.widget.FrameLayout,
        dialogBinding: com.messenger.prime.databinding.DialogEditNameBinding,
        sharedPrefs: android.content.SharedPreferences,
        currentUser: String,
        currentName: String
    ) {
        container.removeAllViews()
        container.addView(dialogBinding.root)
        container.visibility = View.VISIBLE

        // Центрируем карточку и позволяем ей использовать размеры из XML
        val cardParams = dialogBinding.cardContainer.layoutParams as android.widget.FrameLayout.LayoutParams
        cardParams.gravity = android.view.Gravity.CENTER
        dialogBinding.cardContainer.layoutParams = cardParams

        dialogBinding.hazeView.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            tint = dev.chrisbanes.haze.HazeTint(Color(0xFF154B87).copy(alpha = 0.6f)),
                            blurRadius = 24.dp,
                            noiseFactor = 0.05f
                        )
                    )
            )
        }

        dialogBinding.etNewName.setText(currentName)
        dialogBinding.etNewName.setSelection(currentName.length)
        
        dialogBinding.cardContainer.scaleX = 0.8f
        dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f
        
        dialogBinding.dialogRoot.alpha = 0f
        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        dialogBinding.btnSave.setOnClickListener {
            val newName = dialogBinding.etNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                sharedPrefs.edit().putString("${currentUser}_name", newName).apply()
                refreshUserUi()
                hideNameEditDialog(dialogBinding, container)
            }
        }
        
        dialogBinding.btnBack.setOnClickListener { hideNameEditDialog(dialogBinding, container) }
        dialogBinding.dialogRoot.setOnClickListener { hideNameEditDialog(dialogBinding, container) }
        
        dialogBinding.etNewName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.toString() == "\n") { dialogBinding.btnSave.performClick() }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun hideNameEditDialog(db: com.messenger.prime.databinding.DialogEditNameBinding, container: android.widget.FrameLayout) {
        db.dialogRoot.animate().alpha(0f).setDuration(300).start()
        db.cardContainer.animate().scaleX(0.8f).scaleY(0.8f).alpha(0f).setDuration(300).withEndAction {
            container.visibility = View.GONE
            container.removeAllViews()
        }.start()
    }
    private fun animateShowSearchClear() {
        if (!::islandBinding.isInitialized) return
        if (islandBinding.btnSearchClear.visibility == View.VISIBLE && islandBinding.btnSearchClear.alpha == 1f) return
        islandBinding.btnSearchClear.visibility = View.VISIBLE; islandBinding.btnSearchClear.alpha = 0f
        islandBinding.btnSearchClear.translationY = 50f * resources.displayMetrics.density
        islandBinding.btnSearchClear.animate().translationY(0f).alpha(1f).setDuration(400).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
    private fun animateHideSearchClear() {
        if (!::islandBinding.isInitialized) return
        if (islandBinding.btnSearchClear.visibility != View.VISIBLE) return
        islandBinding.btnSearchClear.animate().translationY(50f * resources.displayMetrics.density).alpha(0f).setDuration(300).setInterpolator(android.view.animation.AccelerateInterpolator()).withEndAction {
            islandBinding.btnSearchClear.visibility = View.INVISIBLE; islandBinding.btnSearchClear.translationY = 0f
        }.start()
    }
    private fun hideKeyboardAndClearFocus() {
        if (!::islandBinding.isInitialized) return
        islandBinding.etSearch.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(islandBinding.etSearch.windowToken, 0)
        if (islandBinding.etSearch.text.isNullOrEmpty()) {
            adapter.setSearchActive(false); backCallback.isEnabled = false
            if (!binding.recyclerViewChats.canScrollVertically(-1)) hideIsland()
            animateHideSearchClear()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener)
    }
    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT < 34) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    private fun getAvatarColor(name: String): Int {
        val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
        val hash = name.hashCode()
        val index = (if (hash == Int.MIN_VALUE) 0 else Math.abs(hash)) % colors.size
        return android.graphics.Color.parseColor(colors[index])
    }
}
