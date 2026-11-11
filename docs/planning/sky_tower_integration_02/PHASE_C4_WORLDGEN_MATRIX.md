# C-4 Natural worldgen matrix

All rows are **[OBSERVED]** natural vanilla Jigsaw generation in a real dedicated server (fresh world, `run_tower_composite_probe.sh`; structure starts found by scanning `STRUCTURE_STARTS` chunks, then FULL load). Direct template placement (GameTest) is **not** counted here.
Per sample the probe asserts: tower piece template == the style's composite; exactly 1 composite piece; 0 legacy `tower_5/6/7` children; 16/16 Trial Spawners and 8/8 Vaults at the expected (rotated) anchors; 72/72 reserved barrier cells are air; all piece chunks loaded; automatic registration. Raw JSON in `phase_c4_evidence/`.

| Style | Source | Seed / world | Radius (chunks) | Rot | Site (x,y,z,r) | piece bounds | Result |
|---|---|---|---|---|---|---|---|
| forest | C-3 | 2026110902, normal | 24 | 3 | -288,154,160 | X-288..-240 Y137..233 Z103..160 | PASS (C-3 data, not re-run) |
| forest | C-3 | 2026110903, normal | 160 | 0 | 1600,199,192 | | PASS (C-3) |
| rocky | C-3 | 2026110903, normal | 160 | 1 | 240,137,672 | | PASS (C-3) |
| meadow / forest / rocky | C-3 | 42, normal | 160 | 0 / 3 / 0 | 528,158,-1488 / -352,132,-224 / 1872,171,-880 | | PASS (C-3) |
| **desert** | C-4 | 7001, flat desert | 64 | 1 | 176,1,-352 | X128..176 Y-16..80 Z-352..-295 | PASS |
| desert | C-4 | 7201 | 100 | 0 | -176,15,-560 | | PASS |
| desert | C-4 | 7202 | 100 | **2** | 1008,20,-160 | | PASS |
| desert | C-4 | 7203 | 100 | 3 | 400,20,624 | | PASS |
| **badlands** | C-4 | 7102 | 160 | 0 | -224,65,-224 | | PASS |
| badlands | C-4 | 7104 | 160 | 3 | 176,52,-304 | | PASS |
| **snowy** | C-4 | 7103 | 160 | 3 | 528,40,80 | | PASS |
| snowy | C-4 | 7105 | 160 | 0 | 528,2,192 | | PASS |

All PASS rows: 16 spawners, 8 vaults, 72 air cells, `automatically_registered=true`, `composite_piece_count=1`, `legacy_tower_child_count=0`, `blocked_barrier_cells=0`, `chunks_loaded=true`.

Negative bounded runs: seeds 7002 (badlands) and 7003 (snowy), radius 64 → no tower start (tower is 3/21 ≈ 14.3% of island starts; absence is not a defect). Radius-160 retries (7102–7105) found them. Total bounded effort: 9 C-4 world scans; no unbounded searching.

## Method and caveats
- Controlled biome: `LEVEL_TYPE=minecraft:flat`, `GENERATOR_SETTINGS={"biome":"minecraft:<b>","layers":[bedrock,stone x3,<surface>]}` (do **not** pass `structure_overrides`; `[]` disables all structures). This is genuine vanilla structure generation with the biome pinned, **not** the default overworld terrain. Island heights (e.g. y=1, 2) are artefacts of the flat surface (`project_start_to_heightmap` + `start_height` 40..110 above a −60 surface).
- Therefore terrain/biome-border interplay, real-terrain island visibility, and island-to-neighbour distances in default worlds remain as C-3 observed them (forest/rocky/meadow only) — **[UNVERIFIED]** for desert/badlands/snowy on default terrain.
- Rotations 0,1,2,3 were all observed naturally (rot 2 only in desert 7202). Rotation transforms are additionally exercised by the GameTests across 4 rotations.
- "No major truncation" is evidenced by full anchor/vault/air counts and all piece chunks loading; it is not a visual check **[UNVERIFIED visually]**.

## Reproduce
```sh
export JAVA_HOME=/tmp/jdk25/jdk-25.0.5+8 LEVEL_TYPE=minecraft:flat
export GENERATOR_SETTINGS='{"biome":"minecraft:desert","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3},{"block":"minecraft:sand","height":1}]}'
ALLOW_PARTIAL=1 tools/sky-islands-probe/run_tower_composite_probe.sh 7202 100   # results in build/sky-tower-composite-probe/results/7202.json
```
Badlands: biome `minecraft:badlands`, surface `sandstone`, seed 7102, radius 160. Snowy: `minecraft:snowy_plains`, `snow_block`, seed 7103, radius 160.
