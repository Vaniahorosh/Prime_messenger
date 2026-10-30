package com.messenger.prime

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.util.Locale

object AvatarManager {

    @Volatile private var cachedRadiusPercent: Int? = null
    private val avatarFileExistenceCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    @JvmStatic
    fun clearAvatarRadiusCache() {
        cachedRadiusPercent = null
    }

    @JvmStatic
    fun getCachedRadiusPercent(context: Context): Int {
        cachedRadiusPercent?.let { return it }
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val percent = sp.getInt("settings_avatar_radius_percent", 50)
        cachedRadiusPercent = percent
        return percent
    }

    @JvmStatic
    fun checkFileExistsCached(file: File): Boolean {
        val exists = file.exists() && file.length() > 0
        if (exists) {
            avatarFileExistenceCache[file.absolutePath] = true
        } else {
            avatarFileExistenceCache.remove(file.absolutePath)
        }
        return exists
    }

    @JvmStatic
    @JvmOverloads
    fun invalidateAvatarFileCache(file: File? = null) {
        if (file != null) {
            avatarFileExistenceCache.remove(file.absolutePath)
        } else {
            avatarFileExistenceCache.clear()
        }
    }

    @JvmStatic
    fun isGenericIdentity(identity: String?): Boolean {
        if (identity == null) return true
        val trimmed = identity.trim().lowercase(Locale.US)
        return trimmed.isEmpty() ||
                trimmed == "собеседник" ||
                trimmed == "prime собеседник" ||
                trimmed == "пользователь" ||
                trimmed == "prime user" ||
                trimmed == "контакт" ||
                trimmed == "1" ||
                trimmed == "null"
    }

    @JvmStatic
    fun isLocalUserIdentity(context: Context, identity: String?): Boolean {
        if (identity.isNullOrBlank()) return true
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val myDisplayName = sharedPrefs.getString("${currentUser}_name", currentUser) ?: currentUser

        val trimmed = identity.trim()
        return trimmed.equals(currentUser, ignoreCase = true) ||
                trimmed.equals(myDisplayName, ignoreCase = true) ||
                trimmed.equals("my", ignoreCase = true) ||
                trimmed.equals("me", ignoreCase = true) ||
                isGenericIdentity(trimmed)
    }

    @JvmStatic
    fun isProtectedIdentity(context: Context, identity: String?): Boolean {
        return isLocalUserIdentity(context, identity) || isGenericIdentity(identity)
    }

    @JvmStatic
    fun getMyDisplayName(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        if (currentUser.isBlank()) return "Мой профиль"
        val savedName = sharedPrefs.getString("${currentUser}_name", null)
            ?: sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
        return if (!savedName.isNullOrBlank()) savedName else currentUser
    }

    @JvmStatic
    fun getMyAvatarUri(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        if (currentUser.isBlank()) return null
        val candidates = listOfNotNull(
            sharedPrefs.getString("${currentUser}_avatar", null),
            sharedPrefs.getString("${currentUser}_avatarUri", null)
        )

        for (candidate in candidates) {
            if (candidate.isNotBlank()) {
                val (_, file) = parseAvatarModelAndFile(candidate)
                if (file == null || file.exists()) {
                    return candidate
                }
            }
        }

        val history = AvatarHistoryManager.getMyAvatarHistory(context)
        if (history.isNotEmpty()) {
            return history.first()
        }

        val possibleFiles = arrayOf(
            File(context.filesDir, "avatar_$currentUser.gif"),
            File(context.filesDir, "avatar_$currentUser.jpg")
        )
        for (f in possibleFiles) {
            if (f.exists() && f.length() > 0) {
                return Uri.fromFile(f).toString()
            }
        }
        return null
    }

    @JvmStatic
    fun getAvatarUrl(contactId: String): String {
        // Normalise the contact identifier to avoid duplicate avatars
        val normId = ContactHelper.normalizeKey(contactId)
        return "https://example.com/avatar/" + normId
    }

