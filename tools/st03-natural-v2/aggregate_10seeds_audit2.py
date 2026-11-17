"""Fetch 10-seed check-run outputs from GitHub API and aggregate all 8 ST-03 2nd terrain audit items."""
import csv
import json
import math
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path

SEEDS = [10079, 10033, 10148, 10137, 10142, 10039, 10135, 10061, 314159265, 867530926]
ORIG_8 = [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
    "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest"
]
ADDED_11 = [
    "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:taiga", "minecraft:snowy_plains",
    "minecraft:snowy_taiga", "minecraft:dappled_forest", "minecraft:pale_garden",
    "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:sparse_jungle",
    "minecraft:wooded_badlands"
]
ALL_19 = ORIG_8 + ADDED_11


def fetch_seed_results(commit_sha: str) -> dict:
    cmd = ["gh", "api", "--paginate", f"repos/ItzGamesGD/MoreMinecrafts-Beta/commits/{commit_sha}/check-runs", "--jq", ".check_runs[] | {id: .id, name: .name}"]
    out = subprocess.check_output(cmd, text=True)
    by_seed_parts = defaultdict(dict)
    for line in out.strip().splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        name = row["name"]
        if not name.startswith("st03-natural-"):
            continue
        rest = name[len("st03-natural-"):]
        if rest.startswith("inspect"):
            continue
        parts = rest.rsplit("-", 1)
        if len(parts) != 2:
            continue
        seed_str, part_idx = parts[0], int(parts[1])
        # Keep highest check-run id if re-run
        cid = row["id"]
        if part_idx not in by_seed_parts[seed_str] or cid > by_seed_parts[seed_str][part_idx]:
            by_seed_parts[seed_str][part_idx] = cid

    results = {}
    for seed in SEEDS:
        s_key = str(seed)
        if s_key not in by_seed_parts:
            raise RuntimeError(f"Missing check-run for seed {seed}")
        chunks = []
        for idx in sorted(by_seed_parts[s_key].keys()):
            cid = by_seed_parts[s_key][idx]
            txt = subprocess.check_output(
                ["gh", "api", f"repos/ItzGamesGD/MoreMinecrafts-Beta/check-runs/{cid}", "--jq", ".output.text"],
                text=True
            )
            # gh api --jq adds a trailing newline
            if txt.endswith("\n"):
                txt = txt[:-1]
            chunks.append(txt)
        full_text = "".join(chunks)
        results[s_key] = json.loads(full_text)
    return results


def quantiles_from_hist(hist: Counter) -> dict:
    total = sum(hist.values())
    if total == 0:
        return {"n": 0}
    sorted_items = sorted(hist.items())
    mn = sorted_items[0][0]
    mx = sorted_items[-1][0]
    mean = sum(k * v for k, v in sorted_items) / total
    res = {"n": total, "min": mn, "max": mx, "mean": round(mean, 2)}
    for p in (5, 10, 25, 50, 75, 90, 95):
        target = max(1, math.ceil(total * p / 100.0))
        cum = 0
        for k, v in sorted_items:
            cum += v
            if cum >= target:
                res[f"p{p:02d}"] = k
                break
    return res


def merge_counters(dicts):
    c = Counter()
    for d in dicts:
        for k, v in d.items():
            c[k] += v
    return dict(c)


