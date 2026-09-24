# Pantheon Launcher v3 — "Total Mod Engine" design language

Brand: broken-hearted → reborn, ashes → embers → fire. v3 adds the quantum
machine layer: the launcher is no longer a home screen, it is the control
deck of Brandon's agent computer.

## Palette (locked)

- ash-void `#0B0A09` — base
- charcoal `#12100E` — surfaces
- ember `#E25822` — primary energy, activity, CTAs
- phoenix gold `#F5B942` — headlines, sacred accents
- quantum cyan `#7DF9FF`-ish — data streams, building states, machine layer

Ember = myth. Cyan = machine. They never fight: ember leads, cyan connects.

## The three infinite loops

All procedural, seamless (motion is a pure function of `t mod T` with integer
cycles per period — the loop point is invisible), tunable for speed / density /
hue shift:

1. **Quantum field** — data streams riding sine paths + pulsing network nodes +
   packet bursts. The "my agents are computing" feeling.
2. **Ember storm** — rising embers + falling ash, wrap-around depth layers.
   The brand's home weather.
3. **Nebula drift** — slow radial-gradient nebula blobs + star field. Calm,
   deep, expensive-feeling.

## Quantum agent view (the jaw-drop)

Eight god orbs as compute nodes in a constellation. Bezier particle streams
flow between them; when a god is "working" (real events: command-bar use,
suggestion taps, notification arrivals, god taps) an activity pulse radiates
from its node and streams accelerate toward it. Tap an orb = open that god.
Long-press = quick actions. This is the first thing a demo audience sees.

## Theme engine

Every swappable item lives in one screen: Backgrounds, Layout, Icons,
Color & Type, Motion, Gestures, Search & Badges, Dragon, Command Bar,
Profiles. Presets: Ember Rebirth / Quantum Machine / Void Minimal.
Showroom mode auto-cycles presets every ~10s with a floating stop pill.
Themes export/import as shareable JSON.

## The Forge

Separate from live theming: the Forge sends rebuild requests (app mod, SDK,
feature, theme, other) to Zane's machine via the Pantheon gateway, tracks
work orders queued → building → ready/failed, and installs the returned APK.
Hammer-and-anvil energy, same brand.

## Performance contract (unchanged from v2)

~30fps procedural canvas, pause when screen off, static frame when the
animation toggle is off, no new install-time permissions, no secrets in the APK.
