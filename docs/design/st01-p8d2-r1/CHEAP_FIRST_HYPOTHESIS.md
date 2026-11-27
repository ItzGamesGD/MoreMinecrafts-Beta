# ST-01 P8-D2-R1 — The cheap-first filtering hypothesis

Owner hypothesis: the locator may not exploit ST-01's selective 81×81 Desert requirement efficiently. Proposed sequence: placement candidates → cheap Desert prefilter → full Desert footprint → expensive terrain only for survivors → validate real starts.

## 1. Is the sequence already present? — PARTIAL (stage order yes; within-stage order no)
| Proposed step | Present in production? | Evidence |
|---|---|---|
| 1. placement-region candidates | yes (vanilla, arithmetic) | `VANILLA_LOCATOR_CALL_GRAPH.md` |
| 2. cheap Desert prefilter | **yes** — centre-cell test before anything else | `St01Eligibility.fullFootprintDesert:88-89`; 98.0 % of candidates stop here at ≈ 0.7 ms |
| 3. full 81×81 Desert before terrain | **yes** | `evaluate:55` before `:57-64` |
| 4. terrain only for survivors | **yes, but all 6,561 heights are computed before any terrain rule is applied** | `evaluate:62-72` |
| 5. validate real starts | yes (vanilla), **but the survivor is evaluated a second time** | `getChunk(STRUCTURE_STARTS)` → `Structure.generate` |
So the owner's prefilter **is already present**; the measured cost comes from what happens *after* it.

## 2. Required checks
| Check | Answer | Evidence |
|---|---|---|
| Does biome sampling trigger expensive worldgen? | **No.** `getNoiseBiome` ≈ 0.004 ms; 0.17 % of search time. | instrumentation |
| …but does the Desert test need expensive work? | **Yes, one terrain height per quart cell** (to choose the biome's Y, biomes are 3D): ≈ 0.45 ms, i.e. > 99 % of a cell test. | `cellColumnDesert:100`; instrumentation |
| Is a single-point Desert check a safe rejection prefilter? | **Yes, the existing one is.** The centre cell `(⌊cx/4⌋, ⌊cz/4⌋)` lies inside the swept range (`qx0 = ⌊(cx−42)/4⌋ ≤ ⌊cx/4⌋ ≤ ⌊(cx+38)/4⌋+1 = qx1`, same for z) and is tested with the **same** `cellColumnDesert` predicate, so failing it implies failing the AND over all cells. It can never reject a candidate the full rule accepts. | algebra; 9,600-anchor test |
| Could a height-free single-point test be cheaper? | Not safely: without the surface height the Y of the biome sample is unknown; a safe version would have to test every quart Y the surface could occupy (≈ 96 levels at 0.004 ms ≈ the cost of one height), so there is no gain. | analysis |
| Can the footprint check avoid loading chunks? | **It already does**: `getBaseHeight` and `getNoiseBiome` are noise evaluations; no chunk is loaded or generated during eligibility (`loaded_chunks_delta` = 0 except the one found chunk). | P8-D2 + R1 counters |
| Is terrain sampling the dominant cost? | **Yes**: 97.8 % of wall time; 65.1 % is the 6,561-column stage. | instrumentation |
| Can candidate ordering change safely? | **Between candidates:** changing it changes *which* valid start is returned (vanilla's first-in-scan-order). **Within one candidate:** the checks are an AND of independent predicates, so any order gives the same verdict. | algebra |
| Can cached biome data be reused? | Biome lookups are already cheap; nothing to gain. Height memoization is already done per evaluation; it is **not** reused across the two evaluations of a successful candidate. | code |
| Duplicate evaluations? | **Yes**: every successful candidate twice (≈ 3 s each); the centre cell is also re-tested inside the sweep (its height is memoized; only 2 biome lookups ≈ 0.01 ms are repeated). | instrumentation; code |

## 3. The experiment: an exact-equivalent cheap-first eligibility (probe only)
`St01R1Probe.expEvaluate` (source in `probe/St01R1Probe.java.txt`) evaluates **the same predicates** as `St01Eligibility.evaluate`, in a different order:
1. centre cell first (as production);
2. **spread-first sweep**: corners, edge midpoints, a stride-3 lattice of cells, then every remaining cell (the union is exactly production's cell set);
3. heights: an **11 × 11 lattice** (every 8 blocks) first, then all 6,561 columns, with a running min/max: as soon as `max − min > 16` the candidate is rejected (production would reject it with the same verdict, because the full set's range is ≥ any subset's range and production checks the height difference first);
4. otherwise the production arithmetic unchanged (sort, median `yf`, cut, skirt, stub cell).

Equivalence argument: steps 1-2 compute an AND over the identical cell set with identical per-cell predicates; step 3 only stops early when the production result is already determined as `REJECT_HEIGHT_DIFFERENCE`; step 4 is the production code path on the full set. For early-rejected candidates the **verdict** is identical but the diagnostic numbers in `Result` (min/max/median) are partial.

### Exhaustive comparison over every anchor (`evalcmp`, 6 seeds × 1,600 = 9,600 anchors, ±10,240 blocks)
| Seed | Production verdicts | Verdict mismatches | Full-result mismatches (non-early) | Production time | Cheap-first time | Heights computed (prod → exp) |
|---|---|---|---|---|---|---|
| 1001 | 4 eligible, 12 height-diff, 1 skirt, 1,583 biome | **0** | **0** | 52.9 s | 19.2 s | 118,642 → 41,640 |
| 1003 | 4 height-diff, 1,596 biome | 0 | 0 | 14.2 s | 2.2 s | 30,476 → 3,932 |
| 1004 | 3 eligible, 5 height-diff, 1,592 biome | 0 | 0 | 25.1 s | 13.9 s | 56,037 → 30,186 |
| 1005 | 1 eligible, 11 height-diff, 1,588 biome | 0 | 0 | 37.0 s | 8.9 s | 83,033 → 18,088 |
| 407538027 | 4 eligible, 5 height-diff, 1,591 biome | 0 | 0 | 28.6 s | 14.2 s | 63,377 → 30,869 |
| 1421442253 | 6 height-diff, 1,594 biome | 0 | 0 | 20.1 s | 2.9 s | 43,856 → 5,247 |
| **Total** | 12 eligible, 43 height-diff, 1 skirt | **0** | **0** | **177.8 s** | **61.3 s (2.9×)** | **395,421 → 129,962** |
All 43 height-difference rejections were caught early. Order within each candidate alternated (baseline first on even anchors, experimental first on odd) to cancel warm-up bias; both used fresh memo caches per anchor. The eligible counts per seed equal the P8-C2-C/audit counts (1001: 4, 1004: 3, 1005: 1, 407538027: 4, 1003: 0, 1421442253: 0).

## 4. What the experiment shows and does not show
- **Shows:** an exact-equivalent reordering removes ≈ 2/3 of eligibility work in aggregate, with zero verdict differences on 9,600 real anchors; end-to-end the locator becomes 1.8–7.3× faster when terrain-failing Desert candidates precede the first success or nothing is found (`CONTROLLED_REBENCHMARK.md` §3).
- **Does not show:** any gain when the first full-Desert candidate is eligible (≈ 6 s remains: the eligible candidate needs all 6,561 heights for its median, and it is evaluated twice). It does **not** make the search non-blocking: the remaining seconds are still on the server thread.
- The pass rate (0.69 % full Desert) did not translate into proportional savings, as the owner's caveat anticipated: the cost is concentrated in the few candidates that survive the cheap test.
