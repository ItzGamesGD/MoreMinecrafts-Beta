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

## 5. Alchemy audit

**DRAFT_NEEDS_DECISION / NEEDS_DESIGN_DECISION; unimplemented in Fabric.** Locked direction is vanilla Brewing Stand and Minecraft-native potion infrastructure where appropriate; no giant crafting GUI or Paper architecture port. Paper `alchemy/catalysts.yml` recognizes Redstone (duration), Glowstone Dust (amplifier), Gunpowder (splash), Dragon Breath (lingering), Fermented Spider Eye (restricted inversion), Sculk Catalyst, Echo Shard, Slime Ball, Wind Charge and Fire Charge (special). These are recovered *Paper candidates*, not a locked Fabric catalyst roster. Paper `potions.yml` has eight enabled named potions (vampire, berserk, corrosion, frostbite, shock, bleed, vulnerability, necrosis) and a disabled test potion; their adoption is undecided.

Paper `recipes.yml` currently has `recipes: {}`: **no concrete base + ingredient → potion transitions can be recovered from that production file**. Its stated relationship is one bottle-slot base ID plus one ingredient-slot ID yielding one potion ID; catalysts use staged transitions in the same visible Brewing Stand slots. `BrewingInputResolver` resolves canonical custom potion ID first for bases, then custom item ID or `vanilla:<material>`; for ingredients it distinguishes identified custom items, enabled catalyst item/material and vanilla material. `AlchemyMaterialResolver` explicitly disallows fallback from a custom item to material-only identity. `BrewingStandAlchemyListener` leaves unmatched vanilla brewing untouched; matched invalid custom transitions abort atomically. These are reference behaviors, not prescribed Fabric class design. Final potion/effect/catalyst roster, ingredients and transitions, allowed combinations, UI and balance require decisions. Recommended next Work specification: choose a minimal *existing* confirmed transition only after its source is found or user specifies it; Codex slice then implements one native Brewing Stand transition and identity/persistence tests.

Paper sources: `src/main/resources/alchemy/{catalysts,potions,recipes}.yml`, `src/main/java/com/hyunseo/hyunseorpg/alchemy/{brewing/BrewingInputResolver,brewing/BrewingStandAlchemyListener,material/AlchemyMaterialResolver}.java` in `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper`.

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
| Alchemy | DRAFT_NEEDS_DECISION | NEEDS_DESIGN_DECISION | Stage 10 direction + Paper requirements | no Fabric files; empty Paper recipe registry | one approved transition |
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
3. Resolve one Paper Alchemy ingredient relationship and approve the Fabric roster subset before any brewing slice.
4. Pick one locked T2/T3/T4 item, recover its concept and decide the slice-specific controls, numbers and recipe. Stage 6-9 live checks remain a separate validation backlog.

No entry is currently marked READY_FOR_VERTICAL_SLICE under a strict implementation-ready standard: locked identity alone does not specify the recipe, exact behavior and acceptance criteria of a full slice. Work can make an individual candidate ready after the missing decisions and source recovery; the follow-up does not imply blanket approval.

## 11. Documentation inconsistencies

The original checklist was written during pre-implementation content finalization. Its old `[DECIDE]` groups for Stage 6-9, optional Grappling Hook, extra utility roster and World/Structure completion order are historical. The left-click fishing UI conflicts with implemented shift-right-click. `MOD_DEVELOPMENT_RULES.md` correctly calls for vertical content slices after foundation, but its initial horizontal-foundation wording must not be read as requiring another generic engine build. The content-freeze ZIP requires direct extraction before anyone claims the full Coating matrix or utility mechanics have been recovered. Paper Alchemy's empty recipes file blocks claims of a production formula.

## 12. Final counts

Consolidated table: **45 rows**. Status: IMPLEMENTED **7**, LOCKED_NOT_IMPLEMENTED **24**, DRAFT_NEEDS_DECISION **1**, DEFERRED **2**, RETIRED **11**, STALE_DOC **0** (stale statements are findings, not content rows). Readiness: ALREADY_IMPLEMENTED **7**, READY_FOR_VERTICAL_SLICE **0**, NEEDS_DESIGN_DECISION **25**, DEFERRED **2**, RETIRED **11**. Counts cover the audited groups and retired policies, not every individual matrix effect, species, mob candidate or stale sentence.

This document changes no gameplay implementation and creates no new design.
