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
    val overshoot = OvershootInterpolator(1.8f)

    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().cancel()
                v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(110)
                    .setInterpolator(decelerate)
                    .start()
            }
            MotionEvent.ACTION_MOVE -> {
                val isInside = event.x in 0f..v.width.toFloat() && event.y in 0f..v.height.toFloat()
                val targetScale = if (isInside) 0.96f else 1.0f
                if (v.scaleX != targetScale) {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(120)
                        .setInterpolator(decelerate)
                        .start()
                }
            }
            MotionEvent.ACTION_UP -> {
                val isInside = event.x in 0f..v.width.toFloat() && event.y in 0f..v.height.toFloat()
                v.animate().cancel()
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(overshoot)
                    .start()
                if (isInside) {
                    v.performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                v.animate().cancel()
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(decelerate)
                    .start()
            }
        }
        true
    }
}

fun parseAvatarModelAndFile(avatarUriStr: String?): Pair<Any?, File?> {
    if (avatarUriStr.isNullOrEmpty()) return Pair(null, null)
    return try {
        if (avatarUriStr.startsWith("content://") || avatarUriStr.startsWith("http")) {
            Pair(Uri.parse(avatarUriStr), null)
        } else if (avatarUriStr.startsWith("file://")) {
            val path = Uri.parse(avatarUriStr).path ?: avatarUriStr.removePrefix("file://")
            val file = File(path)
            Pair(if (file.exists()) file else Uri.parse(avatarUriStr), if (file.exists()) file else null)
        } else {
            val file = File(avatarUriStr)
            if (file.exists()) {
                Pair(file, file)
            } else {
                Pair(Uri.parse(avatarUriStr), null)
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

fun animateAvatarCounterBadge(counterView: TextView?, currentIdx: Int, totalCount: Int) {
    if (counterView == null) return
    if (totalCount <= 1) {
        if (counterView.visibility == View.VISIBLE) {
            counterView.animate()
                .alpha(0f)
                .translationY(-30f)
                .setDuration(200)
                .withEndAction { counterView.visibility = View.GONE }
                .start()
        } else {
            counterView.visibility = View.GONE
        }
        return
    }

    counterView.text = "${currentIdx + 1} из $totalCount"

    if (counterView.visibility != View.VISIBLE || counterView.alpha < 0.5f) {
        counterView.visibility = View.VISIBLE
        counterView.translationY = -40f
        counterView.alpha = 0f
        counterView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
    } else {
        counterView.animate().cancel()
        counterView.animate()
            .translationY(14f)
            .setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                counterView.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(1.8f))
                    .start()
            }
            .start()
    }
}
