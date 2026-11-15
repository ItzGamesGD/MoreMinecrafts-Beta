"""Generates the Tomb Guardian (weathered sandstone colossus, reference concept [1]) model geometry and textures.

Run:  python3 tools/tomb-guardian-model/generate.py [--preview DIR]
Out:  src/client/java/dev/itzgamesgd/moreminecrafts/client/boss/TombGuardianGeometry.java   (model layer)
      src/main/resources/assets/moreminecrafts/textures/entity/tomb_guardian/tomb_guardian.png            (body)
      src/main/resources/assets/moreminecrafts/textures/entity/tomb_guardian/tomb_guardian_eyes.png       (emissive)
      src/main/resources/assets/moreminecrafts/textures/entity/tomb_guardian/tomb_guardian_eyes_enraged.png
      src/main/resources/assets/moreminecrafts/textures/item/tomb_telegraph_area.png                      (floor tint)
      src/main/resources/assets/moreminecrafts/textures/item/tomb_telegraph_fill.png                      (charge gauge)
      src/main/resources/assets/moreminecrafts/textures/item/tomb_telegraph_fan_area.png / _fan_fill.png  (sweep fan)

All texels are painted procedurally from their 3D position; no vanilla pixels are read or copied. The emissive
textures carry the cyan (Soul) plus amber glow of the eye sockets and chest cracks; the enraged variant widens
the cracks and overheats the chest core to red-gold. Bone names and pivots are the vanilla Warden's (see model.py).
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

import model as M

ROOT = Path(__file__).resolve().parents[2]
TEX_DIR = ROOT / "src/main/resources/assets/moreminecrafts/textures/entity/tomb_guardian"
ITEM_DIR = ROOT / "src/main/resources/assets/moreminecrafts/textures/item"
JAVA_OUT = ROOT / "src/client/java/dev/itzgamesgd/moreminecrafts/client/boss/TombGuardianGeometry.java"


# ---------------------------------------------------------------- colour and noise

def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[k:k + 2], 16) for k in (0, 2, 4))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[k] + (b[k] - a[k]) * t for k in range(3))


def mul(c, f):
    return tuple(min(255.0, max(0.0, x * f)) for x in c)


SAND_LIGHT, SAND, SAND_DARK, SAND_DEEP = hx("f2e4bb"), hx("dfcca2"), hx("c2aa7a"), hx("96795a")
RED_LIGHT, RED, RED_DARK = hx("b87b69"), hx("97594a"), hx("6a3a31")
GOLD_LIGHT, GOLD, GOLD_DARK = hx("ffec9a"), hx("f4c735"), hx("b9891a")
LAPIS_LIGHT, LAPIS, LAPIS_DARK = hx("3d6fd6"), hx("2142a6"), hx("122468")
NEMES_LIGHT, NEMES, NEMES_DARK = hx("f3e9c9"), hx("e1d3ab"), hx("c0af83")
CRACK, GLYPH, SOCKET = hx("352820"), hx("7d6643"), hx("17110d")
CYAN, CYAN_SOFT, AMBER, AMBER_HOT = hx("5af5e6"), hx("2cb8ae"), hx("ffb43a"), hx("fff0a0")
EMBER, EMBER_HOT, EMBER_GOLD = hx("ff4c18"), hx("ff9a2e"), hx("ffe08a")


def _h(*a):
    h = 2166136261
    for v in a:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
        h ^= h >> 13
    return (h & 0xFFFFFF) / float(0x1000000)


def vnoise(x, y, z, seed):
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    fx, fy, fz = (fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz))

    def lat(a, b, c):
        return _h(a, b, c, seed)

    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                out += w * lat(ix + dx, iy + dy, iz + dz)
    return out


def steps(t, n):
    """Posterise t (0..1) to n levels so the stone reads as pixel art, not a smooth gradient."""
    return math.floor(max(0.0, min(0.999, t)) * n) / (n - 1)


# ---------------------------------------------------------------- materials (colour at a 3D position)

def sand(pos, seed, dirt=0.0):
    x, y, z = pos
    band = vnoise(x * 0.16, y * 0.85, z * 0.16, seed)
    fine = vnoise(x * 1.25, y * 1.25, z * 1.25, seed + 7)
    blot = vnoise(x * 0.2, y * 0.2, z * 0.2, seed + 13)
    t = 0.52 + (band - 0.5) * 0.7 + (fine - 0.5) * 0.3 - dirt * 0.25
    c = mix(SAND_DARK, SAND_LIGHT, steps(t, 6))
    if blot > 0.66:
        c = mix(c, SAND_DEEP, min(0.7, (blot - 0.66) * 1.8))
    if fine > 0.92:
        c = mix(c, SAND_LIGHT, 0.55)
    elif fine < 0.06:
        c = mix(c, SAND_DEEP, 0.5)
    return c


def red(pos, seed):
    x, y, z = pos
    band = vnoise(x * 0.2, y * 1.1, z * 0.2, seed)
    fine = vnoise(x * 1.3, y * 1.3, z * 1.3, seed + 5)
    t = 0.5 + (band - 0.5) * 0.8 + (fine - 0.5) * 0.35
    c = mix(RED_DARK, RED_LIGHT, steps(t, 5))
    if fine > 0.9:
        c = mix(c, RED_LIGHT, 0.5)
    return c


def gold(pos, seed):
    x, y, z = pos
    n = vnoise(x * 0.9, y * 0.9, z * 0.9, seed)
    c = mix(GOLD_DARK, GOLD, steps(0.55 + (n - 0.5) * 0.9, 4))
    if n > 0.82:
        c = mix(c, GOLD_LIGHT, 0.8)
    return c


def lapis(pos, seed):
    x, y, z = pos
    n = vnoise(x * 0.8, y * 0.8, z * 0.8, seed)
    f = vnoise(x * 2.1, y * 2.1, z * 2.1, seed + 3)
    c = mix(LAPIS_DARK, LAPIS_LIGHT, steps(0.45 + (n - 0.5) * 0.9, 4))
    if f > 0.93:
        c = mix(c, GOLD, 0.7)  # pyrite flecks
    return c


def nemes(t, seed, vertical=False):
    """Pleated headcloth: 2px light / 2px dark bands with a faint grain."""
    x, y, z = t.pos
    k = int(math.floor(x if vertical else y))
    band = (k // 2) % 2
    n = vnoise(x * 0.9, y * 0.9, z * 0.9, seed)
    c = mix(NEMES_DARK if band else NEMES, NEMES_LIGHT, 0.3 + (n - 0.5) * 0.4)
    return c


# ---------------------------------------------------------------- atlas helpers

FR = {}
for _p in M.PARTS:
    if _p.box:
        for _f, u0, v0, fw, fh in M.face_rects(_p):
            if fw > 0 and fh > 0:
                FR[(_p.name, _f)] = (u0, v0, fw, fh)


def A(part, face, i, j):
    r = FR.get((part, face))
    if not r:
        return None
    u0, v0, fw, fh = r
    return (u0 + i, v0 + j) if 0 <= i < fw and 0 <= j < fh else None


GLYPHS = {
    "ankh": [".###.", "#...#", "#...#", ".###.", "..#..", ".###.", "..#.."],
    "eye": [".....", ".###.", "#.#.#", ".###.", "..#..", "...#.", "....."],
    "water": [".....", "#.#.#", ".#.#.", ".....", "#.#.#", ".#.#.", "....."],
    "bird": ["..#..", ".###.", "#.#.#", "..#..", ".#.#.", ".#.#.", "....."],
    "sun": ["..#..", ".###.", "#####", ".###.", "..#..", ".....", "....."],
    "scarab": [".#.#.", "..#..", ".###.", "#####", ".###.", "#.#.#", "....."],
    "reed": ["..#..", "..#..", ".##..", "..#..", ".##..", "..#..", "..#.."],
}
GLYPH_KEYS = sorted(GLYPHS)


def line(a, b):
    (x0, y0), (x1, y1) = a, b
    pts = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if (x0, y0) == (x1, y1):
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
    return pts


def path(points, seed=0, jitter=1):
    """Polyline through waypoints with a small deterministic sideways wobble."""
    out = []
    for n, (a, b) in enumerate(zip(points, points[1:])):
        seg = line(a, b)
        for k, (x, y) in enumerate(seg):
            j = int(math.floor(_h(x, y, seed, n) * (2 * jitter + 1))) - jitter if 0 < k < len(seg) - 1 else 0
            out.append((x + (j if abs(b[1] - a[1]) >= abs(b[0] - a[0]) else 0),
                        y + (0 if abs(b[1] - a[1]) >= abs(b[0] - a[0]) else j)))
    return out


# ---------------------------------------------------------------- features

DARK = {}     # atlas pixel -> colour (engravings and plain cracks, drawn on the body texture)
GLOW = {}     # atlas pixel -> "crack" | "crack_core" | "core" | "eye" | "eye_core"
HALO = set()


def stamp_glyph(part, face, i0, j0, key, color=GLYPH):
    for j, row in enumerate(GLYPHS[key]):
        for i, ch in enumerate(row):
            if ch == "#":
                a = A(part, face, i0 + i, j0 + j)
                if a:
                    DARK[a] = color


def glyph_row(part, face, i0, j0, i1, seed):
    """Fill a horizontal strip with glyphs separated by 1px."""
    i, n = i0, 0
    while i + 5 <= i1 + 1:
        stamp_glyph(part, face, i, j0, GLYPH_KEYS[int(_h(seed, n) * len(GLYPH_KEYS))])
        i += 6
        n += 1


def dark_crack(part, face, pts, seed=0, color=CRACK):
    for x, y in path(pts, seed):
        a = A(part, face, x, y)
        if a:
            DARK[a] = color


def glow_crack(part, face, pts, seed=0, core=False):
    for x, y in path(pts, seed):
        a = A(part, face, x, y)
        if a:
            GLOW[a] = "crack"
            DARK[a] = CRACK


def build_features():
    DARK.clear()
    GLOW.clear()
    HALO.clear()

    # --- head face: eye sockets (cyan with an amber heart), brow shadow, cheek cracks, mouth
    for i0 in (3, 10):
        for j in (6, 7):
            for i in range(i0, i0 + 3):
                a = A("head", "front", i, j)
                DARK[a] = SOCKET
                GLOW[a] = "eye"
        GLOW[A("head", "front", i0 + 1, 7)] = "eye_core"
    for i in range(5, 11):
        DARK[A("head", "front", i, 13)] = CRACK
    DARK[A("head", "front", 4, 14)] = CRACK
    DARK[A("head", "front", 11, 14)] = CRACK
    dark_crack("head", "front", [(2, 9), (3, 11), (2, 14)], 31)
    dark_crack("head", "front", [(13, 8), (12, 11), (13, 14)], 32)

    # --- torso front: glowing sternum crack with an amber core, relief glyphs
    glow_crack("body", "front", [(8, 5), (9, 7), (8, 9)], 1)
    glow_crack("body", "front", [(9, 13), (8, 15), (9, 16)], 2)
    glow_crack("body", "front", [(8, 10), (5, 10), (3, 12), (2, 14)], 3)
    glow_crack("body", "front", [(9, 11), (12, 10), (14, 8), (15, 7)], 4)
    glow_crack("body", "front", [(9, 12), (12, 13), (14, 15)], 5)
    for (i, j) in [(8, 10), (9, 10), (8, 11), (9, 11), (8, 12), (9, 12)]:
        GLOW[A("body", "front", i, j)] = "core"
    for (i, j) in [(8, 11), (9, 11)]:
        GLOW[A("body", "front", i, j)] = "core_hot"
    stamp_glyph("body", "front", 1, 8, "ankh")
    stamp_glyph("body", "front", 12, 8, "eye")

    # --- torso back and sides: carved glyph rows and weathering cracks
    glyph_row("body", "back", 1, 7, 17, 11)
    dark_crack("body", "back", [(4, 14), (6, 12), (5, 9)], 41)
    dark_crack("body", "back", [(13, 5), (12, 9), (14, 13)], 42)
    for face in ("right", "left"):
        stamp_glyph("body", face, 3, 6, "scarab")
        dark_crack("body", face, [(8, 4), (7, 10), (9, 15)], 43)

    # --- ribcage overlays (planes in front of the torso): a few carved lines only
    dark_crack("right_ribcage", "front", [(1, 4), (3, 7), (2, 11)], 51)
    dark_crack("left_ribcage", "front", [(7, 3), (5, 8), (6, 12)], 52)

    # --- arms: glowing forearm cracks, dark upper-arm cracks
    for side, flip in (("right", False), ("left", True)):
        arm = f"{side}_arm"
        x = (lambda i: 7 - i) if flip else (lambda i: i)
        glow_crack(arm, "front", [(x(3), 12), (x(4), 14), (x(2), 16)], 61 + flip)
        glow_crack(arm, "right" if not flip else "left", [(3, 3), (4, 5), (3, 6)], 63 + flip)
        glow_crack(arm, "back", [(x(4), 12), (x(3), 15)], 65 + flip)
        dark_crack(arm, "front", [(x(5), 3), (x(6), 5)], 67 + flip)
        dark_crack(arm, "back", [(x(2), 4), (x(3), 6)], 69 + flip)
        stamp_glyph(f"{side}_fist", "back", 3, 1, "sun")

    # --- legs: hieroglyph panel plus cracks
    for side, flip in (("right", False), ("left", True)):
        leg = f"{side}_leg"
        stamp_glyph(leg, "front", 2, 1, "ankh" if not flip else "eye")
        dark_crack(leg, "front", [(1, 7), (2, 8)], 71 + flip)
        dark_crack(leg, "right", [(2, 1), (3, 4), (2, 8)], 73 + flip)
        dark_crack(leg, "left", [(5, 2), (4, 5), (5, 9)], 75 + flip)
        glyph_row(leg, "back", 1, 1, 7, 77 + flip)

    # --- fists: finger separations
    for side in ("right", "left"):
        for i in (2, 5, 8):
            for j in range(4, 9):
                a = A(f"{side}_fist", "front", i, j)
                if a:
                    DARK[a] = SAND_DEEP
        for i in (3, 6):
            for j in range(3, 7):
                a = A(f"{side}_fist", "bottom", i, j)
                if a:
                    DARK[a] = SAND_DEEP

    # --- feet: toe lines
    for side in ("right", "left"):
        for i in (3, 6):
            for j in range(0, 4):
                a = A(f"{side}_foot", "front", i, j)
                if a:
                    DARK[a] = SAND_DEEP

    # soft halo one texel around every glowing crack
    for (px, py), kind in list(GLOW.items()):
        if kind in ("crack",):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (px + dx, py + dy)
                if n not in GLOW:
                    HALO.add(n)


# ---------------------------------------------------------------- body texture

DIRT_BY_PART = {"right_foot": 0.7, "left_foot": 0.7, "right_leg": 0.4, "left_leg": 0.4}


def edge_shade(t, c):
    f = 1.0
    if t.j == 0 and t.side:
        f *= 1.08
    if t.j == t.fh - 1 and t.side:
        f *= 0.86
    if t.i == 0:
        f *= 1.04
    if t.i == t.fw - 1:
        f *= 0.9
    if t.face == "top":
        f *= 1.1
    elif t.face == "bottom":
        f *= 0.72
    return mul(c, f)


def base_color(t):
    p = t.part.name
    seed = (sum(map(ord, p)) * 131) & 0xFFFF
    pos = tuple(t.pos[k] + M.world_offset(t.part)[k] for k in range(3))
    local = t.pos
    dirt = DIRT_BY_PART.get(p, 0.0)

    if p in ("collar",):
        x, y, z = local
        if t.face == "top":
            return gold(pos, seed) if int(math.floor(x)) % 5 in (0, 1) else lapis(pos, seed)
        if t.face == "bottom":
            return mul(gold(pos, seed), 0.7)
        if t.j == 0 and t.side:
            return gold(pos, seed)
        if t.j == t.fh - 1 and t.side:
            return gold(pos, seed) if int(math.floor(x + z)) % 2 == 0 else mul(gold(pos, seed), 0.55)
        return gold(pos, seed) if int(math.floor(x + z)) % 5 in (0, 1) else lapis(pos, seed)
    if p in ("chest_inlay_right", "chest_inlay_left"):
        if t.face == "front" and (t.i in (0, t.fw - 1) or t.j in (0, t.fh - 1)):
            return gold(pos, seed)
        return lapis(pos, seed) if t.face == "front" else gold(pos, seed)
    if p == "belt":
        c = red(pos, seed)
        return mul(c, 0.82) if t.j == t.fh - 1 and t.side else c
    if p == "kilt":
        x, y, z = local
        if t.face == "front":
            if t.j >= 6:
                return gold(pos, seed) if (t.i + t.j) % 2 == 0 else mul(gold(pos, seed), 0.7)
            return mul(sand(pos, seed), 0.8) if t.i % 2 == 0 else sand(pos, seed)
        return sand(pos, seed)
    if p in ("nemes_cap",):
        return nemes(t, seed)
    if p == "nemes_back":
        c = nemes(t, seed, vertical=False)
        if t.side and t.j in (4, 11):
            return gold(pos, seed)                      # gold bands across the headcloth tail
        if t.side and t.j in (5, 12) and (t.i % 4 == 1):
            return lapis(pos, seed)
        if t.side and t.j == t.fh - 1:
            return gold(pos, seed)
        if t.face == "back" and t.i in (0, t.fw - 1):
            return mul(c, 0.85)
        return c
    if p in ("right_tendril", "left_tendril"):
        c = nemes(t, seed)
        return gold(pos, seed) if t.side and t.j >= t.fh - 2 else c
    if p == "brow_band":
        if t.face == "front":
            cx, cy = (t.fw - 1) / 2.0, (t.fh - 1) / 2.0
            if abs(t.i - cx) + abs(t.j - cy) <= 1.6:
                return lapis(pos, seed)
        return gold(pos, seed)
    if p == "uraeus":
        c = gold(pos, seed)
        return SOCKET if t.face == "front" and t.i == 0 and t.j == 3 else c
    if p == "nose":
        return mul(sand(pos, seed), 1.05)
    if p == "beard":
        x, y, z = local
        if t.j >= t.fh - 2 and t.side:
            return gold(pos, seed)
        c = sand(pos, seed)
        return mul(c, 0.78) if (int(math.floor(x + y)) % 3 == 0) else c   # braided cross-hatch
    if p in ("right_pauldron", "left_pauldron"):
        if t.face == "top":
            inside = 2 <= t.i < t.fw - 2 and 2 <= t.j < t.fh - 2
            return lapis(pos, seed) if inside else gold(pos, seed)
        if t.face == "bottom":
            return mul(gold(pos, seed), 0.7)
        if t.side and 2 <= t.j <= 3:
            return lapis(pos, seed)
        return gold(pos, seed)
    if p in ("right_upper_band", "left_upper_band"):
        c = red(pos, seed)
        return mul(c, 0.85) if t.side and t.j in (0, t.fh - 1) else c
    if p in ("right_bracer", "left_bracer"):
        if t.face in ("top", "bottom") or (t.side and t.j in (0, t.fh - 1)):
            return gold(pos, seed)
        if t.side and t.j == 2 and int(math.floor(local[0] + local[2])) % 4 == 0:
            return gold(pos, seed)  # gold studs along the lapis band
        return lapis(pos, seed)
    if p in ("right_fist", "left_fist"):
        c = sand(pos, seed)
        return mul(c, 1.06) if t.face == "front" and t.j <= 2 else c
    if p in ("right_foot", "left_foot"):
        return mul(sand(pos, seed, 0.8), 0.9)
    if p in ("right_ribcage", "left_ribcage"):
        return None  # transparent overlay; only carved lines are drawn on top
    # plain sandstone for head, body, arms, legs
    c = sand(pos, seed, dirt)
    if p == "head" and t.face == "front" and t.j == 5:
        c = mul(c, 0.8)  # shadow under the brow
    return c


def paint_body() -> Image.Image:
    def painter(t):
        key = (t.px, t.py)
        if t.part.name in ("right_ribcage", "left_ribcage"):
            return (*DARK[key], 255) if key in DARK else None
        c = base_color(t)
        c = edge_shade(t, c)
        if key in DARK:
            c = DARK[key]
        return c
    return M.paint(painter)


def paint_glow(enraged: bool) -> Image.Image:
    cyan, cyan_halo = CYAN, mul(CYAN_SOFT, 0.55)
    img = Image.new("RGBA", (M.TEX_W, M.TEX_H), (0, 0, 0, 0))
    px = img.load()

    def put(key, color, alpha=255):
        if 0 <= key[0] < M.TEX_W and 0 <= key[1] < M.TEX_H and not (px[key][3] == 255 and alpha < 255):
            px[key] = (*(int(c) for c in color), alpha)

    for key in HALO:
        put(key, (EMBER if enraged else cyan_halo), 110)
    for key, kind in GLOW.items():
        if kind == "eye":
            put(key, cyan)
        elif kind == "eye_core":
            put(key, AMBER_HOT)
        elif kind == "crack":
            put(key, EMBER_HOT if enraged else cyan)
        elif kind == "core":
            put(key, EMBER if enraged else AMBER)
        elif kind == "core_hot":
            put(key, EMBER_GOLD if enraged else AMBER_HOT)
    if enraged:
        # cracks open: every crack and the core grow by one texel, the core becomes a red-gold blaze
        base = dict(GLOW)
        for (x, y), kind in base.items():
            if kind in ("crack", "core", "core_hot"):
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    k = (x + dx, y + dy)
                    if k not in base and _same_face(k, (x, y)):
                        put(k, EMBER, 235)
        cx, cy = A("body", "front", 8, 11)
        for dy in range(-3, 4):
            for dx in range(-4, 5):
                if abs(dx) / 4.0 + abs(dy) / 3.0 <= 1.0:
                    put((cx + dx, cy + dy), EMBER_GOLD if abs(dx) <= 1 and abs(dy) <= 1 else EMBER_HOT, 255)
        for (i, j) in [(8, 11), (9, 11)]:
            put(A("body", "front", i, j), EMBER_GOLD)
    return img


def _same_face(a, b):
    """True when both atlas pixels belong to the same part face (so a grown crack never leaks into a neighbour)."""
    for (part, face), (u0, v0, fw, fh) in FR.items():
        if u0 <= a[0] < u0 + fw and v0 <= a[1] < v0 + fh:
            return u0 <= b[0] < u0 + fw and v0 <= b[1] < v0 + fh
    return False


# ---------------------------------------------------------------- telegraph textures

def telegraph(fill: bool, fan: bool = False) -> Image.Image:
    """Red floor telegraph plate: a round zone, or (fan) the 120 degree sweep wedge with its apex at the plate centre
    pointing to the top of the image."""
    n = 64
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    c = (n - 1) / 2.0
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - c, y - c)
            if r > 31.5:
                continue
            if fan and abs(math.degrees(math.atan2(x - c, c - y))) > 60.0:
                continue
            if fill:
                px[x, y] = (178, 14, 14, 235) if r < 30 else (120, 8, 8, 255)
            else:
                ring = r >= 29
                px[x, y] = (255, 36, 28, 235) if ring else (255, 72, 60, 120)
    return img


# ---------------------------------------------------------------- java geometry

def f(v):
    s = f"{float(v):.1f}" if float(v).is_integer() else f"{float(v)}"
    return s + "F"


def java_geometry() -> str:
    lines = []
    for p in M.PARTS:
        parent = f"p_{p.parent}" if p.parent else "root"
        if p.box:
            x, y, z, w, h, d = p.box
            cube = f"CubeListBuilder.create().texOffs({p.u}, {p.v}).addBox({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)})"
        else:
            cube = "CubeListBuilder.create()"
        pose = "PartPose.ZERO" if p.pose == (0, 0, 0) else f"PartPose.offset({f(p.pose[0])}, {f(p.pose[1])}, {f(p.pose[2])})"
        lines.append(f'        PartDefinition p_{p.name} = {parent}.addOrReplaceChild("{p.name}", {cube}, {pose});')
    body = "\n".join(lines)
    return f"""package dev.itzgamesgd.moreminecrafts.client.boss;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by tools/tomb-guardian-model/generate.py; do not edit by hand.
 *
 * <p>Sandstone colossus layer for the Tomb Guardian. Every vanilla Warden bone keeps its name, parent and pivot
 * (PartPose) exactly as in WardenModel.createBodyLayer, so the vanilla keyframe animations (emerge, walk, look)
 * still drive it. Only the cubes are reshaped and decorative child parts are added.
 */
