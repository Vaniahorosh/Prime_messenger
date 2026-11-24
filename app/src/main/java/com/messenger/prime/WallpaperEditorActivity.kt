package com.messenger.prime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.messenger.prime.databinding.ActivityWallpaperEditorBinding
import com.messenger.prime.shapes.M3EShapes
import eightbitlab.com.blurview.BlurView
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
    private var isSyncingPhotoSlider = false
    private var activePhotoPickerSheet: PhotoPickerBottomSheet? = null

    private var lastAppliedStatusBarLight: Boolean? = null

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            activePhotoPickerSheet?.refreshMedia()
        }

    private data class PatternChoice(
        val type: String,
        val label: String,
        val emojis: String
    )

    private val patternChoices = listOf(
        PatternChoice("emojis", "Смайлики", "😎 ⭐ 🌸 🔷"),
        PatternChoice("stars", "Звёзды", "⭐ ✨ 🌟 💫"),
        PatternChoice("hearts", "Сердечки", "❤️ 💖 💙 💜"),
        PatternChoice("cats", "Зверята", "🐱 🐶 🦊 🐼"),
        PatternChoice("space", "Космос", "🚀 🛸 👾 🎮"),
        PatternChoice("food", "Еда", "🍕 🍔 🍦 ☕"),
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
        MaterialStyleChoice("material_stars", "Звёзды и космос"),
        MaterialStyleChoice("material_waves", "Волны и овалы")
    )

    private data class ColorChoice(
        val hex: String,
        val label: String
    )

    private val colorChoices = listOf(
        ColorChoice("#151D2A", "По теме"),
        ColorChoice("#F1F5F9", "Светлый"),
        ColorChoice("#0F172A", "Полночь"),
        ColorChoice("#090D16", "Обсидиан"),
        ColorChoice("#0C2438", "Глубина"),
        ColorChoice("#1E1B2E", "Сумерки"),
        ColorChoice("#062C22", "Изумруд"),
        ColorChoice("#261C14", "Эспрессо")
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

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                saveImageFromUri(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

        slidrInterface = null
        val isDark = SettingsSupport.isDarkThemeActive(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge(isDarkIcons = !isDark)

        binding = ActivityWallpaperEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetContactKey = intent.getStringExtra(WallpaperManager.EXTRA_CONTACT_KEY)
        targetContactName = intent.getStringExtra(WallpaperManager.EXTRA_CONTACT_NAME)

        if (!targetContactName.isNullOrBlank()) {
            binding.tvHeaderSubtitle.text = "Чат: $targetContactName"
        } else {
            binding.tvHeaderSubtitle.text = "Общие обои"
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        setupToolbarBlur(isDark)
        setupM3EChrome(accentColor, accentTint, isDark)
        setupInsetsAndStageSize()
        setupListeners()
        setupPhotoTouchGestures()
        setupChipsAndSwatches()
        updatePreviewUI(animateTransition = false)

        SettingsSupport.animateStaggeredEntrance(
            this,
            listOf(
                binding.cardModeSelector,
                binding.cardEmojiControls,
                binding.cardColorControls,
                binding.cardAnimationControls,
                binding.cardWallpaperName
            )
        )

        binding.rootContainer.post {
            PrimeTransitions.protectHorizontalScrollViews(binding.rootContainer, slidrInterface)
        }
    }

    private fun setupToolbarBlur(isDark: Boolean) {
        val baseColor = ContextCompat.getColor(this, R.color.prime_base)
        val overlayColor = if (isDark) "#730F172A".toColorInt() else "#99FFFFFF".toColorInt()
        val frameClear = window.decorView.background ?: baseColor.toDrawable()

        val applyBlur = { blurView: BlurView ->
            blurView.setupBlur(
                rootView = binding.rootContainer,
                blurRadius = 20f,
                tintColor = overlayColor,
                frameClearDrawable = frameClear
            )
        }
        applyBlur(binding.blurEditorBack)
        applyBlur(binding.blurEditorTitle)
        applyBlur(binding.blurEditorSave)
    }

    private fun setupM3EChrome(accentColor: Int, accentTint: ColorStateList, isDark: Boolean) {
        binding.btnBack.tag = "do_not_tint"
        binding.btnBack.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        binding.btnBack.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.prime_text_primary))

        binding.btnSave.tag = "do_not_tint"
        binding.btnSave.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        binding.btnSave.iconTint = accentTint

        binding.bubblePreviewOutgoing.backgroundTintList = accentTint
        binding.btnPickGallery.backgroundTintList = accentTint

        binding.sliderPhotoZoom.thumbTintList = accentTint
        binding.sliderPhotoZoom.trackActiveTintList = accentTint
        binding.sliderPhotoRotation.thumbTintList = accentTint
        binding.sliderPhotoRotation.trackActiveTintList = accentTint
        binding.sliderPhotoDim.thumbTintList = accentTint
        binding.sliderPhotoDim.trackActiveTintList = accentTint

        SettingsSupport.applyM3EBadgeIcon(binding.ivIconEmojiSection, accentColor, M3EShapes.cookie6Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(binding.ivIconMaterialSection, accentColor, M3EShapes.gem, isDark)
        SettingsSupport.applyM3EBadgeIcon(binding.ivIconColorSection, accentColor, M3EShapes.sunny, isDark)
        SettingsSupport.applyM3EBadgeIcon(binding.ivIconAnimationSection, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(binding.ivIconPhotoSection, accentColor, M3EShapes.cookie4Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(binding.ivIconNameSection, accentColor, M3EShapes.pentagon, isDark)

        binding.tvTitleEmojiSection.setTextColor(accentColor)
        binding.tvTitleMaterialSection.setTextColor(accentColor)
        binding.tvTitleColorSection.setTextColor(accentColor)
        binding.tvTitleAnimationSection.setTextColor(accentColor)
        binding.tvTitlePhotoSection.setTextColor(accentColor)
        binding.tvTitleNameSection.setTextColor(accentColor)
    }

    private fun setupInsetsAndStageSize() {
        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels
        val defaultStageHeight = (screenHeight * 0.40f).toInt()
            .coerceIn((256 * density).toInt(), (368 * density).toInt())
        val imeStageHeight = (screenHeight * 0.24f).toInt()
            .coerceIn((184 * density).toInt(), (240 * density).toInt())

        val initialTopMargin = getFallbackStatusBarHeight() + (8 * density).toInt()
        (binding.layoutEditorToolbarRow.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.topMargin = initialTopMargin
            binding.layoutEditorToolbarRow.layoutParams = lp
        }
        binding.layoutPreviewStage.layoutParams = binding.layoutPreviewStage.layoutParams.apply {
            height = defaultStageHeight
        }

        var latestTopMargin = initialTopMargin
        val syncPreviewOverlayTopPadding = {
            val toolbarHeight = binding.layoutEditorToolbarRow.height.takeIf { it > 0 } ?: (52 * density).toInt()
            val topPad = latestTopMargin + toolbarHeight + (10 * density).toInt()
            if (binding.layoutPreviewChatOverlay.paddingTop != topPad) {
                binding.layoutPreviewChatOverlay.updatePadding(top = topPad)
            }
        }

        binding.layoutEditorToolbarRow.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            syncPreviewOverlayTopPadding()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            binding.rootContainer.setPadding(cutout.left, 0, cutout.right, 0)

            val safeTop = maxOf(systemBars.top, cutout.top).takeIf { it > 0 } ?: getFallbackStatusBarHeight()
            latestTopMargin = safeTop + (8 * density).toInt()

            (binding.layoutEditorToolbarRow.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.topMargin != latestTopMargin) {
                    lp.topMargin = latestTopMargin
                    binding.layoutEditorToolbarRow.layoutParams = lp
                }
            }

            val desiredStageHeight = if (imeVisible) imeStageHeight else defaultStageHeight
            binding.layoutPreviewStage.layoutParams.let { lp ->
                if (lp.height != desiredStageHeight) {
                    lp.height = desiredStageHeight
                    binding.layoutPreviewStage.layoutParams = lp
                }
            }

            val bottomInset = maxOf(systemBars.bottom, cutout.bottom, ime.bottom)
            binding.nestedScrollView.updatePadding(
                top = (16 * density).toInt(),
                bottom = bottomInset + (28 * density).toInt()
            )
            syncPreviewOverlayTopPadding()
            insets
        }
        ViewCompat.requestApplyInsets(binding.rootContainer)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        binding.btnSave.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            saveAndFinish()
        }

        binding.btnPickGallery.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            showWallpaperPhotoPicker()
        }

        binding.btnResetPhotoTransform.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            photoScale = 1.0f
            photoRotation = 0f
            photoDx = 0f
            photoDy = 0f
            photoDimOpacity = 0.2f

            binding.sliderPhotoZoom.value = 1.0f
            binding.sliderPhotoRotation.value = 0f
            binding.sliderPhotoDim.value = 0.2f

            updatePreviewUI(animateTransition = false)
        }

        binding.btnClearEmojis.setOnClickListener {
            binding.etCustomEmojis.setText("")
        }

        binding.btnClearName.setOnClickListener {
            binding.etWallpaperName.setText("")
        }

        binding.btnTabEmoji.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectedMode = "PATTERN"
            if (selectedPatternType.startsWith("material_")) {
                selectedPatternType = "emojis"
            }
            updatePreviewUI(animateTransition = true)
        }

        binding.btnTabMaterial.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectedMode = "MATERIAL"
            if (!selectedPatternType.startsWith("material_")) {
                selectedPatternType = "material_all"
            }
            updatePreviewUI(animateTransition = true)
        }

        binding.btnTabGallery.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            if (selectedImageLocalPath.isNullOrEmpty()) {
                showWallpaperPhotoPicker()
            } else {
                selectedMode = "IMAGE"
                updatePreviewUI(animateTransition = true)
            }
        }

        binding.btnOrientGrid.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectedOrientation = "grid"
            updatePreviewUI(animateTransition = false)
        }

        binding.btnOrientStaggered.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectedOrientation = "staggered"
            updatePreviewUI(animateTransition = false)
        }

        binding.btnOrientSpiral.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selectedOrientation = "spiral"
            updatePreviewUI(animateTransition = false)
        }

        binding.sliderPhotoZoom.addOnChangeListener { _, value, _ ->
            if (isSyncingPhotoSlider) return@addOnChangeListener
            photoScale = value
            updatePreviewUI(animateTransition = false)
        }

        binding.sliderPhotoRotation.addOnChangeListener { _, value, _ ->
            photoRotation = value
            updatePreviewUI(animateTransition = false)
        }

        binding.sliderPhotoDim.addOnChangeListener { _, value, _ ->
            photoDimOpacity = value
            updatePreviewUI(animateTransition = false)
        }

        binding.etCustomEmojis.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClearEmojis.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                if (selectedMode == "PATTERN") {
                    updatePreviewUI(animateTransition = false)
                }
            }
        })

        binding.etWallpaperName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClearName.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
    }

    private fun showWallpaperPhotoPicker() {
        val sheet = PhotoPickerBottomSheet(
            activity = this,
            title = "Фото для обоев",
            allowGif = false,
            showPresets = false,
            onMediaSelected = { uri, _ ->
                activePhotoPickerSheet = null
                saveImageFromUri(uri)
            },
            onOpenSystemPicker = {
                activePhotoPickerSheet = null
                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRequestPermission = {
                galleryPermissionLauncher.launch(PhotoPickerBottomSheet.getRequiredImagePermissions())
            },
            onDismissWithoutSelection = {
                activePhotoPickerSheet = null
            }
        )
        activePhotoPickerSheet = sheet
        sheet.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPhotoTouchGestures() {
        var lastX = 0f
        var lastY = 0f
        var activePointerId = MotionEvent.INVALID_POINTER_ID

        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (selectedMode != "IMAGE") return false
                    val factor = detector.scaleFactor
                    if (!factor.isFinite() || factor <= 0f) return false
                    val newScale = (photoScale * factor).coerceIn(
                        binding.sliderPhotoZoom.valueFrom,
                        binding.sliderPhotoZoom.valueTo
                    )
                    photoScale = newScale
                    isSyncingPhotoSlider = true
                    try {
                        binding.sliderPhotoZoom.value = newScale
                    } finally {
                        isSyncingPhotoSlider = false
                    }
                    updatePreviewUI(animateTransition = false)
                    return true
                }
            }
        )

        binding.layoutPreviewStage.setOnTouchListener { v: View, event: MotionEvent ->
            if (selectedMode != "IMAGE") return@setOnTouchListener false
            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = event.getPointerId(0)
                    lastX = event.x
                    lastY = event.y
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        val pointerIndex = event.findPointerIndex(activePointerId)
                        if (pointerIndex >= 0) {
                            val x = event.getX(pointerIndex)
                            val y = event.getY(pointerIndex)
                            photoDx += x - lastX
                            photoDy += y - lastY
                            lastX = x
                            lastY = y
                            updatePreviewUI(animateTransition = false)
                        }
                    }
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        if (newPointerIndex < event.pointerCount) {
                            lastX = event.getX(newPointerIndex)
                            lastY = event.getY(newPointerIndex)
                            activePointerId = event.getPointerId(newPointerIndex)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    v.performClick()
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_CANCEL -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            true
        }
    }

    private fun setupChipsAndSwatches() {
        setupPatternChips()
        setupMaterialChips()
        setupColorSwatches()
        setupAnimationChips()
    }

    private fun createTonalChip(label: String): Chip {
        val density = resources.displayMetrics.density
        return Chip(this).apply {
            text = label
            isCheckable = true
            isCheckedIconVisible = false
            isClickable = true
            isFocusable = true
            chipCornerRadius = 12f * density
            chipStrokeWidth = 0f
            chipMinHeight = 36f * density
            setEnsureMinTouchTargetSize(false)
            textSize = 13f
        }
    }

    private fun setupPatternChips() {
        binding.layoutPatternChips.removeAllViews()
        patternChoices.forEach { choice ->
            val chip = createTonalChip(choice.label).apply {
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectedMode = "PATTERN"
                    selectedPatternType = choice.type
                    binding.etCustomEmojis.setText(choice.emojis)
                    updatePreviewUI(animateTransition = false)
                }
            }
            binding.layoutPatternChips.addView(chip)
        }
    }

    private fun setupMaterialChips() {
        binding.layoutMaterialChips.removeAllViews()
        materialStyleChoices.forEach { choice ->
            val chip = createTonalChip(choice.label).apply {
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectedMode = "MATERIAL"
                    selectedPatternType = choice.type
                    updatePreviewUI(animateTransition = false)
                }
            }
            binding.layoutMaterialChips.addView(chip)
        }
    }

    private fun setupAnimationChips() {
        binding.layoutAnimationChips.removeAllViews()
        animationChoices.forEach { choice ->
            val chip = createTonalChip(choice.label).apply {
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectedAnimation = choice.key
                    updatePreviewUI(animateTransition = false)
                }
            }
            binding.layoutAnimationChips.addView(chip)
        }
    }

    private fun setupColorSwatches() {
        binding.layoutColorSwatches.removeAllViews()
        val density = resources.displayMetrics.density
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val itemSize = (44 * density).toInt()
        val circleSize = (36 * density).toInt()
        val checkSize = (18 * density).toInt()
        val gap = (8 * density).toInt()

        colorChoices.forEachIndexed { index, choice ->
            val resolvedColor = resolveSwatchDisplayColor(choice.hex, isDark)
            val isLightSwatch = ColorUtils.calculateLuminance(resolvedColor) > 0.55

            val container = FrameLayout(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(itemSize, itemSize).apply {
                    marginEnd = if (index < colorChoices.lastIndex) gap else 0
                }
                contentDescription = choice.label
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectedColor = choice.hex
                    updatePreviewUI(animateTransition = false)
                }
            }

            val circleView = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(circleSize, circleSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(resolvedColor)
                    if (isLightSwatch) {
                        setStroke((1 * density).toInt(), ContextCompat.getColor(this@WallpaperEditorActivity, R.color.prime_divider))
                    }
                }
            }

            val checkIcon = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(checkSize, checkSize, Gravity.CENTER)
                setImageResource(R.drawable.ic_done)
                imageTintList = ColorStateList.valueOf(
                    if (isLightSwatch) "#0F172A".toColorInt() else Color.WHITE
                )
                visibility = View.GONE
            }

            container.addView(circleView)
            container.addView(checkIcon)
            binding.layoutColorSwatches.addView(container)
        }
    }

    private fun resolveSwatchDisplayColor(hex: String, isDark: Boolean): Int {
        return if (hex.equals("#151D2A", ignoreCase = true)) {
            if (isDark) "#0F172A".toColorInt() else "#F1F5F9".toColorInt()
        } else {
            try {
                hex.toColorInt()
            } catch (_: Exception) {
                if (isDark) "#0F172A".toColorInt() else "#F1F5F9".toColorInt()
            }
        }
    }

    private fun updateSelectionStates() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val searchBgColor = ContextCompat.getColor(this, R.color.prime_search_bg)
        val surfaceColor = ContextCompat.getColor(this, R.color.prime_surface)
        val textPrimary = ContextCompat.getColor(this, R.color.prime_text_primary)
        val textSecondary = ContextCompat.getColor(this, R.color.prime_text_secondary)

        val styleTab = { btn: MaterialButton, selected: Boolean ->
            btn.backgroundTintList = ColorStateList.valueOf(if (selected) accentColor else Color.TRANSPARENT)
            val fg = if (selected) Color.WHITE else textSecondary
            btn.setTextColor(fg)
            btn.iconTint = ColorStateList.valueOf(fg)
        }
        styleTab(binding.btnTabEmoji, selectedMode == "PATTERN")
        styleTab(binding.btnTabMaterial, selectedMode == "MATERIAL")
        styleTab(binding.btnTabGallery, selectedMode == "IMAGE")

        val styleOrientationBtn = { btn: MaterialButton, selected: Boolean ->
            btn.backgroundTintList = ColorStateList.valueOf(if (selected) surfaceColor else Color.TRANSPARENT)
            btn.setTextColor(if (selected) accentColor else textSecondary)
        }
        styleOrientationBtn(binding.btnOrientGrid, selectedOrientation == "grid")
        styleOrientationBtn(binding.btnOrientStaggered, selectedOrientation == "staggered")
        styleOrientationBtn(binding.btnOrientSpiral, selectedOrientation == "spiral")

        fun styleChips(container: ChipGroup, isSelectedBlock: (Int) -> Boolean) {
            for (i in 0 until container.childCount) {
                val chip = container.getChildAt(i) as? Chip ?: continue
                val selected = isSelectedBlock(i)
                chip.isChecked = selected
                chip.chipStrokeWidth = 0f
                chip.chipBackgroundColor = ColorStateList.valueOf(if (selected) accentColor else searchBgColor)
                chip.setTextColor(if (selected) Color.WHITE else textPrimary)
            }
        }

        styleChips(binding.layoutPatternChips) { i ->
            patternChoices.getOrNull(i)?.type == selectedPatternType && selectedMode == "PATTERN"
        }
        styleChips(binding.layoutMaterialChips) { i ->
            materialStyleChoices.getOrNull(i)?.type == selectedPatternType && selectedMode == "MATERIAL"
        }
        styleChips(binding.layoutAnimationChips) { i ->
            animationChoices.getOrNull(i)?.key == selectedAnimation
        }

        for (i in 0 until binding.layoutColorSwatches.childCount) {
            val container = binding.layoutColorSwatches.getChildAt(i) as? FrameLayout ?: continue
            val selected = colorChoices.getOrNull(i)?.hex.equals(selectedColor, ignoreCase = true)
            val checkIcon = container.getChildAt(1) as? ImageView
            checkIcon?.visibility = if (selected) View.VISIBLE else View.GONE
            container.animate().scaleX(if (selected) 1.08f else 1f).scaleY(if (selected) 1.08f else 1f).setDuration(140).start()
        }

        binding.tvSelectedColorName.text =
            colorChoices.find { it.hex.equals(selectedColor, ignoreCase = true) }?.label ?: "Свой"
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
                val bmp = decodeSampledBitmap(localFile.absolutePath, 2048)
                withContext(Dispatchers.Main) {
                    selectedImageLocalPath = localFile.absolutePath
                    loadedBitmap = bmp
                    selectedMode = "IMAGE"
                    updatePreviewUI(animateTransition = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun decodeSampledBitmap(path: String, maxDim: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sampleSize = 1
            while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, decodeOpts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun updatePreviewUI(animateTransition: Boolean) {
        updateSelectionStates()

        binding.tvValuePhotoZoom.text = String.format(Locale.US, "%.2f×", photoScale)
        binding.tvValuePhotoRotation.text = "${photoRotation.toInt()}°"
        binding.tvValuePhotoDim.text = "${(photoDimOpacity * 100).toInt()}%"

        if (animateTransition && !PrimeTransitions.areAnimationsDisabled(this)) {
            val transitionSet = TransitionSet().apply {
                addTransition(Fade())
                addTransition(ChangeBounds())
                duration = 180L
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(binding.layoutEditorCardsContainer, transitionSet)
        }

        when (selectedMode) {
            "IMAGE" -> {
                binding.cardPhotoControls.visibility = View.VISIBLE
                binding.cardMaterialControls.visibility = View.GONE
                binding.cardEmojiControls.visibility = View.GONE
                binding.cardColorControls.visibility = View.GONE
                binding.cardAnimationControls.visibility = View.GONE

                val imgPath = selectedImageLocalPath
                if (!imgPath.isNullOrEmpty() && loadedBitmap == null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val bmp = decodeSampledBitmap(imgPath, 2048)
                        withContext(Dispatchers.Main) {
                            loadedBitmap = bmp
                            updatePreviewUI(animateTransition = false)
                        }
                    }
                }
            }
            "MATERIAL" -> {
                binding.cardPhotoControls.visibility = View.GONE
                binding.cardMaterialControls.visibility = View.VISIBLE
                binding.cardEmojiControls.visibility = View.GONE
                binding.cardColorControls.visibility = View.VISIBLE
                binding.cardAnimationControls.visibility = View.GONE
            }
            else -> {
                binding.cardPhotoControls.visibility = View.GONE
                binding.cardMaterialControls.visibility = View.GONE
                binding.cardEmojiControls.visibility = View.VISIBLE
                binding.cardColorControls.visibility = View.VISIBLE
                binding.cardAnimationControls.visibility = View.VISIBLE
            }
        }

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
            scale = if (selectedMode == "IMAGE") photoScale else 1.0f,
            offsetX = if (selectedMode == "IMAGE") photoDx else 0f,
            offsetY = if (selectedMode == "IMAGE") photoDy else 0f,
            rotation = if (selectedMode == "IMAGE") photoRotation else 0f,
            dimOpacity = if (selectedMode == "IMAGE") photoDimOpacity else 0f,
            animationEffect = selectedAnimation,
            isAnimated = selectedMode == "PATTERN" && selectedAnimation != "none"
        )

        binding.vPreviewWallpaper.setLiveTempWallpaper(tempWallpaper, loadedBitmap)
        updateStatusBarIconContrast()
    }

    private fun updateStatusBarIconContrast() {
        val isDarkTheme = SettingsSupport.isDarkThemeActive(this)
        val previewIsLight = when (selectedMode) {
            "IMAGE" -> false
            else -> {
                val bgColor = resolveSwatchDisplayColor(selectedColor, isDarkTheme)
                ColorUtils.calculateLuminance(bgColor) > 0.5
            }
        }
        if (lastAppliedStatusBarLight != previewIsLight) {
            lastAppliedStatusBarLight = previewIsLight
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = previewIsLight
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    private fun getFallbackStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (32 * resources.displayMetrics.density).toInt()
    }

    private fun saveAndFinish() {
        val name = binding.etWallpaperName.text?.toString()?.trim().let {
            if (it.isNullOrBlank()) "Мои обои" else it
        }

        val time = System.currentTimeMillis()
        val id = "wp_$time"

        val wallpaper = ChatWallpaper(
            id = id,
            name = name,
            type = selectedMode,
            imagePath = if (selectedMode == "IMAGE") selectedImageLocalPath else null,
            patternType = selectedPatternType,
            customEmojis = binding.etCustomEmojis.text?.toString(),
            backgroundColor = selectedColor,
            layoutOrientation = selectedOrientation,
            scale = if (selectedMode == "IMAGE") photoScale else 1.0f,
            offsetX = if (selectedMode == "IMAGE") photoDx else 0f,
            offsetY = if (selectedMode == "IMAGE") photoDy else 0f,
            rotation = if (selectedMode == "IMAGE") photoRotation else 0f,
            dimOpacity = if (selectedMode == "IMAGE") photoDimOpacity else 0f,
            animationEffect = selectedAnimation,
            isAnimated = selectedMode == "PATTERN" && selectedAnimation != "none",
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
