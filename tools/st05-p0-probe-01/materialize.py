#!/usr/bin/env python3
"""ST-05 Probe 01 G-2 materializer (CANDIDATE artifacts only).

Generates, from the approved contracts (execution spec, binding plan rev.3,
G-1/G-2 preparation record), the deterministic input-side artifacts:
inputs-v1.json, schedules-v1.json, controls-v1.json, expected-traces-v1.json,
runplan-v1.json, invalid-inputs-v1/I001..I029.json, fixture-index-v1.tsv and
CANDIDATE-DIGESTS.sha256.

It creates NO goldens, NO pins, NO subject/observer code, NO results.
Python 3 standard library only; no network; no probe execution.
Usage: python3 materialize.py [--out DIR]
"""
import hashlib
import itertools
import json
import os
import sys
from fractions import Fraction

PREFIX = {"schema": "1", "seed": "7", "dimension": "minecraft:overworld"}
LONG_NS = [1, 2, 3, 4, 8, 16, 64, 256, 1024]


# ---------------------------------------------------------------- canonical JSON
def cj(v, ind=0):
    pad = "  " * ind
    if v is None:
        return "null"
    if v is True:
        return "true"
    if v is False:
        return "false"
    if isinstance(v, str):
        return json.dumps(v, ensure_ascii=True)
    if isinstance(v, list):
        if not v:
            return "[]"
        return "[\n" + ",\n".join(pad + "  " + cj(x, ind + 1) for x in v) + "\n" + pad + "]"
    if isinstance(v, dict):
        if not v:
            return "{}"
        return "{\n" + ",\n".join(
            pad + "  " + json.dumps(k, ensure_ascii=True) + ": " + cj(v[k], ind + 1)
            for k in sorted(v, key=lambda s: s.encode("utf-8"))
        ) + "\n" + pad + "}"
    raise TypeError("non-canonical value type: %r" % (type(v),))


def dump(v):
    return (cj(v) + "\n").encode("utf-8")


def sha(b):
    return hashlib.sha256(b).hexdigest()


def rat(x):
    f = Fraction(x)
    return {"num": str(f.numerator), "den": str(f.denominator)}


def ident(x):
    return {"x": str(x), "y": "0", "z": "0"}


def G(s):
    return [Fraction(s)] * 4


# ---------------------------------------------------------------- record helpers
def base_record(fid, family, refs):
    return {
        "record_schema": "P01-FIXTURE-1",
        "fixture_id": fid,
        "family": family,
        "source_refs": refs,
        "scalar_inputs": [],
        "id_prefix": dict(PREFIX),
        "vertices": [],
        "edges": [],
        "protected_sites": [],
        "unknown_edges": [],
        "population_complete": True,
        "stages": [],
        "events": [],
        "default_schedule": "canonical",
        "control_overrides": {},
        "expect_input_rejection": None,
        "expected_ref": fid,
        "derived": None,
    }


def cand(name, slot, rank, status, comps):
    return {
        "cand": name,
        "slot": str(slot),
        "seed_rank": str(rank),
        "status": status,
        "components": None if comps is None else [rat(c) for c in comps],
    }


def vertex(label, x, comps, roll="WIN", cands=None, fit_complete=True):
    return {
        "label": label,
        "id": ident(x),
        "qualified": True,
        "candidates": cands if cands is not None else [cand("c0", 1, 0, "FIT", comps)],
        "fit_complete": fit_complete,
        "roll": roll,
    }


def idnum(v):
    return int(v["id"]["x"])


def norm_edges(verts, pairs):
    ids = {v["label"]: idnum(v) for v in verts}
    out = []
    for a, b in pairs:
        u, w = (a, b) if ids[a] < ids[b] else (b, a)
        out.append({"u": u, "v": w})
    out.sort(key=lambda e: (ids[e["u"]], ids[e["v"]]))
    return out


def graph(fid, refs, spec, pairs, family="GRAPH", protected=None):
    r = base_record(fid, family, refs)
    r["vertices"] = [vertex(l, x, c) for (l, x, c) in spec]
    r["edges"] = norm_edges(r["vertices"], pairs)
    if protected:
        r["protected_sites"] = protected
    return r


# ---------------------------------------------------------------- fixtures
RAW9 = {0: [0] * 9, 1: [100] * 9, 2: [50, 75, 80, 75, 75, 75, 75, 50, 50]}
for _n in range(3, 12):
    RAW9[_n] = [100 if i == _n - 3 else 0 for i in range(9)]


def arith_record(n):
    r = base_record("P01/ARITH-V%d" % n, "ARITH", ["V%d" % n])
    r["id_prefix"] = None
    r["scalar_inputs"] = [{"label": "V%d" % n, "kind": "RAW9", "values": [rat(x) for x in RAW9[n]]}]
    return r


def fixed_record():
    r = base_record("P01/FIXED-01", "ARITH", ["FIXED-01", "K4"])
    r["id_prefix"] = None
    si = [{"label": "V%d" % n, "kind": "RAW9", "values": [rat(x) for x in RAW9[n]]} for n in (0, 1, 2)]
    si.append({"label": "K4", "kind": "COMP4",
               "values": [rat(Fraction(50001, 1000)), rat(50), rat(0), rat(0)]})
    r["scalar_inputs"] = si
    return r


