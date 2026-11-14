# 04 — Structure overlap safety matrix

Status: **PARTIAL.** Bounding-box intersections are measured on all 172 B starts. Actual block outcomes are measured on 6 B-only sites and 1 D site, on real servers. Entity outcomes and the visual result of sliced structures remain unverified.

The owner's overlap permission ([AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED](../st01-p8d1/AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED.md)) is preserved. **No overlap rejection rule, mask or suppression was implemented.**

## 1. Bounding-box intersections (all eligible anchors, 20 seeds)

Method: vanilla structure starts referenced by the 36 footprint chunks (`STRUCTURE_REFERENCES`). For each start, count the piece boxes that intersect the ST-01 volume, i.e. the 81×81 footprint from Y−32 to Y+39 plus a one-block ring. The candidate's own production ST-01 start is excluded, as are starts whose overall box touches but none of whose pieces does. Source: [results/overlap-split.md](results/overlap-split.md).

| Set | Anchors | With ≥ 1 intersecting vanilla piece | With piece cells inside the excavation band | With pieces crossing the footprint edge (sliced) |
|---|---|---|---|---|
| Policy D (current production) | 46 | 10 (22 %) | 3 | 8 |
| Policy B only | 126 | 33 (26 %) | 14 | 28 |
| **All Policy B** | **172** | **43 (25 %)** | **17** | **36** |

By structure, over all Policy B anchors:

| Structure (step) | Starts intersecting | Piece-box cells in ST-01 volume | Piece-box cells in excavation band | Starts with pieces crossing the edge | Present under D? |
|---|---|---|---|---|---|
| mineshaft (underground) | 18 | 18,703 | 0 | 14 | yes (6) |
| village_desert (surface) | 17 | 283,724 | 3,973 | 17 | yes (5) |
| desert_pyramid (surface) | 7 | 22,425 | 573 | 5 | **new under B** |
| pillager_outpost (surface) | 2 | 22,470 | 2,324 | 1 | **new under B** |
| ruined_portal_desert (surface) | 2 | 1,348 | 345 | 0 | **new under B** |
| mineshaft_mesa (underground) | 1 | 897 | 0 | 1 | **new under B** |

Method note: the amendment records **16 vanilla overlaps over the 46 Policy D pyramids** (a per-overlap count from the P8-C2-C evidence). The 10 here counts *anchors* that have at least one intersecting vanilla *piece* box; starts whose overall box touches the volume but none of whose pieces do are excluded. Both measures are kept; they are not contradictory.

The overlap **rate** per start is about the same under D (22 %) and B (25 %). The **absolute number** of affected vanilla structures per area rises with density, ×4.3 (10 → 43), and three structure types appear that no D-eligible site touched in this sample.

## 2. Actual block outcomes on real servers

Two complementary measurements were taken.

**(a) What correction and assembly would destroy.** The production world (Policy D) was generated to FULL. Before running the production `correct()` for the B plane, the probe counted non-natural, non-ST-01-palette blocks inside the vanilla piece boxes within the footprint ([results/correction](results/correction)):

| Site (B-only) | Structure | Blocks below Y0, overwritten by the body | In the excavation band (cleared) | Above the clearance band | Notable |
|---|---|---|---|---|---|
| 1004 −9416,−7576 | pillager_outpost | 242 | 466 | 537 | outpost tower: dark-oak logs and planks, wool, fences |
| 914490140 7368,3848 | village_desert | 75 | 296 | 0 | hay, farmland and crops, doors, torches, pots |
| 287862831 −7976,−5992 | desert_pyramid + village | 18 | 53 | 0 | terracotta, 1 suspicious sand (archaeology) |
| 1541117449 −9976,−2248 | ruined_portal_desert | 0 | 209 | 0 | netherrack, obsidian, **1 loot chest** |
| 527213223 −9160,−2376 | mineshaft | 1,238 | 0 | 0 | planks, fences, rails, cobwebs |
| 1034165336 4216,−3848 | none | 0 | 0 | 0 | — |

