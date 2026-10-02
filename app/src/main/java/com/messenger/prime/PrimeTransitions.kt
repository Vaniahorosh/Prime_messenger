package com.messenger.prime

import android.app.Activity
import android.content.Intent
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
     * Helper method to start an Activity with transition and notify background lava animation state.
     */
    @JvmStatic
    fun startActivityWithTransition(activity: Activity, intent: Intent) {
        LavaBackgroundState.onTransitionStart()
        activity.startActivity(intent)
        applyOpenTransition(activity)
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
}
