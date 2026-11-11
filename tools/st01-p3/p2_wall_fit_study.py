#!/usr/bin/env python3
"""Feasibility study (NOT an edit): can the P2 candidate be brought inside the X/Z +-40 projection when WALLS are counted (D01 whole-structure reading)?
Derives in-memory variants of the unmodified P2 east model, measures wall-inclusive extents (cavity + P2 minimum wall 2) and re-runs the P2 engine
on each variant. The P2 model/verifier files are only imported, never written.  Usage: python3 -I p2_wall_fit_study.py <out.json>"""
import sys, os, json, copy
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p3_vox as V
C = V.C
from st01_p2_model_east import make_east, mirror_model
import st01_p2_verify as P2V

WALL = C.MIN_WALL

def wall_extent(m):
    ext = {}
    for k, sp in m["spaces"].items():
        for b in sp["boxes"]:
            for ax, (lo, hi) in (("x", (b[0], b[1])), ("z", (b[4], b[5]))):
                e = ext.setdefault(ax, {"min": (999, None), "max": (-999, None)})
                if lo - WALL < e["min"][0]: e["min"] = (lo - WALL, k)
                if hi + WALL > e["max"][0]: e["max"] = (hi + WALL, k)
    return ext

def over40(m):
    out = []
    for k, sp in m["spaces"].items():
        for b in sp["boxes"]:
            for lo, hi, ax in ((b[0], b[1], "x"), (b[4], b[5], "z")):
                if hi + WALL > 40 or lo - WALL < -40: out.append((k, ax, lo, hi))
    return sorted(set(out))

def variant_trim_east(m, new_x1=37):
    """Variant A: V12, FA and LND12 lose their east-most 2 columns (x 38..39). Nothing else moves."""
    v = copy.deepcopy(m)
    for k in ("V12", "LND12"):
        for b in v["spaces"][k]["boxes"]: b[1] = min(b[1], new_x1)
    for b in v["spaces"]["FA"]["boxes"]: b[0] -= 1; b[1] -= 1      # flight A: width 7 kept (x 32..38 -> 31..37)
    return v

def run(m):
    R, G = P2V.run_side(m)
    return dict(passes=sum(c["verdict"] == "PASS" for c in R["checks"]), total=len(R["checks"]),
                fails=[c["id"] for c in R["checks"] if c["verdict"] != "PASS"],
                widths=R["metrics"].get("width_rows"), landings=None)

def main(out):
    E = make_east(); res = {}
    res["baseline_east_wall_extent"] = {a: {k: list(v) for k, v in d.items()} for a, d in wall_extent(E).items()}
    res["baseline_over40"] = over40(E)
    A = variant_trim_east(E)
    res["variantA_over40"] = over40(A)
    res["variantA_wall_extent"] = {a: {k: list(v) for k, v in d.items()} for a, d in wall_extent(A).items()}
    res["variantA_east"] = run(A)
    res["variantA_west"] = run(mirror_model(A))
    import p3_spec as S3, st01_p3_verify as P3V
    rr = P3V.verify(S3.make_hall_east(), A)
    res["variantA_p3_hall"] = dict(passes=sum(c["verdict"] == "PASS" for c in rr.checks), fails=[c["id"] for c in rr.checks if c["verdict"] == "FAIL"],
                                   unverified=[c["id"] for c in rr.checks if c["verdict"] == "UNVERIFIED"])
    res["variantA_landing"] = [A["spaces"]["LND12"]["boxes"][0], A["spaces"]["V12"]["boxes"][0], A["spaces"]["FA"]["boxes"][0]]
    # hall reservation is a reservation incl. walls: check X/Z of reservations separately
    res["reservations"] = {k: r["box"] for k, r in E["res"].items()}
    json.dump(res, open(out, "w"), indent=1)
    print(json.dumps({k: res[k] for k in ("baseline_over40", "variantA_over40", "variantA_east", "variantA_west", "variantA_p3_hall", "variantA_landing")}, indent=1))

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p2_wall_fit.json")
