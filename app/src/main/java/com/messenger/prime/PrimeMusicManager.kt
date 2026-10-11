package com.messenger.prime

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Build
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long,
    val durationStr: String,
    val coverPath: String? = null,
    val isSaved: Boolean = false,
    val fileSize: Long = 0L
)

object PrimeMusicManager {

    interface PlayerListener {
        fun onTrackChanged(track: TrackItem?)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressUpdated(positionMs: Long, durationMs: Long)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: TrackItem? = null
    private var currentPlaylist: List<TrackItem> = emptyList()
    private var currentTrackIndex: Int = -1

    var mediaSession: MediaSessionCompat? = null

    private val listeners = mutableListOf<PlayerListener>()
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                val pos = mp.currentPosition.toLong()
                val dur = mp.duration.toLong()
                notifyProgress(pos, dur)
                handler.postDelayed(this, 250)
            }
        }
    }

    fun addListener(listener: PlayerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        listener.onTrackChanged(currentTrack)
        listener.onPlaybackStateChanged(isPlaying())
        val mp = mediaPlayer
        if (mp != null) {
            listener.onProgressUpdated(mp.currentPosition.toLong(), mp.duration.toLong().coerceAtLeast(1L))
        }
    }

    fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    fun getCurrentTrack(): TrackItem? = currentTrack
    fun getCurrentPlaylist(): List<TrackItem> = currentPlaylist
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    private fun initMediaSessionIfNeeded(context: Context) {
        if (mediaSession != null) return
        val session = MediaSessionCompat(context, "PrimeMusicSession")
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                togglePlayPause(context)
            }
            override fun onPause() {
                togglePlayPause(context)
            }
            override fun onSkipToNext() {
                playNextTrack(context)
            }
            override fun onSkipToPrevious() {
                playPreviousTrack(context)
            }
            override fun onSeekTo(pos: Long) {
                seekTo(pos.toInt())
            }
        })
        session.isActive = true
        mediaSession = session
    }

    private fun updateService(context: Context) {
        val intent = Intent(context, PrimeMusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService(context: Context) {
        val intent = Intent(context, PrimeMusicService::class.java)
        intent.action = "STOP_SERVICE"
        context.startService(intent)
    }

    fun playTrack(context: Context, track: TrackItem, playlist: List<TrackItem> = listOf(track)) {
        initMediaSessionIfNeeded(context)
        currentPlaylist = if (playlist.isNotEmpty()) playlist else listOf(track)
        val foundIdx = currentPlaylist.indexOfFirst { it.path == track.path }
        currentTrackIndex = if (foundIdx >= 0) foundIdx else 0
        currentTrack = track

        stopAndReleasePlayer()

        try {
            val mp = MediaPlayer().apply {
                setDataSource(context, Uri.parse(track.path))
                prepare()
                start()
                setOnCompletionListener {
                    notifyPlaybackState(false)
                    playNextTrack(context)
                }
            }
            mediaPlayer = mp
            updateMediaSessionMetadata(track)
            notifyTrackChanged(track)
            notifyPlaybackState(true)
            updateService(context)
            handler.post(progressRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
            notifyPlaybackState(false)
        }
    }

    private fun updateMediaSessionMetadata(track: TrackItem) {
        val session = mediaSession ?: return
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
        
        if (!track.coverPath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(track.coverPath)
                if (bitmap != null) {
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                }
            } catch (_: Exception) {}
        }
        session.setMetadata(builder.build())
    }

    fun togglePlayPause(context: Context) {
        val mp = mediaPlayer
        if (mp == null) {
            val track = currentTrack
            if (track != null) {
                playTrack(context, track, currentPlaylist)
            }
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            handler.removeCallbacks(progressRunnable)
            notifyPlaybackState(false)
            updateService(context)
        } else {
            mp.start()
            handler.post(progressRunnable)
            notifyPlaybackState(true)
            updateService(context)
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
        mediaPlayer?.seekTo(positionMs)
    }

    @JvmOverloads
    fun stopAndReleasePlayer(context: Context? = null) {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        notifyPlaybackState(false)
        if (context != null) {
            stopService(context)
        }
    }

    private fun notifyTrackChanged(track: TrackItem?) {
        for (l in ArrayList(listeners)) {
            l.onTrackChanged(track)
        }
    }

    private fun notifyPlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
        
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, pos, 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY or 
                            PlaybackStateCompat.ACTION_PAUSE or 
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or 
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
        )

        for (l in ArrayList(listeners)) {
            l.onPlaybackStateChanged(isPlaying)
        }
    }

    private fun notifyProgress(pos: Long, dur: Long) {
        for (l in ArrayList(listeners)) {
            l.onProgressUpdated(pos, dur)
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
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol).toString()
                    val title = c.getString(titleCol) ?: "Без названия"
                    val artist = c.getString(artistCol) ?: "Неизвестный исполнитель"
                    val path = c.getString(dataCol) ?: continue
                    val durMs = c.getLong(durCol)
                    val size = c.getLong(sizeCol)

                    if (!File(path).exists() || foundPaths.contains(path)) continue
                    foundPaths.add(path)

                    val durationStr = formatDuration(durMs)
                    val coverPath = getCoverArtForTrack(context, path, id)

                    list.add(
                        TrackItem(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Неизвестный исполнитель" else artist,
                            path = path,
                            durationMs = durMs,
                            durationStr = durationStr,
                            coverPath = coverPath,
                            isSaved = isTrackInCollection(context, path),
                            fileSize = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback file scan in public Music / Download directories
        try {
            val scanDirs = listOfNotNull(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                File(context.filesDir, "saved_music")
            )
            for (dir in scanDirs) {
                if (!dir.exists()) continue
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile && !foundPaths.contains(file.absolutePath)) {
                        val ext = file.extension.lowercase()
                        if (ext == "mp3" || ext == "m4a" || ext == "aac" || ext == "wav" || ext == "ogg" || ext == "flac") {
                            foundPaths.add(file.absolutePath)
                            val meta = extractMetadata(context, file.absolutePath)
                            list.add(meta)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return list
    }

    fun getSavedTracks(context: Context): List<TrackItem> {
        val list = mutableListOf<TrackItem>()
        val savedDir = File(context.filesDir, "saved_music")
        if (!savedDir.exists()) savedDir.mkdirs()

        val files = savedDir.listFiles() ?: return emptyList()
        for (file in files) {
            if (file.isFile && (file.extension.equals("mp3", true) || file.extension.equals("m4a", true) || file.extension.equals("aac", true) || file.extension.equals("wav", true) || file.extension.equals("ogg", true))) {
                val meta = extractMetadata(context, file.absolutePath)
                list.add(meta.copy(isSaved = true))
            }
        }
        return list
    }

    fun saveTrackToCollection(context: Context, sourcePath: String, title: String, artist: String): TrackItem? {
        try {
            val srcFile = File(sourcePath)
            if (!srcFile.exists()) return null

            val savedDir = File(context.filesDir, "saved_music")
            if (!savedDir.exists()) savedDir.mkdirs()

            val sanitizedTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val targetFile = File(savedDir, "${sanitizedTitle}_${System.currentTimeMillis()}.${srcFile.extension}")

            srcFile.copyTo(targetFile, overwrite = true)

            val meta = extractMetadata(context, targetFile.absolutePath)
            val updated = meta.copy(
                title = if (title.isNotBlank()) title else meta.title,
                artist = if (artist.isNotBlank()) artist else meta.artist,
                isSaved = true
            )
            return updated
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun isTrackInCollection(context: Context, path: String): Boolean {
        if (path.contains("saved_music")) return true
        val savedDir = File(context.filesDir, "saved_music")
        if (!savedDir.exists()) return false
        val fileName = File(path).name
        return savedDir.listFiles()?.any { it.name == fileName } == true
    }

    fun extractMetadata(context: Context, path: String): TrackItem {
        val file = File(path)
        var title = file.nameWithoutExtension
        var artist = "Неизвестный исполнитель"
        var durMs = 0L
        var coverPath: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
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
                val coverFile = File(coversDir, "cover_${file.name.hashCode()}.jpg")
                if (!coverFile.exists()) {
                    FileOutputStream(coverFile).use { out ->
                        out.write(artBytes)
                    }
                }
                coverPath = coverFile.absolutePath
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        return TrackItem(
            id = path,
            title = title,
            artist = artist,
            path = path,
            durationMs = durMs,
            durationStr = formatDuration(durMs),
            coverPath = coverPath,
            isSaved = path.contains("saved_music"),
            fileSize = file.length()
        )
    }

    private fun getCoverArtForTrack(context: Context, path: String, id: String): String? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                val coversDir = File(context.filesDir, "music_covers")
                if (!coversDir.exists()) coversDir.mkdirs()
                val coverFile = File(coversDir, "cover_$id.jpg")
                if (!coverFile.exists()) {
                    FileOutputStream(coverFile).use { out ->
                        out.write(artBytes)
                    }
                }
                return coverFile.absolutePath
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        return null
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
