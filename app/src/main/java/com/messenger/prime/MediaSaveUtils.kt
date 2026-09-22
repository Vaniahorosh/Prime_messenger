package com.messenger.prime

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaSaveUtils {

    /**
     * Сохраняет исходный медиафайл (фото или видео) в галерею устройства.
     */
    fun saveToGallery(context: Context, mediaUri: Uri?, isVideo: Boolean): Boolean {
        if (mediaUri == null) return false
        try {
            val resolver = context.contentResolver
            val inputStream = if ("file".equals(mediaUri.scheme, ignoreCase = true) && mediaUri.path != null) {
                val f = File(mediaUri.path!!)
                if (!f.exists()) return false
                f.inputStream()
            } else {
                resolver.openInputStream(mediaUri)
            } ?: return false

            val timeStamp = System.currentTimeMillis()
            val displayName = if (isVideo) "PRIME_VID_$timeStamp.mp4" else "PRIME_IMG_$timeStamp.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/Prime" else Environment.DIRECTORY_PICTURES + "/Prime"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = resolver.insert(collection, contentValues) ?: return false

                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                return true
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                )
                val primeDir = File(targetDir, "Prime")
                if (!primeDir.exists()) primeDir.mkdirs()

                val targetFile = File(primeDir, displayName)
                targetFile.outputStream().use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(if (isVideo) "video/mp4" else "image/jpeg"),
                    null
                )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
