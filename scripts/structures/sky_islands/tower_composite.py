"""Compose an approved Sky Tower into each weighted island tower Jigsaw element.

The source island and approved tower templates are read-only inputs. Only generated
style/tower.nbt outputs are replaced; pool weights and structure placement stay fixed.
"""
import gzip
import io
import json
from collections import Counter
from pathlib import Path

import nbtlib
from nbtlib import Compound, Int, List, String

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "src/main/resources/data/moreminecrafts"
CANDIDATES = ROOT / "docs/planning/sky_tower_integration_02/phase_c2_side_access.json"
TOWER = DATA / "structure/sky_tower/full_01.nbt"
APPROVED_SHA256 = "cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27"
SHIFT = (15, 0, 3)
TOWER_ORIGIN = (0, 17, 0)
COMPOSITE_SIZE = (58, 97, 49)
TOWER_SIZE = (50, 80, 49)
STYLE_GAPS = {
    "snowy": {(35, 18, 23), (36, 18, 23)},
    "meadow": {(36, 18, 19), (37, 18, 19)},
    "forest": {(36, 18, 19), (37, 18, 19)},
    "badlands": {(35, 18, 19), (36, 18, 19)},
    "desert": {(36, 18, 23), (37, 18, 23)},
    "rocky": {(35, 18, 19), (36, 18, 19)},
}

def load(raw):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(raw)))

def block_rows(template):
    palette = template["palette"]
    rows = {}
    for entry in template["blocks"]:
        pos = tuple(map(int, entry["pos"]))
        rows[pos] = (Compound(palette[int(entry["state"])]),
                     Compound(entry["nbt"]) if "nbt" in entry else None)
    return rows

def shifted(pos, delta):
    return tuple(pos[i] + delta[i] for i in range(3))

def addon_state(block):
    name = block["block"]
    state = Compound({"id": String(name)})
    if name == "minecraft:stone_brick_slab":
        state["properties"] = Compound({"type": String("bottom"), "waterlogged": String("false")})
    return state

def load_candidate(style):
    rows = json.loads(CANDIDATES.read_text())["candidates"]
    return next(row for row in rows if row["id"] == f"{style}_side_opening_trial")

def compose(style, source_bytes, tower_bytes, candidate):
    island, tower = load(source_bytes), load(tower_bytes)
    if island["entities"] or tower["entities"]:
        raise ValueError("composite entity coordinate transforms are not defined; refusing to discard entities")
    rows = {shifted(p, SHIFT): v for p, v in block_rows(island).items()}
    island_rows = len(rows)
    tower_rows = block_rows(tower)
    collisions = sum(1 for p, value in tower_rows.items()
                     if str(value[0]["id"]) != "minecraft:structure_void"
                     and (p[0], p[1] + TOWER_ORIGIN[1], p[2]) in rows)
    for pos, value in tower_rows.items():
        rows[pos[0], pos[1] + TOWER_ORIGIN[1], pos[2]] = value
    gaps = STYLE_GAPS[style]
    bridge_rows = 0
    for block in candidate["bridge_and_support_voxels"]:
        old_pos = tuple(block["pos"])
        if block["component"] == "rail" and old_pos in gaps:
            continue
        pos = shifted(old_pos, SHIFT)
        rows[pos] = (addon_state(block), None)
        bridge_rows += 1
    # The old parent connector moves with the island payload and remains the sole
    # jigsaw attachment point to the existing start pool.
    jigsaws = [pos for pos, (state, _) in rows.items() if str(state["id"]) == "minecraft:jigsaw"]
    if len(jigsaws) != 1 or jigsaws[0] != (36, 16, 24):
        raise ValueError(f"{style}: expected one preserved root connector at (36, 16, 24), got {jigsaws}")
    # This weighted start-pool element already contains the approved full tower.
    # Terminate its legacy tower_top socket so a second tower cannot be attached.
    connector_state, connector_nbt = rows[jigsaws[0]]
    if connector_nbt is None:
        raise ValueError(f"{style}: root connector has no block entity data")
    connector_nbt["pool"] = String("minecraft:empty")
    tower_positions = {(x, y + TOWER_ORIGIN[1], z) for x, y, z in tower_rows}
    clearance = []
    clearance_by_block = Counter()
    for pos, (state, nbt) in list(rows.items()):
        inside = all(0 <= pos[i] < TOWER_SIZE[i] for i in range(3)) and TOWER_ORIGIN[1] <= pos[1] < TOWER_ORIGIN[1] + TOWER_SIZE[1]
        if inside and pos not in tower_positions and str(state["id"]) not in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
            clearance_by_block[str(state["id"])] += 1
            rows[pos] = (Compound({"id": String("minecraft:air")}), None)
            clearance.append(pos)
    for pos in rows:
        if not all(0 <= pos[i] < COMPOSITE_SIZE[i] for i in range(3)):
            raise ValueError(f"{style}: composite voxel outside declared bounds: {pos}")
    palette, keys, entries = [], {}, []
    for pos, (state, nbt) in sorted(rows.items()):
        key = json.dumps(state.unpack(), sort_keys=True, separators=(",", ":"))
        if key not in keys:
            keys[key] = len(palette); palette.append(state)
        entry = Compound({"pos": List[Int](pos), "state": Int(keys[key])})
        if nbt is not None:
            entry["nbt"] = nbt
        entries.append(entry)
    out = nbtlib.File({"DataVersion": Int(int(island["DataVersion"])),
        "size": List[Int](COMPOSITE_SIZE), "palette": List[Compound](palette),
        "blocks": List[Compound](entries), "entities": List[Compound]([])}, root_name="")
    raw = io.BytesIO(); out.write(raw)
    return gzip.compress(raw.getvalue(), mtime=0), {
        "style": style, "size": COMPOSITE_SIZE, "connector": jigsaws[0],
        "tower_origin_from_piece": TOWER_ORIGIN,
        "tower_bounds_from_connector": [[-36, 1, -24], [13, 80, 24]],
        "tower_block_count": len(tower_rows), "island_block_count": island_rows,
        "tower_overwrite_voxel_count": collisions, "added_bridge_support_voxel_count": bridge_rows,
        "tower_clearance_voxel_count": len(clearance), "tower_clearance_by_block": dict(sorted(clearance_by_block.items())),
        "terminated_legacy_socket": True,
        "rail_gap_count": len(gaps), "composite_block_count": len(rows)}
    return manifest