def small_graphs():
    out = []
    # CAND-01
    r = base_record("P01/CAND-01", "GRAPH", ["CAND-01"])
    r["vertices"] = [vertex("U", 0, None, cands=[
        cand("c0", 1, 1, "FIT", [100, 0, 0, 0]),
        cand("c1", 2, 0, "FIT", [0, 100, 0, 0])])]
    out.append(r)
    out.append(graph("P01/CAV-01", ["CAV-01"],
                     [("U", -1, [0, 100, 0, 0]), ("V", 0, [100, 0, 0, 0])], [("U", "V")]))
    k = [("K1", None, [100, 0, 0, 0]), ("K2", None, [0, 100, 0, 0]), ("K3", None, [50, 50, 0, 0])]
    for fid, ids in (("P01/EQUAL-3-C", (0, -2, -1)), ("P01/EQUAL-3-T", (-2, 0, -1))):
        spec = [(l, ids[i], c) for i, (l, _, c) in enumerate(k)]
        out.append(graph(fid, ["K1", "K2", "K3", "CAV-01"], spec,
                         [("K1", "K2"), ("K1", "K3"), ("K2", "K3")]))
    out.append(graph("P01/NEAR-01", ["K4", "K3"],
                     [("P", 1, [Fraction(50001, 1000), 50, 0, 0]), ("Q", 0, [50, 50, 0, 0])], [("P", "Q")]))
    big = Fraction(50 * 2 ** 80 + 1, 2 ** 80)
    out.append(graph("P01/BIG-NEAR-01", ["K4"],
                     [("P", 1, [big, 50, 0, 0]), ("Q", 0, [50, 50, 0, 0])], [("P", "Q")]))
    abcd = [("a", -2), ("b", -1), ("c", 2), ("d", 10)]
    out.append(graph("P01/ID-ORDER-01", ["ID-01", "CAV-01"],
                     [(l, x, [100, 0, 0, 0]) for l, x in abcd],
                     list(itertools.combinations("abcd", 2))))
    chain = [("A", 0, [100, 0, 0, 0]), ("B", 1, [100, 50, 0, 0]), ("C", 2, [100, 100, 0, 0])]
    out.append(graph("P01/CHAIN-3", ["plan-8.2"], chain, [("A", "B"), ("B", "C")]))
    chain_id = [("A", 0, chain[0][2]), ("B", -1, chain[1][2]), ("C", 1, chain[2][2])]
    out.append(graph("P01/CHAIN-ID-01", ["CHAIN-ID-01"], chain_id, [("A", "B"), ("B", "C")]))
    out.append(graph("P01/CHAIN-PROT-01", ["CHAIN-PROT-01"], chain_id, [("A", "B"), ("B", "C")],
                     protected=[{"label": "P", "id": ident(-2), "status": "RETIRED_PROTECTED",
                                 "conflicts_with": ["C"]}]))
    out.append(graph("P01/CYCLE-01", ["CYCLE-01"],
                     [(str(i), i, G(10 * (i + 1))) for i in range(4)],
                     [("0", "1"), ("1", "2"), ("2", "3"), ("3", "0")]))
    out.append(roll_graph("P01/ROLL-01", False))
    out.append(roll_graph("P01/NOFIT-01", True))
    return out


ROLL_UNITS = [  # label, id, tuple, fit slot, roll
    ("a", 0, [100, 100, 0, 0], 8, "WIN"),
    ("b", 1, [100, 0, 0, 0], 1, "WIN"),
    ("c", 2, [50, 0, 0, 0], 3, "LOSS"),
]


def roll_graph(fid, with_d):
    r = base_record(fid, "GRAPH", ["ROLL-01", "plan-8.4"] if not with_d else ["ROLL-01", "FIX-01-A"])
    vs = []
    for (l, x, t, slot, roll) in ROLL_UNITS:
        cs = [cand("s%d" % k, k, k, "FIT" if k == slot else "HARD_REJECT",
                   t if k == slot else None) for k in range(1, 9)]
        vs.append(vertex(l, x, None, roll=roll, cands=cs))
    if with_d:
        cs = [cand("s%d" % k, k, k, "HARD_REJECT", None) for k in range(1, 9)]
        vs.append(vertex("d", 3, None, roll="NOT_ROLLED", cands=cs))
    r["vertices"] = vs
    r["edges"] = norm_edges(vs, [("a", "b")])
    return r


def late_record():
    r = base_record("P01/LATE-01", "LATE", ["CHAIN-LONG-n", "A02-Q07"])
    r["population_complete"] = False
    vs = [vertex("A", 0, [100, 0, 0, 0]), vertex("B", -1, [100, 50, 0, 0]),
          vertex("C", 1, [100, 100, 0, 0]), vertex("D", 2, [100, 100, 100, 100])]
    snaps = []
    for stage, cert, pairs, unk, comp in (
            ("OPEN", "OPEN", [("A", "B"), ("B", "C")], [("C", "D")], False),
            ("REVEAL", "COMPLETE", [("A", "B"), ("B", "C"), ("C", "D")], [], True)):
        snaps.append({"stage": stage, "certificate": cert, "vertices": vs,
                      "edges": norm_edges(vs, pairs),
                      "unknown_edges": norm_edges(vs, unk), "population_complete": comp})
    r["stages"] = snaps
    return r


def long_record(kind, n, ext):
    m = n + 1 + (1 if ext else 0)
    fid = "P01/LONG-%s-%d/%s" % (kind, n, "EXT" if ext else "BASE")
    r = base_record(fid, "LONG", ["CHAIN-LONG-n"])
    vs = []
    for i in range(m):
        if kind == "SCORE":
            # existing tuples keep their BASE denominator n+1; the EXT endpoint is G(100)
            s = Fraction(100 * i, n + 1) if i <= n else Fraction(100)
            vs.append(vertex(str(i), i, G(s)))
        else:
            vs.append(vertex(str(i), -i, [100, 0, 0, 0]))
    r["vertices"] = vs
    r["edges"] = norm_edges(vs, [(str(i), str(i + 1)) for i in range(m - 1)])
    return r


# ---------------------------------------------------------------- accounting logs
def ev(idx, kind, unit, uid, payload, fid):
    if unit is None:
        eid = "%s|population-1|CONFLICT_DECISION" % fid
        idv = None
    else:
        idv = ident(uid)
        eid = "|".join([fid, PREFIX["schema"], PREFIX["seed"], PREFIX["dimension"],
                        str(uid), "0", "0", "decision-1", kind])
    return {"index": str(idx), "kind": kind, "unit": unit, "id": idv, "event_id": eid, "payload": payload}


def roll_units(with_d):
    us = [dict(label=l, id=x, tup=t, slot=s, roll=ro) for (l, x, t, s, ro) in ROLL_UNITS]
    return us


def fit_payload(u):
    slots = [{"slot": str(k), "seed_rank": str(k), "status": "FIT" if k == u["slot"] else "HARD_REJECT"}
             for k in range(1, 9)]
    return {"slots": slots, "selected_slot": str(u["slot"]), "fit_eligible": True,
            "components": [rat(c) for c in u["tup"]], "roll": None}


