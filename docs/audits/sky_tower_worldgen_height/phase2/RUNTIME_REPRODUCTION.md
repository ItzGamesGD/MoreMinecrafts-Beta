# Sky Tower — Runtime Reproduction (Phase 2)

**Status: RUNTIME REPRODUCED — not BLOCKED.** A real Minecraft 26.3 / Fabric dedicated server was launched (JDK 25.0.5, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Gradle 9.6.0 wrapper) for every run below. No production Java, worldgen JSON, NBT or gameplay file was modified.

Base: `validation/sky-tower-mountain-height` @ `038eaef6872e90961afdbf8ccab2581a275332a1` (verified; working tree clean at start).
Labels: `RUNTIME_CONFIRMED` · `SOURCE_CONFIRMED` · `MATHEMATICALLY_DERIVED` · `INFERRED` · `UNVERIFIED` · `BLOCKED`.

## 1. Method and evidence classes

All evidence-producing code is the isolated test-source-set probe `src/gametest/java/.../worldgenprobe/SkyTowerPhase2Probe.java` (inert unless `-Dmm.p2.out` is set; registered only in the gametest `fabric.mod.json`) driven by `tools/sky-tower-phase2/run_probe.sh`.

| Class | What it is | Used for |
|---|---|---|
| **NATURAL** | Real chunk pipeline: `level.getChunk(cx, cz, STRUCTURE_STARTS)` on a fresh world, then reading `chunk.getAllStarts()`; top blocks read after `FULL` generation | ground truth of "is a valid start present" |
| **REPLAY** | The engine's own `Structure.generate(...)` called for every placement-selected chunk of both Sky Island structure sets, using a copy of `ChunkGenerator.createStructures`' weighted retry loop, with JigsawPlacement's own DEBUG log captured | per-candidate trace; rejection reasons |
| **CONTROLLED_REPLAY** | Same, on an in-process `FlatLevelSource` subclass with an exact fixed surface height and pinned biome (vanilla flat caps its surface at top-block Y=255, `FlatLevelSource.getBaseHeight` indexes layers with `getMaxY()`, so T=260 needs the override) | elevation sweep 80…260 |
| **NATURAL (flat)** | Real fresh flat worlds with an exact layer list and pinned biome (`flat_*`) | controlled terrain through the real pipeline |
| **NATURAL (datapack)** | Real fresh flat world with a disposable *world* datapack that overrides one structure JSON (no repo file touched) | one candidate remediation through the real pipeline |
| **FORCED** | `StructureTemplate.placeInWorld` of the tower at a chosen Y (bypasses Jigsaw/padding) | physical clipping at the ceiling |

Independent cross-check (Phase 6) = REPLAY's arithmetic **re-derived separately** (`replicate()`: height, rotation, element, box, `getFirstFreeHeight`, padding, stub biome) vs the engine's real `generate` result, vs the NATURAL pipeline, vs the engine log.

## 2. Phase 0 questions — answers

| # | Question | Answer | Label |
|---|---|---|---|
| 1 | Root-piece bounding-box validation | `JigsawPlacement.addPieces` moves the root piece (`minY = start_height + H - groundLevelDelta`), then `isStartTooCloseToWorldHeightLimits(heightAccessor, dimensionPadding, rootBox)`; true ⇒ `Optional.empty()` | SOURCE + RUNTIME |
| 2 | `dimension_padding: 4` | a single value ⇒ `DimensionPadding[bottom=4, top=4]` (engine log prints exactly this); reject iff `minY < levelMinY + 4` or `maxY > levelMaxY − 4` | SOURCE + RUNTIME |
| 3 | Legal inclusive maximum Y | `getMaxY()` is inclusive (319 read from the live server). Highest accepted root `maxY` = **315**; 17 natural towers accepted at exactly 315, 19/19 at 316 rejected. Physically the world holds blocks to 319 (forced `maxY=319` fits all 97 layers) | RUNTIME |
| 4 | Failed root piece ⇒ structure start rejected? | Yes. All 1,062 padding-rejected attempts returned `INVALID_START`; no start appears in the NATURAL pipeline (85,154 candidates compared, 0 disagreements) | RUNTIME |
| 5 | Pool-element selection vs height sampling independent? | They are consecutive draws of one `WorldgenRandom` (`setLargeFeatureSeed(seed, chunkX, chunkZ)`): height first, then `Rotation.getRandom`, then `getRandomTemplate`. No code couples them. Tower share by start-height bin: χ² = 1.29 (df 3, crit 7.81) on 27,189 candidates ⇒ no dependence detected | SOURCE + RUNTIME-derived |
| 6 | Are alternative elements retried after a selected element fails? | **No.** `getRandomTemplate` is called once; failure returns empty. The only retry is `createStructures`' loop over the *other structures of the set*, which restart from the identical seed: height, rotation and element kind were identical across variants in **22,247 / 22,247** mountain candidates, so a rejected tower is rejected again (and the other variants fail the biome test anyway) | SOURCE + RUNTIME |

