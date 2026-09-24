package com.apexforge.godlauncher.ui.background

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.model.LoopId
import com.apexforge.godlauncher.model.ModBrand
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.model.ParticleType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PERIOD = 20f // seconds per seamless loop
private val TAU = 2f * PI.toFloat()

private data class Seed(val x: Float, val y: Float, val r: Float, val p1: Float, val p2: Float)

private fun hexColor(hex: String, fallback: Color): Color = try {
    val h = hex.trim().removePrefix("#")
    val full = when (h.length) {
        6 -> "FF$h"
        8 -> h
        else -> return fallback
    }
    Color(full.toLong(16))
} catch (_: Exception) {
    fallback
}

private fun shiftHue(color: Color, degrees: Float): Color {
    if (degrees == 0f) return color
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255f).toInt(),
        (color.green * 255f).toInt(),
        (color.blue * 255f).toInt(),
        hsv
    )
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(android.graphics.Color.HSVToColor((color.alpha * 255f).toInt(), hsv))
}

/**
 * v3 infinite loop backgrounds: three seamless PROCEDURAL loops plus a
 * static gradient — no video files, infinite, tiny, interactive.
 *
 * Seamlessness: every moving thing is a pure function of
 * phase = (t / PERIOD) mod 1, using only integer cycle counts per period,
 * so the loop point is mathematically invisible.
 *
 * Battery-sane: the loop phase is driven by an infinite transition whose
 * value is read only inside the draw phase — zero recomposition, the
 * animation runs on the render thread. The transition is not even created
 * until [started] (splash dismissed), so cold start never fights animation
 * tickers while ART is still verifying classes. Paused when not RESUMED;
 * a single static frame at a fixed phase is drawn when disabled.
 */
