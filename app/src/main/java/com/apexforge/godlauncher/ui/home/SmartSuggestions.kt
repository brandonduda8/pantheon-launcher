package com.apexforge.godlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.rememberAppIcon
import com.apexforge.godlauncher.model.AppInfo
import com.apexforge.godlauncher.ui.theme.PhoenixGold

/**
 * "Rising now": the predicted-apps strip above the god dock, learned
 * purely on-device from Brandon's own launch history (see LaunchHistory).
 * Static UI — no motion — so it ignores the animations toggle.
 */
@Composable
fun SmartSuggestions(
    suggestions: List<AppInfo>,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = "Rising now",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = PhoenixGold.copy(alpha = 0.85f),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            items(suggestions, key = { it.packageName }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLaunchApp(app.packageName) }
                        .padding(vertical = 6.dp)
                ) {
                    val icon = rememberAppIcon(app.packageName)
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Text(
                            text = app.label.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = PhoenixGold.copy(alpha = 0.85f),
                            modifier = Modifier.size(46.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}
