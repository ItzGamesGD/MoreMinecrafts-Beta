#!/usr/bin/env python3
"""Negative controls for the P3 verifier: deliberately broken copies of the EAST hall spec must make the verifier FAIL on the
expected checks (and an unmutated copy must not FAIL anything). The real spec is never touched: a temp copy of tools/ is patched.
Usage: python3 -I st01_p3_mutation_tests.py [--out results.json]      (exit 1 if a mutation is NOT detected or the control fails)"""
import os, sys, json, shutil, subprocess, tempfile
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
SPEC = "p3_spec.py"
MUT = [
 ("corner pillars added on the north side (Q52)", [('"N": [7, 25]', '"N": [2, 7, 25]')], {"K4", "K3"}),
 ("pillar base projects into the wall-side passage (Q54 trim removed)", [("for a in range(-2, 3) for i in range(2, 6)}                     # 5 along", "for a in range(-2, 3) for i in range(1, 6)}                     # 5 along")], {"K2"}),
 ("west colonnade pitch changed (gaps 5 and 7, asymmetric) (Q51/Q47)", [('"W": [7, 16, 25]', '"W": [7, 15, 25]')], {"K3", "K8"}),
 ("lintel raised one layer into the stepped ceiling (Q60/Q61)", [("(cell(side, s, i)[0], F + 7, cell(side, s, i)[1])", "(cell(side, s, i)[0], F + 8, cell(side, s, i)[1])")], {"K6", "K1"}),
 ("capital wallward projection 2: touches the wall (Q57)", [("c3 = {(a, i) for a in range(-3, 4) for i in range(1, 7)", "c3 = {(a, i) for a in range(-3, 4) for i in range(0, 7)")], {"K5"}),
 ("corbel run 1 per 1 instead of 1 per 2 (Q46)", [("rise=8, run=2,", "rise=8, run=1,")], {"H2"}),
 ("vertical wall height 9 instead of 8 (Q45)", [("wall_h=8, ", "wall_h=9, ")], {"H1", "H2"}),
 ("obelisk tip runs into the ceiling peak", [("add(\"OBELISK:tip\", \"obelisk\", [box(cx, cx, F + 11, F + 12, cz, cz)])", "add(\"OBELISK:tip\", \"obelisk\", [box(cx, cx, F + 11, F + 18, cz, cz)])")], {"O1", "K7"}),
 ("secondary-chest alcove opening walled up (independent access lost)", [("    add(\"CHEST_SECONDARY\"", "    add(\"BLOCK\", \"statue\", [box(xe, xe, F, F + 4, cz + 3, cz + 6)])\n    add(\"CHEST_SECONDARY\"")], {"A2"}),
 ("interior shrunk to 31x31 (Q43)", [("H = dict(cx=-21, cz=21, half=16,", "H = dict(cx=-21, cz=21, half=15,")], {"H1"}),
 ("hall cavity shifted 2 east out of its reservation", [("H = dict(cx=-21, cz=21, half=16,", "H = dict(cx=-19, cz=21, half=16,")], {"H3"}),
 ("hall and reservation slid 4 west past X=-40 (extension)", [("H = dict(cx=-21, cz=21, half=16,", "H = dict(cx=-25, cz=21, half=16,"), ("res=box(-40, -2, -24, -5, 2, 40),", "res=box(-44, -6, -24, -5, 2, 40),")], {"X1"}),
 ("hall slid north into the antechamber / P2 spaces", [("H = dict(cx=-21, cz=21, half=16,", "H = dict(cx=-21, cz=15, half=16,")], {"H3", "I1"}),
 ("secret-hatch zone overlapped by the colonnade and dais", [("hatch = box(cx + 12, cx + 16, F, F + 3, cz + 12, cz + 16)", "hatch = box(cx + 6, cx + 10, F, F + 3, cz + 10, cz + 14)")], {"R1"}),
 ("boss emergence candidate placed inside the obelisk", [('"BOSS_S": (0, 6)', '"BOSS_S": (0, 0)')], {"B1"}),
 ("dais frontal tread narrowed to 5 (Q32 broad stairs)", [('add("DAIS:tread", "dais", [box(cx - 4, cx + 4, F, F, cz + 9, cz + 10)])', 'add("DAIS:tread", "dais", [box(cx - 2, cx + 2, F, F, cz + 9, cz + 10)])')], {"A1"}),
 ("statue dropped into the wall-side passage (east) blocking the 2-wide route", [("    add(\"CHEST_SECONDARY\"", "    add(\"PBLOCK\", \"statue\", [box(xe - 1, xe - 1, F, F + 3, cz - 8, cz - 8)])\n    add(\"CHEST_SECONDARY\"")], {"A3"}),
 ("lintel carried across the door axis on the north side (Q49)", [("if s - cur[-1] == 9:", "if s - cur[-1] <= 18:")], {"K6"}),
 ("obelisk plinth shifted one block off the hall centre", [("box(cx - 2, cx + 2, F, F + 1, cz - 2, cz + 2)", "box(cx - 1, cx + 3, F, F + 1, cz - 2, cz + 2)")], {"O1"}),
 ("dais body raised to 4 high (Q10/Q32 low dais)", [('add("DAIS", "dais", [box(cx - 5, cx + 5, F, F + 1, cz + 11, cz + 16)])', 'add("DAIS", "dais", [box(cx - 5, cx + 5, F, F + 3, cz + 11, cz + 16)])')], {"A1"}),
]
def run(td):
    out = os.path.join(td, "out")
    subprocess.run([sys.executable, "-I", os.path.join(td, "st01-p3", "st01_p3_verify.py"), out], capture_output=True, text=True, cwd=td)
    J = json.load(open(os.path.join(out, "p3_verification.json")))
    return {c["id"] for c in J["east"]["checks"] if c["verdict"] == "FAIL"}, J
def stage(td):
    for d in ("st01-p1", "st01-p2", "st01-p3"):
        shutil.copytree(os.path.join(TOOLS, d), os.path.join(td, d), ignore=shutil.ignore_patterns("__pycache__"))
bad = 0
with tempfile.TemporaryDirectory() as td:
    stage(td); f, J = run(td)
    ok = not f; bad += 0 if ok else 1
    print(f"{'OK  ' if ok else 'BAD '} control (unmutated spec) -> FAIL set {sorted(f)} (expected none)")
results = []
for name, patches, expect in MUT:
    with tempfile.TemporaryDirectory() as td:
        stage(td); p = os.path.join(td, "st01-p3", SPEC); s = open(p).read()
        for a, b in patches:
            assert a in s, (name, a)
            s = s.replace(a, b, 1)
        open(p, "w").write(s)
        try: f, J = run(td); err = None
        except Exception as e: f, err = set(), repr(e)
    hit = bool(f) and expect <= f
    # a crash/exception inside the verifier is NOT counted as detection
    if err: hit = False
    bad += 0 if hit else 1
    results.append(dict(mutation=name, expected=sorted(expect), failed=sorted(f), detected=hit))
    print(f"{'DETECTED ' if hit else 'MISSED   '} {name} -> expected {sorted(expect)} got FAIL {sorted(f)}{' ERR ' + err if err else ''}")
print(f"{len(MUT) - sum(1 for r in results if not r['detected'])}/{len(MUT)} mutations detected; control {'clean' if bad == sum(1 for r in results if not r['detected']) else 'NOT clean'}")
if "--out" in sys.argv: json.dump(dict(control_clean=(bad == sum(1 for r in results if not r["detected"])), results=results), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
sys.exit(1 if bad else 0)
