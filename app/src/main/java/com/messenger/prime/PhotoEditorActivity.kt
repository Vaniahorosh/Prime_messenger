package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ActivityPhotoEditorBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import java.io.File
import java.io.FileOutputStream

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditorBinding
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
    private var currentBrushColor = Color.RED
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
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        setupTitleSwitcher()
        setupAspectSelector()

        val uriString = intent.getStringExtra("EXTRA_IMAGE_URI")
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            try {
                originalBitmap = loadOptimizedBitmap(uri)
                binding.ivEditorPreview.setImageBitmap(originalBitmap)
                binding.ivEditorPreview.post { centerImage() }
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }

        setupListeners()
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })
    }

    private fun setupTitleSwitcher() {
        binding.tsEditorTitle.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
        binding.tsEditorTitle.setText("Редактор")
    }

    private fun setupAspectSelector() {
        val container = binding.aspectSelectorContainer
        val drop = binding.aspectSelectionDrop
        val labels = listOf(binding.tvRatio11, binding.tvRatio169, binding.tvRatio43, binding.tvRatioFull)

        container.post {
            val cellWidth = container.width / 4f
            val dropParams = drop.layoutParams
            dropParams.width = (cellWidth - 8).toInt() // Небольшой отступ
            drop.layoutParams = dropParams
            
            // Изначально на 1:1 (индекс 0)
            selectRatio(0, false)
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
                    highlightLabel(index)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.performClick()
                    val index = (x / cellWidth).toInt().coerceIn(0, 3)
                    selectRatio(index, true)
                }
            }
            true
        }

        labels.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectRatio(index, true)
            }
        }
    }

    private fun selectRatio(index: Int, animate: Boolean) {
        val container = binding.aspectSelectorContainer
        val drop = binding.aspectSelectionDrop
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
        
        highlightLabel(index)
        val ratios = listOf("1:1", "16:9", "4:3", "Full")
        applyAspectRatio(ratios[index])
        binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun highlightLabel(index: Int) {
        val labels = listOf(binding.tvRatio11, binding.tvRatio169, binding.tvRatio43, binding.tvRatioFull)
        labels.forEachIndexed { i, textView ->
            textView.alpha = if (i == index) 1.0f else 0.5f
            textView.animate()
                .scaleX(if (i == index) 1.1f else 1.0f)
                .scaleY(if (i == index) 1.1f else 1.0f)
                .setDuration(150)
                .start()
        }
    }

    private fun applyAspectRatio(ratio: String) {
        when (ratio) {
            "1:1" -> setCropFrameSize(300, 300)
            "4:3" -> setCropFrameSize(300, 400)
            "16:9" -> setCropFrameSize(300, 533)
            "Full" -> {
                val width = (binding.root.width / resources.displayMetrics.density).toInt() - 40
                val height = (binding.root.height / resources.displayMetrics.density).toInt() - 150
                setCropFrameSize(width, height)
            }
        }
    }

    private fun centerImage() {
        val bitmap = originalBitmap ?: return
        val viewWidth = binding.ivEditorPreview.width.toFloat()
        val viewHeight = binding.ivEditorPreview.height.toFloat()
        val scale = Math.max(viewWidth / bitmap.width, viewHeight / bitmap.height)
        mainMatrix.setScale(scale, scale)
        mainMatrix.postTranslate((viewWidth - bitmap.width * scale) / 2, (viewHeight - bitmap.height * scale) / 2)
        updatePreview()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnReset.setOnClickListener { resetAll() }
        binding.btnUndoTop.setOnClickListener { binding.drawingView.undo() }
        
        binding.btnSetOriginal.setOnClickListener { 
            hideOriginalLayoutWithTimer { setOriginalAndFinish() }
        }
        binding.btnCloseOriginal.setOnClickListener {
            isOriginalLayoutDismissed = true
            hideOriginalLayoutWithTimer { }
        }

        // Инструменты по группам
        binding.toolLight.setOnClickListener { toggleToolGroup(binding.layoutGroupLight, "Освещение") }
        binding.toolGeometry.setOnClickListener { toggleToolGroup(binding.layoutGroupGeometry, "Геометрия") }
        binding.toolBrushGroup.setOnClickListener { toggleToolGroup(binding.layoutGroupBrush, "Рисование") }

        // Группа 1: Свет
        binding.sliderBrightness.addOnChangeListener { _, value, _ -> brightness = value; updatePreview() }
        binding.sliderContrast.addOnChangeListener { _, value, _ -> contrast = value; updatePreview() }
        binding.sliderSaturation.addOnChangeListener { _, value, _ -> saturation = value; updatePreview() }
        binding.sliderTemperature.addOnChangeListener { _, value, _ -> temperature = value; updatePreview() }
        binding.sliderTint.addOnChangeListener { _, value, _ -> tint = value; updatePreview() }

        // Группа 2: Геометрия
        binding.sliderScale.addOnChangeListener { _, value, _ -> 
            val values = FloatArray(9)
            mainMatrix.getValues(values)
            val currentScale = values[Matrix.MSCALE_X]
            val scaleFactor = value / currentScale
            mainMatrix.postScale(scaleFactor, scaleFactor, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
            updatePreview() 
        }
        binding.sliderRotate.addOnChangeListener { _, value, _ -> rotationAngle = value; updatePreview() }
        binding.sliderPerspective.addOnChangeListener { _, value, _ -> perspectiveFactor = value; updatePreview() }
        binding.btnFlipToggle.setOnClickListener { isMirrored = !isMirrored; updatePreview() }

        // Группа 3: Кисть
        binding.sliderBrushSize.addOnChangeListener { _, value, _ -> 
            brushSize = value
            binding.drawingView.setBrushSize(brushSize)
            updateBrushPreview(brushSize)
        }

        binding.sliderBrushSize.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                binding.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(brushSize)
            }

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                binding.brushPreview.visibility = View.GONE
            }
        })
        binding.sliderBrushHardness.addOnChangeListener { _, value, _ -> 
            brushHardness = value
            binding.drawingView.setHardness(brushHardness)
            updateBrushPreview(brushSize)
        }

        binding.sliderBrushHardness.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                binding.brushPreview.visibility = View.VISIBLE
                updateBrushPreview(brushSize)
            }

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                binding.brushPreview.visibility = View.GONE
            }
        })
        binding.tvColorPickerLink.setOnClickListener { showColorPicker() }
        binding.toggleBrushMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                binding.drawingView.setEraser(checkedId == R.id.btnModeEraser)
            }
        }

        binding.drawingView.setOnUndoAvailableListener { available ->
            binding.btnUndoTop.visibility = if (available) View.VISIBLE else View.GONE
            updateTopActionVisibility()
        }

        binding.drawingView.setOnDrawingStateListener { isDrawing ->
            if (isDrawing) {
                binding.groupSlidersContainer.animate().alpha(0f).setDuration(200).start()
            } else {
                binding.groupSlidersContainer.animate().alpha(1f).setDuration(200).start()
            }
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (binding.groupSlidersContainer.visibility == View.VISIBLE) {
                    // Имитируем нажатие на текущую активную кнопку инструмента для закрытия
                    val activeId = when {
                        binding.layoutGroupLight.visibility == View.VISIBLE -> R.id.toolLight
                        binding.layoutGroupGeometry.visibility == View.VISIBLE -> R.id.toolGeometry
                        binding.layoutGroupBrush.visibility == View.VISIBLE -> R.id.toolBrushGroup
                        else -> -1
                    }
                    if (activeId != -1) {
                        findViewById<View>(activeId).performClick()
                    }
                }
                return true
            }
        })

        binding.ivEditorPreview.setOnTouchListener { v: View, event: MotionEvent ->
            gestureDetector.onTouchEvent(event)
            if (mode == PIPETTE) {
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_MOVE) {
                    sampleColor(event.x, event.y)
                    if (event.action == MotionEvent.ACTION_UP) mode = NONE
                }
                return@setOnTouchListener true
            }
            if (binding.drawingView.visibility == View.VISIBLE) return@setOnTouchListener false
            handleTouch(event)
            updateTopActionVisibility()
            true
        }

        binding.btnDone.setOnClickListener { saveAndFinish() }
    }

    private fun sampleColor(x: Float, y: Float) {
        val bitmap = originalBitmap ?: return
        val inverse = Matrix()
        binding.ivEditorPreview.imageMatrix.invert(inverse)
        val pts = floatArrayOf(x, y)
        inverse.mapPoints(pts)
        val px = pts[0].toInt().coerceIn(0, bitmap.width - 1)
        val py = pts[1].toInt().coerceIn(0, bitmap.height - 1)
        currentBrushColor = bitmap.getPixel(px, py)
        binding.drawingView.setBrushColor(currentBrushColor)
        binding.tvColorPickerLink.setTextColor(currentBrushColor)
    }

    private fun setCropFrameSize(w: Int, h: Int) {
        val density = resources.displayMetrics.density
        val params = binding.cropFrame.layoutParams
        params.width = (w * density).toInt().coerceAtMost(binding.root.width - 40)
        params.height = (h * density).toInt().coerceAtMost(binding.root.height - 400)
        binding.cropFrame.layoutParams = params
    }

    private fun toggleToolGroup(group: View, title: String) {
        val isCurrentlyVisible = group.visibility == View.VISIBLE
        val newIndex = when (group.id) {
            R.id.layoutGroupLight -> 0
            R.id.layoutGroupGeometry -> 1
            R.id.layoutGroupBrush -> 2
            else -> -1
        }

        if (isCurrentlyVisible) {
            animateHideToolGroup {
                group.visibility = View.GONE
                binding.groupSlidersContainer.visibility = View.GONE
                binding.tsEditorTitle.setText("Редактор")
                showOriginalLayout()
                binding.drawingView.visibility = View.GONE
                currentToolIndex = -1
            }
        } else {
            val activeGroup = when {
                binding.layoutGroupLight.visibility == View.VISIBLE -> binding.layoutGroupLight
                binding.layoutGroupGeometry.visibility == View.VISIBLE -> binding.layoutGroupGeometry
                binding.layoutGroupBrush.visibility == View.VISIBLE -> binding.layoutGroupBrush
                else -> null
            }

            if (activeGroup != null) {
                val isForward = newIndex > currentToolIndex
                animateHorizontalTransition(activeGroup, group, isForward)
                binding.tsEditorTitle.setText(title)
                binding.drawingView.visibility = if (group == binding.layoutGroupBrush) View.VISIBLE else View.GONE
                currentToolIndex = newIndex
            } else {
                binding.layoutGroupLight.visibility = View.GONE
                binding.layoutGroupGeometry.visibility = View.GONE
                binding.layoutGroupBrush.visibility = View.GONE
                
                group.visibility = View.VISIBLE
                binding.groupSlidersContainer.visibility = View.VISIBLE
                binding.tsEditorTitle.setText(title)
                binding.layoutSetOriginal.visibility = View.GONE
                binding.drawingView.visibility = if (group == binding.layoutGroupBrush) View.VISIBLE else View.GONE
                currentToolIndex = newIndex
                animateShowToolGroup()
            }
        }
    }

    private fun animateHorizontalTransition(oldView: View, newView: View, isForward: Boolean) {
        val screenWidth = binding.root.width.toFloat()
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

    private fun animateShowToolGroup() {
        binding.groupSlidersContainer.clearAnimation()
        binding.groupSlidersContainer.alpha = 0f
        binding.groupSlidersContainer.translationY = 100f
        binding.groupSlidersContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun animateHideToolGroup(onEnd: () -> Unit) {
        binding.groupSlidersContainer.clearAnimation()
        binding.groupSlidersContainer.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(250)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                onEnd()
                binding.groupSlidersContainer.translationY = 0f
            }
            .start()
    }

    private fun updateTopActionVisibility() {
        val hasTransform = brightness != 0f || contrast != 1f || rotationAngle != 0f || perspectiveFactor != 0f || isMirrored || manualScale != 1.0f || saturation != 1f || temperature != 0f || tint != 0f
        val hasDrawing = binding.drawingView.hasHistory()
        val values = FloatArray(9)
        mainMatrix.getValues(values)
        val matrixChanged = values[Matrix.MSCALE_X] != 1.0f || values[Matrix.MTRANS_X] != 0f || values[Matrix.MTRANS_Y] != 0f
        
        binding.btnReset.visibility = if (hasTransform || hasDrawing || matrixChanged) View.VISIBLE else View.GONE
        binding.topActionsLayout.animate().alpha(if (binding.btnReset.visibility == View.VISIBLE || binding.btnUndoTop.visibility == View.VISIBLE) 1f else 0f).setDuration(300).start()
    }

    private fun resetAll() {
        brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
        rotationAngle = 0f; perspectiveFactor = 0f; manualScale = 1.0f; isMirrored = false
        centerImage()
        binding.drawingView.clear()
        binding.drawingView.visibility = View.GONE
        binding.groupSlidersContainer.visibility = View.GONE
        showOriginalLayout()
        binding.tsEditorTitle.setText("Редактор")
        updateTopActionVisibility()
    }

    private fun setOriginalAndFinish() {
        val bitmap = originalBitmap ?: return
        val file = File(cacheDir, "original_${System.currentTimeMillis()}.jpg")
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush(); out.close()
            val resultIntent = Intent(); resultIntent.putExtra("EDITED_IMAGE_URI", Uri.fromFile(file).toString()); setResult(RESULT_OK, resultIntent); finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } catch (e: Exception) { finish() }
    }

    private fun showColorPicker() {
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Prime_AlertDialog).setView(dialogBinding.root).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        
        dialogBinding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentBrushColor)
        dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and currentBrushColor)))
        dialogBinding.etR.setText(Color.red(currentBrushColor).toString())
        dialogBinding.etG.setText(Color.green(currentBrushColor).toString())
        dialogBinding.etB.setText(Color.blue(currentBrushColor).toString())

        dialogBinding.spectrumView.setOnColorChangedListener { color ->
            updateDialogColors(dialogBinding, color)
        }

        dialogBinding.btnPipette.setOnClickListener {
            mode = PIPETTE
            dialog.dismiss()
            PrimeNotification.show(this, "Выберите цвет на фото")
        }

        var isInternalChange = false
        val rgbWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val r = dialogBinding.etR.text.toString().toInt().coerceIn(0, 255)
                    val g = dialogBinding.etG.text.toString().toInt().coerceIn(0, 255)
                    val b = dialogBinding.etB.text.toString().toInt().coerceIn(0, 255)
                    val color = Color.rgb(r, g, b)
                    isInternalChange = true
                    dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
                    isInternalChange = false
                    dialogBinding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (e: Exception) {}
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
                    isInternalChange = false
                    dialogBinding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (e: Exception) {}
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        dialogBinding.etHex.addTextChangedListener(hexWatcher)
        dialogBinding.etR.addTextChangedListener(rgbWatcher); dialogBinding.etG.addTextChangedListener(rgbWatcher); dialogBinding.etB.addTextChangedListener(rgbWatcher)
        dialogBinding.btnApplyColor.setOnClickListener {
            try {
                currentBrushColor = Color.parseColor(dialogBinding.etHex.text.toString())
                binding.drawingView.setBrushColor(currentBrushColor)
                binding.tvColorPickerLink.setTextColor(currentBrushColor)
                dialog.dismiss()
            } catch (e: Exception) {}
        }
        dialog.show()
    }

    private fun updateDialogColors(db: DialogColorPickerBinding, color: Int) {
        db.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        db.etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
        db.etR.setText(Color.red(color).toString())
        db.etG.setText(Color.green(color).toString())
        db.etB.setText(Color.blue(color).toString())
    }

    private fun handleTouch(event: MotionEvent) {
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
                        
                        // Получаем текущий масштаб
                        val values = FloatArray(9)
                        savedMatrix.getValues(values)
                        val currentScale = values[Matrix.MSCALE_X]
                        
                        // Вычисляем итоговый масштаб и ограничиваем его
                        var finalScaleFactor = scaleFactor
                        if (currentScale * scaleFactor < MIN_ZOOM) {
                            finalScaleFactor = MIN_ZOOM / currentScale
                        } else if (currentScale * scaleFactor > MAX_ZOOM) {
                            finalScaleFactor = MAX_ZOOM / currentScale
                        }

                        mainMatrix.set(savedMatrix)
                        mainMatrix.postScale(finalScaleFactor, finalScaleFactor, midPoint.x, midPoint.y)
                        
                        // Синхронизация слайдера масштаба
                        val finalValues = FloatArray(9)
                        mainMatrix.getValues(finalValues)
                        val updatedScale = finalValues[Matrix.MSCALE_X]
                        binding.sliderScale.value = updatedScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
                    }
                }
                updatePreview()
            }
        }
    }

    private fun updatePreview() {
        val displayMatrix = Matrix(mainMatrix)
        displayMatrix.postRotate(rotationAngle, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        if (isMirrored) displayMatrix.postScale(-1f, 1f, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        displayMatrix.postSkew(perspectiveFactor, 0f, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        binding.ivEditorPreview.imageMatrix = displayMatrix
        
        binding.ivEditorPreview.colorFilter = ColorMatrixColorFilter(getCurrentColorMatrix())
        updateTopActionVisibility()
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

    private fun saveAndFinish() {
        val bitmap = originalBitmap ?: return
        val resultBitmap = Bitmap.createBitmap(binding.ivEditorPreview.width, binding.ivEditorPreview.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val finalMatrix = Matrix(mainMatrix)
        finalMatrix.postRotate(rotationAngle, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        if (isMirrored) finalMatrix.postScale(-1f, 1f, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        finalMatrix.postSkew(perspectiveFactor, 0f, binding.ivEditorPreview.width / 2f, binding.ivEditorPreview.height / 2f)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(getCurrentColorMatrix())
        canvas.drawBitmap(bitmap, finalMatrix, paint)
        binding.drawingView.getResultBitmap()?.let { drawingBitmap -> canvas.drawBitmap(drawingBitmap, 0f, 0f, null) }
        val rect = Rect(); binding.cropFrame.getGlobalVisibleRect(rect)
        val viewRect = Rect(); binding.ivEditorPreview.getGlobalVisibleRect(viewRect)
        val left = (rect.left - viewRect.left).coerceIn(0, resultBitmap.width - 1)
        val top = (rect.top - viewRect.top).coerceIn(0, resultBitmap.height - 1)
        val width = rect.width().coerceAtMost(resultBitmap.width - left)
        val height = rect.height().coerceAtMost(resultBitmap.height - top)
        try {
            val croppedBitmap = Bitmap.createBitmap(resultBitmap, left, top, width, height)
            val file = File(cacheDir, "edited_${System.currentTimeMillis()}.jpg")
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

    private fun updateBrushPreview(size: Float) {
        val density = resources.displayMetrics.density
        val pixelSize = (size * density).toInt()
        val params = binding.brushPreview.layoutParams
        params.width = pixelSize
        params.height = pixelSize
        binding.brushPreview.layoutParams = params
        
        // Учитываем прозрачность кисти (жесткость)
        binding.brushPreview.alpha = brushHardness
        
        // Цвет
        binding.brushPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentBrushColor)
    }

    private fun showOriginalLayout() {
        if (isOriginalLayoutDismissed) return
        if (binding.layoutSetOriginal.visibility == View.VISIBLE && binding.layoutSetOriginal.alpha == 1f) return
        
        binding.layoutSetOriginal.visibility = View.VISIBLE
        binding.layoutSetOriginal.alpha = 0f
        binding.layoutSetOriginal.translationX = -400f // Начинаем чуть дальше для более живой анимации
        binding.layoutSetOriginal.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideOriginalLayoutWithTimer(onEnd: () -> Unit) {
        binding.pbCloseTimer.visibility = View.VISIBLE
        binding.pbCloseTimer.progress = 1000
        
        val animator = ValueAnimator.ofInt(1000, 0).apply {
            duration = 800L // Немного ускорим для отзывчивости
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { valueAnimator ->
                binding.pbCloseTimer.progress = valueAnimator.animatedValue as Int
            }
        }
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.layoutSetOriginal.animate()
                    .alpha(0f)
                    .translationX(400f) // Улетает вправо при скрытии
                    .setDuration(300)
                    .withEndAction {
                        binding.layoutSetOriginal.visibility = View.GONE
                        binding.pbCloseTimer.visibility = View.INVISIBLE
                        binding.layoutSetOriginal.translationX = 0f
                        binding.layoutSetOriginal.alpha = 1f
                        onEnd()
                    }
                    .start()
            }
        })
        animator.start()
    }

    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        
        // 1. Получаем размеры фото без загрузки в память
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // 2. Рассчитываем оптимальный масштаб под экран
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

        // 3. Загружаем отмасштабированный битмап
        val finalOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inMutable = true // Чтобы можно было редактировать
        }
        
        val finalStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(finalStream, null, finalOptions)
        finalStream?.close()
        return bitmap
    }

}
