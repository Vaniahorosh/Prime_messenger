package com.messenger.prime

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

fun View.addBounceTouchEffect() {
    val decelerate = DecelerateInterpolator()
    val overshoot = OvershootInterpolator(2.0f)

    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().cancel()
                v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(120)
                    .setInterpolator(decelerate)
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().cancel()
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(overshoot)
                    .start()
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }
            }
        }
        true
    }
}

fun parseAvatarModelAndFile(avatarUriStr: String?): Pair<Any?, File?> {
    if (avatarUriStr.isNullOrEmpty()) return Pair(null, null)
    return try {
        val trimmed = avatarUriStr.trim()
        val ctx = PrimeApplication.appContext

        if (trimmed.startsWith("android.resource://")) {
            val uri = Uri.parse(trimmed)
            val pathSegments = uri.pathSegments
            if (pathSegments.size == 2) {
                val type = pathSegments[0]
                val name = pathSegments[1]
                if (ctx != null) {
                    val resId = ctx.resources.getIdentifier(name, type, ctx.packageName)
                    if (resId != 0) return Pair(resId, null)
                }
            } else if (pathSegments.size == 1) {
                val segment = pathSegments[0]
                val numericId = segment.toIntOrNull()
                if (numericId != null && numericId > 0 && ctx != null) {
                    try {
                        val entryName = ctx.resources.getResourceEntryName(numericId)
                        val resId = ctx.resources.getIdentifier(entryName, "drawable", ctx.packageName)
                        if (resId != 0) return Pair(resId, null)
                    } catch (_: Exception) {}
                    return Pair(numericId, null)
                }
            }
            Pair(uri, null)
        } else if (trimmed.startsWith("content://") ||
            trimmed.startsWith("http://") ||
            trimmed.startsWith("https://")
        ) {
            Pair(Uri.parse(trimmed), null)
        } else if (trimmed.startsWith("file://")) {
            val path = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
            val file = File(path)
            if (file.exists()) {
                Pair(file, file)
            } else {
                Pair(Uri.parse(trimmed), null)
            }
        } else {
            val file = File(trimmed)
            if (file.exists()) {
                Pair(file, file)
            } else {
                val resId = trimmed.toIntOrNull()
                if (resId != null && resId > 0) {
                    Pair(resId, null)
                } else if (ctx != null) {
                    val namedResId = ctx.resources.getIdentifier(trimmed, "drawable", ctx.packageName)
                    if (namedResId != 0) {
                        Pair(namedResId, null)
                    } else {
                        Pair(Uri.parse(trimmed), null)
                    }
                } else {
                    Pair(Uri.parse(trimmed), null)
                }
            }
        }
    } catch (_: Exception) {
        Pair(null, null)
    }
}

// Устанавливает прозрачные системные бары и растягивает контент
fun Activity.setupEdgeToEdge(isDarkIcons: Boolean = false) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.isAppearanceLightStatusBars = isDarkIcons
    controller.isAppearanceLightNavigationBars = isDarkIcons
}

// Автоматически добавляет отступ сверху для статус-бара на любой View/Toolbar/Header
fun View.applyStatusBarTopPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        if (statusBarInset > 0) {
            v.setPadding(v.paddingLeft, statusBarInset + v.paddingTop, v.paddingRight, v.paddingBottom)
        }
        insets
    }
}

fun Activity.setDynamicStatusBar(colorResId: Int, isDarkIcons: Boolean) {
    window.statusBarColor = ContextCompat.getColor(this, colorResId)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isDarkIcons
}

fun TextView.animateSlideDownText(newText: CharSequence?, duration: Long = 180L, onComplete: (() -> Unit)? = null) {
    val target = newText?.toString() ?: ""
    if (this.text?.toString() == target) return
    if (this.visibility != View.VISIBLE || this.text.isNullOrEmpty()) {
        this.text = target
        onComplete?.invoke()
        return
    }

    this.animate().cancel()
    this.animate()
        .translationY(24f)
        .alpha(0f)
        .setDuration(duration)
        .setInterpolator(DecelerateInterpolator())
        .withEndAction {
            this.text = target
            this.translationY = -24f
            this.alpha = 0f
            this.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    onComplete?.invoke()
                }
                .start()
        }
        .start()
}

fun animateAvatarCounterBadge(counterView: TextView?, currentIdx: Int, totalCount: Int) {
    if (counterView == null) return
    if (totalCount <= 1) {
        if (counterView.visibility == View.VISIBLE) {
            counterView.animate().cancel()
            counterView.animate()
                .alpha(0f)
                .translationY(24f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    counterView.visibility = View.GONE
                    counterView.translationY = 0f
                }
                .start()
        } else {
            counterView.visibility = View.GONE
        }
        return
    }

    val targetText = "${currentIdx + 1} из $totalCount"

    if (counterView.visibility != View.VISIBLE || counterView.alpha < 0.1f) {
        counterView.text = targetText
        counterView.visibility = View.VISIBLE
        counterView.translationY = -24f
        counterView.alpha = 0f
        counterView.animate().cancel()
        counterView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    } else {
        counterView.animateSlideDownText(targetText)
    }
}
