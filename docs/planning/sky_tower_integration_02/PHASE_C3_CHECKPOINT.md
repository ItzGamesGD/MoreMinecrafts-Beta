# Phase C-3 continuation checkpoint

- Branch: `feature/sky-tower-integration-02`
- Implementation base HEAD: `1f45fd104dead53449b0c421841b2feeaf9a113b`
- Result commit: this checkpoint is included in the C-3 implementation commit; see `git rev-parse HEAD`
- Push: pending remote fetch/status comparison
- Scope: six composite `sky_islands/<style>/tower.nbt` assets, deterministic NBT generator and validator, Structure Start auto-registration, unit/GameTest coverage, opt-in fresh-world server probe.
- Approved `full_01.nbt` SHA-256 remains `cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27`.
- Start pool entries and all existing island structure/structure-set definitions remain unchanged; weighted `tower` entry remains 3.
- C-2 coordinate choice remains provisional and is not a final visual approval.

## Reproduction

Use JDK 25 and `GRADLE_USER_HOME=/workspace/gradle-home` in the current workspace.

```sh
python3 -m unittest scripts.structures.sky_islands.test_tower_composite -v
python3 scripts/structures/sky_islands/validate.py
python3 scripts/structures/sky_islands/generate.py --check
./gradlew test --offline
./gradlew -Dmmskytower.testFilter='moreminecrafts-gametest:sky_tower_game_tests*' -I docs/planning/sky_tower_integration_02/c3-focused.init.gradle runGameTest --offline
tools/sky-islands-probe/run_tower_composite_probe.sh <seed> <radius-in-chunks>
```

The natural-worldgen probe writes JSON under `build/sky-tower-composite-probe/results/`. Its overall `pass` is false when any of the six styles were not encountered; inspect each style row before classifying a technical failure. Current samples are `PHASE_C3_SERVER_PROBE_RADIUS24.json`, `PHASE_C3_SERVER_PROBE_RADIUS160.json`, `PHASE_C3_SERVER_PROBE_SEED42.json`, and `PHASE_C3_SERVER_RESTART.json`.

## Remaining verification

Natural Jigsaw samples for snowy, badlands and desert were not found across the completed radius-160 scans for seeds `2026110903` and `42`; they remain `UNVERIFIED`. Meadow was observed in seed `42`. A graceful stop/restart probe passed: the same Site remained unique (1→1), its Structure Start reloaded, and the saved progress marker remained 1→1 (test API marker, not combat completion). That marker was set through the test API and does not represent a completed combat encounter. Client movement, appearance and multiplayer remain post-integration QA.
