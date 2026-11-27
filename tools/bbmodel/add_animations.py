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


def tool_pitch(bones, desired, hold, chain, rest=0.0):
    """Keys `hold` so a held tool's pitch in the sagittal plane follows `desired` ([(t, deg)], 0 = upright,
    negative = tip leaning forward, -90 = level and pointing forward) whatever the body chain does.
    Pitch = sum of X rotations down `chain` + the hold bone's rest tilt + its keyframe. Extra channels already
    set on the hold bone (e.g. scale) are kept."""
    tracks = {b: bones.get(b, {}).get("rotation") for b in chain}
    times = sorted({t for t, _ in desired} | {t for fr in tracks.values() if fr for t, _ in fr})
    want = [(t, (p, 0, 0)) for t, p in desired]
    out = []
    for t in times:
        total = sum(_sample(fr, t) for fr in tracks.values() if fr)
        out.append((t, (round(_sample(want, t) - total - rest, 3), 0, 0)))
    bones.setdefault(hold, {})["rotation"] = out


def axe_pitch(bones, desired):
    """piglin_forgemaster: the axe_hold_bone rests at -30 (head leaning forward)."""
    tool_pitch(bones, desired, "axe_hold_bone",
               ("pelvis_bone", "torso_bone", "arm_upper_right_bone", "arm_lower_right_bone"), rest=-30.0)


