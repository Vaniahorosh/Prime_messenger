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

    init {
        huePaint.isAntiAlias = true
        saturationPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        // Рисуем спектр (Hue) по горизонтали
        val hues = IntArray(361) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
        val hueShader = LinearGradient(0f, 0f, width, 0f, hues, null, Shader.TileMode.CLAMP)
        huePaint.shader = hueShader
        canvas.drawRect(0f, 0f, width, height / 2, huePaint)

        // Рисуем Saturation/Value по горизонтали (упрощенно: от белого к текущему Hue, затем к черному)
        val hsv = floatArrayOf(hue, 1f, 1f)
        val currentColor = Color.HSVToColor(hsv)
        val satShader = LinearGradient(0f, height / 2, width, height / 2, 
            intArrayOf(Color.WHITE, currentColor, Color.BLACK), null, Shader.TileMode.CLAMP)
        saturationPaint.shader = satShader
        canvas.drawRect(0f, height / 2, width, height, saturationPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val x = event.x.coerceIn(0f, width.toFloat())
            val y = event.y.coerceIn(0f, height.toFloat())

            if (y < height / 2) {
                hue = (x / width) * 360f
            } else {
                val progress = x / width
                if (progress < 0.5f) {
                    // От белого к цвету (насыщенность)
                    saturation = progress * 2f
                    value = 1f
                } else {
                    // От цвета к черному (яркость)
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
