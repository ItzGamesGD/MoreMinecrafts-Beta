#!/usr/bin/env python3
"""Adds the combat/locomotion animations to the four unimplemented mob .bbmodel sources.

Idempotent: every animation named in ANIMS is removed and re-created, other animations (e.g. the authored idle) are kept.
Rotation values are Blockbench editor degrees (right-handed, +Y up, front = -Z): positive X swings a hanging limb
forwards and drops the nose of a front-facing head upwards; positive Z on a +X bone lifts it up. Positions are pixels.
Looping animations start and end on the same pose.
"""
import json
import math
import sys
import uuid
from pathlib import Path

SRC = Path(__file__).resolve().parent / "source"
NS = uuid.UUID("6f1c3a52-8f3e-4d0b-9c1e-0a5b7d2e4c11")


def sine(amp, period, phase=0.0, off=0.0, n=8):
    """n+1 samples over one period (first == last). Returns [(t, value)]."""
    return [(round(period * i / n, 4), round(off + amp * math.sin(2 * math.pi * (i / n) + phase), 3)) for i in range(n + 1)]


def merge(**axes):
    """merge(x=[(t,v)...], y=..., z=...) -> [(t,(x,y,z))] on the union of times (missing axes hold 0)."""
    times = sorted({t for a in axes.values() for t, _ in a})
    def val(a, t):
        pts = dict(a)
        if t in pts:
            return pts[t]
        ts = sorted(pts)
        lo = max([x for x in ts if x <= t], default=ts[0])
        hi = min([x for x in ts if x >= t], default=ts[-1])
        if lo == hi:
            return pts[lo]
        return pts[lo] + (pts[hi] - pts[lo]) * (t - lo) / (hi - lo)
    return [(t, tuple(round(val(axes[k], t), 3) if k in axes else 0 for k in "xyz")) for t in times]


def kf(*frames):
    """kf((t,x,y,z), ...) -> [(t,(x,y,z))]"""
    return [(round(f[0], 4), tuple(f[1:])) for f in frames]


