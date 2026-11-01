#!/usr/bin/env python3
"""ST-05 Probe 01 G-2 materialization verifier (CANDIDATE artifacts).

AUTHORSHIP / INDEPENDENCE STATEMENT (honest scope):
  * This file imports nothing from materialize.py and never reads its source.
    It re-derives every fixture, schedule, control target, manifest triple,
    invalid-input file and count from the approved documents (execution spec,
    binding plan rev.3, G-1/G-2 preparation record) with its OWN canonical
    writer, OWN lexicographic permutation generator and OWN table-driven
    fixture definitions, then compares the artifacts on disk with that
    independent reconstruction.
  * Both tools were written in the same agent session (no second human/agent
    was available). Independence is therefore structural, not organisational:
    final independent G-2 acceptance REQUIRES a named reviewer (PO-25) who has
    not yet been assigned. Nothing here claims that review happened.

Python 3 standard library only. No network. Does not run any probe.
Usage: python3 verify_materialization.py [--dir DIR]
Exit 0 = all checks passed, 1 = at least one check failed.
"""
import hashlib
import json
import os
import re
import sys
from fractions import Fraction
from math import factorial

HERE = os.path.dirname(os.path.abspath(__file__))
if "--dir" in sys.argv:
    HERE = os.path.abspath(sys.argv[sys.argv.index("--dir") + 1])

ERR = []


def bad(msg):
    ERR.append(msg)


def ok(cond, msg):
    if not cond:
        bad(msg)
    return cond


# ------------------------------------------------------------------ canonical text
def enc(v, lvl=0):
    inner, outer = "  " * (lvl + 1), "  " * lvl
    if v is None:
        return "null"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, str):
        return json.dumps(v)
    if isinstance(v, (list, tuple)):
        if len(v) == 0:
            return "[]"
        return "[\n" + ",\n".join(inner + enc(x, lvl + 1) for x in v) + "\n" + outer + "]"
    if isinstance(v, dict):
        if len(v) == 0:
            return "{}"
        parts = []
        for k in sorted(v.keys(), key=lambda s: s.encode("utf-8")):
            parts.append(inner + json.dumps(k) + ": " + enc(v[k], lvl + 1))
        return "{\n" + ",\n".join(parts) + "\n" + outer + "}"
    raise TypeError("forbidden value type %s" % type(v).__name__)


def canon(v):
    return (enc(v) + "\n").encode("utf-8")


def strict_load(raw):
    def pairs(items):
        d = {}
        for k, v in items:
            if k in d:
                raise ValueError("duplicate key " + k)
            d[k] = v
        return d

    def tok(x):
        raise ValueError("number token " + x)

    return json.loads(raw.decode("utf-8"), object_pairs_hook=pairs, parse_float=tok, parse_int=tok,
                      parse_constant=tok)


def h(b):
    return hashlib.sha256(b).hexdigest()


def perms(n):
    """All permutations of 0..n-1 in lexicographic order (own recursive generator)."""
    if n == 0:
        return [[]]
    res = []

    def rec(prefix, rest):
        if not rest:
            res.append(prefix)
            return
        for i, x in enumerate(rest):
            rec(prefix + [x], rest[:i] + rest[i + 1:])

    rec([], list(range(n)))
    return res


# ------------------------------------------------------------------ expected data
PFX = {"dimension": "minecraft:overworld", "schema": "1", "seed": "7"}


def q(v):
    f = Fraction(v)
    return {"den": str(f.denominator), "num": str(f.numerator)}


def xid(x):
    return {"x": str(x), "y": "0", "z": "0"}


def blank(fid, fam, graph=True):
    return {
        "record_schema": "P01-FIXTURE-1", "fixture_id": fid, "family": fam, "source_refs": ["x"],
        "scalar_inputs": [], "id_prefix": dict(PFX) if graph else None, "vertices": [], "edges": [],
        "protected_sites": [], "unknown_edges": [], "population_complete": True, "stages": [], "events": [],
        "default_schedule": "canonical", "control_overrides": {}, "expect_input_rejection": None,
        "expected_ref": fid, "derived": None}


def cd(name, slot, rank, status, comps):
    return {"cand": name, "slot": str(slot), "seed_rank": str(rank), "status": status,
            "components": None if comps is None else [q(c) for c in comps]}


def vx(label, x, comps, roll="WIN", cands=None, fit=True):
    return {"label": label, "id": xid(x), "qualified": True, "fit_complete": fit, "roll": roll,
            "candidates": cands if cands is not None else [cd("c0", 1, 0, "FIT", comps)]}


def edges_of(verts, pairs):
    pos = {v["label"]: int(v["id"]["x"]) for v in verts}
    es = []
    for a, b in pairs:
        lo, hi = (a, b) if pos[a] < pos[b] else (b, a)
        es.append((pos[lo], pos[hi], lo, hi))
    es.sort()
    return [{"u": lo, "v": hi} for (_, _, lo, hi) in es]


def gr(fid, verts, pairs, fam="GRAPH", prot=None):
    r = blank(fid, fam)
    r["vertices"] = verts
    r["edges"] = edges_of(verts, pairs)
    if prot:
        r["protected_sites"] = prot
    return r


IMPULSE = {0: [0] * 9, 1: [100] * 9, 2: [50, 75, 80, 75, 75, 75, 75, 50, 50]}
for k in range(9):
    IMPULSE[3 + k] = [100 if j == k else 0 for j in range(9)]


def raw(n):
    return {"label": "V%d" % n, "kind": "RAW9", "values": [q(x) for x in IMPULSE[n]]}


def roll_vertices(with_d):
    spec = [("a", 0, [100, 100, 0, 0], 8, "WIN"), ("b", 1, [100, 0, 0, 0], 1, "WIN"), ("c", 2, [50, 0, 0, 0], 3, "LOSS")]
    vs = []
    for (l, x, t, s, ro) in spec:
        cs = [cd("s%d" % k, k, k, "FIT" if k == s else "HARD_REJECT", t if k == s else None) for k in range(1, 9)]
        vs.append(vx(l, x, None, roll=ro, cands=cs))
    if with_d:
        vs.append(vx("d", 3, None, roll="NOT_ROLLED", cands=[cd("s%d" % k, k, k, "HARD_REJECT", None) for k in range(1, 9)]))
    return vs


