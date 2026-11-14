package com.messenger.prime.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class StickerPack(
    val id: String,
    val name: String,
    val author: String = "Вы",
    val isBuiltIn: Boolean = false,
    val coverPath: String? = null,
    val stickerPaths: MutableList<String> = mutableListOf()
)

object StickerManager {

    private const val META_FILE = "stickers_meta.json"
    private val gson = Gson()

    fun getStickerPacks(context: Context): List<StickerPack> {
        val rootDir = getStickersDir(context)
        val metaFile = File(rootDir, META_FILE)

        // Delete legacy Prime Classic folder if it exists
        try {
            val starterDir = File(rootDir, "prime_starter_pack")
            if (starterDir.exists()) {
                starterDir.deleteRecursively()
            }
        } catch (_: Exception) {}

        if (!metaFile.exists()) {
            return emptyList()
        }

        return try {
            val json = metaFile.readText()
            val type = object : TypeToken<List<StickerPack>>() {}.type
            val packs: List<StickerPack> = gson.fromJson(json, type) ?: emptyList()
            // Strictly exclude any legacy "Prime Classic" pack
            val filtered = packs.filter { it.id != "prime_starter_pack" && it.name != "Prime Classic" }
            if (filtered.size != packs.size) {
                savePacks(context, filtered)
            }
            filtered
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun createPack(context: Context, name: String, author: String = "Вы"): StickerPack {
        val trimmedName = name.trim().ifEmpty { "Стикерпак" }
        val trimmedAuthor = author.trim().ifEmpty { "Вы" }
        val id = UUID.randomUUID().toString()
        val newPack = StickerPack(
            id = id,
            name = trimmedName,
            author = trimmedAuthor,
            isBuiltIn = false,
            stickerPaths = mutableListOf()
        )
        val current = getStickerPacks(context).toMutableList()
        current.add(newPack)
        savePacks(context, current)
        return newPack
    }

    fun updatePackDetails(context: Context, packId: String, newName: String, newAuthor: String): Boolean {
        val trimmedName = newName.trim().ifEmpty { return false }
        val trimmedAuthor = newAuthor.trim().ifEmpty { "Вы" }
        val current = getStickerPacks(context).toMutableList()
        val index = current.indexOfFirst { it.id == packId }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(name = trimmedName, author = trimmedAuthor)
            savePacks(context, current)
            return true
        }
        return false
    }

    fun addStickerToPack(context: Context, packId: String, uri: Uri): String? {
        val count = addMultipleStickersToPack(context, packId, listOf(uri))
        return if (count > 0) {
            getPackById(context, packId)?.stickerPaths?.lastOrNull()
        } else {
            null
        }
    }

    fun addMultipleStickersToPack(context: Context, packId: String, uris: List<Uri>): Int {
        var addedCount = 0
        val packDir = File(getStickersDir(context), packId).apply { mkdirs() }
        val newPaths = mutableListOf<String>()

        for (uri in uris) {
            try {
                val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
                val isVideo = mime.startsWith("video/") ||
                        uri.toString().lowercase().endsWith(".mp4") ||
                        uri.toString().lowercase().endsWith(".webm")

                if (isVideo) {
                    // Video Sticker Handling (max 5 seconds, max 3MB, MP4/WEBM)
                    val added = processVideoSticker(context, uri, packDir)
                    if (added != null) {
                        newPaths.add(added)
                        addedCount++
                    }
                } else {
                    // Image Sticker Handling (PNG with transparency, WEBP, JPEG)
                    val added = processImageSticker(context, uri, packDir, addedCount)
                    if (added != null) {
                        newPaths.add(added)
                        addedCount++
                    }
                }
            } catch (_: Exception) {}
        }

        if (newPaths.isNotEmpty()) {
            val current = getStickerPacks(context).toMutableList()
            val pack = current.find { it.id == packId }
            if (pack != null) {
                pack.stickerPaths.addAll(newPaths)
                savePacks(context, current)
            }
        }
        return addedCount
    }

    private fun processImageSticker(context: Context, uri: Uri, packDir: File, index: Int): String? {
        val stickerFile = File(packDir, "sticker_${System.currentTimeMillis()}_$index.png")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val rawBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Scale to standard sticker dimension (max 512x512) while preserving aspect ratio & transparency
            val maxDim = 512
            val w = rawBitmap.width
            val h = rawBitmap.height
            val scale = (maxDim.toFloat() / maxOf(w, h)).coerceAtMost(1.0f)
            val targetW = (w * scale).toInt().coerceAtLeast(1)
            val targetH = (h * scale).toInt().coerceAtLeast(1)

            val scaledBitmap = if (w != targetW || h != targetH) {
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else {
                rawBitmap
            }

            FileOutputStream(stickerFile).use { fos ->
                // PNG preserves transparency completely
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            StickerBitmapCache.put(stickerFile.absolutePath, scaledBitmap)
            return stickerFile.absolutePath
        }
        return null
    }

    private fun processVideoSticker(context: Context, uri: Uri, packDir: File): String? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            // Enforce max 5 seconds for video stickers
            if (durationMs > 6000L) {
                // Video exceeds duration limit
                return null
            }

            val stickerFile = File(packDir, "sticker_vid_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stickerFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    val maxBytes = 4 * 1024 * 1024 // 4MB limit for crisp video sticker

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        if (totalBytes > maxBytes) {
                            stickerFile.delete()
                            return null
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Cache first frame thumbnail
            val firstFrame = retriever.getFrameAtTime(0)
            if (firstFrame != null) {
                StickerBitmapCache.put(stickerFile.absolutePath, firstFrame)
            }
            return stickerFile.absolutePath
        } catch (_: Exception) {
            return null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    fun renamePack(context: Context, packId: String, newName: String): Boolean {
        val trimmed = newName.trim().ifEmpty { return false }
        val current = getStickerPacks(context).toMutableList()
        val index = current.indexOfFirst { it.id == packId }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(name = trimmed)
            savePacks(context, current)
            return true
        }
        return false
    }

    fun getPackById(context: Context, packId: String): StickerPack? {
        return getStickerPacks(context).find { it.id == packId }
    }

    fun deleteSticker(context: Context, packId: String, path: String) {
        val current = getStickerPacks(context).toMutableList()
        val pack = current.find { it.id == packId }
        if (pack != null) {
            pack.stickerPaths.remove(path)
            try {
                File(path).delete()
            } catch (_: Exception) {}
            savePacks(context, current)
        }
    }

    fun deletePack(context: Context, packId: String) {
        val current = getStickerPacks(context).toMutableList()
        val pack = current.find { it.id == packId }
        if (pack != null) {
            current.remove(pack)
            try {
                File(getStickersDir(context), packId).deleteRecursively()
            } catch (_: Exception) {}
            savePacks(context, current)
        }
    }

    fun setPackCover(context: Context, packId: String, coverPath: String?): Boolean {
        val current = getStickerPacks(context).toMutableList()
        val index = current.indexOfFirst { it.id == packId }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(coverPath = coverPath)
            savePacks(context, current)
            return true
        }
        return false
    }

    fun reorderPacks(context: Context, fromIndex: Int, toIndex: Int): Boolean {
        val current = getStickerPacks(context).toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            savePacks(context, current)
            return true
        }
        return false
    }

    fun reorderStickersInPack(context: Context, packId: String, fromIndex: Int, toIndex: Int): Boolean {
        val current = getStickerPacks(context).toMutableList()
        val pack = current.find { it.id == packId } ?: return false
        if (fromIndex in pack.stickerPaths.indices && toIndex in pack.stickerPaths.indices && fromIndex != toIndex) {
            val item = pack.stickerPaths.removeAt(fromIndex)
            pack.stickerPaths.add(toIndex, item)
            savePacks(context, current)
            return true
        }
        return false
    }

    private const val RECENTS_FILE = "recent_stickers.json"

    fun getRecentStickers(context: Context): List<String> {
        return try {
            val file = File(getStickersDir(context), RECENTS_FILE)
            if (!file.exists()) return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(file.readText(), type) ?: emptyList()
            list.filter { File(it).exists() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSticker(context: Context, path: String) {
        try {
            if (!File(path).exists()) return
            val recents = getRecentStickers(context).toMutableList()
            recents.remove(path)
            recents.add(0, path)
            val trimmed = recents.take(24)
            val file = File(getStickersDir(context), RECENTS_FILE)
            file.writeText(gson.toJson(trimmed))
        } catch (_: Exception) {}
    }

    fun clearRecentStickers(context: Context) {
        try {
            val file = File(getStickersDir(context), RECENTS_FILE)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    private fun savePacks(context: Context, packs: List<StickerPack>) {
        try {
            val rootDir = getStickersDir(context)
            val metaFile = File(rootDir, META_FILE)
            metaFile.writeText(gson.toJson(packs))
        } catch (_: Exception) {}
    }

    private fun getStickersDir(context: Context): File {
        return File(context.filesDir, "stickers").apply { mkdirs() }
    }
}
