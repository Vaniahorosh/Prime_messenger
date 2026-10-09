package com.messenger.prime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

/**
 * Custom TextView supporting text outline/contour stroke.
 */
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {

    var strokeColor: Int = Color.BLACK
    var strokeWidthPx: Float = 4f
    var isStrokeEnabled: Boolean = false

    override fun onDraw(canvas: Canvas) {
        if (isStrokeEnabled && strokeWidthPx > 0f) {
            val originalTextColor = currentTextColor
            val p = paint

            // 1. Draw outline stroke
            p.style = Paint.Style.STROKE
            p.strokeWidth = strokeWidthPx
            p.strokeJoin = Paint.Join.ROUND
            p.strokeCap = Paint.Cap.ROUND
            setTextColor(strokeColor)
            super.onDraw(canvas)

            // 2. Draw text fill on top
            p.style = Paint.Style.FILL
            setTextColor(originalTextColor)
            super.onDraw(canvas)
        } else {
            super.onDraw(canvas)
        }
    }
}
