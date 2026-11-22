package com.apexforge.godlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.apexforge.godlauncher.model.BrandSkin
import com.apexforge.godlauncher.model.MotionMode
import com.apexforge.godlauncher.model.PantheonConfig
import com.apexforge.godlauncher.model.PantheonPreset
import com.apexforge.godlauncher.model.Profile
import com.apexforge.godlauncher.model.VortexPreset
import com.apexforge.godlauncher.model.config
import com.apexforge.godlauncher.model.defaultConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

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
        val VORTEX_UNDO_JSON = stringPreferencesKey("vortex_undo_config_json")

        private fun profileKey(profile: Profile) =
            stringPreferencesKey("mod_profile_${profile.name.lowercase()}_json")
    }

    val config: Flow<PantheonConfig> =
        context.pantheonDataStore.data.map { prefs ->
            prefs[CONFIG_JSON]?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
                ?: Profile.DEFAULT.defaultConfig()
        }

    val activeProfile: Flow<Profile> =
        context.pantheonDataStore.data.map { prefs ->
            runCatching { Profile.valueOf(prefs[ACTIVE_PROFILE] ?: "") }.getOrElse {
                Profile.DEFAULT
            }
        }

    suspend fun update(transform: (PantheonConfig) -> PantheonConfig) {
        context.pantheonDataStore.edit { prefs ->
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
        context.pantheonDataStore.edit { prefs ->
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
        context.pantheonDataStore.edit { prefs ->
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

    /** True when there is a pre-Vortex snapshot to return to. */
    val hasVortexUndo: Flow<Boolean> =
        context.pantheonDataStore.data.map { it[VORTEX_UNDO_JSON] != null }

    private fun currentOf(raw: String?): PantheonConfig =
        raw?.let { runCatching { PantheonConfig.fromJsonString(it) }.getOrNull() }
            ?: Profile.DEFAULT.defaultConfig()

    /** Applies VORTEX // EMBER, snapshotting the previous config for Undo. */
    suspend fun applyVortex() {
        context.pantheonDataStore.edit { prefs ->
            val applied = VortexPreset.apply(currentOf(prefs[CONFIG_JSON]), prefs[VORTEX_UNDO_JSON])
            prefs[CONFIG_JSON] = applied.config.toJson().toString()
            applied.undoJson?.let { prefs[VORTEX_UNDO_JSON] = it }
        }
    }

    /** Returns to the exact pre-Vortex config and clears the snapshot. */
    suspend fun undoVortex() {
        context.pantheonDataStore.edit { prefs ->
            val current = currentOf(prefs[CONFIG_JSON])
            prefs[CONFIG_JSON] = VortexPreset.undo(current, prefs[VORTEX_UNDO_JSON]).toJson().toString()
            prefs.remove(VORTEX_UNDO_JSON)
        }
    }

    /** Shipping Pantheon default; dock order and app assignments are kept. */
    suspend fun restoreDefault() {
        context.pantheonDataStore.edit { prefs ->
            prefs[CONFIG_JSON] = VortexPreset.restoreDefault(currentOf(prefs[CONFIG_JSON])).toJson().toString()
            prefs.remove(VORTEX_UNDO_JSON)
            prefs.remove(LAST_PRESET)
        }
    }

    suspend fun setMotionMode(mode: MotionMode) = update { it.copy(motionMode = mode) }

    /** Switch profile: stash live config under the old profile, load the new. */
    suspend fun setActiveProfile(profile: Profile) {
        context.pantheonDataStore.edit { prefs ->
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
            .apply {
                if (config.brandSkin == BrandSkin.VORTEX_EMBER) put("preset", VortexPreset.descriptor(config))
            }
            .toString(2)

    /** Returns true when the JSON parsed and applied; false leaves config untouched. */
    suspend fun importJson(raw: String): Boolean {
        val parsed = runCatching {
            val root = JSONObject(raw)
            val body = if (root.has("config")) root.getJSONObject("config") else root
            PantheonConfig.fromJson(body)
        }.getOrNull() ?: return false
        context.pantheonDataStore.edit { prefs ->
            prefs[CONFIG_JSON] = parsed.toJson().toString()
        }
        return true
    }
}
