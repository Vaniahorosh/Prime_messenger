package com.messenger.prime

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

/**
 * Кастомная система уведомлений ("островков"), заменяющая Toast.
 * Поддерживает анимации появления/исчезновения и смахивание (swipe).
 */
object PrimeNotification {

    private val handler = Handler(Looper.getMainLooper())

    fun show(activity: Activity, message: String) {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(activity)
        val notificationView = inflater.inflate(R.layout.layout_prime_notification, rootLayout, false)
        
        val card = notificationView.findViewById<MaterialCardView>(R.id.notificationCard)
        val textView = notificationView.findViewById<TextView>(R.id.tvNotificationText)
        textView.text = message

        // Настройка контейнера для позиционирования внизу (на "острове")
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
        // Отступ снизу (примерно 10см на экране ~ 250dp, но для удобства возьмем 120dp)
        params.bottomMargin = (30 * activity.resources.displayMetrics.density).toInt()
        notificationView.layoutParams = params

        rootLayout.addView(notificationView)

        // --- АНИМАЦИЯ ПОЯВЛЕНИЯ ---
        notificationView.alpha = 0f
        notificationView.translationY = 100f
        notificationView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Таймер авто-скрытия (через 3 секунды)
        val dismissRunnable = Runnable {
            dismiss(notificationView, 0f, 1f) // улетает вниз по дефолту
        }
        handler.postDelayed(dismissRunnable, 3000)

        // --- ЛОГИКА СВАЙПА (влево, вправо, вниз) ---
        var startX = 0f
        var startY = 0f
        var isDragging = false
        val screenWidth = activity.resources.displayMetrics.widthPixels.toFloat()

        notificationView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    handler.removeCallbacks(dismissRunnable) // останавливаем авто-скрытие
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    
                    // Позволяем свайп влево/вправо или строго вниз
                    v.translationX = dx
                    v.translationY = if (dy > 0) dy else dy * 0.1f // сопротивление вверх
                    v.alpha = 1f - (kotlin.math.abs(dx) / (screenWidth * 0.8f)).coerceIn(0f, 0.5f)
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        handler.postDelayed(dismissRunnable, 2000) // возвращаем таймер если просто тыкнули
                        return@setOnTouchListener false
                    }
                    
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY

                    // Если свайпнули достаточно далеко
                    if (kotlin.math.abs(dx) > screenWidth / 4 || dy > 150f) {
                        dismiss(v, dx, dy)
                    } else {
                        // Возврат на место если не дотянули
                        v.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                        handler.postDelayed(dismissRunnable, 3000)
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun dismiss(view: View, directionX: Float, directionY: Float) {
        val animator = view.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                val parent = view.parent as? ViewGroup
                parent?.removeView(view)
            }

        // Улетает в ту сторону, куда смахнули
        if (kotlin.math.abs(directionX) > kotlin.math.abs(directionY)) {
            animator.translationX(if (directionX > 0) 500f else -500f)
        } else {
            animator.translationY(500f)
        }
        
        animator.start()
    }
}