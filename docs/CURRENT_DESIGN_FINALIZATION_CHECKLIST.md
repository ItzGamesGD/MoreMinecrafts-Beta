# MoreMinecrafts — Current Design Finalization Checklist

Status: **CURRENT DESIGN WORKLIST**

This file is the active checklist for design decisions that remain unresolved after the Stage 17 authority reconciliation.
It is a worklist, not an independent source of gameplay truth. Finalized decisions must be synchronized into the applicable current-authority document before an item is considered complete.

Authority lookup starts at `docs/CONTENT_AUTHORITY_INDEX.md`.
Historical checklists, Paper behavior, recovery ledgers, prototypes, and implementation code are evidence/reference only where current authority differs.

## Status

- [OPEN] Human design decision remains.
- [BALANCE] Gameplay model is settled; numeric/economy tuning remains.
- [LIVE] Requires live validation rather than a new design decision.
- [DEFERRED] Intentionally not active now.
- [SYNC] A recent human direction appears newer than repository authority and must be explicitly reconciled before implementation.
- [DONE] Current authority already resolves the design question.

---

# 0. Design execution order and dependency rule

The numbered system sections below are grouped by system; **their numeric order is not the design execution order**.

Do not assign material sinks first and then invent downstream equipment/content to justify them.
The project follows demand-first dependency closure:

1. finalize downstream content identities and retained rosters,
2. finalize their acquisition/recipe/material requirements,
3. aggregate actual material demand,
4. only then finalize Mining/Material sink identity, recipe quantities, and long-term supply/worldgen economy,
5. perform the final cross-system dependency audit.

### Current execution order

1. Utility Equipment roster/roles and remaining mechanics.
2. Remaining Equipment design: unresolved T2 -> T3 -> T4 identity/progression questions, while leaving material costs open.
3. Enchantment unresolved cross-system design (Laser Arrow / Multi Catch and related integration points).
4. Fishing unresolved optional/economy/ecology decisions.
5. Mobs / Entities roster and gameplay roles.
6. Boss / Major Combat roster, encounter identity, then reward identity.
7. World / Structures / Exploration retained scope and reward/encounter connections.
8. Revisit Resources / Mining / Material Economy using the now-known downstream demand.
9. Resolve acquisition recipes, material allocation, anchor sinks, and long-term material supply.
10. Final cross-system closure and authority audit.

Alchemy, Coating, Knowledge/Research, Trading-as-an-axis, and other explicitly deferred systems do not block this order unless the human explicitly reactivates them.

## Dependency rule

- [DONE] Mining/material structure may exist earlier, but its **final sink identity and economy are downstream-dependent**.
- [DONE] Do not create equipment/content merely to give an existing mineral a use.
- [DONE] Equipment/content identity is designed first; material assignment follows the resulting demand.
- [DONE] Boss reward identity is designed before boss-exclusive T3/T4 recipe dependencies.
- [DONE] Balance/live-validation work does not block unrelated architectural design unless it exposes a design defect.

# 0A. Cross-system finalization

- [OPEN] Complete a final dependency map after the active systems below are designed: material sinks -> recipes -> utility/equipment acquisition -> mobs/boss rewards -> T3/T4 access.
- [DONE] Broad Early -> T1 -> T2 survival identity is fixed without turning MoreMinecrafts into a separate linear RPG tree: vanilla-first early game -> T1 `play Minecraft and grow` -> T2 active exploration/challenge acquisition.
- [OPEN] Extend/close the survival progression ordering beyond T2 only after T3/T4 identities are separately designed.
- [OPEN] After design closure, audit all current-authority documents for contradictions and remove/mark stale design debt.
- [OPEN] Define the boundary between design constants and live-balance/implementation constants consistently across systems.
- [DEFERRED] Knowledge / Research remains idea-only until explicitly promoted.
- [DEFERRED] Trading as a new independent gameplay axis remains idea-only until explicitly promoted.

# 1. Resources / Mining / Material Economy — LATE DEPENDENCY CLOSURE

Current structural authority: `RESOURCE_MINING_MATERIALS_FINALIZATION.md` plus the material-economy debt recorded in `CONTENT_AUTHORITY_INDEX.md`.

