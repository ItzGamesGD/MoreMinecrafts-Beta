# SKY TOWER WORLDGEN — GENERATION DISTRIBUTION & RARITY IMPACT
## Mathematical Analysis of Elevation-Dependent Structure Selection & Selective Rejection

**Date:** 2026-11-09  
**Engineer:** Independent World-Generation Validation Engineer (Fabric 26.3 / Minecraft 1.21)  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Reference Checkpoint:** `5c22f9fb764d545f0ce4241c75bc47f5b54e447d`  

---

## 1. Overview: The Asymmetric Height Problem

In the Sky Islands world-generation design, all island structures within a biome share a common start pool:
`src/main/resources/data/moreminecrafts/worldgen/template_pool/sky_islands/<biome>/start.json`

Every structure start rolled in that biome draws from this pool with fixed weights. However, while all non-tower elements are low-profile floating landmasses (16 to 27 blocks tall), the Sky Tower composite structure is a **massive 97-block vertical monolith**.

When placed over high mountain terrain, this vertical disparity creates an acute **elevation-dependent survival filter**, leading to **selective structure extinction** for the Sky Tower while leaving other island types unaffected.

---

## 2. Template Vertical Profile Comparison

| Element Location | In-Game Element | Pool Weight | Nominal Share | Physical NBT Size | Max Local Y | Total Vertical Height ($\Delta Y$) |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `plain_small_a` | Small Plain Island A | 3 | 14.3% | $25 \times 23 \times 25$ | 15 | **16 blocks** |
| `plain_small_b` | Small Plain Island B | 3 | 14.3% | $27 \times 24 \times 23$ | 16 | **17 blocks** |
| `plain_medium` | Medium Plain Island | 4 | 19.0% | $45 \times 27 \times 35$ | 20 | **21 blocks** |
| `ruins` | Floating Sky Ruins | 6 | 28.6% | $36 \times 27 \times 33$ | 18 | **19 blocks** |
| `temple` | Floating Sky Temple | 2 | 9.5% | $81 \times 45 \times 57$ | 26 | **27 blocks** |
| **`tower`** | **Sky Tower Composite** | **3** | **14.3%** | **$58 \times 97 \times 49$** | **96** | **97 blocks** |
| **TOTAL** | — | **21** | **100.0%** | — | — | — |

The Sky Tower composite is **3.6× to 6.0× taller** than any other structure in the pool.

---

## 3. Placement Success Probability Across Terrain Elevations

Under configured parameters for Mountain Sky Islands (`start_height: uniform 18..70`, `dimension_padding: 4`, legal ceiling: $Y \le 316$):

$$\text{Success Condition: } \text{terrainY} + \text{start\_height} + (\Delta Y - 1) \le 316$$

$$\text{Maximum Safe Offset: } \text{offset}_{\text{safe}} = 316 - \text{terrainY} - \Delta Y + 1$$

| Terrain Surface Elevation | `plain_small_a` (H=16) | `plain_small_b` (H=17) | `plain_medium` (H=21) | `ruins` (H=19) | `temple` (H=27) | **`tower` (H=97)** | Overall Island Success |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **$Y = 80$ (Lowland)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **100.0%** | **100.0%** |
| **$Y = 120$ (Hill)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **100.0%** | **100.0%** |
| **$Y = 140$ (Lower Mountain)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **100.0%** | **100.0%** |
| **$Y = 160$ (Mid Mountain)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **81.1%** | **97.3%** |
| **$Y = 180$ (High Mountain)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **43.4%** | **91.9%** |
| **$Y = 200$ (Mountain Ridge)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **5.7%** | **86.5%** |
| **$Y = 220$ (Jagged Peak)** | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | **0.0% (EXTINCT)** | **85.7%** |
| **$Y = 240$ (Extreme Spire)** | 83.0% | 81.1% | 73.6% | 77.4% | 62.3% | **0.0% (EXTINCT)** | **65.5%** |

---

## 4. Gameplay & Distribution Consequences

### 4.1 Selective Extinction of the Pinnacle Feature
1. **Lowlands & Meadows ($Y \le 140$):**
   - The Sky Tower generates at its full intended frequency of $14.3\%$ of sky island starts.
2. **High Peaks & Jagged Ridges ($Y \ge 200$):**
   - When the Jigsaw generator selects a plain island, ruin, or temple, the structure comfortably stays between $Y = 236$ and $Y = 297$, placing with **100.0% reliability**.
   - But when the generator selects the **Sky Tower** (a 3 in 21 chance), the structure attempts to reach $Y = 336 \dots 366$. `JigsawPlacement` detects the bounding box exceeding $Y = 316$ and **rejects the piece**.
   - As a result, the entire structure start is discarded.
3. **The Resulting Distribution Distortion:**
   - Players traversing jagged alpine peaks will see floating ruins and floating plains, but the signature pinnacle challenge (Sky Tower) is **mathematically erased** from high mountain ridges.
   - The effective ratio of Sky Towers in alpine zones collapses from $1 : 7$ to **$1 : 120$ or zero**.

### 4.2 Impact on Structure Set Density
In `sky_islands_mountain.json`:
- Placement uses `spacing: 14`, `separation: 5`, `frequency: 0.5`.
- At $Y \ge 220$, because every tower selection ($14.3\%$) fails placement, approximately **$1$ out of every $7$ scheduled mountain sky island starts completely vanishes**, resulting in unexplained vacant sky gaps over mountain peaks.

---

## 5. Summary Conclusion

The mountain height overflow issue is **NOT a cosmetic block clipping flaw**. It is a **selective generation suppression filter** that:
- Destroys the intended feature parity across elevations.
- Severely reduces Sky Tower availability in high-altitude mountain biomes (`frozen_peaks`, `jagged_peaks`, `stony_peaks`).
- Distorts the core progression loop by denying players the ability to discover the tower in the most dramatic atmospheric settings.
