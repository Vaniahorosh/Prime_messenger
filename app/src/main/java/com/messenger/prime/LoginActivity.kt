package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.messenger.prime.databinding.ActivityLoginContentBinding
import java.io.File
import java.io.FileOutputStream

class LoginActivity : AppCompatActivity() {

    private var binding: ActivityLoginContentBinding? = null
    private var isPasswordState = false
    private var isRegistrationMode = false
    private var avatarUri: Uri? = null

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val uriStr = uri.toString().lowercase()
            val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
            val isGif = uriStr.endsWith(".gif") || uriStr.contains("gif") || "image/gif".equals(mime, ignoreCase = true)

            if (isGif) {
                try {
                    val login = binding?.etLogin?.text?.toString()?.trim() ?: "temp"
                    val destFile = File(filesDir, "avatar_$login.gif")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    avatarUri = Uri.fromFile(destFile)
                } catch (_: Exception) {
                    avatarUri = uri
                }
                binding?.ivRegisterAvatar?.let { iv ->
                    Glide.with(this).asGif().load(avatarUri ?: uri).centerCrop().into(iv)
                }
                binding?.tvSelectPhoto?.visibility = View.GONE
                PrimeNotification.show(this, "GIF-аватарка выбрана")
            } else {
                val intent = Intent(this, PhotoEditorActivity::class.java)
                intent.putExtra("EXTRA_IMAGE_URI", uri.toString())
                photoEditorLauncher.launch(intent)
                PrimeTransitions.applyOpenTransition(this)
            }
        }
    }

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedUriStr = result.data?.getStringExtra("EDITED_IMAGE_URI") 
                ?: result.data?.getStringExtra("EXTRA_IMAGE_URI")
            if (editedUriStr != null) {
                avatarUri = Uri.parse(editedUriStr)
                binding?.ivRegisterAvatar?.let { iv ->
                    Glide.with(this).load(avatarUri).centerCrop().into(iv)
                }
                binding?.tvSelectPhoto?.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        
        UpdateChecker.checkForUpdates(this)
        
        setupEdgeToEdge()

        val sharedPreferences = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            PrimeTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackgroundState.onActivityResumed()
                    AnimatedBackground(darkTheme = darkTheme)
                    
                    AndroidView(
                        factory = { _ ->
                            val view = layoutInflater.inflate(R.layout.activity_login_content, null)
                            val b = ActivityLoginContentBinding.bind(view)
                            binding = b

                            ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                                val density = resources.displayMetrics.density
                                b.rootConstraint.setPadding(
                                    b.rootConstraint.paddingLeft,
                                    b.rootConstraint.paddingTop,
                                    b.rootConstraint.paddingRight,
                                    insets.bottom + (24 * density).toInt()
                                )
                                windowInsets
                            }

                            // Setup TextSwitcher
                            b.tsTitle.setFactory {
                                val textView = TextView(this@LoginActivity)
                                textView.textSize = 26f
                                textView.setTextColor(if (darkTheme) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.parseColor("#0F172A"))
                                textView.setTypeface(null, android.graphics.Typeface.BOLD)
                                textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                                textView
                            }
                            b.tsTitle.inAnimation = android.view.animation.AnimationUtils.loadAnimation(this@LoginActivity, android.R.anim.slide_in_left)
                            b.tsTitle.outAnimation = android.view.animation.AnimationUtils.loadAnimation(this@LoginActivity, android.R.anim.slide_out_right)
                            b.tsTitle.setText("Приветствуем!")

                            b.loginBlurCard.setupBlur(b.rootConstraint, 20f)

                            b.btnBack.setOnClickListener {
                                if (isRegistrationMode) {
                                    toggleMode(b)
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            // Каскадная плавная анимация появления элементов
                            val animatableViews = listOf(b.btnBack, b.tsTitle, b.flLoginAvatar, b.loginBlurCard)
                            
                            animatableViews.forEach { 
                                it.alpha = 0f 
                                if (it != b.btnBack) {
                                    it.translationY = 50f
                                }
                            }

                            b.btnBack.animate().alpha(1f).setDuration(400L).start()
                            b.tsTitle.animate().alpha(1f).translationY(0f).setDuration(500L).setStartDelay(100L).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                            b.flLoginAvatar.animate().alpha(1f).translationY(0f).setDuration(500L).setStartDelay(150L).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                            b.loginBlurCard.animate().alpha(1f).translationY(0f).setDuration(500L).setStartDelay(200L).setInterpolator(android.view.animation.DecelerateInterpolator()).start()

                            setupContrastColors(b, darkTheme)

                            b.etLogin.addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    val login = s?.toString()?.trim() ?: ""
                                    hideError(b)
                                    b.inputLayoutLogin.error = null
                                    
                                    if (isRegistrationMode) {
                                        if (login.isNotEmpty() && sharedPreferences.contains(login)) {
                                            showError(b, "Этот логин уже занят на устройстве")
                                        }
                                        return
                                    }
                                    
                                    if (login.isNotEmpty() && sharedPreferences.contains(login)) {
                                        // Found user
                                        val name = sharedPreferences.getString("${login}_name", login) ?: login
                                        b.tsTitle.setText("Это вы, $name?")
                                        b.btnToggleMode.text = "Нет это не я"
                                        
                                        b.llPasswordContainer.visibility = View.VISIBLE
                                        b.llPasswordContainer.translationY = -50f
                                        b.llPasswordContainer.alpha = 0f
                                        b.llPasswordContainer.animate().translationY(0f).alpha(1f).setDuration(300L)
                                            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                                            
                                        isPasswordState = true
                                        
                                        val savedAvatarUri = sharedPreferences.getString("${login}_avatar", null)
                                            ?: sharedPreferences.getString("${login}_avatarUri", null)
                                        if (!savedAvatarUri.isNullOrEmpty()) {
                                            loadAvatarIntoView(this@LoginActivity, savedAvatarUri, b.ivLoginAvatar, name)
                                        } else {
                                            Glide.with(this@LoginActivity).clear(b.ivLoginAvatar)
                                            b.ivLoginAvatar.setImageResource(R.drawable.ic_prime_logo_vector)
                                        }
                                    } else {
                                        // User not found
                                        b.tsTitle.setText("Приветствуем!")
                                        b.btnToggleMode.text = "Регистрация"
                                        
                                        if (b.llPasswordContainer.visibility == View.VISIBLE) {
                                            b.llPasswordContainer.animate().translationY(-30f).alpha(0f).setDuration(200L).withEndAction {
                                                b.llPasswordContainer.visibility = View.GONE
                                            }.start()
                                        }
                                        isPasswordState = false
                                        Glide.with(this@LoginActivity).clear(b.ivLoginAvatar)
                                        b.ivLoginAvatar.setImageResource(R.drawable.ic_prime_logo_vector)
                                    }
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            })

                            val textHideErrorWatcher = object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    hideError(b)
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            }
                            b.etName.addTextChangedListener(textHideErrorWatcher)
                            b.etPassword.addTextChangedListener(textHideErrorWatcher)

                            // Setup submit actions
                            val editorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                                    if (isRegistrationMode) {
                                        b.btnRegisterSubmit.performClick()
                                    } else if (isPasswordState) {
                                        submitLogin(b, sharedPreferences)
                                    }
                                    true
                                } else false
                            }

                            b.etPassword.setOnEditorActionListener(editorActionListener)
                            
                            b.inputLayoutLogin.setEndIconOnClickListener {
                                if (!isRegistrationMode) {
                                    val login = b.etLogin.text.toString().trim()
                                    if (login.isNotEmpty()) {
                                        if (sharedPreferences.contains(login)) {
                                            b.etPassword.requestFocus()
                                            showKeyboard(b.etPassword)
                                        } else {
                                            toggleMode(b)
                                        }
                                    }
                                }
                            }
                            
                            b.btnSubmitArrow.setOnClickListener {
                                if (!isRegistrationMode) submitLogin(b, sharedPreferences)
                            }

                            b.btnToggleMode.setOnClickListener {
                                toggleMode(b)
                            }

                            b.btnRegisterSubmit.setOnClickListener {
                                submitRegistration(b, sharedPreferences)
                            }
                            
                            b.flRegisterAvatar.setOnClickListener {
                                if (isRegistrationMode) {
                                    photoPickerLauncher.launch("image/*")
                                }
                            }

                            // Setup predefined avatars
                            val avatars = listOf(
                                b.ivAvatar1 to "android.resource://${packageName}/${R.drawable.avatar_1}",
                                b.ivAvatar2 to "android.resource://${packageName}/${R.drawable.avatar_2}",
                                b.ivAvatar3 to "android.resource://${packageName}/${R.drawable.avatar_3}",
                                b.ivAvatar4 to "android.resource://${packageName}/${R.drawable.avatar_4}",
                                b.ivAvatar5 to "android.resource://${packageName}/${R.drawable.avatar_5}"
                            )

                            avatars.forEach { (imageView, uriString) ->
                                imageView.setOnClickListener {
                                    if (isRegistrationMode) {
                                        avatarUri = android.net.Uri.parse(uriString)
                                        Glide.with(this@LoginActivity)
                                            .load(avatarUri)
                                            .centerCrop()
                                            .into(b.ivRegisterAvatar)
                                        b.tvSelectPhoto.visibility = View.GONE
                                        
                                        // Очищаем выделение со всех
                                        avatars.forEach { (iv, _) -> iv.strokeWidth = 2f }
                                        // Выделяем текущую (увеличиваем рамку или меняем цвет)
                                        imageView.strokeWidth = 6f
                                    }
                                }
                            }

                            view
                        },
                        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()
                    )

                    BlurView(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
                        blurRadius = 20.dp,
                        tint = Color(0x0DFFFFFF)
                    )
                }
            }
        }
    }

    private fun toggleMode(b: ActivityLoginContentBinding) {
        isRegistrationMode = !isRegistrationMode
        
        val transition = androidx.transition.AutoTransition()
        transition.duration = 400
        transition.interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
        TransitionManager.beginDelayedTransition(b.contentContainer, transition)
        
        if (isRegistrationMode) {
            b.tsTitle.setText("Давайте создадим профиль!")
            b.tvSubtitle.visibility = View.GONE
            
            b.flLoginAvatar.visibility = View.GONE
            b.flRegisterAvatar.visibility = View.VISIBLE
            
            b.inputLayoutName.visibility = View.VISIBLE
            b.inputLayoutName.alpha = 1f
            b.btnRegisterSubmit.visibility = View.VISIBLE
            b.btnRegisterSubmit.alpha = 1f
            b.clBottomAvatars.visibility = View.VISIBLE
            b.clBottomAvatars.alpha = 1f
            
            b.llPasswordContainer.visibility = View.VISIBLE
            b.llPasswordContainer.translationY = 0f
            b.llPasswordContainer.alpha = 1f
            b.btnSubmitArrow.visibility = View.GONE
            
            b.btnToggleMode.visibility = View.GONE
            
            // Clear avatar state for new registration
            avatarUri = null
            Glide.with(this).clear(b.ivRegisterAvatar)
            b.ivRegisterAvatar.setImageDrawable(null)
            b.tvSelectPhoto.visibility = View.VISIBLE
            b.tvSelectPhoto.alpha = 1f
            
            val login = b.etLogin.text.toString().trim()
            if (login.isNotEmpty() && getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).contains(login)) {
                 showError(b, "Этот логин уже занят на устройстве")
            } else {
                 hideError(b)
            }
        } else {
            b.tsTitle.setText("Приветствуем!")
            b.tvSubtitle.visibility = View.VISIBLE
            
            b.flRegisterAvatar.visibility = View.GONE
            b.flLoginAvatar.visibility = View.VISIBLE
            
            b.inputLayoutName.visibility = View.GONE
            b.inputLayoutName.alpha = 0f
            b.btnRegisterSubmit.visibility = View.GONE
            b.btnRegisterSubmit.alpha = 0f
            b.clBottomAvatars.visibility = View.GONE
            b.clBottomAvatars.alpha = 0f
            b.tvSelectPhoto.visibility = View.GONE
            b.tvSelectPhoto.alpha = 0f
            
            b.btnToggleMode.visibility = View.VISIBLE
            b.btnSubmitArrow.visibility = View.VISIBLE
            
            // Clear avatar state when exiting registration
            avatarUri = null
            Glide.with(this).clear(b.ivLoginAvatar)
            b.ivLoginAvatar.setImageResource(R.drawable.ic_prime_logo_vector)
            
            // Re-trigger login text watcher to restore UI state
            val currentText = b.etLogin.text
            b.etLogin.text = currentText
        }
    }

    private fun submitLogin(b: ActivityLoginContentBinding, sharedPreferences: android.content.SharedPreferences) {
        val login = b.etLogin.text.toString().trim()
        val password = b.etPassword.text.toString()

        val loginError = ValidationUtils.validateLogin(login, null)
        if (loginError != null) {
            showError(b, loginError)
            return
        }

        val passError = ValidationUtils.validatePassword(password)
        if (passError != null) {
            showError(b, passError)
            return
        }

        val savedPassword = sharedPreferences.getString(login, null)
        if (savedPassword == null) {
            showError(b, "Пользователь не найден")
            return
        }

        if (password == savedPassword) {
            hideError(b)
            val savedName = sharedPreferences.getString("${login}_name", login) ?: login
            val savedAvatar = sharedPreferences.getString("${login}_avatar", null) ?: sharedPreferences.getString("${login}_avatarUri", null)
            
            AvatarManager.syncActiveUserProfile(this, login, savedName, savedAvatar)

            LavaBackgroundState.onTransitionStart()
            startActivity(Intent(this, ChatListActivity::class.java))
            PrimeTransitions.applyOpenTransition(this)
            finishAffinity()
        } else {
            showError(b, "Неверный пароль")
        }
    }

    private fun submitRegistration(b: ActivityLoginContentBinding, sharedPreferences: android.content.SharedPreferences) {
        val login = b.etLogin.text.toString().trim()
        val name = b.etName.text.toString().trim()
        val password = b.etPassword.text.toString()

        val loginError = ValidationUtils.validateLogin(login, sharedPreferences)
        if (loginError != null) {
            showError(b, loginError)
            return
        }

        val nameError = ValidationUtils.validateName(name)
        if (nameError != null) {
            showError(b, nameError)
            return
        }

        val passError = ValidationUtils.validatePassword(password)
        if (passError != null) {
            showError(b, passError)
            return
        }

        hideError(b)

        var finalAvatarStr: String? = null
        if (avatarUri != null) {
            try {
                val timestamp = System.currentTimeMillis()
                val isGif = avatarUri.toString().lowercase().contains(".gif")
                val ext = if (isGif) ".gif" else ".jpg"
                val permFile = File(filesDir, "my_profile_avatar_${timestamp}$ext")
                
                contentResolver.openInputStream(avatarUri!!)?.use { input ->
                    FileOutputStream(permFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (permFile.exists() && permFile.length() > 0) {
                    finalAvatarStr = Uri.fromFile(permFile).toString()
                } else {
                    finalAvatarStr = avatarUri.toString()
                }
            } catch (_: Exception) {
                finalAvatarStr = avatarUri.toString()
            }
        }

        sharedPreferences.edit().apply {
            putString(login, password)
            apply()
        }

        AvatarManager.syncActiveUserProfile(this, login, name, finalAvatarStr)

        LavaBackgroundState.onTransitionStart()
        startActivity(Intent(this, ChatListActivity::class.java))
        PrimeTransitions.applyOpenTransition(this)
        finishAffinity()
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100L)
                }
            }
        } catch (_: Exception) {}
    }

    private fun showError(b: ActivityLoginContentBinding, message: String) {
        b.tvError.text = message
        if (b.tvError.visibility != View.VISIBLE) {
            b.tvError.visibility = View.VISIBLE
            b.tvError.alpha = 0f
            b.tvError.scaleX = 0.85f
            b.tvError.scaleY = 0.85f
            b.tvError.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250L)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        }
        b.tvError.shake()
        triggerVibration()
    }

    private fun hideError(b: ActivityLoginContentBinding) {
        if (b.tvError.visibility == View.VISIBLE) {
            b.tvError.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(150L)
                .withEndAction {
                    b.tvError.visibility = View.GONE
                }
                .start()
        }
    }

    private fun setupContrastColors(b: ActivityLoginContentBinding, darkTheme: Boolean) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val titleColor = if (darkTheme) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.parseColor("#0F172A")
        val subtitleColor = if (darkTheme) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B")
        val iconTint = if (darkTheme) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E293B")
        val hintColorList = android.content.res.ColorStateList.valueOf(if (darkTheme) android.graphics.Color.parseColor("#B0FFFFFF") else android.graphics.Color.parseColor("#64748B"))
        val boxBgColor = if (darkTheme) android.graphics.Color.parseColor("#1AFFFFFF") else android.graphics.Color.parseColor("#10000000")
        val strokeColorList = android.content.res.ColorStateList.valueOf(if (darkTheme) android.graphics.Color.WHITE else accentColor)

        b.tvTitle.setTextColor(titleColor)
        b.tvSubtitle.setTextColor(subtitleColor)
        b.btnBack.setColorFilter(iconTint)
        
        b.etLogin.setTextColor(titleColor)
        b.etName.setTextColor(titleColor)
        b.etPassword.setTextColor(titleColor)
        
        b.inputLayoutLogin.defaultHintTextColor = hintColorList
        b.inputLayoutName.defaultHintTextColor = hintColorList
        b.inputLayoutPassword.defaultHintTextColor = hintColorList

        b.inputLayoutLogin.boxBackgroundColor = boxBgColor
        b.inputLayoutName.boxBackgroundColor = boxBgColor
        b.inputLayoutPassword.boxBackgroundColor = boxBgColor

        b.inputLayoutLogin.setBoxStrokeColorStateList(strokeColorList)
        b.inputLayoutName.setBoxStrokeColorStateList(strokeColorList)
        b.inputLayoutPassword.setBoxStrokeColorStateList(strokeColorList)

        b.inputLayoutPassword.setEndIconTintList(android.content.res.ColorStateList.valueOf(iconTint))

        b.btnToggleMode.setTextColor(titleColor)
        b.btnToggleMode.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (darkTheme) android.graphics.Color.parseColor("#30FFFFFF") else android.graphics.Color.parseColor("#20000000")
        )
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
        imm.showSoftInput(view, 0)
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
        binding?.root?.let {
            ColorAccentManager.tintViewTree(it, ColorAccentManager.getCurrentAccentColor(this))
            it.alpha = 1f
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}
