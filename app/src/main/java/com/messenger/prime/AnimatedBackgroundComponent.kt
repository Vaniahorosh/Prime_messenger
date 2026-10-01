package com.messenger.prime

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.platform.LocalContext
import android.app.ActivityManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Models & Global State ---

data class LavaShapeInstance(
    val id: Int,
    var currentPolygon: RoundedPolygon,
    var targetPolygon: RoundedPolygon,
    val size: Dp,
    val startX: Float, // Normalized 0..1
    val startY: Float, // Normalized 0..1
    val lissajousParams: LissajousParams,
    val morphDuration: Int,
    var lastMorphTime: Long,
    val rotationSpeed: Float,
    val colorPalette: List<Color>
)

data class LissajousParams(
    val a1: Float, val w1: Float, val p1: Float,
    val a2: Float, val w2: Float,
    val b1: Float, val w3: Float, val p2: Float,
    val b2: Float, val w4: Float
)

object LavaBackgroundState {
    var shapes by mutableStateOf<List<LavaShapeInstance>?>(null)
    var startTime: Long = SystemClock.uptimeMillis()
    var pausedTimeOffset: Long = 0L
    private var isTransitioning = false
    private var lastDarkTheme: Boolean? = null

    fun initIfNeeded(darkTheme: Boolean) {
        if (shapes != null && lastDarkTheme == darkTheme) return
        
        if (shapes == null) {
            shapes = List(5) { i ->
                val startPoly = createRandomPolygon()
                LavaShapeInstance(
                    id = i,
                    currentPolygon = startPoly,
                    targetPolygon = createRandomPolygon(),
                    size = (220..420).random().dp,
                    startX = Random.nextFloat(),
                    startY = Random.nextFloat(),
                    lissajousParams = generateLissajousParams(),
                    morphDuration = (4000..7000).random(),
                    lastMorphTime = startTime,
                    rotationSpeed = (0.008f + Random.nextFloat() * 0.015f) * (if (Random.nextBoolean()) 1 else -1),
                    colorPalette = if (darkTheme) getDarkPalette() else getLightPalette()
                )
            }
        } else if (lastDarkTheme != darkTheme) {
            shapes = shapes?.map { shape ->
                shape.copy(colorPalette = if (darkTheme) getDarkPalette() else getLightPalette())
            }
        }
        lastDarkTheme = darkTheme
    }

    fun onTransitionStart() {
        if (!isTransitioning) {
            pausedTimeOffset = SystemClock.uptimeMillis() - startTime
            isTransitioning = true
        }
    }

    fun onActivityResumed() {
        if (isTransitioning) {
            startTime = SystemClock.uptimeMillis() - pausedTimeOffset
            isTransitioning = false
        }
    }
}

// --- Generation Utilities ---

private fun generateLissajousParams() = LissajousParams(
    a1 = 150f + Random.nextFloat() * 100f,
    w1 = 0.0004f + Random.nextFloat() * 0.0004f,
    p1 = Random.nextFloat() * PI.toFloat(),
    a2 = 50f + Random.nextFloat() * 50f,
    w2 = 0.0008f + Random.nextFloat() * 0.0006f,
    b1 = 150f + Random.nextFloat() * 100f,
    w3 = 0.0003f + Random.nextFloat() * 0.0005f,
    p2 = Random.nextFloat() * PI.toFloat(),
    b2 = 50f + Random.nextFloat() * 50f,
    w4 = 0.0007f + Random.nextFloat() * 0.0007f
)

private fun createRandomPolygon(): RoundedPolygon {
    return when (Random.nextInt(3)) {
        0 -> RoundedPolygon( // Cookie/Scalloped
            numVertices = (6..10).random(),
            rounding = CornerRounding(0.4f)
        )
        1 -> RoundedPolygon.star( // Flower/Star
            numVerticesPerRadius = (4..6).random(),
            innerRadius = 0.4f,
            rounding = CornerRounding(0.3f)
        )
        else -> RoundedPolygon( // Blob
            numVertices = (3..5).random(),
            rounding = CornerRounding(0.8f)
        )
    }
}

private fun getLightPalette(): List<Color> {
    val randomChoice = Random.nextInt(3)
    return when (randomChoice) {
        0 -> listOf(
            Color(0xFF29B6F6).copy(alpha = 0.50f), // Sky Cyan
            Color(0xFF1E88E5).copy(alpha = 0.40f), // Vibrant Prime Blue
            Color.Transparent
        )
        1 -> listOf(
            Color(0xFF00E5FF).copy(alpha = 0.40f), // Aqua Cyan
            Color(0xFF1565C0).copy(alpha = 0.45f), // Deep Navy Accent
            Color.Transparent
        )
        else -> listOf(
            Color(0xFF42A5F5).copy(alpha = 0.48f), // Soft Electric Blue
            Color(0xFF0097A7).copy(alpha = 0.38f), // Deep Teal Accent
            Color.Transparent
        )
    }
}

private fun getDarkPalette(): List<Color> {
    val randomChoice = Random.nextInt(3)
    return when (randomChoice) {
        0 -> listOf(
            Color(0xFF1976D2).copy(alpha = 0.48f), // Deep Royal Blue
            Color(0xFF00B0FF).copy(alpha = 0.38f), // Neon Cyan
            Color.Transparent
        )
        1 -> listOf(
            Color(0xFF00E5FF).copy(alpha = 0.35f), // Glowing Aqua
            Color(0xFF3F51B5).copy(alpha = 0.45f), // Deep Indigo
            Color.Transparent
        )
        else -> listOf(
            Color(0xFF2979FF).copy(alpha = 0.42f), // Electric Accent Blue
            Color(0xFF00838F).copy(alpha = 0.38f), // Deep Cyber Teal
            Color.Transparent
        )
    }
}

