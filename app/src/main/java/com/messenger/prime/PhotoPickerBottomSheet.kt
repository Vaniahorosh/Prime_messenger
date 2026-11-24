package com.messenger.prime

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurView
import java.util.Locale
import java.util.concurrent.Executors

class PhotoPickerBottomSheet(
    private val activity: Activity,
    private val title: String = "Выбор фото",
    private val allowGif: Boolean = true,
    private val showPresets: Boolean = false,
    private val onMediaSelected: (uri: Uri, isGif: Boolean) -> Unit,
    private val onOpenSystemPicker: () -> Unit,
    private val onPresetSelected: ((presetUri: Uri) -> Unit)? = null,
    private val onRequestPermission: (() -> Unit)? = null,
    private val onDismissWithoutSelection: (() -> Unit)? = null
) {

    data class PickerMediaItem(
        val uri: Uri,
        val isGif: Boolean
    )

    private var dialog: BottomSheetDialog? = null
    private var rootView: View? = null
    private var adapter: PickerGridAdapter? = null
    private var madeSelection = false
    private val ioExecutor = Executors.newSingleThreadExecutor()

    companion object {
        fun hasGalleryReadAccess(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun isPartialAccessOnly(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
            val fullGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val partialGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            return !fullGranted && partialGranted
        }

        fun getRequiredImagePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_photo_picker, null, false)
        rootView = view
        madeSelection = false

        val d = BottomSheetDialog(activity, R.style.Theme_Prime_BottomSheetDialog)
        d.setContentView(view)
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.45f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    attributes = attributes.apply { blurBehindRadius = 48 }
                } catch (_: Throwable) {}
            }
        }

        d.setOnShowListener {
            val bottomSheetView = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                bottomSheetView.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(bottomSheetView)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val blurTarget = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
            ?: (activity.window.decorView as ViewGroup)
        val overlayColor = getDefaultBlurOverlayColor(activity)
        val blurView = view.findViewById<BlurView>(R.id.blurPhotoPicker)
        blurView?.setupBlur(blurTarget, 20f, overlayColor, activity.window.decorView.background)
        setBlurAutoUpdateSafe(blurView, true)

        val contentLayout = view.findViewById<View>(R.id.layoutPhotoPickerContent)
        val density = activity.resources.displayMetrics.density
        val baseBottomPad = (16 * density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(contentLayout) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val bottomInset = maxOf(sysBars.bottom, cutout.bottom)
            v.updatePadding(bottom = baseBottomPad + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(contentLayout)

        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
        val accentTint = ColorStateList.valueOf(accentColor)

        view.findViewById<TextView>(R.id.tvPickerTitle)?.text = title

        val btnSystemGallery = view.findViewById<MaterialButton>(R.id.btnPickerSystemGallery)
        btnSystemGallery?.setTextColor(accentColor)
        btnSystemGallery?.iconTint = accentTint
        btnSystemGallery?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            madeSelection = true
            d.dismiss()
            onOpenSystemPicker()
        }

        val btnEmptySystem = view.findViewById<MaterialButton>(R.id.btnPickerEmptySystem)
        btnEmptySystem?.setTextColor(accentColor)
        btnEmptySystem?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            madeSelection = true
            d.dismiss()
            onOpenSystemPicker()
        }

        val layoutPresets = view.findViewById<View>(R.id.layoutPresetAvatars)
        if (showPresets) {
            layoutPresets?.visibility = View.VISIBLE
            val presetPairs = listOf(
                R.id.ivPresetAvatar1 to "android.resource://${activity.packageName}/drawable/avatar_1",
                R.id.ivPresetAvatar2 to "android.resource://${activity.packageName}/drawable/avatar_2",
                R.id.ivPresetAvatar3 to "android.resource://${activity.packageName}/drawable/avatar_3",
                R.id.ivPresetAvatar4 to "android.resource://${activity.packageName}/drawable/avatar_4",
                R.id.ivPresetAvatar5 to "android.resource://${activity.packageName}/drawable/avatar_5"
            )
            presetPairs.forEach { (id, uriStr) ->
                view.findViewById<View>(id)?.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    madeSelection = true
                    val presetUri = Uri.parse(uriStr)
                    d.dismiss()
                    if (onPresetSelected != null) {
                        onPresetSelected.invoke(presetUri)
                    } else {
                        onMediaSelected(presetUri, false)
                    }
                }
            }
        } else {
            layoutPresets?.visibility = View.GONE
        }

        val gridContainer = view.findViewById<View>(R.id.layoutPickerGridContainer)
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val gridHeight = (screenHeight * 0.42f).toInt().coerceIn((240 * density).toInt(), (380 * density).toInt())
        gridContainer?.layoutParams = gridContainer?.layoutParams?.apply { height = gridHeight }

        val rvMedia = view.findViewById<RecyclerView>(R.id.rvPickerMedia)
        val isTablet = activity.resources.configuration.smallestScreenWidthDp >= 600
        rvMedia?.layoutManager = GridLayoutManager(activity, if (isTablet) 5 else 3)
        rvMedia?.setHasFixedSize(true)
        val gridAdapter = PickerGridAdapter { item ->
            madeSelection = true
            d.dismiss()
            onMediaSelected(item.uri, item.isGif)
        }
        adapter = gridAdapter
        rvMedia?.adapter = gridAdapter

        d.setOnDismissListener {
            setBlurAutoUpdateSafe(blurView, false)
            ioExecutor.shutdownNow()
            dialog = null
            rootView = null
            if (!madeSelection) {
                onDismissWithoutSelection?.invoke()
            }
        }

        dialog = d
        refreshMedia()
        d.show()
    }

    fun refreshMedia() {
        val view = rootView ?: return
        val hasAccess = hasGalleryReadAccess(activity)
        val partialOnly = isPartialAccessOnly(activity)

        val banner = view.findViewById<View>(R.id.layoutPickerAccessBanner)
        val tvHint = view.findViewById<TextView>(R.id.tvPickerAccessHint)
        val btnGrant = view.findViewById<MaterialButton>(R.id.btnPickerGrantAccess)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvPickerSubtitle)
        val layoutEmpty = view.findViewById<View>(R.id.layoutPickerEmpty)
        val tvEmpty = view.findViewById<TextView>(R.id.tvPickerEmpty)
        val rvMedia = view.findViewById<RecyclerView>(R.id.rvPickerMedia)

        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
        btnGrant?.setTextColor(accentColor)

        if (!hasAccess) {
            banner?.visibility = if (onRequestPermission != null) View.VISIBLE else View.GONE
            tvHint?.text = "Разрешите доступ для показа недавних фото"
            btnGrant?.text = "Разрешить"
            btnGrant?.setOnClickListener {
                onRequestPermission?.invoke()
            }
            tvSubtitle?.text = "Можно выбрать фото в системной галерее"
            rvMedia?.visibility = View.GONE
            layoutEmpty?.visibility = View.VISIBLE
            tvEmpty?.text = "Нет доступа к ленте фото"
            return
        }

        if (partialOnly && onRequestPermission != null) {
            banner?.visibility = View.VISIBLE
            tvHint?.text = "Показаны только выбранные фото"
            btnGrant?.text = "Выбрать ещё"
            btnGrant?.setOnClickListener {
                onRequestPermission.invoke()
            }
        } else {
            banner?.visibility = View.GONE
        }

        if (ioExecutor.isShutdown) return
        ioExecutor.execute {
            val loaded = queryRecentImages()
            activity.runOnUiThread {
                if (dialog == null || rootView == null) return@runOnUiThread
                adapter?.setItems(loaded)
                if (loaded.isEmpty()) {
                    rvMedia?.visibility = View.GONE
                    layoutEmpty?.visibility = View.VISIBLE
                    tvEmpty?.text = "Фотографии не найдены"
                    tvSubtitle?.text = "Откройте системную галерею"
                } else {
                    rvMedia?.visibility = View.VISIBLE
                    layoutEmpty?.visibility = View.GONE
                    tvSubtitle?.text = if (allowGif) "Недавние фото и GIF • ${loaded.size}" else "Недавние фото • ${loaded.size}"
                }
            }
        }
    }

    private fun queryRecentImages(): List<PickerMediaItem> {
        val result = mutableListOf<PickerMediaItem>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        try {
            activity.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                var count = 0
                while (cursor.moveToNext() && count < 90) {
                    val id = cursor.getLong(idCol)
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""
                    val isGif = mime.equals("image/gif", ignoreCase = true) ||
                        name.lowercase(Locale.ROOT).endsWith(".gif")
                    if (!allowGif && isGif) continue

                    val itemUri = ContentUris.withAppendedId(uri, id)
                    result.add(PickerMediaItem(itemUri, isGif))
                    count++
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private class PickerGridAdapter(
        private val onClick: (PickerMediaItem) -> Unit
    ) : RecyclerView.Adapter<PickerGridAdapter.ViewHolder>() {

        private var items: List<PickerMediaItem> = emptyList()

        fun setItems(newItems: List<PickerMediaItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_picker_media, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvGifBadge.visibility = if (item.isGif) View.VISIBLE else View.GONE
            Glide.with(holder.ivThumb.context)
                .load(item.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_photo)
                .into(holder.ivThumb)
            holder.itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivThumb: ImageView = itemView.findViewById(R.id.ivPickerThumb)
            val tvGifBadge: TextView = itemView.findViewById(R.id.tvPickerGifBadge)
        }
    }
}
