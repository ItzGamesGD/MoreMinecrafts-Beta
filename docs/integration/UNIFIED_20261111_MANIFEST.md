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
| GitHub CI on the integration head | see the Draft PR checks (recorded in the final report) |
| ST-01 20-seed worldgen + restart + client capture (`st01-p8c2c-policy-d.yml`) | run via `workflow_dispatch` on the integration branch if accepted by GitHub (recorded in the final report) |
| Sky Tower multi-seed worldgen probes / persistence | covered by the PR-triggered workflows (`worldgen-probe`, persistence jobs) if they run on the PR |

## Excluded work (preserved, unchanged)
Mob track (PAUSED_BY_OWNER), #269 mob+tower candidate, ST-05 pre-wave16 lines, D13-01 (UNVERIFIED), D13 I04a (SUPERSEDED, provisional), #232 (SUPERSEDED), ST-01 Phase 4 / audit / O-14 studies / clock-seam branch, Sky Tower phase-2 validation docs, pre-wave16 docs-only PRs, unrelated-history branches. Reasons per item in the inventory.

## Known defects / incomplete features carried in
- ST-01: visual acceptance UNVERIFIED (P8-C2-C); explorer-map discovery is research only (P8-D1/D2/D2-R1), **not implemented**; vanilla-overlap detection masking in the bounty detector documented (P8-D2), not fixed.
- Sky Tower: as recorded in its own audit documents (`docs/` of int-02).
- D13: I05/I06 royal chest uses the ST-01 port; D13-01 recipe and I04a not integrated.

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
