package com.apexforge.godlauncher.ui.vortex

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.apexforge.godlauncher.model.VortexTokens

private fun hex(s: String) = Color(("FF" + s.removePrefix("#")).toLong(16))

/** Compose colors derived from [VortexTokens] (single source of truth). */
object VortexColors {
    val VoidBlack = hex(VortexTokens.VOID_BLACK)
    val AbyssPurple = hex(VortexTokens.ABYSS_PURPLE)
    val ElectricViolet = hex(VortexTokens.ELECTRIC_VIOLET)
    val PlasmaMagenta = hex(VortexTokens.PLASMA_MAGENTA)
    val StormBlue = hex(VortexTokens.STORM_BLUE)
    val EmberGold = hex(VortexTokens.EMBER_GOLD)
    val DragonEmber = hex(VortexTokens.DRAGON_EMBER)
    val SilverMist = hex(VortexTokens.SILVER_MIST)
    val TextPrimary = hex(VortexTokens.TEXT_PRIMARY)
    val TextMuted = hex(VortexTokens.TEXT_MUTED)
    val Surface = hex(VortexTokens.SURFACE)
    val SurfaceElevated = hex(VortexTokens.SURFACE_ELEVATED)
    val Border = hex(VortexTokens.BORDER)
}

val VortexColorScheme = darkColorScheme(
    primary = VortexColors.ElectricViolet,
    onPrimary = VortexColors.TextPrimary,
    secondary = VortexColors.PlasmaMagenta,
    onSecondary = VortexColors.VoidBlack,
    tertiary = VortexColors.DragonEmber,
    onTertiary = VortexColors.VoidBlack,
    background = VortexColors.VoidBlack,
    onBackground = VortexColors.TextPrimary,
    surface = VortexColors.Surface,
    onSurface = VortexColors.TextPrimary,
    surfaceVariant = VortexColors.SurfaceElevated,
    onSurfaceVariant = VortexColors.TextMuted,
    outline = VortexColors.Border
)
