"""Independent loader-rule engine used ONLY to cross-check invalid-input expectations.

Implements the single-code detection precedence of binding plan section 4.7 (revision 4)
from the plan text. It is not the G-5 loader and shares no code with any other tool.
"""
import json
import re
from fractions import Fraction
from math import gcd

INTRE = re.compile(r"^(0|-?[1-9][0-9]*)$")
REC_KEYS = {"record_schema", "fixture_id", "family", "source_refs", "scalar_inputs", "id_prefix", "vertices", "edges",
            "protected_sites", "unknown_edges", "population_complete", "stages", "events", "default_schedule",
            "control_overrides", "expect_input_rejection", "expected_ref", "derived"}
V_KEYS = {"label", "id", "qualified", "candidates", "fit_complete", "roll"}
C_KEYS = {"cand", "slot", "seed_rank", "status", "components"}
ID_KEYS = {"x", "y", "z"}
PREFIX_KEYS = {"schema", "seed", "dimension"}
SNAP_KEYS = {"stage", "certificate", "vertices", "edges", "unknown_edges", "population_complete"}
ROLLS = {"WIN", "LOSS", "NOT_ROLLED", "NOT_APPLICABLE"}
STATUS = {"FIT", "HARD_REJECT"}
EVRE = re.compile(r"^[^|]+\|[^|]+\|[^|]+\|[^|]+\|[^|]+\|[^|]+\|[^|]+\|decision-1\|[A-Z_]+$|^[^|]+\|population-1\|CONFLICT_DECISION$")


def canon_bytes(v):
    def enc(x, lvl):
        i, o = "  " * (lvl + 1), "  " * lvl
        if x is None: return "null"
        if x is True: return "true"
        if x is False: return "false"
        if isinstance(x, str): return json.dumps(x)
        if isinstance(x, list):
            return "[]" if not x else "[\n" + ",\n".join(i + enc(e, lvl + 1) for e in x) + "\n" + o + "]"
        return "{}" if not x else "{\n" + ",\n".join(i + json.dumps(k) + ": " + enc(x[k], lvl + 1) for k in sorted(x)) + "\n" + o + "}"
    return (enc(v, 0) + "\n").encode()


def _strict(raw):
    def pairs(items):
        d = {}
        for k, v in items:
            if k in d: raise KeyError
            d[k] = v
        return d

    def tok(x): raise TypeError
    return json.loads(raw.decode("utf-8"), object_pairs_hook=pairs, parse_int=tok, parse_float=tok, parse_constant=tok)


def _graphs(r):
    yield r["vertices"], r["edges"], r["unknown_edges"], r["population_complete"]
    for s in r["stages"]:
        yield s["vertices"], s["edges"], s["unknown_edges"], s["population_complete"]


def classify(raw):
    """Return the single code for a record file, or None if the record is valid."""
    try:
        r = _strict(raw)
    except json.JSONDecodeError:
        return "JSON-SYNTAX"
    except KeyError:
        return "DUP-KEY"
    except TypeError:
        return "NUMBER-TOKEN"
    if canon_bytes(r) != raw:
        return "NONCANON"
    return semantic(r)


