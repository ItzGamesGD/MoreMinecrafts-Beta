# Accessory cooldown lifecycle — approved 2026-10-25

Owner-approved policy B for accessory abilities:

- Track remaining server ticks per player and stable ability type, shared by every item of that type.
- Decrement once per server tick while the player is online, including when the accessory is unequipped.
- Preserve remaining ticks through unequip, death, respawn, dimension travel, disconnect, reconnect, saves, and server restart. Offline time does not reduce them.
- Different ability types have independent cooldowns. Activation and cooldown mutation are server-authoritative.

This decision changes cooldown lifecycle only. Ability triggers, durations, enhancement values and other mechanics remain governed by the October 16 and 17 accessory decisions. Proposed values and unresolved ability-specific behavior in those records are not approved here.
