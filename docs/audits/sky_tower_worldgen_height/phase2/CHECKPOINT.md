# Sky Tower Mountain Worldgen — Phase 2 Checkpoint

## Repository state

| Item | Value |
|---|---|
| Branch | `claude/sky-tower-phase-2-validation-twragg` (Phase 2 validation branch) |
| Base (verified) | `validation/sky-tower-mountain-height` @ `038eaef6872e90961afdbf8ccab2581a275332a1`; working tree clean before work |
| Phase 1 deliverables | untouched (`docs/audits/sky_tower_worldgen_height/*.md|csv` identical to base) |
| Production files | **none changed** — `git diff 038eaef -- src/main scripts` is empty; no worldgen JSON, Java, NBT, loot or gameplay change |
| Merge status | not merged; feature branch and `main` untouched; no PR |
| Final commit SHA / remote verification | reported in the delivery message (a file cannot contain its own commit id) |

## What was added (documentation + isolated validation tooling only)

| Path | Purpose |
|---|---|
| `docs/audits/sky_tower_worldgen_height/phase2/` | the 8 required deliverables + `evidence/` (small raw outputs) |
| `src/gametest/java/.../worldgenprobe/SkyTowerPhase2Probe.java` | probe; inert unless `-Dmm.p2.out`; modes `world`, `controlled`, `forced` |
| `src/gametest/resources/fabric.mod.json` | +1 entrypoint line registering the probe in the **gametest** mod only |
| `tools/sky-tower-phase2/` | `run_probe.sh`, `scan_seeds.sh`, `run_regression_probe.sh`, `run_probe.init.gradle`, `analyze.py`, `height_model.py` |

## Deliverable inventory

| File | Content |
|---|---|
| `RUNTIME_REPRODUCTION.md` | status, method classes (NATURAL / REPLAY / CONTROLLED_REPLAY / FORCED), Phase 0 answers, sample sizes, observations, cross-checks, limits, reproduction commands |
| `PLACEMENT_TRACE.csv` | 12,925 per-attempt rows (committed subset, see below) |
| `CORRECTED_HEIGHT_MODEL.md` | corrected formula, element heights, thresholds, Phase 1 claim adjudication |
| `REMEDIATION_BENCHMARK.csv` | aggregates: natural paired (per terrain bin × config) and controlled sweep (biome × T × config), with Wilson 95 % intervals and model prediction |
| `RARITY_DISTRIBUTION.md` | the four separated quantities, tables, paired comparison, biome effects |
| `REGRESSION_ASSESSMENT.md` | constraint × strategy matrix, runtime regression sample, NOT RUN list |
| `PHASE2_VERDICT.md` | answers to the required report items |
| `evidence/` | `run_summaries/*.summary.json` (per run), `forced*.csv`, `elements.csv`, `topblocks_all.csv`, `regression_composite_A1535_jag190.json`, `phase2_stats.json`, one natural padding log |

### `PLACEMENT_TRACE.csv` guide
Subset of the full replay traces: production attempts that are (towers at T ≥ 120) or (T ≥ 160) or (not ACCEPT), from the 8 `full_*` natural seeds and 5 `flat_*` natural worlds (`evidence` = `NATURAL+REPLAY`, `natural_structure_generated` = what the real pipeline stored for that chunk) plus controlled `jagged_peaks` tower attempts (`CONTROLLED_REPLAY`). Columns: seed, chunk, block coordinates, `surface_biome` (at piece centre), `terrain_first_free_y` / `terrain_top_y`, `start_height_sampled` (replica of the engine's first draw; confirmed equal to engine piece `minY − T`), `pool_element`, `element_size_y`, `computed_min_y/max_y`, `limit_max_y`(=315), `stub_y/stub_biome`, `replica_prediction`, `engine_valid`, `engine_log_reason` (the engine's own DEBUG message), `engine_piece_min_y/max_y`. Rows with `COLUMN_BIOME_NONE` are omitted (4.8 M attempts; counts in `evidence/run_summaries`). Full raw traces stay in `build/` (not committed).

## Validation record (exact results)

| Check | Result |
|---|---|
| `./gradlew compileGametestJava` (JDK 25.0.5) | **PASS** |
| `./gradlew test validateStage6Data validateFlameSwordResources validateLowerEquipmentResources --continue` | **PASS** (`BUILD SUCCESSFUL in 52s`) |
| `./gradlew check` on this branch | **FAIL at `:runGameTest`: 1 of the full suite's required tests failed** — `moreminecrafts-gametest:colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise` ("return resumes the same attempt on tick 74"). Unrelated to worldgen / the probe; baseline comparison below |
| Same `runGameTest` on pristine base `038eaef` (git worktree) | **identical single failure** (pre-existing) |
| Probe cross-checks | 0 disagreements: replica vs engine (5.0 M attempts), natural vs replay (85,154 chunks), formula vs engine (260,244 rows) |
| Runtime regression sample (existing C-3 composite probe under candidate A) | snowy tower: 16 spawners, 8 vaults, registered, 1 composite piece |
| `LIVE VALIDATION PENDING` | visual/LOD appearance of lowered islands, client walkthrough, restart persistence under any candidate |
| `NOT RUN` | spawner/vault/barrier probes for B, C, D and for the other five styles under A |

### Baseline game-test comparison
`./gradlew runGameTest` on a git worktree of `038eaef6872e90961afdbf8ccab2581a275332a1` (no Phase 2 changes): **FAIL, the identical single test** — `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise` ("return resumes the same attempt on tick 74"), `1 required tests failed`, `BUILD FAILED in 5m 17s`. The failure is therefore **pre-existing on the base commit and unrelated to Phase 2** (out-of-scope finding; not fixed).

## Resume instructions
Raw results live in `build/sky-tower-phase2/results/` (gitignored). To regenerate: `RUNTIME_REPRODUCTION.md §7`. JDK 25 is required (Loom `options.release = 25`); this environment had JDK 21 only, so a JDK 25 was downloaded to a scratch directory.

## Out-of-scope findings (reported, not fixed)
1. Non-tower islands are also rejected on high terrain (temple at T ≥ 202, plains/ruins at T ≥ 220; flat T=240: 318/745 non-tower fail).
2. General-set towers lose 0.8 % naturally (T ≥ 110); `badlands` 21/434, `forest` 50/6,298.
3. 39 attempts are rejected only by the sky-altitude biome test.
4. Vanilla flat worlds cap the surface at top-block Y=255 (layer index uses `getMaxY()`), so exact T=260 requires an overridden generator (done in-process).
5. One Colosseum game test fails in the full suite (see above).
6. Phase 1 documents contain the corrections listed in `CORRECTED_HEIGHT_MODEL.md §6` (left unchanged as historical evidence).

Recommended next task (suggestion only, not started): owner decision on minimum island clearance, on whether Java is allowed for a terrain-aware structure type, and on accepting tower-free terrain above T ≈ 201/218 — then a bounded implementation task with those decisions as authority.
