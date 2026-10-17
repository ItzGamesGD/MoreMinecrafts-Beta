# Q1-B independent per-quest audit (work in progress)

Authority: catalog A and XP Quest handoff. All 39 reward values were compared against the approved catalog; no discrepancy found. The isolated threshold tests use injected persisted evidence and verify target − 1 versus target. They do **not** prove that a live gameplay event produces that evidence. Registrations remain under review until source-specific gates pass.

| Quest | Target | Raw XP | Tracker evidence | Retroactivity | Focused threshold | Live source evidence |
|---|---:|---:|---|---|---|---|
| `xpq.exp.archaeology_30` | 30 | 500 | KnowledgeRuntime.recordCondition: archaeology_items_brushed | capped Knowledge seed (lower bound) | `threshold_exp_archaeology_30` | K59 archaeology success/empty-loot GameTests |
| `xpq.dim.nether_travel_50k` | 50000 | 2500 | Player.awardStat distance while in Nether | none | `threshold_dim_nether_travel_50k` | Nether movement test |
| `xpq.cmb.shield_500` | 500 | 450 | KnowledgeRuntime.recordCondition: shield_blocks | capped Knowledge seed (lower bound) | `threshold_cmb_shield_500` | K36 actual shield-block GameTest |
| `xpq.cmb.trident_50` | 50 | 450 | KnowledgeRuntime.recordCondition: trident_hostile_kills | capped Knowledge seed (lower bound) | `threshold_cmb_trident_50` | stored trident owner/negative GameTest |
| `xpq.min.first_custom_ore` | 1 | 250 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_first_custom_ore` | ore feature/break/piston tests |
| `xpq.min.custom_ore_survey` | 4 | 350 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_custom_ore_survey` | ore feature/break/piston tests |
| `xpq.min.ore_1000` | 1000 | 500 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_ore_1000` | ore feature/break/piston tests |
| `xpq.min.deepslate_custom` | 4 | 500 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_deepslate_custom` | ore feature/break/piston tests |
| `xpq.min.logs_1000` | 1000 | 500 | KnowledgeRuntime.recordCondition: natural_vanilla_logs_mined (K5 tree marker) | capped Knowledge seed (lower bound) | `threshold_min_logs_1000` | LIVE SOURCE TEST PENDING |
| `xpq.min.pickaxe_5000` | 5000 | 700 | KnowledgeRuntime.recordCondition: pickaxe_durability_used | capped Knowledge seed (lower bound) | `threshold_min_pickaxe_5000` | K10 nested/cancelled durability GameTests |
| `xpq.min.ore_5000` | 5000 | 1100 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_ore_5000` | ore feature/break/piston tests |
| `xpq.min.diamond_ore_64` | 64 | 1100 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_diamond_ore_64` | ore feature/break/piston tests |
| `xpq.min.adamantium_10` | 10 | 1250 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_adamantium_10` | ore feature/break/piston tests |
| `xpq.min.debris_16` | 16 | 1100 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_debris_16` | ore feature/break/piston tests |
| `xpq.min.ore_20000` | 20000 | 3150 | BEFORE+AFTER block break, positive chunk ore marker | none | `threshold_min_ore_20000` | ore feature/break/piston tests |
| `xpq.frm.composite_foods` | 5 | 200 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_frm_composite_foods` | LIVE SOURCE TEST PENDING |
| `xpq.frm.tame_5` | 5 | 350 | TAME_ANIMAL trigger for actual tamer | none: species history unavailable | `threshold_frm_tame_5` | five-species tame test |
| `xpq.frm.harvest_1000` | 1000 | 500 | KnowledgeRuntime.recordCondition: crops_harvested | capped Knowledge seed (lower bound) | `threshold_frm_harvest_1000` | LIVE SOURCE TEST PENDING |
| `xpq.frm.till_1000` | 1000 | 400 | KnowledgeRuntime.recordCondition: farmland_tilled | capped Knowledge seed (lower bound) | `threshold_frm_till_1000` | farmland hoe success/refusal GameTest |
| `xpq.frm.harvest_5000` | 5000 | 1100 | KnowledgeRuntime.recordCondition: crops_harvested | capped Knowledge seed (lower bound) | `threshold_frm_harvest_5000` | LIVE SOURCE TEST PENDING |
| `xpq.frm.barter_200` | 200 | 1100 | KnowledgeRuntime.recordCondition: piglin_barters | capped Knowledge seed (lower bound) | `threshold_frm_barter_200` | K48 direct offerer/no-offer GameTests |
| `xpq.fsh.first_sale` | 1 | 250 | accepted Fisherman fish sale | none | `threshold_fsh_first_sale` | accepted/refused fish-sale test |
| `xpq.fsh.sale_64` | 64 | 500 | accepted Fisherman fish sale | none | `threshold_fsh_sale_64` | accepted/refused fish-sale test |
| `xpq.fsh.catch_500` | 500 | 1100 | KnowledgeRuntime.recordCondition: fishing_successes | capped Knowledge seed (lower bound) | `threshold_fsh_catch_500` | LIVE SOURCE TEST PENDING |
| `xpq.fsh.emerald_500` | 500 | 1100 | accepted Fisherman fish sale | none | `threshold_fsh_emerald_500` | accepted/refused fish-sale test |
| `xpq.cft.sleeping_bag` | 1 | 150 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_cft_sleeping_bag` | LIVE SOURCE TEST PENDING |
| `xpq.cft.all_daggers` | 7 | 500 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_cft_all_daggers` | LIVE SOURCE TEST PENDING |
| `xpq.cft.anvil_50` | 50 | 450 | KnowledgeRuntime.recordCondition: anvil_repairs | capped Knowledge seed (lower bound) | `threshold_cft_anvil_50` | T1 actual anvil repair GameTest |
| `xpq.cft.redstone_100` | 100 | 450 | KnowledgeRuntime.recordCondition: redstone_components_placed | capped Knowledge seed (lower bound) | `threshold_cft_redstone_100` | redstone placement success/failure GameTest |
| `xpq.cft.parachute` | 1 | 400 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_cft_parachute` | LIVE SOURCE TEST PENDING |
| `xpq.cft.all_greatswords` | 7 | 1100 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_cft_all_greatswords` | LIVE SOURCE TEST PENDING |
| `xpq.cft.mm_blocks` | 5 | 3150 | player ITEM_CRAFTED statistic | saved ITEM_CRAFTED stats | `threshold_cft_mm_blocks` | LIVE SOURCE TEST PENDING |
| `xpq.bld.place_1000` | 1000 | 250 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_place_1000` | placement path test |
| `xpq.bld.variety_50` | 50 | 350 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_variety_50` | placement path test |
| `xpq.bld.place_10000` | 10000 | 500 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_place_10000` | placement path test |
| `xpq.bld.variety_150` | 150 | 1100 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_variety_150` | placement path test |
| `xpq.bld.place_50000` | 50000 | 1100 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_place_50000` | placement path test |
| `xpq.bld.place_200000` | 200000 | 3150 | BlockItem.place success / one-time ITEM_USED baseline | ITEM_USED one-time seed (lower bound) | `threshold_bld_place_200000` | placement path test |
| `xpq.alc.potions_250` | 250 | 1100 | KnowledgeRuntime.recordCondition: potions_brewed | capped Knowledge seed (lower bound) | `threshold_alc_potions_250` | brewing output and two-player GameTests |

Common persistence: player attachment, copied on death; monotonic progress and claim-once follow `QuestRules`/`QuestProgressService`. Distinct sets deduplicate by saved key. Shared GameTests cover save/load and player isolation, but per-source live acceptance is still pending where marked. No Q1-C registration is authorized by this table.
