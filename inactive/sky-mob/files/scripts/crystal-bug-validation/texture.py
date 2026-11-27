"""Generates the original 64x64 Crystal Bug texture (P2-D1, approved concept A: rock type).

Run: python3 -I scripts/crystal-bug-validation/texture.py <output.png>

The UV atlas MUST match CrystalBugRenderer.createLayer (same u/v/size per part). box() asserts that no regions overlap
and that everything stays inside 64x64. Every pixel is opaque (no translucency, no emissive layer).

Palette: rough grey rock shell, small purple square eyes, teal / sky blue / purple / pink crystals. Values are pushed
above the nominal reference swatches because the entity renderer darkens textures by roughly 0.75-0.8.
"""
import random
import sys
from PIL import Image

ROCK = [(92, 89, 100), (118, 115, 126), (142, 139, 150), (164, 161, 172), (186, 183, 194)]  # dark -> light
CRACK = (66, 63, 74)
EYE, SOCKET = (186, 96, 255), (48, 34, 66)
CRYSTALS = {  # deep, base, light, highlight
    "teal": [(22, 150, 156), (48, 214, 206), (130, 246, 236), (226, 255, 252)],
    "sky": [(52, 120, 220), (104, 184, 255), (176, 222, 255), (236, 248, 255)],
    "purple": [(92, 52, 196), (150, 100, 255), (198, 160, 255), (240, 226, 255)],
    "pink": [(184, 62, 172), (238, 116, 226), (255, 176, 244), (255, 232, 252)],
}
COLOR_ORIGIN = {"teal": (0, 34), "sky": (32, 34), "purple": (0, 49), "pink": (32, 49)}
# name: (u, v, shaft w, shaft h, tip h, tip u, tip v) inside a colour block - mirrors CrystalBugRenderer.Size
SIZES = {"XL": (0, 0, 3, 5, 2, 0, 10), "L": (12, 0, 2, 4, 2, 12, 8), "M": (20, 0, 2, 3, 2, 12, 8),
         "S": (28, 0, 1, 2, 1, 28, 4)}

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
_registered = []
rng = random.Random(10112026)


class Face:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    def px(self, i, j, c):
        if 0 <= i < self.w and 0 <= j < self.h:
            img.putpixel((self.x + i, self.y + j), c + (255,))

    def fill(self, c):
        for i in range(self.w):
            for j in range(self.h):
                self.px(i, j, c)


def box(name, u, v, w, h, d, shared=False):
    bw, bh = 2 * d + 2 * w, d + h
    assert u >= 0 and v >= 0 and u + bw <= 64 and v + bh <= 64, name
    if not shared:
        for other, (ou, ov, ow, oh) in _registered:
            assert u + bw <= ou or ou + ow <= u or v + bh <= ov or ov + oh <= v, f"{name} overlaps {other}"
        _registered.append((name, (u, v, bw, bh)))
    return {
        "top": Face(u + d, v, w, d), "bottom": Face(u + d + w, v, w, d),
        "right": Face(u, v + d, d, h), "front": Face(u + d, v + d, w, h),
        "left": Face(u + d + w, v + d, d, h), "back": Face(u + 2 * d + w, v + d, w, h),
    }


def rock(f, shade=0, edge=True):
    """Rough stone: mottled greys, short dark cracks, darker lower/outer border for plate separation."""
    for i in range(f.w):
        for j in range(f.h):
            k = 2 + shade + rng.choice((-1, 0, 0, 0, 1))
            f.px(i, j, ROCK[max(0, min(4, k))])
    for _ in range(max(1, f.w * f.h // 14)):
        i, j = rng.randrange(f.w), rng.randrange(f.h)
        f.px(i, j, CRACK)
        f.px(i + rng.choice((-1, 1)), j + rng.choice((0, 1)), ROCK[0])
    if edge and f.h > 2:
        for i in range(f.w):
            f.px(i, f.h - 1, ROCK[0])


def segment(name, u, v, w, h, d):
    faces = box(name, u, v, w, h, d)
    rock(faces["top"], 1, edge=False)
    for i in range(faces["top"].w):  # raised front lip, shadowed rear edge: overlapping plates
        faces["top"].px(i, faces["top"].h - 1, ROCK[1])
        faces["top"].px(i, 0, ROCK[4] if rng.random() < 0.6 else ROCK[3])
    for n in ("front", "back"):
        rock(faces[n], -1)
    for n in ("right", "left"):
        rock(faces[n], 0)
        faces[n].px(0, 0, ROCK[3])
    rock(faces["bottom"], -2, edge=False)
    return faces


def crystal(color, size):
    u0, v0 = COLOR_ORIGIN[color]
    su, sv, w, h, th, tu, tv = SIZES[size]
    deep, base, light, hi = CRYSTALS[color]
    shaft = box(f"{color}_{size}", u0 + su, v0 + sv, w, h, w, )
    tip_name = f"{color}_tip_{tu}_{tv}"
    tip = None if any(r[0] == tip_name for r in _registered) else box(tip_name, u0 + tu, v0 + tv, w, th, w)
    for n, f in shaft.items():
        if n in ("top", "bottom"):
            f.fill(light if n == "top" else deep)
            continue
        side_light = n in ("front", "right")
        for i in range(f.w):
            for j in range(f.h):
                c = base if j > f.h // 3 else light
                if i == f.w - 1 and f.w > 1:
                    c = deep if not side_light else base
                if i == 0 and side_light:
                    c = light
                f.px(i, j, c)
        if f.w > 1 and f.h > 2:  # facet highlight streak
            for j in range(0, f.h - 1):
                if (j + (0 if side_light else 1)) % 3 != 2:
                    f.px(0 if side_light else 1 % f.w, j, hi if side_light and j < f.h // 2 else light)
        f.px(min(1, f.w - 1), f.h - 1, deep)
    if tip:
        for n, f in tip.items():
            f.fill(hi if n == "top" else (light if n in ("front", "right") else base))
            if n not in ("top", "bottom") and f.w > 1:
                f.px(f.w - 1, f.h - 1, base)
                f.px(0, 0, hi)


# ---- body (UVs = CrystalBugRenderer.segment calls) ---------------------------------------------------------------
head = segment("segment0", 0, 0, 6, 4, 4)
segment("segment1", 20, 0, 6, 4, 4)
segment("segment2", 40, 0, 8, 5, 4)
segment("segment3", 0, 9, 8, 5, 4)
segment("segment4", 24, 9, 7, 4, 4)
segment("segment5", 46, 9, 5, 3, 3)
segment("segment6", 0, 18, 3, 2, 2)
leg = box("leg", 10, 18, 2, 2, 2)
for n, f in leg.items():
    rock(f, -1 if n != "top" else 0, edge=False)

# head front: small purple square eyes under a worn brow ridge
f = head["front"]
rock(f, -1, edge=False)
for i in range(f.w):
    f.px(i, 0, ROCK[3] if i % 2 else ROCK[2])
for x in (1, 4):
    f.px(x, 1, SOCKET)
    f.px(x, 2, EYE)
f.px(0, 3, ROCK[0]); f.px(5, 3, ROCK[0]); f.px(2, 3, CRACK); f.px(3, 3, CRACK)

# ---- crystals -----------------------------------------------------------------------------------------------------
for color in CRYSTALS:
    for size in ("XL", "L", "M", "S"):
        crystal(color, size)

img.save(sys.argv[1] if len(sys.argv) > 1 else "crystal_bug.png")
