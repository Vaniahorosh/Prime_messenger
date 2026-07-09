package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityRegisterBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
                return@setOnClickListener
            } else {
                binding.inputLayoutName.error = null
            }

            if (password.length < 8) {
                binding.inputLayoutPassword.error = "Пароль должен быть не менее 8 символов"
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

            Toast.makeText(this, "Аккаунт создан!", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finishAffinity()
        }
    }
}