Additional engine facts found (not in Phase 1):

* The biome test is applied to the **stub position at sky altitude** (`context.isValidBiome(stub)`, quart of `(centerX, bottomY, centerZ)`), not at the surface. 39 attempts were rejected only for this (`STUB_BIOME_REJECT`); 67 of 27,189 mountain candidates have a stub biome different from the surface biome. `RUNTIME_CONFIRMED`
* The cheap pre-check `couldStructureExistInColumn` uses the footprint centre of the *drawn element*, so a wide element can miss a narrow biome patch that a small one would hit. `SOURCE_CONFIRMED`; effect size not isolated (`INFERRED`: tower-only acceptance is 82–90 % even on low terrain, independent of height range).
* `max_distance_from_center: 80` expands to horizontal **and** vertical 80 (irrelevant for the single-piece root). `SOURCE_CONFIRMED`
* The general set's `exclusion_zone` is evaluated by `isStructureChunk` of the mountain *placement*, i.e. independent of whether the mountain candidate generated, so a rejected mountain candidate does not free its neighbourhood for general islands. `SOURCE_CONFIRMED`, not run.

## 3. Runs and sample sizes

| Run set | Seeds | World | Candidates (placement-selected chunks) | Eligible (biome column passes) | Natural vs replay |
|---|---|---|---|---|---|
| `scan_*` (REPLAY only) | 2–13 | natural Overworld, radius 2,500 chunks (≈6,403 km² each) | 945,977 | 116,950 | — |
| `full_*` (NATURAL + REPLAY + strategies) | 2,3,4,5,6,7,8,9 | natural Overworld, radius 2,500 | 630,833 | 78,261 | 81,461 checked, **0 disagreements** |
| `flat_*` (NATURAL flat) | 31 | flat, pinned biome, exact T: jagged_peaks 160/200/240, meadow 180, desert 120 | 3,693 | 3,693 | 3,693 checked, **0 disagreements** |
| `controlled` (CONTROLLED_REPLAY) | 4242 (placements) | in-process flat, 4 biomes × 9 terrains | 6,487 per terrain cell (radius 700) | 5,307 in the jagged_peaks cell | — |
| `dp_A1535_jag_T200` (NATURAL, datapack) | 31 | flat jagged_peaks T=200 | 865 | 865 | 865 checked, 0 disagreements |
| `forced` | — | flat | 7 forced placements + 6 reference | — | — |

Seeds 2–13 recur in `scan_*` and in `full_*` (identical replay); they are not independent samples. Pooled natural statistics in the other documents use the 12 `scan_*` seeds for production behaviour and the 8 `full_*` seeds for paired strategy comparison.

## 4. Natural runtime observations

### 4.1 Rejection reproduced and its mechanism (`RUNTIME_CONFIRMED`)

Engine DEBUG log, captured verbatim from the real server (`full_9`, seed 9; general set, structure `sky_island_forest`, chunk (-2368, 1022)):

