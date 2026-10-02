package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.messenger.prime.databinding.ActivityPersonInformationBinding
import com.messenger.prime.databinding.ItemGalleryMediaBinding
import com.messenger.prime.databinding.ItemPersonDateHeaderBinding
import com.messenger.prime.databinding.ItemPersonFileRowBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class PersonInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonInformationBinding

    private var targetUsername: String = ""
    private var deviceAddress: String? = null
    private var avatarUriStr: String? = null
    private var isOnline: Boolean = false

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.messenger.prime.STATUS_UPDATED" -> {
                    updateLiveStatus()
                }
                "com.messenger.prime.AVATAR_UPDATED", "com.messenger.prime.AVATAR_CHANGED", "com.messenger.prime.NAME_CHANGED" -> {
                    setupHeaderUi()
                }
                "com.messenger.prime.CHAT_DELETED" -> {
                    Toast.makeText(this@PersonInformationActivity, "Чат удален", Toast.LENGTH_SHORT).show()
                    supportFinishAfterTransition()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        PrimeTransitions.setupActivityTransitions(this)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        binding = ActivityPersonInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val extraPadding = (12 * density).toInt()
            val topInset = if (systemBars.top > 0) systemBars.top else (24 * density).toInt()

            binding.layoutWithPhoto.setPadding(
                binding.layoutWithPhoto.paddingLeft,
                topInset + extraPadding,
                binding.layoutWithPhoto.paddingRight,
                extraPadding
            )
            binding.nestedScrollView.setPadding(
                binding.nestedScrollView.paddingLeft,
                binding.nestedScrollView.paddingTop,
                binding.nestedScrollView.paddingRight,
                systemBars.bottom
            )
            insets
        }

        targetUsername = intent.getStringExtra("EXTRA_CHAT_NAME") ?: "Пользователь"
        deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS")
        avatarUriStr = intent.getStringExtra("EXTRA_AVATAR_URI")
        isOnline = intent.getBooleanExtra("EXTRA_IS_ONLINE", false)

        // Set transition names for smooth flying animation
        ViewCompat.setTransitionName(binding.btnBack, "transition_back_btn")
        ViewCompat.setTransitionName(binding.ivPhotoCard, "transition_avatar")
        ViewCompat.setTransitionName(binding.tvUserNameWP, "transition_name")

        PrimeTransitions.attachSlidr(this)

        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.STATUS_UPDATED")
            addAction("com.messenger.prime.AVATAR_UPDATED")
            addAction("com.messenger.prime.CHAT_DELETED")
            addAction("com.messenger.prime.AVATAR_CHANGED")
            addAction("com.messenger.prime.NAME_CHANGED")
        }
        ContextCompat.registerReceiver(
            this, statusUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setupThemeAndBackground()
        setupHeaderUi()
        setupLeftColumnButtons()
        updateLiveStatus()
        loadSharedMediaAndFiles()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            ColorAccentManager.tintViewTree(binding.root, ColorAccentManager.getCurrentAccentColor(this))
        }
        setupHeaderUi()
        updateLiveStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(statusUpdateReceiver)
        } catch (ignored: Exception) {}
    }

    private fun setupThemeAndBackground() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        binding.nestedScrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.prime_base))
        binding.composeHeaderBackground.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PrimeTheme(darkTheme = darkTheme) {
                    AnimatedBackground(darkTheme = darkTheme, ignoreSettingsToggle = false)
                }
            }
        }
    }

    private fun setupHeaderUi() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val targetAddr = deviceAddress ?: ""

        val contactName = if (targetAddr.isNotEmpty()) {
            sharedPrefs.getString("contact_name_$targetAddr", null) ?: targetUsername
        } else targetUsername
        val displayContactName = if (BluetoothAdapter.checkBluetoothAddress(contactName)) "Собеседник" else contactName
        binding.tvUserNameWP.text = displayContactName

        val contactAvatar = if (targetAddr.isNotEmpty()) {
            sharedPrefs.getString("contact_avatar_$targetAddr", null) ?: avatarUriStr
        } else avatarUriStr

        avatarUriStr = contactAvatar

        var photoLoaded = false
        val radiusPx = (14 * resources.displayMetrics.density).toInt()

        if (!avatarUriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(avatarUriStr)
                val file = if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else null
                val signatureKey = ObjectKey(if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis())
                val isGif = avatarUriStr!!.lowercase().contains(".gif")

                if (isGif) {
                    Glide.with(this)
                        .asGif()
                        .load(uri)
                        .centerCrop()
                        .signature(signatureKey)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivPhotoCard)
                    photoLoaded = true
                } else {
                    Glide.with(this)
                        .load(uri)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivPhotoCard)
                    photoLoaded = true
                }
            } catch (ignored: Exception) {}
        }

        if (!photoLoaded) {
            val possibleFiles = listOfNotNull(
                deviceAddress?.let { File(filesDir, "rec_avatar_${it}.gif") },
                deviceAddress?.let { File(filesDir, "rec_avatar_${it}.jpg") },
                File(filesDir, "rec_avatar_${contactName}.gif"),
                File(filesDir, "rec_avatar_${contactName}.jpg")
            )

            for (file in possibleFiles) {
                if (file.exists()) {
                    val signatureKey = ObjectKey(file.lastModified())
                    if (file.name.lowercase().endsWith(".gif")) {
                        Glide.with(this)
                            .asGif()
                            .load(file)
                            .centerCrop()
                            .signature(signatureKey)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .placeholder(R.drawable.ic_person)
                            .into(binding.ivPhotoCard)
                    } else {
                        Glide.with(this)
                            .load(file)
                            .transform(CenterCrop(), RoundedCorners(radiusPx))
                            .signature(signatureKey)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .placeholder(R.drawable.ic_person)
                            .into(binding.ivPhotoCard)
                    }
                    avatarUriStr = Uri.fromFile(file).toString()
                    photoLoaded = true
                    break
                }
            }
        }

        if (!photoLoaded) {
            binding.ivPhotoCard.setImageResource(R.drawable.ic_person)
        }

        val avatarClickListener = View.OnClickListener {
            openAvatarInMediaPlayer()
        }
        binding.photoCard.setOnClickListener(avatarClickListener)
        binding.ivPhotoCard.setOnClickListener(avatarClickListener)
    }

    private fun openAvatarInMediaPlayer() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val targetAddr = deviceAddress ?: ""
        val contactName = if (targetAddr.isNotEmpty()) {
            sharedPrefs.getString("contact_name_$targetAddr", null) ?: targetUsername
        } else targetUsername

        val uriToPass = avatarUriStr ?: run {
            val possibleFiles = listOfNotNull(
                deviceAddress?.let { File(filesDir, "rec_avatar_${it}.gif") },
                deviceAddress?.let { File(filesDir, "rec_avatar_${it}.jpg") },
                File(filesDir, "rec_avatar_${contactName}.gif"),
                File(filesDir, "rec_avatar_${contactName}.jpg"),
                File(filesDir, "rec_avatar_${targetUsername}.gif"),
                File(filesDir, "rec_avatar_${targetUsername}.jpg")
            )
            possibleFiles.firstOrNull { it.exists() }?.let { Uri.fromFile(it).toString() }
        }

        if (uriToPass != null) {
            val displayContactName = if (BluetoothAdapter.checkBluetoothAddress(contactName)) "Собеседник" else contactName

            val msg = ChatMessage(
                "Аватар профиля",
                "В прайме!",
                displayContactName,
                false,
                null,
                0L,
                uriToPass,
                "contact_avatar_preview"
            )
            MediaPlayerActivity.setSharedMediaList(listOf(msg), 0)
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
        } else {
            Toast.makeText(this, "Фотография не установлена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLiveStatus() {
        val targetAddr = deviceAddress ?: targetUsername
        val isConnected = BluetoothConnectionManager.getInstance().isConnected(targetAddr)
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val state = sharedPrefs.getString("activity_state_$targetUsername", "IDLE") ?: "IDLE"
        val typingUntil = sharedPrefs.getLong("typing_until_$targetUsername", 0L)
        val now = System.currentTimeMillis()

        val isTyping = typingUntil > now || "TYPING".equals(state, ignoreCase = true)

        val statusText = when {
            !isConnected -> {
                val lastSeen = sharedPrefs.getLong("last_seen_$targetUsername", 0L)
                if (lastSeen > 0) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "был(а) в " + sdf.format(Date(lastSeen))
                } else {
                    "не в сети"
                }
            }
            isTyping -> "печатает..."
            "VIEWING_PHOTO".equals(state, ignoreCase = true) -> "смотрит фото"
            "VIEWING_VIDEO".equals(state, ignoreCase = true) -> "смотрит видео"
            "VIEWING_FILE".equals(state, ignoreCase = true) -> "смотрит файл"
            "SENDING_PHOTO".equals(state, ignoreCase = true) || "SENDING_VIDEO".equals(state, ignoreCase = true) || "SENDING_FILE".equals(state, ignoreCase = true) || "SENDING_MEDIA".equals(state, ignoreCase = true) -> "Отправка медиа..."
            else -> "в сети"
        }

        binding.tvStatusWP.text = statusText
        binding.tvStatusWP.setTextColor(
            if (isConnected) ContextCompat.getColor(this, R.color.prime_success)
            else ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun setupLeftColumnButtons() {
        // 1. Назад
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 2. Просмотр фото
        binding.btnViewPhoto.setOnClickListener {
            openAvatarInMediaPlayer()
        }

        // 3. Отключиться
        binding.btnDisconnect.setOnClickListener {
            val targetAddr = deviceAddress ?: targetUsername
            BluetoothConnectionManager.getInstance().disconnect(targetAddr)

            val disconnectIntent = Intent("com.messenger.prime.DISCONNECT_REQUESTED").setPackage(packageName)
            sendBroadcast(disconnectIntent)

            updateLiveStatus()
            Toast.makeText(this, "Подключение отключено", Toast.LENGTH_SHORT).show()
        }

        // 4. Удалить чат
        binding.btnDeleteChat.setOnClickListener {
            showBlurDeleteChatDialog()
        }
    }

    private fun showBlurDeleteChatDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_blur_delete_chat, null)
        val blurCard = dialogView.findViewById<BlurView>(R.id.blurDialogCard)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvBlurDialogTitle)
        val btnNo = dialogView.findViewById<View>(R.id.btnBlurDialogNo)
        val btnYes = dialogView.findViewById<View>(R.id.btnBlurDialogYes)

        val displayTitleName = if (BluetoothAdapter.checkBluetoothAddress(targetUsername)) "собеседником" else targetUsername
        tvTitle.text = "Удалить чат с $displayTitleName?"

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.setOnShowListener {
            val decorView = window.decorView as? ViewGroup
            if (decorView != null && blurCard != null) {
                val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RenderEffectBlur()
                } else {
                    RenderScriptBlur(this)
                }
                blurCard.setupWith(decorView, algorithm)
                    .setBlurRadius(20f)
                    .setOverlayColor(Color.parseColor("#80154B87"))
                    .setBlurAutoUpdate(true)
            }
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnYes.setOnClickListener {
            dialog.dismiss()
            performCompleteChatDeletion()
        }

        dialog.show()
    }

    private fun performCompleteChatDeletion() {
        val targetAddr = deviceAddress ?: targetUsername
        if (BluetoothConnectionManager.getInstance().isConnected(targetAddr)) {
            val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val currentUser = sp.getString("current_user", "") ?: ""
            val deletionPayload = "DELETE_CHAT:login=$currentUser;name=$currentUser"
            BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 8.toByte(), deletionPayload.toByteArray(Charsets.UTF_8))
            
            Handler(Looper.getMainLooper()).postDelayed({
                finalizeChatDeletion()
            }, 200)
        } else {
            finalizeChatDeletion()
        }
    }

    @SuppressLint("MissingPermission")
    private fun finalizeChatDeletion() {
        val targetAddr = deviceAddress ?: targetUsername

        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && !deviceAddress.isNullOrEmpty() && BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                val device = adapter.getRemoteDevice(deviceAddress)
                if (device != null && device.bondState == BluetoothDevice.BOND_BONDED) {
                    try {
                        val removeBond = device.javaClass.getMethod("removeBond")
                        removeBond.invoke(device)
                        Log.d("ChatDeletion", "Successfully unbonded device: $deviceAddress")
                    } catch (e: Exception) {
                        Log.e("ChatDeletion", "Failed to invoke removeBond", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PersonInfo", "Failed to remove bond", e)
        }

        BluetoothConnectionManager.getInstance().disconnect(targetAddr)
        if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
            PrimeBluetoothService.stopService(this)
        }

        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress)

        val chatDeletedIntent = Intent("com.messenger.prime.CHAT_DELETED").apply {
            `package` = packageName
        }
        sendBroadcast(chatDeletedIntent)

        Toast.makeText(this, "Переписка и устройство полностью удалены", Toast.LENGTH_SHORT).show()

        val mainIntent = Intent(this, ChatListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(mainIntent)
        PrimeTransitions.applyOpenTransition(this)
        finish()
    }

    override fun finish() {
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }

    private fun formatDateSection(timestamp: Long): String {
        if (timestamp <= 0) return "Ранее"
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val sameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        if (sameDay) return "Сегодня"

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sameYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        if (sameYesterday) return "Вчера"

        val ruLocale = Locale.forLanguageTag("ru-RU")
        val sdf = if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMMM", ruLocale)
        } else {
            SimpleDateFormat("d MMMM yyyy", ruLocale)
        }
        return sdf.format(Date(timestamp))
    }

    private fun loadSharedMediaAndFiles() {
        Executors.newSingleThreadExecutor().execute {
            val lookupKey = if (!deviceAddress.isNullOrEmpty()) deviceAddress!! else targetUsername
            var history = ChatHistoryManager.loadMessages(this, lookupKey)
            if (history.isEmpty() && targetUsername.isNotEmpty()) {
                history = ChatHistoryManager.loadMessages(this, targetUsername)
            }
            if (history.isEmpty() && !deviceAddress.isNullOrEmpty()) {
                val storageKey = ChatHistoryManager.getStorageKey(this, deviceAddress!!)
                if (storageKey.isNotEmpty()) {
                    history = ChatHistoryManager.loadMessages(this, storageKey)
                }
            }
            if (history.isEmpty() && targetUsername.isNotEmpty()) {
                val storageKey = ChatHistoryManager.getStorageKey(this, targetUsername)
                if (storageKey.isNotEmpty()) {
                    history = ChatHistoryManager.loadMessages(this, storageKey)
                }
            }

            val mediaGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()
            val filesGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()

            var totalMediaCount = 0
            var totalFilesCount = 0

            for (msg in history) {
                if (msg.isMultiMedia || (!msg.imagePath.isNullOrEmpty() && msg.imagePath.startsWith("MULTI:"))) {
                    val subItems = msg.getMediaItems()
                    for (it in subItems) {
                        val subMsg = ChatMessage(msg.text, msg.time, msg.senderLogin, msg.isOutgoing, null, msg.timestamp, it.path, msg.messageId)
                        subMsg.messageType = if (it.isVideo) ChatMessage.MessageType.VIDEO else ChatMessage.MessageType.IMAGE
                        subMsg.videoDuration = it.durationStr
                        val dateLabel = formatDateSection(subMsg.timestamp)
                        mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(subMsg)
                        totalMediaCount++
                    }
                } else if (msg.isFile || msg.messageType == ChatMessage.MessageType.FILE || !msg.fileName.isNullOrEmpty()) {
                    val dateLabel = formatDateSection(msg.timestamp)
                    filesGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                    totalFilesCount++
                } else if (!msg.imagePath.isNullOrEmpty() || msg.imageBitmap != null || msg.messageType == ChatMessage.MessageType.IMAGE || msg.messageType == ChatMessage.MessageType.VIDEO) {
                    val dateLabel = formatDateSection(msg.timestamp)
                    mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                    totalMediaCount++
                }
            }

            // Build media adapter list
            val mediaAdapterItems = mutableListOf<MediaListItem>()
            val mediaClickList = mutableListOf<ChatMessage>()
            for ((dateTitle, list) in mediaGrouped) {
                mediaAdapterItems.add(MediaListItem.Header(dateTitle))
                for (m in list) {
                    mediaAdapterItems.add(MediaListItem.Item(m))
                    mediaClickList.add(m)
                }
            }

            // Build files adapter list
            val fileAdapterItems = mutableListOf<FileListItem>()
            for ((dateTitle, list) in filesGrouped) {
                fileAdapterItems.add(FileListItem.Header(dateTitle))
                for (f in list) {
                    fileAdapterItems.add(FileListItem.Item(f))
                }
            }

            val finalMediaCount = totalMediaCount
            val finalFilesCount = totalFilesCount

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                binding.tvMediaCount.text = finalMediaCount.toString()
                if (finalMediaCount == 0) {
                    binding.tvEmptyMedia.visibility = View.VISIBLE
                    binding.rvSharedMedia.visibility = View.GONE
                } else {
                    binding.tvEmptyMedia.visibility = View.GONE
                    binding.rvSharedMedia.visibility = View.VISIBLE

                    val mediaAdapter = PersonMediaAdapter(mediaAdapterItems) { clickedMsg ->
                        val initialIdx = mediaClickList.indexOf(clickedMsg).coerceAtLeast(0)
                        ChatAdapter.showFullScreenMedia(this, null, clickedMsg, mediaClickList, initialIdx)
                    }
                    val gridManager = GridLayoutManager(this, 3)
                    gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (mediaAdapter.getItemViewType(position) == PersonMediaAdapter.TYPE_HEADER) 3 else 1
                        }
                    }
                    binding.rvSharedMedia.layoutManager = gridManager
                    binding.rvSharedMedia.adapter = mediaAdapter
                }

                binding.tvFilesCount.text = finalFilesCount.toString()
                if (finalFilesCount == 0) {
                    binding.tvEmptyFiles.visibility = View.VISIBLE
                    binding.rvSharedFiles.visibility = View.GONE
                } else {
                    binding.tvEmptyFiles.visibility = View.GONE
                    binding.rvSharedFiles.visibility = View.VISIBLE
                    binding.rvSharedFiles.layoutManager = LinearLayoutManager(this)
                    binding.rvSharedFiles.adapter = PersonFilesAdapter(fileAdapterItems) { clickedFileMsg ->
                        val filePath = clickedFileMsg.imagePath ?: clickedFileMsg.fileName ?: ""
                        ChatAdapter.openFile(this, filePath)
                    }
                }
            }
        }
    }

    sealed class MediaListItem {
        data class Header(val title: String) : MediaListItem()
        data class Item(val msg: ChatMessage) : MediaListItem()
    }

    private class PersonMediaAdapter(
        private val items: List<MediaListItem>,
        private val onItemClick: (ChatMessage) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is MediaListItem.Header -> TYPE_HEADER
                is MediaListItem.Item -> TYPE_ITEM
            }
        }

        class HeaderViewHolder(val binding: ItemPersonDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        class ItemViewHolder(val binding: ItemGalleryMediaBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemPersonDateHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(ItemGalleryMediaBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is MediaListItem.Header -> {
                    (holder as HeaderViewHolder).binding.tvDateTitle.text = item.title
                }
                is MediaListItem.Item -> {
                    val h = holder as ItemViewHolder
                    val msg = item.msg
                    val path = msg.imagePath

                    if (msg.isVideo) {
                        h.binding.layoutVideoBadge.visibility = View.VISIBLE
                        h.binding.tvVideoDuration.text = msg.videoDuration ?: "00:00"

                        if (!path.isNullOrEmpty()) {
                            Glide.with(h.itemView.context)
                                .asBitmap()
                                .load(path)
                                .centerCrop()
                                .placeholder(R.drawable.ic_video)
                                .error(R.drawable.ic_video)
                                .into(h.binding.ivGalleryThumbnail)
                        } else {
                            h.binding.ivGalleryThumbnail.setImageResource(R.drawable.ic_video)
                        }
                    } else {
                        h.binding.layoutVideoBadge.visibility = View.GONE

                        if (!path.isNullOrEmpty()) {
                            ChatAdapter.loadMediaImageIntoView(h.itemView.context, path, h.binding.ivGalleryThumbnail)
                        } else if (msg.imageBitmap != null) {
                            h.binding.ivGalleryThumbnail.setImageBitmap(msg.imageBitmap)
                        } else {
                            h.binding.ivGalleryThumbnail.setImageResource(R.drawable.ic_photo)
                        }
                    }

                    h.itemView.setOnClickListener { onItemClick(msg) }
                }
            }
        }

        override fun getItemCount() = items.size
    }

    sealed class FileListItem {
        data class Header(val title: String) : FileListItem()
        data class Item(val msg: ChatMessage) : FileListItem()
    }

    private class PersonFilesAdapter(
        private val items: List<FileListItem>,
        private val onItemClick: (ChatMessage) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is FileListItem.Header -> TYPE_HEADER
                is FileListItem.Item -> TYPE_ITEM
            }
        }

        class HeaderViewHolder(val binding: ItemPersonDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        class ItemViewHolder(val binding: ItemPersonFileRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemPersonDateHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(ItemPersonFileRowBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is FileListItem.Header -> {
                    (holder as HeaderViewHolder).binding.tvDateTitle.text = item.title
                }
                is FileListItem.Item -> {
                    val h = holder as ItemViewHolder
                    val msg = item.msg
                    val resolvedName = msg.fileName?.ifEmpty { null }
                        ?: msg.imagePath?.let { File(it).name }
                        ?: "Документ"
                    h.binding.tvFileName.text = resolvedName
                    
                    val filePath = msg.imagePath ?: ""
                    val sizeBytes = if (msg.fileSize > 0) msg.fileSize else (if (filePath.isNotEmpty()) File(filePath).length() else 0L)
                    h.binding.tvFileSize.text = ChatAdapter.formatFileSize(sizeBytes)
                    h.itemView.setOnClickListener { onItemClick(msg) }
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
