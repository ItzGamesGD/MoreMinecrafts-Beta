# Coverage (20 seeds: [1001, 1002, 1003, 1004, 1005, 1034165336, 119599970, 1421442253, 1541117449, 1928394366, 2048069562, 218938829, 287862831, 318277688, 407538027, 417616547, 516955406, 527213223, 794814944, 914490140])

potential=32000 full_desert=221 area_km2=8388.6 total_probe_s=1300.0 height_computes=1625058

| seed | full Desert | D median eligible | B median eligible | B window-clamped eligible |
|---|---|---|---|---|
| 1001 | 17 | 4 | 14 | 14 |
| 1002 | 18 | 5 | 15 | 16 |
| 1003 | 4 | 0 | 2 | 2 |
| 1004 | 8 | 3 | 7 | 8 |
| 1005 | 12 | 1 | 7 | 8 |
| 1034165336 | 5 | 1 | 4 | 4 |
| 119599970 | 11 | 4 | 9 | 9 |
| 1421442253 | 6 | 0 | 3 | 4 |
| 1541117449 | 31 | 4 | 24 | 25 |
| 1928394366 | 7 | 2 | 6 | 6 |
| 2048069562 | 1 | 1 | 1 | 1 |
| 218938829 | 12 | 2 | 10 | 10 |
| 287862831 | 11 | 4 | 10 | 10 |
| 318277688 | 12 | 2 | 11 | 11 |
| 407538027 | 9 | 4 | 7 | 8 |
| 417616547 | 10 | 3 | 9 | 9 |
| 516955406 | 8 | 1 | 4 | 5 |
| 527213223 | 12 | 2 | 10 | 11 |
| 794814944 | 8 | 1 | 4 | 4 |
| 914490140 | 19 | 2 | 15 | 17 |

## Diff histogram (full-Desert candidates, bucket of 8)

| diff bucket | count |
|---|---|
| 0-7 | 7 |
| 8-15 | 44 |
| 16-23 | 53 |
| 24-31 | 43 |
| 32-39 | 33 |
| 40-47 | 14 |
| 48-55 | 10 |
| 56-63 | 4 |
| 64-71 | 7 |
| 72-79 | 4 |
| 80-+ | 2 |

## Eligible counts and density per plane

| policy | plane | eligible | per 100 km² | verdicts |
|---|---|---|---|---|
| D_16_10_8 | median | 46 | 0.548 | ELIGIBLE=46, REJECT_CUT=5, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=10 |
| D_16_10_8 | mean | 47 | 0.560 | ELIGIBLE=47, REJECT_CUT=4, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=10 |
| D_16_10_8 | trimmed10 | 47 | 0.560 | ELIGIBLE=47, REJECT_CUT=4, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=10 |
| D_16_10_8 | p25 | 45 | 0.536 | ELIGIBLE=45, REJECT_CUT=10, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=6 |
| D_16_10_8 | p40 | 46 | 0.548 | ELIGIBLE=46, REJECT_CUT=7, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=8 |
| D_16_10_8 | p60 | 41 | 0.489 | ELIGIBLE=41, REJECT_CUT=5, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=15 |
| D_16_10_8 | p75 | 38 | 0.453 | ELIGIBLE=38, REJECT_HEIGHT_DIFFERENCE=160, REJECT_SKIRT=23 |
| D_16_10_8 | midrange | 61 | 0.727 | ELIGIBLE=61, REJECT_HEIGHT_DIFFERENCE=160 |
| D_16_10_8 | window_clamped_median | 61 | 0.727 | ELIGIBLE=61, REJECT_HEIGHT_DIFFERENCE=160 |
| B_40_24_32 | median | 172 | 2.050 | ELIGIBLE=172, REJECT_CUT=10, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | mean | 174 | 2.074 | ELIGIBLE=174, REJECT_CUT=8, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | trimmed10 | 172 | 2.050 | ELIGIBLE=172, REJECT_CUT=10, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | p25 | 162 | 1.931 | ELIGIBLE=162, REJECT_CUT=20, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | p40 | 165 | 1.967 | ELIGIBLE=165, REJECT_CUT=17, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | p60 | 173 | 2.062 | ELIGIBLE=173, REJECT_CUT=8, REJECT_HEIGHT_DIFFERENCE=39, REJECT_SKIRT=1 |
| B_40_24_32 | p75 | 175 | 2.086 | ELIGIBLE=175, REJECT_CUT=3, REJECT_HEIGHT_DIFFERENCE=39, REJECT_SKIRT=4 |
| B_40_24_32 | midrange | 182 | 2.170 | ELIGIBLE=182, REJECT_HEIGHT_DIFFERENCE=39 |
| B_40_24_32 | window_clamped_median | 182 | 2.170 | ELIGIBLE=182, REJECT_HEIGHT_DIFFERENCE=39 |

## Correction metrics on eligible candidates (median / p90 / max)

