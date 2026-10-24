package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.window.embedding.ActivityEmbeddingController
import kotlin.math.abs

/**
 * Prime Native Predictive Back Gesture & Touch Controller.
 * Implements native Android 13+ Predictive Back gestures, integrated touch swipe-to-dismiss,
 * and a symmetric inverse Enter Transition (Zoom-In from depth).
 */
object PrimePredictiveBack {

    @Volatile
    @JvmStatic
    var isGestureLocked: Boolean = false

    private val emphasizedInterpolator = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    private val springReleaseInterpolator = PathInterpolator(0.175f, 0.885f, 0.32f, 1.275f)
    private val finishInterpolator = DecelerateInterpolator(1.5f)
    private val enterInterpolator = DecelerateInterpolator(1.5f)

    @JvmStatic
    @JvmOverloads
    fun attach(
        activity: ComponentActivity,
        playEnterTransition: Boolean = false,
        targetView: View? = null,
        onBackCompleted: (() -> Unit)? = null
    ) {
        if (activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
            // Do not attach custom gesture, allow system to handle "Back to Home".
            return
        }
        GestureController(activity, playEnterTransition, targetView, onBackCompleted)
    }

    private class GestureController(
        val activity: ComponentActivity,
        playEnterTransition: Boolean,
        targetView: View?,
        val onBackCompleted: (() -> Unit)?
    ) {
        val rootContent: View = targetView
            ?: activity.findViewById(android.R.id.content)
            ?: activity.window.decorView

        val density = activity.resources.displayMetrics.density
        val screenWidth = activity.resources.displayMetrics.widthPixels.toFloat()
        val maxCornerRadiusPx = 32f * density
        val maxShiftPx = 24f * density
        val maxElevationPx = 16f * density
        val isTablet = activity.resources.configuration.smallestScreenWidthDp >= 600

        var isExecutingBack = false
        var currentCornerRadiusPx: Float = 0f
        var originalOutlineProvider: ViewOutlineProvider? = null
        var originalClipToOutline: Boolean = false

        var previousRootContent: View? = null
        var dimOverlayView: View? = null
        var currentSwipeEdge: Int = BackEventCompat.EDGE_LEFT

        // Touch gesture tracking
        var isSwiping = false
        var isSwipeAllowed = false
        var startX = 0f
        var startY = 0f
        var velocityTracker: VelocityTracker? = null
        val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
        
        var isEnterAnimationRunning = false

        init {
            if (playEnterTransition) {
                // Prepare initial state for Enter Transition immediately
                setupEnterTransitionInitialState()
                
                rootContent.post {
                    executeEnterAnimation()
                }
            }

            // 1. Native Predictive Back Callback
            val backCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    if (activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
                        isEnabled = false
                        activity.onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                        return
                    }
                    if (isEnterAnimationRunning) return
                    startBackGesture(backEvent.swipeEdge)
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (!isEnabled || isExecutingBack || isMultiPaneLayout() || isEnterAnimationRunning) return
                    applyProgress(backEvent.progress, currentSwipeEdge == BackEventCompat.EDGE_LEFT, backEvent.touchY)
                }

                override fun handleOnBackPressed() {
                    if (!isEnabled) return
                    if (isEnterAnimationRunning) {
                        onBackCompleted?.invoke() ?: PrimeTransitions.finishWithTransition(activity)
                        return
                    }
                    executeFinishAnimation()
                }

                override fun handleOnBackCancelled() {
                    if (!isEnabled) return
                    if (isEnterAnimationRunning) return
                    executeCancelAnimation()
                }
            }
            activity.onBackPressedDispatcher.addCallback(activity, backCallback)

