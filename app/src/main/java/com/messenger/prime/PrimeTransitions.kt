package com.messenger.prime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.app.ActivityOptionsCompat
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrInterface
import com.r0adkll.slidr.model.SlidrListener
import com.r0adkll.slidr.model.SlidrPosition

/**
 * Centralized utility object providing Prime Activity transition animations
 * with full support for Android 14+ Predictive Back Gestures (API 34+),
 * dynamic Slidr swipe-to-dismiss translucency conversion, and backward compatibility.
 */
object PrimeTransitions {

    /**
     * Registers both OPEN and CLOSE activity transitions upfront on Android 14+ (API 34+).
     * Registering CLOSE transition upfront in `onCreate` guarantees the system
     * predictive gesture engine can seek and animate the closing transition interactively.
     */
    @JvmStatic
    fun setupActivityTransitions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.prime_open_enter,
                R.anim.prime_open_exit
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.prime_close_enter,
                R.anim.prime_close_exit
            )
        }
    }

    /**
     * Applies the Prime Activity OPEN transition when launching a new Activity.
     * On API 34+, registers both OPEN and proactive CLOSE transitions upfront.
     * On API < 34, falls back to `overridePendingTransition`.
     */
    @JvmStatic
    fun applyOpenTransition(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.prime_open_enter,
                R.anim.prime_open_exit
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.prime_close_enter,
                R.anim.prime_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.prime_open_enter, R.anim.prime_open_exit)
        }
    }

    @Volatile
    private var isSlidrDismissing: Boolean = false

    /**
     * Applies the Prime Activity CLOSE transition when finishing an Activity.
     * On API 34+, registers the CLOSE transition.
     * On API < 34, falls back to `overridePendingTransition`.
     */
    @JvmStatic
    fun applyCloseTransition(activity: Activity) {
        if (isSlidrDismissing) {
            // Skip playing close transition animation if activity was already swiped off screen by Slidr
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.prime_close_enter,
                R.anim.prime_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.prime_close_enter, R.anim.prime_close_exit)
        }
    }

    /**
     * Applies an exclusive horizontal page-flip / book-page slide transition when navigating between
     * Bottom Navigation tabs. Creates a seamless page-turning feel instead of standard overlapping activities.
     */
    @JvmStatic
    @JvmOverloads
    fun applyPageFlipTransition(activity: Activity, isForward: Boolean = true) {
        val enterAnim = if (isForward) R.anim.slide_in_right else R.anim.slide_in_left
        val exitAnim = if (isForward) R.anim.slide_out_left else R.anim.slide_out_right

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                enterAnim,
                exitAnim
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                enterAnim,
                exitAnim
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    /**
     * Helper method to start an Activity with transition and notify background lava animation state.
     * Uses [ActivityOptionsCompat] for seamless window animation preparation across all API levels.
     */
    @JvmStatic
    fun startActivityWithTransition(activity: Activity, intent: Intent) {
        LavaBackgroundState.onTransitionStart()
        val options = ActivityOptionsCompat.makeCustomAnimation(
            activity,
            R.anim.prime_open_enter,
            R.anim.prime_open_exit
        )
        activity.startActivity(intent, options.toBundle())
        applyOpenTransition(activity)
    }

    /**
     * Helper method to start an Activity with custom enter/exit transition animations.
     */
    @JvmStatic
    fun startActivityWithCustomTransition(
        activity: Activity,
        intent: Intent,
        enterAnim: Int,
        exitAnim: Int
    ) {
        LavaBackgroundState.onTransitionStart()
        val options = ActivityOptionsCompat.makeCustomAnimation(
            activity,
            enterAnim,
            exitAnim
        )
        activity.startActivity(intent, options.toBundle())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                enterAnim,
                exitAnim
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    /**
     * Helper method to finish an Activity with transition.
     */
    @JvmStatic
    fun finishWithTransition(activity: Activity) {
        LavaBackgroundState.onTransitionStart()
        activity.finish()
        applyCloseTransition(activity)
    }

    /**
     * Attaches Slidr swipe-to-dismiss gesture to the activity across all API levels.
     * Dynamically converts the Activity to translucent while dragging so the underlying Activity
     * is rendered during the swipe gesture, with Material 3 interactive scaling, corner rounding,
     * and depth elevation shadow.
     */
    @JvmStatic
    @JvmOverloads
    fun attachSlidr(activity: Activity, customListener: SlidrListener? = null): SlidrInterface? {
        val config = activity.resources.configuration
        val isTablet = config.screenWidthDp >= 480 || config.smallestScreenWidthDp >= 480

        if (isTablet) {
            activity.window.setBackgroundDrawableResource(R.color.prime_base)
            return null
        }

        val rootContent: View = activity.findViewById(android.R.id.content)
            ?: activity.window.decorView

        val density = activity.resources.displayMetrics.density
        val maxCornerRadiusPx = 28f * density
        val maxElevationPx = 16f * density

        var originalOutlineProvider: ViewOutlineProvider? = null
        var originalClipToOutline = false
        var isDragging = false

        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .scrimColor(Color.BLACK)
            .scrimStartAlpha(0.65f)
            .scrimEndAlpha(0.0f)
            .velocityThreshold(1200f)
            .distanceThreshold(0.20f)
            .listener(object : SlidrListener {
                override fun onSlideStateChanged(state: Int) {
                    if (state == 0 && isDragging) { // IDLE state
                        isDragging = false
                        resetViewProperties(rootContent, originalOutlineProvider, originalClipToOutline)
                    }
                    customListener?.onSlideStateChanged(state)
                }

                override fun onSlideChange(percent: Float) {
                    val clampedPercent = percent.coerceIn(0f, 1f)

                    if (clampedPercent > 0f && !isDragging) {
                        isDragging = true
                        rootContent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        originalOutlineProvider = rootContent.outlineProvider
                        originalClipToOutline = rootContent.clipToOutline
                        rootContent.clipToOutline = true
                    } else if (clampedPercent == 0f && isDragging) {
                        isDragging = false
                        resetViewProperties(rootContent, originalOutlineProvider, originalClipToOutline)
                    }

                    if (isDragging) {
                        // 1. Interactive scale-down during hand swipe (1.0 -> 0.96)
                        val scale = 1.0f - (clampedPercent * 0.04f)
                        rootContent.scaleX = scale
                        rootContent.scaleY = scale

                        // 2. Dynamic rounded corner clipping during swipe (0dp -> 28dp)
                        val cornerRadius = clampedPercent * maxCornerRadiusPx
                        rootContent.outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                            }
                        }
                        rootContent.invalidateOutline()

                        // 3. Elevation depth shadow over background activity
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            rootContent.outlineAmbientShadowColor = 0x40000000
                            rootContent.outlineSpotShadowColor = 0x60000000
                        }
                        rootContent.translationZ = clampedPercent * maxElevationPx
                    }

                    customListener?.onSlideChange(percent)
                }

                override fun onSlideOpened() {
                    isDragging = false
                    resetViewProperties(rootContent, originalOutlineProvider, originalClipToOutline)
                    customListener?.onSlideOpened()
                }

                override fun onSlideClosed(): Boolean {
                    isSlidrDismissing = true
                    isDragging = false
                    resetViewProperties(rootContent, originalOutlineProvider, originalClipToOutline)
                    val customHandled = customListener?.onSlideClosed() ?: false
                    if (!customHandled) {
                        activity.finish()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            activity.overrideActivityTransition(
                                Activity.OVERRIDE_TRANSITION_CLOSE,
                                0,
                                0
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            activity.overridePendingTransition(0, 0)
                        }
                    }
                    activity.window.decorView.post { isSlidrDismissing = false }
                    return customHandled
                }
            })
            .build()

        return Slidr.attach(activity, slidrConfig)
    }

    private fun resetViewProperties(
        view: View,
        originalOutlineProvider: ViewOutlineProvider?,
        originalClipToOutline: Boolean
    ) {
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.translationZ = 0f
        view.outlineProvider = originalOutlineProvider
        view.clipToOutline = originalClipToOutline
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    }
}