package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.TransitionManager
import com.messenger.prime.databinding.ActivityLoginBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordState = false // Флаг: вводим мы сейчас логин или уже пароль

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Делаем статус-бар синим, а иконки светлыми (false)
        setDynamicStatusBar(R.color.prime_background_blue, false)

        // Настраиваем интерактивный свайп как в Telegram
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT) // Оставляем направление слева направо
            // УДАЛЯЕМ ИЛИ КОММЕНТИРУЕМ .edge(true) и .edgeSize(...)
            .build()

        Slidr.attach(this, slidrConfig) // Прикрепляем магию к нашему экрану

        // Инициализируем локальную "базу данных"
        val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        // Обработка системной кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })

        // Обработка нашей кнопки-стрелочки "Назад"
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Обработка кнопки "Вперед"
        binding.btnForward.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()

            if (login.isEmpty()) {
                binding.inputLayoutLogin.error = "Введите логин"
                return@setOnClickListener
            }
            binding.inputLayoutLogin.error = null

            if (!isPasswordState) {
                // ШАГ 1: ПРОВЕРКА ЛОГИНА
                if (sharedPreferences.contains(login)) {
                    // Пользователь есть в базе - показываем пароль
                    TransitionManager.beginDelayedTransition(binding.root)
                    binding.inputLayoutPassword.visibility = View.VISIBLE
                    binding.etLogin.isEnabled = false // Блокируем логин
                    isPasswordState = true
                } else {
                    // Пользователя нет - идем на регистрацию
                    val intent = Intent(this, RegisterActivity::class.java)
                    intent.putExtra("EXTRA_LOGIN", login)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            } else {
                // ШАГ 2: ПРОВЕРКА ПАРОЛЯ
                val password = binding.etPassword.text.toString()
                val savedPassword = sharedPreferences.getString(login, "")

                if (password == savedPassword) {
                    // Успешный вход!
                    binding.tvError.visibility = View.GONE

                    // СОХРАНЯЕМ СТАТУС АВТОРИЗАЦИИ
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("is_logged_in", true)
                    editor.putString("current_user", login) // Заодно запомним, кто именно вошел
                    editor.apply()

                    startActivity(Intent(this@LoginActivity, ChatListActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finishAffinity()
                } else {
                    // Ошибка пароля
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }
}