# ---------------------------------------------------------------- sky wyvern
def wyvern():
    T = 1.2
    fly = {
        "torso_bone": {"position": merge(y=sine(1.6, T, math.pi / 2)), "rotation": merge(x=sine(3, T, math.pi / 2))},
        "wing_upper_right_bone": {"rotation": merge(z=sine(32, T, 0, 8))},
        "wing_upper_left_bone": {"rotation": merge(z=[(t, -v) for t, v in sine(32, T, 0, 8)])},
        "wing_fore_right_bone": {"rotation": merge(z=sine(22, T, -0.9, 6))},
        "wing_fore_left_bone": {"rotation": merge(z=[(t, -v) for t, v in sine(22, T, -0.9, 6)])},
        "head_bone": {"rotation": merge(x=sine(-4, T, math.pi / 2, 2))},
        "tail_1_bone": {"rotation": merge(x=sine(5, T, -0.6))},
        "tail_2_bone": {"rotation": merge(x=sine(7, T, -1.2))},
        "tail_3_bone": {"rotation": merge(x=sine(9, T, -1.8))},
        "tail_fan_core_bone": {"rotation": merge(x=sine(11, T, -2.4))},
        "thigh_right_bone": {"rotation": kf((0, 25, 0, 0), (T, 25, 0, 0))},
        "thigh_left_bone": {"rotation": kf((0, 25, 0, 0), (T, 25, 0, 0))},
    }
    # bite: lunge forward, jaw snaps open then shut (strike frame at 0.5 s)
    bite = {
        "torso_bone": {"position": kf((0, 0, 0, 0), (0.3, 0, 0.5, 3), (0.5, 0, -1, -7), (0.8, 0, 0, 0)),
                       "rotation": kf((0, 0, 0, 0), (0.3, 8, 0, 0), (0.5, -14, 0, 0), (0.8, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 18, 0, 0), (0.5, -22, 0, 0), (0.8, 0, 0, 0))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -38, 0, 0), (0.45, -42, 0, 0), (0.55, 0, 0, 0), (0.8, 0, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 8), (0.3, 0, 0, 40), (0.5, 0, 0, -12), (0.8, 0, 0, 8))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, -8), (0.3, 0, 0, -40), (0.5, 0, 0, 12), (0.8, 0, 0, -8))},
        "tail_1_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 8, 0, 0), (0.5, -10, 0, 0), (0.8, 0, 0, 0))},
    }
    # dive: rear up (0.7 s), wings fold back, plunge nose-first, pull out (strike at 1.1 s)
    dive = {
        "torso_bone": {"position": kf((0, 0, 0, 0), (0.7, 0, 3, 5), (1.1, 0, -8, -14), (1.7, 0, 0, 0)),
                       "rotation": kf((0, 0, 0, 0), (0.7, 22, 0, 0), (1.1, -52, 0, 0), (1.7, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.7, -14, 0, 0), (1.1, 22, 0, 0), (1.7, 0, 0, 0))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 0, 0, 0), (1.1, -34, 0, 0), (1.5, 0, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 8), (0.7, 0, 0, 55), (1.1, 0, 0, -38), (1.7, 0, 0, 8))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, -8), (0.7, 0, 0, -55), (1.1, 0, 0, 38), (1.7, 0, 0, -8))},
        "wing_fore_right_bone": {"rotation": kf((0, 0, 0, 6), (0.7, 0, 0, 20), (1.1, 0, 0, -52), (1.7, 0, 0, 6))},
        "wing_fore_left_bone": {"rotation": kf((0, 0, 0, -6), (0.7, 0, 0, -20), (1.1, 0, 0, 52), (1.7, 0, 0, -6))},
        "tail_1_bone": {"rotation": kf((0, 0, 0, 0), (0.7, -10, 0, 0), (1.1, 14, 0, 0), (1.7, 0, 0, 0))},
        "tail_fan_core_bone": {"rotation": kf((0, 0, 0, 0), (0.7, -16, 0, 0), (1.1, 22, 0, 0), (1.7, 0, 0, 0))},
        "thigh_right_bone": {"rotation": kf((0, 25, 0, 0), (1.1, 60, 0, 0), (1.7, 25, 0, 0))},
        "thigh_left_bone": {"rotation": kf((0, 25, 0, 0), (1.1, 60, 0, 0), (1.7, 25, 0, 0))},
    }
    # breath: rear back, head rises, jaw gapes, head sweeps side to side while the breath is held (0.8 s - 2.0 s)
    breath = {
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 16, 0, 0), (2.0, 16, 0, 0), (2.5, 0, 0, 0)),
                       "position": kf((0, 0, 0, 0), (0.8, 0, 2, 3), (2.0, 0, 2, 3), (2.5, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -24, 0, 0), (1.1, -24, 22, 0), (1.55, -24, -22, 0), (2.0, -24, 0, 0), (2.5, 0, 0, 0))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -44, 0, 0), (2.0, -44, 0, 0), (2.5, 0, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 8), (0.8, 0, 0, 44), (2.0, 0, 0, 44), (2.5, 0, 0, 8))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, -8), (0.8, 0, 0, -44), (2.0, 0, 0, -44), (2.5, 0, 0, -8))},
        "wing_fore_right_bone": {"rotation": kf((0, 0, 0, 6), (0.8, 0, 0, 24), (2.0, 0, 0, 24), (2.5, 0, 0, 6))},
        "wing_fore_left_bone": {"rotation": kf((0, 0, 0, -6), (0.8, 0, 0, -24), (2.0, 0, 0, -24), (2.5, 0, 0, -6))},
        "tail_1_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -12, 0, 0), (2.0, -12, 0, 0), (2.5, 0, 0, 0))},
    }
    # death: stall, roll onto the back, wings fold limp (hold the final pose)
    death = {
        "torso_bone": {"position": kf((0, 0, 0, 0), (0.6, 0, 2, 2), (1.8, 0, -16, 6)),
                       "rotation": kf((0, 0, 0, 0), (0.6, 12, 0, 8), (1.8, 20, 0, 150))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.6, -20, 0, 10), (1.8, 30, 0, 20))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (0.6, -30, 0, 0), (1.8, -22, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 8), (0.6, 0, 0, 50), (1.8, 0, 0, -50))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, -8), (0.6, 0, 0, -50), (1.8, 0, 0, 50))},
        "wing_fore_right_bone": {"rotation": kf((0, 0, 0, 6), (0.6, 0, 0, 20), (1.8, 0, 0, -40))},
        "wing_fore_left_bone": {"rotation": kf((0, 0, 0, -6), (0.6, 0, 0, -20), (1.8, 0, 0, 40))},
        "tail_1_bone": {"rotation": kf((0, 0, 0, 0), (1.8, 20, 0, 0))},
        "tail_fan_core_bone": {"rotation": kf((0, 0, 0, 0), (1.8, 30, 0, 0))},
    }
    return {
        "fly": (T, "loop", fly), "bite": (0.8, "once", bite), "dive": (1.7, "once", dive),
        "breath": (2.5, "once", breath), "death": (1.8, "hold", death),
    }


