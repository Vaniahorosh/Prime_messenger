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
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val historyKey = if (currentUser.isNotEmpty()) "my_avatar_history_$currentUser" else "my_avatar_history"
        
        var json = sharedPrefs.getString(historyKey, null)
        if (json == null && currentUser.isEmpty()) {
            json = sharedPrefs.getString("my_avatar_history", "[]")
        }
        if (json == null) json = "[]"

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

        // Fallback to single avatar if history is empty
        if (list.isEmpty()) {
            val legacy = if (currentUser.isNotEmpty()) {
                sharedPrefs.getString("${currentUser}_avatar", null)
                    ?: sharedPrefs.getString("${currentUser}_avatarUri", null)
            } else {
                sharedPrefs.getString("my_avatar", null)
                    ?: sharedPrefs.getString("my_local_avatar", null)
                    ?: sharedPrefs.getString("my_avatar_uri", null)
            }
            if (!legacy.isNullOrBlank()) {
                val (_, legacyFile) = parseAvatarModelAndFile(legacy)
                if (legacyFile == null || legacyFile.exists()) {
                    list.add(legacy)
                    saveMyAvatarHistory(context, list)
                }
            }
        }
        return list
    }

    fun saveMyAvatarHistory(context: Context, history: List<String>) {
        val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val historyKey = if (currentUser.isNotEmpty()) "my_avatar_history_$currentUser" else "my_avatar_history"
        val array = JSONArray()
        history.forEach { array.put(it) }

        val editor = sharedPrefs.edit()
            .putString(historyKey, array.toString())

        if (history.isNotEmpty()) {
            val active = history.first()
            if (currentUser.isNotEmpty()) {
                editor.putString("${currentUser}_avatar", active)
                    .putString("${currentUser}_avatarUri", active)
            } else {
                editor.putString("my_avatar", active)
                    .putString("my_local_avatar", active)
                    .putString("my_avatar_uri", active)
            }
            editor.apply()
        } else {
            AvatarManager.clearMyAvatar(context)
        }
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
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val myName = sharedPrefs.getString("${currentUser}_name", currentUser) ?: ""
        val isGeneric = addressOrName.equals("Собеседник", ignoreCase = true) ||
                        addressOrName.equals("Prime Собеседник", ignoreCase = true) ||
                        addressOrName.equals("Пользователь", ignoreCase = true) ||
                        addressOrName.equals("Prime User", ignoreCase = true) ||
                        addressOrName.equals("Контакт", ignoreCase = true)
        val isLocal = addressOrName.equals(currentUser, ignoreCase = true) || addressOrName.equals(myName, ignoreCase = true)

        if (isLocal || isGeneric) return emptyList()

        val historyKey = if (currentUser.isNotEmpty()) "contact_avatar_history_${currentUser}_$addressOrName" else "contact_avatar_history_$addressOrName"
        var json = sharedPrefs.getString(historyKey, null)
        if (json == null && currentUser.isEmpty()) {
            json = sharedPrefs.getString("contact_avatar_history_$addressOrName", "[]")
        }
        if (json == null) json = "[]"

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
            val legacy = if (currentUser.isNotEmpty()) {
                sharedPrefs.getString("contact_avatar_${currentUser}_$addressOrName", null)
            } else {
                sharedPrefs.getString("contact_avatar_$addressOrName", null)
                    ?: sharedPrefs.getString("${addressOrName}_avatar", null)
                    ?: sharedPrefs.getString("${addressOrName}_avatarUri", null)
            }
            if (!legacy.isNullOrBlank()) {
                val (_, legacyFile) = parseAvatarModelAndFile(legacy)
                if (legacyFile == null || legacyFile.exists()) {
                    list.add(legacy)
                    saveContactAvatarHistory(context, addressOrName, list)
                }
            }
        }

        if (list.isEmpty()) {
            val fileCandidates = mutableListOf<File>()
            if (currentUser.isNotEmpty()) {
                fileCandidates.add(File(context.filesDir, "rec_avatar_${currentUser}_$addressOrName.gif"))
                fileCandidates.add(File(context.filesDir, "rec_avatar_${currentUser}_$addressOrName.jpg"))
            } else {
                fileCandidates.add(File(context.filesDir, "rec_avatar_$addressOrName.gif"))
                fileCandidates.add(File(context.filesDir, "rec_avatar_$addressOrName.jpg"))
            }

            for (f in fileCandidates) {
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
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val myName = sharedPrefs.getString("${currentUser}_name", currentUser) ?: ""
        val isGeneric = addressOrName.equals("Собеседник", ignoreCase = true) ||
                        addressOrName.equals("Prime Собеседник", ignoreCase = true) ||
                        addressOrName.equals("Пользователь", ignoreCase = true) ||
                        addressOrName.equals("Prime User", ignoreCase = true) ||
                        addressOrName.equals("Контакт", ignoreCase = true)
        val isLocal = addressOrName.equals(currentUser, ignoreCase = true) || addressOrName.equals(myName, ignoreCase = true)

        if (isLocal || isGeneric) return

        val array = JSONArray()
        history.forEach { array.put(it) }

        val historyKey = if (currentUser.isNotEmpty()) "contact_avatar_history_${currentUser}_$addressOrName" else "contact_avatar_history_$addressOrName"
        val editor = sharedPrefs.edit().putString(historyKey, array.toString())

        if (history.isNotEmpty()) {
            val active = history.first()
            if (currentUser.isNotEmpty()) {
                editor.putString("contact_avatar_${currentUser}_$addressOrName", active)
            } else {
                editor.putString("contact_avatar_$addressOrName", active)
                    .putString("${addressOrName}_avatar", active)
            }
            editor.apply()
        } else {
            AvatarManager.clearContactAvatar(context, addressOrName)
        }
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
