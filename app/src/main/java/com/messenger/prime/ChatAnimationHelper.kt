package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Random

/**
 * Утилита для анимаций действий в чате (Material 3 Expressive & Physics-based):
 * - Взрыв реакций / эмодзи при тапках/выборе
 * - Пружинистый pop-in новых сообщений
 * - Свайп для ответа с эластичным индикатором
 * - Плавные анимации панелей ответа, редактирования, закрепления
 * - Анимация переключения микрофона/камеры
 * - Изогнутые эффекты для галочек статуса и реакций
 */
object ChatAnimationHelper {

    private val random = Random()

    /**
     * Создает эффектное вылетающее эмодзи с взрывом звёздочек при вызове реакций или двойном тапе на сообщение.
     */
    @JvmStatic
    fun spawnReactionBurst(anchorView: View?, reactionEmoji: String?) {
        if (anchorView == null || reactionEmoji.isNullOrEmpty()) return
        val context = anchorView.context ?: return
        val activity = (context as? Activity) ?: return
        val decorView = activity.window?.decorView as? ViewGroup ?: return

        anchorView.post {
            try {
                val density = context.resources.displayMetrics.density
                val location = IntArray(2)
                anchorView.getLocationOnScreen(location)

                val anchorX = location[0] + (anchorView.width / 2)
                val anchorY = location[1] + (anchorView.height / 2)

                val burstContainer = FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipChildren = false
                    clipToPadding = false
                }
                decorView.addView(burstContainer)

                // 1. Главное центральное вылетающее эмодзи
                val mainEmojiTv = TextView(context).apply {
                    text = reactionEmoji
                    textSize = 46f
                    gravity = Gravity.CENTER
                    setShadowLayer(12f * density, 0f, 4f * density, Color.parseColor("#40000000"))
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = anchorX - (30 * density).toInt()
                        topMargin = anchorY - (30 * density).toInt()
                    }
                    scaleX = 0.1f
                    scaleY = 0.1f
                    alpha = 0.9f
                    rotation = (random.nextFloat() - 0.5f) * 30f
                }
                burstContainer.addView(mainEmojiTv)

                // Анимация главного эмодзи
                mainEmojiTv.animate()
                    .scaleX(1.45f)
                    .scaleY(1.45f)
                    .translationY(-90f * density)
                    .alpha(1.0f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .withEndAction {
                        mainEmojiTv.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(-120f * density)
                            .alpha(0.0f)
                            .setDuration(280)
                            .setInterpolator(AccelerateInterpolator())
                            .withEndAction {
                                burstContainer.removeView(mainEmojiTv)
                                if (burstContainer.childCount == 0) {
                                    decorView.removeView(burstContainer)
                                }
                            }
                            .start()
                    }
                    .start()

                // 2. Радиальный взрыв звёздочек / искорок вокруг эмодзи
                val sparkleEmojis = arrayOf("✨", "⭐", "💖", "💫", "🌟", "🔥")
                val particleCount = 7
                for (i in 0 until particleCount) {
                    val particleEmoji = sparkleEmojis[random.nextInt(sparkleEmojis.size)]
                    val particleTv = TextView(context).apply {
                        text = particleEmoji
                        textSize = (14 + random.nextInt(8)).toFloat()
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            leftMargin = anchorX - (15 * density).toInt()
                            topMargin = anchorY - (15 * density).toInt()
                        }
                        scaleX = 0.1f
                        scaleY = 0.1f
                        alpha = 1.0f
                    }
                    burstContainer.addView(particleTv)

                    val angle = (i * (2 * Math.PI / particleCount)) + ((random.nextFloat() - 0.5f) * 0.5)
                    val distance = (45f + random.nextFloat() * 45f) * density
                    val targetX = (Math.cos(angle) * distance).toFloat()
                    val targetY = (Math.sin(angle) * distance).toFloat() - (40f * density)

                    particleTv.animate()
                        .translationX(targetX)
                        .translationY(targetY)
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(0.0f)
                        .setDuration(450 + random.nextInt(150).toLong())
                        .setStartDelay((random.nextInt(60)).toLong())
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            burstContainer.removeView(particleTv)
                            if (burstContainer.childCount == 0) {
                                decorView.removeView(burstContainer)
                            }
                        }
                        .start()
                }

                anchorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Пружинистая анимация сжатия и отскока облачка сообщения при нажатии или выборе.
     */
    @JvmStatic
    fun animateBubbleSpring(bubbleView: View?) {
        if (bubbleView == null) return
        bubbleView.animate().cancel()
        bubbleView.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(90)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                bubbleView.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(150)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .withEndAction {
                        bubbleView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * Плавная всплывающая анимация отправки/получения нового сообщения.
     */
    @JvmStatic
    fun animateMessagePopIn(bubbleView: View) {
        val density = bubbleView.resources.displayMetrics.density
        bubbleView.animate().cancel()
        bubbleView.translationY = 32f * density
        bubbleView.scaleX = 0.84f
        bubbleView.scaleY = 0.84f
        bubbleView.alpha = 0f

        bubbleView.animate()
            .translationY(0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.25f))
            .start()
    }

    /**
     * Анимация появления плашки реакции под сообщением.
     */
    @JvmStatic
    fun animateReactionChip(chipView: View) {
        chipView.scaleX = 0.2f
        chipView.scaleY = 0.2f
        chipView.alpha = 0f
        chipView.animate().cancel()
        chipView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(240)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    /**
     * Плавный показ панели ответа на сообщение.
     */
    @JvmStatic
    fun animateReplyBarShow(layoutReplyBar: View?, titleTv: TextView?, textTv: TextView?, closeBtn: View?) {
        if (layoutReplyBar == null) return
        layoutReplyBar.animate().cancel()
        layoutReplyBar.animate().setListener(null)

        if (layoutReplyBar.visibility != View.VISIBLE || layoutReplyBar.alpha < 0.5f) {
            val density = layoutReplyBar.resources.displayMetrics.density
            layoutReplyBar.visibility = View.VISIBLE
            layoutReplyBar.translationY = 28f * density
            layoutReplyBar.alpha = 0f

            layoutReplyBar.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        }

        closeBtn?.animate()?.cancel()
        closeBtn?.rotation = -90f
        closeBtn?.animate()
            ?.rotation(0f)
            ?.setDuration(220)
            ?.setInterpolator(OvershootInterpolator(1.3f))
            ?.start()

        titleTv?.animate()?.cancel()
        titleTv?.alpha = 0.2f
        titleTv?.translationX = -12f
        titleTv?.animate()?.alpha(1f)?.translationX(0f)?.setDuration(200)?.start()

        textTv?.animate()?.cancel()
        textTv?.alpha = 0.2f
        textTv?.translationX = -12f
        textTv?.animate()?.alpha(1f)?.translationX(0f)?.setDuration(200)?.setStartDelay(30)?.start()
    }

    /**
     * Плавное скрытие панели ответа на сообщение.
     */
    @JvmStatic
    fun animateReplyBarHide(layoutReplyBar: View?, onComplete: Runnable? = null) {
        if (layoutReplyBar == null || layoutReplyBar.visibility != View.VISIBLE) {
            layoutReplyBar?.visibility = View.GONE
            onComplete?.run()
            return
        }
        val density = layoutReplyBar.resources.displayMetrics.density
        layoutReplyBar.animate().cancel()
        layoutReplyBar.animate()
            .translationY(24f * density)
            .alpha(0f)
            .setDuration(180)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutReplyBar.visibility = View.GONE
                    layoutReplyBar.translationY = 0f
                    layoutReplyBar.alpha = 1f
                    onComplete?.run()
                }
            })
            .start()
    }

    /**
     * Плавный показ панели закрепленного сообщения.
     */
    @JvmStatic
    fun animatePinnedBarEntrance(pinnedBarView: View?, pinIcon: View?, titleTv: TextView?) {
        if (pinnedBarView == null) return
        pinnedBarView.animate().cancel()
        if (pinnedBarView.visibility != View.VISIBLE || pinnedBarView.alpha < 0.5f) {
            val density = pinnedBarView.resources.displayMetrics.density
            pinnedBarView.visibility = View.VISIBLE
            pinnedBarView.translationY = -36f * density
            pinnedBarView.alpha = 0f

            pinnedBarView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(260)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

        pinIcon?.animate()?.cancel()
        pinIcon?.rotation = -180f
        pinIcon?.scaleX = 0.4f
        pinIcon?.scaleY = 0.4f
        pinIcon?.animate()
            ?.rotation(0f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(260)
            ?.setInterpolator(OvershootInterpolator(1.4f))
            ?.start()

        titleTv?.animate()?.cancel()
        titleTv?.alpha = 0.3f
        titleTv?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

    /**
     * Анимация 360° переключения кнопки Голосовое / Видеосообщение.
     */
    @JvmStatic
    fun animateVoiceVideoButtonSwitch(button: View?) {
        if (button == null) return
        button.animate().cancel()
        button.animate()
            .rotationBy(360f)
            .scaleX(0.7f)
            .scaleY(0.7f)
            .setDuration(140)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                button.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .start()
            }
            .start()
    }

    /**
     * Анимация обновления галочек статуса сообщения (Отправлено -> Прочитано).
     */
    @JvmStatic
    fun animateStatusCheckmark(checkView: View?) {
        if (checkView == null) return
        checkView.animate().cancel()
        checkView.scaleX = 0.4f
        checkView.scaleY = 0.4f
        checkView.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(160)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction {
                checkView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private var replyDrawableCache: Drawable? = null
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Отрисовка красивого эластичного индикатора свайпа для ответа за смещаемым сообщением.
     */
    @JvmStatic
    fun drawSwipeToReplyIndicator(
        canvas: Canvas,
        itemView: View,
        dX: Float,
        thresholdPx: Float,
        isTriggered: Boolean
    ) {
        val absDx = Math.abs(dX)
        if (absDx < 4f) return

        val context = itemView.context
        val density = context.resources.displayMetrics.density
        val accentColor = ColorAccentManager.getCurrentAccentColor(context)

        val progress = (absDx / Math.abs(thresholdPx)).coerceIn(0f, 1f)

        if (replyDrawableCache == null) {
            replyDrawableCache = ContextCompat.getDrawable(context, R.drawable.ic_reply)?.mutate()
        }

        val icon = replyDrawableCache ?: return

        val iconSize = (22 * density).toInt()
        val circleRadius = (20 * density) * if (isTriggered) 1.15f else (0.7f + 0.3f * progress)

        val circleCenterX = itemView.right - (32 * density) + (dX * 0.15f)
        val circleCenterY = itemView.top + (itemView.height / 2f)

        bgPaint.color = if (isTriggered) accentColor else Color.parseColor("#33FFFFFF")
        bgPaint.style = Paint.Style.FILL

        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, bgPaint)

        val left = (circleCenterX - iconSize / 2f).toInt()
        val top = (circleCenterY - iconSize / 2f).toInt()
        val right = left + iconSize
        val bottom = top + iconSize

        icon.setBounds(left, top, right, bottom)
        icon.colorFilter = PorterDuffColorFilter(
            if (isTriggered) Color.WHITE else accentColor,
            PorterDuff.Mode.SRC_IN
        )

        canvas.save()
        val rotationAngle = (1.0f - progress) * -45f
        canvas.rotate(rotationAngle, circleCenterX, circleCenterY)
        icon.draw(canvas)
        canvas.restore()
    }
}
