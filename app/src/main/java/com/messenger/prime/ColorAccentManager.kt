package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.FloatingActionButton

object ColorAccentManager {

    const val ACCENT_TYPE_DEFAULT = "default"
    const val ACCENT_TYPE_SYSTEM = "system"
    const val ACCENT_TYPE_CUSTOM = "custom"

    val PRESET_COLORS = listOf(
        PresetColor("Фирменный", 0xFF154B87.toInt(), 0xFF4D9FFF.toInt(), 0),
        PresetColor("Изумрудный", 0xFF00897B.toInt(), 0xFF26A69A.toInt(), R.style.ThemeOverlay_Prime_Emerald),
        PresetColor("Фиолетовый", 0xFF7C4DFF.toInt(), 0xFFB388FF.toInt(), R.style.ThemeOverlay_Prime_Purple),
        PresetColor("Янтарный", 0xFFFB8C00.toInt(), 0xFFFFB74D.toInt(), R.style.ThemeOverlay_Prime_Amber),
        PresetColor("Рубиновый", 0xFFE53935.toInt(), 0xFFFF5252.toInt(), R.style.ThemeOverlay_Prime_Ruby),
        PresetColor("Голубой", 0xFF00ACC1.toInt(), 0xFF4DD0E1.toInt(), R.style.ThemeOverlay_Prime_Cyan),
        PresetColor("Розовый", 0xFFE91E63.toInt(), 0xFFFF4081.toInt(), R.style.ThemeOverlay_Prime_Pink)
    )

    data class PresetColor(
        val name: String,
        @ColorInt val lightColor: Int,
        @ColorInt val darkColor: Int,
        val overlayRes: Int
    )

    @JvmStatic
    fun getAccentType(context: Context): String {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        return sp.getString("accent_type", ACCENT_TYPE_DEFAULT) ?: ACCENT_TYPE_DEFAULT
    }

