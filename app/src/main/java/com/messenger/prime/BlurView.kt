package com.messenger.prime

import android.R
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnPreDraw
import eightbitlab.com.blurview.BlurView as EightBitBlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

/**
 * BlurView - Настоящее размытие фона с использованием библиотеки Dimezis BlurView
 * через AndroidView в Jetpack Compose с неразмываемым контентом поверх.
 */
@Composable
fun BlurView(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 16.dp,
    tint: Color = Color.Unspecified,
    shape: Shape = RectangleShape,
    content: (@Composable () -> Unit)? = null
) {
    val overlayColorInt = if (tint != Color.Unspecified) {
        tint.toArgb()
    } else {
        android.graphics.Color.parseColor("#40154B87")
    }

    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center
    ) {
        // 1. Подложка реального размытия через eightbitlab.com.blurview.BlurView
        AndroidView(
            factory = { ctx ->
                EightBitBlurView(ctx).apply {
                    doOnPreDraw { view ->
                        if (view.width <= 0 || view.height <= 0 || !view.isAttachedToWindow) return@doOnPreDraw
                        try {
                            val activity = findActivity(ctx)
                            val targetRootView = activity?.window?.decorView?.findViewById<ViewGroup>(
                                R.id.content)
                                ?: (activity?.window?.decorView as? ViewGroup)
                                ?: (view.rootView as? ViewGroup)

                            if (targetRootView != null && targetRootView != this) {
                                val windowBackground = activity?.window?.decorView?.background
                                val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    RenderEffectBlur()
                                } else {
                                    RenderScriptBlur(ctx)
                                }

                                setupWith(targetRootView, algorithm)
                                    .setBlurRadius(blurRadius.value)
                                    .setOverlayColor(overlayColorInt)
                                    .apply {
                                        if (windowBackground != null) {
                                            setFrameClearDrawable(windowBackground)
                                        }
                                    }
                            } else {
                                setBackgroundColor(overlayColorInt)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            setBackgroundColor(overlayColorInt)
                        }
                    }
                }
            },
            modifier = Modifier.matchParentSize()
        )

        // 2. Четкий контент строго поверх размытия
        if (content != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Поиск родительской Activity.
 */
private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Расширение для настройки eightbitlab.com.blurview.BlurView в XML-разметках Activity.
 */
fun EightBitBlurView.setupBlur(
    rootView: ViewGroup,
    blurRadius: Float = 16f,
    overlayColor: Int = android.graphics.Color.parseColor("#40154B87"),
    frameClearDrawable: Drawable? = null
) {
    doOnPreDraw { view ->
        if (view.width <= 0 || view.height <= 0 || !view.isAttachedToWindow) return@doOnPreDraw
        try {
            val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RenderEffectBlur()
            } else {
                RenderScriptBlur(context)
            }

            val facade = setupWith(rootView, algorithm)
                .setBlurRadius(blurRadius)
                .setOverlayColor(overlayColor)
                .setBlurAutoUpdate(true)

            if (frameClearDrawable != null) {
                facade.setFrameClearDrawable(frameClearDrawable)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            setBackgroundColor(overlayColor)
        }
    }
}

fun Modifier.blurView(
    blurRadius: Dp = 16.dp,
    tint: Color = Color.Unspecified
): Modifier = this.then(
    if (tint != Color.Unspecified) Modifier.background(tint) 
    else Modifier.background(Color(0x40154B87))
)

fun View.applyBlurView(radius: Float = 30f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
    } else {
        setBackgroundColor(android.graphics.Color.parseColor("#40154B87"))
    }
}

object BlurViewHelper {
    @JvmStatic
    @JvmOverloads
    fun setupBlurView(composeView: ComposeView?, tint: Color = Color(0x40154B87)) {
        composeView?.setContent {
            BlurView(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 16.dp,
                tint = tint
            )
        }
    }
}
