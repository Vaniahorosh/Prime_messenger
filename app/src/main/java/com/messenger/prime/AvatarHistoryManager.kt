package com.messenger.prime

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File

object AvatarHistoryManager {

    private fun isSameAvatarPath(path1: String?, path2: String?): Boolean {
        if (path1.isNullOrBlank() || path2.isNullOrBlank()) return false
        if (path1 == path2 || path1.trim() == path2.trim()) return true
        val (_, f1) = parseAvatarModelAndFile(path1)
        val (_, f2) = parseAvatarModelAndFile(path2)
        if (f1 != null && f2 != null) {
            return f1.absolutePath == f2.absolutePath
        }
        return false
    }

    fun getMyAvatarHistory(context: Context): List<String> {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("my_avatar_history", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val path = array.getString(i)
                if (path.isNotBlank()) {
                    val (_, file) = parseAvatarModelAndFile(path)
                    if (file == null || file.exists()) {
                        if (!list.any { isSameAvatarPath(it, path) }) {
                            list.add(path)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback to legacy single avatar if history is empty
        if (list.isEmpty()) {
            val legacy = sharedPrefs.getString("my_avatar", null)
                ?: sharedPrefs.getString("my_local_avatar", null)
                ?: sharedPrefs.getString("my_avatar_uri", null)
            if (!legacy.isNullOrBlank()) {
                val (_, legacyFile) = parseAvatarModelAndFile(legacy)
                if (legacyFile == null || legacyFile.exists()) {
                    list.add(legacy)
                    saveMyAvatarHistory(context, list)
                } else {
                    sharedPrefs.edit()
                        .remove("my_avatar")
                        .remove("my_local_avatar")
                        .remove("my_avatar_uri")
                        .apply()
                }
            }
        }
        return list
    }

    fun saveMyAvatarHistory(context: Context, history: List<String>) {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val array = JSONArray()
        history.forEach { array.put(it) }

        val editor = sharedPrefs.edit()
            .putString("my_avatar_history", array.toString())

        if (history.isNotEmpty()) {
            val active = history.first()
            editor.putString("my_avatar", active)
                .putString("my_local_avatar", active)
                .putString("my_avatar_uri", active)
            if (currentUser.isNotEmpty()) {
                editor.putString("${currentUser}_avatar", active)
                    .putString("${currentUser}_avatarUri", active)
            }
        } else {
            editor.remove("my_avatar")
                .remove("my_local_avatar")
                .remove("my_avatar_uri")
            if (currentUser.isNotEmpty()) {
                editor.remove("${currentUser}_avatar")
                    .remove("${currentUser}_avatarUri")
            }
        }
        editor.apply()
    }

    fun addMyAvatar(context: Context, newAvatarUri: String) {
        val current = getMyAvatarHistory(context).toMutableList()
        current.removeAll { isSameAvatarPath(it, newAvatarUri) }
        current.add(0, newAvatarUri)
        saveMyAvatarHistory(context, current)
    }

    fun removeMyAvatar(context: Context, targetUri: String): String? {
        val current = getMyAvatarHistory(context).toMutableList()
        current.removeAll { isSameAvatarPath(it, targetUri) }

        val (_, file) = parseAvatarModelAndFile(targetUri)
        if (file != null && file.exists() && !file.name.contains("default")) {
            try { file.delete() } catch (_: Exception) {}
        }
        saveMyAvatarHistory(context, current)
        return current.firstOrNull()
    }

    fun getContactAvatarHistory(context: Context, addressOrName: String): List<String> {
        if (addressOrName.isBlank()) return emptyList()
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("contact_avatar_history_$addressOrName", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val path = array.getString(i)
                if (path.isNotBlank()) {
                    val (_, file) = parseAvatarModelAndFile(path)
                    if (file == null || file.exists()) {
                        if (!list.any { isSameAvatarPath(it, path) }) {
                            list.add(path)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            val legacy = sharedPrefs.getString("contact_avatar_$addressOrName", null)
                ?: sharedPrefs.getString("${addressOrName}_avatar", null)
                ?: sharedPrefs.getString("${addressOrName}_avatarUri", null)
            if (!legacy.isNullOrBlank()) {
                val (_, legacyFile) = parseAvatarModelAndFile(legacy)
                if (legacyFile == null || legacyFile.exists()) {
                    list.add(legacy)
                    saveContactAvatarHistory(context, addressOrName, list)
                } else {
                    sharedPrefs.edit()
                        .remove("contact_avatar_$addressOrName")
                        .remove("${addressOrName}_avatar")
                        .remove("${addressOrName}_avatarUri")
                        .apply()
                }
            }
        }

        if (list.isEmpty()) {
            val candidates = arrayOf(
                File(context.filesDir, "rec_avatar_$addressOrName.gif"),
                File(context.filesDir, "rec_avatar_$addressOrName.jpg"),
                File(context.filesDir, "avatar_$addressOrName.gif"),
                File(context.filesDir, "avatar_$addressOrName.jpg")
            )
            for (f in candidates) {
                if (f.exists() && f.length() > 0) {
                    val uriStr = Uri.fromFile(f).toString()
                    list.add(uriStr)
                    saveContactAvatarHistory(context, addressOrName, list)
                    break
                }
            }
        }
        return list
    }

    fun saveContactAvatarHistory(context: Context, addressOrName: String, history: List<String>) {
        if (addressOrName.isBlank()) return
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val array = JSONArray()
        history.forEach { array.put(it) }

        val editor = sharedPrefs.edit()
            .putString("contact_avatar_history_$addressOrName", array.toString())

        if (history.isNotEmpty()) {
            val active = history.first()
            editor.putString("contact_avatar_$addressOrName", active)
                .putString("${addressOrName}_avatar", active)
        } else {
            editor.remove("contact_avatar_$addressOrName")
                .remove("${addressOrName}_avatar")
        }
        editor.apply()
    }

    fun addContactAvatar(context: Context, addressOrName: String, newAvatarUri: String) {
        if (addressOrName.isBlank()) return
        val current = getContactAvatarHistory(context, addressOrName).toMutableList()
        current.removeAll { isSameAvatarPath(it, newAvatarUri) }
        current.add(0, newAvatarUri)
        saveContactAvatarHistory(context, addressOrName, current)
    }

    fun removeContactAvatar(context: Context, addressOrName: String, targetUri: String): String? {
        if (addressOrName.isBlank()) return null
        val current = getContactAvatarHistory(context, addressOrName).toMutableList()
        current.removeAll { isSameAvatarPath(it, targetUri) }

        val (_, file) = parseAvatarModelAndFile(targetUri)
        if (file != null && file.exists()) {
            try { file.delete() } catch (_: Exception) {}
        }
        saveContactAvatarHistory(context, addressOrName, current)
        return current.firstOrNull()
    }
}
