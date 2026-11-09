#!/usr/bin/env python3
"""Generates assets/moreminecrafts/textures/entity/immortal_gladiator.png (64x64, deterministic).

UV layout (must match GladiatorRenderer): head(0,0) hat/helmet(32,0) body(16,16) right_leg(0,16) right_arm(40,16) left_leg(16,48) left_arm(32,48),
crest(0,32) 2x5x8, breastplate(20,32) 9x10x5, pauldron(48,32) 4x3x4 (both shoulders), bracer(48,41) 4x5x4, greave(48,50) 4x5x4.
Visual concept: pale ashen "immortal" fighter in bronze-and-iron Roman gladiator harness, crimson tunic/crest, glowing amber eyes.
"""
import random, sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "immortal_gladiator.png"
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()
rnd = random.Random(5150)

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)

SKIN = (150, 138, 126); SKIN_D = (112, 102, 94)
BRONZE = (168, 118, 52); BRONZE_L = (222, 170, 84); BRONZE_D = (104, 66, 30)
IRON = (74, 78, 88); IRON_L = (118, 124, 136); IRON_D = (40, 42, 50)
CRIMSON = (140, 30, 32); CRIMSON_D = (88, 16, 20)
LEATHER = (84, 54, 32); LEATHER_D = (52, 32, 20)
EYE = (255, 176, 40)

def fill(x, y, w, h, base, noise=0.06):
    for j in range(h):
        for i in range(w):
            px[x + i, y + j] = shade(base, 1 + rnd.uniform(-noise, noise))

def box_faces(tx, ty, w, h, d):
    """Return face rects for a Minecraft box UV at (tx,ty): dict name -> (x,y,w,h)."""
    return {
        "top": (tx + d, ty, w, d), "bottom": (tx + d + w, ty, w, d),
        "right": (tx, ty + d, d, h), "front": (tx + d, ty + d, w, h),
        "left": (tx + d + w, ty + d, d, h), "back": (tx + 2 * d + w, ty + d, w, h),
    }

def put(x, y, c):
    if 0 <= x < 64 and 0 <= y < 64: px[x, y] = c if len(c) == 4 else c + (255,)

def hline(x, y, w, c):
    for i in range(w): put(x + i, y, c)

def vline(x, y, h, c):
    for j in range(h): put(x, y + j, c)

def frame(r, light, dark):
    x, y, w, h = r
    hline(x, y, w, light); vline(x, y, h, light); hline(x, y + h - 1, w, dark); vline(x + w - 1, y, h, dark)

# ---------------------------------------------------------------- head (skin, scarred, glowing eyes)
hd = box_faces(0, 0, 8, 8, 8)
for n, r in hd.items(): fill(*r, SKIN)
x, y, w, h = hd["front"]
for i in (1, 5):                                   # eyes
    put(x + i, y + 3, EYE); put(x + i + 1, y + 3, EYE); put(x + i, y + 4, shade(EYE, .6)); put(x + i + 1, y + 4, shade(EYE, .6))
hline(x + 2, y + 6, 4, SKIN_D); put(x + 3, y + 5, SKIN_D)   # mouth / scar
for k in range(3): put(x + 6, y + 1 + k, CRIMSON_D)           # scar
x, y, w, h = hd["top"]; fill(x, y, w, h, CRIMSON_D)

# ---------------------------------------------------------------- helmet shell (hat layer at 32,0): bronze with eye slit, cheek guards
ht = box_faces(32, 0, 8, 8, 8)
for n, r in ht.items(): fill(*r, BRONZE, .05)
x, y, w, h = ht["front"]
fill(x, y, w, 3, BRONZE_L, .04)                     # brow band
for i in range(w): put(x + i, y + 3, BRONZE_D)
for j in range(4, 8):                               # face opening (transparent -> face shows through), cheek guards at the sides
    for i in range(2, 6): put(x + i, y + j, (0, 0, 0, 0))
vline(x + 3, y + 3, 5, BRONZE_D); vline(x + 4, y + 3, 5, BRONZE_D)  # nose guard
put(x + 3, y + 3, BRONZE_D); put(x + 4, y + 3, BRONZE_D)
for i in range(2, 6):
    put(x + i, y + 3, (0, 0, 0, 0)) if i in (2, 5) else None
x, y, w, h = ht["top"]; fill(x, y, w, h, BRONZE_L, .04); hline(x, y + 3, w, BRONZE_D); hline(x, y + 4, w, BRONZE_D)
for key in ("left", "right", "back"):
    x, y, w, h = ht[key]
    hline(x, y + h - 1, w, BRONZE_D); hline(x, y, w, BRONZE_L)
    for i in range(0, w, 2): put(x + i, y + 2, BRONZE_D)  # rivets
x, y, w, h = ht["bottom"]
for j in range(h):
    for i in range(w): put(x + i, y + j, (0, 0, 0, 0))

# ---------------------------------------------------------------- crest (2x5x8 at 0,32): crimson horsehair with bronze base
cr = box_faces(0, 32, 2, 5, 8)
for n, r in cr.items(): fill(*r, CRIMSON, .12)
for n in ("left", "right"):
    x, y, w, h = cr[n]
    for i in range(0, w, 2): vline(x + i, y, h, CRIMSON_D)
    hline(x, y + h - 1, w, BRONZE_D)
