"""Generates the 128x128 Fallen Sentinel texture (P2-F1, approved concept D "Blood Seraph").

Run: python3 -I scripts/fallen-sentinel-validation/texture.py <output.png>

The UV atlas below MUST match FallenSentinelRenderer.createLayer: every entry in BOXES is (u, v, w, h, d) for one box
UV region, using the vanilla box layout (top/bottom strip of height d, then east | north | west | south faces of height
h). box() asserts that no two regions overlap and that all of them stay inside the 128x128 atlas. Regions that several
boxes share (feathers of equal length, halo segments, mirrored left limbs) are painted once.

Design rules (SKY-MOB authority): fully closed helm with no eyes, eye holes or glowing points; black-steel heavy plate
with dark-gold trim; crimson cloth and crimson corruption cracks (no purple); black layered feathers fading to dark red
at the tips; a black/crimson segmented halo. No emissive texture. Entity shading darkens texture colours by roughly
0.75, so "black" steel is painted as a dark grey-violet that still reads after lighting.
"""
import random
import sys
from PIL import Image

W = H = 128
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
rng = random.Random(1906)

# ---- palette ---------------------------------------------------------------------------------------------------
ST_D, ST, ST_L, ST_H = (30, 28, 34), (50, 47, 56), (70, 66, 78), (96, 90, 104)   # black steel shade/base/light/edge
MAIL, MAIL_D = (40, 37, 44), (24, 22, 27)                                         # mail / under-armour
GD_D, GD, GD_L, GD_H = (112, 74, 30), (170, 120, 48), (208, 160, 74), (236, 198, 112)  # dark gold
CR_D, CR, CR_L, CR_H = (70, 10, 20), (118, 16, 32), (160, 24, 42), (206, 40, 56)  # crimson cloth / crack / halo
FE_K, FE_D = (20, 17, 22), (32, 26, 32)                                           # feather black
FE_R1, FE_R2, FE_R3 = (58, 16, 26), (96, 18, 32), (140, 24, 40)                   # feather red gradient


def jitter(c, amount=6):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + d)) for v in c)


def P(x, y, c):
    img.putpixel((x, y), c if len(c) == 4 else c + (255,))


class Face:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    def px(self, i, j, c):
        if 0 <= i < self.w and 0 <= j < self.h:
            P(self.x + i, self.y + j, c)

    def rect(self, i, j, w, h, c):
        for a in range(w):
            for b in range(h):
                self.px(i + a, j + b, c)

    def fill(self, c, noise=0):
        for a in range(self.w):
            for b in range(self.h):
                self.px(a, b, jitter(c, noise) if noise else c)

    def row(self, j, c):
        self.rect(0, j, self.w, 1, c)

    def col(self, i, c):
        self.rect(i, 0, 1, self.h, c)


_regions = []


def box(name, u, v, w, h, d):
    """Registers one box UV region and returns its faces (top, bottom, east, north, west, south)."""
    rw, rh = 2 * (w + d), d + h
    assert u >= 0 and v >= 0 and u + rw <= W and v + rh <= H, f"{name} outside atlas"
    for other, (ou, ov, orw, orh) in _regions:
        if u < ou + orw and ou < u + rw and v < ov + orh and ov < v + rh:
            raise AssertionError(f"{name} overlaps {other}")
    _regions.append((name, (u, v, rw, rh)))
    return {
        "top": Face(u + d, v, w, d), "bottom": Face(u + d + w, v, w, d),
        "east": Face(u, v + d, d, h), "north": Face(u + d, v + d, w, h),
        "west": Face(u + d + w, v + d, d, h), "south": Face(u + 2 * d + w, v + d, w, h),
    }


# ---- the UV table (keep in sync with FallenSentinelRenderer) --------------------------------------------------------
BOXES = {
    # head / helm
    "head": (0, 0, 8, 8, 8), "crest": (32, 0, 1, 2, 9), "faceplate": (52, 0, 6, 6, 1),
    "cheek": (66, 0, 1, 5, 4), "brow": (76, 0, 9, 2, 1), "neck_guard": (76, 4, 8, 3, 1),
    "halo": (96, 0, 3, 1, 1), "halo_short": (96, 3, 2, 1, 1),
    # vanilla humanoid limbs (left limbs mirror right)
    "leg": (0, 16, 4, 12, 4), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4),
    # torso armour and cloth
    "breastplate": (56, 16, 9, 7, 1), "fauld": (56, 25, 8, 2, 1), "backplate": (76, 16, 9, 9, 1),
    "collar": (96, 16, 9, 2, 5), "belt": (96, 24, 9, 2, 5), "tabard": (0, 44, 5, 10, 1),
    # arm and leg armour
    "pauldron": (0, 32, 6, 3, 6), "pauldron_lower": (24, 32, 5, 2, 5), "pauldron_top": (44, 32, 4, 2, 4),
    "vambrace": (60, 32, 5, 5, 5), "thigh": (80, 32, 5, 4, 5), "knee": (100, 32, 4, 2, 1),
    "sabaton": (100, 36, 5, 2, 5),
    # wings
    "wing_arm": (16, 48, 10, 2, 3), "wing_root": (44, 48, 4, 4, 3),
}
FEATHER_LENGTHS = (15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5)
for k, length in enumerate(FEATHER_LENGTHS):
    BOXES[f"feather_{length}"] = (8 * k, 64, 3, length, 1)
