package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object PrimeLocaleManager {

    private const val PREFS_NAME = "PrimeLocalDB"
    private const val KEY_APP_LANGUAGE = "app_language"

    const val ACTION_LANGUAGE_CHANGED = "com.messenger.prime.ACTION_LANGUAGE_CHANGED"

    const val LANG_SYSTEM = "system"
    const val LANG_RU = "ru"
    const val LANG_EN = "en"

    @JvmStatic
    fun getSavedLanguageCode(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (sp.getString(KEY_APP_LANGUAGE, LANG_SYSTEM)) {
            LANG_RU -> LANG_RU
            LANG_EN -> LANG_EN
            else -> LANG_SYSTEM
        }
    }

    @JvmStatic
    fun applySavedLocale(context: Context) {
        val code = getSavedLanguageCode(context)
        val target = toLocaleList(code)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }

    @JvmStatic
    fun setAppLanguage(context: Context, langCode: String) {
        val normalized = when (langCode) {
            LANG_RU -> LANG_RU
            LANG_EN -> LANG_EN
            else -> LANG_SYSTEM
        }
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_APP_LANGUAGE, normalized).apply()

        val target = toLocaleList(normalized)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }

        context.sendBroadcast(
            Intent(ACTION_LANGUAGE_CHANGED).setPackage(context.packageName)
        )
    }

    @JvmStatic
    fun getCurrentLanguageTag(context: Context): String {
        val saved = getSavedLanguageCode(context)
        val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val configTag = context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()
        return "$saved|$appTags|$configTag"
    }

    @JvmStatic
    fun getEffectiveLanguageCode(context: Context): String {
        val saved = getSavedLanguageCode(context)
        if (saved != LANG_SYSTEM) return saved
        val lang = Resources.getSystem().configuration.locales[0]?.language.orEmpty()
        return if (lang.startsWith("en", ignoreCase = true)) LANG_EN else LANG_RU
    }

    @JvmStatic
    fun getLocalizedContext(context: Context): Context {
        val lang = getEffectiveLanguageCode(context)
        val locale = Locale.forLanguageTag(lang)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    @JvmStatic
    fun getString(context: Context, @StringRes resId: Int, vararg formatArgs: Any): String {
        val localized = getLocalizedContext(context)
        return if (formatArgs.isEmpty()) {
            localized.getString(resId)
        } else {
            localized.getString(resId, *formatArgs)
        }
    }

    @JvmStatic
    fun getQuantityString(
        context: Context,
        @PluralsRes resId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String {
        val localized = getLocalizedContext(context)
        return if (formatArgs.isEmpty()) {
            localized.resources.getQuantityString(resId, quantity)
        } else {
            localized.resources.getQuantityString(resId, quantity, *formatArgs)
        }
    }

    @JvmStatic
    fun getLanguageDisplayName(context: Context, code: String? = getSavedLanguageCode(context)): String {
        return when (code) {
            LANG_RU -> context.getString(R.string.settings_lang_ru)
            LANG_EN -> context.getString(R.string.settings_lang_en)
            else -> context.getString(R.string.settings_lang_system)
        }
    }

    private fun toLocaleList(code: String): LocaleListCompat {
        return when (code) {
            LANG_RU -> LocaleListCompat.forLanguageTags("ru")
            LANG_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
    }
}