def base_log(fid, variant):
    """Return the ordered event list for the accounting variants."""
    units = roll_units(False)
    ids = {u["label"]: u["id"] for u in units}
    evs = []

    def add(kind, unit, payload):
        evs.append((kind, unit, payload))

    for u in units:
        add("QUALIFIED", u["label"], {"value": True})
    if variant == "NOFIT":
        add("QUALIFIED", "d", {"value": True})
        ids["d"] = 3
    for u in units:
        add("FIT_PLAN_COMPLETE", u["label"], fit_payload(u))
    if variant == "NOFIT":
        add("FIT_PLAN_COMPLETE", "d", {
            "slots": [{"slot": str(k), "seed_rank": str(k), "status": "HARD_REJECT"} for k in range(1, 9)],
            "selected_slot": None, "fit_eligible": False, "components": None, "roll": "NOT_ROLLED"})
    fit_idx = {}
    for i, (k, u, p) in enumerate(evs, 1):
        if k == "FIT_PLAN_COMPLETE":
            fit_idx[u] = str(i)
    for u in units:
        add("ORIGINAL_ROLL", u["label"], {"result": u["roll"], "fit_ref": fit_idx[u["label"]]})
    cd_idx = len(evs) + 1
    add("CONFLICT_DECISION", None, {
        "cohort": ["a", "b", "c"], "winners": ["a", "b"], "edges": [{"u": "a", "v": "b"}],
        "trace": [{"v": "a", "d": "ACCEPT", "witness": []},
                  {"v": "b", "d": "REJECT_SPACING", "witness": ["a"]}]})
    add("ACCEPTED_PENDING", "a", {"ref": str(cd_idx)})
    add("SPACING_EXCLUDED", "b", {"ref": str(cd_idx)})
    if variant == "INVALID":
        add("CERTIFICATE_INVALID", "a", {})
    elif variant == "RETIRE":
        for k in ("APPLYING", "UNCERTAIN", "QUARANTINED", "RETIRED_PROTECTED"):
            add(k, "a", {})
    elif variant in ("MODEL-SUCCESS", "MODEL-EDIT"):
        add("MODEL_PUBLICATION_RECEIPT", "a", {"complete": True, "effect": True, "durable": True})
        if variant == "MODEL-EDIT":
            add("MODEL_PLAYER_MODIFIED", "a", {})
    return [ev(i, k, u, ids.get(u), p, fid) for i, (k, u, p) in enumerate(evs, 1)]


def protected_log(fid):
    ids = {"B": -1, "A": 0, "C": 1}
    tup = {"A": [100, 0, 0, 0], "B": [100, 50, 0, 0], "C": [100, 100, 0, 0]}
    order = ["B", "A", "C"]
    evs = []
    for u in order:
        evs.append(("QUALIFIED", u, {"value": True}))
    for u in order:
        evs.append(("FIT_PLAN_COMPLETE", u, {
            "slots": [{"slot": "1", "seed_rank": "0", "status": "FIT"}], "selected_slot": "1",
            "fit_eligible": True, "components": [rat(c) for c in tup[u]], "roll": None}))
    for u in order:
        evs.append(("ORIGINAL_ROLL", u, {"result": "WIN", "fit_ref": str(3 + order.index(u) + 1)}))
    evs.append(("CONFLICT_DECISION", None, {
        "cohort": ["A", "B", "C"], "winners": ["A", "B", "C"],
        "edges": [{"u": "B", "v": "A"}, {"u": "C", "v": "B"}],
        "protected_conflicts": [{"site": "P", "with": ["C"]}],
        "trace": [{"v": "C", "d": "REJECT_PROTECTED", "witness": ["P"]},
                  {"v": "B", "d": "ACCEPT", "witness": []},
                  {"v": "A", "d": "REJECT_SPACING", "witness": ["B"]}]}))
    evs.append(("ACCEPTED_PENDING", "B", {"ref": "10"}))
    evs.append(("SPACING_EXCLUDED", "A", {"ref": "10"}))
    evs.append(("PROTECTION_EXCLUDED", "C", {"ref": "10"}))
    return [ev(i, k, u, ids.get(u), p, fid) for i, (k, u, p) in enumerate(evs, 1)]


def acct_record(fid, variant, refs):
    r = base_record(fid, "ACCT", refs)
    if variant == "PROTECTED":
        r["events"] = protected_log(fid)
        r["protected_sites"] = [{"label": "P", "id": ident(-2), "status": "RETIRED_PROTECTED",
                                 "conflicts_with": ["C"]}]
    else:
        r["events"] = base_log(fid, variant)
    return r


def neg_contradict():
    fid = "P01/NEG-CONTRADICT-01"
    r = base_record(fid, "NEG", ["ROLL-01", "plan-8.4", "A02-Q06"])
    evs = base_log(fid, "BASE")
    evs.append(ev(13, "ORIGINAL_ROLL", "c", 2, {"result": "WIN", "fit_ref": "6"}, fid))
    r["events"] = evs
    return r


def neg_roll_first():
    fid = "P01/NEG-ROLL-FIRST-01"
    r = base_record(fid, "NEG", ["ROLL-01", "plan-8.4"])
    full = base_log(fid, "BASE")  # indices: Q 1-3, F 4-6, R 7-9
    by = {(e["kind"], e["unit"]): e for e in full}
    seq = [by[("QUALIFIED", "a")], by[("QUALIFIED", "b")], by[("QUALIFIED", "c")],
           dict(by[("ORIGINAL_ROLL", "a")]),  # offending early roll, placed before FIT(a)
           by[("FIT_PLAN_COMPLETE", "a")], by[("FIT_PLAN_COMPLETE", "b")], by[("FIT_PLAN_COMPLETE", "c")],
           by[("ORIGINAL_ROLL", "b")], by[("ORIGINAL_ROLL", "c")]]
    out = []
    for i, e in enumerate(seq, 1):
        e = json.loads(json.dumps(e))
        e["index"] = str(i)
        if e["kind"] == "FIT_PLAN_COMPLETE":
            pass
        out.append(e)
    # fix fit_ref of the rolls to the new FIT record indices
    fit = {e["unit"]: e["index"] for e in out if e["kind"] == "FIT_PLAN_COMPLETE"}
    for e in out:
        if e["kind"] == "ORIGINAL_ROLL":
            e["payload"]["fit_ref"] = fit[e["unit"]]
    r["events"] = out
    return r


def derived_record(fid, base, mode, refs):
    r = json.loads(json.dumps(base))
    r["fixture_id"] = fid
    r["expected_ref"] = fid
    r["family"] = "ACCT"
    r["source_refs"] = refs
    r["derived"] = {"base_ref": base["fixture_id"], "mode": mode}
    # event ids belong to the base decision; keep them (the same original decision identity)
    return r


