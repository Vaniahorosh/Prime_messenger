package com.messenger.prime

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PrimeMusicService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val track = PrimeMusicManager.getCurrentTrack()
        val isPlaying = PrimeMusicManager.isPlaying()

        val notification = if (track != null) {
            PrimeNotification.createMediaNotification(this, track, isPlaying, PrimeMusicManager.mediaSession?.sessionToken)
        } else {
            PrimeNotification.createMediaNotification(
                this,
                TrackItem("dummy", "Prime Music", "Воспроизведение", "", 0L, "00:00"),
                isPlaying,
                PrimeMusicManager.mediaSession?.sessionToken
            )
        }

        // ALWAYS call startForeground IMMEDIATELY to prevent ForegroundServiceStartNotAllowed / startForeground timeout crash!
        try {
            startForeground(2001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (track == null && !isPlaying) {
            stopSelf()
        }

        return START_NOT_STICKY
    }
}
