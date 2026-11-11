#!/usr/bin/env python3
"""Negative controls for the P6 verifier: deliberately broken overlays / P3-P4 specs / registries / cover states must FAIL the expected checks.
Usage: python3 -I st01_p6_mutation_tests.py [--out results.json]"""
import os, sys, json, copy
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import numpy as np
import st01_p6_verify as X
S3, S4, OV = X.S3, X.S4, X.OV

def base():
    return OV.extract(), S3.make_hall_east(), S4.make_variant("B")
def fails(r): return {c["id"] for c in r.checks if c["verdict"] == "FAIL"}
M = []
def mut(name, expect, fn): M.append((name, expect, fn))

def m_overlay(edit):
    def f():
        ov, p3, p4 = base(); edit(ov); return fails(X.verify_p6(ov, p4, p3, "E", skip_regress=True))
    return f
def m_spec(edit, **kw):
    def f():
        ov, p3, p4 = base(); edit(p4, p3); return fails(X.verify_p6(ov, p4, p3, "E", skip_regress=True, **kw))
    return f

mut("overlay: V12 left at old X (vestibule not shifted)", {"O2", "P5.X1"}, m_overlay(lambda ov: ov["changed_spaces"]["V12"].__setitem__("after", copy.deepcopy(ov["changed_spaces"]["V12"]["before"]))))
mut("overlay: landing LND12 shrunk to 7x9", {"O2", "O3", "P5.L1"}, m_overlay(lambda ov: [b.__setitem__(1, b[1] - 2) for b in ov["changed_spaces"]["LND12"]["after"]]))
mut("overlay: unapproved space B1 also moved", {"O1", "O2"}, lambda: fails(X.verify_p6((lambda o: (o["changed_spaces"].__setitem__("B1", {"before": make_b1()[0], "after": make_b1()[1]}), o)[1])(OV.extract()), S4.make_variant("B"), S3.make_hall_east(), "E", skip_regress=True)))
mut("overlay: flight B moved one block too far (breaks the stair-landing/C2 contact)", {"O2", "P5.P2R"}, m_overlay(lambda ov: [b.__setitem__(0, b[0] - 1) or b.__setitem__(1, b[1] - 1) for b in ov["changed_spaces"]["FB"]["after"]]))
mut("cover never placed (closed hall has a hole)", {"CO1", "CO2"}, lambda: fails(X.verify_p6(*base()[:1], base()[2], base()[1], "E", cover_mode="none", skip_regress=True)))
mut("cover is 13 blocks (one extra floor cell)", {"CO1", "CO2"}, lambda: fails(X.verify_p6(*base()[:1], base()[2], base()[1], "E", cover_mode="plus1", skip_regress=True)))
mut("cut widened by one floor cell outside the declared 12", {"CO1"}, m_spec(lambda p4, p3: p4["spaces"]["FA"]["boxes"].append([-5, -5, -23, -23, 33, 33])))
mut("Q97 violated: vault+crypt centred under the hall", {"Q97"}, m_spec(lambda p4, p3: (p4["envelopes"].__setitem__("VAULT_ENV", [-40, -2, -32, -19, 2, 40]), p4["envelopes"].__setitem__("CRYPT_ENV", [-40, -2, -32, -22, 2, 40]))))
mut("secret area deeper than the recorded -32 (vault envelope lowered by 4)", {"Q98"}, m_spec(lambda p4, p3: p4["envelopes"].__setitem__("VAULT_ENV", list(p4["envelopes"]["VAULT_ENV"][:2]) + [-36] + list(p4["envelopes"]["VAULT_ENV"][3:]))))
mut("stair headroom 3", {"P5.S1"}, m_spec(lambda p4, p3: [b.__setitem__(3, b[3] - 1) for b in p4["spaces"]["FA"]["boxes"]]))
mut("last stair tread missing", {"P5.S1", "ST1"}, m_spec(lambda p4, p3: p4["spaces"]["FA"]["boxes"].pop()))
mut("registry gap (P2:B2 not registered)", {"R1"}, lambda: fails(X.verify_p6(*base()[:1], base()[2], base()[1], "E", drop_reg="P2:B2", skip_regress=True)))
mut("stray P4 void inside the hall reservation (outside the pierce)", {"P5.I1"}, m_spec(lambda p4, p3: (p4["spaces"].__setitem__("STRAY", {"kind": "corr", "boxes": [[-20, -18, -22, -20, 10, 12]]}))))
mut("vault cross-section walled off by a pile", {"P5.K3"}, m_spec(lambda p4, p3: p4["elements"].__setitem__("PILE_BLOCK", {"kind": "pile", "boxes": [[14, 14, -30, -21, 14, 38]]})))
mut("air leak: P4 tunnel joined to the L3 passage", {"CO4", "P5.I1"}, m_spec(lambda p4, p3: p4["spaces"].__setitem__("LEAK", {"kind": "corr", "boxes": [[0, 2, -26, -22, -9, -1]]})))

def make_b1():
    m = X.make_east(); b = copy.deepcopy(m["spaces"]["B1"]["boxes"]); a = copy.deepcopy(b)
    for x in a: x[0] += 1; x[1] += 1
    return b, a

def ew_mut():
    ov, p3, p4 = base(); p3w = S3.mirror_spec(p3); p4w = S4.mirror_p4(p4)
    p4w["spaces"]["CRYPT"]["boxes"][0][0] += 1; p4w["spaces"]["CRYPT"]["boxes"][0][1] += 1
    re_ = X.verify_p6(ov, p4, p3, "E", skip_regress=True); rw = X.verify_p6(ov, p4w, p3w, "W", skip_regress=True)
    eq = X.mirror_eq(re_, rw); return set() if all(eq.values()) else {"EW1"}
mut("E/W asymmetry: west crypt shifted one block", {"EW1"}, ew_mut)

def run(): 
    out = []; bad = 0
    ov, p3, p4 = base(); f = fails(X.verify_p6(ov, p4, p3, "E", skip_regress=True)); ok = not f; bad += 0 if ok else 1
    print(f"{'OK  ' if ok else 'BAD '} control (unmutated) -> FAIL set {sorted(f)} (expected none)")
    for name, expect, fn in M:
        try: f = fn(); err = None
        except Exception as e: f, err = set(), repr(e)
        hit = err is None and expect <= f; bad += 0 if hit else 1
        out.append(dict(mutation=name, expected=sorted(expect), failed=sorted(f), detected=hit))
        print(f"{'DETECTED ' if hit else 'MISSED   '} {name} -> expected {sorted(expect)} got {sorted(f)}{' ERR ' + err if err else ''}")
    nd = sum(o["detected"] for o in out); print(f"{nd}/{len(M)} mutations detected; control {'clean' if ok else 'NOT clean'}")
    if "--out" in sys.argv: json.dump(dict(control_clean=ok, results=out), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
    return 0 if bad == 0 else 1
if __name__ == "__main__": sys.exit(run())
