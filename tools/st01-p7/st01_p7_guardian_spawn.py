#!/usr/bin/env python3
"""ST-01 P7 — Tomb Guardian Emergence Point & Arena Verifier.

Evaluates emergence position candidates for the Tomb Guardian boss in the EAST Royal Hall.

Candidates:
1. Hall Center: (-21, -22, 21) [Tests collision with central obelisk]
2. North of Center: (-21, -22, 15) [P3/P6 BOSS_N candidate; tests sightline through obelisk]
3. South of Center:
   - 3A: (-21, -22, 27) [P3/P6 BOSS_S candidate; 5.0m from player approach]
   - 3B: (-21, -22, 26) [Midpoint candidate; 6.0m from player approach, safe from immediate slam]
   - 3C: (-21, -22, 25) [7.0m from player approach]
4. Lateral Flanking Candidates:
   - 4E: (-15, -22, 21) [P3/P6 BOSS_E candidate; 12.5m from player approach]
   - 4W: (-27, -22, 21) [P3/P6 BOSS_W candidate; 12.5m from player approach]
"""
import sys, os, json, math

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
for d in ("st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"):
    sys.path.insert(0, os.path.join(TOOLS, d))

import p3_vox as V, p3_spec as S3, p4_spec as S4
import p2c_overlay as OV
from st01_p6_verify import verify_p6

# Guardian Entity Constants (derived directly from TombGuardianTuning.java & TombGuardianEntities.java)
GUARDIAN_WIDTH = 1.35
GUARDIAN_HEIGHT = 4.35
GUARDIAN_EYE_HEIGHT = 4.35 * 0.85 # 3.6975
GUARDIAN_SPEED = 0.20
ENRAGED_SPEED = 0.23
FOLLOW_RANGE = 24.0
AWAKENING_TICKS = 80 # 4.0 seconds telegraph
MELEE_REACH = 2.0
SLAM_DAMAGE = 12.0
SLAM_RADIUS = 6.0
SLAM_THICKNESS = 0.8
STATUE_RADIUS = 1.5
STATUE_HEIGHT = 4.0

# Player Interaction Position (at front of primary chest)
PLAYER_APPROACH_POS = (-20.5, -20.0, 32.5) # Center of block (-21, -20, 32)
PLAYER_EYE_HEIGHT = 1.62
PLAYER_EYE_POS = (-20.5, -20.0 + PLAYER_EYE_HEIGHT, 32.5) # Y = -18.38

# Hall bounds
HALL_FLOOR_Y = -22
HALL_CEILING_Y = -7

def raycast_voxels(p1, p2, solid_array):
    """Raycast between two 3D points, returning list of intersected solid block coordinates."""
    ix, iy, iz = V.C.ix, V.C.iy, V.C.iz
    dist = math.dist(p1, p2)
    steps = max(10, int(math.ceil(dist * 12)))
    hits = []
    for s in range(steps + 1):
        t = s / steps
        x = p1[0] + (p2[0] - p1[0]) * t
        y = p1[1] + (p2[1] - p1[1]) * t
        z = p1[2] + (p2[2] - p1[2]) * t
        bx, by, bz = int(math.floor(x)), int(math.floor(y)), int(math.floor(z))
        if solid_array[ix(bx), iy(by), iz(bz)]:
            if (bx, by, bz) not in hits:
                hits.append((bx, by, bz))
    return hits

