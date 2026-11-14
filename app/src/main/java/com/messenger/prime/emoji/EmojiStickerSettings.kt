package com.messenger.prime.emoji

import android.content.Context

enum class StickerSize(val title: String, val sizeDp: Int) {
    COMPACT("Компактный (120 dp)", 120),
    STANDARD("Стандартный (150 dp)", 150),
    LARGE("Крупный (185 dp)", 185)
}

enum class EmojiGridSize(val title: String, val sizeSp: Int, val itemSizeDp: Int) {
    COMPACT("Компактный (20 sp)", 20, 36),
    STANDARD("Стандартный (24 sp)", 24, 42),
    LARGE("Крупный (28 sp)", 28, 48)
}

object EmojiStickerSettings {
    private const val PREFS_NAME = "PrimeEmojiPrefs"
    private const val KEY_STICKER_SIZE = "setting_sticker_size_dp"
    private const val KEY_EMOJI_GRID_SIZE = "setting_emoji_grid_size_sp"
    private const val KEY_HAPTIC = "setting_haptic_feedback"
    private const val KEY_SHOW_PACK_TAG = "setting_show_pack_tag"
    private const val KEY_AUTO_CLOSE_ON_STICKER = "setting_auto_close_on_sticker"
    private const val KEY_START_WITH_RECENTS = "setting_start_with_recents"
    private const val KEY_PANEL_HEIGHT_PX = "setting_custom_panel_height_px"

    @JvmStatic
    fun getPanelHeightPx(context: Context, defaultPx: Int): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_PANEL_HEIGHT_PX, defaultPx)
    }

    @JvmStatic
    fun setPanelHeightPx(context: Context, heightPx: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_PANEL_HEIGHT_PX, heightPx).apply()
    }

    @JvmStatic
    fun getStickerSizeDp(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_STICKER_SIZE, StickerSize.STANDARD.sizeDp)
    }

    @JvmStatic
    fun setStickerSizeDp(context: Context, sizeDp: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_STICKER_SIZE, sizeDp).apply()
    }

    @JvmStatic
    fun getEmojiGridSizeSp(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_EMOJI_GRID_SIZE, EmojiGridSize.STANDARD.sizeSp)
    }

    @JvmStatic
    fun setEmojiGridSizeSp(context: Context, sizeSp: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_EMOJI_GRID_SIZE, sizeSp).apply()
    }

    @JvmStatic
    fun isHapticEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_HAPTIC, true)
    }

    @JvmStatic
    fun setHapticEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    @JvmStatic
    fun isShowPackTag(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SHOW_PACK_TAG, true)
    }

    @JvmStatic
    fun setShowPackTag(context: Context, show: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_SHOW_PACK_TAG, show).apply()
    }

    @JvmStatic
    fun isAutoCloseOnStickerSend(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_AUTO_CLOSE_ON_STICKER, true)
    }

    @JvmStatic
    fun setAutoCloseOnStickerSend(context: Context, autoClose: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_AUTO_CLOSE_ON_STICKER, autoClose).apply()
    }

    @JvmStatic
    fun isStartWithRecents(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_START_WITH_RECENTS, true)
    }

    @JvmStatic
    fun setStartWithRecents(context: Context, startWithRecents: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_START_WITH_RECENTS, startWithRecents).apply()
    }

    @JvmStatic
    fun clearRecents(context: Context) {
        EmojiData.clearRecents(context)
    }
}
