# Underground Kingdom — Worldgen Feasibility Probe (EXPERIMENTAL)

> **Status: experiment / evidence only.** This is **not design authority**, is not routed from
> `CONTENT_AUTHORITY_INDEX.md`, and authorizes no gameplay. All dimensions, depths and counts are **provisional**.
> Nothing here ships in the mod jar. Rewards use the placeholder **특수아이템(미정)**; the probe only reuses vanilla trial
> vault/spawner mechanics and the vanilla `chests/trial_chambers/reward` loot table as a stand-in.

## 0. Provenance

| Item | Value |
|---|---|
| Repository | `ItzGamesGD/MoreMinecrafts-Beta` |
| Base | `integration/stage23-final-reconciliation-20261016` @ `bdffa599e1197e66d60a99064dd17a0c8209b9ef` — the merge commit of **PR #104**. Its tree is **identical** (`git diff` empty) to PR #104's head `b0de6b754859e090427ff310887ca04188310a8a`. |
| Experiment branch | `experiment/underground-kingdom-worldgen-probe` (new; `main`, PR branches and `stage-*` checkpoints untouched) |
| Read before changing anything | `AGENTS.md`, `docs/CONTENT_AUTHORITY_INDEX.md` (no authority routes an underground-kingdom design; none was invented — the probe is a structure-feasibility test only) |
| Minecraft | **26.3** (`world_version` 5023, data pack format 121). Server jar SHA-1 `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c` |
| Toolchain | Temurin JDK 25.0.4.1, Fabric Loom 1.17.21, Fabric API 0.161.0+26.3 (from `gradle.properties`) |

Note on the base: the task named PR #104 and its "verified integration branch". #104 was merged into
`integration/stage23-final-reconciliation-20261016`, so that branch is used; since its tree equals #104's head it makes no
difference to the result.

## 1. Verdicts

Legend: **PASS** / **FAIL** / **NOT RUN**. "Automated" = produced by the headless harness in this PR. Nothing in this
document is a visual or in-client validation (see §9).

| # | Question | Verdict | Evidence |
|---|---|---|---|
| 1 | Vanilla 26.3 Trial Chambers definitions inspected from the real jar | **PASS** (automated) | §2 |
| 2 | `terrain_adaptation: "encapsulate"` is supported in 26.3 | **PASS** | vanilla `trial_chambers.json` uses it; probe structures load and generate with it |
| 3 | Independent minimal structure (central chamber + 2 corridors + combat room) generates | **PASS** (automated) | 109/109 non-overlapping samples in each of 4 shell×adaptation configs (+ end cap) |
| 4 | Combat room gets a vanilla trial spawner + vault | **PASS** (automated) | exactly 1 `trial_spawner` + 1 `vault` block in 59/60 rooms; the 60th overlapped a vanilla Trial Chamber (2+2) |
| 5 | `encapsulate` vs `none` compared on identical layouts | **PASS** (automated) | layouts identical in 193/193 dense pairs |
| 6 | Pieces stay connected (no disconnected rooms) | **PASS** (automated) | 0 disconnected in all non-overlapping samples |
| 7 | **`encapsulate` guarantees a sealed cavern** | **FAIL** | rock-carved ("bare") cavern sealed in only **29/109** (encapsulate) vs 8/109 (none) |
| 8 | Built-shell structure is sealed against *terrain* | **PASS** (terrain) / **FAIL** (strict) | 0 terrain-caused breaches/intrusions; 3 breaches came from vanilla **monster rooms** (§5.3) |
| 9 | No fluid inside rooms | **PASS** built / **FAIL** bare | built: 0; bare: 23/109 (encapsulate), 32/109 (none) |
| 10 | Encapsulate has a measurable benefit | **PASS** | built-shell cave contact ≈ ÷3 (median 37 vs 112, mean 97 vs 269); bare-cavern breaches ≈ ÷3.7 (mean 104 vs 389) (§4) |
| 11 | Encapsulate generation-time cost | **PASS** (none measurable) | ratio 0.98–1.04 over matched cells (§7) |
| 12 | Mountainous terrain covered | **PASS** | 36 targeted start chunks (surface Y 92–164) × 2 height profiles, + 6 incidental pairs in scenario A |
| 13 | Nearby / overlapping structures covered | **PASS** | trial chambers, mineshafts overlapped in real samples; §6 |
| 14 | Deep placement (Y −56…−44) works with default `dimension_padding` | **FAIL** | 18% of starts rejected; **PASS** with padding 0 or start ≥ −53 (§5.4) |
| 15 | Vanilla placement infrastructure: shared salt/spacing | **FAIL** | 29/30 starts collide with a vanilla Trial Chamber |
| 16 | Vanilla placement infrastructure: add to the vanilla `trial_chambers` set | **FAIL** | no overlap, but our structure displaces ~58% of vanilla Trial Chambers |
| 17 | Vanilla placement infrastructure: `exclusion_zone` against `trial_chambers` | **PASS** | 0/36 overlaps (vs 4/60 without) |
| 18 | Later structure step avoids vanilla feature intrusions | **PASS** (built, partial) | built sealed 120/120 vs 118/120; geode/fossil remnants persist (§5.3) |
| 19 | Static pack validation (jigsaw names/targets/pools, door alignment, closed shell, byte-reproducible) | **PASS** | `tools/worldgen-probe/selftest.py`, 6 tests |
| 20 | Repo CI command `clean test build compileGametestJava runGameTest` | **PASS** | local: JUnit, validation tasks, "All 113 required tests passed" (first attempt hit an upstream Maven HTTP 429 in `:test`; unchanged code re-ran green) |
| 21 | Visual / screenshot / in-client validation | **NOT RUN** | headless container; ASCII slices only (§9) |
| 22 | Fluid behaviour after the world ticks (flow into openings) | **NOT RUN** | measurements are static, immediately after generation |
| 23 | Large structure at Trial-Chambers scale (hundreds of pieces) — performance | **NOT RUN** | probe has 8 pieces; §7 extrapolates only |
| 24 | Playthrough of spawner/vault (spawning, loot, keys) | **NOT RUN** | blocks verified present; behaviour is vanilla and untested here |

