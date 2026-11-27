#!/usr/bin/env python3
"""ST-01 P7 verifier (design-only): (1) exhaustive exploration of the reference state machine with invariants, scenario scripts, persistence round trip,
tomb isolation; (2) geometric opening-safety / golem-route analysis on the P6 world for the east and west variants (+ exact mirror comparison).
Usage: python3 -I st01_p7_verify.py <out_dir>   (exit 1 on any FAIL)"""
import sys, os, json
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p7_statemachine as M
import st01_p7_geometry as G

EXPECT = {   # scenario -> (final record, cover applied, golems)
 "N1": ("ENTRANCE_OPEN", True, 1), "F1": ("FALLBACK_OPEN", True, 0), "F2": ("FALLBACK_OPEN", True, 1), "F3": ("FALLBACK_OPEN", True, 0), "F4": ("FALLBACK_OPEN", True, 0),
 "F5": ("FALLBACK_OPEN", True, 1), "F6": ("ENTRANCE_OPEN", True, 1), "E1": ("ENTRANCE_OPEN", True, 1), "E2": ("ENTRANCE_OPEN", True, 1), "E3": ("SEALED", False, 0),
 "E4": ("ENTRANCE_OPEN", True, 1), "E5": ("FALLBACK_OPEN", True, 1), "E6": ("FALLBACK_OPEN", True, 1)}

def persist(s): return json.dumps(dict(boss=s.boss, rec=s.rec, remaining=s.remaining, cover=s.cover))
def restore(js):
    d = json.loads(js); return M.INIT._replace(boss=d["boss"], rec=d["rec"], remaining=d["remaining"], cover=d["cover"], online=False)

def model_checks(bugs=frozenset()):
    out = []
    allv = []; sizes = []
    for dd in (1, 2, 3, 4):
        M.D = dd; M.INIT = M.INIT._replace(remaining=dd)
        seen, viol, edges = M.explore(bugs); allv += [(dd, v[0]) for v in viol]; sizes.append((dd, len(seen), edges))
    M.D = 2; M.INIT = M.INIT._replace(remaining=2)
    ids = sorted({v[1].split(" ")[0] for v in allv})
    out.append(dict(id="SM1", title="Exhaustive exploration (fallback length D = 1..4 abstract units, all event orders, duplicates, restarts, chunk unload/reload, player leave/join, occupancy): invariants I1-I9 hold in every reachable state/transition",
                    verdict="PASS" if not allv else "FAIL", evidence=f"reachable states/edges per D {sizes}; violations {len(allv)} {ids}; invariants: I1 no opening before defeat, I2 monotone (no regress, no restore), I3 idempotent duplicate events, I4 restart keeps persistent data and deadline, I5 <=1 golem, I6 reward condition untouched, I7 liveness (benign events always open it), I8 cover only with an open record, I9 nobody buried"))
    seen, viol, _ = M.explore(bugs)
    res = []
    for k, evs in M.SCENARIOS.items():
        fin = M.run_scenario(evs, bugs)[-1][1]; key = k.split(" ")[0]
        res.append((key, (fin.rec, fin.cover, fin.ngolem) == EXPECT[key], (fin.rec, fin.cover, fin.ngolem)))
    out.append(dict(id="SM2", title="Normal and failure scenario scripts (N1, F1-F6, E1-E6) end in the expected record/cover/golem state", verdict="PASS" if all(r[1] for r in res) else "FAIL", evidence="; ".join(f"{k}={'ok' if ok else 'MISMATCH ' + str(v)}" for k, ok, v in res)))
    bad = 0
    for s in seen:
        n = M.step(s, "RESTART", bugs); r = M.reconcile(restore(persist(s)), bugs)
        if (n.boss, n.rec, n.remaining, n.cover) != (r.boss, r.rec, r.remaining, r.cover): bad += 1
    out.append(dict(id="SM3", title="Persistence sufficiency: the 4 persisted fields (boss-defeated record, entrance state, remaining time, cover applied) serialised to JSON and restored reproduce exactly the post-restart state for every reachable state", verdict="PASS" if bad == 0 else "FAIL", evidence=f"{len(seen)} reachable states, mismatches {bad}"))
    iso = M.multi_tomb_isolation(bugs)
    out.append(dict(id="SM4", title="Tomb isolation: records keyed by tomb anchor; random event sequences on tomb A never open tomb B (4000 runs x 14 events, fixed seed)", verdict="PASS" if iso == 0 else "FAIL", evidence=f"leaks {iso}"))
    pre = [s for s in seen if not s.boss]
    leaks = [s for s in pre if s.rec != "SEALED" or s.cover or s.ngolem]
    out.append(dict(id="SM5", title="Before the guardian is defeated (any history of early chest touches, spoofed/stale defeat events, timers, restarts, chunk churn) the record stays SEALED, the cover stays closed and no golem exists", verdict="PASS" if not leaks else "FAIL", evidence=f"reachable pre-defeat states {len(pre)}, leaking {len(leaks)}"))
    return out

