package com.apexforge.godlauncher.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

/**
 * Ember's consent-first, on-device soul. This store does not use the network.
 * Files in noBackupFilesDir are app-private and excluded from Android auto-backup.
 * We do not export credentials, identifying fields or other private memories.
 */
data class EmberState(
    val xp: Int = 0,
    val bond: Int = 0,
    val energy: Int = 75,
    val memories: List<String> = emptyList(),
    val lastInteractionAt: Long = 0L
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
    private val atomic = AtomicFile(File(context.applicationContext.noBackupFilesDir, "ember_soul_v0.json"))

    private fun readJson(): JSONObject = try {
        val raw = atomic.openRead().use { it.readBytes().toString(Charsets.UTF_8) }
        JSONObject(raw)
    } catch (_: Exception) { JSONObject() }

    @Synchronized
    fun read(): EmberState {
        val root = readJson()
        val memories = try {
            val arr = root.optJSONArray("memories") ?: JSONArray()
            (0 until min(arr.length(), 40)).mapNotNull { index ->
                arr.optString(index, "").take(240).takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) { emptyList() }
        return EmberState(
            xp = root.optInt("xp", 0).coerceIn(0, 1_000_000),
            bond = root.optInt("bond", 0).coerceIn(0, 100),
            energy = root.optInt("energy", 75).coerceIn(0, 100),
            memories = memories,
            lastInteractionAt = root.optLong("lastInteractionAt", 0L).coerceAtLeast(0L)
        )
    }

    @Synchronized
    fun act(action: EmberAction): EmberState {
        val old = read()
        val now = System.currentTimeMillis()
        if (now - old.lastInteractionAt < 1200L) return old
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
        if (next != old) write(next.copy(lastInteractionAt = now))
        return if (next == old) old else next.copy(lastInteractionAt = now)
    }

    @Synchronized
    fun teach(text: String): EmberState {
        val fact = text.trim().replace(Regex("\\s+"), " ").take(240)
        val old = read()
        if (fact.isBlank() || old.memories.any { it.equals(fact, ignoreCase = true) }) return old
        val next = old.copy(
            xp = (old.xp + 15).coerceAtMost(1_000_000),
            bond = (old.bond + 2).coerceAtMost(100),
            memories = (old.memories + fact).takeLast(40)
        )
        write(next)
        return next
    }

    @Synchronized
    fun forget(text: String): EmberState {
        val next = read().let { it.copy(memories = it.memories.filterNot { fact -> fact == text }) }
        write(next)
        return next
    }

    @Synchronized
    fun erase(): EmberState {
        atomic.delete()
        return EmberState()
    }

    private fun write(state: EmberState) {
        val json = JSONObject()
            .put("xp", state.xp)
            .put("bond", state.bond)
            .put("energy", state.energy)
            .put("lastInteractionAt", state.lastInteractionAt)
            .put("memories", JSONArray(state.memories))
            .toString()
        val stream = atomic.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            atomic.failWrite(stream)
            throw e
        }
    }

    /** A cosmetic dragon egg, not a copy of the owner's private memories. */
    fun giftEggJson(): String = JSONObject()
        .put("schema", "genesis.dragon-egg.v0")
        .put("species", "ember-dragon")
        .put("display_name", "Ember")
        .put("skin", "EMBER")
        .put("starting_level", 1)
        .put("privacy", "No personal memories, identity or credentials included")
        .toString(2)
}
