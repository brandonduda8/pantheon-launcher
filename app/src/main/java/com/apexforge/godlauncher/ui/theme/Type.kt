package com.apexforge.godlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Dark-theme-only typography. The clock gets the hero treatment. */
val PantheonTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 68.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1.5).sp,
        color = StarWhite
    ),
    displayMedium = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Normal,
        color = StarWhite
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = StarWhite
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = StarWhite
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = MutedStar
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MutedStar
    )
)