## Already resolved

- [DONE] Added mineral roster: Titanium / Tungsten / Mythril / Adamantium / Etherium.
- [DONE] Dimension placement and broad worldgen identity.
- [DONE] Stone/deepslate host-rock behavior.
- [DONE] Silk Touch / Fortune / processing families.
- [DONE] Relative rarity targets and Mythril/Etherium vein ceiling.

## Remaining design — blocked until downstream content is finalized

- [DEFERRED] Assign Titanium's anchor sink/use only after retained downstream equipment/content and recipes are known.
- [DEFERRED] Assign Tungsten's anchor sink/use only after retained downstream equipment/content and recipes are known.
- [DEFERRED] Assign Mythril's anchor sink/use only after retained downstream equipment/content and recipes are known.
- [DEFERRED] Assign Etherium's anchor sink/use only after retained downstream equipment/content and recipes are known.
- [DEFERRED] Assign Adamantium's anchor sink/use only after retained downstream equipment/content and recipes are known.
- [DEFERRED] Decide final Adamantium processed-item/intermediate identity only if downstream recipes require one.
- [DEFERRED] Aggregate material demand across Equipment, Utility, Boss rewards, and T3/T4 acquisition before locking long-term spawn economy.
- [BALANCE] Exact recipe quantities.
- [BALANCE] Final spawn frequency based on survival acquisition testing.

# 2. Equipment Progression — T1/T2/T3/T4

Current authority: `STAGE_2_GAMEPLAY_RECONCILIATION.md`, `EARLY_T1_T2_SURVIVAL_PROGRESSION_LOOP_CHECKPOINT.md`, and applicable finalized equipment authority.

## Already resolved

- [DONE] Ordinary Enhancement: T1 +20 / T2 +20; deterministic +5/+10/+15/+20 milestones.
- [DONE] No ordinary T3/T4 Enhancement.
- [DONE] T2 final seven-item roster.
- [DONE] T2 active-skill input uses normal item/right-click semantics rather than old F/Shift+F route.
- [DONE] T3 remains a separate Special-equipment category; T4 remains Endgame rather than a promotion chain.
- [DONE] Early game remains vanilla-first; custom resources/options may appear lightly from roughly iron-era onward but must remain optional rather than forming a mandatory parallel tech tree.
- [DONE] T1 identity: play ordinary Minecraft, gain XP/resources, and mature equipment through parallel Enhancement + XP Enchantment growth; no dedicated T1 boss gate.
- [DONE] Enhancement Stone is a cheap/easy-to-craft supporting material sink from ordinary resources, not a rare exploration/boss progression gate; recipe is 4 Redstone + 4 Diamonds + 1 Lapis Lazuli -> 16 Enhancement Stones (R D R / D L D / R D R).
- [DONE] Applicable T2 equipment may use the same Enhancement, XP Enchantment, and Book Option equipment-growth axes as T1 through explicit equipment profiles.
- [DONE] XP is the primary T1 progression currency; keep meaningful costs and tune XP supply later rather than trivializing the growth tree. Vanilla XP/farms remain valid.
- [DEFERRED] Future Quest system acts as fixed one-time challenge/achievement-like XP/resource supply and connective glue; no daily rotation, rerolls, or Quest-shop economy. Catalogue/rewards remain a dedicated later design pass.
- [DONE] T1 vanilla-style weapon extensions finalized: Dagger / Greatsword / single non-tiered Gauntlet.
- [DONE] Dagger core contract: ~2.5 reach, Hoe-level speed, ~65% same-tier Sword damage, no sweep, 1 Stick + 1 tier material.
- [DONE] Greatsword core contract: vertical single-target left-click, diagonal +50% sweep-range right-click, Shield unavailable while equipped, Sword + 2 tier materials recipe.
- [DONE] Gauntlet core contract: 3-block reach, ~0.6x bare-hand speed, 2 direct damage, X-Z 3.5 x 1.7 secondary region for 0.5 damage + horizontal 2-block push, no Y knockback.
- [BALANCE] Greatsword exact reach/speed/damage point inside the approved 3.5-4.0 / Axe-or-slower / 130-150% windows and Dagger rounded per-tier values.
- [DEFERRED] Gauntlet recipe shape/quantities; material pool is already limited to Gold / Diamond / Titanium / Iron.

