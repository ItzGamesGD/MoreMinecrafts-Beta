# Sky Tower Phase C-4 — Functional Integration QA & Stabilization

Branch `feature/sky-tower-integration-02`, base HEAD `833773b5a3593ffb1a40f8f7cab3f156a3ed0be8` (local == remote at start, clean tree).
Approved tower `full_01.nbt` SHA-256 unchanged: `cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27`. No `tower.nbt`, start pool, weight, structure or structure-set file was modified.

Evidence labels used in every C-4 report: **[OBSERVED]** directly observed in a running Minecraft server; **[SIM]** automated simulation / hook-driven synthetic state; **[STATIC]** code or data inspection; **[ASSUMED]**; **[UNVERIFIED]**; **[DEFECT]**.

## Classification

| Class | Result |
|---|---|
| IMPLEMENTATION COMPLETE | **YES.** All C-3 systems exist; no confirmed gameplay defect found. One tooling defect found and fixed (see below). |
| SERVER FUNCTIONALLY VERIFIED | **YES, with listed gaps.** Real vanilla Trial Spawner combat → completion → barrier removal observed on floor 0 (3 rotations, 1–2 players, destroy-during-combat). Natural generation, auto-registration, anchors, initial barrier state observed for all six styles and all four rotations. Barrier removal survives a real dedicated-server stop/start. |
| CLIENT QA PENDING | **YES.** No GUI client: movement, visuals, real multiplayer not exercised. |
| BLOCKED | **NO.** |

Real combat was exercised **only on floor 0**. Floors 1–3 use the same code path and spawner config, but real combat there is **[UNVERIFIED]** (floors 1–3 completion is covered by hook/synthetic tests). Label: `LIVE COMBAT QA PENDING` for floors 1–3 and for natural-cooldown reactivation (vanilla cooldown is 30 min; only a synthetic state write was used).

## Key results

1. **Real vanilla Trial Spawner lifecycle [OBSERVED]** — a survival `ServerPlayer` (no completion marker, no cooldown block write, no manual `reconcile`) was detected by vanilla; vanilla spawned real zombies (vanilla scaling: 6 per spawner for one player = 24 kills; 8 per spawner for two players = 32 kills); player-attributed damage killed them; each spawner walked `WAITING_FOR_PLAYERS → ACTIVE → WAITING_FOR_REWARD_EJECTION → EJECTING_REWARD → COOLDOWN`. The completion bit was **not** set before `COOLDOWN` (asserted every tick) and was set at it. 4/4 → the real 20-tick barrier tick removed the 18-cell floor-0 wall; floor 1 wall remained; second tower untouched. Details: `PHASE_C4_TRIAL_SPAWNER_TESTS.md`.
2. **Natural worldgen [OBSERVED]** — all six styles and rotations 0–3 sampled; snowy/badlands/desert closed via a controlled single-biome flat world. `PHASE_C4_WORLDGEN_MATRIX.md`.
3. **Barrier persistence across restart [OBSERVED]** on a natural rotation-2 desert tower: walls installed (18+18 owned) → 4 real spawner removals → floor 0 opened, floor 1 stayed → server stop → start: mask 15 kept, floor 0 stays open (not re-walled), floor 1 still walled and owned.
4. **Walkability [STATIC]** — BFS on all six composite NBTs: island connector → bridge → entrance → floors 0–4 → rooftop (y 84) reachable with no walls; with the 72 barrier cells solid only floor 0 (4 spawners) is reachable, i.e. the gating geometry is correct. Not a player walkthrough: `CLIENT WALKTHROUGH QA PENDING`.
5. **Regression**: JUnit 590/590; Sky Tower GameTests 8/8 (5 existing + 3 new); full GameTest suite 534/535 — the one failure is the known Colosseum absence-grace test (passes in isolation). `PHASE_C4_REGRESSION_REPORT.md`.

## Defects

- **[DEFECT, fixed, tooling]** `generate.py --check` compared composite `tower.nbt` as raw gzip bytes, which depends on the local zlib build. On this machine (zlib 1.3) it reported all six composites stale although the decompressed NBT payload is byte-identical to the generator output (verified). Fixed to compare the decompressed payload. Assets untouched.
- **No gameplay/worldgen/progression/barrier defect confirmed.**

## Changes in this phase

- `src/gametest/.../SkyTowerGameTests.java`: +3 real-combat GameTests (+ survival-player helper).
- `src/gametest/.../SkyTowerCompositeWorldgenProbe.java`: opt-in `-Dmm.sky.tower.persistFloorClear` fixture (force-load, real spawner removals, state report before/after restart).
- `tools/sky-islands-probe/run_tower_composite_probe.sh`: optional env `LEVEL_TYPE`, `GENERATOR_SETTINGS`, `PERSIST_FLOOR_CLEAR` (defaults unchanged).
- `scripts/structures/sky_islands/generate.py`: payload comparison in `--check`.
- `docs/planning/sky_tower_integration_02/phase_c4_walkability.py` + `phase_c4_evidence/` (raw JSON/logs).
- No production Java under `src/main` was changed.

## Environment and commands

JDK 25 (`/tmp/jdk25/jdk-25.0.5+8`, Temurin), Gradle 9.6.0 via wrapper, Minecraft 26.3, Fabric/Loom 1.17.21. Container had only JDK 21; JDK 25 was downloaded. `pip install nbtlib==2.0.4` (already pinned by the repo). One transient Maven Central HTTP 429 was retried.

```sh
export JAVA_HOME=/tmp/jdk25/jdk-25.0.5+8
./gradlew test --no-daemon
./gradlew -Dmmskytower.testFilter='moreminecrafts-gametest:sky_tower_game_tests*' \
  -I docs/planning/sky_tower_integration_02/c3-focused.init.gradle runGameTest --no-daemon
./gradlew build --no-daemon
python3 -m unittest scripts.structures.sky_islands.test_tower_composite
python3 scripts/structures/sky_islands/validate.py && python3 scripts/structures/sky_islands/generate.py --check
python3 -I docs/planning/sky_tower_integration_02/phase_c4_walkability.py out.json
```

## Not claimed

No client walkthrough, no real multiplayer, no visual/aesthetic approval, no custom mob pool, no rooftop special reward (deferred by design). See `PHASE_C4_POST_QA_CHECKLIST.md`.