def all_records():
    recs = [arith_record(n) for n in range(12)]
    recs.append(fixed_record())
    recs += small_graphs()
    recs.append(late_record())
    for kind in ("SCORE", "TIE"):
        for n in LONG_NS:
            recs.append(long_record(kind, n, False))
            recs.append(long_record(kind, n, True))
    acct = {}
    for fid, var, refs in (
            ("P01/ACCT-BASE", "BASE", ["ROLL-01", "plan-8.4"]),
            ("P01/ACCT-INVALID", "INVALID", ["LIFE-02"]),
            ("P01/ACCT-RETIRE", "RETIRE", ["LIFE-03"]),
            ("P01/ACCT-MODEL-SUCCESS", "MODEL-SUCCESS", ["ROLL-01", "LIFE-01"]),
            ("P01/ACCT-MODEL-EDIT", "MODEL-EDIT", ["LIFE-04"]),
            ("P01/ACCT-PROTECTED", "PROTECTED", ["CHAIN-PROT-01"]),
            ("P01/NOFIT-01/LOG", "NOFIT", ["ROLL-01", "FIX-01-A"])):
        r = acct_record(fid, var, refs)
        acct[fid] = r
        recs.append(r)
    recs.append(neg_contradict())
    recs.append(neg_roll_first())
    recs.append(derived_record("P01/ACCT-DUP-LOG", acct["P01/ACCT-BASE"], "DUP_LOG", ["A02-Q06", "plan-8.4"]))
    recs.append(derived_record("P01/ACCT-DUP-RECEIPT", acct["P01/ACCT-MODEL-SUCCESS"], "DUP_RECEIPT", ["A02-Q06"]))
    for bname, bfid in (("BASE", "P01/ACCT-BASE"), ("MODEL-SUCCESS", "P01/ACCT-MODEL-SUCCESS")):
        for mode in ("PROJECTION", "SOURCE"):
            recs.append(derived_record("P01/ACCT-LOSS-%s-%s" % (bname, mode), acct[bfid],
                                       "LOSS_" + mode, ["plan-missing-ledger"]))
    return recs


# ---------------------------------------------------------------- schedules
def sched(no, fid, tag, **kw):
    s = {"schedule_no": str(no), "schedule_id": "S-%s-%s-%d" % (fid, tag, no), "tag": tag,
         "batch": None, "vertex_order": [], "edge_order": [], "protected_order": [],
         "candidate_orders": {}, "stage_orders": [], "steps": [], "cut": None}
    s.update(kw)
    return s


def by_id(verts):
    return [v["label"] for v in sorted(verts, key=idnum)]


def edge_orders(edges):
    if len(edges) > 1:
        return [("CANON", edges), ("REVERSE", edges[::-1])]
    return [("CANON", edges)]


def graph_scheds(r):
    fid = r["fixture_id"]
    labels = by_id(r["vertices"])
    eos = edge_orders(r["edges"])
    prot = [p["label"] for p in sorted(r["protected_sites"], key=idnum)]
    pos = [prot] if len(prot) <= 1 else [prot, prot[::-1]]
    out = []
    for p in itertools.permutations(range(len(labels))):
        for en, eo in eos:
            for po in pos:
                out.append(sched(len(out) + 1, fid, "PERM", vertex_order=[labels[i] for i in p],
                                 edge_order=eo, protected_order=po))
    return out


def long_scheds(r):
    fid = r["fixture_id"]
    m = len(r["vertices"])
    inc = [str(i) for i in range(m)]
    dec = inc[::-1]
    evod = [str(i) for i in range(0, m, 2)] + [str(i) for i in range(1, m, 2)]
    alt, lo, hi, t = [], 0, m - 1, True
    while lo <= hi:
        if t:
            alt.append(str(lo)); lo += 1
        else:
            alt.append(str(hi)); hi -= 1
        t = not t
    seqs = []
    for name, s in (("INC", inc), ("DEC", dec), ("EVOD", evod), ("ALT", alt)):
        if s not in [x[1] for x in seqs]:
            seqs.append((name, s))
    out = []
    for name, s in seqs:
        for en, eo in edge_orders(r["edges"]):
            out.append(sched(len(out) + 1, fid, "%s-%s" % (name, en), vertex_order=s, edge_order=eo))
    return out


def late_scheds(r):
    fid = r["fixture_id"]
    combos = []
    for st in r["stages"]:
        labels = by_id(st["vertices"])
        c = []
        for p in itertools.permutations(range(len(labels))):
            for en, eo in edge_orders(st["edges"]):
                c.append((st["stage"], [labels[i] for i in p], eo))
        combos.append(c)
    out = []
    for j in range(len(combos[0])):
        so = [{"stage": s, "vertex_order": vo, "edge_order": eo} for (s, vo, eo) in (combos[0][j], combos[1][j])]
        out.append(sched(j + 1, fid, "STAGES", stage_orders=so))
    return out


def cand_scheds(r):
    fid = r["fixture_id"]
    return [sched(1, fid, "CAND", vertex_order=["U"], candidate_orders={"U": ["c0", "c1"]}),
            sched(2, fid, "CAND", vertex_order=["U"], candidate_orders={"U": ["c1", "c0"]})]


def orderings(events, unit_ids, illegal=None):
    """Legal delivery orderings of an event log: unit-block permutations then PHASE-ABC/CBA.
    unit_ids: unit labels in canonical-ID order. Returns [(tag, [event index strings])]."""
    unit_kinds = ("QUALIFIED", "FIT_PLAN_COMPLETE", "ORIGINAL_ROLL")
    per = {u: {} for u in unit_ids}
    tail = []
    for e in events:
        if e["kind"] in unit_kinds and e["unit"] in per and (e["kind"], e["index"]) not in (illegal or set()):
            per[e["unit"]][e["kind"]] = e["index"]
        elif e["kind"] in unit_kinds and e["unit"] in per and illegal and (e["kind"], e["index"]) in illegal:
            continue
        else:
            tail.append(e["index"])
    res = []
    for p in itertools.permutations(range(len(unit_ids))):
        seq = []
        for i in p:
            u = unit_ids[i]
            seq += [per[u][k] for k in unit_kinds if k in per[u]]
        res.append(("BLOCK-" + "".join(str(i) for i in p), seq + tail))
    for tag, order in (("PHASE-ABC", unit_ids), ("PHASE-CBA", unit_ids[::-1])):
        seq = []
        for k in unit_kinds:
            seq += [per[u][k] for u in order if k in per[u]]
        res.append((tag, seq + tail))
    return res


def acct_scheds(r):
    fid = r["fixture_id"]
    events = r["events"]
    units = sorted({e["unit"]: e["id"] for e in events if e["unit"]}.items(), key=lambda kv: int(kv[1]["x"]))
    unit_ids = [u for u, _ in units]
    ords = orderings(events, unit_ids)
    out = []
    for tag, seq in ords:
        for w in (("worker0", "worker1"), ("worker1", "worker0")):
            steps = []
            for e in seq:
                steps += [{"event": e, "origin": w[0]}, {"event": e, "origin": w[1]}]
            out.append(sched(len(out) + 1, fid, "DELIVERY", steps=steps,
                             vertex_order=[], protected_order=[],
                             candidate_orders={}, batch=None, cut=None, edge_order=[]))
            out[-1]["schedule_id"] = "S-%s-DELIVERY-%s-%s-%d" % (fid, tag, "w0w1" if w[0] == "worker0" else "w1w0", len(out))
    for tag, seq in ords:
        for k in range(len(seq) + 1):
            out.append(sched(len(out) + 1, fid, "REPLAY", steps=[{"event": e, "origin": "none"} for e in seq],
                             cut=str(k)))
            out[-1]["schedule_id"] = "S-%s-REPLAY-%s-cut%d-%d" % (fid, tag, k, len(out))
    return out


