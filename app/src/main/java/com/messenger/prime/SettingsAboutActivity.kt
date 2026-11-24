package com.messenger.prime

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.messenger.prime.browser.AppBrowserManager
import com.messenger.prime.databinding.ActivitySettingsAboutBinding
import com.messenger.prime.shapes.M3EDrawable
import com.messenger.prime.shapes.M3EShapes

class SettingsAboutActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsAboutBinding.inflate(layoutInflater)
        setContentView(b.root)

        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val vName = pInfo.versionName ?: "1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            "Версия $vName (build $vCode)"
        } catch (_: Exception) {
            "Версия ${BuildConfig.VERSION_NAME}"
        }
        b.tvAppVersion.text = versionName

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutAboutHeroStage,
            heroOverlay = b.layoutAboutHeroOverlay,
            scrollView = b.scrollAboutSettings,
            title = "Информация",
            subtitle = null,
            heroHeightFraction = 0.34f,
            heroMinDp = 244,
            heroMaxDp = 304,
            animatedCards = listOf(b.layoutAboutActions, b.cardThanks)
        )

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        b.vAboutHeroWallpaper.setLiveTempWallpaper(
            ChatWallpaper(
                id = "about_hero_backdrop",
                name = "About",
                type = "MATERIAL",
                patternType = "material_all",
                isAnimated = false
            )
        )

        val heroBgAlpha = if (isDark) 72 else 38
        b.flAboutHeroIcon.background = M3EDrawable(
            shape = M3EShapes.cookie7Sided,
            color = ColorUtils.setAlphaComponent(accentColor, heroBgAlpha)
        )
        b.ivAboutHeroLogo.imageTintList = ColorStateList.valueOf(accentColor)
        b.flAboutHeroIcon.addBounceTouchEffect()

        applyHorizontalSegmentedShape(b.cardGithub, isLeft = true)
        applyHorizontalSegmentedShape(b.cardCheckUpdate, isLeft = false)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconGithub, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconCheckUpdate, accentColor, M3EShapes.verySunny, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivThanksIcon, accentColor, M3EShapes.cookie6Sided, isDark)

        b.tvTitleThanks.setTextColor(accentColor)

        b.cardGithub.addBounceTouchEffect()
        b.cardCheckUpdate.addBounceTouchEffect()
        b.cardThanks.addBounceTouchEffect()

        b.cardGithub.setOnClickListener {
            AppBrowserManager.openUrl(this, "https://github.com/Vaniahorosh/Prime_messenger")
        }

        b.cardCheckUpdate.setOnClickListener {
            UpdateChecker.checkForUpdates(this, manualCheck = true)
            PrimeNotification.show(this, "Поиск обновлений...")
        }

        setupThanksCard()
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun applyHorizontalSegmentedShape(card: MaterialCardView, isLeft: Boolean) {
        val density = resources.displayMetrics.density
        val outer = 24f * density
        val inner = 10f * density
        val leftRadius = if (isLeft) outer else inner
        val rightRadius = if (isLeft) inner else outer

        card.strokeWidth = 0
        card.cardElevation = 0f
        card.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, leftRadius)
            .setBottomLeftCorner(CornerFamily.ROUNDED, leftRadius)
            .setTopRightCorner(CornerFamily.ROUNDED, rightRadius)
            .setBottomRightCorner(CornerFamily.ROUNDED, rightRadius)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupThanksCard() {
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
                spawnFloatingHeart(e.x, e.y)
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
        AppBrowserManager.openUrl(this, "https://github.com/Vaniahorosh")
    }

    private fun spawnFloatingHeart(x: Float, y: Float) {
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
}
