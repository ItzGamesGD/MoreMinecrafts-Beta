# P3.10 — final evidence for the production transition of meadow / forest heaven islands (production worldgen NOT switched)

Branch `claude/heaven-sky-island-p3-10-production-evidence-20261121` (on top of P3.9). No production pool, structure ID / set / probability / biome / spawn rule,
NBT (114 originals + 38 heaven) or P3.6 T1 texture was changed. Tools: `tools/heaven-p39/{run_worldgen.sh,run_realplayer.sh,rollback_check.sh}`,
probes `HeavenWorldgenProbe`, `HeavenP310RealPlayerSpawn`, `HabitatBeforeAfterGameTests`; raw data in `worldgen_results/`, `habitat_before_after.txt`.

## 1. Habitat first-lookup delay
Cause (P3.9, re-confirmed by the P3.10 before/after test): `Set.copyOf(HashSet<BlockPos>)` on clustered `BlockPos.hashCode` values; every first lookup built the set (4–65 s) and
every later lookup cost 0.07–0.35 ms. Fix: dense BitSet mask (`SkyIslandHabitat.solids`). Correction to the P3.9 report: the old structure retained only 2–6 MB (not ~100 MB); the cost was CPU time, not heap.

Large islands, legacy (pre-P3.9) vs mask, same JVM, 5 large templates x {meadow, forest} x {original, heaven} (`habitat_before_after.txt`, 20 templates, all answers identical):

| template group | legacy first lookup | legacy 100k lookups | legacy heap | mask first call | mask 100k lookups | mask size |
|---|---|---|---|---|---|---|
| large_a | 4.3–5.8 s | 6.8–12.1 s | 2–3 MB | 17–40 ms | 0.3–3.1 ms | 37–39 KB |
| large_b | 14–19 s | 11–18 s | 3 MB | 19–22 ms | 0.3 ms | 47–50 KB |
| large_c | **50.7–64.5 s** | 25–35 s | 5–6 MB | 27–74 ms | 0.3 ms | 67–70 KB |
| large_d | 12–15 s | 17–27 s | 5 MB | 25–41 ms | 0.3–1.5 ms | 55–60 KB |
| large_e | 20–22 s | 17 s | 5 MB | 42–69 ms | 0.3 ms | 60–65 KB |

Original and heaven templates behave the same (heaven = same geometry). Watchdog: the dedicated server's default `max-tick-time` is 60 s; the legacy first lookup on meadow large_c was 64.5 s
(> 60 s), so the old code could have crashed a default server; the mask's worst first call is 74 ms. In real worldgen (31 natural sites incl. 137x101x155 islands, `HeavenWorldgenProbe`) the worst single Habitat call was 107 ms and
the worst first call 36 ms. Habitat decisions: mask == former set on all 160 templates (P3.9 GameTest, bad = 0); existing Habitat / spawn GameTests pass in the full regression.

## 2. Real worldgen, isolated test worlds only (dedicated server, seed 42, 12 search origins per theme -> 11 meadow / 9 forest / 5 mountain_meadow distinct natural starts, including every size class up to 137x101x155)
Pool pack = the pending D-option pool JSONs as a world data pack (`level.dat`-enabled), world restarted between stages (registries only load at world start, exactly like a production switch).

| stage | meadow (11 sites) | forest (9) | mountain_meadow (5) |
|---|---|---|---|
| vanilla control | vanilla templates | vanilla | vanilla |
| **meadow 3 pools only** | all 11 heaven templates | all 9 still vanilla (fingerprints unchanged) | all 5 heaven (shared `sky_islands/meadow/start`) |
| **meadow + forest 3 pools** | identical to meadow-only | all 9 heaven | identical |

