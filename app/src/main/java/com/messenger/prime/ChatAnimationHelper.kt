package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Утилита для легких и сдержанных Material-анимаций в чате:
 * - Короткий отклик реакции при двойном тапе
 * - Плавное появление новых сообщений
 * - Свайп для ответа
 * - Быстрые переходы панелей (150–200 мс)
 */
object ChatAnimationHelper {

    private val fastOutSlowIn = FastOutSlowInInterpolator()
    private val decelerate = DecelerateInterpolator()
    private val accelerate = AccelerateInterpolator()

    /**
     * Легкая анимация вылета реакции при двойном тапе (1 TextView без лишних теней и частиц).
     */
    @JvmStatic
    fun spawnReactionBurst(anchorView: View?, reactionEmoji: String?) {
        if (anchorView == null || reactionEmoji.isNullOrEmpty()) return
        val context = anchorView.context ?: return
        anchorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (PrimeTransitions.areAnimationsDisabled(context)) return

        val activity = (context as? Activity) ?: return
        val decorView = activity.window?.decorView as? ViewGroup ?: return

        anchorView.post {
            try {
                val density = context.resources.displayMetrics.density
                val location = IntArray(2)
                anchorView.getLocationOnScreen(location)

                val anchorX = location[0] + (anchorView.width / 2)
                val anchorY = location[1] + (anchorView.height / 2)

                val reactionTv = TextView(context).apply {
                    text = reactionEmoji
                    textSize = 28f
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = anchorX - (20 * density).toInt()
                        topMargin = anchorY - (20 * density).toInt()
                    }
                    scaleX = 0.7f
                    scaleY = 0.7f
                    alpha = 0.95f
                }
                decorView.addView(reactionTv)

                reactionTv.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .translationY(-56f * density)
                    .alpha(0.0f)
                    .setDuration(260)
                    .setInterpolator(fastOutSlowIn)
                    .withEndAction {
                        decorView.removeView(reactionTv)
                    }
                    .start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Легкий тактильный отклик пузыря при двойном тапе.
     */
    @JvmStatic
    fun animateBubbleSpring(bubbleView: View?) {
        if (bubbleView == null) return
        if (PrimeTransitions.areAnimationsDisabled(bubbleView.context)) return
        bubbleView.animate().cancel()
        bubbleView.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .setInterpolator(decelerate)
            .withEndAction {
                bubbleView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .setInterpolator(fastOutSlowIn)
                    .start()
            }
            .start()
    }

    /**
     * Быстрое появление нового сообщения (fade + легкий сдвиг снизу).
     */
    @JvmStatic
    fun animateMessagePopIn(bubbleView: View) {
        if (PrimeTransitions.areAnimationsDisabled(bubbleView.context)) {
            bubbleView.translationY = 0f
            bubbleView.scaleX = 1f
            bubbleView.scaleY = 1f
            bubbleView.alpha = 1f
            return
        }
        val density = bubbleView.resources.displayMetrics.density
        bubbleView.animate().cancel()
        bubbleView.translationY = 14f * density
        bubbleView.scaleX = 0.96f
        bubbleView.scaleY = 0.96f
        bubbleView.alpha = 0f

        bubbleView.animate()
            .translationY(0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(170)
            .setInterpolator(fastOutSlowIn)
            .start()
    }

    /**
     * Анимация появления плашки реакции под сообщением.
     */
    @JvmStatic
    fun animateReactionChip(chipView: View) {
        if (PrimeTransitions.areAnimationsDisabled(chipView.context)) {
            chipView.scaleX = 1f
            chipView.scaleY = 1f
            chipView.alpha = 1f
            return
        }
        chipView.scaleX = 0.85f
        chipView.scaleY = 0.85f
        chipView.alpha = 0f
        chipView.animate().cancel()
        chipView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(150)
            .setInterpolator(fastOutSlowIn)
            .start()
    }

    /**
     * Быстрый показ панели ответа/редактирования.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun animateReplyBarShow(layoutReplyBar: View?, titleTv: TextView?, textTv: TextView?, closeBtn: View?) {
        if (layoutReplyBar == null) return
        layoutReplyBar.animate().cancel()
        layoutReplyBar.animate().setListener(null)
        setBlurAutoUpdateSafe(layoutReplyBar, true)

        if (PrimeTransitions.areAnimationsDisabled(layoutReplyBar.context)) {
            layoutReplyBar.translationY = 0f
            layoutReplyBar.alpha = 1f
            layoutReplyBar.visibility = View.VISIBLE
            return
        }

        if (layoutReplyBar.visibility != View.VISIBLE || layoutReplyBar.alpha < 0.5f) {
            val density = layoutReplyBar.resources.displayMetrics.density
            layoutReplyBar.translationY = 16f * density
            layoutReplyBar.alpha = 0f
            layoutReplyBar.visibility = View.VISIBLE

            layoutReplyBar.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(170)
                .setInterpolator(fastOutSlowIn)
                .start()
        }
    }

    /**
     * Быстрое скрытие панели ответа/редактирования.
     */
    @JvmStatic
    fun animateReplyBarHide(layoutReplyBar: View?, onComplete: Runnable? = null) {
        if (layoutReplyBar == null || layoutReplyBar.visibility != View.VISIBLE) {
            layoutReplyBar?.visibility = View.GONE
            setBlurAutoUpdateSafe(layoutReplyBar, false)
            onComplete?.run()
            return
        }
        if (PrimeTransitions.areAnimationsDisabled(layoutReplyBar.context)) {
            layoutReplyBar.visibility = View.GONE
            layoutReplyBar.translationY = 0f
            layoutReplyBar.alpha = 1f
            setBlurAutoUpdateSafe(layoutReplyBar, false)
            onComplete?.run()
            return
        }
        val density = layoutReplyBar.resources.displayMetrics.density
        layoutReplyBar.animate().cancel()
        layoutReplyBar.animate()
            .translationY(12f * density)
            .alpha(0f)
            .setDuration(140)
            .setInterpolator(accelerate)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutReplyBar.visibility = View.GONE
                    layoutReplyBar.translationY = 0f
                    layoutReplyBar.alpha = 1f
                    setBlurAutoUpdateSafe(layoutReplyBar, false)
                    onComplete?.run()
                }
            })
            .start()
    }

