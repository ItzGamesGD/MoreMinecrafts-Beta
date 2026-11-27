# C-4 Trial Spawner, progression and barrier tests

Spawner config in the approved tower is vanilla `minecraft:trial_chamber/melee/zombie/normal` (+ `.../ominous`); all 16 anchors checked in `full_01.nbt` (static). Production code was not changed.

## A. Real vanilla combat [OBSERVED] — `SkyTowerGameTests` (new)

Harness: Fabric GameTest on the real mod server, template `full_01` placed with the production `SkyTowerData` registration, chunks forced and entity-ready. The stock GameTest in-level mock player reports **CREATIVE** (vanilla spawners ignore creative players, so spawners never left `WAITING_FOR_PLAYERS`); the tests therefore use a plain **survival** `ServerPlayer` with a loopback connection placed through `PlayerList.placeNewPlayer`. Players stand at tower-local (24,11,24), within 14 blocks and line of sight of the four floor-0 spawners. Difficulty NORMAL, `spawn_mobs` and `spawner_blocks_work` true.
Mobs are killed with `hurtServer(playerAttack(player), 1000)` on every living zombie in the tower area (real kills, real vanilla death handling). **No** `recordVanillaCompletion`, no `TRIAL_SPAWNER` COOLDOWN block write, no manual `reconcile` is used for the subject tower; the barrier is removed by the production `SkyTowerBarriers.tick`.

| Test | Rot | Players | Result |
|---|---|---|---|
| `realVanillaTrialCombatCompletesFloorZeroAndOpensBarrier` | 0 | 1 | PASS, floor cleared t=263, 24 zombies killed |
| `realVanillaTrialCombatTwoPlayersRotatedHalfTurn` | 2 | 2 | PASS, t=333, 32 kills; bit set once per spawner |
| `realVanillaTrialCombatSpawnerRemovedWhileActiveStillOpensFloor` | 3 | 1 | PASS, t=253, 18 kills; sp3 really removed while ACTIVE → destroyed mask 8, completion by the other three at vanilla COOLDOWN |

Asserted in each: wall actually installed before completion; completion bit never set before vanilla `COOLDOWN` (checked each tick for every non-cooldown state); `completedMask==15`; normal runs `destroyedMask==0`; each spawner reached COOLDOWN; 20 ticks×2 after clearing all 18 floor-0 cells are air and floor-1 cells still barrier; a second tower (other rotation, registered, no player) keeps mask 0 and its wall; NBT round-trip of the production `SkyTowerData` codec preserves masks/ownership; a player-placed stone in a released cell and a plank above the column survive; a synthetic write of ACTIVE onto a completed spawner leaves mask 15 and does not rebuild the wall.
Observed vanilla order differs per run (e.g. sp1,sp3,sp0,sp2 / sp3,sp0,sp2,sp1 / sp1,sp2,sp3,sp0), so completion-order independence was exercised in practice. Full per-spawner state timelines: `phase_c4_evidence/real_combat_log.txt`.

Debugging note (my test, not the mod): the first two attempts never activated the spawners — first because the stock mock player is CREATIVE, then because adding a connection-less player NPE'd; both fixed in the helper.

## B. Hook-driven / synthetic [SIM] — existing and new
| Requirement | Evidence |
|---|---|
| Four spawners complete in different orders | real runs above (order varies) + `SkyTowerProgressTest` (mask math) [SIM] |
| Two towers independent | real tests (second tower) + existing `committedSpawnerRemovalAndVanillaCooldown…WithoutCrossInstanceProgress` |
| Multiple players, same spawner | real 2-player test |
| Destroyed before activation | existing GameTest: 3 spawners really removed via `destroyBlock`, counted with destroyed bits |
| Destroyed during combat | real test above |
| Destroyed after completion | existing: normal-first cause kept, later destruction no-op |
| Completion and destruction overlap | existing (cooldown then destroy; duplicate destroy). **Same-tick** physical overlap not separately tested **[UNVERIFIED]**; both paths run on the server thread and are idempotent on the mask **[STATIC]** |
| Completed spawner reactivates | synthetic state write only **[SIM]**; natural 30-min cooldown expiry not exercised **[UNVERIFIED]** |
| Reinstalled spawner | existing: mask not reset |
| Player modifies nearby blocks / no unrelated deletion | existing (stone/entity/waterlogged/foreign barrier) + new (stone in released cell, plank above) |

## C. Barrier + persistence on a natural tower [OBSERVED] (`PERSIST_FLOOR_CLEAR=true`, desert seed 7202 r=100, rot 2, site `1008,20,-160,2`)
1. After auto-registration the tower chunks are force-loaded; after 60 ticks floor 0 and floor 1: 18 barrier cells each, 18 owned each.
2. Four real `destroyBlock` removals of floor-0 spawners → mask 15, destroyed 15; 60 ticks later floor 0: 0 barrier / 18 air / 0 owned, floor 1: 18 barrier / 18 owned.
3. Server stopped (`halt(false)`, saved) and restarted on the same world: Site count 1→1, Structure Start rediscovered, mask 15, floor 0 18 air / 0 owned (**not** re-walled after 100 ticks), floor 1 18 barrier / 18 owned.
Files: `phase_c4_evidence/floorclear_save_7202.json`, `floorclear_resume_7202.json`. Also the earlier API-marker restart check (`restart_*_7202.json`, mask 1→1; **marker, not combat**).
Probe finding (probe bug, not mod): with chunks merely loaded and not forced, walls are (correctly) not installed — the barrier tick defers until block chunks and the entity-query neighbours are ready, without logging; my first attempts therefore showed 0 walls. Forcing was initially issued before auto-registration and had no effect; fixed.

## D. Not covered / pending
- Real combat on floors 1–3 (`LIVE COMBAT QA PENDING`), real combat → restart in one run (restart used real *removals*, not combat), natural cooldown reactivation, ominous variants, chunk unload mid-fight within one session, real multiplayer clients. Vault interaction/loot is untouched vanilla; not re-tested.
- Observation: `SkyTowerBarriers` installs a floor wall only while all 18 cell chunks (+entity neighbours) are loaded and no entity/foreign block occupies a cell (documented design); a player or mob inside a cell delays wall installation. Not a defect; relevant to client QA.

## Reproduce
```sh
./gradlew -Dmmskytower.testFilter='moreminecrafts-gametest:sky_tower_game_tests*' -I docs/planning/sky_tower_integration_02/c3-focused.init.gradle runGameTest --no-daemon
# restart fixture
export LEVEL_TYPE=minecraft:flat GENERATOR_SETTINGS='{"biome":"minecraft:desert","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3},{"block":"minecraft:sand","height":1}]}' PERSIST_FLOOR_CLEAR=true ALLOW_PARTIAL=1
tools/sky-islands-probe/run_tower_composite_probe.sh 7202 100      # save
tools/sky-islands-probe/run_tower_composite_probe.sh 7202 100 1    # resume (same world)
```
