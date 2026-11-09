#!/usr/bin/env python3
"""
ST-01 Synthetic Terrain Sampling & Exploratory Sensitivity Study
CLASSIFICATION: SYNTHETIC — NOT MINECRAFT WORLDGEN MEASUREMENT

IMPORTANT NOTICE:
This script uses a standalone synthetic 2D Perlin/Simplex noise model.
Its outputs do NOT represent actual Minecraft 26.3 Overworld terrain, biome distributions,
or placement feasibility. All metrics, acceptance rates, and spacing recommendations produced
here are purely unapproved exploratory hypotheses. Real worldgen data must be gathered via
Phase 2 Minecraft server probes (St01DesertWorldgenProbe).
"""

import json
import math
import os
import sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

# Shared cross-language 64-bit mixer (MurmurHash3 finalizer) matching Java St01O14WorldgenFeasibilityTest
def murmur3_64(seed: int, x: int, z: int) -> int:
    MASK64 = 0xFFFFFFFFFFFFFFFF
    coord_hash = (((x & 0xFFFFFFFF) << 32) | (z & 0xFFFFFFFF)) & MASK64
    combined = (seed ^ coord_hash) & MASK64
    combined = (combined ^ (combined >> 33)) & MASK64
    combined = (combined * 0xff51afd7ed558ccd) & MASK64
    combined = (combined ^ (combined >> 33)) & MASK64
    combined = (combined * 0xc4ceb9fe1a85ec53) & MASK64
    combined = (combined ^ (combined >> 33)) & MASK64
    return combined

def get_deterministic_orientation(seed: int, x: int, z: int) -> str:
    h = murmur3_64(seed, x, z)
    return "EAST" if (h & 1) == 0 else "WEST"

# Reproducible pseudo-random noise generator for deterministic terrain/biome synthesis
class SimplexNoise:
    def __init__(self, seed):
        rng = np.random.RandomState(seed & 0xFFFFFFFF)
        self.p = rng.permutation(256)
        self.p = np.concatenate([self.p, self.p])

    def _fade(self, t):
        return t * t * t * (t * (t * 6 - 15) + 10)

    def _lerp(self, t, a, b):
        return a + t * (b - a)

    def _grad(self, hash_val, x, z):
        h = hash_val & 7
        u = x if h < 4 else z
        v = z if h < 4 else x
        return (u if (h & 1) == 0 else -u) + (v if (h & 2) == 0 else -v)

    def noise2d(self, x, z):
        X = int(math.floor(x)) & 255
        Z = int(math.floor(z)) & 255
        xf = x - math.floor(x)
        zf = z - math.floor(z)
        u = self._fade(xf)
        v = self._fade(zf)
        p = self.p
        aa = p[p[X] + Z]
        ab = p[p[X] + Z + 1]
        ba = p[p[X + 1] + Z]
        bb = p[p[X + 1] + Z + 1]
        x1 = self._lerp(u, self._grad(aa, xf, zf), self._grad(ba, xf - 1, zf))
        x2 = self._lerp(u, self._grad(ab, xf, zf - 1), self._grad(bb, xf - 1, zf - 1))
        return self._lerp(v, x1, x2)

class SyntheticOverworldModel:
    """Synthetic Overworld terrain & biome proxy model (NOT real Minecraft 26.3)."""
    def __init__(self, seed):
        self.seed = seed
        self.n_temp = SimplexNoise(seed ^ 0x3A5B8)
        self.n_hum = SimplexNoise(seed ^ 0x91F4C)
        self.n_cont = SimplexNoise(seed ^ 0x51E2D)
        self.n_dune1 = SimplexNoise(seed ^ 0x7E1A2)
        self.n_dune2 = SimplexNoise(seed ^ 0x1C8F3)
        self.n_cave = SimplexNoise(seed ^ 0xA82C4)

    def is_desert(self, x, z):
        cont = self.n_cont.noise2d(x / 1200.0, z / 1200.0)
        if cont < -0.1: return False # Ocean
        temp = self.n_temp.noise2d(x / 800.0, z / 800.0)
        hum = self.n_hum.noise2d(x / 800.0, z / 800.0)
        return temp > 0.15 and hum < -0.10

    def elevation(self, x, z):
        base = 66.0 + 4.0 * self.n_cont.noise2d(x / 600.0, z / 600.0)
        dune_macro = 7.0 * self.n_dune1.noise2d(x / 120.0, z / 120.0)
        dune_micro = 3.0 * self.n_dune2.noise2d(x / 40.0, z / 40.0)
        return round(base + dune_macro + dune_micro, 2)

    def has_cave_void(self, x, z, y):
        cv = self.n_cave.noise2d(x / 30.0, z / 30.0) + math.sin(y / 6.0) * 0.4
        return cv > 0.65

