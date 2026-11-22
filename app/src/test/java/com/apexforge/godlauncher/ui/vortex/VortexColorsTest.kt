package com.apexforge.godlauncher.ui.vortex

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class VortexColorsTest {
    @Test
    fun hexTokensDecodeToOpaqueColors() {
        assertEquals(Color(0xFFF5F3FF), VortexColors.TextPrimary)
        assertEquals(Color(0xFF06070B), VortexColors.VoidBlack)
        assertEquals(Color(0xFF7A2BFF), VortexColors.ElectricViolet)
    }

    @Test
    fun schemeForegroundIsLightOnDark() {
        assertEquals(VortexColors.TextPrimary, VortexColorScheme.onBackground)
        assertEquals(VortexColors.TextPrimary, VortexColorScheme.onSurface)
        assertEquals(VortexColors.VoidBlack, VortexColorScheme.background)
    }
}
