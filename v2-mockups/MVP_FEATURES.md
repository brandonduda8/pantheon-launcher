# Pantheon Launcher v2 — MVP Feature Spec

**Brand promise:** not just a home screen — Brandon's command center.
**Constraints (non-negotiable):** Kotlin + Jetpack Compose + Material3, `com.apexforge.godlauncher`,
minSdk 29 / targetSdk 34, GitHub Actions build, Moto G mid-range performance, no new heavy
dependencies (standard AndroidX/Compose only), no root, no accessibility-service hacks, no heavy ML.

**Research sources:** Android Hire "Best Android Launcher 2026" roundup (updated 2026-09-15),
Android Authority Nova post-mortem, Ordoh Nova-alternatives 2026, launcher-comparison tables.

## What the winners do (research takeaways)

- **Lawnchair (2026's #1, free/open source):** Pixel-style grid, Material You, icon packs, gestures,
  hidden apps, app-drawer folders, on-device search, backup/restore (incl. Nova backup import),
  Private Space support. Lesson: the bar is "familiar grid + deep personalization + zero cost."
- **Niagara:** one-handed vertical list, wave alphabet, notifications on home screen, pop-up widgets,
  Usage Breaker. Lesson: speed-of-access and one-hand use beat icon walls.
- **Smart Launcher 6:** auto-categorized drawer tabs, smart search, Nova backup import. Lesson: the
  drawer should organize itself; nobody wants to file apps by hand.
- **Kvaesitso:** search IS the home screen — apps + contacts + calendar + files + math + web in one
  bar, privacy-first. Lesson: universal search is a killer feature and needs no server.
- **AIO Launcher:** information-dense dashboard (weather, calendar, messages, system stats) as the
  home screen. Lesson: at-a-glance info earns the home screen on a command center.
- **Microsoft Launcher:** customizable feed, Copilot one swipe away. Lesson: an AI assistant belongs
  at swipe distance, not buried behind a button.
- **Action Launcher:** Shutters (swipe icon → widget), Covers. Lesson: layered interactions (tap vs
  swipe) multiply one icon's power.
- **Nova (legacy):** gestures, icon packs, notification badges, backup/restore. Lesson: the power-user
  checklist is gestures + badges + backup; missing any of them loses the "best-in-class" claim.

---

## MUST-HAVE for v2 (ranked by impact-per-effort)

### 1. Phoenix command bar on the home screen (persistent Ask Phoenix)
- **What:** A prominent "Ask Phoenix" bar pinned above the god dock on the home screen (always
  visible, no navigation needed). One tap → Phoenix chat opens; later: voice input.
- **Why Brandon:** Phoenix is the brand's soul and the differentiator against every launcher above —
  nobody else has an AI command center. Currently it takes two taps to reach Phoenix; v2 makes it
  ambient. This is the single feature that makes the launcher *Pantheon*.
- **Buildability:** SMALL (S). `HomeScreen.kt` already hosts the Ask Phoenix button; move/duplicate it
  as a persistent Material3 `SearchBar`/card. Reuses existing `PhoenixScreen` + `PhoenixBrain`
  (Gemini bridge via DataStore API key). Optional voice: `SpeechRecognizer` with
  `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` — standard API, no new dependency.
- **Permissions:** None for text. `RECORD_AUDIO` only if/when voice input ships — requested in-context
  at first use, justifiable as "voice commands for Phoenix."

### 2. Smart suggestions row ("Your day, predicted")
- **What:** A horizontal strip above the dock showing 4–5 apps predicted for *right now*, learned from
  his own launch history (most-used apps by time-of-day bucket + day-of-week). Purely on-device,
  transparent ("Because you open Gmail at 8am").
- **Why Brandon:** Phone-first, job-hunting, building a business — he re-opens the same 6–10 apps on
  a rhythm. Niagara/Kvaesitso win by reducing taps to content; this is our one-handed,
  zero-config version of that. Also honest "smart" — no server, no tracking, nothing to explain.
- **Buildability:** SMALL (S). No new APIs: intercept in the existing `launch()` path (AppRepository /
  MainActivity ViewModel) and log `(packageName, timestamp)` to DataStore. Scoring = weighted count
  per hour-bucket; ~100 lines. Rendering = existing `AppRow` components in a `LazyRow`.
- **Permissions:** None. All on-device; nothing leaves the phone.

### 3. Universal search (apps + contacts + quick actions)
- **What:** One search entry point (search icon on home + swipe-down gesture) that searches installed
  apps first, then contacts (call/text), then quick actions ("money log", "morning brief" →
  opens Phoenix with that prompt), with calculator-style math answers (Kvaesitso-style).
- **Why Brandon:** Kvaesitso proved search can *be* the home screen. For a job hunter, searching
  "Aerotek" → call the recruiter contact, or "Gmail" → launch app, in one bar is daily-use speed.
  Kills the "which drawer tab is it in" problem.
- **Buildability:** SMALL–MEDIUM (S/M). Apps: reuse `AppRepository.loadApps()` + filter (exists).
  Contacts: `ContactsContract` via ContentResolver — standard API, ~60 lines. Quick actions: static
  list mapping phrases → intents/deep links into Phoenix screen. Rank: apps > contacts > actions.
- **Permissions:** `READ_CONTACTS` — requested in-context only when the user first opens universal
  search ("so you can call/text people from search"). Standard, justifiable; graceful degradation
  without it (apps-only search still works).

### 4. Notification badges on god dock + drawer
- **What:** Unread-count badges on the god dock icons and drawer rows, driven by the standard
  notification-listener path — or the lighter alternative: `ShortcutBadger`-style is dead; the
  honest modern path is `NotificationListenerService` counting active notifications per package.
- **Why Brandon:** Part of the Nova-era power-user checklist; without badges the launcher doesn't
  feel best-in-class. Missing messages/job alerts on a job hunt is a real cost.
- **Buildability:** MEDIUM (M). `NotificationListenerService` subclass + `BIND_NOTIFICATION_LISTENER_SERVICE`
  permission; `getActiveNotifications()` → count per packageName → badge state in Compose. Must handle
  the settings-intent flow (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`) and the service being
  killed (it's a system-bound service; reliable). No root, no hack — this is the sanctioned API.
- **Permissions:** Notification access — the sensitive one. Justifiable ("badges need to see
  notifications"), must be opt-in from Settings with clear copy. This is the standard launcher
  pattern (Niagara/Lawnchair do exactly this).

### 5. Gestures (swipe-down = search, double-tap = lock, swipe-up = drawer)
- **What:** Nova-class gesture layer on the home screen: swipe down → universal search; double-tap →
  screen off; swipe up → app drawer; (later, per-god swipe = secondary action).
- **Why Brandon:** One-handed, phone-first operation on a Moto G. Gestures are the #1 Nova nostalgia
  feature and the cheapest "feels pro" upgrade. Swipe-down-to-search pairs with #3.
- **Buildability:** SMALL (S) for everything except lock. Compose `pointerInput`/`detectTapGestures`
  + `draggable` — no library. Screen-off on double-tap: `DevicePolicyManager.lockNow()` requires
  device-admin (heavy, scary permission) — **ship gestures without double-tap-lock in v2**, or use
  the accessibility-free trick of launching with `KEYCODE_SLEEP`... which needs no permission only
  via instrumentation (not available). Honest call: double-tap-to-lock is LATER; swipe gestures are S.
- **Permissions:** None for the v2 set (search/drawer gestures). Double-tap-lock would need device
  admin — deferred with the feature.

### 6. Morning-brief ritual card (Phoenix-generated daily brief)
- **What:** A home-screen card that each morning shows a Phoenix-generated brief: today's focus,
  top 3 priorities, any job-application follow-ups due. Generated on first home-screen view after
  6am via the existing Gemini bridge; cached in DataStore for the day; refresh button.
- **Why Brandon:** He already runs a morning-brief concept in Genesis (brief.py) — this puts it
  where he actually looks, on the launcher. Turns the launcher from app-grid into ritual/command
  center. Directly serves the $1k/week mission (follow-ups = money).
- **Buildability:** MEDIUM (M). Reuses `PhoenixBrain` (Gemini call) + DataStore caching
  (`brief_date`, `brief_text` keys — same pattern as `PantheonStore`). Prompt template with date +
  his focus areas. Needs graceful offline state ("Brief unavailable — check connection") and a
  no-API-key state linking to Settings. No new dependencies.
- **Permissions:** None. (Uses INTERNET, already required for Phoenix.)

### 7. Backup / restore settings
- **What:** Export all launcher state (god-dock assignments, labels, animation toggle, Phoenix API key
  excluded or encrypted, suggestions history opt-out) to a JSON file; import it back. Share/export
  via Storage Access Framework picker.
- **Why Brandon:** Nova/Lawnchair/Smart Launcher all treat backup as table stakes; a launcher without
  it feels unfinished. He rebuilds his setup across the Termux/phone workflow — one-tap restore
  after reinstall is real insurance.
- **Buildability:** SMALL (S). DataStore → JSON serialization (kotlinx.serialization or manual
  JSONObject — kotlinx.serialization is already a transitive Compose dep in most setups; else manual).
  SAF `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT` — no storage permission needed on modern
  Android. API key: export with explicit opt-in checkbox, default off.
- **Permissions:** None (SAF picker = user-granted per file).

### 8. Hide apps + app drawer folders
- **What:** Long-press in drawer → hide app (moves to a "Hidden" section, PIN-optional later);
  drawer supports folders (drag or multi-select → folder). Auto-categorization is explicitly NOT
  v2 (see LATER).
- **Why Brandon:** Job-hunt phone = banking apps, gig apps, personal stuff he doesn't want on the
  front page. Lawnchair/Smart Launcher both ship this; it's expected.
- **Buildability:** SMALL (S). Hidden set = `StringSet` in DataStore, filter in `AppDrawer` list;
  folders = DataStore map folderName → package list, rendered as expandable groups. ~150 lines total.
- **Permissions:** None.

### 9. Per-god secondary actions (tap = launch, swipe-up/long-press menu = actions)
- **What:** Each god dock slot gets optional secondary actions: e.g. Zeus → "morning scan" / "approve
  queue" (deep links into Phoenix prompts or specific app activities); configured in the existing
  long-press assign flow as an "actions" list. Home screen: swipe up on a god icon (or long-press
  menu) reveals its 2–3 quick actions.
- **Why Brandon:** This is the Pantheon brand made functional — gods aren't just app shortcuts, they're
  agents with verbs. One-tap money-lane shortcuts live here (log earning, open money lane).
- **Buildability:** MEDIUM (M). Data model: extend `PantheonStore` with per-god action list
  (label + type: app-launch / phoenix-prompt / url). UI: long-press bottom sheet with "add action"
  + swipe-up gesture on dock icons (pairs with #5's gesture layer). Deep-linking into *other apps'*
  specific screens is unreliable without their exported activities — v2 actions target: launch app,
  open URL, or send prompt to Phoenix. That's the honest scope.
- **Permissions:** None.

### 10. Material You dynamic theming + dark mode polish
- **What:** `dynamicColorScheme` on Android 12+ (Moto G on recent updates supports it), graceful
  fallback palette on older; a real dark/light/auto toggle in Settings.
- **Why Brandon:** "Best-in-class" look for free. Lawnchair won 2026 partly on Material You polish.
  The quantum wallpaper + Phoenix particles deserve a theme system that doesn't clash.
- **Buildability:** SMALL (S). `dynamicDarkColorScheme`/`dynamicLightColorScheme` in `Theme.kt`
  (exists) gated on SDK 31+; Settings toggle persisted in DataStore (same pattern as
  `ANIMATIONS_ENABLED`). No new dependencies.
- **Permissions:** None.

---

## LATER (worth doing, not v2)

- **Auto-categorized drawer (Smart Launcher-style).** Needs a category map (Play Store categories via
  PackageManager are unreliable/offline); honest version needs a bundled mapping or on-device
  heuristic. Real work, marginal gain once universal search (#3) exists. *Defer until search proves
  insufficient.*
- **Widgets on home screen (incl. pop-up widgets / Shutters).** Real `AppWidgetHost` integration is
  genuinely complex (host lifecycle, bind permissions, widget sizing in Compose) — L, and the
  morning-brief card (#6) covers the at-a-glance need. *Defer to v3.*
- **Icon packs.** Standard `appfilter.xml` parsing is doable (Lawnchair does it) but it's a full
  subsystem (pack discovery, drawable resolution, fallback) — M/L with fiddly edge cases. The god
  dock's identity is custom anyway. *Defer; revisit if users ask.*
- **Double-tap-to-sleep.** Requires device-admin (`DevicePolicyManager.lockNow()`) — scary permission
  prompt that hurts install trust for one gesture. *Defer; ship other gestures now.*
- **Usage Breaker / screen-time features (Niagara-style).** Needs `PACKAGE_USAGE_STATS` (special-access
  permission, settings deep-link, user suspicion). Brandon wants *more* phone productivity, not less
  phone. *Defer; wrong direction for the brand.*
- **At-a-glance dashboard widgets (AIO-style weather/system stats).** Weather needs an API key/service;
  system stats are easy but the morning-brief card covers the ritual. *Defer; weather widget is a
  natural v3 add with Open-Meteo (free, no key).*
- **Focus modes / app timers.** `PACKAGE_USAGE_STATS` + `DevicePolicyManager`/`Digital Wellbeing`
  intents — permission-heavy, and Minimalist Phone already owns this lane. Not Pantheon's fight.
  *Defer indefinitely.*
- **Private Space support (Android 15+).** minSdk 29 codebase; `LauncherApps` private-profile APIs are
  35+. The Moto G target may not even have it. *Defer until targetSdk/minSdk reality demands it.*
- **Voice-first Phoenix (wake word / always-listening).** Always-on mic = battery drain + `RECORD_AUDIO`
  foreground-service complexity + trust problem. Tap-to-talk (in #1) is the sane 95%. *Defer.*
- **Cloud sync of settings.** Backup/restore (#7) covers the need without accounts, servers, or privacy
  liability. *Defer until there's a real multi-device story.*

## Explicitly OUT (does not belong)

- **Root-only features** (system app removal, status-bar mods, deep theming of other apps). Non-starter.
- **Accessibility-service hacks** (for gesture injection, auto-click, screen-off workarounds). Play
  Store policy risk, user-trust killer, and unnecessary — every v2 feature above uses sanctioned APIs.
- **On-device ML / heavy "AI"** (custom suggestion models, embeddings). The suggestion engine (#2) is
  counting, not ML; Gemini calls go to the API. A 7B model does not belong in a launcher on a Moto G.
- **Lock-screen replacement.** Separate OS surface, permission minefield, and out of scope for a
  launcher MVP.
- **Duplicate system apps** (own dialer, SMS, gallery inside the launcher). Maintenance black hole;
  deep-link to the real apps instead.

---

## Permission summary (install-time story)

| Permission | Feature | Justification at install time |
|---|---|---|
| `INTERNET` | Phoenix bridge, brief | Already required; "Phoenix AI needs internet" |
| `READ_CONTACTS` | Universal search (#3) | In-context, first search use; "call/text people from search"; optional |
| Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Badges (#4) | Opt-in from Settings; "show unread counts on icons" — standard launcher ask |
| `RECORD_AUDIO` | Voice input for Phoenix (#1, optional) | In-context at first mic tap; "voice commands for Phoenix"; optional |
| None | Everything else (#2, #5 gestures, #6, #7, #8, #9, #10) | No new permissions — strong trust story |

**Install-time pitch:** the launcher asks for *nothing* at install beyond what v1 already needs.
Every sensitive permission is requested in-context, at the moment of first use, with a one-line
reason — and every feature degrades gracefully without it.

## Suggested v2 build order (matches the ranking)

1. Phoenix command bar (#1) + Material You polish (#10) — brand surface, fast wins
2. Smart suggestions (#2) + universal search (#3) + gestures (#5) — the speed layer
3. Badges (#4) + hide/folders (#8) — the power-user checklist
4. Morning brief (#6) + per-god actions (#9) + backup/restore (#7) — the command-center layer