def neg_scheds(r, kind):
    fid = r["fixture_id"]
    events = r["events"]
    out = []
    if kind == "CONTRADICT":
        legal = [e for e in events if e["index"] != "13"]
        unit_ids = ["a", "b", "c"]
        for tag, seq in orderings(legal, unit_ids):
            out.append(sched(len(out) + 1, fid, "NEG-" + tag,
                             steps=[{"event": e, "origin": "none"} for e in seq + ["13"]]))
    else:
        # legal set: everything except the early roll of a (index 4); the early roll is inserted before FIT(a)
        legal = [e for e in events if e["index"] != "4"]
        fit_a = [e["index"] for e in events if e["kind"] == "FIT_PLAN_COMPLETE" and e["unit"] == "a"][0]
        for tag, seq in orderings(legal, ["a", "b", "c"]):
            seq = list(seq)
            seq.insert(seq.index(fit_a), "4")
            out.append(sched(len(out) + 1, fid, "NEG-" + tag,
                             steps=[{"event": e, "origin": "none"} for e in seq]))
    return out


def derived_scheds(r):
    fid = r["fixture_id"]
    mode = r["derived"]["mode"]
    evs = r["events"]
    idxs = [e["index"] for e in evs]
    kinds = {e["index"]: e["kind"] for e in evs}
    if mode == "DUP_LOG":
        seq = idxs + idxs
    elif mode == "DUP_RECEIPT":
        rec = [i for i in idxs if kinds[i] == "MODEL_PUBLICATION_RECEIPT"]
        seq = idxs + rec
    elif mode == "LOSS_PROJECTION":
        seq = [i for i in idxs if kinds[i] not in ("ACCEPTED_PENDING", "SPACING_EXCLUDED")]
    else:  # LOSS_SOURCE: projections, decision and receipt evidence removed
        seq = [i for i in idxs if kinds[i] not in
               ("ACCEPTED_PENDING", "SPACING_EXCLUDED", "CONFLICT_DECISION", "MODEL_PUBLICATION_RECEIPT")]
    return [sched(1, fid, mode, steps=[{"event": e, "origin": "none"} for e in seq])]


def all_schedules(recs):
    fams = []
    for case_no, r in enumerate(recs, 1):
        fid, fam = r["fixture_id"], r["family"]
        if fam == "ARITH":
            if fid == "P01/ARITH-V0":
                ss = [sched(1, fid, "A-LIST", batch={"kind": "CASE_NOS", "items": [str(i) for i in range(1, 13)]}),
                      sched(2, fid, "A-REV", batch={"kind": "CASE_NOS", "items": [str(i) for i in range(12, 0, -1)]})]
            elif fid == "P01/FIXED-01":
                ss = [sched(i + 1, fid, "FIXED", batch={"kind": "SCALAR_LABELS", "items": items})
                      for i, items in enumerate((["V2"], ["V0", "V2", "V1"], ["K4", "V2"]))]
            else:
                continue
        elif fid == "P01/CAND-01":
            ss = cand_scheds(r)
        elif fam == "GRAPH":
            ss = graph_scheds(r)
        elif fam == "LONG":
            ss = long_scheds(r)
        elif fam == "LATE":
            ss = late_scheds(r)
        elif fam == "NEG":
            ss = neg_scheds(r, "CONTRADICT" if "CONTRADICT" in fid else "ROLLFIRST")
        elif r["derived"]:
            ss = derived_scheds(r)
        else:
            ss = acct_scheds(r)
        fams.append({"case_no": str(case_no), "fixture_id": fid, "tag": fam, "count": str(len(ss)),
                     "schedules": ss, "list_sha256": sha(dump(ss))})
    return {"schema": "P01-SCHEDULES-1", "total": str(sum(len(f["schedules"]) for f in fams)), "families": fams}


# ---------------------------------------------------------------- controls
A = "ASR-"
DEF_MP = [A + "ACCEPT", A + "COMPLETE", A + "INPUT-UNCHANGED"]


