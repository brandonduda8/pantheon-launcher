package com.apexforge.godlauncher.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val TAU = 2f * PI.toFloat()

private val EmberOrange = Color(0xFFFF6D00)
private val DeepEmber = Color(0xFFBF360C)
private val PhoenixGold = Color(0xFFFFD54F)

private const val EMBER_COUNT = 80
private const val FEATHER_COUNT = 30

/** Wing sides, hoisted so the draw loop allocates nothing. */
private val WING_SIDES = floatArrayOf(-1f, 1f)

/**
 * Procedural phoenix drawn in Canvas: ember-orange/gold gradient body,
 * fanned wing feathers with an idle flap, swaying tail plumes, plus
 * rising ember particles and drifting feather particles.
 *
 * When [ignite] becomes true a one-shot radial ember burst plays, then
 * the phoenix settles into its idle loop.
 *
 * Performance: particle fields are pre-allocated arrays; draw positions
 * are pure functions of a seamless 20s loop phase (integer cycle counts,
 * so the loop point is invisible). The phase is read only in the draw
 * phase — no tickers, no recomposition. Paused when not RESUMED, static
 * frame when disabled or before the splash dismisses.
 */
@Composable
fun PhoenixFX(
    animationsEnabled: Boolean,
    ignite: Boolean,
    started: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // v7: animations stay parked until the activity is actually resumed.
    // Seeding from the live lifecycle state (not `true`) stops every
    // animated layer from running at full blast through cold start —
    // on software renderers each animated frame took seconds and
    // saturated the main thread, starving the resume transaction
    // (launch timeout, "never resumed" gate failure, v6 phone ANR).
    var visible by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            visible = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val animating = animationsEnabled && visible
    val running = animating && started
    // v6: seamless 0..1 loop phase over 20s, driven by the animation clock.
    // Read only inside Canvas.onDraw — the old 30fps delay(33) ticker that
    // recomposed the whole home screen is gone. (cycleState is a nullable
    // State so no trailing-lambda parsing ambiguity can attach to the
    // animateFloat call.)
    val cycleState: State<Float>? = if (running) {
        val driftTransition = rememberInfiniteTransition(label = "phoenixDrift")
        driftTransition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
            label = "driftCycle"
        )
    } else null
    val phaseOf: () -> Float = { cycleState?.value ?: 0f }

    // Wing-flap driver: 0..1..0 loop, read in the draw phase. Static
    // mid-pose when disabled.
    val flapState: State<Float>? = if (running) {
        val ft = rememberInfiniteTransition(label = "phoenixFlap")
        ft.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "flap"
        )
    } else null

    // One-shot ignition burst. v6: waits for the splash to dismiss — a
    // burst under the splash would be wasted work and a wasted moment.
    val burstScale = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }
    LaunchedEffect(ignite, running) {
        if (!ignite || !running) return@LaunchedEffect
        burstScale.snapTo(0f)
        burstAlpha.snapTo(1f)
        launch { burstScale.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        launch { burstAlpha.animateTo(0f, tween(1400)) }
    }

    // Unit feather petal (scaled/rotated per feather at draw time).
    val featherPath = remember {
        Path().apply {
            moveTo(0f, 0f)
            quadraticBezierTo(0.55f, -0.16f, 1f, 0f)
            quadraticBezierTo(0.55f, 0.16f, 0f, 0f)
            close()
        }
    }

    // Pre-allocated particle fields.
    val embers = remember {
        val rng = Random(1234)
        Array(EMBER_COUNT) {
            floatArrayOf(
                rng.nextFloat() * 0.14f - 0.07f, // x offset frac of w
                rng.nextFloat(),                 // rise offset 0..1
                rng.nextFloat() * 0.10f + 0.05f, // rise speed
                rng.nextFloat() * 2.6f + 1.2f,   // radius px
                rng.nextFloat() * 6.283f,        // phase
                rng.nextFloat() * 3f + 1.5f      // flicker freq
            )
        }
    }
    val feathers = remember {
        val rng = Random(99)
        Array(FEATHER_COUNT) {
            floatArrayOf(
                rng.nextFloat(),                 // x base frac
                rng.nextFloat(),                 // y offset
                rng.nextFloat() * 0.02f + 0.008f,// rise speed
                rng.nextFloat() * 0.03f + 0.01f, // drift speed
                rng.nextFloat() * 6.283f,        // spin phase
                rng.nextFloat() * 1.2f + 0.6f    // spin speed
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val u = minOf(w, h) / 100f
        val cx = w / 2f
        val bodyY = h * 0.155f
        // v6: seamless 20s loop phase; every motion below uses integer
        // cycle counts per period so the wrap point is invisible.
        // Static values identical to the old drift=0 frame when paused.
        val ph = phaseOf()
        val flap = flapState?.value ?: 0.5f
        val flapSin = sin(flap * PI.toFloat())

        // Ignition burst: expanding white-hot ring + flash.
        val bA = burstAlpha.value
        if (bA > 0.01f) {
            val bR = (0.15f + 0.85f * burstScale.value) * minOf(w, h) * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f * bA),
                        EmberOrange.copy(alpha = 0.55f * bA),
                        Color.Transparent
                    ),
                    center = Offset(cx, bodyY),
                    radius = bR
                ),
                radius = bR,
                center = Offset(cx, bodyY)
            )
            drawRect(Color.White.copy(alpha = 0.28f * bA))
        }

        // Tail plumes: fanning tapered lines with a sway.
        withTransform({
            rotate(degrees = if (running) 7f * sin(ph * TAU * 6f) else 0f, pivot = Offset(cx, bodyY + 20f))
        }) {
            for (i in -2..2) {
                val len = (30f - abs(i) * 5f) * u
                val ang = Math.toRadians((90.0 + i * 15.0)).toFloat()
                val ex = cx + kotlin.math.cos(ang) * len
                val ey = bodyY + 20f + kotlin.math.sin(ang) * len
                drawLine(
                    color = lerp(DeepEmber, PhoenixGold, 1f - abs(i) / 2.6f),
                    start = Offset(cx, bodyY + 18f),
                    end = Offset(ex, ey),
                    strokeWidth = (4.5f - abs(i) * 0.9f) * u,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // Wings: 5 fanned feathers per side, flapping.
        // Feathers read charred at the roots, flaring to ember-gold at the tips.
        for (side in WING_SIDES) {
            val shoulder = Offset(cx + side * 7f, bodyY - 6f)
            for (i in 0..4) {
                val len = (30f - i * 3.4f) * u
                val baseDeg = side * (16f + i * 15f)
                val flapDeg = side * flapSin * (9f + i * 2.6f)
                val tipShade = lerp(EmberOrange, PhoenixGold, i / 4f)
                withTransform({
                    translate(shoulder.x, shoulder.y)
                    rotate(baseDeg + flapDeg, Offset.Zero)
                }) {
                    // Charred root: full feather in dark char.
                    withTransform({
                        scale(len, len * 0.95f)
                    }) {
                        drawPath(featherPath, color = Color(0xFF2A1610).copy(alpha = 0.95f))
                    }
                    // Ember-gold tip: glowing outer length over the char.
                    withTransform({
                        translate(len * 0.35f, 0f)
                        scale(len * 0.65f, len * 0.60f)
                    }) {
                        drawPath(featherPath, color = tipShade.copy(alpha = 0.95f))
                    }
                }
            }
        }

        // Body: ember gradient oval.
        val bodyR = 13f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(PhoenixGold, EmberOrange, DeepEmber),
                center = Offset(cx, bodyY - 6f),
                radius = bodyR * 2.2f
            ),
            topLeft = Offset(cx - bodyR, bodyY - 22f),
            size = Size(bodyR * 2f, 44f)
        )

        // Head + beak + eye.
        val headC = Offset(cx, bodyY - 30f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PhoenixGold, EmberOrange),
                center = headC,
                radius = 8f
            ),
            radius = 7f,
            center = headC
        )
        drawLine(
            color = Color(0xFFFFF3C4),
            start = headC + Offset(0f, -1f),
            end = headC + Offset(0f, 4.5f),
            strokeWidth = 3.2f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(Color.White, radius = 1.7f, center = headC + Offset(2.6f, -2.4f))

        // Rising ember particles (spawn near tail, recycle at top).
        val span = h * 0.30f
        val baseY = bodyY + 40f
        for (e in embers) {
            val rise = (ph * ((e[2] * 20f).roundToInt().coerceAtLeast(1)) + e[1]) % 1f
            val yy = baseY - rise * span
            val xx = (0.5f + e[0]) * w + sin(ph * TAU * 5f + e[4]) * 12f
            val flick = 0.35f + 0.65f * abs(sin(ph * TAU * ((e[5] * 20f / TAU).roundToInt().coerceAtLeast(1)) + e[4]))
            drawCircle(
                color = EmberOrange.copy(alpha = 0.9f * flick * (1f - rise * 0.55f)),
                radius = e[3],
                center = Offset(xx, yy)
            )
        }

        // Drifting motes: even indices rise as ember-gold feather sparks,
        // odd indices fall as gray ash — ashes to embers, embers to fire.
        for ((idx, f) in feathers.withIndex()) {
            if (idx % 2 == 0) {
                val fx = (((f[0] + ph * 1.2f) % 1.2f) - 0.1f) * w
                val fyFrac = (((f[1] - ph * ((f[2] * 20f).roundToInt().coerceAtLeast(1))) % 1f) + 1f) % 1f
                val fy = (0.03f + fyFrac * 0.24f) * h
                withTransform({
                    translate(fx, fy)
                    rotate(degrees = ph * 360f * ((f[5] * 3.18f).roundToInt().coerceAtLeast(1)) + f[4] * 57.3f, pivot = Offset.Zero)
                }) {
                    drawOval(
                        color = PhoenixGold.copy(alpha = 0.5f),
                        topLeft = Offset(-3.4f * u, -1.2f * u),
                        size = Size(6.8f * u, 2.4f * u)
                    )
                }
            } else {
                val fx = ((((f[0] + ph * 1.2f) % 1.2f) - 0.1f) * w) +
                    sin(ph * TAU * 3f + f[4]) * 10f
                val fy = (0.02f + ((f[1] + ph) % 1f) * 0.30f) * h
                withTransform({
                    translate(fx, fy)
                    rotate(degrees = ph * 360f * ((f[5] * 1.59f).roundToInt().coerceAtLeast(1)) + f[4] * 57.3f, pivot = Offset.Zero)
                }) {
                    drawOval(
                        color = Color(0xFF8A8F9E).copy(alpha = 0.42f),
                        topLeft = Offset(-2.6f * u, -1.0f * u),
                        size = Size(5.2f * u, 2.0f * u)
                    )
                }
            }
        }
    }
}