for k, length in enumerate((7, 6)):
    BOXES[f"plume_{length}"] = (88 + 8 * k, 64, 3, length, 1)

F = {name: box(name, *spec) for name, spec in BOXES.items()}


def plate(face, base=ST, noise=5):
    """Black steel plate with a lit top edge and a shaded bottom edge."""
    face.fill(base, noise)
    if face.h > 2:
        face.row(0, ST_L)
        face.row(face.h - 1, ST_D)


def crack(face, points, color=CR):
    for i, j in points:
        face.px(i, j, color)


# ---- helm ------------------------------------------------------------------------------------------------------------
for side, f in F["head"].items():
    plate(f, ST)
F["head"]["north"].fill(ST_D, 3)                 # closed visor recess: plain dark plate, no eye marks
F["head"]["north"].row(0, ST_L)
for f in F["crest"].values():
    f.fill(ST_L, 4)
F["crest"]["top"].fill(GD_L)
for side in ("east", "west"):
    F["crest"][side].row(0, GD)
    F["crest"][side].row(1, ST)
F["crest"]["north"].fill(GD)
fp = F["faceplate"]["north"]                     # solid faceplate with a single vertical gold rib (reads as a helm)
fp.fill(ST, 4)
fp.row(0, ST_H)
fp.col(0, ST_L)
fp.col(5, ST_D)
fp.rect(2, 0, 2, 6, GD)
fp.rect(2, 0, 1, 6, GD_L)
fp.row(5, ST_D)
fp.px(2, 5, GD_D)
fp.px(3, 5, GD_D)
for side in ("east", "west", "top", "bottom", "south"):
    F["faceplate"][side].fill(ST_D)
F["faceplate"]["top"].fill(GD)
for f in F["cheek"].values():
    plate(f, ST)
for side in ("east", "west"):
    F["cheek"][side].col(0, GD)                  # gold front edge on the cheek guards
    F["cheek"][side].row(4, GD_D)
for f in F["brow"].values():
    f.fill(GD, 3)
F["brow"]["north"].row(0, GD_H)
F["brow"]["north"].rect(4, 1, 1, 1, CR_H)         # small crimson gem at the brow centre
F["brow"]["north"].rect(3, 1, 3, 1, GD_L)
F["brow"]["north"].px(4, 1, CR_H)
for f in F["neck_guard"].values():
    plate(f, ST)
F["neck_guard"]["south"].row(2, GD)

# ---- halo (no emissive layer; black core, crimson faces, scarlet top) -------------------------------------------------
for name in ("halo", "halo_short"):
    F[name]["top"].fill(CR_H)
    F[name]["bottom"].fill(CR_D)
    for side in ("north", "south"):
        F[name][side].fill(CR_L)
    for side in ("east", "west"):
        F[name][side].fill((26, 6, 12))            # dark broken ends
F["halo"]["north"].px(1, 0, CR)
F["halo"]["south"].px(1, 0, CR)

# ---- limbs and torso base (mostly hidden under plate) ---------------------------------------------------------------
for f in F["leg"].values():
    plate(f, ST)
for side in ("north", "east", "west", "south"):
    F["leg"][side].rect(0, 6, F["leg"][side].w, 4, MAIL)     # greave gap
    F["leg"][side].row(6, ST_D)
for f in F["arm"].values():
    plate(f, MAIL)
for side in ("north", "east", "west", "south"):
    F["arm"][side].rect(0, 10, F["arm"][side].w, 2, ST_D)    # gauntlet
F["arm"]["bottom"].fill(ST_D)
for f in F["body"].values():
    plate(f, MAIL)

bp = F["breastplate"]["north"]                    # overlapping breastplate with a gold chevron and crimson cracks
bp.fill(ST, 5)
bp.row(0, GD)
bp.col(0, ST_L)
bp.col(8, ST_D)
for k in range(4):
    bp.px(k, 1 + k, GD_L)
    bp.px(8 - k, 1 + k, GD_L)
    bp.px(k, 2 + k, GD_D)
    bp.px(8 - k, 2 + k, GD_D)
bp.px(4, 5, GD_H)
bp.px(4, 6, GD)
crack(bp, [(2, 4), (1, 5), (2, 6), (6, 5), (7, 6)])
for side in ("east", "west", "top", "bottom", "south"):
    F["breastplate"][side].fill(ST_D)
