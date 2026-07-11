package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.TransitionManager
import com.messenger.prime.databinding.ActivityLoginBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordState = false // Флаг: вводим мы сейчас логин или уже пароль

    // Контент, который скрыт пока фон не соберётся до своей финальной высоты
    private val contentViews: List<View> by lazy {
        listOf(
            binding.tvTitle,
            binding.tvSubtitle,
            binding.inputLayoutLogin
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Делаем статус-бар прозрачным, чтобы цветная шапка уходила под него
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Иконки статус-бара белые, т.к. шапка темная
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Магия для клавиатуры и кнопки "Назад" (WindowInsets)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Спускаем кнопку "Назад" с учетом высоты статус-бара
            val backParams = binding.btnBack.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            backParams.topMargin = systemBarsInsets.top + (8 * resources.displayMetrics.density).toInt()
            binding.btnBack.layoutParams = backParams

            view.setPadding(0, 0, 0, imeInsets.bottom)
            windowInsets
        }

        // Контент прячем сразу — появится только после того, как фон соберётся до своей высоты
        contentViews.forEach { it.alpha = 0f }

        startHeaderCollapseAnimation()

        // Настраиваем интерактивный свайп как в Telegram
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT) // Оставляем направление слева направо
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

        // Если пользователь уже ввёл логин (сейчас видно поле пароля) и хочет его сменить —
        // клик по полю логина возвращает в режим редактирования логина.
        binding.etLogin.setOnClickListener {
            if (isPasswordState) {
                showLoginStep()
            }
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
                    showPasswordStep()
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

    /**
     * Анимация "сборки" синей плашки topHeader: сначала фон занимает весь экран,
     * затем плавно схлопывается до компактной высоты тулбара (56dp).
     * После того как фон собрался — проявляется контент (заголовок, подзаголовок, поле логина).
     */
    private fun startHeaderCollapseAnimation() {
        val header = binding.topHeader

        val endHeight = (88 * resources.displayMetrics.density).toInt()
        val startHeight = resources.displayMetrics.heightPixels

        val params = header.layoutParams
        params.height = startHeight
        header.layoutParams = params

        animateHeaderTo(startHeight, endHeight)
    }

    private fun animateHeaderTo(startHeight: Int, endHeight: Int) {
        val header = binding.topHeader
        val params = header.layoutParams

        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            params.height = valueAnimator.animatedValue as Int
            header.layoutParams = params
        }

        animator.duration = 500
        animator.startDelay = 150
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                fadeInContent()
            }
        })

        animator.start()
    }

    private fun fadeInContent() {
        contentViews.forEach { view ->
            view.translationY = -30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    /**
     * Показывает поле пароля и делает поле логина "зафиксированным":
     * текст остаётся виден, но не редактируется напрямую — только через клик,
     * который возвращает в режим ввода логина (см. showLoginStep).
     */
    private fun showPasswordStep() {
        TransitionManager.beginDelayedTransition(binding.root)
        binding.inputLayoutPassword.visibility = View.VISIBLE
        binding.etLogin.isFocusable = false
        binding.etLogin.isFocusableInTouchMode = false
        isPasswordState = true

        binding.etPassword.requestFocus()
        showKeyboard(binding.etPassword)
    }

    /**
     * Возвращает экран в режим редактирования логина: прячет поле пароля,
     * очищает его и снова делает поле логина редактируемым.
     */
    private fun showLoginStep() {
        TransitionManager.beginDelayedTransition(binding.root)
        binding.inputLayoutPassword.visibility = View.GONE
        binding.etPassword.text?.clear()
        binding.tvError.visibility = View.GONE
        binding.inputLayoutPassword.error = null

        binding.etLogin.isFocusable = true
        binding.etLogin.isFocusableInTouchMode = true
        binding.etLogin.requestFocus()
        showKeyboard(binding.etLogin)

        isPasswordState = false
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { focusedView ->
            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
            focusedView.clearFocus()
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Перехватываем все касания на уровне Activity: если тап пришёлся не по
     * текущему сфокусированному полю ввода — скрываем клавиатуру. Такой подход
     * надёжнее, чем OnClickListener на root, так как срабатывает даже в пустых
     * зонах экрана, где физически нет вьюхи-обработчика клика (ScrollView/wrap_content).
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val outRect = android.graphics.Rect()
                focused.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}