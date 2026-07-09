package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

        // ==========================================
        // 1. ДЕЛАЕМ СТАТУС-БАР ПРОЗРАЧНЫМ
        // ==========================================
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Настраиваем кнопку назад внутри родного Тулбара
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // ==========================================
        // 2. АНИМАЦИЯ ИМЕНИ И ЦВЕТА СТАТУС-БАРА
        // ==========================================
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@addOnOffsetChangedListener

            // Вычисляем процент сжатия (от 0.0 до 1.0)
            val percentage = Math.abs(verticalOffset).toFloat() / totalScrollRange.toFloat()

            // Плавное появление маленького текста в тулбаре (только в самом конце)
            binding.collapsedTitleLayout.alpha = if (percentage > 0.8f) (percentage - 0.8f) / 0.2f else 0f

            // Плавное исчезновение большого текста на фотке (исчезает быстро при начале скролла)
            binding.expandedTitleLayout.alpha = 1f - (percentage * 2f).coerceAtMost(1f)

            // Перекрашиваем статус-бар, когда шапка полностью свернулась
            if (Math.abs(verticalOffset) >= totalScrollRange - 10) {
                window.statusBarColor = ContextCompat.getColor(this, R.color.prime_background_blue)
            } else {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        // ==========================================
        // 3. ПОДКЛЮЧЕНИЕ СВАЙПА (SLIDR)
        // ==========================================
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        // ==========================================
        // 4. ЗАГРУЗКА И УСТАНОВКА ДАННЫХ
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь")
        val savedPassword = sharedPrefs.getString(currentUser, "")
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)

        binding.tvUserNameExpanded.text = savedName
        binding.tvUserNameCollapsed.text = savedName
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(savedPassword)

        if (savedAvatarUri != null) {
            binding.ivProfilePhoto.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
        }

        // ==========================================
        // 5. КНОПКА ВЫХОДА
        // ==========================================
        binding.btnLogout.setOnClickListener {
            sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}