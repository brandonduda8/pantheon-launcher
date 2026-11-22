package com.apexforge.godlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EmberStateTest {
    @Test fun beginsAsHatchling() {
        val ember = EmberState()
        assertEquals(1, ember.level)
        assertEquals("Hatchling", ember.stage)
        assertEquals(75, ember.energy)
        assertEquals(0, ember.memories.size)
    }

    @Test fun levelsAreBasedOnEarnedExperience() {
        assertEquals(3, EmberState(xp = 200).level)
        assertEquals("Young Dragon", EmberState(xp = 200).stage)
        assertEquals("Sky Guardian", EmberState(xp = 400).stage)
        assertEquals("Elder Guardian", EmberState(xp = 900).stage)
    }

    @Test fun exhaustionTakesPriorityOverBond() {
        assertEquals("Sleepy", EmberState(energy = 10, bond = 100).mood)
        assertEquals("Devoted", EmberState(energy = 40, bond = 75).mood)
        assertEquals("Bright", EmberState(energy = 95, bond = 0).mood)
    }
}
