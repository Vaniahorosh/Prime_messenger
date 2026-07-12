package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.messenger.prime.databinding.ActivityRegisterBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var userLogin: String = ""

    // Переменная для хранения ссылки на выбранное фото
    private var avatarUri: Uri? = null

    // Инструмент для открытия галереи и получения результата
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // Магия: Сохраняем права на чтение этой картинки НАВСЕГДА
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            avatarUri = uri
            binding.ivSelectedAvatar.setImageURI(uri)
            binding.tvAvatarHint.visibility = View.GONE
        }
    }

    // Все view с контентом (кроме tvWelcome — у него отдельная анимация по дуге)
    private val contentViews: List<View> by lazy {
        listOf(
            binding.cvAvatar,
            binding.inputLayoutName,
            binding.inputLayoutPassword,
            binding.tvPasswordHint,
            binding.btnForward
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, imeInsets.bottom)
            windowInsets
        }

        setDynamicStatusBar(R.color.prime_background_blue, false)

        // Контент прячем сразу — появится после того, как фон развернётся
        contentViews.forEach { it.alpha = 0f }
        binding.tvWelcome.alpha = 0f

        startHeaderExpandAnimation()

        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        userLogin = intent.getStringExtra("EXTRA_LOGIN") ?: ""
        binding.tvWelcome.text = "Будем знакомы, $userLogin!"

        // Открываем галерею по клику на CardView
        binding.cvAvatar.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnForward.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (name.isEmpty()) {
                binding.inputLayoutName.error = "Как вас зовут?"
                binding.inputLayoutName.shake()
                binding.btnForward.shake()
                return@setOnClickListener
            } else {
                binding.inputLayoutName.error = null
            }

            if (password.length < 8) {
                binding.inputLayoutPassword.error = "Пароль должен быть не менее 8 символов"
                binding.inputLayoutPassword.shake()
                binding.btnForward.shake()
                return@setOnClickListener
            } else {
                binding.inputLayoutPassword.error = null
            }

            val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(userLogin, password)
            editor.putString("${userLogin}_name", name)

            // Если аватарка выбрана, сохраняем путь к ней
            if (avatarUri != null) {
                editor.putString("${userLogin}_avatar", avatarUri.toString())
            }

            editor.putBoolean("is_logged_in", true)
            editor.putString("current_user", userLogin)

            editor.apply()

            startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finishAffinity()
        }
    }

    /**
     * Анимация разворота синей плашки topHeader на весь экран.
     * Стартует с небольшой задержкой, чтобы совпасть с системным переходом slide_in_right,
     * а после завершения — запускает "прилёт" заголовка по дуге и fade-in остального контента.
     */
    private fun startHeaderExpandAnimation() {
        val header = binding.topHeader
        val params = header.layoutParams

        val startHeight = (65 * resources.displayMetrics.density).toInt()
        val endHeight = resources.displayMetrics.heightPixels

        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            params.height = valueAnimator.animatedValue as Int
            header.layoutParams = params
        }

        animator.duration = 500
        animator.startDelay = 180 // небольшая задержка под slide_in_right
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startWelcomeArcAnimation()
                fadeInContent()
            }
        })

        animator.start()
    }

    /**
     * "Прилёт" заголовка tvWelcome по дуге из-за верхнего края экрана:
     * сначала смещение в сторону и вверх, затем изгиб вниз-к-центру на свою финальную позицию.
     * Реализовано через один ValueAnimator с прогрессом 0..1, где X и Y двигаются
     * по разным кривым — так получается дугообразная, а не прямая траектория.
     */
    private fun startWelcomeArcAnimation() {
        val title = binding.tvWelcome
        val density = resources.displayMetrics.density

        // Стартовая точка: выше экрана и немного смещена вбок
        val startY = -(140 * density)
        val startX = -(60 * density)

        // Пиковая точка дуги (верхушка "прыжка" по горизонтали, ещё выше финальной позиции)
        val peakX = 20 * density

        title.translationX = startX
        title.translationY = startY
        title.alpha = 0f
        title.rotation = -8f
        title.visibility = View.VISIBLE

        val arcAnimator = ValueAnimator.ofFloat(0f, 1f)
        arcAnimator.duration = 550
        arcAnimator.interpolator = AccelerateDecelerateInterpolator()

        arcAnimator.addUpdateListener { animator ->
            val t = animator.animatedValue as Float

            // X: быстро долетает почти до цели, с лёгким перелётом через peakX
            val x = when {
                t < 0.6f -> {
                    val localT = t / 0.6f
                    startX + (peakX - startX) * localT
                }
                else -> {
                    val localT = (t - 0.6f) / 0.4f
                    peakX + (0f - peakX) * localT
                }
            }

            // Y: падает по нарастающей (имитация "провисания" дуги под гравитацией)
            val y = startY * (1 - t) * (1 - t)

            // Alpha: быстро проявляется в первой трети пути
            val alpha = (t / 0.4f).coerceIn(0f, 1f)

            // Rotation: выравнивается к 0 к концу движения
            val rotation = -8f * (1 - t)

            title.translationX = x
            title.translationY = y
            title.alpha = alpha
            title.rotation = rotation
        }

        arcAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Подчищаем значения на всякий случай, чтобы не осталось погрешностей округления
                title.translationX = 0f
                title.translationY = 0f
                title.rotation = 0f
                title.alpha = 1f
            }
        })

        arcAnimator.start()
    }

    private fun fadeInContent() {
        contentViews.forEach { view ->
            view.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
}