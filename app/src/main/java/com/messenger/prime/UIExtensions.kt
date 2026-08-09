package com.messenger.prime

import android.app.Activity
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

// Эта функция теперь будет доступна внутри любой твоей Activity
fun Activity.setDynamicStatusBar(colorResId: Int, isDarkIcons: Boolean) {
    // 1. Устанавливаем цвет фона статус-бара
    window.statusBarColor = ContextCompat.getColor(this, colorResId)

    // 2. Говорим системе, какими должны быть иконки (светлыми или темными)
    // isAppearanceLightStatusBars = true означает, что фон СВЕТЛЫЙ, поэтому иконки станут ТЕМНЫМИ
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isDarkIcons
}