#!/usr/bin/env python3
"""Aggregates raw SkyTowerAdaptiveProbe output (build/sky-tower-adaptive/results). Run with `python3 -I`.
usage: analyze.py <results_dir> <out_dir> --nat nat_2 ... --controlled controlled --flat flat_a ... --det det_a det_b
"""
import argparse, collections, csv, json, math, os, statistics as st


def load(p):
    with open(p, newline="") as f:
        return list(csv.DictReader(f))


def wilson(k, n, z=1.96):
    if n == 0:
        return (float("nan"),) * 2
    p = k / n; d = 1 + z * z / n; c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (max(0, c - h), min(1, c + h))


def tw(r):
    return r["element"].endswith("/tower")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("results"); ap.add_argument("out")
    ap.add_argument("--nat", nargs="*", default=[]); ap.add_argument("--controlled"); ap.add_argument("--flat", nargs="*", default=[]); ap.add_argument("--det", nargs=2)
    a = ap.parse_args(); R = lambda *p: os.path.join(a.results, *p); os.makedirs(a.out, exist_ok=True)
    S = {}

    # ---------------- boundary (controlled)
    rows = load(R(a.controlled, "rows.csv"))
    cell = collections.defaultdict(lambda: collections.defaultdict(list))
    for r in rows:
        if r["set"] != "sky_islands_mountain":
            continue
        cell[r["label"]][r["config"]].append(r)
    out = []
    viol = 0
    for label, d in sorted(cell.items(), key=lambda kv: (kv[0].split("/")[1], int(kv[0].split("/T")[1]))):
        biome, T = label.split("/")[1], int(label.split("/T")[1])
        pr, lg = d["PRODUCTION"], d["LEGACY"]
        ptw = [r for r in pr if tw(r)]; ltw = [r for r in lg if tw(r)]
        pacc = [r for r in ptw if r["outcome"] == "GENERATED" and r["gen_element"].endswith("/tower")]
        lacc = [r for r in ltw if r["outcome"] == "GENERATED" and r["gen_element"].endswith("/tower")]
        offs = [int(r["gen_offset"]) for r in pacc]
        hall = 219 - T
        expect_feasible = hall >= 18
        bad = (not expect_feasible and len(pacc) > 0) or (expect_feasible and len(pacc) != len(ptw)) or any(o < 18 or o > min(70, hall) for o in offs) or any(int(r["gen_max_y"]) > 315 for r in pacc)
        viol += bad
        nt = [r for r in pr if not tw(r)]; ntl = [r for r in lg if not tw(r)]
        out.append({"variant_biome": biome, "terrain_top_T": T, "h_allowed_max_model": hall, "min_offset_18_feasible_model": expect_feasible,
                    "tower_candidates": len(ptw), "adaptive_tower_accepted": len(pacc), "legacy_tower_accepted": len(lacc),
                    "adaptive_accept_rate": round(len(pacc) / len(ptw), 4) if ptw else "", "legacy_accept_rate": round(len(lacc) / len(ltw), 4) if ltw else "",
                    "adaptive_min_offset": min(offs) if offs else "", "adaptive_max_offset": max(offs) if offs else "", "adaptive_mean_offset": round(st.mean(offs), 2) if offs else "",
                    "adaptive_max_piece_maxY": max([int(r["gen_max_y"]) for r in pacc], default=""),
                    "nontower_candidates": len(nt), "adaptive_nontower_accepted": sum(r["outcome"] == "GENERATED" for r in nt), "legacy_nontower_accepted": sum(r["outcome"] == "GENERATED" for r in ntl),
                    "policy_violation": bad})
    with open(os.path.join(a.out, "HEIGHT_BOUNDARY_RESULTS.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(out[0].keys())); w.writeheader(); w.writerows(out)
    S["boundary_rows"] = len(out); S["boundary_policy_violations"] = viol
    # per-row exactness of the equation
    mism = n = 0
    for r in rows:
        if r["config"] == "PRODUCTION" and r["set"] == "sky_islands_mountain" and tw(r) and r["outcome"] in ("GENERATED", "REJECTED"):
            n += 1
            feas = int(r["h_allowed_max"]) >= 18
            mism += (feas != (r["outcome"] == "GENERATED"))
    S["controlled_tower_rows_feasibility_vs_outcome"] = {"rows": n, "mismatches": mism}
    S["controlled_nontower_reject_reason_counts"] = dict(collections.Counter((r["reason"]) for r in rows if r["config"] == "PRODUCTION" and r["outcome"] == "REJECTED"))

    # ---------------- natural pooled (mountain set)
    nr = []
    for d in a.nat:
        nr += load(R(d, "rows.csv"))
    area = len(a.nat) * (2 * 2500 + 1) ** 2 * 256 / 1e6
    S["natural_seeds"] = a.nat; S["natural_area_km2"] = area
    m = collections.defaultdict(list)
    for r in nr:
        if r["set"] == "sky_islands_mountain":
            m[r["config"]].append(r)
    cmp = {}
    for cfg, rs in m.items():
        t = [r for r in rs if tw(r)]
        acc_t = [r for r in t if r["outcome"] == "GENERATED" and r["gen_element"].endswith("/tower")]
        gen = [r for r in rs if r["outcome"] == "GENERATED"]
        offs = [int(r["gen_offset"]) for r in gen]
        toffs = [int(r["gen_offset"]) for r in acc_t]
        nt = [r for r in rs if not tw(r)]
        cmp[cfg] = {"eligible": len(rs), "tower_selected": len(t), "tower_selected_share": round(len(t) / len(rs), 4), "tower_accepted": len(acc_t),
                    "tower_accept_rate": round(len(acc_t) / len(t), 4), "tower_accept_ci95": [round(x, 4) for x in wilson(len(acc_t), len(t))],
                    "nontower_selected": len(nt), "nontower_accepted": sum(r["outcome"] == "GENERATED" for r in nt),
                    "islands_generated": len(gen), "tower_share_of_generated": round(len(acc_t) / len(gen), 4),
                    "towers_per_10000_km2": round(10000 * len(acc_t) / area, 1),
                    "offset_mean_all": round(st.mean(offs), 2), "offset_median_all": st.median(offs),
                    "offset_mean_tower": round(st.mean(toffs), 2), "offset_median_tower": st.median(toffs),
                    "reject_reasons": dict(collections.Counter(r["reason"] for r in rs if r["outcome"] == "REJECTED"))}
    S["natural_mountain_compare"] = cmp
    # per terrain bin
    bins = [(-999, 100), (100, 140), (140, 160), (160, 180), (180, 200), (200, 202), (202, 220), (220, 999)]
    pb = []
    for lo, hi in bins:
        for cfg in ("PRODUCTION", "LEGACY"):
            sub = [r for r in m[cfg] if lo <= int(r["terrain_top_T_at_center"]) < hi and tw(r)]
            acc = [r for r in sub if r["outcome"] == "GENERATED"]
            pb.append({"terrain_bin": f"{lo}..{hi - 1}", "config": cfg, "tower_selected": len(sub), "tower_accepted": len(acc),
                       "rate": round(len(acc) / len(sub), 4) if sub else None, "ci95": [round(x, 3) for x in wilson(len(acc), len(sub))] if sub else None})
    S["natural_tower_by_terrain_bin"] = pb
    # identity: wherever draw1 <= feasible, production must equal legacy (same candidate)
    key = lambda r: (r["label"], r["set"], r["chunk_x"], r["chunk_z"])
    P = {key(r): r for r in nr if r["config"] == "PRODUCTION" and r["set"] == "sky_islands_mountain"}
    L = {key(r): r for r in nr if r["config"] == "LEGACY" and r["set"] == "sky_islands_mountain"}
    same = diff_fit = diff_unfit = 0
    for k, p in P.items():
        l = L[k]; fit = int(p["draw1_roll"]) <= int(p["h_effective_max"])
        eq = (p["outcome"], p["gen_structure"], p["gen_min_y"], p["gen_max_y"], p["rotation_of_piece"]) == (l["outcome"], l["gen_structure"], l["gen_min_y"], l["gen_max_y"], l["rotation_of_piece"])
        if eq: same += 1
        elif fit: diff_fit += 1
        else: diff_unfit += 1
    S["paired_identity"] = {"identical": same, "different_where_draw1_fits": diff_fit, "different_where_draw1_exceeds_feasible_max": diff_unfit}
    # general set unaffected
    g = [(r["config"], r["outcome"], r["gen_min_y"], r["gen_max_y"], r["gen_structure"]) for r in nr if r["set"] == "sky_islands_general"]
    gp = [x[1:] for x in g if x[0] == "PRODUCTION"]; gl = [x[1:] for x in g if x[0] == "LEGACY"]
    S["general_set_identical"] = {"rows": len(gp), "identical": gp == gl}
    # boundary pileup: constrained towers (hEff < 70): offset distribution in [18,hEff]
    pile = {"constrained_towers": 0, "at_upper_boundary": 0, "expected_uniform_at_upper": 0.0, "chi2": 0.0}
    byh = collections.defaultdict(list)
    for r in m["PRODUCTION"]:
        if tw(r) and r["outcome"] == "GENERATED" and 18 <= int(r["h_effective_max"]) < 70 and int(r["draw1_roll"]) > int(r["h_effective_max"]):
            byh[int(r["h_effective_max"])].append(int(r["gen_offset"]))
    resampled = sum(len(v) for v in byh.values())
    up = sum(1 for h, v in byh.items() for o in v if o == h); exp = sum(len(v) / (h - 17) for h, v in byh.items())
    S["resampled_tower_offsets"] = {"resampled_towers": resampled, "at_upper_boundary": up, "expected_if_uniform": round(exp, 1), "clamp_would_give": resampled}
    allc = collections.Counter(); N = 0
    for r in m["PRODUCTION"]:
        if tw(r) and r["outcome"] == "GENERATED" and 18 <= int(r["h_effective_max"]) < 70:
            allc[(int(r["h_effective_max"]), int(r["gen_offset"]))] += 1
    # offset histogram (all production generated mountain islands vs legacy)
    for cfg in ("PRODUCTION", "LEGACY"):
        c = collections.Counter(int(r["gen_offset"]) // 10 * 10 for r in m[cfg] if r["outcome"] == "GENERATED")
        S[f"offset_histogram_{cfg}"] = dict(sorted(c.items()))
    # rotations
    S["rotations_generated_towers_production"] = dict(collections.Counter(r["rotation_of_piece"] for r in m["PRODUCTION"] if r["outcome"] == "GENERATED" and r["gen_element"].endswith("/tower")))
    S["tower_styles_production"] = dict(collections.Counter(r["gen_element"] for r in m["PRODUCTION"] if r["outcome"] == "GENERATED" and r["gen_element"].endswith("/tower")))
    # natural sample records (high terrain towers)
    hi = [r for r in m["PRODUCTION"] if tw(r) and int(r["terrain_top_T_at_center"]) >= 160]
    with open(os.path.join(a.out, "NATURAL_TOWER_SAMPLES_T160plus.csv"), "w", newline="") as f:
        cols = ["label", "seed", "set", "chunk_x", "chunk_z", "first_eligible_structure", "element", "rotation", "terrain_top_T_at_center", "draw1_roll", "h_effective_max", "outcome", "reason",
                "gen_structure", "gen_min_y", "gen_max_y", "gen_offset", "rotation_of_piece"]
        w = csv.writer(f); w.writerow(cols)
        for r in hi:
            w.writerow([r[c] for c in cols])
    S["natural_tower_samples_T160plus"] = len(hi)
    # summaries
    nat_sum = {d: json.load(open(R(d, "summary.json"))) for d in a.nat + a.flat}
    S["natural_pipeline_check"] = {d: {k: v.get(k) for k in ("natural_checked", "natural_vs_replay_agree", "natural_vs_replay_disagree", "evaluations_prod_legacy", "total_time_ms_prod_legacy", "allocated_bytes_prod_legacy")} for d, v in nat_sum.items()}
    # footprint
    fr = []
    for d in a.nat:
        p = R(d, "footprint.csv")
        if os.path.exists(p):
            fr += load(p)
    fr = [r for r in fr if r["columns"] not in ("0", "")]
    def num(x): return int(x)
    S["footprint"] = {"samples": len(fr), "towers": sum(r["element"].endswith("/tower") for r in fr),
                      "bbox_min_y_below_footprint_max_terrain": sum(num(r["bbox_min_y_minus_footprint_max_terrain"]) < 0 for r in fr),
                      "block_intersections_samples_with_any": sum(num(r["block_intersections"]) > 0 for r in fr),
                      "min_block_separation_min": min(num(r["block_min_separation"]) for r in fr), "ref_offset_min": min(num(r["ref_offset"]) for r in fr),
                      "tower_samples_min_block_separation_min": min([num(r["block_min_separation"]) for r in fr if r["element"].endswith("/tower")], default=None),
                      "terrain_span_median": st.median(num(r["footprint_max_terrain"]) - num(r["footprint_min_terrain"]) for r in fr),
                      "terrain_span_max": max(num(r["footprint_max_terrain"]) - num(r["footprint_min_terrain"]) for r in fr)}
    ft = [r for r in fr if r["element"].endswith("/tower")]
    S["footprint"]["tower_ref_offset_minus_block_min_sep_median"] = st.median(num(r["ref_offset"]) - num(r["block_min_separation"]) for r in ft) if ft else None
    with open(os.path.join(a.out, "FOOTPRINT_SAMPLES.csv"), "w", newline="") as f:
        if fr:
            w = csv.DictWriter(f, fieldnames=list(fr[0].keys())); w.writeheader(); w.writerows(fr)
    # flat natural boundary worlds
    fl = {}
    for d in a.flat:
        fr_ = load(R(d, "rows.csv")); mm = [r for r in fr_ if r["set"] == "sky_islands_mountain" and r["config"] == "PRODUCTION"]
        t = [r for r in mm if tw(r)]; acc = [r for r in t if r["outcome"] == "GENERATED"]
        offs = [int(r["gen_offset"]) for r in acc]
        fl[d] = {"tower_candidates": len(t), "tower_generated": len(acc), "offset_min": min(offs, default=None), "offset_max": max(offs, default=None), "max_piece_maxY": max([int(r["gen_max_y"]) for r in acc], default=None),
                 "T": t[0]["terrain_top_T_at_center"] if t else None, "natural_vs_replay": [nat_sum[d]["natural_vs_replay_agree"], nat_sum[d]["natural_vs_replay_disagree"]]}
    S["flat_natural"] = fl
    # determinism
    if a.det:
        def norm(p):
            return [",".join(l.split(",")[1:-1]) for l in open(p).read().splitlines()]  # drop run label and time_us
        A, B = norm(R(a.det[0], "rows.csv")), norm(R(a.det[1], "rows.csv"))
        S["determinism"] = {"rows": len(A), "identical_ignoring_time_column": A == B}
    json.dump(S, open(R("adaptive_stats.json"), "w"), indent=1)
    print(json.dumps(S, indent=1)[:6000])


main()
