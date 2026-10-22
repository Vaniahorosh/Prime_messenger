package com.messenger.prime

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long,
    val durationStr: String = "00:00",
    val coverPath: String? = null,
    val isSaved: Boolean = false,
    val fileSize: Long = 0L
)

object PrimeMusicManager {

    interface PlayerListener {
        fun onTrackChanged(track: TrackItem?) {}
        fun onPlaybackStateChanged(isPlaying: Boolean) {}
        fun onProgressUpdated(progress: Long, duration: Long) {}
    }

    private val listeners = mutableListOf<PlayerListener>()

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: TrackItem? = null
    private var currentPlaylist: List<TrackItem> = emptyList()
    private var currentTrackIndex: Int = -1

    var mediaSession: MediaSessionCompat? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                val pos = mp.currentPosition.toLong()
                val dur = mp.duration.toLong().coerceAtLeast(1L)
                notifyProgress(pos, dur)
                handler.postDelayed(this, 250)
            }
        }
    }

    private fun showToastOnUi(context: Context, text: String) {
        handler.post {
            try {
                Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addListener(listener: PlayerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        val mp = mediaPlayer
        if (mp != null) {
            listener.onProgressUpdated(mp.currentPosition.toLong(), mp.duration.toLong().coerceAtLeast(1L))
        }
    }

    fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    fun getCurrentDuration(): Long = mediaPlayer?.duration?.toLong() ?: 0L

    fun getCurrentTrack(): TrackItem? = currentTrack

    fun getCurrentPlaylist(): List<TrackItem> = currentPlaylist

    private fun initMediaSessionIfNeeded(context: Context) {
        if (mediaSession != null) return
        val appContext = context.applicationContext
        val session = MediaSessionCompat(appContext, "PrimeMusicSession")
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                togglePlayPause(appContext)
            }
            override fun onPause() {
                togglePlayPause(appContext)
            }
            override fun onSkipToNext() {
                playNextTrack(appContext)
            }
            override fun onSkipToPrevious() {
                playPreviousTrack(appContext)
            }
            override fun onSeekTo(pos: Long) {
                seekTo(pos.toInt())
            }
        })
        session.isActive = true
        mediaSession = session
    }

    private fun updateService(context: Context) {
        try {
            val intent = Intent(context.applicationContext, PrimeMusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(intent)
            } else {
                context.applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Failed to start music service", e)
        }
    }

    private fun stopService(context: Context) {
        try {
            val intent = Intent(context.applicationContext, PrimeMusicService::class.java)
            intent.action = "STOP_SERVICE"
            context.applicationContext.startService(intent)
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Failed to stop music service", e)
        }
    }

    fun playTrack(context: Context, track: TrackItem, playlist: List<TrackItem> = listOf(track)) {
        val appContext = context.applicationContext
        val cleanPath = track.path.replace("file://", "").trim()

        if (cleanPath.isEmpty()) {
            showToastOnUi(appContext, "Файл трека не найден")
            return
        }

        val realTrack = track.copy(path = cleanPath)

        initMediaSessionIfNeeded(appContext)
        currentPlaylist = if (playlist.isNotEmpty()) playlist else listOf(realTrack)
        val foundIdx = currentPlaylist.indexOfFirst { it.path == realTrack.path }
        currentTrackIndex = if (foundIdx >= 0) foundIdx else 0
        currentTrack = realTrack

        stopAndReleasePlayer()

        try {
            val mp = MediaPlayer().apply {
                if (cleanPath.startsWith("content://") || cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
                    setDataSource(appContext, Uri.parse(cleanPath))
                } else {
                    val file = File(cleanPath)
                    if (!file.exists()) {
                        showToastOnUi(appContext, "Файл не найден на устройстве")
                        return
                    }
                    setDataSource(file.absolutePath)
                }
                prepareAsync()
                setOnPreparedListener { player ->
                    try {
                        player.start()
                        val dur = player.duration.toLong().coerceAtLeast(1L)
                        currentTrack = realTrack.copy(durationMs = dur, durationStr = formatDuration(dur))
                        
                        handler.post {
                            notifyTrackChanged(currentTrack)
                            notifyPlaybackState(true)
                            updateService(appContext)
                        }
                        handler.post(progressRunnable)
                    } catch (e: Exception) {
                        Log.e("PrimeMusic", "Error starting player", e)
                    }
                }
                setOnCompletionListener {
                    handler.post {
                        notifyPlaybackState(false)
                        playNextTrack(appContext)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("PrimeMusic", "MediaPlayer error: what=$what extra=$extra")
                    handler.post {
                        notifyPlaybackState(false)
                    }
                    true
                }
            }
            mediaPlayer = mp
            updateMediaSessionMetadata(realTrack)
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Error setting up player for: ${realTrack.title}", e)
            showToastOnUi(appContext, "Ошибка воспроизведения аудио")
            handler.post {
                notifyPlaybackState(false)
            }
        }
    }

    fun togglePlayPause(context: Context) {
        val appContext = context.applicationContext
        val mp = mediaPlayer
        if (mp == null) {
            val track = currentTrack
            if (track != null) {
                playTrack(appContext, track, currentPlaylist)
            }
            return
        }

        try {
            if (mp.isPlaying) {
                mp.pause()
                handler.post { notifyPlaybackState(false) }
                handler.removeCallbacks(progressRunnable)
            } else {
                mp.start()
                handler.post { notifyPlaybackState(true) }
                handler.post(progressRunnable)
            }
            currentTrack?.let { updateMediaSessionMetadata(it) }
            updateService(appContext)
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Error toggling play pause", e)
        }
    }

    fun playNextTrack(context: Context) {
        if (currentPlaylist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
        val nextTrack = currentPlaylist[currentTrackIndex]
        playTrack(context, nextTrack, currentPlaylist)
    }

    fun playPreviousTrack(context: Context) {
        if (currentPlaylist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) currentPlaylist.size - 1 else currentTrackIndex - 1
        val prevTrack = currentPlaylist[currentTrackIndex]
        playTrack(context, prevTrack, currentPlaylist)
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmOverloads
    fun stopAndReleasePlayer(context: Context? = null) {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        handler.post { notifyPlaybackState(false) }
        if (context != null) {
            stopService(context)
        }
    }

    private fun updateMediaSessionMetadata(track: TrackItem) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Prime Collection")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)

        if (!track.coverPath.isNullOrEmpty()) {
            val f = File(track.coverPath)
            if (f.exists()) {
                try {
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bmp)
                } catch (_: Exception) {}
            }
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun notifyTrackChanged(track: TrackItem?) {
        handler.post {
            for (l in ArrayList(listeners)) {
                l.onTrackChanged(track)
            }
        }
    }

    private fun notifyPlaybackState(isPlaying: Boolean) {
        handler.post {
            val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L

            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(state, pos, 1f)
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SEEK_TO
                    )
                    .build()
            )

            for (l in ArrayList(listeners)) {
                l.onPlaybackStateChanged(isPlaying)
            }
        }
    }

    private fun notifyProgress(pos: Long, dur: Long) {
        handler.post {
            for (l in ArrayList(listeners)) {
                l.onProgressUpdated(pos, dur)
            }
        }
    }

    // --- Device & Saved Music Scanning ---

    fun getDeviceTracks(context: Context): List<TrackItem> {
        val list = mutableListOf<TrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        val selection: String? = null
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val foundPaths = HashSet<String>()

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (c.moveToNext()) {
                    val path = c.getString(dataCol) ?: continue
                    if (foundPaths.contains(path)) continue
                    foundPaths.add(path)

                    val id = c.getLong(idCol).toString()
                    val title = c.getString(titleCol) ?: "Неизвестный трек"
                    val artist = c.getString(artistCol) ?: "Неизвестный исполнитель"
                    val dur = c.getLong(durCol)

                    list.add(
                        TrackItem(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Неизвестный исполнитель" else artist,
                            path = path,
                            durationMs = dur,
                            durationStr = formatDuration(dur)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Error loading device tracks", e)
        }

        return list
    }

    fun getSavedTracks(context: Context): List<TrackItem> {
        val list = mutableListOf<TrackItem>()
        val savedDir = File(context.filesDir, "saved_music")
        if (!savedDir.exists()) return list

        val files = savedDir.listFiles() ?: return list
        for (f in files) {
            if (f.isFile && (f.name.endsWith(".mp3") || f.name.endsWith(".m4a") || f.name.endsWith(".wav") || f.name.endsWith(".ogg"))) {
                val meta = extractMetadata(context, f.absolutePath)
                list.add(meta.copy(isSaved = true))
            }
        }
        return list
    }

    fun isTrackInCollection(context: Context, path: String): Boolean {
        if (path.isEmpty()) return false
        val savedDir = File(context.filesDir, "saved_music")
        if (!savedDir.exists()) return false
        val fileName = File(path).name
        return savedDir.listFiles()?.any { it.name == fileName } == true
    }

    fun saveTrackToCollection(context: Context, sourcePath: String, title: String, artist: String): TrackItem? {
        if (sourcePath.isEmpty()) return null
        try {
            val savedDir = File(context.filesDir, "saved_music")
            if (!savedDir.exists()) savedDir.mkdirs()

            val srcFile = File(sourcePath.replace("file://", ""))
            val targetFile = File(savedDir, if (srcFile.name.isNotBlank()) srcFile.name else "music_${System.currentTimeMillis()}.mp3")

            if (srcFile.exists()) {
                srcFile.copyTo(targetFile, overwrite = true)
            } else if (sourcePath.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(sourcePath))?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val meta = extractMetadata(context, targetFile.absolutePath)
            return meta.copy(
                title = if (title.isNotBlank()) title else meta.title,
                artist = if (artist.isNotBlank()) artist else meta.artist,
                isSaved = true
            )
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Error saving track to collection", e)
            return null
        }
    }

    fun extractMetadata(context: Context, path: String?): TrackItem {
        if (path.isNullOrEmpty()) {
            return TrackItem("unknown", "Аудиозапись", "Неизвестный исполнитель", "", 0L, "00:00")
        }

        val cleanPath = path.replace("file://", "").trim()
        var title = "Аудиозапись"
        var artist = "Неизвестный исполнитель"
        var durMs = 0L
        var coverPath: String? = null

        try {
            val file = File(cleanPath)
            if (file.exists() && file.nameWithoutExtension.isNotBlank()) {
                title = file.nameWithoutExtension
            }
        } catch (_: Exception) {}

        val retriever = MediaMetadataRetriever()
        try {
            if (cleanPath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(cleanPath))
            } else {
                retriever.setDataSource(cleanPath)
            }

            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            if (!metaTitle.isNullOrBlank()) title = metaTitle
            if (!metaArtist.isNullOrBlank()) artist = metaArtist
            if (!metaDur.isNullOrBlank()) durMs = metaDur.toLongOrNull() ?: 0L

            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                val coversDir = File(context.filesDir, "music_covers")
                if (!coversDir.exists()) coversDir.mkdirs()
                val coverFile = File(coversDir, "cover_${cleanPath.hashCode()}.jpg")
                if (!coverFile.exists()) {
                    FileOutputStream(coverFile).use { out ->
                        out.write(artBytes)
                    }
                }
                coverPath = coverFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("PrimeMusic", "Metadata extraction failed for $cleanPath", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        return TrackItem(
            id = cleanPath,
            title = title,
            artist = artist,
            path = cleanPath,
            durationMs = durMs,
            durationStr = formatDuration(durMs),
            coverPath = coverPath
        )
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }
}
