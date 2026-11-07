package com.messenger.prime

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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

fun getDefaultBlurOverlayColor(context: Context): Int {
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return if (isDark) {
        android.graphics.Color.parseColor("#400F172A")
    } else {
        android.graphics.Color.parseColor("#40FFFFFF") // Better for light theme
    }
}

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val overlayColorInt = if (tint != Color.Unspecified) {
        tint.toArgb()
    } else {
        getDefaultBlurOverlayColor(context)
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
                                android.R.id.content)
                                ?: (activity?.window?.decorView as? ViewGroup)
                                ?: (view.rootView as? ViewGroup)

                            if (targetRootView != null && targetRootView != this) {
                                val windowBackground = activity?.window?.decorView?.background
                                val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                                val isLowRam = activityManager?.isLowRamDevice == true

                                if (isLowRam) {
                                    setBlurEnabled(false)
                                    setBackgroundColor(overlayColorInt)
                                } else {
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
    tintColor: Int? = null,
    frameClearDrawable: Drawable? = null
) {
    val actualTintColor = tintColor ?: getDefaultBlurOverlayColor(context)
    
    outlineProvider = ViewOutlineProvider.BACKGROUND
    clipToOutline = true

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRam = activityManager?.isLowRamDevice == true

    if (isLowRam) {
        this.setBackgroundColor(actualTintColor)
        this.setBlurEnabled(false)
        return
    }

    val initBlur: (View) -> Unit = { v ->
        if (v.width > 0 && v.height > 0 && v.isAttachedToWindow) {
            try {
                val targetRoot = if (rootView == v) {
                    (v.parent as? ViewGroup) ?: rootView
                } else {
                    rootView
                }

                val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RenderEffectBlur()
                } else {
                    RenderScriptBlur(context)
                }

                setBlurEnabled(true)
                setupWith(targetRoot, algorithm)
                    .setBlurRadius(blurRadius.coerceIn(1f, 25f))
                    .setOverlayColor(actualTintColor)
                    .setBlurAutoUpdate(true)
                    .apply {
                        if (frameClearDrawable != null) {
                            setFrameClearDrawable(frameClearDrawable)
                        }
                    }
            } catch (e: Throwable) {
                e.printStackTrace()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        setRenderEffect(RenderEffect.createBlurEffect(blurRadius.coerceIn(1f, 25f), blurRadius.coerceIn(1f, 25f), Shader.TileMode.CLAMP))
                    } catch (_: Throwable) {
                        setBackgroundColor(actualTintColor)
                    }
                } else {
                    setBackgroundColor(actualTintColor)
                }
            }
        }
    }

    if (width > 0 && height > 0 && isAttachedToWindow) {
        initBlur(this)
    } else {
        addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                if (v != null && v.width > 0 && v.height > 0 && v.isAttachedToWindow) {
                    v.removeOnLayoutChangeListener(this)
                    initBlur(v)
                }
            }
        })
    }
}

fun Modifier.blurView(
    blurRadius: Dp = 16.dp,
    tint: Color = Color.Unspecified
): Modifier = composed {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actualTint = if (tint != Color.Unspecified) tint else Color(getDefaultBlurOverlayColor(context))
    this.then(Modifier.background(actualTint))
}

fun View.applyBlurView(radius: Float = 30f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
    } else {
        setBackgroundColor(getDefaultBlurOverlayColor(context))
    }
}

object BlurViewHelper {
    @JvmStatic
    @JvmOverloads
    fun setupBlurView(composeView: ComposeView?, tint: Color = Color.Unspecified) {
        composeView?.setContent {
            BlurView(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 16.dp,
                tint = tint
            )
        }
    }
}
