package com.messenger.prime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.marginEnd

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
        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val cardBgColor = if (isDark) Color.parseColor("#EE0F172A") else Color.parseColor("#FAFAFA")
        val strokeColor = if (isDark) Color.parseColor("#30FFFFFF") else Color.parseColor("#20000000")
        val itemUnselectedBg = if (isDark) Color.parseColor("#15FFFFFF") else Color.parseColor("#0F000000")
        val itemTextColor = if (isDark) Color.parseColor("#E2E8F0") else Color.parseColor("#1E293B")

        // Outer layout card
        val cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * density
            setColor(cardBgColor)
            setStroke((1 * density).toInt(), strokeColor)
        }

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground
            elevation = 24f * density
            setPadding((6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt())
            clipToOutline = true
        }

        // Items container
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Build items
        items.forEachIndexed { index, item ->
            val itemView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (38 * density).toInt()
                ).apply {
                    setMargins(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)

                val shapeDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12f * density
                    if (item.isSelected) {
                        setColor(accentColor)
                    } else {
                        setColor(itemUnselectedBg)
                    }
                }
                background = shapeDrawable

                if (item.iconResId != null) {
                    val iconIv = ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (18 * density).toInt(),
                            (18 * density).toInt()
                        ).apply {
                            setMargins(0, 0, (10 * density).toInt(), 0)
                        }
                        setImageResource(item.iconResId)
                        setColorFilter(if (item.isSelected) Color.WHITE else itemTextColor)
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
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (item.isSelected) Color.WHITE else itemTextColor)
                }
                addView(tv)

                setOnClickListener {
                    dismissAnimated {
                        item.onClick()
                    }
                }
            }
            container.addView(itemView)
        }

        cardLayout.addView(container)

        // Measure card dimensions
        cardLayout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val minWidth = (180 * density).toInt()
        val popupWidth = Math.max(cardLayout.measuredWidth, minWidth)
        val popupHeight = cardLayout.measuredHeight

        popupWindow = PopupWindow(
            cardLayout,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 24f * density
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

        cardLayout.pivotX = pivotX
        cardLayout.pivotY = pivotY
        cardLayout.scaleX = 0.2f
        cardLayout.scaleY = 0.2f
        cardLayout.alpha = 0f

        popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)

        // Card scale animation
        cardLayout.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Cascade children items animation
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = if (isAbove) (20f * density) else (-20f * density)
            child.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(200)
                .setStartDelay(i * 25L)
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

        val cardLayout = popup.contentView as? ViewGroup ?: run {
            popup.dismiss()
            onComplete?.invoke()
            return
        }

        val container = cardLayout.getChildAt(0) as? ViewGroup
        val density = context.resources.displayMetrics.density

        if (container != null) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.animate().cancel()
                child.animate()
                    .alpha(0f)
                    .translationY(-15f * density)
                    .setDuration(150)
                    .start()
            }
        }

        cardLayout.animate().cancel()
        cardLayout.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(180)
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
