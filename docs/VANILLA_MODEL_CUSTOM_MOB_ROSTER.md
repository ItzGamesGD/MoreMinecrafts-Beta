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


## Implementation companion

The approved 40-entry roster is implemented on the stage-19 recovery line by `CustomMobCatalog`. Concrete entity sizes, natural-spawn scopes, and spawn weights are recorded in [the implementation schedule](VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md). Every mob uses its approved vanilla entity family/model and the base family's current attributes and AI, except for the stated entity-size/collision adjustments. The per-mob custom texture work remains pending in [the resource backlog](CUSTOM_MOB_RESOURCE_BACKLOG.md). No roster additions, skills, VFX, custom models, or animations are included.

## Final implementation decisions

The following table records the selected implementation for every approved entry. Spawn weight is the vanilla spawn-list weight and the rarity label is relative within this custom roster. The listed biome tag is paired with the position gate; it does not replace vanilla mobs. Resource status is intentionally pending for every unique texture and visual review.

| Mob | Base entity/model | Primary environment and restriction | Rarity (weight) | Stat multipliers (health/speed/attack/scale/armor) | Equipment | Resource status |
|---|---|---|---|---|---|---|
| Swamp Zombie | Zombie | Swamp biomes; ground position gate; 0.60×1.95 collision size | Common (4) | ×1.20/×0.82/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Cave Zombie | Zombie | Overworld caves; cave position gate; 0.50×1.65 collision size | Uncommon (3) | ×0.90/×1.10/×1.00/×0.88/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Large Zombie | Zombie | Overworld; ground position gate; 0.78×2.54 collision size | Very rare (1) | ×1.55/×0.78/×1.20/×1.30/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Frozen Zombie | Zombie | Mountains; ground position gate; 0.60×1.95 collision size | Uncommon (3) | ×1.08/×0.94/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Mossy Zombie | Zombie | Forests; ground position gate; 0.60×1.95 collision size | Uncommon (3) | ×1.10/×0.92/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Fungal Zombie | Zombie | Jungles; cave position gate; 0.60×1.95 collision size | Rare (2) | ×0.95/×1.02/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Miner Zombie | Zombie | Mineshaft biomes, underground only; cave position gate; 0.60×1.95 collision size | Rare (2) | ×1.12/×0.92/×1.00/×1.00/×1.00 | Iron pickaxe | Unique texture pending; equipment/scale visual review pending |
| Rotten Skeleton | Skeleton | Overworld; ground position gate; 0.60×1.99 collision size | Uncommon (3) | ×0.72/×1.00/×0.82/×1.00/×1.00 | Stone sword | Unique texture pending; equipment/scale visual review pending |
| Cave Skeleton | Skeleton | Overworld caves; cave position gate; 0.60×1.99 collision size | Uncommon (3) | ×0.95/×1.04/×1.00/×1.00/×1.00 | Stone sword | Unique texture pending; equipment/scale visual review pending |
| Armored Skeleton | Skeleton | Mountains; ground position gate; 0.60×1.99 collision size | Rare (2) | ×1.30/×0.82/×1.00/×1.00/×1.5 | Iron helmet and chestplate | Unique texture pending; equipment/scale visual review pending |
| Ashen Skeleton | Skeleton | Nether; ground position gate; 0.60×1.99 collision size | Uncommon (3) | ×1.05/×1.00/×1.05/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Lesser Wither Skeleton | Wither Skeleton | Nether-fortress biomes; nether position gate; 0.55×1.75 collision size | Rare (2) | ×0.72/×1.00/×0.78/×0.86/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Mountain Skeleton | Stray | Mountains; ground position gate; 0.60×1.99 collision size | Uncommon (3) | ×1.05/×0.95/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Miner Skeleton | Skeleton | Mineshaft biomes, underground only; cave position gate; 0.60×1.99 collision size | Rare (2) | ×0.95/×1.02/×1.00/×1.00/×1.00 | Iron pickaxe | Unique texture pending; equipment/scale visual review pending |
| Small Spider | Spider | Overworld caves; cave position gate; 0.55×0.45 collision size | Rare (2) | ×0.60/×1.20/×1.00/×0.72/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Large Spider | Spider | Overworld; ground position gate; 1.20×1.00 collision size | Very rare (1) | ×1.65/×0.82/×1.12/×1.25/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Forest Spider | Spider | Forests; ground position gate; 0.80×0.60 collision size | Uncommon (3) | ×1.00/×0.94/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Desert Spider | Spider | Badlands; ground position gate; 0.80×0.60 collision size | Uncommon (3) | ×1.00/×1.00/×1.12/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Swamp Spider | Cave Spider | Swamp biomes; ground position gate; 0.70×0.50 collision size | Rare (2) | ×0.88/×1.00/×0.80/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Ice Spider | Spider | Mountains; ground position gate; 0.80×0.60 collision size | Rare (2) | ×1.08/×0.92/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Cave Bug | Silverfish | Overworld caves; cave position gate; 0.40×0.30 collision size | Uncommon (3) | ×1.15/×1.00/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Desert Bug | Silverfish | Desert-pyramid biomes, underground only; cave position gate; 0.40×0.30 collision size | Rare (2) | ×1.12/×1.00/×1.05/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| End Bug | Endermite | The End; ground position gate; 0.40×0.30 collision size | Uncommon (3) | ×1.00/×1.00/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Wild Slime | Slime | Forests; ground position gate; 0.52×0.52 collision size | Rare (2) | ×0.82/×1.00/×1.00/×0.82/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Mud Slime | Slime | Swamp biomes; ground position gate; 0.60×0.60 collision size | Uncommon (3) | ×1.15/×0.78/×1.00/×0.90/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Sand Slime | Slime | Badlands; ground position gate; 0.60×0.60 collision size | Uncommon (3) | ×1.00/×1.08/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Lesser Magma Cube | Magma Cube | Nether; nether position gate; 0.65×0.65 collision size | Rare (2) | ×0.78/×1.00/×0.80/×0.82/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Drowned Sailor | Drowned | Shipwreck biomes, water only; water position gate; 0.60×1.95 collision size | Very rare (1) | ×1.12/×1.00/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Deep Sea Drowned | Drowned | Deep oceans, water only; water position gate; 0.60×1.95 collision size | Uncommon (3) | ×1.35/×0.90/×1.00/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| River Drowned | Drowned | Rivers, water only; water position gate; 0.60×1.95 collision size | Rare (2) | ×0.82/×1.00/×0.88/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Wild Piglin | Piglin | Nether; nether position gate; 0.60×1.95 collision size | Rare (2) | ×0.82/×1.00/×0.82/×1.00/×0.75 | Vanilla Piglin weapon/armor rolls | Unique texture pending; equipment/scale visual review pending |
| Bandit | Pillager | Forests; ground position gate; 0.60×1.95 collision size | Rare (2) | ×0.90/×1.00/×1.00/×1.00/×1.00 | Iron sword; melee AI | Unique texture pending; equipment/scale visual review pending |
| Bandit Archer | Pillager | Forests; ground position gate; 0.60×1.95 collision size | Rare (2) | ×0.82/×1.00/×0.92/×1.00/×1.00 | Bow and arrows | Unique texture pending; equipment/scale visual review pending |
| Bandit Warrior | Vindicator | Forests; ground position gate; 0.60×1.95 collision size | Very rare (1) | ×0.85/×1.00/×0.85/×1.00/×1.00 | Vindicator iron axe | Unique texture pending; equipment/scale visual review pending |
| Wandering Raider | Pillager | Overworld; ground position gate; 0.60×1.95 collision size | Very rare (1) | ×0.78/×1.00/×1.00/×1.00/×1.00 | Vanilla Pillager crossbow | Unique texture pending; equipment/scale visual review pending |
| Feral Wolf | Wolf | Forests; ground position gate; 0.60×0.85 collision size | Very rare (1) | ×1.15/×1.00/×1.15/×1.00/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Cave Bat | Bat | Overworld caves; cave position gate; 0.90×0.90 collision size | Rare (2) | ×0.60/×1.08/×0.55/×1.55/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Giant Bee | Bee | Forests; ground position gate; 1.00×0.80 collision size | Very rare (1) | ×1.65/×1.00/×1.25/×1.45/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Lesser Phantom | Phantom | Overworld, visible sky above sea level; sky position gate; 0.70×0.45 collision size | Very rare (1) | ×0.60/×1.00/×0.72/×0.72/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |
| Cave Creeper | Creeper | Overworld caves; cave position gate; 0.60×1.70 collision size | Rare (2) | ×0.92/×1.00/×1.00/×0.90/×1.00 | Vanilla base behavior; no roster-specific equipment | Unique texture pending; equipment/scale visual review pending |

Custom entity tags preserve the relevant vanilla undead, zombie/skeleton, illager/raider, arthropod, aquatic, Smite/Bane, and beehive interactions. Each custom entity loot table aliases its vanilla base family. Full spawn gates, group sizes, aggregate spawn-density review, and the numerical table are also kept in [the implementation schedule](VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md).
