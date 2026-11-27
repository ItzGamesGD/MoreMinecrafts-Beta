# SKY TOWER — AUTHORITY RESOLUTION RECORD
## Chronological Authority Reconstruction & Document Precedence

**Date:** 2026-11-09  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Target Branch:** `feature/sky-tower-integration-02`  
**Integration Baseline (C-4):** `fdaa7effe96746ffe5e6e4454acc916cba277ff4`  

---

## 1. Hierarchy of Document Precedence

In strict compliance with `AGENTS.md` §2 ("Authority"), document precedence in the MoreMinecrafts repository is determined by explicit human approval and chronological succession:

```
[Level 1: Explicit Human Directives]
   ▲  - Direct conversation instructions & design checkpoints explicitly approved by user
   │
[Level 2: Current Design Authority Index]
   │  - docs/CONTENT_AUTHORITY_INDEX.md
   │
[Level 3: Approved Design Decision Records]
   │  - docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md (Q01-Q118)
   │
[Level 4: Approved Artifact Candidates]
   │  - full_01.nbt (commit 16ad9d5) as accepted in FULL BUILD 01 & INTEGRATION-01
   │
[Level 5: Integration Checkpoint Reports & Plans]
   │  - docs/planning/sky_tower_integration_02/ (C-1 through C-4 reports)
   │
[Level 6: Context, Ideation & Historical Documents]
      - docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md (Background T2 specification)
```

> **Principle of Artifact Ratification:**  
> When the owner approves advancing a physical voxel structure candidate (`full_01.nbt`) through multiple integration stages (FULL BUILD 01 -> INTEGRATION-01 -> C-1 -> C-2 -> C-3 -> C-4), physical dimensions and geometric adaptations documented in the candidate report (`DESIGN_CHANGES.md`) supersede prior theoretical numbers (e.g. preliminary diameter estimates in Q01-Q41), provided those adaptations were required to satisfy hard physical voxel clearance constraints.

---

## 2. Chronological Authority Trail

### Stage 1: Initial Sky Islands Tier 2 Context (2026-10-27)
- **Document:** `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md`
- **Key Clauses:**
  - §158–§161: Sky Tower conceived as the central pinnacle challenge of the Sky Islands dimension. 4 combat floors with trial spawners, culminating in an open rooftop sanctuary with concentrated rewards and elevated rare loot rates.
  - §275: Defined the core progression economy:
    - Sky Feather: 15% drop rate (×1).
    - Gale Shard: 5% drop rate (×1).
    - Sky Materials: Mythril, adamantium, cloud ingots.
    - Explicitly superseded older proposals (10% feather, vanilla drops).
  - Authored loot tables: `moreminecrafts:chests/sky_islands/vault.json` and `vault_tower_top.json`.

### Stage 2: Master Design Decisions Q01–Q118 (2026-11-05)
- **Document:** `docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md`
- **Owner Approved Decisions:**
  - **Q01–Q04:** Circular temple tower; 4 combat floors + base + rooftop; 4-block wide continuous exterior spiral stairs; 72 doorway barrier blocks (18 per floor).
  - **Q07:** Preserve vanilla Trial Spawner rules, player scaling, and wave mechanics. Prohibited custom mob managers and unapproved custom mob pools until dedicated sky mobs are implemented.
  - **Q08 & Q24:** 8 standard vaults embedded in arch alcoves (2 per floor); rooftop sanctuary features a special pinnacle reward on the outer altar.
  - **Q19–Q22:** Irreversible floor clearing quorum (4 spawners); spawner destruction counts toward quorum to prevent softlocks; non-destructive barrier removal; vanilla survival interactions (flight, pearls, building) permitted.
  - **Q25–Q43:** Preliminary geometry specifications (D45 outer diameter, D17 central mosaic plaza, 12 rooftop arches, central floating relic purely decorative).
  - **Q69–Q72:** Two-stage build process: Stage 1 clean structural geometry, Stage 2 weathering pass (cracked/mossy stone bricks).

