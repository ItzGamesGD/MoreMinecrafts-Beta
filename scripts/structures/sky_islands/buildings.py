"""Floating ruins (4 variants), aerial trial tower (5-7 floors) and the Sky Temple. Local origin y=0 is the foundation layer."""
import math
import random

from voxel import Vox, DELTA, OPPOSITE
from islands import TOP

CHEST = "moreminecrafts:chests/sky_islands/chest"
VAULT = "moreminecrafts:chests/sky_islands/vault"
VAULT_TOP = "moreminecrafts:chests/sky_islands/vault_tower_top"

# Vanilla trial spawner configurations (data/minecraft/trial_spawner/trial_chamber/...) plus the one approved shulker addition.
SPAWNERS = {
    "zombie": "minecraft:trial_chamber/melee/zombie",
    "husk": "minecraft:trial_chamber/melee/husk",
    "spider": "minecraft:trial_chamber/melee/spider",
    "skeleton": "minecraft:trial_chamber/ranged/skeleton",
    "stray": "minecraft:trial_chamber/ranged/stray",
    "poison_skeleton": "minecraft:trial_chamber/ranged/poison_skeleton",
    "slime": "minecraft:trial_chamber/small_melee/slime",
    "cave_spider": "minecraft:trial_chamber/small_melee/cave_spider",
    "silverfish": "minecraft:trial_chamber/small_melee/silverfish",
    "baby_zombie": "minecraft:trial_chamber/small_melee/baby_zombie",
    "breeze": "minecraft:trial_chamber/breeze",
    "shulker": "moreminecrafts:sky_islands/shulker",
}


def add_spawner(v, x, y, z, kind):
    base = SPAWNERS[kind]
    v.spawner(x, y, z, base + "/normal", base + "/ominous")


def brick(rng, moss=0.3, crack=0.2):
    r = rng.random()
    if r < moss:
        return "mossy_stone_bricks"
    if r < moss + crack:
        return "cracked_stone_bricks"
    return "stone_bricks"


def root_socket(v, cx, final_state):
    v.jigsaw(cx[0], 0, cx[1], "down_north", TOP, TOP, "minecraft:empty", final_state)


# ======================================================================================================
# Floating ruins: four 15 x 15 variants
# ======================================================================================================
def _foundation(v, rng, size, hole=0.10):
    for x in range(size):
        for z in range(size):
            edge = x in (0, size - 1) or z in (0, size - 1)
            if edge and rng.random() < hole:
                continue
            v.put(x, 0, z, brick(rng, 0.35, 0.25))


def _debris(v, rng, size, count, y=1):
    for _ in range(count):
        x, z = rng.randrange(1, size - 1), rng.randrange(1, size - 1)
        if v.get(x, y, z) is None and v.get(x, y - 1, z):
            v.put(x, y, z, rng.choice(["cobblestone", "mossy_cobblestone", "cracked_stone_bricks", "moss_block"]))
            if rng.random() < 0.2:
                v.put(x, y + 1, z, "cobweb")


def ruin_foundations(rng):
    """Variant A: the collapsed footing of a small house."""
    v = Vox((15, 12, 15))
    _foundation(v, rng, 15)
    for x in range(2, 13):
        for z in range(3, 12):
            v.put(x, 0, z, rng.choice(["polished_andesite", "stone_bricks", "mossy_stone_bricks", "smooth_stone"]))
    for x in range(2, 13):
        for z in range(3, 12):
            if not (x in (2, 12) or z in (3, 11)):
                continue
            if z == 11 and 6 <= x <= 8:
                continue  # doorway
            height = rng.choice([1, 2, 2, 3, 4]) if (x in (2, 12) and z in (3, 11)) else rng.choice([0, 1, 1, 2, 3])
            for y in range(1, height + 1):
                v.put(x, y, z, brick(rng))
    add_spawner(v, 7, 1, 6, "zombie")
    v.vault(7, 1, 3, "south", VAULT)
    v.box(7, 2, 3, 7, 3, 3, "mossy_stone_bricks")  # keep the vault backed by a wall remnant
    v.chest(3, 1, 4, "east", CHEST)
    v.chest(11, 1, 10, "west", CHEST)
    _debris(v, rng, 15, 14)
    root_socket(v, (7, 7), "minecraft:stone_bricks")
    return v


