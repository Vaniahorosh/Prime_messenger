package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.messenger.prime.databinding.ActivityPhotoViewBinding

class PhotoViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewBinding
    private var currentUri: Uri? = null
    private var sourceRect: Rect? = null

    private var isClosing = false
    private var photoScale = 1f
    private var photoTranslateX = 0f
    private var photoTranslateY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: android.view.GestureDetector

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentUri = it
            binding.ivFullPhoto.setImageURI(it)
            
            val data = Intent()
            data.putExtra("NEW_URI", it.toString())
            setResult(RESULT_OK, data)
            PrimeNotification.show(this, "Фото обновлено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val uriString = intent.getStringExtra("EXTRA_URI")
        currentUri = uriString?.let { Uri.parse(it) }
        sourceRect = intent.getParcelableExtra("EXTRA_RECT")

        currentUri?.let { binding.ivFullPhoto.setImageURI(it) }
            ?: binding.ivFullPhoto.setImageResource(R.drawable.ic_person)

        initGestures()
        setupListeners()

        // Скрываем элементы управления для анимации появления
        binding.topToolbar.alpha = 0f
        binding.sideActionsTray.alpha = 0f
        binding.viewBackgroundDim.alpha = 0f

        // Запуск анимации появления
        binding.photoRoot.post { startEnterAnimation() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startExitAnimation()
            }
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        binding.btnEditPhoto.setOnClickListener { pickImage.launch("image/*") }
        
        binding.btnDeletePhoto.setOnClickListener {
            val data = Intent()
            data.putExtra("DELETED", true)
            setResult(RESULT_OK, data)
            currentUri = null
            binding.ivFullPhoto.setImageResource(R.drawable.ic_person)
            PrimeNotification.show(this, "Фото удалено")
            startExitAnimation()
        }

        binding.ivFullPhoto.setOnTouchListener { _, event ->
            if (isClosing) return@setOnTouchListener true
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun startEnterAnimation() {
        val rect = sourceRect ?: return
        
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        val startScale = rect.width().toFloat() / screenWidth
        
        binding.ivFullPhoto.pivotX = 0f
        binding.ivFullPhoto.pivotY = 0f
        binding.ivFullPhoto.scaleX = startScale
        binding.ivFullPhoto.scaleY = startScale
        binding.ivFullPhoto.translationX = rect.left.toFloat()
        binding.ivFullPhoto.translationY = rect.top.toFloat()

        binding.ivFullPhoto.animate()
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.ivFullPhoto.pivotX = screenWidth / 2
                binding.ivFullPhoto.pivotY = screenHeight / 2
            }
            .start()

        binding.viewBackgroundDim.animate().alpha(1f).setDuration(450).start()
        
        binding.topToolbar.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(200).start()
        binding.sideActionsTray.animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(300).start()
    }

    private fun startExitAnimation() {
        if (isClosing) return
        isClosing = true

        val rect = sourceRect ?: run { finish(); return }
        
        binding.topToolbar.animate().alpha(0f).translationY(-50f).setDuration(200).start()
        binding.sideActionsTray.animate().alpha(0f).translationX(50f).setDuration(200).start()

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val targetScale = rect.width().toFloat() / screenWidth

        binding.ivFullPhoto.pivotX = 0f
        binding.ivFullPhoto.pivotY = 0f
        
        binding.ivFullPhoto.animate()
            .scaleX(targetScale).scaleY(targetScale)
            .translationX(rect.left.toFloat())
            .translationY(rect.top.toFloat())
            .setDuration(400)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finish()
                    overridePendingTransition(0, 0)
                }
            })
            .start()

        binding.viewBackgroundDim.animate().alpha(0f).setDuration(400).start()
    }

    private fun initGestures() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                photoScale *= detector.scaleFactor
                photoScale = photoScale.coerceIn(1f, 5f)
                updateTransform()
                return true
            }
        })

        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (photoScale > 1f) {
                    photoTranslateX -= distanceX
                    photoTranslateY -= distanceY
                    updateTransform()
                    return true
                } else if (!isClosing && distanceY < -30) {
                    // Свайп вниз для закрытия
                    startExitAnimation()
                    return true
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (photoScale > 1f) {
                    photoScale = 1f
                    photoTranslateX = 0f
                    photoTranslateY = 0f
                } else {
                    photoScale = 2.5f
                }
                updateTransform()
                return true
            }
        })
    }

    private fun updateTransform() {
        binding.ivFullPhoto.scaleX = photoScale
        binding.ivFullPhoto.scaleY = photoScale
        binding.ivFullPhoto.translationX = photoTranslateX
        binding.ivFullPhoto.translationY = photoTranslateY
    }
}
