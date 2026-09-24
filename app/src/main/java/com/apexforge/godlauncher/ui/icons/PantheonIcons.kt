package com.apexforge.godlauncher.ui.icons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.PhoenixGold

/**
 * Bespoke Pantheon iconography — every god sigil is drawn in code, no
 * stock glyphs, no emoji, no downloaded assets. All sigils live in a
 * 100x100 coordinate space scaled to the canvas.
 */
@Composable
fun GodSigil(
    god: God,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        when (god.id) {
            "zeus" -> drawZeus(tint)
            "athena" -> drawAthena(tint)
            "hermes" -> drawHermes(tint)
            "hephaestus" -> drawHephaestus(tint)
            "tyche" -> drawTyche(tint)
            "argus" -> drawArgus(tint)
            "odysseus" -> drawOdysseus(tint)
            "themis" -> drawThemis(tint)
            else -> drawTyche(tint)
        }
    }
}

private fun DrawScope.sig(block: DrawScope.(Float) -> Unit) {
    block(size.minDimension / 100f)
}

private fun DrawScope.drawZeus(tint: Color) = sig { u ->
    val bolt = Path().apply {
        moveTo(58f * u, 6f * u)
        lineTo(30f * u, 56f * u)
        lineTo(47f * u, 56f * u)
        lineTo(40f * u, 94f * u)
        lineTo(70f * u, 42f * u)
        lineTo(52f * u, 42f * u)
        close()
    }
    drawPath(bolt, tint)
}

private fun DrawScope.drawAthena(tint: Color) = sig { u ->
    val s = Stroke(width = 7f * u, cap = StrokeCap.Round)
    // Owl eyes.
    drawCircle(tint, 14f * u, Offset(33f * u, 44f * u), style = s)
    drawCircle(tint, 14f * u, Offset(67f * u, 44f * u), style = s)
    drawCircle(tint, 5f * u, Offset(33f * u, 44f * u))
    drawCircle(tint, 5f * u, Offset(67f * u, 44f * u))
    // Stern brow chevrons.
    drawLine(tint, Offset(14f * u, 20f * u), Offset(46f * u, 30f * u), strokeWidth = 7f * u, cap = StrokeCap.Round)
    drawLine(tint, Offset(86f * u, 20f * u), Offset(54f * u, 30f * u), strokeWidth = 7f * u, cap = StrokeCap.Round)
    // Beak.
    val beak = Path().apply {
        moveTo(42f * u, 66f * u); lineTo(58f * u, 66f * u); lineTo(50f * u, 80f * u); close()
    }
    drawPath(beak, tint)
}

private fun DrawScope.drawHermes(tint: Color) = sig { u ->
    val s = Stroke(width = 7f * u, cap = StrokeCap.Round)
    // Three sweeping wing feathers.
    for (i in 0..2) {
        val y = (26f + i * 20f) * u
        val p = Path().apply {
            moveTo(14f * u, (78f - i * 8f) * u)
            quadraticBezierTo(48f * u, (72f - i * 14f) * u, 86f * u, y)
        }
        drawPath(p, tint, style = s)
    }
}

private fun DrawScope.drawHephaestus(tint: Color) = sig { u ->
    // Hammer head + handle.
    drawRect(tint, Offset(26f * u, 18f * u), Size(48f * u, 26f * u))
    drawRect(tint, Offset(45f * u, 44f * u), Size(10f * u, 44f * u))
    // Spark notch on the head.
    drawLine(
        Color.Black.copy(alpha = 0.35f),
        Offset(26f * u, 31f * u), Offset(74f * u, 31f * u),
        strokeWidth = 4f * u, cap = StrokeCap.Round
    )
}

private fun DrawScope.drawTyche(tint: Color) = sig { u ->
    // Four-point fortune star.
    val star = Path().apply {
        moveTo(50f * u, 6f * u)
        lineTo(61f * u, 39f * u)
        lineTo(94f * u, 50f * u)
        lineTo(61f * u, 61f * u)
        lineTo(50f * u, 94f * u)
        lineTo(39f * u, 61f * u)
        lineTo(6f * u, 50f * u)
        lineTo(39f * u, 39f * u)
        close()
    }
    drawPath(star, tint)
}

private fun DrawScope.drawArgus(tint: Color) = sig { u ->
    val s = Stroke(width = 7f * u, cap = StrokeCap.Round)
    // The all-seeing eye.
    val eye = Path().apply {
        moveTo(10f * u, 50f * u)
        quadraticBezierTo(50f * u, 14f * u, 90f * u, 50f * u)
        quadraticBezierTo(50f * u, 86f * u, 10f * u, 50f * u)
        close()
    }
    drawPath(eye, tint, style = s)
    drawCircle(tint, 9f * u, Offset(50f * u, 50f * u))
}

