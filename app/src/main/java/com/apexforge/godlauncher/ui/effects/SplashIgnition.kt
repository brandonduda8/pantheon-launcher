package com.apexforge.godlauncher.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Smoothstep helper: eases x from 0 to 1 across [e0, e1]. */
private fun ss(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Cold-start ignition: RISING FROM ASHES.
 *
 * Beats, driven by a single 0→1 phase over ~2.6s:
 *  1. Near-black with drifting gray ash.
 *  2. A cracked ember heart/core pulses into view — the broken heart,
 *     still burning.
 *  3. The heart breaks open (white-hot flash).
 *  4. The phoenix burst: expanding ember shockwave, rising embers,
 *     "PANTHEON" fading in.
 *
 * Auto-dismisses via [onFinished]. No tap needed.
 */
@Composable
fun SplashIgnition(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var finished by remember { mutableStateOf(false) }
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        phase.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        delay(2600)
        finished = true
        onFinished()
    }

    // Pre-allocated fields; everything is a pure function of phase.
    val ash = remember {
        val rng = Random(77)
        Array(46) {
            floatArrayOf(
                rng.nextFloat(),                 // x frac
                rng.nextFloat(),                 // y offset
                rng.nextFloat() * 0.25f + 0.10f, // drift speed factor
                rng.nextFloat() * 2.4f + 1.0f,   // radius px
                rng.nextFloat() * 6.283f         // sway phase
            )
        }
    }
    val embers = remember {
        val rng = Random(2026)
        Array(70) {
            floatArrayOf(
                rng.nextFloat() * 0.5f - 0.25f, // x spread frac of w
                rng.nextFloat(),                // rise offset
                rng.nextFloat() * 0.5f + 0.5f,   // rise speed factor
                rng.nextFloat() * 3f + 1.5f,    // radius px
                rng.nextFloat() * 6.283f        // flicker phase
            )
        }
    }
    // Jagged crack polylines across the heart — pre-built unit paths,
    // scaled into place at draw time (no Path allocation in the draw loop).
    val crackPaths = remember {
        listOf(
            listOf(Offset(0f, -0.9f), Offset(0.15f, -0.4f), Offset(-0.1f, 0.1f), Offset(0.1f, 0.7f)),
            listOf(Offset(-0.7f, -0.2f), Offset(-0.3f, -0.05f), Offset(-0.45f, 0.4f)),
            listOf(Offset(0.7f, -0.3f), Offset(0.35f, 0.0f), Offset(0.5f, 0.5f))
        ).map { pts ->
            Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y)
            }
        }
    }

    if (finished) return
    val p = phase.value

    // Beat envelopes.
    val ashAlpha = 1f - ss(0.28f, 0.55f, p)
    val heartAlpha = ss(0.12f, 0.28f, p) * (1f - ss(0.50f, 0.60f, p))
    val heartPulse = 1f + 0.09f * sin(p * 46f) * heartAlpha
    val breakFlash = (1f - abs(p - 0.56f) / 0.07f).coerceIn(0f, 1f)
    val burst = ss(0.54f, 1f, p)
    val titleAlpha = ss(0.64f, 0.82f, p)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030304))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.44f
            val u = minOf(w, h) / 100f

            // Beat 1: drifting gray ash on near-black.
            if (ashAlpha > 0.01f) {
                for (a in ash) {
                    val ax = ((a[0] + p * a[2] * 0.35f) % 1.2f - 0.1f) * w +
                        sin(p * 20f + a[4]) * 10f
                    val ay = ((a[1] + p * a[2] * 0.5f) % 1f) * h
                    drawCircle(
                        color = Color(0xFF8A8F9E).copy(alpha = 0.5f * ashAlpha),
                        radius = a[3],
                        center = Offset(ax, ay)
                    )
                }
            }

            // Beat 2: the cracked ember heart — broken, still burning.
            if (heartAlpha > 0.01f) {
                val hr = 11f * u * heartPulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD54F).copy(alpha = 0.95f * heartAlpha),
                            Color(0xFFFF6D00).copy(alpha = 0.8f * heartAlpha),
                            Color(0xFF7C2D12).copy(alpha = 0.35f * heartAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = hr * 2.4f
                    ),
                    radius = hr * 2.4f,
                    center = Offset(cx, cy)
                )
                // Charred shell with glowing cracks.
                drawCircle(
                    color = Color(0xFF1A0E08).copy(alpha = 0.9f * heartAlpha),
                    radius = hr,
                    center = Offset(cx, cy)
                )
                for (crackPath in crackPaths) {
                    withTransform({
                        translate(cx, cy)
                        scale(hr, hr)
                    }) {
                        drawPath(
                            path = crackPath,
                            color = Color(0xFFFFB066).copy(alpha = 0.95f * heartAlpha),
                            // Stroke is inside the scaled transform: 0.24 unit ≈ 2.6f*u px.
                            style = Stroke(width = 0.24f)
                        )
                    }
                }
            }

            // Beat 3: the heart breaks open — white-hot flash.
            if (breakFlash > 0.01f) {
                drawRect(Color.White.copy(alpha = 0.75f * breakFlash))
                val flashR = (0.1f + 0.5f * breakFlash) * minOf(w, h)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f * breakFlash),
                            Color(0xFFFFD54F).copy(alpha = 0.5f * breakFlash),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = flashR
                    ),
                    radius = flashR,
                    center = Offset(cx, cy)
                )
            }

            // Beat 4: phoenix burst — expanding ember shockwave + rising embers.
            if (burst > 0.01f) {
                val coreR = (0.05f + 0.55f * burst) * minOf(w, h)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f * (1f - burst * 0.6f)),
                            Color(0xFFFFD54F).copy(alpha = 0.8f * (1f - burst * 0.45f)),
                            Color(0xFFFF6D00).copy(alpha = 0.5f * (1f - burst * 0.5f)),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = coreR
                    ),
                    radius = coreR,
                    center = Offset(cx, cy)
                )
                val ringR = (0.08f + 0.9f * burst) * minOf(w, h)
                drawCircle(
                    color = Color(0xFFFFB066).copy(alpha = 0.65f * (1f - burst)),
                    radius = ringR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 6f * (1f - burst) + 2f)
                )
                for (e in embers) {
                    val rise = (e[1] + burst * e[2]) % 1f
                    val ey = cy + 40f - rise * h * 0.45f
                    val ex = cx + e[0] * w + sin(burst * 9f + e[4]) * 14f
                    val flick = 0.4f + 0.6f * abs(sin(burst * 12f + e[4]))
                    drawCircle(
                        color = Color(0xFFFF8A3D).copy(alpha = 0.85f * flick * (1f - rise * 0.6f)),
                        radius = e[3],
                        center = Offset(ex, ey)
                    )
                }
            }

            // Scattered ash still falling through the rebirth.
            if (burst > 0.05f) {
                for (a in ash) {
                    val ax = ((a[0] + burst * 0.2f) % 1.2f - 0.1f) * w
                    val ay = ((a[1] + burst * a[2]) % 1f) * h
                    drawCircle(
                        color = Color(0xFF8A8F9E).copy(alpha = 0.35f * (1f - burst * 0.5f)),
                        radius = a[3] * 0.8f,
                        center = Offset(ax, ay)
                    )
                }
            }
        }

        Text(
            text = "PANTHEON",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 10.sp,
                fontSize = 34.sp
            ),
            color = Color(0xFFFFD54F).copy(alpha = titleAlpha),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
