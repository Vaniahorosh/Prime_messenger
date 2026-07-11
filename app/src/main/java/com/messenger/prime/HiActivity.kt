package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityHiBinding // Замени com.example.prime на свой package

class HiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiBinding

    // Все элементы экрана, которые должны красиво погаснуть перед переходом
    private val allViews: List<View> by lazy {
        listOf(
            binding.btnExit,
            binding.ivLogo,
            binding.tvSlogan,
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
            // Если уже вошли, молча перекидываем в список чатов и закрываем этот экран
            startActivity(Intent(this, ChatListActivity::class.java))
            finish()
            return // Останавливаем выполнение остального кода в HiActivity
        }

        // 2. ЕСЛИ НЕ АВТОРИЗОВАНЫ - ГРУЗИМ ОБЫЧНЫЙ ЭКРАН ПРИВЕТСТВИЯ
        binding = ActivityHiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Делаем статус-бар синим, а иконки светлыми (false)
        setDynamicStatusBar(R.color.prime_background_blue, false)

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

        // Добавим Material фишку: плавное появление элементов при открытии экрана
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000 // 1 секунда
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvSlogan.startAnimation(fadeIn)
        binding.btnPrime.startAnimation(fadeIn)

        // Обработка нажатия на кнопку "Прайме!"
        binding.btnPrime.setOnClickListener {
            // Блокируем повторные нажатия, пока идёт анимация исчезновения
            binding.btnPrime.isEnabled = false
            binding.btnExit.isEnabled = false

            fadeOutAndNavigateToLogin()
        }

        // Обработка кнопки "Выход" в левом верхнем углу
        binding.btnExit.setOnClickListener {
            // Полностью закрываем приложение
            finishAffinity()
        }
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