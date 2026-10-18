# Q1-C implementation progress

Parent: corrected Q1-B `fd4aa831139054f6c736edf58fa6fe5f8cc77814`.

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

## Remaining C2-C6

Unique structure identities, combat attribution, dragon rematch, ominous raids and monument participants are not implemented or registered on this branch. Monument VERIFY quests remain unregistered. Q1-C is not ready for final integration review.
