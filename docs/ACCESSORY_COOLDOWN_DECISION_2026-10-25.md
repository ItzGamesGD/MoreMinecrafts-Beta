# Accessory cooldown lifecycle — approved 2026-10-25

Owner-approved policy B for accessory abilities:

- Track remaining server ticks per player and stable ability type, shared by every item of that type.
- Decrement once per server tick while the player is online, including when the accessory is unequipped.
- Preserve remaining ticks through unequip, death, respawn, dimension travel, disconnect, reconnect, saves, and server restart. Offline time does not reduce them.
- Different ability types have independent cooldowns. Activation and cooldown mutation are server-authoritative.

This decision changes cooldown lifecycle only. Ability triggers, durations, enhancement values and other mechanics remain governed by the October 16 and 17 accessory decisions. Proposed values and unresolved ability-specific behavior in those records are not approved here.

The Wave 7B reconciliation instruction of 2026-10-25 re-confirmed this same lifecycle as its "Decision A"; the content is identical, only the option label differs.

## Ability trigger decisions — approved 2026-10-25

These refine the October 16 triggers. Durations, reductions and cooldown values remain the October 17 enhancement tables.

- **Deep Sea Pearl — motionless.** Motionless means no horizontal movement input (no forward, backward, left or right key in the player's last server-received input). Crouching underwater with no horizontal input for 2 consecutive seconds (40 ticks) activates it. Displacement from water currents, sinking or other external movement does not reset the count. No speed threshold and no same-block/same-position requirement.
- **Ring of Life — re-arm.** Activation requires health ≤25% of max, an expired cooldown, and that the ring is armed. Activating disarms it; it re-arms only when health rises above 25% (exactly 25% stays low). Staying continuously below the threshold does not reactivate as cooldowns expire. The disarmed state is per player and persists through unequip/re-equip, save, reconnect and server restart; respawn at full health re-arms it.
- **Ember Ring — first hit.** The triggering fire-damage hit is not reduced. Protection starts after that hit and only reduces later fire damage during the protection window; the 30-second cooldown starts at the trigger.

**Open (not decided):** if the ring is armed and health drops to ≤25% while its cooldown is still running, the decision does not say whether the ring should fire when the cooldown expires during that same low-health period. The current implementation fires once in that case (the ring is still armed); this awaits owner confirmation.
