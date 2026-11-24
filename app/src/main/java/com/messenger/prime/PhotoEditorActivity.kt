package com.messenger.prime

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider
import com.messenger.prime.databinding.ActivityPhotoEditorContentBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import com.messenger.prime.shapes.M3EShapes
import eightbitlab.com.blurview.BlurView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.sqrt

class PhotoEditorActivity : AppCompatActivity() {

    private var binding: ActivityPhotoEditorContentBinding? = null
    private var originalBitmap: Bitmap? = null
    private var activePhotoPickerSheet: PhotoPickerBottomSheet? = null

    // Фильтры
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    private var temperature = 0f
    private var tint = 0f

    // Трансформация
    private var rotationAngle = 0f
    private var perspectiveFactor = 0f
    private var isMirrored = false
    private var isSyncingScaleSlider = false

    // Кисть
    private var brushSize = 20f
    private var brushHardness = 1.0f
    private var currentBrushColor = Color.RED
    private var currentToolIndex = -1
    private var isOriginalLayoutDismissed = false
    private var isImeCurrentlyVisible = false

    private var isProfilePhoto = false
    private var isStickerMode = false
    private var isCropFrameActive = false
    private var selectedAspectIndex = 3
    private var contactNameExtra: String? = null
    private var isHdQuality = true
    @Volatile
    private var isSaving = false

    // Матрицы для трансформации фото
    private val mainMatrix = Matrix()
    private val baselineMatrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var oldDist = 1f
    private var mode = NONE

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val PIPETTE = 3

