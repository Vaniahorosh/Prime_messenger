package com.messenger.prime

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.messenger.prime.databinding.ActivityRegisterContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max


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
            val uriStr = uri.toString().lowercase()
            val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
            val isGif = uriStr.endsWith(".gif") || uriStr.contains("gif") || "image/gif".equals(mime, ignoreCase = true)

            if (isGif) {
                try {
                    val destFile = File(filesDir, "avatar_$userLogin.gif")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    avatarUri = Uri.fromFile(destFile)
                } catch (_: Exception) {
                    avatarUri = uri
                }
                binding?.ivSelectedAvatar?.let { iv ->
                    Glide.with(this)
                        .asGif()
                        .load(uri)
                        .into(iv)
                }
                binding?.tvAvatarHint?.visibility = View.GONE
                PrimeNotification.show(this, "GIF-аватарка выбрана")
            } else {
                val intent = Intent(this, PhotoEditorActivity::class.java)
                intent.putExtra("EXTRA_IMAGE_URI", uri.toString())
                photoEditorLauncher.launch(intent)
                PrimeTransitions.applyOpenTransition(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        PrimeTransitions.setupActivityTransitions(this)
        
        setupEdgeToEdge()
        userLogin = intent.getStringExtra("EXTRA_LOGIN") ?: ""

        setContent {
            val darkTheme = isSystemInDarkTheme()
            PrimeTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackgroundState.onActivityResumed()
                    AnimatedBackground(
                        darkTheme = darkTheme
                    )

                    AndroidView(
                        factory = { _ ->
                            val view = layoutInflater.inflate(R.layout.activity_register_content, null)
                            val b = ActivityRegisterContentBinding.bind(view)
                            binding = b

                            ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                                val density = resources.displayMetrics.density

                                (b.btnBack.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                                    lp.topMargin = insets.top + (12 * density).toInt()
                                    b.btnBack.layoutParams = lp
                                }
                                b.rootConstraint.setPadding(
                                    b.rootConstraint.paddingLeft,
                                    b.rootConstraint.paddingTop,
                                    b.rootConstraint.paddingRight,
                                    insets.bottom + (24 * density).toInt()
                                )
                                windowInsets
                            }

                            val contentViews = listOf(b.cvAvatar, b.inputLayoutName, b.inputLayoutPassword, b.tvPasswordHint, b.btnForward)
                            contentViews.forEach { it.alpha = 1f }
                            b.tvWelcome.alpha = 1f
                            b.tvWelcome.text = "Будем знакомы, $userLogin!"

                            b.cvAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
                            b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

                            // Apply contrast colors to XML elements
                            setupContrastColors(b, darkTheme)

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

                            b.etPassword.setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE ||
                                    actionId == EditorInfo.IME_ACTION_GO) {
                                    b.btnForward.performClick()
                                    true
                                } else false
                            }

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

                                val sharedPreferences = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                                sharedPreferences.edit().apply {
                                    putString(userLogin, password)
                                    putString("${userLogin}_name", name)
                                    if (avatarUri != null) putString("${userLogin}_avatar", avatarUri.toString())
                                    putBoolean("is_logged_in", true)
                                    putString("current_user", userLogin)
                                    apply()
                                }
                                LavaBackgroundState.onTransitionStart()
                                startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java))
                                PrimeTransitions.applyOpenTransition(this@RegisterActivity)
                                finishAffinity()
                            }
                            
                            view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .imePadding()
                    )

                    // Blur for navigation bar
                    BlurView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
                        blurRadius = 20.dp,
                        tint = Color(0x0DFFFFFF)
                    )
                }
            }
        }

        PrimeTransitions.attachSlidr(this)
    }

    private fun setupContrastColors(b: ActivityRegisterContentBinding, darkTheme: Boolean) {
        val welcomeColor = if (darkTheme) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.WHITE
        b.tvWelcome.setTextColor(welcomeColor)
        
        val inputBg = if (darkTheme) android.graphics.Color.argb(153, 15, 23, 42) else android.graphics.Color.argb(102, 21, 75, 135)
        b.inputLayoutName.boxBackgroundColor = inputBg
        b.inputLayoutPassword.boxBackgroundColor = inputBg
    }

    override fun onResume() {
        super.onResume()
        LavaBackgroundState.onActivityResumed()
        binding?.root?.let {
            ColorAccentManager.tintViewTree(it, ColorAccentManager.getCurrentAccentColor(this))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }
}
