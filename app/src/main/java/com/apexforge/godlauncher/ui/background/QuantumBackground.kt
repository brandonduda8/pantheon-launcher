package com.apexforge.godlauncher.ui.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.ui.theme.NebulaMagenta
import com.apexforge.godlauncher.ui.theme.NebulaTeal
import com.apexforge.godlauncher.ui.theme.NebulaViolet
import com.apexforge.godlauncher.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float
)

private data class Blob(
    val color: Color,
    val baseX: Float, val baseY: Float,
    val radiusFrac: Float,
    val xAmp: Float, val yAmp: Float,
    val xFreq: Float, val yFreq: Float,
    /** 1.0 = full ember accent; lower = faint background depth. */
    val intensity: Float
)

private data class Mote(
    val x: Float, val yOff: Float, val r: Float,
    val speed: Float, val phase: Float, val sway: Float,
    /** true = gray ash, false = glowing ember */
    val ash: Boolean
)

private data class AuroraBand(
    val color: Color,
    val xFrac: Float,
    val widthFrac: Float,
    val speed: Float,
    val phase: Float
)

/**
 * Living deep-space background (v2): a charcoal-cinematic sky — drifting
 * nebula blobs anchored in ember orange/gold with violet/teal/magenta kept
 * only as faint secondary depth, an ember glow rising from gray ash and
 * embers along the bottom, two slow aurora bands, and a twinkling starfield.
 * Ashes to embers, embers to fire.
 *
 * Battery-sane by design:
 *  - ~30fps tick (33ms delay), not 60
 *  - animation coroutine only runs while the activity is RESUMED
 *  - when [animationsEnabled] is off, one static frame is drawn
 *  - particle fields are pre-allocated; the draw loop only mutates
 *    nothing (positions are pure functions of t)
 */
@Composable
fun QuantumBackground(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            visible = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var t by remember { mutableFloatStateOf(0f) }
    val animating = animationsEnabled && visible
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = System.nanoTime()
        while (true) {
            delay(33)
            val now = System.nanoTime()
            t += (now - last) / 1_000_000_000f
            last = now
        }
    }

    val stars = remember {
        val rng = Random(42)
        List(120) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = rng.nextFloat() * 2.2f + 0.6f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                speed = rng.nextFloat() * 1.5f + 0.4f
            )
        }
    }
    val blobs = remember {
        listOf(
            // Violet/teal/magenta kept only as faint secondary depth.
            Blob(NebulaViolet, 0.22f, 0.28f, 0.55f, 0.10f, 0.08f, 0.11f, 0.13f, 0.45f),
            Blob(NebulaTeal, 0.80f, 0.62f, 0.50f, 0.09f, 0.10f, 0.09f, 0.12f, 0.40f),
            Blob(NebulaMagenta, 0.55f, 0.85f, 0.45f, 0.12f, 0.07f, 0.14f, 0.10f, 0.45f),
            // Ember heart of the palette: low, rising from the ashes.
            Blob(Color(0xFFEA580C), 0.50f, 0.88f, 0.52f, 0.10f, 0.06f, 0.10f, 0.15f, 1.0f)
        )
    }
    val auroras = remember {
        listOf(
            AuroraBand(NebulaTeal, 0.30f, 0.16f, 0.021f, 0.0f),
            AuroraBand(NebulaMagenta, 0.68f, 0.12f, 0.016f, 2.1f)
        )
    }
    val motes = remember {
        val rng = Random(7)
        List(44) {
            Mote(
                x = rng.nextFloat(),
                yOff = rng.nextFloat(),
                r = rng.nextFloat() * 1.8f + 0.7f,
                speed = rng.nextFloat() * 0.030f + 0.012f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                sway = rng.nextFloat() * 0.03f + 0.008f,
                ash = rng.nextFloat() < 0.55f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(VoidBlack)
        val w = size.width
        val h = size.height
        val drift = if (animating) t else 0f
        val twoPi = 2f * PI.toFloat()

        // Flowing nebula blobs. Ember burns full; violet/teal/magenta are
        // faint secondary depth so the scene reads charcoal-cinematic.
        for (b in blobs) {
            val cx = (b.baseX + b.xAmp * sin(drift * b.xFreq * twoPi)) * w
            val cy = (b.baseY + b.yAmp * cos(drift * b.yFreq * twoPi)) * h
            val radius = b.radiusFrac * maxOf(w, h)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        b.color.copy(alpha = 0.30f * b.intensity),
                        b.color.copy(alpha = 0.09f * b.intensity),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        // Ember glow rising from the ashes along the bottom.
        val glowPulse = if (animating) 0.085f + 0.035f * sin(drift * 0.8f) else 0.10f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFEA580C).copy(alpha = glowPulse)
                ),
                startY = h * 0.55f,
                endY = h
            )
        )

        // Aurora bands sweeping diagonally across the sky.
        rotate(degrees = -24f, pivot = Offset(w / 2f, h / 2f)) {
            for (a in auroras) {
                val bandX = (a.xFrac + a.speed * drift + a.phase * 0.01f) % 1.4f - 0.2f
                val cx = bandX * w
                val halfW = a.widthFrac * w
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            a.color.copy(alpha = 0.10f),
                            a.color.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startX = cx - halfW,
                        endX = cx + halfW
                    ),
                    topLeft = Offset(cx - halfW, -h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(halfW * 2f, h * 1.4f)
                )
            }
        }

        // Rising ash and embers along the bottom: gray ash drifts up with
        // the glowing embers — ashes to embers, embers to fire.
        for (m in motes) {
            val riseFrac = (drift * m.speed + m.yOff) % 1f
            val my = h * (1f - riseFrac * 0.45f)
            val mx = (m.x + m.sway * sin(drift * 0.9f + m.phase)) * w
            val fade = 1f - riseFrac
            drawCircle(
                color = if (m.ash) {
                    Color(0xFF9AA3B2).copy(alpha = 0.26f * fade)
                } else {
                    Color(0xFFFFB066).copy(alpha = 0.35f * fade)
                },
                radius = m.r,
                center = Offset(mx, my)
            )
        }

        // Twinkling starfield.
        for (s in stars) {
            val twinkle = if (animating) {
                0.35f + 0.65f * abs(sin(drift * s.speed + s.phase))
            } else {
                0.7f
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.75f * twinkle),
                radius = s.r,
                center = Offset(s.x * w, s.y * h)
            )
        }
    }
}
