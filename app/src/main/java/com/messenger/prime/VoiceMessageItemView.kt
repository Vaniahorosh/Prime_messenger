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

class VoiceMessageItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paintUnplayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pathPlayed = Path()
    private val pathUnplayed = Path()

    private var amplitudes: List<Int> = emptyList()
    private var progress: Float = 0f // 0.0 to 1.0

    var isPlaying: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var wavePhase: Float = 0f
    private var waveFactor: Float = 0f // 0.0 (flat line) to 1.0 (snake wave)

    var onSeekListener: ((Float) -> Unit)? = null

    fun setWaveform(amps: List<Int>) {
        amplitudes = amps
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
        waveFactor += (targetFactor - waveFactor) * 0.15f

        if (isPlaying || abs(waveFactor - targetFactor) > 0.005f) {
            if (isPlaying) {
                wavePhase += 0.16f
            }
            postInvalidateOnAnimation()
        } else {
            waveFactor = targetFactor
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        paintPlayed.color = accentColor

        val centerY = paddingTop + usableHeight / 2f
        val maxAmplitude = (usableHeight * 0.38f) * waveFactor
        val splitX = paddingLeft + usableWidth * progress

        pathPlayed.reset()
        pathUnplayed.reset()

        val steps = (usableWidth / density).toInt().coerceAtLeast(40)
        val stepX = usableWidth.toFloat() / steps

        var isPlayedStarted = false
        var isUnplayedStarted = false

        for (i in 0..steps) {
            val x = paddingLeft + i * stepX
            val sineVal = sin((i * 0.18f) + wavePhase).toFloat()
            val y = centerY + sineVal * maxAmplitude

            if (x <= splitX) {
                if (!isPlayedStarted) {
                    pathPlayed.moveTo(x, y)
                    isPlayedStarted = true
                } else {
                    pathPlayed.lineTo(x, y)
                }
            } else {
                if (!isUnplayedStarted) {
                    if (isPlayedStarted) {
                        val midSine = sin(((splitX - paddingLeft) / stepX * 0.18f) + wavePhase).toFloat()
                        val midY = centerY + midSine * maxAmplitude
                        pathPlayed.lineTo(splitX, midY)
                        pathUnplayed.moveTo(splitX, midY)
                    } else {
                        pathUnplayed.moveTo(x, y)
                    }
                    isUnplayedStarted = true
                }
                pathUnplayed.lineTo(x, y)
            }
        }

        if (isPlayedStarted) {
            canvas.drawPath(pathPlayed, paintPlayed)
        }
        if (isUnplayedStarted) {
            canvas.drawPath(pathUnplayed, paintUnplayed)
        }

        // Glowing head dot on snake progress
        if (progress in 0.01f..0.99f) {
            val headSine = sin(((splitX - paddingLeft) / stepX * 0.18f) + wavePhase).toFloat()
            val headY = centerY + headSine * maxAmplitude
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(splitX, headY, 5f * density, dotPaint)
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
