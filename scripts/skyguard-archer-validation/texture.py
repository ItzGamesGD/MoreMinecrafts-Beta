"""Generates the original 64x64 Skyguard Archer (P2-C1, Concept B) texture.

Run: python3 -I scripts/skyguard-archer-validation/texture.py <output.png>
Layout follows SkyguardArcherRenderer.createLayer (player-skin format; leg overlays hold cape and pauldrons).
"""
import sys
from PIL import Image

IV, IV_SH, IV_HI = (234, 226, 208), (200, 190, 170), (248, 244, 232)
GD, GD_SH, GD_HI = (194, 156, 78), (146, 112, 52), (228, 198, 118)
SK, SK_SH, SK_HI = (118, 168, 214), (82, 128, 180), (156, 196, 232)
NV = (46, 70, 120)
BR, BR_SH = (112, 76, 46), (80, 54, 33)
SKIN, SKIN_SH = (240, 212, 188), (218, 186, 160)
HAIR, HAIR_SH = (206, 196, 180), (172, 160, 144)
EYE, EYE_DK, WH = (74, 140, 218), (38, 72, 150), (250, 250, 252)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def px(x, y, c):
    img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def rect(x, y, w, h, c):
    for i in range(w):
        for j in range(h):
            px(x + i, y + j, c)


