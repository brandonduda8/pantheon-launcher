package com.apexforge.godlauncher.ui.vortex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.R
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.MotionMode
import com.apexforge.godlauncher.model.VortexPreset
import com.apexforge.godlauncher.model.VortexTokens
import kotlinx.coroutines.delay
import java.util.Date

private val PanelShape = RoundedCornerShape(VortexTokens.RADIUS_PANEL_DP.dp)
private val ControlShape = RoundedCornerShape(VortexTokens.RADIUS_CONTROL_DP.dp)
private val MinTouch = VortexTokens.MIN_TOUCH_TARGET_DP.dp
private val Mono = FontFamily.Monospace

/** OS signals that gate Vortex motion. All event-driven; nothing polls. */
class VortexEnvironment(val resumed: Boolean, val powerSave: Boolean, val reduceMotion: Boolean)

@Composable
fun rememberVortexEnvironment(): VortexEnvironment {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    fun animatorOff() = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

    var resumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var powerSave by remember { mutableStateOf(pm.isPowerSaveMode) }
    var reduceMotion by remember { mutableStateOf(animatorOff()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) {
                powerSave = pm.isPowerSaveMode
                reduceMotion = animatorOff()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { powerSave = pm.isPowerSaveMode }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return VortexEnvironment(resumed, powerSave, reduceMotion)
}

/**
 * Original static wallpaper drawn inside the launcher (never sets the system
 * wallpaper). Balanced/cinematic add one rotating ring layer: rotation is a
 * graphicsLayer property, so the canvas is recorded once and never redrawn
 * per frame. Battery saver, reduced motion, or background force static.
 */
@Composable
fun VortexBackground(
    requested: MotionMode,
    animationsEnabled: Boolean,
    started: Boolean,
    modifier: Modifier = Modifier
) {
    val env = rememberVortexEnvironment()
    val motion = VortexPreset.effectiveMotion(
        requested, animationsEnabled && started, env.powerSave, env.reduceMotion, env.resumed
    )
    Box(modifier.fillMaxSize().background(VortexColors.VoidBlack)) {
        Image(
            painter = painterResource(R.drawable.vortex_ember_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (motion != MotionMode.STATIC) PortalRings(motion)
    }
}

// Wallpaper geometry (tools/vortex/gen_wallpaper.py): 1080x2400, portal at y=40%, ring r=0.30w.
private const val WP_W = 1080f
private const val WP_H = 2400f

@Composable
private fun PortalRings(motion: MotionMode) {
    val cinematic = motion == MotionMode.CINEMATIC
    val transition = rememberInfiniteTransition(label = "vortex")
    val angle = transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (cinematic) 24_000 else 60_000, easing = LinearEasing)),
        label = "spin"
    )
    val glow = if (cinematic) transition.animateFloat(
        0.35f, 0.8f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "glow"
    ) else null
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = maxOf(size.width / WP_W, size.height / WP_H)
                val cy = size.height / 2f + (0.40f - 0.5f) * WP_H * scale
                transformOrigin = TransformOrigin(0.5f, cy / size.height)
                rotationZ = angle.value
                alpha = glow?.value ?: 0.55f
            }
    ) {
        val scale = maxOf(size.width / WP_W, size.height / WP_H)
        val c = Offset(size.width / 2f, size.height / 2f + (0.40f - 0.5f) * WP_H * scale)
        val r = 0.34f * WP_W * scale
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        for (k in 0 until 3) {
            drawArc(
                color = if (k == 0) VortexColors.PlasmaMagenta else VortexColors.ElectricViolet,
                startAngle = k * 120f, sweepAngle = 70f, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2), style = stroke
            )
        }
        val r2 = 0.22f * WP_W * scale
        drawArc(
            color = VortexColors.StormBlue.copy(alpha = 0.6f),
            startAngle = 200f, sweepAngle = 50f, useCenter = false,
            topLeft = Offset(c.x - r2, c.y - r2), size = Size(r2 * 2, r2 * 2), style = stroke
        )
    }
}

/** Genesis Vortex mark: triangular frame around a spiral, monoline. */
@Composable
fun VortexMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        val tri = Path().apply {
            moveTo(w / 2f, w * 0.06f); lineTo(w * 0.94f, w * 0.88f); lineTo(w * 0.06f, w * 0.88f); close()
        }
        drawPath(tri, VortexColors.ElectricViolet, style = stroke)
        val spiral = Path()
        val cx = w / 2f
        val cy = w * 0.6f
        for (i in 0..48) {
            val t = i / 48f
            val a = t * 4.5f * Math.PI.toFloat()
            val rr = w * 0.03f + t * w * 0.2f
            val x = cx + rr * kotlin.math.cos(a)
            val y = cy + rr * kotlin.math.sin(a)
            if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
        }
        drawPath(spiral, VortexColors.PlasmaMagenta, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
    }
}

