# MoreMinecrafts Full Content Recovery and Reconciliation

Date: 2026-09-24  
Scope: Paper final source/resources + Content Freeze v3 + current MoreMinecrafts design/implementation  
Purpose: master recovery ledger. This document records what survived, changed, was reclassified, was removed from the current baseline, or was explicitly retired. It is documentation only.

## Status vocabulary

- `CURRENT_IMPLEMENTED`: implemented in Fabric.
- `CURRENT_LOCKED`: current design is authoritative.
- `CURRENT_LOCKED_NOT_IMPLEMENTED`: authoritative but not yet implemented in Fabric.
- `CHANGED`: historical design survives in modified form.
- `RECLASSIFIED`: identity survives but tier/carrier/system role changed.
- `REMOVED_FROM_BASELINE`: absent from the current baseline, without evidence of permanent retirement.
- `RETIRED`: explicitly superseded/removed. Do not revive automatically.
- `HISTORICAL_REFERENCE`: evidence only.
- `DEFERRED`: preserved candidate, not baseline implementation scope.
- `REVIEW_REQUIRED`: Paper content exists but current Mod disposition is not final.

## 1. Core structure

### CURRENT_LOCKED

North Star: **Do not replace vanilla. Extend the end of vanilla.**

Use Minecraft-native crafting/mining/brewing/fishing/enchanting/anvil/smithing behavior. Content may interact, but should not be forced through a single RPG progression engine.

Native workstations:
- Crafting Table
- Furnace / Blast Furnace
- Anvil
- Smithing Table
- Brewing Stand

### RETIRED

- Coin-centered progression
- giant custom crafting/progression menus
- custom repair/enchant/growth GUIs
- Mining/Farming/Alchemy standalone GUIs
- character-sheet RPG architecture
- generic RPG mob-level scaling as the Mod baseline

## 2. Equipment progression

### CURRENT_LOCKED

Weapons:
- T1 Vanilla
- T2 Elemental
- T3 Special
- T4 Endgame

Armor:
- T1 Vanilla armor
- T2 Special armor
- T3 Endgame armor

Tiers are categories, not an automatic evolution tree.

Independent growth axes:
- Enhancement = numerical growth
- Coating = functional growth
- Enchantment = Minecraft-native enchantment growth

### Enhancement 2.0: CURRENT_DIRECTION

- T1 Vanilla and T2 Elemental ordinary cap: +20
- deterministic milestones at +5/+10/+15/+20, derived from `enhancement_level`
- no failure, downgrade, destruction, pity, reroll, Promotion, or Coin cost
- Flame Sword and Wind Sword use explicit Elemental sword definitions
- T3 remains outside ordinary Enhancement; T4 will later use kill-point Grade/Mastery
- Anvil is only the current adapter, not locked interaction design
- exact curves, milestone bonuses, tool utility, charge speed, Mace formula, armor arithmetic (within the ~8% full-set ceiling), and costs remain provisional/deferred
- monster scaling is deferred to the future mob/balance stage and is not part of Enhancement

### Promotion/options: RETIRED

- Promotion Stone
- star/grade promotion
- promotion options
- random option roll/reroll
- enchant-slot unlock
- promotion GUI
- promotion combat/repair modifiers

## 3. T2 Elemental equipment

### Historical Paper breadth

Paper carried a much broader element/carrier matrix:
- Fire Sword / Fire Bow
- Water Sword / Poseidon Spear
- Wind Sword / Wind Bow
- Earth Sword / Earth Bow / Earth Mace
- Ice Sword / Ice Bow
- Magic/Dark Sword / Bow

### Current authoritative roster: six

- Fire: Flame Sword
- Water: Flowing Water Sword
- Wind: Wind Sword
- Wind: Wind Archer Bow
- Earth: Earth Mace
- Dark Energy: Staff
- no T2 Ice item

### Reconciliation

- Flame Sword: `CURRENT_IMPLEMENTED`
- Fire Bow: `REMOVED_FROM_BASELINE`
- Water Sword: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- Poseidon Spear: `RECLASSIFIED` to T3 Special
- Wind Sword: `CURRENT_IMPLEMENTED`
- Wind Bow: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- Earth Sword / Earth Bow: `REMOVED_FROM_BASELINE`
- Earth Mace: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- Ice Sword / Ice Bow: `REMOVED_FROM_T2_BASELINE`; Ice identity survives as T3 Frozen Sword
- Dark/Magic Sword / Bow: `CHANGED / RECLASSIFIED` to one Dark Energy Staff