def ruin_pillars(rng):
    """Variant B: a ring of broken pillars around a shrine dais."""
    v = Vox((15, 12, 15))
    _foundation(v, rng, 15, 0.08)
    for i in range(8):
        a = math.radians(45 * i + 22.5)
        x, z = round(7 + 6 * math.cos(a)), round(7 + 6 * math.sin(a))
        h = rng.choice([2, 3, 4, 6, 7])
        for y in range(1, h + 1):
            v.put(x, y, z, "chiseled_stone_bricks" if y == h and h < 7 and rng.random() < 0.4 else brick(rng, 0.4, 0.2))
        if rng.random() < 0.6:
            v.put(x, h + 1, z, "stone_brick_slab", dict(type="bottom", waterlogged="false"))
    # dais
    v.box(5, 1, 5, 9, 1, 9, "polished_andesite")
    for i in range(5, 10):
        v.stairs(i, 1, 4, "stone_brick_stairs", "south")
        v.stairs(i, 1, 10, "stone_brick_stairs", "north")
    for z in range(5, 10):
        v.stairs(4, 1, z, "stone_brick_stairs", "east")
        v.stairs(10, 1, z, "stone_brick_stairs", "west")
    add_spawner(v, 7, 2, 7, "skeleton")
    # shrine alcove with the vault
    v.vault(7, 1, 1, "south", VAULT)
    v.box(6, 1, 0, 8, 3, 0, "mossy_stone_bricks")
    v.box(6, 1, 1, 6, 3, 1, "stone_bricks")
    v.box(8, 1, 1, 8, 3, 1, "stone_bricks")
    v.box(6, 3, 1, 8, 3, 1, "stone_bricks")
    v.put(7, 3, 1, "chiseled_stone_bricks")
    v.chest(2, 1, 12, "north", CHEST)
    v.chest(12, 1, 12, "north", CHEST)
    _debris(v, rng, 15, 12)
    root_socket(v, (7, 7), "minecraft:stone_bricks")
    return v


def ruin_altar(rng):
    """Variant C: a stepped altar with a broken arch."""
    v = Vox((15, 12, 15))
    _foundation(v, rng, 15, 0.08)
    for k in range(3):
        for x in range(3 + k, 12 - k):
            for z in range(3 + k, 12 - k):
                v.put(x, 1 + k, z, brick(rng, 0.3, 0.2) if k < 2 else "chiseled_stone_bricks")
    for x in range(3, 12):
        v.stairs(x, 1, 12, "stone_brick_stairs", "north")
    add_spawner(v, 6, 4, 7, "spider")
    v.vault(8, 4, 7, "south", VAULT)
    # broken arches flanking the altar
    for z in (3, 11):
        for y in range(2, 2 + rng.choice([4, 5, 6])):
            v.put(5, y, z, brick(rng, 0.4, 0.2))
        for y in range(2, 2 + rng.choice([3, 5, 6])):
            v.put(9, y, z, brick(rng, 0.4, 0.2))
    v.chest(1, 1, 3, "east", CHEST)
    v.chest(13, 1, 3, "west", CHEST)
    _debris(v, rng, 15, 12)
    root_socket(v, (7, 7), "minecraft:stone_bricks")
    return v


def ruin_corridor(rng):
    """Variant D: a roofless corridor hall with a row of columns."""
    v = Vox((15, 12, 15))
    _foundation(v, rng, 15, 0.08)
    for x in range(0, 15):
        for z in range(4, 11):
            v.put(x, 0, z, rng.choice(["polished_andesite", "stone_bricks", "smooth_stone", "mossy_stone_bricks"]))
    for x in range(0, 15):
        for z in (4, 10):
            if rng.random() < 0.15:
                continue
            height = rng.choice([2, 3, 4, 5]) if x % 4 else rng.choice([4, 5, 6])
            for y in range(1, height + 1):
                if x % 4 == 2 and 2 <= y <= 3 and 1 <= x <= 13 and rng.random() < 0.7:
                    continue  # window-like gaps between piers
                v.put(x, y, z, brick(rng))
    for x in (3, 7, 11):
        for z in (5, 9):
            h = rng.choice([2, 3, 5])
            for y in range(1, h + 1):
                v.put(x, y, z, "chiseled_stone_bricks" if y == h else "stone_bricks")
    add_spawner(v, 7, 1, 7, "husk")
    v.vault(14, 1, 7, "west", VAULT)
    v.box(14, 2, 6, 14, 3, 8, "mossy_stone_bricks")
    v.chest(1, 1, 5, "south", CHEST)
    v.chest(1, 1, 9, "north", CHEST)
    _debris(v, rng, 15, 10)
    root_socket(v, (7, 7), "minecraft:stone_bricks")
    return v