/** Clock/date header. Ticks once per minute (aligned), only while resumed. */
@Composable
fun VortexHeader(started: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val env = rememberVortexEnvironment()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(env.resumed, started) {
        while (env.resumed && started) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L + 50L)
        }
    }
    val date = Date(now)
    val time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", date).toString()
    val day = DateFormat.format("EEE · MMM d", date).toString().uppercase()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 20.dp)
            .clearAndSetSemantics { contentDescription = "Time $time, ${DateFormat.format("EEEE MMMM d", date)}" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VortexMark(Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "GENESIS VORTEX",
                style = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 3.sp, color = VortexColors.SilverMist)
            )
        }
        Text(
            time,
            style = TextStyle(
                fontSize = 72.sp, fontWeight = FontWeight.Thin, letterSpacing = 2.sp,
                color = VortexColors.TextPrimary
            )
        )
        Text(
            day,
            style = TextStyle(fontFamily = Mono, fontSize = 13.sp, letterSpacing = 2.sp, color = VortexColors.TextMuted)
        )
    }
}

/** Home widgets: Ember companion entry, device vitals, Genesis + Solon entries. */
@Composable
fun VortexWidgetRow(
    onOpenEmber: () -> Unit,
    onOpenGenesis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            EmberWidget(onOpenEmber, Modifier.weight(1.3f))
            VitalsWidget(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            VortexChip("GENESIS HUB", "Open", VortexColors.ElectricViolet, onOpenGenesis, Modifier.weight(1f))
            VortexChip("SOLON", "Planned", VortexColors.StormBlue, null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, onClick: (() -> Unit)?, desc: String, content: @Composable () -> Unit) {
    Box(
        modifier
            .heightIn(min = MinTouch)
            .background(VortexColors.SurfaceElevated.copy(alpha = 0.88f), PanelShape)
            .border(1.dp, VortexColors.Border, PanelShape)
            .let { m -> if (onClick != null) m.clickable(role = Role.Button, onClick = onClick) else m }
            .clearAndSetSemantics { contentDescription = desc }
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun EmberWidget(onOpen: () -> Unit, modifier: Modifier) {
    Panel(modifier, onOpen, "Ember companion. Ready. Opens companion chat.") {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = VortexColors.DragonEmber, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("EMBER", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = VortexColors.TextPrimary))
            }
            Spacer(Modifier.size(4.dp))
            Text("Companion · tap to talk", style = TextStyle(fontSize = 12.sp, color = VortexColors.TextMuted))
            Spacer(Modifier.size(6.dp))
            Text("● READY", style = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = VortexColors.EmberGold))
        }
    }
}

@Composable
private fun VitalsWidget(modifier: Modifier) {
    val context = LocalContext.current
    val env = rememberVortexEnvironment()
    var level by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    fun read(i: Intent?) {
        i ?: return
        val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        level = if (l >= 0 && s > 0) l * 100 / s else -1
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    }
    // Sticky battery broadcast, registered only while the launcher is resumed.
    DisposableEffect(env.resumed) {
        if (!env.resumed) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = read(i)
        }
        read(
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
            )
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val pct = if (level >= 0) "$level%" else "—"
    val chargeText = if (charging) "Charging" else "On battery"
    val saverText = if (env.powerSave) "Saver on" else "Saver off"
    Panel(modifier, null, "Battery $pct, $chargeText, battery $saverText") {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd, null,
                    tint = VortexColors.StormBlue, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(pct, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Light, color = VortexColors.TextPrimary))
            }
            Spacer(Modifier.size(4.dp))
            Text(chargeText, style = TextStyle(fontSize = 12.sp, color = VortexColors.TextMuted))
            Text(saverText.uppercase(), style = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = VortexColors.SilverMist))
        }
    }
}

@Composable
private fun VortexChip(label: String, status: String, accent: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)?, modifier: Modifier) {
    val desc = if (onClick != null) "$label. $status." else "$label. $status, not available yet."
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = MinTouch)
            .background(VortexColors.Surface.copy(alpha = 0.85f), ControlShape)
            .border(1.dp, accent.copy(alpha = if (onClick != null) 0.9f else 0.4f), ControlShape)
            .let { m -> if (onClick != null) m.clickable(role = Role.Button, onClick = onClick) else m }
            .clearAndSetSemantics { contentDescription = desc }
            .padding(horizontal = 12.dp)
    ) {
        Text(label, style = TextStyle(fontFamily = Mono, fontSize = 12.sp, letterSpacing = 1.sp, color = VortexColors.TextPrimary), modifier = Modifier.weight(1f))
        Text(status.uppercase(), style = TextStyle(fontFamily = Mono, fontSize = 10.sp, color = VortexColors.TextMuted))
    }
}

