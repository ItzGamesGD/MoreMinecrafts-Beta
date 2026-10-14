# Stage 2 gameplay and progression reconciliation

Status: implementation reconciliation on the cumulative recovery line. This document records the current contract after comparing the Stage 11 implementation with the authoritative `MoreMinecrafts Enhancement 2.0 재설계안` in Google Drive. It does not claim live combat validation.


## Stage 19 T2 acceptance state

- **T2 IMPLEMENTATION: COMPLETE** — the implemented gameplay structures for Fire Sword, Water Sword, Wind Sword, Earth Mace, and Lightning Sword are integrated on Stage 19.
- **T2 AUTOMATED ACCEPTANCE: PASS** — PR #66 passed Fabric CI #291. The run executed `./gradlew clean build`, all 3 required GameTests, JUnit, configured repository validators, and server/client smoke jobs, including registry initialization.
- **T2 LIVE ACCEPTANCE: PENDING** — no interactive Minecraft test has been performed or is claimed. The gameplay checks below remain live-test debt.
- **T2 VFX: DEFERRED** — final particles, visual assets, orientation and animation polish, hierarchy, and cosmetic timing/density are outside gameplay acceptance. Existing provisional VFX may remain in place.
- **T2 BALANCE / FEEL TUNING: LIVE PENDING** — structural mechanics are accepted; combat feel and documented provisional values remain open to direct live tuning.

Promotion on automated evidence does not claim that live acceptance occurred. Any later live failure should be handled as a focused post-checkpoint defect and does not change the historical fact that Stage 19 was promoted with live acceptance pending.

## Enhancement

- Ordinary T1 and T2 Enhancement cap at +20. Milestones are +5, +10, +15, and +20; transitions are deterministic, with no chance, failure, pity, downgrade, destruction, random options, or promotion.
- Existing pure `EnhancementOperation` remains independent of Anvil UI. Anvil remains a thin temporary adapter.
- Current provisional sword growth remains +2.60 total attack damage at +20. T3 and T4 are excluded from ordinary Enhancement.
- Historical +30 Vanilla / +40 T2 text is marked superseded in the older progression/checklist records. Coating remains on hold.

## T2 roster and ability contracts

The seven-item roster is Fire Sword, Water Sword, Wind Sword, Wind Bow, Earth Mace, Lightning Sword, and Dark Energy Staff. There is no Ice T2 or elemental-by-weapon matrix.

- **Fire Sword:** ordinary successful primary hits produce small fire VFX. Every tenth successful primary hit triggers the horizontal AoE sweep and resets the counter. Right-click activates the rising diagonal ability with a 600-tick cooldown. Its approved production PNG remains protected; runtime presentation follows the Stage 18 resource authority, which superseded the obsolete two-face geometry with a bounded solid six-face handheld model.
- **Water Sword:** retains fast sword identity and a small heal for a qualifying kill. Right-click starts the existing server-owned launch, aimed dive, airborne-to-ground landing check, impact, and water-pillar cleanup sequence. The launch must reach a meaningfully greater height and the aimed dive must traverse meaningfully faster than the current implementation so the sequence reads as a higher rise followed by a faster committed dive. Exact launch height, dive speed, timeout, and combat values remain provisional for live tuning; no blocks are changed.
- **Wind Sword:** successful primary hits produce small wind VFX; every tenth triggers a real AoE sweep and resets. Right-click activates Wind Blade. Wind Step and all Shift+F/secondary routes are removed.
- **Lightning Sword:** the cumulative successful-hit charge triggers at five, applies its proc and resets. There is no timeout or persistence across restarts. Right-click activates Lightning Slash. Exact combat values remain provisional.
- **Earth Mace:** the passive is gated by a valid vanilla smash and affects only its custom item. A valid smash emits a circular ground-travelling seismic wave centered on the landing point; it damages grounded targets caught by the wave, applies a short slow, and displaces them outward. The active remains a right-click forward cone shockwave with damage/displacement, keeping the passive radial/grounded and the active directional/immediate. Neither path modifies terrain. Exact seismic radius, damage, slow duration/strength, displacement, and presentation remain provisional for live tuning. The existing global Mace mixin continues to serve Enhancement smash scaling only.
- **Wind Bow:** remains a vanilla Bow with enchantment and projectile semantics. Homing/autoaim is deferred.
- **Dark Energy Staff:** remains registered as a roster placeholder. Its former prototype ray attack is disabled; gameplay and Enhancement are deferred.

Fire Sword, Wind Sword, and Lightning Sword otherwise retain their current contracts above. Fire and Wind both retain hit-count-based broad AoE passives; this specific overlap was accepted by the dedicated T2 gameplay finalization and is not pending redesign. Their other mechanics and visual identities remain distinct. T2 active skills are requested through the item's normal use/right-click path. The obsolete F/Shift+F client mapping and its unused T2 network dispatch are removed. The server remains authoritative for gameplay effects and hit counters.

