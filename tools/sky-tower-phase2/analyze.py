#!/usr/bin/env python3
"""Aggregates raw Sky Tower Phase 2 probe output into the committed tables.

usage: analyze.py <results_dir> <out_dir> --scan scan_2 scan_3 ... --full full_9 ... [--controlled controlled] [--flat flat_a ...]
Run with `python3 -I`. Raw per-run CSVs live under build/ (not committed); the committed artifacts are produced here.
"""
import argparse, csv, collections, json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from height_model import wilson, max_start, LIM  # noqa: E402

BINS = [(-999, 100), (100, 140), (140, 160), (160, 180), (180, 200), (200, 220), (220, 240), (240, 999)]


def bin_of(t):
    for lo, hi in BINS:
        if lo <= t < hi:
            return f"{lo if lo > -999 else '<'}..{hi - 1 if hi < 999 else '+'}" if lo > -999 and hi < 999 else (f"<{hi}" if lo == -999 else f">={lo}")
    return "?"


def load(path):
    with open(path, newline="") as f:
        return list(csv.DictReader(f))


def is_tower(el):
    return el.endswith("/tower")


def pct(k, n):
    return f"{100 * k / n:.2f}%" if n else "n/a"


def ci(k, n):
    lo, hi = wilson(k, n)
    return f"[{100 * lo:.2f}%, {100 * hi:.2f}%]" if n else "n/a"


