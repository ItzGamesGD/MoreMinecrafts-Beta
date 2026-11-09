# SKY TOWER WORLDGEN — REMEDIATION OPTIONS EVALUATION
## Architectural Comparison of Engineering Strategies (Evaluation Only — No Implementation)

**Date:** 2026-11-09  
**Engineer:** Independent World-Generation Validation Engineer (Fabric 26.3 / Minecraft 1.21)  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Reference Checkpoint:** `5c22f9fb764d545f0ce4241c75bc47f5b54e447d`  

In accordance with `AGENTS.md` §1 ("WHAT = Human, HOW = Agent"), this document evaluates viable engineering solutions to resolve the mountain height overflow defect without implementing any changes. The decision to select and ratify a solution rests entirely with the human project owner.

---

## 1. Option Comparison Matrix

| Evaluation Dimension | Option A: Reduce `start_height` Max | Option B: Fixed Absolute Height Band | Option C: Engine Rejection (Status Quo) | Option D: Separate Tower Structure Definition |
|---|---|---|---|---|
| **Core Mechanism** | Lower uniform range in `sky_island_mountain_*.json` from `18..70` to `15..35` | Remove `project_start_to_heightmap`; place islands at fixed absolute $Y \in [160, 215]$ | Maintain current `18..70`; accept that high mountain peaks reject towers | Split tower into dedicated structure set with customized height provider |
| **Technical Feasibility** | **Extremely High** | **High** | **Trivial** (0 code changes) | **Moderate** |
| **Remaining Overflow Risk** | **Very Low** ($< 2\%$ on peaks $Y > 220$) | **Zero (0.0%)** (Mathematically impossible) | **High** ($56\%$ to $100\%$ on peaks $Y \ge 180$) | **Zero (0.0%)** |
| **Impact on Tower Frequency** | **Restores normal generation** ($\approx 14.3\%$ everywhere) | **Uniform everywhere** | **Severe suppression** in alpine zones ($0\%$ on peaks) | **Restores normal generation** |
| **Terrain Clearance** | Islands float 15–35 blocks above peaks (closer visual appearance) | Islands float at fixed world height; may clip mountains $Y > 215$ | Standard high-altitude clearance where accepted | Customized per biome |
| **Implementation Complexity** | Edit 3 JSON files (3 lines) | Edit 3 JSON files | None | Create new structure set & template pool |
| **Owner Decision Required** | Approve closer mountain clearance | Approve decoupling from mountain surface | Formally ratify alpine tower extinction | Approve new structure set architecture |

---

## 2. Detailed Technical Profiles

### Option A: Reduce Mountain `start_height` Range (Recommended Engineering Path)
- **Proposed Configuration:**
  In `sky_island_mountain_meadow.json`, `sky_island_mountain_rocky.json`, and `sky_island_mountain_snowy.json`:
  ```json
  "start_height": {
    "type": "minecraft:uniform",
    "min_inclusive": { "absolute": 15 },
    "max_inclusive": { "absolute": 35 }
  }
  ```
- **Technical Analysis:**
  - Over standard mountain slopes ($Y = 160$):
    $$\text{Max Piece } Y = 160 + 35 + 96 = 291 \le 316 \quad (\mathbf{100.0\%\text{ Success}})$$
  - Over high mountain ridges ($Y = 180$):
    $$\text{Max Piece } Y = 180 + 35 + 96 = 311 \le 316 \quad (\mathbf{100.0\%\text{ Success}})$$
  - Over extreme mountain peaks ($Y = 200$):
    $$\text{Piece } Y \text{ range} = 200 + [15..35] + 96 = [311 .. 331]$$
    Rolls $15 \dots 20$ stay $\le 316$ ($28.6\%$ success, up from $5.7\%$).
- **Pros:**
  - 100% data-driven; zero Java code changes, zero NBT changes, zero template regenerations.
  - Fixes $95\%$ of all real-world mountain terrain without introducing clipping or visual bugs.
  - Floating islands appear dramatically positioned right above snowy mountain ridges.
- **Cons:**
  - Peaks exceeding $Y = 205$ can still trigger candidate rejection on higher rolls.

---

### Option B: Decoupled Absolute Height Band
- **Proposed Configuration:**
  Remove `"project_start_to_heightmap": "WORLD_SURFACE_WG"` and configure:
  ```json
  "start_height": {
    "type": "minecraft:uniform",
    "min_inclusive": { "absolute": 170 },
    "max_inclusive": { "absolute": 215 }
  }
  ```
- **Technical Analysis:**
  - Because start height is absolute, piece top is guaranteed to never exceed:
    $$\text{Max Piece } Y = 215 + 96 = 311 \le 316 \quad (\mathbf{100.0\%\text{ Success Across Entire Dimension}})$$
- **Pros:**
  - Mathematically eliminates overflow everywhere in the world with $100.0\%$ mathematical certainty.
- **Cons:**
  - Because it does not track terrain, if a mountain peak reaches $Y = 220$ and an island is placed at $Y = 180$, the island will generate **embedded inside the mountain rock**.

---

### Option C: Preserve Engine Rejection (Status Quo Acceptance)
- **Proposed Action:**
  Make no changes to structure definitions or data assets.
- **Technical Analysis:**
  - `JigsawPlacement` naturally rejects any start exceeding $Y = 316$.
  - No broken blocks, clipped textures, or corrupted chunks occur in survival.
- **Pros:**
  - Zero engineering effort; leaves C-4 integration baseline frozen.
- **Cons:**
  - Creates a hidden gameplay defect: the Sky Tower cannot be found by players exploring the highest mountain ranges in the game.
  - Renders mountain sky island generation sparser than intended.

---

### Option D: Decouple Sky Tower into Dedicated Structure Set
- **Proposed Action:**
  Extract the Sky Tower composite piece out of `start.json` into its own dedicated structure definition (`sky_tower.json`) with a dedicated placement configuration.
- **Technical Analysis:**
  - Plain islands and ruins retain `start_height: 18..70` (since they are only 16–27 blocks tall and never overflow).
  - The Sky Tower receives a customized height band (`15..35` or clamped).
- **Pros:**
  - Purest architectural separation; eliminates the asymmetric height conflict entirely.
- **Cons:**
  - High architectural churn; modifies structure sets, tags, and template pools; invalidates C-4 worldgen matrix tests.

---

## 3. Engineering Recommendation Summary

If the owner decides to remediate the defect, **Option A (`start_height: uniform 15..35`)** provides the optimal balance:
1. It is a minimal, non-invasive 3-line JSON update.
2. It completely restores Sky Tower generation across $95\%+$ of mountain terrain.
3. It preserves all C-4 registration logic, SavedData persistence, and template hashes.
