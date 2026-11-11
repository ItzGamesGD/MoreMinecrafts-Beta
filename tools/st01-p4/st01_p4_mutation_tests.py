#!/usr/bin/env python3
"""Negative controls for the P4 verifier: deliberately broken copies of the P4 spec must make the verifier FAIL on the expected checks
(an unmutated copy must not FAIL anything). The real spec is never touched: a temp copy of tools/ is patched.
Usage: python3 -I st01_p4_mutation_tests.py [--out results.json]   (exit 1 if a mutation is NOT detected or the control fails)"""
import os, sys, json, shutil, subprocess, tempfile
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
SPEC = "p4_spec.py"
# (name, patches, expected FAIL ids that must all appear in BOTH candidates' east results)
MUT = [
 ("vault interior 23 wide instead of 25 (candidate A frame)", [("fr = Frame(xe=-17, sign=-1, zc=24, F=F, D=21, Wd=25)", "fr = Frame(xe=-17, sign=-1, zc=24, F=F, D=21, Wd=23)")], {"V1"}, "A"),
 ("centre ceiling zone only 9 high (Q68)", [("parts.append(fr.box(0, D - 1, -4, 4, 0, 9))", "parts.append(fr.box(0, D - 1, -4, 4, 0, 8))")], {"V2"}, "AB"),
 ("pile dropped into the 5-wide aisle (Q70)", [("    el[\"CHEST_BASE\"]", "    el[\"AISLE_PILE\"] = dict(kind=\"pile\", boxes=[fr.box(2, 3, -1, 1, 0, 2)])\n    el[\"CHEST_BASE\"]")], {"V3", "V8"}, "AB"),
 ("dais built 4 high (Q71)", [("            for y in range(3):\n                if (a, b, y) in colbody: continue", "            for y in range(4):\n                if (a, b, y) in colbody: continue")], {"V4"}, "AB"),
 ("front stair narrowed to 5 (Q89)", [("    for a, h in ((D - 12, 2), (D - 11, 2), (D - 10, 3)):\n        for b in range(-3, 4):", "    for a, h in ((D - 12, 2), (D - 11, 2), (D - 10, 3)):\n        for b in range(-2, 3):")], {"V4"}, "AB"),
 ("column lifted off the floor (not floor-founded, Q86)", [("boxes=[fr.box(ca - 1, ca + 1, cb - 1, cb + 1, 0, 0)])", "boxes=[fr.box(ca - 1, ca + 1, cb - 1, cb + 1, 1, 1)])")], {"V5"}, "AB"),
 ("column capital grown to 7x7 (Q84)", [("c1 = {(a, b) for a in range(ca - 2, ca + 3) for b in range(cb - 2, cb + 3)", "c1 = {(a, b) for a in range(ca - 3, ca + 4) for b in range(cb - 3, cb + 4)")], {"V5"}, "A"),
 ("chest body 4 wide instead of 5 (Q72)", [("boxes=[fr.box(D - 6, D - 4, -2, 2, 4, 5)]", "boxes=[fr.box(D - 6, D - 4, -1, 2, 4, 5)]")], {"V6"}, "AB"),
 ("chest base 8 wide (Q93 max 7x5)", [("boxes=[fr.box(D - 7, D - 3, -3, 3, 3, 3)]", "boxes=[fr.box(D - 7, D - 3, -4, 3, 3, 3)]")], {"V6"}, "AB"),
 ("block placed in front of the chest (front access lost)", [("    el[\"CHEST_BASE\"]", "    el[\"FRONT_BLOCK\"] = dict(kind=\"pile\", boxes=[fr.box(D - 8, D - 8, -2, 2, 3, 4)])\n    el[\"CHEST_BASE\"]")], {"V7b", "V7"}, "AB"),
 ("stair treads skip every other block (gradient 2:1)", [("b, tr = stair(\"x\", list(range(-9, 0)), (33, 35), -23)", "b, tr = stair(\"x\", list(range(-9, 0, 2)), (33, 35), -23)")], {"V7"}, "A"),
 ("stair headroom 3 instead of 4", [("def stair(axis, coords, lat, feet_first, head=4):", "def stair(axis, coords, lat, feet_first, head=3):")], {"V9"}, "AB"),
 ("vault raised 10 blocks into the royal-hall reservation", [("        F = -37\n        fr = Frame(xe=-17", "        F = -27\n        fr = Frame(xe=-17")], {"V10"}, "A"),
 ("candidate B vault pushed 10 east past X=+40", [("fr = Frame(xe=6, sign=1, zc=26, F=F, D=25, Wd=21)", "fr = Frame(xe=16, sign=1, zc=26, F=F, D=25, Wd=21)")], {"V12"}, "B"),
 ("declared link vault entrance <-> corridor removed", [('("CV", "VAULT_ENT"), ', '')], {"V10"}, "A"),
 ("crypt interior slid into the vault", [("crypt(-37, (8, 20), (17, 31)", "crypt(-37, (-30, -18), (17, 31)")], {"V10"}, "A"),
 ("beam raised into the stepped ceiling (Q96 independence)", [("el[\"BEAMS\"] = dict(kind=\"beam\", boxes=[fr.box(D - 2, D - 2, -7, 7, 7, 7)", "el[\"BEAMS\"] = dict(kind=\"beam\", boxes=[fr.box(D - 2, D - 2, -7, 7, 7, 8)")], {"V5b"}, "AB"),
 ("only one side has mineral piles (balance, Q69)", [("el[nm] = dict(kind=\"pile\", boxes=cells_to_boxes(best[2][sg]))", "el[nm] = dict(kind=\"pile\", boxes=cells_to_boxes(best[2][sg] if sg > 0 else best[2][sg][:20]))")], {"V8"}, "AB"),
 ("stair carved under the hall outside the hatch pierce volume", [("b, tr = stair(\"x\", list(range(-9, 0)), (33, 35), -23)", "b, tr = stair(\"x\", list(range(-12, 0)), (33, 35), -20)")], {"V10"}, "A"),
]
def stage(td):
    for d in ("st01-p1", "st01-p2", "st01-p3", "st01-p4"):
        shutil.copytree(os.path.join(TOOLS, d), os.path.join(td, d), ignore=shutil.ignore_patterns("__pycache__"))
