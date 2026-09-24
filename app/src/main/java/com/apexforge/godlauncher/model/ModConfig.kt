package com.apexforge.godlauncher.model

import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// Pantheon Mod Engine — every swappable knob of the launcher in one place.
// PantheonConfig is JSON-serializable (enums as names, numbers as
// Float/Int, colors as "#RRGGBB" strings) so themes export/import/share
// without a rebuild. Persisted by ModStore in the existing DataStore.
// ============================================================================

enum class LoopId { QUANTUM_FIELD, EMBER_STORM, NEBULA_DRIFT, STATIC_GRADIENT }
enum class ParticleType { SPARKS, MOTES, PIXELS, ORBS }
enum class DockStyle { EMBER_RING, MINIMAL, GLASS, NEON }
enum class DockPosition { BOTTOM, TOP, LEFT, RIGHT, HIDDEN }
enum class PageIndicatorStyle { DOTS, LINES, NONE, DIAMONDS }
enum class IconShape { CIRCLE, SQUIRCLE, HEXAGON, RAW }
enum class AppTransition { FADE, SCALE, SLIDE, QUANTUM_ZOOM }
enum class PageTransition { CUBE, FADE, SLIDE, FLIP }
enum class GestureAction {
    OPEN_DRAWER, OPEN_SEARCH, OPEN_PHOENIX, TOGGLE_SHOWROOM, NEXT_THEME, NONE
}
enum class SearchBarStyle { ROUNDED, PILL, UNDERLINE, HIDDEN }
enum class SearchBarPosition { TOP, BOTTOM }
enum class BadgeStyle { DOT, COUNT, GLOW }
enum class DragonSkin { EMBER, VOID, QUANTUM, GOLD }
enum class DragonBehavior { CALM, PLAYFUL, HYPER }
enum class DragonPosition { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT, HIDDEN }
enum class CommandBarStyle { EMBER_GLOW, GLASS, MINIMAL }
enum class Profile { DEFAULT, FOCUS, NIGHT, SHOWROOM }
enum class PantheonPreset { EMBER_REBIRTH, QUANTUM_MACHINE, VOID_MINIMAL }

/** Brand defaults shared by presets. */
object ModBrand {
    const val ASH_VOID = "#0B0A09"
    const val CHARCOAL = "#12100E"
    const val EMBER = "#E25822"
    const val GOLD = "#F5B942"
    const val QUANTUM_CYAN = "#22D3EE"
    const val BONE = "#F5F0E8"
}

