package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import java.util.concurrent.Executors
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.messenger.prime.databinding.ActivitySettingsContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition


class SettingsActivity : AppCompatActivity() {

    private var binding: ActivitySettingsContentBinding? = null

    private var isClosing = false
    private val isPhotoMenuMode = mutableStateOf(false)
    private val wobbleAnimators = mutableListOf<Animator>()

    private val isThemeDialogVisible = mutableStateOf(false)

    private var currentAvatarUri: String? = null

    private var currentNameInDB: String = ""
    private var currentLoginInDB: String = ""
    private var currentPassInDB: String = ""

    private val avatarUriState = mutableStateOf<String?>(null)
    private var profileImageView: ImageView? = null

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data
            var uriStr = data?.getStringExtra("EDITED_IMAGE_URI")
            if (uriStr.isNullOrEmpty()) uriStr = data?.getStringExtra("EXTRA_IMAGE_URI")
            if (uriStr.isNullOrEmpty() && data?.data != null) uriStr = data?.data.toString()

            if (!uriStr.isNullOrEmpty()) {
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                val isGif = uriStr.lowercase().contains(".gif")
                val ext = if (isGif) ".gif" else ".jpg"

                val permanentAvatar = File(filesDir, "my_profile_avatar$ext")
                try {
                    val srcUri = Uri.parse(uriStr)
                    val inputStream = try {
                        contentResolver.openInputStream(srcUri)
                    } catch (_: Exception) { null }
                        ?: if (srcUri.scheme == "file" && srcUri.path != null) FileInputStream(File(srcUri.path!!))
                           else FileInputStream(File(uriStr.removePrefix("file://")))

                    inputStream.use { input ->
                        FileOutputStream(permanentAvatar).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val permAvatarUri = Uri.fromFile(permanentAvatar).toString()

                currentAvatarUri = permAvatarUri
                avatarUriState.value = permAvatarUri

                sharedPrefs.edit()
                    .putString("my_avatar", permAvatarUri)
                    .putString("my_avatar_uri", permAvatarUri)
                    .putString("my_local_avatar", permAvatarUri)
                    .putString("${currentUser}_avatar", permAvatarUri)
                    .putString("${currentUser}_avatarUri", permAvatarUri)
                    .apply()

                val b = binding
                if (b != null) {
                    val radiusPx = (14 * resources.displayMetrics.density).toInt()
                    val sig = ObjectKey(if (permanentAvatar.exists()) permanentAvatar.lastModified() else System.currentTimeMillis())
                    if (isGif) {
                        Glide.with(this).asGif().load(permanentAvatar).transform(CenterCrop(), RoundedCorners(radiusPx)).signature(sig).placeholder(R.drawable.ic_person).into(b.ivPhotoCard)
                    } else {
                        Glide.with(this).load(permanentAvatar).transform(CenterCrop(), RoundedCorners(radiusPx)).signature(sig).placeholder(R.drawable.ic_person).into(b.ivPhotoCard)
                    }
                }
                applyAvatarState(permAvatarUri)
                sendProfileUpdateOverBluetooth()
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                PrimeNotification.show(this, if (isGif) "GIF-аватарка установлена" else "Фото готово")
            }
        }
    }

    private fun setGifAvatarDirectly(uri: Uri) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val permanentAvatar = File(filesDir, "my_profile_avatar.gif")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(permanentAvatar).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val permAvatarUri = Uri.fromFile(permanentAvatar).toString()
        currentAvatarUri = permAvatarUri
        avatarUriState.value = permAvatarUri

        sharedPrefs.edit()
            .putString("my_avatar", permAvatarUri)
            .putString("my_avatar_uri", permAvatarUri)
            .putString("my_local_avatar", permAvatarUri)
            .putString("${currentUser}_avatar", permAvatarUri)
            .putString("${currentUser}_avatarUri", permAvatarUri)
            .apply()

