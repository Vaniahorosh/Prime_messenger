package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import kotlin.math.min

/**
 * Window-metrics driven responsive layout helper for Settings hub and detail screens.
 * Uses real window bounds + cutouts rather than smallestScreenWidthDp so multi-window,
 * foldables, tablets, desktop windows, landscape phones, and ultra-narrow screens adapt dynamically.
 */
object SettingsAdaptiveHelper {

    data class WindowProfile(
        val windowWidthPx: Int,
        val windowHeightPx: Int,
        val safeTopPx: Int,
        val safeBottomPx: Int,
        val cutoutLeftPx: Int,
        val cutoutRightPx: Int,
        val density: Float,
        val imeBottomPx: Int,
        val imeVisible: Boolean
    ) {
        val safeWidthPx: Int
            get() = (windowWidthPx - cutoutLeftPx - cutoutRightPx).coerceAtLeast(1)

        val safeHeightPx: Int
            get() = (windowHeightPx - safeTopPx - safeBottomPx).coerceAtLeast(1)

        val widthDp: Float
            get() = windowWidthPx / density

        val heightDp: Float
            get() = windowHeightPx / density

        val safeWidthDp: Float
            get() = safeWidthPx / density

        val safeHeightDp: Float
            get() = safeHeightPx / density

        val isNarrowWidth: Boolean
            get() = safeWidthDp < 380f

        val isCompactHeight: Boolean
            get() = heightDp < 540f

        val isVeryShortHeight: Boolean
            get() = heightDp < 440f

        val isTwoPaneHub: Boolean
            get() = safeWidthDp >= 740f && safeHeightDp >= 300f

        val isTwoPaneDetail: Boolean
            get() = safeWidthDp >= 680f && (safeWidthDp >= heightDp * 1.10f || safeWidthDp >= 840f)
    }

    fun computeWindowProfile(
        activity: Activity,
        safeTopPx: Int,
        safeBottomPx: Int,
        cutoutLeftPx: Int,
        cutoutRightPx: Int,
        imeBottomPx: Int,
        imeVisible: Boolean
    ): WindowProfile {
        val bounds = try {
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
        } catch (_: Exception) {
            null
        }
        val dm = activity.resources.displayMetrics
        val winW = bounds?.width()?.takeIf { it > 0 } ?: dm.widthPixels
        val winH = bounds?.height()?.takeIf { it > 0 } ?: dm.heightPixels
        val density = dm.density.coerceAtLeast(0.75f)
        return WindowProfile(
            windowWidthPx = winW,
            windowHeightPx = winH,
            safeTopPx = safeTopPx,
            safeBottomPx = safeBottomPx,
            cutoutLeftPx = cutoutLeftPx,
            cutoutRightPx = cutoutRightPx,
            density = density,
            imeBottomPx = imeBottomPx,
            imeVisible = imeVisible
        )
    }

    fun computeTwoPaneLeftWidthPx(profile: WindowProfile, minDp: Int = 268, maxDp: Int = 420): Int {
        val d = profile.density
        return (profile.safeWidthPx * 0.38f).toInt()
            .coerceIn((minDp * d).toInt(), (maxDp * d).toInt())
    }

    fun computeSinglePaneHeroHeightPx(
        profile: WindowProfile,
        heroHeightFraction: Float,
        heroMinDp: Int,
        heroMaxDp: Int,
        imeHeroHeightFraction: Float,
        imeHeroMinDp: Int,
        imeHeroMaxDp: Int
    ): Int {
        val d = profile.density
        if (profile.imeVisible) {
            val imeAvailPx = (profile.windowHeightPx - profile.imeBottomPx)
                .coerceAtLeast((220 * d).toInt())
            val imeMinPx = minOf(
                (imeHeroMinDp * d).toInt(),
                (imeAvailPx * 0.34f).toInt().coerceAtLeast((108 * d).toInt())
            )
            val imeMaxPx = minOf(
                (imeHeroMaxDp * d).toInt(),
                (imeAvailPx * 0.40f).toInt().coerceAtLeast(imeMinPx)
            )
            return (imeAvailPx * imeHeroHeightFraction).toInt().coerceIn(imeMinPx, imeMaxPx)
        }

        val availH = profile.safeHeightPx
        val adaptiveMinPx = minOf(
            (heroMinDp * d).toInt(),
            (availH * 0.36f).toInt().coerceAtLeast((136 * d).toInt())
        )
        val adaptiveMaxPx = minOf(
            (heroMaxDp * d).toInt(),
            (availH * 0.44f).toInt().coerceAtLeast(adaptiveMinPx)
        )
        return (profile.windowHeightPx * heroHeightFraction).toInt()
            .coerceIn(adaptiveMinPx, adaptiveMaxPx)
    }

    fun createTwoPaneSheetBackground(context: Context, cornerRadiusDp: Float = 28f): GradientDrawable {
        val radiusPx = cornerRadiusDp * context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(context, R.color.prime_base))
            cornerRadii = floatArrayOf(
                radiusPx, radiusPx,
                0f, 0f,
                0f, 0f,
                radiusPx, radiusPx
            )
        }
    }

    /**
     * Scales centered LinearLayout hero overlay content purely at render layer (pivot + scaleX/scaleY)
     * when vertical or horizontal space is tight, preventing overlap with toolbar or sheet without
     * triggering extra layout passes.
     */
    fun fitLinearHeroOverlay(overlay: View?) {
        val linear = overlay as? LinearLayout ?: return
        val totalW = linear.width
        val totalH = linear.height
        if (totalW <= 0 || totalH <= 0) return

        val availW = (totalW - linear.paddingLeft - linear.paddingRight).coerceAtLeast(1)
        val availH = (totalH - linear.paddingTop - linear.paddingBottom).coerceAtLeast(1)

        var contentH = 0
        var maxChildW = 0
        for (i in 0 until linear.childCount) {
            val child = linear.getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams as? ViewGroup.MarginLayoutParams
            val ch = child.measuredHeight + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)
            val cw = child.measuredWidth + (lp?.leftMargin ?: 0) + (lp?.rightMargin ?: 0)
            contentH += ch
            if (cw > maxChildW) maxChildW = cw
        }

        if (contentH <= 0 || maxChildW <= 0) return

        val scaleH = if (contentH > availH) availH.toFloat() / contentH.toFloat() else 1f
        val scaleW = if (maxChildW > availW) availW.toFloat() / maxChildW.toFloat() else 1f
        val targetScale = min(1f, min(scaleH, scaleW)).coerceIn(0.68f, 1f)

        val pivotX = linear.paddingLeft + availW / 2f
        val pivotY = linear.paddingTop + availH / 2f

        if (linear.pivotX != pivotX) linear.pivotX = pivotX
        if (linear.pivotY != pivotY) linear.pivotY = pivotY
        if (linear.scaleX != targetScale) linear.scaleX = targetScale
        if (linear.scaleY != targetScale) linear.scaleY = targetScale
    }
}
