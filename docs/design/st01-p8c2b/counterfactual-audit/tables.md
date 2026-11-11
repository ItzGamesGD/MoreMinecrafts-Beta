### Funnel (reproduced)

| Stage | Count |
|---|---|
| potential_anchors | 32000 |
| full_desert_footprint | 221 |
| terrain_qualified | 46 |
| entrance_qualified_sequential | 25 |
| fluid_qualified_after_entrance | 4 |
| actual_valid_starts_in_real_world | 4 |

Production verdict counts (unmodified `St01Candidate.decide`): `{"ELIGIBLE": 4, "REJECT_BIOME": 31779, "REJECT_CUT": 5, "REJECT_ENTRANCE": 21, "REJECT_FLUID": 21, "REJECT_HEIGHT_DIFFERENCE": 160, "REJECT_SKIRT": 10}`

### 2x2 contingency (terrain-qualified, n = 46)

| | Fluid PASS | Fluid FAIL | Row total |
|---|---|---|---|
| **Entrance PASS** | n11 = 4 | n10 = 21 | 25 |
| **Entrance FAIL** | n01 = 2 | n00 = 19 | 21 |
| Column total | 6 | 40 | 46 |

| Interaction measure | Value |
|---|---|
| P(entrance_fail\|fluid_fail) | 0.4750 |
| P(entrance_fail\|fluid_pass) | 0.3333 |
| P(fluid_fail\|entrance_fail) | 0.9048 |
| P(fluid_fail\|entrance_pass) | 0.8400 |
| expected_n00_if_independent | 18.2609 |
| fisher_exact_two_sided_p | 0.6734 |
| marginal_entrance_fail_rate | 0.4565 |
| marginal_fluid_fail_rate | 0.8696 |
| odds_ratio_entrance_pass_vs_fluid_pass | 1.8095 |
| phi | 0.0958 |

### Scenarios (area 8388.608 km2 = 20 seeds x 419.4304 km2; label: COUNTERFACTUAL_ELIGIBLE except A)

| Scenario | Entrance | Fluid | Eligible | Per 100 km2 | Poisson 95% | Cluster-bootstrap 95% | vs A (x) | vs A (abs) | Seeds with 0 | Mean/seed | Median/seed |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A | ON | ON | 4 | 0.048 | 0.013-0.122 | 0.000-0.107 | 1.00 | 0.000 | 17 | 0.20 | 0.0 |
| B | OFF | ON | 6 | 0.072 | 0.026-0.156 | 0.012-0.143 | 1.50 | 0.024 | 16 | 0.30 | 0.0 |
| C | ON | OFF | 25 | 0.298 | 0.193-0.440 | 0.203-0.405 | 6.25 | 0.250 | 5 | 1.25 | 1.0 |
| D | OFF | OFF | 46 | 0.548 | 0.401-0.731 | 0.393-0.703 | 11.50 | 0.501 | 2 | 2.30 | 2.0 |

Ratio vs A, paired seed-cluster bootstrap 95%: {"B": {"ci95": [1.0, 3.0], "defined_resamples": 19207, "of": 20000}, "C": {"ci95": [2.875, 26.0], "defined_resamples": 19207, "of": 20000}, "D": {"ci95": [5.0, 49.0], "defined_resamples": 19207, "of": 20000}}

### Per seed

