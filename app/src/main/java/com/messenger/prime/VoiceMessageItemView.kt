package com.messenger.prime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class VoiceMessageItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    
    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Accent color
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }
    
    private val paintUnplayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF") // Dimmed color
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private var amplitudes: List<Int> = emptyList()
    private var progress: Float = 0f // 0.0 to 1.0

    private val barWidth = 3f * density
    private val barSpacing = 2f * density
    private val minBarHeight = 3f * density
    private val rectF = RectF()

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

        val totalBars = (usableWidth / (barWidth + barSpacing)).toInt().coerceAtLeast(1)
        
        val displayAmps = IntArray(totalBars)
        if (amplitudes.isEmpty()) {
            // Draw default quiet bars
            displayAmps.fill(5)
        } else if (amplitudes.size <= totalBars) {
            for (i in displayAmps.indices) {
                if (i < amplitudes.size) {
                    displayAmps[i] = amplitudes[i]
                } else {
                    displayAmps[i] = 2
                }
            }
        } else {
            val step = amplitudes.size.toFloat() / totalBars
            for (i in 0 until totalBars) {
                val startIdx = (i * step).toInt()
                val endIdx = (((i + 1) * step).toInt()).coerceAtMost(amplitudes.size)
                var max = 0
                for (j in startIdx until endIdx) {
                    if (amplitudes[j] > max) max = amplitudes[j]
                }
                displayAmps[i] = max
            }
        }

        val centerY = paddingTop + usableHeight / 2f
        val maxBarHeight = usableHeight * 0.9f
        val splitIndex = (totalBars * progress).toInt()

        for (i in 0 until totalBars) {
            val amp = displayAmps[i].coerceIn(0, 100)
            val barHeight = ((amp / 100f) * maxBarHeight).coerceAtLeast(minBarHeight)
            
            val left = paddingLeft + i * (barWidth + barSpacing)
            val top = centerY - barHeight / 2f
            val right = left + barWidth
            val bottom = centerY + barHeight / 2f

            rectF.set(left, top, right, bottom)
            
            val paint = if (i <= splitIndex && progress > 0f) paintPlayed else paintUnplayed
            canvas.drawRoundRect(rectF, barWidth / 2f, barWidth / 2f, paint)
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