# ---------------------------------------------------------------- floating jellyfish
def jellyfish():
    T = 2.0
    tents = [("tent_f_bone", "x", 0.0), ("tent_b_bone", "x", 3.1), ("tent_c_right_bone", "z", 0.4), ("tent_cb_right_bone", "z", 1.0),
             ("tent_s_right_bone", "z", 1.6), ("tent_c_left_bone", "z", 0.4), ("tent_cb_left_bone", "z", 1.0), ("tent_s_left_bone", "z", 1.6)]
    sign = lambda b: -1 if b.endswith("left_bone") else 1

    def trail(amp, T, lag):
        out = {}
        for b, ax, ph in tents:
            out[b] = {"rotation": merge(**{ax: [(t, v * (sign(b) if ax == "z" else 1)) for t, v in sine(amp, T, -ph - lag)]})}
        return out

    # pulse-propel: contract the bell (scale squash), tentacles stream behind, then relax
    move = {
        "bell_bone": {"position": merge(y=sine(1.5, T, math.pi / 2))},
        "skirt_bone": {"scale": [(t, (1 + 0.1 * -math.sin(2 * math.pi * t / T), 1 + 0.12 * math.sin(2 * math.pi * t / T), 1 + 0.1 * -math.sin(2 * math.pi * t / T)))
                                 for t, _ in sine(1, T)]},
        "core_bone": {"scale": [(t, (1 + 0.08 * math.sin(2 * math.pi * t / T),) * 3) for t, _ in sine(1, T)]},
    }
    move.update(trail(10, T, 0.5))
    for b in ("oarm_f_right_bone", "oarm_f_left_bone", "oarm_b_right_bone", "oarm_b_left_bone"):
        move[b] = {"rotation": merge(x=sine(14, T, -0.8))}
    # sting: draw tentacles in (0-0.4 s), then lash outward and forwards at 0.55 s, recover
    sting = {
        "bell_bone": {"position": kf((0, 0, 0, 0), (0.4, 0, 1.5, 2), (0.55, 0, -1, -4), (1.0, 0, 0, 0))},
        "skirt_bone": {"scale": kf((0, 1, 1, 1), (0.4, 1.12, 0.88, 1.12), (0.55, 0.9, 1.12, 0.9), (1.0, 1, 1, 1))},
        "core_bone": {"scale": kf((0, 1, 1, 1), (0.4, 1.35, 1.35, 1.35), (0.55, 1.6, 1.6, 1.6), (1.0, 1, 1, 1))},
    }
    for b, ax, _ in tents:
        s = sign(b) if ax == "z" else 1
        rest = (0, 0, 0)
        if ax == "z":
            sting[b] = {"rotation": kf((0, *rest), (0.4, 0, 0, -24 * s), (0.55, 0, 0, 52 * s), (1.0, *rest))}
        else:
            d = 1 if b == "tent_f_bone" else -1
            sting[b] = {"rotation": kf((0, *rest), (0.4, -24 * d, 0, 0), (0.55, 52 * d, 0, 0), (1.0, *rest))}
    for b in ("oarm_f_right_bone", "oarm_f_left_bone", "oarm_b_right_bone", "oarm_b_left_bone"):
        sting[b] = {"rotation": kf((0, 0, 0, 0), (0.4, -20, 0, 0), (0.55, 38, 0, 0), (1.0, 0, 0, 0))}
    # death: bell sags and flattens, tentacles splay and go limp, core fades out (scale 0 at the end, hold)
    death = {
        "bell_bone": {"position": kf((0, 0, 0, 0), (0.5, 0, 1, 0), (1.5, 0, -9, 0))},
        "skirt_bone": {"scale": kf((0, 1, 1, 1), (0.5, 1.1, 0.95, 1.1), (1.5, 1.25, 0.45, 1.25))},
        "core_bone": {"scale": kf((0, 1, 1, 1), (0.5, 1.3, 1.3, 1.3), (1.5, 0.05, 0.05, 0.05))},
    }
    for b, ax, _ in tents:
        s = sign(b) if ax == "z" else 1
        if ax == "z":
            death[b] = {"rotation": kf((0, 0, 0, 0), (1.5, 0, 0, 62 * s))}
        else:
            d = 1 if b == "tent_f_bone" else -1
            death[b] = {"rotation": kf((0, 0, 0, 0), (1.5, 62 * d, 0, 0))}
    for b in ("oarm_f_right_bone", "oarm_f_left_bone", "oarm_b_right_bone", "oarm_b_left_bone"):
        death[b] = {"rotation": kf((0, 0, 0, 0), (1.5, 30, 0, 0))}
    return {"move": (T, "loop", move), "sting": (1.0, "once", sting), "death": (1.5, "hold", death)}


