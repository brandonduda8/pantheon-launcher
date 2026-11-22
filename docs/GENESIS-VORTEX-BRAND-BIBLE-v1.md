# GENESIS VORTEX — Brand Bible v1.0
**Status:** Proposed identity, approved direction for prototyping; not a trademark clearance or final production theme.
**Owner:** Genesis / founder. **Date:** 2026-11-21 (America/Chicago).
**Tagline:** Living Intelligence. A World in Your Pocket.

## Purpose
Genesis Vortex is the recognizable visual design system for the user's personal Android experience and the broader Genesis software universe: Pantheon Launcher, Ember, Solon, command terminals, Genesis agents, and—when separately adopted—Aurum Pulse. It should look unlike a stock Android phone yet feel readable, trustworthy, fast, and usable every day.

**Brand attributes:** Electric · Mystical · Precise · Living · Premium · Human-controlled.
**Brand promise:** A personal world of intelligent tools with clear control, consent, and real outcomes.
**Do not:** suggest the dragons are sentient, claim impossible system privileges, obscure critical Android controls, or prioritize spectacle over usability.

## Naming architecture
- **GENESIS** — the overarching ecosystem.
- **GENESIS VORTEX** — brand design system and flagship visual shell.
- **PANTHEON** — launcher product; presents the Vortex visual identity.
- **VORTEX // EMBER** — first signature personal phone preset.
- **EMBER** — fire, action, creation, protection; warm orange lightning.
- **SOLON** — balance, strategy, reflection; blue/silver starlight.
- **AURUM PULSE** — distinct music product, may use a Vortex-compatible theme without losing its own identity.
- **GHOST / ASTRA / ZANE / DEVIN** — operational names; roles are not represented as verified connections by design alone.

The names and logo are working creative choices pending originality and trademark review before any commercial release.

## Color tokens
| Token | Hex | Use |
|---|---|---|
| void-black | `#06070B` | Base canvas |
| abyss-purple | `#2B0A59` | Deep panel / atmospheric depth |
| electric-violet | `#7A2BFF` | Primary brand/accent |
| plasma-magenta | `#D14BFF` | Secondary glow |
| storm-blue | `#3EC8FF` | Intelligence / Solon / information |
| ember-gold | `#F2A63B` | High-value creation cues |
| dragon-ember | `#FF6A2B` | Ember character / action |
| silver-mist | `#C8D2E8` | Tertiary text / Solon |
| text-primary | `#F5F3FF` | Main text |
| text-muted | `#B5A9C9` | Secondary text, only on dark backing |
| surface | `#10101C` | Cards, drawers, bottom sheets |
| surface-elevated | `#19132A` | Focused panels |
| border | `#5D348F` | Borders, sparingly |

**Visual allocation:** ~70% dark space, 20% violet/blue structural treatment, <10% concentrated glows/character accents. Neon is an accent, not body-text color. Validate real text/icon contrast (WCAG AA 4.5:1 regular text, 3:1 large text and icons); do not rely on color alone for meaning.

## Symbol and logo
A simple **open triangular frame around a counterclockwise vortex** is the starting logo concept. Preserve a one-color silhouette variant and a 24-dp app icon version that still reads without any glow. Avoid intricate details, unlicensed fonts/assets and copyrighted franchise iconography. Logo should be usable on phone screens, repo badges, marketing, and merchandise.

## Typography
- **Display / titles:** Orbitron, with a system-sans fallback.
- **Everyday UI:** Space Grotesk, with Android system sans fallback.
- **Console / diagnostics:** JetBrains Mono, with monospace fallback.
- Favor 14–16sp readable body, 20–28sp headers. Respect Android font scaling and touch targets >=48dp.

## Shape and motion language
Panels: charcoal glass with thin purple border and subtle luminous edge. Major UI elements: 16–24dp radii. Minor controls: 10–14dp radius. Icons: original 1.5–2dp geometric strokes with clear filled/outlined states. Vortex rings use a consistent rotation and restrained depth; electric filaments punctuate meaningful events (launch, complete, error), not continuous jitter.

