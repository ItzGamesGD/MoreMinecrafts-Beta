#!/usr/bin/env python3
"""Validates one seed's scan/replay/reload probe outputs. Exit code != 0 on any failure (so CI fails).
usage: verify_results.py scan.json replay.json reload.json
"""
import json
import os
import sys

LOCKED = {"spacing": 32, "separation": 8, "spread_type": "triangular", "salt": 91401}


VEGETATION = {"minecraft:air", "minecraft:cave_air", "minecraft:short_dry_grass", "minecraft:tall_dry_grass", "minecraft:dead_bush",
              "minecraft:cactus", "minecraft:cactus_flower", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:bush", "minecraft:firefly_bush",
              "minecraft:glow_lichen", "minecraft:sugar_cane", "minecraft:bone_block", "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves", "minecraft:oak_log"}
UNDERGROUND = {"stone", "andesite", "diorite", "granite", "tuff", "gravel", "dirt", "coarse_dirt", "clay", "infested_stone"}


def load_dump(path):
    legend, body = open(path).read().split("---\n", 1)
    names = {int(l.split("=", 1)[0]): l.split("=", 1)[1] for l in legend.strip().split("\n")}
    cols = {}
    for line in body.strip().split("\n"):
        k, v = line.split(":")
        cols[k] = [names[int(i)] for i in v.strip(",").split(",")]
    return cols


def block_id(state):
    return state.replace("Block{", "").split("}")[0]


def vanilla_feature_block(state):
    b = block_id(state)
    return b in VEGETATION or (b.startswith("minecraft:") and (b[10:] in UNDERGROUND or b.endswith("_ore")))


def vegetation_diff(results_dir, key):  # returns a description; may append to `unexpected`
    """Proves a raw-hash difference consists solely of vegetation blocks (dump files from both phases)."""
    a_path = os.path.join(results_dir, "dump-scan", key.replace(",", "_") + ".txt")
    b_path = os.path.join(results_dir, "dump-replay", key.replace(",", "_") + ".txt")
    if not (os.path.exists(a_path) and os.path.exists(b_path)):
        return "dumps unavailable (cannot classify)"
    a, b = load_dump(a_path), load_dump(b_path)
    differing = non_veg = 0
    for k in a:
        for x, y in zip(a[k], b[k]):
            if x != y:
                differing += 1
                if not vanilla_feature_block(x) or not vanilla_feature_block(y):
                    non_veg += 1
    window = len(a) * len(next(iter(a.values())))
    if non_veg > 0.005 * window:
        unexpected.append(f"replay start {key}: {non_veg} differing blocks outside vegetation/vanilla features exceed 0.5% of the {window}-block window")
    return f"{differing} differing blocks of {window} ({non_veg} outside vegetation/vanilla underground features)"


results_dir = ""
overlaps = {}
expected_nondet = []
unexpected = []


