package com.apexforge.godlauncher

import android.Manifest
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.apexforge.godlauncher.data.ActivityBus
import com.apexforge.godlauncher.data.AgentBridge
import com.apexforge.godlauncher.data.AppRepository
import com.apexforge.godlauncher.data.LaunchHistory
import com.apexforge.godlauncher.data.ModStore
import com.apexforge.godlauncher.data.NodeProbe
import com.apexforge.godlauncher.data.NodeReach
import com.apexforge.godlauncher.data.PantheonStore
import com.apexforge.godlauncher.data.PhoenixAction
import com.apexforge.godlauncher.data.PhoenixBrain
import com.apexforge.godlauncher.data.PhoenixBriefEngine
import com.apexforge.godlauncher.data.PhoenixNotificationService
import com.apexforge.godlauncher.data.PhoenixProposal
import com.apexforge.godlauncher.data.PhoenixProposalStore
import com.apexforge.godlauncher.data.SnapshotStore
import com.apexforge.godlauncher.data.SystemSnapshot
import com.apexforge.godlauncher.service.PhoenixService
import com.apexforge.godlauncher.model.AppInfo
import com.apexforge.godlauncher.model.AppTransition
import com.apexforge.godlauncher.model.DragonPosition
import com.apexforge.godlauncher.model.GestureAction
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.ui.background.LoopBackground
import com.apexforge.godlauncher.ui.components.AppPickerDialog
import com.apexforge.godlauncher.ui.drawer.AppDrawer
import com.apexforge.godlauncher.ui.effects.DragonMascot
import com.apexforge.godlauncher.ui.effects.PhoenixFX
import com.apexforge.godlauncher.ui.effects.SplashIgnition
import com.apexforge.godlauncher.ui.home.HomeScreen
import com.apexforge.godlauncher.ui.nodes.GenesisScreen
import com.apexforge.godlauncher.ui.phoenix.ChatMessage
import com.apexforge.godlauncher.ui.phoenix.PhoenixCompanion
import com.apexforge.godlauncher.ui.phoenix.PhoenixScreen
import com.apexforge.godlauncher.ui.quantum.GodQuickSheet
import com.apexforge.godlauncher.ui.settings.SettingsScreen
import com.apexforge.godlauncher.ui.forge.ForgeScreen
import com.apexforge.godlauncher.ui.settings.ThemeEngineScreen
import com.apexforge.godlauncher.ui.theme.PantheonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

private sealed interface Screen {
    data object Home : Screen
    data object Drawer : Screen
    data object Settings : Screen
    data object Phoenix : Screen
    data object ThemeEngine : Screen
    data object Forge : Screen
    data object System : Screen
}

class MainActivity : ComponentActivity() {

    companion object {
        /** Extra: open Phoenix chat (service overlay / notification taps). */
        const val EXTRA_OPEN_PHOENIX = "open_phoenix"
    }

