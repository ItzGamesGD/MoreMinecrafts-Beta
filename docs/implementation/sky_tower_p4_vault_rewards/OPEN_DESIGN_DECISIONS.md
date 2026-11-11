# SKY TOWER P4 — DESIGN DECISIONS & ROOFTOP GEOMETRY AUDIT

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  
**Current Gate Status:** **PARTIAL — STOP & REPORT TRIGGERED (Geometric Conflict at Preferred Coordinate)**

---

## 1. Owner Decisions Received & Ratified

The project owner explicitly approved the following decisions for the Sky Tower rooftop special vault:

| Decision Item | Approved Value | Implementation Mapping |
|---|---|---|
| **Container Type** | Standard `minecraft:vault` | Vanilla block; uses `VaultBlockEntity` and `VaultConfig` |
| **Unlock Item** | Standard `minecraft:trial_key` | Preserves standard trial key item requirement |
| **Reward Table** | `moreminecrafts:chests/sky_islands/vault_tower_top` | Configured via `ROOFTOP_VAULT_LOOT_KEY` |
| **Unified Framework** | `SkyTowerVaultRewards.java` | Single unified dispatch for normal (8) and rooftop (1) vaults |
| **Interaction Mechanics** | Vanilla `tryInsertKey` pipeline | Preserves vanilla key consumption, multiplayer cooldowns, and eligibility |
| **Star Covenant Roll** | 10% personal draw per player | `StarCovenantVaultRewards.java` ratified per D13-03-Q11/Q12 |
| **Preferred Location** | South rooftop altar `(24, 65, 10)` | Subject to geometric verification before placement |

---

## 2. Rooftop Placement Geometric Verification (Phase C Audit)

Per the directive:
> *"The preferred coordinate is not an unconditional placement command. Verify its geometry, block occupancy, player access, and compatibility with all six tower variants before modifying any structure. If the proposed location is unsuitable, stop and report alternatives rather than silently moving the Vault."*

A line-by-line 3D voxel inspection of `full_01.nbt` (SHA `cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27`) and all 6 composite island templates (`snowy`, `meadow`, `forest`, `badlands`, `desert`, `rocky`) was executed for the south rooftop altar.

### 2.1 Coordinate Inspection at Preferred Position `(24, 65, 10)`
```
Y=67: (24, 67, 10) = minecraft:air
Y=66: (24, 66, 10) = minecraft:smooth_stone  <-- SOLID BLOCK DIRECTLY ABOVE TARGET
Y=65: (24, 65, 10) = minecraft:smooth_stone  <-- TARGET COORDINATE (OCCUPIED)
Y=64: (24, 64, 10) = minecraft:polished_andesite (Floor foundation)
```

In front of target (`Z=11`):
```
Y=66: (24, 66, 11) = minecraft:air
Y=65: (24, 65, 11) = minecraft:air (Floor walking clearance)
Y=64: (24, 64, 11) = minecraft:polished_andesite (Rooftop sanctuary floor)
```

### 2.2 Architectural Analysis of the South Altar
The south altar spans `X=21..27` and `Z=10..13`:
1. **Flanking Pillars:** Two 4-block-tall stone columns stand at `(21, 65..68, 12)` and `(27, 65..68, 12)`.
2. **Side Pedestals:** Chiseled stone bricks occupy `X=21..22` and `X=26..27` at `Y=65`.
3. **Altar Niche:** The center area `X=23..25`, `Z=11..13` is open air from `Y=65` to `Y=79` (roofless sky per Q41).
4. **South Parapet Wall:** At `Z=10`, a 2-block-tall solid wall of `minecraft:smooth_stone` runs along the perimeter at both `Y=65` and `Y=66`.

