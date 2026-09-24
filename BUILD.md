# Pantheon Launcher — v1 build guide

**Pantheon Launcher** is Brandon's own custom Android home screen, built with
Kotlin + Jetpack Compose + Material3 (dark theme only). Package
`com.apexforge.godlauncher`, minSdk 29, targetSdk 34.

## What v1 does

- **Quantum home canvas** — animated neon nebula (violet/teal/magenta),
  twinkling starfield, slowly rotating zodiac ring. Runs at ~30fps and pauses
  when the app isn't visible, so it sips battery.
- **Live clock + date** at the top.
- **Search bar** — filters installed apps as you type, tap to launch.
- **Pantheon dock** — 8 god quick-actions (Zeus, Athena, Hermes, Hephaestus,
  Tyche, Argus, Odysseus, Themis). Tap launches the assigned app; **long-press
  a god to assign any installed app** (persisted in DataStore).
- **App drawer** — full installed-app grid, alphabetical + searchable.
- **Long-press empty home space** → applies the bundled quantum-mandala
  wallpaper (`res/drawable/quantum_bg.webp`, 720px, ~280KB) as system wallpaper.
- **Settings** — animation on/off toggle, per-god assignment with clear,
  About Pantheon.
- Registers as a real launcher (`CATEGORY_HOME`): after install, Android
  offers it as the default home app.

## How the APK gets built (two paths)

### Path A — GitHub Actions (recommended, zero local setup)

1. **Brandon's one tap:** create a repo (e.g. `brandonduda8/pantheon-launcher`)
   and push this folder's contents to it. That's the only manual step.
2. The workflow in `.github/workflows/build.yml` runs on every push:
   JDK 17 → Gradle 8.7 → `:app:assembleDebug`.
3. Download the APK from the run's **Artifacts** section
   (`pantheon-launcher-debug` → `app-debug.apk`).
4. On the moto g: open the APK, allow "install unknown apps" for the
   browser/Files app when prompted, install, then press Home and pick
   **Pantheon Launcher** as the default launcher (tap "Always").

### Path B — Android Studio on a computer

1. Open this folder in Android Studio (Koala or newer).
2. Let it sync Gradle (needs internet once for dependencies).
3. `Build → Build APK(s)` → `app/build/outputs/apk/debug/app-debug.apk`.
4. Transfer to the phone (USB, Drive, or cable) and install as in step 4 above.

## Tech notes

- Toolchain pinned: AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.06.00,
  Gradle 8.7, JDK 17. No Gradle wrapper jar is committed; CI installs Gradle.
- `QUERY_ALL_PACKAGES` is declared so the drawer sees every app on
  Android 11+. This APK is sideloaded, so no Play policy review applies.
- Icons are rasterized to `ImageBitmap` on `Dispatchers.IO` at load time —
  the UI thread never decodes drawables.
- All launcher state (god assignments, animation toggle) lives in one
  DataStore (`pantheon_prefs`) and survives reboots.
- Release builds enable R8 with the rules in `app/proguard-rules.pro`;
  debug builds (the CI artifact) are unshrunk.

## Project layout

```
god-launcher/
├── settings.gradle / build.gradle / gradle.properties
├── .github/workflows/build.yml   # CI: builds debug APK, uploads artifact
├── BUILD.md                      # this file
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/apexforge/godlauncher/
        │   ├── MainActivity.kt            # screen state, app loading, wallpaper
        │   ├── model/{AppInfo,God}.kt
        │   ├── data/{AppRepository,PantheonStore}.kt
        │   └── ui/
        │       ├── theme/{Color,Type,Theme}.kt
        │       ├── background/QuantumBackground.kt
        │       ├── home/{HomeScreen,ClockWidget,GodDock}.kt
        │       ├── drawer/AppDrawer.kt
        │       ├── settings/SettingsScreen.kt
        │       └── components/{AppRow,AppPickerDialog}.kt
        └── res/
            ├── values/{strings,colors,themes}.xml
            ├── drawable/{quantum_bg.webp,ic_launcher_foreground.xml}
            └── mipmap-anydpi-v26/ic_launcher.xml
```
