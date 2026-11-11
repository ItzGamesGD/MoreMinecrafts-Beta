# SKY TOWER AUDIT — VERIFICATION GAPS

This document identifies all systems where **code exists**, but **adequate runtime, environmental, or client-side verification is missing**.

In accordance with `AGENTS.md` ("Implemented does not automatically mean validated"), passing unit tests or synthetic simulations do not substitute for end-to-end runtime proof.

---

### Verification Gap 1: Graphical Client Walkthrough & Spatial Verification

- **Current Status:** `CLIENT WALKTHROUGH QA PENDING`
- **Scope & Defect:**
  - In all prior phases (FULL BUILD 01, INTEGRATION-01, INTEGRATION-02 C-2 through C-4), automated verification used headless server probes, GameTests, and static BFS pathfinding (`phase_c4_walkability.py`).
  - No human or graphical client has navigated the physical space inside the game engine.
- **Specific Missing Test Scenarios:**
  1. **Bridge Crossing & Entrance:** Walk from island mainland across the composite bridge into the entrance on all 6 island styles (`badlands`, `desert`, `forest`, `meadow`, `rocky`, `snowy`). Verify that slab/stair collision does not cause player snags or accidental falls.
  2. **Exterior Spiral Staircase Clearance:** Walk continuously from Ground Floor (Y=1) through Floor 4 to Rooftop (Y=65) without jumping. Check head clearance under arcade arches and canopies with standard player FOV.
  3. **Scenic Balcony Access:** Step onto each of the 8 balconies, press against the perimeter railings, and verify that players cannot slip through the decorative gaps.
  4. **Visual Bridge Support & Deforestation:** Visually inspect the underside of the island-tower bridge (check for unnatural floating slabs) and verify the visual impact of the 425 deforested leaf/log blocks in the `forest` style composite.

---

### Verification Gap 2: Live Survival Combat on Upper Floors (Floors 1–3)

- **Current Status:** `LIVE COMBAT QA PENDING (Floors 1–3)`
- **Scope & Defect:**
  - C-4 implemented real survival-player combat tests in `SkyTowerGameTests.java` (`realVanillaTrialCombatCompletesFloorZeroAndOpensBarrier`), but **only for Floor 0**.
  - Floors 1, 2, and 3 share the same underlying `SkyTowerData` and spawner lifecycle logic, but live mob spawning, pathfinding, player line-of-sight, and barrier clearing on the higher floors have only been asserted via synthetic hook injections.
- **Specific Missing Test Scenarios:**
  1. Real survival combat clearing all 4 spawners on Floor 1 (Y=21), asserting barrier removal at `(39..44, 22..24, 27)`.
  2. Real survival combat on Floor 2 (Y=33), asserting barrier removal at `(39..44, 34..36, 27)`.
  3. Real survival combat on Floor 3 (Y=47), asserting barrier removal at `(39..44, 48..50, 27)`.
  4. Sequential 4-floor continuous campaign: clearing floors 0 -> 1 -> 2 -> 3 in a single uninterrupted survival gameplay session.

---

### Verification Gap 3: Natural Cooldown Expiry & Reactivation (30-Minute Cycle)

- **Current Status:** `LIVE VALIDATION PENDING`
- **Scope & Defect:**
  - Vanilla Trial Spawners have a 30-minute default cooldown (`target_cooldown_length: 36000` ticks).
  - In C-4, spawner reactivation was tested exclusively by writing a synthetic `ACTIVE` state to a completed spawner.
  - Natural transition from `COOLDOWN` back to `WAITING_FOR_PLAYERS` upon timer expiry was never observed in a running server.
- **Specific Missing Test Scenarios:**
  1. Fast-forward server time by 36,000 ticks after clearing Floor 0.
  2. Verify that spawners return to `WAITING_FOR_PLAYERS`.
  3. Verify that `SkyTowerData.completedMask` remains `15` (does not reset).
  4. Verify that `SkyTowerBarriers` does not reinstall the removed barrier wall.
  5. Verify that player re-triggering the spawner receives ordinary vanilla rewards without re-locking the tower.

---

### Verification Gap 4: Ominous Trial & Ominous Vault Interaction

- **Current Status:** `NOT TESTED`
- **Scope & Defect:**
  - All 16 Trial Spawners contain both `normal` and `ominous` vanilla trial chamber configs in NBT (`trial_chamber/melee/zombie/ominous`).
  - No test has ever exposed a player with the `Trial Omen` effect to a Sky Tower spawner.
- **Specific Missing Test Scenarios:**
  1. Player drinks Ominous Bottle (gains Bad Omen / Trial Omen) and approaches Sky Tower spawner.
  2. Verify ominous spawner transformation: soul flame particles, projectile spawners, armored mobs.
  3. Verify spawner completion ejects Ominous Trial Key.
  4. Verify that ominous completion satisfies `SkyTowerCompletionObserver` identically to normal completion.

---

### Verification Gap 5: Mountain Biome High-Altitude Natural Generation

- **Current Status:** `UNVERIFIED ON DEFAULT TERRAIN`
- **Scope & Defect:**
  - In C-4, natural generation of `desert`, `badlands`, and `snowy` was sampled on single-biome flat worlds with bedrock at Y=-60 (surface at Y=-56), where islands spawned at low coordinates (Y=1..20).
  - High-altitude generation in jagged peaks, stony peaks, and snowy slopes was never sampled.
- **Specific Missing Test Scenarios:**
  1. Dedicated server generation in a standard Overworld seed containing extreme mountain peaks (Y >= 180).
  2. Locate `sky_island_mountain_snowy` or `sky_island_mountain_rocky`.
  3. Verify that the rooftop sanctuary arch tops (Y_piece + 79) do not exceed Y=320.
  4. Verify that all 16 spawners, 8 vaults, and 72 barrier cells generate unclipped.

---

### Verification Gap 6: Mid-Combat Dynamic Chunk Streaming & Player Disconnect

- **Current Status:** `PARTIALLY TESTED`
- **Scope & Defect:**
  - C-4 verified dedicated server restart persistence (`floorclear_save_7202.json` / `floorclear_resume_7202.json`).
  - However, in-session chunk unloading and reloading caused by player movement (walking 128 blocks away mid-fight and returning) was not tested.
  - Player disconnecting and reconnecting during an active trial wave was not tested.
- **Specific Missing Test Scenarios:**
  1. Start trial combat on Floor 1 (spawners active, 4 zombies alive).
  2. Player teleports 500 blocks away (unloading tower chunks).
  3. Player teleports back; assert that spawner state, mob positions, and barrier ownership recover gracefully.
  4. Player disconnects while spawner is `ACTIVE`; reconnects; assert no duplicate spawns or progress corruption.

---

### Verification Gap 7: Multi-Player Scaling & Concurrency (3+ Players)

- **Current Status:** `PARTIALLY TESTED`
- **Scope & Defect:**
  - `SkyTowerGameTests` tested exactly 2 players standing at local `(24, 11, 24)`.
  - Larger survival groups (3–5 players), simultaneous combat on different floors by different players, and players joining mid-encounter have not been verified.
- **Specific Missing Test Scenarios:**
  1. 3+ players engage Floor 0 spawners simultaneously; verify vanilla mob scaling (6 + 2*(N-1) = 10+ mobs).
  2. Player A clears Floor 0; proceeds to Floor 1; Player B remains on Floor 0. Verify barriers on Floor 0 remain open while Floor 1 barriers remain locked until Player A clears Floor 1.