### 2.3 Why `(24, 65, 10)` is Geometrically Problematic
1. **Occupied Block:** `(24, 65, 10)` is not an empty pedestal or air; it is the lower tier of the solid south perimeter wall.
2. **Blocked Overhead Clearance:** The block directly overhead at `(24, 66, 10)` is solid `minecraft:smooth_stone`.
3. **Vanilla Vault Reward Ejection Failure:** Vanilla Minecraft spawns vault reward items at `(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5)` with upward velocity. With `(24, 66, 10)` solid, rewards spawn inside solid stone, causing item entrapment or suffocation.
4. **Occluded Vault Model:** The top cage and spinning display item of the vault are completely hidden under the upper wall block. Only the front face is visible.

---

## 3. Concrete Alternatives for Owner Selection

Per `AGENTS.md` and the task Stop Conditions, the agent must NOT silently relocate the vault. The owner is requested to select one of the following 3 structurally sound alternatives:

### Alternative A: Southern Altar Niche Floor / Base `(24, 65, 11)` — (Recommended)
- **Position:** `(24, 65, 11)`
- **Existing Block:** `minecraft:air` (clean placement, no solid block destroyed)
- **Support Below:** `(24, 64, 11)` `minecraft:polished_andesite` (intact sanctuary floor)
- **Overhead Clearance:** `(24, 66, 11)` is `air` extending to the open sky
- **Backing:** Rests directly against the solid south parapet wall `(24, 65, 10)`
- **Facing:** `facing=north` (looks toward the central floating relic and sanctuary entrance)
- **Pros:** 100% vanilla reward ejection into open air; full 3D vault visibility; framed symmetrically by chiseled stone brick piers and stone columns; zero demolition required.

### Alternative B: Southern Altar Wall Pedestal Mount `(24, 66, 10)`
- **Position:** `(24, 66, 10)`
- **Existing Block:** `minecraft:smooth_stone` (upper tier of the parapet wall)
- **Support Below:** `(24, 65, 10)` `minecraft:smooth_stone` (serves as a 1-block elevated pedestal!)
- **Overhead Clearance:** `(24, 67, 10)` is `air` extending to open sky
- **Facing:** `facing=north`
- **Pros:** Elevated 1 block above floor level at player chest/eye height; upward reward ejection into open sky; leaves floor walking space completely clear.
- **Cons:** Replaces an existing wall block.

### Alternative C: Southern Altar Center Pedestal `(24, 65, 12)`
- **Position:** `(24, 65, 12)`
- **Existing Block:** `minecraft:air`
- **Support Below:** `(24, 64, 12)` `minecraft:polished_andesite`
- **Overhead Clearance:** Open air to sky
- **Pros:** Centered exactly between the two altar columns `(21, 65..68, 12)` and `(27, 65..68, 12)`.
- **Cons:** Stands 2 blocks forward from the back wall.

---

## 4. Placement Implementation Mechanism Comparison

| Evaluation Axis | Runtime Placement (`SkyTowerWorldgenRegistration`) | Dedicated NBT Geometry Revision (`full_01.nbt`) |
|---|---|---|
| **Placement Reliability** | High (triggers deterministically upon chunk load/registration) | Highest (native engine Jigsaw placement) |
| **Chunk Lifecycle** | Persisted into chunk block entity data on first save | Generated natively into chunk during worldgen |
| **Rotation Handling** | Handled via `site.at(localX, localY, localZ)` | Handled natively by Minecraft StructureTemplate |
| **Save/Reload Behavior** | Identical (chunk tile entity stores `VaultConfig`) | Identical |
| **Maintainability** | Clean (zero changes to frozen binary assets) | Requires regenerating 7 NBT files across 6 biomes |
| **Frozen Template Hash** | **PRESERVED** (`cec5bced` SHA remains intact) | **BROKEN** (invalidates approved SHA `16ad9d5`) |
| **Compatibility with Existing Worlds** | **EXCELLENT** (can place vault in already generated sites) | **POOR** (only new chunks receive the vault) |

**Recommendation:** If Alternative A `(24, 65, 11)` or Alternative B `(24, 66, 10)` is approved, runtime placement in `SkyTowerWorldgenRegistration` is the least invasive, technically superior implementation because it completely avoids altering the frozen NBT templates while providing backward-compatibility for existing worlds.
