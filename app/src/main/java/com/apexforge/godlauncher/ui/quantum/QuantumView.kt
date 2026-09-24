package com.apexforge.godlauncher.ui.quantum

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.data.ActivityBus
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.model.PantheonConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val TAU = 2f * PI.toFloat()

private data class Node(val god: God, val nx: Float, val ny: Float, val depth: Float)

private fun bez(p0: Offset, p1: Offset, p2: Offset, s: Float): Offset {
    val u = 1f - s
    return Offset(
        u * u * p0.x + 2f * u * s * p1.x + s * s * p2.x,
        u * u * p0.y + 2f * u * s * p1.y + s * s * p2.y
    )
}

/**
 * The Quantum Agent View: the Pantheon rendered as a living quantum
 * computer. The 8 god orbs are compute nodes in a constellation — Zeus at
 * the core, the rest on the ring — with animated data streams (bezier
 * particle flows) connecting them. When a god "works" (ActivityBus pulse),
 * activity rings radiate from its node and surge particles stream inward
 * from every incident edge.
 *
 * Tap an orb = same as tapping the god in the dock. Long-press = the god
 * quick-action sheet. Subtle parallax follows the last touch point.
 */
@Composable
fun QuantumView(
    config: PantheonConfig,
    godPackages: Map<String, String?>,
    animationsEnabled: Boolean,
    onGodClick: (God) -> Unit,
    onGodLongPress: (God) -> Unit,
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
    // v6: stream phase from an infinite transition, read only in the draw
    // phase — no 30fps tick()/recomposition storm. (cycleState is a nullable
    // State so no trailing-lambda parsing ambiguity can attach to the
    // animateFloat call.)
    val cycleState: State<Float>? = if (running) {
        val qTransition = rememberInfiniteTransition(label = "quantum")
        qTransition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
            label = "quantumCycle"
        )
    } else null
    val phaseOf: () -> Float = { cycleState?.value ?: 0.22f }
    val now = System.currentTimeMillis()

    val textMeasurer = rememberTextMeasurer()

    // Constellation: Zeus at the core, seven gods on the ring.
    val nodes = remember {
        val ring = PANTHEON.filter { it.id != "zeus" }
        val list = mutableListOf(Node(PANTHEON.first { it.id == "zeus" }, 0.5f, 0.44f, 1.3f))
        ring.forEachIndexed { i, god ->
            val a = -PI.toFloat() / 2f + i * TAU / ring.size
            list.add(Node(god, 0.5f + 0.38f * cos(a), 0.44f + 0.38f * sin(a), 0.85f))
        }
        list
    }
    // Edges: ring links + spokes from Zeus.
    val edges = remember {
        val e = mutableListOf<Pair<Int, Int>>()
        for (i in 1..7) {
            e.add(i to (if (i == 7) 1 else i + 1))
            e.add(0 to i)
        }
        e
    }

    var parallaxTarget by remember { mutableStateOf(Offset.Zero) }
    var parallax by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val c = canvasSize
                    if (c != Offset.Zero) {
                        parallaxTarget =
                            (down.position - Offset(c.x / 2f, c.y / 2f)) / c.x * 46f
                    }
                    // wait for release, then ease back
                    var done = false
                    while (!done) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.all { !it.pressed }) done = true
                    }
                    parallaxTarget = Offset.Zero
                }
            }
            .pointerInput(nodes) {
                detectTapGestures(
                    onTap = { offset ->
                        nearestNode(nodes, canvasSize, parallax, offset)?.let { onGodClick(it.god) }
                    },
                    onLongPress = { offset ->
                        nearestNode(nodes, canvasSize, parallax, offset)?.let { onGodLongPress(it.god) }
                    }
                )
            }
    ) {
        canvasSize = Offset(size.width, size.height)
        // Ease parallax toward the touch target. Snaps exactly on arrival
        // (structural equality => no recomposition loop); static when idle.
        val pTarget = parallaxTarget
        parallax = if (animating) {
            val nx = parallax.x + (pTarget.x - parallax.x) * 0.12f
            val ny = parallax.y + (pTarget.y - parallax.y) * 0.12f
            if (abs(nx - pTarget.x) < 0.05f && abs(ny - pTarget.y) < 0.05f) pTarget
            else Offset(nx, ny)
        } else {
            pTarget
        }

        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val orbR = minDim * 0.072f
        val phase = phaseOf()
        val tau = phase * TAU

        // Mod-engine tinting: streams ride the accent, surges burn god-ring.
        val streamColor = parseHexColor(config.colorAccent, Color(0xFF22D3EE))
        val surgeColor = parseHexColor(config.colorGodRing, Color(0xFFF5B942))

        fun nodePos(n: Node): Offset =
            Offset(n.nx * w, n.ny * h) + parallax * n.depth

        val pos = nodes.associateWith { nodePos(it) }
        val heats = nodes.associate { it.god.id to ActivityBus.heat(it.god.id, now) }

        // --- data streams along bezier edges ---
        for ((aIdx, bIdx) in edges) {
            val a = pos[nodes[aIdx]]!!
            val b = pos[nodes[bIdx]]!!
            val mid = (a + b) / 2f
            val dir = b - a
            val len = sqrt(dir.x.pow(2) + dir.y.pow(2))
            if (len < 1f) continue
            val normal = Offset(-dir.y / len, dir.x / len)
            val ctrl = mid + normal * len * 0.14f

            // faint conduit line
            var px = a.x
            var py = a.y
            val steps = 20
            for (sIdx in 1..steps) {
                val p = bez(a, ctrl, b, sIdx.toFloat() / steps)
                drawLine(
                    color = streamColor.copy(alpha = 0.14f),
                    start = Offset(px, py),
                    end = p,
                    strokeWidth = 1.2f
                )
                px = p.x; py = p.y
            }

            // flowing particles (integer cycles per loop -> seamless)
            val heat = maxOf(heats[nodes[aIdx].god.id] ?: 0f, heats[nodes[bIdx].god.id] ?: 0f)
            val perEdge = 5
            for (j in 0 until perEdge) {
                val s = ((phase * 2f) + j.toFloat() / perEdge + aIdx * 0.061f) % 1f
                val p = bez(a, ctrl, b, s)
                val r = 2f + heat * 2.5f
                drawCircle(
                    color = streamColor.copy(alpha = 0.35f + heat * 0.5f),
                    radius = r * 2.2f,
                    center = p
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = r * 0.8f,
                    center = p
                )
            }

            // surge: when a node is hot, particles stream inward along
            // every incident edge — the eye reads it as acceleration
            // toward the working god.
            for (endIdx in listOf(aIdx, bIdx)) {
                val endHeat = heats[nodes[endIdx].god.id] ?: 0f
                if (endHeat > 0.05f) {
                    val target = pos[nodes[endIdx]]!!
                    val surgeN = 7
                    for (k in 0 until surgeN) {
                        val sk = 1f - (((phase * 3f) + k.toFloat() / surgeN) % 1f) * 0.4f
                        val p = if (endIdx == bIdx) bez(a, ctrl, b, sk) else bez(b, ctrl, a, sk)
                        val dist = sqrt(
                            (p.x - target.x).pow(2) + (p.y - target.y).pow(2)
                        )
                        val glow = (1f - (dist / (len * 0.45f)).coerceIn(0f, 1f)) * endHeat
                        if (glow > 0.03f) {
                            drawCircle(
                                color = surgeColor.copy(alpha = glow * 0.85f),
                                radius = 3.4f,
                                center = p
                            )
                        }
                    }
                }
            }
        }

        // --- god orbs ---
        for (n in nodes) {
            val c = pos[n]!!
            val heat = heats[n.god.id] ?: 0f
            val r = orbR * (if (n.god.id == "zeus") 1.28f else 1f)
            val assigned = godPackages[n.god.id] != null

            // glow falloff — flares when the god is working
            val glowR = r * (3.1f + heat * 1.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        n.god.colorA.copy(alpha = 0.34f + heat * 0.45f),
                        n.god.colorA.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = glowR
                ),
                radius = glowR,
                center = c
            )

            // activity pulse rings radiating outward
            if (heat > 0.03f) {
                for (k in 0 until 3) {
                    val rf = ((now / 700.0) + k / 3.0) % 1.0
                    drawCircle(
                        color = surgeColor.copy(alpha = ((1.0 - rf) * heat * 0.8).toFloat()),
                        radius = (rf * r * 3.6 + r).toFloat(),
                        center = c,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // slow breathing halo
            val breathe = if (animating) 1f + 0.06f * sin(tau * 2f + n.nx * 9f) else 1f
            drawCircle(
                color = n.god.colorB.copy(alpha = 0.35f),
                radius = r * 1.32f * breathe,
                center = c,
                style = Stroke(width = 1.6f)
            )

            // the orb itself
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(n.god.colorA, n.god.colorB),
                    startY = c.y - r,
                    endY = c.y + r
                ),
                radius = r,
                center = c
            )
            // monogram
            val glyphStyle = TextStyle(
                color = Color.White,
                fontSize = (r * 0.52f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            val glyphLayout = textMeasurer.measure(n.god.glyph, glyphStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = n.god.glyph,
                style = glyphStyle,
                topLeft = Offset(
                    c.x - glyphLayout.size.width / 2f,
                    c.y - glyphLayout.size.height / 2f
                )
            )
            // name label
            val labelStyle = TextStyle(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            val labelLayout = textMeasurer.measure(n.god.name, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = n.god.name,
                style = labelStyle,
                topLeft = Offset(
                    c.x - labelLayout.size.width / 2f,
                    c.y + r + 6f
                )
            )
            // assigned tick: tiny ember dot under the label
            if (assigned) {
                drawCircle(
                    color = Color(0xFFE25822),
                    radius = 3f,
                    center = Offset(c.x, c.y + r + 6f + labelLayout.size.height + 6f)
                )
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color = try {
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

private fun nearestNode(
    nodes: List<Node>,
    canvasSize: Offset,
    parallax: Offset,
    tap: Offset
): Node? {
    if (canvasSize == Offset.Zero) return null
    val w = canvasSize.x
    val h = canvasSize.y
    var best: Node? = null
    var bestD = Float.MAX_VALUE
    for (n in nodes) {
        val c = Offset(n.nx * w, n.ny * h) + parallax * n.depth
        val d = sqrt((tap.x - c.x).pow(2) + (tap.y - c.y).pow(2))
        if (d < bestD) {
            bestD = d
            best = n
        }
    }
    val hitR = minOf(w, h) * 0.072f * 1.7f
    return if (bestD <= hitR) best else null
}

/**
 * God quick-action sheet: long-press an orb to assign its app or ask
 * Phoenix about the god. Rendered as an overlay by the host.
 */
@Composable
fun GodQuickSheet(
    god: God,
    assignedLabel: String?,
    onLaunch: () -> Unit,
    onAssign: () -> Unit,
    onAskPhoenix: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF14100D).copy(alpha = 0.97f)
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {})
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(god.colorA, god.colorB))
                            )
                    ) {
                        Text(
                            text = god.glyph,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = god.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "God of ${god.domain} · ${assignedLabel ?: "no app assigned"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (assignedLabel != null) {
                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE25822)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Launch $assignedLabel", color = Color.White) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = onAssign,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2118)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (assignedLabel == null) "Assign app" else "Reassign app", color = Color.White) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAskPhoenix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2118)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ask Phoenix about ${god.name}", color = Color.White) }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
