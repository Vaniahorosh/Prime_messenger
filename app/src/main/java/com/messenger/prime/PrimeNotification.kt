package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import eightbitlab.com.blurview.BlurView as EightBitBlurView
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Кастомная система уведомлений ("островков"), заменяющая Toast,
 * а также централизованный менеджер системных push-уведомлений.
 */
object PrimeNotification {

    class MediaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "ACTION_PLAY_PAUSE" -> PrimeMusicManager.togglePlayPause(context)
                "ACTION_NEXT" -> PrimeMusicManager.playNextTrack(context)
                "ACTION_PREV" -> PrimeMusicManager.playPreviousTrack(context)
            }
        }
    }

    private const val DURATION = 3000L // 3 секунды
    private const val TAG = "PrimeNotification"

    private var activeNotificationRef: WeakReference<View>? = null
    private var activeTimerAnimator: ValueAnimator? = null

    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity?, message: String, onUndo: (() -> Unit)? = null) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            try {
                val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread

                // Снимаем предыдущее активное уведомление без наложения
                activeTimerAnimator?.cancel()
                activeTimerAnimator = null
                activeNotificationRef?.get()?.let { oldView ->
                    (oldView.parent as? ViewGroup)?.removeView(oldView)
                }
                activeNotificationRef = null

                val inflater = LayoutInflater.from(activity)
                val notificationView = inflater.inflate(R.layout.layout_prime_notification, rootLayout, false)

                val textView = notificationView.findViewById<TextView>(R.id.tvNotificationText)
                val layoutTimer = notificationView.findViewById<View>(R.id.layoutTimer)
                val pbTimer = notificationView.findViewById<ProgressBar>(R.id.pbTimer)
                val tsSeconds = notificationView.findViewById<TextSwitcher>(R.id.tsTimerSeconds)
                val btnUndo = notificationView.findViewById<ImageButton>(R.id.btnUndo)

                textView.text = message

                // --- НАСТРОЙКА BlurView ---
                val blurView = notificationView as? EightBitBlurView
                if (blurView != null) {
                    try {
                        val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        val overlayColor = if (isDark) {
                            Color.parseColor("#400F172A")
                        } else {
                            Color.parseColor("#40154B87")
                        }
                        val windowBg = activity.window?.decorView?.background
                        blurView.setupBlur(rootLayout, 16f, overlayColor, windowBg)
                    } catch (e: Exception) {
                        Log.w(TAG, "BlurView setup failed, falling back to static background", e)
                    }
                }

                // Настройка параметров отображения
                val density = activity.resources.displayMetrics.density
                val bottomContainer = activity.findViewById<View>(R.id.bottomContainer)
                val inputView = activity.findViewById<View>(R.id.layoutInput)

                val inputBottomOffset = if (bottomContainer != null && bottomContainer.visibility == View.VISIBLE) {
                    val containerParams = bottomContainer.layoutParams as? ViewGroup.MarginLayoutParams
                    val bottomMargin = containerParams?.bottomMargin ?: 0
                    val containerHeight = if (bottomContainer.height > 0) {
                        bottomContainer.height
                    } else if (inputView != null && inputView.visibility == View.VISIBLE) {
                        if (inputView.height > 0) inputView.height else (64 * density).toInt()
                    } else {
                        (68 * density).toInt()
                    }
                    containerHeight + bottomMargin + (12 * density).toInt()
                } else if (inputView != null && inputView.visibility == View.VISIBLE) {
                    val inputHeight = if (inputView.height > 0) inputView.height else (64 * density).toInt()
                    inputHeight + (16 * density).toInt()
                } else {
                    (30 * density).toInt()
                }

                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.BOTTOM
                params.bottomMargin = inputBottomOffset
                notificationView.layoutParams = params

                rootLayout.addView(notificationView)
                activeNotificationRef = WeakReference(notificationView)

                // Тактильный отклик
                try {
                    notificationView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Exception) {}

                // --- АНИМАЦИЯ ПОЯВЛЕНИЯ ---
                notificationView.alpha = 0f
                notificationView.translationY = 100f
                notificationView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                // --- ЛОГИКА ТАЙМЕРА ---
                val textColorPrimary = ContextCompat.getColor(activity, R.color.prime_text_primary)
                tsSeconds.setFactory {
                    TextView(activity).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        gravity = Gravity.CENTER
                        setTextColor(textColorPrimary)
                        textSize = 12f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                }

                var lastSecond = -1
                var isCancelled = false
                val timerAnimator = ValueAnimator.ofInt(1000, 0).apply {
                    duration = DURATION
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Int
                        pbTimer.progress = progress

                        val secondsLeft = ceil(progress.toDouble() * DURATION / 1000000.0).toInt().coerceAtLeast(1)
                        if (secondsLeft != lastSecond) {
                            lastSecond = secondsLeft
                            tsSeconds.setText(secondsLeft.toString())
                        }
                    }
                }
                activeTimerAnimator = timerAnimator

                timerAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isCancelled) {
                            dismiss(notificationView, 0f, 1f)
                        }
                    }
                })
                timerAnimator.start()

                // --- ЛОГИКА UNDO ---
                if (onUndo != null) {
                    layoutTimer.visibility = View.VISIBLE
                    btnUndo.visibility = View.VISIBLE
                    btnUndo.setOnClickListener {
                        isCancelled = true
                        timerAnimator.cancel()
                        try {
                            onUndo.invoke()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onUndo callback", e)
                        }
                        dismiss(notificationView, 1f, 0f)
                    }
                } else {
                    layoutTimer.visibility = View.GONE
                    btnUndo.visibility = View.GONE
                }

                // --- ЛОГИКА СВАЙПА ---
                var startX = 0f
                var startY = 0f
                var isDragging = false
                val screenWidth = activity.resources.displayMetrics.widthPixels.toFloat()

                notificationView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            false // Разрешаем обработку кликов дочерним элементам (например, btnUndo)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            if (abs(dx) > 10f || abs(dy) > 10f) {
                                isDragging = true
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                v.translationX = dx
                                v.translationY = if (dy > 0) dy else dy * 0.1f
                                v.alpha = (1f - (abs(dx) / (screenWidth * 0.8f))).coerceIn(0f, 1f)
                            }
                            isDragging
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isDragging) {
                                val dx = event.rawX - startX
                                val dy = event.rawY - startY

                                if (abs(dx) > screenWidth / 4 || dy > 150f) {
                                    isCancelled = true
                                    timerAnimator.cancel()
                                    dismiss(v, dx, dy)
                                } else {
                                    v.animate()
                                        .translationX(0f)
                                        .translationY(0f)
                                        .alpha(1f)
                                        .setDuration(200)
                                        .start()
                                }
                                isDragging = false
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show PrimeNotification", e)
            }
        }
    }

    private fun dismiss(view: View, directionX: Float, directionY: Float) {
        if (activeNotificationRef?.get() == view) {
            activeNotificationRef = null
        }
        val animator = view.animate()
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                val parent = view.parent as? ViewGroup
                parent?.removeView(view)
            }

        if (abs(directionX) > abs(directionY)) {
            animator.translationX(if (directionX >= 0) 800f else -800f)
        } else {
            animator.translationY(800f)
        }
        animator.start()
    }

    // =========================================================================
    //                    СИСТЕМНЫЕ СООБЩЕНИЯ (SYSTEM NOTIFICATIONS)
    // =========================================================================

    @JvmStatic
    @JvmOverloads
    fun postMessageNotification(
        context: Context?,
        senderName: String?,
        messageText: String?,
        deviceAddress: String? = null
    ) {
        if (context == null || senderName.isNullOrEmpty()) return

        // Подавляем системные уведомления, если активен чат с данным собеседником
        if (PrimeApplication.isChatActive(deviceAddress, senderName) ||
            ChatPersonActivity.isForegroundWithAddress(deviceAddress) ||
            ChatPersonActivity.isForegroundWithAddress(senderName)) {
            return
        }

        // Отображаемое имя контакта
        var displaySenderName = senderName
        if (!deviceAddress.isNullOrEmpty()) {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            var savedName = sp.getString("contact_name_$deviceAddress", null)
            if (savedName.isNullOrEmpty()) {
                savedName = sp.getString("${deviceAddress}_name", null)
            }
            if (!savedName.isNullOrEmpty() && !android.bluetooth.BluetoothAdapter.checkBluetoothAddress(savedName)) {
                displaySenderName = savedName
            }
        }
        if (displaySenderName.isNullOrEmpty() || android.bluetooth.BluetoothAdapter.checkBluetoothAddress(displaySenderName)) {
            displaySenderName = "Prime Собеседник"
        }

        // Если приложение открыто, показываем внутреннее snackbar-уведомление вместо системного
        if (PrimeApplication.isAppInForeground) {
            val act = PrimeApplication.getCurrentActivity()
            if (act != null) {
                act.runOnUiThread {
                    val preview = if (!messageText.isNullOrEmpty()) messageText else "Отправил(а) вложение"
                    show(act, "💬 $displaySenderName: $preview", null)
                }
            }
            return
        }

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Создаем/обновляем единый канал для сообщений
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    PrimeApplication.CHANNEL_MESSAGES_ID,
                    "Сообщения Prime",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о новых сообщениях и событиях чата"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    enableLights(true)
                    setShowBadge(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                nm.createNotificationChannel(channel)
            }

            // Выбираем целевую Activity (планшеты/фолды -> ChatListActivity, телефоны -> ChatPersonActivity)
            val isFoldableOrTablet = context.resources.configuration.smallestScreenWidthDp >= 600
            val targetActivityClass = if (isFoldableOrTablet) ChatListActivity::class.java else ChatPersonActivity::class.java

            val chatIntent = Intent(context, targetActivityClass).apply {
                putExtra("EXTRA_CHAT_NAME", senderName)
                if (!deviceAddress.isNullOrEmpty()) {
                    putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
                }
                putExtra("EXTRA_USE_EXISTING_SOCKET", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val keyStr = if (!deviceAddress.isNullOrEmpty()) deviceAddress else senderName
            val notificationId = 10000 + Math.abs(keyStr.hashCode() % 80000)

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                chatIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Построение Person и MessagingStyle
            val personBuilder = Person.Builder()
                .setName(displaySenderName)
                .setKey(keyStr)

            val avatarFile = getAvatarFileFor(context, senderName, deviceAddress)
            if (avatarFile != null && avatarFile.exists()) {
                try {
                    val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                    if (bmp != null) {
                        personBuilder.setIcon(IconCompat.createWithBitmap(bmp))
                    }
                } catch (_: Throwable) {}
            }
            val senderPerson = personBuilder.build()
            val mePerson = Person.Builder().setName("Вы").build()

            val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
                .setConversationTitle(displaySenderName)
                .addMessage(
                    if (!messageText.isNullOrEmpty()) messageText else "Новое сообщение",
                    System.currentTimeMillis(),
                    senderPerson
                )

            // 1. Прямой ответ (Direct Reply)
            val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
                .setLabel("Ответить...")
                .build()

            val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REPLY
                putExtra("EXTRA_CHAT_NAME", senderName)
                if (!deviceAddress.isNullOrEmpty()) {
                    putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
                }
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            }

            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                replyIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_undo,
                "Ответить",
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            // 2. Отметить как прочитанное
            val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra("EXTRA_CHAT_NAME", senderName)
                if (!deviceAddress.isNullOrEmpty()) {
                    putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
                }
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            }

            val markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                markReadIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val markReadAction = NotificationCompat.Action.Builder(
                R.drawable.ic_done_all,
                "Прочитано",
                markReadPendingIntent
            ).build()

            val builder = NotificationCompat.Builder(context, PrimeApplication.CHANNEL_MESSAGES_ID)
                .setSmallIcon(R.drawable.ic_prime_statusbar)
                .setStyle(messagingStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .addAction(replyAction)
                .addAction(markReadAction)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (avatarFile != null && avatarFile.exists()) {
                try {
                    val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                    if (bmp != null) {
                        builder.setLargeIcon(bmp)
                    }
                } catch (_: Throwable) {}
            }

            nm.notify(notificationId, builder.build())
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to post message notification", e)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun cancelNotificationForUser(context: Context?, senderName: String?, deviceAddress: String? = null) {
        if (context == null) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val keysToCancel = mutableSetOf<String>()
            if (!deviceAddress.isNullOrEmpty()) keysToCancel.add(deviceAddress)
            if (!senderName.isNullOrEmpty()) keysToCancel.add(senderName)

            for (key in keysToCancel) {
                val notificationId = 10000 + Math.abs(key.hashCode() % 80000)
                nm.cancel(notificationId)
                val rawId = Math.abs(key.hashCode())
                nm.cancel(rawId)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to cancel notification for user", e)
        }
    }

    private fun getAvatarFileFor(context: Context, senderName: String?, deviceAddress: String?): File? {
        val filesDir = context.filesDir ?: return null
        val candidates = arrayOf(
            if (deviceAddress != null) File(filesDir, "rec_avatar_${deviceAddress}.jpg") else null,
            if (deviceAddress != null) File(filesDir, "rec_avatar_${deviceAddress}.gif") else null,
            if (senderName != null) File(filesDir, "rec_avatar_${senderName}.jpg") else null,
            if (senderName != null) File(filesDir, "rec_avatar_${senderName}.gif") else null
        )
        for (f in candidates) {
            if (f != null && f.exists() && f.length() > 0) return f
        }
        return null
    }

    // =========================================================================
    //                    МУЗЫКАЛЬНЫЙ ПЛЕЕР (MEDIA NOTIFICATION)
    // =========================================================================

    private const val MUSIC_NOTIFICATION_ID = 2001
    private const val MUSIC_CHANNEL_ID = "prime_music_playback_channel"

    @JvmStatic
    fun createMediaNotification(context: Context, track: TrackItem, isPlaying: Boolean, sessionToken: MediaSessionCompat.Token?): android.app.Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_CHANNEL_ID,
                "Воспроизведение музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление плеера Prime Messenger"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getBroadcast(
            context, 1, Intent("ACTION_PLAY_PAUSE").setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getBroadcast(
            context, 2, Intent("ACTION_PREV").setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getBroadcast(
            context, 3, Intent("ACTION_NEXT").setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        val builder = NotificationCompat.Builder(context, MUSIC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.durationStr)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingOpenIntent)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
            .addAction(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play, "Play/Pause", playPauseIntent)
            .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
            .setStyle(mediaStyle)

        if (!track.coverPath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(track.coverPath)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
            } catch (_: Exception) {}
        }

        return builder.build()
    }
}
