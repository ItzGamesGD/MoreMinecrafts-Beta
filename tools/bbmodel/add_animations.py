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


def _sample(frames, t, axis=0):
    if not frames:
        return 0.0
    frames = sorted(frames)
    if t <= frames[0][0]:
        return frames[0][1][axis]
    for (t0, v0), (t1, v1) in zip(frames, frames[1:]):
        if t0 <= t <= t1:
            return v0[axis] + (v1[axis] - v0[axis]) * ((t - t0) / (t1 - t0) if t1 > t0 else 0)
    return frames[-1][1][axis]


def axe_pitch(bones, desired):
    """Keys piglin_forgemaster.axe_hold_bone so the axe's pitch in the sagittal plane follows `desired`
    ([(t, deg)], 0 = haft upright, negative = head leaning forward) whatever the body chain does.
    Pitch = sum of X rotations down the chain + the hold bone's rest tilt (-30) + its keyframe."""
    chain = ("pelvis_bone", "torso_bone", "arm_upper_right_bone", "arm_lower_right_bone")
    tracks = {b: bones.get(b, {}).get("rotation") for b in chain}
    times = sorted({t for t, _ in desired} | {t for fr in tracks.values() if fr for t, _ in fr})
    want = [(t, (p, 0, 0)) for t, p in desired]
    out = []
    for t in times:
        total = sum(_sample(fr, t) for fr in tracks.values() if fr)
        out.append((t, (round(_sample(want, t) - total + 30.0, 3), 0, 0)))
    bones["axe_hold_bone"] = {"rotation": out}


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
    # ---- authority: docs/planning/boss_sync_2026-11-15 (U-01): basic melee overhead chop (dmg 12), skill 1 diagonal
    # fan slash, skill 2 ground slam -> fissure telegraph -> fire pillars, skill 3 furnace flame (core opens, 2 s
    # emission), phase-2 overheat transition, 2.5 s core-exposed stagger. Times below are the server hit points.
    shake = lambda t0, t1, amp, hz=10: [(round(t0 + i / hz, 3), (0, amp * (-1) ** i, 0)) for i in range(int((t1 - t0) * hz) + 1)]
    attack_overhead = {  # basic: axe cocked over the head (0.5 s), chop lands at 0.75 s
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.5, 0, 1.0, 1), (0.75, 0, -2.5, -2), (1.0, 0, -2.5, -2), (1.3, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 12, 0, 0), (0.75, -30, 0, 0), (1.0, -30, 0, 0), (1.3, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 165, 0, -8), (0.75, 45, 0, -4), (1.0, 45, 0, -4), (1.3, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 25, 0, 0), (0.75, 0, 0, 0), (1.3, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 120, 0, 10), (0.75, 35, 0, 6), (1.0, 35, 0, 6), (1.3, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 14, 0, 0), (0.75, -16, 0, 0), (1.3, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.75, 22, 0, 0), (1.3, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.75, -10, 0, 0), (1.3, 0, 0, 0))},
    }
    axe_pitch(attack_overhead, [(0, -30), (0.5, 55), (0.75, -95), (1.0, -95), (1.3, -30)])
    attack_cleave = {  # basic variant: body winds right, short diagonal chop across to the left at 0.6 s
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 6, -35, 0), (0.6, -16, 40, 0), (0.8, -16, 40, 0), (1.1, 0, 0, 0))},
        "pelvis_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 0, -12, 0), (0.6, 0, 14, 0), (1.1, 0, 0, 0)),
                        "position": kf((0, 0, 0, 0), (0.6, 0, -1.5, -1), (0.8, 0, -1.5, -1), (1.1, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 140, 0, 28), (0.6, 62, 0, -8), (0.8, 62, 0, -8), (1.1, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 22, 0, 0), (0.6, 0, 0, 0), (1.1, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 30, 0, -24), (0.6, 20, 0, -10), (1.1, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 0, 24, 0), (0.6, 0, -34, 0), (0.8, 0, -34, 0), (1.1, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 16, 0, 0), (1.1, 0, 0, 0))},
    }
    axe_pitch(attack_cleave, [(0, -30), (0.45, 40), (0.6, -80), (0.8, -80), (1.1, -30)])
    skill_slash = {  # skill 1: big wind-up to the back right (1.2 s), diagonal fan slash high-right -> low-left at 1.4 s
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 8, -62, 0), (1.4, -26, 70, 0), (1.9, -26, 70, 0), (2.5, 0, 0, 0))},
        "pelvis_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 0, -18, 0), (1.4, 0, 22, 0), (1.9, 0, 22, 0), (2.5, 0, 0, 0)),
                        "position": kf((0, 0, 0, 0), (1.2, 0, -1.5, 0), (1.4, 0, -3, -2), (1.9, 0, -3, -2), (2.5, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 150, 0, 34), (1.4, 50, 0, -22), (1.9, 50, 0, -22), (2.5, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 26, 0, 0), (1.4, 0, 0, 0), (2.5, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 40, 0, -30), (1.4, 18, 0, -12), (2.5, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 0, 30, 0), (1.4, 0, -44, 0), (1.9, 0, -44, 0), (2.5, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.4, 28, 0, 0), (1.9, 28, 0, 0), (2.5, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -14, 0, 0), (1.4, -22, 0, 0), (2.5, 0, 0, 0))},
    }
    axe_pitch(skill_slash, [(0, -30), (1.2, 50), (1.4, -85), (1.9, -85), (2.5, -30)])
    skill_quake = {  # skill 2: axe overhead (1.2 s), slammed into the floor at 1.4 s, held while the fissure telegraphs,
        # fire pillars erupt at 2.6 s (end of the hold), then the axe is wrenched out
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (1.2, 0, 1.5, 1), (1.4, 0, -4, -3), (2.6, 0, -4, -3), (3.3, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 14, 0, 0), (1.4, -38, 0, 0), (2.6, -38, 0, 0), (3.3, 0, 0, 0)),
                       "position": [(0, (0, 0, 0)), (1.4, (0, 0, 0))] + [(t, (0, 0, v[1])) for t, v in shake(1.4, 2.6, 0.35)] + [(2.6, (0, 0, 0)), (3.3, (0, 0, 0))]},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 172, 0, -6), (1.4, 58, 0, -2), (2.6, 58, 0, -2), (3.3, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 28, 0, 0), (1.4, 0, 0, 0), (2.6, 0, 0, 0), (3.3, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 140, 0, 12), (1.4, 48, 0, 6), (2.6, 48, 0, 6), (3.3, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 18, 0, 0), (1.4, -24, 0, 0), (2.6, -24, 0, 0), (3.3, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.4, 26, 0, 8), (2.6, 26, 0, 8), (3.3, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.4, -14, 0, -8), (2.6, -14, 0, -8), (3.3, 0, 0, 0))},
    }
    axe_pitch(skill_quake, [(0, -30), (1.2, 62), (1.4, -92), (2.6, -92), (3.3, -30)])
    skill_furnace = {  # skill 3: axe planted upright in front (0.8 s), core door opens, 2 s of flame (1.0-3.0 s), door shuts, axe pulled back
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.8, 0, -3, -2), (3.2, 0, -3, -2), (4.0, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.8, -12, 0, 0), (1.0, -8, 0, 0), (3.0, -8, 0, 0), (4.0, 0, 0, 0)),
                       "position": [(0, (0, 0, 0)), (1.0, (0, 0, 0))] + [(t, (0, 0, v[1])) for t, v in shake(1.0, 3.0, 0.3, 12)] + [(3.2, (0, 0, 0)), (4.0, (0, 0, 0))]},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 52, 0, -6), (3.2, 52, 0, -6), (4.0, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 10, 0, 0), (3.2, 10, 0, 0), (4.0, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 18, 0, -34), (3.0, 18, 0, -42), (4.0, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 6, 0, 0), (1.0, 26, 0, 0), (3.0, 26, 0, 0), (3.4, 0, 0, 0))},
        "core_door_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 0, 0, 0), (1.0, 0, -105, 0), (3.0, 0, -105, 0), (3.3, 0, 0, 0), (4.0, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 8, 0, 10), (3.2, 8, 0, 10), (4.0, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.8, 8, 0, -10), (3.2, 8, 0, -10), (4.0, 0, 0, 0))},
    }
    axe_pitch(skill_furnace, [(0, -30), (0.8, -172), (3.2, -172), (4.0, -30)])
    overheat = {  # phase 2 (HP <= 45%): axe thrust skyward, roar, whole body shaking while the armour glows (texture overlay is separate)
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.5, 0, -1.5, 0), (2.4, 0, -1.5, 0), (3.0, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 16, 0, 0), (2.4, 16, 0, 0), (3.0, 0, 0, 0)),
                       "position": [(0, (0, 0, 0)), (0.5, (0, 0, 0))] + [(t, (0, 0, v[1])) for t, v in shake(0.5, 2.4, 0.4, 14)] + [(3.0, (0, 0, 0))]},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 34, 0, 0), (2.4, 34, 0, 0), (3.0, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 160, 0, 16), (2.4, 160, 0, 16), (3.0, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 16, 0, 0), (2.4, 16, 0, 0), (3.0, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 40, 0, -78), (2.4, 40, 0, -78), (3.0, 0, 0, 0))},
        "arm_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 24, 0, 0), (2.4, 24, 0, 0), (3.0, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 0, 0, 12), (2.4, 0, 0, 12), (3.0, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 0, 0, -12), (2.4, 0, 0, -12), (3.0, 0, 0, 0))},
    }
    axe_pitch(overheat, [(0, -30), (0.5, 8), (2.4, 8), (3.0, -30)])
    stagger = {  # overheat recovery: 2.5 s hunched and rigid with the core door open (takes 1.5x damage)
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.3, 0, -4, 0), (2.2, 0, -4, 0), (2.5, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -34, 0, 0), (2.2, -34, 0, 0), (2.5, 0, 0, 0)),
                       "position": [(0, (0, 0, 0)), (0.3, (0, 0, 0))] + [(t, (0, 0, v[1])) for t, v in shake(0.3, 2.2, 0.18, 8)] + [(2.5, (0, 0, 0))]},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -22, 0, 0), (2.2, -22, 0, 0), (2.5, 0, 0, 0))},
        "core_door_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 0, -105, 0), (2.2, 0, -105, 0), (2.5, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 30, 0, 12), (2.2, 30, 0, 12), (2.5, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 24, 0, -14), (2.2, 24, 0, -14), (2.5, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 28, 0, 8), (2.2, 28, 0, 8), (2.5, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 20, 0, -8), (2.2, 20, 0, -8), (2.5, 0, 0, 0))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -24, 0, 0), (2.2, -24, 0, 0), (2.5, 0, 0, 0))},
        "leg_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -18, 0, 0), (2.2, -18, 0, 0), (2.5, 0, 0, 0))},
    }
    axe_pitch(stagger, [(0, -30), (0.3, -62), (2.2, -62), (2.5, -30)])
    leap_slam = {  # extra (not in the authority doc): crouch, leap over 1 s, overhead chop on landing at 1.6 s
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.5, 0, -5, 0), (0.7, 0, 4, -2), (1.0, 0, 32, -4), (1.15, 0, 34, -4), (1.55, 0, 0, 0), (1.7, 0, -3, -2), (2.4, 0, 0, 0))},
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.5, -14, 0, 0), (1.0, 14, 0, 0), (1.15, 18, 0, 0), (1.6, -34, 0, 0), (1.9, -34, 0, 0), (2.4, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 20, 0, 0), (1.0, 168, 0, -8), (1.15, 172, 0, -8), (1.6, 48, 0, -4), (1.9, 48, 0, -4), (2.4, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.15, 26, 0, 0), (1.6, 0, 0, 0), (2.4, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 20, 0, 0), (1.0, 140, 0, 12), (1.6, 40, 0, 6), (1.9, 40, 0, 6), (2.4, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (1.15, 12, 0, 0), (1.6, -20, 0, 0), (2.4, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 36, 0, 0), (1.0, 40, 0, 0), (1.55, 14, 0, 0), (1.9, 14, 0, 0), (2.4, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, 30, 0, 0), (1.0, 56, 0, 0), (1.55, 8, 0, 0), (1.9, 8, 0, 0), (2.4, 0, 0, 0))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.5, -44, 0, 0), (1.0, -70, 0, 0), (1.55, -10, 0, 0), (2.4, 0, 0, 0))},
        "leg_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.5, -40, 0, 0), (1.0, -84, 0, 0), (1.55, -10, 0, 0), (2.4, 0, 0, 0))},
    }
    axe_pitch(leap_slam, [(0, -30), (0.5, -10), (1.0, 58), (1.15, 62), (1.6, -92), (1.9, -92), (2.4, -30)])
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
    return {"walk": (W, "loop", walk), "attack_overhead": (1.3, "once", attack_overhead),
            "attack_cleave": (1.1, "once", attack_cleave), "skill_slash": (2.5, "once", skill_slash),
            "skill_quake": (3.3, "once", skill_quake), "skill_furnace": (4.0, "once", skill_furnace),
            "overheat": (3.0, "once", overheat), "stagger": (2.5, "once", stagger),
            "leap_slam": (2.4, "once", leap_slam), "death": (2.2, "hold", death)}


MODELS = {
    "sky_wyvern": ("sky_wyvern", wyvern),
    "floating_jellyfish": ("floating_jellyfish", jellyfish),
    "deep_sea_poseidon": ("deep_sea_poseidon", poseidon),
    "piglin_forgemaster": ("piglin_forgemaster", forgemaster),
}


# animations replaced by differently named ones (removed from the bbmodel on the next run)
OBSOLETE = {"piglin_forgemaster": ("slam", "sweep", "enrage")}


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
        obsolete = {f"animation.{model}.{n}" for n in OBSOLETE.get(key, ())}
        kept = [a for a in data.get("animations", []) if a["name"] not in new and a["name"] not in obsolete]
        data["animations"] = kept + list(new.values())
        path.write_text(json.dumps(data, indent="\t", ensure_ascii=False), encoding="utf-8")
        print(key, [a["name"].split(".")[-1] for a in data["animations"]])


if __name__ == "__main__":
    sys.exit(main())