final class TombGuardianGeometry {{
    static final int TEXTURE_WIDTH = {M.TEX_W};
    static final int TEXTURE_HEIGHT = {M.TEX_H};

    private TombGuardianGeometry() {{}}

    static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }}
}}
"""


# ---------------------------------------------------------------- main

def main():
    preview = None
    if "--preview" in sys.argv:
        preview = Path(sys.argv[sys.argv.index("--preview") + 1])
        preview.mkdir(parents=True, exist_ok=True)
    build_features()
    body = paint_body()
    glow = paint_glow(False)
    glow_enraged = paint_glow(True)
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    body.save(TEX_DIR / "tomb_guardian.png")
    glow.save(TEX_DIR / "tomb_guardian_eyes.png")
    glow_enraged.save(TEX_DIR / "tomb_guardian_eyes_enraged.png")
    telegraph(False).save(ITEM_DIR / "tomb_telegraph_area.png")
    telegraph(True).save(ITEM_DIR / "tomb_telegraph_fill.png")
    telegraph(False, fan=True).save(ITEM_DIR / "tomb_telegraph_fan_area.png")
    telegraph(True, fan=True).save(ITEM_DIR / "tomb_telegraph_fan_fill.png")
    JAVA_OUT.write_text(java_geometry())
    if preview:
        for name, yaw in (("front", 0), ("three_quarter", 35), ("back", 180), ("side", 90)):
            M.render(body, glow, yaw_deg=yaw).save(preview / f"preview_{name}.png")
        M.render(body, glow_enraged, yaw_deg=0).save(preview / "preview_enraged_front.png")
        body.resize((M.TEX_W * 3, M.TEX_H * 3), Image.NEAREST).save(preview / "atlas_body.png")
        glow.resize((M.TEX_W * 3, M.TEX_H * 3), Image.NEAREST).save(preview / "atlas_glow.png")
    print("generated", len(M.PARTS), "parts;", sum(1 for p in M.PARTS if p.box), "cubes")


if __name__ == "__main__":
    main()