    private val store by lazy { PantheonStore(applicationContext) }
    private val modStore by lazy { ModStore(applicationContext) }
    private val repo by lazy { AppRepository(applicationContext) }
    private val brain by lazy { PhoenixBrain(applicationContext) }
    private val launchHistory by lazy { LaunchHistory(applicationContext) }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PHOENIX, false)) {
            ActivityBus.requestNav("phoenix")
        }
    }

    override fun onResume() {
        super.onResume()
        // Brandon may have granted the overlay permission in the system
        // screen while we were paused — nudge the running service so the
        // floating companion appears without extra taps.
        if (PhoenixService.running) PhoenixService.poke(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Always-on Phoenix: if Brandon left "Keep Phoenix alive" on,
        // restart the presence service whenever the launcher opens
        // (Android 12+ forbids starting it silently at boot).
        if (PhoenixService.keepAliveEnabled(this) && !PhoenixService.running) {
            try {
                PhoenixService.start(this)
            } catch (_: Exception) {
            }
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_PHOENIX, false) == true) {
            ActivityBus.requestNav("phoenix")
        }

        setContent {
            PantheonTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val baseDensity = LocalDensity.current

                // v3 mod engine: the whole launcher theme as one live config.
                val config by modStore.config
                    .collectAsStateWithLifecycle(initialValue = PantheonConfig())

                // Installed apps, re-queried every time we return to the foreground
                // so newly installed apps show up without a restart.
                var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
                var reloadTick by remember { mutableIntStateOf(0) }
                // In-context READ_CONTACTS grant for universal search (must be
                // declared before the reload effect that reads it).
                var contactsGranted by remember { mutableStateOf(false) }
                val requestContactsPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> contactsGranted = granted }
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) reloadTick++
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(reloadTick) {
                    apps = repo.loadApps()
                    // Refresh the contacts permission grant (e.g. after the
                    // user changes it in system settings).
                    contactsGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                }

                val animationsEnabled by store.animationsEnabled
                    .collectAsStateWithLifecycle(initialValue = true)
                val godPackages by store.godPackages
                    .collectAsStateWithLifecycle(initialValue = emptyMap())
                val godLabels by store.godLabels
                    .collectAsStateWithLifecycle(initialValue = emptyMap())
                val phoenixApiKey by store.phoenixApiKey
                    .collectAsStateWithLifecycle(initialValue = "")
                val forgeGatewayUrl by store.forgeGatewayUrl
                    .collectAsStateWithLifecycle(initialValue = "")
                val forgeApiKey by store.forgeApiKey
                    .collectAsStateWithLifecycle(initialValue = "")

                // Pantheon state snapshot — bundled probe from Zane's machine.
                // Every UI surface that shows it must label it STORED SNAPSHOT.
                var systemSnapshot by remember { mutableStateOf<SystemSnapshot?>(null) }
                LaunchedEffect(Unit) {
                    systemSnapshot = withContext(Dispatchers.IO) {
                        SnapshotStore.load(applicationContext)
                    }
                }
                val companionVisible by store.companionVisible
                    .collectAsStateWithLifecycle(initialValue = true)
                // Always-on Phoenix: Brandon's taps. Refreshed whenever
                // Settings opens (the system can kill the service).
                var presenceOn by remember {
                    mutableStateOf(
                        PhoenixService.keepAliveEnabled(applicationContext) ||
                            PhoenixService.running
                    )
                }
                // Forge drafts stashed by agents through the Agent Bridge.
                var forgeDraft by remember { mutableStateOf<AgentBridge.ForgeDraft?>(null) }
                var forgeDraftTick by remember { mutableStateOf(0) }
                // Phoenix tap queue: briefs + nudges generated on-device from
                // the stored snapshot. Refreshed whenever the snapshot lands
                // and whenever Phoenix chat opens.
                var proposals by remember { mutableStateOf<List<PhoenixProposal>>(emptyList()) }
                fun refreshProposals() {
                    lifecycleScope.launch(Dispatchers.IO) {
                        PhoenixBriefEngine.refresh(applicationContext, systemSnapshot)
                        val pending = PhoenixProposalStore.pending(applicationContext)
                        withContext(Dispatchers.Main) { proposals = pending }
                    }
                }
                LaunchedEffect(systemSnapshot) { refreshProposals() }

                // Smart suggestions: ranked package names from on-device
                // launch history, resolved against the installed app list.
                val suggestionPackages by launchHistory.topPackages(8)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val suggestions = remember(apps, suggestionPackages, config.suggestionCount) {
                    val rank = suggestionPackages.withIndex()
                        .associate { it.value to it.index }
                    apps.filter { it.packageName in rank }
                        .sortedBy { rank[it.packageName] }
                        .take(config.suggestionCount.coerceIn(3, 8))
                }

                // Notification badges: empty until the user opts in via
                // Settings -> Notification badges (notification access).
                val badgeCounts by PhoenixNotificationService.badgeCounts
                    .collectAsStateWithLifecycle(
                        initialValue = PhoenixNotificationService.badgeCounts.value
                    )

                // Argus pulse: badge traffic means the Watch god is working.
                var badgesPrimed by remember { mutableStateOf(false) }
                LaunchedEffect(badgeCounts) {
                    if (badgesPrimed) ActivityBus.pulse("argus")
                    else badgesPrimed = true
                }

                // Genesis mesh probes: tailnet reachability for zane-box and
                // penguin. Probed on demand (never faked, never cached as live).
                var nodeReach by remember { mutableStateOf<Map<String, NodeReach>>(emptyMap()) }
                var probingNodes by remember { mutableStateOf(false) }

                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var pickingFor by remember { mutableStateOf<God?>(null) }

                fun probeNodes() {
                    if (probingNodes) return
                    probingNodes = true
                    lifecycleScope.launch(Dispatchers.IO) {
                        val results = mutableMapOf<String, NodeReach>()
                        for (node in com.apexforge.godlauncher.data.GENESIS_NODES) {
                            val host = node.host ?: continue
                            results[node.id] = NodeProbe.probe(host, node.port)
                        }
                        withContext(Dispatchers.Main) {
                            nodeReach = results
                            probingNodes = false
                        }
                    }
                }

                /** Genesis Powers: every row fires something real. */
                fun firePower(id: String) {
                    when (id) {
                        "phoenix-ask", "phoenix-queue" -> screen = Screen.Phoenix
                        "nodes", "system", "powers" -> {
                            screen = Screen.System
                            probeNodes()
                        }
                        "forge" -> screen = Screen.Forge
                        "gods", "drawer" -> screen =
                            if (id == "drawer") Screen.Drawer else Screen.Home
                        "wallpaper" -> applyQuantumWallpaper()
                        "theme", "gestures" -> screen = Screen.ThemeEngine
                        "companion" -> screen = Screen.Settings
                        "bridge" -> Toast.makeText(
                            this@MainActivity,
                            "Agent Bridge listens for " +
                                "com.apexforge.godlauncher.agent.COMMAND broadcasts " +
                                "(see AGENT_BRIDGE.md)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                var sheetGod by remember { mutableStateOf<God?>(null) }
                var focusSearchTick by remember { mutableIntStateOf(0) }
                var suggestionPulseIdx by remember { mutableIntStateOf(0) }

                // v2 ignition: splash shows once per process start (survives
                // rotation via rememberSaveable), phoenix ignites once.
                var splashDone by rememberSaveable { mutableStateOf(false) }
                var ignitePhoenix by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2000)
                    ignitePhoenix = false
                }

                // Phoenix chat state (lives as long as the activity does).
                val phoenixMessages = remember {
                    mutableStateListOf(
                        ChatMessage(
                            "I live in your launcher now. I can open any app by name, " +
                                "set your quantum wallpaper, and think with you about " +
                                "anything - jobs, money, building. What are we working on?",
                            fromPhoenix = true
                        )
                    )
                }
                var phoenixBusy by remember { mutableStateOf(false) }

                fun launchApp(packageName: String) {
                    if (!repo.launch(packageName)) {
                        Toast.makeText(
                            this@MainActivity,
                            "Couldn't launch that app",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    // Feed the on-device suggestion engine (DataStore, IO thread).
                    lifecycleScope.launch(Dispatchers.IO) {
                        launchHistory.recordLaunch(packageName)
                    }
                }

                fun sendToPhoenix(text: String) {
                    phoenixMessages.add(ChatMessage(text, fromPhoenix = false))
                    val local = brain.localIntent(text, apps)
                    if (local != null) {
                        val (action, reply) = local
                        when (action) {
                            is PhoenixAction.LaunchApp -> launchApp(action.packageName)
                            PhoenixAction.SetQuantumWallpaper -> applyQuantumWallpaper()
                            PhoenixAction.OpenDrawer -> screen = Screen.Drawer
                            is PhoenixAction.Navigate -> screen = when (action.route) {
                                "theme" -> Screen.ThemeEngine
                                "forge" -> Screen.Forge
                                "settings" -> Screen.Settings
                                "phoenix" -> Screen.Phoenix
                                "drawer" -> Screen.Drawer
                                // genesis, nodes, powers, system all live in the hub
                                else -> Screen.System
                            }
                            null -> { /* pure reply, no device action */ }
                        }
                        phoenixMessages.add(ChatMessage(reply, fromPhoenix = true))
                        return
                    }
                    if (phoenixApiKey.isBlank()) {
                        phoenixMessages.add(
                            ChatMessage(
                                "I need your Gemini API key before I can think - " +
                                    "paste it on this screen to wake me up.",
                                fromPhoenix = true
                            )
                        )
                        return
                    }
                    phoenixBusy = true
                    lifecycleScope.launch {
                        val history = phoenixMessages
                            .windowed(2, 2, partialWindows = false)
                            .filter { !it[0].fromPhoenix && it[1].fromPhoenix }
                            .map { it[0].text to it[1].text }
                        val reply = try {
                            brain.chat(phoenixApiKey, history, text)
                        } catch (e: Exception) {
                            "Something glitched on my end: ${e.message ?: "network error"}. " +
                                "Check the key and your connection, then try again."
                        }
                        phoenixMessages.add(ChatMessage(reply, fromPhoenix = true))
                        phoenixBusy = false
                    }
                }

                /** Phoenix tap queue: "Copy for Zane" — one tap, paste it in
                 * chat, the machine executes. The phone can't reach Zane's
                 * machine itself, so this is the honest handoff. */
                fun copyProposalForZane(proposal: PhoenixProposal) {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    cm.setPrimaryClip(
                        ClipData.newPlainText(
                            "Phoenix proposal",
                            "${proposal.title}\n\n${proposal.body}"
                        )
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "Copied — paste it to Zane and the machine runs it",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch(Dispatchers.IO) {
                        PhoenixProposalStore.setStatus(
                            applicationContext, proposal.id, "approved"
                        )
                        val pending = PhoenixProposalStore.pending(applicationContext)
                        withContext(Dispatchers.Main) { proposals = pending }
                    }
                }

                fun resolveProposal(proposal: PhoenixProposal) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        PhoenixProposalStore.setStatus(
                            applicationContext, proposal.id, "resolved"
                        )
                        val pending = PhoenixProposalStore.pending(applicationContext)
                        withContext(Dispatchers.Main) { proposals = pending }
                    }
                }

                fun dismissProposal(proposal: PhoenixProposal) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        PhoenixProposalStore.setStatus(
                            applicationContext, proposal.id, "dismissed"
                        )
                        val pending = PhoenixProposalStore.pending(applicationContext)
                        withContext(Dispatchers.Main) { proposals = pending }
                    }
                }

                // Agent Bridge: UI-bound commands from external agents
                // (OpenHands / OpenManus / OpenClaw / ZeroClaw via broadcast).
                // Self-contained commands are answered by the manifest
                // receiver; these need the activity, and the receiver buffers
                // them (extraBufferCapacity 64) until this collector runs.
                LaunchedEffect(Unit) {
                    launch {
                        AgentBridge.commands.collect { cmd ->
                            when (cmd.command) {
                                AgentBridge.CMD_PHOENIX_ASK -> {
                                    val text = cmd.params.optString("text")
                                    if (text.isBlank()) {
                                        AgentBridge.sendResult(
                                            this@MainActivity, cmd, false,
                                            JSONObject().put("error", "text required")
                                        )
                                    } else {
                                        sendToPhoenix(text)
                                        screen = Screen.Phoenix
                                        AgentBridge.sendResult(
                                            this@MainActivity, cmd, true,
                                            JSONObject().put("asked", true)
                                        )
                                    }
                                }
                                AgentBridge.CMD_GOD_LAUNCH -> {
                                    val id = cmd.params.optString("id")
                                    val god = PANTHEON.find { it.id == id }
                                    val pkg = god?.let { godPackages[it.id] }
                                    if (god == null || pkg.isNullOrBlank()) {
                                        AgentBridge.sendResult(
                                            this@MainActivity, cmd, false,
                                            JSONObject().put(
                                                "error",
                                                "unknown god or no app assigned: $id"
                                            )
                                        )
                                    } else {
                                        launchApp(pkg)
                                        AgentBridge.sendResult(
                                            this@MainActivity, cmd, true,
                                            JSONObject().put("launched", god.id)
                                                .put("package", pkg)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    launch {
                        AgentBridge.forgeDrafts.collect { draft ->
                            forgeDraft = draft
                            forgeDraftTick++
                            screen = Screen.Forge
                        }
                    }
                }

                // Service overlay / notification taps route here via
                // ActivityBus (onNewIntent), since screen state lives in
                // the composition.
                LaunchedEffect(Unit) {
                    ActivityBus.nav.collect { route ->
                        if (route == "phoenix") screen = Screen.Phoenix
                    }
                }
                // Entering Settings: re-read the presence switch (the system
                // can kill the service). Entering Phoenix: fresh tap queue.
                LaunchedEffect(screen) {
                    if (screen == Screen.Settings) {
                        presenceOn =
                            PhoenixService.keepAliveEnabled(applicationContext) ||
                                PhoenixService.running
                    } else if (screen == Screen.Phoenix) {
                        refreshProposals()
                    }
                }

                /** Gesture actions from the mod engine. */
                fun performGesture(action: GestureAction) {                    when (action) {
                        GestureAction.OPEN_DRAWER -> screen = Screen.Drawer
                        GestureAction.OPEN_SEARCH -> focusSearchTick++
                        GestureAction.OPEN_PHOENIX -> screen = Screen.Phoenix
                        GestureAction.TOGGLE_SHOWROOM -> lifecycleScope.launch {
                            modStore.setShowroomEnabled(!config.showroomEnabled)
                        }
                        GestureAction.NEXT_THEME -> lifecycleScope.launch {
                            modStore.cyclePreset()
                        }
                        GestureAction.NONE -> {}
                    }
                }

                /** Screen transitions from the mod engine (Motion section). */
                @Composable
                fun appEnter(): EnterTransition = when (config.appTransition) {
                    AppTransition.FADE -> fadeIn(tween(280))
                    AppTransition.SCALE ->
                        scaleIn(tween(280), initialScale = 0.92f) + fadeIn(tween(280))
                    AppTransition.SLIDE ->
                        slideInVertically(tween(280)) { it / 4 } + fadeIn(tween(280))
                    AppTransition.QUANTUM_ZOOM ->
                        scaleIn(tween(320), initialScale = 0.6f) + fadeIn(tween(320))
                }

                @Composable
                fun appExit(): ExitTransition = when (config.appTransition) {
                    AppTransition.FADE -> fadeOut(tween(220))
                    AppTransition.SCALE ->
                        scaleOut(tween(220), targetScale = 0.94f) + fadeOut(tween(220))
                    AppTransition.SLIDE ->
                        slideOutVertically(tween(220)) { it / 4 } + fadeOut(tween(220))
                    AppTransition.QUANTUM_ZOOM ->
                        scaleOut(tween(260), targetScale = 0.7f) + fadeOut(tween(260))
                }

                // Showroom loop: while enabled, rotate presets every N seconds.
                LaunchedEffect(config.showroomEnabled, config.showroomSeconds) {
                    if (!config.showroomEnabled) return@LaunchedEffect
                    while (true) {
                        delay(config.showroomSeconds.coerceIn(3, 60) * 1000L)
                        modStore.cyclePreset()
                    }
                }

                // Theme import: pick a .json file, parse, apply, toast.
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    lifecycleScope.launch(Dispatchers.IO) {
                        val ok = try {
                            val text = contentResolver.openInputStream(uri)
                                ?.bufferedReader()?.readText() ?: ""
                            modStore.importJson(text)
                        } catch (_: Exception) {
                            false
                        }
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                if (ok) "Theme imported" else "Couldn't read that theme file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // One-shot prompt from the home command bar: opened the
                // Phoenix screen (via onPhoenixPrompt) and consumed here by
                // sending immediately through the existing chat path.
                var pendingPhoenixPrompt by remember { mutableStateOf<String?>(null) }
                pendingPhoenixPrompt?.let { prompt ->
                    LaunchedEffect(prompt) {
                        pendingPhoenixPrompt = null
                        sendToPhoenix(prompt)
                    }
                }

                // Mod-engine font scale: scales sp across the whole UI.
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        baseDensity.density,
                        fontScale = baseDensity.fontScale * config.fontScale
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LoopBackground(
                            config = config,
                            animationsEnabled = animationsEnabled,
                            started = splashDone
                        )

                        // Living layer, home screen only. The phoenix burns
                        // behind the home UI (clock stays readable) at the
                        // clock area; the dragon overlays per its configured
                        // corner.
                        if (screen == Screen.Home) {
                            PhoenixFX(
                                animationsEnabled = animationsEnabled,
                                ignite = ignitePhoenix,
                                started = splashDone,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        HomeScreen(
                            apps = apps,
                            godLabels = godLabels,
                            godPackages = godPackages,
                            badgeCounts = badgeCounts,
                            suggestions = suggestions,
                            contactsGranted = contactsGranted,
                            onRequestContactsPermission = {
                                requestContactsPermission.launch(
                                    Manifest.permission.READ_CONTACTS
                                )
                            },
                            onLaunchApp = ::launchApp,
                            onSuggestionLaunch = { pkg ->
                                // Rotating god pulse: suggestions keep the
                                // constellation breathing.
                                ActivityBus.pulse(
                                    PANTHEON[suggestionPulseIdx % PANTHEON.size].id
                                )
                                suggestionPulseIdx++
                                launchApp(pkg)
                            },
                            onGodClick = { god ->
                                ActivityBus.pulse(god.id)
                                val pkg = godPackages[god.id]
                                if (pkg != null) launchApp(pkg) else pickingFor = god
                            },
                            onGodLongPress = { god -> pickingFor = god },
                            onOrbLongPress = { god -> sheetGod = god },
                            onOpenDrawer = { screen = Screen.Drawer },
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenPhoenix = { screen = Screen.Phoenix },
                            onOpenSystem = { screen = Screen.System },
                            onPhoenixPrompt = { text ->
                                ActivityBus.pulse("phoenix")
                                pendingPhoenixPrompt = text
                                screen = Screen.Phoenix
                            },
                            onSetWallpaper = { applyQuantumWallpaper() },
                            animationsEnabled = animationsEnabled,
                            splashDone = splashDone,
                            config = config,
                            focusSearchSignal = focusSearchTick,
                            onSwipeUp = { performGesture(config.gestureSwipeUp) },
                            onSwipeDown = { performGesture(config.gestureSwipeDown) },
                            onDoubleTap = { performGesture(config.gestureDoubleTap) },
                            onPinch = { performGesture(config.gesturePinch) },
                            onTwoFingerTap = { performGesture(config.gestureTwoFingerTap) }
                        )

                        if (screen == Screen.Home &&
                            config.dragonPosition != DragonPosition.HIDDEN
                        ) {
                            val dragonAlign = when (config.dragonPosition) {
                                DragonPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                                DragonPosition.BOTTOM_LEFT -> Alignment.BottomStart
                                DragonPosition.TOP_RIGHT -> Alignment.TopEnd
                                DragonPosition.TOP_LEFT -> Alignment.TopStart
                                DragonPosition.HIDDEN -> Alignment.BottomEnd
                            }
                            DragonMascot(
                                animationsEnabled = animationsEnabled,
                                skin = config.dragonSkin,
                                behavior = config.dragonBehavior,
                                started = splashDone,
                                modifier = Modifier
                                    .align(dragonAlign)
                                    .padding(12.dp)
                                    .size((104f * config.dragonSize).dp)
                            )
                        }

                        // Phoenix — Brandon's on-screen companion. Tap to chat,
                        // long-press for a stored-snapshot status bubble.
                        // Swappable art: res/drawable/phoenix_companion.webp.
                        if (screen == Screen.Home && companionVisible) {
                            PhoenixCompanion(
                                snapshot = systemSnapshot,
                                animationsEnabled = animationsEnabled,
                                onOpenChat = { screen = Screen.Phoenix },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp)
                                    .size(112.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = screen == Screen.Drawer,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            AppDrawer(
                                apps = apps,
                                onLaunch = ::launchApp,
                                onClose = { screen = Screen.Home },
                                badgeCounts = badgeCounts,
                                gridCols = config.gridCols,
                                iconSize = config.iconSize,
                                labelsVisible = config.labelsVisible,
                                labelSize = config.labelSize,
                                onSearch = { ActivityBus.pulse("odysseus") },
                                animationsEnabled = animationsEnabled
                            )
                        }

                        AnimatedVisibility(
                            visible = screen == Screen.Settings,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            SettingsScreen(
                                animationsEnabled = animationsEnabled,
                                onToggleAnimations = { enabled ->
                                    lifecycleScope.launch { store.setAnimationsEnabled(enabled) }
                                },
                companionVisible = companionVisible,
                                onToggleCompanion = { visible ->
                                    lifecycleScope.launch { store.setCompanionVisible(visible) }
                                },
                                presenceOn = presenceOn,
                                onTogglePresence = { on ->
                                    if (on) {
                                        try {
                                            PhoenixService.start(this@MainActivity)
                                            presenceOn = true
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Couldn't start Phoenix: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        PhoenixService.stop(this@MainActivity)
                                        presenceOn = false
                                    }
                                },
                                godLabels = godLabels,
                                onPickGodApp = { god -> pickingFor = god },
                                onClearGodApp = { god ->
                                    lifecycleScope.launch { store.clearGodApp(god) }
                                },
                                onOpenThemeEngine = { screen = Screen.ThemeEngine },
                                onOpenForge = { screen = Screen.Forge },
                                forgeGatewayUrl = forgeGatewayUrl,
                                forgeApiKey = forgeApiKey,
                                onSaveGateway = { url, key ->
                                    lifecycleScope.launch {
                                        store.setForgeGatewayUrl(url)
                                        store.setForgeApiKey(key)
                                    }
                                },
                                onBack = { screen = Screen.Home }
                            )
                        }

                        AnimatedVisibility(
                            visible = screen == Screen.ThemeEngine,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            ThemeEngineScreen(
                                config = config,
                                onConfigChange = { new ->
                                    lifecycleScope.launch { modStore.update { _ -> new } }
                                },
                                onProfileSelect = { profile ->
                                    lifecycleScope.launch { modStore.setActiveProfile(profile) }
                                },
                                onPresetApply = { preset ->
                                    lifecycleScope.launch { modStore.applyPreset(preset) }
                                },
                                onExport = { exportTheme(config) },
                                onImport = { importLauncher.launch("application/json") },
                                onBack = { screen = Screen.Home }
                            )
                        }

                        AnimatedVisibility(
                            visible = screen == Screen.Phoenix,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            PhoenixScreen(
                                apiKey = phoenixApiKey,
                                messages = phoenixMessages,
                                busy = phoenixBusy,
                                onSaveApiKey = { key ->
                                    lifecycleScope.launch { store.setPhoenixApiKey(key) }
                                },
                                onSend = ::sendToPhoenix,
                                onBack = { screen = Screen.Home },
                                proposals = proposals,
                                onProposalPrimary = { proposal ->
                                    // "tap" and "genesis" kinds need the machine:
                                    // copy for Zane. Everything else: mark done.
                                    if (proposal.kind == "tap" || proposal.kind == "genesis") {
                                        copyProposalForZane(proposal)
                                    } else {
                                        resolveProposal(proposal)
                                    }
                                },
                                onProposalDismiss = ::dismissProposal
                            )
                        }

                        AnimatedVisibility(
                            visible = screen == Screen.Forge,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            ForgeScreen(
                                gatewayUrl = forgeGatewayUrl,
                                apiKey = forgeApiKey,
                                animationsEnabled = animationsEnabled,
                                onSaveGateway = { url, key ->
                                    lifecycleScope.launch {
                                        store.setForgeGatewayUrl(url)
                                        store.setForgeApiKey(key)
                                    }
                                },
                                onBack = { screen = Screen.Home },
                                prefill = forgeDraft,
                                prefillTick = forgeDraftTick
                            )
                        }

                        // GENESIS hub: nodes (live probes) + powers (everything it
                        // can do / change) + system (stored snapshot, labeled).
                        AnimatedVisibility(
                            visible = screen == Screen.System,
                            enter = appEnter(),
                            exit = appExit()
                        ) {
                            GenesisScreen(
                                snapshot = systemSnapshot,
                                reach = nodeReach,
                                probing = probingNodes,
                                onProbeNodes = { probeNodes() },
                                onPower = ::firePower,
                                onBack = { screen = Screen.Home },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        pickingFor?.let { god ->
                            AppPickerDialog(
                                title = "Assign app to ${god.name}",
                                apps = apps,
                                onPick = { app ->
                                    lifecycleScope.launch {
                                        store.setGodApp(god, app.packageName, app.label)
                                    }
                                    pickingFor = null
                                    Toast.makeText(
                                        this@MainActivity,
                                        "${god.name} → ${app.label}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDismiss = { pickingFor = null }
                            )
                        }

                        // God quick sheet: long-press a quantum orb.
                        sheetGod?.let { god ->
                            GodQuickSheet(
                                god = god,
                                assignedLabel = godLabels[god.id],
                                onLaunch = {
                                    sheetGod = null
                                    godPackages[god.id]?.let { launchApp(it) }
                                },
                                onAssign = {
                                    sheetGod = null
                                    pickingFor = god
                                },
                                onAskPhoenix = {
                                    sheetGod = null
                                    ActivityBus.pulse("phoenix")
                                    pendingPhoenixPrompt =
                                        "Tell me about ${god.name}, god of ${god.domain}, " +
                                            "and how I should use them in my launcher."
                                    screen = Screen.Phoenix
                                },
                                onDismiss = { sheetGod = null }
                            )
                        }

                        // Showroom pill: floating stop control while the
                        // showroom loop cycles presets.
                        if (config.showroomEnabled) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Button(
                                    onClick = {
                                        lifecycleScope.launch {
                                            modStore.setShowroomEnabled(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE25822)
                                    ),
                                    modifier = Modifier.padding(top = 52.dp)
                                ) {
                                    Text(
                                        "SHOWROOM — tap to stop",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Ignition splash: full-screen overlay on cold start.
                        if (!splashDone) {
                            SplashIgnition(onFinished = { splashDone = true })
                        }
                    }
                }
            }
        }
    }

    /** Applies the bundled quantum mandala as the system wallpaper. */
    private fun applyQuantumWallpaper() {
        lifecycleScope.launch(Dispatchers.IO) {
            val ok = try {
                WallpaperManager.getInstance(this@MainActivity)
                    .setResource(R.drawable.quantum_bg)
                true
            } catch (_: Exception) {
                false
            }
            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (ok) "Quantum wallpaper applied" else "Couldn't set wallpaper",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** Theme export: JSON to the app cache dir, shared via FileProvider. */
    private fun exportTheme(config: PantheonConfig) {
        lifecycleScope.launch(Dispatchers.IO) {
            val ok = try {
                val json = modStore.exportJson(config)
                val dir = File(cacheDir, "themes")
                dir.mkdirs()
                val file = File(dir, "pantheon-theme.json")
                file.writeText(json)
                val uri: Uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Pantheon theme"))
                true
            } catch (_: Exception) {
                false
            }
            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (ok) "Theme exported — pick where to share it"
                    else "Theme export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
