package com.apexforge.godlauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apexforge.godlauncher.model.AppTransition
import com.apexforge.godlauncher.model.BadgeStyle
import com.apexforge.godlauncher.model.CommandBarStyle
import com.apexforge.godlauncher.model.DockPosition
import com.apexforge.godlauncher.model.DockStyle
import com.apexforge.godlauncher.model.DragonBehavior
import com.apexforge.godlauncher.model.DragonPosition
import com.apexforge.godlauncher.model.DragonSkin
import com.apexforge.godlauncher.model.GestureAction
import com.apexforge.godlauncher.model.IconShape
import com.apexforge.godlauncher.model.LoopId
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.model.PageIndicatorStyle
import com.apexforge.godlauncher.model.PageTransition
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.model.PantheonPreset
import com.apexforge.godlauncher.model.ParticleType
import com.apexforge.godlauncher.model.Profile
import com.apexforge.godlauncher.model.SearchBarPosition
import com.apexforge.godlauncher.model.SearchBarStyle

private fun pretty(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun parseHex(hex: String): Color? = try {
    val h = hex.trim().removePrefix("#")
    val full = when (h.length) {
        6 -> "FF$h"
        8 -> h
        else -> return null
    }
    require(full.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
    Color(full.toLong(16))
} catch (_: Exception) {
    null
}

/**
 * Theme Engine: the full mod catalog as a real Settings UI. Collapsible
 * sections per category, every control bound live to ModStore — nothing
 * here needs a rebuild. Plus showroom mode, theme export/import, and the
 * profile switcher.
 */
@Composable
fun ThemeEngineScreen(
    config: PantheonConfig,
    onConfigChange: (PantheonConfig) -> Unit,
    onProfileSelect: (Profile) -> Unit,
    onPresetApply: (PantheonPreset) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var openSections by remember { mutableStateOf(setOf("Presets & Profiles")) }
    fun toggle(section: String) {
        openSections = if (section in openSections) openSections - section else openSections + section
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A09).copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Theme Engine",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            item {
                SectionCard(
                    title = "Presets & Profiles",
                    open = "Presets & Profiles" in openSections,
                    onToggle = { toggle("Presets & Profiles") }
                ) {
                    Text(
                        "Presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    EnumChips(
                        options = PantheonPreset.entries.toList(),
                        selected = null,
                        onSelect = onPresetApply,
                        label = { pretty(it.name) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    EnumChips(
                        options = Profile.entries.toList(),
                        selected = config.activeProfile,
                        onSelect = onProfileSelect,
                        label = { pretty(it.name) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchRow(
                        title = "Showroom mode",
                        subtitle = "Cycles presets every ${config.showroomSeconds}s — built for demos",
                        checked = config.showroomEnabled,
                        onChange = { onConfigChange(config.copy(showroomEnabled = it)) }
                    )
                    if (config.showroomEnabled) {
                        SliderRow(
                            label = "Cycle seconds",
                            value = config.showroomSeconds.toFloat(),
                            range = 3f..60f,
                            onChange = { onConfigChange(config.copy(showroomSeconds = it.toInt())) },
                            format = { "${it.toInt()}s" }
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Backgrounds",
                    open = "Backgrounds" in openSections,
                    onToggle = { toggle("Backgrounds") }
                ) {
                    EnumChips(
                        options = LoopId.entries.toList(),
                        selected = config.loopId,
                        onSelect = { onConfigChange(config.copy(loopId = it)) },
                        label = { pretty(it.name) }
                    )
                    SliderRow("Loop speed", config.loopSpeed, 0.25f..2f,
                        { onConfigChange(config.copy(loopSpeed = it)) }, { "%.2fx".format(it) })
                    SliderRow("Loop density", config.loopDensity, 0f..1f,
                        { onConfigChange(config.copy(loopDensity = it)) }, { "%d%%".format((it * 100).toInt()) })
                    SliderRow("Hue shift", config.hueShift, 0f..360f,
                        { onConfigChange(config.copy(hueShift = it)) }, { "%d°".format(it.toInt()) })
                    EnumChips(
                        options = ParticleType.entries.toList(),
                        selected = config.particleType,
                        onSelect = { onConfigChange(config.copy(particleType = it)) },
                        label = { pretty(it.name) }
                    )
                    SliderRow("Particle density", config.particleDensity, 0f..1f,
                        { onConfigChange(config.copy(particleDensity = it)) }, { "%d%%".format((it * 100).toInt()) })
                    SliderRow("Particle speed", config.particleSpeed, 0.25f..2f,
                        { onConfigChange(config.copy(particleSpeed = it)) }, { "%.2fx".format(it) })
                    HexRow("Particle color", config.particleColor,
                        { onConfigChange(config.copy(particleColor = it)) })
                    SliderRow("Vignette", config.vignette, 0f..1f,
                        { onConfigChange(config.copy(vignette = it)) }, { "%d%%".format((it * 100).toInt()) })
                    SliderRow("Dim level", config.dimLevel, 0f..1f,
                        { onConfigChange(config.copy(dimLevel = it)) }, { "%d%%".format((it * 100).toInt()) })
                    SliderRow("Blur level", config.blurLevel, 0f..1f,
                        { onConfigChange(config.copy(blurLevel = it)) }, { "%d%%".format((it * 100).toInt()) })
                }
            }

            item {
                SectionCard(
                    title = "Layout",
                    open = "Layout" in openSections,
                    onToggle = { toggle("Layout") }
                ) {
                    SliderRow("Drawer grid columns", config.gridCols.toFloat(), 3f..6f,
                        { onConfigChange(config.copy(gridCols = it.toInt())) }, { it.toInt().toString() })
                    SliderRow("Drawer grid rows", config.gridRows.toFloat(), 3f..7f,
                        { onConfigChange(config.copy(gridRows = it.toInt())) }, { it.toInt().toString() })
                    SliderRow("Icon size", config.iconSize, 0.7f..1.4f,
                        { onConfigChange(config.copy(iconSize = it)) }, { "%.2fx".format(it) })
                    SwitchRow("App labels", "Show labels under drawer icons",
                        config.labelsVisible, { onConfigChange(config.copy(labelsVisible = it)) })
                    SliderRow("Label size", config.labelSize, 0.7f..1.4f,
                        { onConfigChange(config.copy(labelSize = it)) }, { "%.2fx".format(it) })
                    SliderRow("Dock slots", config.dockSlots.toFloat(), 4f..8f,
                        { onConfigChange(config.copy(dockSlots = it.toInt())) }, { it.toInt().toString() })
                    EnumChips(
                        options = DockStyle.entries.toList(),
                        selected = config.dockStyle,
                        onSelect = { onConfigChange(config.copy(dockStyle = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = DockPosition.entries.toList(),
                        selected = config.dockPosition,
                        onSelect = { onConfigChange(config.copy(dockPosition = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = PageIndicatorStyle.entries.toList(),
                        selected = config.pageIndicatorStyle,
                        onSelect = { onConfigChange(config.copy(pageIndicatorStyle = it)) },
                        label = { pretty(it.name) }
                    )
                    SwitchRow("Infinite scroll", "Reserved for the multi-page home",
                        config.infiniteScroll, { onConfigChange(config.copy(infiniteScroll = it)) })
                    Text(
                        "Dock order & membership",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    DockOrderEditor(config = config, onConfigChange = onConfigChange)
                }
            }

            item {
                SectionCard(
                    title = "Icons",
                    open = "Icons" in openSections,
                    onToggle = { toggle("Icons") }
                ) {
                    EnumChips(
                        options = IconShape.entries.toList(),
                        selected = config.iconShape,
                        onSelect = { onConfigChange(config.copy(iconShape = it)) },
                        label = { pretty(it.name) }
                    )
                    SliderRow("Icon glow", config.iconGlow, 0f..1f,
                        { onConfigChange(config.copy(iconGlow = it)) }, { "%d%%".format((it * 100).toInt()) })
                    SwitchRow("Neon tint", "Push god colors toward neon",
                        config.neonTint, { onConfigChange(config.copy(neonTint = it)) })
                    SwitchRow("Tilt effect", "Subtle icon tilt while animating",
                        config.tiltEffect, { onConfigChange(config.copy(tiltEffect = it)) })
                }
            }

            item {
                SectionCard(
                    title = "Color & Type",
                    open = "Color & Type" in openSections,
                    onToggle = { toggle("Color & Type") }
                ) {
                    HexRow("Background", config.colorBackground,
                        { onConfigChange(config.copy(colorBackground = it)) })
                    HexRow("Accent", config.colorAccent,
                        { onConfigChange(config.copy(colorAccent = it)) })
                    HexRow("Text", config.colorText,
                        { onConfigChange(config.copy(colorText = it)) })
                    HexRow("Dock", config.colorDock,
                        { onConfigChange(config.copy(colorDock = it)) })
                    HexRow("God ring", config.colorGodRing,
                        { onConfigChange(config.copy(colorGodRing = it)) })
                    SliderRow("Font scale", config.fontScale, 0.8f..1.3f,
                        { onConfigChange(config.copy(fontScale = it)) }, { "%.2fx".format(it) })
                    SliderRow("Panel transparency", config.transparency, 0f..1f,
                        { onConfigChange(config.copy(transparency = it)) }, { "%d%%".format((it * 100).toInt()) })
                }
            }

            item {
                SectionCard(
                    title = "Motion",
                    open = "Motion" in openSections,
                    onToggle = { toggle("Motion") }
                ) {
                    SliderRow("Animation speed", config.animationSpeed, 0.25f..2f,
                        { onConfigChange(config.copy(animationSpeed = it)) }, { "%.2fx".format(it) })
                    EnumChips(
                        options = AppTransition.entries.toList(),
                        selected = config.appTransition,
                        onSelect = { onConfigChange(config.copy(appTransition = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = PageTransition.entries.toList(),
                        selected = config.pageTransition,
                        onSelect = { onConfigChange(config.copy(pageTransition = it)) },
                        label = { pretty(it.name) }
                    )
                    SwitchRow("Dock pulse wave", "Breathing halo on the god dock",
                        config.dockPulseWave, { onConfigChange(config.copy(dockPulseWave = it)) })
                }
            }

            item {
                SectionCard(
                    title = "Gestures",
                    open = "Gestures" in openSections,
                    onToggle = { toggle("Gestures") }
                ) {
                    GestureRow("Swipe up", config.gestureSwipeUp,
                        { onConfigChange(config.copy(gestureSwipeUp = it)) })
                    GestureRow("Swipe down", config.gestureSwipeDown,
                        { onConfigChange(config.copy(gestureSwipeDown = it)) })
                    GestureRow("Double tap", config.gestureDoubleTap,
                        { onConfigChange(config.copy(gestureDoubleTap = it)) })
                    GestureRow("Pinch", config.gesturePinch,
                        { onConfigChange(config.copy(gesturePinch = it)) })
                    GestureRow("Two-finger tap", config.gestureTwoFingerTap,
                        { onConfigChange(config.copy(gestureTwoFingerTap = it)) })
                }
            }

            item {
                SectionCard(
                    title = "Search & Badges",
                    open = "Search & Badges" in openSections,
                    onToggle = { toggle("Search & Badges") }
                ) {
                    EnumChips(
                        options = SearchBarStyle.entries.toList(),
                        selected = config.searchBarStyle,
                        onSelect = { onConfigChange(config.copy(searchBarStyle = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = SearchBarPosition.entries.toList(),
                        selected = config.searchBarPosition,
                        onSelect = { onConfigChange(config.copy(searchBarPosition = it)) },
                        label = { pretty(it.name) }
                    )
                    SliderRow("Suggestions", config.suggestionCount.toFloat(), 3f..8f,
                        { onConfigChange(config.copy(suggestionCount = it.toInt())) }, { it.toInt().toString() })
                    EnumChips(
                        options = BadgeStyle.entries.toList(),
                        selected = config.badgeStyle,
                        onSelect = { onConfigChange(config.copy(badgeStyle = it)) },
                        label = { pretty(it.name) }
                    )
                }
            }

            item {
                SectionCard(
                    title = "Dragon",
                    open = "Dragon" in openSections,
                    onToggle = { toggle("Dragon") }
                ) {
                    EnumChips(
                        options = DragonSkin.entries.toList(),
                        selected = config.dragonSkin,
                        onSelect = { onConfigChange(config.copy(dragonSkin = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = DragonBehavior.entries.toList(),
                        selected = config.dragonBehavior,
                        onSelect = { onConfigChange(config.copy(dragonBehavior = it)) },
                        label = { pretty(it.name) }
                    )
                    EnumChips(
                        options = DragonPosition.entries.toList(),
                        selected = config.dragonPosition,
                        onSelect = { onConfigChange(config.copy(dragonPosition = it)) },
                        label = { pretty(it.name) }
                    )
                    SliderRow("Dragon size", config.dragonSize, 0.6f..1.6f,
                        { onConfigChange(config.copy(dragonSize = it)) }, { "%.2fx".format(it) })
                }
            }

            item {
                SectionCard(
                    title = "Command Bar",
                    open = "Command Bar" in openSections,
                    onToggle = { toggle("Command Bar") }
                ) {
                    SwitchRow("Visible", "Phoenix command bar on the home screen",
                        config.commandBarVisible, { onConfigChange(config.copy(commandBarVisible = it)) })
                    EnumChips(
                        options = CommandBarStyle.entries.toList(),
                        selected = config.commandBarStyle,
                        onSelect = { onConfigChange(config.copy(commandBarStyle = it)) },
                        label = { pretty(it.name) }
                    )
                }
            }

            item {
                SectionCard(
                    title = "Quantum View",
                    open = "Quantum View" in openSections,
                    onToggle = { toggle("Quantum View") }
                ) {
                    SwitchRow(
                        title = "Quantum agent view",
                        subtitle = "Living constellation of the 8 gods instead of the classic dock",
                        checked = config.quantumViewEnabled,
                        onChange = { onConfigChange(config.copy(quantumViewEnabled = it)) }
                    )
                }
            }

            item {
                SectionCard(
                    title = "Import / Export",
                    open = "Import / Export" in openSections,
                    onToggle = { toggle("Import / Export") }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onExport,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE25822)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export theme", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onImport,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import theme", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Export writes a JSON file to the app cache and opens the share sheet. " +
                            "Import reads a .json theme file and applies it instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161210).copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF5B942),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (open) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            if (open) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EnumChips(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        for (opt in options) {
            val isSel = opt == selected
            FilterChip(
                selected = isSel,
                onClick = { onSelect(opt) },
                label = { Text(label(opt)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE25822),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF241C14),
                    labelColor = Color.White.copy(alpha = 0.75f)
                )
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    format: (Float) -> String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF5B942)
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFE25822),
                activeTrackColor = Color(0xFFE25822)
            )
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HexRow(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val preview = parseHex(text)
        val error = preview == null
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(preview ?: Color.Transparent)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            if (error) {
                Text(
                    text = "Use #RRGGBB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF87171)
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                parseHex(it)?.let { _ -> onChange(it) }
            },
            singleLine = true,
            modifier = Modifier.width(130.dp),
            isError = error
        )
    }
}

@Composable
private fun GestureRow(
    label: String,
    selected: GestureAction,
    onSelect: (GestureAction) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        EnumChips(
            options = GestureAction.entries.toList(),
            selected = selected,
            onSelect = onSelect,
            label = { pretty(it.name) }
        )
    }
}

@Composable
private fun DockOrderEditor(
    config: PantheonConfig,
    onConfigChange: (PantheonConfig) -> Unit
) {
    val order = config.dockOrder.filter { id -> PANTHEON.any { it.id == id } } +
        PANTHEON.map { it.id }.filter { it !in config.dockOrder }
    order.forEachIndexed { index, godId ->
        val god = PANTHEON.first { it.id == godId }
        val hidden = godId in config.dockHidden
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = god.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hidden) Color.White.copy(alpha = 0.35f) else Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (index > 0) {
                        val next = order.toMutableList()
                        next[index] = next[index - 1].also { next[index - 1] = next[index] }
                        onConfigChange(config.copy(dockOrder = next))
                    }
                },
                enabled = index > 0
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move ${god.name} up",
                    tint = Color.White.copy(alpha = if (index > 0) 0.7f else 0.2f)
                )
            }
            IconButton(
                onClick = {
                    if (index < order.lastIndex) {
                        val next = order.toMutableList()
                        next[index] = next[index + 1].also { next[index + 1] = next[index] }
                        onConfigChange(config.copy(dockOrder = next))
                    }
                },
                enabled = index < order.lastIndex
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move ${god.name} down",
                    tint = Color.White.copy(alpha = if (index < order.lastIndex) 0.7f else 0.2f)
                )
            }
            IconButton(onClick = {
                onConfigChange(
                    config.copy(
                        dockHidden = if (hidden) config.dockHidden - godId
                        else config.dockHidden + godId
                    )
                )
            }) {
                Icon(
                    if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (hidden) "Show ${god.name}" else "Hide ${god.name}",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