def run(td):
    out = os.path.join(td, "out")
    subprocess.run([sys.executable, "-I", os.path.join(td, "st01-p4", "st01_p4_verify.py"), out], capture_output=True, text=True, cwd=td)
    J = json.load(open(os.path.join(out, "p4_verification.json")))
    return {v: {c["id"] for c in J[v]["east"]["checks"] if c["verdict"] == "FAIL"} for v in "AB"}
bad = 0
with tempfile.TemporaryDirectory() as td:
    stage(td); f = run(td); ok = not f["A"] and not f["B"]; bad += 0 if ok else 1
    print(f"{'OK  ' if ok else 'BAD '} control (unmutated spec) -> FAIL sets {f} (expected none)")
results = []
for name, patches, expect, where in MUT:
    with tempfile.TemporaryDirectory() as td:
        stage(td); p = os.path.join(td, "st01-p4", SPEC); s = open(p).read()
        for a, b in patches:
            assert a in s, (name, a)
            s = s.replace(a, b, 1)
        open(p, "w").write(s)
        try: f = run(td); err = None
        except Exception as e: f, err = {"A": set(), "B": set()}, repr(e)
    hit = err is None and all(expect <= f[v] for v in where)
    bad += 0 if hit else 1
    results.append(dict(mutation=name, candidates=where, expected=sorted(expect), failed={v: sorted(f[v]) for v in where}, detected=hit))
    print(f"{'DETECTED ' if hit else 'MISSED   '} {name} [{where}] -> expected {sorted(expect)} got { {v: sorted(f[v]) for v in where} }{' ERR ' + err if err else ''}")
nd = sum(1 for r in results if r["detected"])
print(f"{nd}/{len(MUT)} mutations detected; control {'clean' if bad == len(MUT) - nd else 'NOT clean'}")
if "--out" in sys.argv: json.dump(dict(control_clean=(bad == len(MUT) - nd), results=results), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
sys.exit(1 if bad else 0)
