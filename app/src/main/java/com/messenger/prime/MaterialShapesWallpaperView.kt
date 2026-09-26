package com.messenger.prime

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MaterialShapesWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class ShapeType {
        CIRCLE, ROUNDED_SQUARE, TRIANGLE, DIAMOND, RING, PILL,
        PENTAGON, HEXAGON, STAR_5, STAR_8, CROSS, CONCENTRIC_RINGS, HEART, FLOWER_4,
        BRAND_LOGO
    }

    private data class GridShape(
        val type: ShapeType,
        val col: Int,
        val row: Int,
        val x: Float,
        val y: Float,
        val size: Float,
        val rotation: Float,
        val alphaMultiplier: Float
    )

    private val shapes = mutableListOf<GridShape>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    private val path = Path()
    private val rectF = RectF()
    private var brandLogoDrawable: Drawable? = null

    private val camera3D = Camera()
    private val matrix3D = Matrix()
    private val waveInterpolator = OvershootInterpolator(1.2f)

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    private var maxCols = 1
    private var maxRows = 1

    private var baseColor = Color.TRANSPARENT
    private var baseAlpha = 30

    init {
        brandLogoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_prime_statusbar)
        updateThemeColors()
    }

    fun updateThemeColors() {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        if (darkTheme) {
            // Soft light-gray for dark theme
            baseColor = Color.parseColor("#E2E8F0")
            baseAlpha = 22 // ~8% opacity
        } else {
            // Brand color for light theme (subtle translucent)
            baseColor = ContextCompat.getColor(context, R.color.prime_brand)
            baseAlpha = 32 // ~12% opacity
        }

        paint.color = baseColor
        strokePaint.color = baseColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            generateGridShapes(w, h)
            startEntranceAnimation()
        }
    }

    private fun generateGridShapes(w: Int, h: Int) {
        shapes.clear()
        val density = resources.displayMetrics.density
        val cellSize = (46 * density).toInt() // Small grid cell
        val shapeBaseSize = 17 * density // Small shape size

        maxCols = (w / cellSize) + 1
        maxRows = (h / cellSize) + 1

        val random = Random(System.currentTimeMillis())

        for (col in 0 until maxCols) {
            for (row in 0 until maxRows) {
                // Position jitter
                val offsetX = (random.nextFloat() - 0.5f) * 12 * density
                val offsetY = (random.nextFloat() - 0.5f) * 12 * density

                val cx = (col * cellSize) + (cellSize / 2f) + offsetX
                val cy = (row * cellSize) + (cellSize / 2f) + offsetY

                // ~7% chance for brand logo, otherwise select from diverse Material shapes
                val isLogo = random.nextFloat() < 0.07f
                val type = if (isLogo) {
                    ShapeType.BRAND_LOGO
                } else {
                    val types = arrayOf(
                        ShapeType.CIRCLE,
                        ShapeType.ROUNDED_SQUARE,
                        ShapeType.TRIANGLE,
                        ShapeType.DIAMOND,
                        ShapeType.RING,
                        ShapeType.PILL,
                        ShapeType.PENTAGON,
                        ShapeType.HEXAGON,
                        ShapeType.STAR_5,
                        ShapeType.STAR_8,
                        ShapeType.CROSS,
                        ShapeType.CONCENTRIC_RINGS,
                        ShapeType.HEART,
                        ShapeType.FLOWER_4
                    )
                    types[random.nextInt(types.size)]
                }

                val rot = random.nextFloat() * 360f
                val sizeVar = shapeBaseSize * (0.8f + random.nextFloat() * 0.4f)
                val alphaVar = 0.7f + random.nextFloat() * 0.5f

                shapes.add(GridShape(type, col, row, cx, cy, sizeVar, rot, alphaVar))
            }
        }
    }

    fun startEntranceAnimation() {
        animator?.cancel()
        animProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900 // Smooth wave flip duration
            addUpdateListener { va ->
                animProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (shapes.isEmpty() || animProgress <= 0f) return

        val maxColRow = (maxCols + maxRows).coerceAtLeast(1)

        for (shape in shapes) {
            // Wave progress calculation from top-left to bottom-right
            val normDistance = (shape.col + shape.row).toFloat() / maxColRow
            val waveWindow = 0.45f
            val waveStart = normDistance * (1f - waveWindow)
            val rawProgress = ((animProgress - waveStart) / waveWindow).coerceIn(0f, 1f)

            if (rawProgress <= 0f) continue // Wave hasn't reached shape yet

            val shapeProgress = waveInterpolator.getInterpolation(rawProgress)

            // 3D Card Flip Angle: rotates 180 degrees around Y-axis (from 180° edge-on -> 0° face-up)
            val flipY = (1f - rawProgress) * 180f
            val flipX = sin(rawProgress * Math.PI.toFloat()) * 20f

            val localScale = 0.2f + (0.8f * shapeProgress)
            val currentAlpha = (baseAlpha * shape.alphaMultiplier * rawProgress).toInt().coerceIn(0, 255)

            paint.alpha = currentAlpha
            strokePaint.alpha = currentAlpha

            val radius = shape.size / 2f

            canvas.save()

            // 3D Camera Card Flip
            camera3D.save()
            camera3D.rotateY(flipY)
            camera3D.rotateX(flipX)
            camera3D.getMatrix(matrix3D)
            camera3D.restore()

            matrix3D.preTranslate(-shape.x, -shape.y)
            matrix3D.postTranslate(shape.x, shape.y)
            canvas.concat(matrix3D)

            // 2D Translate, Scale & Rotate
            canvas.translate(shape.x, shape.y)
            canvas.scale(localScale, localScale)
            canvas.rotate(shape.rotation)

            when (shape.type) {
                ShapeType.CIRCLE -> {
                    canvas.drawCircle(0f, 0f, radius, paint)
                }
                ShapeType.ROUNDED_SQUARE -> {
                    rectF.set(-radius, -radius, radius, radius)
                    canvas.drawRoundRect(rectF, radius * 0.35f, radius * 0.35f, paint)
                }
                ShapeType.TRIANGLE -> {
                    path.reset()
                    path.moveTo(0f, -radius)
                    path.lineTo(radius, radius)
                    path.lineTo(-radius, radius)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.DIAMOND -> {
                    path.reset()
                    path.moveTo(0f, -radius)
                    path.lineTo(radius, 0f)
                    path.lineTo(0f, radius)
                    path.lineTo(-radius, 0f)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.RING -> {
                    canvas.drawCircle(0f, 0f, radius, strokePaint)
                }
                ShapeType.PILL -> {
                    rectF.set(-radius * 1.2f, -radius * 0.6f, radius * 1.2f, radius * 0.6f)
                    canvas.drawRoundRect(rectF, radius * 0.6f, radius * 0.6f, paint)
                }
                ShapeType.PENTAGON -> {
                    path.reset()
                    for (i in 0 until 5) {
                        val angle = (i * 2 * Math.PI / 5 - Math.PI / 2).toFloat()
                        val x = (radius * cos(angle)).toFloat()
                        val y = (radius * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.HEXAGON -> {
                    path.reset()
                    for (i in 0 until 6) {
                        val angle = (i * Math.PI / 3 - Math.PI / 2).toFloat()
                        val x = (radius * cos(angle)).toFloat()
                        val y = (radius * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.STAR_5 -> {
                    path.reset()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) radius else radius * 0.45f
                        val angle = (i * Math.PI / 5 - Math.PI / 2).toFloat()
                        val x = (r * cos(angle)).toFloat()
                        val y = (r * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.STAR_8 -> {
                    path.reset()
                    for (i in 0 until 16) {
                        val r = if (i % 2 == 0) radius else radius * 0.5f
                        val angle = (i * Math.PI / 8).toFloat()
                        val x = (r * cos(angle)).toFloat()
                        val y = (r * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.CROSS -> {
                    val w = radius * 0.35f
                    path.reset()
                    path.moveTo(-w, -radius); path.lineTo(w, -radius)
                    path.lineTo(w, -w); path.lineTo(radius, -w)
                    path.lineTo(radius, w); path.lineTo(w, w)
                    path.lineTo(w, radius); path.lineTo(-w, radius)
                    path.lineTo(-w, w); path.lineTo(-radius, w)
                    path.lineTo(-radius, -w); path.lineTo(-w, -w)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.CONCENTRIC_RINGS -> {
                    canvas.drawCircle(0f, 0f, radius, strokePaint)
                    canvas.drawCircle(0f, 0f, radius * 0.5f, strokePaint)
                }
                ShapeType.HEART -> {
                    path.reset()
                    path.moveTo(0f, radius * 0.35f)
                    path.cubicTo(-radius, -radius * 0.6f, -radius * 0.5f, -radius * 1.2f, 0f, -radius * 0.4f)
                    path.cubicTo(radius * 0.5f, -radius * 1.2f, radius, -radius * 0.6f, 0f, radius * 0.35f)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                ShapeType.FLOWER_4 -> {
                    canvas.drawCircle(-radius * 0.35f, 0f, radius * 0.5f, paint)
                    canvas.drawCircle(radius * 0.35f, 0f, radius * 0.5f, paint)
                    canvas.drawCircle(0f, -radius * 0.35f, radius * 0.5f, paint)
                    canvas.drawCircle(0f, radius * 0.35f, radius * 0.5f, paint)
                }
                ShapeType.BRAND_LOGO -> {
                    brandLogoDrawable?.let { drawable ->
                        val logoSize = (shape.size * 1.2f).toInt()
                        drawable.setBounds(-logoSize / 2, -logoSize / 2, logoSize / 2, logoSize / 2)
                        drawable.setTint(baseColor)
                        drawable.alpha = currentAlpha
                        drawable.draw(canvas)
                    }
                }
            }

            canvas.restore()
        }
    }
}
