package com.messenger.prime

import android.annotation.SuppressLint
import android.graphics.Outline
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

class FloatingPipManager(
    private val pipView: FrameLayout,
    private val videoView: TextureVideoView,
    private val onDismiss: () -> Unit
) {
    private var dX = 0f
    private var dY = 0f
    private var isPipVisible = false

    init {
        setupShapeClipping("circle")
        setupDragAndSwipe()
    }

    fun setupShapeClipping(shape: String? = null) {
        val sharedPrefs = pipView.context.getSharedPreferences("PrimeLocalDB", android.content.Context.MODE_PRIVATE)
        val radiusPercent = sharedPrefs.getInt("video_message_corner_radius", 50)

        pipView.clipToOutline = true
        pipView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadiusPx = view.width * (radiusPercent / 100f)
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        pipView.invalidateOutline()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndSwipe() {
        pipView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    view.x = newX
                    view.y = newY

                    val screenWidth = view.resources.displayMetrics.widthPixels
                    if (newX < -view.width * 0.6f || newX > screenWidth - view.width * 0.4f) {
                        dismissPip()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    fun showPip(videoPath: String, shape: String, startMs: Int = 0) {
        if (isPipVisible) return
        isPipVisible = true

        setupShapeClipping(shape)
        val density = pipView.context.resources.displayMetrics.density

        pipView.x = 16 * density
        if (pipView.y <= 0) {
            pipView.y = 120 * density
        }

        pipView.visibility = View.VISIBLE
        pipView.alpha = 1f
        videoView.setVideoPath(videoPath)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            if (startMs > 0) {
                try {
                    mp.seekTo(startMs)
                } catch (ignored: Exception) {}
            }
            mp.start()
        }
    }

    fun pause() {
        videoView.pause()
    }

    fun start() {
        videoView.start()
    }

    fun seekTo(msec: Int) {
        videoView.seekTo(msec)
    }

    fun getCurrentPosition(): Int {
        return videoView.currentPosition
    }

    fun isShowing(): Boolean = isPipVisible

    fun dismissPip() {
        if (!isPipVisible) return
        isPipVisible = false

        pipView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                videoView.stopPlayback()
                pipView.visibility = View.GONE
                pipView.alpha = 1f
                onDismiss()
            }
            .start()
    }
}
