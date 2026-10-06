# Vanilla-Model Custom Mob Implementation Schedule

This companion records implementation choices permitted by the approved roster task. Each entry uses the named vanilla base entity renderer and AI, with the roster-specific exceptions recorded below; dimensions are entity collision dimensions in blocks. Spawn weight is the vanilla biome spawn-list weight. Every type has a group size of 1–3 (underground entries cap at 2). All entries use the monster population category. Cave placement requires no sky access, below sea level, and solid floor; ground placement requires air over solid floor; Nether placement uses air over solid floor; water placement requires water; sky placement requires visible sky above sea level + 12.

| ID | Vanilla base/model | Dimensions | Biome scope | Weight | Position gate |
|---|---|---:|---|---:|---|
| swamp_zombie | Zombie | 0.60×1.95 | Swamp biomes | 4 | ground |
| cave_zombie | Zombie | 0.50×1.65 | Overworld caves | 3 | cave |
| large_zombie | Zombie | 0.78×2.54 | Overworld | 1 | ground |
| frozen_zombie | Zombie | 0.60×1.95 | Mountains | 3 | ground |
| mossy_zombie | Zombie | 0.60×1.95 | Forests | 3 | ground |
| fungal_zombie | Zombie | 0.60×1.95 | Jungles | 2 | cave |
| miner_zombie | Zombie | 0.60×1.95 | Mineshaft biomes, underground only | 2 | cave |
| rotten_skeleton | Skeleton | 0.60×1.99 | Overworld | 3 | ground |
| cave_skeleton | Skeleton | 0.60×1.99 | Overworld caves | 3 | cave |
| armored_skeleton | Skeleton | 0.60×1.99 | Mountains | 2 | ground |
| ashen_skeleton | Skeleton | 0.60×1.99 | Nether | 3 | ground |
| lesser_wither_skeleton | Wither Skeleton | 0.55×1.75 | Nether-fortress biomes | 2 | nether |
| mountain_skeleton | Stray | 0.60×1.99 | Mountains | 3 | ground |
| miner_skeleton | Skeleton | 0.60×1.99 | Mineshaft biomes, underground only | 2 | cave |
| small_spider | Spider | 0.55×0.45 | Overworld caves | 2 | cave |
| large_spider | Spider | 1.20×1.00 | Overworld | 1 | ground |
| forest_spider | Spider | 0.80×0.60 | Forests | 3 | ground |
| desert_spider | Spider | 0.80×0.60 | Badlands | 3 | ground |
| swamp_spider | Cave Spider | 0.70×0.50 | Swamp biomes | 2 | ground |
| ice_spider | Spider | 0.80×0.60 | Mountains | 2 | ground |
| cave_bug | Silverfish | 0.40×0.30 | Overworld caves | 3 | cave |
| desert_bug | Silverfish | 0.40×0.30 | Desert-pyramid biomes, underground only | 2 | cave |
| end_bug | Endermite | 0.40×0.30 | The End | 3 | ground |
| wild_slime | Slime | 0.52×0.52 | Forests | 2 | ground |
| mud_slime | Slime | 0.60×0.60 | Swamp biomes | 3 | ground |
| sand_slime | Slime | 0.60×0.60 | Badlands | 3 | ground |
| lesser_magma_cube | Magma Cube | 0.65×0.65 | Nether | 2 | nether |
| drowned_sailor | Drowned | 0.60×1.95 | Shipwreck biomes, water only | 1 | water |
| deep_sea_drowned | Drowned | 0.60×1.95 | Deep oceans, water only | 3 | water |
| river_drowned | Drowned | 0.60×1.95 | Rivers, water only | 2 | water |
| wild_piglin | Piglin | 0.60×1.95 | Nether | 2 | nether |
| bandit | Pillager | 0.60×1.95 | Forests | 2 | ground |
| bandit_archer | Pillager | 0.60×1.95 | Forests | 2 | ground |
| bandit_warrior | Vindicator | 0.60×1.95 | Forests | 1 | ground |
| wandering_raider | Pillager | 0.60×1.95 | Overworld | 1 | ground |
| feral_wolf | Wolf | 0.60×0.85 | Forests | 1 | ground |
| cave_bat | Bat | 0.90×0.90 | Overworld caves | 2 | cave |
| giant_bee | Bee | 1.00×0.80 | Forests | 1 | ground |
| lesser_phantom | Phantom | 0.70×0.45 | Overworld, visible sky above sea level | 1 | sky |
| cave_creeper | Creeper | 0.60×1.70 | Overworld caves | 2 | cave |

## Spawn-density notes

The weights are deliberately low (1–4 per variant) and variants share the vanilla monster cap rather than creating a separate population budget. The aggregate roster weights by broad class are zombies 18, skeletons 18, spiders 13, small creatures/slimes 20, drowned 6, piglin/illagers 8, and other 7. Because biome predicates often overlap, custom variants replace a modest portion of ordinary monster rolls; they do not add an independent cap. No variant has a structure-distance scan: structure-themed entries use the corresponding vanilla biome structure tag plus their ordinary cave/water/Nether position gate.

