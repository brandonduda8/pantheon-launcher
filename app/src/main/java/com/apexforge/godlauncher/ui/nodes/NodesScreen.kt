package com.apexforge.godlauncher.ui.nodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.GENESIS_NODES
import com.apexforge.godlauncher.data.GenesisNode
import com.apexforge.godlauncher.data.NodeProbe
import com.apexforge.godlauncher.data.NodeReach
import com.apexforge.godlauncher.data.PhoneVitals
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import com.apexforge.godlauncher.ui.theme.VoidPanel

/**
 * Genesis Nodes: every machine in Brandon's mesh with honest status.
 *
 * - THIS PHONE: live vitals read on-device right now (LIVE badge).
 * - ZANE-BOX / PENGUIN: live tailnet TCP probes. ONLINE means the phone
 *   actually reached the node through Tailscale; UNREACHABLE means it
 *   didn't — most likely the Tailscale app isn't connected on this phone.
 *   Nothing here is ever faked or cached as live.
 */
@Composable
fun NodesScreen(
    reach: Map<String, NodeReach>,
    probing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    // Phone vitals are read live on every composition — cheap syscalls.
    val vitals = try {
        NodeProbe.phoneVitals(context)
    } catch (_: Exception) {
        null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060814).copy(alpha = 0.92f))
    ) {
        if (showHeader) {
            NodesHeader(probing = probing, onRefresh = onRefresh, onBack = onBack)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showHeader) {
                item(key = "probe-row") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "live tailnet probes",
                            color = MutedStar,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(
                            onClick = onRefresh,
                            enabled = !probing
                        ) {
                            if (probing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PhoenixGold
                                )
                            } else {
                                Text(
                                    "PROBE NOW",
                                    color = PhoenixGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }
            }
            for (node in GENESIS_NODES) {
                item(key = node.id) {
                    if (node.id == "phone") {
                        PhoneNodeCard(vitals = vitals)
                    } else {
                        MeshNodeCard(
                            node = node,
                            reach = reach[node.id] ?: NodeReach.NotProbed
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Tailnet probes run from this phone through the Tailscale app. " +
                        "If a node shows UNREACHABLE, check that Tailscale is connected.",
                    color = MutedStar,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NodesHeader(
    probing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = "GENESIS NODES",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRefresh, enabled = !probing) {
            if (probing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = PhoenixGold
                )
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = "Probe nodes", tint = PhoenixGold)
            }
        }
    }
}

@Composable
private fun PhoneNodeCard(vitals: PhoneVitals?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(Color(0xFF4ADE80))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "THIS PHONE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                LiveBadge()
            }
            Text(
                text = "Termux · on-device Genesis",
                color = MutedStar,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (vitals == null) {
                Text(
                    text = "Couldn't read vitals.",
                    color = MutedStar,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                VitalRow(
                    label = "BATTERY",
                    value = "${vitals.batteryPct}%${if (vitals.charging) " · charging" else ""}",
                    frac = vitals.batteryPct / 100f
                )
                Spacer(modifier = Modifier.height(8.dp))
                VitalRow(
                    label = "RAM",
                    value = "${vitals.ramFreeMb} / ${vitals.ramTotalMb} MB free",
                    frac = if (vitals.ramTotalMb > 0)
                        vitals.ramFreeMb.toFloat() / vitals.ramTotalMb else 0f
                )
                Spacer(modifier = Modifier.height(8.dp))
                VitalRow(
                    label = "STORAGE",
                    value = "%.1f / %.1f GB free".format(vitals.storageFreeGb, vitals.storageTotalGb),
                    frac = if (vitals.storageTotalGb > 0)
                        (vitals.storageFreeGb / vitals.storageTotalGb).toFloat() else 0f
                )
                Text(
                    text = "uptime ${"%.1f".format(vitals.uptimeHrs)}h · read on-device just now",
                    color = MutedStar,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MeshNodeCard(node: GenesisNode, reach: NodeReach) {
    val (dot, statusText) = when (reach) {
        is NodeReach.Online -> Color(0xFF4ADE80) to "ONLINE · ${reach.latencyMs}ms"
        NodeReach.Unreachable -> Color(0xFFF87171) to "UNREACHABLE"
        NodeReach.NotProbed -> MutedStar to "NOT PROBED YET"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(dot)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = node.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusText,
                    color = dot,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Text(
                text = node.role,
                color = MutedStar,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "tailnet ${node.host}:${node.port} · live probe",
                color = MutedStar,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (node.parkedNote != null) {
                Text(
                    text = node.parkedNote,
                    color = PhoenixGold.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VitalRow(label: String, value: String, frac: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = PhoenixGold,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier.width(72.dp)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { frac.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = EmberOrange,
            trackColor = Color.White.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(EmberOrange.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "LIVE",
            color = EmberOrange,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )
    }
}
