# Custom Mob Resource Backlog

Gameplay registry, vanilla base renderers, localized names, and spawn configuration are implemented. Unique custom mob textures are intentionally not fabricated as placeholders. Until art is approved, each renderer uses its vanilla family texture. Each path is a reserved target for a future art task; using it will require the client renderer to route that entity to its approved texture, without changing entity registration or gameplay code.

| Mob | Future texture path | Status |
|---|---|---|
| Swamp Zombie | `assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png` | Pending art |
| Cave Zombie | `assets/moreminecrafts/textures/entity/mobs/cave_zombie.png` | Pending art |
| Large Zombie | `assets/moreminecrafts/textures/entity/mobs/large_zombie.png` | Pending art |
| Frozen Zombie | `assets/moreminecrafts/textures/entity/mobs/frozen_zombie.png` | Pending art |
| Mossy Zombie | `assets/moreminecrafts/textures/entity/mobs/mossy_zombie.png` | Pending art |
| Fungal Zombie | `assets/moreminecrafts/textures/entity/mobs/fungal_zombie.png` | Pending art |
| Miner Zombie | `assets/moreminecrafts/textures/entity/mobs/miner_zombie.png` | Pending art |
| Rotten Skeleton | `assets/moreminecrafts/textures/entity/mobs/rotten_skeleton.png` | Pending art |
| Cave Skeleton | `assets/moreminecrafts/textures/entity/mobs/cave_skeleton.png` | Pending art |
| Armored Skeleton | `assets/moreminecrafts/textures/entity/mobs/armored_skeleton.png` | Pending art |
| Ashen Skeleton | `assets/moreminecrafts/textures/entity/mobs/ashen_skeleton.png` | Pending art |
| Lesser Wither Skeleton | `assets/moreminecrafts/textures/entity/mobs/lesser_wither_skeleton.png` | Pending art |
| Mountain Skeleton | `assets/moreminecrafts/textures/entity/mobs/mountain_skeleton.png` | Pending art |
| Miner Skeleton | `assets/moreminecrafts/textures/entity/mobs/miner_skeleton.png` | Pending art |
| Small Spider | `assets/moreminecrafts/textures/entity/mobs/small_spider.png` | Pending art |
| Large Spider | `assets/moreminecrafts/textures/entity/mobs/large_spider.png` | Pending art |
| Forest Spider | `assets/moreminecrafts/textures/entity/mobs/forest_spider.png` | Pending art |
| Desert Spider | `assets/moreminecrafts/textures/entity/mobs/desert_spider.png` | Pending art |
| Swamp Spider | `assets/moreminecrafts/textures/entity/mobs/swamp_spider.png` | Pending art |
| Ice Spider | `assets/moreminecrafts/textures/entity/mobs/ice_spider.png` | Pending art |
| Cave Bug | `assets/moreminecrafts/textures/entity/mobs/cave_bug.png` | Pending art |
| Desert Bug | `assets/moreminecrafts/textures/entity/mobs/desert_bug.png` | Pending art |
| End Bug | `assets/moreminecrafts/textures/entity/mobs/end_bug.png` | Pending art |
| Wild Slime | `assets/moreminecrafts/textures/entity/mobs/wild_slime.png` | Pending art |
| Mud Slime | `assets/moreminecrafts/textures/entity/mobs/mud_slime.png` | Pending art |
| Sand Slime | `assets/moreminecrafts/textures/entity/mobs/sand_slime.png` | Pending art |
| Lesser Magma Cube | `assets/moreminecrafts/textures/entity/mobs/lesser_magma_cube.png` | Pending art |
| Drowned Sailor | `assets/moreminecrafts/textures/entity/mobs/drowned_sailor.png` | Pending art |
| Deep Sea Drowned | `assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned.png` | Pending art |
| River Drowned | `assets/moreminecrafts/textures/entity/mobs/river_drowned.png` | Pending art |
| Wild Piglin | `assets/moreminecrafts/textures/entity/mobs/wild_piglin.png` | Pending art |
| Bandit | `assets/moreminecrafts/textures/entity/mobs/bandit.png` | Pending art |
| Bandit Archer | `assets/moreminecrafts/textures/entity/mobs/bandit_archer.png` | Pending art |
| Bandit Warrior | `assets/moreminecrafts/textures/entity/mobs/bandit_warrior.png` | Pending art |
| Wandering Raider | `assets/moreminecrafts/textures/entity/mobs/wandering_raider.png` | Pending art |
| Feral Wolf | `assets/moreminecrafts/textures/entity/mobs/feral_wolf.png` | Pending art |
| Cave Bat | `assets/moreminecrafts/textures/entity/mobs/cave_bat.png` | Pending art |
| Giant Bee | `assets/moreminecrafts/textures/entity/mobs/giant_bee.png` | Pending art |
| Lesser Phantom | `assets/moreminecrafts/textures/entity/mobs/lesser_phantom.png` | Pending art |
| Cave Creeper | `assets/moreminecrafts/textures/entity/mobs/cave_creeper.png` | Pending art |

The paths are reserved deliverables, not currently referenced by the runtime renderer; all current renderers point to the corresponding vanilla texture. No custom geometry, model, animation, or VFX work is in this backlog.

## Follow-up visual review

- Select and review the held/worn equipment presentation for Miner Zombie, Miner Skeleton, Rotten/Cave Skeleton, Armored Skeleton, Bandit, Bandit Archer, Bandit Warrior, Wandering Raider, and Wild Piglin.
- Review entity visual scale and collision proportions together for Cave Zombie, Large Zombie, Lesser Wither Skeleton, Small Spider, Large Spider, Wild Slime, Mud Slime, Lesser Magma Cube, Cave Bat, Giant Bee, Lesser Phantom, and Cave Creeper.
- Add and approve one dedicated texture for every roster entry listed above. Current base-family texture fallback only proves that the vanilla renderer path exists; it does not complete this art work.
