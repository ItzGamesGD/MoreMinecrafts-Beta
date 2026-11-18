"""Per-seed aggregation for ST-03 2nd terrain audit. Reads CSVs produced by St03TerrainAuditProbe and writes result.json."""
import csv
import json
import math
import sys
from collections import Counter, defaultdict
from pathlib import Path

ORIG_8 = {
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
    "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest"
}
ADDED_11 = {
    "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:taiga", "minecraft:snowy_plains",
    "minecraft:snowy_taiga", "minecraft:dappled_forest", "minecraft:pale_garden",
    "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:sparse_jungle",
    "minecraft:wooded_badlands"
}
ALL_19 = ORIG_8 | ADDED_11
FLAT_BIOMES = {
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains",
    "minecraft:savanna", "minecraft:forest", "minecraft:flower_forest",
    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
    "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:dappled_forest"
}
HILLY_BIOMES = {
    "minecraft:meadow", "minecraft:savanna_plateau", "minecraft:wooded_badlands",
    "minecraft:sparse_jungle", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
    "minecraft:pale_garden"
}


def bval(s):
    return s == "true" or s is True


def eval_site(row, use19=True, min_biome_pct=80, max_water_pct=5, max_p=20, max_cut=10, max_skirt=12, max_fill=16, check_dense=True):
    c_pass = bval(row["center_pass_19"]) if use19 else bval(row["center_pass_8"])
    if not c_pass:
        return False, "REJECT_CENTER_BIOME"
    good = int(row["good_19"]) if use19 else int(row["good_8"])
    edge = int(row["edge"])
    samples = int(row.get("samples", 225))
    denom = samples - edge
    if denom <= 0 or 100 * good < min_biome_pct * denom:
        return False, "REJECT_FOOTPRINT_BIOME"
    water = int(row.get("water_samples", row.get("water", 0)))
    if 100 * water > max_water_pct * samples:
        return False, "REJECT_WATER"
    p_width = int(row["p_width"])
    if p_width > max_p:
        return False, "REJECT_HEIGHT_DIFFERENCE"
    mx = int(row["max"])
    mn = int(row["min"])
    med = int(row["median"])
    fmin = int(row["floor_min"])
    low = mx - max_cut
    high_skirt = mn + max_skirt
    high_fill = fmin + max_fill
    if low > high_skirt:
        return False, "REJECT_CUT_SKIRT"
    if low > high_fill:
        return False, "REJECT_FOOTING_FILL"
    if check_dense and bval(row["dense_eval"]):
        y = max(low, min(med, min(high_skirt, high_fill)))
        d_max = int(row["dense_max"])
        d_min = int(row["dense_min"])
        d_fmin = int(row["dense_floor_min"])
        d_cut = max(0, d_max - y)
        d_skirt = max(0, y - d_min)
        d_fill = max(0, y - d_fmin)
        if d_cut > max_cut:
            return False, "REJECT_DENSE_CUT"
        if d_skirt > max_skirt:
            return False, "REJECT_DENSE_SKIRT"
        if d_fill > max_fill:
            return False, "REJECT_DENSE_FILL"
    return True, "ELIGIBLE"


def non_short_circuit_flags(row, use19=True, min_biome_pct=80, max_water_pct=5, max_p=20, max_cut=10, max_skirt=12, max_fill=16):
    c_pass = bval(row["center_pass_19"]) if use19 else bval(row["center_pass_8"])
    good = int(row["good_19"]) if use19 else int(row["good_8"])
    edge = int(row["edge"])
    samples = int(row.get("samples", 225))
    denom = samples - edge
    fp_pass = denom > 0 and (100 * good >= min_biome_pct * denom)
    water = int(row.get("water_samples", row.get("water", 0)))
    w_pass = (100 * water <= max_water_pct * samples)
    h_pass = (int(row["p_width"]) <= max_p)
    mx = int(row["max"])
    mn = int(row["min"])
    fmin = int(row["floor_min"])
    low = mx - max_cut
    high_skirt = mn + max_skirt
    high_fill = fmin + max_fill
    cs_pass = (low <= high_skirt)
    ff_pass = (low <= high_fill)
    dense_pass = True
    dense_cut_pass = True
    dense_skirt_pass = True
    dense_fill_pass = True
    if bval(row["dense_eval"]):
        dense_cut_pass = int(row["dense_ex_cut_cols"]) == 0
        dense_skirt_pass = int(row["dense_ex_skirt_cols"]) == 0
        dense_fill_pass = int(row["dense_ex_fill_cols"]) == 0
        dense_pass = int(row["dense_ex_any_cols"]) == 0
    return {
        "center": c_pass,
        "footprint": fp_pass,
        "water": w_pass,
        "height": h_pass,
        "cut_skirt": cs_pass,
        "footing_fill": ff_pass,
        "dense_any": dense_pass,
        "dense_cut": dense_cut_pass,
        "dense_skirt": dense_skirt_pass,
        "dense_fill": dense_fill_pass,
    }


