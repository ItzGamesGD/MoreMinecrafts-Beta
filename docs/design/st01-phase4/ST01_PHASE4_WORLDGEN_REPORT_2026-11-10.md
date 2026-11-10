# ST-01 Pyramid — Phase 4: production worldgen integration (royal-hall fixture) — implementation report

Status: **PARTIAL** (see §9). Branch `claude/st01-pyramid-phase4-worldgen-roke2l`. Not merged; Draft PR only.

> **Scope decision recorded by the owner for this task.** The repository holds no complete ST-01 pyramid asset. The only structure NBT on any branch is
> `pyramid/royal_hall.nbt` (39×20×39, P8-A + D1). The 81×81×40 shell, entrances, L1–L3 chambers, connections and vault exist only as PROPOSED design
> coordinates. Owner instruction: implement the approved placement/eligibility/registration, use the royal hall **only as a temporary, explicitly
> identified integration fixture**, invent nothing, and report completed-pyramid generation, full-structure terrain adaptation, chamber continuity and
> visual acceptance as UNVERIFIED.

## 1. Recovery findings (read-only phase)

| Item | Finding |
|---|---|
| `origin/main` | `cde842dd` (2026-10-23). None of the ST-01 work is on `main`; PRs #249/#251/#252 are Drafts *targeting* `main` but their branches are 57–63 commits ahead of a merge base that is absent from this (shallow) clone. |
| Checked-out `HEAD` at start | `5c22f9fb` — a **Sky Tower** line, unrelated to ST-01. The designated branch was reset onto the P8-B2 baseline (the old tip is contained in `feature/sky-tower-integration-02`, nothing lost). |
| PR #249 / #251 / #252 (O-14 Phase 1-2 / 3 / 3.1) | **Diagnostic only**: `St01DesertWorldgenProbe`, `St01Phase3CalibrationProbe`, python analysers, probe workflow. No structure registration, no structure set, no structure piece. Their own owner-decision document states "production worldgen source was not modified". |
| Two divergent ST-01 lineages | (a) O-14 probes sit on P11-R (`6d7856b4`), which does **not** contain P8-A/B2; (b) the royal-hall NBT + royal-chest runtime (P8-A, D1, P8-B2, PR #254 `03685656`) does **not** contain O-14. Neither lineage contains the other. |
| Integration baseline chosen | `03685656` (P8-B2 head: latest `royal_hall.nbt` incl. D1, `St01RoyalChestPlacement`, `RoyalChest*`). From O-14 only the *definitions* (81×81 footprint, WORLD_SURFACE_WG, median foundation, cut/skirt, fluid volume) were taken; no diagnostic code was merged. |
| Pre-existing facts the brief's §3 relies on | P8 plan §7 lists spacing 32/separation 8 as UNAPPROVED and a different salt (`0x53543031`); the brief's locked values (salt **91401**) supersede it. The approved values are not yet written into the routed current-authority documents (see §10). |
| Geometry consistency check (owner's pre-implementation requirement) | **Consistent.** P1 frame: world = (cx+X, groundY+Y, cz+Z), anchor X=Z=0, Y=0 = lowest above-ground layer. Hall reservation X[-40,-2] Y[-24,-5] Z[2,40] lies wholly inside the ±40 footprint for all four rotations (unit-tested), so one custom `Structure` anchored at the footprint centre represents both the 81×81 eligibility rule and the buried hall. No blocker for the placement mechanism; the blocker is only the missing pyramid asset. |

## 2. What was implemented (`src/main/java/.../pyramid/worldgen`)

| Class | Role | Replaced when the pyramid exists? |
|---|---|---|
| `St01PlacementConfig` | Locked values: spacing 32, separation 8, triangular, salt 91401, footprint 81, Δh ≤ 16, cut ≤ 10, skirt ≤ 8 | no |
| `St01Eligibility` | Pure, deterministic qualification: (1) full-footprint Desert, (2) MODERATE terrain, (3) underground fluid | no |
| `St01PyramidStructure` | Custom `Structure`: runs `St01Eligibility` against the real generator (no chunk loading) and emits pieces only if eligible | no |
| `St01FootprintCorrectionPiece` | MODERATE correction over exactly the 81×81 footprint | no |
| `St01RoyalHallFixture` (+ `Piece`, `FluidSealPiece`) | **Temporary fixture adapter**: places `royal_hall.nbt` at its approved pyramid-relative position, rotation about the anchor, fluid seal | **yes — only `UNDERGROUND_VOLUME` and `addPieces` change** |
| `St01Worldgen` | registers structure type + 3 piece types (called from `MoreMinecrafts.onInitialize`) | no |
| `data/moreminecrafts/worldgen/structure/st01_pyramid.json`, `structure_set/st01_pyramid.json` | `random_spread`, spacing 32, separation 8, `triangular`, salt 91401, biome `minecraft:desert`, step `surface_structures`, `terrain_adaptation: none` | no |

### Definitions (established O-14 definitions, one deliberate strengthening)
* height difference = max − min of `WORLD_SURFACE_WG` base heights; foundation level `yf` = median; cut = max(0, max − yf); skirt = max(0, yf − min).
* **Strengthening:** O-14 sampled a 4-block grid (441 columns). A sampled grid cannot guarantee the limits for the skipped columns, so production samples **every** footprint column (6561). Cost is bounded (only anchors that already passed the Desert stage get here). This can only make acceptance stricter than Phase 3.1's predictions.
* **Full-footprint Desert.** `BiomeManager` resolves a block's biome to one of the 2×2×2 quart cells `floor((x−2)/4)+{0,1}` (and same for y, z). Requiring *every* such cell around *every* footprint column (22–23 cells per axis, at the cell's surface Y, both vertical candidates) to be Desert guarantees every footprint block resolves to Desert regardless of the jitter; the covering property is unit-tested for 601 anchors incl. negative coordinates. The anchor cell used by vanilla's stub check (`isValidBiome`) is additionally required so a stub accepted here cannot be silently dropped later.
* **Fluid safety.** Every generator base column over the fixture's underground volume **+1 block margin** must be fluid-free (reject otherwise). Because carvers/aquifers can still leave fluid that the noise column cannot show, a `FluidSealPiece` replaces fluid in the 1-block shell around the hall with sandstone (deterministic; never touches the box).
* **Correction.** Above `yf`: cut (to `base+8` to remove already-placed plants). Skirt band `[yf−8, yf−1]`: every non-solid block (air, fluid, replaceable) becomes sand (top 4) / sandstone, so a carved cave or lower column cannot leave a pit. Inputs are pure functions of the seed (generator base height), not of chunk order. Runs only for accepted starts ⇒ a rejected candidate leaves nothing.
* **Rotation.** Four rotations, no mirror, derived from the structure's seeded `WorldgenRandom` (vanilla convention). *This is an implementation choice, not an approved design value — flagged in §10.*

## 3. Tests and CI
### Commands
* Unit/regression: `./gradlew build compileGametestJava -x runGameTest` (19 new ST-01 unit tests: 15 `St01EligibilityTest`, 4 `St01WorldgenResourceTest`, plus the whole existing suite) and `-Dmm.royal.placement=true ... *royal_chest_game_tests*` (P8-B2 regression, 14/14).
* Real server, per seed: `tools/st01-phase4/run_validation.sh <seed> 10240` = **scan** (fresh world) → **replay** (fresh world, *reverse* chunk-generation order) → **reload** (saved scan world restarted); fails on any invariant violation. Workflow: `.github/workflows/st01-phase4-worldgen.yml`.
* Aggregate: `tools/st01-phase4/aggregate.py`.

### CI evidence (green runs: commit `8371c196` run 42763773866 and commit `fa1484c1` run 42765649652 — identical worldgen code, the latter only changes the client-capture test)
regression ✔ · 15/15 `worldgen (<seed>)` ✔ · client-capture job ✔ · aggregate ✔. Earlier runs on this branch failed and were fixed (see §7). The `ST-01 Royal Chest Runtime`-style Guardian GameTest `tomb_guardian_game_tests_risen_statue_is_solid_at_the_approved_size_and_telegraph_stays_walkable` fails **on the untouched baseline `03685656` too** (its own workflow, step "Guardian regression GameTests") and passes locally (24/24); it is an out-of-scope pre-existing finding and was removed from the Phase 4 workflow rather than hidden.

## 4. Measured results (15 seeds, ±10,240 blocks = 419.43 km² each, 1,600 potential anchors per seed)

| seed | potential | center-only desert | full-footprint desert | terrain-qualified | fluid-safe | valid starts | completed | density /100 km² | nearest (blocks) | rejections |
|---|---|---|---|---|---|---|---|---|---|---|
| 1001 | 1600 | 58 | 17 | 4 | 1 | 1 | 1 | 0.238 | 8257 | {'REJECT_BIOME': 1583, 'REJECT_FLUID': 3, 'REJECT_HEIGHT_DIFFERENCE': 12, 'REJECT_SKIRT': 1} |
| 1002 | 1600 | 48 | 18 | 5 | 2 | 2 | 2 | 0.477 | 8992 | {'REJECT_BIOME': 1582, 'REJECT_CUT': 2, 'REJECT_FLUID': 3, 'REJECT_HEIGHT_DIFFERENCE': 10, 'REJECT_SKIRT': 1} |
| 1003 | 1600 | 30 | 4 | 0 | 0 | 0 | 0 | 0.000 | None | {'REJECT_BIOME': 1596, 'REJECT_HEIGHT_DIFFERENCE': 4} |
| 1004 | 1600 | 33 | 8 | 3 | 2 | 2 | 2 | 0.477 | 3224 | {'REJECT_BIOME': 1592, 'REJECT_FLUID': 1, 'REJECT_HEIGHT_DIFFERENCE': 5} |
| 1005 | 1600 | 48 | 12 | 1 | 1 | 1 | 1 | 0.238 | 4746 | {'REJECT_BIOME': 1588, 'REJECT_HEIGHT_DIFFERENCE': 11} |
| 1034165336 | 1600 | 24 | 5 | 1 | 1 | 1 | 1 | 0.238 | 4451 | {'REJECT_BIOME': 1595, 'REJECT_HEIGHT_DIFFERENCE': 4} |
| 1421442253 | 1600 | 30 | 6 | 0 | 0 | 0 | 0 | 0.000 | None | {'REJECT_BIOME': 1594, 'REJECT_HEIGHT_DIFFERENCE': 6} |
| 1541117449 | 1600 | 77 | 31 | 4 | 3 | 3 | 3 | 0.715 | 5776 | {'REJECT_BIOME': 1569, 'REJECT_CUT': 3, 'REJECT_FLUID': 1, 'REJECT_HEIGHT_DIFFERENCE': 23, 'REJECT_SKIRT': 1} |
| 1928394366 | 1600 | 29 | 7 | 2 | 2 | 2 | 2 | 0.477 | 5547 | {'REJECT_BIOME': 1593, 'REJECT_HEIGHT_DIFFERENCE': 5} |
| 2048069562 | 1600 | 13 | 1 | 1 | 1 | 1 | 1 | 0.238 | 7908 | {'REJECT_BIOME': 1599} |
| 287862831 | 1600 | 39 | 11 | 4 | 1 | 1 | 1 | 0.238 | 9426 | {'REJECT_BIOME': 1589, 'REJECT_FLUID': 3, 'REJECT_HEIGHT_DIFFERENCE': 6, 'REJECT_SKIRT': 1} |
| 407538027 | 1600 | 28 | 9 | 4 | 1 | 1 | 1 | 0.238 | 9873 | {'REJECT_BIOME': 1591, 'REJECT_FLUID': 3, 'REJECT_HEIGHT_DIFFERENCE': 5} |
| 527213223 | 1600 | 45 | 12 | 2 | 1 | 1 | 1 | 0.238 | 12380 | {'REJECT_BIOME': 1588, 'REJECT_FLUID': 1, 'REJECT_HEIGHT_DIFFERENCE': 8, 'REJECT_SKIRT': 2} |
| 794814944 | 1600 | 53 | 8 | 1 | 0 | 0 | 0 | 0.000 | None | {'REJECT_BIOME': 1592, 'REJECT_FLUID': 1, 'REJECT_HEIGHT_DIFFERENCE': 6, 'REJECT_SKIRT': 1} |
| 914490140 | 1600 | 58 | 19 | 2 | 0 | 0 | 0 | 0.000 | None | {'REJECT_BIOME': 1581, 'REJECT_FLUID': 2, 'REJECT_HEIGHT_DIFFERENCE': 16, 'REJECT_SKIRT': 1} |

Pooled: 16 starts over 6291.5 km² = 0.254 /100 km² (95% CI 0.145-0.413); seeds with zero starts: 4/15; ratio to Phase 3.1 Woodland Mansion density 0.36x; independent-prediction mismatches: 0

Phase 3.1 prediction vs production (reference seeds):

| seed | P3.1 full-100%-desert | production full-footprint desert | P3.1 moderate-eligible | production valid starts |
|---|---|---|---|---|
| 1001 | 16 | 17 | 4 | 1 |
| 1002 | 18 | 18 | 6 | 2 |
| 1003 | 4 | 4 | 0 | 0 |
| 1004 | 8 | 8 | 3 | 2 |
| 1005 | 10 | 12 | 2 | 1 |


Seeds 1001–1005 are the Phase 3.1 reference seeds; the other ten are an independent deterministic batch `seed_i = (i·0x9E3779B1 + 20261110) mod 2³¹`, i = 1..10.

**Every** one of the 24,000 potential anchors was evaluated twice — by the production `Structure` inside the real generator, and by an independent re-implementation in the probe (own terrain view over `ServerLevel`, own rotation derivation) — and then compared with the actual `StructureStart` obtained from the real world: **0 mismatches**. Stages are reported separately: potential → full-footprint Desert → terrain-qualified → fluid-safe (= predicted valid) → actual valid start → completed (FULL generation + physical checks).

### Phase 3.1 comparison (investigated, not tuned)
* **Desert stage agrees** with Phase 3.1 (16/18/4/8/10 predicted vs 17/18/4/8/12 measured on seeds 1001–1005) even though the coverage rule here is stricter in principle (every BiomeManager candidate cell).
* **Terrain+fluid stages disagree strongly**: Phase 3.1 predicted 15 moderate-eligible anchors on the five seeds, production yields **6**. Cause, measured: the O-14 fluid test inspected five diagnostic columns; production tests the fixture's whole underground volume + 1 block margin. Of 34 terrain-qualified anchors across the 15 seeds, **18 (53 %) are rejected for fluid**; a fluid audit generated real chunks for rejected candidates and every audited one (see per-seed JSON `fluid_rejection_audit`) contains real water/lava in that volume, i.e. these are true positives. A second, smaller effect: sampling all 6561 columns instead of 441.
* **Consequence for the owner:** with the locked parameters and the fixture volume, the pooled density is **0.254 / 100 km² (95 % CI 0.145–0.413), 0.36× the Phase 3.1 Woodland-Mansion density**, and 4/15 seeds have no start within 10,240 blocks. The "1.0× parity" expectation of Phase 3.1 candidate A is **not** reproduced. Nearest start to the origin per seed: 3,224–12,380 blocks. The final pyramid's real underground volume (L1–L3, vault, deeper than the hall) will be larger than the fixture's and will push this lower. No parameter was changed.

## 5. Acceptance matrix (details and evidence)

| Requirement | Result | Evidence / limits |
|---|---|---|
| Approved parameters preserved exactly | PASS | `St01PlacementConfig`; `St01WorldgenResourceTest`; probe re-derives the potential chunk of 400 regions from salt 91401/32/8/triangular = 0 mismatches; frequency 1.0, no exclusion zone; structure appears in exactly one structure set |
| Registration | PASS | structure type, structure, 3 piece types, structure set registered on a real server (all 15 seeds) |
| Real generation of the available NBT | PASS | 16 starts in 15 seeds, all FULL-generated; 16/16 pass physical checks (see below). **Not** a complete pyramid. |
| Full 81×81 Desert footprint | PASS | all 6,561 real `level.getBiome` columns of every start are Desert (0 non-desert); negative coordinates occur (e.g. seed 1004 start at −9128, −8008) |
| MODERATE limits at placement | PASS (rule) / UNVERIFIED (full-structure adaptation) | exhaustive height difference/cut/skirt of every accepted start are within 16/10/8 (e.g. seed 1541117449: Δh 16, cut 9, skirt 7); unit tests at 16/17, 10/11, 8/9; correction fixture builds columns at exactly +10/−8 |
| Rejection leaves nothing | PASS | real chunks of 3 terrain-rejected + up to 6 fluid-rejected candidates per seed: no start, no hall blocks, no flattening |
| Underground fluid safety | PASS for the fixture volume; limits below | 0 fluid in the hall box and 0 in the 1-block shell after sealing for every start. Fluid beyond 1 block, and carver-created fluid, is not predicted by the base-column test (the seal handles the 1-block shell only). |
| Orientation | PASS | chest pair, guardian spawn and hall box match the independently rotated P1 reference frame for all four rotations occurring; agrees with `St01RoyalChestPlacement.worldPos` |
| Neighbouring terrain | PARTIAL | outside the footprint nothing is written; measured edge step to the foundation is ≤ 11 blocks for all but one start (seed 1005: 74, natural neighbouring terrain). Whether the plateau edge looks natural is **visual — UNVERIFIED**. |
| Deterministic replay | PARTIAL | see §6 |
| Completed pyramid, entrances, chamber continuity, secret vault | **UNVERIFIED — asset does not exist** | owner instruction |
| Visual acceptance | **UNVERIFIED** | see §8 |

## 6. Determinism / replay
* **Reload (restart of the saved world): exact.** Every block of every footprint volume and hall hash is byte-identical after a server restart (16/16 starts).
* **Replay (fresh world, same seed, reverse generation order): structure-owned content identical, whole volume not.** Identical for every start: set of starts, anchor, foundation Y, rotation, hall box, chest coordinates, template-masked hall hash, corrected ground-surface profile hash (the latter excused only when another vanilla structure overlaps). Exact block-by-block equality of the whole 81×81×29 window is **not** achieved: 38–326 of 190,269 blocks (≤ 0.17 %) differ per start, all of them vegetation (dry grass, cactus, dead bush, sugar cane, azalea trees) or vanilla underground features (andesite/diorite/granite blobs, ores, fossil bone blocks, glow lichen). A no-structure control window of the same size differs the same way in some starts (e.g. seeds 1004, 1002, 1541117449), so this is vanilla feature order across chunk borders, not ST-01. The same mechanism lets vanilla cave decoration (glow lichen, fossil bone) replace a few hall blocks; the hall voxel check allows only a fixed allow-list of such overlay blocks. **This is "expected nondeterminism", recorded, not claimed as exact replay.** Starts overlapping another vanilla structure (seen: mineshaft ×2, desert village ×1) additionally change the ground there depending on step order.

## 7. Defects found and fixed during validation (all by real-world checks, none by tuning limits)
1. Probe census parse bug (all blocks reported as "?") → registry-aware parse.
2. Correction used heightmaps that include plants placed by neighbouring chunks → generator base height (pure function of the seed) + band rule that replaces plants/trees/fluid/air, keeps only full-block terrain.
3. Caves under cut columns left pits in the new surface → skirt band `[yf-8, yf-1]` sealed.
4. 31 fluid blocks touching the hall (carver/aquifer water invisible to the noise-column check) → `FluidSealPiece` (1-block shell).
5. Footprint hash and hall census too strict for vanilla overlay → template-masked hash, voxel comparison with allow-list, canonical surface-profile hash.
6. Probe port collision on CI runner → free port per phase.
7. Client capture first rendered only sky (chunks not meshed) → pre-generation, client chunk wait, larger render distance (see §8).

## 8. Visual acceptance
**UNVERIFIED.** A rendered-client harness exists (`St01Phase4ClientCaptureTest`, `tools/st01-phase4/run_client_capture.sh`, CI job `client-capture`: real Minecraft 26.3 client under Xvfb with software GL, dedicated server with seed 1004, 10 camera views per start for the two starts of that seed, pre-generated chunks, waits for `LevelRenderer.hasRenderedAllSections()`). It runs green end-to-end and produces 20 screenshots per run, **but every screenshot I inspected shows only sky/fog (sun and horizon gradient), including the top-down view and the views teleported into the buried hall; no terrain, plateau or hall is visible, so the images prove nothing and are not offered as evidence.** The cause is unresolved (the client camera evidently does not reflect the server teleport, or the world pass is not captured under this software-GL setup; the same pattern appeared after pre-generation and after waiting for all sections). The numerical audit (edge step to foundation ≤ 11 blocks for 15 of 16 starts, 74 for one start whose neighbouring terrain is naturally steep; flat top, no leftover terrain above the foundation, sealed skirt band) is **not** a substitute for looking at the plateau edge, foundation exposure, hall entrance or neighbouring terrain. Next step: debug the harness (log `client_pose`, compare with the P8-A harness which does capture a flat world) or capture on a desktop client at the listed anchors (see per-seed JSON).

## 9. Status
PHASE4_STATUS: **PARTIAL** — see the status block in the PR description.

## 10. Known limitations, open decisions for the owner
1. **The complete pyramid asset is missing** (blocker for completed-pyramid generation, entrance accessibility, chamber continuity, vault, visual acceptance, full-structure terrain adaptation and the true underground fluid volume). The fixture adapter is isolated for replacement.
2. **Density**: 0.254 /100 km² (0.36× Mansion) with the locked values and the fixture volume; will drop with a larger final underground volume. Owner decision needed on whether this is acceptable, or on spacing/separation/terrain/volume (not changed here).
3. **Fluid-check definition** (volume = asset's underground volume + 1 block margin; base-column noise) is an implementation choice and the single largest rejection cause (53 % of terrain-qualified anchors).
4. **Rotation policy** (4 rotations, no mirror, seeded) is an implementation choice, not an approved value.
5. **Overlap with other vanilla structures** (mineshaft, village seen) is neither prevented nor specified; the correction/seal writes into them. An exclusion zone would be a design decision.
6. **Natural hall is not registered with the royal-chest runtime** (`St01RoyalChestPlacement.registerPlaced` is not called by worldgen): P8-B2 integration was not modified; instance-ID/anchor-key questions (P8 plan §5) remain open. The natural hall's chests are plain chests until that is decided.
7. **Authority sync:** the locked values (32/8/triangular/salt 91401/MODERATE 16-10-8) are not yet recorded in `docs/CONTENT_AUTHORITY_INDEX.md`'s routed authority; P8 plan §7 still lists different UNAPPROVED values. A design-sync task is needed.
8. Pre-existing out-of-scope findings: the Guardian GameTest failing on baseline CI; Phase 3.1 documents list inconsistent Woodland-Mansion per-seed counts ([3,4,2,4,2] vs [3,3,2,1,6]).


## 11. Files
Production: `pyramid/worldgen/{St01PlacementConfig,St01Eligibility,St01PyramidStructure,St01FootprintCorrectionPiece,St01RoyalHallFixture,St01Worldgen}.java`, `MoreMinecrafts.java` (one init line), `data/moreminecrafts/worldgen/structure/st01_pyramid.json`, `structure_set/st01_pyramid.json`. Tests: `St01EligibilityTest`, `St01WorldgenResourceTest`, probe `worldgenprobe/st01p4/St01Phase4Probe.java`, `St01Phase4ClientCaptureTest.java` (+ `fabric.mod.json` of the gametest set, `build.gradle` run config `st01Phase4Probe`). Tooling: `tools/st01-phase4/*`, `.github/workflows/st01-phase4-worldgen.yml`. Evidence: `docs/design/st01-phase4/evidence/aggregate.{json,md}`.

## 12. Owner acceptance checklist
- [ ] Accept that Phase 4 delivers placement/registration/eligibility/correction/fluid infrastructure verified on a **fixture** (royal hall only), not a pyramid.
- [ ] Decide on density: 0.254 /100 km² (0.36× Mansion), 4/15 seeds with no start in 10,240 blocks, before the real underground volume is added.
- [ ] Confirm or change: fluid-volume definition (+1 block margin, base-column noise) and the 1-block fluid seal.
- [ ] Confirm or change: rotation policy (4 rotations, no mirror).
- [ ] Decide on overlap with vanilla structures (mineshaft, desert village observed): allow or add an exclusion zone.
- [ ] Decide when/how natural hall placement registers with the royal-chest runtime (P8 plan §5 IDs).
- [ ] Record the locked placement values in current authority (design-sync task).
- [ ] Provide/approve the complete pyramid asset (shell, entrances, L1–L3, vault, annex count, underground depth limit), then re-run this suite with the real volume.
- [ ] Visual acceptance of plateau edges, foundation exposure, entrance and neighbouring terrain once a working capture or a desktop client is available.
