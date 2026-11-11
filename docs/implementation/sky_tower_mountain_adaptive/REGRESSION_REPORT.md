# Regression Report

All commands run with JDK 25.0.5 (the environment's JDK 21 cannot build: Loom `options.release = 25`).

| Check | Command | Result |
|---|---|---|
| Unit tests incl. new `AdaptivePlacementMathTest` (4 tests: T = 80…260 incl. 201/202) and updated `SkyIslandsResourceTest` | `./gradlew test` (inside `check --continue`) | **PASS** — 594 tests, 0 failures, 0 errors |
| Data / resource validators | `validateStage6Data validateFlameSwordResources validateLowerEquipmentResources validateAccessoryResources validateFishingResources validateFlameSwordProductionAsset` (+ `validateAccessWidener`: NO-SOURCE) | **PASS** |
| `compileJava`, `compileGametestJava` | gradle | **PASS** |
| Full game-test suite (535 tests) — fresh git worktree of **baseline** `5c22f9f` | `./gradlew runGameTest` | 1 failed: `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise` ("return resumes the same attempt on tick 74") |
| Full game-test suite — fresh git worktree of **this branch** (commit `49737ef`) | `./gradlew runGameTest` | **1 failed, the identical test** |
| General sky-island / other structure behaviour | paired replay | 37,199 general-set rows identical to baseline; 11,851 mountain rows identical; 0 differences where the draw fits |

## The 20/25-failure runs (reported, explained)
Game-test runs executed **in the main working directory** failed 25 (concurrently with another server), 20, 25 (legacy JSON restored) times — mostly `SkyTowerGameTests` ("overlapping tower registration" after a first failure) and Colosseum tests. The same happened with **pristine baseline code stashed into the same directory (20 failures)**, whereas both baseline and this branch pass all but the one Colosseum test in a fresh worktree. Conclusion: environmental (stale run state / server overload in that directory), **not** caused by this change; evidence: pristine-baseline-in-same-directory reproduction and the identical fresh-worktree results. I did not investigate the root cause of the directory effect (out of scope).

## Pre-existing failure
`colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise` fails identically on `038eaef` and `5c22f9f` fresh worktrees → not a regression. Not fixed.

## Not run
`scripts/structures/sky_islands/generate.py --check` (needs `nbtlib`, not installed; JSON output was matched by hand) — UNVERIFIED. Client GUI/visual tests — `LIVE VALIDATION PENDING`.
