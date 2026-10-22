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
    private val onDismiss: () -> Unit,
    private val onClick: () -> Unit = {}
) {
    private var dX = 0f
    private var dY = 0f
    private var initialX = 0f
    private var initialY = 0f
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
                    initialX = event.rawX
                    initialY = event.rawY
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    view.animate().cancel()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    view.x = newX
                    view.y = newY

                    val screenWidth = view.resources.displayMetrics.widthPixels
                    if (newX < -view.width * 0.5f || newX > screenWidth - view.width * 0.5f) {
                        dismissPip()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dist = Math.hypot((event.rawX - initialX).toDouble(), (event.rawY - initialY).toDouble())
                    if (dist < 20) {
                        view.performClick()
                        onClick()
                    } else if (isPipVisible) {
                        val screenWidth = view.resources.displayMetrics.widthPixels
                        val density = view.resources.displayMetrics.density
                        val targetX = if (view.x + view.width / 2 < screenWidth / 2) 16f * density else screenWidth - view.width - 16f * density
                        view.animate()
                            .x(targetX)
                            .setDuration(250)
                            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                            .start()
                    }
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
            mp.isLooping = false
            if (startMs > 0) {
                try {
                    mp.seekTo(startMs)
                } catch (ignored: Exception) {}
            }
            mp.start()
        }
        videoView.setOnCompletionListener {
            hidePipForReturn()
            onDismiss()
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

    fun setSpeed(speed: Float) {
        videoView.setSpeed(speed)
    }

    fun getCurrentPosition(): Int {
        return videoView.currentPosition
    }

    fun isShowing(): Boolean = isPipVisible

    fun hidePipForReturn(): Int {
        if (!isPipVisible) return 0
        isPipVisible = false

        val pos = videoView.currentPosition
        videoView.stopPlayback()

        pipView.animate().cancel()
        pipView.visibility = View.GONE
        pipView.alpha = 1f

        return pos
    }

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
