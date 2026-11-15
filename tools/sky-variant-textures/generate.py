#!/usr/bin/env python3
"""Generate the five group B sky-island variant textures (weathered zombie, sky archer, rock creeper,
pale enderman, azure slime) on the exact 26.3 vanilla UV layouts.

Usage: python3 -I generate.py <minecraft-clientOnly.jar> <output dir>

Vanilla textures are read from the jar only as UV/shape templates (face holes, rib cut-outs, baby layout);
every output pixel is repainted. Output is deterministic (fixed seeds).
"""
import io
import random
import sys
import zipfile

from PIL import Image

T = "assets/minecraft/textures/entity/"


def load(jar, path):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(T + path))).convert("RGBA")


def faces(u, v, w, h, d):
    """Minecraft box-UV faces -> (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def each(rect):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            yield x0 + x, y0 + y, x, y


def jitter(rng, c, n=10):
    k = rng.randint(-n, n)
    return tuple(max(0, min(255, v + k)) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def pick(rng, palette):
    return palette[rng.randrange(len(palette))]


# --------------------------------------------------------------------------- weathered zombie
SKIN = [(104, 128, 122), (114, 138, 130), (96, 118, 114), (124, 144, 136), (88, 108, 104)]
STONE_HAIR = [(118, 120, 120), (136, 138, 138), (100, 102, 102), (150, 152, 150), (86, 88, 88)]
CLOTH = [(82, 132, 172), (72, 118, 158), (96, 146, 184), (64, 104, 140)]
CHAIN_LIGHT = (156, 158, 160)
CHAIN_DARK = (92, 94, 98)
STRAP = (122, 74, 42)
STRAP_DARK = (92, 54, 30)
BOOT = [(104, 64, 40), (90, 54, 34), (118, 76, 46), (76, 46, 28)]
EYE = (30, 38, 40)


def paint_humanoid_zombie(img, rng):
    head = faces(0, 0, 8, 8, 8)
    for name, rect in head.items():
        for px, py, x, y in each(rect):
            if name == "top" or (name != "bottom" and y < 2) or (name == "back" and y < 5):
                c = pick(rng, STONE_HAIR)
            elif name != "bottom" and y == 2 and rng.random() < 0.5:
                c = pick(rng, STONE_HAIR)
            else:
                c = pick(rng, SKIN)
            img.putpixel((px, py), jitter(rng, c, 6))
    fx, fy = head["front"][0], head["front"][1]
    for x in (1, 2, 5, 6):
        img.putpixel((fx + x, fy + 4), EYE + (255,))
    for x in (1, 6):
        img.putpixel((fx + x, fy + 3), (78, 96, 94, 255))
    for x in (3, 4):
        img.putpixel((fx + x, fy + 5), (84, 104, 100, 255))
    for x in (2, 3, 4, 5):
        img.putpixel((fx + x, fy + 6), (70, 86, 84, 255))

    body = faces(16, 16, 8, 12, 4)
    for name, rect in body.items():
        for px, py, x, y in each(rect):
            w = rect[2]
            if name in ("top", "bottom"):
                c = pick(rng, CLOTH)
            elif y >= 10:
                c = pick(rng, CLOTH) if (x + y) % 3 else (58, 94, 128)
            elif y in (8, 9):
                c = STRAP_DARK if y == 9 else STRAP
                if name == "front" and y == 8 and x in (3, 4):
                    c = (170, 150, 90)
            elif name in ("front", "back") and 1 <= x <= w - 2 and y >= 2:
                c = CHAIN_LIGHT if (x + y) % 2 == 0 else CHAIN_DARK
            else:
                c = pick(rng, CLOTH)
            img.putpixel((px, py), jitter(rng, c, 5))
    # Diagonal leather strap across the chest (front) and back.
    fx, fy = body["front"][0], body["front"][1]
    for i in range(8):
        img.putpixel((fx + i, fy + i), STRAP + (255,))
        if i + 1 < 8:
            img.putpixel((fx + i + 1, fy + i), STRAP_DARK + (255,))
    bx, by = body["back"][0], body["back"][1]
    for i in range(8):
        img.putpixel((bx + 7 - i, by + i), STRAP + (255,))

    arm = faces(40, 16, 4, 12, 4)
    for name, rect in arm.items():
        for px, py, x, y in each(rect):
            if name == "top":
                c = pick(rng, CLOTH)
            elif name == "bottom":
                c = pick(rng, SKIN)
            elif y < 3 or (y == 3 and rng.random() < 0.5):
                c = pick(rng, CLOTH)
            else:
                c = pick(rng, SKIN)
            img.putpixel((px, py), jitter(rng, c, 6))

    leg = faces(0, 16, 4, 12, 4)
    for name, rect in leg.items():
        for px, py, x, y in each(rect):
            if name == "top":
                c = pick(rng, CLOTH)
            elif name == "bottom":
                c = (64, 40, 26)
            elif y < 6 or (y == 6 and rng.random() < 0.5):
                c = pick(rng, CLOTH)
                if rng.random() < 0.12:
                    c = (60, 96, 130)
            else:
                c = pick(rng, BOOT)
                if y == 11:
                    c = (64, 40, 26)
            img.putpixel((px, py), jitter(rng, c, 5))


def weathered_zombie(jar):
    rng = random.Random(51)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_humanoid_zombie(img, rng)
    return img


def weathered_zombie_baby(jar):
    """Baby layout differs in 26.3, so classify each vanilla baby pixel and repaint it with the same palette."""
    src = load(jar, "zombie/zombie_baby.png")
    rng = random.Random(52)
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            if max(r, g, b) < 50:
                c = EYE
            elif g > r and g > b and b < 110:          # green skin
                c = pick(rng, STONE_HAIR) if y < 9 and rng.random() < 0.35 else pick(rng, SKIN)
            elif b > r and g > 120:                    # cyan shirt
                c = CHAIN_LIGHT if (x + y) % 2 == 0 else CHAIN_DARK
                if y % 6 == 0:
                    c = STRAP
            elif b > g:                                # purple trousers
                c = pick(rng, CLOTH)
            else:                                      # grey shoes
                c = pick(rng, BOOT)
            out.putpixel((x, y), jitter(rng, c, 5))
    return out


# --------------------------------------------------------------------------- sky archer
SCARF = [(92, 152, 192), (76, 132, 172), (112, 172, 206), (64, 112, 150)]


def bone(v):
    t = max(0.0, min(1.0, (v - 60) / 170))
    dark, light = (96, 86, 70), (238, 230, 208)
    return tuple(int(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (255,)


def sky_archer(jar):
    src = load(jar, "skeleton/skeleton.png")
    rng = random.Random(61)
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a:
                out.putpixel((x, y), bone((r + g + b) / 3))
    head = faces(0, 0, 8, 8, 8)
    # Tan cloth headband around the skull with a fletched quill on its right side.
    for name in ("front", "back", "left", "right"):
        x0, y0, w, _ = head[name]
        for x in range(w):
            for y in (0, 1):
                out.putpixel((x0 + x, y0 + y), jitter(rng, (206, 182, 140) if y == 0 else (178, 152, 112), 6))
    for px, py, _, _ in each(head["top"]):
        out.putpixel((px, py), jitter(rng, (214, 192, 152), 8))
    rx, ry = head["right"][0], head["right"][1]
    for i, c in enumerate([(90, 90, 96), (120, 120, 126), (110, 80, 50)]):
        out.putpixel((rx + 1 + i, ry), c + (255,))
    # Pale-blue glint in the eye sockets.
    fx, fy = head["front"][0], head["front"][1]
    for ex in (1, 2, 5, 6):
        x, y = fx + ex, fy + 4
        if sum(out.getpixel((x, y))[:3]) < 400:
            out.putpixel((x, y), (60, 110, 128, 255))
    # Blue scarf: shoulders band wrapping the body plus a hanging tail on the front.
    body = faces(16, 16, 8, 12, 4)
    for name in ("front", "back", "left", "right"):
        x0, y0, w, _ = body[name]
        for x in range(w):
            for y in (0, 1):
                out.putpixel((x0 + x, y0 + y), jitter(rng, pick(rng, SCARF), 4))
    for px, py, _, _ in each(body["top"]):
        out.putpixel((px, py), jitter(rng, pick(rng, SCARF), 4))
    for y in range(2, 9):
        out.putpixel((body["front"][0] + 5, body["front"][1] + y), jitter(rng, pick(rng, SCARF), 4))
        if y < 7:
            out.putpixel((body["front"][0] + 6, body["front"][1] + y), jitter(rng, (64, 112, 150), 4))
    # Cloth sash at the hip.
    for name in ("front", "back"):
        x0, y0, w, _ = body[name]
        for x in range(w):
            out.putpixel((x0 + x, y0 + 10), jitter(rng, (70, 122, 160), 4))
    # Blue wraps on forearms and shins (2x12x2 limbs).
    for u, v, band in ((40, 16, (6, 7)), (0, 16, (3, 4))):
        for name, rect in faces(u, v, 2, 12, 2).items():
            if name in ("top", "bottom"):
                continue
            for px, py, _, y in each(rect):
                if y in band:
                    out.putpixel((px, py), jitter(rng, pick(rng, SCARF), 4))
    return out


# --------------------------------------------------------------------------- rock creeper
ROCK = [(74, 74, 74), (92, 92, 90), (110, 110, 108), (128, 128, 126), (146, 146, 144), (164, 164, 160)]
LICHEN = [(168, 214, 194), (132, 186, 164), (190, 226, 210)]


def rock_creeper(jar):
    src = load(jar, "creeper/creeper.png")
    rng = random.Random(71)
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    # Two-pixel blocks of stone tone, like cobble/andesite.
    blocks = {}
    for y in range(src.height):
        for x in range(src.width):
            if src.getpixel((x, y))[3] == 0:
                continue
            key = (x // 2, y // 2)
            if key not in blocks:
                blocks[key] = pick(rng, ROCK)
            c = blocks[key] if rng.random() < 0.7 else pick(rng, ROCK)
            if rng.random() < 0.06:
                c = pick(rng, LICHEN)
            out.putpixel((x, y), jitter(rng, c, 6))
    # Face: the vanilla dark face pixels on the head front become black stone.
    fx, fy, fw, fh = faces(0, 0, 8, 8, 8)["front"]
    for x in range(fx, fx + fw):
        for y in range(fy, fy + fh):
            r, g, b, _ = src.getpixel((x, y))
            if max(r, g, b) < 60:
                out.putpixel((x, y), jitter(rng, (26, 26, 26) if (x + y) % 2 else (14, 14, 14), 3))
    # A few cracks on the head top / sides.
    for (x, y) in ((9, 2), (10, 3), (10, 4), (2, 10), (3, 11), (20, 10), (21, 11)):
        out.putpixel((x, y), (48, 48, 48, 255))
    # Dark stone toes: bottom row of each leg side and the leg bottoms.
    for name, rect in faces(0, 16, 4, 6, 4).items():
        if name == "top":
            continue
        for px, py, x, y in each(rect):
            if name == "bottom" or y == rect[3] - 1:
                out.putpixel((px, py), (40, 40, 40, 255) if (x % 2) else (18, 18, 18, 255))
    return out


# --------------------------------------------------------------------------- pale enderman
def pale_enderman(jar):
    src = load(jar, "enderman/enderman.png")
    rng = random.Random(81)
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    light, dark = (80, 84, 92), (50, 53, 60)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            if r == 255:
                c = (225, 242, 255)
            elif r >= 20:
                c = jitter(rng, light, 5)
            else:
                c = jitter(rng, dark, 4)
            out.putpixel((x, y), c[:3] + (a,))
    for x in (9, 14):
        out.putpixel((x, 12), (240, 252, 255, 255))
    return out


def pale_enderman_eyes(jar):
    src = load(jar, "enderman/enderman_eyes.png")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a:
                out.putpixel((x, y), (236, 250, 255, a) if g < 50 else (168, 222, 246, a))
    return out


# --------------------------------------------------------------------------- azure slime
def azure_slime(jar):
    rng = random.Random(91)
    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    outer = faces(0, 0, 8, 8, 8)
    for name, rect in outer.items():
        clouds = set()
        for _ in range(3):
            cx, cy = rng.randrange(1, 6), rng.randrange(1, 7)
            clouds.update({(cx, cy), (cx + 1, cy), (cx + 2, cy) if rng.random() < 0.6 else (cx, cy)})
            if rng.random() < 0.5:
                clouds.add((cx + 1, cy - 1))
        for px, py, x, y in each(rect):
            edge = x in (0, 7) or y in (0, 7)
            if (x, y) in clouds and not edge:
                c = (246, 250, 255, 205)
            elif edge:
                c = jitter(rng, (178, 222, 250), 4)[:3] + (196,)
            else:
                c = jitter(rng, (138, 200, 242), 6)[:3] + (150,)
            out.putpixel((px, py), c)
    inner = faces(0, 16, 6, 6, 6)
    for name, rect in inner.items():
        for px, py, x, y in each(rect):
            out.putpixel((px, py), jitter(rng, pick(rng, [(38, 150, 192), (30, 136, 178), (52, 164, 204)]), 4))
    for u, v in ((32, 0), (32, 4)):
        for name, rect in faces(u, v, 2, 2, 2).items():
            for px, py, x, y in each(rect):
                out.putpixel((px, py), (16, 46, 74, 255))
        fx, fy = faces(u, v, 2, 2, 2)["front"][:2]
        out.putpixel((fx, fy), (176, 236, 255, 255))
    for name, rect in faces(32, 8, 1, 1, 1).items():
        for px, py, _, _ in each(rect):
            out.putpixel((px, py), (16, 46, 74, 255))
    return out


def main():
    jar, dest = sys.argv[1], sys.argv[2]
    outputs = {
        "weathered_zombie": weathered_zombie, "weathered_zombie_baby": weathered_zombie_baby,
        "sky_archer": sky_archer, "rock_creeper": rock_creeper,
        "pale_enderman": pale_enderman, "pale_enderman_eyes": pale_enderman_eyes,
        "azure_slime": azure_slime,
    }
    for name, fn in outputs.items():
        fn(jar).save(f"{dest}/{name}.png", optimize=True)
        print("wrote", name)


if __name__ == "__main__":
    main()
