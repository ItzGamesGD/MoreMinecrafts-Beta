# Sky Tower — Corrected Height Model (Phase 2)

Evidence labels: `RUNTIME_CONFIRMED` · `SOURCE_CONFIRMED` · `MATHEMATICALLY_DERIVED` · `INFERRED` · `UNVERIFIED` · `BLOCKED`.
Phase 1 reports are preserved unchanged; this file corrects them where the runtime or the engine source disagrees.

## 1. The model

Terrain conventions used throughout (they matter by exactly one block):

* **T** = Y of the top surface *block* at the structure-piece centre (what F3 shows). `RUNTIME_CONFIRMED`
* **H** = `WORLD_SURFACE_WG` first-free height = `T + 1` (what `getFirstFreeHeight` returns). `SOURCE_CONFIRMED`

Engine arithmetic (decompiled `JigsawPlacement.addPieces`, `StructurePoolElement.getGroundLevelDelta()==1`) — `SOURCE_CONFIRMED`:

```
bottomY   = start_height + H
root.minY = bottomY - groundLevelDelta(=1) = start_height + T
root.maxY = root.minY + sizeY - 1
REJECT iff root.minY < level.getMinY() + pad.bottom   or   root.maxY > level.getMaxY() - pad.top
```

`LevelHeightAccessor.getMaxY()` is **inclusive** (`getMinY() + getHeight() - 1`). For the Overworld (`min_y=-64`, `height=384`) it is **319**, so with `dimension_padding: 4`:

```
highest accepted root.maxY = 319 - 4 = 315          (NOT 316)
accepted  <=>  start_height <= 315 - T - (sizeY-1)
tower (sizeY = 97):  start_height <= 219 - T   (= 220 - H)
```

`RUNTIME_CONFIRMED` (real 26.3 server, probe `SkyTowerPhase2Probe`):

| Check | Result |
|---|---|
| `level.getMinY()` / `level.getMaxY()` read from the live server | `-64` / `319` |
| Engine debug log text for a rejected tower | `... BoundingBox{minY=226 ... maxY=322 ...} does not fit dimension padding DimensionPadding[bottom=4, top=4]` |
| Replay of the root-piece arithmetic vs engine `Structure.generate` outcome | **0 disagreements** in 197,812 accepted + 1,062 padding-rejected + 39 stub-biome-rejected attempts (attempt counts pool the 12 scan runs, 8 full runs and 5 flat runs; seeds 2–13 recur in scan and full runs, so these are not independent samples) |
| Root piece `minY == start_height + T` and `maxY == minY + sizeY - 1` for every accepted start (engine piece box vs replica) | 197,812 / 197,812 |
| Formula `accepted <=> start <= 315 - T - (sizeY-1)` on every eligible production attempt (12 natural seeds) | 116,955 attempts, **0 mismatches** |
| Same formula on every attempt of the controlled elevation sweep | 143,289 attempts, **0 mismatches** |
| Tower attempts by computed `maxY` (natural overworld, 12 seeds) | `maxY ≤ 315`: 0 padding-rejected (17 accepted at exactly 315); `maxY = 316`: **19 / 19 rejected**; `317…330`: all rejected. `valid_with_maxY>315 = 0` |

## 2. Verdict on the Phase 1 equation

The task text asks to verify `start_height <= 220 - terrainY` for an "effective maximum 316".

* The **inequality is numerically correct only if `terrainY` means H** (`220 - H == 219 - T`). `MATHEMATICALLY_DERIVED`
* Its stated ingredients are both off by one in compensating directions: the real limit on `maxY` is **315**, not 316, and `maxY = T + start + 96` (not `H + start + 96`). So `piece_max_y = terrainY + start_height + 96` with a 316 limit **over-accepts by one roll when terrainY is the surface-block Y**. `RUNTIME_CONFIRMED` (`maxY=316` is rejected 19/19).
* Phase 1 `HEIGHT_BOUNDARY_MATRIX.csv` uses `terrainY = T` (piece_min = terrain + start). Applying the 316 limit there understates every failure rate by one roll (e.g. T=160, 18..70: Phase 1 says 10/53 fail; engine: **11/53**; T=200: Phase 1 3/53 pass, engine **2/53**). `MATHEMATICALLY_DERIVED`, consistent with the runtime boundary above.

## 3. Element heights (NBT, read through the engine's `StructureTemplateManager`)

`RUNTIME_CONFIRMED` (`elements.csv` in every run; identical for all six styles):

| Element | Size X×Y×Z | sizeY | Phase 1 stated |
|---|---|---|---|
| `tower` | 58×97×49 | **97** | 97 ✔ |
| `temple` | 81×45×57 | **45** | 27 ✘ |
| `plain_medium` | 45×27×35 | **27** | 21 ✘ |
| `ruins` | 36×27×33 | **27** | 19 ✘ |
| `plain_small_b` | 27×24×23 | **24** | 17 ✘ |
| `plain_small_a` | 25×23×25 | **23** | 16 ✘ |

Phase 1 used the local max-Y of the island *body*, not the piece bounding-box height; its "100 % for every non-tower element up to T=220" and "overall island success" table are therefore wrong at high terrain (below).

## 4. Highest terrain that can still host each element (`MATHEMATICALLY_DERIVED`, formula verified above)

| Element (sizeY) | mountain 18..70: always accepted for T ≤ | mountain: ever accepted for T ≤ | general 40..110: always T ≤ | general: ever T ≤ |
|---|---|---|---|---|
| tower (97) | **149** | **201** | 109 | 179 |
| temple (45) | 201 | 253 | 161 | 231 |
| plain_medium / ruins (27) | 219 | 271 | 179 | 249 |
| plain_small_b (24) | 222 | 274 | 182 | 252 |
| plain_small_a (23) | 223 | 275 | 183 | 253 |

