package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class MediaPlayerActivity : AppCompatActivity() {

    companion object {
        var sharedMediaList: List<ChatMessage>? = null
        var sharedStartIndex: Int = 0

        @JvmStatic
        fun setSharedMediaList(list: List<ChatMessage>, startIndex: Int) {
            sharedMediaList = list
            sharedStartIndex = startIndex
        }
    }

    private lateinit var ivBlurredBackground: ImageView
    private lateinit var viewPager: ViewPager2
    private lateinit var topOverlay: View
    private lateinit var btnBack: ImageButton
    private lateinit var btnDownload: ImageButton
    private lateinit var tvSenderName: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var bottomOverlay: View
    private lateinit var tvCaption: TextView
    private lateinit var layoutSeekBarRow: View
    private lateinit var tvDuration: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var navBarProgressBar: ProgressBar
    private lateinit var tvSpeedIndicator: TextView

    private var adapter: MediaPagerAdapter? = null
    private var areControlsVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private val autoHideControlsRunnable = Runnable { hideControls() }

    private var currentPlayer: ExoPlayer? = null
    private var isCurrentVideo = false

    private var isScrubbing = false
    private var scrubTargetMs: Long = -1L
    private var isZoomed = false
    private var isSpeedingUp = false
    
    private val deletionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.messenger.prime.CHAT_DELETED" -> {
                    Toast.makeText(this@MediaPlayerActivity, "Чат был удален", Toast.LENGTH_SHORT).show()
                    finish()
                }
                "com.messenger.prime.MSG_DELETED" -> {
                    val deletedId = intent.getStringExtra("messageId") ?: return
                    val list = sharedMediaList?.toMutableList() ?: return
                    val iter = list.iterator()
                    var removedAny = false
                    while (iter.hasNext()) {
                        if (iter.next().messageId == deletedId) {
                            iter.remove()
                            removedAny = true
                        }
                    }
                    if (removedAny) {
                        if (list.isEmpty()) {
                            Toast.makeText(this@MediaPlayerActivity, "Медиафайл удален", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            sharedMediaList = list
                            adapter?.notifyDataSetChanged()
                            if (viewPager.currentItem >= list.size) {
                                viewPager.setCurrentItem(list.size - 1, false)
                            } else {
                                updateUIForPage(viewPager.currentItem)
                            }
                            Toast.makeText(this@MediaPlayerActivity, "Медиафайл удален", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (!isScrubbing) {
                currentPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition
                        val total = player.duration.coerceAtLeast(1L)
                        val progress = ((current * 1000) / total).toInt().coerceIn(0, 1000)
                        seekBar.progress = progress
                        navBarProgressBar.progress = progress
                        updateDurationText(current, total)
                    }
                }
            }
            if (isCurrentVideo) {
                handler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_media_player)

        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.CHAT_DELETED")
            addAction("com.messenger.prime.MSG_DELETED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(deletionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(deletionReceiver, filter)
        }

        initViews()
        setupInsets()

        val list = sharedMediaList ?: emptyList()
        if (list.isEmpty()) {
            Toast.makeText(this, "Нет медиафайлов", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = MediaPagerAdapter(list)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(sharedStartIndex, false)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }
        })
        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        recyclerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            false
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUIForPage(position)
            }
        })

        setupListeners()
        updateUIForPage(sharedStartIndex)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initViews() {
        ivBlurredBackground = findViewById(R.id.ivBlurredBackground)
        viewPager = findViewById(R.id.viewPager)
        topOverlay = findViewById(R.id.topOverlay)
        btnBack = findViewById(R.id.btnBack)
        btnDownload = findViewById(R.id.btnDownload)
        tvSenderName = findViewById(R.id.tvSenderName)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        bottomOverlay = findViewById(R.id.bottomOverlay)
        tvCaption = findViewById(R.id.tvCaption)
        layoutSeekBarRow = findViewById(R.id.layoutSeekBarRow)
        tvDuration = findViewById(R.id.tvDuration)
        seekBar = findViewById(R.id.seekBar)
        navBarProgressBar = findViewById(R.id.navBarProgressBar)
        tvSpeedIndicator = findViewById(R.id.tvSpeedIndicator)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(topOverlay) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = insets.top + (12 * resources.displayMetrics.density).toInt()
            view.layoutParams = lp
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomOverlay) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = insets.bottom + (12 * resources.displayMetrics.density).toInt()
            view.layoutParams = lp
            windowInsets
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        btnFullscreen.setOnClickListener {
            isZoomed = !isZoomed
            val recyclerView = viewPager.getChildAt(0) as RecyclerView
            val holder = recyclerView.findViewHolderForAdapterPosition(viewPager.currentItem) as? MediaViewHolder
            if (isZoomed) {
                holder?.playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                Toast.makeText(this@MediaPlayerActivity, "Заполнение экрана", Toast.LENGTH_SHORT).show()
            } else {
                holder?.playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                Toast.makeText(this@MediaPlayerActivity, "Вместить в экран", Toast.LENGTH_SHORT).show()
            }
        }

        btnDownload.setOnClickListener {
            val list = sharedMediaList ?: return@setOnClickListener
            val pos = viewPager.currentItem
            if (pos in list.indices) {
                val item = list[pos]
                val uriStr = item.imagePath
                var mediaUri: Uri? = null
                if (!uriStr.isNullOrEmpty()) {
                    mediaUri = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                        Uri.parse(uriStr)
                    } else {
                        val file = File(uriStr)
                        if (file.exists()) Uri.fromFile(file) else Uri.parse(uriStr)
                    }
                }
                if (mediaUri != null) {
                    Executors.newSingleThreadExecutor().execute {
                        val success = MediaSaveUtils.saveToGallery(this, mediaUri, item.isVideo || item.messageType == ChatMessage.MessageType.VIDEO)
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "Сохранено в галерею", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Не удалось сохранить файл", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Файл недоступен для сохранения", Toast.LENGTH_SHORT).show()
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isCurrentVideo && currentPlayer != null) {
                    val total = currentPlayer!!.duration.coerceAtLeast(1L)
                    val targetMs = (progress * total) / 1000
                    scrubTargetMs = targetMs
                    
                    // Быстрое и плавное перемещение во время скролла
                    currentPlayer!!.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    currentPlayer!!.seekTo(targetMs)
                    
                    updateDurationText(targetMs, total)
                    navBarProgressBar.progress = progress
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isScrubbing = true
                handler.removeCallbacks(autoHideControlsRunnable)
                handler.removeCallbacks(updateProgressRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                isScrubbing = false
                if (isCurrentVideo && currentPlayer != null && scrubTargetMs != -1L) {
                    // Точное позиционирование после отпускания ползунка
                    currentPlayer!!.setSeekParameters(SeekParameters.EXACT)
                    currentPlayer!!.seekTo(scrubTargetMs)
                }
                showControls()
                handler.post(updateProgressRunnable)
            }
        })
    }

    private fun updateUIForPage(position: Int) {
        val list = sharedMediaList ?: return
        if (position !in list.indices) return

        val item = list[position]
        
        val newSenderName = item.senderLogin ?: "Отправитель"
        val currentName = tvSenderName.text.toString()

        if (currentName != newSenderName && currentName.isNotEmpty()) {
            tvSenderName.animate()
                .translationY(50f)
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    tvSenderName.text = newSenderName
                    tvSenderName.translationY = -50f
                    tvSenderName.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } else {
            tvSenderName.text = newSenderName
            tvSenderName.translationY = 0f
            tvSenderName.alpha = 1f
        }

        val ts = item.timestamp
        val timeStr = if (ts > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)) else item.time ?: "сейчас"
        
        val currentTimeStr = tvTimestamp.text.toString()
        if (currentTimeStr != timeStr && currentTimeStr.isNotEmpty()) {
            tvTimestamp.animate()
                .translationY(50f)
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    tvTimestamp.text = timeStr
                    tvTimestamp.translationY = -50f
                    tvTimestamp.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } else {
            tvTimestamp.text = timeStr
            tvTimestamp.translationY = 0f
            tvTimestamp.alpha = 1f
        }

        val caption = item.text
        if (!caption.isNullOrEmpty() && caption != "Фото" && caption != "Фотография") {
            tvCaption.visibility = View.VISIBLE
            tvCaption.text = caption
        } else {
            tvCaption.visibility = View.GONE
        }

        isCurrentVideo = item.isVideo || item.messageType == ChatMessage.MessageType.VIDEO

        if (isCurrentVideo) {
            layoutSeekBarRow.visibility = View.VISIBLE
            btnPlayPause.visibility = if (areControlsVisible) View.VISIBLE else View.GONE
            btnPlayPause.setImageResource(R.drawable.ic_media_play)
            btnFullscreen.visibility = View.VISIBLE
        } else {
            layoutSeekBarRow.visibility = View.GONE
            btnPlayPause.visibility = View.GONE
            btnFullscreen.visibility = View.GONE
        }

        handler.removeCallbacks(updateProgressRunnable)
        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            val recyclerView = viewPager.getChildAt(0) as RecyclerView
            
            // Pause all other players
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val holder = recyclerView.getChildViewHolder(child) as? MediaViewHolder
                if (holder?.bindingAdapterPosition != position) {
                    holder?.player?.pause()
                }
            }

            val holder = recyclerView.findViewHolderForAdapterPosition(position) as? MediaViewHolder

            if (holder != null && holder.player != null) {
                onPlayerActiveAndReady(holder.player!!, position)
            } else if (!isCurrentVideo) {
                currentPlayer = null
            }
        }, 100)

        showControls()
    }

    fun onPlayerActiveAndReady(player: ExoPlayer, position: Int) {
        if (position == viewPager.currentItem) {
            currentPlayer = player
            if (isCurrentVideo) {
                updateDurationText(player.currentPosition, player.duration.coerceAtLeast(0L))
                handler.removeCallbacks(updateProgressRunnable)
                handler.post(updateProgressRunnable)
                player.playWhenReady = true
                player.play()
                btnPlayPause.setImageResource(R.drawable.ic_media_pause)
            }
        }
    }

    private fun togglePlayPause() {
        if (isCurrentVideo && currentPlayer != null) {
            if (currentPlayer!!.isPlaying) {
                currentPlayer!!.pause()
                btnPlayPause.setImageResource(R.drawable.ic_media_play)
            } else {
                currentPlayer!!.play()
                btnPlayPause.setImageResource(R.drawable.ic_media_pause)
            }
        }
        showControls()
    }

    private fun showSpeedIndicator(show: Boolean) {
        if (show) {
            tvSpeedIndicator.visibility = View.VISIBLE
            tvSpeedIndicator.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
        } else {
            tvSpeedIndicator.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tvSpeedIndicator.visibility = View.GONE
                    }
                })
                .start()
        }
    }

    private fun updateDurationText(currentMs: Long, totalMs: Long) {
        val curStr = formatMs(currentMs)
        val totalStr = formatMs(totalMs)
        val res = "$curStr/$totalStr"
        tvDuration.text = res
    }

    private fun formatMs(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalSec = ms / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    private fun showControls() {
        handler.removeCallbacks(autoHideControlsRunnable)
        if (!areControlsVisible) {
            areControlsVisible = true

            topOverlay.visibility = View.VISIBLE
            topOverlay.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()

            if (isCurrentVideo) {
                btnPlayPause.visibility = View.VISIBLE
                btnPlayPause.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
            }

            bottomOverlay.visibility = View.VISIBLE
            bottomOverlay.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()

            navBarProgressBar.animate().alpha(0f).setDuration(180)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (areControlsVisible) navBarProgressBar.visibility = View.GONE
                    }
                }).start()
        }
        handler.postDelayed(autoHideControlsRunnable, 2500L)
    }

    private fun hideControls() {
        handler.removeCallbacks(autoHideControlsRunnable)
        if (areControlsVisible) {
            areControlsVisible = false

            topOverlay.animate().alpha(0f).translationY(-30f).setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) topOverlay.visibility = View.GONE
                    }
                }).start()

            btnPlayPause.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) btnPlayPause.visibility = View.GONE
                    }
                }).start()

            bottomOverlay.animate().alpha(0f).translationY(30f).setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) bottomOverlay.visibility = View.GONE
                    }
                }).start()

            if (isCurrentVideo) {
                navBarProgressBar.visibility = View.VISIBLE
                navBarProgressBar.animate().alpha(0.4f).setDuration(220).start()
            }
        }
    }

    private fun toggleControls() {
        if (areControlsVisible) hideControls() else showControls()
    }

    override fun onPause() {
        super.onPause()
        currentPlayer?.pause()
        btnPlayPause.setImageResource(R.drawable.ic_media_play)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deletionReceiver)
        handler.removeCallbacksAndMessages(null)
        sharedMediaList = null
        
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        if (recyclerView != null) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val holder = recyclerView.getChildViewHolder(child) as? MediaViewHolder
                holder?.releasePlayer()
            }
        }
        
        currentPlayer?.release()
        currentPlayer = null
    }

    interface MediaPageInteractionListener {
        fun onSingleTap()
        fun onLongPressStart()
        fun onLongPressEnd()
    }

    inner class MediaPagerAdapter(private val items: List<ChatMessage>) : RecyclerView.Adapter<MediaViewHolder>() {

        private val interactionListener = object : MediaPageInteractionListener {
            override fun onSingleTap() {
                toggleControls()
            }

            override fun onLongPressStart() {
                if (isCurrentVideo && currentPlayer != null) {
                    isSpeedingUp = true
                    currentPlayer?.playbackParameters = PlaybackParameters(2.0f)
                    showSpeedIndicator(true)
                }
            }

            override fun onLongPressEnd() {
                if (isSpeedingUp) {
                    isSpeedingUp = false
                    currentPlayer?.playbackParameters = PlaybackParameters(1.0f)
                    showSpeedIndicator(false)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = layoutInflater.inflate(R.layout.item_media_page, parent, false)
            return MediaViewHolder(view, interactionListener)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            holder.bind(items[position], position == viewPager.currentItem)
        }

        override fun getItemCount(): Int = items.size

        override fun onViewRecycled(holder: MediaViewHolder) {
            holder.releasePlayer()
            super.onViewRecycled(holder)
        }
    }

    inner class MediaViewHolder(itemView: View, listener: MediaPageInteractionListener) : RecyclerView.ViewHolder(itemView) {
        val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        var player: ExoPlayer? = null

        init {
            var isHolding = false
            var startX = 0f
            var startY = 0f
            val holdRunnable = Runnable {
                isHolding = true
                listener.onLongPressStart()
            }

            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isHolding = false
                        handler.postDelayed(holdRunnable, 350)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(event.x - startX)
                        val dy = Math.abs(event.y - startY)
                        if (dx > 20 || dy > 20) {
                            if (!isHolding) {
                                handler.removeCallbacks(holdRunnable)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(holdRunnable)
                        if (isHolding) {
                            listener.onLongPressEnd()
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            val dx = Math.abs(event.x - startX)
                            val dy = Math.abs(event.y - startY)
                            if (dx <= 20 && dy <= 20) {
                                listener.onSingleTap()
                                v.performClick()
                            }
                        }
                        isHolding = false
                    }
                }
                true
            }
        }

        fun bind(item: ChatMessage, isActive: Boolean) {
            releasePlayer()

            val isVid = item.isVideo || item.messageType == ChatMessage.MessageType.VIDEO

            if (isVid) {
                playerView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                
                playerView.resizeMode = if (isZoomed) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT

                val uriStr = item.imagePath
                if (!uriStr.isNullOrEmpty()) {
                    val uri = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                        Uri.parse(uriStr)
                    } else {
                        val file = File(uriStr)
                        if (file.exists()) Uri.fromFile(file) else Uri.parse(uriStr)
                    }

                    player = ExoPlayer.Builder(itemView.context).build()
                    playerView.player = player
                    player!!.setMediaItem(MediaItem.fromUri(uri))
                    player!!.repeatMode = Player.REPEAT_MODE_ONE
                    player!!.prepare()
                    
                    if (isActive) {
                        player!!.playWhenReady = true
                    } else {
                        player!!.playWhenReady = false
                        player!!.pause()
                    }

                    player!!.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (bindingAdapterPosition == viewPager.currentItem) {
                                if (playbackState == Player.STATE_READY) {
                                    onPlayerActiveAndReady(player!!, bindingAdapterPosition)
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            if (bindingAdapterPosition == viewPager.currentItem) {
                                Toast.makeText(this@MediaPlayerActivity, "Ошибка воспроизведения видео", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            } else {
                playerView.visibility = View.GONE
                imageView.visibility = View.VISIBLE

                if (item.imageBitmap != null) {
                    imageView.setImageBitmap(item.imageBitmap)
                } else if (!item.imagePath.isNullOrEmpty()) {
                    val path = item.imagePath
                    if (path.startsWith("content://")) {
                        imageView.setImageURI(Uri.parse(path))
                    } else {
                        val file = File(path)
                        if (file.exists()) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                imageView.setImageBitmap(bmp)
                            } else {
                                imageView.setImageURI(Uri.fromFile(file))
                            }
                        } else {
                            imageView.setImageURI(Uri.parse(path))
                        }
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_person)
                }
            }
        }

        fun releasePlayer() {
            player?.release()
            player = null
            playerView.player = null
        }
    }
}
