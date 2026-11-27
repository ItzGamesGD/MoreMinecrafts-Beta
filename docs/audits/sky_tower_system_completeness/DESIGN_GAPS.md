# SKY TOWER AUDIT — DESIGN GAPS & REQUIRED DECISIONS

This document catalogs every system, rule, or contract in the Sky Tower that remains **undesigned, ambiguous, or unfinalized**, requiring human design authority to resolve.

In accordance with `AGENTS.md` ("WHAT = Human, HOW = Agent"), agents must **never invent** gameplay rules or balance values. For each gap below, the required decision is formulated with non-binding engineering options.

---

### Gap 1: Rooftop Special Reward Specification (`REQ-VAULT-04`)

#### 1. Context & Authority Conflict
- **Original Authority:** 
  - `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158 & §161: "시련탑 최상층은 일반 층보다 상자·금고 수량과 깃털 이외 희귀 보상 가중치 증가(B안)."
  - `docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q08 ("각 층 일반 금고 + 최상층 특별 보상") and Q24 ("최상층 특별 보상은 외곽 성역 제단").
  - Data asset `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault_tower_top.json` exists with elevated rare loot weights and unique drops.
- **Current State:**
  - `sky_tower/full_01.nbt` contains **no vault, chest, or reward block entity on the rooftop**.
  - In `INTEGRATION-01/02`, the rooftop reward was labeled "보류" (deferred) to avoid inventing block types or altar positions.

#### 2. Exact Decision Required from Owner
1. **Reward Container Type:** What block represents the rooftop reward?
   - A vanilla `minecraft:vault`?
   - An `ominous_vault` requiring an Ominous Trial Key?
   - A custom loot chest (`minecraft:chest` with custom loot table)?
   - An interactive altar block entity?
2. **Placement Coordinates:** Where in the rooftop sanctuary should it be placed?
   - On one of the outer perimeter altar pedestals (Q24)?
   - At the base of the central floating relic (Q38 originally excluded the center as reward)?
   - Multiple chests/vaults (as suggested in §158)?
3. **Key / Requirement:** What is required to open it?
   - Standard `trial_key`?
   - `ominous_trial_key`?
   - Automatic unlocking upon `allFloorsCleared()`?

#### 3. Non-Binding Engineering Options
- **Option A (Minimal Intervention, Consistent with Data Pack):** Place an `ominous_vault` or standard `vault` on the south perimeter altar at `(24, 65, 10)` configured with `loot_table: "moreminecrafts:chests/sky_islands/vault_tower_top"` and `key_item: "minecraft:trial_key"`.
- **Option B (Pinnacle Ominous Challenge):** Place an `ominous_vault` configured with `vault_tower_top.json` requiring an `ominous_trial_key`, incentivizing players to run the tower in Ominous mode.
- **Option C (Altar Chest):** Place a decorated stone pedestal with a `chest` locked until all 4 floors are cleared.

---

### Gap 2: Whole-Tower Completion Event & Player Notification (`REQ-ROOF-03`, `REQ-CROSS-01`)

#### 1. Context & Authority Conflict
- **Original Authority:** 
  - Q19 requires separate tracking of tower completion.
  - `SkyTowerProgress.allFloorsCleared()` was implemented in Java to return true when all 16 spawners (bits 0..15) are set.
- **Current State:**
  - `allFloorsCleared()` is called **only** in unit tests.
  - Defeating the 16th spawner triggers barrier removal on floor 4, but **no event hook, sound, message, title, advancement, or quest grant** is triggered.
  - The mod has no advancement or quest referencing the Sky Tower in `advancement/` or `xp_quest/catalog.json`.

#### 2. Exact Decision Required from Owner
1. **Trigger Condition:** Is whole-tower completion defined as:
   - Defeating all 16 Trial Spawners across all 4 floors (`allFloorsCleared() == true`)?
   - Stepping into the rooftop sanctuary bounding box (Y=64..79) after all floors are cleared?
   - Opening the rooftop special vault/reward?
2. **Player Experience & Feedback:** What should players experience upon completion?
   - Server-wide or local player broadcast / title display?
   - Sound effect (e.g., `ui.toast.challenge_complete` or raid horn)?
   - Particle effect at the central floating relic?
3. **Advancement & XP Quest Mapping:**
   - Should a dedicated vanilla-style advancement be awarded (e.g., `moreminecrafts:adventure/conquer_sky_tower`)?
   - Should it grant XP Quest progress in `xp_quest/catalog.json`?

#### 3. Non-Binding Engineering Options
- **Option A (Lightweight Event Hook):** When `allFloorsCleared()` transitions to true in `SkyTowerBarriers.reconcile()`, play `UI_TOAST_CHALLENGE_COMPLETE` and display a localized Title to all players within the tower bounding box.
- **Option B (Advancement Integration):** Add a custom criterion trigger `moreminecrafts:sky_tower_conquered` triggered on tower completion, granting a Challenge advancement with XP reward.

---

### Gap 3: Physical Barrier Visual & Auditory Feedback (`REQ-BARR-06`)

#### 1. Context & Authority Conflict
- **Original Authority:**
  - Q02: "신전 결계로 정상 이동 경로 차단" (Temple barrier blocks normal path).
  - Q36: "따뜻한 기본 조명 + 결계·주요 아치·제단 청록색 강조" (Cyan accent highlighting for barriers).
- **Current State:**
  - `SkyTowerBarriers` places 18 `minecraft:barrier` blocks per floor.
  - In survival mode, `minecraft:barrier` is 100% invisible with zero particles and zero sound.
  - Players walk into an invisible wall with no in-game explanation.