## Remaining T2 design

- [DONE] High-level T2 acquisition identity: active Exploration / Challenge -> Elemental Material or equivalent acquisition reward -> T2 equipment.
- [DONE] Preferred vanilla-adjacent foundations include Pillager content, Trial Chambers, valuable exploration chests, and selectable harder Trial/wave challenges.
- [DONE] T2 reward direction may combine random exploration/chest acquisition with guaranteed or higher-value rewards for harder selected Trial/wave content.
- [OPEN] Finalize exact T2 acquisition contract: material identities, per-element mapping, loot tables/probabilities, Trial/wave mechanics, guaranteed thresholds, and recipe quantities.
- [OPEN] Resolve Fire Sword vs Wind Sword passive identity overlap after/during dedicated combat design.
- [OPEN] Design Dark Energy Staff gameplay identity and Enhancement profile; current registered item is only a placeholder.
- [OPEN] Decide whether Wind Bow receives any homing/aim-correction mechanic and, if retained, its behavioral contract.
- [BALANCE] Final T2 combat values, cooldowns, ranges and VFX hierarchy after live combat testing.

## Remaining T3 design

- [OPEN] Reconfirm the current T3 roster against the intended final MoreMinecrafts direction before implementation.
- [OPEN] Finalize each retained T3 item's Fabric-native gameplay identity rather than blindly porting Paper behavior.
- [OPEN] Decide whether T3 needs any separate growth/progression axis at all.
- [OPEN] Define T3 acquisition/recipe dependencies only after relevant material/boss reward identities exist.
- [BALANCE] T3 combat values and recipe quantities after mechanics are fixed.

## Remaining T4 design

- [OPEN] Reconfirm final T4 roster/baseline and each item's defining gameplay identity before implementation.
- [OPEN] Decide whether T4 has a Mastery/progression system.
- [OPEN] If Mastery is retained, define a progression source that does not reward unrestricted weak-mob farming as the dominant strategy.
- [OPEN] Define T4 acquisition and rare-material/boss dependencies only after Boss reward identity is designed.
- [BALANCE] T4 combat values and recipe quantities.

# 3. Coating

Current authority: `CONTENT_AUTHORITY_INDEX.md`.

- [DEFERRED] Coating is temporarily on hold.
- [DEFERRED] Do not resolve template economy, acquisition, balance, or connect Coating to T3/T4 until higher-tier armor/progression is reconsidered.
- [DEFERRED] Historical coating matrix remains reference only.

# 4. Utility Equipment

Current authority: `UTILITY_EQUIPMENT_FINALIZATION.md`.

## Active roster / mechanics — resolved

- [DONE] Active Utility roster: Miner Lantern / Potion Injector / Exploration Sleeping Bag / Portable Furnace / Mining Drill / Backpack.
- [DONE] Miner Lantern: helmet-adjacent socket in the vanilla inventory UI; LambDynamicLights player-centered dynamic light level 15; no Light Block placement.
- [DONE] Potion Injector: 9 slots; left-click opens inventory; right-click uses stored potions in 1 -> 9 order; immediate application without drink time.
- [DONE] Exploration Sleeping Bag: 2 Wool over 2 Wood; skips night; does not set respawn.
- [DONE] Portable Furnace: item-opened vanilla-furnace-style UI; 3x smelt speed and 3x fuel-consumption rate; 64-stack items capped at 16 per slot; remaining contents drop on close.
- [DONE] Mining Drill: no durability; single-block near-instant Deepslate target; 1 FP/block; Coal=8 FP; Coal Block=72 FP; fuel slot max 64; Silk Touch/Fortune supported.
- [DONE] Backpack: player-bound shared persistent storage; 9/27/54-slot tiers; death-safe contents; chest-armor attachment; vanilla container-style UI.
- [DONE] Equipment-form Jet Boots is retired; the Jet Boots enchantment remains separate authority.

