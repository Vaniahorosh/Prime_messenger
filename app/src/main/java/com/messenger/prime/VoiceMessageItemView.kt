package com.messenger.prime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/**
 * Material 3 Expressive Wavy Progress Bar (M3 Snake Slider).
 *
 * Unplayed track: Thin line + round end dot.
 * Played track (Snake segment):
 *  - When paused/idle: Thick flat line segment with rounded caps.
 *  - When playing: Thick animated wavy squiggle ("змейка") that flows along progress.
 */
class VoiceMessageItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Thick active played segment (wavy snake or flat line)
    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Thin unplayed background track line
    private val paintUnplayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        strokeCap = Paint.Cap.ROUND
    }

    // Small end dot paint for unplayed track
    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF")
        style = Paint.Style.FILL
    }

    private val pathPlayed = Path()
    private val pathUnplayed = Path()

    private var progress: Float = 0f // 0.0 to 1.0

    var isPlaying: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var wavePhase: Float = 0f
    private var waveFactor: Float = 0f // 0.0 (flat line) to 1.0 (wavy snake)

    var onSeekListener: ((Float) -> Unit)? = null

    fun setWaveform(amps: List<Int>?) {
        // Retained for API compatibility
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

        val targetFactor = if (isPlaying) 1.0f else 0.0f
        waveFactor += (targetFactor - waveFactor) * 0.16f

        if (isPlaying || abs(waveFactor - targetFactor) > 0.003f) {
            if (isPlaying) {
                wavePhase += 0.20f
            }
            postInvalidateOnAnimation()
        } else {
            waveFactor = targetFactor
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        paintPlayed.color = accentColor

        val centerY = paddingTop + usableHeight / 2f
        val startX = paddingLeft + (3f * density)
        val endX = width - paddingRight - (3f * density)
        val activeWidth = endX - startX
        if (activeWidth <= 0) return

        val splitX = startX + activeWidth * progress.coerceIn(0f, 1f)

        // 1. Draw thin unplayed track line
        pathUnplayed.reset()
        if (splitX < endX) {
            pathUnplayed.moveTo(splitX, centerY)
            pathUnplayed.lineTo(endX, centerY)
            canvas.drawPath(pathUnplayed, paintUnplayed)
        }
        // Draw small dot at the end of unplayed track
        paintDot.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(endX, centerY, 2.5f * density, paintDot)

        // 2. Draw thick active played track (flat segment when paused, wavy squiggle when playing)
        val playedLength = splitX - startX
        if (playedLength > 0.5f * density) {
            pathPlayed.reset()
            pathPlayed.moveTo(startX, centerY)

            val waveAmplitude = (4.5f * density) * waveFactor
            val frequency = 0.22f / density

            if (waveFactor < 0.01f) {
                // Flat thick line segment
                pathPlayed.lineTo(splitX, centerY)
            } else {
                // Wavy squiggle line segment ("змейка")
                val stepX = 2f * density
                var currX = startX
                while (currX < splitX) {
                    currX += stepX
                    if (currX > splitX) currX = splitX

                    val sineVal = sin(((currX - startX) * frequency) - wavePhase).toFloat()
                    val currY = centerY + (sineVal * waveAmplitude)
                    pathPlayed.lineTo(currX, currY)
                }
            }

            canvas.drawPath(pathPlayed, paintPlayed)
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