| policy | plane | n | cut | skirt | excavation blocks | excavation cols | exposed body face sum | exposed body face max | boundary cliff sum | boundary cliff max |
|---|---|---|---|---|---|---|---|---|---|---|
| D_16_10_8 | median | 46 | 5 / p90 8 / max 10 | 6 / p90 8 / max 8 | 2927 / p90 8293 / max 11332 | 1392 / p90 2449 / max 2829 | 352 / p90 797 / max 1112 | 5 / p90 8 / max 11 | 198 / p90 549 / max 728 | 4 / p90 7 / max 10 |
| D_16_10_8 | mean | 47 | 5 / p90 8 / max 10 | 5 / p90 8 / max 8 | 3082 / p90 7736 / max 11332 | 1580 / p90 2220 / max 2943 | 391 / p90 686 / max 1112 | 5 / p90 8 / max 10 | 225 / p90 438 / max 728 | 4 / p90 7 / max 10 |
| D_16_10_8 | trimmed10 | 47 | 5 / p90 8 / max 10 | 6 / p90 8 / max 8 | 3082 / p90 7736 / max 11332 | 1605 / p90 2308 / max 2943 | 408 / p90 686 / max 1112 | 5 / p90 8 / max 10 | 225 / p90 507 / max 728 | 5 / p90 7 / max 10 |
| D_16_10_8 | p25 | 45 | 7 / p90 9 / max 10 | 4 / p90 7 / max 8 | 7404 / p90 14255 / max 19222 | 3050 / p90 4201 / max 4446 | 135 / p90 297 / max 633 | 3 / p90 6 / max 10 | 447 / p90 851 / max 1083 | 5 / p90 8 / max 10 |
| D_16_10_8 | p40 | 46 | 6 / p90 9 / max 10 | 6 / p90 7 / max 8 | 4351 / p90 11068 / max 14045 | 2036 / p90 3066 / max 3238 | 298 / p90 656 / max 979 | 5 / p90 7 / max 10 | 248 / p90 633 / max 728 | 5 / p90 8 / max 10 |
| D_16_10_8 | p60 | 41 | 5 / p90 8 / max 9 | 6 / p90 8 / max 8 | 1530 / p90 5253 / max 8921 | 1022 / p90 1760 / max 2263 | 423 / p90 870 / max 1283 | 5 / p90 8 / max 11 | 119 / p90 409 / max 557 | 3 / p90 6 / max 9 |
| D_16_10_8 | p75 | 38 | 4 / p90 8 / max 10 | 6 / p90 8 / max 8 | 710 / p90 2836 / max 3744 | 426 / p90 1116 / max 1442 | 593 / p90 1129 / max 1559 | 6 / p90 8 / max 12 | 85 / p90 275 / max 557 | 2 / p90 7 / max 9 |
| D_16_10_8 | midrange | 61 | 6 / p90 8 / max 8 | 6 / p90 8 / max 8 | 4160 / p90 14792 / max 24871 | 2017 / p90 5069 / max 6180 | 357 / p90 937 / max 1363 | 5 / p90 8 / max 12 | 275 / p90 612 / max 1069 | 5 / p90 7 / max 8 |
| D_16_10_8 | window_clamped_median | 61 | 6 / p90 10 / max 10 | 6 / p90 8 / max 8 | 3608 / p90 10552 / max 24871 | 1806 / p90 3284 / max 5549 | 324 / p90 757 / max 1112 | 5 / p90 8 / max 12 | 230 / p90 654 / max 850 | 5 / p90 8 / max 10 |
| B_40_24_32 | median | 172 | 8 / p90 18 / max 24 | 10 / p90 21 / max 32 | 5258 / p90 13915 / max 28733 | 1953 / p90 2781 / max 2990 | 499 / p90 1094 / max 2553 | 7 / p90 16 / max 55 | 342 / p90 944 / max 1569 | 6 / p90 16 / max 23 |
| B_40_24_32 | mean | 174 | 8 / p90 18 / max 24 | 10 / p90 21 / max 32 | 5231 / p90 12783 / max 24164 | 1950 / p90 2844 / max 3943 | 566 / p90 1112 / max 1907 | 7 / p90 16 / max 54 | 347 / p90 857 / max 1344 | 6 / p90 15 / max 24 |
| B_40_24_32 | trimmed10 | 172 | 8 / p90 18 / max 24 | 10 / p90 21 / max 32 | 5232 / p90 13683 / max 24552 | 1954 / p90 2761 / max 3602 | 538 / p90 1112 / max 1881 | 7 / p90 16 / max 54 | 342 / p90 879 / max 1371 | 6 / p90 15 / max 23 |
| B_40_24_32 | p25 | 162 | 10 / p90 19 / max 24 | 7 / p90 20 / max 31 | 10962 / p90 24674 / max 45789 | 3492 / p90 4363 / max 4627 | 214 / p90 552 / max 1161 | 5 / p90 14 / max 53 | 619 / p90 1265 / max 2368 | 8 / p90 17 / max 23 |
| B_40_24_32 | p40 | 165 | 9 / p90 18 / max 24 | 9 / p90 21 / max 32 | 7114 / p90 17277 / max 35441 | 2534 / p90 3378 / max 3782 | 389 / p90 876 / max 1578 | 6 / p90 16 / max 54 | 440 / p90 1051 / max 1760 | 7 / p90 16 / max 22 |
| B_40_24_32 | p60 | 173 | 7 / p90 18 / max 24 | 11 / p90 22 / max 32 | 3778 / p90 10326 / max 21978 | 1447 / p90 2174 / max 2466 | 669 / p90 1385 / max 3210 | 8 / p90 17 / max 55 | 280 / p90 800 / max 1268 | 6 / p90 15 / max 24 |
| B_40_24_32 | p75 | 175 | 6 / p90 15 / max 24 | 13 / p90 23 / max 30 | 1584 / p90 6055 / max 14775 | 777 / p90 1361 / max 1540 | 1023 / p90 2157 / max 3565 | 10 / p90 19 / max 56 | 134 / p90 526 / max 1455 | 4 / p90 13 / max 24 |
| B_40_24_32 | midrange | 182 | 11 / p90 17 / max 20 | 11 / p90 17 / max 20 | 8842 / p90 43876 / max 85783 | 2500 / p90 6359 / max 6556 | 417 / p90 2073 / max 4150 | 8 / p90 16 / max 46 | 505 / p90 2117 / max 4310 | 9 / p90 16 / max 19 |
| B_40_24_32 | window_clamped_median | 182 | 8 / p90 21 / max 24 | 10 / p90 21 / max 32 | 5738 / p90 14722 / max 28733 | 1932 / p90 2774 / max 2990 | 521 / p90 1352 / max 2714 | 7 / p90 16 / max 55 | 373 / p90 983 / max 1691 | 7 / p90 18 / max 24 |

## Overlap matrix (B-eligible-plane candidates: 182)

candidates with any overlap: 45

