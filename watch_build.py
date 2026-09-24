#!/usr/bin/env python3
"""One-poll watcher for the Pantheon Launcher APK build.
Tracks the LATEST run of the 'Build release APK' workflow (v6+, signed +
shrunk; new pushes supersede older runs). On success: downloads the APK
artifact to ~/workspace/god-launcher/pantheon-launcher-v6.apk and writes
state DONE. On failure: writes FAILED. Otherwise exits quietly."""
import sys, json, os, urllib.request, urllib.error, zipfile, io
sys.path.insert(0, "/opt/hatch/skills/skill-creator/bin")
import dynamic_credentials as dc
CRED, HOSTS = "custom.github", ("api.github.com",)
BASE = "https://api.github.com"
OUT = os.path.expanduser("~/workspace/god-launcher")
STATE = os.path.join(OUT, "build_state.txt")

def req(method, path, raw=False):
    r = urllib.request.Request(BASE + path, headers={
        "User-Agent": "genesis-os-github-skill/1.0",
        "Accept": "application/vnd.github+json"}, method=method)
    dc.add_surrogate_to_request(r, CRED, entry_name="access_token", allowed_hosts=HOSTS)
    try:
        with urllib.request.urlopen(r, timeout=120) as resp:
            data = resp.read()
            return data if raw else json.loads(data.decode())
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> {e.code}: {e.read().decode()[:200]}")

def latest_run_id():
    runs = req("GET", "/repos/brandonduda8/pantheon-launcher/actions/runs?per_page=5")
    for r in runs.get("workflow_runs", []):
        if r.get("name") == "Build release APK":
            return str(r["id"])
    raise RuntimeError("no 'Build release APK' runs found")

def main():
    if os.path.exists(STATE):
        print(open(STATE).read().strip())
        return
    RUN_ID = latest_run_id()
    run = req("GET", f"/repos/brandonduda8/pantheon-launcher/actions/runs/{RUN_ID}")
    status, conclusion = run["status"], run.get("conclusion")
    print(f"run {RUN_ID}: {status}/{conclusion}")
    if status != "completed":
        return
    if conclusion != "success":
        open(STATE, "w").write(f"FAILED conclusion={conclusion}")
        print("BUILD FAILED")
        return
    arts = req("GET", f"/repos/brandonduda8/pantheon-launcher/actions/runs/{RUN_ID}/artifacts")
    names = [(a["id"], a["name"]) for a in arts.get("artifacts", [])]
    print("artifacts:", names)
    aid = next((i for i, n in names if "apk" in n.lower()), names[0][0] if names else None)
    if aid is None:
        open(STATE, "w").write("FAILED no-artifacts"); print("NO ARTIFACTS"); return
    zdata = req("GET", f"/repos/brandonduda8/pantheon-launcher/actions/artifacts/{aid}/zip", raw=True)
    apk_path = None
    with zipfile.ZipFile(io.BytesIO(zdata)) as z:
        for n in z.namelist():
            if n.endswith(".apk"):
                apk_path = os.path.join(OUT, "pantheon-launcher-v6.apk")
                with open(apk_path, "wb") as f: f.write(z.read(n))
                break
    if apk_path:
        open(STATE, "w").write("DONE")
        print(f"APK saved: {apk_path} ({os.path.getsize(apk_path)} bytes)")
    else:
        open(STATE, "w").write("FAILED no-apk-in-zip"); print("NO APK IN ZIP")

main()