/** Dock medallions in Vortex: violet monoline family, ember for creation, blue for watch. */
fun vortexGods(gods: List<God>): List<God> = gods.map { g ->
    when (g.id) {
        "hephaestus" -> g.copy(colorA = VortexColors.DragonEmber, colorB = VortexColors.AbyssPurple)
        "argus" -> g.copy(colorA = VortexColors.StormBlue, colorB = VortexColors.AbyssPurple)
        else -> g.copy(colorA = VortexColors.ElectricViolet, colorB = VortexColors.AbyssPurple)
    }
}

/** Banner shown while previewing (nothing is saved until Apply). */
@Composable
fun VortexPreviewBanner(onApply: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(VortexColors.SurfaceElevated, PanelShape)
            .border(1.dp, VortexColors.PlasmaMagenta, PanelShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            "PREVIEW · ${VortexTokens.PRESET_NAME}",
            style = TextStyle(fontFamily = Mono, fontSize = 12.sp, color = VortexColors.TextPrimary),
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouch)) {
            Text("Cancel", color = VortexColors.TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onApply,
            colors = ButtonDefaults.buttonColors(containerColor = VortexColors.ElectricViolet),
            modifier = Modifier.heightIn(min = MinTouch)
        ) { Text("Apply", color = VortexColors.TextPrimary) }
    }
}

/** Theme Engine card: Preview / Apply / Undo / Restore default + motion mode. */
@Composable
fun VortexThemeCard(
    active: Boolean,
    hasUndo: Boolean,
    motionMode: MotionMode,
    onPreview: () -> Unit,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onRestoreDefault: () -> Unit,
    onMotionMode: (MotionMode) -> Unit,
    onChooseHomeApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(VortexColors.Surface, PanelShape)
            .border(BorderStroke(1.dp, VortexColors.Border), PanelShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VortexMark(Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                VortexTokens.PRESET_NAME,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = VortexColors.TextPrimary),
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text(
                if (active) "ACTIVE" else "OFF",
                style = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = if (active) VortexColors.EmberGold else VortexColors.TextMuted)
            )
        }
        Text(
            "Electric-purple Genesis skin. Preview first; Apply saves; Undo returns to exactly what you had. Dock apps are never changed.",
            style = TextStyle(fontSize = 13.sp, color = VortexColors.TextMuted)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreview, enabled = !active, modifier = Modifier.weight(1f).heightIn(min = MinTouch)) {
                Text("Preview", color = VortexColors.TextPrimary)
            }
            Button(
                onClick = onApply, enabled = !active,
                colors = ButtonDefaults.buttonColors(containerColor = VortexColors.ElectricViolet),
                modifier = Modifier.weight(1f).heightIn(min = MinTouch)
            ) { Text(if (active) "Applied" else "Apply", color = VortexColors.TextPrimary) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo, enabled = hasUndo, modifier = Modifier.weight(1f).heightIn(min = MinTouch)) {
                Text("Undo", color = VortexColors.TextPrimary)
            }
            OutlinedButton(onClick = onRestoreDefault, modifier = Modifier.weight(1f).heightIn(min = MinTouch)) {
                Text("Restore default", color = VortexColors.TextPrimary)
            }
        }
        if (active) {
            Text("Motion", style = TextStyle(fontFamily = Mono, fontSize = 12.sp, color = VortexColors.SilverMist))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MotionMode.entries.forEach { m ->
                    val selected = m == motionMode
                    val label = m.name.lowercase().replaceFirstChar { it.uppercase() }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MinTouch)
                            .background(if (selected) VortexColors.AbyssPurple else VortexColors.Surface, ControlShape)
                            .border(1.dp, if (selected) VortexColors.PlasmaMagenta else VortexColors.Border, ControlShape)
                            .clickable(role = Role.RadioButton) { onMotionMode(m) }
                            .semantics { contentDescription = "$label motion${if (selected) ", selected" else ""}" }
                    ) {
                        Text((if (selected) "✓ " else "") + label, color = VortexColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }
            Text(
                "Battery saver, system \"remove animations\", and the launcher animation switch force Static automatically.",
                style = TextStyle(fontSize = 12.sp, color = VortexColors.TextMuted)
            )
        }
        OutlinedButton(onClick = onChooseHomeApp, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
            Text("Choose default home app (system screen)", color = VortexColors.TextPrimary)
        }
    }
}
