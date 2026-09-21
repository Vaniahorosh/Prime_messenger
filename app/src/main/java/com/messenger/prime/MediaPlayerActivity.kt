package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.Locale

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var ivBlurredBackground: ImageView
    private lateinit var mediaContainer: FrameLayout
    private lateinit var videoView: VideoView
    private lateinit var ivPhotoMedia: ImageView
    private lateinit var topOverlay: View
    private lateinit var btnBack: ImageButton
    private lateinit var tvSenderName: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var bottomOverlay: View
    private lateinit var tvCaption: TextView
    private lateinit var layoutSeekBarRow: View
    private lateinit var tvDuration: TextView
    private lateinit var seekBar: SeekBar

    private var mediaUri: Uri? = null
    private var isVideo = false

    private val handler = Handler(Looper.getMainLooper())
    private var areControlsVisible = false
    private val autoHideControlsRunnable = Runnable { hideControls() }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (isVideo && videoView.isPlaying) {
                val current = videoView.currentPosition.toLong()
                val total = videoView.duration.toLong().coerceAtLeast(1L)
                val progress = ((current * 1000) / total).toInt().coerceIn(0, 1000)
                seekBar.progress = progress
                updateDurationText(current, total)
                handler.postDelayed(this, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        initViews()
        setupInsets()
        parseIntentData()
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
        tvSenderName = findViewById(R.id.tvSenderName)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        bottomOverlay = findViewById(R.id.bottomOverlay)
        tvCaption = findViewById(R.id.tvCaption)
        layoutSeekBarRow = findViewById(R.id.layoutSeekBarRow)
        tvDuration = findViewById(R.id.tvDuration)
        seekBar = findViewById(R.id.seekBar)
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
            mediaUri = Uri.parse(uriStr)
        } else if (!pathStr.isNullOrEmpty()) {
            val file = File(pathStr)
            if (file.exists()) mediaUri = Uri.fromFile(file)
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

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                val duration = videoView.duration.toLong().coerceAtLeast(0L)
                updateDurationText(0L, duration)
                videoView.start()
                btnPlayPause.setImageResource(R.drawable.ic_media_pause)
                handler.post(updateProgressRunnable)
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

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        mediaContainer.setOnClickListener {
            toggleControls()
        }

        btnPlayPause.setOnClickListener {
            if (isVideo) {
                if (videoView.isPlaying) {
                    videoView.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_media_play)
                    handler.removeCallbacks(updateProgressRunnable)
                } else {
                    videoView.start()
                    btnPlayPause.setImageResource(R.drawable.ic_media_pause)
                    handler.post(updateProgressRunnable)
                }
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_media_play)
            }
            showControls()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isVideo) {
                    val total = videoView.duration.toLong().coerceAtLeast(1L)
                    val targetMs = (progress * total) / 1000
                    videoView.seekTo(targetMs.toInt())
                    updateDurationText(targetMs, total)
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
        }

        // Keep visible for 2.5 seconds (2500 ms) before disappearing again
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (isVideo) {
            videoView.stopPlayback()
        }
    }
}
