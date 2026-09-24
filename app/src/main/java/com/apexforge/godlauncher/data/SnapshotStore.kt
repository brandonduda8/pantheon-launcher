package com.apexforge.godlauncher.data

import android.content.Context
import org.json.JSONObject

/**
 * Reads the bundled Pantheon system snapshot (assets/data-snapshot.json).
 *
 * HONESTY CONTRACT: this file is a stored reading probed from Zane's
 * machine — the phone cannot reach those services live. Every surface that
 * shows this data must label it "snapshot" with the probe timestamp, and
 * must never present it as live state.
 */
data class GodBrainStatus(val name: String, val role: String, val brain: String)

data class FabricJob(val id: String, val title: String, val status: String)

data class GoalStatus(val name: String, val status: String)

data class SystemSnapshot(
    val snapshotAt: String,
    val honesty: String,
    val gpuRemainingH: Double,
    val gpuAllowedH: Double,
    val gpuRefreshAt: String,
    val gpuNote: String,
    val jobsApplied: Int,
    val jobsGoal: Int,
    val moneyNet: String,
    val moneyEntries: Int,
    val gods: List<GodBrainStatus>,
    val mcpServers: List<String>,
    val goals: List<GoalStatus>,
    val fabricQueue: List<FabricJob>,
    val portsListening: Int,
    val portsTotal: Int
) {
    companion object {
        fun empty() = SystemSnapshot(
            snapshotAt = "unknown",
            honesty = "Snapshot failed to load.",
            gpuRemainingH = 0.0, gpuAllowedH = 0.0, gpuRefreshAt = "",
            gpuNote = "", jobsApplied = 0, jobsGoal = 0,
            moneyNet = "", moneyEntries = 0,
            gods = emptyList(), mcpServers = emptyList(),
            goals = emptyList(), fabricQueue = emptyList(),
            portsListening = 0, portsTotal = 0
        )
    }
}

object SnapshotStore {

    /** Call off the main thread. Never throws — returns [SystemSnapshot.empty] on failure. */
    fun load(context: Context): SystemSnapshot {
        return try {
            val text = context.assets.open("data-snapshot.json")
                .bufferedReader().use { it.readText() }
            parse(JSONObject(text))
        } catch (_: Exception) {
            SystemSnapshot.empty()
        }
    }

    private fun parse(root: JSONObject): SystemSnapshot {
        val quota = root.optJSONObject("kaggle_quota")
        val jobs = root.optJSONObject("jobs")?.optJSONObject("sprint")
        val money = root.optJSONObject("money")
        val godsJson = root.optJSONArray("gods")
        val gods = mutableListOf<GodBrainStatus>()
        if (godsJson != null) {
            for (i in 0 until godsJson.length()) {
                val g = godsJson.optJSONObject(i) ?: continue
                gods.add(
                    GodBrainStatus(
                        name = g.optString("name", "?"),
                        role = g.optString("role", ""),
                        brain = g.optString("brain", "")
                    )
                )
            }
        }
        val mcp = root.optJSONObject("mcp")?.optJSONArray("servers")
        val mcpServers = mutableListOf<String>()
        if (mcp != null) {
            for (i in 0 until mcp.length()) mcpServers.add(mcp.optString(i))
        }
        val goalsJson = root.optJSONArray("goals")
        val goals = mutableListOf<GoalStatus>()
        if (goalsJson != null) {
            for (i in 0 until goalsJson.length()) {
                val g = goalsJson.optJSONObject(i) ?: continue
                goals.add(GoalStatus(g.optString("name", "?"), g.optString("status", "")))
            }
        }
        val queueJson = root.optJSONObject("fabric")?.optJSONArray("build_queue")
        val queue = mutableListOf<FabricJob>()
        if (queueJson != null) {
            for (i in 0 until queueJson.length()) {
                val q = queueJson.optJSONObject(i) ?: continue
                queue.add(
                    FabricJob(
                        q.optString("id", "?"),
                        q.optString("title", ""),
                        q.optString("status", "")
                    )
                )
            }
        }
        val ports = root.optJSONObject("ports")
        var listening = 0
        var total = 0
        if (ports != null) {
            val keys = ports.keys()
            while (keys.hasNext()) {
                val p = ports.optJSONObject(keys.next())
                total++
                if (p != null && p.optBoolean("listening", false)) listening++
            }
        }
        return SystemSnapshot(
            snapshotAt = root.optString("snapshot_at", "unknown"),
            honesty = root.optString(
                "_honesty",
                "Stored snapshot — not live."
            ),
            gpuRemainingH = quota?.optDouble("gpu_remaining_h", 0.0) ?: 0.0,
            gpuAllowedH = quota?.optDouble("gpu_allowed_h", 0.0) ?: 0.0,
            gpuRefreshAt = quota?.optString("refresh_at", "") ?: "",
            gpuNote = quota?.optString("note", "") ?: "",
            jobsApplied = jobs?.optInt("applied", 0) ?: 0,
            jobsGoal = jobs?.optInt("goal", 0) ?: 0,
            moneyNet = money?.optString("net_total", "") ?: "",
            moneyEntries = money?.optInt("entries", 0) ?: 0,
            gods = gods,
            mcpServers = mcpServers,
            goals = goals,
            fabricQueue = queue,
            portsListening = listening,
            portsTotal = total
        )
    }
}
