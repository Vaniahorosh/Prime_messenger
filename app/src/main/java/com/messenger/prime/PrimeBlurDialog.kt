package com.messenger.prime

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurView as EightBitBlurView

/**
 * Единый универсальный инновационный диалог в стиле Prime Glassmorphism BlurView с плавной анимацией появления.
 */
object PrimeBlurDialog {

    @JvmStatic
    @JvmOverloads
    fun show(
        activity: Activity,
        title: String,
        message: String? = null,
        positiveText: String = "Да",
        negativeText: String? = "Нет",
        iconRes: Int? = null,
        isPositiveDanger: Boolean = false,
        customView: View? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ): AlertDialog {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_blur_generic, null)
        val blurCard = dialogView.findViewById<EightBitBlurView>(R.id.blurDialogCard)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val customContainer = dialogView.findViewById<FrameLayout>(R.id.dialogCustomViewContainer)
        val btnPositive = dialogView.findViewById<MaterialButton>(R.id.btnDialogPositive)
        val btnNegative = dialogView.findViewById<MaterialButton>(R.id.btnDialogNegative)

        if (blurCard != null) {
            val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                ?: activity.window.decorView as ViewGroup
            val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) {
                Color.parseColor("#450F172A")
            } else {
                Color.parseColor("#45154B87")
            }
            val windowBg = activity.window.decorView.background
            blurCard.setupBlur(rootView, 20f, overlayColor, windowBg)
        }

        tvTitle.text = title

        if (!message.isNullOrEmpty()) {
            tvMessage.text = message
            tvMessage.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
        }

        if (iconRes != null) {
            ivIcon.setImageResource(iconRes)
            ivIcon.visibility = View.VISIBLE
        } else {
            ivIcon.visibility = View.GONE
        }

        if (customView != null) {
            customContainer.removeAllViews()
            customContainer.addView(customView)
            customContainer.visibility = View.VISIBLE
        } else {
            customContainer.visibility = View.GONE
        }

        btnPositive.text = positiveText
        if (isPositiveDanger) {
            btnPositive.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.prime_danger))
            btnPositive.setTextColor(Color.WHITE)
        } else {
            val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
            if (isDark) {
                btnPositive.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                btnPositive.setTextColor(accentColor)
            } else {
                btnPositive.backgroundTintList = ColorStateList.valueOf(accentColor)
                btnPositive.setTextColor(Color.WHITE)
            }
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialog.window != null) {
            try {
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val params = dialog.window?.attributes
                if (params != null) {
                    params.blurBehindRadius = 60
                    dialog.window?.attributes = params
                }
            } catch (_: Throwable) {}
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            onPositive?.invoke()
        }

        if (negativeText != null) {
            btnNegative.text = negativeText
            btnNegative.visibility = View.VISIBLE
            btnNegative.setOnClickListener {
                dialog.dismiss()
                onNegative?.invoke()
            }
        } else {
            btnNegative.visibility = View.GONE
        }

        // Инновационная пружинная анимация появления BlurCard
        if (blurCard != null) {
            blurCard.alpha = 0f
            blurCard.scaleX = 0.82f
            blurCard.scaleY = 0.82f
            blurCard.translationY = 60f

            blurCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(320L)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

        dialog.show()
        return dialog
    }

    @JvmStatic
    @JvmOverloads
    fun showList(
        activity: Activity,
        title: String,
        items: Array<String>,
        iconRes: Int? = null,
        onItemSelected: ((Int, String) -> Unit)? = null
    ): AlertDialog {
        val listView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        var alertDialog: AlertDialog? = null

        items.forEachIndexed { index, itemText ->
            val itemView = TextView(activity).apply {
                text = itemText
                setTextColor(Color.WHITE)
                textSize = 15f
                setPadding(40, 30, 40, 30)
                gravity = Gravity.CENTER_VERTICAL
                
                val glassBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 32f
                    setColor(Color.parseColor("#1FFFFFFF"))
                    setStroke(1, Color.parseColor("#33FFFFFF"))
                }
                background = glassBg
                
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
                layoutParams = lp

                setOnClickListener {
                    alertDialog?.dismiss()
                    onItemSelected?.invoke(index, itemText)
                }
            }
            listView.addView(itemView)
        }

        alertDialog = show(
            activity = activity,
            title = title,
            customView = listView,
            positiveText = "Отмена",
            negativeText = null,
            iconRes = iconRes,
            onPositive = null
        )

        return alertDialog
    }
}
