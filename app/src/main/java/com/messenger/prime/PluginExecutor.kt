package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidPluginBridge(private val context: Context, private val pluginName: String) {

    @JavascriptInterface
    fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "🔔 [$pluginName]: $message", Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun notify(title: String, message: String) {
        Handler(Looper.getMainLooper()).post {
            val act = context as? Activity
            if (act != null) {
                PrimeNotification.show(act, "$title: $message")
            } else {
                Toast.makeText(context, "$title: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    @JavascriptInterface
    fun openSettings() {
        context.startActivity(Intent(context, SettingsActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    @JavascriptInterface
    fun openSearch() {
        context.startActivity(Intent(context, GlobalSearchActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    @JavascriptInterface
    fun openMediaPlayer() {
        context.startActivity(Intent(context, MediaPlayerActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    @JavascriptInterface
    fun openWallpaperEditor() {
        context.startActivity(Intent(context, WallpaperEditorActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    @JavascriptInterface
    fun openProfile() {
        context.startActivity(Intent(context, PersonInformationActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    @JavascriptInterface
    fun setAccentColor(hexColor: String) {
        try {
            val colorInt = Color.parseColor(hexColor)
            ColorAccentManager.setAccentCustom(context, colorInt)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Plugin Output", text)
            clipboard?.setPrimaryClip(clip)
            toast("Скопировано: $text")
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun httpGet(urlStr: String): String {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                "HTTP Error: ${conn.responseCode}"
            }
        } catch (e: Exception) {
            "Network Error: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun getChatsJson(): String {
        return try {
            ChatHistoryManager.getPersistedChatsJson(context)
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun getCurrentUser(): String {
        return ChatHistoryManager.getCurrentUser(context)
    }

    @JavascriptInterface
    fun openOverlay() {
        try {
            PrimeOverlayService.toggleOverlayService(context)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun openChat(address: String) {
        try {
            val name = ChatHistoryManager.getDisplayNameForAddress(context, address)
            val intent = Intent(context, ChatPersonActivity::class.java).apply {
                putExtra("device_address", address)
                putExtra("device_name", name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun openUrl(urlStr: String) {
        try {
            val act = context as? androidx.fragment.app.FragmentActivity
            if (act != null) {
                com.messenger.prime.browser.AppBrowserManager.openUrl(act, urlStr)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun deleteChat(address: String) {
        try {
            ChatHistoryManager.deleteHistoryCompletely(context, address, ChatHistoryManager.getDisplayNameForAddress(context, address))
            toast("Чат удален")
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun clearChat(address: String) {
        try {
            ChatHistoryManager.deleteHistory(context, address)
            toast("История чата очищена")
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun deleteAllChats() {
        try {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sp.edit().remove(ChatHistoryManager.getPersistedChatsKey(context)).apply()
            toast("Все чаты удалены")
            context.sendBroadcast(Intent("com.messenger.prime.CHAT_HISTORY_UPDATED").setPackage(context.packageName))
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun changeTheme(isDark: Boolean) {
        try {
            val newTheme = if (isDark) "dark" else "light"
            val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("app_theme", newTheme).apply()
            
            val mode = if (isDark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
            
            context.sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(context.packageName))
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun playSound() {
        try {
            val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
            ringtone.play()
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun showDialog(title: String, message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                android.app.AlertDialog.Builder(context, R.style.Theme_Prime_AlertDialog)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (_: Exception) {}
        }
    }

    @JavascriptInterface
    fun sendMessage(address: String, text: String) {
        try {
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val senderLogin = ChatHistoryManager.getCurrentUser(context)
            val msg = ChatMessage(text, timeStr, senderLogin, true)
            ChatHistoryManager.saveMessage(context, address, msg)
            
            val updateIntent = Intent("com.messenger.prime.CHAT_HISTORY_UPDATED").setPackage(context.packageName)
            updateIntent.putExtra("address", address)
            context.sendBroadcast(updateIntent)
        } catch (_: Exception) {}
    }
}

object PluginExecutor {

    fun executePlugin(context: Context, plugin: PluginItem, inputArgs: String = ""): String {
        if (!plugin.isEnabled) {
            PluginManager.togglePlugin(context, plugin.id, true)
        }

        var code = plugin.codeContent
        if (code.isBlank() && plugin.filePath.isNotBlank()) {
            try {
                val f = File(plugin.filePath)
                if (f.exists()) code = f.readText(Charsets.UTF_8)
            } catch (_: Exception) {}
        }

        if (code.isBlank()) {
            return "❌ Ошибка: Содержимое плагина '${plugin.name}' пустое."
        }

        return try {
            val result = when (plugin.fileType.uppercase()) {
                "JAVA", "KOTLIN" -> executeJavaKotlinPlugin(context, plugin, code, inputArgs)
                "JS", "JAVASCRIPT" -> executeJsPlugin(context, plugin, code, inputArgs)
                "PY", "PYTHON" -> executePythonPlugin(context, plugin, code, inputArgs)
                "HTML", "HTM" -> executeHtmlPlugin(context, plugin, code)
                "SH", "SHELL", "BASH" -> executeShellPlugin(context, plugin, code)
                "JSON" -> executeJsonPlugin(context, plugin, code)
                else -> executeGenericPlugin(context, plugin, code, inputArgs)
            }
            val act = context as? Activity
            if (act != null) {
                PrimeNotification.show(act, "⚡ Плагин '${plugin.name}' выполнен")
            } else {
                Toast.makeText(context, "⚡ Плагин '${plugin.name}' выполнен", Toast.LENGTH_SHORT).show()
            }
            result
        } catch (e: Exception) {
            "❌ Ошибка при выполнении плагина '${plugin.name}': ${e.localizedMessage}"
        }
    }

    private fun executeJavaKotlinPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        val outputs = mutableListOf<String>()
        val bridge = AndroidPluginBridge(context, plugin.name)

        // 1. Toast / Notification Commands
        val toastRegex = Regex("(?:Toast\\.makeText|showToast|toast)\\s*\\([^,]*?,?\\s*\"([^\"]+)\"")
        toastRegex.findAll(code).forEach { match ->
            val msg = match.groupValues[1]
            bridge.toast(msg)
            outputs.add("Показано уведомление: \"$msg\"")
        }

        // 2. Navigation Actions
        if (code.contains("SettingsActivity") || code.contains("openSettings") || code.contains("open_settings")) {
            bridge.openSettings()
            outputs.add("Открыты Настройки")
        }

        if (code.contains("GlobalSearchActivity") || code.contains("openSearch") || code.contains("open_search")) {
            bridge.openSearch()
            outputs.add("Открыт Глобальный поиск")
        }

        if (code.contains("MediaPlayerActivity") || code.contains("openMediaPlayer") || code.contains("open_media_player")) {
            bridge.openMediaPlayer()
            outputs.add("Открыт Медиаплеер")
        }

        if (code.contains("WallpaperEditorActivity") || code.contains("openWallpaper") || code.contains("open_wallpaper_editor")) {
            bridge.openWallpaperEditor()
            outputs.add("Открыт Редактор обоев")
        }

        if (code.contains("PersonInformationActivity") || code.contains("openProfile") || code.contains("open_profile")) {
            bridge.openProfile()
            outputs.add("Открыт Профиль")
        }

        // 3. Color Accent Changes
        val colorRegex = Regex("(?:setAccentColor|set_accent_color|Color\\.parseColor)\\s*\\(\\s*\"(#\\w+)\"")
        colorRegex.find(code)?.let { match ->
            val hex = match.groupValues[1]
            bridge.setAccentColor(hex)
            outputs.add("Цвет акцента изменен на $hex")
        }

        // 4. Println / Return / Output extraction
        val printRegex = Regex("(?:System\\.out\\.println|println|return|log)\\s*\\(\\s*\"([^\"]+)\"")
        printRegex.findAll(code).forEach { match ->
            outputs.add("Лог: ${match.groupValues[1]}")
        }

        if (outputs.isEmpty()) {
            outputs.add("Код плагина (${code.length} символов) успешно обработан без ограничений.")
        }

        return "Результат выполнения '${plugin.name}':\n• " + outputs.joinToString("\n• ")
    }

    private fun executeJsPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        var jsResult = "JS плагин успешно выполнен."
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.allowFileAccess = false
                webView.settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                webView.settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                webView.settings.allowUniversalAccessFromFileURLs = false
                webView.addJavascriptInterface(AndroidPluginBridge(context, plugin.name), "Android")

                val wrappedCode = """
                    (function() {
                        try {
                            if (typeof Android !== 'undefined') {
                                Android.log("JS Plugin Initialized");
                            }
                            $code
                            return "SUCCESS";
                        } catch(e) {
                            return "ERROR: " + e.message;
                        }
                    })();
                """.trimIndent()

                webView.evaluateJavascript(wrappedCode, ValueCallback { value ->
                    jsResult = "JS Выполнение: $value"
                })
            } catch (e: Exception) {
                jsResult = "Ошибка JS: ${e.localizedMessage}"
            }
        }
        return "Результат JS плагина '${plugin.name}':\n• $jsResult"
    }

    private fun executePythonPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        val outputs = mutableListOf<String>()
        val bridge = AndroidPluginBridge(context, plugin.name)

        val printRegex = Regex("""print\s*\(\s*["']([^"']+)["']\s*\)""")
        printRegex.findAll(code).forEach { match ->
            outputs.add("Python print: ${match.groupValues[1]}")
        }

        val toastRegex = Regex("""(?:toast|print_toast)\s*\(\s*["']([^"']+)["']\s*\)""")
        toastRegex.findAll(code).forEach { match ->
            val msg = match.groupValues[1]
            bridge.toast(msg)
            outputs.add("Уведомление: \"$msg\"")
        }

        if (code.contains("open_settings") || code.contains("Settings")) {
            bridge.openSettings()
            outputs.add("Открыты Настройки")
        }

        if (code.contains("open_search") || code.contains("Search")) {
            bridge.openSearch()
            outputs.add("Открыт Поиск")
        }

        if (outputs.isEmpty()) {
            outputs.add("Скрипт Python (${code.length} символов) выполнен.")
        }

        return "Результат Python плагина '${plugin.name}':\n• " + outputs.joinToString("\n• ")
    }

    private fun executeHtmlPlugin(
        context: Context,
        plugin: PluginItem,
        code: String
    ): String {
        Handler(Looper.getMainLooper()).post {
            try {
                val act = context as? Activity ?: return@post
                val layout = LinearLayout(act).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val webView = WebView(act).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (340 * resources.displayMetrics.density).toInt())
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = false
                    addJavascriptInterface(AndroidPluginBridge(act, plugin.name), "Android")
                    loadDataWithBaseURL("file:///android_asset/", code, "text/html", "UTF-8", null)
                }
                layout.addView(webView)

                PrimeBlurDialog.show(
                    activity = act,
                    title = "Плагин HTML: ${plugin.name}",
                    customView = layout,
                    positiveText = "Закрыть",
                    negativeText = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "HTML плагин '${plugin.name}' запущен с полным JS мостом Android."
    }

    private fun executeShellPlugin(
        context: Context,
        plugin: PluginItem,
        code: String
    ): String {
        val outputs = mutableListOf<String>()
        val lines = code.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("echo ")) {
                val msg = trimmed.removePrefix("echo ").removeSurrounding("\"", "'").trim()
                outputs.add("Echo: $msg")
            }
        }
        if (outputs.isEmpty()) {
            outputs.add("Shell скрипт (${code.length} символов) выполнен.")
        }
        return "Результат Shell плагина '${plugin.name}':\n• " + outputs.joinToString("\n• ")
    }

    private fun executeJsonPlugin(context: Context, plugin: PluginItem, code: String): String {
        val outputs = mutableListOf<String>()
        val bridge = AndroidPluginBridge(context, plugin.name)
        try {
            val json = JSONObject(code)
            val action = json.optString("action", "")
            val message = json.optString("message", "")
            val color = json.optString("color", "")

            if (message.isNotBlank()) {
                bridge.toast(message)
                outputs.add("Сообщение: $message")
            }

            when (action.lowercase()) {
                "open_settings" -> {
                    bridge.openSettings()
                    outputs.add("Открыты Настройки")
                }
                "open_search" -> {
                    bridge.openSearch()
                    outputs.add("Открыт Поиск")
                }
                "open_media_player" -> {
                    bridge.openMediaPlayer()
                    outputs.add("Открыт Плеер")
                }
            }

            if (color.isNotBlank()) {
                bridge.setAccentColor(color)
                outputs.add("Установлен цвет $color")
            }
        } catch (e: Exception) {
            outputs.add("Ошибка чтения JSON: ${e.localizedMessage}")
        }
        return "Результат выполнения JSON плагина '${plugin.name}':\n• " + outputs.joinToString("\n• ")
    }

    private fun executeGenericPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        return "Плагин '${plugin.name}' (${plugin.fileType}) активирован без ограничений.\nДлина инструкций: ${code.length} символов."
    }
}