Three render budgets:
1. **Static / battery saver:** no animation, no animated wallpaper, zero background render loop.
2. **Balanced / default:** subtle effect only while launcher visible; low-motion transition <=250ms; event-driven widgets.
3. **Cinematic:** optional stronger particles and glow; automatic downgrade on power saver/thermal issues.
Support Reduce Motion, accessibility scaling, and manual 'disable effects' in Settings. No continuous animation while app is backgrounded.

## Phone layout — VORTEX // EMBER
**Primary screen:** Portal wallpaper backdrop, legible clock/date at upper third, home symbol, and a compact bottom dock with no more than five essential applications. Put Ember in one *small, actionable companion widget*, not a giant permanent overlay. A readable agent/mission indicator is optional. Keep app drawer search fast and obvious.

**Secondary panel:** Six restrained categories: CORE, MISSIONS, EMBER, SOLON, CREATION, SYSTEM; MONEY visible only when a real verified metric exists. Do not display fabricated live financial information, agent activity, or system status.

**Widgets:** clock, Ember consent-based bond/level, tasks with real data, device vitals sourced through Android APIs, optional Aurum Pulse controls. Show 'unavailable' or 'last updated' for stale data. No hidden collection of contacts, messages, or notifications.
**Navigation:** gestures and home/back behaviors match Android conventions. Provide a visible 'Return to default launcher' route.
**Icons:** original monochrome violet outlines; orange for Ember and creation, blue for Solon and diagnostics. Keep recognizability for non-Genesis apps.

## Terminal signature
Palette: void background, primary text silver mist, commands violet, success cyan/green, warnings ember gold, errors red accessible on dark.
Banner: `GENESIS VORTEX ONLINE`.
Prompt sample: `genesis@vortex:~$`.
Core semantic aliases proposed: `genesis`, `ember`, `solon`, `missions`, `status`, `launch`. These are design goals; commands must not silently execute privileged actions. Existing Termux setup must be backed up before changing it.
Use the provided `assets/vortex/termux-colors.properties` as a theme seed (copy intentionally, not auto-install).

## Sound and feedback
Original short sound marks only: gentle low-frequency startup sweep, crisp violet-energy click, warm Ember completion chime, airy Solon information chime. Do not play unlicensed music. Respect silent/DND mode and system audio policies; all effects can be muted.

## Asset deliverables
1. Vector-compatible logo + monochrome fallback with ownership/licensing record.
2. Responsive wallpaper designs, static baseline first; low-power optional live wallpaper.
3. At least 16 coherent app icons + adaptive icon treatment.
4. Android theme tokens and accessible widget components.
5. Termux theme + optional opt-in startup banner.
6. QA screenshots: default, dark/bright lighting, font scaling, battery saver, no-network, reduced-motion.

## Non-negotiable engineering rules
- Moto G 2025-class phone: unrooted Android 15+; limited 4GB device memory.
- No root requirement; no blanket Shizuku/ADB privileges; no inaccessible system UI hacks.
- No secrets in repositories, themes, screenshots, logs or share artifacts.
- Default to local, offline, consent-based behavior. No background microphone or notification reading without separate explicit user approval.
- Every customization is previewable, reversible and install-safe; never auto-switch the default launcher or reset existing user data.
- Keep mobile process memory and battery use measurable; warm start, ANR and crash tests required.
- Independent review required before release and phone installation.

## Success criteria
A user can recognize Genesis Vortex in under five seconds from the launcher icon, visual hierarchy, and companion widgets; they can still quickly place a call, search an app, dismiss/disable effects, and restore the previous launcher. The preset has an exported/importable deterministic configuration and a CI-verified, independently reviewed build on a real Motorola phone.

## Versioning
`genesis-vortex/1.0` is the visual design contract. Future presets must publish token diffs, screenshot diffs and migration notes. Do not modify current production defaults merely by merging brand documents.