    @JvmStatic
    fun getContactDisplayName(
        context: Context,
        contactId: String?,
        deviceAddress: String? = null,
        fallbackName: String? = null
    ): String {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val keys = mutableListOf<String>()

        if (currentUser.isNotEmpty()) {
            if (!deviceAddress.isNullOrBlank()) keys.add("contact_name_${currentUser}_$deviceAddress")
            if (!contactId.isNullOrBlank()) keys.add("contact_name_${currentUser}_$contactId")
            if (!fallbackName.isNullOrBlank()) keys.add("contact_name_${currentUser}_$fallbackName")
        } else {
            if (!deviceAddress.isNullOrBlank()) {
                keys.add("contact_name_$deviceAddress")
                keys.add("${deviceAddress}_name")
            }
            if (!contactId.isNullOrBlank()) {
                keys.add("contact_name_$contactId")
                keys.add("${contactId}_name")
            }
            if (!fallbackName.isNullOrBlank()) {
                keys.add("contact_name_$fallbackName")
                keys.add("${fallbackName}_name")
            }
        }

        for (key in keys) {
            val saved = sharedPrefs.getString(key, null)
            if (!saved.isNullOrBlank() && saved != "1" && saved != "null") {
                if (DeviceAnonymizer.isMacAddress(saved)) {
                    return DeviceAnonymizer.getAnonymizedName(null, deviceAddress ?: contactId)
                }
                return DeviceAnonymizer.sanitizeDisplayName(saved, deviceAddress ?: contactId)
            }
        }

        val nameCandidate = when {
            !fallbackName.isNullOrBlank() -> fallbackName
            !contactId.isNullOrBlank() -> contactId
            else -> "Собеседник"
        }
        if (DeviceAnonymizer.isMacAddress(nameCandidate)) {
            return DeviceAnonymizer.getAnonymizedName(null, deviceAddress ?: contactId)
        }
        return DeviceAnonymizer.sanitizeDisplayName(nameCandidate, deviceAddress ?: contactId)
    }

    @JvmStatic
    fun getContactAvatarUriOrFile(
        context: Context,
        contactId: String?,
        contactName: String? = null,
        deviceAddress: String? = null
    ): String? {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val keys = mutableListOf<String>()

        if (currentUser.isNotEmpty()) {
            if (!deviceAddress.isNullOrBlank()) keys.add("contact_avatar_${currentUser}_$deviceAddress")
            if (!contactId.isNullOrBlank()) keys.add("contact_avatar_${currentUser}_$contactId")
            if (!contactName.isNullOrBlank()) keys.add("contact_avatar_${currentUser}_$contactName")
        }
        if (!deviceAddress.isNullOrBlank()) {
            keys.add("contact_avatar_$deviceAddress")
            keys.add("${deviceAddress}_avatarUri")
            keys.add("${deviceAddress}_avatar")
        }
        if (!contactId.isNullOrBlank()) {
            keys.add("contact_avatar_$contactId")
            keys.add("${contactId}_avatarUri")
            keys.add("${contactId}_avatar")
        }
        if (!contactName.isNullOrBlank()) {
            keys.add("contact_avatar_$contactName")
            keys.add("${contactName}_avatarUri")
            keys.add("${contactName}_avatar")
        }

        for (key in keys) {
            val valStr = sharedPrefs.getString(key, null)
            if (!valStr.isNullOrBlank()) {
                val (_, file) = parseAvatarModelAndFile(valStr)
                if (file == null || file.exists()) {
                    return valStr
                }
            }
        }

        val filesDir = context.filesDir
        val fileCandidates = mutableListOf<File>()

        if (currentUser.isNotEmpty()) {
            if (!deviceAddress.isNullOrBlank() && !isProtectedIdentity(context, deviceAddress)) {
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$deviceAddress.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$deviceAddress.jpg"))
            }
            if (!contactId.isNullOrBlank() && !isProtectedIdentity(context, contactId)) {
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactId.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactId.jpg"))
            }
            if (!contactName.isNullOrBlank() && !isProtectedIdentity(context, contactName)) {
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactName.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactName.jpg"))
            }
        } else {
            if (!deviceAddress.isNullOrBlank() && !isProtectedIdentity(context, deviceAddress)) {
                fileCandidates.add(File(filesDir, "rec_avatar_$deviceAddress.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_$deviceAddress.jpg"))
            }
            if (!contactId.isNullOrBlank() && !isProtectedIdentity(context, contactId)) {
                fileCandidates.add(File(filesDir, "rec_avatar_$contactId.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_$contactId.jpg"))
            }
            if (!contactName.isNullOrBlank() && !isProtectedIdentity(context, contactName)) {
                fileCandidates.add(File(filesDir, "rec_avatar_$contactName.gif"))
                fileCandidates.add(File(filesDir, "rec_avatar_$contactName.jpg"))
            }
        }

        val foundFile = fileCandidates.firstOrNull { it.exists() && it.length() > 0 }
        if (foundFile != null) {
            return Uri.fromFile(foundFile).toString()
        }
        return null
    }

