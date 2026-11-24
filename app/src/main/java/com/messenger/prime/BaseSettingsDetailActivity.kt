package com.messenger.prime

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurView

abstract class BaseSettingsDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
    }

    protected fun setupDetailHeroChrome(
        rootView: ViewGroup,
        heroStage: View,
        heroOverlay: View? = null,
        scrollView: NestedScrollView,
        title: String,
        subtitle: String? = null,
        heroHeightFraction: Float = 0.30f,
        heroMinDp: Int = 212,
        heroMaxDp: Int = 276,
        imeHeroHeightFraction: Float = 0.19f,
        imeHeroMinDp: Int = 152,
        imeHeroMaxDp: Int = 196,
        applyDefaultBackdrop: Boolean = true,
        animatedCards: List<View> = emptyList()
    ) {
        val isDark = SettingsSupport.isDarkThemeActive(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge(isDarkIcons = !isDark)

        val baseColor = ContextCompat.getColor(this, R.color.prime_base)
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        rootView.setBackgroundColor(baseColor)

        if (applyDefaultBackdrop) {
            applyHeroBackdrop(heroStage, accentColor, isDark, baseColor)
        }

        val toolbarRow = rootView.findViewById<View>(R.id.layoutDetailToolbarRow)
        val tvTitle = rootView.findViewById<TextView>(R.id.tvDetailTitle)
        val tvSubtitle = rootView.findViewById<TextView>(R.id.tvDetailSubtitle)
        val btnBack = rootView.findViewById<MaterialButton>(R.id.btnDetailBack)
        val blurBack = rootView.findViewById<BlurView>(R.id.blurDetailBack)
        val blurToolbar = rootView.findViewById<BlurView>(R.id.blurDetailToolbar)

        tvTitle?.text = title
        if (!subtitle.isNullOrBlank()) {
            tvSubtitle?.text = subtitle
            tvSubtitle?.visibility = View.VISIBLE
        } else {
            tvSubtitle?.visibility = View.GONE
        }

        btnBack?.let { backBtn ->
            backBtn.tag = "do_not_tint"
            backBtn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            backBtn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.prime_text_primary))
            backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        applyToolbarBlur(blurBack, rootView, isDark, baseColor)
        applyToolbarBlur(blurToolbar, rootView, isDark, baseColor)

        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels
        val defaultStageHeight = (screenHeight * heroHeightFraction).toInt()
            .coerceIn((heroMinDp * density).toInt(), (heroMaxDp * density).toInt())
        val imeStageHeight = (screenHeight * imeHeroHeightFraction).toInt()
            .coerceIn((imeHeroMinDp * density).toInt(), (imeHeroMaxDp * density).toInt())

        val initialTopMargin = getFallbackStatusBarHeight() + (8 * density).toInt()
        val marginTarget = toolbarRow ?: blurToolbar
        marginTarget?.let { target ->
            (target.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                lp.topMargin = initialTopMargin
                target.layoutParams = lp
            }
        }
        heroStage.layoutParams?.let { lp ->
            lp.height = defaultStageHeight
            heroStage.layoutParams = lp
        }

        var latestTopMargin = initialTopMargin
        val syncHeroOverlayPadding = {
            if (heroOverlay != null) {
                val toolbarHeight = marginTarget?.height?.takeIf { it > 0 } ?: (52 * density).toInt()
                val topPad = latestTopMargin + toolbarHeight + (8 * density).toInt()
                val bottomPad = (28 * density).toInt()
                if (heroOverlay.paddingTop != topPad || heroOverlay.paddingBottom != bottomPad) {
                    heroOverlay.updatePadding(top = topPad, bottom = bottomPad)
                }
            }
        }

        syncHeroOverlayPadding()
        scrollView.updatePadding(top = (16 * density).toInt(), bottom = (28 * density).toInt())

        marginTarget?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            syncHeroOverlayPadding()
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            rootView.setPadding(cutout.left, 0, cutout.right, 0)

            val safeTop = maxOf(systemBars.top, cutout.top).takeIf { it > 0 } ?: getFallbackStatusBarHeight()
            latestTopMargin = safeTop + (8 * density).toInt()

            marginTarget?.let { target ->
                (target.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    if (lp.topMargin != latestTopMargin) {
                        lp.topMargin = latestTopMargin
                        target.layoutParams = lp
                    }
                }
            }

            val desiredStageHeight = if (imeVisible) imeStageHeight else defaultStageHeight
            heroStage.layoutParams?.let { lp ->
                if (lp.height != desiredStageHeight) {
                    lp.height = desiredStageHeight
                    heroStage.layoutParams = lp
                }
            }

            val bottomInset = maxOf(systemBars.bottom, cutout.bottom, ime.bottom)
            scrollView.updatePadding(
                top = (16 * density).toInt(),
                bottom = bottomInset + (28 * density).toInt()
            )
            syncHeroOverlayPadding()
            insets
        }
        ViewCompat.requestApplyInsets(rootView)

        rootView.post {
            PrimeTransitions.protectHorizontalScrollViews(rootView, null)
        }

        if (animatedCards.isNotEmpty()) {
            SettingsSupport.animateStaggeredEntrance(this, animatedCards)
        }
    }

    protected fun applyHeroBackdrop(
        heroStage: View,
        accentColor: Int = ColorAccentManager.getCurrentAccentColor(this),
        isDark: Boolean = SettingsSupport.isDarkThemeActive(this),
        baseColor: Int = ContextCompat.getColor(this, R.color.prime_base)
    ) {
        val ratio = if (isDark) 0.16f else 0.09f
        heroStage.setBackgroundColor(ColorUtils.blendARGB(baseColor, accentColor, ratio))
    }

    private fun applyToolbarBlur(blurView: BlurView?, rootView: ViewGroup, isDark: Boolean, baseColor: Int) {
        blurView ?: return
        val overlayColor = if (isDark) {
            "#730F172A".toColorInt()
        } else {
            "#99FFFFFF".toColorInt()
        }
        val frameClear = window.decorView.background ?: baseColor.toDrawable()
        blurView.setupBlur(
            rootView = rootView,
            blurRadius = 20f,
            tintColor = overlayColor,
            frameClearDrawable = frameClear
        )
    }

    protected fun configureDetailAction(
        rootView: ViewGroup,
        @DrawableRes iconRes: Int,
        contentDescription: String,
        onClick: () -> Unit
    ) {
        val blurAction = rootView.findViewById<BlurView>(R.id.blurDetailAction)
        val btnAction = rootView.findViewById<MaterialButton>(R.id.btnDetailAction) ?: return
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val baseColor = ContextCompat.getColor(this, R.color.prime_base)
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        blurAction?.visibility = View.VISIBLE
        applyToolbarBlur(blurAction, rootView, isDark, baseColor)

        btnAction.tag = "do_not_tint"
        btnAction.visibility = View.VISIBLE
        btnAction.contentDescription = contentDescription
        btnAction.setIconResource(iconRes)
        btnAction.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        btnAction.iconTint = ColorStateList.valueOf(accentColor)
        btnAction.setOnClickListener { onClick() }
    }

    protected fun updateDetailSubtitle(rootView: ViewGroup, subtitle: String?) {
        val tvSubtitle = rootView.findViewById<TextView>(R.id.tvDetailSubtitle) ?: return
        if (!subtitle.isNullOrBlank()) {
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = View.VISIBLE
        } else {
            tvSubtitle.visibility = View.GONE
        }
    }

    private fun getFallbackStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (32 * resources.displayMetrics.density).toInt()
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }
}
