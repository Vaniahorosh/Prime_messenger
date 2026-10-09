package com.messenger.prime

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.util.Locale

object AvatarManager {

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
            sharedPrefs.getString("${currentUser}_avatarUri", null),
            sharedPrefs.getString("my_avatar", null),
            sharedPrefs.getString("my_local_avatar", null),
            sharedPrefs.getString("my_avatar_uri", null)
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
        }

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

        for (key in keys) {
            val saved = sharedPrefs.getString(key, null)
            if (!saved.isNullOrBlank() && saved != "1" && saved != "null") {
                val isBt = try { BluetoothAdapter.checkBluetoothAddress(saved) } catch (_: Exception) { false }
                return if (isBt) "Собеседник" else saved
            }
        }

        val nameCandidate = when {
            !fallbackName.isNullOrBlank() -> fallbackName
            !contactId.isNullOrBlank() -> contactId
            else -> "Собеседник"
        }
        val isBt = try { BluetoothAdapter.checkBluetoothAddress(nameCandidate) } catch (_: Exception) { false }
        return if (isBt) "Собеседник" else nameCandidate
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
        }

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
                ?: getMyAvatarUri(context)
        }

        if (!resolvedAvatar.isNullOrBlank()) {
            editor.putString("${login}_avatar", resolvedAvatar)
            editor.putString("${login}_avatarUri", resolvedAvatar)
            editor.putString("my_avatar", resolvedAvatar)
            editor.putString("my_local_avatar", resolvedAvatar)
            editor.putString("my_avatar_uri", resolvedAvatar)
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

        return candidates.firstOrNull { it.exists() && it.length() > 0 }
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
        editor.putString("${prefKeySuffix}_$contactKey", value).apply()
    }
}

fun getAvatarCornerRadiusPx(context: Context, viewSizePx: Int = (48 * context.resources.displayMetrics.density).toInt()): Int {
    val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
    val percent = sp.getInt("settings_avatar_radius_percent", 50)
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
        (context is ContextWrapper && context.baseContext is PersonInformationActivity) ||
        context is SettingsActivity ||
        (context is ContextWrapper && context.baseContext is SettingsActivity)

    val targetWidth = imageView.layoutParams?.width?.coerceAtLeast(0) ?: 0
    val viewSizePx = if (targetWidth > 0) targetWidth else (48 * context.resources.displayMetrics.density).toInt()

    val radiusPx = if (isExcludedActivity) {
        (14 * context.resources.displayMetrics.density).toInt()
    } else {
        getAvatarCornerRadiusPx(context, viewSizePx)
    }

    var loaded = false
    val (model, file) = when (avatarSource) {
        is String -> parseAvatarModelAndFile(avatarSource)
        is Uri -> Pair(avatarSource, if (avatarSource.scheme == "file" && avatarSource.path != null) File(avatarSource.path!!) else null)
        is File -> Pair(avatarSource, avatarSource)
        else -> Pair(avatarSource, null)
    }

    if (model != null) {
        if (file == null || file.exists()) {
            val sourceStr = avatarSource.toString().lowercase(Locale.US)
            val isGif = sourceStr.endsWith(".gif") || sourceStr.contains("gif")
            val signatureKey = ObjectKey(if (file != null && file.exists()) "${file.absolutePath}_${file.lastModified()}" else avatarSource.toString())
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
        if (fallbackName.isNotBlank()) {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx.toFloat()
                setColor(getAvatarColor(fallbackName))
            }
            imageView.setImageDrawable(drawable)
        } else {
            imageView.setImageResource(R.drawable.ic_person)
        }
    }
}

fun loadAvatarFileIntoView(context: Context, file: File, imageView: ImageView) {
    loadAvatarIntoView(context, file, imageView)
}

fun loadAvatarUriIntoView(context: Context, uri: Uri, imageView: ImageView) {
    loadAvatarIntoView(context, uri, imageView)
}

fun getAvatarColor(name: String): Int {
    val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
    )
    val hash = name.hashCode()
    val index = (if (hash == Int.MIN_VALUE) 0 else Math.abs(hash)) % colors.size
    return colors[index].toColorInt()
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
            "contact_avatar_history_Собеседник"
        )

        keysToRemove.forEach { key ->
            if (sharedPrefs.contains(key)) {
                editor.remove(key)
            }
        }

        editor.apply()
    } catch (_: Exception) {}
}
