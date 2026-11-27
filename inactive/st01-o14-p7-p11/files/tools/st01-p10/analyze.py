#!/usr/bin/env python3
"""ST-01 P10 measurement analysis: reads the CSVs written by the P10 game tests and writes markdown tables + a JSON summary.
Usage: python3 -I analyze.py <data_dir(suite)> <out_dir> [extra_dir]"""
import sys, os, csv, json, math, collections, statistics as st

def q(xs, p):
    xs = sorted(xs)
    if not xs: return None
    k = max(0, math.ceil(p / 100.0 * len(xs)) - 1); return xs[k]

def dist(xs):
    xs = [x for x in xs if x is not None]
    if not xs: return dict(n=0)
    return dict(n=len(xs), min=min(xs), median=st.median(xs), p90=q(xs, 90), p95=q(xs, 95), p99=q(xs, 99), max=max(xs), mean=round(st.mean(xs), 2), sd=round(st.pstdev(xs), 2))

def num(v, cast=float):
    if v in ("", None): return None
    return cast(v)

def load(path):
    if not os.path.exists(path): return []
    return list(csv.DictReader(open(path)))

def fmt(v, nd=2):
    if v is None: return "-"
    if isinstance(v, float): return f"{v:.{nd}f}".rstrip("0").rstrip(".") if nd else str(round(v))
    return str(v)

def table(headers, rows):
    out = ["| " + " | ".join(headers) + " |", "|" + "|".join(["---"] * len(headers)) + "|"]
    for r in rows: out.append("| " + " | ".join(fmt(c) for c in r) + " |")
    return "\n".join(out)

def tdist(r):
    return [fmt(r.get(k)) for k in ("n", "min", "median", "p90", "p95", "p99", "max")]

