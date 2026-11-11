# ST-01 P8-D2-R1 — Controlled re-benchmark

## Environment and controls
Same container class as P8-D2 (4 vCPU cloud container, JDK 25.0.5, Minecraft 26.3, `view-distance=2`, no players). **Worktree A** = `04a44ffa` + the **unchanged** P8-D2 probe (`St01D2Probe`, `bench` method byte-identical to the one that produced the P8-D2 evidence) — exact reproduction. **Worktree B** = `04a44ffa` + disposable instrumentation + `St01R1Probe`. Every run: fresh world (`world/` deleted), same seed, origin `(0,0)` cold then `(6000,−6000)`, 3 warm repeats (A); B: cold origin only. Runs sequential (no contention). Cache sharing: within one run the second origin and the repeats share the in-process caches (StructureCheck, chunk holders, noise caches); between runs nothing is shared (new JVM, new world).

## 1. Reproduction of P8-D2 (worktree A) — PASS
- **192 / 192 calls** (6 seeds × 4 radii × 8 calls) returned the **same result chunk** and the **same `VERDICT_COUNTS` delta** as P8-D2.
- Non-warm wall times: median deviation **+1.8 %**, median |deviation| **3.3 %**, max |deviation| 28.3 % (one partially warm second-origin call).

| Radius | Cold median (P8-D2) | Cold median (R1) | Cold max (P8-D2 → R1) | Found (both) |
|---|---|---|---|---|
| 5 | 114 ms | 96 ms | 6.3 → 6.2 s | 1 / 6 |
| 10 | 6.3 s | 6.4 s | 7.1 → 7.0 s | 3 / 6 |
| 20 | 10.7 s | 10.7 s | 27.7 → 27.9 s | 5 / 6 |
| 40 | 14.8 s | 15.7 s | 27.6 → 29.0 s | 6 / 6 |
Warm repeats: median 0.23 ms, max 2.0 ms (P8-D2: 0.2 / 1.5 ms).

## 2. Instrumented baseline (worktree B)
Same 24 cold searches; identical result chunks to A in all 24; breakdown in `INSTRUMENTATION_RESULTS.md`.

## 3. Cheap-first locator experiment (worktree B, read-only)
`St01R1Probe` mode `cheaplocate`: **vanilla's exact ring and scan order** (r = 0..R; x-major, z-minor over ring edges; `getPotentialStructureChunk(seed, chunkX + 32k, chunkZ + 32l)`), eligibility evaluated on the server thread through a probe terrain view with the same generator calls as production, and every eligible candidate **validated as a real start** with `getChunk(STRUCTURE_STARTS).getStartForStructure(st01).isValid()` (which runs production `findGenerationPoint` again on a worker). First valid start in a ring is returned, like vanilla. Two eligibility variants:
- `prod`: the unmodified `St01Eligibility.evaluate` — a control for the loop itself.
- `exp`: the exact-equivalent cheap-first variant (`CHEAP_FIRST_HYPOTHESIS.md`).

| Seed | r | Vanilla (A) | Loop + production eligibility | Loop + cheap-first | Speed-up vs vanilla | Result chunk (vanilla = cheap-first) | Cheap-first: candidates / eligible / eligibility ms / validation ms |
|---|---|---|---|---|---|---|---|
| 1001 | 10 | 6.80 s | – | 3.72 s | 1.83× | none = none | 441 / 0 / 3,722 / 0 |
| 1003 | 10 | 7.02 s | – | 0.96 s | 7.28× | none = none | 441 / 0 / 961 / 0 |
| 1004 | 10 | 6.07 s | – | 6.59 s | 0.92× | (−111,168) = | 94 / 1 / 3,221 / 3,364 |
| 1005 | 10 | 6.42 s | – | 6.51 s | 0.98× | (7,296) = | 326 / 1 / 3,203 / 3,308 |
| 407538027 | 10 | 6.29 s | – | 6.14 s | 1.03× | (−210,−241) = | 243 / 1 / 3,073 / 3,060 |
| 1421442253 | 10 | 3.47 s | – | 0.60 s | 5.81× | none = none | 441 / 0 / 594 / 0 |
| 1001 | 20 | 27.91 s | 27.98 s | 11.95 s | 2.34× | (−435,−279) = | 735 / 1 / 8,734 / 3,210 |
| 1003 | 20 | 14.50 s | 14.48 s | 2.22 s | 6.54× | none = none | 1,681 / 0 / 2,208 / 0 |
| 1004 | 20 | 6.85 s | 6.42 s | 6.37 s | 1.08× | (−111,168) = | 94 / 1 / 3,139 / 3,229 |
| 1005 | 20 | 6.71 s | 6.57 s | 6.47 s | 1.04× | (7,296) = | 326 / 1 / 3,279 / 3,186 |
| 407538027 | 20 | 5.90 s | 6.02 s | 5.99 s | 0.98× | (−210,−241) = | 243 / 1 / 3,039 / 2,954 |
| 1421442253 | 20 | 26.76 s | 26.99 s | 9.12 s | 2.93× | (651,618) = | 1,680 / 1 / 5,881 / 3,233 |
| 1001 | 40 | 28.96 s | – | 11.59 s | 2.50× | (−435,−279) = | 735 / 1 / 8,416 / 3,172 |
| 1003 | 40 | 24.92 s | – | 8.86 s | 2.81× | (−121,686) = | 1,758 / 1 / 5,716 / 3,140 |
| 1004 | 40 | 6.22 s | – | 6.28 s | 0.99× | (−111,168) = | 94 / 1 / 3,087 / 3,187 |
| 1005 | 40 | 6.53 s | – | 6.48 s | 1.01× | (7,296) = | 326 / 1 / 3,253 / 3,225 |
| 407538027 | 40 | 6.46 s | – | 5.88 s | 1.10× | (−210,−241) = | 243 / 1 / 2,997 / 2,878 |
| 1421442253 | 40 | 26.55 s | – | 9.21 s | 2.88× | (651,618) = | 1,680 / 1 / 6,087 / 3,115 |

- **Target identity:** identical to vanilla in **18 / 18** configurations (13 found, 5 not found). No false positive is possible by construction (every returned chunk passed the production `isValid()` start check); no false negative was observed against vanilla within the same bound. Exhaustive target-set equivalence is shown at the eligibility level in `CHEAP_FIRST_HYPOTHESIS.md` (9,600 anchors, 0 mismatches).
- **Control:** the same loop with the unmodified production eligibility took the same time as vanilla (6 / 6 within ≈ 2–6 %) and returned the same chunk, so the difference is due to the eligibility evaluation, not to the loop.
- **Main-thread blocked time** = wall time in every variant (all are synchronous on the server thread).
- **Where it does not help:** when the first full-Desert candidate in scan order is also eligible (seeds 1004, 1005, 407538027), the cost is the eligible candidate's 6,561 heights (≈ 3 s) **plus** its duplicate validation (≈ 3 s), so ≈ 6 s remains in every variant.

Limits: one container, one cold run per configuration, 6 seeds; the speed-ups depend on how many terrain-failing full-Desert candidates precede the first success, which is a property of the seed and origin.
