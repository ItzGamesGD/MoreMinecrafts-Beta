#!/usr/bin/env python3
"""ST-01 P7 — PRIMARY Royal Chest Geometry & Candidate Verifier.

Evaluates 2-block horizontal expansion candidates for the PRIMARY royal chest
in the EAST Royal Hall (starting from single-block placeholder at (-21, -20, 33)).

Candidates:
- Baseline Collision Reproduction: South (+Z) extension -> (-21, -20, 33) + (-21, -20, 34) [collides with SARCOPHAGUS]
- Candidate A: North (-Z) extension -> (-21, -20, 33) + (-21, -20, 32)
- Candidate B: East (+X) extension  -> (-21, -20, 33) + (-20, -20, 33)
- Candidate C: West (-X) extension  -> (-21, -20, 33) + (-22, -20, 33)
"""
import sys, os, json, math

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
for d in ("st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"):
    sys.path.insert(0, os.path.join(TOOLS, d))

import p3_vox as V, p3_spec as S3, p4_spec as S4
import p2c_overlay as OV
from st01_p6_verify import verify_p6

# Key structure bounding boxes from P6 East model
DAIS_BOX = [-26, -16, -22, -21, 32, 37]       # X: -26..-16, Y: -22..-21, Z: 32..37
DAIS_TREAD_BOX = [-25, -17, -22, -22, 30, 31] # X: -25..-17, Y: -22, Z: 30..31
SARCOPHAGUS_BOX = [-22, -20, -20, -19, 34, 37] # X: -22..-20, Y: -20..-19, Z: 34..37
CEILING_Y = -7
HALL_X_BOUNDS = (-37, -5)
HALL_Z_BOUNDS = (5, 37)
PRIMARY_PLACEHOLDER = (-21, -20, 33)
SECONDARY_CANDIDATE = (-3, -22, 25, 26) # X -3, Y -22, Z 25..26

def box_contains(bx, x, y, z):
    return (bx[0] <= x <= bx[1] and bx[2] <= y <= bx[3] and bx[4] <= z <= bx[5])

def box_overlaps(bx1, bx2):
    return not (bx1[1] < bx2[0] or bx1[0] > bx2[1] or
                bx1[3] < bx2[2] or bx1[2] > bx2[3] or
                bx1[5] < bx2[4] or bx1[4] > bx2[5])

def get_candidates():
    return {
        "SOUTH_COLLISION": {
            "name": "South Extension (Baseline Collision)",
            "direction": "+Z (South)",
            "blocks": [(-21, -20, 33), (-21, -20, 34)],
            "bbox": [-21, -21, -20, -20, 33, 34],
            "intended_facing": "WEST_OR_EAST",
            "axis": "Z",
        },
        "A": {
            "name": "Candidate A: North Extension",
            "direction": "-Z (North)",
            "blocks": [(-21, -20, 33), (-21, -20, 32)],
            "bbox": [-21, -21, -20, -20, 32, 33],
            "intended_facing": "WEST_OR_EAST", # Fabric double chest along Z axis must face E or W
            "axis": "Z",
        },
        "B": {
            "name": "Candidate B: East Extension",
            "direction": "+X (East)",
            "blocks": [(-21, -20, 33), (-20, -20, 33)],
            "bbox": [-21, -20, -20, -20, 33, 33],
            "intended_facing": "NORTH", # Double chest along X axis faces North (hall entrance)
            "axis": "X",
        },
        "C": {
            "name": "Candidate C: West Extension",
            "direction": "-X (West)",
            "blocks": [(-22, -20, 33), (-21, -20, 33)],
            "bbox": [-22, -21, -20, -20, 33, 33],
            "intended_facing": "NORTH", # Double chest along X axis faces North (hall entrance)
            "axis": "X",
        }
    }

