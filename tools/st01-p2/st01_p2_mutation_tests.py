#!/usr/bin/env python3
"""Negative controls for the P2 verifier: deliberately broken east models must produce FAIL on the expected check(s).
Usage: python3 -I st01_p2_mutation_tests.py   (copies the tool dir to a temp dir, patches the east model, runs the verifier)"""
import os, sys, json, shutil, subprocess, tempfile
HERE = os.path.dirname(os.path.abspath(__file__))
M = "st01_p2_model_east.py"
MUT = [
 ("royal hall reservation slid 10 east (collides with L2/L3 spaces)", [("box(-40, -2, -24, -5, 2, 40)", "box(-30, 8, -24, -5, 2, 40)")], {"C2"}),
 ("royal hall reservation pushed beyond X=-40 (extension)", [("box(-40, -2, -24, -5, 2, 40)", "box(-44, -6, -24, -5, 2, 40)")], {"X1"}),
 ("royal approach squeezed from 5 to 3 wide", [("[box(-11, -4, -22, -19, -9, -5)]", "[box(-11, -4, -22, -19, -8, -6)]")], {"W1", "Q1", "I3"}),
 ("threshold longer than 7", [("threshold_len=6", "threshold_len=9")], {"Q1"}),
 ("branch to B3 detached from the corridor", [("[box(19, 21, -14, -10, 0, 3)]", "[box(19, 21, -14, -10, 0, 2)]")], {"C3", "R1", "R3"}),
 ("tomb T2 dragged onto T1 (3D cavity overlap)", [("[box(18, 24, -6, -2, -26, -18)]", "[box(12, 18, -6, -2, -26, -18)]")], {"C1", "I2", "C3", "C4"}),
 ("T3 given clear 5 under the surface hall (cover rule)", [("[box(9, 15, -6, -3, -3, 3)]", "[box(9, 15, -6, -2, -3, 3)]")], {"C5"}),
 ("hall interior shrunk to 31 wide", [("box(-37, -5, -22, -7, 5, 37)", "box(-36, -6, -22, -7, 5, 37)")], {"H1"}),
 ("corridor C1 detached from the P1 landing", [("jog_boxes(8, 30,", "jog_boxes(10, 30,")], {"C3", "R1"}),
 ("flight B turned to the same axis as flight A", [('stair_boxes("x", [30, 29, 28, 27], (4, 10), -11, 1)', 'stair_boxes("z", [4, 5, 6, 7], (30, 33), -11, 1)')], {"S1", "C3", "R1", "W1"}),
]
bad = 0
for name, patches, expect in MUT:
    with tempfile.TemporaryDirectory() as td:
        for f in os.listdir(HERE):
            if f.endswith(".py"): shutil.copy(os.path.join(HERE, f), td)
        os.makedirs(os.path.join(td, "..", "st01-p1"), exist_ok=True)
        shutil.copy(os.path.join(HERE, "..", "st01-p1", "st01_p1_model.py"), os.path.join(td, "..", "st01-p1", "st01_p1_model.py"))
        s = open(os.path.join(td, M)).read()
        for a, b in patches:
            assert a in s, (name, a)
            s = s.replace(a, b, 1)
        open(os.path.join(td, M), "w").write(s)
        out = os.path.join(td, "o")
        r = subprocess.run([sys.executable, "-I", os.path.join(td, "st01_p2_verify.py"), out], capture_output=True, text=True)
        try:
            R = json.load(open(os.path.join(out, "p2_verification.json")))
            fails = {c["id"] for c in R["east"]["checks"] if c["verdict"] == "FAIL"}
        except Exception as e:
            fails = {"CRASH:" + (r.stderr.strip().splitlines() or ["?"])[-1][:60]}
    hit = bool(fails & expect)
    print(("DETECTED " if hit else "MISSED   "), name, "->", sorted(fails))
    bad += (not hit)
sys.exit(1 if bad else 0)
