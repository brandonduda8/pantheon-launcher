package com.apexforge.godlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

private val Context.launchDataStore by preferencesDataStore(name = "launch_history")

/**
 * On-device launch history for the Smart Suggestions row ("Rising now").
 *
 * Per package we keep (count, lastUsedEpochSeconds, per-hour-bucket counts)
 * as a single compact string: "count|lastUsed|b0,b1,...,b23".
 *
 * Nothing leaves the phone: no server, no tracking, no permissions.
 */
class LaunchHistory(private val context: Context) {

    companion object {
        private const val HOURS = 24
        private const val PREFIX = "launch_"
        private fun keyFor(packageName: String) = stringPreferencesKey("$PREFIX$packageName")

        private data class Stats(
            val count: Long,
            val lastUsed: Long,
            val buckets: IntArray
        )

        private fun serialize(s: Stats): String =
            "${s.count}|${s.lastUsed}|" + s.buckets.joinToString(",")

        private fun parse(raw: String?): Stats? {
            if (raw == null) return null
            val parts = raw.split('|')
            if (parts.size != 3) return null
            val count = parts[0].toLongOrNull() ?: return null
            val lastUsed = parts[1].toLongOrNull() ?: return null
            val buckets = parts[2].split(',').map { it.toIntOrNull() ?: 0 }
            if (buckets.size != HOURS) return null
            return Stats(count, lastUsed, buckets.toIntArray())
        }
    }

    /** Record one successful launch. Called from MainActivity's launchApp path. */
    suspend fun recordLaunch(packageName: String) = withContext(Dispatchers.IO) {
        context.launchDataStore.edit { prefs ->
            val key = keyFor(packageName)
            val nowSec = System.currentTimeMillis() / 1000
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val prev = parse(prefs[key])
            val next = if (prev == null) {
                Stats(1, nowSec, IntArray(HOURS).also { it[hour] = 1 })
            } else {
                prev.buckets[hour] = prev.buckets[hour] + 1
                prev.copy(count = prev.count + 1, lastUsed = nowSec)
            }
            prefs[key] = serialize(next)
        }
    }

    /**
     * Top-N package names by launch score, highest first.
     * Score = totalCount * recencyDecay + (launches in the current hour
     * bucket) * bucketBonus. Scoring runs on Dispatchers.Default so the
     * Moto G's UI thread never touches it.
     */
    fun topPackages(limit: Int = 5): Flow<List<String>> =
        context.launchDataStore.data
            .map { prefs -> scoreAll(prefs, limit) }
            .flowOn(Dispatchers.Default)

    private fun scoreAll(prefs: Preferences, limit: Int): List<String> {
        val nowSec = System.currentTimeMillis() / 1000
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return prefs.asMap().mapNotNull { (key, value) ->
            if (!key.name.startsWith(PREFIX) || value !is String) return@mapNotNull null
            val stats = parse(value) ?: return@mapNotNull null
            val hoursSince = ((nowSec - stats.lastUsed).coerceAtLeast(0) / 3600.0)
            // Recency decays over ~6 hours; the current hour bucket gets a bonus
            // so "the apps you open at 8am" surface at 8am.
            val recency = 1.0 / (1.0 + hoursSince / 6.0)
            val score = stats.count * recency + stats.buckets[hour] * 2.0
            key.name.removePrefix(PREFIX) to score
        }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
