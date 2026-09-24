package com.apexforge.godlauncher.ui.nodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.ui.theme.MutedStar
import com.apexforge.godlauncher.ui.theme.PhoenixGold
import com.apexforge.godlauncher.ui.theme.VoidPanel

/**
 * A thing Genesis can do or Brandon can change — every row does something
 * real when tapped. Nothing decorative, no dead ends.
 */
data class GenesisPower(
    val id: String,
    val group: String,
    val title: String,
    val blurb: String
)

/** The full capability surface, in Brandon's words: everything we can do. */
val GENESIS_POWERS = listOf(
    GenesisPower(
        "phoenix-ask", "COMMAND",
        "Ask Phoenix",
        "The animated mind in your launcher. Type anything — jobs, money, building. " +
            "Say \"open\" + an app name and it opens. Phoenix proposes, you tap, it acts."
    ),
    GenesisPower(
        "phoenix-queue", "COMMAND",
        "Phoenix's tap queue",
        "Briefs, nudges and follow-ups generated on this phone. \"Copy for Zane\" " +
            "hands a job to the machine — you paste it in chat, it gets built."
    ),
    GenesisPower(
        "nodes", "MESH",
        "Genesis nodes",
        "Every machine: this phone live, zane-box and penguin over the tailnet. " +
            "Honest probes — online means reached, unreachable means it didn't."
    ),
    GenesisPower(
        "system", "MESH",
        "System state",
        "The stored snapshot from Zane's machine — GPU quota, god brains, " +
            "fabric queue, jobs, money. Labeled snapshot, never faked as live."
    ),
    GenesisPower(
        "forge", "BUILD",
        "Forge",
        "Draft builds through the Forge gateway. Agents can also drop drafts " +
            "here through the Agent Bridge."
    ),
    GenesisPower(
        "gods", "COMMAND",
        "God dock",
        "Eight gods, eight quick actions. Tap launches, long-press assigns any " +
            "installed app. Your pantheon, your bindings."
    ),
    GenesisPower(
        "drawer", "COMMAND",
        "App drawer + universal search",
        "Every installed app, alphabetical and searchable — plus contacts and " +
            "math, right in the search bar."
    ),
    GenesisPower(
        "wallpaper", "MODIFY",
        "Quantum wallpaper",
        "Apply the living ember-mandala as your system wallpaper. Or ask Phoenix: " +
            "\"set the wallpaper\"."
    ),
    GenesisPower(
        "theme", "MODIFY",
        "Theme engine",
        "Recolor and reshape the whole launcher — background, dock, dragon, " +
            "phoenix FX, motion, fonts, gestures. Export and share themes as JSON."
    ),
    GenesisPower(
        "gestures", "MODIFY",
        "Gestures",
        "Swipe up/down, double-tap, pinch, two-finger tap — every one mapped " +
            "to an action you choose in the theme engine."
    ),
    GenesisPower(
        "companion", "COMMAND",
        "Always-on Phoenix",
        "Keep Phoenix alive with a persistent notification, plus an optional " +
            "floating companion over other apps. Your tap starts it, your tap stops it."
    ),
    GenesisPower(
        "bridge", "BUILD",
        "Agent Bridge",
        "On-device broadcast protocol: OpenHands, OpenManus, OpenClaw and " +
            "ZeroClaw can ask Phoenix things and launch god apps from outside."
    ),
)

/**
 * Powers screen: everything Genesis can do, everything Brandon can modify.
 * Tapping a row fires its real action via [onPower].
 */
@Composable
fun PowersScreen(
    onPower: (String) -> Unit,
    onBack: () -> Unit,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val groups = GENESIS_POWERS.groupBy { it.group }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060814).copy(alpha = 0.92f))
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = "GENESIS POWERS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "everything it can do · everything you can change",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedStar
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for ((group, powers) in groups) {
                item(key = "h-$group") {
                    Text(
                        text = group,
                        color = PhoenixGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                items(powers, key = { "p-${it.id}" }) { power ->
                    PowerRow(power = power, onTap = { onPower(power.id) })
                }
            }
        }
    }
}

@Composable
private fun PowerRow(power: GenesisPower, onTap: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = power.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.padding(top = 2.dp))
                Text(
                    text = power.blurb,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = PhoenixGold
            )
        }
    }
}
