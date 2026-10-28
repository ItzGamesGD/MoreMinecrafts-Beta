# ST-05 Ancient Colosseum — Owner decisions (2026-10-28)

Status: **OWNER-APPROVED DESIGN ONLY. NO IMPLEMENTATION AUTHORIZATION.**
Source baseline: `checkpoint/wave12-unified-20261027` structure authority index. This record adds owner decisions to ST-05; it does not change ST-01 or the Ancient City arena.

## Identity and placement
- Standalone Ancient Colosseum, **only inside an enormous natural Overworld cave**, never a surface colosseum.
- Distinct from the arena inside the Ancient City (Phase 2 F-6).

## Owner-approved decisions
- **Q1 — B:** Sequential combat trials.
- **Q2 — C:** Exactly **seven** combat rounds.
- **Q3 — C:** Limited healing/supplies after every 2–3 rounds. Exact placement, amount and type OPEN.
- **Q4 — A + variety principle:** Traditional escalating progression, with different combat approaches rather than just increasing mob health/count:
  1. Normal combat
  2. Normal combat, varied enemy combinations/attacks
  3. Special combat
  4. Enhanced normal combat
  5. Elite combat
  6. Enhanced elite combat
  7. Final boss
  Detailed enemy roster and mechanics OPEN.
- **Q5 — B:** Mix vanilla mobs and existing MoreMinecrafts custom mobs. Design only the final boss as a new dedicated mob.
- **Q6 — A:** Final boss is the **Immortal Gladiator**, a sword-and-shield duelist emphasizing attack reads, defense and counterattacks.
- **Q7 — B+D:** Technical duelist with two phases. Phase 1: sword combos, shield block/counter, lunging thrust and broad horizontal slash. Phase 2: at an as-yet-undecided HP threshold, discards shield and uses faster, more aggressive sword attacks.
- **Q8 — B:** Dramatic awakening. Briefly pause fighting, break the shield and roar, with an unmistakable transition and player response window.

## Animation implementation alternatives — owner-approved conditional policy
**Do not interpret this as approval to add GeckoLib or implement animations.**
- **If GeckoLib is used:** full expressive boss presentation, including distinct sword combos, blocking/counters, thrusts, shield break, roar, and phase transition; synchronized telegraphs, attack hit windows and visual effects. Exact assets and timing remain implementation-design choices.
- **If GeckoLib is not used:** optimized lightweight animations implemented **within MoreMinecrafts itself**, using simple motions and restrained effects while preserving clear attack telegraphs, shield/attack state readability, and the approved two-phase transition. Avoid building a general-purpose animation engine or requiring extra mods.
- **Decision OPEN:** whether GeckoLib is adopted. Compare 26.3 Fabric compatibility, distribution/runtime dependency burden, maintenance and performance before implementation. Neither route changes the approved gameplay mechanics.
- Animation and damage/hitbox timings must match. The lightweight route is a presentation simplification, **not** permission to delete approved attacks or the phase transition.

## Subsequent owner decision
- **Q9 — B:** Immortal Gladiator phase transition triggers at **40% HP**. Phase 1 runs from full health down to 40%; at 40% the boss pauses, breaks the shield, roars and transitions to faster shieldless sword attacks. Exact animation durations and combat values remain OPEN.

- **Q10 — B:** Exactly two limited resupply opportunities, **after rounds 3 and 5** (before rounds 4 and 6). No resupply after round 6. Exact items, amounts and delivery mechanism OPEN.

- **Q11 — B:** Balanced resupply at the Q10 checkpoints: food, arrows, and a small quantity of healing items. Preserve resource-management pressure; exact item types, grades and quantities OPEN.

- **Q12 — B, golden-apple substitution:** Each resupply after round 3 and round 5 grants **8 cooked beef, 16 arrows, and 1 ordinary golden apple** (not an enchanted golden apple). Instant-health potions are not included. Total over both resupplies: 16 cooked beef, 32 arrows, 2 golden apples. Distribution mechanism OPEN.

- **Q13 — B:** After rounds 3 and 5, a supply chest in the colosseum becomes accessible. Players manually take the approved Q12 items from the chest. Chest placement, visuals, multiplayer ownership and anti-repeat-claim behavior remain OPEN; no repeat farming is authorized.

- **Q14 — A (modified):** Trial-spawner-like automatic progression: after ordinary rounds, a short pause then next round starts automatically; after rounds 3 and 5, provide a substantially longer preparation interval with the Q13 supply chest accessible before automatically starting the next round. Exact durations OPEN. Round 7 ends the trial upon boss defeat.

## Still open
- Q9 phase-change threshold APPROVED (40%); exact transition timing remains OPEN.
- Precise round mob selection and special rules; supply chest placement, multiplayer and claim safeguards; boss stats/cooldowns; death/retry; rewards; geometry and worldgen parameters.
- No code, build, merge or checkpoint promotion is authorized by this document.
