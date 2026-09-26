package com.messenger.prime

import android.bluetooth.BluetoothManager
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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
                "com.messenger.prime.AVATAR_UPDATED" -> {
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
        super.onCreate(savedInstanceState)

        setupEdgeToEdge()

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

        // Attach Slidr for left-to-right swipe dismiss
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                supportFinishAfterTransition()
            }
        })

        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.STATUS_UPDATED")
            addAction("com.messenger.prime.AVATAR_UPDATED")
            addAction("com.messenger.prime.CHAT_DELETED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusUpdateReceiver, filter)
        }

        setupThemeAndBackground()
        setupHeaderUi()
        setupLeftColumnButtons()
        updateLiveStatus()
        loadSharedMediaAndFiles()
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
        binding.tvUserNameWP.text = targetUsername

        var photoLoaded = false
        if (!avatarUriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(avatarUriStr)
                if ("file" == uri.scheme && uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists()) {
                        binding.ivPhotoCard.setImageURI(uri)
                        photoLoaded = true
                    }
                } else if (avatarUriStr!!.startsWith("content://")) {
                    binding.ivPhotoCard.setImageURI(uri)
                    photoLoaded = true
                }
            } catch (ignored: Exception) {}
        }

        if (!photoLoaded) {
            val localAvatarFile = File(filesDir, "avatar_${targetUsername}.jpg")
            if (localAvatarFile.exists()) {
                val bmp = BitmapFactory.decodeFile(localAvatarFile.absolutePath)
                if (bmp != null) {
                    binding.ivPhotoCard.setImageBitmap(bmp)
                    photoLoaded = true
                }
            } else if (!deviceAddress.isNullOrEmpty()) {
                val devAvatarFile = File(filesDir, "avatar_${deviceAddress}.jpg")
                if (devAvatarFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(devAvatarFile.absolutePath)
                    if (bmp != null) {
                        binding.ivPhotoCard.setImageBitmap(bmp)
                        photoLoaded = true
                    }
                }
            }
        }

        if (!photoLoaded) {
            binding.ivPhotoCard.setImageResource(R.drawable.ic_person)
        }

        // Tapping avatar opens full photo in media player / photo viewer
        val avatarClickListener = View.OnClickListener {
            openAvatarInMediaPlayer()
        }
        binding.photoCard.setOnClickListener(avatarClickListener)
        binding.ivPhotoCard.setOnClickListener(avatarClickListener)
    }

    private fun openAvatarInMediaPlayer() {
        val uriToPass = avatarUriStr ?: run {
            val file = File(filesDir, "avatar_${targetUsername}.jpg")
            if (file.exists()) Uri.fromFile(file).toString() else null
        }
        if (uriToPass != null) {
            val intent = Intent(this, PhotoViewActivity::class.java).apply {
                putExtra("EXTRA_URI", uriToPass)
                putExtra("EXTRA_PHOTO_URI", uriToPass)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Фотография отсутствует", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLiveStatus() {
        val isConnected = BluetoothSocketHolder.isConnectedWith(deviceAddress, targetUsername)
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
            "SENDING_PHOTO".equals(state, ignoreCase = true) || "SENDING_MEDIA".equals(state, ignoreCase = true) -> "отправляет фото..."
            "SENDING_VIDEO".equals(state, ignoreCase = true) -> "отправляет видео..."
            "SENDING_FILE".equals(state, ignoreCase = true) -> "отправляет файл..."
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
            supportFinishAfterTransition()
        }

        // 2. Просмотр фото
        binding.btnViewPhoto.setOnClickListener {
            openAvatarInMediaPlayer()
        }

        // 3. Отключиться
        binding.btnDisconnect.setOnClickListener {
            BluetoothSocketHolder.clearSocket()
            PrimeBluetoothService.stopService(this)

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

        tvTitle.text = "Удалить чат с $targetUsername?"

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
        val threadObj = BluetoothSocketHolder.getConnectedThreadInstance()
        if (threadObj is ChatPersonActivity.ConnectedThread && threadObj.isAlive) {
            val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val currentUser = sp.getString("current_user", "") ?: ""
            val deletionPayload = "DELETE_CHAT:login=$currentUser;name=$currentUser"
            threadObj.sendPacket(8.toByte(), deletionPayload.toByteArray(Charsets.UTF_8))
            try { Thread.sleep(100) } catch (ignored: Exception) {}
        }

        BluetoothSocketHolder.clearSocket()
        PrimeBluetoothService.stopService(this)

        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            val bAdapter = bluetoothManager?.adapter
            if (bAdapter != null && bAdapter.isEnabled && !deviceAddress.isNullOrEmpty()) {
                val device = bAdapter.getRemoteDevice(deviceAddress)
                val removeBondMethod = device.javaClass.getMethod("removeBond")
                removeBondMethod.invoke(device)
            }
        } catch (e: Exception) {
            Log.w("PersonInfo", "Failed to remove bond", e)
        }

        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress)

        val chatDeletedIntent = Intent("com.messenger.prime.CHAT_DELETED").setPackage(packageName)
        sendBroadcast(chatDeletedIntent)

        Toast.makeText(this, "Переписка и устройство полностью удалены", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
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
        val history = ChatHistoryManager.loadMessages(this, targetUsername)

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
            } else if (!msg.imagePath.isNullOrEmpty() || msg.imageBitmap != null || msg.messageType == ChatMessage.MessageType.IMAGE || msg.messageType == ChatMessage.MessageType.VIDEO) {
                val dateLabel = formatDateSection(msg.timestamp)
                mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                totalMediaCount++
            } else if (msg.isFile || msg.messageType == ChatMessage.MessageType.FILE || !msg.fileName.isNullOrEmpty()) {
                val dateLabel = formatDateSection(msg.timestamp)
                filesGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                totalFilesCount++
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

        binding.tvMediaCount.text = totalMediaCount.toString()
        if (totalMediaCount == 0) {
            binding.tvEmptyMedia.visibility = View.VISIBLE
            binding.rvSharedMedia.visibility = View.GONE
        } else {
            binding.tvEmptyMedia.visibility = View.GONE
            binding.rvSharedMedia.visibility = View.VISIBLE

            val mediaAdapter = PersonMediaAdapter(mediaAdapterItems) { clickedMsg ->
                ChatAdapter.showFullScreenMedia(this, null, clickedMsg, mediaClickList, 0)
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

        // Build files adapter list
        val fileAdapterItems = mutableListOf<FileListItem>()
        for ((dateTitle, list) in filesGrouped) {
            fileAdapterItems.add(FileListItem.Header(dateTitle))
            for (f in list) {
                fileAdapterItems.add(FileListItem.Item(f))
            }
        }

        binding.tvFilesCount.text = totalFilesCount.toString()
        if (totalFilesCount == 0) {
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

                        val thumb = ChatAdapter.getVideoThumbnail(h.itemView.context, path)
                        if (thumb != null) {
                            h.binding.ivGalleryThumbnail.setImageBitmap(thumb)
                        } else if (!path.isNullOrEmpty()) {
                            ChatAdapter.loadMediaImageIntoView(h.itemView.context, path, h.binding.ivGalleryThumbnail)
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
                    h.binding.tvFileName.text = msg.fileName ?: "Документ"
                    val sizeKb = msg.fileSize / 1024
                    h.binding.tvFileSize.text = if (sizeKb > 1024) "${sizeKb / 1024} МБ" else "$sizeKb КБ"
                    h.itemView.setOnClickListener { onItemClick(msg) }
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
