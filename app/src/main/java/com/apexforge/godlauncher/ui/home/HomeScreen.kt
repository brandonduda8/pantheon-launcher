package com.apexforge.godlauncher.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.ContactResult
import com.apexforge.godlauncher.data.ContactSearch
import com.apexforge.godlauncher.data.MathEval
import com.apexforge.godlauncher.model.AppInfo
import com.apexforge.godlauncher.model.DockPosition
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.model.SearchBarPosition
import com.apexforge.godlauncher.model.SearchBarStyle
import com.apexforge.godlauncher.ui.components.AppRow
import com.apexforge.godlauncher.ui.phoenix.PhoenixCommandBar
import com.apexforge.godlauncher.ui.quantum.QuantumView
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import com.apexforge.godlauncher.ui.theme.VoidPanel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * Home screen v3: clock, universal search, smart suggestions, Phoenix
 * command bar, and the god layer — either the classic GodDock or the
 * QuantumView constellation. Every gesture (swipe up/down, double-tap,
 * pinch, two-finger tap) is mapped through PantheonConfig; dock style,
 * position, order and membership are all swappable without a rebuild.
 *
 * Long-pressing empty space still offers the quantum wallpaper dialog.
 * A long-press on a quantum orb opens the god quick sheet instead — the
 * wallpaper dialog is suppressed via the orb-guard timestamp.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    godLabels: Map<String, String?>,
    godPackages: Map<String, String?>,
    badgeCounts: Map<String, Int>,
    suggestions: List<AppInfo>,
    contactsGranted: Boolean,
    onRequestContactsPermission: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onSuggestionLaunch: (String) -> Unit = onLaunchApp,
    onGodClick: (God) -> Unit,
    onGodLongPress: (God) -> Unit,
    onOrbLongPress: (God) -> Unit = onGodLongPress,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhoenix: () -> Unit,
    onOpenSystem: () -> Unit = {},
    onPhoenixPrompt: (String) -> Unit,
    onSetWallpaper: () -> Unit,
    animationsEnabled: Boolean = true,
    splashDone: Boolean = true,
    config: PantheonConfig = PantheonConfig(),
    focusSearchSignal: Int = 0,
    onSwipeUp: () -> Unit = onOpenDrawer,
    onSwipeDown: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onPinch: () -> Unit = {},
    onTwoFingerTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var orbLongPressAt by remember { mutableLongStateOf(0L) }
    val searchFocusRequester = remember { FocusRequester() }
    val trimmed = query.trim()

    LaunchedEffect(focusSearchSignal) {
        if (focusSearchSignal > 0) searchFocusRequester.requestFocus()
    }

    // --- Universal search sources ---
    val appResults = remember(apps, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else apps.filter { it.label.contains(trimmed, ignoreCase = true) }.take(8)
    }

    var contacts by remember { mutableStateOf<List<ContactResult>>(emptyList()) }
    LaunchedEffect(trimmed, contactsGranted) {
        contacts = if (contactsGranted && trimmed.isNotEmpty()) {
            ContactSearch.search(context, trimmed)
        } else {
            emptyList()
        }
    }

    val mathResult = remember(trimmed) { MathEval.tryEvaluate(trimmed) }

    val allActions = remember {
        listOf(
            QuickAction(
                "wallpaper", "Set quantum wallpaper", "Apply the ember mandala",
                Icons.Filled.Wallpaper, listOf("wallpaper", "background", "theme")
            ),
            QuickAction(
                "settings", "Open settings", "Pantheon settings",
                Icons.Filled.Settings, listOf("settings", "preferences", "options")
            ),
            QuickAction(
                "drawer", "All apps", "Open the app drawer",
                Icons.Filled.Apps, listOf("drawer", "all apps", "apps", "app list")
            ),
            QuickAction(
                "phoenix", "Ask Phoenix", "Open Phoenix chat",
                Icons.Filled.Whatshot, listOf("phoenix", "ai", "chat", "assistant")
            )
        )
    }
    val actionHandlers = remember(onSetWallpaper, onOpenSettings, onOpenDrawer, onOpenPhoenix) {
        mapOf(
            "wallpaper" to onSetWallpaper,
            "settings" to onOpenSettings,
            "drawer" to onOpenDrawer,
            "phoenix" to onOpenPhoenix
        )
    }
    val matchedActions = remember(trimmed) { matchQuickActions(trimmed, allActions) }

    val hasAnyResult = appResults.isNotEmpty() || contacts.isNotEmpty() ||
        matchedActions.isNotEmpty() || mathResult != null
    val showCard = trimmed.isNotEmpty() && (hasAnyResult || !contactsGranted)

    // Dock gods: configured order, minus hidden, capped at dock slots.
    val dockGods = remember(config.dockOrder, config.dockHidden, config.dockSlots) {
        val ordered = config.dockOrder.mapNotNull { id -> PANTHEON.find { it.id == id } } +
            PANTHEON.filter { g -> g.id !in config.dockOrder }
        ordered.filter { it.id !in config.dockHidden }
            .take(config.dockSlots.coerceIn(4, 8))
    }

    fun dial(number: String) {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun text(number: String) {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun copyResult(result: String) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("calculation", result))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun SearchBlock() {
        if (config.searchBarStyle == SearchBarStyle.HIDDEN) return
        val shape = when (config.searchBarStyle) {
            SearchBarStyle.PILL -> RoundedCornerShape(50)
            SearchBarStyle.UNDERLINE -> RoundedCornerShape(0.dp)
            else -> RoundedCornerShape(28.dp)
        }
        val borderless = config.searchBarStyle == SearchBarStyle.UNDERLINE
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps, contacts, actions…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (borderless) Color.Transparent
                else MaterialTheme.colorScheme.outline,
                focusedBorderColor = EmberOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .focusRequester(searchFocusRequester)
        )

        if (showCard) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = VoidPanel.copy(alpha = config.transparency)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .heightIn(max = 340.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (appResults.isNotEmpty()) {
                        item { ResultSectionHeader("Apps") }
                        items(appResults, key = { it.packageName }) { app ->
                            AppRow(app = app, onClick = {
                                query = ""
                                onLaunchApp(app.packageName)
                            }, animationsEnabled = animationsEnabled)
                        }
                    }
                    if (contacts.isNotEmpty()) {
                        item { ResultSectionHeader("Contacts") }
                        items(contacts, key = { it.name }) { contact ->
                            ContactRow(
                                contact = contact,
                                onCall = {
                                    contact.phoneNumber?.let { dial(it) }
                                },
                                onText = {
                                    contact.phoneNumber?.let { text(it) }
                                }
                            )
                        }
                    }
                    if (matchedActions.isNotEmpty()) {
                        item { ResultSectionHeader("Quick actions") }
                        items(matchedActions, key = { it.id }) { action ->
                            QuickActionRow(action = action, onClick = {
                                query = ""
                                actionHandlers[action.id]?.invoke()
                            })
                        }
                    }
                    if (mathResult != null) {
                        item { ResultSectionHeader("Calculator") }
                        item {
                            val formatted = MathEval.format(mathResult)
                            MathRow(
                                expression = trimmed,
                                result = formatted,
                                onCopy = { copyResult(formatted) }
                            )
                        }
                    }
                    if (!contactsGranted) {
                        item {
                            ContactsOptInRow(onClick = onRequestContactsPermission)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GodLayer() {
        if (config.dockPosition == DockPosition.HIDDEN) return
        if (config.quantumViewEnabled) {
            QuantumView(
                config = config,
                godPackages = godPackages,
                animationsEnabled = animationsEnabled,
                onGodClick = onGodClick,
                onGodLongPress = { god ->
                    orbLongPressAt = System.currentTimeMillis()
                    onOrbLongPress(god)
                },
                started = splashDone,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            GodDock(
                godLabels = godLabels,
                godPackages = godPackages,
                badgeCounts = badgeCounts,
                onGodClick = onGodClick,
                onGodLongPress = onGodLongPress,
                animationsEnabled = animationsEnabled,
                dockStyle = config.dockStyle,
                iconShape = config.iconShape,
                iconGlow = config.iconGlow,
                neonTint = config.neonTint,
                tiltEffect = config.tiltEffect,
                dockPulseWave = config.dockPulseWave,
                badgeStyle = config.badgeStyle,
                gods = dockGods,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    @Composable
    fun MainColumn(colModifier: Modifier = Modifier) {
        Column(
            modifier = colModifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            if (config.dockPosition == DockPosition.TOP) {
                GodLayer()
                Spacer(modifier = Modifier.height(8.dp))
            }

            ClockWidget(started = splashDone)

            Spacer(modifier = Modifier.height(12.dp))

            if (config.searchBarPosition == SearchBarPosition.TOP) {
                SearchBlock()
            }

            Spacer(modifier = Modifier.weight(1f))

            SmartSuggestions(
                suggestions = suggestions,
                onLaunchApp = onSuggestionLaunch
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (config.commandBarVisible) {
                PhoenixCommandBar(
                    onSubmit = onPhoenixPrompt,
                    onOpenChat = onOpenPhoenix,
                    style = config.commandBarStyle
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (config.searchBarPosition == SearchBarPosition.BOTTOM) {
                SearchBlock()
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (config.dockPosition == DockPosition.BOTTOM) {
                GodLayer()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        Icons.Filled.Apps,
                        contentDescription = "App drawer",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSystem) {
                    Icon(
                        Icons.Filled.Dns,
                        contentDescription = "Pantheon system",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .twoFingerGestures(onPinch = onPinch, onTwoFingerTap = onTwoFingerTap)
            .modSwipeGestures(onSwipeDown = onSwipeDown, onSwipeUp = onSwipeUp)
            .combinedClickable(
                onClick = { /* empty space tap: dismiss search */ query = "" },
                onLongClick = {
                    // A long-press on a quantum orb opens the god sheet —
                    // don't also pop the wallpaper dialog.
                    if (System.currentTimeMillis() - orbLongPressAt > 1200) {
                        showWallpaperDialog = true
                    }
                },
                onDoubleClick = onDoubleTap
            )
    ) {
        val sideDock = config.dockPosition == DockPosition.LEFT ||
            config.dockPosition == DockPosition.RIGHT
        if (sideDock && !config.quantumViewEnabled) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (config.dockPosition == DockPosition.LEFT) {
                    GodDock(
                        godLabels = godLabels,
                        godPackages = godPackages,
                        badgeCounts = badgeCounts,
                        onGodClick = onGodClick,
                        onGodLongPress = onGodLongPress,
                        animationsEnabled = animationsEnabled,
                        dockStyle = config.dockStyle,
                        iconShape = config.iconShape,
                        iconGlow = config.iconGlow,
                        neonTint = config.neonTint,
                        tiltEffect = config.tiltEffect,
                        dockPulseWave = config.dockPulseWave,
                        badgeStyle = config.badgeStyle,
                        gods = dockGods,
                        vertical = true,
                        modifier = Modifier
                            .width(72.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(vertical = 24.dp)
                    )
                }
                MainColumn(Modifier.weight(1f))
                if (config.dockPosition == DockPosition.RIGHT) {
                    GodDock(
                        godLabels = godLabels,
                        godPackages = godPackages,
                        badgeCounts = badgeCounts,
                        onGodClick = onGodClick,
                        onGodLongPress = onGodLongPress,
                        animationsEnabled = animationsEnabled,
                        dockStyle = config.dockStyle,
                        iconShape = config.iconShape,
                        iconGlow = config.iconGlow,
                        neonTint = config.neonTint,
                        tiltEffect = config.tiltEffect,
                        dockPulseWave = config.dockPulseWave,
                        badgeStyle = config.badgeStyle,
                        gods = dockGods,
                        vertical = true,
                        modifier = Modifier
                            .width(72.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(vertical = 24.dp)
                    )
                }
            }
        } else {
            MainColumn()
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Quantum wallpaper") },
            text = { Text("Apply the bundled quantum-astrology mandala as your system wallpaper?") },
            confirmButton = {
                TextButton(onClick = {
                    showWallpaperDialog = false
                    onSetWallpaper()
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Two-finger gesture watcher: two-finger tap, and pinch (two-finger
 * spread). Lives OUTSIDE the single-finger swipe detector in the modifier
 * chain so it sees events first and consumes the gesture when a second
 * finger lands — single-finger taps/swipes pass through untouched.
 */
@Composable
private fun Modifier.twoFingerGestures(
    onPinch: () -> Unit,
    onTwoFingerTap: () -> Unit
): Modifier {
    // Stable detector: callbacks ride rememberUpdatedState so the
    // pointerInput block never restarts mid-gesture on recomposition.
    val latestPinch by rememberUpdatedState(onPinch)
    val latestTwoFingerTap by rememberUpdatedState(onTwoFingerTap)
    return this.pointerInput(Unit) {
        val pinchThresholdPx = 48.dp.toPx()
        awaitEachGesture {
            val down1 = awaitFirstDown()
            val down2 = withTimeoutOrNull(280) {
                var ev = awaitPointerEvent()
                while (ev.changes.none { it.id != down1.id && it.pressed }) {
                    ev = awaitPointerEvent()
                }
                ev.changes.first { it.id != down1.id && it.pressed }
            }
            if (down2 != null) {
                val ids = setOf(down1.id, down2.id)
                var startDist = 0f
                var pinched = false
                while (true) {
                    val ev = awaitPointerEvent()
                    val pressed = ev.changes.filter { it.pressed && it.id in ids }
                    if (pressed.size >= 2) {
                        val d = (pressed[0].position - pressed[1].position).getDistance()
                        if (startDist == 0f) {
                            startDist = d
                        } else if (!pinched && abs(d - startDist) > pinchThresholdPx) {
                            pinched = true
                            latestPinch()
                        }
                    }
                    // Consume: the inner swipe detector sees consumed changes
                    // and stands down, so a pinch never also fires a swipe.
                    ev.changes.forEach { if (it.id in ids) it.consume() }
                    if (ev.changes.none { it.pressed && it.id in ids }) break
                }
                if (!pinched) latestTwoFingerTap()
            }
        }
    }
}

/**
 * Single-finger vertical swipe with horizontal-drag rejection. Aborts
 * (without firing) the moment a change is consumed — e.g. when the outer
 * two-finger watcher claims the gesture.
 */
@Composable
private fun Modifier.modSwipeGestures(
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit
): Modifier {
    val latestDown by rememberUpdatedState(onSwipeDown)
    val latestUp by rememberUpdatedState(onSwipeUp)
    return this.pointerInput(Unit) {
        val thresholdPx = 120.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            val slopChange = awaitTouchSlopOrCancellation(down.id) { _, _ -> }
            if (slopChange != null) {
                var change: PointerInputChange? = slopChange
                var totalX = 0f
                var totalY = 0f
                var aborted = false
                while (change != null) {
                    if (change.isConsumed) {
                        aborted = true
                        break
                    }
                    val delta = change.position - change.previousPosition
                    totalX += delta.x
                    totalY += delta.y
                    change.consume()
                    change = awaitDragOrCancellation(change.id)
                }
                if (!aborted) {
                    val absY = abs(totalY)
                    if (absY > thresholdPx && absY > abs(totalX) * 1.5f) {
                        if (totalY > 0) latestDown() else latestUp()
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = PhoenixGold.copy(alpha = 0.8f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun ContactRow(
    contact: ContactResult,
    onCall: () -> Unit,
    onText: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            contact.phoneNumber?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
            }
        }
        if (contact.phoneNumber != null) {
            IconButton(onClick = onCall) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = EmberOrange
                )
            }
            IconButton(onClick = onText) {
                Icon(
                    Icons.Filled.Message,
                    contentDescription = "Text ${contact.name}",
                    tint = EmberOrange
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(action: QuickAction, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            action.icon,
            contentDescription = action.title,
            tint = EmberOrange,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = action.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = action.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedStar
            )
        }
    }
}

@Composable
private fun MathRow(expression: String, result: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCopy)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Filled.Calculate,
            contentDescription = "Calculation result",
            tint = EmberOrange,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expression,
                style = MaterialTheme.typography.bodySmall,
                color = MutedStar,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "= $result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Tap to copy",
            style = MaterialTheme.typography.labelSmall,
            color = MutedStar
        )
    }
}

@Composable
private fun ContactsOptInRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Filled.Contacts,
            contentDescription = "Search contacts",
            tint = EmberOrange,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "Search contacts", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Allow access so you can call and text people from search",
                style = MaterialTheme.typography.bodySmall,
                color = MutedStar
            )
        }
    }
}
