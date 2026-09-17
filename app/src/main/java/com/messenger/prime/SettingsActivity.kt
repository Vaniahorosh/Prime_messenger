package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.messenger.prime.databinding.ActivitySettingsContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

class SettingsActivity : AppCompatActivity() {

    private var binding: ActivitySettingsContentBinding? = null

    private var isHeaderExpanded = false
    private var isHeaderMoving = false
    private var isAnimating = false
    private var pullStartY = 0f
    private val PULL_THRESHOLD = 250f
    private var currentAnimator: ValueAnimator? = null
    private var isVibrated = false
    private var isClosing = false
    private val isPhotoMenuMode = mutableStateOf(false)
    private val wobbleAnimators = mutableListOf<Animator>()

    private val isThemeDialogVisible = mutableStateOf(false)

    // Состояния для динамических кнопок в шапке
    private val backLabelState = mutableStateOf("Назад")
    private val backIconState = mutableIntStateOf(R.drawable.ic_arrow_back)
    
    private val logoutLabelState = mutableStateOf("Выход")
    private val logoutIconState = mutableIntStateOf(R.drawable.ic_exit_to_app)
    private val logoutColorState = mutableStateOf(Color(0xFFEF5350))
    
    private val extraSettingsLabelState = mutableStateOf("Настройки")
    private val extraSettingsIconState = mutableIntStateOf(R.drawable.ic_settings)
    private val extraSettingsColorState = mutableStateOf(Color.White)

    private val backLabelAlpha = mutableFloatStateOf(1f)
    private val backLabelTranslationX = mutableFloatStateOf(0f)

    private var currentAvatarUri: String? = null

    private var currentNameInDB: String = ""
    private var currentLoginInDB: String = ""
    private var currentPassInDB: String = ""

    private val avatarUriState = mutableStateOf<String?>(null)
    private var profileImageView: ImageView? = null

