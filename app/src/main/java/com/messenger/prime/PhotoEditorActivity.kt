package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ActivityPhotoEditorContentBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

import java.io.FileOutputStream

class PhotoEditorActivity : AppCompatActivity() {

    private var binding: ActivityPhotoEditorContentBinding? = null
    private var originalBitmap: Bitmap? = null

    // Фильтры
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    private var temperature = 0f
    private var tint = 0f

    // Трансформация
    private var rotationAngle = 0f
    private var perspectiveFactor = 0f
    private var manualScale = 1.0f
    private var isMirrored = false
    
    // Кисть
    private var brushSize = 20f
    private var brushHardness = 1.0f
    private var currentBrushColor = android.graphics.Color.RED
    private var currentToolIndex = -1
    private var isOriginalLayoutDismissed = false

    // Матрицы для трансформации фото
    private val mainMatrix = Matrix()
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
        
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 5.0f
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
        
        setContentView(R.layout.activity_photo_editor)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        val uriString = intent.getStringExtra("EXTRA_IMAGE_URI")

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { _ ->
                        val view = layoutInflater.inflate(R.layout.activity_photo_editor_content, null)
                        val b = ActivityPhotoEditorContentBinding.bind(view)
                        binding = b

                        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                            val topInset = if (systemBarsInsets.top > 0) systemBarsInsets.top else (28 * resources.displayMetrics.density).toInt()
                            b.topBar.setPadding(b.topBar.paddingLeft, topInset, b.topBar.paddingRight, b.topBar.paddingBottom)
                            b.toolsLayout.setPadding(b.toolsLayout.paddingLeft, b.toolsLayout.paddingTop, b.toolsLayout.paddingRight, systemBarsInsets.bottom)
                            windowInsets
                        }

                        setupTitleSwitcher(b)
                        setupAspectSelector(b)

                        val isProfilePhoto = intent.getBooleanExtra("IS_PROFILE_PHOTO", false)
                        if (isProfilePhoto) {
                            b.btnSetOriginal.text = "Поставить фото без редактирования"
                        } else {
                            b.btnSetOriginal.text = "Отправить без редактирования"
                        }

