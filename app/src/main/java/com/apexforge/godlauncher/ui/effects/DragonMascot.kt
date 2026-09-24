package com.apexforge.godlauncher.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.model.DragonBehavior
import com.apexforge.godlauncher.model.DragonSkin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val TAU = 2f * PI.toFloat()

private data class DragonPalette(
    val body: Color,
    val dark: Color,
    val horn: Color,
    val belly: Color,
    val scar: Color
)

private fun DragonSkin.palette(): DragonPalette = when (this) {
    DragonSkin.VOID -> DragonPalette(
        Color(0xFF8B5CF6), Color(0xFF6D28D9),
        Color(0xFFFDE68A), Color(0xFFFEF3C7), Color(0xFF7C2D12)
    )
    DragonSkin.EMBER -> DragonPalette(
        Color(0xFFF97316), Color(0xFF9A3412),
        Color(0xFFFFF3C4), Color(0xFFFFE0B3), Color(0xFF7C2D12)
    )
    DragonSkin.QUANTUM -> DragonPalette(
        Color(0xFF22D3EE), Color(0xFF0E7490),
        Color(0xFFE0FBFF), Color(0xFFCFFAFE), Color(0xFF155E75)
    )
    DragonSkin.GOLD -> DragonPalette(
        Color(0xFFF5B942), Color(0xFFB45309),
        Color(0xFFFFF8E1), Color(0xFFFFF3C4), Color(0xFF92400E)
    )
}

private const val PUFF_COUNT = 18

/**
 * Spyro-style dragon mascot, fully procedural: purple body, cream belly
 * and horns, little wings. Sits near the bottom-right of the home screen.
 *
 * Idle: breathing (body scale pulse), blinking eyes, tail sway, gentle bob.
 * Tap: happy hop + a fire puff from the mouth.
 * Every ~25s: glides across the screen and back.
 *
 * Honors [animationsEnabled]: static pose, no loops, no tap effects when off.
 *
 * v6: idle drivers are infinite transitions read only in the draw phase
 * (zero recomposition), and fire-puff physics advance from real frame
 * time in the draw phase — the 30fps tick() ticker is gone.
 */
