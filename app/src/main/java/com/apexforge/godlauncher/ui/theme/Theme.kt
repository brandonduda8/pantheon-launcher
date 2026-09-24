package com.apexforge.godlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PantheonColorScheme = darkColorScheme(
    // Brand soul: charcoal ashes into ember orange and phoenix gold.
    primary = EmberOrange,
    onPrimary = Color.Black,
    secondary = PhoenixGold,
    onSecondary = Color.Black,
    tertiary = Color(0xFFE25822),
    onTertiary = Color.White,
    background = VoidBlack,
    onBackground = StarWhite,
    surface = Color(0xFF0D1020),
    onSurface = StarWhite,
    surfaceVariant = Color(0xFF161A2E),
    onSurfaceVariant = MutedStar,
    outline = Color(0xFF2A2F45)
)

@Composable
fun PantheonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PantheonColorScheme,
        typography = PantheonTypography,
        content = content
    )
}