def evaluate_candidate(model, cx, cz):
    fp_radius = 40

    # 1. Multi-Criteria Biome Sampling across 81x81 footprint
    center_is_desert = model.is_desert(cx, cz)
    corners_desert = all(model.is_desert(cx + dx, cz + dz) for dx in (-fp_radius, fp_radius) for dz in (-fp_radius, fp_radius))

    sample_step = 10
    total_samples = 0
    desert_samples = 0
    perimeter_total = 0
    perimeter_desert = 0

    for dx in range(-fp_radius, fp_radius + 1, sample_step):
        for dz in range(-fp_radius, fp_radius + 1, sample_step):
            total_samples += 1
            is_d = model.is_desert(cx + dx, cz + dz)
            if is_d:
                desert_samples += 1
            if abs(dx) == fp_radius or abs(dz) == fp_radius:
                perimeter_total += 1
                if is_d:
                    perimeter_desert += 1

    desert_coverage_ratio = desert_samples / float(total_samples)
    perimeter_is_full_desert = (perimeter_desert == perimeter_total)
    full_100_percent_desert = (desert_samples == total_samples)

    biome_metrics = {
        "center_only": center_is_desert,
        "center_and_4_corners": center_is_desert and corners_desert,
        "perimeter_100_percent": perimeter_is_full_desert,
        "full_footprint_100_percent": full_100_percent_desert,
        "coverage_ratio": desert_coverage_ratio
    }

    # Minimum eligibility filter for evaluating geometry: center must be desert
    if not center_is_desert:
        return {
            "eligible": False,
            "reason": "REJECT_CENTER_NOT_DESERT",
            "biome_metrics": biome_metrics,
            "cx": cx, "cz": cz
        }

    # 2. Elevation distribution across full 81x81 grid
    elevations = []
    perimeter_elevations = []
    for dx in range(-fp_radius, fp_radius + 1, 4):
        for dz in range(-fp_radius, fp_radius + 1, 4):
            elev = model.elevation(cx + dx, cz + dz)
            elevations.append(elev)
            if abs(dx) >= fp_radius - 4 or abs(dz) >= fp_radius - 4:
                perimeter_elevations.append(elev)

    elevations = np.array(elevations)
    perimeter_elevations = np.array(perimeter_elevations)

    min_h = float(np.min(elevations))
    max_h = float(np.max(elevations))
    mean_h = float(np.mean(elevations))
    median_h = float(np.median(elevations))
    std_h = float(np.std(elevations))
    delta_h = float(max_h - min_h)

    # 3. Base Anchor Elevation Choice: y0 = round(mean_h)
    y0 = int(round(mean_h))

    # 4. Cutting and filling volume estimates
    cut_depths = np.maximum(0.0, elevations - y0)
    fill_depths = np.maximum(0.0, y0 - elevations)
    cut_volume_approx = int(np.sum(cut_depths) * 16)
    fill_volume_approx = int(np.sum(fill_depths) * 16)

    max_cut_depth = float(max_h - y0)
    max_skirt_depth = float(y0 - min_h)
    exposed_perimeter_ratio = float(np.mean(perimeter_elevations < y0))

    # 5. Cave and fluid interference
    aquifer_risk = (y0 - 32) < 32
    cave_samples = 0
    for dx in (-20, 0, 20):
        for dz in (-20, 0, 20):
            if model.has_cave_void(cx + dx, cz + dz, y0 - 15):
                cave_samples += 1
    has_cave_collision = cave_samples >= 2

    # Deterministic orientation selection using shared 64-bit mixer
    orientation = get_deterministic_orientation(model.seed, cx, cz)

    # Multiple hypothetical exploratory scenarios (NOT approved rules)
    scenarios = {
        "hypothetical_1_strict_4": (full_100_percent_desert and delta_h <= 4.0 and max_skirt_depth <= 2.0 and not has_cave_collision),
        "hypothetical_2_conservative_8": (full_100_percent_desert and delta_h <= 8.0 and max_skirt_depth <= 4.0 and not has_cave_collision),
        "hypothetical_3_moderate_12": (desert_coverage_ratio >= 0.95 and delta_h <= 12.0 and max_skirt_depth <= 6.0),
        "hypothetical_4_permissive_16": (desert_coverage_ratio >= 0.90 and delta_h <= 16.0 and max_skirt_depth <= 10.0),
        "hypothetical_5_unconstrained": (desert_coverage_ratio >= 0.80)
    }

    return {
        "eligible": True,
        "cx": cx, "cz": cz,
        "orientation": orientation,
        "biome_metrics": biome_metrics,
        "elevation_stats": {
            "min_h": min_h, "max_h": max_h, "mean_h": mean_h, "median_h": median_h,
            "std_h": std_h, "delta_h": delta_h
        },
        "chosen_anchor_y0": y0,
        "terrain_correction": {
            "max_cut_depth": max_cut_depth,
            "max_skirt_depth": max_skirt_depth,
            "cut_volume_blocks": cut_volume_approx,
            "fill_volume_blocks": fill_volume_approx,
            "exposed_perimeter_ratio": exposed_perimeter_ratio
        },
        "underground_risks": {
            "aquifer_risk": aquifer_risk,
            "has_cave_collision": has_cave_collision
        },
        "hypothetical_scenario_acceptance": scenarios
    }