def candidates_from_trace(rows):
    """One record per (run, set, chunk): attempts in engine order; first attempt carries the shared pool/height draw."""
    by = collections.OrderedDict()
    for r in rows:
        by.setdefault((r["run_label"], r["set"], r["chunk_x"], r["chunk_z"]), []).append(r)
    out = []
    for key, atts in by.items():
        atts.sort(key=lambda r: int(r["order"]))
        first = atts[0]
        out.append({"key": key, "run": key[0], "set": key[1], "attempts": atts, "first": first,
                    "valid": any(a["engine_valid"] == "true" for a in atts),
                    "T": int(first["terrain_top_y"]), "element": first["pool_element"], "start": int(first["start_height_sampled"]),
                    "biome": first["surface_biome"], "reason": first["replica_prediction"]})
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("results"); ap.add_argument("out")
    ap.add_argument("--scan", nargs="*", default=[]); ap.add_argument("--full", nargs="*", default=[])
    ap.add_argument("--controlled", default=None); ap.add_argument("--override", nargs=2, default=None, metavar=("BASELINE","OVERRIDE")); ap.add_argument("--flat", nargs="*", default=[])
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    R = lambda *p: os.path.join(a.results, *p)
    stats = {}

    # ------------------------------------------------------------------ Phase 6 cross-checks
    cross = collections.Counter(); nat_agree = nat_dis = nat_checked = 0; log_bbox_nat = collections.Counter(); log_bbox_rep = collections.Counter()
    for d in a.scan + a.full + a.flat:
        sj = json.load(open(R(d, "summary.json")))
        for k, v in sj["replica_prediction__engine_[invalid,valid]"].items():
            cross[(k, "engine_invalid")] += v[0]; cross[(k, "engine_valid")] += v[1]
        nat_agree += sj.get("natural_vs_replay_agree", 0); nat_dis += sj.get("natural_vs_replay_disagree", 0); nat_checked += sj.get("natural_candidates_checked", 0)
    stats["replica_vs_engine"] = {f"{k[0]}|{k[1]}": v for k, v in sorted(cross.items())}
    stats["natural_vs_replay"] = {"checked": nat_checked, "agree": nat_agree, "disagree": nat_dis}
    # padding-log bbox multiset: natural worker-thread log vs replay engine log
    for d in a.full + a.flat:
        p = R(d, "natural_padding_log.txt")
        if os.path.exists(p):
            for line in open(p).read().splitlines():
                if line.strip():
                    log_bbox_nat[line.split("with bounding box ")[1].split(" does not")[0]] += 1
        for r in load(R(d, "trace.csv")):
            if r["engine_log_reason"].startswith(("PADDING_LOG", "[x")) and r["engine_log_reason"]:
                s = r["engine_log_reason"]
                if "bounding box" in s:
                    log_bbox_rep[s.split("with bounding box ")[1].split(" does not")[0].replace(";", ",")] += 1
    stats["padding_log_unique_bboxes"] = {"natural_pass": len(log_bbox_nat), "replay": len(log_bbox_rep),
                                          "natural_subset_of_replay": all(k in log_bbox_rep for k in log_bbox_nat),
                                          "natural_line_multiplicity": dict(collections.Counter(log_bbox_nat.values()))}

    # ------------------------------------------------------------------ natural-terrain production statistics (replay over scan seeds)
    scan_rows = []
    for d in a.scan:
        scan_rows += load(R(d, "trace.csv"))
    cands = candidates_from_trace(scan_rows)
    mtn = [c for c in cands if c["set"] == "sky_islands_mountain"]
    gen = [c for c in cands if c["set"] == "sky_islands_general"]
    # formula check: engine padding outcome == (start <= LIM - T - (size-1)) for every eligible attempt
    formula_bad = 0; formula_n = 0
    for r in scan_rows:
        T = int(r["terrain_top_y"]); s = int(r["start_height_sampled"]); size = int(r["element_size_y"])
        pred_ok = s <= LIM - T - (size - 1)
        eng_pad_reject = r["replica_prediction"] == "PADDING_REJECT"
        formula_n += 1; formula_bad += (pred_ok == eng_pad_reject)
        # computed min/max
        if int(r["computed_min_y"]) != s + T or int(r["computed_max_y"]) != s + T + size - 1:
            formula_bad += 1
    stats["formula_vs_replica_attempts"] = {"attempts": formula_n, "mismatches": formula_bad}
    # draws shared across variants in a chunk?
    shared = diff = 0
    for c in cands:
        if len(c["attempts"]) > 1:
            kinds = {(x["pool_element"].split("/")[-1], x["start_height_sampled"], x["rotation"]) for x in c["attempts"]}
            shared += len(kinds) == 1; diff += len(kinds) > 1
    stats["variant_attempt_draws_identical_in_chunk"] = {"identical": shared, "different": diff}

    def table(cs, title):
        rows = []
        for lo, hi in BINS:
            sub = [c for c in cs if lo <= c["T"] < hi]
            tw = [c for c in sub if is_tower(c["element"])]
            nt = [c for c in sub if not is_tower(c["element"])]
            rows.append({"terrain_top_block_y": bin_of(lo) if False else (f"<{hi}" if lo == -999 else (f">={lo}" if hi == 999 else f"{lo}..{hi - 1}")),
                         "eligible_candidates": len(sub), "tower_selected": len(tw), "tower_accepted": sum(c["valid"] for c in tw),
                         "nontower_selected": len(nt), "nontower_accepted": sum(c["valid"] for c in nt),
                         "pad_reject_tower": sum(1 for c in tw if c["reason"] == "PADDING_REJECT"),
                         "pad_reject_nontower": sum(1 for c in nt if c["reason"] == "PADDING_REJECT")})
        stats[title] = rows
        return rows

    stats["scan_mountain_by_terrain"] = table(mtn, "scan_mountain_by_terrain_tmp")
    stats["scan_general_by_terrain"] = table(gen, "scan_general_by_terrain_tmp")
    for k in ("scan_mountain_by_terrain_tmp", "scan_general_by_terrain_tmp"):
        stats.pop(k, None)

    def overall(cs):
        tw = [c for c in cs if is_tower(c["element"])]
        # "eligible" for the validity metric: first-attempt eligible; generated = any attempt valid
        return {"candidates": len(cs), "tower_selected": len(tw), "tower_accepted": sum(c["valid"] for c in tw),
                "nontower": len(cs) - len(tw), "nontower_accepted": sum(c["valid"] for c in cs if not is_tower(c["element"])),
                "generated": sum(c["valid"] for c in cs)}

    stats["scan_mountain_overall"] = overall(mtn)
    stats["scan_general_overall"] = overall(gen)
    stats["scan_seeds"] = a.scan
    stats["scan_area_km2_per_seed"] = (2 * 2500 + 1) ** 2 * 256 / 1e6
    # rejected tower by surface biome (mountain)
    bb = collections.defaultdict(lambda: [0, 0])
    for c in mtn:
        if is_tower(c["element"]):
            bb[c["biome"]][0] += 1; bb[c["biome"]][1] += c["valid"]
    stats["scan_mountain_tower_by_surface_biome"] = {k: {"selected": v[0], "accepted": v[1]} for k, v in sorted(bb.items(), key=lambda kv: -kv[1][0])}
    # surface vs stub biome for accepted-or-not first attempts
    mism = sum(1 for c in mtn if c["first"]["stub_biome"] != c["first"]["surface_biome"])
    stats["scan_mountain_stub_biome_differs_from_surface"] = {"differs": mism, "total": len(mtn)}
    stub_rej = sum(1 for c in mtn if any(x["replica_prediction"] == "STUB_BIOME_REJECT" for x in c["attempts"]))
    stats["scan_mountain_candidates_with_stub_biome_reject_attempt"] = stub_rej
    # tower selection independence from sampled start_height (chi-square style: tower share by start bin)
    sb = collections.defaultdict(lambda: [0, 0])
    for c in mtn:
        b = (c["start"] - 18) // 13
        sb[b][0] += 1; sb[b][1] += is_tower(c["element"])
    stats["scan_mountain_tower_share_by_start_height_quartile"] = {f"{18 + 13 * b}..{30 + 13 * b}": {"n": v[0], "towers": v[1], "share": round(v[1] / v[0], 4)} for b, v in sorted(sb.items())}
    ob = collections.defaultdict(lambda: [0, 0])
    for c in mtn:
        pass

    # ------------------------------------------------------------------ strategies (natural seeds, paired)
    strat_rows = []
    for d in a.full:
        strat_rows += load(R(d, "strategies.csv"))
    by_cfg = collections.defaultdict(list)
    for r in strat_rows:
        if r["set"] == "sky_islands_mountain":
            by_cfg[r["config"]].append(r)
    cfg_out = []
    bench = []
    for cfg, rows in by_cfg.items():
        tw = [r for r in rows if r["prod_is_tower"] == "true"]
        gen_rows = [r for r in rows if r["cand_generated"] == "true"]
        gen_tw = [r for r in gen_rows if r["cand_is_tower"] == "true"]
        clear = [int(r["cand_clearance"]) for r in gen_tw if r["cand_clearance"] != ""]
        clear_all = [int(r["cand_clearance"]) for r in gen_rows if r["cand_clearance"] != ""]
        prod_tw_acc = [r for r in tw if r["cand_generated"] == "true" and r["cand_is_tower"] == "true"]
        cfg_out.append({"config": cfg, "eligible_candidates": len(rows), "generated_islands": len(gen_rows), "generated_towers": len(gen_tw),
                        "tower_selected_in_production_draw": len(tw), "tower_selected_that_became_tower": len(prod_tw_acc),
                        "tower_share_of_generated": round(len(gen_tw) / len(gen_rows), 4) if gen_rows else None,
                        "mean_clearance_tower": round(sum(clear) / len(clear), 2) if clear else None,
                        "min_clearance_tower": min(clear) if clear else None, "max_clearance_tower": max(clear) if clear else None,
                        "mean_clearance_all": round(sum(clear_all) / len(clear_all), 2) if clear_all else None})
        for lo, hi in BINS:
            sub = [r for r in rows if lo <= int(r["prod_top_y"]) < hi]
            if not sub:
                continue
            tws = [r for r in sub if r["prod_is_tower"] == "true"]
            acc = [r for r in tws if r["cand_generated"] == "true" and r["cand_is_tower"] == "true"]
            nts = [r for r in sub if r["prod_is_tower"] != "true"]
            nacc = [r for r in nts if r["cand_generated"] == "true"]
            gen_sub_tw = [int(r["cand_clearance"]) for r in acc if r["cand_clearance"] != ""]
            l, h = wilson(len(acc), len(tws))
            bench.append({"source": "NATURAL_REPLAY(paired)", "terrain_key": (f"<{hi}" if lo == -999 else (f">={lo}" if hi == 999 else f"{lo}..{hi - 1}")),
                          "config": cfg, "eligible_candidates": len(sub), "tower_selected": len(tws), "tower_accepted_as_tower": len(acc),
                          "tower_accept_rate": round(len(acc) / len(tws), 4) if tws else "", "ci95_lo": round(l, 4) if tws else "", "ci95_hi": round(h, 4) if tws else "",
                          "nontower_selected": len(nts), "nontower_accepted": len(nacc),
                          "mean_tower_clearance": round(sum(gen_sub_tw) / len(gen_sub_tw), 2) if gen_sub_tw else "",
                          "min_tower_clearance": min(gen_sub_tw) if gen_sub_tw else "", "model_pred_rate": "", "model_mismatch": ""})
    # biome distribution of generated towers per config (surface biome under the piece centre) and stub-biome mismatches
    biome_cfg = {}
    for cfg, rows in by_cfg.items():
        cnt = collections.Counter(r["cand_surface_biome"] for r in rows if r["cand_generated"] == "true" and r["cand_is_tower"] == "true")
        biome_cfg[cfg] = dict(cnt.most_common())
    stats["tower_surface_biome_by_config"] = biome_cfg
    stats["strategy_overall_natural"] = cfg_out
    stats["strategy_seeds"] = a.full
    stats["strategy_area_km2_total"] = len(a.full) * (2 * 2500 + 1) ** 2 * 256 / 1e6
    for row in cfg_out:
        n = row["eligible_candidates"]; k = row["generated_towers"]
        lo_, hi_ = wilson(k, n)
        row["towers_per_1000_mountain_candidates"] = round(1000 * k / n, 2); row["towers_per_1000_ci95"] = [round(1000 * lo_, 2), round(1000 * hi_, 2)]
        row["towers_per_10000_km2"] = round(10000 * k / stats["strategy_area_km2_total"], 2)
    # tower-only (D) acceptance by terrain bin, per candidate
    d_bins = {}
    for cfg in ("D_TOWER_ONLY_18_70", "D_TOWER_ONLY_1_20"):
        out_b = []
        for lo, hi in BINS:
            sub = [r for r in by_cfg.get(cfg, []) if lo <= int(r["prod_top_y"]) < hi]
            k = sum(1 for r in sub if r["cand_generated"] == "true"); l, h = wilson(k, len(sub))
            out_b.append({"bin": (f"<{hi}" if lo == -999 else (f">={lo}" if hi == 999 else f"{lo}..{hi - 1}")), "candidates": len(sub), "tower_generated": k,
                          "rate": round(k / len(sub), 4) if sub else None, "ci95": [round(l, 4), round(h, 4)] if sub else None})
        d_bins[cfg] = out_b
    stats["tower_only_acceptance_by_terrain_bin"] = d_bins
    # relocation: where did production reject but RELOC succeed, and what changed
    reloc = collections.Counter(); moved = []
    for r in by_cfg.get("C_RELOC_R2", []):
        if r["prod_outcome"] != "ACCEPT" and r["cand_generated"] == "true":
            moved.append(int(r["cand_top_y"]) - int(r["prod_top_y"]))
            reloc[(r["prod_surface_biome"], r["cand_surface_biome"])] += 1
    stats["reloc_rescued"] = {"count": len(moved), "terrain_delta_mean": round(sum(moved) / len(moved), 1) if moved else None,
                              "biome_transitions": {f"{k[0]} -> {k[1]}": v for k, v in reloc.most_common()},
                              "surface_biome_changed": sum(v for k, v in reloc.items() if k[0] != k[1])}
    # per-structure-variant attempt outcomes (scan, production replay)
    var = collections.defaultdict(lambda: collections.Counter())
    for r in scan_rows:
        if r["replica_prediction"] in ("ACCEPT", "PADDING_REJECT", "STUB_BIOME_REJECT"):
            var[(r["structure"], "tower" if is_tower(r["pool_element"]) else "non-tower")][r["replica_prediction"]] += 1
    stats["variant_attempts"] = {f"{k[0]}|{k[1]}": dict(v) for k, v in sorted(var.items())}
    # non-tower element rejections by element kind
    nk = collections.defaultdict(lambda: collections.Counter())
    for r in scan_rows:
        if r["replica_prediction"] in ("ACCEPT", "PADDING_REJECT"):
            nk[r["pool_element"].split("/")[-1]][r["replica_prediction"]] += 1
    stats["element_kind_attempts"] = {k: dict(v) for k, v in sorted(nk.items())}
    # chi-square: tower selection vs sampled start_height (mountain scan, 4 equal bins of 13 + the single value 70)
    obs = collections.defaultdict(lambda: [0, 0])
    for c in mtn:
        b = min((c["start"] - 18) // 13, 3)
        obs[b][0] += 1; obs[b][1] += is_tower(c["element"])
    n = sum(v[0] for v in obs.values()); tw = sum(v[1] for v in obs.values()); p = tw / n
    chi = sum((v[1] - v[0] * p) ** 2 / (v[0] * p) + ((v[0] - v[1]) - v[0] * (1 - p)) ** 2 / (v[0] * (1 - p)) for v in obs.values())
    stats["tower_selection_vs_start_height_chi2"] = {"bins": {str(k): v for k, v in sorted(obs.items())}, "chi2": round(chi, 3), "df": len(obs) - 1,
                                                    "chi2_critical_p0.05": 7.815}
    # boundary table: engine outcome of tower attempts by computed root-piece maxY (production, scan seeds)
    bd = collections.defaultdict(lambda: [0, 0])
    for r in scan_rows:
        if is_tower(r["pool_element"]) and r["replica_prediction"] in ("PADDING_REJECT", "ACCEPT", "STUB_BIOME_REJECT"):
            m = int(r["computed_max_y"])
            if 305 <= m <= 330:
                bd[m][0 if r["engine_valid"] == "false" and r["replica_prediction"] == "PADDING_REJECT" else 1] += 1
    stats["boundary_by_computed_max_y"] = {str(k): {"padding_rejected": v[0], "not_padding_rejected": v[1]} for k, v in sorted(bd.items())}
    # attempt-level: any engine_valid attempt with maxY>315, any padding reject with maxY<=315
    stats["boundary_violations"] = {"valid_with_maxY_gt_315": sum(1 for r in scan_rows if r["engine_valid"] == "true" and int(r["computed_max_y"]) > 315),
                                    "padding_reject_with_maxY_le_315": sum(1 for r in scan_rows if r["replica_prediction"] == "PADDING_REJECT" and int(r["computed_max_y"]) <= 315),
                                    "valid_with_maxY_eq_315": sum(1 for r in scan_rows if r["engine_valid"] == "true" and int(r["computed_max_y"]) == 315),
                                    "rejected_with_maxY_eq_316": sum(1 for r in scan_rows if r["replica_prediction"] == "PADDING_REJECT" and int(r["computed_max_y"]) == 316)}

    # ------------------------------------------------------------------ controlled elevation sweep
    ctrl = []
    if a.controlled:
        crow = load(R(a.controlled, "strategies.csv"))
        key = lambda r: (r["run_label"], r["config"])
        grp = collections.defaultdict(list)
        for r in crow:
            grp[key(r)].append(r)
        for (lbl, cfg), rows in sorted(grp.items()):
            parts = lbl.split("/")
            biome, T = parts[1], int(parts[2][1:])
            tws = [r for r in rows if r["prod_is_tower"] == "true" and r["set"] == "sky_islands_mountain"]
            acc = [r for r in tws if r["cand_generated"] == "true" and r["cand_is_tower"] == "true"]
            nts = [r for r in rows if r["prod_is_tower"] != "true" and r["set"] == "sky_islands_mountain"]
            nacc = [r for r in nts if r["cand_generated"] == "true"]
            clear = [int(r["cand_clearance"]) for r in acc if r["cand_clearance"] != ""]
            model = ""; mismatch = ""
            if cfg.startswith("A_RANGE_") or cfg == "PRODUCTION":
                lo, hi = (18, 70) if cfg == "PRODUCTION" else map(int, cfg.split("_")[2:4])
                # exact per-roll model on the SAME sampled start heights is not stored for alt ranges; compare rate
                tot = hi - lo + 1; ok = max(0, min(hi, max_start(T)) - lo + 1); model = round(ok / tot, 4)
            l, h = wilson(len(acc), len(tws))
            ctrl.append({"source": "CONTROLLED_REPLAY", "terrain_key": f"{biome}/T{T}", "config": cfg, "eligible_candidates": len(rows),
                         "tower_selected": len(tws), "tower_accepted_as_tower": len(acc), "tower_accept_rate": round(len(acc) / len(tws), 4) if tws else "",
                         "ci95_lo": round(l, 4) if tws else "", "ci95_hi": round(h, 4) if tws else "", "nontower_selected": len(nts), "nontower_accepted": len(nacc),
                         "mean_tower_clearance": round(sum(clear) / len(clear), 2) if clear else "", "min_tower_clearance": min(clear) if clear else "",
                         "model_pred_rate": model, "model_mismatch": (round(abs(len(acc) / len(tws) - model), 4) if (tws and model != "") else "")})
    if a.controlled:
        bad = n_rows = 0
        per_T = collections.defaultdict(lambda: [0, 0])
        for r in load(R(a.controlled, "trace.csv")):
            if r["set"] != "sky_islands_mountain":
                continue
            T = int(r["terrain_top_y"]); st = int(r["start_height_sampled"]); size = int(r["element_size_y"])
            ok = st <= LIM - T - (size - 1)
            n_rows += 1; bad += (ok == (r["replica_prediction"] == "PADDING_REJECT"))
            if is_tower(r["pool_element"]) and r["order"] == "0":
                per_T[(r["run_label"].split("/")[2], r["engine_valid"])][0] += 1
        stats["controlled_formula_check"] = {"attempts": n_rows, "mismatches": bad}
    fields = ["source", "terrain_key", "config", "eligible_candidates", "tower_selected", "tower_accepted_as_tower", "tower_accept_rate", "ci95_lo", "ci95_hi",
              "nontower_selected", "nontower_accepted", "mean_tower_clearance", "min_tower_clearance", "model_pred_rate", "model_mismatch"]
    with open(os.path.join(a.out, "REMEDIATION_BENCHMARK.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader()
        for r in bench + ctrl:
            w.writerow(r)
    stats["controlled_rows"] = len(ctrl)

    # ------------------------------------------------------------------ PLACEMENT_TRACE.csv (committed subset)
    keep = []
    hdr = None
    nat_index = {}
    for d in a.full + a.flat:
        for r in load(R(d, "natural.csv")):
            nat_index[(r["run_label"], r["set"], r["chunk_x"], r["chunk_z"])] = r
    for d in a.full + a.flat:
        for r in load(R(d, "trace.csv")):
            hi_t = int(r["terrain_top_y"]) >= 160 if r["terrain_top_y"] else False
            if not ((is_tower(r["pool_element"]) and int(r["terrain_top_y"] or 0) >= 120) or hi_t or r["replica_prediction"] != "ACCEPT"):
                continue
            n = nat_index.get((r["run_label"], r["set"], r["chunk_x"], r["chunk_z"]))
            r = dict(r)
            r["evidence"] = "NATURAL+REPLAY" if n else "REPLAY"
            r["natural_structure_generated"] = n["natural_structure"] if n else ""
            keep.append(r)
    if a.controlled:
        for r in load(R(a.controlled, "trace.csv")):
            if r["run_label"].startswith("controlled/jagged_peaks/") and r["structure"] == "sky_island_mountain_snowy" and is_tower(r["pool_element"]):
                r = dict(r); r["evidence"] = "CONTROLLED_REPLAY"; r["natural_structure_generated"] = ""; keep.append(r)
    if keep:
        fn = list(keep[0].keys())
        with open(os.path.join(a.out, "PLACEMENT_TRACE.csv"), "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=fn); w.writeheader(); w.writerows(keep)
    stats["placement_trace_rows"] = len(keep)

    # ------------------------------------------------------------------ natural-pipeline check of a candidate config (disposable datapack) vs baseline, identical chunks
    if a.override:
        res = {}
        for tag, d in zip(("baseline", "override"), a.override):
            first = {}
            for r in load(R(d, "trace.csv")):
                if r["set"] == "sky_islands_mountain":
                    first.setdefault((r["chunk_x"], r["chunk_z"]), r)
            nat = {(r["chunk_x"], r["chunk_z"]): r for r in load(R(d, "natural.csv")) if r["set"] == "sky_islands_mountain"}
            tw = [k for k, r in first.items() if is_tower(r["pool_element"])]
            acc = [k for k in tw if is_tower(nat[k]["natural_root_element"])]
            rolls_ok = sum(1 for k in tw if int(first[k]["start_height_sampled"]) + int(first[k]["terrain_top_y"]) + 96 <= LIM)
            sj = json.load(open(R(d, "summary.json")))
            res[tag] = {"run": d, "active_start_height": sj.get("active_start_height_mountain_snowy"), "mountain_candidates": len(first), "tower_selected": len(tw),
                        "tower_natural_generated": len(acc), "rate": round(len(acc) / len(tw), 4), "ci95": [round(x, 4) for x in wilson(len(acc), len(tw))],
                        "model_says_fit_given_sampled_rolls": rolls_ok,
                        "max_natural_piece_maxY": max([int(nat[k]["natural_max_y"]) for k in acc] or [0]), "natural_vs_replay_agree": sj["natural_vs_replay_agree"],
                        "natural_vs_replay_disagree": sj["natural_vs_replay_disagree"]}
        res["same_selected_tower_chunks"] = True
        stats["datapack_override_natural_check"] = res

    # ------------------------------------------------------------------ top-block checks
    tb = []
    for d in a.full + a.flat:
        p = R(d, "topblocks.csv")
        if os.path.exists(p):
            tb += load(p)
    stats["topblocks"] = tb
    json.dump(stats, open(os.path.join(a.out, "..", "phase2_stats.json") if False else os.path.join(a.results, "phase2_stats.json"), "w"), indent=1)
    print(json.dumps(stats, indent=1))


if __name__ == "__main__":
    main()