## 2. Vanilla 26.3 Trial Chambers (read from the real jar)

From `data/minecraft/worldgen/structure/trial_chambers.json` and `structure_set/trial_chambers.json`:

* `type: minecraft:jigsaw`, `step: underground_structures`, **`terrain_adaptation: encapsulate`**, `size: 20` (jigsaw depth),
  `max_distance_from_center: 116`, `start_height: uniform −40…−20`, `dimension_padding: 10`,
  `liquid_settings: ignore_waterlogging`, `use_expansion_hack: false`, start pool `trial_chambers/chamber/end`.
* Spawner/ vault content is composed by **pool aliases** (`random_group` / `random`) over `trial_chambers/spawner/contents/*`.
* Placement: `random_spread`, `spacing 34`, `separation 12`, `salt 94251327`.
* Biomes: `#minecraft:has_structure/trial_chambers` — a long list that includes oceans and mushroom fields; not a
  "good underground" tag.
* 191 templates; the largest is 34×19×29.
* **Hard ceiling:** `JigsawStructure.verifyRange` rejects any non-`none` adaptation whose `max_distance_from_center.horizontal`
  exceeds **116** (116 + 12 > 128 is invalid). Vanilla Trial Chambers sit exactly on that ceiling: **a single Jigsaw
  structure with encapsulate cannot be larger than vanilla Trial Chambers' ±116 blocks** (≈ 232 × 232).

### What `encapsulate` actually does in 26.3 (decompiled `Beardifier`, `NoiseRouterData`, `StructureStart`)

* It is a **density-function term**, not a world-protection rule. For every *rigid* pool piece, `Beardifier` adds
  `0.8 × buryContribution(dx/2, dy/2, dz/2)` to `final_density`, where `dx/dy/dz` are distances to the piece's bounding
  box: strongest (+0.8, i.e. "more solid") inside the box and fading linearly to 0 about 12 blocks away.
* The Overworld router computes `finalDensity = min(slide(caves…), noodle) + beardifier`. The bump is added **after** the
  cave terms, so it raises density by ≤ 0.8 and cannot override strongly negative cave density.
* **Biome carvers** (`cave`, `canyon`) run after noise as a separate pass (`NoiseBasedChunkGenerator.generateCarvers`) and
  never consult structures.
* Structure pieces are placed afterwards, in the `underground_structures` decoration step, and overwrite every block their
  template contains (including air).
