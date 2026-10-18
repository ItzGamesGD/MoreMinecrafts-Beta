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

## Remaining C2-C6

Unique structure identities, combat attribution, dragon rematch, ominous raids and monument participants are not implemented or registered on this branch. Monument VERIFY quests remain unregistered. Q1-C is not ready for final integration review.