| Seed | Anchors | Full Desert | Terrain-qualified | A | B | C | D | Actual starts |
|---|---|---|---|---|---|---|---|---|
| 1001 | 1600 | 17 | 4 | 0 | 0 | 1 | 4 | 0 |
| 1002 | 1600 | 18 | 5 | 0 | 0 | 3 | 5 | 0 |
| 1003 | 1600 | 4 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1004 | 1600 | 8 | 3 | 2 | 2 | 3 | 3 | 2 |
| 1005 | 1600 | 12 | 1 | 1 | 1 | 1 | 1 | 1 |
| 119599970 | 1600 | 11 | 4 | 0 | 0 | 3 | 4 | 0 |
| 218938829 | 1600 | 12 | 2 | 0 | 0 | 1 | 2 | 0 |
| 287862831 | 1600 | 11 | 4 | 0 | 0 | 2 | 4 | 0 |
| 318277688 | 1600 | 12 | 2 | 0 | 0 | 1 | 2 | 0 |
| 407538027 | 1600 | 9 | 4 | 0 | 0 | 2 | 4 | 0 |
| 417616547 | 1600 | 10 | 3 | 0 | 0 | 1 | 3 | 0 |
| 516955406 | 1600 | 8 | 1 | 0 | 0 | 1 | 1 | 0 |
| 527213223 | 1600 | 12 | 2 | 0 | 1 | 0 | 2 | 0 |
| 794814944 | 1600 | 8 | 1 | 0 | 0 | 1 | 1 | 0 |
| 914490140 | 1600 | 19 | 2 | 0 | 0 | 2 | 2 | 0 |
| 1034165336 | 1600 | 5 | 1 | 0 | 0 | 0 | 1 | 0 |
| 1421442253 | 1600 | 6 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1541117449 | 1600 | 31 | 4 | 1 | 2 | 1 | 4 | 1 |
| 1928394366 | 1600 | 7 | 2 | 0 | 0 | 2 | 2 | 0 |
| 2048069562 | 1600 | 1 | 1 | 0 | 0 | 0 | 1 | 0 |

### The terrain-qualified candidates (all 46)