data class PantheonConfig(
    // -- background --
    val loopId: LoopId = LoopId.QUANTUM_FIELD,
    val loopSpeed: Float = 1f,
    val loopDensity: Float = 0.7f,
    val hueShift: Float = 0f,
    val particleType: ParticleType = ParticleType.SPARKS,
    val particleDensity: Float = 0.6f,
    val particleSpeed: Float = 1f,
    val particleColor: String = ModBrand.EMBER,
    val vignette: Float = 0.45f,
    val dimLevel: Float = 0f,
    val blurLevel: Float = 0f,
    // -- layout --
    val gridRows: Int = 4,
    val gridCols: Int = 4,
    val iconSize: Float = 1f,
    val labelsVisible: Boolean = true,
    val labelSize: Float = 1f,
    val dockSlots: Int = 8,
    val dockStyle: DockStyle = DockStyle.EMBER_RING,
    val dockPosition: DockPosition = DockPosition.BOTTOM,
    val pageIndicatorStyle: PageIndicatorStyle = PageIndicatorStyle.DOTS,
    val infiniteScroll: Boolean = false,
    /** God ids in dock order. Uses the same god ids as PantheonStore keys. */
    val dockOrder: List<String> = PANTHEON.map { it.id },
    val dockHidden: Set<String> = emptySet(),
    // -- icons --
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconGlow: Float = 0.5f,
    val neonTint: Boolean = true,
    val tiltEffect: Boolean = false,
    // -- color & type --
    val colorBackground: String = ModBrand.ASH_VOID,
    val colorAccent: String = ModBrand.EMBER,
    val colorText: String = ModBrand.BONE,
    val colorDock: String = ModBrand.CHARCOAL,
    val colorGodRing: String = ModBrand.GOLD,
    val fontScale: Float = 1f,
    val transparency: Float = 0.9f,
    // -- motion --
    val animationSpeed: Float = 1f,
    val appTransition: AppTransition = AppTransition.QUANTUM_ZOOM,
    val pageTransition: PageTransition = PageTransition.CUBE,
    val dockPulseWave: Boolean = true,
    // -- gestures --
    val gestureSwipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val gestureSwipeDown: GestureAction = GestureAction.OPEN_SEARCH,
    val gestureDoubleTap: GestureAction = GestureAction.OPEN_PHOENIX,
    val gesturePinch: GestureAction = GestureAction.NEXT_THEME,
    val gestureTwoFingerTap: GestureAction = GestureAction.TOGGLE_SHOWROOM,
    // -- search & badges --
    val searchBarStyle: SearchBarStyle = SearchBarStyle.PILL,
    val searchBarPosition: SearchBarPosition = SearchBarPosition.TOP,
    val suggestionCount: Int = 5,
    val badgeStyle: BadgeStyle = BadgeStyle.GLOW,
    // -- dragon --
    val dragonSkin: DragonSkin = DragonSkin.VOID,
    val dragonBehavior: DragonBehavior = DragonBehavior.PLAYFUL,
    val dragonPosition: DragonPosition = DragonPosition.BOTTOM_RIGHT,
    val dragonSize: Float = 1f,
    // -- command bar --
    val commandBarVisible: Boolean = true,
    val commandBarStyle: CommandBarStyle = CommandBarStyle.EMBER_GLOW,
    // -- quantum agent view --
    val quantumViewEnabled: Boolean = true,
    // -- profiles --
    val activeProfile: Profile = Profile.DEFAULT,
    // -- showroom --
    val showroomEnabled: Boolean = false,
    val showroomSeconds: Int = 10
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("loopId", loopId.name)
        put("loopSpeed", loopSpeed.toDouble())
        put("loopDensity", loopDensity.toDouble())
        put("hueShift", hueShift.toDouble())
        put("particleType", particleType.name)
        put("particleDensity", particleDensity.toDouble())
        put("particleSpeed", particleSpeed.toDouble())
        put("particleColor", particleColor)
        put("vignette", vignette.toDouble())
        put("dimLevel", dimLevel.toDouble())
        put("blurLevel", blurLevel.toDouble())
        put("gridRows", gridRows)
        put("gridCols", gridCols)
        put("iconSize", iconSize.toDouble())
        put("labelsVisible", labelsVisible)
        put("labelSize", labelSize.toDouble())
        put("dockSlots", dockSlots)
        put("dockStyle", dockStyle.name)
        put("dockPosition", dockPosition.name)
        put("pageIndicatorStyle", pageIndicatorStyle.name)
        put("infiniteScroll", infiniteScroll)
        put("dockOrder", JSONArray(dockOrder))
        put("dockHidden", JSONArray(dockHidden.toList()))
        put("iconShape", iconShape.name)
        put("iconGlow", iconGlow.toDouble())
        put("neonTint", neonTint)
        put("tiltEffect", tiltEffect)
        put("colorBackground", colorBackground)
        put("colorAccent", colorAccent)
        put("colorText", colorText)
        put("colorDock", colorDock)
        put("colorGodRing", colorGodRing)
        put("fontScale", fontScale.toDouble())
        put("transparency", transparency.toDouble())
        put("animationSpeed", animationSpeed.toDouble())
        put("appTransition", appTransition.name)
        put("pageTransition", pageTransition.name)
        put("dockPulseWave", dockPulseWave)
        put("gestureSwipeUp", gestureSwipeUp.name)
        put("gestureSwipeDown", gestureSwipeDown.name)
        put("gestureDoubleTap", gestureDoubleTap.name)
        put("gesturePinch", gesturePinch.name)
        put("gestureTwoFingerTap", gestureTwoFingerTap.name)
        put("searchBarStyle", searchBarStyle.name)
        put("searchBarPosition", searchBarPosition.name)
        put("suggestionCount", suggestionCount)
        put("badgeStyle", badgeStyle.name)
        put("dragonSkin", dragonSkin.name)
        put("dragonBehavior", dragonBehavior.name)
        put("dragonPosition", dragonPosition.name)
        put("dragonSize", dragonSize.toDouble())
        put("commandBarVisible", commandBarVisible)
        put("commandBarStyle", commandBarStyle.name)
        put("quantumViewEnabled", quantumViewEnabled)
        put("activeProfile", activeProfile.name)
        put("showroomEnabled", showroomEnabled)
        put("showroomSeconds", showroomSeconds)
    }

    companion object {
        const val VERSION = 3

        fun fromJson(json: JSONObject): PantheonConfig {
            val d = PantheonConfig()
            fun f(key: String, fb: Float) = json.optDouble(key, fb.toDouble()).toFloat()
            fun i(key: String, fb: Int) = json.optInt(key, fb)
            fun s(key: String, fb: String) = json.optString(key, fb)
            fun b(key: String, fb: Boolean) = json.optBoolean(key, fb)
            return d.copy(
                loopId = optEnum(json, "loopId", d.loopId),
                loopSpeed = f("loopSpeed", d.loopSpeed).coerceIn(0.25f, 2f),
                loopDensity = f("loopDensity", d.loopDensity).coerceIn(0f, 1f),
                hueShift = f("hueShift", d.hueShift).coerceIn(0f, 360f),
                particleType = optEnum(json, "particleType", d.particleType),
                particleDensity = f("particleDensity", d.particleDensity).coerceIn(0f, 1f),
                particleSpeed = f("particleSpeed", d.particleSpeed).coerceIn(0.25f, 2f),
                particleColor = s("particleColor", d.particleColor),
                vignette = f("vignette", d.vignette).coerceIn(0f, 1f),
                dimLevel = f("dimLevel", d.dimLevel).coerceIn(0f, 1f),
                blurLevel = f("blurLevel", d.blurLevel).coerceIn(0f, 1f),
                gridRows = i("gridRows", d.gridRows).coerceIn(3, 7),
                gridCols = i("gridCols", d.gridCols).coerceIn(3, 6),
                iconSize = f("iconSize", d.iconSize).coerceIn(0.7f, 1.4f),
                labelsVisible = b("labelsVisible", d.labelsVisible),
                labelSize = f("labelSize", d.labelSize).coerceIn(0.7f, 1.4f),
                dockSlots = i("dockSlots", d.dockSlots).coerceIn(4, 8),
                dockStyle = optEnum(json, "dockStyle", d.dockStyle),
                dockPosition = optEnum(json, "dockPosition", d.dockPosition),
                pageIndicatorStyle = optEnum(json, "pageIndicatorStyle", d.pageIndicatorStyle),
                infiniteScroll = b("infiniteScroll", d.infiniteScroll),
                dockOrder = optStringList(json, "dockOrder", d.dockOrder),
                dockHidden = optStringList(json, "dockHidden", d.dockHidden.toList()).toSet(),
                iconShape = optEnum(json, "iconShape", d.iconShape),
                iconGlow = f("iconGlow", d.iconGlow).coerceIn(0f, 1f),
                neonTint = b("neonTint", d.neonTint),
                tiltEffect = b("tiltEffect", d.tiltEffect),
                colorBackground = s("colorBackground", d.colorBackground),
                colorAccent = s("colorAccent", d.colorAccent),
                colorText = s("colorText", d.colorText),
                colorDock = s("colorDock", d.colorDock),
                colorGodRing = s("colorGodRing", d.colorGodRing),
                fontScale = f("fontScale", d.fontScale).coerceIn(0.8f, 1.3f),
                transparency = f("transparency", d.transparency).coerceIn(0f, 1f),
                animationSpeed = f("animationSpeed", d.animationSpeed).coerceIn(0.25f, 2f),
                appTransition = optEnum(json, "appTransition", d.appTransition),
                pageTransition = optEnum(json, "pageTransition", d.pageTransition),
                dockPulseWave = b("dockPulseWave", d.dockPulseWave),
                gestureSwipeUp = optEnum(json, "gestureSwipeUp", d.gestureSwipeUp),
                gestureSwipeDown = optEnum(json, "gestureSwipeDown", d.gestureSwipeDown),
                gestureDoubleTap = optEnum(json, "gestureDoubleTap", d.gestureDoubleTap),
                gesturePinch = optEnum(json, "gesturePinch", d.gesturePinch),
                gestureTwoFingerTap = optEnum(json, "gestureTwoFingerTap", d.gestureTwoFingerTap),
                searchBarStyle = optEnum(json, "searchBarStyle", d.searchBarStyle),
                searchBarPosition = optEnum(json, "searchBarPosition", d.searchBarPosition),
                suggestionCount = i("suggestionCount", d.suggestionCount).coerceIn(3, 8),
                badgeStyle = optEnum(json, "badgeStyle", d.badgeStyle),
                dragonSkin = optEnum(json, "dragonSkin", d.dragonSkin),
                dragonBehavior = optEnum(json, "dragonBehavior", d.dragonBehavior),
                dragonPosition = optEnum(json, "dragonPosition", d.dragonPosition),
                dragonSize = f("dragonSize", d.dragonSize).coerceIn(0.6f, 1.6f),
                commandBarVisible = b("commandBarVisible", d.commandBarVisible),
                commandBarStyle = optEnum(json, "commandBarStyle", d.commandBarStyle),
                quantumViewEnabled = b("quantumViewEnabled", d.quantumViewEnabled),
                activeProfile = optEnum(json, "activeProfile", d.activeProfile),
                showroomEnabled = b("showroomEnabled", d.showroomEnabled),
                showroomSeconds = i("showroomSeconds", d.showroomSeconds).coerceIn(3, 60)
            )
        }

        fun fromJsonString(raw: String): PantheonConfig =
            fromJson(JSONObject(raw))

        private inline fun <reified T : Enum<T>> optEnum(
            json: JSONObject, key: String, default: T
        ): T = try {
            val name = json.optString(key, default.name)
            enumValueOf<T>(name)
        } catch (_: Exception) {
            default
        }

        private fun optStringList(
            json: JSONObject, key: String, default: List<String>
        ): List<String> = try {
            val arr = json.optJSONArray(key) ?: return default
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
                .ifEmpty { default }
        } catch (_: Exception) {
            default
        }
    }
}

