package com.messenger.prime

import android.annotation.SuppressLint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
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
import androidx.compose.ui.graphics.Color as ComposeColor
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.messenger.prime.databinding.ActivitySettingsContentBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors


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

    private val pluginsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshPluginsList()
        }
    }

    private val pluginFilePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.data != null) {
            val uri = result.data?.data ?: return@registerForActivityResult
            var name: String? = null
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {}
            val plugin = PluginManager.importPluginFile(this, uri, name)
            if (plugin != null) {
                refreshPluginsList()
                PrimeNotification.show(this, "Плагин ${plugin.name} импортирован!")
            } else {
                Toast.makeText(this, "Ошибка импорта плагина", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    setDataAndType(it, mime ?: "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        
        val b = ActivitySettingsContentBinding.inflate(layoutInflater)
        binding = b
        setContentView(b.root)

        UpdateChecker.checkForUpdates(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val darkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !darkTheme)

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

        setupAccountCollapsible(b)
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
        val initialTopInset = getStatusBarHeight()

        b.headerStaticBlock.updatePadding(top = 0)
        b.layoutWithPhoto.updatePadding(
            top = initialTopInset + initialExtraPadding,
            bottom = initialExtraPadding
        )
        b.layoutNoPhoto.updatePadding(
            top = initialTopInset + initialExtraPadding,
            bottom = initialExtraPadding
        )

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
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
        ViewCompat.requestApplyInsets(b.root)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !darkTheme

        // Инициализация кнопок с эффектом BlurView
        setupBlurButtons(b, darkTheme)
        setupStickyBlurToolbar(b, darkTheme)
        setupM3ECardsAndSections(b)

        b.headerStaticBlock.translationZ = 4f
        b.photoCard.translationZ = 8f

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
                        val isRightToLeft = diffX < 0
                        if (isRightToLeft) {
                            currentAvatarIndex = (currentAvatarIndex + 1) % history.size
                        } else {
                            currentAvatarIndex = if (currentAvatarIndex - 1 < 0) history.size - 1 else currentAvatarIndex - 1
                        }
                        animateAvatarCardSlide(b.ivPhotoCard, isRightToLeft) {
                            val h = AvatarHistoryManager.getMyAvatarHistory(this@SettingsActivity)
                            if (currentAvatarIndex in h.indices) {
                                val selectedUri = h[currentAvatarIndex]
                                AvatarHistoryManager.setActiveMyAvatar(this@SettingsActivity, selectedUri)
                                currentAvatarUri = selectedUri
                                avatarUriState.value = selectedUri
                                applyAvatarState(selectedUri)
                                java.util.concurrent.Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
                                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                            }
                            updatePhotoCardImage(b)
                        }
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

    private fun animateAvatarCardSlide(view: View, isRightToLeft: Boolean, onHalfWay: () -> Unit) {
        val width = if (view.width > 0) view.width.toFloat() else (200f * resources.displayMetrics.density)
        val exitX = if (isRightToLeft) -width else width
        val enterX = if (isRightToLeft) width else -width

        view.animate()
            .translationX(exitX)
            .alpha(0.2f)
            .setDuration(140)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                onHalfWay()
                view.translationX = enterX
                view.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(180)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
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
                val signatureKey = ObjectKey(if (file != null && file.exists()) "${file.absolutePath}_${file.lastModified()}" else avatarUri)

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

    private fun showAvatarChoiceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_choose_avatar, null)
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setView(dialogView)
            .create()

        val btnGallery = dialogView.findViewById<View>(R.id.btnGallery)
        val avatars = listOf<Pair<ImageView?, String>>(
            Pair(dialogView.findViewById(R.id.ivAv1), "android.resource://${packageName}/drawable/avatar_1"),
            Pair(dialogView.findViewById(R.id.ivAv2), "android.resource://${packageName}/drawable/avatar_2"),
            Pair(dialogView.findViewById(R.id.ivAv3), "android.resource://${packageName}/drawable/avatar_3"),
            Pair(dialogView.findViewById(R.id.ivAv4), "android.resource://${packageName}/drawable/avatar_4"),
            Pair(dialogView.findViewById(R.id.ivAv5), "android.resource://${packageName}/drawable/avatar_5")
        )

        btnGallery?.setOnClickListener {
            dialog.dismiss()
            pickImage.launch("image/*")
        }

        avatars.forEach { (iv, uri) ->
            iv?.setOnClickListener {
                dialog.dismiss()
                AvatarHistoryManager.addMyAvatar(this@SettingsActivity, uri)
                currentAvatarIndex = 0
                currentAvatarUri = uri
                avatarUriState.value = uri

                val b = binding
                if (b != null) {
                    updatePhotoCardImage(b)
                }
                applyAvatarState(uri)
                java.util.concurrent.Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                PrimeNotification.show(this@SettingsActivity, "Аватарка обновлена")
            }
        }

        dialog.show()
    }

    private fun setupM3ECardsAndSections(b: ActivitySettingsContentBinding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateAccentUi(b)
            
            // Postpone heavy button shape initialization so it doesn't freeze the entrance animation
            b.nestedScrollView.postDelayed({
                val buttons = listOf(
                    b.btnBackWP, b.btnChangePhotoWP, b.btnLogoutWP, b.btnExtraSettingsWP,
                    b.btnBackNP, b.btnChangePhotoNP, b.btnLogoutNP, b.btnExtraSettingsNP
                )
                
                val shapes = listOf(
                    com.messenger.prime.shapes.M3EShapes.cookie6Sided,
                    com.messenger.prime.shapes.M3EShapes.flower,
                    com.messenger.prime.shapes.M3EShapes.verySunny,
                    com.messenger.prime.shapes.M3EShapes.cookie7Sided,
                    com.messenger.prime.shapes.M3EShapes.circle
                )
                
                val btnBgColor = ContextCompat.getColor(this, R.color.glass_white_20)
                
                for ((index, btn) in buttons.withIndex()) {
                    if (btn == null) continue
                    
                    // Use safe drawable background instead of buggy OutlineProvider
                    btn.outlineProvider = null
                    btn.clipToOutline = false
                    btn.elevation = 0f
                    btn.background = com.messenger.prime.shapes.M3EDrawable(shapes.random(), btnBgColor)
                    
                    // Beautiful entrance animation
                    btn.scaleX = 0.5f
                    btn.scaleY = 0.5f
                    btn.alpha = 0f
                    btn.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(350)
                        .setStartDelay((index % 4) * 40L) // Delay per row/group
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                        .start()
                }
            }, 100) // Small delay allows the Activity transition to finish smoothly
        }
        
        // Removed drawing cache optimizations - they cause OOM and ANRs on huge NestedScrollViews
        b.nestedScrollView.isDrawingCacheEnabled = false
    }

    private fun setupBlurButtons(b: ActivitySettingsContentBinding, darkTheme: Boolean) {
        val handleBack = {
            if (isPhotoMenuMode.value) {
                togglePhotoMenuMode(false)
            } else if (isTaskRoot) {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
                PrimeTransitions.applyCloseTransition(this)
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        b.btnBackWP.setOnClickListener { handleBack() }
        b.btnChangePhotoWP.setOnClickListener { showAvatarChoiceDialog() }
        b.btnLogoutWP.setOnClickListener {
            if (isPhotoMenuMode.value) openFullPhoto()
            else showLogoutDialog()
        }
        b.btnExtraSettingsWP.setOnClickListener {
            if (isPhotoMenuMode.value) {
                handlePhotoDeletionWithUndo(currentAvatarUri)
                togglePhotoMenuMode(false)
            } else {
                b.layoutSettingsHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
            }
        }
        b.photoCard.setOnClickListener {
            if (currentAvatarUri != null) togglePhotoMenuMode(!isPhotoMenuMode.value)
            else showAvatarChoiceDialog()
        }
        b.photoCard.setOnLongClickListener {
            if (currentAvatarUri != null) {
                togglePhotoMenuMode(!isPhotoMenuMode.value)
                true
            } else false
        }
        b.btnBackNP.setOnClickListener { handleBack() }
        b.btnChangePhotoNP.setOnClickListener { showAvatarChoiceDialog() }
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        b.btnExtraSettingsNP.setOnClickListener { 
            b.layoutSettingsHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.btnCheckUpdate.setOnClickListener {
            UpdateChecker.checkForUpdates(this, manualCheck = true)
            PrimeNotification.show(this, "Поиск обновлений...")
        }
    }

    private fun setupStickyBlurToolbar(b: ActivitySettingsContentBinding, darkTheme: Boolean) {
        val config = resources.configuration
        val isTablet = config.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
        } catch (_: Throwable) {
            false
        }
        val isTabletOrFoldable = isTablet || isEmbedded

        if (isTabletOrFoldable) {
            b.blurStickyToolbar.visibility = View.GONE
            return
        }

        fun getActualTopInset(): Int {
            val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
            val topFromInsets = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            return if (topFromInsets > 0) topFromInsets else getStatusBarHeight()
        }

        fun updateToolbarPadding() {
            val topInset = getActualTopInset()
            val lp = b.blurStickyToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null && lp.topMargin != 0) {
                lp.topMargin = 0
                b.blurStickyToolbar.layoutParams = lp
            }
            b.blurStickyToolbar.setPadding(0, topInset, 0, 0)
        }

        updateToolbarPadding()

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            updateToolbarPadding()
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(b.blurStickyToolbar) { _, insets ->
            updateToolbarPadding()
            insets
        }
        b.blurStickyToolbar.post {
            updateToolbarPadding()
        }

        val overlayColor = getDefaultBlurOverlayColor(this)
        val targetRootView = (b.root as? ViewGroup)
            ?: (window.decorView.findViewById<ViewGroup>(android.R.id.content))
            ?: (window.decorView as ViewGroup)

        b.blurStickyToolbar.setupBlur(
            rootView = targetRootView,
            blurRadius = 18f,
            tintColor = overlayColor,
            frameClearDrawable = window.decorView.background
        )

        b.btnStickyBack.setOnClickListener {
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        setupStickyNavToolbar(b)
        setupPluginsSection(b)
    }

    private fun smoothScrollToView(nestedScrollView: androidx.core.widget.NestedScrollView, targetView: View) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val animationsDisabled = sharedPrefs.getBoolean("disable_animations", false)

        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val scrollLocation = IntArray(2)
        nestedScrollView.getLocationOnScreen(scrollLocation)
        val toolbarHeight = binding?.blurStickyToolbar?.height ?: 0
        val targetY = nestedScrollView.scrollY + (location[1] - scrollLocation[1]) - toolbarHeight - 16

        if (animationsDisabled) {
            nestedScrollView.scrollTo(0, targetY.coerceAtLeast(0))
        } else {
            nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
        }
    }

    private fun getViewTopInContent(targetView: View, contentView: ViewGroup): Int {
        var y = 0
        var current: View? = targetView
        while (current != null && current != contentView) {
            y += current.top
            val parent = current.parent
            if (parent is View) {
                current = parent
            } else {
                break
            }
        }
        return y
    }

    private fun setupStickyNavToolbar(b: ActivitySettingsContentBinding) {
        val secondaryTextColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

        fun updateActiveTabUI(activeTab: TextView?, sectionName: String) {
            val accentColor = ColorAccentManager.getCurrentAccentColor(this)
            val tabs = listOfNotNull(b.tabStickyProfile, b.tabStickyTheme, b.tabStickyChat, b.tabStickyBrowser, b.tabStickyData, b.tabStickyPlugins, b.tabStickyInfo)
            tabs.forEach { tab ->
                if (tab == activeTab) {
                    tab.setTextColor(accentColor)
                    tab.alpha = 1.0f
                } else {
                    tab.setTextColor(secondaryTextColor)
                    tab.alpha = 0.7f
                }
            }
            b.tvStickySectionBadge.text = sectionName
            b.tvStickySectionBadge.setTextColor(accentColor)

            if (activeTab != null) {
                b.scrollStickyNav.post {
                    val tabLeft = activeTab.left
                    val tabWidth = activeTab.width
                    val scrollWidth = b.scrollStickyNav.width
                    val scrollToX = tabLeft - (scrollWidth / 2) + (tabWidth / 2)
                    b.scrollStickyNav.smoothScrollTo(scrollToX.coerceAtLeast(0), 0)
                }
            }
        }

        // --- Single Tap: Smooth scroll to the section ---
        b.tabStickyProfile.setOnClickListener {
            b.layoutAccountData?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.tabStickyTheme.setOnClickListener {
            b.layoutSettingsHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.tabStickyChat?.setOnClickListener {
            b.layoutChatHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.tabStickyBrowser?.setOnClickListener {
            b.layoutBrowserHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.tabStickyData.setOnClickListener {
            smoothScrollToView(b.nestedScrollView, b.layoutDataHeader)
        }

        b.tabStickyPlugins?.setOnClickListener {
            b.layoutPluginsHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
        }

        b.tabStickyInfo.setOnClickListener {
            smoothScrollToView(b.nestedScrollView, b.layoutInfoHeader)
        }

        // --- Long Click: Open dropdown Popup Menu with choices ---
        b.tabStickyProfile.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Имя и Логин", R.drawable.ic_person) {
                    smoothScrollToView(b.nestedScrollView, b.layoutAccountData)
                }
                .addItem(2, "Сменить фото профиля", R.drawable.ic_camera) {
                    showAvatarChoiceDialog()
                }
                .show()
            true
        }

        b.tabStickyTheme.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Тема оформления", R.drawable.ic_tema) {
                    smoothScrollToView(b.nestedScrollView, b.cardTheme)
                }
                .addItem(2, "Стиль навигации", R.drawable.ic_perspective) {
                    smoothScrollToView(b.nestedScrollView, b.cardNavStyleSection)
                }
                .addItem(3, "Цветовой акцент", R.drawable.ic_pipette) {
                    smoothScrollToView(b.nestedScrollView, b.cardColorAccent)
                }
                .addItem(4, "Живой фон", R.drawable.ic_live_fon) {
                    smoothScrollToView(b.nestedScrollView, b.cardLavaBg)
                }
                .addItem(5, "Отключение анимаций", R.drawable.ic_clock) {
                    b.layoutDisableAnimationsSection?.let { smoothScrollToView(b.nestedScrollView, it) }
                }
                .show()
            true
        }

        b.tabStickyChat?.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Обои чата", R.drawable.ic_photo) {
                    smoothScrollToView(b.nestedScrollView, b.cardWallpaperSection)
                }
                .addItem(2, "Скругление сообщений", R.drawable.ic_edit) {
                    smoothScrollToView(b.nestedScrollView, b.tvTitleBubbleRadius)
                }
                .addItem(3, "Скругление аватарки", R.drawable.ic_person) {
                    smoothScrollToView(b.nestedScrollView, b.tvTitleAvatarRadius)
                }
                .addItem(4, "Стиль в чате", R.drawable.ic_tema) {
                    smoothScrollToView(b.nestedScrollView, b.cardChatStyleSection)
                }
                .addItem(5, "Скругление блоков в чате", R.drawable.ic_perspective) {
                    b.tvTitleBottomBarRadius?.let { smoothScrollToView(b.nestedScrollView, it) }
                }
                .addItem(6, "Информация о собеседнике", R.drawable.ic_person) {
                    smoothScrollToView(b.nestedScrollView, b.cardPartnerInfoSection)
                }
                .addItem(7, "Форма кружков (Видео)", R.drawable.ic_camera) {
                    b.cardVideoNotesSection?.let { smoothScrollToView(b.nestedScrollView, it) }
                }
                .show()
            true
        }

        b.tabStickyBrowser?.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Встроенный браузер", R.drawable.ic_globe) {
                    b.layoutBrowserHeader?.let { smoothScrollToView(b.nestedScrollView, it) }
                }
                .addItem(2, "Поисковая система", R.drawable.ic_search) {
                    smoothScrollToView(b.nestedScrollView, b.cardBrowserSettings)
                }
                .addItem(3, "Очистить кэш", R.drawable.ic_trash) {
                    smoothScrollToView(b.nestedScrollView, b.layoutClearBrowserCache)
                }
                .show()
            true
        }

        b.tabStickyData.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Память Прайма", R.drawable.ic_file) {
                    smoothScrollToView(b.nestedScrollView, b.cardStorage)
                }
                .addItem(2, "Детальная выборка кэша", R.drawable.ic_trash) {
                    smoothScrollToView(b.nestedScrollView, b.layoutDataBlock)
                }
                .show()
            true
        }

        b.tabStickyPlugins?.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Управление плагинами", R.drawable.ic_pipette) {
                    b.cardPlugins?.let { smoothScrollToView(b.nestedScrollView, it) }
                }
                .addItem(2, "Добавить плагин (+)", R.drawable.ic_add) {
                    showAddPluginDialog()
                }
                .show()
            true
        }

        b.tabStickyInfo.setOnLongClickListener { v ->
            PrimePopupMenu.create(this, v)
                .addItem(1, "Исходный код", R.drawable.ic_github_logo) {
                    smoothScrollToView(b.nestedScrollView, b.cardGithub)
                }
                .addItem(2, "Благодарности", R.drawable.ic_heart) {
                    smoothScrollToView(b.nestedScrollView, b.cardThanks)
                }
                .addItem(3, "Проверить обновления", R.drawable.ic_prime_statusbar) {
                    smoothScrollToView(b.nestedScrollView, b.btnCheckUpdate)
                }
                .show()
            true
        }

        // Section progress calculation & active tab sync
        fun updateSectionProgressAndTabs() {
            val contentView = b.nestedScrollView.getChildAt(0) as? ViewGroup ?: return
            val maxScroll = (contentView.height - b.nestedScrollView.height).coerceAtLeast(1)
            val toolbarHeight = b.blurStickyToolbar?.height ?: 0
            val offset = toolbarHeight + (20 * resources.displayMetrics.density).toInt()
            val scrollY = b.nestedScrollView.scrollY

            val profileStart = 0
            val themeStart = (getViewTopInContent(b.layoutSettingsHeader, contentView) - offset).coerceAtLeast(1)
            val chatStart = b.layoutChatHeader?.let { (getViewTopInContent(it, contentView) - offset).coerceAtLeast(themeStart + 1) } ?: (themeStart + 1)
            val browserStart = b.layoutBrowserHeader?.let { (getViewTopInContent(it, contentView) - offset).coerceAtLeast(chatStart + 1) } ?: (chatStart + 1)
            val dataStart = (getViewTopInContent(b.layoutDataHeader, contentView) - offset).coerceAtLeast(browserStart + 1)
            val pluginsStart = b.layoutPluginsHeader?.let { (getViewTopInContent(it, contentView) - offset).coerceAtLeast(dataStart + 1) } ?: (dataStart + 1)
            val infoStart = (getViewTopInContent(b.layoutInfoHeader, contentView) - offset).coerceAtLeast(pluginsStart + 1)

            val sectionStart: Int
            val sectionEnd: Int
            val activeTab: TextView?
            val sectionName: String

            if (scrollY < themeStart) {
                sectionStart = profileStart
                sectionEnd = themeStart
                activeTab = b.tabStickyProfile
                sectionName = "Профиль"
            } else if (scrollY < chatStart) {
                sectionStart = themeStart
                sectionEnd = chatStart
                activeTab = b.tabStickyTheme
                sectionName = "Оформление"
            } else if (scrollY < browserStart) {
                sectionStart = chatStart
                sectionEnd = browserStart
                activeTab = b.tabStickyChat
                sectionName = "В чате"
            } else if (scrollY < dataStart) {
                sectionStart = browserStart
                sectionEnd = dataStart
                activeTab = b.tabStickyBrowser
                sectionName = "Браузер"
            } else if (scrollY < pluginsStart) {
                sectionStart = dataStart
                sectionEnd = pluginsStart
                activeTab = b.tabStickyData
                sectionName = "Данные и память"
            } else if (scrollY < infoStart) {
                sectionStart = pluginsStart
                sectionEnd = infoStart
                activeTab = b.tabStickyPlugins
                sectionName = "Плагины"
            } else {
                sectionStart = infoStart
                sectionEnd = maxScroll
                activeTab = b.tabStickyInfo
                sectionName = "Информация"
            }

            val sectionHeight = (sectionEnd - sectionStart).coerceAtLeast(1)
            val progressPercent = (((scrollY - sectionStart).toFloat() / sectionHeight) * 100).toInt().coerceIn(0, 100)
            b.pbStickyScrollProgress.progress = progressPercent

            updateActiveTabUI(activeTab, sectionName)
        }

        var isToolbarVisible = false

        b.nestedScrollView.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateSectionProgressAndTabs()

            val header = b.layoutSettingsHeader
            if (!header.isAttachedToWindow) return@OnScrollChangeListener

            val locationHeader = IntArray(2)
            header.getLocationOnScreen(locationHeader)

            val locationScroll = IntArray(2)
            b.nestedScrollView.getLocationOnScreen(locationScroll)

            val headerRelativeTop = locationHeader[1] - locationScroll[1]
            val toolbarHeight = b.blurStickyToolbar.height.coerceAtLeast(1)

            val shouldShow = headerRelativeTop <= toolbarHeight + 12

            if (shouldShow != isToolbarVisible) {
                isToolbarVisible = shouldShow
                b.blurStickyToolbar.animate().cancel()
                if (shouldShow) {
                    b.blurStickyToolbar.visibility = View.VISIBLE
                    b.blurStickyToolbar.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .setListener(null)
                        .start()
                } else {
                    b.blurStickyToolbar.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                if (!isToolbarVisible) {
                                    b.blurStickyToolbar.visibility = View.GONE
                                }
                            }
                        })
                        .start()
                }
            }
        })

        b.nestedScrollView.post {
            updateSectionProgressAndTabs()
            val header = b.layoutSettingsHeader
            if (!header.isAttachedToWindow) return@post
            val locationHeader = IntArray(2)
            header.getLocationOnScreen(locationHeader)
            val locationScroll = IntArray(2)
            b.nestedScrollView.getLocationOnScreen(locationScroll)
            val headerRelativeTop = locationHeader[1] - locationScroll[1]
            val toolbarHeight = b.blurStickyToolbar.height.coerceAtLeast(1)
            isToolbarVisible = headerRelativeTop <= toolbarHeight + 12
            b.blurStickyToolbar.visibility = if (isToolbarVisible) View.VISIBLE else View.GONE
            b.blurStickyToolbar.alpha = if (isToolbarVisible) 1f else 0f
        }
    }

    private fun setupPluginsSection(b: ActivitySettingsContentBinding) {
        b.btnAddPlugin?.setOnClickListener {
            showAddPluginDialog()
        }
        try {
            ContextCompat.registerReceiver(
                this,
                pluginsReceiver,
                IntentFilter(PluginManager.ACTION_PLUGINS_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}
        refreshPluginsList()
    }

    private fun refreshPluginsList() {
        val b = binding ?: return
        val list = PluginManager.getAllPlugins(this)
        b.containerPluginsList?.removeAllViews()

        if (list.isEmpty()) {
            b.tvEmptyPluginsNotice?.visibility = View.VISIBLE
            b.containerPluginsList?.visibility = View.GONE
        } else {
            b.tvEmptyPluginsNotice?.visibility = View.GONE
            b.containerPluginsList?.visibility = View.VISIBLE

            list.forEach { plugin ->
                val row = layoutInflater.inflate(R.layout.item_plugin_row, b.containerPluginsList, false)
                val tvName = row.findViewById<TextView>(R.id.tvPluginName)
                val tvSubtitle = row.findViewById<TextView>(R.id.tvPluginSubtitle)
                val switchEnable = row.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchPluginEnable)
                val btnDelete = row.findViewById<View>(R.id.btnDeletePlugin)

                tvName.text = plugin.name
                val statusStr = if (plugin.isEnabled) "Активен" else "Отключен"
                tvSubtitle.text = "${plugin.fileType} • $statusStr"

                switchEnable.isChecked = plugin.isEnabled
                switchEnable.setOnCheckedChangeListener { _, isChecked ->
                    PluginManager.togglePlugin(this, plugin.id, isChecked)
                    refreshPluginsList()
                }

                btnDelete.setOnClickListener {
                    PrimeBlurDialog.show(
                        activity = this,
                        title = "Удаление плагина",
                        message = "Вы уверены, что хотите удалить плагин ${plugin.name}?",
                        positiveText = "Удалить",
                        negativeText = "Отмена",
                        isPositiveDanger = true,
                        onPositive = {
                            PluginManager.deletePlugin(this, plugin.id)
                            refreshPluginsList()
                            PrimeNotification.show(this, "Плагин удален")
                        }
                    )
                }

                row.setOnClickListener {
                    showPluginCodeViewerDialog(plugin)
                }

                b.containerPluginsList?.addView(row)
            }
        }
    }

    private fun showAddPluginDialog() {
        PrimeBlurDialog.showList(
            activity = this,
            title = "Добавить новый плагин",
            items = arrayOf("Из файла с устройства", "Создать файл вручную (Код)"),
            iconRes = R.drawable.ic_add,
            onItemSelected = { index, _ ->
                if (index == 0) {
                    try {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        pluginFilePickerLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Не удалось открыть выбор файлов", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showManualPluginCreationDialog()
                }
            }
        )
    }

    private fun showManualPluginCreationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val tvLabelName = TextView(this).apply {
            text = "Название плагина / файла:"
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }
        val etName = EditText(this).apply {
            hint = "CustomPlugin.java"
            setSingleLine()
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
        }

        val tvLabelLang = TextView(this).apply {
            text = "Язык программирования / Тип:"
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }
        val languages = arrayOf("Java (.java)", "Kotlin (.kt)", "Python (.py)", "JavaScript (.js)", "HTML (.html)", "Shell (.sh)", "JSON (.json)")
        val spinnerLang = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, languages)
        }

        val tvLabelCode = TextView(this).apply {
            text = "Код / Содержимое плагина:"
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }
        val etCode = EditText(this).apply {
            hint = "// Введите исходный код или инструкции..."
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            minLines = 6
            maxLines = 10
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
        }

        layout.addView(tvLabelName)
        layout.addView(etName)
        layout.addView(tvLabelLang)
        layout.addView(spinnerLang)
        layout.addView(tvLabelCode)
        layout.addView(etCode)

        PrimeBlurDialog.show(
            activity = this,
            title = "Создание плагина",
            message = "Выберите язык программирования и введите исходный код.",
            positiveText = "Создать",
            negativeText = "Отмена",
            iconRes = R.drawable.ic_add,
            customView = layout,
            onPositive = {
                val nameInput = etName.text.toString().trim()
                val langIndex = spinnerLang.selectedItemPosition
                val ext = when (langIndex) {
                    1 -> "Kotlin"
                    2 -> "Python"
                    3 -> "JS"
                    4 -> "HTML"
                    5 -> "Shell"
                    6 -> "JSON"
                    else -> "Java"
                }
                val defaultExt = when (ext) {
                    "Kotlin" -> ".kt"
                    "Python" -> ".py"
                    "JS" -> ".js"
                    "HTML" -> ".html"
                    "Shell" -> ".sh"
                    "JSON" -> ".json"
                    else -> ".java"
                }
                val name = if (nameInput.contains(".")) nameInput else if (nameInput.isNotBlank()) "$nameInput$defaultExt" else "Plugin$defaultExt"
                val code = etCode.text.toString().trim()
                val plugin = PluginManager.createPluginFromCode(this, name, code, ext)
                refreshPluginsList()
                PrimeNotification.show(this, "Плагин ${plugin.name} создан!")
            }
        )
    }

    private fun showPluginCodeViewerDialog(plugin: PluginItem) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val etCode = EditText(this).apply {
            setText(plugin.codeContent)
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            minLines = 8
            maxLines = 14
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
        }

        layout.addView(etCode)

        PrimeBlurDialog.show(
            activity = this,
            title = "Код: ${plugin.name}",
            message = "Исходный код плагина (${plugin.fileType}):",
            positiveText = "Сохранить и Запустить",
            negativeText = "Закрыть",
            iconRes = R.drawable.ic_file,
            customView = layout,
            onPositive = {
                val newCode = etCode.text.toString().trim()
                plugin.codeContent = newCode
                PluginManager.savePlugin(this, plugin)
                refreshPluginsList()
                val execResult = PluginExecutor.executePlugin(this, plugin)
                PrimeNotification.show(this, execResult.take(120))
            }
        )
    }

    private fun setupMaterial3ExpressiveDesign(b: ActivitySettingsContentBinding) {
        val density = resources.displayMetrics.density
        val expressRadiusPx = 28f * density

        try {
            val cardsToStyle = arrayOf(
                b.cardTheme,
                b.cardNavStyleSection,
                b.cardColorAccent,
                b.cardLavaBg,
                b.cardChatAppearance,
                b.cardWallpaperSection,
                b.cardChatStyleSection,
                b.cardVideoNotesSection,
                b.cardThanks,
                b.cardGithub,
                b.cardThemeSystem,
                b.cardThemeLight,
                b.cardThemeDark
            )

            cardsToStyle.forEach { card ->
                card?.let {
                    if (it is com.google.android.material.card.MaterialCardView) {
                        it.radius = expressRadiusPx
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsM3", "Error applying M3 Expressive design", e)
        }
    }

    private fun setupListeners(b: ActivitySettingsContentBinding) {
        setupMaterial3ExpressiveDesign(b)
        b.btnBackNP.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        
        b.btnChangePhotoNP.setOnClickListener { showAvatarChoiceDialog() }

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
        b.cardGithub.addBounceTouchEffect()
        b.cardThanks.addBounceTouchEffect()

        // Color Accent Card - Inline Options
        b.tvAccentSummary.text = ColorAccentManager.getAccentSummary(this)
        
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        b.vAccentColorPreview.backgroundTintList = ColorStateList.valueOf(accentColor)

        val updateResetAccentVisibility = {
            b.btnResetAccent?.visibility = if (ColorAccentManager.getAccentType(this) != ColorAccentManager.ACCENT_TYPE_DEFAULT) View.VISIBLE else View.GONE
        }
        updateResetAccentVisibility()

        b.btnResetAccent?.setOnClickListener {
            ColorAccentManager.setAccentDefault(this)
            onAccentColorChanged(b)
            updateResetAccentVisibility()
        }
        
        // Setup Monet Switch
        b.switchMonetAccent.setOnCheckedChangeListener(null)
        b.switchMonetAccent.isChecked = (ColorAccentManager.getAccentType(this) == ColorAccentManager.ACCENT_TYPE_SYSTEM)
        b.switchMonetAccent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ColorAccentManager.setAccentSystem(this)
            } else if (ColorAccentManager.getAccentType(this) == ColorAccentManager.ACCENT_TYPE_SYSTEM) {
                ColorAccentManager.setAccentDefault(this)
            }
            onAccentColorChanged(b)
            updateResetAccentVisibility()
        }

        // Setup Swatches
        setupSwatches(b)

        b.btnCustomPicker.setOnClickListener {
            showCustomColorSpectrumPicker(b)
        }

        // Theme Options Cards
        val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
        updateThemeSelectionUI(b, currentTheme)
        updateThemeChatPreview(b, currentTheme)

        val onThemeSelected = { newTheme: String ->
            updateThemeSelectionUI(b, newTheme)
            updateThemeChatPreview(b, newTheme)
            applyThemeChange(newTheme)
        }

        b.itemThemeSystem.setOnClickListener { onThemeSelected("system") }
        b.itemThemeLight.setOnClickListener { onThemeSelected("light") }
        b.itemThemeDark.setOnClickListener { onThemeSelected("dark") }

        // Lava Background Preview
        val updateLavaPreview = { isChecked: Boolean ->
            b.composeLavaPreview.setContent {
                PrimeTheme(darkTheme = isDark) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedBackground(darkTheme = isDark, ignoreSettingsToggle = false)
                    }
                }
            }
        }

        b.switchLavaBg.setOnCheckedChangeListener(null)
        val isLavaOn = sharedPrefs.getBoolean("settings_lava_bg", true)
        b.switchLavaBg.isChecked = isLavaOn
        b.btnResetLava?.visibility = if (!isLavaOn) View.VISIBLE else View.GONE
        b.btnResetLava?.setOnClickListener {
            b.switchLavaBg.isChecked = true
        }

        b.switchLavaBg.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_lava_bg", isChecked).apply()
            updateLavaPreview(isChecked)
            b.btnResetLava?.visibility = if (!isChecked) View.VISIBLE else View.GONE
            LavaBackgroundState.onActivityResumed()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            restartApp()
        }

        b.cardGithub.setOnClickListener {
            com.messenger.prime.browser.AppBrowserManager.openUrl(this, "https://github.com/Vaniahorosh/Prime_messenger")
        }

        setupNavStyleSection(b)

        b.root.post {
            if (!isFinishing && !isDestroyed) {
                if (isLavaOn) {
                    updateLavaPreview(true)
                }
                setupThanksCard(b)
                setupStorageSection(b)
                setupBrowserSection(b)
                setupWallpaperSection(b)
                setupBubbleRadiusSection(b)
                setupAvatarRadiusSection(b)
                setupChatStyleSection(b)
                setupPartnerInfoSection(b)
                setupM3EShapesChatSection(b)
                setupBottomBarRadiusSection(b)
                setupVideoMessageSection(b)
                setupDisableAnimationsSection(b)
            }
        }
    }

    private fun setupBrowserSection(b: ActivitySettingsContentBinding) {
        val context = this

        // 1. Switch Use Internal Browser
        b.switchUseInternalBrowser.isChecked = com.messenger.prime.browser.BrowserPreferences.isInternalBrowserEnabled(context)
        b.switchUseInternalBrowser.setOnCheckedChangeListener { _, isChecked ->
            com.messenger.prime.browser.BrowserPreferences.setInternalBrowserEnabled(context, isChecked)
        }

        // 2. Search Engine Spinner
        val searchEngines = arrayOf("Google", "Яндекс", "DuckDuckGo", "Bing")
        val searchEngineAdapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, searchEngines)
        searchEngineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerSearchEngine.adapter = searchEngineAdapter

        val currentEngine = com.messenger.prime.browser.BrowserPreferences.getSearchEngine(context)
        val selectedIndex = searchEngines.indexOf(currentEngine).coerceAtLeast(0)
        b.spinnerSearchEngine.setSelection(selectedIndex)

        b.spinnerSearchEngine.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val current = searchEngines.indexOf(com.messenger.prime.browser.BrowserPreferences.getSearchEngine(context)).coerceAtLeast(0)
                val menu = PrimePopupMenu.create(context, v)
                searchEngines.forEachIndexed { index, title ->
                    menu.addItem(title, isSelected = (index == current)) {
                        b.spinnerSearchEngine.setSelection(index)
                        com.messenger.prime.browser.BrowserPreferences.setSearchEngine(context, title)
                    }
                }
                menu.show()
            }
            true
        }

        // 3. Switch JS
        b.switchBrowserJs.isChecked = com.messenger.prime.browser.BrowserPreferences.isJavaScriptEnabled(context)
        b.switchBrowserJs.setOnCheckedChangeListener { _, isChecked ->
            com.messenger.prime.browser.BrowserPreferences.setJavaScriptEnabled(context, isChecked)
        }

        // 4. Switch Cookies & Cache
        b.switchBrowserCookies.isChecked = com.messenger.prime.browser.BrowserPreferences.areCookiesEnabled(context)
        b.switchBrowserCookies.setOnCheckedChangeListener { _, isChecked ->
            com.messenger.prime.browser.BrowserPreferences.setCookiesEnabled(context, isChecked)
        }

        // 5. Switch Desktop Mode default
        b.switchBrowserDesktopMode.isChecked = com.messenger.prime.browser.BrowserPreferences.isDesktopModeEnabled(context)
        b.switchBrowserDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            com.messenger.prime.browser.BrowserPreferences.setDesktopModeEnabled(context, isChecked)
        }

        // 6. Clear Browser Cache
        b.layoutClearBrowserCache.setOnClickListener {
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebStorage.getInstance().deleteAllData()
                android.widget.Toast.makeText(context, "Кэш и данные браузера очищены", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Ошибка при очистке кэша", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupChatStyleSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentStyle = sharedPrefs.getString("settings_chat_header_style", "block") ?: "block"
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val isDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        b.vPreviewTopFog.setBackgroundResource(if (isDark) R.drawable.bg_top_fog_dark else R.drawable.bg_top_fog_light)

        val updateChatStyleUi = { style: String ->
            currentStyle = style
            val isBlock = style == "block"

            val activeColorStateList = ColorStateList.valueOf(accentColor)
            val transparentColorStateList = ColorStateList.valueOf(Color.TRANSPARENT)

            b.btnChatStyleBlock.backgroundTintList = if (isBlock) activeColorStateList else transparentColorStateList
            b.btnChatStyleBlock.setTextColor(if (isBlock) Color.WHITE else if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B"))

            b.btnChatStyleNoBlock.backgroundTintList = if (!isBlock) activeColorStateList else transparentColorStateList
            b.btnChatStyleNoBlock.setTextColor(if (!isBlock) Color.WHITE else if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B"))

            // Update live preview
            if (isBlock) {
                b.cardHeaderPreviewIsland.setCardBackgroundColor(if (isDark) Color.parseColor("#1E293B") else Color.WHITE)
                b.cardHeaderPreviewIsland.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                b.cardHeaderPreviewIsland.strokeColor = accentColor
                b.cardHeaderPreviewIsland.cardElevation = 2 * resources.displayMetrics.density
            } else {
                b.cardHeaderPreviewIsland.setCardBackgroundColor(Color.TRANSPARENT)
                b.cardHeaderPreviewIsland.strokeWidth = 0
                b.cardHeaderPreviewIsland.cardElevation = 0f
            }

            // Enable or disable bottom bar radius slider based on whether "block" mode is selected
            b.tvTitleBottomBarRadius?.alpha = if (isBlock) 1.0f else 0.4f
            b.sliderBottomBarRadius?.isEnabled = isBlock
            b.sliderBottomBarRadius?.alpha = if (isBlock) 1.0f else 0.4f
            b.cardBottomBarPreviewBlock?.alpha = if (isBlock) 1.0f else 0.4f
        }

        updateChatStyleUi(currentStyle)

        b.btnChatStyleBlock.setOnClickListener {
            if (currentStyle != "block") {
                sharedPrefs.edit().putString("settings_chat_header_style", "block").apply()
                updateChatStyleUi("block")
                sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
            }
        }

        b.btnChatStyleNoBlock.setOnClickListener {
            if (currentStyle != "no_block") {
                sharedPrefs.edit().putString("settings_chat_header_style", "no_block").apply()
                updateChatStyleUi("no_block")
                sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
            }
        }
    }

    private fun setupPartnerInfoSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("settings_partner_info_enabled", true)
        b.switchPartnerInfo.isChecked = isEnabled

        b.switchPartnerInfo.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_partner_info_enabled", isChecked).apply()
        }
    }

    private fun setupM3EShapesChatSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("m3e_shapes_chat_enabled", true)
        b.switchM3EShapesChat?.isChecked = isEnabled

        b.switchM3EShapesChat?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("m3e_shapes_chat_enabled", isChecked).apply()
        }
    }

    private fun setupBottomBarRadiusSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentRadius = sharedPrefs.getInt("settings_chat_ui_radius_dp", sharedPrefs.getInt("settings_bottom_bar_radius_dp", 24))
        val density = resources.displayMetrics.density

        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val isDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        b.layoutBottomBarPreviewBg?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(if (isDark) android.graphics.Color.parseColor("#151D2A") else android.graphics.Color.parseColor("#F1F5F9"))
        }
        b.cardBottomBarPreviewBlock?.strokeColor = if (isDark) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#1A000000")

        val updatePreview = { radiusDp: Int ->
            val percent = Math.round((radiusDp / 36f) * 100)
            b.tvBottomBarRadiusValue?.text = "$percent%"
            val radiusPx = radiusDp * density
            b.cardHeaderPreviewInput?.radius = radiusPx
            b.cardBottomBarPreviewInput?.radius = radiusPx
            val accentColor = ColorAccentManager.getCurrentAccentColor(this)
            b.cardHeaderPreviewInput?.strokeColor = accentColor
            b.cardBottomBarPreviewInput?.strokeColor = accentColor
        }

        val btnReset = b.root.findViewById<ImageView>(R.id.btnResetBottomBarRadius)
        val updateResetVisibility = { radiusDp: Int ->
            btnReset?.visibility = if (radiusDp != 24) View.VISIBLE else View.GONE
        }

        updatePreview(currentRadius)
        b.sliderBottomBarRadius?.value = currentRadius.coerceIn(0, 36).toFloat()
        updateResetVisibility(currentRadius)

        btnReset?.setOnClickListener {
            sharedPrefs.edit()
                .putInt("settings_chat_ui_radius_dp", 24)
                .putInt("settings_bottom_bar_radius_dp", 24)
                .putInt("settings_top_bar_radius_dp", 24)
                .apply()
            b.sliderBottomBarRadius?.value = 24f
            updatePreview(24)
            updateResetVisibility(24)
            setupBottomNav(b)
            sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.sliderBottomBarRadius?.addOnChangeListener { _, value, fromUser ->
            val radiusInt = value.toInt()
            updatePreview(radiusInt)
            updateResetVisibility(radiusInt)
            if (fromUser) {
                sharedPrefs.edit()
                    .putInt("settings_chat_ui_radius_dp", radiusInt)
                    .putInt("settings_bottom_bar_radius_dp", radiusInt)
                    .putInt("settings_top_bar_radius_dp", radiusInt)
                    .apply()
                setupBottomNav(b)
                sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        }
    }

    private fun setupDisableAnimationsSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val isDisabled = sharedPrefs.getBoolean("disable_animations", false)
        b.switchDisableAnimations?.isChecked = isDisabled

        b.switchDisableAnimations?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("disable_animations", isChecked).apply()
            PrimeTransitions.applyGlobalAnimationScale(this)
            sendBroadcast(Intent("com.messenger.prime.ANIMATIONS_TOGGLED").setPackage(packageName))
        }
    }

    private fun setupNavStyleSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentNavStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"

        val layoutNavOrder = b.root.findViewById<LinearLayout>(R.id.layoutNavOrder)
        val layoutNavIslandConfig = b.root.findViewById<LinearLayout>(R.id.layoutNavIslandConfig)
        
        val btnResetNavStyle = b.root.findViewById<ImageView>(R.id.btnResetNavStyle)
        
        btnResetNavStyle?.setOnClickListener {
            sharedPrefs.edit().remove("navigation_style").apply()
            currentNavStyle = "island"
            b.rbStyleIsland.performClick()
        }
        
        val updateNavStyleUi = { animate: Boolean ->
            val isIsland = currentNavStyle == "island"
            b.rbStyleIsland.isChecked = isIsland
            b.rbStyleBottomBar.isChecked = !isIsland

            btnResetNavStyle?.visibility = if (!isIsland) View.VISIBLE else View.GONE

            if (animate) {
                TransitionManager.beginDelayedTransition(b.cardNavStyleSection, android.transition.AutoTransition().apply { duration = 200 })
            }
            if (layoutNavOrder != null) {
                layoutNavOrder.visibility = if (!isIsland) View.VISIBLE else View.GONE
            }
            if (layoutNavIslandConfig != null) {
                layoutNavIslandConfig.visibility = if (isIsland) View.VISIBLE else View.GONE
            }

            val accentColor = ColorAccentManager.getCurrentAccentColor(this)
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val radioTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    accentColor,
                    if (isNight) android.graphics.Color.parseColor("#66FFFFFF") else android.graphics.Color.parseColor("#66000000")
                )
            )
            b.rbStyleIsland.buttonTintList = radioTintList
            b.rbStyleBottomBar.buttonTintList = radioTintList

            b.rbStyleIsland.setTextColor(if (isIsland) accentColor else ContextCompat.getColor(this, R.color.prime_text_primary))
            b.rbStyleBottomBar.setTextColor(if (!isIsland) accentColor else ContextCompat.getColor(this, R.color.prime_text_primary))

            b.cardStyleIsland.strokeColor = if (isIsland) accentColor else android.graphics.Color.TRANSPARENT
            b.cardStyleIsland.strokeWidth = if (isIsland) (2 * resources.displayMetrics.density).toInt() else 0

            b.cardStyleBottomBar.strokeColor = if (!isIsland) accentColor else android.graphics.Color.TRANSPARENT
            b.cardStyleBottomBar.strokeWidth = if (!isIsland) (2 * resources.displayMetrics.density).toInt() else 0

            setupBottomNav(b)
        }

        b.switchAiContactIsland?.setOnCheckedChangeListener(null)
        b.switchAiContactIsland?.isChecked = sharedPrefs.getBoolean("ai_chat_contact_enabled", true)
        b.switchAiContactIsland?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("ai_chat_contact_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        val switchNavShowLabels = b.root.findViewWithTag<com.google.android.material.materialswitch.MaterialSwitch>("switchNavShowLabels")
        switchNavShowLabels?.setOnCheckedChangeListener(null)
        switchNavShowLabels?.isChecked = sharedPrefs.getBoolean("nav_show_labels", false)
        switchNavShowLabels?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("nav_show_labels", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        val switchCategoryChips = b.root.findViewWithTag<com.google.android.material.materialswitch.MaterialSwitch>("switchCategoryChips")
        val switchNotesChat = b.root.findViewWithTag<com.google.android.material.materialswitch.MaterialSwitch>("switchNotesChat")
        val btnAddCustomCategory = b.root.findViewWithTag<com.google.android.material.button.MaterialButton>("btnAddCustomCategory")

        switchCategoryChips?.setOnCheckedChangeListener(null)
        switchCategoryChips?.isChecked = sharedPrefs.getBoolean("category_chips_enabled", true)
        switchCategoryChips?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("category_chips_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        switchNotesChat?.setOnCheckedChangeListener(null)
        switchNotesChat?.isChecked = sharedPrefs.getBoolean("notes_chat_enabled", true)
        switchNotesChat?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notes_chat_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.switchIslandPullToSettings?.setOnCheckedChangeListener(null)
        b.switchIslandPullToSettings?.isChecked = sharedPrefs.getBoolean("island_pull_to_settings_enabled", true)
        b.switchIslandPullToSettings?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("island_pull_to_settings_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        btnAddCustomCategory?.setOnClickListener {
            showAddCustomCategoryDialog()
        }
        renderCategoriesList()

        updateNavStyleUi(false)

        val setStyle = { style: String ->
            if (currentNavStyle != style) {
                currentNavStyle = style
                sharedPrefs.edit().putString("navigation_style", style).apply()
                updateNavStyleUi(true)
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        }

        b.cardStyleIsland.setOnClickListener { setStyle("island") }
        b.rbStyleIsland.setOnClickListener { setStyle("island") }

        b.cardStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }
        b.rbStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }
        
        setupNavOrderSection(b)
    }

    private fun setupNavOrderSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val btnResetNavOrder = b.root.findViewById<ImageView>(R.id.btnResetNavOrder)
        
        btnResetNavOrder?.setOnClickListener {
            sharedPrefs.edit()
                .remove("nav_buttons_order")
                .remove("nav_button_chats_visible")
                .remove("nav_button_devices_visible")
                .remove("nav_button_ai_visible")
                .remove("nav_button_search_visible")
                .apply()
            setupNavOrderSection(b)
            setupBottomNav(b)
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }
        
        val defaultOrder = "chats,devices,ai,search,downloads,profile"
        val currentOrderStr = sharedPrefs.getString("nav_buttons_order", defaultOrder) ?: defaultOrder
        val currentOrder = currentOrderStr.split(",").filter { it.isNotEmpty() }

        val isChanged = currentOrderStr != defaultOrder ||
                !sharedPrefs.getBoolean("nav_button_chats_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_devices_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_ai_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_search_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_downloads_visible", true)
        btnResetNavOrder?.visibility = if (isChanged) View.VISIBLE else View.GONE
        
        val buttonConfigs = mutableListOf<BottomBarButtonConfig>()
        val allIds = listOf("chats", "devices", "ai", "search", "downloads", "profile")
        val orderToUse = if (currentOrder.containsAll(allIds)) currentOrder else defaultOrder.split(",")
        
        for (id in orderToUse) {
            when (id) {
                "chats" -> buttonConfigs.add(BottomBarButtonConfig("chats", "Чаты", R.drawable.ic_nav_chats, sharedPrefs.getBoolean("nav_button_chats_visible", true)))
                "devices" -> buttonConfigs.add(BottomBarButtonConfig("devices", "Устройства", R.drawable.ic_nav_devices, sharedPrefs.getBoolean("nav_button_devices_visible", true)))
                "ai" -> buttonConfigs.add(BottomBarButtonConfig("ai", "AI Чат", R.drawable.ic_prime_ai, sharedPrefs.getBoolean("nav_button_ai_visible", true)))
                "search" -> buttonConfigs.add(BottomBarButtonConfig("search", "Поиск", R.drawable.ic_search, sharedPrefs.getBoolean("nav_button_search_visible", true)))
                "downloads" -> buttonConfigs.add(BottomBarButtonConfig("downloads", "Загрузки", R.drawable.ic_download, sharedPrefs.getBoolean("nav_button_downloads_visible", true)))
                "profile" -> buttonConfigs.add(BottomBarButtonConfig("profile", "Профиль", R.drawable.ic_person, true, isMandatory = true))
            }
        }
        
        val adapter = NavButtonsAdapter(buttonConfigs) {
            val newOrder = buttonConfigs.joinToString(",") { it.id }
            val editor = sharedPrefs.edit()
            editor.putString("nav_buttons_order", newOrder)
            buttonConfigs.forEach {
                if (it.id != "profile") {
                    editor.putBoolean("nav_button_${it.id}_visible", it.isVisible)
                }
            }
            editor.apply()
            setupBottomNav(b)
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }
        
        val rvNavButtons = b.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvNavButtons)
        rvNavButtons?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvNavButtons?.adapter = adapter
        
        val touchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(rvNavButtons)
    }

    private fun updateNavUnreadBadge(navView: View) {
        val tvNavChatsUnreadBadge = navView.findViewById<TextView>(R.id.tvNavChatsUnreadBadge) ?: return
        var totalUnread = 0
        try {
            val json = ChatHistoryManager.getPersistedChatsJson(this)
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
        accentColor: Int
    ) {
        if (indicatorView == null) return
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val inactiveColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B")
        val activeIconColor = if (isDark) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE

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

            iconView?.setColorFilter(activeIconColor)
            iconView?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(180)?.start()
        } else {
            indicatorView.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            indicatorView.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setDuration(180)
                .start()

            iconView?.setColorFilter(accentColor)
            iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(180)?.start()
        }
    }

    private fun setupBottomNav(b: ActivitySettingsContentBinding? = binding) {
        val bindingRef = b ?: binding
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"
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
        val btnNavAi = navView.findViewById<View>(R.id.btnNavAi)
        val btnNavSearch = navView.findViewById<View>(R.id.btnNavSearch)
        val btnNavDownloads = navView.findViewById<View>(R.id.btnNavDownloads)
        val btnNavProfile = navView.findViewById<View>(R.id.btnNavProfile)

        val navContainer = (navView as? ViewGroup)?.getChildAt(0) as? ViewGroup
        if (navContainer != null) {
            val defaultOrder = "chats,devices,ai,search,downloads,profile"
            val currentOrderStr = sharedPrefs.getString("nav_buttons_order", defaultOrder) ?: defaultOrder
            val currentOrder = currentOrderStr.split(",").filter { it.isNotEmpty() }

            navContainer.removeAllViews()

            val allNavIds = listOf("chats", "devices", "ai", "search", "downloads", "profile")
            val orderToUse = if (currentOrder.containsAll(allNavIds)) currentOrder else defaultOrder.split(",")

            for (id in orderToUse) {
                val view = when (id) {
                    "chats" -> btnNavChats
                    "devices" -> btnNavDevices
                    "ai" -> btnNavAi
                    "search" -> btnNavSearch
                    "downloads" -> btnNavDownloads
                    "profile" -> btnNavProfile
                    else -> null
                }
                view?.let {
                    val isVisible = sharedPrefs.getBoolean("nav_button_${id}_visible", true)
                    it.visibility = if (isVisible || id == "profile") View.VISIBLE else View.GONE
                    navContainer.addView(it)
                }
            }
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        val vNavChatsIndicator = navView.findViewById<View>(R.id.vNavChatsIndicator)
        val vNavDevicesIndicator = navView.findViewById<View>(R.id.vNavDevicesIndicator)
        val vNavAiIndicator = navView.findViewById<View>(R.id.vNavAiIndicator)
        val vNavSearchIndicator = navView.findViewById<View>(R.id.vNavSearchIndicator)
        val vNavDownloadsIndicator = navView.findViewById<View>(R.id.vNavDownloadsIndicator)
        val vNavProfileIndicator = navView.findViewById<View>(R.id.vNavProfileIndicator)

        val ivNavChatsIcon = navView.findViewById<ImageView>(R.id.ivNavChatsIcon)
        val ivNavDevicesIcon = navView.findViewById<ImageView>(R.id.ivNavDevicesIcon)
        val ivNavAiIcon = navView.findViewById<ImageView>(R.id.ivNavAiIcon)
        val ivNavSearchIcon = navView.findViewById<ImageView>(R.id.ivNavSearchIcon)
        val ivNavDownloadsIcon = navView.findViewById<ImageView>(R.id.ivNavDownloadsIcon)

        applyBottomNavLabelsVisibility(navView, sharedPrefs.getBoolean("nav_show_labels", false))

        updateM3TabState(false, vNavChatsIndicator, ivNavChatsIcon, accentColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, accentColor)
        updateM3TabState(false, vNavAiIndicator, ivNavAiIcon, accentColor)
        updateM3TabState(false, vNavSearchIndicator, ivNavSearchIcon, accentColor)
        updateM3TabState(false, vNavDownloadsIndicator, ivNavDownloadsIcon, accentColor)
        updateM3TabState(true, vNavProfileIndicator, null, accentColor)
        updateNavUnreadBadge(navView)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val myAvatar = AvatarManager.getMyAvatarUri(this)
            val myName = AvatarManager.getMyDisplayName(this)
            loadAvatarIntoView(this, myAvatar, profileAvatarIv, myName)
        }

        val blurNavView = navView as? eightbitlab.com.blurview.BlurView
        if (blurNavView != null) {
            val overlayColor = com.messenger.prime.getDefaultBlurOverlayColor(this)
            val targetRootView = (bindingRef?.root as? ViewGroup)
                ?: (window.decorView.findViewById<ViewGroup>(android.R.id.content))
                ?: (window.decorView as ViewGroup)
            blurNavView.setupBlur(targetRootView, 18f, overlayColor, window.decorView.background)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                blurNavView.applyM3EPathProvider(com.messenger.prime.shapes.M3EShapes::pill)
            }
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

        btnNavAi?.setOnClickListener {
            val intent = Intent(this, AiChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
            finish()
        }

        btnNavSearch?.setOnClickListener {
            val intent = Intent(this, GlobalSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = false)
            finish()
        }

        btnNavDownloads?.setOnClickListener {
            DownloadsActivity.openWithPermissionCheck(this)
        }

        btnNavProfile?.setOnClickListener {
            binding?.nestedScrollView?.smoothScrollTo(0, 0)
        }
    }

    private var wallpaperInDeleteModeId: String? = null

    private val wallpaperEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding?.let { setupWallpaperSection(it) }
            sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
        }
    }

    private fun setupWallpaperSection(b: ActivitySettingsContentBinding) {
        val density = resources.displayMetrics.density
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val wallpapers = WallpaperManager.getWallpapersList(this)
        val activeWp = WallpaperManager.getActiveWallpaper(this)

        b.tvWallpaperSummary.text = activeWp.name
        b.layoutWallpaperOptions.removeAllViews()

        for (wp in wallpapers) {
            val isActive = (wp.id == activeWp.id)
            val isDeleteMode = (wallpaperInDeleteModeId == wp.id && wp.type != "DEFAULT")

            val itemContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams((88 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, (8 * density).toInt(), 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                isClickable = true
                isFocusable = true
                background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_chat_item_light)
                setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
            }

            // Phone Screen Card
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams((76 * density).toInt(), (138 * density).toInt()).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                radius = 18 * density
                cardElevation = 0f
                strokeWidth = if (isActive) (3 * density).toInt() else (1 * density).toInt()
                strokeColor = if (isActive) accentColor else if (isNight) Color.parseColor("#33FFFFFF") else Color.parseColor("#1A000000")
            }

            val cardFrame = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }

            if (wp.type == "IMAGE" && !wp.imagePath.isNullOrEmpty()) {
                val iv = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                val f = File(wp.imagePath)
                if (f.exists()) {
                    Glide.with(this).load(f).into(iv)
                }
                cardFrame.addView(iv)

                if (wp.dimOpacity > 0f) {
                    val dimView = View(this).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(Color.BLACK)
                        alpha = wp.dimOpacity
                    }
                    cardFrame.addView(dimView)
                }
            } else if (wp.type == "PATTERN") {
                val emojis = wp.customEmojis.let {
                    if (it.isNullOrBlank()) "😎 ⭐ 🌸 🔷" else it
                }
                val parts = emojis.split(Regex("\\s+")).filter { it.isNotBlank() }
                val line1 = parts.joinToString(" ")
                val line2 = parts.reversed().joinToString(" ")
                val line3 = if (parts.size > 2) parts.shuffled().joinToString(" ") else line1
                val patternText = "$line1\n$line2\n$line3"

                val tv = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(if (isNight) Color.parseColor("#0F172A") else Color.parseColor("#F8FAFC"))
                    gravity = android.view.Gravity.CENTER
                    text = patternText
                    textSize = 10f
                    setTextColor(if (isNight) Color.parseColor("#B0FFFFFF") else Color.parseColor("#80000000"))
                }
                cardFrame.addView(tv)
            } else if (wp.type == "MATERIAL") {
                val matView = MaterialShapesWallpaperView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
                cardFrame.addView(matView)
            } else {
                // DEFAULT
                val tv = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(if (isNight) Color.parseColor("#151D2A") else Color.parseColor("#F1F5F9"))
                    gravity = android.view.Gravity.CENTER
                    text = "Прайм\nОбои"
                    textSize = 11f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                }
                cardFrame.addView(tv)
            }

            // Camera Notch
            val notch = View(this).apply {
                layoutParams = FrameLayout.LayoutParams((16 * density).toInt(), (4 * density).toInt()).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    topMargin = (5 * density).toInt()
                }
                background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_m3_active_indicator)
                backgroundTintList = ColorStateList.valueOf(if (isNight) Color.parseColor("#40FFFFFF") else Color.parseColor("#30000000"))
            }
            cardFrame.addView(notch)

            card.addView(cardFrame)
            itemContainer.addView(card)

            // Label
            val labelTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (20 * density).toInt()).apply {
                    topMargin = (8 * density).toInt()
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                gravity = android.view.Gravity.CENTER
                text = wp.name
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isSingleLine = true
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
            }
            itemContainer.addView(labelTv)

            // Bottom Indicator
            if (isDeleteMode) {
                val deleteIcon = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                        topMargin = (4 * density).toInt()
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    setImageResource(R.drawable.ic_cancel)
                    imageTintList = ColorStateList.valueOf(Color.parseColor("#FF5252"))
                    isClickable = true
                    setOnClickListener {
                        WallpaperManager.deleteCustomWallpaper(this@SettingsActivity, wp.id)
                        wallpaperInDeleteModeId = null
                        setupWallpaperSection(b)
                        sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
                    }
                }
                itemContainer.addView(deleteIcon)
            } else {
                val radioBtn = RadioButton(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = (4 * density).toInt()
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    minWidth = 0
                    minHeight = 0
                    setPadding(0, 0, 0, 0)
                    isChecked = isActive
                    isClickable = false
                    isFocusable = false
                    val radioTint = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                        intArrayOf(accentColor, if (isNight) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000"))
                    )
                    buttonTintList = radioTint
                }
                itemContainer.addView(radioBtn)
            }

            // Click & Long Click Listeners
            itemContainer.setOnClickListener {
                if (wallpaperInDeleteModeId != null) {
                    wallpaperInDeleteModeId = null
                    setupWallpaperSection(b)
                } else {
                    WallpaperManager.setActiveWallpaper(this, wp.id)
                    setupWallpaperSection(b)
                    sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
                }
            }

            if (wp.type != "DEFAULT") {
                itemContainer.setOnLongClickListener {
                    wallpaperInDeleteModeId = if (wallpaperInDeleteModeId == wp.id) null else wp.id
                    setupWallpaperSection(b)
                    true
                }
            }

            b.layoutWallpaperOptions.addView(itemContainer)
        }

        // Add Wallpaper Button (+)
        val addContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((88 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_chat_item_light)
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())

            setOnClickListener {
                val intent = Intent(this@SettingsActivity, WallpaperEditorActivity::class.java)
                wallpaperEditorLauncher.launch(intent)
                PrimeTransitions.applyOpenTransition(this@SettingsActivity)
            }
        }

        val addCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams((76 * density).toInt(), (138 * density).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            radius = 18 * density
            cardElevation = 0f
            strokeWidth = (2 * density).toInt()
            strokeColor = accentColor
            setCardBackgroundColor(if (isNight) Color.parseColor("#151D2A") else Color.parseColor("#F1F5F9"))
        }

        val addFrame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val addPlusIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(R.drawable.ic_add)
            imageTintList = ColorStateList.valueOf(accentColor)
        }
        addFrame.addView(addPlusIcon)
        addCard.addView(addFrame)
        addContainer.addView(addCard)

        val addLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (20 * density).toInt()).apply {
                topMargin = (8 * density).toInt()
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            gravity = android.view.Gravity.CENTER
            text = "Добавить"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
        }
        addContainer.addView(addLabel)

        b.layoutWallpaperOptions.addView(addContainer)
    }

    private data class StorageBreakdown(
        var photosSize: Long = 0L,
        var photosCount: Int = 0,
        var videosSize: Long = 0L,
        var videosCount: Int = 0,
        var audioSize: Long = 0L,
        var audioCount: Int = 0,
        var gifsSize: Long = 0L,
        var gifsCount: Int = 0,
        var filesSize: Long = 0L,
        var filesCount: Int = 0,
        var avatarsSize: Long = 0L,
        var avatarsCount: Int = 0,
        var cacheSize: Long = 0L,
        var cacheCount: Int = 0,
        var totalSize: Long = 0L,
        var totalCount: Int = 0
    )

    private var currentStorageBreakdown: StorageBreakdown? = null

    private fun setupStorageSection(b: ActivitySettingsContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        b.btnClearStorage.backgroundTintList = accentTint
        b.cbStoragePhotos.buttonTintList = accentTint
        b.cbStorageVideos.buttonTintList = accentTint
        b.cbStorageAudio.buttonTintList = accentTint
        b.cbStorageGifs.buttonTintList = accentTint
        b.cbStorageFiles.buttonTintList = accentTint
        b.cbStorageAvatars.buttonTintList = accentTint
        b.cbStorageCache.buttonTintList = accentTint

        b.rowStoragePhotos.setOnClickListener {
            b.cbStoragePhotos.isChecked = !b.cbStoragePhotos.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageVideos.setOnClickListener {
            b.cbStorageVideos.isChecked = !b.cbStorageVideos.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageAudio.setOnClickListener {
            b.cbStorageAudio.isChecked = !b.cbStorageAudio.isChecked
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
        b.rowStorageAvatars.setOnClickListener {
            b.cbStorageAvatars.isChecked = !b.cbStorageAvatars.isChecked
            updateClearButtonText(b)
        }
        b.rowStorageCache.setOnClickListener {
            b.cbStorageCache.isChecked = !b.cbStorageCache.isChecked
            updateClearButtonText(b)
        }

        b.tvSelectAllStorage.setOnClickListener {
            val allChecked = b.cbStoragePhotos.isChecked &&
                    b.cbStorageVideos.isChecked &&
                    b.cbStorageAudio.isChecked &&
                    b.cbStorageGifs.isChecked &&
                    b.cbStorageFiles.isChecked &&
                    b.cbStorageAvatars.isChecked &&
                    b.cbStorageCache.isChecked

            val targetState = !allChecked
            b.cbStoragePhotos.isChecked = targetState
            b.cbStorageVideos.isChecked = targetState
            b.cbStorageAudio.isChecked = targetState
            b.cbStorageGifs.isChecked = targetState
            b.cbStorageFiles.isChecked = targetState
            b.cbStorageAvatars.isChecked = targetState
            b.cbStorageCache.isChecked = targetState

            b.tvSelectAllStorage.text = if (targetState) "Снять всё" else "Выбрать всё"
            updateClearButtonText(b)
        }

        val listener = CompoundButton.OnCheckedChangeListener { _, _ -> updateClearButtonText(b) }
        b.cbStoragePhotos.setOnCheckedChangeListener(listener)
        b.cbStorageVideos.setOnCheckedChangeListener(listener)
        b.cbStorageAudio.setOnCheckedChangeListener(listener)
        b.cbStorageGifs.setOnCheckedChangeListener(listener)
        b.cbStorageFiles.setOnCheckedChangeListener(listener)
        b.cbStorageAvatars.setOnCheckedChangeListener(listener)
        b.cbStorageCache.setOnCheckedChangeListener(listener)

        b.btnClearStorage.setOnClickListener {
            clearSelectedStorage(b)
        }

        calculateStorageAsync(b)
    }

    private fun calculateStorageAsync(b: ActivitySettingsContentBinding) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bd = StorageBreakdown()
            val allFiles = filesDir.walkTopDown().filter { it.isFile }.toList()

            for (f in allFiles) {
                val len = f.length()
                val name = f.name.lowercase(Locale.US)

                // Protect user profile assets, custom wallpapers, and database/json files
                if (name.startsWith("my_profile_avatar") || name.startsWith("my_avatar") || name.startsWith("wallpaper_img_") || name.endsWith(".json") || name.endsWith(".xml")) {
                    continue
                }

                if (name.startsWith("avatar_") || name.startsWith("rec_avatar_") || name.startsWith("tmp_contact_avatar_")) {
                    bd.avatarsSize += len
                    bd.avatarsCount++
                } else if (name.endsWith(".gif") || name.startsWith("tenor_")) {
                    bd.gifsSize += len
                    bd.gifsCount++
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi") || name.startsWith("rec_video_") || name.startsWith("rec_circle_") || name.startsWith("rec_videonote_") || f.parentFile?.name == "video_notes") {
                    bd.videosSize += len
                    bd.videosCount++
                } else if (name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".opus") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav") || name.startsWith("rec_voice_") || name.startsWith("audio_")) {
                    bd.audioSize += len
                    bd.audioCount++
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.startsWith("rec_photo_") || name.startsWith("cache_img_") || name.startsWith("cam_photo_")) {
                    bd.photosSize += len
                    bd.photosCount++
                } else if (name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log") || name.startsWith("cam_video_")) {
                    bd.cacheSize += len
                    bd.cacheCount++
                } else {
                    bd.filesSize += len
                    bd.filesCount++
                }
            }

            try {
                if (cacheDir != null && cacheDir.exists()) {
                    val cFiles = cacheDir.walkTopDown().filter { it.isFile }.toList()
                    for (cf in cFiles) {
                        bd.cacheSize += cf.length()
                        bd.cacheCount++
                    }
                }
                val extCache = externalCacheDir
                if (extCache != null && extCache.exists()) {
                    val extFiles = extCache.walkTopDown().filter { it.isFile }.toList()
                    for (ef in extFiles) {
                        bd.cacheSize += ef.length()
                        bd.cacheCount++
                    }
                }
            } catch (_: Exception) {}

            bd.totalSize = bd.photosSize + bd.videosSize + bd.audioSize + bd.gifsSize + bd.filesSize + bd.avatarsSize + bd.cacheSize
            bd.totalCount = bd.photosCount + bd.videosCount + bd.audioCount + bd.gifsCount + bd.filesCount + bd.avatarsCount + bd.cacheCount

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    updateStorageUi(b, bd)
                }
            }
        }
    }

    private fun updateStorageUi(b: ActivitySettingsContentBinding, bd: StorageBreakdown) {
        currentStorageBreakdown = bd

        b.tvSizePhotos.text = "${bd.photosCount} ф. (${formatStorageSize(bd.photosSize)})"
        b.tvSizeVideos.text = "${bd.videosCount} ф. (${formatStorageSize(bd.videosSize)})"
        b.tvSizeAudio.text = "${bd.audioCount} ф. (${formatStorageSize(bd.audioSize)})"
        b.tvSizeGifs.text = "${bd.gifsCount} ф. (${formatStorageSize(bd.gifsSize)})"
        b.tvSizeFiles.text = "${bd.filesCount} ф. (${formatStorageSize(bd.filesSize)})"
        b.tvSizeAvatars.text = "${bd.avatarsCount} ф. (${formatStorageSize(bd.avatarsSize)})"
        b.tvSizeCache.text = "${bd.cacheCount} ф. (${formatStorageSize(bd.cacheSize)})"

        b.tvStorageTotalSize.text = "Занято: ${formatStorageSize(bd.totalSize)} (${bd.totalCount} файлов)"

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
        var selectedCount = 0

        val pVisible = b.cbStoragePhotos.isChecked
        val vVisible = b.cbStorageVideos.isChecked
        val aVisible = b.cbStorageAudio.isChecked
        val gVisible = b.cbStorageGifs.isChecked
        val fVisible = b.cbStorageFiles.isChecked
        val avVisible = b.cbStorageAvatars.isChecked
        val cVisible = b.cbStorageCache.isChecked

        if (pVisible) { selectedBytes += bd.photosSize; selectedCount += bd.photosCount }
        if (vVisible) { selectedBytes += bd.videosSize; selectedCount += bd.videosCount }
        if (aVisible) { selectedBytes += bd.audioSize; selectedCount += bd.audioCount }
        if (gVisible) { selectedBytes += bd.gifsSize; selectedCount += bd.gifsCount }
        if (fVisible) { selectedBytes += bd.filesSize; selectedCount += bd.filesCount }
        if (avVisible) { selectedBytes += bd.avatarsSize; selectedCount += bd.avatarsCount }
        if (cVisible) { selectedBytes += bd.cacheSize; selectedCount += bd.cacheCount }

        val total = bd.totalSize.coerceAtLeast(1L).toFloat()

        animateWeight(b.vChartPhotos, if (pVisible) (bd.photosSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartVideos, if (vVisible) (bd.videosSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartAudio, if (aVisible) (bd.audioSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartGifs, if (gVisible) (bd.gifsSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartFiles, if (fVisible) (bd.filesSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartAvatars, if (avVisible) (bd.avatarsSize / total).coerceAtLeast(0.01f) else 0.001f)
        animateWeight(b.vChartOther, if (cVisible) (bd.cacheSize / total).coerceAtLeast(0.01f) else 0.001f)

        b.vChartPhotos.animate().alpha(if (pVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartVideos.animate().alpha(if (vVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartAudio.animate().alpha(if (aVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartGifs.animate().alpha(if (gVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartFiles.animate().alpha(if (fVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartAvatars.animate().alpha(if (avVisible) 1.0f else 0.2f).setDuration(250).start()
        b.vChartOther.animate().alpha(if (cVisible) 1.0f else 0.2f).setDuration(250).start()

        val allChecked = pVisible && vVisible && aVisible && gVisible && fVisible && avVisible && cVisible
        b.tvSelectAllStorage.text = if (allChecked) "Снять всё" else "Выбрать всё"

        if (selectedBytes > 0) {
            b.btnClearStorage.isEnabled = true
            b.btnClearStorage.text = "Очистить: $selectedCount ф. (${formatStorageSize(selectedBytes)})"
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
            var deletedCount = 0

            val deletePhotos = b.cbStoragePhotos.isChecked
            val deleteVideos = b.cbStorageVideos.isChecked
            val deleteAudio = b.cbStorageAudio.isChecked
            val deleteGifs = b.cbStorageGifs.isChecked
            val deleteFiles = b.cbStorageFiles.isChecked
            val deleteAvatars = b.cbStorageAvatars.isChecked
            val deleteCache = b.cbStorageCache.isChecked

            val allFiles = filesDir.walkTopDown().filter { it.isFile }.toList()

            for (f in allFiles) {
                val name = f.name.lowercase(Locale.US)
                val len = f.length()

                // Protect user profile assets, custom wallpapers, and database/json files
                if (name.startsWith("my_profile_avatar") || name.startsWith("my_avatar") || name.startsWith("wallpaper_img_") || name.endsWith(".json") || name.endsWith(".xml")) {
                    continue
                }

                val isAvatar = name.startsWith("avatar_") || name.startsWith("rec_avatar_") || name.startsWith("tmp_contact_avatar_")
                val isGif = name.endsWith(".gif") || name.startsWith("tenor_")
                val isVid = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp") || name.endsWith(".webm") || name.endsWith(".mov") || name.endsWith(".avi") || name.startsWith("rec_video_") || name.startsWith("rec_circle_") || name.startsWith("rec_videonote_") || f.parentFile?.name == "video_notes"
                val isAud = name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".opus") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav") || name.startsWith("rec_voice_") || name.startsWith("audio_")
                val isPhoto = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.startsWith("rec_photo_") || name.startsWith("cache_img_") || name.startsWith("cam_photo_")
                val isTmp = name.startsWith("tmp_") || name.endsWith(".tmp") || name.endsWith(".dat") || name.endsWith(".log") || name.startsWith("cam_video_")

                var delete = false
                if (isAvatar && deleteAvatars) delete = true
                else if (isGif && deleteGifs) delete = true
                else if (isVid && deleteVideos) delete = true
                else if (isAud && deleteAudio) delete = true
                else if (isPhoto && deletePhotos) delete = true
                else if (isTmp && deleteCache) delete = true
                else if (!isAvatar && !isGif && !isVid && !isAud && !isPhoto && !isTmp && deleteFiles) delete = true

                if (delete) {
                    try {
                        if (f.delete()) {
                            deletedBytes += len
                            deletedCount++
                        }
                    } catch (_: Exception) {}
                }
            }

            if (deleteAvatars) {
                AvatarManager.invalidateAvatarFileCache()
                AvatarManager.clearAvatarRadiusCache()
            }

            if (deleteCache) {
                try {
                    if (cacheDir != null && cacheDir.exists()) {
                        val cacheFiles = cacheDir.walkTopDown().filter { it.isFile }.toList()
                        for (cf in cacheFiles) {
                            val len = cf.length()
                            if (cf.delete()) {
                                deletedBytes += len
                                deletedCount++
                            }
                        }
                    }
                    val extCache = externalCacheDir
                    if (extCache != null && extCache.exists()) {
                        val extCacheFiles = extCache.walkTopDown().filter { it.isFile }.toList()
                        for (cf in extCacheFiles) {
                            val len = cf.length()
                            if (cf.delete()) {
                                deletedBytes += len
                                deletedCount++
                            }
                        }
                    }
                    Glide.get(applicationContext).clearDiskCache()
                    withContext(Dispatchers.Main) {
                        try {
                            android.webkit.WebStorage.getInstance().deleteAllData()
                        } catch (_: Exception) {}
                    }
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
                    if (deleteAudio) b.cbStorageAudio.isChecked = false
                    if (deleteGifs) b.cbStorageGifs.isChecked = false
                    if (deleteFiles) b.cbStorageFiles.isChecked = false
                    if (deleteAvatars) b.cbStorageAvatars.isChecked = false
                    if (deleteCache) b.cbStorageCache.isChecked = false

                    PrimeNotification.show(this@SettingsActivity, "Очищено: $deletedCount файлов (${formatStorageSize(deletedBytes)})")
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
        com.messenger.prime.browser.AppBrowserManager.openUrl(this, "https://github.com/Vaniahorosh")
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
        val colorStateList = ColorStateList.valueOf(accentColor)

        // Tint all main section divider headers ("ОФОРМЛЕНИЕ", "В ЧАТЕ", "ДАННЫЕ И ПАМЯТЬ", "ИНФОРМАЦИЯ")
        val sectionHeaderLayouts = listOfNotNull(
            b.layoutSettingsHeader,
            b.layoutChatHeader,
            b.layoutDataHeader,
            b.layoutInfoHeader
        )
        for (headerLayout in sectionHeaderLayouts) {
            for (i in 0 until headerLayout.childCount) {
                val child = headerLayout.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(accentColor)
                } else if (child is View) {
                    child.backgroundTintList = colorStateList
                }
            }
        }

        // Tint all card titles & sub-headers
        b.tvTitleTheme.setTextColor(accentColor)
        b.tvTitleNavStyle.setTextColor(accentColor)
        b.tvTitleAccent.setTextColor(accentColor)
        b.tvTitleLava.setTextColor(accentColor)
        b.tvTitleDisableAnimations?.setTextColor(accentColor)
        b.tvTitleWallpaper.setTextColor(accentColor)
        b.tvTitleBubbleRadius.setTextColor(accentColor)
        b.tvTitleAvatarRadius.setTextColor(accentColor)
        b.tvTitleBottomBarRadius?.setTextColor(accentColor)
        b.tvTitleStorage.setTextColor(accentColor)
        b.tvSelectAllStorage.setTextColor(accentColor)

        // Tint value texts & badges
        b.tvAccentSummary.text = ColorAccentManager.getAccentSummary(this)
        b.vAccentColorPreview.backgroundTintList = colorStateList
        b.tvBubbleRadiusValue.setTextColor(accentColor)
        b.tvAvatarRadiusValue.setTextColor(accentColor)
        b.tvBottomBarRadiusValue?.setTextColor(accentColor)
        b.tvVideoCornerRadiusValue?.setTextColor(accentColor)
        b.tvVideoSizeValue?.setTextColor(accentColor)
        b.tvStickySectionBadge.setTextColor(accentColor)
        b.cardHeaderPreviewInput?.strokeColor = accentColor
        b.cardBottomBarPreviewInput?.strokeColor = accentColor

        // Tint sticky progress line
        b.pbStickyScrollProgress.progressTintList = colorStateList

        // Tint all card header icons with fresh M3EDrawables
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val iconBgAlpha = if (isNight) 60 else 35
        val iconBgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, iconBgAlpha)

        val headerIcons = listOfNotNull(
            b.ivIconTheme,
            b.ivIconNavStyle,
            b.ivIconCategoriesAndNotes,
            b.ivIconAccent,
            b.ivIconLava,
            b.ivIconDisableAnimations,
            b.ivIconWallpaper,
            b.ivIconStorage,
            b.ivThanksIcon
        )

        val iconPadding = (8 * resources.displayMetrics.density).toInt()
        for (icon in headerIcons) {
            icon.imageTintList = colorStateList
            icon.background = com.messenger.prime.shapes.M3EDrawable(
                shape = com.messenger.prime.shapes.M3EShapes.cookie6Sided,
                color = iconBgColor
            )
            icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        }

        // Tint all sliders (thumb, active track, inactive track, halo)
        val sliders = listOfNotNull(
            b.sliderBubbleRadius,
            b.sliderAvatarRadius,
            b.sliderBottomBarRadius,
            b.sliderVideoCornerRadius,
            b.sliderVideoSize
        )
        val inactiveTrackTint = ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 60))
        val haloTint = ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 32))

        for (slider in sliders) {
            slider.thumbTintList = colorStateList
            slider.trackActiveTintList = colorStateList
            slider.trackInactiveTintList = inactiveTrackTint
            slider.haloTintList = haloTint
        }

        // Tint sticky progress line & track
        b.pbStickyScrollProgress.progressTintList = colorStateList
        b.pbStickyScrollProgress.progressBackgroundTintList = ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 40))

        // Spectrum button
        b.btnCustomPicker.tag = "do_not_tint"
        b.btnCustomPicker.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        b.btnCustomPicker.setTextColor(accentColor)
        b.btnCustomPicker.strokeColor = colorStateList
        b.btnCustomPicker.iconTint = colorStateList

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
        updateThemeSelectionUI(b, currentTheme)
        setupNavStyleSection(b)

        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun updateThemeSelectionUI(b: ActivitySettingsContentBinding, themeStr: String) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isSystem = themeStr == "system"
        val isLight = themeStr == "light"
        val isDark = themeStr == "dark"

        b.btnResetTheme?.visibility = if (!isSystem) View.VISIBLE else View.GONE
        b.btnResetTheme?.setOnClickListener {
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("app_theme").apply()
            updateThemeSelectionUI(b, "system")
            updateThemeChatPreview(b, "system")
            applyThemeChange("system")
        }

        b.rbThemeSystem.isChecked = isSystem
        b.rbThemeLight.isChecked = isLight
        b.rbThemeDark.isChecked = isDark

        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val radioTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                if (isNight) android.graphics.Color.parseColor("#66FFFFFF") else android.graphics.Color.parseColor("#66000000")
            )
        )
        b.rbThemeSystem.buttonTintList = radioTintList
        b.rbThemeLight.buttonTintList = radioTintList
        b.rbThemeDark.buttonTintList = radioTintList

        val strokeWidthActive = (3 * resources.displayMetrics.density).toInt()
        val strokeWidthInactive = (1 * resources.displayMetrics.density).toInt()
        val inactiveStrokeColor = if (isNight) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#1A000000")

        b.cardThemeSystem.strokeWidth = if (isSystem) strokeWidthActive else strokeWidthInactive
        b.cardThemeSystem.strokeColor = if (isSystem) accentColor else inactiveStrokeColor

        b.cardThemeLight.strokeWidth = if (isLight) strokeWidthActive else strokeWidthInactive
        b.cardThemeLight.strokeColor = if (isLight) accentColor else inactiveStrokeColor

        b.cardThemeDark.strokeWidth = if (isDark) strokeWidthActive else strokeWidthInactive
        b.cardThemeDark.strokeColor = if (isDark) accentColor else inactiveStrokeColor
    }

    private fun setupSwatches(b: ActivitySettingsContentBinding) {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentType = ColorAccentManager.getAccentType(this)
        val dp36 = (36 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        b.layoutSwatches.tag = "do_not_tint"
        b.layoutSwatches.removeAllViews()

        ColorAccentManager.PRESET_COLORS.forEachIndexed { index, preset ->
            val swatchColor = if (isDark) preset.darkColor else preset.lightColor
            val isSelected = when (accentType) {
                ColorAccentManager.ACCENT_TYPE_SYSTEM -> false
                ColorAccentManager.ACCENT_TYPE_DEFAULT -> index == 0
                ColorAccentManager.ACCENT_TYPE_CUSTOM -> (swatchColor == accentColor) || (preset.lightColor == accentColor || preset.darkColor == accentColor)
                else -> index == 0
            }

            val btn = com.google.android.material.button.MaterialButton(this).apply {
                tag = "do_not_tint"
                layoutParams = LinearLayout.LayoutParams(dp36, dp36).apply {
                    marginEnd = dp8
                }
                insetTop = 0
                insetBottom = 0
                cornerRadius = dp36 / 2
                backgroundTintList = ColorStateList.valueOf(swatchColor)
                if (isSelected) {
                    strokeColor = ColorStateList.valueOf(android.graphics.Color.WHITE)
                    strokeWidth = (2.5f * resources.displayMetrics.density).toInt()
                } else {
                    strokeWidth = 0
                }
                setOnClickListener {
                    if (index == 0) {
                        ColorAccentManager.setAccentDefault(this@SettingsActivity)
                    } else {
                        ColorAccentManager.setAccentCustom(this@SettingsActivity, swatchColor)
                    }
                    onAccentColorChanged(b)
                }
            }
            b.layoutSwatches.addView(btn)
        }
    }

    private fun onAccentColorChanged(b: ActivitySettingsContentBinding) {
        updateAccentUi(b)
        sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
        restartApp()
    }



    private fun showCustomColorSpectrumPicker(b: ActivitySettingsContentBinding) {
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)

        dialogBinding.btnPipette.visibility = View.GONE

        val blurCard = dialogBinding.root.findViewById<BlurView>(R.id.blurDialogCard)
        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            val overlayColor = com.messenger.prime.getDefaultBlurOverlayColor(this)
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
            onAccentColorChanged(b)
            dialog.dismiss()
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
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val isExpanded = sharedPrefs.getBoolean("settings_account_expanded", false)
        b.layoutAccountCollapsible.visibility = if (isExpanded) View.VISIBLE else View.GONE
        b.ivAccountArrow.visibility = View.VISIBLE
        b.ivAccountArrow.rotation = if (isExpanded) -90f else 90f

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
            val newPass = b.etSettingsPassword.text?.toString() ?: ""
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            
            val errorName = ValidationUtils.validateName(newName)
            val errorLogin = ValidationUtils.validateLogin(newLogin, sharedPrefs, currentLoginInDB)
            val errorPass = ValidationUtils.validatePassword(newPass)
            
            if (errorName != null || errorLogin != null || errorPass != null) {
                if (errorName != null) { b.inputLayoutName.error = errorName; b.inputLayoutName.shake() }
                if (errorLogin != null) { b.inputLayoutLogin.error = errorLogin; b.inputLayoutLogin.shake() }
                if (errorPass != null) { b.inputLayoutPassword.error = errorPass; b.inputLayoutPassword.shake() }
                return@setOnClickListener
            }
            
            val oldName = currentNameInDB
            val oldLogin = currentLoginInDB
            val oldPass = currentPassInDB

            if (newLogin != currentLoginInDB) {
                val oldAvatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    ?: sharedPrefs.getString("${currentLoginInDB}_avatarUri", null)

                sharedPrefs.edit().apply {
                    remove(currentLoginInDB)
                    remove("${currentLoginInDB}_name")
                    remove("${currentLoginInDB}_avatar")
                    remove("${currentLoginInDB}_avatarUri")
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", newName)
                    if (oldAvatar != null) {
                        putString("${newLogin}_avatar", oldAvatar)
                        putString("${newLogin}_avatarUri", oldAvatar)
                    }
                    apply()
                }

                val oldGif = File(filesDir, "avatar_$currentLoginInDB.gif")
                val oldJpg = File(filesDir, "avatar_$currentLoginInDB.jpg")
                if (oldGif.exists()) {
                    try { oldGif.renameTo(File(filesDir, "avatar_$newLogin.gif")) } catch (_: Exception) {}
                }
                if (oldJpg.exists()) {
                    try { oldJpg.renameTo(File(filesDir, "avatar_$newLogin.jpg")) } catch (_: Exception) {}
                }
            } else {
                sharedPrefs.edit().apply {
                    putString("${currentLoginInDB}_name", newName)
                    putString(currentLoginInDB, newPass)
                    apply()
                }
            }

            AvatarManager.syncActiveUserProfile(this, newLogin, newName)

            currentNameInDB = newName; currentLoginInDB = newLogin; currentPassInDB = newPass
            Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
            
            b.tvUserNameStatic.text = newName
            b.tvUserNameWP.text = newName
            b.tvAccountHeaderSummary.text = newName
            
            PrimeNotification.show(this, "Данные обновлены") {
                sharedPrefs.edit().apply {
                    if (newLogin != oldLogin) {
                        val currentAvatar = sharedPrefs.getString("${newLogin}_avatar", null)
                        remove(newLogin); remove("${newLogin}_name"); remove("${newLogin}_avatar")
                        putString(oldLogin, oldPass); putString("${oldLogin}_name", oldName)
                        if (currentAvatar != null) putString("${oldLogin}_avatar", currentAvatar)
                    } else { putString("${oldLogin}_name", oldName); putString(oldLogin, oldPass) }
                    apply()
                }
                AvatarManager.syncActiveUserProfile(this, oldLogin, oldName)
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
            val myDisplayName = AvatarManager.getMyDisplayName(this)
            val localAvatarUri = AvatarManager.getMyAvatarUri(this)
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""

            val handshake = "HANDSHAKE:login=$currentUser;name=$myDisplayName;avatar=${localAvatarUri ?: ""};version=${ChatPersonActivity.getAppVersionCode(this)}"
            BluetoothConnectionManager.getInstance().broadcastPacket(0x01.toByte(), handshake.toByteArray(Charsets.UTF_8))

            if (!localAvatarUri.isNullOrEmpty()) {
                try {
                    val uri = if (localAvatarUri.startsWith("content://") || localAvatarUri.startsWith("file://") || localAvatarUri.startsWith("android.resource://") || localAvatarUri.startsWith("http")) {
                        Uri.parse(localAvatarUri)
                    } else {
                        Uri.fromFile(File(localAvatarUri))
                    }
                    contentResolver.openInputStream(uri)?.use { isStream ->
                        val bitmap = BitmapFactory.decodeStream(isStream)
                        if (bitmap != null) {
                            val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, false)
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                            BluetoothConnectionManager.getInstance().broadcastPacket(0x07.toByte(), baos.toByteArray())
                            if (scaled != bitmap) scaled.recycle()
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                BluetoothConnectionManager.getInstance().broadcastPacket(0x07.toByte(), byteArrayOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (!uriToDelete.isNullOrEmpty()) {
            AvatarHistoryManager.removeMyAvatar(this, uriToDelete, deleteFile = false)
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
        val backupUri = uriToDelete
        Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, "Фото удалено", onUndo = {
            if (!backupUri.isNullOrEmpty()) {
                AvatarHistoryManager.addMyAvatar(this@SettingsActivity, backupUri)
                currentAvatarUri = backupUri
                avatarUriState.value = backupUri
                applyAvatarState(backupUri)
                val b2 = binding
                if (b2 != null) {
                    updatePhotoCardImage(b2)
                }
                Executors.newSingleThreadExecutor().execute { sendProfileUpdateOverBluetooth() }
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
            }
        })
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
                getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).edit()
                    .putBoolean("is_logged_in", false)
                    .remove("current_user")
                    .remove("current_user_name")
                    .remove("my_name")
                    .remove("my_avatar")
                    .remove("my_local_name")
                    .remove("my_local_avatar")
                    .remove("my_avatar_uri")
                    .apply()
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
            color = if (isSelected) ComposeColor.White.copy(alpha = 0.2f) else ComposeColor.Transparent,
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
                        selectedColor = ComposeColor.White,
                        unselectedColor = ComposeColor.White.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.material3.Text(
                    text = label,
                    color = ComposeColor.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @SuppressLint("InternalInsetResource", "Discouraged")
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

    private fun updateThemeChatPreview(b: ActivitySettingsContentBinding, themeStr: String) {
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = when (themeStr) {
            "light" -> false
            "dark" -> true
            else -> isSystemDark
        }

        val density = resources.displayMetrics.density

        val containerBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(if (isDark) android.graphics.Color.parseColor("#151D2A") else android.graphics.Color.parseColor("#F1F5F9"))
        }
        b.layoutThemeChatPreviewBg.background = containerBg
        b.cardThemeChatPreview.strokeColor = if (isDark) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#1A000000")

        val avatarRadiusPx = getAvatarCornerRadiusPx(this, (28 * density).toInt())
        val avatarBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = avatarRadiusPx.toFloat()
            setColor(ColorAccentManager.getAvatarColor("Prime"))
        }
        b.tvPreviewAvatar.background = avatarBg

        b.layoutPreviewIncomingBubble.background = ColorAccentManager.createIncomingBubbleDrawable(this)
        b.tvPreviewIncomingText.setTextColor(if (isDark) android.graphics.Color.parseColor("#F8FAFC") else android.graphics.Color.parseColor("#0F172A"))
        b.tvPreviewIncomingTime.setTextColor(if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B"))

        b.layoutPreviewOutgoingBubble.background = ColorAccentManager.createOutgoingBubbleDrawable(this)
        b.tvPreviewOutgoingText.setTextColor(android.graphics.Color.WHITE)
        b.tvPreviewOutgoingTime.setTextColor(android.graphics.Color.parseColor("#CCFFFFFF"))
        b.ivPreviewOutgoingStatus.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#CCFFFFFF"))
    }

    private fun setupBubbleRadiusSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val savedPercent = sharedPrefs.getInt("settings_bubble_radius_percent", 50)

        b.sliderBubbleRadius.value = savedPercent.toFloat()

        val btnReset = b.root.findViewById<ImageView>(R.id.btnResetBubbleRadius)
        val updateResetVisibility = { percent: Int ->
            btnReset?.visibility = if (percent != 50) View.VISIBLE else View.GONE
        }
        updateResetVisibility(savedPercent)

        val updateLabel = { percent: Int ->
            b.tvBubbleRadiusValue.text = "$percent%"
        }
        updateLabel(savedPercent)

        btnReset?.setOnClickListener {
            sharedPrefs.edit().putInt("settings_bubble_radius_percent", 50).apply()
            b.sliderBubbleRadius.value = 50f
            updateLabel(50)
            updateResetVisibility(50)
            val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
            updateThemeChatPreview(b, currentTheme)
            sendBroadcast(Intent("com.messenger.prime.BUBBLE_RADIUS_CHANGED").setPackage(packageName))
        }

        b.sliderBubbleRadius.addOnChangeListener { _, value, fromUser ->
            val percent = value.toInt()
            sharedPrefs.edit().putInt("settings_bubble_radius_percent", percent).apply()
            updateLabel(percent)
            updateResetVisibility(percent)
            val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
            updateThemeChatPreview(b, currentTheme)
            if (fromUser) {
                sendBroadcast(Intent("com.messenger.prime.BUBBLE_RADIUS_CHANGED").setPackage(packageName))
            }
        }
    }

    private fun setupAvatarRadiusSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val savedPercent = sharedPrefs.getInt("settings_avatar_radius_percent", 50)

        b.sliderAvatarRadius.value = savedPercent.toFloat()

        val myAvatar = currentAvatarUri ?: sharedPrefs.getString("my_avatar_uri", null)
        val myName = currentNameInDB.ifBlank { "Пользователь" }

        if (!myAvatar.isNullOrEmpty()) {
            b.ivAvatarPreviewImage.visibility = View.VISIBLE
            b.tvAvatarPreviewInitials.visibility = View.GONE
            loadAvatarIntoView(this, myAvatar, b.ivAvatarPreviewImage, myName)
        } else {
            b.ivAvatarPreviewImage.visibility = View.GONE
            b.tvAvatarPreviewInitials.visibility = View.VISIBLE
            val initial = if (myName.isNotBlank()) myName.take(1).uppercase() else "P"
            b.tvAvatarPreviewInitials.text = initial
        }

        val density = resources.displayMetrics.density
        val viewSizePx = (48 * density).toInt()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        b.layoutAvatarPreviewBg.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(if (isDark) android.graphics.Color.parseColor("#151D2A") else android.graphics.Color.parseColor("#F1F5F9"))
        }
        b.cardAvatarPreviewBlock.strokeColor = if (isDark) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#1A000000")

        val updateAvatarPreview = { percent: Int ->
            b.tvAvatarRadiusValue.text = "$percent%"
            val radiusPx = getAvatarCornerRadiusPx(this, viewSizePx)

            if (b.tvAvatarPreviewInitials.visibility == View.VISIBLE) {
                b.tvAvatarPreviewInitials.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx.toFloat()
                    setColor(ColorAccentManager.getAvatarColor(myName))
                }
            }

            b.ivAvatarPreviewImage.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
                }
            }
            b.ivAvatarPreviewImage.clipToOutline = true
            b.ivAvatarPreviewImage.invalidateOutline()

            val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
            updateThemeChatPreview(b, currentTheme)
        }

        val btnReset = b.root.findViewById<ImageView>(R.id.btnResetAvatarRadius)
        val updateResetVisibility = { percent: Int ->
            btnReset?.visibility = if (percent != 50) View.VISIBLE else View.GONE
        }
        updateResetVisibility(savedPercent)

        btnReset?.setOnClickListener {
            sharedPrefs.edit().putInt("settings_avatar_radius_percent", 50).apply()
            b.sliderAvatarRadius.value = 50f
            AvatarManager.clearAvatarRadiusCache()
            updateAvatarPreview(50)
            updateResetVisibility(50)
            sendBroadcast(Intent("com.messenger.prime.AVATAR_RADIUS_CHANGED").setPackage(packageName))
        }

        updateAvatarPreview(savedPercent)

        b.sliderAvatarRadius.addOnChangeListener { _, value, fromUser ->
            val percent = value.toInt()
            sharedPrefs.edit().putInt("settings_avatar_radius_percent", percent).apply()
            AvatarManager.clearAvatarRadiusCache()
            updateAvatarPreview(percent)
            updateResetVisibility(percent)
            if (fromUser) {
                sendBroadcast(Intent("com.messenger.prime.AVATAR_RADIUS_CHANGED").setPackage(packageName))
            }
        }
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
            binding?.let { b ->
                b.tvThemeSummary.text = when(newTheme) {
                    "light" -> "Светлая"
                    "dark" -> "Темная"
                    else -> "Системная"
                }
                updateThemeSelectionUI(b, newTheme)
                updateThemeChatPreview(b, newTheme)
                setupSwatches(b)
                updateAccentUi(b)
            }
            sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(packageName))
        }
        isThemeDialogVisible.value = false
    }

    override fun onDestroy() {
        stopWobbling()
        try { unregisterReceiver(pluginsReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun setupVideoMessageSection(b: ActivitySettingsContentBinding) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        val savedRadiusPercent = sharedPrefs.getInt("video_message_corner_radius", 50)
        val savedSizeDp = sharedPrefs.getInt("video_message_size_dp", 180)

        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val isDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        b.layoutVideoNotePreviewBg?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(
                if (isDark) Color.parseColor("#1E293B") else Color.parseColor("#E2E8F0"),
                if (isDark) Color.parseColor("#0F172A") else Color.parseColor("#F1F5F9")
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
        }
        b.cardVideoNotePreviewBlock?.strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#1A000000")

        fun updatePreviewAndLabels(radiusPercent: Int, sizeDp: Int) {
            val labelText = when {
                radiusPercent >= 50 -> "$radiusPercent% (Круг)"
                radiusPercent == 0 -> "0% (Квадрат)"
                else -> "$radiusPercent% (Сквиркл)"
            }
            b.tvVideoCornerRadiusValue?.text = labelText
            val sizePercent = Math.round(((sizeDp - 120f) / (260f - 120f)) * 100)
            b.tvVideoSizeValue?.text = "$sizePercent%"

            b.layoutVideoNotePreviewFrame?.let { frame ->
                val density = resources.displayMetrics.density
                val previewSizePx = (sizeDp * density).toInt()

                val lp = frame.layoutParams
                if (lp != null && (lp.width != previewSizePx || lp.height != previewSizePx)) {
                    lp.width = previewSizePx
                    lp.height = previewSizePx
                    frame.layoutParams = lp
                }

                val cornerRadiusPx = (previewSizePx * (radiusPercent / 100f))

                frame.clipToOutline = true
                frame.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                    }
                }
                frame.invalidateOutline()
            }

            b.pbVideoNotePreviewFrame?.let { pb ->
                pb.isBorderProgressMode = true
                pb.staticShapeType = radiusPercent.toString()
                val accentColor = ColorAccentManager.getCurrentAccentColor(this)
                pb.setIndicatorColor(accentColor)
                pb.progress = 65
            }
        }

        val btnResetCorner = b.root.findViewById<ImageView>(R.id.btnResetVideoCorner)
        val btnResetSize = b.root.findViewById<ImageView>(R.id.btnResetVideoSize)

        fun updateResetBtnsVisibility(radiusPercent: Int, sizeDp: Int) {
            btnResetCorner?.visibility = if (radiusPercent != 50) View.VISIBLE else View.GONE
            btnResetSize?.visibility = if (sizeDp != 180) View.VISIBLE else View.GONE
        }

        b.sliderVideoCornerRadius?.value = savedRadiusPercent.toFloat().coerceIn(0f, 50f)
        b.sliderVideoSize?.value = savedSizeDp.toFloat().coerceIn(120f, 260f)

        updatePreviewAndLabels(savedRadiusPercent, savedSizeDp)
        updateResetBtnsVisibility(savedRadiusPercent, savedSizeDp)

        btnResetCorner?.setOnClickListener {
            sharedPrefs.edit().putInt("video_message_corner_radius", 50).putString("video_message_shape", "50").apply()
            b.sliderVideoCornerRadius?.value = 50f
            val currentSize = b.sliderVideoSize?.value?.toInt() ?: 180
            updatePreviewAndLabels(50, currentSize)
            updateResetBtnsVisibility(50, currentSize)
        }

        btnResetSize?.setOnClickListener {
            sharedPrefs.edit().putInt("video_message_size_dp", 180).apply()
            b.sliderVideoSize?.value = 180f
            val currentRadius = b.sliderVideoCornerRadius?.value?.toInt() ?: 50
            updatePreviewAndLabels(currentRadius, 180)
            updateResetBtnsVisibility(currentRadius, 180)
        }

        b.sliderVideoCornerRadius?.addOnChangeListener { _, value, fromUser ->
            val radius = value.toInt()
            val currentSize = b.sliderVideoSize?.value?.toInt() ?: 180
            updateResetBtnsVisibility(radius, currentSize)
            if (fromUser) {
                sharedPrefs.edit().putInt("video_message_corner_radius", radius).putString("video_message_shape", radius.toString()).apply()
                updatePreviewAndLabels(radius, currentSize)
            }
        }

        b.sliderVideoSize?.addOnChangeListener { _, value, fromUser ->
            val size = value.toInt()
            val currentRadius = b.sliderVideoCornerRadius?.value?.toInt() ?: 50
            updateResetBtnsVisibility(currentRadius, size)
            if (fromUser) {
                sharedPrefs.edit().putInt("video_message_size_dp", size).apply()
                updatePreviewAndLabels(currentRadius, size)
            }
        }

        val qualityOptions = arrayOf("Низкое (Быстрое)", "Среднее (Баланс)", "Высокое (HD)")
        val qualityAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityOptions)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerVideoQuality?.adapter = qualityAdapter
        val savedQuality = sharedPrefs.getInt("video_message_quality", 0)
        b.spinnerVideoQuality?.setSelection(savedQuality)
        b.spinnerVideoQuality?.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val current = sharedPrefs.getInt("video_message_quality", 0)
                val menu = PrimePopupMenu.create(this, v)
                qualityOptions.forEachIndexed { index, title ->
                    menu.addItem(title, isSelected = (index == current)) {
                        sharedPrefs.edit().putInt("video_message_quality", index).apply()
                        b.spinnerVideoQuality?.setSelection(index)
                    }
                }
                menu.show()
            }
            true
        }

        val fpsOptions = arrayOf("30 FPS", "60 FPS (Плавно)")
        val fpsAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, fpsOptions)
        fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerVideoFps?.adapter = fpsAdapter
        val savedFps = sharedPrefs.getInt("video_message_fps", 0)
        b.spinnerVideoFps?.setSelection(savedFps)
        b.spinnerVideoFps?.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val current = sharedPrefs.getInt("video_message_fps", 0)
                val menu = PrimePopupMenu.create(this, v)
                fpsOptions.forEachIndexed { index, title ->
                    menu.addItem(title, isSelected = (index == current)) {
                        sharedPrefs.edit().putInt("video_message_fps", index).apply()
                        b.spinnerVideoFps?.setSelection(index)
                    }
                }
                menu.show()
            }
            true
        }

        val savedStab = sharedPrefs.getBoolean("video_message_stabilization", true)
        b.switchVideoStabilization?.isChecked = savedStab
        b.switchVideoStabilization?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("video_message_stabilization", isChecked).apply()
        }

        val apiOptions = arrayOf("CameraX (Стабильно)", "Camera2 (Устаревшее)")
        val apiAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, apiOptions)
        apiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerCameraApi?.adapter = apiAdapter
        val savedApi = sharedPrefs.getInt("video_message_api", 0)
        b.spinnerCameraApi?.setSelection(savedApi)
        b.spinnerCameraApi?.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val current = sharedPrefs.getInt("video_message_api", 0)
                val menu = PrimePopupMenu.create(this, v)
                apiOptions.forEachIndexed { index, title ->
                    menu.addItem(title, isSelected = (index == current)) {
                        sharedPrefs.edit().putInt("video_message_api", index).apply()
                        b.spinnerCameraApi?.setSelection(index)
                    }
                }
                menu.show()
            }
            true
        }
    }

    private fun renderCategoriesList() {
        val container = binding?.root?.findViewWithTag<LinearLayout>("containerCategoriesList") ?: return
        container.removeAllViews()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        val customCats = mutableListOf<Triple<String, String, List<String>>>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                val chatsArr = obj.optJSONArray("chats")
                val chatsList = mutableListOf<String>()
                if (chatsArr != null) {
                    for (j in 0 until chatsArr.length()) {
                        chatsList.add(chatsArr.optString(j))
                    }
                }
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    customCats.add(Triple(id, name, chatsList))
                }
            }
        } catch (_: Exception) {}

        if (customCats.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Свои категории не созданы"
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_secondary))
                textSize = 12f
                setPadding(0, 4, 0, 4)
            }
            container.addView(tvEmpty)
            return
        }

        for (index in customCats.indices) {
            val (catId, catName, catChats) = customCats[index]

            val itemRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = (8 * resources.displayMetrics.density).toInt()
                setPadding(0, p, 0, p)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(this).apply {
                text = catName
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_primary))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            val chatsSummary = if (catChats.isEmpty()) "Без чатов" else "Чаты: ${catChats.joinToString(", ")}"
            val tvSubtitle = TextView(this).apply {
                text = chatsSummary
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.prime_text_secondary))
                textSize = 11f
            }

            textLayout.addView(tvTitle)
            textLayout.addView(tvSubtitle)
            itemRow.addView(textLayout)

            // Button: Edit chats
            val btnEdit = ImageButton(this).apply {
                setImageResource(R.drawable.ic_edit)
                setBackgroundResource(android.R.color.transparent)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@SettingsActivity, R.color.prime_brand))
                setPadding(8, 8, 8, 8)
                setOnClickListener { showEditCustomCategoryDialog(catId, catName, catChats) }
            }
            itemRow.addView(btnEdit)

            // Button: Move Up
            if (index > 0) {
                val btnUp = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_arrow_up)
                    setBackgroundResource(android.R.color.transparent)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@SettingsActivity, R.color.prime_brand))
                    setPadding(8, 8, 8, 8)
                    setOnClickListener { moveCategory(index, index - 1) }
                }
                itemRow.addView(btnUp)
            }

            // Button: Move Down
            if (index < customCats.size - 1) {
                val btnDown = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_expand_more)
                    setBackgroundResource(android.R.color.transparent)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@SettingsActivity, R.color.prime_brand))
                    setPadding(8, 8, 8, 8)
                    setOnClickListener { moveCategory(index, index + 1) }
                }
                itemRow.addView(btnDown)
            }

            // Button: Delete
            val btnDelete = ImageButton(this).apply {
                setImageResource(R.drawable.ic_trash)
                setBackgroundResource(android.R.color.transparent)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@SettingsActivity, R.color.prime_danger))
                setPadding(8, 8, 8, 8)
                setOnClickListener {
                    deleteCustomCategory(catId)
                    renderCategoriesList()
                    PrimeNotification.show(this@SettingsActivity, "Категория «$catName» удалена")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
            itemRow.addView(btnDelete)

            container.addView(itemRow)
        }
    }

    private fun moveCategory(fromIdx: Int, toIdx: Int) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            if (fromIdx in 0 until array.length() && toIdx in 0 until array.length()) {
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                val moved = list.removeAt(fromIdx)
                list.add(toIdx, moved)
                val newArray = JSONArray()
                list.forEach { newArray.put(it) }
                sharedPrefs.edit().putString("custom_categories_json", newArray.toString()).apply()
                renderCategoriesList()
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        } catch (_: Exception) {}
    }

    private fun showEditCustomCategoryDialog(catId: String, currentName: String, currentChats: List<String>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val inputName = EditText(this).apply {
            hint = "Название категории"
            setText(currentName)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
        }
        container.addView(inputName)

        val jsonChats = ChatHistoryManager.getPersistedChatsJson(this)
        val chatNames = mutableListOf<String>()
        try {
            val array = JSONArray(jsonChats)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                if (name.isNotEmpty() && name != "Заметки" && name != "AI Prime Chat" && !chatNames.contains(name)) {
                    chatNames.add(name)
                }
            }
        } catch (_: Exception) {}

        val selectedChats = currentChats.toMutableSet()
        if (chatNames.isNotEmpty()) {
            val tvLabel = TextView(this).apply {
                text = "Выберите чаты для категории:"
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 13f
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }
            container.addView(tvLabel)

            for (chatName in chatNames) {
                val cb = CheckBox(this).apply {
                    text = chatName
                    setTextColor(Color.WHITE)
                    isChecked = selectedChats.contains(chatName)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedChats.add(chatName) else selectedChats.remove(chatName)
                    }
                }
                container.addView(cb)
            }
        }

        PrimeBlurDialog.show(
            activity = this,
            title = "Редактировать категорию",
            message = null,
            positiveText = "Сохранить",
            negativeText = "Отмена",
            customView = container,
            onPositive = {
                val categoryName = inputName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    updateCustomCategory(catId, categoryName, selectedChats.toList())
                    renderCategoriesList()
                    PrimeNotification.show(this, "Категория обновлена")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
        )
    }

    private fun updateCustomCategory(id: String, name: String, chats: List<String>) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") == id) {
                    obj.put("name", name)
                    val chatsArray = JSONArray()
                    chats.forEach { chatsArray.put(it) }
                    obj.put("chats", chatsArray)
                    break
                }
            }
            sharedPrefs.edit().putString("custom_categories_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun showManageCategoriesDialog() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        val customCats = mutableListOf<Pair<String, String>>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    customCats.add(Pair(id, name))
                }
            }
        } catch (_: Exception) {}

        if (customCats.isEmpty()) {
            showAddCustomCategoryDialog()
            return
        }

        val options = mutableListOf<String>()
        customCats.forEach { options.add("🗑 Удалить категорию «${it.second}»") }
        options.add("➕ Добавить новую категорию")

        PrimeBlurDialog.showList(
            activity = this,
            title = "Управление категориями",
            items = options.toTypedArray()
        ) { index, _ ->
            if (index < customCats.size) {
                val catToDelete = customCats[index]
                deleteCustomCategory(catToDelete.first)
                PrimeNotification.show(this, "Категория «${catToDelete.second}» удалена")
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            } else {
                showAddCustomCategoryDialog()
            }
        }
    }

    private fun showAddCustomCategoryDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val inputName = EditText(this).apply {
            hint = "Название категории (напр. Работа)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
        }
        container.addView(inputName)

        val jsonChats = ChatHistoryManager.getPersistedChatsJson(this)
        val chatNames = mutableListOf<String>()
        try {
            val array = JSONArray(jsonChats)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                if (name.isNotEmpty() && name != "Заметки" && name != "AI Prime Chat" && !chatNames.contains(name)) {
                    chatNames.add(name)
                }
            }
        } catch (_: Exception) {}

        val selectedChats = mutableSetOf<String>()
        if (chatNames.isNotEmpty()) {
            val tvLabel = TextView(this).apply {
                text = "Выберите чаты для этой категории:"
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 13f
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }
            container.addView(tvLabel)

            for (chatName in chatNames) {
                val cb = CheckBox(this).apply {
                    text = chatName
                    setTextColor(Color.WHITE)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedChats.add(chatName) else selectedChats.remove(chatName)
                    }
                }
                container.addView(cb)
            }
        }

        PrimeBlurDialog.show(
            activity = this,
            title = "Создать категорию",
            message = null,
            positiveText = "Сохранить",
            negativeText = "Отмена",
            customView = container,
            onPositive = {
                val categoryName = inputName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    saveCustomCategory(categoryName, selectedChats.toList())
                    PrimeNotification.show(this, "Категория «$categoryName» создана")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
        )
    }

    private fun saveCustomCategory(name: String, chats: List<String>) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { JSONArray() }

        val newObj = JSONObject().apply {
            put("id", "cat_${System.currentTimeMillis()}")
            put("name", name)
            val chatsArray = JSONArray()
            chats.forEach { chatsArray.put(it) }
            put("chats", chatsArray)
        }
        array.put(newObj)
        sharedPrefs.edit().putString("custom_categories_json", array.toString()).apply()
    }

    private fun deleteCustomCategory(id: String) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != id) {
                    newArray.put(obj)
                }
            }
            sharedPrefs.edit().putString("custom_categories_json", newArray.toString()).apply()
        } catch (_: Exception) {}
    }
}