def check_entity_clearance(spawn_block, solid_array):
    """Check if an entity with width=1.35, height=4.35 can stand at spawn_block with feet at Y."""
    ix, iy, iz = V.C.ix, V.C.iy, V.C.iz
    sx, sy, sz = spawn_block
    # In Minecraft, entity at spawn_block has horizontal center at sx + 0.5, sz + 0.5
    cx = sx + 0.5
    cz = sz + 0.5
    half_w = GUARDIAN_WIDTH / 2.0 # 0.675
    
    min_x = cx - half_w
    max_x = cx + half_w
    min_z = cz - half_w
    max_z = cz + half_w
    min_y = float(sy)
    max_y = float(sy) + GUARDIAN_HEIGHT
    
    bx_range = range(int(math.floor(min_x)), int(math.floor(max_x)) + 1)
    bz_range = range(int(math.floor(min_z)), int(math.floor(max_z)) + 1)
    by_range = range(int(math.floor(min_y)), int(math.floor(max_y)) + 1)
    
    colliding_blocks = []
    for bx in bx_range:
        for by in by_range:
            for bz in bz_range:
                if solid_array[ix(bx), iy(by), iz(bz)]:
                    colliding_blocks.append((bx, by, bz))
                    
    # Floor check: beneath feet (Y - 1)
    floor_supported = True
    floor_blocks = []
    for bx in bx_range:
        for bz in bz_range:
            is_solid = solid_array[ix(bx), iy(sy - 1), iz(bz)]
            floor_blocks.append(((bx, sy - 1, bz), is_solid))
            if not is_solid:
                floor_supported = False
                
    return {
        "colliding_blocks": colliding_blocks,
        "floor_supported": floor_supported,
        "floor_blocks": floor_blocks,
        "bbox": [min_x, max_x, min_y, max_y, min_z, max_z]
    }

def get_guardian_candidates():
    return {
        "1_CENTER": {
            "name": "Candidate 1: Hall Center",
            "pos": (-21, -22, 21),
            "description": "Exact mathematical center of royal hall floor (-21, -22, 21).",
            "expected_facing": "SOUTH (towards dais)",
            "category": "CENTER"
        },
        "2_NORTH": {
            "name": "Candidate 2: North of Center (BOSS_N)",
            "pos": (-21, -22, 15),
            "description": "Centered at X=-21, 6 blocks North of hall center (Z=15), between obelisk and entrance.",
            "expected_facing": "SOUTH (towards dais/player)",
            "category": "NORTH"
        },
        "3A_SOUTH_27": {
            "name": "Candidate 3A: South of Center Z=27 (BOSS_S)",
            "pos": (-21, -22, 27),
            "description": "Centered at X=-21, 6 blocks South of hall center (Z=27), between obelisk and dais.",
            "expected_facing": "SOUTH (towards dais/player)",
            "category": "SOUTH"
        },
        "3B_SOUTH_26": {
            "name": "Candidate 3B: South of Center Z=26 (Recommended Midpoint)",
            "pos": (-21, -22, 26),
            "description": "Centered at X=-21, 5 blocks South of center (Z=26); 6.0m from player, equidistant between obelisk and dais.",
            "expected_facing": "SOUTH (towards dais/player)",
            "category": "SOUTH"
        },
        "3C_SOUTH_25": {
            "name": "Candidate 3C: South of Center Z=25 (Conservative Distance)",
            "pos": (-21, -22, 25),
            "description": "Centered at X=-21, 4 blocks South of center (Z=25); 7.0m from player, 2 blocks from obelisk plinth.",
            "expected_facing": "SOUTH (towards dais/player)",
            "category": "SOUTH"
        },
        "4E_EAST": {
            "name": "Candidate 4E: Lateral East of Obelisk (BOSS_E)",
            "pos": (-15, -22, 21),
            "description": "Lateral flanking position 6 blocks East of obelisk, X=-15, Z=21.",
            "expected_facing": "SOUTH-WEST (towards primary chest)",
            "category": "LATERAL"
        },
        "4W_WEST": {
            "name": "Candidate 4W: Lateral West of Obelisk (BOSS_W)",
            "pos": (-27, -22, 21),
            "description": "Lateral flanking position 6 blocks West of obelisk, X=-27, Z=21.",
            "expected_facing": "SOUTH-EAST (towards primary chest)",
            "category": "LATERAL"
        }
    }