                        if (uriString != null) {
                            val uri = Uri.parse(uriString)
                            try {
                                originalBitmap = loadOptimizedBitmap(uri)
                                b.ivEditorPreview.setImageBitmap(originalBitmap)
                                b.ivEditorPreview.post { centerImage(b) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        setupListeners(b)
                        
                        view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Размытие для системной панели навигации
                BlurView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
                    blurRadius = 20.dp,
                    tint = Color(0x0DFFFFFF)
                )
            }
        }
    }

    private fun setupTitleSwitcher(b: ActivityPhotoEditorContentBinding) {
        b.tsEditorTitle.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
        b.tsEditorTitle.setText("Редактор")
    }

    private fun setupAspectSelector(b: ActivityPhotoEditorContentBinding) {
        val container = b.aspectSelectorContainer
        val drop = b.aspectSelectionDrop
        val labels = listOf<TextView>(b.tvRatio11, b.tvRatio169, b.tvRatio43, b.tvRatioFull)

        container.post {
            val cellWidth = container.width / 4f
            val dropParams = drop.layoutParams
            dropParams.width = (cellWidth - 8).toInt()
            drop.layoutParams = dropParams
            
            selectRatio(b, 0, false)
        }

        container.setOnTouchListener { v, event ->
            val x = event.x
            val cellWidth = container.width / 4f
            
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val halfDrop = drop.width / 2f
                    val targetX = (x - halfDrop).coerceIn(4f, container.width.toFloat() - drop.width - 4f)
                    drop.translationX = targetX
                    
                    val index = (x / cellWidth).toInt().coerceIn(0, 3)
                    highlightLabel(b, index)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.performClick()
                    val index = (x / cellWidth).toInt().coerceIn(0, 3)
                    selectRatio(b, index, true)
                }
            }
            true
        }

        labels.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectRatio(b, index, true)
            }
        }
    }

    private fun selectRatio(b: ActivityPhotoEditorContentBinding, index: Int, animate: Boolean) {
        val container = b.aspectSelectorContainer
        val drop = b.aspectSelectionDrop
        val cellWidth = container.width / 4f
        val targetX = index * cellWidth + (cellWidth - drop.width) / 2f
        
        if (animate) {
            drop.animate()
                .translationX(targetX)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            drop.translationX = targetX
        }
        
        highlightLabel(b, index)
        val ratios = listOf("1:1", "16:9", "4:3", "Full")
        applyAspectRatio(b, ratios[index])
        (b.root as View).performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun highlightLabel(b: ActivityPhotoEditorContentBinding, index: Int) {
        val labels = listOf<TextView>(b.tvRatio11, b.tvRatio169, b.tvRatio43, b.tvRatioFull)
        labels.forEachIndexed { i, textView ->
            textView.alpha = if (i == index) 1.0f else 0.5f
            textView.animate()
                .scaleX(if (i == index) 1.1f else 1.0f)
                .scaleY(if (i == index) 1.1f else 1.0f)
                .setDuration(150)
                .start()
        }
    }

    private fun applyAspectRatio(b: ActivityPhotoEditorContentBinding, ratio: String) {
        when (ratio) {
            "1:1" -> setCropFrameSize(b, 300, 300)
            "4:3" -> setCropFrameSize(b, 300, 400)
            "16:9" -> setCropFrameSize(b, 300, 533)
            "Full" -> {
                val view = b.topBar.rootView
                val width = (view.width / resources.displayMetrics.density).toInt() - 40
                val height = (view.height / resources.displayMetrics.density).toInt() - 150
                setCropFrameSize(b, width, height)
            }
        }
    }

    private fun centerImage(b: ActivityPhotoEditorContentBinding) {
        val bitmap = originalBitmap ?: return
        val viewWidth = b.ivEditorPreview.width.toFloat()
        val viewHeight = b.ivEditorPreview.height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) return
        val scale = Math.max(viewWidth / bitmap.width, viewHeight / bitmap.height)
        mainMatrix.setScale(scale, scale)
        mainMatrix.postTranslate((viewWidth - bitmap.width * scale) / 2, (viewHeight - bitmap.height * scale) / 2)
        updatePreview(b)
    }

    private fun setupListeners(b: ActivityPhotoEditorContentBinding) {
        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnReset.setOnClickListener { resetAll(b) }
        b.btnUndoTop.setOnClickListener { b.drawingView.undo() }
        
        b.btnSetOriginal.setOnClickListener { 
            hideOriginalLayoutWithTimer(b) { setOriginalAndFinish() }
        }
        b.btnCloseOriginal.setOnClickListener {
            isOriginalLayoutDismissed = true
            hideOriginalLayoutWithTimer(b) { }
        }

        b.toolLight.setOnClickListener { toggleToolGroup(b, b.layoutGroupLight, "Освещение") }
        b.toolGeometry.setOnClickListener { toggleToolGroup(b, b.layoutGroupGeometry, "Геометрия") }
        b.toolBrushGroup.setOnClickListener { toggleToolGroup(b, b.layoutGroupBrush, "Рисование") }

        b.sliderBrightness.addOnChangeListener { _, value, _ -> brightness = value; updatePreview(b) }
        b.sliderContrast.addOnChangeListener { _, value, _ -> contrast = value; updatePreview(b) }
        b.sliderSaturation.addOnChangeListener { _, value, _ -> saturation = value; updatePreview(b) }
        b.sliderTemperature.addOnChangeListener { _, value, _ -> temperature = value; updatePreview(b) }
        b.sliderTint.addOnChangeListener { _, value, _ -> tint = value; updatePreview(b) }

        b.sliderScale.addOnChangeListener { _, value, _ -> 
            val values = FloatArray(9)
            mainMatrix.getValues(values)
            val currentScale = values[Matrix.MSCALE_X]
            val scaleFactor = value / currentScale
            mainMatrix.postScale(scaleFactor, scaleFactor, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
            updatePreview(b) 
        }
        b.sliderRotate.addOnChangeListener { _, value, _ -> rotationAngle = value; updatePreview(b) }
        b.sliderPerspective.addOnChangeListener { _, value, _ -> perspectiveFactor = value; updatePreview(b) }
        b.btnFlipToggle.setOnClickListener { isMirrored = !isMirrored; updatePreview(b) }

        b.sliderBrushSize.addOnChangeListener { _, value, _ -> 
            brushSize = value
            b.drawingView.setBrushSize(brushSize)
            updateBrushPreview(b, brushSize)
        }

        b.sliderBrushSize.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                b.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(b, brushSize)
            }

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                b.brushPreview.visibility = View.GONE
            }
        })
        b.sliderBrushHardness.addOnChangeListener { _, value, _ -> 
            brushHardness = value
            b.drawingView.setHardness(brushHardness)
            updateBrushPreview(b, brushSize)
        }

        b.sliderBrushHardness.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                b.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(b, brushSize)
            }

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                b.brushPreview.visibility = View.GONE
            }
        })
        b.tvColorPickerLink.setOnClickListener { showColorPicker(b) }
        b.toggleBrushMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                b.drawingView.setEraser(checkedId == R.id.btnModeEraser)
            }
        }

        b.drawingView.setOnUndoAvailableListener { available ->
            b.btnUndoTop.visibility = if (available) View.VISIBLE else View.GONE
            updateTopActionVisibility(b)
        }

        b.drawingView.setOnDrawingStateListener { isDrawing ->
            if (isDrawing) {
                b.groupSlidersContainer.animate().alpha(0f).setDuration(200).start()
            } else {
                b.groupSlidersContainer.animate().alpha(1f).setDuration(200).start()
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
                        (b.root as View).findViewById<View>(activeId)?.performClick()
                    }
                }
                return true
            }
        })

        b.ivEditorPreview.setOnTouchListener { v: View, event: MotionEvent ->
            gestureDetector.onTouchEvent(event)
            if (mode == PIPETTE) {
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_MOVE) {
                    sampleColor(b, event.x, event.y)
                    if (event.action == MotionEvent.ACTION_UP) {
                        mode = NONE
                        if (b.layoutGroupBrush.visibility == View.VISIBLE) {
                            b.drawingView.visibility = View.VISIBLE
                        }
                    }
                }
                return@setOnTouchListener true
            }
            if (b.drawingView.visibility == View.VISIBLE) return@setOnTouchListener false
            handleTouch(b, event)
            updateTopActionVisibility(b)
            true
        }

        b.btnDone.setOnClickListener { saveAndFinish(b) }
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
        b.tvColorPickerLink.setTextColor(currentBrushColor)
        b.brushPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
    }

    private fun setCropFrameSize(b: ActivityPhotoEditorContentBinding, w: Int, h: Int) {
        val density = resources.displayMetrics.density
        val params = b.cropFrame.layoutParams
        val view = b.topBar.rootView
        params.width = (w * density).toInt().coerceAtMost(view.width - 40)
        params.height = (h * density).toInt().coerceAtMost(view.height - 400)
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
                b.tsEditorTitle.setText("Редактор")
                showOriginalLayout(b)
                b.drawingView.visibility = View.GONE
                currentToolIndex = -1
            }
        } else {
            val activeGroup = when {
                b.layoutGroupLight.visibility == View.VISIBLE -> b.layoutGroupLight
                b.layoutGroupGeometry.visibility == View.VISIBLE -> b.layoutGroupGeometry
                b.layoutGroupBrush.visibility == View.VISIBLE -> b.layoutGroupBrush
                else -> null
            }

            if (activeGroup != null) {
                val isForward = newIndex > currentToolIndex
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
                b.tsEditorTitle.setText(title)
                b.layoutSetOriginal.visibility = View.GONE
                b.drawingView.visibility = if (group == b.layoutGroupBrush) View.VISIBLE else View.GONE
                currentToolIndex = newIndex
                animateShowToolGroup(b)
            }
        }
    }

    private fun animateHorizontalTransition(b: ActivityPhotoEditorContentBinding, oldView: View, newView: View, isForward: Boolean) {
        val view = b.topBar.rootView
        val screenWidth = view.width.toFloat()
        val outTranslation = if (isForward) -screenWidth else screenWidth
        val inTranslation = if (isForward) screenWidth else -screenWidth

        newView.translationX = inTranslation
        newView.visibility = View.VISIBLE
        newView.alpha = 0f

        oldView.animate()
            .translationX(outTranslation)
            .alpha(0f)
            .setDuration(300)
            .withEndAction { 
                oldView.visibility = View.GONE 
                oldView.translationX = 0f
                oldView.alpha = 1f
            }
            .start()

        newView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun animateShowToolGroup(b: ActivityPhotoEditorContentBinding) {
        b.groupSlidersContainer.clearAnimation()
        b.groupSlidersContainer.alpha = 0f
        b.groupSlidersContainer.translationY = 100f
        b.groupSlidersContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun animateHideToolGroup(b: ActivityPhotoEditorContentBinding, onEnd: () -> Unit) {
        b.groupSlidersContainer.clearAnimation()
        b.groupSlidersContainer.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                onEnd()
                b.groupSlidersContainer.translationY = 0f
            }
            .start()
    }

    private fun updateTopActionVisibility(b: ActivityPhotoEditorContentBinding) {
        val hasTransform = brightness != 0f || contrast != 1f || rotationAngle != 0f || perspectiveFactor != 0f || isMirrored || manualScale != 1.0f || saturation != 1f || temperature != 0f || tint != 0f
        val hasDrawing = b.drawingView.hasHistory()
        val values = FloatArray(9)
        mainMatrix.getValues(values)
        val matrixChanged = values[Matrix.MSCALE_X] != 1.0f || values[Matrix.MTRANS_X] != 0f || values[Matrix.MTRANS_Y] != 0f
        
        b.btnReset.visibility = if (hasTransform || hasDrawing || matrixChanged) View.VISIBLE else View.GONE
        b.topActionsLayout.animate().alpha(if (b.btnReset.visibility == View.VISIBLE || b.btnUndoTop.visibility == View.VISIBLE) 1f else 0f).setDuration(250).start()
    }

    private fun resetAll(b: ActivityPhotoEditorContentBinding) {
        brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
        rotationAngle = 0f; perspectiveFactor = 0f; manualScale = 1.0f; isMirrored = false
        centerImage(b)
        b.drawingView.clear()
        b.drawingView.visibility = View.GONE
        b.groupSlidersContainer.visibility = View.GONE
        showOriginalLayout(b)
        b.tsEditorTitle.setText("Редактор")
        updateTopActionVisibility(b)
    }

    private fun setOriginalAndFinish() {
        val bitmap = originalBitmap ?: return
        val file = java.io.File(cacheDir, "original_${System.currentTimeMillis()}.jpg")
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush(); out.close()
            val resultIntent = Intent(); resultIntent.putExtra("EDITED_IMAGE_URI", Uri.fromFile(file).toString()); setResult(RESULT_OK, resultIntent); finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } catch (e: Exception) { finish() }
    }

    private fun showColorPicker(b: ActivityPhotoEditorContentBinding) {
        var isInternalChange = false
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)

        val dialog = AlertDialog.Builder(this, R.style.Theme_Prime_AlertDialog).setView(dialogBinding.root).create()
        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = win.attributes
                    params.blurBehindRadius = 60
                    win.attributes = params
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun updateDialogColors(db: DialogColorPickerBinding, color: Int) {
            if (isInternalChange) return
            isInternalChange = true
            try {
                db.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                val hexStr = String.format("#%06X", (0xFFFFFF and color))
                if (db.etHex.text?.toString() != hexStr) db.etHex.setText(hexStr)
                val rStr = android.graphics.Color.red(color).toString()
                if (db.etR.text?.toString() != rStr) db.etR.setText(rStr)
                val gStr = android.graphics.Color.green(color).toString()
                if (db.etG.text?.toString() != gStr) db.etG.setText(gStr)
                val bStr = android.graphics.Color.blue(color).toString()
                if (db.etB.text?.toString() != bStr) db.etB.setText(bStr)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isInternalChange = false
            }
        }

        dialogBinding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentBrushColor)
        dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and currentBrushColor)))
        dialogBinding.etR.setText(android.graphics.Color.red(currentBrushColor).toString())
        dialogBinding.etG.setText(android.graphics.Color.green(currentBrushColor).toString())
        dialogBinding.etB.setText(android.graphics.Color.blue(currentBrushColor).toString())

        var dialogSelectedColor = currentBrushColor
        dialogBinding.spectrumView.setOnColorChangedListener { color ->
            dialogSelectedColor = color
            updateDialogColors(dialogBinding, color)
        }

        dialogBinding.btnPipette.setOnClickListener {
            mode = PIPETTE
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
                    val b = dialogBinding.etB.text.toString().toInt().coerceIn(0, 255)
                    val color = android.graphics.Color.rgb(r, g, b)
                    isInternalChange = true
                    dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (e: Exception) {
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
                    val color = android.graphics.Color.parseColor(s.toString())
                    isInternalChange = true
                    dialogBinding.etR.setText(android.graphics.Color.red(color).toString())
                    dialogBinding.etG.setText(android.graphics.Color.green(color).toString())
                    dialogBinding.etB.setText(android.graphics.Color.blue(color).toString())
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (e: Exception) {
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
                currentBrushColor = android.graphics.Color.parseColor(hex)
            } catch (e: Exception) {
                currentBrushColor = dialogSelectedColor
            }
            b.drawingView.setBrushColor(currentBrushColor)
            b.tvColorPickerLink.setTextColor(currentBrushColor)
            b.brushPreview.backgroundTintList = ColorStateList.valueOf(currentBrushColor)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun handleTouch(b: ActivityPhotoEditorContentBinding, event: MotionEvent) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { savedMatrix.set(mainMatrix); startPoint.set(event.x, event.y); mode = DRAG }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) { savedMatrix.set(mainMatrix); midPoint(midPoint, event); mode = ZOOM }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    mainMatrix.set(savedMatrix)
                    mainMatrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        val scaleFactor = newDist / oldDist
                        
                        val values = FloatArray(9)
                        savedMatrix.getValues(values)
                        val currentScale = values[Matrix.MSCALE_X]
                        
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
                        b.sliderScale.value = updatedScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
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

    private fun saveAndFinish(b: ActivityPhotoEditorContentBinding) {
        val bitmap = originalBitmap ?: return
        val resultBitmap = Bitmap.createBitmap(b.ivEditorPreview.width, b.ivEditorPreview.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val finalMatrix = Matrix(mainMatrix)
        finalMatrix.postRotate(rotationAngle, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        if (isMirrored) finalMatrix.postScale(-1f, 1f, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        finalMatrix.postSkew(perspectiveFactor, 0f, b.ivEditorPreview.width / 2f, b.ivEditorPreview.height / 2f)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(getCurrentColorMatrix())
        canvas.drawBitmap(bitmap, finalMatrix, paint)
        b.drawingView.getResultBitmap()?.let { drawingBitmap -> canvas.drawBitmap(drawingBitmap, 0f, 0f, null) }
        val rect = Rect(); b.cropFrame.getGlobalVisibleRect(rect)
        val viewRect = Rect(); b.ivEditorPreview.getGlobalVisibleRect(viewRect)
        val left = (rect.left - viewRect.left).coerceIn(0, resultBitmap.width - 1)
        val top = (rect.top - viewRect.top).coerceIn(0, resultBitmap.height - 1)
        val width = rect.width().coerceAtMost(resultBitmap.width - left)
        val height = rect.height().coerceAtMost(resultBitmap.height - top)
        try {
            val croppedBitmap = Bitmap.createBitmap(resultBitmap, left, top, width, height)
            val file = java.io.File(cacheDir, "edited_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush(); out.close()
            val resultIntent = Intent(); resultIntent.putExtra("EDITED_IMAGE_URI", Uri.fromFile(file).toString()); setResult(RESULT_OK, resultIntent); finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } catch (e: Exception) { finish() }
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun updateBrushPreview(b: ActivityPhotoEditorContentBinding, size: Float) {
        val density = resources.displayMetrics.density
        val pixelSize = (size * density).toInt()
        val params = b.brushPreview.layoutParams
        params.width = pixelSize
        params.height = pixelSize
        b.brushPreview.layoutParams = params
        b.brushPreview.alpha = brushHardness
        b.brushPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentBrushColor)
    }

    private fun showOriginalLayout(b: ActivityPhotoEditorContentBinding) {
        if (isOriginalLayoutDismissed) return
        if (b.layoutSetOriginal.visibility == View.VISIBLE && b.layoutSetOriginal.alpha == 1f) return
        
        b.layoutSetOriginal.visibility = View.VISIBLE
        b.layoutSetOriginal.alpha = 0f
        b.layoutSetOriginal.translationX = -400f
        b.layoutSetOriginal.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideOriginalLayoutWithTimer(b: ActivityPhotoEditorContentBinding, onEnd: () -> Unit) {
        b.pbCloseTimer.visibility = View.GONE
        b.layoutSetOriginal.animate()
            .alpha(0f)
            .translationX(400f)
            .setDuration(250)
            .withEndAction {
                b.layoutSetOriginal.visibility = View.GONE
                b.layoutSetOriginal.translationX = 0f
                b.layoutSetOriginal.alpha = 1f
                onEnd()
            }
            .start()
    }

    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            var inSampleSize = 1
            
            if (options.outHeight > screenHeight || options.outWidth > screenWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= screenHeight && halfWidth / inSampleSize >= screenWidth) {
                    inSampleSize *= 2
                }
            }

            val finalOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inMutable = true
            }
            
            val finalStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(finalStream, null, finalOptions)
            finalStream?.close()
            bitmap
        } catch (t: Throwable) {
            t.printStackTrace()
            try {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                val fallbackStream = contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(fallbackStream, null, options)
                fallbackStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
