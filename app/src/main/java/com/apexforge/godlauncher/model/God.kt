package com.apexforge.godlauncher.model

import androidx.compose.ui.graphics.Color

/**
 * One of the eight Pantheon quick-actions on the home dock.
 * v1 renders each god as a gradient medallion with a two-letter monogram —
 * zero bundled image assets, still unmistakably god-tier.
 */
data class God(
    val id: String,
    val name: String,
    val domain: String,
    val glyph: String,
    val colorA: Color,
    val colorB: Color
)

val PANTHEON: List<God> = listOf(
    God("zeus", "Zeus", "Command", "ZE", Color(0xFFFBBF24), Color(0xFFF97316)),
    God("athena", "Athena", "Wisdom", "AT", Color(0xFF60A5FA), Color(0xFF2563EB)),
    God("hermes", "Hermes", "Messages", "HE", Color(0xFF2DD4BF), Color(0xFF0D9488)),
    God("hephaestus", "Hephaestus", "Forge", "HP", Color(0xFFF87171), Color(0xFFDC2626)),
    God("tyche", "Tyche", "Fortune", "TY", Color(0xFFA3E635), Color(0xFF65A30D)),
    God("argus", "Argus", "Watch", "AR", Color(0xFFA78BFA), Color(0xFF7C3AED)),
    God("odysseus", "Odysseus", "Journey", "OD", Color(0xFF22D3EE), Color(0xFF0891B2)),
    God("themis", "Themis", "Justice", "TH", Color(0xFFF472B6), Color(0xFFDB2777))
)