## Deferred / deleted roster

- [DEFERRED] Glider.
- [DEFERRED] Oxygen Tank.
- [DEFERRED] Grappling Hook.
- [DONE] Deleted/retired: Climbing Gloves / equipment-form Jet Boots / Flare / Buoyancy Capsule / Return Crystal.

## Remaining acquisition / balance work

- [DEFERRED] Exact Utility material anchors and unresolved recipe ingredients until downstream material demand is aggregated.
- [DEFERRED] Exact acquisition timing until recipe/material progression is closed.
- [BALANCE] Exact recipe quantities where not explicitly fixed.
- [BALANCE] Mining Drill exact mining-speed constant that realizes the approved near-instant Deepslate target.
- [DONE] Utility gameplay roles and interaction semantics are sufficiently closed to move to the next downstream design stage.

# 5. Fishing

Current authority: `STAGE_3_FISHING_RESOURCE_RECONCILIATION.md`.

## Already resolved

- [DONE] Final 12-species roster.
- [DONE] Reel / Minnow / Crankbait Tier I–III.
- [DONE] Fish Grade I–V.
- [DONE] Server flow: environment eligibility -> Reel-filtered Species -> minigame -> Minnow permission -> Grade -> Length -> Length-dependent Weight -> ItemStack.
- [DONE] Length/Weight relationship and bounded variation architecture.
- [DONE] Runtime resource geometry separated from immutable approved master PNGs.

## Remaining design / balance

- [OPEN] Decide whether the current substantial-water eligibility remains the final ecology/environment model or receives a dedicated ecology redesign.
- [OPEN] Finalize per-species ecology/environment metadata when ecology restrictions become active.
- [OPEN] Decide the final economy/value use of caught fish.
- [DONE] Fish Trap retained as the only approved passive fish-container/automation concept: 2 x 1 placed block, 9 slots, no bait/fuel, normal current fish ItemStacks, stops when full.
- [DONE] Fishing Net is deleted from the current design.
- [BALANCE] Fish Trap catch interval currently uses 1 fish / 5 minutes.
- [DEFERRED] Fish Trap exact recipe/material assignment; String + structural material such as Titanium is only the approved direction until material-economy closure.
- [DONE] Fisherman custom-Fishing economy is to be designed separately from ordinary vanilla Fishing trades; a Vanilla / Custom Fishing trade presentation split is an allowed direction.
- [OPEN] Finalize Fisherman custom trade list, valuation, and whether that separation is implemented as explicit UI sections/tabs or another presentation.
- [BALANCE] Value Index -> Emerald/reward conversion if selling is retained.
- [BALANCE] Exact module durability and repair/upgrade material costs.
- [BALANCE] Species distribution/encounter weights and exact length/weight curves.
- [BALANCE] Minigame probabilities/speeds if live testing requires changes.
- [LIVE] GUI/card interaction, HUD readability, item presentation and irregular-water full-loop validation.

# 6. Enchantments

Current gameplay-semantics authority: `ENCHANTMENT_BRANCH_SKELETON.md`.

Current T1 progression authority: `T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md`.

## Already resolved