The old idea that every element should receive every weapon carrier is retired.

## 4. T3 Special equipment

### CURRENT_LOCKED_NOT_IMPLEMENTED

1. Thanatos Mace
2. Poseidon Spear
3. Flame Axe
4. Thunder God's Axe
5. Holy Sword Solaris
6. Moonlit Afterglow
7. Frozen Sword

Recovered Paper ability identities:
- Thanatos: Mortal, Death's Oppression, Death Sentence, Ultimatum
- Poseidon: life-steal, pressure-thrust, current-throw, signature, riptide
- Flame Axe: charged heavy attack, spinning throw/return
- Thunder God: lightning-strike, chain-lightning, thunder-god-strike, heavenly-thunder-burst
- Solaris: dawn, one-point, heavenly-wheel, judgment
- Moonlit: passive, yugwang, moon-flash, moon-shadow
- Frozen Sword: current T3 Ice identity, not restoration of the removed T2 Ice matrix

Old Paper elemental special entries that are not current T3:
- Burning/Flame Sword -> T2
- Flowing Water Sword -> T2
- Wind Cutting Sword -> T2
- Wind Archer Bow -> T2
- Earth Special Sword, Burning Bow, Earth Heavy Bow, Freezing Bow, Dark Energy Sword/Bow -> removed from current baseline

## 5. T4 Endgame

### CURRENT_LOCKED_NOT_IMPLEMENTED

**무극검**
- socket forms: Fire / Water / Wind / Earth / Lightning
- unsocketed form is a complete balanced weapon
- Fire: attack/range/splash up, speed/mobility/sustain down
- Water: speed/mobility/sustain up, lower single-hit damage
- Wind: sword-wave and balanced spatial control
- Earth: extreme impact/knockback, slow, miss penalty, hunger cost
- Lightning: burst speed/mobility/damage, low sustain

**천궁**
- rapid / heavy / scatter / arc modes
- left click cycles mode, right click draws/fires
- exact projectile/VFX physics deferred

**천서**
- beam / projectile / area / construct output modes
- geometric transcendent magic-engineering identity
- exact inputs/stats/VFX deferred

**세계수 활**: `DEFERRED / OPTIONAL`
- seed -> growth -> world tree -> absorb/reclaim concept
- may become T3 or optional T4

Old T4 brainstorm concepts are not the authoritative roster.

## 6. Custom enchantments

Paper final retained 20 genuinely custom native enchantments:

1. Blade Chain
2. Light Greatsword
3. Laser Arrow
4. Axe Heavy Strike
5. Titan's Wrath
6. Skill Protection
7. Rolling Landing
8. Wind Arrow
9. Fire Arrow Rain
10. Crossbow Barrage
11. Mining Bonus Drop
12. Area Excavation
13. Auto Replant
14. Auto Smelt
15. Chain Logging
16. Treasure Finder
17. Multi Catch
18. Elytra Launch
19. Precision Flight
20. Explosive Mace

Status: `REVIEW_REQUIRED`. They are valid recovered content and must remain in the master inventory until individually retained/changed/retired.

Twelve historical custom copies of vanilla enchantments were already migrated back to native Minecraft enchantments and are `HISTORICAL_REFERENCE`, not custom content to recreate.

## 7. Utility equipment

### Content Freeze v3 recovered roster

1. Climbing Gloves
2. Miner Lantern
3. Portable Potion Injector
4. Glider
5. Oxygen Tank
6. Sleeping Bag
7. Jet Boots
8. Flare
9. Buoyancy Capsule
10. Return Crystal
11. Portable Furnace
12. Mining Drill

### Current baseline roster

1. Miner Lantern
2. Sleeping Bag
3. Oxygen Tank
4. Jet Boots
5. Flare
6. Return Crystal
7. Portable Furnace
8. Mining Drill
9. Glider
10. Grappling Hook

### Exact recovered contracts

**Climbing Gloves**: `REMOVED_FROM_CURRENT_BASELINE`
- wall climbing, no stamina
- durability -1 per block climbed
- Iron + Leather direction