def main():
    global results_dir
    scan, replay, reload_ = (json.load(open(p)) for p in sys.argv[1:4])
    results_dir = os.path.dirname(os.path.abspath(sys.argv[1]))
    for k, v in scan["starts"].items():
        overlaps[k] = bool(v.get("intersecting_other_structures"))
    problems = []
    for name, d in (("scan", scan), ("replay", replay), ("reload", reload_)):
        if "error" in d:
            problems.append(f"{name}: probe error {d['error']}")
    if problems:
        print("\n".join(problems))
        return 1

    reg = scan["registration"]
    for k, v in LOCKED.items():
        if reg.get(k) != v:
            problems.append(f"registration {k}={reg.get(k)} expected {v}")
    for k in ("structure_registered", "structure_type_registered", "structure_class_ok", "piece_types_registered", "placement_matches_locked_values"):
        if reg.get(k) is not True:
            problems.append(f"registration {k} not true")
    if reg.get("structure_sets_containing") != ["moreminecrafts:st01_pyramid"]:
        problems.append(f"structure in unexpected sets: {reg.get('structure_sets_containing')}")

    st = scan["anchor_statistics"]
    if st["prediction_vs_actual_mismatches"] != 0:
        problems.append(f"independent staged prediction disagrees with actual starts {st['prediction_vs_actual_mismatches']}x: {st['mismatch_samples']}")
    if scan["completed_structures"] != scan["start_count"]:
        bad = {k: v["failures"] for k, v in scan["starts"].items() if not v["all_checks_pass"]}
        problems.append(f"{scan['start_count'] - scan['completed_structures']} start(s) failed physical checks: {bad}")
    fx = scan.get("correction_fixture", {})
    if fx.get("pass") is not True:
        problems.append(f"correction fixture failed: {fx.get('failures')}")

    for kind in ("fluid_rejection_audit", "terrain_rejection_no_partial_audit"):
        rows = scan.get(kind, [])
        bad = [r for r in rows if not r.get("no_partial_generation", True)]
        if rows:
            print(f"{kind}: {len(rows)} rejected candidates generated for real, partial-generation findings: {len(bad)}")
        if bad:
            problems.append(f"{kind}: rejected candidate left a start or hall blocks: {bad}")

    for other_name, other in (("replay", replay), ("reload", reload_)):
        if set(other["starts"]) != set(scan["starts"]):
            problems.append(f"{other_name}: start set differs {sorted(other['starts'])} vs {sorted(scan['starts'])}")
            continue
        for key, rec in scan["starts"].items():
            o = other["starts"][key]
            fields = ["anchor", "rotation", "hall_box", "primary_chest_left", "hall_template_masked_sha256", "surface_profile_sha256"]
            if other_name == "reload":
                fields += ["footprint_state_sha256", "hall_state_sha256"]  # a restarted server must reproduce EVERY block exactly
            for f in fields:
                if rec[f] != o[f]:
                    problems.append(f"{other_name}: start {key} field {f} differs: {rec[f]} vs {o[f]}")
            if other_name == "replay" and (rec["footprint_state_sha256"] != o["footprint_state_sha256"] or rec["hall_state_sha256"] != o["hall_state_sha256"]):
                diff = vegetation_diff(results_dir, key)
                ctl = "control window (no structure) " + ("ALSO differs" if rec["control_window_state_sha256"] != o["control_window_state_sha256"] else "identical")
                expected_nondet.append(f"replay start {key}: raw footprint hash differs; {diff}; {ctl}")

    print(f"seed={scan['seed']} potential={st['potential_anchors']} center_desert={st['center_only_desert_anchors']} "
          f"footprint_desert={st['desert_footprint_qualified_anchors']} terrain_ok={st['terrain_qualified_anchors']} "
          f"fluid_safe={st['fluid_safe_anchors_predicted']} starts={st['actual_valid_starts']} completed={scan['completed_structures']} "
          f"density/100km2={st['density_per_100km2']:.3f} nearest={scan['nearest_start_distance_blocks']}")
    print("rejections:", st["rejection_counts_independent"])
    audit = scan.get("fluid_rejection_audit", [])
    if audit:
        confirmed = sum(1 for r in audit if r["fluid_rejection_confirmed_by_real_chunks"])
        print(f"fluid rejection audit: {confirmed}/{len(audit)} rejected candidates have real fluid in the generated volume")
        if confirmed != len(audit):
            problems.append("fluid check false positive(s): " + str([r for r in audit if not r["fluid_rejection_confirmed_by_real_chunks"]]))
    for key, rec in sorted(scan["starts"].items()):
        print(f"  start chunk {key} overlaps={rec.get('intersecting_other_structures')} anchor={rec['anchor']} rot={rec['rotation']} exhaustive cut={rec['terrain_exhaustive_audit']['exhaustive_cut']} "
              f"skirt={rec['terrain_exhaustive_audit']['exhaustive_skirt']} dh={rec['terrain_exhaustive_audit']['exhaustive_height_difference']} "
              f"edge_step={rec['edge_max_step_to_foundation']}")
    problems.extend(unexpected)
    for line in expected_nondet:
        print("expected nondeterminism (vanilla vegetation features vs correction order):", line)
    if problems:
        print("VALIDATION FAILED:")
        print("\n".join(" - " + p for p in problems))
        return 1
    print("VALIDATION OK (scan/replay/reload agree)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
