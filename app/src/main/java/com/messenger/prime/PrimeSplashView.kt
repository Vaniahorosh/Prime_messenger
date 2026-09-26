package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.PathParser
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Анимированный векторный логотип Prime Messenger (@drawable/ic_prime_statusbar.xml)
 * Со статичной крупной брендовой надписью "всегда будь в прайме".
 */
class PrimeSplashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Векторные пути из ic_prime_statusbar.xml (viewport 486x544)
    private val pathMain: Path = PathParser.createPathFromPathData(
        "M115,13 L257,13 C268,13 277,22 277,34 C277,45 268,54 257,54 L116,54 C81,54 53,82 53,117 L53,319 L136,402 C144,410 144,423 136,432 C128,440 115,440 107,432 L17,342 C12,337 10,331 10,324 L10,117 C10,59 57,13 115,13 Z"
    )
    private val pathTopRight: Path = PathParser.createPathFromPathData(
        "M302,13 L371,13 C430,13 477,59 477,117 L477,198 C477,210 468,219 456,219 C444,219 434,210 434,198 L434,117 C434,82 406,54 371,54 L302,54 C290,54 281,45 281,34 C281,22 290,13 302,13 Z"
    )
    private val pathBottomRight: Path = PathParser.createPathFromPathData(
        "M443,236 C449,236 455,239 460,244 C467,251 467,262 460,270 L341,389 C333,397 321,397 313,389 C305,381 305,369 313,361 L428,246 C432,240 437,237 443,236 Z"
    )
    private val pathTail: Path = PathParser.createPathFromPathData(
        "M286,389 C293,389 300,393 305,400 C313,410 313,422 305,431 L205,529 C195,538 181,531 176,526 C166,517 156,507 161,495 C163,490 166,486 170,482 L270,393 C275,390 280,389 286,389 Z"
    )

    private val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val tealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (isDarkTheme) Color.parseColor("#3CAEA3") else Color.WHITE
    }

    private val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F1B33")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (isDarkTheme) Color.parseColor("#3CAEA3") else Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkTheme) Color.parseColor("#3CAEA3") else Color.WHITE
        textSize = 22f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(8f, 0f, 2f, Color.parseColor("#60000000"))
    }

    private var iconScale = 0f
    private var iconAlpha = 0f
    private var dot1Scale = 1f
    private var dot2Scale = 1f
    private var dot3Scale = 1f

    private val fastOutSlowIn = FastOutSlowInInterpolator()
    private val overshootInterpolator = OvershootInterpolator(1.25f)

    private var onAnimationFinished: (() -> Unit)? = null

    fun setOnAnimationFinishedListener(listener: () -> Unit) {
        this.onAnimationFinished = listener
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        startSmoothMasterAnimation()
    }

    private fun startSmoothMasterAnimation() {
        val totalDurationMs = 380L

        val masterAnimator = ValueAnimator.ofFloat(0f, totalDurationMs.toFloat()).apply {
            duration = totalDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val timeMs = anim.animatedValue as Float

                // 1. Быстрое упругое появление иконки (0 мс .. 140 мс)
                val iconProgress = (timeMs / 140f).coerceIn(0f, 1f)
                iconAlpha = iconProgress
                iconScale = overshootInterpolator.getInterpolation(iconProgress)

                // 2. Стремительная жидкая волна точек (80 мс на точку)
                dot1Scale = calculateDotWaveScale(timeMs, startMs = 100f, durationMs = 80f)
                dot2Scale = calculateDotWaveScale(timeMs, startMs = 130f, durationMs = 80f)
                dot3Scale = calculateDotWaveScale(timeMs, startMs = 160f, durationMs = 80f)

                invalidate()
            }
        }

        masterAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                postDelayed({
                    onAnimationFinished?.invoke()
                }, 20)
            }
        })

        masterAnimator.start()
    }

    private fun calculateDotWaveScale(timeMs: Float, startMs: Float, durationMs: Float): Float {
        if (timeMs < startMs || timeMs > (startMs + durationMs)) return 1.0f
        val progress = (timeMs - startMs) / durationMs
        val sineVal = sin(progress * PI.toFloat()).coerceIn(0f, 1f)
        val easedSine = fastOutSlowIn.getInterpolation(sineVal)
        return 1.0f - 0.70f * easedSine
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val logoCenterY = h / 2f - (25f * resources.displayMetrics.density)
        val targetWidthPx = Math.min(w, h * (486f / 544f)) * 0.40f
        val targetHeightPx = targetWidthPx * (544f / 486f)
        val scale = (targetWidthPx / 486f) * iconScale

        // --- Отрисовка анимированного логотипа ---
        if (scale > 0f) {
            val alphaInt = (iconAlpha * 255).toInt().coerceIn(0, 255)
            tealPaint.alpha = alphaInt
            tailPaint.alpha = alphaInt
            dotPaint.alpha = alphaInt

            canvas.save()
            canvas.translate(w / 2f, logoCenterY)
            canvas.scale(scale, scale)
            canvas.translate(-243f, -272f)

            canvas.drawPath(pathMain, tealPaint)
            canvas.drawPath(pathTopRight, tealPaint)
            canvas.drawPath(pathBottomRight, tealPaint)
            canvas.drawPath(pathTail, tailPaint)

            canvas.drawCircle(161f, 210f, 26f * dot1Scale, dotPaint)
            canvas.drawCircle(243f, 210f, 26f * dot2Scale, dotPaint)
            canvas.drawCircle(326f, 210f, 26f * dot3Scale, dotPaint)

            canvas.restore()
        }

        // --- Отрисовка статичной крупной брендовой надписи "всегда будь в прайме" по центру ---
        val baseTextY = logoCenterY + (targetHeightPx / 2f) + (42f * resources.displayMetrics.density)
        canvas.drawText("всегда будь в прайме", w / 2f, baseTextY, textPaint)
    }
}