* Jigsaw connection: 0 unresolved jigsaw blocks in every site; the island bounding boxes are identical to vanilla in all 25 sites (same footprint).
* Structure function: loot-table containers and spawners per site are identical to vanilla in all sites (e.g. meadow#1 16 spawners, forest#6 4 loot containers + 2 spawners, meadow#10 2 + 1, forest#11 2 + 1).
* Vanilla-theme blocks inside piece boxes: forest 0 / mountain_meadow 0 / meadow 0 except meadow#1 (2 690 natural dirt/grass/leaf/grass cells inside the tall Sky Tower piece box, which reaches down to the natural terrain; the same pieces on mountain_meadow#9 show 0). Heaven templates themselves contain no vanilla terrain (P3.8 JUnit).
* Habitat: every standable column on heaven islands is a Habitat cell (meadow 8442/8665 -> all heaven islands 100 % except meadow#1's tower-foot natural terrain; forest 2158/2158; mountain_meadow 4054/4054). Vanilla islands show the same behaviour (e.g. forest 70/87).
* Save / reload: every stage saved and restarted; reloaded block fingerprints equal the pre-restart ones for the same-stage reruns (see section 3 noise note).

### Natural spawning with a REAL player (`HeavenP310RealPlayerSpawn`, real client connected to the dedicated server, survival, midnight, view distance 8, 300 `NaturalSpawner` attempts per island chunk + 600 ticks of the server's own spawning)
Sites: meadow, meadow#2, meadow#3, forest, forest#1, forest#3, mountain_meadow; `realplayer_vanilla.json` vs `realplayer_heaven.json`.
| | vanilla | heaven |
|---|---|---|
| spawned total over 7 sites | 21 / 77 / 315 / 41 / 60 / 225 / 31 | 30 / 61 / 335 / 26 / 78 / 258 / 33 |
| spawned on Habitat cells | 1 / 11 / 27 / 4 / 1 / 9 / 0 | 0 / 4 / 24 / 3 / 1 / 15 / 2 |
Both worlds spawn the same mob families (vanilla hostiles plus the sky mobs cloud_slime, crystal_bug, skyguard_archer, sky_archer, cloud_wraith, pale_enderman ...) and the Habitat-restricted spawns occur on heaven islands as on vanilla ones. The numbers are random (300 attempts, different RNG state), so this is a **qualitative equivalence check, not a statistical proof** that the per-chunk spawn rate is unchanged. Not run: long-duration natural spawning with a moving player, mob cap saturation.

## 3. Existing-world compatibility (real chunk saves, restart between stages, 25 sites)
* A: vanilla west half (chunks with x <= box middle) saved -> heaven packs enabled -> restart -> rest generated: 25/25 sites finish with vanilla template family only (0 mixed); 19/25 block fingerprints equal the vanilla control, the other 6 differ by 15–190 blocks (< 0.4 %).
* B: heaven west half saved -> packs removed (rollback) -> restart -> rest generated: 25/25 heaven only; 23/25 equal; 2 differ by 0 / 10 blocks.
* Noise baseline: two independent vanilla controls differ from each other in 7/25 sites by 2–177 blocks (random ticks: grass / leaves / vines). The A/B differences are the same size, so no seam is visible; a real seam measured in P3.8 (overwriting templates in place, option A) is 36 000–62 000 blocks.
* Reason: pieces persist their template location; blocks are read at chunk generation, so a saved start keeps its old family.
* Layers: **already generated chunks** never change; **saved StructureStarts** keep their pieces' template locations (vanilla stays vanilla, heaven stays heaven, also after rollback); **new chunks / new starts** follow the pools active at that moment.
* Not verified: a real legacy player world with additional mods/datapacks that also edit sky-island pools; worlds generated by older mod versions; multiplayer chunk-load races.

## 4. File integrity and rollback (`rollback_check.sh`, on a copy)
baseline 58 files -> copy 0 deviations -> `apply`: exactly 6 files change -> `revert`: 0 deviations (byte-identical); repository production pools == baseline. JUnit: live pools stay vanilla; the gametest data packs are byte-identical to `heaven_worldgen_pending/` (`HeavenP310ResourceTest`).
Rollback scope: it restores FILES (and therefore all future generation). It does NOT convert chunks / StructureStarts that were already generated with heaven pools; those stay heaven. Test worlds: delete the world / pack. Existing worlds: no world-data rollback exists.

## 5. Verdict table
| item | result |
|---|---|
| Habitat root cause + fix, before/after | PASS |
| Watchdog / tick-stall risk | PASS (legacy 64.5 s > 60 s default; now <= 107 ms) |
| Habitat judgement equivalence (160 templates) | PASS |
| Meadow 3 pools: generation / jigsaw / function / save / reload | PASS (11 sites) |
| mountain_meadow follows meadow pools | PASS (5 sites) |
| Forest 3 pools: same checks | PASS (9 sites) |
| NaturalSpawner with a real player | PASS qualitative / statistical equivalence NOT VERIFIED |
| Partial generation / chunk boundaries in saved worlds | PASS (no seam; noise-level differences) |
| File rollback | PASS |
| World-data rollback | NOT POSSIBLE by design (stated) |
| Real-client visual quality of worldgen output | NOT RUN in P3.10 (P3.8 client captures only) |
| Other 4 themes / §4 / F7 / G2 | untouched / deferred |

## 6. Remaining risks
Single seed (42); spawn-rate equivalence unproven statistically; production switch is irreversible for already generated chunks; mountain_meadow changes together with meadow (by design, T2).

## 7. Independent verdicts
* **meadow: READY for production transition, pending owner approval** (11 natural sites, all checks pass; mountain_meadow follows).
* **forest: READY for production transition, pending owner approval**; it can be applied independently of meadow (forest-only pools were not run alone, but forest pools share no pool with meadow; the meadow-only stage left every forest island vanilla).
Approvals needed: (1) run `pending_pools.py apply` for the chosen theme(s) (6 files; forest-only/meadow-only application needs the script to take a theme argument, not done yet); (2) accept that mountain_meadow follows meadow; (3) accept that already generated chunks and saved starts stay as they are and cannot be converted; (4) keep F7 / G2 deferred; (5) optionally a statistical spawn-rate run.
