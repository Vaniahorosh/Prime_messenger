package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityHiBinding // Замени com.example.prime на свой package

class HiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiBinding

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

        // Добавим Material фишку: плавное появление элементов при открытии экрана
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000 // 1 секунда
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvSlogan.startAnimation(fadeIn)
        binding.btnPrime.startAnimation(fadeIn)

        // Обработка нажатия на кнопку "Прайме!"
        binding.btnPrime.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Обработка кнопки "Выход" в левом верхнем углу
        binding.btnExit.setOnClickListener {
            // Полностью закрываем приложение
            finishAffinity()
        }
    }
}