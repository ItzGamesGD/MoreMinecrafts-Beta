#!/usr/bin/env python3 -I
"""Corrected Sky Tower root-piece height model (MATHEMATICALLY_DERIVED from the decompiled 26.3 JigsawPlacement; see
docs/audits/sky_tower_worldgen_height/phase2/CORRECTED_HEIGHT_MODEL.md).

Engine facts (each re-checked against runtime output by analyze.py):
  * root piece minY = start_height + T            where T = top surface block Y (= WORLD_SURFACE_WG first-free height - 1)
  * root piece maxY = minY + sizeY - 1
  * rejected iff maxY > getMaxY() - padding.top   (getMaxY() is INCLUSIVE: 319 in the Overworld) -> limit 315 for dimension_padding 4
  * hence accepted iff start_height <= LIM - T - (sizeY - 1)   (tower: sizeY = 97 -> start_height <= 219 - T)
"""
import csv
import math
import sys

MIN_Y, HEIGHT = -64, 384
MAX_Y_INCL = MIN_Y + HEIGHT - 1          # 319
PAD_TOP = 4
LIM = MAX_Y_INCL - PAD_TOP               # 315 : highest legal root-piece maxY (inclusive)
TOWER_SIZE_Y = 97                        # tower composite, measured from the NBT through the engine (elements.csv)
OTHER_SIZE_Y = {"plain_small_a": 23, "plain_small_b": 24, "plain_medium": 27, "ruins": 27, "temple": 45}
TERRAINS = [80, 120, 140, 160, 180, 200, 220, 240, 260]


def max_start(T, size_y=TOWER_SIZE_Y, lim=LIM):
    """Largest start_height with root-piece maxY <= lim at top-block terrain T."""
    return lim - T - (size_y - 1)


def accept(T, lo, hi, size_y=TOWER_SIZE_Y, lim=LIM):
    """(accepted rolls, total rolls, probability) for uniform integer start_height in [lo, hi]."""
    total = hi - lo + 1
    ok = max(0, min(hi, max_start(T, size_y, lim)) - lo + 1)
    return ok, total, ok / total


def max_feasible_terrain(start_min, size_y=TOWER_SIZE_Y, lim=LIM):
    """Highest terrain top-block Y for which even the smallest roll (start_min) is accepted."""
    return lim - start_min - (size_y - 1)


def full_success_terrain(start_max, size_y=TOWER_SIZE_Y, lim=LIM):
    """Highest terrain top-block Y for which EVERY roll up to start_max is accepted."""
    return lim - start_max - (size_y - 1)


def wilson(k, n, z=1.96):
    if n == 0:
        return (float("nan"), float("nan"))
    p = k / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (max(0.0, c - h), min(1.0, c + h))


def main(out):
    ranges = [("current 18..70", 18, 70), ("Phase1 15..35", 15, 35), ("18..35", 18, 35), ("10..30", 10, 30), ("5..20", 5, 20), ("1..18", 1, 18), ("1..10", 1, 10)]
    w = csv.writer(out)
    w.writerow(["range", "start_min", "start_max", "terrain_top_block_y", "terrain_first_free_y", "max_accepted_start_height", "accepted_rolls", "total_rolls",
                "tower_accept_probability_given_selected", "phase1_formula_probability", "difference_vs_phase1"])
    for name, lo, hi in ranges:
        for T in TERRAINS:
            ok, tot, p = accept(T, lo, hi)
            # Phase 1 formula taken literally with terrainY = top block Y: start_height <= 220 - terrainY
            ok1 = max(0, min(hi, 220 - T) - lo + 1)
            w.writerow([name, lo, hi, T, T + 1, max_start(T), ok, tot, f"{p:.4f}", f"{ok1 / tot:.4f}", f"{p - ok1 / tot:+.4f}"])


if __name__ == "__main__":
    main(sys.stdout)
