# Adamantium world generation (F-3) — mismatches, measurements and bounded alternatives

> **Historical analysis (2026-10-27).** The choice it asks for was made by the owner on 2026-10-28: D-F3-1 every generated block in Y 8–22, D-F3-2 one attempt per chunk, D-F3-3 about 50% of Ancient Debris in aggregate matched samples (this replaces the ~80% tuning target), O-1 one placed feature. Candidate E is approved for integration. See `docs/preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md`. Nothing below is authority.


Date: 2026-10-27. Status: **analysis and proposal only.** No shipped gameplay data was changed; the pre-F-3 files are pinned by a characterization test. Choosing between
the alternatives below is an owner decision, because they differ in abundance and in how "Y 8–22" is read.

## 1. The approved rule (exact)

Source: F-3 in [PHASE2_OWNER_DECISIONS_2026-10-26.md](../../preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md), applied by
[PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md](../../preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md#f-3--adamantium-world-generation), with the 2026-10-18 design
[ADAMANTIUM_EQUIPMENT_DESIGN](../../preservation/recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) §"Ore generation and refining":

1. Nether only.
2. Height **Y 8–22**, biased toward **Y 15**.
3. Vein size **1–2** ore blocks.
4. **Binding acceptance:** Adamantium must produce **fewer generated ore blocks than Ancient Debris** in matched samples of comparable newly generated chunks. Validation must
   disclose dimension, biome and height-sampling method. "Vein size alone is not evidence of rarity."
5. The 2026-10-18 "approximately 80% of ancient debris" target "stays as a tuning target"; it is **not** the binding criterion.
6. "The current data (uniform Y 8–24, size 3) is **not** authoritative. Correcting it is a separate implementation task."

## 2. What ships today

| File | Value |
|---|---|
| `worldgen/feature/ore_adamantium.json` | `minecraft:ore`, size 3, discard on air exposure 1.0, targets `#minecraft:base_stone_nether` → `moreminecrafts:adamantium_debris` |
| `worldgen/placed_feature/ore_adamantium.json` | count 1, in_square, `height_range` **uniform** Y 8–24, biome filter |
| `worldgen/ModOreGeneration` | `BiomeSelectors.foundInTheNether()`, `UNDERGROUND_DECORATION` |

Vanilla Ancient Debris, read from the 26.3 game files (`minecraft-common.jar`): `ore_ancient_debris_large` = `scattered_ore`, size 3, discard 1.0, **no count modifier**, trapezoid
Y 8–24; `ore_debris_small` = `scattered_ore`, size 2, discard 1.0, uniform Y 8 to top−8. Both are in `UNDERGROUND_DECORATION` on every Nether biome.

## 3. Method (disclosure required by F-3)

- **Dimension:** `minecraft:the_nether`. **Generator:** the real vanilla Nether generator, 26.3.
- **Matched sample:** one dedicated server per cell; a 40×40-chunk square around the origin forced to FULL generation; only the 36×36 = **1,296 interior chunks** counted (2-chunk
  margin, because feature placement writes into neighbouring chunks). Ancient Debris and Adamantium are counted **in the same chunks**. Seeds **1, 2, 3** → **3,888 chunks** per configuration.
- **Height-sampling method:** every block from the dimension's min Y to max Y (0–255) is scanned; counts are of generated blocks, not of placement attempts, so air-exposure
  discard, terrain and spill are all included. Y is the block's own Y.
- **Biome mix** (chunk centre, pooled over the 3 seeds): nether_wastes 57.4%, soul_sand_valley 18.6%, basalt_deltas 12.3%, crimson_forest 9.6%, warped_forest 2.1%.
- **Vein shape:** connected components of ore blocks using 26-neighbour adjacency. It cannot tell one vein from two adjacent veins, so clusters of 3–4 may be two touching 1–2 veins.
- **Deterministic:** re-running the same seed reproduced a byte-identical result (checked). Variants are applied as a datapack override, with no code change.
- **Reproduce:** `tools/ore-sampling/run_sampling.sh <label> <seed> [variant.json|baseline] [radius]`, then `tools/ore-sampling/check_f3.py`. Raw results: [`adamantium/results/`](adamantium/results/).
- **Limits:** terrain varies by seed (Ancient Debris itself varied 1,907–2,081 per seed), so the per-seed ratio range is the honest uncertainty. Three seeds is a bounded sample, not a
  proof of the long-run rate. No live play was done.

## 4. Mismatches between what ships and F-3

Measured on the shipped data (`baseline_current`, 3,888 chunks):

| # | F-3 requirement | Shipped | Evidence | Verdict |
|---|---|---|---|---|
| M1 | Nether only | Nether biomes only | `AdamantiumF3WorldgenTest.adamantiumIsRegisteredOnlyForNetherBiomes`; no Adamantium in any Overworld sample | **Matches** |
| M2 | Height Y 8–22 | Placement uniform Y 8–24 | Blocks found at **Y 6–23**; **189 of 1,599 (11.8%)** lie outside Y 8–22 | **Mismatch** |
| M3 | Biased toward Y 15 | Flat distribution (uniform) | Only **30%** of blocks in Y 13–17; the exact triangle centred on 15 puts 34/64 = **53%** there (observed 53–54% for the origin 8–22 candidates); a unit test shows the uniform provider is flat within 10% | **Mismatch** |
| M4 | Vein size 1–2 | `ore` size 3 | **21** clusters of 3–4 blocks among 1,235 (1.7%) | **Mismatch** (small share) |
| M5 | Fewer blocks than Ancient Debris | 1,599 vs 6,068 | **26.4%** of debris (26.2–26.6% per seed) | **Already satisfied** |
| M6 | ~80% tuning target | 26.4% | far below | **Below target** (not binding) |

Two further findings that change how F-3 can be implemented:

- **`minecraft:ore` cannot express "vein size 1–2".** With size 2 it generated **zero** Adamantium in 400 chunks (seed 1) at 1, 2, 3, 4, 5 and 6 placements per chunk
  ([`grid-scan-400-chunks/ore2_c*`](adamantium/grid-scan-400-chunks/)). A JSON-only check passes such a configuration. Only `scattered_ore` (what vanilla Ancient Debris uses) produces
  1–2 block veins. So even a minimal fix changes the feature **type**, not just two numbers.
- **`scattered_ore` spills one block vertically.** With the origin range 8–22 it produced blocks at Y 7 and Y 23 (0.2% of blocks). Only an origin range of **9–21** keeps every
  generated block inside Y 8–22, and that range is still centred on Y 15.

## 5. Height provider: no numeric choice is needed

`trapezoid` from 8 to 22 with no plateau is exactly "Y 8–22, biased toward Y 15": Minecraft's `TrapezoidHeight` draws `min + rand(0..7) + rand(0..7)`, a triangle whose maximum is
**Y 15**. `AdamantiumF3WorldgenTest` verifies this with the real `TrapezoidHeight` class (support exactly 8–22, mode 15, counts within 5σ of the exact distribution). The same shape with
origin 9–21 also peaks at exactly 15.

## 6. Bounded alternatives (what is decided and what is left open)

**Fixed by F-3, identical in every candidate:** `scattered_ore`, size 2, discard 1.0, `#base_stone_nether` target, trapezoid without plateau, in_square and biome filters, Nether-only registration.
**Left open for the owner (the only differences):** placements per chunk, and the origin range.

Results over **3 seeds × 1,296 chunks** (generated by `check_f3.py` from the raw JSON, [`adamantium/f3_summary.md`](adamantium/f3_summary.md)):

| Candidate | Placements / chunk | Origin Y | Adamantium vs Ancient Debris (pooled; per-seed range) | Blocks / chunk | Block Y range | Blocks outside Y 8–22 | Y 13–17 share | Clusters (1 / 2 / 3+) | Binding rule |
|---|---|---|---|---:|---|---:|---:|---|---|
| shipped (`ore`, size 3) | 1 | uniform 8–24 | 26.4% (26.2–26.6%) | 0.411 | 6–23 | 189 (11.8%) | 30% | 903 / 311 / 21 | pass |
| **A** | 1 | 8–22 | 49.2% (47.5–51.8%) | 0.769 | 7–23 | 7 (0.2%) | 53% | 1686 / 651 / 0 | pass |
| **B** | 1–2 (uniform) | 8–22 | 72.5% (67.5–78.3%) | 1.132 | 7–23 | 5 (0.1%) | 54% | 2510 / 942 / 2 | pass |
| **C** | 1 | 8–21 | 49.8% (47.8–52.4%) | 0.777 | 7–21 | 3 (0.1%) | 55% | 1698 / 661 / 0 | pass |
| **D** | 1–2 (uniform) | 8–21 | 73.3% (67.5–79.5%) | 1.144 | 7–22 | 8 (0.2%) | 55% | 2523 / 955 / 4 | pass |
| **E** | 1 | 9–21 | 49.6% (48.0–52.2%) | 0.774 | 8–22 | **0** | 60% | 1697 / 654 / 1 | pass |
| **F** | 1–2 (uniform) | 9–21 | 73.2% (68.5–78.8%) | 1.142 | 8–22 | **0** | 61% | 2520 / 954 / 4 | pass |

Reading the table:

- **Every F-3-compliant candidate is rarer than Ancient Debris on every seed**, so the binding criterion holds with margin; the worst per-seed ratio is 79.5%.
- **Every candidate raises abundance relative to what ships (26%).** F-3 requires the vein-size and height changes, and `scattered_ore` yields about 1.9× more blocks per placement
  than the size-3 `ore` it replaces. Even the single-placement candidates therefore change the economy, toward the owner's 80% tuning target. This is a balance consequence of F-3, not a free choice.
- **Candidates ending at Y 21/22 for the origin (A–D) all produce a few blocks at Y 7.** Only E and F keep every block inside Y 8–22.
- Clusters of 3–4 in B, D, E and F are two separate 1–2 veins touching, not a larger vein (a single placement cannot exceed 2).
- Reaching the "≈80%" target more closely than B/F (72–73%) needs a non-uniform count (an average near 1.6 placements). That is a further balance value and was **not** sampled or chosen.

## 7. Smallest change consistent with the approved design

Changes required by F-3, all four mandatory:

1. `feature/ore_adamantium.json`: `type` `minecraft:ore` → `minecraft:scattered_ore`.
2. `feature/ore_adamantium.json`: `size` 3 → 2.
3. `placed_feature/ore_adamantium.json`: `height_range` uniform 8–24 → trapezoid (no plateau).
4. the trapezoid's range, which is the single interpretive question (below).

**No new numeric balance value is introduced by candidate A or E**: the placement count stays at its shipped value of 1. These two are the smallest change. They differ in how F-3's "Y 8–22" is read:

- **A** reads it as the vein origin range, literally 8–22: the minimal text change; 0.2% of blocks sit at Y 7 or 23.
- **E** reads it as the range of **every generated block**: origin 9–21, zero blocks outside Y 8–22, still peaking at Y 15.

My recommendation is **E** if the owner means "no Adamantium outside Y 8–22", and **A** if the origin range is what was meant. I did not apply either: both raise abundance from about 26% to about 49% of Ancient
Debris, which is a gameplay-economy change the owner should accept explicitly. B, D and F additionally introduce a new placement-count distribution.

Files for the implementation task, once decided, are exactly `tools/ore-sampling/alternatives/<candidate>.json` (`feature` and `placed_feature` objects, copied to the two shipped paths); the unit test
`shippedAdamantiumDataIsStillThePreF3Configuration` must then be updated in the same change, and `check_f3.py` re-run on the shipped data.

## 8. Decisions needed

| ID | Question | Options | Default if unanswered |
|---|---|---|---|
| D-F3-1 | Does "Y 8–22" bound the vein origin or every generated block? | origin (A/B/C/D) or every block (E/F) | none; implementation stays blocked |
| D-F3-2 | Placements per chunk / how close to the 80% tuning target | 1 (≈49%), uniform 1–2 (≈73%), or another distribution (not sampled) | none |
| D-F3-3 | Accept that any compliant configuration raises abundance above the shipped 26% | yes / no | none |

Implementation can then be done as one small change with the measurements above as acceptance evidence, and no new design work.
