# SKY TOWER — OWNER DECISIONS REQUIRED
## Design Decisions Required from Human Authority to Finalize Sky Tower

**Date:** 2026-11-09  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Target Branch:** `feature/sky-tower-integration-02`  
**Integration Baseline (C-4):** `fdaa7effe96746ffe5e6e4454acc916cba277ff4`  

In accordance with `AGENTS.md` ("WHAT = Human, HOW = Agent"), coding agents are strictly prohibited from inventing gameplay rules, container types, balance percentages, or progression triggers. The following decisions must be made by the human project owner before the Sky Tower can be transitioned from integration candidate to fully complete feature.

---

### Decision 1: Sky Tower Vault Loot Table Reconciliation
- **Affected Requirements:** `REQ-VAULT-02`
- **Question for Owner:**  
  Should the 8 Vaults in the Sky Tower be updated to drop custom Sky Islands loot (`moreminecrafts:chests/sky_islands/vault`), or should they continue dropping vanilla trial chamber rewards?

#### Context
- `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 explicitly defines the Tier 2 progression loot table: **Sky Feather 15% (×1), Gale Shard 5% (×1)**, and custom materials. This table already exists at `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault.json`.
- However, Q07 in `SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` mandated preserving vanilla trial rewards until custom pools were finalized. During INTEGRATION-02, the developer preserved vanilla NBT to maintain SHA stability.
- Without custom loot, the Sky Tower yields **0% Sky Feathers and 0% Gale Shards**, making Tier 2 weapons (**Wind Bow**, **Wind Sword**) and accessories (**Feather Charm**) uncraftable from tower exploration.

#### Non-Binding Engineering Options
- **Option A (Inject Custom Loot via NBT Update — Recommended):**  
  Update `full_01.nbt` so the 8 Vault block entities configure `config.loot_table: "moreminecrafts:chests/sky_islands/vault"`.  
  *Pros:* Immediately activates Tier 2 progression economy; 100% compliant with §275.  
  *Cons:* Modifies NBT SHA (`16ad9d5`), requiring regenerating composite templates.
- **Option B (Server-Side Loot Table Injection Hook):**  
  Keep `full_01.nbt` frozen; in `SkyTowerWorldgenRegistration`, when a site registers, update the Vault block entities programmatically to point to `sky_islands/vault`.  
  *Pros:* Zero changes to NBT templates; cleanly reversible via config.  
  *Cons:* Introduces extra Java runtime block entity modification logic.
- **Option C (Defer Custom Loot to Phase D):**  
  Formally ratify vanilla trial rewards as the temporary baseline; schedule loot table integration for a future balance pass.  
  *Pros:* No changes required now.  
  *Cons:* Tower exploration remains disconnected from Sky Realm crafting progression.

---

### Decision 2: Rooftop Sanctuary Special Reward Container & Location
- **Affected Requirements:** `REQ-VAULT-04`
- **Question for Owner:**  
  What type of container should house the rooftop special reward, and where precisely in the rooftop sanctuary should it be placed?

#### Context
- Both `SKY_ISLANDS_T2_CONTEXT` §158/§161 and Q08/Q24 mandate a pinnacle special reward on the rooftop with doubled unique drop rates. Data table `moreminecrafts:chests/sky_islands/vault_tower_top.json` exists in data assets.
- In `full_01.nbt`, the rooftop sanctuary (Y=64..79) contains only the 12 arches and the floating relic; no chest, vault, or altar entity exists.

#### Non-Binding Engineering Options
- **Option A (South Perimeter Ominous Vault — Recommended):**  
  Place an `ominous_vault` on the south perimeter altar pedestal at `(24, 65, 10)` configured with `loot_table: "moreminecrafts:chests/sky_islands/vault_tower_top"` and `key_item: "minecraft:ominous_trial_key"`.  
  *Pros:* Matches Q24 ("외곽 성역 제단"); creates high-stakes climax requiring players to bring an Ominous Key.  
  *Cons:* Players without Ominous Key cannot loot the pinnacle reward immediately.
- **Option B (South Perimeter Standard Vault):**  
  Place a standard `minecraft:vault` on the south perimeter altar configured with `vault_tower_top.json` requiring a standard `minecraft:trial_key`.  
  *Pros:* Guaranteed payout for any player who reaches the rooftop with a trial key.  
  *Cons:* Less distinct from lower floor vaults.
- **Option C (Relic Base Sanctuary Chest):**  
  Place a custom decorated stone chest at the base of the central floating relic `(24, 65, 24)` that unlocks when `allFloorsCleared()` is true.  
  *Pros:* Dramatic visual centerpiece; no key required.  
  *Cons:* Requires custom locking/unlocking Java logic and conflicts with Q38 (which stated the floating relic was purely decorative).

---

### Decision 3: Whole-Tower Completion Event & Player Experience
- **Affected Requirements:** `REQ-ROOF-03`, `REQ-CROSS-01`
- **Question for Owner:**  
  Should completing the Sky Tower trigger a player-facing completion event (sound, title broadcast, advancement), and what constitutes tower completion?

#### Context
- `SkyTowerProgress.allFloorsCleared()` exists in code (`completedMask == 0xFFFF`), but has no production callers. When the 16th spawner is defeated, Floor 4 barriers open, but no sound, title, or advancement triggers.
- The repository currently has zero advancements or quests referencing `sky_tower`.

#### Non-Binding Engineering Options
- **Option A (Local Sound & Title on Floor 4 Clear):**  
  When `allFloorsCleared()` becomes true, play `UI_TOAST_CHALLENGE_COMPLETE` and display a title (`"Sky Tower Conquered!"`) to all players inside the tower bounding box.  
  *Pros:* Simple, satisfying player feedback; zero external dependencies.  
  *Cons:* No persistent achievement in advancement log.
- **Option B (Advancement Integration):**  
  Add a custom Challenge advancement `moreminecrafts:adventure/conquer_sky_tower` awarded when `allFloorsCleared()` is true and player enters the rooftop bounding box (`Y >= 65`).  
  *Pros:* Integrated into vanilla advancement UI; awards XP and bragging rights.  
  *Cons:* Requires authoring advancement JSON and criterion trigger.
- **Option C (No Event Hook — Pure Sandbox Sandbox):**  
  Leave tower completion as purely physical (barriers removed, rooftop accessible); no artificial broadcasts or fanfare.  
  *Pros:* Matches vanilla trial chamber design (which has no overall chamber completion toast).  
  *Cons:* May feel anti-climactic after 4 floors of combat.

---

### Decision 4: Barrier Visual Representation
- **Affected Requirements:** `REQ-BARR-06`
- **Question for Owner:**  
  Should the stairway barrier doorways remain standard invisible `minecraft:barrier` blocks, or should they incorporate visual indicators (particles or translucent blocks)?

#### Context
- Approved Phase B implemented vanilla `minecraft:barrier` (18 blocks per floor). They are 100% invisible in survival mode.
- Q36 discussed cyan accent highlights in preliminary visual ideation.

#### Non-Binding Engineering Options
- **Option A (Keep Vanilla Barrier as Approved — Recommended):**  
  Maintain standard invisible `minecraft:barrier`.  
  *Pros:* 100% verified in C-4 GameTests; zero client rendering overhead; completely stable.  
  *Cons:* Invisible collision can initially surprise players until they notice the trial spawners.
- **Option B (Ambient Particle Border):**  
  When players approach an active barrier within 3 blocks, spawn subtle cyan electric spark particles (`minecraft:electric_spark`) along the doorway perimeter.  
  *Pros:* Keeps vanilla barrier physics while providing clear visual cue.  
  *Cons:* Requires minor server tick particle emission logic.
- **Option C (Translucent Cyan Stained Glass):**  
  Replace `minecraft:barrier` with `minecraft:cyan_stained_glass` placed and removed by `SkyTowerBarriers`.  
  *Pros:* Highly visible, immersive mystical forcefield appearance.  
  *Cons:* Survival players can theoretically break glass unless custom event cancellation is added.

---

### Decision 5: Mountain Biome Height Band Clamp
- **Affected Requirements:** `REQ-WGEN-03`
- **Question for Owner:**  
  How should the 97-block tower height budget be protected from truncating at Y=320 over extreme mountain terrain?

#### Context
- `sky_island_mountain_*.json` allows `start_height: uniform 18..70` above `WORLD_SURFACE_WG`. On high mountain peaks (Y >= 200), max height reaches Y=376, exceeding the Y=320 Overworld ceiling.

#### Non-Binding Engineering Options
- **Option A (Reduce Mountain start_height Max — Recommended):**  
  Adjust `start_height` in `sky_island_mountain_*.json` to `min: 15, max: 40`.  
  *Pros:* Pure JSON configuration change; guarantees island + tower stays <= Y=320 in 99.9% of terrain.  
  *Cons:* Mountain islands will float slightly closer to mountain peaks (15–40 blocks above surface).
- **Option B (Structure Start Height Clamp):**  
  Configure a maximum absolute height ceiling (e.g. `max_inclusive: { "absolute": 220 }`) in the structure definition.  
  *Pros:* Island never spawns above Y=220 regardless of mountain height.  
  *Cons:* If mountain peak is Y=230, island could generate embedded in or below the peak.

---

### Decision 6: Trial Spawner Mob Diversity Timing
- **Affected Requirements:** `REQ-SPAWN-06`
- **Question for Owner:**  
  Should the 16 Trial Spawners be diversified across floors using vanilla trial mob archetypes now, or maintained as all-zombie spawners until custom Sky Realm mobs are ready?

#### Context
- All 16 spawners currently spawn zombies (`trial_chamber/melee/zombie/normal`). Q07 deferred custom sky mobs.

#### Non-Binding Engineering Options
- **Option A (Diversify Vanilla Roles Now):**  
  Update `full_01.nbt` spawner configurations:  
  - Floor 1: Zombies (`trial_chamber/melee/zombie`)  
  - Floor 2: Skeletons (`trial_chamber/ranged/skeleton`)  
  - Floor 3: Spiders / Strays (`trial_chamber/small_melee/spider`)  
  - Floor 4: Mixed Zombies + Breezes (`trial_chamber/breeze`)  
  *Pros:* Drastically improves combat variety and gameplay engagement immediately.  
  *Cons:* Requires modifying `full_01.nbt` spawner NBT tags and re-running composite generation.
- **Option B (Maintain Zombie Baseline until Custom Roster — Recommended):**  
  Keep all-zombie baseline until the dedicated Sky Realm mob roster (Spirit of the Sky, Shulker, etc.) is ready for full integration.  
  *Pros:* Keeps C-4 candidate stable; avoids throwaway vanilla balancing.  
  *Cons:* Combat remains repetitive across floors in the interim.
