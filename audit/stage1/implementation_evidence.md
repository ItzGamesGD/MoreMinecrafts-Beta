# Research & Knowledge runtime audit — PR #76 HEAD

## Executive summary

The current integrated branch contains 70 Knowledge nodes, 70 V3 Research nodes and 17 V4 Research nodes: 157 distinct IDs, all with matching layout entries and Korean title keys. The branch does not contain 157 completed gameplay behaviors. Four V4 effects have a purchased-state consumer but lack direct effect verification; 8 V4 entries expose effect metadata without a convincing consumer; 144 nodes are partial; one Knowledge entry is blocked by an unresolved taxonomy. No entry meets the strict A criterion on available evidence. These are source-audit judgments, not live-client results.

| Population | Count | A | B | C | D | E | F |
|---|---:|---:|---:|---:|---:|---:|---:|
| Knowledge | 70 | 0 | 0 | 69 | 0 | 0 | 1 |
| Research V3 | 70 | 0 | 0 | 70 | 0 | 0 | 0 |
| Research V4 | 17 | 0 | 4 | 5 | 8 | 0 | 0 |
| Total | 157 | 0 | 4 | 144 | 8 | 0 | 1 |

C for V3 describes partial *infrastructure and cost data*, not purchasable or working effects. D for V4 describes a purchasable definition and effect lookup metadata with no gameplay consumer found. The full row-specific rationale, source line, conditions, costs, status flags and uncertainty appear in `node_matrix.json` and `node_matrix.csv`.

## Source authority and ancestry

Remote PR #75 remains open at `d1f113720cf7d71720450d4709b3834b137d43f4`. Remote PR #76 remains open at `5dad54adc65d80d755156a5248120d2618d4f9ac`; local HEAD matches #76. Its merge base with #75 is exactly #75 HEAD, and #76 contains 23 later commits. PR #75 targets `stage-20/checkpoint` (`20121ca7cceae03602038de31076bd9ace115208`). No unmerged sibling branch code was counted as runtime. `docs/CONTENT_AUTHORITY_INDEX.md` places latest Research and Knowledge authority in `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, including the V4 amendment at lines 909–942. Historical proposals and old UI interaction text yield to later accepted decisions. V3 tiers are development categories, not compulsory purchase edges. Research V4 Knowledge prerequisites use ANY-ONE semantics (`ResearchPurchaseRules.java:27-32`).

## Catalog and shared execution path

`KnowledgeCatalog.java:34-103` defines all 70 Knowledge IDs. `ResearchCatalog.java:17-33` defines 17 V4 entries and loads 70 V3 entries from `research_v3.json` at lines 75–102. V3 ledger self-check requires 70 definitions, 178 levels and 130,360 raw XP; V4 has 17 levels and 16,910 raw XP. All seven discipline IDs are `resources`, `production`, `living`, `combat`, `ecology`, `world`, `alchemy`. `ProgressionNodeLayoutCatalog.java:82-128` validates layout coverage, and this audit independently matched all 157 IDs. All Korean title keys were found. The 157 layout branches and icons are presentation metadata, not prerequisite or effect evidence.

`KnowledgeRuntime.java:25-29,190-238` stores per-player counters in a persistent attachment, copies on death, increments revision, sends deltas, and unlocks Knowledge after a condition reaches its target. `ProgressionSyncRuntime.java:14-48` handles join and other sync points. These shared mechanics do not establish that each condition has a collector or each reward is applied. `ResearchPurchaseRules.java:23-34` blocks undefined V3 prerequisites. `ResearchPurchaseService.java` performs server-side validation and cost deduction for eligible definitions; purchase tests demonstrate the transaction, not its effect.

## Knowledge by discipline

