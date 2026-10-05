package com.messenger.prime

import android.Manifest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
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
import androidx.lifecycle.lifecycleScope
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.BitmapFactory
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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import androidx.compose.ui.unit.IntOffset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun AnimatedSwipeableBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(dismissSheet: (onComplete: (() -> Unit)?) -> Unit) -> Unit
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = onDismissRequest,
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

        val coroutineScope = rememberCoroutineScope()
        val offsetY = remember { Animatable(1000f) }
        var isClosing by remember { mutableStateOf(false) }

        val triggerDismiss: (onComplete: (() -> Unit)?) -> Unit = remember {
            { onComplete ->
                if (!isClosing) {
                    isClosing = true
                    coroutineScope.launch {
                        offsetY.animateTo(
                            targetValue = 1200f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        )
                        onDismissRequest()
                        onComplete?.invoke()
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        val progress = (1f - (offsetY.value / 600f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { triggerDismiss(null) },
            contentAlignment = Alignment.BottomCenter
        ) {
            BlurView(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, offsetY.value.toInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY.value > 120f) {
                                    triggerDismiss(null)
                                } else {
                                    coroutineScope.launch {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetY.animateTo(0f)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                                    offsetY.snapTo(newOffset)
                                }
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}, // prevent closing when clicking content
                blurRadius = 24.dp,
                tint = Color(0x660F172A),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .width(48.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                    )

                    content { onComplete -> triggerDismiss(onComplete) }
                }
            }
        }
    }
}

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



@Composable
fun CachedAvatarView(name: String, macAddress: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE) }

    val avatarUriStr = remember(name, macAddress) {
        val spAvatar = if (macAddress.isNotEmpty()) sharedPrefs.getString("contact_avatar_$macAddress", null) else null
        spAvatar ?: (if (name.isNotEmpty()) sharedPrefs.getString("contact_avatar_$name", null) else null)
        ?: (if (macAddress.isNotEmpty()) sharedPrefs.getString("${macAddress}_avatarUri", null) else null)
        ?: (if (name.isNotEmpty()) sharedPrefs.getString("${name}_avatarUri", null) else null)
        ?: (if (macAddress.isNotEmpty()) sharedPrefs.getString("${macAddress}_avatar", null) else null)
        ?: (if (name.isNotEmpty()) sharedPrefs.getString("${name}_avatar", null) else null)
    }

    var bitmap by remember(name, macAddress, avatarUriStr) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(name, macAddress, avatarUriStr) {
        withContext(Dispatchers.IO) {
            val candidates = mutableListOf<File>()
            if (!avatarUriStr.isNullOrEmpty()) {
                val parsed = Uri.parse(avatarUriStr)
                if ("file".equals(parsed.scheme, ignoreCase = true) && parsed.path != null) {
                    candidates.add(File(parsed.path!!))
                } else {
                    candidates.add(File(avatarUriStr))
                }
            }
            if (macAddress.isNotEmpty()) {
                candidates.add(File(context.filesDir, "rec_avatar_$macAddress.gif"))
                candidates.add(File(context.filesDir, "rec_avatar_$macAddress.jpg"))
                candidates.add(File(context.filesDir, "avatar_$macAddress.gif"))
                candidates.add(File(context.filesDir, "avatar_$macAddress.jpg"))
            }
            if (name.isNotEmpty()) {
                candidates.add(File(context.filesDir, "rec_avatar_$name.gif"))
                candidates.add(File(context.filesDir, "rec_avatar_$name.jpg"))
                candidates.add(File(context.filesDir, "avatar_$name.gif"))
                candidates.add(File(context.filesDir, "avatar_$name.jpg"))
            }

            val foundFile = candidates.firstOrNull { it.exists() && it.length() > 0 }
            if (foundFile != null) {
                try {
                    BitmapFactory.decodeFile(foundFile.absolutePath)?.let { bmp ->
                        bitmap = bmp.asImageBitmap()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val avatarShape = RoundedCornerShape(10.dp)

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(avatarShape)
        )
    } else {
        val colors = listOf(0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722)
        val hash = name.hashCode()
        val color = Color(colors[(if (hash == Int.MIN_VALUE) 0 else abs(hash)) % colors.size])
        val initial = if (name.isNotEmpty()) name.take(1).uppercase() else "P"

        Box(
            modifier = modifier.clip(avatarShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListContentBinding
    private lateinit var adapter: ChatListAdapter
    private var allChats: List<ChatModel> = ArrayList()

    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkConnected = true
    private val isContentBindingReady = mutableStateOf(false)

    private val isContactDialogVisible = mutableStateOf(false)
    private val chatListState = mutableStateListOf<ChatModel>()
    private val typingExpireHandler = Handler(Looper.getMainLooper())
    private val typingExpireRunnable = Runnable { reloadChatsFromDb() }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isReceiverRegistered = false
    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    private val pairedDevices = mutableStateListOf<BluetoothDevice>()
    private val isScanningState = mutableStateOf(false)
    private val deviceRssiMap = mutableStateMapOf<String, Short>()

    private var topInsetPx: Int = 0
    private var bottomInsetPx: Int = 0

    private var startY = 0f
    private var initialIsAtTop = false
    private var isPulling = false
    private var isThresholdCrossed = false
    private val pullThreshold = 350f
    private var isTransitioning = false

    private val isIncomingConnectionDialogVisible = mutableStateOf(false)
    private var incomingSocket: BluetoothSocket? = null
    private var incomingDeviceName: String = ""
    private var incomingDeviceMac: String = ""
    
    private val isFoundDeviceDialogVisible = mutableStateOf(false)
    private var foundDeviceName: String = ""
    private var foundDeviceMac: String = ""

    var currentSearchQuery: String = ""
        private set

    fun performSearchQuery(queryRaw: String) {
        val query = queryRaw.trim().lowercase().replace('ё', 'е')
        currentSearchQuery = queryRaw.trim()

        if (query == "/blocktestme") {
            adapter.setSearchQuery(queryRaw.trim())
            adapter.setSearchActive(true)
            backCallback.isEnabled = true
            val testContact = ChatModel("block_test_contact", "Тестирование активити блока", "Нажмите, чтобы протестировать", "сейчас", null, OnlineStatus.ONLINE)
            adapter.updateList(listOf(testContact))
            updateEmptyState()
            return
        }

        if (query.isEmpty()) {
            adapter.setSearchQuery("")
            adapter.setSearchActive(false)
            backCallback.isEnabled = false
            val mainList = ArrayList(chatListState)
            allChats = mainList
            adapter.updateList(mainList)
            updateEmptyState()
        } else {
            adapter.setSearchQuery(queryRaw.trim())
            adapter.setSearchActive(true)
            backCallback.isEnabled = true
            
            val queryTokens = query.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            val filteredSaved = chatListState.filter { chat ->
                val nameLower = chat.name.lowercase().replace('ё', 'е')
                val lastMsgLower = chat.lastMessage.lowercase().replace('ё', 'е')
                val idLower = chat.id.lowercase().replace('ё', 'е')

                queryTokens.all { token ->
                    nameLower.contains(token) || lastMsgLower.contains(token) || idLower.contains(token)
                }
            }

            val safeDiscoveredList = try {
                ArrayList(discoveredDevices)
            } catch (_: Exception) {
                emptyList<BluetoothDevice>()
            }

            val matchingDiscovered = safeDiscoveredList.filter { dev ->
                if (dev == null) return@filter false
                val devAddr = try { dev.address } catch (_: Exception) { null }
                if (devAddr.isNullOrEmpty()) return@filter false

                val devName = try { @Suppress("MissingPermission") dev.name } catch (_: Exception) { null } ?: ""
                val devNameLower = devName.lowercase().replace('ё', 'е')
                val macLower = devAddr.lowercase()

                val isDiscoveredMatch = queryTokens.all { token ->
                    devNameLower.contains(token) || macLower.contains(token)
                }

                isDiscoveredMatch && !filteredSaved.any { it.id.equals(devAddr, ignoreCase = true) }
            }.map { dev ->
                val devAddr = try { dev.address } catch (_: Exception) { "" } ?: ""
                val devName = try { @Suppress("MissingPermission") dev.name ?: "Prime Собеседник" } catch (_: Exception) { "Prime Собеседник" }
                val finalName = if (BluetoothAdapter.checkBluetoothAddress(devName)) "Prime Собеседник" else devName
                ChatModel(
                    id = devAddr,
                    name = finalName,
                    lastMessage = "Найден поблизости (Bluetooth)",
                    time = "сейчас",
                    avatarUri = null,
                    onlineStatus = OnlineStatus.ONLINE
                )
            }

            val fullSearchResults = filteredSaved + matchingDiscovered
            allChats = fullSearchResults
            adapter.updateList(fullSearchResults)
            updateEmptyState()
        }
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isContactDialogVisible.value) {
                isContactDialogVisible.value = false
                stopBluetoothScan()
                return
            }
            if (::binding.isInitialized && binding.layoutIslandHeader.btnHeaderSearchClear.visibility == View.VISIBLE) {
                binding.layoutIslandHeader.btnHeaderSearchClear.performClick()
                return
            }
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

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

        val savedName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("contact_name_$deviceAddress", null)
            ?: getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${deviceAddress}_name", null)
        val resolvedTargetName = if (!savedName.isNullOrEmpty() && !BluetoothAdapter.checkBluetoothAddress(savedName)) {
            savedName
        } else if (targetName.isNotEmpty() && !BluetoothAdapter.checkBluetoothAddress(targetName) && targetName != "Собеседник") {
            targetName
        } else if (deviceAddress.isNotEmpty()) {
            deviceAddress
        } else {
            "Собеседник"
        }

        // 1. Immediately cancel Bluetooth discovery synchronously to free radio module!
        try {
            val bManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            val bAdapter = bManager?.adapter
            @Suppress("MissingPermission")
            bAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Failed to cancel discovery: ${e.message}")
        }

        // 2. Start Bluetooth service immediately
        PrimeBluetoothService.startService(this)

        // 3. Trigger forced connection in BluetoothConnectionManager immediately
        if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            try {
                val bManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
                val bAdapter = bManager?.adapter
                if (bAdapter != null && bAdapter.isEnabled) {
                    val device = bAdapter.getRemoteDevice(deviceAddress)
                    val currentUser = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("current_user", "") ?: ""
                    val myDisplayName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${currentUser}_name", currentUser) ?: currentUser

                    BluetoothConnectionManager.getInstance().connectToDevice(
                        bAdapter,
                        device,
                        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"),
                        myDisplayName,
                        resolvedTargetName,
                        true
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatListActivity", "Failed to connectToDevice in navigateToChatPerson", e)
            }
        }

        // 4. Open ChatPersonActivity immediately
        runOnUiThread {
            stopBluetoothScan()
            stopAcceptThread()
            isContactDialogVisible.value = false
            isIncomingConnectionDialogVisible.value = false
            isFoundDeviceDialogVisible.value = false

            val chatIntent = Intent(this@ChatListActivity, ChatPersonActivity::class.java).apply {
                putExtra("EXTRA_CHAT_NAME", resolvedTargetName)
                putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
                putExtra("EXTRA_AUTO_CONNECT", true)
                if (useExistingSocket) {
                    putExtra("EXTRA_USE_EXISTING_SOCKET", true)
                }
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(chatIntent)
            PrimeTransitions.applyOpenTransition(this@ChatListActivity)
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

    private var primeDiscoveredPeer: ChatModel? = null

    private fun handlePrimeDeviceFound(device: BluetoothDevice, name: String?) {
        if (primeDevices.add(device.address)) {
            val knownContact = chatListState.find { it.id.equals(device.address, ignoreCase = true) }
            val rawName = knownContact?.name ?: name
            val finalName = if (rawName.isNullOrBlank() || BluetoothAdapter.checkBluetoothAddress(rawName)) "Prime Собеседник" else rawName
            
            if (knownContact == null) {
                primeDiscoveredPeer = ChatModel(
                    id = device.address,
                    name = finalName,
                    lastMessage = "Найден поблизости",
                    time = "сейчас",
                    avatarUri = null,
                    onlineStatus = OnlineStatus.ONLINE
                )
            }

            runOnUiThread {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@runOnUiThread
                }
                triggerPrimeFoundVibration()
                PrimeNotification.show(this@ChatListActivity, "Найден: $finalName")
                if (::adapter.isInitialized) {
                    adapter.updateList(allChats)
                }
            }
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
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        if (rssi != Short.MIN_VALUE) {
                            deviceRssiMap[device.address] = rssi
                        }

                        if (!discoveredDevices.any { it.address == device.address }) {
                            discoveredDevices.add(device)
                        } else {
                            val idx = discoveredDevices.indexOfFirst { it.address == device.address }
                            if (idx != -1) {
                                discoveredDevices[idx] = device
                            }
                        }
                        val devName = try { device.name } catch (_: Exception) { null }
                        val isSavedInChats = chatListState.any { it.id.equals(device.address, ignoreCase = true) }
                        
                        if (isSavedInChats || devName?.contains("Prime", ignoreCase = true) == true) {
                            handlePrimeDeviceFound(device, devName)
                        } else {
                            val cachedUuids = try { device.uuids } catch (_: Exception) { null }
                            if (cachedUuids != null) {
                                for (uuid in cachedUuids) {
                                    if (uuid.uuid.toString().equals(primeUuid.toString(), ignoreCase = true)) {
                                        handlePrimeDeviceFound(device, devName)
                                        break
                                    }
                                }
                            }
                        }
                        try {
                            device.fetchUuidsWithSdp()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Fast delayed re-checks after Android resolves name/SDP UUIDs
                        val mainHandler = Handler(Looper.getMainLooper())
                        val checkTask = Runnable {
                            val updatedName = try { device.name } catch (_: Exception) { null }
                            val updatedUuids = try { device.uuids } catch (_: Exception) { null }
                            val isSaved = chatListState.any { it.id.equals(device.address, ignoreCase = true) }
                            val isPrime = (updatedName?.contains("Prime", ignoreCase = true) == true) ||
                                          (updatedUuids?.any { it.uuid.toString().equals(primeUuid.toString(), ignoreCase = true) } == true) ||
                                          isSaved
                            if (isPrime) {
                                handlePrimeDeviceFound(device, updatedName)
                            }
                        }
                        mainHandler.postDelayed(checkTask, 1200L)
                        mainHandler.postDelayed(checkTask, 2800L)
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        val devName = try { device.name } catch (_: Exception) { null }
                        if (devName?.contains("Prime", ignoreCase = true) == true) {
                            handlePrimeDeviceFound(device, devName)
                        }
                        val idx = discoveredDevices.indexOfFirst { it.address == device.address }
                        if (idx != -1) {
                            discoveredDevices[idx] = device
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val btAdapter = bluetoothAdapter
                    try {
                        btAdapter?.bondedDevices?.let { bonded ->
                            pairedDevices.clear()
                            pairedDevices.addAll(bonded)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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

                        val isSaved = chatListState.any { it.id.equals(device.address, ignoreCase = true) }
                        if (uuidList.any { it.equals(primeUuid.toString(), ignoreCase = true) } || isSaved) {
                            val devName = try { device.name ?: "Prime Собеседник" } catch(e: Exception) { "Prime Собеседник" }
                            handlePrimeDeviceFound(device, devName)
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
                refreshUserUi()
            }
        }

        override fun onLost(network: Network) {
            runOnUiThread {
                isNetworkConnected = false
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "my_name" || key == "my_avatar" || key == "my_local_name" || key == "my_local_avatar" || key == "current_user" || key?.endsWith("_name") == true || key?.endsWith("_avatar") == true) {
            runOnUiThread { refreshUserUi() }
        }
        if (key == "persisted_chats" || key?.startsWith("contact_") == true || key?.startsWith("chat_") == true) {
            runOnUiThread { reloadChatsFromDb() }
        }
    }



    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isTransitioning) return true
        if (!::binding.isInitialized) return super.dispatchTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                initialIsAtTop = !binding.recyclerViewChats.canScrollVertically(-1)
                isPulling = false
                isThresholdCrossed = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - startY

                if (initialIsAtTop && dy > 10f && !adapter.isSearchActive) {
                    isPulling = true
                    val pullDist = dy * 0.42f
                    val progress = (dy / pullThreshold).coerceIn(0f, 1.2f)
                    
                    if (progress >= 1.0f && !isThresholdCrossed) {
                        isThresholdCrossed = true
                        binding.recyclerViewChats.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    } else if (progress < 1.0f) {
                        isThresholdCrossed = false
                    }
                    
                    binding.layoutIslandHeader.root.translationY = pullDist
                    binding.recyclerViewChats.translationY = pullDist
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPulling) {
                    if (isThresholdCrossed && !isTransitioning) {
                        isTransitioning = true
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                        PrimeTransitions.applyOpenTransition(this)
                        
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
        val decelerate = DecelerateInterpolator()
        binding.layoutIslandHeader.root.animate()
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(decelerate)
            .start()
        binding.recyclerViewChats.animate()
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(decelerate)
            .withEndAction {
                isPulling = false
                isThresholdCrossed = false
            }
            .start()
    }

    private fun resetPullUiInstant() {
        if (!::binding.isInitialized) return
        binding.layoutIslandHeader.root.animate().cancel()
        binding.recyclerViewChats.animate().cancel()
        binding.layoutIslandHeader.root.translationY = 0f
        binding.recyclerViewChats.translationY = 0f
        isPulling = false
        isThresholdCrossed = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        PrimeTransitions.setupActivityTransitions(this)

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        checkNotificationPermission()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", "Мой профиль") ?: "Мой профиль"

        val initialChats = try {
            loadContacts()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        chatListState.clear()
        chatListState.addAll(initialChats)
        allChats = ArrayList(chatListState)

        adapter = ChatListAdapter(
            chatList = allChats,
            onChatClick = { chat ->
                if (chat.id == "block_test_contact") {
                    val parts = (chat.lastMessage ?: "").split(" ")
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
                        PrimeTransitions.applyOpenTransition(this)
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
            val isDialogVisible by isContactDialogVisible
            val isNameEditVisible by isNameEditDialogVisible
            val isIncomingDialogVisible by isIncomingConnectionDialogVisible
            val isFoundDialogVisible by isFoundDeviceDialogVisible
            
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
                        (binding.recyclerViewChats.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
                        
                        setupIslandHeaderView()
                        setupSwipeToDelete()
                        isContentBindingReady.value = true
                        view
                    },
                    update = { _ ->
                        this@ChatListActivity.topInsetPx = topInsetPx
                        this@ChatListActivity.bottomInsetPx = bottomInsetPx

                        val topMargin = topInsetPx + (12 * resources.displayMetrics.density).toInt()
                        val lp = binding.layoutIslandHeader.root.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                        if (lp != null && lp.topMargin != topMargin) {
                            lp.topMargin = topMargin
                            binding.layoutIslandHeader.root.layoutParams = lp
                        }

                        updateNavigationStyleUi()
                        updateEmptyState()
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

                AnimatedSwipeableBottomSheet(
                    isVisible = isDialogVisible,
                    onDismissRequest = { isContactDialogVisible.value = false }
                ) { dismissSheet ->
                    val versionName = try {
                        packageManager.getPackageInfo(packageName, 0).versionName ?: "Beta 0.45"
                    } catch (e: Exception) {
                        "Beta 0.45"
                    }
                    val myVerCode = try { ChatPersonActivity.getAppVersionCode(this@ChatListActivity) } catch (e: Exception) { 1 }

                    Text(
                        text = "Поиск Prime-собеседников",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ваш ID: $packageName • Prime $versionName",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val rawAllDevices = (pairedDevices.map { it to true } + discoveredDevices.map { it to false })
                        .distinctBy { it.first.address }
                        .filter { (device, _) ->
                            val name = try { @Suppress("MissingPermission") device.name } catch (e: Exception) { null }
                            val devUuids = try { @Suppress("MissingPermission") device.uuids } catch (e: Exception) { null }
                            val hasPrimeUuid = devUuids?.any { it.uuid.toString().equals(primeUuid.toString(), ignoreCase = true) } == true
                            val isSavedInChats = chatListState.any { it.id.equals(device.address, ignoreCase = true) }
                            
                            val isExplicitPrime = (name?.contains("Prime", ignoreCase = true) == true) || 
                                                  primeDevices.contains(device.address) || 
                                                  hasPrimeUuid || isSavedInChats
                            if (isExplicitPrime) return@filter true

                            val isUnknownName = name.isNullOrBlank() || 
                                                name.equals("Неизвестное", ignoreCase = true) || 
                                                name.equals("Unknown", ignoreCase = true) || 
                                                name.equals("null", ignoreCase = true) ||
                                                name.equals("1", ignoreCase = true)
                            if (isUnknownName) return@filter false

                            val bluetoothClass = try { @Suppress("MissingPermission") device.bluetoothClass } catch (e: Exception) { null }
                            val majorClass = bluetoothClass?.majorDeviceClass
                            val lowerName = name?.lowercase() ?: ""
                            
                            val isUnwantedDevice = majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO || 
                                                   majorClass == BluetoothClass.Device.Major.WEARABLE ||
                                                   majorClass == BluetoothClass.Device.Major.PERIPHERAL ||
                                                   majorClass == BluetoothClass.Device.Major.HEALTH ||
                                                   majorClass == BluetoothClass.Device.Major.TOY ||
                                                   majorClass == BluetoothClass.Device.Major.IMAGING ||
                                                   lowerName.contains("buds") ||
                                                   lowerName.contains("headphone") ||
                                                   lowerName.contains("earphone") ||
                                                   lowerName.contains("airpods") ||
                                                   lowerName.contains("audio") ||
                                                   lowerName.contains("speaker") ||
                                                   lowerName.contains("jbl") ||
                                                   lowerName.contains("sony") ||
                                                   lowerName.contains("bose") ||
                                                   lowerName.contains("wh-") ||
                                                   lowerName.contains("wi-") ||
                                                   lowerName.contains("tws")
                                                   
                            !isUnwantedDevice
                        }

                    val primeOnlyDevices = rawAllDevices.filter { (device, _) ->
                        val name = try { @Suppress("MissingPermission") device.name } catch (e: Exception) { null }
                        val devUuids = try { @Suppress("MissingPermission") device.uuids } catch (e: Exception) { null }
                        val hasPrimeUuid = devUuids?.any { it.uuid.toString().equals(primeUuid.toString(), ignoreCase = true) } == true
                        val isSavedInChats = chatListState.any { it.id.equals(device.address, ignoreCase = true) }
                        
                        (name?.contains("Prime", ignoreCase = true) == true) || 
                        primeDevices.contains(device.address) || 
                        hasPrimeUuid || isSavedInChats
                    }
                    
                    // Умная сортировка: устройства с более сильным сигналом (ближе) в начале списка
                    val pairedPrimeList = primeOnlyDevices.filter { it.second }
                        .sortedByDescending { deviceRssiMap[it.first.address]?.toInt() ?: -100 }
                    val discoveredPrimeList = primeOnlyDevices.filter { !it.second }
                        .sortedByDescending { deviceRssiMap[it.first.address]?.toInt() ?: -100 }

                    val allPrimeList = discoveredPrimeList + pairedPrimeList

                    val otherDevices = rawAllDevices.filterNot { (device, _) ->
                        primeOnlyDevices.any { it.first.address == device.address }
                    }.sortedByDescending { deviceRssiMap[it.first.address]?.toInt() ?: -100 }

                    if (isScanningState.value) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(90.dp)) {
                            RadarAnimation()
                            Text(
                                text = if (primeOnlyDevices.isEmpty()) "Поиск Prime-пользователей..." else "Найдено Prime: ${primeOnlyDevices.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { startUnifiedSearchAndDiscoverable() },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                        ) {
                            Text("⚡ Повторить поиск и авто-видимость", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. HORIZONTAL LAZYROW for Prime Users (Nearest by RSSI first)
                    if (allPrimeList.isNotEmpty()) {
                        Text(
                            text = "⚡ Prime-собеседники (${allPrimeList.size})",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allPrimeList, key = { "prime_row_" + it.first.address }) { (device, isPaired) ->
                                val devMac = device.address
                                val knownContact = chatListState.find { it.id == devMac }
                                var devName = knownContact?.name ?: try { @Suppress("MissingPermission") device.name ?: "Prime Собеседник" } catch (_: Exception) { "Prime Собеседник" }
                                if (BluetoothAdapter.checkBluetoothAddress(devName)) devName = "Prime Собеседник"
                                val rssi = deviceRssiMap[devMac]

                                val proximityText = if (!isPaired) {
                                    if (rssi != null && rssi != Short.MIN_VALUE) {
                                        when {
                                            rssi >= -62 -> "🟢 ~1-3м ($rssi dBm)"
                                            rssi >= -78 -> "🟡 ~5-8м ($rssi dBm)"
                                            else -> "⚪ В радиусе ($rssi dBm)"
                                        }
                                    } else "Новый"
                                } else "Сопряжен"

                                Card(
                                    modifier = Modifier
                                        .width(122.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
                                        .clickable {
                                            dismissSheet {
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x3300E676))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            CachedAvatarView(
                                                name = devName,
                                                macAddress = devMac,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                                    .border(1.dp, Color.Black, CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = devName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = proximityText,
                                            color = if (rssi != null && rssi >= -62) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Button(
                                            onClick = {
                                                dismissSheet {
                                                    isContactDialogVisible.value = false
                                                    navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.fillMaxWidth().height(28.dp)
                                        ) {
                                            Text("Связь", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. VERTICAL LAZYCOLUMN for Other Discovered Non-Audio Devices (Sorted by RSSI)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (otherDevices.isNotEmpty()) {
                            item {
                                Text(
                                    text = "📡 Другие устройства (${otherDevices.size})",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            items(otherDevices, key = { "other_dev_" + it.first.address }) { (device, isPaired) ->
                                var devName = try { @Suppress("MissingPermission") device.name ?: "Устройство" } catch (_: Exception) { "Устройство" }
                                if (BluetoothAdapter.checkBluetoothAddress(devName)) devName = "Устройство"
                                val devMac = device.address
                                val rssi = deviceRssiMap[devMac]

                                val statusSubText = if (isPaired) "Ранее сопряжено" else {
                                    if (rssi != null && rssi != Short.MIN_VALUE) {
                                        when {
                                            rssi >= -62 -> "🟢 Очень близко (~1-3м)"
                                            rssi >= -78 -> "🟡 Рядом (~5-8м)"
                                            else -> "⚪ В радиусе действия ($rssi dBm)"
                                        }
                                    } else "Устройство поблизости"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0AFFFFFF))
                                        .clickable {
                                            dismissSheet {
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CachedAvatarView(name = devName, macAddress = devMac, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = devName, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = statusSubText, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            dismissSheet {
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(devName, devMac, useExistingSocket = false)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Проверить", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (primeOnlyDevices.isEmpty() && otherDevices.isEmpty() && !isScanningState.value) {
                        Text(
                            text = "Устройства не найдены поблизости",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isIncomingDialogVisible) {
                    Dialog(
                        onDismissRequest = { 
                            isIncomingConnectionDialogVisible.value = false
                            try { incomingSocket?.close() } catch (e: Exception) {}
                        },
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
                                .clickable { 
                                    isIncomingConnectionDialogVisible.value = false
                                    try { incomingSocket?.close() } catch (e: Exception) {}
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BlurView(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(32.dp))
                                    .clickable(enabled = true) {},
                                blurRadius = 20.dp,
                                tint = Color(0x33154B87),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CachedAvatarView(
                                        name = incomingDeviceName,
                                        macAddress = incomingDeviceMac,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Входящее подключение",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Пользователь $incomingDeviceName хочет начать чат.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        OutlinedButton(
                                            onClick = {
                                                isIncomingConnectionDialogVisible.value = false
                                                try { incomingSocket?.close() } catch (e: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0x40FFFFFF))
                                        ) {
                                            Text("Отклонить", fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Button(
                                            onClick = {
                                                isIncomingConnectionDialogVisible.value = false
                                                val socket = incomingSocket
                                                if (socket != null && socket.isConnected) {
                                                    BluetoothSocketHolder.registerConnection(incomingDeviceMac, incomingDeviceName, socket, null)
                                                    navigateToChatPerson(incomingDeviceName, incomingDeviceMac, useExistingSocket = true)
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                                        ) {
                                            Text("Принять", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFoundDialogVisible) {
                    Dialog(
                        onDismissRequest = { isFoundDeviceDialogVisible.value = false },
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
                                .clickable { isFoundDeviceDialogVisible.value = false },
                            contentAlignment = Alignment.Center
                        ) {
                            BlurView(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(32.dp))
                                    .clickable(enabled = true) {},
                                blurRadius = 20.dp,
                                tint = Color(0x33154B87),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CachedAvatarView(
                                        name = foundDeviceName,
                                        macAddress = foundDeviceMac,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Найден собеседник!",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Поблизости найден пользователь $foundDeviceName.\nХотите подключиться к нему?",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        OutlinedButton(
                                            onClick = { isFoundDeviceDialogVisible.value = false },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0x40FFFFFF))
                                        ) {
                                            Text("Отмена", fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Button(
                                            onClick = {
                                                isFoundDeviceDialogVisible.value = false
                                                isContactDialogVisible.value = false
                                                navigateToChatPerson(foundDeviceName, foundDeviceMac, useExistingSocket = false)
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                                        ) {
                                            Text("Подключиться", fontWeight = FontWeight.Bold)
                                        }
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
                                            val newName = nameInput.trim()
                                            sharedPrefs.edit {
                                                putString("my_name", newName)
                                                putString("my_local_name", newName)
                                                putString("current_user_name", newName)
                                                if (currentUser.isNotEmpty()) {
                                                    putString("${currentUser}_name", newName)
                                                }
                                            }
                                            runOnUiThread {
                                                refreshUserUi()
                                                sendBroadcast(Intent("com.messenger.prime.NAME_CHANGED").setPackage(packageName))
                                                try {
                                                    BluetoothSocketHolder.notifyProfileChanged(this@ChatListActivity)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
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
            }
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkConnected = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback)
        onBackPressedDispatcher.addCallback(this, backCallback)
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
        // Text popup removed per user request
    }

    private val onChatListChanged = {
        runOnUiThread { reloadChatsFromDb() }
    }

    private val avatarChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.messenger.prime.CHAT_DELETED" -> {
                    reloadChatsFromDb()
                    ChatListNotifier.notifyChanged()
                }
                "com.messenger.prime.AVATAR_CHANGED" -> {
                    runOnUiThread { refreshUserUi() }
                }
                "com.messenger.prime.NAV_STYLE_CHANGED" -> {
                    runOnUiThread { updateNavigationStyleUi() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ChatListNotifier.subscribe(onChatListChanged)
        try {
            val filter = IntentFilter().apply {
                addAction("com.messenger.prime.CHAT_DELETED")
                addAction("com.messenger.prime.AVATAR_CHANGED")
                addAction("com.messenger.prime.NAV_STYLE_CHANGED")
            }
            ContextCompat.registerReceiver(this, avatarChangedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (_: Exception) {}
        reloadChatsFromDb()
        updateNavigationStyleUi()
    }

    override fun onPause() {
        super.onPause()
        stopBluetoothScan()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(avatarChangedReceiver)
        } catch (_: Exception) {}
        stopBluetoothScan()
        stopAcceptThread()
        typingExpireHandler.removeCallbacks(typingExpireRunnable)
        ChatListNotifier.unsubscribe(onChatListChanged)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reloadChatsFromDb()
        updateNavigationStyleUi()
        if (intent.getBooleanExtra("EXTRA_ACTION_SCAN", false)) {
            onStartChatClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserUi()
        reloadChatsFromDb()
        updateNavigationStyleUi()
        if (intent != null && intent.getBooleanExtra("EXTRA_ACTION_SCAN", false)) {
            intent.removeExtra("EXTRA_ACTION_SCAN")
            onStartChatClicked()
        }
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
        lifecycleScope.launch(Dispatchers.IO) {
            val chats = loadContacts()
            withContext(Dispatchers.Main) {
                chatListState.clear()
                chatListState.addAll(chats)
                allChats = ArrayList(chatListState)
                if (::adapter.isInitialized) {
                    adapter.updateList(allChats)
                }
                updateEmptyState()
                scheduleTypingExpiration()
            }
        }
    }

    private fun refreshUserUi() {
        if (!::binding.isInitialized) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val name = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", "Мой профиль") ?: "Мой профиль"
        val avatar = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24 * resources.displayMetrics.density
            setColor(accentColor)
        }
        binding.layoutIslandHeader.islandHeaderItem.background = bg

        var loaded = false
        val possibleFiles = listOfNotNull(
            if (!avatar.isNullOrEmpty()) {
                val uri = avatar.toUri()
                if ("file" == uri.scheme && uri.path != null) File(uri.path!!) else File(avatar)
            } else null,
            File(filesDir, "avatar_$currentUser.gif"),
            File(filesDir, "avatar_$currentUser.jpg")
        )

        val avatarFile = possibleFiles.firstOrNull { it.exists() && it.length() > 0 }
        if (avatarFile != null && avatarFile.exists()) {
            try {
                val isGif = avatarFile.name.lowercase().endsWith(".gif")
                val radiusPx = (14 * resources.displayMetrics.density).toInt()
                val signatureKey = ObjectKey(avatarFile.lastModified())
                if (isGif) {
                    Glide.with(this)
                        .asGif()
                        .load(avatarFile)
                        .override(200, 200)
                        .centerCrop()
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(binding.layoutIslandHeader.ivHeaderAvatar)
                } else {
                    Glide.with(this)
                        .load(avatarFile)
                        .override(200, 200)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(binding.layoutIslandHeader.ivHeaderAvatar)
                }
                binding.layoutIslandHeader.tvHeaderInitials.visibility = View.GONE
                binding.layoutIslandHeader.ivHeaderAvatar.visibility = View.VISIBLE
                loaded = true
            } catch (_: Exception) {}
        }

        if (!loaded) {
            val initial = if (name.isNotEmpty()) name.take(1).uppercase() else "П"
            binding.layoutIslandHeader.tvHeaderInitials.text = initial
            binding.layoutIslandHeader.tvHeaderInitials.visibility = View.VISIBLE
            binding.layoutIslandHeader.ivHeaderAvatar.visibility = View.INVISIBLE
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0.24f * 48 * resources.displayMetrics.density
                setColor(ColorAccentManager.getAvatarColor(name))
            }
            binding.layoutIslandHeader.tvHeaderInitials.background = avatarBg
        }
    }

    private fun updateM3TabState(
        isActive: Boolean,
        indicatorView: View?,
        iconView: ImageView?,
        labelView: TextView?,
        accentColor: Int,
        secondaryColor: Int
    ) {
        if (indicatorView == null || labelView == null) return
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val visibleInactiveColor = if (isDark) android.graphics.Color.parseColor("#E6FFFFFF") else android.graphics.Color.parseColor("#E6154B87")

        indicatorView.visibility = View.VISIBLE

        if (isActive) {
            indicatorView.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            iconView?.setColorFilter(android.graphics.Color.WHITE)
            labelView.setTextColor(accentColor)
            labelView.setTypeface(null, android.graphics.Typeface.BOLD)
            labelView.alpha = 1.0f
        } else {
            indicatorView.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            iconView?.setColorFilter(visibleInactiveColor)
            labelView.setTextColor(visibleInactiveColor)
            labelView.setTypeface(null, android.graphics.Typeface.NORMAL)
            labelView.alpha = 1.0f
        }
    }

    private fun updateNavigationStyleUi() {
        if (!::binding.isInitialized) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"
        val navView = findViewById<View>(R.id.blurBottomNav) ?: return

        val density = resources.displayMetrics.density
        val lp = binding.recyclerViewChats.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        if (navStyle == "bottom_bar") {
            binding.layoutIslandHeader.root.visibility = View.GONE
            navView.visibility = View.VISIBLE
            navView.bringToFront()

            val navLp = navView.layoutParams as? ViewGroup.MarginLayoutParams
            if (navLp != null) {
                val targetMargin = bottomInsetPx + (12 * density).toInt()
                if (navLp.bottomMargin != targetMargin) {
                    navLp.bottomMargin = targetMargin
                    navView.layoutParams = navLp
                }
            }

            if (lp != null) {
                lp.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                binding.recyclerViewChats.layoutParams = lp
            }

            val topPadding = topInsetPx + (12 * density).toInt()
            val bottomPadding = bottomInsetPx + (88 * density).toInt()
            binding.recyclerViewChats.setPadding(
                binding.recyclerViewChats.paddingLeft,
                topPadding,
                binding.recyclerViewChats.paddingRight,
                bottomPadding
            )

            setupBottomNav()
        } else {
            binding.layoutIslandHeader.root.visibility = View.VISIBLE
            navView.visibility = View.GONE

            if (lp != null) {
                lp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.topToBottom = R.id.layoutIslandHeader
                binding.recyclerViewChats.layoutParams = lp
            }

            val topPadding = (8 * density).toInt()
            val bottomPadding = bottomInsetPx + (24 * density).toInt()
            binding.recyclerViewChats.setPadding(
                binding.recyclerViewChats.paddingLeft,
                topPadding,
                binding.recyclerViewChats.paddingRight,
                bottomPadding
            )
        }
    }

    private fun setupBottomNav() {
        if (!::binding.isInitialized) return
        val navView = findViewById<View>(R.id.blurBottomNav) ?: return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"

        if (navStyle != "bottom_bar") {
            navView.visibility = View.GONE
            return
        }

        navView.visibility = View.VISIBLE
        navView.bringToFront()

        val density = resources.displayMetrics.density

        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = v.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                val targetMargin = systemBars.bottom + (12 * density).toInt()
                if (lp.bottomMargin != targetMargin) {
                    lp.bottomMargin = targetMargin
                    v.layoutParams = lp
                }
            }
            insets
        }

        val btnNavChats = navView.findViewById<View>(R.id.btnNavChats)
        val btnNavDevices = navView.findViewById<View>(R.id.btnNavDevices)
        val btnNavSearch = navView.findViewById<View>(R.id.btnNavSearch)
        val btnNavProfile = navView.findViewById<View>(R.id.btnNavProfile)

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val secondaryColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

        val vNavChatsIndicator = navView.findViewById<View>(R.id.vNavChatsIndicator)
        val vNavDevicesIndicator = navView.findViewById<View>(R.id.vNavDevicesIndicator)
        val vNavSearchIndicator = navView.findViewById<View>(R.id.vNavSearchIndicator)
        val vNavProfileIndicator = navView.findViewById<View>(R.id.vNavProfileIndicator)

        val ivNavChatsIcon = navView.findViewById<ImageView>(R.id.ivNavChatsIcon)
        val ivNavDevicesIcon = navView.findViewById<ImageView>(R.id.ivNavDevicesIcon)
        val ivNavSearchIcon = navView.findViewById<ImageView>(R.id.ivNavSearchIcon)

        val tvNavChatsLabel = navView.findViewById<TextView>(R.id.tvNavChatsLabel)
        val tvNavDevicesLabel = navView.findViewById<TextView>(R.id.tvNavDevicesLabel)
        val tvNavSearchLabel = navView.findViewById<TextView>(R.id.tvNavSearchLabel)
        val tvNavProfileLabel = navView.findViewById<TextView>(R.id.tvNavProfileLabel)

        updateM3TabState(true, vNavChatsIndicator, ivNavChatsIcon, tvNavChatsLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, tvNavDevicesLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavSearchIndicator, ivNavSearchIcon, tvNavSearchLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavProfileIndicator, null, tvNavProfileLabel, accentColor, secondaryColor)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val myAvatar = sharedPrefs.getString("my_avatar", null)
                ?: sharedPrefs.getString("my_local_avatar", null)
            if (!myAvatar.isNullOrEmpty()) {
                val (_, file) = parseAvatarModelAndFile(myAvatar)
                if (file != null && file.exists()) {
                    Glide.with(this).load(file).into(profileAvatarIv)
                } else {
                    Glide.with(this).load(myAvatar).into(profileAvatarIv)
                }
            } else {
                profileAvatarIv.setImageResource(R.drawable.ic_person)
            }
        }

        val blurNavView = navView as? eightbitlab.com.blurview.BlurView
        if (blurNavView != null) {
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#700F172A") else android.graphics.Color.parseColor("#70154B87")
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            blurNavView.setupBlur(rootView, 22f, overlayColor, window.decorView.background)
        }

        btnNavChats?.setOnClickListener {
            binding.recyclerViewChats.smoothScrollToPosition(0)
        }

        btnNavDevices?.setOnClickListener {
            onStartChatClicked()
        }

        btnNavSearch?.setOnClickListener {
            val intent = Intent(this, GlobalSearchActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
        }

        btnNavProfile?.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
        }
    }

    private fun openHeaderSearch() {
        backCallback.isEnabled = true
        val header = binding.layoutIslandHeader

        header.tvHeaderTitle.animate().cancel()
        header.etHeaderSearch.animate().cancel()
        header.layoutHeaderNormalButtons.animate().cancel()
        header.btnHeaderSearchClear.animate().cancel()

        header.tvHeaderTitle.visibility = View.GONE
        header.layoutHeaderNormalButtons.visibility = View.GONE

        header.etHeaderSearch.alpha = 0f
        header.etHeaderSearch.visibility = View.VISIBLE
        header.etHeaderSearch.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        header.btnHeaderSearchClear.alpha = 0f
        header.btnHeaderSearchClear.visibility = View.VISIBLE
        header.btnHeaderSearchClear.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        header.etHeaderSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        @Suppress("DEPRECATION")
        imm?.showSoftInput(header.etHeaderSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeHeaderSearch() {
        val header = binding.layoutIslandHeader

        header.tvHeaderTitle.animate().cancel()
        header.etHeaderSearch.animate().cancel()
        header.layoutHeaderNormalButtons.animate().cancel()
        header.btnHeaderSearchClear.animate().cancel()

        header.etHeaderSearch.text?.clear()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(header.etHeaderSearch.windowToken, 0)
        header.etHeaderSearch.clearFocus()

        header.etHeaderSearch.visibility = View.GONE
        header.btnHeaderSearchClear.visibility = View.GONE

        header.tvHeaderTitle.alpha = 0f
        header.tvHeaderTitle.visibility = View.VISIBLE
        header.tvHeaderTitle.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        header.layoutHeaderNormalButtons.alpha = 0f
        header.layoutHeaderNormalButtons.visibility = View.VISIBLE
        header.layoutHeaderNormalButtons.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        performSearchQuery("")
    }

    private fun setupIslandHeaderView() {
        if (!::binding.isInitialized) return
        
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24 * resources.displayMetrics.density
            setColor(accentColor)
        }
        binding.layoutIslandHeader.islandHeaderItem.background = bg

        binding.layoutIslandHeader.btnHeaderSearch.setOnClickListener { openHeaderSearch() }
        binding.layoutIslandHeader.btnHeaderSearchClear.setOnClickListener { closeHeaderSearch() }

        binding.layoutIslandHeader.etHeaderSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.layoutIslandHeader.btnHeaderStartChat.setOnClickListener { onStartChatClicked() }
        binding.layoutIslandHeader.ivHeaderAvatar.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
        }
        binding.layoutIslandHeader.tvHeaderInitials.setOnClickListener {
            binding.layoutIslandHeader.ivHeaderAvatar.performClick()
        }
        binding.layoutIslandHeader.ivHeaderAvatar.setOnLongClickListener {
            showLogoutDialog()
            true
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
                PrimeTransitions.applyOpenTransition(this)
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
        
        try {
            val currentUser = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("current_user", "") ?: ""
            val myDisplayName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${currentUser}_name", currentUser) ?: currentUser
            val payload = "DELETE_CHAT:login=$myDisplayName;name=$myDisplayName".toByteArray(Charsets.UTF_8)
            
            // Execute Bluetooth packet send and disconnect in background to avoid dropping frames
            Thread {
                try {
                    BluetoothConnectionManager.getInstance().sendPacket(targetId, 8.toByte(), payload)
                    Thread.sleep(80)
                } catch (e: Exception) {
                    Log.e("ChatListActivity", "Error sending deletion packet", e)
                } finally {
                    BluetoothConnectionManager.getInstance().disconnect(targetId)
                    BluetoothSocketHolder.removeConnection(contact.id, contact.name)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("ChatListActivity", "Error setting up deletion thread", e)
        }
        
        // Completely forget the device (unpair/removeBond) if it's a Bluetooth MAC address
        try {
            val bManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bAdapter = bManager.adapter
            if (bAdapter != null && bAdapter.isEnabled && BluetoothAdapter.checkBluetoothAddress(contact.id)) {
                val device = bAdapter.getRemoteDevice(contact.id)
                val removeBondMethod = device.javaClass.getMethod("removeBond")
                removeBondMethod.invoke(device)
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Remove bond ignored: ${e.message}")
        }
        
        ChatHistoryManager.deleteHistoryCompletely(this, targetName, targetId)
        saveContacts()
        
        if (::adapter.isInitialized) {
            adapter.updateList(allChats)
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
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is ChatListAdapter.ChatViewHolder) {
                    viewHolder.resetReveal()
                }
            }
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder is ChatListAdapter.ChatViewHolder) {
                    if (viewHolder.isRevealed) viewHolder.resetReveal()
                }
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val currentList = adapter.getChatList()
                if (position >= 0 && position < currentList.size) {
                    deleteContact(currentList[position])
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
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition().setDuration(200))
            if (::adapter.isInitialized && adapter.isSearchActive) {
                if (allChats.isEmpty()) {
                    binding.tvEmptyState.text = if (currentSearchQuery.isEmpty()) "Начните вводить текст для поиска" else "По запросу «$currentSearchQuery» ничего не найдено"
                    binding.layoutIslandHeader.vStartChatHighlightRing.visibility = View.GONE
                    binding.layoutIslandHeader.vStartChatHighlightRing.clearAnimation()
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.layoutIslandHeader.vStartChatHighlightRing.visibility = View.GONE
                    binding.layoutIslandHeader.vStartChatHighlightRing.clearAnimation()
                }
            } else {
                binding.tvEmptyState.text = "Прайма не видно, начните с кем нибудь общение!"
                if (chatListState.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    
                    binding.layoutIslandHeader.vStartChatHighlightRing.visibility = View.VISIBLE
                    val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.4f)
                    val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.4f)
                    val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
                    
                    val pulse = ObjectAnimator.ofPropertyValuesHolder(binding.layoutIslandHeader.vStartChatHighlightRing, scaleX, scaleY, alpha)
                    pulse.duration = 1500
                    pulse.repeatCount = ObjectAnimator.INFINITE
                    pulse.start()
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.layoutIslandHeader.vStartChatHighlightRing.visibility = View.GONE
                    binding.layoutIslandHeader.vStartChatHighlightRing.clearAnimation()
                }
            }
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
    private fun formatSmartTime(timestamp: Long): String {
        if (timestamp <= 0) return "сейчас"
        val nowCal = java.util.Calendar.getInstance()
        val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = nowCal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)

        if (isToday) {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }

        val yestCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yestCal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                yestCal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Вчера"
        }

        val isSameYear = nowCal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR)
        return if (isSameYear) {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        } else {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun loadContacts(): List<ChatModel> {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val myName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null) ?: ""

        val json = sharedPrefs.getString("persisted_chats", null)
        val tempList = ArrayList<Pair<ChatModel, Long>>()
        if (!json.isNullOrEmpty() && json != "[]") {
            try {
                val array = JSONArray(json)
                val newArray = JSONArray()
                val now = System.currentTimeMillis()
                for (i in 0 until array.length()) {
                    try {
                        val item = array.opt(i)
                        if (item is JSONObject) {
                            val idStr = item.optString("id", System.currentTimeMillis().toString() + i)
                            val nameStr = item.optString("name", "Контакт")

                            val savedName = sharedPrefs.getString("contact_name_$idStr", null)
                                ?: sharedPrefs.getString("${idStr}_name", null)
                            val rawFinalName = if (!savedName.isNullOrEmpty() && savedName != "1") savedName else nameStr
                            val finalName = if (BluetoothAdapter.checkBluetoothAddress(rawFinalName)) "Собеседник" else rawFinalName

                            if (myName.isNotEmpty() && finalName.equals(myName, ignoreCase = true)) {
                                continue
                            }

                            val hasHistory = try {
                                val hasByName = ChatHistoryManager.hasHistory(this@ChatListActivity, finalName)
                                if (hasByName) true else ChatHistoryManager.hasHistory(this@ChatListActivity, idStr)
                            } catch (_: Exception) {
                                false
                            }

                            val isUnknownName = finalName.isBlank() || 
                                                finalName.equals("Неизвестное", ignoreCase = true) || 
                                                finalName.equals("Unknown", ignoreCase = true) || 
                                                finalName.equals("null", ignoreCase = true) ||
                                                finalName.equals("1", ignoreCase = true) ||
                                                finalName.equals("Контакт", ignoreCase = true)
                            val hasNoMessages = item.optString("lastMessage", "").isBlank() && !hasHistory
                            if (isUnknownName && hasNoMessages) {
                                continue
                            }

                            newArray.put(item)

                            val rawAvatar = if (item.isNull("avatarUri")) null else item.optString("avatarUri")
                            val typingUntil = item.optLong("typingUntil", 0L)
                            val isTyping = typingUntil > now
                            val rawActState = item.optString("activityState", "IDLE")
                            val actState = if ("TYPING".equals(rawActState, ignoreCase = true) && !isTyping) "IDLE" else rawActState

                            val savedAvatar = sharedPrefs.getString("contact_avatar_$idStr", null)
                                ?: sharedPrefs.getString("${idStr}_avatar", null)
                                ?: sharedPrefs.getString("${idStr}_avatarUri", null)
                            val finalAvatar = if (!savedAvatar.isNullOrEmpty()) savedAvatar else (if (rawAvatar.isNullOrEmpty()) null else rawAvatar)

                            val isConnectedInManager = BluetoothConnectionManager.getInstance().isConnected(idStr) ||
                                BluetoothConnectionManager.getInstance().isConnected(finalName)
                            val isSocketConnected = BluetoothSocketHolder.isConnectedWith(idStr, finalName) || isConnectedInManager

                            val savedStatusStr = item.optString("onlineStatus", "OFFLINE")
                            val realOnlineStatus = if (isSocketConnected || "ONLINE".equals(savedStatusStr, ignoreCase = true)) {
                                OnlineStatus.ONLINE
                            } else {
                                OnlineStatus.OFFLINE
                            }

                            val finalLastMsg = item.optString("lastMessage", "")
                            val finalTime = item.optString("time", "сейчас")
                            val finalMessageStatus = try { MessageStatus.valueOf(item.optString("messageStatus", "NONE")) } catch(e: Exception) { MessageStatus.NONE }
                            val lastTimestamp = item.optLong("timestamp", 0L)

                            val finalUnreadCount = item.optInt("unreadCount", 0)

                            val chat = ChatModel(
                                idStr,
                                finalName,
                                finalLastMsg,
                                finalTime,
                                if (finalAvatar.isNullOrEmpty()) null else finalAvatar,
                                realOnlineStatus,
                                finalMessageStatus,
                                finalUnreadCount,
                                item.optBoolean("isMuted", false),
                                isTyping,
                                typingUntil,
                                actState
                            )
                            tempList.add(Pair(chat, lastTimestamp))
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                if (newArray.length() != array.length()) {
                    sharedPrefs.edit().putString("persisted_chats", newArray.toString()).apply()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        tempList.sortByDescending { it.second }
        return tempList.map { it.first }
    }
    private fun showNameEditDialog() {
        isNameEditDialogVisible.value = true
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

        performDiscovery()

        try {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            }
            enableDiscoverableLauncher.launch(discoverableIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    @SuppressLint("MissingPermission")
    private fun performDiscovery() {
        val btAdapter = bluetoothAdapter ?: return
        if (!btAdapter.isEnabled) return

        primeDevices.clear()
        pairedDevices.clear()
        try {
            btAdapter.bondedDevices?.let { bonded ->
                pairedDevices.addAll(bonded)
                for (dev in bonded) {
                    val devName = try { dev.name } catch (_: Exception) { null }
                    val devAddr = dev.address
                    val devUuids = try { dev.uuids } catch (_: Exception) { null }
                    val hasPrimeUuid = devUuids?.any { it.uuid.toString().equals(primeUuid.toString(), ignoreCase = true) } == true
                    val isSavedInChats = chatListState.any { 
                        it.id.equals(devAddr, ignoreCase = true) || (devName != null && it.name.equals(devName, ignoreCase = true)) 
                    }

                    if (isSavedInChats || devName?.contains("Prime", ignoreCase = true) == true || hasPrimeUuid) {
                        handlePrimeDeviceFound(dev, devName)
                    }
                    try { dev.fetchUuidsWithSdp() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        discoveredDevices.clear()
        deviceRssiMap.clear()
        isScanningState.value = true

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
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

                    runOnUiThread {
                        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            try { socket.close() } catch (e: Exception) {}
                            return@runOnUiThread
                        }

                        if (!isIncomingConnectionDialogVisible.value) {
                            triggerPrimeFoundVibration()
                            incomingSocket = socket
                            incomingDeviceName = devName
                            incomingDeviceMac = devMac
                            isIncomingConnectionDialogVisible.value = true
                        } else {
                            BluetoothSocketHolder.registerConnection(devMac, devName, socket, null)
                            ChatListNotifier.notifyChanged()
                        }
                    }
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
        PrimeTransitions.applyCloseTransition(this)
    }
}
