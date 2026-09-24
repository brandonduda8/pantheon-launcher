package com.apexforge.godlauncher.ui.nodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.NodeReach
import com.apexforge.godlauncher.data.SystemSnapshot
import com.apexforge.godlauncher.ui.system.SystemScreen
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar

/**
 * GENESIS — the interface into everything. Three views, one roof:
 *
 * - NODES: every machine, honestly probed.
 * - POWERS: everything it can do, everything Brandon can change.
 * - SYSTEM: the stored snapshot from Zane's machine (labeled as such).
 *
 * This is the screen Brandon asked for: the void that shows everything
 * Genesis is and everything it can become.
 */
@Composable
fun GenesisScreen(
    snapshot: SystemSnapshot?,
    reach: Map<String, NodeReach>,
    probing: Boolean,
    onProbeNodes: () -> Unit,
    onPower: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("NODES", "POWERS", "SYSTEM")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060814))
    ) {
        // The void header — ember on charcoal.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            EmberOrange.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
        ) {
            Column {
                Text(
                    text = "GENESIS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "the interface into everything",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStar
                )
            }
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[tab]),
                    color = EmberOrange
                )
            }
        ) {
            titles.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            title,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp,
                            color = if (tab == i) Color.White else MutedStar
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> NodesScreen(
                    reach = reach,
                    probing = probing,
                    onRefresh = onProbeNodes,
                    onBack = onBack,
                    showHeader = false
                )
                1 -> PowersScreen(
                    onPower = onPower,
                    onBack = onBack,
                    showHeader = false
                )
                else -> SystemScreen(
                    snapshot = snapshot,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