## Runtime behavior and vanilla integration

Feral Wolf and Cave Bat use simple player-targeting melee behavior; Cave Creeper explosion radius is 60% of a vanilla Creeper blast. Miner Zombie and Miner Skeleton carry iron pickaxes, Rotten/Cave Skeleton carry stone swords, Bandit uses an iron sword with melee AI, Bandit Archer uses a bow, and Armored Skeleton carries an iron helmet and chestplate. Custom entities alias the matching vanilla family loot table and join vanilla entity tags for undead, zombie/skeleton, raider/illager, arthropod, aquatic, Smite/Bane, and beehive behavior.

## Stat multipliers

Multipliers are applied once to the matching vanilla base attribute during spawn finalization. Omitted changes use ×1.00. Visual scale multiplies the vanilla `scale` attribute; entity dimensions remain as listed above.

| ID | Health | Speed | Attack | Visual scale | Armor |
|---|---:|---:|---:|---:|---:|
| swamp_zombie | ×1.20 | ×0.82 | ×1.00 | ×1.00 | ×1.00 |
| cave_zombie | ×0.90 | ×1.10 | ×1.00 | ×0.88 | ×1.00 |
| large_zombie | ×1.55 | ×0.78 | ×1.20 | ×1.30 | ×1.00 |
| frozen_zombie | ×1.08 | ×0.94 | ×1.00 | ×1.00 | ×1.00 |
| mossy_zombie | ×1.10 | ×0.92 | ×1.00 | ×1.00 | ×1.00 |
| fungal_zombie | ×0.95 | ×1.02 | ×1.00 | ×1.00 | ×1.00 |
| miner_zombie | ×1.12 | ×0.92 | ×1.00 | ×1.00 | ×1.00 |
| rotten_skeleton | ×0.72 | ×1.00 | ×0.82 | ×1.00 | ×1.00 |
| cave_skeleton | ×0.95 | ×1.04 | ×1.00 | ×1.00 | ×1.00 |
| armored_skeleton | ×1.30 | ×0.82 | ×1.00 | ×1.00 | ×1.5 |
| ashen_skeleton | ×1.05 | ×1.00 | ×1.05 | ×1.00 | ×1.00 |
| lesser_wither_skeleton | ×0.72 | ×1.00 | ×0.78 | ×0.86 | ×1.00 |
| mountain_skeleton | ×1.05 | ×0.95 | ×1.00 | ×1.00 | ×1.00 |
| miner_skeleton | ×0.95 | ×1.02 | ×1.00 | ×1.00 | ×1.00 |
| small_spider | ×0.60 | ×1.20 | ×1.00 | ×0.72 | ×1.00 |
| large_spider | ×1.65 | ×0.82 | ×1.12 | ×1.25 | ×1.00 |
| forest_spider | ×1.00 | ×0.94 | ×1.00 | ×1.00 | ×1.00 |
| desert_spider | ×1.00 | ×1.00 | ×1.12 | ×1.00 | ×1.00 |
| swamp_spider | ×0.88 | ×1.00 | ×0.80 | ×1.00 | ×1.00 |
| ice_spider | ×1.08 | ×0.92 | ×1.00 | ×1.00 | ×1.00 |
| cave_bug | ×1.15 | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| desert_bug | ×1.12 | ×1.00 | ×1.05 | ×1.00 | ×1.00 |
| wild_slime | ×0.82 | ×1.00 | ×1.00 | ×0.82 | ×1.00 |
| mud_slime | ×1.15 | ×0.78 | ×1.00 | ×0.90 | ×1.00 |
| sand_slime | ×1.00 | ×1.08 | ×1.00 | ×1.00 | ×1.00 |
| lesser_magma_cube | ×0.78 | ×1.00 | ×0.80 | ×0.82 | ×1.00 |
| drowned_sailor | ×1.12 | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| deep_sea_drowned | ×1.35 | ×0.90 | ×1.00 | ×1.00 | ×1.00 |
| river_drowned | ×0.82 | ×1.00 | ×0.88 | ×1.00 | ×1.00 |
| wild_piglin | ×0.82 | ×1.00 | ×0.82 | ×1.00 | ×0.75 |
| bandit | ×0.90 | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| bandit_archer | ×0.82 | ×1.00 | ×0.92 | ×1.00 | ×1.00 |
| bandit_warrior | ×0.85 | ×1.00 | ×0.85 | ×1.00 | ×1.00 |
| wandering_raider | ×0.78 | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| feral_wolf | ×1.15 | ×1.00 | ×1.15 | ×1.00 | ×1.00 |
| cave_bat | ×0.60 | ×1.08 | ×0.55 | ×1.55 | ×1.00 |
| giant_bee | ×1.65 | ×1.00 | ×1.25 | ×1.45 | ×1.00 |
| lesser_phantom | ×0.60 | ×1.00 | ×0.72 | ×0.72 | ×1.00 |
| cave_creeper | ×0.92 | ×1.00 | ×1.00 | ×0.90 | ×1.00 |