        val b = binding
        if (b != null) {
            val radiusPx = (14 * resources.displayMetrics.density).toInt()
            val sig = ObjectKey(if (permanentAvatar.exists()) permanentAvatar.lastModified() else System.currentTimeMillis())
            Glide.with(this).asGif().load(permanentAvatar).transform(CenterCrop(), RoundedCorners(radiusPx)).signature(sig).placeholder(R.drawable.ic_person).into(b.ivPhotoCard)
        }
        applyAvatarState(permAvatarUri)
        sendProfileUpdateOverBluetooth()
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, "GIF-аватарка установлена")
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val uriStr = it.toString().lowercase()
            val mime = try { contentResolver.getType(it) } catch (_: Exception) { null }
            val isGif = uriStr.endsWith(".gif") || uriStr.contains("gif") || "image/gif".equals(mime, ignoreCase = true)

            if (isGif) {
                setGifAvatarDirectly(it)
            } else {
                val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                    putExtra("EXTRA_IMAGE_URI", it.toString())
                    putExtra("IS_PROFILE_PHOTO", true)
                }
                photoEditorLauncher.launch(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun updateBackCallbackState() {
        val b = binding
        backCallback.isEnabled = isPhotoMenuMode.value || (b != null && b.layoutAccountCollapsible.visibility == View.VISIBLE)
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val b = binding
            if (isPhotoMenuMode.value) {
                togglePhotoMenuMode(false)
            } else if (b != null && b.layoutAccountCollapsible.visibility == View.VISIBLE) {
                toggleAccountCollapsible(b)
            }
            updateBackCallbackState()
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

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser
        currentPassInDB = sharedPrefs.getString(currentUser, "") ?: ""
        
        val savedName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", "Я") ?: "Я"
        currentNameInDB = savedName
        val savedAvatarUri = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val darkTheme = isSystemInDarkTheme()
            val isThemeVisible = isThemeDialogVisible.value
            val density = LocalDensity.current
            val statusBarTopPx = WindowInsets.statusBars.getTop(density)
            val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
            
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
                            b.tvUserNameWP.text = savedName
                            b.tvUserNameWP.isSelected = true
                            b.etSettingsName.setText(savedName)
                            b.etSettingsLogin.setText(currentUser)
                            b.etSettingsPassword.setText(currentPassInDB)
                            b.tvAccountHeaderSummary.text = savedName
                            val versionName = try {
                                val pInfo = packageManager.getPackageInfo(packageName, 0)
                                val vName = pInfo.versionName ?: "1.0"
                                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    pInfo.longVersionCode
                                } else {
                                    @Suppress("DEPRECATION")
                                    pInfo.versionCode.toLong()
                                }
                                "Prime $vName (build $vCode)"
                            } catch (e: Exception) {
                                "Prime ${BuildConfig.VERSION_NAME}"
                            }
                            b.tvAppVersion.text = versionName

                            val isExpanded = sharedPrefs.getBoolean("settings_account_expanded", false)
                            b.layoutAccountCollapsible.visibility = if (isExpanded) View.VISIBLE else View.GONE
                            b.ivAccountArrow.rotation = if (isExpanded) -90f else 90f

                            setupComposePhoto(b)
                            setupListeners(b)
                            applyAvatarState(savedAvatarUri)

                            // Инициализация живого фона в шапке
                            b.composeHeaderBackground.apply {
                                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                                setContent {
                                    PrimeTheme(darkTheme = darkTheme) {
                                        AnimatedBackground(
                                            darkTheme = darkTheme,
                                            ignoreSettingsToggle = false
                                        )
                                    }
                                }
                            }

                            b.nestedScrollView.setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_base))
                            b.headerStaticBlock.clipChildren = true

                            val sysDensity = resources.displayMetrics.density
                            val initialExtraPadding = (12 * sysDensity).toInt()
                            val initialTopInset = if (statusBarTopPx > 0) statusBarTopPx else getStatusBarHeight()

                            b.headerStaticBlock.updatePadding(top = 0)
                            b.layoutWithPhoto.updatePadding(
                                top = initialTopInset + initialExtraPadding,
                                bottom = initialExtraPadding
                            )
                            b.layoutNoPhoto.updatePadding(
                                top = initialTopInset + initialExtraPadding,
                                bottom = initialExtraPadding
                            )

                            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                                val sDensity = resources.displayMetrics.density
                                val extraPadding = (12 * sDensity).toInt()
                                val topInset = if (systemBars.top > 0) systemBars.top else getStatusBarHeight()

                                b.headerStaticBlock.updatePadding(top = 0)
                                b.layoutWithPhoto.updatePadding(
                                    top = topInset + extraPadding,
                                    bottom = extraPadding
                                )
                                b.layoutNoPhoto.updatePadding(
                                    top = topInset + extraPadding,
                                    bottom = extraPadding
                                )
                                
                                b.nestedScrollView.updatePadding(bottom = systemBars.bottom)
                                insets
                            }
                            ViewCompat.requestApplyInsets(view)

                            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                            windowInsetsController.isAppearanceLightStatusBars = !darkTheme

                            // Инициализация кнопок с эффектом BlurView
                            setupBlurButtons(b, darkTheme)

                            b.headerStaticBlock.translationZ = 4f
                            b.photoCard.translationZ = 8f

                            view
                        },
                        update = { _ ->
                            val b = binding ?: return@AndroidView
                            val sysDensity = resources.displayMetrics.density
                            val extraPadding = (12 * sysDensity).toInt()
                            val topInset = if (statusBarTopPx > 0) statusBarTopPx else getStatusBarHeight()

                            b.headerStaticBlock.updatePadding(top = 0)
                            b.layoutWithPhoto.updatePadding(
                                top = topInset + extraPadding,
                                bottom = extraPadding
                            )
                            b.layoutNoPhoto.updatePadding(
                                top = topInset + extraPadding,
                                bottom = extraPadding
                            )
                            
                            if (navBarBottomPx > 0) {
                                b.nestedScrollView.updatePadding(bottom = navBarBottomPx)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Кастомное окно выбора темы с эффектом BlurView
                    if (isThemeVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { isThemeDialogVisible.value = false },
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
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

        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private fun setupComposePhoto(b: ActivitySettingsContentBinding) {
        b.ivPhotoCard.apply {
            setOnClickListener {
                if (currentAvatarUri != null) togglePhotoMenuMode(!isPhotoMenuMode.value)
                else pickImage.launch("image/*")
            }
            profileImageView = this
        }
        updatePhotoCardImage(b)
    }

    private fun updatePhotoCardImage(b: ActivitySettingsContentBinding) {
        val avatarUri = currentAvatarUri ?: avatarUriState.value
        val radiusPx = (14 * resources.displayMetrics.density).toInt()
        if (!avatarUri.isNullOrEmpty()) {
            try {
                val model: Any = if (avatarUri.startsWith("content://") || avatarUri.startsWith("file://") || avatarUri.startsWith("http")) {
                    Uri.parse(avatarUri)
                } else {
                    val f = File(avatarUri)
                    if (f.exists()) f else Uri.parse(avatarUri)
                }
                val file = if (model is File) model else if (model is Uri && "file" == model.scheme && model.path != null) File(model.path!!) else null
                val isGif = avatarUri.lowercase().contains(".gif")
                val signatureKey = ObjectKey(if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis())

                if (isGif) {
                    Glide.with(this)
                        .asGif()
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(b.ivPhotoCard)
                } else {
                    Glide.with(this)
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(R.drawable.ic_person)
                        .into(b.ivPhotoCard)
                }
            } catch (e: Exception) {
                b.ivPhotoCard.setImageResource(R.drawable.ic_person)
            }
        } else {
            b.ivPhotoCard.setImageResource(R.drawable.ic_person)
        }
    }

    private fun setupBlurButtons(b: ActivitySettingsContentBinding, darkTheme: Boolean) {
        b.btnBackWP.setOnClickListener {
            if (isPhotoMenuMode.value) togglePhotoMenuMode(false)
            else onBackPressedDispatcher.onBackPressed()
        }
        b.btnChangePhotoWP.setOnClickListener { pickImage.launch("image/*") }
        b.btnLogoutWP.setOnClickListener {
            if (isPhotoMenuMode.value) openFullPhoto()
            else showLogoutDialog()
        }
        b.btnExtraSettingsWP.setOnClickListener {
            if (isPhotoMenuMode.value) {
                handlePhotoDeletionWithUndo(currentAvatarUri)
                togglePhotoMenuMode(false)
            } else {
                b.nestedScrollView.smoothScrollTo(0, 1000)
            }
        }
        b.photoCard.setOnClickListener {
            if (currentAvatarUri != null) togglePhotoMenuMode(!isPhotoMenuMode.value)
            else pickImage.launch("image/*")
        }
        b.photoCard.setOnLongClickListener {
            if (currentAvatarUri != null) {
                togglePhotoMenuMode(!isPhotoMenuMode.value)
                true
            } else false
        }
        b.btnBackNP.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnChangePhotoNP.setOnClickListener { pickImage.launch("image/*") }
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        b.btnExtraSettingsNP.setOnClickListener { b.nestedScrollView.smoothScrollTo(0, 1000) }
    }

    private fun setupListeners(b: ActivitySettingsContentBinding) {
        b.btnBackNP.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        
        b.btnChangePhotoNP.setOnClickListener { pickImage.launch("image/*") }

        b.btnExtraSettingsNP.setOnClickListener {
            b.nestedScrollView.smoothScrollTo(0, 1000)
        }

        setupInlineAccountEditing(b)
        setupAccountCollapsible(b)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        val theme = sharedPrefs.getString("app_theme", "system")
        b.tvThemeSummary.text = when(theme) {
            "light" -> "Светлая"
            "dark" -> "Темная"
            else -> "Системная"
        }
        b.cardTheme.setOnClickListener { showThemeDialog(b) }

        b.switchLavaBg.setOnCheckedChangeListener(null)
        b.switchLavaBg.isChecked = sharedPrefs.getBoolean("settings_lava_bg", true)
        b.switchLavaBg.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_lava_bg", isChecked).apply()
            restartApp()
        }
        b.cardLavaBg.setOnClickListener {
            b.switchLavaBg.isChecked = !b.switchLavaBg.isChecked
        }

        b.cardGithub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Vaniahorosh/Prime_messenger"))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun togglePhotoMenuMode(enable: Boolean) {
        if (isPhotoMenuMode.value == enable) return
        isPhotoMenuMode.value = enable
        
        val b = binding ?: return
        
        if (enable) {
            b.btnBackWP.setIconResource(R.drawable.ic_cancel)
            b.btnBackWP.setIconTintResource(R.color.white)
            b.tvBackLabelWP.text = "Закрыть"
            
            b.btnLogoutWP.setIconResource(R.drawable.ic_person)
            b.btnLogoutWP.setIconTintResource(R.color.white)
            b.tvLogoutWP.text = "Просмотр"
            b.tvLogoutWP.setTextColor(android.graphics.Color.WHITE)
            
            b.btnExtraSettingsWP.setIconResource(R.drawable.ic_cancel)
            b.btnExtraSettingsWP.setIconTintResource(R.color.prime_danger_light)
            b.tvExtraSettingsWP.text = "Удалить"
            b.tvExtraSettingsWP.setTextColor(ContextCompat.getColor(this, R.color.prime_danger_light))
            
            startWobbling(b)
        } else {
            b.btnBackWP.setIconResource(R.drawable.ic_arrow_back)
            b.btnBackWP.setIconTintResource(R.color.white)
            b.tvBackLabelWP.text = "Назад"
            
            b.btnLogoutWP.setIconResource(R.drawable.ic_exit_to_app)
            b.btnLogoutWP.setIconTintResource(R.color.prime_danger_light)
            b.tvLogoutWP.text = "Выход"
            b.tvLogoutWP.setTextColor(ContextCompat.getColor(this, R.color.prime_danger_light))
            
            b.btnExtraSettingsWP.setIconResource(R.drawable.ic_settings)
            b.btnExtraSettingsWP.setIconTintResource(R.color.white)
            b.tvExtraSettingsWP.text = "К Настройкам"
            b.tvExtraSettingsWP.setTextColor(android.graphics.Color.WHITE)
            
            stopWobbling()
        }
        updateBackCallbackState()
    }

    private fun flipView(view: View, label: View, onHalfway: () -> Unit) {
    }

    private fun startWobbling(b: ActivitySettingsContentBinding) {
        stopWobbling()
        val viewsToWobble = listOf(
            b.btnBackWPLayout,
            b.btnChangePhotoWPLayout,
            b.btnLogoutWPLayout,
            b.btnExtraSettingsWPLayout
        )

        viewsToWobble.forEach { view ->
            val animator = ObjectAnimator.ofFloat(view, "rotation", -2.5f, 2.5f).apply {
                duration = 100
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
            wobbleAnimators.add(animator)
        }
    }

    private fun stopWobbling() {
        wobbleAnimators.forEach { it.cancel() }
        wobbleAnimators.clear()
        binding?.let { b ->
            val viewsToReset = listOf(
                b.btnBackWPLayout,
                b.btnChangePhotoWPLayout,
                b.btnLogoutWPLayout,
                b.btnExtraSettingsWPLayout
            )
            viewsToReset.forEach { view ->
                view.rotation = 0f
            }
        }
    }

    private val chatDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val navIntent = Intent(this@SettingsActivity, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(navIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.messenger.prime.CHAT_DELETED")
        ContextCompat.registerReceiver(this, chatDeletedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        LavaBackgroundState.onActivityResumed()
        isClosing = false
        val b = binding
        if (b != null && isPhotoMenuMode.value) startWobbling(b)
        
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", "Я") ?: "Я"
        val savedAvatarUri = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)
        
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri
        if (b != null) {
            b.tvUserNameStatic.text = savedName
            b.tvUserNameWP.text = savedName
            b.tvUserNameWP.isSelected = true
            b.etSettingsName.setText(savedName)
            b.tvAccountHeaderSummary.text = savedName
            currentNameInDB = savedName
            applyAvatarState(savedAvatarUri)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(chatDeletedReceiver) } catch (e: Exception) {}
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
        b.layoutAccountData.visibility = View.VISIBLE
        b.layoutAccountData.alpha = 1f
        b.layoutAccountData.translationY = 0f
        b.layoutSettingsHeader.translationY = 0f
        b.layoutSwitches.translationY = 0f
        b.layoutInfoHeader.translationY = 0f
        b.layoutInfoBlock.translationY = 0f
        updatePhotoCardImage(b)
    }

    private fun openFullPhoto() {
        val avatarUri = currentAvatarUri ?: avatarUriState.value
        if (avatarUri.isNullOrEmpty()) {
            PrimeNotification.show(this, "Фотография не установлена")
            return
        }
        if (isClosing) return

        val myName = currentNameInDB.ifEmpty { "Я" }
        val msg = ChatMessage(
            "Аватар профиля",
            "Это вы",
            myName,
            true,
            null,
            0L,
            avatarUri,
            "my_avatar_preview"
        )
        MediaPlayerActivity.setSharedMediaList(listOf(msg), 0)
        val intent = Intent(this, MediaPlayerActivity::class.java)
        startActivity(intent)
    }

    private fun toggleAccountCollapsible(b: ActivitySettingsContentBinding) {
        val isExpanded = b.layoutAccountCollapsible.visibility == View.VISIBLE
        b.layoutAccountData.translationY = 0f
        TransitionManager.beginDelayedTransition(b.layoutAccountData, AutoTransition().apply { duration = 200 })
        b.layoutAccountCollapsible.visibility = if (isExpanded) View.GONE else View.VISIBLE
        b.ivAccountArrow.animate().rotation(if (isExpanded) 90f else -90f).setDuration(200).start()
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("settings_account_expanded", !isExpanded).apply()
        updateBackCallbackState()
    }

    private fun setupAccountCollapsible(b: ActivitySettingsContentBinding) {
        b.layoutAccountHeader.setOnClickListener {
            toggleAccountCollapsible(b)
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

        val scrollToField = { v: View ->
            v.postDelayed({
                val density = resources.displayMetrics.density
                val layoutParent = v.parent as? View ?: v
                val inputLayout = layoutParent.parent as? View ?: layoutParent
                val targetY = b.layoutAccountData.top + inputLayout.top - (40 * density).toInt()
                b.nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
            }, 100)
        }

        b.etSettingsName.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToField(v)
        }
        b.etSettingsLogin.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToField(v)
        }
        b.etSettingsPassword.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.postDelayed({
                    val density = resources.displayMetrics.density
                    val targetY = b.layoutAccountData.top + b.layoutAccountCollapsible.bottom - (40 * density).toInt()
                    b.nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
                }, 100)
            }
        }

        b.etSettingsName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.etSettingsLogin.requestFocus()
                true
            } else false
        }

        b.etSettingsLogin.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.etSettingsPassword.requestFocus()
                true
            } else false
        }

        b.etSettingsPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                b.btnSaveAccount.performClick()
                true
            } else false
        }

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
                putString("my_name", newName)
                putString("my_local_name", newName)
                putString("current_user_name", newName)
                if (newLogin != currentLoginInDB) {
                    val avatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    putString("current_user", newLogin); putString(newLogin, newPass); putString("${newLogin}_name", newName)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)
                    remove(currentLoginInDB); remove("${currentLoginInDB}_name"); remove("${currentLoginInDB}_avatar")
                } else { putString("${currentLoginInDB}_name", newName); putString(currentLoginInDB, newPass) }
                apply()
            }
            currentNameInDB = newName; currentLoginInDB = newLogin; currentPassInDB = newPass
            sendBroadcast(Intent("com.messenger.prime.NAME_CHANGED").setPackage(packageName))
            sendProfileUpdateOverBluetooth()
            
            b.tvUserNameStatic.text = newName
            b.tvUserNameWP.text = newName
            b.tvAccountHeaderSummary.text = newName
            
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
                b.tvUserNameStatic.text = oldName
                b.tvUserNameWP.text = oldName
                b.tvAccountHeaderSummary.text = oldName
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
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun sendProfileUpdateOverBluetooth() {
        try {
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: return
            val myDisplayName = sharedPrefs.getString("my_name", null)
                ?: sharedPrefs.getString("my_local_name", null)
                ?: sharedPrefs.getString("current_user_name", null)
                ?: sharedPrefs.getString("${currentUser}_name", currentUser) ?: currentUser
            val localAvatarUri = sharedPrefs.getString("my_avatar", null)
                ?: sharedPrefs.getString("my_local_avatar", null)
                ?: sharedPrefs.getString("my_avatar_uri", null)
                ?: sharedPrefs.getString("${currentUser}_avatar", "") ?: ""

            val allThreads = BluetoothSocketHolder.getAllConnectedThreads()
            for (threadObj in allThreads) {
                if (threadObj is ChatPersonActivity.ConnectedThread && threadObj.isAlive) {
                    val handshake = "HANDSHAKE:login=$currentUser;name=$myDisplayName;avatar=$localAvatarUri;version=${ChatPersonActivity.getAppVersionCode(this)}"
                    threadObj.sendPacket(0x01.toByte(), handshake.toByteArray(Charsets.UTF_8))

                    if (localAvatarUri.isNotEmpty()) {
                        try {
                            val isStream = contentResolver.openInputStream(Uri.parse(localAvatarUri))
                            if (isStream != null) {
                                val bitmap = BitmapFactory.decodeStream(isStream)
                                isStream.close()
                                if (bitmap != null) {
                                    val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, false)
                                    val baos = ByteArrayOutputStream()
                                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                                    threadObj.sendPacket(0x07.toByte(), baos.toByteArray())
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        val fJpg = File(filesDir, "my_profile_avatar.jpg")
        val fGif = File(filesDir, "my_profile_avatar.gif")
        val fUserJpg = File(filesDir, "avatar_${currentUser}.jpg")
        val fUserGif = File(filesDir, "avatar_${currentUser}.gif")
        if (fJpg.exists()) fJpg.delete()
        if (fGif.exists()) fGif.delete()
        if (fUserJpg.exists()) fUserJpg.delete()
        if (fUserGif.exists()) fUserGif.delete()

        sharedPrefs.edit()
            .remove("my_avatar")
            .remove("my_local_avatar")
            .remove("my_avatar_uri")
            .remove("${currentUser}_avatar")
            .remove("${currentUser}_avatarUri")
            .apply()

        currentAvatarUri = null
        avatarUriState.value = null

        try {
            Glide.get(this).clearMemory()
            Executors.newSingleThreadExecutor().execute {
                try {
                    Glide.get(applicationContext).clearDiskCache()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val b = binding
        if (b != null) {
            b.ivPhotoCard.setImageResource(R.drawable.ic_person)
        }
        applyAvatarState(null)
        sendProfileUpdateOverBluetooth()
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, "Фото удалено")
    }

    private fun showLogoutDialog() {
        PrimeBlurDialog.show(
            activity = this,
            title = "Выход",
            message = "Сделать выход из аккаунта?",
            positiveText = "Да",
            negativeText = "Нет",
            isPositiveDanger = true,
            onPositive = {
                val allThreads = BluetoothSocketHolder.getAllConnectedThreads()
                if (allThreads.isNotEmpty()) {
                    try {
                        for (threadObj in allThreads) {
                            if (threadObj != null) {
                                val method = threadObj.javaClass.getMethod("sendPacket", Byte::class.java, ByteArray::class.java)
                                val currentUser = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("current_user", "") ?: ""
                                val myDisplayName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${currentUser}_name", currentUser) ?: currentUser
                                val payload = "DELETE_CHAT:login=$myDisplayName;name=$myDisplayName".toByteArray(Charsets.UTF_8)
                                method.invoke(threadObj, 8.toByte(), payload) // TYPE_CHAT_DELETED = 0x08
                            }
                        }
                        Thread.sleep(100)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    BluetoothSocketHolder.clearSocket()
                    PrimeBluetoothService.stopService(this)
                }
                getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        )
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

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (36 * resources.displayMetrics.density).toInt()
    }

    private fun restartApp() {
        val intent = Intent(this, HiActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun applyThemeChange(newTheme: String) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system")
        
        if (newTheme != currentTheme) {
            sharedPrefs.edit().putString("app_theme", newTheme).apply()
            
            val mode = when (newTheme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            restartApp()
        }
        isThemeDialogVisible.value = false
    }
}
