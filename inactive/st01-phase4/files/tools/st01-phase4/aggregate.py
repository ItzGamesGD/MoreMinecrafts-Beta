#!/usr/bin/env python3
"""Aggregates per-seed ST-01 Phase 4 scan results: usage aggregate.py <results-root> <out-json> [out-md]
results-root contains <seed>/scan.json. Exits non-zero if no seed is found."""
import glob
import json
import math
import os
import sys

# Phase 3.1 PREDICTIONS (diagnostic, docs/design/st01-o14/phase3_1_statistical_summary.json, CFG_32_08_TRI), seeds 1001..1005
PHASE31 = {
    1001: {"full_100_desert": 16, "moderate": 4},
    1002: {"full_100_desert": 18, "moderate": 6},
    1003: {"full_100_desert": 4, "moderate": 0},
    1004: {"full_100_desert": 8, "moderate": 3},
    1005: {"full_100_desert": 10, "moderate": 2},
}
MANSION_POOLED_DENSITY = 0.715  # per 100 km2, Phase 3.1 measured Woodland Mansion baseline (5 seeds)


def poisson_ci(k, z=1.96):
    """Garwood exact-style approximation (Wilson-Hilferty) 95% CI for a Poisson count."""
    lo = 0.0 if k == 0 else k * (1 - 1 / (9 * k) - z / (3 * math.sqrt(k))) ** 3
    hi = (k + 1) * (1 - 1 / (9 * (k + 1)) + z / (3 * math.sqrt(k + 1))) ** 3
    return max(lo, 0.0), hi


def main():
    root, out_json = sys.argv[1], sys.argv[2]
    out_md = sys.argv[3] if len(sys.argv) > 3 else None
    rows = []
    for path in sorted(glob.glob(os.path.join(root, "*", "scan.json"))):
        d = json.load(open(path))
        if "error" in d:
            print("probe error in", path, d["error"])
            return 1
        st = d["anchor_statistics"]
        rows.append({
            "seed": d["seed"],
            "potential_anchors": st["potential_anchors"],
            "center_only_desert": st["center_only_desert_anchors"],
            "full_footprint_desert": st["desert_footprint_qualified_anchors"],
            "terrain_qualified": st["terrain_qualified_anchors"],
            "fluid_safe": st["fluid_safe_anchors_predicted"],
            "actual_valid_starts": st["actual_valid_starts"],
            "completed": d["completed_structures"],
            "mismatches": st["prediction_vs_actual_mismatches"],
            "rejections": st["rejection_counts_independent"],
            "area_km2": st["area_km2"],
            "density_per_100km2": st["density_per_100km2"],
            "nearest_start_distance_blocks": d["nearest_start_distance_blocks"],
            "starts": {k: {"anchor": v["anchor"], "rotation": v["rotation"], "pass": v["all_checks_pass"]} for k, v in d["starts"].items()},
            "fluid_audit": d.get("fluid_rejection_audit", []),
        })
    if not rows:
        print("no seed results found under", root)
        return 1
    tot_area = sum(r["area_km2"] for r in rows)
    tot_starts = sum(r["actual_valid_starts"] for r in rows)
    lo, hi = poisson_ci(tot_starts)
    agg = {
        "seeds": len(rows),
        "total_area_km2": tot_area,
        "total_starts": tot_starts,
        "total_completed": sum(r["completed"] for r in rows),
        "pooled_density_per_100km2": tot_starts * 100 / tot_area,
        "pooled_density_95ci": [lo * 100 / tot_area, hi * 100 / tot_area],
        "mean_starts_per_seed": tot_starts / len(rows),
        "zero_start_seeds": sum(1 for r in rows if r["actual_valid_starts"] == 0),
        "ratio_to_phase31_mansion_density": (tot_starts * 100 / tot_area) / MANSION_POOLED_DENSITY,
        "total_prediction_mismatches": sum(r["mismatches"] for r in rows),
    }
    ref = [r for r in rows if r["seed"] in PHASE31]
    cmp_rows = [{"seed": r["seed"], "phase31_full_100_desert": PHASE31[r["seed"]]["full_100_desert"], "production_full_footprint_desert": r["full_footprint_desert"],
                 "phase31_moderate": PHASE31[r["seed"]]["moderate"], "production_valid_starts": r["actual_valid_starts"]} for r in ref]
    result = {"aggregate": agg, "per_seed": rows, "phase31_comparison": cmp_rows}
    json.dump(result, open(out_json, "w"), indent=2)

    md = ["| seed | potential | center-only desert | full-footprint desert | terrain-qualified | fluid-safe | valid starts | completed | density /100 km² | nearest (blocks) | rejections |",
          "|---|---|---|---|---|---|---|---|---|---|---|"]
    for r in rows:
        md.append(f"| {r['seed']} | {r['potential_anchors']} | {r['center_only_desert']} | {r['full_footprint_desert']} | {r['terrain_qualified']} | {r['fluid_safe']} | "
                  f"{r['actual_valid_starts']} | {r['completed']} | {r['density_per_100km2']:.3f} | {r['nearest_start_distance_blocks'] and round(r['nearest_start_distance_blocks'])} | {r['rejections']} |")
    md.append("")
    md.append(f"Pooled: {agg['total_starts']} starts over {agg['total_area_km2']:.1f} km² = {agg['pooled_density_per_100km2']:.3f} /100 km² "
              f"(95% CI {agg['pooled_density_95ci'][0]:.3f}-{agg['pooled_density_95ci'][1]:.3f}); seeds with zero starts: {agg['zero_start_seeds']}/{agg['seeds']}; "
              f"ratio to Phase 3.1 Woodland Mansion density {agg['ratio_to_phase31_mansion_density']:.2f}x; independent-prediction mismatches: {agg['total_prediction_mismatches']}")
    if cmp_rows:
        md += ["", "Phase 3.1 prediction vs production (reference seeds):", "", "| seed | P3.1 full-100%-desert | production full-footprint desert | P3.1 moderate-eligible | production valid starts |", "|---|---|---|---|---|"]
        for c in cmp_rows:
            md.append(f"| {c['seed']} | {c['phase31_full_100_desert']} | {c['production_full_footprint_desert']} | {c['phase31_moderate']} | {c['production_valid_starts']} |")
    text = "\n".join(md)
    print(text)
    if out_md:
        open(out_md, "w").write(text + "\n")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        open(os.environ["GITHUB_STEP_SUMMARY"], "a").write(text + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
