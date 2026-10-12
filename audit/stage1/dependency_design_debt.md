# Dependency and design debt

## Decision blockers

1. **V3 purchase graph:** all `v3.research.01`–`v3.research.70` have `prerequisitesDefined=false` (`research_v3.json`), and `ResearchPurchaseRules.java:23-31` correctly rejects purchases. The older design mapping (`RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md:396-419`) is explicitly directional, not a finalized graph. Approve real knowledge/research edges before enabling purchases. Do not infer edges from `node_layout.json` branch order.
2. **Rare mobs:** Knowledge #51 lacks an approved three-species list; its collector is absent. V4 `mob_capture` depends specifically on #51. Decide species and the exact ecology +5% family taxonomy before implementing either. The ecology design expressly forbids stacked +5% from overlapping families (`RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md:220-230`).
3. **V3 effect precision:** several V3 effects have approved mechanics in design, while others retain follow-up details. Reconcile latest overlays per node before programming. In particular the older tier sequence rule was superseded in current authority. Do not use names, cost rows, or icon order as gameplay specifications.
4. **Knowledge information reward surface:** approved information rewards need a readable information artifact or screen. `rewardKey` currently only reaches `ProgressionScreenData.java:29`; decide presentation/content without inventing numeric boosts.

## Shared missing implementation

- **Event collectors:** no runtime writer was found for `ore_types_smelted` (#11), `shield_blocks` (#36), `special_ammunition_used` (#40), `piglin_barters` (#48), `rare_mob_types_encountered` (#51), `archaeology_items_brushed` (#59), and brewing keys for #63–66/#69–70. Catalog conditions and state APIs alone do not advance these entries.
- **Condition semantics:** #5 all-log counting, #12 OR instead of AND, #14 crafted total instead of kinds, #30 craft count instead of ten food types and cooking, and #34/#35/#38 death-time held-item inference each differ from intended triggers. #22/#23/#25 observe attempted interactions; validate success before credit. #8 eight reachable rock types need live or unit demonstration. These are independent of reward implementation.
- **Knowledge rewards:** numeric XP/drop/durability bonuses and all twelve ecology +5% damage effects need real event consumers. Information rewards need inspectable content. Recipe rewards need a complete server crafting gate across 2×2 and 3×3 plus recipe book/automation review; `KnowledgeCraftingMenuMixin.java:14-22` only targets `CraftingMenu.slotChangedCraftingGrid`.
- **Research effects:** `ResearchRewards.java:15-31` lists 17 metadata entries but has one external gameplay use, for `combat_root` in `LivingEntityMixin.java:59-65`. Movement actions read researched state directly in `ResearchMovementRuntime.java:31-105`. Other V4 effects need state-gated consumers and direct tests. V3 has no node-specific state consumer found.
- **Persistence and sync:** the shared attachment, copy on death and delta sync are present (`KnowledgeRuntime.java:25-29,190-238`; `ProgressionSyncRuntime.java`). Add focused tests for condition unlock, reconnect and multiplayer visibility when implementing collectors; no new persistence format is indicated by this audit.

## Existing functionality and duplicate risks

- `AutoFarmBlockEntity`, `FishTrapBlockEntity`, `FishingLogic`, `MobCaptureRuntime`, `FilteredChestMenu` and `ParachuteItem` contain functioning or partial stand-alone features relevant to V3/V4 names. They are not evidence that a purchased Research changes gameplay. Later work should inspect their precise entrypoints and attach eligibility or level behavior only where the approved design requires it, preserving base features.
- `Lumberjack` enchant already handles connected tree felling. Current design retains a separate 1×1 timed lumber machine and rejects a paid controlled-logging node (`RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md:767`). Reusing the enchant as the machine effect would duplicate or alter approved scope.
- Knowledge #41–52 share one ecology damage framework; build a single taxonomy and one applicable 5% bonus per target. Knowledge #60/#62 share `structures_visited` with different thresholds; retain both.
- V4 `combat_root` and `ecology_root` are separate 1% paid effects. Neither substitutes for Knowledge ecology's 5% species bonus. Decide stacking once, with tests.
- The 13 Knowledge recipe JSON files are approved alternatives. Preserve vanilla recipes and avoid broad recipe blocking. `KnowledgeCraftingRecipeTest.java` checks JSON structure, not live output denial.

## Priority of independently safe work

Collector, semantic-counter and test work can proceed without changing approved prices or V3 purchase rules. Implementing V3 purchase edges, rare-species taxonomy or ambiguous V3 effect details needs explicit design decisions. V4 purchase service and persistence are shared infrastructure; effect integration can be split by discipline without creating parallel progression states.