| structure [step] | occurrences |
|---|---|
| minecraft:mineshaft [UNDERGROUND_STRUCTURES] | 19 |
| minecraft:village_desert [SURFACE_STRUCTURES] | 17 |
| minecraft:desert_pyramid [SURFACE_STRUCTURES] | 7 |
| minecraft:ruined_portal_desert [SURFACE_STRUCTURES] | 3 |
| minecraft:pillager_outpost [SURFACE_STRUCTURES] | 2 |
| minecraft:mineshaft_mesa [UNDERGROUND_STRUCTURES] | 1 |
- seed 1001 chunk -398,-180 diff 26: minecraft:desert_pyramid@-401,-183 step=SURFACE_STRUCTURES pieces=1 vol_cells=375 exc_cells=6 edge_crossing=1
- seed 1001 chunk -18,-368 diff 23: minecraft:desert_pyramid@-20,-371 step=SURFACE_STRUCTURES pieces=1 vol_cells=1575 exc_cells=0 edge_crossing=1
- seed 1001 chunk 48,-434 diff 9: minecraft:village_desert@50,-430 step=SURFACE_STRUCTURES pieces=30 vol_cells=19682 exc_cells=181 edge_crossing=7
- seed 1002 chunk -80,-459 diff 30: minecraft:mineshaft@-81,-463 step=UNDERGROUND_STRUCTURES pieces=9 vol_cells=212 exc_cells=0 edge_crossing=4
- seed 1002 chunk 8,-87 diff 24: minecraft:mineshaft_mesa@15,-86 step=UNDERGROUND_STRUCTURES pieces=6 vol_cells=897 exc_cells=0 edge_crossing=1
- seed 1002 chunk 353,-628 diff 25: minecraft:village_desert@354,-630 step=SURFACE_STRUCTURES pieces=59 vol_cells=28049 exc_cells=451 edge_crossing=7
- seed 1002 chunk 396,593 diff 18: minecraft:mineshaft@393,600 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=90 exc_cells=0 edge_crossing=1
- seed 1002 chunk 495,-208 diff 35: minecraft:mineshaft@498,-206 step=UNDERGROUND_STRUCTURES pieces=17 vol_cells=524 exc_cells=0 edge_crossing=2
- seed 1003 chunk 612,100 diff 29: minecraft:mineshaft@613,102 step=UNDERGROUND_STRUCTURES pieces=38 vol_cells=2438 exc_cells=0 edge_crossing=8
- seed 1004 chunk -589,-474 diff 24: minecraft:pillager_outpost@-589,-474 step=SURFACE_STRUCTURES pieces=8 vol_cells=22470 exc_cells=2324 edge_crossing=0
- seed 1004 chunk -571,-501 diff 10: minecraft:mineshaft@-577,-499 step=UNDERGROUND_STRUCTURES pieces=8 vol_cells=535 exc_cells=0 edge_crossing=5
- seed 1005 chunk 618,-500 diff 21: minecraft:desert_pyramid@619,-500 step=SURFACE_STRUCTURES pieces=1 vol_cells=6615 exc_cells=0 edge_crossing=0
- seed 1034165336 chunk 118,205 diff 27: minecraft:mineshaft@119,207 step=UNDERGROUND_STRUCTURES pieces=27 vol_cells=1810 exc_cells=0 edge_crossing=0
- seed 1034165336 chunk 179,-213 diff 12: minecraft:village_desert@178,-217 step=SURFACE_STRUCTURES pieces=5 vol_cells=2064 exc_cells=43 edge_crossing=5
- seed 119599970 chunk -311,559 diff 12: minecraft:mineshaft@-315,560 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=0 exc_cells=0 edge_crossing=0
- seed 119599970 chunk 355,-15 diff 10: minecraft:village_desert@358,-16 step=SURFACE_STRUCTURES pieces=31 vol_cells=13881 exc_cells=0 edge_crossing=5
- seed 119599970 chunk 358,21 diff 29: minecraft:village_desert@358,17 step=SURFACE_STRUCTURES pieces=2 vol_cells=6080 exc_cells=146 edge_crossing=1
- seed 1541117449 chunk -624,-141 diff 25: minecraft:ruined_portal_desert@-625,-139 step=SURFACE_STRUCTURES pieces=1 vol_cells=700 exc_cells=345 edge_crossing=0
- seed 1541117449 chunk -370,425 diff 34: minecraft:village_desert@-374,425 step=SURFACE_STRUCTURES pieces=6 vol_cells=4736 exc_cells=188 edge_crossing=4
- seed 1541117449 chunk 109,-319 diff 8: minecraft:village_desert@106,-322 step=SURFACE_STRUCTURES pieces=15 vol_cells=10750 exc_cells=17 edge_crossing=4
- seed 1541117449 chunk 214,330 diff 23: minecraft:ruined_portal_desert@215,330 step=SURFACE_STRUCTURES pieces=1 vol_cells=648 exc_cells=0 edge_crossing=0
- seed 1541117449 chunk 209,417 diff 36: minecraft:ruined_portal_desert@212,418 step=SURFACE_STRUCTURES pieces=1 vol_cells=49 exc_cells=28 edge_crossing=1
- seed 1541117449 chunk 274,180 diff 29: minecraft:mineshaft@275,173 step=UNDERGROUND_STRUCTURES pieces=3 vol_cells=96 exc_cells=0 edge_crossing=2
- seed 1541117449 chunk 274,180 diff 29: minecraft:mineshaft@271,184 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=54 exc_cells=0 edge_crossing=1
- seed 1541117449 chunk 328,176 diff 7: minecraft:mineshaft@322,179 step=UNDERGROUND_STRUCTURES pieces=5 vol_cells=486 exc_cells=0 edge_crossing=3
- seed 218938829 chunk -598,203 diff 24: minecraft:village_desert@-599,205 step=SURFACE_STRUCTURES pieces=56 vol_cells=33648 exc_cells=276 edge_crossing=8
- seed 218938829 chunk 73,-535 diff 27: minecraft:village_desert@68,-534 step=SURFACE_STRUCTURES pieces=2 vol_cells=0 exc_cells=0 edge_crossing=2
- seed 218938829 chunk 618,-525 diff 32: minecraft:village_desert@622,-524 step=SURFACE_STRUCTURES pieces=10 vol_cells=10233 exc_cells=345 edge_crossing=4
- seed 287862831 chunk -499,-375 diff 36: minecraft:village_desert@-503,-374 step=SURFACE_STRUCTURES pieces=8 vol_cells=5639 exc_cells=161 edge_crossing=3
- seed 287862831 chunk -499,-375 diff 36: minecraft:desert_pyramid@-498,-375 step=SURFACE_STRUCTURES pieces=1 vol_cells=6615 exc_cells=537 edge_crossing=0
- seed 287862831 chunk -207,454 diff 22: minecraft:desert_pyramid@-204,453 step=SURFACE_STRUCTURES pieces=1 vol_cells=315 exc_cells=30 edge_crossing=1
- seed 287862831 chunk 72,207 diff 12: minecraft:mineshaft@68,206 step=UNDERGROUND_STRUCTURES pieces=5 vol_cells=155 exc_cells=0 edge_crossing=0
- seed 287862831 chunk 72,207 diff 12: minecraft:mineshaft@74,213 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=0 exc_cells=0 edge_crossing=0
- seed 287862831 chunk 142,270 diff 24: minecraft:mineshaft@144,265 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=12 exc_cells=0 edge_crossing=1
- seed 287862831 chunk 178,208 diff 20: minecraft:village_desert@181,214 step=SURFACE_STRUCTURES pieces=10 vol_cells=10935 exc_cells=0 edge_crossing=2
- seed 318277688 chunk -430,70 diff 20: minecraft:mineshaft@-424,68 step=UNDERGROUND_STRUCTURES pieces=7 vol_cells=698 exc_cells=0 edge_crossing=4
- seed 318277688 chunk 620,-275 diff 19: minecraft:mineshaft@620,-278 step=UNDERGROUND_STRUCTURES pieces=61 vol_cells=4785 exc_cells=0 edge_crossing=16
- seed 417616547 chunk -410,70 diff 19: minecraft:mineshaft@-405,68 step=UNDERGROUND_STRUCTURES pieces=2 vol_cells=0 exc_cells=0 edge_crossing=1
- seed 417616547 chunk -185,-340 diff 37: minecraft:village_desert@-186,-334 step=SURFACE_STRUCTURES pieces=3 vol_cells=3654 exc_cells=3 edge_crossing=1
- seed 516955406 chunk -459,548 diff 30: minecraft:mineshaft@-456,548 step=UNDERGROUND_STRUCTURES pieces=1 vol_cells=36 exc_cells=0 edge_crossing=1
- seed 516955406 chunk -459,548 diff 30: minecraft:desert_pyramid@-460,545 step=SURFACE_STRUCTURES pieces=1 vol_cells=1575 exc_cells=0 edge_crossing=1
- seed 527213223 chunk -573,-149 diff 14: minecraft:mineshaft@-570,-147 step=UNDERGROUND_STRUCTURES pieces=84 vol_cells=7266 exc_cells=0 edge_crossing=10
- seed 527213223 chunk -182,-307 diff 37: minecraft:village_desert@-179,-303 step=SURFACE_STRUCTURES pieces=45 vol_cells=30958 exc_cells=258 edge_crossing=11
- seed 527213223 chunk -120,-301 diff 12: minecraft:mineshaft@-118,-298 step=UNDERGROUND_STRUCTURES pieces=2 vol_cells=30 exc_cells=0 edge_crossing=1
- seed 794814944 chunk 298,-154 diff 17: minecraft:desert_pyramid@300,-156 step=SURFACE_STRUCTURES pieces=1 vol_cells=5355 exc_cells=0 edge_crossing=1
- seed 914490140 chunk -468,489 diff 4: minecraft:village_desert@-469,495 step=SURFACE_STRUCTURES pieces=6 vol_cells=4863 exc_cells=0 edge_crossing=2
- seed 914490140 chunk 17,102 diff 22: minecraft:pillager_outpost@13,106 step=SURFACE_STRUCTURES pieces=1 vol_cells=0 exc_cells=0 edge_crossing=1
- seed 914490140 chunk 294,167 diff 20: minecraft:village_desert@292,170 step=SURFACE_STRUCTURES pieces=44 vol_cells=36633 exc_cells=382 edge_crossing=10
- seed 914490140 chunk 460,240 diff 14: minecraft:village_desert@460,240 step=SURFACE_STRUCTURES pieces=98 vol_cells=61919 exc_cells=1522 edge_crossing=3

