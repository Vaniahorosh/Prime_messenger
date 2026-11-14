package com.messenger.prime.emoji

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StickerBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // at least 16MB cache

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(path: String): Bitmap? {
        return lruCache.get(path)
    }

    fun put(path: String, bitmap: Bitmap) {
        lruCache.put(path, bitmap)
    }

    suspend fun loadOptimized(path: String, targetSize: Int = 256): Bitmap? {
        val cached = get(path)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists() || file.length() == 0L) return@withContext null

                // Video stickers support (MP4, WEBM)
                if (path.endsWith(".mp4", ignoreCase = true) || path.endsWith(".webm", ignoreCase = true)) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(path)
                        val frame = retriever.getFrameAtTime(0)
                        if (frame != null) {
                            put(path, frame)
                        }
                        return@withContext frame
                    } finally {
                        try { retriever.release() } catch (_: Throwable) {}
                    }
                }

                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, boundsOptions)

                var sampleSize = 1
                while (boundsOptions.outWidth / (sampleSize * 2) >= targetSize &&
                    boundsOptions.outHeight / (sampleSize * 2) >= targetSize
                ) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
                if (bitmap != null) {
                    put(path, bitmap)
                }
                bitmap
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun clear() {
        lruCache.evictAll()
    }
}
