package com.messenger.prime

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Расширение для анимации "дрожания" вьюхи и тактильной отдачи.
 * Используется при ошибках ввода.
 */
fun View.shake() {
    // Тактильный отклик (вибрация ошибки)
    this.performHapticFeedback(HapticFeedbackConstants.REJECT)

    // Анимация дрожания по горизонтали
    val shake = ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 25f, -25f, 20f, -20f, 15f, -15f, 10f, -10f, 0f)
    )
    shake.duration = 500
    shake.start()
}

/**
 * Применяет эффект размытия (blur) для глассморфизма.
 * Работает на Android 12 (API 31) и выше.
 */
fun View.applyGlassBlur(radius: Float = 30f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
    }
}