    @JvmStatic
    fun clearContactAvatar(
        context: Context,
        contactId: String?,
        contactName: String? = null,
        deviceAddress: String? = null
    ) {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val editor = sharedPrefs.edit()

        val keysToRemove = mutableListOf<String>()
        val targets = listOfNotNull(contactId, contactName, deviceAddress).filter { it.isNotBlank() }

        targets.forEach { target ->
            keysToRemove.add("contact_avatar_$target")
            keysToRemove.add("${target}_avatar")
            keysToRemove.add("${target}_avatarUri")
            keysToRemove.add("contact_avatar_history_$target")
            if (currentUser.isNotEmpty()) {
                keysToRemove.add("contact_avatar_${currentUser}_$target")
                keysToRemove.add("contact_avatar_history_${currentUser}_$target")
                val userGif = File(context.filesDir, "rec_avatar_${currentUser}_$target.gif")
                val userJpg = File(context.filesDir, "rec_avatar_${currentUser}_$target.jpg")
                if (userGif.exists()) try { userGif.delete() } catch (_: Exception) {}
                if (userJpg.exists()) try { userJpg.delete() } catch (_: Exception) {}
            }

            val gifFile = File(context.filesDir, "rec_avatar_$target.gif")
            val jpgFile = File(context.filesDir, "rec_avatar_$target.jpg")
            if (gifFile.exists()) try { gifFile.delete() } catch (_: Exception) {}
            if (jpgFile.exists()) try { jpgFile.delete() } catch (_: Exception) {}
        }

        keysToRemove.forEach { key -> editor.remove(key) }
        editor.apply()
        invalidateAvatarFileCache()
    }

    @JvmStatic
    fun clearMyAvatar(context: Context) {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        val editor = sharedPrefs.edit()
            .remove("my_avatar")
            .remove("my_local_avatar")
            .remove("my_avatar_uri")
            .remove("my_avatar_history")

        if (currentUser.isNotEmpty()) {
            editor.remove("${currentUser}_avatar")
                .remove("${currentUser}_avatarUri")
                .remove("my_avatar_history_$currentUser")

            val gifFile = File(context.filesDir, "avatar_$currentUser.gif")
            val jpgFile = File(context.filesDir, "avatar_$currentUser.jpg")
            if (gifFile.exists()) try { gifFile.delete() } catch (_: Exception) {}
            if (jpgFile.exists()) try { jpgFile.delete() } catch (_: Exception) {}
        }
        editor.apply()
    }

    @JvmStatic
    fun syncActiveUserProfile(
        context: Context,
        login: String,
        name: String,
        avatarUriStr: String? = null
    ) {
        if (login.isBlank()) return
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        editor.putBoolean("is_logged_in", true)
        editor.putString("current_user", login)

        if (name.isNotBlank()) {
            editor.putString("${login}_name", name)
            editor.putString("my_name", name)
            editor.putString("my_local_name", name)
            editor.putString("current_user_name", name)
        }

        val resolvedAvatar = if (!avatarUriStr.isNullOrBlank()) {
            avatarUriStr
        } else {
            sharedPrefs.getString("${login}_avatar", null)
                ?: sharedPrefs.getString("${login}_avatarUri", null)
        }

        if (!resolvedAvatar.isNullOrBlank()) {
            editor.putString("${login}_avatar", resolvedAvatar)
            editor.putString("${login}_avatarUri", resolvedAvatar)
            AvatarHistoryManager.addMyAvatar(context, resolvedAvatar)
        }

        editor.apply()

        try {
            context.sendBroadcast(android.content.Intent("com.messenger.prime.NAME_CHANGED").setPackage(context.packageName))
            context.sendBroadcast(android.content.Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(context.packageName))
        } catch (_: Exception) {}
    }

