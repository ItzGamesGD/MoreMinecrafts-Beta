"""Generates the original 64x64 Skyguard Archer texture (P2-C1 refinement, Concept B Celestial Patrol Archer).

Run: python3 -I scripts/skyguard-archer-validation/texture.py <output.png>

The UV atlas below MUST match SkyguardArcherRenderer.createLayer (same u/v/size per part). Every box is registered
through box(), which asserts that no two parts overlap and that everything stays inside 64x64.

Palette note: the Minecraft entity renderer multiplies texture colors by roughly 0.75-0.8 (measured on the first C1
capture: ivory 234,226,208 -> 183,181,172). Texture values are therefore pushed close to full range with warm highlights,
cool shadows and saturated gold/blue instead of nominal "design hex" values.
Light direction is top-left-front on every surface.
"""
import random
import sys
from PIL import Image

# ---- palette ---------------------------------------------------------------------------------------------------
IV, IV_L, IV_W = (255, 251, 240), (255, 255, 252), (255, 242, 218)          # ivory base / highlight / warm tuft
IV_C, IV_S, IV_D = (234, 236, 250), (208, 210, 232), (176, 180, 214)        # cool shadow / fold / deep fold
GD, GD_L, GD_H = (255, 208, 88), (255, 236, 150), (255, 252, 214)           # gold base / light / specular
GD_S, GD_D, GD_X = (224, 150, 42), (160, 98, 26), (104, 62, 22)             # gold shade / recess / outline
SK, SK_L, SK_S = (112, 196, 255), (176, 230, 255), (68, 144, 232)           # sky blue base / light / shade
NV, NV_L, NV_D = (44, 72, 132), (66, 104, 176), (26, 44, 92)                # deep blue accent
BR, BR_L, BR_S, BR_D = (158, 104, 60), (204, 146, 92), (108, 66, 38), (68, 40, 24)  # leather
SKIN, SKIN_S, BLUSH = (255, 228, 204), (238, 194, 168), (250, 198, 184)
HAIR, HAIR_S, BROW = (232, 226, 212), (196, 188, 174), (156, 130, 108)
EYE, EYE_L, EYE_D = (58, 152, 255), (214, 242, 255), (24, 62, 156)
CLEAR = (0, 0, 0, 0)

img = Image.new("RGBA", (64, 64), CLEAR)
_registered = []


def P(x, y, c):
    img.putpixel((x, y), c if len(c) == 4 else c + (255,))


class Face:
    """A rectangular texture region with local (i, j) coordinates; writes are clipped to the region."""

    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    def px(self, i, j, c):
        if 0 <= i < self.w and 0 <= j < self.h:
            P(self.x + i, self.y + j, c)

    def rect(self, i, j, w, h, c):
        for a in range(w):
            for b in range(h):
                self.px(i + a, j + b, c)

    def fill(self, c):
        self.rect(0, 0, self.w, self.h, c)

    def col(self, i, c, j0=0, j1=None):
        self.rect(i, j0, 1, (self.h if j1 is None else j1) - j0, c)

    def row(self, j, c, i0=0, i1=None):
        self.rect(i0, j, (self.w if i1 is None else i1) - i0, 1, c)

    def border(self, c):
        self.row(0, c); self.row(self.h - 1, c); self.col(0, c); self.col(self.w - 1, c)


def box(name, u, v, w, h, d):
    """Registers a cuboid UV block (Minecraft box layout) and returns its six faces."""
    bw, bh = 2 * d + 2 * w, d + h
    assert u >= 0 and v >= 0 and u + bw <= 64 and v + bh <= 64, name
    for other, (ou, ov, ow, oh) in _registered:
        assert u + bw <= ou or ou + ow <= u or v + bh <= ov or ov + oh <= v, f"{name} overlaps {other}"
    _registered.append((name, (u, v, bw, bh)))
    return {
        "top": Face(u + d, v, w, d), "bottom": Face(u + d + w, v, w, d),
        "right": Face(u, v + d, d, h), "front": Face(u + d, v + d, w, h),
        "left": Face(u + d + w, v + d, d, h), "back": Face(u + 2 * d + w, v + d, w, h),
    }


