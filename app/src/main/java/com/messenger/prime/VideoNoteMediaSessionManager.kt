package com.messenger.prime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

/**
 * Publishes system MediaSession and Notification for Video Notes so they show up
 * in Android system media controls (Notification Shade, Lock Screen, Wearables)
 * and respond to System Media Play/Pause/Stop actions.
 */
class VideoNoteMediaSessionManager(
    private val context: Context,
    private val actionListener: OnMediaSessionActionListener? = null
) {
    interface OnMediaSessionActionListener {
        fun onMediaPlay()
        fun onMediaPause()
        fun onMediaStop()
    }

    private var mediaSession: MediaSessionCompat? = null
    private var notificationManager: NotificationManager? = null

    init {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Воспроизведение кружков",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением видеосообщений"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun startMediaSession(senderName: String, videoPath: String?, durationMs: Int, isPlaying: Boolean) {
        stopMediaSession()

        try {
            val session = MediaSessionCompat(context, "PrimeVideoNoteSession").apply {
                isActive = true
            }
            mediaSession = session

            session.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    actionListener?.onMediaPlay()
                    updatePlaybackState(true, session.controller.playbackState?.position ?: 0L, 1.0f)
                }

                override fun onPause() {
                    actionListener?.onMediaPause()
                    updatePlaybackState(false, session.controller.playbackState?.position ?: 0L, 0.0f)
                }

                override fun onStop() {
                    actionListener?.onMediaStop()
                    stopMediaSession()
                }
            })

            var videoThumbnail: Bitmap? = null
            if (!videoPath.isNullOrEmpty()) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    videoThumbnail = retriever.getFrameAtTime(100000)
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Видеосообщение")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, senderName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Prime Messenger")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs.toLong())

            videoThumbnail?.let {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
            }

            session.setMetadata(metadataBuilder.build())
            updatePlaybackState(isPlaying, 0L, 1.0f)

            showMediaNotification(senderName, isPlaying, videoThumbnail)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, positionMs: Long, speed: Float) {
        val session = mediaSession ?: return
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, positionMs, speed)
            .build()

        session.setPlaybackState(playbackState)
    }

    private fun showMediaNotification(senderName: String, isPlaying: Boolean, thumbnail: Bitmap?) {
        val session = mediaSession ?: return

        val intent = Intent(context, ChatPersonActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Видеосообщение")
            .setContentText(senderName)
            .setSmallIcon(R.drawable.ic_camera)
            .setLargeIcon(thumbnail)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setOngoing(isPlaying)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    fun stopMediaSession() {
        try {
            mediaSession?.apply {
                isActive = false
                release()
            }
            mediaSession = null
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val CHANNEL_ID = "prime_video_note_playback"
        private const val NOTIFICATION_ID = 2025
    }
}
