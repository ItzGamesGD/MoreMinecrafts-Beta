#!/usr/bin/env python3
"""
tools/st01-o14/aggregate_phase3_1_multiseed.py

Aggregates multiple real Minecraft 26.3 worldgen probe runs across seeds (e.g. 1001-1005)
and generates:
1. phase3_1_raw_measurements.json
2. phase3_1_statistical_summary.json
3. phase3_1_configuration_comparison.csv
"""

import csv
import json
import math
import sys
from pathlib import Path
from typing import Dict, List, Any


def wilson_interval(k: int, n: int, confidence: float = 0.95) -> tuple:
    if n == 0:
        return (0.0, 0.0)
    z = 1.95996  # for 95%
    p = k / n
    denom = 1.0 + z * z / n
    center = (p + z * z / (2 * n)) / denom
    margin = (z * math.sqrt((p * (1 - p) + z * z / (4 * n)) / n)) / denom
    return (max(0.0, center - margin), min(1.0, center + margin))


def aggregate_multiseed(seed_files: List[Path], replay_file: Path, out_dir: Path) -> dict:
    if not seed_files:
        raise ValueError("No seed files provided for aggregation")

    seed_data_list = []
    for f in seed_files:
        if not f.exists():
            raise FileNotFoundError(f"Missing seed file: {f}")
        with open(f, "r", encoding="utf-8") as fp:
            d = json.load(fp)
            seed_data_list.append(d)

    # 1. Deterministic Replay Check (Seed 1002 vs Replay 1002)
    replay_status = "NOT_EVALUATED"
    if replay_file and replay_file.exists():
        with open(replay_file, "r", encoding="utf-8") as fp:
            replay_data = json.load(fp)
        
        # Match seed 1002 against replay
        s1002 = next((d for d in seed_data_list if d.get("world_seed") == 1002), None)
        if s1002:
            assert s1002["world_seed"] == replay_data["world_seed"], "Replay seed mismatch"
            m1 = s1002.get("stage2_woodland_mansion_baseline", {}).get("placement_metrics")
            m2 = replay_data.get("stage2_woodland_mansion_baseline", {}).get("placement_metrics")
            assert m1 == m2, f"Replay mansion placement mismatch: {m1} != {m2}"
            st1 = s1002.get("stage3_st01_placement_simulation")
            st2 = replay_data.get("stage3_st01_placement_simulation")
            assert st1 == st2, "Replay ST-01 simulation mismatch"
            replay_status = "BIT_FOR_BIT_MATCH_CONFIRMED"
        else:
            replay_status = "SEED_1002_MISSING"

    # 2. Raw Measurements Structure
    raw_measurements = {
        "version": "1.0-phase3.1",
        "provenance": "REAL_MINECRAFT_26_3_MULTISEED_VALIDATION",
        "replay_verification": {
            "status": replay_status,
            "seed": 1002
        },
        "seeds_evaluated": [d.get("world_seed") for d in seed_data_list],
        "total_seeds": len(seed_data_list),
        "seed_runs": seed_data_list
    }

    # 3. Statistical Summary Computation
    total_area_per_seed_km2 = seed_data_list[0].get("performance_metrics", {}).get("exploration_area_sq_km", 419.4304)
    total_pooled_area_km2 = total_area_per_seed_km2 * len(seed_data_list)

    # Woodland Mansion Baseline Pool
    wm_counts = []
    wm_nearest_dists = []
    for d in seed_data_list:
        wm_info = d.get("stage2_woodland_mansion_baseline", {})
        metrics = wm_info.get("placement_metrics", {})
        count = metrics.get("valid_placed_structures", 0)
        # If observed_structures exist:
        obs = wm_info.get("observed_structures", [])
        if count == 0 and obs:
            count = len(obs)
        wm_counts.append(count)
        
        dists = [s.get("distance_from_origin") for s in obs if "distance_from_origin" in s]
        cross = wm_info.get("nearest_structure_cross_check")
        if cross and "distance_from_origin" in cross:
            dists.append(cross["distance_from_origin"])
        if dists:
            wm_nearest_dists.append(min(dists))

    wm_total_structures = sum(wm_counts)
    wm_mean_count = sum(wm_counts) / len(wm_counts)
    wm_pooled_density = (wm_total_structures / total_pooled_area_km2) * 100.0 if total_pooled_area_km2 > 0 else 0.0
    wm_zero_seeds = sum(1 for c in wm_counts if c == 0)

    wm_stat_summary = {
        "total_seeds": len(seed_data_list),
        "total_area_sq_km": total_pooled_area_km2,
        "per_seed_counts": wm_counts,
        "total_structures_observed": wm_total_structures,
        "mean_structures_per_seed": round(wm_mean_count, 3),
        "zero_structure_seed_frequency": round(wm_zero_seeds / len(seed_data_list), 4),
        "pooled_density_per_100_sq_km": round(wm_pooled_density, 3),
        "nearest_distance_min": min(wm_nearest_dists) if wm_nearest_dists else None,
        "nearest_distance_mean": round(sum(wm_nearest_dists) / len(wm_nearest_dists), 1) if wm_nearest_dists else None
    }

    # Configuration Evaluation Pool
    cfg_names = [
        "CFG_80_20_TRI",
        "CFG_64_20_TRI",
        "CFG_48_16_TRI",
        "CFG_32_08_TRI",
        "CFG_32_08_LIN",
        "CFG_24_06_TRI"
    ]

    configs_summary = {}
    csv_rows = []

    for cname in cfg_names:
        per_seed_strict = []
        per_seed_exploratory = []
        per_seed_moderate = []
        per_seed_generous = []
        per_seed_full_desert = []
        per_seed_nearest = []

        params = {}

        for d in seed_data_list:
            sim = d.get("stage3_st01_placement_simulation", {})
            cfg_obj = sim.get("configurations", {}).get(cname, {})
            if not params:
                params = cfg_obj.get("parameters", {})
            
            c_counts = cfg_obj.get("counts", {})
            per_seed_strict.append(c_counts.get("eligible_strict", 0))
            per_seed_exploratory.append(c_counts.get("eligible_exploratory", 0))
            per_seed_moderate.append(c_counts.get("eligible_moderate", 0))
            per_seed_generous.append(c_counts.get("eligible_generous", 0))
            per_seed_full_desert.append(c_counts.get("full_100_percent_desert", 0))

            samples = cfg_obj.get("sample_eligible_placements", [])
            dists = [s.get("distance_from_origin") for s in samples if "distance_from_origin" in s]
            if dists:
                per_seed_nearest.append(min(dists))

        n_seeds = len(seed_data_list)
        tot_exp = sum(per_seed_exploratory)
        tot_mod = sum(per_seed_moderate)
        tot_strict = sum(per_seed_strict)
        tot_gen = sum(per_seed_generous)
        tot_full_desert = sum(per_seed_full_desert)

        density_strict = (tot_strict / total_pooled_area_km2) * 100.0
        density_exp = (tot_exp / total_pooled_area_km2) * 100.0
        density_mod = (tot_mod / total_pooled_area_km2) * 100.0
        density_gen = (tot_gen / total_pooled_area_km2) * 100.0

        zero_exp_seeds = sum(1 for c in per_seed_exploratory if c == 0)
        zero_mod_seeds = sum(1 for c in per_seed_moderate if c == 0)

        # Ratios against Woodland Mansion pooled density
        ratio_exp = round(density_exp / wm_pooled_density, 3) if wm_pooled_density > 0 else None
        ratio_mod = round(density_mod / wm_pooled_density, 3) if wm_pooled_density > 0 else None

        pass_parity_strict_0_5_to_2_0 = (ratio_exp is not None and 0.5 <= ratio_exp <= 2.0)
        pass_parity_wide_0_25_to_3_0 = (ratio_exp is not None and 0.25 <= ratio_exp <= 3.0)

        # Confidence interval on exploratory density (Wilson on candidate chunks or Poisson approx)
        ci_exp_lower = max(0.0, (tot_exp - 1.96 * math.sqrt(tot_exp + 0.5)) / total_pooled_area_km2 * 100.0)
        ci_exp_upper = (tot_exp + 1.96 * math.sqrt(tot_exp + 0.5)) / total_pooled_area_km2 * 100.0

        cfg_summary = {
            "configuration": cname,
            "parameters": params,
            "per_seed_counts": {
                "full_100_desert": per_seed_full_desert,
                "eligible_strict": per_seed_strict,
                "eligible_exploratory": per_seed_exploratory,
                "eligible_moderate": per_seed_moderate,
                "eligible_generous": per_seed_generous
            },
            "pooled_counts": {
                "total_full_100_desert": tot_full_desert,
                "total_strict": tot_strict,
                "total_exploratory": tot_exp,
                "total_moderate": tot_mod,
                "total_generous": tot_gen
            },
            "zero_structure_seed_frequency": {
                "exploratory": round(zero_exp_seeds / n_seeds, 4),
                "moderate": round(zero_mod_seeds / n_seeds, 4)
            },
            "densities_per_100_sq_km": {
                "strict": round(density_strict, 3),
                "exploratory": round(density_exp, 3),
                "moderate": round(density_mod, 3),
                "generous": round(density_gen, 3),
                "exploratory_95pct_ci": [round(ci_exp_lower, 3), round(ci_exp_upper, 3)]
            },
            "woodland_mansion_pooled_density": round(wm_pooled_density, 3),
            "rarity_ratio_to_mansion": {
                "exploratory": ratio_exp,
                "moderate": ratio_mod
            },
            "parity_compliance": {
                "strict_standard_0_5_to_2_0": pass_parity_strict_0_5_to_2_0,
                "wide_standard_0_25_to_3_0": pass_parity_wide_0_25_to_3_0,
                "conflict_flag": (pass_parity_strict_0_5_to_2_0 != pass_parity_wide_0_25_to_3_0)
            },
            "nearest_distance": {
                "min": min(per_seed_nearest) if per_seed_nearest else None,
                "mean": round(sum(per_seed_nearest) / len(per_seed_nearest), 1) if per_seed_nearest else None
            }
        }
        configs_summary[cname] = cfg_summary

        csv_rows.append({
            "configuration": cname,
            "spacing": params.get("spacing_chunks"),
            "separation": params.get("separation_chunks"),
            "spread_type": params.get("spread_type"),
            "total_full_desert": tot_full_desert,
            "eligible_exploratory": tot_exp,
            "eligible_moderate": tot_mod,
            "density_exploratory_per_100km2": round(density_exp, 3),
            "density_moderate_per_100km2": round(density_mod, 3),
            "mansion_density_per_100km2": round(wm_pooled_density, 3),
            "rarity_ratio_exploratory": ratio_exp if ratio_exp is not None else "N/A",
            "rarity_ratio_moderate": ratio_mod if ratio_mod is not None else "N/A",
            "zero_seed_freq_exploratory": round(zero_exp_seeds / n_seeds, 2),
            "pass_parity_0_5_to_2_0": pass_parity_strict_0_5_to_2_0,
            "pass_parity_0_25_to_3_0": pass_parity_wide_0_25_to_3_0
        })

    statistical_summary = {
        "version": "1.0-phase3.1",
        "provenance": "PHASE3_1_MULTISEED_STATISTICAL_SUMMARY",
        "multiseed_metadata": {
            "seed_list": [d.get("world_seed") for d in seed_data_list],
            "num_seeds": len(seed_data_list),
            "total_area_evaluated_sq_km": total_pooled_area_km2,
            "replay_status": replay_status
        },
        "woodland_mansion_baseline": wm_stat_summary,
        "configurations": configs_summary
    }

    # Write files
    out_dir.mkdir(parents=True, exist_ok=True)
    raw_path = out_dir / "phase3_1_raw_measurements.json"
    stat_path = out_dir / "phase3_1_statistical_summary.json"
    csv_path = out_dir / "phase3_1_configuration_comparison.csv"

    with open(raw_path, "w", encoding="utf-8") as f:
        json.dump(raw_measurements, f, indent=2)
    with open(stat_path, "w", encoding="utf-8") as f:
        json.dump(statistical_summary, f, indent=2)

    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=list(csv_rows[0].keys()))
        writer.writeheader()
        writer.writerows(csv_rows)

    print(f"Multi-seed aggregation complete:")
    print(f"  Raw measurements: {raw_path}")
    print(f"  Statistical summary: {stat_path}")
    print(f"  Comparison CSV: {csv_path}")

    return statistical_summary


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: aggregate_phase3_1_multiseed.py <out_dir> <replay_file> <seed1.json> <seed2.json> ...")
        sys.exit(1)
    out_dir = Path(sys.argv[1])
    replay_file = Path(sys.argv[2])
    seed_files = [Path(p) for p in sys.argv[3:]]
    aggregate_multiseed(seed_files, replay_file, out_dir)
