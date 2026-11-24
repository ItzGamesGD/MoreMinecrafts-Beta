"""Biome-styled sky island base templates (terrain, vegetation, jigsaw links and structure sockets)."""
import math
import random

from voxel import Vox

LINK = "moreminecrafts:sky_link"
TOP = "moreminecrafts:sky_top"

# Gravity blocks (sand, red sand, gravel) are only ever placed with a non-falling block directly beneath.
STYLES = {
    "snowy": dict(top="snow_block", sub=["snow_block", "stone", "stone"], body=["stone", "stone", "andesite", "packed_ice", "cobblestone"],
                  veg="snowy"),
    "meadow": dict(top="grass_block", sub=["dirt", "dirt", "coarse_dirt"], body=["stone", "stone", "andesite", "tuff"], veg="meadow"),
    "forest": dict(top="grass_block", sub=["dirt", "dirt", "podzol"], body=["stone", "stone", "andesite", "mossy_cobblestone"], veg="forest"),
    "badlands": dict(top="red_sand", sub=["red_sandstone", "red_sandstone", "orange_terracotta"],
                     body=["terracotta", "orange_terracotta", "yellow_terracotta", "white_terracotta", "red_terracotta", "terracotta"],
                     veg="badlands"),
    "desert": dict(top="sand", sub=["sandstone", "sandstone", "sandstone"], body=["sandstone", "sandstone", "cut_sandstone", "smooth_sandstone"],
                   veg="desert"),
    "rocky": dict(top="gravel", sub=["stone", "andesite", "stone"], body=["stone", "andesite", "tuff", "stone", "cobblestone"], veg="rocky"),
}
GRAVITY = {"sand", "red_sand", "gravel"}
FLOWERS = ["poppy", "dandelion", "azure_bluet", "oxeye_daisy", "cornflower", "allium"]


