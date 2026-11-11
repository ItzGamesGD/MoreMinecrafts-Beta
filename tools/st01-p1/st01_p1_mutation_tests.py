#!/usr/bin/env python3
"""Negative controls: deliberately broken ST-01 P1 models must make the verifier report FAIL.
Usage: python3 -I st01_p1_mutation_tests.py   (runs st01_p1_model.py on patched copies)"""
import os, subprocess, sys, tempfile, json
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = open(os.path.join(HERE, "st01_p1_model.py")).read()
MUT = {
 "altar pushed 4 blocks north (breaks slope shell)": ('"S10": ("altar chamber", "room", (-7, 7), (11, 17), (-20, -12)', '"S10": ("altar chamber", "room", (-7, 7), (11, 17), (-24, -16)', {"A2"}),
 "altar overlaps passage (3D volume collision)":     ('(11, 17), (-20, -12)', '(11, 17), (-20, -10)', {"D1"}),
 "column dropped into descent stair opening":        ('for zc in (2, 8, 14):', 'for zc in (-3, 8, 14):', {"D1", "B5", "B1", "B2"}),
 "hall roof 8 blocks too tall (breaks slope shell)": ('"S04": ("grand hall (nave + gallery air volume)", "room", (-18, 18), (0, 17)', '"S04": ("grand hall (nave + gallery air volume)", "room", (-18, 18), (0, 25)', {"A2"}),
 "undeclared contact: vestibule touches hall":       ('(-10, 10), (0, 11), (21, 24)', '(-10, 10), (0, 11), (21, 25)', {"D1", "D3"}),
}
bad = 0
for name, (a, b, expect) in MUT.items():
    assert a in SRC, name
    with tempfile.TemporaryDirectory() as td:
        f = os.path.join(td, "m.py"); open(f, "w").write(SRC.replace(a, b, 1))
        out = os.path.join(td, "o")
        subprocess.run([sys.executable, "-I", f, out], capture_output=True, text=True)
        R = json.load(open(os.path.join(out, "verification.json")))
    fails = {c["id"] for c in R["checks"] if c["verdict"] == "FAIL"} | ({"COLLISION"} if R["collisions"] else set())
    hit = bool(fails & (expect | {"COLLISION"}))
    print(("DETECTED " if hit else "MISSED   "), name, "->", sorted(fails))
    bad += (not hit)
sys.exit(1 if bad else 0)
