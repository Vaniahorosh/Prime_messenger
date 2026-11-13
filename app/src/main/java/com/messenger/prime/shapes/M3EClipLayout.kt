package com.messenger.prime.shapes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

class M3EClipLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var clipPath: Path? = null
    private var m3eShape: RoundedPolygon? = null
    private var pathProvider: ((width: Float, height: Float) -> Path)? = null

    fun setShape(shape: RoundedPolygon) {
        this.m3eShape = shape
        this.pathProvider = null
        updatePath()
    }

    fun setPathProvider(provider: (width: Float, height: Float) -> Path) {
        this.pathProvider = provider
        this.m3eShape = null
        updatePath()
    }

    private fun updatePath() {
        if (width == 0 || height == 0) return

        clipPath = if (m3eShape != null) {
            val androidPath = m3eShape!!.toPath()
            val bounds = RectF()
            androidPath.computeBounds(bounds, true)
            val scaleX = width.toFloat() / bounds.width()
            val scaleY = height.toFloat() / bounds.height()
            val matrix = android.graphics.Matrix()
            matrix.postTranslate(-bounds.left, -bounds.top)
            matrix.postScale(scaleX, scaleY)
            androidPath.transform(matrix)
            androidPath
        } else if (pathProvider != null) {
            pathProvider!!.invoke(width.toFloat(), height.toFloat())
        } else {
            null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && clipPath != null) {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setPath(clipPath!!)
                }
            }
        } else {
            // Force redraw for manual clipping on canvas
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && clipPath != null) {
            val saveCount = canvas.save()
            canvas.clipPath(clipPath!!)
            super.dispatchDraw(canvas)
            canvas.restoreToCount(saveCount)
        } else {
            super.dispatchDraw(canvas)
        }
    }
}
