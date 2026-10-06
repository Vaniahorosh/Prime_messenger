package com.messenger.prime

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
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
        val myLocalName = sharedPrefs.getString("my_name", "") ?: ""

        val trimmed = identity.trim()
        return trimmed.equals(currentUser, ignoreCase = true) ||
                trimmed.equals(myDisplayName, ignoreCase = true) ||
                trimmed.equals(myLocalName, ignoreCase = true) ||
                trimmed.equals("my", ignoreCase = true) ||
                trimmed.equals("me", ignoreCase = true) ||
                isGenericIdentity(trimmed)
    }

    @JvmStatic
    fun isProtectedIdentity(context: Context, identity: String?): Boolean {
        return isLocalUserIdentity(context, identity) || isGenericIdentity(identity)
    }

    @JvmStatic
    fun getContactAvatarFile(context: Context, contactId: String?, contactName: String?): File? {
        val filesDir = context.filesDir
        val candidates = mutableListOf<File>()

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
        sharedPrefs.edit().putString("${prefKeySuffix}_$contactKey", value).apply()
    }
}

fun loadAvatarFileIntoView(context: Context, file: File, imageView: ImageView) {
    try {
        Glide.with(context).clear(imageView)
    } catch (_: Exception) {}

    val isGif = file.name.lowercase().endsWith(".gif")
    val radiusPx = (14 * context.resources.displayMetrics.density).toInt()
    val signatureKey = ObjectKey(if (file.exists()) "${file.absolutePath}_${file.lastModified()}" else file.name)
    if (isGif) {
        Glide.with(context)
            .asGif()
            .load(file)
            .centerCrop()
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    } else {
        Glide.with(context)
            .load(file)
            .transform(CenterCrop(), RoundedCorners(radiusPx))
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    }
}

fun loadAvatarUriIntoView(context: Context, uri: Uri, imageView: ImageView) {
    try {
        Glide.with(context).clear(imageView)
    } catch (_: Exception) {}

    val uriStr = uri.toString().lowercase()
    val isGif = uriStr.endsWith(".gif") || uriStr.contains("gif")
    val radiusPx = (14 * context.resources.displayMetrics.density).toInt()
    val file = if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else null
    val signatureKey = ObjectKey(if (file != null && file.exists()) "${file.absolutePath}_${file.lastModified()}" else uri.toString())
    if (isGif) {
        Glide.with(context)
            .asGif()
            .load(uri)
            .centerCrop()
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    } else {
        Glide.with(context)
            .load(uri)
            .transform(CenterCrop(), RoundedCorners(radiusPx))
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    }
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