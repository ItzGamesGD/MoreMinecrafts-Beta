# Distribution Comparison — adaptive vs baseline (paired)

Data: the same 11,900 eligible mountain-set candidates (5 natural seeds, 32,013 km²) evaluated under PRODUCTION (adaptive) and LEGACY (baseline JSON). Intervals 95 % Wilson. RUNTIME_VERIFIED (engine `Structure.generate`; 49,099/49,099 agree with the natural pipeline).

| Metric | Baseline | Adaptive |
|---|---|---|
| Tower selection share (draw within eligible candidates; nominal 3/21 = 14.29 %) | 14.50 % (1,725/11,900) [13.9, 15.1] | identical (same draws) |
| Tower accepted given selected | 96.70 % [95.74, 97.44] | **99.42 % [98.94, 99.68]** |
| Non-tower accepted given selected | 10,171 / 10,175 | 10,173 / 10,175 |
| Islands generated | 11,839 | 11,888 |
| Tower share of generated islands | 14.09 % | 14.43 % |
| Effective frequency (accepted towers per 10,000 km², mountain set) | 521.0 | 535.7 (+2.8 %) |
| Mean / median offset, all generated islands | 43.99 / 44 | 43.92 / 44 |
| Mean / median offset, towers | 43.41 / 43 | 42.98 / 43 |
| Remaining tower rejections | 57 (padding) | 10 (9 infeasible `h_eff<18` at T ≥ 202, 1 sky-biome) |

*Selection share is quantity "draw probability in the pool", not an absolute spawn probability; the absolute frequency is the per-area row (it also contains the `random_spread` hit rate and biome eligibility).*

## Paired identity
Of 11,900 paired mountain candidates: **11,851 identical** (structure, root box, rotation); **0 differ where the draw-1 roll already fitted**; 49 differ, all where the roll exceeded the feasible maximum (47 towers resampled into existence + 2 non-tower islands). **General set: 37,199 rows identical.**

## Offsets / pile-ups
* Offset histogram (decile bins, generated mountain islands): adaptive 10:474, 20:2276, 30:2182, 40:2261, 50:2250, 60:2224, 70:221 vs baseline 464, 2253, 2171, 2258, 2249, 2223, 221 — the shape is unchanged; the extra 49 islands fall mostly (44 of 49) in the 10–39 bins.
* **Clamping check**: 47 natural towers needed a replacement offset; 1 is at the upper boundary `h_eff` (uniform expectation 3.8; a clamp would put 47/47 there). No boundary pile-up.
* Controlled sweep: at T=160 offsets 18..59 (mean 38.1), T=180 18..39 (28.3), T=200 18..19, T=201 exactly 18 — the uniform-on-feasible-range distribution (sample means differ from the exact means only because every cell reuses the same 797 chunks).

## Controlled elevation sweep (`HEIGHT_BOUNDARY_RESULTS.csv`, identical for snowy/rocky/meadow variants)
Adaptive tower acceptance: 100 % at T = 80, 120, 140, 160, 180, 200, 201 (797/797); 0 % at T = 202, 218, 219, 220, 240, 260. Baseline: 100 %, 100 %, 100 %, 81.8 %, 44.9 %, 2.9 %, 1.5 %, 0 …
Non-tower elements are *not* rejected by the tower rule: adaptive accepts 4,510/4,510 non-tower candidates through T = 240 and 3,984/4,510 at T = 260 (the tallest, temple 45, no longer fits there); baseline loses non-towers from T = 202 (4,497) down to 1,001 at T = 260.

## Frequency caveat
Terrain at T ≥ 180 is 1.7 % of mountain candidates; the overall rarity change (+2.8 % towers, +0.4 % islands) is small by construction. Absolute numbers are for these 5 seeds; seed-to-seed spread of the baseline in Phase 2 was 485–510 per 10,000 km².
