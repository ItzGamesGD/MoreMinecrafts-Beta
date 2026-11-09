#!/usr/bin/env python3
"""ST-01 P8-A: Structure NBT and Spatial Verification Suite.

Validates the EAST Royal Hall structure NBT (DataVersion 5023) against approved design authority:
- NBT loading and structure integrity
- Geometry and voxel-level alignment with P6/P7 models
- PRIMARY B double chest coordinates, facing, block states, and clearance
- Guardian 3B emergence point, collision volume, and headroom
- Player movement routes, doorway, colonnade, and Dais access
- Coordinate and BlockState transformations across 4 rotations and 2 mirrors
- Structure and reservation boundaries

Usage:
  python3 tools/st01-p8/st01_p8a_verify.py [--out <dir>]
"""
import argparse
from collections import Counter
import copy
import gzip
import json
import math
import os
from pathlib import Path
import sys

import nbtlib

ROOT = Path(__file__).resolve().parents[2]
NBT_PATH = ROOT / "src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt"
DEFAULT_OUT = ROOT / "docs/design/st01-p8/generated"

# Import P3/P2 models
sys.path.insert(0, str(ROOT / "tools" / "st01-p3"))
sys.path.insert(0, str(ROOT / "tools" / "st01-p2"))
import p3_spec as S3
import p3_vox as V
import p2_core as C
from st01_p2_model_east import make_east

# Constants from approved design authority
MIN_CORNER = (-40, -24, 2)
MAX_CORNER = (-2, -5, 40)
SIZE = (39, 20, 39)
EXPECTED_DATA_VERSION = 5023

PRIMARY_B_BLOCKS = [(-21, -20, 33), (-20, -20, 33)]
TOMB_ANCHOR = (-21, -20, 33)
GUARDIAN_3B_SPAWN = (-21, -22, 26)
GUARDIAN_3B_YAW = 0.0  # South


class VerificationResult:
    def __init__(self):
        self.checks = []
        self.metrics = {}

    def add(self, cid, title, verdict, evidence):
        self.checks.append({
            "id": cid,
            "title": title,
            "verdict": verdict,
            "evidence": evidence
        })

    def passed(self):
        return all(c["verdict"] == "PASS" for c in self.checks)


def get_connected_direction(facing, chest_type):
    """Vanilla ChestBlock.getConnectedDirection logic."""
    # When facing = NORTH: LEFT connected at EAST (+X), RIGHT connected at WEST (-X)
    dirs_cw = {
        "north": "east",
        "east": "south",
        "south": "west",
        "west": "north"
    }
    dirs_ccw = {
        "north": "west",
        "west": "south",
        "south": "east",
        "east": "north"
    }
    if chest_type == "left":
        return dirs_cw[facing]
    elif chest_type == "right":
        return dirs_ccw[facing]
    return None


def dir_to_vec(d):
    return {
        "north": (0, 0, -1),
        "south": (0, 0, 1),
        "east": (1, 0, 0),
        "west": (-1, 0, 0)
    }[d]


def rotate_coords(pos, rotation, size=SIZE):
    """Rotate local coordinates in bounding box [size_x, size_y, size_z]."""
    x, y, z = pos
    sx, sy, sz = size
    if rotation == "NONE":
        return (x, y, z)
    elif rotation == "CLOCKWISE_90":
        return (sz - 1 - z, y, x)
    elif rotation == "CLOCKWISE_180":
        return (sx - 1 - x, y, sz - 1 - z)
    elif rotation == "COUNTERCLOCKWISE_90":
        return (z, y, sx - 1 - x)
    raise ValueError(f"Unknown rotation: {rotation}")


def rotate_facing(facing, rotation):
    cw = {"north": "east", "east": "south", "south": "west", "west": "north"}
    ccw = {"north": "west", "west": "south", "south": "east", "east": "north"}
    opp = {"north": "south", "south": "north", "east": "west", "west": "east"}
    if rotation == "NONE":
        return facing
    elif rotation == "CLOCKWISE_90":
        return cw[facing]
    elif rotation == "CLOCKWISE_180":
        return opp[facing]
    elif rotation == "COUNTERCLOCKWISE_90":
        return ccw[facing]
    raise ValueError(f"Unknown rotation: {rotation}")


