# 09 — Implementation feasibility decision

**Research verdict: POLICY_B_FEASIBILITY = CONDITIONAL_GO.**

Policy B (40 / 24 / 32, 81×81, full Desert) is internally feasible with the existing placement, assembly and correction architecture. It was demonstrated on real servers with production code. Shipping it still requires the owner decisions listed below, because several of its consequences are gameplay-visible and are not settled by current authority.

**This document approves nothing. It changes no threshold, and no production code was changed.**

## Evidence summary

| Question | Answer | Where |
|---|---|---|
| Baseline reproduced? | **PASS**: 32,000 / 221 / 46 / 0.548 per 100 km², exact | [01](01-baseline-and-policy-comparison.md) |
| Are 40 / 24 / 32 internally consistent? | yes: every diff ≤ 40 has an admissible plane; skirt 32 equals the body depth (no floating body); cut 24 never truncates the clearance | [01](01-baseline-and-policy-comparison.md) |
| Coverage | 46 → **172** eligible (×3.74; 2.050 per 100 km²); every seed gets at least one; Desert becomes the limit (221 max) | [05](05-coverage-simulation.md) |
| Plane algorithm | median, mean and trimmed mean are equivalent; the window-clamped median reaches the 182 ceiling at almost no volume cost; midrange is not advisable | [02](02-construction-plane-analysis.md) |
| Correction volume | excavation p90 8,293 → 13,915, max 11,332 → 28,733; per-start correction time unchanged (≈ 3 s over 36 chunks on worker threads); per-area work ×3.74 | [03](03-terrain-correction-architecture.md), [06](06-search-and-worldgen-performance.md) |
| Search performance | first-eligible latency improved or unchanged on 6/6 seeds (up to −87 %); per-anchor cost identical; cheap-first order C saves 28–34 % more where rough Desert dominates | [06](06-search-and-worldgen-performance.md) |
| Determinism and chunk safety | real pipeline under B: 21/21 starts identical across NORMAL / REVERSE / RANDOM / restart, 0 owned-cell mismatches, 0 prediction mismatches | [07](07-determinism-and-chunk-safety.md) |
| Overlap | rate per start unchanged (≈ 25 %); count per area ×4.3; vanilla pieces inside the footprint are replaced (0 survive, measured on 6 real sites); 36 sliced structures at the edge; new types (desert temples, outposts, ruined portals) | [04](04-structure-overlap-safety-matrix.md) |
| Exposed underground body | solid walls only (no room opens); exposed-face p90 8 → 16 | [03](03-terrain-correction-architecture.md), [08](08-visual-and-geometric-evidence.md) |
| Fluids | 0 leak faces on 8 real sites, 0 fluid in the completion box on 21 starts; shell water against the body ≤ 1,402 cells | [03](03-terrain-correction-architecture.md) |

## Conditions (owner decisions required before any implementation)

| # | Decision | Why it is not an implementation detail |
|---|---|---|
| G1 | **Adopt 40 / 24 / 32** as current authority, replacing the MODERATE 16 / 10 / 8 limits | changes approved terrain limits and rarity (×3.74 density) |
| G2 | **Construction plane**: keep `median` (172) or adopt `window_clamped_median` (182) | changes `yf`, the vertical position, for about 6 % of starts |
| G3 | **Entrance drop**: accept up to about 32 blocks below the route threshold (10 observed on a real B start), or require an approach ramp or steps | a ramp is new geometry; acceptance is a playability call |
| G4 | **Edge treatment**: accept cliffs up to 23 and exposed sandstone walls up to the full body height, or approve blending outside the footprint | blending breaks "never writes outside the footprint" |
| G5 | **Overlap consequences at ×4.3**: accept slicing at the edge, loot and archaeology loss inside the footprint, and the new structure types under the existing permission | the permission forbids rejection and suppression; scale and types are new facts |
| G6 | **Test contract**: when G1 is adopted, update the Policy D probe assertion `moderate_limits_respected` to the adopted limits | the assertion encodes 16 / 10 / 8; it is the only failing check under B |
| G7 | **Visual acceptance**: client captures of B starts | LIVE VALIDATION PENDING |

## What implementation would involve once G1–G6 are decided

All of this is HOW, and none of it was done:

1. Change the three constants in `St01PlacementConfig`, plus the plane function if G2 says so.
2. Update the eligibility boundary unit tests (`St01EligibilityTest`) and the probe assertion (G6).
3. Optionally adopt the P8-D2-R1 exact-equivalent cheap-first order (C) for search latency. It is independent of B.
4. Run the full 20-seed Policy D CI matrix (`st01-p8c2c-policy-d.yml`) and the client capture against the new limits.

## Status lines

* P8_C3_A_STATUS: **COMPLETE.** All nine deliverables are present. Items explicitly NOT RUN: real-pipeline validation on the other 18 seeds, client captures, entity outcomes and a player-load benchmark.
* POLICY_B_FEASIBILITY: **CONDITIONAL_GO** (G1–G7).
* BASELINE_REPRODUCED: **PASS**.
* GENERATION_COVERAGE: **MEASURED**.
* STRUCTURE_OVERLAP_SAFETY: **PARTIAL**: bounding boxes on all 172 starts, actual blocks on 7 real sites; entities and edge slicing unresolved.
* SEARCH_PERFORMANCE: **IMPROVED**: first-eligible latency, measured on 6 seeds; per-anchor cost unchanged; total worldgen correction work per area ×3.74.
* PRODUCTION_TERRAIN_CODE_CHANGED: **NO**.
