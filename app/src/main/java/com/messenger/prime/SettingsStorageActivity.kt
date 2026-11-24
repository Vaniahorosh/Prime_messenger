package com.messenger.prime

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.webkit.WebStorage
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.messenger.prime.databinding.ActivitySettingsStorageBinding
import com.messenger.prime.shapes.M3EShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsStorageActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsStorageBinding
    private var currentStorageBreakdown: StorageBreakdown? = null

    private data class StorageBreakdown(
        var photosSize: Long = 0L,
        var photosCount: Int = 0,
        var videosSize: Long = 0L,
        var videosCount: Int = 0,
        var audioSize: Long = 0L,
        var audioCount: Int = 0,
        var gifsSize: Long = 0L,
        var gifsCount: Int = 0,
        var filesSize: Long = 0L,
        var filesCount: Int = 0,
        var avatarsSize: Long = 0L,
        var avatarsCount: Int = 0,
        var cacheSize: Long = 0L,
        var cacheCount: Int = 0,
        var totalSize: Long = 0L,
        var totalCount: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsStorageBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutStorageHeroStage,
            heroOverlay = b.layoutStorageHeroOverlay,
            scrollView = b.scrollStorageSettings,
            title = "Данные и память",
            subtitle = "Подсчёт...",
            heroHeightFraction = 0.30f,
            heroMinDp = 220,
            heroMaxDp = 276,
            animatedCards = listOf(b.cardStorage)
        )

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconStorage, accentColor, M3EShapes.cookie4Sided, isDark)
        b.tvTitleStorage.setTextColor(accentColor)
        b.tvSelectAllStorage.setTextColor(accentColor)
        b.btnClearStorage.backgroundTintList = accentTint

        b.cbStoragePhotos.buttonTintList = accentTint
        b.cbStorageVideos.buttonTintList = accentTint
        b.cbStorageAudio.buttonTintList = accentTint
        b.cbStorageGifs.buttonTintList = accentTint
        b.cbStorageFiles.buttonTintList = accentTint
        b.cbStorageAvatars.buttonTintList = accentTint
        b.cbStorageCache.buttonTintList = accentTint

        setupListeners()
        ColorAccentManager.tintViewTree(b.root, accentColor)
        calculateStorageAsync()
    }

    private fun setupListeners() {
        b.rowStoragePhotos.setOnClickListener {
            b.cbStoragePhotos.isChecked = !b.cbStoragePhotos.isChecked
        }
        b.rowStorageVideos.setOnClickListener {
            b.cbStorageVideos.isChecked = !b.cbStorageVideos.isChecked
        }
        b.rowStorageAudio.setOnClickListener {
            b.cbStorageAudio.isChecked = !b.cbStorageAudio.isChecked
        }
        b.rowStorageGifs.setOnClickListener {
            b.cbStorageGifs.isChecked = !b.cbStorageGifs.isChecked
        }
        b.rowStorageFiles.setOnClickListener {
            b.cbStorageFiles.isChecked = !b.cbStorageFiles.isChecked
        }
        b.rowStorageAvatars.setOnClickListener {
            b.cbStorageAvatars.isChecked = !b.cbStorageAvatars.isChecked
        }
        b.rowStorageCache.setOnClickListener {
            b.cbStorageCache.isChecked = !b.cbStorageCache.isChecked
        }

        b.tvSelectAllStorage.setOnClickListener {
            val allChecked = b.cbStoragePhotos.isChecked &&
                b.cbStorageVideos.isChecked &&
                b.cbStorageAudio.isChecked &&
                b.cbStorageGifs.isChecked &&
                b.cbStorageFiles.isChecked &&
                b.cbStorageAvatars.isChecked &&
                b.cbStorageCache.isChecked

            val targetState = !allChecked
            b.cbStoragePhotos.isChecked = targetState
            b.cbStorageVideos.isChecked = targetState
            b.cbStorageAudio.isChecked = targetState
            b.cbStorageGifs.isChecked = targetState
            b.cbStorageFiles.isChecked = targetState
            b.cbStorageAvatars.isChecked = targetState
            b.cbStorageCache.isChecked = targetState

            b.tvSelectAllStorage.text = if (targetState) "Снять всё" else "Выбрать всё"
            updateClearButtonText()
        }

        val listener = CompoundButton.OnCheckedChangeListener { _, _ -> updateClearButtonText() }
        b.cbStoragePhotos.setOnCheckedChangeListener(listener)
        b.cbStorageVideos.setOnCheckedChangeListener(listener)
        b.cbStorageAudio.setOnCheckedChangeListener(listener)
        b.cbStorageGifs.setOnCheckedChangeListener(listener)
        b.cbStorageFiles.setOnCheckedChangeListener(listener)
        b.cbStorageAvatars.setOnCheckedChangeListener(listener)
        b.cbStorageCache.setOnCheckedChangeListener(listener)

        b.btnClearStorage.setOnClickListener {
            clearSelectedStorage()
        }
    }

    private fun calculateStorageAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bd = StorageBreakdown()
            val allFiles = filesDir.walkTopDown().filter { it.isFile }.toList()

            for (f in allFiles) {
                val len = f.length()
                val name = f.name.lowercase(Locale.US)

                if (name.startsWith("my_profile_avatar") || name.startsWith("my_avatar") || name.startsWith("wallpaper_img_") || name.endsWith(".json") || name.endsWith(".xml")) {
                    continue
                }

                when {
                    name.startsWith("avatar_") || name.startsWith("rec_avatar_") || name.startsWith("tmp_contact_avatar_") -> {
                        bd.avatarsSize += len
                        bd.avatarsCount++
                    }
                    name.endsWith(".gif") || name.startsWith("tenor_") -> {
                        bd.gifsSize += len
                        bd.gifsCount++
                    }
                    name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi") || name.startsWith("rec_video_") || name.startsWith("rec_circle_") || name.startsWith("rec_videonote_") || f.parentFile?.name == "video_notes" -> {
                        bd.videosSize += len
                        bd.videosCount++
                    }
                    name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".opus") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav") || name.startsWith("rec_voice_") || name.startsWith("audio_") -> {
                        bd.audioSize += len
                        bd.audioCount++
                    }
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.startsWith("rec_photo_") || name.startsWith("cache_img_") || name.startsWith("cam_photo_") -> {
                        bd.photosSize += len
                        bd.photosCount++
                    }
                    name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log") || name.startsWith("cam_video_") -> {
                        bd.cacheSize += len
                        bd.cacheCount++
                    }
                    else -> {
                        bd.filesSize += len
                        bd.filesCount++
                    }
                }
            }

            try {
                if (cacheDir != null && cacheDir.exists()) {
                    cacheDir.walkTopDown().filter { it.isFile }.forEach { cf ->
                        bd.cacheSize += cf.length()
                        bd.cacheCount++
                    }
                }
                val extCache = externalCacheDir
                if (extCache != null && extCache.exists()) {
                    extCache.walkTopDown().filter { it.isFile }.forEach { ef ->
                        bd.cacheSize += ef.length()
                        bd.cacheCount++
                    }
                }
            } catch (_: Exception) {}

            bd.totalSize = bd.photosSize + bd.videosSize + bd.audioSize + bd.gifsSize + bd.filesSize + bd.avatarsSize + bd.cacheSize
            bd.totalCount = bd.photosCount + bd.videosCount + bd.audioCount + bd.gifsCount + bd.filesCount + bd.avatarsCount + bd.cacheCount

            val formattedTotal = SettingsSupport.formatStorageSize(bd.totalSize)
            getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                .edit()
                .putString("cached_storage_summary", formattedTotal)
                .apply()

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    updateStorageUi(bd)
                }
            }
        }
    }

    private fun updateStorageUi(bd: StorageBreakdown) {
        currentStorageBreakdown = bd

        b.tvSizePhotos.text = "${bd.photosCount} ф. (${SettingsSupport.formatStorageSize(bd.photosSize)})"
        b.tvSizeVideos.text = "${bd.videosCount} ф. (${SettingsSupport.formatStorageSize(bd.videosSize)})"
        b.tvSizeAudio.text = "${bd.audioCount} ф. (${SettingsSupport.formatStorageSize(bd.audioSize)})"
        b.tvSizeGifs.text = "${bd.gifsCount} ф. (${SettingsSupport.formatStorageSize(bd.gifsSize)})"
        b.tvSizeFiles.text = "${bd.filesCount} ф. (${SettingsSupport.formatStorageSize(bd.filesSize)})"
        b.tvSizeAvatars.text = "${bd.avatarsCount} ф. (${SettingsSupport.formatStorageSize(bd.avatarsSize)})"
        b.tvSizeCache.text = "${bd.cacheCount} ф. (${SettingsSupport.formatStorageSize(bd.cacheSize)})"

        val totalFormatted = SettingsSupport.formatStorageSize(bd.totalSize)
        b.tvHeroStorageTotalSize.text = totalFormatted
        b.tvStorageTotalSize.text = "Всего файлов: ${bd.totalCount}"
        updateDetailSubtitle(b.root, totalFormatted)

        updateClearButtonText()
    }

    private fun animateWeight(view: View, targetWeight: Float) {
        val lp = view.layoutParams as? LinearLayout.LayoutParams ?: return
        val startWeight = if (lp.weight <= 0f) 0.01f else lp.weight
        ValueAnimator.ofFloat(startWeight, targetWeight).apply {
            duration = 360L
            addUpdateListener {
                val p = view.layoutParams as LinearLayout.LayoutParams
                p.weight = it.animatedValue as Float
                view.layoutParams = p
            }
            start()
        }
    }

    private fun updateClearButtonText() {
        val bd = currentStorageBreakdown ?: return
        var selectedBytes = 0L
        var selectedCount = 0

        val pVisible = b.cbStoragePhotos.isChecked
        val vVisible = b.cbStorageVideos.isChecked
        val aVisible = b.cbStorageAudio.isChecked
        val gVisible = b.cbStorageGifs.isChecked
        val fVisible = b.cbStorageFiles.isChecked
        val avVisible = b.cbStorageAvatars.isChecked
        val cVisible = b.cbStorageCache.isChecked

        if (pVisible) { selectedBytes += bd.photosSize; selectedCount += bd.photosCount }
        if (vVisible) { selectedBytes += bd.videosSize; selectedCount += bd.videosCount }
        if (aVisible) { selectedBytes += bd.audioSize; selectedCount += bd.audioCount }
        if (gVisible) { selectedBytes += bd.gifsSize; selectedCount += bd.gifsCount }
        if (fVisible) { selectedBytes += bd.filesSize; selectedCount += bd.filesCount }
        if (avVisible) { selectedBytes += bd.avatarsSize; selectedCount += bd.avatarsCount }
        if (cVisible) { selectedBytes += bd.cacheSize; selectedCount += bd.cacheCount }

        val total = bd.totalSize.coerceAtLeast(1L).toFloat()

        animateWeight(b.vChartPhotos, if (pVisible) (bd.photosSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartVideos, if (vVisible) (bd.videosSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartAudio, if (aVisible) (bd.audioSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartGifs, if (gVisible) (bd.gifsSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartFiles, if (fVisible) (bd.filesSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartAvatars, if (avVisible) (bd.avatarsSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartOther, if (cVisible) (bd.cacheSize / total).coerceAtLeast(0.01f) else 0.001f)

        b.vChartPhotos.animate().alpha(if (pVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartVideos.animate().alpha(if (vVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartAudio.animate().alpha(if (aVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartGifs.animate().alpha(if (gVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartFiles.animate().alpha(if (fVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartAvatars.animate().alpha(if (avVisible) 1.0f else 0.2f).setDuration(220).start()
        b.vChartOther.animate().alpha(if (cVisible) 1.0f else 0.2f).setDuration(220).start()

        val allChecked = pVisible && vVisible && aVisible && gVisible && fVisible && avVisible && cVisible
        b.tvSelectAllStorage.text = if (allChecked) "Снять всё" else "Выбрать всё"

        if (selectedBytes > 0) {
            b.btnClearStorage.isEnabled = true
            b.btnClearStorage.text = "Очистить: $selectedCount ф. (${SettingsSupport.formatStorageSize(selectedBytes)})"
        } else {
            b.btnClearStorage.isEnabled = false
            b.btnClearStorage.text = "Ничего не выбрано"
        }
    }

    private fun clearSelectedStorage() {
        b.btnClearStorage.isEnabled = false
        b.btnClearStorage.text = "Очистка..."

        lifecycleScope.launch(Dispatchers.IO) {
            var deletedBytes = 0L
            var deletedCount = 0

            val deletePhotos = b.cbStoragePhotos.isChecked
            val deleteVideos = b.cbStorageVideos.isChecked
            val deleteAudio = b.cbStorageAudio.isChecked
            val deleteGifs = b.cbStorageGifs.isChecked
            val deleteFiles = b.cbStorageFiles.isChecked
            val deleteAvatars = b.cbStorageAvatars.isChecked
            val deleteCache = b.cbStorageCache.isChecked

            val allFiles = filesDir.walkTopDown().filter { it.isFile }.toList()

            for (f in allFiles) {
                val name = f.name.lowercase(Locale.US)
                val len = f.length()

                if (name.startsWith("my_profile_avatar") || name.startsWith("my_avatar") || name.startsWith("wallpaper_img_") || name.endsWith(".json") || name.endsWith(".xml")) {
                    continue
                }

                val isAvatar = name.startsWith("avatar_") || name.startsWith("rec_avatar_") || name.startsWith("tmp_contact_avatar_")
                val isGif = name.endsWith(".gif") || name.startsWith("tenor_")
                val isVid = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi") || name.startsWith("rec_video_") || name.startsWith("rec_circle_") || name.startsWith("rec_videonote_") || f.parentFile?.name == "video_notes"
                val isAud = name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".opus") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav") || name.startsWith("rec_voice_") || name.startsWith("audio_")
                val isPhoto = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.startsWith("rec_photo_") || name.startsWith("cache_img_") || name.startsWith("cam_photo_")
                val isTmp = name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log") || name.startsWith("cam_video_")

                val delete = when {
                    isAvatar -> deleteAvatars
                    isGif -> deleteGifs
                    isVid -> deleteVideos
                    isAud -> deleteAudio
                    isPhoto -> deletePhotos
                    isTmp -> deleteCache
                    else -> deleteFiles
                }

                if (delete) {
                    try {
                        if (f.delete()) {
                            deletedBytes += len
                            deletedCount++
                        }
                    } catch (_: Exception) {}
                }
            }

            if (deleteAvatars) {
                AvatarManager.invalidateAvatarFileCache()
                AvatarManager.clearAvatarRadiusCache()
            }

            if (deleteCache) {
                try {
                    if (cacheDir != null && cacheDir.exists()) {
                        cacheDir.walkTopDown().filter { it.isFile }.forEach { cf ->
                            val len = cf.length()
                            if (cf.delete()) {
                                deletedBytes += len
                                deletedCount++
                            }
                        }
                    }
                    val extCache = externalCacheDir
                    if (extCache != null && extCache.exists()) {
                        extCache.walkTopDown().filter { it.isFile }.forEach { cf ->
                            val len = cf.length()
                            if (cf.delete()) {
                                deletedBytes += len
                                deletedCount++
                            }
                        }
                    }
                    Glide.get(applicationContext).clearDiskCache()
                    withContext(Dispatchers.Main) {
                        try {
                            WebStorage.getInstance().deleteAllData()
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    try {
                        Glide.get(applicationContext).clearMemory()
                    } catch (_: Exception) {}

                    if (deletePhotos) b.cbStoragePhotos.isChecked = false
                    if (deleteVideos) b.cbStorageVideos.isChecked = false
                    if (deleteAudio) b.cbStorageAudio.isChecked = false
                    if (deleteGifs) b.cbStorageGifs.isChecked = false
                    if (deleteFiles) b.cbStorageFiles.isChecked = false
                    if (deleteAvatars) b.cbStorageAvatars.isChecked = false
                    if (deleteCache) b.cbStorageCache.isChecked = false

                    PrimeNotification.show(
                        this@SettingsStorageActivity,
                        "Очищено: $deletedCount файлов (${SettingsSupport.formatStorageSize(deletedBytes)})"
                    )
                    calculateStorageAsync()
                }
            }
        }
    }
}