### Stage 3: FULL BUILD 01 Candidate Construction (2026-11-06)
- **Documents:** `docs/planning/sky_tower_full_build_01/DESIGN_CHANGES.md`, `FULL_BUILD_01_REPORT.md`, candidate NBT `full_01.nbt` (`16ad9d5`).
- **Engineering Adaptations:**
  - **Outer Diameter (D45 -> D50.16):** With D45, placing a 4-block wide exterior staircase, 3x3 perimeter pillars, and a 26-block interior arena resulted in physical voxel clipping. The main body was adjusted to center D50.16 (D51 outer boundary).
  - **Central Mosaic Plaza (D17 -> D11):** Scaled down from D17 to D11 to preserve a 14-block clear detection and mob movement perimeter around the 4 Trial Spawners.
  - **Balconies (Spiral Offset -> North/South Symmetry):** Mirrored north/south to align directly over structural piers and stair landings.
  - **Vault Blocks:** Placed 8 vanilla `minecraft:vault` blocks with `minecraft:chests/trial_chambers/reward`.
  - **Rooftop Rewards:** Builder omitted placing a physical chest or vault entity on the rooftop, leaving the sanctuary open with only the floating relic.
  - **Weathering Pass:** Report line 21 explicitly stated: *"풍화 단계는 수행하지 않았다"* (Weathering pass was not performed).
- **Owner Action:** The user reviewed and approved FULL BUILD 01 as the candidate baseline for integration.

### Stage 4: INTEGRATION-01 Architecture (2026-11-07)
- **Document:** `docs/planning/sky_tower_integration_01/ARCHITECTURE.md`, `DECISION_RECORD.md`
- **Architecture Established:**
  - Non-destructive `SkyTowerBarriers` lifecycle using vanilla `minecraft:barrier`.
  - Bitmask tracking (`SkyTowerProgress`) for spawner completions and destructions.
  - `SkyTowerData` SavedData persistence with quarantine protection.
  - Developer flagged in `DECISION_REQUIRED.md`: 8 vaults in NBT use vanilla trial reward, whereas §275 specifies `sky_islands/vault`.
  - Developer also noted rooftop reward and whole-tower completion event were deferred pending owner direction.

### Stage 5: INTEGRATION-02 Implementation & Verification C-1 through C-4 (2026-11-08 ~ 2026-11-09)
- **Checkpoints Verified:**
  - **C-1:** Jigsaw composite structure generation replacing legacy multi-piece tower.
  - **C-2:** Island bridge connection and entrance walkability (BFS swept collision).
  - **C-3:** Chunk load auto-registration (`SkyTowerWorldgenRegistration`) verified across all 6 biomes on dedicated server.
  - **C-4:** Complete GameTest combat suite (single player, 2-player scaling, spawner destruction quorum, waterlogged barrier preservation, server restart persistence).
- **Freezing Policy:** In `INTEGRATION_02_REPORT.md` line 11, the team explicitly preserved vanilla vault NBT to maintain SHA immutability of `full_01.nbt` (`16ad9d5`), isolating the technical integration from balance changes.

---

## 3. Authority Reconciliation on Key Topics

### 1. Vault Loot Tables: Q07 vs. Context §275
- **Status:** **AUTHORITY CONFLICT (DEFERRED PENDING OWNER RECONCILIATION)**
- **Reconciliation:**
  - `SKY_ISLANDS_T2_CONTEXT` §275 mandates custom loot (`moreminecrafts:chests/sky_islands/vault`) for Sky Feathers and Gale Shards.
  - Q07 mandates preserving vanilla trial spawner and vault rewards until custom pools are formally agreed upon.
  - INTEGRATION-02 preserved vanilla NBT to stabilize C-1..C-4 testing.
  - **Current Rule:** The implementation reflects the Q07 / candidate freeze. Reconciling this conflict requires an explicit owner decision to either update `full_01.nbt` to `sky_islands/vault` or implement runtime loot table injection.

