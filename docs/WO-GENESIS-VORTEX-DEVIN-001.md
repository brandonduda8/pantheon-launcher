# WO-GENESIS-VORTEX-DEVIN-001 — Engineering execution brief
**Recipient:** Devin (AI software engineering agent), after owner activates a Devin session or configured GitHub automation.
**Sponsor:** Genesis founder. **Design owner:** Genesis Vortex identity.
**Repository:** `brandonduda8/pantheon-launcher`.
**Starting point:** `main`. Do NOT merge the parallel Ember draft PR #5 into this task without independent review.
**Assets:** `docs/GENESIS-VORTEX-BRAND-BIBLE-v1.md`, `design/genesis-vortex.tokens.json`, `assets/vortex/termux-colors.properties`.

## Mission
Deliver the first *working*, beautiful, safe, reversible **VORTEX // EMBER** brand preset inside the existing Android Pantheon Launcher. Produce a PR and installable CI artifact only after required tests—not just concept art.

## First: read-only repository audit (mandatory)
1. Inventory current launcher architecture, theme engine/export, wallpapers, app drawer, permissions, widgets, animations, accessibility, and tests.
2. Compare the current `README.md` note about failing API-34 emulator smoke tests against current GitHub Actions. Do not claim green without a recent verified run.
3. Inspect `MainActivity`, theme persistence, setup/rollback, current Phoenix/dragon interactions, battery and graphics loops, and the existing permissions in AndroidManifest.
4. Review existing `v2-mockups/`, `v3-mockups/`, and `AGENT_BRIDGE.md`; reuse working code instead of rewriting the launcher.
5. Submit a short audit with evidence, blockers and exact file plan *before* broad changes.

## Deliverable A — Theme engine integration
- Implement a versioned brand token mapping from `design/genesis-vortex.tokens.json` to Compose/Android colors, dimensions, and text styles. Bundle fonts only when their exact distribution licenses are verified; system fallback must work.
- Add in-app **Preview / Apply / Undo / Restore Default** controls for `VORTEX // EMBER`; settings must persist safely and never erase existing dock/app assignments.
- Add an export/importable preset descriptor with schema version, visual tokens, icon pack selection and migration behavior; exclude passwords, device identifiers, personal facts, API keys, financial data and companion memory.
- Ensure theme application cannot modify device-global settings without clear OS confirmation.

## Deliverable B — Visual surfaces
- Rebrand home screen, clock/date, dock, app drawer, app icons, quick-access Genesis hub and built-in widgets using the design system.
- Ship *at least one original static wallpaper* (best performance) and an optional live-visual implementation that respects visibility and power saver.
- Build an elegant Ember companion entrypoint and style treatment that works even if the unfinished separate Ember feature PR is not merged. Plan future Solon without fake functionality.
- Ensure 48dp touch targets, scalable text, talkback labels, legible contrast and non-color-only status indicators.

## Deliverable C — Performance and reliability
Target the Motorola Moto G 2025 class (4GB RAM, unrooted, Android 15+). Favor view-scope animations, avoid per-frame allocations, and cap effects with explicit low-power mode. Record actual CPU/memory/frame-time values (not estimates) from emulator and physical device when available; measure process memory, crashes, startup, battery, and thermal effects. Preserve existing launcher stability and app launching. Android system UI/lock screen/notification shade are out of scope.

## Deliverable D — Terminal + optional companions
- Package the Termux colors file as user-opt-in installation instructions with backup/restore.
- Provide safe shell theme/alias snippets as files and docs; no commands that require root, silently edit startup files, or automatically run external services.
- Structure a companion widget integration interface; no cloud or network tool execution by default.

## Validation gates
- Compile Android debug and run all existing/new relevant tests.
- Execute a launch / app list / application open / home/back / theme preview-apply-undo / restart / disable theme / no network smoke matrix.
- Verify no new dangerous permissions, exported components, or remote command execution paths; report any security-related changes explicitly.
- Test reduced motion, large font, TalkBack semantics, wallpaper fallback, battery saver, app cold-start, and rotation/orientation if supported.
- Run CI, upload an APK artifact and report exact workflow run and SHA; mark *not validated* until Moto G real-device checks and independent reviewer verdict.
- Never merge, publish to Play Store, install on a device, enable overlays, or incur spend without founder authorization.

## Handoff
Open a **draft PR** against `main` with before/after screenshots, architecture/permission delta, performance evidence, emulator logs, test results, a migration and rollback plan, and remaining blockers. Zane performs independent verification, the founder authorizes release, and the Genesis reviewer critiques PR changes without self-approval.

## Work phases
- **P0:** audit, design contract and CI baseline, no code changes.
- **P1:** minimal theme tokens + reversible preset + static wallpaper, validated PR.
- **P2:** custom widgets/icons + Ember and Solon entrypoints, validated PR.
- **P3:** optional live effects, Termux setup, resource budgets, Moto G smoke test.

**Definition of done:** A usable Android home screen with genuinely applied Vortex visuals, a reversible preset, a passing verified build, and full QA receipts. If your Devin account is not integrated with this repository, the issue and this file are the complete prompt to start a session manually; do not claim that Devin has begun until a session/PR receipt exists.