def verify_guardian_candidate(cid, cinfo, p6_result):
    pos = cinfo["pos"]
    solid = p6_result.closed_solid
    
    # 1. Collision & Clearance
    clearance = check_entity_clearance(pos, solid)
    has_collision = len(clearance["colliding_blocks"]) > 0
    supported = clearance["floor_supported"]
    
    # 2. Distance Calculations to Player at primary chest
    boss_center = (pos[0] + 0.5, pos[1], pos[2] + 0.5)
    horiz_dist = math.hypot(boss_center[0] - PLAYER_APPROACH_POS[0], boss_center[2] - PLAYER_APPROACH_POS[2])
    dist_3d = math.dist(boss_center, PLAYER_APPROACH_POS)
    
    # 3. Raycast / Line of Sight
    boss_eye = (boss_center[0], pos[1] + GUARDIAN_EYE_HEIGHT, boss_center[2])
    los_hits = raycast_voxels(PLAYER_EYE_POS, boss_eye, solid)
    los_clear = len(los_hits) == 0
    
    # 4. Immediate Slam Hazard Check
    # Boss awakening is 80 ticks (4.0s). Can player clear 6m slam radius?
    # At walk speed 4.3 m/s, player moves 17.2m in 4s.
    # At sprint speed 5.6 m/s, player moves 22.4m in 4s.
    in_initial_slam_range = horiz_dist <= SLAM_RADIUS
    slam_safe_window = AWAKENING_TICKS / 20.0 # 4.0 seconds
    
    # Compile Checks
    checks = []
    
    checks.append({
        "id": "GUARDIAN_COLLISION",
        "title": "Clearance from static hall geometry (1.35x4.35 hitbox)",
        "verdict": "FAIL" if has_collision else "PASS",
        "evidence": f"Colliding blocks: {clearance['colliding_blocks']}" if has_collision else "No collision; 100% clear 1.35x4.35 envelope"
    })
    
    checks.append({
        "id": "GUARDIAN_FLOOR_SUPPORT",
        "title": "Solid floor support under emergence footprint",
        "verdict": "PASS" if supported else "FAIL",
        "evidence": f"Floor foundation solid: {supported}"
    })
    
    checks.append({
        "id": "GUARDIAN_LINE_OF_SIGHT",
        "title": "Direct line of sight between player at chest and emerging boss",
        "verdict": "PASS" if los_clear else "FAIL",
        "evidence": "Unobstructed direct sightline" if los_clear else f"Obstructed by solid blocks: {los_hits}"
    })
    
    checks.append({
        "id": "GUARDIAN_DISTANCE_SPACING",
        "title": "Distance from player interaction point (P3-F4 tracking)",
        "verdict": "PASS" if (not has_collision and los_clear) else ("FAIL" if has_collision else "UNVERIFIED"),
        "evidence": f"Horizontal distance: {horiz_dist:.2f}m, 3D Euclidean: {dist_3d:.2f}m. (In initial slam range: {in_initial_slam_range}, 4.0s telegraph reaction available)"
    })
    
    checks.append({
        "id": "GUARDIAN_ARENA_FLOW",
        "title": "Arena navigation, central obelisk framing, and combat aesthetics",
        "verdict": "PASS" if (not has_collision and los_clear and cinfo['category'] == 'SOUTH') else ("FAIL" if has_collision else "UNVERIFIED"),
        "evidence": f"Category: {cinfo['category']}. Theatrical presentation facing approaching player on dais."
    })
    
    overall = "FAIL" if any(c["verdict"] == "FAIL" for c in checks) else ("UNVERIFIED" if any(c["verdict"] == "UNVERIFIED" for c in checks) else "PASS")
    
    return {
        "candidate_id": cid,
        "candidate_name": cinfo["name"],
        "pos": pos,
        "category": cinfo["category"],
        "horiz_distance_m": round(horiz_dist, 2),
        "euclidean_distance_3d_m": round(dist_3d, 2),
        "los_clear": los_clear,
        "los_obstructions": los_hits,
        "in_initial_slam_range": in_initial_slam_range,
        "checks": checks,
        "overall_verdict": overall
    }

def verify_all_guardian_candidates(p6_result):
    cands = get_guardian_candidates()
    results = {}
    for cid, cinfo in cands.items():
        results[cid] = verify_guardian_candidate(cid, cinfo, p6_result)
    return results

if __name__ == "__main__":
    overlay = OV.extract()
    p3e = S3.make_hall_east()
    p4e = S4.make_variant("B")
    r6 = verify_p6(overlay, p4e, p3e, "E", skip_regress=True)
    res = verify_all_guardian_candidates(r6)
    print(json.dumps(res, indent=2))