@Composable
fun LoopBackground(
    config: PantheonConfig,
    animationsEnabled: Boolean,
    started: Boolean = true,
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

    val animating = animationsEnabled && visible
    val running = animating && started
    // v6: phase 0..1 over one seamless PERIOD, derived from the animation
    // clock. The state is read only inside Canvas.onDraw (draw phase), so
    // frames never recompose — the old 30fps while(true){delay(33)} ticker
    // starved the main thread on low-end devices and the app never loaded.
    val phaseOf: () -> Float = if (running) {
        val loopTransition = rememberInfiniteTransition(label = "loopBg")
        val cycle = loopTransition.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween((PERIOD * 1000).toInt(), easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "loopCycle"
        )
        val speed = config.loopSpeed
        { (((cycle.value * speed) % 1f) + 1f) % 1f }
    } else {
        // Static frame when paused: the same fixed, good-looking phase as before.
        { 0.22f }
    }

    val bg = hexColor(config.colorBackground, Color(0xFF0B0A09))
    val accent = shiftHue(hexColor(config.colorAccent, Color(0xFFE25822)), config.hueShift)
    val godRing = shiftHue(hexColor(config.colorGodRing, Color(0xFFF5B942)), config.hueShift)
    val particle = shiftHue(hexColor(config.particleColor, Color(0xFFE25822)), config.hueShift)
    val dimBg = bg.copy(red = bg.red * 0.25f, green = bg.green * 0.25f, blue = bg.blue * 0.25f)

    // v6: background gradients are pure functions of the theme colors —
    // build them once per theme instead of allocating a Brush every frame.
    // (Brush.verticalGradient with default start/end spans the draw bounds,
    // exactly like the old explicit startY=0/endY=h.)
    val bgBrush = remember(bg, dimBg) { Brush.verticalGradient(listOf(bg, dimBg)) }
    val bgTriBrush = remember(bg, dimBg) { Brush.verticalGradient(listOf(bg, dimBg, bg)) }

    // Max-size seed pools; density only changes how many are drawn.
    val seeds = remember(config.loopId) {
        val rng = Random(config.loopId.ordinal * 7919 + 13)
        List(260) {
            Seed(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = rng.nextFloat(),
                p1 = rng.nextFloat() * TAU,
                p2 = rng.nextFloat() * TAU
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= 31 && config.blurLevel > 0.01f) {
                    val r = config.blurLevel * 28f
                    renderEffect = BlurEffect(r, r)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phase = phaseOf()
            val tau = phase * TAU
            val w = size.width
            val h = size.height
            val density = config.loopDensity
            val pDensity = config.particleDensity
            val pSpeed = config.particleSpeed

            when (config.loopId) {
                LoopId.QUANTUM_FIELD -> {
                    drawRect(brush = bgBrush)
                    // --- flowing data streams: particles riding sine paths ---
                    val streams = 6
                    val perStreamMax = 24
                    val perStream = (4 + 20 * pDensity).toInt().coerceAtMost(perStreamMax)
                    val cycles = 2 // traversals per period (integer -> seamless)
                    for (i in 0 until streams) {
                        val baseY = (i + 0.5f) / streams * h
                        val amp = h * 0.028f
                        fun pathY(x: Float): Float {
                            val xf = x / w
                            return baseY + amp * sin(xf * 3f * TAU + tau * 2f * pSpeed + i * 1.7f)
                        }
                        // faint stream guide line
                        var lx = 0f
                        var ly = pathY(0f)
                        val steps = 24
                        for (sIdx in 1..steps) {
                            val x = sIdx.toFloat() / steps * w
                            val y = pathY(x)
                            drawLine(
                                color = particle.copy(alpha = 0.10f),
                                start = Offset(lx, ly),
                                end = Offset(x, y),
                                strokeWidth = 1.5f
                            )
                            lx = x; ly = y
                        }
                        for (j in 0 until perStream) {
                            val seed = seeds[(i * perStreamMax + j) % seeds.size]
                            val s = ((phase * cycles * pSpeed) + j.toFloat() / perStream + i * 0.13f + seed.p1 / TAU * 0.05f) % 1f
                            val x = s * w
                            val y = pathY(x)
                            drawQuantumParticle(
                                type = config.particleType,
                                color = particle,
                                center = Offset(x, y),
                                baseR = 2.2f + seed.r * 2.4f,
                                alpha = 0.85f
                            )
                        }
                    }
                    // --- pulsing network nodes on a grid ---
                    val cols = 4
                    val rows = 3
                    for (gy in 0 until rows) {
                        for (gx in 0 until cols) {
                            val nx = (gx + 0.5f) / cols
                            val ny = (gy + 0.5f) / rows
                            val pulse = 0.5f + 0.5f * sin(tau * 2f + (gx * rows + gy) * 1.3f)
                            val c = Offset(nx * w, ny * h)
                            drawCircle(
                                color = godRing.copy(alpha = 0.16f),
                                radius = 14f + 10f * pulse,
                                center = c
                            )
                            drawCircle(
                                color = godRing.copy(alpha = 0.35f + 0.35f * pulse),
                                radius = 2.4f + 2f * pulse,
                                center = c
                            )
                        }
                    }
                    // --- packet bursts: expanding rings, 3 per period ---
                    for (b in 0 until 3) {
                        val bt = ((phase * 3f) + b * 0.37f) % 1f
                        if (bt < 0.18f) {
                            val f = bt / 0.18f
                            val bx = (0.25f + b * 0.25f) * w
                            val by = (0.3f + (b % 2) * 0.35f) * h
                            drawCircle(
                                color = particle.copy(alpha = (1f - f) * 0.7f),
                                radius = f * 90f + 6f,
                                center = Offset(bx, by),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                        }
                    }
                }

                LoopId.EMBER_STORM -> {
                    drawRect(brush = bgBrush)
                    // --- rising embers + falling ash, 3 depth layers, wrap-around ---
                    val layerCounts = intArrayOf(26, 34, 44)
                    val layerSize = floatArrayOf(2.6f, 1.9f, 1.3f)
                    val riseCycles = intArrayOf(1, 2, 3)
                    val fallCycles = intArrayOf(2, 3, 4)
                    var seedIdx = 0
                    for (layer in 0 until 3) {
                        val count = (layerCounts[layer] * density * (0.4f + 0.6f * pDensity)).toInt()
                        for (k in 0 until count) {
                            val seed = seeds[seedIdx % seeds.size]
                            seedIdx++
                            // rising ember
                            val yf = (seed.y + phase * riseCycles[layer] * pSpeed) % 1f
                            val ey = h * (1f - yf * 1.25f)
                            val ex = (seed.x + 0.03f * sin(tau * 2f * pSpeed + seed.p1)) * w
                            val fade = (1f - yf).coerceAtLeast(0f)
                            val emberColor = lerp(godRing, particle, layer / 2f)
                            drawCircle(
                                color = emberColor.copy(alpha = 0.6f * fade),
                                radius = layerSize[layer] * (0.7f + seed.r * 0.6f),
                                center = Offset(ex, ey)
                            )
                            // falling ash mote
                            val yf2 = (seed.p2 / TAU + phase * fallCycles[layer] * pSpeed) % 1f
                            val ay = h * yf2
                            val ax = (seed.x + 0.02f * cos(tau * 3f * pSpeed + seed.p2)) * w
                            drawCircle(
                                color = Color(0xFF9AA3B2).copy(alpha = 0.28f * (1f - yf2 * 0.5f)),
                                radius = layerSize[layer] * 0.8f,
                                center = Offset(ax, ay)
                            )
                        }
                    }
                    // ember glow rising from the bottom
                    val glowPulse = 0.10f + 0.05f * sin(tau)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                particle.copy(alpha = glowPulse + 0.10f)
                            ),
                            startY = h * 0.55f,
                            endY = h
                        )
                    )
                }

                LoopId.NEBULA_DRIFT -> {
                    drawRect(color = bg)
                    // --- slow-drifting layered radial-gradient nebula blobs ---
                    val blobColors = listOf(
                        accent, godRing,
                        shiftHue(Color(0xFF22D3EE), config.hueShift),
                        shiftHue(Color(0xFF8B5CF6), config.hueShift),
                        particle
                    )
                    val blobBases = listOf(
                        Offset(0.22f, 0.28f), Offset(0.78f, 0.62f), Offset(0.55f, 0.85f),
                        Offset(0.80f, 0.18f), Offset(0.30f, 0.70f)
                    )
                    for (bIdx in blobColors.indices) {
                        val seed = seeds[bIdx]
                        val c1 = 1 + (bIdx % 2) // integer cycles -> seamless
                        val c2 = 1 + ((bIdx + 1) % 2)
                        val cx = (blobBases[bIdx].x + 0.09f * cos(tau * c1 * pSpeed + seed.p1)) * w
                        val cy = (blobBases[bIdx].y + 0.08f * sin(tau * c2 * pSpeed + seed.p2)) * h
                        val radius = (0.42f + seed.r * 0.18f) * maxOf(w, h)
                        val col = blobColors[bIdx]
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    col.copy(alpha = 0.26f),
                                    col.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    }
                    // --- starfield ---
                    val starCount = (40 + 90 * density).toInt().coerceAtMost(130)
                    for (sIdx in 0 until starCount) {
                        val seed = seeds[(sIdx + 8) % seeds.size]
                        val tw = 0.30f + 0.70f * abs(sin(tau * (1 + sIdx % 3) + seed.p1))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f * tw),
                            radius = 0.8f + seed.r * 1.8f,
                            center = Offset(seed.x * w, seed.y * h)
                        )
                    }
                }

                LoopId.STATIC_GRADIENT -> {
                    drawRect(brush = bgTriBrush)
                    // whisper of ember at the bottom so it never reads flat
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(w / 2f, h),
                            radius = w * 0.7f
                        ),
                        radius = w * 0.7f,
                        center = Offset(w / 2f, h)
                    )
                }
            }

            // --- post: dim + vignette ---
            if (config.dimLevel > 0.01f) {
                drawRect(color = Color.Black.copy(alpha = config.dimLevel))
            }
            if (config.vignette > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = config.vignette * 0.85f)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = maxOf(w, h) * 0.72f
                    ),
                    radius = maxOf(w, h) * 0.72f,
                    center = Offset(w / 2f, h / 2f)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuantumParticle(
    type: ParticleType,
    color: Color,
    center: Offset,
    baseR: Float,
    alpha: Float
) {
    when (type) {
        ParticleType.SPARKS -> {
            drawCircle(color = color.copy(alpha = alpha * 0.35f), radius = baseR * 2.4f, center = center)
            drawCircle(color = color.copy(alpha = alpha), radius = baseR, center = center)
            drawCircle(color = Color.White.copy(alpha = alpha * 0.9f), radius = baseR * 0.35f, center = center)
        }
        ParticleType.MOTES -> {
            drawCircle(color = color.copy(alpha = alpha * 0.5f), radius = baseR * 0.8f, center = center)
        }
        ParticleType.PIXELS -> {
            val s = baseR * 1.6f
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(center.x - s / 2f, center.y - s / 2f),
                size = androidx.compose.ui.geometry.Size(s, s)
            )
        }
        ParticleType.ORBS -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseR * 3f
                ),
                radius = baseR * 3f,
                center = center
            )
        }
    }
}

/** Brand palette for previews/tests without a full config. */
@Suppress("unused")
fun defaultModConfig(): PantheonConfig = PantheonConfig(
    colorBackground = ModBrand.ASH_VOID,
    colorAccent = ModBrand.EMBER
)
