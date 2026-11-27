# Unregistered 125: readiness register

Source: Q1-C `17e980912d636b0572a3f25b24995ffde31c754c`, catalog blob `95ddc0bf15d381e6694dd49ec774f4d04650e48c`. Classification is an audit recommendation, not a catalog status change or owner approval. Full source fields and Korean working objectives are in `quest_inventory.csv`.

## Counts

| Class | Count | Meaning |
|---|---:|---|
| DESIGN_READY | 42 | Encoded objective is plausible; per-quest tests and owner content review still needed. |
| TECHNICAL_DEPENDENCY | 42 | NOW entry has a `pending` tracker or missing event evidence. |
| DESIGN_REVIEW | 6 | Objective, feasibility, or promotion needs owner decision. |
| VERIFY_REQUIRED | 15 | Existing VERIFY mechanics or survival path remains unresolved. |
| CONTENT_DEPENDENCY | 20 | FUTURE feature and survival path remain gated. |

The 88 unregistered NOW entries remain separate from 17 VERIFY and 20 FUTURE entries. None is recommended for automatic registration.

## Per-quest disposition

| ID | Status | Tier | Tracker | Class | Exact reason | Next action |
|---|---|---|---|---|---|---|
| `xpq.exp.walk_10k` | NOW | EARLY | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.explorer_maps` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.exp.biomes_25` | NOW | MID | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.rare_biomes` | NOW | MID | knowledge_seen | DESIGN_REVIEW | Unregistered NOW; Objective/tier/reward or factual attainability needs owner review | Get per-quest test and owner content review before registration |
| `xpq.exp.height_extremes` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.exp.sleeping_bag_biomes` | VERIFY | MID | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.exp.parachute_landing` | VERIFY | MID | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.exp.walk_50k` | NOW | LATE | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.biomes_40` | NOW | LATE | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.sherds_15` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.exp.sail_20k` | NOW | LATE | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.ride_10k` | NOW | LATE | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.elytra_25k` | NOW | LATE | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.exp.travel_250k` | NOW | ENDGAME | stat_custom | DESIGN_REVIEW | Unregistered NOW; Objective/tier/reward or factual attainability needs owner review | Get per-quest test and owner content review before registration |
| `xpq.exp.biomes_all_overworld` | VERIFY | ENDGAME | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.dim.blaze_rods_32` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.dim.shulker_boxes` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cmb.wither_skulls_3` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.boss.monument_clear` | VERIFY | LATE | distinct_set | DESIGN_REVIEW | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.boss.monuments_3` | VERIFY | ENDGAME | distinct_set | DESIGN_REVIEW | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.min.obsidian_64` | NOW | EARLY_MID | stat_mined | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.min.stone_10000` | NOW | MID | stat_mined | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.min.amethyst_spyglass` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.min.adamantium_debris` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.min.all_wood` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.min.vein_500` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.min.lumberjack_200` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.frm.harvest_all_crops` | NOW | EARLY | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.composite_foods` | NOW | EARLY | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.frm.harvest_1000` | NOW | MID | counter | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.frm.breed_200` | NOW | MID | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.frm.honey_32` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.master_trade` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.mending_book` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.ancient_harvest` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.professions_10` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.frm.villager_discount_20` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.frm.harvest_5000` | NOW | LATE | counter | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.frm.trades_200` | NOW | LATE | stat_custom | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.frm.soil_study_harvest` | VERIFY | LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.fsh.freshwater_4` | NOW | EARLY | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.fsh.first_module` | NOW | EARLY | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.fsh.grade_four` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.fsh.tier3_rig` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.fsh.water_biomes_8` | NOW | MID | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.fsh.multi_catch` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.fsh.grade_five` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.fsh.ocean_trio` | NOW | MID_LATE | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.fsh.catch_500` | NOW | LATE | counter | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.fsh.full_catalogue` | NOW | LATE | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.fsh.giant_catch` | VERIFY | LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.fsh.grade_five_5` | NOW | ENDGAME | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.sleeping_bag` | NOW | EARLY | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.cft.enchanting_30` | NOW | EARLY_MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.smelt_1000` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.all_daggers` | NOW | MID | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.cft.parachute` | NOW | MID | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.cft.conduit` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.recipes_200` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.mm_metallurgy` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.all_greatswords` | NOW | LATE | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.cft.netherite_tools` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.cft.mm_blocks` | NOW | ENDGAME | stat_crafted_distinct | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.bld.storage_50` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.bld.copper_ages` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.alc.shock_potion` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.alc.types_12` | NOW | MID | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.alc.splash_lingering` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.alc.ingredients_all` | NOW | MID | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.alc.effects_16` | NOW | MID_LATE | knowledge_seen | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.alc.enhanced_3` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.alc.shock_10` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.eqp.enhance_5` | NOW | EARLY | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.custom_enchant` | NOW | EARLY_MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.enhance_10` | NOW | MID | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.enhance_trio` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.enhance_15` | NOW | MID_LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.custom_enchant_5` | VERIFY | LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.eqp.enhance_20` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.first_t2_core` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.t2_cores_3` | NOW | LATE | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.enhance_20_armor` | NOW | ENDGAME | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.eqp.custom_enchant_all` | VERIFY | ENDGAME | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.eqp.all_t2_cores` | NOW | ENDGAME | pending | TECHNICAL_DEPENDENCY | Unregistered NOW; NOW but tracker is pending; objective cannot register as encoded | Specify and test the exact server evidence hook before registration |
| `xpq.grw.knowledge_10` | NOW | EARLY | knowledge_count | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.first_research` | NOW | EARLY | research_nodes | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.seven_roots` | NOW | EARLY_MID | research_roots | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.knowledge_20` | NOW | EARLY_MID | knowledge_count | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.research_10` | NOW | MID | research_nodes | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.knowledge_35` | NOW | MID | knowledge_count | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.first_t2` | NOW | MID | research_tier | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.discipline_breadth` | NOW | MID_LATE | research_breadth | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.spent_10k` | NOW | MID_LATE | research_value | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.knowledge_50` | NOW | LATE | knowledge_count | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.research_25` | NOW | LATE | research_nodes | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.first_t3` | NOW | LATE | research_tier | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.spent_50k` | NOW | LATE | research_value | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.knowledge_complete` | NOW | ENDGAME | knowledge_count | DESIGN_REVIEW | Unregistered NOW; Objective/tier/reward or factual attainability needs owner review | Get per-quest test and owner content review before registration |
| `xpq.grw.research_50` | NOW | ENDGAME | research_nodes | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.t3_5` | NOW | ENDGAME | research_tier | DESIGN_READY | Unregistered NOW; Design specified; per-quest automated and live validation absent | Get per-quest test and owner content review before registration |
| `xpq.grw.discipline_complete` | NOW | ENDGAME | research_discipline_complete | DESIGN_REVIEW | Unregistered NOW; Objective/tier/reward or factual attainability needs owner review | Get per-quest test and owner content review before registration |
| `xpq.grw.climbing_100` | VERIFY | MID | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.grw.wall_jump_50` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.grw.fall_roll_20` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.grw.throwable_pickaxe` | VERIFY | MID_LATE | pending | VERIFY_REQUIRED | VERIFY gate remains; survival mechanics or detection needs evidence | Resolve specific factual/mechanics uncertainty and owner registration gate |
| `xpq.eqp.first_t2_weapon` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.t2_arsenal` | FUTURE | ENDGAME | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.t2_plus20` | FUTURE | ENDGAME | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cmb.elemental_100` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cmb.elemental_skills` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.accessory_first` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.accessory_full` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.accessory_all` | FUTURE | ENDGAME | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.frm.first_capture` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.frm.capture_10` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.frm.auto_farm` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.fsh.fish_trap` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.min.drill_1000` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cft.backpack_large` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cft.portable_furnace` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.alc.injector` | FUTURE | MID_LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cmb.gauntlet_50` | FUTURE | MID_LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.cmb.tactical_ammo` | FUTURE | MID | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.t3_special` | FUTURE | LATE | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
| `xpq.eqp.t3_set` | FUTURE | ENDGAME | pending | CONTENT_DEPENDENCY | FUTURE survival path or feature gate not approved for activation | Keep FUTURE until feature and owner gate are confirmed |
