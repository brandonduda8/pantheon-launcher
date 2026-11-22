package com.apexforge.godlauncher.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Genesis Vortex token contract, mirrored from design/genesis-vortex.tokens.json.
 * VortexTokensTest fails the build if this drifts from the JSON.
 */
object VortexTokens {
    const val TOKENS_VERSION = "1.0.0"
    const val PRESET_NAME = "VORTEX // EMBER"

    const val VOID_BLACK = "#06070B"
    const val ABYSS_PURPLE = "#2B0A59"
    const val ELECTRIC_VIOLET = "#7A2BFF"
    const val PLASMA_MAGENTA = "#D14BFF"
    const val STORM_BLUE = "#3EC8FF"
    const val EMBER_GOLD = "#F2A63B"
    const val DRAGON_EMBER = "#FF6A2B"
    const val SILVER_MIST = "#C8D2E8"
    const val TEXT_PRIMARY = "#F5F3FF"
    const val TEXT_MUTED = "#B5A9C9"
    const val SURFACE = "#10101C"
    const val SURFACE_ELEVATED = "#19132A"
    const val BORDER = "#5D348F"

    val PALETTE: Map<String, String> = linkedMapOf(
        "voidBlack" to VOID_BLACK,
        "abyssPurple" to ABYSS_PURPLE,
        "electricViolet" to ELECTRIC_VIOLET,
        "plasmaMagenta" to PLASMA_MAGENTA,
        "stormBlue" to STORM_BLUE,
        "emberGold" to EMBER_GOLD,
        "dragonEmber" to DRAGON_EMBER,
        "silverMist" to SILVER_MIST,
        "textPrimary" to TEXT_PRIMARY,
        "textMuted" to TEXT_MUTED,
        "surface" to SURFACE,
        "surfaceElevated" to SURFACE_ELEVATED,
        "border" to BORDER
    )

    const val RADIUS_PANEL_DP = 20
    const val RADIUS_CONTROL_DP = 12
    const val MIN_TOUCH_TARGET_DP = 48
    val DEFAULT_MOTION = MotionMode.BALANCED