def run_study():
    seeds = [1001, 2002, 3003, 4004, 5005]
    area_radius_blocks = 4096
    
    print("Running Part 1: Synthetic Terrain Sampling...")
    all_evaluated_candidates = []
    scenario_counts = {
        "total_evaluated": 0,
        "center_desert_eligible": 0,
        "full_100_desert_eligible": 0,
        "hypothetical_1_strict_4": 0,
        "hypothetical_2_conservative_8": 0,
        "hypothetical_3_moderate_12": 0,
        "hypothetical_4_permissive_16": 0,
        "hypothetical_5_unconstrained": 0
    }

    deltas_all = []
    cuts_all = []
    skirts_all = []

    for seed in seeds:
        model = SyntheticOverworldModel(seed)
        for x in range(-area_radius_blocks + 128, area_radius_blocks, 256):
            for z in range(-area_radius_blocks + 128, area_radius_blocks, 256):
                res = evaluate_candidate(model, x, z)
                scenario_counts["total_evaluated"] += 1
                if res["eligible"]:
                    scenario_counts["center_desert_eligible"] += 1
                    if res["biome_metrics"]["full_footprint_100_percent"]:
                        scenario_counts["full_100_desert_eligible"] += 1
                    deltas_all.append(res["elevation_stats"]["delta_h"])
                    cuts_all.append(res["terrain_correction"]["cut_volume_blocks"])
                    skirts_all.append(res["terrain_correction"]["max_skirt_depth"])
                    for sc, passed in res["hypothetical_scenario_acceptance"].items():
                        if passed:
                            scenario_counts[sc] += 1
                    if len(all_evaluated_candidates) < 100:
                        all_evaluated_candidates.append(res)

    terrain_summary = {
        "provenance": "SYNTHETIC — NOT MINECRAFT WORLDGEN MEASUREMENT",
        "notice": (
            "All data in this document originates from a standalone Python Simplex noise model. "
            "It does NOT reflect real Minecraft 26.3 worldgen and must not be used for production decisions."
        ),
        "seeds_sampled": seeds,
        "area_sampled_sq_km": ((area_radius_blocks * 2) / 1000.0) ** 2,
        "candidates_evaluated": scenario_counts["total_evaluated"],
        "center_desert_eligible_candidates": scenario_counts["center_desert_eligible"],
        "full_100_desert_eligible_candidates": scenario_counts["full_100_desert_eligible"],
        "synthetic_biome_frequency": {
            "center_only": scenario_counts["center_desert_eligible"] / float(scenario_counts["total_evaluated"]),
            "full_100_percent": scenario_counts["full_100_desert_eligible"] / float(scenario_counts["total_evaluated"])
        },
        "synthetic_elevation_delta_distribution": {
            "mean_delta_h": float(np.mean(deltas_all)),
            "median_delta_h": float(np.median(deltas_all)),
            "std_delta_h": float(np.std(deltas_all)),
            "min_delta_h": float(np.min(deltas_all)),
            "max_delta_h": float(np.max(deltas_all)),
            "percentiles": {
                "p25": float(np.percentile(deltas_all, 25)),
                "p50": float(np.percentile(deltas_all, 50)),
                "p75": float(np.percentile(deltas_all, 75)),
                "p90": float(np.percentile(deltas_all, 90))
            }
        },
        "hypothetical_scenario_rates": {
            "hypothetical_1_strict_4": {
                "accepted_count": scenario_counts["hypothetical_1_strict_4"],
                "rate_of_desert_candidates": scenario_counts["hypothetical_1_strict_4"] / float(scenario_counts["center_desert_eligible"])
            },
            "hypothetical_2_conservative_8": {
                "accepted_count": scenario_counts["hypothetical_2_conservative_8"],
                "rate_of_desert_candidates": scenario_counts["hypothetical_2_conservative_8"] / float(scenario_counts["center_desert_eligible"])
            },
            "hypothetical_3_moderate_12": {
                "accepted_count": scenario_counts["hypothetical_3_moderate_12"],
                "rate_of_desert_candidates": scenario_counts["hypothetical_3_moderate_12"] / float(scenario_counts["center_desert_eligible"])
            },
            "hypothetical_4_permissive_16": {
                "accepted_count": scenario_counts["hypothetical_4_permissive_16"],
                "rate_of_desert_candidates": scenario_counts["hypothetical_4_permissive_16"] / float(scenario_counts["center_desert_eligible"])
            },
            "hypothetical_5_unconstrained": {
                "accepted_count": scenario_counts["hypothetical_5_unconstrained"],
                "rate_of_desert_candidates": scenario_counts["hypothetical_5_unconstrained"] / float(scenario_counts["center_desert_eligible"])
            }
        },
        "sample_candidate_records": all_evaluated_candidates
    }

    out_terrain = os.path.join(ROOT, "docs", "design", "st01-o14", "st01_o14_terrain_measurements.json")
    with open(out_terrain, "w") as f:
        json.dump(terrain_summary, f, indent=2)
    print(f"Wrote synthetic terrain measurements to {out_terrain}")

    # -------------------------------------------------------------
    # Part 2: Exploratory Spacing Sensitivity Analysis
    # -------------------------------------------------------------
    print("Running Part 2: Exploratory Spacing Sensitivity Analysis...")
    spacing_configs = [
        {"name": "dense_32_16", "spacing_chunks": 32, "separation_chunks": 16, "spacing_blocks": 512, "separation_blocks": 256},
        {"name": "standard_48_24", "spacing_chunks": 48, "separation_chunks": 24, "spacing_blocks": 768, "separation_blocks": 384},
        {"name": "moderate_64_32", "spacing_chunks": 64, "separation_chunks": 32, "spacing_blocks": 1024, "separation_blocks": 512},
        {"name": "rare_96_48", "spacing_chunks": 96, "separation_chunks": 48, "spacing_blocks": 1536, "separation_blocks": 768},
        {"name": "monumental_128_64", "spacing_chunks": 128, "separation_chunks": 64, "spacing_blocks": 2048, "separation_blocks": 1024}
    ]

    spacing_results = []

    for cfg in spacing_configs:
        sp_blocks = cfg["spacing_blocks"]
        sep_blocks = cfg["separation_blocks"]
        total_grid_cells = 0
        total_biome_eligible = 0
        total_terrain_eligible_scen2 = 0
        total_terrain_eligible_scen3 = 0
        total_placed_scen2 = 0
        total_placed_scen3 = 0
        placed_coords_scen3 = []

        for seed in seeds:
            model = SyntheticOverworldModel(seed)
            seed_placed = []
            for gx in range(-area_radius_blocks, area_radius_blocks, sp_blocks):
                for gz in range(-area_radius_blocks, area_radius_blocks, sp_blocks):
                    total_grid_cells += 1
                    rng = np.random.RandomState((seed ^ (gx * 31 + gz)) & 0xFFFFFFFF)
                    jitter_range = sp_blocks - sep_blocks
                    cx = gx + (rng.randint(0, jitter_range) if jitter_range > 0 else 0)
                    cz = gz + (rng.randint(0, jitter_range) if jitter_range > 0 else 0)

                    cand = evaluate_candidate(model, cx, cz)
                    if cand["eligible"]:
                        total_biome_eligible += 1
                        if cand["hypothetical_scenario_acceptance"]["hypothetical_2_conservative_8"]:
                            total_terrain_eligible_scen2 += 1
                            if all(math.hypot(cx - px, cz - pz) >= sep_blocks for px, pz in seed_placed):
                                total_placed_scen2 += 1

                        if cand["hypothetical_scenario_acceptance"]["hypothetical_3_moderate_12"]:
                            total_terrain_eligible_scen3 += 1
                            if all(math.hypot(cx - px, cz - pz) >= sep_blocks for px, pz in seed_placed):
                                total_placed_scen3 += 1
                                seed_placed.append((cx, cz))
                                placed_coords_scen3.append((cx, cz))

        nn_distances = []
        for i in range(len(placed_coords_scen3)):
            min_dist = float("inf")
            for j in range(len(placed_coords_scen3)):
                if i != j:
                    d = math.hypot(placed_coords_scen3[i][0] - placed_coords_scen3[j][0],
                                   placed_coords_scen3[i][1] - placed_coords_scen3[j][1])
                    if d < min_dist:
                        min_dist = d
            if min_dist != float("inf"):
                nn_distances.append(min_dist)

        sampled_area_sq_km = len(seeds) * (((area_radius_blocks * 2) / 1000.0) ** 2)
        structures_per_100_sq_km = (total_placed_scen3 / sampled_area_sq_km) * 100.0
        hypothetical_sqrt_radius = math.sqrt((sampled_area_sq_km * 1_000_000.0) / total_placed_scen3) if total_placed_scen3 > 0 else float("inf")

        spacing_results.append({
            "config": cfg,
            "total_grid_candidates": total_grid_cells,
            "center_biome_eligible_candidates": total_biome_eligible,
            "biome_pass_rate": total_biome_eligible / float(total_grid_cells),
            "hypothetical_scenario_2": {
                "terrain_eligible": total_terrain_eligible_scen2,
                "successfully_placed": total_placed_scen2,
                "realized_acceptance_rate": total_placed_scen2 / float(total_grid_cells)
            },
            "hypothetical_scenario_3": {
                "terrain_eligible": total_terrain_eligible_scen3,
                "successfully_placed": total_placed_scen3,
                "realized_acceptance_rate": total_placed_scen3 / float(total_grid_cells),
                "hypothetical_density_per_100_sq_km": structures_per_100_sq_km,
                "hypothetical_sqrt_area_radius_blocks": hypothetical_sqrt_radius,
                "exploration_note": (
                    "CAUTION: sqrt(area / count) assumes uniform 2D distribution across continuous land. "
                    "In actual gameplay, deserts are non-contiguous patches (~7% of world), meaning player travel "
                    "distance is governed by desert finding + desert traversal, not uniform grid density."
                ),
                "nearest_neighbor_distance": {
                    "mean_blocks": float(np.mean(nn_distances)) if nn_distances else None,
                    "min_blocks": float(np.min(nn_distances)) if nn_distances else None,
                    "max_blocks": float(np.max(nn_distances)) if nn_distances else None
                }
            }
        })

    spacing_summary = {
        "provenance": "SYNTHETIC — NOT MINECRAFT WORLDGEN MEASUREMENT",
        "status": "UNAPPROVED_EXPLORATORY_HYPOTHESIS",
        "disclaimer": (
            "These spacing trade-offs are derived solely from a synthetic noise simulation. "
            "No parameter here is an approved production recommendation."
        ),
        "seeds_sampled": seeds,
        "sampled_area_sq_km_per_seed": ((area_radius_blocks * 2) / 1000.0) ** 2,
        "total_sampled_area_sq_km": sampled_area_sq_km,
        "spacing_configurations": spacing_results
    }

    out_spacing = os.path.join(ROOT, "docs", "design", "st01-o14", "st01_o14_spacing_analysis.json")
    with open(out_spacing, "w") as f:
        json.dump(spacing_summary, f, indent=2)
    print(f"Wrote synthetic spacing analysis to {out_spacing}")

if __name__ == "__main__":
    run_study()
