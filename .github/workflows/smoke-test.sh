#!/usr/bin/env bash
# Pantheon Launcher — API 34 emulator smoke test.
#
# Why a file and not an inline `script:` block: android-emulator-runner
# executes EACH LINE of an inline script in its own fresh `/usr/bin/sh -c`,
# so shell variables (PKG=...) silently vanish between lines. The old inline
# gate therefore ran `am start -n /.MainActivity` (empty package), the app
# never launched, and the "process not running" failure was the gate's own
# bug — not the app's. This file runs as ONE shell, so state persists.
#
# Gate: install release APK, launch MainActivity, wait ~45s, dump logcat,
# FAIL on any ANR or fatal crash from com.apexforge.godlauncher.
set -euo pipefail

PKG=com.apexforge.godlauncher
APK="${1:-apk/app-release.apk}"

echo "== smoke test: $APK on $(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r'))"
ls -la "$APK"

echo "== installing"
adb install -r "$APK"

adb logcat -c

echo "== launching $PKG/.MainActivity"
START_OUT="$(adb shell am start -n "$PKG/.MainActivity" 2>&1)"
echo "$START_OUT"
if echo "$START_OUT" | grep -qi "error"; then
    echo "SMOKE_FAIL: 'am start' reported an error — the activity did not launch"
    exit 1
fi

echo "== launched, waiting 45s for splash + home to settle..."
sleep 45

# DIAGNOSTIC (v7): capture main-thread stack during the settle window.
# If the emulator's software renderer is overwhelming the app, SIGQUIT
# dumps every thread's stack to logcat — definitive proof of where the
# main thread is stuck (vs guessing from frame stats).
APP_PID="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$APP_PID" ]; then
    echo "== diagnostic: SIGQUIT to $PKG (pid $APP_PID) for thread dump"
    adb shell "kill -3 $APP_PID" 2>/dev/null || true
    sleep 3
fi

adb logcat -d > logcat.txt || true
adb shell screencap -p /sdcard/smoke.png >/dev/null 2>&1 || true
adb pull /sdcard/smoke.png smoke.png >/dev/null 2>&1 || echo "no screenshot" > smoke.png.txt
adb shell "cat /data/anr/traces.txt" > anr-traces.txt 2>/dev/null \
    || echo "no /data/anr/traces.txt (expected without root)" > anr-traces.txt

FAIL=0
echo "== ANR / crash check for $PKG"

if grep -q "ANR in $PKG" logcat.txt; then
    echo "SMOKE_FAIL: ANR detected for $PKG"
    grep -B2 -A60 "ANR in $PKG" logcat.txt | head -120
    FAIL=1
fi

# A fatal crash prints "FATAL EXCEPTION" followed within a few lines by
# "Process: <package>". Scope the check to our package — emulator system
# processes can crash during boot and that is not our signal.
if grep -A4 "FATAL EXCEPTION" logcat.txt | grep -q "Process: $PKG"; then
    echo "SMOKE_FAIL: fatal crash in $PKG"
    grep -B8 -A25 "FATAL EXCEPTION" logcat.txt | head -100
    FAIL=1
fi

if ! adb shell pidof "$PKG" >/dev/null 2>&1; then
    echo "SMOKE_FAIL: process $PKG not running after 45s"
    echo "--- last ActivityManager lines ---"
    grep -E "ActivityManager" logcat.txt | tail -20 || true
    FAIL=1
else
    echo "process $PKG alive (pid $(adb shell pidof "$PKG" | tr -d '\r'))"
fi

# Confirm MainActivity actually reached the resumed state (not just "process
# alive but stuck on a black screen").
if adb shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -q "mResumedActivity.*$PKG"; then
    echo "MainActivity is the resumed activity"
else
    echo "SMOKE_FAIL: MainActivity never reached resumed state"
    adb shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -E "mResumedActivity|mFocusedApp" | head -5 || true
    FAIL=1
    # DIAGNOSTIC (v7): extended resume polling — does it EVER resume?
    # If the emulator is just slow to warm up (SwiftShader), the activity
    # may resume at +90s. This distinguishes "slow warmup" from "stuck".
    echo "== diagnostic: polling for resumed state (up to +120s)..."
    for i in 1 2 3 4 5; do
        sleep 15
        if adb shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -q "mResumedActivity.*$PKG"; then
            echo "DIAGNOSTIC: MainActivity resumed at +$((45 + i * 15))s (slow warmup, not stuck)"
            break
        fi
        echo "DIAGNOSTIC: still not resumed at +$((45 + i * 15))s"
    done
fi

if [ "$FAIL" -ne 0 ]; then
    echo "== SMOKE RESULT: FAIL"
    exit 1
fi
echo "SMOKE_PASS"
echo "== SMOKE RESULT: PASS — no ANR, no crash, MainActivity resumed"
