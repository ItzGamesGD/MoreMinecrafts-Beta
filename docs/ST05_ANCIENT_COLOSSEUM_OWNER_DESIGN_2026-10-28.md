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

- **Q41 — B:** Round scaled regular-mob threat budgets to the **nearest integer** after applying Q40's +60%-per-additional-participant multiplier. For 1/2/3/4 players, rounded budgets are respectively: R1–2 **20/32/44/56**; R3–4 **32/51/70/90**; R5–6 **48/77/106/134**. Tie handling for exact .5 values remains an implementation detail; mob threat score calibration and exact composition remain OPEN.

- **Q42 — B:** When randomized monster composition cannot satisfy both exact mob-count scaling and the rounded threat budget, **prioritize staying within the threat budget** (Q36/Q39–Q41). Treat Q21's +60% per additional player as a **target mob-count growth rate**, not a hard exact-count guarantee; actual total mob count may vary with the selected mob threat scores. Preserve Q24's maximum 12 simultaneously alive and Q25's batch replenishment. Exact selection/fill behavior and minimum viable composition remain OPEN.

- **Q43 — B:** For each round 1–6 randomized mob composition, target a **total threat score of at least 85% and no more than 100% of that round's rounded threat budget** (Q39–Q41), while retaining Q42 budget priority. With integer mob scores, minimum score is ceil(0.85 × budget): solo R1–2 **17–20**, R3–4 **28–32**, R5–6 **41–48**. Handling an impossible 85%-fill combination remains OPEN.

- **Q44 — D:** If no eligible monster composition can satisfy Q43's **85–100% budget-fill range**, permit an exceptional composition that **exceeds the rounded Q39–Q41 threat budget by at most 5%** (maximum 105% of budget), while preserving the round's candidate pool and identity. This is an explicitly owner-approved exception to Q42's normally strict budget cap, not a general relaxation. Exact integer-score handling and fallback when no valid composition exists even within 105% remain OPEN.

- **Q45 — A:** Calculate the exceptional Q44 threat-score ceiling as **floor(rounded round budget × 1.05)**, ensuring the approved 5% maximum overshoot is never exceeded by integer mob threat scores. Solo budgets 20/32/48 therefore have exceptional ceilings **21/33/50**. Q43's 85–100% fill remains the primary normal target; Q44–Q45 only apply when a normal eligible composition is unavailable.

- **Q46 — A:** If no round-eligible composition can meet the normal Q43 85–100% threat-fill band or the Q44–Q45 exceptional ≤105% allowance, select the **highest-total-threat valid composition that does not exceed the base rounded round budget**, even if below the 85% minimum. Preserve the round-specific candidate pool; do not borrow monsters from adjacent rounds or inflate the cap further. An entirely empty/unusable candidate pool is a separate data-validation failure, not an approved gameplay fallback.

- **Q47 — B:** Assign each eligible regular/elite mob type an **integer threat score on a 1–10 scale**, based on Q37's holistic combat capability assessment. Suggested classification bands: weak **1–3**, ordinary **4–6**, strong/elite **7–10**; these are scoring guides rather than preassigned scores for specific mobs. Preserve Q39 budgets 20/32/48, Q40–Q41 multiplayer scaling/rounding and Q43–Q46 composition constraints. Actual mob-by-mob scores and roster remain OPEN.

- **Q48 — B:** Use **cumulative threat-score eligibility** for round-specific monster candidate pools: rounds 1–2 may select scores **1–4**, rounds 3–4 scores **1–7**, and rounds 5–6 scores **1–10**. Higher rounds retain access to lower-threat mobs, enabling mixed strong/weak compositions while respecting each round's identity and Q35–Q47 budget rules. Round 7 remains the separate Immortal Gladiator boss encounter. Actual candidate mob identities and selection weights remain OPEN.

- **Q49 — C:** Within the Q48 cumulative candidate pools, **shift random selection weights by round progression**: early rounds favor lower-threat mobs, middle rounds favor medium-threat mobs, and late rounds favor higher-threat mobs. Keep weaker mobs eligible in later rounds; exact probability tables are deferred to implementation/balance validation.
- **Owner process direction after Q49:** Stop drilling into low-level threat-score arithmetic and probability parameters. Resume high-level unresolved ST-05 design decisions, prioritizing completion rewards, replay/reward repeatability, boss-specific mechanics, architecture, world generation, and combat UI/presentation. Technical parameters remain deferred; do not treat this as implementation authorization.

