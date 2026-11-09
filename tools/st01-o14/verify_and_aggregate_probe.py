#!/usr/bin/env python3
"""
ST-01 O-14 Phase 2 Real Minecraft Worldgen Probe Verification and Aggregation Tool.

Validates:
1. Single-seed measurement schema, arithmetic, and candidate sanity.
2. Deterministic replay consistency between two runs of the same seed.
3. Multi-seed measurement aggregation and output synthesis.
"""

import sys
import json
import math
from pathlib import Path


def validate_single_seed_file(file_path: Path) -> dict:
    if not file_path.exists():
        raise FileNotFoundError(f"Measurement file not found: {file_path}")

    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 1. Provenance & Version
    assert data.get("provenance") == "REAL_MINECRAFT_26_3_WORLDGEN_MEASUREMENT", f"Invalid provenance: {data.get('provenance')}"
    assert data.get("minecraft_version") == "26.3", f"Unexpected MC version: {data.get('minecraft_version')}"
    assert "world_seed" in data, "Missing world_seed"
    assert data.get("heightmap_type") == "WORLD_SURFACE_WG", f"Unexpected heightmap: {data.get('heightmap_type')}"

    # 2. Candidate counts
    counts = data.get("candidate_counts", {})
    total = counts.get("total_grid_candidates", 0)
    center_desert = counts.get("center_in_desert", 0)
    corners_desert = counts.get("center_and_corners_in_desert", 0)
    full_desert = counts.get("full_100_percent_in_desert", 0)
    eligible = counts.get("eligible_under_exploratory_limits", 0)

    assert total > 0, f"No candidates evaluated in {file_path}"
    assert 0 <= center_desert <= total, "Invalid center_in_desert count"
    assert 0 <= corners_desert <= center_desert, "corners_desert must be <= center_desert"
    assert 0 <= full_desert <= center_desert, "full_desert must be <= center_desert"
    assert 0 <= eligible <= center_desert, "eligible must be <= center_desert"

    # 3. Elevation Distribution
    dist = data.get("real_desert_elevation_delta_distribution")
    if dist:
        min_dh = dist.get("min_delta_h", 0.0)
        max_dh = dist.get("max_delta_h", 0.0)
        median_dh = dist.get("median_delta_h", 0.0)
        assert min_dh <= median_dh <= max_dh, f"Inconsistent delta_h distribution: min={min_dh}, med={median_dh}, max={max_dh}"

    # 4. Sample candidate records
    records = data.get("sample_candidate_records", [])
    for r in records:
        elev = r.get("elevation", {})
        min_h = elev.get("min_h", 0)
        max_h = elev.get("max_h", 0)
        delta_h = elev.get("delta_h", 0)
        assert min_h <= max_h, f"min_h ({min_h}) > max_h ({max_h})"
        assert delta_h == (max_h - min_h), f"delta_h ({delta_h}) != max_h - min_h ({max_h - min_h})"

        orientation = r.get("orientation")
        assert orientation in ("EAST", "WEST"), f"Invalid orientation: {orientation}"

    print(f"PASS: {file_path.name} validated successfully (seed={data.get('world_seed')}, evaluated={total}, desert={center_desert}, eligible={eligible})")
    return data


def verify_replay(file1: Path, file2: Path) -> bool:
    print(f"\n--- Verifying Deterministic Replay between {file1.name} and {file2.name} ---")
    data1 = validate_single_seed_file(file1)
    data2 = validate_single_seed_file(file2)

    assert data1["world_seed"] == data2["world_seed"], f"Seeds do not match: {data1['world_seed']} vs {data2['world_seed']}"
    assert data1["candidate_counts"] == data2["candidate_counts"], f"Candidate counts mismatch: {data1['candidate_counts']} vs {data2['candidate_counts']}"
    assert data1.get("rejection_breakdown") == data2.get("rejection_breakdown"), "Rejection breakdown mismatch"

    records1 = data1.get("sample_candidate_records", [])
    records2 = data2.get("sample_candidate_records", [])
    assert len(records1) == len(records2), f"Record counts mismatch: {len(records1)} vs {len(records2)}"

    for i, (r1, r2) in enumerate(zip(records1, records2)):
        assert r1["anchor_x"] == r2["anchor_x"] and r1["anchor_z"] == r2["anchor_z"], f"Coord mismatch at index {i}"
        assert r1["orientation"] == r2["orientation"], f"Orientation mismatch at index {i}"
        assert r1["elevation"] == r2["elevation"], f"Elevation mismatch at index {i}"
        assert r1["biome_coverage"] == r2["biome_coverage"], f"Biome coverage mismatch at index {i}"
        assert r1["foundation_analysis"] == r2["foundation_analysis"], f"Foundation mismatch at index {i}"

    print(f"REPLAY_PASS: Byte/value-identical match across {len(records1)} candidates between runs!")
    return True


