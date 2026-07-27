package com.messenger.prime

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
        setupAspectWheel()

        val uriString = intent.getStringExtra("EXTRA_IMAGE_URI")
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            try {
                originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                }
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

    private fun setupAspectWheel() {
        val ratios = listOf("1:1", "4:3", "16:9", "Full")
        val adapter = AspectAdapter(ratios)
        binding.rvAspectRatios.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAspectRatios.adapter = adapter
        
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvAspectRatios)

        binding.rvAspectRatios.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var lastCenterPos = -1
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val center = recyclerView.width / 2f
                val d0 = 0f
                val d1 = 0.6f * center
                val s0 = 1.2f
                val s1 = 0.8f
                
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenter = (child.left + child.right) / 2f
                    val d = Math.abs(center - childCenter)
                    val scale = s0 + (s1 - s0) * (d - d0) / (d1 - d0)
                    val finalScale = Math.max(s1, Math.min(s0, scale))
                    child.scaleX = finalScale
                    child.scaleY = finalScale
                    child.alpha = 0.3f + (1f - 0.3f) * (finalScale - s1) / (s0 - s1)
                }

                // Реактивное переключение пропорций (усиленная стабильность)
                val centerView = snapHelper.findSnapView(recyclerView.layoutManager)
                if (centerView != null) {
                    val pos = recyclerView.getChildAdapterPosition(centerView)
                    if (pos != RecyclerView.NO_POSITION) {
                        val actualPos = pos % ratios.size
                        if (actualPos != lastCenterPos) {
                            lastCenterPos = actualPos
                            applyAspectRatio(ratios[actualPos])
                            binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Дополнительная фиксация при остановке
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(recyclerView.layoutManager)
                    if (centerView != null) {
                        val pos = recyclerView.getChildAdapterPosition(centerView)
                        if (pos != RecyclerView.NO_POSITION) {
                            val actualPos = pos % ratios.size
                            if (actualPos != lastCenterPos) {
                                lastCenterPos = actualPos
                                applyAspectRatio(ratios[actualPos])
                            }
                        }
                    }
                }
            }
        })

        binding.rvAspectRatios.post {
            val itemWidth = (120 * resources.displayMetrics.density).toInt()
            val padding = binding.rvAspectRatios.width / 2 - itemWidth / 2
            binding.rvAspectRatios.setPadding(padding, 0, padding, 0)
            
            // Идеальное центрирование начальной позиции
            val mid = (Int.MAX_VALUE / 2)
            val offset = mid % ratios.size
            val startPos = mid - offset
            (binding.rvAspectRatios.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(startPos, padding)
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
        
        binding.btnSetOriginal.setOnClickListener { setOriginalAndFinish() }

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
        }
        binding.sliderBrushHardness.addOnChangeListener { _, value, _ -> 
            brushHardness = value
            binding.drawingView.setHardness(brushHardness)
        }
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

        binding.ivEditorPreview.setOnTouchListener { v: View, event: MotionEvent ->
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
        binding.layoutGroupLight.visibility = View.GONE
        binding.layoutGroupGeometry.visibility = View.GONE
        binding.layoutGroupBrush.visibility = View.GONE
        
        if (!isCurrentlyVisible) {
            binding.groupSlidersContainer.visibility = View.VISIBLE
            group.visibility = View.VISIBLE
            binding.tsEditorTitle.setText(title)
            binding.btnSetOriginal.visibility = View.GONE
            binding.drawingView.visibility = if (group == binding.layoutGroupBrush) View.VISIBLE else View.GONE
        } else {
            binding.groupSlidersContainer.visibility = View.GONE
            binding.tsEditorTitle.setText("Редактор")
            binding.btnSetOriginal.visibility = View.VISIBLE
            binding.drawingView.visibility = View.GONE
        }
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
        binding.btnSetOriginal.visibility = View.VISIBLE
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
                        mainMatrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        mainMatrix.postScale(scale, scale, midPoint.x, midPoint.y)
                        
                        // Синхронизация слайдера масштаба
                        val values = FloatArray(9)
                        mainMatrix.getValues(values)
                        val currentScale = values[Matrix.MSCALE_X]
                        binding.sliderScale.value = currentScale.coerceIn(0.5f, 3.0f)
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

    inner class AspectAdapter(private val ratios: List<String>) : 
        RecyclerView.Adapter<AspectAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val actualPos = position % ratios.size
            (holder.itemView as TextView).apply {
                text = ratios[actualPos]
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                layoutParams = ViewGroup.LayoutParams((120 * resources.displayMetrics.density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
        override fun getItemCount() = Int.MAX_VALUE
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
