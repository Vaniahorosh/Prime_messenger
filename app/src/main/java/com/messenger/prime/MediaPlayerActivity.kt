package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class AspectRatioVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        if (videoWidth != width || videoHeight != height) {
            videoWidth = width
            videoHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = getDefaultSize(videoWidth, widthMeasureSpec)
        var height = getDefaultSize(videoHeight, heightMeasureSpec)
        if (videoWidth > 0 && videoHeight > 0) {
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

            val viewRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
            val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

            if (videoRatio > viewRatio) {
                width = widthSpecSize
                height = (width / videoRatio).toInt()
            } else {
                height = heightSpecSize
                width = (height * videoRatio).toInt()
            }
        }
        setMeasuredDimension(width, height)
    }
}

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var ivBlurredBackground: ImageView
    private lateinit var mediaContainer: FrameLayout
    private lateinit var videoView: AspectRatioVideoView
    private lateinit var ivPhotoMedia: ImageView
    private lateinit var topOverlay: View
    private lateinit var btnBack: ImageButton
    private lateinit var btnDownload: ImageButton
    private lateinit var tvSenderName: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var bottomOverlay: View
    private lateinit var tvCaption: TextView
    private lateinit var layoutSeekBarRow: View
    private lateinit var tvDuration: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var navBarProgressBar: ProgressBar

    private var mediaUri: Uri? = null
    private var isVideo = false

    private val handler = Handler(Looper.getMainLooper())
    private var areControlsVisible = false
    private val autoHideControlsRunnable = Runnable { hideControls() }

    private val backgroundBlurExecutor = Executors.newSingleThreadExecutor()
    private val blurRetriever = MediaMetadataRetriever()
    private var isRetrieverPrepared = false

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Pinch-to-zoom for Photo Media
    private var scaleFactor = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (isVideo && videoView.isPlaying) {
                val current = videoView.currentPosition.toLong()
                val total = videoView.duration.toLong().coerceAtLeast(1L)
                val progress = ((current * 1000) / total).toInt().coerceIn(0, 1000)
                seekBar.progress = progress
                navBarProgressBar.progress = progress
                updateDurationText(current, total)
                handler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager

        initViews()
        setupInsets()
        parseIntentData()
        setupGestureDetectors()
        setupMedia()
        setupListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initViews() {
        ivBlurredBackground = findViewById(R.id.ivBlurredBackground)
        mediaContainer = findViewById(R.id.mediaContainer)
        videoView = findViewById(R.id.videoView)
        ivPhotoMedia = findViewById(R.id.ivPhotoMedia)
        topOverlay = findViewById(R.id.topOverlay)
        btnBack = findViewById(R.id.btnBack)
        btnDownload = findViewById(R.id.btnDownload)
        tvSenderName = findViewById(R.id.tvSenderName)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        bottomOverlay = findViewById(R.id.bottomOverlay)
        tvCaption = findViewById(R.id.tvCaption)
        layoutSeekBarRow = findViewById(R.id.layoutSeekBarRow)
        tvDuration = findViewById(R.id.tvDuration)
        seekBar = findViewById(R.id.seekBar)
        navBarProgressBar = findViewById(R.id.navBarProgressBar)
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

    private fun parseIntentData() {
        val uriStr = intent.getStringExtra("EXTRA_URI")
        val pathStr = intent.getStringExtra("EXTRA_PATH")
        if (!uriStr.isNullOrEmpty()) {
            mediaUri = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                Uri.parse(uriStr)
            } else {
                val file = File(uriStr)
                if (file.exists()) Uri.fromFile(file) else Uri.parse(uriStr)
            }
        } else if (!pathStr.isNullOrEmpty()) {
            val file = File(pathStr)
            mediaUri = if (file.exists()) Uri.fromFile(file) else Uri.parse(pathStr)
        }

        val senderName = intent.getStringExtra("EXTRA_SENDER_NAME") ?: "Отправитель"
        val timestamp = intent.getStringExtra("EXTRA_TIMESTAMP") ?: "сейчас"
        val caption = intent.getStringExtra("EXTRA_CAPTION")

        tvSenderName.text = senderName
        tvTimestamp.text = timestamp

        if (!caption.isNullOrEmpty()) {
            tvCaption.visibility = View.VISIBLE
            tvCaption.text = caption
        } else {
            tvCaption.visibility = View.GONE
        }
    }

    private fun setupGestureDetectors() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!isVideo) {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(1.0f, 4.0f)
                    ivPhotoMedia.scaleX = scaleFactor
                    ivPhotoMedia.scaleY = scaleFactor
                    return true
                }
                return false
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isVideo) {
                    val width = mediaContainer.width
                    if (width > 0) {
                        val touchX = e.x
                        if (touchX < width / 3f) {
                            seekByOffset(-10000L)
                        } else if (touchX > (width * 2 / 3f)) {
                            seekByOffset(10000L)
                        } else {
                            togglePlayPause()
                        }
                    }
                    return true
                } else {
                    scaleFactor = if (scaleFactor > 1.2f) 1.0f else 2.2f
                    ivPhotoMedia.animate()
                        .scaleX(scaleFactor)
                        .scaleY(scaleFactor)
                        .setDuration(200)
                        .start()
                    return true
                }
            }
        })
    }

    private fun setupMedia() {
        var path = mediaUri?.path?.lowercase(Locale.getDefault()) ?: ""
        if (path.isEmpty() && mediaUri != null) {
            path = mediaUri.toString().lowercase(Locale.getDefault())
        }

        isVideo = path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".3gp") || path.endsWith(".webm") || path.contains("video")

        if (isVideo && mediaUri != null) {
            videoView.visibility = View.VISIBLE
            ivPhotoMedia.visibility = View.GONE
            videoView.setVideoURI(mediaUri)

            initRetriever(mediaUri)
            loadInitialVideoThumbnail(mediaUri)

            videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Ошибка воспроизведения видео", Toast.LENGTH_SHORT).show()
                videoView.visibility = View.GONE
                ivPhotoMedia.visibility = View.VISIBLE
                ivPhotoMedia.setImageResource(R.drawable.ic_video)
                true
            }

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                val vWidth = mp.videoWidth
                val vHeight = mp.videoHeight
                if (vWidth > 0 && vHeight > 0) {
                    videoView.setVideoSize(vWidth, vHeight)
                }
                val duration = videoView.duration.toLong().coerceAtLeast(0L)
                updateDurationText(0L, duration)

                requestAudioFocus()
                videoView.keepScreenOn = true
                videoView.start()
                btnPlayPause.setImageResource(R.drawable.ic_media_pause)
                handler.post(updateProgressRunnable)
                showControls()
            }
        } else {
            videoView.visibility = View.GONE
            videoView.stopPlayback()
            ivPhotoMedia.visibility = View.VISIBLE
            layoutSeekBarRow.visibility = View.GONE

            if (mediaUri != null) {
                ivPhotoMedia.setImageURI(mediaUri)
                ivBlurredBackground.setImageURI(mediaUri)
                ivBlurredBackground.applyGlassBlur(50f)
            } else {
                ivPhotoMedia.setImageResource(R.drawable.ic_person)
                ivBlurredBackground.setImageResource(R.drawable.ic_person)
                ivBlurredBackground.applyGlassBlur(50f)
            }
            updateDurationText(0L, 0L)
        }
    }

    private fun initRetriever(uri: Uri?) {
        if (uri == null) return
        backgroundBlurExecutor.execute {
            try {
                if ("file".equals(uri.scheme, ignoreCase = true) && uri.path != null) {
                    blurRetriever.setDataSource(uri.path)
                } else {
                    blurRetriever.setDataSource(this, uri)
                }
                isRetrieverPrepared = true
            } catch (e: Exception) {
                isRetrieverPrepared = false
            }
        }
    }

    private fun loadInitialVideoThumbnail(uri: Uri?) {
        if (uri == null) return
        backgroundBlurExecutor.execute {
            try {
                val bmp = blurRetriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) {
                    runOnUiThread {
                        if (!isFinishing) {
                            ivBlurredBackground.setImageBitmap(bmp)
                            ivBlurredBackground.applyGlassBlur(50f)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnDownload.setOnClickListener {
            if (mediaUri != null) {
                Executors.newSingleThreadExecutor().execute {
                    val success = MediaSaveUtils.saveToGallery(this, mediaUri, isVideo)
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

        mediaContainer.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            gestureDetector.onTouchEvent(event)
            true
        }

        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isVideo) {
                    val total = videoView.duration.toLong().coerceAtLeast(1L)
                    val targetMs = (progress * total) / 1000
                    videoView.seekTo(targetMs.toInt())
                    updateDurationText(targetMs, total)
                    navBarProgressBar.progress = progress

                    if (isRetrieverPrepared) {
                        val targetUs = targetMs * 1000L
                        backgroundBlurExecutor.execute {
                            try {
                                val bmp = blurRetriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                if (bmp != null) {
                                    runOnUiThread {
                                        if (!isFinishing) {
                                            ivBlurredBackground.setImageBitmap(bmp)
                                            ivBlurredBackground.applyGlassBlur(50f)
                                        }
                                    }
                                }
                            } catch (ignored: Exception) {}
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                handler.removeCallbacks(autoHideControlsRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                showControls()
            }
        })
    }

    private fun togglePlayPause() {
        if (isVideo) {
            if (videoView.isPlaying) {
                videoView.pause()
                videoView.keepScreenOn = false
                btnPlayPause.setImageResource(R.drawable.ic_media_play)
                handler.removeCallbacks(updateProgressRunnable)
                abandonAudioFocus()
            } else {
                requestAudioFocus()
                videoView.keepScreenOn = true
                videoView.start()
                btnPlayPause.setImageResource(R.drawable.ic_media_pause)
                handler.post(updateProgressRunnable)
            }
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_media_play)
        }
        showControls()
    }

    private fun seekByOffset(offsetMs: Long) {
        if (!isVideo) return
        val total = videoView.duration.toLong().coerceAtLeast(1L)
        val current = videoView.currentPosition.toLong()
        val targetMs = (current + offsetMs).coerceIn(0L, total)
        videoView.seekTo(targetMs.toInt())
        val progress = ((targetMs * 1000) / total).toInt().coerceIn(0, 1000)
        seekBar.progress = progress
        navBarProgressBar.progress = progress
        updateDurationText(targetMs, total)

        val text = if (offsetMs > 0) "+10 сек" else "-10 сек"
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        showControls()
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            if (isVideo && videoView.isPlaying) {
                                togglePlayPause()
                            }
                        }
                    }
                    .build()
                audioFocusRequest = focusRequest
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            if (isVideo && videoView.isPlaying) {
                                togglePlayPause()
                            }
                        }
                    },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showControls() {
        handler.removeCallbacks(autoHideControlsRunnable)
        if (!areControlsVisible) {
            areControlsVisible = true

            topOverlay.visibility = View.VISIBLE
            topOverlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()

            btnPlayPause.visibility = View.VISIBLE
            btnPlayPause.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()

            bottomOverlay.visibility = View.VISIBLE
            bottomOverlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()

            navBarProgressBar.animate()
                .alpha(0f)
                .setDuration(180)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (areControlsVisible) navBarProgressBar.visibility = View.GONE
                    }
                })
                .start()
        }

        handler.postDelayed(autoHideControlsRunnable, 2500L)
    }

    private fun hideControls() {
        handler.removeCallbacks(autoHideControlsRunnable)
        if (areControlsVisible) {
            areControlsVisible = false

            topOverlay.animate()
                .alpha(0f)
                .translationY(-30f)
                .setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) topOverlay.visibility = View.GONE
                    }
                })
                .start()

            btnPlayPause.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) btnPlayPause.visibility = View.GONE
                    }
                })
                .start()

            bottomOverlay.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(220)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!areControlsVisible) bottomOverlay.visibility = View.GONE
                    }
                })
                .start()

            if (isVideo) {
                navBarProgressBar.visibility = View.VISIBLE
                navBarProgressBar.animate()
                    .alpha(0.4f)
                    .setDuration(220)
                    .start()
            }
        }
    }

    private fun toggleControls() {
        if (areControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun updateDurationText(currentMs: Long, totalMs: Long) {
        val curStr = formatMs(currentMs)
        val totalStr = formatMs(totalMs)
        tvDuration.text = "$curStr/$totalStr"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    override fun onPause() {
        super.onPause()
        if (isVideo && videoView.isPlaying) {
            videoView.pause()
            videoView.keepScreenOn = false
            btnPlayPause.setImageResource(R.drawable.ic_media_play)
            handler.removeCallbacks(updateProgressRunnable)
            abandonAudioFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        backgroundBlurExecutor.shutdownNow()
        abandonAudioFocus()
        try {
            blurRetriever.release()
        } catch (ignored: Exception) {}
        if (isVideo) {
            videoView.keepScreenOn = false
            videoView.stopPlayback()
        }
    }
}
