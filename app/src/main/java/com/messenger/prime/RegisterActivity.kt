package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityRegisterBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var userLogin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Делаем статус-бар синим, а иконки светлыми (false)
        setDynamicStatusBar(R.color.prime_background_blue, false)

        // Подключаем магию свайпа как в Telegram для экрана регистрации
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT) // Оставляем направление слева направо
            // УДАЛЯЕМ ИЛИ КОММЕНТИРУЕМ .edge(true) и .edgeSize(...)
            .build()
        Slidr.attach(this, slidrConfig)

        // Получаем логин с предыдущего экрана
        userLogin = intent.getStringExtra("EXTRA_LOGIN") ?: ""

        binding.tvWelcome.text = "Будем знакомы, $userLogin!"

        // Обработка системного жеста "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })

        // Обработка кнопки "Назад" (стрелочки)
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnForward.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (name.isEmpty()) {
                binding.inputLayoutName.error = "Как вас зовут?"
                return@setOnClickListener
            } else {
                binding.inputLayoutName.error = null
            }

            // Проверка на длину пароля (от 8 символов)
            if (password.length < 8) {
                binding.inputLayoutPassword.error = "Пароль должен быть не менее 8 символов"
                return@setOnClickListener
            } else {
                binding.inputLayoutPassword.error = null
            }

            // Сохраняем пользователя в локальную "базу данных"
            val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(userLogin, password)
            editor.putString("${userLogin}_name", name)

            // ДОБАВЛЯЕМ ЭТИ ДВЕ СТРОЧКИ:
            editor.putBoolean("is_logged_in", true)
            editor.putString("current_user", userLogin)

            editor.apply()

            Toast.makeText(this, "Аккаунт создан!", Toast.LENGTH_SHORT).show()

            // Переход в список чатов
            startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java))

            // ДОБАВЛЯЕМ ЭТУ СТРОЧКУ ДЛЯ АНИМАЦИИ:
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

            finishAffinity()
        }
    }
}