def main(data, out, extra=None):
    os.makedirs(out, exist_ok=True); S = {}; md = []
    # ---------------------------------------------------------------- route sweeps (base)
    base = load(os.path.join(data, "route_trials_base.csv"))
    byc = collections.defaultdict(list)
    for r in base: byc[(r["side"], r["start"])].append(r)
    rows = []; S["route"] = {}
    for (side, start), rs in sorted(byc.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        arr = [num(r["t_r15"], int) for r in rs if r["outcome"] == "ARRIVED"]
        d = dist(arr); fail = sum(r["outcome"] != "ARRIVED" for r in rs)
        S["route"][f"{side}:{start}"] = dict(trials=len(rs), failures=fail, arrival_r15=d)
        rows.append([side, start, len(rs), len(rs) - fail] + [d.get(k) for k in ("min", "median", "p90", "p95", "max")])
    md.append("### Route sweep, scale 0.5 / speed 0.25 (arrival tick at radius 1.5, ticks after spawn)\n\n" + table(["side", "start", "trials", "arrived", "min", "median", "p90", "p95", "max"], rows))
    # E/W mirror comparison
    rows = []
    for start in sorted({k[1] for k in byc}):
        e = [num(r["t_r15"], int) for r in byc.get(("E", start), []) if r["outcome"] == "ARRIVED"]; w = [num(r["t_r15"], int) for r in byc.get(("W", start), []) if r["outcome"] == "ARRIVED"]
        if e and w:
            ec = [round(num(r["final_dx"]), 2) for r in byc[("E", start)] if r["final_dx"]]; wc = [round(-num(r["final_dx"]), 2) for r in byc[("W", start)] if r["final_dx"]]
            rows.append([start, st.median(e), st.median(w), st.median(w) - st.median(e), max(e) - min(e), max(w) - min(w), st.median(ec), st.median(wc)])
    S["mirror"] = [dict(start=r[0], median_E=r[1], median_W=r[2], delta=r[3]) for r in rows]
    md.append("### East vs West (mirror) comparison, median arrival tick and mirrored final x offset from the stop centre\n\n" + table(["start", "E median", "W median", "W-E", "E spread", "W spread", "E final dx", "W final dx (mirrored)"], rows))
    # ---------------------------------------------------------------- radius table
    rows = []
    for (side, start), rs in sorted(byc.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        if side != "E": continue
        rr = []
        for key in ("t_r05", "t_r10", "t_r123", "t_r15", "t_r20"):
            v = [num(r[key], int) for r in rs if r[key] not in ("", "-1")]
            rr.append(f"{len(v)}/{len(rs)}" + (f" (med {int(st.median(v))})" if v else ""))
        rows.append([start] + rr)
    md.append("### Arrival radius vs detection (EAST, trials that ever enter the radius while grounded; median tick in parentheses)\n\n" + table(["start", "r=0.5", "r=1.0", "r=1.23", "r=1.5", "r=2.0"], rows))
    # ---------------------------------------------------------------- rest / gap
    rows = []
    for (side, start), rs in sorted(byc.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        mg = [num(r["min_gap"]) for r in rs if r["min_gap"]]; rg = [num(r["rest_gap"]) for r in rs if r["rest_gap"]]; dr = [num(r["max_drift"]) for r in rs]
        ov = sum(int(r["overlap_ticks"]) for r in rs); fdx = [num(r["final_dx"]) for r in rs if r["final_dx"]]; fdz = [num(r["final_dz"]) for r in rs if r["final_dz"]]
        fell = sum(1 for r in rs if r["fell_tick"] not in ("", "-1"))
        rows.append([side, start, round(st.median(fdx), 2), round(st.median(fdz), 2), round(min(mg), 3) if mg else None, round(st.median(rg), 2) if rg else None, round(max(dr), 2), ov, fell])
    md.append("### Where the golem rests (relative to the stop-cell centre) and its clearance to the cover (hitbox-edge to cover-edge, blocks)\n\n" + table(["side", "start", "rest dx", "rest dz", "min gap (all ticks after arrival)", "median rest gap", "max drift after r=1.5", "ticks overlapping cover", "fell"], rows))
    # ---------------------------------------------------------------- startup / stalls
    fp = [num(r["first_path"], int) for r in base if r["first_path"] not in ("", "-1")]
    stl = [num(r["max_stall"], int) for r in base]; lost = sum(int(r["lost_path"]) for r in base)
    S["startup"] = dict(first_path=dist(fp), max_stall=dist(stl), lost_path_ticks_total=lost, trials=len(base))
    md.append(f"### Navigation start-up and stalls (all {len(base)} base trials)\n\nFirst non-null path after spawn (ticks): {S['startup']['first_path']}\n\nLongest consecutive still stretch before arrival (ticks): {S['startup']['max_stall']}; ticks with a lost path after the first path: {lost}.")
    # ---------------------------------------------------------------- O-4 zones
    z = load(os.path.join(data, "route_trials_zones.csv")); zc = collections.defaultdict(list)
    for r in z: zc[r["cond"]].append(r)
    rows = []; S["zones"] = []
    for c, rs in sorted(zc.items(), key=lambda kv: (kv[0].split(":")[1][:2], float(kv[0].split("@")[1].split(":")[0]), kv[0])):
        okr = [r for r in rs if r["outcome"] == "ARRIVED"]
        zone, dd = c.split(":")[1].split("@")[0], c.split("@")[1].split(":")[0]
        t = [num(r["t_r15"], int) for r in okr]; mg = [num(r["min_gap"]) for r in okr if r["min_gap"]]
        rowd = [zone, dd, c.split(":")[2].replace(";", ","), f"{len(okr)}/{len(rs)}", st.median(t) if t else None, round(min(mg), 3) if mg else None, round(st.median([num(r['rest_gap']) for r in okr if r['rest_gap']]), 2) if okr and any(r['rest_gap'] for r in okr) else None, round(st.median([math.hypot(num(r['final_dx']), num(r['final_dz'])) for r in okr]), 2) if okr else None, sum(int(r["overlap_ticks"]) for r in okr)]
        rows.append(rowd); S["zones"].append(dict(zone=zone, dist=dd, delta=c.split(":")[2].replace(";", ","), ok=len(okr), n=len(rs), median_arrival=rowd[4], min_gap=rowd[5], rest_gap=rowd[6], rest_offset=rowd[7], overlap=rowd[8]))
    md.append("### O-4 stop-cell candidates (start BOSS_S; zone = P7 cell-centre distance to cover rectangle; delta = offset from the PROPOSED Z2 stop cell)\n\n" + table(["zone", "cell dist", "delta (x,z)", "arrived", "median tick (r=1.5)", "min hitbox gap", "median rest gap", "median rest offset from own cell centre", "overlap ticks"], rows))
    # ---------------------------------------------------------------- O-6
    o6 = load(os.path.join(data, "route_trials_o6.csv")); oc = collections.defaultdict(list)
    for r in o6: oc[(r["cond"].split(":")[1], r["cond"].split(":")[0])].append(r)
    rows = []; S["o6"] = []
    base_ref = {k[1]: v for k, v in byc.items() if k[0] == "E"}
    for (start, c), rs in sorted(oc.items(), key=lambda kv: (kv[0][0], kv[0][1])):
        okr = [r for r in rs if r["outcome"] == "ARRIVED"]; t = [num(r["t_r15"], int) for r in okr]
        mg = [num(r["min_gap"]) for r in okr if r["min_gap"]]
        rows.append([start, c, f"{len(okr)}/{len(rs)}", st.median(t) if t else None, max(t) if t else None, round(min(mg), 3) if mg else None, sum(int(r["overlap_ticks"]) for r in okr), sum(int(r["lost_path"]) for r in rs), max(int(r["max_stall"]) for r in rs)])
        S["o6"].append(dict(start=start, cond=c, ok=len(okr), n=len(rs), median=rows[-1][3], max=rows[-1][4], min_gap=rows[-1][5], overlap=rows[-1][6]))
    md.append("### O-6 one-variable-at-a-time sweep (EAST; baseline scale 0.5 / speed 0.25 is the `base:` row of the route table)\n\n" + table(["start", "variant", "arrived", "median tick", "max tick", "min hitbox gap", "overlap ticks", "lost-path ticks", "max stall"], rows))
    # ---------------------------------------------------------------- load comparison (suite dir + sibling ../load dir)
    import glob
    ld = []
    for d_ in (data, os.path.join(data, "..", "load")):
        for f_ in sorted(glob.glob(os.path.join(d_, "route_trials_load*.csv"))): ld += load(f_)
    if ld:
        rows = []
        for lab in sorted({r["label"] for r in ld}, key=lambda x: int(x.replace("load", "").replace("ms", ""))):
            for cond in sorted({r["cond"] for r in ld if r["label"] == lab}):
                rs = [r for r in ld if r["label"] == lab and r["cond"] == cond]; t = [num(r["t_r15"], int) for r in rs if r["outcome"] == "ARRIVED"]
                ref = [num(r["t_r15"], int) for r in base_ref.get("BOSS_S" if "BOSS_S" in cond else "DOOR_IN", []) if r["outcome"] == "ARRIVED"]
                rows.append([lab, cond, f"{len(t)}/{len(rs)}", st.median(t) if t else None, max(t) if t else None, st.median(ref) if ref else None, max(ref) if ref else None, round(st.mean(num(r['avg_tick_ms']) for r in rs), 1), sum(int(r['lost_path']) for r in rs), max(int(r['max_stall']) for r in rs)])
        S["load"] = [dict(zip(["label", "cond", "ok", "median", "max", "ref_median", "ref_max", "avg_tick_ms", "lost_path", "max_stall"], r)) for r in rows]
        md.append("### Server tick stretched by a busy loop (label = burn per tick) vs the unloaded reference\n\n" + table(["load", "condition", "arrived", "median tick", "max tick", "unloaded median", "unloaded max", "avg server tick ms", "lost-path ticks", "max stall"], rows))
    # ---------------------------------------------------------------- flows
    fl = load(os.path.join(data, "flow_runs.csv")); fc = collections.defaultdict(list)
    for r in fl: fc[(r["scenario"], r["side"])].append(r)
    rows = []; S["flows"] = {}
    for (sc, side), rs in sorted(fc.items()):
        states = collections.Counter(r["final_state"] for r in rs); ot = [num(r["open_tick"], int) for r in rs if r["open_tick"] not in ("", "-1")]
        und = sum(1 for r in rs if r["arrived_undetected_tick"] not in ("", "-1")); mm = collections.Counter(r["voxel_mismatch_after_open"] for r in rs)
        d = dist(ot); S["flows"][f"{sc}:{side}"] = dict(states=dict(states), open_tick=d, arrived_undetected=und, voxel_mismatch=dict(mm))
        rows.append([sc, side, len(rs), dict(states), d.get("min"), d.get("median"), d.get("p90"), d.get("p95"), d.get("max"), und, dict(mm)])
    md.append("### Full pipeline in the real hall (victory -> golem -> open): final states and open tick (ticks after the valid victory)\n\n" + table(["scenario", "side", "runs", "final states", "min", "median", "p90", "p95", "max", "arrived-but-undetected", "voxel mismatch after open"], rows))
    race = [r for r in fl if r["scenario"] == "race"]
    if race:
        rows = [[(r["params"].split("attribute ")[1].split(" ")[0] if "attribute " in r["params"] else r["params"]), r["final_state"], r["open_tick"], r["golem_dist_to_stop"], r["golem_gap_to_cover"], r["voxel_mismatch_after_open"]] for r in race]
        md.append("### Fallback-vs-arrival race (fallback 600 ticks TEST-ONLY; golem speed set so the walk takes ~500-700 ticks)\n\n" + table(["golem speed attr", "final state", "open tick", "golem dist to stop", "golem gap to cover", "voxel mismatch"], rows))
    # ---------------------------------------------------------------- cover safety
    cs = load(os.path.join(data, "cover_safety.csv"))
    if cs:
        rows = [[r["side"], r["occupant"], r["dx_from_anchor"], r["dz_from_anchor"], r["drop"], r["stayed_on_cell"], r["in_wall"], r["hp_before"], r["hp_after"], r["alive"]] for r in cs]
        md.append("### Cover opening with occupants (proxies; drop in blocks)\n\n" + table(["side", "occupant", "dx", "dz", "drop", "stayed on cell", "in wall", "hp before", "hp after", "alive"], rows))
    hw = load(os.path.join(data, "hatch_walk.csv"))
    if hw:
        md.append("### After opening: golem walk from the stop point (reached tick, -1 = not within 1500 ticks)\n\n" + table(["side", "golem -> goal", "reached tick"], [[r["side"], r["golem_goal"], r["reached_tick"]] for r in hw]))
    if extra:
        door = load(os.path.join(extra, "route_trials_door.csv")); g = collections.defaultdict(list)
        for r in door: g[r["cond"]].append(r)
        rows = []
        for c, rs in g.items():
            ok = [r for r in rs if r["outcome"] == "ARRIVED"]; rows.append([c.split(":")[0].replace("door-scale", ""), f"{len(ok)}/{len(rs)}", st.median([num(r["t_r15"], int) for r in ok]) if ok else None, round(2.7 * float(c.split(":")[0].replace("door-scale", "")), 2), round(1.4 * float(c.split(":")[0].replace("door-scale", "")), 2)])
        S["door"] = [dict(scale=r[0], ok=r[1], median=r[2]) for r in rows]
        md.append("### O-6: passing the real 3x3 door from outside (start CORRIDOR_OUT, speed 0.25)\n\n" + table(["scale", "arrived at the stop point", "median tick", "hitbox height", "hitbox width"], rows))
        op = load(os.path.join(extra, "route_trials_open.csv")); g = collections.defaultdict(list)
        for r in op: g[(r["side"], r["cond"])].append(r)
        rows = []
        for (sd, c), rs in sorted(g.items()):
            ok = [r for r in rs if r["outcome"] == "ARRIVED"]; mg = [num(r["min_gap"]) for r in rs if r["min_gap"]]
            rows.append([sd, c.replace("open-", ""), f"{len(ok)}/{len(rs)}", sum(1 for r in rs if r["fell_tick"] not in ("", "-1")), round(min(mg), 3) if mg else None, sum(int(r["overlap_ticks"]) for r in rs), max(abs(num(r["final_dy"])) for r in rs)])
        S["open"] = [dict(side=r[0], cond=r[1], ok=r[2], fell=r[3], min_gap=r[4]) for r in rows]
        md.append("### Golem arriving beside the cover with the cover REMOVED at the first arrival (golems keep moving for 80 ticks)\n\n" + table(["side", "condition", "arrived", "fell into the opening", "min hitbox gap", "overlap ticks", "max |final dy|"], rows))
        zd = load(os.path.join(extra, "route_trials_zones_door.csv")); zs = load(os.path.join(data, "route_trials_zones.csv"))
        def cells(rows_):
            g_ = collections.defaultdict(list)
            for r in rows_: g_[r["cond"]].append(r)
            out_ = {}
            for c, rs in g_.items():
                ok = [r for r in rs if r["outcome"] == "ARRIVED"]
                out_[c] = (len(ok), len(rs), min([num(r["min_gap"]) for r in ok if r["min_gap"]] or [99]), sum(int(r["overlap_ticks"]) for r in ok), st.median([num(r["t_r15"], int) for r in ok]) if ok else None)
            return out_
        a, b = cells(zs), cells(zd); rows = []; safe = []
        for c in sorted(a, key=lambda k: (k.split(":")[1].split("@")[0], float(k.split("@")[1].split(":")[0]), k)):
            if c not in b: continue
            zone, dd, dl = c.split(":")[1].split("@")[0], c.split("@")[1].split(":")[0], c.split(":")[2].replace(";", ",")
            both = min(a[c][2], b[c][2]); ov = a[c][3] + b[c][3]
            rows.append([zone, dd, dl, f"{a[c][0]}/{a[c][1]}", round(a[c][2], 2), f"{b[c][0]}/{b[c][1]}", round(b[c][2], 2), ov, a[c][4], b[c][4]])
            if a[c][0] == a[c][1] and b[c][0] == b[c][1] and ov == 0 and both >= 1.0: safe.append((zone, dd, dl, both))
        S["zones_both"] = dict(safe_cells=safe)
        md.append("### O-4 stop-cell candidates approached from BOTH directions (west approach = start BOSS_S, n=6; east-aisle approach = start DOOR_IN, n=4)\n\n" + table(["zone", "cell dist", "delta (x,z)", "BOSS_S arrived", "BOSS_S min gap", "DOOR_IN arrived", "DOOR_IN min gap", "overlap ticks (both)", "BOSS_S median tick", "DOOR_IN median tick"], rows) + "\n\nCells that arrived in every trial from both starts, never overlapped the cover and kept a hitbox gap >= 1.0 block: " + ("; ".join(f"{z} d={d} delta=({dl}) gap>={g_:.2f}" for z, d, dl, g_ in safe) or "none"))
    open(os.path.join(out, "p10_measurement_tables.md"), "w").write("\n\n".join(md) + "\n")
    json.dump(S, open(os.path.join(out, "p10_measurement_summary.json"), "w"), indent=1, default=str)
    print("\n\n".join(md))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
