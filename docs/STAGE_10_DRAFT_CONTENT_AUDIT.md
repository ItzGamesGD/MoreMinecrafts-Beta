# Stage 10 — Draft Content Completion / Design Audit

> Work audit at main `771391a73fbc0ff1743ad0e978ed1626265943b1` (Stage 9 PR #23 merged). Integration checkpoint: `stage-10/draft-content`. This is a specification and documentation audit, not authorization to implement all entries.

## 1. Executive summary

Stage 10 replaces the obsolete mandatory World / Structure / Exploration stage. Standalone Exploration progression is retired. The Fabric tree already contains materials/worldgen (Stage 6), fishing (Stage 7), three Stage 8 zombie variants, Skeleton King (Stage 9), Enhancement, Flame Sword and Wind Sword. LIVE PENDING means human validation remains; it does not mean missing implementation. The largest unrecovered design sources are the full Coating effect matrix and the historical Utility Equipment specification packaged in the Paper content-freeze ZIP. Do not fill these gaps by invention.

## 2. Method and precedence

Applied in order: (1) latest decisions in the Stage 10 request; (2) finalization records; (3) Fabric main for actual implementation only; (4) stage implementation records; (5) older checklist; (6) Paper as behavior/reference. Inspected all eight requested Fabric documents, the restored Stage 1-2 record, the recursive Fabric source/resource tree and Paper Alchemy YAML/resolvers/brewing listener. The ZIP-only Freeze v3 was located at `docs/mod-handoff/HyunseoRPG_Content_Freeze_Stage8_Mod_Handoff_2026-09-21_v3.zip` in the Paper repository, but its matrix/utility contents could not be verified through the available UTF-8 repository interface. Absence from the Fabric tree is an implementation-status observation, not a change to design intent.

Status: IMPLEMENTED, LOCKED_NOT_IMPLEMENTED, DRAFT_NEEDS_DECISION, DEFERRED, RETIRED, STALE_DOC. Readiness: ALREADY_IMPLEMENTED, READY_FOR_VERTICAL_SLICE, NEEDS_DESIGN_DECISION, DEFERRED, RETIRED. Counts below refer only to the consolidated table's discrete rows; stale-document findings are separately listed.

## 3. Coating audit

**LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION.** Functional armor-only progression, separate from Enhancement and vanilla Enchantment. Minecraft-native smithing-template-style application uses new dedicated coating items and recipes, with existing project minerals as materials. One shared placeholder visual pattern is permitted. No weapon/tool coating is designed. Each armor ItemStack progresses 0 → 1 → 2 → 3 coatings and then cannot accept another. No removal or replacement now; eventual replacement requires a separately specified removal item/mechanic. This future intent is DEFERRED, not current behavior. No coating implementation is present in Fabric's registries, components or resources.

The older checklist names Copper, Quartz, Amethyst, Lapis, Emerald, Gold, Diamond, Netherite, Iron, Redstone, Titanium, Tungsten and Mythril as a matrix; excludes Aquamarine, Adamantium and Etherium; and records Iron Leggings knockback resistance +30% with ranged damage -15%, and Gold Boots ignoring block slowing plus a lava-movement effect. Those are the only exact effects recoverable from accessible text. The Gold lava effect's precise rule and every other matrix cell remain **UNVERIFIED** pending authoritative Freeze v3 extraction. Do not mistake old template economy questions for a decision to use existing vanilla templates: dedicated coating items/recipes are now locked. Acquisition, consumption, complete per-slot effect matrix, stacking/conflict rules, effect predicates and balance need source recovery or explicit decision. Boundary for a later slice: one verified armor coating end to end, plus persistence/cap validation; only after its effect definition is recovered and approved. No cosmetic roster or removal item belongs in that slice by default.

## 4. Utility Equipment audit

All ten entries below are retained independent vanilla-expansion utility content, with **no Fabric implementation found**. No standalone Exploration levels, structures or currency may be required. The older checklist contains extra Climbing Gloves, Potion Injector and Buoyancy equipment; those are **STALE_DOC** relative to the latest roster, not automatically retired project-wide. It marked Grappling Hook optional; the latest roster retains it while its mechanics remain deferred.

| Item | Status / readiness | Recovered behavior | Gaps and dependencies | Suggested slice boundary |
|---|---|---|---|---|
| Miner Lantern | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | Light 15; hand or helmet integration | recipe, light implementation and retention behavior; existing material recipe | one light item and native equip interaction |
| Sleeping Bag | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | skips night; does not reset spawn | activation/multiplayer and recipe | one sleep interaction |
| Oxygen Tank | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | custom oxygen consumed before vanilla air | capacity, recharge, equip, recipe | underwater air reserve |
| Jet Boots | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | stacked air jumps, ground/slime reset, no generic fuel | jump count, equip and recipe | one controlled jump tier |
| Flare | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | charged moving-light projectile | duration, materials, projectile rules | one projectile |
| Return Crystal | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | expensive one-use channel cancelled by movement/hit | destination, channel duration, recipe | one safe channel/arrival |
| Portable Furnace | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | food-only, smoker-like, on-demand fuel | inventory persistence, fuel efficiency, recipe | one food processing interaction |
| Mining Drill | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | endgame, coal-block fuel; Fortune/Silk/Unbreaking, no Efficiency | speed, fuel consumption, 128-block capacity, recipe | one mining item |
| Glider | LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION | retained; older checklist specifies no final physics | gravity/speed/steering, durability and recipe | flight behavior after design signoff |
| Grappling Hook | DEFERRED / DEFERRED | retained roster, previously optional/on hold | entire interaction/physics and recipe | separate future design and slice |

Older checklist behaviors are evidence, not license to decide missing mechanics. For every utility slice remove any old dependence on Exploration event engine, mandatory structures, Coin economy or job levels. Exact recipes and balance await individual slices.

## 5. Alchemy historical recovery and Stage 10 brewing specification

**Fabric status: Shock base potion IMPLEMENTED / LIVE PENDING; the other seven potions and all catalysts remain LOCKED_NOT_IMPLEMENTED.** The bounded Stage 10A implementation uses the Minecraft 26.3 data-driven `minecraft:brewing` recipe for Thick Potion + Copper Ingot, native registered potion/effect identities, and the exact recovered Shock contract. See `STAGE_10A_SHOCK_POTION.md` for implementation and validation detail. The full eight-potion/catalyst rollout still requires slice-specific validation and balance. Paper implementation is evidence of retained design, never Fabric implementation. The prior assessment that an empty final recipe file implied no recoverable Alchemy design was incorrect.

### 5.1 Timeline and source trail

| Period | Evidence | Intent and interpretation |
|---|---|---|
| Early/middle Paper, import `6903c540` (August 17) | `alchemy/recipes.yml` v1, `effects.yml`, `potions.yml`, `items.yml`, registry/factory and U0-U11 reports | Eight named production potion identities, effects and multi-material ingredient lists. Farming-derived materials and custom crafting path existed. The test-speed potion was disabled. The U11 migration-gap report describes earlier missing runtime wiring; later commits repaired it. |
| Runtime recovery, August 18-20 | `29ad0ad`, `d4a6b796`, `8021e2d7`, `dc407bd8`, `36d0d5a`, `490dd0b4`, `builds/2026-08-19_092_alchemy-live-contracts.md`, `builds/2026-08-20_093_alchemy-effect-live-fixes.md` | Effect handlers and special catalyst delivery evolved. Shock timing was revised (the August 19 test note says 60 ticks; the later `effects.yml` and `ShockTimingPolicy` use **320 ticks**). Latest code/config wins for the recovered baseline; live server validation remained pending. |
| Simplification before September 21 | `docs/gameplay-subsystem-simplification-stage-6.md` at parent `9cb5b943` | Exploration removed; Farming/Cooking still present in that snapshot. Alchemy backend explicitly retained. |
| Farming/Cooking removal and stand integration, `0db90dba` (September 21; parent `9cb5b943`) | Commit diff changes `AlchemyRecipeDefinition` from list to `baseInputId + ingredientId + resultPotionId`, adds `BrewingInputResolver` / `BrewingTransactionPlanner` / `BrewingStandAlchemyListener`, removes Farming and old Alchemy GUI/bypass, replaces v1 recipes with v2 `recipes: {}` | Deliberately removed: Farming/Cooking and custom crafting frontend. Deliberately retained: Alchemy effect, potion, catalyst, recipe and potion-use backend, with vanilla stand as sole creation frontend. **Old recipe data was not remapped**, not proof of intentional potion retirement. |
| Final Paper `10cbdc99` and checked branches `backup/final-main-to-drive`, `codex/inspect-and-correct-pr-#51-d6r0fv` | Same empty v2 transition file, enabled eight potions/effects and ten catalysts; source still includes handlers | No authoritative final one-ingredient transition for any of the eight was found in the checked history/branches. The recorded design remains, but production brewing acquisition is incomplete. |

Commit path history for `alchemy/recipes.yml` consists of import `6903c540` and migration `0db90dba`; `YamlAlchemyRecipeRegistry.java` has the same two relevant commits. Effect/catalyst histories were followed through the August runtime fixes. See [pre-removal recipe](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/9cb5b943c9a0567c979b691b77998c7d349f3d4e/src/main/resources/alchemy/recipes.yml), [retention/migration commit](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/commit/0db90dba631a7cbd49adb7e7a58e1e0d63bbce91), [final effects](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/10cbdc99d069d910006b21111a83ac0c307e2d7f/src/main/resources/alchemy/effects.yml) and [final catalysts](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/10cbdc99d069d910006b21111a83ac0c307e2d7f/src/main/resources/alchemy/catalysts.yml).

### 5.2 RECOVERED_PAPER_DESIGN — special potion/effect roster

All eight production `potions.yml` definitions are enabled, default to DRINK and point to the corresponding `effect_*` entry. `effects.yml` declares 1,200 ticks, amplifier 0, max one stack, `LIVING_ENTITY`, `REFRESH_DURATION`, remove on death, no persistence through logout or world change; all numbers are Paper baselines marked BALANCE_PENDING, **not** new Fabric balance decisions. A new application refreshes the expiry rather than increasing stacks. The potion normally affects its consumer, while transformed splash/lingering versions affect living targets reached by delivery; do not infer that drinking an offensive potion automatically targets an enemy. `effect_test_speed` is development-only (disabled potion), not a ninth production potion.

| Potion / effect | Recovered handler contract and trigger | Key edge/dependency |
|---|---|---|
| `potion_vampire / effect_vampire` (흡혈) | On valid dealt damage, heals source by 5% of final dealt damage; caps at maximum health. | Requires attributed positive damage and a live source; `ProductionEffectListener` resolves projectile shooter. No heal on invalid/nonpositive damage. |
| `potion_berserk / effect_berserk` (광폭화) | Outgoing damage ×1.10; incoming damage ×1.05 through combat modification. | Both directions apply while active; combat event ordering and other modifiers matter. |
| `potion_corrosion / effect_corrosion` (부식) | Target's incoming damage ×1.10. | Distinct named effect from Vulnerability though both currently modify incoming damage. |
| `potion_frostbite / effect_frostbite` (동상) | Movement-speed attribute modifier −0.15 plus Slowness amplifier 0, refreshed while owned. | Do not overwrite/remove a stronger or unrelated existing Slowness; clean owned modifier/effect on removal. |
| `potion_shock / effect_shock` (감전) | First pulse after 320 ticks, then interval-based 1.0 damage pulse; each valid pulse roots movement for 20 ticks, capped below next pulse interval. | Free movement before/between pulses; look direction and actions remain usable. Invalid/dead target skips lock; remove/expiry clears it. Earlier 60-tick report was superseded by later YAML. |
| `potion_bleed / effect_bleed` (출혈) | Periodic 1.0 effect damage on 20-tick interval. | Effect source attribution and cleanup required. |
| `potion_vulnerability / effect_vulnerability` (취약) | Target's incoming damage ×1.20. | Fermented Spider Eye has a specific inversion override to `effect_corrosion`. |
| `potion_necrosis / effect_necrosis` (괴사) | MAX_HEALTH attribute modifier −0.10 while active. | Remove modifier on expiry/lifecycle cleanup and preserve safe health bounds. |

`EffectService`, handlers and `PaperPotionUseService` are the behavior sources. Exact interaction with other effects and PVP/boss policies beyond recorded definitions must be checked in each slice; do not invent a new effect. Only the eight enabled production identities are retained by this audit.

### 5.3 RECOVERED_PAPER_DESIGN — catalyst inventory

The ten enabled entries remain in final `catalysts.yml`. Ordinary catalysts transform a canonical potion with a single catalyst identity; the Paper transformation rejects disabled/invalid potion identity, duplicate catalyst and incompatible modes. A catalyst is a *later brewing transition*, separate from the ingredient that creates the base potion. The Paper stand contract consumes one ingredient per successful transition; the old non-consuming `CatalystApplicationService.transform` was a planning service, not evidence of free catalyst use. Allowed-potion restrictions default to unrestricted when no list is set. Precise Fabric cross-combination policy, where undefined, is deferred to its slice.

| Catalyst material (ID) | Recovered function | Restrictions / baseline |
|---|---|---|
| REDSTONE (`redstone`) | DURATION transformation | Config `duration-multiplier-percent: 100` (currently **no actual extension**, BALANCE_PENDING); do not claim a longer effect without a later balance decision. |
| GLOWSTONE_DUST (`glowstone_dust`) | AMPLIFIER +1 | Duration percent 100; effect amplifier cap in old runtime 10. |
| GUNPOWDER (`gunpowder`) | DELIVERY → SPLASH | Paper requires DRINK source; converts item/delivery metadata. |
| DRAGON_BREATH (`dragon_breath`) | DELIVERY → LINGERING | Rejects already lingering; exact permitted predecessor and native sequencing need slice validation. |
| FERMENTED_SPIDER_EYE (`fermented_spider_eye`) | INVERSION | Only `potion_vulnerability` explicitly allowed; effect override `effect_corrosion`. Other inversions unapproved. |
| SCULK_CATALYST (`sculk`) | SPECIAL splash propagation | Impact-origin cloud chain, up to 8, 10-tick propagation delay, search radius 8, max total distance 64, attenuation floor 0.5; cloud visual radius 1.25. Ordinary SCULK is not accepted. |
| ECHO_SHARD (`echo_shard`) | SPECIAL splash repeat | Applies to affected living targets and repeats on the same valid targets after 10 ticks; avoids source/duplicate scheduling. |
| SLIME_BALL (`slime`) | SPECIAL bouncing splash | Initial splash and bounded bounce projectiles; max propagation count 4, final next collision splash; long safety path forces final splash rather than silently dropping effect. |
| WIND_CHARGE (`wind_charge`) | SPECIAL directional splash | Starts at actual impact, direction from projectile velocity, travels to first eligible target; distance 16 and lifetime 40-tick bounds. |
| FIRE_CHARGE / custom `fireball` identity (`fireball`) | SPECIAL small fireball delivery | On impact applies potion effect in radius 4, no terrain damage or ignition. Item identity is important; source code distinguishes custom item ID from mere base material. |

Source: `catalysts.yml`, `CatalystApplicationService`, `BoundedSpecialCatalystExecutionService`, `PaperPotionUseListener`, `PaperPotionUseService`. Existing Paper runtime bounds are recovered baseline, not Fabric implementation. Sculk/Echo/Slime/Wind/Fireball are delivery mechanisms, not new potion effects.

### 5.4 RECOVERED_HISTORICAL_RECIPE and ingredient inventory

All eight enabled v1 `recipes.yml` entries are **HISTORICAL_REFERENCE**; a ninth test-speed entry was disabled. Each listed a set/list of several simultaneous materials, not a final native stand transition. No quantities were specified there. The `abundance_essence` farming exchange was disabled pending bridge mapping; `corrosion_essence` existed as an item but its exchange/crafting route was missing. Garlic/chili processed products depended on the now-retired Farming/Cooking subsystem. They retain thematic evidence but are **not prerequisites in the Stage 10 mappings**. `items.yml` identified custom items by canonical ID, even where they shared a vanilla display material. Pre-migration `YamlAlchemyRecipeRegistry` accepted `vanilla:<MATERIAL>` or recognized custom item IDs. The modern resolver gives canonical custom ID precedence, then enabled catalyst identity, then vanilla material; it never silently treats a custom ingredient as an ordinary material solely because of its display item.

| Result | Historical multi-material ingredients (quantities unspecified) | Disposition |
|---|---|---|
| Vampire | `abundance_essence`, SPIDER_EYE, `processed_garlic_concentrate_normal` | HISTORICAL_REFERENCE; farming inputs retired |
| Berserk | `abundance_essence`, BLAZE_POWDER, `processed_chili_extract_normal`, SUGAR | HISTORICAL_REFERENCE; farming inputs retired |
| Corrosion | `corrosion_essence`, SLIME_BALL, ROTTEN_FLESH | HISTORICAL_REFERENCE; essence acquisition unresolved/retired dependency |
| Frostbite | `corrosion_essence`, SNOWBALL, ICE | HISTORICAL_REFERENCE |
| Shock | `corrosion_essence`, COPPER_INGOT, REDSTONE | HISTORICAL_REFERENCE; Redstone also catalyst, kept conceptually separate |
| Bleed | `corrosion_essence`, CACTUS, `processed_chili_extract_normal` | HISTORICAL_REFERENCE |
| Vulnerability | `corrosion_essence`, ROTTEN_FLESH, SPIDER_EYE | HISTORICAL_REFERENCE |
| Necrosis | `corrosion_essence`, WITHER_ROSE, SPIDER_EYE | HISTORICAL_REFERENCE |

Distinct historical ordinary ingredient roster: custom `abundance_essence`, `corrosion_essence`, `processed_garlic_concentrate_normal`, `processed_chili_extract_normal`; vanilla SPIDER_EYE, BLAZE_POWDER, SUGAR, SLIME_BALL, ROTTEN_FLESH, SNOWBALL, ICE, COPPER_INGOT, REDSTONE, CACTUS, WITHER_ROSE. Catalyst materials are inventoried separately above. The old multi-material recipe shape and farming-derived acquisition are **RETIRED_ARCHITECTURE**, while the eight potion identities and effects survived.

### 5.5 RECOVERED_FINAL_BREWING_MAPPING versus NEW_STAGE_10_BREWING_MAPPING

**RECOVERED_FINAL_BREWING_MAPPING: none.** The September 21 migration added an explicit `base-input-id + ingredient-id → result-potion-id` schema but left `recipes: {}`. The checked main and remaining branches do not supply a complete transition table. A final Paper production route for these eight therefore was **not completed in inspected history**, while their effect and catalyst design was preserved.

Under the user's explicit exception, Work finalizes the following **NEW_STAGE_10_BREWING_MAPPING** as a proposed current Stage 10 design, not a claim about Paper history. Each row is exactly one Brewing Stand transition. `Thick Potion` is a vanilla brewing input used here as one common special-potion base; it is obtained by vanilla brewing before these transitions, with no new intermediate item. This deliberately avoids overwriting vanilla Awkward + Spider Eye (Poison) and Awkward + Blaze Powder (Strength), keeps unique base/ingredient pairs, and needs a Fabric 26.3 collision check before Codex registers recipes. Each mapping retains an existing vanilla material from its historical identity. The removed Farming essence/processed items are excluded because their production path was retired. Ingredient count per stand operation is one; standard Brewing Stand fuel and up-to-three bottle slots remain vanilla.

| Base bottle | One ingredient | Result | Label / rationale |
|---|---|---|---|
| Thick Potion | SPIDER_EYE | `potion_vampire` | NEW_STAGE_10_BREWING_MAPPING; retained historical eye, removed garlic/farming essence |
| Thick Potion | BLAZE_POWDER | `potion_berserk` | NEW_STAGE_10_BREWING_MAPPING; retained blaze, removed chili/farming essence; SUGAR not required |
| Thick Potion | SLIME_BALL | `potion_corrosion` | NEW_STAGE_10_BREWING_MAPPING; retained slime as ordinary ingredient in this **specific** recipe, while slime catalyst applies only to an existing finished potion; ingredient/catalyst role determined by base identity |
| Thick Potion | SNOWBALL | `potion_frostbite` | NEW_STAGE_10_BREWING_MAPPING; retained cold material; ICE not required |
| Thick Potion | COPPER_INGOT | `potion_shock` | NEW_STAGE_10_BREWING_MAPPING; retained conductor material; historical REDSTONE is a separate catalyst, not a mandatory second ingredient |
| Thick Potion | CACTUS | `potion_bleed` | NEW_STAGE_10_BREWING_MAPPING; retained cactus; farming chili removed |
| Thick Potion | ROTTEN_FLESH | `potion_vulnerability` | NEW_STAGE_10_BREWING_MAPPING; retained rot; SPIDER_EYE not required |
| Thick Potion | WITHER_ROSE | `potion_necrosis` | NEW_STAGE_10_BREWING_MAPPING; retained rare material; SPIDER_EYE not required |

A finished canonical potion + one eligible catalyst is a subsequent transition to the **same potion identity with modified duration/amplifier/delivery/special metadata**, not an ordinary `potion_* → different effect` recipe unless the recovered inversion explicitly overrides the effect. One catalyst maximum reflects recovered Paper duplication rejection; catalyst sequencing beyond one requires a separate decision. Slime Ball's dual role is contextual and must not cause the resolver to route Thick + Slime Ball to the catalyst transformation. Fabric must identify the *actual potion contents* of the bottle (Thick versus custom potion) and the ingredient's canonical identity; Paper's material-only fallback for vanilla potion bottles is insufficient for this specification.

### 5.6 Remaining unresolved and recommended Codex slice

| Item | Classification | Consequence |
|---|---|---|
| Full numerical balance of eight effects and catalysts, especially Redstone's 100% duration baseline | UNRECOVERED final balance | Retain recorded Paper provisional values or decide later; do not silently improve them. |
| Exact Fabric 26.3 native brewing conflict/precedence and Thick input recognition | Needs implementation-time validation | Codex must verify existing vanilla transitions remain unaffected; if a conflict exists, return to Work for a new mapping decision. |
| Special catalyst combinations, repeat use, source/target edge cases under multiplayer | Partially RECOVERED_PAPER_DESIGN | Each catalyst's later slice must specify acceptance and live checks; do not expand first potion slice. |
| Farming-derived essences and processed crop acquisition | RETIRED_ARCHITECTURE | Do not reintroduce farming; historical references stay documented. |
| Live Paper server behavior | UNRECOVERED validation | Automated build reports exist, but test reports mark actual server checks pending. |

**READY_FOR_VERTICAL_SLICE boundary: Shock base potion only.** Later Codex work may implement exactly one Thick Potion + Copper Ingot → `potion_shock` transition, canonical potion identity and consumption, 1,200-tick/320-tick pulse/1.0 damage/20-tick movement root recovered baseline, refresh-only stack behavior, living-target application through the appropriate potion delivery, lifecycle cleanup, no catalyst in this first slice, no custom crafting UI and no Farming dependencies. Test input matching, untouched vanilla brewing, up-to-three bottles, effect timing, persistence/cleanup, dedicated server and client. Work spec and user review precede Codex implementation. Other seven mappings are recorded but not a mandate to implement all eight together.

## 6. Equipment roster audit

`docs/EQUIPMENT_PROGRESSION_FINALIZATION.md` is the design authority. Tiers are categories, not a Promotion tree. T2: Flame Sword and Wind Sword **IMPLEMENTED / ALREADY_IMPLEMENTED**; Water Sword, Wind Archer Bow, Earth Mace and Dark Energy Staff **LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION** for individual abilities, recipe and input. T3 seven **LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION**: Thanatos Mace, Poseidon Spear, Flame Axe, Thunder God Axe, Holy Sword Solaris, Moonlit Afterglow, Frozen Sword. T4 baseline three **LOCKED_NOT_IMPLEMENTED / NEEDS_DESIGN_DECISION**: 무극검, 천궁, 천서. Optional World Tree Bow **DEFERRED / DEFERRED**. Enhancement foundation **IMPLEMENTED / ALREADY_IMPLEMENTED**. Exact stats/cooldowns/recipes/VFX/SFX can wait for each item. Roster lock does not make the entire roster implementation-ready or mandate Stage 10 implementation.

## 7. Secondary stale-document audit

- **Materials — IMPLEMENTED / ALREADY_IMPLEMENTED.** Five ores/materials, drops, storage, smelting, tags and worldgen are in Fabric Stage 6. `RESOURCE_MINING_MATERIALS_FINALIZATION.md` says Adamantium intermediate name awaits definition, whereas runtime names `adamantium_debris` and `adamantium_ingot`; it is a historical design snapshot. Stage 6 worldgen's runtime data is authoritative for current placement. LIVE PENDING is sampling/testing.
- **Fishing — IMPLEMENTED / ALREADY_IMPLEMENTED.** `STAGE_7_FISHING.md`, source and 15 equipment recipes specify a working vertical slice; older checklist's species/grades/window/module-role `[DECIDE]` entries are stale as implementation-status statements. There is also a **design/implementation discrepancy**: old checklist says left-click opens one-row UI, Stage 7 records shift-right-click. Do not silently rewrite intended control; seek explicit decision if changing implemented input. Fisherman exchange and final economic reward remain open.
- **Mobs — IMPLEMENTED / ALREADY_IMPLEMENTED** for Bomb Zombie, Leap Zombie and Mining Giant. Older checklist `[REVIEW]` and global roster `[DECIDE]` do not undo these three. Other Paper mob candidates remain separate design questions, not Stage 10 commitments.
- **Bosses — IMPLEMENTED / ALREADY_IMPLEMENTED** for Skeleton King, operator summon only. Old boss checklist omitted it and still describes only Thousand Eyes/Gateway prototypes. Survival encounter/progression is **not** implied by the test command.

## 8. Retired and out of scope

Standalone Exploration progression, Paper Exploration event engine, mandatory exploration structures, dungeon progression, exploration-specific economy, old Coin economy, job-level progression, Promotion, Fragment progression, giant custom crafting GUI and generic RPG world progression are **RETIRED / RETIRED**. Their presence in historical documents is not an instruction to implement them. Standalone World / Structure is not Stage 10.

## 9. Consolidated candidate table

| Content | Status | Readiness | Authority | Fabric evidence / unresolved dependency | Later slice |
|---|---|---|---|---|---|
| Coating (armor) | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | Stage 10 decisions + checklist; Freeze v3 matrix pending | no coating files; missing complete effect matrix | one verified coating |
| Miner Lantern | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | Stage 10 roster + checklist | no item; recipe/equip | one utility |
| Sleeping Bag | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; sleep rules | one utility |
| Oxygen Tank | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; capacity | one utility |
| Jet Boots | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; jumps | one utility |
| Flare | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; duration | one utility |
| Return Crystal | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; destination | one utility |
| Portable Furnace | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; fuel | one utility |
| Mining Drill | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; speed/fuel | one utility |
| Glider | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | Stage 10 roster + checklist | no item; physics | one utility |
| Grappling Hook | DEFERRED | DEFERRED | Stage 10 roster | no item; mechanics unresolved | later design |
| Alchemy (Shock base slice) | LOCKED_NOT_IMPLEMENTED | READY_FOR_VERTICAL_SLICE | Paper history + NEW_STAGE_10_BREWING_MAPPING | no Fabric files; Thick + Copper, recovered effect, no catalyst | one Shock potion |
| Flame Sword | IMPLEMENTED | ALREADY_IMPLEMENTED | equipment finalization + Fabric special/flame | in source | validation only |
| Wind Sword | IMPLEMENTED | ALREADY_IMPLEMENTED | same + Fabric special/wind | in source | validation only |
| Water Sword | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | equipment finalization | no item; ability/recipe | one weapon |
| Wind Archer Bow | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; ability/recipe | one weapon |
| Earth Mace | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; carrier adaptation | one weapon |
| Dark Energy Staff | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; carrier adaptation | one weapon |
| Thanatos Mace | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | equipment finalization + Paper concept | no item; detailed contract | one weapon |
| Poseidon Spear | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| Flame Axe | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| Thunder God Axe | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| Holy Sword Solaris | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| Moonlit Afterglow | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| Frozen Sword | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; detailed contract | one weapon |
| 무극검 | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | equipment finalization | no item; modes/recipe | one endgame item |
| 천궁 | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; modes/recipe | one endgame item |
| 천서 | LOCKED_NOT_IMPLEMENTED | NEEDS_DESIGN_DECISION | same | no item; modes/recipe | one endgame item |
| 세계수 활 | DEFERRED | DEFERRED | equipment finalization optional | no item; optional concept | future design |
| Enhancement foundation | IMPLEMENTED | ALREADY_IMPLEMENTED | equipment finalization + Fabric enhancement | source/mixin | validation only |
| Stage 6 materials | IMPLEMENTED | ALREADY_IMPLEMENTED | Stage 6 document + source/data | runtime present | validation only |
| Stage 7 fishing | IMPLEMENTED | ALREADY_IMPLEMENTED | Stage 7 document + source/data | runtime present | validation only |
| Stage 8 mob trio | IMPLEMENTED | ALREADY_IMPLEMENTED | Fabric zombie source + backlog | runtime present | validation only |
| Stage 9 Skeleton King | IMPLEMENTED | ALREADY_IMPLEMENTED | Fabric skeleton source + backlog | runtime present | validation only |
| Standalone Exploration progression | RETIRED | RETIRED | Stage 10 decision | never revive | none |
| Paper Exploration event engine | RETIRED | RETIRED | same | never port | none |
| Mandatory exploration structures | RETIRED | RETIRED | same | no requirement | none |
| Dungeon progression | RETIRED | RETIRED | same | no requirement | none |
| Exploration-specific economy | RETIRED | RETIRED | same | no requirement | none |
| Coin economy | RETIRED | RETIRED | same | no requirement | none |
| Job-level progression | RETIRED | RETIRED | same | no requirement | none |
| Promotion system | RETIRED | RETIRED | same | no requirement | none |
| Fragment progression | RETIRED | RETIRED | same | no requirement | none |
| Giant crafting GUI | RETIRED | RETIRED | same | no requirement | none |
| Generic RPG world progression | RETIRED | RETIRED | same | no requirement | none |

## 10. Suggested follow-up batches (not mandatory order)

1. Recover full Coating matrix and historical Utility Freeze v3, resolve missing exact contracts; then specify a single coating or utility item.
2. Select one Utility item whose missing mechanics and recipe have been decided, and specify its native interaction.
3. The Shock base slice has a recovered effect contract and NEW_STAGE_10_BREWING_MAPPING; validate recipe collision and implement it alone after user review. Other effects/catalysts remain separate slices.
4. Pick one locked T2/T3/T4 item, recover its concept and decide the slice-specific controls, numbers and recipe. Stage 6-9 live checks remain a separate validation backlog.

Only the bounded Shock base-potion slice is now READY_FOR_VERTICAL_SLICE because its effect contract, single-ingredient mapping and boundary are explicit. This is no permission to implement it in this Work PR. Other equipment/utility/coating candidates remain blocked by their respective missing specifications.

## 11. Documentation inconsistencies

The original checklist was written during pre-implementation content finalization. Its old `[DECIDE]` groups for Stage 6-9, optional Grappling Hook, extra utility roster and World/Structure completion order are historical. The left-click fishing UI conflicts with implemented shift-right-click. `MOD_DEVELOPMENT_RULES.md` correctly calls for vertical content slices after foundation, but its initial horizontal-foundation wording must not be read as requiring another generic engine build. The content-freeze ZIP requires direct extraction before anyone claims the full Coating matrix or utility mechanics have been recovered. Paper Alchemy's empty final recipes file is a migration gap; eight obsolete multi-material recipes and the retained effects/catalysts were recovered from history, and eight new Stage 10 stand mappings are separately labeled.

## 12. Final counts

Consolidated table: **45 rows**. Status: IMPLEMENTED **7**, LOCKED_NOT_IMPLEMENTED **25**, DRAFT_NEEDS_DECISION **0**, DEFERRED **2**, RETIRED **11**, STALE_DOC **0** (stale statements are findings, not content rows). Readiness: ALREADY_IMPLEMENTED **7**, READY_FOR_VERTICAL_SLICE **1**, NEEDS_DESIGN_DECISION **24**, DEFERRED **2**, RETIRED **11**. Counts cover the audited groups and retired policies, not every individual matrix effect, species, mob candidate or stale sentence.

This document changes no gameplay implementation and creates no new design except the explicitly authorized NEW_STAGE_10_BREWING_MAPPING decisions above.