## Search (first eligible anchor in ring order)

| seed | variant | order | ring | first eligible | candidates | height computes | ms |
|---|---|---|---|---|---|---|---|
| 1001 | A | 0 | ring 14 | -435,-279 | 735 | 56092 | 24864 |
| 1001 | B | 1 | ring 8 | 80,-247 | 261 | 7130 | 3168 |
| 1001 | C | 2 | ring 8 | 80,-247 | 261 | 7130 | 3221 |
| 1001 | C | 3 | ring 8 | 80,-247 | 261 | 7130 | 3232 |
| 1001 | B | 4 | ring 8 | 80,-247 | 261 | 7130 | 3251 |
| 1001 | A | 5 | ring 14 | -435,-279 | 735 | 56092 | 24968 |
| 1003 | A | 0 | ring -1 | None | 1681 | 30986 | 14693 |
| 1003 | B | 1 | ring 18 | -45,596 | 1294 | 22756 | 10737 |
| 1003 | C | 2 | ring 18 | -45,596 | 1294 | 16410 | 7838 |
| 1003 | C | 3 | ring 18 | -45,596 | 1294 | 16410 | 7713 |
| 1003 | B | 4 | ring 18 | -45,596 | 1294 | 22756 | 10773 |
| 1003 | A | 5 | ring -1 | None | 1681 | 30986 | 14519 |
| 1004 | A | 0 | ring 5 | -111,168 | 94 | 6738 | 3021 |
| 1004 | B | 1 | ring 5 | -111,168 | 94 | 6738 | 3108 |
| 1004 | C | 2 | ring 5 | -111,168 | 94 | 6738 | 2970 |
| 1004 | C | 3 | ring 5 | -111,168 | 94 | 6738 | 2999 |
| 1004 | B | 4 | ring 5 | -111,168 | 94 | 6738 | 2929 |
| 1004 | A | 5 | ring 5 | -111,168 | 94 | 6738 | 3100 |
| 1005 | A | 0 | ring 9 | 7,296 | 326 | 6970 | 3287 |
| 1005 | B | 1 | ring 9 | 7,296 | 326 | 6970 | 3371 |
| 1005 | C | 2 | ring 9 | 7,296 | 326 | 6970 | 3160 |
| 1005 | C | 3 | ring 9 | 7,296 | 326 | 6970 | 3356 |
| 1005 | B | 4 | ring 9 | 7,296 | 326 | 6970 | 3349 |
| 1005 | A | 5 | ring 9 | 7,296 | 326 | 6970 | 3192 |
| 1421442253 | A | 0 | ring 20 | 651,618 | 1680 | 50580 | 22974 |
| 1421442253 | B | 1 | ring 15 | -473,426 | 870 | 14965 | 6642 |
| 1421442253 | C | 2 | ring 15 | -473,426 | 870 | 9539 | 4527 |
| 1421442253 | C | 3 | ring 15 | -473,426 | 870 | 9539 | 4409 |
| 1421442253 | B | 4 | ring 15 | -473,426 | 870 | 14965 | 6949 |
| 1421442253 | A | 5 | ring 20 | 651,618 | 1680 | 50580 | 22644 |
| 407538027 | A | 0 | ring 8 | -210,-241 | 243 | 6908 | 3012 |
| 407538027 | B | 1 | ring 8 | -210,-241 | 243 | 6908 | 2897 |
| 407538027 | C | 2 | ring 8 | -210,-241 | 243 | 6908 | 3006 |
| 407538027 | C | 3 | ring 8 | -210,-241 | 243 | 6908 | 2954 |
| 407538027 | B | 4 | ring 8 | -210,-241 | 243 | 6908 | 2965 |
| 407538027 | A | 5 | ring 8 | -210,-241 | 243 | 6908 | 2993 |
## Excavation, foundation fill and affected chunks (B-median-eligible grids)

