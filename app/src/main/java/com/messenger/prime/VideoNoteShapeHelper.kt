package com.messenger.prime

import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.messenger.prime.shapes.M3EShapes

/**
 * Universal helper for Material 3 Expressive (M3E) video note ("кружок") shapes.
 * Handles shape resolution from tags, hardware outline clipping for Views & TextureViews,
 * and exact perimeter Path calculation for PrimeWaterLoadingView progress rings.
 */
object VideoNoteShapeHelper {

    data class ShapeOption(
        val tag: String,
        val displayName: String,
        val polygon: RoundedPolygon? = null
    )

    val AVAILABLE_SHAPES by lazy {
        listOf(
            ShapeOption("50", "Круг", M3EShapes.circle),
            ShapeOption("0", "Квадрат", M3EShapes.square),
            ShapeOption("m3e_flower", "Цветок", M3EShapes.flower),
            ShapeOption("m3e_cookie6Sided", "Печенье 6", M3EShapes.cookie6Sided),
            ShapeOption("m3e_verySunny", "Солнце", M3EShapes.verySunny),
            ShapeOption("m3e_clover4Leaf", "Клевер", M3EShapes.clover4Leaf),
            ShapeOption("m3e_burst", "Вспышка", M3EShapes.burst),
            ShapeOption("m3e_puffy", "Облако", M3EShapes.puffy),
            ShapeOption("m3e_puffyDiamond", "Ромб", M3EShapes.puffyDiamond),
            ShapeOption("m3e_gem", "Кристалл", M3EShapes.gem),
            ShapeOption("m3e_cookie7Sided", "Звезда 7", M3EShapes.cookie7Sided),
            ShapeOption("m3e_cookie12Sided", "Печенье 12", M3EShapes.cookie12Sided),
            ShapeOption("m3e_cookie4Sided", "Печенье 4", M3EShapes.cookie4Sided),
            ShapeOption("m3e_clover8Leaf", "Цветок 8", M3EShapes.clover8Leaf),
            ShapeOption("m3e_sunny", "Мягкое солнце", M3EShapes.sunny),
            ShapeOption("m3e_softBurst", "Мягкая вспышка", M3EShapes.softBurst),
            ShapeOption("m3e_boom", "Взрыв", M3EShapes.boom),
            ShapeOption("m3e_softBoom", "Мягкий взрыв", M3EShapes.softBoom),
            ShapeOption("m3e_pentagon", "Пятиугольник", M3EShapes.pentagon),
            ShapeOption("m3e_clamshell", "Ракушка", M3EShapes.clamshell),
            ShapeOption("m3e_triangle", "Треугольник", M3EShapes.triangle),
            ShapeOption("m3e_heart", "Сердце", null)
        )
    }

    @JvmStatic
    fun getDisplayName(context: android.content.Context, tag: String?, fallback: String = ""): String {
        return when (tag?.trim()?.lowercase()) {
            "50", "circle" -> PrimeLocaleManager.getString(context, R.string.shape_circle)
            "0", "square" -> PrimeLocaleManager.getString(context, R.string.shape_square)
            "m3e_flower", "flower" -> PrimeLocaleManager.getString(context, R.string.shape_flower)
            "m3e_cookie6sided", "cookie6sided" -> PrimeLocaleManager.getString(context, R.string.shape_cookie6)
            "m3e_verysunny", "verysunny" -> PrimeLocaleManager.getString(context, R.string.shape_very_sunny)
            "m3e_clover4leaf", "clover4leaf" -> PrimeLocaleManager.getString(context, R.string.shape_clover4)
            "m3e_burst", "burst" -> PrimeLocaleManager.getString(context, R.string.shape_burst)
            "m3e_puffy", "puffy" -> PrimeLocaleManager.getString(context, R.string.shape_puffy)
            "m3e_puffydiamond", "puffydiamond" -> PrimeLocaleManager.getString(context, R.string.shape_puffy_diamond)
            "m3e_gem", "gem" -> PrimeLocaleManager.getString(context, R.string.shape_gem)
            "m3e_cookie7sided", "cookie7sided" -> PrimeLocaleManager.getString(context, R.string.shape_cookie7)
            "m3e_cookie12sided", "cookie12sided" -> PrimeLocaleManager.getString(context, R.string.shape_cookie12)
            "m3e_cookie4sided", "cookie4sided" -> PrimeLocaleManager.getString(context, R.string.shape_cookie4)
            "m3e_clover8leaf", "clover8leaf" -> PrimeLocaleManager.getString(context, R.string.shape_clover8)
            "m3e_sunny", "sunny" -> PrimeLocaleManager.getString(context, R.string.shape_sunny)
            "m3e_softburst", "softburst" -> PrimeLocaleManager.getString(context, R.string.shape_soft_burst)
            "m3e_boom", "boom" -> PrimeLocaleManager.getString(context, R.string.shape_boom)
            "m3e_softboom", "softboom" -> PrimeLocaleManager.getString(context, R.string.shape_soft_boom)
            "m3e_pentagon", "pentagon" -> PrimeLocaleManager.getString(context, R.string.shape_pentagon)
            "m3e_clamshell", "clamshell" -> PrimeLocaleManager.getString(context, R.string.shape_clamshell)
            "m3e_triangle", "triangle" -> PrimeLocaleManager.getString(context, R.string.shape_triangle)
            "m3e_heart", "heart" -> PrimeLocaleManager.getString(context, R.string.shape_heart)
            else -> fallback
        }
    }