- **Q50 — B+C combined:** The Ancient Colosseum completion rewards combine **rare enhancement/crafting materials** that connect to existing MoreMinecrafts equipment progression with **colosseum-exclusive equipment** associated with the Immortal Gladiator. Prioritize useful rare materials as the main reward pillar, with exclusive gear as a distinctive special reward. Exact items, quantities, drop probabilities, repeatability and reward ownership remain OPEN.

- **Q51 — owner custom (vanilla Trial Spawner model):** Colosseum replay/reward reset follows the **vanilla Trial Spawner's cooldown/re-activation principle** rather than Q51 A–D. After a successful completion, impose a **30-minute / 36,000 game-tick cooldown**, after which the arena can reactivate for a new trial. Repeated successful runs can yield rewards; **no special first-clear-only reward rule** is introduced. For multiplayer, preserve the Trial Spawner principle of tracking player-specific reward eligibility to avoid duplicate payouts. This adapts the replay/reward lifecycle only: ST-05 retains its own seven rounds and Immortal Gladiator boss. Exact server-persistence, player eligibility and cooldown edge cases remain implementation/open design details.

- **Q52 — C:** Colosseum-exclusive equipment reward identities are a **Gladiator's Sword and Gladiator's Shield** pair, reflecting the Immortal Gladiator's phase-one sword-and-shield duelist style. Both are eligible exclusive completion rewards under Q50. This does **not** require granting both simultaneously or as a guaranteed set; acquisition method, unique abilities, stats and drop rates remain OPEN.

- **Q53 — B:** Gladiator's Sword and Gladiator's Shield (Q52) have **attack/defense-linked unique combat identities** rather than merely higher base stats: the sword rewards successfully chained attacks, while the shield rewards well-timed blocks followed by counters. Favor natural extensions of existing Minecraft combat actions without requiring a complex new control scheme. Exact trigger windows, bonuses, damage, duration, cooldown and balance remain OPEN.

- **Q54 — A (owner-customized):** Architectural atmosphere is a **ruined ancient Roman-style colosseum** constructed primarily from **deepslate-family blocks** in the deep underground cavern. Convey age with partial damage to spectator stands and outer masonry; optionally incorporate **small amounts of D-style natural cave decoration** (moss/vines etc.) where appropriate, without changing the primary A identity. **Keep the arena's interior fighting space brightly and clearly illuminated** for combat visibility, and preserve its intentionally constructed, generally flat combat floor. The surrounding cavern may remain darker. This is an owner choice, **not a request for further image generation**.

- **Q55 — A:** Colosseum spectator seating, upper tiers and stands are **decorative architecture only**, expressing scale and ancient ruin atmosphere. They are not designed as traversable exploration routes or part of the encounter combat area. Player combat and the seven-round trial are confined to the **central, flat, well-lit circular fighting floor**, preserving the existing spawn and leave-zone rules. Do not add spectator-stand exploration as a required gameplay loop.

- **Q56 — owner clarification of Q27/Q28:** The colosseum has **four cardinal player entrances (north, east, south, west)**. These are for player access only; **no monsters spawn from entrance gates**. The earlier correction revoking Q27/Q28's *monster-gate* interpretation must not be construed as revoking the four-direction *player entrance* layout. Preserve Q29–Q30's fixed-center 12–24-block mob-spawn annulus. Q56 does not introduce a new entrance decision; it records and clarifies the owner's prior design intent.

- **Q57 — B:** The Ancient Colosseum is a **rare large underground structure**, less common than ordinary dungeons but discoverable through sustained exploration of the Overworld's deep, sufficiently large natural caves. Preserve the deepslate ruin architecture, four cardinal player entrances and brightly lit central combat floor. Exact structure spacing, separation, cave-fit checks and generation sampling remain OPEN for worldgen implementation/testing; do not equate this qualitative tier with an unapproved numerical rate.

- **Q58 — D:** Allow **two discovery routes** for the rare underground Ancient Colosseum: incidental discovery through natural cave exploration, and intentional search using a **dedicated exploration map pointing to the structure**. Do not make map ownership mandatory to access or activate the colosseum. Map acquisition source, rarity, trade terms and location behavior remain OPEN.

