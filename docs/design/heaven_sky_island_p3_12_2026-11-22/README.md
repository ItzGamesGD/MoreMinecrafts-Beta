# P3.12 — production worldgen transition: meadow (+mountain_meadow) first, then forest

Branch `claude/heaven-sky-island-p3-12-production-transition-20261122` (on P3.11). Approvals: authority §24.6 of `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md`.

## 1. What changed in production (src/main)
Exactly 6 files, `src/main/resources/data/moreminecrafts/worldgen/template_pool/sky_islands/{meadow,forest}/{start,satellite,tower_top}.json`: element template locations `sky_islands/<theme>/<n>` -> `sky_islands/heaven/<theme>/<n>`; pool IDs, weights, fallbacks, processors, projection unchanged. Applied with `pending_pools.py apply meadow` (commit 0a4ae57, verified) and later `apply forest`. The other 52 worldgen / tag files are byte-identical to `tools/heaven-p38/worldgen_baseline.sha256`; structure IDs / sets / probabilities / biome tags / spawn rules, the 114 original + 38 heaven NBTs and the P3.6 T1 textures are unchanged; other 4 themes untouched. Backup of the 6 pre-transition files + SHA-256: `backup_pre_p312/`.
Other changed files (tests / tools only): `HeavenP38ResourceTest`, `HeavenP310ResourceTest`, `SkyIslandsResourceTest`, `HeavenP35GameTests` (expectations that encoded the pre-approval state: live pools vanilla / unlinked P3 template mixes vanilla satellites / tower_top names the vanilla tower), `HeavenWorldgenProbe`, `HeavenP312ClientRender`, `fabric.mod.json` (gametest), `tools/heaven-p39/*`, authority §24.6.

## 2. Meadow stage (forest still vanilla) - production jar, no data pack, seed 42, same 25 natural sites
| check | result |
|---|---|
| meadow 11 sites / mountain_meadow 5 sites | heaven 16/16 (shared pool), forest 9/9 vanilla |
| jigsaw left | 0 everywhere |
| loot containers, spawners, island box vs vanilla control | identical |
| fingerprint vs earlier pack-based heaven run | meadow 10/11 equal (1 = tick noise), mountain_meadow 5/5 |
| reload (second server start) | meadow 11/11, forest 9/9, mountain_meadow 5/5 fingerprint equal |
| Habitat | meadow 8442/8665, mountain_meadow 4054/4054, first call <= 37 ms |
| real player, 300 NaturalSpawner attempts / chunk + 600 ticks (7 sites) | same mob families as before (vanilla hostiles + sky mobs), Habitat spawns present on heaven islands; qualitative only |
| old world (old pools, west half saved) finished under the production pools | 25/25 vanilla family: saved StructureStarts keep their template locations |
| production-west then file rollback (old pack) then finish | 25/25 heaven family stays |
Gate passed -> forest applied.

## 3. Full state (meadow + forest)
Same checks: heaven 11/11 meadow, 9/9 forest, 5/5 mountain_meadow; jigsaw 0; loot / spawner / size identical to vanilla; reload equal 11+9+5; vanilla-theme blocks 0 for forest and mountain_meadow, meadow 2 692 = natural terrain (P3.11 trace: 0 template blocks, y 74..78 at the ground); Habitat forest 2158/2158; old-world west-half then production finish 25/25 vanilla; heaven west-half then rollback finish 25/25 heaven; real player (7 sites) as above. Raw data: `worldgen_results/`.

## 4. Real client rendering (`HeavenP312ClientRender`, real client + dedicated server, production jar, 1920x1080 software GL, `client_screenshots/`)
16 screenshots (8 sites x noon / midnight). Inspected by eye: meadow, meadow#3, forest, forest#1, mountain_meadow#4 show the heaven islands (grass / loam, cherry and white trees, flowers) in natural terrain; the midnight meadow shot is dark but readable. NOT satisfied: forest#3 camera missed the island (framing, island not in view, no judgement possible); mountain_meadow#4's far-left satellite looks fragmentary (could be client chunk meshing or the satellite design, not decided); the other screenshots (meadow#2, mountain_meadow, night shots) were not inspected. Software renderer: no GPU / performance conclusion. => appearance sign-off beyond the inspected frames: LIVE VALIDATION PENDING.

## 5. Open finding (not caused by the transition, unresolved)
In two-stage generation (half of an island's chunks generated, server restarted, rest generated) forest#6 and mountain_meadow#9 end with exactly half of their vault / trial_spawner / chest block entities in the probe scan (the missing ones are in the chunks generated in the second stage). Reproduced with the OLD vanilla pools in both stages (`dgv_vfinish.json`) and with vanilla `minecraft:trial_chambers` (`dgt1_single` vs `dgt2_finish`: vault 38 -> 28, 21 -> 8), so it is independent of heaven / pool switching and of this mod's structures. Block states and fingerprints are unaffected. Cause not determined (harness artefact of the probe's forced partial generation + restart, or vanilla behaviour); not investigated further (out of scope). Risk: unknown whether real players could hit it when exploring islands across sessions; not observed in single-stage generation, reload or the real-player run.

## 6. Rollback scope
`pending_pools.py revert meadow|forest|all` (verified on copies, 20/20 checks: exact files, idempotent, refusal on tampered pools, byte-identical to the baseline hashes / `backup_pre_p312`). It restores POOL FILES (future generation) only. Already generated chunks and saved StructureStarts keep the heaven pieces (verified: heaven west half + rollback + finish = 25/25 heaven); worlds generated before the transition keep vanilla pieces (25/25 vanilla). mountain_meadow follows meadow. No world-data conversion exists. Existing user worlds were never touched (all runs in `build/heaven-worldgen/` worlds).

## 7. Regression
`rm -rf build/run/gameTest/world && ./gradlew --no-daemon test build` (6 GB game-test heap): after updating the 4 test expectations above, **GameTest 749/749, JUnit green, BUILD SUCCESSFUL (9 min 9 s)**. Earlier attempts in this task failed only on those expectations (2 JUnit, 1 GameTest), not on product behaviour.

## 8. Remaining risks
Section 5 anomaly; single seed (42); spawn comparison qualitative only; appearance partly inspected (forest#3 / night / far satellite open); transition is irreversible for already generated chunks; mountain_meadow cannot be switched alone; other mods editing sky-island pools untested.
