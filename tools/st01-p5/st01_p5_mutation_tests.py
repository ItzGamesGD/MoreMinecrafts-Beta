#!/usr/bin/env python3
"""Negative controls for the P5 integrated verifier: broken copies of the P2-variant / P4 spec / P5 cover rule must produce FAIL on the expected checks.
Usage: python3 -I st01_p5_mutation_tests.py [--out results.json]"""
import os, sys, json, shutil, subprocess, tempfile
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
# (name, file(dir/name), patches, expected FAIL ids that must appear for candidate B / layout C east)
MUT = [
 ("hidden stair starts one block too low (first step 2 high)", "st01-p4/p4_spec.py", [("b, tr = stair(\"x\", list(range(-9, -2)), (33, 35), -23)", "b, tr = stair(\"x\", list(range(-9, -2)), (33, 35), -24)")], {"S1"}),
 ("last stair tread missing (landing 2 below)", "st01-p4/p4_spec.py", [("b, tr = stair(\"x\", list(range(-9, -2)), (33, 35), -23)", "b, tr = stair(\"x\", list(range(-9, -3)), (33, 35), -23)")], {"S1"}),
 ("stair headroom 3", "st01-p4/p4_spec.py", [("def stair(axis, coords, lat, feet_first, head=4):", "def stair(axis, coords, lat, feet_first, head=3):")], {"S1"}),
 ("cover never placed (closed hall has a hole)", "st01-p5/st01_p5_verify.py", [("closed_solid[cover] = True", "pass")], {"K1"}),
 ("cover placed one layer too high (floor raised)", "st01-p5/st01_p5_verify.py", [("closed_solid[cover] = True", "closed_solid[np.roll(cover, 1, axis=1)] = True")], {"K1"}),
 ("pierce volume shrunk (cut leaves the declared volume)", "st01-p4/p4_spec.py", [("pierce = box(-9, -5, -24, -19, 33, 37)\n        goals.update(HATCH=(-9, -22, 36), LANDING_A=(0, -30, 34), VAULT_ENTRY", "pierce = box(-9, -7, -24, -19, 33, 37)\n        goals.update(HATCH=(-9, -22, 36), LANDING_A=(0, -30, 34), VAULT_ENTRY")], {"I1", "K4"}),
 ("candidate B vault dropped to the hall depth (collides with L2)", "st01-p4/p4_spec.py", [("        F = -30\n        fr = Frame(xe=6", "        F = -22\n        fr = Frame(xe=6")], {"I1"}),
 ("candidate B vault pushed past X=+40", "st01-p4/p4_spec.py", [("fr = Frame(xe=6, sign=1, zc=26, F=F, D=25, Wd=21)", "fr = Frame(xe=16, sign=1, zc=26, F=F, D=25, Wd=21)")], {"X1"}),
 ("P2-F1 layout C landing shrunk to 7x9", "st01-p5/p2f1_variants.py", [("    for b in S[\"LND12\"][\"boxes\"]: b[0] -= 2; b[1] -= 2\n    for b in S[\"FA\"]", "    for b in S[\"LND12\"][\"boxes\"]: b[0] -= 2; b[1] -= 4\n    for b in S[\"FA\"]")], {"L1"}),
 ("P2-F1 layout C flight B left overlapping C2", "st01-p5/p2f1_variants.py", [("S[\"C2\"][\"boxes\"] = [[14, 24, -14, -10, 4, 10]]", "S[\"C2\"][\"boxes\"] = [[14, 26, -14, -10, 4, 10]]")], {"P2R"}),
 ("P2-F1 layout C vestibule left at the old X (not shifted)", "st01-p5/p2f1_variants.py", [("    for b in S[\"V12\"][\"boxes\"]: b[0] -= 2; b[1] -= 2\n", "")], {"X1", "P2R"}),
 ("P1 descent treads missing from the integrated world (chain broken)", "st01-p5/st01_p5_verify.py", [("        solid[V.sl([-5, 5, -6, -k - 1, -k, -k])] = True", "        pass")], {"K2", "K3"}),
]
def stage(td):
    for d in ("st01-p1", "st01-p2", "st01-p3", "st01-p4", "st01-p5"):
        shutil.copytree(os.path.join(TOOLS, d), os.path.join(td, d), ignore=shutil.ignore_patterns("__pycache__"))
def run(td):
    out = os.path.join(td, "out")
    subprocess.run([sys.executable, "-I", os.path.join(td, "st01-p5", "st01_p5_verify.py"), out], capture_output=True, text=True, cwd=td)
    J = json.load(open(os.path.join(out, "p5_verification.json")))
    return {c["id"] for c in J["B|C"]["east"]["checks"] if c["verdict"] == "FAIL"}
bad = 0
with tempfile.TemporaryDirectory() as td:
    stage(td); f = run(td); ok = not f; bad += 0 if ok else 1
    print(f"{'OK  ' if ok else 'BAD '} control (unmutated) -> FAIL set {sorted(f)} (expected none)")
results = []
for name, fp, patches, expect in MUT:
    with tempfile.TemporaryDirectory() as td:
        stage(td); p = os.path.join(td, fp); s = open(p).read()
        for a, b in patches:
            assert a in s, (name, a)
            s = s.replace(a, b, 1)
        open(p, "w").write(s)
        try: f = run(td); err = None
        except Exception as e: f, err = set(), repr(e)
    hit = err is None and expect <= f
    bad += 0 if hit else 1
    results.append(dict(mutation=name, expected=sorted(expect), failed=sorted(f), detected=hit))
    print(f"{'DETECTED ' if hit else 'MISSED   '} {name} -> expected {sorted(expect)} got {sorted(f)}{' ERR ' + err if err else ''}")
nd = sum(r["detected"] for r in results)
print(f"{nd}/{len(MUT)} mutations detected; control {'clean' if bad == len(MUT) - nd else 'NOT clean'}")
if "--out" in sys.argv: json.dump(dict(control_clean=(bad == len(MUT) - nd), results=results), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
sys.exit(1 if bad else 0)
