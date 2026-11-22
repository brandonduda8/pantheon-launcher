package com.apexforge.godlauncher.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VortexTokensTest {
    private fun tokensJson(): JSONObject {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "design/genesis-vortex.tokens.json").exists()) dir = dir.parentFile
        return JSONObject(File(dir!!, "design/genesis-vortex.tokens.json").readText())
    }

    @Test fun paletteMatchesDesignTokens() {
        val json = tokensJson()
        val palette = json.getJSONObject("palette")
        assertEquals(palette.length(), VortexTokens.PALETTE.size)
        for (key in palette.keys()) {
            assertEquals("token $key", palette.getString(key).uppercase(), VortexTokens.PALETTE[key]?.uppercase())
        }
        assertEquals(json.getString("version"), VortexTokens.TOKENS_VERSION)
        assertEquals(json.getString("preset"), VortexTokens.PRESET_NAME)
        assertEquals(json.getInt("minTouchTargetDp"), VortexTokens.MIN_TOUCH_TARGET_DP)
        assertEquals(json.getJSONObject("radiiDp").getInt("panel"), VortexTokens.RADIUS_PANEL_DP)
        assertEquals(json.getJSONObject("radiiDp").getInt("control"), VortexTokens.RADIUS_CONTROL_DP)
        assertEquals(json.getString("defaultMotionMode"), VortexTokens.DEFAULT_MOTION.name.lowercase())
        val modes = json.getJSONArray("motionModes").let { a -> List(a.length()) { a.getString(it) } }
        assertEquals(modes, MotionMode.entries.map { it.name.lowercase() })
    }

    @Test fun bodyTextMeetsWcagAA() {
        val bgs = listOf(VortexTokens.VOID_BLACK, VortexTokens.SURFACE, VortexTokens.SURFACE_ELEVATED)
        for (bg in bgs) for (fg in listOf(VortexTokens.TEXT_PRIMARY, VortexTokens.TEXT_MUTED, VortexTokens.SILVER_MIST)) {
            assertTrue("$fg on $bg", VortexTokens.contrast(fg, bg) >= 4.5)
        }
    }

    @Test fun accentsMeetNonTextContrast() {
        for (fg in listOf(VortexTokens.ELECTRIC_VIOLET, VortexTokens.PLASMA_MAGENTA, VortexTokens.STORM_BLUE,
            VortexTokens.DRAGON_EMBER, VortexTokens.EMBER_GOLD)) {
            assertTrue("$fg on void", VortexTokens.contrast(fg, VortexTokens.VOID_BLACK) >= 3.0)
        }
    }
}

class VortexPresetTest {
    private val custom = PantheonPreset.QUANTUM_MACHINE.config().copy(
        dockOrder = listOf("athena", "zeus", "hermes"),
        dockHidden = setOf("tyche"),
        fontScale = 1.3f,
        activeProfile = Profile.NIGHT
    )

    @Test fun applyPreservesDockAndAccessibilityPrefs() {
        val applied = VortexPreset.apply(custom, null).config
        assertEquals(BrandSkin.VORTEX_EMBER, applied.brandSkin)
        assertEquals(custom.dockOrder, applied.dockOrder)
        assertEquals(custom.dockHidden, applied.dockHidden)
        assertEquals(1.3f, applied.fontScale)
        assertEquals(Profile.NIGHT, applied.activeProfile)
        assertEquals(VortexTokens.ELECTRIC_VIOLET, applied.colorAccent)
        assertEquals(MotionMode.BALANCED, applied.motionMode)
        assertFalse(applied.showroomEnabled)
        assertEquals(GestureAction.NONE, applied.gesturePinch)
    }

    @Test fun undoRestoresExactPreviousConfig() {
        val applied = VortexPreset.apply(custom, null)
        val restored = VortexPreset.undo(applied.config, applied.undoJson)
        assertEquals(custom, restored)
    }

