package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import eightbitlab.com.blurview.BlurView
import java.util.Locale

/**
 * Pure View Manager for the Top Video Note Island with custom BlurView Speed Menu Overlay.
 * Features an expanding island animation from the speed circle button and smooth cascading speed items,
 * with outside-touch dismiss overlay and clean dismiss handling on island hide.
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

    var isRecordingActive: Boolean = false
        set(value) {
            field = value
            if (value) {
                hideIsland()
            }
        }

    private val speedMenuBlur: BlurView? = (islandView.context as? Activity)?.findViewById(R.id.layoutVideoSpeedMenuBlur)
        ?: islandView.rootView?.findViewById(R.id.layoutVideoSpeedMenuBlur)
    private val speedItemsContainer: LinearLayout? = speedMenuBlur?.findViewById(R.id.layoutVideoSpeedItemsContainer)
    private val speedMenuDismissView: View? = (islandView.context as? Activity)?.findViewById(R.id.viewVideoSpeedMenuDismiss)
        ?: islandView.rootView?.findViewById(R.id.viewVideoSpeedMenuDismiss)

    private val speeds = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 5.0f)
    private var currentDurationMs = 0
    private var isSpeedMenuOpen = false
    private var isBlurInitialized = false
    private var isSpeedMenuBlurInitialized = false

    init {
        btnPlayPause.setOnClickListener {
            onPlayPauseClickListener?.invoke()
        }

        btnSpeed.setOnClickListener {
            toggleSpeedMenu()
        }

        btnClose.setOnClickListener {
            hideIsland()
            onCloseClickListener?.invoke()
        }

        speedMenuDismissView?.setOnClickListener {
            closeSpeedMenuAnimated()
        }
    }

    private fun getSavedSpeed(context: Context): Float {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        return sp.getFloat("video_note_playback_speed", 1.0f)
    }

    private fun saveSpeed(context: Context, speed: Float) {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sp.edit().putFloat("video_note_playback_speed", speed).apply()
    }

    private fun formatSpeedLabel(speed: Float): String {
        return if (speed == 1.0f) "1.0x" else String.format(Locale.US, "%.2fx", speed).replace(".00", ".0")
    }

    private fun toggleSpeedMenu() {
        if (isSpeedMenuOpen) {
            closeSpeedMenuAnimated()
        } else {
            openSpeedMenuAnimated()
        }
    }

    private fun buildSpeedMenuItems(context: Context) {
        val container = speedItemsContainer ?: return
        container.removeAllViews()

        val currentSpeed = getSavedSpeed(context)
        val density = context.resources.displayMetrics.density
        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        val primaryTextColor = androidx.core.content.ContextCompat.getColor(context, R.color.prime_text_primary)
        val idleBgColor = androidx.core.content.ContextCompat.getColor(context, R.color.glass_white_10)

        speeds.forEach { speed ->
            val isSelected = Math.abs(speed - currentSpeed) < 0.05f
            val itemTv = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (36 * density).toInt()
                ).apply {
                    setMargins(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)

                val label = when (speed) {
                    1.0f -> "1.0x (Обычная)"
                    else -> String.format(Locale.US, "%.2fx", speed).replace(".00", ".0")
                }
                text = label
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)

                if (isSelected) {
                    setTextColor(Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 10f * density
                        setColor(accentColor)
                    }
                } else {
                    setTextColor(primaryTextColor)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 10f * density
                        setColor(idleBgColor)
                    }
                }

                setOnClickListener {
                    saveSpeed(context, speed)
                    btnSpeed.text = formatSpeedLabel(speed)
                    onSpeedClickListener?.invoke(speed)
                    closeSpeedMenuAnimated()
                }
            }
            container.addView(itemTv)
        }
    }

    private fun openSpeedMenuAnimated() {
        val menu = speedMenuBlur ?: return
        val context = menu.context

        buildSpeedMenuItems(context)
        if (!isSpeedMenuBlurInitialized) {
            setupSpeedMenuBlur(menu)
            isSpeedMenuBlurInitialized = true
        }
        setBlurAutoUpdateSafe(menu, true)

        isSpeedMenuOpen = true
        speedMenuDismissView?.visibility = View.VISIBLE
        speedMenuDismissView?.bringToFront()

        menu.visibility = View.VISIBLE
        menu.bringToFront()

        menu.pivotX = menu.width.toFloat().coerceAtLeast(100f)
        menu.pivotY = 0f
        menu.scaleX = 0.92f
        menu.scaleY = 0.92f
        menu.alpha = 0f

        menu.animate().cancel()
        menu.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(160)
            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .start()
    }

    private fun closeSpeedMenuAnimated(onComplete: (() -> Unit)? = null) {
        speedMenuDismissView?.visibility = View.GONE
        val menu = speedMenuBlur
        if (menu == null || !isSpeedMenuOpen) {
            onComplete?.invoke()
            return
        }

        isSpeedMenuOpen = false
        menu.animate().cancel()
        menu.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .alpha(0f)
            .setDuration(130)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                menu.visibility = View.GONE
                menu.scaleX = 1.0f
                menu.scaleY = 1.0f
                menu.alpha = 1.0f
                setBlurAutoUpdateSafe(menu, false)
                onComplete?.invoke()
            }
            .start()
    }

    private fun setupSpeedMenuBlur(menuBlur: BlurView) {
        try {
            val context = menuBlur.context
            val activity = context as? Activity ?: return
            val decorView = activity.window.decorView
            val rootView = decorView.findViewById<ViewGroup>(android.R.id.content) ?: return

            val density = context.resources.displayMetrics.density
            val cornerRadiusPx = 18f * density

            menuBlur.clipToOutline = true
            menuBlur.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                }
            }

            val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) {
                Color.parseColor("#500F172A")
            } else {
                Color.parseColor("#50FFFFFF")
            }

            val windowBackground = decorView.background

            menuBlur.setupWith(rootView, createSafeBlurAlgorithm(context))
                .setFrameClearDrawable(windowBackground)
                .setOverlayColor(overlayColor)
                .setBlurRadius(16f)
            menuBlur.invalidateOutline()
        } catch (t: Throwable) {
            Log.e("VideoIsland", "Speed BlurView setup error", t)
        }
    }

    private fun setupBlur() {
        try {
            val context = islandView.context
            val activity = context as? Activity ?: return
            val decorView = activity.window.decorView
            val rootView = decorView.findViewById<ViewGroup>(android.R.id.content) ?: return

            val density = context.resources.displayMetrics.density
            val cornerRadiusPx = 26f * density

            islandView.clipToOutline = true
            islandView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                }
            }

            val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) {
                Color.parseColor("#400F172A")
            } else {
                Color.parseColor("#40FFFFFF")
            }

            val windowBackground = decorView.background

            islandView.setupWith(rootView, createSafeBlurAlgorithm(context))
                .setFrameClearDrawable(windowBackground)
                .setOverlayColor(overlayColor)
                .setBlurRadius(16f)
                .setBlurAutoUpdate(true)
            islandView.invalidateOutline()
        } catch (t: Throwable) {
            Log.e("VideoIsland", "BlurView setup error", t)
        }
    }

    fun updatePlayPauseIcon(isPlaying: Boolean) {
        btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
    }

    fun updateProgressUI(currentMs: Int, totalDurationMs: Int) {
        if (totalDurationMs > 0) {
            this.currentDurationMs = totalDurationMs
        }
        val dur = this.currentDurationMs
        if (dur > 0) {
            val safeCurMs = currentMs.coerceIn(0, dur)
            pbProgress.progress = ((safeCurMs.toLong() * 100) / dur).toInt().coerceIn(0, 100)

            val curSec = safeCurMs / 1000
            val durSec = dur / 1000

            tvDuration.text = String.format(
                Locale.US,
                "%02d:%02d / %02d:%02d",
                curSec / 60, curSec % 60,
                durSec / 60, durSec % 60
            )
        }
    }

    fun getCurrentSpeed(context: Context): Float = getSavedSpeed(context)

    fun showIsland(senderName: String, durationMs: Int = 0) {
        if (isRecordingActive) return

        tvSenderName.text = senderName
        if (durationMs > 0) {
            this.currentDurationMs = durationMs
        }

        val isAlreadyVisible = islandView.visibility == View.VISIBLE

        if (!isAlreadyVisible) {
            islandView.animate().cancel()
            islandView.translationY = -24f
            islandView.alpha = 0f
            islandView.visibility = View.VISIBLE
            islandView.elevation = 8f * islandView.context.resources.displayMetrics.density
            islandView.bringToFront()

            if (!isBlurInitialized) {
                setupBlur()
                isBlurInitialized = true
            }
            setBlurAutoUpdateSafe(islandView, true)

            islandView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start()
        }

        val savedSpeed = getSavedSpeed(islandView.context)
        btnSpeed.text = formatSpeedLabel(savedSpeed)

        updateProgressUI(0, durationMs)
    }

    fun hideIsland() {
        speedMenuDismissView?.visibility = View.GONE
        speedMenuBlur?.animate()?.cancel()
        speedMenuBlur?.visibility = View.GONE
        setBlurAutoUpdateSafe(speedMenuBlur, false)
        isSpeedMenuOpen = false

        if (islandView.visibility != View.VISIBLE) {
            setBlurAutoUpdateSafe(islandView, false)
            return
        }

        islandView.animate().cancel()
        islandView.animate()
            .translationY(-20f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                islandView.visibility = View.GONE
                islandView.translationY = 0f
                islandView.alpha = 1f
                setBlurAutoUpdateSafe(islandView, false)
            }
            .start()
    }
}
