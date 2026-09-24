package com.apexforge.godlauncher.ui.forge

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.data.FORGE_REQUEST_TYPES
import com.apexforge.godlauncher.data.AgentBridge
import com.apexforge.godlauncher.data.ForgeApi
import com.apexforge.godlauncher.data.ForgeResult
import com.apexforge.godlauncher.data.ForgeWorkOrder
import com.apexforge.godlauncher.data.forgeTypeLabel
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import com.apexforge.godlauncher.ui.theme.VoidBlack
import com.apexforge.godlauncher.ui.theme.VoidPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val ForgeAmber = Color(0xFFF5B942)
private val ForgeCyan = Color(0xFF22D3EE)
private val ForgeGreen = Color(0xFF4ADE80)
private val ForgeRed = Color(0xFFF87171)

private fun statusColor(status: String): Color = when (status) {
    "queued" -> ForgeAmber
    "building" -> ForgeCyan
    "ready" -> ForgeGreen
    "failed" -> ForgeRed
    else -> Color.Gray
}

/**
 * The Forge: Brandon's build-request interface to Zane's machine.
 * He describes the app/SDK/feature change he wants; the crew builds it;
 * a finished APK comes back as a download-and-install button.
 *
 * Separate from the on-device theme mod engine — the Forge is for
 * changes that need a rebuild.
 */
