#!/usr/bin/env python3
"""Exports one CSV row per probe sample (structure coordinates + defect metrics).

Usage: python3 -I tools/worldgen-probe/export_csv.py <results_dir> > samples.csv
Coordinates are absolute block coordinates of the physical footprint (union of piece boxes), so any row can be
reproduced with `run_probe.sh` (same seed/placement/profile) and inspected in-game with /tp.
"""
import csv
import glob
import json
import os
import sys

COLS = ["shell", "adaptation", "profile", "placement", "seed", "start_chunk", "generated", "start_state",
        "min_x", "min_y", "min_z", "max_x", "max_y", "max_z", "core_pieces", "disconnected_core_pieces",
        "breach_leak_cells", "shell_holes", "interior_foreign", "interior_decoration", "interior_fluid",
        "exposed_air_faces", "exposed_water_faces", "exposed_lava_faces",
        "ring_air", "ring_water", "ring_lava", "ring_solid", "surface_y", "depth_below_surface", "mountain_biome",
        "overlapping_structures", "nearby_structures_16"]


def main(d):
    w = csv.writer(sys.stdout, lineterminator="\n")
    w.writerow(COLS)
    for f in sorted(glob.glob(os.path.join(d, "*.json"))):
        shell, adapt, profile, placement, seed = os.path.basename(f)[:-5].split("__")
        doc = json.load(open(f))
        for s in doc.get("samples", []):
            if not s.get("generated"):
                w.writerow([shell, adapt, profile, placement, seed, s["start_chunk"], False, s.get("start_state", "")] + [""] * (len(COLS) - 8))
                continue
            c = s["context"]
            box = [int(v) for v in s["box"].split(",")]
            deco = sum(v.get("interior_decoration_features", 0) for v in s["pieces"].values())
            w.writerow([shell, adapt, profile, placement, seed, s["start_chunk"], True, ""] + box + [
                s["core_pieces_present"], s["disconnected_core_pieces"], s["breach_leak_cells"], s["shell_holes_total"],
                s["interior_foreign_total"], deco, s["interior_fluid_total"], s["shell_exposed_faces_air"],
                s["shell_exposed_faces_water"], s["shell_exposed_faces_lava"], c["ring_air"], c["ring_water"], c["ring_lava"],
                c["ring_solid"], c["surface_y"], c["depth_below_surface"], c["mountain_biome"],
                "|".join(c["overlapping_structures"]), "|".join(c["nearby_structures_16"])])


if __name__ == "__main__":
    main(sys.argv[1])