    private val photoViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("DELETED", false) == true) {
                handlePhotoDeletionWithUndo(currentAvatarUri)
            } else {
                val newUri = data?.getStringExtra("NEW_URI")
                if (newUri != null) {
                    currentAvatarUri = newUri
                    val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                    sharedPrefs.edit().putString("${currentUser}_avatar", newUri).apply()
                    applyAvatarState(newUri)
                }
            }
        }
    }

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedUriString = result.data?.getStringExtra("EDITED_IMAGE_URI")
            if (editedUriString != null) {
                currentAvatarUri = editedUriString
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
                applyAvatarState(currentAvatarUri)
                PrimeNotification.show(this, "Фото готово")
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, PhotoEditorActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URI", it.toString())
            photoEditorLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isPhotoMenuMode.value) {
                togglePhotoMenuMode(false)
            } else {
                finish()
            }
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
        
        setContentView(R.layout.activity_settings)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setupEdgeToEdge(isDarkIcons = true)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser
        currentPassInDB = sharedPrefs.getString(currentUser, "") ?: ""
        
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        currentNameInDB = savedName
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val hazeState = remember { HazeState() }
            val darkTheme = isSystemInDarkTheme()
            val isThemeVisible = isThemeDialogVisible.value
            
            PrimeTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackgroundState.onActivityResumed()
                    
                    AndroidView(
                        factory = { _ ->
                            val view = layoutInflater.inflate(R.layout.activity_settings_content, null)
                            val b = ActivitySettingsContentBinding.bind(view)
                            binding = b

                            // Сброс смещений при инициализации
                            b.layoutSettingsBodyContainer.translationY = 0f

                            b.tvUserNameStatic.text = savedName
                            b.etSettingsName.setText(savedName)
                            b.etSettingsLogin.setText(currentUser)
                            b.etSettingsPassword.setText(currentPassInDB)
                            b.tvAccountHeaderSummary.text = savedName

                            val isExpanded = sharedPrefs.getBoolean("settings_account_expanded", false)
                            b.layoutAccountCollapsible.visibility = if (isExpanded) View.VISIBLE else View.GONE
                            b.ivAccountArrow.rotation = if (isExpanded) -90f else 90f

                            setupComposePhoto(b)
                            setupListeners(b)
                            applyAvatarState(savedAvatarUri)

                            // Изоляция фонов
                            val bgColorInt = if (darkTheme) {
                                android.graphics.Color.parseColor("#1E293B")
                            } else {
                                android.graphics.Color.parseColor("#F1F5F9")
                            }
                            b.nestedScrollView.setBackgroundColor(bgColorInt)
                            b.headerStaticBlock.clipChildren = true

                            ViewCompat.setOnApplyWindowInsetsListener(b.headerStaticBlock) { _, insets ->
                                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                                val density = resources.displayMetrics.density
                                val extraPadding = (8 * density).toInt()

                                b.layoutWithPhoto.updatePadding(top = statusBarInset + extraPadding)
                                b.layoutNoPhoto.updatePadding(top = statusBarInset + extraPadding)
                                insets
                            }

                            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                            windowInsetsController.isAppearanceLightStatusBars = !darkTheme

                            // Инициализация кнопок с эффектом Haze
                            setupHazeButtons(b, hazeState, darkTheme)
                            
                            // Установка Haze для внутренних вью (если они есть в разметке)
                            b.root.findViewById<ComposeView>(R.id.hazeView)?.setContent {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeEffect(
                                            state = hazeState,
                                            style = HazeStyle(
                                                tint = dev.chrisbanes.haze.HazeTint(Color(0xFF154B87).copy(alpha = 0.6f)),
                                                blurRadius = 24.dp,
                                                noiseFactor = 0.05f
                                            )
                                        )
                                )
                            }

                            b.composeHeaderBackground.apply {
                                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                                setContent {
                                    PrimeTheme(darkTheme = darkTheme) {
                                        AnimatedBackground(
                                            darkTheme = darkTheme,
                                            ignoreSettingsToggle = false, // Respect toggle here
                                            modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                        )
                                    }
                                }
                            }
                            
                            b.headerStaticBlock.translationZ = 4f
                            b.photoCard.translationZ = 8f

                            view
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Кастомное окно выбора темы с эффектом Haze
                    if (isThemeVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { isThemeDialogVisible.value = false },
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                hazeState = hazeState,
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth()
                                    .clickable(enabled = false) { } 
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Тема оформления",
                                        color = Color.White,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    val currentTheme = sharedPrefs.getString("app_theme", "system")
                                    
                                    ThemeOptionItem("Системная (Рекомендуется)", "system", currentTheme == "system") {
                                        applyThemeChange("system")
                                    }
                                    ThemeOptionItem("Светлая", "light", currentTheme == "light") {
                                        applyThemeChange("light")
                                    }
                                    ThemeOptionItem("Темная", "dark", currentTheme == "dark") {
                                        applyThemeChange("dark")
                                    }
                                }
                            }
                        }
                    }

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

        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private fun setupComposePhoto(b: ActivitySettingsContentBinding) {
        b.composePhotoCard.setContent {
            val avatarUri = avatarUriState.value
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setOnClickListener {
                                if (!isHeaderExpanded && currentAvatarUri != null) togglePhotoMenuMode(true)
                                else if (currentAvatarUri != null) openFullPhoto()
                                else pickImage.launch("image/*")
                            }
                            profileImageView = this
                        }
                    },
                    update = { view ->
                        if (avatarUri != null) view.setImageURI(Uri.parse(avatarUri))
                        else view.setImageResource(R.drawable.ic_person)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun setupHazeButtons(b: ActivitySettingsContentBinding, hazeState: HazeState, darkTheme: Boolean) {
        b.composeBackWP.setContent {
            HazeIconButton(
                hazeState = hazeState,
                buttonState = isPhotoMenuMode.value,
                iconRes = backIconState.value,
                label = backLabelState.value,
                darkTheme = darkTheme,
                isShaking = isPhotoMenuMode.value,
                labelAlpha = backLabelAlpha.floatValue,
                labelTranslationX = backLabelTranslationX.floatValue
            ) { onBackPressedDispatcher.onBackPressed() }
        }

        b.composeChangePhoto.setContent {
            HazeIconButton(
                hazeState = hazeState,
                buttonState = isPhotoMenuMode.value,
                iconRes = R.drawable.ic_add,
                label = "Фото",
                darkTheme = darkTheme,
                isShaking = isPhotoMenuMode.value
            ) { pickImage.launch("image/*") }
        }

        b.composeLogout.setContent {
            HazeIconButton(
                hazeState = hazeState,
                buttonState = isPhotoMenuMode.value,
                iconRes = logoutIconState.value,
                label = logoutLabelState.value,
                iconTint = logoutColorState.value,
                labelColor = logoutColorState.value,
                darkTheme = darkTheme,
                isShaking = isPhotoMenuMode.value
            ) { 
                if (isPhotoMenuMode.value) openFullPhoto()
                else showLogoutDialog() 
            }
        }

        b.composeExtraSettings.setContent {
            HazeIconButton(
                hazeState = hazeState,
                buttonState = isPhotoMenuMode.value,
                iconRes = extraSettingsIconState.value,
                label = extraSettingsLabelState.value,
                iconTint = extraSettingsColorState.value,
                labelColor = extraSettingsColorState.value,
                darkTheme = darkTheme,
                isShaking = isPhotoMenuMode.value
            ) { 
                if (isPhotoMenuMode.value) {
                    handlePhotoDeletionWithUndo(currentAvatarUri)
                    togglePhotoMenuMode(false)
                } else {
                    b.nestedScrollView.smoothScrollTo(0, 1000)
                }
            }
        }
    }

    @Composable
    private fun HazeIconButton(
        hazeState: HazeState,
        buttonState: Boolean,
        iconRes: Int,
        label: String,
        iconTint: Color = Color.White,
        labelColor: Color = Color.White,
        darkTheme: Boolean,
        isShaking: Boolean = false,
        labelAlpha: Float = 1f,
        labelTranslationX: Float = 0f,
        onClick: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotation"
        )
        
        val translateX by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "translateX"
        )

        AnimatedContent(
            targetState = buttonState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.7f, animationSpec = tween(400)))
                    .togetherWith(fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.7f, animationSpec = tween(400)))
            },
            label = "buttonFlip"
        ) { isMenu ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(110.dp)
                    .graphicsLayer {
                        // Используем isMenu для фиксации состояния анимации
                        val unused = isMenu
                        if (isShaking) {
                            rotationZ = rotation
                            translationX = translateX
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 20.dp,
                                noiseFactor = 0.05f,
                                tint = dev.chrisbanes.haze.HazeTint(Color.White.copy(alpha = 0.1f))
                            )
                        )
                        .border(
                            width = if (darkTheme) 1.dp else 0.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = iconRes),
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                androidx.compose.material3.Text(
                    text = label,
                    color = labelColor.copy(alpha = 0.8f * labelAlpha),
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .graphicsLayer {
                            translationX = labelTranslationX
                        }
                )
            }
        }
    }

    private fun setupListeners(b: ActivitySettingsContentBinding) {
        b.btnBackNP.setOnClickListener { finish() }
        
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        
        b.btnChangePhotoNP.setOnClickListener { pickImage.launch("image/*") }

        b.btnExtraSettingsNP.setOnClickListener {
            b.nestedScrollView.smoothScrollTo(0, 1000)
        }

        setupInlineAccountEditing(b)
        setupAccountCollapsible(b)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        b.switchAnimations.isChecked = sharedPrefs.getBoolean("settings_animations", true)
        b.switchLavaBg.isChecked = sharedPrefs.getBoolean("settings_lava_bg", true)
        b.switchBlocked.isChecked = sharedPrefs.getBoolean("settings_show_blocked", false)
        b.switchSearch.isChecked = sharedPrefs.getBoolean("settings_hide_search", false)

        val theme = sharedPrefs.getString("app_theme", "system")
        b.tvThemeSummary.text = when(theme) {
            "light" -> "Светлая"
            "dark" -> "Темная"
            else -> "Системная"
        }
        b.cardTheme.setOnClickListener { showThemeDialog(b) }

        b.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_animations", isChecked).apply()
        }
        b.switchLavaBg.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_lava_bg", isChecked).apply()
            // Пересоздаем, чтобы изменения фона подхватились сразу
            recreate()
        }
        b.switchBlocked.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_show_blocked", isChecked).apply()
        }
        b.switchSearch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_hide_search", isChecked).apply()
        }

        setupHeaderExpansion(b)
    }

    private fun togglePhotoMenuMode(enable: Boolean) {
        if (isPhotoMenuMode.value == enable) return
        isPhotoMenuMode.value = enable
        
        val b = binding ?: return
        
        // В новой версии с Compose кнопками мы просто меняем состояния
        if (enable) {
            backLabelState.value = "Закрыть"
            backIconState.intValue = R.drawable.ic_cancel
            
            logoutLabelState.value = "Просмотр"
            logoutIconState.intValue = R.drawable.ic_person
            logoutColorState.value = Color.White
            
            extraSettingsLabelState.value = "Удалить"
            extraSettingsIconState.intValue = R.drawable.ic_cancel
            extraSettingsColorState.value = Color(0xFFEF5350)
            
            startWobbling(b)
        } else {
            backLabelState.value = "Назад"
            backIconState.intValue = R.drawable.ic_arrow_back
            
            logoutLabelState.value = "Выход"
            logoutIconState.intValue = R.drawable.ic_exit_to_app
            logoutColorState.value = Color(0xFFEF5350)
            
            extraSettingsLabelState.value = "Настройки"
            extraSettingsIconState.intValue = R.drawable.ic_settings
            extraSettingsColorState.value = Color.White
            
            stopWobbling()
        }
    }

    private fun flipView(view: View, label: View, onHalfway: () -> Unit) {
        // Метод больше не используется для основных кнопок шапки, так как они в Compose
    }

    private fun startWobbling(b: ActivitySettingsContentBinding) {
        // Объединяем в список те вью, которые остались (например кнопки в режиме без фото)
        // Но основные кнопки шапки теперь в Compose, анимацию покачивания для них нужно делать внутри Composable.
        // Для простоты пока оставим этот метод пустым или адаптируем.
    }

    private fun stopWobbling() {
        wobbleAnimators.forEach { it.cancel() }
        wobbleAnimators.clear()
        // В новой версии иконки в Compose, управление их состоянием через стейты
    }

    override fun onResume() {
        super.onResume()
        LavaBackgroundState.onActivityResumed()
        isClosing = false
        val b = binding
        if (b != null && isPhotoMenuMode.value) startWobbling(b)
        
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri
        if (b != null) {
            b.tvUserNameStatic.text = savedName
            b.etSettingsName.setText(savedName)
            b.tvAccountHeaderSummary.text = savedName
            currentNameInDB = savedName
            applyAvatarState(savedAvatarUri)
        }
    }

    override fun onPause() {
        super.onPause()
        stopWobbling()
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        if (android.os.Build.VERSION.SDK_INT < 34) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun applyAvatarState(avatarUri: String?) {
        val b = binding ?: return
        if (avatarUri != null) {
            b.layoutWithPhoto.visibility = View.VISIBLE
            b.layoutNoPhoto.visibility = View.GONE
            b.headerStaticBlock.minimumHeight = (320 * resources.displayMetrics.density).toInt()
        } else {
            b.layoutWithPhoto.visibility = View.GONE
            b.layoutNoPhoto.visibility = View.VISIBLE
            b.headerStaticBlock.minimumHeight = 0
            if (isPhotoMenuMode.value) togglePhotoMenuMode(false)
        }
    }

    private fun setupHeaderExpansion(b: ActivitySettingsContentBinding) {
        b.nestedScrollView.setOnTouchListener { v, event ->
            if (isAnimating) return@setOnTouchListener true
            
            val isKeyboardVisible = ViewCompat.getRootWindowInsets(b.root)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            val isAccountExpanded = b.layoutAccountCollapsible.visibility == View.VISIBLE
            if (isKeyboardVisible || isAccountExpanded) return@setOnTouchListener false

            if (b.nestedScrollView.scrollY > 0 && !isHeaderExpanded) {
                pullStartY = -1f
                return@setOnTouchListener false
            }
            if (currentAvatarUri == null) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val headerHeight = b.headerStaticBlock.height
                    if (event.y > headerHeight && !isHeaderExpanded) {
                        pullStartY = -1f
                    } else {
                        pullStartY = event.y
                    }
                    isVibrated = false
                    isHeaderMoving = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pullStartY == -1f) {
                        pullStartY = event.y
                        return@setOnTouchListener false
                    }
                    val dy = event.y - pullStartY
                    if (!isHeaderExpanded) {
                        if (dy > 20f && b.nestedScrollView.scrollY == 0) {
                            if (!isHeaderMoving) {
                                isHeaderMoving = true
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)
                            updateHeaderAnimation(b, progress)
                            if (progress >= 1f && !isVibrated) {
                                b.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                isVibrated = true
                            }
                            return@setOnTouchListener true
                        }
                    } else {
                        if (dy < -20f) {
                            if (!isHeaderMoving) {
                                isHeaderMoving = true
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            val progress = (1f - (Math.abs(dy) / PULL_THRESHOLD)).coerceIn(0f, 1f)
                            updateHeaderAnimation(b, progress)
                            if (progress <= 0f) {
                                isHeaderExpanded = false
                                isHeaderMoving = false
                                v.parent.requestDisallowInterceptTouchEvent(false)
                                return@setOnTouchListener false 
                            }
                            return@setOnTouchListener true
                        } else if (dy > 150f && !isClosing) {
                            openFullPhoto()
                            pullStartY = event.y
                            return@setOnTouchListener true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (pullStartY != -1f && isHeaderMoving) {
                        val dy = event.y - pullStartY
                        if (!isHeaderExpanded) {
                            if (dy > PULL_THRESHOLD / 2) animateHeaderState(b, true)
                            else animateHeaderState(b, false)
                        } else {
                            if (dy < -PULL_THRESHOLD / 3) animateHeaderState(b, false)
                            else animateHeaderState(b, true)
                        }
                    }
                    pullStartY = -1f
                    isHeaderMoving = false
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    private fun updateHeaderAnimation(b: ActivitySettingsContentBinding, progress: Float) {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val leftColWidth = b.layoutLeftColumn.width.toFloat()
        val cardWidth = b.photoCard.width.toFloat()
        val cardHeight = b.photoCard.height.toFloat()
        val headerHeight = b.headerStaticBlock.height.toFloat()

        if (leftColWidth == 0f || cardWidth == 0f || headerHeight == 0f) return

        val statusBarHeight = ViewCompat.getRootWindowInsets(b.root)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top?.toFloat() ?: 0f
        val innerPadding = b.layoutWithPhoto.paddingTop.toFloat()
        val totalOffsetUp = statusBarHeight + innerPadding

        val targetScaleX = screenWidth / cardWidth
        val targetScaleY = (headerHeight + totalOffsetUp) / cardHeight 
        
        val currentScaleX = 1f + (targetScaleX - 1f) * progress
        val currentScaleY = 1f + (targetScaleY - 1f) * progress
        
        b.photoCard.scaleX = currentScaleX
        b.photoCard.scaleY = currentScaleY
        
        b.layoutPhotoInternal.scaleX = 1f / currentScaleX
        b.layoutPhotoInternal.scaleY = 1f / currentScaleY
        b.layoutPhotoInternal.pivotX = 0f
        b.layoutPhotoInternal.pivotY = cardHeight
        
        val extraPadding = (16 * progress * density)
        b.tvUserNameWP.translationX = extraPadding
        b.tvUserNameWP.translationY = -extraPadding
        b.tvStatusWP.translationX = extraPadding
        b.tvStatusWP.translationY = -extraPadding

        val gradView = b.viewPhotoInfoGradient
        gradView.pivotX = 0f
        gradView.pivotY = gradView.height.toFloat()
        gradView.scaleY = 1f / currentScaleY
        gradView.scaleX = 1f

        val otherAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
        val otherTranslationX = -leftColWidth * progress

        backLabelAlpha.floatValue = otherAlpha
        backLabelTranslationX.floatValue = otherTranslationX

        b.composeBackWP.alpha = 1f
        b.composeBackWP.translationX = 0f
        b.composeBackWP.translationZ = 10f * density * progress
        b.layoutLeftColumn.alpha = otherAlpha
        b.layoutLeftColumn.translationX = otherTranslationX

        b.photoCard.pivotX = 0f
        b.photoCard.pivotY = 0f
        b.photoCard.translationX = -(leftColWidth * progress)
        b.photoCard.translationY = -(totalOffsetUp * progress)

        b.photoCard.radius = (24 * (1f - progress)).coerceAtLeast(0f) * density
        b.photoCard.cardElevation = (8 * (1f - progress)).coerceAtLeast(0f) * density
        
        val expandedExtraHeight = (screenWidth - headerHeight).coerceAtLeast(0f)
        val pushDown = expandedExtraHeight * progress
        b.layoutSettingsBodyContainer.translationY = pushDown
    }

    private fun animateHeaderState(b: ActivitySettingsContentBinding, expand: Boolean) {
        currentAnimator?.cancel()
        isAnimating = true
        val cardWidth = b.photoCard.width
        if (cardWidth == 0) return
        val currentProgress = (b.photoCard.scaleX - 1f) / ((resources.displayMetrics.widthPixels.toFloat() / cardWidth) - 1f)
        val startVal = if (currentProgress.isNaN()) 0f else currentProgress.coerceIn(0f, 1f)
        val animator = ValueAnimator.ofFloat(startVal, if (expand) 1f else 0f)
        currentAnimator = animator
        animator.addUpdateListener { anim -> updateHeaderAnimation(b, anim.animatedValue as Float) }
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isHeaderExpanded = expand
                isAnimating = false
                currentAnimator = null
            }
        })
        animator.start()
    }

    private fun openFullPhoto() {
        if (currentAvatarUri == null || isClosing) return
        isClosing = true
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("EXTRA_URI", currentAvatarUri)
        val rect = Rect()
        profileImageView?.getGlobalVisibleRect(rect)
        intent.putExtra("EXTRA_RECT", rect)
        photoViewLauncher.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun setupAccountCollapsible(b: ActivitySettingsContentBinding) {
        b.layoutAccountHeader.setOnClickListener {
            val isExpanded = b.layoutAccountCollapsible.visibility == View.VISIBLE
            b.layoutAccountData.translationY = 0f
            TransitionManager.beginDelayedTransition(b.layoutAccountData, AutoTransition().apply { duration = 200 })
            b.layoutAccountCollapsible.visibility = if (isExpanded) View.GONE else View.VISIBLE
            b.ivAccountArrow.animate().rotation(if (isExpanded) 90f else -90f).setDuration(200).start()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("settings_account_expanded", !isExpanded).apply()
        }
    }

    private fun setupInlineAccountEditing(b: ActivitySettingsContentBinding) {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAccountChanges(b)
                b.inputLayoutName.error = null
                b.inputLayoutLogin.error = null
                b.inputLayoutPassword.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        b.etSettingsName.addTextChangedListener(textWatcher)
        b.etSettingsLogin.addTextChangedListener(textWatcher)
        b.etSettingsPassword.addTextChangedListener(textWatcher)

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.postDelayed({
                    val parent = v.parent as? View ?: return@postDelayed
                    val targetY = b.layoutAccountData.top + parent.top - (60 * resources.displayMetrics.density).toInt()
                    b.nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
                }, 100)
            }
        }
        b.etSettingsName.onFocusChangeListener = focusListener
        b.etSettingsLogin.onFocusChangeListener = focusListener
        b.etSettingsPassword.onFocusChangeListener = focusListener

        b.btnSaveAccount.setOnClickListener {
            val newName = b.etSettingsName.text.toString().trim()
            val newLogin = b.etSettingsLogin.text.toString().trim()
            val newPass = b.etSettingsPassword.text.toString()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            
            val errorName = if (newName.isEmpty()) "Имя не может быть пустым" else ValidationUtils.getValidationError(newName, false)
            var errorLogin = when {
                newLogin.isEmpty() -> "Логин не может быть пустым"
                newLogin != currentLoginInDB && sharedPrefs.contains(newLogin) -> "Этот логин уже занят"
                else -> ValidationUtils.getValidationError(newLogin, true)
            }
            var errorPass = if (newPass.length < 8) "Минимум 8 символов" else null
            
            if (errorName != null || errorLogin != null || errorPass != null) {
                if (errorName != null) { b.inputLayoutName.error = errorName; b.inputLayoutName.shake() }
                if (errorLogin != null) { b.inputLayoutLogin.error = errorLogin; b.inputLayoutLogin.shake() }
                if (errorPass != null) { b.inputLayoutPassword.error = errorPass; b.inputLayoutPassword.shake() }
                return@setOnClickListener
            }
            
            val oldName = currentNameInDB
            val oldLogin = currentLoginInDB
            val oldPass = currentPassInDB

            sharedPrefs.edit().apply {
                if (newLogin != currentLoginInDB) {
                    val avatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    putString("current_user", newLogin); putString(newLogin, newPass); putString("${newLogin}_name", newName)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)
                    remove(currentLoginInDB); remove("${currentLoginInDB}_name"); remove("${currentLoginInDB}_avatar")
                } else { putString("${currentLoginInDB}_name", newName); putString(currentLoginInDB, newPass) }
                apply()
            }
            currentNameInDB = newName; currentLoginInDB = newLogin; currentPassInDB = newPass
            
            b.tvUserNameStatic.text = newName; b.tvAccountHeaderSummary.text = newName
            
            PrimeNotification.show(this, "Данные обновлены") {
                sharedPrefs.edit().apply {
                    if (newLogin != oldLogin) {
                        val currentAvatar = sharedPrefs.getString("${newLogin}_avatar", null)
                        putString("current_user", oldLogin); putString(oldLogin, oldPass); putString("${oldLogin}_name", oldName)
                        if (currentAvatar != null) putString("${oldLogin}_avatar", currentAvatar)
                        remove(newLogin); remove("${newLogin}_name"); remove("${newLogin}_avatar")
                    } else { putString("${oldLogin}_name", oldName); putString(oldLogin, oldPass) }
                    apply()
                }
                currentNameInDB = oldName; currentLoginInDB = oldLogin; currentPassInDB = oldPass
                b.etSettingsName.setText(oldName); b.etSettingsLogin.setText(oldLogin); b.etSettingsPassword.setText(oldPass)
                b.tvUserNameStatic.text = oldName; b.tvAccountHeaderSummary.text = oldName
                checkAccountChanges(b)
            }
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(b.etSettingsName.windowToken, 0)
            b.etSettingsName.clearFocus(); b.etSettingsLogin.clearFocus(); b.etSettingsPassword.clearFocus()
            checkAccountChanges(b)
        }
        checkAccountChanges(b)
    }

    private fun checkAccountChanges(b: ActivitySettingsContentBinding) {
        val newName = b.etSettingsName.text.toString().trim()
        val newLogin = b.etSettingsLogin.text.toString().trim()
        val newPass = b.etSettingsPassword.text.toString()
        val nameChanged = newName != currentNameInDB; val loginChanged = newLogin != currentLoginInDB; val passChanged = newPass != currentPassInDB
        
        if (nameChanged) { b.inputLayoutName.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel); b.inputLayoutName.setStartIconOnClickListener { b.etSettingsName.setText(currentNameInDB) } } 
        else { b.inputLayoutName.startIconDrawable = null; b.inputLayoutName.setStartIconOnClickListener(null) }
        
        if (loginChanged) { b.inputLayoutLogin.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel); b.inputLayoutLogin.setStartIconOnClickListener { b.etSettingsLogin.setText(currentLoginInDB) } } 
        else { b.inputLayoutLogin.startIconDrawable = null; b.inputLayoutLogin.setStartIconOnClickListener(null) }
        
        if (passChanged) { b.inputLayoutPassword.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel); b.inputLayoutPassword.setStartIconOnClickListener { b.etSettingsPassword.setText(currentPassInDB) } } 
        else { b.inputLayoutPassword.startIconDrawable = null; b.inputLayoutPassword.setStartIconOnClickListener(null) }
        
        animateSaveButton(b, (nameChanged || loginChanged || passChanged) && newLogin.isNotEmpty() && newName.isNotEmpty())
    }

    private fun animateSaveButton(b: ActivitySettingsContentBinding, show: Boolean) {
        if (show && b.btnSaveAccount.visibility == View.VISIBLE) return
        if (!show && b.btnSaveAccount.visibility == View.GONE) return
        b.btnSaveAccount.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let { focusedView -> imm.hideSoftInputFromWindow(focusedView.windowToken, 0); focusedView.clearFocus() }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (uriToDelete == null) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        sharedPrefs.edit().remove("${currentUser}_avatar").apply()
        currentAvatarUri = null; applyAvatarState(null)
        PrimeNotification.show(this, "Фото удалено") { sharedPrefs.edit().putString("${currentUser}_avatar", uriToDelete).apply(); currentAvatarUri = uriToDelete; applyAvatarState(uriToDelete) }
    }

    private fun showLogoutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog).setTitle("Выход").setMessage("Сделать выход из аккаунта?").setPositiveButton("Да") { _, _ -> getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).edit().putBoolean("is_logged_in", false).apply(); startActivity(Intent(this, LoginActivity::class.java)); finishAffinity() }.setNegativeButton("Нет", null).show()
    }

    private fun showThemeDialog(b: ActivitySettingsContentBinding) {
        isThemeDialogVisible.value = true
    }

    @Composable
    private fun ThemeOptionItem(
        label: String,
        value: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = androidx.compose.material3.RadioButtonDefaults.colors(
                        selectedColor = Color.White,
                        unselectedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.material3.Text(
                    text = label,
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    private fun applyThemeChange(newTheme: String) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system")
        
        if (newTheme != currentTheme) {
            sharedPrefs.edit().putString("app_theme", newTheme).apply()
            
            // Notification and restart
            PrimeNotification.show(this, "Тема применена")
            
            val intent = Intent(this, HiActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finishAffinity()
        }
        isThemeDialogVisible.value = false
    }
}
