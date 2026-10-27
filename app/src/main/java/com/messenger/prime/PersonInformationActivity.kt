package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.messenger.prime.databinding.ActivityPersonInformationBinding
import com.messenger.prime.databinding.ItemGalleryMediaBinding
import com.messenger.prime.databinding.ItemPersonDateHeaderBinding
import com.messenger.prime.databinding.ItemPersonFileRowBinding
import com.messenger.prime.databinding.ItemPersonMusicRowBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class PersonInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonInformationBinding

    private var targetUsername: String = ""
    private var deviceAddress: String? = null
    private var avatarUriStr: String? = null
    private var isOnline: Boolean = false

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.messenger.prime.STATUS_UPDATED" -> {
                    updateLiveStatus()
                }
                "com.messenger.prime.AVATAR_UPDATED", "com.messenger.prime.AVATAR_CHANGED", "com.messenger.prime.NAME_CHANGED" -> {
                    setupHeaderUi()
                }
                "com.messenger.prime.CHAT_DELETED" -> {
                    Toast.makeText(this@PersonInformationActivity, "Чат удален", Toast.LENGTH_SHORT).show()
                    supportFinishAfterTransition()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

        val config = resources.configuration
        val isFoldableOrTablet = config.smallestScreenWidthDp >= 600
        if (isFoldableOrTablet) {
            val density = resources.displayMetrics.density
            val targetWidth = (380 * density).toInt()
            window.setLayout(targetWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.START or Gravity.TOP)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.25f)
        }

        val isDark = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDarkIcons = !isDark)

        binding = ActivityPersonInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val density = resources.displayMetrics.density
            val extraPadding = (12 * density).toInt()
            val topInset = if (systemBars.top > 0) systemBars.top else (24 * density).toInt()

            binding.root.setPadding(cutout.left, 0, cutout.right, 0)

            binding.layoutWithPhoto.setPadding(
                binding.layoutWithPhoto.paddingLeft,
                maxOf(topInset, cutout.top) + extraPadding,
                binding.layoutWithPhoto.paddingRight,
                extraPadding
            )
            binding.nestedScrollView.setPadding(
                binding.nestedScrollView.paddingLeft,
                binding.nestedScrollView.paddingTop,
                binding.nestedScrollView.paddingRight,
                maxOf(systemBars.bottom, cutout.bottom)
            )

            val lp = binding.blurStickyToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null && lp.topMargin != 0) {
                lp.topMargin = 0
                binding.blurStickyToolbar.layoutParams = lp
            }
            binding.blurStickyToolbar.setPadding(0, maxOf(topInset, cutout.top), 0, 0)

            insets
        }

        targetUsername = intent.getStringExtra("EXTRA_CHAT_NAME") ?: "Пользователь"
        deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS")
        avatarUriStr = intent.getStringExtra("EXTRA_AVATAR_URI")
        isOnline = intent.getBooleanExtra("EXTRA_IS_ONLINE", false)




        // PrimePredictiveBack is now attached centrally in PrimeApplication

        val filter = IntentFilter().apply {
            addAction("com.messenger.prime.STATUS_UPDATED")
            addAction("com.messenger.prime.AVATAR_UPDATED")
            addAction("com.messenger.prime.CHAT_DELETED")
            addAction("com.messenger.prime.AVATAR_CHANGED")
            addAction("com.messenger.prime.NAME_CHANGED")
        }
        ContextCompat.registerReceiver(
            this, statusUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setupThemeAndBackground()
        setupHeaderUi()
        setupLeftColumnButtons()
        setupCategoryTabs()
        updateLiveStatus()
        PrimeTransitions.protectHorizontalScrollViews(binding.root as android.view.ViewGroup)
        PrimeMusicManager.addListener(playerListener)
        loadSharedMediaAndFiles()
    }

    private val liveStatusHandler = Handler(Looper.getMainLooper())
    private val liveStatusRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                updateLiveStatus()
                liveStatusHandler.postDelayed(this, 2000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            ColorAccentManager.tintViewTree(binding.root, ColorAccentManager.getCurrentAccentColor(this))
        }
        setupHeaderUi()
        updateLiveStatus()
        liveStatusHandler.removeCallbacks(liveStatusRunnable)
        liveStatusHandler.post(liveStatusRunnable)
    }

    override fun onPause() {
        super.onPause()
        liveStatusHandler.removeCallbacks(liveStatusRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(statusUpdateReceiver)
        } catch (ignored: Exception) {}
        PrimeMusicManager.removeListener(playerListener)
    }

    private val playerListener = object : PrimeMusicManager.PlayerListener {
        override fun onTrackChanged(track: TrackItem?) {
            runOnUiThread {
                binding.rvSharedMusic.adapter?.notifyDataSetChanged()
            }
        }

        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            runOnUiThread {
                binding.rvSharedMusic.adapter?.notifyDataSetChanged()
            }
        }

        override fun onProgressUpdated(positionMs: Long, durationMs: Long) {}
    }

    private fun setupCategoryTabs() {
        // Switch between media, files, links, music
        binding.tabPhotoVideo.setOnClickListener { selectTab(0) }
        binding.tabFiles.setOnClickListener { selectTab(1) }
        binding.tabGifs.setOnClickListener { selectTab(2) }
        binding.tabMusic.setOnClickListener { selectTab(3) }

        binding.tabPhotoVideoSticky.setOnClickListener { selectTab(0) }
        binding.tabFilesSticky.setOnClickListener { selectTab(1) }
        binding.tabGifsSticky.setOnClickListener { selectTab(2) }
        binding.tabMusicSticky.setOnClickListener { selectTab(3) }

        selectTab(0)
    }

    private fun selectTab(index: Int) {
        val selectedBg = R.drawable.bg_date_bubble
        val defaultBg = android.R.color.transparent
        val context = this
        val colorBrand = ContextCompat.getColor(context, R.color.prime_brand)
        val colorSec = ContextCompat.getColor(context, R.color.prime_text_secondary)

        // Reset main tabs
        binding.tabPhotoVideo.setBackgroundResource(defaultBg)
        binding.tabFiles.setBackgroundResource(defaultBg)
        binding.tabGifs.setBackgroundResource(defaultBg)
        binding.tabMusic.setBackgroundResource(defaultBg)

        binding.ivTabPhotoIcon.setColorFilter(colorSec)
        binding.tvTabPhotoTitle.setTextColor(colorSec)
        binding.tvTabPhotoCount.setTextColor(colorSec)

        binding.ivTabFilesIcon.setColorFilter(colorSec)
        binding.tvTabFilesTitle.setTextColor(colorSec)
        binding.tvTabFilesCount.setTextColor(colorSec)

        binding.ivTabGifsIcon.setColorFilter(colorSec)
        binding.tvTabGifsTitle.setTextColor(colorSec)
        binding.tvTabGifsCount.setTextColor(colorSec)

        binding.ivTabMusicIcon.setColorFilter(colorSec)
        binding.tvTabMusicTitle.setTextColor(colorSec)
        binding.tvTabMusicCount.setTextColor(colorSec)

        // Reset sticky tabs
        binding.tabPhotoVideoSticky.setBackgroundResource(defaultBg)
        binding.tabFilesSticky.setBackgroundResource(defaultBg)
        binding.tabGifsSticky.setBackgroundResource(defaultBg)
        binding.tabMusicSticky.setBackgroundResource(defaultBg)

        binding.ivTabPhotoIconSticky.setColorFilter(colorSec)
        binding.tvTabPhotoTitleSticky.setTextColor(colorSec)
        binding.tvTabPhotoCountSticky.setTextColor(colorSec)

        binding.ivTabFilesIconSticky.setColorFilter(colorSec)
        binding.tvTabFilesTitleSticky.setTextColor(colorSec)
        binding.tvTabFilesCountSticky.setTextColor(colorSec)

        binding.ivTabGifsIconSticky.setColorFilter(colorSec)
        binding.tvTabGifsTitleSticky.setTextColor(colorSec)
        binding.tvTabGifsCountSticky.setTextColor(colorSec)

        binding.ivTabMusicIconSticky.setColorFilter(colorSec)
        binding.tvTabMusicTitleSticky.setTextColor(colorSec)
        binding.tvTabMusicCountSticky.setTextColor(colorSec)

        when (index) {
            0 -> {
                binding.tabPhotoVideo.setBackgroundResource(selectedBg)
                binding.ivTabPhotoIcon.setColorFilter(colorBrand)
                binding.tvTabPhotoTitle.setTextColor(colorBrand)
                binding.tvTabPhotoCount.setTextColor(colorBrand)

                binding.tabPhotoVideoSticky.setBackgroundResource(selectedBg)
                binding.ivTabPhotoIconSticky.setColorFilter(colorBrand)
                binding.tvTabPhotoTitleSticky.setTextColor(colorBrand)
                binding.tvTabPhotoCountSticky.setTextColor(colorBrand)

                binding.tvSectionHeaderTitle.text = "Фото и видео"
                binding.ivSectionHeaderIcon.setImageResource(R.drawable.ic_photo)
                binding.rvSharedMedia.visibility = if (binding.tvTabPhotoCount.text == "0") View.GONE else View.VISIBLE
                binding.rvSharedFiles.visibility = View.GONE
                binding.rvSharedGifs.visibility = View.GONE
                binding.rvSharedMusic.visibility = View.GONE
                binding.tvEmptySection.visibility = if (binding.tvTabPhotoCount.text == "0") View.VISIBLE else View.GONE
                binding.tvEmptySection.text = "Нет фотографий и видео"
            }
            1 -> {
                binding.tabFiles.setBackgroundResource(selectedBg)
                binding.ivTabFilesIcon.setColorFilter(colorBrand)
                binding.tvTabFilesTitle.setTextColor(colorBrand)
                binding.tvTabFilesCount.setTextColor(colorBrand)

                binding.tabFilesSticky.setBackgroundResource(selectedBg)
                binding.ivTabFilesIconSticky.setColorFilter(colorBrand)
                binding.tvTabFilesTitleSticky.setTextColor(colorBrand)
                binding.tvTabFilesCountSticky.setTextColor(colorBrand)

                binding.tvSectionHeaderTitle.text = "Файлы"
                binding.ivSectionHeaderIcon.setImageResource(R.drawable.ic_file)
                binding.rvSharedMedia.visibility = View.GONE
                binding.rvSharedFiles.visibility = if (binding.tvTabFilesCount.text == "0") View.GONE else View.VISIBLE
                binding.rvSharedGifs.visibility = View.GONE
                binding.rvSharedMusic.visibility = View.GONE
                binding.tvEmptySection.visibility = if (binding.tvTabFilesCount.text == "0") View.VISIBLE else View.GONE
                binding.tvEmptySection.text = "Нет отправленных файлов"
            }
            2 -> {
                binding.tabGifs.setBackgroundResource(selectedBg)
                binding.ivTabGifsIcon.setColorFilter(colorBrand)
                binding.tvTabGifsTitle.setTextColor(colorBrand)
                binding.tvTabGifsCount.setTextColor(colorBrand)

                binding.tabGifsSticky.setBackgroundResource(selectedBg)
                binding.ivTabGifsIconSticky.setColorFilter(colorBrand)
                binding.tvTabGifsTitleSticky.setTextColor(colorBrand)
                binding.tvTabGifsCountSticky.setTextColor(colorBrand)

                binding.tvSectionHeaderTitle.text = "GIF"
                binding.ivSectionHeaderIcon.setImageResource(R.drawable.ic_prime_gif)
                binding.rvSharedMedia.visibility = View.GONE
                binding.rvSharedFiles.visibility = View.GONE
                binding.rvSharedGifs.visibility = if (binding.tvTabGifsCount.text == "0") View.GONE else View.VISIBLE
                binding.rvSharedMusic.visibility = View.GONE
                binding.tvEmptySection.visibility = if (binding.tvTabGifsCount.text == "0") View.VISIBLE else View.GONE
                binding.tvEmptySection.text = "Нет отправленных GIF"
            }
            3 -> {
                binding.tabMusic.setBackgroundResource(selectedBg)
                binding.ivTabMusicIcon.setColorFilter(colorBrand)
                binding.tvTabMusicTitle.setTextColor(colorBrand)
                binding.tvTabMusicCount.setTextColor(colorBrand)

                binding.tabMusicSticky.setBackgroundResource(selectedBg)
                binding.ivTabMusicIconSticky.setColorFilter(colorBrand)
                binding.tvTabMusicTitleSticky.setTextColor(colorBrand)
                binding.tvTabMusicCountSticky.setTextColor(colorBrand)

                binding.tvSectionHeaderTitle.text = "Музыка"
                binding.ivSectionHeaderIcon.setImageResource(R.drawable.ic_music)
                binding.rvSharedMedia.visibility = View.GONE
                binding.rvSharedFiles.visibility = View.GONE
                binding.rvSharedGifs.visibility = View.GONE
                binding.rvSharedMusic.visibility = if (binding.tvTabMusicCount.text == "0") View.GONE else View.VISIBLE
                binding.tvEmptySection.visibility = if (binding.tvTabMusicCount.text == "0") View.VISIBLE else View.GONE
                binding.tvEmptySection.text = "Нет аудиозаписей"
            }
        }
    }

    private var hasAnySharedMedia = false
    private var isToolbarVisible = false

    private fun setupStickyBlurToolbar(darkTheme: Boolean) {
        val config = resources.configuration
        val isTablet = config.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
        } catch (_: Throwable) {
            false
        }
        val isTabletOrFoldable = isTablet || isEmbedded

        if (isTabletOrFoldable) {
            binding.blurStickyToolbar.visibility = View.GONE
            return
        }

        fun getActualTopInset(): Int {
            val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
            val topFromInsets = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            return if (topFromInsets > 0) topFromInsets else getStatusBarHeight()
        }

        fun updateToolbarPadding() {
            val topInset = getActualTopInset()
            val lp = binding.blurStickyToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null && lp.topMargin != 0) {
                lp.topMargin = 0
                binding.blurStickyToolbar.layoutParams = lp
            }
            binding.blurStickyToolbar.setPadding(0, topInset, 0, 0)
        }

        updateToolbarPadding()

        ViewCompat.setOnApplyWindowInsetsListener(binding.blurStickyToolbar) { _, insets ->
            updateToolbarPadding()
            insets
        }
        binding.blurStickyToolbar.post {
            updateToolbarPadding()
        }

        val overlayColor = if (darkTheme) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
        val targetRootView = (binding.personInfoRoot as? ViewGroup)
            ?: (window.decorView.findViewById<ViewGroup>(android.R.id.content))
            ?: (window.decorView as ViewGroup)

        binding.blurStickyToolbar.setupBlur(
            rootView = targetRootView,
            blurRadius = 18f,
            tintColor = overlayColor,
            frameClearDrawable = window.decorView.background
        )

        binding.btnStickyBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        updateStickyHeaderData()

        binding.nestedScrollView.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            val photoCard = binding.photoCard
            if (!photoCard.isAttachedToWindow) return@OnScrollChangeListener

            val locationPhoto = IntArray(2)
            photoCard.getLocationOnScreen(locationPhoto)

            val locationScroll = IntArray(2)
            binding.nestedScrollView.getLocationOnScreen(locationScroll)

            val photoBottomRelative = (locationPhoto[1] + photoCard.height) - locationScroll[1]
            val toolbarHeight = binding.blurStickyToolbar.height.coerceAtLeast(1)

            val shouldShow = hasAnySharedMedia && (photoBottomRelative <= toolbarHeight + 12)

            if (shouldShow != isToolbarVisible) {
                isToolbarVisible = shouldShow
                binding.blurStickyToolbar.animate().cancel()
                if (shouldShow) {
                    binding.blurStickyToolbar.visibility = View.VISIBLE
                    binding.blurStickyToolbar.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .setListener(null)
                        .start()
                } else {
                    binding.blurStickyToolbar.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .setListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                if (!isToolbarVisible) {
                                    binding.blurStickyToolbar.visibility = View.GONE
                                }
                            }
                        })
                        .start()
                }
            }
        })
    }

    @SuppressLint("InternalInsetResource", "Discouraged")
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (24 * resources.displayMetrics.density).toInt()
    }

    private fun updateStickyHeaderData() {
        if (!::binding.isInitialized) return
        binding.tvStickyName.text = binding.tvUserNameWP.text
        binding.tvStickyStatus.animateSlideDownText(binding.tvStatusWP.text)
        binding.tvStickyStatus.setTextColor(binding.tvStatusWP.currentTextColor)
    }

    private fun setupThemeAndBackground() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val themePref = sharedPrefs.getString("app_theme", "system") ?: "system"
        val darkTheme = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        binding.nestedScrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.prime_base))
        binding.composeHeaderBackground.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PrimeTheme(darkTheme = darkTheme) {
                    AnimatedBackground(darkTheme = darkTheme, ignoreSettingsToggle = false)
                }
            }
        }

        setupStickyBlurToolbar(darkTheme)
    }

    private var currentAvatarIndex = 0

    private fun getCombinedContactAvatarHistory(): List<String> {
        val list = mutableListOf<String>()
        fun addAllUnique(items: List<String>) {
            for (item in items) {
                if (item.isNotBlank() && !list.contains(item)) {
                    list.add(item)
                }
            }
        }

        if (!deviceAddress.isNullOrEmpty()) {
            addAllUnique(AvatarHistoryManager.getContactAvatarHistory(this, deviceAddress!!))
        }
        if (!targetUsername.isNullOrEmpty()) {
            addAllUnique(AvatarHistoryManager.getContactAvatarHistory(this, targetUsername))
        }

        if (list.isEmpty() && !avatarUriStr.isNullOrEmpty()) {
            val (_, file) = parseAvatarModelAndFile(avatarUriStr)
            if (file == null || file.exists()) {
                if (!deviceAddress.isNullOrEmpty()) {
                    AvatarHistoryManager.addContactAvatar(this, deviceAddress!!, avatarUriStr!!)
                }
                if (!targetUsername.isNullOrEmpty()) {
                    AvatarHistoryManager.addContactAvatar(this, targetUsername, avatarUriStr!!)
                }
                if (!deviceAddress.isNullOrEmpty()) {
                    addAllUnique(AvatarHistoryManager.getContactAvatarHistory(this, deviceAddress!!))
                }
                if (!targetUsername.isNullOrEmpty()) {
                    addAllUnique(AvatarHistoryManager.getContactAvatarHistory(this, targetUsername))
                }
            }
        }
        return list
    }

    private fun setupHeaderUi() {
        var displayContactName = AvatarManager.getContactDisplayName(this, targetUsername, deviceAddress, targetUsername)
        if (displayContactName.isBlank() || displayContactName == "Пользователь" || displayContactName == "Собеседник") {
            val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val nameFromAddr = if (!deviceAddress.isNullOrEmpty()) sp.getString("contact_name_$deviceAddress", null) else null
            val nameFromUser = if (!targetUsername.isNullOrEmpty()) sp.getString("contact_name_$targetUsername", null) else null
            if (!nameFromAddr.isNullOrBlank()) displayContactName = nameFromAddr
            else if (!nameFromUser.isNullOrBlank()) displayContactName = nameFromUser
        }
        val cleanName = displayContactName.replace("💻", "").trim()
        val isDesktop = AvatarManager.isDesktopContact(this, targetUsername, deviceAddress)
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        binding.tvUserNameWP.text = AvatarManager.formatNameWithDesktopIcon(this, cleanName, isDesktop, accentColor)

        setupAvatarCardSwipe()
        updateHeaderPhoto()
        updateStickyHeaderData()
    }

    private fun updateHeaderPhoto() {
        val history = getCombinedContactAvatarHistory()

        if (history.isEmpty()) {
            binding.ivPhotoCard.setImageResource(R.drawable.ic_person)
            animateAvatarCounterBadge(binding.tvAvatarCounter, 0, 0)
            avatarUriStr = null
            return
        }

        if (currentAvatarIndex !in history.indices) {
            currentAvatarIndex = 0
        }

        val uriStr = history[currentAvatarIndex]
        avatarUriStr = uriStr

        animateAvatarCounterBadge(binding.tvAvatarCounter, currentAvatarIndex, history.size)

        val radiusPx = (14 * resources.displayMetrics.density).toInt()
        try {
            val (model, file) = parseAvatarModelAndFile(uriStr)
            if (model != null) {
                val signatureKey = ObjectKey(if (file != null && file.exists()) "${file.absolutePath}_${file.lastModified()}" else uriStr)
                val isGif = uriStr.lowercase().contains(".gif")

                if (isGif) {
                    Glide.with(this)
                        .asGif()
                        .load(model)
                        .centerCrop()
                        .signature(signatureKey)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivPhotoCard)
                } else {
                    Glide.with(this)
                        .load(model)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .signature(signatureKey)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivPhotoCard)
                }
            } else {
                binding.ivPhotoCard.setImageResource(R.drawable.ic_person)
            }
        } catch (_: Exception) {
            binding.ivPhotoCard.setImageResource(R.drawable.ic_person)
        }
    }

    private fun setupAvatarCardSwipe() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 60 && Math.abs(velocityX) > 100) {
                    val history = getCombinedContactAvatarHistory()
                    if (history.size > 1) {
                        val isRightToLeft = diffX < 0
                        if (isRightToLeft) {
                            currentAvatarIndex = (currentAvatarIndex + 1) % history.size
                        } else {
                            currentAvatarIndex = if (currentAvatarIndex - 1 < 0) history.size - 1 else currentAvatarIndex - 1
                        }
                        animateAvatarCardSlide(binding.ivPhotoCard, isRightToLeft) {
                            updateHeaderPhoto()
                        }
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                openAvatarInMediaPlayer()
                return true
            }
        })

        binding.photoCard.setOnTouchListener { _, event ->
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

    private fun openAvatarInMediaPlayer() {
        val contactHistory = getCombinedContactAvatarHistory()

        if (contactHistory.isNotEmpty()) {
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val targetKey = if (!deviceAddress.isNullOrEmpty()) deviceAddress!! else targetUsername
            val contactName = if (targetKey.isNotEmpty()) {
                sharedPrefs.getString("contact_name_$targetKey", null) ?: targetUsername
            } else targetUsername

            val displayContactName = if (BluetoothAdapter.checkBluetoothAddress(contactName)) "Собеседник" else contactName

            val mediaList = contactHistory.mapIndexed { idx, uriStr ->
                ChatMessage(
                    "Аватар профиля",
                    if (idx == 0) "В прайме! (Активный)" else "В прайме!",
                    displayContactName,
                    false,
                    null,
                    0L,
                    uriStr,
                    "contact_avatar_preview_$idx"
                )
            }

            val startIdx = currentAvatarIndex.coerceIn(0, mediaList.size - 1)
            MediaPlayerActivity.setSharedMediaList(mediaList, startIdx)
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
        } else {
            Toast.makeText(this, "Фотография не установлена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLiveStatus() {
        val isConnected = (!deviceAddress.isNullOrEmpty() && BluetoothConnectionManager.getInstance().isConnected(deviceAddress))
            || (!targetUsername.isNullOrEmpty() && BluetoothConnectionManager.getInstance().isConnected(targetUsername))

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val state1 = sharedPrefs.getString("activity_state_$targetUsername", "IDLE") ?: "IDLE"
        val state2 = if (!deviceAddress.isNullOrEmpty()) sharedPrefs.getString("activity_state_$deviceAddress", "IDLE") ?: "IDLE" else "IDLE"
        val typing1 = sharedPrefs.getLong("typing_until_$targetUsername", 0L)
        val typing2 = if (!deviceAddress.isNullOrEmpty()) sharedPrefs.getLong("typing_until_$deviceAddress", 0L) else 0L
        val now = System.currentTimeMillis()

        val activeState = if (!"IDLE".equals(state1, ignoreCase = true)) state1 else state2
        val isTyping = typing1 > now || typing2 > now || "TYPING".equals(state1, ignoreCase = true) || "TYPING".equals(state2, ignoreCase = true)

        val statusText = when {
            !isConnected -> {
                val lastSeen1 = sharedPrefs.getLong("last_seen_$targetUsername", 0L)
                val lastSeen2 = if (!deviceAddress.isNullOrEmpty()) sharedPrefs.getLong("last_seen_$deviceAddress", 0L) else 0L
                val lastSeen = maxOf(lastSeen1, lastSeen2)
                if (lastSeen > 0) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "был(а) в " + sdf.format(Date(lastSeen))
                } else {
                    "не в сети"
                }
            }
            isTyping -> "печатает..."
            "RECORDING_VOICE".equals(activeState, ignoreCase = true) || "RECORDING_AUDIO".equals(activeState, ignoreCase = true) -> "записывает голосовое..."
            "RECORDING_VIDEO_NOTE".equals(activeState, ignoreCase = true) -> "записывает видеокружок..."
            "SENDING_VIDEO_NOTE".equals(activeState, ignoreCase = true) -> "отправка видеокружка..."
            "SELECTING_MEDIA".equals(activeState, ignoreCase = true) || "SELECTING_FILE".equals(activeState, ignoreCase = true) -> "выбирает файл или медиа..."
            "VIEWING_PHOTO".equals(activeState, ignoreCase = true) -> "смотрит фото"
            "VIEWING_VIDEO".equals(activeState, ignoreCase = true) -> "смотрит видео"
            "VIEWING_FILE".equals(activeState, ignoreCase = true) -> "смотрит файл"
            "SENDING_PHOTO".equals(activeState, ignoreCase = true) || "SENDING_VIDEO".equals(activeState, ignoreCase = true) || "SENDING_FILE".equals(activeState, ignoreCase = true) || "SENDING_MEDIA".equals(activeState, ignoreCase = true) -> "Отправка медиа..."
            "READING_MODE".equals(activeState, ignoreCase = true) -> "в режиме чтения"
            "LISTENING_MUSIC".equals(activeState, ignoreCase = true) || "LISTENING_AUDIO".equals(activeState, ignoreCase = true) || activeState.startsWith("LISTENING_TRACK", ignoreCase = true) -> "слушает аудио..."
            else -> "в сети"
        }

        binding.tvStatusWP.animateSlideDownText(statusText)
        binding.tvStatusWP.setTextColor(
            if (isConnected) ContextCompat.getColor(this, R.color.prime_success)
            else ContextCompat.getColor(this, R.color.white)
        )
        updateStickyHeaderData()
    }

    private fun setupLeftColumnButtons() {
        binding.photoCard.addBounceTouchEffect()
        binding.btnViewPhoto.addBounceTouchEffect()
        binding.btnDisconnect.addBounceTouchEffect()
        binding.btnDeleteChat.addBounceTouchEffect()

        // 1. Назад
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 2. Просмотр фото
        binding.btnViewPhoto.setOnClickListener {
            openAvatarInMediaPlayer()
        }

        // 3. Отключиться
        binding.btnDisconnect.setOnClickListener {
            val targetAddr = deviceAddress ?: targetUsername
            BluetoothConnectionManager.getInstance().disconnect(targetAddr)

            val disconnectIntent = Intent("com.messenger.prime.DISCONNECT_REQUESTED").setPackage(packageName)
            sendBroadcast(disconnectIntent)

            updateLiveStatus()
            Toast.makeText(this, "Подключение отключено", Toast.LENGTH_SHORT).show()
        }

        // 4. Удалить чат
        binding.btnDeleteChat.setOnClickListener {
            showBlurDeleteChatDialog()
        }
    }

    private fun showBlurDeleteChatDialog() {
        val displayTitleName = if (BluetoothAdapter.checkBluetoothAddress(targetUsername)) "собеседником" else targetUsername
        PrimeBlurDialog.show(
            activity = this,
            title = "Удалить чат с $displayTitleName?",
            message = "Вы уверены, что хотите полностью удалить данный чат и очистить всю переписку?",
            positiveText = "Удалить",
            negativeText = "Отмена",
            isPositiveDanger = true,
            onPositive = {
                performCompleteChatDeletion()
            }
        )
    }

    private fun performCompleteChatDeletion() {
        val targetAddr = deviceAddress ?: targetUsername
        if (BluetoothConnectionManager.getInstance().isConnected(targetAddr)) {
            val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val currentUser = sp.getString("current_user", "") ?: ""
            val deletionPayload = "DELETE_CHAT:login=$currentUser;name=$currentUser"
            BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 8.toByte(), deletionPayload.toByteArray(Charsets.UTF_8))
            PrimeCompanionServer.broadcastPacket(8.toByte(), deletionPayload.toByteArray(Charsets.UTF_8))
            
            Handler(Looper.getMainLooper()).postDelayed({
                finalizeChatDeletion()
            }, 350)
        } else {
            finalizeChatDeletion()
        }
    }

    @SuppressLint("MissingPermission")
    private fun finalizeChatDeletion() {
        val targetAddr = deviceAddress ?: targetUsername

        // Ensure MAC is not blocked
        try {
            BluetoothConnectionManager.getInstance().unblockMacAddress(targetAddr)
            deviceAddress?.let {
                if (it.isNotEmpty()) BluetoothConnectionManager.getInstance().unblockMacAddress(it)
            }
        } catch (_: Exception) {}

        // 1. Disconnect current socket FIRST & cancel background reconnects
        try {
            BluetoothConnectionManager.getInstance().disconnect(targetAddr)
            deviceAddress?.let {
                if (it.isNotEmpty()) {
                    BluetoothConnectionManager.getInstance().disconnect(it)
                    PrimeBluetoothService.cancelReconnect(this, it)
                }
            }
            PrimeBluetoothService.cancelReconnect(this, targetAddr)
        } catch (_: Exception) {}

        // 2. Unbond / remove bond from Bluetooth adapter
        DeviceBindingManager.unbindDeviceSecurely(this, targetUsername, deviceAddress)

        if (!BluetoothSocketHolder.hasAnyActiveConnection()) {
            PrimeBluetoothService.stopService(this)
        }

        // 3. Complete cleanup of history, avatars, files, and SharedPreferences keys
        AvatarManager.clearContactAvatar(this, targetUsername, targetUsername, deviceAddress)
        ChatHistoryManager.deleteHistoryCompletely(this, targetUsername, deviceAddress)

        val chatDeletedIntent = Intent("com.messenger.prime.CHAT_DELETED").apply {
            `package` = packageName
        }
        sendBroadcast(chatDeletedIntent)

        Toast.makeText(this, "Переписка и устройство полностью удалены", Toast.LENGTH_SHORT).show()

        val mainIntent = Intent(this, ChatListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(mainIntent)
        PrimeTransitions.applyOpenTransition(this)
        finish()
    }

    override fun finish() {
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }

    private fun formatDateSection(timestamp: Long): String {
        if (timestamp <= 0) return "Ранее"
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val sameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        if (sameDay) return "Сегодня"

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sameYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        if (sameYesterday) return "Вчера"

        val ruLocale = Locale.forLanguageTag("ru-RU")
        val sdf = if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMMM", ruLocale)
        } else {
            SimpleDateFormat("d MMMM yyyy", ruLocale)
        }
        return sdf.format(Date(timestamp))
    }

    private fun loadSharedMediaAndFiles() {
        Executors.newSingleThreadExecutor().execute {
            val lookupKey = if (!deviceAddress.isNullOrEmpty()) deviceAddress!! else targetUsername
            var history = ChatHistoryManager.loadMessages(this, lookupKey)
            if (history.isEmpty() && targetUsername.isNotEmpty()) {
                history = ChatHistoryManager.loadMessages(this, targetUsername)
            }
            if (history.isEmpty() && !deviceAddress.isNullOrEmpty()) {
                val storageKey = ChatHistoryManager.getStorageKey(this, deviceAddress!!)
                if (storageKey.isNotEmpty()) {
                    history = ChatHistoryManager.loadMessages(this, storageKey)
                }
            }
            if (history.isEmpty() && targetUsername.isNotEmpty()) {
                val storageKey = ChatHistoryManager.getStorageKey(this, targetUsername)
                if (storageKey.isNotEmpty()) {
                    history = ChatHistoryManager.loadMessages(this, storageKey)
                }
            }

            val mediaGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()
            val filesGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()
            val gifsGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()
            val musicGrouped = LinkedHashMap<String, MutableList<ChatMessage>>()

            var totalMediaCount = 0
            var totalFilesCount = 0
            var totalGifsCount = 0
            var totalMusicCount = 0

            for (msg in history) {
                if (msg.isVideoNote || (msg.text != null && msg.text.startsWith("VIDEO_NOTE:")) || (msg.imagePath != null && msg.imagePath.contains("video_notes"))) {
                    // BARRIER: Video notes are strictly excluded from PersonInformationActivity media gallery!
                    continue
                }

                if (msg.isMultiMedia || (!msg.imagePath.isNullOrEmpty() && msg.imagePath.startsWith("MULTI:"))) {
                    val subItems = msg.getMediaItems()
                    for (it in subItems) {
                        val subMsg = ChatMessage(msg.text, msg.time, msg.senderLogin, msg.isOutgoing, null, msg.timestamp, it.path, msg.messageId)
                        subMsg.messageType = if (it.isVideo) ChatMessage.MessageType.VIDEO else ChatMessage.MessageType.IMAGE
                        subMsg.videoDuration = it.durationStr
                        val dateLabel = formatDateSection(subMsg.timestamp)
                        mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(subMsg)
                        totalMediaCount++
                    }
                } else {
                    val pathOrName = (msg.imagePath ?: msg.fileName ?: "").lowercase(Locale.US)
                    val isVoice = msg.messageType == ChatMessage.MessageType.VOICE || pathOrName.contains("/voices/") || pathOrName.contains("voice_")
                    val isVid = !isVoice && (msg.isVideo || pathOrName.endsWith(".mp4") || pathOrName.endsWith(".mkv") || pathOrName.endsWith(".3gp") || pathOrName.endsWith(".webm") || pathOrName.endsWith(".mov") || pathOrName.endsWith(".avi"))
                    val isImg = !isVoice && (msg.messageType == ChatMessage.MessageType.IMAGE || (!msg.imagePath.isNullOrEmpty() && !msg.isFile) || msg.imageBitmap != null)
                    val isGif = !isVoice && pathOrName.endsWith(".gif")
                    val isMusic = !isVoice && (msg.messageType == ChatMessage.MessageType.MUSIC || pathOrName.endsWith(".mp3") || pathOrName.endsWith(".m4a") || pathOrName.endsWith(".aac") || pathOrName.endsWith(".wav") || pathOrName.endsWith(".ogg") || pathOrName.endsWith(".flac"))

                    if (isGif) {
                        msg.messageType = ChatMessage.MessageType.IMAGE
                        val dateLabel = formatDateSection(msg.timestamp)
                        gifsGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                        totalGifsCount++
                    } else if (isMusic) {
                        msg.messageType = ChatMessage.MessageType.MUSIC
                        val dateLabel = formatDateSection(msg.timestamp)
                        musicGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                        totalMusicCount++
                    } else if (isVid) {
                        msg.messageType = ChatMessage.MessageType.VIDEO
                        val dateLabel = formatDateSection(msg.timestamp)
                        mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                        totalMediaCount++
                    } else if (msg.isFile || msg.messageType == ChatMessage.MessageType.FILE || (!msg.fileName.isNullOrEmpty() && !isImg)) {
                        val dateLabel = formatDateSection(msg.timestamp)
                        filesGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                        totalFilesCount++
                    } else if (isImg || !msg.imagePath.isNullOrEmpty()) {
                        val dateLabel = formatDateSection(msg.timestamp)
                        mediaGrouped.getOrPut(dateLabel) { mutableListOf() }.add(msg)
                        totalMediaCount++
                    }
                }
            }

            // Build media adapter list
            val mediaAdapterItems = mutableListOf<MediaListItem>()
            val mediaClickList = mutableListOf<ChatMessage>()
            for ((dateTitle, list) in mediaGrouped) {
                mediaAdapterItems.add(MediaListItem.Header(dateTitle))
                for (m in list) {
                    mediaAdapterItems.add(MediaListItem.Item(m))
                    mediaClickList.add(m)
                }
            }

            // Build files adapter list
            val fileAdapterItems = mutableListOf<FileListItem>()
            for ((dateTitle, list) in filesGrouped) {
                fileAdapterItems.add(FileListItem.Header(dateTitle))
                for (f in list) {
                    fileAdapterItems.add(FileListItem.Item(f))
                }
            }

            // Build gifs adapter list
            val gifAdapterItems = mutableListOf<MediaListItem>()
            val gifClickList = mutableListOf<ChatMessage>()
            for ((dateTitle, list) in gifsGrouped) {
                gifAdapterItems.add(MediaListItem.Header(dateTitle))
                for (m in list) {
                    gifAdapterItems.add(MediaListItem.Item(m))
                    gifClickList.add(m)
                }
            }
            
            // Build music adapter list
            val musicAdapterItems = mutableListOf<FileListItem>()
            for ((dateTitle, list) in musicGrouped) {
                musicAdapterItems.add(FileListItem.Header(dateTitle))
                for (m in list) {
                    musicAdapterItems.add(FileListItem.Item(m))
                }
            }

            val finalMediaCount = totalMediaCount
            val finalFilesCount = totalFilesCount
            val finalGifsCount = totalGifsCount
            val finalMusicCount = totalMusicCount

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                binding.tvTabPhotoCount.text = finalMediaCount.toString()
                binding.tvTabFilesCount.text = finalFilesCount.toString()
                binding.tvTabGifsCount.text = finalGifsCount.toString()
                binding.tvTabMusicCount.text = finalMusicCount.toString()

                binding.tvTabPhotoCountSticky.text = finalMediaCount.toString()
                binding.tvTabFilesCountSticky.text = finalFilesCount.toString()
                binding.tvTabGifsCountSticky.text = finalGifsCount.toString()
                binding.tvTabMusicCountSticky.text = finalMusicCount.toString()

                val totalAllMedia = finalMediaCount + finalFilesCount + finalGifsCount + finalMusicCount
                hasAnySharedMedia = totalAllMedia > 0

                if (!hasAnySharedMedia && isToolbarVisible) {
                    isToolbarVisible = false
                    binding.blurStickyToolbar.visibility = View.GONE
                    binding.blurStickyToolbar.alpha = 0f
                }

                if (finalMediaCount == 0) {
                    binding.rvSharedMedia.visibility = View.GONE
                } else {
                    binding.rvSharedMedia.visibility = View.VISIBLE

                    val mediaAdapter = PersonMediaAdapter(mediaAdapterItems) { clickedMsg ->
                        val initialIdx = mediaClickList.indexOf(clickedMsg).coerceAtLeast(0)
                        ChatAdapter.showFullScreenMedia(this, null, clickedMsg, mediaClickList, initialIdx)
                    }
                    val gridManager = GridLayoutManager(this, 3)
                    gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (mediaAdapter.getItemViewType(position) == PersonMediaAdapter.TYPE_HEADER) 3 else 1
                        }
                    }
                    binding.rvSharedMedia.layoutManager = gridManager
                    binding.rvSharedMedia.adapter = mediaAdapter
                }

                if (finalFilesCount == 0) {
                    binding.rvSharedFiles.visibility = View.GONE
                } else {
                    binding.rvSharedFiles.visibility = View.VISIBLE
                    binding.rvSharedFiles.layoutManager = LinearLayoutManager(this)
                    binding.rvSharedFiles.adapter = PersonFilesAdapter(fileAdapterItems) { clickedFileMsg ->
                        val filePath = clickedFileMsg.imagePath ?: clickedFileMsg.fileName ?: ""
                        ChatAdapter.openFile(this, filePath)
                    }
                }
                
                if (finalGifsCount == 0) {
                    binding.rvSharedGifs.visibility = View.GONE
                } else {
                    binding.rvSharedGifs.visibility = View.VISIBLE
                    val gifAdapter = PersonMediaAdapter(gifAdapterItems) { clickedMsg ->
                        val initialIdx = gifClickList.indexOf(clickedMsg).coerceAtLeast(0)
                        ChatAdapter.showFullScreenMedia(this, null, clickedMsg, gifClickList, initialIdx)
                    }
                    val gridManager = GridLayoutManager(this, 3)
                    gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (gifAdapter.getItemViewType(position) == PersonMediaAdapter.TYPE_HEADER) 3 else 1
                        }
                    }
                    binding.rvSharedGifs.layoutManager = gridManager
                    binding.rvSharedGifs.adapter = gifAdapter
                }
                
                if (finalMusicCount == 0) {
                    binding.rvSharedMusic.visibility = View.GONE
                } else {
                    binding.rvSharedMusic.visibility = View.VISIBLE
                    binding.rvSharedMusic.layoutManager = LinearLayoutManager(this)
                    binding.rvSharedMusic.adapter = PersonMusicAdapter(musicAdapterItems) { clickedMusicMsg ->
                        val track = TrackItem(
                            id = clickedMusicMsg.messageId ?: "",
                            title = clickedMusicMsg.trackTitle ?: "Аудиозапись",
                            artist = clickedMusicMsg.artistName ?: "Неизвестен",
                            path = clickedMusicMsg.imagePath ?: "",
                            durationMs = 0L,
                            durationStr = clickedMusicMsg.audioDuration ?: "00:00",
                            coverPath = null,
                            isSaved = false,
                            fileSize = clickedMusicMsg.fileSize
                        )
                        
                        val allTracks = musicGrouped.values.flatten().map {
                            TrackItem(
                                id = it.messageId ?: "",
                                title = it.trackTitle ?: "Аудиозапись",
                                artist = it.artistName ?: "Неизвестен",
                                path = it.imagePath ?: "",
                                durationMs = 0L,
                                durationStr = it.audioDuration ?: "00:00",
                                coverPath = null,
                                isSaved = false,
                                fileSize = it.fileSize
                            )
                        }
                        
                        PrimeMusicManager.playTrack(this, track, allTracks)
                    }
                }
                
                selectTab(0)
            }
        }
    }

    private class PersonMusicAdapter(
        private val items: List<FileListItem>,
        private val onItemClick: (ChatMessage) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is FileListItem.Header -> TYPE_HEADER
                is FileListItem.Item -> TYPE_ITEM
            }
        }

        class HeaderViewHolder(val binding: ItemPersonDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        class ItemViewHolder(val binding: ItemPersonMusicRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemPersonDateHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(ItemPersonMusicRowBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is FileListItem.Header -> {
                    (holder as HeaderViewHolder).binding.tvDateTitle.text = item.title
                }
                is FileListItem.Item -> {
                    val h = holder as ItemViewHolder
                    val msg = item.msg
                    
                    val trackTitle = if (!msg.trackTitle.isNullOrEmpty()) msg.trackTitle else "Аудиозапись"
                    val artistName = if (!msg.artistName.isNullOrEmpty()) msg.artistName else "Неизвестен"
                    val audioDuration = if (!msg.audioDuration.isNullOrEmpty()) msg.audioDuration else "00:00"
                    
                    h.binding.tvMusicTitle.text = trackTitle
                    h.binding.tvMusicArtist.text = String.format("%s • %s", artistName, audioDuration)
                    
                    val currentTrack = PrimeMusicManager.getCurrentTrack()
                    val isPlaying = PrimeMusicManager.isPlaying()
                    val isActive = currentTrack != null && (currentTrack.id == msg.messageId || currentTrack.path == msg.imagePath)

                    val context = h.itemView.context
                    if (isActive) {
                        h.binding.ivMusicPlayBadge.visibility = View.VISIBLE
                        h.binding.ivMusicPlayBadge.setImageResource(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
                        h.binding.tvMusicTitle.setTextColor(ColorAccentManager.getCurrentAccentColor(context))
                    } else {
                        h.binding.ivMusicPlayBadge.visibility = View.GONE
                        h.binding.tvMusicTitle.setTextColor(ContextCompat.getColor(context, R.color.prime_text_primary))
                    }

                    h.binding.flMusicCover.setOnClickListener {
                        if (isActive) {
                            PrimeMusicManager.togglePlayPause(context)
                        } else {
                            onItemClick(msg)
                        }
                    }

                    h.itemView.setOnClickListener {
                        if (isActive) {
                            PrimeMusicManager.togglePlayPause(context)
                        } else {
                            onItemClick(msg)
                        }
                    }
                }
            }
        }

        override fun getItemCount() = items.size
    }

    sealed class MediaListItem {
        data class Header(val title: String) : MediaListItem()
        data class Item(val msg: ChatMessage) : MediaListItem()
    }

    private class PersonMediaAdapter(
        private val items: List<MediaListItem>,
        private val onItemClick: (ChatMessage) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is MediaListItem.Header -> TYPE_HEADER
                is MediaListItem.Item -> TYPE_ITEM
            }
        }

        class HeaderViewHolder(val binding: ItemPersonDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        class ItemViewHolder(val binding: ItemGalleryMediaBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemPersonDateHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(ItemGalleryMediaBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is MediaListItem.Header -> {
                    (holder as HeaderViewHolder).binding.tvDateTitle.text = item.title
                }
                is MediaListItem.Item -> {
                    val h = holder as ItemViewHolder
                    val msg = item.msg
                    val path = msg.imagePath

                    if (msg.isVideo) {
                        h.binding.layoutVideoBadge.visibility = View.VISIBLE
                        h.binding.tvVideoDuration.text = msg.videoDuration?.ifEmpty { "00:00" } ?: "00:00"

                        if (!path.isNullOrEmpty()) {
                            val (model, file) = parseAvatarModelAndFile(path)
                            val sigKey = ObjectKey(if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis())
                            Glide.with(h.itemView.context)
                                .asBitmap()
                                .load(model)
                                .centerCrop()
                                .signature(sigKey)
                                .placeholder(R.drawable.ic_video)
                                .error(R.drawable.ic_video)
                                .into(h.binding.ivGalleryThumbnail)
                        } else {
                            h.binding.ivGalleryThumbnail.setImageResource(R.drawable.ic_video)
                        }
                    } else {
                        h.binding.layoutVideoBadge.visibility = View.GONE

                        if (!path.isNullOrEmpty()) {
                            val isGif = path.lowercase(Locale.US).contains(".gif")
                            val (model, file) = parseAvatarModelAndFile(path)
                            val sigKey = ObjectKey(if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis())

                            if (isGif && model != null) {
                                Glide.with(h.itemView.context)
                                    .asGif()
                                    .load(model)
                                    .centerCrop()
                                    .signature(sigKey)
                                    .placeholder(R.drawable.ic_photo)
                                    .error(R.drawable.ic_photo)
                                    .into(h.binding.ivGalleryThumbnail)
                            } else {
                                ChatAdapter.loadMediaImageIntoView(h.itemView.context, path, h.binding.ivGalleryThumbnail)
                            }
                        } else if (msg.imageBitmap != null) {
                            h.binding.ivGalleryThumbnail.setImageBitmap(msg.imageBitmap)
                        } else {
                            h.binding.ivGalleryThumbnail.setImageResource(R.drawable.ic_photo)
                        }
                    }

                    h.itemView.setOnClickListener { onItemClick(msg) }
                }
            }
        }

        override fun getItemCount() = items.size
    }

    sealed class FileListItem {
        data class Header(val title: String) : FileListItem()
        data class Item(val msg: ChatMessage) : FileListItem()
    }

    private class PersonFilesAdapter(
        private val items: List<FileListItem>,
        private val onItemClick: (ChatMessage) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is FileListItem.Header -> TYPE_HEADER
                is FileListItem.Item -> TYPE_ITEM
            }
        }

        class HeaderViewHolder(val binding: ItemPersonDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        class ItemViewHolder(val binding: ItemPersonFileRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemPersonDateHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(ItemPersonFileRowBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is FileListItem.Header -> {
                    (holder as HeaderViewHolder).binding.tvDateTitle.text = item.title
                }
                is FileListItem.Item -> {
                    val h = holder as ItemViewHolder
                    val msg = item.msg
                    var resolvedName = msg.fileName?.ifEmpty { null }
                        ?: msg.imagePath?.let { File(it).name }
                        ?: "Документ"
                    
                    if (resolvedName.startsWith("content://") || resolvedName.contains("%2F")) {
                        resolvedName = try {
                            val dec = Uri.decode(resolvedName)
                            File(dec).name
                        } catch (_: Exception) { "Документ" }
                    }
                    
                    h.binding.tvFileName.text = resolvedName

                    val filePath = msg.imagePath ?: ""
                    val sizeBytes = if (msg.fileSize > 0) msg.fileSize else (if (filePath.isNotEmpty()) {
                        val (_, f) = parseAvatarModelAndFile(filePath)
                        f?.length() ?: 0L
                    } else 0L)
                    h.binding.tvFileSize.text = ChatAdapter.formatFileSize(sizeBytes)
                    h.itemView.setOnClickListener { onItemClick(msg) }
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
