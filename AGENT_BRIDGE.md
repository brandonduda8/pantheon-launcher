# Agent Bridge

How external agent frameworks talk to the Pantheon launcher **without any
credentials**, over on-device explicit broadcasts. Same device only —
nothing here crosses the network.

Status: **protocol-ready, not connected.** No framework below is wired to a
live endpoint. Anything claiming otherwise is wrong.

## Protocol

Send (explicit broadcast to this package):

- action: `com.apexforge.godlauncher.agent.COMMAND`
- extras:
  - `command` — one of `ping`, `snapshot.get`, `phoenix.ask`, `forge.submit`, `god.launch`
  - `request_id` — caller-chosen id, echoed back in the reply
  - `params` — JSON object string
  - `result_action` — action the launcher replies on (default `com.apexforge.godlauncher.agent.RESULT`)

Reply (broadcast on `result_action`):

- extras: `request_id`, `ok` (boolean), `data` (JSON object string)

Commands:

| command | params | behavior |
|---|---|---|
| `ping` | — | immediate reply: `{pong, package, version}` |
| `snapshot.get` | — | immediate reply with the bundled **stored snapshot** fields (`stored_snapshot: true`, `snapshot_at`, gpu/jobs/money/gods/fabric/ports). Never live. |
| `phoenix.ask` | `{text}` | opens Phoenix chat and sends `text`; reply `{asked: true}` once acted on. Buffered if the home activity isn't up yet. |
| `forge.submit` | `{type, title, detail}` | stashes a Forge draft; opens the Forge screen with the composer pre-filled; immediate reply `{stashed, title}`. `title` is required. |
| `god.launch` | `{id}` | launches the app assigned to the god (`zeus`, `athena`, `hermes`, `hephaestus`, `tyche`, `argus`, `odysseus`, `themis`); reply `{launched, package}` or an error if unassigned. |
| `phoenix.proposals` | — | immediate reply with Phoenix's pending tap queue (`proposals[]`: id, kind, title, body, createdAt, status). Machine-side agents poll this, execute, then resolve. |
| `phoenix.proposal.resolve` | `{id, status}` | marks a proposal `resolved`/`approved`/`dismissed`; reply `{id, status}` or an error. |

### Firing from a machine with adb access to the phone

```sh
adb shell am broadcast \
  -a com.apexforge.godlauncher.agent.COMMAND \
  -n com.apexforge.godlauncher/.data.AgentBridgeReceiver \
  --es command ping \
  --es request_id req-1 \
  --es params '{}' \
  --es result_action com.apexforge.godlauncher.agent.RESULT
```

Replies are broadcasts too: one-way commands work from adb today; full
duplex needs a listener on the phone (Tasker profile, or a tiny listener
APK). Machine-side agents over plain adb cannot subscribe to the reply
broadcast — plan for that before promising round-trips.

## Framework adapters (confirmed surfaces only)

### OpenHands — https://github.com/OpenHands/OpenHands (MIT)
Software-engineering agent platform. Integration surfaces: REST API,
WebSocket/events, agent server, OpenAI-compatible endpoints. Needs a backend
execution environment (Docker/Codespaces/E2B) — not on-device. Launcher
route: an OpenHands run with adb access to the phone (e.g. the Codespaces
starter in `genesis-os/remote-compute/codespaces-openhands/`) fires the
broadcasts above via `adb shell am broadcast`. Not connected.

### OpenManus — https://github.com/FoundationAgents/OpenManus (MIT)
CLI/MCP-first agent; an experimental A2A HTTP server (port 10000) was
reported upstream. Launcher route: a tool/skill inside OpenManus that shells
to adb. A2A support is experimental — treat as such. Not connected.

### OpenClaw — https://github.com/openclaw/openclaw (MIT)
Local-first agent gateway; its WebSocket gateway is the best launcher
surface. Launcher route: a gateway skill that fires adb broadcasts.
Port/protocol details need confirmation from the canonical repo before
hardcoding. Not connected.

### ZeroClaw — https://github.com/zeroclaw-labs/zeroclaw (MIT/Apache-2.0)
Lightweight Rust agent runtime; CLI plus gateway/daemon surfaces. Launcher
route: a daemon task that fires adb broadcasts on a schedule or event.
Exact REST/WebSocket details need canonical confirmation before hardcoding.
Not connected.

## Honesty rules for agents using this bridge

1. `snapshot.get` returns a **stored snapshot** — the launcher labels it
   that way in every UI surface, and so must you.
2. Phoenix chat on the phone uses Brandon's own Gemini key — never ask for
   it, never log it, never relay it.
3. The Forge gateway URL/key are Brandon's own configuration — agents may
   submit drafts, never read or change the credentials.
4. UI-bound commands buffer until the home activity collects them; do not
   present a buffered command as completed until its reply arrives.
