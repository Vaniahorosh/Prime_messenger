package com.messenger.prime

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.messenger.prime.databinding.ActivitySettingsLanguageBinding
import com.messenger.prime.shapes.M3EShapes

class SettingsLanguageActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsLanguageBinding.inflate(layoutInflater)
        setContentView(b.root)

        val currentCode = PrimeLocaleManager.getSavedLanguageCode(this)
        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutLanguageHeroStage,
            heroOverlay = b.layoutLanguageHeroOverlay,
            scrollView = b.scrollLanguageSettings,
            title = getString(R.string.settings_language_title),
            subtitle = PrimeLocaleManager.getLanguageDisplayName(this, currentCode),
            animatedCards = listOf(b.cardLangSystem, b.cardLangRussian, b.cardLangEnglish, b.cardLangInfo)
        )

        setupHeroBlurAndBadges()
        setupSegmentedCards()
        bindSelectionState(currentCode)

        b.cardLangSystem.setOnClickListener { selectLanguage(PrimeLocaleManager.LANG_SYSTEM) }
        b.cardLangRussian.setOnClickListener { selectLanguage(PrimeLocaleManager.LANG_RU) }
        b.cardLangEnglish.setOnClickListener { selectLanguage(PrimeLocaleManager.LANG_EN) }
    }

    private fun setupHeroBlurAndBadges() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val baseColor = ContextCompat.getColor(this, R.color.prime_base)

        SettingsSupport.applyM3EBadgeIcon(b.ivHeroLanguageIcon, accentColor, M3EShapes.cookie9Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivLangSystemBadge, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivLangRussianBadge, accentColor, M3EShapes.cookie6Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivLangEnglishBadge, accentColor, M3EShapes.circle, isDark)

        val overlayColor = if (isDark) "#730F172A".toColorInt() else "#99FFFFFF".toColorInt()
        val frameClear = window.decorView.background ?: baseColor.toDrawable()
        b.blurLanguageHeroCard.setupBlur(
            rootView = b.layoutLanguageHeroStage,
            blurRadius = 20f,
            tintColor = overlayColor,
            frameClearDrawable = frameClear
        )

        b.tvHeroLanguageBadgeCode.tag = "do_not_tint"
        b.tvHeroLanguageBadgeCode.backgroundTintList = ColorStateList.valueOf(accentColor)
        b.tvHeroLanguageBadgeCode.setTextColor(ColorAccentManager.getOnAccentColor(accentColor))
    }

    private fun setupSegmentedCards() {
        SettingsSupport.applySegmentedGroupShape(b.cardLangSystem, 0, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardLangRussian, 1, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardLangEnglish, 2, 3)

        b.cardLangSystem.addBounceTouchEffect()
        b.cardLangRussian.addBounceTouchEffect()
        b.cardLangEnglish.addBounceTouchEffect()
    }

    private fun bindSelectionState(code: String) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val density = resources.displayMetrics.density
        val surfaceColor = ContextCompat.getColor(this, R.color.prime_surface)
        val selectedSurface = ColorUtils.blendARGB(surfaceColor, accentColor, if (isDark) 0.14f else 0.06f)

        val items = listOf(
            Triple(PrimeLocaleManager.LANG_SYSTEM, b.cardLangSystem, b.rbLangSystem),
            Triple(PrimeLocaleManager.LANG_RU, b.cardLangRussian, b.rbLangRussian),
            Triple(PrimeLocaleManager.LANG_EN, b.cardLangEnglish, b.rbLangEnglish)
        )

        items.forEach { (itemCode, card, rb) ->
            val selected = itemCode == code
            rb.isChecked = selected
            rb.buttonTintList = ColorStateList.valueOf(accentColor)
            card.setCardBackgroundColor(if (selected) selectedSurface else surfaceColor)
            card.strokeWidth = if (selected) (1.5f * density).toInt() else 0
            card.strokeColor = accentColor
        }

        b.tvHeroLanguageBadgeCode.text = when (code) {
            PrimeLocaleManager.LANG_RU -> "RU"
            PrimeLocaleManager.LANG_EN -> "EN"
            else -> "AUTO"
        }
        b.tvHeroLanguageName.text = PrimeLocaleManager.getLanguageDisplayName(this, code)
        updateDetailSubtitle(b.root, PrimeLocaleManager.getLanguageDisplayName(this, code))
    }

    private fun selectLanguage(code: String) {
        val currentCode = PrimeLocaleManager.getSavedLanguageCode(this)
        try {
            b.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}

        if (currentCode == code) return
        PrimeLocaleManager.setAppLanguage(this, code)
        bindSelectionState(code)
    }
}
