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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
        
        setContentView(R.layout.activity_login)

        setupEdgeToEdge()

        val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
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
                        contentViews.forEach { it.alpha = 0f }

                        startHeaderCollapseAnimation(b, contentViews)

                        b.btnBack.setOnClickListener {
                            onBackPressedDispatcher.onBackPressed()
                        }

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
                                    val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                                    intent.putExtra("EXTRA_LOGIN", login)
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
                    modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                )

                // Размытие для системной панели навигации
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

        // Возвращаем Slidr для всех версий
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)
    }

    private fun startHeaderCollapseAnimation(b: ActivityLoginContentBinding, contentViews: List<View>) {
        val header = b.topHeader
        val endHeight = (88 * resources.displayMetrics.density).toInt()
        val startHeight = resources.displayMetrics.heightPixels
        
        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            val p = header.layoutParams
            p.height = valueAnimator.animatedValue as Int
            header.layoutParams = p
        }
        animator.duration = 500
        animator.startDelay = 150
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                fadeInContent(contentViews)
            }
        })
        animator.start()
    }

    private fun fadeInContent(contentViews: List<View>) {
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

    private fun showPasswordStep(b: ActivityLoginContentBinding) {
        val root = b.topHeader.parent as ViewGroup
        TransitionManager.beginDelayedTransition(root)
        b.inputLayoutPassword.visibility = View.VISIBLE
        b.etLogin.isFocusable = false
        b.etLogin.isFocusableInTouchMode = false
        isPasswordState = true
        b.etPassword.requestFocus()
        showKeyboard(b.etPassword)
    }

    private fun showLoginStep(b: ActivityLoginContentBinding) {
        val root = b.topHeader.parent as ViewGroup
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
