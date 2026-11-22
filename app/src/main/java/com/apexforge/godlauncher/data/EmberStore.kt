package com.apexforge.godlauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

/**
 * Ember's consent-first, on-device soul. This store does not use the network.
 * All learned facts are written only when the owner taps "Teach Ember".
 * No credentials, device identifiers, or Genesis secrets are exported.
 */
data class EmberState(
    val xp: Int = 0,
    val bond: Int = 0,
    val energy: Int = 75,
    val memories: List<String> = emptyList()
) {
    val level: Int get() = 1 + xp / 100
    val stage: String get() = when {
        level >= 10 -> "Elder Guardian"
        level >= 5 -> "Sky Guardian"
        level >= 3 -> "Young Dragon"
        else -> "Hatchling"
    }
    val mood: String get() = when {
        energy < 25 -> "Sleepy"
        bond >= 60 -> "Devoted"
        energy > 80 -> "Bright"
        else -> "Curious"
    }
}

enum class EmberAction { PET, PRACTICE, REST }

class EmberStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ember_soul_v0", Context.MODE_PRIVATE)

    @Synchronized
    fun read(): EmberState {
        val memories = try {
            val arr = JSONArray(prefs.getString("memories", "[]") ?: "[]")
            (0 until min(arr.length(), 40)).mapNotNull { index ->
                arr.optString(index, "").take(240).takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) { emptyList() }
        return EmberState(
            xp = prefs.getInt("xp", 0).coerceIn(0, 1_000_000),
            bond = prefs.getInt("bond", 0).coerceIn(0, 100),
            energy = prefs.getInt("energy", 75).coerceIn(0, 100),
            memories = memories
        )
    }

    @Synchronized
    fun act(action: EmberAction): EmberState {
        val old = read()
        // UI interactions have a short cooldown so accidental taps do not farm levels.
        val now = System.currentTimeMillis()
        val key = "action_" + action.name
        if (now - prefs.getLong(key, 0L) < 1500L) return old
        val next = when (action) {
            EmberAction.PET -> old.copy(
                xp = (old.xp + 2).coerceAtMost(1_000_000),
                bond = (old.bond + 3).coerceAtMost(100),
                energy = (old.energy + 1).coerceAtMost(100)
            )
            EmberAction.PRACTICE -> if (old.energy < 10) old else old.copy(
                xp = (old.xp + 12).coerceAtMost(1_000_000),
                bond = (old.bond + 1).coerceAtMost(100),
                energy = (old.energy - 10).coerceAtLeast(0)
            )
            EmberAction.REST -> old.copy(energy = (old.energy + 25).coerceAtMost(100))
        }
        write(next)
        prefs.edit().putLong(key, now).apply()
        return next
    }

    @Synchronized
    fun teach(text: String): EmberState {
        val fact = text.trim().replace(Regex("\\s+"), " ").take(240)
        val old = read()
        if (fact.isBlank() || old.memories.any { it.equals(fact, ignoreCase = true) }) return old
        val updated = old.copy(
            xp = (old.xp + 15).coerceAtMost(1_000_000),
            bond = (old.bond + 2).coerceAtMost(100),
            memories = (old.memories + fact).takeLast(40)
        )
        write(updated)
        return updated
    }

    @Synchronized
    fun forget(text: String): EmberState {
        val old = read()
        val updated = old.copy(memories = old.memories.filterNot { it == text })
        write(updated)
        return updated
    }

    @Synchronized
    fun erase(): EmberState {
        prefs.edit().clear().apply()
        return EmberState()
    }

    private fun write(state: EmberState) {
        prefs.edit()
            .putInt("xp", state.xp)
            .putInt("bond", state.bond)
            .putInt("energy", state.energy)
            .putString("memories", JSONArray(state.memories).toString())
            .apply()
    }

    /** An egg to gift, not a copy of the owner's personal memories. */
    fun giftEggJson(): String = JSONObject()
        .put("schema", "genesis.dragon-egg.v0")
        .put("species", "ember-dragon")
        .put("display_name", "Ember")
        .put("skin", "EMBER")
        .put("starting_level", 1)
        .put("privacy", "No personal memories, identity or credentials included")
        .toString(2)
}
