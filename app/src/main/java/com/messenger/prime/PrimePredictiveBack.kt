package com.messenger.prime

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.window.embedding.ActivityEmbeddingController

/**
 * Prime Native Predictive Back Gesture Controller.
 * Implements native Android 13+ / 14+ Predictive Back gestures with interactive 60/120fps
 * scaling, rounded corner clipping, elevation depth shadow, and tablet Activity Embedding (split layout) awareness.
 */
object PrimePredictiveBack {

    private val emphasizedInterpolator = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    private val springReleaseInterpolator = PathInterpolator(0.175f, 0.885f, 0.32f, 1.275f)

    /**
     * Attaches interactive Predictive Back gesture handling to the specified [activity].
     *
     * @param activity Target ComponentActivity (or AppCompatActivity).
     * @param targetView Custom View to animate during back swipe gesture (defaults to android.R.id.content).
     * @param onBackCompleted Custom action executed on back gesture completion (defaults to finishWithTransition).
     */
    @JvmStatic
    @JvmOverloads
    fun attach(
        activity: ComponentActivity,
        targetView: View? = null,
        onBackCompleted: (() -> Unit)? = null
    ) {
        val rootContent: View = targetView
            ?: activity.findViewById(android.R.id.content)
            ?: activity.window.decorView

        var isExecutingBack = false
        val density = activity.resources.displayMetrics.density
        val maxCornerRadiusPx = 28f * density
        val maxShiftPx = 32f * density
        val maxElevationPx = 16f * density

        val isTablet = activity.resources.configuration.smallestScreenWidthDp >= 600

        val callback = object : OnBackPressedCallback(true) {

            private var originalOutlineProvider: ViewOutlineProvider? = null
            private var originalClipToOutline: Boolean = false
            private var currentCornerRadiusPx: Float = 0f

            private fun isMultiPaneLayout(): Boolean {
                val isEmbedded = try {
                    ActivityEmbeddingController.getInstance(activity).isActivityEmbedded(activity)
                } catch (_: Exception) {
                    false
                }
                return isEmbedded || (isTablet && activity !is ChatPersonActivity && activity !is SettingsActivity && activity !is PersonInformationActivity)
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                if (isExecutingBack) return

                // Skip full-screen displacement when activity is running in a multi-pane split layout
                if (isMultiPaneLayout()) {
                    return
                }

                // Enable hardware layer for maximum rendering performance
                rootContent.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // Save original view outline settings
                originalOutlineProvider = rootContent.outlineProvider
                originalClipToOutline = rootContent.clipToOutline

                // Setup dynamic rounded corner outline provider
                rootContent.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height,
                            currentCornerRadiusPx
                        )
                    }
                }
                rootContent.clipToOutline = true
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (isExecutingBack) return

                if (isMultiPaneLayout()) {
                    return
                }

                val rawProgress = backEvent.progress
                val progress = emphasizedInterpolator.getInterpolation(rawProgress)
                val isLeftEdge = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT

                // 1. Interactive Scale (from 1.0 down to 0.95)
                val scale = 1.0f - (progress * 0.05f)
                rootContent.scaleX = scale
                rootContent.scaleY = scale

                // 2. Interactive Translation in swipe direction
                val shiftDirection = if (isLeftEdge) 1f else -1f
                rootContent.translationX = shiftDirection * (progress * maxShiftPx)

                // 3. Interactive Corner Rounding (0dp -> 28dp)
                currentCornerRadiusPx = progress * maxCornerRadiusPx
                rootContent.invalidateOutline()

                // 4. Interactive Elevation Depth Shadow
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rootContent.outlineAmbientShadowColor = 0x40000000
                    rootContent.outlineSpotShadowColor = 0x60000000
                }
                rootContent.translationZ = progress * maxElevationPx
            }

            override fun handleOnBackPressed() {
                if (isExecutingBack) return
                isExecutingBack = true

                // Cleanly reset local interactive transforms before kicking off Window exit transition
                resetViewProperties()

                if (onBackCompleted != null) {
                    onBackCompleted.invoke()
                } else {
                    PrimeTransitions.finishWithTransition(activity)
                }

                rootContent.postDelayed({
                    isExecutingBack = false
                }, 350)
            }

            override fun handleOnBackCancelled() {
                if (isExecutingBack) return

                // Spring back smoothly to original state
                rootContent.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationX(0f)
                    .translationZ(0f)
                    .setDuration(240)
                    .setInterpolator(springReleaseInterpolator)
                    .setUpdateListener {
                        val currentScale = rootContent.scaleX
                        val currentProgress = ((1.0f - currentScale) / 0.05f).coerceIn(0f, 1f)
                        currentCornerRadiusPx = currentProgress * maxCornerRadiusPx
                        rootContent.invalidateOutline()
                    }
                    .withEndAction {
                        resetViewProperties()
                    }
                    .start()
            }

            private fun resetViewProperties() {
                rootContent.scaleX = 1.0f
                rootContent.scaleY = 1.0f
                rootContent.translationX = 0f
                rootContent.translationZ = 0f
                currentCornerRadiusPx = 0f
                rootContent.outlineProvider = originalOutlineProvider
                rootContent.clipToOutline = originalClipToOutline
                rootContent.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        }

        activity.onBackPressedDispatcher.addCallback(activity, callback)
    }
}
