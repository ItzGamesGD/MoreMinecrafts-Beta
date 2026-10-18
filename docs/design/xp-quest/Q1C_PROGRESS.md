# Q1-C implementation progress

Parent: corrected Q1-B `fd4aa831139054f6c736edf58fa6fe5f8cc77814` (PR #120). Verified checkpoint resumed: `8de8a1864ab92c3fc6a8c228d5c73ea1065ecb3f` (C1 and the partial C2 vault hook, preserved unchanged).

| Phase | Subsystem | Commit |
|---|---|---|
| 1 | C2 vault acceptance, 2 quests registered | `7531fa7` |
| 2 | C2 unique structures, 4 quests registered | `dff2721` |
| 3 | C3 combat and kill attribution, 24 quests registered | `5d3e7e9` |
| 4 | C4 Ender Dragon rematch, 1 quest registered | `7184c22` |
| 5 | C5 raid victory, 2 quests registered | `2e125e6` |
| 6 | C6 Ocean Monument participants (monument quests stay VERIFY); T2 monument id repair | `e3e61ff` |
| 7 | Q1-C threshold table and registration invariants | `61154a8` |
| 7 | K5 chunk-persistence harness race (separate commit) | `61a13b0` |

Status words: IMPLEMENTED, TESTED (automated GameTest or unit test passes), REGISTERED (`registered: true` in the catalog), VERIFY (held by authority), BLOCKED.

## C1 structure loot

Implemented source evidence at the two Minecraft 26.3 generation calls: `RandomizableContainer.unpackLootTable(Player)` and `ContainerEntity.unpackChestVehicleLootTable(Player)`. The hooks retain vanilla `GENERATE_LOOT` behavior, accept only a real server player and an existing `chests/*` loot table, and persist player-specific evidence keyed by block position or container-entity UUID. The event classifier recognises the approved mineshaft, dungeon, temple, shipwreck, underwater ruin, buried treasure, fortress, ancient city, bastion and end-city loot tables. Per-table and per-container evidence are stored separately.

Ten GameTests cover block and chest-minecart generation, actual opener versus another player, reopening, null-player generation, invalid tables, rejection of a valid mod table that imitates a vanilla dungeon path, and each registered structure-loot condition's positive and negative threshold. A separate filtered two-process clean restart test verifies that both a block container and a chest minecart, their consumed loot tables, player-specific evidence, and three completed quests survive a real save/reload; another player receives no credit on reopening either. Nine C1 loot quests are registered: `xpq.exp.first_structure_loot`, `xpq.exp.mineshaft_loot`, `xpq.exp.dungeon_loot`, `xpq.exp.temples`, `xpq.exp.ocean_relics`, `xpq.dim.fortress_loot`, `xpq.dim.ancient_city_loot`, `xpq.dim.bastion_all`, and `xpq.dim.end_city_loot_10`. The full Java 25 gate passed with 336 unit tests and 227 GameTests. Catalog entries and rewards are unchanged. This does not exercise a JVM kill.

## C2 vault source (phase 1, complete)

The Minecraft 26.3 `VaultBlockEntity.Server.tryInsertKey` call to `VaultServerData.addToRewardedPlayers` records successful reward evidence for the actual key user, keyed by dimension and vault position, in separate normal/ominous sets (capped at the target of 5). The hook runs only after vanilla accepted the key, had a non-empty reward and was not already rewarding that player; wrong keys, inactive vaults, empty rewards and repeat use never reach it. Inventory possession of vault rewards is not used.

Ten GameTests (`QuestVaultRewardGameTests`) cover: five distinct normal and five distinct ominous vaults (4 is insufficient, 5 completes only the matching quest), repeated use of one vault, normal+ominous pooling (4+4 completes neither), wrong key type, invalid key, inactive vault, empty reward table, bystander and two players rewarded by the same vault with separate evidence, player save/load, and once-only completion with claim retries (exactly +500 raw XP, later vault events and retries grant nothing). `QuestVaultRestartGameTests` covers player reload, a real vault chunk save, Minecraft unload and reload (vanilla's persisted rewarded-player list still refuses the same player), and a filtered two-process clean restart (write JVM, then read JVM, wired into the K5 Production Restart workflow). This is a clean restart, not a JVM kill.

Registered after those tests passed: `xpq.dim.trial_vaults_5` and `xpq.dim.ominous_vaults_5`.

## C2 unique structures (phase 2)

Authority routing: the XP Quest handoff assigns `xpq.dim.end_cities_3` to Q1-C. `xpq.exp.villages_5`, `xpq.exp.structures_15` and `xpq.exp.grand_expedition` are the remaining structure-entry quests in catalog A and are not assigned to any other batch. No other catalog quest is defined by structure entry (`dim.fortress_loot`, `dim.bastion_all`, `dim.ancient_city_loot`, `dim.end_city_loot_10` and the temple/ocean quests are loot-table quests completed in C1).

**Finding (CODE VERIFIED, 26.3):** the existing Knowledge `structures_visited` keys come from `StructureManager.getAllStructuresAt(pos)`, which returns the chunk's structure *references*. `ChunkGenerator.createReferences` adds a reference whenever a structure's bounding box intersects the chunk's X/Z footprint, with no Y check and no piece check. The key is therefore recorded for any player in a chunk the structure's box touches (for example below it, or beside it), which is proximity and not entry. Those keys are not used as quest evidence. Entry is detected with `getStructureWithPieceAt` (inside a generated piece), which is what vanilla location predicates use.

Detector: `QuestStructureEntry` runs every 10 server ticks for each living, non-spectator player. Each structure type is stored once (`set.structure_types_entered`); end cities are additionally keyed `dimension:startChunk` (`set.end_city_starts`, capped at the target of 3).

| Quest | Detection source | Identity / dedup | Attribution | Historical evidence | Persistence | Tests | Decision |
|---|---|---|---|---|---|---|---|
| `xpq.dim.end_cities_3` | piece-level entry into `minecraft:end_city` | dimension + structure start chunk; re-entry and other pieces of the same city add nothing | player standing in the piece | none (approved tracking `I`) | player attachment | other structure rejected, repeats rejected, three distinct complete, dimension identity, teleport-style jump, spectator rejected, real server tick, save/load, two-process restart | REGISTERED |
| `xpq.exp.villages_5` | piece-level entry into the five `minecraft:village_*` types | structure type id | same | see note | same | gap inside bounding box, altitude, same-chunk-outside rejected; outpost rejected; five types complete | REGISTERED (forward-only) |
| `xpq.exp.structures_15` | piece-level entry into any structure | structure type id (15 distinct) | same | see note | same | 14 insufficient, 15th completes | REGISTERED (forward-only) |
| `xpq.exp.grand_expedition` | piece-level entry into stronghold, ancient_city, monument, mansion, trial_chambers | structure type id | same | see note | same | four landmarks insufficient; five complete; ids exist in the registry (the monument id is `minecraft:monument`) | REGISTERED (forward-only) |

Note on historical evidence for the three Knowledge-tracker quests: catalog A tags them `R` (full retroactivity from Knowledge keys). Because those keys are chunk-reference proximity (above), importing them would award entry that never happened, so retroactivity is **not** applied and progress starts from the first piece-level entry after this change. This follows owner policy 3 (full recognition only from reliable persisted evidence, no invented history). The catalog A `R` label itself is unchanged; if the owner wants a one-time import of the old keys, that is a separate decision. Overlapping structures are each recorded (both pieces must contain the position). Entry in the Nether/End dimensions works through the same per-level structure manager.

Tests: `QuestStructureEntryGameTests` (9), `QuestStructureRestartGameTests` (clean two-process restart, wired into the K5 Production Restart workflow). The structure fixture registers real `StructureStart`/chunk-reference records with genuine end-city template pieces under the structure id being tested, so the unmodified vanilla `StructureManager` queries run.

## C3 combat and kill attribution (phase 3)

Two evidence sources, chosen by the catalog's own tracking tag:

- **Vanilla `ENTITY_KILLED` statistic (tag `S`, retroactive).** CODE VERIFIED in 26.3: `LivingEntity.die` calls `source.getEntity().killedEntity(...)`, and `Player.killedEntity` awards the statistic. The statistic therefore goes to the *final blow's causing entity*: a player's melee, an arrow they own, TNT they lit. A tamed wolf, fire after the hit, lava and falls do not award it, even though vanilla still gives the kill credit (loot, advancements) to the last player who hurt the victim for 100 ticks. Nothing is double counted because `die` runs once per entity (a second `die` call and repeated callbacks are tested). Existing statistics count at the first reconciliation (policy 3).
- **`ServerLivingEntityEvents.AFTER_DEATH` + `getKillCredit()` (tag `I`, install-forward).** `QuestCombatKills` credits the vanilla kill-credit `ServerPlayer` once per victim UUID (bounded recent-set guard). Weapon, mace and long-shot counters additionally require that same player to be the damage source's causing *and* direct entity (melee) or the owner of the arrow; bandit counters use plain kill credit.

New tracker kinds: `counter_each` (number of listed counters >= 1), `counter_gated` (sum of counters, capped at target-1 until every listed counter is >= 1) and `stat_killed_each` (number of listed entity types with a credited kill). New item tags `#moreminecrafts:daggers` and `#moreminecrafts:greatswords` (seven registry ids each, as specified in the handoff).

| Quest | Source | Registration |
|---|---|---|
| `xpq.cmb.hostile_100/500/2000/5000` | `ENTITY_KILLED`, every `MobCategory.MONSTER` type | REGISTERED |
| `xpq.cmb.vanilla_species_20` | distinct `minecraft:` Monster types | REGISTERED |
| `xpq.cmb.bestiary_5/10` | distinct `moreminecrafts:` Monster types (37 registered, audit test) | REGISTERED |
| `xpq.cmb.breeze_10`, `guardians_30`, `lesser_wither_10`, `ravagers_5`, `brutes_3` | exact entity type | REGISTERED |
| `xpq.boss.elder_guardian`, `warden`, `wither`, `warden_3` | exact entity type (live wither and warden kills tested) | REGISTERED |
| `xpq.boss.twin_bosses` | `stat_killed_each` {ender_dragon, wither}; repeats of one boss never substitute | REGISTERED |
| `xpq.cmb.mm_arms`, `dagger_100`, `greatsword_100`, `netherite_blades` | final blow with the tagged / exact weapon on a Monster | REGISTERED |
| `xpq.cmb.bandits_20` | kill credit; 20 total and each of three species >= 1 | REGISTERED |
| `xpq.cmb.long_shot_25` | arrow owned by the player, weapon is a bow or crossbow, >= 30 blocks (3-D) | REGISTERED |
| `xpq.cmb.mace_smash_10` | `mace_smash` damage type by the player on a Monster | REGISTERED |
| `xpq.cmb.cave_dwellers` | VERIFY: the list names `small_cave_spider` and `swamp_cave_spider`, which are not registered ids (the registry has `small_spider`, `swamp_spider`), and `cave_bat` is Ambient, not Monster | unregistered |
| `xpq.cmb.bestiary_25` | VERIFY: 37 Monster-category types exist, but natural obtainability of 25 is the open question | unregistered |
| `xpq.cmb.habitats_5` | VERIFY: the habitat-to-mob mapping is not defined in authority | unregistered |
| `xpq.cmb.wither_skulls_3` | NOW but an item-acquisition quest, not kill attribution; no hook in this batch | unregistered |
| `xpq.cmb.raids_3` | handled with the raid hook (phase 5) | see phase 5 |

Tests (`QuestCombatGameTests`, 17): melee credits only the attacker; projectile owner credited and a mob-owned arrow credits nobody; owned TNT credited, lava and fall credit nobody; last player attacker wins; repeated `die` and repeated `AFTER_DEATH` count once; exact entity types (cow, cave zombie vs zombie, elder guardian vs guardian, wither skeleton vs lesser, piglin vs brute, pillager vs bandit); live kills for breeze, guardian, ravager, brute, lesser wither skeleton, wither and warden; exact count and species thresholds (99/100, 19/20, 4/5 custom, 1,999/2,000/5,000); retroactive statistics; weapon final-blow rules including fire-after-hit and a shared kill; bandit gating; long-shot distance, weapon and owner; mace smash; bestiary registry audit. `QuestCombatRestartGameTests`: disconnect/rejoin in one process and a clean two-process restart (statistics file + player record), wired into the K5 Production Restart workflow.

Out-of-scope finding (not repaired): `KnowledgeEventCollectors.projectileKill` calls `arrow.getWeaponItem().getItem()` although `AbstractArrow.getWeaponItem()` is `@Nullable` (dispenser-fired arrows have no weapon). A GameTest probe confirmed that an arrow without a weapon killing any `Monster` throws a `NullPointerException` out of `LivingEntity.die` through the AFTER_DEATH event. This predates Q1-C and is left unchanged per the task boundary; Q1-C's own hook is null-safe.

## C4 Ender Dragon rematch (phase 4)

Mapped 26.3 lifecycle (CODE VERIFIED): the dragon's health reaching zero runs `LivingEntity.die` (vanilla kill credit, AFTER_DEATH), then `EnderDragon.tickDeath` runs a 200-tick death animation, and only at `dragonDeathTime >= 200` does it call `EnderDragonFight.setDragonKilled(dragon)`, which sets `hasPreviouslyKilledDragon = true` and `dragonKilled = true`. `kill()` (for example `/kill`) also calls `setDragonKilled`. Vanilla kill credit expires after 100 ticks, before the animation ends, so credit is recorded when the dragon dies and consumed when the fight completes.

`QuestDragonFightMixin` injects at the HEAD of `setDragonKilled` and reports (own dragon and fight not already completed, `hasPreviouslyKilledDragon()` before mutation) to `QuestDragonRematch`. Credit is granted only when the previous-kill flag was already true, the dying dragon is the fight's own (`dragonUUID`), the fight had not already completed, and a credited player was recorded. The credited player is the dragon's vanilla kill-credit player (the last player to damage it), as in the handoff. An offline credited player gets a pending credit in world `SavedData` (`QuestDragonRematchData`, also storing the in-flight credit across a save) applied once at login. Quest: `xpq.boss.dragon_rematch`, a `counter` on `dragon_rematch_kills`, target 1.

Tests (`QuestDragonRematchGameTests`, 5, plus `QuestDragonRematchDataTest`, 3 unit): first defeat is not a rematch (and still records the vanilla statistic used by `twin_bosses`); the legitimately respawned dragon (respawn stage END creates it through the fight's own factory) credits only its killer, not the first-kill player or a present bystander; repeated `setDragonKilled` adds nothing; a dragon with no attributable player credits nobody and raises nothing; the last damager, not the player who summoned or first hurt the dragon, gets the credit; a dragon that is not the fight's own and `kill()` never credit; offline killer receives the credit once at login; a clean two-process restart (fight flag, pending credit, one-time application) wired into the K5 Production Restart workflow. REGISTERED: `xpq.boss.dragon_rematch`.

## C5 raid victory (phase 5)

Mapped 26.3 lifecycle (CODE VERIFIED): `Raider.die` adds the player who delivered the final blow to the raid's `heroesOfTheVillage`; `Raid.tick` turns the status ONGOING to VICTORY once all waves are spawned, no raiders remain and `postRaidTicks` reaches 40, then for every hero that is present in the level and not a spectator it adds Hero of the Village, awards `Stats.RAID_WIN` and fires the `RAID_WIN` trigger. LOSS, STOPPED (cancelled, abandoned, peaceful, timed out) and incomplete raids never reach that block, and a heroless victory simply has no heroes. `QuestRaidVictoryMixin` wraps that single `awardStat(RAID_WIN)` call, so credit is exactly the set of heroes vanilla rewards, once per victory, with the raid's omen level read at victory (`Raid.getRaidOmenLevel()`, as the handoff specifies). `xpq.boss.omen_raid` needs omen level >= 3 (counter `omen_raid_wins`, target 1); `xpq.cmb.raids_3` counts every victory (counter `raid_victories`, target 3). Both are install-forward.

Tests (`QuestRaidGameTests`, 6 including the restart test): levels 3, 4 and 5 qualify with two heroes credited and a bystander not; levels 0 to 2 count only toward raids_3 (three victories complete it, two do not); a lost raid (village gone with a raider alive, stays lost after the raider dies), a stopped raid, an unfinished raid and a heroless victory credit nobody; a spectating hero, an offline hero id and 700 further ticks after victory add nothing and player save/load keeps credit; a real raider killed through `Raider.die` makes only its killer a hero; clean two-process restart (wired into the K5 Production Restart workflow) with exactly-once claim. REGISTERED: `xpq.boss.omen_raid`, `xpq.cmb.raids_3`.

## C6 Ocean Monument participants (phase 6)

**P1 repair (existing code, required dependency).** `T2CoreAcquisition` identified the monument by structure id `minecraft:ocean_monument`, which is not a 26.3 structure id (the registry id is `minecraft:monument`; a GameTest asserts `ocean_monument` is absent). A real-start probe showed the existing completion path never claimed a monument (`claimed=false`, no Heart of Currents) before the id was corrected, and claimed it afterwards. Participant credit hangs off that path, so the id was corrected and nothing else about the reward changed (same piece-level test, same deferral, same claim set, same single Heart of Currents; the replay test confirms exactly one Heart per monument).

Design (handoff 3.7). `MonumentRewardData` gains persisted participant evidence (monument start chunk, player UUID, last combat game tick, Elder Guardian damage flag; bounded to 64 players per monument and 256 monuments) and pending offline credits (bounded to 1,024), both optional codec fields, so old saves load unchanged. `QuestMonumentParticipation` records evidence only while the monument is uncompleted:
- (a) a player damages an Elder Guardian whose position is inside that monument's bounds (`DamageSource.getEntity()` resolves arrows, tridents and owned entities to the player; a mob-owned arrow is rejected);
- (b) the player is inside the bounds and deals damage to, or receives damage from, a Guardian or Elder Guardian. If the guardian they hit is inside a different monument, it is not combat for the monument they stand in.
Presence alone, a bystander and combat outside the bounds never qualify. The killing blow is reported from `AFTER_DEATH` (which runs before the Fabric `AFTER_DAMAGE` event and before completion), non-lethal damage from `AFTER_DAMAGE`. When the existing completion path claims the monument, immediately or on the deferred chunk-load retry, every player with Elder Guardian damage plus every player whose last combat is within 12,000 game ticks receives one credit (`set.monuments_cleared`, a distinct set of `dimension:startChunk`, capped at the three needed). Offline participants get a pending credit applied at login; repeated completion events and logins add nothing.

Tests: `QuestMonumentGameTests` (7): melee, owned-arrow-from-outside and guardian-beam victim all credited, bystander/far player/combat outside bounds rejected, exactly one Heart of Currents; mob arrow and wrong-monument evidence rejected; 12,000/12,001-tick window and the Elder Guardian special case (pure data and live); offline credit at login once; same-monument replay and duplicate completion rejected and three distinct monuments counted as three; deferred completion after a real chunk unload (evidence kept, completes when the chunks reload). `QuestMonumentRestartGameTests`: participant evidence and a pending offline credit written by one JVM, the surviving elder restored, the monument completed by the second JVM (stored participants, the finisher and the returning offline player each credited once, replay adds nothing), wired into the K5 Production Restart workflow. Unit tests cover the new `MonumentRewardData` fields, bounds, and loading of saves that lack them.

| Quest | Status |
|---|---|
| `xpq.boss.monument_clear` | VERIFY. CODE VERIFIED, TEST VERIFIED, REGISTRATION BLOCKED pending separate owner authorization. Tracker preconfigured as `distinct_set` `set.monuments_cleared`, target 1; still `registered: false`. |
| `xpq.boss.monuments_3` | VERIFY. CODE VERIFIED, TEST VERIFIED (three-distinct-monuments test), REGISTRATION BLOCKED pending separate owner authorization. Tracker preconfigured the same way, target 3; still `registered: false`. |

## Per-quest registration gates

Newly registered by this task (33): `xpq.dim.trial_vaults_5`, `xpq.dim.ominous_vaults_5`, `xpq.dim.end_cities_3`, `xpq.exp.villages_5`, `xpq.exp.structures_15`, `xpq.exp.grand_expedition`, `xpq.cmb.hostile_100`, `hostile_500`, `hostile_2000`, `hostile_5000`, `vanilla_species_20`, `bestiary_5`, `bestiary_10`, `breeze_10`, `guardians_30`, `lesser_wither_10`, `ravagers_5`, `brutes_3`, `mm_arms`, `dagger_100`, `greatsword_100`, `netherite_blades`, `bandits_20`, `long_shot_25`, `mace_smash_10`, `xpq.boss.elder_guardian`, `warden`, `wither`, `warden_3`, `twin_bosses`, `dragon_rematch`, `omen_raid`, and `xpq.cmb.raids_3`. Together with the nine C1 loot quests registered earlier on this branch, Q1-C registers 42 quests; 72 quests are registered in total (30 Q1-B + 42 Q1-C), 70,900 raw XP.

Every registered quest has: its source-specific positive, negative and attribution tests (sections above), the `QuestC3ThresholdGameTests` target-1/target table or a Q1-B/C1 `threshold_*` test, and a persistence test (player save/load, rejoin, and a clean two-process restart where evidence lives in world or statistics files).

| Quest | IMPLEMENTED | TESTED | REGISTERED | Notes |
|---|---|---|---|---|
| `xpq.boss.monument_clear` | yes | yes | no | VERIFY; REGISTRATION BLOCKED pending separate owner authorization |
| `xpq.boss.monuments_3` | yes | yes | no | VERIFY; REGISTRATION BLOCKED pending separate owner authorization |
| `xpq.cmb.cave_dwellers` | no | no | no | VERIFY; list names unregistered ids (`small_cave_spider`, `swamp_cave_spider`) and an Ambient mob (`cave_bat`); needs an authority correction |
| `xpq.cmb.bestiary_25` | tracker exists | registry audit only | no | VERIFY; 37 Monster-category custom types exist, obtainability of 25 not established |
| `xpq.cmb.habitats_5` | no | no | no | VERIFY; no authority mapping from habitat to mob types |
| `xpq.cmb.wither_skulls_3` | no | no | no | NOW but an item-acquisition event, not kill attribution; no hook written in this batch |
| other VERIFY / FUTURE quests | no | no | no | untouched |

Invariants checked against the base catalog: identical order and ids, `tab`, `tier`, `status`, `rewardRawXp` and `target` for all 200 entries; 200 entries; NOW 160 / VERIFY 20 / FUTURE 20; active NOW+VERIFY 180,000; FUTURE 29,100; total 209,100 (unit-asserted). Only `registered` flags and trackers of the quests above changed; the two monument quests have a preconfigured tracker but remain unregistered.

## Validation record

Java 25.0.4 (Temurin) `./gradlew clean test build compileGametestJava runGameTest --no-daemon` on the head before this document: BUILD SUCCESSFUL, 342 unit tests and 287 GameTests passed (checkpoint `8de8a18` was 336 and 229). The focused `quest_*` GameTests (72) passed on six consecutive fresh worlds. Interrupted: owner stopped the task after the local restart-chain run (see F-6); the final GitHub check on the last pushed head was not awaited. Local server startup is exercised by the GameTest server; the official server and client smoke results are the GitHub checks below. Local client smoke is NOT RUN (no graphics support in this container).

Durability, kept distinct:
- *In-process save/reload*: player save and load (vault, structure, raid, monument), disconnect and rejoin (combat, dragon, monument), a real chunk save with Minecraft unload and reload (vault chunk, monument deferred completion).
- *Clean two-process JVM restart* (write JVM exits, read JVM starts on the same world): vault, structure entry, combat statistics and counters, dragon pending credit and fight flag, raid credit, monument participant evidence and pending offline credit, in addition to the earlier C1 and ore tests. All are in the K5 Production Restart workflow and were also run locally in workflow order.
- *Forced JVM termination and recovery*: NOT RUN. A clean restart is not a `kill -9` test.
- *Actual multiplayer / live acceptance*: LIVE VALIDATION PENDING.

## Findings

| ID | Sev | Finding | Disposition |
|---|---|---|---|
| F-1 | P1 | `T2CoreAcquisition` matched structure id `minecraft:ocean_monument`; the 26.3 id is `minecraft:monument`, so the existing Ocean Monument completion and Heart of Currents reward never ran for a real monument | Repaired in `e3e61ff` (required dependency of C6); real-start probe showed `claimed=false` before and `claimed=true` after |
| F-2 | P1 | Knowledge `structures_visited` keys come from chunk structure *references* (X/Z box-vs-chunk, no Y, no piece test), so they record proximity, not entry | Not used; piece-level `QuestStructureEntry` replaces them for the four structure quests. Retroactive import of old keys is therefore not applied (see C2 section) |
| F-3 | P1, out of scope | `KnowledgeEventCollectors.projectileKill` dereferences `AbstractArrow.getWeaponItem()` which is `@Nullable`; an arrow with no weapon item (dispenser-fired) that kills any `Monster` throws a `NullPointerException` out of `LivingEntity.die` | NOT repaired (AGENTS section 5); confirmed with a GameTest probe; Q1-C code is null-safe. Recommend a one-line null guard as a separate task |
| F-4 | P2 | K5 chunk-persistence GameTest waited on a single last-writer-wins unload slot; another marked chunk unloading in the same window hid the awaited unload and the wait hit its 240-tick limit (reported as `tick 241`) | Repaired in `61a13b0`, see below |
| F-6 | P2, open, UNDIAGNOSED | Local run of the whole K5 Production Restart chain in workflow order on one fresh world passed 16 of 18 steps; the two monument steps (write and read) each reported `1 required tests failed` and printed no marker. The same monument pair passed when run alone on a fresh world, and the GitHub K5 Production Restart check was green at `61154a8` (which already contained the monument steps) | Work was stopped by the owner before investigation. Next step: rerun the chain locally, read `rc_step.log` for the monument write step, and check for state shared with the earlier steps |
| F-5 | P2, open | `k5RegisteredNonVanillaPlacedTreeIsRejectedDuringNaturalDecoration` fails intermittently ("modded-feature target begins unmarked") when the K5 GameTest class is run on its own (4 of 20 runs after F-4, 2 of 9 before); it never failed in 14 full-suite runs | Unchanged, unrelated to Q1-C; retained as an open P2 test-isolation defect |

K5 disposition: the 240-tick timeout was not simply raised. Root cause (test-harness defect, not production behavior): `K5NaturalTreeProbe` kept only the most recent marked-chunk unload, so two unloads between polls lost the first. Reproduction: with the K5 GameTest group on a clean world and the old harness, 3 of 9 runs failed with `removing the FORCED ticket did not reach ServerLevel.unload on tick 241` (the same message and tick as the CI failure); with the fix 0 of 20 failed. A new deterministic regression test (`k5UnloadObservationSurvivesUnrelatedMarkedChunkUnloads`) fails on the old harness and passes on the new one. Standalone the original test passed 15 of 15 and the full suite passed 12 of 12 locally, so the failure needs concurrent K5 unloads; the Q1-C changes do not touch that code path.

## LIVE VALIDATION PENDING

Real-world feel and balance of every registered Q1-C quest; naturally generated vaults, end cities, villages and monuments (tests use real `StructureStart` records with end-city template pieces under the tested structure ids); a live Ender Dragon rematch with the full crystal respawn sequence; a live omen raid; real monument fights with several players; multiplayer attribution on a dedicated server; JVM-kill durability; official client smoke beyond the GitHub check.

## Remaining work and recommendation

Not started, by instruction: Q1-D and Q1-E. Open: F-3 and F-5 above, the VERIFY quests in the table, and `xpq.cmb.wither_skulls_3`. Integration review of PR #121 should wait for the owner's decision on retroactivity of the three Knowledge-tracker structure quests (C2 section) and on promoting the two monument quests.


## Owner-approved cave-dwellers roster and next ecology audit (2026-10-18)

- **Decision:** `xpq.cmb.cave_dwellers` requires one player-credited kill of each of exactly six registered types: `moreminecrafts:cave_zombie`, `cave_skeleton`, `cave_creeper`, `small_spider`, `swamp_spider`, and `cave_bat`. `cave_bat` is `MobCategory.AMBIENT` and is explicitly included as a **quest-specific exception**; do not apply a generic Monster-category filter to this quest.
- The two incorrect spider IDs were corrected in the authority catalog and implementation handoff. The six entity declarations and corresponding natural-spawn registration calls were verified in `CustomMobCatalog`. The `swamp_spider` uses the swamp habitat spawn tag, so the quest is **not** restricted to caves despite its title.
- **Implementation pending:** `xpq.cmb.cave_dwellers` remains `status=VERIFY`, `registered=false` until GameTests demonstrate six unique species, ambient bat inclusion, unrelated type rejection, other-player attribution, duplicate kills, persistent completion, and safe stat retroactivity. No XP or status change authorized.
- **Next audit (not an owner decision):** `CustomMobCatalog.initialize()` explicitly registers natural spawn candidates for all 37 Monster-category types, but declaration and `addSpawn` calls alone do **not** prove that 25 species can actually spawn under reachable world conditions. Verify biome tag JSONs, spawn predicate/structure restrictions and live worldgen before `bestiary_25` registration.
- **Five habitat candidate mappings for review:** desert `desert_spider`/`sand_slime`, swamp `swamp_zombie`/`swamp_spider`/`mud_slime`, snowy `frozen_zombie`/`ice_spider`, fungal `fungal_zombie`, wild slime `wild_slime`. These are candidates derived from `CustomMobCatalog`'s `add(..., HABITAT, ...)` calls, **not approved authoritative quest mappings**. Check biome tag reachability and spawn predicates before locking or registering `habitats_5`.
