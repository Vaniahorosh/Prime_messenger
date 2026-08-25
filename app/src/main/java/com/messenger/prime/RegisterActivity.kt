package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.messenger.prime.databinding.ActivityRegisterContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

class RegisterActivity : AppCompatActivity() {

    private var binding: ActivityRegisterContentBinding? = null
    private var userLogin: String = ""
    private var avatarUri: Uri? = null

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedUriString = result.data?.getStringExtra("EDITED_IMAGE_URI")
            if (editedUriString != null) {
                avatarUri = Uri.parse(editedUriString)
                binding?.ivSelectedAvatar?.setImageURI(avatarUri)
                binding?.tvAvatarHint?.visibility = View.GONE
                PrimeNotification.show(this, "Фото готово")
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, PhotoEditorActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URI", uri.toString())
            photoEditorLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

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
        
        setContentView(R.layout.activity_register)

        setupEdgeToEdge()
        userLogin = intent.getStringExtra("EXTRA_LOGIN") ?: ""

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { _ ->
                        val view = layoutInflater.inflate(R.layout.activity_register_content, null)
                        val b = ActivityRegisterContentBinding.bind(view)
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

                        val contentViews = listOf<View>(b.cvAvatar, b.inputLayoutName, b.inputLayoutPassword, b.tvPasswordHint, b.btnForward)
                        contentViews.forEach { it.alpha = 0f }
                        b.tvWelcome.alpha = 0f
                        b.tvWelcome.text = "Будем знакомы, $userLogin!"

                        startHeaderExpandAnimation(b, contentViews)

                        b.cvAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
                        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

                        b.etName.addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                b.inputLayoutName.error = null
                            }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })

                        b.etPassword.addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                b.inputLayoutPassword.error = null
                            }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })

                        b.btnForward.setOnClickListener {
                            val name = b.etName.text.toString().trim()
                            val password = b.etPassword.text.toString()

                            if (name.isEmpty()) {
                                b.inputLayoutName.error = "Как вас зовут?"
                                b.inputLayoutName.shake()
                                b.btnForward.shake()
                                return@setOnClickListener
                            }

                            val error = ValidationUtils.getValidationError(name, false)
                            if (error != null) {
                                b.inputLayoutName.error = error
                                b.inputLayoutName.shake()
                                b.btnForward.shake()
                                return@setOnClickListener
                            } else {
                                b.inputLayoutName.error = null
                            }

                            if (password.length < 8) {
                                b.inputLayoutPassword.error = "Пароль должен быть не менее 8 символов"
                                b.inputLayoutPassword.shake()
                                b.btnForward.shake()
                                return@setOnClickListener
                            } else {
                                b.inputLayoutPassword.error = null
                            }

                            val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                            sharedPreferences.edit().apply {
                                putString(userLogin, password)
                                putString("${userLogin}_name", name)
                                if (avatarUri != null) putString("${userLogin}_avatar", avatarUri.toString())
                                putBoolean("is_logged_in", true)
                                putString("current_user", userLogin)
                                apply()
                            }
                            startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finishAffinity()
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

    private fun startHeaderExpandAnimation(b: ActivityRegisterContentBinding, contentViews: List<View>) {
        val header = b.topHeader
        val startHeight = (65 * resources.displayMetrics.density).toInt()
        val endHeight = resources.displayMetrics.heightPixels
        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            val p = header.layoutParams
            p.height = valueAnimator.animatedValue as Int
            header.layoutParams = p
        }
        animator.duration = 500
        animator.startDelay = 180
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startWelcomeArcAnimation(b)
                fadeInContent(contentViews)
            }
        })
        animator.start()
    }

    private fun startWelcomeArcAnimation(b: ActivityRegisterContentBinding) {
        val title = b.tvWelcome
        val density = resources.displayMetrics.density
        val startY = -(140 * density)
        val startX = -(60 * density)
        val peakX = 20 * density
        title.translationX = startX
        title.translationY = startY
        title.alpha = 0f
        title.rotation = -8f
        title.visibility = View.VISIBLE

        val arcAnimator = ValueAnimator.ofFloat(0f, 1f)
        arcAnimator.duration = 550
        arcAnimator.interpolator = AccelerateDecelerateInterpolator()
        arcAnimator.addUpdateListener { animator ->
            val t = animator.animatedValue as Float
            val x = if (t < 0.6f) startX + (peakX - startX) * (t / 0.6f) else peakX + (0f - peakX) * ((t - 0.6f) / 0.4f)
            val y = startY * (1 - t) * (1 - t)
            title.translationX = x
            title.translationY = y
            title.alpha = (t / 0.4f).coerceIn(0f, 1f)
            title.rotation = -8f * (1 - t)
        }
        arcAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                title.translationX = 0f
                title.translationY = 0f
                title.rotation = 0f
                title.alpha = 1f
            }
        })
        arcAnimator.start()
    }

    private fun fadeInContent(contentViews: List<View>) {
        contentViews.forEach { view ->
            view.animate().alpha(1f).setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