**Miner Lantern**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- held or helmet-integrated dynamic light
- Paper prototype used LIGHT level 15, air positions only, no block overwrite
- preserve helmet data
- Titanium + Fire Heart-family material

**Portable Potion Injector**: `REMOVED_FROM_CURRENT_BASELINE`
- right click consumes first stored finished potion instantly
- left click one-row storage UI
- no manufacturing, battery, or fuel
- Iron + iron pieces + Redstone

**Glider**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- pre-Elytra glide
- no powered ascent/propulsion
- gravity/velocity control
- light metal + paper direction

**Oxygen Tank**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- chestplate integration
- tank oxygen consumed before vanilla air
- automatic recharge in air
- oxygen and armor durability are separate state
- Copper + Titanium

**Sleeping Bag**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- sleep without changing respawn
- reusable, not a villager bed
- Wool + Wood

**Jet Boots**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- standalone boots around Iron defense
- normal enchants
- stacked air jumps, no fuel
- ground/Slime contact resets stack
- Lv1 crafted, higher levels via recipes
- function/level can be transferred to other boots
- max level/stack balance deferred
- Titanium + Copper + Leather + Fire Heart-family material

**Flare**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- consumable, bow-like charge
- charge controls velocity/range
- moving projectile carries dynamic light
- Iron + Gunpowder + luminous/structural material

**Buoyancy Capsule**: `REMOVED_FROM_CURRENT_BASELINE`
- one-use emergency underwater ascent
- ends at/out of water
- name/recipe deferred

**Return Crystal**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- very expensive one-use long-range return
- channeling, cancelled by movement/damage
- consumed only on success
- destination unresolved
- Diamond + Lapis + spatial/fantasy material

**Portable Furnace**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- food only, smoker-like speed
- fuel consumed only while cooking
- stack size 1 is intentional inventory tradeoff
- may be more fuel efficient than furnace/smoker
- old draft: Stone 4 + Iron 1

**Mining Drill**: `CURRENT_LOCKED_NOT_IMPLEMENTED`
- endgame pickaxe role
- target near Deepslate instant-mine boundary
- Efficiency prohibited
- Fortune/Silk Touch/Unbreaking allowed
- Coal Block only, max 128, inserted fuel cannot be removed
- Unbreaking reduces fuel consumption
- 3x3/vein mining not locked
- Titanium + Tungsten + Copper + Redstone + optional endgame material

**Grappling Hook**: `DEFERRED`
- current roster retains it
- mechanics/physics/recipe not yet locked

Removed-from-baseline does not mean permanently retired unless a later decision says so.

## 8. Coating

### CURRENT_LOCKED_NOT_IMPLEMENTED

- Armor only
- functional growth, independent of tier/enhancement/enchantment
- smithing-template-style application
- dedicated coating items/templates and recipes
- one shared placeholder visual pattern is sufficient initially
- maximum 3 coatings per armor ItemStack
- 0 -> 1 -> 2 -> 3, then locked
- duplicate material coating normally prohibited
- only logical conflicts require incompatibility rules
- current removal/replacement unavailable
- future replacement is intended after a removal item/mechanic exists

Historical Freeze v3 expected no general slot cap. This is `CHANGED`: current cap is 3.

### Full recovered armor matrix: 13 material families x 4 slots = 52 cells

**Copper**
- Helmet: repeated attacks on same target charge; 3 stacks triggers lightning and resets.
- Chestplate: stores combat damage as charge, later releases a large lightning attack through melee/ranged attack.
- Leggings: hit gives charge, stable max 5; +5% movement per stack, non-compounding; overflow resets and damages nearby enemies electrically.
- Boots: on hit 50% chance to reduce that damage 10%, Speed II 2s, about 5s cooldown.

**Quartz**
- Helmet: always critical; 40% chance critical damage +20%.
- Chestplate: reflect 20% of received melee damage.
- Leggings: chance to nullify/greatly reduce projectile damage; redirect toward attacker when feasible.
- Boots: landing after jump creates short precision state; first attack gets stronger critical damage.

