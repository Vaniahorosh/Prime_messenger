package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import eightbitlab.com.blurview.BlurView as EightBitBlurView

/**
 * Глобальное универсальное выпадающее меню в стиле M3 Expressive с поддержкой настоящего BlurView.
 */
class PrimePopupMenu private constructor(
    private val context: Context,
    private val anchorView: View
) {
    data class MenuItem(
        val id: Int,
        val title: CharSequence,
        @DrawableRes val iconResId: Int? = null,
        val isSelected: Boolean = false,
        val onClick: () -> Unit
    )

    private val items = mutableListOf<MenuItem>()
    private var popupWindow: PopupWindow? = null
    private var isDismissing = false

    fun addItem(
        id: Int,
        title: CharSequence,
        @DrawableRes iconResId: Int? = null,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ): PrimePopupMenu {
        items.add(MenuItem(id, title, iconResId, isSelected, onClick))
        return this
    }

    fun addItem(
        title: CharSequence,
        @DrawableRes iconResId: Int? = null,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ): PrimePopupMenu {
        items.add(MenuItem(items.size, title, iconResId, isSelected, onClick))
        return this
    }

    fun show() {
        if (items.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val blurBgColor = if (isDark) Color.parseColor("#450F172A") else Color.parseColor("#45154B87")
        val strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#20FFFFFF")
        val itemUnselectedBg = if (isDark) Color.parseColor("#15FFFFFF") else Color.parseColor("#0F000000")
        val itemTextColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")

        // Root Blur Card Container
        val blurCard = EightBitBlurView(context).apply {
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(blurBgColor)
                setStroke((1 * density).toInt(), strokeColor)
            }
            background = bgDrawable
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 20f * density
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
        }

        // Blur setup over parent window decorView
        val activity = (context as? Activity) ?: (anchorView.context as? Activity)
        if (activity != null) {
            try {
                val rootView = (activity.window.decorView.findViewById<ViewGroup>(android.R.id.content))
                    ?: (activity.window.decorView as ViewGroup)
                val windowBg = activity.window.decorView.background
                val overlayColor = getDefaultBlurOverlayColor(activity)
                blurCard.setupBlur(rootView, 20f, overlayColor, windowBg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Items container layout
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Build items
        items.forEachIndexed { index, item ->
            val itemView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (42 * density).toInt()
                ).apply {
                    setMargins(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)

                val itemBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    if (item.isSelected) {
                        setColor(accentColor)
                    } else {
                        setColor(itemUnselectedBg)
                    }
                }
                background = itemBg

                if (item.iconResId != null && item.iconResId != 0) {
                    val iconIv = ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (20 * density).toInt(),
                            (20 * density).toInt()
                        ).apply {
                            setMargins(0, 0, (12 * density).toInt(), 0)
                        }
                        setImageResource(item.iconResId)
                        val iconColor = if (item.isSelected) Color.WHITE else accentColor
                        setColorFilter(iconColor)
                    }
                    addView(iconIv)
                }

                val tv = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    text = item.title
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (item.isSelected) Color.WHITE else itemTextColor)
                    setSingleLine()
                    ellipsize = TextUtils.TruncateAt.END
                }
                addView(tv)

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).setInterpolator(OvershootInterpolator(1.1f)).start()
                        }
                    }
                    false
                }

                setOnClickListener {
                    dismissAnimated {
                        item.onClick()
                    }
                }
            }
            container.addView(itemView)
        }

        blurCard.addView(container)

        // Measure card dimensions
        blurCard.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val minWidth = (180 * density).toInt()
        val popupWidth = Math.max(blurCard.measuredWidth, minWidth)
        val popupHeight = blurCard.measuredHeight

        popupWindow = PopupWindow(
            blurCard,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 20f * density
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
        }

        // Screen position calculation
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorWidth = anchorView.width
        val anchorHeight = anchorView.height

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        var x = anchorX + (anchorWidth / 2) - (popupWidth / 2)
        if (x + popupWidth > screenWidth - (16 * density).toInt()) {
            x = screenWidth - popupWidth - (16 * density).toInt()
        }
        if (x < (16 * density).toInt()) {
            x = (16 * density).toInt()
        }

        var y = anchorY + anchorHeight + (4 * density).toInt()
        val isAbove = (y + popupHeight > screenHeight - (32 * density).toInt())
        if (isAbove) {
            y = anchorY - popupHeight - (4 * density).toInt()
        }

        val pivotX = if (x + (popupWidth / 2) > anchorX + (anchorWidth / 2)) 0f else popupWidth.toFloat()
        val pivotY = if (isAbove) popupHeight.toFloat() else 0f

        blurCard.pivotX = pivotX
        blurCard.pivotY = pivotY
        blurCard.scaleX = 0.85f
        blurCard.scaleY = 0.85f
        blurCard.alpha = 0f

        popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)

        // Card scale animation
        blurCard.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(260)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Cascade children items animation
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = if (isAbove) (16f * density) else (-16f * density)
            child.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(180)
                .setStartDelay(i * 20L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun dismiss() {
        dismissAnimated(null)
    }

    private fun dismissAnimated(onComplete: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true

        val popup = popupWindow ?: run {
            onComplete?.invoke()
            return
        }

        val blurCard = popup.contentView as? ViewGroup ?: run {
            popup.dismiss()
            onComplete?.invoke()
            return
        }

        val container = blurCard.getChildAt(0) as? ViewGroup
        val density = context.resources.displayMetrics.density

        if (container != null) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.animate().cancel()
                child.animate()
                    .alpha(0f)
                    .translationY(-10f * density)
                    .setDuration(120)
                    .start()
            }
        }

        blurCard.animate().cancel()
        blurCard.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(160)
            .withEndAction {
                try {
                    popup.dismiss()
                } catch (ignored: Exception) {}
                onComplete?.invoke()
            }
            .start()
    }

    companion object {
        @JvmStatic
        fun create(context: Context, anchorView: View): PrimePopupMenu {
            return PrimePopupMenu(context, anchorView)
        }

        @JvmStatic
        fun show(
            context: Context,
            anchorView: View,
            titles: List<String>,
            icons: List<Int>?,
            selectedIndex: Int,
            onItemSelect: (Int) -> Unit
        ) {
            val menu = create(context, anchorView)
            titles.forEachIndexed { index, title ->
                val icon = if (icons != null && index in icons.indices) icons[index] else null
                menu.addItem(index, title, iconResId = icon, isSelected = (index == selectedIndex)) {
                    onItemSelect(index)
                }
            }
            menu.show()
        }
    }
}
