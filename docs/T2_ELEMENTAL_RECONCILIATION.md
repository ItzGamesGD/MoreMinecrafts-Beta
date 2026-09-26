# Stage 11 T2 Elemental reconciliation

> **Checkpoint 12 overlay:** `CHECKPOINT_12.md` supersedes this implementation-preparation snapshot where
> runtime status differs. Fire, Water, Wind Sword, Lightning, and Earth contracts are now implemented with
> provisional values; Wind Bow is intentionally omitted and Staff remains deferred scaffolding.

> **Status:** implementation-preparation record.  The roster statements marked **LOCKED** come from
> `EQUIPMENT_PROGRESSION_FINALIZATION.md`; Paper behavior is identity evidence, not authority over
> later Fabric decisions.  No row in this document authorizes a recipe or an unresolved balance value.

## 1. Authoritative roster and source mapping

The current **LOCKED** roster is seven items across six elemental families:

| Current family and carrier | Paper identity retained for consideration | Stage 11 disposition |
|---|---|---|
| Fire — 불꽃검 — Sword | Fire sword carrier and fire attack identity | **CHANGED / PROTOTYPE ONLY:** the Fabric item exists, but its always-on radial splash and current Flame Cleave are not the revised passive and active. |
| Water — 흐르는 물의 검 — Sword | Fast attack speed, small heal on kill, and a temporary speed bonus | **KEPT IN PART / DEFERRED:** speed and heal identity remain; the old speed bonus is not automatically restored. The rise, aimed dive, and water-pillar active needs its unresolved movement, impact, damage, radius, and cooldown decisions. |
| Wind — 바람을 가르는 검 — Sword | Fast sword and wind attack identity | **CHANGED / PROTOTYPE ONLY:** Wind Blade and Wind Step remain useful experiments, but two actives conflict with the provisional one-active T2 shape. The final active is undecided. |
| Wind — 바람궁수의 활 — Bow | Wind influences the arrow; Paper reference range 12 and turn rate 0.12 | **DEFERRED:** those numbers and full auto-aim are historical references, not approved Fabric behavior. |
| Earth — 대지의 철퇴 — Mace | Earth Sword/Bow slowing and knockback; weight/displacement identity | **CHANGED / DEFERRED:** Earth Sword and Earth Bow are not roster items. Translate only the identity that fits a mace after smash and ground-control decisions are made. |
| Lightning — 번개의 검 — Sword | No matching final Paper T2 implementation was identified | **NEW / DEFERRED:** charge accumulation and 뇌격참 are provisionally defined, but every state/reset, damage, movement, hit-shape, cooldown, and VFX value remains open. |
| Dark Energy — 마기가 깃든 지팡이 — Staff | Dark Sword life steal/weakness; Dark Bow wither/weakness | **RECLASSIFIED / DEFERRED:** neither historical carrier returns. No projectile, beam, mana, or spell-mode system is authorized. |

Paper Fire Bow, Earth Sword/Bow, Ice Sword/Bow, and Dark/Magic Sword/Bow are historical evidence only.
Poseidon Spear belongs to T3. There is no T2 Ice equipment. The five material families do not yet imply
any equipment recipe, material count, boss drop, or workstation.

## 2. Current Fabric prototype audit

### Flame Sword

Current code is intentionally described as a prototype:

- `FlameSwordItem.hurtEnemy` invokes `FlameSwordEffects.splash` after a server-player hit. The effect
  runs on every hit, searches a 2.5-block radius around the primary target, excludes the attacker and
  primary target, and applies 35% of a hard-coded nine-point pre-mitigation prototype value.
- The `ThreadLocal` recursion guard prevents nested splash execution, but it is not an every-ten-hit
  counter and does not establish that a single gameplay hit was counted once across future hooks.
- `FlameCleaveAbility` is a four-block, 90-degree horizontal cone for six damage, three seconds of fire,
  and a 100-tick cooldown. It is bound to `PRIMARY` (the current F-key path), not right click.
- The target instead requires lightweight fire VFX on ordinary hits, a no-cooldown horizontal fire sweep
  on every tenth successful hit, and a substantially stronger diagonal slash on right click with a
  30-second cooldown.

The rebuilt passive must not use `PROTOTYPE_DIRECT_DAMAGE`. It should obtain damage from one explicit,
centrally testable passive-damage policy that can account for the enhanced source stack without causing
the normal Enhancement attack modifier to be applied twice. The exact policy/value is not yet approved,
so this slice does not replace the live prototype or silently rebalance it.

Reusable pieces are the server-side target/PvP filters, the cone geometry as a possible starting primitive,
the cooldown service, and the server particle/sound path. Prototype-only pieces are the radial-on-every-hit
cadence, hard-coded direct-damage proxy, current cone/value/cooldown, ignition contract, and current input.

### Wind Sword

- `WindSwordItem` currently supplies only native item behavior; it has no basic-hit VFX or passive hook.
- `WindBladeAbility` is a server-side, block-clipped, 12-block narrow path with one damage attempt per
  selected entity and Sweep Attack particles along the path. Its targeting/occlusion geometry may be
  reusable, but the whole ability must not be silently selected as the final active.
- `WindStepAbility` applies a server-selected horizontal impulse and synchronizes motion. Its eligibility,
  motion sync, and successful-cast cooldown handling may be reused if the chosen design needs movement.
  It too remains a prototype, not a finalized second active.