**Amethyst**
- Helmet: within 10 blocks, Warden/Sculk-detectable vibration/game events show vanilla vibration from event to wearer, wearer-only, with Sculk detection sound.
- Chestplate: on hit immediately beams attacker with Amethyst laser, ranged damage + knockback.
- Leggings: ranged hit emits resonance wave toward attacker; Sonic Boom-like presentation allowed.
- Boots: sufficiently high landing emits resonance shockwave and knocks nearby enemies away.

**Lapis Lazuli**
- Helmet: XP gain +30%; enchantment/skill cooldown -15%.
- Chestplate: supports equipped enchantment functions; per-enchant table still deferred.
- Leggings: while running at low HP, consume XP to heal.
- Boots: replace part of fall damage with XP cost; normal fall damage if no XP.

**Emerald**
- Helmet: increased mob drops; permanent Fortune III-equivalent ore effect.
- Chestplate: lethal damage leaves HP 1 and grants strong absorption/protection; ~10 min cooldown.
- Leggings: enhanced Hero of the Village and future village-content effects.
- Boots: first entry into a new chunk can grant luck stacks, consumed for extra reward rolls on natural loot/mob kills/ore mining.

**Gold**
- Helmet: expands Piglin/Villager trade UI/interactions.
- Chestplate: melee attacker gets about 1.5s Slowness V.
- Leggings: movement +15%, attack speed +10%.
- Boots: ignore block movement slowdown such as Soul Sand/Cobweb; lava movement treated around water movement level, exact lava movement/damage rules deferred.

**Diamond**
- Helmet: projectile/headshot damage -35%, explosion -25%.
- Chestplate: each hit grants 4s incoming damage -6%, max 4 stacks (-24%); 4s without hit resets.
- Leggings: all knockback immunity, 100% knockback resistance.
- Boots: convert a large part of fall damage into durability loss; old baseline example ~70% reduction.

**Netherite**
- Helmet: increased damage to burning enemies.
- Chestplate: fire/lava immunity; situations that would cause fire damage instead heal; attacks may ignite targets, synergizing with Helmet.
- Leggings: lava movement approximately water-like including horizontal and vertical control.
- Boots: Hot Foot, temporary Obsidian over lava that later restores original lava. Alternative B-plan retired.

**Iron**
- Helmet: additional melee reduction while facing attacker; old baseline 15%.
- Chestplate: 10% chance to fully negate melee hit.
- Leggings: knockback resistance +30%, ranged damage -15%.
- Boots: accurately land on enemy from 3+ blocks and transfer own fall damage to target, Mace-impact presentation.

**Redstone**
- Helmet: after taking damage, next melee attack becomes fully charged; ~4s cooldown.
- Chestplate: low-HP automatic overclock, ~4s major movement/attack/mining speed boost, long cooldown.
- Leggings: kill increases attack/movement speed; later kills refresh duration; max stacks exist.
- Boots: special ground input gives strong vertical jump, short cooldown.

**Titanium**
- Helmet: strongly reduces high-speed collision damage from Elytra/Riptide/etc, plus slight defense.
- Chestplate: greatly reduces movement slowdown while aiming/using food/shield/items.
- Leggings: better sprint efficiency/base movement; airborne jump input gives forward dash-leap.
- Boots: special input gives short fast directional dash on ground or air.

**Tungsten**
- Helmet: frontal sprint collision deals physical damage, large knockback, short stagger; wearer keeps sprint.
- Chestplate: very strong against small single hits below a threshold.
- Leggings: after sustained sprint, first melee attack gains momentum-scaled damage + large knockback.
- Boots: Sneak preparation anchors wearer: near-total knockback immunity, resistance to explosion/water/bubble/piston displacement, extreme damage reduction, no movement/jump; release Sneak to end.

**Mithril**
- Helmet: greatly shortens harmful status durations.
- Chestplate: greatly reduces potion/magic/sonic/custom magical damage.
- Leggings: when timed positive buff expires, retain weakened echo briefly.
- Boots: landing under condition/cooldown creates temporary magic circle; allies gain cooldown recovery/positive-effect support, enemies' magic/special effects weaken.

Coating-excluded:
- Aquamarine
- Adamantium
- Etherium
- Luminite (explicitly retired from coating)

## 9. Materials and mining

### Current ore roster: CURRENT_IMPLEMENTED

1. Titanium
2. Tungsten
3. Mythril
4. Adamantium
5. Etherium