// --- Components ---

@Composable
fun PrimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accentColorInt = ColorAccentManager.getCurrentAccentColor(context)
    val accentColor = Color(accentColorInt)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            background = Color(0xFF1E293B),
            surface = Color(0xFF334155)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = accentColor,
            background = accentColor,
            surface = Color(0xFFF1F5F9)
        )
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun AnimatedBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    ignoreSettingsToggle: Boolean = true // By default, ignore toggle (for Entry screens)
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
    val isEnabled = sharedPrefs.getBoolean("settings_lava_bg", true)
    
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRam = activityManager?.isLowRamDevice == true
    
    val accentColorInt = ColorAccentManager.getCurrentAccentColor(context)
    val bgColor = if (darkTheme) Color(0xFF1E293B) else Color(accentColorInt)
    
    // If not ignoring toggle AND setting is off, OR if it's a weak device -> show static background
    if ((!ignoreSettingsToggle && !isEnabled) || isLowRam) {
        Box(modifier = modifier.fillMaxSize().background(bgColor))
        return
    }
    
    LavaBackgroundState.initIfNeeded(darkTheme)
    val shapes = LavaBackgroundState.shapes ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .blur(20.dp)
    ) {
        shapes.forEach { shape ->
            LavaShape(shape)
        }
        NoiseOverlay()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    BlurView(
        modifier = modifier,
        blurRadius = blurRadius,
        tint = if (darkTheme) {
            Color(0xFF1E293B).copy(alpha = 0.6f)
        } else {
            Color(0xFF154B87).copy(alpha = 0.6f)
        },
        shape = RoundedCornerShape(32.dp)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
private fun LavaShape(instance: LavaShapeInstance) {
    val infiniteTransition = rememberInfiniteTransition(label = "lava")
    
    // Ticker for frame updates
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "ticker"
    )

    // Using side-effect to handle multi-step morphing logic
    val currentTime = SystemClock.uptimeMillis()
    val totalElapsed = currentTime - LavaBackgroundState.startTime
    
    // Update morphing targets
    LaunchedEffect(elapsed) {
        val morphElapsed = SystemClock.uptimeMillis() - instance.lastMorphTime
        if (morphElapsed >= instance.morphDuration) {
            instance.currentPolygon = instance.targetPolygon
            instance.targetPolygon = createRandomPolygon()
            instance.lastMorphTime = SystemClock.uptimeMillis()
        }
    }

    val morphProgress = ((SystemClock.uptimeMillis() - instance.lastMorphTime).toFloat() / instance.morphDuration)
        .coerceIn(0f, 1f)
    // Applying FastOutSlowInEasing manually for the morph
    val easedMorphProgress = FastOutSlowInEasing.transform(morphProgress)
    
    val morph = remember(instance.currentPolygon, instance.targetPolygon) {
        Morph(instance.currentPolygon, instance.targetPolygon)
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val t = totalElapsed.toFloat()
        val p = instance.lissajousParams
        
        // Lissajous Orbit Calculation
        val xDrift = p.a1 * sin(p.w1 * t + p.p1) + p.a2 * cos(p.w2 * t)
        val yDrift = p.b1 * cos(p.w3 * t + p.p2) + p.b2 * sin(p.w4 * t)
        
        val xPos = instance.startX * size.width + xDrift
        val yPos = instance.startY * size.height + yDrift
        
        // Scale Pulse
        val scale = 0.85f + 0.3f * ((sin(t * 0.0005f) + 1f) / 2f)
        
        // Rotation
        val rotation = (t * instance.rotationSpeed) % 360f

        translate(xPos, yPos) {
            rotate(rotation) {
                val sizePx = instance.size.toPx() * scale
                val path = morph.toComposePath(easedMorphProgress, sizePx)
                
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = instance.colorPalette,
                        center = Offset.Zero,
                        radius = sizePx / 1.5f
                    )
                )
            }
        }
    }
}

@Composable
private fun NoiseOverlay() {
    // Generate static noise points once
    val points = remember {
        List(150) { Offset(Random.nextFloat(), Random.nextFloat()) }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "noise")
    val noiseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(noiseAlpha)) {
        points.forEach { point ->
            drawCircle(
                color = Color.White,
                radius = 1.5f,
                center = Offset(point.x * size.width, point.y * size.height)
            )
        }
    }
}

// --- Extension ---

fun Morph.toComposePath(progress: Float, size: Float): Path {
    val p = android.graphics.Path()
    val features = this.asCubics(progress)
    var isFirst = true
    features.forEach { cubic ->
        if (isFirst) {
            p.moveTo(cubic.anchor0X * size, cubic.anchor0Y * size)
            isFirst = false
        }
        p.cubicTo(
            cubic.control0X * size, cubic.control0Y * size,
            cubic.control1X * size, cubic.control1Y * size,
            cubic.anchor1X * size, cubic.anchor1Y * size
        )
    }
    p.close()
    
    val bounds = android.graphics.RectF()
    p.computeBounds(bounds, true)
    p.offset(-bounds.centerX(), -bounds.centerY())
    return p.asComposePath()
}