    @Test fun reapplyKeepsOriginalSnapshot() {
        val first = VortexPreset.apply(custom, null)
        val tweaked = first.config.copy(motionMode = MotionMode.CINEMATIC)
        val second = VortexPreset.apply(tweaked, first.undoJson)
        assertEquals(first.undoJson, second.undoJson)
        assertEquals(MotionMode.CINEMATIC, second.config.motionMode)
        assertEquals(custom, VortexPreset.undo(second.config, second.undoJson))
    }

    @Test fun undoWithoutSnapshotFallsBackToDefault() {
        val v = VortexPreset.config(custom)
        val r = VortexPreset.undo(v, null)
        assertEquals(BrandSkin.PANTHEON, r.brandSkin)
        assertEquals(custom.dockOrder, r.dockOrder)
        assertEquals(BrandSkin.PANTHEON, VortexPreset.undo(v, "{corrupt").brandSkin)
    }

    @Test fun restoreDefaultIsShippingPantheonWithDockKept() {
        val r = VortexPreset.restoreDefault(VortexPreset.config(custom))
        assertEquals(Profile.DEFAULT.defaultConfig().copy(
            dockOrder = custom.dockOrder, dockHidden = custom.dockHidden, activeProfile = Profile.NIGHT
        ), r)
    }

    @Test fun motionConstraintsForceStatic() {
        val b = MotionMode.CINEMATIC
        assertEquals(b, VortexPreset.effectiveMotion(b, true, false, false, true))
        assertEquals(MotionMode.STATIC, VortexPreset.effectiveMotion(b, false, false, false, true))
        assertEquals(MotionMode.STATIC, VortexPreset.effectiveMotion(b, true, true, false, true))
        assertEquals(MotionMode.STATIC, VortexPreset.effectiveMotion(b, true, false, true, true))
        assertEquals(MotionMode.STATIC, VortexPreset.effectiveMotion(b, true, false, false, false))
    }

    @Test fun v3ConfigMigratesToPantheonBalanced() {
        val v3 = PantheonPreset.VOID_MINIMAL.config().toJson().apply {
            remove("brandSkin"); remove("motionMode")
        }
        val parsed = PantheonConfig.fromJson(v3)
        assertEquals(BrandSkin.PANTHEON, parsed.brandSkin)
        assertEquals(MotionMode.BALANCED, parsed.motionMode)
        assertEquals(PantheonPreset.VOID_MINIMAL.config(), parsed)
        val junk = JSONObject().put("brandSkin", "NOT_A_SKIN").put("motionMode", 7)
        assertEquals(BrandSkin.PANTHEON, PantheonConfig.fromJson(junk).brandSkin)
    }

    @Test fun vortexRoundTripsThroughJson() {
        val v = VortexPreset.config(custom).copy(motionMode = MotionMode.STATIC)
        assertEquals(v, PantheonConfig.fromJsonString(v.toJson().toString()))
    }

    @Test fun descriptorHasSchemaAndNoSecrets() {
        val d = VortexPreset.descriptor(VortexPreset.config())
        assertEquals("genesis-vortex/preset", d.getString("schema"))
        assertEquals(1, d.getInt("schemaVersion"))
        assertEquals(PantheonConfig.VERSION, d.getJSONObject("migration").getInt("toConfigVersion"))
        val all = (d.toString() + VortexPreset.config().toJson().toString()).lowercase()
        for (banned in listOf("apikey", "api_key", "gateway", "token\"", "password", "contact", "memory\"", "imei", "android_id")) {
            assertFalse("export contains $banned", all.contains(banned))
        }
        assertFalse(d.getJSONObject("privacy").getBoolean("credentialsInExport"))
    }

    @Test fun builtInPresetsAndCycleDoNotIncludeVortex() {
        for (p in PantheonPreset.entries) assertEquals(BrandSkin.PANTHEON, p.config().brandSkin)
        assertNull(PantheonPreset.entries.firstOrNull { it.name.contains("VORTEX") })
    }
}