* `StructureStart.getBoundingBox()` is **inflated by 12** for any adaptation other than `none`. This inflated box decides
  which chunks hold references to the structure; it is *not* the physical footprint.
* Net effect: encapsulate thickens the rock *around* pieces (statistically fewer caves touching them) but guarantees nothing.

## 3. Probe design

All dimensions provisional. One structure, `moreminecrafts:probe_kingdom`, vanilla Jigsaw:

```
[end_cap] — corridor_b — [central_chamber 23×12×23] — corridor_a — [combat_room 17×10×17 : trial spawner + vault]
              5×6×24                                     5×6×24
```

* Doors are 3 wide × 4 high; all pieces are `rigid`; jigsaw `size` 8; `max_distance_from_center` 64.
* The combat room uses **vanilla** connector chains (`trial_chambers/spawner/melee` → `spawner_connector`, and
  `trial_chambers/reward/all` → `reward_connector`) plus the vanilla melee `pool_alias` — i.e. the closest vanilla
  architecture, referenced by pool id, with no vanilla file modified.
* Templates are generated by `tools/worldgen-probe/build_probe_pack.py` (pure Python, byte-reproducible) into a throw-away
  data pack in the world folder. They are **not** in `src/main/resources`.
* Two shell modes: **built** (1-block tuff-brick shell; interior air) and **bare** (no shell blocks at all — only
  interior air is placed, so the walls are whatever terrain generation left; this models the intended "cavern carved
  into rock connected by rock tunnels" case).
* Variants differ **only** in `terrain_adaptation` (verified byte-for-byte by the self-test).

Scenario matrix (130 world runs, 1,536 sample structures; `tools/worldgen-probe/run_matrix.sh`):

| Scenario | Placement | Start Y | Seeds | Purpose |
|---|---|---|---|---|
| A | `dense` (spacing 12 / sep 6) | `mid` −40…−20, `deep` −56…−44, `high` 20…40 | 1, 42, 1337, 20261017, −8675309 | main matrix: shell × adaptation × profile |
| B | `coincident` (vanilla spacing/salt) | mid | 1, 42, 1337 | forced overlap with vanilla Trial Chambers |
| C | `excluded` (`exclusion_zone` vs `minecraft:trial_chambers`, 12 chunks) | mid | 1, 42, 1337 | vanilla placement infrastructure |
| D | `deepnopad` (`deep` with `dimension_padding 0`) | −56…−44 | 1, 42 | isolate the padding failure |
| E | `shared` (probe added to the **vanilla** `trial_chambers` structure_set) | mid | 1, 42, 1337 | vanilla placement infrastructure |
| F | `densemtn` (start chunks with base terrain height ≥ 120) | mid, high | 1, 42, 1337 | mountainous terrain |
| G | `denselate` (structure `step: strongholds`) | mid, high | all five | vanilla feature intrusion |

Per scenario the harness takes the first 12 deterministic start chunks of the structure's placement around the world
origin, forces the surrounding 11×11 chunks to `FULL`, then reads the **actual generated blocks**.

### Metrics (all from the generated world)

| Metric | Definition |
|---|---|
| generated | a valid `StructureStart` with all 5 core pieces (chamber, corridor A/B, combat room, end cap) |
| disconnected | core piece whose interior is not reached by a 6-neighbour flood fill (collision-free cells) from the chamber |
| **sealed / leak cells** | the same flood fill, bounded to the footprint + 1 block: any reached cell outside the union of piece boxes is a leak. Sealed = 0 leak cells |
| shell holes | passable cells on a piece's box surface whose outside neighbour is not another core piece (i.e. not a doorway) |
| exposed faces | solid surface cells whose outside neighbour is open air / water / lava — cave contact, *not* a breach |
| foreign blocks | non-air, non-fluid interior blocks outside the spawner/vault sub-pieces, excluding vanilla biome decoration (vines, lichen, spore blossom, …) |
| interior fluid | water/lava cells inside rooms |
| terrain labels | derived from the unmodified `none` run: **caves** (≥ 5% of a 10-block halo is open air), **water** (≥ 100 halo samples), **lava** (≥ 20), **mountain** (mountain biome or surface ≥ 110), **overlap/nearby** other structures |

## 4. Results

Full tables: [`underground-kingdom-probe/results.md`](underground-kingdom-probe/results.md) (generated). Per-sample
coordinates and metrics: [`underground-kingdom-probe/samples.csv`](underground-kingdom-probe/samples.csv). Overlapping
samples are listed separately wherever they would confound a defect count.

**Built shell (what a built dungeon would be)** — terrain never breached it in any scenario:

| | encapsulate | none |
|---|---:|---:|
| generated (non-overlap, mid+high) | 109/109 | 109/109 |
| sealed from outside | 107/109 (2 monster rooms) | 108/109 (1 monster room) |
| terrain blocks / fluid inside rooms | 0 / 0 | 0 / 0 |
| median shell faces touching open cave air | **37** | **112** (≈ ÷3; mean 97 vs 269) |
| …in caves-labelled samples | 71 | 456 |
| …in water-labelled samples | 4 | 30 |

**Bare rock-carved cavern (the intended "cavern in rock" case)** — this is where the sealing question is decided:

| | encapsulate | none |
|---|---:|---:|
| sealed from outside (non-overlap, mid+high) | **29/109** | **8/109** |
| structures with water/lava *inside* the rooms (same samples) | 23/109 | 32/109 |
| median breach cells (all dense pairs, incl. overlaps) | 44 | 138 (mean 104 vs 389; encapsulate fewer/equal/more in 118/73/2 pairs) |
| sealed, caves-labelled (dense pairs) | 14/72 | 2/72 |
| sealed, water-labelled (dense pairs) | 15/64 | 3/64 |
| sealed, lava-labelled (dense pairs) | 16/61 | 6/61 |
| sealed, mountain-targeted (36 starts × mid+high = 72 sample-runs) | 18/72 | 4/72 |

**Conclusion: encapsulate helps (≈3× fewer breaches, ≈3× less cave contact) but does not guarantee a sealed cavern — the
large majority of carved-rock rooms still opened into natural caves, and aquifer water/lava still entered some.**
Under mountains (surface Y 92–164, depth below surface 90–150) the built shell was sealed 72/72 in each variant, and
the bare cavern behaved like other terrain (18/72 vs 4/72).

## 5. Observed cave-generation interactions

### 5.1 Order of operations (why a built shell is robust and a bare cavern is not)

Noise terrain (+ encapsulate density bump) → biome carvers → … → `underground_structures`: **jigsaw pieces overwrite
the terrain they occupy** → features of the same and later steps. Because pieces are placed after caves exist, a template
that *contains* its walls cannot be breached by terrain — only by something placed later. A template that relies on
surrounding rock is only as sealed as the terrain happened to be.

### 5.2 What encapsulate changes (and does not)

Pieces' neighbours are denser (cave contact falls ~3×), but carvers and strongly negative cave densities ignore it, so
natural caves still touch or enter the footprint (see `slices_bare_*` evidence below).

### 5.3 Vanilla features that share the `underground_structures` step

Every built-shell anomaly that was *not* an overlap with another vanilla structure was a vanilla **feature**; **none** was
terrain. They occur at 7 distinct locations (of 109 non-overlapping structures per variant; one location has both a dungeon and a geode); a given location may hit one
variant and not the other because terrain differs slightly:

| Intruder | Evidence (marker blocks near the footprint) | Effect | Distinct locations |
|---|---|---|---:|
| Monster room (dungeon) | `spawner` + `mossy_cobblestone` | opened the shell (24–55 breach cells) | 3 (encapsulate 2, none 1) |
| Amethyst geode | `smooth_basalt` / `calcite` / `budding_amethyst` | 5–25 foreign blocks inside a room | 4 |
| Fossil | `bone_block` | 8 foreign blocks | 1 |
| Vegetation (vines, glow lichen, spore blossom) | lush-cave decoration | grows on interior surfaces; 75–80/109 structures; harmless, excluded from "foreign" | — |

Mitigation tested (scenario G): moving the structure to `step: strongholds` (placed after those features). Built shells:
**sealed 120/120 vs 118/120**; foreign-block structures 3 → 2 (geode remnants of 12 blocks and 2 fossil blocks persisted —
neighbouring-chunk decoration can still write across chunk borders, so this is mitigation, not a guarantee). It does not
help bare caverns (sealed 29/120 with the late step vs 31/120 with the default).

### 5.4 Other observed interactions

* **Dimension padding.** `JigsawPlacement` rejects a start whose centre piece lies below `minY + padding`
  (−64 + 10 = −54). The centre piece's box starts at `start_y − 1`, so start Y ≤ −54 fails: the `deep` profile lost
  22/120 starts per adaptation (accepted boxes had minY −54 … −45); with padding 0 all 48/48 generated.
* **Overlap with vanilla structures.** At probe spacing (mid profile, per variant), 4/60 starts overlapped a vanilla Trial Chamber and 3/60 a
  mineshaft. An overlapped room is filled with the other structure's blocks (460–2,930 foreign blocks and a disconnected
  piece in the worst cases). Identical salt/spacing makes it systematic: 29/30.
