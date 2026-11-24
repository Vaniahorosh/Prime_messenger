package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.embedding.ActivityEmbeddingController
import com.google.android.material.imageview.ShapeableImageView
import com.messenger.prime.shapes.M3EShapes
import eightbitlab.com.blurview.BlurView
import org.json.JSONArray

object PrimeBottomNavHelper {

    enum class NavTab {
        CHATS, DEVICES, AI, SEARCH, DOWNLOADS, PROFILE
    }

    fun isBottomBarEnabled(activity: Activity, hideOnTabletOrEmbedded: Boolean = false): Boolean {
        val sharedPrefs = activity.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"
        if (navStyle != "bottom_bar") return false
        if (!hideOnTabletOrEmbedded) return true

        val isTablet = activity.resources.configuration.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            ActivityEmbeddingController.getInstance(activity).isActivityEmbedded(activity)
        } catch (_: Exception) {
            false
        }
        return !isTablet && !isEmbedded
    }

    fun applyBottomMargin(navView: View, bottomInsetPx: Int, extraDp: Int = 12) {
        val density = navView.resources.displayMetrics.density
        val lp = navView.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val targetMargin = bottomInsetPx + (extraDp * density).toInt()
        if (lp.bottomMargin != targetMargin) {
            lp.bottomMargin = targetMargin
            navView.layoutParams = lp
        }
    }

    fun updateForIme(navView: View?, isImeVisible: Boolean, shouldShowWhenClosed: Boolean) {
        if (navView == null) return
        if (!shouldShowWhenClosed) {
            navView.visibility = View.GONE
            setBlurAutoUpdateSafe(navView, false)
            return
        }
        if (isImeVisible) {
            if (navView.visibility == View.VISIBLE) {
                navView.animate().cancel()
                navView.visibility = View.GONE
                setBlurAutoUpdateSafe(navView, false)
            }
        } else {
            if (navView.visibility != View.VISIBLE) {
                navView.alpha = 0f
                navView.translationY = 24f * navView.resources.displayMetrics.density
                navView.visibility = View.VISIBLE
                navView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            setBlurAutoUpdateSafe(navView, true)
        }
    }

    fun setup(
        activity: Activity,
        activeTab: NavTab,
        blurTargetRoot: ViewGroup? = null,
        hideOnTabletOrEmbedded: Boolean = false,
        onBeforeNavigate: (() -> Unit)? = null,
        onSameTabReselect: ((NavTab) -> Unit)? = null,
        onTabClickOverride: ((NavTab) -> Boolean)? = null
    ): View? {
        val navView = activity.findViewById<View>(R.id.blurBottomNav) ?: return null
        if (!isBottomBarEnabled(activity, hideOnTabletOrEmbedded)) {
            navView.visibility = View.GONE
            setBlurAutoUpdateSafe(navView, false)
            return navView
        }

        val sharedPrefs = activity.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        navView.visibility = View.VISIBLE
        navView.translationY = 0f
        navView.alpha = 1f
        navView.bringToFront()

        val rootInsets = ViewCompat.getRootWindowInsets(navView) ?: ViewCompat.getRootWindowInsets(activity.window.decorView)
        if (rootInsets != null) {
            val sysBottom = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val cutoutBottom = rootInsets.getInsets(WindowInsetsCompat.Type.displayCutout()).bottom
            applyBottomMargin(navView, maxOf(sysBottom, cutoutBottom))
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

        val showLabels = sharedPrefs.getBoolean("nav_show_labels", false)
        applyBottomNavLabelsVisibility(navView, showLabels)

        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
        val isDark = SettingsSupport.isDarkThemeActive(activity)

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

        val tvNavChatsLabel = navView.findViewById<TextView>(R.id.tvNavChatsLabel)
        val tvNavDevicesLabel = navView.findViewById<TextView>(R.id.tvNavDevicesLabel)
        val tvNavAiLabel = navView.findViewById<TextView>(R.id.tvNavAiLabel)
        val tvNavSearchLabel = navView.findViewById<TextView>(R.id.tvNavSearchLabel)
        val tvNavDownloadsLabel = navView.findViewById<TextView>(R.id.tvNavDownloadsLabel)
        val tvNavProfileLabel = navView.findViewById<TextView>(R.id.tvNavProfileLabel)

        val disableAnims = PrimeTransitions.areAnimationsDisabled(activity)
        updateTabState(activity, activeTab == NavTab.CHATS, vNavChatsIndicator, ivNavChatsIcon, tvNavChatsLabel, accentColor, isDark, disableAnims)
        updateTabState(activity, activeTab == NavTab.DEVICES, vNavDevicesIndicator, ivNavDevicesIcon, tvNavDevicesLabel, accentColor, isDark, disableAnims)
        updateTabState(activity, activeTab == NavTab.AI, vNavAiIndicator, ivNavAiIcon, tvNavAiLabel, accentColor, isDark, disableAnims)
        updateTabState(activity, activeTab == NavTab.SEARCH, vNavSearchIndicator, ivNavSearchIcon, tvNavSearchLabel, accentColor, isDark, disableAnims)
        updateTabState(activity, activeTab == NavTab.DOWNLOADS, vNavDownloadsIndicator, ivNavDownloadsIcon, tvNavDownloadsLabel, accentColor, isDark, disableAnims)
        updateTabState(activity, activeTab == NavTab.PROFILE, vNavProfileIndicator, null, tvNavProfileLabel, accentColor, isDark, disableAnims)
        updateNavUnreadBadge(activity, navView)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val myAvatar = AvatarManager.getMyAvatarUri(activity)
            val myName = AvatarManager.getMyDisplayName(activity)
            loadAvatarIntoView(activity, myAvatar, profileAvatarIv, myName)
            if (profileAvatarIv is ShapeableImageView) {
                val density = activity.resources.displayMetrics.density
                if (activeTab == NavTab.PROFILE) {
                    val isBrightAccent = ColorUtils.calculateLuminance(accentColor) > 0.5
                    profileAvatarIv.strokeColor = ColorStateList.valueOf(if (isBrightAccent) Color.parseColor("#0F172A") else Color.WHITE)
                    profileAvatarIv.strokeWidth = 1.5f * density
                } else {
                    profileAvatarIv.strokeWidth = 0f
                }
            }
        }

        val blurNavView = navView as? BlurView
        if (blurNavView != null) {
            val overlayColor = getDefaultBlurOverlayColor(activity)
            val targetRootView = blurTargetRoot
                ?: activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                ?: (activity.window.decorView as ViewGroup)
            blurNavView.setupBlur(targetRootView, 20f, overlayColor, activity.window.decorView.background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                blurNavView.applyM3EPathProvider(M3EShapes::pill)
            }
            setBlurAutoUpdateSafe(blurNavView, true)
        }

        val hapticTap = { view: View ->
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }

        val bindTab = { view: View?, tab: NavTab, action: () -> Unit ->
            view?.let { v ->
                if (!disableAnims) {
                    v.addM3EBounceEffect(scaleDown = 0.93f)
                }
                v.setOnClickListener {
                    hapticTap(it)
                    if (onTabClickOverride?.invoke(tab) == true) return@setOnClickListener
                    if (tab == activeTab) {
                        onSameTabReselect?.invoke(tab)
                    } else {
                        onBeforeNavigate?.invoke()
                        action()
                    }
                }
            }
        }

        val finishIfSubRoot = {
            if (activity !is ChatListActivity) {
                activity.finish()
            }
        }

        bindTab(btnNavChats, NavTab.CHATS) {
            val intent = Intent(activity, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(activity, isForward = false)
            finishIfSubRoot()
        }

        bindTab(btnNavDevices, NavTab.DEVICES) {
            val intent = Intent(activity, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_ACTION_SCAN", true)
            }
            activity.startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(activity, isForward = false)
            finishIfSubRoot()
        }

        bindTab(btnNavAi, NavTab.AI) {
            val intent = Intent(activity, AiChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            PrimeTransitions.applyOpenTransition(activity)
            finishIfSubRoot()
        }

        bindTab(btnNavSearch, NavTab.SEARCH) {
            val intent = Intent(activity, GlobalSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(activity, isForward = tabOrderForward(activeTab, NavTab.SEARCH))
            finishIfSubRoot()
        }

        bindTab(btnNavDownloads, NavTab.DOWNLOADS) {
            DownloadsActivity.openWithPermissionCheck(activity)
        }

        bindTab(btnNavProfile, NavTab.PROFILE) {
            val intent = Intent(activity, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(activity, isForward = true)
            finishIfSubRoot()
        }

        return navView
    }

    private fun tabOrderForward(from: NavTab, to: NavTab): Boolean {
        return to.ordinal > from.ordinal
    }

    fun updateNavUnreadBadge(context: Context, navView: View?) {
        val tvBadge = navView?.findViewById<TextView>(R.id.tvNavChatsUnreadBadge) ?: return
        var totalUnread = 0
        try {
            val json = ChatHistoryManager.getPersistedChatsJson(context)
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                totalUnread += obj.optInt("unreadCount", 0)
            }
        } catch (_: Exception) {}

        if (totalUnread > 0) {
            tvBadge.text = if (totalUnread > 99) "99+" else totalUnread.toString()
            tvBadge.visibility = View.VISIBLE
        } else {
            tvBadge.visibility = View.GONE
        }
    }

    private fun updateTabState(
        context: Context,
        isActive: Boolean,
        indicatorView: View?,
        iconView: ImageView?,
        labelView: TextView?,
        accentColor: Int,
        isDark: Boolean,
        disableAnims: Boolean
    ) {
        if (indicatorView == null) return
        val inactiveColor = ContextCompat.getColor(context, R.color.prime_text_secondary)
        val isBrightAccent = ColorUtils.calculateLuminance(accentColor) > 0.45
        val activeIconColor = if (isBrightAccent) Color.parseColor("#0F172A") else Color.WHITE

        indicatorView.visibility = View.VISIBLE
        indicatorView.animate().cancel()
        iconView?.animate()?.cancel()

        if (isActive) {
            indicatorView.backgroundTintList = ColorStateList.valueOf(accentColor)
            if (disableAnims) {
                indicatorView.scaleX = 1.0f
                indicatorView.scaleY = 1.0f
                indicatorView.alpha = 1.0f
                iconView?.scaleX = 1.08f
                iconView?.scaleY = 1.08f
            } else {
                indicatorView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(240)
                    .setInterpolator(OvershootInterpolator(1.25f))
                    .start()
                iconView?.animate()?.scaleX(1.08f)?.scaleY(1.08f)?.setDuration(180)?.start()
            }
            iconView?.setColorFilter(activeIconColor)
            labelView?.setTextColor(accentColor)
        } else {
            indicatorView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            if (disableAnims) {
                indicatorView.scaleX = 0.72f
                indicatorView.scaleY = 0.72f
                indicatorView.alpha = 0f
                iconView?.scaleX = 1.0f
                iconView?.scaleY = 1.0f
            } else {
                indicatorView.animate()
                    .scaleX(0.72f)
                    .scaleY(0.72f)
                    .alpha(0f)
                    .setDuration(160)
                    .start()
                iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(160)?.start()
            }
            iconView?.setColorFilter(inactiveColor)
            labelView?.setTextColor(if (isDark) Color.parseColor("#94A3B8") else inactiveColor)
        }
    }
}
