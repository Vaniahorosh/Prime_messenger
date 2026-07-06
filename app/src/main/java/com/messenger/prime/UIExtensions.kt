package com.messenger.prime

import android.app.Activity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

// Эта функция теперь будет доступна внутри любой твоей Activity
fun Activity.setDynamicStatusBar(colorResId: Int, isDarkIcons: Boolean) {
    // 1. Устанавливаем цвет фона статус-бара
    window.statusBarColor = ContextCompat.getColor(this, colorResId)

    // 2. Говорим системе, какими должны быть иконки (светлыми или темными)
    // isAppearanceLightStatusBars = true означает, что фон СВЕТЛЫЙ, поэтому иконки станут ТЕМНЫМИ
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isDarkIcons
}