### 2. Physical Dimensions: Q03/Q25 vs. FULL BUILD 01
- **Status:** **SUPERSEDED BY ARTIFACT APPROVAL**
- **Reconciliation:**
  - D45, D17 center mosaic, and spiral-offset balconies were preliminary design targets.
  - During physical block placement, geometric conflicts forced adaptations to center D50.16, D11 center mosaic, and N/S symmetrical balconies.
  - When the owner approved FULL BUILD 01 and authorized C-1..C-4 integration against `16ad9d5`, the physical candidate was formally accepted.
  - **Current Rule:** The physical dimensions in `full_01.nbt` are authoritative. The theoretical targets in Q01–Q41 are superseded.

### 3. Rooftop Special Reward: Q08/Q24/§161 vs. Empty Rooftop
- **Status:** **CONFIRMED IMPLEMENTATION & DESIGN GAP**
- **Reconciliation:**
  - Context §161 and Q08/Q24 consistently mandate a rooftop special reward.
  - Omission in `full_01.nbt` was an incomplete implementation during FULL BUILD 01, which was deferred during C-1..C-4 to avoid inventing container types.
  - **Current Rule:** The rooftop reward remains an active, unfulfilled requirement. Owner decision is required to specify container type and coordinates.

### 4. Barrier Visual Indicator: Q36 vs. Phase B Vanilla Barrier
- **Status:** **APPROVED BASELINE WITH OPTIONAL DESIGN ENHANCEMENT**
- **Reconciliation:**
  - Q36 discussed cyan accent highlights during visual ideation.
  - Phase B of INTEGRATION-01/02 explicitly selected and implemented vanilla `minecraft:barrier` to ensure reliable, zero-dependency collision.
  - **Current Rule:** The vanilla invisible barrier is the approved technical baseline. Adding ambient particles or translucent glass is an aesthetic enhancement that does not invalidate C-4.

### 5. Whole-Tower Completion Event: Q19 vs. Production Absence
- **Status:** **DESIGN INCOMPLETE (FALSE POSITIVE DEFECT)**
- **Reconciliation:**
  - Q19 required completion tracking in data structures (satisfied by `SkyTowerProgress` bitmask).
  - No document ever approved a specific player-facing completion event (title, sound, or advancement).
  - `SkyTowerProgress.allFloorsCleared()` was implemented as an internal query helper.
  - **Current Rule:** Whole-tower completion behavior was never designed. It is a design gap, not a broken implementation.

---

## 4. Master Authority Precedence Summary Table

| Requirement Area | Old Context (2026-10-27) | Approved Decisions (2026-11-05) | Candidate Artifact (FULL BUILD 01) | Current Surviving Authority |
|---|---|---|---|---|
| **Outer Diameter** | D45 (preliminary) | D45 (Q03/Q25) | Center D50.16 (D51 bounding) | **Center D50.16 (Candidate Ratified)** |
| **Central Mosaic** | Undefined | D17 (Q42) | D11 (14-block clear radius) | **D11 (Candidate Ratified)** |
| **Balconies** | Undefined | Spiral Offset (Q73) | Symmetrical North/South | **North/South Symmetrical (Ratified)** |
| **Vault Loot** | `sky_islands/vault` (§275) | Vanilla Rewards (Q07) | Vanilla `trial_chambers/reward` | **AUTHORITY CONFLICT (Requires Decision)** |
| **Rooftop Reward** | `vault_tower_top` (§161) | Special Reward Altar (Q08/Q24) | Omitted (Empty Rooftop) | **REQUIRED (Container Undecided)** |
| **Barriers** | Undefined | Cyan Accents (Q36) | Vanilla `minecraft:barrier` | **Vanilla Barrier Approved Baseline** |
| **Spawner Mobs** | Varied Trial Archetypes | Vanilla Baseline / No Custom Pools (Q07) | 100% Melee Zombies | **Zombie Baseline Approved; Diversity Deferred** |
| **Weathering** | Graded Erosion | Stage 2 Pass (Q69-Q72) | Omitted (Clean Stone Brick) | **Approved Stage 2 Backlog** |
| **Advancement** | Undefined | Not requested | None | **NOT REQUIRED (Out of Scope)** |
