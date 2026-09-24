# Mockup #4 — The Forge screen

**File:** `mockup-4-forge.png` (1080×2400, portrait phone)

Procedural Pillow render of the Forge screen (no dedicated media tool in
this session), in the locked ash-fire brand language: charcoal `#0B0A09`
base, ember `#E25822` primary, phoenix gold `#F5B942`, quantum cyan
`#22D3EE` reserved for the "building" live state.

## What's shown

- **Header:** back chevron, "The Forge" title, "Request rebuilds from
  Zane's crew" subtitle, refresh action.
- **Connection card:** green liveness dot, gateway URL, "API key: set",
  EDIT affordance (opens the URL + px- key dialog).
- **New request composer:** type chips (App mod selected → `launcher-mod`,
  SDK, Feature, Theme, Other), title field (required), multi-line details
  field, ember "Send to the Forge" button.
- **My work orders:** three cards covering the status lifecycle —
  BUILDING (cyan pill with glow = the live pulse state), QUEUED (amber),
  READY (green) with the prominent "Download & install" button and the
  "Install unknown apps" helper text.

## Design decisions

- Status pills carry the state color; only BUILDING pulses (cyan), and
  the pulse is gated on the animation toggle + resumed lifecycle —
  static frame otherwise.
- Ready orders without an APK URL show result text (or a graceful
  "no download attached" line) instead of the button — the button only
  appears when `ForgeApi.extractApkUrl()` finds one.
- Empty states (not shown): unconfigured gateway, no orders, unreachable
  gateway with Retry — all rendered as centered cards in the same style.