| Discipline | IDs | Findings |
|---|---|---|
| resources | 1–10 | Ore, item and durability collectors exist. #5 counts all acquired logs as large-tree logs. #8 counts base-stone tag members; reaching eight intended rock types is unproven. Numeric XP, sapling and durability rewards lack consumers. |
| production | 11–19 | #11 has no ore-smelting collector. #12 unlocks on crafting **or** using a blast furnace though design says both. #14 counts crafted tools, not types. #16 anvil repair has a GameTest for progress. Recipe/info rewards and smelting XP bonuses lack demonstrated application. |
| living | 20–30 | Crop, tilling, fishing and breeding adapters exist. #22/#23/#25 observe attempted interactions before success. #30 counts food crafting completions, not ten distinct crafted/cooked types. Recipe/info and fishing XP rewards remain unproved. |
| combat | 31–40 | Kill adapters exist but #34/#35/#38 inspect held item at death rather than projectile source. #36 shield-block collector and #40 special ammunition collector are absent. No Knowledge bonus or information consumer found. |
| ecology | 41–52 | Kill adapters cover most species. #48 piglin barters have no collector. #51 rare species list is unresolved and encounter collector absent. Approved +5% species/family damage rewards have no consumer, and taxonomy overlap is pending. |
| world | 53–62 | Biome, dimension, travel, structure and trade polling exists. #59 archaeology brush loot has no collector. #57 parachute recipe data exists, with crafting gate scope needing runtime validation. Structure visits #60/#62 share a counter with target thresholds 5/10. |
| alchemy | 63–70 | #67 effect-type polling and #68 ingredient observation exist. No collector found for #63–66 or #69–70 brewing conditions. Brewing XP and information rewards lack consumers. |

The approved Knowledge design at `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md:157-238` includes information rewards, small numerical rewards and ecology +5% damage. Information rewards need a real discoverable information surface; they do not require an invented numeric buff. `KnowledgeCatalog.rewardKey` is passed to screen data (`ProgressionScreenData.java:29`), but no gameplay/info reward consumer was found. Thirteen approved alternative recipe IDs have JSON and a `CraftingMenu` gate (`ProgressionRecipeLocks.java:11-31`, `KnowledgeCraftingMenuMixin.java:14-22`, `KnowledgeCraftingRecipeTest.java`); 2×2 inventory crafting and other output paths are not directly verified. Vanilla recipes must stay usable.

## Research by catalog and discipline

All 70 V3 rows have `prerequisitesDefined=false` and empty prerequisite sets in `research_v3.json`. The purchase validator returns `MISSING_PREREQUISITE`. No node-specific V3 purchased-state gameplay consumer was found. Existing stand-alone features with similar names cannot be counted as V3 Research effects. Approved prices remain unchanged; precise prerequisite topology remains a design blocker.

V4's four B rows are `combat_root`, `climbing`, `wall_jump`, and `fall_roll`. `LivingEntityMixin.java:59-65` checks the purchased combat root to alter hostile damage. `ResearchMovementRuntime.java:31-105` checks purchased movement Research during climbing, jump and fall handling. None has a direct gameplay-effect GameTest or client validation in this audit. Five C rows (`throwable_pickaxe`, `nearby_chest_ingredients`, `hopper_filter`, `mob_capture`, `piglin_barter`) have potentially related independent systems or items but no demonstrated purchased-state integration. Eight D rows (`ecology_root`, `automatic_tool_switch`, `shield_counter`, `ballistic_control`, `bow_charge_mobility`, `falling_axe_smash`, `weapon_swap_handling`, `mob_aggro_redirection`) have definitions and effect metadata only on found paths. `ResearchRewards.java:15-31` is a lookup table, not effect application; its only external gameplay use is the combat-root call above. `mob_capture` requires Knowledge #51, which currently has no normal acquisition path.

| Discipline | Knowledge | V3 Research | V4 Research | Total |
|---|---:|---:|---:|---:|
| resources | 10 | 5 | 2 | 17 |
| production | 9 | 8 | 3 | 20 |
| living | 11 | 13 | 0 | 24 |
| combat | 10 | 9 | 6 | 25 |
| ecology | 12 | 13 | 3 | 28 |
| world | 10 | 10 | 3 | 23 |
| alchemy | 8 | 12 | 0 | 20 |
| **Total** | **70** | **70** | **17** | **157** |

## Verification scope and limitations

Existing catalog, ledger, recipe JSON, purchase-policy and GameTests establish selected data or transaction paths. Anvil repairs have a direct progress GameTest; purchase GameTests prove purchase and state persistence. No current test proves 157 individual effects. This read-only audit ran no build, client, or new test and did not trigger CI. `verification_status=UNIT` on V3 refers to catalog/ledger checks only. Where a collector is present, ordinary-survival reachability is marked likely or uncertain based on source semantics; it is not claimed as live-client validated.