x, y, w, h = cr["top"]; fill(x, y, w, h, CRIMSON_D)

# ---------------------------------------------------------------- torso: crimson tunic, leather straps, belt
bd = box_faces(16, 16, 8, 12, 4)
for n, r in bd.items(): fill(*r, CRIMSON, .07)
for n in ("front", "back", "left", "right"):
    x, y, w, h = bd[n]
    hline(x, y + h - 4, w, LEATHER); hline(x, y + h - 3, w, LEATHER_D)             # belt
    put(x + w // 2, y + h - 4, BRONZE_L) if n in ("front", "back") else None
    hline(x, y + h - 1, w, CRIMSON_D)
x, y, w, h = bd["front"]
for j in range(h - 4): put(x + j // 3, y + j, LEATHER_D); put(x + w - 1 - j // 3, y + j, LEATHER_D)  # crossed straps

# ---------------------------------------------------------------- breastplate (9x10x5 at 20,32): bronze cuirass with iron trim
bp = box_faces(20, 32, 9, 10, 5)
for n, r in bp.items(): fill(*r, BRONZE, .05)
x, y, w, h = bp["front"]
frame(bp["front"], BRONZE_L, BRONZE_D)
vline(x + w // 2, y + 1, h - 2, BRONZE_D)                                          # sternum seam
for k in range(3):                                                                  # muscle cuirass banding
    hline(x + 1, y + 3 + 2 * k, w // 2 - 1, BRONZE_L); hline(x + w // 2 + 1, y + 3 + 2 * k, w // 2 - 1, BRONZE_L)
    hline(x + 1, y + 4 + 2 * k, w - 2, BRONZE_D)
put(x + w // 2, y + 2, EYE)                                                         # glowing gem
x, y, w, h = bp["back"]; frame(bp["back"], BRONZE_L, BRONZE_D); vline(x + w // 2, y + 1, h - 2, BRONZE_D)
for n in ("left", "right"): frame(bp[n], BRONZE_L, BRONZE_D)
x, y, w, h = bp["top"]; fill(x, y, w, h, IRON)
x, y, w, h = bp["bottom"]; fill(x, y, w, h, IRON_D)

# ---------------------------------------------------------------- arms (skin, wraps) and legs (leather greaves lacing)
def limb(tx, ty):
    f = box_faces(tx, ty, 4, 12, 4)
    for n, r in f.items(): fill(*r, SKIN, .06)
    for n in ("front", "back", "left", "right"):
        x, y, w, h = f[n]
        for j in range(5, 12): hline(x, y + j, w, LEATHER if j % 2 else LEATHER_D)  # lower wraps
        hline(x, y + 4, w, LEATHER_D)
        for j in range(0, 3): hline(x, y + j, w, CRIMSON)                            # sleeve hem under the pauldron
    x, y, w, h = f["top"]; fill(x, y, w, h, IRON_D)
    return f
limb(40, 16); limb(32, 48)

def leg(tx, ty):
    f = box_faces(tx, ty, 4, 12, 4)
    for n, r in f.items(): fill(*r, SKIN, .06)
    for n in ("front", "back", "left", "right"):
        x, y, w, h = f[n]
        for j in range(0, 5): hline(x, y + j, w, CRIMSON if j < 4 else CRIMSON_D)    # tunic skirt (pteruges)
        for j in range(5, 7): hline(x, y + j, w, LEATHER_D if j % 2 else LEATHER)
        hline(x, y + h - 2, w, LEATHER_D); hline(x, y + h - 1, w, LEATHER_D)          # sandal straps
        for j in range(7, 10):
            put(x, y + j, LEATHER_D); put(x + w - 1, y + j, LEATHER_D)
    return f
leg(0, 16); leg(16, 48)

# ---------------------------------------------------------------- pauldron 4x3x4 (48,32): layered bronze plate, iron rim, spike stud
pd = box_faces(48, 32, 4, 3, 4)
for n, r in pd.items():
    fill(*r, BRONZE, .05); frame(r, BRONZE_L, BRONZE_D)
x, y, w, h = pd["top"]; fill(x, y, w, h, BRONZE_L, .04); put(x + 1, y + 1, IRON_L); put(x + 2, y + 2, IRON_D)
x, y, w, h = pd["bottom"]; fill(x, y, w, h, IRON_D)
# ---------------------------------------------------------------- bracer 4x5x4 (48,41) and greave 4x5x4 (48,50)
for (tx, ty, base, hi, lo) in ((48, 41, IRON, IRON_L, IRON_D), (48, 50, BRONZE, BRONZE_L, BRONZE_D)):
    f = box_faces(tx, ty, 4, 5, 4)
    for n, r in f.items():
        fill(*r, base, .05); frame(r, hi, lo)
    for n in ("front", "back"):
        x, y, w, h = f[n]; put(x + 1, y + 2, EYE if base is BRONZE else hi); put(x + 2, y + 2, lo)
    x, y, w, h = f["top"]; fill(x, y, w, h, lo)
    x, y, w, h = f["bottom"]; fill(x, y, w, h, lo)

img.save(OUT)
print("wrote", OUT, img.size)
