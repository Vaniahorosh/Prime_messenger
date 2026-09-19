package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.TransitionManager
import com.messenger.prime.databinding.ActivityLoginContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

class LoginActivity : AppCompatActivity() {

    private var binding: ActivityLoginContentBinding? = null
    private var isPasswordState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            // Forward transition to Register (if happens)
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in_slow,
                R.anim.stay_slow
            )
            // Back transition to Hi
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
        
        setupEdgeToEdge()

        val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        setContent {
            val hazeState = remember { HazeState() }
            val darkTheme = isSystemInDarkTheme()
            PrimeTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                    LavaBackgroundState.onActivityResumed()
                    AnimatedBackground(
                        darkTheme = darkTheme
                    )
                    
                    AndroidView(
                        factory = { _ ->
                            val view = layoutInflater.inflate(R.layout.activity_login_content, null)
                            val b = ActivityLoginContentBinding.bind(view)
                            binding = b

                            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                                val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                                val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

                                val backParams = b.btnBack.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                                backParams.topMargin = systemBarsInsets.top + (8 * resources.displayMetrics.density).toInt()
                                b.btnBack.layoutParams = backParams

                                v.setPadding(0, 0, 0, imeInsets.bottom)
                                windowInsets
                            }

                            val contentViews = listOf<View>(b.tvTitle, b.tvSubtitle, b.inputLayoutLogin)
                            contentViews.forEach { it.alpha = 1f }

                            b.btnBack.setOnClickListener {
                                onBackPressedDispatcher.onBackPressed()
                            }

                            // Apply contrast colors to XML elements
                            setupContrastColors(b, darkTheme)

                            b.etLogin.setOnClickListener {
                                if (isPasswordState) {
                                    showLoginStep(b)
                                }
                            }

                            b.btnForward.setOnClickListener {
                                val login = b.etLogin.text.toString().trim()
                                if (login.isEmpty()) {
                                    b.inputLayoutLogin.error = "Введите логин"
                                    b.inputLayoutLogin.shake()
                                    b.btnForward.shake()
                                    return@setOnClickListener
                                }

                                val error = ValidationUtils.getValidationError(login, true)
                                if (error != null) {
                                    b.inputLayoutLogin.error = error
                                    b.inputLayoutLogin.shake()
                                    b.btnForward.shake()
                                    return@setOnClickListener
                                }
                                b.inputLayoutLogin.error = null

                                if (!isPasswordState) {
                                    if (sharedPreferences.contains(login)) {
                                        showPasswordStep(b)
                                    } else {
                                        LavaBackgroundState.onTransitionStart()
                                        
                                        // Fade out content before navigation
                                        b.root.animate()
                                            .alpha(0f)
                                            .setDuration(600L)
                                            .start()

                                        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                                        intent.putExtra("EXTRA_LOGIN", login)
                                        startActivity(intent)
                                        overridePendingTransition(R.anim.fade_in_slow, R.anim.stay_slow)
                                    }
                                } else {
                                    val password = b.etPassword.text.toString()
                                    val savedPassword = sharedPreferences.getString(login, "")
                                    if (password == savedPassword) {
                                        b.tvError.visibility = View.GONE
                                        sharedPreferences.edit().apply {
                                            putBoolean("is_logged_in", true)
                                            putString("current_user", login)
                                            apply()
                                        }
                                        startActivity(Intent(this@LoginActivity, ChatListActivity::class.java))
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                        finishAffinity()
                                    } else {
                                        b.tvError.visibility = View.VISIBLE
                                        b.inputLayoutPassword.shake()
                                        b.btnForward.shake()
                                    }
                                }
                            }
                            
                            view
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Blur for navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
                            .hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 20.dp,
                                    noiseFactor = 0f,
                                    tint = dev.chrisbanes.haze.HazeTint(Color(0x0DFFFFFF))
                                )
                            )
                    )
                }
            }
        }

        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)
    }

    private fun setupContrastColors(b: ActivityLoginContentBinding, darkTheme: Boolean) {
        val titleColor = if (darkTheme) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.WHITE
        val subtitleColor = if (darkTheme) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.argb(191, 255, 255, 255)
        
        b.tvTitle.setTextColor(titleColor)
        b.tvSubtitle.setTextColor(subtitleColor)
        
        // Input layouts background and stroke
        val inputBg = if (darkTheme) android.graphics.Color.argb(153, 15, 23, 42) else android.graphics.Color.argb(102, 21, 75, 135)
        b.inputLayoutLogin.boxBackgroundColor = inputBg
        b.inputLayoutPassword.boxBackgroundColor = inputBg
    }

    private fun showPasswordStep(b: ActivityLoginContentBinding) {
        val root = b.btnBack.parent as ViewGroup
        TransitionManager.beginDelayedTransition(root)
        b.inputLayoutPassword.visibility = View.VISIBLE
        b.etLogin.isFocusable = false
        b.etLogin.isFocusableInTouchMode = false
        isPasswordState = true
        b.etPassword.requestFocus()
        showKeyboard(b.etPassword)
    }

    private fun showLoginStep(b: ActivityLoginContentBinding) {
        val root = b.btnBack.parent as ViewGroup
        TransitionManager.beginDelayedTransition(root)
        b.inputLayoutPassword.visibility = View.GONE
        b.etPassword.text?.clear()
        b.tvError.visibility = View.GONE
        b.inputLayoutPassword.error = null
        b.etLogin.isFocusable = true
        b.etLogin.isFocusableInTouchMode = true
        b.etLogin.requestFocus()
        showKeyboard(b.etLogin)
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

    override fun onResume() {
        super.onResume()
        LavaBackgroundState.onActivityResumed()
        // Восстанавливаем видимость контента (после fadeOut при переходе вперед)
        binding?.root?.alpha = 1f
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
