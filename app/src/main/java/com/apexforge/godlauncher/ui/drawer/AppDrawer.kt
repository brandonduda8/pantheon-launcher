package com.apexforge.godlauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apexforge.godlauncher.model.AppInfo
import com.apexforge.godlauncher.ui.components.AppGridItem
import com.apexforge.godlauncher.ui.theme.VoidBlack

/**
 * Full-screen app drawer: every installed launcher app, alphabetical,
 * filterable. Back button (or system back) returns home.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onLaunch: (String) -> Unit,
    onClose: () -> Unit,
    badgeCounts: Map<String, Int> = emptyMap(),
    gridCols: Int = 4,
    iconSize: Float = 1f,
    labelsVisible: Boolean = true,
    labelSize: Float = 1f,
    onSearch: () -> Unit = {},
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    // Odysseus pulse: the journey begins the moment a drawer search starts.
    var wasBlank by remember { mutableStateOf(true) }
    LaunchedEffect(query) {
        val blank = query.isBlank()
        if (!blank && wasBlank) onSearch()
        wasBlank = blank
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols.coerceIn(3, 6)),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.packageName }) { app ->
                AppGridItem(
                    app = app,
                    onClick = { onLaunch(app.packageName) },
                    badgeCount = badgeCounts[app.packageName] ?: 0,
                    iconSize = iconSize,
                    labelsVisible = labelsVisible,
                    labelSize = labelSize,
                    animationsEnabled = animationsEnabled
                )
            }
        }
    }
}
