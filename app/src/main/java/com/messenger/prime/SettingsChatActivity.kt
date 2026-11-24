package com.messenger.prime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.messenger.prime.databinding.ActivitySettingsChatBinding
import com.messenger.prime.shapes.M3EShapes
import java.io.File

class SettingsChatActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsChatBinding
    private var wallpaperInDeleteModeId: String? = null

    private val wallpaperEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setupWallpaperSection()
                sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutChatHeroStage,
            heroOverlay = b.layoutChatHeroOverlay,
            scrollView = b.scrollChatSettings,
            title = "В чате",
            subtitle = "Обои, пузырьки и кружки",
            heroHeightFraction = 0.36f,
            heroMinDp = 256,
            heroMaxDp = 328,
            applyDefaultBackdrop = false,
            animatedCards = listOf(
                b.cardWallpaperSection,
                b.cardChatBubblesAndAvatars,
                b.cardChatUiAndGestures,
                b.cardVideoNotesSection
            )
        )

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconWallpaper, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconVideoNotes, accentColor, M3EShapes.cookie7Sided, isDark)
        tintSliders(accentColor)

        setupWallpaperSection()
        setupBubbleRadiusSection()
        setupAvatarRadiusSection()
        setupChatStyleSection()
        setupPartnerInfoOpenModeSection()
        setupPartnerInfoSection()
        setupM3EShapesChatSection()
        setupBottomBarRadiusSection()
        setupVideoMessageSection()

        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun tintSliders(accentColor: Int) {
        val colorStateList = ColorStateList.valueOf(accentColor)
        val inactiveTrackTint = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accentColor, 60))
        val haloTint = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accentColor, 32))

        val sliders = listOf(
            b.sliderBubbleRadius,
            b.sliderAvatarRadius,
            b.sliderBottomBarRadius,
            b.sliderVideoCornerRadius,
            b.sliderVideoSize
        )
        for (slider in sliders) {
            slider.thumbTintList = colorStateList
            slider.trackActiveTintList = colorStateList
            slider.trackInactiveTintList = inactiveTrackTint
            slider.haloTintList = haloTint
        }
    }

    private fun setupWallpaperSection() {
        val density = resources.displayMetrics.density
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isNight = SettingsSupport.isDarkThemeActive(this)

        val wallpapers = WallpaperManager.getWallpapersList(this)
        val activeWp = WallpaperManager.getActiveWallpaper(this)

        b.vHeroChatWallpaper.setLiveTempWallpaper(activeWp)
        b.tvWallpaperSummary.text = activeWp.name
        b.layoutWallpaperOptions.removeAllViews()

        for (wp in wallpapers) {
            val isActive = (wp.id == activeWp.id)
            val isDeleteMode = (wallpaperInDeleteModeId == wp.id && wp.type != "DEFAULT")

            val itemContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (88 * density).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, (8 * density).toInt(), 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                isFocusable = true
                background = ContextCompat.getDrawable(this@SettingsChatActivity, R.drawable.bg_chat_item_light)
                setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
            }

            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (76 * density).toInt(),
                    (138 * density).toInt()
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                radius = 18 * density
                cardElevation = 0f
                strokeWidth = if (isActive) (2.5f * density).toInt() else 0
                strokeColor = accentColor
            }

            val cardFrame = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            when {
                wp.type == "IMAGE" && !wp.imagePath.isNullOrEmpty() -> {
                    val iv = ImageView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    val f = File(wp.imagePath)
                    if (f.exists()) {
                        Glide.with(this).load(f).into(iv)
                    }
                    cardFrame.addView(iv)

                    if (wp.dimOpacity > 0f) {
                        val dimView = View(this).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(Color.BLACK)
                            alpha = wp.dimOpacity
                        }
                        cardFrame.addView(dimView)
                    }
                }
                wp.type == "PATTERN" -> {
                    val emojis = wp.customEmojis.let { if (it.isNullOrBlank()) "😎 ⭐ 🌸 🔷" else it }
                    val parts = emojis.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val line1 = parts.joinToString(" ")
                    val line2 = parts.reversed().joinToString(" ")
                    val line3 = if (parts.size > 2) parts.shuffled().joinToString(" ") else line1
                    val tv = TextView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(if (isNight) Color.parseColor("#0F172A") else Color.parseColor("#F8FAFC"))
                        gravity = Gravity.CENTER
                        text = "$line1\n$line2\n$line3"
                        textSize = 10f
                        setTextColor(if (isNight) Color.parseColor("#B0FFFFFF") else Color.parseColor("#80000000"))
                    }
                    cardFrame.addView(tv)
                }
                wp.type == "MATERIAL" -> {
                    val matView = MaterialShapesWallpaperView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    cardFrame.addView(matView)
                }
                else -> {
                    val tv = TextView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(if (isNight) Color.parseColor("#151D2A") else Color.parseColor("#F1F5F9"))
                        gravity = Gravity.CENTER
                        text = "Прайм\nОбои"
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(accentColor)
                    }
                    cardFrame.addView(tv)
                }
            }

            val notch = View(this).apply {
                layoutParams = FrameLayout.LayoutParams((16 * density).toInt(), (4 * density).toInt()).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = (5 * density).toInt()
                }
                background = ContextCompat.getDrawable(this@SettingsChatActivity, R.drawable.bg_m3_active_indicator)
                backgroundTintList = ColorStateList.valueOf(
                    if (isNight) Color.parseColor("#40FFFFFF") else Color.parseColor("#30000000")
                )
            }
            cardFrame.addView(notch)
            card.addView(cardFrame)
            itemContainer.addView(card)

            val labelTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    (20 * density).toInt()
                ).apply {
                    topMargin = (8 * density).toInt()
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                gravity = Gravity.CENTER
                text = wp.name
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                isSingleLine = true
                setTextColor(ContextCompat.getColor(this@SettingsChatActivity, R.color.prime_text_primary))
            }
            itemContainer.addView(labelTv)

            if (isDeleteMode) {
                val deleteIcon = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                        topMargin = (4 * density).toInt()
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    setImageResource(R.drawable.ic_cancel)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@SettingsChatActivity, R.color.prime_danger))
                    isClickable = true
                    setOnClickListener {
                        WallpaperManager.deleteCustomWallpaper(this@SettingsChatActivity, wp.id)
                        wallpaperInDeleteModeId = null
                        setupWallpaperSection()
                        sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
                    }
                }
                itemContainer.addView(deleteIcon)
            } else {
                val radioBtn = RadioButton(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (4 * density).toInt()
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    minWidth = 0
                    minHeight = 0
                    setPadding(0, 0, 0, 0)
                    isChecked = isActive
                    isClickable = false
                    isFocusable = false
                    buttonTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            accentColor,
                            if (isNight) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000")
                        )
                    )
                }
                itemContainer.addView(radioBtn)
            }

            itemContainer.setOnClickListener {
                if (wallpaperInDeleteModeId != null) {
                    wallpaperInDeleteModeId = null
                    setupWallpaperSection()
                } else {
                    WallpaperManager.setActiveWallpaper(this, wp.id)
                    setupWallpaperSection()
                    sendBroadcast(Intent("com.messenger.prime.WALLPAPER_CHANGED").setPackage(packageName))
                }
            }

            if (wp.type != "DEFAULT") {
                itemContainer.setOnLongClickListener {
                    wallpaperInDeleteModeId = if (wallpaperInDeleteModeId == wp.id) null else wp.id
                    setupWallpaperSection()
                    true
                }
            }

            b.layoutWallpaperOptions.addView(itemContainer)
        }

        val addContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((88 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(this@SettingsChatActivity, R.drawable.bg_chat_item_light)
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())

            setOnClickListener {
                val intent = Intent(this@SettingsChatActivity, WallpaperEditorActivity::class.java)
                wallpaperEditorLauncher.launch(intent)
                PrimeTransitions.applyOpenTransition(this@SettingsChatActivity)
            }
        }

        val addCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams((76 * density).toInt(), (138 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            radius = 18 * density
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(ColorUtils.setAlphaComponent(accentColor, if (isNight) 44 else 24))
        }

        val addFrame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val addPlusIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.ic_add)
            imageTintList = ColorStateList.valueOf(accentColor)
        }
        addFrame.addView(addPlusIcon)
        addCard.addView(addFrame)
        addContainer.addView(addCard)

        val addLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (20 * density).toInt()
            ).apply {
                topMargin = (8 * density).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
            gravity = Gravity.CENTER
            text = "Добавить"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
        }
        addContainer.addView(addLabel)
        b.layoutWallpaperOptions.addView(addContainer)
    }

    private fun updateThemeChatPreview() {
        val density = resources.displayMetrics.density

        val avatarRadiusPx = getAvatarCornerRadiusPx(this, (28 * density).toInt())
        val avatarBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = avatarRadiusPx.toFloat()
            setColor(ColorAccentManager.getAvatarColor("Prime"))
        }
        b.tvPreviewAvatar.background = avatarBg

        b.layoutPreviewIncomingBubble.background = ColorAccentManager.createIncomingBubbleDrawable(this)
        b.tvPreviewIncomingText.setTextColor(
            ContextCompat.getColor(this, R.color.prime_text_primary)
        )
        b.tvPreviewIncomingTime.setTextColor(
            ContextCompat.getColor(this, R.color.prime_text_secondary)
        )

        b.layoutPreviewOutgoingBubble.background = ColorAccentManager.createOutgoingBubbleDrawable(this)
        b.tvPreviewOutgoingText.setTextColor(Color.WHITE)
        b.tvPreviewOutgoingTime.setTextColor(Color.parseColor("#CCFFFFFF"))
        b.ivPreviewOutgoingStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#CCFFFFFF"))

        if (!PrimeTransitions.areAnimationsDisabled(this)) {
            val bubbleInterpolator = OvershootInterpolator(1.15f)
            b.layoutPreviewIncomingBubble.animate().cancel()
            b.layoutPreviewIncomingBubble.scaleX = 0.96f
            b.layoutPreviewIncomingBubble.scaleY = 0.96f
            b.layoutPreviewIncomingBubble.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .setInterpolator(bubbleInterpolator)
                .start()

            b.layoutPreviewOutgoingBubble.animate().cancel()
            b.layoutPreviewOutgoingBubble.scaleX = 0.96f
            b.layoutPreviewOutgoingBubble.scaleY = 0.96f
            b.layoutPreviewOutgoingBubble.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setStartDelay(40)
                .setDuration(200)
                .setInterpolator(bubbleInterpolator)
                .start()
        }
    }

    private fun setupBubbleRadiusSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val savedPercent = sharedPrefs.getInt("settings_bubble_radius_percent", 50)
        b.sliderBubbleRadius.value = savedPercent.toFloat().coerceIn(0f, 100f)

        val updateResetVisibility = { percent: Int ->
            b.btnResetBubbleRadius.visibility = if (percent != 50) View.VISIBLE else View.GONE
        }
        val updateLabel = { percent: Int ->
            b.tvBubbleRadiusValue.text = "$percent%"
        }
        updateLabel(savedPercent)
        updateResetVisibility(savedPercent)
        updateThemeChatPreview()

        b.btnResetBubbleRadius.setOnClickListener {
            sharedPrefs.edit().putInt("settings_bubble_radius_percent", 50).apply()
            b.sliderBubbleRadius.value = 50f
            updateLabel(50)
            updateResetVisibility(50)
            updateThemeChatPreview()
            sendBroadcast(Intent("com.messenger.prime.BUBBLE_RADIUS_CHANGED").setPackage(packageName))
        }

        b.sliderBubbleRadius.addOnChangeListener { _, value, fromUser ->
            val percent = value.toInt()
            sharedPrefs.edit().putInt("settings_bubble_radius_percent", percent).apply()
            updateLabel(percent)
            updateResetVisibility(percent)
            updateThemeChatPreview()
            if (fromUser) {
                sendBroadcast(Intent("com.messenger.prime.BUBBLE_RADIUS_CHANGED").setPackage(packageName))
            }
        }
    }

    private fun setupAvatarRadiusSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val savedPercent = sharedPrefs.getInt("settings_avatar_radius_percent", 50)
        b.sliderAvatarRadius.value = savedPercent.toFloat().coerceIn(0f, 100f)

        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val myAvatar = sharedPrefs.getString("my_avatar", null)
            ?: sharedPrefs.getString("my_local_avatar", null)
            ?: sharedPrefs.getString("my_avatar_uri", null)
            ?: sharedPrefs.getString("${currentUser}_avatar", null)
        val myName = AvatarManager.getMyDisplayName(this).ifBlank { "Пользователь" }

        if (!myAvatar.isNullOrEmpty()) {
            b.ivAvatarPreviewImage.visibility = View.VISIBLE
            b.tvAvatarPreviewInitials.visibility = View.GONE
            loadAvatarIntoView(this, myAvatar, b.ivAvatarPreviewImage, myName)
        } else {
            b.ivAvatarPreviewImage.visibility = View.GONE
            b.tvAvatarPreviewInitials.visibility = View.VISIBLE
            b.tvAvatarPreviewInitials.text = myName.take(1).uppercase()
        }

        val density = resources.displayMetrics.density
        val viewSizePx = (48 * density).toInt()
        b.layoutAvatarPreviewBg.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(ContextCompat.getColor(this@SettingsChatActivity, R.color.prime_base))
        }
        b.cardAvatarPreviewBlock.strokeWidth = 0

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

            b.ivAvatarPreviewImage.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
                }
            }
            b.ivAvatarPreviewImage.clipToOutline = true
            b.ivAvatarPreviewImage.invalidateOutline()
            updateThemeChatPreview()
        }

        val updateResetVisibility = { percent: Int ->
            b.btnResetAvatarRadius.visibility = if (percent != 50) View.VISIBLE else View.GONE
        }

        updateResetVisibility(savedPercent)
        updateAvatarPreview(savedPercent)

        b.btnResetAvatarRadius.setOnClickListener {
            sharedPrefs.edit().putInt("settings_avatar_radius_percent", 50).apply()
            b.sliderAvatarRadius.value = 50f
            AvatarManager.clearAvatarRadiusCache()
            updateAvatarPreview(50)
            updateResetVisibility(50)
            sendBroadcast(Intent("com.messenger.prime.AVATAR_RADIUS_CHANGED").setPackage(packageName))
        }

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

    private fun setupChatStyleSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentStyle = sharedPrefs.getString("settings_chat_header_style", "block") ?: "block"
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        b.vPreviewTopFog.setBackgroundResource(if (isDark) R.drawable.bg_top_fog_dark else R.drawable.bg_top_fog_light)
        b.btnChatStyleBlock.tag = "do_not_tint"
        b.btnChatStyleNoBlock.tag = "do_not_tint"

        val updateChatStyleUi = { style: String ->
            currentStyle = style
            val isBlock = style == "block"

            val activeColorStateList = ColorStateList.valueOf(accentColor)
            val transparentColorStateList = ColorStateList.valueOf(Color.TRANSPARENT)
            val secondaryTextColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

            b.btnChatStyleBlock.backgroundTintList = if (isBlock) activeColorStateList else transparentColorStateList
            b.btnChatStyleBlock.setTextColor(if (isBlock) Color.WHITE else secondaryTextColor)

            b.btnChatStyleNoBlock.backgroundTintList = if (!isBlock) activeColorStateList else transparentColorStateList
            b.btnChatStyleNoBlock.setTextColor(if (!isBlock) Color.WHITE else secondaryTextColor)

            if (isBlock) {
                b.cardHeaderPreviewIsland.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.prime_surface)
                )
                b.cardHeaderPreviewIsland.strokeWidth = 0
                b.cardHeaderPreviewIsland.cardElevation = 2 * resources.displayMetrics.density
            } else {
                b.cardHeaderPreviewIsland.setCardBackgroundColor(Color.TRANSPARENT)
                b.cardHeaderPreviewIsland.strokeWidth = 0
                b.cardHeaderPreviewIsland.cardElevation = 0f
            }

            b.tvTitleBottomBarRadius.alpha = if (isBlock) 1.0f else 0.4f
            b.sliderBottomBarRadius.isEnabled = isBlock
            b.sliderBottomBarRadius.alpha = if (isBlock) 1.0f else 0.4f
            b.cardBottomBarPreviewBlock.alpha = if (isBlock) 1.0f else 0.4f
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

    private fun setupPartnerInfoOpenModeSection() {
        val sec = b.cardPartnerInfoOpenModeSection
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentMode = sharedPrefs.getString("settings_partner_info_open_mode", "drag") ?: "drag"
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        sec.tvTitlePartnerInfoOpenMode.setTextColor(accentColor)
        sec.ivPreviewGestureIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sec.tvPreviewGestureHint.setTextColor(accentColor)
        sec.btnPartnerInfoModeDrag.tag = "do_not_tint"
        sec.btnPartnerInfoModeSwipe.tag = "do_not_tint"

        var isAnimationRunning = false
        val runPreviewAnimation = { mode: String ->
            val chatFg = sec.vPreviewChatForeground
            val chatHeader = sec.vPreviewChatHeader
            if (!isAnimationRunning && !PrimeTransitions.areAnimationsDisabled(this)) {
                isAnimationRunning = true
                chatFg.animate().cancel()
                chatHeader.animate().cancel()

                chatFg.translationX = 0f
                chatFg.scaleX = 1f
                chatFg.scaleY = 1f
                chatFg.alpha = 1f
                chatHeader.translationY = 0f

                if (mode == "swipe") {
                    val targetX = -chatFg.width.toFloat() * 0.45f
                    chatFg.animate()
                        .translationX(if (targetX != 0f) targetX else -160f)
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .alpha(0.7f)
                        .setDuration(420)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            chatFg.postDelayed({
                                chatFg.animate()
                                    .translationX(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .alpha(1.0f)
                                    .setDuration(380)
                                    .setInterpolator(DecelerateInterpolator())
                                    .withEndAction { isAnimationRunning = false }
                                    .start()
                            }, 450)
                        }
                        .start()
                } else {
                    val density = resources.displayMetrics.density
                    chatHeader.animate()
                        .translationY(22f * density)
                        .setDuration(380)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            chatHeader.postDelayed({
                                chatHeader.animate()
                                    .translationY(0f)
                                    .setDuration(340)
                                    .setInterpolator(DecelerateInterpolator())
                                    .withEndAction { isAnimationRunning = false }
                                    .start()
                            }, 450)
                        }
                        .start()
                }
            }
        }

        val updateModeUi = { mode: String, triggerAnim: Boolean ->
            currentMode = mode
            val isDrag = mode == "drag"

            val activeColorStateList = ColorStateList.valueOf(accentColor)
            val transparentColorStateList = ColorStateList.valueOf(Color.TRANSPARENT)
            val unselectedTextColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

            sec.btnPartnerInfoModeDrag.backgroundTintList = if (isDrag) activeColorStateList else transparentColorStateList
            sec.btnPartnerInfoModeDrag.setTextColor(if (isDrag) Color.WHITE else unselectedTextColor)

            sec.btnPartnerInfoModeSwipe.backgroundTintList = if (!isDrag) activeColorStateList else transparentColorStateList
            sec.btnPartnerInfoModeSwipe.setTextColor(if (!isDrag) Color.WHITE else unselectedTextColor)

            if (isDrag) {
                sec.tvPreviewGestureHint.text = "Потяните вниз для инфо"
                sec.ivPreviewGestureIcon.setImageResource(R.drawable.ic_arrow_back)
                sec.ivPreviewGestureIcon.rotation = -90f
                b.cardPartnerInfoSection.alpha = 1.0f
                b.switchPartnerInfo.isEnabled = true
                b.switchPartnerInfo.isChecked = sharedPrefs.getBoolean("settings_partner_info_enabled", true)
                b.tvSubtitlePartnerInfo.text = "Переходить в профиль при перетягивании с верхнего края чата"
            } else {
                sec.tvPreviewGestureHint.text = "Свайп справа налево для инфо"
                sec.ivPreviewGestureIcon.setImageResource(R.drawable.ic_arrow_back)
                sec.ivPreviewGestureIcon.rotation = 0f
                b.cardPartnerInfoSection.alpha = 0.45f
                b.switchPartnerInfo.isEnabled = false
                b.switchPartnerInfo.isChecked = false
                b.tvSubtitlePartnerInfo.text = "Недоступно: выбран свайп справа налево"
            }

            if (triggerAnim) {
                sec.vPreviewChatForeground.post { runPreviewAnimation(mode) }
            }
        }

        updateModeUi(currentMode, false)
        sec.vPreviewChatForeground.post { runPreviewAnimation(currentMode) }

        sec.btnPartnerInfoModeDrag.setOnClickListener {
            if (currentMode != "drag") {
                sharedPrefs.edit().putString("settings_partner_info_open_mode", "drag").apply()
                updateModeUi("drag", true)
            }
        }

        sec.btnPartnerInfoModeSwipe.setOnClickListener {
            if (currentMode != "swipe") {
                sharedPrefs.edit().putString("settings_partner_info_open_mode", "swipe").apply()
                updateModeUi("swipe", true)
            }
        }

        sec.btnResetPartnerInfoOpenMode.setOnClickListener {
            if (currentMode != "drag") {
                sharedPrefs.edit().putString("settings_partner_info_open_mode", "drag").apply()
                updateModeUi("drag", true)
            }
        }
    }

    private fun setupPartnerInfoSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val openMode = sharedPrefs.getString("settings_partner_info_open_mode", "drag") ?: "drag"
        val isEnabled = sharedPrefs.getBoolean("settings_partner_info_enabled", true)

        if (openMode == "swipe") {
            b.switchPartnerInfo.isChecked = false
            b.switchPartnerInfo.isEnabled = false
            b.cardPartnerInfoSection.alpha = 0.45f
            b.tvSubtitlePartnerInfo.text = "Недоступно: выбран свайп справа налево"
        } else {
            b.switchPartnerInfo.isChecked = isEnabled
            b.switchPartnerInfo.isEnabled = true
            b.cardPartnerInfoSection.alpha = 1.0f
            b.tvSubtitlePartnerInfo.text = "Переходить в профиль при перетягивании с верхнего края чата"
        }

        b.switchPartnerInfo.setOnCheckedChangeListener { _, isChecked ->
            if (b.switchPartnerInfo.isEnabled) {
                sharedPrefs.edit().putBoolean("settings_partner_info_enabled", isChecked).apply()
            }
        }
    }

    private fun setupM3EShapesChatSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        b.switchM3EShapesChat.isChecked = sharedPrefs.getBoolean("m3e_shapes_chat_enabled", true)
        b.switchM3EShapesChat.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("m3e_shapes_chat_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
        }
    }

    private fun setupBottomBarRadiusSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentRadius = sharedPrefs.getInt(
            "settings_chat_ui_radius_dp",
            sharedPrefs.getInt("settings_bottom_bar_radius_dp", 24)
        )
        val density = resources.displayMetrics.density

        b.layoutBottomBarPreviewBg.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(ContextCompat.getColor(this@SettingsChatActivity, R.color.prime_base))
        }
        b.cardBottomBarPreviewBlock.strokeWidth = 0
        b.cardHeaderPreviewInput.strokeWidth = 0
        b.cardBottomBarPreviewInput.strokeWidth = 0

        val updatePreview = { radiusDp: Int ->
            val percent = Math.round((radiusDp / 36f) * 100)
            b.tvBottomBarRadiusValue.text = "$percent%"
            val radiusPx = radiusDp * density
            b.cardHeaderPreviewInput.radius = radiusPx
            b.cardBottomBarPreviewInput.radius = radiusPx
            b.cardHeaderPreviewIsland.radius = radiusPx
        }

        val updateResetVisibility = { radiusDp: Int ->
            b.btnResetBottomBarRadius.visibility = if (radiusDp != 24) View.VISIBLE else View.GONE
        }

        updatePreview(currentRadius)
        b.sliderBottomBarRadius.value = currentRadius.coerceIn(0, 36).toFloat()
        updateResetVisibility(currentRadius)

        b.btnResetBottomBarRadius.setOnClickListener {
            sharedPrefs.edit()
                .putInt("settings_chat_ui_radius_dp", 24)
                .putInt("settings_bottom_bar_radius_dp", 24)
                .putInt("settings_top_bar_radius_dp", 24)
                .apply()
            b.sliderBottomBarRadius.value = 24f
            updatePreview(24)
            updateResetVisibility(24)
            sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.sliderBottomBarRadius.addOnChangeListener { _, value, fromUser ->
            val radiusInt = value.toInt()
            updatePreview(radiusInt)
            updateResetVisibility(radiusInt)
            if (fromUser) {
                sharedPrefs.edit()
                    .putInt("settings_chat_ui_radius_dp", radiusInt)
                    .putInt("settings_bottom_bar_radius_dp", radiusInt)
                    .putInt("settings_top_bar_radius_dp", radiusInt)
                    .apply()
                sendBroadcast(Intent("com.messenger.prime.CHAT_UI_RADIUS_CHANGED").setPackage(packageName))
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupVideoMessageSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        val savedShape = sharedPrefs.getString("video_message_shape", "50") ?: "50"
        val savedRadiusPercent = sharedPrefs.getInt(
            "video_message_corner_radius",
            VideoNoteShapeHelper.getCornerRadiusPercent(savedShape, 50)
        )
        val savedSizeDp = sharedPrefs.getInt("video_message_size_dp", 180)

        b.layoutVideoNotePreviewBg.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * resources.displayMetrics.density
            setColor(ContextCompat.getColor(this@SettingsChatActivity, R.color.prime_base))
        }
        b.cardVideoNotePreviewBlock.strokeWidth = 0

        fun updatePreviewAndLabels(shapeTag: String, radiusPercent: Int, sizeDp: Int) {
            val shapeOption = VideoNoteShapeHelper.AVAILABLE_SHAPES.firstOrNull {
                it.tag.equals(shapeTag, ignoreCase = true)
            }
            val shapeName = shapeOption?.displayName
                ?: if (radiusPercent >= 50) "Круг" else if (radiusPercent == 0) "Квадрат" else "$radiusPercent%"
            b.tvVideoShapeValue.text = shapeName

            b.tvVideoCornerRadiusValue.text = when {
                VideoNoteShapeHelper.isM3EShape(shapeTag) -> shapeName
                radiusPercent >= 50 -> "$radiusPercent% (Круг)"
                radiusPercent == 0 -> "0% (Квадрат)"
                else -> "$radiusPercent% (Сквиркл)"
            }

            val sizePercent = Math.round(((sizeDp - 120f) / (260f - 120f)) * 100)
            b.tvVideoSizeValue.text = "$sizePercent%"

            b.layoutVideoNotePreviewFrame.let { frame ->
                val previewSizePx = (sizeDp * resources.displayMetrics.density).toInt()
                val lp = frame.layoutParams
                if (lp != null && (lp.width != previewSizePx || lp.height != previewSizePx)) {
                    lp.width = previewSizePx
                    lp.height = previewSizePx
                    frame.layoutParams = lp
                }
                VideoNoteShapeHelper.applyShapeToView(frame, shapeTag, radiusPercent)
            }

            b.pbVideoNotePreviewFrame.let { pb ->
                pb.isBorderProgressMode = true
                pb.staticShapeType = shapeTag
                pb.setIndicatorColor(accentColor)
                pb.progress = 65
            }
        }

        fun updateResetBtnsVisibility(radiusPercent: Int, sizeDp: Int) {
            b.btnResetVideoCorner.visibility = if (radiusPercent != 50) View.VISIBLE else View.GONE
            b.btnResetVideoSize.visibility = if (sizeDp != 180) View.VISIBLE else View.GONE
        }

        fun styleChip(chip: Chip, selected: Boolean) {
            val bgColor = if (selected) {
                ColorUtils.setAlphaComponent(accentColor, if (isDark) 72 else 36)
            } else {
                ContextCompat.getColor(this, R.color.prime_base)
            }
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.setTextColor(
                if (selected) accentColor else ContextCompat.getColor(this, R.color.prime_text_primary)
            )
            chip.chipStrokeWidth = 0f
        }

        b.sliderVideoCornerRadius.value = savedRadiusPercent.toFloat().coerceIn(0f, 50f)
        b.sliderVideoSize.value = savedSizeDp.toFloat().coerceIn(120f, 260f)

        b.chipGroupVideoShapes.let { chipGroup ->
            chipGroup.removeAllViews()
            val currentShape = sharedPrefs.getString("video_message_shape", "50") ?: "50"
            VideoNoteShapeHelper.AVAILABLE_SHAPES.forEach { shape ->
                val isSelected = shape.tag.equals(currentShape, ignoreCase = true)
                val chip = Chip(this).apply {
                    id = View.generateViewId()
                    tag = shape.tag
                    text = shape.displayName
                    isCheckable = true
                    isClickable = true
                    isChecked = isSelected
                    styleChip(this, isSelected)
                    setOnClickListener { chipGroup.check(id) }
                }
                chipGroup.addView(chip)
            }

            chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
                val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
                val selectedChip = group.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
                val selectedTag = selectedChip.tag as? String ?: return@setOnCheckedStateChangeListener

                val currentRadius = when (selectedTag) {
                    "0" -> 0
                    "50" -> 50
                    else -> VideoNoteShapeHelper.getCornerRadiusPercent(
                        selectedTag,
                        b.sliderVideoCornerRadius.value.toInt()
                    )
                }
                val currentSize = b.sliderVideoSize.value.toInt()

                sharedPrefs.edit()
                    .putString("video_message_shape", selectedTag)
                    .putInt("video_message_corner_radius", currentRadius)
                    .apply()

                b.sliderVideoCornerRadius.value = currentRadius.toFloat().coerceIn(0f, 50f)
                updatePreviewAndLabels(selectedTag, currentRadius, currentSize)
                updateResetBtnsVisibility(currentRadius, currentSize)

                for (i in 0 until group.childCount) {
                    val child = group.getChildAt(i) as? Chip ?: continue
                    styleChip(child, child.id == checkedId)
                }
            }
        }

        updatePreviewAndLabels(savedShape, savedRadiusPercent, savedSizeDp)
        updateResetBtnsVisibility(savedRadiusPercent, savedSizeDp)

        b.btnResetVideoCorner.setOnClickListener {
            sharedPrefs.edit()
                .putInt("video_message_corner_radius", 50)
                .putString("video_message_shape", "50")
                .apply()
            b.sliderVideoCornerRadius.value = 50f
            val currentSize = b.sliderVideoSize.value.toInt()
            updatePreviewAndLabels("50", 50, currentSize)
            updateResetBtnsVisibility(50, currentSize)

            for (i in 0 until b.chipGroupVideoShapes.childCount) {
                val chip = b.chipGroupVideoShapes.getChildAt(i) as? Chip ?: continue
                val matches = chip.tag == "50"
                chip.isChecked = matches
                styleChip(chip, matches)
            }
        }

        b.btnResetVideoSize.setOnClickListener {
            sharedPrefs.edit().putInt("video_message_size_dp", 180).apply()
            b.sliderVideoSize.value = 180f
            val currentShapeTag = sharedPrefs.getString("video_message_shape", "50") ?: "50"
            val currentRadius = b.sliderVideoCornerRadius.value.toInt()
            updatePreviewAndLabels(currentShapeTag, currentRadius, 180)
            updateResetBtnsVisibility(currentRadius, 180)
        }

        b.sliderVideoCornerRadius.addOnChangeListener { _, value, fromUser ->
            val radius = value.toInt()
            val currentSize = b.sliderVideoSize.value.toInt()
            updateResetBtnsVisibility(radius, currentSize)
            if (fromUser) {
                val shapeTag = radius.toString()
                sharedPrefs.edit()
                    .putInt("video_message_corner_radius", radius)
                    .putString("video_message_shape", shapeTag)
                    .apply()
                updatePreviewAndLabels(shapeTag, radius, currentSize)

                for (i in 0 until b.chipGroupVideoShapes.childCount) {
                    val chip = b.chipGroupVideoShapes.getChildAt(i) as? Chip ?: continue
                    val tag = chip.tag as? String
                    val matches = (radius >= 50 && tag == "50") || (radius == 0 && tag == "0")
                    chip.isChecked = matches
                    styleChip(chip, matches)
                }
            }
        }

        b.sliderVideoSize.addOnChangeListener { _, value, fromUser ->
            val size = value.toInt()
            val currentShapeTag = sharedPrefs.getString("video_message_shape", "50") ?: "50"
            val currentRadius = b.sliderVideoCornerRadius.value.toInt()
            updateResetBtnsVisibility(currentRadius, size)
            if (fromUser) {
                sharedPrefs.edit().putInt("video_message_size_dp", size).apply()
                updatePreviewAndLabels(currentShapeTag, currentRadius, size)
            }
        }

        val qualityOptions = arrayOf("Низкое (Быстрое)", "Среднее (Баланс)", "Высокое (HD)")
        var qualityIdx = sharedPrefs.getInt("video_message_quality", 0).coerceIn(0, qualityOptions.lastIndex)
        b.tvVideoQualityValue.text = qualityOptions[qualityIdx]
        b.tvVideoQualityValue.setTextColor(accentColor)
        b.rowVideoQuality.setOnClickListener { v ->
            val menu = PrimePopupMenu.create(this, v)
            qualityOptions.forEachIndexed { index, title ->
                menu.addItem(title, isSelected = (index == qualityIdx)) {
                    qualityIdx = index
                    sharedPrefs.edit().putInt("video_message_quality", index).apply()
                    b.tvVideoQualityValue.text = title
                }
            }
            menu.show()
        }

        val fpsOptions = arrayOf("30 FPS", "60 FPS (Плавно)")
        var fpsIdx = sharedPrefs.getInt("video_message_fps", 0).coerceIn(0, fpsOptions.lastIndex)
        b.tvVideoFpsValue.text = fpsOptions[fpsIdx]
        b.tvVideoFpsValue.setTextColor(accentColor)
        b.rowVideoFps.setOnClickListener { v ->
            val menu = PrimePopupMenu.create(this, v)
            fpsOptions.forEachIndexed { index, title ->
                menu.addItem(title, isSelected = (index == fpsIdx)) {
                    fpsIdx = index
                    sharedPrefs.edit().putInt("video_message_fps", index).apply()
                    b.tvVideoFpsValue.text = title
                }
            }
            menu.show()
        }

        b.switchVideoStabilization.isChecked = sharedPrefs.getBoolean("video_message_stabilization", true)
        b.switchVideoStabilization.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("video_message_stabilization", isChecked).apply()
        }

        val apiOptions = arrayOf("CameraX (Стабильно)", "Camera2 (Устаревшее)")
        var apiIdx = sharedPrefs.getInt("video_message_api", 0).coerceIn(0, apiOptions.lastIndex)
        b.tvCameraApiValue.text = apiOptions[apiIdx]
        b.tvCameraApiValue.setTextColor(accentColor)
        b.rowCameraApi.setOnClickListener { v ->
            val menu = PrimePopupMenu.create(this, v)
            apiOptions.forEachIndexed { index, title ->
                menu.addItem(title, isSelected = (index == apiIdx)) {
                    apiIdx = index
                    sharedPrefs.edit().putInt("video_message_api", index).apply()
                    b.tvCameraApiValue.text = title
                }
            }
            menu.show()
        }
    }
}
