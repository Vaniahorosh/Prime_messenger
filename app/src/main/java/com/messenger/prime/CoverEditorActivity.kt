package com.messenger.prime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.messenger.prime.databinding.ActivityCoverEditorBinding
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.sqrt

class CoverEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoverEditorBinding
    private var selectedUri: Uri? = null
    private var loadedBitmap: Bitmap? = null
    private var activePhotoPickerSheet: PhotoPickerBottomSheet? = null

    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            activePhotoPickerSheet?.refreshMedia()
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedUri = uri
                loadPreview(uri)
            } else if (selectedUri == null) {
                finish()
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge(isDarkIcons = !isDark)

        binding = ActivityCoverEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val overlayColor = getDefaultBlurOverlayColor(this)
        val bg = window.decorView.background
        binding.blurHeader.setupBlur(binding.coverEditorRoot, 20f, overlayColor, bg)
        binding.layoutBottomBar.setupBlur(binding.coverEditorRoot, 20f, overlayColor, bg)

        ViewCompat.setOnApplyWindowInsetsListener(binding.coverEditorRoot) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val safeTop = maxOf(sysBars.top, cutout.top)
            val safeBottom = maxOf(sysBars.bottom, cutout.bottom)

            v.updatePadding(left = cutout.left, right = cutout.right)
            binding.blurHeader.setPadding(0, safeTop, 0, 0)
            binding.layoutBottomBar.setPadding(0, 0, 0, safeBottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.coverEditorRoot)

        applyAccentColor()
        setupTouchGestures()

        val uriExtra = intent.getStringExtra("EXTRA_IMAGE_URI") ?: intent.getStringExtra("COVER_URI")
        if (!uriExtra.isNullOrBlank()) {
            selectedUri = Uri.parse(uriExtra)
            loadPreview(selectedUri!!)
        } else {
            showCoverPhotoPicker()
        }

        binding.btnCancel.setOnClickListener {
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }
        binding.btnApplyCover.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            confirmAndSave()
        }
        binding.btnSelectAnother.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            showCoverPhotoPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        setBlurAutoUpdateSafe(binding.blurHeader, true)
        setBlurAutoUpdateSafe(binding.layoutBottomBar, true)
    }

    override fun onPause() {
        setBlurAutoUpdateSafe(binding.blurHeader, false)
        setBlurAutoUpdateSafe(binding.layoutBottomBar, false)
        super.onPause()
    }

    private fun showCoverPhotoPicker() {
        val sheet = PhotoPickerBottomSheet(
            activity = this,
            title = "Обложка профиля",
            allowGif = false,
            showPresets = false,
            onMediaSelected = { uri, _ ->
                activePhotoPickerSheet = null
                selectedUri = uri
                loadPreview(uri)
            },
            onOpenSystemPicker = {
                activePhotoPickerSheet = null
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRequestPermission = {
                galleryPermissionLauncher.launch(PhotoPickerBottomSheet.getRequiredImagePermissions())
            },
            onDismissWithoutSelection = {
                activePhotoPickerSheet = null
                if (selectedUri == null && !isFinishing) {
                    finish()
                }
            }
        )
        activePhotoPickerSheet = sheet
        sheet.show()
    }

    private fun applyAccentColor() {
        val accent = ColorAccentManager.getCurrentAccentColor(this)
        binding.btnApplyCover.backgroundTintList = ColorStateList.valueOf(accent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchGestures() {
        binding.ivCoverPreview.scaleType = ImageView.ScaleType.MATRIX

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                if (!scaleFactor.isFinite() || scaleFactor <= 0f) return false
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                binding.ivCoverPreview.imageMatrix = matrix
                return true
            }
        })

        binding.ivCoverPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = ZOOM
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    mode = NONE
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG && !scaleGestureDetector.isInProgress) {
                        matrix.set(savedMatrix)
                        val dx = event.x - start.x
                        val dy = event.y - start.y
                        matrix.postTranslate(dx, dy)
                        binding.ivCoverPreview.imageMatrix = matrix
                    }
                }
            }
            true
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

    private fun loadPreview(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    loadedBitmap = resource
                    binding.ivCoverPreview.setImageBitmap(resource)
                    resetMatrixForBitmap(resource)
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
    }

    private fun resetMatrixForBitmap(bitmap: Bitmap) {
        val viewWidth = binding.ivCoverPreview.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val viewHeight = binding.ivCoverPreview.height.takeIf { it > 0 } ?: (240 * resources.displayMetrics.density).toInt()

        val scale: Float
        val dx: Float
        val dy: Float

        if (bitmap.width * viewHeight > viewWidth * bitmap.height) {
            scale = viewHeight.toFloat() / bitmap.height.toFloat()
            dx = (viewWidth - bitmap.width * scale) * 0.5f
            dy = 0f
        } else {
            scale = viewWidth.toFloat() / bitmap.width.toFloat()
            dx = 0f
            dy = (viewHeight - bitmap.height * scale) * 0.5f
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        binding.ivCoverPreview.imageMatrix = matrix
    }

    private fun confirmAndSave() {
        val uri = selectedUri ?: return
        val bmp = loadedBitmap
        val viewWidth = binding.ivCoverPreview.width
        val viewHeight = binding.ivCoverPreview.height
        val matrixCopy = Matrix(matrix)

        Executors.newSingleThreadExecutor().execute {
            try {
                val timestamp = System.currentTimeMillis()
                val permanentCover = File(filesDir, "my_profile_cover_${timestamp}.jpg")

                if (bmp != null && viewWidth > 0 && viewHeight > 0) {
                    val cropped = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(cropped)
                    canvas.drawBitmap(bmp, matrixCopy, null)
                    FileOutputStream(permanentCover).use { out ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    cropped.recycle()
                } else {
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(permanentCover).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val coverUriStr = Uri.fromFile(permanentCover).toString()
                val sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                val currentUser = sp.getString("current_user", "") ?: ""
                sp.edit().putString("${currentUser}_cover", coverUriStr).apply()

                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    val resultIntent = Intent().apply {
                        putExtra("COVER_URI", coverUriStr)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    PrimeNotification.show(this, "Обложка профиля обновлена")
                    finish()
                    PrimeTransitions.applyCloseTransition(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) finish()
                }
            }
        }
    }
}