| Seed | Chunk | Anchor (x,y,z) | Orient | Route | Max entrance obstruction | Fluid blocks (in volume / total) | Fluid class | Entrance | Fluid | Overlaps |
|---|---|---|---|---|---|---|---|---|---|---|
| 1001 | -477,-217 | -7624,69,-3464 | NORTH | EAST | +7 | 1280 / 1310 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 1001 | -463,458 | -7400,65,7336 | EAST | WEST | +2 | 71 / 71 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 1001 | -435,-279 | -6952,70,-4456 | WEST | WEST | +2 | 2132 / 2298 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | - |
| 1001 | 48,-434 | 776,69,-6936 | WEST | WEST | -1 | 6 / 52 | IN_VOLUME_SOLID_ONLY | pass | FAIL | village_desert |
| 1002 | 44,-147 | 712,72,-2344 | SOUTH | EAST | +0 | 159 / 159 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 1002 | 329,527 | 5272,71,8440 | NORTH | WEST | -4 | 174 / 292 | IN_VOLUME_SOLID_ONLY | pass | FAIL | - |
| 1002 | 366,-49 | 5864,69,-776 | SOUTH | EAST | +7 | 2836 / 3156 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | - |
| 1002 | 521,-210 | 8344,66,-3352 | SOUTH | WEST | +5 | 63 / 158 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 1002 | 613,-172 | 9816,67,-2744 | WEST | EAST | -1 | 336 / 438 | IN_VOLUME_SOLID_ONLY | pass | FAIL | - |
| 1004 | -571,-501 | -9128,68,-8008 | WEST | WEST | -4 | 0 / 0 | NONE | pass | pass | mineshaft |
| 1004 | -146,522 | -2328,66,8360 | NORTH | EAST | +1 | 74 / 105 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 1004 | -111,168 | -1768,70,2696 | EAST | WEST | +1 | 0 / 0 | NONE | pass | pass | - |
| 1005 | 7,296 | 120,69,4744 | EAST | WEST | +0 | 0 / 0 | NONE | pass | pass | - |
| 119599970 | -467,106 | -7464,63,1704 | EAST | WEST | +1 | 4339 / 4699 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 119599970 | -311,559 | -4968,72,8952 | SOUTH | WEST | -1 | 18 / 26 | IN_VOLUME_SOLID_ONLY | pass | FAIL | mineshaft |
| 119599970 | 10,629 | 168,66,10072 | NORTH | EAST | +5 | 1497 / 1508 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | - |
| 119599970 | 355,-15 | 5688,67,-232 | NORTH | WEST | +0 | 1637 / 1736 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | village_desert |
| 218938829 | -406,-565 | -6488,69,-9032 | SOUTH | EAST | +4 | 40 / 40 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 218938829 | 42,-597 | 680,70,-9544 | NORTH | EAST | -1 | 14735 / 15954 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 287862831 | -503,-308 | -8040,67,-4920 | SOUTH | WEST | +2 | 5784 / 6065 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 287862831 | 72,207 | 1160,66,3320 | NORTH | EAST | +3 | 3685 / 4226 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | mineshaft, mineshaft |
| 287862831 | 170,233 | 2728,67,3736 | WEST | EAST | +0 | 170 / 175 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | village_desert |
| 287862831 | 336,-215 | 5384,68,-3432 | NORTH | WEST | -2 | 7655 / 8083 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | mineshaft |
| 318277688 | 532,396 | 8520,68,6344 | NORTH | WEST | -4 | 3788 / 4257 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 318277688 | 560,389 | 8968,72,6232 | WEST | EAST | +3 | 28 / 43 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 407538027 | -376,-362 | -6008,66,-5784 | NORTH | EAST | -2 | 275 / 365 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 407538027 | -276,-256 | -4408,68,-4088 | SOUTH | WEST | +2 | 522 / 575 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | village_desert |
| 407538027 | -210,-241 | -3352,68,-3848 | WEST | EAST | +3 | 2698 / 2804 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | village_plains |
| 407538027 | -186,588 | -2968,71,9416 | EAST | EAST | +0 | 5164 / 5869 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 417616547 | -591,561 | -9448,67,8984 | EAST | EAST | +4 | 381 / 398 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 417616547 | 549,-45 | 8792,66,-712 | NORTH | EAST | +2 | 1 / 6 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |
| 417616547 | 586,-53 | 9384,75,-840 | NORTH | EAST | -4 | 996 / 1094 | IN_VOLUME_SOLID_ONLY | pass | FAIL | - |
| 516955406 | 197,260 | 3160,71,4168 | NORTH | EAST | -3 | 4439 / 5420 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 527213223 | -120,-301 | -1912,68,-4808 | EAST | WEST | +3 | 163 / 194 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | mineshaft, mineshaft |
| 527213223 | 622,-460 | 9960,65,-7352 | NORTH | WEST | +5 | 0 / 0 | NONE | FAIL | pass | - |
| 794814944 | 303,-241 | 4856,67,-3848 | NORTH | WEST | -3 | 14161 / 14503 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 914490140 | -468,489 | -7480,64,7832 | WEST | WEST | +0 | 3448 / 3709 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | village_desert |
| 914490140 | 431,230 | 6904,65,3688 | EAST | WEST | -1 | 2031 / 2364 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 1034165336 | 179,-213 | 2872,68,-3400 | EAST | EAST | +2 | 2886 / 3077 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | village_desert |
| 1541117449 | -408,486 | -6520,63,7784 | SOUTH | WEST | +3 | 8826 / 9880 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | - |
| 1541117449 | 109,-319 | 1752,68,-5096 | NORTH | WEST | +3 | 1468 / 1527 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | FAIL | FAIL | village_desert |
| 1541117449 | 301,198 | 4824,71,3176 | WEST | EAST | +3 | 0 / 0 | NONE | FAIL | pass | - |
| 1541117449 | 328,176 | 5256,65,2824 | EAST | EAST | +1 | 0 / 0 | NONE | pass | pass | mineshaft |
| 1928394366 | -497,430 | -7944,68,6888 | SOUTH | WEST | -3 | 229 / 232 | IN_VOLUME_INCLUDES_AUTHORED_CAVITY | pass | FAIL | - |
| 1928394366 | -84,-337 | -1336,70,-5384 | NORTH | EAST | -5 | 188 / 237 | IN_VOLUME_SOLID_ONLY | pass | FAIL | - |
| 2048069562 | 102,483 | 1640,73,7736 | NORTH | WEST | +2 | 266 / 324 | IN_VOLUME_SOLID_ONLY | FAIL | FAIL | - |

### Entrance failures (n = 21)

