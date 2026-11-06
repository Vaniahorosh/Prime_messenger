package com.messenger.prime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

enum class DownloadSourceType {
    CHAT,
    BROWSER,
    UPDATE,
    OTHER
}

data class DownloadItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val sourceType: DownloadSourceType,
    val sourceName: String,
    val timestamp: Long,
    val userLogin: String
)

object DownloadsManager {
    private const val PREFS_NAME = "PrimeDownloadsDB"
    private const val KEY_DOWNLOADS = "downloads_list_v2"

    @JvmStatic
    fun getCurrentUser(context: Context): String {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        return sp.getString("current_user", "") ?: ""
    }

    @JvmStatic
    fun downloadBrowserFile(
        context: Context,
        downloadUrl: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        val currentUser = getCurrentUser(context)
        val fileName = try {
            android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }

        val destFile = File(context.filesDir, fileName)
        val transferId = "browser_dl_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val mimeToUse = if (mimetype.isNullOrEmpty() || mimetype == "*/*") getMimeType(fileName) else mimetype

        var isCancelled = false
        var httpConn: HttpURLConnection? = null

        val cancelAction = Runnable {
            isCancelled = true
            try {
                httpConn?.disconnect()
            } catch (_: Exception) {}
            if (destFile.exists()) {
                destFile.delete()
            }
            TransferManager.removeTransfer(transferId)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Скачивание отменено", Toast.LENGTH_SHORT).show()
            }
        }

        TransferManager.updateTransferProgress(
            id = transferId,
            fileName = fileName,
            filePath = destFile.absolutePath,
            fileSize = contentLength,
            progress = 0,
            isSending = false,
            sourceName = "Из браузера",
            sourceType = DownloadSourceType.BROWSER,
            userLogin = currentUser,
            cancelAction = cancelAction
        )

        Toast.makeText(context, "Загрузка '$fileName' начата в Центре загрузок", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                httpConn = conn
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                if (!userAgent.isNullOrEmpty()) {
                    conn.setRequestProperty("User-Agent", userAgent)
                }
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK && conn.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw Exception("HTTP ${conn.responseCode}")
                }

                val totalSize = if (contentLength > 0) contentLength else conn.contentLength.toLong()

                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isCancelled) return@thread

                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalSize > 0) {
                                val progress = (downloadedBytes * 100 / totalSize).toInt().coerceIn(0, 99)
                                TransferManager.updateTransferProgress(
                                    id = transferId,
                                    fileName = fileName,
                                    filePath = destFile.absolutePath,
                                    fileSize = totalSize,
                                    progress = progress,
                                    isSending = false,
                                    sourceName = "Из браузера",
                                    sourceType = DownloadSourceType.BROWSER,
                                    userLogin = currentUser,
                                    cancelAction = cancelAction
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (isCancelled) return@thread

                TransferManager.removeTransfer(transferId)
                addDownload(
                    context = context,
                    fileName = fileName,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    mimeType = mimeToUse,
                    sourceType = DownloadSourceType.BROWSER,
                    sourceName = "Из браузера"
                )

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Файл '$fileName' сохранен в Загрузки", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (!isCancelled) {
                    TransferManager.removeTransfer(transferId)
                    if (destFile.exists()) destFile.delete()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Ошибка скачивания из браузера: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun addDownload(
        context: Context,
        fileName: String,
        filePath: String,
        fileSize: Long,
        mimeType: String,
        sourceType: DownloadSourceType,
        sourceName: String
    ) {
        val currentUser = getCurrentUser(context)
        if (currentUser.isEmpty()) return

        val file = File(filePath)
        val actualSize = if (file.exists() && file.length() > 0) file.length() else fileSize
        val item = DownloadItem(
            id = "dl_${System.currentTimeMillis()}_${(1000..9999).random()}",
            fileName = fileName,
            filePath = filePath,
            fileSize = actualSize,
            mimeType = if (mimeType.isEmpty() || mimeType == "*/*") getMimeType(fileName) else mimeType,
            sourceType = sourceType,
            sourceName = sourceName,
            timestamp = System.currentTimeMillis(),
            userLogin = currentUser
        )

