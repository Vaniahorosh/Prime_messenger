package com.messenger.prime

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.messenger.prime.shapes.M3EShapes
import java.io.File
import kotlin.math.sin
import kotlin.random.Random

class MaterialShapesWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class ShapeType {
        CIRCLE, SQUARE, TRIANGLE, PENTAGON, GEM, CLAMSHELL,
        SUNNY, VERY_SUNNY, COOKIE_4, COOKIE_6, COOKIE_7, COOKIE_9, COOKIE_12,
        CLOVER_4, CLOVER_8, BURST, SOFT_BURST, BOOM, SOFT_BOOM,
        FLOWER, PUFFY, PUFFY_DIAMOND, ARCH, SEMICIRCLE, PILL, SLANTED, HEART,
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

    private var brandLogoDrawable: Drawable? = null

    private val matrix3D = Matrix()
    private val waveInterpolator = OvershootInterpolator(1.2f)

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    private var maxCols = 1
    private var maxRows = 1

    private var baseColor = Color.TRANSPARENT
    private var baseAlpha = 30

    private val unitPaths = mutableMapOf<ShapeType, Path>()

    init {
        brandLogoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_prime_statusbar)
        initUnitPaths()
        updateThemeColors()
    }

    private fun Path.normalizeToUnit(): Path {
        val bounds = RectF()
        this.computeBounds(bounds, true)
        val width = bounds.width()
        val height = bounds.height()
        val maxDim = maxOf(width, height)
        if (maxDim <= 0f) return this
        val scale = 2f / maxDim
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        val matrix = Matrix()
        matrix.postTranslate(-centerX, -centerY)
        matrix.postScale(scale, scale)
        this.transform(matrix)
        return this
    }

    private fun RoundedPolygon.toNormalizedPath(): Path {
        return this.toPath().normalizeToUnit()
    }

    private fun initUnitPaths() {
        unitPaths[ShapeType.CIRCLE] = M3EShapes.circle.toNormalizedPath()
        unitPaths[ShapeType.SQUARE] = M3EShapes.square.toNormalizedPath()
        unitPaths[ShapeType.TRIANGLE] = M3EShapes.triangle.toNormalizedPath()
        unitPaths[ShapeType.PENTAGON] = M3EShapes.pentagon.toNormalizedPath()
        unitPaths[ShapeType.GEM] = M3EShapes.gem.toNormalizedPath()
        unitPaths[ShapeType.CLAMSHELL] = M3EShapes.clamshell.toNormalizedPath()
        unitPaths[ShapeType.SUNNY] = M3EShapes.sunny.toNormalizedPath()
        unitPaths[ShapeType.VERY_SUNNY] = M3EShapes.verySunny.toNormalizedPath()
        unitPaths[ShapeType.COOKIE_4] = M3EShapes.cookie4Sided.toNormalizedPath()
        unitPaths[ShapeType.COOKIE_6] = M3EShapes.cookie6Sided.toNormalizedPath()
        unitPaths[ShapeType.COOKIE_7] = M3EShapes.cookie7Sided.toNormalizedPath()
        unitPaths[ShapeType.COOKIE_9] = M3EShapes.cookie9Sided.toNormalizedPath()
        unitPaths[ShapeType.COOKIE_12] = M3EShapes.cookie12Sided.toNormalizedPath()
        unitPaths[ShapeType.CLOVER_4] = M3EShapes.clover4Leaf.toNormalizedPath()
        unitPaths[ShapeType.CLOVER_8] = M3EShapes.clover8Leaf.toNormalizedPath()
        unitPaths[ShapeType.BURST] = M3EShapes.burst.toNormalizedPath()
        unitPaths[ShapeType.SOFT_BURST] = M3EShapes.softBurst.toNormalizedPath()
        unitPaths[ShapeType.BOOM] = M3EShapes.boom.toNormalizedPath()
        unitPaths[ShapeType.SOFT_BOOM] = M3EShapes.softBoom.toNormalizedPath()
        unitPaths[ShapeType.FLOWER] = M3EShapes.flower.toNormalizedPath()
        unitPaths[ShapeType.PUFFY] = M3EShapes.puffy.toNormalizedPath()
        unitPaths[ShapeType.PUFFY_DIAMOND] = M3EShapes.puffyDiamond.toNormalizedPath()
        unitPaths[ShapeType.ARCH] = M3EShapes.arch().normalizeToUnit()
        unitPaths[ShapeType.SEMICIRCLE] = M3EShapes.semicircle().normalizeToUnit()
        unitPaths[ShapeType.PILL] = M3EShapes.pill().normalizeToUnit()
        unitPaths[ShapeType.SLANTED] = M3EShapes.slanted().normalizeToUnit()
        unitPaths[ShapeType.HEART] = M3EShapes.heart().normalizeToUnit()
    }

    private var isLavaEnabled = true
    private var contactKey: String? = null
    private var customImageBitmap: Bitmap? = null
    private var activeWallpaper: ChatWallpaper? = null

    fun setContactKey(key: String?) {
        if (this.contactKey != key) {
            this.activeWallpaper = null
        }
        this.contactKey = key
        updateThemeColors()
    }

    fun setLiveTempWallpaper(wallpaper: ChatWallpaper, bitmap: Bitmap? = null) {
        this.activeWallpaper = wallpaper
        if (wallpaper.type == "IMAGE") {
            if (bitmap != null) {
                this.customImageBitmap = bitmap
            } else if (!wallpaper.imagePath.isNullOrEmpty()) {
                val f = File(wallpaper.imagePath)
                if (f.exists()) {
                    try {
                        this.customImageBitmap = BitmapFactory.decodeFile(f.absolutePath)
                    } catch (_: Exception) {
                        this.customImageBitmap = null
                    }
                }
            }
        } else {
            this.customImageBitmap = null
        }

        if (width > 0 && height > 0) {
            generateGridShapes(width, height)
        }
        invalidate()
    }

    fun refreshWallpaper() {
        activeWallpaper = null
        updateThemeColors()
    }

    fun updateThemeColors() {
        val wp = activeWallpaper ?: WallpaperManager.getActiveWallpaper(context, contactKey)
        activeWallpaper = wp

        if (wp.type == "IMAGE" && !wp.imagePath.isNullOrEmpty()) {
            val f = File(wp.imagePath)
            if (f.exists()) {
                try {
                    customImageBitmap = BitmapFactory.decodeFile(f.absolutePath)
                } catch (_: Exception) {
                    customImageBitmap = null
                }
            } else {
                customImageBitmap = null
            }
        } else {
            customImageBitmap = null
        }

        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        isLavaEnabled = sharedPrefs.getBoolean("settings_lava_bg", true)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val customBgHex = wp.backgroundColor
        val isDefaultBg = customBgHex.isNullOrBlank() || customBgHex.equals("#151D2A", ignoreCase = true) || customBgHex.equals("#0F172A", ignoreCase = true)

        val effectiveBgColor = when {
            !isDefaultBg -> {
                try { Color.parseColor(customBgHex) } catch (_: Exception) { if (darkTheme) Color.parseColor("#0F172A") else Color.parseColor("#F1F5F9") }
            }
            darkTheme -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#F1F5F9")
        }

        // Luminance check to detect light vs dark background
        val red = Color.red(effectiveBgColor)
        val green = Color.green(effectiveBgColor)
        val blue = Color.blue(effectiveBgColor)
        val isLightBg = (0.299 * red + 0.587 * green + 0.114 * blue) > 160

        if (isLightBg) {
            baseColor = ColorAccentManager.getCurrentAccentColor(context)
            baseAlpha = 42
        } else {
            baseColor = Color.parseColor("#E2E8F0")
            baseAlpha = 24
        }

        paint.color = baseColor
        strokePaint.color = baseColor

        val isLoopingPattern = wp.type == "PATTERN" && wp.isAnimated && wp.animationEffect in listOf("float", "pulse", "rotate")
        if (isLoopingPattern && (animator == null || !animator!!.isRunning)) {
            startEntranceAnimation()
        }

        invalidate()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateThemeColors()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val isLowRam = activityManager?.isLowRamDevice == true

            if (isLowRam) {
                shapes.clear()
                invalidate()
                return
            }

            generateGridShapes(w, h)
            startEntranceAnimation()
        }
    }

    private fun generateGridShapes(w: Int, h: Int) {
        shapes.clear()
        val density = resources.displayMetrics.density
        val cellSize = (46 * density).toInt()
        val shapeBaseSize = 17 * density

        maxCols = (w / cellSize) + 1
        maxRows = (h / cellSize) + 1

        val random = Random(System.currentTimeMillis())

        for (col in 0 until maxCols) {
            for (row in 0 until maxRows) {
                val offsetX = (random.nextFloat() - 0.5f) * 12 * density
                val offsetY = (random.nextFloat() - 0.5f) * 12 * density

                val cx = (col * cellSize) + (cellSize / 2f) + offsetX
                val cy = (row * cellSize) + (cellSize / 2f) + offsetY

                val isLogo = random.nextFloat() < 0.07f
                val type = if (isLogo) {
                    ShapeType.BRAND_LOGO
                } else {
                    val types = when (activeWallpaper?.patternType) {
                        "material_geo" -> arrayOf(
                            ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.PENTAGON,
                            ShapeType.GEM, ShapeType.CLAMSHELL, ShapeType.ARCH,
                            ShapeType.SEMICIRCLE, ShapeType.SLANTED, ShapeType.PILL
                        )
                        "material_stars" -> arrayOf(
                            ShapeType.SUNNY, ShapeType.VERY_SUNNY, ShapeType.BURST,
                            ShapeType.SOFT_BURST, ShapeType.BOOM, ShapeType.SOFT_BOOM,
                            ShapeType.COOKIE_4, ShapeType.COOKIE_6, ShapeType.COOKIE_7,
                            ShapeType.COOKIE_12, ShapeType.CLOVER_4, ShapeType.CLOVER_8
                        )
                        "material_waves" -> arrayOf(
                            ShapeType.FLOWER, ShapeType.PUFFY, ShapeType.PUFFY_DIAMOND,
                            ShapeType.CIRCLE, ShapeType.SEMICIRCLE, ShapeType.PILL, ShapeType.HEART
                        )
                        else -> arrayOf(
                            ShapeType.CIRCLE, ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.PENTAGON,
                            ShapeType.GEM, ShapeType.CLAMSHELL, ShapeType.SUNNY, ShapeType.VERY_SUNNY,
                            ShapeType.COOKIE_4, ShapeType.COOKIE_6, ShapeType.COOKIE_7, ShapeType.COOKIE_9,
                            ShapeType.COOKIE_12, ShapeType.CLOVER_4, ShapeType.CLOVER_8, ShapeType.BURST,
                            ShapeType.SOFT_BURST, ShapeType.BOOM, ShapeType.SOFT_BOOM, ShapeType.FLOWER,
                            ShapeType.PUFFY, ShapeType.PUFFY_DIAMOND, ShapeType.ARCH, ShapeType.SEMICIRCLE,
                            ShapeType.PILL, ShapeType.SLANTED, ShapeType.HEART
                        )
                    }
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
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        if (sp.getBoolean("disable_animations", false)) {
            animProgress = 1f
            invalidate()
            return
        }

        animProgress = 0f
        val wp = activeWallpaper ?: WallpaperManager.getActiveWallpaper(context, contactKey)
        val isLoopingPatternAnim = wp.type == "PATTERN" && wp.isAnimated && wp.animationEffect in listOf("float", "pulse", "rotate")

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isLoopingPatternAnim) 3000L else 900L
            repeatCount = if (isLoopingPatternAnim) ValueAnimator.INFINITE else 0
            repeatMode = ValueAnimator.RESTART
            if (isLoopingPatternAnim) {
                interpolator = android.view.animation.LinearInterpolator()
            }
            addUpdateListener { va ->
                animProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE) {
            animator?.pause()
        } else if (animator?.isPaused == true) {
            animator?.resume()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val wp = activeWallpaper ?: WallpaperManager.getActiveWallpaper(context, contactKey)

        if (wp.type == "IMAGE" && customImageBitmap != null) {
            val bmp = customImageBitmap!!
            val viewW = width.toFloat()
            val viewH = height.toFloat()

            if (viewW > 0 && viewH > 0) {
                val scaleX = viewW / bmp.width
                val scaleY = viewH / bmp.height
                val scale = Math.max(scaleX, scaleY) * wp.scale

                val dx = (viewW - bmp.width * scale) / 2f + wp.offsetX
                val dy = (viewH - bmp.height * scale) / 2f + wp.offsetY

                matrix3D.reset()
                matrix3D.postScale(scale, scale)
                if (wp.rotation != 0f) {
                    matrix3D.postRotate(wp.rotation, viewW / 2f, viewH / 2f)
                }
                matrix3D.postTranslate(dx, dy)

                val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bmp, matrix3D, drawPaint)

                // Dimming overlay
                if (wp.dimOpacity > 0f) {
                    val dimPaint = Paint().apply {
                        color = Color.BLACK
                        alpha = (wp.dimOpacity * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.drawRect(0f, 0f, viewW, viewH, dimPaint)
                }
            }
            return
        }

        if (wp.type == "PATTERN") {
            drawEmojiPattern(canvas, wp)
            return
        }

        // MATERIAL / DEFAULT mode: Draw background color if set
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val customBgHex = wp.backgroundColor
        val isDefaultBg = customBgHex.isNullOrBlank() || customBgHex.equals("#151D2A", ignoreCase = true) || customBgHex.equals("#0F172A", ignoreCase = true)

        val effectiveBgColor = when {
            !isDefaultBg -> {
                try { Color.parseColor(customBgHex) } catch (_: Exception) { if (darkTheme) Color.parseColor("#0F172A") else Color.parseColor("#F1F5F9") }
            }
            darkTheme -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#F1F5F9")
        }

        canvas.drawColor(effectiveBgColor)

        super.onDraw(canvas)
        if (!isLavaEnabled) return
        if (shapes.isEmpty() || animProgress <= 0f) return

        val maxColRow = (maxCols + maxRows).coerceAtLeast(1)

        for (shape in shapes) {
            val normDistance = (shape.col + shape.row).toFloat() / maxColRow
            val waveWindow = 0.45f
            val waveStart = normDistance * (1f - waveWindow)
            val rawProgress = ((animProgress - waveStart) / waveWindow).coerceIn(0f, 1f)

            if (rawProgress <= 0f) continue

            val shapeProgress = waveInterpolator.getInterpolation(rawProgress)

            val localScale = 0.2f + (0.8f * shapeProgress)
            val currentAlpha = (baseAlpha * shape.alphaMultiplier * rawProgress).toInt().coerceIn(0, 255)

            paint.alpha = currentAlpha
            strokePaint.alpha = currentAlpha

            val radius = shape.size / 2f

            canvas.save()

            // 1. Move canvas to shape position
            canvas.translate(shape.x, shape.y)

            // 2. Apply 2D scale and rotation around local (0,0)
            canvas.scale(localScale, localScale)
            canvas.rotate(shape.rotation)

            when (shape.type) {
                ShapeType.BRAND_LOGO -> {
                    brandLogoDrawable?.let { drawable ->
                        val logoSize = (shape.size * 1.2f).toInt()
                        drawable.setBounds(-logoSize / 2, -logoSize / 2, logoSize / 2, logoSize / 2)
                        drawable.setTint(baseColor)
                        drawable.alpha = currentAlpha
                        drawable.draw(canvas)
                    }
                }
                else -> {
                    unitPaths[shape.type]?.let { unitPath ->
                        canvas.save()
                        canvas.scale(radius, radius)
                        canvas.drawPath(unitPath, paint)
                        canvas.restore()
                    }
                }
            }

            canvas.restore()
        }
    }

    private fun drawEmojiPattern(canvas: Canvas, wp: ChatWallpaper) {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val customBgHex = wp.backgroundColor
        val isDefaultBg = customBgHex.isNullOrBlank() || customBgHex.equals("#151D2A", ignoreCase = true) || customBgHex.equals("#0F172A", ignoreCase = true)

        val effectiveBgColor = when {
            !isDefaultBg -> {
                try { Color.parseColor(customBgHex) } catch (_: Exception) { if (darkTheme) Color.parseColor("#0F172A") else Color.parseColor("#F1F5F9") }
            }
            darkTheme -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#F1F5F9")
        }

        canvas.drawColor(effectiveBgColor)

        val emojis = wp.customEmojis.let {
            if (it.isNullOrBlank()) "😎 ⭐ 🌸 🔷" else it
        }

        val parts = emojis.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return

        val density = resources.displayMetrics.density
        val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f * density * wp.scale
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }

        val cellW = 68f * density * wp.scale
        val cellH = 68f * density * wp.scale

        val cols = (width / cellW).toInt() + 2
        val rows = (height / cellH).toInt() + 2

        canvas.save()

        if (wp.isAnimated && animProgress > 0f) {
            when (wp.animationEffect) {
                "float" -> {
                    val animOffsetY = sin(animProgress * Math.PI.toFloat() * 2) * 10f * density
                    canvas.translate(0f, animOffsetY)
                }
                "pulse" -> {
                    val scale = 0.96f + sin(animProgress * Math.PI.toFloat() * 2) * 0.04f
                    canvas.scale(scale, scale, width / 2f, height / 2f)
                }
                "rotate" -> {
                    val rot = animProgress * 360f
                    canvas.rotate(rot, width / 2f, height / 2f)
                }
            }
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val index = (r * cols + c) % parts.size
                val emojiStr = parts[index]

                var x = c * cellW + (cellW / 2f)
                val y = r * cellH + (cellH / 2f)

                if (wp.layoutOrientation == "staggered" && r % 2 == 1) {
                    x += cellW / 2f
                } else if (wp.layoutOrientation == "spiral") {
                    x += (r % 3) * (cellW / 3f)
                }

                canvas.drawText(emojiStr, x, y, textPaint)
            }
        }

        canvas.restore()

        if (wp.dimOpacity > 0f) {
            val dimPaint = Paint().apply {
                color = Color.BLACK
                alpha = (wp.dimOpacity * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        }
    }
}
