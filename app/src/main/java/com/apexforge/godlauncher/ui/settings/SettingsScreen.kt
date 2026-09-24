package com.apexforge.godlauncher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexforge.godlauncher.R
import com.apexforge.godlauncher.data.PhoenixNotificationService
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.PANTHEON
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.VoidBlack
import com.apexforge.godlauncher.ui.theme.VoidPanel

/**
 * Settings: animation toggle, per-god app assignment, About.
 * God rows open the same app picker used by long-press on the dock.
 */
@Composable
fun SettingsScreen(
    animationsEnabled: Boolean,
    onToggleAnimations: (Boolean) -> Unit,
    companionVisible: Boolean = true,
    onToggleCompanion: (Boolean) -> Unit = {},
    presenceOn: Boolean = false,
    onTogglePresence: (Boolean) -> Unit = {},
    godLabels: Map<String, String?>,
    onPickGodApp: (God) -> Unit,
    onClearGodApp: (God) -> Unit,
    onOpenThemeEngine: () -> Unit = {},
    onOpenForge: () -> Unit = {},
    forgeGatewayUrl: String = "",
    forgeApiKey: String = "",
    onSaveGateway: (url: String, key: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    // Notification-access grant state, refreshed every time we return to
    // the foreground (e.g. coming back from the system settings screen).
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var badgesGranted by remember {
        mutableStateOf(PhoenixNotificationService.isAccessGranted(context))
    }
    // "Float over other apps" grant, refreshed on return (Brandon grants
    // it in the system screen this button opens).
    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                badgesGranted = PhoenixNotificationService.isAccessGranted(context)
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.94f))
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
            Text(text = "Pantheon Settings", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1410).copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenThemeEngine)
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theme Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmberOrange
                            )
                            Text(
                                text = "Backgrounds, layout, icons, motion, gestures, dragon, profiles — the total mod engine",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmberOrange
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1410).copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenForge)
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "The Forge",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmberOrange
                            )
                            Text(
                                text = "Request rebuilds from Zane's crew — app mods, SDK, features, themes",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmberOrange
                        )
                    }
                }
                GatewaySettingsCard(
                    gatewayUrl = forgeGatewayUrl,
                    apiKey = forgeApiKey,
                    onSave = onSaveGateway
                )
                // Phoenix presence: the always-on companion. Both taps are
                // Brandon's; the costs are stated before he taps.
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A1608).copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Phoenix presence — always on",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmberOrange
                        )
                        Text(
                            text = "Two optional taps keep Phoenix with you everywhere, " +
                                "not just on the home screen. Phoenix never moves money " +
                                "on its own — every money move is your tap.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingRow(
                            title = "Keep Phoenix alive",
                            subtitle = "Foreground service: Phoenix stays one tap away " +
                                "with a persistent notification, and refreshes your " +
                                "brief every 30 minutes."
                        ) {
                            Switch(
                                checked = presenceOn,
                                onCheckedChange = onTogglePresence
                            )
                        }
                        Text(
                            text = "Battery, honestly: a foreground service keeps the " +
                                "launcher process alive, which uses real battery. It " +
                                "idles between 30-minute snapshot checks — Android shows " +
                                "the exact draw under Settings → Battery → Pantheon " +
                                "Launcher. Android can still kill Phoenix under memory " +
                                "pressure; it restarts the next time you open the " +
                                "launcher (Android 12+ won't let it restart silently " +
                                "at boot).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        SettingRow(
                            title = "Float over other apps",
                            subtitle = if (overlayGranted)
                                "Phoenix hovers over other apps — tap to chat, drag to move."
                            else
                                "Needs the \"display over other apps\" permission. " +
                                    "Phoenix draws only its own image — no screen reading."
                        ) {
                            if (overlayGranted) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Overlay permission granted",
                                    tint = EmberOrange
                                )
                            } else {
                                TextButton(onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse(
                                                "package:" + context.packageName
                                            )
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }) { Text("Grant") }
                            }
                        }
                    }
                }
                SettingRow(
                    title = "Animated background",
                    subtitle = "Nebula drift, starfield twinkle, zodiac ring"
                ) {
                    Switch(
                        checked = animationsEnabled,
                        onCheckedChange = onToggleAnimations
                    )
                }
                SettingRow(
                    title = "Phoenix companion",
                    subtitle = "Phoenix floats on the home screen — tap to chat, " +
                        "long-press for a stored-snapshot status bubble"
                ) {
                    Switch(
                        checked = companionVisible,
                        onCheckedChange = onToggleCompanion
                    )
                }
                SettingRow(
                    title = "Notification badges",
                    subtitle = if (badgesGranted)
                        "Showing unread counts on the god dock and drawer"
                    else
                        "Show unread counts on icons — needs notification access"
                ) {
                    if (badgesGranted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Notification badges enabled",
                            tint = EmberOrange
                        )
                    } else {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) { Text("Enable") }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "God dock assignments",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(PANTHEON, key = { it.id }) { god ->
                val label = godLabels[god.id]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickGodApp(god) }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = god.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = label ?: "Not assigned — tap to choose",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (label != null) {
                        IconButton(onClick = { onClearGodApp(god) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Clear ${god.name} assignment",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VoidPanel),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "About Pantheon", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.about_text),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}

/**
 * Secure settings area for the Forge gateway. URL + px- key are stored
 * in DataStore on this phone only — never hardcoded, never in the APK.
 */
@Composable
private fun GatewaySettingsCard(
    gatewayUrl: String,
    apiKey: String,
    onSave: (url: String, key: String) -> Unit
) {
    var url by remember(gatewayUrl) { mutableStateOf(gatewayUrl) }
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    val dirty = url.trim() != gatewayUrl.trim() || key.trim() != apiKey.trim()

    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pantheon gateway",
                style = MaterialTheme.typography.titleMedium,
                color = EmberOrange
            )
            Text(
                text = "Connects the Forge to Zane's machine. Stored only on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Pantheon gateway URL") },
                placeholder = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmberOrange,
                    focusedLabelColor = EmberOrange,
                    cursorColor = EmberOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Pantheon API key (px-…)") },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmberOrange,
                    focusedLabelColor = EmberOrange,
                    cursorColor = EmberOrange
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSave(url.trim().trimEnd('/'), key.trim()) },
                enabled = dirty,
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save gateway")
            }
        }
    }
}
