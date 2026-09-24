# Pantheon Launcher v2 — Visual Design Spec
**Design lead mockups:** `mockup-1-home.png` · `mockup-2-dragon.png` · `mockup-3-splash.png`
All portrait phone format (≈1080×2400, 9:19.5). Style: dark cinematic AAA splash art — **charcoal ashes + deep ember orange + phoenix gold**, no photoreal humans.

---

## Brand soul (locked 2026-09-23)

### The thesis (Brandon's words)
> "I need it to be the broken hearted brand."
> "I don't wear my scars. I mastered them."
> Theme: "life changing content on the most valuable information no one tells you about life pain death and growth."

### The narrative: ASHES → EMBERS → FIRE
Every screen, animation, and icon serves one story: **ruin into rebirth**. The user opens the launcher and rises from ashes every time.

1. **ASHES** — the bottom of every screen is charcoal black, falling ash, cracked dark ground. This is where Brandon started: broken, heavy, honest. The dock sits *in* the ash — the gods glow from within the ruin, not above it.
2. **EMBERS** — the middle band is where things smolder: ember cracks in the stone, rising sparks, the dragon's ember breath. Embers are the present tense — pain that hasn't died, it just learned to glow.
3. **FIRE** — the top of every screen is the phoenix: wings spread, gold and deep ember orange, feathers of living flame. This is the direction everything rises toward. Nothing moves downward except ash. Nothing falls except the past.

The phoenix is **not decoration — it's him**. Brandon's life thesis rendered as a bird. The launcher's one job, visually, is to make him feel like he rises every time he unlocks his phone.

### How the elements serve the story
- **God dock:** 8 icons glowing like embers in the dark at the bottom of the ash. The gods are the tools of the rebirth — each halo a coal waking up. Left→right they brighten in sequence, like a fire catching.
- **Dragon:** scarred-but-loyal. Weathered scales, one nicked horn, a scar across the eye ridge — and warm loyal golden eyes. It's the version of Brandon that survived: damaged, still guarding the home screen. Curled loyally above the dock, tail between its own paws, tiny ember breath — fire it kept for itself.
- **Boot/ignition:** the screen must feel like RISING FROM ASHES. Ash settles first (the ruin), then the heart cracks open (mockup-3), the phoenix tears out of it, and the fire parts to reveal the wordmark. You don't open an app — you ignite.
- **Clock/search/Ask Phoenix:** UI floats in the ember band between ruin and rebirth — warm gold, readable, never competing with the fire.

---

## 1. Color Palette (locked)

### Core — the ash-fire spectrum
| Token | Hex | Use |
|---|---|---|
| `ash-void` | `#0B0A09` | Deep background base — the darkness everything rises from |
| `charcoal` | `#12100E` | Ash ground / dock glass base / cracked stone |
| `ash-grey` | `#3A3632` | Falling ash particles, cracked-stone texture |
| `smoke` | `#5C554E` | Ember smoke trails, dimmed background states |
| `ember-deep` | `#8A2A0E` | Ember shadow / fire trailing glow |
| `ember` | `#E25822` | Primary accent — Ask Phoenix button, ember bursts, fire core |
| `ember-hot` | `#F97316` | Fire bright edge, phoenix feather mids |
| `gold` | `#F5B942` | Phoenix gold — clock, wordmark, feather tips, fire heart |
| `starlight` | `#F5EFE6` | Warm text over dark |
| `glass` | `#1A1512CC` (alpha) | Frosted search bar / dock tray, blur 12–16dp behind |

### Per-God Halo Colors (dock ring + icon glow)
| God | Halo | Icon glyph |
|---|---|---|
| Zeus | `#3B82F6` (electric blue) | lightning bolt |
| Athena | `#A855F7` (wisdom violet) | owl |
| Hermes | `#22D3EE` (swift cyan) | winged caduceus |
| Hephaestus | `#E25822` (forge ember — matches brand) | hammer + anvil |
| Tyche | `#F5B942` (fortune gold — matches brand) | four-leaf clover |
| Argus | `#E879F9` (all-seeing magenta) | eye |
| Odysseus | `#FB7185` (voyager rose) | ship |
| Themis | `#38BDF8` (justice sky-blue) | scales |

Two of the eight (Hephaestus, Tyche) deliberately share the brand's ember/gold — the gods that forge and bless the rebirth. The rest are coals in the dark, kept slightly dimmer so the ash-fire spectrum dominates.

Dock tray: dark charcoal glass rounded container (28dp corner), each icon = 56dp circle, ring 2dp in god color, glow radius ~8dp, label 11sp in halo color at 70% alpha.

---

## 2. Particle Aesthetics (procedural, Moto G-safe)

Everything below is Compose Canvas-drawn — **no video, no Lottie, no sprite sheets**.

