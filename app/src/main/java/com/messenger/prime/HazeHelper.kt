package com.messenger.prime

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView

object HazeHelper {
    
    fun applyHardwareBlur(view: View, radius: Float = 32f, disableEffect: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (disableEffect) {
                view.setRenderEffect(null)
            } else {
                view.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                )
            }
        } else {
            view.setBackgroundColor(Color.parseColor("#99000000"))
        }
    }

    fun attachScrollThrottler(recyclerView: RecyclerView, blurView: View) {
        applyHardwareBlur(blurView, disableEffect = false)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val isScrolling = newState == RecyclerView.SCROLL_STATE_DRAGGING || 
                                  newState == RecyclerView.SCROLL_STATE_SETTLING
                
                applyHardwareBlur(blurView, disableEffect = isScrolling)
            }
        })
    }
}