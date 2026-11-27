# Unified integration checkpoint manifest — 2026-11-11

| Field | Value |
|---|---|
| Integration branch | `integration/unified-20261111` |
| Base | `checkpoint/wave16-unified-20261104` @ `65072bcaa5f645feb6e524557f5e77b7e40990ff` |
| Strategy | `git merge --no-ff` of three development-line tips in dependency order; conflicts resolved only where the behavior is unambiguous (recorded below); no cherry-picks, no history rewriting, no source branch modified |
| Inventory and dependency graph | [UNIFIED_20261111_INVENTORY.md](UNIFIED_20261111_INVENTORY.md) |

## Integration sequence
| Batch | Incoming ref @ SHA | Resulting integration SHA | Conflicts | Targeted check after batch |
|---|---|---|---|---|
| B1 Sky Tower | `feature/sky-tower-integration-02` @ `25e6ed6e` (PRs #263, #267, #271) | `e2cb3cbe` | none | (included in the B2 compile) |
| B2 D13 accessory line | `implement/d13-03-i06-p4-st01-integration-20261108` @ `6c7923e0` (#228, #233-#235, #237, #241-#243, #245) | `60a0aa71` | 1 (test) | `compileJava compileTestJava compileGametestJava`: PASS |
| B3 ST-01 line | `research/st01-p8d2-r1-search-audit-20261111` @ `d01ca8ac` (#231…#272) | `ed0bf7b8` | 9 | compile: PASS; full suite: PASS (below) |

## Conflict resolution record
| # | File | Sides | Resolution | Why it is unambiguous |
|---|---|---|---|---|
| C1 | `src/gametest/.../colosseum/ColosseumGameTests.java` (`absenceGraceResumesOnReturnAndExpiresOtherwise`) | Sky Tower #267: widen the grace to 10,000 ms and re-arm a 1 ms timing; D13: drive the 1,500 ms grace through the deterministic per-arena `ManualClock` | **D13 version** of the file (drops #267's 3-line change) | Both are test-only fixes of the same CI-timing flake; the textual auto-merge mixed them into a contradictory test (1 ms re-arm vs a 1,500 ms assertion). The deterministic-clock version is the stronger fix and is used identically in three lines (patch-identical commits). No gameplay code involved. |
| C2–C7 | `RoyalChestGameTests.java`, `PyramidRoyalChestRewards.java`, `RoyalChestContainer.java`, `RoyalChestData.java`, `RoyalChestRuntime.java`, `PyramidRoyalChestRewardsTest.java` (add/add) | D13 I05/I06-P3 (structure-independent, "not yet wired") vs ST-01 P8-B2 | **ST-01 version** | ST-01 commit `c9480752` states it "selectively port[s] P3 engine from 97afeb66 and I05 pure accessory draw from 18362bb9 … preserve D1 Guardian sources and reward-model probabilities", and wires the engine to the generated pyramid; D13's own P4 review (`6c7923e0`) recorded that integration as blocked on exactly what P8-B2/P8-C2 later delivered. Same approved design (I06 Q01–Q45), later and wired implementation. All other D13 royal-chest files were byte-identical in both lines. |
| C8 | `MoreMinecrafts.java` (initializers) | D13: 4 accessory initializers + `RoyalChestRuntime` + `PyramidRoyalChestRewards.initialize()`; ST-01: `RoyalChestRuntime` + `St01Worldgen` | union: the 4 D13 initializers, `RoyalChestRuntime.initialize()` **once**, `St01Worldgen.initialize()`; `PyramidRoyalChestRewards.initialize()` dropped | the ST-01 `PyramidRoyalChestRewards` (chosen in C3) has no `initialize()`; registering the runtime twice would double its event hooks |
| C9 | `src/gametest/resources/fabric.mod.json` | Sky Tower probes vs ST-01 probe; ST-01 GameTest classes | union of both entry lists | additive registrations; JSON validated |
| C10 | `docs/CONTENT_AUTHORITY_INDEX.md` | D13 sections vs ST-01 sections appended at the same place | union, D13 sections first then ST-01 (chronological) | append-only index entries; none edits another |

## Validation (combined tree `ed0bf7b8`, local, 4-vCPU container, JDK 25.0.5)
| Check | Result |
|---|---|
| `./gradlew clean test build compileGametestJava runGameTest --no-daemon` (the Fabric CI command) | **PASS** — BUILD SUCCESSFUL in 5 m 28 s |
| Unit tests | **681 / 681 PASS** (146 test classes, 0 skipped) |
| GameTests | **All 576 required tests passed** |
| Mixin config duplicates / lang JSON validity | PASS (118 mixins, no duplicates) |
| Sky Tower multi-seed worldgen probes / persistence | covered by the PR-triggered workflows (see CI below) |

## GitHub CI on `0743aa7480f714206ce3113348ceb19761c04b02` (Draft PR #273 + `workflow_dispatch`)

53 check runs: **49 success, 2 skipped (security-scan follow-up jobs gated on the scan), 2 failure**. Neither failure is caused by the integration; both are attributed with independent evidence below. Because two checks are red, this checkpoint is a **CANDIDATE, not VERIFIED**.

| Workflow / job | Result | Evidence |
|---|---|---|
| Fabric CI `build` (`clean test build compileGametestJava runGameTest`), `server-smoke`, `client-smoke` | PASS | run 42999622576 |
| ST-01 P8-C2-C Policy D natural worldgen (`workflow_dispatch`): `regression`, `guardian-regression`, 20 × `worldgen (seed)`, `client-capture`, `aggregate` | **PASS 24/24** | run 42999635096 |
| ST-01 P8-C2-A complete assembly: `blueprint-and-export`, `guardian-regression`, `client-capture` | PASS | run 42999622681 |
| ST-01 P8-C2-A `actual-placement` | attempt 1 **FAIL**, attempt 2 **PASS** — classified **INTERMITTENT (timing)** | attempt 1 failed in GameTest `royal_chest_game_tests_codec_round_trip_preserves_state_without_reroll_or_restock` ("one guardian, got 0 on tick 0", `RoyalChestGameTests.defeat`, with "Can't keep up! 2072ms behind" on the runner). The identical command (`-Dmm.royal.placement=true -Dfabric-api.gametest.filter=moreminecrafts-gametest:*royal_chest_game_tests*`) on the identical SHA passed in run 42999635096 `regression`, in the separate `royal-chest` workflow (run 42999622476), in the full `build` GameTest suite, in one diagnostic re-run of the failed job (attempt 2), and twice locally. No code was changed and no assertion weakened. Root cause of the timing sensitivity not investigated (out of scope). |
| `full-history-secret-scan` (gitleaks over every remote ref) | **FAIL — repository-wide, not introduced** | 1 finding, rule `generic-api-key`, `tools/st05-p0-probe-01/freeze_integrity.py`, commit `cec8bff8` (2026-11-01). That commit is **not reachable from this branch** (`git merge-base --is-ancestor cec8bff8 HEAD` false); it is only on `audit/st05-po25-review-package-20261102` and `probe/st05-p0-probe-01`. The scan fails for any PR to `main` while those refs exist. Owner action required (review/allowlist or branch decision); not resolved here because it needs history/branch decisions outside scope. |
| `client-visual-validation` | **FAIL — inherited from the wave16 baseline** | `CLIENT_UI_INTERACTION_FAILED XP Quest selector did not open the three-node tree with all 37 bestiary targets`. The harness (`VisualValidationHarness.java:408`, unchanged since the root commit) hard-codes 37; the target list is every `moreminecrafts` `MobCategory.MONSTER` entity type (`ProgressionScreenData.java:74`). wave12 (last PASS, run 40608002343) registered MONSTER types only in `CustomMobCatalog`; the wave16 base `65072bca` additionally registers the Royal Tomb Guardian (`02d0973e`) and Gladiator (`408cfda1`), so the count is no longer 37. The identical failure occurs on `work/st01-o14-phase1-phase2-20261109` @ `80b3bbbe` (run 42600760884), which descends from wave16 and contains none of this integration's three merges. Fixing it requires a design decision (whether bosses/arena enemies count as Bestiary species, or the expected count), so it is **not changed** here. |
| All other PR-triggered jobs (persistence, K5 repeats, restart, reward-restart, crafting source, mapped sources, worldgen-probe ×3, Sky Tower blueprint/static/coverage, real client/TestInput, `check`) | PASS | PR #273 checks |

## Excluded work (preserved, unchanged)
Mob track (PAUSED_BY_OWNER), #269 mob+tower candidate, ST-05 pre-wave16 lines, D13-01 (UNVERIFIED), D13 I04a (SUPERSEDED, provisional), #232 (SUPERSEDED), ST-01 Phase 4 / audit / O-14 studies / clock-seam branch, Sky Tower phase-2 validation docs, pre-wave16 docs-only PRs, unrelated-history branches. Reasons per item in the inventory.

## Known defects / incomplete features carried in
- ST-01: visual acceptance UNVERIFIED (P8-C2-C); explorer-map discovery is research only (P8-D1/D2/D2-R1), **not implemented**; vanilla-overlap detection masking in the bounty detector documented (P8-D2), not fixed.
- Sky Tower: as recorded in its own audit documents (`docs/` of int-02).
- D13: I05/I06 royal chest uses the ST-01 port; D13-01 recipe and I04a not integrated.
- Baseline (wave16) defect: `client-visual-validation` bestiary target count (above) — needs an owner decision.
- Repository-wide: gitleaks finding on commit `cec8bff8` reachable from two non-integrated refs — needs an owner decision.
- Royal chest GameTest timing sensitivity on slow runners (one intermittent failure out of six executions on this SHA).

## Checkpoint refs
| Ref | SHA | Status |
|---|---|---|
| `checkpoint/unified-20261111-candidate-1` | `0743aa7480f714206ce3113348ceb19761c04b02` | CANDIDATE (code tree validated above; manifest without CI record) — immutable, not moved |
| `checkpoint/unified-20261111-candidate-2` | the commit that adds this CI record (docs only; code tree identical to candidate-1) | CANDIDATE — not VERIFIED because two CI checks are red for pre-existing reasons |

No `checkpoint/unified-20261111` (VERIFIED) ref was created.

## Reproduction
```
git fetch origin
git checkout -b repro origin/checkpoint/wave16-unified-20261104
git merge --no-ff origin/feature/sky-tower-integration-02            # B1, no conflicts
git merge --no-ff origin/implement/d13-03-i06-p4-st01-integration-20261108   # B2, resolve C1 with the incoming version
git merge --no-ff origin/research/st01-p8d2-r1-search-audit-20261111         # B3, resolve C2-C10 as above
./gradlew clean test build compileGametestJava runGameTest --no-daemon
```
The resulting tree equals `ed0bf7b8` (compare with `git diff ed0bf7b8`).
