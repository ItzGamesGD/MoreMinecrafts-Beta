# Stage 2 gameplay and progression reconciliation

Status: implementation reconciliation on the cumulative recovery line. This document records the current contract after comparing the Stage 11 implementation with the authoritative `MoreMinecrafts Enhancement 2.0 재설계안` in Google Drive. It does not claim live combat validation.

## Enhancement

- Ordinary T1 and T2 Enhancement cap at +20. Milestones are +5, +10, +15, and +20; transitions are deterministic, with no chance, failure, pity, downgrade, destruction, random options, or promotion.
- Existing pure `EnhancementOperation` remains independent of Anvil UI. Anvil remains a thin temporary adapter.
- Current provisional sword growth remains +2.60 total attack damage at +20. T3 and T4 are excluded from ordinary Enhancement.
- Historical +30 Vanilla / +40 T2 text is marked superseded in the older progression/checklist records. Coating remains on hold.

## T2 roster and ability contracts

The seven-item roster is Fire Sword, Water Sword, Wind Sword, Wind Bow, Earth Mace, Lightning Sword, and Dark Energy Staff. There is no Ice T2 or elemental-by-weapon matrix.

- **Fire Sword:** ordinary successful primary hits produce small fire VFX. Every tenth successful primary hit triggers the horizontal AoE sweep and resets the counter. Right-click activates the rising diagonal ability with a 600-tick cooldown. The approved production PNG and its two-face model geometry are unchanged.
- **Water Sword:** retains fast sword identity and a small heal for a qualifying kill. Right-click starts the existing server-owned launch, aimed dive, airborne-to-ground landing check, impact, and water-pillar cleanup sequence. The launch must reach a meaningfully greater height and the aimed dive must traverse meaningfully faster than the current implementation so the sequence reads as a higher rise followed by a faster committed dive. Exact launch height, dive speed, timeout, and combat values remain provisional for live tuning; no blocks are changed.
- **Wind Sword:** successful primary hits produce small wind VFX; every tenth triggers a real AoE sweep and resets. Right-click activates Wind Blade. Wind Step and all Shift+F/secondary routes are removed.
- **Lightning Sword:** the cumulative successful-hit charge triggers at five, applies its proc and resets. There is no timeout or persistence across restarts. Right-click activates Lightning Slash. Exact combat values remain provisional.
- **Earth Mace:** the passive is gated by a valid vanilla smash and affects only its custom item. A valid smash emits a circular ground-travelling seismic wave centered on the landing point; it damages grounded targets caught by the wave, applies a short slow, and displaces them outward. The active remains a right-click forward cone shockwave with damage/displacement, keeping the passive radial/grounded and the active directional/immediate. Neither path modifies terrain. Exact seismic radius, damage, slow duration/strength, displacement, and presentation remain provisional for live tuning. The existing global Mace mixin continues to serve Enhancement smash scaling only.
- **Wind Bow:** remains a vanilla Bow with enchantment and projectile semantics. Homing/autoaim is deferred.
- **Dark Energy Staff:** remains registered as a roster placeholder. Its former prototype ray attack is disabled; gameplay and Enhancement are deferred.

Fire Sword, Wind Sword, and Lightning Sword otherwise retain their current contracts above; their existing hit-threshold structures are intentionally not redesigned in this pass. T2 active skills are requested through the item's normal use/right-click path. The obsolete F/Shift+F client mapping and its unused T2 network dispatch are removed. The server remains authoritative for gameplay effects and hit counters.

## Hit-progress lifetime and boundaries

Fire, Wind, and Lightning progress remains server-memory state keyed by player UUID and weapon family. It is recorded only from each item's primary `hurtEnemy` callback, so active and passive direct damage cannot recursively advance the counter. A main-hand family change, leaving the tracked sword families, death, or logout clears progress. Lightning does not decay with elapsed time; restart clears all in-memory progress.

## Validation and status

- JUnit covers the family thresholds, family isolation, Lightning's cumulative threshold/reset, and progress cleanup policy.
- The repo's existing Enhancement +20/milestone tests remain in force; no Enhancement mechanics were redesigned in this slice.
- CI run #195 passed `./gradlew build` (JUnit plus configured Stage 6 and Flame Sword C1-A/C1-B validators), distributable JAR collection, server smoke, and client/resource smoke. No separate Fishing-specific validator is configured in the inherited workflow.
- Server readiness was confirmed by `Done (4.993s)! For help, type help`; the fatal registry/datapack marker scan was clean. Client smoke created the Vulkan/llvmpipe backend, reloaded `moreminecrafts`, created `minecraft:textures/atlas/items.png-atlas`, and found no texture/model/decode/bake/stitch errors.
- No live combat-feel or PvP claim is made. Client-side item presentation, movement, targeting, VFX hierarchy, multiplayer behavior, and balance tuning remain LIVE PENDING.