    @JvmStatic
    fun getContactAvatarFile(context: Context, contactId: String?, contactName: String?): File? {
        val filesDir = context.filesDir
        val candidates = mutableListOf<File>()
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        if (currentUser.isNotEmpty()) {
            if (!contactId.isNullOrBlank() && !isProtectedIdentity(context, contactId)) {
                candidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactId.gif"))
                candidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactId.jpg"))
            }
            if (!contactName.isNullOrBlank() && !isProtectedIdentity(context, contactName)) {
                candidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactName.gif"))
                candidates.add(File(filesDir, "rec_avatar_${currentUser}_$contactName.jpg"))
            }
        }

        if (!contactId.isNullOrBlank() && !isProtectedIdentity(context, contactId)) {
            candidates.add(File(filesDir, "rec_avatar_$contactId.gif"))
            candidates.add(File(filesDir, "rec_avatar_$contactId.jpg"))
        }
        if (!contactName.isNullOrBlank() && !isProtectedIdentity(context, contactName)) {
            candidates.add(File(filesDir, "rec_avatar_$contactName.gif"))
            candidates.add(File(filesDir, "rec_avatar_$contactName.jpg"))
        }

        return candidates.firstOrNull { checkFileExistsCached(it) }
    }

    @JvmStatic
    fun saveContactPreferenceSafely(context: Context, contactKey: String?, prefKeySuffix: String, value: String?) {
        if (contactKey.isNullOrBlank() || isProtectedIdentity(context, contactKey)) {
            return
        }
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val editor = sharedPrefs.edit()
        if (currentUser.isNotEmpty()) {
            editor.putString("${prefKeySuffix}_${currentUser}_$contactKey", value)
        }
        editor.putString("${prefKeySuffix}_$contactKey", value)
        editor.apply()
        invalidateAvatarFileCache()
    }

    @JvmStatic
    fun getContactPreferenceSafely(context: Context, contactKey: String?, prefKeySuffix: String): String? {
        if (contactKey.isNullOrBlank()) return null
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        if (currentUser.isNotEmpty()) {
            val userSpecific = sharedPrefs.getString("${prefKeySuffix}_${currentUser}_$contactKey", null)
            if (userSpecific != null) return userSpecific
        }
        return sharedPrefs.getString("${prefKeySuffix}_$contactKey", null)
    }

    @JvmStatic
    fun isDesktopContact(context: Context, contactId: String?, deviceAddress: String? = null): Boolean {
        if (!contactId.isNullOrBlank()) {
            val pref = getContactPreferenceSafely(context, contactId, "is_desktop")
            if ("true".equals(pref, ignoreCase = true)) return true
        }
        if (!deviceAddress.isNullOrBlank()) {
            val pref = getContactPreferenceSafely(context, deviceAddress, "is_desktop")
            if ("true".equals(pref, ignoreCase = true)) return true
        }
        return false
    }

    @JvmStatic
    @JvmOverloads
    fun formatNameWithDesktopIcon(
        context: Context,
        rawName: String?,
        isDesktop: Boolean,
        tintColor: Int = ColorAccentManager.getCurrentAccentColor(context)
    ): CharSequence {
        val nameToUse = if (rawName.isNullOrBlank()) "Собеседник" else rawName
        val cleanName = nameToUse.replace("💻", "").trim()
        if (!isDesktop) return cleanName

        val ssb = SpannableStringBuilder(cleanName)
        ssb.append("  ") // space for vector icon
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pc_monitor)?.mutate()
        if (drawable != null) {
            val size = (16 * context.resources.displayMetrics.density).toInt()
            drawable.setBounds(0, 0, size, size)
            DrawableCompat.setTint(drawable, tintColor)
            val align = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ImageSpan.ALIGN_CENTER
            } else {
                ImageSpan.ALIGN_BASELINE
            }
            val imageSpan = ImageSpan(drawable, align)
            ssb.setSpan(imageSpan, ssb.length - 1, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return ssb
    }
}

fun getAvatarCornerRadiusPx(context: Context, viewSizePx: Int = (48 * context.resources.displayMetrics.density).toInt()): Int {
    val percent = AvatarManager.getCachedRadiusPercent(context)
    val density = context.resources.displayMetrics.density
    val minRadiusPx = 4f * density
    val maxRadiusPx = viewSizePx / 2f
    return (minRadiusPx + (maxRadiusPx - minRadiusPx) * (percent / 100f)).toInt()
}

