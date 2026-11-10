# Checkpoint — Sky Tower Mountain Adaptive Placement (Option B)

| Item | Value |
|---|---|
| Implementation branch | `implementation/sky-tower-mountain-adaptive-b` |
| Base | `feature/sky-tower-integration-02` @ `5c22f9fb764d545f0ce4241c75bc47f5b54e447d` (verified, no divergence) |
| Phase 2 branch (reference only, not merged) | `claude/sky-tower-phase-2-validation-twragg` @ `d12a6e114f00055b6cc62e7e272474ed46f33bbb` |
| Final commit SHA / remote verification | reported in the delivery message (a file cannot contain its own SHA) |
| Merge status | not merged into `feature/sky-tower-integration-02` or `main`; no PR |

## Reproduce
```bash
export JAVA_HOME=<JDK 25>
./gradlew test                                   # AdaptivePlacementMathTest, SkyIslandsResourceTest, …
tools/sky-tower-adaptive/run_probe.sh nat_9 9 world -Dmm.ad.radius=2500 -Dmm.ad.naturalMax=20000 -Dmm.ad.footprintSamples=250
tools/sky-tower-adaptive/run_probe.sh controlled 4242 controlled -Dmm.ad.radius=700 -Dmm.ad.biomes=minecraft:jagged_peaks,minecraft:stony_peaks,minecraft:meadow
LEVEL_TYPE=minecraft:flat GENERATOR_SETTINGS='{"biome":"minecraft:jagged_peaks","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":265}]}' \
  tools/sky-tower-adaptive/run_probe.sh flat_snowy_T201 31 world -Dmm.ad.radius=300 -Dmm.ad.naturalMax=3000     # layers = T+64 stone
LEVEL_TYPE=minecraft:flat GENERATOR_SETTINGS='…T=201…' tools/sky-tower-adaptive/run_integrity_probe.sh integ_snowy_T201 31 60   # then … 60 1 for restart
python3 -I tools/sky-tower-adaptive/analyze.py build/sky-tower-adaptive/results docs/implementation/sky_tower_mountain_adaptive --nat nat_2 … --controlled controlled --flat … --det det_a det_c
```
Raw per-candidate CSVs stay in `build/` (controlled `rows.csv` is 102 MB); committed: aggregates, 92 natural T ≥ 160 samples, 1,250 footprint samples, run summaries, integrity/rooftop evidence.

## Deliverables
`IMPLEMENTATION_REPORT.md`, `PLACEMENT_ALGORITHM.md`, `HEIGHT_BOUNDARY_RESULTS.csv`, `NATURAL_GENERATION_RESULTS.md`, `DISTRIBUTION_COMPARISON.md`, `STRUCTURE_INTEGRITY.md`, `PERFORMANCE_AND_DETERMINISM.md`, `REGRESSION_REPORT.md`, `CHECKPOINT.md`, `evidence/`.

## Verification summary
Boundary: 39 controlled cells, 0 policy violations; T=201 feasible (797/797), T≥202 rejected (0/797) for all 3 variants; same on real flat worlds (120/120 vs 0/120). Natural: 49,099/49,099 pipeline==replay. Determinism: 3 identical runs. Integrity: 16/8/72, rooftop, registration, restart, baseline-world load. Regression: tests 594/0; game tests identical to baseline in a fresh worktree.

## Out-of-scope findings (not fixed)
Pre-existing Colosseum game-test failure; environment-dependent mass game-test failures in a stale directory; `generate.py --check` unrunnable without `nbtlib`.
