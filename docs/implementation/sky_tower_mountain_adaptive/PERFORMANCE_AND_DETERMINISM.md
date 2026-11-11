# Performance and Determinism

Labels: RUNTIME_VERIFIED / PARTIALLY_VERIFIED. Timings were taken while 3 server JVMs shared a 4-core machine, so they are indicative (±noise), not benchmarks.

## Extra work per candidate (SOURCE_VERIFIED, structure lines 92–110)
* Biome-ineligible candidates (≈ 88 % of placement candidates): vanilla already computes element box and the biome-column check inside `addPieces`; adaptive does them first and returns, plus one extra `WorldgenRandom`/`LegacyRandomSource` allocation. No heightmap query. (Not separately timed → PARTIALLY_VERIFIED.)
* Biome-eligible candidates: **+1 `getFirstOccupiedHeight`** (the centre column, then `addPieces` queries it again), a second column check, and one look-ahead RNG stream.
* No footprint scans, no loops, no retries.

## Measured (49,099 eligible candidates, 5 natural seeds; engine `Structure.generate` loop incl. the weighted retry, same candidates for both)
| Metric | Baseline | Adaptive | Δ |
|---|---|---|---|
| Time | 31.7 s (0.65 ms/candidate) | 40.6 s (0.83 ms/candidate) | +28 % on eligible candidates (≈ +0.18 ms) |
| Allocation (thread allocated bytes) | 3.09 GB (62.9 KB/candidate) | 3.45 GB (70.3 KB/candidate) | +11.8 % |
| Base-height (heightmap) queries, controlled sweep (206,973 evaluations) | 206,973 (1.00 / candidate) | 398,022 (1.92 / candidate) | +0.92 per eligible candidate |

Biome-eligible candidates are ≈ 12 % of placement candidates (116,950 of 945,977 in the Phase 2 scan); the extra cost applies only to them. Absolute added time ≈ 0.18 ms per eligible start, i.e. negligible per chunk. No unapproved large cost (stop condition not triggered).

## Random-number consumption
Real structure random: identical to vanilla (1 draw before `addPieces`, then rotation + element). Look-ahead uses a separate stream: 3 mirrored draws + at most 1 replacement draw. Consequence: for every candidate that vanilla accepted the result is bit-identical (11,851 identical, 0 different among fitting candidates).

## Determinism (RUNTIME_VERIFIED)
Seed 9, radius 1,200, 4,665 paired rows, three independent server runs (`det_a`, `det_b`, `det_c` — `det_c` on the final code after a refactor): rows identical in all fields except the run label and timing. Natural pipeline vs replay: 49,099/49,099 + 8×865 identical.

## Existing vs new chunks
Already generated chunks/structure starts are persisted and unaffected (verified with a baseline-generated saved world, `STRUCTURE_INTEGRITY.md`). Newly generated chunks differ from baseline only for candidates the policy intentionally changes: tower/island starts whose draw exceeded the feasible offset (49 of 11,900 mountain candidates in the sample, all at T ≥ 150, where `219 − T < 70`), i.e. the approved placement change; mixed-age seams at terrain generated before/after the update are possible (INFERRED).
