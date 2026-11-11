# SKY TOWER AUDIT — PRIORITIZED REMEDIATION BACKLOG

This document outlines the concrete, prioritized remediation tasks required to bring the Sky Tower from its current C-4 state to true **feature completeness**.

In strict accordance with `AGENTS.md` ("WHAT = Human, HOW = Agent"), tasks are categorized into:
- **Track 1: Engineering Work (Autonomous / No Design Decision Required)**
- **Track 2: Owner Decisions Required (Design Authority / WHAT Approval)**

---

### Track 1: Engineering Remediation (Autonomous / No New Design Needed)

The following items are implementation defects where explicit design authority already exists, and engineering can proceed autonomously within task scope.

#### Task E-1: Link Authoritative Loot Table to the 8 Combat Floor Vaults (`REQ-VAULT-02`) [CRITICAL]
- **Problem:** All 8 Vaults in `full_01.nbt` are hardcoded to vanilla `minecraft:chests/trial_chambers/reward`. `moreminecrafts:chests/sky_islands/vault.json` exists in data assets with the approved 15% Sky Feather and 5% Gale Shard rates, but is never used.
- **Action:** 
  1. Update `sky_tower/full_01.nbt` block entity data for the 8 vaults, setting `config.loot_table` to `"moreminecrafts:chests/sky_islands/vault"`.
  2. Re-run `scripts/structures/sky_islands/generate.py` to regenerate the six composite `tower.nbt` assets.
  3. Re-run `test_tower_composite.py` to verify deterministic composite output.
- **Authority Basis:** `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275.

#### Task E-2: Fix Silent Registration Denial in `SkyTowerWorldgenRegistration` (`REQ-WGEN-07`) [HIGH]
- **Problem:** `anchorsPresent()` requires all 72 barrier cells to be air. If a block, snow layer, or entity occupies a cell when chunks load, registration is skipped silently and indefinitely, permanently disabling tower progression.
- **Action:**
  1. Modify `SkyTowerWorldgenRegistration.anchorsPresent()` so that it asserts spawner and vault anchors, but handles barrier cells gracefully (logging a warning rather than permanently blocking registration).
  2. Allow `SkyTowerBarriers.reconcile()` to handle individual cell deferral without disabling structure registration.
  3. Add a dedicated unit test in `SkyTowerWorldgenRegistrationTest.java` verifying registration succeeds even if a barrier cell is temporarily obstructed.

#### Task E-3: Cap Mountain Structure Height to Prevent World Ceiling Truncation (`REQ-WGEN-03`) [HIGH]
- **Problem:** In `sky_island_mountain_*.json`, `start_height: 18..70` above mountain surface can cause the 97-block composite structure to exceed Y=320 over peaks >= Y=180.
- **Action:**
  1. Adjust `start_height.max_inclusive.absolute` from `70` to `45` in `sky_island_mountain_snowy.json`, `sky_island_mountain_rocky.json`, and `sky_island_mountain_meadow.json`.
  2. Verify that `max_surface (200) + start_height (45) + tower_height (70) <= 315`, leaving a safe buffer below Y=320.

#### Task E-4: Complete Live Combat GameTests for Floors 1–3 (`REQ-SPAWN-04`) [MEDIUM]
- **Problem:** Real survival combat GameTests exist only for Floor 0; Floors 1–3 were tested via synthetic state injection.
- **Action:**
  1. Extend `SkyTowerGameTests.java` to execute real survival combat on Floors 1, 2, and 3, asserting floor-by-floor barrier clearing.

---

### Track 2: Owner Decisions Required (Design Authority Approval Needed)

The following items cannot be implemented autonomously because they require new design choices, gameplay rules, or visual approvals.

#### Decision O-1: Rooftop Special Reward Specification (`REQ-VAULT-04`) [CRITICAL]
- **Choice Needed:**
  - Container block type: `minecraft:vault`, `minecraft:ominous_vault`, `minecraft:chest`, or custom altar?
  - Key requirement: standard `trial_key`, `ominous_trial_key`, or keyless upon clearing all floors?
  - Exact placement: south perimeter altar (`24, 65, 10`) or central pedestal?
- **Engineering Recommendation:** Place a `minecraft:vault` on the south perimeter altar at `(24, 65, 10)` referencing `moreminecrafts:chests/sky_islands/vault_tower_top.json`, requiring a `minecraft:trial_key`.

#### Decision O-2: Whole-Tower Completion Event & Player Notification (`REQ-ROOF-03`) [HIGH]
- **Choice Needed:**
  - What feedback plays when a player clears all 4 floors or enters the rooftop?
  - Should `SkyTowerProgress.allFloorsCleared()` trigger a title broadcast, sound, or advancement?
- **Engineering Recommendation:** Hook `allFloorsCleared()` in `SkyTowerBarriers.reconcile()`. When true, trigger a localized title announcement and award a new advancement `moreminecrafts:adventure/conquer_sky_tower`.

#### Decision O-3: Barrier Visual Feedback (`REQ-BARR-06`) [HIGH]
- **Choice Needed:**
  - Invisible `minecraft:barrier` + ambient cyan particle emitters?
  - Translucent `minecraft:cyan_stained_glass` placed and removed dynamically?
  - Custom `moreminecrafts:temple_barrier` block?
- **Engineering Recommendation:** Use `minecraft:cyan_stained_glass` (or invisible barrier with ambient electric spark particles) to make the barrier visible and thematic for survival players.

#### Decision O-4: Interim Spawner Mob Diversification (`REQ-SPAWN-06`) [MEDIUM]
- **Choice Needed:**
  - Should the 16 spawners remain 100% zombies until the custom sky mob roster merges, or should vanilla archetypes be distributed across floors (Floor 1: Zombies, Floor 2: Skeletons, Floor 3: Strays/Spiders, Floor 4: Zombies + Breezes)?
- **Engineering Recommendation:** Distribute vanilla mob archetypes across floors now; swap to custom sky mobs in the future mob integration task.

#### Decision O-5: Ratify FULL BUILD 01 Architectural Baseline (`REQ-ARCH-01`, `REQ-ARCH-02`) [LOW]
- **Choice Needed:**
  - Formally approve the geometric adaptations made in FULL BUILD 01 (outer diameter center D50.16, central pattern D11, north/south balcony symmetry, exposed structural ceilings) as permanent design authority in `CONTENT_AUTHORITY_INDEX.md`.

---

### Prioritized Remediation Roadmap

```mermaid
flowchart TD
    subgraph Phase 1: Critical Gameplay Repair [Immediate - Engineering]
        E1["E-1: Link vault.json to 8 Vaults in NBT"]
        E2["E-2: Fix silent registration denial in WorldgenRegistration"]
        E3["E-3: Cap mountain start_height to prevent Y>320 truncation"]
    end

    subgraph Phase 2: Owner Design Resolutions [Decisions Required]
        O1["O-1: Approve rooftop reward container & placement"]
        O2["O-2: Approve completion notification & advancement"]
        O3["O-3: Approve barrier visual representation"]
        O4["O-4: Approve spawner mob diversification"]
    end

    subgraph Phase 3: Integration & Final Polish
        I1["Apply rooftop vault & mob configs to NBT"]
        I2["Hook allFloorsCleared to Title & Advancement"]
        I3["Apply barrier visual feedback"]
        I4["Run full live client walkthrough QA"]
    end

    E1 --> Phase 2
    E2 --> Phase 2
    E3 --> Phase 2
    Phase 2 --> I1
    Phase 2 --> I2
    Phase 2 --> I3
    I1 --> I4
    I2 --> I4
    I3 --> I4
```