Historical transitions:
- Etherium: candidate -> active final ore
- Aquamarine: candidate -> removed from final ore roster
- Luminite: coating retired and removed from final ore roster
- Aluminum: not required final material

Worldgen:
- Titanium: Overworld Y -16..80, peak 32
- Tungsten: Overworld Y -48..32, peak -16
- Mythril: Overworld Y -64..0, peak -48
- Etherium: Overworld Y -32..64, peak 16
- Adamantium: Nether, Ancient-Debris-like

Supply:
- Titanium/Tungsten: rarer than Copper, more common than Iron
- Mythril/Etherium: rarer than Diamond, about Diamond attempt rate x0.67, vein max 6
- Adamantium: Ancient-Debris-like

Drops:
- Titanium/Tungsten: raw -> smelt -> ingot, Fortune applies, Silk Touch ore
- Mythril/Etherium: direct material, Fortune applies, diamond-like XP, no smelting
- Adamantium: no Fortune quantity bonus, smelt to material

Old fragment economy is `RETIRED`:
fire/water/wind/earth/ice fragments and soul-stone currency loops must not return as progression currency. Elemental material identities such as Fire Heart may still be reused as crafting ingredients.

## 10. Fishing

### CURRENT_IMPLEMENTED

Vanilla casting/hook/water/wait/Lure/non-catch behavior stays vanilla.

Custom challenge begins only on valid biting catch:
- server authoritative deterministic ping-pong timing
- 20-tick half-cycle
- inclusive center 0.42-0.58
- timeout 160 ticks
- rod-use confirmation
- no A/D steering, fish resistance, or rarity difficulty

Resolution:
environment/depth -> Reel -> species/depth pool -> Minnow gate -> valid pool -> Crankbait grade bias -> optional Luck -> server result

Modules:
- Reel: depth/discovery access only
- Minnow: hard capture gate
- Crankbait: grade probability modifier after valid pool
- each 5 grades and durability

Fish ItemStacks persist species, grade, length.

`CHANGED`: Freeze v3 used left-click for module UI; current implementation uses Shift + Right Click.

## 11. Alchemy

### Eight recovered production effects

- Vampire: heal source 5% of valid final dealt damage, capped at max HP
- Berserk: outgoing x1.10, incoming x1.05
- Corrosion: incoming x1.10
- Frostbite: movement attribute -0.15 + Slowness I with owned cleanup
- Shock: 1200 ticks baseline; first/interval pulse 320 ticks; 1.0 damage; 20-tick movement root per valid pulse
- Bleed: 1.0 effect damage every 20 ticks
- Vulnerability: incoming x1.20
- Necrosis: MAX_HEALTH -10%

Common Paper baseline: 1200 ticks, amp 0, one stack, refresh duration, remove on death/logout/world change. These are recovered baselines, not automatically final Fabric balance.

### Ten catalysts

- Redstone: duration transformation; Paper 100% multiplier is effectively no-op and balance pending
- Glowstone Dust: amplifier +1
- Gunpowder: Drink -> Splash
- Dragon Breath: Lingering delivery
- Fermented Spider Eye: explicit Vulnerability -> Corrosion inversion
- Sculk Catalyst: bounded splash propagation chain
- Echo Shard: delayed repeat on affected targets
- Slime Ball: bouncing splash
- Wind Charge: directional splash
- Fire Charge/custom fireball: small-fireball delivery, radius 4, no terrain damage/ignition

### Historical multi-material recipes: HISTORICAL_REFERENCE

- Vampire: abundance essence + Spider Eye + processed garlic concentrate
- Berserk: abundance essence + Blaze Powder + processed chili extract + Sugar
- Corrosion: corrosion essence + Slime Ball + Rotten Flesh
- Frostbite: corrosion essence + Snowball + Ice
- Shock: corrosion essence + Copper Ingot + Redstone
- Bleed: corrosion essence + Cactus + processed chili extract
- Vulnerability: corrosion essence + Rotten Flesh + Spider Eye
- Necrosis: corrosion essence + Wither Rose + Spider Eye

The old multi-material/Farming-derived architecture is retired.

### Current Stage 10 Brewing mappings

Paper's final Brewing Stand transition architecture existed, but its final transition table was empty. The current authorized native mappings are:

- Thick Potion + Spider Eye -> Vampire
- Thick Potion + Blaze Powder -> Berserk
- Thick Potion + Slime Ball -> Corrosion
- Thick Potion + Snowball -> Frostbite
- Thick Potion + Copper Ingot -> Shock
- Thick Potion + Cactus -> Bleed
- Thick Potion + Rotten Flesh -> Vulnerability
- Thick Potion + Wither Rose -> Necrosis

Catalysts are subsequent brewing transitions. Shock is the currently prepared first vertical slice.

## 12. Mobs

### CURRENT_IMPLEMENTED

- Bomb Zombie
- Leap Zombie
- Mining Giant

Mining Giant is `CHANGED`: Paper falling-rock attack was explicitly rejected. Fabric uses Tunnel Breaker, a telegraphed fixed-direction corridor cleave with no terrain changes.

### Recovered Paper mob inventory: REVIEW_REQUIRED unless otherwise classified

- golden_bulwark
- mire_shaman
- frost_blaze
- explosive_breeze
- riptide_drowned
- lava_cube
- stone_armored_zombie
- fake_explosion_creeper
- swapping_witch
- charging_zombie
- rapid_shooter
- splitting_creeper
- elite_zombie_revival profile

Exploration/Outpost-bound historical mobs:
- shield_raider
- crossbow_raider
- charger_raider
- banner_raider
- spike_evoker
- ravager_rider

Their old Exploration spawn loop is retired. Their identities are not automatically permanently deleted and require explicit review before reuse.

Old RPG level/EXP mob scaling and elemental-fragment progression drops are retired.

## 13. Bosses and complex combat

### Skeleton King: CURRENT_IMPLEMENTED

Recovered Paper baseline:
- summon guards: cooldown 200, 3 guards, radius 4
- bone barrage: cooldown 120, 8 arrows, spread 30
- weakness curse: cooldown 160, Weakness 100 ticks amp 0

Fabric adds native boss bar, persistence, ownership/cap/lifecycle, reload support, orphan-guard grace/reconciliation, without a generic BossEngine or phase framework.

### Thousand Eyes: PROTOTYPE_REFERENCE / REVIEW_REQUIRED

Paper OP-only prototype, not production spawn content:
- central laser
- gateway burst
- scatter lasers
- path dash
- central Ender Eye + display/satellite visual assembly

### Gateway Boss: PROTOTYPE_REFERENCE / REVIEW_REQUIRED

Paper isolated test harness:
- seven gateways
- payload/actor caps
- Arrow, Trident, fireballs, Wither Skull, Shulker Bullet, Wind Charge, End Crystal, Sonic Boom, Dragon Breath, Flame Stream, Beam
- melee/drop/lunge/orbit weapon patterns
- some higher patterns remained spec gaps

Do not auto-promote prototypes into Fabric production, but do not lose them from the master inventory.

Custom Wither/Dragon sessions, contribution/reward/cooldown/settlement/mailbox systems are retired. Vanilla bosses remain vanilla.

## 14. Exploration/world/structures

Standalone Exploration progression and its generic event engine are `RETIRED`.

Historical Paper content included:
- structure registry/selection
- Desert Pyramid
- Outpost
- Shipwreck
- Ocean Monument
- raids/events/persistence/rewards

Do not restore these as a mandatory RPG exploration system. Ocean Monument's final Paper model was dormant/historical.

This does **not** ban future structures or world content. New world content should be independent vanilla expansion rather than revival of the old Exploration progression framework.

## 15. Farming, cooking, professions

### RETIRED

Custom Farming progression:
- crop quality/profile/promotion
- hoe promotion
- delivery progression
- abundance/favor/essence economy
- farming stat tokens
- farming hub/persistence/profession progression

Custom Cooking system is retired.

Old profession/class RPG systems are retired:
- profession levels
- class restrictions/stats
- weapon specialization progression
- proficiency
- mana
- skill tree
- stat points

Vanilla farming remains. Future crops/foods/materials may be introduced independently as vanilla extensions.

## 16. Quests, shops, economy

### RETIRED

- Quest runtime/registry/progress/rewards
- Coin
- Magic Stone currency
- fragment currencies
- progression shops
- shop-based gear progression
- custom repair economy

Vanilla durability/repair remains.

## 17. Historical progression loop vs current