def geo_checks():
    gE = G.analyse("E"); gW = G.analyse("W")
    mir = {}
    mir["ga1"] = [(q["fall"], q["headroom"], q["landing_y"]) for q in sorted(gE.metrics["ga1"], key=lambda q: (q["col"][1], q["col"][0]))] == [(q["fall"], q["headroom"], q["landing_y"]) for q in sorted(gW.metrics["ga1"], key=lambda q: (q["col"][1], -q["col"][0]))]
    mir["zones"] = {k: (v["cells"], v["min_dist_to_cover"]) for k, v in gE.metrics["zones"].items()} == {k: (v["cells"], v["min_dist_to_cover"]) for k, v in gW.metrics["zones"].items()}
    mir["routes"] = gE.metrics["route_len"] == gW.metrics["route_len"]
    mir["offsets"] = gE.metrics["cover_offsets"]["dx"] == tuple(-v for v in gW.metrics["cover_offsets"]["dx"][::-1])
    return gE, gW, mir

def main(outdir):
    os.makedirs(outdir, exist_ok=True)
    mc = model_checks(); gE, gW, mir = geo_checks()
    ch = [dict(side="-", **c) for c in mc] + [dict(side="E", **c) for c in gE.checks] + [dict(side="W", **c) for c in gW.checks]
    ch.append(dict(side="E/W", id="EW1", title="East and west geometric results are exact mirrors (drops, headroom, zone sizes/distances, route lengths, cover offsets)", verdict="PASS" if all(mir.values()) else "FAIL", evidence=str(mir)))
    ch.append(dict(side="-", id="LV1", title="Real-server behaviour: block removal timing, entity spawn/pathfinding, persistence across a real restart, chunk loading, multiplayer races, damage", verdict="UNVERIFIED", evidence="requires an implementation and a running server; not available in this design-only task"))
    seen, viol, edges = M.explore()
    J = dict(checks=ch, metrics=dict(east={k: v for k, v in gE.metrics.items()}, west={k: v for k, v in gW.metrics.items()}, model=dict(states=len(seen), edges=edges)),
             scenarios={k: [(e, s._asdict()) for e, s in M.run_scenario(v)] for k, v in M.SCENARIOS.items()},
             transition_table=sorted({(a.rec, e, M.step(a, e).rec) for a in seen for e in M.EVENTS if M.step(a, e).rec != a.rec}),
             fallback_sizing_illustrative={str(sp): t for sp, t in G.fallback_sizing(gE.metrics["Lmax"]).items()})
    json.dump(J, open(os.path.join(outdir, "p7_verification.json"), "w"), indent=1, default=str)
    for c in ch: print(c["side"], c["id"], c["verdict"])
    return 1 if any(c["verdict"] == "FAIL" for c in ch) else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p7out"))