- [DONE] Retained enchantment roster and removed legacy concepts.
- [DONE] Core gameplay semantics for Multi Shot, Crossbow Barrage, Vein Mining, Auto Replant, Auto Smelt, Lumberjack, Elytra Launch, Explosive Mace, Feather Boots and Jet Boots.
- [DONE] T1 ordinary/random enchanting progression is replaced by an XP-funded per-equipment skill tree.
- [DONE] Same-enchantment levels unlock sequentially; ordinary unrelated branches do not receive artificial prerequisites.
- [DONE] Global durability branch is Unbreaking I -> II -> III -> Mending and is deliberately expensive.
- [DONE] Compatibility model is free stacking / premium stacking / true structural exclusion.
- [DONE] Mending + Infinity allowed.
- [DONE] Crossbow Piercing + Multishot allowed.
- [DONE] Bow Multi Shot vs Laser Arrow remains structural exclusivity.
- [DONE] Crossbow Barrage vs Multishot remains structural exclusivity.
- [DONE] Powerful overlapping damage/protection families may stack through additional XP premiums instead of blanket prohibition.
- [DONE] Armor protection-family premium baseline is +500 / +1,000 / +2,000 XP for the second / third / fourth families.
- [DONE] T1 XP cost direction is intentionally expensive; later balance should primarily tune XP supply.
- [DONE] Approved equipment-tree placements for Sword, Greatsword, Dagger, Axe, Pickaxe, Shovel, Hoe, Mace, Spear, Bow, Crossbow, Helmet, Chestplate, Leggings, Boots, Elytra, Shield and Fishing Rod are recorded in the T1 finalization authority.
- [DONE] Enhancement remains a separate +0 -> +20 T1 growth axis.
- [DONE] Trident tree: Impaling I-V / Loyalty I-III / Riptide I-III / Channeling / Unbreaking I-III -> Mending; mechanically impossible Trident combinations remain structural exclusions.
- [DONE] The old Enchanted Book + Anvil direct-enchantment route must not bypass the XP skill tree.
- [DONE] Eligible retained Enchanted Books may serve as a secondary Book Option layer: one application establishes a fixed base Option, while higher book levels may add a level bonus.
- [DONE] Book Options are not freely overwritten; changing/clearing one requires a dedicated reset resource.
- [DONE] Not every Enchanted Book must survive; books unsuitable for a meaningful Option may have their itemized book variant removed after classification.
- [DEFERRED] Quest catalogue and XP/resource reward supply are intentionally deferred to a dedicated cross-system design pass.
- [DONE] No Jet Boots naming/role conflict remains because the historical Utility equipment-form Jet Boots is retired; the enchantment remains authoritative.

## Remaining design / explicit authority gaps

- [OPEN] Laser Arrow dedicated projectile/resource/VFX/presentation contract; its Bow tree placement is already fixed at Power III and exclusive with Multi Shot.
- [OPEN] Multi Catch integration with the current Fishing pipeline and its exact skill-tree placement; do not restore Paper reward multiplication.
- [OPEN] Jet Boots exact placement/prerequisite/cost in the new T1 XP skill tree.
- [OPEN] Gauntlet enchantment-tree identity/placement.
- [OPEN] Sword Looting exact tree placement because it was not explicitly placed in the approved Sword-tree pass.
- [OPEN] Chestplate Thorns placement; omission from the bulk armor pass is not authority to delete it.
- [OPEN] Exact skill-tree persistence/binding semantics if implementation must distinguish physical-item state from player/category-wide state.
- [OPEN] Classify every Enchanted Book as retained Book Option / special redesign / itemized book removed.
- [OPEN] Finalize Book Option base effects, level-bonus formula, slot model/UI, and reset-resource identity/economy.
- [OPEN] Exact respec/reset interaction and cost.
- [BALANCE] Final per-node numeric costs where only a cost class/range is authoritative.
- [BALANCE] Crossbow Barrage interval and other explicitly live-tunable constants.
- [LIVE] Enchantment interaction/combat feel where headless CI cannot prove behavior.

# 6A. Villager trading integration

Current checkpoint authority: `VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md`.

- [DONE] Farmer: keep current vanilla role.
- [DONE] Fisherman: solve through separate custom-Fishing economy design; Vanilla / Custom Fishing trade separation is an allowed direction.
- [OPEN] Fletcher: retain vanilla baseline, with exact high-throughput economy/combat-supply review deferred to a focused pass.
- [DONE] Librarian is not automatically replaced wholesale; direct book enchanting is removed and retained books are recontextualized through the Book Option design.
- [DONE] Librarian retained-book policy: 1% eligible-candidate retention, one use per restock, fixed effective 64-Emerald price despite vanilla modifiers, retained-book allow-list only.
- [DONE] Cartographer: no current change. Any future promoted Exploration/structure content authority must explicitly review Cartographer trade integration.
- [DONE] Cleric: no current change. Any future promoted Brewing/Alchemy/relevant supernatural-resource authority must explicitly review Cleric trade integration.
- [DONE] Armorer / Weaponsmith / Toolsmith: corresponding intact equipment + Emeralds upgrades to moderately enchanted corresponding equipment; generated equipment offers are converted, submitted progression is preserved, and no-op upgrades are hidden.
- [DONE] Mason / Shepherd / Butcher / Leatherworker: keep current vanilla roles unless a later concrete system creates a conflict.
- [DONE] Villager balance must not depend only on nominal rarity because breeding/restocking can create repeatable supply.

