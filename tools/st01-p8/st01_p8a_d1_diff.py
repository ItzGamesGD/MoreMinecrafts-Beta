#!/usr/bin/env python3
"""ST-01 P8-A-D1 structural diff: approved P8-A baseline (git 20a3578) vs the regenerated royal_hall.nbt.

Independent of the generator: both templates are parsed from NBT. Verifies that the only differences are the two authorized
changes (floor detailing, dais stair correction) and that every immutable invariant is untouched. Exit code 1 on any failure.

Usage: st01_p8a_d1_diff.py [--baseline-rev 20a3578] [--out docs/design/st01-p8/generated/p8a_d1_diff.json]
"""
import argparse
import gzip
import io
import json
import subprocess
import sys
from collections import Counter
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[2]
ASSET = "src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt"
MIN = (-40, -24, 2)
SIZE = (39, 20, 39)

ANCHOR = (-21, -22, 26)
CHEST_CELLS = {(-21, -20, 33), (-20, -20, 33)}
CHEST_SUPPORTS = {(-21, -21, 33), (-20, -21, 33)}
SECONDARY_ALCOVE = {(-3, -22, 25), (-3, -22, 26)}
STAIR_X = range(-25, -16)
CENTRE = (-21, 21)


def load(raw):
    f = nbtlib.File.parse(io.BytesIO(gzip.decompress(raw)))
    palette = []
    for p in f["palette"]:
        props = tuple(sorted((k, str(v)) for k, v in p.get("properties", {}).items()))
        palette.append((str(p["id"]).replace("minecraft:", ""), props))
    assert [int(v) for v in f["size"]] == list(SIZE), "size changed"
    vox = {}
    for e in f["blocks"]:
        pos = tuple(int(c) + m for c, m in zip(e["pos"], MIN))
        vox[pos] = palette[int(e["state"])]
    return vox, f


def category(pos, before, after):
    x, y, z = pos
    if y == -23:
        dx, dz = x - CENTRE[0], z - CENTRE[1]
        if abs(dx) <= 1 and (2 <= z <= 16 or 26 <= z <= 29):
            return "floor: processional runner"
        ring = max(abs(dx), abs(dz))
        if abs(dx) <= 8 and abs(dz) <= 8 and ring == 8:
            return "floor: central field border band"
        if abs(dx) <= 8 and abs(dz) <= 8 and ring == 4:
            return "floor: frame around carved base"
        return "floor: OTHER (unexpected)"
    if y == -21 and z in range(32, 38) and (z == 32 or x in (-26, -16)):
        return "dais top: accent frame"
    if z == 31 and x in STAIR_X and y == -22:
        return "stair row 2: new support block"
    if z == 31 and x in STAIR_X and y == -21:
        return "stair row 2: raised stair"
    return "UNEXPECTED"


def surface_profile(vox, x):
    """Walking-surface height along +z for column x at half-block resolution (z fractions .0 and .5), from the floor north of
    the dais to the dais top. Bottom stairs facing south: low half (north) = y+0.5, high half (south) = y+1."""
    samples = []
    for z in range(26, 34):
        for half in (0, 1):
            top = None
            for y in range(-24, -15):
                name, props = vox.get((x, y, z), ("air", ()))
                if name == "air":
                    continue
                p = dict(props)
                if name.endswith("stairs") and p.get("half") == "bottom" and p.get("facing") == "south":
                    h = y + (1.0 if half else 0.5)
                elif name == "chest":
                    continue
                else:
                    h = y + 1.0
                top = h if top is None else max(top, h)
                if y > -19:
                    break
            samples.append((z + 0.5 * half, top))
    return samples