@Composable
fun ForgeScreen(
    gatewayUrl: String,
    apiKey: String,
    animationsEnabled: Boolean,
    onSaveGateway: (url: String, key: String) -> Unit,
    onBack: () -> Unit,
    prefill: AgentBridge.ForgeDraft? = null,
    prefillTick: Int = 0,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            visible = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configured = gatewayUrl.isNotBlank() && apiKey.isNotBlank()
    var orders by remember { mutableStateOf<List<ForgeWorkOrder>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var unreachable by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var showGatewayDialog by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun refreshOrders() {
        if (!configured || isRefreshing) return
        scope.launch {
            isRefreshing = true
            when (val r = ForgeApi.fetchRequests(gatewayUrl, apiKey)) {
                is ForgeResult.Ok -> {
                    orders = r.value
                    unreachable = false
                    lastError = null
                }
                is ForgeResult.HttpError -> {
                    lastError = if (r.code == 401) "Invalid or revoked key"
                    else "Error ${r.code}: ${r.message}"
                    unreachable = false
                }
                is ForgeResult.NetworkError -> {
                    unreachable = true
                }
            }
            isRefreshing = false
        }
    }

    // Auto-refresh every 15s while the screen is visible and configured.
    LaunchedEffect(configured, visible) {
        if (!configured || !visible) return@LaunchedEffect
        refreshOrders()
        while (true) {
            delay(15_000)
            refreshOrders()
        }
    }

    // Download-complete receiver: fire the install intent when a Forge
    // build finishes downloading.
    val activeDownloads = remember { mutableStateMapOf<Long, String>() }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                val orderId = activeDownloads.remove(id) ?: return
                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val ok = dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                    c.moveToFirst() &&
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                        DownloadManager.STATUS_SUCCESSFUL
                } && ForgeDownload.apkFile(ctx, orderId).exists()
                if (ok) {
                    ForgeDownload.fireInstall(ctx, orderId)
                    toast("Build downloaded — tap Install to update")
                } else {
                    toast("Download failed — try again")
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    // --- custom pull-to-refresh (no material M2 dep): overscroll at the
    // top of the list stretches, release past the trigger refreshes. ---
    var pullPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val triggerPx = remember(density) { with(density) { 96.dp.toPx() } }
    val pullConnection = remember(configured) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // UserInput source check removed: this Compose version no
                // longer exposes NestedScrollSource.UserInput. Direct drags
                // are the only thing that produce positive available.y here
                // in practice; flings settle through the same path harmlessly.
                if (available.y > 0f && configured && !isRefreshing) {
                    pullPx = (pullPx + available.y * 0.45f)
                        .coerceIn(0f, triggerPx * 2.4f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }
    // Release detection: when the pull settles (no new drag events for
    // ~160ms), snap back and refresh if past the trigger.
    LaunchedEffect(pullPx) {
        if (pullPx <= 0f) return@LaunchedEffect
        delay(160)
        val settled = pullPx
        pullPx = 0f
        if (settled >= triggerPx) refreshOrders()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "The Forge", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Request rebuilds from Zane's crew",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
                Text(
                    text = "The gateway runs on Zane's machine — your phone reaches it " +
                        "only through the URL you configure here. " +
                        "Work-order history below is live from that gateway; " +
                        "snapshot numbers elsewhere in the launcher are not.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedStar.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (configured) {
                IconButton(onClick = { refreshOrders() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh work orders")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullConnection)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ConnectionCard(
                    gatewayUrl = gatewayUrl,
                    apiKeySet = apiKey.isNotBlank(),
                    onEdit = { showGatewayDialog = true }
                )
            }

            item {
                if (!configured) {
                    EmptyStateCard(
                        title = "Forge not connected",
                        body = "Add your Pantheon gateway URL + px- key to connect " +
                            "to the Forge and request builds."
                    ) { showGatewayDialog = true }
                } else {
                    // An agent draft arriving via the Agent Bridge pre-fills the
                    // composer; prefillTick bumps so a new draft resets the fields.
                    val draftTick = remember(prefillTick) { prefillTick }
                    ComposerCard(
                        initialType = prefill?.type?.ifBlank { null } ?: "launcher-mod",
                        initialTitle = prefill?.title ?: "",
                        initialDetail = prefill?.detail ?: "",
                        keyTick = draftTick,
                        onSubmit = { type, title, detail ->
                            if (title.isBlank()) {
                                toast("Title is required")
                            } else {
                                scope.launch {
                                    when (val r = ForgeApi.submitRequest(
                                        gatewayUrl, apiKey, type,
                                        title.trim(), detail.trim()
                                    )) {
                                        is ForgeResult.Ok -> {
                                            toast("Queued: ${r.value.id}")
                                            refreshOrders()
                                        }
                                        is ForgeResult.HttpError -> toast(
                                            if (r.code == 401) "Invalid or revoked key"
                                            else "Error ${r.code}: ${r.message}"
                                        )
                                        is ForgeResult.NetworkError ->
                                            toast("Gateway unreachable")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                if (pullPx > 2f || isRefreshing) {
                    PullIndicator(pullPx = pullPx, triggerPx = triggerPx, refreshing = isRefreshing)
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "My work orders",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = EmberOrange
                        )
                    }
                }
            }

            when {
                !configured -> {
                    // Empty state already shown above the composer slot.
                }
                unreachable -> {
                    item {
                        EmptyStateCard(
                            title = "Gateway unreachable",
                            body = "Couldn't reach the Pantheon gateway. Check the URL " +
                                "and that Zane's machine is online.",
                            actionLabel = "Retry"
                        ) { refreshOrders() }
                    }
                }
                orders.isEmpty() && !isRefreshing -> {
                    item {
                        EmptyStateCard(
                            title = "No work orders yet",
                            body = "Describe the build you want above — the crew " +
                                "picks it up and it lands here.",
                            actionLabel = "Refresh"
                        ) { refreshOrders() }
                    }
                }
                else -> {
                    if (lastError != null) {
                        item {
                            Text(
                                text = lastError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ForgeRed,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    items(orders, key = { it.id }) { order ->
                        WorkOrderCard(
                            order = order,
                            animationsEnabled = animationsEnabled,
                            visible = visible,
                            onDownloadInstall = { wo ->
                                val apkUrl = ForgeApi.extractApkUrl(wo.result)
                                if (apkUrl == null) {
                                    toast("No download attached to this build")
                                } else {
                                    try {
                                        val id = ForgeDownload.enqueue(context, wo.id, apkUrl)
                                        activeDownloads[id] = wo.id
                                        toast("Downloading build…")
                                    } catch (_: Exception) {
                                        toast("Download failed to start")
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showGatewayDialog) {
        GatewayDialog(
            initialUrl = gatewayUrl,
            initialKey = apiKey,
            onDismiss = { showGatewayDialog = false },
            onSave = { url, key ->
                onSaveGateway(url.trim().trimEnd('/'), key.trim())
                showGatewayDialog = false
                toast("Gateway saved")
            }
        )
    }
}

/** Connection status card: URL + key state, tap to edit. */
@Composable
private fun ConnectionCard(
    gatewayUrl: String,
    apiKeySet: Boolean,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (gatewayUrl.isNotBlank() && apiKeySet) ForgeGreen else MutedStar)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (gatewayUrl.isBlank()) "Gateway: not set"
                    else "Gateway: $gatewayUrl",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (apiKeySet) "API key: set" else "API key: not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
            }
            Text(
                text = "EDIT",
                style = MaterialTheme.typography.labelLarge,
                color = EmberOrange
            )
        }
    }
}

/** Dialog for editing the gateway URL + API key (stored in DataStore). */
@Composable
private fun GatewayDialog(
    initialUrl: String,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (url: String, key: String) -> Unit
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var key by remember(initialKey) { mutableStateOf(initialKey) }
    var keyVisible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pantheon gateway") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Gateway URL") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    colors = forgeFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API key (px-…)") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                if (keyVisible) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = if (keyVisible) "Hide key" else "Show key"
                            )
                        }
                    },
                    colors = forgeFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Stored only on this phone — never in the app package.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url, key) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun forgeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmberOrange,
    focusedLabelColor = EmberOrange,
    cursorColor = EmberOrange
)

/** The "new request" composer: type chips + title + detail + submit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComposerCard(
    onSubmit: (type: String, title: String, detail: String) -> Unit,
    initialType: String = "launcher-mod",
    initialTitle: String = "",
    initialDetail: String = "",
    keyTick: Int = 0
) {
    var type by remember(keyTick) { mutableStateOf(initialType) }
    var title by remember(keyTick) { mutableStateOf(initialTitle) }
    var detail by remember(keyTick) { mutableStateOf(initialDetail) }

    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "New request",
                style = MaterialTheme.typography.titleMedium,
                color = PhoenixGold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FORGE_REQUEST_TYPES.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(forgeTypeLabel(t)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmberOrange.copy(alpha = 0.25f),
                            selectedLabelColor = EmberOrange
                        )
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 200) title = it },
                label = { Text("Title (required)") },
                singleLine = true,
                colors = forgeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { if (it.length <= 4000) detail = it },
                label = { Text("Details — what should the crew build?") },
                minLines = 3,
                maxLines = 6,
                supportingText = {
                    Text("${detail.length}/4000", style = MaterialTheme.typography.labelSmall)
                },
                colors = forgeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onSubmit(type, title, detail)
                    title = ""
                    detail = ""
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send to the Forge")
            }
            Text(
                text = "The crew builds it and the finished APK lands in your work orders below.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedStar
            )
        }
    }
}

/** One work order card: type chip, status pill, title, expandable detail. */
@Composable
private fun WorkOrderCard(
    order: ForgeWorkOrder,
    animationsEnabled: Boolean,
    visible: Boolean,
    onDownloadInstall: (ForgeWorkOrder) -> Unit
) {
    var expanded by remember(order.id) { mutableStateOf(false) }
    val apkUrl = remember(order.result) { ForgeApi.extractApkUrl(order.result) }
    val resultText = remember(order.result) { ForgeApi.resultText(order.result) }

    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = forgeTypeLabel(order.type).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmberOrange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmberOrange.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    status = order.status,
                    animationsEnabled = animationsEnabled,
                    visible = visible
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.ts.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedStar
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = order.title, style = MaterialTheme.typography.titleSmall)
            if (order.detail.isNotBlank()) {
                Text(
                    text = order.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                    Text(if (expanded) "Less" else "More")
                }
            }
            if (order.status == "ready") {
                Spacer(modifier = Modifier.height(4.dp))
                if (apkUrl != null) {
                    Button(
                        onClick = { onDownloadInstall(order) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & install", color = Color.Black)
                    }
                    Text(
                        text = "If install is blocked, allow \u201cInstall unknown apps\u201d " +
                            "for Pantheon Launcher in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedStar,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (resultText != null) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Build finished — no download attached.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedStar,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (order.status == "failed" && resultText != null) {
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgeRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Status pill: queued=amber, building=cyan pulse, ready=green, failed=red.
 * The pulse runs only while animations are enabled AND the screen is
 * resumed; otherwise a static frame.
 */
@Composable
private fun StatusPill(status: String, animationsEnabled: Boolean, visible: Boolean) {
    val color = statusColor(status)
    val pulse = animationsEnabled && visible && status == "building"
    val transition = rememberInfiniteTransition(label = "forge-status")
    val alpha: Float by if (pulse) {
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "building-pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    Text(
        text = status.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color.copy(alpha = alpha),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f * alpha))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PullIndicator(pullPx: Float, triggerPx: Float, refreshing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = EmberOrange
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = EmberOrange.copy(alpha = (pullPx / triggerPx).coerceIn(0.25f, 1f)),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pullPx >= triggerPx) "Release to refresh" else "Pull to refresh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String, actionLabel: String = "Configure", onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PhoenixGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MutedStar,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * App-private APK download + install. DownloadManager writing into the
 * app cache dir needs no new permission; the install intent goes through
 * the existing FileProvider with a read grant.
 */
private object ForgeDownload {
    fun apkFile(context: Context, orderId: String): File =
        File(File(context.cacheDir, "forge").apply { mkdirs() }, "$orderId.apk")

    fun enqueue(context: Context, orderId: String, url: String): Long {
        val dest = apkFile(context, orderId)
        if (dest.exists()) dest.delete()
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Pantheon Forge build")
            setDescription(orderId)
            setDestinationUri(Uri.fromFile(dest))
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setMimeType("application/vnd.android.package-archive")
        }
        return (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
            .enqueue(req)
    }

    fun fireInstall(context: Context, orderId: String) {
        val apk = apkFile(context, orderId)
        if (!apk.exists()) return
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