    /** WCAG 2.x relative-luminance contrast ratio between two #RRGGBB colors. */
    fun contrast(a: String, b: String): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(hex: String): Double {
        val v = hex.removePrefix("#").toLong(16)
        fun ch(shift: Int): Double {
            val c = ((v shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
    }
}

/**
 * VORTEX // EMBER preset: opt-in, reversible skin layered on PantheonConfig.
 * Pure functions so apply / undo / restore / export are unit-testable.
 */
object VortexPreset {
    const val SCHEMA = "genesis-vortex/preset"
    const val SCHEMA_VERSION = 1
    const val PRESET_ID = "vortex-ember"

    /** Vortex visuals on top of [base], keeping the user's dock arrangement and profile. */
    fun config(base: PantheonConfig = PantheonConfig()): PantheonConfig = PantheonConfig(
        brandSkin = BrandSkin.VORTEX_EMBER,
        motionMode = if (base.brandSkin == BrandSkin.VORTEX_EMBER) base.motionMode else VortexTokens.DEFAULT_MOTION,
        loopId = LoopId.STATIC_GRADIENT,
        loopDensity = 0.3f,
        particleType = ParticleType.MOTES,
        particleDensity = 0.2f,
        particleColor = VortexTokens.DRAGON_EMBER,
        vignette = 0.35f,
        dockStyle = DockStyle.GLASS,
        dockSlots = 5,
        iconShape = IconShape.SQUIRCLE,
        iconGlow = 0.3f,
        neonTint = false,
        tiltEffect = false,
        colorBackground = VortexTokens.VOID_BLACK,
        colorAccent = VortexTokens.ELECTRIC_VIOLET,
        colorText = VortexTokens.TEXT_PRIMARY,
        colorDock = VortexTokens.SURFACE,
        colorGodRing = VortexTokens.PLASMA_MAGENTA,
        appTransition = AppTransition.FADE,
        pageTransition = PageTransition.FADE,
        dockPulseWave = false,
        gesturePinch = GestureAction.NONE,
        gestureTwoFingerTap = GestureAction.NONE,
        searchBarStyle = SearchBarStyle.PILL,
        badgeStyle = BadgeStyle.DOT,
        dragonPosition = DragonPosition.HIDDEN,
        commandBarVisible = false,
        quantumViewEnabled = false,
        showroomEnabled = false,
        // preserved user state
        dockOrder = base.dockOrder,
        dockHidden = base.dockHidden,
        dockPosition = base.dockPosition,
        searchBarPosition = base.searchBarPosition,
        labelsVisible = base.labelsVisible,
        fontScale = base.fontScale,
        activeProfile = base.activeProfile,
        gestureSwipeUp = base.gestureSwipeUp,
        gestureSwipeDown = base.gestureSwipeDown,
        gestureDoubleTap = base.gestureDoubleTap
    )

    data class Applied(val config: PantheonConfig, val undoJson: String?)

    /**
     * Applies Vortex. The undo snapshot is the pre-Vortex config; re-applying
     * while already on Vortex keeps the original snapshot instead of overwriting it.
     */
    fun apply(current: PantheonConfig, existingUndo: String?): Applied {
        val undo = if (current.brandSkin == BrandSkin.VORTEX_EMBER) existingUndo
        else current.toJson().toString()
        return Applied(config(current), undo)
    }

    /** Returns the pre-Vortex snapshot, or the shipping default when none/corrupt. */
    fun undo(current: PantheonConfig, undoJson: String?): PantheonConfig =
        undoJson?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
            ?: restoreDefault(current)

    /** Shipping Pantheon default, keeping dock arrangement (assignments live elsewhere). */
    fun restoreDefault(current: PantheonConfig): PantheonConfig =
        Profile.DEFAULT.defaultConfig().copy(
            dockOrder = current.dockOrder,
            dockHidden = current.dockHidden,
            activeProfile = current.activeProfile
        )

    /**
     * Effective motion after OS/user constraints. Battery saver, reduced
     * motion (animator scale 0), launcher animations off, or not visible
     * all force STATIC.
     */
    fun effectiveMotion(
        requested: MotionMode,
        animationsEnabled: Boolean,
        powerSave: Boolean,
        reduceMotion: Boolean,
        visible: Boolean
    ): MotionMode =
        if (!animationsEnabled || powerSave || reduceMotion || !visible) MotionMode.STATIC else requested

    /**
     * Shareable preset descriptor: schema, tokens, icon pack, migration.
     * Contains only visual settings; never keys, gateways, contacts, memory.
     */
    fun descriptor(config: PantheonConfig): JSONObject = JSONObject()
        .put("schema", SCHEMA)
        .put("schemaVersion", SCHEMA_VERSION)
        .put("presetId", PRESET_ID)
        .put("presetName", VortexTokens.PRESET_NAME)
        .put("tokensVersion", VortexTokens.TOKENS_VERSION)
        .put("palette", JSONObject(VortexTokens.PALETTE as Map<*, *>))
        .put("motionMode", config.motionMode.name.lowercase())
        .put("motionModes", JSONArray(MotionMode.entries.map { it.name.lowercase() }))
        .put("iconPack", JSONObject().put("id", "vortex-monoline").put("shape", config.iconShape.name))
        .put("fonts", JSONObject()
            .put("display", "sans-serif")
            .put("ui", "sans-serif")
            .put("code", "monospace")
            .put("note", "Orbitron/Space Grotesk/JetBrains Mono not bundled until licenses verified"))
        .put("migration", JSONObject()
            .put("fromConfigVersion", 3)
            .put("toConfigVersion", PantheonConfig.VERSION)
            .put("behavior", "missing brandSkin/motionMode default to PANTHEON/BALANCED; unknown keys ignored"))
        .put("privacy", JSONObject()
            .put("personalFactsInExport", false)
            .put("credentialsInExport", false))
}
