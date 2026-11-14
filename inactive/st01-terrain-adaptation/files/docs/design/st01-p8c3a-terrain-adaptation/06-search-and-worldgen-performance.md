# 06 — Search and worldgen performance

Status: **measured** in a local 4-vCPU container on JDK 25.0.5. Absolute times are container-specific; the relative comparisons were made within one process each.

## Method (comparable conditions)

* **Search** (`mode=search`): from origin (0, 0), a ring-ordered walk over `random_spread` anchors up to ring 20, the same order vanilla `findNearestMapStructure` uses. It stops at the first eligible anchor and records wall time, anchors visited and generator height computations.
* Variants run in the order **A, B, C, C, B, A** in one process, so JIT warm-up and OS cache effects are balanced. Each variant uses a fresh per-variant height memo: a cold height cache, exactly as production evaluates a new anchor.
* Six seeds were used: four from P8-D2-R1 (1001, 1003, 1004, 1005) and two of the long-search seeds (407538027, 1421442253).
* This walk is eligibility only. Vanilla adds a structure-reference lookup per anchor, which is identical for all variants.

## Search latency to the first eligible anchor

| seed | A first eligible (ring) | A ms | A height computes | B first eligible (ring) | B ms | B heights | C ms | C heights | C vs B | B vs A |
|---|---|---|---|---|---|---|---|---|---|---|
| 1001 | −435,−279 (14) | 24,916 | 56,092 | 80,−247 (8) | 3,209 | 7,130 | 3,227 | 7,130 | +1 % | **−87 %** |
| 1003 | none within ring 20 | 14,606 | 30,986 | −45,596 (18) | 10,755 | 22,756 | 7,776 | 16,410 | **−28 %** | −26 % (A found nothing) |
| 1004 | −111,168 (5) | 3,061 | 6,738 | −111,168 (5) | 3,018 | 6,738 | 2,985 | 6,738 | −1 % | −1 % |
| 1005 | 7,296 (9) | 3,240 | 6,970 | 7,296 (9) | 3,360 | 6,970 | 3,258 | 6,970 | −3 % | +4 % |
| 1421442253 | 651,618 (20) | 22,809 | 50,580 | −473,426 (15) | 6,795 | 14,965 | 4,468 | 9,539 | **−34 %** | **−70 %** |
| 407538027 | −210,−241 (8) | 3,003 | 6,908 | −210,−241 (8) | 2,931 | 6,908 | 2,980 | 6,908 | +2 % | −2 % |

Time is the mean of the two runs per variant. The run-to-run spread was ≤ 4 %. Raw data: [results/search](results/search).

### Interpretation (no inference from eligibility alone)

* **Per anchor evaluated, A and B cost the same.** Both run the Desert gate, then all 6,561 heights. When A and B stop at the same anchor (1004, 1005, 407538027) their height computes are identical and their times agree within noise.
* **B is faster only because it stops earlier.** It reaches an eligible anchor at a smaller ring on 1001 (8 vs 14), 1421442253 (15 vs 20) and 1003 (18 vs none). That is a real latency reduction for `/locate`-style searches. It follows from the higher acceptance rate, but it is measured, not inferred.
* **C reduces cost only on rejected terrain anchors.** The 9 × 9 lattice range check, then the early-exit sweep, stops as soon as `max − min > 40`. On seeds whose search crosses many rough Desert anchors (1003, 1421442253) C cuts height computes by 28–36 % and time by 28–34 %. On the others it changes nothing (±3 %). C never changed which anchor was found.
* **Main-thread blocking.** Vanilla map/locate searches run on the server thread, so the times above are blocking time. Under B the worst observed blocking in this sample is 10.8 s (1003), against A's 24.9 s (1001), and C brings it to 7.8 s. The P8-D2-R1 caching and orchestration recommendation (R3) is orthogonal and still applies.

## Worldgen cost

| Item | Policy D | Policy B | Basis |
|---|---|---|---|
| Eligibility cost per anchor (STRUCTURE_STARTS, worker threads) | Desert gate + 6,561 heights for full-Desert anchors | identical | same code path |
| Accepted starts per 100 km² | 0.548 | 2.050 | [05](05-coverage-simulation.md) |
| Chunks touched per start (81×81 footprint) | 36 | 36 | every start spans exactly 6 × 6 chunks (min = max = 36 over all 172) |
| Correction execution per start (`St01FootprintCorrectionPiece.correct` over its 36 chunks, real chunks) | 3.09 – 3.14 s on the D-eligible targets | 2.81 – 3.12 s on the B-only targets | [results/correction](results/correction); dominated by one generator `getBaseHeight` call per column (6,561), not by the number of blocks cleared |
| Blocks cleared per start | — | 1,628 – 17,198 on B-only targets | real chunks |
| FULL generation of the 36 + ring chunks around a start (probe, includes vanilla features) | 9.0 – 10.0 s | 6.2 – 7.7 s | dominated by vanilla generation, not by ST-01 |

**Consequence.** Per start, the correction cost is the same under both policies: about 85 ms per chunk on worldgen threads, because the per-column height lookup dominates. **Per area, total ST-01 correction and assembly work rises by the same factor as density, ×3.74.** Correction has no main-thread component.

The real-pipeline experiment generated 64 chunks per start in FULL and took 8 m 42 s for seed 1004 (4 phases) and 16 m 0 s for seed 1001 (4 phases), in line with the Policy D CI times for those seeds (8–12 min on GitHub runners). This is not a controlled A/B timing.

## Not measured

* Server tick impact under player exploration. A synthetic chunk-loading benchmark with players is **NOT RUN**: it requires a production change or a dedicated load harness.
* The R3 cache (P8-D2-R1). It was not implemented and is not part of this comparison.
