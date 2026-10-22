package com.messenger.prime

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Path as AndroidPath
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom Loading View that replaces standard ProgressBar.
 * Supports both spinning/water filling morphing shapes AND border frame progress mode.
 */
class PrimeWaterLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    var isBorderProgressMode: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isScrubbingEnabled: Boolean = false

    var onProgressChangeListener: ((Int) -> Unit)? = null

    var staticShapeType: String = "circle"
        set(value) {
            field = value
            invalidate()
        }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#154B87")
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#42A5F5")
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#40FFFFFF")
    }

    private val activeProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#00E676")
        strokeCap = Paint.Cap.ROUND
    }

    private val reusableNativePath = AndroidPath()
    private val waterPath = AndroidPath()
    private val pathMeasure = PathMeasure()
    private val segmentPath = AndroidPath()

    private var morphProgress = 0f
    private var rotationDegrees = 0f
    private var waveOffset = 0f

    private var animator: ValueAnimator? = null

    // Shapes
    private val shapes = listOf(
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.35f, rounding = CornerRounding(0.25f)),
        RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.75f, rounding = CornerRounding(0.35f)),
        RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.50f, rounding = CornerRounding(0.3f)),
        RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.82f, rounding = CornerRounding(0.4f)),
        RoundedPolygon.star(numVerticesPerRadius = 3, innerRadius = 0.5f, rounding = CornerRounding(0.3f)),
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.8f, rounding = CornerRounding(0.2f))
    )

    private var currentShapeIndex = 0
    private var nextShapeIndex = 1
    private var activeMorph = Morph(shapes[0], shapes[1])

    init {
        progressDrawable = null
    }

    fun setIndicatorColor(color: Int) {
        strokePaint.color = color
        activeProgressPaint.color = color
        invalidate()
    }

    fun setWaterColor(color: Int) {
        waterPaint.color = color
        invalidate()
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (progress > 0 && progress < max && !isBorderProgressMode) {
            isIndeterminate = false
        }
        invalidate()
    }

    @Synchronized
    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        if (progress > 0 && progress < max && !isBorderProgressMode) {
            isIndeterminate = false
        }
        invalidate()
    }

    private fun startAnimation() {
        if (animator?.isStarted == true) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (!isBorderProgressMode) {
                    morphProgress += 0.015f
                    if (morphProgress >= 1f) {
                        morphProgress = 0f
                        currentShapeIndex = nextShapeIndex
                        nextShapeIndex = Random.nextInt(shapes.size)
                        while (nextShapeIndex == currentShapeIndex) {
                            nextShapeIndex = Random.nextInt(shapes.size)
                        }
                        activeMorph = Morph(shapes[currentShapeIndex], shapes[nextShapeIndex])
                    }

                    rotationDegrees += 1.5f
                    waveOffset += 0.05f
                }
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            animator?.cancel()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (48 * resources.displayMetrics.density).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredSize, heightSize)
            else -> desiredSize
        }

        val finalHeight = if (height < desiredSize / 2) desiredSize else height
        val finalWidth = if (width < desiredSize / 2) desiredSize else width

        setMeasuredDimension(finalWidth, finalHeight)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (!isScrubbingEnabled || !isBorderProgressMode) return super.onTouchEvent(event)

        val x = event.x - width / 2f
        val y = event.y - height / 2f

        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN,
            android.view.MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                // Calculate angle from center (0 degrees is top, clockwise)
                var angle = Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat()
                angle += 90f // Shift 0 to top
                if (angle < 0f) angle += 360f

                val newProgress = ((angle / 360f) * max).toInt().coerceIn(0, max)
                progress = newProgress
                onProgressChangeListener?.invoke(newProgress)
                invalidate()
                return true
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun drawBorderProgress(canvas: Canvas, w: Float, h: Float) {
        val radius = min(w, h) * 0.45f
        reusableNativePath.rewind()

        val density = resources.displayMetrics.density
        var cornerRadiusPx = radius
        try {
            val percent = staticShapeType.trim().toInt()
            cornerRadiusPx = (radius * (percent / 50f)).coerceIn(0f, radius)
        } catch (e: Exception) {
            when (staticShapeType) {
                "square" -> cornerRadiusPx = 0f
                "star" -> cornerRadiusPx = radius * 0.7f
                "prime" -> cornerRadiusPx = radius * 0.4f
                else -> cornerRadiusPx = radius
            }
        }

        val rect = RectF(-radius, -radius, radius, radius)
        reusableNativePath.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, AndroidPath.Direction.CW)

        canvas.save()
        canvas.translate(w / 2f, h / 2f)

        // Track frame
        canvas.drawPath(reusableNativePath, trackPaint)

        // Active perimeter progress
        pathMeasure.setPath(reusableNativePath, false)
        val length = pathMeasure.length
        val fillPercent = if (max > 0) progress.toFloat() / max.toFloat() else 0f

        segmentPath.rewind()
        pathMeasure.getSegment(0f, length * fillPercent, segmentPath, true)
        canvas.drawPath(segmentPath, activeProgressPaint)

        canvas.restore()
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (isBorderProgressMode) {
            drawBorderProgress(canvas, w, h)
            return
        }

        val radius = min(w, h) * 0.45f

        reusableNativePath.rewind()
        val cubics = activeMorph.asCubics(morphProgress)
        var isFirst = true
        cubics.forEach { cubic ->
            if (isFirst) {
                reusableNativePath.moveTo(cubic.anchor0X * radius, cubic.anchor0Y * radius)
                isFirst = false
            }
            reusableNativePath.cubicTo(
                cubic.control0X * radius, cubic.control0Y * radius,
                cubic.control1X * radius, cubic.control1Y * radius,
                cubic.anchor1X * radius, cubic.anchor1Y * radius
            )
        }
        reusableNativePath.close()

        canvas.save()
        canvas.translate(w / 2f, h / 2f)
        canvas.rotate(rotationDegrees)

        canvas.drawPath(reusableNativePath, strokePaint)
        canvas.clipPath(reusableNativePath)

        val fillPercentage = if (isIndeterminate) {
            0.5f + 0.1f * sin(waveOffset * 3f)
        } else {
            if (max > 0) progress.toFloat() / max.toFloat() else 0f
        }

        val waterY = radius - (radius * 2 * fillPercentage)

        waterPath.rewind()
        waterPath.moveTo(-radius * 1.5f, waterY)

        val waveAmplitude = radius * 0.15f
        val waveFrequency = 1.5f / radius
        var x = -radius * 1.5f
        while (x <= radius * 1.5f) {
            val y = waterY + waveAmplitude * sin((x + waveOffset * radius * 0.5f) * waveFrequency)
            waterPath.lineTo(x, y)
            x += 10f
        }
        waterPath.lineTo(radius * 1.5f, radius * 1.5f)
        waterPath.lineTo(-radius * 1.5f, radius * 1.5f)
        waterPath.close()

        canvas.rotate(-rotationDegrees)
        canvas.drawPath(waterPath, waterPaint)

        canvas.restore()
    }
}
