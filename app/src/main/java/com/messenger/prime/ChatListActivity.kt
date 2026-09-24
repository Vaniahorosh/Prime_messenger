package com.messenger.prime

import android.Manifest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.messenger.prime.databinding.ActivityChatListContentBinding
import com.messenger.prime.databinding.LayoutIslandBinding

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.graphics.toColorInt
import kotlin.math.abs

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .border(2.dp, Color(0xFF00E676), CircleShape)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_prime_statusbar),
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(36.dp)
        )
    }
}



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
    private val typingExpireHandler = Handler(Looper.getMainLooper())
    private val typingExpireRunnable = Runnable { reloadChatsFromDb() }


    private var startY = 0f
    private var isPulling = false
    private var isThresholdCrossed = false
    private val pullThreshold = 350f
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

    private val enableDiscoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        performDiscovery()
    }

    private fun getRequiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
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

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Log.w("ChatListActivity", "POST_NOTIFICATIONS permission denied")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val requiredPerms = getRequiredBluetoothPermissions()
        
        val allGranted = requiredPerms.all { perms ->
            permissions[perms] == true || ActivityCompat.checkSelfPermission(this, perms) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (allGranted) {
            isContactDialogVisible.value = true
        } else {
            val permanentlyDenied = requiredPerms.any { perms ->
                ActivityCompat.checkSelfPermission(this, perms) != PackageManager.PERMISSION_GRANTED && 
                !ActivityCompat.shouldShowRequestPermissionRationale(this, perms)
            }
            if (permanentlyDenied) {
                showSettingsDialog()
            } else {
                PrimeNotification.show(this, "Необходимы разрешения для поиска устройств")
            }
        }
    }

    private fun onStartChatClicked() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
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
        val requiredPerms = getRequiredBluetoothPermissions()
        val missingPerms = requiredPerms.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPerms.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(requiredPerms)
        } else {
            isContactDialogVisible.value = true
        }
    }

    private val primeUuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val primeDevices = mutableStateSetOf<String>()
    private var lastChatLaunchTime = 0L

    @Synchronized
    private fun navigateToChatPerson(
        targetName: String,
        deviceAddress: String,
        useExistingSocket: Boolean = false
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastChatLaunchTime < 1000L) {
            Log.d("ChatListActivity", "navigateToChatPerson ignored due to debounce")
            return
        }
        lastChatLaunchTime = now

        runOnUiThread {
            stopBluetoothScan()
            isContactDialogVisible.value = false

            val chatIntent = Intent(this@ChatListActivity, ChatPersonActivity::class.java).apply {
                putExtra("EXTRA_CHAT_NAME", targetName)
                putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
                if (useExistingSocket) {
                    putExtra("EXTRA_USE_EXISTING_SOCKET", true)
                }
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(chatIntent)
            if (Build.VERSION.SDK_INT < 34) {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerPrimeFoundVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                        val devName = try { device.name } catch (_: Exception) { null }
                        if (devName?.contains("Prime", ignoreCase = true) == true) {
                            if (primeDevices.add(device.address)) {
                                triggerPrimeFoundVibration()
                                val devName = try { device.name } catch (_: Exception) { null }
                                if (devName != null) {
                                    PrimeNotification.show(this@ChatListActivity, "⚡ Найден Prime-пользователь: $devName!")
                                } else {
                                    PrimeNotification.show(this@ChatListActivity, "⚡ Найден Prime-пользователь!")
                                }
                            }
                        }

                        val cachedUuids = try { device.uuids } catch (_: Exception) { null }
                        if (cachedUuids != null) {
                            for (uuid in cachedUuids) {
                                if (uuid.uuid.toString().equals(primeUuid.toString(), ignoreCase = true)) {
                                    if (primeDevices.add(device.address)) {
                                        val nameToShow = devName ?: "Prime Собеседник"
                                        triggerPrimeFoundVibration()
                                        PrimeNotification.show(this@ChatListActivity, "⚡ Найден Prime-пользователь: $nameToShow!")
                                    }
                                    break
                                }
                            }
                        }

                        try {
                            device.fetchUuidsWithSdp()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                BluetoothDevice.ACTION_UUID -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val extraUuids = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, ParcelUuid::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                        }
                    } catch (e: Exception) { null }

                    if (device != null) {
                        val uuidList = mutableListOf<String>()
                        extraUuids?.forEach { uuidList.add(it.toString()) }
                        try {
                            device.uuids?.forEach { uuidList.add(it.uuid.toString()) }
                        } catch (e: Exception) {}

                        if (uuidList.any { it.equals(primeUuid.toString(), ignoreCase = true) }) {
                            if (primeDevices.add(device.address)) {
                                val devName = try { device.name ?: "Prime Собеседник" } catch(e: Exception) { "Prime Собеседник" }
                                triggerPrimeFoundVibration()
                                PrimeNotification.show(this@ChatListActivity, "⚡ Найден Prime-пользователь: $devName!")
                            }
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

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "current_user" || key?.endsWith("_name") == true || key?.endsWith("_avatar") == true) {
            runOnUiThread { refreshUserUi() }
        } else if (key == "persisted_chats") {
            runOnUiThread { reloadChatsFromDb() }
        }
    }



    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isTransitioning) return true
        if (!::binding.isInitialized) return super.dispatchTouchEvent(event)

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
                    val progress = (dy / pullThreshold).coerceIn(0f, 1.2f)
                    
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
                        if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
                        
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
        if (!::binding.isInitialized) return
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
        if (!::binding.isInitialized) return
        binding.recyclerViewChats.translationY = 0f
        isPulling = false
        isThresholdCrossed = false
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isContactDialogVisible.value) {
                isContactDialogVisible.value = false
                stopBluetoothScan()
                return
            }
            if (adapter.isSearchActive) {
                if (::islandBinding.isInitialized && islandBinding.etSearch.hasFocus()) {
                    hideKeyboardAndClearFocus()
                } else if (::islandBinding.isInitialized) {
                    islandBinding.etSearch.text?.clear()
                    hideKeyboardAndClearFocus()
                }
                return
            }
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
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

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        checkNotificationPermission()

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
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
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
                            if (android.os.Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
                        }
                    }
                } else {
                    try { 
                        @Suppress("MissingPermission")
                        bluetoothAdapter?.cancelDiscovery() 
                    } catch (e: Exception) {}
                    navigateToChatPerson(chat.name, chat.id)
                }
            },
            onDeleteClick = { chat, _ ->
                deleteContact(chat)
            }
        )

        setContent {
            val isIslandVisible by isIslandVisibleState
            val isDialogVisible by isContactDialogVisible
            val isNameEditVisible by isNameEditDialogVisible
            
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val density = LocalDensity.current
            val topInsetPx = with(density) { systemBarsPadding.calculateTopPadding().roundToPx() }
            val bottomInsetPx = with(density) { systemBarsPadding.calculateBottomPadding().roundToPx() }
            
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
                    update = { _ ->
                        binding.recyclerViewChats.setPadding(
                            binding.recyclerViewChats.paddingLeft,
                            topInsetPx + (8 * resources.displayMetrics.density).toInt(),
                            binding.recyclerViewChats.paddingRight,
                            bottomInsetPx + (100 * resources.displayMetrics.density).toInt()
                        )
                        updateEmptyState()
                        if (isContentBindingReady.value && isIslandBindingReady.value) {
                            setupLegacyListeners()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                LaunchedEffect(isDialogVisible) {
                    if (isDialogVisible) {
                        startUnifiedSearchAndDiscoverable()
                    } else {
                        stopBluetoothScan()
                    }
                }

                if (isDialogVisible) {
                    Dialog(
                        onDismissRequest = { isContactDialogVisible.value = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        val view = LocalView.current
                        DisposableEffect(Unit) {
                            val window = (view.parent as? DialogWindowProvider)?.window
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                                try {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                                    val params = window.attributes
                                    params.blurBehindRadius = 60
                                    window.attributes = params
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                            onDispose {}
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isContactDialogVisible.value = false },
                            contentAlignment = Alignment.Center
                        ) {
                        BlurView(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .clickable(enabled = true) {}, // prevent closing on inner click
                            blurRadius = 20.dp,
                            tint = Color(0x33154B87),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                            val myVerCode = try { ChatPersonActivity.getAppVersionCode(this@ChatListActivity) } catch (e: Exception) { 1 }

                            Text(
                                text = "Поиск Prime-собеседников",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Приложение: Prime v1.0 (код версии: $myVerCode) • ID: com.messenger.prime",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val rawAllDevices = (pairedDevices.map { it to true } + discoveredDevices.map { it to false })
                                .distinctBy { it.first.address }
                                
                            val primeOnlyDevices = rawAllDevices.filter { (device, _) ->
                                val name = try {
                                    @Suppress("MissingPermission")
                                    device.name
                                } catch (e: Exception) { null }

                                val devUuids = try {
                                    @Suppress("MissingPermission")
                                    device.uuids
                                } catch (e: Exception) { null }

                                val hasPrimeUuid = devUuids?.any { it?.uuid?.toString().equals(primeUuid.toString(), ignoreCase = true) } == true
                                val isExplicitPrime = (name?.contains("Prime", ignoreCase = true) == true) || 
                                                      primeDevices.contains(device.address) || 
                                                      hasPrimeUuid

                                isExplicitPrime
                            }

                            val allDevices = primeOnlyDevices

                            if (isScanningState.value) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                    RadarAnimation()
                                    Text(
                                        text = if (allDevices.isEmpty()) "Поиск Prime-пользователей поблизости..." else "Найдено Prime: ${allDevices.size}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { startUnifiedSearchAndDiscoverable() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                                ) {
                                    Text("⚡ Поиск и авто-видимость", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 280.dp)
                            ) {
                                items(allDevices, key = { it.first.address }) { (device, isPaired) ->
                                    val devName = try {
                                        @Suppress("MissingPermission")
                                        device.name ?: "Prime Собеседник"
                                    } catch (_: Exception) { "Prime Собеседник" }
                                    val devMac = device.address

                                    val devUuids = try {
                                        @Suppress("MissingPermission")
                                        device.uuids
                                    } catch (_: Exception) { null }
                                    val hasPrimeUuid = devUuids?.any { it?.uuid?.toString().equals(primeUuid.toString(), ignoreCase = true) } == true
                                    val isPrimeVerified = (devName.contains("Prime", ignoreCase = true)) || 
                                                          primeDevices.contains(devMac) || 
                                                          hasPrimeUuid

@OptIn(ExperimentalFoundationApi::class)
                                    Row(
                                        modifier = Modifier.animateItemPlacement()
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x2200E676))
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF00E676),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF00E676).copy(alpha=0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_prime_statusbar),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.fillMaxSize().padding(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = devName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isPrimeVerified) "⚡ Prime v1.0" else "Доступно",
                                                    color = if (isPrimeVerified) Color(0xFF00E676) else Color(0xFF81D4FA),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isPrimeVerified) Color(0x3300E676) else Color(0x3381D4FA))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isPrimeVerified) "$devMac • Подтвержденный сервис" else "$devMac • Требует проверки",
                                                color = Color.White.copy(alpha=0.6f),
                                                fontSize = 11.sp
                                            )
                                            if (isPaired) {
                                                Text(text = "Сопряжено", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Button(
                                            onClick = {
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF00E676),
                                                contentColor = Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = if (isPrimeVerified) "Подключиться" else "Проверить",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (allDevices.isEmpty() && !isScanningState.value) {
                                Text(
                                    text = "Prime-пользователи не найдены поблизости",
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

                if (isNameEditVisible) {
                    Dialog(
                        onDismissRequest = { isNameEditDialogVisible.value = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        val view = LocalView.current
                        DisposableEffect(Unit) {
                            val window = (view.parent as? DialogWindowProvider)?.window
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                                try {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                                    val params = window.attributes
                                    params.blurBehindRadius = 60
                                    window.attributes = params
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                            onDispose {}
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isNameEditDialogVisible.value = false },
                            contentAlignment = Alignment.Center
                        ) {
                        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
                        
                        var nameInput by remember { mutableStateOf(currentName) }
                        val focusManager = LocalFocusManager.current

                        BlurView(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth()
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(32.dp))
                                .clickable(enabled = true) {
                                    focusManager.clearFocus()
                                },
                            blurRadius = 20.dp,
                            tint = Color(0x33154B87),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
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
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
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
                                            sharedPrefs.edit { putString("${currentUser}_name", nameInput.trim()) }
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
            }
            }

                BlurView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
                    blurRadius = 20.dp,
                    tint = Color(0x0DFFFFFF)
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
                        BlurView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(24.dp)),
                            blurRadius = 20.dp,
                            tint = Color(0x33154B87),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            AndroidView(
                                factory = { _ ->
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
    }

    private fun setupLegacyListeners() {
        if (!::islandBinding.isInitialized || !::binding.isInitialized) return
        if (isLegacyListenersSetup) return
        isLegacyListenersSetup = true
        
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
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
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
            val uri = avatarUri.toUri()
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
            islandBinding.tvToolbarInitials.background = bg
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
            sharedPrefs.edit { putBoolean("hint_scroll_top_shown", true) }
        }
    }

    private val onChatListChanged = {
        runOnUiThread { reloadChatsFromDb() }
    }

    override fun onStart() {
        super.onStart()
        ChatListNotifier.subscribe(onChatListChanged)
        reloadChatsFromDb()
    }

    override fun onStop() {
        super.onStop()
        typingExpireHandler.removeCallbacks(typingExpireRunnable)
        ChatListNotifier.unsubscribe(onChatListChanged)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reloadChatsFromDb()
    }

    override fun onResume() {
        super.onResume()
        refreshUserUi()
        reloadChatsFromDb()
        isPulling = false; isThresholdCrossed = false
        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            if (acceptThread == null || !acceptThread!!.isAlive) {
                startAcceptThread()
            }
        }
    }

    private fun scheduleTypingExpiration() {
        typingExpireHandler.removeCallbacks(typingExpireRunnable)
        val now = System.currentTimeMillis()
        val nextExpiry = chatListState
            .filter { it.typingUntil > now }
            .minOfOrNull { it.typingUntil }
        if (nextExpiry != null) {
            val delay = (nextExpiry - now + 100L).coerceAtLeast(100L)
            typingExpireHandler.postDelayed(typingExpireRunnable, delay)
        }
    }

    private fun reloadChatsFromDb() {
        loadContacts()
        allChats = ArrayList(chatListState)
        if (::adapter.isInitialized) {
            if (adapter.isSearchActive && ::islandBinding.isInitialized) {
                val query = islandBinding.etSearch.text.toString().trim().lowercase()
                if (query.isNotEmpty()) {
                    val filtered = allChats.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
                    adapter.updateList(filtered)
                } else {
                    adapter.updateList(allChats)
                }
            } else {
                adapter.updateList(allChats)
            }
        }
        updateEmptyState()
        scheduleTypingExpiration()
    }

    private fun refreshUserUi() {
        if (!::binding.isInitialized || !::islandBinding.isInitialized) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val avatar = sharedPrefs.getString("${currentUser}_avatar", null)
        val name = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        var loaded = false

        val avatarFile = if (!avatar.isNullOrEmpty()) {
            val uri = avatar.toUri()
            if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else File(filesDir, "avatar_$currentUser.jpg")
        } else {
            File(filesDir, "avatar_$currentUser.jpg")
        }

        if (avatarFile.exists()) {
            try {
                islandBinding.ivToolbarAvatar.setImageURI(null)
                islandBinding.ivToolbarAvatar.setImageURI(Uri.fromFile(avatarFile))
                islandBinding.tvToolbarInitials.visibility = View.GONE
                islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
                adapter.updateAvatar(Uri.fromFile(avatarFile).toString())
                loaded = true
            } catch (e: Exception) {
                e.printStackTrace()
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
            islandBinding.tvToolbarInitials.background = bg
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
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun deleteContact(contact: ChatModel) {
        val index = chatListState.indexOfFirst { it.id == contact.id || it.name.equals(contact.name, ignoreCase = true) }
        if (index == -1) return
        
        val targetName = chatListState[index].name
        val targetId = chatListState[index].id
        chatListState.removeAt(index)
        allChats = ArrayList(chatListState)
        
        if (BluetoothSocketHolder.isConnectedWith(contact.id, contact.name)) {
            try {
                val thread = BluetoothSocketHolder.getConnectedThreadInstance()
                if (thread != null) {
                    val method = thread.javaClass.getMethod("sendPacket", Byte::class.java, ByteArray::class.java)
                    val currentUser = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("current_user", "") ?: ""
                    val myDisplayName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${currentUser}_name", currentUser) ?: currentUser
                    val payload = "DELETE_CHAT:login=$myDisplayName;name=$myDisplayName".toByteArray(Charsets.UTF_8)
                    method.invoke(thread, 8.toByte(), payload) // TYPE_CHAT_DELETED = 0x08
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            BluetoothSocketHolder.clearSocket()
            PrimeBluetoothService.stopService(this)
        }
        
        // Completely forget the device (unpair/removeBond) if it's a Bluetooth MAC address
        try {
            val bManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bAdapter = bManager.adapter
            if (bAdapter != null && bAdapter.isEnabled) {
                val device = bAdapter.getRemoteDevice(contact.id)
                val removeBondMethod = device.javaClass.getMethod("removeBond")
                removeBondMethod.invoke(device)
            }
        } catch (e: Exception) {
            e.printStackTrace() // Ignore if invalid MAC or reflection fails
        }
        
        ChatHistoryManager.deleteHistoryCompletely(this, targetName, targetId)
        saveContacts()
        
        if (::adapter.isInitialized) {
            if (adapter.isSearchActive && ::islandBinding.isInitialized) {
                val query = islandBinding.etSearch.text.toString().trim().lowercase()
                val filtered = allChats.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
                adapter.updateList(filtered, notify = true)
            } else {
                adapter.updateList(allChats, notify = true)
            }
        }
        updateEmptyState()
        binding.recyclerViewChats.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private val isNameEditDialogVisible = mutableStateOf(false)

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
                        val iconRight = itemView.right - iconMargin
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
        runOnUiThread {
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition().setDuration(250))
            binding.layoutEmptyState.visibility = if (chatListState.isEmpty()) View.VISIBLE else View.GONE
        }
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
                put("typingUntil", chat.typingUntil)
                put("activityState", chat.activityState)
            }
            array.put(obj)
        }
        sharedPrefs.edit().putString("persisted_chats", array.toString()).apply()
    }
    private fun loadContacts() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("persisted_chats", null)
        val tempList = ArrayList<ChatModel>()
        if (!json.isNullOrEmpty() && json != "[]") {
            try {
                val array = JSONArray(json)
                val now = System.currentTimeMillis()
                for (i in 0 until array.length()) {
                    try {
                        val item = array.opt(i)
                        if (item is JSONObject) {
                            val rawAvatar = if (item.isNull("avatarUri")) null else item.optString("avatarUri")
                            val typingUntil = item.optLong("typingUntil", 0L)
                            val isTyping = typingUntil > now
                            val rawActState = item.optString("activityState", "IDLE")
                            val actState = if ("TYPING".equals(rawActState, ignoreCase = true) && !isTyping) "IDLE" else rawActState
                            
                            val idStr = item.optString("id", System.currentTimeMillis().toString() + i)
                            val nameStr = item.optString("name", "Контакт")
                            val isSocketConnected = BluetoothSocketHolder.isConnectedWith(idStr, nameStr)
                            val realOnlineStatus = if (isSocketConnected) {
                                OnlineStatus.ONLINE
                            } else {
                                try { OnlineStatus.valueOf(item.optString("onlineStatus", "OFFLINE")) } catch(e: Exception) { OnlineStatus.OFFLINE }
                            }

                            val chat = ChatModel(
                                idStr,
                                nameStr,
                                item.optString("lastMessage", ""),
                                item.optString("time", "сейчас"),
                                if (rawAvatar.isNullOrEmpty()) null else rawAvatar,
                                realOnlineStatus,
                                try { MessageStatus.valueOf(item.optString("messageStatus", "NONE")) } catch(e: Exception) { MessageStatus.NONE },
                                item.optInt("unreadCount", 0),
                                item.optBoolean("isMuted", false),
                                isTyping,
                                typingUntil,
                                actState
                            )
                            tempList.add(chat)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        chatListState.clear()
        chatListState.addAll(tempList)
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
        if (islandBinding.btnSearchClear.isVisible && islandBinding.btnSearchClear.alpha == 1f) return
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
    private fun startUnifiedSearchAndDiscoverable() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            PrimeNotification.show(this, "Bluetooth не поддерживается устройством")
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            try {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                PrimeNotification.show(this, "Необходимо включить Bluetooth для поиска")
            }
            return
        }

        val requiredPerms = getRequiredBluetoothPermissions()
        val missingPerms = requiredPerms.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPerms.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(requiredPerms)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                               locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            if (!isGpsEnabled) {
                PrimeNotification.show(this, "Включите геолокацию в шторке для поиска устройств поблизости")
            }
        }

        startAcceptThread()

        try {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            }
            enableDiscoverableLauncher.launch(discoverableIntent)
        } catch (e: Exception) {
            performDiscovery()
        }
    }



    @SuppressLint("MissingPermission")
    private fun performDiscovery() {
        val btAdapter = bluetoothAdapter ?: return
        if (!btAdapter.isEnabled) return

        pairedDevices.clear()
        try {
            btAdapter.bondedDevices?.let { pairedDevices.addAll(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        discoveredDevices.clear()
        isScanningState.value = true

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_UUID)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            ContextCompat.registerReceiver(this, bluetoothReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            isReceiverRegistered = true
        }

        try {
            if (btAdapter.isDiscovering) {
                btAdapter.cancelDiscovery()
            }
            btAdapter.startDiscovery()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startAcceptThread()
    }

    private var acceptThread: AcceptThread? = null

    @SuppressLint("MissingPermission")
    private fun startAcceptThread() {
        stopAcceptThread()
        acceptThread = AcceptThread().apply { start() }
    }

    private fun stopAcceptThread() {
        try {
            acceptThread?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        acceptThread = null
    }

    private val primeUuidForServer = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null
        @Volatile private var isRunning = true

        init {
            try {
                mmServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("PrimeChat", primeUuidForServer)
            } catch (e: Exception) {
                Log.e("ChatListActivity", "AcceptThread listen failed", e)
            }
        }

        override fun run() {
            val serverSocket = mmServerSocket ?: return
            while (isRunning && !isInterrupted) {
                val socket: BluetoothSocket = try {
                    serverSocket.accept()
                } catch (e: Exception) {
                    break
                }

                if (socket.isConnected) {
                    val device = try { socket.remoteDevice } catch (e: Exception) { null }
                    val devMac = device?.address ?: ""
                    val devName = try { device?.name ?: "Prime Собеседник" } catch (e: Exception) { "Prime Собеседник" }

                    BluetoothSocketHolder.setSocket(socket)
                    BluetoothSocketHolder.setActiveDeviceAddress(devMac)
                    BluetoothSocketHolder.setActiveTargetUsername(devName)

                    try { mmServerSocket?.close() } catch (e: Exception) {}

                    navigateToChatPerson(devName, devMac, useExistingSocket = true)
                    break
                }
            }
        }

        fun cancel() {
            isRunning = false
            try { mmServerSocket?.close() } catch (e: Exception) {}
        }
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
        stopAcceptThread()
        typingExpireHandler.removeCallbacksAndMessages(null)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener)
    }
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    private fun getAvatarColor(name: String): Int {
        val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
        val hash = name.hashCode()
        val index = (if (hash == Int.MIN_VALUE) 0 else abs(hash)) % colors.size
        return colors[index].toColorInt()
    }
}
