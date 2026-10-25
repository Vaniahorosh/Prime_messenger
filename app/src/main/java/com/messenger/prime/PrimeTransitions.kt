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
     * Checks if global animations are disabled in SharedPreferences for weak devices.
     */
    @JvmStatic
    fun areAnimationsDisabled(context: android.content.Context): Boolean {
        val sp = context.getSharedPreferences("PrimeLocalDB", android.content.Context.MODE_PRIVATE)
        return sp.getBoolean("disable_animations", false)
    }

    /**
     * Registers both OPEN and CLOSE activity transitions upfront on Android 14+ (API 34+).
     * Registering CLOSE transition upfront in `onCreate` guarantees the system
     * predictive gesture engine can seek and animate the closing transition interactively.
     */
    @JvmStatic
    fun setupActivityTransitions(activity: Activity) {
        if (activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
            // Do not override transition for root activity so system Predictive Back to Home gesture works
            return
        }
        if (areAnimationsDisabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
            return
        }
        val isTablet = activity.resources.getBoolean(R.bool.isTablet)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                if (!isTablet) 0 else R.anim.prime_open_enter,
                if (!isTablet) 0 else R.anim.prime_open_exit
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                if (!isTablet) 0 else R.anim.prime_close_enter,
                if (!isTablet) 0 else R.anim.prime_close_exit
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
        if (areAnimationsDisabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
            return
        }

        if (PrimePredictiveBack.isBottomSheetOrDialog(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_bottom, R.anim.fade_out_hold)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.fade_out_hold, R.anim.slide_out_bottom)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out_hold)
            }
            return
        }

        val isTablet = activity.resources.getBoolean(R.bool.isTablet)
        // Disable XML transitions for phones (custom PrimePredictiveBack handles Enter transition)
        val enterAnim = if (!isTablet) 0 else R.anim.prime_open_enter
        val exitAnim = if (!isTablet) 0 else R.anim.prime_open_exit
        val closeEnterAnim = if (!isTablet) 0 else R.anim.prime_close_enter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                enterAnim,
                exitAnim
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                closeEnterAnim,
                if (!isTablet) 0 else R.anim.prime_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
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
        if (activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
            // Root activity exiting to Home: allow system Predictive Back to handle transition
            return
        }
        if (isSlidrDismissing) {
            // Skip playing close transition animation if activity was already swiped off screen by Slidr
            return
        }
        if (areAnimationsDisabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
            return
        }

        if (PrimePredictiveBack.isBottomSheetOrDialog(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.fade_out_hold, R.anim.slide_out_bottom)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(R.anim.fade_out_hold, R.anim.slide_out_bottom)
            }
            return
        }

        val isTablet = activity.resources.getBoolean(R.bool.isTablet)
        // Disable XML transitions for phones (custom PrimePredictiveBack handles Exit transition)
        val enterAnim = if (!isTablet) 0 else R.anim.prime_close_enter
        val exitAnim = if (!isTablet) 0 else R.anim.prime_close_exit

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
     * Applies an exclusive horizontal page-flip / book-page slide transition when navigating between
     * Bottom Navigation tabs. Creates a seamless page-turning feel instead of standard overlapping activities.
     */
    @JvmStatic
    @JvmOverloads
    fun applyPageFlipTransition(activity: Activity, isForward: Boolean = true) {
        if (areAnimationsDisabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
            return
        }
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
        if (activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
            return null
        }
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
                var isGestureActive = false
                var screenWidth = 0f
                var contentView: android.view.View? = null
                var prevContentView: android.view.View? = null

                override fun handleOnBackStarted(backEvent: androidx.activity.BackEventCompat) {
                    isGestureActive = true
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
                        // The user is swiping from the left edge to the right (dragging current activity right)
                        content.translationX = backEvent.touchX

                        val progress = (backEvent.touchX / screenWidth).coerceIn(0f, 1f)

                        val alpha = (0.55f * (1f - progress) * 255).toInt()
                        activity.window.decorView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))

                        // Seamlessly slide previous activity in from the left (-20% -> 0%) and scale up (0.96 -> 1.0)
                        prevContentView?.let {
                            val scale = 0.96f + (0.04f * progress)
                            it.scaleX = scale
                            it.scaleY = scale
                            it.translationX = -0.2f * screenWidth * (1f - progress)
                        }

                        customListener?.onSlideChange(progress)
                    } else {
                        // For right edge back gesture, perform subtle scale feedback
                        val scale = 1f - (0.05f * backEvent.progress)
                        content.scaleX = scale
                        content.scaleY = scale
                    }
                }

                override fun handleOnBackCancelled() {
                    isGestureActive = false
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
                                    prev.translationX = -0.2f * screenWidth * (1f - progress)
                                }
                            }
                        }
                        .withEndAction {
                            activity.window.decorView.setBackgroundColor(Color.TRANSPARENT)
                            prevContentView?.scaleX = 1f
                            prevContentView?.scaleY = 1f
                            prevContentView?.translationX = 0f
                        }
                        .start()
                }

                override fun handleOnBackPressed() {
                    if (!isGestureActive) {
                        isEnabled = false
                        activity.onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                        return
                    }
                    isGestureActive = false
                    val content = contentView ?: return
                    if (isLeftEdge) {
                        isSlidrDismissing = true
                        customListener?.onSlideClosed()
                        // Animate current activity off-screen to the right and previous activity into place
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
                                    prev.translationX = -0.2f * screenWidth * (1f - progress)
                                }
                            }
                            .withEndAction {
                                prevContentView?.scaleX = 1f
                                prevContentView?.scaleY = 1f
                                prevContentView?.translationX = 0f
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

        // Apply full-screen Slidr swipe-to-close across the entire screen
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .edge(false)
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

    /**
     * Protects all HorizontalScrollViews, Sliders, and horizontal scrolling containers
     * from triggering Activity Slidr swipe-to-close gestures or parent interception.
     */
    @JvmStatic
    fun protectHorizontalScrollViews(container: android.view.ViewGroup, slidrInterface: SlidrInterface? = null) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val isHorizontalRecycler = child is androidx.recyclerview.widget.RecyclerView && 
                (child.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.orientation == androidx.recyclerview.widget.RecyclerView.HORIZONTAL
                
            if (child is android.widget.HorizontalScrollView ||
                child is com.google.android.material.slider.Slider ||
                child is androidx.viewpager.widget.ViewPager ||
                child is androidx.viewpager2.widget.ViewPager2 ||
                isHorizontalRecycler
            ) {
                setupProtectionOnView(child, slidrInterface)
            }
            if (child is android.view.ViewGroup) {
                protectHorizontalScrollViews(child, slidrInterface)
            }
        }
    }

    /**
     * Attaches touch listener to a horizontal scrolling view to lock Slidr and request
     * disallowInterceptTouchEvent while horizontal scrolling is active.
     */
    @JvmStatic
    fun setupProtectionOnView(view: android.view.View, slidrInterface: SlidrInterface? = null) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    slidrInterface?.lock()
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    slidrInterface?.lock()
                    v.onTouchEvent(event)
                    return@setOnTouchListener true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    v.onTouchEvent(event)
                    slidrInterface?.unlock()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
