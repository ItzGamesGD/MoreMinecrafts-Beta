"""Tiny voxel template builder that writes Minecraft 26.3 id/properties structure NBT (see generate_ancient_city_arena.py)."""
import gzip
import io

import nbtlib
from nbtlib import Compound, Int, List, String

DATA_VERSION = 5023
FACINGS = ("north", "east", "south", "west")
DELTA = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


class Vox:
    def __init__(self, size=None):
        self.blocks = {}
        self.size = size  # explicit (x, y, z) template size; otherwise the block extent

    # -- primitives -------------------------------------------------------------------------------
    def put(self, x, y, z, name, props=None, nbt=None):
        assert x >= 0 and y >= 0 and z >= 0, (x, y, z, name)
        if self.size is not None:
            assert x < self.size[0] and y < self.size[1] and z < self.size[2], (x, y, z, name, self.size)
        self.blocks[x, y, z] = (name, tuple(sorted((props or {}).items())), nbt)

    def get(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def remove(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def box(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, name, props)

    # -- shaped blocks ----------------------------------------------------------------------------
    def stairs(self, x, y, z, name, facing, half="bottom"):
        self.put(x, y, z, name, dict(facing=facing, half=half, shape="straight", waterlogged="false"))

    def slab(self, x, y, z, name, kind="bottom"):
        self.put(x, y, z, name, dict(type=kind, waterlogged="false"))

    def log(self, x, y, z, name, axis="y"):
        self.put(x, y, z, name, dict(axis=axis))

    def leaves(self, x, y, z, name):
        if (x, y, z) not in self.blocks:
            self.put(x, y, z, name, dict(persistent="true", distance="7", waterlogged="false"))

    # -- block entities ---------------------------------------------------------------------------
    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state, joint="aligned"):
        self.put(x, y, z, "jigsaw", dict(orientation=orientation), {
            "id": String("minecraft:jigsaw"), "name": String(name), "target": String(target),
            "pool": String(pool), "joint": String(joint), "final_state": String(final_state),
            "selection_priority": Int(0), "placement_priority": Int(0)})

    def chest(self, x, y, z, facing, loot_table):
        self.put(x, y, z, "chest", dict(facing=facing, type="single", waterlogged="false"),
                 {"id": String("minecraft:chest"), "LootTable": String(loot_table)})

    def vault(self, x, y, z, facing, loot_table):
        self.put(x, y, z, "vault", dict(facing=facing, ominous="false", vault_state="inactive"), {
            "id": String("minecraft:vault"),
            "config": Compound({"loot_table": String(loot_table),
                                "key_item": Compound({"id": String("minecraft:trial_key"), "count": Int(1)})})})

    def spawner(self, x, y, z, normal_config, ominous_config):
        self.put(x, y, z, "trial_spawner", dict(ominous="false", trial_spawner_state="inactive"), {
            "id": String("minecraft:trial_spawner"),
            "normal_config": String(normal_config), "ominous_config": String(ominous_config)})

    # -- connected blocks -------------------------------------------------------------------------
    def finish_walls(self):
        """Compute wall connection states for every wall block (templates are not re-shaped at placement)."""
        for (x, y, z), (name, _, _) in list(self.blocks.items()):
            if not name.endswith("_wall"):
                continue

            def side(dx, dz):
                other = self.blocks.get((x + dx, y, z + dz))
                if other and (other[0].endswith("_wall") or other[0].endswith("bricks") or other[0] in SOLID_FOR_WALLS):
                    return "low" if self.blocks.get((x, y + 1, z)) is None else "tall"
                return "none"

            states = {"north": side(0, -1), "south": side(0, 1), "east": side(1, 0), "west": side(-1, 0)}
            straight = (states["north"] != "none" and states["south"] != "none" and states["east"] == "none" and states["west"] == "none") or \
                       (states["east"] != "none" and states["west"] != "none" and states["north"] == "none" and states["south"] == "none")
            above = self.blocks.get((x, y + 1, z))
            up = "false" if straight and above is None else "true"
            self.blocks[x, y, z] = (name, tuple(sorted(dict(states, up=up, waterlogged="false").items())), None)

    # -- output -----------------------------------------------------------------------------------
    def extent(self):
        xs, ys, zs = zip(*self.blocks)
        return max(xs) + 1, max(ys) + 1, max(zs) + 1

    def to_nbt(self):
        size = self.size or self.extent()
        palette, lookup, entries = [], {}, []
        for position, (name, properties, entity) in sorted(self.blocks.items()):
            key = (name, properties)
            if key not in lookup:
                lookup[key] = len(palette)
                state = Compound({"id": String("minecraft:" + name)})
                if properties:
                    state["properties"] = Compound({k: String(v) for k, v in properties})
                palette.append(state)
            entry = Compound({"pos": List[Int](position), "state": Int(lookup[key])})
            if entity:
                entry["nbt"] = Compound(entity)
            entries.append(entry)
        structure = nbtlib.File({
            "DataVersion": Int(DATA_VERSION), "size": List[Int](size),
            "palette": List[Compound](palette), "blocks": List[Compound](entries),
            "entities": List[Compound]([]),
        }, root_name="")
        raw = io.BytesIO()
        structure.write(raw)
        return gzip.compress(raw.getvalue(), mtime=0)

    def count(self, name):
        return sum(1 for entry in self.blocks.values() if entry[0] == name)


SOLID_FOR_WALLS = {"stone", "cobblestone", "mossy_cobblestone", "smooth_quartz", "quartz_block", "andesite"}
