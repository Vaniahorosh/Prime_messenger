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
 * Official Material 3 Expressive Wavy Progress Indicator (M3 Wavy Slider).
 *
 * Active track:
 *  - When playing: Smooth sine wave with endpoint damping for seamless connection.
 *  - When paused/idle: Morphs amplitude to 0 (flat rounded line segment).
 * Unplayed track:
 *  - Thin straight line with end stop dot.
 */
class VoiceMessageItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // M3 Active Wavy Track Paint
    private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // M3 Inactive Track Line Paint
    private val paintInactive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        strokeCap = Paint.Cap.ROUND
    }

    // M3 Track Stop Dot Paint
    private val paintStopDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF")
        style = Paint.Style.FILL
    }

    private val activePath = Path()
    private val inactivePath = Path()

    private var progress: Float = 0f // 0.0 to 1.0

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

    fun setWaveform(amps: List<Int>?) {
        invalidate()
    }

    fun setProgress(prog: Float) {
        progress = prog.coerceIn(0f, 1f)
        invalidate()
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
                // Calm, fluid 20dp/sec wave movement rate
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
        val startX = paddingLeft + (3f * density)
        val endX = width - paddingRight - (3f * density)
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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val usableWidth = width - paddingLeft - paddingRight
                if (usableWidth > 0) {
                    val tapX = (event.x - paddingLeft).coerceIn(0f, usableWidth.toFloat())
                    val tapProgress = (tapX / usableWidth).coerceIn(0f, 1f)
                    setProgress(tapProgress)
                    onSeekListener?.invoke(tapProgress)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
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