def exp_main_records():
    R = []
    for n in range(12):
        r = blank("P01/ARITH-V%d" % n, "ARITH", graph=False)
        r["scalar_inputs"] = [raw(n)]
        R.append(r)
    r = blank("P01/FIXED-01", "ARITH", graph=False)
    r["scalar_inputs"] = [raw(0), raw(1), raw(2),
                          {"label": "K4", "kind": "COMP4", "values": [q(Fraction(50001, 1000)), q(50), q(0), q(0)]}]
    R.append(r)
    r = blank("P01/CAND-01", "GRAPH")
    r["vertices"] = [vx("U", 0, None, cands=[cd("c0", 1, 1, "FIT", [100, 0, 0, 0]), cd("c1", 2, 0, "FIT", [0, 100, 0, 0])])]
    R.append(r)
    R.append(gr("P01/CAV-01", [vx("U", -1, [0, 100, 0, 0]), vx("V", 0, [100, 0, 0, 0])], [("U", "V")]))
    ks = {"K1": [100, 0, 0, 0], "K2": [0, 100, 0, 0], "K3": [50, 50, 0, 0]}
    tri = [("K1", "K2"), ("K1", "K3"), ("K2", "K3")]
    R.append(gr("P01/EQUAL-3-C", [vx("K1", 0, ks["K1"]), vx("K2", -2, ks["K2"]), vx("K3", -1, ks["K3"])], tri))
    R.append(gr("P01/EQUAL-3-T", [vx("K1", -2, ks["K1"]), vx("K2", 0, ks["K2"]), vx("K3", -1, ks["K3"])], tri))
    R.append(gr("P01/NEAR-01", [vx("P", 1, [Fraction(50001, 1000), 50, 0, 0]), vx("Q", 0, [50, 50, 0, 0])], [("P", "Q")]))
    eps = Fraction(1, 2 ** 80)
    R.append(gr("P01/BIG-NEAR-01", [vx("P", 1, [50 + eps, 50, 0, 0]), vx("Q", 0, [50, 50, 0, 0])], [("P", "Q")]))
    four = [("a", -2), ("b", -1), ("c", 2), ("d", 10)]
    allpairs = [(four[i][0], four[j][0]) for i in range(4) for j in range(i + 1, 4)]
    R.append(gr("P01/ID-ORDER-01", [vx(l, x, [100, 0, 0, 0]) for l, x in four], allpairs))
    A3, B3, C3 = [100, 0, 0, 0], [100, 50, 0, 0], [100, 100, 0, 0]
    R.append(gr("P01/CHAIN-3", [vx("A", 0, A3), vx("B", 1, B3), vx("C", 2, C3)], [("A", "B"), ("B", "C")]))
    cid = [vx("A", 0, A3), vx("B", -1, B3), vx("C", 1, C3)]
    R.append(gr("P01/CHAIN-ID-01", cid, [("A", "B"), ("B", "C")]))
    R.append(gr("P01/CHAIN-PROT-01", [vx("A", 0, A3), vx("B", -1, B3), vx("C", 1, C3)], [("A", "B"), ("B", "C")],
                prot=[{"label": "P", "id": xid(-2), "status": "RETIRED_PROTECTED", "conflicts_with": ["C"]}]))
    R.append(gr("P01/CYCLE-01", [vx(str(i), i, [Fraction(10 * (i + 1))] * 4) for i in range(4)],
                [("0", "1"), ("1", "2"), ("2", "3"), ("3", "0")]))
    for fid, wd in (("P01/ROLL-01", False), ("P01/NOFIT-01", True)):
        vs = roll_vertices(wd)
        R.append(gr(fid, vs, [("a", "b")]))
    # LATE-01
    r = blank("P01/LATE-01", "LATE")  # top-level population_complete stays True (inert graph, POP-FLAG rule)
    lv = [vx("A", 0, A3), vx("B", -1, B3), vx("C", 1, C3), vx("D", 2, [100] * 4)]
    r["stages"] = [
        {"stage": "OPEN", "certificate": "OPEN", "vertices": lv, "population_complete": False,
         "edges": edges_of(lv, [("A", "B"), ("B", "C")]), "unknown_edges": edges_of(lv, [("C", "D")])},
        {"stage": "REVEAL", "certificate": "COMPLETE", "vertices": lv, "population_complete": True,
         "edges": edges_of(lv, [("A", "B"), ("B", "C"), ("C", "D")]), "unknown_edges": []}]
    R.append(r)
    for kind in ("SCORE", "TIE"):
        for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024):
            for ext in (0, 1):
                m = n + 1 + ext
                if kind == "SCORE":
                    tuples = [[Fraction(100 * i, n + 1)] * 4 if i <= n else [Fraction(100)] * 4 for i in range(m)]
                    xs = list(range(m))
                else:
                    tuples = [[100, 0, 0, 0]] * m
                    xs = [-i for i in range(m)]
                vs = [vx(str(i), xs[i], tuples[i]) for i in range(m)]
                R.append(gr("P01/LONG-%s-%d/%s" % (kind, n, "EXT" if ext else "BASE"), vs,
                            [(str(i), str(i + 1)) for i in range(m - 1)], fam="LONG"))
    return R


def event(idx, kind, unit, uid, payload, fid):
    if unit is None:
        return {"index": str(idx), "kind": kind, "unit": None, "id": None, "payload": payload,
                "event_id": fid + "|population-1|CONFLICT_DECISION"}
    return {"index": str(idx), "kind": kind, "unit": unit, "id": xid(uid), "payload": payload,
            "event_id": "%s|1|7|minecraft:overworld|%s|0|0|decision-1|%s" % (fid, uid, kind)}


ROLL_U = [("a", 0, [100, 100, 0, 0], 8, "WIN"), ("b", 1, [100, 0, 0, 0], 1, "WIN"), ("c", 2, [50, 0, 0, 0], 3, "LOSS")]


def roll_log(fid, variant):
    rows = []  # (kind, unit, uid, payload)
    for (u, x, t, s, ro) in ROLL_U:
        rows.append(("QUALIFIED", u, x, {"value": True}))
    if variant == "NOFIT":
        rows.append(("QUALIFIED", "d", 3, {"value": True}))
    for (u, x, t, s, ro) in ROLL_U:
        rows.append(("FIT_PLAN_COMPLETE", u, x, {
            "slots": [{"slot": str(k), "seed_rank": str(k), "status": "FIT" if k == s else "HARD_REJECT"} for k in range(1, 9)],
            "selected_slot": str(s), "fit_eligible": True, "components": [q(c) for c in t], "roll": None}))
    if variant == "NOFIT":
        rows.append(("FIT_PLAN_COMPLETE", "d", 3, {
            "slots": [{"slot": str(k), "seed_rank": str(k), "status": "HARD_REJECT"} for k in range(1, 9)],
            "selected_slot": None, "fit_eligible": False, "components": None, "roll": "NOT_ROLLED"}))
    fitpos = {r[1]: str(i + 1) for i, r in enumerate(rows) if r[0] == "FIT_PLAN_COMPLETE"}
    for (u, x, t, s, ro) in ROLL_U:
        rows.append(("ORIGINAL_ROLL", u, x, {"result": ro, "fit_ref": fitpos[u]}))
    dpos = str(len(rows) + 1)
    rows.append(("CONFLICT_DECISION", None, None, {
        "cohort": ["a", "b", "c"], "winners": ["a", "b"], "edges": [{"u": "a", "v": "b"}],
        "trace": [{"v": "a", "d": "ACCEPT", "witness": []}, {"v": "b", "d": "REJECT_SPACING", "witness": ["a"]}]}))
    rows.append(("ACCEPTED_PENDING", "a", 0, {"ref": dpos}))
    rows.append(("SPACING_EXCLUDED", "b", 1, {"ref": dpos}))
    extra = {"INVALID": [("CERTIFICATE_INVALID",)], "RETIRE": [("APPLYING",), ("UNCERTAIN",), ("QUARANTINED",), ("RETIRED_PROTECTED",)],
             "MODEL-SUCCESS": [("MODEL_PUBLICATION_RECEIPT",)], "MODEL-EDIT": [("MODEL_PUBLICATION_RECEIPT",), ("MODEL_PLAYER_MODIFIED",)]}.get(variant, [])
    for (k,) in extra:
        pl = {"complete": True, "durable": True, "effect": True} if k == "MODEL_PUBLICATION_RECEIPT" else {}
        rows.append((k, "a", 0, pl))
    return [event(i + 1, k, u, x, p, fid) for i, (k, u, x, p) in enumerate(rows)]


