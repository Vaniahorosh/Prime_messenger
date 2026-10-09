package com.messenger.prime

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UpdateChecker {

    fun checkForUpdates(activity: Activity, manualCheck: Boolean = false) {
        thread {
            try {
                val url = URL("https://api.github.com/repos/Vaniahorosh/Prime_messenger/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "")
                    val releaseUrl = json.optString("html_url", "https://github.com/Vaniahorosh/Prime_messenger/releases/")
                    val releaseNotes = json.optString("body", "Доступна новая версия!")

                    var apkDownloadUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i)
                            val name = asset?.optString("name", "") ?: ""
                            if (name.endsWith(".apk")) {
                                apkDownloadUrl = asset?.optString("browser_download_url")
                                break
                            }
                        }
                    }

                    val currentVersion = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.0"
                    
                    val remoteVersion = tagName.replace("v", "").replace("V", "").trim()
                    val localVersion = currentVersion.replace("v", "").replace("V", "").trim()

                    if (isNewerVersion(localVersion, remoteVersion)) {
                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(activity, remoteVersion, releaseNotes, releaseUrl, apkDownloadUrl)
                        }
                    } else if (manualCheck) {
                        Handler(Looper.getMainLooper()).post {
                            PrimeNotification.show(activity, "У вас последняя версия ($localVersion) ✓")
                        }
                    }
                } else if (manualCheck) {
                    Handler(Looper.getMainLooper()).post {
                        PrimeNotification.show(activity, "Ошибка проверки обновлений")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (manualCheck) {
                    Handler(Looper.getMainLooper()).post {
                        PrimeNotification.show(activity, "Нет подключения к серверу")
                    }
                }
            }
        }
    }

    private fun isNewerVersion(local: String, remote: String): Boolean {
        try {
            val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(localParts.size, remoteParts.size)
            for (i in 0 until maxLength) {
                val l = localParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            return remote > local
        }
        return false
    }

    private fun showUpdateDialog(
        activity: Activity, 
        version: String, 
        notes: String, 
        releaseUrl: String, 
        apkUrl: String?
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        PrimeBlurDialog.show(
            activity = activity,
            title = "Доступно обновление $version!",
            message = "Вышла новая версия приложения с новыми функциями и исправлениями.\n\nЧто нового:\n$notes",
            positiveText = "Скачать",
            negativeText = "Позже",
            iconRes = R.drawable.ic_download,
            onPositive = {
                if (apkUrl != null) {
                    downloadAndInstallApk(activity, apkUrl)
                } else {
                    // Fallback to browser if no APK asset was found
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    private fun downloadAndInstallApk(activity: Activity, downloadUrl: String) {
        val uiContainer = createProgressDialog(activity)
        val progressDialog = uiContainer.dialog
        val progressBar = uiContainer.progressBar
        val tvProgress = uiContainer.tvProgress

        thread {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }

                val fileLength = connection.contentLength
                val tempFile = File(activity.cacheDir, "prime_update.apk")
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val input = connection.inputStream
                val output = FileOutputStream(tempFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        Handler(Looper.getMainLooper()).post {
                            progressBar.isIndeterminate = false
                            progressBar.progress = progress
                            tvProgress.text = "Загружено: $progress%"
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                Handler(Looper.getMainLooper()).post {
                    progressDialog.dismiss()
                    installApk(activity, tempFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    progressDialog.dismiss()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        activity.startActivity(intent)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    private fun installApk(activity: Activity, apkFile: File) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(settingsIntent)
                    PrimeNotification.show(activity, "Разрешите установку неизвестных приложений")
                    return
                }
            }

            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val resInfoList = activity.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                activity.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            PrimeNotification.show(activity, "Ошибка запуска установки APK")
        }
    }

    private class ProgressUI(
        val dialog: AlertDialog,
        val progressBar: ProgressBar,
        val tvProgress: TextView
    )

    private fun createProgressDialog(activity: Activity): ProgressUI {
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            gravity = Gravity.CENTER
        }

        val tvTitle = TextView(activity).apply {
            text = "Загрузка обновления..."
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvProgress = TextView(activity).apply {
            text = "Подготовка..."
            setPadding(0, 20, 0, 0)
        }

        layout.addView(tvTitle)
        layout.addView(progressBar)
        layout.addView(tvProgress)

        val dialog = PrimeBlurDialog.show(
            activity = activity,
            title = "Загрузка обновления",
            message = null,
            positiveText = "В фон",
            negativeText = null,
            customView = layout,
            onPositive = {
                // Диалог просто закроется, а поток скачивания продолжит работу
            }
        )
        
        try {
            dialog.findViewById<TextView>(R.id.tvDialogTitle)?.visibility = android.view.View.GONE
        } catch (e: Exception) {}

        return ProgressUI(dialog, progressBar, tvProgress)
    }
}
