# Ember Living Dragon — first integration (v0)

## What this branch actually builds

Ember is a **companion experience inside Pantheon Launcher**, not an independent APK yet.
Tap the procedural dragon on the Pantheon home screen to open Ember. The existing
Canvas avatar still handles breathing, blinking, bobbing, and fire particles.
The new companion experience adds:

- A persistent on-device soul (stage, experience, energy, bond, and up to 40 user-taught memories).
- Pet, practice, and rest interactions with deterministic growth. These are *game* stats; they do not indicate model training or increased factual intelligence.
- Owner-controlled memory creation, selective forgetting, and a two-step full reset.
- Local simple responses and Android text-to-speech when an available system voice exists.
- An explicit opt-in switch for cloud AI replies. Each press sends the current message and up to 20 most recent intentionally stored memories to Gemini. The switch defaults **off**; it requires the key already configured in Phoenix. Offline interactions never invoke EmberBrain.
- A shareable, cosmetic-only JSON dragon egg with **no** personal memories, provider keys, or identifying fields. Receiving and hatching eggs is **not** implemented yet.

## What is NOT built or verified

This is not yet an always-on independent Android companion, home-screen widget
on other launchers, live wallpaper, speech recognition, autonomous agent runtime,
MCP tool executor, 3D/VRM dragon, or released store app. No claims of sentience.
No real device install, runtime performance, AI connection, or emulator test
is verified until CI and the device gate run.

## Test and install gate

1. Review this draft PR's diff; do not merge before independent review.
2. Let the secret-free PR workflow build a **debug APK**.
3. Confirm unit/build jobs pass. Perform an Android 14/15/16 emulator smoke test,
   then a physical Moto G test with animations and battery profiling.
4. Pantheon is a replacement HOME launcher: avoid setting it as the default
   until the Home button, app drawer, back navigation, and ANR gates pass.
5. Android signing identity matters: debug APK signing may not match a
   currently installed release. Do not uninstall a working launcher
   without backing up settings and verifying a reversible install path.

## Architecture to grow into

- **Ember Avatar** — current low-cost Canvas dragon; later custom skeletal 2D,
  optional VRM on capable devices; throttle updates by lifecycle.
- **Ember Soul** — local consent-based memories and growth; later searchable
  memory with encrypted export, owner review and provenance.
- **Ember Mind** — provider-neutral model router and bounded reasoning;
  current Gemini bridge is a temporary module, never an unrestricted agent.
- **Ember Hands** — future Genesis capability broker with explicit per-action
  permissions, audit receipts, revocation, and no token leakage.
- **Dragon Egg/Gifts** — per-recipient claim, independent identity, ownership,
  licensed art, consent for memory transfer (defaults to never).
- **Solon** — separate identity and memory, sharing a permissioned engine.
  Yin–yang is a creative relationship, not shared private memories.

## Security and privacy checklist

- [x] No embedded API keys or user secrets
- [x] Cloud thinking opt-in (off by default)
- [x] Private memory opt-in for each fact
- [x] Privacy-safe exported gift
- [x] Reset/forget controls
- [x] No background microphone or ambient surveillance
- [x] No new Android permissions in v0
- [ ] Independent code review
- [ ] CI build + unit tests
- [ ] Emulator test
- [ ] Physical device battery/ANR test
- [ ] Cloud model live test
- [ ] Per-user encryption/biometric lock
- [ ] Accessibility and gift import UX
