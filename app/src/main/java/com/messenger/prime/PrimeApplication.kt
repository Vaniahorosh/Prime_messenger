package com.messenger.prime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import android.app.Activity
import android.os.Bundle
import java.lang.ref.WeakReference

class PrimeApplication : Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getString("app_theme", "system")
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Канал сообщений
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Сообщения Prime",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых сообщениях и событиях чата"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }

            // Канал фонового Bluetooth сервиса
            val btServiceChannel = NotificationChannel(
                PrimeBluetoothService.CHANNEL_ID,
                "Служба Bluetooth Prime",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о работе фоновой службы соединения"
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(btServiceChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES_ID = "prime_messages"
        
        @JvmStatic
        var isAppInForeground: Boolean = false
            private set
            
        @JvmStatic
        private var currentActivityRef: WeakReference<Activity>? = null
        
        @JvmStatic
        fun getCurrentActivity(): Activity? = currentActivityRef?.get()
    }
    
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            isAppInForeground = true
        }
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }
    override fun onActivityPaused(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef?.clear()
            currentActivityRef = null
        }
    }
    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            isAppInForeground = false
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
