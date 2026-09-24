package com.apexforge.godlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apexforge.godlauncher.model.God
import com.apexforge.godlauncher.model.PANTHEON
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pantheonDataStore by preferencesDataStore(name = "pantheon_prefs")

/**
 * All persisted launcher state: god-dock app assignments and the
 * animations toggle. Single DataStore, read as Flows, written from
 * coroutines — survives process death and reboots.
 */
class PantheonStore(private val context: Context) {

    companion object {
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val PHOENIX_API_KEY = stringPreferencesKey("phoenix_api_key")
        fun godAppKey(god: God) = stringPreferencesKey("god_app_${god.id}")
        fun godLabelKey(god: God) = stringPreferencesKey("god_label_${god.id}")

        /** Forge: Brandon's Pantheon gateway (Zane's machine). User-configured, never hardcoded. */
        val FORGE_GATEWAY_URL = stringPreferencesKey("forge_gateway_url")
        val FORGE_API_KEY = stringPreferencesKey("forge_api_key")

        /** Phoenix companion visibility on the home screen. */
        val COMPANION_VISIBLE = booleanPreferencesKey("companion_visible")
    }

    val animationsEnabled: Flow<Boolean> =
        context.pantheonDataStore.data.map { it[ANIMATIONS_ENABLED] ?: true }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.pantheonDataStore.edit { it[ANIMATIONS_ENABLED] = enabled }
    }

    /** Brandon's own Gemini API key for Phoenix's voice. Stored only on this phone. */
    val phoenixApiKey: Flow<String> =
        context.pantheonDataStore.data.map { it[PHOENIX_API_KEY] ?: "" }

    suspend fun setPhoenixApiKey(key: String) {
        context.pantheonDataStore.edit { it[PHOENIX_API_KEY] = key }
    }

    /** godId -> assigned package name (null = unassigned). */
    val godPackages: Flow<Map<String, String?>> =
        context.pantheonDataStore.data.map { prefs ->
            PANTHEON.associate { it.id to prefs[godAppKey(it)] }
        }

    /** godId -> human-readable label of the assigned app. */
    val godLabels: Flow<Map<String, String?>> =
        context.pantheonDataStore.data.map { prefs ->
            PANTHEON.associate { it.id to prefs[godLabelKey(it)] }
        }

    suspend fun setGodApp(god: God, packageName: String, label: String) {
        context.pantheonDataStore.edit { prefs ->
            prefs[godAppKey(god)] = packageName
            prefs[godLabelKey(god)] = label
        }
    }

    suspend fun clearGodApp(god: God) {
        context.pantheonDataStore.edit { prefs ->
            prefs.remove(godAppKey(god))
            prefs.remove(godLabelKey(god))
        }
    }

    /** Base URL of the Pantheon gateway on Zane's machine, e.g. https://.... */
    val forgeGatewayUrl: Flow<String> =
        context.pantheonDataStore.data.map { it[FORGE_GATEWAY_URL] ?: "" }

    /** Bearer key for the gateway (px-...). Stored only on this phone. */
    val forgeApiKey: Flow<String> =
        context.pantheonDataStore.data.map { it[FORGE_API_KEY] ?: "" }

    suspend fun setForgeGatewayUrl(url: String) {
        context.pantheonDataStore.edit { it[FORGE_GATEWAY_URL] = url }
    }

    suspend fun setForgeApiKey(key: String) {
        context.pantheonDataStore.edit { it[FORGE_API_KEY] = key }
    }

    /** Phoenix companion on/off (default on). */
    val companionVisible: Flow<Boolean> =
        context.pantheonDataStore.data.map { it[COMPANION_VISIBLE] ?: true }

    suspend fun setCompanionVisible(visible: Boolean) {
        context.pantheonDataStore.edit { it[COMPANION_VISIBLE] = visible }
    }
}
