# ST-01 P8-D2 — Structure-search benchmark (measured; one machine; not a performance guarantee)

Supersedes the single-seed observations of P8-D1 (seed 1004, radius 20, 6–9 s). Raw data: `probe/` (outputs under `evidence/`), full per-call table:
`probe/bench-table-raw.md`. Probe: `St01D2Probe` mode `bench` (source in `probe/`), driven by `probe/run_all.sh`.

## 1. Environment and method
- One cloud container: **4 vCPU, JDK 25.0.5, max heap 3,420 MB**, Minecraft 26.3 dedicated server via the repo's Gradle run configuration, P8-C2-C branch, `view-distance=2`, no players. **Not** the CI runners and not a production server. Each (seed, radius) is a **fresh world** (cold). Single run per configuration (no repeats of the cold call); JVM start-up is included in the first call of each process (JIT/class loading are part of "cold").
- Seeds (6 of the 20 audit seeds, chosen to include seeds with 0, 1, 3, 4 pyramids in the audit area): 1001, 1003, 1004, 1005, 407538027, 1421442253. Radii: 5, 10, 20, 40 (all seeds); 100 (seeds 1004 and 1003 only). Origins: (0,0) cold; (6000,−6000) "later origin, partially warm" (same world, after the first origin); then 3 warm repeats at each origin.
- Call measured: `ServerLevel.findNearestMapStructure(HolderSet.direct(st01), origin, radius, skipKnownStructures=false)` — exactly what the vanilla trade modifier executes. Recorded per call: wall time (the call runs **synchronously on the server thread, so wall time = server-thread blocked time**), thread CPU time, heap pool peak, GC time, the **count of ST-01 generation-point evaluations** (the production counter `St01PyramidStructure.VERDICT_COUNTS`), loaded-chunk delta, result, validity of the returned chunk (`getStartForStructure(...).isValid()`), ring distance and distance in blocks.
- Not measured: tick-rate effects on connected players, concurrent calls, CI-runner or low-end hardware timings, radius-100 **exhaustive** failure, memory beyond the heap pools, CPU other than the calling thread.

## 2. The radius unit (determined, not assumed)
Radius is counted in **structure-placement regions** (spacing = 32 chunks = 512 blocks), as a square: radius *r* examines (2r+1)² candidate chunks. Evidence: a search that finds nothing evaluates exactly **121 / 441 / 1,681** candidates at radius **5 / 10 / 20** (= 11² / 21² / 41²). So r=5 ≈ 2.6 km, 10 ≈ 5.1 km, 20 ≈ 10.2 km, 40 ≈ 20.5 km, 100 ≈ 51.2 km (each a half-width). The vanilla explorer-map value 100 therefore means up to 40,401 candidates.

## 3. Results
**Cold first call, origin (0,0), fresh world, 6 seeds:**

| radius | median | p90 | max | min | pyramid found | median candidate evaluations |
|---|---|---|---|---|---|---|
| 5 | 114 ms | 6.3 s | 6.3 s | 82 ms | 1 / 6 | 121 |
| 10 | 6.3 s | 7.1 s | 7.1 s | 3.2 s | 3 / 6 | 384 |
| 20 | 10.7 s | 27.7 s | 27.7 s | 6.0 s | 5 / 6 | 532 |
| 40 | 14.8 s | 27.6 s | 27.6 s | 6.0 s | 6 / 6 | 532 |
| 100 (2 seeds: 1004, 1003) | 6.3 s / 23.3 s | – | 23.3 s | 6.3 s | 2 / 2 | 95 / 1,759 |

(With n = 6 the "p90" is the second-largest value; treat all percentiles as indicative.)

- **Distance to the nearest pyramid** (vanilla result) in these 6 seeds: 3.2, 4.7, 5.1, 8.3, 11.1 and 14.4 km (ring 5, 9, 8, 14, 21, 20). One seed (1003) needs ring 21, so **radius 20 failed** there after evaluating all 1,681 candidates (15.1 s) and radius ≥ 21 is needed to find a pyramid in all six seeds.
- **Cost is not proportional to area; it is proportional to the number of candidates the search must evaluate before the first success**, each costing 0.6–76 ms (median 16 ms) on average: non-Desert candidates are cheap (≈ 0.7 ms) and Desert candidates run the 81×81 Desert sweep and 6,561 height samples. Single seeds jump from ≈ 0.1 s to ≈ 6 s as soon as one Desert candidate is evaluated. The radius-100 run for seed 1003 cost the same as radius 40 (23 s) because the search stops at the first success (ring 21).
- **A failed search costs the whole area.** The only measured failure beyond radius 5: radius 20, seed 1003: 15.1 s for 1,681 candidates. **Radius-100 exhaustion was not measured**; extrapolating at that failed-search rate (≈ 9 ms per candidate × 40,401 ≈ 6 min) is an *extrapolation, not a result*, and is the reason radius 100 was not run to exhaustion.
- **Warm repeats** (same origin, immediately after): n = 146, median **0.2 ms**, max 1.5 ms; zero new evaluations. A different origin in the same world ("partially warm") still cost 0.08–18.6 s because new candidates were evaluated.
- **Resources:** peak heap during a call ≤ 667 MB (pool peaks, includes the baseline server), GC ≤ 112 ms per call, calling-thread CPU ≈ 84 % of wall time (the search is CPU-bound on the server thread), **+1 loaded chunk when a pyramid is found and 0 otherwise** (candidate chunks are evaluated to the structure-start stage without staying loaded).
- **Correctness of the result:** all **116 found results (100 %) were valid ST-01 starts** (`isValid()`); where ground truth was enumerated (seed 1004, ±10,240 blocks) the returned chunk was one of the three real starts. Vanilla's "nearest" is ring-order approximate: in seed 1001 the vanilla result was at 8.3 km while a different real start was at 7.0 km (found by the incremental probe, see `NONBLOCKING_SEARCH_OPTIONS.md`).

## 4. What this means for the two channels
1. Any search that actually evaluates Desert candidates blocks the server thread for **seconds** (6–28 s at radius ≥ 10 in this environment). 20 ticks per second means 120–560 consecutive missed ticks for a single call.
2. **Channel B1 as a vanilla trade runs this call when the offer is generated** (first inventory open or level-up): a player-triggered multi-second server stall, repeated for each such villager. A small radius is cheap (≈ 0.1 s at radius 5) but usually finds nothing (1 / 6 seeds).
3. **Channel A4** has the same cost wherever the map is created (acceptance/completion/exchange).
4. Warm lookups are essentially free (≈ 0.2 ms), so caching of **found** results is highly effective; caching of **misses** per area is what protects the server from repeated failed searches.
5. These numbers are from one 4-vCPU container and must not be reused as guarantees; the benchmark is reproducible with `probe/run_all.sh` and should be repeated on the target hardware before fixing a radius.
