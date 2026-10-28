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

- **Q15 — custom:** Inter-round delay is **10 seconds** after ordinary rounds and **35 seconds** after resupply rounds 3 and 5. Both delays end with automatic progression. These are owner-approved values, replacing Q14's previously unspecified durations.

- **Q16 — B:** If all participants leave the arena combat zone, pause the trial; resume if they return within a grace period, otherwise reset the run. If at least one participant remains, the trial continues. Exact grace duration OPEN. Death, reconnect, server restart and multiplayer edge cases OPEN.

- **Q17 — B:** Grace period for all participants leaving the arena is **60 seconds**. Returning before expiry resumes the paused trial; expiration resets the run. At least one participant remaining prevents this exit pause. Exact timer boundary behavior and server lifecycle handling remain implementation details/open edge cases.

- **Q18 — B:** The trial fails on total party defeat, not on the first participant death. In single-player, death fails the run. In multiplayer, surviving participants may continue; all participants dead means failure and reset. Whether dead players may rejoin after respawn remains OPEN (Q19). Interaction with Q16's empty-arena pause requires explicit resolution.

- **Q19 — B:** In multiplayer, a dead participant may rejoin only after surviving teammates clear the current round, before the next round begins. No rejoining the ongoing round. Total-party defeat still immediately fails and resets the trial under Q18. Precise admission mechanics and timing within the 10-second/35-second transition remain OPEN.

- **Q20 — B:** Multiplayer difficulty scales primarily through additional enemy counts in regular rounds; elite encounters should avoid excessive arena crowding. The final Immortal Gladiator boss gains some HP with additional participants. Exact scaling coefficients, caps and participant counting rules remain OPEN.

- **Q21 — C:** For non-boss encounter enemy counts, scale relative to the one-player baseline by **+60% per additional participant**: multiplier `1 + 0.60 × (participants - 1)` (1p 100%, 2p 160%, 3p 220%, 4p 280%). Integer rounding, simultaneous mob cap, staggered spawning and participant count lock remain OPEN. Boss HP scaling is a separate decision.

- **Q22 — B:** Final boss maximum HP scales **+35% per additional participant**: multiplier `1 + 0.35 × (participants - 1)` (1p 100%, 2p 135%, 3p 170%, 4p 205%). Phase-two awakening still triggers at 40% of the boss's scaled maximum HP. Base boss HP and participant-count locking rules remain OPEN.

- **Q23 — B:** Recalculate active participant count **at the start of each round** and lock that round's difficulty for its duration. Apply Q21 (+60% enemies per additional participant) for non-boss rounds and Q22 (+35% maximum boss HP per additional participant) at the start of round 7. Joins, departures and Q19 re-entry affect the next round only. Exact eligibility for counting participants remains OPEN.

- **Q24 — B:** In regular and elite rounds, cap simultaneously alive encounter mobs at **12**. If a round's scaled total exceeds 12, spawn the remainder as existing encounter mobs are defeated. The round-7 Immortal Gladiator boss is treated separately. Exact replenishment timing and batch size remain OPEN.

- **Q25 — C:** Batch replenishment in regular/elite rounds: once alive encounter mob count falls to **6 or fewer**, summon remaining assigned mobs up to the **12-alive** Q24 cap, repeating until the round's total allocation is exhausted. No additional mobs after the allocation is depleted. Exact spawn placement/telegraph remain OPEN.

- **Q26 — owner custom:** The structure is a circular Roman-colosseum-like arena. The **first wave starts when a player reaches the center** of the arena. Enemies emerge from the **outer circular perimeter** and move into the central combat space; Q25 batch reinforcements use the perimeter as well. Later rounds retain Q14/Q15 automatic progression (10-second standard / 35-second resupply waits). Number and arrangement of outer gates/spawn points remain OPEN.

- **Q27 — A:** The circular colosseum has **four outer enemy entrances**, oriented north, east, south and west. They serve the initial and Q25 batch-replenishment enemy spawns. Which gates activate per wave and exact spawn allocation remain OPEN.

- **Q28 — B:** For each initial or replenishment spawn batch, randomly select **2–3 of the four cardinal outer entrances** as active spawn directions. Maintain Q24's 12-alive cap and Q25's replenishment trigger at 6 or fewer alive. Exact distribution across selected entrances, warning cues and spawn safety remain OPEN.

- **Owner correction superseding the earlier Q26–Q28 interpretation:** Arena entrances are for **players entering the colosseum**, NOT monster spawn gates. The first wave triggers when a player reaches the center. Monsters spawn Trial-Spawner-style at valid positions in a **much wider circular area centered on the player**, suitable for the large colosseum. Preserve Q24 (12 simultaneous mobs) and Q25 (batch refill at 6 or fewer). **Withdraw the previously recorded Q27 four cardinal monster gates and Q28 random 2–3 gate activation as erroneous interpretations; neither is an approved monster-spawn rule.** Q29 gate-warning presentation is inapplicable and unapproved. Exact player entrance count, spawn radii, minimum safe distance, center-tracking behavior, telegraphs and line-of-sight rules remain OPEN.