    @JvmStatic
    fun getCurrentAccentColor(context: Context): Int {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val type = sp.getString("accent_type", ACCENT_TYPE_DEFAULT) ?: ACCENT_TYPE_DEFAULT

        return when (type) {
            ACCENT_TYPE_SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        context.getColor(android.R.color.system_accent1_600)
                    } catch (_: Exception) {
                        getDefaultColor(context)
                    }
                } else {
                    getDefaultColor(context)
                }
            }
            ACCENT_TYPE_CUSTOM -> {
                sp.getInt("accent_custom_color", getDefaultColor(context))
            }
            else -> getDefaultColor(context)
        }
    }

    @JvmStatic
    fun getDefaultColor(context: Context): Int {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (isDark) Color.parseColor("#4D9FFF") else Color.parseColor("#154B87")
    }

    @JvmStatic
    fun getThemeOverlayRes(context: Context): Int {
        val currentColor = getCurrentAccentColor(context)
        val matchedPreset = PRESET_COLORS.find { it.lightColor == currentColor || it.darkColor == currentColor }
        return matchedPreset?.overlayRes ?: 0
    }

    @JvmStatic
    fun getAccentSummary(context: Context): String {
        val type = getAccentType(context)
        if (type == ACCENT_TYPE_SYSTEM) return "Системный (Monet)"
        if (type == ACCENT_TYPE_DEFAULT) return "Фирменный"

        val currentColor = getCurrentAccentColor(context)
        val matchedPreset = PRESET_COLORS.find { it.lightColor == currentColor || it.darkColor == currentColor }
        if (matchedPreset != null) {
            return matchedPreset.name
        }

        return String.format("#%06X", (0xFFFFFF and currentColor))
    }

    @JvmStatic
    fun setAccentDefault(context: Context) {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sp.edit()
            .putString("accent_type", ACCENT_TYPE_DEFAULT)
            .remove("accent_custom_color")
            .apply()
    }

    @JvmStatic
    fun setAccentSystem(context: Context) {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sp.edit()
            .putString("accent_type", ACCENT_TYPE_SYSTEM)
            .apply()
    }

    @JvmStatic
    fun getAvatarColor(name: String): Int {
        val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
        val hash = name.hashCode()
        val index = (if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)) % colors.size
        return Color.parseColor(colors[index])
    }

    @JvmStatic
    fun setAccentCustom(context: Context, colorInt: Int) {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sp.edit()
            .putString("accent_type", ACCENT_TYPE_CUSTOM)
            .putInt("accent_custom_color", colorInt)
            .apply()
    }

    @JvmStatic
    fun applyAccentToActivity(activity: Activity) {
        if (getAccentType(activity) == ACCENT_TYPE_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(activity)
        } else {
            val overlayRes = getThemeOverlayRes(activity)
            if (overlayRes != 0) {
                activity.theme.applyStyle(overlayRes, true)
            }
        }
    }

    @JvmStatic
    fun tintTextViews(context: Context, vararg textViews: TextView?) {
        val accentColor = getCurrentAccentColor(context)
        for (tv in textViews) {
            tv?.setTextColor(accentColor)
        }
    }

    @JvmStatic
    fun tintViews(context: Context, vararg views: View?) {
        val accentColor = getCurrentAccentColor(context)
        val colorStateList = ColorStateList.valueOf(accentColor)

        for (v in views) {
            if (v == null) continue
            when (v) {
                is TextView -> v.setTextColor(accentColor)
                is FloatingActionButton -> v.backgroundTintList = colorStateList
                is MaterialButton -> {
                    if (v.icon != null && v.text.isNullOrEmpty()) {
                        v.iconTint = colorStateList
                    } else {
                        v.backgroundTintList = colorStateList
                    }
                }
                is ImageView -> v.imageTintList = colorStateList
                is ProgressBar -> {
                    v.indeterminateTintList = colorStateList
                    v.progressTintList = colorStateList
                }
            }
        }
    }

    @JvmStatic
    fun tintViewTree(view: View?, accentColor: Int) {
        if (view == null) return
        val colorStateList = ColorStateList.valueOf(accentColor)

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                tintViewTree(view.getChildAt(i), accentColor)
            }
        }

        when (view) {
            is FloatingActionButton -> {
                view.backgroundTintList = colorStateList
            }
            is MaterialButton -> {
                if (view.icon != null && view.text.isNullOrEmpty()) {
                    view.iconTint = colorStateList
                } else if (view.backgroundTintList != null && isBrandColor(view.backgroundTintList!!.defaultColor)) {
                    view.backgroundTintList = colorStateList
                }
            }
            is ImageView -> {
                val tint = view.imageTintList
                if (tint != null && isBrandColor(tint.defaultColor)) {
                    view.imageTintList = colorStateList
                }
            }
            is ProgressBar -> {
                view.indeterminateTintList = colorStateList
                view.progressTintList = colorStateList
            }
            is TextView -> {
                val currentTextColors = view.textColors
                if (currentTextColors != null) {
                    val defaultColor = currentTextColors.defaultColor
                    if (isBrandColor(defaultColor)) {
                        view.setTextColor(accentColor)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun isBrandColor(colorInt: Int): Boolean {
        if (colorInt == 0) return false
        val isBrandHex = colorInt == Color.parseColor("#154B87") ||
                        colorInt == Color.parseColor("#4D9FFF") ||
                        colorInt == Color.parseColor("#CC154B87") ||
                        colorInt == Color.parseColor("#CC4D9FFF") ||
                        colorInt == Color.parseColor("#154A86") ||
                        colorInt == Color.parseColor("#007BFF") ||
                        colorInt == Color.parseColor("#38BDF8")
        if (isBrandHex) return true

        return PRESET_COLORS.any { it.lightColor == colorInt || it.darkColor == colorInt }
    }

    @JvmStatic
    fun getBubbleCornerRadiusPx(context: Context): Float {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val percent = sp.getInt("settings_bubble_radius_percent", 50)
        val density = context.resources.displayMetrics.density
        val minDp = 4f
        val maxDp = 28f
        val dp = minDp + (maxDp - minDp) * (percent / 100f)
        return dp * density
    }

    @JvmStatic
    fun createIncomingBubbleDrawable(context: Context): GradientDrawable {
        val radiusPx = getBubbleCornerRadiusPx(context)
        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val density = context.resources.displayMetrics.density
        val tailRadius = 4 * density
        val radii = floatArrayOf(
            radiusPx, radiusPx,
            radiusPx, radiusPx,
            radiusPx, radiusPx,
            tailRadius, tailRadius
        )

        val bgCol = if (isDark) Color.parseColor("#263343") else Color.WHITE
        val strokeCol = if (isDark) Color.parseColor("#3A4A5B") else Color.parseColor("#E2E8F0")

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(bgCol)
            setStroke((0.5f * density).toInt(), strokeCol)
        }
    }

    @JvmStatic
    fun createOutgoingBubbleDrawable(context: Context): GradientDrawable {
        val radiusPx = getBubbleCornerRadiusPx(context)
        val accentColor = getCurrentAccentColor(context)
        val hsv = FloatArray(3)
        Color.colorToHSV(accentColor, hsv)
        hsv[2] = (hsv[2] * 1.15f).coerceAtMost(1.0f)

        val density = context.resources.displayMetrics.density
        val tailRadius = 4 * density
        val radii = floatArrayOf(
            radiusPx, radiusPx,
            radiusPx, radiusPx,
            tailRadius, tailRadius,
            radiusPx, radiusPx
        )

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.HSVToColor(hsv), accentColor)
        ).apply {
            cornerRadii = radii
        }
    }
}
