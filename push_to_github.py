#!/usr/bin/env python3
"""Push ~/workspace/god-launcher to brandonduda8/pantheon-launcher via the git-database API.
One commit, one push event -> one Actions run. Never touches raw credentials."""
from __future__ import annotations
import base64, json, os, sys, urllib.request, urllib.error

sys.path.insert(0, "/opt/hatch/skills/skill-creator/bin")
import dynamic_credentials as dc

CRED, HOSTS = "custom.github", ("api.github.com",)
BASE, UA = "https://api.github.com", "genesis-os-github-skill/1.0"
OWNER, REPO, ROOT = "brandonduda8", "pantheon-launcher", os.path.expanduser("~/workspace/god-launcher")

def req(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, headers={
        "User-Agent": UA, "Accept": "application/vnd.github+json",
        "Content-Type": "application/json"}, method=method)
    dc.add_surrogate_to_request(r, CRED, entry_name="access_token", allowed_hosts=HOSTS)
    try:
        with urllib.request.urlopen(r, timeout=120) as resp:
            raw = resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> HTTP {e.code}: {e.read().decode('utf-8','replace')[:300]}")
    return json.loads(raw) if raw.strip() else {}

files = []
for dp, dns, fns in os.walk(ROOT):
    dns[:] = [d for d in dns if d != ".git"]
    for fn in fns:
        # Build artifacts stay local: the 56MB APK exceeds the git-database
        # blob API size limit (HTTP 422) and doesn't belong in source history.
        if fn.endswith(".apk"):
            print(f"skip artifact: {os.path.relpath(os.path.join(dp, fn), ROOT)}", flush=True)
            continue
        full = os.path.join(dp, fn)
        files.append((os.path.relpath(full, ROOT), full))
files.sort()
print(f"files: {len(files)}", flush=True)

tree = []
for rel, full in files:
    with open(full, "rb") as f:
        content = base64.b64encode(f.read()).decode()
    blob = req("POST", f"/repos/{OWNER}/{REPO}/git/blobs",
               {"content": content, "encoding": "base64"})
    tree.append({"path": rel, "mode": "100644", "type": "blob", "sha": blob["sha"]})
print("blobs done", flush=True)

t = req("POST", f"/repos/{OWNER}/{REPO}/git/trees", {"tree": tree})
c = req("POST", f"/repos/{OWNER}/{REPO}/git/commits",
        {"message": "Pantheon Launcher v6 - fix release compile errors: (1) trailing-lambda parsing hazard in PhoenixParticles/QuantumView phaseOf - a `{ cycle.value }` on the line after `)` was parsed as a trailing lambda to animateFloat; restructured to nullable State<Float> cycleState with a plain phaseOf lambda; (2) GodDock drawBehind import corrected to androidx.compose.ui.draw.drawBehind; (3) added missing State import in QuantumView.", "tree": t["sha"]})
try:
    req("POST", f"/repos/{OWNER}/{REPO}/git/refs",
        {"ref": "refs/heads/main", "sha": c["sha"]})
except RuntimeError as e:
    if "422" in str(e):
        req("PATCH", f"/repos/{OWNER}/{REPO}/git/refs/heads/main", {"sha": c["sha"], "force": True})
    else:
        raise
req("PATCH", f"/repos/{OWNER}/{REPO}", {"default_branch": "main"})
print(f"PUSHED: commit {c['sha'][:7]} to main")
