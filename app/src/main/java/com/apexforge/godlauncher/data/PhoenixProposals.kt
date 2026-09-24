package com.apexforge.godlauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phoenix's tap queue — the "Phoenix proposes, Brandon taps" loop.
 *
 * The phone can't do GPU training, web research, or job applications
 * on-device, and it can't reach Zane's machine over the network. So
 * Phoenix works in two honest lanes:
 *
 *  1. ON-DEVICE: briefs, nudges, and follow-ups generated locally from
 *     the stored snapshot + the device clock. Shown in the Phoenix chat
 *     screen, the floating companion, and the service notification.
 *  2. TAP QUEUE: anything needing the machine (research, builds, agent
 *     runs) becomes a proposal. Brandon taps "Copy for Zane" and pastes
 *     it into chat — one tap, the machine executes. Machine-side agents
 *     can also pull pending proposals over the Agent Bridge
 *     (phoenix.proposals / phoenix.proposal.resolve).
 *
 * Phoenix NEVER moves money on its own. Every money move is Brandon's tap.
 * Every number here is snapshot-derived and labeled as such.
 */
data class PhoenixProposal(
    val id: String,
    val kind: String, // brief | nudge | followup | money | genesis | tap
    val title: String,
    val body: String,
    val createdAt: Long,
    val status: String // pending | approved | resolved | dismissed
)

object PhoenixProposalStore {
    private const val FILE = "phoenix_proposals.json"
    private const val MAX_KEPT = 60
    private val lock = Any()

    private fun file(context: Context) =
        java.io.File(context.filesDir, FILE)

