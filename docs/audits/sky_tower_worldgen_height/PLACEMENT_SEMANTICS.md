# SKY TOWER WORLDGEN — MINECRAFT PLACEMENT SEMANTICS
## Independent Source-Level Analysis of Jigsaw Placement & Height Boundaries

**Date:** 2026-11-09  
**Engineer:** Independent World-Generation Validation Engineer (Fabric 26.3 / Minecraft 1.21)  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Reference Branch:** `feature/sky-tower-integration-02` (`5c22f9fb`)  
**Validation Branch:** `validation/sky-tower-mountain-height`  

---

## 1. Executive Summary of Placement Semantics

This document formally establishes the exact runtime placement semantics of the Sky Tower composite structure within the Minecraft Fabric 26.3 world-generation pipeline.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       MINECRAFT WORLDGEN PIPELINE                            │
│                                                                             │
│  [Structure Placement]                                                      │
│     │                                                                       │
│     ├─► Heightmap Sampling: WORLD_SURFACE_WG at chunk center ───────► terrainY│
│     │                                                                       │
│     ├─► HeightProvider Sampling: uniform(18..70) ──────────────────► offset │
│     │                                                                       │
│     ├─► Piece Position: piece_minY = terrainY + offset                      │
│     │                                                                       │
│     ├─► Single Composite Piece Bounding Box:                                │
│     │      minY = piece_minY                                                │
│     │      maxY = piece_minY + 96                                           │
│     │                                                                       │
│     └─► JigsawPlacement Boundary Assertion:                                 │
│            Is (minY < -64 + 4) OR (maxY > 320 - 4)?                         │
│               ├── YES ──► REJECT PIECE (start.isValid() = false, NO SPAWN)  │
│               └── NO  ──► ACCEPT PIECE (generate in surface_structures)     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Placement Parameters & Mechanics

### 2.1 `start_height` Mode: Relative vs. Absolute
- **Configuration in `sky_island_mountain_*.json`:**
  ```json
  "project_start_to_heightmap": "WORLD_SURFACE_WG",
  "start_height": {
    "type": "minecraft:uniform",
    "min_inclusive": { "absolute": 18 },
    "max_inclusive": { "absolute": 70 }
  }
  ```
- **Semantics:**
  - In Minecraft's `Structure` codec, when `project_start_to_heightmap` is **present**, `start_height` operates as a **relative offset** added to the heightmap value.
  - The `{ "absolute": 18 }` syntax within `min_inclusive` is a `VerticalAnchor` specifying an integer delta of 18 blocks (as opposed to `above_bottom` or `below_top`).
  - **CONFIRMED FACT:** The structure start Y is computed as:
    $$\text{structure\_start\_y} = \text{WORLD\_SURFACE\_WG}(x, z) + \text{sample}(\text{uniform}(18, 70))$$
  - **Empirical Proof:** In C-4 flat world test evidence (`docs/planning/sky_tower_integration_02/phase_c4_evidence/desert_7001.json`), the flat world surface was at $Y = -60$. The recorded piece bounding box was `minY = -16, maxY = 80`. Because $-60 + 44 = -16$, the sampled offset was $44$, proving direct relative addition to surface elevation.

### 2.2 Heightmap Projection: `WORLD_SURFACE_WG`
- **Definition:** `WORLD_SURFACE_WG` queries the highest motion-blocking or non-air block during the world-generation phase prior to structure decoration.
- **Mountain Biome Elevations:**
  - Over standard lowlands (desert, plains, ocean), `WORLD_SURFACE_WG` evaluates to $Y = 62 \dots 80$.
  - In Mountain biomes (`minecraft:meadow`, `cherry_grove`), terrain reaches $Y = 90 \dots 160$.
  - In Extreme Mountain biomes (`minecraft:stony_peaks`, `snowy_slopes`, `frozen_peaks`, `jagged_peaks`), terrain regularly reaches $Y = 180 \dots 240+$, with mountain spires reaching up to $Y = 256$.

### 2.3 Shared Structure Bounding Box & Composite Extent
- **Template Architecture:**
  In `scripts/structures/sky_islands/tower_composite.py`, the island terrain base, stone slab bridge, and the 80-block Sky Tower are composed into a **single unified Jigsaw piece**:
  ```python
  COMPOSITE_SIZE = (58, 97, 49)
  TOWER_SIZE = (50, 80, 49)
  TOWER_ORIGIN = (0, 17, 0)
  ```
- **Voxel Span:**
  - Local $Y = 0 \dots 16$ (17 blocks): Natural floating island underside and terrain.
  - Local $Y = 16$ (at $(36, 16, 24)$): Preserved root Jigsaw connector (terminated with `minecraft:empty`).
  - Local $Y = 17$: Tower entrance and base level (`site.y = piece_minY + 17`).
  - Local $Y = 17 \dots 96$ (80 blocks): Sky Tower body, 4 combat floors, and rooftop sanctuary.
  - Local $Y = 96$: Crown of the 12 rooftop arches, capped with `minecraft:smooth_stone`.
- **Bounding Box Equality:**
  - The island and tower **share exactly one piece bounding box**.
  - Vertical height span:
    $$\Delta Y = 96 - 0 + 1 = 97\text{ blocks}$$
  - There are no separate child tower pieces; the legacy multi-piece Jigsaw chain was completely eliminated in INTEGRATION-02.

