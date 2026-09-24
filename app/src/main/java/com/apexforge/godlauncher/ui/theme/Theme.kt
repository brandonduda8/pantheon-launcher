package com.apexforge.godlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PantheonColorScheme = darkColorScheme(
    primary = NebulaViolet,
    onPrimary = Color.White,
    secondary = NebulaTeal,
    onSecondary = Color.Black,
    tertiary = NebulaMagenta,
    onTertiary = Color.Black,
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
