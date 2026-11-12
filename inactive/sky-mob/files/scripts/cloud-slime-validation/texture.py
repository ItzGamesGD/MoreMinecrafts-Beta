"""Deterministic generator for textures/entity/cloud_slime.png (P2-E1-R2, concept A). Usage: texture.py <out.png>

Layout (64x64): membrane 8x8x8 box UV at (0,0), core 6x6x6 box UV at (0,16), eye UV at (40,0),
opaque exterior cloud UV in rows 32-63.
"""
import random
import sys
from PIL import Image

rng = random.Random(230)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()


def clamp(v):
    return max(0, min(255, int(v)))


def box_faces(u, v, s):
    """Face origins of an s*s*s cube in the standard box UV layout."""
    return {"top": (u + s, v), "bottom": (u + 2 * s, v), "east": (u, v + s), "north": (u + s, v + s),
            "west": (u + 2 * s, v + s), "south": (u + 3 * s, v + s)}


# Membrane: soft bright sky-blue, denser 1 px rim.
for name, (u, v) in box_faces(0, 0, 8).items():
    for y in range(8):
        for x in range(8):
            edge = x in (0, 7) or y in (0, 7)
            r, g, b = (182, 228, 255) if edge else (150, 210, 250)
            n = rng.randint(-4, 4)
            px[u + x, v + y] = (clamp(r + n), clamp(g + n), clamp(b), 175 if edge else 130 + rng.randint(-5, 5))

# Core: opaque pale blue, lighter toward the top so the body reads soft rather than glassy.
for name, (u, v) in box_faces(0, 16, 6).items():
    for y in range(6):
        for x in range(6):
            t = 0 if name == "top" else (14 if name == "bottom" else y * 4)
            n = rng.randint(-4, 4)
            px[u + x, v + y] = (clamp(168 - t + n), clamp(214 - t + n), clamp(248 - t // 2), 255)

# Eyes: bright cyan with a pale top highlight.
for y in range(8):
    for x in range(40, 48):
        px[x, y] = (226, 252, 255, 255) if y == 0 else (86, 224, 255, 255)

# Exterior clouds: mostly white with soft pale-blue shading.
cloud = [(255, 255, 255), (247, 251, 255), (234, 244, 253), (216, 233, 249)]
for y in range(32, 64):
    for x in range(64):
        r, g, b = rng.choices(cloud, [6, 4, 2, 1])[0]
        px[x, y] = (r, g, b, 255)

img.save(sys.argv[1])
