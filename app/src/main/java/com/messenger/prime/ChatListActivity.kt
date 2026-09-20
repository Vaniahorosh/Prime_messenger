package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.location.LocationManager
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
import java.io.File
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.messenger.prime.databinding.ActivityChatListContentBinding
import com.messenger.prime.databinding.LayoutIslandBinding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.*
import java.util.UUID

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListContentBinding
    private lateinit var islandBinding: LayoutIslandBinding
    private lateinit var adapter: ChatListAdapter
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

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isReceiverRegistered = false
    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    private val pairedDevices = mutableStateListOf<BluetoothDevice>()
    private val isScanningState = mutableStateOf(false)

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            checkAndRequestPermissions()
        } else {
            PrimeNotification.show(this, "Необходимо включить Bluetooth для поиска")
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this, R.style.Theme_Prime_AlertDialog)
            .setTitle("Требуется доступ к Bluetooth")
            .setMessage("Для поиска собеседников поблизости приложению необходим доступ к Bluetooth. Пожалуйста, включите его в настройках.")
            .setPositiveButton("Открыть настройки") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val requiredPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        val allGranted = requiredPerms.all { 
            permissions[it] == true || ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (allGranted) {
            isContactDialogVisible.value = true
        } else {
            val permanentlyDenied = requiredPerms.any { 
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED && 
                !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }
            if (permanentlyDenied) {
                showSettingsDialog()
            } else {
                PrimeNotification.show(this, "Необходимы разрешения для поиска")
            }
        }
    }

    private fun onStartChatClicked() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            PrimeNotification.show(this, "Bluetooth не поддерживается")
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val requiredPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        val missingPerms = requiredPerms.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPerms.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(requiredPerms)
        } else {
            isContactDialogVisible.value = true
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        if (!discoveredDevices.any { it.address == device.address }) {
                            discoveredDevices.add(device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanningState.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanningState.value = false
                }
            }
        }
    }

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

        adapter = ChatListAdapter(
            allChats, savedAvatarUri, savedName,
            onStartChatClick = { onStartChatClicked() },
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
                    try { bluetoothAdapter?.cancelDiscovery() } catch (e: Exception) {}
                    val intent = Intent(this, ChatPersonActivity::class.java)
                    intent.putExtra("EXTRA_CHAT_NAME", chat.name)
                    intent.putExtra("EXTRA_DEVICE_ADDRESS", chat.id)
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
            val isNameEditVisible by isNameEditDialogVisible
            
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        val view = layoutInflater.inflate(R.layout.activity_chat_list_content, null)
                        val contentBinding = ActivityChatListContentBinding.bind(view)
                        binding = contentBinding
                        
                        binding.recyclerViewChats.layoutManager = LinearLayoutManager(context)
                        binding.recyclerViewChats.adapter = adapter
                        
                        binding.btnStartChatEmpty.setOnClickListener {
                            onStartChatClicked()
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
                    modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)
                )

                LaunchedEffect(isDialogVisible) {
                    if (isDialogVisible) {
                        startBluetoothScan()
                    } else {
                        stopBluetoothScan()
                    }
                }

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
                                .clickable(enabled = true) {} // prevent closing on inner click
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Поиск собеседников",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isScanningState.value) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Ищем пользователей Prime...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            } else {
                                Button(
                                    onClick = { startBluetoothScan() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF154B87)
                                    )
                                ) {
                                    Text("Повторить поиск", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { 
                                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                    }
                                    startActivity(discoverableIntent)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33FFFFFF),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text("Сделать меня видимым (5 мин)", fontWeight = FontWeight.Bold)
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 300.dp)
                            ) {
                                val allDevices = pairedDevices.map { it to true } + discoveredDevices.map { it to false }
                                
                                items(allDevices) { (device, isPaired) ->
                                    val devName = try { device.name ?: "Неизвестное устройство" } catch (e: SecurityException) { "Неизвестное устройство" }
                                    val devMac = device.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                stopBluetoothScan()
                                                isContactDialogVisible.value = false

                                                val intent = Intent(this@ChatListActivity, ChatPersonActivity::class.java).apply {
                                                    putExtra("EXTRA_CHAT_NAME", devName)
                                                    putExtra("EXTRA_DEVICE_ADDRESS", devMac)
                                                }
                                                startActivity(intent)
                                                if (Build.VERSION.SDK_INT < 34) {
                                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha=0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_person),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.fillMaxSize().padding(12.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = devName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(text = devMac, color = Color.White.copy(alpha=0.5f), fontSize = 12.sp)
                                            if (isPaired) {
                                                Text(text = "Сопряжено", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (discoveredDevices.isEmpty() && pairedDevices.isEmpty() && !isScanningState.value) {
                                Text(
                                    text = "Устройства не найдены",
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(16.dp)
                                )
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

                AnimatedVisibility(
                    visible = isNameEditVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = true, onClick = {
                                isNameEditDialogVisible.value = false
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
                        
                        var nameInput by remember { mutableStateOf(currentName) }
                        val focusManager = LocalFocusManager.current

                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 24.dp,
                                        noiseFactor = 0.05f,
                                        tint = HazeTint(Color(0xFF154B87).copy(alpha = 0.6f))
                                    )
                                )
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(32.dp))
                                .clickable(enabled = true) {
                                    focusManager.clearFocus()
                                }
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Изменить имя",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { if (it.length <= 16) nameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                    }
                                ),
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                label = { Text("Новое имя") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { isNameEditDialogVisible.value = false },
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0x40FFFFFF))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_back),
                                        contentDescription = "Назад",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        if (nameInput.isNotBlank()) {
                                            sharedPrefs.edit().putString("${currentUser}_name", nameInput.trim()).apply()
                                            runOnUiThread { refreshUserUi() }
                                            isNameEditDialogVisible.value = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF154B87)
                                    )
                                ) {
                                    Text(
                                        text = "Сохранить",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
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
        var loaded = false
        if (!avatarUri.isNullOrEmpty()) {
            val uri = Uri.parse(avatarUri)
            val file = if (uri.scheme == "file") File(uri.path ?: "") else null
            if (file == null || file.exists()) {
                try {
                    islandBinding.ivToolbarAvatar.setImageURI(uri)
                    islandBinding.tvToolbarInitials.visibility = View.GONE
                    islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
                    loaded = true
                } catch (e: Exception) {
                    loaded = false
                }
            }
        }
        if (!loaded) {
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
        var loaded = false
        if (!avatar.isNullOrEmpty()) {
            val uri = Uri.parse(avatar)
            val file = if (uri.scheme == "file") File(uri.path ?: "") else null
            if (file == null || file.exists()) {
                try {
                    islandBinding.ivToolbarAvatar.setImageURI(null)
                    islandBinding.ivToolbarAvatar.setImageURI(uri)
                    islandBinding.tvToolbarInitials.visibility = View.GONE
                    islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
                    adapter.updateAvatar(avatar)
                    loaded = true
                } catch (e: Exception) {
                    loaded = false
                }
            }
        }
        if (!loaded) {
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
    private val isNameEditDialogVisible = mutableStateOf(false)
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
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder is ChatListAdapter.ChatViewHolder) {
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
        isNameEditDialogVisible.value = true
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
    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return
        
        // Сразу загружаем сопряженные устройства
        pairedDevices.clear()
        bluetoothAdapter?.bondedDevices?.let { pairedDevices.addAll(it) }

        discoveredDevices.clear()
        isScanningState.value = true
        
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            ContextCompat.registerReceiver(this, bluetoothReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            isReceiverRegistered = true
        }
        
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                PrimeNotification.show(this, "Включите геолокацию в шторке для поиска устройств поблизости")
            }
        }
        
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun stopBluetoothScan() {
        isScanningState.value = false
        try { bluetoothAdapter?.cancelDiscovery() } catch (e: Exception) {}
        
        if (isReceiverRegistered) {
            try { unregisterReceiver(bluetoothReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBluetoothScan()
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
