# ST-01 P8-D2 — Explorer-map XP quest compatibility audit (code and data read; no runtime test needed)

Question: could ST-01 explorer maps trigger or duplicate existing XP rewards, in particular `xpq.exp.explorer_maps`?

## Findings
| # | Finding | Evidence | Status |
|---|---|---|---|
| 1 | `xpq.exp.explorer_maps` is **unregistered** and its tracker is `pending`; nothing can complete it today | `data/moreminecrafts/xp_quest/catalog.json:100-111` (`"registered": false`, `"tracker": {"kind":"pending"}`); `XpQuestCatalogData` says `pending` quests get hooks later; no code references the id (`grep` over `src/`) | NO current hook |
| 2 | The approved design of that quest is map-specific: collect each of **three** explorer maps — **ocean, woodland, trial** — and enter *the exact structure that map targets*, in order (acquisition → matching visit); a visit without the matching map does not count | `docs/design/xp-quest/audit-200/OWNER_DECISIONS_2026-10-21.md:48`; `A_XP_QUEST_CATALOG.md:74` | design only |
| 3 | An ST-01 map is **not one of those three**, so under the approved design it cannot complete or duplicate that quest, *provided the future hook identifies maps by their target structure / vanilla map item*, not by "any filled map" | derived from 2 | conditional |
| 4 | The quest needs "each map's actual target identity" recorded. A vanilla `exploration_map` stack records **no structure identity**: it carries only `map_id`, a decoration at the target position and the (optional) item type; vanilla per-structure items (`ocean_monument_map`, `woodland_mansion_map`, `buried_trial_chambers_map`, … `Items` in 26.3) are what identifies them | `ExplorationMapFunction.run` bytecode (map id, `addTargetDecoration`); fixture result (`VANILLA_EXPLORER_MAP_AUDIT.md`) | design risk for the future hook, not for ST-01 |
| 5 | If the future hook keys on item type `minecraft:filled_map` or on any exploration decoration, a custom ST-01 map using `filled_map` (+`target_x`) could be miscounted. A dedicated map item or a decoration/identity that is not ocean/woodland/trial avoids it | derived | **owner/implementation requirement** for P8-D3/D4 |
| 6 | **Existing, independent of maps:** `xpq.exp.structures_15` is `distinct_set` over `set.structure_types_entered` with **no entry filter**, and `QuestStructureEntry.observe` records every structure type the player stands in a piece of, so entering an ST-01 Pyramid already counts as one more distinct structure type. This happens with or without any map | `catalog.json:248-259`; `progression/QuestStructureEntry.java:34-57` | existing behavior; flag only |
| 7 | `xpq.exp.grand_expedition` and `xpq.exp.villages_5` use explicit entry lists that do not contain ST-01 | `catalog.json:146-163, 261-275` | no effect |
| 8 | `quest_presentation.json:336` uses `minecraft:filled_map` only as the **icon** of `grand_expedition` | `assets/moreminecrafts/progression/quest_presentation.json:336` | cosmetic |
| 9 | No advancement in `data/moreminecrafts/advancement` references maps or cartography | `grep` of the advancement tree | none |

## Conclusions
- **No accidental XP today** and none from ST-01 maps as designed. Verdict: PASS (audited), with the implementation requirement of finding 5.
- The quest code that already handles structure *overlap correctly* is relevant to the bounty detector: `QuestStructureEntry.observe` loops over
  `structureManager.getAllStructuresAt(pos).keySet()` and calls `getStructureWithPieceAt(x, y, z, structure)` per structure — see `OVERLAP_DETECTION_AUDIT.md`.
- Out of scope and untouched: the quest catalog, rewards and trackers.
