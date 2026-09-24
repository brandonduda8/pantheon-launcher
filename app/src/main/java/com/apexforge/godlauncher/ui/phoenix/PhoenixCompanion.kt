package com.apexforge.godlauncher.ui.phoenix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.R
import com.apexforge.godlauncher.data.SystemSnapshot
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Phoenix, the on-screen companion — Brandon's Spyro-like phoenix,
 * floating on the home screen. Tap opens Phoenix chat; long-press (or
 * tapping the bubble) shows a status bubble fed by the stored system
 * snapshot, always labeled as such.
 *
 * The art is swappable: replace res/drawable/phoenix_companion.webp and
 * the launcher picks it up on the next build — no code changes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoenixCompanion(
    snapshot: SystemSnapshot?,
    animationsEnabled: Boolean,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bubbleOpen by remember { mutableStateOf(false) }

    // Idle life: gentle bob + breathing ember halo.
    // v7: animation states are read only in the draw/layout phase
    // (graphicsLayer / offset lambdas) — the companion no longer
    // recomposes every animation frame.
    val bobState: State<Float>?
    val haloScaleState: State<Float>?
    val haloAlphaState: State<Float>?
    if (animationsEnabled) {
        val t = rememberInfiniteTransition(label = "phoenixIdle")
        bobState = t.animateFloat(
            0f, 2f * PI.toFloat(),
            infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "bob"
        )
        haloScaleState = t.animateFloat(
            1f, 1.22f,
            infiniteRepeatable(tween(2400), RepeatMode.Reverse),
            label = "haloScale"
        )
        haloAlphaState = t.animateFloat(
            0.42f, 0.16f,
            infiniteRepeatable(tween(2400), RepeatMode.Reverse),
            label = "haloAlpha"
        )
    } else {
        bobState = null; haloScaleState = null; haloAlphaState = null
    }
    val density = LocalDensity.current

    val statusLine = remember(snapshot) {
        if (snapshot == null) "Waking up…"
        else "GPU ${trimHours(snapshot.gpuRemainingH)}h left · " +
            "Jobs ${snapshot.jobsApplied}/${snapshot.jobsGoal} · " +
            snapshot.moneyNet.substringBefore(" (")
    }
    val snapshotCaption = remember(snapshot) {
        val at = snapshot?.snapshotAt?.replace("T", " ")?.removeSuffix("Z") ?: ""
        "STORED SNAPSHOT · $at UTC"
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Ember halo behind the bird.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // The animated alpha used to be baked into the brush (which
                // forced a recomposition every frame); it is now layer alpha
                // in the draw phase with the brush at its 0.42 peak —
                // identical output, zero recomposition.
                .graphicsLayer {
                    val hs = haloScaleState?.value ?: 1.1f
                    scaleX = hs
                    scaleY = hs
                    alpha = (haloAlphaState?.value ?: 0.25f) / 0.42f
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmberOrange.copy(alpha = 0.42f),
                            PhoenixGold.copy(alpha = 0.42f * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        // The bird itself.
        val interaction = remember { MutableInteractionSource() }
        Image(
            painter = painterResource(id = R.drawable.phoenix_companion),
            contentDescription = "Phoenix companion — tap to chat",
            modifier = Modifier
                .fillMaxSize(0.92f)
                .offset {
                    // Draw-phase bob: no recomposition.
                    val bobPx = with(density) {
                        (sin(bobState?.value ?: 0f) * 7f).dp.toPx()
                    }
                    IntOffset(0, bobPx.roundToInt())
                }
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onOpenChat,
                    onLongClick = { bubbleOpen = !bubbleOpen }
                )
        )
        // Status bubble.
        AnimatedVisibility(
            visible = bubbleOpen,
            enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF20D1020)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenChat,
                        onLongClick = { bubbleOpen = false }
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = snapshotCaption,
                        style = MaterialTheme.typography.labelSmall,
                        color = PhoenixGold.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Tap to talk to Phoenix",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun trimHours(h: Double): String {
    val s = "%.2f".format(h).trimEnd('0').trimEnd('.')
    return s.ifEmpty { "0" }
}
