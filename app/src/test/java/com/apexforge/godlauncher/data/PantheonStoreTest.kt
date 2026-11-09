package com.apexforge.godlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.apexforge.godlauncher.model.PANTHEON
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** PantheonStore over the single shared pantheon_prefs DataStore (Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PantheonStoreTest {

    private lateinit var context: Context
    private lateinit var store: PantheonStore

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        // The DataStore delegate is a process-wide singleton; start every test empty.
        context.pantheonDataStore.edit { it.clear() }
        store = PantheonStore(context)
    }

    @Test
    fun defaultsWhenNothingStored() = runBlocking {
        assertTrue(store.animationsEnabled.first())
        assertTrue(store.companionVisible.first())
        assertEquals("", store.phoenixApiKey.first())
        assertEquals("", store.forgeGatewayUrl.first())
        assertEquals("", store.forgeApiKey.first())
        val packages = store.godPackages.first()
        assertEquals(PANTHEON.map { it.id }.toSet(), packages.keys)
        assertTrue(packages.values.all { it == null })
        assertTrue(store.godLabels.first().values.all { it == null })
    }

    @Test
    fun togglesRoundTrip() = runBlocking {
        store.setAnimationsEnabled(false)
        store.setCompanionVisible(false)
        assertFalse(store.animationsEnabled.first())
        assertFalse(store.companionVisible.first())
        store.setAnimationsEnabled(true)
        assertTrue(store.animationsEnabled.first())
    }

    @Test
    fun stringSettingsRoundTrip() = runBlocking {
        store.setPhoenixApiKey("gemini-key")
        store.setForgeGatewayUrl("https://forge.example")
        store.setForgeApiKey("px-123")
        assertEquals("gemini-key", store.phoenixApiKey.first())
        assertEquals("https://forge.example", store.forgeGatewayUrl.first())
        assertEquals("px-123", store.forgeApiKey.first())
    }

    @Test
    fun godAssignmentSetsPackageAndLabelForThatGodOnly() = runBlocking {
        val zeus = PANTHEON.first { it.id == "zeus" }
        store.setGodApp(zeus, "com.android.chrome", "Chrome")

        val packages = store.godPackages.first()
        val labels = store.godLabels.first()
        assertEquals("com.android.chrome", packages["zeus"])
        assertEquals("Chrome", labels["zeus"])
        PANTHEON.filter { it.id != "zeus" }.forEach {
            assertNull(packages[it.id])
            assertNull(labels[it.id])
        }
    }

    @Test
    fun reassigningAndClearingAGod() = runBlocking {
        val athena = PANTHEON.first { it.id == "athena" }
        val hermes = PANTHEON.first { it.id == "hermes" }
        store.setGodApp(athena, "a.one", "One")
        store.setGodApp(athena, "a.two", "Two")
        store.setGodApp(hermes, "h.msg", "Messages")
        assertEquals("a.two", store.godPackages.first()["athena"])
        assertEquals("Two", store.godLabels.first()["athena"])

        store.clearGodApp(athena)
        assertNull(store.godPackages.first()["athena"])
        assertNull(store.godLabels.first()["athena"])
        // Clearing one god leaves the others alone.
        assertEquals("h.msg", store.godPackages.first()["hermes"])
    }

    @Test
    fun keysAreStablePerGod() {
        val zeus = PANTHEON.first { it.id == "zeus" }
        assertEquals("god_app_zeus", PantheonStore.godAppKey(zeus).name)
        assertEquals("god_label_zeus", PantheonStore.godLabelKey(zeus).name)
    }
}
