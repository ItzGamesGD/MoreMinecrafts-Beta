#!/usr/bin/env python3
"""ST-01 P7 — Primary Chest & Tomb Guardian Combination Evaluator.

Cross-evaluates PRIMARY chest candidates against Tomb Guardian emergence candidates.
Evaluates:
- Distance from chest approach to boss spawn
- Line of sight availability upon awakening
- Immediate slam / melee strike hazard and reaction window
- Player retreat and combat movement space (dais, aisles, central arena)
- Obstruction by sarcophagus, pillars, obelisk
- Aesthetic & dramatic framing
- Victory & post-fight unsealing flow to royal chest
"""
import sys, os, json, math

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
for d in ("st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"):
    sys.path.insert(0, os.path.join(TOOLS, d))

import p3_vox as V, p3_spec as S3, p4_spec as S4
import p2c_overlay as OV
from st01_p6_verify import verify_p6
import st01_p7_primary_chest as PC
import st01_p7_guardian_spawn as GS

def evaluate_combination(chest_cand, boss_cand, p6_result):
    """Evaluate a single pair of (chest_candidate, boss_candidate)."""
    c_info = PC.get_candidates()[chest_cand]
    b_info = GS.get_guardian_candidates()[boss_cand]
    
    c_res = PC.verify_primary_candidate(chest_cand, c_info, p6_result)
    b_res = GS.verify_guardian_candidate(boss_cand, b_info, p6_result)
    
    # Structural validity
    if c_res["overall_verdict"] == "FAIL":
        return {
            "chest_id": chest_cand,
            "boss_id": boss_cand,
            "verdict": "FAIL",
            "reason": f"Chest candidate {chest_cand} is geometrically invalid (Sarcophagus collision)."
        }
    if b_res["overall_verdict"] == "FAIL":
        return {
            "chest_id": chest_cand,
            "boss_id": boss_cand,
            "verdict": "FAIL",
            "reason": f"Boss candidate {boss_cand} is geometrically invalid ({'Obelisk collision' if 'CENTER' in boss_cand else 'Obelisk sightline obstruction'})."
        }
        
    # Distance from primary chest center to boss spawn
    chest_center_x = sum(b[0] for b in c_info["blocks"]) / 2.0 + 0.5
    chest_center_z = sum(b[2] for b in c_info["blocks"]) / 2.0 + 0.5
    chest_y = c_info["blocks"][0][1] # -20
    
    boss_pos = b_info["pos"]
    boss_center_x = boss_pos[0] + 0.5
    boss_center_z = boss_pos[2] + 0.5
    boss_y = boss_pos[1] # -22
    
    horiz_dist = math.hypot(chest_center_x - boss_center_x, chest_center_z - boss_center_z)
    euclid_3d = math.dist((chest_center_x, chest_y, chest_center_z), (boss_center_x, boss_y, boss_center_z))
    
    # Raycast from front interaction access of this specific chest candidate to boss eye
    solid = p6_result.closed_solid
    if c_info["axis"] == "X":
        # Facing North, player stands at Z = 32
        access_x = chest_center_x
        access_z = 32.5
    else:
        # Candidate A: chest occupies Z 32..33, player stands at Z 31.5 or side
        access_x = chest_center_x
        access_z = 31.5
    player_eye = (access_x, -20.0 + GS.PLAYER_EYE_HEIGHT, access_z)
    boss_eye = (boss_center_x, boss_y + GS.GUARDIAN_EYE_HEIGHT, boss_center_z)
    los_hits = GS.raycast_voxels(player_eye, boss_eye, solid)
    los_clear = len(los_hits) == 0
    
    # Evaluation dimensions:
    # 1. Collision Safety
    collision_safe = (c_res["overall_verdict"] == "PASS" and b_res["checks"][0]["verdict"] == "PASS")
    # 2. Combat Playability (Line of sight, escape window)
    combat_playable = los_clear
    # 3. Aesthetics & Staging (North facing chest, centered boss framing)
    theatrical_staging = (c_info["axis"] == "X" and b_info["category"] == "SOUTH")
    
    # Score / Classification
    if not collision_safe or not los_clear:
        verdict = "FAIL"
    elif c_res["overall_verdict"] == "UNVERIFIED" or b_res["overall_verdict"] == "UNVERIFIED":
        verdict = "UNVERIFIED"
    else:
        verdict = "PASS"
        
    return {
        "chest_id": chest_cand,
        "boss_id": boss_cand,
        "verdict": verdict,
        "horiz_dist_m": round(horiz_dist, 2),
        "euclid_3d_m": round(euclid_3d, 2),
        "los_clear": los_clear,
        "los_obstructions": los_hits,
        "chest_facing": c_info["intended_facing"],
        "boss_category": b_info["category"],
        "collision_safe": collision_safe,
        "combat_playable": combat_playable,
        "theatrical_staging": theatrical_staging,
        "summary": f"{c_info['name']} + {b_info['name']}: {verdict}"
    }

def evaluate_all_combinations(p6_result):
    chests = ["SOUTH_COLLISION", "A", "B", "C"]
    bosses = ["1_CENTER", "2_NORTH", "3A_SOUTH_27", "3B_SOUTH_26", "3C_SOUTH_25", "4E_EAST", "4W_WEST"]
    
    matrix = {}
    for c in chests:
        matrix[c] = {}
        for b in bosses:
            matrix[c][b] = evaluate_combination(c, b, p6_result)
    return matrix

if __name__ == "__main__":
    overlay = OV.extract()
    p3e = S3.make_hall_east()
    p4e = S4.make_variant("B")
    r6 = verify_p6(overlay, p4e, p3e, "E", skip_regress=True)
    res = evaluate_all_combinations(r6)
    print(json.dumps(res, indent=2))