```
Center piece Single[Left[moreminecrafts:sky_islands/forest/tower]] with bounding box
BoundingBox{minX=-37936, minY=229, minZ=16352, maxX=-37888, maxY=325, maxZ=16409}
does not fit dimension padding DimensionPadding[bottom=4, top=4]
```

Earlier smoke run, seed 1: `minY=226, maxY=322` on terrain top 159 with sampled `start_height` 67 (159+67=226 ✔). In the full runs every padding rejection appears once on a worldgen worker thread (NATURAL) and once from the probe's own server-thread replay, 823 unique boxes, identical text; 2 boxes appear only on the replay side because those spawn-area chunks were generated before the log capture was installed.

### 4.2 Natural production behaviour (12 seeds, mountain set, first eligible attempt; `PLACEMENT_TRACE.csv` has the rows)

| Terrain top block T | eligible candidates | tower selected | tower accepted | tower rejected | non-tower accepted / selected |
|---|---|---|---|---|---|
| < 100 | 8,393 | 1,159 | 1,159 | 0 | 7,229 / 7,234 |
| 100–139 | 14,507 | 2,049 | 2,047 | 0 | 12,457 / 12,458 |
| 140–159 | 2,774 | 443 | 426 | 17 | 2,331 / 2,331 |
| 160–179 | 1,042 | 125 | 83 | 42 | 917 / 917 |
| 180–199 | 330 | 52 | 14 | 38 | 278 / 278 |
| 200–219 | 104 | 15 | 0 | 15 | 87 / 89 |
| 220–239 | 33 | 6 | 0 | 6 | 22 / 27 |
| ≥ 240 | 6 | 1 | 0 | 1 | 1 / 5 |

(The two tower non-padding losses at T<140 and the non-tower losses at low T are stub-biome rejections.)

By surface biome (mountain-set towers, 12 seeds): meadow 1,257/1,257, windswept_hills 300/300, cherry_grove 280/280, windswept_gravelly 106/106, grove 768/784, snowy_slopes 423/429, windswept_forest 205/206, **frozen_peaks 159/199, jagged_peaks 131/166, stony_peaks 88/109**.

By variant (attempt level, all runs): `mountain_snowy` tower 97 padding rejections / 1,582; `mountain_rocky` 22 / 725; `mountain_meadow` 0 / 1,543; general `forest` 50 / 6,298, `badlands` 21 / 434, `meadow` 25 / 4,060, `snowy` 8 / 1,312, `desert` 0 / 623. All three mountain variants and all five general variants were exercised.

### 4.3 Controlled terrain through the real pipeline (`flat_*`, NATURAL)

| World | mountain/general candidates | towers selected | towers actually generated (natural) | non-tower generated |
|---|---|---|---|---|
| flat jagged_peaks T=160 | 865 | 120 | **98** (81.7 %) | 745 / 745 |
| flat meadow T=180 | 865 | 120 | **47** (39.2 %) | 745 / 745 |
| flat jagged_peaks T=200 | 865 | 120 | **6** (5.0 %) | 745 / 745 |
| flat jagged_peaks T=240 | 865 | 120 | **0** | **427 / 745** |
| flat desert T=120 (general set, lowland-style control) | 233 | 39 | 35 (89.7 %) | 194 / 194 |

Natural vs REPLAY agree on every candidate of every run. Model check: T=120 general range 40..110 ⇒ rolls 100..110 fail ⇒ expected ≈ 15.5 % tower loss; observed 4/39 = 10.3 % (n too small to distinguish).

### 4.4 Expected top blocks (`RUNTIME_CONFIRMED`)

45 naturally generated towers were taken to `FULL` and their rooftop layer (`box.maxY`) read: all 45 contain exactly the reference **64 `smooth_stone`** crown blocks (identical to the forced reference placement of every style). Extra blocks in the layer are natural snow on snowy towers only. Piece `maxY` of the samples ranged 220…315 and includes **11 towers at exactly 315** — i.e. a tower flush against the padding limit generates complete.

### 4.5 FORCED placement — physical clipping (`forced.csv`)