private fun DrawScope.drawOdysseus(tint: Color) = sig { u ->
    // Compass rose.
    val c = 50f * u
    fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        drawPath(Path().apply {
            moveTo(x1 * u, y1 * u); lineTo(x2 * u, y2 * u); lineTo(x3 * u, y3 * u); close()
        }, tint)
    }
    tri(50f, 8f, 58f, 50f, 42f, 50f)    // N
    tri(50f, 92f, 58f, 50f, 42f, 50f)   // S
    tri(92f, 50f, 50f, 58f, 50f, 42f)   // E
    tri(8f, 50f, 50f, 58f, 50f, 42f)    // W
    drawCircle(Color.Black.copy(alpha = 0.4f), 7f * u, Offset(c, c))
}

private fun DrawScope.drawThemis(tint: Color) = sig { u ->
    val s = Stroke(width = 6f * u, cap = StrokeCap.Round)
    // Pillar, beam, hanging pans.
    drawLine(tint, Offset(50f * u, 12f * u), Offset(50f * u, 88f * u), strokeWidth = 7f * u, cap = StrokeCap.Round)
    drawLine(tint, Offset(22f * u, 24f * u), Offset(78f * u, 24f * u), strokeWidth = 7f * u, cap = StrokeCap.Round)
    drawLine(tint, Offset(26f * u, 24f * u), Offset(26f * u, 40f * u), strokeWidth = s.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(74f * u, 24f * u), Offset(74f * u, 40f * u), strokeWidth = s.width, cap = StrokeCap.Round)
    drawArc(tint, 0f, 180f, false, topLeft = Offset(14f * u, 40f * u), size = Size(24f * u, 14f * u), style = s)
    drawArc(tint, 0f, 180f, false, topLeft = Offset(62f * u, 40f * u), size = Size(24f * u, 14f * u), style = s)
    // Base.
    drawLine(tint, Offset(34f * u, 88f * u), Offset(66f * u, 88f * u), strokeWidth = 7f * u, cap = StrokeCap.Round)
}

/**
 * Quantum frame — the Pantheon treatment applied to stock app icons so
 * nothing on the home screen looks like default Android: a slowly
 * rotating triple-arc ring (ember / cyan / gold) with corner ticks.
 */
@Composable
fun QuantumFrame(
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val rot by if (animationsEnabled) {
        val t = rememberInfiniteTransition(label = "qframe")
        t.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
            label = "qrot"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val u = size.minDimension
            val ringR = u / 2f - u * 0.10f
            val sw = (u * 0.035f).coerceAtLeast(2f)
            rotate(rot) {
                drawArc(
                    EmberOrange, 0f, 110f, false,
                    topLeft = Offset((u - ringR * 2f) / 2f, (u - ringR * 2f) / 2f),
                    size = Size(ringR * 2f, ringR * 2f),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
                drawArc(
                    Color(0xFF7DF9FF), 140f, 80f, false,
                    topLeft = Offset((u - ringR * 2f) / 2f, (u - ringR * 2f) / 2f),
                    size = Size(ringR * 2f, ringR * 2f),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
                drawArc(
                    PhoenixGold, 245f, 60f, false,
                    topLeft = Offset((u - ringR * 2f) / 2f, (u - ringR * 2f) / 2f),
                    size = Size(ringR * 2f, ringR * 2f),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
            // Corner ticks — static machine marks.
            val tick = u * 0.12f
            val c = EmberOrange.copy(alpha = 0.85f)
            val w = (u * 0.03f).coerceAtLeast(2f)
            drawLine(c, Offset(0f, 0f), Offset(tick, 0f), w, StrokeCap.Round)
            drawLine(c, Offset(0f, 0f), Offset(0f, tick), w, StrokeCap.Round)
            drawLine(c, Offset(u, 0f), Offset(u - tick, 0f), w, StrokeCap.Round)
            drawLine(c, Offset(u, 0f), Offset(u, tick), w, StrokeCap.Round)
            drawLine(c, Offset(0f, u), Offset(tick, u), w, StrokeCap.Round)
            drawLine(c, Offset(0f, u), Offset(0f, u - tick), w, StrokeCap.Round)
            drawLine(c, Offset(u, u), Offset(u - tick, u), w, StrokeCap.Round)
            drawLine(c, Offset(u, u), Offset(u, u - tick), w, StrokeCap.Round)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