def main():
    commit_sha = sys.argv[1]
    out_dir = Path(sys.argv[2])
    out_dir.mkdir(parents=True, exist_ok=True)

    per_seed = fetch_seed_results(commit_sha)
    (out_dir / "per_seed_raw.json").write_text(json.dumps(per_seed, indent=2))

    seeds_data = [per_seed[str(s)] for s in SEEDS]
    for sd in seeds_data:
        if sd.get("status") != "PASS":
            raise RuntimeError(f"Seed {sd.get('seed')} failed: {sd.get('error')}")

    agg = {
        "commit_sha": commit_sha,
        "seeds": SEEDS,
        "total_candidates": sum(sd["total_candidates"] for sd in seeds_data),
        "valid_starts_19": sum(sd["valid_starts_19"] for sd in seeds_data),
        "valid_starts_8": sum(sd["valid_starts_8"] for sd in seeds_data),
        "valid_starts_overlap_any_3d": sum(sd["valid_starts_overlap_any"] for sd in seeds_data),
        "valid_starts_overlap_village_3d": sum(sd["valid_starts_overlap_village"] for sd in seeds_data),
        "valid_starts_overlap_any_2d": sum(sd.get("valid_starts_overlap_any_2d", 0) for sd in seeds_data),
        "valid_starts_overlap_village_2d": sum(sd.get("valid_starts_overlap_village_2d", 0) for sd in seeds_data),
        "surface_queries": sum(sd["surface_queries"] for sd in seeds_data),
        "dense_chunk_reads": sum(sd["dense_chunk_reads"] for sd in seeds_data),
        "elapsed_seconds_per_seed": {str(sd["seed"]): round(sd["elapsed_seconds"], 1) for sd in seeds_data},
        "valid_starts_19_per_seed": {str(sd["seed"]): sd["valid_starts_19"] for sd in seeds_data},
        "valid_starts_8_per_seed": {str(sd["seed"]): sd["valid_starts_8"] for sd in seeds_data},
    }

    # 1 & 2. N=0 and N=2 Pareto (19-biome and 8-biome)
    for key in ("n0_pareto_19", "n0_pareto_8"):
        by_b = defaultdict(Counter)
        for sd in seeds_data:
            for b, bd in sd[key]["by_biome"].items():
                for k, v in bd.items():
                    by_b[b][k] += v
        agg[key] = {
            "first_sample": merge_counters([sd[key]["first_sample"] for sd in seeds_data]),
            "first_with_dense": merge_counters([sd[key]["first_with_dense"] for sd in seeds_data]),
            "indep_fail_all": merge_counters([sd[key]["indep_fail_all"] for sd in seeds_data]),
            "indep_fail_center_el": merge_counters([sd[key]["indep_fail_center_el"] for sd in seeds_data]),
            "sole_fail_sample_center_el": merge_counters([sd[key]["sole_fail_sample_center_el"] for sd in seeds_data]),
            "sole_fail_with_dense_center_el": merge_counters([sd[key]["sole_fail_with_dense_center_el"] for sd in seeds_data]),
            "fail_count_dist_center_el": merge_counters([sd[key]["fail_count_dist_center_el"] for sd in seeds_data]),
            "by_biome": {b: dict(c) for b, c in sorted(by_b.items())},
        }

    for key in ("n2_pareto_19", "n2_pareto_8"):
        by_b = defaultdict(Counter)
        for sd in seeds_data:
            for b, bd in sd[key]["deepest_by_n0_biome_center_el"].items():
                for k, v in bd.items():
                    by_b[b][k] += v
        agg[key] = {
            "deepest_sample": merge_counters([sd[key]["deepest_sample"] for sd in seeds_data]),
            "deepest_dense": merge_counters([sd[key]["deepest_dense"] for sd in seeds_data]),
            "deepest_by_n0_biome_center_el": {b: dict(c) for b, c in sorted(by_b.items())},
        }

    # 3. Single relaxations & search radii N=0..4
    relax_keys = list(seeds_data[0]["single_relaxations"].keys())
    agg["single_relaxations"] = {
        rk: merge_counters([sd["single_relaxations"][rk] for sd in seeds_data])
        for rk in relax_keys
    }
    agg["search_radii_n0_to_n4"] = {
        f"N{n}": merge_counters([sd["search_radii_n0_to_n4"][f"N{n}"] for sd in seeds_data])
        for n in range(5)
    }

    # 4. Footprint sizes (112, 96, 80, 64)
    fp_agg = {}
    for sz in ("112", "96", "80", "64"):
        fp_agg[sz] = {
            "first_sample": merge_counters([sd["footprint_sizes_n0"][sz]["first_sample"] for sd in seeds_data]),
            "first_with_dense": merge_counters([sd["footprint_sizes_n0"][sz]["first_with_dense"] for sd in seeds_data]),
            "indep_fail_center_el": merge_counters([sd["footprint_sizes_n0"][sz]["indep_fail_center_el"] for sd in seeds_data]),
            "n2_counts": merge_counters([sd["footprint_sizes_n0"][sz].get("n2_counts", {}) for sd in seeds_data]),
            "mean_p_width_center_el": round(
                sum(sd["footprint_sizes_n0"][sz]["p_width_center_el"]["mean"] * sd["footprint_sizes_n0"][sz]["p_width_center_el"]["n"] for sd in seeds_data)
                / sum(sd["footprint_sizes_n0"][sz]["p_width_center_el"]["n"] for sd in seeds_data), 2
            ),
            "mean_r_width_center_el": round(
                sum(sd["footprint_sizes_n0"][sz]["r_width_center_el"]["mean"] * sd["footprint_sizes_n0"][sz]["r_width_center_el"]["n"] for sd in seeds_data)
                / sum(sd["footprint_sizes_n0"][sz]["r_width_center_el"]["n"] for sd in seeds_data), 2
            ),
        }
    agg["footprint_sizes"] = fp_agg

    # 5. Elevation & slope analysis (exact quantiles from merged histograms!)
    elev_groups = [
        "center_el_all_n0", "center_el_failed_n0", "center_el_passed_n0",
        "center_el_failed_n2", "flat_biomes_failed_n0", "hilly_biomes_failed_n0"
    ]
    elev_agg = {}
    for grp in elev_groups:
        p_hist = Counter()
        r_hist = Counter()
        octs = Counter()
        total_n = 0
        weighted_slope = 0.0
        weighted_tail = 0.0
        for sd in seeds_data:
            g = sd["elevation_analysis"][grp]
            n = g["p_width"]["n"]
            total_n += n
            weighted_slope += g["slope_mag_112"]["mean"] * n
            weighted_tail += g["outlier_tail_r_minus_p"]["mean"] * n
            for k, v in g["p_hist"].items():
                p_hist[int(k)] += v
            for k, v in g["r_hist"].items():
                r_hist[int(k)] += v
            for k, v in g["slope_octants"].items():
                octs[k] += v
        elev_agg[grp] = {
            "p_width": quantiles_from_hist(p_hist),
            "r_width": quantiles_from_hist(r_hist),
            "mean_outlier_tail_r_minus_p": round(weighted_tail / total_n, 2) if total_n else 0,
            "mean_slope_mag_112": round(weighted_slope / total_n, 2) if total_n else 0,
            "slope_octants": dict(octs),
            "p_hist": {str(k): v for k, v in sorted(p_hist.items())},
            "r_hist": {str(k): v for k, v in sorted(r_hist.items())},
        }

    by_biome_elev = {}
    for b in ALL_19:
        tot_cel = sum(sd["elevation_analysis"]["by_biome"][b]["all_center_el_p"].get("n", 0) for sd in seeds_data)
        tot_fail = sum(sd["elevation_analysis"]["by_biome"][b]["failed_n0_p"].get("n", 0) for sd in seeds_data)
        if tot_cel > 0:
            mean_p_cel = sum(sd["elevation_analysis"]["by_biome"][b]["all_center_el_p"].get("mean", 0) * sd["elevation_analysis"]["by_biome"][b]["all_center_el_p"].get("n", 0) for sd in seeds_data) / tot_cel
            mean_r_cel = sum(sd["elevation_analysis"]["by_biome"][b]["all_center_el_r"].get("mean", 0) * sd["elevation_analysis"]["by_biome"][b]["all_center_el_r"].get("n", 0) for sd in seeds_data) / tot_cel
        else:
            mean_p_cel = mean_r_cel = 0.0
        if tot_fail > 0:
            mean_p_fail = sum(sd["elevation_analysis"]["by_biome"][b]["failed_n0_p"].get("mean", 0) * sd["elevation_analysis"]["by_biome"][b]["failed_n0_p"].get("n", 0) for sd in seeds_data) / tot_fail
            mean_r_fail = sum(sd["elevation_analysis"]["by_biome"][b]["failed_n0_r"].get("mean", 0) * sd["elevation_analysis"]["by_biome"][b]["failed_n0_r"].get("n", 0) for sd in seeds_data) / tot_fail
            mean_slope_fail = sum(sd["elevation_analysis"]["by_biome"][b]["failed_n0_slope_mag"].get("mean", 0) * sd["elevation_analysis"]["by_biome"][b]["failed_n0_slope_mag"].get("n", 0) for sd in seeds_data) / tot_fail
        else:
            mean_p_fail = mean_r_fail = mean_slope_fail = 0.0
        by_biome_elev[b] = {
            "n_center_el": tot_cel,
            "mean_p_center_el": round(mean_p_cel, 2),
            "mean_r_center_el": round(mean_r_cel, 2),
            "n_failed_n0": tot_fail,
            "mean_p_failed_n0": round(mean_p_fail, 2),
            "mean_r_failed_n0": round(mean_r_fail, 2),
            "mean_slope_failed_n0": round(mean_slope_fail, 2),
        }
    elev_agg["by_biome_summary"] = by_biome_elev
    agg["elevation_analysis"] = elev_agg

    # 6. Sample vs Dense discrepancy
    svd = {
        "n0_sample_pass_count": sum(sd["sample_vs_dense"]["n0_sample_pass_count"] for sd in seeds_data),
        "n0_breakdown": merge_counters([sd["sample_vs_dense"]["n0_breakdown"] for sd in seeds_data]),
        "n0_max_cut_increase_hist": merge_counters([sd["sample_vs_dense"]["n0_max_cut_increase_hist"] for sd in seeds_data]),
        "n0_max_skirt_increase_hist": merge_counters([sd["sample_vs_dense"]["n0_max_skirt_increase_hist"] for sd in seeds_data]),
        "n0_max_fill_increase_hist": merge_counters([sd["sample_vs_dense"]["n0_max_fill_increase_hist"] for sd in seeds_data]),
        "n2_candidate_level": merge_counters([sd["sample_vs_dense"]["n2_candidate_level"] for sd in seeds_data]),
        "n2_site_level": merge_counters([sd["sample_vs_dense"]["n2_site_level"] for sd in seeds_data]),
    }
    agg["sample_vs_dense"] = svd

    # 7. Other variables
    all_overlaps = []
    for sd in seeds_data:
        all_overlaps.extend(sd["other_variables"]["valid_overlaps_detail"])

    ov_by_type_3d = Counter()
    ov_by_type_2d = Counter()
    for item in all_overlaps:
        types3 = {s.split("@")[0] for s in item.get("overlaps_3d", [])}
        types2 = {s.split("@")[0] for s in item.get("overlaps_2d", [])}
        for t in types3:
            ov_by_type_3d[t] += 1
        for t in types2:
            ov_by_type_2d[t] += 1

    agg["other_variables"] = {
        "ocean_river_adjacency": merge_counters([sd["other_variables"]["ocean_river_adjacency"] for sd in seeds_data]),
        "triangular_offset_x_hist": merge_counters([sd["other_variables"]["triangular_offset_x_hist"] for sd in seeds_data]),
        "triangular_offset_z_hist": merge_counters([sd["other_variables"]["triangular_offset_z_hist"] for sd in seeds_data]),
        "triangular_chebyshev_total": merge_counters([sd["other_variables"]["triangular_chebyshev_total"] for sd in seeds_data]),
        "triangular_chebyshev_center_el": merge_counters([sd["other_variables"]["triangular_chebyshev_center_el"] for sd in seeds_data]),
        "triangular_chebyshev_pass_n0": merge_counters([sd["other_variables"]["triangular_chebyshev_pass_n0"] for sd in seeds_data]),
        "triangular_chebyshev_pass_n2": merge_counters([sd["other_variables"]["triangular_chebyshev_pass_n2"] for sd in seeds_data]),
        "overlaps_by_structure_type_3d": dict(ov_by_type_3d),
        "overlaps_by_structure_type_2d": dict(ov_by_type_2d),
        "valid_overlaps_detail": all_overlaps,
    }

    (out_dir / "st03_terrain_audit_v2_2026-11-17_summary.json").write_text(json.dumps(agg, indent=2))
    print("Aggregated 10-seed summary written successfully.")


if __name__ == "__main__":
    main()
