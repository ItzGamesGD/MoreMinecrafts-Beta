# SKY TOWER P4 — REGRESSION & SCOPE AUDIT REPORT
## Scope Boundary Compliance & Regression Analysis

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  

---

## 1. Compliance with Section 9 Scope Exclusions

| Excluded System / Component | Requirement | Verification Method | Status |
|---|---|---|---|
| **Adaptive Mountain Worldgen** | Must NOT modify | `git diff dev/itzgamesgd/moreminecrafts/worldgen/` | **UNTOUCHED (0 changes)** |
| **Structure Generation Rates** | Must NOT modify | `git diff data/moreminecrafts/worldgen/structure_set/` | **UNTOUCHED (0 changes)** |
| **Sky Tower NBT Geometry** | Must NOT modify | `verify_template.py` SHA256 check | **UNTOUCHED (100% byte-identical)** |
| **Bridge Layout** | Must NOT modify | Template inspection | **UNTOUCHED (0 changes)** |
| **Combat-Floor Geometry** | Must NOT modify | Template inspection | **UNTOUCHED (0 changes)** |
| **Trial Spawner Placement** | Must NOT modify | Spawner coordinates check (16 spawners) | **UNTOUCHED (0 changes)** |
| **Barrier Progression** | Must NOT modify | `SkyTowerBarriers.java`, `SkyTowerProgress.java` | **UNTOUCHED (0 changes)** |
| **Other Structures' Vaults** | Must NOT modify | Mixin scoping audit (`isSkyTowerVault`) | **100% ISOLATED** |
| **Unrelated Loot Systems** | Must NOT modify | Git diff on vanilla loot / other mod loot | **UNTOUCHED (0 changes)** |
| **Ancient Colosseum (ST-05)** | Must NOT modify | Git diff on `st05` / Colosseum tests | **UNTOUCHED (0 changes)** |

---

## 2. Vault Isolation & Foreign Vault Scoping Audit

A critical failure mode in custom vault implementations is global leakage—where modifying vault loot tables accidentally affects vanilla Trial Chambers or custom vaults in other dimensions/structures.

### 2.1 Technical Scoping Guard
In `SkyTowerVaultRewards.java`:
```java
public static VaultConfig resolveVaultConfig(ServerLevel level, BlockPos pos, VaultConfig current) {
    if (level == null || pos == null || current == null) return current;
    if (isNormalVault(level, pos)) {
        // Only Sky Tower normal vaults receive NORMAL_VAULT_LOOT_KEY
        ...
    }
    if (isRooftopVault(level, pos)) {
        // Only Sky Tower rooftop vault receives ROOFTOP_VAULT_LOOT_KEY
        ...
    }
    return current; // Unrelated vaults return original unmodified config
}
```

### 2.2 Mixin Safety
In `SkyTowerVaultRewardMixin.java`:
- The `@ModifyVariable` only alters `VaultConfig` if `resolveVaultConfig` returns a different config.
- When an unrelated vault is accessed (e.g. at a vanilla Trial Chamber):
  - `findSite` returns `null`.
  - `isNormalVault` and `isRooftopVault` return `false`.
  - The incoming `current` config is returned without modification.
  - No database lookup or state mutation occurs.

---

## 3. World Data Compatibility & Clean Upgrade Path

1. **Existing World Upgrades:**
   - Worlds generated prior to P4 may have Sky Tower vaults initialized with the vanilla default `TRIAL_CHAMBERS_REWARD`.
   - When a player approaches or interacts with the vault, `resolveVaultConfig` dynamically updates the `VaultBlockEntity` and marks it dirty, seamlessly upgrading existing world vaults without world regeneration.
2. **Fresh World Generation:**
   - Newly generated towers configure all 8 normal vaults at chunk registration time via `configureSiteVaults`.
   - Vault configs are serialized into standard chunk NBT, ensuring offline permanence.
3. **Save/Reload Safety:**
   - No custom block entities or ticking entities were introduced.
   - All persistence utilizes vanilla Minecraft's `VaultConfig.CODEC` and `SavedData`.
