"""Deterministic generator for textures/entity/cloud_slime.png (P2-E1, concept C). Usage: texture.py <out.png>

Layout (64x64): membrane box UV at (0,0) for an 8x8x8 cube, eye UV at (40,0), opaque cloud UV in rows 32-63.
"""
import random
import sys
from PIL import Image

rng = random.Random(230)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

# Membrane: clear sky-blue glass, slightly denser 1 px rim, lighter toward the top, sparse glint texels.
BODY = (118, 205, 246)
RIM = (176, 232, 255)
faces = {  # name: (u, v) of an 8x8 face in the standard box layout
    "top": (8, 0), "bottom": (16, 0), "east": (0, 8), "north": (8, 8), "west": (16, 8), "south": (24, 8)}
for name, (u, v) in faces.items():
    for y in range(8):
        for x in range(8):
            edge = x in (0, 7) or y in (0, 7)
            shade = 0 if name in ("top",) else (y - 3.5) * 3  # gentle top-to-bottom darkening on the sides
            if name == "bottom":
                shade = 14
            base = RIM if edge else BODY
            n = rng.randint(-5, 5)
            r, g, b = (max(0, min(255, int(c - shade + n))) for c in base)
            a = 150 if edge else 104 + rng.randint(-6, 6)
            px[u + x, v + y] = (r, g, b, a)
    if name != "bottom":
        # Glass glint in the upper-left corner: an L of near-white texels (texture only, not emissive).
        for (x, y) in ((1, 1), (2, 1), (1, 2)):
            px[u + x, v + y] = (238, 252, 255, 210)
        # A single faint diagonal glint toward the lower right.
        px[u + 6, v + 5] = (214, 244, 255, 170)

# Eyes: small pale light-blue rectangles, brighter on top.
for y in range(4):
    for x in range(40, 48):
        px[x, y] = (226, 247, 255, 255) if y < 2 else (164, 218, 245, 255)

# Clouds: opaque white voxels with soft blue-grey undershading so stacked layers read apart.
cloud = [(255, 255, 255), (248, 252, 255), (236, 245, 252), (220, 236, 248)]
for y in range(32, 64):
    for x in range(64):
        row = (y - 32) % 8
        weights = [5, 4, 2, 1] if row < 5 else [2, 3, 4, 3]
        r, g, b = rng.choices(cloud, weights)[0]
        px[x, y] = (r, g, b, 255)

img.save(sys.argv[1])
