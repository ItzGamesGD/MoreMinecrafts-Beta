# SKY TOWER — CONFIRMED IMPLEMENTATION GAPS
## Verified Technical & Asset Deficiencies (Purged of False Positives)

**Date:** 2026-11-09  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Target Branch:** `feature/sky-tower-integration-02`  
**Integration Baseline (C-4):** `fdaa7effe96746ffe5e6e4454acc916cba277ff4`  

---

## 1. Overview & Triage Methodology

This document isolates the **genuine, evidence-backed implementation gaps** identified during the independent revalidation. All speculative claims, false positives, and unmandated feature requests from the preliminary audit have been filtered out:

- **Purged:** Entity collision causing registration failure (disproved by block state semantics).
- **Purged:** Missing `allFloorsCleared()` caller as a defect (disproved; feature was never designed).
- **Purged:** Missing Advancements / XP Quests (disproved; never mandated by design authority).
- **Purged:** Geometry outer diameter mismatch (superseded by candidate artifact approval).

The remaining confirmed gaps below represent real deficiencies in assets, configuration, or code that require engineering or owner resolution.

---

## 2. Inventory of Confirmed Gaps

### GAP-01: Sky Tower Vault Loot Table Disconnection
- **Requirement ID:** `REQ-VAULT-02`
- **Severity:** **CRITICAL** (Core Progression Economy Blocker)
- **Classification:** Implementation Defect resulting from Authority Conflict
- **File References:**
  - `src/main/resources/data/moreminecrafts/structure/sky_tower/full_01.nbt`
  - `src/main/resources/data/moreminecrafts/structure/sky_islands/*/tower.nbt` (all 6 biomes)
  - `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault.json`
- **Observed State:**
  - In `full_01.nbt`, all 8 Vault block entities (anchors at `(16, floorY+1, 15|33)` across floors 0..3) have their NBT `config.loot_table` property explicitly set to:
    `minecraft:chests/trial_chambers/reward`
  - The mod's authoritative Tier 2 loot table `moreminecrafts:chests/sky_islands/vault.json` contains:
    - `moreminecrafts:sky_feather` (weight/chance: 0.15 ×1)
    - `moreminecrafts:gale_shard` (weight/chance: 0.05 ×1)
    - `moreminecrafts:chests/sky_islands/materials` (weight: 3)
  - This loot table is completely unreferenced by any placed block in the Sky Tower.
- **Gameplay Impact:**
  - Defeating trial spawners and opening vaults yields vanilla copper, iron, emeralds, and food.
  - The tower yields **0% Sky Feathers and 0% Gale Shards**.
  - Players are completely unable to craft Tier 2 items (**Wind Bow**, **Wind Sword**, and **Feather Charm**), severing the mod's progression chain.

---

### GAP-02: Rooftop Sanctuary Special Reward Container Omission
- **Requirement ID:** `REQ-VAULT-04`
- **Severity:** **CRITICAL** (Pinnacle Progression Blocker)
- **Classification:** Missing Asset / Design Incomplete
- **File References:**
  - `src/main/resources/data/moreminecrafts/structure/sky_tower/full_01.nbt`
  - `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault_tower_top.json`
- **Observed State:**
  - Inspection of `full_01.nbt` reveals that within the rooftop sanctuary bounding box (`Y = 64..79`), there is **zero reward container**. No `minecraft:vault`, no `minecraft:chest`, and no interactive altar block entity exists.
  - The custom loot table `moreminecrafts:chests/sky_islands/vault_tower_top.json` exists in data assets (featuring doubled rare weights and elevated material drops), but is completely orphaned.
- **Gameplay Impact:**
  - Conquering all 4 combat floors and reaching the top of the tower provides a dramatic visual vista (12 open arches, floating relic), but **zero gameplay reward or loot**.

---

### GAP-03: Mountain Biome Height Band Ceiling Truncation Risk
- **Requirement ID:** `REQ-WGEN-03`
- **Severity:** **MEDIUM** (Worldgen Edge-Case Risk)
- **Classification:** Worldgen Configuration Defect
- **File References:**
  - `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_meadow.json`
  - `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_rocky.json`
  - `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_snowy.json`
- **Observed State:**
  - All three mountain island structure definitions configure:
    ```json
    "start_height": {
      "type": "minecraft:uniform",
      "min_inclusive": { "absolute": 18 },
      "max_inclusive": { "absolute": 70 }
    },
    "project_start_to_heightmap": "WORLD_SURFACE_WG"
    ```
  - The composite tower structure is 97 blocks high (Y=0..96).
  - Mountain biomes in Minecraft 1.21 frequently generate mountain surfaces between Y=180 and Y=230.
  - When `start_height` rolls near 70 over surface Y=210, structure start is placed at Y=280.
  - The tower top reaches Y = 280 + 96 = Y=376, exceeding the Overworld build height limit of Y=320 by 56 blocks.
- **Gameplay Impact:**
  - Jigsaw structure generation either truncates blocks above Y=320 or fails piece placement, leading to sliced or incomplete towers in mountain biomes.

---

### GAP-04: Architectural Stage 2 Weathering Pass Omission
- **Requirement ID:** `REQ-ARCH-06`
- **Severity:** **LOW / MEDIUM** (Visual Aesthetic Defect)
- **Classification:** Incomplete Implementation of Approved Two-Stage Process
- **File References:**
  - `src/main/resources/data/moreminecrafts/structure/sky_tower/full_01.nbt`
  - `docs/planning/sky_tower_full_build_01/FULL_BUILD_01_REPORT.md` line 21
- **Observed State:**
  - Q69–Q72 established a two-stage construction plan where Stage 1 focused on structural geometry and Stage 2 was a dedicated weathering pass applying cracked stone bricks, mossy stone bricks, cobblestone erosion, and environmental decay.
  - `FULL_BUILD_01_REPORT.md` line 21 explicitly confirms: *"풍화 단계는 수행하지 않았다."*
  - The structure generated in survival worlds uses pristine, unweathered stone bricks, smooth stone, and polished andesite throughout.
- **Gameplay Impact:**
  - The structure does not visually convey its lore as an ancient, weathered temple tower exposed to sky winds for centuries. Collision and gameplay are unaffected.

---

### GAP-05: Registration Preflight Doorway Air Check Fragility
- **Requirement ID:** `REQ-WGEN-07`
- **Severity:** **LOW** (Edge-Case Robustness Defect)
- **Classification:** Implementation Fragility
- **File References:**
  - `src/main/java/dev/itzgamesgd/moreminecrafts/skytower/SkyTowerWorldgenRegistration.java` lines 110–116
- **Observed State:**
  - `anchorsPresent()` requires all 72 barrier doorway cells to be strictly air (`!level.getBlockState(pos).isAir() return false;`).
  - While entities do not trigger this condition and `tick()` actively retries every server tick, if an external block (e.g., falling snow in Snowy Mountain biome, or a player-placed block before chunk registration) occupies any doorway cell, registration is deferred until that block is cleared.
- **Gameplay Impact:**
  - In Snowy biomes, if snow accumulates in an exposed entrance doorway before chunk registration runs, the site remains pending until a player or entity clears the snow.