Runtime check: non-tower islands *are* rejected on natural high terrain — `temple` 6 and `ruins` 7 padding rejections over 12 seeds, and 318/745 non-tower candidates fail on the flat T=240 natural world (`flat_jag_T240`: 427 generated).

## 5. Acceptance probability of a selected tower, uniform start range, per terrain T

`P = clamp((min(hi, 219-T) - lo + 1) / (hi - lo + 1), 0, 1)`. Terrain values requested by the task are treated as **T** (top block); H is `T+1`.

| Range | 80 | 120 | 140 | 160 | 180 | 200 | 220 | 240 | 260 | always ≤ T | ever ≤ T |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **current 18..70** | 100 | 100 | 100 | 79.2 | 41.5 | 3.8 | 0 | 0 | 0 | 149 | 201 |
| Phase 1 proposal **15..35** | 100 | 100 | 100 | 100 | 100 | **23.8** | 0 | 0 | 0 | 184 | 204 |
| 18..35 | 100 | 100 | 100 | 100 | 100 | 11.1 | 0 | 0 | 0 | 184 | 201 |
| 10..30 | 100 | 100 | 100 | 100 | 100 | 47.6 | 0 | 0 | 0 | 189 | 209 |
| 5..20 | 100 | 100 | 100 | 100 | 100 | 93.8 | 0 | 0 | 0 | 199 | 214 |
| 1..18 | 100 | 100 | 100 | 100 | 100 | 100 | 0 | 0 | 0 | 201 | 218 |
| 1..10 | 100 | 100 | 100 | 100 | 100 | 100 | 0 | 0 | 0 | 209 | 218 |

(percent; full table with Phase 1 comparison: `python3 -I tools/sky-tower-phase2/height_model.py`.)

Required clearance `c` (island underside above the surface block = `start_height`) bounds the feasible terrain for a 97-block piece: `T ≤ 315 - c - 96` → c=1: **218**, c=10: 209, **c=18: 201**, c=35: 184, c=70: 149. Even `c = 0` is impossible above T = 219.

### Runtime agreement (controlled elevation sweep, `CONTROLLED_REPLAY`, 797 mountain tower draws per terrain cell)

Per-row the formula is exact (0 mismatches). Aggregate acceptance matches the iid-uniform model to within a few points (e.g. 18..70 at T=160: 652/797 = 81.8 % vs 79.2 %; at T=200: 23/797 = 2.9 % vs 3.8 %; 15..35 at T=200: 236/797 = 29.6 % vs 23.8 %). Every cell reuses the same 797 chunks, so the offsets are one shared sample of start-height rolls (their empirical distribution passes χ² vs uniform: 60.2 < 69.8 at df 52), not a model error. `RUNTIME_CONFIRMED`.

## 6. Phase 1 claims — adjudication

| Phase 1 claim | Verdict | Basis |
|---|---|---|
| Root-piece rejection (not clipping) in natural generation; start invalid | **RUNTIME_CONFIRMED** | 1,062 engine rejections, all with no start; natural pipeline agrees on 85,154 candidates |
| `dimension_padding:4` ⇒ rejection above Y=316 | **REJECTED (off by one)** | limit is maxY ≤ 315; 316 rejected 19/19 |
| "Over 15..35 restores generation success to **> 95 %**" | **REJECTED** | Undefined baseline and unsupported. Natural data: production already accepts 96.7 % of mountain towers overall (2,611/2,699), so there is nothing to "restore" to 95 % globally. On the failure range it is false: 15..35 accepts 23.8 % at T=200 (model; 29.6 % observed sample), 56.7 % (34/60) of natural towers at T ≥ 180, and 0 % at T ≥ 220. No replacement percentage is offered |
| Sky Tower success 5.7 % at T=200; 0 % at T ≥ 220 | **REJECTED as stated** (model gives 3.8 % at 200; 0 % ≥ 220 holds) | formula correction §2 |
| "100 % rejection above Y ≥ 220 (jagged peaks)" | **MATHEMATICALLY_DERIVED ✔ / not naturally common** | only 7 of 3,850 natural mountain tower candidates sat at T ≥ 220 in 12 seeds |
| Non-tower islands succeed 100 % up to T=220 / immune | **REJECTED** | actual sizes 23–45; temple fails from T ≥ 202, plains/ruins from T ≥ 220; observed natural rejections |
| General (40..110) variants "immune ≤ Y=100, only fail if forced over ≥ 160" | **REJECTED** | tower fails from T ≥ 110: 78/1,244 natural general towers at T 100–139; `flat_desert_T120` natural: 4/39 |
| Meadow variant "moderately affected (140–170)" | **REJECTED (empirically)** | natural `mountain_meadow` towers: 1,542 accepted, 0 padding rejections (1 stub-biome rejection) |
| Forced placement above 224 clips at Y=320 | **RUNTIME_CONFIRMED** | forced: `maxY=319` full fit, `320` clips 1 layer, `330` clips 11 |
| Option A "pure data fix, no Java/NBT" | **SOURCE/RUNTIME_CONFIRMED as to mechanism**; its coverage claim rejected above | datapack-override natural run, §Benchmark |
| Case M-1 (seed 42, jagged_peaks, T=210) / M-2 (seed 1337, frozen_peaks, T=190) as observed | **UNVERIFIED (hypothetical)** | no coordinates; not re-run. Equivalent controlled flat worlds were run instead |
| 1,536-sample historical `dimension_padding` audit (18–36 % starts absent) | **UNVERIFIED** | not re-inspected in Phase 2 |
| Phase 1 "runtime evidence" | **Source-level simulation, not runtime** | Phase 1 itself records that the server could not be launched |
