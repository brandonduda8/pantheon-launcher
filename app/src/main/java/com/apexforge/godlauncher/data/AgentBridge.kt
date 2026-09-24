package com.apexforge.godlauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apexforge.godlauncher.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Agent Bridge — how external agent frameworks talk to the launcher
 * without any credentials: OpenHands, OpenManus, OpenClaw, ZeroClaw, or
 * on-device automation like Tasker/Automate.
 *
 * Protocol (explicit broadcast intents, same device only):
 *   send  -> action [ACTION_COMMAND], extras:
 *              [EXTRA_COMMAND]      one of CMD_* below
 *              [EXTRA_REQUEST_ID]   caller-chosen id, echoed back
 *              [EXTRA_PARAMS]       JSON object string
 *              [EXTRA_RESULT_ACTION] action the launcher replies on
 *                                    (default [ACTION_RESULT])
 *   reply -> action EXTRA_RESULT_ACTION (or ACTION_RESULT), extras:
 *              [EXTRA_REQUEST_ID], [EXTRA_OK] (boolean),
 *              [EXTRA_DATA] (JSON object string)
 *
 * Self-contained commands (ping, snapshot.get, forge.submit,
 * phoenix.proposals, phoenix.proposal.resolve) are answered by the
 * manifest-registered receiver immediately, even if the launcher UI isn't
 * up. UI-bound commands (phoenix.ask, god.launch) are buffered in
 * [commands] (extraBufferCapacity 64, so they survive until the home
 * activity collects them) and the activity replies once it acts.
 *
 * Adapters for the four frameworks live in AGENT_BRIDGE.md — confirmed
 * protocol surfaces only, no fake "live" claims.
 */
object AgentBridge {
    const val ACTION_COMMAND = "com.apexforge.godlauncher.agent.COMMAND"
    const val ACTION_RESULT = "com.apexforge.godlauncher.agent.RESULT"
    const val EXTRA_COMMAND = "command"
    const val EXTRA_REQUEST_ID = "request_id"
    const val EXTRA_PARAMS = "params"
    const val EXTRA_RESULT_ACTION = "result_action"
    const val EXTRA_OK = "ok"
    const val EXTRA_DATA = "data"

    const val CMD_PING = "ping"
    const val CMD_SNAPSHOT_GET = "snapshot.get"
    const val CMD_PHOENIX_ASK = "phoenix.ask"
    const val CMD_FORGE_SUBMIT = "forge.submit"
    const val CMD_GOD_LAUNCH = "god.launch"
    const val CMD_PROPOSALS = "phoenix.proposals"
    const val CMD_PROPOSAL_RESOLVE = "phoenix.proposal.resolve"

    data class AgentCommand(
        val command: String,
        val requestId: String,
        val params: JSONObject,
        val resultAction: String
    )

    data class ForgeDraft(val type: String, val title: String, val detail: String)

    /** UI-bound commands, buffered until the home activity collects them. */
    private val _commands = MutableSharedFlow<AgentCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<AgentCommand> = _commands.asSharedFlow()

    /** Forge drafts stashed by agents; the Forge screen pre-fills them. */
    private val _forgeDrafts = MutableSharedFlow<ForgeDraft>(extraBufferCapacity = 8)
    val forgeDrafts: SharedFlow<ForgeDraft> = _forgeDrafts.asSharedFlow()

    internal fun emitCommand(cmd: AgentCommand) {
        _commands.tryEmit(cmd)
    }

    internal fun stashForgeDraft(draft: ForgeDraft) {
        _forgeDrafts.tryEmit(draft)
    }

    /** Build a command intent for agent-side senders (documented in AGENT_BRIDGE.md). */
    fun buildCommand(
        command: String,
        requestId: String,
        params: JSONObject = JSONObject(),
        resultAction: String = ACTION_RESULT
    ): Intent = Intent(ACTION_COMMAND).apply {
        setPackage(BuildConfig.APPLICATION_ID)
        putExtra(EXTRA_COMMAND, command)
        putExtra(EXTRA_REQUEST_ID, requestId)
        putExtra(EXTRA_PARAMS, params.toString())
        putExtra(EXTRA_RESULT_ACTION, resultAction)
    }

    /** Reply to a command on its result action. */
    fun sendResult(
        context: Context,
        cmd: AgentCommand,
        ok: Boolean,
        data: JSONObject = JSONObject()
    ) = sendRawResult(context, cmd.resultAction, cmd.requestId, ok, data)

    internal fun sendRawResult(
        context: Context,
        resultAction: String,
        requestId: String,
        ok: Boolean,
        data: JSONObject
    ) {
        context.sendBroadcast(
            Intent(resultAction).apply {
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_OK, ok)
                putExtra(EXTRA_DATA, data.toString())
            }
        )
    }
}