**(b) What actually remains after real Policy B generation.** The same probe pre-scan was run in the disposable Policy B worktree, where the real B start had been generated ([results/actual-generation-overlap](results/actual-generation-overlap)):

| Site | Structure | Non-natural blocks left inside the footprint in vanilla piece boxes | Of which vanilla blocks |
|---|---|---|---|
| 1004 −9416,−7576 | pillager_outpost | 7 (`wall_torch`) | **0**: `wall_torch` is in the ST-01 assembly palette |
| 1001 −6360,−2872 | desert_pyramid | 0 | **0** |
| 914490140 7368,3848 | village_desert | 13 (`wall_torch`) | **0** |
| 1541117449 −9976,−2248 | ruined_portal_desert | 0 | **0** |
| 527213223 −9160,−2376 | mineshaft | 109 (diamond, gold, lapis blocks, wall torches) | **0**: all four block types are in the ST-01 assembly palette |
| 287862831 −7976,−5992 | desert_pyramid + village | 0 | **0** |

Policy D reference (seed 1001, 776,−6936, village overlap, production world): **0**.

**Finding.** In real generation the ST-01 start wins inside its own footprint, under both policies. No vanilla block survives inside the 81×81 area, so there are no floating remnants: the 537 outpost blocks above the clearance band in (a) are absent in (b). The 0 owned-cell mismatches of the verifier on all 21 experiment starts ([07](07-determinism-and-chunk-safety.md)) agree. Vanilla structures are therefore **cut at the footprint edge**: the parts outside stay, the parts inside become pyramid.

## 3. Damage categories

| Category | Under D | Under B | Evidence |
|---|---|---|---|
| Bounding-box intersection | 10 / 46 starts | 43 / 172 starts | §1 |
| Actual block intersection inside the footprint | vanilla blocks inside are replaced | same mechanism, ×4.3 more often | §2 |
| Excavation damage | 3 starts with piece cells in the band | 17 starts | §1, §2(a) |
| Foundation (body) overwrite | mineshafts and village paths below Y0 | adds outposts and temples | §2(a) |
| Edge-blending damage | none (no blending exists) | none | [03](03-terrain-correction-architecture.md) |
| Sliced structures at the footprint edge | 8 starts | 36 starts | §1 |
| Generation-order conflicts | none observed: ST-01 replaces everything inside, in all chunk orders | none observed (hashes identical in NORMAL / REVERSE / RANDOM) | [07](07-determinism-and-chunk-safety.md) |
| Chunk-border inconsistencies | none | none observed | [07](07-determinism-and-chunk-safety.md) |
| Loot and archaeology loss | possible | observed: a ruined-portal chest and desert-temple suspicious sand inside the footprint are replaced | §2(a) |
| Entities (villagers, pillagers, mineshaft chest minecarts) inside the footprint | **unverified** | **unverified** | not measured |

## 4. Protective options (owner decision; none implemented)

| Option | What it would do | Conflict with current authority |
|---|---|---|
| Accept as today | keep the permission; vanilla parts inside the footprint are replaced | none; this is current behaviour, scaled ×4.3 |
| Protective mask | skip ST-01 writes on vanilla piece cells | a *suppression* of ST-01 content, which the amendment excludes as a reaction to overlap; also breaks "the assembly owns every footprint cell" and the exact owned-cell placement the verifier checks; a pyramid with village fragments inside |
| Local correction suppression | skip excavation only where vanilla pieces exist | a suppression rule (excluded by the amendment); leaves vanilla blocks or natural terrain in structure-void cells above Y0; same contract issue |
| Deterministic priority (vanilla wins on some types) | e.g. never place over desert pyramids or outposts | a rejection or relocation rule; the amendment says overlap must not become a rejection, exclusion, suppression or relocation rule |
| Edge cleanup of sliced vanilla pieces | remove fragments outside the footprint | writes outside the footprint; not approved |

**Unresolved and reported explicitly:** sliced vanilla structures at the edge (36 starts), entity fate inside the footprint, and loot or archaeology loss. Each needs an owner decision before Policy B ships.