# ---------------------------------------------------------------- humanoids
def poseidon():
    T = 4.0
    idle = {
        "pelvis_bone": {"position": merge(y=sine(0.3, T, 0, 0, 8))},
        "torso_core_bone": {"rotation": merge(x=sine(1.2, T, 0), y=sine(1.0, T, 1.2))},
        "head_core_bone": {"rotation": merge(x=sine(1.5, T, math.pi), y=sine(3, T, 0.6))},
        "arm_upper_right_bone": {"rotation": merge(x=sine(1.5, T, 0.4), z=sine(1, T, 0, 2))},
        "arm_upper_left_bone": {"rotation": merge(x=sine(-1.5, T, 0.4), z=sine(1, T, 0, -2))},
        "cape_top_bone": {"rotation": merge(x=sine(3, T, -0.8, 4))},
    }
    W = 1.0
    sw = lambda a, ph=0: merge(x=sine(a, W, ph))
    walk = {
        "pelvis_bone": {"position": merge(y=[(t, abs(v)) for t, v in sine(0.5, W, math.pi / 2, 0, 8)])},
        "leg_upper_right_bone": {"rotation": sw(32)},
        "leg_upper_left_bone": {"rotation": sw(-32)},
        "leg_lower_right_bone": {"rotation": merge(x=[(t, max(0, v)) for t, v in sine(35, W, -1.2, 5)])},
        "leg_lower_left_bone": {"rotation": merge(x=[(t, max(0, v)) for t, v in sine(-35, W, -1.2, -5)])},
        "arm_upper_right_bone": {"rotation": sw(-22)},
        "arm_upper_left_bone": {"rotation": sw(22)},
        "torso_core_bone": {"rotation": merge(y=sine(4, W))},
        "cape_top_bone": {"rotation": merge(x=sine(5, W, -1, 8))},
    }
    # thrust: coil back (0-0.5 s), drive the trident forwards at 0.7 s, hold, recover
    thrust = {
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 4, 22, 0), (0.7, -10, -16, 0), (1.3, -6, -10, 0), (1.8, 0, 0, 0))},
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.5, 0, -0.5, 1.5), (0.7, 0, -0.5, -3), (1.8, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, -35, 10, 10), (0.7, 88, -8, 0), (1.3, 82, -6, 0), (1.8, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 35, 0, 0), (0.7, 0, 0, 0), (1.8, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 24, 0, -10), (0.7, 40, 0, -6), (1.8, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 2, 12, 0), (0.7, -4, -10, 0), (1.8, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, -8, 0, 0), (0.7, 20, 0, 0), (1.8, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 14, 0, 0), (0.7, -16, 0, 0), (1.8, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.7, 18, 0, 0), (1.8, 0, 0, 0))},
    }
    # summon: both arms raise the trident overhead (1.2 s), hold while the storm builds, slam down (2.4 s)
    summon = {
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (1.2, 0, 1, 0), (2.4, 0, 1, 0), (2.6, 0, -1.2, 0), (3.2, 0, 0, 0))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 10, 0, 0), (2.4, 10, 0, 0), (2.6, -14, 0, 0), (3.2, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 172, 0, -6), (2.4, 172, 0, -6), (2.6, 40, 0, 0), (3.2, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 172, 0, 6), (2.4, 172, 0, 6), (2.6, 40, 0, 0), (3.2, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 8, 0, 0), (2.6, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 22, 0, 0), (2.4, 22, 0, 0), (2.6, -16, 0, 0), (3.2, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -10, 0, 0), (2.4, -14, 0, 0), (2.6, 30, 0, 0), (3.2, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -10, 0, 4), (2.6, 6, 0, 4), (3.2, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -10, 0, -4), (2.6, 6, 0, -4), (3.2, 0, 0, 0))},
    }
    death = {
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.8, 0, -6, 2), (2.0, 0, -15, 6)), "rotation": kf((0, 0, 0, 0), (0.8, 10, 0, 0), (2.0, 88, 0, 0))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 8, 0, 0), (2.0, 4, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 24, 0, 8), (2.0, 14, 0, 12))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 40, 0, 24), (2.0, 20, 0, 34))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 30, 0, -20), (2.0, 20, 0, -34))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (2.0, 24, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 10, 0, 6), (2.0, 4, 0, 10))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 22, 0, -6), (2.0, 10, 0, -10))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 30, 0, 0), (2.0, 74, 0, 0))},
    }
    return {"idle": (T, "loop", idle), "walk": (W, "loop", walk), "thrust": (1.8, "once", thrust),
            "summon": (3.2, "once", summon), "death": (2.0, "hold", death)}


