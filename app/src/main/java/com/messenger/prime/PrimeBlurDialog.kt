package com.messenger.prime

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurView as EightBitBlurView

/**
 * Универсальный диалог в стиле Prime Glassmorphism BlurView.
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
                Color.parseColor("#400F172A")
            } else {
                Color.parseColor("#40154B87")
            }
            val windowBg = activity.window.decorView.background
            blurCard.setupBlur(rootView, 16f, overlayColor, windowBg)
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

        dialog.show()
        return dialog
    }
}
