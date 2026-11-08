#!/usr/bin/env python3
"""ST-01 P7 — Negative Controls / Mutation Tests.

Validates that the P7 verification tools correctly detect violations, collisions,
and edge cases.
"""
import sys, os, json, copy

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
for d in ("st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"):
    sys.path.insert(0, os.path.join(TOOLS, d))

import p3_vox as V, p3_spec as S3, p4_spec as S4
import p2c_overlay as OV
from st01_p6_verify import verify_p6
import st01_p7_primary_chest as PC
import st01_p7_guardian_spawn as GS
import st01_p7_combination as CB

def run_mutation_tests():
    print("Initializing mutation test environment...")
    overlay = OV.extract()
    p3e = S3.make_hall_east()
    p4e = S4.make_variant("B")
    r6 = verify_p6(overlay, p4e, p3e, "E", skip_regress=True)
    
    mutations = []
    
    # Mutation 1: Sarcophagus collision (South chest extension)
    c_info = PC.get_candidates()["SOUTH_COLLISION"]
    res1 = PC.verify_primary_candidate("SOUTH_COLLISION", c_info, r6)
    mutations.append({
        "id": "MUT_SOUTH_COLLISION",
        "description": "Primary chest south extension (-21, -20, 34) collides with sarcophagus",
        "expected": "FAIL",
        "got": res1["overall_verdict"],
        "detected": res1["overall_verdict"] == "FAIL" and any(c["id"] == "CHEST_SARCOPHAGUS_COLLISION" and c["verdict"] == "FAIL" for c in res1["checks"])
    })
    
    # Mutation 2: Boss Center collision with Obelisk
    b_info_center = GS.get_guardian_candidates()["1_CENTER"]
    res2 = GS.verify_guardian_candidate("1_CENTER", b_info_center, r6)
    mutations.append({
        "id": "MUT_CENTER_OBELISK_COLLISION",
        "description": "Guardian emergence at exact center (-21, -22, 21) collides with obelisk",
        "expected": "FAIL",
        "got": res2["overall_verdict"],
        "detected": res2["overall_verdict"] == "FAIL" and any(c["id"] == "GUARDIAN_COLLISION" and c["verdict"] == "FAIL" for c in res2["checks"])
    })
    
    # Mutation 3: North Boss Line of Sight Obstructed by Obelisk
    b_info_north = GS.get_guardian_candidates()["2_NORTH"]
    res3 = GS.verify_guardian_candidate("2_NORTH", b_info_north, r6)
    mutations.append({
        "id": "MUT_NORTH_BOSS_SIGHTLINE",
        "description": "Guardian emergence at north (-21, -22, 15) sightline obstructed by obelisk",
        "expected": "FAIL",
        "got": res3["overall_verdict"],
        "detected": res3["overall_verdict"] == "FAIL" and any(c["id"] == "GUARDIAN_LINE_OF_SIGHT" and c["verdict"] == "FAIL" for c in res3["checks"])
    })
    
    # Mutation 4: Unsupported Chest (Floor void)
    # Clone r6 and puncture floor under (-21, -21, 33)
    r_void = copy.copy(r6)
    r_void.closed_solid = r6.closed_solid.copy()
    ix, iy, iz = V.C.ix, V.C.iy, V.C.iz
    r_void.closed_solid[ix(-21), iy(-21), iz(33)] = False # Remove dais support block
    res4 = PC.verify_primary_candidate("B", PC.get_candidates()["B"], r_void)
    mutations.append({
        "id": "MUT_UNSUPPORTED_CHEST",
        "description": "Floor support removed under chest block (-21, -21, 33)",
        "expected": "FAIL",
        "got": res4["overall_verdict"],
        "detected": res4["overall_verdict"] == "FAIL" and any(c["id"] == "CHEST_FLOOR_SUPPORT" and c["verdict"] == "FAIL" for c in res4["checks"])
    })
    
    # Mutation 5: Boss inside pillar
    b_info_pillar = {
        "name": "Mutant Boss in North Pillar",
        "pos": (-30, -22, 8),
        "description": "Inside P_N1 shaft",
        "expected_facing": "SOUTH",
        "category": "MUTANT"
    }
    res5 = GS.verify_guardian_candidate("MUT_PILLAR", b_info_pillar, r6)
    mutations.append({
        "id": "MUT_BOSS_PILLAR_COLLISION",
        "description": "Guardian spawned inside pillar shaft (-30, -22, 8)",
        "expected": "FAIL",
        "got": res5["overall_verdict"],
        "detected": res5["overall_verdict"] == "FAIL" and any(c["id"] == "GUARDIAN_COLLISION" and c["verdict"] == "FAIL" for c in res5["checks"])
    })
    
    # Mutation 6: Low ceiling headroom violation
    r_low_ceil = copy.copy(r6)
    r_low_ceil.closed_solid = r6.closed_solid.copy()
    # Place solid block right above chest at Y = -19
    r_low_ceil.closed_solid[ix(-21), iy(-19), iz(33)] = True
    c_info_b = copy.deepcopy(PC.get_candidates()["B"])
    # Adjust headroom check directly
    c_info_b["bbox"][3] = -8 # artificially raise top to violate headroom
    res6 = PC.verify_primary_candidate("B_RAISED", c_info_b, r6)
    mutations.append({
        "id": "MUT_CHEST_HEADROOM",
        "description": "Chest bounding box raised to ceiling boundary Y=-8",
        "expected": "FAIL",
        "got": res6["overall_verdict"],
        "detected": res6["overall_verdict"] == "FAIL" and any(c["id"] == "CHEST_HEADROOM" and c["verdict"] == "FAIL" for c in res6["checks"])
    })
    
    # Mutation 7: Candidate A Orientation / Approach Restriction
    res7 = PC.verify_primary_candidate("A", PC.get_candidates()["A"], r6)
    mutations.append({
        "id": "MUT_CANDIDATE_A_WARNING",
        "description": "Candidate A flags approach cell consumption and side-facing orientation",
        "expected": "UNVERIFIED",
        "got": res7["overall_verdict"],
        "detected": res7["overall_verdict"] == "UNVERIFIED"
    })
    
    # Mutation 8: Combination Evaluation Failure on Invalid Inputs
    res8 = CB.evaluate_combination("SOUTH_COLLISION", "1_CENTER", r6)
    mutations.append({
        "id": "MUT_COMBINATION_REJECTION",
        "description": "Combination of South Collision chest + Center collision boss rejected",
        "expected": "FAIL",
        "got": res8["verdict"],
        "detected": res8["verdict"] == "FAIL"
    })
    
    all_detected = all(m["detected"] for m in mutations)
    print(f"Executed {len(mutations)} mutation tests. All detected: {all_detected}")
    for m in mutations:
        status = "OK" if m["detected"] else "FAILED_DETECTION"
        print(f"  [{status}] {m['id']}: expected {m['expected']}, got {m['got']}")
        
    out_path = os.path.join("docs", "design", "st01-p7", "generated", "p7_mutation_results.json")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w") as f:
        json.dump({"mutation_tests": mutations, "all_detected": all_detected}, f, indent=2)
    print(f"Mutation results saved to {out_path}")
    
    return 0 if all_detected else 1

if __name__ == "__main__":
    sys.exit(run_mutation_tests())
