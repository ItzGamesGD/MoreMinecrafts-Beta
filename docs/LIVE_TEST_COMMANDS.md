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