- **Q59 — owner custom:** Obtain the Ancient Colosseum exploration map through **existing vanilla-style map acquisition systems**, analogous to the Woodland Mansion exploration map, including **cartographer villager trades** as an example. Integrate into established systems rather than creating a dedicated new vendor or standalone map acquisition mechanic. Natural cave discovery (Q58) remains valid. Exact trade tier, price, availability and other compatible existing sources remain OPEN.

- **Q60 — owner custom:** Like the vanilla Trial Spawner, use a **blue particle effect to signal trial activation/start**. Do **not** show ongoing round number, remaining mob count, progress percentage, or a dedicated actionbar/bossbar/HUD for rounds 1–6. Preserve the separate Q31 approximately one-second particle warning at mob spawn points. This decision does not remove the normal health bar of the round-7 boss; boss UI remains a separate vanilla-style presentation consideration.

- **Q61 — B:** At the start of round 7 after the Q15 standard **10-second post-round-6 interval**, summon the Immortal Gladiator at the **center of the arena** with a distinct awakening presentation: particles converge at the center, the sword-and-shield boss materializes, raises its weapons, and then begins combat. Preserve Q7–Q9's two-phase behavior and 40%-HP shield-break transition. Exact particle type/count, animation duration and hitbox timing remain implementation/balance details; no statue or entrance-gate spawn mechanic is introduced.

- **Q62 — A:** When the Immortal Gladiator is defeated in round 7, use an **ordinary mob death presentation**. Do not add a bespoke collapse, kneeling, petrification, arena-wide victory lighting, or celebratory particle sequence. Successful completion makes the previously approved repeatable trial rewards available, but **reward delivery/claim method remains OPEN**; do not assume that rewards must drop from the boss or spawn at arena center.

- **Q63 — owner custom, combined reward routes:** (1) On defeat, the Immortal Gladiator **drops its loot at its feet**, using normal item drops; (2) on successful trial completion, a **Trial Spawner-style reward device presents items and/or keys above the device** for distribution, analogous to vanilla Trial Spawner output; (3) place **separate Vault(s)** in the colosseum so keys can unlock **additional loot**. Preserve Q51's repeatable 30-minute trial cooldown and player-specific eligibility/no duplicate rewards; do not presume all three channels share the same reward pool. Exact device placement, vault count, key type/eligibility, drop tables, loot probabilities and per-player multiplayer claim behavior remain OPEN for later decisions.

- **Q64 — C:** The Ancient Colosseum contains **only vanilla Ominous Vaults** as its separate key-opened vault rewards; do not place regular Vaults or add a custom Colosseum Vault block. Preserve Q63's three channels: Immortal Gladiator drops at its feet, Trial Spawner-style reward output above the reward device, and additional loot from Ominous Vaults. The method of obtaining compatible Ominous Trial Keys, the number and placement of vaults, and loot tables remain OPEN.

- **Q65 — owner custom; supersedes Q64's vault-only layout where conflicting:** Arrange **seven evenly spaced Vaults as floor decoration around the arena**, corresponding to seven directions/positions, and **one Ominous Vault in the center**. Interpret the seven surrounding Vaults as ordinary vanilla Vaults (as contrasted with the explicitly ominous central Vault), pending owner correction. Include **one corresponding regular trial key in each wave's reward**, across all seven waves, and have the Immortal Gladiator **also drop an Ominous Trial Key at its feet** upon defeat. Vanilla loot-table selection or custom vault loot tables are both permissible design options, **not yet chosen**. Keep Q63's trial-style reward output and boss foot drops; avoid asserting final loot odds or multiplayer claim rules. Q64's 'ominous vaults only' is **superseded** by this newer explicit seven-surrounding-plus-one-center layout; do not erase the history of Q64.

## Still open
- Q9 phase-change threshold APPROVED (40%); exact transition timing remains OPEN.
- Precise round mob selection and special rules; supply chest placement, multiplayer and claim safeguards; boss stats/cooldowns; death/retry; rewards; geometry and worldgen parameters.
- No code, build, merge or checkpoint promotion is authorized by this document.