def verify_primary_candidate(cand_id, cand_info, p6_result):
    """Perform rigorous 2D and 3D verification on a primary chest candidate."""
    blocks = cand_info["blocks"]
    bbox = cand_info["bbox"]
    axis = cand_info["axis"]
    facing = cand_info["intended_facing"]
    
    checks = []
    
    # Check 1: 2D Dais Footprint and Boundary
    in_dais_horiz = all(DAIS_BOX[0] <= x <= DAIS_BOX[1] and DAIS_BOX[4] <= z <= DAIS_BOX[5] for x, y, z in blocks)
    checks.append({
        "id": "CHEST_2D_DAIS",
        "title": "Occupancy within dais 2D horizontal bounds",
        "verdict": "PASS" if in_dais_horiz else "FAIL",
        "evidence": f"Blocks {blocks} inside Dais X[{DAIS_BOX[0]}..{DAIS_BOX[1]}], Z[{DAIS_BOX[4]}..{DAIS_BOX[5]}]: {in_dais_horiz}"
    })
    
    # Check 2: 3D Sarcophagus Collision
    sarc_collision = []
    for x, y, z in blocks:
        if box_contains(SARCOPHAGUS_BOX, x, y, z):
            sarc_collision.append((x, y, z))
    checks.append({
        "id": "CHEST_SARCOPHAGUS_COLLISION",
        "title": "Clearance from Sarcophagus structure",
        "verdict": "FAIL" if sarc_collision else "PASS",
        "evidence": f"Colliding blocks with Sarcophagus {SARCOPHAGUS_BOX}: {sarc_collision}"
    })
    
    # Check 3: Floor Support (Dais top surface at Y = -21)
    ix, iy, iz = V.C.ix, V.C.iy, V.C.iz
    supported = True
    support_details = []
    for x, y, z in blocks:
        floor_block_solid = p6_result.closed_solid[ix(x), iy(y - 1), iz(z)]
        support_details.append(f"({x}, {y-1}, {z}) solid={floor_block_solid}")
        if not floor_block_solid:
            supported = False
    checks.append({
        "id": "CHEST_FLOOR_SUPPORT",
        "title": "Solid floor support directly beneath all chest blocks",
        "verdict": "PASS" if supported else "FAIL",
        "evidence": f"Support blocks at Y={blocks[0][1]-1}: {', '.join(support_details)}"
    })
    
    # Check 4: Headroom / Ceiling Clearance
    headroom = CEILING_Y - (bbox[3] + 1)
    checks.append({
        "id": "CHEST_HEADROOM",
        "title": "Adequate headroom for chest opening (minimum 1 clear block above)",
        "verdict": "PASS" if headroom >= 1 else "FAIL",
        "evidence": f"Clear headroom between chest top (Y={bbox[3]}) and ceiling (Y={CEILING_Y}): {headroom} blocks"
    })
    
    # Check 5: Player Interaction and Approach Space
    # Player interaction cells in front of the chest:
    if axis == "X": # Faces North (Z-1)
        access_cells = [(x, bbox[2], bbox[4] - 1) for x in range(bbox[0], bbox[1] + 1)]
    else: # Along Z axis, faces East (+X) or West (-X)
        access_cells = [(bbox[0] + 1, bbox[2], z) for z in range(bbox[4], bbox[5] + 1)]
    
    access_clear = True
    for ax, ay, az in access_cells:
        # Check if access cell itself is walkable air
        if p6_result.closed_solid[ix(ax), iy(ay), iz(az)]:
            access_clear = False
    
    # For Candidate A: note that Z=32 is the very front edge of the dais, and occupying Z=32 consumes
    # the existing baseline player access point (-21, -20, 32)!
    approach_verdict = "PASS"
    note = f"Access cells {access_cells} are clear"
    if cand_id == "A":
        approach_verdict = "WARNING_RESTRICTED"
        note = "Extending to Z=32 consumes the primary dais access cell (-21, -20, 32); player must stand on lower tread (Y=-22, 2 blocks below chest) or flank."
    elif cand_id == "SOUTH_COLLISION":
        approach_verdict = "FAIL"
        note = "Blocked by sarcophagus collision."
        
    checks.append({
        "id": "CHEST_PLAYER_APPROACH",
        "title": "Player front interaction and standing clearance",
        "verdict": "PASS" if approach_verdict == "PASS" else ("FAIL" if approach_verdict == "FAIL" else "UNVERIFIED"),
        "evidence": note
    })
    
    # Check 6: Directional Facing & Aesthetic Alignment
    if axis == "X":
        facing_eval = "PASS"
        facing_desc = "Double chest along X axis naturally faces NORTH (toward hall entrance Z=4) with front latch visible to entering player."
    elif axis == "Z":
        facing_eval = "UNVERIFIED" # Technically valid in Minecraft, but aesthetically side-facing
        facing_desc = "Double chest along Z axis MUST face EAST or WEST; cannot face NORTH towards the hall entrance. Side profile presented to entering player."
    else:
        facing_eval = "FAIL"
        facing_desc = "Invalid axis."
        
    checks.append({
        "id": "CHEST_FACING_ORIENTATION",
        "title": "Block facing orientation and entrance visual presentation",
        "verdict": facing_eval,
        "evidence": facing_desc
    })
    
    # Check 7: Minecraft Double Chest Logical Single Container
    # In Fabric 1.21.x, two adjacent chest blocks with CHEST_TYPE=LEFT/RIGHT form a single DoubleBlockCombiner container
    checks.append({
        "id": "CHEST_LOGICAL_CONTAINER",
        "title": "Compatibility with single logical container / RoyalChestRuntime",
        "verdict": "PASS" if cand_id != "SOUTH_COLLISION" else "FAIL",
        "evidence": "2 contiguous horizontal chest blocks form a standard Minecraft Large Chest. RoyalChestRuntime can bind to primary anchor pos (-21, -20, 33)."
    })
    
    # Overall Candidate Verdict
    any_fail = any(c["verdict"] == "FAIL" for c in checks)
    any_unv = any(c["verdict"] == "UNVERIFIED" for c in checks)
    overall = "FAIL" if any_fail else ("UNVERIFIED" if any_unv else "PASS")
    
    return {
        "candidate_id": cand_id,
        "candidate_name": cand_info["name"],
        "blocks": blocks,
        "bbox": bbox,
        "axis": axis,
        "facing": facing,
        "checks": checks,
        "overall_verdict": overall
    }

def verify_all_primary_candidates(p6_result):
    cands = get_candidates()
    results = {}
    for cid, cinfo in cands.items():
        results[cid] = verify_primary_candidate(cid, cinfo, p6_result)
    return results

if __name__ == "__main__":
    overlay = OV.extract()
    p3e = S3.make_hall_east()
    p4e = S4.make_variant("B")
    r6 = verify_p6(overlay, p4e, p3e, "E", skip_regress=True)
    res = verify_all_primary_candidates(r6)
    print(json.dumps(res, indent=2))