        val items = getAllDownloads(context).toMutableList()
        // Prevent exact duplicate path for same user
        items.removeAll { it.filePath == filePath && it.userLogin == currentUser }
        items.add(0, item)
        saveAllDownloads(context, items)
    }

    @JvmStatic
    fun getDownloadsForCurrentUser(context: Context): List<DownloadItem> {
        val currentUser = getCurrentUser(context)
        if (currentUser.isEmpty()) return emptyList()

        // Sync local files stored in app's internal files directory for this user
        scanAndSyncLocalUserFiles(context)

        return getAllDownloads(context).filter { download ->
            download.userLogin == currentUser &&
            download.sourceType != DownloadSourceType.OTHER &&
            !download.fileName.lowercase(Locale.ROOT).contains("avatar") &&
            !download.fileName.lowercase(Locale.ROOT).contains("profile") &&
            !download.fileName.lowercase(Locale.ROOT).contains("setting") &&
            File(download.filePath).exists()
        }.sortedByDescending { it.timestamp }
    }

    @JvmStatic
    fun deleteDownload(context: Context, item: DownloadItem) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        val items = getAllDownloads(context).toMutableList()
        items.removeAll { it.id == item.id || it.filePath == item.filePath }
        saveAllDownloads(context, items)
    }

    private fun getAllDownloads(context: Context): List<DownloadItem> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_DOWNLOADS, "[]") ?: "[]"
        val list = mutableListOf<DownloadItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("sourceType", "OTHER")
                val sourceType = try { DownloadSourceType.valueOf(typeStr) } catch (_: Exception) { DownloadSourceType.OTHER }
                list.add(
                    DownloadItem(
                        id = obj.optString("id"),
                        fileName = obj.optString("fileName"),
                        filePath = obj.optString("filePath"),
                        fileSize = obj.optLong("fileSize"),
                        mimeType = obj.optString("mimeType"),
                        sourceType = sourceType,
                        sourceName = obj.optString("sourceName"),
                        timestamp = obj.optLong("timestamp"),
                        userLogin = obj.optString("userLogin")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveAllDownloads(context: Context, list: List<DownloadItem>) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
                put("fileSize", item.fileSize)
                put("mimeType", item.mimeType)
                put("sourceType", item.sourceType.name)
                put("sourceName", item.sourceName)
                put("timestamp", item.timestamp)
                put("userLogin", item.userLogin)
            }
            array.put(obj)
        }
        sp.edit().putString(KEY_DOWNLOADS, array.toString()).apply()
    }

    private fun scanAndSyncLocalUserFiles(context: Context) {
        val filesDir = context.filesDir ?: return
        val existingPaths = getAllDownloads(context).map { it.filePath }.toSet()
        val files = filesDir.listFiles() ?: return

        for (file in files) {
            if (file.isFile && !existingPaths.contains(file.absolutePath)) {
                val name = file.name.lowercase(Locale.ROOT)
                // Exclude all personal profile avatars, settings, history, and system configs
                if (name.contains("avatar") ||
                    name.contains("profile") ||
                    name.contains("setting") ||
                    name.contains("history") ||
                    name.endsWith(".json") ||
                    name.endsWith(".xml") ||
                    name.endsWith(".db") ||
                    name.endsWith(".dat")
                ) {
                    continue
                }

                val (sourceType, sourceName) = when {
                    name.contains("update") || name.endsWith(".apk") ->
                        DownloadSourceType.UPDATE to "Обновление Prime"
                    name.startsWith("browser_") || name.contains("download") ->
                        DownloadSourceType.BROWSER to "Из браузера"
                    name.startsWith("rec_file_") || name.startsWith("sent_file_") ->
                        DownloadSourceType.CHAT to "Файлы чата"
                    name.startsWith("rec_photo_") || name.startsWith("sent_photo_") ->
                        DownloadSourceType.CHAT to "Фото чата"
                    name.startsWith("rec_media_") || name.startsWith("sent_media_") ->
                        DownloadSourceType.CHAT to "Медиа чата"
                    name.startsWith("sent_gif_") ->
                        DownloadSourceType.CHAT to "GIF чата"
                    else -> null to null
                }

                if (sourceType != null && sourceName != null) {
                    addDownload(
                        context = context,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        mimeType = getMimeType(file.name),
                        sourceType = sourceType,
                        sourceName = sourceName
                    )
                }
            }
        }
    }

    @JvmStatic
    fun getMimeType(fileName: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") -> "video/mp4"
            lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".ogg") -> "audio/mpeg"
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") || lower.endsWith(".json") || lower.endsWith(".html") -> "text/plain"
            else -> "*/*"
        }
    }

    @JvmStatic
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 Б"
        val units = arrayOf("Б", "КБ", "МБ", "ГБ")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.ROOT, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    @JvmStatic
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