def semantic(r):
    # ---- shape tier
    verts_all = [v for vs, _, _, _ in _graphs(r) for v in vs]
    for v in verts_all:
        if isinstance(v.get("id"), dict) and set(v["id"]) & PREFIX_KEYS:
            return "ID-PREFIX-MIXED"
    if REC_KEYS - set(r): return "MISSING-KEY"
    for v in verts_all:
        if V_KEYS - set(v): return "MISSING-KEY"
        for c in v["candidates"]:
            if C_KEYS - set(c): return "MISSING-KEY"
    if set(r) - REC_KEYS: return "EXTRA-KEY"
    for v in verts_all:
        if set(v) - V_KEYS or set(v["id"]) - ID_KEYS: return "EXTRA-KEY"
        for c in v["candidates"]:
            if set(c) - C_KEYS: return "EXTRA-KEY"
    for s in r["stages"]:
        if set(s) - SNAP_KEYS: return "EXTRA-KEY"
    for v in verts_all:
        if v["roll"] not in ROLLS: return "ENUM-VALUE"
        for c in v["candidates"]:
            if c["status"] not in STATUS: return "ENUM-VALUE"
    # ---- integers
    ints, rats = [], []
    for v in verts_all:
        ints += [v["id"][k] for k in v["id"]]
        for c in v["candidates"]:
            ints += [c["slot"], c["seed_rank"]]
            if c["components"] is not None: rats += c["components"]
    for s in r["scalar_inputs"]: rats += s["values"]
    for o in rats: ints += [o["num"], o["den"]]
    for e in r["events"]: ints.append(e["index"])
    if any(not (isinstance(x, str) and INTRE.match(x)) for x in ints): return "INT-FORMAT"
    if any(abs(int(x)).bit_length() > 4096 for x in ints): return "INT-BITS"
    # ---- rationals
    if any(int(o["den"]) == 0 for o in rats): return "RAT-ZERO-DEN"
    if any(int(o["den"]) < 0 for o in rats): return "RAT-NEG-DEN"
    if any(gcd(abs(int(o["num"])), int(o["den"])) != 1 for o in rats): return "RAT-UNREDUCED"
    if any(not (0 <= Fraction(int(o["num"]), int(o["den"])) <= 100) for o in rats): return "SCORE-RANGE"
    for v in verts_all:
        for c in v["candidates"]:
            if c["components"] is not None and len(c["components"]) != 4: return "TUPLE-ARITY"
    for s in r["scalar_inputs"]:
        if len(s["values"]) != (9 if s["kind"] == "RAW9" else 4): return "TUPLE-ARITY"
    # ---- identity and graph (per snapshot for staged records)
    graphs = list(_graphs(r))
    def per(code_fn):
        for g in graphs:
            c = code_fn(*g)
            if c: return c
        return None
    def ids_dup(vs, es, un, pc):
        keys = [json.dumps(v["id"], sort_keys=True) for v in vs]
        return "ID-DUPLICATE" if len(set(keys)) != len(keys) else None
    def lab_dup(vs, es, un, pc):
        l = [v["label"] for v in vs]
        return "LABEL-DUPLICATE" if len(set(l)) != len(l) else None
    def e_self(vs, es, un, pc):
        return "EDGE-SELF" if any(e["u"] == e["v"] for e in es) else None
    def e_dang(vs, es, un, pc):
        l = {v["label"] for v in vs}
        return "EDGE-DANGLING" if any(e["u"] not in l or e["v"] not in l for e in es + un) else None
    def e_dup(vs, es, un, pc):
        k = [frozenset((e["u"], e["v"])) for e in es]
        return "EDGE-DUPLICATE" if len(set(k)) != len(k) else None
    def e_unk(vs, es, un, pc):
        a = {frozenset((e["u"], e["v"])) for e in es}
        return "EDGE-UNKNOWN-CONFLICT" if any(frozenset((e["u"], e["v"])) in a for e in un) else None
    def popf(vs, es, un, pc):
        return "POP-FLAG" if bool(un) == bool(pc) else None
    for fn in (ids_dup, lab_dup, e_self, e_dang, e_dup, e_unk, popf):
        c = per(fn)
        if c: return c
    prev = None
    for vs, es, un, pc in graphs[1:] if r["stages"] else []:
        pass
    if r["stages"]:
        sets = [{frozenset((e["u"], e["v"])) for e in s["edges"]} for s in r["stages"]]
        for a, b in zip(sets, sets[1:]):
            if not a <= b: return "STAGE-NOT-MONOTONE"
    # ---- candidates
    for v in verts_all:
        if len(v["candidates"]) > 8: return "CAND-OVERFLOW"
    for v in verts_all:
        if len({c["slot"] for c in v["candidates"]}) != len(v["candidates"]): return "CAND-SLOT-DUP"
    for v in verts_all:
        if len({c["seed_rank"] for c in v["candidates"]}) != len(v["candidates"]): return "CAND-RANK-DUP"
    for v in verts_all:
        for c in v["candidates"]:
            if (c["components"] is None) != (c["status"] == "HARD_REJECT"): return "CAND-NULL-MISMATCH"
    for v in verts_all:
        if v["roll"] in ("WIN", "LOSS") and not v["fit_complete"]: return "ROLL-BEFORE-FIT-STATIC"
    # ---- events
    for e in r["events"]:
        if not EVRE.match(e["event_id"]): return "EVENT-ID-FORMAT"
    return None
