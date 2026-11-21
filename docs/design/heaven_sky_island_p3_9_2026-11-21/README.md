# P3.9 — Habitat first-lookup fix + isolated real-worldgen verification of option D (production worldgen NOT switched)

## 1. Habitat first-lookup delay
Instrumented (`HabitatPerfInstrumentation`, `-Dmm.habitat.instrument=true`): data size is small (≤ ~6k solid cells/template), GC is 0, no allocation spike.
The whole time is in `Set.copyOf(HashSet<BlockPos>)` (4–88 s on large templates): `BlockPos.hashCode` is ((y+z*31)*31+x), so a template's cells have only
~44–55 % distinct hashes and the open-addressing `ImmutableCollections.SetN` probes linearly through huge clusters; each later lookup also cost ~0.24 ms.
Fix: `SkyIslandHabitat.solids` builds a dense `BitSet` mask over the template's bounding box (O(1) lookup, 40–72 KB per large template instead of ~100 MB heap).
`HabitatMaskGameTests` compares it with a reference HashSet over 160 templates (6 themes × 19 originals, 38 heaven, ruins a–d, temple, tower 5–7): membership identical (bad = 0).

| | first call (large originals/heaven) | repeat lookup | memory |
|---|---|---|---|
| before | 7.0 / 10.9 s (a), 19.6 / 21.5 (b), 75.5 / 69.5 (c), 19.2 / 20.2 (d), 26.2 / 30.8 s (e) | ~0.24 ms | ~100 MB heap transient |
| after | max 293 ms over all 160 templates | 200k lookups = 4–18 ms (≈ 0.02–0.09 µs) | 2.65 MB total for all 160 masks |

Habitat decisions and mob spawn rules are unchanged (membership-identical mask; existing Habitat / spawn GameTests in the full regression).

## 2. Real worldgen (dedicated server, seed 42, `tools/heaven-p39/run_worldgen.sh`, probe `HeavenWorldgenProbe`)
The pending pool JSONs are put in the **test world's** `datapacks/` and enabled in `level.dat` before server start, so worldgen registries load with them exactly as at a
production transition (restart). Production pools in `src/main/resources/data/` are untouched. Raw outputs: `worldgen_results/`.
Sites: meadow chunk (32,-24), forest (-22,-14), mountain_meadow (-118,47), located naturally with `findNearestMapStructure`.

| stage | meadow | forest | mountain_meadow |
|---|---|---|---|
| vanilla control | vanilla template, 1215 vanilla-theme blocks | vanilla, 1523 | vanilla, 1060 |
| meadow pack only | heaven templates, 0 vanilla-theme blocks, 2167 heaven blocks | still vanilla (unchanged fingerprint) | **heaven** (shared `sky_islands/meadow/start`), 2233 heaven blocks |
| meadow+forest packs | identical to meadow-only | heaven, 0 vanilla-theme blocks, 2671 heaven blocks | heaven, identical |
Jigsaw blocks left: 0; Habitat support on all standable columns (86/86, 84/84, 78/78; vanilla control 87/87, 76/92, 80/80); a second fresh world gives identical fingerprints (deterministic).
Natural-spawn rule probe (SpawnPlacements.checkSpawnRules, NATURAL): 0 passes in vanilla and heaven alike — **inconclusive**; no player exists on the probe server so `NaturalSpawner` cannot run; spawn rules are covered by the existing GameTests only.

Partial generation / chunk boundaries (real chunk saves, server restart between stages):
* A: vanilla west half generated → pool pack enabled → restart → finished: all-vanilla, final fingerprints equal the pure-vanilla control (no seam, no heaven pieces mixed in).
* B: heaven west half → pack removed (rollback) → restart → finished: all-heaven, fingerprints equal to the pure-heaven run.
Reason: pieces store template locations; block content is read at chunk generation.

## 3. Rollback (`tools/heaven-p39/rollback_check.sh`, rehearsed on a copy)
copy == baseline (58 files, 0 deviations) → `apply`: exactly 6 files differ → `revert`: 0 deviations (byte-identical to `worldgen_baseline.sha256`); repo pools == baseline.
Test world: removing the pack makes new starts vanilla again but already generated heaven pieces keep their heaven template (scenario B). Existing world after a production switch+revert: same.

## 4. Not verified / risks
* NaturalSpawner with a real player (spawn counts) and real-client rendering of worldgen output: NOT RUN / LIVE VALIDATION PENDING.
* Only seed 42, one site per target; other 4 themes remain vanilla (on hold, §4 decision).
* A real existing player world with the pack enabled mid-life was modelled by editing `level.dat`; the production switch (`pending_pools.py apply`) is the owner's decision.
* Approvals needed for production transition: run `apply` (6 files), accept that old chunks keep vanilla and new ones are heaven, accept F7/G2 deferrals.
