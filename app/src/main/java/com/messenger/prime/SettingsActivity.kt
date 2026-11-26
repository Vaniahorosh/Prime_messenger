package com.messenger.prime

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import com.messenger.prime.browser.BrowserPreferences
import com.messenger.prime.databinding.ActivitySettingsContentBinding
import com.messenger.prime.shapes.M3EDrawable
import com.messenger.prime.shapes.M3EShapes
import eightbitlab.com.blurview.BlurView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs

class SettingsActivity : AppCompatActivity() {

    private var binding: ActivitySettingsContentBinding? = null

    private var isClosing = false
    private val isPhotoMenuMode = mutableStateOf(false)
    private val wobbleAnimators = mutableListOf<Animator>()

    private var currentAvatarUri: String? = null
    private var currentNameInDB: String = ""
    private var currentLoginInDB: String = ""
    private var currentAvatarIndex = 0

    private var initialTheme: String = "system"
    private var initialAccentColor: Int = 0
    private var initialLanguageTag: String = ""
    private var latestBottomInset: Int = 0
    private var activePhotoPickerSheet: PhotoPickerBottomSheet? = null
    private var isPickerForCover: Boolean = false

    private val photoEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                        } catch (_: Exception) {
                            null
                        } ?: if (srcUri.scheme == "file" && srcUri.path != null) {
                            FileInputStream(File(srcUri.path!!))
                        } else {
                            FileInputStream(File(uriStr.removePrefix("file://")))
                        }

                        inputStream.use { input ->
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

                    binding?.let { updatePhotoCardImage(it) }
                    applyAvatarState(permAvatarUri)
                    Executors.newSingleThreadExecutor().execute {
                        SettingsSupport.sendProfileUpdateOverBluetooth(this)
                    }
                    sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                    PrimeNotification.show(
                        this,
                        getString(if (isGif) R.string.settings_avatar_gif_added else R.string.settings_avatar_added)
                    )
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

        binding?.let { updatePhotoCardImage(it) }
        applyAvatarState(permAvatarUri)
        Executors.newSingleThreadExecutor().execute {
            SettingsSupport.sendProfileUpdateOverBluetooth(this)
        }
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, getString(R.string.settings_avatar_gif_added))
    }

    private val coverEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val coverUriStr = result.data?.getStringExtra("COVER_URI")
                val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                val currentUser = sp.getString("current_user", "") ?: ""

                val finalCoverUri = if (!coverUriStr.isNullOrBlank()) {
                    coverUriStr
                } else {
                    sp.getString("${currentUser}_cover", null) ?: sp.getString("my_profile_cover", null)
                }

                if (!finalCoverUri.isNullOrBlank()) {
                    sp.edit()
                        .putString("${currentUser}_cover", finalCoverUri)
                        .putString("my_profile_cover", finalCoverUri)
                        .apply()
                }

                updateHeaderCover()
                Executors.newSingleThreadExecutor().execute {
                    SettingsSupport.sendProfileUpdateOverBluetooth(this)
                }
                sendBroadcast(Intent("com.messenger.prime.COVER_CHANGED").setPackage(packageName))
                PrimeNotification.show(this, getString(R.string.settings_cover_updated_short))
            }
        }

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            activePhotoPickerSheet?.refreshMedia()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { handlePickedMediaUri(it) }
        }

    private fun handlePickedMediaUri(uri: Uri) {
        if (isPickerForCover) {
            isPickerForCover = false
            val intent = Intent(this, CoverEditorActivity::class.java).apply {
                putExtra("EXTRA_IMAGE_URI", uri.toString())
            }
            coverEditorLauncher.launch(intent)
            PrimeTransitions.applyOpenTransition(this)
            return
        }

        val uriStr = uri.toString().lowercase()
        val mime = try {
            contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        val isGif = uriStr.endsWith(".gif") ||
            uriStr.contains("gif") ||
            "image/gif".equals(mime, ignoreCase = true)

        if (isGif) {
            setGifAvatarDirectly(uri)
        } else {
            val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                setDataAndType(uri, mime ?: "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("EXTRA_IMAGE_URI", uri.toString())
                putExtra("IS_PROFILE_PHOTO", true)
            }
            photoEditorLauncher.launch(intent)
            PrimeTransitions.applyOpenTransition(this)
        }
    }

    private fun updateBackCallbackState() {
        backCallback.isEnabled = isPhotoMenuMode.value
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (isPhotoMenuMode.value) {
                togglePhotoMenuMode(false)
            }
            updateBackCallbackState()
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
                "com.messenger.prime.NAV_STYLE_CHANGED",
                "com.messenger.prime.AVATAR_CHANGED" -> {
                    runOnUiThread {
                        setupBottomNav()
                        binding?.let { refreshHubSummaries(it) }
                    }
                }
                "com.messenger.prime.COVER_CHANGED" -> {
                    runOnUiThread { updateHeaderCover() }
                }
                "com.messenger.prime.WALLPAPER_CHANGED",
                PluginManager.ACTION_PLUGINS_CHANGED -> {
                    runOnUiThread { binding?.let { refreshHubSummaries(it) } }
                }
                "com.messenger.prime.THEME_CHANGED",
                "com.messenger.prime.ACCENT_CHANGED",
                PrimeLocaleManager.ACTION_LANGUAGE_CHANGED -> {
                    runOnUiThread { recreate() }
                }
            }
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
        window.statusBarColor = Color.TRANSPARENT

        val darkTheme = SettingsSupport.isDarkThemeActive(this)
        setupEdgeToEdge(isDarkIcons = !darkTheme)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        initialTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
        initialAccentColor = ColorAccentManager.getCurrentAccentColor(this)
        initialLanguageTag = PrimeLocaleManager.getCurrentLanguageTag(this)

        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser

        val savedName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", null)
            ?: getString(R.string.settings_default_me)
        currentNameInDB = savedName

        val savedAvatarUri = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri

        b.layoutSettingsBodyContainer.translationY = 0f
        b.tvUserNameStatic.text = savedName
        b.tvUserNameWP.text = savedName
        b.tvUserNameWP.isSelected = true

        setupComposePhoto(b)
        setupHeaderListeners(b)
        setupSectionNavigation(b)
        applyAvatarState(savedAvatarUri)
        updateHeaderCover()

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

        b.nestedScrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.prime_base))
        b.headerStaticBlock.clipChildren = true

        val sysDensity = resources.displayMetrics.density
        val initialExtraPadding = (12 * sysDensity).toInt()
        val initialTopInset = getStatusBarHeight()
        val bottomCompactPadding = (4 * sysDensity).toInt()

        b.headerStaticBlock.updatePadding(top = 0, bottom = 0)
        b.layoutWithPhoto.updatePadding(
            top = initialTopInset + initialExtraPadding,
            bottom = bottomCompactPadding
        )
        b.layoutNoPhoto.updatePadding(
            top = initialTopInset + initialExtraPadding,
            bottom = bottomCompactPadding
        )

        b.btnEditCover?.let { btn ->
            (btn.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                lp.topMargin = initialTopInset + (16 * sysDensity).toInt()
                btn.layoutParams = lp
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val sDensity = resources.displayMetrics.density
            val extraPadding = (12 * sDensity).toInt()
            val compactPadding = (4 * sDensity).toInt()
            val topInset = maxOf(systemBars.top, cutout.top).takeIf { it > 0 } ?: getStatusBarHeight()
            latestBottomInset = maxOf(systemBars.bottom, cutout.bottom)

            b.root.setPadding(cutout.left, 0, cutout.right, 0)
            b.headerStaticBlock.updatePadding(top = 0, bottom = 0)
            b.layoutWithPhoto.updatePadding(
                top = topInset + extraPadding,
                bottom = compactPadding
            )
            b.layoutNoPhoto.updatePadding(
                top = topInset + extraPadding,
                bottom = compactPadding
            )

            b.btnEditCover?.let { btn ->
                (btn.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    lp.topMargin = topInset + (16 * sDensity).toInt()
                    btn.layoutParams = lp
                }
            }

            applyAdaptiveLayout(b, insets)
            applyBottomBarInsets(b)
            insets
        }
        ViewCompat.requestApplyInsets(b.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme

        setupM3ECardsAndSections(b)
        refreshHubSummaries(b)
        setupBottomNav(b)

        b.headerStaticBlock.translationZ = 4f
        b.photoCard.translationZ = 8f

        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private fun applyBottomBarInsets(b: ActivitySettingsContentBinding) {
        val density = resources.displayMetrics.density
        val navView = b.root.findViewById<View>(R.id.blurBottomNav)
        val isNavVisible = navView != null && navView.visibility == View.VISIBLE
        if (isNavVisible && navView != null) {
            (navView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                val targetMargin = latestBottomInset + (12 * density).toInt()
                if (lp.bottomMargin != targetMargin) {
                    lp.bottomMargin = targetMargin
                    navView.layoutParams = lp
                }
            }
            b.nestedScrollView.updatePadding(bottom = latestBottomInset + (92 * density).toInt())
        } else {
            b.nestedScrollView.updatePadding(bottom = latestBottomInset + (24 * density).toInt())
        }
    }

    private fun applyAdaptiveLayout(b: ActivitySettingsContentBinding, insets: WindowInsetsCompat) {
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

        val profile = SettingsAdaptiveHelper.computeWindowProfile(
            activity = this,
            safeTopPx = systemBars.top,
            safeBottomPx = systemBars.bottom,
            cutoutLeftPx = cutout.left,
            cutoutRightPx = cutout.right,
            imeBottomPx = ime.bottom,
            imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        )

        val mainContainer = b.headerStaticBlock.parent as? androidx.constraintlayout.widget.ConstraintLayout ?: return
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(mainContainer)

        if (profile.isTwoPaneHub) {
            val leftWidthPx = SettingsAdaptiveHelper.computeTwoPaneLeftWidthPx(profile)

            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START)
            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.END, b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.START)
            constraintSet.constrainWidth(b.headerStaticBlock.id, leftWidthPx)

            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.START, b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.END)
            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END)
            constraintSet.constrainWidth(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT)

            val coverHeight = (180 * profile.density).toInt()
            constraintSet.constrainHeight(b.composeHeaderBackground.id, coverHeight)
            constraintSet.constrainHeight(b.ivProfileCover.id, coverHeight)

            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
            constraintSet.setMargin(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.TOP, (75 * profile.density).toInt())

            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.TOP, b.ivProfileCover.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            constraintSet.clear(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            constraintSet.setMargin(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.TOP, (16 * profile.density).toInt())
        } else {
            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START)
            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END)
            constraintSet.constrainWidth(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT)

            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START)
            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END)
            constraintSet.constrainWidth(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT)

            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.TOP, b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            constraintSet.connect(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            constraintSet.setMargin(b.layoutSettingsBodyContainer.id, androidx.constraintlayout.widget.ConstraintSet.TOP, (12 * profile.density).toInt())

            val coverHeight = (240 * profile.density).toInt()
            constraintSet.constrainHeight(b.composeHeaderBackground.id, coverHeight)
            constraintSet.constrainHeight(b.ivProfileCover.id, coverHeight)

            constraintSet.connect(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
            constraintSet.setMargin(b.headerStaticBlock.id, androidx.constraintlayout.widget.ConstraintSet.TOP, (135 * profile.density).toInt())
        }
        constraintSet.applyTo(mainContainer)
    }

    private fun updateHeaderCover() {
        val b = binding ?: return
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val currentUser = sp.getString("current_user", "") ?: ""
        val coverUriStr = sp.getString("${currentUser}_cover", null)
            ?: sp.getString("my_profile_cover", null)

        val iv = b.ivProfileCover
        if (!coverUriStr.isNullOrBlank()) {
            iv.visibility = View.VISIBLE
            Glide.with(this)
                .load(Uri.parse(coverUriStr))
                .centerCrop()
                .into(iv)
        } else {
            iv.visibility = View.GONE
        }
    }

    private fun showCoverOptionDialog() {
        val b = binding ?: return
        val anchorView = b.btnEditCover ?: return
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val currentUser = sp.getString("current_user", "") ?: ""
        val coverUriStr = sp.getString("${currentUser}_cover", null)
            ?: sp.getString("my_profile_cover", null)

        val hasCover = !coverUriStr.isNullOrBlank()
        val menu = PrimePopupMenu.create(this, anchorView)

        if (hasCover) {
            menu.addItem(getString(R.string.settings_cover_edit), R.drawable.ic_edit) {
                showCoverPickerSheet()
            }
            menu.addItem(getString(R.string.settings_cover_remove), R.drawable.ic_trash) {
                sp.edit()
                    .remove("${currentUser}_cover")
                    .remove("my_profile_cover")
                    .apply()
                updateHeaderCover()
                Executors.newSingleThreadExecutor().execute {
                    SettingsSupport.sendProfileUpdateOverBluetooth(this)
                }
                sendBroadcast(Intent("com.messenger.prime.COVER_CHANGED").setPackage(packageName))
                PrimeNotification.show(this, getString(R.string.settings_cover_removed_short))
            }
        } else {
            menu.addItem(getString(R.string.settings_cover_choose), R.drawable.ic_edit) {
                showCoverPickerSheet()
            }
        }

        menu.show()
    }

    private fun showCoverPickerSheet() {
        isPickerForCover = true
        val sheet = PhotoPickerBottomSheet(
            activity = this,
            title = getString(R.string.settings_cover_dialog_title),
            allowGif = false,
            showPresets = false,
            onMediaSelected = { uri, _ ->
                activePhotoPickerSheet = null
                handlePickedMediaUri(uri)
            },
            onOpenSystemPicker = {
                activePhotoPickerSheet = null
                isPickerForCover = true
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRequestPermission = {
                galleryPermissionLauncher.launch(PhotoPickerBottomSheet.getRequiredImagePermissions())
            },
            onDismissWithoutSelection = {
                activePhotoPickerSheet = null
            }
        )
        activePhotoPickerSheet = sheet
        sheet.show()
    }

    private fun setupComposePhoto(b: ActivitySettingsContentBinding) {
        b.ivPhotoCard.setOnClickListener {
            if (currentAvatarUri != null) togglePhotoMenuMode(!isPhotoMenuMode.value)
            else showAvatarChoiceDialog()
        }
        setupAvatarCardSwipe(b)
        updatePhotoCardImage(b)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAvatarCardSwipe(b: ActivitySettingsContentBinding) {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY) && abs(diffX) > 60 && abs(velocityX) > 100) {
                    val history = AvatarHistoryManager.getMyAvatarHistory(this@SettingsActivity)
                    if (history.size > 1) {
                        val isRightToLeft = diffX < 0
                        currentAvatarIndex = if (isRightToLeft) {
                            (currentAvatarIndex + 1) % history.size
                        } else {
                            if (currentAvatarIndex - 1 < 0) history.size - 1 else currentAvatarIndex - 1
                        }
                        animateAvatarCardSlide(b.ivPhotoCard, isRightToLeft) {
                            val h = AvatarHistoryManager.getMyAvatarHistory(this@SettingsActivity)
                            if (currentAvatarIndex in h.indices) {
                                val selectedUri = h[currentAvatarIndex]
                                AvatarHistoryManager.setActiveMyAvatar(this@SettingsActivity, selectedUri)
                                currentAvatarUri = selectedUri
                                applyAvatarState(selectedUri)
                                Executors.newSingleThreadExecutor().execute {
                                    SettingsSupport.sendProfileUpdateOverBluetooth(this@SettingsActivity)
                                }
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
                else showAvatarChoiceDialog()
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
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                onHalfWay()
                view.translationX = enterX
                view.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(180)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun updatePhotoCardImage(b: ActivitySettingsContentBinding) {
        val history = AvatarHistoryManager.getMyAvatarHistory(this)
        val defaultAvatarDrawable = {
            createLetterAvatarDrawable(
                this,
                "Prime",
                14f * resources.displayMetrics.density,
                (240 * resources.displayMetrics.density).toInt()
            )
        }

        if (history.isEmpty()) {
            b.ivPhotoCard.setImageDrawable(defaultAvatarDrawable())
            animateAvatarCounterBadge(b.tvAvatarCounter, 0, 0)
            currentAvatarUri = null
            return
        }

        if (currentAvatarIndex !in history.indices) {
            currentAvatarIndex = 0
        }

        val avatarUri = history[currentAvatarIndex]
        currentAvatarUri = avatarUri

        animateAvatarCounterBadge(b.tvAvatarCounter, currentAvatarIndex, history.size)

        val radiusPx = (14 * resources.displayMetrics.density).toInt()
        try {
            val (model, file) = parseAvatarModelAndFile(avatarUri)
            if (model != null) {
                val isGif = avatarUri.lowercase().contains(".gif")
                val signatureKey = ObjectKey(
                    if (file != null && file.exists()) "${file.absolutePath}_${file.lastModified()}" else avatarUri
                )

                if (isGif) {
                    Glide.with(this)
                        .asGif()
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(defaultAvatarDrawable())
                        .into(b.ivPhotoCard)
                } else {
                    Glide.with(this)
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .placeholder(defaultAvatarDrawable())
                        .into(b.ivPhotoCard)
                }
            } else {
                b.ivPhotoCard.setImageDrawable(defaultAvatarDrawable())
            }
        } catch (_: Exception) {
            b.ivPhotoCard.setImageDrawable(defaultAvatarDrawable())
        }
    }

    private fun showAvatarChoiceDialog() {
        isPickerForCover = false
        val sheet = PhotoPickerBottomSheet(
            activity = this,
            title = getString(R.string.settings_photo_profile_title),
            allowGif = true,
            showPresets = true,
            onMediaSelected = { uri, isGif ->
                activePhotoPickerSheet = null
                if (isGif) {
                    setGifAvatarDirectly(uri)
                } else {
                    val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
                    val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                        setDataAndType(uri, mime ?: "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("EXTRA_IMAGE_URI", uri.toString())
                        putExtra("IS_PROFILE_PHOTO", true)
                    }
                    photoEditorLauncher.launch(intent)
                    PrimeTransitions.applyOpenTransition(this)
                }
            },
            onPresetSelected = { presetUri ->
                activePhotoPickerSheet = null
                val uriStr = presetUri.toString()
                AvatarHistoryManager.addMyAvatar(this@SettingsActivity, uriStr)
                currentAvatarIndex = 0
                currentAvatarUri = uriStr

                binding?.let { updatePhotoCardImage(it) }
                applyAvatarState(uriStr)
                Executors.newSingleThreadExecutor().execute {
                    SettingsSupport.sendProfileUpdateOverBluetooth(this@SettingsActivity)
                }
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
                PrimeNotification.show(this@SettingsActivity, getString(R.string.settings_avatar_updated_short))
            },
            onOpenSystemPicker = {
                activePhotoPickerSheet = null
                isPickerForCover = false
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRequestPermission = {
                galleryPermissionLauncher.launch(PhotoPickerBottomSheet.getRequiredImagePermissions())
            },
            onDismissWithoutSelection = {
                activePhotoPickerSheet = null
            }
        )
        activePhotoPickerSheet = sheet
        sheet.show()
    }

    private fun setupHeaderListeners(b: ActivitySettingsContentBinding) {
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
                b.nestedScrollView.smoothScrollTo(0, b.layoutSettingsBodyContainer.top)
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
            } else {
                false
            }
        }

        b.btnBackNP.setOnClickListener { handleBack() }
        b.btnChangePhotoNP.setOnClickListener { showAvatarChoiceDialog() }
        b.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        b.btnExtraSettingsNP.setOnClickListener {
            b.nestedScrollView.smoothScrollTo(0, b.layoutSettingsBodyContainer.top)
        }
        b.btnEditCover?.setOnClickListener {
            showCoverOptionDialog()
        }
    }

    private fun setupSectionNavigation(b: ActivitySettingsContentBinding) {
        val openSection: (Class<out BaseSettingsDetailActivity>) -> Unit = { target ->
            startActivity(Intent(this, target))
            PrimeTransitions.applyOpenTransition(this)
        }

        b.cardSectionAccount.setOnClickListener { openSection(SettingsAccountActivity::class.java) }
        b.cardSectionAppearance.setOnClickListener { openSection(SettingsAppearanceActivity::class.java) }
        b.cardSectionChat.setOnClickListener { openSection(SettingsChatActivity::class.java) }
        b.cardSectionBrowser.setOnClickListener { openSection(SettingsBrowserActivity::class.java) }
        b.cardSectionStorage.setOnClickListener { openSection(SettingsStorageActivity::class.java) }
        b.cardSectionPlugins.setOnClickListener { openSection(SettingsPluginsActivity::class.java) }
        b.cardSectionLanguage.setOnClickListener { openSection(SettingsLanguageActivity::class.java) }
        b.cardSectionAbout.setOnClickListener { openSection(SettingsAboutActivity::class.java) }
    }

    private fun setupM3ECardsAndSections(b: ActivitySettingsContentBinding) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        b.tvHubGroupTitle1.setTextColor(accentColor)
        b.tvHubGroupTitle2.setTextColor(accentColor)
        b.tvHubGroupTitle3.setTextColor(accentColor)

        SettingsSupport.applySegmentedGroupShape(b.cardSectionAccount, 0, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardSectionAppearance, 1, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardSectionChat, 2, 3)

        SettingsSupport.applySegmentedGroupShape(b.cardSectionBrowser, 0, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardSectionStorage, 1, 3)
        SettingsSupport.applySegmentedGroupShape(b.cardSectionPlugins, 2, 3)

        SettingsSupport.applySegmentedGroupShape(b.cardSectionLanguage, 0, 2)
        SettingsSupport.applySegmentedGroupShape(b.cardSectionAbout, 1, 2)

        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconAccount, accentColor, M3EShapes.circle, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconAppearance, accentColor, M3EShapes.flower, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconChat, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconBrowser, accentColor, M3EShapes.verySunny, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconStorage, accentColor, M3EShapes.cookie4Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconPlugins, accentColor, M3EShapes.gem, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconLanguage, accentColor, M3EShapes.cookie9Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivHubIconAbout, accentColor, M3EShapes.cookie7Sided, isDark)

        listOf(
            b.cardSectionAccount,
            b.cardSectionAppearance,
            b.cardSectionChat,
            b.cardSectionBrowser,
            b.cardSectionStorage,
            b.cardSectionPlugins,
            b.cardSectionLanguage,
            b.cardSectionAbout
        ).forEach { it.addBounceTouchEffect() }

        val headerButtons = listOfNotNull(
            b.btnBackWP, b.btnChangePhotoWP, b.btnLogoutWP, b.btnExtraSettingsWP,
            b.btnBackNP, b.btnChangePhotoNP, b.btnLogoutNP, b.btnExtraSettingsNP,
            b.btnEditCover
        )
        headerButtons.forEach { it.tag = "do_not_tint" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            b.nestedScrollView.postDelayed({
                val shapes = listOf(
                    M3EShapes.cookie6Sided,
                    M3EShapes.flower,
                    M3EShapes.verySunny,
                    M3EShapes.cookie7Sided,
                    M3EShapes.circle
                )
                val btnBgColor = if (isDark) {
                    ContextCompat.getColor(this, R.color.glass_white_20)
                } else {
                    ContextCompat.getColor(this, R.color.prime_search_bg)
                }
                val iconColor = if (isDark) Color.WHITE else ContextCompat.getColor(this, R.color.prime_text_primary)
                val disableAnimations = PrimeTransitions.areAnimationsDisabled(this)

                for ((index, btn) in headerButtons.withIndex()) {
                    btn.backgroundTintList = null
                    btn.outlineProvider = null
                    btn.clipToOutline = false
                    btn.elevation = 0f
                    btn.background = M3EDrawable(shapes[index % shapes.size], btnBgColor)
                    if (btn == b.btnLogoutWP || btn == b.btnLogoutNP) {
                        btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.prime_danger))
                    } else {
                        btn.iconTint = ColorStateList.valueOf(iconColor)
                    }

                    if (!disableAnimations) {
                        btn.scaleX = 0.5f
                        btn.scaleY = 0.5f
                        btn.alpha = 0f
                        btn.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(300)
                            .setStartDelay((index % 4) * 35L)
                            .setInterpolator(OvershootInterpolator(1.15f))
                            .start()
                    }
                }
            }, 80)
        }

        SettingsSupport.animateStaggeredEntrance(
            this,
            listOf(
                b.tvHubGroupTitle1,
                b.cardSectionAccount,
                b.cardSectionAppearance,
                b.cardSectionChat,
                b.tvHubGroupTitle2,
                b.cardSectionBrowser,
                b.cardSectionStorage,
                b.cardSectionPlugins,
                b.tvHubGroupTitle3,
                b.cardSectionLanguage,
                b.cardSectionAbout
            )
        )

        ColorAccentManager.tintViewTree(b.layoutSettingsBodyContainer, accentColor)
    }

    private fun refreshHubSummaries(b: ActivitySettingsContentBinding) {
        val sp = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sp.getString("current_user", "") ?: ""
        val savedName = sp.getString("my_name", null)
            ?: sp.getString("my_local_name", null)
            ?: sp.getString("current_user_name", null)
            ?: sp.getString("${currentUser}_name", null)
            ?: getString(R.string.settings_default_me)

        b.tvHubSubtitleAccount.text = getString(R.string.settings_hub_account_sub_format, savedName)

        val theme = sp.getString("app_theme", "system") ?: "system"
        b.tvHubSubtitleAppearance.text = getString(
            R.string.settings_hub_appearance_sub_format,
            SettingsSupport.getThemeDisplayName(this, theme),
            ColorAccentManager.getAccentSummary(this)
        )

        val activeWallpaper = WallpaperManager.getActiveWallpaper(this)
        b.tvHubSubtitleChat.text = getString(
            R.string.settings_hub_chat_wallpaper_format,
            WallpaperManager.getDisplayName(this, activeWallpaper)
        )

        val browserEnabled = BrowserPreferences.isInternalBrowserEnabled(this)
        val searchEngine = BrowserPreferences.getSearchEngine(this)
        val searchEngineDisplay = if (searchEngine == "Яндекс") {
            getString(R.string.settings_browser_yandex)
        } else {
            searchEngine
        }
        b.tvHubSubtitleBrowser.text = if (browserEnabled) {
            getString(R.string.settings_hub_browser_sub_format, searchEngineDisplay)
        } else {
            getString(R.string.settings_hub_browser_external_sub)
        }

        val cachedBytes = sp.getLong("cached_storage_bytes", -1L)
        val cachedStorage = if (cachedBytes >= 0L) {
            SettingsSupport.formatStorageSize(cachedBytes)
        } else {
            sp.getString("cached_storage_summary", null)
        }
        b.tvHubSubtitleStorage.text = if (!cachedStorage.isNullOrBlank()) {
            getString(R.string.settings_hub_storage_sub_format, cachedStorage)
        } else {
            getString(R.string.settings_hub_storage_default_sub)
        }

        val plugins = PluginManager.getAllPlugins(this)
        val enabledPlugins = plugins.count { it.isEnabled }
        b.tvHubSubtitlePlugins.text = if (plugins.isEmpty()) {
            getString(R.string.settings_hub_plugins_empty)
        } else {
            getString(R.string.settings_hub_plugins_sub_format, plugins.size, enabledPlugins)
        }

        b.tvHubSubtitleLanguage.text = PrimeLocaleManager.getLanguageDisplayName(this)

        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val vName = pInfo.versionName ?: "1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            getString(R.string.settings_hub_about_sub_format, vName, vCode)
        } catch (_: Exception) {
            getString(R.string.settings_hub_about_sub_simple_format, BuildConfig.VERSION_NAME)
        }
        b.tvHubSubtitleAbout.text = versionName
    }

    private fun togglePhotoMenuMode(enable: Boolean) {
        if (isPhotoMenuMode.value == enable) return
        isPhotoMenuMode.value = enable

        val b = binding ?: return

        if (enable) {
            b.btnBackWP.setIconResource(R.drawable.ic_cancel)
            b.btnBackWP.setIconTintResource(R.color.prime_text_primary)
            b.tvBackLabelWP.text = getString(R.string.settings_close)

            b.btnLogoutWP.setIconResource(R.drawable.ic_person)
            b.btnLogoutWP.setIconTintResource(R.color.prime_text_primary)
            b.tvLogoutWP.text = getString(R.string.settings_view_photo)
            b.tvLogoutWP.setTextColor(ContextCompat.getColor(this, R.color.prime_text_primary))

            b.btnExtraSettingsWP.setIconResource(R.drawable.ic_trash)
            b.btnExtraSettingsWP.setIconTintResource(R.color.prime_danger)
            b.tvExtraSettingsWP.text = getString(R.string.action_delete)
            b.tvExtraSettingsWP.setTextColor(ContextCompat.getColor(this, R.color.prime_danger))

            startWobbling(b)
        } else {
            b.btnBackWP.setIconResource(R.drawable.ic_arrow_back)
            b.btnBackWP.setIconTintResource(R.color.prime_text_primary)
            b.tvBackLabelWP.text = getString(R.string.action_back)

            b.btnLogoutWP.setIconResource(R.drawable.ic_exit_to_app)
            b.btnLogoutWP.setIconTintResource(R.color.prime_danger)
            b.tvLogoutWP.text = getString(R.string.settings_logout)
            b.tvLogoutWP.setTextColor(ContextCompat.getColor(this, R.color.prime_danger))

            b.btnExtraSettingsWP.setIconResource(R.drawable.ic_settings)
            b.btnExtraSettingsWP.setIconTintResource(R.color.prime_text_primary)
            b.tvExtraSettingsWP.text = getString(R.string.settings_nav_to_settings)
            b.tvExtraSettingsWP.setTextColor(ContextCompat.getColor(this, R.color.prime_text_primary))

            stopWobbling()
        }
        updateBackCallbackState()
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
            listOf(
                b.btnBackWPLayout,
                b.btnChangePhotoWPLayout,
                b.btnLogoutWPLayout,
                b.btnExtraSettingsWPLayout
            ).forEach { it.rotation = 0f }
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
        updatePhotoCardImage(b)
    }

    private fun openFullPhoto() {
        val myHistory = AvatarHistoryManager.getMyAvatarHistory(this)
        if (myHistory.isEmpty()) {
            PrimeNotification.show(this, getString(R.string.settings_photo_not_set))
            return
        }
        if (isClosing) return

        val myName = currentNameInDB.ifEmpty { getString(R.string.settings_default_me) }
        val mediaList = myHistory.mapIndexed { idx, uriStr ->
            ChatMessage(
                getString(R.string.settings_avatar_title),
                if (idx == 0) getString(R.string.settings_avatar_you_active) else getString(R.string.settings_avatar_you),
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
        startActivity(Intent(this, MediaPlayerActivity::class.java))
        PrimeTransitions.applyOpenTransition(this)
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (!uriToDelete.isNullOrEmpty()) {
            AvatarHistoryManager.removeMyAvatar(this, uriToDelete, deleteFile = false)
        }

        val history = AvatarHistoryManager.getMyAvatarHistory(this)
        if (history.isNotEmpty()) {
            currentAvatarIndex = 0
            currentAvatarUri = history[0]
            applyAvatarState(history[0])
        } else {
            currentAvatarIndex = 0
            currentAvatarUri = null
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

        binding?.let { updatePhotoCardImage(it) }
        val backupUri = uriToDelete
        Executors.newSingleThreadExecutor().execute {
            SettingsSupport.sendProfileUpdateOverBluetooth(this)
        }
        sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
        PrimeNotification.show(this, getString(R.string.settings_photo_deleted), onUndo = {
            if (!backupUri.isNullOrEmpty()) {
                AvatarHistoryManager.addMyAvatar(this@SettingsActivity, backupUri)
                currentAvatarUri = backupUri
                applyAvatarState(backupUri)
                binding?.let { updatePhotoCardImage(it) }
                Executors.newSingleThreadExecutor().execute {
                    SettingsSupport.sendProfileUpdateOverBluetooth(this@SettingsActivity)
                }
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
            }
        })
    }

    private fun showLogoutDialog() {
        PrimeBlurDialog.show(
            activity = this,
            title = getString(R.string.settings_logout_prompt_title),
            message = getString(R.string.settings_logout_prompt_msg),
            positiveText = getString(R.string.common_yes),
            negativeText = getString(R.string.common_no),
            isPositiveDanger = true,
            onPositive = {
                val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                BluetoothConnectionManager.getInstance().broadcastPacket(9.toByte(), "DISCONNECT".toByteArray(Charsets.UTF_8))
                Thread.sleep(100)
                BluetoothConnectionManager.getInstance().disconnect()
                BluetoothSocketHolder.clearSocket()
                PrimeBluetoothService.stopService(this)
                sp.edit()
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

    private fun setupBottomNav(b: ActivitySettingsContentBinding? = binding) {
        val bindingRef = b ?: binding ?: return
        val checkTabletOrEmbedded = {
            resources.configuration.smallestScreenWidthDp >= 600 ||
                try {
                    androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
                } catch (_: Exception) {
                    false
                }
        }
        val blurTarget = (bindingRef.root as? ViewGroup)
            ?: window.decorView.findViewById<ViewGroup>(android.R.id.content)
        PrimeBottomNavHelper.setup(
            activity = this,
            activeTab = PrimeBottomNavHelper.NavTab.PROFILE,
            blurTargetRoot = blurTarget,
            hideOnTabletOrEmbedded = true,
            onSameTabReselect = {
                binding?.nestedScrollView?.smoothScrollTo(0, 0)
            },
            onTabClickOverride = { tab ->
                if ((tab == PrimeBottomNavHelper.NavTab.CHATS || tab == PrimeBottomNavHelper.NavTab.DEVICES) && checkTabletOrEmbedded()) {
                    finish()
                    PrimeTransitions.applyCloseTransition(this)
                    true
                } else {
                    false
                }
            }
        )
        applyBottomBarInsets(bindingRef)
    }

    override fun onResume() {
        super.onResume()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val latestTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
        val latestAccent = ColorAccentManager.getCurrentAccentColor(this)
        val latestLanguageTag = PrimeLocaleManager.getCurrentLanguageTag(this)
        if (latestTheme != initialTheme || latestAccent != initialAccentColor || latestLanguageTag != initialLanguageTag) {
            recreate()
            return
        }

        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.CHAT_DELETED")
            addAction("com.messenger.prime.NAV_STYLE_CHANGED")
            addAction("com.messenger.prime.AVATAR_CHANGED")
            addAction("com.messenger.prime.COVER_CHANGED")
            addAction("com.messenger.prime.WALLPAPER_CHANGED")
            addAction("com.messenger.prime.THEME_CHANGED")
            addAction("com.messenger.prime.ACCENT_CHANGED")
            addAction(PrimeLocaleManager.ACTION_LANGUAGE_CHANGED)
            addAction(PluginManager.ACTION_PLUGINS_CHANGED)
        }
        ContextCompat.registerReceiver(this, chatDeletedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setupBottomNav()
        LavaBackgroundState.onActivityResumed()
        isClosing = false

        val b = binding
        if (b != null && isPhotoMenuMode.value) startWobbling(b)

        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser

        val savedName = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", null)
            ?: getString(R.string.settings_default_me)
        val savedAvatarUri = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)

        currentAvatarUri = savedAvatarUri
        currentNameInDB = savedName

        if (b != null) {
            b.tvUserNameStatic.text = savedName
            b.tvUserNameWP.text = savedName
            b.tvUserNameWP.isSelected = true
            updatePhotoCardImage(b)
            applyAvatarState(savedAvatarUri)
            updateHeaderCover()
            refreshHubSummaries(b)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(chatDeletedReceiver)
        } catch (_: Exception) {}
        stopWobbling()
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }

    override fun onDestroy() {
        stopWobbling()
        super.onDestroy()
    }

    @SuppressLint("InternalInsetResource", "Discouraged")
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            (36 * resources.displayMetrics.density).toInt()
        }
    }
}