| target maxY | layers inside world | clipped layers | highest readable layer: non-air blocks placed vs reference placement of the same layer |
|---|---|---|---|
| 80 | 97 | 0 | 64 / 64 (crown) |
| 315 | 97 | 0 | 64 / 64 (crown) |
| 316 | 97 | 0 | 64 / 64 (crown) |
| **319** | 97 | 0 | 64 / 64 (crown) |
| **320** | 96 | **1** | 52 / 52 (Y=319 is local layer 95; crown layer lost) |
| 324 | 92 | 5 | 120 / 120 (local layer 91) |
| 330 | 86 | 11 | 42 / 42 (local layer 85) |

So: the Jigsaw padding (315) is **four blocks more conservative than physics** (319); anything placed by force at `maxY ≥ 320` loses its upper layers silently. `RUNTIME_CONFIRMED`; natural generation never reaches this path because the root is rejected first.

## 5. Phase-6 independent verification summary

| Approach A (runtime) | Approach B (independent re-derivation) | Result |
|---|---|---|
| Engine `Structure.generate` valid/invalid | replica `predicted` (ACCEPT / PADDING / STUB / COLUMN) | 0 disagreements over 5.0 M attempts incl. 4,805,399 column-miss, 1,062 padding, 39 stub |
| Engine piece box (valid starts) | replica `minY/maxY/element` | 197,812 / 197,812 match |
| NATURAL chunk-pipeline starts | REPLAY loop | 85,154 / 85,154 agree |
| Engine log bounding boxes (worker threads) | replica-predicted rejected boxes | 823 unique, set equality (natural ⊆ replay) |
| Real formula check | `start ≤ 315 − T − (sizeY−1)` | 0 mismatches / 260,244 attempt rows (natural 116,955 + controlled 143,289) |

No discrepancy needed investigation except the doubled worker/server log lines (explained: the probe's own replay).

## 6. Limitations (stated honestly)

* High natural peaks are rare: of 27,189 natural mountain-set candidates only 473 have T ≥ 180 (≈ 1.7 %); intervals in the high strata are wide (Rarity doc).
* Controlled-terrain *strategy* results are CONTROLLED_REPLAY (engine `generate` on an in-process generator), except the single datapack-override run, which is NATURAL.
* Strategies B (dynamic height), C-retry/relocate/substitute and D-tower-only are **prototype structures inside the probe**, delegating to the real `JigsawPlacement.addPieces`; they are not production code and are not registrable by data alone.
* Vault/Trial Spawner counts, barrier registration and bridge integrity were **not re-run** (`NOT RUN`): no production asset changed; see `REGRESSION_ASSESSMENT.md`.
* Phase 1's historical 1,536-sample audit and seeds 42/1337 (M-1, M-2) were not re-inspected (`UNVERIFIED`).

## 7. Reproduce

```bash
export JAVA_HOME=<JDK 25>
tools/sky-tower-phase2/scan_seeds.sh 2500 3 2 3 4 5 6 7 8 9 10 11 12 13        # REPLAY scan
tools/sky-tower-phase2/run_probe.sh full_9 9 world -Dmm.p2.radius=2500 -Dmm.p2.naturalMax=12000   # NATURAL+REPLAY+strategies
tools/sky-tower-phase2/run_probe.sh controlled 4242 controlled -Dmm.p2.radius=700
LEVEL_TYPE=minecraft:flat GENERATOR_SETTINGS='{"biome":"minecraft:jagged_peaks","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":264}]}' \
  tools/sky-tower-phase2/run_probe.sh flat_jag_T200 31 world -Dmm.p2.radius=300 -Dmm.p2.noStrategies=true
LEVEL_TYPE=minecraft:flat tools/sky-tower-phase2/run_probe.sh forced 777 forced
python3 -I tools/sky-tower-phase2/analyze.py build/sky-tower-phase2/results docs/audits/sky_tower_worldgen_height/phase2 \
  --scan scan_2 … scan_13 --full full_2 … full_9 --controlled controlled --flat flat_* --override flat_jag_T200 dp_A1535_jag_T200
```

(Flat layer count = `T + 64` stone + 1 bedrock gives top block T.)
