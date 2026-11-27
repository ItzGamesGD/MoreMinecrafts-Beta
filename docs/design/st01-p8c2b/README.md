# ST-01 P8-C2-B — complete pyramid natural worldgen integration

Status: **PARTIAL**. The complete pyramid generates naturally and all automated integrity, order-independence, terrain,
fluid, registration, Guardian and persistence checks pass on real Minecraft 26.3 servers. It is PARTIAL because of:
- the measured rarity;
- the unresolved vanilla-overlap policy;
- visual acceptance (§7).

Each needs an owner decision (§8).

Branch `claude/st01-p8c2b-natural-worldgen-isauq8`, based on P8-C2-A `c25f9d44` (contains P8-B2 `03685656`, G0-R1
`c26ea0f8`, D1 `e229d001`). Authority amendment: [AUTHORITY-AMENDMENT-O14-MODERATE.md](AUTHORITY-AMENDMENT-O14-MODERATE.md).

## 1. Sources

| Source | Taken | Not taken |
|---|---|---|
| Phase 4 `ada10ba3` (PR #258) | `St01PlacementConfig`, `St01Eligibility`, `St01PyramidStructure`, `St01Worldgen`, `St01FootprintCorrectionPiece` (rewritten), structure + structure-set JSON, `St01EligibilityTest`, `St01WorldgenResourceTest` (adapted); probe/run-script design | `St01RoyalHallFixture` and its `FluidSealPiece`, the Phase 4 probe, workflow, evidence |
| P8-C2-A `c25f9d44` (PR #259) | the four exported templates (unchanged bytes), `St01PyramidAssembly` transforms, `verify_placement.py` | — |
| P8-B2 `03685656` (PR #254) | `St01RoyalChestPlacement.registerPlaced`, `RoyalChestRuntime`, `RoyalChestData` (unchanged) | — |

Assets (unchanged, SHA-256):
- `pyramid/st01/assembly_east.nbt`: `fe5afb19c2587f1678f2947040c43aec67503b60380599f45934dd6034d6616c`
- `pyramid/st01/assembly_west.nbt`: `55cb285c5a727a5268c708d8fe9ab4ece9f56e995922c1776d936ce2139c2818`
- `pyramid/royal_hall.nbt` (D1): `7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423`
- `pyramid/st01/d1_seam_exception.nbt`: `72f32c062daecb018d4184910e9d11c980b864c527af0383e50178dcae521b89`

## 2. Implementation (`src/main/java/.../pyramid/worldgen`, `pyramid/St01RouteSelection`)

- **Structure.** `St01PyramidStructure` (+ `St01Candidate`): the anchor is the start chunk centre, and the orientation
  is one of four rotations from the start's seeded random. Stages run in order, against the real generator, with no
  chunk loading:
  1. full-footprint Desert;
  2. MODERATE 16/10/8 over all 6,561 columns;
  3. entrance approach;
  4. fluid in the complete underground volume.

  Ground = (cx, median, cz). Pieces, in the approved order: footprint correction, then ASSEMBLY, D1_ROYAL_HALL,
  D1_SEAM_EXCEPTION.
- **Pieces.** `St01AssemblyPiece` uses `St01PyramidAssembly.pieces(route).get(i).settings(orientation)` unchanged, plus
  `knownShape=true` and `IGNORE_WATERLOGGING`. Without `knownShape`, vanilla re-derives the shapes of blocks at the
  clipped chunk edge from neighbouring-chunk contents, which depends on generation order. Vanilla clips each piece to
  the decorating chunk.
- **Terrain.** `St01FootprintCorrectionPiece`: the assembly owns every cell from Y−32 through the full 81×81 Y0 layer.
  So cut and skirt are bounded by eligibility, and the only correction is clearing natural terrain in the structure-void
  cells above Y0. The clearing stays inside the 81×81 footprint, up to the column's natural height + 8, never above
  Y39. Nothing is written outside the footprint, nothing is added, and there is no rectangular clearing.
- **Order-independence guard.** `St01WorldgenGuard` (+ `St01WorldGenRegionGuardMixin`): inside a valid ST-01 completion
  box, only ST-01 pieces may write during world generation. Without it, neighbour-chunk features (fossils, lakes,
  vegetation, ruined-portal parts) overwrote pyramid blocks or not, depending on which chunk generated first. Measured:
  39–1,062 refused writes per start.
- **Route.** `St01RouteSelection`: route = bit63(`mix`(seed, "moreminecrafts:st01_route/x,y,z", salt)) of the transformed
  tomb anchor. 0 = EAST, 1 = WEST. `mix` is the existing P8-B2 hash, and the route does not change the anchor.
- **Completion.** `St01PyramidCompletion`: a persisted, recoverable completion protocol (javadoc).
  - `CHUNK_LOAD` commits a chunk to every ST-01 start it references. The start chunk also records the metadata.
  - Once all 36 box chunks are committed and both PRIMARY chunks are loaded, `END_SERVER_TICK` calls the unchanged
    `registerPlaced` with origin and settings from `St01PyramidAssembly.describe`.
  - Registration is idempotent. A REGISTERED record whose P8-B2 instance is missing is re-registered deterministically.
- **Fluid seal.** The Phase 4 1-block fluid seal is **not retained**. The complete assembly writes every underground cell,
  and its outer faces and floor are solid (the `underground_boundary_sealed` check). The seal would only replace natural
  fluid outside the structure, with no safety gain. Adjacent fluid is reported, not removed: 0–54 blocks per start
  (§5).

## 3. Validation (actual results)

| Area | Evidence | Result |
|---|---|---|
| Unit | `St01EligibilityTest` (18: 16/17, 10/11, 8/9, entrance 1/2, full-volume fluid ±1 incl. treasury/royal hall, biome covering), `St01RouteSelectionTest` (exact reproducibility; 200,000-sample and 160,000-grid 50:50 within 4σ), `St01PyramidCompletionTest` (36 chunks, normal/reverse/random, no metadata ⇒ never complete, codec), `St01WorldgenResourceTest`, plus P8-C2-A/P8-B2 unit tests | 49/49 PASS local; CI `regression` PASS |
| Eight cases through worldgen pieces | `St01NaturalPieceGameTests` (4 rotations × 2 routes, NORMAL/REVERSE/RANDOM chunk order) + `verify_placement.py` on the 8 dumps (full-grid equality, D1 + 12 seam cells, traversal, rooms, secret, route open/filled, Guardian clearance, PRIMARY, torches, sealed boundary, 5/5 negative controls) | 8/8 PASS. Only ServerLevel artifact: in this harness `ServerLevel.setBlock` runs neighbour-shape updates that worldgen does not, dropping one wall torch (16,34,50) for WEST in REVERSE/RANDOM order. That is the only tolerated difference; real-worldgen order identity is from the natural probe |
| Natural worldgen, 20 seeds × 4 phases | `tools/st01-p8c2b/run_validation.sh`: scan (normal order), reverse, random, restart reload; `verify_results.py` | see §4/§5 |
| Guardian regression | `tomb_guardian_game_tests_*` | CI PASS |
| P8-B2 regression | `*royal_chest_game_tests*` | CI PASS |
| P8-C2-A regression | eight-case placement + verifier | CI PASS |
| Visual | §7 | UNVERIFIED |

Per natural start, every phase checks the following in game:
- 0 structure-owned cells differ from the approved templates (303,482 cells);
- 0 non-air cells in structure void;
- 0 fluid in the box;
- 0 non-Desert footprint columns;
- MODERATE respected;
- entrance opening clear, approach ≤ +1;
- registration exactly at the last box chunk (never earlier);
- repeated completion idempotent;
- full lifecycle in scan:
  - PRIMARY left/right → one instance, SECONDARY absent;
  - the first use awakens exactly one Guardian, and the second use stays SEALED;
  - after defeat, the chest is unsealed and the first open is OPENED_AND_GENERATED;
  - the 54-slot shared container is reopened from both halves, insertion is refused;
  - after defeat, interaction returns ALREADY_DEFEATED, and re-registration does not reset anything.

Reload re-checks:
- persisted defeat;
- generated contents byte-equal including the withdrawn slot;
- OPENED without regeneration;
- no Guardian.

Across the four phases, these are identical:
- anchor, orientation, route, canonical id;
- structure-owned hash;
- **whole completion-box hash**.

`verify_placement.py` passes on every natural dump of every phase.

Cross-check: the NORMAL piece-test hashes for EAST/WEST (`0f5bfaab…`) and WEST/WEST (`aac44437…`) are identical to the
whole-box hashes of the natural seed-1004 starts. The natural pyramids equal the ideal placement cell for cell.

## 4. Natural starts (all four pass every check in all phases)

| Seed | Start chunk | Ground | Orientation / route | Δh / cut / skirt | Edge rise / drop | Shell fluid | Guard refusals | Vanilla overlap |
|---|---|---|---|---|---|---|---|---|
| 1004 | −111,168 | (−1768, 70, 2696) | EAST / WEST | 12 / 4 / 8 | 3 / 5 | 7 | 39 | — |
| 1004 | −571,−501 | (−9128, 68, −8008) | WEST / WEST | 10 / 6 / 4 | 6 / 4 | 54 | 775 | mineshaft, 6 pieces |
| 1005 | 7,296 | (120, 69, 4744) | EAST / WEST | 13 / 6 / 7 | **73** / 5 | 8 | 1062 | — |
| 1541117449 | 328,176 | (5256, 65, 2824) | EAST / EAST | 7 / 4 / 3 | 4 / 2 | 0 | 794 | mineshaft, 5 pieces |

Column notes:
- **Edge rise / drop:** the natural surface just outside the footprint, relative to the walking level.
- **Seed 1005, edge rise 73:** natural steep terrain beyond the footprint, untouched by design (Phase 4 measured the
  same, 74).
- **Shell fluid:** natural fluid outside the sealed wall, left in place.

The catalyst draw is NONE for all four starts, so no PRIMARY holds a catalyst (deferred-SECONDARY handling is covered by the P8-B2 GameTests).

## 5. Rarity and rejection stages (CI run 42899510519 on `58646bef`, 20 seeds, ±10,240 blocks each; 20/20 seeds PASS)

| Stage | Total |
|---|---|
| Potential anchors | 32,000 |
| Full-footprint Desert | 221 |
| Terrain-qualified (16/10/8) | 46 (rejected: Δh 160, cut 5, skirt 10) |
| Entrance-qualified | 25 (21 rejected) |
| Fluid-safe = predicted = actual starts | 4 (21 rejected); prediction vs actual mismatches 0 |
| Completed and verified | 4 |
| EAST / WEST | 1 / 3 |
| Orientations | EAST 3, WEST 1 (natural); all four through pieces |

Per seed:
- seed 1004: 2 starts;
- seeds 1005 and 1541117449: 1 start each;
- the other 17/20 seeds: none (full table in the aggregate artifact).

Phase 3.1 reference seeds 1001–1005, desert-qualified counts:

| | 1001 | 1002 | 1003 | 1004 | 1005 |
|---|---|---|---|---|---|
| This run | 17 | 18 | 4 | 8 | 12 |
| Phase 3.1 | 16 | 18 | 4 | 8 | 10 |

Pooled density: **0.048 / 100 km²**.
- 95 % Garwood interval: 0.013–0.122.
- Ratio to the Phase 4 fixture (0.254): 0.19×.
- Ratio to the Phase 3.1 Mansion reference (0.715): 0.07×.

Sampling limitations: only 4 events; 20 seeds × 419 km².

The drop against Phase 4 comes from the two stages that the complete structure adds or enlarges. Of 46
terrain-qualified anchors, 21 are lost to entrance obstruction and 21 to fluid anywhere in the 83×34×83 volume. These
are implementation definitions (§8), not tuned.

## 6. Vanilla structure overlap (investigated, not decided)

Vanilla offers no general mechanism:
- `exclusion_zone` excludes one other structure set within a chunk radius; ancient cities/villages use it, but it cannot
  cover mineshafts;
- terrain adaptation does not prevent overwrites;
- step order (mineshafts in UNDERGROUND_STRUCTURES before ST-01's SURFACE_STRUCTURES) only decides who writes last
  inside one chunk.

Current behaviour: inside the box, ST-01 always wins, deterministically. The template overwrites earlier pieces, and
the guard refuses later ones.

Measured: 2 of the 4 starts intersect a mineshaft (5–6 pieces). The pyramid's sealed wall keeps the corridors out, and
interior sealing passes. No desert village overlapped in these seeds; Phase 4 saw one.

Alternatives for the owner:
- **A.** Keep "ST-01 owns its box" (current).
- **B.** `exclusion_zone` against `minecraft:villages`: vanilla, measurable rarity cost; mineshafts unaffected.
- **C.** Reject candidates whose box intersects any vanilla start. Needs structure-state access in
  `findGenerationPoint` (mixin), so a further rarity reduction.
- **D.** Suppress vanilla mineshaft/village pieces inside the box.

## 7. Visual acceptance — UNVERIFIED

`St01NaturalPyramidClientCaptureTest` + `tools/st01-p8c2b/run_client_capture.sh` capture the following of
naturally generated starts:
- views: exterior, terrain edges, entrance, interior;
- the Guardian encounter and royal chest menu, driven by the real client's use-item-on-block packets.

The local run failed: this container has no GLX visual or Vulkan surface (`BackendCreationException`), and the run
timed out. The CI `client-capture` job of run 42899510519 completed: 26 posed views plus 2 royal-chest-menu screenshots
of the two natural seed-1004 pyramids (artifact `st01-p8c2b-client-captures`, ID 13802178775). The agent could not
download or inspect the images, and Phase 4 saw a capture setup produce sky-only images. So the content of these
captures is unreviewed, and visual acceptance remains UNVERIFIED pending owner review. Real-player walking traversal is NOT RUN; traversal is the voxel navigator of
`verify_placement.py`.

## 8. Owner decisions outstanding

1. **Rarity:** 0.048 / 100 km², 17/20 seeds without a pyramid. Accept, or change parameters (locked; not changed
   here).
2. **Entrance-access definition:** approach row ≤ walking level + 1. It rejects 21/46 terrain-qualified anchors.
3. **Fluid-volume definition:** whole underground block + 1 margin, base-column fluid. It rejects 21/25
   entrance-qualified anchors. The 1-block seal is not retained.
4. **Rotation policy:** four rotations from seeded random, carried over from Phase 4.
5. **Vanilla overlap policy:** §6, A–D.
6. **Visual acceptance** of the CI captures.