def aggregate_multiseed(files: list[Path], output_path: Path) -> dict:
    print(f"\n--- Aggregating {len(files)} Seed Measurements ---")
    all_data = [validate_single_seed_file(f) for f in files]

    total_evaluated = sum(d["candidate_counts"]["total_grid_candidates"] for d in all_data)
    total_center_desert = sum(d["candidate_counts"]["center_in_desert"] for d in all_data)
    total_corners_desert = sum(d["candidate_counts"]["center_and_corners_in_desert"] for d in all_data)
    total_full_desert = sum(d["candidate_counts"]["full_100_percent_in_desert"] for d in all_data)
    total_eligible = sum(d["candidate_counts"]["eligible_under_exploratory_limits"] for d in all_data)

    all_delta_h = []
    all_rejections = {}
    seeds_list = []

    for d in all_data:
        seeds_list.append(d["world_seed"])
        for k, v in d.get("rejection_breakdown", {}).items():
            all_rejections[k] = all_rejections.get(k, 0) + v
        for rec in d.get("sample_candidate_records", []):
            dh = rec.get("elevation", {}).get("delta_h")
            if dh is not None:
                all_delta_h.append(float(dh))

    all_delta_h.sort()
    n = len(all_delta_h)

    agg_dist = {}
    if n > 0:
        agg_dist = {
            "sample_size": n,
            "mean_delta_h": round(sum(all_delta_h) / n, 2),
            "median_delta_h": round(all_delta_h[n // 2], 2),
            "min_delta_h": round(all_delta_h[0], 2),
            "max_delta_h": round(all_delta_h[-1], 2),
            "p25_delta_h": round(all_delta_h[int(n * 0.25)], 2),
            "p75_delta_h": round(all_delta_h[int(n * 0.75)], 2),
            "p90_delta_h": round(all_delta_h[int(n * 0.90)], 2),
        }

    aggregate_report = {
        "provenance": "REAL_MINECRAFT_26_3_WORLDGEN_MEASUREMENT_AGGREGATE",
        "minecraft_version": "26.3",
        "seeds_measured": sorted(seeds_list),
        "seed_count": len(seeds_list),
        "total_candidates_evaluated": total_evaluated,
        "aggregate_candidate_counts": {
            "total_grid_candidates": total_evaluated,
            "center_in_desert": total_center_desert,
            "center_and_corners_in_desert": total_corners_desert,
            "full_100_percent_in_desert": total_full_desert,
            "eligible_under_exploratory_limits": total_eligible,
        },
        "aggregate_rates": {
            "center_desert_rate": round(total_center_desert / total_evaluated, 4) if total_evaluated else 0,
            "corners_given_center_desert_rate": round(total_corners_desert / total_center_desert, 4) if total_center_desert else 0,
            "full_given_center_desert_rate": round(total_full_desert / total_center_desert, 4) if total_center_desert else 0,
            "eligibility_rate_overall": round(total_eligible / total_evaluated, 4) if total_evaluated else 0,
            "eligibility_given_center_desert": round(total_eligible / total_center_desert, 4) if total_center_desert else 0,
        },
        "aggregate_rejection_breakdown": all_rejections,
        "aggregate_elevation_delta_distribution": agg_dist,
        "per_seed_summaries": [
            {
                "seed": d["world_seed"],
                "total_candidates": d["candidate_counts"]["total_grid_candidates"],
                "center_desert": d["candidate_counts"]["center_in_desert"],
                "eligible": d["candidate_counts"]["eligible_under_exploratory_limits"],
                "delta_h_summary": d.get("real_desert_elevation_delta_distribution", {})
            }
            for d in all_data
        ]
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(aggregate_report, f, indent=2)

    print(f"MULTISEED_PASS: Aggregate written to {output_path} (seeds={seeds_list}, total_candidates={total_evaluated}, eligible={total_eligible})")
    return aggregate_report


def main():
    if len(sys.argv) < 2:
        print("Usage: verify_and_aggregate_probe.py <command> [args...]")
        print("Commands:")
        print("  verify <file.json>")
        print("  replay <file1.json> <file2.json>")
        print("  aggregate <out.json> <file1.json> [file2.json ...]")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "verify":
        validate_single_seed_file(Path(sys.argv[2]))
    elif cmd == "replay":
        verify_replay(Path(sys.argv[2]), Path(sys.argv[3]))
    elif cmd == "aggregate":
        out = Path(sys.argv[2])
        inputs = [Path(p) for p in sys.argv[3:]]
        aggregate_multiseed(inputs, out)
    else:
        print(f"Unknown command: {cmd}")
        sys.exit(1)


if __name__ == "__main__":
    main()
