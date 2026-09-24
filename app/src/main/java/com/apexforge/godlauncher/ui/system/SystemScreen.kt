package com.apexforge.godlauncher.ui.system

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.FabricJob
import com.apexforge.godlauncher.data.GoalStatus
import com.apexforge.godlauncher.data.GodBrainStatus
import com.apexforge.godlauncher.data.SystemSnapshot
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import com.apexforge.godlauncher.ui.theme.VoidPanel

/**
 * System screen — the whole Pantheon state at a glance, rendered from the
 * bundled snapshot (assets/data-snapshot.json, probed on Zane's machine).
 *
 * Nothing here is live: the phone cannot reach the VM's localhost
 * services, so every number is labeled with the probe timestamp. Live
 * surfaces remain Phoenix chat (his Gemini key) and the Forge gateway
 * (his configured URL).
 */
@Composable
fun SystemScreen(
    snapshot: SystemSnapshot?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060814).copy(alpha = 0.92f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "PANTHEON SYSTEM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
        if (snapshot == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading snapshot…", color = MutedStar)
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SnapshotBanner(snapshot.snapshotAt)
            }
            item {
                GpuCard(snapshot)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "JOBS",
                        value = "${snapshot.jobsApplied}/${snapshot.jobsGoal}",
                        sub = "applications",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "MONEY",
                        value = snapshot.moneyNet.substringBefore(" (").ifBlank { "$0.00" },
                        sub = "${snapshot.moneyEntries} ledger entries",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                GodsCard(snapshot.gods)
            }
            item {
                FabricCard(snapshot.fabricQueue)
            }
            item {
                InfraCard(snapshot)
            }
            item {
                GoalsCard(snapshot.goals)
            }
        }
    }
}

@Composable
private fun SnapshotBanner(snapshotAt: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A08)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "STORED SNAPSHOT — NOT LIVE",
                color = PhoenixGold,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Probed ${snapshotAt.replace("T", " ").removeSuffix("Z")} UTC on Zane's machine. " +
                    "This phone can't reach those services, so these numbers are a reading, not a feed.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun GpuCard(s: SystemSnapshot) {
    val frac = if (s.gpuAllowedH > 0) {
        (s.gpuRemainingH / s.gpuAllowedH).toFloat().coerceIn(0f, 1f)
    } else 0f
    Panel(title = "GPU QUOTA — KAGGLE") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = trim2(s.gpuRemainingH),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp
            )
            Text(
                text = " / ${trim2(s.gpuAllowedH)} hrs left",
                color = MutedStar,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = EmberOrange,
            trackColor = Color.White.copy(alpha = 0.12f)
        )
        Text(
            text = "Refreshes ${s.gpuRefreshAt.replace("T", " ").removeSuffix("Z")} UTC",
            color = MutedStar, fontSize = 12.sp
        )
        if (s.gpuNote.isNotBlank()) {
            Text(
                text = s.gpuNote,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun GodsCard(gods: List<GodBrainStatus>) {
    Panel(title = "GOD BRAINS — ${gods.size}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (g in gods) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (g.brain.contains("harvested", ignoreCase = true) ||
                                        g.brain.contains("COMPLETE", ignoreCase = true)
                                    ) EmberOrange else MutedStar
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = g.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = " · ${g.role}",
                            color = MutedStar,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = g.brain,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FabricCard(queue: List<FabricJob>) {
    Panel(title = "COMPUTE FABRIC QUEUE") {
        if (queue.isEmpty()) {
            Text("Queue empty in snapshot.", color = MutedStar, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (j in queue) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusDot(j.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = j.title.ifBlank { j.id },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = j.id, color = MutedStar, fontSize = 11.sp)
                        }
                        Text(
                            text = j.status,
                            color = statusColor(j.status),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfraCard(s: SystemSnapshot) {
    Panel(title = "MACHINE & HANDS") {
        Text(
            text = "Ports up: ${s.portsListening}/${s.portsTotal} (at probe time, on Zane's machine)",
            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "MCP hands (${s.mcpServers.size})", color = MutedStar, fontSize = 12.sp)
        Text(
            text = s.mcpServers.joinToString(" · ").ifBlank { "none in snapshot" },
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun GoalsCard(goals: List<GoalStatus>) {
    Panel(title = "GOALS — ${goals.size}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (g in goals) {
                Text(
                    text = g.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = g.status,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = PhoenixGold,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title, color = PhoenixGold, fontWeight = FontWeight.Black,
                fontSize = 11.sp, letterSpacing = 1.5.sp
            )
            Text(
                text = value, color = Color.White, fontWeight = FontWeight.Black,
                fontSize = 24.sp, modifier = Modifier.padding(top = 4.dp)
            )
            Text(text = sub, color = MutedStar, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(statusColor(status))
    )
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "COMPLETE" -> Color(0xFF4ADE80)
    "RUNNING", "BUILDING" -> Color(0xFF7DF9FF)
    "ERROR", "BLOCKED", "FAILED" -> Color(0xFFF87171)
    else -> MutedStar
}

private fun trim2(h: Double): String =
    "%.2f".format(h).trimEnd('0').trimEnd('.').ifEmpty { "0" }
