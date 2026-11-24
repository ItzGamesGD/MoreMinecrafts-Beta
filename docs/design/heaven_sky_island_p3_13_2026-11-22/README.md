# P3.13 — final verification of the P3.12 open items (production pools unchanged)

Branch `claude/heaven-sky-island-p3-13-final-verification-20261122` (on P3.12). Only test / probe / tool / documentation files changed; the 6 production pool files, NBTs, textures, gameplay, other 4 themes, F7 / G2 untouched. All worlds are isolated test worlds under `build/`.

## 1. Block-entity discrepancy (forest#6, mountain_meadow#9) - RESOLVED: probe aggregation / lazy block-entity initialisation, NO data loss
Owner hypothesis (chunk-loading timing) confirmed by observation.
| question | evidence | result |
|---|---|---|
| does the BLOCK exist? | `bedump`: every vault / trial_spawner / chest / bee nest block exists in all runs | yes (29 / 24 / 8 / 8 blocks; trial_chambers 193 / 125) |
| does the block ENTITY exist? | after `level.getBlockEntity(pos)`: `exist_after_access` = blocks_with_BE in every run (single stage, reload, two-stage + restart, vanilla pools, heaven production pools, vanilla `minecraft:trial_chambers`) | yes, all |
| is its DATA identical? | position + block + NBT hash identical between single-stage generation, reload and two-stage-with-restart (`analyze_bedump.py`, 0 differences in 12 world pairs); fresh NBT is the template data (`nbt_samples_fresh_generation.txt`: vault `loot_table moreminecrafts:chests/sky_islands/vault`, `key_item trial_key`; trial_spawner `normal_config` / `ominous_config`) | yes |
| why did P3.12 count half? | `be_map_entries_over_ticks` (`s2_finish.json`): forest#6 5 -> 8 and mountain_meadow#9 12 -> 24 entries in the chunk maps within 5 server ticks (0 / 1 tick: 5 / 12; 5 / 20 / 60 ticks: all) - the P3.12 probe counted chunk-map entries right after generation, before the chunks' pending block-entity NBT was promoted; the same effect appears in vanilla trial chambers (28 vs 38 vaults) | probe timing artefact |
| real player exploration (`HeavenP313Exploration`, singleplayer, session A west edge -> save -> reopen -> east edge -> centre, production pools, `p313_explore_keep.json`) | in every sample the block entities of the loaded chunks were already in the chunk maps (5 / 5, 8 / 8, 24 / 24, never fewer); final full load: all block entities exist, same positions and block ids as the single-stage reference; chests NBT identical; trial spawners / vaults differ only by RUNTIME state of the live world (`server_data.state_updating_resumes_at:1363L`, `spawn_data:{entity:shulker}`), configs identical (verified by NBT samples, and a probe world that only ticks 60 server ticks has identical hashes) | no loss |
Classification: **probe problem (counting before promotion) - not a chunk-data defect, not a world-data defect. No fix needed; no product change.** (The earlier note that the `inMap` flag was 0 everywhere in `bedump` is a bug of that single flag in the dump, which uses a pre-access `containsKey`; the exploration test uses `getBlockEntities().get()` and is consistent with the tick experiment.)

## 2. Client appearance - all 8 sites x day / night / opposite view inspected (`client_screenshots/`, `contact_sheets/`)
Loading-aware capture (`HeavenP313ClientRender`): waits until `LevelRenderer.hasRenderedAllSections()` (`renderedAll=true` for every frame), camera perpendicular to the island's long axis, plus an opposite-side frame.
* meadow, meadow#2, meadow#3, mountain_meadow, mountain_meadow#4, forest, forest#1: heaven-style islands (grass / loam, cherry and white trees, flowers, giant flowers), natural terrain / village below, night frames dark but readable. No missing textures observed in the frames.
* **mountain_meadow#4 (fragmentary look in P3.12): resolved.** The early frame (taken 25 ticks after arrival) and the fully loaded frames are identical; the island is a main island with two separate floating satellite islands (as designed by the satellite pool), nothing is cut off after full loading. It was a distant / small view, not a loading defect.
* **forest#3 (island not in P3.12 frame): cause found = camera / server chunk-tracking range, not a defect.** Client vs server census: client received 0 of the 409 island blocks while the server had 409 (399 heaven blocks) and the island chunks were `tracked=false` for the player (camera ~118 blocks from the island, beyond the player's effective tracking range; raising the view distance did not help). Vanilla trial chambers in the same run had client == server only when the player was close. Moving the camera above the centre (all island chunks within range) gives client == server (409 / 409) and a complete image: main island with 4 satellites, heaven style (`heaven-p313-fixed-*forest_3*`).
Side note for gameplay: a player simply does not receive chunks beyond the tracking range - normal Minecraft behaviour, identical for vanilla structures.
Software renderer only; no GPU judgement. Night frames are dark by nature (no lighting, moon only).

## 3. Verdict per theme
| | production pools | worldgen (P3.12) | block entities | client appearance | verdict |
|---|---|---|---|---|---|
| meadow | applied | PASS | PASS (no loss) | PASS (3 sites x 3 frames inspected) | PASS |
| mountain_meadow | follows meadow | PASS | PASS | PASS (2 sites; #4 early vs loaded identical) | PASS |
| forest | applied | PASS | PASS | PASS (3 sites; forest#3 fixed camera) | PASS |
Classification of the P3.12 open items: block-entity "halves" = **probe problem** (counted before chunk promotion); forest#3 missing island = **test-camera / chunk-tracking range**, no world defect; mountain_meadow#4 "fragment" = **view distance / design (satellites)**, no defect. **Real world-data defects found: none.**

## 4. Remaining limits (not defects)
Single seed (42); spawn comparison qualitative (P3.10-P3.12); night frames only coarse; software renderer; other mods / datapacks editing sky-island pools untested; the transition itself stays irreversible for already generated chunks (rollback = pool files only).

## 5. Regression
`rm -rf build/run/gameTest/world && ./gradlew --no-daemon test build` on the final HEAD (6 GB game-test heap): **GameTest 749/749, JUnit green, BUILD SUCCESSFUL (10 min 11 s)**. The 6 production pool files are unchanged from P3.12 (`git diff` of `src/main` against P3.12 is empty).

## 6. Completion
All three P3.12 open items are explained by observation (probe timing artefact, test camera / chunk-tracking range, satellite design at a distance); no real world-data defect, no product change needed. P3.12 transition can be declared complete (see the verdict table in section 3).