def trident_pitch(bones, desired):
    """deep_sea_poseidon: the trident stands upright at rest (trident_hold_bone at the right fist)."""
    tool_pitch(bones, desired, "trident_hold_bone",
               ("pelvis_bone", "torso_core_bone", "arm_upper_right_bone", "arm_lower_right_bone"))


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
    # wing_buffet (SKY_ISLANDS_T2 §3 날개 밀쳐내기, defensive): rear up, raise both wings, beat them forward at 0.45 s
    wing_buffet = {
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 24, 0, 0), (0.45, 10, 0, 0), (0.7, 6, 0, 0), (1.0, 0, 0, 0)),
                       "position": kf((0, 0, 0, 0), (0.3, 0, 2, 2), (0.45, 0, 1, -1), (1.0, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 16, 0, 0), (0.45, -10, 0, 0), (1.0, 0, 0, 0))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -24, 0, 0), (0.5, -10, 0, 0), (1.0, 0, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 8), (0.3, 0, -20, 58), (0.45, 0, 46, -20), (0.7, 0, 30, -10), (1.0, 0, 0, 8))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, -8), (0.3, 0, 20, -58), (0.45, 0, -46, 20), (0.7, 0, -30, 10), (1.0, 0, 0, -8))},
        "wing_fore_right_bone": {"rotation": kf((0, 0, 0, 6), (0.3, 0, 0, 28), (0.45, 0, 20, -24), (1.0, 0, 0, 6))},
        "wing_fore_left_bone": {"rotation": kf((0, 0, 0, -6), (0.3, 0, 0, -28), (0.45, 0, -20, 24), (1.0, 0, 0, -6))},
        "tail_1_bone": {"rotation": kf((0, 0, 0, 0), (0.3, -10, 0, 0), (0.45, 8, 0, 0), (1.0, 0, 0, 0))},
        "tail_3_bone": {"rotation": kf((0, 0, 0, 0), (0.45, -10, 0, 0), (1.0, 0, 0, 0))},
        "thigh_right_bone": {"rotation": kf((0, 25, 0, 0), (0.3, 50, 0, 0), (1.0, 25, 0, 0))},
        "thigh_left_bone": {"rotation": kf((0, 25, 0, 0), (0.3, 50, 0, 0), (1.0, 25, 0, 0))},
    }
    # hurt (피격 반응): short recoil, head snaps back, wings flinch
    hurt = {
        "torso_bone": {"rotation": kf((0, 0, 0, 0), (0.08, 10, 0, -6), (0.4, 0, 0, 0)), "position": kf((0, 0, 0, 0), (0.08, 0, 0.5, 1.5), (0.4, 0, 0, 0))},
        "head_bone": {"rotation": kf((0, 0, 0, 0), (0.08, 18, 10, 0), (0.4, 0, 0, 0))},
        "jaw_bone": {"rotation": kf((0, 0, 0, 0), (0.08, -20, 0, 0), (0.4, 0, 0, 0))},
        "wing_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.08, 0, 0, 16), (0.4, 0, 0, 0))},
        "wing_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.08, 0, 0, -16), (0.4, 0, 0, 0))},
        "tail_2_bone": {"rotation": kf((0, 0, 0, 0), (0.08, 0, 14, 0), (0.4, 0, 0, 0))},
    }
    return {
        "fly": (T, "loop", fly), "dive": (1.7, "once", dive), "breath": (2.5, "once", breath),
        "wing_buffet": (1.0, "once", wing_buffet), "hurt": (0.4, "once", hurt), "death": (1.8, "hold", death),
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
    # ---- authority: docs/planning/boss_sync_2026-11-15 (U-01) 심해의 해신: free-swimming underwater boss, basic
    # trident thrust/swing (8), skill 1 spinning-trident pull, skill 2 1.2 s telegraph then 14-block piercing dash,
    # skill 3 trident throw that returns after 1.5 s. Owner (2026-11-26): it must look like it floats, never walks.
    T = 4.0
    hover = {  # neutral buoyancy: slow bob, legs dangling and treading, arms drifting, cape and loincloth waving
        "pelvis_bone": {"position": merge(y=sine(1.4, T, 0), z=sine(0.6, T, 1.1)),
                        "rotation": merge(x=sine(3, T, 0.7), z=sine(2, T, 1.9))},
        "torso_core_bone": {"rotation": merge(x=sine(2, T, 1.3), y=sine(3, T, 0.4))},
        "head_core_bone": {"rotation": merge(x=sine(3, T, 2.0), y=sine(5, T, 0.9))},
        "leg_upper_right_bone": {"rotation": merge(x=sine(9, T, 0.0, 12), z=sine(2, T, 0, 4))},
        "leg_upper_left_bone": {"rotation": merge(x=sine(9, T, math.pi, 6), z=sine(2, T, 0, -4))},
        "leg_lower_right_bone": {"rotation": merge(x=sine(8, T, -0.9, -20))},
        "leg_lower_left_bone": {"rotation": merge(x=sine(8, T, math.pi - 0.9, -14))},
        "arm_upper_right_bone": {"rotation": merge(x=sine(5, T, 0.5, 14), z=sine(3, T, 1.0, 8))},
        "arm_lower_right_bone": {"rotation": merge(x=sine(4, T, 0.9, 16))},
        "arm_upper_left_bone": {"rotation": merge(x=sine(6, T, 2.0, 6), z=sine(7, T, 0.6, -22))},
        "arm_lower_left_bone": {"rotation": merge(x=sine(6, T, 2.6, 20))},
        "cape_top_bone": {"rotation": merge(x=sine(9, T, -0.8, -18), z=sine(3, T, 0.3))},
        "loin_front_bone": {"rotation": merge(x=sine(7, T, -1.4, 6))},
    }
    trident_pitch(hover, [(0, 0), (T, 0)])
    S = 1.6
    swim = {  # body laid forward, flutter kick, trident levelled ahead, free arm sculling, cape streaming back
        "pelvis_bone": {"rotation": merge(x=sine(4, S, 0.0, -58), z=sine(3, S, 0.6)), "position": merge(y=sine(0.8, S, 1.2))},
        "head_core_bone": {"rotation": merge(x=sine(3, S, 0.4, 44))},
        "torso_core_bone": {"rotation": merge(x=sine(3, S, 0.9), y=sine(4, S, 0.3))},
        "leg_upper_right_bone": {"rotation": merge(x=sine(20, S, 0.0, -8))},
        "leg_upper_left_bone": {"rotation": merge(x=sine(20, S, math.pi, -8))},
        "leg_lower_right_bone": {"rotation": merge(x=sine(16, S, -1.0, -14))},
        "leg_lower_left_bone": {"rotation": merge(x=sine(16, S, math.pi - 1.0, -14))},
        "arm_upper_right_bone": {"rotation": merge(x=sine(4, S, 0.5, 150), z=sine(3, S, 0, 6))},
        "arm_lower_right_bone": {"rotation": merge(x=sine(3, S, 0.8, 10))},
        "arm_upper_left_bone": {"rotation": merge(x=sine(32, S, 0.0, 30), z=sine(10, S, 1.0, -26))},
        "arm_lower_left_bone": {"rotation": merge(x=sine(18, S, 0.7, 30))},
        "cape_top_bone": {"rotation": merge(x=sine(12, S, -0.6, -52), z=sine(5, S, 0.4))},
        "loin_front_bone": {"rotation": merge(x=sine(10, S, -0.8, 30))},
    }
    trident_pitch(swim, [(0, -95), (S, -95)])
    float_legs = lambda *ts: {  # legs keep treading water during the attacks
        "leg_upper_right_bone": {"rotation": [(t, (10 + 8 * math.sin(i), 0, 4)) for i, t in enumerate(ts)]},
        "leg_upper_left_bone": {"rotation": [(t, (6 - 8 * math.sin(i), 0, -4)) for i, t in enumerate(ts)]},
        "leg_lower_right_bone": {"rotation": [(t, (-20 + 6 * math.sin(i + 1), 0, 0)) for i, t in enumerate(ts)]},
        "leg_lower_left_bone": {"rotation": [(t, (-14 - 6 * math.sin(i + 1), 0, 0)) for i, t in enumerate(ts)]},
    }
    attack_thrust = {  # basic (8): draw the trident back to the hip, drive it straight ahead at 0.55 s
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.35, 0, 0.5, 2.5), (0.55, 0, -0.5, -4), (0.75, 0, -0.5, -4), (1.0, 0, 0, 0))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.35, 4, -26, 0), (0.55, -8, 18, 0), (0.75, -8, 18, 0), (1.0, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.35, 20, 0, 12), (0.55, 86, 0, 0), (0.75, 86, 0, 0), (1.0, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.35, 72, 0, 0), (0.55, 0, 0, 0), (0.75, 0, 0, 0), (1.0, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.35, 40, 0, -20), (0.55, -10, 0, -30), (1.0, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.35, 0, 18, 0), (0.55, -4, -12, 0), (1.0, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.35, -8, 0, 0), (0.55, -30, 0, 0), (1.0, 0, 0, 0))},
        **float_legs(0, 0.35, 0.55, 1.0),
    }
    trident_pitch(attack_thrust, [(0, 0), (0.3, -90), (0.75, -90), (1.0, 0)])
    attack_swing = {  # basic variant (8): wind to the right, sweep the trident across the front at 0.65 s
        "pelvis_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 0, -16, 0), (0.65, 0, 20, 0), (0.85, 0, 20, 0), (1.2, 0, 0, 0))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 0, -48, 0), (0.65, -6, 52, 0), (0.85, -6, 52, 0), (1.2, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 78, 0, 30), (0.65, 84, 0, -10), (0.85, 84, 0, -10), (1.2, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 14, 0, 0), (0.65, 0, 0, 0), (1.2, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 20, 0, -36), (0.65, 30, 0, -14), (1.2, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.45, 0, 26, 0), (0.65, 0, -30, 0), (1.2, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.45, -6, 0, 14), (0.65, -20, 0, -18), (1.2, 0, 0, 0))},
        **float_legs(0, 0.45, 0.65, 1.2),
    }
    trident_pitch(attack_swing, [(0, 0), (0.4, -90), (0.85, -90), (1.2, 0)])
    # 450 deg/s. The three prongs repeat every 120 deg, so the earlier 1080 deg/s (108 deg per frame at 10 fps)
    # looked frozen on video; 45 deg per frame at 10 fps and 7.5 at 60 fps both read as spinning.
    spin = [(0.6, (0, 0, 0)), (2.6, (0, 900, 0)), (3.0, (0, 900, 0))]
    skill_pull = {  # skill 1: arm thrust straight ahead, the trident spins on its own axis (0.6-2.6 s) and drags targets in
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.6, 0, 0, 2), (2.6, 0, 0, 3), (3.0, 0, 0, 0))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 6, 0, 0), (2.6, 8, 0, 0), (3.0, 0, 0, 0)),
                            "position": [(0, (0, 0, 0)), (0.6, (0, 0, 0))] + [(round(0.6 + i * 0.1, 3), (0.25 * (-1) ** i, 0, 0)) for i in range(1, 20)] + [(2.6, (0, 0, 0)), (3.0, (0, 0, 0))]},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 90, 0, 0), (2.6, 90, 0, 0), (3.0, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.3, 30, 0, 0), (0.6, 0, 0, 0), (2.6, 0, 0, 0), (3.0, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 30, 0, -40), (2.6, 30, 0, -46), (3.0, 0, 0, 0))},
        "arm_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 40, 0, 0), (2.6, 40, 0, 0), (3.0, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.6, -6, 0, 0), (2.6, -6, 0, 0), (3.0, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.6, -24, 0, 0), (1.6, -34, 0, 6), (2.6, -24, 0, -6), (3.0, 0, 0, 0))},
        "trident_shaft_bone": {"rotation": spin},
        **float_legs(0, 0.6, 1.6, 2.6, 3.0),
    }
    trident_pitch(skill_pull, [(0, 0), (0.6, -90), (2.6, -90), (3.0, 0)])
    skill_charge = {  # skill 2: coil and aim for 1.2 s (telegraph), streamlined piercing dash 1.2-1.9 s, recover
        "pelvis_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -22, 0, 0), (1.3, -80, 0, 0), (1.9, -80, 0, 0), (2.6, 0, 0, 0)),
                        "position": kf((0, 0, 0, 0), (1.2, 0, 0, 4), (1.3, 0, 0, -2), (1.9, 0, 0, -2), (2.6, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 18, 0, 0), (1.3, 66, 0, 0), (1.9, 66, 0, 0), (2.6, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 70, 0, 0), (1.3, 170, 0, 0), (1.9, 170, 0, 0), (2.6, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 40, 0, 0), (1.3, 0, 0, 0), (1.9, 0, 0, 0), (2.6, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -30, 0, -20), (1.3, -14, 0, -6), (1.9, -14, 0, -6), (2.6, 0, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 52, 0, 6), (1.3, -6, 0, 2), (1.9, -6, 0, 2), (2.6, 0, 0, 0))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, 46, 0, -6), (1.3, -6, 0, -2), (1.9, -6, 0, -2), (2.6, 0, 0, 0))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -74, 0, 0), (1.3, 0, 0, 0), (1.9, 0, 0, 0), (2.6, 0, 0, 0))},
        "leg_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -70, 0, 0), (1.3, 0, 0, 0), (1.9, 0, 0, 0), (2.6, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (1.2, -10, 0, 0), (1.3, -70, 0, 0), (1.9, -70, 0, 0), (2.6, 0, 0, 0))},
        "loin_front_bone": {"rotation": kf((0, 0, 0, 0), (1.3, 50, 0, 0), (1.9, 50, 0, 0), (2.6, 0, 0, 0))},
    }
    trident_pitch(skill_charge, [(0, 0), (1.2, -90), (1.9, -90), (2.6, 0)])
    hidden = [(0, (1, 1, 1)), (0.7, (1, 1, 1)), (0.72, (0.001, 0.001, 0.001)), (2.18, (0.001, 0.001, 0.001)), (2.2, (1, 1, 1)), (2.8, (1, 1, 1))]
    skill_throw = {  # skill 3: overhand throw released at 0.7 s; the hand is empty until the trident flies back at 2.2 s
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.55, 14, -32, 0), (0.7, -14, 22, 0), (0.9, -14, 22, 0), (2.1, 0, 0, 0), (2.2, 6, -8, 0), (2.8, 0, 0, 0))},
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.55, 0, 0.5, 2), (0.7, 0, 0, -2), (1.2, 0, 0, 0), (2.2, 0, 0, 1), (2.8, 0, 0, 0))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.55, 192, 0, 8), (0.7, 70, 0, 0), (0.9, 60, 0, 0), (1.6, 20, 0, 6), (2.1, 118, 0, 10), (2.2, 108, 0, 10), (2.8, 0, 0, 0))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (0.55, 34, 0, 0), (0.7, 0, 0, 0), (2.1, 10, 0, 0), (2.2, 24, 0, 0), (2.8, 0, 0, 0))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.55, 60, 0, -14), (0.7, -18, 0, -24), (1.2, 0, 0, -14), (2.8, 0, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.55, 10, 20, 0), (0.7, -4, -12, 0), (1.2, 0, 0, 0), (2.1, 10, 0, 0), (2.8, 0, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.55, -6, 0, 0), (0.7, -26, 0, 0), (1.4, -10, 0, 0), (2.8, 0, 0, 0))},
        "trident_hold_bone": {"scale": hidden},
        **float_legs(0, 0.55, 0.7, 1.4, 2.2, 2.8),
    }
    trident_pitch(skill_throw, [(0, 0), (0.55, -70), (0.7, -90), (2.2, -10), (2.8, 0)])
    death = {  # goes limp and sinks, rolling onto its back, arms and cape floating up
        "pelvis_bone": {"position": kf((0, 0, 0, 0), (0.6, 0, 1.5, 0), (2.5, 0, -10, 3)),
                        "rotation": kf((0, 0, 0, 0), (0.6, 12, 0, 6), (2.5, 72, 0, 18))},
        "torso_core_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 10, 0, 0), (2.5, 6, 0, 0))},
        "head_core_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 22, 0, 10), (2.5, 30, 0, 18))},
        "arm_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 30, 0, 30), (2.5, 50, 0, 70))},
        "arm_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 20, 0, -36), (2.5, 46, 0, -74))},
        "arm_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (2.5, 30, 0, 0))},
        "arm_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (2.5, 34, 0, 0))},
        "leg_upper_right_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 18, 0, 6), (2.5, 26, 0, 14))},
        "leg_upper_left_bone": {"rotation": kf((0, 0, 0, 0), (0.6, 8, 0, -6), (2.5, 34, 0, -12))},
        "leg_lower_right_bone": {"rotation": kf((0, 0, 0, 0), (2.5, -30, 0, 0))},
        "leg_lower_left_bone": {"rotation": kf((0, 0, 0, 0), (2.5, -40, 0, 0))},
        "cape_top_bone": {"rotation": kf((0, 0, 0, 0), (0.6, -20, 0, 0), (2.5, -60, 0, 0))},
    }
    trident_pitch(death, [(0, 0), (0.6, -20), (2.5, -60)])
    return {"idle": (T, "loop", hover), "swim": (S, "loop", swim), "attack_thrust": (1.0, "once", attack_thrust),
            "attack_swing": (1.2, "once", attack_swing), "skill_pull": (3.0, "once", skill_pull),
            "skill_charge": (2.6, "once", skill_charge), "skill_throw": (2.8, "once", skill_throw),
            "death": (2.5, "hold", death)}


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
    axe_pitch(attack_overhead, [(0, -30), (0.5, 55), (0.75, -90), (1.0, -90), (1.3, -30)])
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
    axe_pitch(attack_cleave, [(0, -30), (0.45, 40), (0.6, -90), (0.8, -90), (1.1, -30)])
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
    axe_pitch(skill_slash, [(0, -30), (1.2, 50), (1.4, -90), (1.9, -90), (2.5, -30)])
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
    axe_pitch(skill_quake, [(0, -30), (1.2, 62), (1.4, -90), (2.6, -90), (3.3, -30)])
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
    axe_pitch(leap_slam, [(0, -30), (0.5, -10), (1.0, 58), (1.15, 62), (1.6, -90), (1.9, -90), (2.4, -30)])
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
OBSOLETE = {"piglin_forgemaster": ("slam", "sweep", "enrage"), "deep_sea_poseidon": ("walk", "thrust", "summon"), "sky_wyvern": ("bite",)}


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
