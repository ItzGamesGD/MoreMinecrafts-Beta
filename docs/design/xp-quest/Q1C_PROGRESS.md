# Q1-C implementation progress

Parent: corrected Q1-B `fd4aa831139054f6c736edf58fa6fe5f8cc77814`.

## C1 structure loot

Implemented source evidence at the two Minecraft 26.3 generation calls: `RandomizableContainer.unpackLootTable(Player)` and `ContainerEntity.unpackChestVehicleLootTable(Player)`. The hooks retain vanilla `GENERATE_LOOT` behavior, accept only a real server player and an existing `chests/*` loot table, and persist player-specific evidence keyed by block position or container-entity UUID. The event classifier recognises the approved mineshaft, dungeon, temple, shipwreck, underwater ruin, buried treasure, fortress, ancient city, bastion and end-city loot tables. Per-table and per-container evidence are stored separately.

Three GameTests cover block and chest-minecart generation, actual opener versus another player, reopening, null-player generation, invalid tables and two ancient-city containers. They do not cover a real save/restart or every approved table. Therefore **all C1 quests remain unregistered**; catalog entries and rewards are unchanged. A container save/reload and second-process attribution test, plus focused classification/threshold tests for each condition, are required before promotion.

## C2-C6

Vaults, unique structures, combat attribution, dragon rematch, ominous raids and monument participants are not implemented or registered on this branch. Monument VERIFY quests remain unregistered. No Q1-C feature is ready for final integration review.