/** The three built-in presets. */
fun PantheonPreset.config(): PantheonConfig = when (this) {
    PantheonPreset.EMBER_REBIRTH -> PantheonConfig(
        loopId = LoopId.EMBER_STORM,
        loopDensity = 0.8f,
        particleType = ParticleType.SPARKS,
        particleColor = ModBrand.EMBER,
        dockStyle = DockStyle.EMBER_RING,
        dragonSkin = DragonSkin.EMBER,
        dragonBehavior = DragonBehavior.PLAYFUL,
        commandBarStyle = CommandBarStyle.EMBER_GLOW,
        colorBackground = ModBrand.ASH_VOID,
        colorAccent = ModBrand.EMBER,
        colorText = ModBrand.BONE,
        colorDock = ModBrand.CHARCOAL,
        colorGodRing = ModBrand.GOLD,
        quantumViewEnabled = true,
        badgeStyle = BadgeStyle.GLOW
    )
    PantheonPreset.QUANTUM_MACHINE -> PantheonConfig(
        loopId = LoopId.QUANTUM_FIELD,
        loopDensity = 0.85f,
        hueShift = 185f,
        particleType = ParticleType.PIXELS,
        particleColor = ModBrand.QUANTUM_CYAN,
        particleSpeed = 1.25f,
        dockStyle = DockStyle.NEON,
        iconShape = IconShape.HEXAGON,
        neonTint = true,
        commandBarStyle = CommandBarStyle.GLASS,
        colorBackground = "#07090D",
        colorAccent = ModBrand.QUANTUM_CYAN,
        colorText = "#E8F6FF",
        colorDock = "#0A1218",
        colorGodRing = ModBrand.QUANTUM_CYAN,
        quantumViewEnabled = true,
        dragonSkin = DragonSkin.QUANTUM,
        dragonBehavior = DragonBehavior.HYPER,
        appTransition = AppTransition.QUANTUM_ZOOM,
        badgeStyle = BadgeStyle.GLOW
    )
    PantheonPreset.VOID_MINIMAL -> PantheonConfig(
        loopId = LoopId.STATIC_GRADIENT,
        loopDensity = 0.2f,
        particleDensity = 0.15f,
        particleColor = "#9AA3B2",
        dockStyle = DockStyle.MINIMAL,
        iconShape = IconShape.SQUIRCLE,
        iconGlow = 0.12f,
        neonTint = false,
        labelsVisible = false,
        dockPulseWave = false,
        tiltEffect = false,
        commandBarStyle = CommandBarStyle.MINIMAL,
        colorBackground = "#080808",
        colorAccent = "#9AA3B2",
        colorText = "#EDEDED",
        colorDock = "#0E0E0E",
        colorGodRing = "#9AA3B2",
        quantumViewEnabled = false,
        dragonSkin = DragonSkin.VOID,
        dragonBehavior = DragonBehavior.CALM,
        vignette = 0.6f,
        transparency = 0.75f,
        badgeStyle = BadgeStyle.DOT,
        animationSpeed = 0.75f
    )
}

/** First-run config used when a profile has no stored theme yet. */
fun Profile.defaultConfig(): PantheonConfig = when (this) {
    Profile.DEFAULT -> PantheonPreset.EMBER_REBIRTH.config()
    Profile.FOCUS -> PantheonPreset.VOID_MINIMAL.config()
    Profile.NIGHT -> PantheonPreset.EMBER_REBIRTH.config().copy(
        loopId = LoopId.NEBULA_DRIFT,
        loopDensity = 0.5f,
        dimLevel = 0.35f,
        vignette = 0.65f,
        dragonBehavior = DragonBehavior.CALM,
        animationSpeed = 0.6f
    )
    Profile.SHOWROOM -> PantheonPreset.QUANTUM_MACHINE.config().copy(
        showroomEnabled = true,
        showroomSeconds = 10
    )
}
