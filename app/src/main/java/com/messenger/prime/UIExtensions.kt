package com.messenger.prime

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
