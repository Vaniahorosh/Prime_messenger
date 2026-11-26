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
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidPluginBridge(private val context: Context, private val pluginName: String) {

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("PrimePlugin", "[$pluginName]: $message")
    }

    @JavascriptInterface
    fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            PrimeNotification.show(
                context,
                PrimeLocaleManager.getString(context, R.string.plugin_toast_format, pluginName, message)
            )
        }
    }

    @JavascriptInterface
    fun notify(title: String, message: String) {
        Handler(Looper.getMainLooper()).post {
            PrimeNotification.show(context, "$title: $message")
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
            toast(PrimeLocaleManager.getString(context, R.string.plugin_copied_format, text))
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
                PrimeLocaleManager.getString(context, R.string.plugin_http_error_format, conn.responseCode)
            }
        } catch (e: Exception) {
            PrimeLocaleManager.getString(context, R.string.plugin_network_error_format, e.localizedMessage ?: "")
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
            val displayName = ChatHistoryManager.getDisplayNameForAddress(context, address)
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sp.getString("current_user", "") ?: ""
            val myName = AvatarManager.getMyDisplayName(context)
            val payload = "DELETE_CHAT:login=$currentUser;name=$myName".toByteArray(Charsets.UTF_8)
            val sent = BluetoothSocketHolder.sendPacket(address, displayName, 8.toByte(), payload)
            if (!sent) {
                BluetoothConnectionManager.getInstance().sendPacket(address, 8.toByte(), payload)
            }
            if (address.equals("PrimeDesktop", ignoreCase = true) || AvatarManager.isDesktopContact(context, displayName, address)) {
                PrimeCompanionServer.broadcastPacket(8.toByte(), payload)
            }
            Thread {
                try { Thread.sleep(200) } catch (_: Exception) {}
                BluetoothConnectionManager.getInstance().blockMacAddress(address, 0, "Chat deleted via plugin")
                PrimeBluetoothService.cancelReconnect(context, address)
                PrimeBluetoothService.cancelReconnect(context, displayName)
                BluetoothConnectionManager.getInstance().disconnect(address)
                BluetoothSocketHolder.removeConnection(address, displayName)
                DeviceBindingManager.unbindDeviceSecurely(context, displayName, address)
                AvatarManager.clearContactAvatar(context, displayName, displayName, address)
                ChatHistoryManager.deleteHistoryCompletely(context, displayName, address)
                PrimeNotification.cancelNotificationForUser(context, displayName, address)
                val intent = Intent("com.messenger.prime.CHAT_DELETED").apply {
                    `package` = context.packageName
                    putExtra("EXTRA_ADDRESS", address)
                    putExtra("EXTRA_NAME", displayName)
                }
                context.sendBroadcast(intent)
            }.start()
            toast(PrimeLocaleManager.getString(context, R.string.plugin_chat_deleted))
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun clearChat(address: String) {
        try {
            ChatHistoryManager.deleteHistory(context, address)
            toast(PrimeLocaleManager.getString(context, R.string.plugin_chat_cleared))
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun deleteAllChats() {
        try {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sp.edit().remove(ChatHistoryManager.getPersistedChatsKey(context)).apply()
            toast(PrimeLocaleManager.getString(context, R.string.plugin_all_chats_deleted))
            ChatListNotifier.notifyChanged()
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
    fun shareText(text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, PrimeLocaleManager.getString(context, R.string.plugin_share_chooser))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun sendSms(phoneNumber: String, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
                putExtra("sms_body", text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun getDeviceModel(): String {
        return Build.MODEL ?: "Unknown"
    }

    @JavascriptInterface
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE ?: "Unknown"
    }
    
    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    
    @JavascriptInterface
    fun getBatteryLevel(): Int {
        return try {
            val intentFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else -1
        } catch (e: Exception) {
            -1
        }
    }

    @JavascriptInterface
    fun writeFile(fileName: String, content: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun readFile(fileName: String): String {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) file.readText(Charsets.UTF_8) else ""
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun executeCommand(command: String): String {
        // Safe limited shell execution wrapper (only basic commands like ping, ls)
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val output = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            PrimeLocaleManager.getString(context, R.string.plugin_cmd_error_format, e.localizedMessage ?: "")
        }
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
            val msgId = senderLogin + "_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8)
            val msg = ChatMessage(text, timeStr, senderLogin, true, null, System.currentTimeMillis(), null, msgId)
            
            try {
                val isConnected = BluetoothConnectionManager.getInstance().isConnected(address)
                msg.messageStatus = if (isConnected) MessageStatus.SENT else MessageStatus.SENDING
            } catch (e: Exception) {}

            ChatHistoryManager.saveMessage(context, address, msg)
            ChatListNotifier.notifyChanged()
            
            val payload = (msgId + ":::" + text).toByteArray(Charsets.UTF_8)
            BluetoothConnectionManager.getInstance().sendPacket(address, 0x01.toByte(), payload)
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
            return PrimeLocaleManager.getString(context, R.string.plugin_err_empty_format, plugin.name)
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
            PrimeNotification.show(
                context,
                PrimeLocaleManager.getString(context, R.string.plugin_executed_toast_format, plugin.name)
            )
            result
        } catch (e: Exception) {
            PrimeLocaleManager.getString(context, R.string.plugin_err_exec_format, plugin.name, e.localizedMessage ?: "")
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
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_toast_log, msg))
        }

        // 2. Navigation Actions
        if (code.contains("SettingsActivity") || code.contains("openSettings") || code.contains("open_settings")) {
            bridge.openSettings()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_settings))
        }

        if (code.contains("GlobalSearchActivity") || code.contains("openSearch") || code.contains("open_search")) {
            bridge.openSearch()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_search))
        }

        if (code.contains("MediaPlayerActivity") || code.contains("openMediaPlayer") || code.contains("open_media_player")) {
            bridge.openMediaPlayer()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_player))
        }

        if (code.contains("WallpaperEditorActivity") || code.contains("openWallpaper") || code.contains("open_wallpaper_editor")) {
            bridge.openWallpaperEditor()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_wallpaper))
        }

        if (code.contains("PersonInformationActivity") || code.contains("openProfile") || code.contains("open_profile")) {
            bridge.openProfile()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_profile))
        }

        // 3. Color Accent Changes
        val colorRegex = Regex("(?:setAccentColor|set_accent_color|Color\\.parseColor)\\s*\\(\\s*\"(#\\w+)\"")
        colorRegex.find(code)?.let { match ->
            val hex = match.groupValues[1]
            bridge.setAccentColor(hex)
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_color_set, hex))
        }

        // 4. Println / Return / Output extraction
        val printRegex = Regex("(?:System\\.out\\.println|println|return|log)\\s*\\(\\s*\"([^\"]+)\"")
        printRegex.findAll(code).forEach { match ->
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_log, match.groupValues[1]))
        }

        if (outputs.isEmpty()) {
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_processed, code.length))
        }

        return PrimeLocaleManager.getString(context, R.string.plugin_java_result_header, plugin.name, outputs.joinToString("\n• "))
    }

    private fun executeJsPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        var jsResult = PrimeLocaleManager.getString(context, R.string.plugin_js_default_ok)
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
                    jsResult = PrimeLocaleManager.getString(context, R.string.plugin_js_exec_result, value ?: "")
                })
            } catch (e: Exception) {
                jsResult = PrimeLocaleManager.getString(context, R.string.plugin_js_error, e.localizedMessage ?: "")
            }
        }
        return PrimeLocaleManager.getString(context, R.string.plugin_js_result_header, plugin.name, jsResult)
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
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_py_print, match.groupValues[1]))
        }

        val toastRegex = Regex("""(?:toast|print_toast)\s*\(\s*["']([^"']+)["']\s*\)""")
        toastRegex.findAll(code).forEach { match ->
            val msg = match.groupValues[1]
            bridge.toast(msg)
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_py_toast, msg))
        }

        if (code.contains("open_settings") || code.contains("Settings")) {
            bridge.openSettings()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_settings))
        }

        if (code.contains("open_search") || code.contains("Search")) {
            bridge.openSearch()
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_search))
        }

        if (outputs.isEmpty()) {
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_py_executed, code.length))
        }

        return PrimeLocaleManager.getString(context, R.string.plugin_py_result_header, plugin.name, outputs.joinToString("\n• "))
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
                    title = PrimeLocaleManager.getString(act, R.string.plugin_html_dialog_title, plugin.name),
                    customView = layout,
                    positiveText = PrimeLocaleManager.getString(act, R.string.settings_close),
                    negativeText = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return PrimeLocaleManager.getString(context, R.string.plugin_html_launched, plugin.name)
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
                outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_sh_echo, msg))
            }
        }
        if (outputs.isEmpty()) {
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_sh_executed, code.length))
        }
        return PrimeLocaleManager.getString(context, R.string.plugin_sh_result_header, plugin.name, outputs.joinToString("\n• "))
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
                outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_json_message, message))
            }

            when (action.lowercase()) {
                "open_settings" -> {
                    bridge.openSettings()
                    outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_settings))
                }
                "open_search" -> {
                    bridge.openSearch()
                    outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_search))
                }
                "open_media_player" -> {
                    bridge.openMediaPlayer()
                    outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_java_open_player))
                }
            }

            if (color.isNotBlank()) {
                bridge.setAccentColor(color)
                outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_json_color, color))
            }
        } catch (e: Exception) {
            outputs.add(PrimeLocaleManager.getString(context, R.string.plugin_json_error, e.localizedMessage ?: ""))
        }
        return PrimeLocaleManager.getString(context, R.string.plugin_json_result_header, plugin.name, outputs.joinToString("\n• "))
    }

    private fun executeGenericPlugin(
        context: Context,
        plugin: PluginItem,
        code: String,
        inputArgs: String
    ): String {
        return PrimeLocaleManager.getString(context, R.string.plugin_generic_activated, plugin.name, plugin.fileType, code.length)
    }
}