- The target passive is every ten successful hits, a broad wind sweep, actual AoE damage, and a Sweep
  Attack-like effect at or near each affected enemy. Neither active currently implements that passive.

Keep both prototype bindings until the single active is chosen. At that point remove the losing binding
deliberately, retain only primitives the chosen design needs, and revise the older Stage 5B live checks
rather than presenting both prototypes as the final kit.

### Ability authority and Enhancement 2.0

`EquipmentAbilityBindings.activate` resolves the main-hand stack on the server; packets choose only
`PRIMARY` or `SECONDARY`. `AbilityActivation` checks server-owned cooldown state and starts cooldown only
after successful activation. This is the correct authority boundary for future T2 actives.

`EnhancementCatalog` maps `moreminecrafts:flame_sword` and `moreminecrafts:wind_sword` by identifier to
`ELEMENTAL` / `SWORD`, avoiding a `ModItems` static initialization cycle. Future roster items should use
the same ID-based pattern with their actual profile (`SWORD`, `RANGED`, or `MACE`) once registered. A Staff
profile does not exist and must not be invented while the Staff is deferred. `EnhancementOperation` stays
UI-independent and the Anvil stays a temporary adapter.

## 3. Bounded architecture added in this slice

`SuccessfulHitCounter` is an immutable, Minecraft-bootstrap-independent state transition for every-N
successful-hit passives. One call advances one hit, reports at most one trigger, and resets progress at the
threshold. It deliberately does **not** choose storage, persistence, player-versus-target ownership, event
hooks, or a threshold for underdesigned mechanics.

For Fire and Wind, the authoritative threshold is ten. Runtime integration is deferred until the hit hook,
player-owned state lifetime, and passive damage policies can be implemented together. Merely adding a
counter without those decisions would risk client authority, duplicate counting, or an accidental balance
contract. Lightning must not adopt ten by analogy: its threshold and reset/timeout semantics remain open.

## 4. Implementation seams for deferred equipment

### Lightweight sword hit VFX

Existing effects use `ServerLevel.sendParticles`; no custom particle packet or display entity is needed for
the intended small ordinary-hit budget. A future narrow helper should accept the server level, target, and
an elemental VFX identifier, then emit a fixed small budget once from the authoritative successful-hit
hook. It should not own damage, counters, target selection, or active-skill effects. Creating that helper
before any final particle choices would only replace four direct calls with an unproven abstraction, so it
is not added here.

### Wind Archer's Bow

The existing `ProjectileWeaponItemMixin#createProjectile` seam sees the server-created projectile and the
launching weapon, and `AbstractArrow` persists both owner and fired-from-weapon state. This is a suitable
place to tag/snapshot a Wind Bow shot while preserving vanilla Power/Punch/Flame/Infinity and existing
Enhancement launch damage. Steering itself should run server-side on the arrow tick (or a dedicated arrow
subclass), use owner-safe/PvP-safe target filtering, and persist only state needed across chunk/save reload.

Before implementation decide: always-on versus active mode, acquisition cone and original-aim tolerance,
range, maximum turn rate, retarget policy, obstruction rules, valid entity/PvP rules, and behavior after
owner logout. Mild correction inside a limited cone is the recommended direction because it preserves aim
and the Paper wind identity without granting unconditional auto-aim; it is not yet a locked mechanic.

### Earth Mace

Minecraft 26.3 exposes mace behavior through `MaceItem.hurtEnemy`, `postHurtEnemy`,
`getAttackDamageBonus`, `canSmashAttack`, and its internal knockback flow. The safest future carrier is a
dedicated Earth Mace item subclass using the successful server-side hit/post-hit seam and vanilla
`canSmashAttack`, rather than globally modifying every mace or duplicating the fall-distance formula.
Enhancement's smash formula remains deferred. Decide whether the defining trait is a landing shockwave,
knockback, brief slowing, or another localized control effect before adding a mixin or damage value.

### Water and Lightning swords

Water needs an explicit server-owned movement state machine only after impact/landing and collision rules
are settled; a plain immediate velocity impulse cannot safely define the two-phase skill. Lightning should
use player-owned server runtime state unless a per-target decision is made, but no attachment is registered
until threshold, timeout/reset, death/logout/reload persistence, and active parameters are decided.

## 5. Deliberate non-implementation and next decisions

This slice does not change gameplay, register unfinished items, add recipes, select Wind's active, replace
the Flame prototype, create elemental VFX, implement homing, alter vanilla mace behavior, or implement the
Dark Energy Staff. Therefore it adds no new LIVE PENDING runtime claims.

The next bounded T2 slice needs these decisions first:

1. Fire passive damage source/formula and exact sweep geometry; diagonal slash damage/shape and whether the
   input system gains an authoritative right-click route.
2. Player-owned Fire/Wind counter lifetime across death, logout, restart, and weapon switching, plus the
   single authoritative successful-hit hook.
3. Wind's one final active and the disposition of Wind Blade versus Wind Step.
4. Concrete basic-hit particle/sound choices and per-hit budgets for all four elemental swords.
5. Water movement/impact contract; Lightning charge/reset/active contract; Wind Bow steering contract; and
   Earth Mace passive/active contract.
6. Staff combat design and Enhancement profile only in a later, explicitly authorized slice.
