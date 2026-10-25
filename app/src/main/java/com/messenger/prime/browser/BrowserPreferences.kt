package com.messenger.prime.browser

import android.content.Context
import android.content.SharedPreferences
import java.net.URLEncoder

object BrowserPreferences {
    private const val PREFS_NAME = "PrimeLocalDB"

    private const val KEY_USE_INTERNAL_BROWSER = "browser_use_internal"
    private const val KEY_SEARCH_ENGINE = "browser_search_engine"
    private const val KEY_ENABLE_JS = "browser_enable_js"
    private const val KEY_ENABLE_COOKIES = "browser_enable_cookies"
    private const val KEY_DESKTOP_MODE = "browser_desktop_mode"

    fun isInternalBrowserEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_USE_INTERNAL_BROWSER, true)
    }

    fun setInternalBrowserEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_INTERNAL_BROWSER, enabled).apply()
    }

    fun getSearchEngine(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google"
    }

    fun setSearchEngine(context: Context, engine: String) {
        getPrefs(context).edit().putString(KEY_SEARCH_ENGINE, engine).apply()
    }

    fun isJavaScriptEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_ENABLE_JS, true)
    }

    fun setJavaScriptEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_JS, enabled).apply()
    }

    fun areCookiesEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_ENABLE_COOKIES, true)
    }

    fun setCookiesEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_COOKIES, enabled).apply()
    }

    fun isDesktopModeEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_DESKTOP_MODE, false)
    }

    fun setDesktopModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply()
    }

    fun buildSearchOrUrl(context: Context, input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://") || trimmed.startsWith("about:")) {
            return trimmed
        }

        val isHostPattern = trimmed.contains(".") && !trimmed.contains(" ") && trimmed.indexOf(".") > 0 && trimmed.indexOf(".") < trimmed.length - 1
        if (isHostPattern) {
            return "https://$trimmed"
        }

        val encodedQuery = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed
        }

        return when (getSearchEngine(context)) {
            "Яндекс", "Yandex" -> "https://yandex.ru/search/?text=$encodedQuery"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encodedQuery"
            "Bing" -> "https://www.bing.com/search?q=$encodedQuery"
            else -> "https://www.google.com/search?q=$encodedQuery"
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
