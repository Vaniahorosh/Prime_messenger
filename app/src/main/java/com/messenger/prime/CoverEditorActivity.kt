package com.messenger.prime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.messenger.prime.databinding.ActivityCoverEditorBinding
import java.io.File
import java.io.FileOutputStream

class CoverEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoverEditorBinding
    private var selectedUri: Uri? = null
    private var loadedBitmap: Bitmap? = null

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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            loadPreview(uri)
        } else if (selectedUri == null) {
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityCoverEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.coverEditorRoot) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            binding.blurHeader.setPadding(0, statusBarHeight, 0, 0)
            binding.layoutBottomBar.setPadding(0, 0, 0, navBarHeight)
            insets
        }

        applyAccentColor()
        setupTouchGestures()

        val uriExtra = intent.getStringExtra("EXTRA_IMAGE_URI") ?: intent.getStringExtra("COVER_URI")
        if (uriExtra != null) {
            selectedUri = Uri.parse(uriExtra)
            loadPreview(selectedUri!!)
        } else {
            pickImageLauncher.launch("image/*")
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnApplyCover.setOnClickListener { confirmAndSave() }
        binding.btnSelectAnother.setOnClickListener { pickImageLauncher.launch("image/*") }
    }

    private fun applyAccentColor() {
        val accent = ColorAccentManager.getCurrentAccentColor(this)
        binding.btnApplyCover.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchGestures() {
        binding.ivCoverPreview.scaleType = ImageView.ScaleType.MATRIX

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                binding.ivCoverPreview.imageMatrix = matrix
                return true
            }
        })

        binding.ivCoverPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
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
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
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
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                val timestamp = System.currentTimeMillis()
                val permanentCover = File(filesDir, "my_profile_cover_${timestamp}.jpg")

                if (loadedBitmap != null && binding.ivCoverPreview.width > 0 && binding.ivCoverPreview.height > 0) {
                    val viewWidth = binding.ivCoverPreview.width
                    val viewHeight = binding.ivCoverPreview.height
                    val cropped = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(cropped)
                    canvas.drawBitmap(loadedBitmap!!, matrix, null)
                    FileOutputStream(permanentCover).use { out ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
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
                    val resultIntent = Intent().apply {
                        putExtra("COVER_URI", coverUriStr)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    PrimeNotification.show(this, "Обложка профиля обновлена")
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { finish() }
            }
        }
    }
}
