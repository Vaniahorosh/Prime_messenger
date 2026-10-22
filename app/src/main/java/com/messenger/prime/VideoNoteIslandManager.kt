package com.messenger.prime

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

/**
 * Pure View Manager for the Top Video Note Island.
 * Delegates all playback actions to the central VideoNote controller in ChatPersonActivity.
 */
class VideoNoteIslandManager(
    private val islandView: BlurView,
    private val btnPlayPause: ImageButton,
    private val tvSenderName: TextView,
    private val pbProgress: ProgressBar,
    private val tvDuration: TextView,
    private val btnSpeed: TextView,
    private val btnClose: ImageButton
) {
    var onPlayPauseClickListener: (() -> Unit)? = null
    var onSpeedClickListener: ((Float) -> Unit)? = null
    var onCloseClickListener: (() -> Unit)? = null

    private val speeds = arrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 5.0f)
    private var currentSpeedIndex = 1 // Default 1.0x
    private var currentDurationMs = 0

    init {
        btnPlayPause.setOnClickListener {
            onPlayPauseClickListener?.invoke()
        }

        btnSpeed.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
            val speed = speeds[currentSpeedIndex]
            btnSpeed.text = String.format(java.util.Locale.US, "%.1fx", speed)
            onSpeedClickListener?.invoke(speed)
        }

        btnClose.setOnClickListener {
            onCloseClickListener?.invoke()
        }
    }

    private fun setupBlur() {
        try {
            val decorView = (islandView.context as? Activity)?.window?.decorView ?: return
            val rootView = decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
            val windowBackground = decorView.background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                islandView.setupWith(rootView, RenderEffectBlur())
                    .setFrameClearDrawable(windowBackground)
                    .setBlurRadius(16f)
            } else {
                try {
                    islandView.setupWith(rootView, RenderScriptBlur(islandView.context))
                        .setFrameClearDrawable(windowBackground)
                        .setBlurRadius(16f)
                } catch (t: Throwable) {
                    Log.e("VideoIsland", "RenderScriptBlur unavailable", t)
                }
            }
        } catch (t: Throwable) {
            Log.e("VideoIsland", "BlurView setup error", t)
        }
    }

    fun updatePlayPauseIcon(isPlaying: Boolean) {
        btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
    }

    fun updateProgressUI(currentMs: Int, totalDurationMs: Int) {
        val dur = if (totalDurationMs > 0) totalDurationMs else currentDurationMs
        if (dur > 0) {
            this.currentDurationMs = dur
            pbProgress.progress = (currentMs * 100) / dur
            val curSec = currentMs / 1000
            val durSec = dur / 1000
            tvDuration.text = String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, durSec / 60, durSec % 60)
        }
    }

    fun showIsland(senderName: String, durationMs: Int = 0) {
        tvSenderName.text = senderName
        this.currentDurationMs = durationMs

        islandView.animate().cancel()
        islandView.translationY = -120f
        islandView.alpha = 0f
        islandView.visibility = View.VISIBLE
        islandView.elevation = 24f * islandView.context.resources.displayMetrics.density
        islandView.bringToFront()

        setupBlur()

        islandView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        btnPlayPause.setImageResource(R.drawable.ic_media_pause)
        val speed = speeds[currentSpeedIndex]
        btnSpeed.text = String.format(java.util.Locale.US, "%.1fx", speed)

        updateProgressUI(0, durationMs)
    }

    fun hideIsland() {
        islandView.animate().cancel()
        islandView.animate()
            .translationY(-120f)
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                islandView.visibility = View.GONE
                islandView.translationY = 0f
                islandView.alpha = 1f
            }
            .start()
    }
}