def quantiles(vals):
    if not vals:
        return {"n": 0}
    a = sorted(vals)
    n = len(a)
    res = {"n": n, "min": a[0], "max": a[-1], "mean": round(sum(a) / n, 2)}
    for p in (5, 10, 25, 50, 75, 90, 95):
        res[f"p{p:02d}"] = a[max(0, math.ceil(n * p / 100.0) - 1)]
    return res


def main():
    folder = Path(sys.argv[1])
    summary = json.loads((folder / "audit-summary.json").read_text())
    if summary.get("status") != "PASS":
        (folder / "result.json").write_text(json.dumps(summary, indent=2))
        sys.exit(1)

    cands = list(csv.DictReader((folder / "candidates_audit.csv").open()))
    search_rows = list(csv.DictReader((folder / "search_n2_audit.csv").open()))
    sub_rows = list(csv.DictReader((folder / "subboxes_audit.csv").open()))

    # Index search_n2_audit by (rx, rz)
    by_cand_n2 = defaultdict(list)
    for r in search_rows:
        by_cand_n2[(int(r["rx"]), int(r["rz"]))].append(r)

    # Index subboxes by (size)
    by_sub_size = defaultdict(list)
    for r in sub_rows:
        by_sub_size[int(r["size"])].append(r)

    # 1 & 2. N=0 Pareto (first failure, independent failure, sole failure) for 19-biome and 8-biome
    def analyze_n0(use19):
        first_sample = Counter()
        first_with_dense = Counter()
        indep_fail_all = Counter()
        indep_fail_center_el = Counter()
        sole_fail_sample_center_el = Counter()
        sole_fail_with_dense_center_el = Counter()
        fail_count_dist_center_el = Counter()
        by_biome = defaultdict(lambda: Counter())

        for r in cands:
            b = r["center_biome_lower"]
            ok_s, vf_s = eval_site(r, use19=use19, check_dense=False)
            ok_d, vf_d = eval_site(r, use19=use19, check_dense=True)
            first_sample[vf_s] += 1
            first_with_dense[vf_d] += 1

            flags = non_short_circuit_flags(r, use19=use19)
            sample_stages = ["center", "footprint", "water", "height", "cut_skirt", "footing_fill"]
            for st in sample_stages + ["dense_any", "dense_cut", "dense_skirt", "dense_fill"]:
                if not flags[st]:
                    indep_fail_all[st] += 1

            by_biome[b]["total_n0"] += 1
            if flags["center"]:
                by_biome[b]["center_el"] += 1
                by_biome[b][f"first_s_{vf_s}"] += 1
                by_biome[b][f"first_d_{vf_d}"] += 1

                failed_s = [st for st in ["footprint", "water", "height", "cut_skirt", "footing_fill"] if not flags[st]]
                failed_d = failed_s + ([] if flags["dense_any"] else ["dense_any"])
                fail_count_dist_center_el[len(failed_s)] += 1

                for st in ["footprint", "water", "height", "cut_skirt", "footing_fill", "dense_any", "dense_cut", "dense_skirt", "dense_fill"]:
                    if not flags[st]:
                        indep_fail_center_el[st] += 1
                        by_biome[b][f"indep_fail_{st}"] += 1

                if len(failed_s) == 1:
                    sole_fail_sample_center_el[failed_s[0]] += 1
                    by_biome[b][f"sole_s_{failed_s[0]}"] += 1
                if len(failed_d) == 1:
                    sole_fail_with_dense_center_el[failed_d[0]] += 1
                    by_biome[b][f"sole_d_{failed_d[0]}"] += 1

            # Also track N=2 outcome for this candidate keyed by its N=0 center biome
            if bval(r["n2_pass_19_sample" if use19 else "n2_pass_8_sample"]):
                by_biome[b]["n2_pass_sample"] += 1
            if bval(r["n2_pass_19_dense" if use19 else "n2_pass_8_dense"]):
                by_biome[b]["n2_pass_dense"] += 1

        return {
            "first_sample": dict(first_sample),
            "first_with_dense": dict(first_with_dense),
            "indep_fail_all": dict(indep_fail_all),
            "indep_fail_center_el": dict(indep_fail_center_el),
            "sole_fail_sample_center_el": dict(sole_fail_sample_center_el),
            "sole_fail_with_dense_center_el": dict(sole_fail_with_dense_center_el),
            "fail_count_dist_center_el": {str(k): v for k, v in sorted(fail_count_dist_center_el.items())},
            "by_biome": {k: dict(v) for k, v in sorted(by_biome.items())},
        }

    summary["n0_pareto_19"] = analyze_n0(True)
    summary["n0_pareto_8"] = analyze_n0(False)

    # N=2 candidate-level Pareto (deepest stage reached across 25 sites)
    STAGE_ORDER = [
        "REJECT_CENTER_BIOME", "REJECT_FOOTPRINT_BIOME", "REJECT_WATER",
        "REJECT_HEIGHT_DIFFERENCE", "REJECT_CUT_SKIRT", "REJECT_FOOTING_FILL",
        "REJECT_DENSE_CUT", "REJECT_DENSE_SKIRT", "REJECT_DENSE_FILL", "ELIGIBLE"
    ]
    def analyze_n2_pareto(use19):
        deepest_sample = Counter()
        deepest_dense = Counter()
        deepest_by_n0_biome = defaultdict(lambda: Counter())
        for r in cands:
            rx, rz = int(r["rx"]), int(r["rz"])
            b0 = r["center_biome_lower"]
            sites = by_cand_n2[(rx, rz)]
            best_s_idx = -1
            best_d_idx = -1
            for s in sites:
                _, vs = eval_site(s, use19=use19, check_dense=False)
                _, vd = eval_site(s, use19=use19, check_dense=True)
                best_s_idx = max(best_s_idx, STAGE_ORDER.index(vs))
                best_d_idx = max(best_d_idx, STAGE_ORDER.index(vd))
            ds = STAGE_ORDER[best_s_idx]
            dd = STAGE_ORDER[best_d_idx]
            deepest_sample[ds] += 1
            deepest_dense[dd] += 1
            if bval(r["center_pass_19" if use19 else "center_pass_8"]):
                deepest_by_n0_biome[b0][f"deepest_s_{ds}"] += 1
                deepest_by_n0_biome[b0][f"deepest_d_{dd}"] += 1
        return {
            "deepest_sample": dict(deepest_sample),
            "deepest_dense": dict(deepest_dense),
            "deepest_by_n0_biome_center_el": {k: dict(v) for k, v in sorted(deepest_by_n0_biome.items())},
        }

    summary["n2_pareto_19"] = analyze_n2_pareto(True)
    summary["n2_pareto_8"] = analyze_n2_pareto(False)

    # 3. Single-relaxation experiments (at N=0 and N=2, and N=0..4 for search radius)
    RELAX_CONFIGS = [
        ("baseline",       80,  5, 20, 10, 12, 16),
        ("water_10",       80, 10, 20, 10, 12, 16),
        ("water_15",       80, 15, 20, 10, 12, 16),
        ("p_width_24",     80,  5, 24, 10, 12, 16),
        ("p_width_28",     80,  5, 28, 10, 12, 16),
        ("p_width_32",     80,  5, 32, 10, 12, 16),
        ("cut_12",         80,  5, 20, 12, 12, 16),
        ("cut_14",         80,  5, 20, 14, 12, 16),
        ("cut_16",         80,  5, 20, 16, 12, 16),
        ("skirt_14",       80,  5, 20, 10, 14, 16),
        ("skirt_16",       80,  5, 20, 10, 16, 16),
        ("skirt_20",       80,  5, 20, 10, 20, 16),
        ("fill_20",        80,  5, 20, 10, 12, 20),
        ("fill_24",        80,  5, 20, 10, 12, 24),
        ("biome_70",       70,  5, 20, 10, 12, 16),
        ("biome_60",       60,  5, 20, 10, 12, 16),
        ("earthwork_L3",   80,  5, 32, 16, 20, 24),
    ]
    relax_results = {}
    for name, mb, mw, mp, mc, ms, mf in RELAX_CONFIGS:
        n0_s_all = n0_d_all = n0_s_cel = n0_d_cel = 0
        n2_s_all = n2_d_all = n2_s_cel = n2_d_cel = 0
        for r in cands:
            cel = bval(r["center_pass_19"])
            oks0, _ = eval_site(r, True, mb, mw, mp, mc, ms, mf, False)
            okd0, _ = eval_site(r, True, mb, mw, mp, mc, ms, mf, True)
            if oks0:
                n0_s_all += 1
                if cel: n0_s_cel += 1
            if okd0:
                n0_d_all += 1
                if cel: n0_d_cel += 1

            sites = by_cand_n2[(int(r["rx"]), int(r["rz"]))]
            oks2 = any(eval_site(s, True, mb, mw, mp, mc, ms, mf, False)[0] for s in sites)
            okd2 = any(eval_site(s, True, mb, mw, mp, mc, ms, mf, True)[0] for s in sites)
            if oks2:
                n2_s_all += 1
                if cel: n2_s_cel += 1
            if okd2:
                n2_d_all += 1
                if cel: n2_d_cel += 1
        relax_results[name] = {
            "n0_sample_all": n0_s_all, "n0_dense_all": n0_d_all,
            "n0_sample_center_el": n0_s_cel, "n0_dense_center_el": n0_d_cel,
            "n2_sample_all": n2_s_all, "n2_dense_all": n2_d_all,
            "n2_sample_center_el": n2_s_cel, "n2_dense_center_el": n2_d_cel,
        }

    # Search radius N=0..4 (for 19-biome and 8-biome)
    radius_results = {}
    for n in range(5):
        radius_results[f"N{n}"] = {
            "pass_19_sample_all": sum(1 for r in cands if bval(r[f"n{n}_pass_19_sample"])),
            "pass_19_dense_all": sum(1 for r in cands if bval(r[f"n{n}_pass_19_dense"])),
            "pass_19_sample_center_el": sum(1 for r in cands if bval(r["center_pass_19"]) and bval(r[f"n{n}_pass_19_sample"])),
            "pass_19_dense_center_el": sum(1 for r in cands if bval(r["center_pass_19"]) and bval(r[f"n{n}_pass_19_dense"])),
            "pass_8_sample_all": sum(1 for r in cands if bval(r[f"n{n}_pass_8_sample"])),
            "pass_8_dense_all": sum(1 for r in cands if bval(r[f"n{n}_pass_8_dense"])),
            "pass_8_sample_center_el": sum(1 for r in cands if bval(r["center_pass_8"]) and bval(r[f"n{n}_pass_8_sample"])),
            "pass_8_dense_center_el": sum(1 for r in cands if bval(r["center_pass_8"]) and bval(r[f"n{n}_pass_8_dense"])),
        }
    summary["single_relaxations"] = relax_results
    summary["search_radii_n0_to_n4"] = radius_results

    # 4. Footprint size counterfactuals (112, 96, 80, 64 at N=0 and N=2)
    subbox_summary = {}
    for sz in (112, 96, 80, 64):
        rows = by_sub_size[sz]
        first_s = Counter()
        first_d = Counter()
        indep_cel = Counter()
        p_vals_cel = []
        r_vals_cel = []
        n2_counts = Counter()
        for r in rows:
            oks, vs = eval_site(r, True, 80, 5, 20, 10, 12, 16, False)
            okd, vd = eval_site(r, True, 80, 5, 20, 10, 12, 16, True)
            first_s[vs] += 1
            first_d[vd] += 1
            cel19 = bval(r["center_pass_19"])
            cel8 = bval(r["center_pass_8"])
            if bval(r["n2_pass_19_sample"]):
                n2_counts["n2_pass_19_sample_all"] += 1
                if cel19: n2_counts["n2_pass_19_sample_center_el"] += 1
            if bval(r["n2_pass_19_dense"]):
                n2_counts["n2_pass_19_dense_all"] += 1
                if cel19: n2_counts["n2_pass_19_dense_center_el"] += 1
            if bval(r["n2_pass_8_sample"]):
                n2_counts["n2_pass_8_sample_all"] += 1
                if cel8: n2_counts["n2_pass_8_sample_center_el"] += 1
            if bval(r["n2_pass_8_dense"]):
                n2_counts["n2_pass_8_dense_all"] += 1
                if cel8: n2_counts["n2_pass_8_dense_center_el"] += 1
            if cel19:
                p_vals_cel.append(int(r["p_width"]))
                r_vals_cel.append(int(r["r_width"]))
                fl = non_short_circuit_flags(r, True, 80, 5, 20, 10, 12, 16)
                for st in ["footprint", "water", "height", "cut_skirt", "footing_fill", "dense_any", "dense_cut", "dense_skirt", "dense_fill"]:
                    if not fl[st]:
                        indep_cel[st] += 1
        subbox_summary[str(sz)] = {
            "first_sample": dict(first_s),
            "first_with_dense": dict(first_d),
            "indep_fail_center_el": dict(indep_cel),
            "n2_counts": dict(n2_counts),
            "p_width_center_el": quantiles(p_vals_cel),
            "r_width_center_el": quantiles(r_vals_cel),
        }
    summary["footprint_sizes_n0"] = subbox_summary

    # 5. Elevation & slope analysis for failed vs passed center-eligible N=0 candidates
    def height_slope_group(rows):
        p_w = [int(r["p_width"]) for r in rows]
        r_w = [int(r["r_width"]) for r in rows]
        r_minus_p = [int(r["r_width"]) - int(r["p_width"]) for r in rows]
        s_mag = [float(r["slope_mag"]) for r in rows]
        octs = Counter(r["slope_octant"] for r in rows)
        p_hist = Counter(p_w)
        r_hist = Counter(r_w)
        return {
            "p_width": quantiles(p_w),
            "r_width": quantiles(r_w),
            "outlier_tail_r_minus_p": quantiles(r_minus_p),
            "slope_mag_112": quantiles(s_mag),
            "slope_octants": dict(octs),
            "p_hist": {str(k): v for k, v in sorted(p_hist.items())},
            "r_hist": {str(k): v for k, v in sorted(r_hist.items())},
        }

    cel_all = [r for r in cands if bval(r["center_pass_19"])]
    cel_failed_n0 = [r for r in cel_all if not bval(r["n0_pass_19_dense"])]
    cel_passed_n0 = [r for r in cel_all if bval(r["n0_pass_19_dense"])]
    cel_failed_n2 = [r for r in cel_all if not bval(r["n2_pass_19_dense"])]
    cel_flat_failed = [r for r in cel_failed_n0 if r["center_biome_lower"] in FLAT_BIOMES]
    cel_hilly_failed = [r for r in cel_failed_n0 if r["center_biome_lower"] in HILLY_BIOMES]

    by_biome_heights = {}
    for b in sorted(ALL_19):
        b_rows = [r for r in cel_all if r["center_biome_lower"] == b]
        b_fail = [r for r in cel_failed_n0 if r["center_biome_lower"] == b]
        by_biome_heights[b] = {
            "all_center_el_p": quantiles([int(r["p_width"]) for r in b_rows]),
            "all_center_el_r": quantiles([int(r["r_width"]) for r in b_rows]),
            "failed_n0_p": quantiles([int(r["p_width"]) for r in b_fail]),
            "failed_n0_r": quantiles([int(r["r_width"]) for r in b_fail]),
            "failed_n0_slope_mag": quantiles([float(r["slope_mag"]) for r in b_fail]),
        }

    summary["elevation_analysis"] = {
        "center_el_all_n0": height_slope_group(cel_all),
        "center_el_failed_n0": height_slope_group(cel_failed_n0),
        "center_el_passed_n0": height_slope_group(cel_passed_n0),
        "center_el_failed_n2": height_slope_group(cel_failed_n2),
        "flat_biomes_failed_n0": height_slope_group(cel_flat_failed),
        "hilly_biomes_failed_n0": height_slope_group(cel_hilly_failed),
        "by_biome": by_biome_heights,
    }

    # 6. Sample (225) vs Full-Column (12,544) discrepancy analysis
    n0_sample_pass_rows = [r for r in cands if eval_site(r, True, 80, 5, 20, 10, 12, 16, False)[0]]
    n0_discrepancy = Counter()
    ex_cut_vals = []
    ex_skirt_vals = []
    ex_fill_vals = []
    ex_any_vals = []
    delta_cut = Counter()
    delta_skirt = Counter()
    delta_fill = Counter()
    for r in n0_sample_pass_rows:
        ec = int(r["dense_ex_cut_cols"])
        es = int(r["dense_ex_skirt_cols"])
        ef = int(r["dense_ex_fill_cols"])
        ea = int(r["dense_ex_any_cols"])
        d_max = int(r["dense_max"])
        d_min = int(r["dense_min"])
        d_fmin = int(r["dense_floor_min"])
        if ea == 0:
            n0_discrepancy["dense_pass"] += 1
        else:
            n0_discrepancy["dense_fail_any"] += 1
            if ec > 0: n0_discrepancy["exceed_cut"] += 1; ex_cut_vals.append(ec)
            if es > 0: n0_discrepancy["exceed_skirt"] += 1; ex_skirt_vals.append(es)
            if ef > 0: n0_discrepancy["exceed_fill"] += 1; ex_fill_vals.append(ef)
            if ec > 0 and es == 0 and ef == 0: n0_discrepancy["only_cut"] += 1
            if es > 0 and ec == 0 and ef == 0: n0_discrepancy["only_skirt"] += 1
            if ef > 0 and ec == 0 and es == 0: n0_discrepancy["only_fill"] += 1
            if (d_max - 10) <= min(d_min + 12, d_fmin + 16):
                n0_discrepancy["reclamp_foundation_would_pass"] += 1
            else:
                n0_discrepancy["true_dense_span_impossible"] += 1
            ex_any_vals.append(ea)
        delta_cut[max(0, int(r["dense_max_cut"]) - int(r["opt_cut"]))] += 1
        delta_skirt[max(0, int(r["dense_max_skirt"]) - int(r["opt_skirt"]))] += 1
        delta_fill[max(0, int(r["dense_max_fill"]) - int(r["opt_fill"]))] += 1

    # B) At N=2 candidate level and across all 25*2601 N<=2 sites
    n2_site_discrepancy = Counter()
    n2_cand_discrepancy = Counter()
    for r in cands:
        s_pass = bval(r["n2_pass_19_sample"])
        d_pass = bval(r["n2_pass_19_dense"])
        if s_pass and d_pass:
            n2_cand_discrepancy["both_pass"] += 1
        elif s_pass and not d_pass:
            n2_cand_discrepancy["sample_pass_dense_fail"] += 1
        else:
            n2_cand_discrepancy["sample_fail"] += 1

    for s in search_rows:
        if eval_site(s, True, 80, 5, 20, 10, 12, 16, False)[0]:
            n2_site_discrepancy["sample_pass_sites"] += 1
            ec = int(s["dense_ex_cut_cols"])
            es = int(s["dense_ex_skirt_cols"])
            ef = int(s["dense_ex_fill_cols"])
            ea = int(s["dense_ex_any_cols"])
            d_max = int(s["dense_max"])
            d_min = int(s["dense_min"])
            d_fmin = int(s["dense_floor_min"])
            if ea == 0:
                n2_site_discrepancy["dense_pass_sites"] += 1
            else:
                n2_site_discrepancy["dense_fail_sites"] += 1
                if ec > 0: n2_site_discrepancy["exceed_cut_sites"] += 1
                if es > 0: n2_site_discrepancy["exceed_skirt_sites"] += 1
                if ef > 0: n2_site_discrepancy["exceed_fill_sites"] += 1
                if (d_max - 10) <= min(d_min + 12, d_fmin + 16):
                    n2_site_discrepancy["reclamp_foundation_would_pass_sites"] += 1
                else:
                    n2_site_discrepancy["true_dense_span_impossible_sites"] += 1

    summary["sample_vs_dense"] = {
        "n0_sample_pass_count": len(n0_sample_pass_rows),
        "n0_breakdown": dict(n0_discrepancy),
        "n0_exceed_cut_cols_quantiles": quantiles(ex_cut_vals),
        "n0_exceed_skirt_cols_quantiles": quantiles(ex_skirt_vals),
        "n0_exceed_fill_cols_quantiles": quantiles(ex_fill_vals),
        "n0_exceed_any_cols_quantiles": quantiles(ex_any_vals),
        "n0_max_cut_increase_hist": {str(k): v for k, v in sorted(delta_cut.items())},
        "n0_max_skirt_increase_hist": {str(k): v for k, v in sorted(delta_skirt.items())},
        "n0_max_fill_increase_hist": {str(k): v for k, v in sorted(delta_fill.items())},
        "n2_candidate_level": dict(n2_cand_discrepancy),
        "n2_site_level": dict(n2_site_discrepancy),
    }

    # 7. Other variables: Ocean/River adjacency, Triangular distribution bias, Structure overlaps
    adj_stats = Counter()
    for r in cel_all:
        oc_fp = int(r["ocean_cells"]) > 0
        rv_fp = int(r["river_cells"]) > 0
        bc_fp = int(r["beach_cells"]) > 0
        oc_64 = bval(r["adj_ocean_64"])
        rv_64 = bval(r["adj_river_64"])
        p_n0 = bval(r["n0_pass_19_dense"])
        p_n2 = bval(r["n2_pass_19_dense"])
        w_fail = not non_short_circuit_flags(r, True)["water"]
        fp_fail = not non_short_circuit_flags(r, True)["footprint"]

        for label, cond in [
            ("all_center_el", True),
            ("ocean_in_footprint", oc_fp),
            ("river_in_footprint", rv_fp),
            ("beach_in_footprint", bc_fp),
            ("adj_ocean_64", oc_64),
            ("adj_river_64", rv_64),
            ("adj_ocean_or_river_64", oc_64 or rv_64),
            ("inland_no_ocean_river_64", not (oc_64 or rv_64)),
        ]:
            if cond:
                adj_stats[f"{label}_total"] += 1
                if p_n0: adj_stats[f"{label}_pass_n0"] += 1
                if p_n2: adj_stats[f"{label}_pass_n2"] += 1
                if w_fail: adj_stats[f"{label}_water_fail"] += 1
                if fp_fail: adj_stats[f"{label}_footprint_fail"] += 1

    # Triangular offset distribution (offset_x, offset_z in 0..42, center = 21)
    tri_offset_x = Counter(int(r["offset_x"]) for r in cands)
    tri_offset_z = Counter(int(r["offset_z"]) for r in cands)
    tri_cheb_total = Counter(int(r["chebyshev_center"]) for r in cands)
    tri_cheb_cel = Counter(int(r["chebyshev_center"]) for r in cel_all)
    tri_cheb_pass_n0 = Counter(int(r["chebyshev_center"]) for r in cands if bval(r["n0_pass_19_dense"]))
    tri_cheb_pass_n2 = Counter(int(r["chebyshev_center"]) for r in cands if bval(r["n2_pass_19_dense"]))

    summary["other_variables"] = {
        "ocean_river_adjacency": dict(adj_stats),
        "triangular_offset_x_hist": {str(k): v for k, v in sorted(tri_offset_x.items())},
        "triangular_offset_z_hist": {str(k): v for k, v in sorted(tri_offset_z.items())},
        "triangular_chebyshev_total": {str(k): v for k, v in sorted(tri_cheb_total.items())},
        "triangular_chebyshev_center_el": {str(k): v for k, v in sorted(tri_cheb_cel.items())},
        "triangular_chebyshev_pass_n0": {str(k): v for k, v in sorted(tri_cheb_pass_n0.items())},
        "triangular_chebyshev_pass_n2": {str(k): v for k, v in sorted(tri_cheb_pass_n2.items())},
        "valid_starts_overlap_any": summary["valid_starts_overlap_any"],
        "valid_starts_overlap_village": summary["valid_starts_overlap_village"],
        "valid_starts_overlap_any_2d": summary.get("valid_starts_overlap_any_2d", 0),
        "valid_starts_overlap_village_2d": summary.get("valid_starts_overlap_village_2d", 0),
        "valid_overlaps_detail": summary["valid_overlaps_detail"],
    }

    (folder / "result.json").write_text(json.dumps(summary, indent=2))
    print(f"PASS: audit2 summary written for seed {summary['seed']}: valid19={summary['valid_starts_19']}, valid8={summary['valid_starts_8']}")


if __name__ == "__main__":
    main()
