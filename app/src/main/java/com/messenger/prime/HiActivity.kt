package com.messenger.prime

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.messenger.prime.databinding.ActivityHiBinding

class HiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentDataIndex = -1

    private data class DynamicContent(val slogan: String, val button: String)

    private val contents = listOf(
        DynamicContent("Всегда будь в", "Прайме!"),
        DynamicContent("Всегда сообщение", "Быстрее!"),
        DynamicContent("Всегда будь на", "Связи!"),
        DynamicContent("Всегда будь в", "Приватности!"),
        DynamicContent("Всегда мы", "Кастомнее!")
    )

    // Все элементы экрана, которые должны красиво погаснуть перед переходом
    private val allViews: List<View> by lazy {
        listOf(
            binding.btnExit,
            binding.ivLogo,
            binding.textSwitcherSlogan,
            binding.btnPrime,
            binding.tvLicense
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. ПРОВЕРЯЕМ АВТОРИЗАЦИЮ ПЕРЕД ЗАГРУЗКОЙ ЭКРАНА
        val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            startActivity(Intent(this, ChatListActivity::class.java))
            finish()
            return
        }

        binding = ActivityHiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setDynamicStatusBar(R.color.prime_background_blue, false)

        setupTextSwitcher()
        startDynamicSequence()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            
            val headerParams = binding.topHeader.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            headerParams.height = systemBarsInsets.top + (64 * resources.displayMetrics.density).toInt()
            binding.topHeader.layoutParams = headerParams

            val backParams = binding.btnExit.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            backParams.topMargin = systemBarsInsets.top + (8 * resources.displayMetrics.density).toInt()
            binding.btnExit.layoutParams = backParams

            windowInsets
        }

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        binding.ivLogo.startAnimation(fadeIn)
        binding.textSwitcherSlogan.startAnimation(fadeIn)
        binding.btnPrime.startAnimation(fadeIn)

        binding.btnPrime.setOnClickListener {
            binding.btnPrime.isEnabled = false
            binding.btnExit.isEnabled = false
            handler.removeCallbacksAndMessages(null) // Останавливаем таймер
            fadeOutAndNavigateToLogin()
        }

        binding.btnExit.setOnClickListener {
            finishAffinity()
        }
    }

    private fun setupTextSwitcher() {
        binding.textSwitcherSlogan.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@HiActivity, R.color.prime_button_teal))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }

        binding.textSwitcherSlogan.inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_in)
        binding.textSwitcherSlogan.outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_out)
    }

    private fun startDynamicSequence() {
        val runnable = object : Runnable {
            override fun run() {
                var nextIndex: Int
                do {
                    nextIndex = (contents.indices).random()
                } while (nextIndex == currentDataIndex)

                currentDataIndex = nextIndex
                val content = contents[currentDataIndex]

                // Смена текста слогана с анимацией вылета
                binding.textSwitcherSlogan.setText(content.slogan)

                // Смена текста кнопки с плавным затуханием
                binding.btnPrime.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction {
                        binding.btnPrime.text = content.button
                        binding.btnPrime.animate().alpha(1f).setDuration(400).start()
                    }
                    .start()

                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
    }

    override fun onResume() {
        super.onResume()

        // При возврате на этот экран (например, по кнопке "Назад" с LoginActivity)
        // Activity не пересоздаётся заново — восстанавливаем видимость и кликабельность
        // элементов, которые могли остаться погашенными после fadeOutAndNavigateToLogin().
        if (::binding.isInitialized) {
            allViews.forEach { view ->
                view.animate().cancel()
                view.alpha = 1f
            }
            binding.btnPrime.isEnabled = true
            binding.btnExit.isEnabled = true
        }
    }

    /**
     * Плавно гасит все элементы экрана (fade-out), и только после этого
     * запускает переход на LoginActivity.
     */
    private fun fadeOutAndNavigateToLogin() {
        val fadeOutDuration = 300L
        var finishedCount = 0
        val totalCount = allViews.size

        allViews.forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(fadeOutDuration)
                .withEndAction {
                    finishedCount++
                    // Как только погас последний элемент — переходим на экран логина
                    if (finishedCount == totalCount) {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                .start()
        }
    }
}