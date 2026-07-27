package com.messenger.prime

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
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
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextSwitcher
import android.widget.TextView

/**
 * Кастомная система уведомлений ("островков"), заменяющая Toast.
 * Поддерживает таймер обратного отсчета и отмену действия (Undo).
 */
object PrimeNotification {

    private val handler = Handler(Looper.getMainLooper())
    private const val DURATION = 3000L // 3 секунды

    fun show(activity: Activity, message: String, onUndo: (() -> Unit)? = null) {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(activity)
        val notificationView = inflater.inflate(R.layout.layout_prime_notification, rootLayout, false)
        
        val textView = notificationView.findViewById<TextView>(R.id.tvNotificationText)
        val layoutTimer = notificationView.findViewById<View>(R.id.layoutTimer)
        val pbTimer = notificationView.findViewById<ProgressBar>(R.id.pbTimer)
        val tsSeconds = notificationView.findViewById<TextSwitcher>(R.id.tsTimerSeconds)
        val btnUndo = notificationView.findViewById<ImageButton>(R.id.btnUndo)
        
        textView.text = message

        // Настройка параметров отображения
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
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

        // --- ЛОГИКА ТАЙМЕРА ---
        tsSeconds.setFactory {
            TextView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        }
        
        var lastSecond = -1
        val timerAnimator = ValueAnimator.ofInt(1000, 0).apply {
            duration = DURATION
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                pbTimer.progress = progress
                
                // Обновление секунд (3..2..1)
                val secondsLeft = Math.ceil(progress.toDouble() * DURATION / 1000000.0).toInt().coerceAtLeast(1)
                if (secondsLeft != lastSecond) {
                    lastSecond = secondsLeft
                    tsSeconds.setText(secondsLeft.toString())
                }
            }
        }

        val dismissRunnable = Runnable {
            dismiss(notificationView, 0f, 1f)
        }

        timerAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                dismissRunnable.run()
            }
        })
        timerAnimator.start()

        // --- ЛОГИКА ТАЙМЕРА И UNDO ---
        if (onUndo != null) {
            layoutTimer.visibility = View.VISIBLE
            btnUndo.visibility = View.VISIBLE
            btnUndo.setOnClickListener {
                timerAnimator.cancel()
                handler.removeCallbacks(dismissRunnable)
                onUndo.invoke()
                dismiss(notificationView, 1f, 0f) // улетает вправо
            }
        } else {
            layoutTimer.visibility = View.GONE
            btnUndo.visibility = View.GONE
        }

        // --- ЛОГИКА СВАЙПА ---
        var startX = 0f
        var startY = 0f
        var isDragging = false
        val screenWidth = activity.resources.displayMetrics.widthPixels.toFloat()

        notificationView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    v.translationX = dx
                    v.translationY = if (dy > 0) dy else dy * 0.1f
                    v.alpha = 1f - (kotlin.math.abs(dx) / (screenWidth * 0.8f)).coerceIn(0f, 0.5f)
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        return@setOnTouchListener false
                    }
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY

                    if (kotlin.math.abs(dx) > screenWidth / 4 || dy > 150f) {
                        timerAnimator.cancel()
                        dismiss(v, dx, dy)
                    } else {
                        v.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(200)
                            .start()
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

        if (kotlin.math.abs(directionX) > kotlin.math.abs(directionY)) {
            animator.translationX(if (directionX >= 0) 800f else -800f)
        } else {
            animator.translationY(800f)
        }
        animator.start()
    }
}