| seed | chunk | diff | yf | cut | skirt | excavated solid blocks above Y0 (Σ max(0,h−1−yf)) | body cells above natural ground (Σ max(0,yf+1−h)) | columns whose ground is below Y0 | affected chunks |
|---|---|---|---|---|---|---|---|---|---|
| 1001 | -541,431 | 29 | 70 | 11 | 18 | 8848 | 12761 | 3673 | 36 |
| 1001 | -477,-217 | 15 | 69 | 9 | 6 | 6064 | 16736 | 3618 | 36 |
| 1001 | -473,-180 | 22 | 72 | 13 | 9 | 5209 | 9178 | 4420 | 36 |
| 1001 | -462,435 | 28 | 71 | 16 | 12 | 14722 | 17869 | 3371 | 36 |
| 1001 | -463,458 | 11 | 65 | 4 | 7 | 839 | 7907 | 4678 | 36 |
| 1001 | -435,-279 | 14 | 70 | 7 | 7 | 8765 | 14506 | 3495 | 36 |
| 1001 | -430,-237 | 22 | 70 | 11 | 11 | 4522 | 8097 | 3926 | 36 |
| 1001 | -398,-180 | 26 | 74 | 15 | 11 | 10735 | 14720 | 3489 | 36 |
| 1001 | -17,-395 | 40 | 81 | 21 | 19 | 12017 | 19602 | 3467 | 36 |
| 1001 | -18,-368 | 23 | 72 | 17 | 6 | 13812 | 12032 | 3771 | 36 |
| 1001 | 48,-434 | 9 | 69 | 3 | 6 | 1144 | 9953 | 4121 | 36 |
| 1001 | 80,-247 | 16 | 77 | 3 | 13 | 2050 | 12628 | 3553 | 36 |
| 1001 | 114,-309 | 24 | 76 | 11 | 13 | 9735 | 19654 | 3513 | 36 |
| 1001 | 371,-117 | 28 | 79 | 12 | 16 | 8043 | 19308 | 3440 | 36 |
| 1002 | -80,-459 | 30 | 72 | 24 | 6 | 24432 | 9127 | 3880 | 36 |
| 1002 | -89,-413 | 15 | 65 | 13 | 2 | 12666 | 8339 | 3376 | 36 |
| 1002 | 8,-87 | 24 | 80 | 9 | 15 | 7143 | 10313 | 3609 | 36 |
| 1002 | 44,-147 | 10 | 72 | 4 | 6 | 1168 | 11357 | 4835 | 36 |
| 1002 | 141,522 | 14 | 65 | 3 | 11 | 2266 | 8438 | 3612 | 36 |
| 1002 | 170,79 | 33 | 70 | 8 | 25 | 3352 | 16798 | 3864 | 36 |
| 1002 | 200,35 | 14 | 64 | 13 | 1 | 13915 | 5531 | 3285 | 36 |
| 1002 | 208,70 | 23 | 69 | 4 | 19 | 1825 | 10491 | 4694 | 36 |
| 1002 | 329,527 | 13 | 71 | 5 | 8 | 2082 | 17134 | 4145 | 36 |
| 1002 | 353,-628 | 25 | 70 | 10 | 15 | 3006 | 15335 | 4401 | 36 |
| 1002 | 366,-49 | 16 | 69 | 10 | 6 | 3726 | 7841 | 3764 | 36 |
| 1002 | 364,-26 | 22 | 75 | 10 | 12 | 3152 | 10739 | 4027 | 36 |
| 1002 | 396,593 | 18 | 64 | 8 | 10 | 7114 | 5959 | 3551 | 36 |
| 1002 | 521,-210 | 9 | 66 | 6 | 3 | 5077 | 8712 | 3444 | 36 |
| 1002 | 613,-172 | 13 | 67 | 7 | 6 | 3219 | 10317 | 3907 | 36 |
| 1003 | -45,596 | 19 | 72 | 17 | 2 | 13609 | 4626 | 3758 | 36 |
| 1003 | 612,100 | 29 | 67 | 8 | 21 | 8327 | 15135 | 3342 | 36 |
| 1004 | -589,-474 | 24 | 72 | 15 | 9 | 9939 | 19575 | 3582 | 36 |
| 1004 | -571,-501 | 10 | 68 | 6 | 4 | 2772 | 11011 | 3995 | 36 |
| 1004 | -146,522 | 10 | 66 | 7 | 3 | 5934 | 7715 | 4027 | 36 |
| 1004 | -111,168 | 12 | 70 | 4 | 8 | 2309 | 9210 | 3762 | 36 |
| 1004 | -50,493 | 20 | 72 | 11 | 9 | 7450 | 11770 | 3349 | 36 |
| 1004 | 335,-275 | 29 | 66 | 11 | 18 | 10587 | 9095 | 3694 | 36 |
| 1004 | 463,-276 | 17 | 67 | 8 | 9 | 7610 | 11467 | 3854 | 36 |
| 1005 | -17,449 | 28 | 74 | 18 | 10 | 21965 | 9470 | 3393 | 36 |
| 1005 | 7,296 | 13 | 69 | 6 | 7 | 4662 | 7438 | 3819 | 36 |
| 1005 | 39,493 | 26 | 73 | 16 | 10 | 13242 | 12624 | 3487 | 36 |
| 1005 | 172,-304 | 37 | 78 | 22 | 15 | 21300 | 17928 | 3494 | 36 |
| 1005 | 428,487 | 34 | 74 | 23 | 11 | 24492 | 17070 | 3354 | 36 |
| 1005 | 432,556 | 28 | 68 | 5 | 23 | 3514 | 13699 | 3781 | 36 |
| 1005 | 618,-500 | 21 | 66 | 9 | 12 | 9500 | 5992 | 3284 | 36 |
| 1034165336 | 118,205 | 27 | 69 | 21 | 6 | 19463 | 8800 | 3981 | 36 |
| 1034165336 | 179,-213 | 12 | 68 | 7 | 5 | 9492 | 11284 | 3603 | 36 |
| 1034165336 | 233,-218 | 25 | 66 | 3 | 22 | 1297 | 16306 | 4338 | 36 |
| 1034165336 | 263,-241 | 33 | 74 | 24 | 9 | 17284 | 15993 | 3404 | 36 |
| 119599970 | -467,106 | 4 | 63 | 4 | 0 | 505 | 4960 | 4960 | 36 |
| 119599970 | -396,131 | 25 | 64 | 6 | 19 | 5952 | 6624 | 3886 | 36 |
| 119599970 | -311,559 | 12 | 72 | 5 | 7 | 989 | 9869 | 5455 | 36 |
| 119599970 | 10,629 | 14 | 66 | 8 | 6 | 7290 | 10348 | 3683 | 36 |
| 119599970 | 206,-334 | 21 | 65 | 3 | 18 | 1611 | 9884 | 3847 | 36 |
| 119599970 | 335,-25 | 27 | 69 | 15 | 12 | 10002 | 13483 | 3667 | 36 |
| 119599970 | 355,-15 | 10 | 67 | 2 | 8 | 793 | 7440 | 4189 | 36 |
| 119599970 | 358,21 | 29 | 70 | 22 | 7 | 5732 | 6697 | 3765 | 36 |
| 119599970 | 357,41 | 36 | 78 | 20 | 16 | 21425 | 28961 | 3434 | 36 |
| 1421442253 | -630,387 | 28 | 79 | 12 | 16 | 5428 | 12770 | 3871 | 36 |
| 1421442253 | -473,426 | 27 | 71 | 9 | 18 | 6669 | 16946 | 3611 | 36 |
| 1421442253 | -110,-636 | 23 | 68 | 7 | 16 | 4497 | 7368 | 3701 | 36 |
| 1541117449 | -624,-141 | 25 | 75 | 19 | 6 | 12909 | 13595 | 3678 | 36 |
| 1541117449 | -499,625 | 35 | 76 | 22 | 13 | 28733 | 20365 | 3389 | 36 |
| 1541117449 | -440,592 | 19 | 66 | 3 | 16 | 1330 | 8798 | 4351 | 36 |
| 1541117449 | -408,486 | 7 | 63 | 5 | 2 | 3608 | 4845 | 4256 | 36 |
| 1541117449 | -370,425 | 34 | 71 | 17 | 17 | 12031 | 10588 | 3326 | 36 |
| 1541117449 | -52,-239 | 16 | 70 | 4 | 12 | 1911 | 9843 | 3344 | 36 |
| 1541117449 | -46,-176 | 16 | 72 | 13 | 3 | 8881 | 6691 | 3596 | 36 |
| 1541117449 | -24,-140 | 17 | 68 | 12 | 5 | 6332 | 12202 | 3305 | 36 |
| 1541117449 | -26,-112 | 34 | 70 | 9 | 25 | 9946 | 15788 | 4069 | 36 |
| 1541117449 | -17,-51 | 22 | 71 | 14 | 8 | 7606 | 13736 | 4352 | 36 |
| 1541117449 | 8,-186 | 25 | 75 | 6 | 19 | 9381 | 19257 | 3379 | 36 |
| 1541117449 | 109,-319 | 8 | 68 | 3 | 5 | 345 | 9760 | 5120 | 36 |
| 1541117449 | 214,330 | 23 | 69 | 17 | 6 | 3779 | 12482 | 4311 | 36 |
| 1541117449 | 196,398 | 18 | 68 | 2 | 16 | 67 | 12566 | 4987 | 36 |
| 1541117449 | 274,180 | 29 | 72 | 21 | 8 | 22927 | 10475 | 3376 | 36 |
| 1541117449 | 301,198 | 16 | 71 | 9 | 7 | 11332 | 14686 | 3848 | 36 |
| 1541117449 | 341,-625 | 21 | 68 | 12 | 9 | 5848 | 11716 | 3841 | 36 |
| 1541117449 | 328,176 | 7 | 65 | 4 | 3 | 3282 | 6605 | 3842 | 36 |
| 1541117449 | 331,370 | 11 | 63 | 11 | 0 | 12469 | 4125 | 4125 | 36 |
| 1541117449 | 360,-600 | 18 | 74 | 8 | 10 | 4475 | 12256 | 3703 | 36 |
| 1541117449 | 404,-633 | 19 | 75 | 9 | 10 | 7960 | 16503 | 3562 | 36 |
| 1541117449 | 427,-595 | 26 | 78 | 5 | 21 | 3199 | 18303 | 3717 | 36 |
| 1541117449 | 483,-593 | 16 | 70 | 12 | 4 | 4247 | 6921 | 3948 | 36 |
| 1541117449 | 513,-592 | 29 | 78 | 15 | 14 | 2865 | 25261 | 3739 | 36 |
| 1928394366 | -531,623 | 25 | 70 | 16 | 9 | 6055 | 13987 | 4329 | 36 |
| 1928394366 | -497,430 | 8 | 68 | 4 | 4 | 2128 | 8410 | 3960 | 36 |
| 1928394366 | -408,398 | 28 | 71 | 20 | 8 | 19114 | 17499 | 3410 | 36 |
| 1928394366 | -84,-337 | 8 | 70 | 2 | 6 | 389 | 15523 | 4813 | 36 |
| 1928394366 | -49,-303 | 17 | 66 | 12 | 5 | 15177 | 12119 | 3351 | 36 |
| 1928394366 | 491,43 | 38 | 71 | 11 | 27 | 14746 | 19567 | 3545 | 36 |
| 2048069562 | 102,483 | 10 | 73 | 3 | 7 | 1427 | 8741 | 3613 | 36 |
| 218938829 | -598,203 | 24 | 63 | 3 | 21 | 987 | 14835 | 4869 | 36 |
| 218938829 | -406,-565 | 9 | 69 | 4 | 5 | 1332 | 8199 | 3733 | 36 |
| 218938829 | -396,-531 | 20 | 72 | 2 | 18 | 662 | 11223 | 3926 | 36 |
| 218938829 | 42,-597 | 15 | 70 | 8 | 7 | 10419 | 12723 | 3424 | 36 |
| 218938829 | 46,-558 | 16 | 70 | 4 | 12 | 2763 | 9764 | 3372 | 36 |
| 218938829 | 41,-438 | 32 | 81 | 9 | 23 | 2621 | 12719 | 3467 | 36 |
| 218938829 | 73,-535 | 27 | 82 | 15 | 12 | 15763 | 27886 | 3384 | 36 |
| 218938829 | 108,-634 | 29 | 66 | 7 | 22 | 4915 | 15700 | 3465 | 36 |
| 218938829 | 592,-604 | 18 | 72 | 4 | 14 | 922 | 11447 | 4944 | 36 |
| 218938829 | 618,-525 | 32 | 69 | 3 | 29 | 1743 | 12205 | 4247 | 36 |
| 287862831 | -499,-375 | 36 | 70 | 4 | 32 | 2854 | 9365 | 3486 | 36 |
| 287862831 | -503,-308 | 7 | 67 | 4 | 3 | 1365 | 9009 | 4440 | 36 |
| 287862831 | -207,454 | 22 | 67 | 8 | 14 | 9411 | 12761 | 3651 | 36 |
| 287862831 | -93,-633 | 17 | 72 | 8 | 9 | 8569 | 14838 | 3594 | 36 |
| 287862831 | 72,207 | 12 | 66 | 7 | 5 | 4889 | 9611 | 3885 | 36 |
| 287862831 | 142,270 | 24 | 74 | 13 | 11 | 12479 | 18214 | 3472 | 36 |
| 287862831 | 178,208 | 20 | 70 | 2 | 18 | 45 | 12849 | 5652 | 36 |
| 287862831 | 170,233 | 11 | 67 | 4 | 7 | 1699 | 9074 | 4056 | 36 |
| 287862831 | 303,-236 | 16 | 72 | 7 | 9 | 2795 | 15595 | 4691 | 36 |
| 287862831 | 336,-215 | 10 | 68 | 5 | 5 | 978 | 11866 | 4448 | 36 |
| 318277688 | -463,-369 | 25 | 64 | 6 | 19 | 3000 | 6398 | 3856 | 36 |
| 318277688 | -430,70 | 20 | 71 | 14 | 6 | 9957 | 11993 | 4029 | 36 |
| 318277688 | -414,75 | 26 | 83 | 6 | 20 | 4116 | 27471 | 3714 | 36 |
| 318277688 | -215,-531 | 18 | 73 | 5 | 13 | 6802 | 15866 | 3416 | 36 |
| 318277688 | -179,-509 | 26 | 73 | 16 | 10 | 12806 | 9845 | 3582 | 36 |
| 318277688 | 81,265 | 23 | 67 | 8 | 15 | 5745 | 14260 | 3821 | 36 |
| 318277688 | 141,304 | 17 | 73 | 9 | 8 | 3972 | 11860 | 4077 | 36 |
| 318277688 | 229,273 | 20 | 78 | 3 | 17 | 1504 | 13101 | 3750 | 36 |
| 318277688 | 532,396 | 10 | 68 | 5 | 5 | 3532 | 13224 | 4046 | 36 |
| 318277688 | 560,389 | 14 | 72 | 8 | 6 | 9880 | 5980 | 3592 | 36 |
| 318277688 | 620,-275 | 19 | 65 | 15 | 4 | 7623 | 5485 | 3584 | 36 |
| 407538027 | -376,-362 | 7 | 66 | 3 | 4 | 906 | 9614 | 4527 | 36 |
| 407538027 | -345,-349 | 26 | 72 | 13 | 13 | 13769 | 12220 | 3778 | 36 |
| 407538027 | -276,-256 | 12 | 68 | 6 | 6 | 1884 | 6927 | 3544 | 36 |
| 407538027 | -241,557 | 23 | 71 | 19 | 4 | 1878 | 8205 | 4933 | 36 |
| 407538027 | -210,-241 | 10 | 68 | 4 | 6 | 1556 | 10573 | 3560 | 36 |
| 407538027 | -186,588 | 9 | 71 | 1 | 8 | 0 | 16392 | 5316 | 36 |
| 407538027 | -155,553 | 29 | 69 | 24 | 5 | 14171 | 12434 | 3602 | 36 |
| 417616547 | -591,561 | 13 | 67 | 6 | 7 | 5308 | 6186 | 3514 | 36 |
| 417616547 | -497,104 | 19 | 74 | 11 | 8 | 10304 | 17180 | 3430 | 36 |
| 417616547 | -410,70 | 19 | 71 | 11 | 8 | 5412 | 11864 | 3723 | 36 |
| 417616547 | -217,524 | 22 | 70 | 15 | 7 | 6430 | 17274 | 3812 | 36 |
| 417616547 | -185,-340 | 37 | 64 | 6 | 31 | 4537 | 10418 | 4340 | 36 |
| 417616547 | -112,-185 | 13 | 68 | 3 | 10 | 1730 | 6995 | 3494 | 36 |
| 417616547 | 549,-45 | 6 | 66 | 3 | 3 | 1369 | 6489 | 3367 | 36 |
| 417616547 | 586,-53 | 10 | 75 | 5 | 5 | 3082 | 10598 | 3432 | 36 |
| 417616547 | 586,108 | 18 | 73 | 8 | 10 | 7001 | 18232 | 3341 | 36 |
| 516955406 | -459,548 | 30 | 70 | 13 | 17 | 3120 | 12571 | 5003 | 36 |
| 516955406 | 146,268 | 25 | 68 | 20 | 5 | 17560 | 12797 | 3451 | 36 |
| 516955406 | 197,260 | 16 | 71 | 8 | 8 | 8293 | 15087 | 3419 | 36 |
| 516955406 | 261,294 | 26 | 64 | 6 | 20 | 4069 | 8301 | 4754 | 36 |
| 527213223 | -588,-144 | 19 | 70 | 2 | 17 | 382 | 12307 | 4541 | 36 |
| 527213223 | -573,-149 | 14 | 65 | 5 | 9 | 1574 | 6232 | 3571 | 36 |
| 527213223 | -182,-307 | 37 | 86 | 23 | 14 | 12972 | 20942 | 3341 | 36 |
| 527213223 | -120,-301 | 12 | 68 | 8 | 4 | 6128 | 9038 | 3510 | 36 |
| 527213223 | 309,616 | 15 | 67 | 6 | 9 | 7830 | 11194 | 3323 | 36 |
| 527213223 | 325,618 | 35 | 67 | 4 | 31 | 3906 | 13650 | 3341 | 36 |
| 527213223 | 429,45 | 24 | 75 | 7 | 17 | 4768 | 8412 | 3586 | 36 |
| 527213223 | 496,-19 | 33 | 68 | 6 | 27 | 2863 | 21603 | 4169 | 36 |
| 527213223 | 528,71 | 20 | 75 | 8 | 12 | 10545 | 18224 | 3634 | 36 |
| 527213223 | 622,-460 | 14 | 65 | 10 | 4 | 6761 | 7188 | 3910 | 36 |
| 794814944 | -80,333 | 16 | 71 | 6 | 10 | 1866 | 14937 | 4483 | 36 |
| 794814944 | 303,-241 | 12 | 67 | 8 | 4 | 3588 | 12519 | 4234 | 36 |
| 794814944 | 298,-154 | 17 | 80 | 9 | 8 | 6754 | 7958 | 3961 | 36 |
| 794814944 | 300,468 | 34 | 82 | 15 | 19 | 10857 | 44439 | 3456 | 36 |
| 914490140 | -468,489 | 4 | 64 | 3 | 1 | 240 | 7041 | 5747 | 36 |
| 914490140 | -311,520 | 33 | 74 | 5 | 28 | 3833 | 10505 | 3337 | 36 |
| 914490140 | -244,527 | 39 | 70 | 13 | 26 | 10568 | 21950 | 3446 | 36 |
| 914490140 | 17,102 | 22 | 65 | 5 | 17 | 2421 | 10316 | 4368 | 36 |
| 914490140 | 22,134 | 33 | 77 | 12 | 21 | 10425 | 12376 | 3374 | 36 |
| 914490140 | 72,71 | 34 | 69 | 8 | 26 | 4173 | 16937 | 4271 | 36 |
| 914490140 | 294,167 | 20 | 68 | 7 | 13 | 2032 | 11738 | 3890 | 36 |
| 914490140 | 398,340 | 30 | 74 | 24 | 6 | 25551 | 11583 | 3289 | 36 |
| 914490140 | 431,230 | 15 | 65 | 7 | 8 | 7561 | 7593 | 3511 | 36 |
| 914490140 | 460,240 | 14 | 68 | 4 | 10 | 2167 | 8325 | 3635 | 36 |
| 914490140 | 525,303 | 32 | 73 | 10 | 22 | 7154 | 12804 | 4249 | 36 |
| 914490140 | 553,-530 | 30 | 64 | 6 | 24 | 3477 | 14042 | 3927 | 36 |
| 914490140 | 555,-464 | 35 | 84 | 19 | 16 | 21788 | 16710 | 3652 | 36 |
| 914490140 | 551,272 | 23 | 68 | 4 | 19 | 3497 | 19141 | 3631 | 36 |
| 914490140 | 592,-466 | 34 | 70 | 5 | 29 | 2092 | 10537 | 4131 | 36 |

Totals over 172 B-median-eligible candidates: excavated median 5258 max 28733; fill median 11862 max 44439; affected chunks min 36 max 36