def forgemaster():
    W = 1.4
    sw = lambda a, ph=0: merge(x=sine(a, W, ph))
    walk = {  # heavy plod, big torso counter-twist
        "pelvis_bone": {"position": merge(y=[(t, abs(v)) for t, v in sine(0.9, W, math.pi / 2, 0, 8)])},
        "leg_upper_right_bone": {"rotation": sw(28)},
        "leg_upper_left_bone": {"rotation": sw(-28)},
        "leg_lower_right_bone": {"rotation": merge(x=[(t, max(0, v)) for t, v in sine(30, W, -1.2, 4)])},
        "leg_lower_left_bone": {"rotation": merge(x=[(t, max(0, v)) for t, v in sine(-30, W, -1.2, -4)])},
        "arm_upper_right_bone": {"rotation": merge(x=sine(-14, W), z=[(t, 8) for t, _ in sine(1, W)])},
        "arm_upper_left_bone": {"rotation": merge(x=sine(18, W), z=[(t, -6) for t, _ in sine(1, W)])},
        "torso_bone": {"rotation": merge(x=[(t, 6 + v) for t, v in sine(1.5, W, math.pi / 2)], y=sine(6, W))},
        "head_bone": {"rotation": merge(y=sine(-4, W))},
        "loin_front_bone": {"rotation": merge(x=sine(-6, W, 0.6))},
        "loin_back_bone": {"rotation": merge(x=sine(6, W, 0.6))},
    }
    # slam: axe over the head (0-1.0 s), crash down at 1.2 s (server windup point), recover
    slam = {
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (1.0, 0, 1.2, 1), (1.2, 0, -2.2, -2), (1.7, 0, -2.2, -2), (2.4, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 14, 0, 0), (1.2, -34, 0, 0), (1.7, -34, 0, 0), (2.4, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 168, 0, -8), (1.2, 40, 0, -4), (1.7, 40, 0, -4), (2.4, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 20, 0, 0), (1.2, 0, 0, 0), (2.4, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 130, 0, 10), (1.2, 40, 0, 6), (1.7, 40, 0, 6), (2.4, 0, 0, 0))},
        "arm_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 14, 0, 0), (1.2, 0, 0, 0), (2.4, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 14, 0, 0), (1.2, -18, 0, 0), (2.4, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 24, 0, 0), (2.4, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -12, 0, 0), (2.4, 0, 0, 0))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 10, 0, 0), (1.2, 0, 0, 0))},
    }
    # sweep: cock the axe out to the right (0.8 s), wide horizontal arc to the left at 1.0 s
    sweep = {
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -4, -38, 0), (1.0, -6, 55, 0), (1.3, -6, 55, 0), (1.9, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 88, -62, 14), (1.0, 92, 70, 0), (1.3, 92, 70, 0), (1.9, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 12, 0, 0), (1.0, 0, 0, 0), (1.9, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 26, 0, -22), (1.0, 18, 0, -8), (1.9, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 0, 22, 0), (1.0, 0, -40, 0), (1.3, 0, -40, 0), (1.9, 0, 0, 0))},
        "pelvis_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 0, -10, 0), (1.0, 0, 16, 0), (1.9, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.0, 12, 0, 0), (1.9, 0, 0, 0))},
    }
    # enrage: the core door swings open, head back, arms flung wide, torso shakes (roar 0.4-2.0 s), close and settle
    shake = [(round(0.4 + 0.1 * i, 3), (0, 0.45 * (-1) ** i, 0)) for i in range(17)]
    enrage = {
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 14, 0, 0), (2.0, 14, 0, 0), (2.6, 0, 0, 0)),
                       "position": [(0, (0, 0, 0))] + shake + [(2.6, (0, 0, 0))]},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 32, 0, 0), (2.0, 32, 0, 0), (2.6, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 20, 0, 70), (2.0, 20, 0, 70), (2.6, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 20, 0, -70), (2.0, 20, 0, -70), (2.6, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 24, 0, 0), (2.0, 24, 0, 0), (2.6, 0, 0, 0))},
        "arm_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 24, 0, 0), (2.0, 24, 0, 0), (2.6, 0, 0, 0))},
        "core_door_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 0, -105, 0), (2.0, 0, -105, 0), (2.6, 0, 0, 0))},
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.4, 0, -0.8, 0), (2.0, 0, -0.8, 0), (2.6, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 0, 0, 10), (2.6, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.4, 0, 0, -10), (2.6, 0, 0, 0))},
    }
    # death: stagger, core door pops open, kneel, then pitch forward (hold)
    death = {
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.8, 0, -8, 0), (2.2, 0, -14, -6)), "rotation": kf((0, 0, 0, 0), (0.8, 0, 0, 0), (2.2, -40, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -22, 0, 0), (2.2, -48, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -20, 0, 0), (2.2, -40, 0, 0))},
        "core_door_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 0, -95, 0), (2.2, 0, -95, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 78, 0, 0), (2.2, 90, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 72, 0, 0), (2.2, 86, 0, 0))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -88, 0, 0), (2.2, -92, 0, 0))},
        "leg_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -84, 0, 0), (2.2, -92, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 30, 0, 18), (2.2, 55, 0, 12))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 20, 0, -18), (2.2, 40, 0, -12))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (2.2, 20, 0, 0))},
        "loin_front_bone": {"rotation": kf((0, 0, 0, 0), (2.2, 40, 0, 0))},
    }
    return {"walk": (W, "loop", walk), "slam": (2.4, "once", slam), "sweep": (1.9, "once", sweep),
            "enrage": (2.6, "once", enrage), "death": (2.2, "hold", death)}