# 7. Alchemy

Repository authority currently still describes Alchemy as a Brewing Stand extension and lists discoverability debt.

- [SYNC] Recent human direction places Alchemy on hold; repository authority must be explicitly reconciled before any Alchemy implementation/design task proceeds.
- [DEFERRED] Until that reconciliation, do not expand potion/effect/catalyst roster or UX.
- [DEFERRED] If Alchemy is later reactivated, first decide the retained content roster and discovery UX without creating a large combinatorial catalyst matrix.

# 8. Mobs / Entities

No current finalized roster authority is routed from the authority index.

- [OPEN] Audit existing implemented/reference mobs against the present MoreMinecrafts philosophy.
- [OPEN] Decide final retained/deleted mob roster.
- [OPEN] Decide vanilla-variant vs custom-entity implementation identity per retained mob.
- [OPEN] Define biome/dimension/environment and spawn conditions per retained mob.
- [OPEN] Define meaningful drops only after material/equipment dependencies are known.
- [OPEN] Finalize each retained mob's core combat pattern.
- [BALANCE] HP / damage / speed / cooldown / rarity / group size.

# 9. Boss / Major Combat

Current authority records Boss -> T3/T4 dependency order as deferred.

- [OPEN] Audit historical bosses/prototypes and decide final boss roster.
- [OPEN] Define each retained boss's encounter identity and progression role.
- [OPEN] Decide encounter form: structure / summon / natural / other.
- [OPEN] Define reward identity before inventing boss-exclusive crafting dependencies.
- [OPEN] Define rare material/drop only after reward identity.
- [OPEN] Connect boss rewards to T3/T4 only after the preceding decisions.
- [OPEN] Decide arena/phase/gimmick structure per boss.
- [BALANCE] Boss combat values and reward rates.

# 10. World / Structures / Exploration

No current finalized structure/exploration roster authority is routed from the authority index.

- [OPEN] Re-evaluate whether this axis remains active under the current “expand vanilla” philosophy.
- [OPEN] Audit historical structure expansion concepts and retain/delete/defer each one.
- [OPEN] Distinguish vanilla-structure variation from genuinely new structures.
- [OPEN] Define structure-specific mobs/loot/puzzles only for retained structures.
- [OPEN] Connect structures to materials/bosses only after those reward identities exist.
- [OPEN] Decide whether any new biome is actually needed.
- [OPEN] Decide whether any new dimension is actually needed.
- [BALANCE] Discovery frequency and loot value.

# 11. New gameplay axes

- [DEFERRED] Knowledge / Research remains a parked concept, not implementation authority.
- [DEFERRED] If explicitly promoted later, design it as horizontal progression; current idea direction is vanilla-XP-funded persistent knowledge with small cross-system benefits and no recipe gating.
- [DEFERRED] Trading as an independent axis remains parked.
- [DEFERRED] Other new axes require a dedicated human-approved design task before expansion.

# 12. Final design closure gate

The design phase is not complete until:

- [OPEN] Every active section above has no unresolved [OPEN] or [SYNC] item that materially changes gameplay architecture.
- [OPEN] Every finalized decision has been synchronized into its applicable current-authority document.
- [OPEN] `CONTENT_AUTHORITY_INDEX.md` routes every active system to its current authority.
- [OPEN] Historical documents that conflict with current authority are clearly marked historical/superseded.
- [OPEN] Cross-system dependency audit is complete.
- [OPEN] Implementation-ready tasks can be written without inventing missing gameplay design.
- [OPEN] Balance-only and live-validation-only items are clearly separated from design blockers.

Only after this gate should the project treat the broad content design as frozen enough for systematic implementation.