fun loadAvatarIntoView(
    context: Context,
    avatarSource: Any?,
    imageView: ImageView,
    fallbackName: String = ""
) {
    try {
        Glide.with(context).clear(imageView)
    } catch (_: Exception) {}

    val isExcludedActivity = context is PersonInformationActivity ||
        (context is ContextWrapper && context.baseContext is PersonInformationActivity)

    val targetWidth = imageView.layoutParams?.width?.coerceAtLeast(0) ?: 0
    val viewSizePx = if (targetWidth > 0) targetWidth else (48 * context.resources.displayMetrics.density).toInt()

    val radiusPx = if (isExcludedActivity) {
        (14 * context.resources.displayMetrics.density).toInt()
    } else {
        getAvatarCornerRadiusPx(context, viewSizePx)
    }

    if (imageView is com.google.android.material.imageview.ShapeableImageView) {
        imageView.shapeAppearanceModel = imageView.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(radiusPx.toFloat())
            .build()
    }

    var loaded = false
    val (model, file) = when (avatarSource) {
        is String -> parseAvatarModelAndFile(avatarSource)
        is Uri -> Pair(avatarSource, if (avatarSource.scheme == "file" && avatarSource.path != null) File(avatarSource.path!!) else null)
        is File -> Pair(avatarSource, avatarSource)
        else -> Pair(avatarSource, null)
    }

    if (model != null) {
        if (file == null || AvatarManager.checkFileExistsCached(file)) {
            val sourceStr = avatarSource.toString().lowercase(Locale.US)
            val isGif = sourceStr.endsWith(".gif") || sourceStr.contains("gif")
            val signatureKey = ObjectKey(if (file != null && AvatarManager.checkFileExistsCached(file)) "${file.absolutePath}_${file.lastModified()}" else avatarSource.toString())
            val isCircle = radiusPx >= (viewSizePx / 2 - 2)

            try {
                if (isGif) {
                    Glide.with(context)
                        .asGif()
                        .load(model)
                        .centerCrop()
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(imageView)
                } else if (isCircle) {
                    Glide.with(context)
                        .load(model)
                        .transform(CenterCrop(), CircleCrop())
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(imageView)
                } else {
                    Glide.with(context)
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(imageView)
                }
                loaded = true
            } catch (_: Exception) {}
        }
    }

    if (!loaded) {
        val letterName = if (fallbackName.isNotBlank()) fallbackName else "P"
        val drawable = createLetterAvatarDrawable(context, letterName, radiusPx.toFloat(), viewSizePx)
        imageView.setImageDrawable(drawable)
    }
}

fun createLetterAvatarDrawable(context: Context, name: String, radiusPx: Float, sizePx: Int = 120): android.graphics.drawable.Drawable {
    val cleanName = if (name.isNotBlank()) name.trim() else "P"
    val letter = cleanName.substring(0, 1).uppercase(Locale.getDefault())
    val color = ColorAccentManager.getAvatarColor(cleanName)
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = color
    val rect = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
    canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = sizePx * 0.45f
    textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    textPaint.textAlign = android.graphics.Paint.Align.CENTER

    val y = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(letter, sizePx / 2f, y, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun loadAvatarFileIntoView(context: Context, file: File, imageView: ImageView) {
    loadAvatarIntoView(context, file, imageView)
}

fun loadAvatarUriIntoView(context: Context, uri: Uri, imageView: ImageView) {
    loadAvatarIntoView(context, uri, imageView)
}

fun getAvatarColor(name: String): Int {
    return ColorAccentManager.getAvatarColor(name)
}

fun sanitizeLocalUserProfile(context: Context) {
    try {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        if (currentUser.isBlank()) return

        val myDisplayName = sharedPrefs.getString("${currentUser}_name", currentUser) ?: currentUser

        val editor = sharedPrefs.edit()

        val keysToRemove = listOf(
            "contact_avatar_$currentUser",
            "contact_name_$currentUser",
            "contact_avatar_history_$currentUser",
            "contact_avatar_$myDisplayName",
            "contact_name_$myDisplayName",
            "contact_avatar_history_$myDisplayName",
            "contact_avatar_Собеседник",
            "contact_name_Собеседник",
            "contact_avatar_history_Собеседник",
            "Собеседник_name",
            "contact_name_PrimeDesktop",
            "contact_avatar_PrimeDesktop",
            "PrimeDesktop_name",
            "contact_name_Prime User",
            "Prime User_name",
            "contact_name_null",
            "null_name",
            "contact_name_1",
            "1_name"
        )

        keysToRemove.forEach { key ->
            if (sharedPrefs.contains(key)) {
                editor.remove(key)
            }
        }

        editor.apply()
    } catch (_: Exception) {}
}