def _noise(rng, size):
    """Smooth-ish deterministic 2D noise: bilinear interpolation of a coarse random grid."""
    coarse = [[rng.random() for _ in range(size // 4 + 24)] for _ in range(size // 4 + 24)]

    def sample(x, z):
        fx, fz = x / 4.0, z / 4.0
        ix, iz = int(fx), int(fz)
        tx, tz = fx - ix, fz - iz
        a = coarse[ix][iz] * (1 - tx) + coarse[ix + 1][iz] * tx
        b = coarse[ix][iz + 1] * (1 - tx) + coarse[ix + 1][iz + 1] * tx
        return a * (1 - tz) + b * tz
    return sample


def tree(v, rng, kind, x, y, z, limit):
    """Small persistent tree rooted on the block at (x, y, z); skipped when it would leave the template."""
    h = rng.randint(4, 5) if kind != "spruce" else rng.randint(6, 8)
    if x - 3 < 0 or z - 3 < 0 or x + 3 >= limit[0] or z + 3 >= limit[1] or y + h + 3 >= limit[2]:
        return
    log, leaf = {"oak": ("oak_log", "oak_leaves"), "birch": ("birch_log", "birch_leaves"), "spruce": ("spruce_log", "spruce_leaves")}[kind]
    for i in range(1, h + 1):
        v.log(x, y + i, z, log)
    if kind == "spruce":
        for idx, r in enumerate([2, 2, 1, 1, 0]):
            ly = y + h - 3 + idx
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) <= r:
                        v.leaves(x + dx, ly, z + dz, leaf)
        return
    top = y + h
    for dy in range(-1, 3):
        r = 2 if dy < 1 else 1
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) == r and abs(dz) == r and (dy == 2 or rng.random() < 0.5):
                    continue
                if dx == 0 and dz == 0 and dy < 1:
                    continue
                v.leaves(x + dx, top + dy, z + dz, leaf)


def _water_cells(rng, kind, cx, cz, rx, rz, surface, blocked):
    """Cells (x, z) of a small enclosed brook or pond kept well inside the plateau (d <= 0.62) so water never reaches the rim."""
    def inside(x, z):
        return (x, z) in surface and surface[x, z][1] <= 0.62 and not blocked(x, z)
    cells = set()
    if kind == "pond":
        for _ in range(40):  # first candidate centre whose pond fits entirely on free plateau ground
            px = cx + rng.randint(-rx // 2, rx // 2)
            pz = cz + rng.randint(-rz // 2, rz // 2)
            pr = rng.choice([3, 4])
            cand = set()
            for x in range(px - pr - 1, px + pr + 2):
                for z in range(pz - pr - 1, pz + pr + 2):
                    if ((x - px) ** 2 / (pr + rng.random() * 0.8) ** 2 + (z - pz) ** 2 / (pr - 0.5 + rng.random() * 0.8) ** 2) <= 1.0:
                        cand.add((x, z))
            if cand and all(inside(*c) for c in cand):
                cells = cand
                break
    elif kind == "brook":
        a0 = rng.random() * 6.28
        sweep = 2.3 + rng.random() * 0.8
        wobble, phase = 0.06 + rng.random() * 0.05, rng.random() * 6.28
        steps = int(max(rx, rz) * 2.6)
        for i in range(steps + 1):
            t = i / steps
            ang = a0 + sweep * t
            rad = 0.48 + wobble * math.sin(t * 9.0 + phase) + 0.10 * math.sin(t * 3.14)
            bx, bz = cx + rx * rad * math.cos(ang), cz + rz * rad * math.sin(ang)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if abs(dx) + abs(dz) <= 1 or t % 0.2 < 0.1:
                        cells.add((round(bx) + dx, round(bz) + dz))
    cells = {c for c in cells if inside(*c)}
    # every water cell needs land (or water) on all four sides at the same level, and at least a one-block bank
    cells = {c for c in cells if all(((c[0] + dx, c[1] + dz) in surface and surface[c[0] + dx, c[1] + dz][1] <= 0.7)
                                     for dx in (-1, 0, 1) for dz in (-1, 0, 1))}
    return cells


def build_island(style, name, rx, rz, depth, seed, reserved=(), top_socket=None, links=(), margin=2, link_pad=5, clearance=14,
                 veg_scale=1.0, link_dy=None, water=None, child_socket=None):
    """Create one island template.

    reserved: rectangles (x0, z0, x1, z1) kept free of vegetation (structure footprints).
    top_socket: pool name for the structure jigsaw on the plateau centre, or None.
    links: list of (side, pool) with side in east/west; link_dy shifts the child's plateau relative to ours.
    clearance: empty template height above the plateau. A jigsaw child attached inside this piece's own bounding box must fit
    entirely inside that box, so a structure socket needs the structure's full height reserved here.
    """
    spec = STYLES[style]
    rng = random.Random(f"{name}/{style}/{seed}")
    ytop = depth + 1
    pad_w = link_pad if any(s == "west" for s, _ in links) else margin
    pad_e = link_pad if any(s == "east" for s, _ in links) else margin
    cx, cz = pad_w + rx, margin + rz
    width, length = cx + rx + 1 + pad_e, cz + rz + 1 + margin
    height = ytop + clearance
    v = Vox((width, height, length))
    noise = _noise(rng, max(width, length) + 8)
    phase1, phase2 = rng.random() * 6.28, rng.random() * 6.28
    surface = {}
    for x in range(width):
        for z in range(length):
            dx, dz = (x - cx) / rx, (z - cz) / rz
            ang = math.atan2(dz, dx)
            mod = 1.0 + 0.09 * math.sin(3 * ang + phase1) + 0.06 * math.sin(5 * ang + phase2) + 0.10 * (noise(x, z) - 0.5)
            d = math.hypot(dx, dz) / mod
            if d > 1.0:
                continue
            edge = max(0.0, (d - 0.80) / 0.20)
            surf = ytop - int(edge ** 1.4 * 2.99)
            bottom = ytop - round(depth * (1 - d ** 2) ** 0.9 + (noise(x + 31, z + 17) - 0.5) * 3)
            bottom = min(bottom, surf - 2)
            bottom = max(bottom, 0)
            if d < 0.55 and rng.random() < 0.07:
                bottom = max(0, bottom - rng.randint(2, 5))
            surface[x, z] = (surf, d)
            for y in range(bottom, surf + 1):
                dist = surf - y
                if dist == 0:
                    block = spec["top"]
                elif dist <= 2:
                    block = rng.choice(spec["sub"])
                else:
                    band = spec["body"]
                    block = band[(y + (x + z) // 7) % len(band)] if style == "badlands" else rng.choice(band)
                if block in GRAVITY and (y == bottom):
                    block = "stone"
                v.put(x, y, z, block)
    # vegetation
    def reserved_at(x, z):
        return any(x0 - 1 <= x <= x1 + 1 and z0 - 1 <= z <= z1 + 1 for x0, z0, x1, z1 in reserved)
    # optional enclosed water (medium/large variants only): the 1-2 top layers are carved, banks stay at the surface level
    water_cells = set()
    if water:
        water_cells = _water_cells(random.Random(f"{name}/{style}/{seed}/water"), water, cx, cz, rx, rz, surface,
                                   reserved_at)
        deep = 2 if water == "pond" else 1
        for (x, z) in water_cells:
            surf = surface[x, z][0]
            v.remove(x, surf, z)
            for k in range(1, deep + 1):
                v.put(x, surf - k, z, "water", dict(level="0"))
    trees = []
    for (x, z), (surf, d) in sorted(surface.items()):
        if d > 0.88 or reserved_at(x, z) or abs(x - cx) + abs(z - cz) == 0 or (x, z) in water_cells:
            continue
        r = rng.random()
        s = veg_scale
        y = surf + 1
        veg = spec["veg"]
        if veg == "snowy":
            if r < 0.55 * s:
                v.put(x, y, z, "snow", dict(layers="1"))
            elif r > 0.988 and all(abs(x - tx) + abs(z - tz) > 5 for tx, tz in trees):
                trees.append((x, z)); tree(v, rng, "spruce", x, surf, z, (width, length, height))
        elif veg == "meadow":
            if r < 0.22 * s:
                v.put(x, y, z, "short_grass")
            elif r < 0.27 * s:
                v.put(x, y, z, rng.choice(FLOWERS))
            elif r < 0.285 * s and y + 1 < height:
                v.put(x, y, z, "tall_grass", dict(half="lower")); v.put(x, y + 1, z, "tall_grass", dict(half="upper"))
            elif r < 0.292 * s and all(abs(x - tx) + abs(z - tz) > 7 for tx, tz in trees):
                trees.append((x, z)); tree(v, rng, "oak", x, surf, z, (width, length, height))
        elif veg == "forest":
            if r < 0.18 * s:
                v.put(x, y, z, "short_grass")
            elif r < 0.22 * s:
                v.put(x, y, z, "fern")
            elif r < 0.235 * s:
                v.put(x, y, z, rng.choice(FLOWERS))
            elif r < 0.275 * s and all(abs(x - tx) + abs(z - tz) > 5 for tx, tz in trees):
                trees.append((x, z)); tree(v, rng, rng.choice(["oak", "oak", "birch"]), x, surf, z, (width, length, height))
        elif veg == "badlands":
            if r < 0.025 * s:
                v.put(x, y, z, "dead_bush")
            elif r < 0.032 * s:
                for i in range(rng.randint(1, 3)):
                    if y + i < height:
                        v.put(x, y + i, z, rng.choice(["terracotta", "orange_terracotta"]))
        elif veg == "desert":
            if r < 0.03 * s:
                v.put(x, y, z, "dead_bush")
            elif r < 0.038 * s and all(abs(x - tx) + abs(z - tz) > 3 for tx, tz in trees):
                trees.append((x, z))
                for i in range(rng.randint(1, 3)):
                    v.put(x, y + i, z, "cactus", dict(age="0"))
        elif veg == "rocky":
            if r < 0.05 * s:
                v.put(x, y, z, "cobblestone")
            elif r < 0.07 * s:
                v.put(x, y, z, "mossy_cobblestone")
            elif r < 0.075 * s:
                v.put(x, y, z, "moss_block")
    # plateau socket for the structure, replacing the surface block of the centre cell
    if top_socket:
        surf_here = surface[cx, cz][0]
        v.jigsaw(cx, surf_here, cz, "up_north", TOP, TOP, top_socket, "minecraft:" + spec["top"])
    if child_socket:
        # A composite child (the Sky Tower) attaches here. Its own connector sits on the plateau level, so this
        # parent connector is buried one block below the surface and is replaced by the subsurface block.
        px, pz, child_pool = child_socket
        surf_here = surface[px, pz][0]
        v.jigsaw(px, surf_here - 1, pz, "up_north", TOP, TOP, child_pool, "minecraft:" + spec["sub"][0])
    for side, pool in links:
        edge_x = 0 if side == "west" else width - 1
        dy = (link_dy or {}).get(side, 0)
        v.jigsaw(edge_x, ytop - dy, cz, "west_up" if side == "west" else "east_up", LINK, LINK, pool, "minecraft:air")
    return v, (cx, cz, ytop)