* **Start-box inflation.** `StructureStart.getBoundingBox()` was 12 blocks larger than the pieces on every side with
  encapsulate; any overlap logic must use piece boxes.
* **Interior fluid** only occurred where rock was absent (bare), never behind a built shell.

### 5.5 ASCII evidence (headless, y = −39 slice, seed 1, start chunk 3,4)

[`slices_bare_none_mid_seed1.txt`](underground-kingdom-probe/slices_bare_none_mid_seed1.txt) shows a natural cave (`,`)
running straight into corridor A (`+++++` = open cells on the box surface) with `none`;
[`slices_bare_encapsulate_mid_seed1.txt`](underground-kingdom-probe/slices_bare_encapsulate_mid_seed1.txt) is the same
sample with encapsulate (66 leak cells instead of 594). `slices_built_*` show the same sample with a built shell
(sealed in both).

## 6. Vanilla Trial Chambers placement infrastructure — any advantage?

| Option | Result | Verdict |
|---|---|---|
| Own structure_set, own salt/spacing (`dense`) | 7/60 overlap another structure (4 vanilla Trial Chamber) | baseline |
| Reuse vanilla spacing + salt (`coincident`) | 29/30 collide, 29/30 pieces disconnected | **reject** |
| Add our structure to the vanilla `minecraft:trial_chambers` set (`shared`) | 0 overlaps, but ours was chosen for 21/36 start chunks, i.e. **vanilla Trial Chambers are displaced** | **reject** (violates "do not replace vanilla") |
| Own set + `exclusion_zone` vs `minecraft:trial_chambers` (12 chunks) | **0/36 overlaps**, vanilla untouched | **adopt** |
| Reuse vanilla pools by id (spawner/vault chains) | works; 59/60 rooms had 1 spawner + 1 vault | **adopt** (reference, don't copy or modify) |
| Reuse biome tag `#minecraft:has_structure/trial_chambers` | includes oceans/mushroom fields | use an own tag |

The only thing vanilla placement offers over an independent structure is non-overlap with vanilla Trial Chambers, and
`exclusion_zone` gives that without touching vanilla. Mineshaft/other overlaps are not prevented by either.

## 7. Performance

* Measured (matched cells, 12 start regions × 121 chunks ≈ 1,450 chunks each): **encapsulate / none generation time =
  0.98 – 1.04** across 8 shell×profile groups — indistinguishable from noise.
* Not measured (**NOT RUN**): the probe has 5 rigid pieces. `Beardifier` evaluates every rigid piece within ~12 blocks of
  a chunk for each density sample, so cost grows with piece count and local density; a Trial-Chambers-scale structure
  (hundreds of pieces) should be profiled before committing to a piece budget.
* Harness cost is irrelevant to the game (it only exists in the gametest source set).
* Run-to-run reproducibility (96 samples re-run): layouts 96/96 identical; structural metrics 95/96 identical (one
  geode-adjacent foreign-block count 0 → 5); cave-contact counts differed by a few faces in 8/96 (≤ ~2%). Treat
  exposure counts as approximate; vanilla multi-threaded chunk generation is not perfectly order-independent.

## 8. Recommendation

**Independent vanilla Jigsaw structure with vanilla Trial-Chambers-style plumbing (hybrid by reference), and a
self-contained built shell — do not rely on `encapsulate` to seal anything.**

1. **Structure:** its own `jigsaw` structure + structure_set (own salt) with `exclusion_zone` against
   `minecraft:trial_chambers`; do not touch, share, or coincide with the vanilla set; define an own biome tag.
2. **Terrain adaptation:** keep `encapsulate` (free, and it cut cave contact ≈ 3×), but treat it as cosmetic padding.
3. **Seal by construction:** every district, the central cavern and every rock tunnel must carry its own wall/lining
   blocks in the templates (a "rock" lining made of stone/deepslate/tuff is fine). The intended "cavern in rock" look can
   be preserved by lining with natural-looking rock instead of bricks. A bare cavern failed in 73–93% of samples.
4. **Step:** place it late (e.g. `strongholds`) so vanilla dungeons/geodes/fossils cannot cut the lining; accept rare
   residue, or re-validate after any change.
5. **Reuse vanilla mechanics by reference:** trial spawner and vault pools/aliases worked unchanged; reward
   content = 특수아이템(미정) later (not implemented).
6. **Height:** keep start boxes at Y ≥ −53 (or set `dimension_padding` lower) — see §5.4.
7. **Scale:** a single Jigsaw structure cannot exceed ±116 blocks with any terrain adaptation, i.e. exactly the vanilla Trial
   Chambers ceiling. "Comparable to Trial Chambers" is therefore feasible, but a *larger* kingdom would need several
   structures or a non-Jigsaw approach. Jigsaw `size` is capped at 20.
8. **Not recommended:** a purely carved ("bare") design that depends on surrounding rock; sharing/modifying the vanilla
   `trial_chambers` set; custom code that edits world generation (not needed for any finding here).

## 9. Remaining risks / not validated

* **Visual and in-client validation: NOT RUN.** There is no display here; lighting, aesthetics, readability and walkability
  were not looked at. ASCII slices are an automated plan-view substitute.
* **Fluid settling: NOT RUN.** Counts are taken at generation time; water can still flow into any opening once the world ticks.
* **Large-scale performance: NOT RUN** (see §7).
* **Cross-chunk feature residue** (geode/fossil/dungeon) after moving to a later step is rare but not zero.
* Terrain labels are heuristics (thresholds in `tools/worldgen-probe/aggregate.py`); "mountain" has only 6 incidental
  pairs in scenario A, backed by the 72 targeted starts of scenario F.
* Seeds are deterministic but finite (5 + 3); a different seed set can move percentages, not the qualitative picture.
* Probe vault loot is the vanilla trial-chamber reward table; the production reward (특수아이템(미정)) is undecided.
* No design decision was invented. Nothing else changed in gameplay, items, mobs or loot.

## 10. Reproduce

```bash
# JDK 25 required (Minecraft 26.3). From the repo root:
python3 -I tools/worldgen-probe/selftest.py                # static pack validation (seconds)
tools/worldgen-probe/run_probe.sh encapsulate mid dense 1 12 bare   # one cell: ~40 s
#   args: <encapsulate|none> <mid|deep|high|deepnopad> <dense|coincident|excluded|shared|densemtn|denselate> <seed> [samples] [built|bare]
#   env:  PROBE_SLICES=2 (write ASCII slices), PROBE_MIN_SURFACE=120 PROBE_SEARCH_RADIUS=400 (mountain targeting)
tools/worldgen-probe/run_matrix.sh 12                       # full matrix, roughly 2 h on 4 cores
python3 -I tools/worldgen-probe/aggregate.py build/worldgen-probe/results > results.md
python3 -I tools/worldgen-probe/export_csv.py build/worldgen-probe/results > samples.csv
```

How it runs: `./gradlew runWorldgenProbe` (a Loom dedicated-server run config in `build.gradle`, not part of
`check`/`build`/CI) starts a real 26.3 server whose world folder holds the generated data pack; the
`WorldgenProbe` entrypoint (gametest source set, inert unless `-Dmm.probe.out` is set) measures the generated chunks and
writes JSON. Every sample row in `samples.csv` carries absolute block coordinates, so a world can be recreated with the
same seed/placement/profile and visited with `/tp`.

Files: `tools/worldgen-probe/*`, `src/gametest/java/.../worldgenprobe/WorldgenProbe.java`,
`src/gametest/resources/fabric.mod.json` (+1 entrypoint), `build.gradle` (+1 run config),
`docs/underground-kingdom-probe/*`.
