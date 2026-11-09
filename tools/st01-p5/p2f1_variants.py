"""P2-F1 candidate re-layouts (in-memory copies of the UNMODIFIED P2 east model; nothing is written back).
C: transition group (V12, FA, LND12, FB) shifted 2 west with C1/C2 shortened and BRB1 re-seated: 9x9 landing, 7-wide stairs, same flights kept."""
import copy, os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, "..", "st01-p2"))
from st01_p2_model_east import make_east, jog_boxes, stair_boxes, mirror_model

def variant_C(m):
    v = copy.deepcopy(m); S = v["spaces"]
    S["C1"]["boxes"] = jog_boxes(8, 28, -13, 2, -6, -2, 7, 22, 28)
    for b in S["V12"]["boxes"]: b[0] -= 2; b[1] -= 2
    for b in S["LND12"]["boxes"]: b[0] -= 2; b[1] -= 2
    for b in S["FA"]["boxes"]: b[0] -= 2; b[1] -= 2
    b, t = stair_boxes("x", [28, 27, 26, 25], (4, 10), -11, 1); S["FB"]["boxes"] = b; S["FB"]["treads"] = t
    S["C2"]["boxes"] = [[14, 24, -14, -10, 4, 10]]
    S["BRB1"]["boxes"] = [[22, 24, -14, -10, 11, 14]]
    return v

def variant_A(m):
    v = copy.deepcopy(m)
    for k in ("V12", "LND12"):
        for b in v["spaces"][k]["boxes"]: b[1] = min(b[1], 37)
    for b in v["spaces"]["FA"]["boxes"]: b[0] -= 1; b[1] -= 1
    return v
