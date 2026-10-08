# Custom Mob Resource Backlog

Gameplay registry, vanilla base renderers, localized names, and spawn configuration are implemented. The production pass created a dedicated base texture and renderer route for all 40 ordinary custom mobs while retaining each vanilla model, animation, and gameplay implementation. Exact official Minecraft 26.3 template sources, dimensions, production method, and per-mob in-game visual QA captures are recorded in [the production texture manifest](CUSTOM_MOB_TEXTURE_PRODUCTION_MANIFEST.md).

## Oct 7 production-art status

## 40-mob production-art status

The 40 dedicated base textures use exact Minecraft 26.3 client-JAR templates and were inspected in the actual Minecraft client. All 40 retain the existing vanilla model family and have dedicated texture paths connected in the client renderer. The template alpha masks, dimensions, and UV layout were verified for all 78 base and state/overlay PNG resources. The family comparison images and selected details are listed in the [production texture manifest](CUSTOM_MOB_TEXTURE_PRODUCTION_MANIFEST.md). Art remains reviewable as production candidates; no unrelated approved master assets were changed.

The five new T2 core items currently reference vanilla item models as temporary display references. Those references are functional, but they are not custom production icons.

| Mob | Texture path | Status |
|---|---|---|
| Swamp Zombie | `assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png` | Production candidate; in-game QA pass |
| Cave Zombie | `assets/moreminecrafts/textures/entity/mobs/cave_zombie.png` | Production candidate; in-game QA pass |
| Large Zombie | `assets/moreminecrafts/textures/entity/mobs/large_zombie.png` | Production candidate; in-game QA pass |
| Frozen Zombie | `assets/moreminecrafts/textures/entity/mobs/frozen_zombie.png` | Production candidate; in-game QA pass |
| Mossy Zombie | `assets/moreminecrafts/textures/entity/mobs/mossy_zombie.png` | Production candidate; in-game QA pass |
| Fungal Zombie | `assets/moreminecrafts/textures/entity/mobs/fungal_zombie.png` | Production candidate; in-game QA pass |
| Miner Zombie | `assets/moreminecrafts/textures/entity/mobs/miner_zombie.png` | Production candidate; in-game QA pass |
| Rotten Skeleton | `assets/moreminecrafts/textures/entity/mobs/rotten_skeleton.png` | Production candidate; in-game QA pass |
| Cave Skeleton | `assets/moreminecrafts/textures/entity/mobs/cave_skeleton.png` | Production candidate; in-game QA pass |
| Armored Skeleton | `assets/moreminecrafts/textures/entity/mobs/armored_skeleton.png` | Production candidate; in-game QA pass |
| Ashen Skeleton | `assets/moreminecrafts/textures/entity/mobs/ashen_skeleton.png` | Production candidate; in-game QA pass |
| Lesser Wither Skeleton | `assets/moreminecrafts/textures/entity/mobs/lesser_wither_skeleton.png` | Production candidate; in-game QA pass |
| Mountain Skeleton | `assets/moreminecrafts/textures/entity/mobs/mountain_skeleton.png` | Production candidate; in-game QA pass |
| Miner Skeleton | `assets/moreminecrafts/textures/entity/mobs/miner_skeleton.png` | Production candidate; in-game QA pass |
| Small Spider | `assets/moreminecrafts/textures/entity/mobs/small_spider.png` | Production candidate; in-game QA pass |
| Large Spider | `assets/moreminecrafts/textures/entity/mobs/large_spider.png` | Production candidate; in-game QA pass |
| Forest Spider | `assets/moreminecrafts/textures/entity/mobs/forest_spider.png` | Production candidate; in-game QA pass |
| Desert Spider | `assets/moreminecrafts/textures/entity/mobs/desert_spider.png` | Production candidate; in-game QA pass |
| Swamp Spider | `assets/moreminecrafts/textures/entity/mobs/swamp_spider.png` | Production candidate; in-game QA pass |
| Ice Spider | `assets/moreminecrafts/textures/entity/mobs/ice_spider.png` | Production candidate; in-game QA pass |
| Cave Bug | `assets/moreminecrafts/textures/entity/mobs/cave_bug.png` | Production candidate; in-game QA pass |
| Desert Bug | `assets/moreminecrafts/textures/entity/mobs/desert_bug.png` | Production candidate; in-game QA pass |
| End Bug | `assets/moreminecrafts/textures/entity/mobs/end_bug.png` | Production candidate; in-game QA pass |
| Wild Slime | `assets/moreminecrafts/textures/entity/mobs/wild_slime.png` | Production candidate; in-game QA pass |
| Mud Slime | `assets/moreminecrafts/textures/entity/mobs/mud_slime.png` | Production candidate; in-game QA pass |
| Sand Slime | `assets/moreminecrafts/textures/entity/mobs/sand_slime.png` | Production candidate; in-game QA pass |
| Lesser Magma Cube | `assets/moreminecrafts/textures/entity/mobs/lesser_magma_cube.png` | Production candidate; in-game QA pass |
| Drowned Sailor | `assets/moreminecrafts/textures/entity/mobs/drowned_sailor.png` | Production candidate; in-game QA pass |
| Deep Sea Drowned | `assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned.png` | Production candidate; in-game QA pass |
| River Drowned | `assets/moreminecrafts/textures/entity/mobs/river_drowned.png` | Production candidate; in-game QA pass |
| Wild Piglin | `assets/moreminecrafts/textures/entity/mobs/wild_piglin.png` | Production candidate; in-game QA pass |
| Bandit | `assets/moreminecrafts/textures/entity/mobs/bandit.png` | Production candidate; in-game QA pass |
| Bandit Archer | `assets/moreminecrafts/textures/entity/mobs/bandit_archer.png` | Production candidate; in-game QA pass |
| Bandit Warrior | `assets/moreminecrafts/textures/entity/mobs/bandit_warrior.png` | Production candidate; in-game QA pass |
| Wandering Raider | `assets/moreminecrafts/textures/entity/mobs/wandering_raider.png` | Production candidate; in-game QA pass |
| Feral Wolf | `assets/moreminecrafts/textures/entity/mobs/feral_wolf.png` | Production candidate; in-game QA pass |
| Cave Bat | `assets/moreminecrafts/textures/entity/mobs/cave_bat.png` | Production candidate; in-game QA pass |
| Giant Bee | `assets/moreminecrafts/textures/entity/mobs/giant_bee.png` | Production candidate; in-game QA pass |
| Lesser Phantom | `assets/moreminecrafts/textures/entity/mobs/lesser_phantom.png` | Production candidate; in-game QA pass |
| Cave Creeper | `assets/moreminecrafts/textures/entity/mobs/cave_creeper.png` | Production candidate; in-game QA pass |

Each texture path is now referenced by the corresponding existing vanilla-family renderer route. Companion state and overlay paths are documented in the production manifest. No custom geometry, model, animation, or VFX work was introduced.

## Follow-up visual review

- Select and review the held/worn equipment presentation for Miner Zombie, Miner Skeleton, Rotten/Cave Skeleton, Armored Skeleton, Bandit, Bandit Archer, Bandit Warrior, Wandering Raider, and Wild Piglin.
- Review entity visual scale and collision proportions together for Cave Zombie, Large Zombie, Lesser Wither Skeleton, Small Spider, Large Spider, Wild Slime, Mud Slime, Lesser Magma Cube, Cave Bat, Giant Bee, Lesser Phantom, and Cave Creeper.
- Review and approve the production candidate art in the family comparison/detail screenshots. All 40 texture routes and in-game QA captures are complete; equipment and scale remain part of the existing approved implementation rather than new gameplay changes.