    fun list(context: Context): List<PhoenixProposal> = synchronized(lock) {
        try {
            val f = file(context)
            if (!f.exists()) return emptyList()
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                PhoenixProposal(
                    id = o.optString("id"),
                    kind = o.optString("kind", "nudge"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    createdAt = o.optLong("createdAt", 0L),
                    status = o.optString("status", "pending")
                )
            }.filter { it.id.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun pending(context: Context): List<PhoenixProposal> =
        list(context).filter { it.status == "pending" }
            .sortedByDescending { it.createdAt }

    private fun writeLocked(context: Context, proposals: List<PhoenixProposal>) {
        try {
            val pruned = proposals
                .sortedByDescending { it.createdAt }
                .take(MAX_KEPT)
            val arr = JSONArray()
            for (p in pruned) {
                arr.put(
                    JSONObject()
                        .put("id", p.id)
                        .put("kind", p.kind)
                        .put("title", p.title)
                        .put("body", p.body)
                        .put("createdAt", p.createdAt)
                        .put("status", p.status)
                )
            }
            file(context).writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    /** Adds proposals, skipping ids already present (daily briefs dedupe naturally). */
    fun addAll(context: Context, proposals: List<PhoenixProposal>) = synchronized(lock) {
        if (proposals.isEmpty()) return
        val cur = list(context).toMutableList()
        val ids = cur.map { it.id }.toSet()
        for (p in proposals) {
            if (p.id !in ids) cur.add(p)
        }
        writeLocked(context, cur)
    }

    fun setStatus(context: Context, id: String, status: String): Boolean = synchronized(lock) {
        val cur = list(context)
        var changed = false
        val next = cur.map {
            if (it.id == id && it.status != status) {
                changed = true
                it.copy(status = status)
            } else it
        }
        if (changed) writeLocked(context, next)
        changed
    }
}

/**
 * Builds today's brief + nudges from the stored snapshot and device clock.
 * Template-driven, on-device, $0. Everything snapshot-derived is labeled.
 */
object PhoenixBriefEngine {

    fun refresh(context: Context, snapshot: SystemSnapshot?) {
        val now = System.currentTimeMillis()
        PhoenixProposalStore.addAll(context, buildBrief(snapshot, now))
    }

    fun buildBrief(snapshot: SystemSnapshot?, now: Long): List<PhoenixProposal> {
        val out = mutableListOf<PhoenixProposal>()
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val at = snapshot?.snapshotAt?.replace("T", " ")?.removeSuffix("Z") ?: "unknown"

        val head = if (snapshot == null) {
            "Snapshot hasn't loaded yet — check back in a moment."
        } else {
            "STORED SNAPSHOT (probed $at UTC) — " +
                "GPU ${trim2(snapshot.gpuRemainingH)}/${trim2(snapshot.gpuAllowedH)}h left · " +
                "Jobs ${snapshot.jobsApplied}/${snapshot.jobsGoal} · " +
                "Ledger ${snapshot.moneyNet.substringBefore(" (").ifBlank { "$0.00" }} · " +
                "${snapshot.fabricQueue.count { it.status.equals("ERROR", true) }} fabric jobs errored."
        }

        out.add(
            PhoenixProposal(
                id = "brief-$day",
                kind = "brief",
                title = "Phoenix brief — $day",
                body = "$head\n\nWorth your taps today:\n" +
                    briefPriorities(snapshot).joinToString("\n") { "• $it" },
                createdAt = now,
                status = "pending"
            )
        )

        if (snapshot != null) {
            // Jobs nudge.
            if (snapshot.jobsApplied < snapshot.jobsGoal) {
                val left = snapshot.jobsGoal - snapshot.jobsApplied
                out.add(
                    PhoenixProposal(
                        id = "jobs-$day",
                        kind = "followup",
                        title = "$left applications to go (${snapshot.jobsApplied}/${snapshot.jobsGoal})",
                        body = "Snapshot count: ${snapshot.jobsApplied} of ${snapshot.jobsGoal}. " +
                            "Next tap: tell Zane \"next application\" and answer the screener " +
                            "questions one at a time — the machine fills and submits.",
                        createdAt = now,
                        status = "pending"
                    )
                )
            }
            // GPU nudge.
            if (snapshot.gpuRemainingH < 5.0 && snapshot.gpuRemainingH > 0) {
                out.add(
                    PhoenixProposal(
                        id = "gpu-$day",
                        kind = "nudge",
                        title = "GPU running low — ${trim2(snapshot.gpuRemainingH)}h left",
                        body = "Snapshot shows ${trim2(snapshot.gpuRemainingH)}h of " +
                            "${trim2(snapshot.gpuAllowedH)}h remaining (refresh " +
                            "${snapshot.gpuRefreshAt.replace("T", " ").removeSuffix("Z")} UTC). " +
                            "Spend the rest on the highest money-impact run first — " +
                            "ask Zane what Zeus recommends.",
                        createdAt = now,
                        status = "pending"
                    )
                )
            }
            // Errored fabric jobs.
            val errored = snapshot.fabricQueue.filter {
                it.status.equals("ERROR", true) || it.status.equals("FAILED", true)
            }
            for (job in errored.take(3)) {
                out.add(
                    PhoenixProposal(
                        id = "fabric-${job.id}-$day",
                        kind = "genesis",
                        title = "Fabric job errored: ${job.title.ifBlank { job.id }}",
                        body = "Stored queue shows ${job.id} in ${job.status}. " +
                            "Tap \"Copy for Zane\" and paste it into chat — " +
                            "the machine diagnoses and re-queues.",
                        createdAt = now,
                        status = "pending"
                    )
                )
            }
            // Money nudge — Phoenix never moves money itself.
            out.add(
                PhoenixProposal(
                    id = "money-$day",
                    kind = "money",
                    title = "Money check — ledger ${snapshot.moneyNet.substringBefore(" (").ifBlank { "$0.00" }}",
                    body = "I can't move money on my own — every money move is your tap. " +
                        "Today's highest-leverage move: keep the application sprint " +
                        "going (${snapshot.jobsApplied}/${snapshot.jobsGoal}). A paycheck " +
                        "is the fastest $1k/week path; agent income builds on top.",
                    createdAt = now,
                    status = "pending"
                )
            )
        }
        return out
    }

    private fun briefPriorities(snapshot: SystemSnapshot?): List<String> {
        if (snapshot == null) return listOf("Waiting on the snapshot to load.")
        val out = mutableListOf<String>()
        if (snapshot.jobsApplied < snapshot.jobsGoal) {
            out.add("Applications: ${snapshot.jobsApplied}/${snapshot.jobsGoal} — answer the next screener round.")
        }
        val errored = snapshot.fabricQueue.count {
            it.status.equals("ERROR", true) || it.status.equals("FAILED", true)
        }
        if (errored > 0) out.add("$errored fabric job(s) need a re-queue decision.")
        val running = snapshot.fabricQueue.count {
            it.status.equals("RUNNING", true) || it.status.equals("COMPLETE", true)
        }
        if (running > 0) out.add("$running fabric job(s) in flight or landed — check what needs harvest.")
        out.add("One Genesis improvement: tell Phoenix what felt slow today — it becomes a Forge draft.")
        return out.take(4)
    }

    private fun trim2(h: Double): String =
        "%.2f".format(h).trimEnd('0').trimEnd('.').ifEmpty { "0" }
}
