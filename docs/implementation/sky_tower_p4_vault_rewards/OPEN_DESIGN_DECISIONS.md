# SKY TOWER P4 — DESIGN DECISIONS & ROOFTOP GEOMETRY FINALIZATION

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  
**Current Gate Status:** **ALL DESIGN DECISIONS FINALIZED & APPROVED BY OWNER**

---

## 1. Owner Decisions Inventory (All Approved)

| Decision Item | Approved Value | Implementation Mapping | Status |
|---|---|---|---|
| **1. Rooftop Vault Coordinate** | Local `(24, 65, 11)` | `SkyTowerLayout.Site.rooftopVault()` | **IMPLEMENTED** |
| **2. Container Block Type** | Standard `minecraft:vault` | Vanilla `Blocks.VAULT` (`VaultBlockEntity`) | **IMPLEMENTED** |
| **3. Vault Facing Direction** | Local North, structure-rotated | `SkyTowerVaultRewards.rooftopVaultFacing(rotation)` | **IMPLEMENTED** |
| **4. Unlock Key Item** | `minecraft:trial_key` | `new ItemStack(Items.TRIAL_KEY)` in `VaultConfig` | **IMPLEMENTED** |
| **5. Reward Table** | `moreminecrafts:chests/sky_islands/vault_tower_top` | `ROOFTOP_VAULT_LOOT_KEY` | **IMPLEMENTED** |
| **6. Eight Normal Vaults** | 100% Preserved | `site.vault(0..7)` on Floors 0..3 untouched | **IMPLEMENTED** |
| **7. Placement Mechanism** | Runtime placement via registration | `SkyTowerWorldgenRegistration` + `CHUNK_LOAD` | **IMPLEMENTED** |
| **8. Frozen NBT Templates** | Preserved (Zero NBT mutations) | SHA `cec5bced` byte-identical to `16ad9d5` | **IMPLEMENTED** |
| **9. Star Covenant Draw** | 10% personal roll per player | `StarCovenantVaultRewards.java` | **IMPLEMENTED** |

---

## 2. Geometric Resolution: Transition from (24, 65, 10) to (24, 65, 11)

1. **Initial Coordinate Audit `(24, 65, 10)`:**
   - Occupied by `minecraft:smooth_stone` (lower south parapet wall).
   - Overhead block `(24, 66, 10)` was solid `minecraft:smooth_stone`, causing upward item ejection obstruction and vault occlusion.
2. **Approved Coordinate `(24, 65, 11)`:**
   - Existing block: `minecraft:air` across all 6 biome composite templates.
   - Floor foundation: `(24, 64, 11)` `minecraft:polished_andesite`.
   - Overhead clearance: Open air from `Y=66` to `Y=79` (unobstructed sky).
   - Architectural harmony: Rests flush against the south parapet wall `(24, 65, 10)`, framed symmetrically by the chiseled stone brick piers and stone columns at `X=21` and `X=27`.
   - Facing: Local North (`facing=north`), looking across the sanctuary towards the central floating relic and rooftop stair entry.

---

## 3. Structure Rotation Mapping Table

The rooftop vault facing transforms cleanly with site rotation (clockwise):

| Site Rotation | Structure Angle | Rooftop Vault Point `(x, y, z)` | Vault Facing Property |
|---|---|---|---|
| **0** (`NONE`) | 0° | `(x + 24, y + 65, z + 11)` | `Direction.NORTH` |
| **1** (`CLOCKWISE_90`) | 90° CW | `(x - 11, y + 65, z + 24)` | `Direction.EAST` |
| **2** (`CLOCKWISE_180`) | 180° CW | `(x - 24, y + 65, z - 11)` | `Direction.SOUTH` |
| **3** (`COUNTERCLOCKWISE_90`) | 270° CW | `(x + 11, y + 65, z - 24)` | `Direction.WEST` |

---

## 4. Open Decisions Remaining

**NONE.** All gameplay, architectural, container, balance, and placement decisions have been resolved and durably recorded in current authority.