- **Revised Q29 — B:** Spawn mobs at valid positions in a wide **12–24-block radial annulus around the player**, Trial-Spawner-style, not from the colosseum's player entrances. Keep the Q24 12-alive cap and Q25 batch refill at 6 or fewer. Vertical range, arena-boundary clipping, distribution and center-tracking behavior remain OPEN.

- **Q30 — A:** The **fixed colosseum arena center** is the origin of the revised Q29 12–24-block radial mob spawn annulus, independent of any player's position, including in multiplayer. The center-entry trigger for starting round 1 remains unchanged. Valid spawn positions must stay inside the combat arena; vertical constraints and placement safety remain OPEN.

- **Q31 — B:** For ordinary and elite mob spawns in the fixed-center 12–24-block arena annulus, display a lightweight **particle warning for approximately 1 second at each selected valid spawn point** before the mob materializes. Apply to initial and Q25 batch replenishment spawns. Specific particle types, sound and boss entrance staging remain OPEN.

- **Q32 — B:** Monster spawn points must lie inside the colosseum combat floor within the fixed 12–24-block radial annulus, on solid supporting ground with enough clearance for each mob's collision box; reject wall/obstacle intersections and immediately hazardous or inaccessible terrain such as lava. Preserve the Q31 ~1-second particle warning. Fallback behavior when insufficient valid spawn points exist remains OPEN.

- **Owner clarification to Q32:** The colosseum is **built inside a natural cave**, but its **arena fighting floor is intentionally constructed and generally flat**, not an uneven natural cave floor. Q32's valid-spawn checks are basic solid-floor and collision/clearance safeguards, not a mandate for elaborate terrain/hazard handling or conservative placement constraints.
- **Q33 — B:** If some positions in the fixed 12–24-block annulus fail basic validation, spawn the mobs that can be placed safely, retain the unspawned allocated mobs in the round's pending queue, and retry later without losing their allocation. Behavior if **no valid spawn positions remain persistently** is still OPEN.

- **Q34 — A:** Unspawned mobs retained in the Q33 pending queue are retried **on the next regular batch-spawn cycle when alive encounter mobs are at or below 6**, using the existing Q25 replenishment system. No separate fixed-interval retry loop. Preserve total allocated mobs without duplication or loss; persistent all-position failure handling remains OPEN.

- **Q35 — B:** For rounds 1–6, randomly draw monster combinations from **round-specific eligible candidate pools**, maintaining each round's already-approved combat role and difficulty progression. Candidate pools may include vanilla and existing MoreMinecrafts mobs (Q5). Round 7 remains the Immortal Gladiator boss encounter. Exact roster, weights and composition constraints remain OPEN.

- **Q36 — C:** Use a **threat-budget system** for randomized round-specific mob combinations (Q35): each eligible mob type has a threat score, and each round's total selected composition must fit its threat budget. Stronger mobs consume more budget, permitting fewer total mobs at a given budget. Exact scores, per-round budgets, reconciliation with Q21 participant-count scaling and Q24/Q25 concurrent/batch limits remain OPEN.

- **Q37 — B:** Assign each eligible mob type a threat score using a **holistic combat-capability assessment**: health, damage output, attack patterns (including ranged/AOE), mobility, and special abilities. Use these scores in the Q36 per-round threat budgets; exact numeric weights, mob ratings and testing-based adjustments remain OPEN.

- **Q38 — C:** Threat budgets for rounds 1–6 follow a **three-tier stepped escalation**: rounds 1–2 at similar baseline budgets, rounds 3–4 at a higher budget tier, and rounds 5–6 at a substantially higher final pre-boss tier. Preserve existing per-round identities (R3 special, R4 enhanced regular, R5 elite, R6 enhanced elite) and supply breaks after R3/R5; R7 is the separately designed boss fight. Exact numeric budgets and within-tier differences remain OPEN.

- **Q39 — B:** Approve provisional **single-player threat budgets**: rounds 1–2 = **20 points each**, rounds 3–4 = **32 points each**, rounds 5–6 = **48 points each**. Round 7 is a separate boss encounter without this regular-mob budget. These are owner-selected numerical design targets; individual mob threat scores and resulting encounter duration require later balancing. Multiplayer scaling interaction with Q21 remains OPEN.

- **Q40 — B:** Scale each round's regular-mob **threat budget by +60% of its solo baseline per additional participant**, matching the Q21 monster-count scaling: multiplier = 1 + 0.60 × (participants − 1), using Q23's participant count locked at each round start. From Q39 solo budgets 20/32/48, two players yield 32/51.2/76.8, three yield 44/70.4/105.6, and four yield 56/89.6/134.4 for rounds 1–2 / 3–4 / 5–6 respectively. Integer rounding and how to reconcile exact headcounts with budget remain OPEN. Q24's 12-alive cap remains unchanged; R7 boss HP uses the separately approved Q22 scaling.

## Still open
- Q9 phase-change threshold APPROVED (40%); exact transition timing remains OPEN.
- Precise round mob selection and special rules; supply chest placement, multiplayer and claim safeguards; boss stats/cooldowns; death/retry; rewards; geometry and worldgen parameters.
- No code, build, merge or checkpoint promotion is authorized by this document.
