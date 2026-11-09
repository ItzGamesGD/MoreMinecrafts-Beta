# Custom Mob Resource Backlog

Gameplay registry, vanilla base renderers, localized names, and spawn configuration are implemented. The repository contains a dedicated base texture route for all 40 ordinary custom mobs and state companions. The current Oct 7 content-authority record still classifies production artwork and in-game review as pending; the texture manifest records provenance and audit limits.

## Oct 7 production-art status

## 40-mob production-art status

The 40 base PNGs use Minecraft 26.3 client-JAR templates, retain the vanilla model family, and have dedicated renderer paths. This audit verified all 78 PNG dimensions and per-pixel alpha masks against their corresponding templates in the cited client JAR. Pixel-level UV region correctness and in-game appearance are not independently verified here. The PR description reports a screenshot archive, but `/workspace/MoreMinecrafts-40-Mob-Texture-Review.zip` is absent from this checkout. In-game visual review remains pending.

The five new T2 core items currently reference vanilla item models as temporary display references. Those references are functional, but they are not custom production icons.

| Mob | Texture path | Status |
|---|---|---|
| Swamp Zombie | `assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png` | Production candidate; visual QA pending |
| Cave Zombie | `assets/moreminecrafts/textures/entity/mobs/cave_zombie.png` | Production candidate; visual QA pending |
| Large Zombie | `assets/moreminecrafts/textures/entity/mobs/large_zombie.png` | Production candidate; visual QA pending |
| Frozen Zombie | `assets/moreminecrafts/textures/entity/mobs/frozen_zombie.png` | Production candidate; visual QA pending |
| Mossy Zombie | `assets/moreminecrafts/textures/entity/mobs/mossy_zombie.png` | Production candidate; visual QA pending |
| Fungal Zombie | `assets/moreminecrafts/textures/entity/mobs/fungal_zombie.png` | Production candidate; visual QA pending |
| Miner Zombie | `assets/moreminecrafts/textures/entity/mobs/miner_zombie.png` | Production candidate; visual QA pending |
| Rotten Skeleton | `assets/moreminecrafts/textures/entity/mobs/rotten_skeleton.png` | Production candidate; visual QA pending |
| Cave Skeleton | `assets/moreminecrafts/textures/entity/mobs/cave_skeleton.png` | Production candidate; visual QA pending |
| Armored Skeleton | `assets/moreminecrafts/textures/entity/mobs/armored_skeleton.png` | Production candidate; visual QA pending |
| Ashen Skeleton | `assets/moreminecrafts/textures/entity/mobs/ashen_skeleton.png` | Production candidate; visual QA pending |
| Lesser Wither Skeleton | `assets/moreminecrafts/textures/entity/mobs/lesser_wither_skeleton.png` | Production candidate; visual QA pending |
| Mountain Skeleton | `assets/moreminecrafts/textures/entity/mobs/mountain_skeleton.png` | Production candidate; visual QA pending |
| Miner Skeleton | `assets/moreminecrafts/textures/entity/mobs/miner_skeleton.png` | Production candidate; visual QA pending |
| Small Spider | `assets/moreminecrafts/textures/entity/mobs/small_spider.png` | Production candidate; visual QA pending |
| Large Spider | `assets/moreminecrafts/textures/entity/mobs/large_spider.png` | Production candidate; visual QA pending |
| Forest Spider | `assets/moreminecrafts/textures/entity/mobs/forest_spider.png` | Production candidate; visual QA pending |
| Desert Spider | `assets/moreminecrafts/textures/entity/mobs/desert_spider.png` | Production candidate; visual QA pending |
| Swamp Spider | `assets/moreminecrafts/textures/entity/mobs/swamp_spider.png` | Production candidate; visual QA pending |
| Ice Spider | `assets/moreminecrafts/textures/entity/mobs/ice_spider.png` | Production candidate; visual QA pending |
| Cave Bug | `assets/moreminecrafts/textures/entity/mobs/cave_bug.png` | Production candidate; visual QA pending |
| Desert Bug | `assets/moreminecrafts/textures/entity/mobs/desert_bug.png` | Production candidate; visual QA pending |
| End Bug | `assets/moreminecrafts/textures/entity/mobs/end_bug.png` | Production candidate; visual QA pending |
| Wild Slime | `assets/moreminecrafts/textures/entity/mobs/wild_slime.png` | Production candidate; visual QA pending |
| Mud Slime | `assets/moreminecrafts/textures/entity/mobs/mud_slime.png` | Production candidate; visual QA pending |
| Sand Slime | `assets/moreminecrafts/textures/entity/mobs/sand_slime.png` | Production candidate; visual QA pending |
| Lesser Magma Cube | `assets/moreminecrafts/textures/entity/mobs/lesser_magma_cube.png` | Production candidate; visual QA pending |
| Drowned Sailor | `assets/moreminecrafts/textures/entity/mobs/drowned_sailor.png` | Production candidate; visual QA pending |
| Deep Sea Drowned | `assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned.png` | Production candidate; visual QA pending |
| River Drowned | `assets/moreminecrafts/textures/entity/mobs/river_drowned.png` | Production candidate; visual QA pending |
| Wild Piglin | `assets/moreminecrafts/textures/entity/mobs/wild_piglin.png` | Production candidate; visual QA pending |
| Bandit | `assets/moreminecrafts/textures/entity/mobs/bandit.png` | Production candidate; visual QA pending |
| Bandit Archer | `assets/moreminecrafts/textures/entity/mobs/bandit_archer.png` | Production candidate; visual QA pending |
| Bandit Warrior | `assets/moreminecrafts/textures/entity/mobs/bandit_warrior.png` | Production candidate; visual QA pending |
| Wandering Raider | `assets/moreminecrafts/textures/entity/mobs/wandering_raider.png` | Production candidate; visual QA pending |
| Feral Wolf | `assets/moreminecrafts/textures/entity/mobs/feral_wolf.png` | Production candidate; visual QA pending |
| Cave Bat | `assets/moreminecrafts/textures/entity/mobs/cave_bat.png` | Production candidate; visual QA pending |
| Giant Bee | `assets/moreminecrafts/textures/entity/mobs/giant_bee.png` | Production candidate; visual QA pending |
| Lesser Phantom | `assets/moreminecrafts/textures/entity/mobs/lesser_phantom.png` | Production candidate; visual QA pending |
| Cave Creeper | `assets/moreminecrafts/textures/entity/mobs/cave_creeper.png` | Production candidate; visual QA pending |

Each texture path is now referenced by the corresponding existing vanilla-family renderer route. Companion state and overlay paths are documented in the production manifest. No custom geometry, model, animation, or VFX work was introduced.

## Follow-up visual review

- Select and review the held/worn equipment presentation for Miner Zombie, Miner Skeleton, Rotten/Cave Skeleton, Armored Skeleton, Bandit, Bandit Archer, Bandit Warrior, Wandering Raider, and Wild Piglin.
- Review entity visual scale and collision proportions together for Cave Zombie, Large Zombie, Lesser Wither Skeleton, Small Spider, Large Spider, Wild Slime, Mud Slime, Lesser Magma Cube, Cave Bat, Giant Bee, Lesser Phantom, and Cave Creeper.
- Review and approve the production candidate art in the family comparison/detail screenshots. The referenced screenshot archive was not available in this checkout, so in-game QA remains pending; equipment and scale remain part of the existing approved implementation rather than new gameplay changes.
