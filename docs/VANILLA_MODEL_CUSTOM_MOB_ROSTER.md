# Vanilla-Model Custom Mob Roster

Status: **Approved implementation scope**  
Resource status: **Approved for follow-up resource work**  
Baseline: `stage-19/checkpoint`

## Scope rules

These mobs are intended to expand the ordinary Minecraft ecosystem rather than act as elite encounters.

- Reuse vanilla entity models and vanilla animations.
- No custom 3D model is required for the initial implementation.
- No dedicated particles are required.
- No active skills or special ability systems are required.
- Differences should stay simple: texture, held/worn equipment, scale, spawn conditions, and modest base-stat changes where appropriate.
- Do not add complex AI, particles, active skills, or elite-style encounter mechanics merely to make a mob more distinctive without a later design decision.
- Follow-up resource work should at minimum consider a distinct texture for each mob. Held equipment may reuse existing assets unless a later resource pass decides otherwise.
- These are implementation targets, not merely an idea pool.

## Approved ordinary mob roster

### Zombie family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Swamp Zombie | Zombie | Swamp spawn; slightly slower and somewhat tougher than a normal zombie. |
| Cave Zombie | Zombie | Underground spawn; smaller and faster. |
| Large Zombie | Zombie | About 1.3x scale; slower and somewhat stronger. |
| Frozen Zombie | Zombie / Stray visual language | Snowy-biome zombie variant. |
| Mossy Zombie | Zombie | Forest / lush-cave zombie variant. |
| Fungal Zombie | Zombie | Mushroom / damp-cave zombie variant. |
| Miner Zombie | Zombie | Miner appearance and pickaxe; no mining AI required. |

### Skeleton family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Rotten Skeleton | Skeleton | Fragile melee skeleton. |
| Cave Skeleton | Skeleton | Underground melee skeleton using stone weapons. |
| Armored Skeleton | Skeleton | Slower skeleton wearing heavier armor. |
| Ashen Skeleton | Skeleton | Nether-themed ordinary skeleton. |
| Lesser Wither Skeleton | Wither Skeleton | Smaller/weaker, more ordinary wither-skeleton variant. |
| Mountain Skeleton | Stray | Mountain-spawning skeleton variant. |
| Miner Skeleton | Skeleton | Mine-themed melee skeleton carrying mining tools. |

### Spider family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Small Spider | Cave Spider | Smaller/faster spider without poison. |
| Large Spider | Spider | Larger/slower spider with more health. |
| Forest Spider | Spider | Forest variant; may remain neutral in daylight. |
| Desert Spider | Spider | Desert variant with slightly higher attack instead of poison. |
| Swamp Spider | Cave Spider | Swamp variant with weak poison. |
| Ice Spider | Spider | Snowy-biome spider variant. |

### Small creatures

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Cave Bug | Silverfish | Ordinary underground-spawning bug. |
| Desert Bug | Silverfish | Desert / pyramid-area variant. |
| End Bug | Endermite | Naturally occurring End variant. |

### Slime family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Wild Slime | Slime | Small ordinary slime for forest/swamp environments. |
| Mud Slime | Slime | Slower swamp-themed slime. |
| Sand Slime | Slime | Desert-themed slime. |
| Lesser Magma Cube | Magma Cube | Smaller, weaker magma cube appearing more broadly in the Nether. |

### Drowned family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Drowned Sailor | Drowned | Shipwreck-associated ordinary drowned variant. |
| Deep-Sea Drowned | Drowned | Slightly tougher deep-ocean variant. |
| River Drowned | Drowned | Weaker drowned variant that can occur in rivers. |

### Piglin and illager family

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Wild Piglin | Piglin | Lightly equipped piglin using simple stone/gold equipment. |
| Bandit | Pillager | Melee pillager variant using an iron sword. |
| Bandit Archer | Pillager | Ordinary bow-using bandit. |
| Bandit Warrior | Vindicator | Weaker ordinary vindicator-style melee unit. |
| Wandering Raider | Pillager | Small-number overworld roaming pillager variant. |

### Other ordinary variants

| Mob | Vanilla base | Initial identity |
| --- | --- | --- |
| Feral Wolf | Wolf | Hostile wild-wolf variant. |
| Cave Bat | Bat | Larger hostile bat dealing light damage. |
| Giant Bee | Bee | Larger neutral bee with somewhat more aggressive retaliation. |
| Lesser Phantom | Phantom | Smaller, weaker phantom variant that may be more common. |
| Cave Creeper | Creeper | Underground creeper variant with reduced explosion strength. |

## Preserved legacy design candidates

The following older concepts remain preserved separately and are **not part of the ordinary 40-mob roster above**:

- **Mire Shaman** — preserved custom-mob design; may justify more specialized behavior.
- **Shard Golem** — preserved design candidate; details remain to be finalized.

Their preservation does not imply that the ordinary-mob restrictions above must apply to them.

## Follow-up resource pass

All 40 ordinary mobs above are resource-work targets.

Initial resource priorities:

1. Dedicated texture/skin identity.
2. Vanilla-compatible held/worn equipment selection.
3. Scale/readability review where scale is part of the concept.
4. Spawn-environment visual fit.
5. Only after explicit later approval: custom models, custom animations, dedicated particles, or skill/VFX assets.

The ordinary roster should remain inexpensive and population-friendly. A mob does not need an active mechanic merely to justify its existence.
