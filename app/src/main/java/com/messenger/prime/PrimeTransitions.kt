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
    fun attachSlidr(activity: Activity, customListener: SlidrListener? = null): SlidrInterface? {
        val config = activity.resources.configuration
        // Material Design 3 window size classes: 600dp+ is Medium/Expanded (multi-pane possible)
        val isMultiPane = config.screenWidthDp >= 600 || config.smallestScreenWidthDp >= 600

        if (isMultiPane) {
            activity.window.setBackgroundDrawableResource(R.color.prime_base)
            return null
        }

        // On Android 14+ with Predictive Back, edge swipes are consumed by the system gesture.
        // We intercept the predictive back callback to implement a real-time, interactive slide
        // that strictly follows the finger, mimicking iOS swipe-to-dismiss.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity is androidx.activity.ComponentActivity) {
            val backCallback = object : androidx.activity.OnBackPressedCallback(true) {
                var isLeftEdge = false
                var screenWidth = 0f
                var contentView: android.view.View? = null
                var prevContentView: android.view.View? = null

                override fun handleOnBackStarted(backEvent: androidx.activity.BackEventCompat) {
                    isLeftEdge = backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_LEFT
                    screenWidth = activity.resources.displayMetrics.widthPixels.toFloat()
                    contentView = activity.findViewById(android.R.id.content)

                    val prevAct = PrimeApplication.getPreviousActivity()
                    prevContentView = prevAct?.findViewById(android.R.id.content)

                    // Make window fully transparent during interactive swipe so previous activity is visible
                    activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    customListener?.onSlideOpened()
                }

                override fun handleOnBackProgressed(backEvent: androidx.activity.BackEventCompat) {
                    val content = contentView ?: return
                    if (isLeftEdge) {
                        // The user is swiping from the left edge.
                        // Translate the entire content exactly following the finger X coordinate.
                        content.translationX = backEvent.touchX

                        // Calculate progress
                        val progress = (backEvent.touchX / screenWidth).coerceIn(0f, 1f)

                        // Apply interactive dark scrim to the decor view background
                        val alpha = (0.55f * (1f - progress) * 255).toInt()
                        activity.window.decorView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))

                        // Interactive depth reveal for previous activity
                        prevContentView?.let {
                            val scale = 0.96f + (0.04f * progress)
                            it.scaleX = scale
                            it.scaleY = scale
                        }

                        customListener?.onSlideChange(progress)
                    } else {
                        // For the right edge, fallback to a subtle scale down (similar to system default)
                        val scale = 1f - (0.05f * backEvent.progress)
                        content.scaleX = scale
                        content.scaleY = scale
                    }
                }

                override fun handleOnBackCancelled() {
                    val content = contentView ?: return
                    // Gesture cancelled, animate back to normal smoothly
                    content.animate()
                        .translationX(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250)
                        .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                        .setUpdateListener {
                            if (isLeftEdge) {
                                val progress = (content.translationX / screenWidth).coerceIn(0f, 1f)
                                val alpha = (0.55f * (1f - progress) * 255).toInt()
                                activity.window.decorView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))

                                prevContentView?.let { prev ->
                                    val scale = 0.96f + (0.04f * progress)
                                    prev.scaleX = scale
                                    prev.scaleY = scale
                                }
                            }
                        }
                        .withEndAction {
                            activity.window.decorView.setBackgroundColor(Color.TRANSPARENT)
                            prevContentView?.scaleX = 1f
                            prevContentView?.scaleY = 1f
                        }
                        .start()
                }

                override fun handleOnBackPressed() {
                    val content = contentView ?: return
                    if (isLeftEdge) {
                        isSlidrDismissing = true
                        customListener?.onSlideClosed()
                        // Animate the rest of the way off the screen interactively
                        content.animate()
                            .translationX(screenWidth)
                            .setDuration(200)
                            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                            .setUpdateListener {
                                val progress = (content.translationX / screenWidth).coerceIn(0f, 1f)
                                val alpha = (0.55f * (1f - progress) * 255).toInt()
                                activity.window.decorView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))

                                prevContentView?.let { prev ->
                                    val scale = 0.96f + (0.04f * progress)
                                    prev.scaleX = scale
                                    prev.scaleY = scale
                                }
                            }
                            .withEndAction {
                                prevContentView?.scaleX = 1f
                                prevContentView?.scaleY = 1f
                                activity.finish()
                                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                                activity.window.decorView.post { isSlidrDismissing = false }
                            }
                            .start()
                    } else {
                        content.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .withEndAction {
                                activity.finish()
                                applyCloseTransition(activity)
                            }
                            .start()
                    }
                }
            }
            activity.onBackPressedDispatcher.addCallback(activity, backCallback)
        }

        // Apply Slidr for API < 34 AND for non-edge FULL-SCREEN swipes on API 34+
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .edge(false) // Full-screen swipe: drag from anywhere!
            .edgeSize(0f)
            .scrimColor(Color.BLACK)
            .scrimStartAlpha(0.55f)
            .scrimEndAlpha(0.0f)
            .velocityThreshold(2400f)
            .distanceThreshold(0.25f)
            .listener(object : SlidrListener {
                var prevContentView: android.view.View? = null

                override fun onSlideStateChanged(state: Int) {
                    customListener?.onSlideStateChanged(state)
                }

                override fun onSlideChange(percent: Float) {
                    val p = percent.coerceIn(0f, 1f)
                    prevContentView?.let {
                        val scale = 0.96f + (0.04f * p)
                        it.scaleX = scale
                        it.scaleY = scale
                    }
                    customListener?.onSlideChange(percent)
                }

                override fun onSlideOpened() {
                    val prevAct = PrimeApplication.getPreviousActivity()
                    prevContentView = prevAct?.findViewById(android.R.id.content)
                    customListener?.onSlideOpened()
                }

                override fun onSlideClosed(): Boolean {
                    prevContentView?.scaleX = 1f
                    prevContentView?.scaleY = 1f

                    isSlidrDismissing = true
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
}
