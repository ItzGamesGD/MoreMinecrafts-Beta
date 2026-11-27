# Structure Integrity

No NBT, template pool, loot table, Trial Spawner, Vault, barrier or registration code was modified (`git diff 5c22f9f -- src/main/resources/data/moreminecrafts/structure src/main/java/.../skytower` is empty).

| Requirement | Result | Evidence |
|---|---|---|
| One complete composite tower piece | **RUNTIME_VERIFIED** — `composite_piece_count 1`, `legacy_tower_child_count 0` for snowy, rocky, meadow at T=201 (root `219..315`) | `evidence/integrity/integ_*_T201.composite.json` (existing C-3 probe `SkyTowerCompositeWorldgenProbe` on the production structures) |
| 16 Trial Spawners / 8 Vaults | **RUNTIME_VERIFIED** — 16 / 8 for all three variants at the extreme feasible height | same |
| 72 reserved barrier cells | **RUNTIME_VERIFIED** — `runtime_barrier_air_cells 72`, `blocked_barrier_cells 0` | same |
| Rooftop intact | **RUNTIME_VERIFIED** — crown layer at `maxY` has exactly the reference 64 `smooth_stone` in 6/6 samples per variant at T=201 (maxY 315) and 8/8 natural overworld towers (maxY 310–315; extra non-air blocks are natural snow on snowy towers) | `evidence/rooftop/*.csv` |
| Automatic registration | **RUNTIME_VERIFIED** — `automatically_registered true`, 1 site, site Y = `minY+17 = 236` | integrity JSONs |
| Persistent progression after restart | **RUNTIME_VERIFIED** — snowy T=201 world restarted: 1 site before/after, structure start reloaded, progress preserved, no duplicates | `integ_snowy_T201.restart.json` (note: synthetic progress fixture is 0 there) |
| Existing generated world | **RUNTIME_VERIFIED** — a snowy tower generated and saved by the **baseline** build (root `176..272`, progress 1) was loaded by the adaptive build: structure start reloaded, site and progress unchanged, `pass true` | `compat_baseline_world_*.json` |
| Bridge connectivity | **PARTIALLY_VERIFIED** — the bridge is inside the single composite NBT (unchanged; existing `SkyIslandsResourceTest` and the C-4 static BFS cover it); placement is a rigid Y translation. No new runtime walk-through | — |
| Client walk-through / visuals | UNVERIFIED (`LIVE VALIDATION PENDING`) | — |

The composite probe's overall `pass: false` in single-biome flat worlds only reflects that the other five styles cannot exist there; each found style reports `pass: true`.

## Physical terrain clearance (design §7) — reported separately, no policy change
Measured on 1,250 naturally generated mountain islands (185 towers; 5 seeds) using the lowest actual template block per footprint column (every 4th block) against `WORLD_SURFACE_WG` terrain (`FOOTPRINT_SAMPLES.csv`):

| Quantity | Value |
|---|---|
| Reference-point offset (approved policy) | min 18 (none below) |
| Terrain top under the footprint | median span 17 blocks, max 122 |
| Island bounding-box bottom below the highest footprint terrain | 23 / 1,250 islands (7 / 185 towers) |
| Samples with ≥ 1 column where an island block lies at or below terrain | 11 / 1,250 (0.9 %); towers 2 / 185; 71 of 76,254 sampled columns (0.09 %) |
| Minimum block-level separation (free blocks between terrain and lowest island block) | overall −44, towers −18; median 40 (towers 36), 5th percentile 14 |

Intersections occur on steep terrain (footprint terrain span 25–85) at reference offsets 18–59, i.e. they are a property of the reference-point policy, not of the adaptive rule (the adaptive rule never lowers the offset below 18 and leaves the offset unchanged wherever baseline accepted). **Owner decision needed only if terrain-footprint clearance should be guaranteed**; the approved B-02 does not require it, so nothing was changed. The baseline was not separately measured (INFERRED identical in kind).
