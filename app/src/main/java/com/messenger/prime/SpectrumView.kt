package com.messenger.prime

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SpectrumView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val huePaint = Paint()
    private val saturationPaint = Paint()
    private var hue = 0f
    private var saturation = 1f
    private var value = 1f

    private var onColorChanged: ((Int) -> Unit)? = null

    private val hues = IntArray(361) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
    private var hueShader: Shader? = null
    private var satShader: Shader? = null
    private var lastHueForShader = -1f

    init {
        huePaint.isAntiAlias = true
        saturationPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            hueShader = LinearGradient(0f, 0f, w.toFloat(), 0f, hues, null, Shader.TileMode.CLAMP)
            huePaint.shader = hueShader
            updateSatShader(w.toFloat(), h.toFloat())
        }
    }

    private fun updateSatShader(w: Float = width.toFloat(), h: Float = height.toFloat()) {
        if (w <= 0 || h <= 0) return
        val currentColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        satShader = LinearGradient(0f, h / 2f, w, h / 2f,
            intArrayOf(Color.WHITE, currentColor, Color.BLACK), null, Shader.TileMode.CLAMP)
        saturationPaint.shader = satShader
        lastHueForShader = hue
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        if (hueShader == null) {
            hueShader = LinearGradient(0f, 0f, width, 0f, hues, null, Shader.TileMode.CLAMP)
            huePaint.shader = hueShader
        }
        if (satShader == null || lastHueForShader != hue) {
            updateSatShader(width, height)
        }

        val rx = 16f
        val rectHue = RectF(0f, 0f, width, height / 2f - 4f)
        canvas.drawRoundRect(rectHue, rx, rx, huePaint)

        val rectSat = RectF(0f, height / 2f + 4f, width, height)
        canvas.drawRoundRect(rectSat, rx, rx, saturationPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val x = event.x.coerceIn(0f, width.toFloat())
            val y = event.y.coerceIn(0f, height.toFloat())

            if (y < height / 2f) {
                hue = (x / width) * 360f
            } else {
                val progress = x / width
                if (progress < 0.5f) {
                    saturation = progress * 2f
                    value = 1f
                } else {
                    saturation = 1f
                    value = 1f - (progress - 0.5f) * 2f
                }
            }

            val color = Color.HSVToColor(floatArrayOf(hue, saturation, value))
            onColorChanged?.invoke(color)
            invalidate()
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setOnColorChangedListener(listener: (Int) -> Unit) {
        onColorChanged = listener
    }
}
