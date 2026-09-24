package com.apexforge.godlauncher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.model.BadgeStyle
import com.apexforge.godlauncher.model.DockStyle
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.IconShape
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.ui.icons.GodSigil
import com.apexforge.godlauncher.ui.theme.EmberOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private object HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        val path = Path()
        for (i in 0 until 6) {
            val a = PI.toFloat() / 3f * i - PI.toFloat() / 2f
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * The Pantheon dock: god quick-actions. Tap launches the assigned app (or
 * opens the picker when unassigned); long-press always opens the picker.
 *
 * v3: fully mod-driven — dock style (ember ring / minimal / glass / neon),
 * icon shape (circle / squircle / hexagon / raw), glow, neon tint, tilt,
 * pulse wave, badge style, and the visible god list (order + membership)
 * all come from PantheonConfig. Set [vertical] for the LEFT/RIGHT side
 * dock strip.
 */
@Composable
fun GodDock(
    godLabels: Map<String, String?>,
    godPackages: Map<String, String?> = emptyMap(),
    badgeCounts: Map<String, Int> = emptyMap(),
    onGodClick: (God) -> Unit,
    onGodLongPress: (God) -> Unit,
    animationsEnabled: Boolean = true,
    started: Boolean = true,
    dockStyle: DockStyle = DockStyle.EMBER_RING,
    iconShape: IconShape = IconShape.CIRCLE,
    iconGlow: Float = 0.5f,
    neonTint: Boolean = true,
    tiltEffect: Boolean = false,
    dockPulseWave: Boolean = true,
    badgeStyle: BadgeStyle = BadgeStyle.GLOW,
    gods: List<God> = PANTHEON,
    vertical: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (god in gods) {
                SideGodButton(
                    god = god,
                    badgeCount = badgeCounts[godPackages[god.id]] ?: 0,
                    animationsEnabled = animationsEnabled,
                    iconShape = iconShape,
                    badgeStyle = badgeStyle,
                    onClick = { onGodClick(god) },
                    onLongPress = { onGodLongPress(god) }
                )
            }
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in gods.chunked(4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (god in row) {
                    GodButton(
                        god = god,
                        assignedLabel = godLabels[god.id],
                        badgeCount = badgeCounts[godPackages[god.id]] ?: 0,
                        animationsEnabled = animationsEnabled,
                        started = started,
                        dockStyle = dockStyle,
                        iconShape = iconShape,
                        iconGlow = iconGlow,
                        neonTint = neonTint,
                        tiltEffect = tiltEffect,
                        dockPulseWave = dockPulseWave,
                        badgeStyle = badgeStyle,
                        onClick = { onGodClick(god) },
                        onLongPress = { onGodLongPress(god) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // pad short rows so spacing stays even
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GodButton(
    god: God,
    assignedLabel: String?,
    badgeCount: Int = 0,
    animationsEnabled: Boolean,
    started: Boolean = true,
    dockStyle: DockStyle,
    iconShape: IconShape,
    iconGlow: Float,
    neonTint: Boolean,
    tiltEffect: Boolean,
    dockPulseWave: Boolean,
    badgeStyle: BadgeStyle,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // v7: tilt stays parked until the activity is actually resumed —
    // eight infinite transitions must not wake the choreographer
    // through cold start.
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "godPress"
    )
    val flare by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(180),
        label = "godFlare"
    )

    // v7: halo tickers are not created until the splash dismisses — 16
    // fewer animation drivers fighting cold start on low-end devices.
    val pulsing = animationsEnabled && started && dockPulseWave &&
        dockStyle != DockStyle.MINIMAL
    // v6: halo/tilt phases are read inside graphicsLayer/drawBehind blocks
    // (draw phase) — eight icons no longer recompose every frame.
    val haloScaleState: State<Float>? = if (pulsing) {
        val halo = rememberInfiniteTransition(label = "godHalo")
        halo.animateFloat(
            1f, 1.3f,
            infiniteRepeatable(tween(2200), RepeatMode.Reverse),
            label = "haloScale"
        )
    } else null
    val haloAlphaState: State<Float>? = if (pulsing) {
        val haloA = rememberInfiniteTransition(label = "godHaloA")
        haloA.animateFloat(
            0.34f, 0.10f,
            infiniteRepeatable(tween(2200), RepeatMode.Reverse),
            label = "haloAlpha"
        )
    } else null
    val tiltState: State<Float>? = if (tiltEffect && animationsEnabled && resumed) {
        val tt = rememberInfiniteTransition(label = "godTilt")
        tt.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "tiltPhase"
        )
    } else null
    val glowScale = 0.25f + iconGlow

    val emberA = lerp(god.colorA, Color(0xFFFF6D00), 0.45f)
    val emberB = lerp(god.colorB, Color(0xFF7C2D12), 0.55f)
    // v6: halo brush hoisted — the animated alpha is applied at draw time.
    val haloEmberBrush = remember(emberA, emberB) {
        Brush.radialGradient(
            colors = listOf(
                emberA,
                emberB.copy(alpha = 0.45f),
                Color.Transparent
            )
        )
    }
    val medallionA = if (neonTint) lerp(god.colorA, Color.White, 0.14f) else god.colorA
    val medallionB = if (neonTint) lerp(god.colorB, Color.White, 0.10f) else god.colorB

    val medallionShape: Shape? = when (iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUIRCLE -> RoundedCornerShape(20.dp)
        IconShape.HEXAGON -> HexagonShape
        IconShape.RAW -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .graphicsLayer {
                    rotationZ = tiltState?.let { sin(it.value * 2f * PI.toFloat()) * 6f } ?: 0f
                }
        ) {
            // Style layer behind the medallion.
            when (dockStyle) {
                DockStyle.EMBER_RING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val hs = (haloScaleState?.value ?: 1.12f) + flare * 0.22f
                                scaleX = hs
                                scaleY = hs
                            }
                            .clip(CircleShape)
                            .drawBehind {
                                val ha = (((haloAlphaState?.value ?: 0.20f) + flare * 0.35f) * glowScale)
                                    .coerceAtMost(1f)
                                drawCircle(brush = haloEmberBrush, alpha = ha)
                            }
                    )
                }
                DockStyle.NEON -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val hs = (haloScaleState?.value ?: 1.12f) + flare * 0.22f
                                scaleX = hs
                                scaleY = hs
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        god.colorA.copy(alpha = (0.55f * glowScale + flare * 0.3f).coerceAtMost(1f)),
                                        god.colorB.copy(alpha = 0.18f * glowScale),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                DockStyle.GLASS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.86f)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
                DockStyle.MINIMAL -> { /* nothing behind the medallion */ }
            }

            if (iconShape == IconShape.RAW) {
                GodSigil(
                    god = god,
                    tint = medallionA,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer(
                            scaleX = pressScale,
                            scaleY = pressScale
                        )
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
                        .clip(medallionShape!!)
                        .background(Brush.linearGradient(listOf(medallionA, medallionB)))
                ) {
                    GodSigil(
                        god = god,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            GodBadge(badgeCount = badgeCount, style = badgeStyle)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = god.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = assignedLabel ?: "hold to assign",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun GodBadge(badgeCount: Int, style: BadgeStyle) {
    if (badgeCount <= 0) return
    when (style) {
        BadgeStyle.DOT -> {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(EmberOrange)
            )
        }
        BadgeStyle.COUNT -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2118))
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        BadgeStyle.GLOW -> {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(EmberOrange.copy(alpha = 0.35f))
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(EmberOrange)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SideGodButton(
    god: God,
    badgeCount: Int,
    animationsEnabled: Boolean,
    iconShape: IconShape,
    badgeStyle: BadgeStyle,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape: Shape? = when (iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUIRCLE -> RoundedCornerShape(14.dp)
        IconShape.HEXAGON -> HexagonShape
        IconShape.RAW -> null
    }
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
            .padding(vertical = 6.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        if (shape == null) {
            GodSigil(
                god = god,
                tint = god.colorA,
                modifier = Modifier
                    .size(26.dp)
                    .padding(8.dp)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(god.colorA, god.colorB)))
            ) {
                GodSigil(
                    god = god,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (badgeCount > 0) {
            val dot = badgeStyle == BadgeStyle.DOT
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (dot) 10.dp else 18.dp)
                    .clip(CircleShape)
                    .background(EmberOrange)
            ) {
                if (!dot) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