### 2.4 Invariance Under Horizontal Rotations
- **Minecraft Jigsaw Rotations:**
  `PoolElementStructurePiece` applies rotations via `net.minecraft.world.level.block.Rotation` (`NONE`, `CLOCKWISE_90`, `CLOCKWISE_180`, `COUNTERCLOCKWISE_90`).
- **Mathematical Transformation:**
  Rotation transforms coordinates in the horizontal $XZ$ plane only:
  $$(x', z') = R_\theta(x, z), \quad y' = y$$
- **CONFIRMED FACT:** The vertical bounding box coordinates (`minY` and `maxY`) are **100% invariant** across all 4 rotations:
  $$\text{minY}(\theta) = \text{piece\_minY}, \quad \text{maxY}(\theta) = \text{piece\_minY} + 96 \quad (\forall \theta \in \{0^\circ, 90^\circ, 180^\circ, 270^\circ\})$$
  This was verified across C-4 runtime captures:
  - Rotation 0 (`badlands_7102.json`): $Y = 48 \dots 144$ ($\Delta Y = 97$).
  - Rotation 1 (`desert_7001.json`): $Y = -16 \dots 80$ ($\Delta Y = 97$).
  - Rotation 2 (`desert_7202.json`): $Y = 3 \dots 99$ ($\Delta Y = 97$).
  - Rotation 3 (`snowy_7103.json`): $Y = 23 \dots 119$ ($\Delta Y = 97$).

---

## 3. Dimension Legal Boundaries & Engine Overflow Behavior

### 3.1 Legal Build Limits in Overworld (Minecraft 1.21 / 26.3)
- $\text{minBuildHeight} = -64$
- $\text{maxBuildHeight} = 320$
- Legal block coordinate range: $-64 \le Y \le 319$.

### 3.2 `dimension_padding` Assertion in `JigsawPlacement`
- All six mountain structure JSON files specify:
  ```json
  "dimension_padding": 4
  ```
- In Minecraft 26.3 `JigsawPlacement.addPieces`:
  When a pool element is evaluated for placement, its bounding box is checked against the dimension limits padded by `dimension_padding`:
  $$\text{minAllowedY} = \text{minBuildHeight} + \text{dimension\_padding} = -64 + 4 = -60$$
  $$\text{maxAllowedY} = \text{maxBuildHeight} - \text{dimension\_padding} = 320 - 4 = 316$$
- **The Assertion Rule:**
  $$\text{If } (\text{pieceBox.maxY}() > 316) \implies \text{REJECT PIECE}$$

### 3.3 What Actually Happens When `maxY > 316`: Rejection vs. Clipping
Two distinct engine failure modes exist depending on whether the check occurs at structure assembly or voxel rasterization:

1. **Failure Mode 1 — Structure Candidate Rejection (`JigsawPlacement`):**
   - Because the Sky Tower is the **root piece** of the structure start, if `pieceBox.maxY() > 316`, `JigsawPlacement` fails to place the root piece.
   - `StructureStart.getPieces()` remains empty.
   - The structure start is invalid (`start.isValid() == false`).
   - **RESULT:** The structure candidate is **silently aborted**. No blocks are placed, no island spawns, and no tower appears in the world.
   - **CONFIRMED IN PRESERVED AUDIT:** In `docs/audit/BRANCH-W11-20261027/preserved/4e511a90e849378defab9c38395a7b4fadfa8b49.md` §5.4, testing demonstrated that when a start piece's bounding box violated `dimension_padding`, the structure lost $18\%$ to $36\%$ of its starts (`start absent`), whereas removing padding (`dimension_padding: 0`) allowed $100\%$ of starts to generate.

2. **Failure Mode 2 — Block Slicing / Chunk Storage Truncation (`LevelChunkSection`):**
   - If an unpadded structure or forced placement places blocks at $Y \ge 320$:
   - In Minecraft's chunk storage, each chunk holds exactly 24 `LevelChunkSection` arrays (each $16 \times 16 \times 16$), spanning $Y = -64$ to $Y = 319$. There is no section at $Y = 320$.
   - `LevelChunk.setBlockState(pos)` checks `isOutsideBuildHeight(pos)`. For any position where $Y \ge 320$, the call returns immediately without modifying storage.
   - **RESULT:** Physical block truncation (clipping). The arches, floating relic, or upper combat floors would be sliced off at the $Y = 319$ boundary.

---

## 4. Master Semantics Conclusion

| Question | Source-Level Answer | Empirical Status |
|---|---|---|
| Is `start_height` relative or absolute? | **Relative** (added to `WORLD_SURFACE_WG`) | **CONFIRMED FACT** |
| Do island and tower share one bounding box? | **Yes** (Single composite piece `58x97x49`) | **CONFIRMED FACT** |
| Exact vertical extent of composite NBT? | **97 blocks** (Local Y 0 to 96) | **CONFIRMED FACT** |
| Do rotations change vertical bounds? | **No** (Y bounds are strictly invariant) | **CONFIRMED FACT** |
| Dimension legal block bounds? | **Y = -64 to 319** (Ceiling at Y = 320) | **CONFIRMED FACT** |
| Engine behavior on $Y > 316$? | **Candidate Rejection** (`start.isValid() = false`) | **CONFIRMED BY CODE & HISTORICAL AUDIT** |
| Engine behavior on $Y \ge 320$ if forced? | **Hard Block Clipping** (Chunk storage boundary) | **CONFIRMED ENGINE BEHAVIOR** |
