package com.messenger.prime

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

// --- M3 Expressive Shapes Setup ---
private object M3ExpressiveShapes {
    // 1. 4-pointed expressive star
    val star4 = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.35f,
        rounding = CornerRounding(0.25f)
    )

    // 2. 8-pointed scalloped flower / clover
    val clover8 = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.75f,
        rounding = CornerRounding(0.35f)
    )

    // 3. 5-pointed smooth star
    val star5 = RoundedPolygon.star(
        numVerticesPerRadius = 5,
        innerRadius = 0.50f,
        rounding = CornerRounding(0.3f)
    )

    // 4. Smooth 6-pointed rounded polygon / squircle
    val squircle6 = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        innerRadius = 0.82f,
        rounding = CornerRounding(0.4f)
    )

    val morphs = listOf(
        Morph(star4, clover8),
        Morph(clover8, star5),
        Morph(star5, squircle6),
        Morph(squircle6, star4)
    )
}

// Convert Morph progress (0..1) to Compose Path using fixed intrinsic centroid (0,0)
private fun Morph.toComposePathForLoading(progress: Float, radius: Float): Path {
    val androidPath = AndroidPath()
    val cubics = this.asCubics(progress)
    var isFirst = true
    cubics.forEach { cubic ->
        if (isFirst) {
            androidPath.moveTo(cubic.anchor0X * radius, cubic.anchor0Y * radius)
            isFirst = false
        }
        androidPath.cubicTo(
            cubic.control0X * radius, cubic.control0Y * radius,
            cubic.control1X * radius, cubic.control1Y * radius,
            cubic.anchor1X * radius, cubic.anchor1Y * radius
        )
    }
    androidPath.close()
    return androidPath.asComposePath()
}

// Convert Morph progress (0..1) to Android Path using fixed intrinsic centroid (0,0)
private fun Morph.toAndroidPathForLoading(progress: Float, radius: Float): AndroidPath {
    val androidPath = AndroidPath()
    val cubics = this.asCubics(progress)
    var isFirst = true
    cubics.forEach { cubic ->
        if (isFirst) {
            androidPath.moveTo(cubic.anchor0X * radius, cubic.anchor0Y * radius)
            isFirst = false
        }
        androidPath.cubicTo(
            cubic.control0X * radius, cubic.control0Y * radius,
            cubic.control1X * radius, cubic.control1Y * radius,
            cubic.anchor1X * radius, cubic.anchor1Y * radius
        )
    }
    androidPath.close()
    return androidPath
}

/**
 * Custom Compose M3 Expressive Loading Indicator.
 * Solid filled M3 Expressive shapes morphing seamlessly in place on their exact central axis without rotation.
 */
@Composable
fun M3ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 56.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loader_stationary")

    // Continuous morph progress 0..4
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph"
    )

    // Gentle breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radiusPx = this.size.minDimension * 0.42f

            val totalMorphs = M3ExpressiveShapes.morphs.size
            val currentSegment = morphProgress.toInt().coerceIn(0, totalMorphs - 1)
            val segmentProgress = morphProgress - currentSegment

            val activeMorph = M3ExpressiveShapes.morphs[currentSegment]
            val path = activeMorph.toComposePathForLoading(segmentProgress, radiusPx)

            // Translate to exact center of Canvas and draw stationary morphing shape
            translate(center.x, center.y) {
                scale(pulseScale) {
                    // Soft radial outer glow
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                            radius = radiusPx * 1.4f
                        ),
                        style = Fill
                    )

                    // Main solid filled shape
                    drawPath(
                        path = path,
                        color = color,
                        style = Fill
                    )

                    // Subtle inner highlight contour
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Custom Android View for XML layouts or programmatic Java/Kotlin view usage.
 * Solid filled M3 Expressive shapes morphing seamlessly in place on central axis without rotation.
 */
class M3ExpressiveLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF154B87.toInt()
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0x40FFFFFF
    }

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    init {
        startAnimation()
    }

    fun setColor(colorInt: Int) {
        fillPaint.color = colorInt
        invalidate()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 4f).apply {
            duration = 3200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                animProgress = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator?.isStarted != true) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val radius = Math.min(w, h) * 0.38f

        val totalMorphs = M3ExpressiveShapes.morphs.size
        val currentSegment = animProgress.toInt().coerceIn(0, totalMorphs - 1)
        val segmentProgress = animProgress - currentSegment

        val activeMorph = M3ExpressiveShapes.morphs[currentSegment]
        val path = activeMorph.toAndroidPathForLoading(segmentProgress, radius)

        // Translate to exact center of View and draw stationary morphing shape
        canvas.save()
        canvas.translate(w / 2f, h / 2f)
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        canvas.restore()
    }
}
