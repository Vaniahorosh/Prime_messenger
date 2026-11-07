package com.messenger.prime

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eightbitlab.com.blurview.BlurView

class ChatHeaderMenuBottomSheet(
    private val activity: Activity,
    private val contactName: String,
    private val isConnected: Boolean,
    private val isReaderMode: Boolean,
    private val onProfileClick: () -> Unit,
    private val onToTopClick: () -> Unit,
    private val onSearchClick: () -> Unit,
    private val onWallpaperClick: () -> Unit,
    private val onMuteClick: () -> Unit,
    private val onReaderModeToggle: () -> Unit,
    private val onDeleteClick: () -> Unit
) {
    private var dialog: BottomSheetDialog? = null

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_chat_header_menu, null, false)

        val d = BottomSheetDialog(activity, R.style.Theme_Prime_BottomSheetDialog)
        d.setContentView(view)

        // Make background transparent for floating islands
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        d.setOnShowListener {
            val bottomSheetView = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                bottomSheetView.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(bottomSheetView)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.window?.setDimAmount(0.45f)

        // Setup BlurViews
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
            ?: (activity.window.decorView as ViewGroup)
        val windowBg = activity.window.decorView.background
        val overlayColor = getDefaultBlurOverlayColor(activity)

        view.findViewById<BlurView>(R.id.blurHeaderMenu)?.setupBlur(rootView, 16f, overlayColor, windowBg)
        view.findViewById<BlurView>(R.id.blurHeaderMenuCancel)?.setupBlur(rootView, 16f, overlayColor, windowBg)

        // Initialize UI
        val tvMenuTitle = view.findViewById<TextView>(R.id.tvMenuTitle)
        tvMenuTitle.text = contactName

        val btnProfile = view.findViewById<View>(R.id.btnMenuProfile)
        val btnToTop = view.findViewById<View>(R.id.btnMenuToTop)
        val btnSearch = view.findViewById<View>(R.id.btnMenuSearch)
        val btnWallpaper = view.findViewById<View>(R.id.btnMenuWallpaper)
        val btnMute = view.findViewById<View>(R.id.btnMenuMute)
        val btnReaderMode = view.findViewById<View>(R.id.btnMenuReaderMode)
        val btnDelete = view.findViewById<View>(R.id.btnMenuDelete)
        val btnCancel = view.findViewById<View>(R.id.btnMenuCancel)

        val tvReaderModeText = view.findViewById<TextView>(R.id.tvReaderModeText)
        val ivReaderModeIcon = view.findViewById<ImageView>(R.id.ivReaderModeIcon)

        if (isReaderMode) {
            tvReaderModeText.text = "Отключить читателя"
            ivReaderModeIcon.setImageResource(R.drawable.ic_close)
        } else {
            tvReaderModeText.text = "Режим читателя"
            ivReaderModeIcon.setImageResource(R.drawable.ic_book)
        }

        // Apply theme color
        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)

        ColorAccentManager.tintViewTree(view, accentColor)

        // Some functions unavailable if not connected
        if (!isConnected && !isReaderMode) {
            btnMute.alpha = 0.5f
            btnMute.isEnabled = false
            btnWallpaper.alpha = 0.5f
            btnWallpaper.isEnabled = false
        }

        // Click listeners
        btnProfile.setOnClickListener { d.dismiss(); onProfileClick() }
        btnToTop.setOnClickListener { d.dismiss(); onToTopClick() }
        btnSearch.setOnClickListener { d.dismiss(); onSearchClick() }
        btnWallpaper.setOnClickListener { d.dismiss(); onWallpaperClick() }
        btnMute.setOnClickListener { d.dismiss(); onMuteClick() }
        btnReaderMode.setOnClickListener { d.dismiss(); onReaderModeToggle() }
        btnDelete.setOnClickListener { d.dismiss(); onDeleteClick() }
        btnCancel.setOnClickListener { d.dismiss() }

        dialog = d
        d.show()
    }
}
