package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivitySettingsBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Растягиваем дизайн на весь экран (под статус-бар и нижнюю панель навигации)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // 2. Сдвигаем кнопку "Назад" вниз на высоту статус-бара, чтобы она не налезла на часы
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack) { view, windowInsets ->
            val topInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams

            // Берем высоту статус-бара + добавляем 16dp отступа
            val marginInPx = (16 * resources.displayMetrics.density).toInt()
            params.topMargin = topInsets + marginInPx
            view.layoutParams = params

            windowInsets
        }

        // Подключаем Slidr для закрытия экрана свайпом слева направо
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        // ==========================================
        // 1. ЗАГРУЗКА ДАННЫХ ИЗ SharedPreferences
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        // Достаем имя, пароль и аватарку именно для текущего пользователя
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь")
        val savedPassword = sharedPrefs.getString(currentUser, "")
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)

        // ==========================================
        // 2. УСТАНОВКА ДАННЫХ В ИНТЕРФЕЙС
        // ==========================================
        binding.tvUserName.text = savedName
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(savedPassword)

        if (savedAvatarUri != null) {
            binding.ivProfilePhoto.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
        }

        // ==========================================
        // 3. ОБРАБОТКА НАЖАТИЙ НА КНОПКИ
        // ==========================================

        // Кнопка "Назад"
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Кнопка "Выход"
        binding.btnLogout.setOnClickListener {
            // Сбрасываем флаг авторизации
            sharedPrefs.edit().putBoolean("is_logged_in", false).apply()

            // Открываем экран логина и убиваем историю экранов, чтобы нельзя было вернуться назад по кнопке "Back"
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}