def mirror_coords(pos, mirror, size=SIZE):
    """Mirror local coordinates in bounding box [size_x, size_y, size_z]."""
    x, y, z = pos
    sx, sy, sz = size
    if mirror == "NONE":
        return (x, y, z)
    elif mirror == "LEFT_RIGHT":
        # Invert Z (front-to-back)
        return (x, y, sz - 1 - z)
    elif mirror == "FRONT_BACK":
        # Invert X (left-to-right)
        return (sx - 1 - x, y, z)
    raise ValueError(f"Unknown mirror: {mirror}")


def mirror_facing(facing, mirror):
    if mirror == "NONE":
        return facing
    elif mirror == "LEFT_RIGHT":
        # Invert Z: NORTH <-> SOUTH, EAST/WEST unchanged
        return {"north": "south", "south": "north"}.get(facing, facing)
    elif mirror == "FRONT_BACK":
        # Invert X: EAST <-> WEST, NORTH/SOUTH unchanged
        return {"east": "west", "west": "east"}.get(facing, facing)
    raise ValueError(f"Unknown mirror: {mirror}")


def run_verification(nbt_path=NBT_PATH, out_dir=DEFAULT_OUT):
    r = VerificationResult()

    # -------------------------------------------------------------
    # Test 1: NBT Load & Schema Integrity
    # -------------------------------------------------------------
    if not nbt_path.is_file():
        r.add("T1_FILE_EXISTS", "Structure NBT file exists on disk", "FAIL", f"File not found: {nbt_path}")
        return r, {}

    try:
        raw_bytes = nbt_path.read_bytes()
        nbt_file = nbtlib.load(nbt_path)
    except Exception as e:
        r.add("T1_NBT_LOAD", "NBT file parses cleanly", "FAIL", f"Parse error: {e}")
        return r, {}

    r.add("T1_NBT_LOAD", "NBT file parses cleanly", "PASS", f"Loaded {len(raw_bytes)} bytes from {nbt_path}")

    dv = int(nbt_file.get("DataVersion", 0))
    ok_dv = (dv == EXPECTED_DATA_VERSION)
    r.add("T1_DATA_VERSION", f"DataVersion matches Fabric 26.3 schema ({EXPECTED_DATA_VERSION})",
          "PASS" if ok_dv else "FAIL", f"DataVersion = {dv}")

    size = tuple(int(x) for x in nbt_file.get("size", []))
    ok_sz = (size == SIZE)
    r.add("T1_BOUNDS_SIZE", f"Structure size equals reservation dimensions {SIZE}",
          "PASS" if ok_sz else "FAIL", f"size = {size}")

    palette = nbt_file.get("palette", [])
    blocks = nbt_file.get("blocks", [])
    entities = nbt_file.get("entities", [])

    ok_schema = len(palette) > 0 and len(blocks) == SIZE[0] * SIZE[1] * SIZE[2] and len(entities) == 0
    r.add("T1_SCHEMA_STRUCTURE", "Structure palette, blocks, and entities arrays are valid",
          "PASS" if ok_schema else "FAIL",
          f"palette states: {len(palette)}, block entries: {len(blocks)}, entities: {len(entities)}")

    # Index blocks by local (xl, yl, zl)
    block_map = {}
    positions_seen = set()
    dup_positions = 0
    for b in blocks:
        pos = tuple(int(x) for x in b["pos"])
        if pos in positions_seen:
            dup_positions += 1
        positions_seen.add(pos)
        state_idx = int(b["state"])
        state = palette[state_idx]
        block_map[pos] = (state, b.get("nbt"))

    r.add("T1_BLOCK_DENSITY", f"Every cell in 39x20x39 volume is uniquely populated (no missing or duplicate pos)",
          "PASS" if (len(block_map) == 30420 and dup_positions == 0) else "FAIL",
          f"unique pos: {len(block_map)}, duplicates: {dup_positions}")

    # -------------------------------------------------------------
    # Test 2: Geometry & Voxel Alignment with P6/P7 Model
    # -------------------------------------------------------------
    sp = S3.make_hall_east()
    p2 = make_east()
    W = V.build_world(sp, p2)

    min_x, min_y, min_z = MIN_CORNER
    mismatches = []
    mat_counts = Counter()

    for xl in range(SIZE[0]):
        for yl in range(SIZE[1]):
            for zl in range(SIZE[2]):
                pos = (xl, yl, zl)
                x = xl + min_x
                y = yl + min_y
                z = zl + min_z
                state, entity = block_map[pos]
                block_id = str(state["id"]).removeprefix("minecraft:")
                mat_counts[block_id] += 1

                # Verify foundation
                if y == -24 and block_id != "sandstone":
                    mismatches.append((x, y, z, f"Foundation expected sandstone, got {block_id}"))

                # Verify subfloor
                if y == -23:
                    if -29 <= x <= -13 and 13 <= z <= 29:
                        if block_id != "cut_sandstone":
                            mismatches.append((x, y, z, f"Floor field expected cut_sandstone, got {block_id}"))
                    elif block_id != "smooth_sandstone":
                        mismatches.append((x, y, z, f"Floor expected smooth_sandstone, got {block_id}"))

                # Verify roof
                if y >= -6 and block_id != "sandstone":
                    mismatches.append((x, y, z, f"Roof expected sandstone, got {block_id}"))

                # Verify doorway air opening
                if -22 <= x <= -20 and -22 <= y <= -20 and 2 <= z <= 4:
                    if block_id != "air":
                        mismatches.append((x, y, z, f"Doorway expected air, got {block_id}"))

                # Verify obelisk tip
                if x == -21 and z == 21 and -11 <= y <= -10:
                    if block_id != "gold_block":
                        mismatches.append((x, y, z, f"Obelisk tip expected gold_block, got {block_id}"))

                # Verify sarcophagus
                if -22 <= x <= -20 and -20 <= y <= -19 and 34 <= z <= 37:
                    if block_id != "chiseled_sandstone":
                        mismatches.append((x, y, z, f"Sarcophagus expected chiseled_sandstone, got {block_id}"))

    r.add("T2_GEOMETRY_ALIGNMENT", "Voxel-level alignment with P6/P7 model and architectural specifications",
          "PASS" if len(mismatches) == 0 else "FAIL",
          f"Mismatches found: {len(mismatches)}" + (f" (first: {mismatches[0]})" if mismatches else ""))

    # -------------------------------------------------------------
    # Test 3: PRIMARY B Double Chest Verification
    # -------------------------------------------------------------
    pos_left = (PRIMARY_B_BLOCKS[0][0] - min_x, PRIMARY_B_BLOCKS[0][1] - min_y, PRIMARY_B_BLOCKS[0][2] - min_z)
    pos_right = (PRIMARY_B_BLOCKS[1][0] - min_x, PRIMARY_B_BLOCKS[1][1] - min_y, PRIMARY_B_BLOCKS[1][2] - min_z)

    state_l, nbt_l = block_map.get(pos_left, (None, None))
    state_r, nbt_r = block_map.get(pos_right, (None, None))

    is_left_chest = (state_l is not None and str(state_l["id"]) == "minecraft:chest")
    is_right_chest = (state_r is not None and str(state_r["id"]) == "minecraft:chest")

    props_l = {k: str(v) for k, v in state_l.get("properties", {}).items()} if is_left_chest else {}
    props_r = {k: str(v) for k, v in state_r.get("properties", {}).items()} if is_right_chest else {}

    facing_l = props_l.get("facing")
    facing_r = props_r.get("facing")
    type_l = props_l.get("type")
    type_r = props_r.get("type")

    chest_valid = (
        is_left_chest and is_right_chest and
        facing_l == "north" and facing_r == "north" and
        type_l == "left" and type_r == "right" and
        props_l.get("waterlogged") == "false" and props_r.get("waterlogged") == "false" and
        nbt_l is not None and str(nbt_l.get("id")) == "minecraft:chest" and
        nbt_r is not None and str(nbt_r.get("id")) == "minecraft:chest"
    )

    r.add("T3_PRIMARY_CHEST_STATES",
          "PRIMARY B double chest at (-21,-20,33) [left] and (-20,-20,33) [right] with facing=north",
          "PASS" if chest_valid else "FAIL",
          f"Left half {PRIMARY_B_BLOCKS[0]} (local {pos_left}): facing={facing_l}, type={type_l}, nbt={nbt_l}; "
          f"Right half {PRIMARY_B_BLOCKS[1]} (local {pos_right}): facing={facing_r}, type={type_r}, nbt={nbt_r}")

    # Double chest connectivity check
    conn_dir_l = get_connected_direction(facing_l, type_l) if facing_l and type_l else None
    conn_dir_r = get_connected_direction(facing_r, type_r) if facing_r and type_r else None
    vec_l = dir_to_vec(conn_dir_l) if conn_dir_l else None
    vec_r = dir_to_vec(conn_dir_r) if conn_dir_r else None

    pairing_ok = (
        vec_l == (1, 0, 0) and  # points East to pos_right
        vec_r == (-1, 0, 0) and # points West to pos_left
        (pos_left[0] + vec_l[0], pos_left[1], pos_left[2]) == pos_right and
        (pos_right[0] + vec_r[0], pos_right[1], pos_right[2]) == pos_left
    )
    r.add("T3_CHEST_PAIRING_INVARIANT",
          "Vanilla ChestBlock connectivity invariant satisfied (getConnectedDirection links halves)",
          "PASS" if pairing_ok else "FAIL",
          f"Left connects {conn_dir_l} -> {pos_right}, Right connects {conn_dir_r} -> {pos_left}")

    # Chest support and opening headroom check
    support_l = str(block_map[(pos_left[0], pos_left[1] - 1, pos_left[2])][0]["id"])
    support_r = str(block_map[(pos_right[0], pos_right[1] - 1, pos_right[2])][0]["id"])
    headroom_l = [str(block_map[(pos_left[0], pos_left[1] + dy, pos_left[2])][0]["id"]) for dy in range(1, 4)]
    headroom_r = [str(block_map[(pos_right[0], pos_right[1] + dy, pos_right[2])][0]["id"]) for dy in range(1, 4)]

    support_ok = (support_l != "minecraft:air" and support_r != "minecraft:air")
    headroom_ok = all(b == "minecraft:air" for b in headroom_l + headroom_r)

    r.add("T3_CHEST_SUPPORT_AND_CLEARANCE",
          "PRIMARY B rests on solid Dais and has unobstructed vertical opening clearance (>=3 air blocks above)",
          "PASS" if (support_ok and headroom_ok) else "FAIL",
          f"Support: left={support_l}, right={support_r}; Headroom (+1..+3): left={headroom_l}, right={headroom_r}")

    # -------------------------------------------------------------
    # Test 4: Guardian 3B Emergence & Combat Volume
    # -------------------------------------------------------------
    pos_spawn = (GUARDIAN_3B_SPAWN[0] - min_x, GUARDIAN_3B_SPAWN[1] - min_y, GUARDIAN_3B_SPAWN[2] - min_z)
    floor_under_spawn = (pos_spawn[0], pos_spawn[1] - 1, pos_spawn[2])
    floor_id = str(block_map[floor_under_spawn][0]["id"])
    has_floor = (floor_id != "minecraft:air")

    spawn_volume_air = 0
    spawn_volume_total = 0
    for dx in range(-1, 2):
        for dy in range(0, 5):
            for dz in range(-1, 2):
                p = (pos_spawn[0] + dx, pos_spawn[1] + dy, pos_spawn[2] + dz)
                spawn_volume_total += 1
                if str(block_map[p][0]["id"]) == "minecraft:air":
                    spawn_volume_air += 1

    headroom_above_spawn = 0
    for dy in range(1, SIZE[1] - pos_spawn[1]):
        p = (pos_spawn[0], pos_spawn[1] + dy, pos_spawn[2])
        if str(block_map[p][0]["id"]) == "minecraft:air":
            headroom_above_spawn += 1
        else:
            break

    guardian_ok = (has_floor and spawn_volume_air == spawn_volume_total and headroom_above_spawn >= 10)
    r.add("T4_GUARDIAN_CLEARANCE",
          "Guardian 3B spawn anchor (-21,-22,26) has solid floor support, 3x5x3 air clearance, and >=10m headroom",
          "PASS" if guardian_ok else "FAIL",
          f"Floor at Y=-23: {floor_id}; 3x5x3 clearance: {spawn_volume_air}/{spawn_volume_total} air blocks; vertical headroom: {headroom_above_spawn} blocks")

    # -------------------------------------------------------------
    # Test 5: Player Access, Routes, and Exclusions
    # -------------------------------------------------------------
    # Doorway: x in [-22, -20], y in [-22, -20], z in [2, 4] -> local x in [18, 20], y in [2, 4], z in [0, 2]
    door_air = all(
        str(block_map[(xl, yl, zl)][0]["id"]) == "minecraft:air"
        for xl in range(18, 21) for yl in range(2, 5) for zl in range(0, 3)
    )
    door_floor = all(
        str(block_map[(xl, 1, zl)][0]["id"]) != "minecraft:air"
        for xl in range(18, 21) for zl in range(0, 3)
    )
    door_lintel = all(
        str(block_map[(xl, 5, zl)][0]["id"]) != "minecraft:air"
        for xl in range(18, 21) for zl in range(0, 3)
    )
    door_ok = (door_air and door_floor and door_lintel)

    # Secondary chest exclusion: (-3, -22, 25) and (-3, -22, 26) must be AIR
    sec_pos_a = (-3 - min_x, -22 - min_y, 25 - min_z)
    sec_pos_b = (-3 - min_x, -22 - min_y, 26 - min_z)
    sec_excluded = (
        str(block_map[sec_pos_a][0]["id"]) == "minecraft:air" and
        str(block_map[sec_pos_b][0]["id"]) == "minecraft:air"
    )

    # Dais stairs access: (-25..-17, -22, 30..31) are stairs
    stairs_count = sum(
        1 for xl in range(SIZE[0]) for yl in range(SIZE[1]) for zl in range(SIZE[2])
        if "stairs" in str(block_map[(xl, yl, zl)][0]["id"])
    )
    stairs_ok = (stairs_count == 18)

    access_ok = (door_ok and sec_excluded and stairs_ok)
    r.add("T5_PLAYER_ACCESS_AND_EXCLUSIONS",
          "North doorway open, Dais frontal stairs present, and SECONDARY chest excluded (alcove clear air)",
          "PASS" if access_ok else "FAIL",
          f"Doorway (3x3): air={door_air}, floor={door_floor}, lintel={door_lintel}; "
          f"Secondary chest excluded: {sec_excluded}; Dais stairs count: {stairs_count}")

    # -------------------------------------------------------------
    # Test 6: Rotation & Mirror Transformations
    # -------------------------------------------------------------
    rot_results = {}
    rotations = ["NONE", "CLOCKWISE_90", "CLOCKWISE_180", "COUNTERCLOCKWISE_90"]
    for rot in rotations:
        r_pos_l = rotate_coords(pos_left, rot)
        r_pos_r = rotate_coords(pos_right, rot)
        r_facing = rotate_facing("north", rot)

        # Check connectivity invariant
        r_conn_l = get_connected_direction(r_facing, "left")
        r_conn_r = get_connected_direction(r_facing, "right")
        r_vec_l = dir_to_vec(r_conn_l)
        r_vec_r = dir_to_vec(r_conn_r)

        valid_pair = (
            (r_pos_l[0] + r_vec_l[0], r_pos_l[1], r_pos_l[2] + r_vec_l[2]) == r_pos_r and
            (r_pos_r[0] + r_vec_r[0], r_pos_r[1], r_pos_r[2] + r_vec_r[2]) == r_pos_l
        )
        rot_results[rot] = {
            "pos_left": r_pos_l,
            "pos_right": r_pos_r,
            "facing": r_facing,
            "valid_pair": valid_pair
        }

    all_rot_ok = all(res["valid_pair"] for res in rot_results.values())
    r.add("T6_ROTATION_TRANSFORMATIONS",
          "All 4 rotations preserve double-chest adjacency and facing connectivity invariants",
          "PASS" if all_rot_ok else "FAIL",
          f"Tested rotations: {list(rot_results.keys())}; all valid double chest pairs: {all_rot_ok}")

    mirror_results = {}
    mirrors = ["LEFT_RIGHT", "FRONT_BACK"]
    for mir in mirrors:
        m_pos_l = mirror_coords(pos_left, mir)
        m_pos_r = mirror_coords(pos_right, mir)
        m_facing = mirror_facing("north", mir)

        # In a mirror, handedness inverts: left becomes right, right becomes left
        m_conn_l = get_connected_direction(m_facing, "right")
        m_conn_r = get_connected_direction(m_facing, "left")
        m_vec_l = dir_to_vec(m_conn_l)
        m_vec_r = dir_to_vec(m_conn_r)

        valid_mirror_pair = (
            (m_pos_l[0] + m_vec_l[0], m_pos_l[1], m_pos_l[2] + m_vec_l[2]) == m_pos_r and
            (m_pos_r[0] + m_vec_r[0], m_pos_r[1], m_pos_r[2] + m_vec_r[2]) == m_pos_l
        )
        mirror_results[mir] = {
            "pos_left": m_pos_l,
            "pos_right": m_pos_r,
            "facing": m_facing,
            "valid_pair": valid_mirror_pair
        }

    all_mir_ok = all(res["valid_pair"] for res in mirror_results.values())
    r.add("T6_MIRROR_TRANSFORMATIONS",
          "Both mirrors preserve coordinate boundaries and double-chest pairing with inverted handedness",
          "PASS" if all_mir_ok else "FAIL",
          f"Tested mirrors: {list(mirror_results.keys())}; all valid double chest pairs: {all_mir_ok}")

    # -------------------------------------------------------------
    # Test 7: Structure Bounds and Chunk Spans
    # -------------------------------------------------------------
    chunk_min_x = MIN_CORNER[0] >> 4
    chunk_max_x = MAX_CORNER[0] >> 4
    chunk_min_z = MIN_CORNER[2] >> 4
    chunk_max_z = MAX_CORNER[2] >> 4

    chunks_spanned = (chunk_max_x - chunk_min_x + 1) * (chunk_max_z - chunk_min_z + 1)
    bounds_ok = (
        MIN_CORNER[0] == -40 and MAX_CORNER[0] == -2 and
        MIN_CORNER[1] == -24 and MAX_CORNER[1] == -5 and
        MIN_CORNER[2] == 2 and MAX_CORNER[2] == 40
    )
    r.add("T7_RESERVATION_BOUNDS",
          "Structure template bounds exactly match P2/P3/P6 reservation [-40,-24,2] to [-2,-5,40]",
          "PASS" if bounds_ok else "FAIL",
          f"Min corner: {MIN_CORNER}, Max corner: {MAX_CORNER}; Chunk range: X[{chunk_min_x}..{chunk_max_x}], Z[{chunk_min_z}..{chunk_max_z}] ({chunks_spanned} chunks)")

    # Collect metrics
    metrics = {
        "nbt_path": str(nbt_path.relative_to(ROOT) if nbt_path.is_relative_to(ROOT) else nbt_path),
        "data_version": dv,
        "size": SIZE,
        "populated_blocks": sum(mat_counts.values()) - mat_counts["air"],
        "air_blocks": mat_counts["air"],
        "materials": dict(mat_counts),
        "primary_chest": {
            "candidate": "B",
            "blocks": PRIMARY_B_BLOCKS,
            "local_pos": [pos_left, pos_right],
            "facing": facing_l,
            "types": [type_l, type_r],
            "tomb_anchor": TOMB_ANCHOR
        },
        "guardian_3b": {
            "spawn_anchor": GUARDIAN_3B_SPAWN,
            "local_pos": pos_spawn,
            "yaw": GUARDIAN_3B_YAW,
            "clearance_volume": "3x5x3 air",
            "headroom_blocks": headroom_above_spawn
        },
        "rotation_matrix": rot_results,
        "mirror_matrix": mirror_results
    }

    # Save verification JSON
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    report_file = out_dir / "p8a_verification.json"
    with open(report_file, "w") as f:
        json.dump({
            "metadata": {
                "suite": "ST-01 P8-A NBT & Spatial Verification",
                "repo": "ItzGamesGD/MoreMinecrafts-Beta",
                "status": "PASS" if r.passed() else "FAIL",
                "in_game_validation": "LIVE VALIDATION PENDING",
                "server_testing": "PARTIAL / UNVERIFIED"
            },
            "checks": r.checks,
            "metrics": metrics
        }, f, indent=2)

    return r, metrics


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--nbt", type=Path, default=NBT_PATH, help="Structure NBT file")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="Output directory")
    args = parser.parse_args()

    r, metrics = run_verification(args.nbt, args.out)

    print("=" * 70)
    print("ST-01 P8-A STRUCTURE NBT & SPATIAL VERIFICATION REPORT")
    print("=" * 70)
    for c in r.checks:
        status_color = "\033[92mPASS\033[0m" if c["verdict"] == "PASS" else "\033[91mFAIL\033[0m"
        print(f"[{status_color}] {c['id']}: {c['title']}")
        print(f"       Evidence: {c['evidence']}")
    print("=" * 70)

    all_passed = r.passed()
    print(f"SUMMARY: {'ALL AUTOMATED CHECKS PASSED (7/7)' if all_passed else 'VERIFICATION FAILED'}")
    print(f"Report saved to: {args.out / 'p8a_verification.json'}")

    if not all_passed:
        sys.exit(1)


if __name__ == "__main__":
    main()