def prot_log(fid):
    t = {"A": [100, 0, 0, 0], "B": [100, 50, 0, 0], "C": [100, 100, 0, 0]}
    xs = {"B": -1, "A": 0, "C": 1}
    rows = [("QUALIFIED", u, xs[u], {"value": True}) for u in "BAC"]
    rows += [("FIT_PLAN_COMPLETE", u, xs[u], {"slots": [{"slot": "1", "seed_rank": "0", "status": "FIT"}], "selected_slot": "1",
                                              "fit_eligible": True, "components": [q(c) for c in t[u]], "roll": None}) for u in "BAC"]
    rows += [("ORIGINAL_ROLL", u, xs[u], {"result": "WIN", "fit_ref": str(4 + i)}) for i, u in enumerate("BAC")]
    rows.append(("CONFLICT_DECISION", None, None, {
        "cohort": ["A", "B", "C"], "winners": ["A", "B", "C"], "edges": [{"u": "B", "v": "A"}, {"u": "C", "v": "B"}],
        "protected_conflicts": [{"site": "P", "with": ["C"]}],
        "trace": [{"v": "C", "d": "REJECT_PROTECTED", "witness": ["P"]}, {"v": "B", "d": "ACCEPT", "witness": []},
                  {"v": "A", "d": "REJECT_SPACING", "witness": ["B"]}]}))
    rows += [("ACCEPTED_PENDING", "B", -1, {"ref": "10"}), ("SPACING_EXCLUDED", "A", 0, {"ref": "10"}),
             ("PROTECTION_EXCLUDED", "C", 1, {"ref": "10"})]
    return [event(i + 1, k, u, x, p, fid) for i, (k, u, x, p) in enumerate(rows)]


def exp_acct_records(main_ids):
    R = []
    spec = [("P01/ACCT-BASE", "BASE"), ("P01/ACCT-INVALID", "INVALID"), ("P01/ACCT-RETIRE", "RETIRE"),
            ("P01/ACCT-MODEL-SUCCESS", "MODEL-SUCCESS"), ("P01/ACCT-MODEL-EDIT", "MODEL-EDIT"),
            ("P01/ACCT-PROTECTED", "PROTECTED"), ("P01/NOFIT-01/LOG", "NOFIT")]
    byid = {}
    for fid, var in spec:
        r = blank(fid, "ACCT")
        if var == "PROTECTED":
            r["events"] = prot_log(fid)
            r["protected_sites"] = [{"label": "P", "id": xid(-2), "status": "RETIRED_PROTECTED", "conflicts_with": ["C"]}]
        else:
            r["events"] = roll_log(fid, var)
        R.append(r)
        byid[fid] = r
    # negatives
    base = roll_log("P01/NEG-CONTRADICT-01", "BASE")
    r = blank("P01/NEG-CONTRADICT-01", "NEG")
    r["events"] = base + [event(13, "ORIGINAL_ROLL", "c", 2, {"result": "WIN", "fit_ref": "6"}, "P01/NEG-CONTRADICT-01")]
    R.append(r)
    fid = "P01/NEG-ROLL-FIRST-01"
    full = {(e["kind"], e["unit"]): e for e in roll_log(fid, "BASE")}
    order = [("QUALIFIED", "a"), ("QUALIFIED", "b"), ("QUALIFIED", "c"), ("ORIGINAL_ROLL", "a"),
             ("FIT_PLAN_COMPLETE", "a"), ("FIT_PLAN_COMPLETE", "b"), ("FIT_PLAN_COMPLETE", "c"),
             ("ORIGINAL_ROLL", "b"), ("ORIGINAL_ROLL", "c")]
    evs = []
    for i, key in enumerate(order, 1):
        e = json.loads(json.dumps(full[key]))
        e["index"] = str(i)
        evs.append(e)
    fitpos = {e["unit"]: e["index"] for e in evs if e["kind"] == "FIT_PLAN_COMPLETE"}
    for e in evs:
        if e["kind"] == "ORIGINAL_ROLL":
            e["payload"]["fit_ref"] = fitpos[e["unit"]]
    r = blank(fid, "NEG")
    r["events"] = evs
    R.append(r)
    for fid, bid, mode in (("P01/ACCT-DUP-LOG", "P01/ACCT-BASE", "DUP_LOG"), ("P01/ACCT-DUP-RECEIPT", "P01/ACCT-MODEL-SUCCESS", "DUP_RECEIPT"),
                           ("P01/ACCT-LOSS-BASE-PROJECTION", "P01/ACCT-BASE", "LOSS_PROJECTION"),
                           ("P01/ACCT-LOSS-BASE-SOURCE", "P01/ACCT-BASE", "LOSS_SOURCE"),
                           ("P01/ACCT-LOSS-MODEL-SUCCESS-PROJECTION", "P01/ACCT-MODEL-SUCCESS", "LOSS_PROJECTION"),
                           ("P01/ACCT-LOSS-MODEL-SUCCESS-SOURCE", "P01/ACCT-MODEL-SUCCESS", "LOSS_SOURCE")):
        r = json.loads(json.dumps(byid[bid]))
        r["fixture_id"] = fid
        r["expected_ref"] = fid
        r["derived"] = {"base_ref": bid, "mode": mode}
        R.append(r)
    return R


# ------------------------------------------------------------------ expected schedules
def blank_sched(no, fid, **kw):
    s = {"schedule_no": str(no), "batch": None, "vertex_order": [], "edge_order": [], "protected_order": [],
         "candidate_orders": {}, "stage_orders": [], "steps": [], "cut": None}
    s.update(kw)
    return s


def ids_sorted(verts):
    return [v["label"] for v in sorted(verts, key=lambda v: int(v["id"]["x"]))]


def eorders(edges):
    return [edges] if len(edges) <= 1 else [edges, list(reversed(edges))]


def exp_graph_sched(r):
    lab = ids_sorted(r["vertices"])
    pl = [p["label"] for p in sorted(r["protected_sites"], key=lambda p: int(p["id"]["x"]))]
    pos = [pl] if len(pl) <= 1 else [pl, list(reversed(pl))]
    out = []
    for p in perms(len(lab)):
        for eo in eorders(r["edges"]):
            for po in pos:
                out.append(blank_sched(len(out) + 1, r["fixture_id"], vertex_order=[lab[i] for i in p], edge_order=eo, protected_order=po))
    return out