## T2 elemental VFX language

The five core elemental combat lineages share a reusable VFX composition model: **Trail -> Impact -> Skill Shape -> Residual**. Individual effects may omit stages that do not improve readability, but new T2 effects should be assembled from this common visual grammar rather than invented as unrelated one-off particle systems.

Reusable world-space VFX primitives are **Arc, Trail, Ring, Beam, Burst, and Ground Decal**. These primitives are intended to be reusable by later tiers. GeckoLib, where used, is for item/model animation needs; world-space slashes, trails, rings, pillars, impacts, and decals remain a separate VFX concern rather than being forced into the model-animation layer.

- **Fire:** thick, irregular, persistent motion language. Primary swings use a short flame trail and ember impact. The tenth-hit sweep uses a broad, thick horizontal flame arc. The rising right-click ability reads as a diagonal upward flame arc rather than a vertical pillar, avoiding silhouette overlap with Water.
- **Water:** translucent water motion with splash/foam accents. Launch begins with a circular foot-level splash/compression cue; the aiming interval remains visually restrained for readability. The accelerated dive produces a long narrow water trail that reinforces speed. Landing resolves as a circular splash followed shortly by the vertical water pillar.
- **Wind:** thin, clean, fast-decaying motion language, deliberately contrasting Fire despite their shared tenth-hit threshold structure. Ordinary hits use restrained crescent/air-cut cues. The tenth-hit sweep uses multiple thin curved air lines rather than a filled arc. Wind Blade is represented by layered compressed crescent/air blades travelling forward.
- **Earth:** dust, debris, seismic rings, and temporary ground-crack decals. A valid smash begins with a landing dust/debris burst, followed by the circular seismic ring; temporary crack presentation may appear along the affected ground but must not modify blocks or terrain. The right-click cone shockwave instead pushes dust and small debris forward, keeping passive and active silhouettes distinct.
- **Lightning:** sparse particles with intensity concentrated into brightness, branching, and very short timing rather than screen-filling volume. Ordinary pre-proc hits use restrained sparks. The fifth-hit proc uses a brief bright flash with branching electricity. Lightning Slash uses a very short bright slash followed by a thin electrical afterimage.

Exact colors, texture assets, particle counts, lifetimes, dimensions, opacity, sound timing, camera response, and performance budgets remain provisional until implementation and live visual tuning. The governing resource principle is **simple model / strong silhouette / detailed runtime VFX**; high-detail item geometry is not required merely to make an elemental weapon visually rich.

## Hit-progress lifetime and boundaries

Fire, Wind, and Lightning progress remains server-memory state keyed by player UUID and weapon family. It is recorded only from each item's primary `hurtEnemy` callback, so active and passive direct damage cannot recursively advance the counter. A main-hand family change, leaving the tracked sword families, death, or logout clears progress. Lightning does not decay with elapsed time; restart clears all in-memory progress.

## Validation and status

### Automated evidence

PR #66 was merged into the Stage 19 candidate from head `7e5dbd730e56d4f4c564e4833ae57bd98955b267`. Fabric CI #291 passed its PR merge candidate with `./gradlew clean build`. The run executed the configured JUnit suite, all 3 required GameTests, Fishing resource validation, Flame Sword production asset/resource validation, lower-equipment validation, and Stage 6 data validation. Server and client smoke jobs passed, and common/client registry initialization completed. This is automated acceptance evidence; it is not live gameplay acceptance.

### Live acceptance — PENDING

No interactive Minecraft client test is recorded. Live validation remains required for:

- Fire's 10th-hit behavior, active combat, and PvP/allied filtering.
- Water launch and dive feel, aimed movement, ground-contact impact, repeated activation, and cleanup under death, logout, dimension change, and timeout.
- Wind's 10th-hit behavior, Wind Blade targeting and block clipping, and combat feel.
- Earth's valid smash gate, impact-ground centering, radial wave, grounded eligibility, damage/Slowness/displacement, and active forward cone.
- Lightning's fifth-hit proc/reset and Slash targeting/damage.
- Shared family-switch and runtime cleanup behavior, server authority, and PvP/allied filtering in multiplayer.

### VFX — DEFERRED

Final custom VFX assets, particle/arc appearance and orientation, animation polish, visual hierarchy, cosmetic timing, and density remain deferred. Provisional effects already in the implementation may stay in place. VFX quality does not block T2 gameplay acceptance.

### Balance / feel tuning — LIVE PENDING

Water launch/dive velocities and impact tuning, Earth radius/damage/slow/displacement, Lightning Slash tuning, and other values marked provisional in code remain live-tunable. Their current values do not block structural gameplay acceptance.
