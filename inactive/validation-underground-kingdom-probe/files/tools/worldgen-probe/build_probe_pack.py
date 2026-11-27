#!/usr/bin/env python3
"""Builds the experimental "underground kingdom" worldgen probe datapack (Minecraft 26.3).

PROBE ONLY. All dimensions are provisional. Nothing here is shipped in the mod jar; the pack is
generated into a world's datapacks folder by tools/worldgen-probe/run_matrix.sh.

Layout (one central chamber, two corridors, one combat room, plus a sealed end cap so corridor B
has a terminus):

    [end_cap] -- corridor_b -- [central_chamber] -- corridor_a -- [combat_room: trial spawner + vault]

The only difference between the "encapsulate" and "none" variants is the structure's
`terrain_adaptation`; templates, pools, seeds and placement are byte-identical.
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402

NS = "moreminecrafts"
SHELL = "minecraft:tuff_bricks"
AIR = "minecraft:air"
DATA_VERSION = 5023  # Minecraft 26.3 world_version

# Provisional dimensions (x, y, z) in template space; shell is 1 block thick on every face.
CENTRAL = (23, 12, 23)
CORRIDOR = (5, 6, 24)
COMBAT = (17, 10, 17)
CAP = (5, 6, 5)
DOOR_W, DOOR_H = 3, 4  # doorway cross-section, matches corridor interior

PROFILES = {  # start_height (absolute Y) ranges; "mid" equals vanilla Trial Chambers
    "mid": (-40, -20),
    "deep": (-56, -44),
    "high": (20, 40),
    "deepnopad": (-56, -44),  # same heights as "deep" but dimension_padding 0 (control for the padding finding)
}
PLACEMENTS = {
    "dense": dict(spacing=12, separation=6, salt=20261017),
    "denselate": dict(spacing=12, separation=6, salt=20261017),  # same positions as dense; structure step = strongholds (after geodes/fossils/dungeons)
    "densemtn": dict(spacing=12, separation=6, salt=20261017),  # same as dense; harness filters to high-terrain start chunks
    "coincident": dict(spacing=34, separation=12, salt=94251327),  # shares vanilla Trial Chambers' start chunks
    "excluded": dict(spacing=12, separation=6, salt=20261017,
                     exclusion_zone={"other_set": "minecraft:trial_chambers", "chunk_count": 12}),
    # not a separate set: our structure is added (weight 1) to the *vanilla* minecraft:trial_chambers set,
    # so every start chunk picks vanilla OR ours (weighted). Writes an override of a vanilla file.
    "shared": dict(spacing=34, separation=12, salt=94251327),
}


BARE = False  # set from --shell


class Template:
    def __init__(self, size):
        self.size = size
        self.palette = []
        self.blocks = {}

    def state(self, block_id, props=None):
        entry = (block_id, tuple(sorted((props or {}).items())))
        if entry not in self.palette:
            self.palette.append(entry)
        return self.palette.index(entry)

    def shell(self):
        """Built shell: 1-thick tuff-brick box, air inside. Bare: interior air only; the surrounding
        rock is whatever terrain generation left there (the "carved cavern in rock" case)."""
        sx, sy, sz = self.size
        for x in range(sx):
            for y in range(sy):
                for z in range(sz):
                    edge = x in (0, sx - 1) or y in (0, sy - 1) or z in (0, sz - 1)
                    if edge and BARE:
                        continue
                    self.blocks[(x, y, z)] = (self.state(SHELL if edge else AIR), None)

    def air_box(self, x0, y0, z0, x1, y1, z1):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.blocks[(x, y, z)] = (self.state(AIR), None)

    def door_z(self, z, cx):
        """Open a DOOR_W x DOOR_H doorway centred on cx through the wall layer at z."""
        half = DOOR_W // 2
        self.air_box(cx - half, 1, z, cx + half, DOOR_H, z)

    def jigsaw(self, pos, orientation, name, target, pool, joint="aligned", final="minecraft:air"):
        sid = self.state("minecraft:jigsaw", {"orientation": orientation})
        tag = nbt.compound({
            "id": nbt.string("minecraft:jigsaw"),
            "name": nbt.string(name),
            "target": nbt.string(target),
            "pool": nbt.string(pool),
            "joint": nbt.string(joint),
            "final_state": nbt.string(final),
            "placement_priority": nbt.integer(0),
            "selection_priority": nbt.integer(0),
        })
        self.blocks[pos] = (sid, tag)

    def dump(self):
        palette = []
        for block_id, props in self.palette:
            e = {"id": nbt.string(block_id)}
            if props:
                e["properties"] = nbt.compound({k: nbt.string(v) for k, v in props})
            palette.append(nbt.compound(e))
        blocks = []
        for pos in sorted(self.blocks):
            sid, tag = self.blocks[pos]
            b = {"pos": nbt.lst(nbt.INT, [nbt.integer(c) for c in pos]), "state": nbt.integer(sid)}
            if tag is not None:
                b["nbt"] = tag
            blocks.append(nbt.compound(b))
        root = nbt.compound({
            "DataVersion": nbt.integer(DATA_VERSION),
            "size": nbt.lst(nbt.INT, [nbt.integer(c) for c in self.size]),
            "palette": nbt.lst(nbt.COMPOUND, palette),
            "blocks": nbt.lst(nbt.COMPOUND, blocks),
            "entities": nbt.lst(nbt.COMPOUND, []),
        })
        return nbt.dumps(root)


def central_chamber():
    t = Template(CENTRAL)
    t.shell()
    sx, sy, sz = CENTRAL
    cx = sx // 2
    t.door_z(0, cx)
    t.door_z(sz - 1, cx)
    t.jigsaw((cx, 1, 0), "north_up", f"{NS}:probe/chamber_door", f"{NS}:probe/corridor_in", f"{NS}:probe/corridor_a")
    t.jigsaw((cx, 1, sz - 1), "south_up", f"{NS}:probe/chamber_door", f"{NS}:probe/corridor_in", f"{NS}:probe/corridor_b")
    return t


def corridor(far_pool):
    t = Template(CORRIDOR)
    t.shell()
    sx, sy, sz = CORRIDOR
    cx = sx // 2
    t.air_box(1, 1, 0, sx - 2, DOOR_H, 0)  # open both ends
    t.air_box(1, 1, sz - 1, sx - 2, DOOR_H, sz - 1)
    t.jigsaw((cx, 1, 0), "north_up", f"{NS}:probe/corridor_in", "minecraft:empty", "minecraft:empty")
    t.jigsaw((cx, 1, sz - 1), "south_up", f"{NS}:probe/corridor_out", f"{NS}:probe/room_door", far_pool)
    return t


def room(size, with_contents):
    t = Template(size)
    t.shell()
    sx, sy, sz = size
    cx = sx // 2
    t.door_z(0, cx)
    t.jigsaw((cx, 1, 0), "north_up", f"{NS}:probe/room_door", "minecraft:empty", "minecraft:empty")
    if with_contents:
        # Vanilla two-step connector chains (same host config vanilla chambers use): a trial spawner
        # in the middle of the floor, a vault near the back wall. Both are 100% vanilla templates.
        t.jigsaw((cx, 3, sz // 2), "east_up", "minecraft:empty", "minecraft:spawner_connector",
                 "minecraft:trial_chambers/spawner/melee")
        t.jigsaw((cx, 3, sz - 5), "north_up", "minecraft:empty", "minecraft:reward_connector",
                 "minecraft:trial_chambers/reward/all")
    return t


def pool(*locations):
    return {
        "elements": [{
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": loc,
                "processors": {"processors": []},
                "projection": "rigid",
            },
            "weight": 1,
        } for loc in locations],
        "fallback": "minecraft:empty",
    }


def structure_json(adaptation, profile, step="underground_structures"):
    lo, hi = PROFILES[profile]
    return {
        "type": "minecraft:jigsaw",
        "biomes": "#minecraft:is_overworld",
        "dimension_padding": 0 if profile == "deepnopad" else 10,
        "liquid_settings": "ignore_waterlogging",
        "max_distance_from_center": 64,
        "pool_aliases": [{
            "type": "minecraft:random",
            "alias": "minecraft:trial_chambers/spawner/contents/melee",
            "targets": [
                {"data": "minecraft:trial_chambers/spawner/melee/zombie", "weight": 1},
                {"data": "minecraft:trial_chambers/spawner/melee/husk", "weight": 1},
                {"data": "minecraft:trial_chambers/spawner/melee/spider", "weight": 1},
            ],
        }],
        "size": 8,
        "spawn_overrides": {},
        "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}},
        "start_pool": f"{NS}:probe/start",
        "step": step,
        "terrain_adaptation": adaptation,
        "use_expansion_hack": False,
    }


def shared_vanilla_set_json():
    out = structure_set_json("shared")
    out["structures"] = [{"structure": "minecraft:trial_chambers", "weight": 1}] + out["structures"]
    return out


def structure_set_json(placement):
    p = dict(PLACEMENTS[placement])
    exclusion = p.pop("exclusion_zone", None)
    out = {"type": "minecraft:random_spread", **p}
    if exclusion:
        out["exclusion_zone"] = exclusion
    return {"placement": out, "structures": [{"structure": f"{NS}:probe_kingdom", "weight": 1}]}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(path, mode) as f:
        f.write(data if isinstance(data, bytes) else json.dumps(data, indent=2, sort_keys=True) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--adaptation", choices=["encapsulate", "none", "bury", "beard_box", "beard_thin"], required=True)
    ap.add_argument("--profile", choices=sorted(PROFILES), default="mid")
    ap.add_argument("--placement", choices=sorted(PLACEMENTS), default="dense")
    ap.add_argument("--shell", choices=["built", "bare"], default="built")
    a = ap.parse_args()
    global BARE
    BARE = a.shell == "bare"

    write(os.path.join(a.out, "pack.mcmeta"),
          {"pack": {"description": "MoreMinecrafts underground kingdom worldgen probe (experimental)",
                    "min_format": 121, "max_format": 121}})
    d = os.path.join(a.out, "data", NS)
    s = os.path.join(d, "structure", "probe")
    write(os.path.join(s, "central_chamber.nbt"), central_chamber().dump())
    write(os.path.join(s, "corridor_a.nbt"), corridor(f"{NS}:probe/combat").dump())
    write(os.path.join(s, "corridor_b.nbt"), corridor(f"{NS}:probe/end_cap").dump())
    write(os.path.join(s, "combat_room.nbt"), room(COMBAT, True).dump())
    write(os.path.join(s, "end_cap.nbt"), room(CAP, False).dump())
    pools = os.path.join(d, "worldgen", "template_pool", "probe")
    write(os.path.join(pools, "start.json"), pool(f"{NS}:probe/central_chamber"))
    write(os.path.join(pools, "corridor_a.json"), pool(f"{NS}:probe/corridor_a"))
    write(os.path.join(pools, "corridor_b.json"), pool(f"{NS}:probe/corridor_b"))
    write(os.path.join(pools, "combat.json"), pool(f"{NS}:probe/combat_room"))
    write(os.path.join(pools, "end_cap.json"), pool(f"{NS}:probe/end_cap"))
    write(os.path.join(d, "worldgen", "structure", "probe_kingdom.json"), structure_json(a.adaptation, a.profile, "strongholds" if a.placement == "denselate" else "underground_structures"))
    if a.placement == "shared":
        write(os.path.join(a.out, "data", "minecraft", "worldgen", "structure_set", "trial_chambers.json"), shared_vanilla_set_json())
    else:
        write(os.path.join(d, "worldgen", "structure_set", "probe_kingdom.json"), structure_set_json(a.placement))


if __name__ == "__main__":
    main()