    /**
     * Resolves a shape tag to an M3E RoundedPolygon if applicable.
     */
    @JvmStatic
    fun getShapePolygon(tag: String?): RoundedPolygon? {
        if (tag.isNullOrBlank()) return null
        val clean = tag.trim().lowercase().removePrefix("m3e_")

        return when (clean) {
            "flower" -> M3EShapes.flower
            "cookie6sided", "cookie6" -> M3EShapes.cookie6Sided
            "verysunny" -> M3EShapes.verySunny
            "clover4leaf", "clover" -> M3EShapes.clover4Leaf
            "burst" -> M3EShapes.burst
            "puffy" -> M3EShapes.puffy
            "puffydiamond" -> M3EShapes.puffyDiamond
            "gem" -> M3EShapes.gem
            "cookie7sided", "cookie7", "star" -> M3EShapes.cookie7Sided
            "cookie12sided", "cookie12" -> M3EShapes.cookie12Sided
            "cookie4sided", "cookie4" -> M3EShapes.cookie4Sided
            "cookie9sided", "cookie9" -> M3EShapes.cookie9Sided
            "clover8leaf" -> M3EShapes.clover8Leaf
            "sunny" -> M3EShapes.sunny
            "softburst" -> M3EShapes.softBurst
            "boom" -> M3EShapes.boom
            "softboom" -> M3EShapes.softBoom
            "pentagon" -> M3EShapes.pentagon
            "clamshell" -> M3EShapes.clamshell
            "triangle" -> M3EShapes.triangle
            "circle" -> M3EShapes.circle
            "square" -> M3EShapes.square
            else -> null
        }
    }

    /**
     * Converts a tag to a corner radius percentage (0..50).
     */
    @JvmStatic
    fun getCornerRadiusPercent(tag: String?, fallback: Int = 50): Int {
        if (tag.isNullOrBlank()) return fallback
        val clean = tag.trim().lowercase()
        if (clean == "circle" || clean == "50") return 50
        if (clean == "square" || clean == "0") return 0
        if (clean == "star") return 35
        if (clean == "prime") return 25
        val num = clean.removePrefix("radius_").toIntOrNull()
        if (num != null) return num.coerceIn(0, 50)
        return fallback
    }

    /**
     * Checks if a tag represents an M3E custom shape (non-slider standard rect).
     */
    @JvmStatic
    fun isM3EShape(tag: String?): Boolean {
        if (tag.isNullOrBlank()) return false
        val clean = tag.trim().lowercase().removePrefix("m3e_")
        if (clean == "circle" || clean == "50" || clean == "square" || clean == "0") return false
        if (clean.toIntOrNull() != null) return false
        if (clean == "heart") return true
        return getShapePolygon(tag) != null
    }

    /**
     * Computes the exact Path representation of the shape scaled into [w] x [h] with optional stroke inset.
     */
    @JvmStatic
    @JvmOverloads
    fun getShapePath(tag: String?, width: Float, height: Float, strokeInset: Float = 0f): Path? {
        val availW = (width - strokeInset * 2f).coerceAtLeast(1f)
        val availH = (height - strokeInset * 2f).coerceAtLeast(1f)
        val clean = tag?.trim()?.lowercase()?.removePrefix("m3e_")

        if (clean == "heart") {
            val p = M3EShapes.heart(availW, availH)
            if (strokeInset > 0f) {
                p.offset(strokeInset, strokeInset)
            }
            return p
        }

        val polygon = getShapePolygon(tag)
        if (polygon != null) {
            val p = polygon.toPath()
            val bounds = RectF()
            p.computeBounds(bounds, true)
            if (bounds.width() > 0 && bounds.height() > 0) {
                val scaleX = availW / bounds.width()
                val scaleY = availH / bounds.height()
                val matrix = android.graphics.Matrix()
                matrix.postTranslate(-bounds.left, -bounds.top)
                matrix.postScale(scaleX, scaleY)
                if (strokeInset > 0f) {
                    matrix.postTranslate(strokeInset, strokeInset)
                }
                p.transform(matrix)
                return p
            }
        }
        return null
    }

    /**
     * Applies shape clipping to any View using OutlineProvider.
     * Uses setPath on API 33+ for M3E shapes, and setRoundRect on older APIs or for standard roundness.
     */
    @JvmStatic
    @JvmOverloads
    fun applyShapeToView(view: View?, tag: String?, radiusPercentFallback: Int = 50) {
        if (view == null) return
        val isM3E = isM3EShape(tag)

        if (isM3E && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            view.elevation = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                view.outlineAmbientShadowColor = Color.TRANSPARENT
                view.outlineSpotShadowColor = Color.TRANSPARENT
            }
            view.clipToOutline = true
            view.outlineProvider = object : ViewOutlineProvider() {
                private val cachedPath = Path()
                private var lastWidth = 0
                private var lastHeight = 0

                override fun getOutline(v: View, outline: Outline) {
                    if (v.width <= 0 || v.height <= 0) return
                    if (v.width != lastWidth || v.height != lastHeight) {
                        lastWidth = v.width
                        lastHeight = v.height

                        val path = getShapePath(tag, v.width.toFloat(), v.height.toFloat(), 0f)
                        if (path != null) {
                            cachedPath.rewind()
                            cachedPath.addPath(path)
                        }
                    }
                    if (!cachedPath.isEmpty) {
                        try {
                            outline.setPath(cachedPath)
                        } catch (_: Exception) {
                            val minSide = minOf(v.width, v.height)
                            outline.setRoundRect(0, 0, v.width, v.height, minSide / 2f)
                        }
                    }
                }
            }
            view.invalidateOutline()
            return
        }

        // Standard rounded rectangle / circle / square fallback
        val percent = getCornerRadiusPercent(tag, radiusPercentFallback)
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                val minSide = minOf(v.width, v.height)
                val r = if (percent >= 50) minSide / 2f else minSide * (percent / 100f)
                outline.setRoundRect(0, 0, v.width, v.height, r)
            }
        }
        view.invalidateOutline()
    }
}
