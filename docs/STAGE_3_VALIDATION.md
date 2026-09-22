# Stage 3 enhancement vertical slice

Stage 3 deliberately implements one operation only: an unenhanced Iron Sword in the first
vanilla-anvil input plus an Enhancement Stone in the second input produces a level-one enhanced
copy. The anvil charges one level and consumes one stone through its normal result-take path.

`EnhancementOperation` owns eligibility, costs, the deterministic state transition, and the probe
attack-damage value. `AnvilMenuMixin` is only an adapter to that operation. The persistent and
network-synchronized `moreminecrafts:enhancement_level` integer Data Component is the sole source
of enhancement state. No custom packet or custom persistence format is used.

## Manual runtime checklist

Run these checks in a new dedicated-server world and repeat the presentation checks in a client
world:

1. Give the player an Iron Sword, at least two Enhancement Stones, and at least two XP levels.
2. Put the sword in the anvil's left input and the stones in its right input. Confirm the output is
   available at a cost of exactly one level.
3. Take the output. Confirm one stone and one level were consumed, the sword shows
   `Enhancement +1`, and its attack-damage tooltip is one point higher.
4. Attack the same type of unarmored target with fresh and enhanced Iron Swords and confirm the
   enhanced sword deals one additional point of server-observed damage.
5. Confirm a non-Iron Sword and an Iron Sword paired with any other material do not enter this
   path.
6. Add a custom name, durability damage, and a vanilla enchantment to the probe sword before
   enhancement. Confirm all three remain on the output.
7. Separately verify vanilla rename, ordinary repair, and enchanted-book operations still work.
8. Move the enhanced sword between inventory and container slots, then drop and pick it up.
   Confirm its level and damage bonus remain.
9. Log out and reconnect, then restart the dedicated server and reconnect. Confirm the level,
   tooltip, and damage bonus remain after each step.
10. Observe both client and dedicated-server logs for mixin errors. No MoreMinecrafts gameplay
    packet should be registered or sent.

The save/reload, reconnect, and container-sync checks exercise Minecraft's normal ItemStack Data
Component serialization and synchronization rather than a parallel mod-owned persistence path.
