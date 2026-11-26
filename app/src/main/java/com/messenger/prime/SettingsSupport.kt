package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.graphics.shapes.RoundedPolygon
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.messenger.prime.shapes.M3EDrawable
import com.messenger.prime.shapes.M3EShapes
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

object SettingsSupport {

    fun isDarkThemeActive(context: Context): Boolean {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        return when (sp.getString("app_theme", "system")) {
            "dark", "teto_dark" -> true
            "light", "teto_light" -> false
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun getThemeDisplayName(context: Context, theme: String?): String = when (theme) {
        "light" -> context.getString(R.string.settings_theme_light)
        "dark" -> context.getString(R.string.settings_theme_dark)
        "teto_light" -> context.getString(R.string.settings_theme_teto_light)
        "teto_dark" -> context.getString(R.string.settings_theme_teto_dark)
        else -> context.getString(R.string.settings_theme_system)
    }

    fun getThemeDisplayName(theme: String?): String {
        val ctx = PrimeApplication.appContext
        if (ctx != null) return getThemeDisplayName(PrimeLocaleManager.getLocalizedContext(ctx), theme)
        return when (theme) {
            "light" -> "Светлая"
            "dark" -> "Тёмная"
            "teto_light" -> "Тето (Светлая)"
            "teto_dark" -> "Тето (Тёмная)"
            else -> "Системная"
        }
    }

    fun formatStorageSize(bytes: Long): String {
        val ctx = PrimeApplication.appContext
        val lang = ctx?.let { PrimeLocaleManager.getEffectiveLanguageCode(it) } ?: Locale.getDefault().language
        val locale = Locale.forLanguageTag(lang)
        val isEn = lang.startsWith("en", ignoreCase = true)
        val units = if (isEn) arrayOf("B", "KB", "MB", "GB") else arrayOf("Б", "КБ", "МБ", "ГБ")
        if (bytes <= 0L) return "0 ${units[0]}"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(locale, "%.1f %s", kb, units[1])
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(locale, "%.1f %s", mb, units[2])
        val gb = mb / 1024.0
        return String.format(locale, "%.2f %s", gb, units[3])
    }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, HiActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity.startActivity(intent)
        PrimeTransitions.applyOpenTransition(activity)
        activity.finishAffinity()
    }

    fun sendProfileUpdateOverBluetooth(context: Context) {
        try {
            val myDisplayName = AvatarManager.getMyDisplayName(context)
            val localAvatarUri = AvatarManager.getMyAvatarUri(context)
            val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""

            val handshake =
                "HANDSHAKE:login=$currentUser;name=$myDisplayName;avatar=${localAvatarUri ?: ""};version=${ChatPersonActivity.getAppVersionCode(context)}"
            BluetoothConnectionManager.getInstance()
                .broadcastPacket(0x01.toByte(), handshake.toByteArray(Charsets.UTF_8))

            if (!localAvatarUri.isNullOrEmpty()) {
                try {
                    val uri = if (
                        localAvatarUri.startsWith("content://") ||
                        localAvatarUri.startsWith("file://") ||
                        localAvatarUri.startsWith("android.resource://") ||
                        localAvatarUri.startsWith("http")
                    ) {
                        Uri.parse(localAvatarUri)
                    } else {
                        Uri.fromFile(File(localAvatarUri))
                    }
                    context.contentResolver.openInputStream(uri)?.use { isStream ->
                        val bitmap = BitmapFactory.decodeStream(isStream)
                        if (bitmap != null) {
                            val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, false)
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                            BluetoothConnectionManager.getInstance()
                                .broadcastPacket(0x07.toByte(), baos.toByteArray())
                            if (scaled != bitmap) scaled.recycle()
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                BluetoothConnectionManager.getInstance().broadcastPacket(0x07.toByte(), byteArrayOf())
            }

            val localCoverUri = sharedPrefs.getString("${currentUser}_cover", null)?.takeIf { it.isNotBlank() }
                ?: sharedPrefs.getString("my_profile_cover", null)
            if (!localCoverUri.isNullOrEmpty()) {
                try {
                    val coverUri = if (
                        localCoverUri.startsWith("content://") ||
                        localCoverUri.startsWith("file://") ||
                        localCoverUri.startsWith("android.resource://") ||
                        localCoverUri.startsWith("http")
                    ) {
                        Uri.parse(localCoverUri)
                    } else {
                        Uri.fromFile(File(localCoverUri))
                    }
                    context.contentResolver.openInputStream(coverUri)?.use { isStream ->
                        val bitmap = BitmapFactory.decodeStream(isStream)
                        if (bitmap != null) {
                            val maxDim = 640
                            val w = bitmap.width
                            val h = bitmap.height
                            val scaled = if (w > maxDim || h > maxDim) {
                                val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
                                Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
                            } else {
                                bitmap
                            }
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
                            BluetoothConnectionManager.getInstance()
                                .broadcastPacket(0x11.toByte(), baos.toByteArray())
                            if (scaled != bitmap) scaled.recycle()
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                BluetoothConnectionManager.getInstance().broadcastPacket(0x11.toByte(), byteArrayOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applySegmentedGroupShape(
        card: MaterialCardView,
        index: Int,
        totalCount: Int,
        outerRadiusDp: Float = 24f,
        innerRadiusDp: Float = 10f
    ) {
        val density = card.resources.displayMetrics.density
        val outer = outerRadiusDp * density
        val inner = innerRadiusDp * density
        val top = if (index == 0) outer else inner
        val bottom = if (index == totalCount - 1) outer else inner

        card.strokeWidth = 0
        card.cardElevation = 0f
        card.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, top)
            .setTopRightCorner(CornerFamily.ROUNDED, top)
            .setBottomLeftCorner(CornerFamily.ROUNDED, bottom)
            .setBottomRightCorner(CornerFamily.ROUNDED, bottom)
            .build()
    }

    fun applyM3EBadgeIcon(
        iconView: ImageView?,
        accentColor: Int,
        shape: RoundedPolygon = M3EShapes.cookie6Sided,
        isNight: Boolean
    ) {
        if (iconView == null) return
        val bgAlpha = if (isNight) 58 else 32
        val bgColor = ColorUtils.setAlphaComponent(accentColor, bgAlpha)
        val paddingPx = (8 * iconView.resources.displayMetrics.density).toInt()
        iconView.tag = "do_not_tint"
        iconView.imageTintList = ColorStateList.valueOf(accentColor)
        iconView.background = M3EDrawable(shape = shape, color = bgColor)
        iconView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    }

    fun animateStaggeredEntrance(context: Context, views: List<View>) {
        if (views.isEmpty()) return
        if (PrimeTransitions.areAnimationsDisabled(context)) {
            views.forEach {
                it.alpha = 1f
                it.translationY = 0f
            }
            return
        }
        val density = context.resources.displayMetrics.density
        val offsetPx = 12f * density
        val interpolator = FastOutSlowInInterpolator()

        views.forEachIndexed { index, view ->
            view.animate().cancel()
            view.alpha = 0f
            view.translationY = offsetPx
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .setStartDelay((index * 28L).coerceAtMost(210L))
                .setInterpolator(interpolator)
                .start()
        }
    }
}
