package com.messenger.prime

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.messenger.prime.databinding.ActivityWallpaperEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class WallpaperEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperEditorBinding
    private var slidrInterface: com.r0adkll.slidr.model.SlidrInterface? = null

    private var targetContactKey: String? = null
    private var targetContactName: String? = null

    private var selectedMode = "PATTERN" // "PATTERN", "MATERIAL", "IMAGE"
    private var selectedPatternType = "emojis"
    private var selectedOrientation = "grid" // "grid", "staggered", "spiral"
    private var selectedColor = "#151D2A"
    private var selectedAnimation = "float" // "none", "float", "pulse", "rotate"
    private var selectedImageLocalPath: String? = null

    private var photoScale = 1.0f
    private var photoDx = 0f
    private var photoDy = 0f
    private var photoRotation = 0f
    private var photoDimOpacity = 0.2f
    private var loadedBitmap: Bitmap? = null

    private data class PatternChoice(
        val type: String,
        val label: String,
        val emojis: String
    )

    private val patternChoices = listOf(
        PatternChoice("emojis", "Смайлики", "😎 ⭐ 🌸 🔷"),
        PatternChoice("stars", "Звёзды", "⭐ ✨ 🌟 💫"),
        PatternChoice("hearts", "Сердечки", "❤️ 💖 💙 💜"),
        PatternChoice("cats", "Котики", "🐱 🐶 🦊 🐼"),
        PatternChoice("space", "Космос", "🚀 🛸 👾 🎮"),
        PatternChoice("food", "Вкусняшки", "🍕 🍔 🍦 ☕"),
        PatternChoice("party", "Праздник", "🎉 🎈 🎁 🎨"),
        PatternChoice("flowers", "Цветы", "🌸 🌺 🌻 🌼")
    )

    private data class MaterialStyleChoice(
        val type: String,
        val label: String
    )

    private val materialStyleChoices = listOf(
        MaterialStyleChoice("material_all", "Вселенная"),
        MaterialStyleChoice("material_geo", "Геометрия"),
        MaterialStyleChoice("material_stars", "Звёзды & Космос"),
        MaterialStyleChoice("material_waves", "Волны & Овалы")
    )

    private data class OrientationChoice(
        val key: String,
        val label: String
    )

    private val orientationChoices = listOf(
        OrientationChoice("grid", "Сетка"),
        OrientationChoice("staggered", "Шахматка"),
        OrientationChoice("spiral", "Диагональ")
    )

    private data class ColorChoice(
        val hex: String,
        val label: String
    )

    private val colorChoices = listOf(
        ColorChoice("#151D2A", "Авто (Тема)"),
        ColorChoice("#F1F5F9", "Светлый"),
        ColorChoice("#0F172A", "Полночный"),
        ColorChoice("#090D16", "Глубокая ночь"),
        ColorChoice("#1E1B2E", "Фиолетовый"),
        ColorChoice("#062C22", "Изумрудный"),
        ColorChoice("#261C14", "Шоколадный")
    )

    private data class AnimationChoice(
        val key: String,
        val label: String
    )

    private val animationChoices = listOf(
        AnimationChoice("none", "Без анимации"),
        AnimationChoice("float", "Парение"),
        AnimationChoice("pulse", "Пульсация"),
        AnimationChoice("rotate", "Вращение")
    )

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveImageFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorAccentManager.applyAccentToActivity(this)

        slidrInterface = null
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityWallpaperEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetContactKey = intent.getStringExtra(WallpaperManager.EXTRA_CONTACT_KEY)
        targetContactName = intent.getStringExtra(WallpaperManager.EXTRA_CONTACT_NAME)

        if (!targetContactName.isNullOrBlank()) {
            binding.tvHeaderSubtitle.text = "Для чата: $targetContactName"
            binding.tvPreviewChatName.text = targetContactName
            try {
                val avatarUri = AvatarManager.getContactAvatarUriOrFile(this, targetContactName, targetContactName, null)
                if (!avatarUri.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(this).load(avatarUri).centerCrop().into(binding.ivPreviewAvatar)
                }
            } catch (_: Exception) {}
        } else {
            binding.tvHeaderSubtitle.text = "Общие обои для чатов"
            binding.tvPreviewChatName.text = "Анна"
        }

        val rootContainer = findViewById<ViewGroup>(R.id.rootContainer)
        rootContainer?.post {
            PrimeTransitions.protectHorizontalScrollViews(rootContainer, slidrInterface)
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        binding.btnSave.backgroundTintList = accentTint
        binding.cardLivePreview.strokeColor = accentColor
        binding.inputLayoutWallpaperName.boxStrokeColor = accentColor
        binding.inputLayoutCustomEmojis.boxStrokeColor = accentColor

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        binding.btnPickGallery.setOnClickListener {
            pickMediaLauncher.launch("image/*")
        }

        binding.btnResetPhotoTransform.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            photoScale = 1.0f
            photoRotation = 0f
            photoDx = 0f
            photoDy = 0f
            photoDimOpacity = 0.2f

            binding.sliderPhotoZoom.value = 1.0f
            binding.sliderPhotoRotation.value = 0f
            binding.sliderPhotoDim.value = 0.2f

            updatePreviewUI()
        }

        // Mode Tab Buttons
        binding.btnTabEmoji.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            selectedMode = "PATTERN"
            if (selectedPatternType.startsWith("material_")) {
                selectedPatternType = "emojis"
            }
            updatePreviewUI()
        }

        binding.btnTabMaterial.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            selectedMode = "MATERIAL"
            if (!selectedPatternType.startsWith("material_")) {
                selectedPatternType = "material_all"
            }
            updatePreviewUI()
        }

        binding.btnTabGallery.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            if (selectedImageLocalPath.isNullOrEmpty()) {
                pickMediaLauncher.launch("image/*")
            } else {
                selectedMode = "IMAGE"
                updatePreviewUI()
            }
        }

        // Photo Sliders
        binding.sliderPhotoZoom.addOnChangeListener { _, value, _ ->
            photoScale = value
            updatePreviewUI()
        }

        binding.sliderPhotoRotation.addOnChangeListener { _, value, _ ->
            photoRotation = value
            updatePreviewUI()
        }

        binding.sliderPhotoDim.addOnChangeListener { _, value, _ ->
            photoDimOpacity = value
            updatePreviewUI()
        }

        // Custom Emojis Watcher
        binding.etCustomEmojis.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (selectedMode == "PATTERN") {
                    updatePreviewUI()
                }
            }
        })

        setupPhotoTouchGestures()
        setupChips()
        updatePreviewUI()

        binding.btnSave.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            saveAndFinish()
        }
    }

    private fun setupPhotoTouchGestures() {
        var lastX = 0f
        var lastY = 0f

        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (selectedMode != "IMAGE") return false
                photoScale = (photoScale * detector.scaleFactor).coerceIn(1.0f, 3.0f)
                binding.sliderPhotoZoom.value = photoScale
                updatePreviewUI()
                return true
            }
        })

        binding.cardLivePreview.setOnTouchListener { v: View, event: MotionEvent ->
            if (selectedMode != "IMAGE") return@setOnTouchListener false
            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        photoDx += dx
                        photoDy += dy
                        lastX = event.x
                        lastY = event.y
                        updatePreviewUI()
                    }
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            true
        }
    }

    private fun setupChips() {
        setupPatternChips()
        setupMaterialChips()
        setupOrientationChips()
        setupColorChips()
        setupAnimationChips()
    }

    private fun setupPatternChips() {
        binding.layoutPatternChips.removeAllViews()

        patternChoices.forEach { choice ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = choice.label
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                isFocusable = true
                chipCornerRadius = 12 * resources.displayMetrics.density

                setOnClickListener {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    selectedMode = "PATTERN"
                    selectedPatternType = choice.type
                    binding.etCustomEmojis.setText(choice.emojis)
                    updateChipsSelection()
                    updatePreviewUI()
                }
            }
            binding.layoutPatternChips.addView(chip)
        }
    }

    private fun setupMaterialChips() {
        binding.layoutMaterialChips.removeAllViews()

        materialStyleChoices.forEach { choice ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = choice.label
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                isFocusable = true
                chipCornerRadius = 12 * resources.displayMetrics.density

                setOnClickListener {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    selectedMode = "MATERIAL"
                    selectedPatternType = choice.type
                    updateChipsSelection()
                    updatePreviewUI()
                }
            }
            binding.layoutMaterialChips.addView(chip)
        }
    }

    private fun setupOrientationChips() {
        binding.layoutOrientationChips.removeAllViews()

        orientationChoices.forEach { choice ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = choice.label
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                isFocusable = true
                chipCornerRadius = 12 * resources.displayMetrics.density

                setOnClickListener {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    selectedOrientation = choice.key
                    updateChipsSelection()
                    updatePreviewUI()
                }
            }
            binding.layoutOrientationChips.addView(chip)
        }
    }

    private fun setupColorChips() {
        binding.layoutColorChips.removeAllViews()

        colorChoices.forEach { choice ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = choice.label
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                isFocusable = true
                chipCornerRadius = 12 * resources.displayMetrics.density

                setOnClickListener {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    selectedColor = choice.hex
                    updateChipsSelection()
                    updatePreviewUI()
                }
            }
            binding.layoutColorChips.addView(chip)
        }
    }

    private fun setupAnimationChips() {
        binding.layoutAnimationChips.removeAllViews()

        animationChoices.forEach { choice ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = choice.label
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                isFocusable = true
                chipCornerRadius = 12 * resources.displayMetrics.density

                setOnClickListener {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    selectedAnimation = choice.key
                    updateChipsSelection()
                    updatePreviewUI()
                }
            }
            binding.layoutAnimationChips.addView(chip)
        }
    }

    private fun updateChipsSelection() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val density = resources.displayMetrics.density

        fun styleChips(container: com.google.android.material.chip.ChipGroup, isSelectedBlock: (Int) -> Boolean) {
            for (i in 0 until container.childCount) {
                val chip = container.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                val selected = isSelectedBlock(i)

                chip.isChecked = selected

                chip.chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                    intArrayOf(accentColor, ContextCompat.getColor(this@WallpaperEditorActivity, R.color.prime_surface))
                )
                chip.setTextColor(if (selected) android.graphics.Color.WHITE else ContextCompat.getColor(this@WallpaperEditorActivity, R.color.prime_text_primary))
                chip.chipStrokeColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                    intArrayOf(android.graphics.Color.TRANSPARENT, ContextCompat.getColor(this@WallpaperEditorActivity, R.color.prime_divider))
                )
                chip.chipStrokeWidth = if (selected) 0f else (1 * density)
            }
        }

        styleChips(binding.layoutPatternChips) { i -> patternChoices.getOrNull(i)?.type == selectedPatternType && selectedMode == "PATTERN" }
        styleChips(binding.layoutMaterialChips) { i -> materialStyleChoices.getOrNull(i)?.type == selectedPatternType && selectedMode == "MATERIAL" }
        styleChips(binding.layoutOrientationChips) { i -> orientationChoices.getOrNull(i)?.key == selectedOrientation }
        styleChips(binding.layoutColorChips) { i -> colorChoices.getOrNull(i)?.hex == selectedColor }
        styleChips(binding.layoutAnimationChips) { i -> animationChoices.getOrNull(i)?.key == selectedAnimation }

        val updateBtn = { btn: com.google.android.material.button.MaterialButton, isSelected: Boolean ->
            btn.strokeColor = ColorStateList.valueOf(if (isSelected) accentColor else ContextCompat.getColor(this, R.color.prime_divider))
            btn.strokeWidth = if (isSelected) (2 * density).toInt() else (1 * density).toInt()
        }

        updateBtn(binding.btnTabEmoji, selectedMode == "PATTERN")
        updateBtn(binding.btnTabMaterial, selectedMode == "MATERIAL")
        updateBtn(binding.btnTabGallery, selectedMode == "IMAGE")
    }

    private fun saveImageFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val time = System.currentTimeMillis()
                val localFile = File(filesDir, "wallpaper_img_${time}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                withContext(Dispatchers.Main) {
                    selectedImageLocalPath = localFile.absolutePath
                    loadedBitmap = bmp
                    selectedMode = "IMAGE"
                    updatePreviewUI()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePreviewUI() {
        updateChipsSelection()

        // Update Slider labels
        binding.tvLabelPhotoZoom.text = "Масштаб (Zoom): ${String.format(Locale.US, "%.2fx", photoScale)}"
        binding.tvLabelPhotoRotation.text = "Поворот (Rotation): ${photoRotation.toInt()}°"
        binding.tvLabelPhotoDim.text = "Затемнение фона: ${(photoDimOpacity * 100).toInt()}%"

        // Smooth layout transitions
        android.transition.TransitionManager.beginDelayedTransition(
            findViewById<ViewGroup>(R.id.rootContainer),
            android.transition.AutoTransition().apply { duration = 200 }
        )

        when (selectedMode) {
            "IMAGE" -> {
                binding.layoutPhotoControls.visibility = View.VISIBLE
                binding.layoutMaterialControls.visibility = View.GONE
                binding.layoutEmojiControls.visibility = View.GONE

                binding.vPhotoDimOverlay.visibility = View.VISIBLE
                binding.vPhotoDimOverlay.alpha = photoDimOpacity

                if (!selectedImageLocalPath.isNullOrEmpty() && loadedBitmap == null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val bmp = BitmapFactory.decodeFile(selectedImageLocalPath!!)
                        withContext(Dispatchers.Main) {
                            loadedBitmap = bmp
                            updatePreviewUI()
                        }
                    }
                }
            }
            "MATERIAL" -> {
                binding.layoutPhotoControls.visibility = View.GONE
                binding.layoutMaterialControls.visibility = View.VISIBLE
                binding.layoutEmojiControls.visibility = View.GONE

                binding.vPhotoDimOverlay.visibility = View.GONE
            }
            else -> {
                // "PATTERN"
                binding.layoutPhotoControls.visibility = View.GONE
                binding.layoutMaterialControls.visibility = View.GONE
                binding.layoutEmojiControls.visibility = View.VISIBLE

                binding.vPhotoDimOverlay.visibility = View.GONE
            }
        }

        // Create temp wallpaper for live canvas rendering
        val customEmojisText = binding.etCustomEmojis.text?.toString().let {
            if (it.isNullOrBlank()) "😎 ⭐ 🌸 🔷" else it
        }

        val tempWallpaper = ChatWallpaper(
            id = "preview_temp",
            name = "Preview",
            type = selectedMode,
            imagePath = selectedImageLocalPath,
            patternType = selectedPatternType,
            customEmojis = customEmojisText,
            backgroundColor = selectedColor,
            layoutOrientation = selectedOrientation,
            scale = photoScale,
            offsetX = photoDx,
            offsetY = photoDy,
            rotation = photoRotation,
            dimOpacity = photoDimOpacity,
            animationEffect = selectedAnimation,
            isAnimated = selectedAnimation != "none"
        )

        binding.vPreviewWallpaper.setLiveTempWallpaper(tempWallpaper, loadedBitmap)

        // Update Info Badge
        val colorName = colorChoices.find { it.hex == selectedColor }?.label ?: "Темный"
        val animName = animationChoices.find { it.key == selectedAnimation }?.label ?: "Без анимации"

        val badgeText = when (selectedMode) {
            "IMAGE" -> "🖼️ Фото • Zoom ${String.format(Locale.US, "%.1fx", photoScale)} • Dim ${(photoDimOpacity * 100).toInt()}%"
            "MATERIAL" -> "🔷 Material 3 • ${materialStyleChoices.find { it.type == selectedPatternType }?.label ?: "Вселенная"}"
            else -> "✨ Смайлы • $animName • $colorName"
        }
        binding.tvPreviewInfoBadge.text = badgeText

        val rootContainer = findViewById<ViewGroup>(R.id.rootContainer)
        rootContainer?.post {
            PrimeTransitions.protectHorizontalScrollViews(rootContainer, slidrInterface)
        }
    }

    private fun saveAndFinish() {
        val name = binding.etWallpaperName.text?.toString()?.trim().let {
            if (it.isNullOrBlank()) "Мои Обои" else it
        }

        val time = System.currentTimeMillis()
        val id = "wp_$time"

        val wallpaper = ChatWallpaper(
            id = id,
            name = name,
            type = selectedMode,
            imagePath = if (selectedMode == "IMAGE") selectedImageLocalPath else null,
            patternType = if (selectedMode == "PATTERN") selectedPatternType else selectedPatternType,
            customEmojis = binding.etCustomEmojis.text?.toString(),
            backgroundColor = selectedColor,
            layoutOrientation = selectedOrientation,
            scale = photoScale,
            offsetX = photoDx,
            offsetY = photoDy,
            rotation = photoRotation,
            dimOpacity = photoDimOpacity,
            animationEffect = selectedAnimation,
            isAnimated = selectedAnimation != "none",
            createdAt = time
        )

        WallpaperManager.saveCustomWallpaper(this, wallpaper)
        WallpaperManager.setActiveWallpaper(this, id, targetContactKey)

        sendBroadcast(Intent(WallpaperManager.ACTION_WALLPAPER_CHANGED).setPackage(packageName))

        setResult(Activity.RESULT_OK)
        finish()
        PrimeTransitions.applyCloseTransition(this)
    }
}