MODELS = {
    "sky_wyvern": ("sky_wyvern", wyvern),
    "floating_jellyfish": ("floating_jellyfish", jellyfish),
    "deep_sea_poseidon": ("deep_sea_poseidon", poseidon),
    "piglin_forgemaster": ("piglin_forgemaster", forgemaster),
}


def build_animation(model, name, length, loop, bones, groups, existing_uuid=None):
    full = f"animation.{model}.{name}"
    anim = {
        "uuid": existing_uuid or str(uuid.uuid5(NS, full)), "name": full,
        "loop": {"loop": "loop", "once": "once", "hold": "hold"}[loop], "length": length, "snapping": 24,
        "selected": False, "override": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "",
        "animators": {},
    }
    for bone, channels in bones.items():
        if bone not in groups:
            raise SystemExit(f"{model}.{name}: unknown bone {bone}")
        keyframes = []
        for channel, frames in channels.items():
            for t, (x, y, z) in frames:
                if t > length + 1e-6:
                    raise SystemExit(f"{model}.{name}.{bone}.{channel}: keyframe {t} beyond length {length}")
                keyframes.append({
                    "uuid": str(uuid.uuid5(NS, f"{full}/{bone}/{channel}/{t}")), "channel": channel, "time": t,
                    "data_points": [{"x": str(round(x, 3) + 0.0), "y": str(round(y, 3) + 0.0), "z": str(round(z, 3) + 0.0)}],
                    "interpolation": "linear", "color": -1,
                })
        keyframes.sort(key=lambda k: (k["time"], k["channel"]))
        anim["animators"][groups[bone]] = {"name": bone, "type": "bone", "keyframes": keyframes}
    return anim


def main():
    for key, (model, fn) in MODELS.items():
        path = SRC / f"{key}.bbmodel"
        data = json.loads(path.read_text(encoding="utf-8"))
        groups = {g["name"]: g["uuid"] for g in data["groups"]}
        new = {f"animation.{model}.{n}": build_animation(model, n, ln, lp, b, groups) for n, (ln, lp, b) in fn().items()}
        kept = [a for a in data.get("animations", []) if a["name"] not in new]
        data["animations"] = kept + list(new.values())
        path.write_text(json.dumps(data, indent="\t", ensure_ascii=False), encoding="utf-8")
        print(key, [a["name"].split(".")[-1] for a in data["animations"]])


if __name__ == "__main__":
    sys.exit(main())
