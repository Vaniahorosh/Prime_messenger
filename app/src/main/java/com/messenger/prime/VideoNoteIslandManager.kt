package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
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

    private val speedMenuBlur: BlurView? = (islandView.parent as? ViewGroup)?.findViewById(R.id.layoutVideoSpeedMenuBlur)
    private val speedItemsContainer: LinearLayout? = speedMenuBlur?.findViewById(R.id.layoutVideoSpeedItemsContainer)
    private val speedMenuDismissView: View? = (islandView.parent as? ViewGroup)?.findViewById(R.id.viewVideoSpeedMenuDismiss)

    private val speeds = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 5.0f)
    private var currentDurationMs = 0
    private var isSpeedMenuOpen = false

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

        speeds.forEachIndexed { index, speed ->
            val isSelected = Math.abs(speed - currentSpeed) < 0.05f
            val itemTv = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (34 * density).toInt()
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
                typeface = android.graphics.Typeface.DEFAULT_BOLD

                if (isSelected) {
                    setTextColor(Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(accentColor)
                    }
                } else {
                    setTextColor(Color.parseColor("#E2E8F0"))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(Color.parseColor("#15FFFFFF"))
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
        val container = speedItemsContainer ?: return
        val context = menu.context

        buildSpeedMenuItems(context)
        setupSpeedMenuBlur(menu)

        isSpeedMenuOpen = true
        speedMenuDismissView?.visibility = View.VISIBLE
        speedMenuDismissView?.bringToFront()

        menu.visibility = View.VISIBLE
        menu.bringToFront()

        val density = context.resources.displayMetrics.density

        menu.pivotX = menu.width.toFloat().coerceAtLeast(100f)
        menu.pivotY = 0f
        menu.scaleX = 0.2f
        menu.scaleY = 0.2f
        menu.alpha = 0f

        menu.animate().cancel()
        menu.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = -20f * density
            child.animate().cancel()
            child.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(200)
                .setStartDelay(i * 25L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun closeSpeedMenuAnimated(onComplete: (() -> Unit)? = null) {
        speedMenuDismissView?.visibility = View.GONE
        val menu = speedMenuBlur
        if (menu == null || !isSpeedMenuOpen) {
            onComplete?.invoke()
            return
        }

        isSpeedMenuOpen = false
        val container = speedItemsContainer
        val density = menu.context.resources.displayMetrics.density

        if (container != null) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.animate().cancel()
                child.animate()
                    .alpha(0f)
                    .translationY(-15f * density)
                    .setDuration(150)
                    .start()
            }
        }

        menu.animate().cancel()
        menu.animate()
            .scaleX(0.2f)
            .scaleY(0.2f)
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                menu.visibility = View.GONE
                menu.scaleX = 1.0f
                menu.scaleY = 1.0f
                menu.alpha = 1.0f
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                menuBlur.setupWith(rootView, RenderEffectBlur())
                    .setFrameClearDrawable(windowBackground)
                    .setOverlayColor(overlayColor)
                    .setBlurRadius(16f)
            } else {
                try {
                    menuBlur.setupWith(rootView, RenderScriptBlur(context))
                        .setFrameClearDrawable(windowBackground)
                        .setOverlayColor(overlayColor)
                        .setBlurRadius(16f)
                } catch (t: Throwable) {
                    Log.e("VideoIsland", "RenderScriptBlur unavailable", t)
                }
            }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                islandView.setupWith(rootView, RenderEffectBlur())
                    .setFrameClearDrawable(windowBackground)
                    .setOverlayColor(overlayColor)
                    .setBlurRadius(16f)
                    .setBlurAutoUpdate(true)
            } else {
                try {
                    islandView.setupWith(rootView, RenderScriptBlur(context))
                        .setFrameClearDrawable(windowBackground)
                        .setOverlayColor(overlayColor)
                        .setBlurRadius(16f)
                        .setBlurAutoUpdate(true)
                } catch (t: Throwable) {
                    Log.e("VideoIsland", "RenderScriptBlur unavailable", t)
                }
            }
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
        tvSenderName.text = senderName
        if (durationMs > 0) {
            this.currentDurationMs = durationMs
        }

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

        val savedSpeed = getSavedSpeed(islandView.context)
        btnSpeed.text = formatSpeedLabel(savedSpeed)
        if (savedSpeed != 1.0f) {
            islandView.post {
                onSpeedClickListener?.invoke(savedSpeed)
            }
        }

        updateProgressUI(0, durationMs)
    }

    fun hideIsland() {
        speedMenuDismissView?.visibility = View.GONE
        speedMenuBlur?.animate()?.cancel()
        speedMenuBlur?.visibility = View.GONE
        isSpeedMenuOpen = false

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
