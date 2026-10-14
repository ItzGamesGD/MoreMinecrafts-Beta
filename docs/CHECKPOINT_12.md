# Checkpoint 12 — T2 Equipment Completion / Prototype Replacement

> Base checkpoint: `stage-11/checkpoint` at `1b95a0cbb2435fbf7ae5dfa1b562674e7a6cf833`.
> Gameplay is server authoritative. Numeric damage, range, displacement, and all cooldowns except Fire's
> locked 30 seconds are **PROVISIONAL / LIVE PENDING**, not final balance.

## Implemented

- **Fire / 불꽃검:** a successful primary hit emits a small ember effect. Player-owned runtime progress
  procs exactly every 10 successful primary hits and resets, producing a broad horizontal flame sweep.
  Right click invokes a stronger rising diagonal flame wave; its cooldown is 600 ticks (30 seconds).
  The former every-hit radial splash and hard-coded direct-damage proxy are removed.
- **Water / 흐르는 물의 검:** retains fast attack speed and a two-health-point provisional kill heal.
  Right click starts a server-owned launch, directed dive, wait-for-ground, resolve, cleanup sequence.
  The vertical water pillar is placed approximately two blocks ahead only after an observed airborne-to-
  grounded transition; it never resolves at a preselected air position.
- **Wind / 바람을 가르는 검:** a successful primary hit emits a cheap wind effect. Every 10 hits produces
  a broad damaging AoE with per-target Sweep Attack effects. Right-click Wind Blade is the sole active;
  the unused Wind Step prototype has been removed.
- **Lightning / 번개의 검:** successful primary hits build player-owned server-runtime charge. Hit five
  adds concentrated lightning damage and resets the charge. There is no clock or timeout decay. Right
  click performs the short movement and concentrated forward hit volume of Lightning Slash.
- **Earth / 대지의 철퇴:** only `MaceItem.canSmashAttack` successful Earth Mace hits emit a narrow forward,
  non-destructive fracture that adds damage and brief Slowness. Grounded right click emits Earth Shatter,
  a broad forward cone that damages and displaces multiple valid targets without changing blocks.
- All five completed weapons use the item-use/right-click route. Target selection, PvP/team filtering,
  counter advancement, movement, damage, cooldowns, and particles are resolved on the server.
- Enhancement catalog profiles remain ID-based: elemental swords use `SWORD`, Wind Bow uses `RANGED`,
  and Earth Mace uses `MACE`. Ability/passive damage is explicit and does not reapply enhancement attack
  modifiers. No Staff profile is registered.

## Provisional lifecycle and balance

Fire, Wind, and Lightning counters live in server memory by player UUID. They survive weapon switches,
death, and same-process reconnects, reset on their own proc, and reset when the server process restarts.
This deliberately narrow policy creates no save format while persistence decisions remain unspecified.
Only the item successful-hit callbacks advance counters; ability and passive `hurtServer` calls do not.

Water is cancelled on death, disconnect, an 80-tick safety timeout, or server shutdown. Its cooldown starts
when launch succeeds, not on landing. Current damage, healing, movement, state timing, area, and cooldown
are test defaults. Fire/Wind passive dimensions and damage, Wind Blade values, Lightning values, and both
Earth abilities' values are likewise provisional.

## Deferred and intentionally omitted

- **Wind Archer's Bow — INTENTIONALLY OMITTED:** the registered vanilla-like test carrier remains, with no
  tracking, acquisition cone, auto-aim, steering, turn rate, or active homing mode.
- **Dark Energy Staff — DEFERRED:** its existing temporary right-click carrier/beam remains provisional
  live-test scaffolding only. It is not a finalized combat system and is excluded from Enhancement.
- Recipes, survival acquisition, bespoke models/textures/animations/SFX, polished VFX, and final balance
  remain separate stages. The current vanilla particles are functional placeholders.

## Automated and live validation

Pure tests lock the 10/10/5 counter thresholds and reset behavior, Lightning's lack of a time input,
Water's airborne-before-landing transition, Earth lane/cone geometry, and Enhancement classifications.
Visual hierarchy, collision feel, multiplayer motion, effects, damage exactly-once behavior, and balance
remain **LIVE PENDING** in `LIVE_VALIDATION_BACKLOG.md`.
