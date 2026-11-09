package com.messenger.prime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Material 3 Expressive Wavy SeekBar / Slider.
 *
 * Dedicated ultra-smooth seekbar for media playback and scrubbing.
 * Features:
 *  - M3 Wavy Progress Indicator (smooth sine wave with endpoint damping).
 *  - 1:1 touch tracking with 44dp hit area.
 *  - Prevents player progress updates while user is scrubbing.
 *  - Glowing thumb dot on touch.
 */
class WavySeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Active track (thick line or wavy sine)
    private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Inactive background track line
    private val paintInactive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        strokeCap = Paint.Cap.ROUND
    }

    // End stop dot paint
    private val paintStopDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF")
        style = Paint.Style.FILL
    }

    // Interactive thumb dot paint
    private val paintThumb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val activePath = Path()
    private val inactivePath = Path()

    private var progress: Float = 0f // 0.0 to 1.0

    var isScrubbing: Boolean = false
        private set

    var isPlaying: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var wavePhase: Float = 0f
    private var currentAmplitude: Float = 0f
    private var lastFrameTimeNanos: Long = 0L

    var onSeekListener: ((Float) -> Unit)? = null
    var onScrubStateChangeListener: ((Boolean) -> Unit)? = null

    fun setProgress(prog: Float) {
        if (!isScrubbing) {
            progress = prog.coerceIn(0f, 1f)
            invalidate()
        }
    }

    fun forceProgress(prog: Float) {
        progress = prog.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minHeight = (44 * density).toInt()
        val h = resolveSize(minHeight, heightMeasureSpec)
        val w = resolveSize((200 * density).toInt(), widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val usableWidth = width - paddingLeft - paddingRight
        val usableHeight = height - paddingTop - paddingBottom
        if (usableWidth <= 0 || usableHeight <= 0) return

        val now = System.nanoTime()
        val dtSec = if (lastFrameTimeNanos > 0L && isPlaying) {
            ((now - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        } else {
            0.016f
        }
        lastFrameTimeNanos = if (isPlaying) now else 0L

        val targetAmplitude = if (isPlaying) (4.5f * density) else 0f
        currentAmplitude += (targetAmplitude - currentAmplitude) * (12f * dtSec).coerceIn(0.05f, 0.5f)

        if (isPlaying || abs(currentAmplitude - targetAmplitude) > 0.05f) {
            if (isPlaying) {
                wavePhase += (20f * density) * dtSec
            }
            postInvalidateOnAnimation()
        } else {
            currentAmplitude = targetAmplitude
            lastFrameTimeNanos = 0L
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        paintActive.color = accentColor

        val centerY = paddingTop + usableHeight / 2f
        val startX = paddingLeft + (4f * density)
        val endX = width - paddingRight - (4f * density)
        val activeWidth = endX - startX
        if (activeWidth <= 0) return

        val splitX = startX + activeWidth * progress.coerceIn(0f, 1f)

        // 1. Draw Inactive Track Line + Stop Dot
        inactivePath.reset()
        if (splitX < endX) {
            inactivePath.moveTo(splitX, centerY)
            inactivePath.lineTo(endX, centerY)
            canvas.drawPath(inactivePath, paintInactive)
        }
        paintStopDot.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(endX, centerY, 2.5f * density, paintStopDot)

        // 2. Draw Active M3 Wavy Segment
        val playedLength = splitX - startX
        if (playedLength > 0.5f * density) {
            activePath.reset()
            activePath.moveTo(startX, centerY)

            val wavelength = 22f * density
            val fadeDistance = 12f * density

            if (currentAmplitude < 0.1f) {
                // Flat active segment
                activePath.lineTo(splitX, centerY)
            } else {
                // M3 Expressive Sine Wave with endpoint damping
                val stepX = 1.5f * density
                var currX = startX
                while (currX <= splitX) {
                    val d1 = min(1f, (currX - startX) / fadeDistance)
                    val d2 = min(1f, (splitX - currX) / fadeDistance)
                    val damping = min(d1, d2).coerceIn(0f, 1f)

                    val sineVal = sin((2.0 * PI * (currX - wavePhase) / wavelength)).toFloat()
                    val currY = centerY + (currentAmplitude * damping * sineVal)

                    if (currX == startX) {
                        activePath.moveTo(currX, currY)
                    } else {
                        activePath.lineTo(currX, currY)
                    }

                    if (currX == splitX) break
                    currX += stepX
                    if (currX > splitX) currX = splitX
                }
            }

            canvas.drawPath(activePath, paintActive)
        }

        // 3. Draw Thumb Dot when scrubbing
        if (isScrubbing || isPlaying || progress in 0.01f..0.99f) {
            val thumbRadius = if (isScrubbing) 6.5f * density else 4.5f * density
            paintThumb.color = accentColor
            canvas.drawCircle(splitX, centerY, thumbRadius, paintThumb)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val startX = paddingLeft + (4f * density)
        val endX = width - paddingRight - (4f * density)
        val activeWidth = endX - startX

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isScrubbing = true
                parent?.requestDisallowInterceptTouchEvent(true)
                onScrubStateChangeListener?.invoke(true)
                if (activeWidth > 0) {
                    val tapX = (event.x - startX).coerceIn(0f, activeWidth)
                    val newProgress = (tapX / activeWidth).coerceIn(0f, 1f)
                    progress = newProgress
                    invalidate()
                    onSeekListener?.invoke(newProgress)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeWidth > 0) {
                    val tapX = (event.x - startX).coerceIn(0f, activeWidth)
                    val newProgress = (tapX / activeWidth).coerceIn(0f, 1f)
                    progress = newProgress
                    invalidate()
                    onSeekListener?.invoke(newProgress)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScrubbing = false
                parent?.requestDisallowInterceptTouchEvent(false)
                onScrubStateChangeListener?.invoke(false)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
