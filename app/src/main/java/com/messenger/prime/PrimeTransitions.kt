package com.messenger.prime

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.Color
import android.os.Build
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

    /**
     * Applies the Prime Activity CLOSE transition when finishing an Activity.
     * On API 34+, registers the CLOSE transition.
     * On API < 34, falls back to `overridePendingTransition`.
     */
    @JvmStatic
    fun applyCloseTransition(activity: Activity) {
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
     * Attaches Slidr swipe-to-dismiss gesture to the activity across all API levels.
     * Dynamically converts the Activity to translucent while dragging so the underlying Activity
     * is rendered during the swipe gesture, while preserving non-translucent window status for
     * Android 14+ native Predictive Back gestures.
     */
    @JvmStatic
    @JvmOverloads
    fun attachSlidr(activity: Activity, customListener: SlidrListener? = null): SlidrInterface {
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .scrimColor(Color.BLACK)
            .scrimStartAlpha(0.7f)
            .scrimEndAlpha(0.0f)
            .velocityThreshold(2400f)
            .distanceThreshold(0.25f)
            .listener(object : SlidrListener {
                override fun onSlideStateChanged(state: Int) {
                    // state 1 = DRAGGING, 0 = IDLE, 2 = SETTLING
                    if (state == 1) {
                        convertToTranslucent(activity)
                    } else if (state == 0) {
                        convertFromTranslucent(activity)
                    }
                    customListener?.onSlideStateChanged(state)
                }

                override fun onSlideChange(percent: Float) {
                    customListener?.onSlideChange(percent)
                }

                override fun onSlideOpened() {
                    customListener?.onSlideOpened()
                }

                override fun onSlideClosed(): Boolean {
                    val customHandled = customListener?.onSlideClosed() ?: false
                    if (!customHandled) {
                        applyCloseTransition(activity)
                    }
                    return customHandled
                }
            })
            .build()
        return Slidr.attach(activity, slidrConfig)
    }

    /**
     * Dynamically converts an Activity window to translucent so the activity behind it is rendered.
     */
    @JvmStatic
    fun convertToTranslucent(activity: Activity) {
        try {
            val method = Activity::class.java.getDeclaredMethod(
                "convertToTranslucent",
                Class.forName("android.app.Activity\$TranslucentConversionListener"),
                ActivityOptions::class.java
            )
            method.isAccessible = true
            method.invoke(activity, null, null)
        } catch (_: Exception) {}
    }

    /**
     * Restores an Activity window to non-translucent status.
     */
    @JvmStatic
    fun convertFromTranslucent(activity: Activity) {
        try {
            val method = Activity::class.java.getDeclaredMethod("convertFromTranslucent")
            method.isAccessible = true
            method.invoke(activity)
        } catch (_: Exception) {}
    }
}
