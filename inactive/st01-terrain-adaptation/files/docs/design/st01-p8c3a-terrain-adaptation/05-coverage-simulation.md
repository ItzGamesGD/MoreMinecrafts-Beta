# 05 — Coverage simulation

Status: **measured** with the production Desert gate and the production generator heights, on 20 seeds × 1,600 anchors (±10,240 blocks, 8,388.6 km²). The raw per-anchor data, including the 81×81 height grid of every Policy-B-relevant anchor, is in [results/coverage](results/coverage). The full tables are in [results/analysis.md](results/analysis.md).

## Per seed (median plane unless noted)

| seed | potential anchors | full-Desert | Policy D | Policy B | B, window-clamped plane |
|---|---|---|---|---|---|
| 1001 | 1600 | 17 | 4 | 14 | 14 |
| 1002 | 1600 | 18 | 5 | 15 | 16 |
| 1003 | 1600 | 4 | 0 | 2 | 2 |
| 1004 | 1600 | 8 | 3 | 7 | 8 |
| 1005 | 1600 | 12 | 1 | 7 | 8 |
| 1034165336 | 1600 | 5 | 1 | 4 | 4 |
| 119599970 | 1600 | 11 | 4 | 9 | 9 |
| 1421442253 | 1600 | 6 | 0 | 3 | 4 |
| 1541117449 | 1600 | 31 | 4 | 24 | 25 |
| 1928394366 | 1600 | 7 | 2 | 6 | 6 |
| 2048069562 | 1600 | 1 | 1 | 1 | 1 |
| 218938829 | 1600 | 12 | 2 | 10 | 10 |
| 287862831 | 1600 | 11 | 4 | 10 | 10 |
| 318277688 | 1600 | 12 | 2 | 11 | 11 |
| 407538027 | 1600 | 9 | 4 | 7 | 8 |
| 417616547 | 1600 | 10 | 3 | 9 | 9 |
| 516955406 | 1600 | 8 | 1 | 4 | 5 |
| 527213223 | 1600 | 12 | 2 | 10 | 11 |
| 794814944 | 1600 | 8 | 1 | 4 | 4 |
| 914490140 | 1600 | 19 | 2 | 15 | 17 |
| **total** | **32000** | **221** | **46** | **172** | **182** |

## Density

| Policy / plane | Eligible | per 100 km² | per seed (mean) | seeds with 0 |
|---|---|---|---|---|
| D, median (production) | 46 | 0.548 | 2.3 | 2 (1003, 1421442253) |
| B, median | 172 | 2.050 | 8.6 | 0 |
| B, window-clamped median | 182 | 2.170 | 9.1 | 0 |
| upper bound if every full-Desert anchor passed terrain | 221 | 2.635 | 11.1 | 0 |

## Where the candidates are lost

| Stage | Remaining | Pass rate |
|---|---|---|
| potential anchors | 32,000 | — |
| full 81×81 Desert footprint | 221 | 0.69 % |
| terrain, Policy D | 46 | 20.8 % of Desert |
| terrain, Policy B | 172 | 77.8 % of Desert |

Under Policy B, **the Desert gate, not terrain, becomes the dominant limit**. 172 of the 221 possible anchors are accepted, so any further terrain relaxation could add at most 49 (+28 %).

## Height-difference distribution of the 221 full-Desert anchors

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

Policy D accepts only the first two buckets (diff ≤ 16) and Policy B the first five (diff ≤ 40). The 39 anchors above 40 are mostly Desert margins against badlands or mountains.

## Real-world cross-check

In the disposable real-pipeline experiment with production code and limits 40/24/32 ([07](07-determinism-and-chunk-safety.md)), real servers generated **7 starts on seed 1004 and 14 on seed 1001**. That is exactly the probe's Policy B count for those seeds, with `prediction_vs_actual_mismatches = 0`. Of these, 3 + 4 were also Policy D-eligible, matching the Policy D column above.

## Not measured

* Seeds beyond the 20 audit seeds, or radii beyond ±10,240.
* Player-perceived rarity: whether 2.05 per 100 km² is the desired rarity is a design question for the owner. The Policy D rarity was itself an owner-calibrated outcome.