def faces(u, v, w, h, d):
    """Box UV faces: name -> (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


# ---- Head (0,0): hair, pale face, blue eyes --------------------------------------------------------------
f = faces(0, 0, 8, 8, 8)
for name in ("top", "right", "left", "back"):
    rect(*f[name], HAIR)
rect(*f["bottom"], SKIN_SH)
fx, fy = f["front"][:2]
rect(fx, fy, 8, 8, SKIN)
rect(fx, fy, 8, 2, HAIR)
for x in (0, 1, 2, 5, 6, 7):
    px(fx + x, fy + 2, HAIR_SH if x in (2, 5) else HAIR)
for x in (0, 7):
    for y in range(3, 7):
        px(fx + x, fy + y, HAIR)
px(fx + 1, fy + 4, WH); px(fx + 2, fy + 4, EYE); px(fx + 2, fy + 5, EYE_DK); px(fx + 1, fy + 5, SKIN_SH)
px(fx + 6, fy + 4, WH); px(fx + 5, fy + 4, EYE); px(fx + 5, fy + 5, EYE_DK); px(fx + 6, fy + 5, SKIN_SH)
px(fx + 3, fy + 7, SKIN_SH); px(fx + 4, fy + 7, SKIN_SH)

# ---- Hood (hat layer, 32,0): ivory hood, raised gold trim around the face opening ------------------------
f = faces(32, 0, 8, 8, 8)
tx, ty = f["top"][:2]
rect(tx, ty, 8, 8, IV)
for i in range(8):
    px(tx + 3, ty + i, IV_SH); px(tx + 4, ty + i, IV_SH)
    px(tx + i, ty + 7, GD)  # front edge of the crown
hx, hy = f["front"][:2]
rect(hx, hy, 8, 8, IV)
rect(hx + 1, hy + 2, 6, 6, (0, 0, 0, 0))  # face opening
for i in range(1, 7):
    px(hx + i, hy + 1, GD)
for j in range(1, 8):
    px(hx, hy + j, GD); px(hx + 7, hy + j, GD)
px(hx + 3, hy, GD_SH); px(hx + 4, hy, GD_SH)
for side in ("right", "left"):
    sx, sy = f[side][:2]
    rect(sx, sy, 8, 8, IV)
    front_col = 7 if side == "right" else 0
    for j in range(8):
        px(sx + front_col, sy + j, GD)
        px(sx + (6 if side == "right" else 1), sy + j, IV_HI)
    for i in range(8):
        px(sx + i, sy + 7, IV_SH)
bx, by = f["back"][:2]
rect(bx, by, 8, 8, IV)
for j in range(8):
    px(bx + 3, by + j, IV_SH)
for i in range(8):
    px(bx + i, by + 7, GD_SH)

# Laurel (56,16) 3x2x1: gold leaves.
f = faces(56, 16, 3, 2, 1)
for name, (x, y, w, h) in f.items():
    rect(x, y, w, h, GD)
lx, ly = f["front"][:2]
px(lx, ly, GD_HI); px(lx + 1, ly, GD); px(lx + 2, ly, GD_HI)
px(lx, ly + 1, GD); px(lx + 1, ly + 1, GD_SH); px(lx + 2, ly + 1, GD)

# ---- Body (16,16): ivory tunic, blue collar, brown belt, blue front panel ---------------------------------
f = faces(16, 16, 8, 12, 4)
for name in ("top", "bottom"):
    rect(*f[name], IV_SH)
x0, y0 = f["top"][:2]
rect(x0, y0, 8, 2, SK)
for name in ("front", "back", "right", "left"):
    x, y, w, h = f[name]
    rect(x, y, w, h, IV)
    rect(x, y, w, 2, SK)                  # scarf / collar
    rect(x, y + 7, w, 2, BR)              # belt
    rect(x, y + 11, w, 1, IV_SH)
x, y = f["front"][:2]
px(x + 3, y + 1, GD); px(x + 4, y + 1, GD)              # collar clasp
for j in range(2, 7):
    px(x + 3, y + j, IV_HI); px(x + 4, y + j, IV_HI)
px(x + 3, y + 4, GD); px(x + 4, y + 4, GD)              # chest emblem
rect(x + 3, y + 7, 2, 2, GD); px(x + 3, y + 8, GD_SH)   # buckle
for j in range(9, 12):
    px(x + 2, y + j, GD); px(x + 5, y + j, GD)
    px(x + 3, y + j, SK); px(x + 4, y + j, SK_SH)
x, y = f["back"][:2]
rect(x + 2, y + 7, 4, 2, BR_SH)

# Tunic overlay (16,32): raised gold-edged chest plate and layered hem.
f = faces(16, 32, 8, 12, 4)
x, y = f["front"][:2]
for i in range(8):
    px(x + i, y + 2, GD)
for j in range(3, 7):
    px(x, y + j, GD); px(x + 7, y + j, GD)
    px(x + 1, y + j, IV_HI); px(x + 6, y + j, IV_HI)
for i in (0, 1, 6, 7):
    for j in range(9, 12):
        px(x + i, y + j, IV_HI if j < 11 else GD)
for side in ("right", "left"):
    x, y, w, h = f[side]
    for i in range(w):
        px(x + i, y + 2, GD)
        px(x + i, y + 11, GD)

# ---- Arms (right 40,16; left 32,48): ivory sleeves, gold-trimmed bracers, brown gloves -------------------
for u, v in ((40, 16), (32, 48)):
    f = faces(u, v, 4, 12, 4)
    rect(*f["top"], IV)
    rect(*f["bottom"], BR_SH)
    for name in ("front", "back", "right", "left"):
        x, y, w, h = f[name]
        rect(x, y, w, 6, IV)
        rect(x, y + 6, w, 1, GD)
        rect(x, y + 7, w, 3, SK if name in ("right", "left") else IV_SH)
        rect(x, y + 9, w, 1, GD)
        rect(x, y + 10, w, 2, BR)
    x, y = f["front"][:2]
    rect(x + 1, y + 7, 2, 2, IV_SH)
# Sleeve overlays (right 40,32; left 48,48): raised bracer cuffs.
for u, v in ((40, 32), (48, 48)):
    f = faces(u, v, 4, 12, 4)
    for name in ("front", "back", "right", "left"):
        x, y, w, h = f[name]
        rect(x, y + 6, w, 1, GD_HI)
        rect(x, y + 9, w, 1, GD)

# ---- Legs (right 0,16; left 16,48): ivory trousers, brown boots with gold cuff --------------------------
for u, v in ((0, 16), (16, 48)):
    f = faces(u, v, 4, 12, 4)
    rect(*f["top"], IV_SH)
    rect(*f["bottom"], BR_SH)
    for name in ("front", "back", "right", "left"):
        x, y, w, h = f[name]
        rect(x, y, w, 6, IV)
        rect(x, y + 5, w, 1, IV_SH)
        rect(x, y + 6, w, 1, BR_SH)
        rect(x, y + 7, w, 5, BR)
        rect(x, y + 11, w, 1, BR_SH)
    x, y = f["front"][:2]
    rect(x + 1, y + 7, 2, 1, GD_SH)

# ---- Pauldrons (right 0,50; left 0,57) 4x3x4: ivory plate, gold rim ------------------------------------
for u, v in ((0, 50), (0, 57)):
    f = faces(u, v, 4, 3, 4)
    x, y, w, h = f["top"]
    rect(x, y, w, h, IV_HI)
    for i in range(4):
        px(x + i, y, GD); px(x + i, y + 3, GD); px(x, y + i, GD); px(x + 3, y + i, GD)
    rect(*f["bottom"], IV_SH)
    for name in ("front", "back", "right", "left"):
        x, y, w, h = f[name]
        rect(x, y, w, 1, IV_HI)
        rect(x, y + 1, w, 1, IV)
        rect(x, y + 2, w, 1, GD)

# ---- Cape (0,32) 7x17x1: sky blue, gold border and celestial star, dark-blue inner -----------------------
f = faces(0, 32, 7, 17, 1)
rect(*f["top"], GD)
rect(*f["bottom"], GD_SH)
rect(*f["right"], GD)
rect(*f["left"], GD)
rect(*f["front"], NV)
x, y, w, h = f["back"]  # outward face
rect(x, y, w, h, SK)
for j in range(h):
    px(x + 2, y + j, SK_SH); px(x + 4, y + j, SK_SH)
    px(x, y + j, GD); px(x + 6, y + j, GD)
for i in range(w):
    px(x + i, y, GD); px(x + i, y + 1, GD_SH if i in (0, 6) else SK_HI)
    px(x + i, y + 15, NV if i not in (0, 6) else GD)
    px(x + i, y + 16, GD)
cx, cy = x + 3, y + 6  # four-point star
for dx, dy in ((0, -2), (0, -1), (0, 0), (0, 1), (0, 2), (-1, 0), (1, 0), (-2, 0), (2, 0)):
    px(cx + dx, cy + dy, GD_HI if (dx, dy) == (0, 0) else GD)
for dx, dy in ((-2, 5), (2, 5), (0, 7)):
    px(cx + dx, cy + dy, GD)
for j in range(10, 15):
    px(x + 3, y + j, SK)  # keep the centre fold clean below the star

img.save(sys.argv[1] if len(sys.argv) > 1 else "skyguard_archer.png")
