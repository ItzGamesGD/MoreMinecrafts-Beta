"""Generates the four pyramid mob variant textures (adult + baby) on the exact vanilla 26.3 UV layouts.

Run: python3 tools/pyramid-mob-textures/generate.py [--preview DIR]
Output: src/main/resources/assets/moreminecrafts/textures/entity/pyramid/*.png

Every texel is painted from its 3D face position (see uvmodel.Texel), so wrappings, collars and bands
continue around each part. No vanilla texture pixels are read or copied.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from uvmodel import adult_humanoid, baby_zombie, skeleton, paint, render, uv_mask

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/moreminecrafts/textures/entity/pyramid"


# ---------------------------------------------------------------- helpers

def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[k:k + 2], 16) for k in (0, 2, 4))


def noise(*a) -> float:
    h = 0x9E3779B1
    for x in a:
        h ^= (int(x) + 0x7F4A7C15 + ((h << 6) & 0xFFFFFFFF) + (h >> 2)) & 0xFFFFFFFF
        h = (h * 0x85EBCA6B) & 0xFFFFFFFF
        h ^= h >> 13
    return (h & 0xFFFFFF) / float(0x1000000)


def pick(ramp, t):
    t = min(0.999, max(0.0, t))
    return ramp[int(t * len(ramp))]


def step(ramp, idx):
    return ramp[max(0, min(len(ramp) - 1, idx))]


def mul(c, f):
    return tuple(max(0, min(255, round(x * f))) for x in c[:3])


def mix(a, b, t):
    return tuple(round(a[k] + (b[k] - a[k]) * t) for k in range(3))


def front_col(t):
    """Column on the front face, or None."""
    return t.i if t.face == "front" else None


def vpos(t):
    """Vertical position 0..1 down a side face."""
    return (t.j + 0.5) / t.fh


def is_hand(t):
    return "arm" in t.box.part and t.face == "bottom"


# ---------------------------------------------------------------- palettes

LINEN = [hx(c) for c in ("6b5636", "8f7a52", "b09a6c", "c8b388", "dccaa0", "eadcb6")]
SAND = [hx(c) for c in ("7a5f34", "9c7c45", "b89656", "cdb06c")]
DRY_SKIN = [hx(c) for c in ("2e2016", "40301f", "54402b")]

DARK_WRAP = [hx(c) for c in ("17110d", "221a14", "2e241c", "3b2f25", "4b3c2f", "5d4b3a")]
GOLD = [hx(c) for c in ("5e3f0c", "8a6316", "b48a22", "d8ae34", "f0d060", "fff0a8")]
CARNELIAN = [hx(c) for c in ("5e1a10", "8e2c1a", "b4452a")]
TURQUOISE = [hx(c) for c in ("1e5a56", "2f8a7e", "52b5a2")]
KOHL = hx("161a2c")

BONE = [hx(c) for c in ("1d1814", "2c241e", "3d332b", "504439", "65574a", "7b6b5b", "8f7f6d")]
LAPIS = [hx(c) for c in ("101d4a", "182d6e", "21409a", "3058bb", "4b77d1")]
GOLD_FLECK = hx("e8c35a")

ASH_SKIN = [hx(c) for c in ("3a3732", "4c4842", "5f5a52", "736d63", "878073", "999283")]
ROBE = [hx(c) for c in ("16223d", "1f3157", "2a4372", "36558a", "4b6b9d", "6a86ab", "8aa0b8")]
TRIM = [hx(c) for c in ("4f3a17", "765a24", "9c7a33", "bd9a48")]
WHITE_LINEN = [hx(c) for c in ("8c8470", "aea58c", "cbc3a6", "ddd6bb")]
LEATHER = [hx(c) for c in ("2c1d12", "45301d", "5f4429")]


# ---------------------------------------------------------------- shared wrap pattern

def wrap_band(t, band_h, slope, phase=0.0):
    """Helical bandage band for side faces; returns (band index, row inside band)."""
    r = t.j + phase + slope * band_h * t.s / t.perimeter
    band = math.floor(r / band_h)
    return band, r - band * band_h


def top_rings(t):
    cx, cy = (t.fw - 1) / 2, (t.fh - 1) / 2
    return max(abs(t.i - cx), abs(t.j - cy))


MOSS_SKIN = [hx(c) for c in ("1f2e17", "2b3d1f", "37502a", "446234", "52733e")]
NAVY = [hx(c) for c in ("0e1428", "151e3c", "1d2a52")]
SOOT_BONE = [hx(c) for c in ("141110", "1f1b19", "2b2622", "37312c", "443d36", "524a42")]


def ring_dist(t):
    """Distance (in texels, around the part) from the centre of the front face; seamless."""
    c = t.box.d + t.box.w / 2 - 0.5
    d = abs(t.s - c)
    return min(d, t.perimeter - d)


def ring_x(t):
    """Signed position around the ring from the front centre (-P/2..P/2)."""
    c = t.box.d + t.box.w / 2 - 0.5
    d = t.s - c
    P = t.perimeter
    if d > P / 2:
        d -= P
    if d < -P / 2:
        d += P
    return d


# ================================================================ 1. Buried Mummy

def buried_mummy(t, baby):
    """Sand-bleached wrappings: mottled linen with darker chevron and diagonal wraps; wrapped face."""
    b, part = t.box, t.box.part
    if b.layer == "outer":
        return None
    seed = 11
    g = noise(seed, t.px, t.py)
    blotch = noise(seed, t.px // 2, t.py // 2, 3)
    if part == "head" and t.face == "front":
        n = t.fw
        eye_row = 3 if n == 8 else 2
        eyes = (1, 2, 5, 6) if n == 8 else (1, 4)
        if t.j == eye_row and t.i in eyes:
            return hx("120c08")
        if t.j == eye_row and n == 8 and t.i in (0, 3, 4, 7):
            return step(SAND, 0)  # shadowed gap between the face wraps
        if t.j in (eye_row - 1, eye_row + 1) and t.i in eyes:
            return step(SAND, 1)
    if t.side:
        # wraps: chevrons that meet at the front/back centre lines, sloping diagonally over the sides
        # integer / half slopes give clean pixel-art diagonals instead of a stair-step checker
        slope = {"head": 0.5, "body": 0.5, "right_arm": 0.5, "left_arm": 0.5,
                 "right_leg": -0.5, "left_leg": -0.5}[part]
        band_h = 4 if not baby else 3
        if part == "head" and t.face == "front":
            r = t.j + 0.5
        else:
            r = t.j + math.floor(slope * math.floor(ring_dist(t))) + {"head": 0, "body": 1, "right_arm": 2,
                "left_arm": 2, "right_leg": 0, "left_leg": 0}[part]
        band = math.floor(r / band_h)
        rr = r - band * band_h
        if rr < 0.9:
            c = step(SAND, 1 if blotch < 0.7 else 0)  # brown seam where one wrap overlaps the next
        elif rr < 1.9 and band_h == 4:
            c = step(LINEN, 2)  # shadow under the overlapping wrap
        elif rr < band_h - 1:
            c = step(LINEN, 4)  # lit face of the wrap
        else:
            c = step(LINEN, 3)
        # broad dusty mottling (2x2 texel cells) rather than per-pixel noise
        if rr >= 0.9:
            if blotch > 0.74:
                c = mix(c, step(SAND, 2), 0.6)
            elif blotch < 0.16:
                c = step(LINEN, 5)  # sun-bleached patch
        if "leg" in part and t.j >= t.fh - (2 if not baby else 1):
            c = mix(c, step(DRY_SKIN, 2), 0.55)  # sand-darkened feet
        return mul(c, 1.04 - 0.1 * vpos(t))
    if t.face == "top":
        ring = int(top_rings(t))
        return step(LINEN, 4 if ring % 2 == 0 else 2) if part == "head" else step(LINEN, 3)
    if is_hand(t):
        return step(SAND, 1) if t.j % 2 == 0 else step(LINEN, 3)  # wrapped fingers
    if "leg" in part:
        return step(DRY_SKIN, 1)
    return step(LINEN, 1)


# ================================================================ 2. Crypt Archer (skeleton 64x32)

def crypt_archer(t, baby=False):
    """Soot-dark bones, lapis-set gold crown, gold-and-lapis collar and forearm bands."""
    b, part = t.box, t.box.part
    if b.layer == "outer":
        return archer_crown(t)
    seed = 23
    g = noise(seed, t.px, t.py)
    bone = lambda k: step(SOOT_BONE, k + (g > 0.75) - (g < 0.15))
    if part == "head":
        if t.face == "front":
            i, j = t.i, t.j
            if j == 3 and i in (1, 2, 5, 6):
                return hx("050404")
            if j == 4 and i in (1, 2, 5, 6):
                return hx("050404") if i in (2, 5) else step(SOOT_BONE, 1)
            if j == 5 and i in (3, 4):
                return hx("050404")  # nasal cavity
            if j == 6 and 1 <= i <= 6:
                return hx("050404")  # open grin
            if j == 7 and 1 <= i <= 6:
                return step(SOOT_BONE, 4) if i % 2 else step(SOOT_BONE, 2)  # teeth
            return mul(bone(4), 1.02 - 0.06 * vpos(t))
        if t.face == "top":
            return bone(4)
        if t.face == "bottom":
            return bone(1)
        return mul(bone(3), 1.03 - 0.08 * vpos(t))
    if part == "body":
        return archer_body(t, g)
    if "arm" in part:
        if t.face == "top":
            return bone(3)
        if t.face == "bottom":
            return bone(2)
        if t.j in (7, 8, 9):  # forearm bracer: gold edges, lapis band
            return step(GOLD, 4 if t.j == 7 else 2) if t.j != 8 else step(LAPIS, 3 if t.s % 2 else 2)
        c = bone(3) if t.face in ("front", "back") and t.i == 1 else bone(4)
        return mul(c, 1.04 - 0.1 * vpos(t))
    if "leg" in part:
        if t.face in ("top", "bottom"):
            return bone(2)
        c = bone(5) if t.j == 5 else (bone(3) if t.face in ("front", "back") and t.i == 1 else bone(4))
        return mul(c, 1.04 - 0.1 * vpos(t))
    return bone(3)


def archer_body(t, g):
    j = t.j
    if t.face == "top":
        return step(GOLD, 3) if t.j >= t.fh - 1 else step(SOOT_BONE, 3)
    if t.face == "bottom":
        return step(SOOT_BONE, 2)
    x = ring_x(t)
    # collar necklace: gold row, lapis beads, then gold drops that narrow toward the sternum
    if j == 0:
        return step(GOLD, 4)
    if j == 1:
        return step(LAPIS, 3) if int(x) % 2 == 0 else step(GOLD, 3)
    if j == 2 and t.face in ("front", "back") and abs(x) <= 2.5:
        return step(LAPIS, 2) if abs(x) < 1 else step(GOLD, 3)
    if j == 3 and t.face == "front" and abs(x) < 1:
        return step(GOLD, 4)  # pendant
    bone = lambda k: step(SOOT_BONE, k + (g > 0.7))
    if t.face in ("front", "back"):
        if t.i in (3, 4):
            return bone(3 if t.face == "front" else 2)  # sternum / spine
        if j in (3, 5, 7) and 1 <= t.i <= 6:
            return bone(4 if t.face == "front" else 3)
        if j >= 9 and 1 <= t.i <= 6 and j != 10:
            return bone(3)  # pelvis
        return None
    if j in (3, 5, 7):
        return bone(3)
    if j in (9, 11) and t.i in (1, 2):
        return bone(2)
    return None


def archer_crown(t):
    """Gold crown on the head overlay: raised corner points, lapis stones, open top."""
    n = t.fw
    if t.face == "top":
        edge = t.i in (0, n - 1) or t.j in (0, n - 1)
        return step(GOLD, 3) if edge else None
    if t.face == "bottom":
        return None
    if t.j == 0:
        if t.i in (0, n - 1):
            return step(GOLD, 5)  # corner points
        if t.face == "front" and t.i in (3, 4):
            return step(LAPIS, 4)  # central stone rising above the band
        return None
    if t.j == 1:
        if t.face == "front" and t.i in (2, 3, 4, 5):
            return step(LAPIS, 3 if t.i in (3, 4) else 2)
        if t.face != "front" and t.i in (2, 5):
            return step(LAPIS, 3)
        return step(GOLD, 4)
    if t.j == 2:
        return step(GOLD, 2)
    return None


# ================================================================ 3. Funeral Servant (zombie)

def funeral_servant(t, baby):
    """Dark-green undead in a worn ceremonial blue robe with tarnished gold edging."""
    b, part = t.box, t.box.part
    if b.layer == "outer":
        return None
    seed = 37
    g = noise(seed, t.px, t.py)
    skin = lambda k: step(MOSS_SKIN, k + (g > 0.75) - (g < 0.15))
    if part == "head":
        n = t.fw
        if t.face == "front":
            eye_r = 4 if n == 8 else 3
            eyes = (1, 2, 5, 6) if n == 8 else (1, 4)
            if t.j == eye_r and t.i in eyes:
                return hx("0d100a")
            if t.j == eye_r - 1 and t.i in eyes:
                return step(MOSS_SKIN, 1)
            if n == 8 and t.j == 6 and t.i in (3, 4):
                return step(MOSS_SKIN, 0)
            return mul(skin(3), 1.03 - 0.08 * vpos(t))
        if t.face == "bottom":
            return skin(1)
        return mul(skin(3 if t.face != "back" else 2), 1.03 - 0.08 * vpos(t))
    if part == "body":
        return servant_robe(t, baby, g)
    if "arm" in part:
        cuff = 8 if not baby else 3
        if t.face == "top":
            return step(ROBE, 3)
        if t.face == "bottom":
            return skin(2)
        if t.j < cuff:
            c = step(ROBE, 3 + (noise(seed, t.s, t.j // 3) > 0.7) - (t.s % 4 == 0))
            return mul(c, 1.03 - 0.08 * vpos(t))
        if t.j == cuff:
            return step(GOLD, 3 if t.s % 2 else 2)
        return mul(skin(3), 1.02 - 0.08 * vpos(t))
    if "leg" in part:
        return servant_leg(t, baby, g)
    return skin(2)


def robe_tone(t, row_div=3):
    fade = noise(41, t.s // 2, t.j // row_div)
    c = step(ROBE, 3 + (fade > 0.8) - (t.s % 3 == 0))
    if noise(43, t.s, t.j) > 0.975:
        c = step(ROBE, 5)  # threadbare spot
    return c


def servant_robe(t, baby, g):
    j = t.j
    if t.face == "top":
        return step(ROBE, 3)
    if t.face == "bottom":
        return step(NAVY, 1)
    if t.face == "front":
        inner = (3, 4) if not baby else (1, 2)
        edge = (2, 5) if not baby else (0, 3)
        if j == 0 and t.i in inner:
            return step(MOSS_SKIN, 3)  # neck
        if t.i in inner:
            return step(NAVY, 1 + (j % 4 == 0))  # dark under-robe between the open fronts
        if t.i in edge:
            return step(GOLD, 3 if j % 2 else 2)  # gold edging down both front panels
    if t.face == "back":
        mid = t.fw / 2 - 0.5
        if j == (1 if not baby else 0) and abs(t.i - mid) <= (2 if not baby else 1):
            return step(GOLD, 3)  # yoke trim at the back of the neck
    if j == 0:
        return step(ROBE, 4)
    return mul(robe_tone(t), 1.03 - 0.06 * vpos(t))


def servant_leg(t, baby, g):
    """The robe continues over the legs; inner front edge keeps the gold trim; dark boots."""
    if t.face == "top":
        return step(ROBE, 3)
    if t.face == "bottom":
        return step(NAVY, 0)
    hem = t.fh - (3 if not baby else 1)
    boot = t.fh - (1 if not baby else 1)
    x = ring_x(t)
    if t.j >= boot:
        return step(SOOT_BONE, 2 + (t.s % 2))
    if t.j == hem:
        return step(GOLD, 3)
    if t.j > hem:
        return step(NAVY, 1)
    # the right leg's inner edge is +x side, the left leg is mirrored by the model, so trim the
    # inner front column of the shared right-leg UV
    if t.face == "front":
        if t.i == t.fw - 1:
            return step(NAVY, 1 + (t.j % 3 == 0))
        if t.i == t.fw - 2:
            return step(GOLD, 3 if t.j % 2 else 2)
    return mul(robe_tone(t), 1.0 - 0.06 * vpos(t))


# ================================================================ 4. Gilded Mummy

def gilded_mummy(t, baby):
    """Dark bandages, gold death mask, gold-and-lapis nemes, collar and royal gold bands."""
    b, part = t.box, t.box.part
    if b.layer == "outer":
        return gilded_nemes(t, baby)
    seed = 71
    g = noise(seed, t.px, t.py)
    if part == "head":
        return gilded_mask(t, baby, g)
    if t.face == "top":
        if part == "body":
            return step(GOLD, 4) if t.j % 2 else step(LAPIS, 3)
        return step(DARK_WRAP, 3)
    if t.side:
        j = t.j
        x = ring_x(t)
        if part == "body":
            c = gilded_body(t, baby, x)
            if c is not None:
                return c
        if "arm" in part:
            band = (3, 4, 5) if not baby else (1,)
            if j in band:
                if not baby and j == 4 and t.s % 2 == 0:
                    return step(DARK_WRAP, 0)  # inset marks on the armlet
                return step(GOLD, 4 if j != band[-1] else 2)
            if j >= t.fh - (2 if not baby else 1):
                return step(GOLD, 3 if (t.s + j) % 2 else 2)  # gold wrist bands
        if "leg" in part:
            # gold bands at the thigh and shin, and an inner strip forming a V with the other leg
            if (not baby and j in (2, 8)) or (baby and j == 1):
                return step(GOLD, 4 if j != 8 else 3)
            if not baby and t.face == "front" and t.i == t.fw - 1 and j < 8:
                return step(GOLD, 3)  # inner gold strip forming a V with the other leg
            if j == t.fh - 1:
                return step(DARK_WRAP, 0)
        # tight horizontal bandages
        tone = 3 if j % 2 == 0 else 2
        if j % 4 == 3:
            tone = 1
        tone += (g > 0.8) - (g < 0.1)
        return mul(step(DARK_WRAP, tone), 1.05 - 0.1 * vpos(t))
    if is_hand(t):
        return step(GOLD, 4) if t.i % 2 == 0 else step(GOLD, 2)  # gold finger stalls
    return step(DARK_WRAP, 1)


def gilded_body(t, baby, x):
    j = t.j
    collar = 4 if not baby else 2
    if j < collar:
        # broad collar of alternating gold and lapis rows, wider at the front
        if t.face in ("right", "left") and j >= collar - 1:
            return None
        return step(GOLD, 4) if j % 2 == 0 else step(LAPIS, 2 + (int(x) % 2 == 0))
    belt = 7 if not baby else 3
    if j == belt:
        return step(GOLD, 4 if int(x) % 3 else 2)
    if t.face == "front":
        if not baby and j > belt and abs(abs(x) - (4.5 - (j - belt))) < 0.6:
            return step(GOLD, 3)  # gold V sash below the belt
        if not baby and j < belt and abs(x) < 1 and j % 2 == 0:
            return step(GOLD, 3)  # small gold pectoral studs
    return None


def gilded_mask(t, baby, g):
    n = t.fw
    G = lambda k: step(GOLD, k)
    if t.face == "front":
        i, j = t.i, t.j
        if n == 8:
            if j == 0:
                return G(3)
            if j == 2 and i in (1, 2, 5, 6):
                return step(LAPIS, 1)  # brows
            if j == 3:
                if i in (1, 6):
                    return step(LAPIS, 1)  # kohl wings
                if i in (2, 5):
                    return hx("0e0a06")  # dark eyes
            if i in (3, 4) and j in (3, 4):
                return G(5) if i == 3 else G(4)  # nose
            if j == 5 and i in (3, 4):
                return G(2)
            if j == 6 and i in (3, 4):
                return G(1)  # closed lips
            if i in (0, 7):
                return G(2)
            return G(4 if (i * 3 + j) % 7 else 5)
        if j == 2 and i in (1, 4):
            return hx("0e0a06")
        if j == 1 and i in (1, 4):
            return step(LAPIS, 1)
        if j == 4 and i in (2, 3):
            return G(1)
        if i in (2, 3) and j in (2, 3):
            return G(5) if i == 2 else G(4)
        return G(2) if i in (0, 5) else G(4)
    if t.face == "bottom":
        if n == 8 and t.i in (3, 4) and t.j >= 5:
            return step(GOLD, 3 if t.j % 2 else 2)  # false beard
        return step(GOLD, 2)
    return step(GOLD, 2)


def gilded_nemes(t, baby):
    """Gold-and-lapis striped nemes; the mask face stays open; gold brow band with a uraeus."""
    n = t.fw
    stripe = lambda k: step(GOLD, 4) if k % 2 == 0 else step(LAPIS, 2)
    if t.face == "front":
        if t.j == 0:
            mid = (n // 2 - 1, n // 2)
            if t.i in mid:
                return step(LAPIS, 3) if t.i == mid[0] else step(GOLD, 5)
            return step(GOLD, 5)
        if n == 8 and t.j == 1 and t.i in (3, 4):
            return step(GOLD, 3)  # uraeus body
        if t.i in (0, n - 1):
            return stripe(t.j)  # lappets framing the mask
        return None
    if t.face == "bottom":
        return None
    return stripe(t.j)  # top, sides and back


# ---------------------------------------------------------------- build

VARIANTS = {
    "buried_mummy": (buried_mummy, True),
    "crypt_archer": (crypt_archer, False),
    "funeral_servant": (funeral_servant, True),
    "gilded_mummy": (gilded_mummy, True),
}


def build():
    images = {}
    for name, (painter, has_baby) in VARIANTS.items():
        if name == "crypt_archer":
            images[name] = (skeleton(), paint(skeleton(), lambda t: painter(t, False)))
            continue
        adult = adult_humanoid(True)
        images[name] = (adult, paint(adult, lambda t, p=painter: p(t, False)))
        if has_baby:
            baby = baby_zombie()
            images[name + "_baby"] = (baby, paint(baby, lambda t, p=painter: p(t, True)))
    return images


def main(argv):
    images = build()
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (model, img) in images.items():
        allowed = uv_mask(model)
        for x in range(img.width):
            for y in range(img.height):
                if img.getpixel((x, y))[3] and (x, y) not in allowed:
                    raise SystemExit(f"{name}: pixel {x},{y} outside the vanilla UV layout")
        img.save(OUT / f"{name}.png", optimize=True)
    if "--preview" in argv:
        from PIL import Image
        d = Path(argv[argv.index("--preview") + 1])
        d.mkdir(parents=True, exist_ok=True)
        for name, (model, img) in images.items():
            sc = 9 if "baby" not in name else 20
            row = [render(model, img, yaw_deg=y, pitch_deg=10, scale=sc, size=(320, 400)) for y in (30, -40, 180)]
            sheet = Image.new("RGBA", (320 * 3, 400))
            for c, im in enumerate(row):
                sheet.paste(im, (c * 320, 0))
            sheet.save(d / f"preview_{name}.png")
            img.resize((img.width * 8, img.height * 8), Image.NEAREST).save(d / f"flat_{name}.png")

if __name__ == "__main__":
    main(sys.argv[1:])
