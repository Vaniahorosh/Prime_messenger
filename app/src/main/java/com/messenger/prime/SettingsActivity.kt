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
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.messenger.prime.databinding.DialogColorAccentBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import eightbitlab.com.blurview.BlurView
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
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
                val isGif = uriStr.lowercase().contains(".gif")
                val ext = if (isGif) ".gif" else ".jpg"
                val timestamp = System.currentTimeMillis()

                val permanentAvatar = File(filesDir, "my_profile_avatar_${timestamp}$ext")
                val tempAvatar = File(filesDir, "tmp_my_profile_avatar$ext")
                try {
                    val srcUri = Uri.parse(uriStr)
                    val inputStream = try {
                        contentResolver.openInputStream(srcUri)
                    } catch (_: Exception) { null }
                        ?: if (srcUri.scheme == "file" && srcUri.path != null) FileInputStream(File(srcUri.path!!))
                           else FileInputStream(File(uriStr.removePrefix("file://")))

                    inputStream?.use { input ->
                        FileOutputStream(tempAvatar).use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                    if (tempAvatar.exists() && tempAvatar.length() > 0) {
                        if (permanentAvatar.exists()) permanentAvatar.delete()
                        tempAvatar.renameTo(permanentAvatar)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val permAvatarUri = Uri.fromFile(permanentAvatar).toString()

                AvatarHistoryManager.addMyAvatar(this, permAvatarUri)
                currentAvatarIndex = 0
                currentAvatarUri = permAvatarUri
                avatarUriState.value = permAvatarUri

                val b = binding
                if (b != null) {
                    updatePhotoCardImage(b)
                }
                applyAvatarState(permAvatarUri)
                Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                PrimeNotification.show(this, if (isGif) "GIF-аватарка добавлена" else "Фото добавлено")
            }
        }
    }

    private fun setGifAvatarDirectly(uri: Uri) {
        val timestamp = System.currentTimeMillis()
        val permanentAvatar = File(filesDir, "my_profile_avatar_${timestamp}.gif")
        val tempAvatar = File(filesDir, "tmp_my_profile_avatar.gif")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempAvatar).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            if (tempAvatar.exists() && tempAvatar.length() > 0) {
                if (permanentAvatar.exists()) permanentAvatar.delete()
                tempAvatar.renameTo(permanentAvatar)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val permAvatarUri = Uri.fromFile(permanentAvatar).toString()
        AvatarHistoryManager.addMyAvatar(this, permAvatarUri)
        currentAvatarIndex = 0
        currentAvatarUri = permAvatarUri
        avatarUriState.value = permAvatarUri

        val b = binding
        if (b != null) {
            updatePhotoCardImage(b)
        }
        applyAvatarState(permAvatarUri)
        Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, "GIF-аватарка добавлена")
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
                PrimeTransitions.applyOpenTransition(this)
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
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_settings)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        PrimePredictiveBack.attach(this) {
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

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
                                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                                val sDensity = resources.displayMetrics.density
                                val extraPadding = (12 * sDensity).toInt()
                                val topInset = if (systemBars.top > 0) systemBars.top else getStatusBarHeight()

                                b.root.setPadding(cutout.left, 0, cutout.right, 0)

                                b.headerStaticBlock.updatePadding(top = 0)
                                b.layoutWithPhoto.updatePadding(
                                    top = maxOf(topInset, cutout.top) + extraPadding,
                                    bottom = extraPadding
                                )
                                b.layoutNoPhoto.updatePadding(
                                    top = maxOf(topInset, cutout.top) + extraPadding,
                                    bottom = extraPadding
                                )
                                
                                b.nestedScrollView.updatePadding(bottom = maxOf(systemBars.bottom, cutout.bottom))
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
        setupAvatarCardSwipe(b)
        updatePhotoCardImage(b)
    }

    private var currentAvatarIndex = 0

    private fun setupAvatarCardSwipe(b: ActivitySettingsContentBinding) {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 60 && Math.abs(velocityX) > 100) {
                    val history = AvatarHistoryManager.getMyAvatarHistory(this@SettingsActivity)
                    if (history.size > 1) {
                        if (diffX < 0) {
                            currentAvatarIndex = (currentAvatarIndex + 1) % history.size
                        } else {
                            currentAvatarIndex = if (currentAvatarIndex - 1 < 0) history.size - 1 else currentAvatarIndex - 1
                        }
                        updatePhotoCardImage(b)
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (currentAvatarUri != null) togglePhotoMenuMode(!isPhotoMenuMode.value)
                else pickImage.launch("image/*")
                return true
            }
        })

        b.photoCard.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun updatePhotoCardImage(b: ActivitySettingsContentBinding) {
        val history = AvatarHistoryManager.getMyAvatarHistory(this)
        if (history.isEmpty()) {
            b.ivPhotoCard.setImageResource(R.drawable.ic_person)
            animateAvatarCounterBadge(b.tvAvatarCounter, 0, 0)
            currentAvatarUri = null
            return
        }

        if (currentAvatarIndex !in history.indices) {
            currentAvatarIndex = 0
        }

        val avatarUri = history[currentAvatarIndex]
        currentAvatarUri = avatarUri
        avatarUriState.value = avatarUri

        animateAvatarCounterBadge(b.tvAvatarCounter, currentAvatarIndex, history.size)

        val radiusPx = (14 * resources.displayMetrics.density).toInt()
        try {
            val (model, file) = parseAvatarModelAndFile(avatarUri)
            if (model != null) {
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
            } else {
                b.ivPhotoCard.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {
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
        b.cardTheme.addBounceTouchEffect()
        b.cardLavaBg.addBounceTouchEffect()
        b.cardGithub.addBounceTouchEffect()
        b.cardColorAccent.addBounceTouchEffect()
        b.cardThanks.addBounceTouchEffect()

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

        updateAccentUi(b)
        b.cardColorAccent.setOnClickListener { showColorAccentDialog(b) }

        setupThanksCard(b)
        setupStorageSection(b)
        setupNavStyleSection(b)
    }

    private fun setupNavStyleSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentNavStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"

        val updateNavStyleUi = {
            val isIsland = currentNavStyle == "island"
            b.rbStyleIsland.isChecked = isIsland
            b.rbStyleBottomBar.isChecked = !isIsland

            val accentColor = ColorAccentManager.getCurrentAccentColor(this)
            b.cardStyleIsland.strokeColor = if (isIsland) accentColor else android.graphics.Color.TRANSPARENT
            b.cardStyleIsland.strokeWidth = if (isIsland) (2 * resources.displayMetrics.density).toInt() else 0

            b.cardStyleBottomBar.strokeColor = if (!isIsland) accentColor else android.graphics.Color.TRANSPARENT
            b.cardStyleBottomBar.strokeWidth = if (!isIsland) (2 * resources.displayMetrics.density).toInt() else 0

            setupBottomNav(b)
        }

        updateNavStyleUi()

        val setStyle = { style: String ->
            if (currentNavStyle != style) {
                currentNavStyle = style
                sharedPrefs.edit().putString("navigation_style", style).apply()
                updateNavStyleUi()
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        }

        b.cardStyleIsland.setOnClickListener { setStyle("island") }
        b.rbStyleIsland.setOnClickListener { setStyle("island") }

        b.cardStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }
        b.rbStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }
    }

    private fun updateNavUnreadBadge(navView: View) {
        val tvNavChatsUnreadBadge = navView.findViewById<TextView>(R.id.tvNavChatsUnreadBadge) ?: return
        var totalUnread = 0
        try {
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("persisted_chats", "[]") ?: "[]"
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                totalUnread += obj.optInt("unreadCount", 0)
            }
        } catch (_: Exception) {}

        if (totalUnread > 0) {
            tvNavChatsUnreadBadge.text = if (totalUnread > 99) "99+" else totalUnread.toString()
            tvNavChatsUnreadBadge.visibility = View.VISIBLE
        } else {
            tvNavChatsUnreadBadge.visibility = View.GONE
        }
    }

    private fun updateM3TabState(
        isActive: Boolean,
        indicatorView: View?,
        iconView: ImageView?,
        labelView: TextView?,
        accentColor: Int,
        secondaryColor: Int
    ) {
        if (indicatorView == null || labelView == null) return
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val visibleInactiveColor = if (isDark) android.graphics.Color.parseColor("#E6FFFFFF") else android.graphics.Color.parseColor("#E6154B87")

        indicatorView.visibility = View.VISIBLE

        if (isActive) {
            indicatorView.backgroundTintList = ColorStateList.valueOf(accentColor)
            indicatorView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                .start()

            iconView?.setColorFilter(android.graphics.Color.WHITE)
            iconView?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(180)?.start()

            labelView.setTextColor(accentColor)
            labelView.setTypeface(null, android.graphics.Typeface.BOLD)
            labelView.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.alpha(1.0f)?.setDuration(180)?.start()
        } else {
            indicatorView.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            indicatorView.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setDuration(180)
                .start()

            iconView?.setColorFilter(visibleInactiveColor)
            iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(180)?.start()

            labelView.setTextColor(visibleInactiveColor)
            labelView.setTypeface(null, android.graphics.Typeface.NORMAL)
            labelView.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.alpha(0.85f)?.setDuration(180)?.start()
        }
    }

    private fun setupBottomNav(b: ActivitySettingsContentBinding? = binding) {
        val bindingRef = b ?: binding
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"
        val navView = bindingRef?.root?.findViewById<View>(R.id.blurBottomNav)
            ?: findViewById<View>(R.id.blurBottomNav)
            ?: return

        val density = resources.displayMetrics.density
        val nestedScrollView = bindingRef?.nestedScrollView
            ?: findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
        } catch (_: Exception) {
            false
        }

        if (navStyle != "bottom_bar" || isTablet || isEmbedded) {
            navView.visibility = View.GONE
            nestedScrollView?.setPadding(
                nestedScrollView.paddingLeft,
                nestedScrollView.paddingTop,
                nestedScrollView.paddingRight,
                (24 * density).toInt()
            )
            return
        }

        navView.visibility = View.VISIBLE
        navView.bringToFront()

        val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
        val systemBarsBottom = rootInsets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

        val initialLp = navView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (initialLp != null) {
            val targetMargin = systemBarsBottom + (12 * density).toInt()
            if (initialLp.bottomMargin != targetMargin) {
                initialLp.bottomMargin = targetMargin
                navView.layoutParams = initialLp
            }
        }

        nestedScrollView?.setPadding(
            nestedScrollView.paddingLeft,
            nestedScrollView.paddingTop,
            nestedScrollView.paddingRight,
            systemBarsBottom + (88 * density).toInt()
        )

        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            if (lp != null) {
                val targetMargin = systemBars.bottom + (12 * density).toInt()
                if (lp.bottomMargin != targetMargin) {
                    lp.bottomMargin = targetMargin
                    v.layoutParams = lp
                }
            }
            nestedScrollView?.setPadding(
                nestedScrollView.paddingLeft,
                nestedScrollView.paddingTop,
                nestedScrollView.paddingRight,
                systemBars.bottom + (88 * density).toInt()
            )
            insets
        }

        val btnNavChats = navView.findViewById<View>(R.id.btnNavChats)
        val btnNavDevices = navView.findViewById<View>(R.id.btnNavDevices)
        val btnNavSearch = navView.findViewById<View>(R.id.btnNavSearch)

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val secondaryColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

        val vNavChatsIndicator = navView.findViewById<View>(R.id.vNavChatsIndicator)
        val vNavDevicesIndicator = navView.findViewById<View>(R.id.vNavDevicesIndicator)
        val vNavSearchIndicator = navView.findViewById<View>(R.id.vNavSearchIndicator)
        val vNavProfileIndicator = navView.findViewById<View>(R.id.vNavProfileIndicator)

        val ivNavChatsIcon = navView.findViewById<ImageView>(R.id.ivNavChatsIcon)
        val ivNavDevicesIcon = navView.findViewById<ImageView>(R.id.ivNavDevicesIcon)
        val ivNavSearchIcon = navView.findViewById<ImageView>(R.id.ivNavSearchIcon)

        val tvNavChatsLabel = navView.findViewById<TextView>(R.id.tvNavChatsLabel)
        val tvNavDevicesLabel = navView.findViewById<TextView>(R.id.tvNavDevicesLabel)
        val tvNavSearchLabel = navView.findViewById<TextView>(R.id.tvNavSearchLabel)
        val tvNavProfileLabel = navView.findViewById<TextView>(R.id.tvNavProfileLabel)

        updateM3TabState(false, vNavChatsIndicator, ivNavChatsIcon, tvNavChatsLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, tvNavDevicesLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavSearchIndicator, ivNavSearchIcon, tvNavSearchLabel, accentColor, secondaryColor)
        updateM3TabState(true, vNavProfileIndicator, null, tvNavProfileLabel, accentColor, secondaryColor)
        updateNavUnreadBadge(navView)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val myAvatar = sharedPrefs.getString("my_avatar", null)
                ?: sharedPrefs.getString("my_local_avatar", null)
            if (!myAvatar.isNullOrEmpty()) {
                val (_, file) = parseAvatarModelAndFile(myAvatar)
                if (file != null && file.exists()) {
                    Glide.with(this).load(file).into(profileAvatarIv)
                } else {
                    Glide.with(this).load(myAvatar).into(profileAvatarIv)
                }
            } else {
                profileAvatarIv.setImageResource(R.drawable.ic_person)
            }
        }

        val blurNavView = navView as? eightbitlab.com.blurview.BlurView
        if (blurNavView != null) {
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#700F172A") else android.graphics.Color.parseColor("#70154B87")
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            blurNavView.setupBlur(rootView, 22f, overlayColor, window.decorView.background)
        }

        btnNavChats?.setOnClickListener {
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600 ||
                    try {
                        androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
                    } catch (_: Exception) {
                        false
                    }
            if (isTablet) {
                finish()
                PrimeTransitions.applyCloseTransition(this)
            } else {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                PrimeTransitions.applyPageFlipTransition(this, isForward = false)
                finish()
            }
        }

        btnNavDevices?.setOnClickListener {
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600 ||
                    try {
                        androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
                    } catch (_: Exception) {
                        false
                    }
            if (isTablet) {
                finish()
                PrimeTransitions.applyCloseTransition(this)
            } else {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_ACTION_SCAN", true)
                }
                startActivity(intent)
                PrimeTransitions.applyPageFlipTransition(this, isForward = false)
                finish()
            }
        }

        btnNavSearch?.setOnClickListener {
            val intent = Intent(this, GlobalSearchActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = false)
        }
    }

    private data class StorageBreakdown(
        var photosSize: Long = 0L,
        var videosSize: Long = 0L,
        var gifsSize: Long = 0L,
        var filesSize: Long = 0L,
        var cacheSize: Long = 0L,
        var totalSize: Long = 0L
    )

    private var currentStorageBreakdown: StorageBreakdown? = null

    private fun setupStorageSection(b: ActivitySettingsContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        b.btnClearStorage.backgroundTintList = accentTint
        b.cbStoragePhotos.buttonTintList = accentTint
        b.cbStorageVideos.buttonTintList = accentTint
        b.cbStorageGifs.buttonTintList = accentTint
        b.cbStorageFiles.buttonTintList = accentTint
        b.cbStorageCache.buttonTintList = accentTint

        b.rowStoragePhotos.setOnClickListener {
            b.cbStoragePhotos.isChecked = !b.cbStoragePhotos.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageVideos.setOnClickListener {
            b.cbStorageVideos.isChecked = !b.cbStorageVideos.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageGifs.setOnClickListener {
            b.cbStorageGifs.isChecked = !b.cbStorageGifs.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageFiles.setOnClickListener {
            b.cbStorageFiles.isChecked = !b.cbStorageFiles.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageCache.setOnClickListener {
            b.cbStorageCache.isChecked = !b.cbStorageCache.isChecked
            updateClearButtonText(b)
        }

        val listener = CompoundButton.OnCheckedChangeListener { _, _ -> updateClearButtonText(b) }
        b.cbStoragePhotos.setOnCheckedChangeListener(listener)
        b.cbStorageVideos.setOnCheckedChangeListener(listener)
        b.cbStorageGifs.setOnCheckedChangeListener(listener)
        b.cbStorageFiles.setOnCheckedChangeListener(listener)
        b.cbStorageCache.setOnCheckedChangeListener(listener)

        b.btnClearStorage.setOnClickListener {
            clearSelectedStorage(b)
        }

        calculateStorageAsync(b)
    }

    private fun calculateStorageAsync(b: ActivitySettingsContentBinding) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bd = StorageBreakdown()
            val files = filesDir.listFiles() ?: emptyArray()

            for (f in files) {
                if (!f.isFile) continue
                val len = f.length()
                val name = f.name.lowercase(Locale.US)

                if (name.endsWith(".gif")) {
                    bd.gifsSize += len
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi")) {
                    bd.videosSize += len
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.startsWith("rec_photo_") || name.startsWith("rec_avatar_") || name.startsWith("avatar_") || name.startsWith("cache_img_")) {
                    bd.photosSize += len
                } else if (name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log")) {
                    bd.cacheSize += len
                } else {
                    bd.filesSize += len
                }
            }

            try {
                if (cacheDir != null && cacheDir.exists()) {
                    bd.cacheSize += cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                }
                val extCache = externalCacheDir
                if (extCache != null && extCache.exists()) {
                    bd.cacheSize += extCache.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                }
            } catch (_: Exception) {}

            bd.totalSize = bd.photosSize + bd.videosSize + bd.gifsSize + bd.filesSize + bd.cacheSize

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    updateStorageUi(b, bd)
                }
            }
        }
    }

    private fun updateStorageUi(b: ActivitySettingsContentBinding, bd: StorageBreakdown) {
        currentStorageBreakdown = bd

        b.tvSizePhotos.text = formatStorageSize(bd.photosSize)
        b.tvSizeVideos.text = formatStorageSize(bd.videosSize)
        b.tvSizeGifs.text = formatStorageSize(bd.gifsSize)
        b.tvSizeFiles.text = formatStorageSize(bd.filesSize)
        b.tvSizeCache.text = formatStorageSize(bd.cacheSize)

        b.tvStorageTotalSize.text = "Всего занято: ${formatStorageSize(bd.totalSize)}"

        val total = bd.totalSize.coerceAtLeast(1L).toFloat()

        animateWeight(b.vChartPhotos, (bd.photosSize / total).coerceAtLeast(0.01f))
        animateWeight(b.vChartVideos, (bd.videosSize / total).coerceAtLeast(0.01f))
        animateWeight(b.vChartGifs, (bd.gifsSize / total).coerceAtLeast(0.01f))
        animateWeight(b.vChartFiles, (bd.filesSize / total).coerceAtLeast(0.01f))
        animateWeight(b.vChartOther, (bd.cacheSize / total).coerceAtLeast(0.01f))

        updateClearButtonText(b)
    }

    private fun animateWeight(view: View, targetWeight: Float) {
        val lp = view.layoutParams as? LinearLayout.LayoutParams ?: return
        val startWeight = if (lp.weight <= 0f) 0.01f else lp.weight
        val anim = ValueAnimator.ofFloat(startWeight, targetWeight)
        anim.duration = 400
        anim.addUpdateListener {
            val w = it.animatedValue as Float
            val p = view.layoutParams as LinearLayout.LayoutParams
            p.weight = w
            view.layoutParams = p
        }
        anim.start()
    }

    private fun updateClearButtonText(b: ActivitySettingsContentBinding) {
        val bd = currentStorageBreakdown ?: return
        var selectedBytes = 0L

        val pVisible = b.cbStoragePhotos.isChecked
        val vVisible = b.cbStorageVideos.isChecked
        val gVisible = b.cbStorageGifs.isChecked
        val fVisible = b.cbStorageFiles.isChecked
        val cVisible = b.cbStorageCache.isChecked

        if (pVisible) selectedBytes += bd.photosSize
        if (vVisible) selectedBytes += bd.videosSize
        if (gVisible) selectedBytes += bd.gifsSize
        if (fVisible) selectedBytes += bd.filesSize
        if (cVisible) selectedBytes += bd.cacheSize

        val total = bd.totalSize.coerceAtLeast(1L).toFloat()

        val pWeight = if (pVisible) (bd.photosSize / total).coerceAtLeast(0.01f) else 0.001f
        val vWeight = if (vVisible) (bd.videosSize / total).coerceAtLeast(0.01f) else 0.001f
        val gWeight = if (gVisible) (bd.gifsSize / total).coerceAtLeast(0.01f) else 0.001f
        val fWeight = if (fVisible) (bd.filesSize / total).coerceAtLeast(0.01f) else 0.001f
        val cWeight = if (cVisible) (bd.cacheSize / total).coerceAtLeast(0.01f) else 0.001f

        animateWeight(b.vChartPhotos, pWeight)
        animateWeight(b.vChartVideos, vWeight)
        animateWeight(b.vChartGifs, gWeight)
        animateWeight(b.vChartFiles, fWeight)
        animateWeight(b.vChartOther, cWeight)

        b.vChartPhotos.animate().alpha(if (pVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartVideos.animate().alpha(if (vVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartGifs.animate().alpha(if (gVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartFiles.animate().alpha(if (fVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartOther.animate().alpha(if (cVisible) 1.0f else 0.2f).setDuration(250).start()

        if (selectedBytes > 0) {
            b.btnClearStorage.isEnabled = true
            b.btnClearStorage.text = "Очистить: ${formatStorageSize(selectedBytes)}"
        } else {
            b.btnClearStorage.isEnabled = false
            b.btnClearStorage.text = "Ничего не выбрано"
        }
    }

    private fun clearSelectedStorage(b: ActivitySettingsContentBinding) {
        b.btnClearStorage.isEnabled = false
        b.btnClearStorage.text = "Очистка..."

        lifecycleScope.launch(Dispatchers.IO) {
            var deletedBytes = 0L

            val deletePhotos = b.cbStoragePhotos.isChecked
            val deleteVideos = b.cbStorageVideos.isChecked
            val deleteGifs = b.cbStorageGifs.isChecked
            val deleteFiles = b.cbStorageFiles.isChecked
            val deleteCache = b.cbStorageCache.isChecked

            val files = filesDir.listFiles() ?: emptyArray()

            for (f in files) {
                if (!f.isFile) continue
                val name = f.name.lowercase(Locale.US)
                val len = f.length()

                val isGif = name.endsWith(".gif")
                val isVid = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi")
                val isPhoto = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.startsWith("rec_photo_") || name.startsWith("cache_img_")
                val isTmp = name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log")

                if (name.startsWith("my_profile_avatar")) continue

                var delete = false
                if (isGif && deleteGifs) delete = true
                else if (isVid && deleteVideos) delete = true
                else if (isPhoto && deletePhotos) delete = true
                else if (isTmp && deleteCache) delete = true
                else if (!isGif && !isVid && !isPhoto && !isTmp && deleteFiles) delete = true

                if (delete) {
                    try {
                        if (f.delete()) {
                            deletedBytes += len
                        }
                    } catch (_: Exception) {}
                }
            }

            if (deleteCache) {
                try {
                    if (cacheDir != null && cacheDir.exists()) {
                        val cacheFiles = cacheDir.walkTopDown().filter { it.isFile }.toList()
                        for (cf in cacheFiles) {
                            val len = cf.length()
                            if (cf.delete()) deletedBytes += len
                        }
                    }
                    val extCache = externalCacheDir
                    if (extCache != null && extCache.exists()) {
                        val extCacheFiles = extCache.walkTopDown().filter { it.isFile }.toList()
                        for (cf in extCacheFiles) {
                            val len = cf.length()
                            if (cf.delete()) deletedBytes += len
                        }
                    }
                    Glide.get(applicationContext).clearDiskCache()
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    try {
                        Glide.get(applicationContext).clearMemory()
                    } catch (_: Exception) {}

                    // Reset cleared checkboxes
                    if (deletePhotos) b.cbStoragePhotos.isChecked = false
                    if (deleteVideos) b.cbStorageVideos.isChecked = false
                    if (deleteGifs) b.cbStorageGifs.isChecked = false
                    if (deleteFiles) b.cbStorageFiles.isChecked = false
                    if (deleteCache) b.cbStorageCache.isChecked = false

                    PrimeNotification.show(this@SettingsActivity, "Освобождено ${formatStorageSize(deletedBytes)}")
                    calculateStorageAsync(b)
                }
            }
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0) return "0 Б"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f КБ", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f МБ", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.2f ГБ", gb)
    }

    private fun setupThanksCard(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        var heartCount = sharedPrefs.getInt("thanks_heart_count", 0)

        val updateHeartBadge = {
            if (heartCount > 0) {
                b.layoutHeartBadge.visibility = View.VISIBLE
                b.tvHeartCount.text = heartCount.toString()
            } else {
                b.layoutHeartBadge.visibility = View.GONE
            }
        }
        updateHeartBadge()

        val devPrefix = "Главный разработчик: "
        val devName = "Vaniahorosh"
        val spannable = SpannableString(devPrefix + devName)
        val brandColor = ColorAccentManager.getCurrentAccentColor(this)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openGithubDev()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = brandColor
                ds.isUnderlineText = true
                ds.isFakeBoldText = true
            }
        }

        val startIdx = devPrefix.length
        val endIdx = startIdx + devName.length
        spannable.setSpan(clickableSpan, startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        b.tvThanksMainDev.text = spannable
        b.tvThanksMainDev.movementMethod = LinkMovementMethod.getInstance()

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                heartCount++
                sharedPrefs.edit().putInt("thanks_heart_count", heartCount).apply()
                updateHeartBadge()

                b.layoutHeartBadge.animate().cancel()
                b.layoutHeartBadge.scaleX = 1.3f
                b.layoutHeartBadge.scaleY = 1.3f
                b.layoutHeartBadge.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()

                b.cardThanks.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                spawnFloatingHeart(b, e.x, e.y)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val devRect = Rect()
                b.tvThanksMainDev.getGlobalVisibleRect(devRect)
                val cardRect = Rect()
                b.cardThanks.getGlobalVisibleRect(cardRect)
                val rawX = cardRect.left + e.x.toInt()
                val rawY = cardRect.top + e.y.toInt()

                if (devRect.contains(rawX, rawY)) {
                    openGithubDev()
                }
                return true
            }
        })

        b.cardThanks.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun openGithubDev() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Vaniahorosh"))
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun spawnFloatingHeart(b: ActivitySettingsContentBinding, x: Float, y: Float) {
        val density = resources.displayMetrics.density
        val heartSize = (48 * density).toInt()
        val heartView = ImageView(this).apply {
            setImageResource(R.drawable.ic_heart)
            layoutParams = FrameLayout.LayoutParams(heartSize, heartSize)
            this.x = (x - heartSize / 2f).coerceAtLeast(0f)
            this.y = (y - heartSize / 2f).coerceAtLeast(0f)
            scaleX = 0.3f
            scaleY = 0.3f
            alpha = 1.0f
            rotation = (-20..20).random().toFloat()
        }

        b.overlayHeartsContainer.addView(heartView)

        val targetY = heartView.y - (120 * density)

        heartView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .translationYBy(-40 * density)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                heartView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .y(targetY)
                    .alpha(0f)
                    .setDuration(600)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        b.overlayHeartsContainer.removeView(heartView)
                    }
                    .start()
            }
            .start()
    }

    private fun updateAccentUi(b: ActivitySettingsContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        b.tvAccentSummary.text = ColorAccentManager.getAccentSummary(this)
        b.vAccentColorPreview.backgroundTintList = ColorStateList.valueOf(accentColor)
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun showColorAccentDialog(b: ActivitySettingsContentBinding) {
        val dialogBinding = DialogColorAccentBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)

        val blurCard = dialogBinding.root.findViewById<BlurView>(R.id.blurDialogCard)
        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#400F172A") else android.graphics.Color.parseColor("#40154B87")
            blurCard.setupBlur(rootView, 16f, overlayColor, window.decorView.background)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    win.attributes = win.attributes.apply { blurBehindRadius = 60 }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        dialog.setOnShowListener {
            blurCard?.apply {
                alpha = 0f
                scaleX = 0.82f
                scaleY = 0.82f
                translationY = 60f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(300L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }
        }

        val isMonetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        dialogBinding.layoutMonetSwitch.visibility = if (isMonetSupported) View.VISIBLE else View.GONE
        val currentType = ColorAccentManager.getAccentType(this)

        dialogBinding.switchMonetAccent.setOnCheckedChangeListener(null)
        dialogBinding.switchMonetAccent.isChecked = (currentType == ColorAccentManager.ACCENT_TYPE_SYSTEM)

        val updateControlsState = { isMonetOn: Boolean ->
            dialogBinding.scrollPalette.alpha = if (isMonetOn) 0.5f else 1.0f
            dialogBinding.btnCustomPicker.alpha = if (isMonetOn) 0.5f else 1.0f
            for (i in 0 until dialogBinding.layoutSwatches.childCount) {
                dialogBinding.layoutSwatches.getChildAt(i).isEnabled = !isMonetOn
            }
            dialogBinding.btnCustomPicker.isEnabled = !isMonetOn
        }

        updateControlsState(dialogBinding.switchMonetAccent.isChecked)

        dialogBinding.switchMonetAccent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ColorAccentManager.setAccentSystem(this)
                updateAccentUi(b)
                sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
                dialog.dismiss()
                restartApp()
            } else {
                ColorAccentManager.setAccentDefault(this)
                updateAccentUi(b)
                updateControlsState(false)
                sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
                restartApp()
            }
        }

        dialogBinding.layoutSwatches.removeAllViews()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val currentColor = ColorAccentManager.getCurrentAccentColor(this)

        ColorAccentManager.PRESET_COLORS.forEach { preset ->
            val colorInt = if (isDark) preset.darkColor else preset.lightColor
            val swatch = View(this).apply {
                val sizePx = (36 * resources.displayMetrics.density).toInt()
                val marginPx = (8 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_brush_preview)
                backgroundTintList = ColorStateList.valueOf(colorInt)
                elevation = 4f

                if (colorInt == currentColor && currentType != ColorAccentManager.ACCENT_TYPE_SYSTEM) {
                    alpha = 1.0f
                    scaleX = 1.2f
                    scaleY = 1.2f
                } else {
                    alpha = 0.85f
                }

                setOnClickListener {
                    ColorAccentManager.setAccentCustom(this@SettingsActivity, colorInt)
                    updateAccentUi(b)
                    sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
                    dialog.dismiss()
                    restartApp()
                }
            }
            dialogBinding.layoutSwatches.addView(swatch)
        }

        dialogBinding.btnCustomPicker.setOnClickListener {
            dialog.dismiss()
            showCustomColorSpectrumPicker(b)
        }

        dialogBinding.btnResetDefault.setOnClickListener {
            ColorAccentManager.setAccentDefault(this)
            updateAccentUi(b)
            sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
            dialog.dismiss()
            restartApp()
        }

        dialog.show()
    }

    private fun showCustomColorSpectrumPicker(b: ActivitySettingsContentBinding) {
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)

        dialogBinding.btnPipette.visibility = View.GONE

        val blurCard = dialogBinding.root.findViewById<BlurView>(R.id.blurDialogCard)
        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#400F172A") else android.graphics.Color.parseColor("#40154B87")
            blurCard.setupBlur(rootView, 16f, overlayColor, window.decorView.background)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    win.attributes = win.attributes.apply { blurBehindRadius = 60 }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        dialog.setOnShowListener {
            blurCard?.apply {
                alpha = 0f
                scaleX = 0.82f
                scaleY = 0.82f
                translationY = 60f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(300L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }
        }

        var selectedColorInt = ColorAccentManager.getCurrentAccentColor(this)

        var isInternalChange = false
        fun updateDialogColors(color: Int) {
            if (isInternalChange) return
            isInternalChange = true
            try {
                dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                val hexStr = String.format("#%06X", (0xFFFFFF and color))
                if (dialogBinding.etHex.text?.toString() != hexStr) dialogBinding.etHex.setText(hexStr)
                val rStr = android.graphics.Color.red(color).toString()
                if (dialogBinding.etR.text?.toString() != rStr) dialogBinding.etR.setText(rStr)
                val gStr = android.graphics.Color.green(color).toString()
                if (dialogBinding.etG.text?.toString() != gStr) dialogBinding.etG.setText(gStr)
                val bStr = android.graphics.Color.blue(color).toString()
                if (dialogBinding.etB.text?.toString() != bStr) dialogBinding.etB.setText(bStr)
            } catch (_: Exception) {
            } finally {
                isInternalChange = false
            }
        }

        updateDialogColors(selectedColorInt)

        dialogBinding.spectrumView.setOnColorChangedListener { color ->
            selectedColorInt = color
            updateDialogColors(color)
        }

        val rgbWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val r = dialogBinding.etR.text.toString().toInt().coerceIn(0, 255)
                    val g = dialogBinding.etG.text.toString().toInt().coerceIn(0, 255)
                    val bVal = dialogBinding.etB.text.toString().toInt().coerceIn(0, 255)
                    val color = android.graphics.Color.rgb(r, g, bVal)
                    selectedColorInt = color
                    isInternalChange = true
                    dialogBinding.etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
        }

        val hexWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val color = android.graphics.Color.parseColor(s.toString())
                    selectedColorInt = color
                    isInternalChange = true
                    dialogBinding.etR.setText(android.graphics.Color.red(color).toString())
                    dialogBinding.etG.setText(android.graphics.Color.green(color).toString())
                    dialogBinding.etB.setText(android.graphics.Color.blue(color).toString())
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
        }

        dialogBinding.etHex.addTextChangedListener(hexWatcher)
        dialogBinding.etR.addTextChangedListener(rgbWatcher)
        dialogBinding.etG.addTextChangedListener(rgbWatcher)
        dialogBinding.etB.addTextChangedListener(rgbWatcher)

        dialogBinding.btnApplyColor.setOnClickListener {
            try {
                var hex = dialogBinding.etHex.text.toString().trim()
                if (!hex.startsWith("#")) hex = "#$hex"
                selectedColorInt = android.graphics.Color.parseColor(hex)
            } catch (_: Exception) {}

            ColorAccentManager.setAccentCustom(this, selectedColorInt)
            updateAccentUi(b)
            sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
            dialog.dismiss()
            restartApp()
        }

        dialog.show()
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
            when (intent?.action) {
                "com.messenger.prime.CHAT_DELETED" -> {
                    val navIntent = Intent(this@SettingsActivity, ChatListActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(navIntent)
                    finish()
                }
                "com.messenger.prime.NAV_STYLE_CHANGED", "com.messenger.prime.AVATAR_CHANGED" -> {
                    runOnUiThread { setupBottomNav() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.CHAT_DELETED")
            addAction("com.messenger.prime.NAV_STYLE_CHANGED")
            addAction("com.messenger.prime.AVATAR_CHANGED")
        }
        ContextCompat.registerReceiver(this, chatDeletedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        setupBottomNav()
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
            updatePhotoCardImage(b)
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
        PrimeTransitions.applyCloseTransition(this)
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
        val myHistory = AvatarHistoryManager.getMyAvatarHistory(this)
        if (myHistory.isEmpty()) {
            PrimeNotification.show(this, "Фотография не установлена")
            return
        }
        if (isClosing) return

        val myName = currentNameInDB.ifEmpty { "Я" }
        val mediaList = myHistory.mapIndexed { idx, uriStr ->
            ChatMessage(
                "Аватар профиля",
                if (idx == 0) "Это вы (Активный)" else "Это вы",
                myName,
                true,
                null,
                0L,
                uriStr,
                "my_avatar_preview_$idx"
            )
        }

        val startIdx = currentAvatarIndex.coerceIn(0, mediaList.size - 1)
        MediaPlayerActivity.setSharedMediaList(mediaList, startIdx)
        val intent = Intent(this, MediaPlayerActivity::class.java)
        startActivity(intent)
        PrimeTransitions.applyOpenTransition(this)
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

            val handshake = "HANDSHAKE:login=$currentUser;name=$myDisplayName;avatar=$localAvatarUri;version=${ChatPersonActivity.getAppVersionCode(this)}"
            BluetoothConnectionManager.getInstance().broadcastPacket(0x01.toByte(), handshake.toByteArray(Charsets.UTF_8))

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
                            BluetoothConnectionManager.getInstance().broadcastPacket(0x07.toByte(), baos.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (!uriToDelete.isNullOrEmpty()) {
            AvatarHistoryManager.removeMyAvatar(this, uriToDelete)
        }

        val history = AvatarHistoryManager.getMyAvatarHistory(this)
        if (history.isNotEmpty()) {
            currentAvatarIndex = 0
            currentAvatarUri = history[0]
            avatarUriState.value = history[0]
            applyAvatarState(history[0])
        } else {
            currentAvatarIndex = 0
            currentAvatarUri = null
            avatarUriState.value = null
            applyAvatarState(null)
        }

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
            updatePhotoCardImage(b)
        }
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
                val currentUser = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("current_user", "") ?: ""
                val myDisplayName = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).getString("${currentUser}_name", currentUser) ?: currentUser
                val payload = "DELETE_CHAT:login=$myDisplayName;name=$myDisplayName".toByteArray(Charsets.UTF_8)
                BluetoothConnectionManager.getInstance().broadcastPacket(8.toByte(), payload) // TYPE_CHAT_DELETED = 0x08
                Thread.sleep(100)
                BluetoothConnectionManager.getInstance().disconnect()
                BluetoothSocketHolder.clearSocket()
                PrimeBluetoothService.stopService(this)
                getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                PrimeTransitions.applyOpenTransition(this)
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
        PrimeTransitions.applyOpenTransition(this)
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
