package com.messenger.prime.shapes

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.Path
import android.graphics.RectF
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class M3EDrawable(
    private val shape: RoundedPolygon,
    private val color: Int
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@M3EDrawable.color
        style = Paint.Style.FILL
    }
    private val path = Path()
    private var lastWidth = 0
    private var lastHeight = 0

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() == 0 || b.height() == 0) return
        if (b.width() != lastWidth || b.height() != lastHeight) {
            lastWidth = b.width()
            lastHeight = b.height()

            val androidPath = shape.toPath()
            val pathBounds = RectF()
            androidPath.computeBounds(pathBounds, true)

            if (pathBounds.width() > 0 && pathBounds.height() > 0) {
                val scaleX = b.width().toFloat() / pathBounds.width()
                val scaleY = b.height().toFloat() / pathBounds.height()
                val matrix = android.graphics.Matrix()
                matrix.postTranslate(-pathBounds.left, -pathBounds.top)
                matrix.postScale(scaleX, scaleY)
                androidPath.transform(matrix)

                path.rewind()
                path.addPath(androidPath)
            }
        }
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

object M3EShapes {

    val circle: RoundedPolygon by lazy { RoundedPolygon(
        numVertices = 8,
        rounding = CornerRounding(1f)
    ) }

    val square: RoundedPolygon by lazy { RoundedPolygon(
        numVertices = 4,
        rounding = CornerRounding(0.1f)
    ) }

    fun slanted(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.moveTo(width * 0.2f, 0f)
        path.lineTo(width, 0f)
        path.lineTo(width * 0.8f, height)
        path.lineTo(0f, height)
        path.close()
        return path
    }

    // Many of these are custom Paths or RoundedPolygons.
    // For pure Path based ones, we can just return a Path normalized to [0, 1] bounds.
    fun arch(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(width, height)
        path.lineTo(width, height / 2)
        path.addArc(RectF(0f, 0f, width, height), 0f, -180f)
        path.lineTo(0f, height)
        path.close()
        return path
    }

    fun semicircle(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.addArc(RectF(0f, 0f, width, height * 2), 180f, 180f)
        path.close()
        return path
    }

    fun pill(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, width, height), minOf(width, height) / 2f, minOf(width, height) / 2f, Path.Direction.CW)
        return path
    }

    val triangle: RoundedPolygon by lazy { RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.2f)) }

    val clamshell: RoundedPolygon by lazy { RoundedPolygon(numVertices = 8, rounding = CornerRounding(0.3f)) }

    val pentagon: RoundedPolygon by lazy { RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.3f)) }

    val gem: RoundedPolygon by lazy { RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.3f)) }

    val verySunny: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 8, radius = 1f, innerRadius = 0.6f, rounding = CornerRounding(0.1f)) }

    val sunny: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 8, radius = 1f, innerRadius = 0.7f, rounding = CornerRounding(0.15f)) }

    val cookie4Sided: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 4, radius = 1f, innerRadius = 0.7f, rounding = CornerRounding(0.3f)) }
    val cookie6Sided: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 6, radius = 1f, innerRadius = 0.8f, rounding = CornerRounding(0.3f)) }
    val cookie7Sided: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 7, radius = 1f, innerRadius = 0.8f, rounding = CornerRounding(0.3f)) }
    val cookie9Sided: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 9, radius = 1f, innerRadius = 0.8f, rounding = CornerRounding(0.3f)) }
    val cookie12Sided: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 12, radius = 1f, innerRadius = 0.85f, rounding = CornerRounding(0.2f)) }

    val clover4Leaf: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 4, radius = 1f, innerRadius = 0.4f, rounding = CornerRounding(0.4f)) }
    val clover8Leaf: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 8, radius = 1f, innerRadius = 0.6f, rounding = CornerRounding(0.3f)) }

    val burst: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 12, radius = 1f, innerRadius = 0.5f, rounding = CornerRounding(0.02f)) }
    val softBurst: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 10, radius = 1f, innerRadius = 0.6f, rounding = CornerRounding(0.1f)) }

    val boom: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 14, radius = 1f, innerRadius = 0.3f, rounding = CornerRounding(0f)) }
    val softBoom: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 16, radius = 1f, innerRadius = 0.5f, rounding = CornerRounding(0.05f)) }

    val flower: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 8, radius = 1f, innerRadius = 0.4f, rounding = CornerRounding(0.4f), innerRounding = CornerRounding(0.05f)) }

    val puffy: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 10, radius = 1f, innerRadius = 0.7f, rounding = CornerRounding(0.4f)) }
    val puffyDiamond: RoundedPolygon by lazy { RoundedPolygon.star(numVerticesPerRadius = 4, radius = 1f, innerRadius = 0.5f, rounding = CornerRounding(0.4f)) }

    // Complex custom paths (normalized 0..1 bounds)
    fun heart(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        val hw = width / 2f
        path.moveTo(hw, height * 0.25f)
        path.cubicTo(width * 0.1f, -height * 0.1f, -width * 0.15f, height * 0.6f, hw, height * 0.95f)
        path.moveTo(hw, height * 0.25f)
        path.cubicTo(width * 0.9f, -height * 0.1f, width * 1.15f, height * 0.6f, hw, height * 0.95f)
        path.close()
        return path
    }

    fun ghostIsh(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, width, height * 0.8f), width / 2f, width / 2f, Path.Direction.CW)
        // Add wavy bottom manually or just return an arch for now
        return path
    }

    fun bun(width: Float = 1f, height: Float = 1f): Path {
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, width, height * 0.45f), width * 0.2f, width * 0.2f, Path.Direction.CW)
        path.addRoundRect(RectF(0f, height * 0.55f, width, height), width * 0.2f, width * 0.2f, Path.Direction.CW)
        return path
    }
}
