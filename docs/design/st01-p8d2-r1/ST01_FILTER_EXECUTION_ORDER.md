# ST-01 P8-D2-R1 — Actual execution order of ST-01's filters inside the locator

Repo `04a44ffa`: `pyramid/worldgen/St01PyramidStructure.java`, `St01Candidate.java`, `St01Eligibility.java`.

| # | Step | Where | Cost driver |
|---|---|---|---|
| 0 | Placement candidate (region → chunk) | vanilla `RandomSpreadStructurePlacement.getPotentialStructureChunk` | arithmetic |
| 1 | `Rotation.getRandom(context.random())` | `St01PyramidStructure.findGenerationPoint:57` | trivial |
| 2 | **Centre-cell Desert test** (early exit): `cellColumnDesert(centre)` = `surfaceHeight(cell centre)` (one `getBaseHeight`) then `isDesertCell(qy)` `&&` `isDesertCell(qy+1)` | `St01Eligibility.fullFootprintDesert:88-89`, `cellColumnDesert:99-103` | **1 terrain height** + ≤ 2 biome lookups |
| 3 | **Full-footprint Desert sweep**: every quart cell `qx0..qx1 × qz0..qz1` (22 × 22 = 484 cells, row-major from the north-west corner, centre included again), each = 1 height (memoized per evaluation) + ≤ 2 biome lookups, first failure exits | `fullFootprintDesert:90-94` | up to 484 heights |
| 4 | **Terrain height sampling**: all 6,561 columns (81×81), memoized (cell-centre columns from step 3 are reused) | `evaluate:57-64` | up to 6,561 heights |
| 5 | Sort; height difference ≤ 16, cut ≤ 10, skirt ≤ 8 (in that order) | `evaluate:65-72` | trivial |
| 6 | **Stub-cell Desert** at `(anchor, yf)` | `evaluate:76-78` | 1 biome lookup |
| 7 | Route selection (only if eligible) | `St01Candidate.decide:30-34` | trivial |
| 8 | vanilla `isValidBiome(stub)` | `Structure.findValidGenerationPoint` L295 | 1 biome lookup |
| 9 | `getChunk(STRUCTURE_STARTS)` → **steps 1-8 repeated on a worker** + `addPieces` (template lookup) | vanilla pipeline + `St01PyramidStructure.addPieces:68-75` | duplicate of 2-6 + pieces |
| 10 | `isValid()` → locate position | vanilla | trivial |

**Entrance obstruction and underground fluid are not evaluated (Policy D)**, and vanilla overlap is not checked — consistent with the approved policy. The locator and natural worldgen run the **same** `findGenerationPoint`, so the locator cannot disagree with real worldgen (P8-C2-C: 46/46 predictions matched real starts). The terrain view is `GeneratorTerrain` (`St01PyramidStructure.java:86-121`): heights = `chunkGenerator.getBaseHeight(x, z, WORLD_SURFACE_WG, …)` (noise-only, no chunk is loaded), biomes = `context.biomeResolver().getNoiseBiome(...)`.

## Answer to the critical question
**Yes — a cheap-first Desert rejection is already present.** Non-Desert candidates are rejected at step 2, before the 81×81 sweep, before any terrain sampling and before any chunk operation. But the "cheap" test is **not biome-only**: it needs one `getBaseHeight` to know at which Y to sample the biome (3D biomes), and that height dominates its cost (`INSTRUMENTATION_RESULTS.md`).

What is **not** cheap-first: (a) the sweep in step 3 scans row-major from a corner, so a footprint that fails on the far side samples many cells first; (b) step 4 computes all 6,561 heights before checking the range, although the height-difference rule could reject on a partial set; (c) step 9 recomputes everything for every successful candidate.

## Stage vocabulary
Potential anchor (step 0) → centre-Desert candidate (step 2) → full-Desert candidate (step 3) → terrain-qualified (step 5) → stub-Desert / **eligible** (steps 6-8) → **valid structure start** (step 9-10) → fully generated pyramid (only when its chunks reach FULL; not part of the locator).
