package com.messenger.prime

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ChatWallpaper(
    val id: String,
    val name: String,
    val type: String, // "DEFAULT", "PATTERN", "IMAGE", "MATERIAL"
    val imagePath: String? = null,
    val patternType: String? = "shapes",
    val customEmojis: String? = null,
    val backgroundColor: String? = "#151D2A",
    val layoutOrientation: String = "grid",
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val dimOpacity: Float = 0.2f,
    val animationEffect: String = "float",
    val isAnimated: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object WallpaperManager {

    const val ACTION_WALLPAPER_CHANGED = "com.messenger.prime.WALLPAPER_CHANGED"
    const val EXTRA_CONTACT_KEY = "contact_key"
    const val EXTRA_CONTACT_NAME = "contact_name"

    private const val PREFS_NAME = "PrimeLocalDB"
    private const val KEY_WALLPAPERS_JSON = "custom_wallpapers_json_v1"
    private const val KEY_GLOBAL_ACTIVE_WALLPAPER = "global_chat_wallpaper_id"
    private const val KEY_PER_CONTACT_WALLPAPER_PREFIX = "chat_wallpaper_for_"

    @JvmStatic
    fun getDefaultWallpaper(): ChatWallpaper {
        return ChatWallpaper(
            id = "default",
            name = "Стандартные",
            type = "DEFAULT"
        )
    }

    @JvmStatic
    fun getWallpapersList(context: Context): List<ChatWallpaper> {
        val list = mutableListOf<ChatWallpaper>()
        list.add(getDefaultWallpaper())

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_WALLPAPERS_JSON, null) ?: return list

        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val type = obj.optString("type")
                val imagePath = if (obj.has("imagePath")) obj.optString("imagePath") else null
                val patternType = if (obj.has("patternType")) obj.optString("patternType") else "shapes"
                val customEmojis = if (obj.has("customEmojis")) obj.optString("customEmojis") else null
                val backgroundColor = if (obj.has("backgroundColor")) obj.optString("backgroundColor") else "#151D2A"
                val layoutOrientation = obj.optString("layoutOrientation", "grid")
                val scale = obj.optDouble("scale", 1.0).toFloat()
                val offsetX = obj.optDouble("offsetX", 0.0).toFloat()
                val offsetY = obj.optDouble("offsetY", 0.0).toFloat()
                val rotation = obj.optDouble("rotation", 0.0).toFloat()
                val dimOpacity = obj.optDouble("dimOpacity", 0.2).toFloat()
                val animationEffect = obj.optString("animationEffect", "float")
                val isAnimated = obj.optBoolean("isAnimated", true)
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (id.isNotBlank() && name.isNotBlank()) {
                    list.add(ChatWallpaper(id, name, type, imagePath, patternType, customEmojis, backgroundColor, layoutOrientation, scale, offsetX, offsetY, rotation, dimOpacity, animationEffect, isAnimated, createdAt))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    @JvmStatic
    fun getActiveWallpaper(context: Context, contactKey: String? = null): ChatWallpaper {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val activeId = if (!contactKey.isNullOrBlank()) {
            sp.getString("${KEY_PER_CONTACT_WALLPAPER_PREFIX}${contactKey.lowercase()}", null)
                ?: sp.getString(KEY_GLOBAL_ACTIVE_WALLPAPER, "default")
        } else {
            sp.getString(KEY_GLOBAL_ACTIVE_WALLPAPER, "default")
        }

        val all = getWallpapersList(context)
        return all.find { it.id == activeId } ?: getDefaultWallpaper()
    }

    @JvmStatic
    fun setActiveWallpaper(context: Context, wallpaperId: String, contactKey: String? = null) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!contactKey.isNullOrBlank()) {
            sp.edit().putString("${KEY_PER_CONTACT_WALLPAPER_PREFIX}${contactKey.lowercase()}", wallpaperId).apply()
        } else {
            sp.edit().putString(KEY_GLOBAL_ACTIVE_WALLPAPER, wallpaperId).apply()
        }
    }

    @JvmStatic
    fun saveCustomWallpaper(context: Context, wallpaper: ChatWallpaper) {
        val list = getWallpapersList(context).toMutableList()
        list.removeAll { it.id == wallpaper.id }
        list.add(wallpaper)

        val customOnly = list.filter { it.type != "DEFAULT" }
        saveCustomWallpapersList(context, customOnly)
    }

    @JvmStatic
    fun deleteCustomWallpaper(context: Context, wallpaperId: String) {
        if (wallpaperId == "default") return

        val list = getWallpapersList(context).toMutableList()
        val toDelete = list.find { it.id == wallpaperId } ?: return

        if (!toDelete.imagePath.isNullOrEmpty()) {
            try {
                val f = File(toDelete.imagePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }

        list.removeAll { it.id == wallpaperId }
        val customOnly = list.filter { it.type != "DEFAULT" }
        saveCustomWallpapersList(context, customOnly)

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentGlobal = sp.getString(KEY_GLOBAL_ACTIVE_WALLPAPER, "default")
        if (currentGlobal == wallpaperId) {
            sp.edit().putString(KEY_GLOBAL_ACTIVE_WALLPAPER, "default").apply()
        }
    }

    @JvmStatic
    fun applyToView(view: View, contactKey: String? = null) {
        val context = view.context
        val wp = getActiveWallpaper(context, contactKey)

        if (wp.type == "IMAGE" && !wp.imagePath.isNullOrEmpty()) {
            val file = File(wp.imagePath)
            if (file.exists()) {
                Glide.with(context)
                    .load(file)
                    .centerCrop()
                    .into(object : CustomViewTarget<View, Drawable>(view) {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            view.background = resource
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {}
                        override fun onResourceCleared(placeholder: Drawable?) {}
                    })
                return
            }
        }

        if (view is MaterialShapesWallpaperView) {
            view.updateThemeColors()
        }
    }

    private fun saveCustomWallpapersList(context: Context, customList: List<ChatWallpaper>) {
        val jsonArray = JSONArray()
        for (wp in customList) {
            val obj = JSONObject()
            obj.put("id", wp.id)
            obj.put("name", wp.name)
            obj.put("type", wp.type)
            if (wp.imagePath != null) obj.put("imagePath", wp.imagePath)
            if (wp.patternType != null) obj.put("patternType", wp.patternType)
            if (wp.customEmojis != null) obj.put("customEmojis", wp.customEmojis)
            if (wp.backgroundColor != null) obj.put("backgroundColor", wp.backgroundColor)
            obj.put("layoutOrientation", wp.layoutOrientation)
            obj.put("scale", wp.scale)
            obj.put("offsetX", wp.offsetX)
            obj.put("offsetY", wp.offsetY)
            obj.put("rotation", wp.rotation)
            obj.put("dimOpacity", wp.dimOpacity)
            obj.put("animationEffect", wp.animationEffect)
            obj.put("isAnimated", wp.isAnimated)
            obj.put("createdAt", wp.createdAt)
            jsonArray.put(obj)
        }

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_WALLPAPERS_JSON, jsonArray.toString()).apply()
    }
}