    /**
     * Плавный показ панели закрепленного сообщения.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun animatePinnedBarEntrance(pinnedBarView: View?, pinIcon: View?, titleTv: TextView?) {
        if (pinnedBarView == null) return
        pinnedBarView.animate().cancel()
        setBlurAutoUpdateSafe(pinnedBarView, true)
        if (PrimeTransitions.areAnimationsDisabled(pinnedBarView.context)) {
            pinnedBarView.visibility = View.VISIBLE
            pinnedBarView.translationY = 0f
            pinnedBarView.alpha = 1f
            return
        }
        if (pinnedBarView.visibility != View.VISIBLE || pinnedBarView.alpha < 0.5f) {
            val density = pinnedBarView.resources.displayMetrics.density
            pinnedBarView.visibility = View.VISIBLE
            pinnedBarView.translationY = -16f * density
            pinnedBarView.alpha = 0f

            pinnedBarView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(fastOutSlowIn)
                .start()
        }
    }

    /**
     * Быстрое переключение кнопки Голосовое / Видеосообщение без 360-вращения.
     */
    @JvmStatic
    fun animateVoiceVideoButtonSwitch(button: View?) {
        if (button == null) return
        if (PrimeTransitions.areAnimationsDisabled(button.context)) return
        button.animate().cancel()
        button.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(80)
            .setInterpolator(decelerate)
            .withEndAction {
                button.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(110)
                    .setInterpolator(fastOutSlowIn)
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
        if (PrimeTransitions.areAnimationsDisabled(checkView.context)) return
        checkView.animate().cancel()
        checkView.scaleX = 0.75f
        checkView.scaleY = 0.75f
        checkView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .setInterpolator(fastOutSlowIn)
            .start()
    }

    private var replyDrawableCache: Drawable? = null
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Отрисовка индикатора свайпа для ответа за смещаемым сообщением.
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

        val iconSize = (20 * density).toInt()
        val circleRadius = (18 * density) * if (isTriggered) 1.08f else (0.75f + 0.25f * progress)

        val circleCenterX = itemView.right - (32 * density) + (dX * 0.15f)
        val circleCenterY = itemView.top + (itemView.height / 2f)

        bgPaint.color = if (isTriggered) accentColor else Color.parseColor("#2694A3B8")
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

        icon.draw(canvas)
    }
}
