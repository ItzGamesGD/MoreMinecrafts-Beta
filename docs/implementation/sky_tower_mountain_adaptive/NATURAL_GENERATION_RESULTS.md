# Natural Generation Results

Labels: RUNTIME_VERIFIED unless stated. All runs are real Fabric 26.3 dedicated servers (JDK 25.0.5) using the **production** structures (`AdaptiveJigsawStructure`) through the real chunk pipeline; "replay" = the engine's own `Structure.generate` called for the same placement-selected chunks, with the baseline legacy `minecraft:jigsaw` definitions (`tools/sky-tower-adaptive/legacy/`) evaluated on identical chunks for pairing.

## Samples
| Run set | Seeds | World | Eligible candidates evaluated | Natural pipeline vs replay |
|---|---|---|---|---|
| `nat_*` | 2, 3, 6, 7, 9 | natural Overworld, radius 2,500 chunks (≈6,403 km² each, 32,013 km² total) | 49,099 (mountain 11,900; general 37,199) | **49,099 / 49,099 agree** (start/element/box) |
| `flat_{snowy,rocky,meadow}_T{200,201,202}`, `flat_snowy_T240` | 31 | real flat worlds, pinned biome (jagged_peaks / stony_peaks / meadow), exact terrain top T | 865 each (120 tower draws each) | 8 × 865 agree, 0 disagree |
| `controlled` (in-process fixed-T generator) | 4242 | 3 variants × 13 terrains, radius 700 | 4,510 non-tower + 797 tower draws per cell | n/a (CONTROLLED, labelled) |
| `top_*`, `integ_*` | 31, 9 | flat T=201 ×3 variants; natural seed 9 | see STRUCTURE_INTEGRITY.md | — |

Per-candidate fields (seed, chunk, element, rotation, centre, T, draw-1 roll, `h_allowed`, `h_effective`, outcome, reason, generated structure/box/offset/rotation) are in `evidence/` summaries and `NATURAL_TOWER_SAMPLES_T160plus.csv` (92 natural tower candidates at T ≥ 160: coordinates = chunk x/z, columns `terrain_top_T_at_center`, `draw1_roll`, `h_effective_max`, `gen_min_y/max_y`, `gen_offset`, `rotation_of_piece`). Examples:

| seed | chunk | variant | element | T | draw-1 | h_eff | outcome | root piece Y | offset | rotation |
|---|---|---|---|---|---|---|---|---|---|---|
| 2 | (-979, 1918) | snowy | tower | 195 | 57 | 24 | **generated** (resampled) | 218..314 | 23 | CLOCKWISE_90 |
| 6 | (2469, -2373) | snowy | tower | 200 | 51 | 19 | generated (resampled) | 218..314 | 18 | CLOCKWISE_90 |
| 7 | (-154, 1178) | rocky | tower | 196 | 18 | 23 | generated (roll kept, fits) | 214..310 | 18 | COUNTERCLOCKWISE_90 |
| 7 | (-881, 857) | snowy | tower | 200 | 29 | 19 | generated (resampled) | 218..314 | 18 | CLOCKWISE_180 |
| 3 | (-1595, 507) | snowy | tower | 204 | 43 | 15 | **rejected** (h_eff 15 < 18) | — | — | — |
| 3 | (-2080, 2016) | snowy | tower | 249 | 26 | −30 | rejected | — | — | — |

## Natural results (production, 5 seeds, mountain set)
| Terrain T | tower selected | accepted (adaptive) | accepted (legacy) |
|---|---|---|---|
| < 100 (lowland control) | 497 | 497 (100 %) | 497 |
| 100–139 | 908 | 907 | 907 |
| 140–159 | 228 | 228 (100 %) | 222 (97.4 %) |
| 160–179 | 52 | 52 (100 %) | 34 (65.4 %) |
| 180–199 | 29 | 29 (100 %) | 8 (27.6 %) |
| 200–201 | 2 | 2 | 0 |
| 202–219 | 5 | 0 (policy) | 0 |
| ≥ 220 | 4 | 0 (policy) | 0 |
| **all** | 1,725 | **1,715 = 99.42 %** [98.94, 99.68] | 1,668 = 96.70 % [95.74, 97.44] |

The one missing tower at T 100–139 is a sky-altitude biome rejection (identical in legacy). Every adaptive rejection at T ≥ 202 is the approved policy (`h_eff < 18`). Tower variants generated: snowy 741, meadow 665, rocky 309; rotations NONE 422, CW90 453, CW180 419, CCW90 421 (**all four rotations**). Generated roots reach exactly `maxY = 315` (e.g. flat T=201: all 120 towers per variant, `minY=219, maxY=315`).

## Flat natural boundary worlds (real pipeline, 120 tower draws each)
| World | T | towers generated | offsets | max root maxY |
|---|---|---|---|---|
| flat snowy | 200 | 120 / 120 | 18–19 | 315 |
| flat snowy / rocky / meadow | **201** | **120 / 120** each | 18 only | 315 |
| flat snowy / rocky / meadow | **202** | **0 / 120** each | — | — |
| flat snowy | 240 | 0 / 120 | — | — |

## Other
* **Lowland controls**: T < 100 natural bin 497/497 identical to legacy; general set rows: 37,199 evaluated, adaptive == legacy for every row (`general_set_identical: true`).
* High-elevation peaks: natural T ≥ 180 tower candidates = 29 + 2 + 5 + 4 = 40; all 31 that are feasible (T ≤ 201) generated.
* "Sample sizes at T ≥ 200 are small (11 natural towers); the exact boundary behaviour is established by the controlled sweep and the flat natural worlds, not by these counts."