/**
 * Manifest-registered receiver (see AndroidManifest.xml). Answers
 * self-contained commands immediately; buffers UI-bound ones for the
 * home activity. Same-process shared flows, so a second receiver
 * instance is never registered dynamically — no duplicate delivery.
 */
class AgentBridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AgentBridge.ACTION_COMMAND) return
        val command = intent.getStringExtra(AgentBridge.EXTRA_COMMAND) ?: return
        val requestId = intent.getStringExtra(AgentBridge.EXTRA_REQUEST_ID) ?: return
        val resultAction =
            intent.getStringExtra(AgentBridge.EXTRA_RESULT_ACTION) ?: AgentBridge.ACTION_RESULT
        val params = try {
            JSONObject(intent.getStringExtra(AgentBridge.EXTRA_PARAMS) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }

        when (command) {
            AgentBridge.CMD_PING -> {
                AgentBridge.sendRawResult(
                    context, resultAction, requestId, true,
                    JSONObject()
                        .put("pong", true)
                        .put("package", context.packageName)
                        .put("version", BuildConfig.VERSION_NAME)
                )
            }
            AgentBridge.CMD_SNAPSHOT_GET -> {
                // Asset parse off the main thread; the reply is a broadcast.
                Thread {
                    val data = try {
                        val s = SnapshotStore.load(context)
                        if (s == null) {
                            JSONObject().put("error", "snapshot missing")
                        } else {
                            JSONObject()
                                .put("snapshot_at", s.snapshotAt)
                                .put("stored_snapshot", true)
                                .put("gpu_remaining_h", s.gpuRemainingH)
                                .put("gpu_allowed_h", s.gpuAllowedH)
                                .put("gpu_refresh_at", s.gpuRefreshAt)
                                .put("jobs_applied", s.jobsApplied)
                                .put("jobs_goal", s.jobsGoal)
                                .put("money_net", s.moneyNet)
                                .put("gods", JSONArray(s.gods.map {
                                    JSONObject()
                                        .put("name", it.name)
                                        .put("role", it.role)
                                        .put("brain", it.brain)
                                }))
                                .put("fabric_queue", JSONArray(s.fabricQueue.map {
                                    JSONObject()
                                        .put("id", it.id)
                                        .put("title", it.title)
                                        .put("status", it.status)
                                }))
                                .put("ports_listening", s.portsListening)
                                .put("ports_total", s.portsTotal)
                        }
                    } catch (e: Exception) {
                        JSONObject().put("error", e.message ?: "parse failed")
                    }
                    AgentBridge.sendRawResult(
                        context, resultAction, requestId,
                        !data.has("error"), data
                    )
                }.start()
            }
            AgentBridge.CMD_FORGE_SUBMIT -> {
                val draft = AgentBridge.ForgeDraft(
                    type = params.optString("type", "launcher-mod"),
                    title = params.optString("title"),
                    detail = params.optString("detail")
                )
                if (draft.title.isBlank()) {
                    AgentBridge.sendRawResult(
                        context, resultAction, requestId, false,
                        JSONObject().put("error", "title required")
                    )
                } else {
                    AgentBridge.stashForgeDraft(draft)
                    AgentBridge.sendRawResult(
                        context, resultAction, requestId, true,
                        JSONObject()
                            .put("stashed", true)
                            .put("title", draft.title)
                    )
                }
            }
            AgentBridge.CMD_PHOENIX_ASK, AgentBridge.CMD_GOD_LAUNCH -> {
                // UI-bound: buffered; the home activity acts and replies.
                AgentBridge.emitCommand(
                    AgentBridge.AgentCommand(command, requestId, params, resultAction)
                )
            }
            AgentBridge.CMD_PROPOSALS -> {
                // Phoenix's tap queue — machine-side agents poll this,
                // execute what they can, then resolve.
                val data = JSONObject().put(
                    "proposals",
                    JSONArray(
                        PhoenixProposalStore.pending(context).map { p ->
                            JSONObject()
                                .put("id", p.id)
                                .put("kind", p.kind)
                                .put("title", p.title)
                                .put("body", p.body)
                                .put("createdAt", p.createdAt)
                                .put("status", p.status)
                        }
                    )
                )
                AgentBridge.sendRawResult(context, resultAction, requestId, true, data)
            }
            AgentBridge.CMD_PROPOSAL_RESOLVE -> {
                val id = params.optString("id")
                val status = params.optString("status", "resolved")
                val ok = id.isNotBlank() &&
                    PhoenixProposalStore.setStatus(context, id, status)
                AgentBridge.sendRawResult(
                    context, resultAction, requestId, ok,
                    if (ok) JSONObject().put("id", id).put("status", status)
                    else JSONObject().put("error", "unknown proposal id: $id")
                )
            }
            else -> {
                AgentBridge.sendRawResult(
                    context, resultAction, requestId, false,
                    JSONObject().put("error", "unknown command: $command")
                )
            }
        }
    }
}
