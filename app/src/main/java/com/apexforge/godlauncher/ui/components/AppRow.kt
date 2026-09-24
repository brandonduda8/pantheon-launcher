package com.apexforge.godlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.rememberAppIcon
import com.apexforge.godlauncher.model.AppInfo
import com.apexforge.godlauncher.ui.icons.QuantumFrame
import com.apexforge.godlauncher.ui.theme.EmberOrange

/**
 * v6: resolves the icon lazily via [rememberAppIcon] and shows a
 * first-letter tile while it loads, instead of pinning every installed
 * app's bitmap at startup.
 */
@Composable
private fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier) {
    val icon = rememberAppIcon(packageName)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = label,
            modifier = modifier
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = label.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Single app row shared by search results and the picker dialog. */
@Composable
fun AppRow(
    app: AppInfo,
    onClick: () -> Unit,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        QuantumFrame(
            animationsEnabled = animationsEnabled,
            modifier = Modifier.size(44.dp)
        ) {
            AppIcon(
                packageName = app.packageName,
                label = app.label,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Grid cell for the full app drawer, with an optional ember unread badge. */
@Composable
fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    iconSize: Float = 1f,
    labelsVisible: Boolean = true,
    labelSize: Float = 1f,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(8.dp)
        ) {
            QuantumFrame(
                animationsEnabled = animationsEnabled,
                modifier = Modifier.size((56f * iconSize).dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    label = app.label,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (labelsVisible) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.label,
                    fontSize = (11f * labelSize).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (badgeCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EmberOrange)
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