RUINS = {"ruins_a": ruin_foundations, "ruins_b": ruin_pillars, "ruins_c": ruin_altar, "ruins_d": ruin_corridor}


# ======================================================================================================
# Aerial trial tower: 19 x 19 footprint, 5-7 floors of 7 blocks, outer wall box x/z 1..17
# ======================================================================================================
FLOOR_H = 7
SPAWNER_BY_FLOOR = ["zombie", "skeleton", "spider", "husk", "poison_skeleton", "slime", "breeze"]


def tower(floors, rng):
    size_y = floors * FLOOR_H + 4
    v = Vox((19, size_y, 19))
    cx = cz = 9

    def wall(x, y, z):
        v.put(x, y, z, brick(rng, 0.35, 0.18))

    # foundation + floor slabs (slab f at y = f * FLOOR_H; f = 0 is the foundation)
    for f in range(floors):
        y0 = f * FLOOR_H
        for x in range(1, 18):
            for z in range(1, 18):
                v.put(x, y0, z, "mossy_stone_bricks" if rng.random() < 0.3 else "stone_bricks" if f == 0 else
                      rng.choice(["stone_bricks", "polished_andesite", "cracked_stone_bricks"]))
    # outer walls with windows
    for f in range(floors):
        y0 = f * FLOOR_H
        top = f == floors - 1
        for y in range(y0 + 1, y0 + FLOOR_H):
            for i in range(1, 18):
                for (x, z) in ((i, 1), (i, 17), (1, i), (17, i)):
                    wall(x, y, z)
        # arrow-slit / arch windows
        for side in range(4):
            for i in (4, 8, 12):
                if rng.random() < 0.25:
                    continue
                x, z = [(i, 1), (i, 17), (1, i), (17, i)][side]
                for y in (y0 + 3, y0 + 4):
                    v.remove(x, y, z)
        if top:
            for i in range(1, 18):
                for (x, z) in ((i, 1), (i, 17), (1, i), (17, i)):
                    if i % 2 == 0:
                        wall(x, y0 + FLOOR_H, z)  # crenellations
    # ground-floor doorway (south wall z = 17 is "front")
    for x in (8, 9, 10):
        for y in (1, 2, 3):
            v.remove(x, y, 17)
    for x in (8, 9, 10):
        v.stairs(x, 0, 18, "stone_brick_stairs", "north")
    v.put(9, 4, 17, "chiseled_stone_bricks")
    # stair flights between floors, alternating west / east wall
    for f in range(floors - 1):
        y0 = f * FLOOR_H
        if f % 2 == 0:
            for i in range(1, 8):
                for x in (2, 3):
                    v.stairs(x, y0 + i, 1 + i, "stone_brick_stairs", "south")
                    for yy in range(y0 + 1, y0 + i):
                        v.put(x, yy, 1 + i, "stone_bricks")
            for z in (6, 7):
                for x in (2, 3):
                    v.remove(x, y0 + FLOOR_H, z)
        else:
            for i in range(1, 8):
                for x in (14, 15):
                    v.stairs(x, y0 + i, 16 - i, "stone_brick_stairs", "north")
                    for yy in range(y0 + 1, y0 + i):
                        v.put(x, yy, 16 - i, "stone_bricks")
            for z in (10, 11):
                for x in (14, 15):
                    v.remove(x, y0 + FLOOR_H, z)
    # two exterior stairs with landings and wall doors (west -> floor 1, east -> floor 2)
    if floors >= 3:
        for (xs, flight_floor, side) in ((0, 0, "west"), (18, 1, "east")):
            y0 = flight_floor * FLOOR_H
            for i in range(1, 8):
                z = 1 + i if side == "west" else 17 - i
                facing = "south" if side == "west" else "north"
                v.stairs(xs, y0 + i, z, "stone_brick_stairs", facing)
                for yy in range(0, y0 + i):
                    v.put(xs, yy, z, "mossy_stone_bricks" if rng.random() < 0.3 else "stone_bricks")
            land_z = 9
            for yy in range(0, y0 + FLOOR_H + 1):
                v.put(xs, yy, land_z, "stone_bricks")
            wx = 1 if side == "west" else 17
            for y in (y0 + FLOOR_H + 1, y0 + FLOOR_H + 2):
                v.remove(wx, y, land_z)
    # partitions: low broken walls with wide gaps on every floor but the first and the last (wide arenas)
    for f in range(1, floors - 1):
        y0 = f * FLOOR_H
        for i in range(2, 17):
            if not (5 <= i <= 7 or 11 <= i <= 13):
                if rng.random() < 0.85:
                    for y in range(y0 + 1, y0 + rng.choice([2, 3, 3]) + 1):
                        v.put(9, y, i, brick(rng))
            if not (2 <= i <= 4 or 13 <= i <= 16):
                if rng.random() < 0.85:
                    for y in range(y0 + 1, y0 + rng.choice([2, 3, 3]) + 1):
                        v.put(i, y, 9, brick(rng))
    # south-face balconies with a door on every upper floor
    for f in range(1, floors):
        y0 = f * FLOOR_H
        for x in range(6, 13):
            v.put(x, y0, 18, "stone_brick_slab", dict(type="top", waterlogged="false"))
        for y in (y0 + 1, y0 + 2):
            for x in (8, 9, 10):
                v.remove(x, y, 17)
    # floor furnishing
    for f in range(floors):
        y0 = f * FLOOR_H
        top = f == floors - 1
        kind = SPAWNER_BY_FLOOR[f % len(SPAWNER_BY_FLOOR)]
        ground = f == 0
        spawn_spots = [(13, 5)]
        if ground or top:
            spawn_spots.append((5, 13) if not top else (9, 13))
        if top:
            spawn_spots = [(6, 6), (12, 6)]
        for i, (sx, sz) in enumerate(spawn_spots):
            add_spawner(v, sx, y0 + 1, sz, kind if i == 0 else ("shulker" if top else "skeleton" if kind != "skeleton" else "zombie"))
        # vaults sit against the walls, one per floor (three on the crown), loot table is the crown table on the top floor
        if top:
            vault_spots = [(9, 16, "north"), (2, 12, "east"), (16, 12, "west")]
            chest_spots = [(2, 5, "east"), (16, 5, "west"), (5, 16, "north"), (13, 16, "north")]
            table = VAULT_TOP
        else:
            vault_spots = [(2, 14, "east")] if f % 2 == 0 else [(16, 5, "west")]
            chest_spots = [(8, 2, "south"), (11, 16, "north")] if f % 2 == 0 else [(5, 16, "north"), (8, 2, "south")]
            table = VAULT
        for (x, z, facing) in vault_spots:
            v.vault(x, y0 + 1, z, facing, table)
        for (x, z, facing) in chest_spots:
            v.chest(x, y0 + 1, z, facing, CHEST)
    # clear furnishing cells that the partition/stair passes may have overwritten, then ruin the upper walls
    protected = {pos for pos, (name, _, _) in v.blocks.items() if name in ("trial_spawner", "vault", "chest", "jigsaw")}
    for pos, (name, _, _) in list(v.blocks.items()):
        x, y, z = pos
        on_wall = x in (1, 17) or z in (1, 17)
        if on_wall and name.endswith("bricks") and pos not in protected:
            rel = y % FLOOR_H
            floor = y // FLOOR_H
            p = 0.05 + 0.10 * (rel / FLOOR_H) + (0.12 if floor >= floors - 2 else 0)
            if rel >= 4 and rng.random() < p and not (z == 17 and x in (8, 9, 10)):
                v.remove(x, y, z)
    # root socket
    root_socket(v, (cx, cz), "minecraft:stone_bricks")
    # a collapsed corner on the two top floors
    cxn, czn = (1, 1) if rng.random() < 0.5 else (17, 1)
    for (x, y, z) in list(v.blocks):
        if y >= (floors - 2) * FLOOR_H + 3 and abs(x - cxn) + abs(z - czn) <= 4 and (x, y, z) not in protected and v.get(x, y, z) and v.get(x, y, z).endswith("bricks") and (x in (1, 17) or z in (1, 17)):
            v.remove(x, y, z)
    # moss/debris near the entrance
    for x in range(6, 13):
        for z in (18,):
            if rng.random() < 0.4 and x not in (8, 9, 10):
                v.put(x, 0, z, "mossy_cobblestone")
    return v


