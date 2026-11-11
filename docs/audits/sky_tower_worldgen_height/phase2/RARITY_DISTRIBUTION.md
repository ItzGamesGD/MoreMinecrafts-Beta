# Sky Tower — Rarity & Distribution Benchmark (Phase 2)

Labels: `RUNTIME_CONFIRMED` · `MATHEMATICALLY_DERIVED` · `INFERRED` · `UNVERIFIED`. Intervals are 95 % Wilson. "Eligible candidate" = a placement-selected chunk whose biome column accepts the drawn element (the engine's cheap pre-check), i.e. a candidate that reaches the height/padding test.

Data: **production behaviour** = 12 natural Overworld seeds (2–13), radius 2,500 chunks each (≈ 76,831 km² total; 945,977 placement candidates; 116,950 eligible; engine `Structure.generate` REPLAY, 85,154 of those chunks additionally verified against the NATURAL pipeline with 0 disagreements). **Paired strategy comparison** = 8 of those seeds (2–9), the *same* 18,787 eligible mountain-set candidates evaluated under every configuration (51,220 km²). Raw per-candidate rows: `PLACEMENT_TRACE.csv` (production, committed subset) and `REMEDIATION_BENCHMARK.csv` (aggregates).

## 1. The four quantities (kept separate)

| # | Quantity | Nominal | Observed (natural, production, mountain set) |
|---|---|---|---|
| 1 | **Tower selection probability within the start pool** (`3/21`) | 14.29 % | 3,850 / 27,189 = **14.16 %** [13.75, 14.58]; general set 12,727 / 89,761 = 14.18 % [13.95, 14.41]. No dependence on the sampled start height (χ² 1.29, df 3, crit 7.81) |
| 2 | **Conditional tower acceptance** `P(valid start | tower drawn)` | 100 % | **96.86 %** [96.26, 97.36] overall (3,729 / 3,850), but strongly terrain-dependent (§2). General set: 99.17 % [99.00, 99.32] |
| 3 | **Absolute world-generation frequency** (accepted towers per area) | not derivable from 3/21 | mountain set: **485 per 10,000 km²** (12 seeds: 3,729 towers / 76,831 km²); 510 per 10,000 km² on the 8 paired seeds; per 1,000 eligible mountain candidates (8 paired seeds): 139.0 [134.1, 144.0]. General set: 12,622 towers / 76,831 km² = 1,643 per 10,000 km² |
| 4 | **Tower share among successfully generated islands** | 14.29 % | mountain **13.79 %** [13.38, 14.20] (3,729 / 27,051); at T ≥ 180 only 4.8 % [2.9, 7.9] (14 / 292), at T 200–219 0 % |

The nominal 3/21 is quantity 1 only. Quantity 3 additionally multiplies by the `random_spread` hit rate (`spacing 14, separation 5, frequency 0.5`), biome eligibility, and quantity 2; it is a *terrain-weighted* number, not a constant. `MATHEMATICALLY_DERIVED` + `RUNTIME_CONFIRMED`.

## 2. Conditional acceptance by natural terrain (production, 12 seeds, mountain set)

| Terrain top block T | eligible | tower selected (share) | **tower accepted of selected** | non-tower accepted / selected | islands generated | tower share of generated |
|---|---|---|---|---|---|---|
| < 100 | 8,393 | 1,159 (13.8 %) | 1,159 (100 %) | 7,229 / 7,234 | 8,388 | 13.8 % |
| 100–139 | 14,507 | 2,049 (14.1 %) | 2,047 (99.9 %) | 12,457 / 12,458 | 14,504 | 14.1 % |
| 140–159 | 2,774 | 443 (16.0 %) | 426 (**96.2 %** [93.9, 97.6]) | 2,331 / 2,331 | 2,757 | 15.5 % |
| 160–179 | 1,042 | 125 (12.0 %) | 83 (**66.4 %** [57.7, 74.1]) | 917 / 917 | 1,000 | 8.3 % |
| 180–199 | 330 | 52 (15.8 %) | 14 (**26.9 %** [16.8, 40.3]) | 278 / 278 | 292 | 4.8 % |
| 200–219 | 104 | 15 | 0 (0 %, upper bound 20.4 %) | 87 / 89 | 87 | 0 % |
| 220–239 | 33 | 6 | 0 | 22 / 27 | 22 | 0 % |
| ≥ 240 | 6 | 1 | 0 | 1 / 5 | 1 | — |

*Sample-size limitation:* only 473 of 27,189 mountain candidates (1.7 %) lie at T ≥ 180 and 22 towers at T ≥ 200. Their rates are reported with counts and intervals; do **not** read the T ≥ 200 rows as precise percentages (the elevation law itself is exact, see `CORRECTED_HEIGHT_MODEL.md`; what is uncertain is only how often natural terrain is that high).

General set (40..110): tower accepted 100 % at T < 100, **93.7 %** [92.2, 94.9] at 100–139 (1,166 / 1,244), 48.7 % at 140–159 (19 / 39), 3 / 7 at 160–179. The general set therefore also loses towers (105 of 12,727, 0.8 %) — more than Phase 1 stated.

**Impact of "no change" (strategy E), measured:** 121 of 3,850 mountain towers (**3.1 %**) are lost naturally; as a fraction of *all* mountain-set islands 0.45 % [0.37, 0.53]. Loss is concentrated in alpine biomes: frozen_peaks 159 / 199 accepted, jagged_peaks 131 / 166, stony_peaks 88 / 109 (≈ 20 % lost each), grove 98 %, snowy_slopes 98.6 %; meadow, cherry_grove, windswept_hills / gravelly: 100 %. This directly contradicts "100 % extinction on jagged peaks" for the natural world and "meadow moderately affected".

## 3. Paired comparison of configurations (8 seeds, identical 18,787 eligible mountain candidates; 2,699 of them draw a tower)

| Config | islands generated | towers generated | towers per 1,000 eligible candidates | selected towers that generated | tower share of generated | mean clearance of towers (min..max) |
|---|---|---|---|---|---|---|
| **E: current 18..70** | 18,687 | 2,611 | 139.0 [134.1, 144.0] | 2,611 / 2,699 = 96.7 % [96.0, 97.3] | 14.0 % | 43.3 (18..70) |
| A 15..35 (Phase 1) | 18,750 | 2,672 | 142.2 [137.3, 147.3] | 99.0 % [98.5, 99.3] | 14.25 % | **25.1** (15..35) |
| A 18..35 | 18,747 | 2,668 | 142.0 | 98.9 % | 14.2 % | 26.3 (18..35) |
| A 10..30 | 18,752 | 2,677 | 142.5 | 99.2 % | 14.3 % | 20.1 (10..30) |
| A 5..20 | 18,760 | 2,687 | 143.0 | 99.6 % | 14.3 % | 12.6 (5..20) |
| A 1..18 / 1..10 | 18,755 | 2,687 | 143.0 | 99.6 % | 14.3 % | 9.3 / 5.5 |
| **B dynamic 18..70** (truncate to feasible) | 18,765 | 2,683 | 142.8 | 2,683 / 2,699 = **99.4 %** [99.0, 99.6] | 14.3 % | **42.9** (18..70) |
| **B dynamic 1..70** | 18,772 | 2,692 | 143.3 | 2,692 / 2,699 = **99.7 %** [99.5, 99.9] | 14.3 % | 34.8 (1..70) |
| C retry ×3 (re-roll height) | 18,745 | 2,662 | 141.7 | 98.6 % | 14.2 % | 43.2 |
| C retry ×8 | 18,757 | 2,673 | 142.3 | 99.0 % | 14.25 % | 43.1 |
| C substitute element (non-tower fits) | 18,778 | 2,611 | 139.0 | 96.7 % (towers unchanged) | 13.9 % | 43.3 |
| C relocate ≤ 2 chunks | 18,772 | 2,687 | 143.0 | 99.6 % | 14.3 % | 43.6 |
| D tower-only 18..70 / 1..20 | 16,062 / 16,590 | (all towers) | not comparable (§5) | — | 100 % | 43.6 / 10.4 |

Paired totals differ by ≤ 81 towers of 2,699 (3 %); the effect of any strategy on the *overall* rarity is small because the failure range is rare terrain. The paired **high-terrain stratum (T ≥ 180, 60 selected towers, natural)**: current 11 (18.3 % [10.6, 29.9]); A 15..35 34 (56.7 % [44.1, 68.4]); B 18..70 45 (75.0 % [62.8, 84.2]); B 1..70 54 (90.0 % [79.9, 95.3]); the remaining 6 in B 1..70 sit at T ≥ 220 where **no** placement of a 97-block piece with clearance ≥ 1 is possible. (Per-bin counts for every config: `REMEDIATION_BENCHMARK.csv`, rows `source=NATURAL_REPLAY(paired)`.)

## 4. Terrain and biome distribution effects

* **Terrain distribution of mountain candidates (natural):** 84 % lie at T < 140 (22,900 / 27,189); T ≥ 140: 4,289 (15.8 %); T ≥ 160: 1,515 (5.6 %); T ≥ 180: 473 (1.7 %).
* **Biome distribution of generated towers** (8 seeds; share of all towers): `frozen_peaks` 106 (4.1 %) → A 15..35: 129 (4.8 %) → B 1..70: 135 (5.0 %); `jagged_peaks` 89 (3.4 %) → 106 / 112 (4.2 %); `stony_peaks` 65 (2.5 %) → 73 / 81 (3.0 %). Meadow stays 872 under every non-relocating strategy: those strategies change **only** alpine rows, i.e. they *restore* the intended biome mix.
* **Relocation (C) distorts biome:** it rescued 85 towers, mean terrain change −30 blocks; the surface biome changed for 46 / 85 (54 %), mostly alpine → `grove` (jagged → grove 15, frozen → grove 14, frozen → snowy_slopes 6); grove towers 556 → 605 while jagged_peaks stays 89 and frozen_peaks 106 — alpine towers are *not* recovered, they are moved down the mountain. `RUNTIME_CONFIRMED` (replay); would also break the structure-start/chunk association assumptions (§SOURCE of `JigsawStructure`), `INFERRED`.
* **Stub-biome interaction:** lowering the start height (A) moves the sky position, so a few more candidates pass/fail the sky-altitude biome test; the paired data show no net adverse effect (generated islands rise by 63 for A 15..35).

## 5. Tower-only dedicated placement (D) — what can and cannot be said

A dedicated tower structure would have its own placement density, which is a design parameter (**OWNER DECISION**; not inferred here). The acceptance of a drawn tower at a candidate, per terrain bin, is measurable (per eligible candidate, mountain set):

| T bin | candidates | 18..70 | 1..20 |
|---|---|---|---|
| < 100 | 5,743 | 82.0 % | 81.9 % |
| 100–139 | 9,959 | 90.5 % | 90.4 % |
| 140–159 | 1,960 | 89.4 % | 94.9 % |
| 160–179 | 762 | 64.6 % | 95.3 % |
| 180–199 | 250 | 36.4 % | 93.6 % [89.9, 96.0] |
| 200–219 | 83 | 10.8 % | 65.1 % [54.3, 74.4] |
| 220–239 | 24 | 0 % | 29.2 % [14.9, 49.2] |

The 82–90 % ceiling at low terrain is independent of the range (so not a height effect); it is consistent with the footprint-centre biome pre-check of the 58×49 composite missing narrow biome patches (`INFERRED`, not isolated). D with a low range fixes the same terrain as A/B but changes the pool weights (tower removed from six start pools) and needs a new structure set — a tower-rarity design change.

## 6. Provenance of every number

`tools/sky-tower-phase2/analyze.py` → `phase2_stats.json` (not committed; regenerate with the command in `RUNTIME_REPRODUCTION.md §7`). No percentage here is taken from Phase 1.