data class BottomBarButtonConfig(
    val id: String,
    val title: String,
    val iconRes: Int,
    var isVisible: Boolean,
    val isMandatory: Boolean = false
)

class NavButtonsAdapter(
    private val items: MutableList<BottomBarButtonConfig>,
    private val onOrderChanged: () -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<NavButtonsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(android.R.id.icon)
        val tvTitle: TextView = view.findViewById(android.R.id.title)
        val switchVisibility: com.google.android.material.materialswitch.MaterialSwitch = view.findViewById(android.R.id.checkbox)
        val ivDragHandle: ImageView = view.findViewById(android.R.id.icon2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            
            val scale = context.resources.displayMetrics.density
            val paddingV = (12 * scale).toInt()
            setPadding(0, paddingV, 0, paddingV)
            
            val icon = ImageView(context).apply {
                id = android.R.id.icon
                layoutParams = LinearLayout.LayoutParams((24 * scale).toInt(), (24 * scale).toInt()).apply {
                    marginEnd = (16 * scale).toInt()
                }
            }
            addView(icon)
            
            val title = TextView(context).apply {
                id = android.R.id.title
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.prime_text_primary))
                textSize = 15f
            }
            addView(title)
            
            val switch = com.google.android.material.materialswitch.MaterialSwitch(context).apply {
                id = android.R.id.checkbox
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            addView(switch)
            
            val dragHandle = ImageView(context).apply {
                id = android.R.id.icon2
                layoutParams = LinearLayout.LayoutParams((24 * scale).toInt(), (24 * scale).toInt()).apply {
                    marginStart = (16 * scale).toInt()
                }
                setImageResource(R.drawable.ic_drag_handle)
                imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.prime_text_secondary))
            }
            addView(dragHandle)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivIcon.setImageResource(item.iconRes)
        val accent = ColorAccentManager.getCurrentAccentColor(holder.itemView.context)
        holder.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(accent)
        holder.tvTitle.text = item.title
        
        holder.switchVisibility.setOnCheckedChangeListener(null)
        holder.switchVisibility.isChecked = item.isVisible
        holder.switchVisibility.isEnabled = !item.isMandatory
        holder.switchVisibility.alpha = if (item.isMandatory) 0.5f else 1f
        
        holder.switchVisibility.setOnCheckedChangeListener { _, isChecked ->
            item.isVisible = isChecked
            onOrderChanged()
        }
    }

    override fun getItemCount() = items.size
    
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
        onOrderChanged()
    }
}