F["breastplate"]["top"].fill(GD)
for f in F["fauld"].values():
    f.fill(ST, 4)
F["fauld"]["north"].row(1, GD)
bk = F["backplate"]["south"]
bk.fill(ST, 5)
bk.row(0, GD)
bk.row(8, GD_D)
crack(bk, [(4, 2), (3, 3), (4, 4), (5, 5), (5, 6), (2, 6), (6, 3)])
for side in ("east", "west", "top", "bottom", "north"):
    F["backplate"][side].fill(ST_D)
for f in F["collar"].values():
    f.fill(ST_L, 4)
for side in ("north", "east", "west", "south"):
    F["collar"][side].row(1, GD)
F["collar"]["top"].fill(ST_D)
for f in F["belt"].values():
    f.fill(CR, 5)
    if f.h >= 2:
        f.row(1, CR_D)
F["belt"]["north"].rect(3, 0, 3, 2, GD)           # gold buckle
F["belt"]["north"].px(4, 0, GD_H)
tb = F["tabard"]["north"]                         # crimson tabard with gold border and a gold cross
tb.fill(CR, 5)
tb.col(0, GD_D)
tb.col(4, GD_D)
tb.row(9, GD)
tb.rect(2, 2, 1, 6, GD_L)
tb.rect(1, 3, 3, 1, GD_L)
tb.px(2, 2, GD_H)
for side in ("south", "east", "west", "top", "bottom"):
    F["tabard"][side].fill(CR_D, 4)
F["tabard"]["south"].row(9, GD_D)

# ---- shoulders, arms, legs -----------------------------------------------------------------------------------------
for name in ("pauldron", "pauldron_lower", "pauldron_top"):
    for side, f in F[name].items():
        plate(f, ST)
        if side not in ("top", "bottom"):
            f.row(f.h - 1, GD)                    # gold lower rim on every tier
    F[name]["top"].fill(ST_L, 4)
F["pauldron"]["top"].rect(0, 0, 6, 1, GD_L)
F["pauldron"]["top"].rect(0, 5, 6, 1, GD_D)
crack(F["pauldron"]["north"], [(1, 1), (2, 1)])
crack(F["pauldron"]["south"], [(3, 1), (4, 0)])
for f in F["pauldron_top"].values():
    f.row(0, GD_L)
for f in F["vambrace"].values():
    plate(f, ST)
for side in ("north", "east", "west", "south"):
    F["vambrace"][side].row(0, GD)
    F["vambrace"][side].row(4, GD_D)
for f in F["thigh"].values():
    plate(f, ST)
for side in ("north", "east", "west", "south"):
    F["thigh"][side].row(3, GD)
crack(F["thigh"]["north"], [(1, 1), (2, 2)])
for f in F["knee"].values():
    f.fill(GD, 3)
F["knee"]["north"].row(0, GD_H)
for f in F["sabaton"].values():
    plate(f, ST)
F["sabaton"]["north"].row(0, GD)
F["sabaton"]["bottom"].fill(ST_D)

# ---- wings: black feathers fading to dark red tips, notched cutout ends ---------------------------------------------
for f in F["wing_arm"].values():
    f.fill(FE_K, 3)
for side in ("north", "south"):
    F["wing_arm"][side].row(0, FE_D)
    F["wing_arm"][side].row(1, FE_R1)
for f in F["wing_root"].values():
    f.fill(FE_K, 3)


def feather(faces, length, tip_at_bottom=True):
    for side, f in faces.items():
        if side in ("top", "bottom"):
            f.fill(FE_K if (side == "top") == tip_at_bottom else FE_R2)
            continue
        for j in range(f.h):
            t = j / max(1, f.h - 1) if tip_at_bottom else 1 - j / max(1, f.h - 1)
            c = FE_K if t < 0.6 else FE_D if t < 0.72 else FE_R1 if t < 0.84 else FE_R2 if t < 0.94 else FE_R3
            for i in range(f.w):
                f.px(i, j, jitter(c, 3))
        if side in ("north", "south"):
            shaft = (44, 36, 42) if length > 6 else FE_D
            for j in range(f.h - 2):
                f.px(1, j + (0 if tip_at_bottom else 2), shaft)
            tip = f.h - 1 if tip_at_bottom else 0
            f.px(0, tip, (0, 0, 0, 0))            # notched, slightly pointed feather tip
            f.px(f.w - 1, tip, (0, 0, 0, 0))
        else:
            tip = f.h - 1 if tip_at_bottom else 0
            f.px(0, tip, (0, 0, 0, 0))


for length in FEATHER_LENGTHS:
    feather(F[f"feather_{length}"], length)
for length in (7, 6):
    feather(F[f"plume_{length}"], length, tip_at_bottom=False)

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "fallen_sentinel.png"
    img.save(out)
    print(f"wrote {out} with {len(_regions)} UV regions, no overlaps")
