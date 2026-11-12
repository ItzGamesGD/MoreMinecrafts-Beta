# Colosseum clock-seam remediation: local verification (2026-11-10)

Subject: `remediation/st01-colosseum-clock-seam-20261110` @ `f2c0a6c88d1751080f8113688e275a5fa8b4104a`
= `work/st01-p8c2a-complete-assembly-20261110-r1` @ `c25f9d44` + the two commits of PR #240 (`9691d1b5`, `f6c45ee3`) cherry-picked with `-x`.
Tree id of the branch tip: `62758e86c83dd8d96b40d2291824d218df51aafa` (identical to the tree that was built and tested below).

## What the change is

6 files, **all under `colosseum/` paths** (0 files elsewhere), +178/-11:

| File | Kind |
|---|---|
| `src/main/.../colosseum/ArenaController.java` (+7) | adds `useClock(LongSupplier)`, `clockOverride()` and `now(serverNow)`; the override is "never serialised", so a restored controller always uses the server wall clock |
| `src/main/.../colosseum/ColosseumRuntime.java` (2 lines) | `ArenaRecovery.open(...)` and `tickSite(...)` receive `d.controller(id).now(now)` instead of `now` |
| `src/gametest/.../ManualClock.java` (new), `ColosseumGameTests.java`, `ColosseumCombatGameTests.java` | tests drive wall-clock timers from a per-arena manual clock |
| `src/test/.../ArenaControllerClockTest.java` (new, 2 tests) | JUnit coverage of the seam |

Behaviour preservation: with no override installed `now(serverNow)` returns `serverNow` unchanged, so production behaviour is identical by construction. This is an inspection plus the unit test, not a proof against the live game.

## Environment

Linux, 4 cores, 15 GB RAM, Temurin 25.0.5+8 (the project requires `release = 25`), Gradle 9.6.0 wrapper, Fabric Loom 1.17.21, Minecraft 26.3. Each run used a **fresh git worktree** (no prior `run/` or `build/`), `--no-daemon`, and no other CPU-heavy work during the GameTest (it is timing-sensitive).

## Results

| Run | Tree | Command | Result |
|---|---|---|---|
| A. fix applied | `62758e86…` | `./gradlew test compileGametestJava` | `BUILD SUCCESSFUL in 1m 46s`; JUnit 130 suites, **594 tests, 0 failures, 0 errors**, incl. `ArenaControllerClockTest` 2/2 |
| A. fix applied | `62758e86…` | `./gradlew runGameTest` | `BUILD SUCCESSFUL in 4m 36s`; **"All 551 required tests passed"** |
| B. clean control, unmodified tip `c25f9d44` | `c25f9d44` | `./gradlew runGameTest` (fresh worktree) | `BUILD FAILED in 4m 46s`; **"1 required tests failed"**: `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise :: grace running on tick 49` (identical to the failure GitHub Actions recorded for this exact head: Fabric CI run `42794946788`, `:runGameTest` failed, '1 required tests failed', same test) |
| C. contaminated control (**excluded**) | `c25f9d44` | `./gradlew runGameTest` in the worktree previously used for run A | `BUILD FAILED`, **18** Colosseum tests failed with `Cannot read field … because … is null` |

Run C is **not evidence**: it reused a worktree whose saved GameTest world from run A was still on disk, producing a failure mode that CI (always a clean checkout) never shows (CI had exactly 1 failure on this head). It is recorded here so nobody finds the number later and misreads it. Run B replaces it.

## What this does and does not establish

* Established: the same machine, same command, same base: unmodified tip fails exactly the CI failure; tip + the two commits passes the full suite and unit tests.
* Not established: that the seam is the *cause* of the CI difference. n = 1 per arm locally; the supporting evidence is the CI cross-tabulation (all 11 wave16 descendants containing #240 or #242 pass (final snapshot); 15 without either fail; 7 without either pass).
* Not run: live (in-game) validation; the PR-level CI of this branch (no PR was opened, and `build.yml` triggers on pull requests, not on pushes to this branch name).

## Not applied anywhere else

No existing branch was modified. Applying the same change to other ST-01 stack members, and the design-linked Tomb Guardian test alignment for D13 I01-I04a, are owner decision D-8.
