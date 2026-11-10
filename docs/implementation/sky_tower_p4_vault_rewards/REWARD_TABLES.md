# SKY TOWER P4 — REWARD TABLES SPECIFICATION
## Comprehensive Loot Tables, Weights, and Yield Analysis

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  

---

## 1. Normal Vault Reward Pool (`moreminecrafts:chests/sky_islands/vault`)

- **Resource Location:** `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault.json`
- **Container Type:** `minecraft:vault`
- **Required Key:** `minecraft:trial_key`
- **Total Pools:** 5

### Pool 0: Primary Sub-Table Draw (1 Roll)
Rolls: 1  
Weight sum: 8 + 2 + 3 = 13

| Entry ID | Type | Weight | Probability | Description |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_rare` | loot_table | 8 | 61.54% (8/13) | Vanilla rare trial rewards (diamonds, emeralds, enchanted books) |
| `minecraft:chests/trial_chambers/reward_common` | loot_table | 2 | 15.38% (2/13) | Vanilla common trial supplies |
| `moreminecrafts:chests/sky_islands/materials` | loot_table | 3 | 23.08% (3/13) | Sky Realm progression materials |

### Pool 1: Common Supplies (1–3 Rolls)
Rolls: Uniform 1 to 3 (Average: 2.0 rolls)

| Entry ID | Type | Weight | Probability | Description |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_common` | loot_table | 1 | 100% | Bread, arrows, honey bottles, baked potatoes, iron ingots |

### Pool 2: Unique Reward Draw (Conditional)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.25` (25.0%)

| Entry ID | Type | Weight | Probability | Description |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_unique` | loot_table | 1 | 25.00% | Heavy Core, Flow / Guster banner patterns, Wind Charge |

### Pool 3: Sky Feather Progression Material (Independent Draw)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.15` (15.0%)

| Entry ID | Type | Count | Probability | Description |
|---|---|---|---|---|
| `moreminecrafts:sky_feather` | item | 1 | 15.00% | Core crafting component for Wind Sword and Wind Bow |

### Pool 4: Gale Shard Progression Material (Independent Draw)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.05` (5.0%)

| Entry ID | Type | Count | Probability | Description |
|---|---|---|---|---|
| `moreminecrafts:gale_shard` | item | 1 | 5.00% | Rare pinnacle component for Wind Archer's Bow |

---

## 2. Rooftop Special Vault Reward Pool (`moreminecrafts:chests/sky_islands/vault_tower_top`)

- **Resource Location:** `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault_tower_top.json`
- **Container Type:** Special Vault (Pending Owner Decision on Normal vs Ominous)
- **Total Pools:** 5

### Pool 0: Elevated Pinnacle Sub-Table Draw (1 Roll)
Rolls: 1  
Weight sum: 14 + 2 + 6 = 22

| Entry ID | Type | Weight | Probability | Comparison vs Normal |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_rare` | loot_table | 14 | 63.64% (14/22) | Elevated rare weighting (+2.1% net probability, weight 14 vs 8) |
| `minecraft:chests/trial_chambers/reward_common` | loot_table | 2 | 9.09% (2/22) | Decreased common filler (-6.3% probability) |
| `moreminecrafts:chests/sky_islands/materials` | loot_table | 6 | 27.27% (6/22) | Elevated sky materials (+4.2% net probability, weight 6 vs 3) |

### Pool 1: Common Supplies (1–3 Rolls)
Rolls: Uniform 1 to 3 (Average: 2.0 rolls)

| Entry ID | Type | Weight | Probability | Description |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_common` | loot_table | 1 | 100% | Identical baseline supply sustain |

### Pool 2: Doubled Unique Reward Draw (Conditional)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.50` (50.0%)

| Entry ID | Type | Weight | Probability | Comparison vs Normal |
|---|---|---|---|---|
| `minecraft:chests/trial_chambers/reward_unique` | loot_table | 1 | 50.00% | **Doubled from 25%**; pinnacle reward incentive |

### Pool 3: Sky Feather Progression Material (Independent Draw)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.15` (15.0%)

| Entry ID | Type | Count | Probability | Description |
|---|---|---|---|---|
| `moreminecrafts:sky_feather` | item | 1 | 15.00% | Preserved 15% rate per §275 |

### Pool 4: Gale Shard Progression Material (Independent Draw)
Rolls: 1  
Condition: `minecraft:random_chance` with `chance: 0.05` (5.0%)

| Entry ID | Type | Count | Probability | Description |
|---|---|---|---|---|
| `moreminecrafts:gale_shard` | item | 1 | 5.00% | Preserved 5% rate per §275 |

### Pool 5: Extra Personal Accessory Draw (Code Driven: `StarCovenantVaultRewards`)
- **Authority:** D13-03-Q11/Q12 (`docs/planning/D13_03_DECISIONS_Q01-Q47.md`)
- **Item:** `moreminecrafts:star_covenant` (Tier 2 Accessory)
- **Chance:** 10.00% (`roll < 0.10D`)
- **Execution:** Rolled exactly once per player per rooftop vault; result saved permanently in `moreminecrafts:star_covenant_vault_claims`.

---

## 3. Sky Islands Materials Sub-Table (`moreminecrafts:chests/sky_islands/materials`)

- **Resource Location:** `src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/materials.json`
- **Total Pools:** 1 (Rolls: 1)
- **Weight sum:** 4 + 3 + 3 + 2 = 12

| Item ID | Count Range | Weight | Relative Share | Mean Yield per Draw |
|---|---|---|---|---|
| `moreminecrafts:mythril` | 1–2 (Uniform) | 4 | 33.33% (4/12) | 0.50 ingots |
| `moreminecrafts:titanium_ingot` | 1–2 (Uniform) | 3 | 25.00% (3/12) | 0.375 ingots |
| `moreminecrafts:tungsten_ingot` | 1–2 (Uniform) | 3 | 25.00% (3/12) | 0.375 ingots |
| `moreminecrafts:adamantium_ingot`| 1 (Fixed) | 2 | 16.67% (2/12) | 0.167 ingots |

---

## 4. Expected Yield per Complete Tower Run (8 Normal Vaults)

Assuming a single player clears all 4 combat floors and unlocks all 8 normal vaults:

| Item / Category | Probability per Vault | Expected Total per Tower (8 Vaults) | Minimum Possible | Maximum Possible |
|---|---|---|---|---|
| **Sky Feather** | 15% | **1.20** | 0 | 8 |
| **Gale Shard** | 5% | **0.40** | 0 | 8 |
| **Unique Trial Item** | 25% | **2.00** | 0 | 8 |
| **Materials Draw** | 23.08% | **1.85** | 0 | 8 |
| **Rare Trial Draw** | 61.54% | **4.92** | 0 | 8 |
| **Common Items** | 100% (avg 2 rolls) | **16.00** | 8 | 24 |

*Note: With an expected 1.20 Sky Feathers and 0.40 Gale Shards per tower, crafting a Wind Sword (2 Sky Feathers) requires an average of ~1.67 tower clears, and crafting a Wind Archer's Bow (2 Sky Feathers + 1 Gale Shard) requires an average of ~2.50 tower clears, precisely aligning with Tier 2 progression targets.*
