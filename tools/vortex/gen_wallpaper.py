"""Generates the original VORTEX // EMBER static wallpaper (no third-party art).

Usage: python3 tools/vortex/gen_wallpaper.py
Output: app/src/main/res/drawable-nodpi/vortex_ember_wallpaper.webp (1080x2400)
Palette comes from design/genesis-vortex.tokens.json. Deterministic (seeded).
"""
import json, math, os
import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TOKENS = json.load(open(os.path.join(ROOT, "design", "genesis-vortex.tokens.json")))
C = {k: v["value"] if isinstance(v, dict) else v for k, v in TOKENS["color"].items()} if "color" in TOKENS else None


def hx(s):
    s = s.lstrip("#")
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32) / 255.0


def palette():
    flat = {}
    def walk(node):
        if isinstance(node, dict):
            for k, v in node.items():
                if isinstance(v, str) and v.startswith("#") and len(v) == 7:
                    flat[k] = v
                else:
                    walk(v)
    walk(TOKENS)
    return flat


P = palette()
W, H = 1080, 2400
rng = np.random.default_rng(7)
y, x = np.mgrid[0:H, 0:W].astype(np.float32)
cx, cy = W * 0.5, H * 0.40
dx, dy = (x - cx) / W, (y - cy) / W
r = np.sqrt(dx * dx + dy * dy)
theta = np.arctan2(dy, dx)

void = hx(P.get("voidBlack", "#06070B"))
abyss = hx(P.get("abyssPurple", "#2B0A59"))
violet = hx(P.get("electricViolet", "#7A2BFF"))
magenta = hx(P.get("plasmaMagenta", "#D14BFF"))
storm = hx(P.get("stormBlue", "#3EC8FF"))
ember = hx(P.get("dragonEmber", "#FF6A2B"))
gold = hx(P.get("emberGold", "#F2A63B"))

img = np.zeros((H, W, 3), np.float32) + void
# deep abyss haze
haze = np.exp(-(r / 0.55) ** 2)[..., None]
img = img * (1 - haze * 0.85) + abyss * haze * 0.85
# logarithmic spiral arms
arms = 0.5 + 0.5 * np.cos(3 * theta - 9.0 * np.log(r + 0.03))
arms = (arms ** 6) * np.exp(-((r - 0.28) / 0.22) ** 2)
img += violet * arms[..., None] * 0.55
img += storm * ((arms ** 3) * np.exp(-((r - 0.12) / 0.07) ** 2))[..., None] * 0.35
# portal ring + magenta inner rim
ring = np.exp(-((r - 0.30) / 0.012) ** 2)
img += magenta * ring[..., None] * 0.55
inner = np.exp(-((r - 0.18) / 0.006) ** 2) * (0.5 + 0.5 * np.cos(theta * 6))
img += violet * inner[..., None] * 0.4
# void core
core = np.exp(-(r / 0.07) ** 2)[..., None]
img = img * (1 - core) + void * core
# ember spark trail (lower-right arc) — <10% accent
spark = np.exp(-((r - 0.30) / 0.02) ** 2) * np.clip(np.cos(theta - 0.9), 0, 1) ** 12
img += ember * spark[..., None] * 0.6 + gold * (spark ** 2)[..., None] * 0.3
# stars
stars = (rng.random((H, W)) > 0.9993).astype(np.float32) * rng.uniform(0.3, 0.9, (H, W)).astype(np.float32)
img += hx(P.get("silverMist", "#C8D2E8")) * stars[..., None] * (r > 0.33)[..., None]
# vignette keeps UI text readable
vig = np.clip(1.15 - 0.9 * np.sqrt(((x - W / 2) / W) ** 2 + ((y - H / 2) / H) ** 2) * 1.6, 0.25, 1)
img *= vig[..., None]
# bottom fade for dock legibility
fade = np.clip((y - H * 0.70) / (H * 0.30), 0, 1)[..., None]
img = img * (1 - 0.65 * fade) + void * 0.65 * fade

out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))
dst = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "vortex_ember_wallpaper.webp")
os.makedirs(os.path.dirname(dst), exist_ok=True)
out.save(dst, "WEBP", quality=80, method=6)
print(dst, os.path.getsize(dst), "bytes")
