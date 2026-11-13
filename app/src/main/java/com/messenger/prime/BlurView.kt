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
import androidx.core.view.doOnLayout
import eightbitlab.com.blurview.BlurView as EightBitBlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import com.messenger.prime.shapes.PolygonShape
import com.messenger.prime.shapes.PathShape
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Canvas
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.messenger.prime.shapes.M3EShapes

fun View.applyM3EShape(shape: RoundedPolygon) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.elevation = 0f // CRITICAL: Prevent Mali/Lenovo GPU ANR from concave shadow casting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.outlineAmbientShadowColor = android.graphics.Color.TRANSPARENT
            this.outlineSpotShadowColor = android.graphics.Color.TRANSPARENT
        }
        this.clipToOutline = true
        this.outlineProvider = object : ViewOutlineProvider() {
            private val cachedPath = Path()
            private var lastWidth = 0
            private var lastHeight = 0

            override fun getOutline(view: View, outline: Outline) {
                if (view.width <= 0 || view.height <= 0) return
                if (view.width != lastWidth || view.height != lastHeight) {
                    lastWidth = view.width
                    lastHeight = view.height
                    
                    val androidPath = shape.toPath()
                    val bounds = RectF()
                    androidPath.computeBounds(bounds, true)
                    
                    if (bounds.width() <= 0f || bounds.height() <= 0f) return

                    val scaleX = view.width.toFloat() / bounds.width()
                    val scaleY = view.height.toFloat() / bounds.height()
                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(-bounds.left, -bounds.top)
                    matrix.postScale(scaleX, scaleY)
                    androidPath.transform(matrix)
                    
                    cachedPath.rewind()
                    cachedPath.addPath(androidPath)
                }
                if (!cachedPath.isEmpty) {
                    outline.setPath(cachedPath)
                }
            }
        }
        this.invalidateOutline()
        this.doOnLayout { it.invalidateOutline() }
        this.post { this.invalidateOutline() }
    }
}

fun View.applyM3EPathProvider(pathProvider: (width: Float, height: Float) -> Path) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.elevation = 0f // CRITICAL: Prevent Mali/Lenovo GPU ANR from concave shadow casting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.outlineAmbientShadowColor = android.graphics.Color.TRANSPARENT
            this.outlineSpotShadowColor = android.graphics.Color.TRANSPARENT
        }
        this.clipToOutline = true
        this.outlineProvider = object : ViewOutlineProvider() {
            private val cachedPath = Path()
            private var lastWidth = 0
            private var lastHeight = 0

            override fun getOutline(view: View, outline: Outline) {
                if (view.width <= 0 || view.height <= 0) return
                if (view.width != lastWidth || view.height != lastHeight) {
                    lastWidth = view.width
                    lastHeight = view.height
                    cachedPath.rewind()
                    val newPath = pathProvider(view.width.toFloat(), view.height.toFloat())
                    if (newPath != null && !newPath.isEmpty) {
                        cachedPath.addPath(newPath)
                    }
                }
                if (!cachedPath.isEmpty) {
                    outline.setPath(cachedPath)
                }
            }
        }
        this.invalidateOutline()
        this.doOnLayout { it.invalidateOutline() }
        this.post { this.invalidateOutline() }
    }
}

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
@JvmOverloads
fun EightBitBlurView.setupBlur(
    rootView: ViewGroup,
    blurRadius: Float = 16f,
    tintColor: Int? = null,
    frameClearDrawable: Drawable? = null,
    m3eShape: RoundedPolygon? = null,
    m3ePathProvider: ((width: Float, height: Float) -> Path)? = null
) {
    val actualTintColor = tintColor ?: getDefaultBlurOverlayColor(context)
    
    // Custom clip path for API < 33 if needed, or ViewOutlineProvider for API 33+
    if (m3eShape != null || m3ePathProvider != null) {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val path = if (m3eShape != null) {
                        val androidPath = m3eShape.toPath()
                        val bounds = RectF()
                        androidPath.computeBounds(bounds, true)
                        val scaleX = view.width / bounds.width()
                        val scaleY = view.height / bounds.height()
                        val matrix = android.graphics.Matrix()
                        matrix.postTranslate(-bounds.left, -bounds.top)
                        matrix.postScale(scaleX, scaleY)
                        androidPath.transform(matrix)
                        androidPath
                    } else {
                        m3ePathProvider!!.invoke(view.width.toFloat(), view.height.toFloat())
                    }
                    outline.setPath(path)
                } else {
                    // Fallback for older APIs: set bounds outline and we'll clip on draw if necessary
                    outline.setRect(0, 0, view.width, view.height)
                }
            }
        }
    } else {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

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
