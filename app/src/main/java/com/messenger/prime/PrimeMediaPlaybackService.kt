package com.messenger.prime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class PrimeMediaPlaybackService : Service() {

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val CHANNEL_ID = "prime_voice_playback_channel"
        private const val NOTIFICATION_ID = 2004

        private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
        val playbackState: StateFlow<PlaybackState> = _playbackState
        
        var currentVoiceMessageId: String? = null

        @JvmStatic
        fun observePlaybackState(
            lifecycleOwner: androidx.lifecycle.LifecycleOwner,
            listener: PlaybackStateListener
        ) {
            lifecycleOwner.lifecycleScope.launch {
                playbackState.collect { state ->
                    listener.onStateChanged(currentVoiceMessageId, state)
                }
            }
        }
    }

    fun interface PlaybackStateListener {
        fun onStateChanged(msgId: String?, state: PlaybackState)
    }

    sealed class PlaybackState {
        object Idle : PlaybackState()
        data class Playing(val progress: Float, val durationMs: Long) : PlaybackState()
        data class Paused(val progress: Float, val durationMs: Long) : PlaybackState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startProgressLoop()
                    } else {
                        stopProgressLoop()
                        val dur = duration.coerceAtLeast(0L)
                        val pos = currentPosition.coerceAtLeast(0L)
                        val prog = if (dur > 0) pos.toFloat() / dur else 0f
                        _playbackState.value = PlaybackState.Paused(prog, dur)
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        stopVoicePlayback()
                    }
                }
            })
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val p = player
                if (p != null && p.isPlaying) {
                    val dur = p.duration.coerceAtLeast(0L)
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val prog = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                    _playbackState.value = PlaybackState.Playing(prog, dur)
                }
                delay(50)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "PLAY_VOICE" -> {
                val path = intent.getStringExtra("EXTRA_PATH")
                val author = intent.getStringExtra("EXTRA_AUTHOR") ?: "Prime"
                val msgId = intent.getStringExtra("EXTRA_MSG_ID")
                val initialSeekProgress = intent.getFloatExtra("EXTRA_SEEK_PROGRESS", -1f)

                if (path != null) {
                    currentVoiceMessageId = msgId
                    
                    val metadata = MediaMetadata.Builder()
                        .setTitle(author)
                        .setSubtitle("Голосовое сообщение")
                        .build()
                        
                    val mediaItem = MediaItem.Builder()
                        .setUri(path)
                        .setMediaMetadata(metadata)
                        .build()
                        
                    player?.let { p ->
                        p.setMediaItem(mediaItem)
                        p.prepare()
                        if (initialSeekProgress in 0f..1f && p.duration > 0) {
                            p.seekTo((initialSeekProgress * p.duration).toLong())
                        }
                        p.play()
                    }
                    startForegroundNotification(author)
                }
            }
            "PAUSE_VOICE" -> {
                player?.pause()
            }
            "RESUME_VOICE" -> {
                player?.play()
            }
            "TOGGLE_VOICE" -> {
                player?.let { p ->
                    if (p.isPlaying) p.pause() else p.play()
                }
            }
            "SEEK_VOICE" -> {
                val prog = intent.getFloatExtra("EXTRA_PROGRESS", -1f)
                player?.let { p ->
                    if (prog in 0f..1f && p.duration > 0) {
                        p.seekTo((prog * p.duration).toLong())
                    }
                }
            }
            "STOP_VOICE" -> {
                stopVoicePlayback()
            }
        }
        return START_STICKY
    }

    private fun stopVoicePlayback() {
        stopProgressLoop()
        player?.stop()
        currentVoiceMessageId = null
        _playbackState.value = PlaybackState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startForegroundNotification(author: String) {
        val intent = Intent(this, ChatPersonActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PrimeMediaPlaybackService::class.java).apply {
            action = "STOP_VOICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Голосовое сообщение от $author")
            .setContentText("Воспроизведение Prime Messenger")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_cancel, "Остановить", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Воспроизведение голосовых сообщений",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновое воспроизведение голосовых сообщений Prime"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopProgressLoop()
        player?.release()
        player = null
        super.onDestroy()
    }
}