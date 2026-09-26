# Pantheon Launcher

A custom Android home screen (`com.apexforge.godlauncher`) built with Kotlin, Jetpack Compose and Material 3. It registers as a real launcher (`CATEGORY_HOME`). minSdk 29, targetSdk 34, currently **v7.0.0**.

> **CI status:** the signed release build passes. The API-34 **emulator smoke gate is currently failing**: it has been red on every run since it was added (2026-09-24), and the latest v7 commits target cold-start jank and ANRs. Treat v7 as work in progress until the gate is green.

## What it does

- **Animated home canvas:** neon nebula, starfield and a slowly rotating zodiac ring, about 30 fps. It pauses when the launcher isn't visible.
- **Live clock and date**, plus a **search bar** that filters installed apps as you type.
- **Pantheon dock:** 8 quick-action "gods" (Zeus, Athena, Hermes, Hephaestus, Tyche, Argus, Odysseus, Themis). Tap one to launch its app; long-press to assign any installed app (persisted in DataStore).
- **App drawer:** every installed app, alphabetical and searchable.
- **GENESIS hub (v7):** live phone vitals and network probes for Brandon's own machines, a list of every action the launcher can take, and a stored system snapshot, clearly labeled as stored rather than live.
- **Phoenix on-device intents:** battery, storage, time, date, and navigation to every screen.
- **Agent bridge:** a credential-free, same-device broadcast protocol that lets external agent frameworks talk to the launcher. It's protocol-ready but not yet connected; see [AGENT_BRIDGE.md](AGENT_BRIDGE.md).

## Build and CI

`.github/workflows/build.yml` runs on every push to `main`:

1. **build:** JDK 17 and Gradle 8.7 produce a signed, R8-shrunk release APK, using a persistent keystore stored in repo secrets so updates install in place. The APK is uploaded as the `pantheon-launcher-release` artifact.
2. **smoke-test:** installs that APK on an API-34 emulator, launches it, and fails on any ANR or fatal crash from the package ([`smoke-test.sh`](.github/workflows/smoke-test.sh)). The logs and a screenshot are uploaded as artifacts.

No Gradle wrapper is committed; CI installs a pinned Gradle. For local builds and sideloading, see **[BUILD.md](BUILD.md)** (written for v1; the build steps still apply).

## Repo map

- `app/`: the launcher source
- `BUILD.md`: build and install guide
- `AGENT_BRIDGE.md`: the agent broadcast protocol
- `v2-mockups/`, `v3-mockups/`: design notes and mockups
- `push_to_github.py`, `watch_build.py`: helper scripts for pushing and watching CI builds
