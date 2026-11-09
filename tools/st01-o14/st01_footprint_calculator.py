#!/usr/bin/env python3
"""
ST-01 Footprint & Bounding Volume Calculator (Corrected)
Reconstructs exact 3D bounding boxes, volumes, chunk coverage, and foundation parameters
from approved P1-P6 geometry specifications.

Distinguishes:
1. Structure body footprint (81x81)
2. Underground structure footprint (East / West, 80x69)
3. Foundation footprint (81x81 vertical downward projection)
4. Terrain correction / safety blending buffers (hypothetical 85x85, 89x89)
5. Actual required chunk-loading footprint analysis
"""

import json
import math
import os
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

def analyze_chunk_coverage(min_x_rel, max_x_rel, min_z_rel, max_z_rel):
    """
    Enumerate all 256 subchunk coordinate phase combinations (dx in 0..15, dz in 0..15)
    and calculate minimum, maximum, and full distribution of chunk counts.
    """
    counts = Counter()
    span_details = Counter()
    for dx in range(16):
        for dz in range(16):
            c_min_x = math.floor((dx + min_x_rel) / 16.0)
            c_max_x = math.floor((dx + max_x_rel) / 16.0)
            c_min_z = math.floor((dz + min_z_rel) / 16.0)
            c_max_z = math.floor((dz + max_z_rel) / 16.0)
            sx = int(c_max_x - c_min_x + 1)
            sz = int(c_max_z - c_min_z + 1)
            total = sx * sz
            counts[total] += 1
            span_details[(sx, sz, total)] += 1

    unique_spans = sorted([[sx, sz, total, count] for (sx, sz, total), count in span_details.items()])
    return {
        "dimensions": {
            "width_x": max_x_rel - min_x_rel + 1,
            "length_z": max_z_rel - min_z_rel + 1,
            "rel_bounds": {
                "min_x": min_x_rel, "max_x": max_x_rel,
                "min_z": min_z_rel, "max_z": max_z_rel
            }
        },
        "min_chunks": min(counts.keys()),
        "max_chunks": max(counts.keys()),
        "distribution_out_of_256": dict(sorted(counts.items())),
        "distinct_chunk_configurations": unique_spans
    }

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

    # 2. Underground Bounding Box (P4-B / P6 Verified Elements)
    # Adopted model: lowest cavity floor Y = -30, lowest envelope / structure block Y = -32
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

    # 3. Combined Bounding Box (P1 + P6 adopted Candidate B)
    comb_min_x, comb_max_x = -40, 40
    comb_min_z, comb_max_z = -40, 40
    comb_min_y, comb_max_y = -32, 39
    comb_width = comb_max_x - comb_min_x + 1   # 81
    comb_length = comb_max_z - comb_min_z + 1  # 81
    comb_height = comb_max_y - comb_min_y + 1  # 72
    comb_volume = comb_width * comb_length * comb_height # 472,392

    # 4. Independent Chunk Coverage Analysis across all 256 phases (dx in 0..15, dz in 0..15)
    cov_body = analyze_chunk_coverage(ag_min_x, ag_max_x, ag_min_z, ag_max_z)
    cov_ug_east = analyze_chunk_coverage(ug_east_min_x, ug_east_max_x, ug_east_min_z, ug_east_max_z)
    cov_ug_west = analyze_chunk_coverage(ug_west_min_x, ug_west_max_x, ug_west_min_z, ug_west_max_z)
    cov_foundation = analyze_chunk_coverage(ag_min_x, ag_max_x, ag_min_z, ag_max_z)
    cov_buffer_89 = analyze_chunk_coverage(-44, 44, -44, 44) # illustrative +4 block skirt
    cov_buffer_85 = analyze_chunk_coverage(-42, 42, -42, 42) # illustrative +2 block skirt

    result = {
        "status": "CALCULATED",
        "provenance": {
            "calculator_version": "Phase 1 corrected (2026-11-09)",
            "methodology": "Exact 256-phase discrete subchunk enumeration across all X/Z offsets (0..15)"
        },
        "aboveground": {
            "outer_dimensions": {"width_x": ag_width, "length_z": ag_length, "height_y": ag_height},
            "box_coordinates": {"min_x": ag_min_x, "max_x": ag_max_x, "min_y": ag_min_y, "max_y": ag_max_y, "min_z": ag_min_z, "max_z": ag_max_z},
            "enclosing_volume": ag_box_volume,
            "stepped_shell_solid_volume": pyramid_solid_volume,
            "design_authority": "P1 Approved Specification (81x81x40)"
        },
        "underground": {
            "model_adopted": "P4-B / P6 Candidate B (Lateral Shift)",
            "lowest_structure_block_y": -32,
            "lowest_cavity_floor_y": -30,
            "depth_reconciliation_note": (
                "P4-B/P6 verified model has lowest cavity floor Y=-30 and lowest structural block Y=-32. "
                "The P11 Q98 text ('floor Y=-36, lowest structure Y=-37') is an inconsistency with the adopted model; "
                "Q98 official maximum depth ceiling remains UNAPPROVED."
            ),
            "east": {
                "outer_dimensions": {"width_x": ug_east_width, "length_z": ug_east_length, "height_y": ug_height},
                "box_coordinates": {"min_x": ug_east_min_x, "max_x": ug_east_max_x, "min_y": ug_min_y, "max_y": ug_max_y, "min_z": ug_east_min_z, "max_z": ug_east_max_z}
            },
            "west": {
                "outer_dimensions": {"width_x": ug_west_width, "length_z": ug_west_length, "height_y": ug_height},
                "box_coordinates": {"min_x": ug_west_min_x, "max_x": ug_west_max_x, "min_y": ug_min_y, "max_y": ug_max_y, "min_z": ug_west_min_z, "max_z": ug_west_max_z}
            }
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
        "chunk_coverage_analysis": {
            "1_structure_body_81x81": {
                "category": "Structure Body Footprint",
                "status": "MATHEMATICALLY_INVARIANT",
                "analysis": cov_body,
                "invariance_proof": (
                    "Interval [anchor - 40, anchor + 40] has span 80 blocks. Because 80 = 5 * 16, "
                    "floor((dx + 40)/16) - floor((dx - 40)/16) == 5 for every integer dx in 0..15. "
                    "Adding 1 gives exactly 6 chunks on each axis, yielding strictly 6x6 = 36 chunks across all 256 phases."
                )
            },
            "2_underground_structure": {
                "category": "Underground Structure Footprint (80x69)",
                "east": cov_ug_east,
                "west": cov_ug_west,
                "note": "Spans 25, 30, or 36 chunks depending on anchor phase offset within subchunk."
            },
            "3_foundation_footprint": {
                "category": "Foundation Footprint",
                "analysis": cov_foundation,
                "note": "Downwards vertical extension directly beneath the 81x81 body; identical 36-chunk invariant."
            },
            "4_terrain_blending_buffer_hypothetical": {
                "category": "Safety / Blending Skirt Buffer (HYPOTHETICAL)",
                "buffer_plus_4_blocks_89x89": {
                    "analysis": cov_buffer_89,
                    "note": "Illustrative only; spans 36 (25%), 42 (50%), or 49 (25%) chunks. NOT a production default."
                },
                "buffer_plus_2_blocks_85x85": {
                    "analysis": cov_buffer_85,
                    "note": "Illustrative only; spans 36 (56.25%), 42 (37.5%), or 49 (6.25%) chunks."
                }
            },
            "5_actual_required_loading_footprint": {
                "conclusion": (
                    "While the unbuffered 81x81 body strictly occupies 36 chunks, any terrain-preparation, "
                    "blending skirt, or multi-piece placement operation requiring an outer buffer will cross into "
                    "neighboring chunks, spanning 36, 42, or 49 chunks. Worldgen ticket management MUST determine "
                    "chunk bounds dynamically from the active bounding envelope rather than assuming a fixed 36 chunks."
                )
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
    print(f"Wrote updated footprint bounds to {out_file}")
    return result

if __name__ == "__main__":
    calculate_footprint()