def exp_long_sched(r):
    m = len(r["vertices"])
    inc = [str(i) for i in range(m)]
    seqs = [inc, inc[::-1], inc[0::2] + inc[1::2]]
    alt = []
    for k in range(m):
        alt.append(str(k // 2) if k % 2 == 0 else str(m - 1 - k // 2))
    seqs.append(alt)
    uniq = []
    for s in seqs:
        if s not in uniq:
            uniq.append(s)
    out = []
    for s in uniq:
        for eo in eorders(r["edges"]):
            out.append(blank_sched(len(out) + 1, r["fixture_id"], vertex_order=s, edge_order=eo))
    return out


def exp_late_sched(r):
    stage_c = []
    for st in r["stages"]:
        lab = ids_sorted(st["vertices"])
        stage_c.append([(st["stage"], [lab[i] for i in p], eo) for p in perms(len(lab)) for eo in eorders(st["edges"])])
    out = []
    for j in range(len(stage_c[0])):
        out.append(blank_sched(j + 1, r["fixture_id"], stage_orders=[
            {"stage": s, "vertex_order": vo, "edge_order": eo} for (s, vo, eo) in (stage_c[0][j], stage_c[1][j])]))
    return out


def log_orders(events, with_illegal=None):
    """Return list of index-sequences: unit-block permutations then PHASE-ABC, PHASE-CBA."""
    units = {}
    tail = []
    for e in events:
        if e["kind"] in ("QUALIFIED", "FIT_PLAN_COMPLETE", "ORIGINAL_ROLL"):
            units.setdefault(e["unit"], {})[e["kind"]] = e["index"]
        else:
            tail.append(e["index"])
    names = sorted(units, key=lambda u: int(next(e["id"]["x"] for e in events if e["unit"] == u)))
    kinds = ["QUALIFIED", "FIT_PLAN_COMPLETE", "ORIGINAL_ROLL"]
    res = []
    for p in perms(len(names)):
        seq = []
        for i in p:
            seq += [units[names[i]][k] for k in kinds if k in units[names[i]]]
        res.append(seq + tail)
    for order in (names, list(reversed(names))):
        seq = []
        for k in kinds:
            seq += [units[u][k] for u in order if k in units[u]]
        res.append(seq + tail)
    return res


def exp_acct_sched(r):
    ords = log_orders(r["events"])
    out = []
    for o in ords:
        for first, second in (("worker0", "worker1"), ("worker1", "worker0")):
            out.append(blank_sched(len(out) + 1, r["fixture_id"],
                                   steps=[s for e in o for s in ({"event": e, "origin": first}, {"event": e, "origin": second})]))
    for o in ords:
        for k in range(len(o) + 1):
            out.append(blank_sched(len(out) + 1, r["fixture_id"], cut=str(k), steps=[{"event": e, "origin": "none"} for e in o]))
    return out


def exp_neg_sched(r):
    fid = r["fixture_id"]
    out = []
    if "CONTRADICT" in fid:
        legal = [e for e in r["events"] if e["index"] != "13"]
        for o in log_orders(legal):
            out.append(blank_sched(len(out) + 1, fid, steps=[{"event": e, "origin": "none"} for e in o + ["13"]]))
    else:
        legal = [e for e in r["events"] if e["index"] != "4"]
        fa = [e["index"] for e in r["events"] if e["kind"] == "FIT_PLAN_COMPLETE" and e["unit"] == "a"][0]
        for o in log_orders(legal):
            o = list(o)
            o.insert(o.index(fa), "4")
            out.append(blank_sched(len(out) + 1, fid, steps=[{"event": e, "origin": "none"} for e in o]))
    return out


def exp_derived_sched(r):
    kinds = {e["index"]: e["kind"] for e in r["events"]}
    idx = [e["index"] for e in r["events"]]
    m = r["derived"]["mode"]
    if m == "DUP_LOG":
        seq = idx + idx
    elif m == "DUP_RECEIPT":
        seq = idx + [i for i in idx if kinds[i] == "MODEL_PUBLICATION_RECEIPT"]
    elif m == "LOSS_PROJECTION":
        seq = [i for i in idx if kinds[i] not in ("ACCEPTED_PENDING", "SPACING_EXCLUDED")]
    else:
        seq = [i for i in idx if kinds[i] not in ("ACCEPTED_PENDING", "SPACING_EXCLUDED", "CONFLICT_DECISION", "MODEL_PUBLICATION_RECEIPT")]
    return [blank_sched(1, r["fixture_id"], steps=[{"event": e, "origin": "none"} for e in seq])]


def exp_schedules(recs):
    fams = []
    for no, r in enumerate(recs, 1):
        fid, fam = r["fixture_id"], r["family"]
        if fid == "P01/ARITH-V0":
            ss = [blank_sched(1, fid, batch={"kind": "CASE_NOS", "items": [str(i) for i in range(1, 13)]}),
                  blank_sched(2, fid, batch={"kind": "CASE_NOS", "items": [str(i) for i in range(12, 0, -1)]})]
        elif fid == "P01/FIXED-01":
            ss = [blank_sched(i + 1, fid, batch={"kind": "SCALAR_LABELS", "items": it})
                  for i, it in enumerate((["V2"], ["V0", "V2", "V1"], ["K4", "V2"]))]
        elif fam == "ARITH":
            continue
        elif fid == "P01/CAND-01":
            ss = [blank_sched(1, fid, vertex_order=["U"], candidate_orders={"U": ["c0", "c1"]}),
                  blank_sched(2, fid, vertex_order=["U"], candidate_orders={"U": ["c1", "c0"]})]
        elif fam == "GRAPH":
            ss = exp_graph_sched(r)
        elif fam == "LONG":
            ss = exp_long_sched(r)
        elif fam == "LATE":
            ss = exp_late_sched(r)
        elif fam == "NEG":
            ss = exp_neg_sched(r)
        elif r["derived"]:
            ss = exp_derived_sched(r)
        else:
            ss = exp_acct_sched(r)
        fams.append((no, fid, fam, ss))
    return fams


# ------------------------------------------------------------------ invalid-input codes (plan 4.7 / prep C.7)
CODES = ["JSON-SYNTAX", "DUP-KEY", "NONCANON", "NUMBER-TOKEN", "INT-FORMAT", "INT-BITS", "MISSING-KEY", "EXTRA-KEY",
         "ENUM-VALUE", "RAT-ZERO-DEN", "RAT-NEG-DEN", "RAT-UNREDUCED", "SCORE-RANGE", "TUPLE-ARITY", "ID-DUPLICATE",
         "ID-PREFIX-MIXED", "LABEL-DUPLICATE", "EDGE-SELF", "EDGE-DANGLING", "EDGE-DUPLICATE", "EDGE-UNKNOWN-CONFLICT",
         "POP-FLAG", "STAGE-NOT-MONOTONE", "CAND-OVERFLOW", "CAND-NULL-MISMATCH", "CAND-RANK-DUP", "CAND-SLOT-DUP",
         "ROLL-BEFORE-FIT-STATIC", "EVENT-ID-FORMAT"]


def exp_invalid_bytes(code, base):
    """Reproduce the exact bytes of the invalid file for `code` from the CAV-01 base record."""
    b = json.loads(json.dumps(base))
    U, V = b["vertices"][0], b["vertices"][1]
    txt = canon(base).decode()
    if code == "JSON-SYNTAX":
        return txt.replace('"fixture_id": "P01/CAV-01"', '"fixture_id": "P01/CAV-01', 1).encode()
    if code == "DUP-KEY":
        line = '  "fixture_id": "P01/CAV-01",\n'
        return txt.replace(line, line + line, 1).encode()
    if code == "NONCANON":
        ks = sorted(base, key=lambda s: s.encode(), reverse=True)
        return ("{\n" + ",\n".join("  " + json.dumps(k) + ": " + enc(base[k], 1) for k in ks) + "\n}\n").encode()
    if code == "NUMBER-TOKEN":
        return txt.replace('"slot": "1"', '"slot": 1', 1).encode()
    if code == "INT-FORMAT":
        return txt.replace('"x": "-1"', '"x": "-01"', 1).encode()
    if code == "INT-BITS":
        V["id"]["y"] = str(1 << 4096)
    elif code == "MISSING-KEY":
        del U["qualified"]
    elif code == "EXTRA-KEY":
        U["note"] = "x"
    elif code == "ENUM-VALUE":
        V["roll"] = "MAYBE"
    elif code == "RAT-ZERO-DEN":
        U["candidates"][0]["components"][0] = {"num": "1", "den": "0"}
    elif code == "RAT-NEG-DEN":
        U["candidates"][0]["components"][0] = {"num": "1", "den": "-2"}
    elif code == "RAT-UNREDUCED":
        U["candidates"][0]["components"][0] = {"num": "2", "den": "4"}
    elif code == "SCORE-RANGE":
        U["candidates"][0]["components"][0] = {"num": "101", "den": "1"}
    elif code == "TUPLE-ARITY":
        U["candidates"][0]["components"] = U["candidates"][0]["components"][:3]
    elif code == "ID-DUPLICATE":
        V["id"] = dict(U["id"])
    elif code == "ID-PREFIX-MIXED":
        V["id"]["dimension"] = "minecraft:the_nether"
    elif code == "LABEL-DUPLICATE":
        w = json.loads(json.dumps(U))
        w["id"] = xid(1)
        b["vertices"].append(w)
    elif code == "EDGE-SELF":
        b["edges"] = [{"u": "U", "v": "U"}]
    elif code == "EDGE-DANGLING":
        b["edges"] = [{"u": "U", "v": "W"}]
    elif code == "EDGE-DUPLICATE":
        b["edges"] = b["edges"] + [{"u": "U", "v": "V"}]
    elif code == "EDGE-UNKNOWN-CONFLICT":
        b["unknown_edges"] = [{"u": "U", "v": "V"}]
        b["population_complete"] = False
    elif code == "POP-FLAG":
        b["population_complete"] = False
    elif code == "STAGE-NOT-MONOTONE":
        vs = b["vertices"]
        b["stages"] = [{"stage": "OPEN", "certificate": "OPEN", "vertices": vs, "edges": b["edges"], "unknown_edges": [], "population_complete": True},
                       {"stage": "REVEAL", "certificate": "COMPLETE", "vertices": vs, "edges": [], "unknown_edges": [], "population_complete": True}]
    elif code == "CAND-OVERFLOW":
        U["candidates"] = [cd("c%d" % k, k, k - 1, "FIT", [0, 100, 0, 0]) for k in range(1, 10)]
    elif code == "CAND-NULL-MISMATCH":
        U["candidates"][0]["components"] = None
    elif code == "CAND-RANK-DUP":
        U["candidates"] = [cd("c0", 1, 0, "FIT", [0, 100, 0, 0]), cd("c1", 2, 0, "FIT", [100, 0, 0, 0])]
    elif code == "CAND-SLOT-DUP":
        U["candidates"] = [cd("c0", 1, 0, "FIT", [0, 100, 0, 0]), cd("c1", 1, 1, "FIT", [100, 0, 0, 0])]
    elif code == "ROLL-BEFORE-FIT-STATIC":
        U["fit_complete"] = False
    elif code == "EVENT-ID-FORMAT":
        b["events"] = [{"index": "1", "kind": "QUALIFIED", "unit": "U", "id": xid(-1), "payload": {"value": True},
                        "event_id": "P01/CAV-01|1|7|minecraft:overworld|-1|0|0|QUALIFIED"}]
    else:
        raise KeyError(code)
    return canon(b)


# ------------------------------------------------------------------ assertions / controls
ASR = ["ACCEPT", "REJECT-CODE", "ARITH-COMP", "ARITH-TOTAL", "INPUT-UNCHANGED", "STATUS", "CAND-ORDER", "CAND-SELECTED",
       "ORDER", "DECISION", "WITNESS", "WITNESS-SET", "FINAL-SETS", "SCHED-INVARIANT", "COUNTERS", "ACCT-SETS",
       "EVENT-IDEM", "REPLAY", "NOEXTRA", "NOPHYS", "COMPLETE", "LATE"]


def req_set(fid, fam, invalid=False):
    if invalid:
        return {"REJECT-CODE", "COMPLETE"}
    base = {"ACCEPT", "INPUT-UNCHANGED", "SCHED-INVARIANT", "NOEXTRA", "COMPLETE"}
    if fam == "ARITH":
        return base | {"ARITH-COMP", "ARITH-TOTAL"}
    if fam in ("GRAPH", "LONG", "LATE"):
        s = base | {"ARITH-COMP", "ARITH-TOTAL", "STATUS", "ORDER", "DECISION", "WITNESS", "WITNESS-SET", "FINAL-SETS"}
        if fid in ("P01/CAND-01", "P01/ROLL-01", "P01/NOFIT-01"):
            s |= {"CAND-ORDER", "CAND-SELECTED"}
        if fid in ("P01/ROLL-01", "P01/NOFIT-01"):
            s |= {"COUNTERS"}
        if fam == "LATE":
            s |= {"LATE"}
        return s
    return base | {"STATUS", "COUNTERS", "ACCT-SETS", "EVENT-IDEM", "REPLAY", "NOPHYS"}


def na_reason(fid, fam, invalid):
    if invalid:
        return "invalid-input case: only rejection code and completeness apply"
    if fam == "ARITH":
        return "arithmetic-only case without graph evaluation"
    if fam in ("GRAPH", "LONG", "LATE"):
        return "no accounting events in this family"
    return "no graph evaluation in this family"


def exp_assertions(fid, fam, invalid=False):
    rs = req_set(fid, fam, invalid)
    return [{"id": "ASR-" + a, "mode": "REQUIRED" if a in rs else "NA",
             "na_reason": None if a in rs else na_reason(fid, fam, invalid)} for a in ASR]


# controls: id -> (identification, must_fail, extra_must_pass)
CTL = [
    ("N-INNER-WEIGHT", "SIGNATURE", ["ARITH-COMP", "ARITH-TOTAL"], []),
    ("N-EARLY-FLOOR", "SIGNATURE", ["ARITH-COMP", "ARITH-TOTAL"], []),
    ("N-RELATIVE", "SIGNATURE", ["ARITH-TOTAL"], []),
    ("N-CAND-COMPONENT", "SIGNATURE", ["CAND-ORDER", "CAND-SELECTED"], ["FINAL-SETS"]),
    ("N-CAND-ARRIVAL", "SIGNATURE", ["CAND-ORDER", "CAND-SELECTED"], ["FINAL-SETS"]),
    ("N-CROSS-C-FIRST", "SIGNATURE", ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-CROSS-SEED", "SIGNATURE", ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-C-FIRST", "SIGNATURE", ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-T-FIRST", "SIGNATURE", ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-ROUND-2", "SIGNATURE", ["ARITH-TOTAL", "ORDER", "FINAL-SETS"], []),
    ("N-FLOAT64", "SIGNATURE", ["ARITH-COMP", "FINAL-SETS"], []),
    ("N-FIXED64", "OUTCOME_CLASS", [], []),
    ("N-ID-STRING", "SIGNATURE", ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-REJECTED-VETO", "SIGNATURE", ["DECISION", "WITNESS", "FINAL-SETS"], ["ORDER", "ARITH-TOTAL"]),
    ("N-ID-FIRST", "SIGNATURE", ["ORDER"], ["ARITH-TOTAL"]),
    ("N-PROTECTION-IGNORE", "SIGNATURE", ["DECISION", "FINAL-SETS"], ["ORDER"]),
    ("N-OMIT-ENDPOINT", "SIGNATURE", ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-LOCAL-CAP", "SIGNATURE", ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"]),
    ("N-TIE-ID-DESC", "SIGNATURE", ["ORDER"], ["ARITH-TOTAL"]),
    ("N-ARRIVAL", "SIGNATURE", ["ORDER"], ["ARITH-TOTAL"]),
    ("N-UNKNOWN-EMPTY", "SIGNATURE", ["LATE", "FINAL-SETS"], []),
    ("N-QUALIFIED-DENOM", "SIGNATURE", ["COUNTERS", "STATUS"], ["ARITH-TOTAL"]),
    ("N-NINTH", "OUTCOME_CLASS", ["COUNTERS", "STATUS"], ["ARITH-TOTAL"]),
    ("N-ROLL-FIRST", "SIGNATURE", ["STATUS", "REPLAY"], []),
    ("N-ACCEPT-IS-COMPLETE", "SIGNATURE", ["NOPHYS", "COUNTERS"], []),
    ("N-POSTROLL-NOFIT", "SIGNATURE", ["COUNTERS", "STATUS"], []),
    ("N-REROLL", "SIGNATURE", ["NOEXTRA", "COUNTERS"], []),
    ("N-IGNORE-P", "SIGNATURE", ["ACCT-SETS", "COUNTERS"], []),
    ("N-LOGICAL-IS-PHYSICAL", "SIGNATURE", ["NOPHYS", "COUNTERS"], []),
    ("N-ERASE-HISTORY", "OUTCOME_CLASS", ["ACCT-SETS", "COUNTERS"], []),
    ("N-ADDITIVE", "SIGNATURE", ["EVENT-IDEM", "COUNTERS"], []),
    ("N-NEW-EPOCH", "OUTCOME_CLASS", ["REPLAY", "NOEXTRA"], []),
    ("N-MISSING-MEANS-EMPTY", "OUTCOME_CLASS", ["REPLAY", "NOEXTRA"], []),
]


# ------------------------------------------------------------------ main verification
def main():
    files_ok = True
    p = lambda n: os.path.join(HERE, n)
    # allowed content
    allowed = {"inputs-v1.json", "schedules-v1.json", "controls-v1.json", "expected-traces-v1.json", "runplan-v1.json",
               "fixture-index-v1.tsv", "materialize.py", "verify_materialization.py", "CANDIDATE-DIGESTS.sha256", "invalid-inputs-v1",
               "oracle"}  # oracle/: G-3 candidate content (goldens live there; pins/COMPLETE still forbidden)
    present = set(os.listdir(HERE)) - {"__pycache__"}
    ok(present <= allowed, "unexpected entries (goldens/pins/other?): %s" % sorted(present - allowed))
    ok(allowed - {"oracle"} - present == set(), "missing entries: %s" % sorted(allowed - {"oracle"} - present))
    for forbidden in ("goldens-v1.json", "pins-v1.json", "COMPLETE"):
        ok(not os.path.exists(p(forbidden)), "forbidden artifact present: " + forbidden)

    raw_in = open(p("inputs-v1.json"), "rb").read()
    ok(len(raw_in) <= 16 * 1024 * 1024, "inputs-v1.json exceeds 16 MiB")
    try:
        inputs = strict_load(raw_in)
    except Exception as e:
        bad("inputs-v1.json not strict-loadable: %s" % e)
        return
    ok(canon(inputs) == raw_in, "inputs-v1.json is not canonical (round-trip mismatch)")
    exp_recs = exp_main_records() + exp_acct_records(None)
    ok(len(exp_recs) == 78, "independent record count is %d, expected 78" % len(exp_recs))
    ok(isinstance(inputs, list) and len(inputs) == 78, "inputs-v1.json holds %s records, expected 78" % (len(inputs) if isinstance(inputs, list) else "?"))
    ids = [r.get("fixture_id") for r in inputs]
    ok(len(set(ids)) == len(ids), "duplicate fixture ids")
    ok(ids == [r["fixture_id"] for r in exp_recs], "fixture id / case_no order differs from the contract")
    for i, (e, a) in enumerate(zip(exp_recs, inputs), 1):
        a2 = json.loads(json.dumps(a))
        ok(isinstance(a2.get("source_refs"), list) and a2["source_refs"] and all(isinstance(x, str) for x in a2["source_refs"]),
           "case %d: source_refs must be a non-empty string list" % i)
        a2["source_refs"] = e["source_refs"]
        if a2 != e:
            diffs = [k for k in e if a2.get(k) != e[k]] + [k for k in a2 if k not in e]
            bad("case %d (%s): record differs from independent reconstruction in %s" % (i, e["fixture_id"], diffs))
    # no key outside the schema
    schema_keys = set(exp_recs[0].keys())
    for r in inputs:
        ok(set(r) == schema_keys, "record %s has unexpected key set" % r.get("fixture_id"))

    # ----- schedules
    raw_s = open(p("schedules-v1.json"), "rb").read()
    scheds = strict_load(raw_s)
    ok(canon(scheds) == raw_s, "schedules-v1.json not canonical")
    fams = exp_schedules(exp_recs)
    exp_counts = {}
    ok([ (f["case_no"], f["fixture_id"]) for f in scheds["families"]] == [(str(n), fid) for (n, fid, fam, ss) in fams],
       "schedule family list differs from the contract")
    sched_by_case = {}
    for f, (n, fid, fam, ss) in zip(scheds["families"], fams):
        got = []
        seen_ids = set()
        for s in f["schedules"]:
            s2 = dict(s)
            sid = s2.pop("schedule_id")
            tag = s2.pop("tag")
            ok(isinstance(sid, str) and sid.startswith("S-" + fid + "-") and sid not in seen_ids, "%s: bad/duplicate schedule_id %s" % (fid, sid))
            seen_ids.add(sid)
            ok(isinstance(tag, str) and tag, "%s: empty tag" % fid)
            got.append(s2)
        ok(got == ss, "%s: schedules differ from independent reconstruction (got %d, want %d)" % (fid, len(got), len(ss)))
        ok(f["count"] == str(len(ss)), "%s: count field %s != %d" % (fid, f["count"], len(ss)))
        ok(f["list_sha256"] == h(canon(f["schedules"])), "%s: list_sha256 mismatch" % fid)
        exp_counts[fid] = len(ss)
        sched_by_case[str(n)] = f["schedules"]
    total_main = sum(exp_counts.values())
    ok(scheds["total"] == str(total_main), "schedules total field mismatch")
    # closed-form cross-check of counts (independent of enumeration)
    f3, f4 = factorial(3), factorial(4)
    formula = {
        "arith": 5, "small": 2 + 2 + 2 * f3 * 2 + 2 + 2 + f4 * 2 + f3 * 2 + f3 * 2 + f3 * 2 + f4 * 2,
        "rollnofit": f3 + f4, "late": f4 * 2, "acct_delivery": 6 * (f3 + 2) * 2 + (f4 + 2) * 2,
        "dup_loss": 6, "neg": 16,
        "replay": 8 * 13 + 8 * 14 + 8 * 17 + 8 * 14 + 8 * 15 + 26 * 15 + 8 * 14}
    long_total = 0
    for m_ext in (0, 1):
        for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024):
            m = n + 1 + m_ext
            distinct = {2: 2, 3: 3}.get(m, 4)
            long_total += distinct * (2 if m - 1 > 1 else 1)
    long_total *= 2
    formula["long"] = long_total
    ok(formula["long"] == 268, "closed-form LONG count %d != 268" % formula["long"])
    ok(formula["small"] == 164, "closed-form small-graph count %d != 164" % formula["small"])
    clean_formula = sum(formula.values()) + 29
    ok(clean_formula == 1800, "closed-form clean session count %d != 1800" % clean_formula)
    ok(total_main + 29 == 1800, "enumerated clean sessions %d != 1800" % (total_main + 29))
    ok(total_main == 1771, "enumerated main schedules %d != 1771" % total_main)

    # ----- invalid inputs
    inv_dir = p("invalid-inputs-v1")
    names = sorted(os.listdir(inv_dir))
    ok(names == ["I%03d.json" % i for i in range(1, 30)], "invalid-input directory listing is not I001..I029.json: %s" % names[:5])
    order = sorted(CODES, key=lambda c: h(("P01-INVALID-1|" + c).encode()))
    ok(len(set(CODES)) == 29 and len(CODES) == 29, "independent invalid code list is not 29 distinct codes")
    # base = the CAV-01 record as stored (already verified equal to the reconstruction apart from free-text source_refs)
    base = [r for r in inputs if r["fixture_id"] == "P01/CAV-01"][0]
    inv_bytes = {}
    for i, code in enumerate(order, 1):
        fn = "I%03d.json" % i
        data = open(os.path.join(inv_dir, fn), "rb").read() if fn in names else b""
        inv_bytes[fn] = data
        ok(len(data) <= 64 * 1024, "%s exceeds 64 KiB" % fn)
        ok(data == exp_invalid_bytes(code, base), "%s: bytes differ from the reconstruction for defect %s (numbering or defect isolation)" % (fn, code))
        ok(data != canon(base), "%s identical to the valid base" % fn)

    # ----- controls
    raw_c = open(p("controls-v1.json"), "rb").read()
    ctrl = strict_load(raw_c)
    ok(canon(ctrl) == raw_c, "controls-v1.json not canonical")
    cases = {r["fixture_id"]: str(i) for i, r in enumerate(exp_recs, 1)}
    fam_of = {r["fixture_id"]: r["family"] for r in exp_recs}
    sch = {fid: ss for (n, fid, fam, ss) in fams}

    def sel(fid, delivery=None):
        ss = sch[fid]
        res = []
        for s in ss:
            if delivery is None or (delivery == (s["cut"] is None)):
                res.append(s["schedule_no"])
        return {"case_no": cases[fid], "schedule_nos": res}

    ext = ["P01/LONG-%s-%d/EXT" % (k, n) for k in ("SCORE", "TIE") for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024)]
    tie = ["P01/LONG-TIE-%d/%s" % (n, b) for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024) for b in ("BASE", "EXT")]
    alll = ["P01/LONG-%s-%d/%s" % (k, n, b) for k in ("SCORE", "TIE") for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024) for b in ("BASE", "EXT")]
    arith = [{"case_no": "1", "schedule_nos": ["1", "2"]}, {"case_no": cases["P01/FIXED-01"], "schedule_nos": ["1", "2", "3"]}]
    T = {
        "N-INNER-WEIGHT": [arith[0]], "N-EARLY-FLOOR": arith, "N-RELATIVE": [arith[1]],
        "N-CAND-COMPONENT": [sel("P01/CAND-01")], "N-CAND-ARRIVAL": [sel("P01/CAND-01")],
        "N-CROSS-C-FIRST": [sel("P01/CAV-01")], "N-CROSS-SEED": [sel("P01/CAV-01")],
        "N-C-FIRST": [sel("P01/EQUAL-3-C")], "N-T-FIRST": [sel("P01/EQUAL-3-T")],
        "N-ROUND-2": [sel("P01/NEAR-01"), sel("P01/BIG-NEAR-01")], "N-FLOAT64": [sel("P01/BIG-NEAR-01")],
        "N-FIXED64": [sel("P01/BIG-NEAR-01")], "N-ID-STRING": [sel("P01/ID-ORDER-01")],
        "N-REJECTED-VETO": [sel(f) for f in ("P01/CHAIN-3", "P01/CHAIN-ID-01", "P01/CHAIN-PROT-01", "P01/CYCLE-01")],
        "N-ID-FIRST": [sel(f) for f in ("P01/CHAIN-3", "P01/CHAIN-ID-01", "P01/CYCLE-01")],
        "N-PROTECTION-IGNORE": [sel("P01/CHAIN-PROT-01")],
        "N-OMIT-ENDPOINT": [sel(f) for f in ext], "N-LOCAL-CAP": [sel(f) for f in ext],
        "N-TIE-ID-DESC": [sel(f) for f in tie], "N-ARRIVAL": [sel(f) for f in alll],
        "N-UNKNOWN-EMPTY": [sel("P01/LATE-01")],
        "N-QUALIFIED-DENOM": [sel("P01/NOFIT-01"), sel("P01/NOFIT-01/LOG", True)],
        "N-NINTH": [sel("P01/NOFIT-01"), sel("P01/NOFIT-01/LOG", True)],
        "N-ROLL-FIRST": [sel("P01/NEG-ROLL-FIRST-01")],
        "N-ACCEPT-IS-COMPLETE": [sel("P01/ACCT-BASE", True)], "N-POSTROLL-NOFIT": [sel("P01/ACCT-INVALID", True)],
        "N-REROLL": [sel("P01/ACCT-RETIRE", True)], "N-IGNORE-P": [sel("P01/ACCT-PROTECTED", True)],
        "N-LOGICAL-IS-PHYSICAL": [sel("P01/ACCT-MODEL-SUCCESS", True)], "N-ERASE-HISTORY": [sel("P01/ACCT-MODEL-EDIT", True)],
        "N-ADDITIVE": [sel("P01/ACCT-DUP-LOG"), sel("P01/ACCT-DUP-RECEIPT")],
        "N-NEW-EPOCH": [sel(f, False) for f in ("P01/ACCT-BASE", "P01/ACCT-INVALID", "P01/ACCT-RETIRE", "P01/ACCT-MODEL-SUCCESS",
                                                "P01/ACCT-MODEL-EDIT", "P01/ACCT-PROTECTED", "P01/NOFIT-01/LOG")],
        "N-MISSING-MEANS-EMPTY": [sel("P01/ACCT-LOSS-%s-%s" % (b, m)) for b in ("BASE", "MODEL-SUCCESS") for m in ("PROJECTION", "SOURCE")],
    }
    ok(len(CTL) == 33, "independent control table has %d entries" % len(CTL))
    rows = ctrl["controls"]
    ok([c["id"] for c in rows] == [c[0] for c in CTL], "control ids/order differ from the contract")
    ok(ctrl["control_count"] == "33", "control_count != 33")
    total_ctl = 0
    mapping_detect = {}
    for row, (cid, ident, mf, mp) in zip(rows, CTL):
        ok(row["identification"] == ident, "%s: identification" % cid)
        ok(row["must_fail"] == ["ASR-" + x for x in mf], "%s: must_fail" % cid)
        ok(row["must_pass"] == list(dict.fromkeys(["ASR-ACCEPT", "ASR-COMPLETE", "ASR-INPUT-UNCHANGED"] + ["ASR-" + x for x in mp])),
           "%s: must_pass" % cid)
        ok(row["trace_level"] == "SUMMARY", "%s: trace level" % cid)
        ok(row["targets"] == T[cid], "%s: targets differ from independent derivation" % cid)
        n = sum(len(t["schedule_nos"]) for t in T[cid])
        ok(n > 0, "%s has no applicable fixture/session" % cid)
        ok(row["sessions"] == str(n), "%s: sessions %s != %d" % (cid, row["sessions"], n))
        total_ctl += n
        want_ov = {"cross_candidate_seed_order": ["V", "U"]} if cid == "N-CROSS-SEED" else {}
        ok(row["control_overrides"] == want_ov, "%s: control_overrides" % cid)
        # detect_on
        if cid == "N-CAND-ARRIVAL":
            want = {"mode": "EXPLICIT", "schedules": [{"case_no": cases["P01/CAND-01"], "schedule_nos":
                    [s["schedule_no"] for s in sch["P01/CAND-01"] if s["candidate_orders"]["U"] == ["c0", "c1"]]}]}
        elif cid == "N-ARRIVAL":
            want = {"mode": "EXPLICIT", "schedules": [{"case_no": cases[f], "schedule_nos": [
                s["schedule_no"] for s in sch[f] if s["vertex_order"] != [str(i) for i in range(len(next(r for r in exp_recs if r["fixture_id"] == f)["vertices"]) - 1, -1, -1)]]} for f in alll]}
        else:
            want = {"mode": "ALL_SCHEDULES", "schedules": []}
        ok(row["detect_on"] == want, "%s: detect_on" % cid)
        ok(row["must_fail_provisional"] == (cid in ("N-ACCEPT-IS-COMPLETE", "N-POSTROLL-NOFIT", "N-REROLL", "N-IGNORE-P",
                                                      "N-LOGICAL-IS-PHYSICAL", "N-ERASE-HISTORY")), "%s: provisional flag" % cid)
    ok(total_ctl == 2344, "independent control sessions %d != 2344" % total_ctl)
    ok(ctrl["total_sessions"] == "2344", "controls total_sessions field")

    # ----- manifest and runplan
    raw_m = open(p("expected-traces-v1.json"), "rb").read()
    man = strict_load(raw_m)
    ok(canon(man) == raw_m, "expected-traces-v1.json not canonical")
    want_tr = []
    batchmap = {(str(n), s["schedule_no"]): s["batch"] for (n, fid, fam, ss) in fams for s in ss}

    def trip(subject, case, sno, level, batch, asr):
        return {"triple_id": "%s/%s/%s" % (subject, case, sno), "subject": subject, "case_no": case, "schedule_no": sno,
                "trace_level": level, "batch": batch, "trace_file": "subject/traces/%s/%s.%s.jsonl" % (case, sno, subject),
                "assertions": asr}

    for (n, fid, fam, ss) in fams:
        for s in ss:
            want_tr.append(trip("CLEAN", str(n), s["schedule_no"], "FULL", s["batch"], exp_assertions(fid, fam_of[fid])))
    for i in range(1, 30):
        want_tr.append(trip("CLEAN", "I%03d" % i, "1", "FULL", None, exp_assertions(None, None, True)))
    famof_case = {str(i): (r["fixture_id"], r["family"]) for i, r in enumerate(exp_recs, 1)}
    for cid, ident, mf, mp in CTL:
        for t in T[cid]:
            fid, fm = famof_case[t["case_no"]]
            for sn in t["schedule_nos"]:
                want_tr.append(trip(cid, t["case_no"], sn, "SUMMARY", batchmap[(t["case_no"], sn)], exp_assertions(fid, fm)))
    ok(len(want_tr) == 4144, "independent triple count %d != 4144" % len(want_tr))
    ok(man["triple_count"] == "4144", "manifest triple_count field")
    ok(man["triples"] == want_tr, "manifest triples differ from independent reconstruction")
    keys = [t["triple_id"] for t in man["triples"]]
    ok(len(set(keys)) == len(keys), "duplicate manifest triple ids")
    # coverage: every main record and every invalid file covered by a CLEAN triple
    covered = set()
    for t in man["triples"]:
        if t["subject"] != "CLEAN":
            continue
        covered.add(t["case_no"])
        if t["batch"] and t["batch"]["kind"] == "CASE_NOS":
            covered |= set(t["batch"]["items"])
    ok(covered == {str(i) for i in range(1, 79)} | {"I%03d" % i for i in range(1, 30)}, "manifest does not cover every main record and invalid file")
    raw_r = open(p("runplan-v1.json"), "rb").read()
    rp = strict_load(raw_r)
    ok(canon(rp) == raw_r, "runplan-v1.json not canonical")
    ok(rp["entries"] == [{"case_no": t["case_no"], "schedule_no": t["schedule_no"], "subject": t["subject"], "trace_level": t["trace_level"],
                          "trace_file": t["trace_file"]} for t in man["triples"]], "runplan is not the projection of the manifest")
    ok("assertions" not in raw_r.decode() and "fixture_id" not in raw_r.decode() and "na_reason" not in raw_r.decode(),
       "runplan leaks assertions or fixture names")
    ok(rp["triple_count"] == "4144", "runplan triple_count")

    # ----- fixture index
    idx_lines = open(p("fixture-index-v1.tsv"), "rb").read().decode().split("\n")
    ok(idx_lines[-1] == "", "fixture index must end with LF")
    idx_lines = idx_lines[:-1]
    want_idx = ["%s\t%s" % (r["fixture_id"], h(canon(r))) for r in inputs]
    want_idx += ["I%03d\t%s" % (i, h(inv_bytes["I%03d.json" % i])) for i in range(1, 30)]
    ok(len(want_idx) == 107, "independent index line count %d != 107" % len(want_idx))
    ok(idx_lines == want_idx, "fixture-index-v1.tsv differs from recomputation")

    # ----- candidate digests
    dig = open(p("CANDIDATE-DIGESTS.sha256")).read().strip().split("\n")
    seen = {}
    for line in dig:
        m = re.fullmatch(r"([0-9a-f]{64})  (\S+)", line)
        if not ok(m, "malformed digest line: %r" % line):
            continue
        seen[m.group(2)] = m.group(1)
    need = ["inputs-v1.json", "schedules-v1.json", "controls-v1.json", "expected-traces-v1.json", "runplan-v1.json",
            "fixture-index-v1.tsv", "materialize.py", "verify_materialization.py"] + ["invalid-inputs-v1/I%03d.json" % i for i in range(1, 30)]
    ok(sorted(seen) == sorted(need), "digest list covers the wrong files")
    for name, dg in seen.items():
        fp = p(name)
        ok(os.path.exists(fp) and h(open(fp, "rb").read()) == dg, "digest mismatch for %s" % name)

    summary = {"records": len(inputs), "invalid_files": len(names), "clean_sessions": total_main + 29, "controls": len(rows),
               "control_sessions": total_ctl, "triples": len(man["triples"]), "index_lines": len(idx_lines)}
    print("independent counts:", json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    main()
    if ERR:
        print("VERIFICATION FAILED (%d problems)" % len(ERR))
        for e in ERR[:60]:
            print(" -", e)
        sys.exit(1)
    print("VERIFICATION PASSED")
