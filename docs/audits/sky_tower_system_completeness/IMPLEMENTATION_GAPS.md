# SKY TOWER AUDIT — IMPLEMENTATION GAPS

This document details all systems and features where **design authority was approved or resources were created**, but the runtime implementation is **absent, disconnected, or incomplete**.

---

### Implementation Gap 1: Sky Tower Vault Loot Table Linkage (`REQ-VAULT-02`)

#### 1. Authority & Specification
- **Authority:** `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 explicitly establishes:
  > *"하늘섬 금고 독립 보상: 돌풍 파편 5% ×1, 천공의 깃털 15% ×1... 기존 하늘섬 금고의 깃털 10% 또는 바닐라 시련 금고 돌풍 파편 지급 제안은 SUPERSEDED."*
- **Existing Asset:** `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault.json` was written specifically to satisfy this contract. It includes:
  - `moreminecrafts:sky_feather` (chance: 0.15, count: 1)
  - `moreminecrafts:gale_shard` (chance: 0.05, count: 1)
  - `moreminecrafts:chests/sky_islands/materials` (weight: 3)
  - Vanilla trial rewards (reward_rare, reward_common, reward_unique).

#### 2. The Implementation Defect
- In `src/main/resources/data/moreminecrafts/structure/sky_tower/full_01.nbt` (and all six generated composite templates `sky_islands/*/tower.nbt`), every single one of the 8 Vault block entities has its `config.loot_table` hardcoded to:
  `minecraft:chests/trial_chambers/reward`
- In `INTEGRATION-01` (`DECISION_REQUIRED.md`), the developer flagged this mismatch:
  > *"2. 기능 금고 설정: 승인 NBT 금고8개는 vanilla reward. 현재 권위의 하늘섬 금고는 moreminecrafts:chests/sky_islands/vault(깃털15%·돌풍 파편5%)."*
- In `INTEGRATION-02`, the developer chose to freeze the NBT SHA without updating the loot table or providing an injection hook.

#### 3. Gameplay Consequence
- Players conquering combat floors in the Sky Tower open vaults with Trial Keys and receive **only standard vanilla trial chamber items** (iron, emeralds, honey bottles, arrows).
- The Sky Tower yields **0% Sky Feathers and 0% Gale Shards**.
- This breaks the core Tier 2 progression loop: players cannot craft the **Wind Bow** (`wind_archers_bow.json`) or **Wind Sword** (`wind_sword.json`), and cannot craft the **Feather Charm** accessory.

---

### Implementation Gap 2: Rooftop Special Reward Vault Omission (`REQ-VAULT-04`)

#### 1. Authority & Specification
- **Authority:**
  - `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158 & §161: The rooftop of the aerial trial tower is the largest challenge area and combines a concentrated reward zone with elevated rare drop rates.
  - Q08: "각 층 일반 금고 + 최상층 특별 보상" (Standard vault on each floor + special reward on rooftop).
  - Q24: "최상층 특별 보상은 외곽 성역 제단" (Rooftop special reward is on the outer sanctuary altar).
- **Existing Asset:** `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault_tower_top.json` exists in data assets, containing:
  - `reward_unique` chance increased from 0.25 to 0.50 (double!).
  - `reward_rare` weight increased from 8 to 14.
  - `materials` weight increased from 3 to 6.
  - Sky Feather 15% and Gale Shard 5%.

#### 2. The Implementation Defect
- In `sky_tower/full_01.nbt`, the rooftop sanctuary (Y=64..79) contains **zero vaults, zero chests, and zero reward block entities**.
- The loot table `moreminecrafts:chests/sky_islands/vault_tower_top.json` is **not referenced by any placed block in the entire mod**. It exists solely as an orphaned JSON file and in unit/GameTest integrity assertions (`SkyIslandsResourceTest.java`, `SkyIslandsGameTests.java`).

#### 3. Gameplay Consequence
- Reaching the top of the tower leaves players stranded in an empty sanctuary with no reward for their conquest.

---

### Implementation Gap 3: Whole-Tower Completion Event Dead End (`REQ-ROOF-03`)

#### 1. Authority & Specification
- **Authority:** Q19 requires separate completion tracking for tower progression.
- **Implemented Code:**
  `src/main/java/dev/itzgamesgd/moreminecrafts/skytower/SkyTowerProgress.java`:
  ```java
  /** Combat fact only; sanctum arrival / overall-content completion is a separate undecided policy. */
  public boolean allFloorsCleared() { return completedMask == 0xffff; }
  ```

#### 2. The Implementation Defect
- A repository-wide code search (`grep -rn "allFloorsCleared" src/`) shows that `allFloorsCleared()` is referenced **only in `SkyTowerProgressTest.java`**:
  ```java
  src/main/java/.../SkyTowerProgress.java:27: public boolean allFloorsCleared() { ... }
  src/test/java/.../SkyTowerProgressTest.java:16: assertFalse(p.allFloorsCleared());
  src/test/java/.../SkyTowerProgressTest.java:22: assertTrue(restored.allFloorsCleared());
  ```
- It is **never called by any server tick, listener, mixin, command, or progression tracker**.

#### 3. Gameplay Consequence
- When players defeat the 16th Trial Spawner on Floor 4, the barrier on Floor 4 is removed. That is the end of the code execution.
- No completion message or title appears.
- No victory sound plays.
- No advancement criteria trigger.
- No entry in the player's knowledge or progression state is recorded.

---

### Implementation Gap 4: Silent Indefinite Registration Denial on Barrier Obstruction (`REQ-WGEN-07`)

#### 1. Implementation Defect Location
- `src/main/java/dev/itzgamesgd/moreminecrafts/skytower/SkyTowerWorldgenRegistration.java` lines 110–116:
  ```java
  for (int floor = 0; floor < 4; floor++) {
      for (int cell = 0; cell < 18; cell++) {
          var point = site.barrier(floor, cell);
          var pos = new BlockPos(point.x(), point.y(), point.z());
          if (!level.getBlockState(pos).isAir()) return false;
      }
  }
  ```
- And in `tick()`:
  ```java
  if (!allBoundsChunksLoaded(...) || !anchorsPresent(pending.level(), pending.site())) continue;
  ```

#### 2. Technical Failure Mode
- `anchorsPresent()` is intended to verify that the template placed successfully. However, it requires **every single one of the 72 barrier cells to be strictly AIR**.
- If a player enters the structure before all chunks load and places a torch, block, or water bucket in any doorway, or if snow falls into a doorway, or if an entity clips into a doorway block space:
  1. `anchorsPresent()` returns `false`.
  2. `tick()` executes `continue`, skipping registration.
  3. No warning log or error is emitted (it silently defers).
  4. The site remains trapped in `PENDING` indefinitely as long as that block is non-air.
  5. The tower is **never registered** in `SkyTowerData`.
  6. As a result, `SkyTowerCompletionObserver` ignores all spawners, `SkyTowerBarriers` installs zero barriers, and the tower's gameplay systems are permanently dead for that world instance.

---

### Implementation Gap 5: Weathering Pass Completely Omitted (`REQ-ARCH-06`)

#### 1. Authority & Specification
- **Authority:** Q69–Q72 establish a two-stage construction policy:
  - Stage 1: Build clean, geometrically sound temple structure and verify functionality.
  - Stage 2: Apply a separate weathering pass (cracked stone bricks, mossy stone bricks, cobblestone erosion, and environmental differentiation based on sun/rain exposure).

#### 2. The Implementation Defect
- In `FULL_BUILD_01_REPORT.md` line 21:
  > *"풍화 단계는 수행하지 않았다. 부유 유물 이외의 석재는 하나의 면 연결 구조로 묶었다."*
- The template was frozen in `16ad9d5` with clean stone bricks, smooth stone, and polished andesite.
- Stage 2 weathering was never performed. The structure generated in survival worlds has pristine, unweathered surfaces, contrary to the approved design theme ("풍화된 고대 원형 천공 신전탑").

---

### Implementation Gap 6: Trial Spawner Mob Diversity Omitted (`REQ-SPAWN-06`)

#### 1. Authority & Specification
- Vanilla trial chambers derive their gameplay variety from pairing diverse mob categories: melee, small melee, ranged, and Breezes.
- `SKY_ISLANDS_T2_CONTEXT` §160 intended for Sky Tower trial spawners to feature varied challenges.

#### 2. The Implementation Defect
- In `full_01.nbt`, all 16 spawners (4 floors × 4 altars) are configured with `trial_chamber/melee/zombie/normal` and `ominous`.
- Not a single spawner features skeletons, strays, cave spiders, silverfish, slimes, or Breezes.
- Combat is completely uniform across all 4 floors.

---

### Implementation Gap 7: Total Absence of Advancements and XP Quests (`REQ-CROSS-01`)

#### 1. Authority & Specification
- The project maintains an extensive advancement and quest system (`docs/CONTENT_AUTHORITY_INDEX.md`, `src/main/resources/data/moreminecrafts/advancement`, `xp_quest/catalog.json`).
- Major milestone structures (Ancient City, Colosseum, Pyramid) feature associated advancements or quest objectives.

#### 2. The Implementation Defect
- A comprehensive search of `src/main/resources/data/` reveals **zero advancements** and **zero XP quests** referencing `sky_tower`.
- Players discovering or completing the Sky Tower receive no progression recognition in the mod's advancement or quest trees.
