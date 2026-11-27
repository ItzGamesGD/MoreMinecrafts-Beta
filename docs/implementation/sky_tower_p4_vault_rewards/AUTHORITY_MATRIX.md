# SKY TOWER P4 — AUTHORITY MATRIX
## Vault Rewards Authority & Status Inventory

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  

---

## 1. Classification Definitions

Per `AGENTS.md` and P4 task instructions, every entry is categorized into:
- **APPROVED:** Explicitly authorized in approved design records or human decisions.
- **IMPLEMENTED:** Present and operational in code/data assets.
- **PROPOSED:** Documented in candidate proposals or planning discussions without final human approval.
- **UNDEFINED:** No authoritative specification exists.

---

## 2. Master Authority Matrix

| Reward Entry / Parameter | Source Document | Source Commit | Registry ID / Value | Quantity | Weight / Probability | Applicable Vault | Status |
|---|---|---|---|---|---|---|---|
| **Normal Vault Count** | `SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q08, Q18, Q25 | `1449bf1` / `16ad9d5` | `minecraft:vault` | 8 (2 per floor, 4 floors) | Fixed (100%) | Normal Vaults (Floors 1–4) | **APPROVED & IMPLEMENTED** |
| **Normal Key Item** | Vanilla / Candidate NBT | `16ad9d5` | `minecraft:trial_key` | 1 | Required for unlock | Normal Vaults (Floors 1–4) | **APPROVED & IMPLEMENTED** |
| **Normal Sky Feather** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `moreminecrafts:sky_feather` | 1 | 15% independent roll (`random_chance: 0.15`) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Gale Shard** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `moreminecrafts:gale_shard` | 1 | 5% independent roll (`random_chance: 0.05`) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Rare Pool** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_rare` | 1 roll (pool) | Weight 8 (8/13 = 61.5%) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Common in Pool 0** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_common` | 1 roll (pool) | Weight 2 (2/13 = 15.4%) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Materials in Pool 0**| `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `moreminecrafts:chests/sky_islands/materials` | 1 roll (pool) | Weight 3 (3/13 = 23.1%) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Common Rolls** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_common` | 1–3 rolls (uniform) | 100% | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Normal Unique Reward** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_unique` | 1 roll | 25% chance (`random_chance: 0.25`) | Normal Vaults | **APPROVED & IMPLEMENTED** |
| **Sky Material: Mythril** | `materials.json` / §275 | `631d9504` | `moreminecrafts:mythril` | 1–2 (uniform) | Weight 4 (4/12 = 33.3%) | Materials sub-table | **APPROVED & IMPLEMENTED** |
| **Sky Material: Titanium** | `materials.json` / §275 | `631d9504` | `moreminecrafts:titanium_ingot` | 1–2 (uniform) | Weight 3 (3/12 = 25.0%) | Materials sub-table | **APPROVED & IMPLEMENTED** |
| **Sky Material: Tungsten** | `materials.json` / §275 | `631d9504` | `moreminecrafts:tungsten_ingot` | 1–2 (uniform) | Weight 3 (3/12 = 25.0%) | Materials sub-table | **APPROVED & IMPLEMENTED** |
| **Sky Material: Adamantium**| `materials.json` / §275 | `631d9504` | `moreminecrafts:adamantium_ingot` | 1 | Weight 2 (2/12 = 16.7%) | Materials sub-table | **APPROVED & IMPLEMENTED** |
| **Rooftop Sky Feather** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `moreminecrafts:sky_feather` | 1 | 15% independent roll (`random_chance: 0.15`) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Gale Shard** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `moreminecrafts:gale_shard` | 1 | 5% independent roll (`random_chance: 0.05`) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Rare Pool** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_rare` | 1 roll (pool) | Weight 14 (14/22 = 63.6%) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Common in Pool 0**| `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_common` | 1 roll (pool) | Weight 2 (2/22 = 9.1%) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Materials Pool 0** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `moreminecrafts:chests/sky_islands/materials` | 1 roll (pool) | Weight 6 (6/22 = 27.3%) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Common Rolls** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_common` | 1–3 rolls (uniform) | 100% | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Unique Reward** | `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §161, §275 | `631d9504` | `minecraft:chests/trial_chambers/reward_unique` | 1 roll | 50% chance (`random_chance: 0.50`, doubled) | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Star Covenant Accessory** | `D13_03_DECISIONS_Q01-Q47.md` D13-03-Q11/Q12 | `7a3a401` | `moreminecrafts:star_covenant` | 1 | 10% per player per vault, rolled once, saved | Rooftop Special Vault | **APPROVED & IMPLEMENTED** |
| **Rooftop Vault Structure Presence** | Structure NBT `full_01.nbt` audit | `16ad9d5` | `minecraft:vault` or container | 0 present | 0 | Rooftop Sanctuary (Y >= 64) | **NOT PRESENT IN NBT** |
| **Rooftop Container Type** | `OWNER_DECISIONS_REQUIRED.md` Decision 2 | `5c22f9f` | `minecraft:vault` vs `ominous_vault` vs chest | 1 | Unresolved | Rooftop Sanctuary | **UNDEFINED (Requires Owner Decision)** |
| **Rooftop Placement Coordinates** | `OWNER_DECISIONS_REQUIRED.md` Decision 2 | `5c22f9f` | Altar `(24, 65, 10)` vs Relic `(24, 65, 24)` | 1 location | Unresolved | Rooftop Sanctuary | **UNDEFINED (Requires Owner Decision)** |
| **Rooftop Key Requirement** | `OWNER_DECISIONS_REQUIRED.md` Decision 2 | `5c22f9f` | `minecraft:trial_key` vs `ominous_trial_key` | 1 | Unresolved | Rooftop Sanctuary | **UNDEFINED (Requires Owner Decision)** |

---

## 3. Mandatory Gate Evaluation

1. **Separable Approved Content:**
   - Normal Vaults: 8 vaults across 4 floors are fully approved and verified present in structure templates. Custom loot table `moreminecrafts:chests/sky_islands/vault` and materials table `moreminecrafts:chests/sky_islands/materials` are fully approved and implemented.
   - Rooftop Reward Pool: `moreminecrafts:chests/sky_islands/vault_tower_top` is fully approved and implemented in data assets. Star Covenant reward logic (10% personal draw) is fully approved (D13-03-Q11/Q12) and implemented in `StarCovenantVaultRewards.java`.
   - Unified Framework: `SkyTowerVaultRewards` provides the unified dispatch and configuration layer for both normal and rooftop vaults.

2. **Unresolved Parameters (Gate Triggered):**
   - The rooftop special vault has **zero physical presence** in the candidate structure template (`full_01.nbt`).
   - The rooftop container type (standard vault, ominous vault, or chest) and exact coordinates remain **UNDEFINED** pending owner decision (Decision 2).
   - Under Section 4 and Section 11 of the P4 directive, these parameters must NOT be invented, and P4 cannot be reported as COMPLETE.
   - **Verdict:** **PARTIAL** (fully approved components implemented; open rooftop physical design submitted for owner review).
