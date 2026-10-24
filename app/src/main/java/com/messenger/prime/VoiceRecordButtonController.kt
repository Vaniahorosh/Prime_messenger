package com.messenger.prime

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class VoiceRecordButtonController(
    private val button: View,
    private val listener: VoiceRecordListener
) {
    interface VoiceRecordListener {
        fun onClick()
        fun onRecordStart()
        fun onRecordMove(dx: Float, dy: Float)
        fun onRecordCancel()
        fun onRecordLocked()
        fun onRecordFinish()
    }

    private var startX = 0f
    private var startY = 0f
    private var isRecording = false
    private var isLocked = false
    private var isLongPressTriggered = false

    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        isRecording = true
        button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        listener.onRecordStart()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        button.setOnTouchListener { v, event ->
            // Prevent parent viewgroup from intercepting touches
            v.parent?.requestDisallowInterceptTouchEvent(true)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isRecording = false
                    isLocked = false
                    isLongPressTriggered = false

                    // Lock PrimePredictiveBack gestures
                    PrimePredictiveBack.isGestureLocked = true

                    // Post long press runnable (180ms threshold)
                    handler.postDelayed(longPressRunnable, 180)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY

                    if (!isLongPressTriggered) {
                        // If moved too much before long press, cancel long press
                        if (abs(dx) > 30f || abs(dy) > 30f) {
                            handler.removeCallbacks(longPressRunnable)
                        }
                    } else if (isRecording) {
                        listener.onRecordMove(dx, dy)

                        if (!isLocked) {
                            // Lock condition (swipe up)
                            if (dy < -180f) {
                                isLocked = true
                                listener.onRecordLocked()
                            }

                            // Cancel condition (swipe left)
                            if (dx < -200f) {
                                isRecording = false
                                handler.removeCallbacks(longPressRunnable)
                                listener.onRecordCancel()
                                PrimePredictiveBack.isGestureLocked = false
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    PrimePredictiveBack.isGestureLocked = false

                    if (isLongPressTriggered) {
                        if (isRecording && !isLocked) {
                            isRecording = false
                            listener.onRecordFinish()
                        }
                    } else {
                        // It was a short tap / click
                        listener.onClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    PrimePredictiveBack.isGestureLocked = false
                    if (isRecording) {
                        isRecording = false
                        listener.onRecordCancel()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
