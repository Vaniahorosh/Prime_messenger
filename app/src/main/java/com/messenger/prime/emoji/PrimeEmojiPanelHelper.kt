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
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.messenger.prime.PrimeNotification
import com.messenger.prime.R
import com.messenger.prime.setBlurAutoUpdateSafe

class PrimeEmojiPanelHelper(
    private val activity: ComponentActivity,
    private val panelContainer: View,
    private val composeView: ComposeView,
    private val btnEmojiToggle: ImageButton,
    private val etMessage: EditText,
    private val onSendSticker: (filePath: String, packId: String, packName: String) -> Unit,
    private val onPickStickerImage: (packId: String) -> Unit
) {
    interface PanelAnimationHost {
        fun isAnimationsDisabled(): Boolean
        fun getSystemBarsBottomInset(): Int
        fun getBottomMarginPx(): Int
        fun setBottomMarginPx(marginPx: Int)
        fun onPanelOpenAnimationStart(deltaPx: Int)
        fun onPanelOpenAnimationProgress(deltaPx: Int, progress: Float)
        fun onPanelOpenAnimationEnd()
        fun onPanelCloseAnimationStart(panelSpacePx: Int)
        fun onPanelCloseAnimationProgress(panelSpacePx: Int, progress: Float)
        fun onPanelCloseAnimationEnd()
        fun onPanelResized(newPanelHeightPx: Int, isFinished: Boolean)
        fun onHandoffToKeyboardCompleted(currentImeBottom: Int, removedPanelSpacePx: Int)
    }

    var animationHost: PanelAnimationHost? = null

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
    private var preDrawCleanup: (() -> Unit)? = null

    private val defaultHeightPx: Int by lazy {
        (320 * activity.resources.displayMetrics.density).toInt()
    }

    var currentPanelHeightPx: Int = 0
        private set
    private var exactPanelHeightPx: Float = 0f

    var lastImeHeightPx: Int = 0
        private set
    var isImeVisible: Boolean = false
        private set

    var isHandoffToKeyboardPending: Boolean = false
        private set

    private var isClosingWithoutKeyboard: Boolean = false

    private val handoffTimeoutRunnable = Runnable {
        if (isHandoffToKeyboardPending) {
            isHandoffToKeyboardPending = false
            // IME did not appear (e.g. hardware keyboard) — close cleanly
            if (panelContainer.visibility == View.VISIBLE) {
                startSmoothCloseWithoutKeyboard()
            }
        }
    }

    private var isComposeInitialized: Boolean = false

    init {
        currentPanelHeightPx = EmojiStickerSettings.getPanelHeightPx(activity, defaultHeightPx)
        exactPanelHeightPx = currentPanelHeightPx.toFloat()
        setupToggleButton()
        setupMessageInput()
        setupNetworkMonitoring()
        updateToggleState(isAvailable)
    }

    private fun ensureComposeViewSetup() {
        if (isComposeInitialized) return
        isComposeInitialized = true
        setupComposeView()
    }

    fun getPanelTopMarginPx(): Int {
        return (panelContainer.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
    }

    /**
     * Height currently occupied by the emoji panel (including its top margin) inside bottomContainer.
     * Returns 0 while closing without keyboard so message list padding transitions cleanly.
     */
    fun getOccupyingHeightWithMargin(): Int {
        if (isClosingWithoutKeyboard) return 0
        if ((isPanelOpen || isHandoffToKeyboardPending) && panelContainer.visibility == View.VISIBLE) {
            val lpH = panelContainer.layoutParams?.height ?: 0
            val h = when {
                panelContainer.height > 0 -> panelContainer.height
                lpH > 0 -> lpH
                currentPanelHeightPx > 0 -> currentPanelHeightPx
                else -> defaultHeightPx
            }
            return h + getPanelTopMarginPx()
        }
        return 0
    }

    fun onImeInsetsChanged(imeHeight: Int) {
        isImeVisible = imeHeight > 0
        if (imeHeight > 0) {
            lastImeHeightPx = imeHeight
        }
    }

    /**
     * Called when IME finishes opening (or immediately if no WindowInsetsAnimation ran)
     * during an emoji-panel -> keyboard handoff.
     */
    fun completeHandoffToKeyboard(currentImeBottom: Int) {
        if (!isHandoffToKeyboardPending) return
        panelContainer.removeCallbacks(handoffTimeoutRunnable)
        isHandoffToKeyboardPending = false
        isPanelOpen = false
        isClosingWithoutKeyboard = false
        onPanelStateChanged?.invoke(false)
        cancelActiveAnimator()
        btnEmojiToggle.setImageResource(R.drawable.ic_emoji)

        val removedSpace = ((panelContainer.height.takeIf { it > 0 }
            ?: panelContainer.layoutParams?.height?.takeIf { it > 0 }
            ?: currentPanelHeightPx) + getPanelTopMarginPx())

        panelContainer.visibility = View.GONE
        panelContainer.alpha = 1f
        panelContainer.translationY = 0f
        setBlurAutoUpdateSafe(panelContainer, false)

        val lp = panelContainer.layoutParams
        if (lp != null) {
            lp.height = if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx
            panelContainer.layoutParams = lp
        }

        animationHost?.onHandoffToKeyboardCompleted(currentImeBottom, removedSpace)
    }

    private fun cancelActiveAnimator() {
        preDrawCleanup?.invoke()
        preDrawCleanup = null
        currentAnimator?.cancel()
        currentAnimator = null
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
                onResizeFinished = {
                    onPanelResizeFinished()
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
        val minH = (180 * density)
        val maxH = (activity.resources.displayMetrics.heightPixels * 0.80f)
        if (exactPanelHeightPx <= 0f) {
            exactPanelHeightPx = currentPanelHeightPx.toFloat()
        }
        val requestedH = exactPanelHeightPx + deltaPx

        // If user pulls down significantly below minimum height (by > 70dp), smoothly close
        if (requestedH < minH - (70f * density)) {
            exactPanelHeightPx = currentPanelHeightPx.toFloat()
            closePanel()
            return
        }

        val clampedH = requestedH.coerceIn(minH, maxH)
        exactPanelHeightPx = clampedH
        val newH = clampedH.toInt()
        if (newH != currentPanelHeightPx) {
            currentPanelHeightPx = newH
            val lp = panelContainer.layoutParams
            if (lp != null) {
                lp.height = newH
                panelContainer.layoutParams = lp
            }
            animationHost?.onPanelResized(newH, false)
        }
    }

    private fun onPanelResizeFinished() {
        val finalH = currentPanelHeightPx
        if (finalH > 0) {
            EmojiStickerSettings.setPanelHeightPx(activity, finalH)
            animationHost?.onPanelResized(finalH, true)
        }
    }

    private fun setupToggleButton() {
        btnEmojiToggle.setOnClickListener {
            if (!isAvailable) {
                PrimeNotification.show(activity, "Эмодзи и стикеры недоступны без подключения к собеседнику")
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
        btnEmojiToggle.isClickable = true
        btnEmojiToggle.animate()
            .alpha(if (available) 1.0f else 0.38f)
            .setDuration(160)
            .start()
    }

    fun togglePanel() {
        if (isPanelOpen) {
            closePanel(showKeyboard = true)
        } else {
            openPanel()
        }
    }

    private fun getLiveImeBottomPx(): Int {
        val rootInsets = ViewCompat.getRootWindowInsets(panelContainer)
        if (rootInsets != null) {
            return rootInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        }
        return if (isImeVisible) lastImeHeightPx else 0
    }

    fun openPanel() {
        if (!isAvailable) {
            PrimeNotification.show(activity, "Эмодзи и стикеры недоступны без подключения к собеседнику")
            return
        }
        ensureComposeViewSetup()

        panelContainer.removeCallbacks(handoffTimeoutRunnable)
        cancelActiveAnimator()
        isHandoffToKeyboardPending = false
        isClosingWithoutKeyboard = false

        val density = activity.resources.displayMetrics.density
        val minH = (180 * density).toInt()
        val liveImeBottom = getLiveImeBottomPx()
        val wasKeyboardOpen = liveImeBottom > 0 || isImeVisible
        val effectiveIme = if (liveImeBottom > 0) liveImeBottom else lastImeHeightPx

        val targetHeight = (if (currentPanelHeightPx > 0) currentPanelHeightPx else (if (effectiveIme > 0) effectiveIme else defaultHeightPx))
            .coerceAtLeast(minH)
        currentPanelHeightPx = targetHeight
        exactPanelHeightPx = targetHeight.toFloat()

        val panelTopMargin = getPanelTopMarginPx()
        val targetPanelSpace = targetHeight + panelTopMargin

        isPanelOpen = true
        onPanelStateChanged?.invoke(true)
        btnEmojiToggle.setImageResource(R.drawable.ic_keyboard)

        if (wasKeyboardOpen && effectiveIme > 0) {
            val host = animationHost
            val sbBottom = host?.getSystemBarsBottomInset() ?: 0
            val preTotalBottom = effectiveIme.coerceAtLeast(host?.getBottomMarginPx() ?: sbBottom)

            val lp = panelContainer.layoutParams
            if (lp != null) {
                lp.height = targetHeight
                panelContainer.layoutParams = lp
            }
            panelContainer.visibility = View.VISIBLE
            panelContainer.alpha = 1.0f
            panelContainer.translationY = 0f
            setBlurAutoUpdateSafe(panelContainer, true)

            // Adjust bottomMargin immediately so IME + panel NEVER double-stack
            val initialBottomMargin = maxOf(sbBottom, effectiveIme - targetPanelSpace)
            host?.setBottomMarginPx(initialBottomMargin)

            val postTotalBottom = initialBottomMargin + targetPanelSpace
            val extraDelta = postTotalBottom - preTotalBottom

            if (extraDelta > 2 && host != null && !host.isAnimationsDisabled()) {
                host.onPanelOpenAnimationStart(extraDelta)
                val observer = panelContainer.doOnPreDraw {
                    preDrawCleanup = null
                    host.onPanelOpenAnimationProgress(extraDelta, 0f)
                    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 175
                        interpolator = this@PrimeEmojiPanelHelper.interpolator
                        var cancelled = false
                        addUpdateListener { va ->
                            host.onPanelOpenAnimationProgress(extraDelta, va.animatedFraction)
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationCancel(animation: Animator) {
                                cancelled = true
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                if (!cancelled) {
                                    currentAnimator = null
                                    host.onPanelOpenAnimationEnd()
                                }
                            }
                        })
                    }
                    currentAnimator = anim
                    anim.start()
                }
                preDrawCleanup = { observer.removeListener() }
            }

            imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
            etMessage.clearFocus()
        } else {
            // Smooth open from closed state (pure GPU translationY + message list sync)
            imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
            etMessage.clearFocus()

            val startPanelSpace = if (panelContainer.visibility == View.VISIBLE) {
                (panelContainer.height.takeIf { it > 0 } ?: targetHeight) + panelTopMargin
            } else {
                0
            }
            val delta = (targetPanelSpace - startPanelSpace).coerceAtLeast(0)
            val host = animationHost

            if (delta > 0 && host != null && !host.isAnimationsDisabled()) {
                host.onPanelOpenAnimationStart(delta)
            }

            val lp = panelContainer.layoutParams
            if (lp != null) {
                lp.height = targetHeight
                panelContainer.layoutParams = lp
            }
            panelContainer.visibility = View.VISIBLE
            panelContainer.translationY = 0f
            setBlurAutoUpdateSafe(panelContainer, true)

            if (delta > 0 && host != null && !host.isAnimationsDisabled()) {
                panelContainer.alpha = 0f
                val observer = panelContainer.doOnPreDraw {
                    preDrawCleanup = null
                    host.onPanelOpenAnimationProgress(delta, 0f)
                    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 185
                        interpolator = this@PrimeEmojiPanelHelper.interpolator
                        var cancelled = false
                        addUpdateListener { va ->
                            val p = va.animatedFraction
                            panelContainer.alpha = (p * 1.35f).coerceIn(0f, 1f)
                            host.onPanelOpenAnimationProgress(delta, p)
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationCancel(animation: Animator) {
                                cancelled = true
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                if (!cancelled) {
                                    currentAnimator = null
                                    panelContainer.alpha = 1f
                                    host.onPanelOpenAnimationEnd()
                                }
                            }
                        })
                    }
                    currentAnimator = anim
                    anim.start()
                }
                preDrawCleanup = { observer.removeListener() }
            } else {
                panelContainer.alpha = 1f
                host?.onPanelOpenAnimationEnd()
            }
        }
    }

    fun closePanel(showKeyboard: Boolean = false) {
        if (!isPanelOpen && panelContainer.visibility == View.GONE && !isClosingWithoutKeyboard) return

        panelContainer.removeCallbacks(handoffTimeoutRunnable)
        cancelActiveAnimator()

        isPanelOpen = false
        onPanelStateChanged?.invoke(false)
        btnEmojiToggle.setImageResource(R.drawable.ic_emoji)

        if (showKeyboard) {
            isClosingWithoutKeyboard = false
            isHandoffToKeyboardPending = true
            etMessage.requestFocus()
            imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT)
            panelContainer.postDelayed(handoffTimeoutRunnable, 450)
        } else {
            isHandoffToKeyboardPending = false
            startSmoothCloseWithoutKeyboard()
        }
    }

    private fun startSmoothCloseWithoutKeyboard() {
        val host = animationHost
        val currentH = panelContainer.height.takeIf { it > 0 }
            ?: panelContainer.layoutParams?.height?.takeIf { it > 0 }
            ?: currentPanelHeightPx.takeIf { it > 0 }
            ?: defaultHeightPx
        val panelSpace = currentH + getPanelTopMarginPx()

        if (host == null || host.isAnimationsDisabled() || panelSpace <= 0 || panelContainer.visibility != View.VISIBLE) {
            finishCloseWithoutKeyboard()
            return
        }

        isClosingWithoutKeyboard = true
        host.onPanelCloseAnimationStart(panelSpace)

        val observer = panelContainer.doOnPreDraw {
            preDrawCleanup = null
            host.onPanelCloseAnimationProgress(panelSpace, 0f)
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 165
                interpolator = this@PrimeEmojiPanelHelper.interpolator
                var cancelled = false
                addUpdateListener { va ->
                    val p = va.animatedFraction
                    panelContainer.alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
                    host.onPanelCloseAnimationProgress(panelSpace, p)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        cancelled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!cancelled) {
                            currentAnimator = null
                            finishCloseWithoutKeyboard()
                        }
                    }
                })
            }
            currentAnimator = anim
            anim.start()
        }
        preDrawCleanup = { observer.removeListener() }
    }

    private fun finishCloseWithoutKeyboard() {
        isClosingWithoutKeyboard = false
        panelContainer.visibility = View.GONE
        panelContainer.alpha = 1f
        panelContainer.translationY = 0f
        setBlurAutoUpdateSafe(panelContainer, false)
        val lp = panelContainer.layoutParams
        if (lp != null) {
            lp.height = if (currentPanelHeightPx > 0) currentPanelHeightPx else defaultHeightPx
            panelContainer.layoutParams = lp
        }
        animationHost?.onPanelCloseAnimationEnd()
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
            panelContainer.removeCallbacks(handoffTimeoutRunnable)
            cancelActiveAnimator()
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }
}