```json
{
 "columns_over_threshold_values": [
  4,
  4,
  5,
  5,
  7,
  7,
  7,
  7,
  11,
  13,
  13,
  15,
  20,
  24,
  25,
  25,
  25,
  25,
  25,
  25,
  25
 ],
 "excess_2_to_4": 12,
 "excess_5_to_10": 2,
 "excess_over_10": 0,
 "excess_over_threshold_eq_1": 7,
 "failing": 21,
 "inside_edge_row_rise_values": [
  2,
  2,
  2,
  2,
  2,
  2,
  3,
  3,
  3,
  3,
  3,
  3,
  3,
  4,
  4,
  5,
  5,
  5,
  5,
  6,
  6
 ],
 "max_obstruction_values": [
  2,
  2,
  2,
  2,
  2,
  2,
  2,
  3,
  3,
  3,
  3,
  3,
  3,
  3,
  4,
  4,
  5,
  5,
  5,
  7,
  7
 ],
 "obstruction_exceeds_approved_cut_limit_10": 0,
 "passing_max_obstruction_values": [
  -5,
  -4,
  -4,
  -4,
  -4,
  -3,
  -3,
  -3,
  -2,
  -2,
  -1,
  -1,
  -1,
  -1,
  -1,
  0,
  0,
  0,
  0,
  0,
  0,
  1,
  1,
  1,
  1
 ],
 "row_outside_footprint_all": true
}
```

### Fluid failures (n = 40)

```json
{
 "candidate_class_counts": {
  "IN_VOLUME_INCLUDES_AUTHORED_CAVITY": 24,
  "IN_VOLUME_SOLID_ONLY": 16
 },
 "candidates_with_fluid_in_cavity": 24,
 "candidates_with_fluid_in_unwritten_cells": 0,
 "candidates_with_in_volume_fluid_touching_boundary_cells": 37,
 "candidates_with_margin_fluid_adjacent_to_cavity": 0,
 "cell_classes": {
  "MARGIN_OUTSIDE_FRAME": 8780,
  "VOLUME_CAVITY_AIR": 10506,
  "VOLUME_SOLID": 88143
 },
 "failing": 40,
 "fluid_blocks_by_dy_band_8": {
  "-16": 30800,
  "-24": 38474,
  "-32": 20725,
  "-40": 1670,
  "-8": 15760
 },
 "fluid_blocks_by_dy_minus33_to_0": {
  "-1": 3245,
  "-10": 3682,
  "-11": 3654,
  "-12": 3611,
  "-13": 3593,
  "-14": 3803,
  "-15": 4317,
  "-16": 4272,
  "-17": 4581,
  "-18": 5263,
  "-19": 5759,
  "-2": 1338,
  "-20": 5694,
  "-21": 5005,
  "-22": 4674,
  "-23": 4097,
  "-24": 3401,
  "-25": 2873,
  "-26": 2755,
  "-27": 3077,
  "-28": 2715,
  "-29": 2449,
  "-3": 843,
  "-30": 2703,
  "-31": 2279,
  "-32": 1874,
  "-33": 1670,
  "-4": 1314,
  "-5": 1862,
  "-6": 2306,
  "-7": 2137,
  "-8": 2715,
  "-9": 3868
 },
 "in_volume_fluid_blocks_total": 98649,
 "lava_blocks_total": 0,
 "margin_only_fluid_blocks_total": 8780,
 "other_blocks_total": 0,
 "per_candidate_in_volume_fluid_blocks": [
  1,
  6,
  18,
  28,
  40,
  63,
  71,
  74,
  159,
  163,
  170,
  174,
  188,
  229,
  266,
  275,
  336,
  381,
  522,
  996,
  1280,
  1468,
  1497,
  1637,
  2031,
  2132,
  2698,
  2836,
  2886,
  3448,
  3685,
  3788,
  4339,
  4439,
  5164,
  5784,
  7655,
  8826,
  14161,
  14735
 ],
 "water_blocks_total": 107429,
 "would_pass_without_margin": 0
}
```

### Vanilla overlaps among terrain-qualified candidates

```json
{
 "by_scenario_eligible_with_overlap": {
  "A": 2,
  "B": 2,
  "C": 8,
  "D": 14
 },
 "by_structure": {
  "minecraft:mineshaft": 8,
  "minecraft:village_desert": 7,
  "minecraft:village_plains": 1
 },
 "terrain_qualified_candidates_with_any_overlap": 14
}
```

Integrity: `{"candidate_records": 221, "duplicate_candidate_records": 0, "orientation_counts_terrain_qualified": {"EAST": 10, "NORTH": 18, "SOUTH": 9, "WEST": 9}, "production_verdict_disagreements": [], "route_counts_terrain_qualified": {"EAST": 22, "WEST": 24}}`

