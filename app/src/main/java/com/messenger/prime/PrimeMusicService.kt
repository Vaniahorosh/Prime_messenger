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
        
        if (track != null) {
            val notification = PrimeNotification.createMediaNotification(this, track, isPlaying, PrimeMusicManager.mediaSession?.sessionToken)
            startForeground(2001, notification)
        }
        
        return START_NOT_STICKY
    }
}
