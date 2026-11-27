# 40-Mob Spawn and Ecology Audit — 2026-10-08

## Review baseline

- PR: #72, `resources/40-custom-mob-production-textures`
- Base: `stage-20/checkpoint` at `20121ca7cceae03602038de31076bd9ace115208`
- Reviewed source before fixes: `5d25c08b47d3b942dfb3580202a387763dee2714`
- Roster authority: [Vanilla-model custom mob roster](VANILLA_MODEL_CUSTOM_MOB_ROSTER.md)
- Spawn implementation schedule: [Vanilla-model custom mob implementation schedule](VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md)
- Current resource authority: [Content authority index](CONTENT_AUTHORITY_INDEX.md) and its Oct 7 continuation/implementation records

The current task explicitly supersedes the earlier Wild Piglin conversion behavior recorded in the implementation schedule. The implementation schedule documents the roster's existing biome, position, weight, group-size, and structure choices. This audit preserves those documented choices and identifies where the vanilla family uses a special natural-spawn system that the schedule does not map.

## Shared spawn mechanism

`CustomMobCatalog.initialize()` registers each variant through Fabric `BiomeModifications.addSpawn`, using `spec.type.getCategory()` at [`CustomMobCatalog.java`](../src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java#L199). This puts variants into the ordinary biome spawn list for their vanilla population category. It does not add a second spawner, custom population cap, or additional category spawn-attempt loop. Every variant therefore uses its `EntityType` category's existing cap; weights change which entries are picked from that shared category list. Minecraft has a category list here, not an isolated “zombie-only” or “spider-only” list.

The same registration installs a vanilla-family spawn predicate, an `ON_GROUND`, `IN_WATER`, or sky-capable placement, the approved position gate, and any documented structure association. Ordinary hostile variants are in `MONSTER`, so a dark hostile-mob tower can select them only when its biome selector, family predicate, and position gate all pass. This does not guarantee that every tower selects every variant. Feral Wolf and Giant Bee use `CREATURE`; Cave Bat uses `AMBIENT`; none use the hostile `MONSTER` cap or a standard dark hostile-mob tower.

Placement shorthand: `GROUND` = air above solid floor; `CAVE` = no sky access, below sea level, solid floor; `NETHER` = air above solid floor; `WATER` = water at spawn position; `SKY` = visible sky above sea level + 12. Biome selectors, weights, and group sizes below are the current source values. Every group is 1–3 except `CAVE` groups, which are 1–2.

## Per-mob registration audit

| Mob / ID | Custom type → base family | Category; weight | Biome selector / placement | Structure gate and ordinary tower eligibility | Audit result |
|---|---|---:|---|---|---|
| Swamp Zombie `swamp_zombie` | `Zombie` → Zombie | MONSTER; 4 | `swamp_habitat` / GROUND | None; dark swamp tower can select it | Shared Monster list/cap; roster schedule match |
| Cave Zombie `cave_zombie` | `Zombie` → Zombie | MONSTER; 3 | Overworld / CAVE | None; only eligible underground dark spaces below sea level | Shared Monster list/cap; roster schedule match |
| Large Zombie `large_zombie` | `Zombie` → Zombie | MONSTER; 1 | Overworld / GROUND | None; eligible in dark Overworld towers | Shared Monster list/cap; roster schedule match |
| Frozen Zombie `frozen_zombie` | `Zombie` → Zombie | MONSTER; 3 | `snowy_habitat` / GROUND | None; eligible in matching snowy dark towers | Shared Monster list/cap; roster schedule match |
| Mossy Zombie `mossy_zombie` | `Zombie` → Zombie | MONSTER; 3 | `is_forest` / GROUND | None; eligible in matching forest dark towers | Shared Monster list/cap; roster schedule match |
| Fungal Zombie `fungal_zombie` | `Zombie` → Zombie | MONSTER; 2 | `fungal_habitat` / CAVE | None; eligible only in matching underground dark spaces | Shared Monster list/cap; roster schedule match |
| Miner Zombie `miner_zombie` | `MinerZombie` → Zombie | MONSTER; 2 | `has_mineshaft` / CAVE | Must be inside a generated mineshaft piece; no ordinary tower spawn | Structure exception is documented in the implementation schedule |
| Rotten Skeleton `rotten_skeleton` | `MeleeSkeleton` → Skeleton | MONSTER; 3 | Overworld / GROUND | None; eligible in dark Overworld towers | Shared Monster list/cap; roster schedule match |
| Cave Skeleton `cave_skeleton` | `MeleeSkeleton` → Skeleton | MONSTER; 3 | Overworld / CAVE | None; only underground dark spaces below sea level | Shared Monster list/cap; roster schedule match |
| Armored Skeleton `armored_skeleton` | `ArmoredSkeleton` → Skeleton | MONSTER; 2 | `is_mountain` / GROUND | None; eligible in matching dark mountain towers | Shared Monster list/cap; roster schedule match |
| Ashen Skeleton `ashen_skeleton` | `Skeleton` → Skeleton | MONSTER; 3 | `is_nether` / GROUND | None; not an Overworld tower mob | Shared Monster list/cap; roster schedule match |
| Lesser Wither Skeleton `lesser_wither_skeleton` | `WitherSkeleton` → Wither Skeleton | MONSTER; 2 | `has_nether_fortress` / NETHER | Must be inside a generated Nether Fortress piece; not an Overworld tower mob | Structure exception is documented in the implementation schedule |
| Mountain Skeleton `mountain_skeleton` | `Stray` → Stray | MONSTER; 3 | `is_mountain` / GROUND | None; eligible in matching dark mountain towers, with Stray spawn rules | Shared Monster list/cap; roster schedule match |
| Miner Skeleton `miner_skeleton` | `MeleeSkeleton` → Skeleton | MONSTER; 2 | `has_mineshaft` / CAVE | Must be inside a generated mineshaft piece; no ordinary tower spawn | Structure exception is documented in the implementation schedule |
| Small Spider `small_spider` | `SmallCaveSpider` → Cave Spider | MONSTER; 2 | Overworld / CAVE | None; only underground dark spaces below sea level | Shared Monster list/cap; Cave Spider poison is intentionally suppressed |
| Large Spider `large_spider` | `Spider` → Spider | MONSTER; 1 | Overworld / GROUND | None; eligible in dark Overworld towers | Shared Monster list/cap; roster schedule match |
| Forest Spider `forest_spider` | `Spider` → Spider | MONSTER; 3 | `is_forest` / GROUND | None; eligible in matching forest dark towers | Shared Monster list/cap; roster schedule match |
| Desert Spider `desert_spider` | `Spider` → Spider | MONSTER; 3 | `desert_habitat` / GROUND | None; eligible in matching desert dark towers | Shared Monster list/cap; roster schedule match |
| Swamp Spider `swamp_spider` | `SwampCaveSpider` → Cave Spider | MONSTER; 2 | `swamp_habitat` / GROUND | None; eligible in matching swamp dark towers | Shared Monster list/cap; roster schedule match |
| Ice Spider `ice_spider` | `Spider` → Spider | MONSTER; 2 | `snowy_habitat` / GROUND | None; eligible in matching snowy dark towers | Shared Monster list/cap; roster schedule match |
| Cave Bug `cave_bug` | `Silverfish` → Silverfish | MONSTER; 3 | Overworld / CAVE | None; only underground dark spaces below sea level | Shared Monster list/cap; roster schedule match |
| Desert Bug `desert_bug` | `Silverfish` → Silverfish | MONSTER; 2 | `has_desert_pyramid` / CAVE | Exact generated pyramid footprint, including its documented 14-block underground volume; not a general tower spawn | Explicit exception documented in the implementation schedule |
| End Bug `end_bug` | `Endermite` → Endermite | MONSTER; 3 | `is_end` / GROUND | None; not an Overworld tower mob | Shared Monster list/cap; roster schedule match |
| Wild Slime `wild_slime` | `CatalogSlime` → Slime | MONSTER; 2 | Forests + `swamp_habitat` / GROUND | No structure; eligible in matching dark hostile towers | Explicit Slime-family exception: normal Monster checks replace vanilla Slime's slime-chunk/full-moon gate |
| Mud Slime `mud_slime` | `CatalogSlime` → Slime | MONSTER; 3 | `swamp_habitat` / GROUND | No structure; eligible in matching dark swamp towers | Explicit Slime-family exception: normal Monster checks replace vanilla Slime's special gate |
| Sand Slime `sand_slime` | `CatalogSlime` → Slime | MONSTER; 3 | `desert_habitat` / GROUND | No structure; eligible in matching dark desert towers | Explicit Slime-family exception: normal Monster checks replace vanilla Slime's special gate |
| Lesser Magma Cube `lesser_magma_cube` | `CatalogMagmaCube` → Magma Cube | MONSTER; 2 | `is_nether` / NETHER | No structure; not an Overworld tower mob | Shares Monster cap/list and uses Magma Cube family spawn predicate |
| Drowned Sailor `drowned_sailor` | `Drowned` → Drowned | MONSTER; 1 | `has_shipwreck` / WATER | Must be in a generated shipwreck piece and water; not a dry mob tower | Structure exception documented in the implementation schedule |
| Deep Sea Drowned `deep_sea_drowned` | `Drowned` → Drowned | MONSTER; 3 | `is_deep_ocean` / WATER | Water only; not a dry mob tower | Shared Monster list/cap; Drowned family spawn predicate |
| River Drowned `river_drowned` | `Drowned` → Drowned | MONSTER; 2 | `is_river` / WATER | Water only; not a dry mob tower | Shared Monster list/cap; Drowned family spawn predicate |
| Wild Piglin `wild_piglin` | `WildPiglin` → Piglin | MONSTER; 2 | `is_nether` / NETHER | No structure; not an Overworld tower mob | Shared Monster list/cap; Nether-only natural spawn; conversion defect fixed |
| Bandit `bandit` | `BanditEntity` → Pillager | MONSTER; 2 | `is_forest` / GROUND | None; eligible in matching dark forest towers | Shared Monster list/cap; Raider/Illager tags and raid-buff hook; not inserted into special raid/patrol composition lists |
| Bandit Archer `bandit_archer` | `BanditArcher` → Pillager | MONSTER; 2 | `is_forest` / GROUND | None; eligible in matching dark forest towers | Shared Monster list/cap; bow and raid-buff hook; not inserted into special raid/patrol composition lists |
| Bandit Warrior `bandit_warrior` | `Vindicator` → Vindicator | MONSTER; 1 | `is_forest` / GROUND | None; eligible in matching dark forest towers | Shared Monster list/cap; Illager/Raider tags; not inserted into special raid/patrol composition lists |
| Wandering Raider `wandering_raider` | `Pillager` → Pillager | MONSTER; 1 | Overworld / GROUND | None; eligible in dark Overworld towers | Shared Monster list/cap; not inserted into special raid/patrol composition lists |
| Feral Wolf `feral_wolf` | `FeralWolf` → Wolf | CREATURE; 1 | `is_forest` / GROUND | None; not a dark hostile-mob tower spawn | Shares Wolf/Creature cap; hostile targeting and hostile-distance despawn; offspring fix added |
| Cave Bat `cave_bat` | `CaveBatEntity` → Bat | AMBIENT; 2 | Overworld / CAVE | None; natural cave context, not Monster cap/tower | Shares Bat/Ambient cap; code uses direct steering toward targets; real cave navigation/attack remains live validation |
| Giant Bee `giant_bee` | `GiantBeeEntity` → Bee | CREATURE; 1 | `is_forest` / GROUND | None; not a dark hostile-mob tower spawn | Shares Bee/Creature cap; vanilla Bee behavior; offspring fix added |
| Lesser Phantom `lesser_phantom` | `LesserPhantom` → Phantom | MONSTER; 1 | Overworld / SKY | Visible sky above sea level + 12; can enter the ordinary Monster list at eligible dark times, not a dry cave tower | **Design conflict:** vanilla Phantom uses the insomnia `PhantomSpawner`; this is currently an ordinary category-list spawn and has no approved insomnia probability mapping |
| Cave Creeper `cave_creeper` | `Creeper` → Creeper | MONSTER; 2 | Overworld / CAVE | None; only underground dark spaces below sea level | Shared Monster list/cap; roster schedule match |

## AI, tags, loot, and interactions

- Entity implementations inherit the named vanilla class except the documented custom behavior classes. Family registration predicates use `Monster`, `Drowned`, `Piglin`, `Wolf`, `Bat`, `Animal`, `Silverfish`, `Endermite`, `Stray`, or `MagmaCube` checks as appropriate. No custom mob-cap or separate despawn manager exists.
- Feral Wolf and Cave Bat implement hostile targeting while retaining the vanilla `CREATURE`/`AMBIENT` categories; Feral Wolf clears its target when tamed and untamed wolves have a 128-block despawn cutoff. Taming, live pathfinding, and attack feel still require an interactive client/server check.
- Giant Bee inherits vanilla bee anger, nectar, flower, hive, and honey behavior. Its custom child identity and custom stat profile are now produced by `GiantBeeEntity`.
- Slime split identity has a GameTest. Small/Swamp Spider poison behavior and Bandit Archer ranged behavior have GameTests. Hostile-family tags, selected concrete semantic tags, and family categories are checked by existing JUnit/GameTests.
- Wild Piglin retains Piglin AI/equipment and Nether spawn context. A new subclass resets the overworld-conversion timer before vanilla tick processing; a regression GameTest now requires it to remain a live `WILD_PIGLIN` past the vanilla threshold.
- All 40 custom loot JSON files refer to the corresponding vanilla family entity table. The Minecraft 26.3 server JAR was checked recursively for `entity_properties` exact-type conditions in those referenced tables; none of the 40 family aliases is blocked by a base-type check.
- Vanilla advancement criteria are different: `adventure/kill_a_mob`, `adventure/kill_all_mobs`, and `husbandry/bred_all_animals` enumerate exact vanilla entity types. Custom variants are not automatically included in those criteria. Whether their criteria should be expanded is not specified by the current roster authority; no advancement override was invented.
- No `SpawnEggItem` registration or spawn-egg resource exists for the 40 variants. The current authority does not require eggs, so no creative-item or survival acquisition path was added. A spawner can refer to a registered custom EntityType by ID, but no dedicated spawn egg interaction or live spawner test is present.
- Special raid/patrol composition uses explicit vanilla entity selections; Illager tags and `applyRaidBuffs` preserve Illager semantics but do not make the four custom raiders appear in Pillager patrols or raid waves. The authority documents weapon/raid-buff behavior but does not set substitution probabilities. This remains a design decision, not an inferred implementation change.

## Resource and visual audit

- 40 entity registrations and 40 renderer routes exist; repository JUnit coverage inventories 40 IDs/renderers and 78 expected PNG paths.
- All 78 PNGs decode, have unique file contents, and match the dimensions and per-pixel alpha masks of their corresponding templates in the official Minecraft 26.3 client JAR (SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`). Pixel-level UV region correctness and in-game appearance were not independently verified in this audit.
- State routing exists for baby textures, spider/phantom eyes, Bee/Wolf states, Stray overlay, Drowned outer layers, and Creeper power armor. No `*_detail` PNG assets exist; the manifest's `*-detail-*.png` names are screenshot names.
- The PR description reports a 38-screenshot archive and visual pass, but the archive is absent from this checkout. The current Oct 7 resource authority still says production artwork and in-game review are pending. Therefore in-game texture QA is **LIVE VALIDATION PENDING**; the manifest/backlog were corrected to reflect this. Redistribution/license status for the 78 derivatives is not documented or confirmed.

## Defects, fixes, and unresolved decisions

| Severity | Finding | Action |
|---|---|---|
| High | Wild Piglin converted to vanilla Zombified Piglin after its overworld timer elapsed, losing its approved identity. | Fixed with `WildPiglin` tick guard and a regression GameTest. |
| Medium | Breeding Feral Wolf returned vanilla Wolf; Giant Bee used vanilla Bee's offspring factory. Both could lose custom type/profile. | Fixed with custom offspring factories and profile application; regression GameTest added. |
| Low | The shared GROUND predicate rejected magma-block floors beyond the approved `air over solid floor` rule. | Removed the extra block-specific restriction. |
| Design decision | Lesser Phantom's scheduled sky/category spawn does not use vanilla insomnia spawning. No matching chance/rate is authorized. | No spawner integration or probability was invented. |
| Design decision | Custom Illagers have tags and raid-buff hooks, but no special patrol/raid-wave selection. | No variant weights or wave substitutions were invented. |
| Design decision | Vanilla exact-type advancement criteria omit custom variants. | No advancement replacement or criteria expansion was invented. |
| Review dependency | Texture redistribution/license status is unknown; screenshot archive was not available here. | Recorded as unconfirmed/pending; production approval remains pending. |

Automated test/build results and current PR review-thread state are reported in the task completion note. Interactive cave pathfinding, raid/patrol participation, spawner use, and final texture appearance are not claimed as automatically validated.
