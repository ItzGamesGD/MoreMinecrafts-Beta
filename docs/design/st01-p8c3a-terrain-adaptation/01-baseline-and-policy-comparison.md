# 01 — Baseline reproduction and policy comparison

Status: **research evidence only. No threshold, algorithm or production code is changed or approved by this document.**

## Method

* Code under test: candidate checkpoint `checkpoint/unified-20261111-candidate-2` (`11946339`); its code tree is identical to `0743aa74`, the commit the probe worktree was built from.
* The probe ([`probe/St01C3AProbe.java.txt`](probe/St01C3AProbe.java.txt)) calls the **production** `St01Eligibility.fullFootprintDesert` (centre cell, then the 484-cell Desert sweep) and the production `WORLD_SURFACE_WG` base height of the real 26.3 generator. On every full-Desert anchor it samples all 6,561 footprint columns, then applies the **same verdict formulas** as `St01Eligibility.evaluate` (diff → cut → skirt → stub-biome) to two parameter sets:
  * `D_16_10_8`: the current production Policy D / MODERATE limits;
  * `B_40_24_32`: the owner's design candidate.
* Anchors: every `random_spread` potential anchor (spacing 32, separation 8, salt 91401) whose centre lies within ±10,240 blocks, which is 1,600 per seed. The run uses the same 20 seeds as the P8-C2-B, P8-C2-C and P8-D2-R1 audits, the same origin and radius, and a fresh world per seed.
* Area: 20 × 20,480² m² = **8,388.6 km²**.

## Baseline reproduction: PASS

| Quantity | Previously reported | Reproduced |
|---|---|---|
| Placement candidates (20 seeds) | 32,000 | **32,000** |
| Desert-qualified (full footprint) | 221 | **221** |
| Terrain-qualified (Policy D, median plane) | 46 | **46** |
| Density | ≈ 0.548 / 100 km² | **0.548 / 100 km²** |

Per-seed values are in [05](05-coverage-simulation.md). The real-server CI run on the integration head (run 42999635096, 20 seeds, Policy D) also passed.

## Comparison of the four requested variants

All four variants use the production median plane and the same 32,000 anchors.

| Variant | Definition | Eligible starts | Density / 100 km² | vs A | Eligibility-stage height samples | Notes |
|---|---|---|---|---|---|---|
| **A** | Policy D 16 / 10 / 8, production order | **46** | **0.548** | — | 6,561 per full-Desert anchor (production) | baseline |
| **B** | 40 / 24 / 32, production eligibility order | **172** | **2.050** | ×3.74 | identical to A per evaluated anchor | 126 anchors are eligible only under B |
| **C** | B with the P8-D2-R1 exact-equivalent cheap-first order (9 × 9 lattice range check, then full sweep with early exit) | **172** (same set as B by construction; spot-checked by search runs) | 2.050 | ×3.74 | fewer on *rejected* anchors only | cost differs; verdict does not |
| **D** | B plus the measured terrain-correction cost | **172** | 2.050 | ×3.74 | as B | adds correction and assembly execution per accepted start; see [03](03-terrain-correction-architecture.md) and [06](06-search-and-worldgen-performance.md) |

Rejections among the 221 full-Desert anchors (median plane):

| Verdict | Policy D | Policy B |
|---|---|---|
| ELIGIBLE | 46 | 172 |
| REJECT_HEIGHT_DIFFERENCE | 160 | 39 |
| REJECT_CUT | 5 | 10 |
| REJECT_SKIRT | 10 | 0 |
| REJECT_BIOME_STUB | 0 | 0 |

## Is 40 / 24 / 32 internally feasible?

**Yes, with one plane-algorithm caveat. The thresholds were not altered.**

1. **Geometric consistency.** A plane `yf` is admissible when `max − yf ≤ 24` and `yf − min ≤ 32`. Such a plane exists exactly when `diff ≤ 24 + 32 = 56`. The binding limit is therefore `diff ≤ 40`, and every anchor with `diff ≤ 40` has a non-empty admissible window `[max − 24, min + 32]`.
2. **Median-plane losses.** With the production **median** plane, 10 of those anchors are still rejected: the footprint is mostly low ground with a high minority, so the median sits more than 24 below the top. A plane clamped into the admissible window would accept all 10, giving 182 eligible (2.170 / 100 km²). This changes `yf` for those anchors, so it is a plane-algorithm choice for the owner ([02](02-construction-plane-analysis.md)), not a threshold change.
3. **Skirt 32 equals the underground body depth.** The assembly owns every footprint cell from Y−32 to Y0. With skirt ≤ 32, the lowest natural ground inside the footprint is never more than 32 below Y0. The body floor (Y−32) is therefore never above the lowest footprint column, and no column of the body floats inside the footprint. In the coverage data the median plane reaches **exactly 32** (seed 287862831, chunk −499,−375); [08](08-visual-and-geometric-evidence.md) shows it is an interior pit that the body fills.
4. **Cut 24 vs correction box.** The correction box spans Y+1..Y+39 and clears up to natural height + 8 (vegetation clearance). With cut ≤ 24 the highest clearance is Y+32 < Y+39, so the clearance is **never truncated** by the box under B.
5. **Real pipeline.** The unchanged production pipeline was run in a disposable worktree with only the three constants set to 40/24/32. It generated 21 real starts on seeds 1004 and 1001 (7 + 14, exactly the probe's prediction). All production placement, determinism and persistence checks passed except the probe's own assertion of the 16/10/8 limits ([07](07-determinism-and-chunk-safety.md)).

## What is preserved

Geometry, rooms, entrances, loot, Guardian, rotations, access-route variants, Desert footprint, spacing / separation / salt and overlap permission are untouched. Only the three limits differ between A and B. The real-pipeline experiment confirms that owned-cell content is bit-identical in its verifier: 0 owned-cell mismatches on all 21 starts.
