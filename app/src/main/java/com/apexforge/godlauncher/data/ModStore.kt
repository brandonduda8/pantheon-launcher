package com.apexforge.godlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.model.PantheonPreset
import com.apexforge.godlauncher.model.Profile
import com.apexforge.godlauncher.model.config
import com.apexforge.godlauncher.model.defaultConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Same DataStore file as PantheonStore (name-keyed singleton) — the mod
// engine lives in new keys, no migration needed.
private val Context.modDataStore by preferencesDataStore(name = "pantheon_prefs")

/**
 * Mod engine persistence: the whole PantheonConfig as one JSON string in
 * the existing DataStore, exposed as Flows, mutated with setters.
 * Per-profile configs are stored under their own keys; switching profiles
 * stashes the live config under the old profile and loads the new one
 * (or the profile's factory default on first use).
 */
class ModStore(private val context: Context) {

    companion object {
        val CONFIG_JSON = stringPreferencesKey("pantheon_config_json_v3")
        val ACTIVE_PROFILE = stringPreferencesKey("mod_active_profile")
        val LAST_PRESET = stringPreferencesKey("mod_last_preset")

        private fun profileKey(profile: Profile) =
            stringPreferencesKey("mod_profile_${profile.name.lowercase()}_json")
    }

    val config: Flow<PantheonConfig> =
        context.modDataStore.data.map { prefs ->
            prefs[CONFIG_JSON]?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                ?: Profile.DEFAULT.defaultConfig()
        }

    val activeProfile: Flow<Profile> =
        context.modDataStore.data.map { prefs ->
            runCatching { Profile.valueOf(prefs[ACTIVE_PROFILE] ?: "") }.getOrElse {
                Profile.DEFAULT
            }
        }

    suspend fun update(transform: (PantheonConfig) -> PantheonConfig) {
        context.modDataStore.edit { prefs ->
            val current = prefs[CONFIG_JSON]
                ?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                ?: Profile.DEFAULT.defaultConfig()
            prefs[CONFIG_JSON] = transform(current).toJson().toString()
        }
    }

    suspend fun setShowroomEnabled(enabled: Boolean) =
        update { it.copy(showroomEnabled = enabled) }

    suspend fun setQuantumViewEnabled(enabled: Boolean) =
        update { it.copy(quantumViewEnabled = enabled) }

    /** Applies a built-in preset as the live config (records it for cycling). */
    suspend fun applyPreset(preset: PantheonPreset) {
        context.modDataStore.edit { prefs ->
            prefs[CONFIG_JSON] = preset.config().copy(
                showroomEnabled = prefs[CONFIG_JSON]
                    ?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                    ?.showroomEnabled ?: false
            ).toJson().toString()
            prefs[LAST_PRESET] = preset.name
        }
    }

    /** NEXT_THEME gesture: rotate through the three built-in presets. */
    suspend fun cyclePreset() {
        val order = PantheonPreset.entries
        context.modDataStore.edit { prefs ->
            val last = prefs[LAST_PRESET]?.let { runCatching { PantheonPreset.valueOf(it) }.getOrNull() }
            val next = order[(order.indexOf(last).let { if (it < 0) 0 else it } + 1) % order.size]
            prefs[CONFIG_JSON] = next.config().copy(
                showroomEnabled = prefs[CONFIG_JSON]
                    ?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                    ?.showroomEnabled ?: false
            ).toJson().toString()
            prefs[LAST_PRESET] = next.name
        }
    }

    /** Switch profile: stash live config under the old profile, load the new. */
    suspend fun setActiveProfile(profile: Profile) {
        context.modDataStore.edit { prefs ->
            val oldName = prefs[ACTIVE_PROFILE] ?: Profile.DEFAULT.name
            prefs[CONFIG_JSON]?.let { prefs[profileKey(profileOf(oldName))] = it }
            val stored = prefs[profileKey(profile)]
            val next = stored?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                ?: profile.defaultConfig()
            prefs[CONFIG_JSON] = next.copy(activeProfile = profile).toJson().toString()
            prefs[ACTIVE_PROFILE] = profile.name
        }
    }

    private fun profileOf(name: String): Profile =
        runCatching { Profile.valueOf(name) }.getOrElse { Profile.DEFAULT }

    fun exportJson(config: PantheonConfig): String =
        JSONObject()
            .put("pantheonThemeVersion", PantheonConfig.VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("config", config.toJson())
            .toString(2)

    /** Returns true when the JSON parsed and applied; false leaves config untouched. */
    suspend fun importJson(raw: String): Boolean {
        val parsed = runCatching {
            val root = JSONObject(raw)
            val body = if (root.has("config")) root.getJSONObject("config") else root
            PantheonConfig.fromJson(body)
        }.getOrNull() ?: return false
        context.modDataStore.edit { prefs ->
            prefs[CONFIG_JSON] = parsed.toJson().toString()
        }
        return true
    }
}
