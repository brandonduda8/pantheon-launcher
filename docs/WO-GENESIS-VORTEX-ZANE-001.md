# WO-GENESIS-VORTEX-ZANE-001 — Alliance QA and release control
**Owner:** Zane (independent verifier/operator), as relayed by the founder.
**Project:** Genesis Vortex identity + Pantheon Launcher implementation.
**Primary repository:** `brandonduda8/pantheon-launcher`.
**Engineering implementer:** Devin, once its repo access and session are verified; not yet presumed active.
**Docs:** `GENESIS-VORTEX-BRAND-BIBLE-v1.md`, `WO-GENESIS-VORTEX-DEVIN-001.md`, design tokens and terminal palette.

## Mission
Maintain alignment among the founder, Astra/Genesis design, Devin engineering, and Zane independent QA. Stop the project from becoming only an impressive image: require a working safe, installable Android experience. Preserve founder's existing launcher/app data.

## Required actions
1. Confirm Devin identity, repository access, connection method (Devin app / configured GitHub automation) and the exact start receipt before reporting Devin 'working.' Do not impersonate or guess account identity.
2. Review the brand contract for readability, asset provenance, consistency and conflicting Genesis/Pantheon/Ember/Solon naming.
3. Read-only audit current launcher architecture and CI baseline. Existing `README.md` notes release build works but API-34 emulator smoke gate historically failed; re-check live.
4. Review new design PR for scope, missing permissions, data loss, startup regressions, battery/thermal impact, accessibility, and privacy.
5. Verify no root or Shizuku is required for baseline appearance; all elevated controls separately opt-in and constrained.
6. Independently exercise: first launch; app launch/search; widget visibility; wallpaper static; token accessibility; theme preview, apply, revert; phone restart; low power/no internet; Android font scaling; optional Termux theme opt-in.
7. Require evidence for memory/frame timing, pass/fail test matrix, screenshots and exact APK artifact link, commit SHA, device/API, and rollback instructions.
8. Perform explicit audit of Ember PR #5 independently; do not merge unfinished companion code merely to meet visual milestone.
9. Preserve a hard release gate: no APK promoted or system-default launcher changed until founder gives permission. Do not log keys/tokens.

## Status language
Use exactly **VERIFIED**, **REPORTED**, **BLOCKED**, **NOT TESTED**, or **FAILED** on each milestone; evidence must show source, time, tool and commit SHA when applicable. Distinguish an Android CI success from physical Motorola compatibility.

## Exit criteria
- Founder can apply a coherent Vortex preset on Moto G and restore defaults without data loss.
- Screen performance is acceptable in low-memory and battery-saver modes.
- All CI tests are green, independent review is recorded, and release provenance is clear.
- Devin or an alternative engineer has delivered an actual PR, not only a plan.
- Final report lists what shipped, remaining blockers, QA receipts, and next engineering priorities.
