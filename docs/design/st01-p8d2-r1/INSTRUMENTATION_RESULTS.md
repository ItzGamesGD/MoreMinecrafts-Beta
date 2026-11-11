# ST-01 P8-D2-R1 — Instrumentation results (per-stage cost attribution)

## Method
- **Disposable worktree B** (`/tmp/r1/wtB`, detached at `04a44ffa`, never pushed): timing counters added to the production classes (`probe/instrumentation.patch`); all guarded by `St01Instr.ON` (`-Dmm.st01.instr=true`). Counters are per thread class (`main` = server thread, `worker` = worldgen workers).
- Instrumented points: `findGenerationPoint` (inclusive, by verdict); `St01Eligibility.evaluate` stage 1 (Desert footprint: pass/reject, centre-cell outcome, cells swept) and stage 2 (6,561-column height loop); every `getBaseHeight` that is actually computed (memo misses) and every `getNoiseBiome`; `addPieces` (incl. template lookup).
- Same call, seeds, radii and cold fresh-world condition as P8-D2: `findNearestMapStructure(st01, (0,64,0), r, false)`, 6 seeds × radii 5/10/20/40 = 24 searches.
- **Inclusive vs exclusive:** `fgp.total ⊃ stage1 ⊃ height+biome`; `fgp.total ⊃ stage2 ⊃ height`. Exclusive "vanilla rest" = wall − main-thread `fgp.total` − worker `fgp.total` − `addPieces`. No inclusive timing is added to another inclusive timing below. Overhead of the instrumentation: instrumented wall times are within the A/B noise of the uninstrumented reproduction (e.g. 1001 r20: 27.9 s vs 28.8 s).
- Not separable without patching Minecraft: chunk-system scheduling inside `getChunk(STRUCTURE_STARTS)` (it is inside "vanilla rest" + worker time), `StructureCheck` cache lookups individually (they are inside "vanilla rest").

## Totals over the 24 searches (228.7 s of server-thread wall time)
| Component | ms | share of wall |
|---|---|---|
| **Terrain heights `getBaseHeight` (main + worker)** | 223,552 | **97.8 %** |
| of which the 6,561-column stage on the main thread | 148,793 | 65.1 % |
| of which the Desert stage (centre test + sweep) on the main thread | 31,445 | 13.8 % |
| **Duplicate evaluation of the successful candidate (worker, chunk pipeline)** | 45,083 | **19.7 %** |
| Biome lookups `getNoiseBiome` (all) | 397 | 0.17 % |
| `addPieces` incl. template lookup | 1,872 | 0.8 % |
| Vanilla locator rest (enumeration, StructureCheck, chunk scheduling, `getChunk`) | 1,337 | 0.58 % |

(The first three rows overlap: the duplicate evaluation is itself mostly height sampling.)

**Unit costs (this container):** one computed terrain height **0.452 ms** (395,263 on the main thread); one biome lookup ≈ **0.004 ms** (≈ 115,000 lookups, 397 ms); a candidate rejected at the centre cell **0.61–0.86 ms** (one height + ≤ 2 biome lookups + overhead); Desert sweep of a full-Desert candidate ≈ **267 ms**; 6,561-column stage ≈ **2,705 ms** per full-Desert candidate; `addPieces` 89–168 ms.

## Per-stage survival (main-thread evaluations, 24 searches)
| Stage | Candidates | Share |
|---|---|---|
| Evaluated by the locator (`checkStructurePresence`) | 12,280 | 100 % |
| Rejected at the centre cell | 12,038 | 98.0 % |
| Centre cell Desert | 242 | 2.0 % |
| Full 81×81 Desert footprint | 55 | 0.45 % |
| Eligible (terrain + stub) | 15 | 0.12 % |
Exhaustive over all anchors (9,600 = 6 seeds × 1,600, `evalcmp`): centre Desert 227 (2.4 %), terrain-stage verdicts 56 (0.58 %; 43 height difference, 1 skirt, 12 eligible). Consistent with the 20-seed audit (221 / 32,000 = 0.69 % full Desert).

## Per-search breakdown (selected; full table `evidence/B-bench-summary.json`)
| Seed, radius | Wall | Main-thread eval | Duplicate (worker) | Height computes (main) | Candidates → centre pass → full Desert → eligible | Notes |
|---|---|---|---|---|---|---|
| 1004, 5 | 6.3 s | 3.09 s | 3.08 s | 6,738 | 94 → 1 → 1 → 1 | nearest pyramid is the first Desert candidate: half the time is the duplicate |
| 1001, 10 | 6.8 s | 6.76 s | – | 14,610 | 441 → 8 → 2 → 0 | two full-Desert candidates fail terrain after 6,561 heights each (5.5 s) |
| 1003, 20 | 14.5 s | 14.40 s | – | 30,986 | 1,681 → 35 → 4 → 0 | not found; 4 terrain failures = 10.8 s |
| 1001, 20 | 28.8 s | 25.51 s | 3.02 s | 56,092 | 735 → 30 → 8 → 1 | 7 terrain failures (6 height difference, 1 skirt) = 21.8 s before the success |
| 1421442253, 20 | 26.5 s | 23.14 s | 3.12 s | 50,580 | 1,680 → 31 → 7 → 1 | 6 height-difference failures before the success |

## Findings
1. The search time is **almost entirely terrain height sampling** (97.8 %). Biome sampling is negligible; vanilla's own locator machinery is negligible.
2. Within that, the largest single block is the **6,561-column stage for full-Desert candidates that then fail the terrain rule** (43 of 55 full-Desert candidates over the exhaustive set fail on height difference).
3. **Every successful candidate is evaluated twice** (main + worker), costing ≈ 3 s again; for a search whose first full-Desert candidate succeeds this is ≈ 48 % of the wall time.
4. The "cheap" centre-cell Desert test costs ≈ 0.7 ms, of which ≈ 0.45 ms is the one height it needs to pick the biome's Y.