def build_controls(recs, scheds):
    case = {r["fixture_id"]: i for i, r in enumerate(recs, 1)}
    fam = {f["fixture_id"]: f for f in scheds["families"]}

    def allof(fid, tags=None):
        f = fam[fid]
        ns = [s["schedule_no"] for s in f["schedules"] if tags is None or s["tag"] in tags]
        return {"case_no": str(case[fid]), "schedule_nos": ns}

    def tg(*fids, tags=None):
        return [allof(f, tags) for f in fids]

    long_ext = ["P01/LONG-%s-%d/EXT" % (k, n) for k in ("SCORE", "TIE") for n in LONG_NS]
    long_tie = ["P01/LONG-TIE-%d/%s" % (n, b) for n in LONG_NS for b in ("BASE", "EXT")]
    long_all = ["P01/LONG-%s-%d/%s" % (k, n, b) for k in ("SCORE", "TIE") for n in LONG_NS for b in ("BASE", "EXT")]

    def arrival_detect():
        res = []
        for fid in long_all:
            f = fam[fid]
            m = len(recs[case[fid] - 1]["vertices"])
            dec = [str(i) for i in range(m - 1, -1, -1)]
            res.append({"case_no": str(case[fid]),
                        "schedule_nos": [s["schedule_no"] for s in f["schedules"] if s["vertex_order"] != dec]})
        return res

    arith_all = [{"case_no": "1", "schedule_nos": ["1", "2"]}, {"case_no": str(case["P01/FIXED-01"]), "schedule_nos": ["1", "2", "3"]}]
    rows = []

    def C(cid, text, ident_, mf, mp, targets, detect=None, ov=None, oc=None, prov=False):
        rows.append({
            "id": cid, "text": text, "identification": ident_, "trace_level": "SUMMARY",
            "must_fail": [A + x for x in mf], "must_pass": list(dict.fromkeys(DEF_MP + [A + x for x in mp])),
            "must_fail_provisional": prov, "outcome_classes": oc or [],
            "targets": targets,
            "detect_on": detect if detect else {"mode": "ALL_SCHEDULES", "schedules": []},
            "control_overrides": ov or {},
            "sessions": str(sum(len(t["schedule_nos"]) for t in targets)),
        })

    SIG, OCL = "SIGNATURE", "OUTCOME_CLASS"
    C("N-INNER-WEIGHT", "Swaps the L/H inner coefficients of C", SIG, ["ARITH-COMP", "ARITH-TOTAL"], [], [arith_all[0]])
    C("N-EARLY-FLOOR", "Floors C before the outer weighting", SIG, ["ARITH-COMP", "ARITH-TOTAL"], [], arith_all)
    C("N-RELATIVE", "Renormalizes S across the batch minimum and maximum", SIG, ["ARITH-TOTAL"], [], [arith_all[1]])
    C("N-CAND-COMPONENT", "Selects the candidate by component (C) first", SIG, ["CAND-ORDER", "CAND-SELECTED"], ["FINAL-SETS"], tg("P01/CAND-01"))
    c1 = allof("P01/CAND-01")
    c1_first = [s["schedule_no"] for s in fam["P01/CAND-01"]["schedules"] if s["candidate_orders"]["U"] == ["c0", "c1"]]
    C("N-CAND-ARRIVAL", "Selects the candidate by arrival order", SIG, ["CAND-ORDER", "CAND-SELECTED"], ["FINAL-SETS"],
      [c1], detect={"mode": "EXPLICIT", "schedules": [{"case_no": c1["case_no"], "schedule_nos": c1_first}]})
    C("N-CROSS-C-FIRST", "Orders cavern winners by component C first", SIG, ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"], tg("P01/CAV-01"))
    C("N-CROSS-SEED", "Orders cavern winners by candidate seed order", SIG, ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"],
      tg("P01/CAV-01"), ov={"cross_candidate_seed_order": ["V", "U"]})
    C("N-C-FIRST", "Orders equal totals by component C descending", SIG, ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"], tg("P01/EQUAL-3-C"))
    C("N-T-FIRST", "Orders equal totals by component T descending", SIG, ["ORDER", "DECISION", "FINAL-SETS"], ["ARITH-TOTAL"], tg("P01/EQUAL-3-T"))
    C("N-ROUND-2", "Rounds totals before comparing", SIG, ["ARITH-TOTAL", "ORDER", "FINAL-SETS"], [], tg("P01/NEAR-01", "P01/BIG-NEAR-01"))
    C("N-FLOAT64", "Uses binary64 arithmetic", SIG, ["ARITH-COMP", "FINAL-SETS"], [], tg("P01/BIG-NEAR-01"))
    C("N-FIXED64", "Uses 64-bit integer rationals (UA / WR / OK classes)", OCL, [], [], tg("P01/BIG-NEAR-01"), oc=["UA", "WR"])
    C("N-ID-STRING", "Compares canonical IDs as decimal strings", SIG, ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"], tg("P01/ID-ORDER-01"))
    C("N-REJECTED-VETO", "A rejected neighbour blocks later vertices", SIG, ["DECISION", "WITNESS", "FINAL-SETS"], ["ORDER", "ARITH-TOTAL"],
      tg("P01/CHAIN-3", "P01/CHAIN-ID-01", "P01/CHAIN-PROT-01", "P01/CYCLE-01"))
    C("N-ID-FIRST", "Orders by canonical ID before score", SIG, ["ORDER"], ["ARITH-TOTAL"], tg("P01/CHAIN-3", "P01/CHAIN-ID-01", "P01/CYCLE-01"))
    C("N-PROTECTION-IGNORE", "Ignores protected-site conflicts", SIG, ["DECISION", "FINAL-SETS"], ["ORDER"], tg("P01/CHAIN-PROT-01"))
    C("N-OMIT-ENDPOINT", "Drops the late extension endpoint", SIG, ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"], tg(*long_ext))
    C("N-LOCAL-CAP", "Assumes the first n+1 listed vertices suffice", SIG, ["ORDER", "FINAL-SETS"], ["ARITH-TOTAL"], tg(*long_ext))
    C("N-TIE-ID-DESC", "Reverses canonical-ID order on equal totals", SIG, ["ORDER"], ["ARITH-TOTAL"], tg(*long_tie))
    C("N-ARRIVAL", "Processes vertices in arrival order", SIG, ["ORDER"], ["ARITH-TOTAL"], tg(*long_all),
      detect={"mode": "EXPLICIT", "schedules": arrival_detect()})
    C("N-UNKNOWN-EMPTY", "Treats an UNKNOWN edge as absent", SIG, ["LATE", "FINAL-SETS"], [], tg("P01/LATE-01"))
    C("N-QUALIFIED-DENOM", "Counts qualified-but-unfit units in the roll denominator", SIG, ["COUNTERS", "STATUS"], ["ARITH-TOTAL"],
      [allof("P01/NOFIT-01")] + [allof("P01/NOFIT-01/LOG", {"DELIVERY"})])
    C("N-NINTH", "Fabricates a ninth fit check (counter excess)", OCL, ["COUNTERS", "STATUS"], ["ARITH-TOTAL"],
      [allof("P01/NOFIT-01")] + [allof("P01/NOFIT-01/LOG", {"DELIVERY"})], oc=["COUNTER_EXCESS"])
    C("N-ROLL-FIRST", "Records a roll before the fit checks complete", SIG, ["STATUS", "REPLAY"], [], tg("P01/NEG-ROLL-FIRST-01"))
    for cid, fid, text, mf in (
            ("N-ACCEPT-IS-COMPLETE", "P01/ACCT-BASE", "Reports an accepted decision as physically complete", ["NOPHYS", "COUNTERS"]),
            ("N-POSTROLL-NOFIT", "P01/ACCT-INVALID", "Removes an invalidated unit from the received-roll denominator", ["COUNTERS", "STATUS"]),
            ("N-REROLL", "P01/ACCT-RETIRE", "Rerolls or re-epochs a retired unit", ["NOEXTRA", "COUNTERS"]),
            ("N-IGNORE-P", "P01/ACCT-PROTECTED", "Ignores the protected site in accounting", ["ACCT-SETS", "COUNTERS"])):
        C(cid, text, SIG, mf, ["COMPLETE"], [allof(fid, {"DELIVERY"})], prov=True)
    C("N-LOGICAL-IS-PHYSICAL", "Reports modeled success as physical completion", SIG, ["NOPHYS", "COUNTERS"], ["COMPLETE"],
      [allof("P01/ACCT-MODEL-SUCCESS", {"DELIVERY"})], prov=True)
    C("N-ERASE-HISTORY", "Erases modeled history after a modeled player edit", OCL, ["ACCT-SETS", "COUNTERS"], ["COMPLETE"],
      [allof("P01/ACCT-MODEL-EDIT", {"DELIVERY"})], oc=["MODELED_SUCCESS_ZERO", "RETIRED_FOR_EDIT"], prov=True)
    C("N-ADDITIVE", "Counts duplicate deliveries additively", SIG, ["EVENT-IDEM", "COUNTERS"], [], tg("P01/ACCT-DUP-LOG", "P01/ACCT-DUP-RECEIPT"))
    replay = [allof(f, {"REPLAY"}) for f in ("P01/ACCT-BASE", "P01/ACCT-INVALID", "P01/ACCT-RETIRE", "P01/ACCT-MODEL-SUCCESS",
                                              "P01/ACCT-MODEL-EDIT", "P01/ACCT-PROTECTED", "P01/NOFIT-01/LOG")]
    C("N-NEW-EPOCH", "Creates a new decision epoch on replay", OCL, ["REPLAY", "NOEXTRA"], [], replay, oc=["SECOND_DECISION"])
    C("N-MISSING-MEANS-EMPTY", "Treats missing source evidence as an unrolled unit", OCL, ["REPLAY", "NOEXTRA"], [],
      tg("P01/ACCT-LOSS-BASE-PROJECTION", "P01/ACCT-LOSS-BASE-SOURCE", "P01/ACCT-LOSS-MODEL-SUCCESS-PROJECTION",
         "P01/ACCT-LOSS-MODEL-SUCCESS-SOURCE"), oc=["REROLLED_AS_UNROLLED"])
    return {"schema": "P01-CONTROLS-1", "total_sessions": str(sum(int(r["sessions"]) for r in rows)),
            "control_count": str(len(rows)), "controls": rows}


# ---------------------------------------------------------------- invalid inputs
def invalid_files():
    base = [r for r in all_records() if r["fixture_id"] == "P01/CAV-01"][0]

    def mut(fn):
        r = json.loads(json.dumps(base))
        fn(r)
        return dump(r)

    U = lambda r: r["vertices"][0]
    V = lambda r: r["vertices"][1]
    files = {}

    def edit_text(old, new):
        t = dump(base).decode("utf-8")
        assert t.count(old) == 1, old
        return t.replace(old, new).encode("utf-8")

    files["JSON-SYNTAX"] = edit_text('"fixture_id": "P01/CAV-01"', '"fixture_id": "P01/CAV-01')
    files["DUP-KEY"] = edit_text('  "fixture_id": "P01/CAV-01",\n', '  "fixture_id": "P01/CAV-01",\n  "fixture_id": "P01/CAV-01",\n')
    # NONCANON: same content, top-level keys in descending order (nested objects canonical)
    keys = sorted(base, key=lambda s: s.encode())[::-1]
    files["NONCANON"] = ("{\n" + ",\n".join('  ' + json.dumps(k) + ": " + cj(base[k], 1) for k in keys) + "\n}\n").encode()
    t0 = dump(base).decode()
    first_slot = '"slot": "1"'
    i = t0.index(first_slot)
    files["NUMBER-TOKEN"] = (t0[:i] + '"slot": 1' + t0[i + len(first_slot):]).encode()
    files["INT-FORMAT"] = edit_text('"x": "-1"', '"x": "-01"')
    files["INT-BITS"] = mut(lambda r: V(r)["id"].__setitem__("y", str(2 ** 4096)))
    files["MISSING-KEY"] = mut(lambda r: U(r).pop("qualified"))
    files["EXTRA-KEY"] = mut(lambda r: U(r).__setitem__("note", "x"))
    files["ENUM-VALUE"] = mut(lambda r: V(r).__setitem__("roll", "MAYBE"))
    files["RAT-ZERO-DEN"] = mut(lambda r: U(r)["candidates"][0]["components"].__setitem__(0, {"num": "1", "den": "0"}))
    files["RAT-NEG-DEN"] = mut(lambda r: U(r)["candidates"][0]["components"].__setitem__(0, {"num": "1", "den": "-2"}))
    files["RAT-UNREDUCED"] = mut(lambda r: U(r)["candidates"][0]["components"].__setitem__(0, {"num": "2", "den": "4"}))
    files["SCORE-RANGE"] = mut(lambda r: U(r)["candidates"][0]["components"].__setitem__(0, {"num": "101", "den": "1"}))
    files["TUPLE-ARITY"] = mut(lambda r: U(r)["candidates"][0]["components"].pop())
    files["ID-DUPLICATE"] = mut(lambda r: V(r).__setitem__("id", dict(U(r)["id"])))
    files["ID-PREFIX-MIXED"] = mut(lambda r: V(r)["id"].__setitem__("dimension", "minecraft:the_nether"))

    def labdup(r):
        w = json.loads(json.dumps(U(r)))
        w["id"] = ident(1)
        r["vertices"].append(w)
    files["LABEL-DUPLICATE"] = mut(labdup)
    files["EDGE-SELF"] = mut(lambda r: r.__setitem__("edges", [{"u": "U", "v": "U"}]))
    files["EDGE-DANGLING"] = mut(lambda r: r.__setitem__("edges", [{"u": "U", "v": "W"}]))
    files["EDGE-DUPLICATE"] = mut(lambda r: r["edges"].append({"u": "U", "v": "V"}))

    def eu(r):
        r["unknown_edges"] = [{"u": "U", "v": "V"}]
        r["population_complete"] = False
    files["EDGE-UNKNOWN-CONFLICT"] = mut(eu)
    files["POP-FLAG"] = mut(lambda r: r.__setitem__("population_complete", False))

    def snm(r):
        vs = r["vertices"]
        r["stages"] = [
            {"stage": "OPEN", "certificate": "OPEN", "vertices": vs, "edges": r["edges"], "unknown_edges": [], "population_complete": True},
            {"stage": "REVEAL", "certificate": "COMPLETE", "vertices": vs, "edges": [], "unknown_edges": [], "population_complete": True}]
    files["STAGE-NOT-MONOTONE"] = mut(snm)

    def over(r):
        U(r)["candidates"] = [cand("c%d" % k, k, k - 1, "FIT", [0, 100, 0, 0]) for k in range(1, 10)]
    files["CAND-OVERFLOW"] = mut(over)
    files["CAND-NULL-MISMATCH"] = mut(lambda r: U(r)["candidates"][0].__setitem__("components", None))

    def rdup(r):
        U(r)["candidates"] = [cand("c0", 1, 0, "FIT", [0, 100, 0, 0]), cand("c1", 2, 0, "FIT", [100, 0, 0, 0])]
    files["CAND-RANK-DUP"] = mut(rdup)

    def sdup(r):
        U(r)["candidates"] = [cand("c0", 1, 0, "FIT", [0, 100, 0, 0]), cand("c1", 1, 1, "FIT", [100, 0, 0, 0])]
    files["CAND-SLOT-DUP"] = mut(sdup)
    files["ROLL-BEFORE-FIT-STATIC"] = mut(lambda r: U(r).__setitem__("fit_complete", False))

    def evbad(r):
        r["events"] = [{"index": "1", "kind": "QUALIFIED", "unit": "U", "id": ident(-1),
                        "event_id": "P01/CAV-01|1|7|minecraft:overworld|-1|0|0|QUALIFIED", "payload": {"value": True}}]
    files["EVENT-ID-FORMAT"] = mut(evbad)
    assert len(files) == 29
    order = sorted(files, key=lambda c: sha(("P01-INVALID-1|" + c).encode()))
    return [("I%03d" % (i + 1), c, files[c]) for i, c in enumerate(order)]


# ---------------------------------------------------------------- manifest
ALL_ASR = ["ACCEPT", "REJECT-CODE", "ARITH-COMP", "ARITH-TOTAL", "INPUT-UNCHANGED", "STATUS", "CAND-ORDER",
           "CAND-SELECTED", "ORDER", "DECISION", "WITNESS", "WITNESS-SET", "FINAL-SETS", "SCHED-INVARIANT",
           "COUNTERS", "ACCT-SETS", "EVENT-IDEM", "REPLAY", "NOEXTRA", "NOPHYS", "COMPLETE", "LATE"]
GROUP_NA = {
    "ARITH": "arithmetic-only case without graph evaluation",
    "GRAPH": "no accounting events in this family",
    "ACCT": "no graph evaluation in this family",
    "INVALID": "invalid-input case: only rejection code and completeness apply",
}


def required_for(fid, fam):
    ar = ["ACCEPT", "ARITH-COMP", "ARITH-TOTAL", "INPUT-UNCHANGED", "SCHED-INVARIANT", "NOEXTRA", "COMPLETE"]
    gr = ar + ["STATUS", "ORDER", "DECISION", "WITNESS", "WITNESS-SET", "FINAL-SETS"]
    if fam == "ARITH":
        return ar, "ARITH"
    if fam in ("GRAPH", "LONG", "LATE"):
        req = list(gr)
        if fid in ("P01/CAND-01", "P01/ROLL-01", "P01/NOFIT-01"):
            req += ["CAND-ORDER", "CAND-SELECTED"]
        if fid in ("P01/ROLL-01", "P01/NOFIT-01"):
            req += ["COUNTERS"]
        if fam == "LATE":
            req += ["LATE"]
        return req, "GRAPH"
    return ["ACCEPT", "INPUT-UNCHANGED", "STATUS", "COUNTERS", "ACCT-SETS", "EVENT-IDEM", "REPLAY", "NOEXTRA",
            "NOPHYS", "SCHED-INVARIANT", "COMPLETE"], "ACCT"


def assertion_list(fid, fam, invalid=False):
    if invalid:
        req, grp = ["REJECT-CODE", "COMPLETE"], "INVALID"
    else:
        req, grp = required_for(fid, fam)
    out = []
    for a in ALL_ASR:
        if a in req:
            out.append({"id": A + a, "mode": "REQUIRED", "na_reason": None})
        else:
            out.append({"id": A + a, "mode": "NA", "na_reason": GROUP_NA[grp]})
    return out


def build_manifest(recs, scheds, controls, inv):
    fam = {f["fixture_id"]: f for f in scheds["families"]}
    triples = []

    def add(subject, case_no, sno, level, batch, assertions):
        triples.append({
            "triple_id": "%s/%s/%s" % (subject, case_no, sno), "subject": subject, "case_no": case_no,
            "schedule_no": sno, "trace_level": level, "batch": batch,
            "trace_file": "subject/traces/%s/%s.%s.jsonl" % (case_no, sno, subject), "assertions": assertions})

    info = {}
    for i, r in enumerate(recs, 1):
        info[str(i)] = (r["fixture_id"], r["family"])
    for i, r in enumerate(recs, 1):
        f = fam.get(r["fixture_id"])
        if not f:
            continue
        al = assertion_list(r["fixture_id"], r["family"])
        for s in f["schedules"]:
            add("CLEAN", str(i), s["schedule_no"], "FULL", s["batch"], al)
    inval = assertion_list(None, None, True)
    for (name, code, _) in inv:
        add("CLEAN", name, "1", "FULL", None, inval)
    sched_batch = {(f["case_no"], s["schedule_no"]): s["batch"] for f in scheds["families"] for s in f["schedules"]}
    for c in controls["controls"]:
        for t in c["targets"]:
            fid, fm = info[t["case_no"]]
            al = assertion_list(fid, fm)
            for sn in t["schedule_nos"]:
                add(c["id"], t["case_no"], sn, "SUMMARY", sched_batch[(t["case_no"], sn)], al)
    return {"schema": "P01-EXPECTED-TRACES-1", "triple_count": str(len(triples)), "triples": triples}


def build_runplan(manifest):
    return {"schema": "P01-RUNPLAN-1", "triple_count": manifest["triple_count"],
            "entries": [{"case_no": t["case_no"], "schedule_no": t["schedule_no"], "subject": t["subject"],
                         "trace_level": t["trace_level"], "trace_file": t["trace_file"]} for t in manifest["triples"]]}


# ---------------------------------------------------------------- main
def main():
    out = os.path.dirname(os.path.abspath(__file__))
    if "--out" in sys.argv:
        out = os.path.abspath(sys.argv[sys.argv.index("--out") + 1])
    os.makedirs(os.path.join(out, "invalid-inputs-v1"), exist_ok=True)
    recs = all_records()
    scheds = all_schedules(recs)
    controls = build_controls(recs, scheds)
    inv = invalid_files()
    manifest = build_manifest(recs, scheds, controls, inv)
    runplan = build_runplan(manifest)
    w = {}
    w["inputs-v1.json"] = dump(recs)
    w["schedules-v1.json"] = dump(scheds)
    w["controls-v1.json"] = dump(controls)
    w["expected-traces-v1.json"] = dump(manifest)
    w["runplan-v1.json"] = dump(runplan)
    for name, code, data in inv:
        w["invalid-inputs-v1/%s.json" % name] = data
    idx = []
    for r in recs:
        idx.append("%s\t%s\n" % (r["fixture_id"], sha(dump(r))))
    for name, code, data in inv:
        idx.append("%s\t%s\n" % (name, sha(data)))
    w["fixture-index-v1.tsv"] = "".join(idx).encode()
    for name, data in w.items():
        with open(os.path.join(out, name), "wb") as fh:
            fh.write(data)
    lines = ["%s  %s\n" % (sha(open(os.path.join(out, n), "rb").read()), n) for n in sorted(w)]
    for n in ("materialize.py", "verify_materialization.py"):
        p = os.path.join(out, n)
        if os.path.exists(p):
            lines.append("%s  %s\n" % (sha(open(p, "rb").read()), n))
    with open(os.path.join(out, "CANDIDATE-DIGESTS.sha256"), "w") as fh:
        fh.write("".join(sorted(lines, key=lambda s: s.split("  ", 1)[1])))
    print("records", len(recs), "invalid", len(inv), "schedules", scheds["total"],
          "controls", controls["control_count"], "control_sessions", controls["total_sessions"],
          "triples", manifest["triple_count"], "index_lines", len(idx))


if __name__ == "__main__":
    main()
