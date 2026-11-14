# Cumulative live test command

Operator-only helper command for cumulative live validation. It deliberately calls the existing gameplay identities rather than implementing parallel test-only mob/equipment behavior.

## Give kits

- `/mmtest give enhancement` - 64 Enhancement Stones, Iron Sword, Iron Pickaxe, Iron Chestplate.
- `/mmtest give equipment` - Flame Sword and Wind Sword.
- `/mmtest give fishing` - vanilla Fishing Rod plus Reel, Crankbait and Minnow grades 1-5.
- `/mmtest give materials` - Stage 6 processed/raw custom materials.
- `/mmtest give shock` - three Thick Potions, Copper Ingots, and one ready-made Shock Potion; use
  the supplies to validate the actual Brewing Stand recipe separately from direct effect testing.
- `/mmtest give all` - all kits above.

Worldgen itself must still be tested in newly generated chunks; the materials kit does not validate ore placement.

## Spawn helpers

- `/mmtest spawn bomb`
- `/mmtest spawn leap`
- `/mmtest spawn mining_giant`
- `/mmtest spawn skeleton_king`

Zombie helpers create a normal Minecraft Zombie and assign the existing MoreMinecrafts variant state. Skeleton King uses the same existing Skeleton King state used by the legacy `/summonskeletonking` command. These commands therefore bypass natural spawn probability only, not the actual combat implementation.

## Permissions

All `/mmtest` commands require game-master/operator permission.

## Scope

This is validation infrastructure only. It does not alter natural spawn probabilities, progression, recipes, balance, or ordinary player access.

## Grand Unified 2026-11-12 helpers (existing commands; no code change)

Used by the ACT-03 / ACT-04 / ACT-05 checklist at the end of [`LIVE_VALIDATION_BACKLOG.md`](LIVE_VALIDATION_BACKLOG.md). All require operator permission. They exist in the baseline; this section only documents them.

### `/mmtomb` — Tomb Guardian encounter (royal chest PRIMARY)

- `/mmtomb status <x y z>` - prints `DORMANT`, `ACTIVE guardian=… presence=…` or `DEFEATED`, plus `sealed=true|false`, for the tomb whose anchor is the PRIMARY chest's **left half** position.
- `/mmtomb end <x y z>` - removes the bound guardian without a kill and releases the binding (retry allowed). Not a defeat.
- `/mmtomb start <anchor> [spawn]` - stands in for the royal-chest interaction (spawns the guardian) without needing a registered chest; does not exercise chest registration.
- `/mmtomb spawn` - an unbound guardian for combat checks (no tomb, completes nothing). `/mmtomb attack melee|slam|statue` forces an attack from the nearest idle guardian.

### `/mmskytower` — Sky Tower registry

- `/mmskytower register <x> <y> <z> <rotation 0-3>` - registers an already placed `moreminecrafts:sky_tower/full_01` (absolute integer origin; `~` is not accepted) and configures its 8 normal vaults and the rooftop vault. Fails unless all 16 trial spawners and 8 vaults are present and loaded.
- `/mmskytower status` - lists registered tower ids (`dimension|x,y,z|rotation`) with spawner completion masks.
- Controlled placement: `/place template moreminecrafts:sky_tower/full_01 <x> <y> <z> [none|clockwise_90|180|counterclockwise_90]`, then register with rotation `0|1|2|3` respectively.

### Vanilla commands the checklist relies on

`/locate structure moreminecrafts:st01_pyramid` (and `moreminecrafts:sky_island_*`), `/forceload add`, `/loot give @s loot <table>` (T2 chest accessory pools), `/damage @s <n>` (HP thresholds), `/effect give`, `/gamerule keep_inventory`, `/gamerule natural_health_regeneration` (use tab-completion if the 26.x spelling differs).

## Ancient Crystal Guardian model POC (`/mmcrystalguardian`)

Operator-only, visual/animation verification of the converted Blockbench model. The test entity `moreminecrafts:ancient_crystal_guardian_test`
has no AI, no attack, no drops, no natural spawn and no spawn egg, and is not saved with the world. It is **not** a content decision.

- `/mmcrystalguardian spawn [pos]` (or `/summon moreminecrafts:ancient_crystal_guardian_test`)
- `/mmcrystalguardian walk [ticks] [speed]` - forced straight walk (defaults 100 ticks, speed x1.0, range 0.1-3.0)
- `/mmcrystalguardian stop` / `reset` - stop walking / also cancel pending loop swings
- `/mmcrystalguardian swing` - one visual right-arm swing (ignored while one is running; no damage, no target search)
- `/mmcrystalguardian loop <count> [gap]` - repeated swings with `gap` ticks of rest (default 10)
- `/mmcrystalguardian status`

Commands act on every test guardian in the executing dimension. Real-client capture: `scripts/crystal_guardian/run_client_capture.sh`.