# ======================================================================================================
# Sky Temple: 45 x 27 open single-storey Greek temple around a sunken central trial arena
# ======================================================================================================
W, D = 45, 27
CX, CZ = 22, 13


def temple(rng):
    v = Vox((W, 22, D))

    def marble(weathered=0.18):
        r = rng.random()
        if r < weathered * 0.5:
            return "mossy_stone_bricks"
        if r < weathered:
            return "cracked_stone_bricks"
        return rng.choice(["smooth_quartz", "smooth_quartz", "quartz_block", "calcite"])

    # stylobate: three inset steps, stair rim facing inwards
    for k in range(3):
        for x in range(k, W - k):
            for z in range(k, D - k):
                rim = x in (k, W - 1 - k) or z in (k, D - 1 - k)
                if k < 2 and rim:
                    if z in (k, D - 1 - k) and k <= x < W - k:
                        facing = "south" if z == k else "north"
                    else:
                        facing = "east" if x == k else "west"
                    corner = (x in (k, W - 1 - k)) and (z in (k, D - 1 - k))
                    if corner:
                        v.put(x, k, z, marble(0.1))
                    else:
                        v.stairs(x, k, z, "smooth_quartz_stairs", facing)
                else:
                    v.put(x, k, z, marble(0.12) if k < 2 else
                          ("polished_diorite" if (x + z) % 2 == 0 else "smooth_quartz"))
    # sunken central arena (21 x 13) carved out of the floor, rim of chiselled marble, ringed floor
    ax0, ax1, az0, az1 = 12, 32, 7, 19
    for x in range(ax0, ax1 + 1):
        for z in range(az0, az1 + 1):
            ring = min(x - ax0, ax1 - x, z - az0, az1 - z)
            v.put(x, 1, z, "smooth_stone" if ring % 3 == 0 else "stone_bricks" if ring % 3 == 1 else "polished_andesite")
            v.remove(x, 2, z)
    for x in range(ax0 - 1, ax1 + 2):
        for z in (az0 - 1, az1 + 1):
            v.put(x, 2, z, "chiseled_quartz_block" if (x - ax0) % 4 == 0 else marble(0.1))
    for z in range(az0 - 1, az1 + 2):
        for x in (ax0 - 1, ax1 + 1):
            v.put(x, 2, z, "chiseled_quartz_block" if (z - az0) % 4 == 0 else marble(0.1))
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if dx * dx + dz * dz <= 10:
                v.put(CX + dx, 1, CZ + dz, "chiseled_stone_bricks")
    # stairs down into the arena on the four sides
    for z in (CZ - 1, CZ, CZ + 1):
        v.stairs(ax0 - 1, 2, z, "smooth_quartz_stairs", "west", "bottom")
        v.stairs(ax1 + 1, 2, z, "smooth_quartz_stairs", "east", "bottom")
    for x in (CX - 1, CX, CX + 1):
        v.stairs(x, 2, az0 - 1, "smooth_quartz_stairs", "north", "bottom")
        v.stairs(x, 2, az1 + 1, "smooth_quartz_stairs", "south", "bottom")
    add_spawner(v, CX - 7, 2, CZ, "zombie")
    add_spawner(v, CX + 7, 2, CZ, "shulker")
    # peristyle: 13 x 7 columns, 9 high with capitals and entablature
    xs = list(range(4, 41, 3))
    zs = list(range(4, 23, 3))
    columns = [(x, z) for x in xs for z in (4, 22)] + [(x, z) for x in (4, 40) for z in zs[1:-1]]
    for (x, z) in columns:
        top_y = rng.choice([4, 6, 8, 11, 11, 11, 11, 11, 11])
        v.put(x, 3, z, "smooth_quartz")
        for y in range(4, top_y + 1):
            v.put(x, y, z, "quartz_pillar", dict(axis="y"))
        if top_y == 11:
            v.put(x, 12, z, "chiseled_quartz_block")
            v.put(x, 13, z, "smooth_quartz_slab", dict(type="bottom", waterlogged="false"))
        else:
            # fallen drum next to a broken column
            dx, dz = rng.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
            fx, fz = x + dx, z + dz
            if 2 <= fx < W - 2 and 2 <= fz < D - 2 and v.get(fx, 3, fz) is None and (fx, fz) not in columns:
                v.put(fx, 3, fz, "quartz_pillar", dict(axis="x" if dx else "z"))
    # entablature between intact capitals, only where both neighbours survived
    def intact(x, z):
        return v.get(x, 12, z) == "chiseled_quartz_block"
    for z in (4, 22):
        for x in range(4, 40, 3):
            if intact(x, z) and intact(x + 3, z):
                for xx in range(x, x + 4):
                    v.put(xx, 14, z, "smooth_quartz")
                    v.put(xx, 15, z, "quartz_bricks" if xx % 2 == 0 else "chiseled_quartz_block")
                    v.slab(xx, 16, z, "smooth_quartz_slab")
    for x in (4, 40):
        for z in range(7, 19, 3):
            if intact(x, z) and intact(x, z + 3):
                for zz in range(z, z + 4):
                    v.put(x, 14, zz, "smooth_quartz")
                    v.put(x, 15, zz, "quartz_bricks")
                    v.slab(x, 16, zz, "smooth_quartz_slab")
    # broken pediments on both short ends
    for x in (4, 40):
        for z in range(4, 23):
            h = 4 - abs(z - CZ) * 4 // 9
            for y in range(17, 17 + max(0, h)):
                if rng.random() < 0.72:
                    v.put(x, y, z, "smooth_quartz" if y == 17 + h - 1 or rng.random() < 0.8 else "calcite")
    # side chambers flanking the arena (west and east), each with a vault and two chests
    def chamber(x0, x1, facing_in):
        z0, z1 = 7, 19
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    if (x == (x1 if facing_in == "east" else x0)) and CZ - 1 <= z <= CZ + 1:
                        continue  # doorway towards the arena
                    height = rng.choice([4, 5, 6, 6, 7])
                    for y in range(3, 3 + height):
                        v.put(x, y, z, marble(0.22))
    chamber(6, 10, "east")
    chamber(34, 38, "west")
    v.vault(7, 3, CZ, "east", VAULT)
    v.vault(37, 3, CZ, "west", VAULT)
    v.chest(8, 3, 8, "south", CHEST)
    v.chest(8, 3, 18, "north", CHEST)
    v.chest(36, 3, 8, "south", CHEST)
    v.chest(36, 3, 18, "north", CHEST)
    # rubble, moss and fallen blocks
    for _ in range(70):
        x, z = rng.randrange(2, W - 2), rng.randrange(2, D - 2)
        in_chamber = (6 <= x <= 10 or 34 <= x <= 38) and 7 <= z <= 19
        if v.get(x, 3, z) is None and v.get(x, 2, z) and not in_chamber and not (ax0 - 1 <= x <= ax1 + 1 and az0 - 1 <= z <= az1 + 1):
            v.put(x, 3, z, rng.choice(["moss_block", "cracked_stone_bricks", "mossy_cobblestone", "smooth_quartz_slab"]))
            if v.get(x, 3, z) == "smooth_quartz_slab":
                v.slab(x, 3, z, "smooth_quartz_slab")
    root_socket(v, (CX, CZ), "minecraft:smooth_quartz")
    return v
