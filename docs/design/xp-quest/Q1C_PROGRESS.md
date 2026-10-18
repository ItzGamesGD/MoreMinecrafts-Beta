# Q1-C implementation progress

Parent: corrected Q1-B `fd4aa831139054f6c736edf58fa6fe5f8cc77814`.

## C1 structure loot

Implemented source evidence at the two Minecraft 26.3 generation calls: `RandomizableContainer.unpackLootTable(Player)` and `ContainerEntity.unpackChestVehicleLootTable(Player)`. The hooks retain vanilla `GENERATE_LOOT` behavior, accept only a real server player and an existing `chests/*` loot table, and persist player-specific evidence keyed by block position or container-entity UUID. The event classifier recognises the approved mineshaft, dungeon, temple, shipwreck, underwater ruin, buried treasure, fortress, ancient city, bastion and end-city loot tables. Per-table and per-container evidence are stored separately.

Ten GameTests cover block and chest-minecart generation, actual opener versus another player, reopening, null-player generation, invalid tables, rejection of a valid mod table that imitates a vanilla dungeon path, and each registered structure-loot condition's positive and negative threshold. A separate filtered two-process clean restart test verifies that both a block container and a chest minecart, their consumed loot tables, player-specific evidence, and three completed quests survive a real save/reload; another player receives no credit on reopening either. Nine C1 loot quests are registered: `xpq.exp.first_structure_loot`, `xpq.exp.mineshaft_loot`, `xpq.exp.dungeon_loot`, `xpq.exp.temples`, `xpq.exp.ocean_relics`, `xpq.dim.fortress_loot`, `xpq.dim.ancient_city_loot`, `xpq.dim.bastion_all`, and `xpq.dim.end_city_loot_10`. The full Java 25 gate passed with 336 unit tests and 227 GameTests. Catalog entries and rewards are unchanged. This does not exercise a JVM kill.

## C2 vault source (phase 1, complete)

The Minecraft 26.3 `VaultBlockEntity.Server.tryInsertKey` call to `VaultServerData.addToRewardedPlayers` records successful reward evidence for the actual key user, keyed by dimension and vault position, in separate normal/ominous sets (capped at the target of 5). The hook runs only after vanilla accepted the key, had a non-empty reward and was not already rewarding that player; wrong keys, inactive vaults, empty rewards and repeat use never reach it. Inventory possession of vault rewards is not used.

Ten GameTests (`QuestVaultRewardGameTests`) cover: five distinct normal and five distinct ominous vaults (4 is insufficient, 5 completes only the matching quest), repeated use of one vault, normal+ominous pooling (4+4 completes neither), wrong key type, invalid key, inactive vault, empty reward table, bystander and two players rewarded by the same vault with separate evidence, player save/load, and once-only completion with claim retries (exactly +500 raw XP, later vault events and retries grant nothing). `QuestVaultRestartGameTests` covers player reload, a real vault chunk save, Minecraft unload and reload (vanilla's persisted rewarded-player list still refuses the same player), and a filtered two-process clean restart (write JVM, then read JVM, wired into the K5 Production Restart workflow). This is a clean restart, not a JVM kill.

Registered after those tests passed: `xpq.dim.trial_vaults_5` and `xpq.dim.ominous_vaults_5`.

## Remaining C2-C6

Unique structure identities, combat attribution, dragon rematch, ominous raids and monument participants are not implemented or registered on this branch. Monument VERIFY quests remain unregistered. Q1-C is not ready for final integration review.