- **Falling ash** (lower screen): ≤ 40 particles, 2–6px grey `#3A3632 → #5C554E`, slow downward drift with sine wobble, fade in/out at life ends. Ash only falls — it never rises.
- **Rising embers** (upper screen): ≤ 60 particles, 2–5px, additive `SRC_OVER` glow, colors cycled `#E25822 → #F97316 → #F5B942`, rising drift, fade at life ends. Embers only rise — they never fall. (The up/down rule is the brand, encoded as physics.)
- **Ash-ground base**: static pre-rendered gradient bitmap (one 512×1024 texture scaled up, drawn once per frame — or cached as a `Bitmap` layer redrawn only on resize). Vertical narrative: `#0B0A09` top → ember crack band at the horizon → `#12100E` cracked ground bottom. Ember cracks are 3–4 thin jagged radial gradients in `#E25822` fading to `#8A2A0E`.
- **Phoenix plume trails**: 12–18 particles/s emitted from wing tips on the idle loop, curling upward with decreasing alpha, gold→ember→transparent. Pool capped; recycled.
- **Dragon ember breath**: on dragon tap, one-shot burst of 25 ember particles from snout, 0.8s life, orange→smoke fade. Fire core drawn as layered radial gradient (gold-white → `#F5B942` → `#E25822` → transparent).
- **Splash ignition** (`mockup-3`): single full-screen 2.5s sequence — ash-grey settle (0–0.4s) → charcoal heart cracks open at center, radial ember burst 0→1.2 scale with 120 recycled spark particles (0.4–1.2s) → phoenix flame silhouette tears out of the broken heart (1.2–1.8s) → fire parts revealing "PANTHEON" wordmark + thesis line fade-up (1.8–2.5s) → crossfade into home, embers from the burst seed the idle ember system.

**Performance budget:** never more than ~150 live particles; target 60fps with `drawScope` batching, `BlendModePlus` additive for glow layers, and `LaunchedEffect` timers rather than per-frame allocations (pool all particle objects).

---

## 3. Animation Beats — broken → reborn in motion

### Ignition (app boot splash)
Ash settles (0–0.4s, the ruin) → charcoal heart cracks open, ember shockwave + 120 sparks (0.4–1.2s, the breaking) → phoenix flame silhouette tears upward from the broken heart (1.2–1.8s, the rising) → fire parts, "PANTHEON" + "I don't wear my scars. I mastered them." fade up (1.8–2.5s, the claim) → crossfade to home, embers seed the idle system (the aftermath lives on). **Must feel like rising from ashes, not like a logo.**

### Idle loop (home screen, always on)
1. Embers rise continuously; ash falls continuously — the two never cross.
2. Phoenix wings pulse (2% scale + glow intensity oscillation, 4s) — breathing fire.
3. Dragon tail sways (±10°, 3s sine); occasional slow blink; ember breath curls up every ~7s on its own.
4. God halos pulse in a left→right ignition wave, 6s cycle — like coals catching fire one by one.
5. Clock updates every minute in warm gold; "Ask Phoenix" button glows with a slow ember breathing.

### Tap reactions
- **God icon:** halo flares (ring 2→5dp, glow 8→24dp, 300ms), ember ring burst (15 particles), haptic tick, then launch. Tapping a god = waking a coal.
- **Ask Phoenix:** button glow blooms ember→gold, ember ripple rises upward from the button, voice/search sheet slides up.
- **Dragon mascot:** happy hop (translateY −40dp spring back, 400ms), one ember breath puff, tail wag speeds up briefly. The dragon guards; you greet it.
- **Search bar:** focus glow in ember-hot, keyboard up.
- **Long-press on home:** background dims 30%, phoenix descends slightly (parallax −60dp), "edit mode" hint appears.

---

## 4. Mockup Notes for the Builder

- **mockup-1-home**: home composition target — reads bottom-to-top: smoldering charcoal ruin at the bottom → ember band with clock/search/Ask Phoenix in the middle → phoenix of ember and gold erupting across the top. God dock glows like embers in the ash. (Dock icon labels in the mockup are placeholder art; the actual 8-god roster comes from the app config.)
- **mockup-2-dragon**: dragon mascot reference — small purple dragon, weathered scales, one nicked horn, scar across one eye ridge, warm loyal golden eyes, curled loyally above the dock, tiny ember breath. Compose as simple shape paths (body/head/tail/wings) + Canvas-drawn scars + ember-breath particle puff. Cute-guardian, not menacing.
- **mockup-3-splash**: the ignition reference frame — charcoal heart breaking open with phoenix fire bursting out, ash falling below, embers rising above, "PANTHEON" wordmark (wide letter-spaced thin serif, gold with ember glow) and the thesis line "I don't wear my scars. I mastered them." small beneath. This is the brand soul on one screen; the boot sequence animates *toward* this frame then parts into the home screen.

## 5. Hard Constraints (from Brandon)
- Dark theme only. No light mode variant needed.
- No photoreal humans anywhere.
- Moto G target: procedural Canvas only; ≤ ~150 live particles; pre-rendered ash-ground bitmap reused; no heavy video/Lottie deps. The ash-falls / ember-rises particle rule is cheap (same pool, different velocity vectors) — and it's the brand, so keep it.
