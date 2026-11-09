#!/usr/bin/env python3
"""
ST-01 Footprint & Bounding Volume Calculator
Reconstructs exact 3D bounding boxes, volumes, chunk coverage, and foundation parameters
from approved P1-P6 geometry specifications.
"""

import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

def calculate_footprint():
    # 1. Aboveground Outer Shell (P1 Approved Geometry)
    ag_min_x, ag_max_x = -40, 40
    ag_min_z, ag_max_z = -40, 40
    ag_min_y, ag_max_y = 0, 39
    ag_width = ag_max_x - ag_min_x + 1   # 81
    ag_length = ag_max_z - ag_min_z + 1  # 81
    ag_height = ag_max_y - ag_min_y + 1  # 40
    ag_box_volume = ag_width * ag_length * ag_height # 262,440

    # Stepped pyramid shell solid volume (uncarved)
    pyramid_solid_volume = sum((2 * (40 - y) + 1) ** 2 for y in range(40)) # 91,880

    # 2. Underground Bounding Box (P2-P6 Verified Elements)
    # Lowest structure block is Y = -32 (lowest cavity Y = -30, lowest envelope Y = -32)
    ug_min_y, ug_max_y = -32, -1
    ug_height = ug_max_y - ug_min_y + 1  # 32

    # East variant bounds
    ug_east_min_x, ug_east_max_x = -40, 39
    ug_east_min_z, ug_east_max_z = -28, 40
    ug_east_width = ug_east_max_x - ug_east_min_x + 1   # 80
    ug_east_length = ug_east_max_z - ug_east_min_z + 1  # 69

    # West variant bounds (exact X-mirror)
    ug_west_min_x, ug_west_max_x = -39, 40
    ug_west_min_z, ug_west_max_z = -28, 40
    ug_west_width = ug_west_max_x - ug_west_min_x + 1   # 80
    ug_west_length = ug_west_max_z - ug_west_min_z + 1  # 69

    # 3. Combined Bounding Box
    comb_min_x, comb_max_x = -40, 40
    comb_min_z, comb_max_z = -40, 40
    comb_min_y, comb_max_y = -32, 39
    comb_width = comb_max_x - comb_min_x + 1   # 81
    comb_length = comb_max_z - comb_min_z + 1  # 81
    comb_height = comb_max_y - comb_min_y + 1  # 72
    comb_volume = comb_width * comb_length * comb_height # 472,392

    # 4. Chunk Coverage Analysis
    # Analyze chunk count for an 81-block span across all 16 subchunk offsets (0..15)
    chunk_counts_81 = []
    for offset in range(16):
        min_b = offset - 40
        max_b = offset + 40
        min_c = math.floor(min_b / 16.0)
        max_c = math.floor(max_b / 16.0)
        chunks = int(max_c - min_c + 1)
        chunk_counts_81.append(chunks)

    # 2D chunk count for 81x81
    distinct_2d_chunks_81 = set()
    for dx in range(16):
        for dz in range(16):
            cx_span = math.floor((dx + 40) / 16.0) - math.floor((dx - 40) / 16.0) + 1
            cz_span = math.floor((dz + 40) / 16.0) - math.floor((dz - 40) / 16.0) + 1
            distinct_2d_chunks_81.add((int(cx_span), int(cz_span), int(cx_span * cz_span)))

    # Terrain buffer margin: 4-block perimeter skirt (89x89)
    distinct_2d_chunks_89 = set()
    for dx in range(16):
        for dz in range(16):
            cx_span = math.floor((dx + 44) / 16.0) - math.floor((dx - 44) / 16.0) + 1
            cz_span = math.floor((dz + 44) / 16.0) - math.floor((dz - 44) / 16.0) + 1
            distinct_2d_chunks_89.add((int(cx_span), int(cz_span), int(cx_span * cz_span)))

    result = {
        "status": "CALCULATED",
        "aboveground": {
            "outer_dimensions": {"width_x": ag_width, "length_z": ag_length, "height_y": ag_height},
            "box_coordinates": {"min_x": ag_min_x, "max_x": ag_max_x, "min_y": ag_min_y, "max_y": ag_max_y, "min_z": ag_min_z, "max_z": ag_max_z},
            "enclosing_volume": ag_box_volume,
            "stepped_shell_solid_volume": pyramid_solid_volume,
            "design_authority": "P1 Approved Specification (81x81x40)"
        },
        "underground": {
            "east": {
                "outer_dimensions": {"width_x": ug_east_width, "length_z": ug_east_length, "height_y": ug_height},
                "box_coordinates": {"min_x": ug_east_min_x, "max_x": ug_east_max_x, "min_y": ug_min_y, "max_y": ug_max_y, "min_z": ug_east_min_z, "max_z": ug_east_max_z},
                "lowest_structure_block_y": -32,
                "lowest_cavity_floor_y": -30
            },
            "west": {
                "outer_dimensions": {"width_x": ug_west_width, "length_z": ug_west_length, "height_y": ug_height},
                "box_coordinates": {"min_x": ug_west_min_x, "max_x": ug_west_max_x, "min_y": ug_min_y, "max_y": ug_max_y, "min_z": ug_west_min_z, "max_z": ug_west_max_z},
                "lowest_structure_block_y": -32,
                "lowest_cavity_floor_y": -30
            },
            "design_authority": "P2-P6 Verified Model & Overlay"
        },
        "combined": {
            "dimensions": {"width_x": comb_width, "length_z": comb_length, "height_y": comb_height},
            "box_coordinates": {"min_x": comb_min_x, "max_x": comb_max_x, "min_y": comb_min_y, "max_y": comb_max_y, "min_z": comb_min_z, "max_z": comb_max_z},
            "total_bounding_volume": comb_volume,
            "vertical_profile": {
                "aboveground_layers": 40,
                "underground_layers": 32,
                "total_layers": 72
            }
        },
        "chunk_coverage": {
            "unbuffered_81x81": {
                "span_x_chunks": 6,
                "span_z_chunks": 6,
                "total_chunks_covered": 36,
                "invariance_note": "Analytically invariant across all 256 subchunk coordinate phase offsets (always exactly 6x6=36 chunks)."
            },
            "buffered_89x89_with_skirt": {
                "possible_chunk_spans": sorted(list(distinct_2d_chunks_89)),
                "min_chunks": min(c[2] for c in distinct_2d_chunks_89),
                "max_chunks": max(c[2] for c in distinct_2d_chunks_89),
                "distribution_note": "Spans 36, 42, or 49 chunks depending on subchunk anchor alignment."
            }
        },
        "asset_audit": {
            "nbt_template_files_count": 0,
            "exported_nbt_status": "MISSING / NOT_EXPORTED",
            "model_representation": "Algorithmic voxel coordinate model (P1-P6 Python / Java builders)",
            "unverified_assumptions": [
                "NBT structure piece splitting (vanilla jigsaw 48x48 cap requires >=8 pieces)",
                "Foundation skirt maximum downward depth limit (unapproved)",
                "Perimeter terrain blending slope angle (unapproved)"
            ]
        }
    }

    out_file = os.path.join(ROOT, "docs", "design", "st01-o14", "st01_o14_footprint_bounds.json")
    with open(out_file, "w") as f:
        json.dump(result, f, indent=2)
    print(f"Wrote footprint bounds to {out_file}")
    return result

if __name__ == "__main__":
    calculate_footprint()