        private const val MIN_ZOOM = 0.15f
        private const val MAX_ZOOM = 5.0f
    }

    private var isGifMode = false
    private var originalUri: Uri? = null

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            activePhotoPickerSheet?.refreshMedia()
        }

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                binding?.let { loadMediaUriIntoEditor(it, uri) }
            } else if (originalUri == null) {
                finish()
            }
        }

    private fun updateBackCallbackState() {
        backCallback.isEnabled = (currentToolIndex != -1 || mode == PIPETTE)
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (mode == PIPETTE) {
                mode = NONE
                updateBackCallbackState()
                return
            }
            if (currentToolIndex != -1) {
                binding?.let { b ->
                    val activeGroup = when (currentToolIndex) {
                        0 -> b.layoutGroupLight
                        1 -> b.layoutGroupGeometry
                        2 -> b.layoutGroupBrush
                        else -> null
                    }
                    if (activeGroup != null) {
                        toggleToolGroup(b, activeGroup, defaultTitle())
                    }
                }
                updateBackCallbackState()
            }
        }
    }

    private val chatDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val navIntent = Intent(this@PhotoEditorActivity, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(navIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backCallback)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge(isDarkIcons = false)

        val b = ActivityPhotoEditorContentBinding.inflate(layoutInflater)
        binding = b
        setContentView(b.root)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        isProfilePhoto = intent.getBooleanExtra("IS_PROFILE_PHOTO", false)
        isStickerMode = intent.getBooleanExtra("IS_STICKER_MODE", false)
        isCropFrameActive = isProfilePhoto || isStickerMode
        selectedAspectIndex = if (isCropFrameActive) 0 else 3
        contactNameExtra = intent.getStringExtra("EXTRA_CONTACT_NAME")
        isHdQuality = intent.getBooleanExtra("IS_HD_QUALITY", true)
        val initialCaption = intent.getStringExtra("EXTRA_CAPTION")

        setupBlurChrome(b)
        setupTitleSwitcher(b)
        setupAspectSelector(b)
        setupBrushQuickSwatches(b)
        applyAccentColors(b)
        setupInsetsAndHeights(b)
        setCropFrameVisible(b, isCropFrameActive)

        if (isProfilePhoto) {
            b.tsEditorTitle.setText("Аватар профиля")
            b.layoutCaptionContainer.visibility = View.GONE
            b.btnHdQualityToggle.visibility = View.GONE
            b.aspectSelectorContainer.visibility = View.GONE
        } else if (isStickerMode) {
            b.tsEditorTitle.setText("Редактор стикера")
            b.layoutCaptionContainer.visibility = View.GONE
            b.btnHdQualityToggle.visibility = View.GONE
            b.aspectSelectorContainer.visibility = View.VISIBLE
            applyAspectRatio(b, "1:1")
        } else {
            b.tsEditorTitle.setText(defaultTitle())
            b.layoutCaptionContainer.visibility = View.VISIBLE
            b.btnHdQualityToggle.visibility = View.VISIBLE
            if (!initialCaption.isNullOrBlank()) {
                b.etCaption.setText(initialCaption)
            }
        }

        setupListeners(b)
        updateNumericLabels(b)
        updateToolHighlight(b)

        val uriString = intent.getStringExtra("EXTRA_IMAGE_URI") ?: intent.dataString
        if (!uriString.isNullOrBlank()) {
            loadMediaUriIntoEditor(b, Uri.parse(uriString))
        } else {
            b.root.post { showPhotoPicker(b) }
        }

        val filter = IntentFilter("com.messenger.prime.CHAT_DELETED")
        ContextCompat.registerReceiver(this, chatDeletedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        binding?.let { toggleChromeBlurAutoUpdate(it, true) }
    }

    override fun onPause() {
        binding?.let { toggleChromeBlurAutoUpdate(it, false) }
        super.onPause()
    }

    override fun onDestroy() {
        try { unregisterReceiver(chatDeletedReceiver) } catch (_: Exception) {}
        try {
            originalBitmap?.recycle()
            originalBitmap = null
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun defaultTitle(): String {
        return when {
            isProfilePhoto -> "Аватар профиля"
            isStickerMode -> "Редактор стикера"
            !contactNameExtra.isNullOrBlank() -> contactNameExtra!!
            else -> "Редактор"
        }
    }

    private fun setupBlurChrome(b: ActivityPhotoEditorContentBinding) {
        val overlayDark = "#990B0F17".toColorInt()
        val overlayPanel = "#B30E131D".toColorInt()
        val bg = window.decorView.background

        val pills = listOf(
            b.blurTopBack,
            b.blurTopTitle,
            b.blurTopPick,
            b.topActionsLayout,
            b.blurSetOriginal,
            b.blurCaptionBox,
            b.toolsLayout
        )
        pills.forEach { blurView ->
            blurView.setupBlur(b.photoEditorRoot, 20f, overlayDark, bg)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                blurView.applyM3EPathProvider(M3EShapes::pill)
            }
        }

        b.groupSlidersContainer.setupBlur(b.photoEditorRoot, 22f, overlayPanel, bg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            b.btnDone.applyM3EPathProvider(M3EShapes::pill)
        }
    }

    private fun toggleChromeBlurAutoUpdate(b: ActivityPhotoEditorContentBinding, enabled: Boolean) {
        listOf<BlurView>(
            b.blurTopBack,
            b.blurTopTitle,
            b.blurTopPick,
            b.topActionsLayout,
            b.blurSetOriginal,
            b.groupSlidersContainer,
            b.blurCaptionBox,
            b.toolsLayout
        ).forEach { setBlurAutoUpdateSafe(it, enabled) }
    }

    private fun setupInsetsAndHeights(b: ActivityPhotoEditorContentBinding) {
        val density = resources.displayMetrics.density
        val maxPanelHeight = (resources.displayMetrics.heightPixels * 0.34f).toInt()
            .coerceIn((200 * density).toInt(), (320 * density).toInt())

        b.scrollToolControls.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            if (v.height > maxPanelHeight && v.layoutParams.height != maxPanelHeight) {
                v.post {
                    if (v.height > maxPanelHeight) {
                        v.layoutParams = v.layoutParams.apply { height = maxPanelHeight }
                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(b.photoEditorRoot) { _, windowInsets ->
            val sysBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            val safeTop = maxOf(sysBars.top, cutout.top).takeIf { it > 0 } ?: getStatusBarHeight()
            val safeBottom = maxOf(sysBars.bottom, cutout.bottom, ime.bottom)

            b.photoEditorRoot.updatePadding(left = cutout.left, right = cutout.right)
            b.topBar.updatePadding(top = safeTop + (8 * density).toInt())
            b.bottomStackContainer.updatePadding(bottom = safeBottom + (12 * density).toInt())

            if (isImeCurrentlyVisible != imeVisible) {
                isImeCurrentlyVisible = imeVisible
                if (imeVisible) {
                    b.toolsRow.visibility = View.GONE
                    b.layoutSetOriginal.visibility = View.GONE
                    b.groupSlidersContainer.visibility = View.GONE
                } else {
                    if (!isGifMode) {
                        b.toolsRow.visibility = View.VISIBLE
                        if (currentToolIndex != -1) {
                            b.groupSlidersContainer.visibility = View.VISIBLE
                        } else if (!isOriginalLayoutDismissed) {
                            b.layoutSetOriginal.visibility = View.VISIBLE
                        }
                    } else if (!isOriginalLayoutDismissed) {
                        b.layoutSetOriginal.visibility = View.VISIBLE
                    }
                }
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(b.photoEditorRoot)
    }

    private fun loadMediaUriIntoEditor(b: ActivityPhotoEditorContentBinding, uri: Uri) {
        originalUri = uri
        val uriLower = uri.toString().lowercase(Locale.ROOT)
        val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
        isGifMode = uriLower.endsWith(".gif") ||
            uriLower.contains("gif") ||
            "image/gif".equals(mime, ignoreCase = true) ||
            (uri.scheme == "file" && uri.path != null && File(uri.path!!).name.lowercase(Locale.ROOT).endsWith(".gif"))

        if (isGifMode) {
            b.toolsRow.visibility = View.GONE
            b.toolsLayout.visibility = View.GONE
            b.toolsScroll.visibility = View.GONE
            b.groupSlidersContainer.visibility = View.GONE
            b.aspectSelectorContainer.visibility = View.GONE
            b.cropFrame.visibility = View.GONE
            b.maskTop.visibility = View.GONE
            b.maskBottom.visibility = View.GONE
            b.maskLeft.visibility = View.GONE
            b.maskRight.visibility = View.GONE
            b.drawingView.visibility = View.GONE
            b.topActionsLayout.visibility = View.GONE
            b.tsEditorTitle.setText("GIF Просмотр")
            b.tvMediaInfo.visibility = View.GONE
            b.btnSetOriginal.text = if (isProfilePhoto) "Выбрать этот GIF" else "Отправить GIF"
            b.layoutSetOriginal.visibility = View.VISIBLE

            Glide.with(this)
                .asGif()
                .load(uri)
                .into(b.ivEditorPreview)
        } else {
            brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
            rotationAngle = 0f; perspectiveFactor = 0f; isMirrored = false
            isSyncingScaleSlider = true
            try {
                b.sliderBrightness.value = 0f
                b.sliderContrast.value = 1f
                b.sliderSaturation.value = 1f
                b.sliderTemperature.value = 0f
                b.sliderTint.value = 0f
                b.sliderRotate.value = 0f
                b.sliderPerspective.value = 0f
            } finally {
                isSyncingScaleSlider = false
            }
            b.btnFlipToggle.backgroundTintList = ColorStateList.valueOf("#22FFFFFF".toColorInt())
            b.drawingView.clear()
            updateNumericLabels(b)

            b.toolsRow.visibility = if (isImeCurrentlyVisible) View.GONE else View.VISIBLE
            b.toolsLayout.visibility = View.VISIBLE
            b.toolsScroll.visibility = View.VISIBLE
            b.tsEditorTitle.setText(defaultTitle())
            b.btnSetOriginal.text = when {
                isProfilePhoto -> "Поставить без редактирования"
                isStickerMode -> "Стикер без редактирования"
                else -> "Отправить без редактирования"
            }
            isCropFrameActive = isProfilePhoto || isStickerMode
            selectedAspectIndex = if (isCropFrameActive) 0 else 3
            setCropFrameVisible(b, isCropFrameActive)
            if (!isOriginalLayoutDismissed && currentToolIndex == -1 && !isImeCurrentlyVisible) {
                b.layoutSetOriginal.visibility = View.VISIBLE
            }

            try {
                originalBitmap?.let { if (!it.isRecycled) it.recycle() }
                originalBitmap = loadOptimizedBitmap(uri)
                val bmp = originalBitmap
                if (bmp != null) {
                    val w = bmp.width
                    val h = bmp.height
                    val fileSize = getFileSizeFromUri(uri)
                    val sizeStr = formatFileSize(fileSize)
                    b.tvMediaInfo.text = "${w}×${h} • ~$sizeStr"
                    b.tvMediaInfo.visibility = View.VISIBLE

                    b.ivEditorPreview.setImageBitmap(bmp)
                    b.ivEditorPreview.post {
                        if (b.ivEditorPreview.width > 0 && b.ivEditorPreview.height > 0) {
                            centerImage(b)
                        } else {
                            b.ivEditorPreview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    b.ivEditorPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                    centerImage(b)
                                }
                            })
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showPhotoPicker(b: ActivityPhotoEditorContentBinding) {
        val sheet = PhotoPickerBottomSheet(
            activity = this,
            title = if (isProfilePhoto) "Фото профиля" else "Выбор фото",
            allowGif = !isStickerMode,
            showPresets = false,
            onMediaSelected = { uri, _ ->
                activePhotoPickerSheet = null
                loadMediaUriIntoEditor(b, uri)
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
                if (originalUri == null && !isFinishing) {
                    finish()
                }
            }
        )
        activePhotoPickerSheet = sheet
        sheet.show()
    }

    private fun setupTitleSwitcher(b: ActivityPhotoEditorContentBinding) {
        b.tsEditorTitle.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 16.5f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
        b.tsEditorTitle.setText("Редактор")
    }

    private fun setupBrushQuickSwatches(b: ActivityPhotoEditorContentBinding) {
        val colors = listOf(
            "#EF4444".toColorInt(),
            "#F97316".toColorInt(),
            "#FACC15".toColorInt(),
            "#22C55E".toColorInt(),
            "#3B82F6".toColorInt(),
            "#A855F7".toColorInt(),
            "#FFFFFF".toColorInt()
        )
        val density = resources.displayMetrics.density
        val size = (30 * density).toInt()
        val gap = (8 * density).toInt()
        b.layoutBrushSwatches.removeAllViews()

        colors.forEachIndexed { index, col ->
            val v = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    marginEnd = if (index < colors.lastIndex) gap else 0
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(col)
                    setStroke((1.5f * density).toInt(), "#66FFFFFF".toColorInt())
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    currentBrushColor = col
                    b.drawingView.setBrushColor(col)
                    b.brushPreview.backgroundTintList = ColorStateList.valueOf(col)
                    updateBrushSwatchSelection(b)
                }
            }
            b.layoutBrushSwatches.addView(v)
        }
        updateBrushSwatchSelection(b)
    }

    private fun updateBrushSwatchSelection(b: ActivityPhotoEditorContentBinding) {
        for (i in 0 until b.layoutBrushSwatches.childCount) {
            val child = b.layoutBrushSwatches.getChildAt(i)
            val drawable = child.background as? GradientDrawable
            val isSelected = drawable?.color?.defaultColor == currentBrushColor
            child.animate()
                .scaleX(if (isSelected) 1.18f else 1f)
                .scaleY(if (isSelected) 1.18f else 1f)
                .setDuration(140)
                .start()
        }
    }

    private fun setCropFrameVisible(b: ActivityPhotoEditorContentBinding, visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        b.cropFrame.visibility = v
        b.maskTop.visibility = v
        b.maskBottom.visibility = v
        b.maskLeft.visibility = v
        b.maskRight.visibility = v
    }

    private fun positionAspectDrop(b: ActivityPhotoEditorContentBinding, index: Int, animate: Boolean) {
        val container = b.aspectSelectorContainer
        val drop = b.aspectSelectionDrop
        val cellWidth = container.width / 4f
        if (cellWidth <= 0f) return
        val dropWidth = (cellWidth - 8).toInt().coerceAtLeast(1)
        if (drop.layoutParams.width != dropWidth) {
            drop.layoutParams = drop.layoutParams.apply { width = dropWidth }
        }
        val targetX = index * cellWidth + (cellWidth - dropWidth) / 2f
        if (animate) {
            drop.animate()
                .translationX(targetX)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            drop.translationX = targetX
        }
        highlightLabel(b, index)
    }

    private fun setupAspectSelector(b: ActivityPhotoEditorContentBinding) {
        val container = b.aspectSelectorContainer
        val drop = b.aspectSelectionDrop
        val labels = listOf(b.tvRatio11, b.tvRatio169, b.tvRatio43, b.tvRatioFull)

        container.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if ((right - left) > 0 && (right - left) != (oldRight - oldLeft)) {
                container.post { positionAspectDrop(b, selectedAspectIndex, false) }
            }
        }
        container.post { positionAspectDrop(b, selectedAspectIndex, false) }

        container.setOnTouchListener { v, event ->
            val x = event.x
            val cellWidth = (container.width / 4f).coerceAtLeast(1f)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val halfDrop = drop.width / 2f
                    val targetX = (x - halfDrop).coerceIn(4f, max(4f, container.width.toFloat() - drop.width - 4f))
                    drop.translationX = targetX
                    val index = (x / cellWidth).toInt().coerceIn(0, 3)
                    highlightLabel(b, index)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.performClick()
                    val index = (x / cellWidth).toInt().coerceIn(0, 3)
                    selectRatio(b, index)
                }
            }
            true
        }

        labels.forEachIndexed { index, textView ->
            textView.setOnClickListener { selectRatio(b, index) }
        }
    }

    private fun selectRatio(b: ActivityPhotoEditorContentBinding, index: Int) {
        selectedAspectIndex = index.coerceIn(0, 3)
        positionAspectDrop(b, selectedAspectIndex, true)

        val ratios = listOf("1:1", "16:9", "4:3", "Full")
        applyAspectRatio(b, ratios[selectedAspectIndex])
        b.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun highlightLabel(b: ActivityPhotoEditorContentBinding, index: Int) {
        val labels = listOf(b.tvRatio11, b.tvRatio169, b.tvRatio43, b.tvRatioFull)
        labels.forEachIndexed { i, textView ->
            textView.alpha = if (i == index) 1.0f else 0.55f
            textView.animate()
                .scaleX(if (i == index) 1.08f else 1.0f)
                .scaleY(if (i == index) 1.08f else 1.0f)
                .setDuration(150)
                .start()
        }
    }

    private fun applyAspectRatio(b: ActivityPhotoEditorContentBinding, ratio: String) {
        val bmp = originalBitmap
        val isLandscape = bmp != null && bmp.width > bmp.height
        when (ratio) {
            "1:1" -> {
                isCropFrameActive = true
                setCropFrameVisible(b, true)
                setCropFrameSize(b, 300, 300)
                b.cropFrame.post { centerImage(b) }
            }
            "4:3" -> {
                isCropFrameActive = true
                setCropFrameVisible(b, true)
                if (isLandscape) setCropFrameSize(b, 340, 255) else setCropFrameSize(b, 285, 380)
                b.cropFrame.post { centerImage(b) }
            }
            "16:9" -> {
                isCropFrameActive = true
                setCropFrameVisible(b, true)
                if (isLandscape) setCropFrameSize(b, 360, 202) else setCropFrameSize(b, 270, 480)
                b.cropFrame.post { centerImage(b) }
            }
            "Full" -> {
                isCropFrameActive = false
                setCropFrameVisible(b, false)
                centerImage(b)
            }
        }
    }

    private fun centerImage(b: ActivityPhotoEditorContentBinding) {
        val bitmap = originalBitmap ?: return
        var viewWidth = b.ivEditorPreview.width.toFloat()
        var viewHeight = b.ivEditorPreview.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) {
            viewWidth = resources.displayMetrics.widthPixels.toFloat()
            viewHeight = resources.displayMetrics.heightPixels.toFloat()
        }
        val density = resources.displayMetrics.density
        val scale = if (isCropFrameActive) {
            val cropWidth = b.cropFrame.width.toFloat().takeIf { it > 0 } ?: (300 * density)
            val cropHeight = b.cropFrame.height.toFloat().takeIf { it > 0 } ?: (300 * density)
            max(cropWidth / bitmap.width, cropHeight / bitmap.height)
        } else {
            val availW = (viewWidth - 24f * density).coerceAtLeast(120f)
            val availH = (viewHeight - 230f * density).coerceAtLeast(180f)
            minOf(availW / bitmap.width, availH / bitmap.height)
        }
        mainMatrix.setScale(scale, scale)
        mainMatrix.postTranslate((viewWidth - bitmap.width * scale) / 2f, (viewHeight - bitmap.height * scale) / 2f)
        baselineMatrix.set(mainMatrix)

        isSyncingScaleSlider = true
        try {
            b.sliderScale.value = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        } finally {
            isSyncingScaleSlider = false
        }
        updatePreview(b)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners(b: ActivityPhotoEditorContentBinding) {
        b.btnBack.setOnClickListener {
            if (currentToolIndex != -1 || mode == PIPETTE) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                finish()
            }
        }
        b.btnPickAnotherPhoto.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            showPhotoPicker(b)
        }
        b.btnReset.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            resetAll(b)
        }
        b.btnUndoTop.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            b.drawingView.undo()
        }

        b.btnSetOriginal.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            hideOriginalLayoutWithTimer(b) { setOriginalAndFinish() }
        }
        b.btnCloseOriginal.setOnClickListener {
            isOriginalLayoutDismissed = true
            hideOriginalLayoutWithTimer(b) { }
        }

        b.toolAutoEnhance.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            brightness = 12f
            contrast = 1.18f
            saturation = 1.15f
            temperature = 3f
            tint = 0f
            b.sliderBrightness.value = brightness
            b.sliderContrast.value = contrast
            b.sliderSaturation.value = saturation
            b.sliderTemperature.value = temperature
            b.sliderTint.value = tint
            updateNumericLabels(b)
            updatePreview(b)
            PrimeNotification.show(this, "Авто-улучшение применено")
        }

        b.toolLight.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            toggleToolGroup(b, b.layoutGroupLight, "Свет и цвет")
        }
        b.toolGeometry.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            toggleToolGroup(b, b.layoutGroupGeometry, "Геометрия")
        }
        b.toolBrushGroup.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            toggleToolGroup(b, b.layoutGroupBrush, "Рисование")
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isBrightAccent = ColorUtils.calculateLuminance(accentColor) > 0.45
        val updateHdQualityUi = {
            if (isHdQuality) {
                b.btnHdQualityToggle.text = "HD"
                b.btnHdQualityToggle.backgroundTintList = ColorStateList.valueOf(accentColor)
                b.btnHdQualityToggle.setTextColor(if (isBrightAccent) "#0F172A".toColorInt() else Color.WHITE)
            } else {
                b.btnHdQualityToggle.text = "SD"
                b.btnHdQualityToggle.backgroundTintList = ColorStateList.valueOf("#24FFFFFF".toColorInt())
                b.btnHdQualityToggle.setTextColor("#CBD5E1".toColorInt())
            }
        }
        updateHdQualityUi()

        b.btnHdQualityToggle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            isHdQuality = !isHdQuality
            updateHdQualityUi()
            PrimeNotification.show(this, if (isHdQuality) "Высокое качество (HD)" else "Стандартное качество (SD)")
        }

        val initialTool = intent.getIntExtra("EXTRA_INITIAL_TOOL_GROUP", -1)
        if (initialTool >= 0) {
            b.root.post {
                when (initialTool) {
                    0 -> toggleToolGroup(b, b.layoutGroupLight, "Свет и цвет")
                    1 -> toggleToolGroup(b, b.layoutGroupGeometry, "Геометрия")
                    2 -> toggleToolGroup(b, b.layoutGroupBrush, "Рисование")
                }
            }
        }

        b.sliderBrightness.addOnChangeListener { _, value, _ ->
            brightness = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderContrast.addOnChangeListener { _, value, _ ->
            contrast = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderSaturation.addOnChangeListener { _, value, _ ->
            saturation = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderTemperature.addOnChangeListener { _, value, _ ->
            temperature = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderTint.addOnChangeListener { _, value, _ ->
            tint = value
            updateNumericLabels(b)
            updatePreview(b)
        }

        b.sliderScale.addOnChangeListener { _, value, _ ->
            if (isSyncingScaleSlider) return@addOnChangeListener
            val values = FloatArray(9)
            mainMatrix.getValues(values)
            val currentScale = values[Matrix.MSCALE_X]
            if (currentScale.isFinite() && currentScale > 0.001f) {
                val scaleFactor = value / currentScale
                if (scaleFactor.isFinite() && scaleFactor > 0f) {
                    mainMatrix.postScale(scaleFactor, scaleFactor, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
                }
            }
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderRotate.addOnChangeListener { _, value, _ ->
            rotationAngle = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.sliderPerspective.addOnChangeListener { _, value, _ ->
            perspectiveFactor = value
            updateNumericLabels(b)
            updatePreview(b)
        }
        b.btnFlipToggle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            isMirrored = !isMirrored
            it.backgroundTintList = ColorStateList.valueOf(if (isMirrored) accentColor else "#22FFFFFF".toColorInt())
            updatePreview(b)
        }

        b.sliderBrushSize.addOnChangeListener { _, value, _ ->
            brushSize = value
            b.drawingView.setBrushSize(brushSize)
            updateNumericLabels(b)
            updateBrushPreview(b, brushSize)
        }

        b.sliderBrushSize.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                b.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(b, brushSize)
            }
            override fun onStopTrackingTouch(slider: Slider) {
                b.brushPreview.visibility = View.GONE
            }
        })

        b.sliderBrushHardness.addOnChangeListener { _, value, _ ->
            brushHardness = value
            b.drawingView.setHardness(brushHardness)
            updateNumericLabels(b)
            updateBrushPreview(b, brushSize)
        }

        b.sliderBrushHardness.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                b.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(b, brushSize)
            }
            override fun onStopTrackingTouch(slider: Slider) {
                b.brushPreview.visibility = View.GONE
            }
        })

        b.tvColorPickerLink.setOnClickListener { showColorPicker(b) }
        b.toggleBrushMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isEraser = checkedId == R.id.btnModeEraser
                b.drawingView.setEraser(isEraser)
                b.btnModeBrush.backgroundTintList = ColorStateList.valueOf(if (!isEraser) accentColor else "#16FFFFFF".toColorInt())
                b.btnModeEraser.backgroundTintList = ColorStateList.valueOf(if (isEraser) accentColor else "#16FFFFFF".toColorInt())
            }
        }

        b.drawingView.setOnUndoAvailableListener { available ->
            b.btnUndoTop.visibility = if (available) View.VISIBLE else View.GONE
            updateTopActionVisibility(b)
        }

        b.drawingView.setOnDrawingStateListener { isDrawing ->
            val targetPanelAlpha = if (isDrawing) 0f else 1f
            val targetMaskAlpha = if (isDrawing) 0.35f else 1f
            b.groupSlidersContainer.animate().alpha(targetPanelAlpha).setDuration(180).start()
            listOf(b.maskTop, b.maskBottom, b.maskLeft, b.maskRight).forEach {
                it.animate().alpha(targetMaskAlpha).setDuration(180).start()
            }
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (b.groupSlidersContainer.visibility == View.VISIBLE) {
                    val activeId = when {
                        b.layoutGroupLight.visibility == View.VISIBLE -> R.id.toolLight
                        b.layoutGroupGeometry.visibility == View.VISIBLE -> R.id.toolGeometry
                        b.layoutGroupBrush.visibility == View.VISIBLE -> R.id.toolBrushGroup
                        else -> -1
                    }
                    if (activeId != -1) {
                        b.root.findViewById<View>(activeId)?.performClick()
                    }
                }
                return true
            }
        })

        b.ivEditorPreview.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (mode == PIPETTE) {
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_MOVE) {
                    sampleColor(b, event.x, event.y)
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        mode = NONE
                        updateBackCallbackState()
                        if (b.layoutGroupBrush.visibility == View.VISIBLE) {
                            b.drawingView.visibility = View.VISIBLE
                        }
                    }
                }
                return@setOnTouchListener true
            }
            if (b.drawingView.visibility == View.VISIBLE || isGifMode) return@setOnTouchListener false
            handleTouch(b, event)
            updateTopActionVisibility(b)
            true
        }

        b.btnDone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            saveAndFinish(b)
        }
    }

    private fun updateNumericLabels(b: ActivityPhotoEditorContentBinding) {
        b.tvValBrightness.text = String.format(Locale.US, "%+.0f", brightness)
        b.tvValContrast.text = String.format(Locale.US, "%.2f×", contrast)
        b.tvValSaturation.text = "${(saturation * 100).toInt()}%"
        b.tvValTemperature.text = String.format(Locale.US, "%+.0f", temperature)
        b.tvValTint.text = String.format(Locale.US, "%+.0f", tint)

        b.tvValScale.text = String.format(Locale.US, "%.2f×", b.sliderScale.value)
        b.tvValRotate.text = "${rotationAngle.toInt()}°"
        b.tvValPerspective.text = String.format(Locale.US, "%.2f", perspectiveFactor)

        b.tvValBrushSize.text = "${brushSize.toInt()} px"
        b.tvValBrushHardness.text = "${(brushHardness * 100).toInt()}%"
    }

    private fun updateToolHighlight(b: ActivityPhotoEditorContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isBrightAccent = ColorUtils.calculateLuminance(accentColor) > 0.45
        val activeIconColor = if (isBrightAccent) "#0F172A".toColorInt() else Color.WHITE

        val tools = listOf(
            0 to b.toolLight,
            1 to b.toolGeometry,
            2 to b.toolBrushGroup
        )
        tools.forEach { (idx, btn) ->
            val isActive = currentToolIndex == idx
            btn.backgroundTintList = ColorStateList.valueOf(if (isActive) accentColor else Color.TRANSPARENT)
            btn.imageTintList = ColorStateList.valueOf(if (isActive) activeIconColor else Color.WHITE)
        }
    }

    private fun applyAccentColors(b: ActivityPhotoEditorContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentCsl = ColorStateList.valueOf(accentColor)
        val isBrightAccent = ColorUtils.calculateLuminance(accentColor) > 0.45

        b.btnDone.backgroundTintList = accentCsl
        b.btnDone.imageTintList = ColorStateList.valueOf(if (isBrightAccent) "#0F172A".toColorInt() else Color.WHITE)
        b.pbCloseTimer.progressTintList = accentCsl

        val sliders = listOf(
            b.sliderBrightness, b.sliderContrast, b.sliderSaturation,
            b.sliderTemperature, b.sliderTint, b.sliderScale,
            b.sliderRotate, b.sliderPerspective, b.sliderBrushSize,
            b.sliderBrushHardness
        )
        for (slider in sliders) {
            slider.thumbTintList = accentCsl
            slider.trackActiveTintList = accentCsl
        }

        b.btnModeBrush.backgroundTintList = accentCsl
    }

    private fun sampleColor(b: ActivityPhotoEditorContentBinding, x: Float, y: Float) {
        val bitmap = originalBitmap ?: return
        val inverse = Matrix()
        b.ivEditorPreview.imageMatrix.invert(inverse)
        val pts = floatArrayOf(x, y)
        inverse.mapPoints(pts)
        val px = pts[0].toInt().coerceIn(0, bitmap.width - 1)
        val py = pts[1].toInt().coerceIn(0, bitmap.height - 1)
        currentBrushColor = bitmap.getPixel(px, py)
        b.drawingView.setBrushColor(currentBrushColor)
        b.brushPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
        updateBrushSwatchSelection(b)
    }

    private fun setCropFrameSize(b: ActivityPhotoEditorContentBinding, w: Int, h: Int) {
        val density = resources.displayMetrics.density
        val params = b.cropFrame.layoutParams
        val view = b.photoEditorRoot
        val maxW = (view.width - 40).coerceAtLeast((200 * density).toInt())
        val maxH = (view.height - (260 * density).toInt()).coerceAtLeast((200 * density).toInt())
        params.width = (w * density).toInt().coerceAtMost(maxW)
        params.height = (h * density).toInt().coerceAtMost(maxH)
        b.cropFrame.layoutParams = params
    }

    private fun toggleToolGroup(b: ActivityPhotoEditorContentBinding, group: View, title: String) {
        val isCurrentlyVisible = group.visibility == View.VISIBLE
        val newIndex = when (group.id) {
            R.id.layoutGroupLight -> 0
            R.id.layoutGroupGeometry -> 1
            R.id.layoutGroupBrush -> 2
            else -> -1
        }

        if (isCurrentlyVisible) {
            animateHideToolGroup(b) {
                group.visibility = View.GONE
                b.groupSlidersContainer.visibility = View.GONE
                b.tsEditorTitle.setText(defaultTitle())
                showOriginalLayout(b)
                b.drawingView.visibility = View.GONE
                currentToolIndex = -1
                updateToolHighlight(b)
                updateBackCallbackState()
            }
        } else {
            val activeGroup = when {
                b.layoutGroupLight.visibility == View.VISIBLE -> b.layoutGroupLight
                b.layoutGroupGeometry.visibility == View.VISIBLE -> b.layoutGroupGeometry
                b.layoutGroupBrush.visibility == View.VISIBLE -> b.layoutGroupBrush
                else -> null
            }

            b.scrollToolControls.scrollTo(0, 0)
            if (b.scrollToolControls.layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                b.scrollToolControls.layoutParams = b.scrollToolControls.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            if (activeGroup != null) {
                val isForward = newIndex > currentToolIndex
                b.groupSlidersContainer.alpha = 1f
                b.groupSlidersContainer.visibility = View.VISIBLE
                animateHorizontalTransition(b, activeGroup, group, isForward)
                b.tsEditorTitle.setText(title)
                b.drawingView.visibility = if (group == b.layoutGroupBrush) View.VISIBLE else View.GONE
                currentToolIndex = newIndex
            } else {
                b.layoutGroupLight.visibility = View.GONE
                b.layoutGroupGeometry.visibility = View.GONE
                b.layoutGroupBrush.visibility = View.GONE

                group.visibility = View.VISIBLE
                b.groupSlidersContainer.visibility = View.VISIBLE
                b.groupSlidersContainer.alpha = 1f
                b.tsEditorTitle.setText(title)
                b.layoutSetOriginal.visibility = View.GONE
                b.drawingView.visibility = if (group == b.layoutGroupBrush) View.VISIBLE else View.GONE
                currentToolIndex = newIndex
                animateShowToolGroup(b)
            }
            updateToolHighlight(b)
        }
        updateBackCallbackState()
    }

    private fun animateHorizontalTransition(b: ActivityPhotoEditorContentBinding, oldView: View, newView: View, isForward: Boolean) {
        val screenWidth = b.photoEditorRoot.width.toFloat().takeIf { it > 0f } ?: resources.displayMetrics.widthPixels.toFloat()
        val outTranslation = if (isForward) -screenWidth else screenWidth
        val inTranslation = if (isForward) screenWidth else -screenWidth

        newView.translationX = inTranslation
        newView.visibility = View.VISIBLE
        newView.alpha = 0f

        oldView.animate()
            .translationX(outTranslation)
            .alpha(0f)
            .setDuration(240)
            .withEndAction {
                oldView.visibility = View.GONE
                oldView.translationX = 0f
                oldView.alpha = 1f
            }
            .start()

        newView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(240)
            .start()
    }

    private fun animateShowToolGroup(b: ActivityPhotoEditorContentBinding) {
        b.groupSlidersContainer.clearAnimation()
        b.groupSlidersContainer.alpha = 0f
        b.groupSlidersContainer.translationY = 60f
        b.groupSlidersContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateHideToolGroup(b: ActivityPhotoEditorContentBinding, onEnd: () -> Unit) {
        b.groupSlidersContainer.clearAnimation()
        b.groupSlidersContainer.animate()
            .alpha(0f)
            .translationY(60f)
            .setDuration(200)
            .withEndAction {
                onEnd()
                b.groupSlidersContainer.translationY = 0f
                b.groupSlidersContainer.alpha = 1f
            }
            .start()
    }

    private fun updateTopActionVisibility(b: ActivityPhotoEditorContentBinding) {
        val hasTransform = brightness != 0f || contrast != 1f || rotationAngle != 0f ||
            perspectiveFactor != 0f || isMirrored || saturation != 1f || temperature != 0f || tint != 0f
        val hasDrawing = b.drawingView.hasHistory()
        val matrixChanged = mainMatrix != baselineMatrix

        b.btnReset.visibility = if (hasTransform || hasDrawing || matrixChanged) View.VISIBLE else View.GONE
        val anyVisible = b.btnReset.visibility == View.VISIBLE || b.btnUndoTop.visibility == View.VISIBLE
        if (anyVisible) {
            if (b.topActionsLayout.visibility != View.VISIBLE) {
                b.topActionsLayout.visibility = View.VISIBLE
            }
            b.topActionsLayout.animate().alpha(1f).setDuration(200).start()
        } else {
            b.topActionsLayout.animate().alpha(0f).setDuration(180).withEndAction {
                if (b.btnReset.visibility != View.VISIBLE && b.btnUndoTop.visibility != View.VISIBLE) {
                    b.topActionsLayout.visibility = View.GONE
                }
            }.start()
        }
    }

    private fun resetAll(b: ActivityPhotoEditorContentBinding) {
        brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
        rotationAngle = 0f; perspectiveFactor = 0f; isMirrored = false

        isSyncingScaleSlider = true
        try {
            b.sliderBrightness.value = 0f
            b.sliderContrast.value = 1f
            b.sliderSaturation.value = 1f
            b.sliderTemperature.value = 0f
            b.sliderTint.value = 0f
            b.sliderRotate.value = 0f
            b.sliderPerspective.value = 0f
        } finally {
            isSyncingScaleSlider = false
        }
        b.btnFlipToggle.backgroundTintList = ColorStateList.valueOf("#22FFFFFF".toColorInt())

        isCropFrameActive = isProfilePhoto || isStickerMode
        selectedAspectIndex = if (isCropFrameActive) 0 else 3
        setCropFrameVisible(b, isCropFrameActive)
        positionAspectDrop(b, selectedAspectIndex, false)
        centerImage(b)
        b.drawingView.clear()
        b.drawingView.visibility = View.GONE
        b.layoutGroupLight.visibility = View.GONE
        b.layoutGroupGeometry.visibility = View.GONE
        b.layoutGroupBrush.visibility = View.GONE
        b.groupSlidersContainer.visibility = View.GONE
        currentToolIndex = -1
        updateToolHighlight(b)
        updateNumericLabels(b)
        updateBackCallbackState()
        showOriginalLayout(b)
        b.tsEditorTitle.setText(defaultTitle())
        updateTopActionVisibility(b)
    }

    private fun saveGifAndFinish(uri: Uri) {
        try {
            val tempFile = File(cacheDir, "avatar_temp_${System.currentTimeMillis()}.gif")
            val inputStream = try {
                contentResolver.openInputStream(uri)
            } catch (_: Throwable) { null }
                ?: if (uri.scheme == "file" && uri.path != null) FileInputStream(File(uri.path!!))
                else FileInputStream(File(uri.toString().removePrefix("file://")))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val gifUriStr = Uri.fromFile(tempFile).toString()
            val captionText = binding?.etCaption?.text?.toString()?.trim() ?: ""
            val resultIntent = Intent().apply {
                putExtra("EDITED_IMAGE_URI", gifUriStr)
                putExtra("EXTRA_IMAGE_URI", gifUriStr)
                putExtra("EXTRA_CAPTION", captionText)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (t: Throwable) {
            t.printStackTrace()
            finish()
        }
    }

    private fun setOriginalAndFinish() {
        if (isSaving) return
        if (isGifMode) {
            originalUri?.let {
                saveGifAndFinish(it)
                return
            }
        }
        val bitmap = originalBitmap
        val srcUri = originalUri
        val captionText = binding?.etCaption?.text?.toString()?.trim() ?: ""
        if (!isProfilePhoto && !isStickerMode && srcUri != null) {
            isSaving = true
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    val file = File(cacheDir, "avatar_edited_${System.currentTimeMillis()}.jpg")
                    var copied = false
                    try {
                        val stream = try {
                            contentResolver.openInputStream(srcUri)
                        } catch (_: Throwable) {
                            val rawPath = srcUri.path ?: srcUri.toString().removePrefix("file://")
                            val f = File(rawPath)
                            if (f.exists()) FileInputStream(f) else null
                        }
                        if (stream != null) {
                            stream.use { input ->
                                FileOutputStream(file).use { out ->
                                    input.copyTo(out)
                                    out.flush()
                                }
                            }
                            if (file.length() > 0L) copied = true
                        }
                    } catch (_: Throwable) {}

                    if (!copied && bitmap != null && !bitmap.isRecycled) {
                        try {
                            FileOutputStream(file).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                                out.flush()
                            }
                            copied = true
                        } catch (_: Throwable) {}
                    }

                    val uriStr = if (copied) Uri.fromFile(file).toString() else srcUri.toString()
                    val resultIntent = Intent().apply {
                        putExtra("EDITED_IMAGE_URI", uriStr)
                        putExtra("EXTRA_IMAGE_URI", uriStr)
                        putExtra("EXTRA_CAPTION", captionText)
                        data = Uri.parse(uriStr)
                    }
                    runOnUiThread {
                        if (isDestroyed || isFinishing) return@runOnUiThread
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                } finally {
                    executor.shutdown()
                }
            }
        } else if (bitmap != null) {
            isSaving = true
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    val file = File(cacheDir, "avatar_edited_${System.currentTimeMillis()}.jpg")
                    try {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                            out.flush()
                        }
                        val uriStr = Uri.fromFile(file).toString()
                        val resultIntent = Intent().apply {
                            putExtra("EDITED_IMAGE_URI", uriStr)
                            putExtra("EXTRA_IMAGE_URI", uriStr)
                            putExtra("EXTRA_CAPTION", captionText)
                            data = Uri.parse(uriStr)
                        }
                        runOnUiThread {
                            if (isDestroyed || isFinishing) return@runOnUiThread
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        isSaving = false
                        runOnUiThread {
                            if (!isDestroyed && !isFinishing) finishFallback()
                        }
                    }
                } finally {
                    executor.shutdown()
                }
            }
        } else {
            finishFallback()
        }
    }

    private fun finishFallback() {
        val fallbackUriStr = originalUri?.toString() ?: intent.getStringExtra("EXTRA_IMAGE_URI") ?: intent.dataString
        val captionText = binding?.etCaption?.text?.toString()?.trim() ?: ""
        if (!fallbackUriStr.isNullOrEmpty()) {
            val resultIntent = Intent().apply {
                putExtra("EDITED_IMAGE_URI", fallbackUriStr)
                putExtra("EXTRA_IMAGE_URI", fallbackUriStr)
                putExtra("EXTRA_CAPTION", captionText)
                data = Uri.parse(fallbackUriStr)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun showColorPicker(b: ActivityPhotoEditorContentBinding) {
        var isInternalChange = false
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)

        val blurCard = dialogBinding.root.findViewById<BlurView>(R.id.blurDialogCard)
        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            val overlayColor = getDefaultBlurOverlayColor(this)
            val windowBg = window.decorView.background
            blurCard.setupBlur(rootView, 16f, overlayColor, windowBg)
        }

        val dialog = AlertDialog.Builder(this, R.style.Theme_Prime_AlertDialog).setView(dialogBinding.root).create()
        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = win.attributes
                    params.blurBehindRadius = 60
                    win.attributes = params
                } catch (_: Throwable) {}
            }
        }

        fun updateDialogColors(db: DialogColorPickerBinding, color: Int) {
            if (isInternalChange) return
            isInternalChange = true
            try {
                db.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                val hexStr = String.format("#%06X", (0xFFFFFF and color))
                if (db.etHex.text?.toString() != hexStr) db.etHex.setText(hexStr)
                val rStr = Color.red(color).toString()
                if (db.etR.text?.toString() != rStr) db.etR.setText(rStr)
                val gStr = Color.green(color).toString()
                if (db.etG.text?.toString() != gStr) db.etG.setText(gStr)
                val bStr = Color.blue(color).toString()
                if (db.etB.text?.toString() != bStr) db.etB.setText(bStr)
            } catch (_: Exception) {
            } finally {
                isInternalChange = false
            }
        }

        dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
        dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and currentBrushColor)))
        dialogBinding.etR.setText(Color.red(currentBrushColor).toString())
        dialogBinding.etG.setText(Color.green(currentBrushColor).toString())
        dialogBinding.etB.setText(Color.blue(currentBrushColor).toString())

        var dialogSelectedColor = currentBrushColor
        dialogBinding.spectrumView.setOnColorChangedListener { color ->
            dialogSelectedColor = color
            updateDialogColors(dialogBinding, color)
        }

        dialogBinding.btnPipette.setOnClickListener {
            mode = PIPETTE
            updateBackCallbackState()
            b.drawingView.visibility = View.GONE
            dialog.dismiss()
            PrimeNotification.show(this, "Выберите цвет на фото")
        }

        val rgbWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val r = dialogBinding.etR.text.toString().toInt().coerceIn(0, 255)
                    val g = dialogBinding.etG.text.toString().toInt().coerceIn(0, 255)
                    val bl = dialogBinding.etB.text.toString().toInt().coerceIn(0, 255)
                    val color = Color.rgb(r, g, bl)
                    isInternalChange = true
                    dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        val hexWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val color = Color.parseColor(s.toString())
                    isInternalChange = true
                    dialogBinding.etR.setText(Color.red(color).toString())
                    dialogBinding.etG.setText(Color.green(color).toString())
                    dialogBinding.etB.setText(Color.blue(color).toString())
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        dialogBinding.etHex.addTextChangedListener(hexWatcher)
        dialogBinding.etR.addTextChangedListener(rgbWatcher)
        dialogBinding.etG.addTextChangedListener(rgbWatcher)
        dialogBinding.etB.addTextChangedListener(rgbWatcher)

        dialogBinding.btnApplyColor.setOnClickListener {
            try {
                var hex = dialogBinding.etHex.text.toString().trim()
                if (!hex.startsWith("#")) hex = "#$hex"
                currentBrushColor = Color.parseColor(hex)
            } catch (_: Exception) {
                currentBrushColor = dialogSelectedColor
            }
            b.drawingView.setBrushColor(currentBrushColor)
            b.brushPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
            updateBrushSwatchSelection(b)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun handleTouch(b: ActivityPhotoEditorContentBinding, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(mainMatrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(mainMatrix)
                        midPoint(midPoint, event)
                        mode = ZOOM
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    mainMatrix.set(savedMatrix)
                    mainMatrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                } else if (mode == ZOOM && event.pointerCount >= 2) {
                    val newDist = spacing(event)
                    if (newDist > 10f && oldDist > 0f) {
                        val scaleFactor = newDist / oldDist
                        val values = FloatArray(9)
                        savedMatrix.getValues(values)
                        val currentScale = values[Matrix.MSCALE_X]
                        if (currentScale.isFinite() && currentScale > 0.001f && scaleFactor.isFinite() && scaleFactor > 0f) {
                            var finalScaleFactor = scaleFactor
                            if (currentScale * scaleFactor < MIN_ZOOM) {
                                finalScaleFactor = MIN_ZOOM / currentScale
                            } else if (currentScale * scaleFactor > MAX_ZOOM) {
                                finalScaleFactor = MAX_ZOOM / currentScale
                            }

                            mainMatrix.set(savedMatrix)
                            mainMatrix.postScale(finalScaleFactor, finalScaleFactor, midPoint.x, midPoint.y)

                            val finalValues = FloatArray(9)
                            mainMatrix.getValues(finalValues)
                            val updatedScale = finalValues[Matrix.MSCALE_X]
                            if (updatedScale.isFinite()) {
                                isSyncingScaleSlider = true
                                try {
                                    b.sliderScale.value = updatedScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
                                } finally {
                                    isSyncingScaleSlider = false
                                }
                                b.tvValScale.text = String.format(Locale.US, "%.2f×", b.sliderScale.value)
                            }
                        }
                    }
                }
                updatePreview(b)
            }
        }
    }

    private fun updatePreview(b: ActivityPhotoEditorContentBinding) {
        val displayMatrix = Matrix(mainMatrix)
        displayMatrix.postRotate(rotationAngle, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        if (isMirrored) displayMatrix.postScale(-1f, 1f, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        displayMatrix.postSkew(perspectiveFactor, 0f, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        b.ivEditorPreview.imageMatrix = displayMatrix
        b.ivEditorPreview.colorFilter = ColorMatrixColorFilter(getCurrentColorMatrix())
        updateTopActionVisibility(b)
    }

    private fun getCurrentColorMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        val tempMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, temperature, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, -temperature, 0f, 0f, 0f, 1f, 0f))
        cm.postConcat(tempMatrix)
        val tintMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, -tint, 0f, 1f, 0f, 0f, tint, 0f, 0f, 1f, 0f, -tint, 0f, 0f, 0f, 1f, 0f))
        cm.postConcat(tintMatrix)
        val baseFilters = ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, brightness, 0f, contrast, 0f, 0f, brightness, 0f, 0f, contrast, 0f, brightness, 0f, 0f, 0f, 1f, 0f))
        cm.postConcat(baseFilters)
        return cm
    }

    @SuppressLint("InternalInsetResource", "Discouraged")
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (32 * resources.displayMetrics.density).toInt()
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            val p = uri.path
            if (p != null) return File(p).length()
        }
        try {
            contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (col != -1) return cursor.getLong(col)
                }
            }
        } catch (_: Exception) {}
        return 0L
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 КБ"
        val kb = size / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f КБ", kb)
        val mb = kb / 1024.0
        return String.format(Locale.getDefault(), "%.1f МБ", mb)
    }

    private fun saveAndFinish(b: ActivityPhotoEditorContentBinding) {
        if (isSaving) return
        val gifUri = originalUri
        if (isGifMode && gifUri != null) {
            isSaving = true
            saveGifAndFinish(gifUri)
            return
        }
        val bitmap = originalBitmap
        if (bitmap == null) {
            setOriginalAndFinish()
            return
        }
        val viewWidth = b.ivEditorPreview.width
        val viewHeight = b.ivEditorPreview.height
        if (viewWidth <= 0 || viewHeight <= 0) {
            setOriginalAndFinish()
            return
        }

        isSaving = true
        val captionText = b.etCaption.text?.toString()?.trim() ?: ""
        val drawingBitmap = b.drawingView.getResultBitmap()
        val rect = Rect()
        val cropActive = isCropFrameActive && b.cropFrame.visibility == View.VISIBLE
        if (cropActive) {
            b.cropFrame.getGlobalVisibleRect(rect)
        }
        val viewRect = Rect()
        b.ivEditorPreview.getGlobalVisibleRect(viewRect)
        val currentMatrix = getCurrentColorMatrix()
        val hasGeometryChange = rotationAngle != 0f || perspectiveFactor != 0f || isMirrored ||
            (mainMatrix != baselineMatrix) || drawingBitmap != null
        val matrixCopy = Matrix(mainMatrix)
        val rotAngle = rotationAngle
        val mirrored = isMirrored
        val perspFactor = perspectiveFactor
        val hdQuality = isHdQuality

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                var outputBitmap: Bitmap
                var resultBitmapToRecycle: Bitmap? = null

                if (!cropActive && !hasGeometryChange) {
                    val maxDim = if (hdQuality) maxOf(bitmap.width, bitmap.height) else 1280
                    val scale = (maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
                    val outW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                    val outH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                    outputBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(outputBitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    paint.colorFilter = ColorMatrixColorFilter(currentMatrix)
                    val fullMatrix = Matrix().apply { setScale(scale, scale) }
                    canvas.drawBitmap(bitmap, fullMatrix, paint)
                } else {
                    val resultBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                    resultBitmapToRecycle = resultBitmap
                    val canvas = Canvas(resultBitmap)
                    val finalMatrix = Matrix(matrixCopy)
                    finalMatrix.postRotate(rotAngle, viewWidth / 2f, viewHeight / 2f)
                    if (mirrored) finalMatrix.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                    finalMatrix.postSkew(perspFactor, 0f, viewWidth / 2f, viewHeight / 2f)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    paint.colorFilter = ColorMatrixColorFilter(currentMatrix)
                    canvas.drawBitmap(bitmap, finalMatrix, paint)

                    drawingBitmap?.let { db ->
                        canvas.drawBitmap(db, 0f, 0f, null)
                        try { if (!db.isRecycled) db.recycle() } catch (_: Throwable) {}
                    }

                    outputBitmap = resultBitmap
                    if (cropActive && rect.width() > 0 && rect.height() > 0 && viewRect.width() > 0 && viewRect.height() > 0) {
                        val cropLeft = (rect.left - viewRect.left).coerceIn(0, resultBitmap.width - 1)
                        val cropTop = (rect.top - viewRect.top).coerceIn(0, resultBitmap.height - 1)
                        val cropRight = (rect.right - viewRect.left).coerceIn(cropLeft + 1, resultBitmap.width)
                        val cropBottom = (rect.bottom - viewRect.top).coerceIn(cropTop + 1, resultBitmap.height)
                        val width = cropRight - cropLeft
                        val height = cropBottom - cropTop

                        if (width > 0 && height > 0 && (width < resultBitmap.width || height < resultBitmap.height)) {
                            outputBitmap = Bitmap.createBitmap(resultBitmap, cropLeft, cropTop, width, height)
                        }
                    } else if (!cropActive) {
                        val mappedBounds = android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                        finalMatrix.mapRect(mappedBounds)
                        if (mappedBounds.intersect(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat())) {
                            val cropLeft = kotlin.math.floor(mappedBounds.left.toDouble()).toInt().coerceIn(0, resultBitmap.width - 1)
                            val cropTop = kotlin.math.floor(mappedBounds.top.toDouble()).toInt().coerceIn(0, resultBitmap.height - 1)
                            val cropRight = kotlin.math.ceil(mappedBounds.right.toDouble()).toInt().coerceIn(cropLeft + 1, resultBitmap.width)
                            val cropBottom = kotlin.math.ceil(mappedBounds.bottom.toDouble()).toInt().coerceIn(cropTop + 1, resultBitmap.height)
                            val width = cropRight - cropLeft
                            val height = cropBottom - cropTop

                            if (width >= 16 && height >= 16 && (width < resultBitmap.width || height < resultBitmap.height)) {
                                outputBitmap = Bitmap.createBitmap(resultBitmap, cropLeft, cropTop, width, height)
                            }
                        }
                    }
                }

                val finalBitmap = if (isStickerMode && (outputBitmap.width > 512 || outputBitmap.height > 512)) {
                    val maxDim = 512f
                    val scale = (maxDim / maxOf(outputBitmap.width, outputBitmap.height)).coerceAtMost(1f)
                    val targetW = (outputBitmap.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (outputBitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(outputBitmap, targetW, targetH, true)
                } else {
                    outputBitmap
                }

                val file = if (isStickerMode) {
                    File(cacheDir, "sticker_edited_${System.currentTimeMillis()}.png")
                } else {
                    File(cacheDir, "avatar_edited_${System.currentTimeMillis()}.jpg")
                }

                val jpegQuality = if (hdQuality) 94 else 82
                FileOutputStream(file).use { out ->
                    if (isStickerMode) {
                        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                    }
                    out.flush()
                }

                if (finalBitmap != outputBitmap && !finalBitmap.isRecycled) {
                    try { finalBitmap.recycle() } catch (_: Throwable) {}
                }
                if (outputBitmap != resultBitmapToRecycle && !outputBitmap.isRecycled) {
                    try { outputBitmap.recycle() } catch (_: Throwable) {}
                }
                if (resultBitmapToRecycle != null && !resultBitmapToRecycle.isRecycled) {
                    try { resultBitmapToRecycle.recycle() } catch (_: Throwable) {}
                }

                val uriStr = Uri.fromFile(file).toString()
                val resultIntent = Intent().apply {
                    putExtra("EDITED_IMAGE_URI", uriStr)
                    putExtra("EXTRA_IMAGE_URI", uriStr)
                    putExtra("EXTRA_CAPTION", captionText)
                    data = Uri.parse(uriStr)
                }
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                isSaving = false
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) setOriginalAndFinish()
                }
            } finally {
                executor.shutdown()
            }
        }
    }

    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2f, y / 2f)
    }

    private fun updateBrushPreview(b: ActivityPhotoEditorContentBinding, size: Float) {
        val density = resources.displayMetrics.density
        val pixelSize = (size * density).toInt().coerceAtLeast(4)
        val params = b.brushPreview.layoutParams
        params.width = pixelSize
        params.height = pixelSize
        b.brushPreview.layoutParams = params
        b.brushPreview.alpha = brushHardness
        b.brushPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
    }

    private fun showOriginalLayout(b: ActivityPhotoEditorContentBinding) {
        if (isOriginalLayoutDismissed || isImeCurrentlyVisible) return
        if (b.layoutSetOriginal.visibility == View.VISIBLE && b.layoutSetOriginal.alpha == 1f) return

        b.layoutSetOriginal.visibility = View.VISIBLE
        b.layoutSetOriginal.alpha = 0f
        b.layoutSetOriginal.translationY = 24f
        b.layoutSetOriginal.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideOriginalLayoutWithTimer(b: ActivityPhotoEditorContentBinding, onEnd: () -> Unit) {
        b.pbCloseTimer.visibility = View.GONE
        b.layoutSetOriginal.animate()
            .alpha(0f)
            .translationY(24f)
            .setDuration(200)
            .withEndAction {
                b.layoutSetOriginal.visibility = View.GONE
                b.layoutSetOriginal.translationY = 0f
                b.layoutSetOriginal.alpha = 1f
                onEnd()
            }
            .start()
    }

    private fun getExifRotation(uri: Uri): Int {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return 0
            val exif = android.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            return when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            return 0
        }
    }

    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var decodedHeader = false

            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                    if (options.outWidth > 0 && options.outHeight > 0) decodedHeader = true
                }
            } catch (_: Throwable) {}

            if (!decodedHeader) {
                val rawPath = uri.path ?: uri.toString().removePrefix("file://")
                if (File(rawPath).exists()) {
                    BitmapFactory.decodeFile(rawPath, options)
                    if (options.outWidth > 0 && options.outHeight > 0) decodedHeader = true
                }
            }

            if (!decodedHeader || options.outWidth <= 0 || options.outHeight <= 0) return null

            val targetMaxW = maxOf(resources.displayMetrics.widthPixels, 2048)
            val targetMaxH = maxOf(resources.displayMetrics.heightPixels, 2048)
            var inSampleSize = 1

            if (options.outHeight > targetMaxH || options.outWidth > targetMaxW) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize > targetMaxH || halfWidth / inSampleSize > targetMaxW) {
                    inSampleSize *= 2
                }
            }

            val finalOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inMutable = true
            }

            var decodedBitmap: Bitmap? = try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, finalOptions)
                }
            } catch (_: Throwable) { null }

            if (decodedBitmap == null) {
                val rawPath = uri.path ?: uri.toString().removePrefix("file://")
                if (File(rawPath).exists()) {
                    decodedBitmap = BitmapFactory.decodeFile(rawPath, finalOptions)
                }
            }

            if (decodedBitmap == null) return null

            val rotation = getExifRotation(uri)
            return if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                if (rotated != decodedBitmap && !decodedBitmap.isRecycled) {
                    try { decodedBitmap.recycle() } catch (_: Throwable) {}
                }
                rotated
            } else {
                decodedBitmap
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    override fun finish() {
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }
}
