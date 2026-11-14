package com.messenger.prime.emoji

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.messenger.prime.R

class PrimeEmojiPanelHelper(
    private val activity: ComponentActivity,
    private val panelContainer: View,
    private val composeView: ComposeView,
    private val btnEmojiToggle: ImageButton,
    private val etMessage: EditText,
    private val onSendSticker: (filePath: String, packId: String, packName: String) -> Unit,
    private val onPickStickerImage: (packId: String) -> Unit
) {
    var isPanelOpen: Boolean = false
        private set

    var onPanelStateChanged: ((isOpen: Boolean) -> Unit)? = null

    private var isConnectionAvailable: Boolean = false
    private var isNetworkConnected: Boolean = true

    val isAvailable: Boolean
        get() = isConnectionAvailable && isNetworkConnected

    private val imm: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private val connectivityManager: ConnectivityManager =
        activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val interpolator = FastOutSlowInInterpolator()
    private var currentAnimator: ValueAnimator? = null

    private val defaultHeightPx: Int by lazy {
        (320 * activity.resources.displayMetrics.density).toInt()
    }

    var currentPanelHeightPx: Int = 0
    var lastImeHeightPx: Int = 0
        private set

    init {
        currentPanelHeightPx = EmojiStickerSettings.getPanelHeightPx(activity, defaultHeightPx)
        setupComposeView()
        setupToggleButton()
        setupMessageInput()
        setupNetworkMonitoring()
        updateToggleState(isAvailable)
    }

    fun onImeInsetsChanged(imeHeight: Int) {
        if (imeHeight > 0) {
            lastImeHeightPx = imeHeight
            // If user opens keyboard while panel is open, silently hide panel without bounce
            if (isPanelOpen) {
                isPanelOpen = false
                onPanelStateChanged?.invoke(false)
                currentAnimator?.cancel()
                btnEmojiToggle.setImageResource(R.drawable.ic_emoji)
                panelContainer.visibility = View.GONE
                panelContainer.alpha = 0f
                panelContainer.translationY = 0f
            }
        }
    }

    fun deleteLastChar() {
        val selectionStart = etMessage.selectionStart
        val selectionEnd = etMessage.selectionEnd
        if (selectionStart != selectionEnd) {
            val min = minOf(selectionStart, selectionEnd)
            val max = maxOf(selectionStart, selectionEnd)
            etMessage.text?.delete(min, max)
        } else if (selectionStart > 0) {
            val text = etMessage.text ?: return
            val count = if (selectionStart >= 2 && Character.isSurrogatePair(text[selectionStart - 2], text[selectionStart - 1])) {
                2
            } else {
                1
            }
            text.delete(selectionStart - count, selectionStart)
        }
    }

    private fun setupComposeView() {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        composeView.setContent {
            PrimeEmojiStickerPanel(
                onEmojiSelected = { emoji ->
                    insertEmojiAtCursor(emoji)
                },
                onStickerSelected = { stickerPath, packId, packName ->
                    onSendSticker(stickerPath, packId, packName)
                },
                onAddStickerRequested = { packId ->
                    onPickStickerImage(packId)
                },
                onCloseRequested = {
                    closePanel()
                },
                onResizeRequested = { deltaPx ->
                    onPanelResize(deltaPx)
                },
                onDeleteBackwardRequested = {
                    deleteLastChar()
                }
            )
        }
    }

    fun setConnectionAvailable(available: Boolean) {
        isConnectionAvailable = available
        activity.runOnUiThread {
            updateToggleState(isAvailable)
            if (!isAvailable && isPanelOpen) {
                closePanel(false)
            }
        }
    }

    fun onPanelResize(deltaPx: Float) {
        val density = activity.resources.displayMetrics.density
        val minH = (180 * density).toInt()
        val maxH = (activity.resources.displayMetrics.heightPixels * 0.80f).toInt()
        val requestedH = currentPanelHeightPx + deltaPx.toInt()

        // If user pulls down significantly below minimum height (by > 70dp), smoothly close
        if (requestedH < minH - (70 * density)) {
            closePanel()
            return
        }

        val newH = requestedH.coerceIn(minH, maxH)
        currentPanelHeightPx = newH
        val lp = panelContainer.layoutParams
        if (lp != null) {
            lp.height = newH
            panelContainer.layoutParams = lp
        }
        EmojiStickerSettings.setPanelHeightPx(activity, newH)
    }

    private fun setupToggleButton() {
        btnEmojiToggle.setOnClickListener {
            if (!isAvailable) {
                Toast.makeText(
                    activity,
                    "Эмодзи и стикеры недоступны без подключения к собеседнику",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            togglePanel()
        }
    }

    private fun setupMessageInput() {
        etMessage.setOnClickListener {
            if (isPanelOpen) {
                closePanel(showKeyboard = true)
            }
        }
        etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isPanelOpen) {
                closePanel(showKeyboard = true)
            }
        }
    }

    private fun setupNetworkMonitoring() {
        val currentNet = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNet)
        isNetworkConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activity.runOnUiThread {
                    isNetworkConnected = true
                    updateToggleState(isAvailable)
                }
            }

            override fun onLost(network: Network) {
                activity.runOnUiThread {
                    isNetworkConnected = false
                    updateToggleState(false)
                    if (isPanelOpen) {
                        closePanel()
                    }
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun updateToggleState(available: Boolean) {
        btnEmojiToggle.isEnabled = available
        btnEmojiToggle.isClickable = true // Keep clickable to show toast if offline
        btnEmojiToggle.animate()
            .alpha(if (available) 1.0f else 0.38f)
            .setDuration(180)
            .start()
    }

    fun togglePanel() {
        if (isPanelOpen) {
            closePanel(showKeyboard = true)
        } else {
            openPanel()
        }
    }

    fun openPanel() {
        if (!isAvailable) {
            Toast.makeText(
                activity,
                "Эмодзи и стикеры недоступны без подключения к собеседнику",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val wasKeyboardOpen = lastImeHeightPx > 0 && etMessage.hasFocus()
        val targetHeight = if (currentPanelHeightPx > 0) currentPanelHeightPx else (if (lastImeHeightPx > 0) lastImeHeightPx else defaultHeightPx)
        val density = activity.resources.displayMetrics.density

        isPanelOpen = true
        onPanelStateChanged?.invoke(true)
        currentAnimator?.cancel()

        if (wasKeyboardOpen) {
            // Smooth seamless keyboard-to-panel handoff:
            // Set panel height to last known keyboard height so the input bar does not drop down!
            val initialH = if (lastImeHeightPx > 0) lastImeHeightPx else targetHeight
            val lp = panelContainer.layoutParams
            if (lp != null) {
                lp.height = initialH
                panelContainer.layoutParams = lp
            }
            panelContainer.visibility = View.VISIBLE
            panelContainer.alpha = 1.0f
            panelContainer.translationY = 0f

            imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
            etMessage.clearFocus()

            // If target height differs from keyboard height, smoothly adjust
            if (initialH != targetHeight) {
                val anim = ValueAnimator.ofInt(initialH, targetHeight).apply {
                    duration = 200
                    interpolator = this@PrimeEmojiPanelHelper.interpolator
                    addUpdateListener { va ->
                        val h = va.animatedValue as Int
                        val pLp = panelContainer.layoutParams
                        if (pLp != null) {
                            pLp.height = h
                            panelContainer.layoutParams = pLp
                        }
                    }
                }
                currentAnimator = anim
                anim.start()
            }
        } else {
            // Soft open when keyboard wasn't open
            imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
            etMessage.clearFocus()

            val startHeight = if (panelContainer.height > 0 && panelContainer.visibility == View.VISIBLE) {
                panelContainer.height
            } else {
                0
            }

            panelContainer.visibility = View.VISIBLE
            panelContainer.alpha = 0f
            panelContainer.translationY = 16f * density

            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 240
                interpolator = this@PrimeEmojiPanelHelper.interpolator
                addUpdateListener { va ->
                    val progress = va.animatedFraction
                    val h = (startHeight + (targetHeight - startHeight) * progress).toInt()
                    val lp = panelContainer.layoutParams
                    if (lp != null) {
                        lp.height = h.coerceAtLeast(1)
                        panelContainer.layoutParams = lp
                    }
                    panelContainer.alpha = progress
                    panelContainer.translationY = (1f - progress) * (16f * density)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val lp = panelContainer.layoutParams
                        if (lp != null) {
                            lp.height = targetHeight
                            panelContainer.layoutParams = lp
                        }
                        panelContainer.alpha = 1.0f
                        panelContainer.translationY = 0f
                    }
                })
            }
            currentAnimator = anim
            anim.start()
        }

        btnEmojiToggle.setImageResource(R.drawable.ic_keyboard)
    }

    fun closePanel(showKeyboard: Boolean = false) {
        if (!isPanelOpen && panelContainer.visibility == View.GONE) return

        isPanelOpen = false
        onPanelStateChanged?.invoke(false)
        currentAnimator?.cancel()

        btnEmojiToggle.setImageResource(R.drawable.ic_emoji)

        if (showKeyboard) {
            etMessage.requestFocus()
            imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT)

            val startHeight = if (panelContainer.height > 0) panelContainer.height else (if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx)
            val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 180
                interpolator = this@PrimeEmojiPanelHelper.interpolator
                addUpdateListener { va ->
                    val progress = 1f - va.animatedFraction
                    val h = (startHeight * progress).toInt()
                    val lp = panelContainer.layoutParams
                    if (lp != null) {
                        lp.height = h.coerceAtLeast(0)
                        panelContainer.layoutParams = lp
                    }
                    panelContainer.alpha = progress
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        panelContainer.visibility = View.GONE
                        panelContainer.alpha = 0f
                        val lp = panelContainer.layoutParams
                        if (lp != null) {
                            lp.height = if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx
                            panelContainer.layoutParams = lp
                        }
                    }
                })
            }
            currentAnimator = anim
            anim.start()
        } else {
            val startHeight = if (panelContainer.height > 0) panelContainer.height else (if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx)
            val density = activity.resources.displayMetrics.density

            val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 200
                interpolator = this@PrimeEmojiPanelHelper.interpolator
                addUpdateListener { va ->
                    val progress = va.animatedFraction
                    val currentProgress = 1f - progress
                    val h = (startHeight * currentProgress).toInt()
                    val lp = panelContainer.layoutParams
                    if (lp != null) {
                        lp.height = h.coerceAtLeast(0)
                        panelContainer.layoutParams = lp
                    }
                    panelContainer.alpha = currentProgress
                    panelContainer.translationY = (1f - currentProgress) * (16f * density)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        panelContainer.visibility = View.GONE
                        panelContainer.alpha = 0f
                        panelContainer.translationY = 0f
                        val lp = panelContainer.layoutParams
                        if (lp != null) {
                            lp.height = if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx
                            panelContainer.layoutParams = lp
                        }
                    }
                })
            }
            currentAnimator = anim
            anim.start()
        }
    }

    private fun insertEmojiAtCursor(emoji: String) {
        val start = etMessage.selectionStart.coerceAtLeast(0)
        val end = etMessage.selectionEnd.coerceAtLeast(0)
        val min = minOf(start, end)
        val max = maxOf(start, end)

        etMessage.text?.replace(min, max, emoji)
        etMessage.setSelection(min + emoji.length)
    }

    fun onDestroy() {
        try {
            currentAnimator?.cancel()
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }
}
