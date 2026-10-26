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
import com.google.android.material.color.DynamicColors
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

class PrimeApplication : Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        appContext = this.applicationContext
        PrimeTransitions.applyGlobalAnimationScale(this)
        registerActivityLifecycleCallbacks(this)
        sanitizeLocalUserProfile(this)
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getString("app_theme", "system")
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        if (ColorAccentManager.getAccentType(this) == ColorAccentManager.ACCENT_TYPE_SYSTEM) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        createNotificationChannels()
        
        try {
            androidx.window.embedding.RuleController.getInstance(this)
                .setRules(androidx.window.embedding.RuleController.parseRules(this, R.xml.main_split_config))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        var appContext: Context? = null
            private set

        @JvmStatic
        var isAppInForeground: Boolean = false
            private set
            
        @JvmStatic
        private var currentActivityRef: WeakReference<Activity>? = null
        
        @JvmField
        var currentActiveChatAddress: String? = null
        
        @JvmField
        var currentActiveChatName: String? = null
        
        @JvmStatic
        @JvmOverloads
        fun isChatActive(address: String?, name: String? = null): Boolean {
            if (!isAppInForeground) return false
            val activeAddr = currentActiveChatAddress
            val activeName = currentActiveChatName
            if (activeAddr.isNullOrEmpty() && activeName.isNullOrEmpty()) return false

            val matchAddr = !address.isNullOrEmpty() && (
                activeAddr?.equals(address, ignoreCase = true) == true ||
                activeName?.equals(address, ignoreCase = true) == true
            )
            val matchName = !name.isNullOrEmpty() && (
                activeAddr?.equals(name, ignoreCase = true) == true ||
                activeName?.equals(name, ignoreCase = true) == true
            )

            return matchAddr || matchName
        }
        
        @JvmStatic
        fun getCurrentActivity(): Activity? = currentActivityRef?.get()

        @JvmStatic
        fun getPreviousActivity(): Activity? {
            if (activityStack.size >= 2) {
                return activityStack[activityStack.size - 2].get()
            }
            return null
        }
        
        val activityStack = mutableListOf<WeakReference<Activity>>()
    }
    
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityStack.add(WeakReference(activity))
        ColorAccentManager.applyAccentToActivity(activity)
        PrimeTransitions.setupActivityTransitions(activity)
        val isExcluded = activity is PhotoEditorActivity || 
                         activity is WallpaperEditorActivity || 
                         activity is MediaPlayerActivity
        
        if (!isExcluded) {
            val isBottomSheet = PrimePredictiveBack.isBottomSheetOrDialog(activity)
            val isRootActivity = activity.isTaskRoot || getPreviousActivity() == null
            
            if (activity is androidx.activity.ComponentActivity && !isBottomSheet && !isRootActivity) {
                // Attach predictive back & touch swiper, and play Enter transition for new instances
                PrimePredictiveBack.attach(activity, playEnterTransition = savedInstanceState == null)
            }
            
            // Globally disable standard window transitions ONLY for child activities so custom Enter/Exit swiper works.
            // For root activities exiting to Home, do NOT override transitions so System Predictive Back to Home gesture works.
            if (!isBottomSheet && !isRootActivity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    activity.overridePendingTransition(0, 0)
                }
            }
        } else if (PrimePredictiveBack.isBottomSheetOrDialog(activity)) {
            // Apply bottom slide up explicitly for excluded modal activities (e.g. MediaPlayerActivity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_bottom, R.anim.fade_out_hold)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.fade_out_hold, R.anim.slide_out_bottom)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out_hold)
            }
        }
    }
    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            isAppInForeground = true
        }
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        // Ensure it's at the top of the stack
        activityStack.removeAll { it.get() == null || it.get() == activity }
        activityStack.add(WeakReference(activity))
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
    override fun onActivityDestroyed(activity: Activity) {
        activityStack.removeAll { it.get() == null || it.get() == activity }
    }
}
