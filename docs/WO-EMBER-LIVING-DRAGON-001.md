# WO-EMBER-LIVING-DRAGON-001 — Genesis Dragon Companion Program

**Owner:** Genesis founder  
**Builder lane:** Zane / Genesis engineers  
**Independent review lane:** ChatGPT AI Code Reviewer for PRs not authored by itself; Zane or another independent reviewer for PRs authored by ChatGPT  
**Initial target:** Motorola Moto G (2025), Android, low-memory / battery-sensitive  
**Reference implementation:** Pantheon Launcher PR #5 (DRAFT, not release-verified)

## North star

Ember must feel like an embodied, growing **character**, not a chat card.
He breathes, blinks, wanders, rests, plays, remembers only what the recipient
chooses to teach, talks out loud, and can eventually help with permissioned
real tasks. Solon is a separate yin–yang dragon with independent identity.
Each gift dragon starts as a fresh egg and becomes unique to its recipient.
Never imply AI is sentient, human, or truly emotionally dependent.

## Platform layers (build once, reuse for each dragon)

1. **Dragon Body:** layered 2D animation first (reuse Pantheon DragonMascot);
   on capable hardware offer optimized 3D (Godot/VRM/glTF), idle animation,
   emotional facial expression and lip sync.
2. **Dragon Soul:** distinct name, personality, skin, evolution state,
   preferences and owner-approved episodic + semantic memory.
3. **Dragon Mind:** provider-neutral model-router interface with local/offline
   fallback; remote S1/NVIDIA/OpenRouter/Groq/Gemini through secured broker.
   Choose providers by verified availability, latency, privacy and budget.
4. **Dragon Voice/Ears:** Android TTS first, opt-in push-to-talk STT next;
   evaluate sherpa-onnx or whisper.cpp for offline recognition, respecting
   model/voice licensing. No always-on microphone by default.
5. **Dragon Hands:** Genesis/MCP capabilities only through a separate broker,
   with per-tool scopes, verified receipts, explicit confirmations for
   money, outreach, installs, account changes, or destructive actions.
6. **Dragon Habitat:** Pantheon home launcher, Android widget for users who
   retain their own launcher, optional user-authorized overlay elsewhere,
   later live wallpaper. Respect Android background-start restrictions.
7. **Dragon Egg Foundry:** mint signed/versioned cosmetic gift packages,
   recipient claims and creates a NEW private memory store. No transfer of
   sender's personal memories, embeddings, tokens or service credentials.
8. **Dragon Alliance:** Ember/Solon may offer complementary abilities and
   coordinate via a user-approved Genesis task bridge, but do not silently
   share personal memory between dragon owners.

## Execution order and acceptance gates

### P0 — Close the current draft safely

- Read every file changed in PR #5, run lint/build/JUnit; confirm no secrets,
  no broad permissions, no background microphone and no unbounded network.
- Validate data survives app restart, memory creation/forget/reset works,
  and gift JSON contains no private notes or keys.
- Run Android emulator, then real Moto G cold-start ANR, memory and battery checks.
- Smoke-test AI opt-in using an owner-controlled key without logging it.
- Record a real PASS/FAIL receipt for each check. No self-approved merge.
- Validate installation/replacement-launcher rollback before handing out APKs.

### P1 — Living presence everywhere (Android)

- First make Ember visible and interactive in Pantheon (present in PR #5).
- Add lightweight, tappable home-screen widget for standard launchers.
- Add optional overlay with explicit Android permission request, permanent
  stop control and visible service notification; disabled by default.
- Suspend animation off-screen; cap CPU/RAM; support reduced motion and
  battery saver; test on the Moto G's 4 GB RAM.
- Ship independent signed APK only when widget/overlay path, package identity,
  install/update signing, and receiver permissions are audited.

### P2 — Real intelligence without losing personality

- Implement a versioned **EmberBrainProvider** interface rather than
  hardcoding a single cloud model.
- Integrate Genesis Anchor as a capability broker over a user-approved
  authenticated transport (not a loopback-only address from a remote phone).
- Memory model: fact + provenance + timestamp + confidence + expiration
  and per-fact consent. Support search, correction, deletion, and export.
- Run deterministic tool tests with fake services, then real read-only
  tools, then narrow write permissions with human approvals.
- Publish transparent costs, quotas and fallbacks. Offline = honest.

### P3 — Personality, voice, and evolution

- Express speech with mouth motion, idle reactions, gestures and context
  animation; voice muting must be easy and persistent.
- Evolve visible appearance through genuine user-chosen milestones, not
  artificial emotional dependence or streak pressure.
- Offer creator tooling for eyes, wings, horns, palette, voice, lore,
  animations and talents, with asset/license checks.

### P4 — Gift-ready app + Solon companion

- Build Dragon Egg v1 manifest/schema, signed export/import, claim flow,
  distinct recipient UUID, independent local vault.
- Recipient onboarding: choose nickname, voice, consented memory, optional
  integrations. Default private, no account required for local use.
- Ember (warm fire / action) and Solon (calm counterbalance / reflection)
  are separate experiences sharing only the audited runtime.
- Test gifting across two phones, reinstall, migration and revocation.
- Only then produce release APK and distribution documentation.

## Open-source candidate evaluation (do not copy blindly)

- **Godot Engine:** MIT; strong optional 2D/3D dragon world. Not needed
  for the low-memory first animated mascot.
- **Blender:** creative asset pipeline; review individual model licenses.
- **sherpa-onnx:** Apache-2.0 Android-capable offline STT/TTS/VAD.
- **whisper.cpp:** MIT speech-to-text runtime; budget memory per model.
- **Piper:** older MIT repo is archived; maintained piper1-gpl is GPL
  family, so treat commercial redistribution and voice license separately.
- **GooseDroid, Sparrow, Pocket Familiar:** study Android pet overlay UX
  and lifecycle patterns; verify every repository and asset license before use.
- **Godot VRM / VRM ecosystem:** evaluate actual renderer performance,
  asset redistribution rights, and Android integration on real hardware.

## Merge/review governance

1. A PR gets a named independent reviewer and CI status, not an implicit approval.
2. Reviewer inspects changed lines, tests, secrets, permissions, dependencies,
   Android lifecycle/battery, backward compatibility and release fallback.
3. Label findings BLOCKER / IMPORTANT / SUGGESTION and give file paths.
4. Builder fixes and reruns; reviewer verifies the exact new commit SHA.
5. Owner approves release-impacting merges; no agent auto-merges its own code.
6. Record final receipt: commit, tests, device, blockers, next action.

**Completion definition:** Ember is actually installable, interactive on a real
phone, can be stopped, honors memory choices, has a demonstrated working
model/fallback, and passes the PR audit. A beautiful mockup or chat-only demo
does **not** count as shipped.