#### 2. Exact Decision Required from Owner
1. **Visual Representation:** How should the barrier be visible to survival players?
   - Invisible barrier block + continuous ambient particles (e.g., `sculk_charge_pop` or cyan `portal` particles)?
   - Custom mod block (e.g., translucent cyan barrier / forcefield block)?
   - Vanilla translucent block (e.g., cyan stained glass or tinted glass) placed by `SkyTowerBarriers` and removed on floor clear?
2. **Feedback on Impact / Clear:** Should a sound or particle burst play when a player bumps into the barrier or when the barrier dissolves?

#### 3. Non-Binding Engineering Options
- **Option A (Zero New Blocks — Particle Emitter / Server Particle):** When a player is within 3 blocks of an active barrier wall, `SkyTowerBarriers.tick` spawns subtle cyan particles (`minecraft:electric_spark` or `minecraft:soul_fire_flame`) along the 6×3 doorway grid.
- **Option B (Translucent Stained Glass):** Use `minecraft:cyan_stained_glass` instead of `minecraft:barrier`. It is visually prominent, immersive, matches Q10/Q36 cyan themes, allows seeing through to the staircase, and drops nothing when cleared by the server.
- **Option C (Custom Barrier Block):** Introduce `moreminecrafts:temple_barrier` (unbreakable in survival, translucent animated texture, dissolves with sound on floor clear).

---

### Gap 4: Trial Spawner Mob Role Diversification (`REQ-SPAWN-06`)

#### 1. Context & Authority Conflict
- **Original Authority:**
  - `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §160: Retain vanilla trial spawner player-scaling and mob mechanics; map vanilla mob archetypes to the tower.
  - Q07: Retain vanilla trial spawner rules and rewards; custom wave manager prohibited; custom sky mobs deferred.
- **Current State:**
  - All 16 Trial Spawners in `full_01.nbt` are hardcoded to `minecraft:trial_chamber/melee/zombie/normal` and `ominous`.
  - Every single combat encounter on all 4 floors is identical: standard vanilla zombies.
  - No ranged mobs (skeletons/strays), no small melee (spiders/slimes), and no Breezes exist in the tower.

#### 2. Exact Decision Required from Owner
1. **Floor-by-Floor Spawner Archetypes:** While custom sky mob pools are deferred, should the 16 spawners use diverse vanilla spawner configurations across floors?
   - Floor 1: Melee (Zombies / Husks)
   - Floor 2: Small Melee & Ranged (Cave Spiders / Skeletons)
   - Floor 3: Ranged & Cold (Strays / Spiders)
   - Floor 4: Elite / Mixed (Zombies + Skeletons + Breezes)
2. **Breeze Inclusion:** Should the Sky Tower include Breeze spawners (`minecraft:trial_chamber/breeze/normal`)?

#### 3. Non-Binding Engineering Options
- **Option A (Progressive Vanilla Combat):** Update `full_01.nbt` spawner NBT tags so that Floor 1 has Zombies, Floor 2 has Skeletons, Floor 3 has Spiders/Strays, and Floor 4 has a combination of Zombies and Breezes.
- **Option B (Maintain Zombie Baseline until Custom Roster):** Keep current zombie spawners until the full custom Sky Realm mob roster (Spirit of the Sky, Shulker, etc.) is implemented.

---

### Gap 5: Mountain Worldgen Height Band & Ceiling Protection (`REQ-WGEN-03`)

#### 1. Context & Authority Conflict
- **Structure Height:** Composite structure height is 97 blocks (Y=0..96).
- **Worldgen Config:** `sky_island_mountain_*.json` structures configure `start_height: 18..70` above `WORLD_SURFACE_WG`.
- **Current State:**
  - Over standard terrain (surface Y=64..100), the tower generates comfortably between Y=120 and Y=270.
  - Over extreme mountain peaks (surface Y=180..240), an island spawned at surface + 70 reaches Y=250..310. A 97-block tower will exceed Y=320, resulting in the top 20–50 blocks (including the sanctuary) being truncated by vanilla worldgen.
  - In C-3 and C-4, natural generation was verified only on flat worlds or low-altitude seeds.

#### 2. Exact Decision Required from Owner
- Should the `start_height` max parameter for mountain sky island structures be capped so that `start_height.max + surface + 97 <= 315`?
- Or should the structure definition enforce a maximum Y clamp?

#### 3. Non-Binding Engineering Options
- **Option A (Config Tuning):** Adjust `sky_island_mountain_*.json` `start_height` from `18..70` to `18..45` or set a fixed max Y clamp.
- **Option B (Feature Placement Filter):** Add a height check in structure placement to prevent spawning above Y=210.

---

### Gap 6: Durable Baseline Authority Synchronization (`REQ-ARCH-01`, `REQ-ARCH-02`, `REQ-ARCH-04`)

#### 1. Context & Authority Conflict
- In FULL BUILD 01 (`docs/planning/sky_tower_full_build_01/DESIGN_CHANGES.md`), several geometric adaptations were made to resolve physical clearance conflicts:
  - Tower body outer diameter expanded from D45 to center D50.16 (D51 boundary).
  - Central floor pattern scaled from D17 to D11.
  - Balconies shifted from spiral offset to north/south bilateral symmetry.
  - Ceilings changed from full vaulted arches to exposed flat structural slabs.
  - Lighting shifted from warm to cool white/cyan.
- The document explicitly noted: *"Q01–Q118 원장과 P1–P6를 수정하지 않는다... 최종 시각 디자인 승인은 아니다."*
- These adaptations have remained in provisional working status without formal synchronization into current design authority (`AGENTS.md` §2).

#### 2. Exact Decision Required from Owner
- Formally ratify or amend the FULL BUILD 01 geometric adaptations into `docs/CONTENT_AUTHORITY_INDEX.md` and the master design record, establishing D50.16, D11 center pattern, and N/S balconies as permanent design authority.
