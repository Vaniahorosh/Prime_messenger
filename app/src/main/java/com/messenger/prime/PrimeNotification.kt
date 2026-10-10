package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eightbitlab.com.blurview.BlurView as EightBitBlurView
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Кастомная система уведомлений ("островков"), заменяющая Toast.
 * Поддерживает таймер обратного отсчета и отмену действия (Undo).
 */
object PrimeNotification {

    private const val DURATION = 3000L // 3 секунды

    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity?, message: String, onUndo: (() -> Unit)? = null) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            try {
                val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread
                val inflater = LayoutInflater.from(activity)
                val notificationView = inflater.inflate(R.layout.layout_prime_notification, rootLayout, false)

                val textView = notificationView.findViewById<TextView>(R.id.tvNotificationText)
                val layoutTimer = notificationView.findViewById<View>(R.id.layoutTimer)
                val pbTimer = notificationView.findViewById<ProgressBar>(R.id.pbTimer)
                val tsSeconds = notificationView.findViewById<TextSwitcher>(R.id.tsTimerSeconds)
                val btnUndo = notificationView.findViewById<ImageButton>(R.id.btnUndo)

                textView.text = message

                // --- НАСТРОЙКА BlurView НАСТОЯЩЕГО РАЗМЫТИЯ ---
                val blurView = notificationView as? EightBitBlurView
                if (blurView != null) {
                    val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    val overlayColor = if (isDark) {
                        Color.parseColor("#400F172A")
                    } else {
                        Color.parseColor("#40154B87")
                    }
                    val windowBg = activity.window?.decorView?.background
                    blurView.setupBlur(rootLayout, 16f, overlayColor, windowBg)
                }

                // Настройка параметров отображения
                val density = activity.resources.displayMetrics.density
                val bottomContainer = activity.findViewById<View>(R.id.bottomContainer)
                val inputView = activity.findViewById<View>(R.id.layoutInput)

                val inputBottomOffset = if (bottomContainer != null && bottomContainer.visibility == View.VISIBLE) {
                    val containerParams = bottomContainer.layoutParams as? ViewGroup.MarginLayoutParams
                    val bottomMargin = containerParams?.bottomMargin ?: 0
                    val containerHeight = if (bottomContainer.height > 0) {
                        bottomContainer.height
                    } else if (inputView != null && inputView.visibility == View.VISIBLE) {
                        if (inputView.height > 0) inputView.height else (64 * density).toInt()
                    } else {
                        (68 * density).toInt()
                    }
                    containerHeight + bottomMargin + (12 * density).toInt()
                } else if (inputView != null && inputView.visibility == View.VISIBLE) {
                    val inputHeight = if (inputView.height > 0) inputView.height else (64 * density).toInt()
                    inputHeight + (16 * density).toInt()
                } else {
                    (30 * density).toInt()
                }

                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.BOTTOM
                params.bottomMargin = inputBottomOffset
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
                val textColorPrimary = ContextCompat.getColor(activity, R.color.prime_text_primary)
                tsSeconds.setFactory {
                    TextView(activity).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        gravity = Gravity.CENTER
                        setTextColor(textColorPrimary)
                        textSize = 12f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                }

                var lastSecond = -1
                var isCancelled = false
                val timerAnimator = ValueAnimator.ofInt(1000, 0).apply {
                    duration = DURATION
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Int
                        pbTimer.progress = progress

                        // Обновление секунд (3..2..1)
                        val secondsLeft = ceil(progress.toDouble() * DURATION / 1000000.0).toInt().coerceAtLeast(1)
                        if (secondsLeft != lastSecond) {
                            lastSecond = secondsLeft
                            tsSeconds.setText(secondsLeft.toString())
                        }
                    }
                }

                timerAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isCancelled) {
                            dismiss(notificationView, 0f, 1f)
                        }
                    }
                })
                timerAnimator.start()

                // --- ЛОГИКА ТАЙМЕРА И UNDO ---
                if (onUndo != null) {
                    layoutTimer.visibility = View.VISIBLE
                    btnUndo.visibility = View.VISIBLE
                    btnUndo.setOnClickListener {
                        isCancelled = true
                        timerAnimator.cancel()
                        try {
                            onUndo.invoke()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
                            v.alpha = (1f - (abs(dx) / (screenWidth * 0.8f))).coerceIn(0f, 1f)
                            isDragging = true
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.performClick()
                            if (!isDragging) {
                                return@setOnTouchListener false
                            }
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY

                            if (abs(dx) > screenWidth / 4 || dy > 150f) {
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
            } catch (e: Exception) {
                e.printStackTrace()
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

        if (abs(directionX) > abs(directionY)) {
            animator.translationX(if (directionX >= 0) 800f else -800f)
        } else {
            animator.translationY(800f)
        }
        animator.start()
    }

    private const val MUSIC_NOTIFICATION_ID = 2001
    private const val MUSIC_CHANNEL_ID = "prime_music_playback_channel"

    @JvmStatic
    fun showMediaNotification(context: Context, track: TrackItem?, isPlaying: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_CHANNEL_ID,
                "Воспроизведение музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление плеера Prime Messenger"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (track == null) {
            notificationManager.cancel(MUSIC_NOTIFICATION_ID)
            return
        }

        val openIntent = Intent(context, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MUSIC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.durationStr)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingOpenIntent)

        if (!track.coverPath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(track.coverPath)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
            } catch (_: Exception) {}
        }

        notificationManager.notify(MUSIC_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun cancelMediaNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(MUSIC_NOTIFICATION_ID)
    }
}