def cloth(f, rng, base=IV, density=9, folds=(), seam=None):
    """Woven ivory cloth: base tone, small clustered warm/cool tufts, soft vertical folds, optional stitched seam."""
    f.fill(base)
    for _ in range(max(1, f.w * f.h // density)):
        x, y = rng.randrange(f.w), rng.randrange(f.h)
        c = rng.choice((IV_W, IV_W, IV_C, IV_L))
        f.px(x, y, c)
        if rng.random() < 0.5:
            f.px(x + 1, y, c)
        else:
            f.px(x, y + 1, c)
    for i in folds:  # light edge, then a shadow line with gaps
        for j in range(f.h):
            f.px(i - 1, j, IV_L if j % 3 else base)
            f.px(i, j, IV_S if j % 4 != 3 else IV_C)
            f.px(i + 1, j, IV_C if j % 2 else base)
    if seam is not None:  # stitched seam: dotted line
        for j in range(0, f.h, 2):
            f.px(seam, j, IV_D)


rng = random.Random(26112026)

# =============================================================================================================
# Fixed player-skin regions
# =============================================================================================================
head = box("head", 0, 0, 8, 8, 8)
hood = box("hood", 32, 0, 8, 8, 8)
rleg = box("leg", 0, 16, 4, 12, 4)
body = box("body", 16, 16, 8, 12, 4)
rarm = box("arm", 40, 16, 4, 12, 4)

# ---- head: silver hair, pale face, bright blue eyes ---------------------------------------------------------
for n in ("top", "right", "left", "back"):
    f = head[n]
    f.fill(HAIR)
    for _ in range(f.w * f.h // 5):  # strand highlights
        f.px(rng.randrange(f.w), rng.randrange(f.h), HAIR_S if rng.random() < 0.6 else IV_L)
head["top"].col(3, HAIR_S); head["top"].col(4, IV_L)  # parting
for n in ("right", "left"):  # ear/cheek skin below the hair line
    f = head[n]
    f.rect(1, 4, f.w - 2, 4, SKIN); f.rect(2, 4, 2, 1, SKIN_S)
head["back"].rect(0, 6, 8, 2, HAIR_S)
head["bottom"].fill(SKIN_S)
f = head["front"]
f.fill(SKIN)
f.row(0, HAIR); f.row(1, HAIR)
for i in (3, 4):
    f.px(i, 1, SKIN)
f.px(2, 1, HAIR_S); f.px(5, 1, HAIR_S)
for j in range(2, 6):
    f.px(0, j, HAIR); f.px(7, j, HAIR)
f.px(1, 2, HAIR_S); f.px(6, 2, HAIR_S)
f.px(1, 3, BROW); f.px(2, 3, BROW); f.px(5, 3, BROW); f.px(6, 3, BROW)           # brows
f.px(1, 4, EYE_L); f.px(2, 4, EYE); f.px(1, 5, EYE); f.px(2, 5, EYE_D)           # left eye (bright blue)
f.px(6, 4, EYE_L); f.px(5, 4, EYE); f.px(6, 5, EYE); f.px(5, 5, EYE_D)           # right eye
f.px(1, 6, BLUSH); f.px(6, 6, BLUSH)
f.px(3, 6, SKIN_S); f.px(4, 6, (226, 150, 142))                                   # nose shade / mouth
f.px(3, 7, SKIN_S); f.px(4, 7, SKIN_S)

# ---- hood (hat layer, inflated 0.75): ivory cowl, open face, raised gold trim handled by separate boxes ---------
f = hood["top"]
cloth(f, rng, folds=(), seam=None)
f.col(3, IV_S); f.col(4, IV_S)
for j in range(0, 8, 2):
    f.px(3, j, IV_D)
f.row(7, GD); f.px(0, 7, GD_S); f.px(7, 7, GD_S)       # front edge of the crown
f = hood["front"]
cloth(f, rng)
f.rect(1, 2, 6, 6, CLEAR)                               # face opening
f.row(1, IV_S, 0, 8); f.row(0, IV_L)
f.col(0, IV_S, 2, 8); f.col(7, IV_S, 2, 8)
f.px(0, 2, GD); f.px(7, 2, GD)
for j in range(3, 8):
    f.px(0, j, GD if j % 2 else GD_L); f.px(7, j, GD if j % 2 else GD_L)
for n in ("right", "left"):
    f = hood[n]
    cloth(f, rng, folds=(2, 5))
    front = 7 if n == "right" else 0
    f.col(front, GD, 0, 8)
    f.col(front + (-1 if n == "right" else 1), IV_L)
    for j in range(1, 8, 2):
        f.px(front, j, GD_L)
    f.row(7, GD_S)
    f.px(3 if n == "right" else 4, 3, GD_S)             # small stitched stud
f = hood["back"]
cloth(f, rng, folds=(2, 5), seam=3)
f.row(7, GD); f.row(6, GD_S, 0, 8)
f = hood["bottom"]
f.fill(IV_S)
f.border(GD_S)

# =============================================================================================================
# Hood add-ons
# =============================================================================================================
hood_back = box("hood_back", 40, 57, 6, 5, 2)
for n, f in hood_back.items():
    cloth(f, rng, folds=(1,) if f.w > 2 else ())
hood_back["back"].row(4, GD_S); hood_back["bottom"].fill(IV_S)
hood_back["front"].row(4, GD_S)

rim_top = box("rim_top", 16, 58, 8, 1, 1)
for n, f in rim_top.items():
    f.fill(GD)
rim_top["front"].fill(GD_L)
for i in range(0, 8, 2):
    rim_top["front"].px(i, 0, GD_H)
rim_top["top"].fill(GD_L); rim_top["bottom"].fill(GD_S)
for n in ("right", "left"):
    rim_top[n].fill(GD_S)

rim_side = box("rim_side", 10, 55, 1, 7, 1)
for n, f in rim_side.items():
    f.fill(GD)
for j in range(7):  # lit upper end, shaded lower end, recess pixels
    rim_side["front"].px(0, j, GD_L if j < 2 else (GD if j < 5 else GD_S))
rim_side["front"].px(0, 3, GD_H)
for n in ("right", "left", "back"):
    rim_side[n].fill(GD_S)
rim_side["bottom"].fill(GD_D)

laurel_c = box("laurel_c", 56, 16, 2, 5, 1)
laurel_s = box("laurel_s", 56, 22, 2, 4, 1)
for lf, h in ((laurel_c, 5), (laurel_s, 4)):
    for n, f in lf.items():
        f.fill(GD_S)
    fr = lf["front"]  # leaf: lit left half, shaded right half, bright tip, dark base, central vein
    fr.col(0, GD_L); fr.col(1, GD)
    fr.px(0, 0, GD_H); fr.px(1, 0, GD_L)
    fr.px(1, h - 2, GD_S); fr.row(h - 1, GD_D)
    fr.px(0, h // 2, GD_H)
    lf["back"].fill(GD); lf["back"].row(h - 1, GD_D)
    lf["top"].fill(GD_H)

# =============================================================================================================
# Torso
# =============================================================================================================
f = body["front"]
cloth(f, rng, folds=(1, 6))
f.col(3, SK_L); f.col(4, SK)                              # sky-blue central tunic panel
for j in range(1, 12, 3):
    f.px(3, j, SK); f.px(4, j, SK_S)
f.col(2, GD); f.col(5, GD_S)                              # gold piping beside the panel
for j in range(0, 12, 2):
    f.px(2, j, GD_L)
f.rect(3, 5, 2, 2, GD); f.px(3, 5, GD_H); f.px(4, 6, GD_S)  # small celestial gem on the panel
for n in ("right", "left"):
    f = body[n]
    cloth(f, rng, folds=(2,))
    f.col(0 if n == "left" else 3, GD_S, 0, 12)
f = body["back"]
cloth(f, rng, folds=(2, 5), seam=3)
f.col(3, SK_S); f.col(4, SK)
body["top"].fill(IV_L); body["bottom"].fill(IV_S)

chest = box("chest", 16, 32, 8, 4, 4)
f = chest["front"]
f.fill(IV)                                                # ivory vest plates with a gold frame and open centre
cloth(f, rng, density=7)
f.rect(3, 1, 2, 3, CLEAR)                                 # gap shows the sky-blue panel beneath
f.row(0, GD); f.row(3, GD_S)
f.col(0, GD); f.col(7, GD_S); f.col(2, GD_L, 1, 3); f.col(5, GD_S, 1, 3)
f.px(1, 1, GD_H); f.px(6, 1, GD_L); f.px(1, 2, IV_S); f.px(6, 2, IV_S)
f.px(2, 0, GD_L); f.px(5, 0, GD_L)
for n in ("right", "left", "back"):
    cloth(chest[n], rng)
    chest[n].row(0, GD); chest[n].row(chest[n].h - 1, GD_S)
chest["top"].fill(IV_L); chest["top"].border(GD)
chest["bottom"].fill(IV_S)

belt = box("belt", 16, 40, 8, 2, 4)
for n, f in belt.items():
    f.fill(BR)
for n in ("front", "right", "left", "back"):
    f = belt[n]
    f.row(0, BR_L); f.row(f.h - 1, BR_S)
    for i in range(1, f.w, 2):
        f.px(i, 0, BR)                                    # stitching
f = belt["front"]
f.rect(3, 0, 2, 2, GD); f.px(3, 0, GD_H); f.px(4, 1, GD_S); f.px(3, 1, GD_L)
f.px(2, 0, BR_D); f.px(5, 0, BR_D); f.px(2, 1, BR_D); f.px(5, 1, BR_D)   # buckle recess
belt["top"].fill(BR_L); belt["bottom"].fill(BR_S)

collar = box("collar", 16, 46, 8, 2, 4)
for n, f in collar.items():
    f.fill(SK)
    for i in range(0, f.w, 3):
        f.col(i, SK_S);
        if i + 1 < f.w:
            f.col(i + 1, SK_L)
f = collar["front"]
f.row(0, SK_L); f.row(1, SK_S)
f.rect(3, 0, 2, 1, GD); f.px(3, 0, GD_H); f.rect(3, 1, 2, 1, GD_S)        # gold clasp
collar["top"].fill(SK_L); collar["top"].border(SK); collar["bottom"].fill(SK_S)

hem = box("hem", 16, 52, 8, 2, 4)
for n in ("front", "right", "left", "back"):
    f = hem[n]
    f.fill(IV); f.row(0, IV_L)
    for i in range(f.w):
        f.px(i, 1, GD if i % 2 == 0 else GD_L)
hem["front"].px(3, 0, SK_L); hem["front"].px(4, 0, SK)
hem["top"].fill(IV_S); hem["bottom"].fill(GD_S)

# =============================================================================================================
# Cape (static): 7 wide centre panel + two 3 wide flaps that flare outwards
# =============================================================================================================
cape = box("cape", 0, 32, 7, 22, 1)
f = cape["back"]                                           # outer face (seen from behind)
for j in range(22):
    shade = SK_L if j < 6 else (SK if j < 14 else SK_S)
    for i in range(7):
        f.px(i, j, shade)
for i in (1, 5):                                           # vertical folds: shadow line + lighter neighbour
    for j in range(22):
        f.px(i, j, SK_S if j < 14 else NV_L)
        f.px(i + 1 if i == 1 else i - 1, j, SK_L if j < 14 else SK)
f.px(3, 0, SK_S)
# gold celestial wing emblem (7 wide), spine and star
emblem = [
    "...H...",
    "G..G..G",
    "GG.G.GG",
    ".GGHGG.",
    "..GGG..",
    "...G...",
    "...S...",
]
for dj, line in enumerate(emblem):
    for i, ch in enumerate(line):
        if ch == "G":
            f.px(i, 4 + dj, GD if (i + dj) % 2 else GD_L)
        elif ch == "H":
            f.px(i, 4 + dj, GD_H)
        elif ch == "S":
            f.px(i, 4 + dj, GD_S)
for j in range(12, 15):
    f.px(3, j, GD_S)
for j in (18, 19, 20):                                     # hem band
    for i in range(7):
        f.px(i, j, GD if (i + j) % 2 else GD_L)
f.row(21, GD_D)
f.row(17, GD_S)
cape["front"].fill(NV_L)                                   # inner face
for i in (1, 5):
    cape["front"].col(i, NV)
for j in (19, 20):
    cape["front"].row(j, GD_S)
cape["front"].row(21, GD_D)
cape["top"].fill(GD_S); cape["bottom"].fill(GD_D)
cape["right"].fill(GD_S); cape["left"].fill(GD_S)

flap = box("flap", 56, 32, 3, 20, 1)
f = flap["back"]                                           # outer edge = last column (mirrors for the other flap)
for j in range(20):
    shade = SK_L if j < 6 else (SK if j < 13 else SK_S)
    for i in range(3):
        f.px(i, j, shade)
for j in range(20):
    f.px(1, j, SK_S if j < 13 else NV_L)
    f.px(2, j, GD if j % 2 else GD_L)                      # gold trim on the outer edge
f.px(0, 9, GD); f.px(0, 10, GD_L)                          # echo of the emblem feathers
f.px(1, 8, GD_S); f.px(0, 8, GD_L)
for j in (16, 17, 18):
    for i in range(3):
        f.px(i, j, GD if (i + j) % 2 else GD_L)
f.row(19, GD_D)
flap["front"].fill(NV_L)
flap["front"].col(1, NV)
flap["front"].row(19, GD_D)
flap["top"].fill(GD_S); flap["bottom"].fill(GD_D)
flap["right"].fill(GD_S); flap["left"].fill(GD_S)

# =============================================================================================================
# Limbs and armour
# =============================================================================================================
f = rarm["front"]
cloth(f, rng, folds=(1,))
for j in range(6, 10):
    f.row(j, SK_S if j % 2 else SK, 0, 4)                  # blue sleeve lining under the bracer
f.rect(0, 10, 4, 2, BR); f.row(10, BR_S, 0, 4); f.px(1, 11, BR_L)
for n in ("right", "left", "back"):
    f = rarm[n]
    cloth(f, rng, folds=(2,))
    for j in range(6, 10):
        f.row(j, SK_S if j % 2 else SK, 0, 4)
    f.rect(0, 10, 4, 2, BR); f.row(10, BR_S, 0, 4); f.px(2, 11, BR_L)
rarm["top"].fill(IV_L); rarm["bottom"].fill(BR_S)

bracer = box("bracer", 40, 32, 4, 4, 4)
for n in ("front", "right", "left", "back"):
    f = bracer[n]
    f.fill(GD); f.row(0, GD_L); f.row(3, GD_S)
    f.px(1, 1, SK_L); f.px(2, 1, SK); f.px(1, 2, SK); f.px(2, 2, SK_S)   # inlaid sky-blue gem band
    f.px(0, 1, GD_H); f.col(3, GD_D, 1, 4)
bracer["top"].fill(GD_L); bracer["bottom"].fill(GD_D)

paul1 = box("paul1", 40, 40, 4, 1, 4)                      # big top plate: gold-framed ivory, sky-blue gem
f = paul1["top"]
f.fill(IV); f.border(GD); f.px(0, 0, GD_H); f.px(3, 0, GD_L); f.px(0, 3, GD_S); f.px(3, 3, GD_D)
f.rect(1, 1, 2, 2, SK); f.px(1, 1, SK_L); f.px(2, 2, SK_S)
for n in ("front", "right", "left", "back"):
    paul1[n].fill(GD_L); paul1[n].px(0, 0, GD_H)
paul1["bottom"].fill(GD_D)
paul2 = box("paul2", 40, 45, 4, 1, 4)                      # middle plate: ivory band
for n in ("front", "right", "left", "back"):
    paul2[n].fill(IV_S)
paul2["front"].px(1, 0, IV_L); paul2["front"].px(2, 0, IV_L)
paul2["top"].fill(IV); paul2["bottom"].fill(IV_D)

cuff = box("cuff", 40, 50, 4, 3, 4)
for n in ("front", "right", "left", "back"):
    f = cuff[n]
    f.fill(BR); f.row(0, GD_L); f.row(1, GD_S); f.row(2, BR_S)
    for i in range(0, 4, 2):
        f.px(i, 2, BR_D)                                   # seam
cuff["front"].px(1, 1, GD_H); cuff["front"].px(2, 2, BR_L)
cuff["top"].fill(GD); cuff["top"].border(GD_L); cuff["bottom"].fill(BR_S)

f = rleg["front"]
f.fill(NV)
f.col(1, NV_L); f.col(3, NV_D)
for j in range(0, 6, 2):
    f.px(2, j, NV_L)
for j in range(6, 12):                                     # boots
    f.row(j, BR if j < 11 else BR_D)
f.row(6, BR_L); f.row(9, BR_S); f.px(1, 8, BR_L); f.px(2, 7, GD_S)
for n in ("right", "left", "back"):
    f = rleg[n]
    f.fill(NV); f.col(1, NV_L); f.col(3, NV_D)
    for j in range(6, 12):
        f.row(j, BR if j < 11 else BR_D)
    f.row(6, BR_L); f.row(9, BR_S)
    f.px(2, 8, BR_S)
rleg["top"].fill(NV); rleg["bottom"].fill(BR_D)

tabard = box("tabard", 0, 55, 4, 6, 1)                     # hangs from each leg; outer edge = column 0 on both legs
f = tabard["front"]
cloth(f, rng, density=7)
f.col(0, GD); f.col(3, SK)
f.px(0, 0, GD_L); f.px(0, 2, GD_L); f.px(0, 4, GD_L)
f.px(3, 1, SK_L); f.px(3, 3, SK_S); f.px(2, 3, IV_S)
f.rect(1, 1, 1, 1, IV_L)
for i in range(4):
    f.px(i, 5, GD if i % 2 == 0 else GD_L)
f.row(4, IV_S, 1, 3)
tabard["back"].fill(IV_C); tabard["back"].row(5, GD_S)
tabard["top"].fill(IV_S); tabard["bottom"].fill(GD_D)
tabard["right"].fill(GD_S); tabard["left"].fill(IV_S)

img.save(sys.argv[1] if len(sys.argv) > 1 else "skyguard_archer.png")
