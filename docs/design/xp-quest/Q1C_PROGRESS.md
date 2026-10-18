# Q1-C implementation progress

Parent: corrected Q1-B `fd4aa831139054f6c736edf58fa6fe5f8cc77814`.

## C1 structure loot

Implemented source evidence at the two Minecraft 26.3 generation calls: `RandomizableContainer.unpackLootTable(Player)` and `ContainerEntity.unpackChestVehicleLootTable(Player)`. The hooks retain vanilla `GENERATE_LOOT` behavior, accept only a real server player and an existing `chests/*` loot table, and persist player-specific evidence keyed by block position or container-entity UUID. The event classifier recognises the approved mineshaft, dungeon, temple, shipwreck, underwater ruin, buried treasure, fortress, ancient city, bastion and end-city loot tables. Per-table and per-container evidence are stored separately.

Ten GameTests cover block and chest-minecart generation, actual opener versus another player, reopening, null-player generation, invalid tables, rejection of a valid mod table that imitates a vanilla dungeon path, and each registered structure-loot condition's positive and negative threshold. A separate filtered two-process clean restart test verifies that both a block container and a chest minecart, their consumed loot tables, player-specific evidence, and three completed quests survive a real save/reload; another player receives no credit on reopening either. Nine C1 loot quests are registered: `xpq.exp.first_structure_loot`, `xpq.exp.mineshaft_loot`, `xpq.exp.dungeon_loot`, `xpq.exp.temples`, `xpq.exp.ocean_relics`, `xpq.dim.fortress_loot`, `xpq.dim.ancient_city_loot`, `xpq.dim.bastion_all`, and `xpq.dim.end_city_loot_10`. The full Java 25 gate passed with 336 unit tests and 227 GameTests. Catalog entries and rewards are unchanged. This does not exercise a JVM kill.

## C2-C6

Vaults, unique structures, combat attribution, dragon rematch, ominous raids and monument participants are not implemented or registered on this branch. Monument VERIFY quests remain unregistered. No Q1-C feature is ready for final integration review.
