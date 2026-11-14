package com.messenger.prime

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import eightbitlab.com.blurview.BlurView

class ChatWallpaperPickerBottomSheet(
    private val activity: Activity,
    private val contactKey: String?,
    private val contactName: String?,
    private val onWallpaperChanged: () -> Unit
) {
    private var dialog: BottomSheetDialog? = null
    private var selectedWallpaper: ChatWallpaper? = null

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_chat_wallpaper_picker, null, false)

        val d = BottomSheetDialog(activity, R.style.Theme_Prime_BottomSheetDialog)
        d.setContentView(view)

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

        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
            ?: (activity.window.decorView as ViewGroup)
        val windowBg = activity.window.decorView.background
        val overlayColor = getDefaultBlurOverlayColor(activity)

        view.findViewById<BlurView>(R.id.blurWallpaperPicker)?.setupBlur(rootView, 16f, overlayColor, windowBg)

        val tvTitle = view.findViewById<TextView>(R.id.tvPickerTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvPickerSubtitle)
        val tvBadge = view.findViewById<TextView>(R.id.tvActiveWallpaperBadge)
        val rvOptions = view.findViewById<RecyclerView>(R.id.rvWallpaperOptions)

        val btnApplyForChat = view.findViewById<MaterialButton>(R.id.btnApplyForChat)
        val btnApplyGlobal = view.findViewById<MaterialButton>(R.id.btnApplyGlobal)
        val btnOpenEditor = view.findViewById<MaterialButton>(R.id.btnOpenEditor)
        val btnReset = view.findViewById<MaterialButton>(R.id.btnResetContactWallpaper)

        val displayName = if (!contactName.isNullOrBlank()) contactName else "Чата"
        tvTitle.text = "Обои чата"
        tvSubtitle.text = if (!contactName.isNullOrBlank()) "Фон для чата с $contactName" else "Общий фон чатов"

        val activeWp = WallpaperManager.getActiveWallpaper(activity, contactKey)
        selectedWallpaper = activeWp
        val hasCustomOverride = WallpaperManager.hasCustomWallpaperForContact(activity, contactKey)

        if (hasCustomOverride) {
            tvBadge.text = "Персональные: ${activeWp.name}"
            btnReset?.visibility = View.VISIBLE
        } else {
            tvBadge.text = "Текущие: ${activeWp.name}"
            btnReset?.visibility = View.GONE
        }

        val allWallpapers = WallpaperManager.getWallpapersList(activity)

        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
        val accentStateList = ColorStateList.valueOf(accentColor)
        val dangerColor = ContextCompat.getColor(activity, R.color.prime_danger)
        val dangerStateList = ColorStateList.valueOf(dangerColor)

        btnApplyForChat?.backgroundTintList = accentStateList
        btnApplyGlobal?.strokeColor = accentStateList
        btnApplyGlobal?.setTextColor(ContextCompat.getColor(activity, R.color.prime_text_primary))

        btnOpenEditor?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        btnOpenEditor?.setTextColor(accentColor)
        btnOpenEditor?.iconTint = accentStateList

        btnReset?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        btnReset?.setTextColor(dangerColor)
        btnReset?.iconTint = dangerStateList

        val adapter = WallpaperPickerAdapter(allWallpapers, selectedWallpaper?.id ?: "default", accentColor) { wp ->
            selectedWallpaper = wp
            tvBadge.text = "Выбрано: ${wp.name}"
        }

        rvOptions.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        rvOptions.adapter = adapter

        btnApplyForChat?.setOnClickListener {
            val wp = selectedWallpaper ?: return@setOnClickListener
            if (!contactKey.isNullOrBlank()) {
                WallpaperManager.setActiveWallpaper(activity, wp.id, contactKey)
                PrimeNotification.show(activity, "Обои «${wp.name}» установлены для чата $displayName")
            } else {
                WallpaperManager.setActiveWallpaper(activity, wp.id, null)
                PrimeNotification.show(activity, "Обои «${wp.name}» установлены для всех чатов")
            }
            activity.sendBroadcast(Intent(WallpaperManager.ACTION_WALLPAPER_CHANGED).setPackage(activity.packageName))
            onWallpaperChanged()
            d.dismiss()
        }

        btnApplyGlobal?.setOnClickListener {
            val wp = selectedWallpaper ?: return@setOnClickListener
            WallpaperManager.setActiveWallpaper(activity, wp.id, null)
            activity.sendBroadcast(Intent(WallpaperManager.ACTION_WALLPAPER_CHANGED).setPackage(activity.packageName))
            PrimeNotification.show(activity, "Обои «${wp.name}» установлены по умолчанию для всех чатов")
            onWallpaperChanged()
            d.dismiss()
        }

        btnReset?.setOnClickListener {
            if (!contactKey.isNullOrBlank()) {
                WallpaperManager.removeContactWallpaper(activity, contactKey)
                activity.sendBroadcast(Intent(WallpaperManager.ACTION_WALLPAPER_CHANGED).setPackage(activity.packageName))
                PrimeNotification.show(activity, "Сброшено на стандартные обои")
                onWallpaperChanged()
                d.dismiss()
            }
        }

        btnOpenEditor?.setOnClickListener {
            val intent = Intent(activity, WallpaperEditorActivity::class.java).apply {
                putExtra(WallpaperManager.EXTRA_CONTACT_KEY, contactKey)
                putExtra(WallpaperManager.EXTRA_CONTACT_NAME, contactName)
            }
            activity.startActivity(intent)
            d.dismiss()
        }

        d.setOnDismissListener {
            dialog = null
            selectedWallpaper = null
        }

        dialog = d
        d.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        selectedWallpaper = null
    }

    private inner class WallpaperPickerAdapter(
        private val items: List<ChatWallpaper>,
        private var selectedId: String,
        private val accentColor: Int,
        private val onItemSelect: (ChatWallpaper) -> Unit
    ) : RecyclerView.Adapter<WallpaperPickerAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val cardWallpaper: MaterialCardView = v.findViewById(R.id.cardWallpaper)
            val vPreview: MaterialShapesWallpaperView = v.findViewById(R.id.vWallpaperPreview)
            val badge: FrameLayout = v.findViewById(R.id.layoutSelectionBadge)
            val tvName: TextView = v.findViewById(R.id.tvWallpaperName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_picker, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name

            holder.vPreview.setLiveTempWallpaper(item)

            val isSelected = (item.id == selectedId)
            if (isSelected) {
                holder.cardWallpaper.strokeWidth = (2.5f * holder.itemView.resources.displayMetrics.density).toInt()
                holder.cardWallpaper.strokeColor = accentColor
                holder.badge.visibility = View.VISIBLE
            } else {
                holder.cardWallpaper.strokeWidth = 0
                holder.badge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                val previousId = selectedId
                selectedId = item.id
                val prevIndex = items.indexOfFirst { it.id == previousId }
                if (prevIndex != -1) notifyItemChanged(prevIndex)
                notifyItemChanged(position)
                onItemSelect(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