            // Dynamically evaluate if we should enable the callback (so system Back to Home works)
            activity.lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    backCallback.isEnabled = !activity.isTaskRoot && PrimeApplication.getPreviousActivity() != null
                }
            })

            // 2. Custom Touch Interceptor for Swipe-to-Dismiss
            val originalWindowCallback = activity.window.callback
            activity.window.callback = object : Window.Callback by originalWindowCallback {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    if (isGestureLocked || activity.isTaskRoot || PrimeApplication.getPreviousActivity() == null) {
                        return originalWindowCallback.dispatchTouchEvent(event)
                    }
                    
                    if (isExecutingBack || isMultiPaneLayout() || isEnterAnimationRunning) {
                        return originalWindowCallback.dispatchTouchEvent(event)
                    }

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            isSwiping = false
                            
                            // Check immediately if we touched an interactive element
                            // Passing dx=0 because we don't know the direction yet, but we want to fail fast for SeekBars/Sliders
                            isSwipeAllowed = !hasHorizontalConflict(rootContent, startX.toInt(), startY.toInt(), 0f, startX, screenWidth, density, activity)
                            
                            if (isSwipeAllowed) {
                                velocityTracker = VelocityTracker.obtain()
                                velocityTracker?.addMovement(event)
                            }
                            // Allow children to receive the touch event
                            return originalWindowCallback.dispatchTouchEvent(event)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isSwipeAllowed) {
                                return originalWindowCallback.dispatchTouchEvent(event)
                            }

                            velocityTracker?.addMovement(event)
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY

                            if (!isSwiping) {
                                // If vertical scroll is dominating, cancel swipe ability
                                if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                                    isSwipeAllowed = false
                                    return originalWindowCallback.dispatchTouchEvent(event)
                                }

                                val isMovingRight = dx > touchSlop
                                val isMovingLeft = dx < -touchSlop
                                // Strict horizontal check
                                val isHorizontal = abs(dx) > abs(dy) * 1.5f

                                if ((isMovingRight || isMovingLeft) && isHorizontal) {
                                    // Deep hit-test again now with direction (dx) for ScrollViews and Swipe-to-Reply
                                    if (!hasHorizontalConflict(rootContent, startX.toInt(), startY.toInt(), dx, startX, screenWidth, density, activity)) {
                                        isSwiping = true
                                        val edge = if (isMovingRight) BackEventCompat.EDGE_LEFT else BackEventCompat.EDGE_RIGHT
                                        startBackGesture(edge)

                                        // Cancel touch sequence for child views
                                        val cancelEvent = MotionEvent.obtain(event)
                                        cancelEvent.action = MotionEvent.ACTION_CANCEL
                                        originalWindowCallback.dispatchTouchEvent(cancelEvent)
                                        cancelEvent.recycle()
                                    } else {
                                        isSwipeAllowed = false // Abort trying to swipe for this gesture
                                    }
                                }
                            }

                            if (isSwiping) {
                                val isLeft = currentSwipeEdge == BackEventCompat.EDGE_LEFT
                                val progress = (abs(event.rawX - startX) / screenWidth).coerceIn(0f, 1f)

                                // Apply identical progress mapping to native predictive back
                                applyProgress(progress, isLeft, event.rawY)
                                return true // Consume
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isSwiping) {
                                velocityTracker?.addMovement(event)
                                velocityTracker?.computeCurrentVelocity(1000)
                                val vx = velocityTracker?.xVelocity ?: 0f
                                val isLeft = currentSwipeEdge == BackEventCompat.EDGE_LEFT

                                val progress = (abs(event.rawX - startX) / screenWidth).coerceIn(0f, 1f)

                                val velocityCondition = if (isLeft) vx > 500f else vx < -500f

                                if (progress > 0.35f || velocityCondition) {
                                    executeFinishAnimation()
                                } else {
                                    executeCancelAnimation()
                                }

                                velocityTracker?.recycle()
                                velocityTracker = null
                                isSwiping = false
                                return true
                            }
                            velocityTracker?.recycle()
                            velocityTracker = null
                        }
                    }
                    return originalWindowCallback.dispatchTouchEvent(event)
                }
            }
        }

        private fun setupEnterTransitionInitialState() {
            if (isMultiPaneLayout()) return
            
            rootContent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            originalOutlineProvider = rootContent.outlineProvider
            originalClipToOutline = rootContent.clipToOutline
            
            currentCornerRadiusPx = maxCornerRadiusPx
            rootContent.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, currentCornerRadiusPx)
                }
            }
            rootContent.clipToOutline = true
            
            rootContent.scaleX = 0.86f
            rootContent.scaleY = 0.86f
            rootContent.alpha = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rootContent.outlineAmbientShadowColor = 0x40000000
                rootContent.outlineSpotShadowColor = 0x60000000
            }
            rootContent.translationZ = maxElevationPx
            
            val prevActivity = PrimeApplication.getPreviousActivity()
            if (prevActivity != null && !prevActivity.isDestroyed) {
                val prevRoot = prevActivity.findViewById<ViewGroup>(android.R.id.content)
                if (prevRoot != null) {
                    previousRootContent = prevRoot
                    prevRoot.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    
                    if (dimOverlayView == null) {
                        dimOverlayView = View(prevActivity).apply {
                            setBackgroundColor(Color.BLACK)
                            alpha = 0f
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                    if (dimOverlayView?.parent == null) {
                        prevRoot.addView(dimOverlayView)
                    }
                    
                    prevRoot.scaleX = 1.0f
                    prevRoot.scaleY = 1.0f
                    prevRoot.pivotX = prevRoot.width / 2f
                    prevRoot.pivotY = prevRoot.height / 2f
                }
            }
        }

        private fun executeEnterAnimation() {
            if (isMultiPaneLayout()) return
            isEnterAnimationRunning = true
            
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 280
            animator.interpolator = enterInterpolator
            
            val startScale = 0.86f
            val startCorners = maxCornerRadiusPx
            val startZ = maxElevationPx
            
            animator.addUpdateListener { anim ->
                val frac = anim.animatedFraction
                
                // Top screen zoom in and fade in
                val newScale = startScale + ((1f - startScale) * frac)
                rootContent.scaleX = newScale
                rootContent.scaleY = newScale
                
                // Alpha fades in quicker (first 100ms approx -> frac ~ 0.35)
                val alphaFrac = (frac / 0.35f).coerceIn(0f, 1f)
                rootContent.alpha = alphaFrac
                
                currentCornerRadiusPx = startCorners * (1f - frac)
                rootContent.invalidateOutline()
                rootContent.translationZ = startZ * (1f - frac)
                
                // Background screen zooms out to 0.94 and dims
                previousRootContent?.let { prev ->
                    val pScale = 1.0f - (0.06f * frac)
                    prev.scaleX = pScale
                    prev.scaleY = pScale
                }
                dimOverlayView?.alpha = 0.3f * frac
            }
            
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    resetViewProperties()
                    resetPreviousViewProperties()
                    isEnterAnimationRunning = false
                }
            })
            animator.start()
        }

        /**
         * Recursively hit-tests the View hierarchy to check if a conflicting horizontally scrollable
         * or interactive element is present under the finger.
         */
        private fun hasHorizontalConflict(
            v: View, x: Int, y: Int, dx: Float, startX: Float, screenWidth: Float, density: Float, activity: ComponentActivity
        ): Boolean {
            if (v.visibility != View.VISIBLE) return false

            val location = IntArray(2)
            v.getLocationOnScreen(location)
            val left = location[0]
            val top = location[1]
            val right = left + v.width
            val bottom = top + v.height

            // Target is outside this view
            if (x < left || x > right || y < top || y > bottom) return false

            // Check children recursively first (top-down in Z-order conceptually)
            if (v is ViewGroup) {
                for (i in v.childCount - 1 downTo 0) {
                    val child = v.getChildAt(i)
                    if (hasHorizontalConflict(child, x, y, dx, startX, screenWidth, density, activity)) {
                        return true
                    }
                }
            }

            // Protect Multi-Selection in ChatPersonActivity
            if (activity.javaClass.simpleName == "ChatPersonActivity") {
                try {
                    val field = activity.javaClass.getDeclaredField("isMultiSelectionActive")
                    field.isAccessible = true
                    if (field.getBoolean(activity)) {
                        val edgeWidthPx = 40f * density
                        if (startX > edgeWidthPx && startX < screenWidth - edgeWidthPx) {
                            return true
                        }
                    }
                } catch (_: Exception) {}
            }

            // Check if current view is a globally conflicting input type (Slider, SeekBar, etc.)
            val className = v.javaClass.simpleName
            val idName = try { v.resources.getResourceEntryName(v.id) } catch (_: Exception) { "" }
            
            if (idName.contains("Voice") || idName.contains("Mic") || idName.contains("Record") || idName.contains("btnVoiceVideoNote") ||
                className.contains("SeekBar") || className.contains("Slider") || className.contains("Switch") || className.contains("ProgressBar")) {
                return true
            }

            // Protect RecyclerView's ItemTouchHelper (Swipe-to-Reply / Swipe-to-Delete)
            if (className.contains("RecyclerView") || className.contains("ChatMessageView")) {
                val edgeWidthPx = 40f * density
                val isEdgeSwipe = startX <= edgeWidthPx || startX >= screenWidth - edgeWidthPx
                
                // If it's a left swipe (dx < 0) from the center, we assume it's Swipe-to-Reply
                if (dx < 0 && !isEdgeSwipe) {
                    return true
                }
            }

            // Check if current view can scroll horizontally in the swiping direction.
            // If dx > 0 (swipe right), check if view can scroll LEFT (-1)
            // If dx < 0 (swipe left), check if view can scroll RIGHT (1)
            val direction = if (dx > 0) -1 else 1
            if (dx != 0f && v.canScrollHorizontally(direction)) {
                return true
            }

            return false
        }

        private fun isMultiPaneLayout(): Boolean {
            val isEmbedded = try {
                ActivityEmbeddingController.getInstance(activity).isActivityEmbedded(activity)
            } catch (_: Exception) {
                false
            }
            return isEmbedded || (isTablet && activity !is ChatPersonActivity && activity !is SettingsActivity && activity !is PersonInformationActivity)
        }

        private fun startBackGesture(edge: Int) {
            if (isExecutingBack || isMultiPaneLayout()) return
            currentSwipeEdge = edge

            rootContent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            originalOutlineProvider = rootContent.outlineProvider
            originalClipToOutline = rootContent.clipToOutline

            rootContent.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, currentCornerRadiusPx)
                }
            }
            rootContent.clipToOutline = true

            val prevActivity = PrimeApplication.getPreviousActivity()
            if (prevActivity != null && !prevActivity.isDestroyed) {
                val prevRoot = prevActivity.findViewById<ViewGroup>(android.R.id.content)
                if (prevRoot != null) {
                    previousRootContent = prevRoot
                    prevRoot.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    if (dimOverlayView == null) {
                        dimOverlayView = View(prevActivity).apply {
                            setBackgroundColor(Color.BLACK)
                            alpha = 0.3f
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                    if (dimOverlayView?.parent == null) {
                        prevRoot.addView(dimOverlayView)
                    }

                    prevRoot.scaleX = 0.94f
                    prevRoot.scaleY = 0.94f
                    prevRoot.pivotX = prevRoot.width / 2f
                    prevRoot.pivotY = prevRoot.height / 2f
                }
            }
        }

        private fun applyProgress(rawProgress: Float, isLeftEdge: Boolean, touchY: Float) {
            val progress = emphasizedInterpolator.getInterpolation(rawProgress)

            val scale = 1.0f - (progress * 0.14f)
            rootContent.scaleX = scale
            rootContent.scaleY = scale

            val pivotX = if (isLeftEdge) rootContent.width.toFloat() else 0f
            val targetPivotY = if (touchY.isNaN()) (rootContent.height / 2f) else touchY
            val currentPivotY = (rootContent.height / 2f) * 0.5f + targetPivotY * 0.5f
            
            rootContent.pivotX = pivotX
            rootContent.pivotY = currentPivotY

            val shiftDirection = if (isLeftEdge) 1f else -1f
            rootContent.translationX = shiftDirection * (progress * maxShiftPx)

            currentCornerRadiusPx = progress * maxCornerRadiusPx
            rootContent.invalidateOutline()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rootContent.outlineAmbientShadowColor = 0x40000000
                rootContent.outlineSpotShadowColor = 0x60000000
            }
            rootContent.translationZ = progress * maxElevationPx

            previousRootContent?.let { prev ->
                val prevScale = 0.94f + (progress * 0.06f)
                prev.scaleX = prevScale
                prev.scaleY = prevScale
                dimOverlayView?.alpha = 0.3f * (1f - progress)
            }
        }

        private fun executeFinishAnimation() {
            if (isExecutingBack || isMultiPaneLayout()) {
                if (!isExecutingBack) {
                    onBackCompleted?.invoke() ?: PrimeTransitions.finishWithTransition(activity)
                }
                return
            }
            isExecutingBack = true

            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 200
            animator.interpolator = finishInterpolator

            val startScale = rootContent.scaleX
            val startTransX = rootContent.translationX
            val prevStartScale = previousRootContent?.scaleX ?: 0.94f
            val dimStartAlpha = dimOverlayView?.alpha ?: 0.3f

            val isLeftEdge = currentSwipeEdge == BackEventCompat.EDGE_LEFT
            val shiftDirection = if (isLeftEdge) 1f else -1f
            val targetTransX = startTransX + (shiftDirection * maxShiftPx * 1.5f)

            animator.addUpdateListener { anim ->
                val frac = anim.animatedFraction
                val newScale = startScale - (frac * 0.06f)
                rootContent.scaleX = newScale
                rootContent.scaleY = newScale
                rootContent.alpha = 1f - frac
                rootContent.translationX = startTransX + ((targetTransX - startTransX) * frac)

                previousRootContent?.let { prev ->
                    val pScale = prevStartScale + ((1f - prevStartScale) * frac)
                    prev.scaleX = pScale
                    prev.scaleY = pScale
                }
                dimOverlayView?.alpha = dimStartAlpha * (1f - frac)
            }

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Do not call resetViewProperties() to prevent the visual flash back to scale 1.0!
                    // Keep the departing view fully invisible.
                    rootContent.alpha = 0f
                    rootContent.visibility = View.INVISIBLE
                    
                    resetPreviousViewProperties()
                    
                    if (onBackCompleted != null) {
                        onBackCompleted.invoke()
                    } else {
                        activity.finish()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            activity.overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            activity.overridePendingTransition(0, 0)
                        }
                    }
                    rootContent.postDelayed({ isExecutingBack = false }, 100)
                }
            })
            animator.start()
        }

        private fun executeCancelAnimation() {
            if (isExecutingBack || isMultiPaneLayout()) return

            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 350
            animator.interpolator = springReleaseInterpolator

            val startScale = rootContent.scaleX
            val startTransX = rootContent.translationX
            val startTransZ = rootContent.translationZ
            val startCorners = currentCornerRadiusPx

            val prevStartScale = previousRootContent?.scaleX ?: 1.0f
            val dimStartAlpha = dimOverlayView?.alpha ?: 0.0f

            animator.addUpdateListener { anim ->
                val frac = anim.animatedFraction

                rootContent.scaleX = startScale + ((1f - startScale) * frac)
                rootContent.scaleY = startScale + ((1f - startScale) * frac)
                rootContent.translationX = startTransX * (1f - frac)
                rootContent.translationZ = startTransZ * (1f - frac)

                currentCornerRadiusPx = startCorners * (1f - frac)
                rootContent.invalidateOutline()

                previousRootContent?.let { prev ->
                    val pScale = prevStartScale - ((prevStartScale - 0.94f) * frac)
                    prev.scaleX = pScale
                    prev.scaleY = pScale
                }
                dimOverlayView?.alpha = dimStartAlpha + ((0.3f - dimStartAlpha) * frac)
            }

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    resetViewProperties()
                    resetPreviousViewProperties()
                }
            })
            animator.start()
        }

        private fun resetViewProperties() {
            rootContent.scaleX = 1.0f
            rootContent.scaleY = 1.0f
            rootContent.translationX = 0f
            rootContent.translationZ = 0f
            rootContent.alpha = 1.0f
            rootContent.pivotX = rootContent.width / 2f
            rootContent.pivotY = rootContent.height / 2f
            currentCornerRadiusPx = 0f
            rootContent.outlineProvider = originalOutlineProvider
            rootContent.clipToOutline = originalClipToOutline
            rootContent.setLayerType(View.LAYER_TYPE_NONE, null)
        }

        private fun resetPreviousViewProperties() {
            previousRootContent?.let { prev ->
                prev.scaleX = 1.0f
                prev.scaleY = 1.0f
                prev.alpha = 1.0f
                prev.pivotX = prev.width / 2f
                prev.pivotY = prev.height / 2f
                prev.setLayerType(View.LAYER_TYPE_NONE, null)
                dimOverlayView?.let { dim ->
                    (prev as? ViewGroup)?.removeView(dim)
                }
            }
            previousRootContent = null
            dimOverlayView = null
        }
    }
}
