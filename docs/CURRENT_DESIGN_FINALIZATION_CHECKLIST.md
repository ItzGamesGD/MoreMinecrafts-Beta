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

# 0. Cross-system finalization

- [OPEN] Complete a final dependency map after the active systems below are designed: material sinks -> recipes -> utility/equipment acquisition -> mobs/boss rewards -> T3/T4 access.
- [OPEN] Decide the intended overall survival progression ordering without turning MoreMinecrafts into a separate linear RPG progression.
- [OPEN] After design closure, audit all current-authority documents for contradictions and remove/mark stale design debt.
- [OPEN] Define the boundary between design constants and live-balance/implementation constants consistently across systems.
- [DEFERRED] Knowledge / Research remains idea-only until explicitly promoted.
- [DEFERRED] Trading as a new independent gameplay axis remains idea-only until explicitly promoted.

# 1. Resources / Mining / Material Economy

Current structural authority: `RESOURCE_MINING_MATERIALS_FINALIZATION.md` plus the material-economy debt recorded in `CONTENT_AUTHORITY_INDEX.md`.

## Already resolved

- [DONE] Added mineral roster: Titanium / Tungsten / Mythril / Adamantium / Etherium.
- [DONE] Dimension placement and broad worldgen identity.
- [DONE] Stone/deepslate host-rock behavior.
- [DONE] Silk Touch / Fortune / processing families.
- [DONE] Relative rarity targets and Mythril/Etherium vein ceiling.

## Remaining design

- [OPEN] Give Titanium a meaningful anchor sink/use.
- [OPEN] Give Tungsten a meaningful anchor sink/use.
- [OPEN] Give Mythril a meaningful anchor sink/use.
- [OPEN] Give Etherium a meaningful anchor sink/use.
- [OPEN] Give Adamantium a meaningful anchor sink/use.
- [OPEN] Decide final Adamantium processed-item/intermediate identity if recipes require one.
- [OPEN] Reconcile material demand across Equipment, Utility, and later T3/T4 recipes before locking long-term spawn economy.
- [BALANCE] Exact recipe quantities.
- [BALANCE] Final spawn frequency based on survival acquisition testing.

# 2. Equipment Progression — T1/T2/T3/T4

Current authority: `STAGE_2_GAMEPLAY_RECONCILIATION.md` and applicable finalized equipment authority.

## Already resolved

- [DONE] Ordinary Enhancement: T1 +20 / T2 +20; deterministic +5/+10/+15/+20 milestones.
- [DONE] No ordinary T3/T4 Enhancement.
- [DONE] T2 final seven-item roster.
- [DONE] T2 active-skill input uses normal item/right-click semantics rather than old F/Shift+F route.
- [DONE] T3 remains a separate Special-equipment category; T4 remains Endgame rather than a promotion chain.

## Remaining T2 design

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

Current authority is incomplete; historical utility decisions must be reconciled before implementation.

## Roster / role reconciliation

- [OPEN] Rebuild the final Utility roster from the latest human decisions rather than treating the historical checklist as current authority.
- [OPEN] For every retained Utility item, define its unique gameplay role and acquisition stage.
- [OPEN] Resolve movement overlap among the retained movement utilities, deferred Grappling Hook, Return Crystal and vanilla Elytra progression; equipment-form Jet Boots is retired and must not be reintroduced.
- [OPEN] Decide which historical Utility concepts are deleted, retained, or intentionally deferred.
- [DONE] Equipment-form Jet Boots is retired. The retained Jet Boots concept is the enchantment defined by `ENCHANTMENT_BRANCH_SKELETON.md`; there is no Utility-vs-Enchantment Jet Boots conflict to resolve.

## Mechanics / acquisition

- [OPEN] Finalize retained Utility controls and interaction semantics.
- [OPEN] Finalize retained Utility durability/consumption/fuel rules.
- [OPEN] Finalize retained Utility recipes and material anchors.
- [OPEN] Finalize Return Crystal destination/channel semantics if retained.
- [OPEN] Finalize Portable Furnace constraints/efficiency if retained.
- [OPEN] Finalize Drill mining/fuel contract if retained.
- [BALANCE] Recipe quantities, acquisition timing and movement values.

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
- [OPEN] Decide whether fish-container / 어망 functionality returns; Stage 3 explicitly did not add it.
- [OPEN] Decide whether a dedicated Fisherman sale path returns; Stage 3 explicitly did not add it.
- [BALANCE] Value Index -> Emerald/reward conversion if selling is retained.
- [BALANCE] Exact module durability and repair/upgrade material costs.
- [BALANCE] Species distribution/encounter weights and exact length/weight curves.
- [BALANCE] Minigame probabilities/speeds if live testing requires changes.
- [LIVE] GUI/card interaction, HUD readability, item presentation and irregular-water full-loop validation.

# 6. Enchantments

Current authority: `ENCHANTMENT_BRANCH_SKELETON.md`.

## Already resolved

- [DONE] Retained enchantment roster and removed legacy concepts.
- [DONE] Core gameplay semantics for Multi Shot, Crossbow Barrage, Vein Mining, Auto Replant, Auto Smelt, Lumberjack, Elytra Launch, Explosive Mace, Feather Boots and Jet Boots.
- [DONE] Laser Arrow vs Multi Shot exclusivity.
- [DONE] Feather Boots vs Jet Boots exclusivity.
- [DONE] Enchantments belong in the vanilla acquisition ecosystem rather than a custom shop/currency.

## Remaining design

- [OPEN] Laser Arrow dedicated projectile/resource/VFX/presentation contract.
- [OPEN] Multi Catch integration with the current Fishing pipeline; do not restore Paper reward multiplication.
- [OPEN] Decide final enchantment acquisition weights/distribution only after the complete roster/economy context is ready.
- [DONE] No Jet Boots naming/role conflict remains because the historical Utility equipment-form Jet Boots is retired; the enchantment remains authoritative.
- [BALANCE] Crossbow Barrage interval and other explicitly live-tunable constants.
- [LIVE] Stage 17 enchantment interaction/combat feel where headless CI cannot prove behavior.

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