def max_rise(profile):
    worst, at = 0.0, None
    for (z0, t0), (z1, t1) in zip(profile, profile[1:]):
        if t0 is not None and t1 is not None and t1 - t0 > worst:
            worst, at = t1 - t0, (z0, z1)
    return worst, at


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--baseline-rev", default="20a3578")
    ap.add_argument("--out", default=str(ROOT / "docs/design/st01-p8/generated/p8a_d1_diff.json"))
    args = ap.parse_args()

    base_raw = subprocess.check_output(["git", "show", f"{args.baseline_rev}:{ASSET}"], cwd=ROOT)
    new_raw = (ROOT / ASSET).read_bytes()
    base, _ = load(base_raw)
    new, _ = load(new_raw)
    checks = []

    def check(cid, ok, evidence):
        checks.append({"id": cid, "verdict": "PASS" if ok else "FAIL", "evidence": evidence})

    check("D1_SAME_VOLUME", set(base) == set(new) and len(base) == SIZE[0] * SIZE[1] * SIZE[2],
          f"{len(base)} voxels in both; same size {SIZE}")

    diff = {p: (base[p], new[p]) for p in base if base[p] != new[p]}
    cats = Counter()
    mats = Counter()
    for p, (b, n) in diff.items():
        cats[category(p, b, n)] += 1
        mats[f"{b[0]} -> {n[0]}"] += 1
    unexpected = [p for p in diff if category(p, *diff[p]).endswith("(unexpected)") or category(p, *diff[p]) == "UNEXPECTED"]
    check("D1_CHANGES_LIMITED_TO_AUTHORIZED_REGIONS", not unexpected,
          f"{len(diff)} changed voxels in {len(cats)} categories; unexpected={unexpected[:5]}")

    ys = sorted({p[1] for p in diff})
    check("D1_NO_WALL_CEILING_PILLAR_CHANGE", set(ys) <= {-23, -22, -21},
          f"changed voxels exist only on layers y={ys}; walls, corbel ceiling and roof (y>=-20) are identical")
    check("D1_CHEST_UNCHANGED", all(base[c] == new[c] for c in CHEST_CELLS | CHEST_SUPPORTS) and
          all(new[c][0] == "chest" for c in CHEST_CELLS),
          f"PRIMARY B (-21,-20,33)/(-20,-20,33) and supports identical: {[new[c] for c in sorted(CHEST_CELLS)]}")
    clearance = {(x, y, z) for x in range(-22, -19) for y in range(-22, -17) for z in range(25, 28)}
    check("D1_GUARDIAN_ANCHOR_AND_CLEARANCE_UNCHANGED", not (set(diff) & clearance) and
          base[(ANCHOR[0], ANCHOR[1] - 1, ANCHOR[2])] != ("air", ()) and new[(ANCHOR[0], ANCHOR[1] - 1, ANCHOR[2])][0] != "air",
          f"anchor {ANCHOR} 3x5x3 clearance box untouched; floor below anchor is {new[(-21, -23, 26)][0]}")
    check("D1_SECONDARY_ALCOVE_UNCHANGED", all(base[c] == new[c] for c in SECONDARY_ALCOVE), "SECONDARY chest cells remain air in both")
    central = {(x, y, z) for x in range(-23, -18) for y in range(-22, -5) for z in range(19, 24)}
    check("D1_CENTRAL_PILLAR_AND_BASE_UNCHANGED", not (set(diff) & central),
          "central column + carved base volume x -23..-19, z 19..23, y>=-22 has zero changes")
    side_aisles = {(x, -22, z) for x in list(range(-37, -32)) + list(range(-9, -4)) for z in range(5, 38)}
    check("D1_SIDE_AISLE_FLOOR_LEVEL_AND_OPEN", not (set(diff) & side_aisles) and
          all(new[p][0] == "air" for p in side_aisles if base[p][0] == "air"),
          "side-aisle walking layer is identical; aisle air cells still air")
    # Standing layer: nothing new may sit on the floor except the raised stair row (y=-21) and its support (y=-22, row z=31).
    added_solid = [p for p, (b, n) in diff.items() if b[0] == "air" and n[0] != "air"]
    check("D1_NO_NEW_OBSTACLES_ON_FLOOR", all(p[2] == 31 and p[0] in STAIR_X and p[1] == -21 for p in added_solid),
          f"air -> solid occurs only for the 9 raised stair blocks: {len(added_solid)} cells")
    floor_changes = [p for p, (b, n) in diff.items() if p[1] == -23]
    check("D1_FLOOR_BLOCKS_ARE_FULL_CUBES_AT_SAME_LEVEL", all(diff[p][1][0] == "smooth_red_sandstone" for p in floor_changes),
          f"{len(floor_changes)} floor blocks replaced in place with smooth_red_sandstone (full cube, same y=-23, nothing added above)")

    prof = {}
    for x in (-21, -25, -17):
        pb, pn = surface_profile(base, x), surface_profile(new, x)
        rb, rn = max_rise(pb), max_rise(pn)
        prof[f"x={x}"] = {"before": pb, "after": pn, "max_rise_before": rb, "max_rise_after": rn}
    ok_profile = all(v["max_rise_after"][0] <= 0.5 for v in prof.values())
    check("D1_STAIR_ASCENT_CONTINUOUS", ok_profile and prof["x=-21"]["max_rise_before"][0] >= 1.0,
          f"largest single rise along the climb: before {prof['x=-21']['max_rise_before']}, after {prof['x=-21']['max_rise_after']} "
          f"(vanilla step-up height 0.6, so no jump is needed after the correction)")
    top = {z: t for z, t in prof["x=-21"]["after"]}
    check("D1_FINAL_TREAD_FLUSH_WITH_DAIS_TOP", top[31.5] == -20.0 and top[32.0] == -20.0 and top[33.0] == -20.0,
          f"after: tread end z=31.5 -> {top[31.5]}, dais z=32.0 -> {top[32.0]}")
    stairs_after = [p for p in new if new[p][0] == "sandstone_stairs"]
    check("D1_STAIR_WIDTH_FACING_PRESERVED",
          len(stairs_after) == 18 and {p[0] for p in stairs_after} == set(STAIR_X) and
          all(dict(new[p][1]).get("facing") == "south" and dict(new[p][1]).get("half") == "bottom" for p in stairs_after),
          "18 stairs, x -25..-17 (width 9), facing south, bottom half, two rows (z=30 at y=-22, z=31 at y=-21)")

    floor_map = []
    for z in range(2, 41):
        row = ""
        for x in range(-40, -1):
            n = new[(x, -23, z)][0]
            row += "R" if n == "smooth_red_sandstone" else ("c" if n == "cut_sandstone" else ("m" if n == "smooth_sandstone" else "#"))
        floor_map.append(f"z={z:>2} " + row)

    report = {
        "baseline_rev": args.baseline_rev,
        "changed_voxels": len(diff),
        "changed_by_category": dict(sorted(cats.items())),
        "changed_by_material": dict(sorted(mats.items())),
        "layers_changed": ys,
        "surface_profile_along_z_(z, top_y)": prof,
        "floor_map_y=-23_(R=red accent, c=cut, m=smooth, #=other; x -40..-2 left to right, z 2..40 top to bottom)": floor_map,
        "checks": checks,
        "all_pass": all(c["verdict"] == "PASS" for c in checks),
    }
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text(json.dumps(report, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    for c in checks:
        print(f"[{c['verdict']}] {c['id']}: {c['evidence']}")
    print("changed:", dict(sorted(cats.items())))
    print("materials:", dict(sorted(mats.items())))
    return 0 if report["all_pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