@Composable
fun DragonMascot(
    animationsEnabled: Boolean,
    skin: DragonSkin = DragonSkin.VOID,
    behavior: DragonBehavior = DragonBehavior.PLAYFUL,
    started: Boolean = true,
    modifier: Modifier = Modifier
) {
    val pal = skin.palette()
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            visible = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val animating = animationsEnabled && visible
    val running = animating && started

    // Screen + mascot size in px for the glide traverse.
    val screenWPx = with(density) { config.screenWidthDp.dp.toPx() }
    val dragonWPx = with(density) { 104.dp.toPx() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Idle drivers (static defaults when animations are off). States are
    // read only in the draw phase — no per-frame recomposition.
    // Behavior tunes the tempo: CALM is slow and never glides,
    // PLAYFUL is the classic, HYPER is all bounce.
    val breathMs = when (behavior) {
        DragonBehavior.CALM -> 4200
        DragonBehavior.HYPER -> 1400
        else -> 2400
    }
    val breathState: State<Float>? = if (running) {
        val bt = rememberInfiniteTransition(label = "dragonBreath")
        bt.animateFloat(
            1f, 1.035f,
            infiniteRepeatable(tween(breathMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath"
        )
    } else null

    val blinkMs = when (behavior) {
        DragonBehavior.CALM -> 6000
        DragonBehavior.HYPER -> 2600
        else -> 4800
    }
    val blinkState: State<Float>? = if (running) {
        val kt = rememberInfiniteTransition(label = "dragonBlink")
        kt.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(blinkMs, easing = LinearEasing), RepeatMode.Restart),
            label = "blink"
        )
    } else null

    val bobAmp = when (behavior) {
        DragonBehavior.CALM -> 2f
        DragonBehavior.HYPER -> 5.5f
        else -> 3.5f
    }
    val bobMs = when (behavior) {
        DragonBehavior.CALM -> 4200
        DragonBehavior.HYPER -> 1800
        else -> 3000
    }
    val bobState: State<Float>? = if (running) {
        val ot = rememberInfiniteTransition(label = "dragonBob")
        ot.animateFloat(
            -bobAmp, bobAmp,
            infiniteRepeatable(tween(bobMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bob"
        )
    } else null

    // Tail sway: one sway per ~3.14s, matching the old sin(tick * 2.0) tempo.
    val swayState: State<Float>? = if (running) {
        val st = rememberInfiniteTransition(label = "dragonSway")
        st.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3140, easing = LinearEasing), RepeatMode.Restart),
            label = "sway"
        )
    } else null

    // Hop + glide offsets.
    val hopY = remember { Animatable(0f) }
    val glideX = remember { Animatable(0f) }

    // Glide loop: right -> left -> back. CALM never glides.
    val glideDelayMs = when (behavior) {
        DragonBehavior.CALM -> 0L
        DragonBehavior.HYPER -> 8_000L
        else -> 25_000L
    }
    LaunchedEffect(running, screenWPx, glideDelayMs) {
        if (!running || glideDelayMs == 0L) return@LaunchedEffect
        while (true) {
            delay(glideDelayMs)
            glideX.animateTo(
                -(screenWPx - dragonWPx),
                animationSpec = tween(7000, easing = LinearEasing)
            )
            glideX.animateTo(0f, animationSpec = tween(7000, easing = FastOutSlowInEasing))
        }
    }

    // Fire-puff particles (pre-allocated). v6: physics advance in the draw
    // phase from real frame timestamps — no ticker coroutine.
    val puffLife = remember { FloatArray(PUFF_COUNT) }
    val puffPos = remember { Array(PUFF_COUNT) { FloatArray(2) } }
    val puffVel = remember { Array(PUFF_COUNT) { FloatArray(2) } }
    val lastFrameNs = remember { longArrayOf(0L) }

    fun onTap() {
        if (!animating) return
        // Happy hop.
        scope.launch {
            hopY.animateTo(-with(density) { 18.dp.toPx() }, tween(200, easing = FastOutSlowInEasing))
            hopY.animateTo(0f, spring(dampingRatio = 0.35f))
        }
        // Fire puff from the mouth (mouth ~ (0.82w, 0.42h) of canvas).
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w > 0f) {
            val rng = Random.Default
            for (i in 0 until PUFF_COUNT) {
                puffLife[i] = 0.7f + rng.nextFloat() * 0.3f
                puffPos[i][0] = w * 0.82f
                puffPos[i][1] = h * 0.42f
                puffVel[i][0] = w * (0.15f + rng.nextFloat() * 0.55f)
                puffVel[i][1] = -h * (0.25f + rng.nextFloat() * 0.65f)
            }
        }
    }

    // Pre-built unit shapes (no Path allocation in the draw loop).
    val wingPath = remember {
        Path().apply {
            moveTo(0f, 0f)
            quadraticBezierTo(0.28f, -0.55f, 0.72f, -0.58f)
            quadraticBezierTo(0.55f, -0.38f, 0.62f, -0.12f)
            quadraticBezierTo(0.36f, -0.18f, 0.30f, 0.04f)
            close()
        }
    }
    val spikePath = remember {
        Path().apply {
            moveTo(0f, 0f)
            lineTo(1f, 0f)
            lineTo(0.5f, -1f)
            close()
        }
    }
    // Tail curve control points in local units (scaled by s at draw time).
    val tailFracs = remember {
        listOf(Offset(30f, 76f), Offset(18f, 84f), Offset(9f, 78f), Offset(6f, 66f))
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .clickable { onTap() }
    ) {
        // v6: idle drivers are read here, in the draw phase — the animation
        // clock invalidates draws directly, never recomposing. Puff physics
        // advance from real frame timestamps, also with no ticker.
        val breath = breathState?.value ?: 1f
        val blink = blinkState?.value ?: 1f
        val eyeSY = if (blink < 0.07f) 0.12f + 0.88f * (blink / 0.07f) else 1f
        val bobPx = with(density) { (bobState?.value ?: 0f).dp.toPx() }
        val sway = swayState?.value ?: 0f

        val nowNs = System.nanoTime()
        val dt = if (lastFrameNs[0] == 0L) 0f
        else ((nowNs - lastFrameNs[0]) / 1_000_000_000f).coerceAtMost(0.1f)
        lastFrameNs[0] = nowNs
        if (running && dt > 0f) {
            for (i in 0 until PUFF_COUNT) {
                if (puffLife[i] > 0f) {
                    puffLife[i] = (puffLife[i] - dt * 1.5f).coerceAtLeast(0f)
                    puffPos[i][0] += puffVel[i][0] * dt
                    puffPos[i][1] += puffVel[i][1] * dt
                    puffVel[i][1] -= 0.35f * dt // hot air rises
                }
            }
        }

        val w = size.width
        val h = size.height
        val s = w / 120f // local unit

        withTransform({
            translate(left = glideX.value, top = bobPx + hopY.value)
        }) {
            val cx = w / 2f
            val bodyC = Offset(cx - 4f * s, 66f * s)

            // Tail: curved segments with sway (drawn behind body).
            withTransform({
                rotate(
                    degrees = if (running) 12f * sin(sway * TAU) else 0f,
                    pivot = Offset(30f * s, 76f * s)
                )
            }) {
                for (i in 0 until tailFracs.size - 1) {
                    drawLine(
                        color = pal.body,
                        start = tailFracs[i] * s,
                        end = tailFracs[i + 1] * s,
                        strokeWidth = (9f - i * 1.6f) * s,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                // Tail tip spike.
                withTransform({
                    translate(6f * s, 66f * s)
                    rotate(-35f, Offset.Zero)
                    scale(7f * s, 7f * s)
                }) {
                    drawPath(spikePath, color = pal.horn)
                }
            }

            // Far wing.
            withTransform({
                translate(44f * s, 52f * s)
                rotate(-18f - (breath - 1f) * 400f, Offset.Zero)
                scale(34f * s, 34f * s)
            }) {
                drawPath(wingPath, color = pal.dark.copy(alpha = 0.85f))
            }

            // Body (breathing scale).
            withTransform({
                scale(breath, breath, pivot = bodyC)
            }) {
                drawOval(
                    color = pal.body,
                    topLeft = Offset(28f * s, 46f * s),
                    size = Size(56f * s, 44f * s)
                )
                // Belly.
                drawOval(
                    color = pal.belly,
                    topLeft = Offset(50f * s, 60f * s),
                    size = Size(26f * s, 26f * s)
                )
                // Feet.
                drawRoundRect(
                    color = pal.dark,
                    topLeft = Offset(36f * s, 84f * s),
                    size = Size(15f * s, 9f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
                )
                drawRoundRect(
                    color = pal.dark,
                    topLeft = Offset(60f * s, 84f * s),
                    size = Size(15f * s, 9f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
                )
            }

            // Back spikes.
            for (i in 0..2) {
                withTransform({
                    translate((40f + i * 12f) * s, 48f * s)
                    rotate(-12f, Offset.Zero)
                    scale(6f * s, 8f * s)
                }) {
                    drawPath(spikePath, color = pal.horn)
                }
            }

            // Near wing (over body).
            withTransform({
                translate(52f * s, 50f * s)
                rotate(-8f - (breath - 1f) * 400f, Offset.Zero)
                scale(40f * s, 40f * s)
            }) {
                drawPath(wingPath, color = pal.body)
            }

            // Head + snout.
            val headC = Offset(80f * s, 42f * s)
            drawCircle(color = pal.body, radius = 16f * s, center = headC)
            drawOval(
                color = pal.body,
                topLeft = Offset(80f * s, 42f * s),
                size = Size(28f * s, 17f * s)
            )
            // Smile.
            drawArc(
                color = pal.dark,
                startAngle = 20f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = Offset(86f * s, 46f * s),
                size = Size(12f * s, 8f * s),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f * s)
            )

            // Horns: the left horn is nicked — shorter and canted,
            // a scar from an old battle. The right horn stands intact.
            withTransform({
                translate(72f * s, 30f * s)
                rotate(-26f, Offset.Zero)
                scale(6f * s, 7f * s)
            }) {
                drawPath(spikePath, color = pal.horn)
            }
            withTransform({
                translate(82f * s, 30f * s)
                rotate(-8f, Offset.Zero)
                scale(6f * s, 11f * s)
            }) {
                drawPath(spikePath, color = pal.horn)
            }

            // Scar ridge over the eye: two jagged dark segments.
            val scarColor = pal.scar
            val scarW = 2.2f * s
            val scarCap = androidx.compose.ui.graphics.StrokeCap.Round
            drawLine(scarColor, Offset(81f * s, 30.5f * s), Offset(87f * s, 32f * s), scarW, scarCap)
            drawLine(scarColor, Offset(87f * s, 32f * s), Offset(93f * s, 30f * s), scarW, scarCap)

            // Warm, loyal eyes: amber iris, dark pupil, catchlight.
            // The whole eye squashes vertically on blink.
            val eyeC = Offset(88f * s, 38f * s)
            withTransform({
                scale(1f, eyeSY, pivot = eyeC)
            }) {
                drawOval(
                    color = Color(0xFFFFF6E0),
                    topLeft = Offset(eyeC.x - 5f * s, eyeC.y - 5f * s),
                    size = Size(10f * s, 10f * s)
                )
                if (eyeSY > 0.4f) {
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 3.4f * s,
                        center = eyeC
                    )
                    drawCircle(
                        color = Color(0xFF1E1B2E),
                        radius = 1.7f * s,
                        center = eyeC + Offset(0.8f * s, 0f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 0.9f * s,
                        center = eyeC + Offset(-1f * s, -1.2f * s)
                    )
                }
            }

            // Fire puff particles from the mouth.
            val scaleF = w / 360f
            for (i in 0 until PUFF_COUNT) {
                val life = puffLife[i]
                if (life > 0f) {
                    val grow = 1f - life
                    drawCircle(
                        color = androidx.compose.ui.graphics.lerp(
                            Color(0xFFFFF3C4), Color(0xFFBF360C), grow
                        ).copy(alpha = life.coerceAtMost(1f)),
                        radius = (2.5f + grow * 8f) * scaleF,
                        center = Offset(puffPos[i][0], puffPos[i][1])
                    )
                }
            }
        }
    }
}