Historical:
vanilla activity -> fragments -> crafting -> upgrade stones -> enhancement -> hunting/boss -> promotion/magic stones -> promotion/enchantments

This unified loop is retired.

Current:
Minecraft play -> vanilla resources/XP/loot/crafting -> optional Enhancement/native Enchantment -> Elemental/Special/Endgame content

Content categories are not required to share one currency/progression spine.

## 18. Consolidated explicit retirement list

Systems:
- Coin / Magic Stone / fragment currency loops
- Promotion and random options
- custom repair economy
- Class / profession-level / player-stat RPG
- Mana / proficiency / skill tree
- Quest runtime
- Farming progression
- Cooking
- Mining progression/GUI
- Exploration progression/event engine
- mandatory Dungeon progression
- custom Wither/Dragon settlement
- giant custom crafting/progression UI
- generic RPG mob scaling

Roster/design:
- broad every-element-every-weapon T2 matrix
- Fire Bow, Earth Sword/Bow, T2 Ice Sword/Bow, old Dark/Magic Sword/Bow from current T2 baseline
- Luminite coating
- Mining Giant falling-rock ability
- Netherite Boots coating B-plan
- active custom copies of vanilla enchantments
- old non-authoritative T4 brainstorm roster

Removed from current baseline but **not** proven permanently retired:
- Climbing Gloves
- Portable Potion Injector
- Buoyancy Capsule
- Aquamarine ore candidate
- Luminite ore candidate

## 19. Consolidated major changes/reclassifications

1. Promotion-heavy equipment growth -> T1/T2/T3/T4 categories with independent enhancement/coating/enchantment.
2. Broad elemental carrier matrix -> six T2 items.
3. Poseidon Spear -> T3 Special.
4. Dark Energy Sword/Bow -> Staff.
5. T2 Ice removed while T3 Frozen Sword survives.
6. Chance/Coin enhancement -> deterministic native anvil.
7. Coating unlimited-slot direction -> maximum three per armor item.
8. Coating replacement -> future-supported, currently unavailable until removal mechanic exists.
9. Freeze utility 12 -> current baseline 10; three removed, Grappling Hook added/deferred.
10. RPG mining -> vanilla mining + five custom ores.
11. Etherium candidate -> active final ore.
12. Fishing module UI left-click -> Shift + Right Click.
13. Conceptual fishing timing -> fixed server-authoritative timing.
14. Multi-material custom Alchemy -> vanilla Brewing Stand one-ingredient base transitions.
15. RPG mob scaling -> fixed content-specific contracts.
16. Mining Giant falling rocks -> Tunnel Breaker.
17. Standalone Exploration -> retired; future world content independent.
18. Abstract endgame -> locked baseline of 무극검 / 천궁 / 천서.

## 20. Master backlog that must not disappear

### Equipment
- Water Sword
- Wind Archer Bow
- Earth Mace
- Dark Energy Staff
- seven T3 Specials
- three baseline T4s
- World Tree Bow optional

### Utility
- current ten-item roster

### Coating
- full 13-family x 4-slot = 52-cell matrix

### Alchemy
- eight potion/effects
- ten catalysts

### Enchantments
- all 20 recovered genuine custom enchantments until explicit disposition

### Mobs
- all recovered Paper mob candidates until explicit disposition
- Exploration-bound mobs retained as historical/review entries, not automatically restored

### Boss/prototypes
- Thousand Eyes
- Gateway Boss

## 21. Recovery conclusions

Most of the surviving content identity is indeed late-stage design. This is expected because the final Paper refactor intentionally removed large horizontal RPG systems and old progression dependencies before the Fabric migration. What survived that cleanup, or was locked in the Stage 8 Content Freeze, is disproportionately close to the current design.

Therefore:
- late Paper/Freeze documents are high-value recovery sources;
- final Paper source alone is insufficient for content history;
- deleted systems must not be mistaken for accidentally missing content;
- removed-from-baseline items must not be mislabeled as permanently retired;
- future audits should use this ledger as the master reconciliation layer before implementation decisions.

### Stale Stage 10 audit note

The earlier Stage 10 audit stated that the full Freeze v3 coating matrix could not be recovered. That statement is now stale: the v3 archive has been directly decoded and the complete 13-material, 52-cell armor coating matrix is recovered above.
